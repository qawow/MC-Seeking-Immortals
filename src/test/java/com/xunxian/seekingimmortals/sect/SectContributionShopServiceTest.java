package com.xunxian.seekingimmortals.sect;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SectContributionShopServiceTest {
    @Test
    void loadsContributionShopsAndShelves() {
        SectContributionShopService.Snapshot snapshot = SectContributionShopService.builtin();
        assertTrue(snapshot.shopCount() >= 3, "contribution shops");
        assertTrue(snapshot.shelfFactionCount() >= 3, "shelf factions");
        assertTrue(SectContributionShopService.shopForFaction("huangfeng_valley").isPresent()
                || SectContributionShopService.shopForFaction("huangfeng").isPresent()
                || !snapshot.shopsByFaction().isEmpty());
        assertTrue(SectContributionShopService.shelvesForFaction("huangfeng").isPresent()
                || SectContributionShopService.shelvesForFaction("huangfeng_valley").isPresent()
                || !snapshot.shelvesByFaction().isEmpty());
    }

    @Test
    void redlineNoInfiniteContributionToSpiritStone() {
        assertFalse(SectContributionShopService.allowsInfiniteContributionToSpiritStoneExchange());
        assertTrue(SectContributionShopService.isContributionCurrency("sect_contribution"));
        assertTrue(SectContributionShopService.isContributionCurrency("sect_contribution_point"));
        assertTrue(SectContributionShopService.isNeverListItem("掌天瓶"));
        assertTrue(SectContributionShopService.isNeverListItem("palm_heaven_bottle"));
        assertTrue(SectContributionShopService.isNeverListItem("绿液"));
        assertFalse(SectContributionShopService.isNeverListItem("spirit_grass"));
    }

    @Test
    void shelfRepGateFiltersTiers() {
        var shelves = SectContributionShopService.openShelves("huangfeng", 0);
        var high = SectContributionShopService.openShelves("huangfeng", 60);
        // If shelves present, higher rep should open at least as many.
        if (!SectContributionShopService.shelvesForFaction("huangfeng").isEmpty()
                || SectContributionShopService.shelvesForFaction("huangfeng").isPresent()) {
            assertTrue(high.size() >= shelves.size());
        }
    }
}
