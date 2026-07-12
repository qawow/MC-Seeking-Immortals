package com.xunxian.seekingimmortals.structure;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PuppetAssemblyBenchStructureTest {
    @Test
    void baseIsThreeByThreeExcludingCore() {
        assertEquals(8, PuppetAssemblyBenchStructure.baseOffsets().size());
        assertFalse(PuppetAssemblyBenchStructure.baseOffsets().contains(BlockPos.ZERO));
    }

    @Test
    void frameIsFourCornerPillarsTwoHigh() {
        assertEquals(8, PuppetAssemblyBenchStructure.frameOffsets().size());
        assertTrue(PuppetAssemblyBenchStructure.frameOffsets().contains(new BlockPos(-1, 1, -1)));
    }

    @Test
    void completeOnlyWhenBaseAndFrameReady() {
        assertTrue(new PuppetAssemblyBenchStructure.CheckResult(0, 0).complete());
        assertFalse(new PuppetAssemblyBenchStructure.CheckResult(2, 0).complete());
    }
}
