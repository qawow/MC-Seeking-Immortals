package com.xunxian.seekingimmortals.catalog;

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
 * Generic loader for bulk *_index.json files under data/seeking_immortals/catalog.
 */
public final class BulkCatalogIndexService {
    private static final Snapshot BUILTIN = loadBuiltin();

    private BulkCatalogIndexService() {}

    public static Snapshot builtin() {
        return BUILTIN;
    }

    public record Entry(String id, String display) {}
    public record IndexFile(String file, String source, String primaryKey, Map<String, Entry> entries) {
        public int size() { return entries.size(); }
    }

    public record Snapshot(Map<String, IndexFile> indexes) {
        public int fileCount() { return indexes.size(); }
        public int totalEntries() {
            int total = 0;
            for (IndexFile file : indexes.values()) total += file.size();
            return total;
        }
        public Optional<IndexFile> find(String name) {
            if (name == null) return Optional.empty();
            String key = name.trim().toLowerCase(Locale.ROOT);
            if (indexes.containsKey(key)) return Optional.of(indexes.get(key));
            if (!key.endsWith("_index") && indexes.containsKey(key + "_index")) {
                return Optional.of(indexes.get(key + "_index"));
            }
            return Optional.empty();
        }
        public List<String> sampleFiles(int limit) {
            List<String> list = new ArrayList<>();
            int i = 0;
            for (IndexFile file : indexes.values()) {
                list.add(file.file() + " | entries=" + file.size() + " | key=" + file.primaryKey());
                if (++i >= Math.max(1, limit)) break;
            }
            return list;
        }
    }

    private static Snapshot loadBuiltin() {
        Map<String, IndexFile> map = new LinkedHashMap<>();
        // Load from text_material manifest catalog list is heavy; load known bulk indexes by probing common names via classloader is hard.
        // Instead, load the directory listing from a generated bulk_manifest.
        JsonObject root = readJson("data/" + SeekingImmortalsMod.MODID + "/catalog/bulk_index_manifest.json");
        if (root == null) {
            return new Snapshot(Map.of());
        }
        JsonArray files = root.has("files") && root.get("files").isJsonArray() ? root.getAsJsonArray("files") : new JsonArray();
        for (JsonElement element : files) {
            String file = "";
            try { file = element.getAsString(); } catch (Exception ignored) { continue; }
            if (file.isBlank()) continue;
            JsonObject indexRoot = readJson("data/" + SeekingImmortalsMod.MODID + "/catalog/" + file);
            if (indexRoot == null) continue;
            Map<String, Entry> entries = new LinkedHashMap<>();
            JsonArray arr = indexRoot.has("entries") && indexRoot.get("entries").isJsonArray()
                    ? indexRoot.getAsJsonArray("entries") : new JsonArray();
            for (JsonElement e : arr) {
                if (!e.isJsonObject()) continue;
                JsonObject o = e.getAsJsonObject();
                String id = str(o, "id");
                if (id.isBlank()) continue;
                entries.put(id, new Entry(id, str(o, "display")));
            }
            String stem = file.endsWith(".json") ? file.substring(0, file.length() - 5) : file;
            map.put(stem.toLowerCase(Locale.ROOT), new IndexFile(file, str(indexRoot, "source"), str(indexRoot, "primary_key"),
                    Collections.unmodifiableMap(entries)));
        }
        return new Snapshot(Collections.unmodifiableMap(map));
    }

    private static JsonObject readJson(String path) {
        try (InputStream stream = BulkCatalogIndexService.class.getClassLoader().getResourceAsStream(path)) {
            if (stream == null) return null;
            try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                return JsonParser.parseReader(reader).getAsJsonObject();
            }
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String str(JsonObject o, String key) {
        if (o == null || !o.has(key) || o.get(key).isJsonNull()) return "";
        try { return o.get(key).getAsString(); } catch (Exception ignored) { return String.valueOf(o.get(key)); }
    }
}
