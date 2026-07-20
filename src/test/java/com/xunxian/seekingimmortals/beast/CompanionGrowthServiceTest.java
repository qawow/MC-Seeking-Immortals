package com.xunxian.seekingimmortals.beast;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompanionGrowthServiceTest {
    @Test
    void authoredStageCountsMapToStableCapstoneThresholds() {
        assertEquals(List.of(), CompanionGrowthService.evolutionThresholds(1));
        assertEquals(List.of(20), CompanionGrowthService.evolutionThresholds(2));
        assertEquals(List.of(10, 20), CompanionGrowthService.evolutionThresholds(3));
        assertEquals(List.of(7, 14, 20), CompanionGrowthService.evolutionThresholds(4));
        assertEquals(20, CompanionGrowthService.experienceToNextLevel(0));
        assertEquals(210, CompanionGrowthService.experienceToNextLevel(19));
    }

    @Test
    void thresholdRetainsExperienceUntilEvolutionStationIsReady() {
        CompanionGrowthService.Progress start = new CompanionGrowthService.Progress(6, 0, 0);
        CompanionGrowthService.Update blocked = CompanionGrowthService.grant(start, 100, 4, false);
        assertEquals(new CompanionGrowthService.Progress(6, 80, 0), blocked.progress());
        assertTrue(blocked.evolutionBlocked());
        assertEquals(0, blocked.levelsGained());

        CompanionGrowthService.Update evolved = CompanionGrowthService.grant(blocked.progress(), 0, 4, true);
        assertEquals(new CompanionGrowthService.Progress(7, 0, 1), evolved.progress());
        assertFalse(evolved.evolutionBlocked());
        assertEquals(1, evolved.levelsGained());
        assertEquals(1, evolved.evolutionsGained());
    }

    @Test
    void legacyGrowthKeepsItsPublishedStageAndNewStatsScaleMonotonically() {
        CompanionGrowthService.Progress legacy = CompanionGrowthService.legacyProgress(14, 4);
        assertEquals(new CompanionGrowthService.Progress(14, 0, 2), legacy);
        assertTrue(CompanionGrowthService.statMultiplier(legacy)
                > CompanionGrowthService.statMultiplier(new CompanionGrowthService.Progress(7, 0, 1)));
        CompanionGrowthService.Update capped = CompanionGrowthService.grant(
                new CompanionGrowthService.Progress(20, 99, 3), 9999, 4, true);
        assertEquals(new CompanionGrowthService.Progress(20, 0, 3), capped.progress());
    }
}
