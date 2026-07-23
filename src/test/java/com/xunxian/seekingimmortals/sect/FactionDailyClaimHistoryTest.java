package com.xunxian.seekingimmortals.sect;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FactionDailyClaimHistoryTest {
    @Test
    void returningToARegionDuringTheSameRollDoesNotClaimTwice() {
        CompoundTag root = new CompoundTag();
        long now = 1000L;
        long until = 5000L;
        String regionA = FactionConflictEventService.dailyClaimKey("tiannan", "border_war", until);
        String regionB = FactionConflictEventService.dailyClaimKey("mulan", "border_war", until);

        FactionConflictEventService.recordDailyClaim(root, regionA, now);
        FactionConflictEventService.recordDailyClaim(root, regionB, now);

        assertTrue(FactionConflictEventService.hasDailyClaim(root, regionA, now));
        assertTrue(FactionConflictEventService.hasDailyClaim(root, regionB, now));
        assertEquals(2, FactionConflictEventService.dailyClaimCount(root, now));
    }

    @Test
    void legacySingleEventMarkerMigratesToTheCurrentRollKey() {
        CompoundTag root = new CompoundTag();
        root.putString("LastDailyEvent", "MULAN_SCOUT_CLASH");
        String key = FactionConflictEventService.dailyClaimKey(
                "mulan_grassland", "mulan_scout_clash", 8000L);

        assertTrue(FactionConflictEventService.hasDailyClaim(root, key, 1000L));
        assertEquals(key, root.getString("LastDailyEvent"));
        assertEquals(1, FactionConflictEventService.dailyClaimCount(root, 1000L));
    }

    @Test
    void claimsExpireAtTheRollBoundaryAndHistoryStaysBounded() {
        CompoundTag root = new CompoundTag();
        String expired = FactionConflictEventService.dailyClaimKey("tiannan", "old", 200L);
        FactionConflictEventService.recordDailyClaim(root, expired, 100L);
        assertTrue(FactionConflictEventService.hasDailyClaim(root, expired, 199L));
        assertFalse(FactionConflictEventService.hasDailyClaim(root, expired, 200L));

        for (int i = 0; i < 40; i++) {
            FactionConflictEventService.recordDailyClaim(root,
                    FactionConflictEventService.dailyClaimKey("region_" + i, "event_" + i, 10000L + i),
                    300L);
        }
        assertEquals(32, FactionConflictEventService.dailyClaimCount(root, 300L));
        assertFalse(FactionConflictEventService.hasDailyClaim(root,
                FactionConflictEventService.dailyClaimKey("region_0", "event_0", 10000L), 300L));
        assertTrue(FactionConflictEventService.hasDailyClaim(root,
                FactionConflictEventService.dailyClaimKey("region_39", "event_39", 10039L), 300L));
    }

    @Test
    void compatibilityCallsReceiveAFiniteFallbackWindow() {
        assertEquals(24000L, FactionConflictEventService.fallbackClaimUntil(100L));
        assertEquals(24000L, FactionConflictEventService.fallbackClaimUntil(23999L));
        assertEquals(48000L, FactionConflictEventService.fallbackClaimUntil(24000L));
        assertEquals(Long.MAX_VALUE, FactionConflictEventService.fallbackClaimUntil(Long.MAX_VALUE - 5L));

        CompoundTag root = new CompoundTag();
        FactionConflictEventService.recordDailyClaim(root,
                FactionConflictEventService.dailyClaimKey("tiannan", "event", 0L), 100L);
        assertEquals(0, FactionConflictEventService.dailyClaimCount(root, 100L));
    }

    @Test
    void claimHistoryAloneIsNotActiveStateThatRequiresRepeatedCleanup() {
        CompoundTag root = new CompoundTag();
        FactionConflictEventService.recordDailyClaim(root,
                FactionConflictEventService.dailyClaimKey("tiannan", "border_war", 5000L), 1000L);

        assertFalse(FactionConflictEventService.hasActiveState(root));
        root.putLong("ActiveUntil", 5000L);
        assertTrue(FactionConflictEventService.hasActiveState(root));
        root.remove("ActiveUntil");
        root.putInt("PriceModBp", 11000);
        assertTrue(FactionConflictEventService.hasActiveState(root));
    }
}
