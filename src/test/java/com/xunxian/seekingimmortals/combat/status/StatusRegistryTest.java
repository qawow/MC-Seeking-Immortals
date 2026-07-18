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
}
