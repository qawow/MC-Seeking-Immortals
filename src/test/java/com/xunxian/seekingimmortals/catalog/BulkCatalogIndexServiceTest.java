package com.xunxian.seekingimmortals.catalog;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class BulkCatalogIndexServiceTest {
    @Test
    void loadsBulkCatalogManifestAndEntries() {
        BulkCatalogIndexService.Snapshot snapshot = BulkCatalogIndexService.builtin();
        assertTrue(snapshot.fileCount() >= 50);
        assertTrue(snapshot.totalEntries() >= 500);
        assertTrue(snapshot.find("formation_catalog_index").isPresent()
                || snapshot.find("formation_catalog").isPresent());
        assertTrue(snapshot.sampleFiles(5).size() >= 1);
    }
}
