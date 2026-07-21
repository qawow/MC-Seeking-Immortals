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
    public static final int SIZE = 3;
    public static final int RADIUS = 1;
    public static final int HEIGHT = 3;
    public static final int SMOKESTACK_CLEARANCE = 3;

    private static final List<BlockPos> WALL_OFFSETS = buildWallOffsets();
    private static final List<BlockPos> INNER_OFFSETS = buildInnerOffsets();
    private static final List<BlockPos> SMOKESTACK_OFFSETS = buildSmokestackOffsets();

    private RefinementFurnaceStructure() {}

    /**
     * @param center 炼器台控制器位置
     * @param controllerBlock 炼器台控制器
     * @param brickBlock 耐火砖方块
     * @param lavaBlock 熔岩方块
     * @return 验证结果
     */
    public static CheckResult validate(Level level, BlockPos center, Block controllerBlock,
                                       Block brickBlock, Block lavaBlock) {
        boolean controllerPresent = level.getBlockState(center).is(controllerBlock);

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

        // 三层实体结构上方的烟道需要额外净空。
        boolean hasSmokestack = true;
        for (BlockPos offset : SMOKESTACK_OFFSETS) {
            if (!level.getBlockState(center.offset(offset)).isAir()) {
                hasSmokestack = false;
                break;
            }
        }

        boolean fullyActive = controllerPresent && missingWalls == 0 && blockedInner == 0
                && hasLava && hasSmokestack;
        return new CheckResult(controllerPresent ? 0 : 1, missingWalls, blockedInner,
                hasLava, hasSmokestack, fullyActive);
    }

    private static List<BlockPos> buildWallOffsets() {
        List<BlockPos> offsets = new ArrayList<>();
        // y=-1..1 构成真实的 3x3x3 炉体；三层中心依次为熔岩、控制器和烟口。
        for (int y = -1; y <= 1; y++) {
            for (int x = -RADIUS; x <= RADIUS; x++) {
                for (int z = -RADIUS; z <= RADIUS; z++) {
                    if (Math.abs(x) == RADIUS || Math.abs(z) == RADIUS) {
                        offsets.add(new BlockPos(x, y, z));
                    }
                }
            }
        }
        return List.copyOf(offsets);
    }

    private static List<BlockPos> buildInnerOffsets() {
        return List.of(new BlockPos(0, 1, 0));
    }

    private static List<BlockPos> buildSmokestackOffsets() {
        List<BlockPos> offsets = new ArrayList<>();
        for (int y = 2; y < 2 + SMOKESTACK_CLEARANCE; y++) {
            offsets.add(new BlockPos(0, y, 0));
        }
        return List.copyOf(offsets);
    }

    static List<BlockPos> wallOffsets() {
        return WALL_OFFSETS;
    }

    static List<BlockPos> innerOffsets() {
        return INNER_OFFSETS;
    }

    static List<BlockPos> smokestackOffsets() {
        return SMOKESTACK_OFFSETS;
    }

    public record CheckResult(int missingController, int missingWalls, int blockedInner, boolean hasLava,
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
