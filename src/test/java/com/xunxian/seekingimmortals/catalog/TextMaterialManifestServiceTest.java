package com.xunxian.seekingimmortals.catalog;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TextMaterialManifestServiceTest {
    @Test
    void loadsFullTextMaterialManifest() {
        TextMaterialManifestService.Snapshot snapshot = TextMaterialManifestService.builtin();
        assertEquals(163, snapshot.catalogFiles());
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
