package com.xunxian.seekingimmortals.catalog;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExtendedCatalogServiceTest {
    @Test
    void loadsQuestSectEconomyAndStoryIndexes() {
        ExtendedCatalogService.Snapshot snapshot = ExtendedCatalogService.builtin();
        assertEquals(17, snapshot.priceBands().size());
        assertEquals(62, snapshot.questChains().size());
        assertEquals(28, snapshot.sects().size());
        assertEquals(57, snapshot.consumables().size());
        assertEquals(7, snapshot.chapters().size());
        assertEquals(71, snapshot.dailyEvents().size());
        assertEquals(55, snapshot.alchemyRecipes().size());
        assertEquals(33, snapshot.spatialNodes().size());
        assertTrue(snapshot.materials().size() >= 200);
        assertTrue(snapshot.pills().size() >= 100);
        assertTrue(snapshot.artifacts().size() >= 200);
        assertEquals(6, snapshot.talismanMaterials().size());
        assertTrue(snapshot.totalIndexedEntries() > 800);
        assertTrue(snapshot.findQuest("huangfeng_cultivation_path").isPresent());
        assertTrue(snapshot.findSect("huangfeng_valley").isPresent());
        assertTrue(snapshot.findBand("talisman_low").isPresent());
        ExtendedCatalogService.QuestStartRequirements fallenDemon = snapshot
                .findQuest("fallen_demon_campaign")
                .orElseThrow()
                .startRequirements();
        assertEquals("NASCENT_SOUL", fallenDemon.realmMin());
        assertEquals("fallen_demon_valley", fallenDemon.region());
    }
}
