package com.xunxian.seekingimmortals.structure;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RefinementFurnaceStructureTest {
    @Test
    void physicalFurnaceFitsDeclaredThreeByThreeByThreeBounds() {
        List<BlockPos> walls = RefinementFurnaceStructure.wallOffsets();
        Set<BlockPos> uniqueWalls = new HashSet<>(walls);

        assertEquals(RefinementFurnaceStructure.SIZE, RefinementFurnaceStructure.HEIGHT);
        assertEquals(24, walls.size());
        assertEquals(walls.size(), uniqueWalls.size());
        for (BlockPos offset : walls) {
            assertTrue(Math.abs(offset.getX()) <= 1);
            assertTrue(offset.getY() >= -1 && offset.getY() <= 1);
            assertTrue(Math.abs(offset.getZ()) <= 1);
        }
        for (int y = -1; y <= 1; y++) {
            int layer = y;
            assertEquals(8, walls.stream().filter(offset -> offset.getY() == layer).count());
        }

        assertFalse(uniqueWalls.contains(BlockPos.ZERO), "controller must occupy the middle center");
        assertFalse(uniqueWalls.contains(new BlockPos(0, -1, 0)), "lava must occupy the bottom center");
        assertFalse(uniqueWalls.contains(new BlockPos(0, 1, 0)), "roof center must remain open");
    }

    @Test
    void smokestackClearanceStartsAboveThePhysicalFurnace() {
        assertEquals(List.of(new BlockPos(0, 1, 0)), RefinementFurnaceStructure.innerOffsets());
        assertEquals(List.of(
                        new BlockPos(0, 2, 0),
                        new BlockPos(0, 3, 0),
                        new BlockPos(0, 4, 0)),
                RefinementFurnaceStructure.smokestackOffsets());
    }
}
