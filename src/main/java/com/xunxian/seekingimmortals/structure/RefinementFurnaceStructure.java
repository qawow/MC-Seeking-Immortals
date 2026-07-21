package com.xunxian.seekingimmortals.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;

/**
 * 炼器熔炉结构验证 - 3×3×3 立方体熔炉
 * 结构：外层耐火砖 + 内层空气 + 底部熔岩 + 顶部烟囱
 * 功能：炼制高级法宝、武器淬炼、器灵觉醒
 */
public final class RefinementFurnaceStructure {
    public static final int RADIUS = 1;
    public static final int HEIGHT = 3;

    private static final List<BlockPos> WALL_OFFSETS = buildWallOffsets();
    private static final List<BlockPos> INNER_OFFSETS = buildInnerOffsets();

    private RefinementFurnaceStructure() {}

    /**
     * @param center 中心工作位置（玩家站立处）
     * @param brickBlock 耐火砖方块
     * @param lavaBlock 熔岩方块
     * @return 验证结果
     */
    public static CheckResult validate(Level level, BlockPos center, Block brickBlock, Block lavaBlock) {
        // 检查外墙
        int missingWalls = 0;
        for (BlockPos offset : WALL_OFFSETS) {
            if (!level.getBlockState(center.offset(offset)).is(brickBlock)) {
                missingWalls++;
            }
        }

        // 检查内部空间（应为空气）
        int blockedInner = 0;
        for (BlockPos offset : INNER_OFFSETS) {
            if (!level.getBlockState(center.offset(offset)).isAir()) {
                blockedInner++;
            }
        }

        // 检查底部熔岩
        boolean hasLava = level.getBlockState(center.below()).is(lavaBlock);

        // 检查顶部烟囱（向上3格应为空）
        boolean hasSmokestack = true;
        for (int y = HEIGHT; y < HEIGHT + 3; y++) {
            if (!level.getBlockState(center.above(y)).isAir()) {
                hasSmokestack = false;
                break;
            }
        }

        boolean fullyActive = missingWalls == 0 && blockedInner == 0 && hasLava && hasSmokestack;
        return new CheckResult(missingWalls, blockedInner, hasLava, hasSmokestack, fullyActive);
    }

    private static List<BlockPos> buildWallOffsets() {
        List<BlockPos> offsets = new ArrayList<>();
        // 底层（y=-1）
        for (int x = -RADIUS; x <= RADIUS; x++) {
            for (int z = -RADIUS; z <= RADIUS; z++) {
                if (x != 0 || z != 0) { // 排除中心（熔岩位置）
                    offsets.add(new BlockPos(x, -1, z));
                }
            }
        }

        // 中间层和顶层的四周墙壁
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = -RADIUS; x <= RADIUS; x++) {
                for (int z = -RADIUS; z <= RADIUS; z++) {
                    // 只要边缘
                    if (Math.abs(x) == RADIUS || Math.abs(z) == RADIUS) {
                        offsets.add(new BlockPos(x, y, z));
                    }
                }
            }
        }

        // 顶部封闭（除中心烟囱）
        for (int x = -RADIUS; x <= RADIUS; x++) {
            for (int z = -RADIUS; z <= RADIUS; z++) {
                if (Math.abs(x) == RADIUS || Math.abs(z) == RADIUS) {
                    offsets.add(new BlockPos(x, HEIGHT, z));
                }
            }
        }

        return List.copyOf(offsets);
    }

    private static List<BlockPos> buildInnerOffsets() {
        List<BlockPos> offsets = new ArrayList<>();
        // 内部空间（不包括边缘墙壁）
        for (int y = 0; y < HEIGHT; y++) {
            offsets.add(new BlockPos(0, y, 0)); // 中心
        }
        return List.copyOf(offsets);
    }

    public record CheckResult(int missingWalls, int blockedInner, boolean hasLava,
                             boolean hasSmokestack, boolean fullyActive) {
        public boolean complete() {
            return fullyActive;
        }

        public double efficiency() {
            if (fullyActive) return 1.0;

            double wallScore = Math.max(0, 1.0 - (missingWalls * 0.05));
            double innerScore = blockedInner == 0 ? 1.0 : 0.5;
            double lavaScore = hasLava ? 1.0 : 0.0;
            double smokestackScore = hasSmokestack ? 1.0 : 0.8;

            // 熔岩是必需的
            if (!hasLava) return 0.0;

            return wallScore * innerScore * smokestackScore * 0.8;
        }

        public int maxTemperature() {
            if (fullyActive) return 2000; // 完整结构：2000°C
            if (hasLava) return (int) (1200 * efficiency()); // 部分结构：降低温度
            return 0;
        }
    }
}
