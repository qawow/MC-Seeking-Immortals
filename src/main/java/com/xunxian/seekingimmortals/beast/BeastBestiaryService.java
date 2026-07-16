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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * M10: data-driven beast bestiary (~1850 entries) built from shipped runtime compact index
 * plus detailed {@code beast_bestiary.json} drops/regions.
 */
public final class BeastBestiaryService {
    private static final Snapshot SNAPSHOT = load();

    private BeastBestiaryService() {}

    public record BeastEntry(
            String id,
            String display,
            String category,
            int threat,
            int tier,
            int tierMax,
            String element,
            List<String> regions,
            List<String> drops,
            String habitat,
            boolean tameable,
            boolean trueSpirit,
            boolean companionOnly,
            String entityIdHint) {

        public BeastTierService.ScaledStats scaledStats() {
            return BeastTierService.scaleStats(tier);
        }
    }

    public record Snapshot(Map<String, BeastEntry> byId, Map<String, String> displayToId) {
        public int size() {
            return byId.size();
        }

        public Optional<BeastEntry> find(String idOrDisplay) {
            if (idOrDisplay == null || idOrDisplay.isBlank()) {
                return Optional.empty();
            }
            String key = idOrDisplay.trim().toLowerCase(Locale.ROOT);
            BeastEntry direct = byId.get(key);
            if (direct != null) {
                return Optional.of(direct);
            }
            String mapped = displayToId.get(idOrDisplay.trim());
            if (mapped != null) {
                return Optional.ofNullable(byId.get(mapped));
            }
            // Fuzzy: strip spaces / full-width.
            String compact = key.replace(" ", "").replace("　", "");
            for (Map.Entry<String, String> e : displayToId.entrySet()) {
                String d = e.getKey().toLowerCase(Locale.ROOT).replace(" ", "").replace("　", "");
                if (d.equals(compact)) {
                    return Optional.ofNullable(byId.get(e.getValue()));
                }
            }
            return Optional.empty();
        }
    }

    public static Snapshot snapshot() {
        return SNAPSHOT;
    }

    public static int size() {
        return SNAPSHOT.size();
    }

    public static Optional<BeastEntry> find(String idOrDisplay) {
        return SNAPSHOT.find(idOrDisplay);
    }

    public static Map<String, BeastEntry> all() {
        return SNAPSHOT.byId();
    }

    public static boolean isTrueSpirit(String beastId) {
        return find(beastId).map(BeastEntry::trueSpirit).orElse(false);
    }

    public static boolean isCompanionOnly(String beastId) {
        return find(beastId).map(BeastEntry::companionOnly).orElse(false);
    }

    public static boolean isBannedFromDailySpawn(String beastId) {
        Optional<BeastEntry> entry = find(beastId);
        if (entry.isEmpty()) {
            return BeastCompanionService.isProtectedCompanion(beastId);
        }
        BeastEntry beast = entry.get();
        return beast.trueSpirit() || beast.companionOnly() || BeastCompanionService.isProtectedCompanion(beast.id());
    }

    private static Snapshot load() {
        Map<String, BeastEntry> byId = new LinkedHashMap<>();
        Map<String, String> displayToId = new LinkedHashMap<>();

        // Primary: compact runtime index (~1850).
        JsonObject runtime = readJson("data/" + SeekingImmortalsMod.MODID + "/text_material/beast_bestiary_runtime.json");
        if (runtime == null) {
            // Fallback to summary if runtime missing.
            runtime = readJson("data/" + SeekingImmortalsMod.MODID + "/text_material/bestiary_summary_v101.json");
        }
        if (runtime != null && runtime.has("creatures") && runtime.get("creatures").isJsonArray()) {
            for (JsonElement element : runtime.getAsJsonArray("creatures")) {
                if (!element.isJsonObject()) {
                    continue;
                }
                putCreature(byId, displayToId, element.getAsJsonObject());
            }
        }

        // Overlay detailed beast_bestiary for drops/regions/tier.
        JsonObject detailed = readJson("data/" + SeekingImmortalsMod.MODID + "/text_material/beast_bestiary.json");
        if (detailed != null && detailed.has("beasts") && detailed.get("beasts").isJsonArray()) {
            for (JsonElement element : detailed.getAsJsonArray("beasts")) {
                if (!element.isJsonObject()) {
                    continue;
                }
                JsonObject o = element.getAsJsonObject();
                String id = str(o, "id").toLowerCase(Locale.ROOT);
                if (id.isBlank()) {
                    continue;
                }
                BeastEntry prev = byId.get(id);
                int tier = o.has("tier") ? o.get("tier").getAsInt() : (prev == null ? 1 : prev.tier());
                List<String> regions = stringList(o, "regions");
                List<String> drops = stringList(o, "drops");
                boolean pet = o.has("pet_capable") && o.get("pet_capable").getAsBoolean();
                String display = str(o, "display");
                if (display.isBlank() && prev != null) {
                    display = prev.display();
                }
                String category = prev == null ? "yaoshou" : prev.category();
                boolean trueSpirit = prev != null && prev.trueSpirit();
                boolean companionOnly = prev != null && prev.companionOnly() || pet && BeastCompanionService.isProtectedCompanion(id);
                BeastEntry next = new BeastEntry(
                        id,
                        display.isBlank() ? id : display,
                        category,
                        prev == null ? tier : prev.threat(),
                        BeastTierService.clampTier(tier),
                        prev == null ? BeastTierService.clampTier(tier) : prev.tierMax(),
                        str(o, "element").isBlank() && prev != null ? prev.element() : str(o, "element"),
                        regions.isEmpty() && prev != null ? prev.regions() : List.copyOf(regions),
                        drops.isEmpty() && prev != null ? prev.drops() : List.copyOf(drops),
                        prev == null ? String.join(",", regions) : prev.habitat(),
                        pet || (prev != null && prev.tameable()),
                        trueSpirit,
                        companionOnly,
                        str(o, "entity_id_hint").isBlank() && prev != null ? prev.entityIdHint() : str(o, "entity_id_hint"));
                byId.put(id, next);
                if (!next.display().isBlank()) {
                    displayToId.put(next.display(), id);
                }
            }
        }

        // Mark companion / true-spirit flags from companion catalog + category.
        for (String companionId : BeastCompanionService.companionIds()) {
            BeastEntry prev = byId.get(companionId);
            if (prev == null) {
                byId.put(companionId, new BeastEntry(
                        companionId, companionId, "lingshou", 3, 1, 13, "",
                        List.of(), List.of(), "", true, false, true, ""));
                continue;
            }
            byId.put(companionId, new BeastEntry(
                    prev.id(), prev.display(), prev.category(), prev.threat(), prev.tier(), prev.tierMax(),
                    prev.element(), prev.regions(), prev.drops(), prev.habitat(), true,
                    prev.trueSpirit() || "zhenling".equals(prev.category()) || "zhenling_bloodline".equals(prev.category()),
                    true, prev.entityIdHint()));
            displayToId.putIfAbsent(prev.display(), companionId);
        }

        return new Snapshot(Collections.unmodifiableMap(byId), Collections.unmodifiableMap(displayToId));
    }

