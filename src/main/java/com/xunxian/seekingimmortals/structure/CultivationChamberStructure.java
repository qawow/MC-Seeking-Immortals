package com.xunxian.seekingimmortals.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;

/**
 * 修炼密室结构验证（适用于静室、双修静室等修炼类结构）。
 * 结构：封闭空间 + 蒲团位 + 聚灵阵纹 + 隔音墙壁。
 */
public final class CultivationChamberStructure {
    public static final int CHAMBER_RADIUS = 1;
    public static final int CHAMBER_HEIGHT = 3;

    private static final List<BlockPos> FLOOR_OFFSETS = buildFloorOffsets();
    private static final List<BlockPos> WALL_OFFSETS = buildWallOffsets();
    private static final List<BlockPos> CEILING_OFFSETS = buildCeilingOffsets();
    private static final List<BlockPos> CUSHION_OFFSETS = buildCushionOffsets();

    private CultivationChamberStructure() {}

    public static CheckResult validate(Level level, BlockPos center, Block wallBlock, Block floorBlock, Block cushionBlock) {
        int missingFloor = 0;
        for (BlockPos offset : FLOOR_OFFSETS) {
            if (!level.getBlockState(center.offset(offset)).is(floorBlock)) {
                missingFloor++;
            }
        }

        int missingWalls = 0;
        for (BlockPos offset : WALL_OFFSETS) {
            if (!level.getBlockState(center.offset(offset)).is(wallBlock)) {
                missingWalls++;
            }
        }

        int missingCeiling = 0;
        for (BlockPos offset : CEILING_OFFSETS) {
            if (!level.getBlockState(center.offset(offset)).is(wallBlock)) {
                missingCeiling++;
            }
        }

        int missingCushions = 0;
        for (BlockPos offset : CUSHION_OFFSETS) {
            if (!level.getBlockState(center.offset(offset)).is(cushionBlock)) {
                missingCushions++;
            }
        }

        return new CheckResult(missingFloor, missingWalls, missingCeiling, missingCushions);
    }

    private static List<BlockPos> buildFloorOffsets() {
        List<BlockPos> offsets = new ArrayList<>();
        for (int x = -CHAMBER_RADIUS; x <= CHAMBER_RADIUS; x++) {
            for (int z = -CHAMBER_RADIUS; z <= CHAMBER_RADIUS; z++) {
                offsets.add(new BlockPos(x, 0, z));
            }
        }
        return List.copyOf(offsets);
    }

    private static List<BlockPos> buildWallOffsets() {
        List<BlockPos> offsets = new ArrayList<>();
        for (int y = 1; y < CHAMBER_HEIGHT - 1; y++) {
            for (int x = -CHAMBER_RADIUS; x <= CHAMBER_RADIUS; x++) {
                for (int z = -CHAMBER_RADIUS; z <= CHAMBER_RADIUS; z++) {
                    // 只在边缘放置墙壁，保持内部空间
                    if (Math.abs(x) == CHAMBER_RADIUS || Math.abs(z) == CHAMBER_RADIUS) {
                        offsets.add(new BlockPos(x, y, z));
                    }
                }
            }
        }
        return List.copyOf(offsets);
    }

    private static List<BlockPos> buildCeilingOffsets() {
        List<BlockPos> offsets = new ArrayList<>();
        int ceilingY = CHAMBER_HEIGHT - 1;
        for (int x = -CHAMBER_RADIUS; x <= CHAMBER_RADIUS; x++) {
            for (int z = -CHAMBER_RADIUS; z <= CHAMBER_RADIUS; z++) {
                offsets.add(new BlockPos(x, ceilingY, z));
            }
        }
        return List.copyOf(offsets);
    }

    private static List<BlockPos> buildCushionOffsets() {
        List<BlockPos> offsets = new ArrayList<>();
        // 中心位置放置蒲团
        offsets.add(new BlockPos(0, 1, 0));
        return List.copyOf(offsets);
    }

    public record CheckResult(int missingFloor, int missingWalls, int missingCeiling, int missingCushions) {
        public boolean complete() {
            return missingFloor <= 0 && missingWalls <= 0 && missingCeiling <= 0 && missingCushions <= 0;
        }
    }
}
