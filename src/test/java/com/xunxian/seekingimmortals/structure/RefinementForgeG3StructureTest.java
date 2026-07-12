package com.xunxian.seekingimmortals.structure;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RefinementForgeG3StructureTest {
    @Test
    void ringIsFiveByFiveBorderExcludingCore() {
        // 5x5 border: 5*4-4 = 16
        assertEquals(16, RefinementForgeG3Structure.ringOffsets().size());
        assertFalse(RefinementForgeG3Structure.ringOffsets().contains(BlockPos.ZERO));
        assertTrue(RefinementForgeG3Structure.ringOffsets().contains(new BlockPos(2, 0, 0)));
    }

    @Test
    void frameIsFourCornerPillarsHeightThree() {
        assertEquals(12, RefinementForgeG3Structure.frameOffsets().size());
        assertTrue(RefinementForgeG3Structure.frameOffsets().contains(new BlockPos(-2, 1, -2)));
        assertTrue(RefinementForgeG3Structure.frameOffsets().contains(new BlockPos(2, 3, 2)));
    }

    @Test
    void apertureIsThreeByThreeByThree() {
        assertEquals(27, RefinementForgeG3Structure.apertureOffsets().size());
    }

    @Test
    void completeOnlyWhenAllReady() {
        assertTrue(new RefinementForgeG3Structure.CheckResult(0, 0, 0).complete());
        assertFalse(new RefinementForgeG3Structure.CheckResult(1, 0, 0).complete());
        assertFalse(new RefinementForgeG3Structure.CheckResult(0, 1, 0).complete());
        assertFalse(new RefinementForgeG3Structure.CheckResult(0, 0, 1).complete());
    }
}
