package com.xunxian.seekingimmortals.structure;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FixedTeleportArrayStructureTest {
    @Test
    void ringIsFiveByFiveBorderExcludingCore() {
        // 5x5 square border: 5*4-4 = 16 edge blocks
        assertEquals(16, FixedTeleportArrayStructure.ringOffsets().size());
        assertFalse(FixedTeleportArrayStructure.ringOffsets().contains(BlockPos.ZERO));
        assertTrue(FixedTeleportArrayStructure.ringOffsets().contains(new BlockPos(-2, 0, -2)));
        assertTrue(FixedTeleportArrayStructure.ringOffsets().contains(new BlockPos(2, 0, 0)));
        assertTrue(FixedTeleportArrayStructure.ringOffsets().contains(new BlockPos(0, 0, 2)));
    }

    @Test
    void apertureIsThreeByThreeByThreeAboveCore() {
        assertEquals(27, FixedTeleportArrayStructure.apertureOffsets().size());
        assertTrue(FixedTeleportArrayStructure.apertureOffsets().contains(new BlockPos(0, 1, 0)));
        assertTrue(FixedTeleportArrayStructure.apertureOffsets().contains(new BlockPos(-1, 3, 1)));
    }

    @Test
    void completeOnlyWhenRingAndApertureReady() {
        assertTrue(new FixedTeleportArrayStructure.CheckResult(0, 0).complete());
        assertFalse(new FixedTeleportArrayStructure.CheckResult(1, 0).complete());
        assertFalse(new FixedTeleportArrayStructure.CheckResult(0, 1).complete());
    }
}
