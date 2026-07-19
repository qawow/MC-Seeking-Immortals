package com.xunxian.seekingimmortals.structure;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.xunxian.seekingimmortals.SeekingImmortalsMod;
import com.xunxian.seekingimmortals.catalog.ItemCatalogService;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Structure material lists from multiblock_material_prices_v135 used_by_sample.
 */
public final class MultiblockMaterialCatalog {
    private static final Snapshot BUILTIN = loadBuiltin();

    private MultiblockMaterialCatalog() {}

    public static Snapshot builtin() {
        return BUILTIN;
    }

    public record MaterialRef(String id, String display, int minPrice, int maxPrice) {}

    public record Snapshot(Map<String, List<MaterialRef>> byStructure) {
        public Snapshot {
            Map<String, List<MaterialRef>> copy = new LinkedHashMap<>();
            if (byStructure != null) {
                byStructure.forEach((k, v) -> copy.put(k, List.copyOf(v == null ? List.of() : v)));
            }
            byStructure = Collections.unmodifiableMap(copy);
        }

        public List<MaterialRef> materialsFor(String stationId) {
            if (stationId == null || stationId.isBlank()) {
                return List.of();
            }
            return byStructure.getOrDefault(stationId.trim().toLowerCase(Locale.ROOT), List.of());
        }

        public int structureCount() {
            return byStructure.size();
        }
    }

    /** Resolve authored material ids into concrete items when possible. */
    public static List<Item> resolveItems(String stationId) {
        List<Item> items = new ArrayList<>();
        for (MaterialRef ref : BUILTIN.materialsFor(stationId)) {
            Item item = ItemCatalogService.resolveCatalogItem(ref.id());
            if (item != null && item != Items.AIR) {
                items.add(item);
            }
        }
        return List.copyOf(items);
    }

    private static Snapshot loadBuiltin() {
        Map<String, List<MaterialRef>> map = new LinkedHashMap<>();
        JsonObject root = readJson("data/" + SeekingImmortalsMod.MODID
                + "/text_material/multiblock_material_prices_v135.json");
        if (root == null) {
            return new Snapshot(map);
        }
        JsonArray materials = root.has("materials") && root.get("materials").isJsonArray()
                ? root.getAsJsonArray("materials") : new JsonArray();
        for (JsonElement element : materials) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject o = element.getAsJsonObject();
            String id = str(o, "id");
            if (id.isBlank()) {
                continue;
            }
            String display = str(o, "display");
            int min = 5;
            int max = 80;
            if (o.has("price_spirit_stone_low") && o.get("price_spirit_stone_low").isJsonObject()) {
                JsonObject price = o.getAsJsonObject("price_spirit_stone_low");
                min = Math.max(0, intVal(price, "min", min));
                max = Math.max(min, intVal(price, "max", max));
            }
            MaterialRef ref = new MaterialRef(id, display.isBlank() ? id : display, min, max);
            if (o.has("used_by_sample") && o.get("used_by_sample").isJsonArray()) {
                for (JsonElement sample : o.getAsJsonArray("used_by_sample")) {
                    String station = sample.isJsonPrimitive() ? sample.getAsString() : "";
                    if (station == null || station.isBlank()) {
                        continue;
                    }
                    String key = station.trim().toLowerCase(Locale.ROOT);
                    map.computeIfAbsent(key, ignored -> new ArrayList<>()).add(ref);
                }
            }
        }
        return new Snapshot(map);
    }

    private static JsonObject readJson(String path) {
        try (InputStream in = MultiblockMaterialCatalog.class.getClassLoader().getResourceAsStream(path)) {
            if (in == null) {
                return null;
            }
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String str(JsonObject o, String key) {
        return o != null && o.has(key) && o.get(key).isJsonPrimitive() ? o.get(key).getAsString() : "";
    }

    private static int intVal(JsonObject o, String key, int fallback) {
        try {
            return o != null && o.has(key) && o.get(key).isJsonPrimitive() ? o.get(key).getAsInt() : fallback;
        } catch (Exception ignored) {
            return fallback;
        }
    }
}
