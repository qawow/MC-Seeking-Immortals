package com.xunxian.seekingimmortals.skill.effect.spell;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SelfBuffSpellTest {
    @Test
    void canonicalStatusDurationScalesWithVanillaBuffDuration() {
        assertEquals(600, SelfBuffSpell.scaledStatusDuration(600, 1));
        assertEquals(640, SelfBuffSpell.scaledStatusDuration(600, 3));
        assertEquals(1, SelfBuffSpell.scaledStatusDuration(0, 1));
    }
}
