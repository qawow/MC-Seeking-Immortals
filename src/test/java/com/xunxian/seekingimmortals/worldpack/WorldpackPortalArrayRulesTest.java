package com.xunxian.seekingimmortals.worldpack;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorldpackPortalArrayRulesTest {
    @Test
    void spiritRealmRegionsRequirePortalArray() {
        WorldpackDataService.Snapshot snapshot = WorldpackDataService.builtin();

        assertFalse(WorldpackGameplayService.requiresPortalArray(region(snapshot, "qinglan_mountains")));
        assertFalse(WorldpackGameplayService.requiresPortalArray(region(snapshot, "tiannan")));
        assertTrue(WorldpackGameplayService.requiresPortalArray(region(snapshot, "tianyuan")));
        assertTrue(WorldpackGameplayService.requiresPortalArray(region(snapshot, "spirit_realm_border")));
        assertTrue(WorldpackGameplayService.requiresPortalArray(region(snapshot, "spirit_fengyuan")));
        assertTrue(WorldpackGameplayService.requiresPortalArray(region(snapshot, "barbarian_wasteland")));
    }

    @Test
    void portalArrayChoosesCurrentRealmLayerDestination() {
        WorldpackDataService.Snapshot snapshot = WorldpackDataService.builtin();

        assertEquals("tianyuan", WorldpackGameplayService.choosePortalArrayDestination("qinglan_mountains", snapshot));
        assertEquals("tianyuan", WorldpackGameplayService.choosePortalArrayDestination("tiannan", snapshot));
        assertEquals("spirit_fengyuan", WorldpackGameplayService.choosePortalArrayDestination("tianyuan", snapshot));
        assertEquals("tianyuan", WorldpackGameplayService.choosePortalArrayDestination("spirit_realm_border", snapshot));
        assertEquals("tianyuan", WorldpackGameplayService.choosePortalArrayDestination("spirit_fengyuan", snapshot));
        assertEquals("tianyuan", WorldpackGameplayService.choosePortalArrayDestination("unknown_region", snapshot));
    }

    @Test
    void demonRiftPortalRequiresSealBreachEventGate() {
        WorldpackDataService.Snapshot snapshot = WorldpackDataService.builtin();
        WorldpackDataService.RegionCard fallenDemon = region(snapshot, "fallen_demon_valley");

        assertTrue(WorldpackGameplayService.requiresDemonRiftEventGate(fallenDemon));
        assertTrue(WorldpackGameplayService.canUseDemonRiftPortalOrigin("great_jin_central", snapshot));
        assertTrue(WorldpackGameplayService.canUseDemonRiftPortalOrigin("dajin", snapshot));
        assertTrue(WorldpackGameplayService.canUseDemonRiftPortalOrigin("kunwu", snapshot));
        assertFalse(WorldpackGameplayService.canUseDemonRiftPortalOrigin("tiannan", snapshot));
        assertEquals("tianyuan", WorldpackGameplayService.choosePortalArrayDestination("great_jin_central", snapshot, false));
        assertEquals("fallen_demon_valley", WorldpackGameplayService.choosePortalArrayDestination("great_jin_central", snapshot, true));
        assertEquals("fallen_demon_valley", WorldpackGameplayService.choosePortalArrayDestination("kunwu", snapshot, true));
        assertEquals("great_jin_central", WorldpackGameplayService.choosePortalArrayDestination("fallen_demon_valley", snapshot, true));
        assertTrue(WorldpackGameplayService.isAncientDemonSealBreach(snapshot,
                new WorldpackSavedData.EventRoll("fallen_demon_valley", "ancient_demon_seal_breach", 12000L)));
        assertFalse(WorldpackGameplayService.isAncientDemonSealBreach(snapshot,
                new WorldpackSavedData.EventRoll("fallen_demon_valley", "demon_qi_surge", 12000L)));
    }

    @Test
    void windFeatherRaftRouteOnlyTargetsTianyuanFromGreatJin() {
        WorldpackDataService.Snapshot snapshot = WorldpackDataService.builtin();
        WorldpackDataService.RegionCard tianyuan = region(snapshot, "tianyuan");
        WorldpackDataService.RegionCard fengyuan = region(snapshot, "spirit_fengyuan");

        assertTrue(WorldpackGameplayService.canUseWindFeatherRaftRoute("great_jin_central", tianyuan));
        assertTrue(WorldpackGameplayService.canUseWindFeatherRaftRoute("dajin", tianyuan));
        assertFalse(WorldpackGameplayService.canUseWindFeatherRaftRoute("tiannan", tianyuan));
        assertFalse(WorldpackGameplayService.canUseWindFeatherRaftRoute("great_jin_central", fengyuan));
    }

    @Test
    void portalArrayFeeOnlyAppliesFromTianyuanToSpiritFengyuan() {
        WorldpackDataService.Snapshot snapshot = WorldpackDataService.builtin();
        WorldpackDataService.RegionCard tianyuan = region(snapshot, "tianyuan");
        WorldpackDataService.RegionCard fengyuan = region(snapshot, "spirit_fengyuan");
        WorldpackDataService.RegionCard border = region(snapshot, "spirit_realm_border");

        assertTrue(WorldpackGameplayService.requiresPortalArrayTravelFee("tianyuan", fengyuan));
        assertFalse(WorldpackGameplayService.requiresPortalArrayTravelFee("spirit_fengyuan", tianyuan));
        assertFalse(WorldpackGameplayService.requiresPortalArrayTravelFee("qinglan_mountains", tianyuan));
        assertFalse(WorldpackGameplayService.requiresPortalArrayTravelFee("tianyuan", border));
        assertFalse(WorldpackGameplayService.requiresPortalArrayTravelFee("tianyuan", null));
    }

    @Test
    void netherRiverFerryFeeConsumesThirtyYinStonesOnlyTowardYinming() {
        WorldpackDataService.Snapshot snapshot = WorldpackDataService.builtin();
        WorldpackDataService.RegionCard yinming = region(snapshot, "yinming");
        WorldpackDataService.RegionCard netherRiver = region(snapshot, "nether_river");
        WorldpackDataService.RegionCard tiannan = region(snapshot, "tiannan");

        assertTrue(WorldpackGameplayService.requiresNetherRiverFerryFee("nether_river", yinming));
        assertEquals(30, WorldpackGameplayService.netherRiverFerryYinStoneFee("nether_river", yinming));
        assertFalse(WorldpackGameplayService.requiresNetherRiverFerryFee("yinming", netherRiver));
        assertEquals(0, WorldpackGameplayService.netherRiverFerryYinStoneFee("yinming", netherRiver));
        assertFalse(WorldpackGameplayService.requiresNetherRiverFerryFee("nether_river", tiannan));
        assertFalse(WorldpackGameplayService.requiresNetherRiverFerryFee("nether_river", null));
    }

    @Test
    void routeRequirementsDescribePortalAndFerryTravelForTheWorldpackUi() {
        WorldpackGameplayService.RouteRequirement mortalToTianyuan =
                WorldpackGameplayService.routeRequirementForDisplay("tiannan", "tianyuan");
        WorldpackGameplayService.RouteRequirement dajinToTianyuan =
                WorldpackGameplayService.routeRequirementForDisplay("great_jin_central", "tianyuan");
        WorldpackGameplayService.RouteRequirement tianyuanToFengyuan =
                WorldpackGameplayService.routeRequirementForDisplay("tianyuan", "spirit_fengyuan");
        WorldpackGameplayService.RouteRequirement greatJinToDemonRift =
                WorldpackGameplayService.routeRequirementForDisplay("great_jin_central", "fallen_demon_valley");
        WorldpackGameplayService.RouteRequirement netherToYinming =
                WorldpackGameplayService.routeRequirementForDisplay("nether_river", "yinming");
        WorldpackGameplayService.RouteRequirement yinmingToNether =
                WorldpackGameplayService.routeRequirementForDisplay("yinming", "nether_river");

        assertEquals(WorldpackGameplayService.ROUTE_HINT_PORTAL_ARRAY, mortalToTianyuan.translationKey());
        assertEquals(WorldpackGameplayService.ROUTE_HINT_WIND_FEATHER_RAFT, dajinToTianyuan.translationKey());
        assertEquals(WorldpackGameplayService.ROUTE_HINT_PORTAL_ARRAY_FEE, tianyuanToFengyuan.translationKey());
        assertEquals("seeking_immortals:alliance_merit_token", tianyuanToFengyuan.itemId());
        assertEquals(WorldpackGameplayService.ROUTE_HINT_DEMON_RIFT_EVENT, greatJinToDemonRift.translationKey());
        assertEquals(WorldpackGameplayService.ROUTE_HINT_NETHER_FERRY_FEE, netherToYinming.translationKey());
        assertEquals(30, netherToYinming.amount());
        assertEquals("seeking_immortals:yin_stone", netherToYinming.itemId());
        assertEquals(WorldpackGameplayService.ROUTE_HINT_NETHER_FERRY_RETURN, yinmingToNether.translationKey());
        assertFalse(WorldpackGameplayService.routeRequirementForDisplay("tiannan", "tiannan").isPresent());
        assertFalse(WorldpackGameplayService.routeRequirementForDisplay("tiannan", "missing_region").isPresent());
    }

    @Test
    void diyuanUsesPermitAsSecretRealmTicket() {
        WorldpackDataService.Snapshot snapshot = WorldpackDataService.builtin();

        WorldpackDataService.SecretRealm diyuan = snapshot.findSecretRealm("diyuan")
                .orElseThrow(() -> new AssertionError("Missing Diyuan secret realm"));
        WorldpackDataService.SecretRealm demonGold = snapshot.findSecretRealm("demon_gold_mountain")
                .orElseThrow(() -> new AssertionError("Missing Demon Gold Mountain secret realm"));

        assertEquals("seeking_immortals:diyuan_permit", diyuan.ticketItem());
        assertEquals("seeking_immortals:immortal_jade", demonGold.ticketItem());
    }

    @Test
    void defaultDimensionAnchorsUseGeneratedPortalPlatforms() {
        WorldpackSavedData.Anchor tianyuan = new WorldpackSavedData.Anchor(
                "tianyuan_anchor", "seeking_immortals:tianyuan", 0.5D, 82.0D, 0.5D, 0.0F, 0.0F);
        WorldpackSavedData.Anchor fengyuan = new WorldpackSavedData.Anchor(
                "fengyuan_anchor", "seeking_immortals:spirit_fengyuan", 0.5D, 96.0D, 0.5D, 0.0F, 0.0F);
        WorldpackSavedData.Anchor yinming = new WorldpackSavedData.Anchor(
                "yinming_gate_anchor", "seeking_immortals:yin_ming_pocket", 0.5D, 70.0D, 0.5D, 0.0F, 0.0F);
        WorldpackSavedData.Anchor netherRiver = new WorldpackSavedData.Anchor(
                "nether_ferry_anchor", "seeking_immortals:nether_river_pocket", 160.5D, 74.0D, 0.5D, 0.0F, 0.0F);
        WorldpackSavedData.Anchor fallenDemon = new WorldpackSavedData.Anchor(
                "fallen_demon_anchor", "seeking_immortals:demon_rift", 0.5D, 78.0D, 0.5D, 0.0F, 0.0F);
        WorldpackSavedData.Anchor customSamePlace = new WorldpackSavedData.Anchor(
                "custom_anchor", "seeking_immortals:tianyuan", 0.5D, 82.0D, 0.5D, 0.0F, 0.0F);
        WorldpackSavedData.Anchor movedDefault = new WorldpackSavedData.Anchor(
                "tianyuan_anchor", "seeking_immortals:tianyuan", 8.5D, 82.0D, 0.5D, 0.0F, 0.0F);

        assertTrue(WorldpackGameplayService.usesDefaultPortalPlatform(tianyuan));
        assertTrue(WorldpackGameplayService.usesDefaultPortalPlatform(fengyuan));
        assertTrue(WorldpackGameplayService.usesDefaultPortalPlatform(yinming));
        assertTrue(WorldpackGameplayService.usesDefaultPortalPlatform(netherRiver));
        assertTrue(WorldpackGameplayService.usesDefaultPortalPlatform(fallenDemon));
        assertEquals(new BlockPos(0, 81, 0), WorldpackGameplayService.defaultPortalPlatformBaseCenter(tianyuan));
        assertEquals(new BlockPos(0, 95, 0), WorldpackGameplayService.defaultPortalPlatformBaseCenter(fengyuan));
        assertEquals(new BlockPos(0, 69, 0), WorldpackGameplayService.defaultPortalPlatformBaseCenter(yinming));
        assertEquals(new BlockPos(160, 73, 0), WorldpackGameplayService.defaultPortalPlatformBaseCenter(netherRiver));
        assertEquals(new BlockPos(0, 77, 0), WorldpackGameplayService.defaultPortalPlatformBaseCenter(fallenDemon));
        assertFalse(WorldpackGameplayService.usesDefaultPortalPlatform(customSamePlace));
        assertFalse(WorldpackGameplayService.usesDefaultPortalPlatform(movedDefault));
    }

    private static WorldpackDataService.RegionCard region(WorldpackDataService.Snapshot snapshot, String id) {
        return snapshot.findRegion(id).orElseThrow(() -> new AssertionError("Missing region " + id));
    }
}
