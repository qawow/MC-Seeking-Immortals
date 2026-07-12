package com.xunxian.seekingimmortals.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;

/**
 * Refinement forge G2 middle size: full 3x3 base of forge blocks including center,
 * corner pillars y=1 only. Between g1 (3x3 no center) and g3 (r=2 ring).
 */
public final class RefinementForgeG2Structure {
    private static final List<BlockPos> BASE_OFFSETS = buildBaseOffsets();
    private static final List<BlockPos> FRAME_OFFSETS = buildFrameOffsets();

    private RefinementForgeG2Structure() {}

    public static CheckResult validate(Level level, BlockPos center, Block baseBlock, Block frameBlock) {
        int missingBase = 0;
        for (BlockPos offset : BASE_OFFSETS) {
            if (!level.getBlockState(center.offset(offset)).is(baseBlock)) {
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

    public record CheckResult(int missingBaseBlocks, int missingFrameBlocks) {
        public boolean complete() {
            return missingBaseBlocks <= 0 && missingFrameBlocks <= 0;
        }
    }

    private static List<BlockPos> buildBaseOffsets() {
        List<BlockPos> offsets = new ArrayList<>();
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                if (x == 0 && z == 0) continue; // center is controller itself
                offsets.add(new BlockPos(x, 0, z));
            }
        }
        return List.copyOf(offsets);
    }

    private static List<BlockPos> buildFrameOffsets() {
        List<BlockPos> offsets = new ArrayList<>();
        int[] corners = {-1, 1};
        for (int x : corners) {
            for (int z : corners) {
                offsets.add(new BlockPos(x, 1, z));
            }
        }
        return List.copyOf(offsets);
    }
}
