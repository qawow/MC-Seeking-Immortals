package com.xunxian.seekingimmortals.structure;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LongRangeTeleportArrayStructureTest {
    @Test
    void ringIsNineByNineBorderExcludingCore() {
        // 9x9 square border: 9*4-4 = 32 edge blocks
        assertEquals(32, LongRangeTeleportArrayStructure.ringOffsets().size());
        assertFalse(LongRangeTeleportArrayStructure.ringOffsets().contains(BlockPos.ZERO));
        assertTrue(LongRangeTeleportArrayStructure.ringOffsets().contains(new BlockPos(-4, 0, -4)));
        assertTrue(LongRangeTeleportArrayStructure.ringOffsets().contains(new BlockPos(4, 0, 0)));
    }

    @Test
    void frameIsFourCornerPillarsHeightFive() {
        // 4 corners * 5 height = 20
        assertEquals(20, LongRangeTeleportArrayStructure.frameOffsets().size());
        assertTrue(LongRangeTeleportArrayStructure.frameOffsets().contains(new BlockPos(-4, 1, -4)));
        assertTrue(LongRangeTeleportArrayStructure.frameOffsets().contains(new BlockPos(4, 5, 4)));
        assertFalse(LongRangeTeleportArrayStructure.frameOffsets().contains(new BlockPos(-4, 0, -4)));
    }

    @Test
    void apertureIsSevenBySevenByFiveAboveCore() {
        // |x|,|z| <= 3, y = 1..5 => 7*7*5 = 245
        assertEquals(245, LongRangeTeleportArrayStructure.apertureOffsets().size());
        assertTrue(LongRangeTeleportArrayStructure.apertureOffsets().contains(new BlockPos(0, 1, 0)));
        assertTrue(LongRangeTeleportArrayStructure.apertureOffsets().contains(new BlockPos(-3, 5, 3)));
        assertFalse(LongRangeTeleportArrayStructure.apertureOffsets().contains(new BlockPos(0, 0, 0)));
    }

    @Test
    void completeOnlyWhenRingFrameAndApertureReady() {
        assertTrue(new LongRangeTeleportArrayStructure.CheckResult(0, 0, 0).complete());
        assertFalse(new LongRangeTeleportArrayStructure.CheckResult(1, 0, 0).complete());
        assertFalse(new LongRangeTeleportArrayStructure.CheckResult(0, 1, 0).complete());
        assertFalse(new LongRangeTeleportArrayStructure.CheckResult(0, 0, 1).complete());
    }
}
