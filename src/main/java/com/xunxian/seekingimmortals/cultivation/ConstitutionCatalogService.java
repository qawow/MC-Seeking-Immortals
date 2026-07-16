package com.xunxian.seekingimmortals.cultivation;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.xunxian.seekingimmortals.SeekingImmortalsMod;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 体质目录（语料 constitution_catalog）。
 * <p>优先读 jar 内 {@code text_material/constitution_catalog.json}，失败时用内置兜底。</p>
 */
public final class ConstitutionCatalogService {
    /** 与灵根叠乘总修炼速度上限（语料 stack_cap_note）。 */
    public static final double STACK_CAP = 2.5D;

    private static final Snapshot BUILTIN = loadBuiltin();

    private ConstitutionCatalogService() {}

    public static Snapshot builtin() {
        return BUILTIN;
    }

    public record ConstitutionEntry(
            String id,
            String display,
            String rarity,
            double cultivationMult,
            double breakthroughMult,
            double tribulationDamageDelta,
            double physicalResist,
            double debuffResist,
            boolean deathSave,
            Map<String, Double> elementBonus) {
        public double safeCultivationMult() {
            return cultivationMult <= 0.0D ? 1.0D : cultivationMult;
        }

        public double safeBreakthroughMult() {
            return breakthroughMult <= 0.0D ? 1.0D : breakthroughMult;
        }
    }

    public record Snapshot(Map<String, ConstitutionEntry> byId, double stackCap) {
        public Optional<ConstitutionEntry> find(String id) {
            if (id == null || id.isBlank()) return Optional.empty();
            return Optional.ofNullable(byId.get(id.trim().toLowerCase()));
        }

        public int size() {
            return byId.size();
        }
    }

    public static double clampStackedCultivation(double rootMult, double constitutionMult) {
        double stacked = Math.max(0.0D, rootMult) * Math.max(0.0D, constitutionMult);
        return Math.min(STACK_CAP, stacked);
    }

    public static double cultivationMultiplier(String constitutionId) {
        return builtin().find(constitutionId).map(ConstitutionEntry::safeCultivationMult).orElseGet(() -> {
            SpecialPhysique physique = SpecialPhysique.fromConstitutionId(constitutionId);
            return physique.getCultivationMultiplier();
        });
    }

    public static double breakthroughMultiplier(String constitutionId) {
        return builtin().find(constitutionId).map(ConstitutionEntry::safeBreakthroughMult).orElseGet(() -> {
            SpecialPhysique physique = SpecialPhysique.fromConstitutionId(constitutionId);
            return physique.getBreakthroughMultiplier();
        });
    }

    public static double tribulationDamageDelta(String constitutionId) {
        return builtin().find(constitutionId).map(ConstitutionEntry::tribulationDamageDelta).orElse(0.0D);
    }

