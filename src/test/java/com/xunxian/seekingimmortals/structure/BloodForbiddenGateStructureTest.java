package com.xunxian.seekingimmortals.structure;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BloodForbiddenGateStructureTest {
    @Test
    void ringIsSevenBySevenBorderExcludingCore() {
        assertEquals(24, BloodForbiddenGateStructure.ringOffsets().size());
        assertFalse(BloodForbiddenGateStructure.ringOffsets().contains(BlockPos.ZERO));
        assertTrue(BloodForbiddenGateStructure.ringOffsets().contains(new BlockPos(-3, 0, -3)));
        assertTrue(BloodForbiddenGateStructure.ringOffsets().contains(new BlockPos(3, 0, 0)));
    }

    @Test
    void frameIsFourCornerPillarsHeightFour() {
        assertEquals(16, BloodForbiddenGateStructure.frameOffsets().size());
        assertTrue(BloodForbiddenGateStructure.frameOffsets().contains(new BlockPos(-3, 1, -3)));
        assertTrue(BloodForbiddenGateStructure.frameOffsets().contains(new BlockPos(3, 4, 3)));
    }

    @Test
    void apertureIsFiveByFiveByFourAboveCore() {
        assertEquals(100, BloodForbiddenGateStructure.apertureOffsets().size());
        assertTrue(BloodForbiddenGateStructure.apertureOffsets().contains(new BlockPos(0, 1, 0)));
        assertTrue(BloodForbiddenGateStructure.apertureOffsets().contains(new BlockPos(-2, 4, 2)));
    }

    @Test
    void completeOnlyWhenRingFrameAndApertureReady() {
        assertTrue(new BloodForbiddenGateStructure.CheckResult(0, 0, 0).complete());
        assertFalse(new BloodForbiddenGateStructure.CheckResult(1, 0, 0).complete());
        assertFalse(new BloodForbiddenGateStructure.CheckResult(0, 1, 0).complete());
        assertFalse(new BloodForbiddenGateStructure.CheckResult(0, 0, 1).complete());
    }
}
