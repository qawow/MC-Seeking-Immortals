package com.xunxian.seekingimmortals.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;

/**
 * 灯塔/哨塔结构验证（适用于乱星海灯塔阵、千竹机关塔等高耸类结构）。
 * 结构：3×3 基座 + 垂直塔身 + 顶部信标。
 */
public final class TowerStructure {
    public static final int BASE_RADIUS = 1;
    public static final int TOWER_HEIGHT = 6;

    private static final List<BlockPos> BASE_OFFSETS = buildBaseOffsets();
    private static final List<BlockPos> TOWER_BODY_OFFSETS = buildTowerBodyOffsets();
    private static final List<BlockPos> BEACON_OFFSETS = buildBeaconOffsets();

    private TowerStructure() {}

    public static CheckResult validate(Level level, BlockPos center, Block towerBlock, Block beaconBlock) {
        int missingBase = 0;
        for (BlockPos offset : BASE_OFFSETS) {
            if (!level.getBlockState(center.offset(offset)).is(towerBlock)) {
                missingBase++;
            }
        }

        int missingTowerBody = 0;
        for (BlockPos offset : TOWER_BODY_OFFSETS) {
            if (!level.getBlockState(center.offset(offset)).is(towerBlock)) {
                missingTowerBody++;
            }
        }

        int missingBeacon = 0;
        for (BlockPos offset : BEACON_OFFSETS) {
            if (!level.getBlockState(center.offset(offset)).is(beaconBlock)) {
                missingBeacon++;
            }
        }

        return new CheckResult(missingBase, missingTowerBody, missingBeacon);
    }

    private static List<BlockPos> buildBaseOffsets() {
        List<BlockPos> offsets = new ArrayList<>();
        for (int x = -BASE_RADIUS; x <= BASE_RADIUS; x++) {
            for (int z = -BASE_RADIUS; z <= BASE_RADIUS; z++) {
                offsets.add(new BlockPos(x, 0, z));
            }
        }
        return List.copyOf(offsets);
    }

    private static List<BlockPos> buildTowerBodyOffsets() {
        List<BlockPos> offsets = new ArrayList<>();
        // 塔身采用中心柱 + 四角支撑结构
        for (int y = 1; y < TOWER_HEIGHT - 1; y++) {
            // 中心柱
            offsets.add(new BlockPos(0, y, 0));
            // 四角支撑（每隔一层）
            if (y % 2 == 0) {
                offsets.add(new BlockPos(-1, y, -1));
                offsets.add(new BlockPos(-1, y, 1));
                offsets.add(new BlockPos(1, y, -1));
                offsets.add(new BlockPos(1, y, 1));
            }
        }
        return List.copyOf(offsets);
    }

    private static List<BlockPos> buildBeaconOffsets() {
        List<BlockPos> offsets = new ArrayList<>();
        int topY = TOWER_HEIGHT - 1;
        // 顶部 3×3 信标平台
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                offsets.add(new BlockPos(x, topY, z));
            }
        }
        return List.copyOf(offsets);
    }

    public record CheckResult(int missingBase, int missingTowerBody, int missingBeacon) {
        public boolean complete() {
            return missingBase <= 0 && missingTowerBody <= 0 && missingBeacon <= 0;
        }
    }
}
