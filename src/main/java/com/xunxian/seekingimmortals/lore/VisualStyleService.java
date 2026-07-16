package com.xunxian.seekingimmortals.lore;

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

/**
 * M16: visual style bible summary (v118 palette + v122 look-card counts) for encyclopedia pages.
 */
public final class VisualStyleService {
    private static final Snapshot BUILTIN = loadBuiltin();

    private VisualStyleService() {}

    public record Snapshot(
            String styleGuideId,
            Map<String, String> palette,
            Map<String, String> fieldSchema,
            Map<String, Integer> styleCounts,
            Map<String, Integer> lookCardCounts,
            String description,
            boolean present) {
        public List<String> paletteLines() {
            List<String> lines = new ArrayList<>();
            palette.forEach((k, v) -> lines.add(k + ": " + v));
            return lines;
        }

        public List<String> countLines() {
            List<String> lines = new ArrayList<>();
            styleCounts.forEach((k, v) -> lines.add("style." + k + "=" + v));
            lookCardCounts.forEach((k, v) -> lines.add("look." + k + "=" + v));
            return lines;
        }
    }

    public static Snapshot builtin() {
        return BUILTIN;
    }

    public static boolean present() {
        return BUILTIN.present();
    }

    private static Snapshot loadBuiltin() {
        JsonObject style = readJson("data/" + SeekingImmortalsMod.MODID + "/text_material/visual_style_v118.json");
        JsonObject look = readJson("data/" + SeekingImmortalsMod.MODID + "/text_material/visual_look_cards_v122.json");
        if (style == null && look == null) {
            return new Snapshot("", Map.of(), Map.of(), Map.of(), Map.of(), "", false);
        }
        Map<String, String> palette = style == null ? Map.of() : stringifyMap(asObject(style.get("palette")));
        Map<String, String> schema = style == null ? Map.of() : stringifyMap(asObject(style.get("field_schema")));
        Map<String, Integer> styleCounts = style == null ? Map.of() : intMap(asObject(style.get("counts")));
        Map<String, Integer> lookCounts = look == null ? Map.of() : intMap(asObject(look.get("counts")));
        String id = style == null ? "" : str(style, "style_guide_id");
        String desc = style == null ? "" : str(style, "description");
        if (look != null && !str(look, "description").isBlank()) {
            desc = desc.isBlank() ? str(look, "description") : desc + " | " + str(look, "description");
        }
        return new Snapshot(id, palette, schema, styleCounts, lookCounts, desc, true);
    }

    private static JsonObject asObject(JsonElement element) {
        return element != null && element.isJsonObject() ? element.getAsJsonObject() : null;
    }

    private static Map<String, String> stringifyMap(JsonObject object) {
        if (object == null) {
            return Map.of();
        }
        Map<String, String> map = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> e : object.entrySet()) {
            if (e.getValue() == null || e.getValue().isJsonNull()) {
                continue;
            }
            if (e.getValue().isJsonPrimitive()) {
                map.put(e.getKey(), e.getValue().getAsString());
            } else {
                map.put(e.getKey(), e.getValue().toString());
            }
        }
        return Collections.unmodifiableMap(map);
    }

    private static Map<String, Integer> intMap(JsonObject object) {
        if (object == null) {
            return Map.of();
        }
        Map<String, Integer> map = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> e : object.entrySet()) {
            try {
                if (e.getValue().isJsonPrimitive() && e.getValue().getAsJsonPrimitive().isNumber()) {
                    map.put(e.getKey(), e.getValue().getAsInt());
                }
            } catch (Exception ignored) {
                // skip
            }
        }
        return Collections.unmodifiableMap(map);
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

    private static JsonObject readJson(String path) {
        try (InputStream stream = VisualStyleService.class.getClassLoader().getResourceAsStream(path)) {
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
}
