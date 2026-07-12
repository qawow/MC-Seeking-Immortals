package com.xunxian.seekingimmortals.structure;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThunderTribulationAltarStructureTest {
    @Test
    void ringIsOuterEdgeRadiusTwoExcludingCore() {
        // Outer perimeter of 5x5: 5*4 - 4 = 16
        assertEquals(16, ThunderTribulationAltarStructure.ringOffsets().size());
        assertFalse(ThunderTribulationAltarStructure.ringOffsets().contains(BlockPos.ZERO));
        assertFalse(ThunderTribulationAltarStructure.ringOffsets().contains(new BlockPos(1, 0, 0)));
        assertFalse(ThunderTribulationAltarStructure.ringOffsets().contains(new BlockPos(0, 0, 1)));
        assertTrue(ThunderTribulationAltarStructure.ringOffsets().contains(new BlockPos(-2, 0, -2)));
        assertTrue(ThunderTribulationAltarStructure.ringOffsets().contains(new BlockPos(2, 0, 0)));
        assertTrue(ThunderTribulationAltarStructure.ringOffsets().contains(new BlockPos(0, 0, 2)));
        assertTrue(ThunderTribulationAltarStructure.ringOffsets().contains(new BlockPos(-2, 0, 1)));
    }

    @Test
    void pillarsAreFourCornersHeightThree() {
        // 4 corners * 3 height = 12
        assertEquals(12, ThunderTribulationAltarStructure.pillarOffsets().size());
        assertTrue(ThunderTribulationAltarStructure.pillarOffsets().contains(new BlockPos(-2, 1, -2)));
        assertTrue(ThunderTribulationAltarStructure.pillarOffsets().contains(new BlockPos(2, 3, 2)));
        assertFalse(ThunderTribulationAltarStructure.pillarOffsets().contains(new BlockPos(-2, 0, -2)));
        assertFalse(ThunderTribulationAltarStructure.pillarOffsets().contains(new BlockPos(0, 1, 0)));
        assertFalse(ThunderTribulationAltarStructure.pillarOffsets().contains(new BlockPos(-1, 1, -1)));
    }

    @Test
    void apertureIsThreeByThreeByThreeAboveCore() {
        // |x|,|z| <= 1 and y=1..3 => 3 * 3 * 3 = 27
        assertEquals(27, ThunderTribulationAltarStructure.apertureOffsets().size());
        assertTrue(ThunderTribulationAltarStructure.apertureOffsets().contains(new BlockPos(0, 1, 0)));
        assertTrue(ThunderTribulationAltarStructure.apertureOffsets().contains(new BlockPos(1, 2, -1)));
        assertTrue(ThunderTribulationAltarStructure.apertureOffsets().contains(new BlockPos(-1, 3, 1)));
        assertFalse(ThunderTribulationAltarStructure.apertureOffsets().contains(new BlockPos(0, 0, 0)));
        assertFalse(ThunderTribulationAltarStructure.apertureOffsets().contains(new BlockPos(2, 1, 0)));
    }

    @Test
    void completeOnlyWhenRingPillarsAndApertureReady() {
        assertTrue(new ThunderTribulationAltarStructure.CheckResult(0, 0, 0).complete());
        assertFalse(new ThunderTribulationAltarStructure.CheckResult(1, 0, 0).complete());
        assertFalse(new ThunderTribulationAltarStructure.CheckResult(0, 1, 0).complete());
        assertFalse(new ThunderTribulationAltarStructure.CheckResult(0, 0, 1).complete());
    }
}
