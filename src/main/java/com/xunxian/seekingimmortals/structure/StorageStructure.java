package com.xunxian.seekingimmortals.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;

/**
 * 储藏结构验证（适用于灵石库、仓库等储存类结构）。
 * 结构：3×3 基座 + 四周围墙 + 内部储物空间 + 入口验证。
 */
public final class StorageStructure {
    public static final int WALL_RADIUS = 1;
    public static final int WALL_HEIGHT = 2;

    private static final List<BlockPos> FLOOR_OFFSETS = buildFloorOffsets();
    private static final List<BlockPos> WALL_OFFSETS = buildWallOffsets();
    private static final BlockPos ENTRANCE_OFFSET = new BlockPos(0, 0, WALL_RADIUS);

    private StorageStructure() {}

    public static CheckResult validate(Level level, BlockPos center, Block wallBlock, Block floorBlock) {
        int missingFloor = 0;
        for (BlockPos offset : FLOOR_OFFSETS) {
            if (!level.getBlockState(center.offset(offset)).is(floorBlock)) {
                missingFloor++;
            }
        }

        int missingWalls = 0;
        for (BlockPos offset : WALL_OFFSETS) {
            // 跳过入口位置
            if (offset.equals(ENTRANCE_OFFSET)) {
                continue;
            }
            if (!level.getBlockState(center.offset(offset)).is(wallBlock)) {
                missingWalls++;
            }
        }

        // 验证入口是否畅通
        boolean entranceBlocked = !level.getBlockState(center.offset(ENTRANCE_OFFSET)).isAir();

        return new CheckResult(missingFloor, missingWalls, entranceBlocked);
    }

    private static List<BlockPos> buildFloorOffsets() {
        List<BlockPos> offsets = new ArrayList<>();
        for (int x = -WALL_RADIUS; x <= WALL_RADIUS; x++) {
            for (int z = -WALL_RADIUS; z <= WALL_RADIUS; z++) {
                offsets.add(new BlockPos(x, 0, z));
            }
        }
        return List.copyOf(offsets);
    }

    private static List<BlockPos> buildWallOffsets() {
        List<BlockPos> offsets = new ArrayList<>();
        for (int y = 1; y <= WALL_HEIGHT; y++) {
            for (int x = -WALL_RADIUS; x <= WALL_RADIUS; x++) {
                for (int z = -WALL_RADIUS; z <= WALL_RADIUS; z++) {
                    // 只在边缘放置墙壁
                    if (Math.abs(x) == WALL_RADIUS || Math.abs(z) == WALL_RADIUS) {
                        offsets.add(new BlockPos(x, y, z));
                    }
                }
            }
        }
        return List.copyOf(offsets);
    }

    public record CheckResult(int missingFloor, int missingWalls, boolean entranceBlocked) {
        public boolean complete() {
            return missingFloor <= 0 && missingWalls <= 0 && !entranceBlocked;
        }
    }
}
