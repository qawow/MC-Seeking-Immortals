package com.xunxian.seekingimmortals.structure;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SectEarthFireRoomMultiblockTest {
    @Test
    void earthFireRoomBlueprintRequiresAnchorMagmaAndFourArrayNodes() {
        List<BlockPos> offsets = SectEarthFireRoomMultiblock.requiredOffsets();

        assertEquals(6, new HashSet<>(offsets).size());
        assertTrue(offsets.contains(BlockPos.ZERO));
        assertTrue(offsets.contains(new BlockPos(0, -1, 0)));
        assertTrue(offsets.contains(new BlockPos(1, 0, 0)));
        assertTrue(offsets.contains(new BlockPos(-1, 0, 0)));
        assertTrue(offsets.contains(new BlockPos(0, 0, 1)));
        assertTrue(offsets.contains(new BlockPos(0, 0, -1)));
    }

    @Test
    void scanRangeMatchesPlayerFacingEarthFireText() {
        assertEquals(5, SectEarthFireRoomMultiblock.HORIZONTAL_RANGE);
        assertEquals(2, SectEarthFireRoomMultiblock.VERTICAL_RANGE);
    }
}
