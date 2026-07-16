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
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * 突破基础成功率目录，对齐 {@code realm_breakthrough_v98.json}。
 * <p>红线：本服务只提供成功率/描述数据，不负责消耗剧情唯一道具。</p>
 */
public final class BreakthroughCatalog {
    private static final Snapshot BUILTIN = loadBuiltin();

    private BreakthroughCatalog() {}

    public static Snapshot builtin() {
        return BUILTIN;
    }

    public record Entry(String id, String from, String to, String display, double baseSuccess) {}

    public record Snapshot(Map<String, Entry> byId, Map<String, Double> baseByTransition) {
        public Optional<Entry> find(String id) {
            if (id == null || id.isBlank()) return Optional.empty();
            return Optional.ofNullable(byId.get(id.trim().toLowerCase(Locale.ROOT)));
        }

        public double baseSuccess(Realm currentRealm, boolean realmCrossing) {
            if (currentRealm == null) return 0.10D;
            if (!realmCrossing) {
                // 小境界推进
                return switch (currentRealm) {
                    case MORTAL -> 0.70D;
                    case QI_REFINING -> 0.85D;
                    case FOUNDATION_ESTABLISHMENT -> 0.75D;
                    case CORE_FORMATION -> 0.70D;
                    case NASCENT_SOUL -> 0.60D;
                    case SOUL_TRANSFORMATION -> 0.50D;
                    case VOID_REFINEMENT -> 0.45D;
                    case UNITY -> 0.40D;
                    case MAHAYANA -> 0.35D;
                    case TRIBULATION -> 0.20D;
                    case TRUE_IMMORTAL -> 0.10D;
                };
            }
            // 大境界跨越：from 当前 → next
            String key = currentRealm.name() + "->" + currentRealm.next().name();
            Double mapped = baseByTransition.get(key);
            if (mapped != null) return mapped;
            // design-id 键
            String designKey = currentRealm.getDesignId() + "->" + currentRealm.next().getDesignId();
            mapped = baseByTransition.get(designKey.toUpperCase(Locale.ROOT));
            if (mapped != null) return mapped;
            return switch (currentRealm) {
                case MORTAL -> 0.70D;
                case QI_REFINING -> 0.35D;
                case FOUNDATION_ESTABLISHMENT -> 0.20D;
                case CORE_FORMATION -> 0.15D;
                case NASCENT_SOUL -> 0.08D;
                case SOUL_TRANSFORMATION -> 0.12D;
                case VOID_REFINEMENT -> 0.10D;
                case UNITY -> 0.06D;
                case MAHAYANA, TRIBULATION -> 0.03D;
                case TRUE_IMMORTAL -> 0.01D;
            };
        }
    }

    public static double baseSuccess(PlayerCultivation cultivation) {
        if (cultivation == null) return 0.10D;
        Realm current = cultivation.getRealm();
        Realm nextRealm = cultivation.getNextBreakthroughRealm();
        boolean crossing = nextRealm != null && nextRealm != current;
        return builtin().baseSuccess(current, crossing);
    }

