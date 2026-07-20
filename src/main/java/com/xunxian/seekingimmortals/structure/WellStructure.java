package com.xunxian.seekingimmortals.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;

/**
 * 水井/灵井结构验证（适用于聚灵井、灵泉等竖井类结构）。
 * 结构：2×2 井口 + 垂直井壁 + 底部水源。
 */
public final class WellStructure {
    public static final int WELL_RADIUS = 1;
    public static final int WELL_DEPTH = 2;

    private static final List<BlockPos> WELL_RIM_OFFSETS = buildWellRimOffsets();
    private static final List<BlockPos> WELL_SHAFT_OFFSETS = buildWellShaftOffsets();
    private static final BlockPos WATER_SOURCE_OFFSET = new BlockPos(0, -WELL_DEPTH, 0);

    private WellStructure() {}

    public static CheckResult validate(Level level, BlockPos center, Block rimBlock, Block waterBlock) {
        int missingRim = 0;
        for (BlockPos offset : WELL_RIM_OFFSETS) {
            if (!level.getBlockState(center.offset(offset)).is(rimBlock)) {
                missingRim++;
            }
        }

        int missingShaft = 0;
        for (BlockPos offset : WELL_SHAFT_OFFSETS) {
            if (!level.getBlockState(center.offset(offset)).is(rimBlock)) {
                missingShaft++;
            }
        }

        // 验证底部水源
        boolean missingWater = !level.getBlockState(center.offset(WATER_SOURCE_OFFSET)).is(waterBlock);

        return new CheckResult(missingRim, missingShaft, missingWater);
    }

    private static List<BlockPos> buildWellRimOffsets() {
        List<BlockPos> offsets = new ArrayList<>();
        // 井口边缘围栏
        for (int x = -WELL_RADIUS; x <= WELL_RADIUS; x++) {
            for (int z = -WELL_RADIUS; z <= WELL_RADIUS; z++) {
                // 只在边缘放置
                if (Math.abs(x) == WELL_RADIUS || Math.abs(z) == WELL_RADIUS) {
                    offsets.add(new BlockPos(x, 0, z));
                }
            }
        }
        return List.copyOf(offsets);
    }

    private static List<BlockPos> buildWellShaftOffsets() {
        List<BlockPos> offsets = new ArrayList<>();
        // 井壁
        for (int y = -1; y > -WELL_DEPTH; y--) {
            for (int x = -WELL_RADIUS; x <= WELL_RADIUS; x++) {
                for (int z = -WELL_RADIUS; z <= WELL_RADIUS; z++) {
                    // 只在边缘放置井壁
                    if (Math.abs(x) == WELL_RADIUS || Math.abs(z) == WELL_RADIUS) {
                        offsets.add(new BlockPos(x, y, z));
                    }
                }
            }
        }
        return List.copyOf(offsets);
    }

    public record CheckResult(int missingRim, int missingShaft, boolean missingWater) {
        public boolean complete() {
            return missingRim <= 0 && missingShaft <= 0 && !missingWater;
        }
    }
}
