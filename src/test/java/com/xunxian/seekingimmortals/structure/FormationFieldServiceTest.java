package com.xunxian.seekingimmortals.structure;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FormationFieldServiceTest {
    @Test
    void fieldKindsExposeRingConfig() {
        assertEquals(2, FormationFieldService.FieldKind.SPIRIT_GATHER.radius());
        assertTrue(FormationFieldService.FieldKind.SPIRIT_GATHER.usesSpiritGatheringRing());
        assertEquals(2, FormationFieldService.FieldKind.KILL_SWORD.radius());
        assertTrue(!FormationFieldService.FieldKind.KILL_SWORD.usesSpiritGatheringRing());
    }

    @Test
    void activeCountStartsEmptyAfterClear() {
        FormationFieldService.clearAll();
        assertEquals(0, FormationFieldService.activeCount());
    }

    @Test
    void fieldEffectContainsUsesRadius() {
        FormationFieldService.FieldEffect effect = new FormationFieldService.FieldEffect(
                "spirit_gather",
                FormationFieldService.FieldKind.SPIRIT_GATHER,
                new net.minecraft.core.BlockPos(0, 64, 0),
                2,
                100,
                50,
                "cultivation_speed_1.2",
                false);
        assertTrue(effect.contains(new net.minecraft.core.BlockPos(2, 64, 0)));
        assertTrue(!effect.contains(new net.minecraft.core.BlockPos(4, 64, 0)));
    }
}
