package com.xunxian.seekingimmortals.worldpack;

import com.xunxian.seekingimmortals.cultivation.PlayerCultivation;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DailyEventEffectCatalogTest {
    @Test
    void builtInCatalogPreservesDailyAndTianyuanSources() {
        DailyEventEffectCatalog.Snapshot catalog = DailyEventEffectCatalog.builtin();

        assertEquals(76, catalog.authoredEventCount());
        assertEquals(76, catalog.count());
        assertEquals(List.of("chaotic_sea"), event("chaotic_sea_beast_tide").regions());
        assertEquals(List.of("tianyuan"), event("tianyuan_merit_double_day").regions());
        assertTrue(event("spirit_rain").matchesRegion("anywhere"));
    }

    @Test
    void scalarArrayAndObjectSemanticsRemainExact() {
        DailyEventEffectCatalog.Event ghost = event("ghost_wail_night");
        DailyEventEffectCatalog.Event scout = event("evt_demonic_scout");
        DailyEventEffectCatalog.Event tax = event("chaotic_sea_tax_raid");

        assertEquals("yin_wraith", ghost.spawn());
        assertTrue(ghost.hasToken("yin_wraith"));
        assertTrue(scout.hasToken("random_ambush_low"));
        assertEquals("1.2", tax.objectEffects().get("tax_mult"));
        assertEquals("0.3", tax.objectEffects().get("inverse_star_smuggle_chance"));
        assertEquals(1.2D, DailyEventEffectExecutor.marketPriceMultiplier(
                tax, "chaotic_sea_island_general", "pearl"), 0.0001D);
    }

    @Test
    void eventsWithoutAuthoredEffectsReceiveNoSyntheticFallback() {
        DailyEventEffectCatalog.Event bandit = event("bandit_cultivator");

        assertTrue(bandit.legacyEffects().isEmpty());
        assertTrue(bandit.tokens().isEmpty());
        assertEquals(2, bandit.combatTier());
    }

    @Test
    void unknownFieldsAndTokensArePreservedAndCollectionsAreImmutable() {
        String json = """
                {"events":[{"id":"future_event","display":"Future","region":"tiannan",
                "weight":1,"effects":["future_effect"],"future_field":{"nested":true},
                "setting":{"lore":"test"}}]}
                """;
        DailyEventEffectCatalog.Event future = DailyEventEffectCatalog
                .parseForTest(new StringReader(json)).find("future_event").orElseThrow();

        assertEquals(List.of("future_effect"), future.unknownTokens());
        assertEquals(DailyEventEffectCatalog.Coverage.PRESERVED,
                future.authoredFields().stream()
                        .filter(field -> field.field().equals("future_field"))
                        .findFirst().orElseThrow().coverage());
        assertThrows(UnsupportedOperationException.class, () -> future.regions().add("dajin"));
        assertThrows(UnsupportedOperationException.class, () -> future.rawFields().put("x", "1"));
        assertThrows(UnsupportedOperationException.class, () -> future.tokens().clear());
    }

    @Test
    void fractionalWeightAndEveryAuthoredFieldRemainAvailable() {
        assertEquals(0.4D, event("open_sky_tribulation_omen").weight(), 0.0001D);

        for (DailyEventEffectCatalog.Event event : DailyEventEffectCatalog.builtin().list()) {
            assertFalse(event.authoredFields().isEmpty(), event.id());
            assertEquals(event.rawFields().size(), event.authoredFields().size(), event.id());
        }
    }

    @Test
    void pureRuntimeHelpersUseExactAuthoredMultipliers() {
        assertEquals(1.20D, DailyEventEffectExecutor.cultivationMultiplier(
                event("spirit_vein_pulse")), 0.0001D);
        assertEquals(30, DailyEventEffectExecutor.adjustContributionReward(
                20, event("contribution_bonus_day")));
        assertEquals(40, DailyEventEffectExecutor.adjustMeritReward(
                20, event("tianyuan_merit_double_day")));
        assertEquals(60, DailyEventEffectExecutor.adjustFerryCost(
                30, event("chaotic_sea_pirate_raid")));
        assertEquals(130, DailyEventEffectExecutor.adjustMarketCost(
                100, event("tiannan_herb_price_spike"), "market_herbal_stall", "spirit_grass"));
    }

    @Test
    void dailyEventBreakthroughBonusDoesNotBecomePillAssistance() {
        PlayerCultivation cultivation = new PlayerCultivation();
        PlayerCultivation.BreakthroughChanceBreakdown base = cultivation.getBreakthroughChanceBreakdown(
                PlayerCultivation.BreakthroughChanceModifiers.NONE);
        PlayerCultivation.BreakthroughChanceBreakdown boosted = cultivation.getBreakthroughChanceBreakdown(
                new PlayerCultivation.BreakthroughChanceModifiers(0.0D, 0.0D, 0.0D, 0.05D));

        assertEquals(base.chance() + 0.05D, boosted.chance(), 0.0001D);
        assertEquals(0.0D, base.eventBonus(), 0.0001D);
        assertEquals(0.05D, boosted.eventBonus(), 0.0001D);
        assertEquals(0.0D, cultivation.getBreakthroughPillBonus(), 0.0001D);
        assertFalse(cultivation.isBreakthroughAssisted());
    }

    private static DailyEventEffectCatalog.Event event(String id) {
        return DailyEventEffectCatalog.builtin().find(id)
                .orElseThrow(() -> new AssertionError("Missing daily event " + id));
    }
}
