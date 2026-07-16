package com.xunxian.seekingimmortals.structure;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.xunxian.seekingimmortals.SeekingImmortalsMod;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * M07 data-driven multiblock structure index + station pattern templates.
 * Authoritative count is 86 (v134 entries + 4 v135 war structures).
 */
public final class MultiblockStructureCatalog {
    private static final Snapshot BUILTIN = loadBuiltin();

    private MultiblockStructureCatalog() {}

    public static Snapshot builtin() {
        return BUILTIN;
    }

    public record StructureEntry(
            String id,
            String display,
            String type,
            String dimensions,
            int sizeW,
            int sizeH,
            int sizeD,
            int radius,
            boolean large,
            String defaultState,
            int maxHp,
            StationPattern pattern
    ) {}

    public record StationPattern(
            String validator,
            int tier,
            int radius,
            String ringRole,
            String coreRole
    ) {
        public StationPattern {
            validator = validator == null || validator.isBlank() ? "ring" : validator.trim().toLowerCase(Locale.ROOT);
            tier = Math.max(0, tier);
            radius = Math.max(0, radius);
            ringRole = ringRole == null ? "" : ringRole.trim().toLowerCase(Locale.ROOT);
            coreRole = coreRole == null ? "" : coreRole.trim().toLowerCase(Locale.ROOT);
        }
    }

    public record Snapshot(Map<String, StructureEntry> structures) {
        public Snapshot {
            structures = structures == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(structures));
        }

        public int size() {
            return structures.size();
        }

