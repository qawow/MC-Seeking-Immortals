package com.xunxian.seekingimmortals.combat.status;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StatusRegistryTest {

    @Test
    void outgoingDamageMultipliersStackMultiplicatively() {
        assertEquals(1.0D, StatusRegistryTestSupport.outgoingDamageMultiplier(), 0.0001D);

        double multiplier = StatusRegistryTestSupport.outgoingDamageMultiplier("berserk", "sword_intent");

        assertEquals(1.20D * 1.10D, multiplier, 0.0001D);
    }

    @Test
    void activeSealAndConcealStatusesExposeTheirConsumerFlags() {
        assertTrue(StatusRegistryTestSupport.blocksTechnique("seal_nascent"));
        assertFalse(StatusRegistryTestSupport.blocksTechnique("conceal_qi"));
        assertTrue(StatusRegistryTestSupport.hidesRealm("conceal_qi"));
        assertFalse(StatusRegistryTestSupport.hidesRealm("seal_nascent"));
    }

    @Test
    void accuracyDeltasStackAndResistedDurationsNeverGrow() {
        assertEquals(-0.18D,
                StatusRegistryTestSupport.accuracyDelta("soul_shock", "fear"), 0.0001D);
        assertEquals(2, StatusRegistry.resistedDuration(4, 0.50D));
        assertEquals(1, StatusRegistry.resistedDuration(1, 0.75D));
        assertEquals(4, StatusRegistry.resistedDuration(4, 0.0D));
    }

    @Test
    void emergencyAntidoteStillHonorsDeclaredFamilies() {
        StatusCatalogService.AntidoteClear rule = StatusCatalogService.builtin().antidotes().stream()
                .filter(antidote -> "huiyang_emergency".equals(antidote.id()))
                .findFirst()
                .orElseThrow();
        assertTrue(rule.emergency());
        assertTrue(PoisonAntidoteService.matchesFamilyRule(rule,
                StatusCatalogService.builtin().find("poison").orElseThrow()));
        assertTrue(PoisonAntidoteService.matchesFamilyRule(rule,
                StatusCatalogService.builtin().find("soul_wound").orElseThrow()));
        assertFalse(PoisonAntidoteService.matchesFamilyRule(rule,
                StatusCatalogService.builtin().find("shield").orElseThrow()));
        assertFalse(PoisonAntidoteService.matchesFamilyRule(rule,
                StatusCatalogService.builtin().find("sword_intent").orElseThrow()));
    }
}
