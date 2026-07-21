package com.xunxian.seekingimmortals.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;

/**
 * 高级聚灵阵结构验证 - 5×5 八卦阵型
 * 结构：中心聚灵阵核心 + 八个方位灵石柱 + 外围灵石地基
 * 功能：大幅提升周围灵气浓度（3×3区块范围）
 */
public final class AdvancedSpiritGatheringArrayStructure {
    public static final int ARRAY_RADIUS = 2;
    public static final int PILLAR_HEIGHT = 3;

    private static final List<BlockPos> BASE_OFFSETS = buildBaseOffsets();
    private static final List<BlockPos> PILLAR_OFFSETS = buildPillarOffsets();

    private AdvancedSpiritGatheringArrayStructure() {}

    /**
     * @param center 中心聚灵阵方块位置
     * @param spiritStoneBlock 灵石方块（用于地基和柱子）
     * @param coreBlock 阵核方块（用于柱顶）
     * @return 验证结果
     */
    public static CheckResult validate(Level level, BlockPos center, Block spiritStoneBlock, Block coreBlock) {
        // 检查中心阵核
        if (!level.getBlockState(center).is(coreBlock)) {
            return new CheckResult(1, 0, 0, false);
        }

        // 检查地基（5×5外围，除中心3×3）
        int missingBase = 0;
        for (BlockPos offset : BASE_OFFSETS) {
            if (!level.getBlockState(center.offset(offset)).is(spiritStoneBlock)) {
                missingBase++;
            }
        }

        // 检查八个方位的灵石柱
        int missingPillars = 0;
        int completePillars = 0;
        for (BlockPos baseOffset : PILLAR_OFFSETS) {
            boolean pillarComplete = true;
            for (int y = 1; y <= PILLAR_HEIGHT; y++) {
                BlockPos pillarPos = center.offset(baseOffset).above(y);
                Block expectedBlock = (y == PILLAR_HEIGHT) ? coreBlock : spiritStoneBlock;
                if (!level.getBlockState(pillarPos).is(expectedBlock)) {
                    pillarComplete = false;
                    missingPillars++;
                    break;
                }
            }
            if (pillarComplete) {
                completePillars++;
            }
        }

        boolean fullyActive = missingBase == 0 && completePillars == 8;
        return new CheckResult(missingBase, missingPillars, completePillars, fullyActive);
    }

    private static List<BlockPos> buildBaseOffsets() {
        List<BlockPos> offsets = new ArrayList<>();
        // 5×5 外围，排除中心 3×3
        for (int x = -ARRAY_RADIUS; x <= ARRAY_RADIUS; x++) {
            for (int z = -ARRAY_RADIUS; z <= ARRAY_RADIUS; z++) {
                // 排除中心区域
                if (Math.abs(x) == ARRAY_RADIUS || Math.abs(z) == ARRAY_RADIUS) {
                    offsets.add(new BlockPos(x, -1, z));
                }
            }
        }
        return List.copyOf(offsets);
    }

    private static List<BlockPos> buildPillarOffsets() {
        List<BlockPos> offsets = new ArrayList<>();
        // 八个方位：东南西北 + 四个对角
        offsets.add(new BlockPos(ARRAY_RADIUS, 0, 0));      // 东
        offsets.add(new BlockPos(-ARRAY_RADIUS, 0, 0));     // 西
        offsets.add(new BlockPos(0, 0, ARRAY_RADIUS));      // 南
        offsets.add(new BlockPos(0, 0, -ARRAY_RADIUS));     // 北
        offsets.add(new BlockPos(ARRAY_RADIUS, 0, ARRAY_RADIUS));    // 东南
        offsets.add(new BlockPos(-ARRAY_RADIUS, 0, ARRAY_RADIUS));   // 西南
        offsets.add(new BlockPos(ARRAY_RADIUS, 0, -ARRAY_RADIUS));   // 东北
        offsets.add(new BlockPos(-ARRAY_RADIUS, 0, -ARRAY_RADIUS));  // 西北
        return List.copyOf(offsets);
    }

    public record CheckResult(int missingBase, int missingPillars, int completePillars, boolean fullyActive) {
        public boolean complete() {
            return fullyActive;
        }

        public int missingTotal() {
            return Math.max(0, missingBase) + Math.max(0, missingPillars);
        }

        public double efficiency() {
            if (fullyActive) return 1.0;
            // 部分完成也能提供一定效率
            double baseEfficiency = Math.max(0, 1.0 - (missingBase * 0.05));
            double pillarEfficiency = completePillars / 8.0;
            return baseEfficiency * pillarEfficiency * 0.7; // 最高70%效率（未完全激活）
        }
    }
}
