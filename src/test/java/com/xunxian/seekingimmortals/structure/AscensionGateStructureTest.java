package com.xunxian.seekingimmortals.structure;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AscensionGateStructureTest {
    @Test
    void ringIsSevenBySevenOuterEdge() {
        // perimeter of square radius 3 = 7*4-4 = 24
        assertEquals(24, AscensionGateStructure.ringOffsets().size());
        assertFalse(AscensionGateStructure.ringOffsets().contains(BlockPos.ZERO));
    }

    @Test
    void frameIsFourCornersFiveHigh() {
        assertEquals(20, AscensionGateStructure.frameOffsets().size());
        assertTrue(AscensionGateStructure.frameOffsets().contains(new BlockPos(-3, 1, -3)));
        assertTrue(AscensionGateStructure.frameOffsets().contains(new BlockPos(3, 5, 3)));
    }

    @Test
    void apertureIsFiveByFiveByFive() {
        assertEquals(125, AscensionGateStructure.apertureOffsets().size());
    }

    @Test
    void completeRequiresAllParts() {
        assertTrue(new AscensionGateStructure.CheckResult(0, 0, 0).complete());
        assertFalse(new AscensionGateStructure.CheckResult(1, 0, 0).complete());
        assertFalse(new AscensionGateStructure.CheckResult(0, 1, 0).complete());
        assertFalse(new AscensionGateStructure.CheckResult(0, 0, 1).complete());
    }
}
