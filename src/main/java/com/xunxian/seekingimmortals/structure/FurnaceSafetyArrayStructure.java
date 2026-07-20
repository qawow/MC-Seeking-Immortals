package com.xunxian.seekingimmortals.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;

/**
 * 丹炉防爆小阵结构验证（3×3×1 防护阵法）。
 * 结构：3×3 基座 + 四角阵基 + 中心控制核心。
 */
public final class FurnaceSafetyArrayStructure {
    public static final int BASE_RADIUS = 1;

    private static final List<BlockPos> ARRAY_BASE_OFFSETS = buildArrayBaseOffsets();
    private static final List<BlockPos> CORNER_OFFSETS = buildCornerOffsets();
    private static final BlockPos CENTER = BlockPos.ZERO;

    private FurnaceSafetyArrayStructure() {}

    public static CheckResult validate(Level level, BlockPos center, Block arrayBlock, Block coreBlock) {
        int missingBase = 0;
        for (BlockPos offset : ARRAY_BASE_OFFSETS) {
            if (!level.getBlockState(center.offset(offset)).is(arrayBlock)) {
                missingBase++;
            }
        }

        int missingCorners = 0;
        for (BlockPos offset : CORNER_OFFSETS) {
            if (!level.getBlockState(center.offset(offset)).is(arrayBlock)) {
                missingCorners++;
            }
        }

        boolean corePresent = level.getBlockState(center.offset(CENTER)).is(coreBlock);

        return new CheckResult(missingBase, missingCorners, corePresent ? 0 : 1);
    }

    private static List<BlockPos> buildArrayBaseOffsets() {
        List<BlockPos> offsets = new ArrayList<>();
        // 四边中点阵基
        offsets.add(new BlockPos(-BASE_RADIUS, 0, 0));
        offsets.add(new BlockPos(BASE_RADIUS, 0, 0));
        offsets.add(new BlockPos(0, 0, -BASE_RADIUS));
        offsets.add(new BlockPos(0, 0, BASE_RADIUS));
        return List.copyOf(offsets);
    }

    private static List<BlockPos> buildCornerOffsets() {
        List<BlockPos> offsets = new ArrayList<>();
        // 四角阵基
        int[] corners = { -BASE_RADIUS, BASE_RADIUS };
        for (int x : corners) {
            for (int z : corners) {
                offsets.add(new BlockPos(x, 0, z));
            }
        }
        return List.copyOf(offsets);
    }

    public record CheckResult(int missingBase, int missingCorners, int missingCore) {
        public boolean complete() {
            return missingBase <= 0 && missingCorners <= 0 && missingCore <= 0;
        }
    }
}
