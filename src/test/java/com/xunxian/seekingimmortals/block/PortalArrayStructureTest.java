package com.xunxian.seekingimmortals.block;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PortalArrayStructureTest {
    @Test
    void portalArrayBaseIsThirteenByThirteenAroundCore() {
        assertEquals(169, PortalArrayStructure.baseOffsets().size());
        assertTrue(PortalArrayStructure.baseOffsets().contains(BlockPos.ZERO));
        assertTrue(PortalArrayStructure.baseOffsets().contains(new BlockPos(-6, 0, -6)));
        assertTrue(PortalArrayStructure.baseOffsets().contains(new BlockPos(6, 0, 6)));
    }

    @Test
    void portalArrayRequiresFourTenHighCornerPillars() {
        assertEquals(40, PortalArrayStructure.frameOffsets().size());
        assertTrue(PortalArrayStructure.frameOffsets().contains(new BlockPos(-6, 1, -6)));
        assertTrue(PortalArrayStructure.frameOffsets().contains(new BlockPos(6, 5, -6)));
        assertTrue(PortalArrayStructure.frameOffsets().contains(new BlockPos(-6, 10, 6)));
        assertTrue(PortalArrayStructure.frameOffsets().contains(new BlockPos(6, 10, 6)));
    }

    @Test
    void portalArrayRequiresElevenByElevenNineHighAperture() {
        assertEquals(1089, PortalArrayStructure.apertureOffsets().size());
        assertTrue(PortalArrayStructure.apertureOffsets().contains(new BlockPos(0, 1, 0)));
        assertTrue(PortalArrayStructure.apertureOffsets().contains(new BlockPos(-5, 1, -5)));
        assertTrue(PortalArrayStructure.apertureOffsets().contains(new BlockPos(5, 9, 5)));
    }

    @Test
    void checkResultOnlyCompletesWhenBaseFrameAndApertureAreReady() {
        assertTrue(new PortalArrayStructure.CheckResult(0, 0, 0).complete());
        assertFalse(new PortalArrayStructure.CheckResult(1, 0, 0).complete());
        assertFalse(new PortalArrayStructure.CheckResult(0, 1, 0).complete());
        assertFalse(new PortalArrayStructure.CheckResult(0, 0, 1).complete());
    }
}
