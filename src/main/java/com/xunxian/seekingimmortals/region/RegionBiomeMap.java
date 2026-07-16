package com.xunxian.seekingimmortals.region;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.xunxian.seekingimmortals.SeekingImmortalsMod;
import net.minecraft.resources.ResourceLocation;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
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
 * worldgen_biomes → biome/dimension binding table for region resolution.
 * Dimension ids come from M13 when present; string constants are used until then.
 */
public final class RegionBiomeMap {
    private static final Snapshot BUILTIN = loadBuiltin();

    private RegionBiomeMap() {}

    public static Snapshot builtin() {
        return BUILTIN;
    }

    public record BiomeBinding(String biomeId, String regionId, String display) {
        public BiomeBinding {
            biomeId = biomeId == null ? "" : biomeId;
            regionId = regionId == null ? "" : regionId;
            display = display == null ? "" : display;
        }
    }

    public record Snapshot(List<BiomeBinding> bindings,
                           Map<String, String> biomeToRegion,
                           Map<String, List<String>> regionToBiomes) {
        public Snapshot {
            bindings = bindings == null ? List.of() : List.copyOf(bindings);
            biomeToRegion = biomeToRegion == null
                    ? Map.of()
                    : Collections.unmodifiableMap(new LinkedHashMap<>(biomeToRegion));
            Map<String, List<String>> copy = new LinkedHashMap<>();
            if (regionToBiomes != null) {
                regionToBiomes.forEach((key, value) -> copy.put(key, List.copyOf(value)));
            }
            regionToBiomes = Collections.unmodifiableMap(copy);
        }

        public Optional<String> regionForBiome(String biomeId) {
            if (biomeId == null || biomeId.isBlank()) {
                return Optional.empty();
            }
            String direct = biomeToRegion.get(biomeId);
            if (direct != null) {
                return Optional.of(direct);
            }
            String key = biomeId.trim().toLowerCase(Locale.ROOT);
            for (Map.Entry<String, String> entry : biomeToRegion.entrySet()) {
                if (entry.getKey().equalsIgnoreCase(key)
                        || entry.getKey().endsWith(":" + key)
                        || key.endsWith(entry.getKey())) {
                    return Optional.of(entry.getValue());
                }
            }
            // path-only match: seeking_immortals:tiannan_forest → tiannan via prefix
            int colon = key.indexOf(':');
            String path = colon >= 0 ? key.substring(colon + 1) : key;
            for (Map.Entry<String, String> entry : biomeToRegion.entrySet()) {
                String candidate = entry.getKey();
                int c = candidate.indexOf(':');
                String candidatePath = c >= 0 ? candidate.substring(c + 1) : candidate;
                if (path.startsWith(entry.getValue() + "_") || path.equals(entry.getValue())
                        || candidatePath.equals(path)) {
                    return Optional.of(entry.getValue());
                }
            }
            return Optional.empty();
        }

        public Optional<String> regionForBiome(ResourceLocation biomeId) {
            return biomeId == null ? Optional.empty() : regionForBiome(biomeId.toString());
        }

        public List<String> biomesForRegion(String regionId) {
            if (regionId == null || regionId.isBlank()) {
                return List.of();
            }
            List<String> direct = regionToBiomes.get(regionId);
            if (direct != null) {
                return direct;
            }
            for (Map.Entry<String, List<String>> entry : regionToBiomes.entrySet()) {
                if (entry.getKey().equalsIgnoreCase(regionId)) {
                    return entry.getValue();
                }
            }
            return List.of();
        }

        public int bindingCount() {
            return bindings.size();
        }
    }

    private static Snapshot loadBuiltin() {
        List<BiomeBinding> bindings = new ArrayList<>();
        Map<String, String> biomeToRegion = new LinkedHashMap<>();
        Map<String, List<String>> regionToBiomes = new LinkedHashMap<>();
        JsonObject root = readJson("data/" + SeekingImmortalsMod.MODID + "/text_material/worldgen_biomes.json");
        if (root == null) {
            return new Snapshot(bindings, biomeToRegion, regionToBiomes);
        }
        for (JsonElement element : array(root, "biomes")) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject object = element.getAsJsonObject();
            String biomeId = str(object, "id");
            String regionId = str(object, "region");
            if (biomeId.isBlank() || regionId.isBlank()) {
                continue;
            }
            BiomeBinding binding = new BiomeBinding(biomeId, regionId, str(object, "display"));
            bindings.add(binding);
            biomeToRegion.putIfAbsent(biomeId, regionId);
            regionToBiomes.computeIfAbsent(regionId, ignored -> new ArrayList<>()).add(biomeId);
        }
        // freeze lists
        Map<String, List<String>> frozen = new LinkedHashMap<>();
        regionToBiomes.forEach((key, value) -> frozen.put(key, List.copyOf(new LinkedHashSet<>(value))));
        return new Snapshot(bindings, biomeToRegion, frozen);
    }

    private static JsonObject readJson(String path) {
        try (InputStream stream = RegionBiomeMap.class.getClassLoader().getResourceAsStream(path)) {
            if (stream == null) {
                SeekingImmortalsMod.LOGGER.warn("Missing biome map resource {}", path);
                return null;
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                return JsonParser.parseReader(reader).getAsJsonObject();
            }
        } catch (Exception exception) {
            SeekingImmortalsMod.LOGGER.warn("Failed to load biome map resource {}", path, exception);
            return null;
        }
    }

    private static JsonArray array(JsonObject object, String key) {
        return object != null && object.has(key) && object.get(key).isJsonArray()
                ? object.getAsJsonArray(key) : new JsonArray();
    }

    private static String str(JsonObject object, String key) {
        return object != null && object.has(key) && object.get(key).isJsonPrimitive()
                ? object.get(key).getAsString() : "";
    }
}
