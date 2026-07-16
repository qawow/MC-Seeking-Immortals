package com.xunxian.seekingimmortals.catalog;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuctionSoftServiceTest {
    @Test
    void loadsAuctionVenuesAndLots() {
        AuctionSoftService.Snapshot snapshot = AuctionSoftService.builtin();
        assertTrue(snapshot.venueCount() >= 1);
        assertTrue(snapshot.lotCount() >= 1);
        assertTrue(snapshot.minIncrementPct() > 0.0D);
        AuctionSoftService.Venue wanbao = snapshot.findVenue("wanbao_auction").orElseThrow();
        assertEquals(0, wanbao.repMin(), "a fresh player must be able to place the first Wanbao bid");
    }

    @Test
    void mergesWanbaoFrameworkPool() {
        AuctionSoftService.Snapshot snapshot = AuctionSoftService.builtin();
        // economy_auction_bands has 4 lots; wanbao stock+lots add more framework entries
        assertTrue(snapshot.lotCount() >= 8,
                "expected bands+wanbao merged lots, got " + snapshot.lotCount());
        assertTrue(snapshot.lots().stream().anyMatch(lot -> lot.id().startsWith("wanbao_")));
    }
}
