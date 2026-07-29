package com.xunxian.seekingimmortals.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CultivationStatsInteractionTest {
    @Test
    void keyboardMovementScaleUsesFivePercentSteps() {
        assertEquals(0.55D, CultivationStatsScreen.keyboardMovementScale(0.50D, 1), 0.0001D);
        assertEquals(0.45D, CultivationStatsScreen.keyboardMovementScale(0.50D, -1), 0.0001D);
        assertEquals(1.0D, CultivationStatsScreen.keyboardMovementScale(1.0D, 1), 0.0001D);
        assertEquals(0.0D, CultivationStatsScreen.keyboardMovementScale(0.0D, -1), 0.0001D);
    }

    @Test
    void movementScaleDoesNotResendAcknowledgedOrPendingValue() {
        assertFalse(CultivationStatsScreen.shouldSendMovementScale(0.55D, 0.55D, Double.NaN));
        assertFalse(CultivationStatsScreen.shouldSendMovementScale(0.55D, 0.50D, 0.55D));
        assertTrue(CultivationStatsScreen.shouldSendMovementScale(0.50D, 0.50D, 0.55D));
        assertTrue(CultivationStatsScreen.shouldSendMovementScale(0.60D, 0.50D, 0.55D));
    }

    @Test
    void movementScaleKeepsPendingValueWhenSliderIsRebuilt() {
        assertEquals(0.75D, CultivationStatsScreen.initialMovementScale(0.20D, 0.75D), 0.0001D);
        assertEquals(0.20D, CultivationStatsScreen.initialMovementScale(0.20D, Double.NaN), 0.0001D);
        assertEquals(0.20D, CultivationStatsScreen.initialMovementScale(0.20D, Double.POSITIVE_INFINITY), 0.0001D);
    }

    @Test
    void breakthroughButtonWaitsForSyncBeforeAllowingAnotherRequest() {
        assertTrue(CultivationStatsScreen.breakthroughRequestCanStart(-1));
        assertFalse(CultivationStatsScreen.breakthroughRequestCanStart(0));
        Object before = new Object();
        assertFalse(CultivationStatsScreen.breakthroughSyncArrived(before, before));
        assertTrue(CultivationStatsScreen.breakthroughSyncArrived(before, new Object()));
    }
}
