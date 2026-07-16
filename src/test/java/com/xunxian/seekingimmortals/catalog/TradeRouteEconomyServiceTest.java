package com.xunxian.seekingimmortals.catalog;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TradeRouteEconomyServiceTest {
    @Test
    void loadsTradeRoutesAndHubs() {
        TradeRouteEconomyService.Snapshot snapshot = TradeRouteEconomyService.builtin();
        assertTrue(snapshot.routeCount() >= 13);
        assertFalse(snapshot.auctionHubs().isEmpty());
        TradeRouteEconomyService.TradeRoute route = snapshot.find("tiannan_to_chaotic_sea").orElseThrow();
        assertEquals("tiannan", route.fromRegion());
        assertEquals("chaotic_sea", route.toRegion());
        assertTrue(route.feeLowStone() > 0);
    }

    @Test
    void regionQueriesAndPriceModifierStayBounded() {
        assertFalse(TradeRouteEconomyService.from("tiannan").isEmpty());
        double mod = TradeRouteEconomyService.priceModifier("dajin", "artifact");
        assertTrue(mod >= 0.75D && mod <= 1.75D);
        assertTrue(TradeRouteEconomyService.shopRegion("dajin_wanbao_pavilion").isPresent());
    }
}
