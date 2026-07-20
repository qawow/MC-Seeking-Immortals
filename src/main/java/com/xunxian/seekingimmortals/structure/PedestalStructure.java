package com.xunxian.seekingimmortals.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;

/**
 * 承座/供台结构验证（适用于古鼎/宝鼎承座、血色禁地试炼坛等 3×3×2 供奉类结构）。
 * 结构：3×3 基座 + 中心供台 + 四周能量柱。
 */
public final class PedestalStructure {
    public static final int BASE_RADIUS = 1;
    public static final int PEDESTAL_HEIGHT = 2;

    private static final List<BlockPos> BASE_OFFSETS = buildBaseOffsets();
    private static final List<BlockPos> PILLAR_OFFSETS = buildPillarOffsets();
    private static final BlockPos ALTAR = new BlockPos(0, 1, 0);

    private PedestalStructure() {}

    /**
     * 验证宝鼎承座结构
     */
    public static CheckResult validate(Level level, BlockPos center, Block baseBlock, Block pillarBlock, Block altarBlock) {
        int missingBase = 0;
        for (BlockPos offset : BASE_OFFSETS) {
            if (!level.getBlockState(center.offset(offset)).is(baseBlock)) {
                missingBase++;
            }
        }

        int missingPillars = 0;
        for (BlockPos offset : PILLAR_OFFSETS) {
            if (!level.getBlockState(center.offset(offset)).is(pillarBlock)) {
                missingPillars++;
            }
        }

        boolean altarPresent = level.getBlockState(center.offset(ALTAR)).is(altarBlock);

        return new CheckResult(missingBase, missingPillars, altarPresent ? 0 : 1);
    }

    private static List<BlockPos> buildBaseOffsets() {
        List<BlockPos> offsets = new ArrayList<>();
        // 3×3 基座
        for (int x = -BASE_RADIUS; x <= BASE_RADIUS; x++) {
            for (int z = -BASE_RADIUS; z <= BASE_RADIUS; z++) {
                offsets.add(new BlockPos(x, 0, z));
            }
        }
        return List.copyOf(offsets);
    }

    private static List<BlockPos> buildPillarOffsets() {
        List<BlockPos> offsets = new ArrayList<>();
        // 四个方向的能量柱
        offsets.add(new BlockPos(-BASE_RADIUS, 1, 0));
        offsets.add(new BlockPos(BASE_RADIUS, 1, 0));
        offsets.add(new BlockPos(0, 1, -BASE_RADIUS));
        offsets.add(new BlockPos(0, 1, BASE_RADIUS));
        return List.copyOf(offsets);
    }

    public record CheckResult(int missingBase, int missingPillars, int missingAltar) {
        public boolean complete() {
            return missingBase <= 0 && missingPillars <= 0 && missingAltar <= 0;
        }
    }
}
