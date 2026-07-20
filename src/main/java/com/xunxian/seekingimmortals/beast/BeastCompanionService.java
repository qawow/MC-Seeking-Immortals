package com.xunxian.seekingimmortals.beast;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.xunxian.seekingimmortals.SeekingImmortalsMod;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * M10: spirit-beast companion catalog + growth stages.
 * Red line: protected companions / true spirits never enter daily spawn tables.
 */
public final class BeastCompanionService {
    /** Hard-coded protected set from region_spawn_tables_v98 global_rules + companion corpus. */
    private static final Set<String> HARD_PROTECTED_IDS = Set.of(
            "shi_jin_chong", "ti_hun_shou", "tian_lan_shou", "bing_feng",
            "xue_yu_zhizhu", "liu_yi_shuang_gong", "tu_jia_long", "bao_lin_shou",
            "zhi_xian", "qu_er", "yin_yue_xueling", "yuling_wuxing_infant");

    private static final Set<String> HARD_PROTECTED_DISPLAY = Set.of(
            "噬金虫", "啼魂", "啼魂兽", "天澜圣兽", "冰凤", "血玉蜘蛛", "六翼霜蚣",
            "土甲龙", "豹麟兽", "芝仙", "曲儿", "银月", "雪玲");

    private static final Snapshot SNAPSHOT = load();

    private BeastCompanionService() {}

    public record GrowthStage(int stage, String name, List<String> abilities, List<String> feed, String combat) {}

    public record CompanionDef(
            String id,
            String display,
            String type,
            int startTier,
            int capTier,
            List<String> feed,
            List<String> abilities,
            List<GrowthStage> stages) {}

    public record Snapshot(Map<String, CompanionDef> byId, Set<String> protectedIds, Set<String> protectedDisplays) {
        public int size() {
            return byId.size();
        }
    }

    public static Snapshot snapshot() {
        return SNAPSHOT;
    }

    public static int size() {
        return SNAPSHOT.size();
    }

    public static Set<String> companionIds() {
        return SNAPSHOT.byId().keySet();
    }

    public static Optional<CompanionDef> find(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(SNAPSHOT.byId().get(id.trim().toLowerCase(Locale.ROOT)));
    }

    public static boolean isProtectedCompanion(String idOrDisplay) {
        if (idOrDisplay == null || idOrDisplay.isBlank()) {
            return false;
        }
        String raw = idOrDisplay.trim();
        String key = raw.toLowerCase(Locale.ROOT);
        if (HARD_PROTECTED_IDS.contains(key) || SNAPSHOT.protectedIds().contains(key)) {
            return true;
        }
        if (HARD_PROTECTED_DISPLAY.contains(raw) || SNAPSHOT.protectedDisplays().contains(raw)) {
            return true;
        }
        // Partial display match (e.g. "天澜圣兽（分身/本体线）")
        for (String display : SNAPSHOT.protectedDisplays()) {
            if (raw.contains(display) || display.contains(raw)) {
                return true;
            }
        }
        return SNAPSHOT.byId().containsKey(key);
    }

    /**
     * Growth stage for a contract growth counter (0..N). Caps at last stage.
     */
    public static Optional<GrowthStage> stageForGrowth(String companionId, int growth) {
        Optional<CompanionDef> def = find(companionId);
        if (def.isEmpty() || def.get().stages().isEmpty()) {
            return Optional.empty();
        }
        List<GrowthStage> stages = def.get().stages();
        int idx = Math.max(0, Math.min(stages.size() - 1, growth / Math.max(1, 20 / stages.size())));
        // Map growth 0-20 across stages evenly.
        int mapped = (int) Math.floor((Math.max(0, Math.min(20, growth)) / 20.0D) * (stages.size() - 1));
        return Optional.of(stages.get(Math.max(0, Math.min(stages.size() - 1, mapped))));
    }

    public static int stageCount(String companionId) {
        return find(companionId).map(def -> Math.max(1, def.stages().size())).orElse(1);
    }

