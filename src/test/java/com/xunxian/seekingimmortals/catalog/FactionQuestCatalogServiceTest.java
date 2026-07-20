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

    @Test
    void uppercaseChronicleIdsResolveThroughLowercaseLookups() {
        FactionQuestCatalogService.Snapshot snapshot = FactionQuestCatalogService.builtin();
        // Runtime lookups normalize to lowercase; authored ids like A4_/M1_ must stay reachable.
        assertTrue(snapshot.chronicleEvents().containsKey("a4_void_palace_built"));
        assertTrue(snapshot.chronicleEvents().containsKey("a1_kunwu_peak"));
        assertTrue(snapshot.chronicleEvents().containsKey("e_ancient_demon_seal_weak"));
        assertTrue(snapshot.chronicleEvents().containsKey("m1_five_realms"));
        // Entry keeps its original display id for UI.
        assertTrue(snapshot.chronicleEvents().get("a4_void_palace_built").id().startsWith("A4"));
    }
}
