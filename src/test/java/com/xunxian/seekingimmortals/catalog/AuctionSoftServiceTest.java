package com.xunxian.seekingimmortals.catalog;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AuctionSoftServiceTest {
    @Test
    void loadsAuctionVenuesAndLots() {
        AuctionSoftService.Snapshot snapshot = AuctionSoftService.builtin();
        assertTrue(snapshot.venueCount() >= 1);
        assertTrue(snapshot.lotCount() >= 1);
        assertTrue(snapshot.minIncrementPct() > 0.0D);
    }
}
