package com.xunxian.seekingimmortals.lore;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.xunxian.seekingimmortals.SeekingImmortalsMod;
import net.minecraft.network.chat.Component;

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

/**
 * M16: read-only numeric overview (v103) for encyclopedia quick-reference pages.
 */
public final class NumericOverviewService {
    private static final Map<String, String> CURRENCY_LABELS = Map.of(
            "low_stone", "screen.seeking_immortals.compendium.numeric.currency.low_stone",
            "mid_stone", "screen.seeking_immortals.compendium.numeric.currency.mid_stone",
            "high_stone", "screen.seeking_immortals.compendium.numeric.currency.high_stone",
            "top_stone", "screen.seeking_immortals.compendium.numeric.currency.top_stone");
    private static final Map<String, String> BREAKTHROUGH_LABELS = Map.of(
            "qi_to_foundation", "screen.seeking_immortals.compendium.numeric.breakthrough.qi_to_foundation",
            "foundation_to_core", "screen.seeking_immortals.compendium.numeric.breakthrough.foundation_to_core",
            "core_to_nascent", "screen.seeking_immortals.compendium.numeric.breakthrough.core_to_nascent",
            "nascent_to_deity", "screen.seeking_immortals.compendium.numeric.breakthrough.nascent_to_deity",
            "deity_to_void", "screen.seeking_immortals.compendium.numeric.breakthrough.deity_to_void",
            "void_to_integration", "screen.seeking_immortals.compendium.numeric.breakthrough.void_to_integration",
            "integration_to_great", "screen.seeking_immortals.compendium.numeric.breakthrough.integration_to_great",
            "great_to_true_immortal", "screen.seeking_immortals.compendium.numeric.breakthrough.great_to_true_immortal");
    private static final Map<String, String> CONTENT_LABELS = Map.of(
            "bestiary_expansion_total", "screen.seeking_immortals.compendium.numeric.content.bestiary",
            "item_desc_total", "screen.seeking_immortals.compendium.numeric.content.items");
    private static final Map<String, String> CATALOG_LABELS = Map.of(
            "pills", "screen.seeking_immortals.compendium.visual.count.pills",
            "herbs", "screen.seeking_immortals.compendium.visual.count.herbs",
            "materials", "screen.seeking_immortals.compendium.visual.count.materials",
            "artifacts", "screen.seeking_immortals.compendium.visual.count.artifacts",
            "consumables", "screen.seeking_immortals.compendium.visual.count.consumables",
            "manuals", "screen.seeking_immortals.compendium.numeric.catalog.manuals",
            "beasts", "screen.seeking_immortals.compendium.visual.count.beasts");
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
            List<String> lines = new ArrayList<>();
            appendKnownNumbers(lines, currency, CURRENCY_LABELS);
            if (currency.containsKey("isolated")) {
                lines.add(Component.translatable(
                        "screen.seeking_immortals.compendium.numeric.currency.isolated").getString());
            }
            return List.copyOf(lines);
        }

        public List<String> breakthroughLines() {
            List<String> lines = new ArrayList<>();
            for (Map.Entry<String, String> entry : breakthroughBaseSuccess.entrySet()) {
                String labelKey = BREAKTHROUGH_LABELS.get(entry.getKey());
                String percent = percentText(entry.getValue());
                if (labelKey != null && !percent.isBlank()) {
                    lines.add(Component.translatable(
                            "screen.seeking_immortals.compendium.numeric.percent_entry",
                            Component.translatable(labelKey), percent).getString());
                }
            }
            return List.copyOf(lines);
        }

        public List<String> threatLines() {
            List<String> lines = new ArrayList<>();
            for (Map.Entry<String, String> entry : combatThreatGuide.entrySet()) {
                if (entry.getKey().matches("[1-9]")) {
                    lines.add(Component.translatable(
                            "screen.seeking_immortals.compendium.numeric.threat_entry",
                            entry.getKey(), Component.translatable(
                                    "screen.seeking_immortals.compendium.numeric.threat." + entry.getKey()))
                            .getString());
                }
            }
            return List.copyOf(lines);
        }

        public List<String> summaryLines() {
            List<String> lines = new ArrayList<>();
            lines.add(Component.translatable("screen.seeking_immortals.compendium.numeric.summary",
                    sects, questChains, chronicleEvents).getString());
            lines.add(Component.translatable("screen.seeking_immortals.compendium.numeric.techniques",
                    techniquesDeep).getString());
            appendKnownNumbers(lines, contentSnapshot, CONTENT_LABELS);
            if (!originalCatalogCounts.isEmpty()) {
                List<String> catalog = new ArrayList<>();
                for (Map.Entry<String, Integer> entry : originalCatalogCounts.entrySet()) {
                    String label = CATALOG_LABELS.get(entry.getKey());
                    if (label != null) {
                        catalog.add(Component.translatable(
                                "screen.seeking_immortals.compendium.numeric.catalog_entry",
                                Component.translatable(label), Math.max(0, entry.getValue())).getString());
                    }
                }
                if (!catalog.isEmpty()) {
                    lines.add(Component.translatable(
                            "screen.seeking_immortals.compendium.numeric.catalog_header").getString());
                    lines.addAll(catalog);
                }
            }
            return List.copyOf(lines);
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

    private static void appendKnownNumbers(List<String> lines, Map<String, String> values,
                                           Map<String, String> labels) {
        for (Map.Entry<String, String> entry : values.entrySet()) {
            String labelKey = labels.get(entry.getKey());
            String value = numberText(entry.getValue());
            if (labelKey != null && !value.isBlank()) {
                lines.add(Component.translatable(
                        "screen.seeking_immortals.compendium.numeric.entry",
                        Component.translatable(labelKey), value).getString());
            }
        }
    }

    private static String numberText(String raw) {
        if (raw == null) {
            return "";
        }
        String value = raw.trim();
        return value.matches("[-+]?[0-9]+(?:\\.[0-9]+)?") ? value : "";
    }

    private static String percentText(String raw) {
        String value = numberText(raw);
        if (value.isBlank()) {
            return "";
        }
        try {
            double percent = Double.parseDouble(value) * 100.0D;
            if (Math.abs(percent - Math.rint(percent)) < 0.0001D) {
                return Long.toString(Math.round(percent));
            }
            return String.format(Locale.ROOT, "%.1f", percent)
                    .replaceAll("0+$", "")
                    .replaceAll("\\.$", "");
        } catch (NumberFormatException ignored) {
            return "";
        }
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
