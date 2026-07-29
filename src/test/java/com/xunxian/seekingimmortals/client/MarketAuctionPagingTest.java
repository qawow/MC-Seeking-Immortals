package com.xunxian.seekingimmortals.client;

import com.xunxian.seekingimmortals.network.SyncAuctionLadderPacket;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the two halls keep distinct pagination models after the journal-shell migration:
 * Market = client PAGE_SIZE=6 slices; Auction = server page index + in-page scroll height.
 */
class MarketAuctionPagingTest {
    @AfterEach
    void resetAuction() {
        ClientAuctionLadderData.reset();
    }

    @Test
    void marketClientPagingUsesFixedPageSizeSix() {
        assertEquals(6, MarketHallScreen.pageSize());

        assertEquals(0, MarketHallScreen.maxPage(0));
        assertEquals(0, MarketHallScreen.maxPage(1));
        assertEquals(0, MarketHallScreen.maxPage(6));
        assertEquals(1, MarketHallScreen.maxPage(7));
        assertEquals(2, MarketHallScreen.maxPage(13));
        assertEquals(2, MarketHallScreen.maxPage(18));
        assertEquals(3, MarketHallScreen.maxPage(19));
    }

    @Test
    void marketClientPageClampAndSliceStayOnClientEntries() {
        // 13 entries → pages 0..2 with slices [0,6), [6,12), [12,13)
        int count = 13;
        int maxPage = MarketHallScreen.maxPage(count);
        assertEquals(2, maxPage);
        assertEquals(0, MarketHallScreen.clampPage(-3, maxPage));
        assertEquals(2, MarketHallScreen.clampPage(99, maxPage));
        assertEquals(1, MarketHallScreen.clampPage(1, maxPage));

        assertEquals(0, MarketHallScreen.pageStart(0));
        assertEquals(6, MarketHallScreen.pageEnd(0, count));
        assertEquals(6, MarketHallScreen.pageItemCount(0, count));

        assertEquals(6, MarketHallScreen.pageStart(1));
        assertEquals(12, MarketHallScreen.pageEnd(1, count));
        assertEquals(6, MarketHallScreen.pageItemCount(1, count));

        assertEquals(12, MarketHallScreen.pageStart(2));
        assertEquals(13, MarketHallScreen.pageEnd(2, count));
        assertEquals(1, MarketHallScreen.pageItemCount(2, count));

        // Content height for a page depends only on that page's row count, not total entries.
        assertEquals(MarketHallScreen.calculateContentHeight(6),
                MarketHallScreen.calculateContentHeight(MarketHallScreen.pageItemCount(0, count)));
        assertEquals(MarketHallScreen.calculateContentHeight(1),
                MarketHallScreen.calculateContentHeight(MarketHallScreen.pageItemCount(2, count)));
    }

    @Test
    void marketPageControlsWouldEnableBasedOnClientMaxPage() {
        int maxPage = MarketHallScreen.maxPage(13);
        assertFalse(MarketHallScreen.clampPage(0, maxPage) > 0); // previous inactive on first page
        assertTrue(0 < maxPage); // next active on first page
        assertTrue(MarketHallScreen.clampPage(2, maxPage) > 0); // previous active on last page
        assertFalse(2 < maxPage); // next inactive on last page
    }

    @Test
    void auctionServerPagingUsesSnapshotPageAndMaxPage() {
        ClientAuctionLadderData.set(new SyncAuctionLadderPacket(0, 6, 20, lots(6)));
        ClientAuctionLadderData.Snapshot first = ClientAuctionLadderData.get();
        assertEquals(0, first.page());
        assertEquals(3, first.maxPage()); // (20-1)/6 = 3
        assertEquals(6, first.lots().size());
        assertFalse(AuctionHallScreen.canPagePrevious(first.page()));
        assertTrue(AuctionHallScreen.canPageNext(first.synced(), first.page(), first.maxPage()));
        assertEquals(0, AuctionHallScreen.previousPage(first.page()));
        assertEquals(1, AuctionHallScreen.nextPage(first.page()));

        ClientAuctionLadderData.set(new SyncAuctionLadderPacket(3, 6, 20, lots(2)));
        ClientAuctionLadderData.Snapshot last = ClientAuctionLadderData.get();
        assertEquals(3, last.page());
        assertEquals(3, last.maxPage());
        assertEquals(2, last.lots().size());
        assertTrue(AuctionHallScreen.canPagePrevious(last.page()));
        assertFalse(AuctionHallScreen.canPageNext(last.synced(), last.page(), last.maxPage()));
        assertEquals(2, AuctionHallScreen.previousPage(last.page()));
        assertEquals(4, AuctionHallScreen.nextPage(last.page())); // request may be clamped server-side
    }