    private static void putCreature(Map<String, BeastEntry> byId, Map<String, String> displayToId, JsonObject o) {
        String id = str(o, "id").toLowerCase(Locale.ROOT);
        if (id.isBlank()) {
            return;
        }
        String category = str(o, "category").toLowerCase(Locale.ROOT);
        int threat = o.has("threat") && o.get("threat").isJsonPrimitive() ? o.get("threat").getAsInt() : 1;
        int[] band = parseTierHint(str(o, "tier_hint"), threat, o);
        boolean trueSpirit = "zhenling".equals(category) || "zhenling_bloodline".equals(category);
        boolean tameable = isTruthy(str(o, "tameable")) || (o.has("pet_capable") && o.get("pet_capable").getAsBoolean());
        boolean companionOnly = BeastCompanionService.isProtectedCompanion(id) || trueSpirit;
        List<String> regions = stringList(o, "regions");
        List<String> drops = stringList(o, "drops");
        String display = str(o, "display");
        if (display.isBlank()) {
            display = id;
        }
        BeastEntry entry = new BeastEntry(
                id,
                display,
                category,
                threat,
                band[0],
                band[1],
                str(o, "element"),
                List.copyOf(regions),
                List.copyOf(drops),
                str(o, "habitat"),
                tameable,
                trueSpirit,
                companionOnly,
                str(o, "entity_id_hint"));
        byId.put(id, entry);
        displayToId.put(display, id);
    }

    private static int[] parseTierHint(String hint, int threat, JsonObject o) {
        if (o.has("tier") && o.get("tier").isJsonPrimitive()) {
            int t = BeastTierService.clampTier(o.get("tier").getAsInt());
            return new int[]{t, t};
        }
        if (hint == null || hint.isBlank() || "null".equalsIgnoreCase(hint) || "none".equalsIgnoreCase(hint)) {
            int t = BeastTierService.clampTier(Math.max(1, threat));
            return new int[]{t, t};
        }
        String h = hint.trim();
        if (h.startsWith("[")) {
            // [5, 8] or [5,8]
            String body = h.replace("[", "").replace("]", "");
            String[] parts = body.split(",");
            try {
                int a = BeastTierService.clampTier(Integer.parseInt(parts[0].trim()));
                int b = parts.length > 1 ? BeastTierService.clampTier(Integer.parseInt(parts[1].trim())) : a;
                return new int[]{Math.min(a, b), Math.max(a, b)};
            } catch (Exception ignored) {
                // fall through
            }
        }
        if (h.matches("\\d+")) {
            int t = BeastTierService.clampTier(Integer.parseInt(h));
            return new int[]{t, t};
        }
        // special / special_growth / DEITY_to_GREAT etc. — high tier companion/true spirit
        if (h.toLowerCase(Locale.ROOT).contains("special") || h.toUpperCase(Locale.ROOT).contains("DEITY")
                || h.toUpperCase(Locale.ROOT).contains("GREAT")) {
            return new int[]{9, 13};
        }
        int t = BeastTierService.clampTier(Math.max(1, threat));
        return new int[]{t, t};
    }

    private static boolean isTruthy(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        String v = value.trim().toLowerCase(Locale.ROOT);
        return "true".equals(v) || "yes".equals(v) || "1".equals(v) || "capture".equals(v) || "tameable".equals(v);
    }

    private static List<String> stringList(JsonObject object, String key) {
        List<String> list = new ArrayList<>();
        if (object == null || !object.has(key) || !object.get(key).isJsonArray()) {
            return list;
        }
        for (JsonElement element : object.getAsJsonArray(key)) {
            try {
                if (element.isJsonPrimitive()) {
                    String v = element.getAsString().trim();
                    if (!v.isBlank()) {
                        list.add(v);
                    }
                }
            } catch (Exception ignored) {
                // skip
            }
        }
        return list;
    }

    private static JsonObject readJson(String path) {
        try (InputStream stream = BeastBestiaryService.class.getClassLoader().getResourceAsStream(path)) {
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
