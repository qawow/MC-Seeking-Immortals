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
 * M16: read-only numeric overview (v103) for encyclopedia quick-reference pages.
 */
public final class NumericOverviewService {
    private static final Snapshot BUILTIN = loadBuiltin();

    private NumericOverviewService() {}

    public record Snapshot(
            Map<String, String> currency,
            Map<String, String> breakthroughBaseSuccess,
            Map<String, String> reputationBands,
            Map<String, String> combatThreatGuide,
            Map<String, String> contentSnapshot,
            Map<String, Integer> originalCatalogCounts,
            int sects,
            int questChains,
            int chronicleEvents,
            int techniquesDeep,
            boolean present) {
        public List<String> currencyLines() {
            return mapLines(currency);
        }

        public List<String> breakthroughLines() {
            return mapLines(breakthroughBaseSuccess);
        }

        public List<String> threatLines() {
            return mapLines(combatThreatGuide);
        }

        public List<String> summaryLines() {
            List<String> lines = new ArrayList<>();
            lines.add("sects=" + sects + " quest_chains=" + questChains + " chronicle=" + chronicleEvents);
            lines.add("techniques_deep=" + techniquesDeep);
            lines.addAll(mapLines(contentSnapshot));
            if (!originalCatalogCounts.isEmpty()) {
                StringBuilder sb = new StringBuilder("catalog:");
                originalCatalogCounts.forEach((k, v) -> sb.append(' ').append(k).append('=').append(v));
                lines.add(sb.toString());
            }
            return lines;
        }
    }

    public static Snapshot builtin() {
        return BUILTIN;
    }

    public static boolean present() {
        return BUILTIN.present();
    }

    public static List<String> sampleLines(int limit) {
        List<String> all = new ArrayList<>();
        all.addAll(BUILTIN.currencyLines());
        all.addAll(BUILTIN.breakthroughLines());
        all.addAll(BUILTIN.summaryLines());
        if (all.size() <= limit) {
            return all;
        }
        return List.copyOf(all.subList(0, Math.max(1, limit)));
    }

    private static List<String> mapLines(Map<String, String> map) {
        List<String> lines = new ArrayList<>();
        for (Map.Entry<String, String> e : map.entrySet()) {
            lines.add(e.getKey() + ": " + e.getValue());
        }
        return lines;
    }

    private static Snapshot loadBuiltin() {
        JsonObject root = readJson("data/" + SeekingImmortalsMod.MODID + "/text_material/numeric_overview_v103.json");
        if (root == null) {
            return new Snapshot(Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), 0, 0, 0, 0, false);
        }
        return new Snapshot(
                stringifyMap(root.getAsJsonObject("currency")),
                stringifyMap(root.getAsJsonObject("breakthrough_base_success")),
                stringifyMap(root.getAsJsonObject("reputation_bands")),
                stringifyMap(root.getAsJsonObject("combat_threat_guide")),
                stringifyMap(root.getAsJsonObject("content_snapshot")),
                intMap(root.getAsJsonObject("original_catalog_counts")),
                asInt(root, "sects"),
                asInt(root, "quest_chains"),
                asInt(root, "chronicle_events"),
                asInt(root, "techniques_deep"),
                true
        );
    }

    private static Map<String, String> stringifyMap(JsonObject object) {
        if (object == null) {
            return Map.of();
        }
        Map<String, String> map = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> e : object.entrySet()) {
            map.put(e.getKey(), stringify(e.getValue()));
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

    private static String stringify(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return "";
        }
        if (element.isJsonPrimitive()) {
            return element.getAsString();
        }
        return element.toString();
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

    private static JsonObject readJson(String path) {
        try (InputStream stream = NumericOverviewService.class.getClassLoader().getResourceAsStream(path)) {
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
