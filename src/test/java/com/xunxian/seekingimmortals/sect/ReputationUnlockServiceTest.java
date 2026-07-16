package com.xunxian.seekingimmortals.sect;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReputationUnlockServiceTest {
    @Test
    void loadsUnlockTableAndScale() {
        ReputationUnlockService.Snapshot snapshot = ReputationUnlockService.builtin();
        assertTrue(snapshot.factions().size() >= 20);
        assertEquals(-100, snapshot.scale().min());
        assertEquals(100, snapshot.scale().max());
        assertEquals(ReputationUnlockService.TIER_HOSTILE, ReputationUnlockService.tierFor(-50));
        assertEquals(ReputationUnlockService.TIER_FRIENDLY, ReputationUnlockService.tierFor(30));
        assertEquals(ReputationUnlockService.TIER_EXALTED, ReputationUnlockService.tierFor(90));
    }

    @Test
    void unlockAndLockQueriesFollowThresholds() {
        // huangfeng thresholds: 0/20/40/60 unlocks, -40 locks
        List<String> atZero = ReputationUnlockService.unlockedFor("huangfeng", 0);
        assertFalse(atZero.isEmpty());
        List<String> atForty = ReputationUnlockService.unlockedFor("huangfeng_valley", 40);
        assertTrue(atForty.size() >= atZero.size());
        List<String> lockedHostile = ReputationUnlockService.lockedFor("huangfeng", -40);
        assertFalse(lockedHostile.isEmpty());
        assertFalse(ReputationUnlockService.isRegionAccessOpen("huangfeng", -40));
        assertTrue(ReputationUnlockService.isRegionAccessOpen("huangfeng", 0));
        assertTrue(ReputationUnlockService.isShopTierOpen("huangfeng", 25, 20));
        assertFalse(ReputationUnlockService.isShopTierOpen("huangfeng", 10, 20));
    }
}
