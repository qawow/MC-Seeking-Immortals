package com.xunxian.seekingimmortals.catalog;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TextMaterialManifestServiceTest {
    @Test
    void loadsFullTextMaterialManifest() throws Exception {
        TextMaterialManifestService.Snapshot snapshot = TextMaterialManifestService.builtin();
        Path root = Path.of("src/main/resources/data/seeking_immortals/text_material");
        Set<String> shipped;
        try (var paths = Files.walk(root)) {
            shipped = paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".json"))
                    .filter(path -> !path.getFileName().toString().equals("manifest.json"))
                    .map(root::relativize)
                    .map(Path::toString)
                    .map(path -> path.replace('\\', '/'))
                    .collect(Collectors.toSet());
        }
        Set<String> indexed = snapshot.files().values().stream()
                .map(TextMaterialManifestService.FileEntry::file)
                .collect(Collectors.toSet());

        assertEquals(shipped, indexed, "manifest must index every shipped text-material JSON exactly once");
        assertEquals(shipped.size(), snapshot.totalFiles());
        assertEquals(21, snapshot.techniqueFiles());
        assertEquals(snapshot.totalFiles() - snapshot.techniqueFiles(), snapshot.catalogFiles());
        assertTrue(snapshot.totalEntries() > 3000);
        assertTrue(snapshot.contains("quest_chains"));
        assertTrue(snapshot.contains("formation_catalog"));
        assertTrue(snapshot.contains("merchant_shops"));
        assertTrue(snapshot.contains("techniques/formation"));
        assertTrue(snapshot.find("spatial_nodes_catalog").isPresent());
    }
}
