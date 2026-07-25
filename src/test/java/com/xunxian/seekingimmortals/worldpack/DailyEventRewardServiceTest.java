package com.xunxian.seekingimmortals.worldpack;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DailyEventRewardServiceTest {
    @Test
    void claimIdentityIncludesRegionEventAndRollExpiry() {
        assertEquals("tiannan|wandering_merchant|48000",
                DailyEventRewardService.claimKey("TIANNAN", "WANDERING_MERCHANT", 48000L));
        assertFalse(DailyEventRewardService.claimKey("tiannan", "wandering_merchant", 48000L)
                .equals(DailyEventRewardService.claimKey("tiannan", "wandering_merchant", 72000L)));
    }

    @Test
    void combatRewardsCannotUseThePassiveClaimPath() {
        assertTrue(DailyEventEncounterService.hasCombatPlan("tiannan",
                DailyEventEffectCatalog.builtin().find("rogue_cultivator_duel").orElseThrow()));
        assertTrue(DailyEventEncounterService.hasCombatPlan("tianyuan",
                DailyEventEffectCatalog.builtin().find("merit_convoy_ambush").orElseThrow()));
        assertFalse(DailyEventEncounterService.hasCombatPlan("tiannan",
                DailyEventEffectCatalog.builtin().find("talisman_master_visit").orElseThrow()));
    }
}
