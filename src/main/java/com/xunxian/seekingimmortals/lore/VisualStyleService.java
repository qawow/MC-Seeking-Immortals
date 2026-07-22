package com.xunxian.seekingimmortals.lore;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.xunxian.seekingimmortals.SeekingImmortalsMod;
import com.xunxian.seekingimmortals.util.PlayerDisplayText;
import net.minecraft.network.chat.Component;

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
    private static final Map<String, String> PALETTE_TONES = Map.ofEntries(
            Map.entry("qi", "清透冰青"),
            Map.entry("fire", "赤焰"),
            Map.entry("water", "澄蓝"),
            Map.entry("wood", "青碧"),
            Map.entry("metal", "淡金"),
            Map.entry("earth", "岩褐"),
            Map.entry("thunder", "紫电"),
            Map.entry("yin", "幽紫"),
            Map.entry("heal", "翠青"),
            Map.entry("poison", "碧绿"),
            Map.entry("soul", "淡紫"));
    private static final Map<String, String> DISPLAY_COUNT_KEYS = Map.of(
            "pills", "pills",
            "herbs", "herbs",
            "materials", "materials",
            "artifacts", "artifacts",
            "consumables", "consumables",
            "techniques", "techniques",
            "methods", "methods",
            "beasts", "beasts",
            "npcs", "npcs");
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
            palette.forEach((key, value) -> lines.add(Component.translatable(
                    "screen.seeking_immortals.compendium.visual.palette_entry",
                    PlayerDisplayText.translatedOr(
                            "screen.seeking_immortals.compendium.visual.palette." + key,
                            "screen.seeking_immortals.compendium.visual.unknown_category"),
                    PALETTE_TONES.getOrDefault(key, "")).getString()));
            return lines;
        }

        public List<String> countLines() {
            List<String> lines = new ArrayList<>();
            Map<String, Integer> merged = new LinkedHashMap<>();
            mergeKnownCounts(merged, styleCounts);
            mergeKnownCounts(merged, lookCardCounts);
            for (Map.Entry<String, Integer> entry : merged.entrySet()) {
                lines.add(countLine(entry.getKey(), entry.getValue()));
            }
            int total = Math.max(value(styleCounts, "total_item_entries"), value(lookCardCounts, "entries"));
            if (total > 0) {
                lines.add(countLine("total_item_entries", total));
            }
            return lines;
        }

        /** Authored descriptions may contain file names and version markers; hide those from players. */
        public String displayDescription() {
            return safeChinese(description) ? description.trim() : "";
        }

        private static void mergeKnownCounts(Map<String, Integer> target, Map<String, Integer> source) {
            for (Map.Entry<String, Integer> entry : source.entrySet()) {
                if (DISPLAY_COUNT_KEYS.containsKey(entry.getKey())) {
                    target.merge(entry.getKey(), Math.max(0, entry.getValue()), Math::max);
                }
            }
        }

        private static int value(Map<String, Integer> values, String key) {
            return values == null ? 0 : Math.max(0, values.getOrDefault(key, 0));
        }

        private static String countLine(String key, int value) {
            return Component.translatable(
                    "screen.seeking_immortals.compendium.visual.style_count",
                    PlayerDisplayText.translatedOr(
                            "screen.seeking_immortals.compendium.visual.count." + key,
                            "screen.seeking_immortals.compendium.visual.unknown_category"),
                    Math.max(0, value)).getString();
        }
    }

    public static Snapshot builtin() {
        return BUILTIN;
    }

    public static boolean present() {
        return BUILTIN.present();
    }

    private static boolean safeChinese(String raw) {
        if (raw == null || raw.isBlank()) {
            return false;
        }
        boolean hasHan = false;
        for (int i = 0; i < raw.length(); i++) {
            if (Character.UnicodeScript.of(raw.charAt(i)) == Character.UnicodeScript.HAN) {
                hasHan = true;
                break;
            }
        }
        return hasHan && !raw.matches(".*[A-Za-z_][A-Za-z0-9_.:/-]*.*");
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
