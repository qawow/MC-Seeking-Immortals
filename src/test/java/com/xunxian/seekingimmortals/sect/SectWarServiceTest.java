package com.xunxian.seekingimmortals.sect;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SectWarServiceTest {
    @Test
    void warPhaseUsesElapsedFractionWhenStartKnown() {
        SectWarService.WarData data = new SectWarService.WarData();
        data.active = true;
        data.startedAtGameTime = 1000L;
        data.endsAtGameTime = 1000L + 20L * 60L * 10L; // 10 min window

        long early = data.startedAtGameTime + 20L * 60L; // 1 min in (~10%)
        long mid = data.startedAtGameTime + 20L * 60L * 5L; // 5 min (~50%)
        long late = data.startedAtGameTime + 20L * 60L * 9L; // 9 min (~90%)

        assertEquals(1, SectWarService.warPhase(data, early));
        assertEquals(2, SectWarService.warPhase(data, mid));
        assertEquals(3, SectWarService.warPhase(data, late));
    }

    @Test
    void warPhaseFallsBackToRemainBucketsWithoutStart() {
        SectWarService.WarData data = new SectWarService.WarData();
        data.active = true;
        data.startedAtGameTime = 0L;
        data.endsAtGameTime = 20L * 60L * 20L;

        assertEquals(1, SectWarService.warPhase(data, 0L)); // 20 min remain
        assertEquals(2, SectWarService.warPhase(data, 20L * 60L * 16L)); // 4 min remain
        assertEquals(3, SectWarService.warPhase(data, 20L * 60L * 19L)); // 1 min remain
    }

    @Test
    void warDataReportsThirdArmyWhenPresent() {
        SectWarService.WarData data = new SectWarService.WarData();
        data.factionC = "";
        assertFalse(data.hasThirdArmy());
        assertEquals(2, data.armyCount());
        data.factionC = "tianlan";
        assertTrue(data.hasThirdArmy());
        assertEquals(3, data.armyCount());
    }
}
