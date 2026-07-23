package com.xunxian.seekingimmortals.worldpack;

import com.xunxian.seekingimmortals.cultivation.Realm;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DailyEventSemanticsTest {
    @Test
    void parserKeepsAllAuthoredEffectShapesAndSingularRegion() {
        DailyEventEffectCatalog.Snapshot snapshot = DailyEventEffectCatalog.parseForTest(new StringReader("""
                {"schema_version":5,"events":[
                  {"id":"single","region":"great_jin","weight":0.4,"effect":"herb_shop_price_x1.3"},
                  {"id":"debuff","regions":["yinming"],"debuff":"demon_qi_tick"},
                  {"id":"array","regions":["tiannan"],"effects":["ferry_delay","unknown_token"]},
                  {"id":"object","region":"chaotic_sea","effects":{"tax_mult":1.2,"pvp_disabled_factions":["star_palace"],"duration_days":3}}
                ]}
                """));

        DailyEventEffectCatalog.Event single = snapshot.find("single").orElseThrow();
        assertEquals(List.of("great_jin"), single.regions());
        assertEquals(0.4D, single.weight(), 0.00001D);
        assertTrue(single.hasToken("herb_shop_price_x1.3"));

        DailyEventEffectCatalog.Event debuff = snapshot.find("debuff").orElseThrow();
        assertEquals("demon_qi_tick", debuff.debuff());

        DailyEventEffectCatalog.Event array = snapshot.find("array").orElseThrow();
        assertTrue(array.hasToken("ferry_delay"));
        assertTrue(array.unknownTokens().contains("unknown_token"));

        DailyEventEffectCatalog.Event object = snapshot.find("object").orElseThrow();
        assertEquals("1.2", object.objectEffects().get("tax_mult"));
        assertEquals("3", object.objectEffects().get("duration_days"));
        assertEquals("[\"star_palace\"]", object.objectEffects().get("pvp_disabled_factions"));
        assertTrue(object.hasToken("tax_mult"));
    }

    @Test
    void unknownAuthoredSignalsFailClosedWithoutGenericFallback() {
        DailyEventEffectCatalog.Snapshot snapshot = DailyEventEffectCatalog.parseForTest(new StringReader("""
                {"events":[{"id":"unknown","region":"x","weight":2,"combat_tier":3,"rewards":["ore"]}]}
                """));
        DailyEventEffectCatalog.Event event = snapshot.find("unknown").orElseThrow();

        assertTrue(event.legacyEffects().isEmpty());
        assertTrue(event.tokens().isEmpty());
        assertFalse(event.hasToken("aura_plus_5"));
        assertFalse(event.hasToken("trade_risk_up"));
    }

    @Test
    void builtInCatalogContainsTheAuthoritativeSpecialShapes() {
        DailyEventEffectCatalog.Snapshot snapshot = DailyEventEffectCatalog.builtin();

        assertTrue(snapshot.authoredEventCount() >= 71);
        assertTrue(snapshot.find("tiannan_herb_price_spike").orElseThrow().hasToken("herb_shop_price_x1.3"));
        assertTrue(snapshot.find("demon_qi_surge").orElseThrow().hasToken("demon_qi_tick"));
        assertTrue(snapshot.find("chaotic_sea_beast_tide").orElseThrow().hasToken("spawn_beast_wave"));
        assertTrue(snapshot.find("chaotic_sea_tax_raid").orElseThrow().hasToken("tax_mult"));
        assertTrue(snapshot.find("tianyuan_merit_double_day").orElseThrow().hasToken("merit_mult_2"));
        assertTrue(snapshot.find("tianyuan_merit_double_day").orElseThrow()
                .unknownTokens().contains("merit_mult_2"));
        assertTrue(snapshot.find("chaotic_sea_tax_raid").orElseThrow()
                .unknownTokens().contains("inverse_star_smuggle_chance"));
        assertTrue(snapshot.find("chaotic_sea_beast_tide").orElseThrow()
                .unknownTokens().contains("star_palace_patrol_bonus"));
        assertEquals(DailyEventEffectCatalog.Coverage.PRESERVED,
                snapshot.find("mulan_border_patrol").orElseThrow().authoredFields().stream()
                        .filter(field -> "war_phase".equals(field.field()))
                        .findFirst().orElseThrow().coverage());
    }

    @Test
    void schedulerProjectsSingleRegionAuthoredEventsWithoutInventingEffects() {
        WorldpackDataService.Snapshot worldpack = WorldpackDataService.builtin();
        List<WorldpackDataService.DailyEvent> greatJin =
                com.xunxian.seekingimmortals.region.DailyEventScheduler.expandedCandidates("great_jin", worldpack);

        WorldpackDataService.DailyEvent projected = greatJin.stream()
                .filter(event -> "great_jin_auction_week".equals(event.id()))
                .findFirst()
                .orElseThrow();
        assertTrue(projected.effects().contains("auction_active"));
        WorldpackDataService.DailyEvent ambient = greatJin.stream()
                .filter(event -> "open_sky_tribulation_omen".equals(event.id()))
                .findFirst()
                .orElseThrow();
        assertTrue(ambient.effects().isEmpty());

        List<WorldpackDataService.DailyEvent> runtimeGreatJin =
                com.xunxian.seekingimmortals.region.DailyEventScheduler.expandedCandidates(
                        "great_jin_central", worldpack);
        assertEquals(DailyEventEffectCatalog.builtin().find("great_jin_auction_week").orElseThrow()
                .scaledWeight(1000), runtimeGreatJin.stream()
                .filter(event -> "great_jin_auction_week".equals(event.id()))
                .findFirst().orElseThrow().weight());
        assertTrue(runtimeGreatJin.stream().anyMatch(event -> "great_jin_auction_week".equals(event.id())));
        assertTrue(com.xunxian.seekingimmortals.region.DailyEventScheduler
                .expandedCandidates("spirit_realm_border", worldpack).stream()
                .anyMatch(event -> "spirit_storm_major".equals(event.id())));
    }

    @Test
    void executorUsesTypedEconomicAndProgressionModifiers() {
        DailyEventEffectCatalog.Snapshot snapshot = DailyEventEffectCatalog.parseForTest(new StringReader("""
                {"events":[
                  {"id":"price","effect":"herb_shop_price_x1.3"},
                  {"id":"ferry","effect":"ferry_cost_double"},
                  {"id":"contribution","effect":"contribution_gain_1.5_1day"},
                  {"id":"merit","effect":"merit_mult_2"},
                  {"id":"delayed","effects":["ferry_delay"]},
                  {"id":"not_delayed","effects":{"ferry_delay":false}},
                  {"id":"unknown","effect":"not_executed"}
                ]}
                """));

        DailyEventEffectCatalog.Event price = snapshot.find("price").orElseThrow();
        assertEquals(1.30D, DailyEventEffectExecutor.marketPriceMultiplier(price, "herbal_stall", "spirit_herb"), 0.00001D);
        assertEquals(1.0D, DailyEventEffectExecutor.marketPriceMultiplier(price, "general_store", "iron_ingot"), 0.00001D);
        assertEquals(130, DailyEventEffectExecutor.adjustMarketCost(100, price, "herbal_stall", "spirit_herb"));

        DailyEventEffectCatalog.Event ferry = snapshot.find("ferry").orElseThrow();
        assertEquals(60, DailyEventEffectExecutor.adjustFerryCost(30, ferry));

        DailyEventEffectCatalog.Event contribution = snapshot.find("contribution").orElseThrow();
        DailyEventEffectCatalog.Event merit = snapshot.find("merit").orElseThrow();
        assertEquals(15, DailyEventEffectExecutor.adjustContributionReward(10, contribution));
        assertEquals(10, DailyEventEffectExecutor.adjustMeritReward(10, contribution));
        assertEquals(10, DailyEventEffectExecutor.adjustContributionReward(10, merit));
        assertEquals(20, DailyEventEffectExecutor.adjustMeritReward(10, merit));
        assertTrue(DailyEventEffectExecutor.definesContributionReward(contribution));
        assertFalse(DailyEventEffectExecutor.definesMeritReward(contribution));
        assertTrue(DailyEventEffectExecutor.isFerryDelayed(snapshot.find("delayed").orElseThrow()));
        assertFalse(DailyEventEffectExecutor.isFerryDelayed(snapshot.find("not_delayed").orElseThrow()));

        DailyEventEffectCatalog.Event unknown = snapshot.find("unknown").orElseThrow();
        assertEquals(1.0D, DailyEventEffectExecutor.marketPriceMultiplier(unknown, "herbal_stall", "spirit_herb"), 0.00001D);
        assertEquals(10, DailyEventEffectExecutor.adjustContributionReward(10, unknown));
    }

    @Test
    void nestedRealmGateRewardsCoverageAndFixedPointWeightRemainExact() {
        DailyEventEffectCatalog.Event event = DailyEventEffectCatalog.parseForTest(new StringReader("""
                {"events":[{"id":"nested","region":"diyuan","weight":0.4,
                  "rewards":["future_reward"],"rewards_tag":"future_tag",
                  "learn_requirements":{"trigger":{"realm_min":"VOID_REFINING"}}}]}
                """)).find("nested").orElseThrow();

        assertEquals("void_refining", event.realmMin());
        assertEquals(400, event.scaledWeight(1000));
        assertEquals(DailyEventEffectCatalog.Coverage.PRESERVED, event.authoredFields().stream()
                .filter(field -> "rewards".equals(field.field())).findFirst().orElseThrow().coverage());
        assertEquals(DailyEventEffectCatalog.Coverage.PRESERVED, event.authoredFields().stream()
                .filter(field -> "rewards_tag".equals(field.field())).findFirst().orElseThrow().coverage());
        assertFalse(DailyEventEffectExecutor.meetsRealmMinimum(Realm.SOUL_TRANSFORMATION, event.realmMin()));
        assertTrue(DailyEventEffectExecutor.meetsRealmMinimum(Realm.VOID_REFINEMENT, event.realmMin()));
        assertFalse(DailyEventEffectExecutor.meetsRealmMinimum(Realm.TRUE_IMMORTAL, "future_realm"));
    }

    @Test
    void ferryRouteScopeIsExplicitAndBidirectional() {
        assertTrue(FerryTravelPolicy.isRegionFerryRoute("tiannan", "chaotic_sea"));
        assertTrue(FerryTravelPolicy.isRegionFerryRoute("chaotic_sea", "outer_sea_market"));
        assertTrue(FerryTravelPolicy.isRegionFerryRoute("yinming", "nether_river"));
        assertTrue(FerryTravelPolicy.isRegionFerryRoute("chaotic_sea", "tiannan"));
        assertTrue(FerryTravelPolicy.isFerryVehicle("chaotic_sea_ferry"));
        assertTrue(FerryTravelPolicy.isFerryRoute("ghost_boat_route", "ship_sea", ""));
        assertTrue(FerryTravelPolicy.isFerryRoute("mortal_tiannan_to_chaotic_sea", "sea_ship", ""));
        assertFalse(FerryTravelPolicy.isFerryRoute("private_cross_region", "flying_boat", ""));
        assertFalse(FerryTravelPolicy.isFerryRoute("tianyuan_array", "teleport_array", "sect_gate"));
    }

    @Test
    void breakthroughBonusRequiresAConsistentActiveAuthoredRoll() {
        DailyEventEffectCatalog.Event gated = DailyEventEffectCatalog.parseForTest(new StringReader("""
                {"events":[{"id":"breakthrough_day","region":"tiannan","realm_min":"core_formation",
                  "effect":"breakthrough_chance_small"}]}
                """)).find("breakthrough_day").orElseThrow();
        long now = 1200L;
        CompoundTag valid = breakthroughState("breakthrough_day", "tiannan", 2400L, 2400L, 0.05D);

        assertEquals(0.05D, DailyEventEffectExecutor.breakthroughBonusForState(
                valid, gated, Realm.CORE_FORMATION, "tiannan", now), 0.00001D);
        assertEquals(0.0D, DailyEventEffectExecutor.breakthroughBonusForState(
                valid, gated, Realm.FOUNDATION_ESTABLISHMENT, "tiannan", now), 0.00001D);
        assertEquals(0.0D, DailyEventEffectExecutor.breakthroughBonusForState(
                valid, gated, Realm.CORE_FORMATION, "chaotic_sea", now), 0.00001D);

        CompoundTag missingRegion = breakthroughState("breakthrough_day", "", 2400L, 2400L, 0.05D);
        assertEquals(0.0D, DailyEventEffectExecutor.breakthroughBonusForState(
                missingRegion, gated, Realm.CORE_FORMATION, "tiannan", now), 0.00001D);

        CompoundTag orphaned = new CompoundTag();
        orphaned.putString("BreakthroughEvent", "breakthrough_day");
        orphaned.putLong("BreakthroughUntil", 2400L);
        orphaned.putDouble("BreakthroughBonus", 0.05D);
        assertEquals(0.0D, DailyEventEffectExecutor.breakthroughBonusForState(
                orphaned, gated, Realm.CORE_FORMATION, "tiannan", now), 0.00001D);

        CompoundTag overlong = breakthroughState("breakthrough_day", "tiannan", 2000L, 2400L, 0.20D);
        assertEquals(0.0D, DailyEventEffectExecutor.breakthroughBonusForState(
                overlong, gated, Realm.CORE_FORMATION, "tiannan", now), 0.00001D);
    }

    private static CompoundTag breakthroughState(String eventId, String region, long activeUntil,
                                                 long breakthroughUntil, double bonus) {
        CompoundTag tag = new CompoundTag();
        tag.putString("ActiveId", eventId);
        tag.putString("ActiveRegion", region);
        tag.putLong("ActiveUntil", activeUntil);
        tag.putString("BreakthroughEvent", eventId);
        tag.putLong("BreakthroughUntil", breakthroughUntil);
        tag.putDouble("BreakthroughBonus", bonus);
        return tag;
    }
}
