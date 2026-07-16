package com.xunxian.seekingimmortals.catalog;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TextMaterialManifestServiceTest {
    @Test
    void loadsFullTextMaterialManifest() {
        TextMaterialManifestService.Snapshot snapshot = TextMaterialManifestService.builtin();
        // M02: catalog_files recount includes newly published matrix files; keep lower bound only.
        assertTrue(snapshot.catalogFiles() >= 163);
        assertEquals(21, snapshot.techniqueFiles());
        assertTrue(snapshot.totalFiles() >= 184);
        assertTrue(snapshot.totalEntries() > 3000);
        assertTrue(snapshot.contains("quest_chains"));
        assertTrue(snapshot.contains("formation_catalog"));
        assertTrue(snapshot.contains("merchant_shops"));
        assertTrue(snapshot.contains("techniques/formation"));
        assertTrue(snapshot.find("spatial_nodes_catalog").isPresent());
    }
}
