package com.xunxian.seekingimmortals.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;

/**
 * Talisman table multiblock: 3x3 base of table blocks excluding center,
 * plus four corner spirit-ore pillars y=1..2.
 * Text-material block_items_catalog id: talisman_table.
 */
public final class TalismanTableStructure {
    private static final List<BlockPos> BASE_OFFSETS = buildBaseOffsets();
    private static final List<BlockPos> FRAME_OFFSETS = buildFrameOffsets();

    private TalismanTableStructure() {}

    public static CheckResult validate(Level level, BlockPos center, Block tableBlock, Block frameBlock) {
        int missingBase = 0;
        for (BlockPos offset : BASE_OFFSETS) {
            if (!level.getBlockState(center.offset(offset)).is(tableBlock)) {
                missingBase++;
            }
        }
        int missingFrame = 0;
        for (BlockPos offset : FRAME_OFFSETS) {
            if (!level.getBlockState(center.offset(offset)).is(frameBlock)) {
                missingFrame++;
            }
        }
        return new CheckResult(missingBase, missingFrame);
    }

    public static List<BlockPos> baseOffsets() {
        return BASE_OFFSETS;
    }

    public static List<BlockPos> frameOffsets() {
        return FRAME_OFFSETS;
    }

    private static List<BlockPos> buildBaseOffsets() {
        List<BlockPos> offsets = new ArrayList<>();
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                if (x == 0 && z == 0) {
                    continue;
                }
                offsets.add(new BlockPos(x, 0, z));
            }
        }
        return List.copyOf(offsets);
    }

    private static List<BlockPos> buildFrameOffsets() {
        List<BlockPos> offsets = new ArrayList<>();
        int[] corners = {-1, 1};
        for (int y = 1; y <= 2; y++) {
            for (int x : corners) {
                for (int z : corners) {
                    offsets.add(new BlockPos(x, y, z));
                }
            }
        }
        return List.copyOf(offsets);
    }

    public record CheckResult(int missingBaseBlocks, int missingFrameBlocks) {
        public boolean complete() {
            return missingBaseBlocks <= 0 && missingFrameBlocks <= 0;
        }
    }
}
