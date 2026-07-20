package com.xunxian.seekingimmortals.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;

/**
 * 大殿结构验证（适用于虚天殿主殿厅堂、昆吾封印大殿等大型建筑结构）。
 * 结构：9×9 基座 + 外围立柱 + 中央大厅。
 */
public final class GrandHallStructure {
    public static final int HALL_RADIUS = 4;
    public static final int HALL_HEIGHT = 5;

    private static final List<BlockPos> FOUNDATION_OFFSETS = buildFoundationOffsets();
    private static final List<BlockPos> PILLAR_OFFSETS = buildPillarOffsets();
    private static final List<BlockPos> CEILING_OFFSETS = buildCeilingOffsets();

    private GrandHallStructure() {}

    public static CheckResult validate(Level level, BlockPos center, Block foundationBlock, Block pillarBlock) {
        int missingFoundation = 0;
        for (BlockPos offset : FOUNDATION_OFFSETS) {
            if (!level.getBlockState(center.offset(offset)).is(foundationBlock)) {
                missingFoundation++;
            }
        }

        int missingPillars = 0;
        for (BlockPos offset : PILLAR_OFFSETS) {
            if (!level.getBlockState(center.offset(offset)).is(pillarBlock)) {
                missingPillars++;
            }
        }

        int missingCeiling = 0;
        for (BlockPos offset : CEILING_OFFSETS) {
            if (!level.getBlockState(center.offset(offset)).is(foundationBlock)) {
                missingCeiling++;
            }
        }

        return new CheckResult(missingFoundation, missingPillars, missingCeiling);
    }

    private static List<BlockPos> buildFoundationOffsets() {
        List<BlockPos> offsets = new ArrayList<>();
        for (int x = -HALL_RADIUS; x <= HALL_RADIUS; x++) {
            for (int z = -HALL_RADIUS; z <= HALL_RADIUS; z++) {
                offsets.add(new BlockPos(x, 0, z));
            }
        }
        return List.copyOf(offsets);
    }

    private static List<BlockPos> buildPillarOffsets() {
        List<BlockPos> offsets = new ArrayList<>();
        // 外围立柱（四个角落 + 中点）
        int[] positions = { -HALL_RADIUS, 0, HALL_RADIUS };
        for (int y = 1; y < HALL_HEIGHT - 1; y++) {
            for (int x : positions) {
                for (int z : positions) {
                    // 跳过中心点
                    if (x == 0 && z == 0) {
                        continue;
                    }
                    // 只在边缘放置立柱
                    if (Math.abs(x) == HALL_RADIUS || Math.abs(z) == HALL_RADIUS) {
                        offsets.add(new BlockPos(x, y, z));
                    }
                }
            }
        }
        return List.copyOf(offsets);
    }

    private static List<BlockPos> buildCeilingOffsets() {
        List<BlockPos> offsets = new ArrayList<>();
        int ceilingY = HALL_HEIGHT - 1;
        for (int x = -HALL_RADIUS; x <= HALL_RADIUS; x++) {
            for (int z = -HALL_RADIUS; z <= HALL_RADIUS; z++) {
                offsets.add(new BlockPos(x, ceilingY, z));
            }
        }
        return List.copyOf(offsets);
    }

    public record CheckResult(int missingFoundation, int missingPillars, int missingCeiling) {
        public boolean complete() {
            return missingFoundation <= 0 && missingPillars <= 0 && missingCeiling <= 0;
        }
    }
}
