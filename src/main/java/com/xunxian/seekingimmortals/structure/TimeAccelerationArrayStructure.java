package com.xunxian.seekingimmortals.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;

/**
 * 时间加速阵基结构验证（5×5×1 时间类阵法）。
 * 结构：5×5 阵盘 + 四角时晶 + 中心核心 + 环绕聚灵阵。
 */
public final class TimeAccelerationArrayStructure {
    public static final int BASE_RADIUS = 2;

    private static final List<BlockPos> ARRAY_RING_OFFSETS = buildArrayRingOffsets();
    private static final List<BlockPos> TIME_CRYSTAL_OFFSETS = buildTimeCrystalOffsets();
    private static final BlockPos CENTER = BlockPos.ZERO;

    private TimeAccelerationArrayStructure() {}

    public static CheckResult validate(Level level, BlockPos center, Block arrayBlock, Block crystalBlock, Block coreBlock) {
        int missingRing = 0;
        for (BlockPos offset : ARRAY_RING_OFFSETS) {
            if (!level.getBlockState(center.offset(offset)).is(arrayBlock)) {
                missingRing++;
            }
        }

        int missingCrystals = 0;
        for (BlockPos offset : TIME_CRYSTAL_OFFSETS) {
            if (!level.getBlockState(center.offset(offset)).is(crystalBlock)) {
                missingCrystals++;
            }
        }

        boolean corePresent = level.getBlockState(center.offset(CENTER)).is(coreBlock);

        return new CheckResult(missingRing, missingCrystals, corePresent ? 0 : 1);
    }

    private static List<BlockPos> buildArrayRingOffsets() {
        List<BlockPos> offsets = new ArrayList<>();
        // 5×5 外圈阵盘（不包括四角和中心）
        for (int x = -BASE_RADIUS; x <= BASE_RADIUS; x++) {
            for (int z = -BASE_RADIUS; z <= BASE_RADIUS; z++) {
                // 排除四角
                if ((Math.abs(x) == BASE_RADIUS && Math.abs(z) == BASE_RADIUS) || (x == 0 && z == 0)) {
                    continue;
                }
                offsets.add(new BlockPos(x, 0, z));
            }
        }
        return List.copyOf(offsets);
    }

    private static List<BlockPos> buildTimeCrystalOffsets() {
        List<BlockPos> offsets = new ArrayList<>();
        // 四角时晶
        int[] corners = { -BASE_RADIUS, BASE_RADIUS };
        for (int x : corners) {
            for (int z : corners) {
                offsets.add(new BlockPos(x, 0, z));
            }
        }
        return List.copyOf(offsets);
    }

    public record CheckResult(int missingRing, int missingCrystals, int missingCore) {
        public boolean complete() {
            return missingRing <= 0 && missingCrystals <= 0 && missingCore <= 0;
        }
    }
}
