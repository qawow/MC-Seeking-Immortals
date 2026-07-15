package com.xunxian.seekingimmortals.catalog;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuctionAppraisalGateTest {
    @Test
    void highTierLotsRequireAppraisalByDefault() {
        AuctionSoftService.AppraisalGateMode previous = AuctionSoftService.APPRAISAL_GATE_MODE;
        long previousThreshold = AuctionSoftService.HIGH_TIER_MIN_EQUIV;
        try {
            AuctionSoftService.APPRAISAL_GATE_MODE = AuctionSoftService.AppraisalGateMode.HIGH_TIER;
            AuctionSoftService.HIGH_TIER_MIN_EQUIV = 80L;
            AuctionSoftService.Lot low = new AuctionSoftService.Lot("low", "Low", 10, 20, "", "v", "item", java.util.List.of());
            AuctionSoftService.Lot high = new AuctionSoftService.Lot("high", "High", 100, 200, "", "v", "item", java.util.List.of());
            assertFalse(AuctionSoftService.requiresAppraisal(low));
            assertTrue(AuctionSoftService.requiresAppraisal(high));
            AuctionSoftService.APPRAISAL_GATE_MODE = AuctionSoftService.AppraisalGateMode.ALL;
            assertTrue(AuctionSoftService.requiresAppraisal(low));
            AuctionSoftService.APPRAISAL_GATE_MODE = AuctionSoftService.AppraisalGateMode.OFF;
            assertFalse(AuctionSoftService.requiresAppraisal(high));
        } finally {
            AuctionSoftService.APPRAISAL_GATE_MODE = previous;
            AuctionSoftService.HIGH_TIER_MIN_EQUIV = previousThreshold;
        }
    }
}
