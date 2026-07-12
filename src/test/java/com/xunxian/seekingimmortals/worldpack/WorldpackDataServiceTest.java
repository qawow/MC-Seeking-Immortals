package com.xunxian.seekingimmortals.worldpack;

import com.xunxian.seekingimmortals.cultivation.PlayerCultivation;
import com.xunxian.seekingimmortals.cultivation.Realm;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorldpackDataServiceTest {
    @Test
    void builtInWorldpackSnapshotLoads() {
        WorldpackDataService.Snapshot snapshot = WorldpackDataService.builtin();

        assertFalse(snapshot.regions().isEmpty());
        assertFalse(snapshot.secretRealms().isEmpty());
        assertFalse(snapshot.dailyEvents().isEmpty());
        assertTrue(snapshot.findRegion("qinglan_mountains").isPresent());
        assertTrue(snapshot.findSecretRealm("mist_cave_trial").isPresent());
    }

    @Test
    void builtInWorldpackReferencesExistingRegions() {
        WorldpackDataService.Snapshot snapshot = WorldpackDataService.builtin();

        for (WorldpackDataService.SecretRealm realm : snapshot.secretRealms()) {
            assertTrue(snapshot.findRegion(realm.regionId()).isPresent(), realm.id());
        }
        for (WorldpackDataService.DailyEvent event : snapshot.dailyEvents()) {
            assertTrue(snapshot.findRegion(event.regionId()).isPresent(), event.id());
        }
    }

    @Test
    void builtInWorldpackIncludesFactionConflictEvents() {
        WorldpackDataService.Snapshot snapshot = WorldpackDataService.builtin();

        WorldpackDataService.DailyEvent jointDefense = snapshot.findDailyEvent("seven_sects_joint_defense")
                .orElseThrow(() -> new AssertionError("Missing seven_sects_joint_defense"));
        assertEquals("tiannan", jointDefense.regionId());
        assertTrue(jointDefense.effects().contains("aura_plus_5"));
        assertTrue(jointDefense.effects().contains("rare_loot_hint"));

        WorldpackDataService.DailyEvent blockade = snapshot.findDailyEvent("chaotic_sea_blockade")
                .orElseThrow(() -> new AssertionError("Missing chaotic_sea_blockade"));
        assertEquals("chaotic_sea", blockade.regionId());
        assertEquals(List.of("trade_risk_up"), blockade.effects());

        WorldpackDataService.DailyEvent ritual = snapshot.findDailyEvent("tianlan_holy_beast_ritual")
                .orElseThrow(() -> new AssertionError("Missing tianlan_holy_beast_ritual"));
        assertEquals("tianlan", ritual.regionId());
        assertTrue(ritual.effects().contains("secret_realm_ticket_hint"));

        WorldpackDataService.DailyEvent tianlanScout = snapshot.findDailyEvent("tianlan_mulan_scout_clash")
                .orElseThrow(() -> new AssertionError("Missing tianlan_mulan_scout_clash"));
        assertEquals("tianlan", tianlanScout.regionId());
        assertEquals(9, tianlanScout.weight());
        assertEquals(18000, tianlanScout.durationTicks());
        assertEquals(List.of("trade_risk_up", "rare_loot_hint"), tianlanScout.effects());
    }

    @Test
    void builtInWorldpackIncludesTradeRouteEvents() {
        WorldpackDataService.Snapshot snapshot = WorldpackDataService.builtin();

        WorldpackDataService.DailyEvent tiannanSeaRoute = snapshot.findDailyEvent("tiannan_to_chaotic_sea")
                .orElseThrow(() -> new AssertionError("Missing tiannan_to_chaotic_sea"));
        assertEquals("tiannan", tiannanSeaRoute.regionId());
        assertTrue(tiannanSeaRoute.effects().contains("trade_risk_up"));
        assertTrue(tiannanSeaRoute.effects().contains("secret_realm_ticket_hint"));

        WorldpackDataService.DailyEvent netherFerry = snapshot.findDailyEvent("nether_river_ferry")
                .orElseThrow(() -> new AssertionError("Missing nether_river_ferry"));
        assertEquals("nether_river", netherFerry.regionId());
        assertTrue(netherFerry.effects().contains("secret_realm_ticket_hint"));

        WorldpackDataService.DailyEvent meritConvoy = snapshot.findDailyEvent("route_tianyuan_merit_convoy")
                .orElseThrow(() -> new AssertionError("Missing route_tianyuan_merit_convoy"));
        assertEquals("tianyuan", meritConvoy.regionId());
        assertTrue(meritConvoy.effects().contains("aura_plus_5"));
    }

    @Test
    void builtInWorldpackIncludesOuterSeaPearlEvents() {
        WorldpackDataService.Snapshot snapshot = WorldpackDataService.builtin();

        WorldpackDataService.RegionCard outerSeaMarket = snapshot.findRegion("outer_sea_market")
                .orElseThrow(() -> new AssertionError("Missing outer_sea_market"));
        assertEquals("foundation_establishment", outerSeaMarket.minRealm());
        assertEquals("outer_sea_market_anchor", outerSeaMarket.travelAnchor());
        assertTrue(outerSeaMarket.tags().contains("pearl"));

        WorldpackDataService.DailyEvent pearlBoats = snapshot.findDailyEvent("pearl_diving_mortals")
                .orElseThrow(() -> new AssertionError("Missing pearl_diving_mortals"));
        assertEquals("outer_sea_market", pearlBoats.regionId());
        assertEquals(6, pearlBoats.weight());
        assertTrue(pearlBoats.effects().contains("rare_loot_hint"));

        WorldpackDataService.DailyEvent stormShelter = snapshot.findDailyEvent("pearl_storm_shelter")
                .orElseThrow(() -> new AssertionError("Missing pearl_storm_shelter"));
        assertEquals("outer_sea_market", stormShelter.regionId());
        assertEquals(List.of("trade_risk_up"), stormShelter.effects());

        WorldpackDataService.DailyEvent taxDispute = snapshot.findDailyEvent("pearl_tax_dispute")
                .orElseThrow(() -> new AssertionError("Missing pearl_tax_dispute"));
        assertEquals("outer_sea_market", taxDispute.regionId());
        assertTrue(taxDispute.effects().contains("trade_risk_up"));
        assertTrue(taxDispute.effects().contains("rare_loot_hint"));
    }

    @Test
    void builtInWorldpackIncludesStarPalaceCityRegion() {
        WorldpackDataService.Snapshot snapshot = WorldpackDataService.builtin();

        WorldpackDataService.RegionCard starPalaceCity = snapshot.findRegion("star_palace_city")
                .orElseThrow(() -> new AssertionError("Missing star_palace_city"));
        assertEquals("foundation_establishment", starPalaceCity.minRealm());
        assertEquals("star_palace_city_anchor", starPalaceCity.travelAnchor());
        assertTrue(starPalaceCity.tags().contains("star_palace"));
        assertTrue(starPalaceCity.tags().contains("auction"));
        assertTrue(starPalaceCity.tags().contains("patrol_board"));
    }

    @Test
    void builtInWorldpackIncludesQixuanVillageRegion() {
        WorldpackDataService.Snapshot snapshot = WorldpackDataService.builtin();

        WorldpackDataService.RegionCard qixuanVillage = snapshot.findRegion("qixuan_village")
                .orElseThrow(() -> new AssertionError("Missing qixuan_village"));
        assertEquals("qi_refining", qixuanVillage.minRealm());
        assertEquals("qixuan_village_anchor", qixuanVillage.travelAnchor());
        assertEquals(0.75D, qixuanVillage.auraMultiplier());
        assertTrue(qixuanVillage.tags().contains("starter"));
        assertTrue(qixuanVillage.tags().contains("qixuan"));
        assertTrue(qixuanVillage.tags().contains("quest"));
    }

    @Test
    void builtInWorldpackIncludesTiannanNorthWasteRegion() {
        WorldpackDataService.Snapshot snapshot = WorldpackDataService.builtin();

        WorldpackDataService.RegionCard northWaste = snapshot.findRegion("tiannan_north_waste")
                .orElseThrow(() -> new AssertionError("Missing tiannan_north_waste"));
        assertEquals("qi_refining", northWaste.minRealm());
        assertEquals("tiannan_north_waste_anchor", northWaste.travelAnchor());
        assertEquals(0.7D, northWaste.auraMultiplier());
        assertTrue(northWaste.tags().contains("demonic"));
        assertTrue(northWaste.tags().contains("north_waste"));
        assertTrue(northWaste.tags().contains("low_corrupt"));
        assertTrue(northWaste.tags().contains("six_sects"));
    }

    @Test
    void builtInWorldpackIncludesInverseStarHideoutRegion() {
        WorldpackDataService.Snapshot snapshot = WorldpackDataService.builtin();

        WorldpackDataService.RegionCard hideout = snapshot.findRegion("inverse_star_hideout")
                .orElseThrow(() -> new AssertionError("Missing inverse_star_hideout"));
        assertEquals("foundation_establishment", hideout.minRealm());
        assertEquals("inverse_star_hideout_anchor", hideout.travelAnchor());
        assertTrue(hideout.tags().contains("inverse_star"));
        assertTrue(hideout.tags().contains("hidden"));
        assertTrue(hideout.tags().contains("black_market"));
    }

    @Test
    void builtInWorldpackIncludesWutuBorderRegionAndRaidEvent() {
        WorldpackDataService.Snapshot snapshot = WorldpackDataService.builtin();

        WorldpackDataService.RegionCard wutuBorder = snapshot.findRegion("wutu_border")
                .orElseThrow(() -> new AssertionError("Missing wutu_border"));
        assertEquals("foundation_establishment", wutuBorder.minRealm());
        assertEquals("wutu_border_anchor", wutuBorder.travelAnchor());
        assertEquals(0.85D, wutuBorder.auraMultiplier());
        assertTrue(wutuBorder.tags().contains("wutu"));
        assertTrue(wutuBorder.tags().contains("feud"));
        assertTrue(wutuBorder.tags().contains("faction_war"));

        WorldpackDataService.DailyEvent raid = snapshot.findDailyEvent("wutu_raid_mulan_camp")
                .orElseThrow(() -> new AssertionError("Missing wutu_raid_mulan_camp"));
        assertEquals("wutu_border", raid.regionId());
        assertEquals(5, raid.weight());
        assertEquals(18000, raid.durationTicks());
        assertTrue(raid.effects().contains("trade_risk_up"));
        assertTrue(raid.effects().contains("rare_loot_hint"));
    }

    @Test
    void builtInWorldpackIncludesMulanRegionAndSourceCardEvents() {
        WorldpackDataService.Snapshot snapshot = WorldpackDataService.builtin();

        WorldpackDataService.RegionCard mulan = snapshot.findRegion("mulan")
                .orElseThrow(() -> new AssertionError("Missing mulan"));
        assertEquals("foundation_establishment", mulan.minRealm());
        assertEquals("mulan_steppe_anchor", mulan.travelAnchor());
        assertEquals(0.95D, mulan.auraMultiplier());
        assertTrue(mulan.tags().contains("mulan"));
        assertTrue(mulan.tags().contains("fashi"));
        assertTrue(mulan.tags().contains("holy_bird"));
        assertTrue(mulan.tags().contains("beast_taming"));
        assertTrue(mulan.tags().contains("faction_war"));
        assertTrue(WorldpackGameplayService.meetsMinRealm(Realm.FOUNDATION_ESTABLISHMENT, mulan.minRealm()));
        assertFalse(WorldpackGameplayService.meetsMinRealm(Realm.QI_REFINING, mulan.minRealm()));

        WorldpackDataService.DailyEvent migration = snapshot.findDailyEvent("nomad_migration")
                .orElseThrow(() -> new AssertionError("Missing nomad_migration"));
        assertEquals("mulan", migration.regionId());
        assertEquals(4, migration.weight());
        assertEquals(24000, migration.durationTicks());
        assertTrue(migration.effects().contains("trade_risk_up"));
        assertTrue(migration.effects().contains("rare_loot_hint"));

        WorldpackDataService.DailyEvent patrol = snapshot.findDailyEvent("fashi_patrol")
                .orElseThrow(() -> new AssertionError("Missing fashi_patrol"));
        assertEquals("mulan", patrol.regionId());
        assertEquals(5, patrol.weight());
        assertEquals(18000, patrol.durationTicks());
        assertEquals(List.of("trade_risk_up", "rare_loot_hint"), patrol.effects());

        WorldpackDataService.DailyEvent festival = snapshot.findDailyEvent("beast_tame_festival")
                .orElseThrow(() -> new AssertionError("Missing beast_tame_festival"));
        assertEquals("mulan", festival.regionId());
        assertEquals(3, festival.weight());
        assertEquals(24000, festival.durationTicks());
        assertTrue(festival.effects().contains("aura_plus_5"));
        assertTrue(festival.effects().contains("rare_loot_hint"));

        WorldpackDataService.DailyEvent raid = snapshot.findDailyEvent("grassland_beast_raid")
                .orElseThrow(() -> new AssertionError("Missing grassland_beast_raid"));
        assertEquals("mulan", raid.regionId());
        assertEquals(4, raid.weight());
        assertEquals(18000, raid.durationTicks());
        assertEquals(List.of("trade_risk_up", "rare_loot_hint"), raid.effects());
    }

    @Test
    void builtInWorldpackIncludesSpiritRealmBorderRegionAndEvents() {
        WorldpackDataService.Snapshot snapshot = WorldpackDataService.builtin();

        WorldpackDataService.RegionCard border = snapshot.findRegion("spirit_realm_border")
                .orElseThrow(() -> new AssertionError("Missing spirit_realm_border"));
        assertEquals("soul_transformation", border.minRealm());
        assertEquals("spirit_realm_border_anchor", border.travelAnchor());
        assertEquals(1.3D, border.auraMultiplier());
        assertTrue(border.tags().contains("spirit_realm"));
        assertTrue(border.tags().contains("wild_land"));
        assertTrue(border.tags().contains("spatial_rift"));
        assertTrue(WorldpackGameplayService.meetsMinRealm(Realm.SOUL_TRANSFORMATION, border.minRealm()));
        assertFalse(WorldpackGameplayService.meetsMinRealm(Realm.NASCENT_SOUL, border.minRealm()));

        WorldpackDataService.DailyEvent beastHorde = snapshot.findDailyEvent("wild_land_beast_horde")
                .orElseThrow(() -> new AssertionError("Missing wild_land_beast_horde"));
        assertEquals("spirit_realm_border", beastHorde.regionId());
        assertEquals(6, beastHorde.weight());
        assertTrue(beastHorde.effects().contains("trade_risk_up"));
        assertTrue(beastHorde.effects().contains("rare_loot_hint"));

        WorldpackDataService.DailyEvent voidRift = snapshot.findDailyEvent("void_rift_surge")
                .orElseThrow(() -> new AssertionError("Missing void_rift_surge"));
        assertEquals("spirit_realm_border", voidRift.regionId());
        assertEquals(4, voidRift.weight());
        assertEquals(18000, voidRift.durationTicks());
        assertTrue(voidRift.effects().contains("trade_risk_up"));
        assertTrue(voidRift.effects().contains("secret_realm_ticket_hint"));
    }

    @Test
    void builtInWorldpackIncludesSpiritEventHooks() {
        WorldpackDataService.Snapshot snapshot = WorldpackDataService.builtin();

        WorldpackDataService.DailyEvent dajinDemonQi = snapshot.findDailyEvent("dajin_demon_qi_surge")
                .orElseThrow(() -> new AssertionError("Missing dajin_demon_qi_surge"));
        assertEquals("dajin", dajinDemonQi.regionId());
        assertEquals(7, dajinDemonQi.weight());
        assertEquals(18000, dajinDemonQi.durationTicks());
        assertEquals(List.of("trade_risk_up", "secret_realm_ticket_hint"), dajinDemonQi.effects());

        WorldpackDataService.DailyEvent ghostRumor = snapshot.findDailyEvent("ghost_expose_rumor")
                .orElseThrow(() -> new AssertionError("Missing ghost_expose_rumor"));
        assertEquals("tiannan", ghostRumor.regionId());
        assertEquals(2, ghostRumor.weight());
        assertTrue(ghostRumor.effects().contains("trade_risk_up"));
        assertTrue(ghostRumor.effects().contains("secret_realm_ticket_hint"));

        WorldpackDataService.DailyEvent spiritStorm = snapshot.findDailyEvent("spirit_storm_major")
                .orElseThrow(() -> new AssertionError("Missing spirit_storm_major"));
        assertEquals("spirit_realm_border", spiritStorm.regionId());
        assertEquals(4, spiritStorm.weight());
        assertTrue(spiritStorm.effects().contains("trade_risk_up"));
        assertTrue(spiritStorm.effects().contains("aura_plus_5"));

        WorldpackDataService.DailyEvent ruinWhisper = snapshot.findDailyEvent("ancient_ruin_whisper")
                .orElseThrow(() -> new AssertionError("Missing ancient_ruin_whisper"));
        assertEquals("spirit_fengyuan", ruinWhisper.regionId());
        assertEquals(2, ruinWhisper.weight());
        assertEquals(12000, ruinWhisper.durationTicks());
        assertTrue(ruinWhisper.effects().contains("secret_realm_ticket_hint"));
        assertTrue(ruinWhisper.effects().contains("rare_loot_hint"));
    }

    @Test
    void builtInWorldpackIncludesHighRealmDailyEventHooks() {
        WorldpackDataService.Snapshot snapshot = WorldpackDataService.builtin();

        WorldpackDataService.DailyEvent pearlSnatch = snapshot.findDailyEvent("cultivator_pearl_snatch")
                .orElseThrow(() -> new AssertionError("Missing cultivator_pearl_snatch"));
        assertEquals("chaotic_sea", pearlSnatch.regionId());
        assertEquals(3, pearlSnatch.weight());
        assertEquals(List.of("trade_risk_up", "rare_loot_hint"), pearlSnatch.effects());

        WorldpackDataService.DailyEvent spiritEcho = snapshot.findDailyEvent("spirit_realm_call_echo")
                .orElseThrow(() -> new AssertionError("Missing spirit_realm_call_echo"));
        assertEquals("chaotic_sea", spiritEcho.regionId());
        assertTrue(spiritEcho.effects().contains("aura_plus_5"));
        assertTrue(spiritEcho.effects().contains("secret_realm_ticket_hint"));

        WorldpackDataService.DailyEvent dajinFeud = snapshot.findDailyEvent("dajin_clan_feud")
                .orElseThrow(() -> new AssertionError("Missing dajin_clan_feud"));
        assertEquals("dajin", dajinFeud.regionId());
        assertEquals(4, dajinFeud.weight());
        assertTrue(dajinFeud.effects().contains("trade_risk_up"));

        WorldpackDataService.DailyEvent ascensionPressure = snapshot.findDailyEvent("ascension_pressure_hint")
                .orElseThrow(() -> new AssertionError("Missing ascension_pressure_hint"));
        assertEquals("dajin", ascensionPressure.regionId());
        assertEquals(2, ascensionPressure.weight());
        assertTrue(ascensionPressure.effects().contains("aura_plus_5"));

        WorldpackDataService.DailyEvent fallenMiasma = snapshot.findDailyEvent("fallen_demon_miasma")
                .orElseThrow(() -> new AssertionError("Missing fallen_demon_miasma"));
        assertEquals("fallen_demon_valley", fallenMiasma.regionId());
        assertEquals(List.of("trade_risk_up", "secret_realm_ticket_hint"), fallenMiasma.effects());

        WorldpackDataService.DailyEvent clanAuction = snapshot.findDailyEvent("clan_auction_spirit_realm")
                .orElseThrow(() -> new AssertionError("Missing clan_auction_spirit_realm"));
        assertEquals("spirit_fengyuan", clanAuction.regionId());
        assertTrue(clanAuction.effects().contains("secret_realm_ticket_hint"));
        assertTrue(clanAuction.effects().contains("rare_loot_hint"));

        WorldpackDataService.DailyEvent vehicleInsight = snapshot.findDailyEvent("great_vehicle_insight")
                .orElseThrow(() -> new AssertionError("Missing great_vehicle_insight"));
        assertEquals("spirit_fengyuan", vehicleInsight.regionId());
        assertTrue(vehicleInsight.effects().contains("aura_plus_5"));

        WorldpackDataService.DailyEvent capInsight = snapshot.findDailyEvent("mortal_realm_cap_insight")
                .orElseThrow(() -> new AssertionError("Missing mortal_realm_cap_insight"));
        assertEquals("tiannan", capInsight.regionId());
        assertEquals(3, capInsight.weight());
        assertTrue(capInsight.effects().contains("secret_realm_ticket_hint"));

        WorldpackDataService.DailyEvent siege = snapshot.findDailyEvent("demon_beast_siege")
                .orElseThrow(() -> new AssertionError("Missing demon_beast_siege"));
        assertEquals("tianyuan", siege.regionId());
        assertEquals(5, siege.weight());
        assertTrue(siege.effects().contains("trade_risk_up"));

        WorldpackDataService.DailyEvent embassy = snapshot.findDailyEvent("demon_clan_embassy")
                .orElseThrow(() -> new AssertionError("Missing demon_clan_embassy"));
        assertEquals("tianyuan", embassy.regionId());
        assertTrue(embassy.effects().contains("rare_loot_hint"));

        WorldpackDataService.DailyEvent convoy = snapshot.findDailyEvent("merit_convoy_ambush")
                .orElseThrow(() -> new AssertionError("Missing merit_convoy_ambush"));
        assertEquals("tianyuan", convoy.regionId());
        assertEquals(List.of("trade_risk_up", "rare_loot_hint"), convoy.effects());

        WorldpackDataService.DailyEvent tribulationCloud = snapshot.findDailyEvent("tribulation_cloud_gather")
                .orElseThrow(() -> new AssertionError("Missing tribulation_cloud_gather"));
        assertEquals("tianyuan", tribulationCloud.regionId());
        assertEquals(2, tribulationCloud.weight());
        assertTrue(tribulationCloud.effects().contains("aura_plus_5"));

        WorldpackDataService.DailyEvent yinLuo = snapshot.findDailyEvent("yin_luo_patrol")
                .orElseThrow(() -> new AssertionError("Missing yin_luo_patrol"));
        assertEquals("yinming", yinLuo.regionId());
        assertTrue(yinLuo.effects().contains("trade_risk_up"));

        WorldpackDataService.DailyEvent yinCorruption = snapshot.findDailyEvent("yin_corruption_warning")
                .orElseThrow(() -> new AssertionError("Missing yin_corruption_warning"));
        assertEquals("yinming", yinCorruption.regionId());
        assertEquals(15, yinCorruption.weight());
        assertEquals(18000, yinCorruption.durationTicks());
        assertEquals(List.of("trade_risk_up"), yinCorruption.effects());

        WorldpackDataService.DailyEvent yinWind = snapshot.findDailyEvent("yin_wind_howl")
                .orElseThrow(() -> new AssertionError("Missing yin_wind_howl"));
        assertEquals("yinming", yinWind.regionId());
        assertEquals(List.of("trade_risk_up", "secret_realm_ticket_hint"), yinWind.effects());
    }

    @Test
    void builtInWorldpackIncludesGreatJinCentralRegion() {
        WorldpackDataService.Snapshot snapshot = WorldpackDataService.builtin();

        WorldpackDataService.RegionCard greatJin = snapshot.findRegion("great_jin_central")
                .orElseThrow(() -> new AssertionError("Missing great_jin_central"));
        assertEquals("core_formation", greatJin.minRealm());
        assertEquals("great_jin_central_anchor", greatJin.travelAnchor());
        assertEquals(1.5D, greatJin.auraMultiplier());
        assertTrue(greatJin.tags().contains("great_jin_clans"));
        assertTrue(greatJin.tags().contains("wanbao_pavilion"));
        assertTrue(greatJin.tags().contains("refinement_halls"));
        assertTrue(greatJin.tags().contains("cross_region_array"));
        assertTrue(greatJin.tags().contains("ancient_artifact"));
        assertTrue(WorldpackGameplayService.meetsMinRealm(Realm.CORE_FORMATION, greatJin.minRealm()));
        assertFalse(WorldpackGameplayService.meetsMinRealm(Realm.FOUNDATION_ESTABLISHMENT, greatJin.minRealm()));

        WorldpackDataService.DailyEvent auctionWeek = snapshot.findDailyEvent("great_jin_auction_week")
                .orElseThrow(() -> new AssertionError("Missing great_jin_auction_week"));
        assertEquals("great_jin_central", auctionWeek.regionId());
        assertEquals(4, auctionWeek.weight());
        assertEquals(24000, auctionWeek.durationTicks());
        assertTrue(auctionWeek.effects().contains("secret_realm_ticket_hint"));
        assertTrue(auctionWeek.effects().contains("rare_loot_hint"));
    }

    @Test
    void builtInWorldpackIncludesDajinAuctionNoticeExactId() {
        WorldpackDataService.Snapshot snapshot = WorldpackDataService.builtin();

        WorldpackDataService.DailyEvent auctionNotice = snapshot.findDailyEvent("auction_notice")
                .orElseThrow(() -> new AssertionError("Missing auction_notice"));
        assertEquals("dajin", auctionNotice.regionId());
        assertEquals(5, auctionNotice.weight());
        assertEquals(24000, auctionNotice.durationTicks());
        assertEquals(List.of("secret_realm_ticket_hint", "rare_loot_hint"), auctionNotice.effects());
    }

    @Test
    void builtInWorldpackIncludesExtremeWestRegion() {
        WorldpackDataService.Snapshot snapshot = WorldpackDataService.builtin();

        WorldpackDataService.RegionCard extremeWest = snapshot.findRegion("extreme_west")
                .orElseThrow(() -> new AssertionError("Missing extreme_west"));
        assertEquals("foundation_establishment", extremeWest.minRealm());
        assertEquals("extreme_west_anchor", extremeWest.travelAnchor());
        assertEquals(1.0D, extremeWest.auraMultiplier());
        assertTrue(extremeWest.tags().contains("thousand_bamboo"));
        assertTrue(extremeWest.tags().contains("puppet"));
        assertTrue(extremeWest.tags().contains("ironwood"));
        assertTrue(extremeWest.tags().contains("puppet_core_blank"));

        WorldpackDataService.SecretRealm puppetTower = snapshot.findSecretRealm("thousand_bamboo_puppet_tower")
                .orElseThrow(() -> new AssertionError("Missing thousand_bamboo_puppet_tower"));
        assertEquals("extreme_west_thousand_bamboo", puppetTower.regionId());
    }

    @Test
    void builtInWorldpackIncludesKunwuRegionColdSnapAndRealm() {
        WorldpackDataService.Snapshot snapshot = WorldpackDataService.builtin();

        WorldpackDataService.RegionCard kunwu = snapshot.findRegion("kunwu")
                .orElseThrow(() -> new AssertionError("Missing kunwu"));
        assertEquals("core_formation", kunwu.minRealm());
        assertEquals("kunwu_mountain_anchor", kunwu.travelAnchor());
        assertEquals(1.2D, kunwu.auraMultiplier());
        assertTrue(kunwu.tags().contains("dajin"));
        assertTrue(kunwu.tags().contains("extreme_cold"));
        assertTrue(kunwu.tags().contains("ancient_seal"));
        assertTrue(WorldpackGameplayService.meetsMinRealm(Realm.CORE_FORMATION, kunwu.minRealm()));
        assertFalse(WorldpackGameplayService.meetsMinRealm(Realm.FOUNDATION_ESTABLISHMENT, kunwu.minRealm()));

        WorldpackDataService.SecretRealm kunwuMountain = snapshot.findSecretRealm("kunwu_mountain")
                .orElseThrow(() -> new AssertionError("Missing kunwu_mountain"));
        assertEquals("kunwu", kunwuMountain.regionId());
        assertEquals("core_formation", kunwuMountain.minRealm());
        assertTrue(kunwuMountain.tags().contains("puppet"));
        assertTrue(kunwuMountain.tags().contains("demon"));

        WorldpackDataService.DailyEvent coldSnap = snapshot.findDailyEvent("kunwu_cold_snap")
                .orElseThrow(() -> new AssertionError("Missing kunwu_cold_snap"));
        assertEquals("kunwu", coldSnap.regionId());
        assertEquals(3, coldSnap.weight());
        assertEquals(18000, coldSnap.durationTicks());
        assertTrue(coldSnap.effects().contains("trade_risk_up"));
        assertTrue(coldSnap.effects().contains("secret_realm_ticket_hint"));

        WorldpackDataService.DailyEvent dajinRuins = snapshot.findDailyEvent("ancient_ruins_whisper")
                .orElseThrow(() -> new AssertionError("Missing ancient_ruins_whisper"));
        assertEquals("dajin", dajinRuins.regionId());
        assertEquals(4, dajinRuins.weight());
        assertEquals(List.of("secret_realm_ticket_hint"), dajinRuins.effects());

        WorldpackDataService.DailyEvent kunwuRuins = snapshot.findDailyEvent("kunwu_ancient_ruins_whisper")
                .orElseThrow(() -> new AssertionError("Missing kunwu_ancient_ruins_whisper"));
        assertEquals("kunwu", kunwuRuins.regionId());
        assertEquals(4, kunwuRuins.weight());
        assertEquals(12000, kunwuRuins.durationTicks());
        assertEquals(List.of("secret_realm_ticket_hint", "rare_loot_hint"), kunwuRuins.effects());
    }

    @Test
    void builtInWorldpackIncludesBeastMigrationEvents() {
        WorldpackDataService.Snapshot snapshot = WorldpackDataService.builtin();

        WorldpackDataService.DailyEvent mulanMigration = snapshot.findDailyEvent("mulan_beast_migration")
                .orElseThrow(() -> new AssertionError("Missing mulan_beast_migration"));
        assertEquals("mulan_grassland", mulanMigration.regionId());
        assertTrue(mulanMigration.effects().contains("trade_risk_up"));
        assertTrue(mulanMigration.effects().contains("rare_loot_hint"));

        WorldpackDataService.DailyEvent fengyuanMigration = snapshot.findDailyEvent("fengyuan_beast_migration")
                .orElseThrow(() -> new AssertionError("Missing fengyuan_beast_migration"));
        assertEquals("spirit_fengyuan", fengyuanMigration.regionId());
        assertTrue(fengyuanMigration.effects().contains("trade_risk_up"));
        assertTrue(fengyuanMigration.effects().contains("rare_loot_hint"));
    }

    @Test
    void builtInWorldpackIncludesDiyuanPressureEvent() {
        WorldpackDataService.Snapshot snapshot = WorldpackDataService.builtin();

        WorldpackDataService.SecretRealm diyuan = snapshot.findSecretRealm("diyuan")
                .orElseThrow(() -> new AssertionError("Missing diyuan"));
        assertEquals("spirit_fengyuan", diyuan.regionId());
        assertEquals("void_refinement", diyuan.minRealm());
        assertTrue(diyuan.tags().contains("no_fly"));
        assertTrue(diyuan.tags().contains("spirit_chaos"));

        PlayerCultivation cultivation = new PlayerCultivation();
        assertFalse(WorldpackGameplayService.isFlightSuppressed(cultivation));
        cultivation.setWorldpackActiveSecretRealmId("diyuan");
        assertTrue(WorldpackGameplayService.isFlightSuppressed(cultivation));
        cultivation.setWorldpackActiveSecretRealmId("mist_cave_trial");
        assertFalse(WorldpackGameplayService.isFlightSuppressed(cultivation));

        WorldpackDataService.DailyEvent pressureWave = snapshot.findDailyEvent("diyuan_pressure_wave")
                .orElseThrow(() -> new AssertionError("Missing diyuan_pressure_wave"));
        assertEquals("spirit_fengyuan", pressureWave.regionId());
        assertEquals(5, pressureWave.weight());
        assertEquals(18000, pressureWave.durationTicks());
        assertEquals(List.of("trade_risk_up", "secret_realm_ticket_hint"), pressureWave.effects());
    }

    @Test
    void builtInWorldpackIncludesBarbarianWastelandEvents() {
        WorldpackDataService.Snapshot snapshot = WorldpackDataService.builtin();

        WorldpackDataService.RegionCard barbarianWasteland = snapshot.findRegion("barbarian_wasteland")
                .orElseThrow(() -> new AssertionError("Missing barbarian_wasteland"));
        assertEquals("void_refinement", barbarianWasteland.minRealm());
        assertEquals("barbarian_wasteland_anchor", barbarianWasteland.travelAnchor());
        assertTrue(barbarianWasteland.tags().contains("demon_kings"));

        WorldpackDataService.DailyEvent beastTide = snapshot.findDailyEvent("barbarian_beast_tide")
                .orElseThrow(() -> new AssertionError("Missing barbarian_beast_tide"));
        assertEquals("barbarian_wasteland", beastTide.regionId());
        assertTrue(beastTide.effects().contains("trade_risk_up"));
        assertTrue(beastTide.effects().contains("rare_loot_hint"));

        WorldpackDataService.DailyEvent kingRoar = snapshot.findDailyEvent("barbarian_king_roar")
                .orElseThrow(() -> new AssertionError("Missing barbarian_king_roar"));
        assertEquals("barbarian_wasteland", kingRoar.regionId());
        assertTrue(kingRoar.effects().contains("secret_realm_ticket_hint"));
    }

    @Test
    void builtInWorldpackIncludesBarbarianKingTerritories() {
        WorldpackDataService.Snapshot snapshot = WorldpackDataService.builtin();
        List<String> kingTerritories = List.of(
                "king_bear_mountain",
                "king_fox_mist",
                "king_pine_ancient",
                "king_roc_sky",
                "king_snake_swamp",
                "king_tiger_jungle",
                "king_turtle_depth");

        for (String territoryId : kingTerritories) {
            WorldpackDataService.SecretRealm realm = snapshot.findSecretRealm(territoryId)
                    .orElseThrow(() -> new AssertionError("Missing " + territoryId));
            assertEquals("barbarian_wasteland", realm.regionId(), territoryId);
            assertEquals("void_refinement", realm.minRealm(), territoryId);
            assertEquals("seeking_immortals:immortal_jade", realm.ticketItem(), territoryId);
            assertEquals(600000, realm.cooldownTicks(), territoryId);
            assertTrue(realm.tags().contains("barbarian_king"), territoryId);
            assertTrue(realm.tags().contains("territory"), territoryId);
            assertTrue(WorldpackGameplayService.meetsMinRealm(Realm.VOID_REFINEMENT, realm.minRealm()), territoryId);
            assertFalse(WorldpackGameplayService.meetsMinRealm(Realm.SOUL_TRANSFORMATION, realm.minRealm()), territoryId);
        }

        assertTrue(snapshot.findSecretRealm("king_bear_mountain").orElseThrow().tags().contains("strength_earth"));
        assertTrue(snapshot.findSecretRealm("king_fox_mist").orElseThrow().tags().contains("illusion_wind"));
        assertTrue(snapshot.findSecretRealm("king_pine_ancient").orElseThrow().tags().contains("wood_array"));
        assertTrue(snapshot.findSecretRealm("king_roc_sky").orElseThrow().tags().contains("wind_flight"));
        assertTrue(snapshot.findSecretRealm("king_snake_swamp").orElseThrow().tags().contains("poison_water"));
        assertTrue(snapshot.findSecretRealm("king_tiger_jungle").orElseThrow().tags().contains("melee_hunt"));
        assertTrue(snapshot.findSecretRealm("king_turtle_depth").orElseThrow().tags().contains("defense_array"));
    }

    @Test
    void builtInWorldpackIncludesTianyuanAuctionNotice() {
        WorldpackDataService.Snapshot snapshot = WorldpackDataService.builtin();

        WorldpackDataService.DailyEvent auctionNotice = snapshot.findDailyEvent("tianyuan_auction_notice")
                .orElseThrow(() -> new AssertionError("Missing tianyuan_auction_notice"));
        assertEquals("tianyuan", auctionNotice.regionId());
        assertTrue(auctionNotice.effects().contains("secret_realm_ticket_hint"));
        assertTrue(auctionNotice.effects().contains("rare_loot_hint"));

        WorldpackDataService.DailyEvent treasureFair = snapshot.findDailyEvent("tianyuan_treasure_fair_rumor")
                .orElseThrow(() -> new AssertionError("Missing tianyuan_treasure_fair_rumor"));
        assertEquals("tianyuan", treasureFair.regionId());
        assertEquals(4, treasureFair.weight());
        assertTrue(treasureFair.effects().contains("secret_realm_ticket_hint"));
        assertTrue(treasureFair.effects().contains("rare_loot_hint"));

        WorldpackDataService.DailyEvent voidRift = snapshot.findDailyEvent("tianyuan_void_rift_sighting")
                .orElseThrow(() -> new AssertionError("Missing tianyuan_void_rift_sighting"));
        assertEquals("tianyuan", voidRift.regionId());
        assertEquals(3, voidRift.weight());
        assertEquals(12000, voidRift.durationTicks());
        assertEquals(List.of("trade_risk_up", "secret_realm_ticket_hint"), voidRift.effects());
    }

    @Test
    void builtInWorldpackIncludesExactPirateRaidSourceId() {
        WorldpackDataService.Snapshot snapshot = WorldpackDataService.builtin();

        WorldpackDataService.DailyEvent pirateRaid = snapshot.findDailyEvent("pirate_raid")
                .orElseThrow(() -> new AssertionError("Missing pirate_raid"));
        assertEquals("chaotic_sea", pirateRaid.regionId());
        assertEquals(11, pirateRaid.weight());
        assertEquals(18000, pirateRaid.durationTicks());
        assertEquals(List.of("trade_risk_up"), pirateRaid.effects());
    }

    @Test
    void builtInWorldpackIncludesFengyuanTreasureFairExactId() {
        WorldpackDataService.Snapshot snapshot = WorldpackDataService.builtin();

        WorldpackDataService.DailyEvent treasureFair = snapshot.findDailyEvent("treasure_fair_rumor")
                .orElseThrow(() -> new AssertionError("Missing treasure_fair_rumor"));
        assertEquals("spirit_fengyuan", treasureFair.regionId());
        assertEquals(4, treasureFair.weight());
        assertEquals(24000, treasureFair.durationTicks());
        assertEquals(List.of("secret_realm_ticket_hint", "rare_loot_hint"), treasureFair.effects());

        WorldpackDataService.DailyEvent legacyFengyuan = snapshot.findDailyEvent("fengyuan_treasure_fair")
                .orElseThrow(() -> new AssertionError("Missing fengyuan_treasure_fair"));
        assertEquals("spirit_fengyuan", legacyFengyuan.regionId());
        assertEquals(List.of("secret_realm_ticket_hint"), legacyFengyuan.effects());
    }

    @Test
    void builtInWorldpackIncludesContributionBonusDay() {
        WorldpackDataService.Snapshot snapshot = WorldpackDataService.builtin();

        WorldpackDataService.DailyEvent contributionDay = snapshot.findDailyEvent("contribution_bonus_day")
                .orElseThrow(() -> new AssertionError("Missing contribution_bonus_day"));
        assertEquals("tiannan", contributionDay.regionId());
        assertEquals(3, contributionDay.weight());
        assertEquals(24000, contributionDay.durationTicks());
        assertEquals(List.of(WorldpackGameplayService.EFFECT_SECT_CONTRIBUTION_BONUS), contributionDay.effects());
    }

    @Test
    void builtInWorldpackIncludesFallenDemonValleyRealm() {
        WorldpackDataService.Snapshot snapshot = WorldpackDataService.builtin();

        WorldpackDataService.RegionCard fallenDemonRegion = snapshot.findRegion("fallen_demon_valley")
                .orElseThrow(() -> new AssertionError("Missing fallen_demon_valley region"));
        assertEquals("fallen_demon_anchor", fallenDemonRegion.travelAnchor());
        assertEquals("nascent_soul", fallenDemonRegion.minRealm());
        assertTrue(fallenDemonRegion.tags().contains("ancient_demon"));
        assertTrue(fallenDemonRegion.tags().contains("demon_rift"));

        WorldpackDataService.SecretRealm fallenDemonValley = snapshot.findSecretRealm("fallen_demon_valley")
                .orElseThrow(() -> new AssertionError("Missing fallen_demon_valley"));
        assertEquals("fallen_demon_valley", fallenDemonValley.regionId());
        assertEquals("nascent_soul", fallenDemonValley.minRealm());
        assertTrue(fallenDemonValley.tags().contains("ancient_demon"));
        assertTrue(fallenDemonValley.tags().contains("spatial_rift"));

        WorldpackDataService.DailyEvent ancientDemonSeal = snapshot.findDailyEvent("ancient_demon_seal_breach")
                .orElseThrow(() -> new AssertionError("Missing ancient_demon_seal_breach"));
        assertEquals("fallen_demon_valley", ancientDemonSeal.regionId());
        assertEquals(List.of("trade_risk_up", "secret_realm_ticket_hint", "rare_loot_hint"), ancientDemonSeal.effects());
    }

    @Test
    void builtInWorldpackIncludesMultiregionDailyEventBackfill() {
        WorldpackDataService.Snapshot snapshot = WorldpackDataService.builtin();

        WorldpackDataService.DailyEvent tiannanTalisman = snapshot.findDailyEvent("tiannan_talisman_master_visit")
                .orElseThrow(() -> new AssertionError("Missing tiannan_talisman_master_visit"));
        assertEquals("tiannan", tiannanTalisman.regionId());
        assertEquals(5, tiannanTalisman.weight());
        assertEquals(List.of("herb_shop_bonus", "secret_realm_ticket_hint"), tiannanTalisman.effects());

        WorldpackDataService.DailyEvent tiannanDuel = snapshot.findDailyEvent("tiannan_rogue_cultivator_duel")
                .orElseThrow(() -> new AssertionError("Missing tiannan_rogue_cultivator_duel"));
        assertEquals("tiannan", tiannanDuel.regionId());
        assertEquals(List.of("trade_risk_up", "rare_loot_hint"), tiannanDuel.effects());

        WorldpackDataService.DailyEvent dajinDuel = snapshot.findDailyEvent("dajin_rogue_cultivator_duel")
                .orElseThrow(() -> new AssertionError("Missing dajin_rogue_cultivator_duel"));
        assertEquals("dajin", dajinDuel.regionId());
        assertEquals(18000, dajinDuel.durationTicks());
        assertTrue(dajinDuel.effects().contains("rare_loot_hint"));

        WorldpackDataService.DailyEvent netherGhost = snapshot.findDailyEvent("nether_river_ghost_wail_night")
                .orElseThrow(() -> new AssertionError("Missing nether_river_ghost_wail_night"));
        assertEquals("nether_river", netherGhost.regionId());
        assertEquals(10, netherGhost.weight());
        assertEquals(List.of("secret_realm_ticket_hint", "rare_loot_hint"), netherGhost.effects());
    }

    @Test
    void builtInWorldpackIncludesRemainingDailyRandomExactIds() {
        WorldpackDataService.Snapshot snapshot = WorldpackDataService.builtin();

        WorldpackDataService.DailyEvent spiritRain = snapshot.findDailyEvent("spirit_rain")
                .orElseThrow(() -> new AssertionError("Missing spirit_rain"));
        assertEquals("qinglan_mountains", spiritRain.regionId());
        assertEquals(3, spiritRain.weight());
        assertEquals(24000, spiritRain.durationTicks());
        assertEquals(List.of(WorldpackGameplayService.EFFECT_SPIRIT_RAIN_BONUS), spiritRain.effects());

        WorldpackDataService.DailyEvent wanderingMerchant = snapshot.findDailyEvent("wandering_merchant")
                .orElseThrow(() -> new AssertionError("Missing wandering_merchant"));
        assertEquals("tiannan", wanderingMerchant.regionId());
        assertEquals(10, wanderingMerchant.weight());
        assertEquals(List.of("herb_shop_bonus", "secret_realm_ticket_hint"), wanderingMerchant.effects());

        WorldpackDataService.DailyEvent bandit = snapshot.findDailyEvent("bandit_cultivator")
                .orElseThrow(() -> new AssertionError("Missing bandit_cultivator"));
        assertEquals("tiannan", bandit.regionId());
        assertEquals(8, bandit.weight());
        assertEquals(List.of("trade_risk_up", "rare_loot_hint"), bandit.effects());

        WorldpackDataService.DailyEvent sectRecruit = snapshot.findDailyEvent("sect_recruit")
                .orElseThrow(() -> new AssertionError("Missing sect_recruit"));
        assertEquals("tiannan", sectRecruit.regionId());
        assertEquals(6, sectRecruit.weight());
        assertEquals(List.of("secret_realm_ticket_hint"), sectRecruit.effects());

        WorldpackDataService.DailyEvent demonQi = snapshot.findDailyEvent("demon_qi_surge")
                .orElseThrow(() -> new AssertionError("Missing demon_qi_surge"));
        assertEquals("fallen_demon_valley", demonQi.regionId());
        assertEquals(7, demonQi.weight());
        assertEquals(List.of("trade_risk_up"), demonQi.effects());

        WorldpackDataService.DailyEvent beastMigration = snapshot.findDailyEvent("beast_migration")
                .orElseThrow(() -> new AssertionError("Missing beast_migration"));
        assertEquals("mulan", beastMigration.regionId());
        assertEquals(4, beastMigration.weight());
        assertEquals(List.of("trade_risk_up", "rare_loot_hint"), beastMigration.effects());
    }

    @Test
    void builtInWorldpackIncludesRegionCardExactDailyEventIds() {
        WorldpackDataService.Snapshot snapshot = WorldpackDataService.builtin();

        assertDailyEvent(snapshot, "sect_recruitment_spring", "tiannan", 6, 24000,
                List.of("secret_realm_ticket_hint"));
        assertDailyEvent(snapshot, "bandit_cultivator_road", "tiannan", 8, 18000,
                List.of("trade_risk_up", "rare_loot_hint"));
        assertDailyEvent(snapshot, "spirit_herb_gatherers", "tiannan", 10, 24000,
                List.of("herb_shop_bonus"));
        assertDailyEvent(snapshot, "tianlan_patrol", "tianlan", 7, 18000,
                List.of("trade_risk_up", "rare_loot_hint"));
        assertDailyEvent(snapshot, "ghost_fog_truce", "chaotic_sea", 3, 24000,
                List.of("secret_realm_ticket_hint"));
        assertDailyEvent(snapshot, "spatial_rift_flutter", "spirit_realm_border", 4, 18000,
                List.of("trade_risk_up", "secret_realm_ticket_hint"));
        assertDailyEvent(snapshot, "king_territory_intrusion", "barbarian_wasteland", 4, 18000,
                List.of("trade_risk_up", "secret_realm_ticket_hint", "rare_loot_hint"));
        assertDailyEvent(snapshot, "fengyuan_clan_feud", "spirit_fengyuan", 4, 18000,
                List.of("trade_risk_up", "rare_loot_hint"));
        assertDailyEvent(snapshot, "spirit_storm", "spirit_fengyuan", 4, 24000,
                List.of("trade_risk_up", "aura_plus_5"));
        assertDailyEvent(snapshot, "merit_patrol", "tianyuan", 5, 24000,
                List.of("trade_risk_up", "rare_loot_hint"));
    }

    @Test
    void builtInWorldpackIncludesMulanTianlanWarPhaseHooks() {
        WorldpackDataService.Snapshot snapshot = WorldpackDataService.builtin();

        assertDailyEvent(snapshot, "border_skirmish", "mulan", 5, 18000,
                List.of("trade_risk_up", "rare_loot_hint"));
        assertDailyEvent(snapshot, "fashi_array_clash", "mulan", 4, 18000,
                List.of("trade_risk_up", "rare_loot_hint"));
        assertDailyEvent(snapshot, "holy_beast_intervention", "mulan", 3, 24000,
                List.of("aura_plus_5", "secret_realm_ticket_hint"));
        assertDailyEvent(snapshot, "ceasefire_or_escalation", "tianlan", 3, 24000,
                List.of("trade_risk_up", "rare_loot_hint"));
    }

    private static void assertDailyEvent(WorldpackDataService.Snapshot snapshot, String id, String regionId,
                                         int weight, int durationTicks, List<String> effects) {
        WorldpackDataService.DailyEvent event = snapshot.findDailyEvent(id)
                .orElseThrow(() -> new AssertionError("Missing " + id));
        assertEquals(regionId, event.regionId());
        assertEquals(weight, event.weight());
        assertEquals(durationTicks, event.durationTicks());
        assertEquals(effects, event.effects());
    }

    @Test
    void parsesWorldpackDataForTests() {
        String regions = """
                { "regions": [
                  {
                    "id": "test_region",
                    "display_zh": "测试地",
                    "display_en": "Test Region",
                    "aura_multiplier": 1.25,
                    "min_realm": "qi_refining",
                    "travel_anchor": "test_anchor",
                    "tags": ["starter"]
                  }
                ] }
                """;
        String realms = """
                { "secret_realms": [
                  {
                    "id": "test_realm",
                    "region_id": "test_region",
                    "display_zh": "测试秘境",
                    "display_en": "Test Realm",
                    "min_realm": "foundation_establishment",
                    "ticket_item": "minecraft:emerald",
                    "cooldown_ticks": 20,
                    "return_policy": "return_to_entry_anchor",
                    "tags": ["trial"]
                  }
                ] }
                """;
        String events = """
                { "daily_events": [
                  {
                    "id": "test_event",
                    "region_id": "test_region",
                    "display_zh": "测试事件",
                    "display_en": "Test Event",
                    "weight": 3,
                    "duration_ticks": 40,
                    "effects": ["aura_plus_5"]
                  }
                ] }
                """;

        WorldpackDataService.Snapshot snapshot = WorldpackDataService.parseForTest(
                new StringReader(regions),
                new StringReader(realms),
                new StringReader(events));

        assertEquals("test_region", snapshot.regions().get(0).id());
        assertEquals(1.25D, snapshot.regions().get(0).auraMultiplier());
        assertEquals("test_realm", snapshot.secretRealms().get(0).id());
        assertEquals(20, snapshot.secretRealms().get(0).cooldownTicks());
        assertEquals("test_event", snapshot.dailyEvents().get(0).id());
        assertEquals(3, snapshot.dailyEvents().get(0).weight());
    }

    @Test
    void worldpackGameplayRealmGatesAcceptAliases() {
        assertTrue(WorldpackGameplayService.meetsMinRealm(Realm.FOUNDATION_ESTABLISHMENT, "foundation"));
        assertTrue(WorldpackGameplayService.meetsMinRealm(Realm.CORE_FORMATION, "foundation_establishment"));
        assertFalse(WorldpackGameplayService.meetsMinRealm(Realm.QI_REFINING, "core_formation"));
    }

    @Test
    void contributionBonusDayIncreasesSectContributionRewards() {
        assertEquals(20, WorldpackGameplayService.adjustSectContributionReward(20, List.of()));
        assertEquals(30, WorldpackGameplayService.adjustSectContributionReward(
                20, List.of(WorldpackGameplayService.EFFECT_SECT_CONTRIBUTION_BONUS)));
        assertEquals(38, WorldpackGameplayService.adjustSectContributionReward(
                25, List.of(WorldpackGameplayService.EFFECT_SECT_CONTRIBUTION_BONUS)));
    }

    @Test
    void spiritRainBonusIncreasesAuraGainByTenPercent() {
        assertEquals(100, WorldpackGameplayService.adjustAuraGainForEffects(100, List.of()));
        assertEquals(105, WorldpackGameplayService.adjustAuraGainForEffects(
                100, List.of(WorldpackGameplayService.EFFECT_AURA_PLUS_5)));
        assertEquals(110, WorldpackGameplayService.adjustAuraGainForEffects(
                100, List.of(WorldpackGameplayService.EFFECT_SPIRIT_RAIN_BONUS)));
        assertEquals(115, WorldpackGameplayService.adjustAuraGainForEffects(
                100, List.of(WorldpackGameplayService.EFFECT_AURA_PLUS_5,
                        WorldpackGameplayService.EFFECT_SPIRIT_RAIN_BONUS)));
    }

    @Test
    void savedDataTracksAnchorsAndDailyEventRolls() {
        String regions = """
                { "regions": [
                  { "id": "test_region", "display_zh": "测试地", "display_en": "Test", "travel_anchor": "test_anchor" }
                ] }
                """;
        String realms = "{ \"secret_realms\": [] }";
        String events = """
                { "daily_events": [
                  {
                    "id": "test_event",
                    "region_id": "test_region",
                    "display_zh": "测试事件",
                    "display_en": "Test Event",
                    "weight": 1,
                    "duration_ticks": 40,
                    "effects": ["aura_plus_5", "herb_shop_bonus"]
                  }
                ] }
                """;
        WorldpackDataService.Snapshot snapshot = WorldpackDataService.parseForTest(
                new StringReader(regions),
                new StringReader(realms),
                new StringReader(events));
        WorldpackSavedData data = new WorldpackSavedData();

        data.setAnchor("test_anchor", "minecraft:overworld", 1.0D, 65.0D, 2.0D, 90.0F, 10.0F);
        WorldpackSavedData.EventRoll roll = data.getOrRollDailyEvent("test_region", snapshot, 0L, RandomSource.create(1L));

        assertTrue(data.hasAnchor("test_anchor"));
        assertEquals("test_event", roll.eventId());
        assertEquals(40L, roll.untilTick());
        assertEquals(2, snapshot.findDailyEvent(roll.eventId()).orElseThrow().effects().size());
    }

    @Test
    void playerWorldpackStatePersistsAsAdditiveNbt() {
        PlayerCultivation cultivation = new PlayerCultivation();
        cultivation.setWorldpackCurrentRegionId("yueling_mist_valley");
        cultivation.setWorldpackActiveSecretRealmId("mist_cave_trial");
        cultivation.setWorldpackReturnLocation("minecraft:overworld", 10.0D, 70.0D, -3.0D, 180.0F, 15.0F);
        cultivation.setWorldpackCooldownUntil("mist_cave_trial", 12345L);
        cultivation.setWorldpackDailyEvent("mist_valley_seal_thins", 24000L);

        CompoundTag tag = cultivation.saveNBTData();
        PlayerCultivation loaded = new PlayerCultivation();
        loaded.loadNBTData(tag);

        assertEquals("yueling_mist_valley", loaded.getWorldpackCurrentRegionId());
        assertEquals("mist_cave_trial", loaded.getWorldpackActiveSecretRealmId());
        assertTrue(loaded.hasWorldpackReturnLocation());
        assertEquals("minecraft:overworld", loaded.getWorldpackReturnDimension());
        assertEquals(12345L, loaded.getWorldpackCooldownUntil("mist_cave_trial"));
        assertEquals("mist_valley_seal_thins", loaded.getWorldpackActiveDailyEventId());
        assertEquals(24000L, loaded.getWorldpackActiveDailyEventUntilTick());
    }
}
