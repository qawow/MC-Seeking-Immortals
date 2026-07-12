package com.xunxian.seekingimmortals.structure;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BloodSacrificeAltarStructureTest {
    @Test
    void baseIsThreeByThreeFloorExcludingCore() {
        assertEquals(8, BloodSacrificeAltarStructure.baseOffsets().size());
        assertFalse(BloodSacrificeAltarStructure.baseOffsets().contains(BlockPos.ZERO));
        assertTrue(BloodSacrificeAltarStructure.baseOffsets().contains(new BlockPos(-1, 0, -1)));
        assertTrue(BloodSacrificeAltarStructure.baseOffsets().contains(new BlockPos(1, 0, 0)));
        assertTrue(BloodSacrificeAltarStructure.baseOffsets().contains(new BlockPos(0, 0, 1)));
    }

    @Test
    void pillarsAreFourCornersHeightTwo() {
        // 4 corners * 2 height = 8
        assertEquals(8, BloodSacrificeAltarStructure.pillarOffsets().size());
        assertTrue(BloodSacrificeAltarStructure.pillarOffsets().contains(new BlockPos(-1, 1, -1)));
        assertTrue(BloodSacrificeAltarStructure.pillarOffsets().contains(new BlockPos(1, 2, 1)));
        assertFalse(BloodSacrificeAltarStructure.pillarOffsets().contains(new BlockPos(-1, 0, -1)));
        assertFalse(BloodSacrificeAltarStructure.pillarOffsets().contains(new BlockPos(0, 1, 0)));
    }

    @Test
    void apertureIsTwoBlocksAboveCore() {
        assertEquals(2, BloodSacrificeAltarStructure.apertureOffsets().size());
        assertTrue(BloodSacrificeAltarStructure.apertureOffsets().contains(new BlockPos(0, 1, 0)));
        assertTrue(BloodSacrificeAltarStructure.apertureOffsets().contains(new BlockPos(0, 2, 0)));
        assertFalse(BloodSacrificeAltarStructure.apertureOffsets().contains(new BlockPos(0, 0, 0)));
    }

    @Test
    void completeOnlyWhenBasePillarsAndApertureReady() {
        assertTrue(new BloodSacrificeAltarStructure.CheckResult(0, 0, 0).complete());
        assertFalse(new BloodSacrificeAltarStructure.CheckResult(1, 0, 0).complete());
        assertFalse(new BloodSacrificeAltarStructure.CheckResult(0, 1, 0).complete());
        assertFalse(new BloodSacrificeAltarStructure.CheckResult(0, 0, 1).complete());
    }
}