    public static Optional<GrowthStage> stageForEvolution(String companionId, int evolutionStage) {
        Optional<CompanionDef> def = find(companionId);
        if (def.isEmpty() || def.get().stages().isEmpty()) {
            return Optional.empty();
        }
        List<GrowthStage> stages = def.get().stages();
        return Optional.of(stages.get(Math.max(0, Math.min(stages.size() - 1, evolutionStage))));
    }

    public static int tierForEvolution(String companionId, int evolutionStage) {
        Optional<CompanionDef> optional = find(companionId);
        if (optional.isEmpty()) {
            return 1;
        }
        CompanionDef def = optional.get();
        int maxEvolution = Math.max(1, def.stages().size() - 1);
        double ratio = Math.max(0, Math.min(maxEvolution, evolutionStage)) / (double) maxEvolution;
        return BeastTierService.clampTier((int) Math.round(
                def.startTier() + (def.capTier() - def.startTier()) * ratio));
    }

    public static double growthStatMultiplier(String companionId, int growth) {
        Optional<GrowthStage> stage = stageForGrowth(companionId, growth);
        if (stage.isEmpty()) {
            return 1.0D + Math.max(0, growth) * 0.05D;
        }
        return 1.0D + stage.get().stage() * 0.35D + Math.max(0, growth) * 0.03D;
    }

    public static double growthStatMultiplier(String companionId, CompanionGrowthService.Progress progress) {
        Optional<GrowthStage> stage = stageForEvolution(companionId,
                progress == null ? 0 : progress.evolutionStage());
        double authored = stage.map(value -> value.stage() * 0.12D).orElse(0.0D);
        return CompanionGrowthService.statMultiplier(progress) + authored;
    }

