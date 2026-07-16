package com.xunxian.seekingimmortals.region;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.xunxian.seekingimmortals.SeekingImmortalsMod;

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
 * items_by_region → itemsForRegion(regionId) for M05 route pricing, M09 loot, M16 codex.
 */
public final class RegionItemsService {
    private static final Snapshot BUILTIN = loadBuiltin();

    private RegionItemsService() {}

    public static Snapshot builtin() {
        return BUILTIN;
    }

    public static List<RegionItem> itemsForRegion(String regionId) {
        return BUILTIN.itemsForRegion(regionId);
    }

    public record RegionItem(String id, String display, String category, String rarity) {
        public RegionItem {
            id = id == null ? "" : id;
            display = display == null ? "" : display;
            category = category == null ? "" : category;
            rarity = rarity == null ? "" : rarity;
        }
    }

    public record RegionBundle(String regionId, String display, List<RegionItem> items) {
        public RegionBundle {
            regionId = regionId == null ? "" : regionId;
            display = display == null ? "" : display;
            items = items == null ? List.of() : List.copyOf(items);
        }
    }

    public record Snapshot(Map<String, RegionBundle> byRegion) {
        public Snapshot {
            byRegion = byRegion == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(byRegion));
        }

        public Optional<RegionBundle> find(String regionId) {
            if (regionId == null || regionId.isBlank()) {
                return Optional.empty();
            }
            RegionBundle direct = byRegion.get(regionId);
            if (direct != null) {
                return Optional.of(direct);
            }
            String key = regionId.trim().toLowerCase(Locale.ROOT);
            for (Map.Entry<String, RegionBundle> entry : byRegion.entrySet()) {
                if (entry.getKey().equalsIgnoreCase(key)) {
                    return Optional.of(entry.getValue());
                }
            }
            return Optional.empty();
        }

        public List<RegionItem> itemsForRegion(String regionId) {
            return find(regionId).map(RegionBundle::items).orElse(List.of());
        }

        public Set<String> regionIds() {
            return byRegion.keySet();
        }

        public int regionCount() {
            return byRegion.size();
        }
    }

    private static Snapshot loadBuiltin() {
        Map<String, RegionBundle> map = new LinkedHashMap<>();
        JsonObject root = readJson("data/" + SeekingImmortalsMod.MODID + "/text_material/items_by_region.json");
        if (root == null) {
            return new Snapshot(map);
        }
        for (JsonElement element : array(root, "regions")) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject object = element.getAsJsonObject();
            String id = str(object, "id");
            if (id.isBlank()) {
                continue;
            }
            List<RegionItem> items = new ArrayList<>();
            appendCategory(items, object, "herbs", "herb");
            appendCategory(items, object, "ores", "ore");
            appendCategory(items, object, "materials", "material");
            appendCategory(items, object, "artifacts", "artifact");
            appendCategory(items, object, "pills", "pill");
            appendCategory(items, object, "talismans", "talisman");
            // de-dupe by item id while preserving first category.
            Map<String, RegionItem> unique = new LinkedHashMap<>();
            for (RegionItem item : items) {
                unique.putIfAbsent(item.id(), item);
            }
            map.put(id, new RegionBundle(id, str(object, "display"), List.copyOf(unique.values())));
        }
        return new Snapshot(map);
    }

    private static void appendCategory(List<RegionItem> out, JsonObject region, String key, String category) {
        for (JsonElement element : array(region, key)) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject object = element.getAsJsonObject();
            String id = str(object, "id");
            if (id.isBlank()) {
                continue;
            }
            out.add(new RegionItem(id, str(object, "display"), category, str(object, "rarity")));
        }
        // secret_realm_drops may be bare strings
        if ("materials".equals(category)) {
            for (JsonElement element : array(region, "secret_realm_drops")) {
                if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
                    String id = element.getAsString();
                    if (!id.isBlank()) {
                        out.add(new RegionItem(id, id, "secret_realm_drop", "regional"));
                    }
                }
            }
        }
    }

    private static JsonObject readJson(String path) {
        try (InputStream stream = RegionItemsService.class.getClassLoader().getResourceAsStream(path)) {
            if (stream == null) {
                SeekingImmortalsMod.LOGGER.warn("Missing region items resource {}", path);
                return null;
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                return JsonParser.parseReader(reader).getAsJsonObject();
            }
        } catch (Exception exception) {
            SeekingImmortalsMod.LOGGER.warn("Failed to load region items resource {}", path, exception);
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