        public Optional<StructureEntry> find(String id) {
            if (id == null || id.isBlank()) {
                return Optional.empty();
            }
            StructureEntry direct = structures.get(id);
            if (direct != null) {
                return Optional.of(direct);
            }
            String key = id.trim().toLowerCase(Locale.ROOT);
            StructureEntry lower = structures.get(key);
            if (lower != null) {
                return Optional.of(lower);
            }
            for (StructureEntry entry : structures.values()) {
                if (entry.id().equalsIgnoreCase(id)) {
                    return Optional.of(entry);
                }
            }
            return Optional.empty();
        }
    }

    private static Snapshot loadBuiltin() {
        Map<String, StructureEntry> map = new LinkedHashMap<>();
        // Prefer merged runtime index; fall back to station patterns alone.
        JsonObject index = readJson("data/" + SeekingImmortalsMod.MODID + "/text_material/multiblock_structure_index.json");
        JsonObject patternsRoot = readJson("data/" + SeekingImmortalsMod.MODID + "/text_material/multiblock_station_patterns.json");
        Map<String, StationPattern> patterns = new LinkedHashMap<>();
        if (patternsRoot != null) {
            for (JsonElement element : array(patternsRoot, "stations")) {
                if (!element.isJsonObject()) {
                    continue;
                }
                JsonObject o = element.getAsJsonObject();
                String id = str(o, "id");
                if (id.isBlank()) {
                    continue;
                }
                patterns.put(id, parsePattern(o.get("pattern"), o));
            }
        }

        if (index != null) {
            for (JsonElement element : array(index, "entries")) {
                if (!element.isJsonObject()) {
                    continue;
                }
                JsonObject o = element.getAsJsonObject();
                String id = str(o, "id");
                if (id.isBlank()) {
                    continue;
                }
                StationPattern pattern = patterns.getOrDefault(id, parsePattern(null, o));
                int[] size = parseSize(str(o, "dimensions"));
                boolean large = bool(o, "large", size[0] * size[1] * size[2] > 16 * 16 * 16 || Math.max(size[0], size[2]) > 16);
                int radius = Math.max(1, Math.max(size[0], size[2]) / 2);
                if (pattern.radius() > 0) {
                    radius = pattern.radius();
                }
                map.put(id, new StructureEntry(
                        id,
                        str(o, "display").isBlank() ? id : str(o, "display"),
                        str(o, "type").isBlank() ? "structure" : str(o, "type"),
                        str(o, "dimensions"),
                        size[0],
                        size[1],
                        size[2],
                        radius,
                        large,
                        str(o, "default_state").isBlank() ? "intact" : str(o, "default_state"),
                        Math.max(1, intVal(o, "max_hp", 100)),
                        pattern
                ));
            }
        }

        // Ensure every station pattern is present even if index missed it.
        for (Map.Entry<String, StationPattern> e : patterns.entrySet()) {
            if (map.containsKey(e.getKey())) {
                continue;
            }
            map.put(e.getKey(), new StructureEntry(
                    e.getKey(), e.getKey(), "structure", "", 3, 1, 3,
                    Math.max(1, e.getValue().radius()), false, "intact", 100, e.getValue()));
        }
        return new Snapshot(map);
    }

    private static StationPattern parsePattern(JsonElement patternEl, JsonObject stationOrEntry) {
        if (patternEl != null && patternEl.isJsonObject()) {
            JsonObject p = patternEl.getAsJsonObject();
            return new StationPattern(
                    str(p, "validator"),
                    intVal(p, "tier", 0),
                    intVal(p, "radius", 0),
                    str(p, "ring_role"),
                    str(p, "core_role")
            );
        }
        String type = str(stationOrEntry, "type");
        int[] size = parseSize(str(stationOrEntry, "dimensions"));
        int radius = Math.max(1, Math.max(size[0], size[2]) / 2);
        if ("single_block".equals(type) || "ore".equals(type) || "crop_block".equals(type) || "utility_block".equals(type)) {
            return new StationPattern("single_core", 0, 0, "", "any_solid");
        }
        if ("array_block".equals(type)) {
            return new StationPattern("ring", 0, radius, "spirit_gathering_array", "");
        }
        return new StationPattern("ring", 0, Math.min(radius, 4), "spirit_ore", "");
    }

    /** Parse Chinese/ASCII dimension strings like 3×5×2 or 1x1x2 into w,h,d. */
    static int[] parseSize(String dimensions) {
        if (dimensions == null || dimensions.isBlank()) {
            return new int[] {3, 1, 3};
        }
        String normalized = dimensions.replace('×', 'x').replace('X', 'x');
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d+)\\s*x\\s*(\\d+)\\s*x\\s*(\\d+)").matcher(normalized);
        if (m.find()) {
            int a = Integer.parseInt(m.group(1));
            int b = Integer.parseInt(m.group(2));
            int c = Integer.parseInt(m.group(3));
            // Corpus usually 宽×深×高; store as w,h,d = a,c,b
            return new int[] {Math.max(1, a), Math.max(1, c), Math.max(1, b)};
        }
        m = java.util.regex.Pattern.compile("(\\d+)\\s*x\\s*(\\d+)").matcher(normalized);
        if (m.find()) {
            int a = Integer.parseInt(m.group(1));
            int b = Integer.parseInt(m.group(2));
            return new int[] {Math.max(1, a), 1, Math.max(1, b)};
        }
        return new int[] {3, 1, 3};
    }

    private static JsonObject readJson(String path) {
        try (InputStream in = MultiblockStructureCatalog.class.getClassLoader().getResourceAsStream(path)) {
            if (in == null) {
                return null;
            }
            JsonElement root = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            return root != null && root.isJsonObject() ? root.getAsJsonObject() : null;
        } catch (Exception e) {
            SeekingImmortalsMod.LOGGER.warn("Failed loading multiblock catalog {}", path, e);
            return null;
        }
    }

    private static JsonArray array(JsonObject root, String key) {
        if (root == null || !root.has(key) || !root.get(key).isJsonArray()) {
            return new JsonArray();
        }
        return root.getAsJsonArray(key);
    }

    private static String str(JsonObject o, String key) {
        if (o == null || !o.has(key) || o.get(key).isJsonNull()) {
            return "";
        }
        try {
            return o.get(key).getAsString().trim();
        } catch (RuntimeException ignored) {
            return "";
        }
    }

    private static int intVal(JsonObject o, String key, int def) {
        if (o == null || !o.has(key) || o.get(key).isJsonNull()) {
            return def;
        }
        try {
            return o.get(key).getAsInt();
        } catch (RuntimeException ignored) {
            return def;
        }
    }

    private static boolean bool(JsonObject o, String key, boolean def) {
        if (o == null || !o.has(key) || o.get(key).isJsonNull()) {
            return def;
        }
        try {
            return o.get(key).getAsBoolean();
        } catch (RuntimeException ignored) {
            return def;
        }
    }
}
