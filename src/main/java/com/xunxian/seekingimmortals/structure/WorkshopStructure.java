package com.xunxian.seekingimmortals.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;

/**
 * 工坊结构验证（适用于傀儡工坊、修缮帐等工作类结构）。
 * 结构：5×5 基座 + 工作区域 + 四角支柱 + 工具放置点。
 */
public final class WorkshopStructure {
    public static final int BASE_RADIUS = 2;
    public static final int PILLAR_HEIGHT = 2;

    private static final List<BlockPos> BASE_OFFSETS = buildBaseOffsets();
    private static final List<BlockPos> WORKBENCH_OFFSETS = buildWorkbenchOffsets();
    private static final List<BlockPos> PILLAR_OFFSETS = buildPillarOffsets();

    private WorkshopStructure() {}

    public static CheckResult validate(Level level, BlockPos center, Block baseBlock, Block pillarBlock, Block workbenchBlock) {
        int missingBase = 0;
        for (BlockPos offset : BASE_OFFSETS) {
            if (!level.getBlockState(center.offset(offset)).is(baseBlock)) {
                missingBase++;
            }
        }

        int missingWorkbenches = 0;
        for (BlockPos offset : WORKBENCH_OFFSETS) {
            if (!level.getBlockState(center.offset(offset)).is(workbenchBlock)) {
                missingWorkbenches++;
            }
        }

        int missingPillars = 0;
        for (BlockPos offset : PILLAR_OFFSETS) {
            if (!level.getBlockState(center.offset(offset)).is(pillarBlock)) {
                missingPillars++;
            }
        }

        return new CheckResult(missingBase, missingWorkbenches, missingPillars);
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

    private static List<BlockPos> buildWorkbenchOffsets() {
        List<BlockPos> offsets = new ArrayList<>();
        // 中心 3×3 工作区域
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                if (x != 0 || z != 0) { // 中心留空用于工人站位
                    offsets.add(new BlockPos(x, 1, z));
                }
            }
        }
        return List.copyOf(offsets);
    }

    private static List<BlockPos> buildPillarOffsets() {
        List<BlockPos> offsets = new ArrayList<>();
        int[] corners = { -BASE_RADIUS, BASE_RADIUS };
        for (int y = 1; y <= PILLAR_HEIGHT; y++) {
            for (int x : corners) {
                for (int z : corners) {
                    offsets.add(new BlockPos(x, y, z));
                }
            }
        }
        return List.copyOf(offsets);
    }

    public record CheckResult(int missingBase, int missingWorkbenches, int missingPillars) {
        public boolean complete() {
            return missingBase <= 0 && missingWorkbenches <= 0 && missingPillars <= 0;
        }
    }
}
