package com.xunxian.seekingimmortals.catalog;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class LoreCatalogServiceTest {
    @Test
    void loadsNpcDimensionSkillFactionAndPuppetIndexes() {
        LoreCatalogService.Snapshot snapshot = LoreCatalogService.builtin();
        assertTrue(snapshot.npcArchetypes().size() >= 10);
        assertTrue(snapshot.dimensions().size() >= 5);
        assertTrue(snapshot.skillTrees().size() >= 20);
        assertTrue(snapshot.factionNodes().size() >= 10);
        assertTrue(snapshot.puppetDefinitions().size() >= 5);
        assertTrue(snapshot.races().size() >= 3);
        assertTrue(snapshot.currencies().size() >= 3);
        assertTrue(snapshot.beasts().size() >= 20);
        assertTrue(snapshot.lootTables().size() >= 5);
        assertTrue(snapshot.constitutions().size() >= 5);
        assertTrue(snapshot.spiritRootGrades().size() >= 3);
        assertTrue(snapshot.ghostStages().size() >= 1);
        assertTrue(snapshot.ascensionStages().size() >= 1);
        assertTrue(snapshot.totalEntries() >= 100);
        assertTrue(SummonHonestMvpService.puppetDefinitionCount() >= 5);
    }
}
