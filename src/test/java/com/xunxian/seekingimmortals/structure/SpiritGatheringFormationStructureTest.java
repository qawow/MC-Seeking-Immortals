package com.xunxian.seekingimmortals.structure;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpiritGatheringFormationStructureTest {
    @Test
    void ringIsFiveByFiveBorderExcludingCore() {
        // Outer perimeter of 5x5: 5*4 - 4 = 16
        assertEquals(16, SpiritGatheringFormationStructure.ringOffsets().size());
        assertFalse(SpiritGatheringFormationStructure.ringOffsets().contains(BlockPos.ZERO));
        assertFalse(SpiritGatheringFormationStructure.ringOffsets().contains(new BlockPos(1, 0, 0)));
        assertFalse(SpiritGatheringFormationStructure.ringOffsets().contains(new BlockPos(0, 0, 1)));
        assertTrue(SpiritGatheringFormationStructure.ringOffsets().contains(new BlockPos(-2, 0, -2)));
        assertTrue(SpiritGatheringFormationStructure.ringOffsets().contains(new BlockPos(2, 0, 0)));
        assertTrue(SpiritGatheringFormationStructure.ringOffsets().contains(new BlockPos(0, 0, 2)));
        assertTrue(SpiritGatheringFormationStructure.ringOffsets().contains(new BlockPos(-2, 0, 1)));
    }

    @Test
    void completeOnlyWhenRingReady() {
        assertTrue(new SpiritGatheringFormationStructure.CheckResult(0).complete());
        assertFalse(new SpiritGatheringFormationStructure.CheckResult(1).complete());
        assertFalse(new SpiritGatheringFormationStructure.CheckResult(16).complete());
    }
}