    private static Snapshot loadBuiltin() {
        Map<String, ConstitutionEntry> map = new LinkedHashMap<>();
        JsonObject root = readJson("data/" + SeekingImmortalsMod.MODID + "/text_material/constitution_catalog.json");
        if (root == null) {
            root = readJson("data/" + SeekingImmortalsMod.MODID + "/catalog/constitution_index.json");
        }
        if (root != null) {
            JsonArray array = firstArray(root, "constitutions", "entries");
            for (JsonElement element : array) {
                if (!element.isJsonObject()) continue;
                JsonObject o = element.getAsJsonObject();
                String id = str(o, "id").toLowerCase();
                if (id.isBlank()) continue;
                JsonObject bonus = o.has("bonus") && o.get("bonus").isJsonObject()
                        ? o.getAsJsonObject("bonus") : new JsonObject();
                Map<String, Double> elementBonus = new LinkedHashMap<>();
                double cultivation = 1.0D;
                double breakthrough = 1.0D;
                double tribDelta = 0.0D;
                double physical = 0.0D;
                double debuff = 0.0D;
                boolean deathSave = false;
                for (Map.Entry<String, JsonElement> entry : bonus.entrySet()) {
                    String key = entry.getKey();
                    JsonElement value = entry.getValue();
                    if (value == null || value.isJsonNull()) continue;
                    if ("cultivation".equals(key) && value.isJsonPrimitive()) {
                        cultivation = value.getAsDouble();
                    } else if ("breakthrough".equals(key) && value.isJsonPrimitive()) {
                        breakthrough = value.getAsDouble();
                    } else if ("thunder_tribulation".equals(key) && value.isJsonPrimitive()) {
                        tribDelta = value.getAsDouble();
                    } else if ("physical_resist".equals(key) && value.isJsonPrimitive()) {
                        physical = value.getAsDouble();
                    } else if ("debuff_resist".equals(key) && value.isJsonPrimitive()) {
                        debuff = value.getAsDouble();
                    } else if ("death_save".equals(key) && value.isJsonPrimitive()) {
                        deathSave = value.getAsBoolean();
                    } else if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber()) {
                        elementBonus.put(key, value.getAsDouble());
                    }
                }
                // 无 bonus 时从 rarity/tier 给温和默认
                if (!o.has("bonus")) {
                    cultivation = 1.05D;
                }
                map.put(id, new ConstitutionEntry(
                        id,
                        str(o, "display").isBlank() ? id : str(o, "display"),
                        firstNonBlank(str(o, "rarity"), str(o, "tier"), "common"),
                        cultivation,
                        breakthrough,
                        tribDelta,
                        physical,
                        debuff,
                        deathSave,
                        Collections.unmodifiableMap(elementBonus)));
            }
        }
        if (map.isEmpty()) {
            // 内置最小兜底，保证 API 可用
            putFallback(map, "tongyu_fengsui", "通玉凤髓", 1.15D, 1.05D, 0.0D);
            putFallback(map, "dragon_chant", "龙吟之体", 0.80D, 0.70D, -0.10D);
            putFallback(map, "self_govern", "自治之体", 1.10D, 1.05D, 0.0D);
            putFallback(map, "undying", "不灭之体", 1.15D, 1.20D, 0.0D);
            putFallback(map, "vajra_undamage", "金刚不坏", 1.20D, 1.10D, 0.0D);
            putFallback(map, "gold_forge", "锻金之体", 1.15D, 1.10D, 0.0D);
            putFallback(map, "nine_spirit_sword", "九灵剑体", 1.35D, 1.25D, 0.0D);
            putFallback(map, "void_shadow", "虚影之体", 1.10D, 1.15D, 0.0D);
            putFallback(map, "true_spirit_bloodline_generic", "真灵血脉（泛）", 1.25D, 1.20D, 0.0D);
            putFallback(map, "spirit_body_mild", "灵体（弱）", 1.10D, 1.05D, 0.0D);
            putFallback(map, "fire_spirit_root_variant", "变异火灵根", 1.10D, 1.15D, 0.0D);
            putFallback(map, "sword_intent_body", "剑意灵躯", 1.30D, 1.20D, 0.0D);
            putFallback(map, "yin_yang_unbalanced", "阴阳不调", 1.05D, 0.90D, 0.0D);
        }
        return new Snapshot(Collections.unmodifiableMap(map), STACK_CAP);
    }

    private static void putFallback(Map<String, ConstitutionEntry> map, String id, String display,
                                    double cultivation, double breakthrough, double tribDelta) {
        map.put(id, new ConstitutionEntry(id, display, "common", cultivation, breakthrough, tribDelta,
                0.0D, 0.0D, false, Map.of()));
    }

    private static JsonArray firstArray(JsonObject root, String... keys) {
        for (String key : keys) {
            if (root.has(key) && root.get(key).isJsonArray()) {
                return root.getAsJsonArray(key);
            }
        }
        return new JsonArray();
    }

    private static JsonObject readJson(String path) {
        try (InputStream stream = ConstitutionCatalogService.class.getClassLoader().getResourceAsStream(path)) {
            if (stream == null) return null;
            try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                return JsonParser.parseReader(reader).getAsJsonObject();
            }
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String str(JsonObject object, String key) {
        if (object == null || !object.has(key) || object.get(key).isJsonNull()) return "";
        try {
            JsonElement e = object.get(key);
            if (e.isJsonPrimitive()) return e.getAsString();
            return e.toString();
        } catch (Exception ignored) {
            return "";
        }
    }

    private static String firstNonBlank(String... values) {
        if (values == null) return "";
        for (String value : values) {
            if (value != null && !value.isBlank()) return value;
        }
        return "";
    }
}
