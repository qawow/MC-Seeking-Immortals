package com.xunxian.seekingimmortals.catalog;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class FactionQuestCatalogServiceTest {
    @Test
    void loadsFactionQuestAndChronicleIndexes() {
        FactionQuestCatalogService.Snapshot snapshot = FactionQuestCatalogService.builtin();
        assertTrue(snapshot.chronicleEvents().size() >= 20);
        assertTrue(snapshot.factionConflicts().size() >= 5);
        assertTrue(snapshot.questHooks().size() >= 50);
        assertTrue(snapshot.merchantShops().size() >= 40);
        assertTrue(snapshot.tradeRoutes().size() >= 5);
        assertTrue(snapshot.totalEntries() >= 100);
    }
}
