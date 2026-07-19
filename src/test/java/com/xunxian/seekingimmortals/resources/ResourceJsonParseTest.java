package com.xunxian.seekingimmortals.resources;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResourceJsonParseTest {
    private static final Path PATCHOULI_GUIDE_ROOT = Path.of(
            "src/main/resources/data/seeking_immortals/patchouli_books/seeking_immortals_guide");

    @Test
    void shippedJsonResourcesParse() {
        List<Path> roots = List.of(
                Path.of("src/main/resources/assets/seeking_immortals/lang"),
                Path.of("src/main/resources/data/seeking_immortals/shops"),
                Path.of("src/main/resources/data/seeking_immortals/recipes"),
                Path.of("src/main/resources/data/seeking_immortals/alchemy"),
                Path.of("src/main/resources/data/seeking_immortals/artifacts"),
                PATCHOULI_GUIDE_ROOT);

        for (Path root : roots) {
            if (!Files.exists(root)) {
                continue;
            }
            assertDoesNotThrow(() -> parseTree(root), "Failed to parse JSON under " + root);
        }
    }

    @Test
    void patchouliGuideUsesCanonicalBookLayout() throws Exception {
        assertTrue(Files.exists(PATCHOULI_GUIDE_ROOT.resolve("book.json")),
                "Patchouli requires book.json directly under the book id folder");
        assertFalse(Files.exists(PATCHOULI_GUIDE_ROOT.resolve("zh_cn/book.json")),
                "Language folders must not contain book.json; it makes the starter book invalid");
        assertFalse(Files.exists(PATCHOULI_GUIDE_ROOT.resolve("en_us/book.json")),
                "Language folders must not contain book.json; it makes the starter book invalid");

        assertLanguageContentMatches("categories");
        assertLanguageContentMatches("entries");
    }

    private static void parseTree(Path root) throws Exception {
        try (Stream<Path> paths = Files.walk(root)) {
            for (Path path : paths.filter(path -> path.toString().endsWith(".json")).toList()) {
                try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                    JsonParser.parseReader(reader);
                }
            }
        }
    }

    private static void assertLanguageContentMatches(String folder) throws Exception {
        Path zhRoot = PATCHOULI_GUIDE_ROOT.resolve(Path.of("zh_cn", folder));
        Path enRoot = PATCHOULI_GUIDE_ROOT.resolve(Path.of("en_us", folder));

        assertTrue(Files.isDirectory(zhRoot), "Missing zh_cn Patchouli " + folder + " folder");
        assertTrue(Files.isDirectory(enRoot), "Missing en_us Patchouli " + folder + " folder");
        assertEquals(relativeJsonFiles(zhRoot), relativeJsonFiles(enRoot),
                "Patchouli " + folder + " files must match between zh_cn and en_us");
    }

    private static List<String> relativeJsonFiles(Path root) throws Exception {
        try (Stream<Path> paths = Files.walk(root)) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".json"))
                    .map(root::relativize)
                    .map(path -> path.toString().replace('\\', '/'))
                    .sorted()
                    .toList();
        }
    }
}