    @Test
    void auctionNextStaysEnabledWhileUnsyncedSoPageRequestsCanStillFire() {
        ClientAuctionLadderData.reset();
        ClientAuctionLadderData.Snapshot empty = ClientAuctionLadderData.get();
        assertFalse(empty.synced());
        assertTrue(AuctionHallScreen.canPageNext(empty.synced(), empty.page(), empty.maxPage()));
    }

    @Test
    void auctionBidAvailabilityWaitsForSyncOrTimeoutInsteadOfAllowingReplay() {
        assertTrue(AuctionHallScreen.canBid(false, false));
        assertFalse(AuctionHallScreen.canBid(false, true));
        assertFalse(AuctionHallScreen.canBid(true, false));

        long before = ClientAuctionLadderData.revision();
        SyncAuctionLadderPacket snapshot = new SyncAuctionLadderPacket(0, 6, 1, lots(1));
        ClientAuctionLadderData.set(snapshot);
        long firstSync = ClientAuctionLadderData.revision();
        ClientAuctionLadderData.set(snapshot);
        assertTrue(firstSync > before);
        assertTrue(ClientAuctionLadderData.revision() > firstSync,
                "an unchanged rejection snapshot must still release a pending bid button");
    }

    @Test
    void auctionInPageScrollHeightFollowsCurrentServerPageLotCountOnly() {
        // Server page 0 with 6 lots vs page 3 with 2 lots — content height differs by row count,
        // independent of totalLots / maxPage (which only gate the server page buttons).
        int fullPageHeight = AuctionHallScreen.calculateContentHeight(6);
        int shortPageHeight = AuctionHallScreen.calculateContentHeight(2);
        assertTrue(fullPageHeight > shortPageHeight);

        assertEquals(0, AuctionHallScreen.clampScroll(-5, fullPageHeight, 80));
        assertEquals(fullPageHeight - 80, AuctionHallScreen.clampScroll(9999, fullPageHeight, 80));
        assertEquals(0, AuctionHallScreen.clampScroll(40, shortPageHeight, shortPageHeight + 10));
    }

    @Test
    void marketAndAuctionPaginationModelsStayDistinct() {
        // Market never consults ClientAuctionLadderData; Auction never uses Market PAGE_SIZE for page index.
        assertEquals(6, MarketHallScreen.pageSize());
        ClientAuctionLadderData.set(new SyncAuctionLadderPacket(1, 8, 25, lots(8)));
        ClientAuctionLadderData.Snapshot auction = ClientAuctionLadderData.get();
        assertEquals(1, auction.page());
        assertEquals(8, auction.pageSize()); // server pageSize may differ from market 6
        assertEquals(3, auction.maxPage()); // (25-1)/8
        // Market maxPage for 25 entries with PAGE_SIZE=6 is different from auction maxPage with pageSize=8.
        assertEquals(4, MarketHallScreen.maxPage(25));
        assertNotEquals(MarketHallScreen.maxPage(25), auction.maxPage());
    }

    private static void assertNotEquals(int a, int b) {
        org.junit.jupiter.api.Assertions.assertNotEquals(a, b);
    }

    private static List<SyncAuctionLadderPacket.LotBid> lots(int count) {
        List<SyncAuctionLadderPacket.LotBid> list = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            list.add(new SyncAuctionLadderPacket.LotBid(
                    "lot_" + i, "Lot " + i, 10 + i, 20 + i, 1, 100, "leader", false));
        }
        return list;
    }
}
