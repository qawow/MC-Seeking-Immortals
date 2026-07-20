package com.xunxian.seekingimmortals.structure;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RefinementForgeHighStructureTest {
    @Test
    void ringRadiusScalesWithGrade() {
        assertEquals(3, Math.max(2, Math.min(6, 4 - 1)));
        assertEquals(24, RefinementForgeHighStructure.ringOffsets(3).size()); // 7x7 ring perimeter-ish
        assertEquals(32, RefinementForgeHighStructure.ringOffsets(4).size());
        assertEquals(40, RefinementForgeHighStructure.ringOffsets(5).size());
        assertFalse(RefinementForgeHighStructure.ringOffsets(3).contains(BlockPos.ZERO));
        assertTrue(RefinementForgeHighStructure.ringOffsets(3).contains(new BlockPos(3, 0, 0)));
        assertTrue(RefinementForgeHighStructure.ringOffsets(3).contains(new BlockPos(-3, 0, 2)));
    }

    @Test
    void frameIsFourCornerPillars() {
        assertEquals(16, RefinementForgeHighStructure.frameOffsets(3, 4).size()); // 4 corners * 4 height
        assertTrue(RefinementForgeHighStructure.frameOffsets(3, 4).contains(new BlockPos(3, 1, 3)));
        assertTrue(RefinementForgeHighStructure.frameOffsets(3, 4).contains(new BlockPos(-3, 4, -3)));
    }

    @Test
    void completeOnlyWhenAllPartsReady() {
        assertTrue(new RefinementForgeHighStructure.CheckResult(0, 0, 0, 4).complete());
        assertFalse(new RefinementForgeHighStructure.CheckResult(1, 0, 0, 4).complete());
        assertFalse(new RefinementForgeHighStructure.CheckResult(0, 1, 0, 5).complete());
        assertFalse(new RefinementForgeHighStructure.CheckResult(0, 0, 1, 6).complete());
    }

    @Test
    void highForgeValidatorsAreDedicatedNotFallbackRing() throws Exception {
        String source = java.nio.file.Files.readString(java.nio.file.Path.of(
                "src", "main", "java", "com", "xunxian", "seekingimmortals",
                "structure", "MultiblockStationService.java"));
        assertTrue(source.contains("case \"refinement_forge_g4\""));
        assertTrue(source.contains("case \"refinement_forge_g5\""));
        assertTrue(source.contains("case \"refinement_forge_g6\""));
        assertTrue(source.contains("RefinementForgeHighStructure.validate"));
    }
}
