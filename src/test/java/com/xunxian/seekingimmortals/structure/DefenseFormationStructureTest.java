package com.xunxian.seekingimmortals.structure;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefenseFormationStructureTest {
    @Test
    void ringIsFiveByFiveBorderExcludingCore() {
        // Outer perimeter of 5x5: 5*4 - 4 = 16
        assertEquals(16, DefenseFormationStructure.ringOffsets().size());
        assertFalse(DefenseFormationStructure.ringOffsets().contains(BlockPos.ZERO));
        assertFalse(DefenseFormationStructure.ringOffsets().contains(new BlockPos(1, 0, 0)));
        assertFalse(DefenseFormationStructure.ringOffsets().contains(new BlockPos(0, 0, 1)));
        assertTrue(DefenseFormationStructure.ringOffsets().contains(new BlockPos(-2, 0, -2)));
        assertTrue(DefenseFormationStructure.ringOffsets().contains(new BlockPos(2, 0, 0)));
        assertTrue(DefenseFormationStructure.ringOffsets().contains(new BlockPos(0, 0, 2)));
        assertTrue(DefenseFormationStructure.ringOffsets().contains(new BlockPos(-2, 0, 1)));
    }

    @Test
    void completeOnlyWhenRingReady() {
        assertTrue(new DefenseFormationStructure.CheckResult(0).complete());
        assertFalse(new DefenseFormationStructure.CheckResult(1).complete());
        assertFalse(new DefenseFormationStructure.CheckResult(16).complete());
    }
}
