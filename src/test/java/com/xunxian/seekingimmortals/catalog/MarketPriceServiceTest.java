package com.xunxian.seekingimmortals.catalog;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarketPriceServiceTest {
    @Test
    void loadsRegionsAndTags() {
        MarketPriceService.Snapshot snapshot = MarketPriceService.builtin();
        assertTrue(snapshot.regions().size() >= 10, "expected market regions from v100");
        assertTrue(snapshot.taggedItemCount() >= 100, "expected item economy tags");
        assertTrue(snapshot.blockedItemCount() >= 1);
        assertTrue(snapshot.commodityMaster().size() >= 4);
    }

    @Test
    void blocksUniqueItemsFromOpenMarketAndAuction() {
        assertTrue(MarketPriceService.isBlockedFromOpenMarket("palm_bottle"));
        assertTrue(MarketPriceService.isBlockedFromOpenMarket("green_liquid"));
        assertFalse(MarketPriceService.isAuctionEligible("palm_bottle"));
        assertTrue(MarketPriceService.isAuctionEligible("spirit_stone_shard")
                || !MarketPriceService.findTag("spirit_stone_shard").isPresent()
                || !MarketPriceService.isBlockedFromOpenMarket("spirit_stone_shard"));
    }

    @Test
    void suggestedCostFallsBackSafely() {
        int cost = MarketPriceService.suggestedShopCost("totally_unknown_item_xyz", "tiannan", 42);
        assertTrue(cost >= 1);
        int priced = MarketPriceService.applyPricing("unknown_item", "tiannan", 10, 1.1D, 1.0D);
        assertTrue(priced >= 1);
    }
}
