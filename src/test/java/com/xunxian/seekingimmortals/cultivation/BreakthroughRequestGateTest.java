package com.xunxian.seekingimmortals.cultivation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BreakthroughRequestGateTest {
    @Test
    void duplicateRequestsAreBlockedUntilTheServerTickBoundary() {
        long gateUntil = BreakthroughService.nextBreakthroughRequestGate(100L);
        assertEquals(110L, gateUntil);
        assertTrue(BreakthroughService.isBreakthroughRequestBlocked(100L, gateUntil));
        assertTrue(BreakthroughService.isBreakthroughRequestBlocked(109L, gateUntil));
        assertFalse(BreakthroughService.isBreakthroughRequestBlocked(110L, gateUntil));
    }
}
