package com.xunxian.seekingimmortals.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;

/**
 * 仙品丹鼎结构验证（5×5×5 大型丹炉结构）。
 * 结构：双层基座 + 鼎身 + 鼎盖 + 四角支柱。
 */
public final class ImmortalAlchemyCauldronStructure {
    public static final int BASE_RADIUS = 2;
    public static final int CAULDRON_HEIGHT = 5;

    private static final List<BlockPos> BASE_OFFSETS = buildBaseOffsets();
    private static final List<BlockPos> PILLAR_OFFSETS = buildPillarOffsets();
    private static final List<BlockPos> BODY_OFFSETS = buildBodyOffsets();
    private static final List<BlockPos> LID_OFFSETS = buildLidOffsets();

    private ImmortalAlchemyCauldronStructure() {}

    public static CheckResult validate(Level level, BlockPos center, Block baseBlock, Block bodyBlock, Block pillarBlock) {
        int missingBase = 0;
        for (BlockPos offset : BASE_OFFSETS) {
            if (!level.getBlockState(center.offset(offset)).is(baseBlock)) {
                missingBase++;
            }
        }

        int missingBody = 0;
        for (BlockPos offset : BODY_OFFSETS) {
            if (!level.getBlockState(center.offset(offset)).is(bodyBlock)) {
                missingBody++;
            }
        }

        int missingPillars = 0;
        for (BlockPos offset : PILLAR_OFFSETS) {
            if (!level.getBlockState(center.offset(offset)).is(pillarBlock)) {
                missingPillars++;
            }
        }

        int missingLid = 0;
        for (BlockPos offset : LID_OFFSETS) {
            if (!level.getBlockState(center.offset(offset)).is(bodyBlock)) {
                missingLid++;
            }
        }

        return new CheckResult(missingBase, missingBody, missingPillars, missingLid);
    }

    private static List<BlockPos> buildBaseOffsets() {
        List<BlockPos> offsets = new ArrayList<>();
        // 双层 5×5 基座
        for (int y = 0; y <= 1; y++) {
            for (int x = -BASE_RADIUS; x <= BASE_RADIUS; x++) {
                for (int z = -BASE_RADIUS; z <= BASE_RADIUS; z++) {
                    offsets.add(new BlockPos(x, y, z));
                }
            }
        }
        return List.copyOf(offsets);
    }

    private static List<BlockPos> buildBodyOffsets() {
        List<BlockPos> offsets = new ArrayList<>();
        // 鼎身：中层 3×3 空间围边
        for (int y = 2; y <= 3; y++) {
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    // 边缘方块，中心留空
                    if (x != 0 || z != 0) {
                        offsets.add(new BlockPos(x, y, z));
                    }
                }
            }
        }
        return List.copyOf(offsets);
    }

    private static List<BlockPos> buildPillarOffsets() {
        List<BlockPos> offsets = new ArrayList<>();
        // 四角支柱：从基座到鼎盖
        int[] corners = { -BASE_RADIUS, BASE_RADIUS };
        for (int y = 1; y <= CAULDRON_HEIGHT - 1; y++) {
            for (int x : corners) {
                for (int z : corners) {
                    offsets.add(new BlockPos(x, y, z));
                }
            }
        }
        return List.copyOf(offsets);
    }

    private static List<BlockPos> buildLidOffsets() {
        List<BlockPos> offsets = new ArrayList<>();
        // 鼎盖：顶层 3×3
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                offsets.add(new BlockPos(x, CAULDRON_HEIGHT - 1, z));
            }
        }
        return List.copyOf(offsets);
    }

    public record CheckResult(int missingBase, int missingBody, int missingPillars, int missingLid) {
        public boolean complete() {
            return missingBase <= 0 && missingBody <= 0 && missingPillars <= 0 && missingLid <= 0;
        }
    }
}
