package com.xunxian.seekingimmortals.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;

/**
 * 血池结构验证（适用于血色禁地血池、炼尸坑等血液类结构）。
 * 结构：5×5 池壁 + 底部 + 血液填充。
 */
public final class BloodPoolStructure {
    public static final int POOL_RADIUS = 2;
    public static final int POOL_DEPTH = 2;

    private static final List<BlockPos> POOL_FLOOR_OFFSETS = buildPoolFloorOffsets();
    private static final List<BlockPos> POOL_WALL_OFFSETS = buildPoolWallOffsets();
    private static final List<BlockPos> BLOOD_LIQUID_OFFSETS = buildBloodLiquidOffsets();

    private BloodPoolStructure() {}

    public static CheckResult validate(Level level, BlockPos center, Block poolBlock, Block bloodBlock) {
        int missingFloor = 0;
        for (BlockPos offset : POOL_FLOOR_OFFSETS) {
            if (!level.getBlockState(center.offset(offset)).is(poolBlock)) {
                missingFloor++;
            }
        }

        int missingWalls = 0;
        for (BlockPos offset : POOL_WALL_OFFSETS) {
            if (!level.getBlockState(center.offset(offset)).is(poolBlock)) {
                missingWalls++;
            }
        }

        int missingBlood = 0;
        for (BlockPos offset : BLOOD_LIQUID_OFFSETS) {
            if (!level.getBlockState(center.offset(offset)).is(bloodBlock)) {
                missingBlood++;
            }
        }

        return new CheckResult(missingFloor, missingWalls, missingBlood);
    }

    private static List<BlockPos> buildPoolFloorOffsets() {
        List<BlockPos> offsets = new ArrayList<>();
        int floorY = -POOL_DEPTH;
        for (int x = -POOL_RADIUS; x <= POOL_RADIUS; x++) {
            for (int z = -POOL_RADIUS; z <= POOL_RADIUS; z++) {
                offsets.add(new BlockPos(x, floorY, z));
            }
        }
        return List.copyOf(offsets);
    }

    private static List<BlockPos> buildPoolWallOffsets() {
        List<BlockPos> offsets = new ArrayList<>();
        for (int y = -POOL_DEPTH + 1; y <= 0; y++) {
            for (int x = -POOL_RADIUS; x <= POOL_RADIUS; x++) {
                for (int z = -POOL_RADIUS; z <= POOL_RADIUS; z++) {
                    // 只在边缘放置池壁
                    if (Math.abs(x) == POOL_RADIUS || Math.abs(z) == POOL_RADIUS) {
                        offsets.add(new BlockPos(x, y, z));
                    }
                }
            }
        }
        return List.copyOf(offsets);
    }

    private static List<BlockPos> buildBloodLiquidOffsets() {
        List<BlockPos> offsets = new ArrayList<>();
        // 池内血液填充（不含边缘）
        for (int y = -POOL_DEPTH + 1; y < 0; y++) {
            for (int x = -POOL_RADIUS + 1; x < POOL_RADIUS; x++) {
                for (int z = -POOL_RADIUS + 1; z < POOL_RADIUS; z++) {
                    offsets.add(new BlockPos(x, y, z));
                }
            }
        }
        return List.copyOf(offsets);
    }

    public record CheckResult(int missingFloor, int missingWalls, int missingBlood) {
        public boolean complete() {
            return missingFloor <= 0 && missingWalls <= 0 && missingBlood <= 0;
        }
    }
}
