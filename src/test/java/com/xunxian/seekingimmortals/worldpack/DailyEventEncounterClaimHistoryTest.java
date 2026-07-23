package com.xunxian.seekingimmortals.worldpack;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DailyEventEncounterClaimHistoryTest {
    @Test
    void returningToARegionDuringTheSameRollRemainsLatched() {
        CompoundTag latch = new CompoundTag();
        long now = 1000L;
        long until = 5000L;

        DailyEventEncounterService.recordEncounterClaim(latch, "tiannan", "beast_tide", until, now);
        DailyEventEncounterService.recordEncounterClaim(latch, "chaotic_sea", "beast_tide", until, now);

        assertTrue(DailyEventEncounterService.hasEncounterClaim(
                latch, "tiannan", "beast_tide", until, now));
        assertTrue(DailyEventEncounterService.hasEncounterClaim(
                latch, "chaotic_sea", "beast_tide", until, now));
        assertEquals(2, DailyEventEncounterService.encounterClaimCount(latch, now));
    }

    @Test
    void legacyLatchMigratesAndExpiresAtItsBoundary() {
        CompoundTag latch = new CompoundTag();
        latch.putString("Region", "Tiannan");
        latch.putString("Event", "Bandit_Raid");
        latch.putLong("Until", 2000L);

        assertTrue(DailyEventEncounterService.hasEncounterClaim(
                latch, "tiannan", "bandit_raid", 2000L, 1000L));
        assertEquals(1, DailyEventEncounterService.encounterClaimCount(latch, 1000L));
        assertFalse(DailyEventEncounterService.hasEncounterClaim(
                latch, "tiannan", "bandit_raid", 2000L, 2000L));
        assertFalse(latch.contains("Event"));
    }

    @Test
    void matchingLegacyBooleanLatchBecomesTheCurrentScopedClaimBeforeRemoval() {
        CompoundTag playerData = new CompoundTag();
        CompoundTag latch = new CompoundTag();
        playerData.putBoolean("seeking_immortals_daily_spawned_Bandit_Raid", true);
        playerData.putBoolean("seeking_immortals_daily_spawned_other_event", true);

        assertTrue(DailyEventEncounterService.migrateLegacyBooleanClaims(
                playerData, latch, "tiannan", "bandit_raid", 5000L, 1000L));
        assertFalse(playerData.contains("seeking_immortals_daily_spawned_Bandit_Raid"));
        assertFalse(playerData.contains("seeking_immortals_daily_spawned_other_event"));
        assertTrue(DailyEventEncounterService.hasEncounterClaim(
                latch, "tiannan", "bandit_raid", 5000L, 1000L));
        assertEquals(1, DailyEventEncounterService.encounterClaimCount(latch, 1000L));
    }

    @Test
    void encounterHistoryIsBoundedAndZeroUntilUsesFiniteFallback() {
        CompoundTag latch = new CompoundTag();
        for (int i = 0; i < 40; i++) {
            DailyEventEncounterService.recordEncounterClaim(
                    latch, "region_" + i, "event_" + i, 10000L + i, 100L);
        }

        assertEquals(32, DailyEventEncounterService.encounterClaimCount(latch, 100L));
        assertFalse(DailyEventEncounterService.hasEncounterClaim(
                latch, "region_0", "event_0", 10000L, 100L));
        assertTrue(DailyEventEncounterService.hasEncounterClaim(
                latch, "region_39", "event_39", 10039L, 100L));

        CompoundTag compatibility = new CompoundTag();
        DailyEventEncounterService.recordEncounterClaim(
                compatibility, "tiannan", "bandit_raid", 0L, 100L);
        assertEquals(24000L, DailyEventEncounterService.fallbackClaimUntil(100L));
        assertTrue(DailyEventEncounterService.hasEncounterClaim(
                compatibility, "tiannan", "bandit_raid", 0L, 100L));
        assertFalse(DailyEventEncounterService.hasEncounterClaim(
                compatibility, "tiannan", "bandit_raid", 0L, 24000L));
    }
}
