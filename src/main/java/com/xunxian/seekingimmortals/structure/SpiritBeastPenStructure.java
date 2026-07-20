package com.xunxian.seekingimmortals.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;

/**
 * 灵兽笼舍结构验证（3×3×3 可拼联兽舍）。
 * 结构：3×3 地板 + 四周围栏 + 顶部遮棚 + 内部饲料槽。
 */
public final class SpiritBeastPenStructure {
    public static final int BASE_RADIUS = 1;
    public static final int PEN_HEIGHT = 3;

    private static final List<BlockPos> FLOOR_OFFSETS = buildFloorOffsets();
    private static final List<BlockPos> FENCE_OFFSETS = buildFenceOffsets();
    private static final List<BlockPos> ROOF_OFFSETS = buildRoofOffsets();
    private static final BlockPos FEEDER = new BlockPos(0, 1, 0);

    private SpiritBeastPenStructure() {}

    public static CheckResult validate(Level level, BlockPos center, Block floorBlock, Block fenceBlock, Block roofBlock, Block feederBlock) {
        int missingFloor = 0;
        for (BlockPos offset : FLOOR_OFFSETS) {
            if (!level.getBlockState(center.offset(offset)).is(floorBlock)) {
                missingFloor++;
            }
        }

        int missingFences = 0;
        for (BlockPos offset : FENCE_OFFSETS) {
            if (!level.getBlockState(center.offset(offset)).is(fenceBlock)) {
                missingFences++;
            }
        }

        int missingRoof = 0;
        for (BlockPos offset : ROOF_OFFSETS) {
            if (!level.getBlockState(center.offset(offset)).is(roofBlock)) {
                missingRoof++;
            }
        }

        boolean feederPresent = level.getBlockState(center.offset(FEEDER)).is(feederBlock);

        return new CheckResult(missingFloor, missingFences, missingRoof, feederPresent ? 0 : 1);
    }

    private static List<BlockPos> buildFloorOffsets() {
        List<BlockPos> offsets = new ArrayList<>();
        // 3×3 地板
        for (int x = -BASE_RADIUS; x <= BASE_RADIUS; x++) {
            for (int z = -BASE_RADIUS; z <= BASE_RADIUS; z++) {
                offsets.add(new BlockPos(x, 0, z));
            }
        }
        return List.copyOf(offsets);
    }

    private static List<BlockPos> buildFenceOffsets() {
        List<BlockPos> offsets = new ArrayList<>();
        // 四周围栏（外圈，高度1-2）
        for (int y = 1; y <= 2; y++) {
            for (int x = -BASE_RADIUS; x <= BASE_RADIUS; x++) {
                for (int z = -BASE_RADIUS; z <= BASE_RADIUS; z++) {
                    // 只保留边缘方块
                    if (Math.abs(x) == BASE_RADIUS || Math.abs(z) == BASE_RADIUS) {
                        offsets.add(new BlockPos(x, y, z));
                    }
                }
            }
        }
        return List.copyOf(offsets);
    }

    private static List<BlockPos> buildRoofOffsets() {
        List<BlockPos> offsets = new ArrayList<>();
        // 顶部遮棚（不覆盖中心）
        for (int x = -BASE_RADIUS; x <= BASE_RADIUS; x++) {
            for (int z = -BASE_RADIUS; z <= BASE_RADIUS; z++) {
                if (x != 0 || z != 0) { // 中心留空
                    offsets.add(new BlockPos(x, PEN_HEIGHT - 1, z));
                }
            }
        }
        return List.copyOf(offsets);
    }

    public record CheckResult(int missingFloor, int missingFences, int missingRoof, int missingFeeder) {
        public boolean complete() {
            return missingFloor <= 0 && missingFences <= 0 && missingRoof <= 0 && missingFeeder <= 0;
        }
    }
}