    private static Snapshot loadBuiltin() {
        Map<String, Entry> byId = new LinkedHashMap<>();
        Map<String, Double> transitions = new LinkedHashMap<>();
        JsonObject root = readJson("data/" + SeekingImmortalsMod.MODID + "/text_material/realm_breakthrough_v98.json");
        if (root != null && root.has("breakthroughs") && root.get("breakthroughs").isJsonArray()) {
            JsonArray array = root.getAsJsonArray("breakthroughs");
            for (JsonElement element : array) {
                if (!element.isJsonObject()) continue;
                JsonObject o = element.getAsJsonObject();
                String id = str(o, "id").toLowerCase(Locale.ROOT);
                String from = str(o, "from");
                String to = str(o, "to");
                double base = o.has("base_success") && o.get("base_success").isJsonPrimitive()
                        ? o.get("base_success").getAsDouble() : 0.0D;
                Entry entry = new Entry(id, from, to, str(o, "display"), base);
                if (!id.isBlank()) byId.put(id, entry);
                registerTransition(transitions, from, to, base);
            }
        }
        // 明确大境界跨越映射（即使 JSON 缺字段也保证）
        putTransition(transitions, "MORTAL", "QI_REFINING", 0.70D);
        putTransition(transitions, "QI_REFINING", "FOUNDATION", 0.35D);
        putTransition(transitions, "QI_REFINING", "FOUNDATION_ESTABLISHMENT", 0.35D);
        putTransition(transitions, "FOUNDATION", "CORE_FORMATION", 0.20D);
        putTransition(transitions, "FOUNDATION_ESTABLISHMENT", "CORE_FORMATION", 0.20D);
        putTransition(transitions, "CORE_FORMATION", "NASCENT_SOUL", 0.15D);
        putTransition(transitions, "NASCENT_SOUL", "DEITY_TRANSFORMATION", 0.08D);
        putTransition(transitions, "NASCENT_SOUL", "SOUL_TRANSFORMATION", 0.08D);
        putTransition(transitions, "DEITY_TRANSFORMATION", "VOID_REFINEMENT", 0.12D);
        putTransition(transitions, "SOUL_TRANSFORMATION", "VOID_REFINEMENT", 0.12D);
        putTransition(transitions, "VOID_REFINEMENT", "BODY_INTEGRATION", 0.10D);
        putTransition(transitions, "VOID_REFINEMENT", "UNITY", 0.10D);
        putTransition(transitions, "BODY_INTEGRATION", "GREAT_VEHICLE", 0.06D);
        putTransition(transitions, "UNITY", "MAHAYANA", 0.06D);
        putTransition(transitions, "GREAT_VEHICLE", "TRUE_IMMORTAL", 0.03D);
        putTransition(transitions, "MAHAYANA", "TRIBULATION", 0.05D);
        putTransition(transitions, "MAHAYANA", "TRUE_IMMORTAL", 0.03D);
        putTransition(transitions, "TRIBULATION", "TRUE_IMMORTAL", 0.03D);
        putTransition(transitions, "TRIBULATION_LAND", "TRUE_IMMORTAL", 0.03D);
        return new Snapshot(Collections.unmodifiableMap(byId), Collections.unmodifiableMap(transitions));
    }

    private static void registerTransition(Map<String, Double> map, String from, String to, double base) {
        if (from == null || to == null || from.isBlank() || to.isBlank() || base <= 0.0D) return;
        String f = from.trim().toUpperCase(Locale.ROOT);
        String t = to.trim().toUpperCase(Locale.ROOT);
        // 忽略层内伪 from/to
        if (f.contains("_N") || t.contains("_N") || f.contains("EARLY") || f.contains("LATE")
                || f.contains("PEAK") || t.contains("EARLY") || t.contains("LATE") || t.contains("PEAK")
                || f.contains("MID")) {
            // 大境界跨越条目仍可能带 PEAK/LATE 后缀
            Realm fromRealm = extractRealm(f);
            Realm toRealm = extractRealm(t);
            if (fromRealm != null && toRealm != null && fromRealm != toRealm) {
                putTransition(map, fromRealm.name(), toRealm.name(), base);
                putTransition(map, fromRealm.getDesignId(), toRealm.getDesignId(), base);
            }
            return;
        }
        putTransition(map, f, t, base);
        Realm fromRealm = Realm.fromDesignId(f);
        Realm toRealm = Realm.fromDesignId(t);
        if (fromRealm != null && toRealm != null) {
            putTransition(map, fromRealm.name(), toRealm.name(), base);
            putTransition(map, fromRealm.getDesignId(), toRealm.getDesignId(), base);
        }
    }

    private static Realm extractRealm(String token) {
        if (token == null || token.isBlank()) return null;
        String t = token.toUpperCase(Locale.ROOT);
        for (String candidate : new String[] {
                "TRUE_IMMORTAL", "TRIBULATION_LAND", "TRIBULATION", "GREAT_VEHICLE", "MAHAYANA",
                "BODY_INTEGRATION", "UNITY", "VOID_REFINEMENT", "DEITY_TRANSFORMATION", "SOUL_TRANSFORMATION",
                "NASCENT_SOUL", "CORE_FORMATION", "FOUNDATION_ESTABLISHMENT", "FOUNDATION", "QI_REFINING", "MORTAL"
        }) {
            if (t.startsWith(candidate) || t.contains(candidate)) {
                return Realm.fromDesignId(candidate);
            }
        }
        return Realm.fromDesignId(t);
    }

    private static void putTransition(Map<String, Double> map, String from, String to, double base) {
        if (from == null || to == null) return;
        map.put(from.toUpperCase(Locale.ROOT) + "->" + to.toUpperCase(Locale.ROOT), base);
    }

    private static JsonObject readJson(String path) {
        try (InputStream stream = BreakthroughCatalog.class.getClassLoader().getResourceAsStream(path)) {
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
}
