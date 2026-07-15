package com.xunxian.seekingimmortals.worldpack;

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
}
