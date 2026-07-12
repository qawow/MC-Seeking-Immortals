package com.xunxian.seekingimmortals.structure;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RingFormationStructureTest {
    @Test
    void defaultRadiusTwoRingHasSixteenBlocks() {
        assertEquals(16, RingFormationStructure.ringOffsets(2).size());
        assertFalse(RingFormationStructure.ringOffsets(2).contains(BlockPos.ZERO));
    }

    @Test
    void completeWhenMissingRingIsZero() {
        assertTrue(new RingFormationStructure.CheckResult(0).complete());
        assertFalse(new RingFormationStructure.CheckResult(3).complete());
    }

    @Test
    void radiusOneRingHasEightBlocks() {
        assertEquals(8, RingFormationStructure.ringOffsets(1).size());
        assertFalse(RingFormationStructure.ringOffsets(1).contains(BlockPos.ZERO));
        assertTrue(RingFormationStructure.ringOffsets(1).contains(new BlockPos(1, 0, 0)));
        assertTrue(RingFormationStructure.ringOffsets(1).contains(new BlockPos(-1, 0, -1)));
    }

    @Test
    void radiusThreeRingHasTwentyFourBlocks() {
        assertEquals(24, RingFormationStructure.ringOffsets(3).size());
    }
}
