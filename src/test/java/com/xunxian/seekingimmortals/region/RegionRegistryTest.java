package com.xunxian.seekingimmortals.region;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegionRegistryTest {
    @Test
    void allShippedRegionCardsAreParsed() {
        RegionRegistry.Snapshot snapshot = RegionRegistry.builtin();
        assertEquals(22, snapshot.cardCount(), "expected 22 author region cards");
        List<String> expected = List.of(
                "barbarian_wasteland", "chaotic_sea", "dajin", "extreme_west_thousand_bamboo",
                "fallen_demon_valley", "great_jin_central", "inverse_star_hideout", "kunwu",
                "mulan", "mulan_grassland", "nether_river", "outer_sea_market", "qixuan_village",
                "spirit_fengyuan", "spirit_realm_border", "star_palace_city", "tianlan", "tiannan",
                "tiannan_north_waste", "tianyuan", "wutu_border", "yinming");
        Set<String> cardIds = new HashSet<>(snapshot.cardIds());
        for (String id : expected) {
            assertTrue(cardIds.contains(id) || snapshot.find(id).isPresent(), "missing card " + id);
            assertTrue(snapshot.find(id).isPresent(), "card not registered " + id);
            assertTrue(snapshot.find(id).orElseThrow().hasCard());
        }
        // File stem extreme_west_thousand_bamboo declares id extreme_west; both must resolve.
        assertTrue(snapshot.find("extreme_west").isPresent());
        assertTrue(snapshot.find("extreme_west_thousand_bamboo").isPresent());
    }

    @Test
    void worldpackRegionsRemainReachableThroughRegistry() {
        RegionRegistry.Snapshot snapshot = RegionRegistry.builtin();
        assertTrue(snapshot.find("qinglan_mountains").isPresent());
        assertTrue(snapshot.find("qinglan_mountains").orElseThrow().hasWorldpack());
        assertTrue(snapshot.find("tiannan").isPresent());
        assertTrue(RegionRegistry.auraMultiplier("tianyuan") >= 1.5D);
    }

    @Test
    void itemsByRegionQueryWorks() {
        assertFalse(RegionItemsService.itemsForRegion("tiannan").isEmpty());
        assertTrue(RegionItemsService.itemsForRegion("tiannan").stream()
                .anyMatch(item -> "yellow_essence_grass".equals(item.id())));
        assertTrue(RegionItemsService.itemsForRegion("missing_region_xyz").isEmpty());
        assertTrue(RegionItemsService.builtin().regionCount() >= 8);
    }

    @Test
    void biomeMapBindsModBiomesToRegions() {
        RegionBiomeMap.Snapshot biomes = RegionBiomeMap.builtin();
        assertTrue(biomes.bindingCount() >= 10);
        assertEquals("tiannan", biomes.regionForBiome("seeking_immortals:tiannan_forest").orElse(""));
        assertEquals("chaotic_sea", biomes.regionForBiome("seeking_immortals:chaotic_sea_shallow").orElse(""));
        assertFalse(biomes.biomesForRegion("mulan").isEmpty());
    }

    @Test
    void travelRouteGraphLoadsHubsAndTradeEdges() {
        TravelRouteGraph.Snapshot graph = TravelRouteGraph.builtin();
        assertTrue(graph.hubCount() >= 20);
        assertTrue(graph.routeCount() >= 20);
        assertFalse(graph.routesBetween("tiannan", "chaotic_sea").isEmpty());
        assertTrue(graph.isConnected("tiannan", "chaotic_sea"));
    }

    @Test
    void dailyEventSchedulerExposesExpandedEventsAndHooks() {
        assertTrue(DailyEventScheduler.expandedEventCount() >= 50);
        List<String> fired = new java.util.ArrayList<>();
        DailyEventHook hook = (regionId, eventId) -> fired.add(regionId + ":" + eventId);
        DailyEventScheduler.registerHook(hook);
        try {
            DailyEventScheduler.onDailyEvent("tiannan", "wandering_merchant");
            assertEquals(List.of("tiannan:wandering_merchant"), fired);
            assertTrue(RegionEventConfig.isDailyEventsEnabled());
            RegionEventConfig.setDailyEventsEnabled(false);
            assertFalse(RegionEventConfig.isDailyEventsEnabled());
            RegionEventConfig.setDailyEventsEnabled(true);
        } finally {
            DailyEventScheduler.clearHooks();
        }
        assertEquals(0, DailyEventScheduler.hookCount());
    }

    @Test
    void expandedCandidatesCoverMultiRegionTextEvents() {
        var snapshot = com.xunxian.seekingimmortals.worldpack.WorldpackDataService.builtin();
        var dajin = DailyEventScheduler.expandedCandidates("dajin", snapshot);
        assertTrue(dajin.stream().anyMatch(event -> "wandering_merchant".equals(event.id())
                || "auction_notice".equals(event.id())
                || "talisman_master_visit".equals(event.id())));
        var star = DailyEventScheduler.expandedCandidates("tianyuan", snapshot);
        assertFalse(star.isEmpty());
    }

    @Test
    void regionMultiplierClampsAndApplies() {
        assertEquals(1.0D, SpiritualAuraCompat.regionMultiplierOrDefault(""), 1e-9);
        double tianyuan = com.xunxian.seekingimmortals.spiritual.SpiritualAuraManager.getRegionMultiplier("tianyuan");
        assertTrue(tianyuan >= 1.5D);
        assertTrue(tianyuan <= 2.5D);
    }

    /** Tiny local helper to keep SpiritualAuraManager import usage intentional. */
    private static final class SpiritualAuraCompat {
        private static double regionMultiplierOrDefault(String regionId) {
            return com.xunxian.seekingimmortals.spiritual.SpiritualAuraManager.getRegionMultiplier(regionId);
        }
    }
}
