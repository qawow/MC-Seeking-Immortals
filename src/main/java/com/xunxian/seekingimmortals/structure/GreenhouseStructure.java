package com.xunxian.seekingimmortals.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;

/**
 * 温室/暖房结构验证（适用于灵药暖室等培育类结构）。
 * 结构：5×5 基座 + 玻璃墙面 + 温控装置 + 种植区。
 */
public final class GreenhouseStructure {
    public static final int BASE_RADIUS = 2;
    public static final int WALL_HEIGHT = 3;

    private static final List<BlockPos> FLOOR_OFFSETS = buildFloorOffsets();
    private static final List<BlockPos> GLASS_WALL_OFFSETS = buildGlassWallOffsets();
    private static final List<BlockPos> PLANTING_AREA_OFFSETS = buildPlantingAreaOffsets();
    private static final List<BlockPos> ROOF_OFFSETS = buildRoofOffsets();

    private GreenhouseStructure() {}

    public static CheckResult validate(Level level, BlockPos center, Block floorBlock, Block glassBlock, Block soilBlock) {
        int missingFloor = 0;
        for (BlockPos offset : FLOOR_OFFSETS) {
            if (!level.getBlockState(center.offset(offset)).is(floorBlock)) {
                missingFloor++;
            }
        }

        int missingGlassWalls = 0;
        for (BlockPos offset : GLASS_WALL_OFFSETS) {
            BlockPos absolute = center.offset(offset);
            // 玻璃墙可以是玻璃或空气（窗户）
            if (!level.getBlockState(absolute).is(glassBlock) && !level.getBlockState(absolute).isAir()) {
                missingGlassWalls++;
            }
        }

        int missingPlantingArea = 0;
        for (BlockPos offset : PLANTING_AREA_OFFSETS) {
            if (!level.getBlockState(center.offset(offset)).is(soilBlock)) {
                missingPlantingArea++;
            }
        }

        int missingRoof = 0;
        for (BlockPos offset : ROOF_OFFSETS) {
            if (!level.getBlockState(center.offset(offset)).is(glassBlock)) {
                missingRoof++;
            }
        }

        return new CheckResult(missingFloor, missingGlassWalls, missingPlantingArea, missingRoof);
    }

    private static List<BlockPos> buildFloorOffsets() {
        List<BlockPos> offsets = new ArrayList<>();
        for (int x = -BASE_RADIUS; x <= BASE_RADIUS; x++) {
            for (int z = -BASE_RADIUS; z <= BASE_RADIUS; z++) {
                offsets.add(new BlockPos(x, 0, z));
            }
        }
        return List.copyOf(offsets);
    }

    private static List<BlockPos> buildGlassWallOffsets() {
        List<BlockPos> offsets = new ArrayList<>();
        for (int y = 1; y < WALL_HEIGHT; y++) {
            for (int x = -BASE_RADIUS; x <= BASE_RADIUS; x++) {
                for (int z = -BASE_RADIUS; z <= BASE_RADIUS; z++) {
                    // 只在边缘放置玻璃墙
                    if (Math.abs(x) == BASE_RADIUS || Math.abs(z) == BASE_RADIUS) {
                        offsets.add(new BlockPos(x, y, z));
                    }
                }
            }
        }
        return List.copyOf(offsets);
    }

    private static List<BlockPos> buildPlantingAreaOffsets() {
        List<BlockPos> offsets = new ArrayList<>();
        // 内部 3×3 种植区域
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                offsets.add(new BlockPos(x, 1, z));
            }
        }
        return List.copyOf(offsets);
    }

    private static List<BlockPos> buildRoofOffsets() {
        List<BlockPos> offsets = new ArrayList<>();
        int roofY = WALL_HEIGHT;
        for (int x = -BASE_RADIUS; x <= BASE_RADIUS; x++) {
            for (int z = -BASE_RADIUS; z <= BASE_RADIUS; z++) {
                offsets.add(new BlockPos(x, roofY, z));
            }
        }
        return List.copyOf(offsets);
    }

    public record CheckResult(int missingFloor, int missingGlassWalls, int missingPlantingArea, int missingRoof) {
        public boolean complete() {
            return missingFloor <= 0 && missingGlassWalls <= 0 && missingPlantingArea <= 0 && missingRoof <= 0;
        }
    }
}
