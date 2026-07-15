package com.xunxian.seekingimmortals.structure;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AlchemyFurnaceShellStructureTest {
    @Test
    void lowTierRequiresOnlyTopLid() {
        List<BlockPos> g1 = AlchemyFurnaceShellStructure.requiredOffsets(1);
        List<BlockPos> g2 = AlchemyFurnaceShellStructure.requiredOffsets(2);
        assertEquals(1, g1.size());
        assertEquals(1, g2.size());
        assertEquals(1, AlchemyFurnaceShellStructure.requiredCount(1));
        assertTrue(g1.contains(new BlockPos(0, 1, 0)));
        assertFalse(g1.contains(new BlockPos(1, 0, 0)));
    }

    @Test
    void highTierRequiresLidOuterRingAndMagmaUnder() {
        // Lid + radius-2 outer ring (16) = 17; T4+ adds magma under => 18.
        assertEquals(17, AlchemyFurnaceShellStructure.requiredCount(3));
        assertEquals(18, AlchemyFurnaceShellStructure.requiredCount(4));
        assertEquals(18, AlchemyFurnaceShellStructure.requiredCount(5));

        HashSet<BlockPos> t4 = new HashSet<>(AlchemyFurnaceShellStructure.requiredOffsets(4));
        assertTrue(t4.contains(new BlockPos(0, 1, 0)));
        assertTrue(t4.contains(new BlockPos(0, -1, 0)));
        assertTrue(t4.contains(new BlockPos(2, 0, 0)));
    }

    @Test
    void predicateRequirementSupportsCustomMatchersWithoutRegistryBlocks() {
        MultiblockPattern.BlockRequirement always = new MultiblockPattern.BlockRequirement(
                new BlockPos(1, 0, 0), state -> true);
        MultiblockPattern.BlockRequirement never = new MultiblockPattern.BlockRequirement(
                new BlockPos(-1, 0, 0), state -> false);
        assertEquals(new BlockPos(1, 0, 0), always.offset());
        assertFalse(never.matcher().test(null));
        assertTrue(always.matcher().test(null));
        assertEquals(2, MultiblockPattern.offsets(List.of(always, never)).size());
    }
}
