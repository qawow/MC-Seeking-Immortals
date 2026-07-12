package com.xunxian.seekingimmortals.design;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class SettingCatalogSummaryServiceTest {
    @Test
    void summarizesSettingPackWithoutTrustingMalformedJson() {
        Path dataRoot = Path.of("\u6587\u672c\u6750\u6599", "data");
        assumeTrue(Files.exists(dataRoot), "setting-pack source materials are not present");

        SettingCatalogSummary summary = SettingCatalogSummaryService.summarize(dataRoot);

        assertEquals(346, summary.declaredTechniqueCount());
        assertEquals(20, summary.declaredTechniqueFileCount());
        assertTrue(summary.presentFiles() >= 20);
        assertTrue(summary.validFiles() > 0);
        assertFalse(summary.find("techniques/index.json").orElseThrow().error().contains("\n"));
    }

    @Test
    void recordsIndividualCatalogCounts() {
        Path dataRoot = Path.of("\u6587\u672c\u6750\u6599", "data");
        assumeTrue(Files.exists(dataRoot), "setting-pack source materials are not present");

        SettingCatalogSummary summary = SettingCatalogSummaryService.summarize(dataRoot);

        SettingCatalogSummary.CatalogFileStatus talismanCatalog = summary.find("talisman_catalog.json").orElseThrow();
        assertTrue(talismanCatalog.present());
        assertTrue(talismanCatalog.valid());
        assertEquals(47, talismanCatalog.entryCount());
    }
}

