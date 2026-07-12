package com.xunxian.seekingimmortals.catalog;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChronicleTradeSoftServiceTest {
    @Test
    void loadsChronicleAndTradeRouteIndexes() {
        assertTrue(ChronicleTradeSoftService.chronicleCount() >= 20);
        assertTrue(ChronicleTradeSoftService.tradeRouteCount() >= 5);
        assertFalse(ChronicleTradeSoftService.sampleChronicle(3).isEmpty());
        assertFalse(ChronicleTradeSoftService.sampleTradeRoutes(3).isEmpty());
    }
}
