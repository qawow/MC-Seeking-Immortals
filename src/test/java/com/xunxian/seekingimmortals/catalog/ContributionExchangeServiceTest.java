package com.xunxian.seekingimmortals.catalog;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContributionExchangeServiceTest {
    @Test
    void forbidsInfiniteStoneSwap() {
        assertFalse(ContributionExchangeService.isInfiniteStoneSwapAllowed());
    }

    @Test
    void loadsFactionRatesAndMeritCatalogs() {
        assertTrue(ContributionExchangeService.factionRates().size() >= 3);
        assertEquals(1.0D, ContributionExchangeService.lowStonePerContribution("huangfeng_valley"), 0.001D);
        assertTrue(ContributionExchangeService.estimateLowStoneValue("huangfeng_valley", 100) >= 100L);
        assertFalse(ContributionExchangeService.catalogFor("star_palace").isEmpty()
                || ContributionExchangeService.catalogFor("patrol_merit").isEmpty()
                || ContributionExchangeService.catalogFor("tianyuan").isEmpty()
                || ContributionExchangeService.catalogFor("merit_points").isEmpty()
                || ContributionExchangeService.catalogFor("inverse_star").isEmpty()
                || ContributionExchangeService.catalogFor("smuggle_credit").isEmpty());
    }

    @Test
    void itemEquivHintsPresent() {
        assertTrue(ContributionExchangeService.itemEquivMid("foundation_pill").isPresent());
    }
}
