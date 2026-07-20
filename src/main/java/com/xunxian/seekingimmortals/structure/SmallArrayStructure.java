package com.xunxian.seekingimmortals.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;

/**
 * 小型阵法结构通用验证器。
 * 适用于：阵旗桩、阵法维稳碑、灵田灌溉阵盘、丹炉防爆小阵、血色禁地出口阵等。
 * 结构：单柱/单点 + 周围能量节点。
 */
public final class SmallArrayStructure {
    private SmallArrayStructure() {}

    /**
     * 验证单柱阵法（如阵旗桩 1×1×2）
     */
    public static CheckResult validatePost(Level level, BlockPos center, Block postBlock, Block baseBlock) {
        boolean basePresent = level.getBlockState(center).is(baseBlock);
        boolean postPresent = level.getBlockState(center.above()).is(postBlock);

        return new CheckResult(basePresent ? 0 : 1, postPresent ? 0 : 1);
    }

    /**
     * 验证3×3阵盘（如出口阵、防爆阵）
     */
    public static CheckResult validate3x3Array(Level level, BlockPos center, Block arrayBlock, Block coreBlock) {
        List<BlockPos> ringOffsets = build3x3Ring();

        int missingRing = 0;
        for (BlockPos offset : ringOffsets) {
            if (!level.getBlockState(center.offset(offset)).is(arrayBlock)) {
                missingRing++;
            }
        }

        boolean corePresent = level.getBlockState(center).is(coreBlock);

        return new CheckResult(missingRing, corePresent ? 0 : 1);
    }

    /**
     * 验证单点灌溉阵盘（1×1×1）
     */
    public static CheckResult validateSinglePoint(Level level, BlockPos center, Block arrayBlock) {
        boolean present = level.getBlockState(center).is(arrayBlock);
        return new CheckResult(present ? 0 : 1, 0);
    }

    private static List<BlockPos> build3x3Ring() {
        List<BlockPos> offsets = new ArrayList<>();
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                if (x != 0 || z != 0) { // 排除中心
                    offsets.add(new BlockPos(x, 0, z));
                }
            }
        }
        return List.copyOf(offsets);
    }

    public record CheckResult(int missingPrimary, int missingSecondary) {
        public boolean complete() {
            return missingPrimary <= 0 && missingSecondary <= 0;
        }
    }
}
