package com.xunxian.seekingimmortals.structure;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpiritHerbPlanterStructureTest {
    @Test
    void radiusOneRingHasEightPlots() {
        assertEquals(8, SpiritHerbPlanterStructure.ringOffsets().size());
        assertFalse(SpiritHerbPlanterStructure.ringOffsets().contains(BlockPos.ZERO));
        assertTrue(SpiritHerbPlanterStructure.ringOffsets().contains(new BlockPos(1, 0, 0)));
        assertTrue(SpiritHerbPlanterStructure.ringOffsets().contains(new BlockPos(-1, 0, -1)));
    }

    @Test
    void completeWhenMissingSoilIsZero() {
        assertTrue(new SpiritHerbPlanterStructure.CheckResult(0).complete());
        assertFalse(new SpiritHerbPlanterStructure.CheckResult(2).complete());
    }
}
