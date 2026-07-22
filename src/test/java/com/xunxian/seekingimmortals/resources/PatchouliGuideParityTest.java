package com.xunxian.seekingimmortals.resources;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PatchouliGuideParityTest {
    private static final Path BOOK_ROOT = Path.of(
            "src/main/resources/data/seeking_immortals/patchouli_books/seeking_immortals_guide");
    private static final Path ITEM_MODEL_ROOT = Path.of(
            "src/main/resources/assets/seeking_immortals/models/item");
    private static final Pattern INTERNAL_LINK = Pattern.compile("\\$\\(l:([^)]+)\\)");

    @Test
    void zhAndEnEntryParityAndCategoriesExpanded() throws IOException {
        Set<String> zhEntries = stems(BOOK_ROOT.resolve("zh_cn/entries"));
        Set<String> enEntries = stems(BOOK_ROOT.resolve("en_us/entries"));
        assertEquals(zhEntries, enEntries, "zh/en patchouli entries must match");
        assertTrue(zhEntries.size() >= 104, "expected expanded patchouli entries, got " + zhEntries.size());
        assertTrue(zhEntries.contains("name_alias_glossary"));
        assertTrue(zhEntries.contains("numeric_overview"));
        assertTrue(zhEntries.contains("bestiary_compendium"));
        assertTrue(zhEntries.contains("chronicle_timeline"));
        assertTrue(zhEntries.contains("lore_hub"));
        assertTrue(zhEntries.contains("manual_formula_study"));
        assertTrue(zhEntries.contains("multiblock_building"));
        assertTrue(zhEntries.contains("item_usage_requirements"));
        assertTrue(zhEntries.contains("combat_status_guide"));

        // 0.2.93 P2-10 new-system guide pages
        assertTrue(zhEntries.contains("station_operations"));
        assertTrue(zhEntries.contains("delivery_outbox"));
        assertTrue(zhEntries.contains("consumable_semantics"));
        assertTrue(zhEntries.contains("companion_growth"));
        assertTrue(zhEntries.contains("craft_recipe_browser"));
        assertTrue(zhEntries.contains("sect_specialty_play"));
        assertTrue(zhEntries.contains("secret_realm_playbook"));

        Set<String> zhCategories = stems(BOOK_ROOT.resolve("zh_cn/categories"));
        Set<String> enCategories = stems(BOOK_ROOT.resolve("en_us/categories"));
        assertEquals(zhCategories, enCategories, "zh/en patchouli categories must match");
        assertTrue(zhCategories.contains("techniques"));
        assertTrue(zhCategories.contains("beasts_puppets"));
        assertTrue(zhCategories.contains("reference"));
        assertTrue(zhCategories.contains("quests"));
        assertTrue(zhCategories.size() >= 6);

        for (String stem : zhCategories) {
            JsonObject zh = readJson(BOOK_ROOT.resolve("zh_cn/categories/" + stem + ".json"));
            JsonObject en = readJson(BOOK_ROOT.resolve("en_us/categories/" + stem + ".json"));
            validateCategory(zh, "zh_cn category " + stem);
            validateCategory(en, "en_us category " + stem);
            assertEquals(requiredString(zh, "icon", stem), requiredString(en, "icon", stem),
                    "category icon parity: " + stem);
            assertEquals(zh.get("sortnum").getAsInt(), en.get("sortnum").getAsInt(),
                    "category sortnum parity: " + stem);
            assertFalse(containsHan(requiredString(en, "name", stem)),
                    "English category name still contains Han characters: " + stem);
            assertFalse(containsHan(requiredString(en, "description", stem)),
                    "English category description still contains Han characters: " + stem);
            assertNoHan(en, "en_us category " + stem);
        }

        for (String stem : zhEntries) {
            JsonObject zh = readJson(BOOK_ROOT.resolve("zh_cn/entries/" + stem + ".json"));
            JsonObject en = readJson(BOOK_ROOT.resolve("en_us/entries/" + stem + ".json"));
            validateEntry(zh, zhCategories, "zh_cn entry " + stem);
            validateEntry(en, enCategories, "en_us entry " + stem);
            assertEquals(requiredString(zh, "category", stem), requiredString(en, "category", stem),
                    "entry category parity: " + stem);
            assertEquals(requiredString(zh, "icon", stem), requiredString(en, "icon", stem),
                    "entry icon parity: " + stem);
            assertEquals(zh.get("sortnum").getAsInt(), en.get("sortnum").getAsInt(),
                    "entry sortnum parity: " + stem);

            JsonArray zhPages = zh.getAsJsonArray("pages");
            JsonArray enPages = en.getAsJsonArray("pages");
            assertEquals(zhPages.size(), enPages.size(), "page count parity: " + stem);
            for (int i = 0; i < zhPages.size(); i++) {
                JsonObject zhPage = zhPages.get(i).getAsJsonObject();
                JsonObject enPage = enPages.get(i).getAsJsonObject();
                assertEquals(requiredString(zhPage, "type", stem + " page " + i),
                        requiredString(enPage, "type", stem + " page " + i),
                        "page type parity: " + stem + " page " + i);
                assertEquals(optionalString(zhPage, "anchor", stem + " page " + i),
                        optionalString(enPage, "anchor", stem + " page " + i),
                        "page anchor parity: " + stem + " page " + i);
            }
            assertNoHan(en, "en_us entry " + stem);
        }

        validateInternalLinks(zhEntries, "zh_cn");
        validateInternalLinks(enEntries, "en_us");

        JsonObject book = readJson(BOOK_ROOT.resolve("book.json"));
        assertTrue(book.get("version").getAsInt() >= 5,
                "book version should track the runtime-handbook audit");
    }

    private static void validateCategory(JsonObject root, String context) {
        requiredString(root, "name", context);
        requiredString(root, "description", context);
        validateIcon(requiredString(root, "icon", context), context);
        assertTrue(root.has("sortnum") && root.get("sortnum").isJsonPrimitive()
                        && root.getAsJsonPrimitive("sortnum").isNumber(),
                "numeric sortnum required: " + context);
    }

    private static void validateEntry(JsonObject root, Set<String> categories, String context) {
        requiredString(root, "name", context);
        String category = requiredString(root, "category", context);
        String categoryStem = category.substring(category.indexOf(':') + 1);
        assertTrue(category.contains(":"), "category should be namespaced: " + context);
        assertTrue(categories.contains(categoryStem),
                "entry references missing category " + category + ": " + context);
        validateIcon(requiredString(root, "icon", context), context);
        assertTrue(root.has("sortnum") && root.get("sortnum").isJsonPrimitive()
                        && root.getAsJsonPrimitive("sortnum").isNumber(),
                "numeric sortnum required: " + context);
        assertTrue(root.has("pages") && root.get("pages").isJsonArray()
                        && !root.getAsJsonArray("pages").isEmpty(),
                "entry needs pages: " + context);

        JsonArray pages = root.getAsJsonArray("pages");
        for (int i = 0; i < pages.size(); i++) {
            assertTrue(pages.get(i).isJsonObject(), "page must be an object: " + context + " page " + i);
            JsonObject page = pages.get(i).getAsJsonObject();
            String type = requiredString(page, "type", context + " page " + i);
            if ("patchouli:text".equals(type)) {
                requiredString(page, "text", context + " page " + i);
            }
        }
    }

    private static void validateIcon(String icon, String context) {
        int separator = icon.indexOf(':');
        assertTrue(separator > 0 && separator < icon.length() - 1,
                "icon should be namespaced: " + icon + " in " + context);
        if (icon.startsWith("seeking_immortals:")) {
            String itemId = icon.substring(separator + 1);
            assertTrue(Files.isRegularFile(ITEM_MODEL_ROOT.resolve(itemId + ".json")),
                    "missing local item model for icon " + icon + " in " + context);
        }
    }

    private static String requiredString(JsonObject root, String key, String context) {
        assertTrue(root.has(key) && root.get(key).isJsonPrimitive(),
                "string " + key + " required: " + context);
        String value = root.get(key).getAsString();
        assertFalse(value.isBlank(), "nonblank " + key + " required: " + context);
        return value;
    }

    private static String optionalString(JsonObject root, String key, String context) {
        if (!root.has(key)) {
            return null;
        }
        return requiredString(root, key, context);
    }

    private static void assertNoHan(JsonElement root, String context) {
        walkStrings(root, "$", (path, value) -> assertFalse(containsHan(value),
                "English Patchouli string contains Han characters: " + context + " " + path + " = " + value));
    }

    private static void validateInternalLinks(Set<String> entries, String language) throws IOException {
        Map<String, Set<String>> anchors = new HashMap<>();
        Map<String, JsonObject> roots = new HashMap<>();
        for (String stem : entries) {
            JsonObject root = readJson(BOOK_ROOT.resolve(language + "/entries/" + stem + ".json"));
            roots.put(stem, root);
            Set<String> entryAnchors = new HashSet<>();
            for (JsonElement pageElement : root.getAsJsonArray("pages")) {
                JsonObject page = pageElement.getAsJsonObject();
                if (!page.has("anchor")) {
                    continue;
                }
                String anchor = requiredString(page, "anchor", language + " entry " + stem);
                assertTrue(entryAnchors.add(anchor),
                        "duplicate Patchouli anchor: " + language + " " + stem + "#" + anchor);
            }
            anchors.put(stem, entryAnchors);
        }

        for (Map.Entry<String, JsonObject> source : roots.entrySet()) {
            walkStrings(source.getValue(), "$", (path, value) -> {
                Matcher matcher = INTERNAL_LINK.matcher(value);
                while (matcher.find()) {
                    String target = matcher.group(1);
                    if (target.contains("://")) {
                        continue;
                    }
                    int hash = target.indexOf('#');
                    String entry = hash >= 0 ? target.substring(0, hash) : target;
                    String anchor = hash >= 0 ? target.substring(hash + 1) : "";
                    int namespace = entry.indexOf(':');
                    if (namespace >= 0) {
                        entry = entry.substring(namespace + 1);
                    }
                    assertTrue(entries.contains(entry), "missing internal Patchouli link target: "
                            + language + " " + source.getKey() + " " + path + " -> " + target);
                    if (!anchor.isEmpty()) {
                        assertTrue(anchors.get(entry).contains(anchor), "missing internal Patchouli anchor: "
                                + language + " " + source.getKey() + " " + path + " -> " + target);
                    }
                }
            });
        }
    }

    private static void walkStrings(JsonElement element, String path, BiConsumer<String, String> consumer) {
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
            consumer.accept(path, element.getAsString());
            return;
        }
        if (element.isJsonArray()) {
            for (int index = 0; index < element.getAsJsonArray().size(); index++) {
                walkStrings(element.getAsJsonArray().get(index), path + "[" + index + "]", consumer);
            }
            return;
        }
        if (element.isJsonObject()) {
            for (Map.Entry<String, JsonElement> child : element.getAsJsonObject().entrySet()) {
                walkStrings(child.getValue(), path + "." + child.getKey(), consumer);
            }
        }
    }

    private static boolean containsHan(String value) {
        return value.codePoints().anyMatch(codePoint ->
                Character.UnicodeScript.of(codePoint) == Character.UnicodeScript.HAN);
    }

    private static JsonObject readJson(Path path) throws IOException {
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    private static Set<String> stems(Path dir) throws IOException {
        try (Stream<Path> stream = Files.list(dir)) {
            return stream.filter(path -> path.getFileName().toString().endsWith(".json"))
                    .map(path -> path.getFileName().toString().replace(".json", ""))
                    .collect(Collectors.toSet());
        }
    }
}
