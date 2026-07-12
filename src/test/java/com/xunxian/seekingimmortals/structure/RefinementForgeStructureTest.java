package com.xunxian.seekingimmortals.structure;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RefinementForgeStructureTest {
    @Test
    void baseIsThreeByThreeExcludingCore() {
        assertEquals(8, RefinementForgeStructure.baseOffsets().size());
        assertFalse(RefinementForgeStructure.baseOffsets().contains(BlockPos.ZERO));
        assertTrue(RefinementForgeStructure.baseOffsets().contains(new BlockPos(-1, 0, -1)));
        assertTrue(RefinementForgeStructure.baseOffsets().contains(new BlockPos(1, 0, 0)));
    }

    @Test
    void frameIsFourCornerPillarsTwoHigh() {
        assertEquals(8, RefinementForgeStructure.frameOffsets().size());
        assertTrue(RefinementForgeStructure.frameOffsets().contains(new BlockPos(-1, 1, -1)));
        assertTrue(RefinementForgeStructure.frameOffsets().contains(new BlockPos(1, 2, 1)));
    }

    @Test
    void completeOnlyWhenBaseAndFrameReady() {
        assertTrue(new RefinementForgeStructure.CheckResult(0, 0).complete());
        assertFalse(new RefinementForgeStructure.CheckResult(1, 0).complete());
        assertFalse(new RefinementForgeStructure.CheckResult(0, 1).complete());
    }
}