    private static Snapshot load() {
        Map<String, CompanionDef> byId = new LinkedHashMap<>();
        Set<String> protectedIds = new LinkedHashSet<>(HARD_PROTECTED_IDS);
        Set<String> protectedDisplays = new LinkedHashSet<>(HARD_PROTECTED_DISPLAY);

        // Growth stages authority.
        Map<String, List<GrowthStage>> growth = new LinkedHashMap<>();
        JsonObject growthRoot = readJson("data/" + SeekingImmortalsMod.MODID + "/text_material/spirit_beast_growth_v99.json");
        if (growthRoot != null && growthRoot.has("companions") && growthRoot.get("companions").isJsonArray()) {
            for (JsonElement element : growthRoot.getAsJsonArray("companions")) {
                if (!element.isJsonObject()) {
                    continue;
                }
                JsonObject o = element.getAsJsonObject();
                String id = str(o, "id").toLowerCase(Locale.ROOT);
                if (id.isBlank()) {
                    continue;
                }
                List<GrowthStage> stages = new ArrayList<>();
                if (o.has("stages") && o.get("stages").isJsonArray()) {
                    for (JsonElement se : o.getAsJsonArray("stages")) {
                        if (!se.isJsonObject()) {
                            continue;
                        }
                        JsonObject s = se.getAsJsonObject();
                        int stage = s.has("stage") ? s.get("stage").getAsInt() : stages.size();
                        stages.add(new GrowthStage(
                                stage,
                                str(s, "name"),
                                stringList(s, "ability"),
                                stringList(s, "feed"),
                                str(s, "combat")));
                    }
                }
                growth.put(id, List.copyOf(stages));
                protectedIds.add(id);
                String display = str(o, "display");
                if (!display.isBlank()) {
                    protectedDisplays.add(display);
                }
            }
        }

        JsonObject companionRoot = readJson("data/" + SeekingImmortalsMod.MODID + "/text_material/spirit_beast_companion_v93.json");
        if (companionRoot != null && companionRoot.has("companions") && companionRoot.get("companions").isJsonArray()) {
            for (JsonElement element : companionRoot.getAsJsonArray("companions")) {
                if (!element.isJsonObject()) {
                    continue;
                }
                JsonObject o = element.getAsJsonObject();
                String id = str(o, "id").toLowerCase(Locale.ROOT);
                if (id.isBlank()) {
                    continue;
                }
                int start = o.has("start_tier") ? o.get("start_tier").getAsInt() : 1;
                int cap = o.has("cap_tier_structure") ? o.get("cap_tier_structure").getAsInt() : 13;
                List<GrowthStage> stages = growth.getOrDefault(id, List.of());
                // Prefer growth stages; fall back to companion growth_stages if present.
                if (stages.isEmpty() && o.has("growth_stages") && o.get("growth_stages").isJsonArray()) {
                    List<GrowthStage> built = new ArrayList<>();
                    int i = 0;
                    for (JsonElement se : o.getAsJsonArray("growth_stages")) {
                        if (!se.isJsonObject()) {
                            continue;
                        }
                        JsonObject s = se.getAsJsonObject();
                        built.add(new GrowthStage(
                                i++,
                                str(s, "stage").isBlank() ? str(s, "name") : str(s, "stage"),
                                stringList(s, "ability"),
                                stringList(s, "feed"),
                                str(s, "combat")));
                    }
                    stages = List.copyOf(built);
                }
                String display = str(o, "display");
                CompanionDef def = new CompanionDef(
                        id,
                        display,
                        str(o, "type"),
                        BeastTierService.clampTier(start),
                        BeastTierService.clampTier(cap),
                        stringList(o, "feed"),
                        stringList(o, "abilities"),
                        stages);
                byId.put(id, def);
                protectedIds.add(id);
                if (!display.isBlank()) {
                    protectedDisplays.add(display);
                }
            }
        }

        // Ensure growth-only companions still register.
        for (Map.Entry<String, List<GrowthStage>> e : growth.entrySet()) {
            if (byId.containsKey(e.getKey())) {
                continue;
            }
            byId.put(e.getKey(), new CompanionDef(
                    e.getKey(), e.getKey(), "companion", 1, 13, List.of(), List.of(), e.getValue()));
            protectedIds.add(e.getKey());
        }

        // region_spawn_tables global_rules companion_no_wild_farm displays
        JsonObject regionRoot = readJson("data/" + SeekingImmortalsMod.MODID + "/text_material/region_spawn_tables_v98.json");
        if (regionRoot != null && regionRoot.has("global_rules") && regionRoot.get("global_rules").isJsonObject()) {
            JsonObject rules = regionRoot.getAsJsonObject("global_rules");
            if (rules.has("companion_no_wild_farm") && rules.get("companion_no_wild_farm").isJsonArray()) {
                for (JsonElement el : rules.getAsJsonArray("companion_no_wild_farm")) {
                    if (el.isJsonPrimitive()) {
                        protectedDisplays.add(el.getAsString().trim());
                    }
                }
            }
        }

        return new Snapshot(
                Collections.unmodifiableMap(byId),
                Collections.unmodifiableSet(protectedIds),
                Collections.unmodifiableSet(protectedDisplays));
    }

    private static List<String> stringList(JsonObject object, String key) {
        List<String> list = new ArrayList<>();
        if (object == null || !object.has(key)) {
            return list;
        }
        JsonElement element = object.get(key);
        if (element.isJsonArray()) {
            for (JsonElement e : element.getAsJsonArray()) {
                if (e.isJsonPrimitive()) {
                    String v = e.getAsString().trim();
                    if (!v.isBlank()) {
                        list.add(v);
                    }
                }
            }
        } else if (element.isJsonPrimitive()) {
            String v = element.getAsString().trim();
            if (!v.isBlank()) {
                list.add(v);
            }
        }
        return list;
    }

    private static JsonObject readJson(String path) {
        try (InputStream stream = BeastCompanionService.class.getClassLoader().getResourceAsStream(path)) {
            if (stream == null) {
                return null;
            }
            try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                return JsonParser.parseReader(reader).getAsJsonObject();
            }
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String str(JsonObject object, String key) {
        if (object == null || !object.has(key) || object.get(key).isJsonNull()) {
            return "";
        }
        try {
            JsonElement e = object.get(key);
            if (e.isJsonPrimitive()) {
                return e.getAsString().trim();
            }
            return e.toString();
        } catch (Exception ignored) {
            return "";
        }
    }
}
