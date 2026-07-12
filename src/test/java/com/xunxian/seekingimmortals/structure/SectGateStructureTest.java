package com.xunxian.seekingimmortals.structure;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SectGateStructureTest {
    @Test
    void ringIsSevenBySevenBorderExcludingCore() {
        // 7x7 square border: 7*4-4 = 24 edge blocks
        assertEquals(24, SectGateStructure.ringOffsets().size());
        assertFalse(SectGateStructure.ringOffsets().contains(BlockPos.ZERO));
        assertTrue(SectGateStructure.ringOffsets().contains(new BlockPos(-3, 0, -3)));
        assertTrue(SectGateStructure.ringOffsets().contains(new BlockPos(3, 0, 0)));
        assertTrue(SectGateStructure.ringOffsets().contains(new BlockPos(0, 0, 3)));
    }

    @Test
    void frameIsFourCornerPillarsHeightFour() {
        // 4 corners * 4 height = 16
        assertEquals(16, SectGateStructure.frameOffsets().size());
        assertTrue(SectGateStructure.frameOffsets().contains(new BlockPos(-3, 1, -3)));
        assertTrue(SectGateStructure.frameOffsets().contains(new BlockPos(3, 4, 3)));
        assertFalse(SectGateStructure.frameOffsets().contains(new BlockPos(-3, 0, -3)));
    }

    @Test
    void apertureIsFiveByFiveByFourAboveCore() {
        // |x|,|z| <= 2, y = 1..4 => 5*5*4 = 100
        assertEquals(100, SectGateStructure.apertureOffsets().size());
        assertTrue(SectGateStructure.apertureOffsets().contains(new BlockPos(0, 1, 0)));
        assertTrue(SectGateStructure.apertureOffsets().contains(new BlockPos(-2, 4, 2)));
        assertFalse(SectGateStructure.apertureOffsets().contains(new BlockPos(0, 0, 0)));
    }

    @Test
    void completeOnlyWhenRingFrameAndApertureReady() {
        assertTrue(new SectGateStructure.CheckResult(0, 0, 0).complete());
        assertFalse(new SectGateStructure.CheckResult(1, 0, 0).complete());
        assertFalse(new SectGateStructure.CheckResult(0, 1, 0).complete());
        assertFalse(new SectGateStructure.CheckResult(0, 0, 1).complete());
    }
}
