package com.xunxian.seekingimmortals.worldpack;

import com.xunxian.seekingimmortals.beast.BeastBestiaryService;
import com.xunxian.seekingimmortals.beast.BeastCompanionService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BeastSpawnTableServiceTest {
    @Test
    void loadsSpawnTablesFromTextMaterial() {
        assertTrue(BeastSpawnTableService.tableCount() > 0);
        assertFalse(BeastSpawnTableService.tables().isEmpty());
        assertTrue(BeastSpawnTableService.findTable("tiannan", "forest").isPresent());
    }

    @Test
    void regionSpawnTablesMergedAndCompanionsBanned() {
        // region_spawn_tables_v98 keys (M06 region ids)
        assertTrue(
                BeastSpawnTableService.findTable("blood_forbidden", "any").isPresent()
                        || BeastSpawnTableService.findTable("blood_forbidden", "").isPresent()
                        || BeastSpawnTableService.tables().stream().anyMatch(t -> "blood_forbidden".equals(t.region())));
        assertTrue(BeastCompanionService.isProtectedCompanion("shi_jin_chong"));
        assertTrue(BeastSpawnTableService.isBanned("shi_jin_chong"));
        assertTrue(BeastBestiaryService.isBannedFromDailySpawn("bing_feng"));
        for (BeastSpawnTableService.Table table : BeastSpawnTableService.tables()) {
            for (BeastSpawnTableService.Weight weight : table.weights()) {
                assertFalse(BeastSpawnTableService.isBanned(weight.beastId()),
                        "banned beast in table " + table.region() + ": " + weight.beastId());
            }
        }
    }
}
