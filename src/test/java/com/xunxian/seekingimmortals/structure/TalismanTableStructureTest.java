package com.xunxian.seekingimmortals.structure;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TalismanTableStructureTest {
    @Test
    void baseIsThreeByThreeExcludingCore() {
        assertEquals(8, TalismanTableStructure.baseOffsets().size());
        assertFalse(TalismanTableStructure.baseOffsets().contains(BlockPos.ZERO));
    }

    @Test
    void frameIsFourCornerPillarsTwoHigh() {
        assertEquals(8, TalismanTableStructure.frameOffsets().size());
        assertTrue(TalismanTableStructure.frameOffsets().contains(new BlockPos(-1, 1, -1)));
        assertTrue(TalismanTableStructure.frameOffsets().contains(new BlockPos(1, 2, 1)));
    }

    @Test
    void completeOnlyWhenBaseAndFrameReady() {
        assertTrue(new TalismanTableStructure.CheckResult(0, 0).complete());
        assertFalse(new TalismanTableStructure.CheckResult(1, 0).complete());
        assertFalse(new TalismanTableStructure.CheckResult(0, 1).complete());
    }
}
