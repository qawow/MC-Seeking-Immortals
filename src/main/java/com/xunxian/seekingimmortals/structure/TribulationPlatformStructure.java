package com.xunxian.seekingimmortals.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;

/**
 * 渡劫平台结构验证 - 9×9 高空平台
 * 结构：金属地基 + 避雷针阵列 + 防护符石 + 高度要求
 * 功能：降低天劫伤害、提高渡劫成功率
 */
public final class TribulationPlatformStructure {
    public static final int PLATFORM_RADIUS = 4;
    public static final int MIN_HEIGHT = 120; // 最低高度要求
    public static final int LIGHTNING_ROD_HEIGHT = 5;

    private static final List<BlockPos> PLATFORM_OFFSETS = buildPlatformOffsets();
    private static final List<BlockPos> ROD_POSITIONS = buildRodPositions();
    private static final List<BlockPos> PROTECTION_RUNES = buildProtectionRunes();

    private TribulationPlatformStructure() {}

    /**
     * @param center 平台中心祭坛
     * @param metalBlock 金属方块（地基）
     * @param controllerBlock 中心祭坛方块
     * @param lightningRodBlock 避雷针方块
     * @param runeBlock 防护符石
     * @return 验证结果
     */
    public static CheckResult validate(Level level, BlockPos center, Block metalBlock,
                                      Block controllerBlock, Block lightningRodBlock, Block runeBlock) {
        // 检查高度
        boolean heightRequirement = center.getY() >= MIN_HEIGHT;
        boolean controllerPresent = level.getBlockState(center).is(controllerBlock);

        // 检查平台地基
        int missingPlatform = 0;
        for (BlockPos offset : PLATFORM_OFFSETS) {
            if (!level.getBlockState(center.offset(offset)).is(metalBlock)) {
                missingPlatform++;
            }
        }

        // 检查避雷针（四个角落，每个5格高）
        int missingRods = 0;
        int completeRods = 0;
        for (BlockPos baseOffset : ROD_POSITIONS) {
            boolean rodComplete = true;
            for (int y = 1; y <= LIGHTNING_ROD_HEIGHT; y++) {
                if (!level.getBlockState(center.offset(baseOffset).above(y)).is(lightningRodBlock)) {
                    rodComplete = false;
                    missingRods++;
                    break;
                }
            }
            if (rodComplete) {
                completeRods++;
            }
        }

        // 检查防护符石（八个方位）
        int missingRunes = 0;
        for (BlockPos offset : PROTECTION_RUNES) {
            if (!level.getBlockState(center.offset(offset)).is(runeBlock)) {
                missingRunes++;
            }
        }

        // 检查中心空间（应为空气，用于玩家渡劫）
        boolean centerClear = level.getBlockState(center.above()).isAir() &&
                            level.getBlockState(center.above(2)).isAir() &&
                            level.getBlockState(center.above(3)).isAir();

        boolean fullyActive = heightRequirement && controllerPresent && missingPlatform == 0 &&
                            completeRods == 4 && missingRunes == 0 && centerClear;

        return new CheckResult(heightRequirement, controllerPresent, missingPlatform, completeRods,
                             missingRunes, centerClear, fullyActive);
    }

    private static List<BlockPos> buildPlatformOffsets() {
        List<BlockPos> offsets = new ArrayList<>();
        // 9×9 平台
        for (int x = -PLATFORM_RADIUS; x <= PLATFORM_RADIUS; x++) {
            for (int z = -PLATFORM_RADIUS; z <= PLATFORM_RADIUS; z++) {
                if (x == 0 && z == 0) {
                    continue;
                }
                offsets.add(new BlockPos(x, 0, z));
            }
        }
        return List.copyOf(offsets);
    }

    private static List<BlockPos> buildRodPositions() {
        List<BlockPos> offsets = new ArrayList<>();
        // 四个角落
        offsets.add(new BlockPos(PLATFORM_RADIUS, 0, PLATFORM_RADIUS));
        offsets.add(new BlockPos(-PLATFORM_RADIUS, 0, PLATFORM_RADIUS));
        offsets.add(new BlockPos(PLATFORM_RADIUS, 0, -PLATFORM_RADIUS));
        offsets.add(new BlockPos(-PLATFORM_RADIUS, 0, -PLATFORM_RADIUS));
        return List.copyOf(offsets);
    }

    private static List<BlockPos> buildProtectionRunes() {
        List<BlockPos> offsets = new ArrayList<>();
        int radius = PLATFORM_RADIUS - 1;
        // 八个方位，高度+1
        offsets.add(new BlockPos(radius, 1, 0));      // 东
        offsets.add(new BlockPos(-radius, 1, 0));     // 西
        offsets.add(new BlockPos(0, 1, radius));      // 南
        offsets.add(new BlockPos(0, 1, -radius));     // 北
        offsets.add(new BlockPos(radius, 1, radius));      // 东南
        offsets.add(new BlockPos(-radius, 1, radius));     // 西南
        offsets.add(new BlockPos(radius, 1, -radius));     // 东北
        offsets.add(new BlockPos(-radius, 1, -radius));    // 西北
        return List.copyOf(offsets);
    }

    public record CheckResult(boolean heightRequirement, boolean controllerPresent,
                             int missingPlatform, int completeRods,
                             int missingRunes, boolean centerClear, boolean fullyActive) {
        public boolean complete() {
            return fullyActive;
        }

        public double damageReduction() {
            if (fullyActive) return 0.5; // 完整结构：减少50%伤害

            if (!heightRequirement) return 0.0; // 高度不足无效果

            double platformScore = Math.max(0, 1.0 - (missingPlatform * 0.02));
            double rodScore = completeRods / 4.0;
            double runeScore = Math.max(0, 1.0 - (missingRunes * 0.08));
            double clearScore = centerClear ? 1.0 : 0.8;

            return platformScore * rodScore * runeScore * clearScore * 0.4;
        }

        public double successRateBonus() {
            if (fullyActive) return 0.3; // 完整结构：+30%成功率
            return damageReduction() * 0.6; // 部分结构：按比例增加
        }

        public boolean canAttractTribulation() {
            return heightRequirement && completeRods >= 2;
        }
    }
}
