package com.xunxian.seekingimmortals.skill;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpecialSkillServiceTest {
    @Test
    void multiCastCooldownScaleDefaultsToOneWithoutPlayer() {
        assertEquals(1.0D, SpecialSkillService.multiCastCooldownScale(null), 1e-9);
    }

    @Test
    void dualCastRequiresSkillLevel() {
        assertEquals(0, SpecialSkillService.dualCastExtraSlots(null));
        assertTrue(!SpecialSkillService.canDualCast(null));
    }

    @Test
    void specialsArrayCoversExpectedSurface() {
        assertTrue(SpecialSkillService.SPECIALS.length >= 7);
        boolean hasMulti = false;
        for (SkillType type : SpecialSkillService.SPECIALS) {
            assertEquals(SkillCategory.SPECIAL, type.getCategory());
            if (type == SkillType.MULTI_CASTING) {
                hasMulti = true;
            }
        }
        assertTrue(hasMulti);
    }
}
