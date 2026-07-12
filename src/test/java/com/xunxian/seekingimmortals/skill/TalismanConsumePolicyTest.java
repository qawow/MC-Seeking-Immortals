package com.xunxian.seekingimmortals.skill;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TalismanConsumePolicyTest {
    @Test
    void detectsCastTalismanTechniqueIds() {
        assertTrue(TalismanConsumePolicy.requiresTalisman("cast_fire_burst_talisman", null));
        assertTrue(TalismanConsumePolicy.requiresTalisman("fire_talisman", null));
        assertFalse(TalismanConsumePolicy.requiresTalisman("fireball_art", null));
        assertFalse(TalismanConsumePolicy.requiresTalisman("beast_summon", null));
    }
}
