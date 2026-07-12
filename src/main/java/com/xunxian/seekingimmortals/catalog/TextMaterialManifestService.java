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
import java.util.Map;
import java.util.Optional;

/**
 * Full 文本材料/data bulk runtime inventory.
 * Every source JSON under 文本材料/data is shipped to data/seeking_immortals/text_material/
 * and indexed by this service for commands/tools. Deep gameplay still uses specialized services.
 */
public final class TextMaterialManifestService {
    private static final Snapshot BUILTIN = loadBuiltin();

    private TextMaterialManifestService() {}

    public static Snapshot builtin() {
        return BUILTIN;
    }

    public record FileEntry(String id, String file, int entries, String primaryKey) {}

    public record Snapshot(int catalogFiles, int techniqueFiles, int totalFiles, int totalEntries,
                           Map<String, FileEntry> files) {
        public Optional<FileEntry> find(String id) {
            return Optional.ofNullable(files.get(id == null ? "" : id));
        }

        public boolean contains(String id) {
            return files.containsKey(id == null ? "" : id);
        }

        public List<String> ids() {
            return List.copyOf(files.keySet());
        }
    }

    private static Snapshot loadBuiltin() {
        JsonObject root = readJson("data/" + SeekingImmortalsMod.MODID + "/text_material/manifest.json");
        if (root == null) {
            return new Snapshot(0, 0, 0, 0, Map.of());
        }
        Map<String, FileEntry> files = new LinkedHashMap<>();
        int totalEntries = 0;
        for (JsonElement element : array(root, "files")) {
            if (!element.isJsonObject()) continue;
            JsonObject o = element.getAsJsonObject();
            String id = str(o, "id");
            if (id.isBlank()) continue;
            int entries = asInt(o, "entries");
            totalEntries += Math.max(0, entries);
            files.put(id, new FileEntry(id, str(o, "file"), entries, str(o, "primary_key")));
        }
        return new Snapshot(asInt(root, "catalog_files"), asInt(root, "technique_files"),
                asInt(root, "total_files"), totalEntries, Collections.unmodifiableMap(files));
    }

    private static JsonObject readJson(String path) {
        try (InputStream stream = TextMaterialManifestService.class.getClassLoader().getResourceAsStream(path)) {
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

    private static JsonArray array(JsonObject root, String key) {
        if (root == null || !root.has(key) || !root.get(key).isJsonArray()) {
            return new JsonArray();
        }
        return root.getAsJsonArray(key);
    }

    private static String str(JsonObject object, String key) {
        if (object == null || !object.has(key) || object.get(key).isJsonNull()) {
            return "";
        }
        try {
            return object.get(key).getAsString();
        } catch (Exception ignored) {
            return String.valueOf(object.get(key));
        }
    }

    private static int asInt(JsonObject object, String key) {
        if (object == null || !object.has(key) || !object.get(key).isJsonPrimitive()) {
            return 0;
        }
        try {
            return object.get(key).getAsInt();
        } catch (Exception ignored) {
            return 0;
        }
    }
}
