package com.xunxian.seekingimmortals.resources;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PatchouliGuideParityTest {
    private static final Path BOOK_ROOT = Path.of(
            "src/main/resources/data/seeking_immortals/patchouli_books/seeking_immortals_guide");

    @Test
    void zhAndEnEntryParityAndCategoriesExpanded() throws IOException {
        Set<String> zh = stems(BOOK_ROOT.resolve("zh_cn/entries"));
        Set<String> en = stems(BOOK_ROOT.resolve("en_us/entries"));
        assertEquals(zh, en, "zh/en patchouli entries must match");
        assertTrue(zh.size() >= 80, "expected expanded patchouli entries, got " + zh.size());
        assertTrue(zh.contains("name_alias_glossary"));
        assertTrue(zh.contains("numeric_overview"));
        assertTrue(zh.contains("bestiary_compendium"));
        assertTrue(zh.contains("chronicle_timeline"));
        assertTrue(zh.contains("lore_hub"));

        Set<String> cats = stems(BOOK_ROOT.resolve("zh_cn/categories"));
        assertTrue(cats.contains("techniques"));
        assertTrue(cats.contains("beasts_puppets"));
        assertTrue(cats.contains("reference"));
        assertTrue(cats.size() >= 6);

        // referenced icons should look like item ids
        for (String stem : zh) {
            Path path = BOOK_ROOT.resolve("zh_cn/entries/" + stem + ".json");
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                String icon = root.get("icon").getAsString();
                assertFalse(icon.isBlank());
                assertTrue(icon.contains(":"), "icon should be namespaced: " + icon + " in " + stem);
            }
        }
    }

    private static Set<String> stems(Path dir) throws IOException {
        try (Stream<Path> stream = Files.list(dir)) {
            return stream.filter(p -> p.getFileName().toString().endsWith(".json"))
                    .map(p -> p.getFileName().toString().replace(".json", ""))
                    .collect(Collectors.toSet());
        }
    }
}
