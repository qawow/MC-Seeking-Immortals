package com.xunxian.seekingimmortals.skill.effect.spell;

import com.xunxian.seekingimmortals.skill.effect.AbstractTechniqueEffectResolver;
import com.xunxian.seekingimmortals.skill.effect.TechniqueVfxPalette;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SelfBuffSpellTest {
    @Test
    void canonicalStatusDurationScalesWithVanillaBuffDuration() {
        assertEquals(600, SelfBuffSpell.scaledStatusDuration(600, 1));
        assertEquals(640, SelfBuffSpell.scaledStatusDuration(600, 3));
        assertEquals(1, SelfBuffSpell.scaledStatusDuration(0, 1));
    }

    @Test
    void authoredVisualIdentityOverridesTheFireballVirtualSkill() {
        var virtualSkill = AbstractTechniqueEffectResolver.virtualSkill();

        assertEquals("time_haste_self", SelfBuffSpell.resolveVisualSemantic(
                virtualSkill, "time_haste_self", "buff_self"));
        assertEquals(TechniqueVfxPalette.Family.WIND, SelfBuffSpell.resolveVisualFamily(
                virtualSkill, "wind", TechniqueVfxPalette.Family.NEUTRAL));
        assertEquals(TechniqueVfxPalette.Family.FIRE, SelfBuffSpell.resolveVisualFamily(
                virtualSkill, "", TechniqueVfxPalette.Family.NEUTRAL));
    }
}
