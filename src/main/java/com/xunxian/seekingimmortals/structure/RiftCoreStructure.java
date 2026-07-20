package com.xunxian.seekingimmortals.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;

/**
 * 空间裂隙/传送核心结构验证（适用于坠魔裂隙口、洞府核心等）。
 * 结构：能量环 + 核心稳定器 + 空间锚点。
 */
public final class RiftCoreStructure {
    private RiftCoreStructure() {}

    /**
     * 验证 5×5×3 大型裂隙（坠魔裂隙口）
     */
    public static CheckResult validateLargeRift(Level level, BlockPos center, Block baseBlock, Block riftBlock, Block anchorBlock) {
        List<BlockPos> baseOffsets = build5x5Base();
        List<BlockPos> riftRing = buildRiftRing();
        List<BlockPos> anchors = buildAnchors();

        int missingBase = 0;
        for (BlockPos offset : baseOffsets) {
            if (!level.getBlockState(center.offset(offset)).is(baseBlock)) {
                missingBase++;
            }
        }

        int missingRift = 0;
        for (BlockPos offset : riftRing) {
            if (!level.getBlockState(center.offset(offset)).is(riftBlock)) {
                missingRift++;
            }
        }

        int missingAnchors = 0;
        for (BlockPos offset : anchors) {
            if (!level.getBlockState(center.offset(offset)).is(anchorBlock)) {
                missingAnchors++;
            }
        }

        return new CheckResult(missingBase, missingRift, missingAnchors);
    }

    /**
     * 验证 1×1×1 洞府核心（单方块核心 + 周围能量环）
     */
    public static CheckResult validateSmallCore(Level level, BlockPos center, Block coreBlock, Block energyBlock) {
        boolean corePresent = level.getBlockState(center).is(coreBlock);

        // 周围8个方向的能量节点
        List<BlockPos> energyOffsets = buildEnergyRing();
        int missingEnergy = 0;
        for (BlockPos offset : energyOffsets) {
            if (!level.getBlockState(center.offset(offset)).is(energyBlock)) {
                missingEnergy++;
            }
        }

        return new CheckResult(corePresent ? 0 : 1, missingEnergy, 0);
    }

    /**
     * 验证 3×3×3 幻世种子
     */
    public static CheckResult validateWorldSeed(Level level, BlockPos center, Block shellBlock, Block seedBlock, Block energyBlock) {
        List<BlockPos> shell = build3x3Shell();

        int missingShell = 0;
        for (BlockPos offset : shell) {
            if (!level.getBlockState(center.offset(offset)).is(shellBlock)) {
                missingShell++;
            }
        }

        // 中心种子
        boolean seedPresent = level.getBlockState(center.offset(0, 1, 0)).is(seedBlock);

        // 六个方向的能量节点
        int missingEnergy = 0;
        for (BlockPos offset : List.of(
                new BlockPos(0, 1, -1), new BlockPos(0, 1, 1),
                new BlockPos(-1, 1, 0), new BlockPos(1, 1, 0),
                new BlockPos(0, 0, 0), new BlockPos(0, 2, 0))) {
            if (!level.getBlockState(center.offset(offset)).is(energyBlock)) {
                missingEnergy++;
            }
        }

        return new CheckResult(seedPresent ? 0 : 1, missingShell, missingEnergy);
    }

    private static List<BlockPos> build5x5Base() {
        List<BlockPos> offsets = new ArrayList<>();
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                offsets.add(new BlockPos(x, 0, z));
            }
        }
        return List.copyOf(offsets);
    }

    private static List<BlockPos> buildRiftRing() {
        List<BlockPos> offsets = new ArrayList<>();
        // 中层 3×3 环形裂隙
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                if (x != 0 || z != 0) { // 排除中心
                    offsets.add(new BlockPos(x, 1, z));
                }
            }
        }
        return List.copyOf(offsets);
    }

    private static List<BlockPos> buildAnchors() {
        List<BlockPos> offsets = new ArrayList<>();
        // 四角空间锚点
        int[] corners = { -2, 2 };
        for (int x : corners) {
            for (int z : corners) {
                offsets.add(new BlockPos(x, 2, z));
            }
        }
        return List.copyOf(offsets);
    }

    private static List<BlockPos> buildEnergyRing() {
        List<BlockPos> offsets = new ArrayList<>();
        // 水平8方向
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                if (x != 0 || z != 0) {
                    offsets.add(new BlockPos(x, 0, z));
                }
            }
        }
        return List.copyOf(offsets);
    }

    private static List<BlockPos> build3x3Shell() {
        List<BlockPos> offsets = new ArrayList<>();
        // 3×3×3 外壳（不含中心）
        for (int x = -1; x <= 1; x++) {
            for (int y = 0; y <= 2; y++) {
                for (int z = -1; z <= 1; z++) {
                    if (!(x == 0 && y == 1 && z == 0)) { // 排除中心种子位置
                        offsets.add(new BlockPos(x, y, z));
                    }
                }
            }
        }
        return List.copyOf(offsets);
    }

    public record CheckResult(int missingPrimary, int missingSecondary, int missingTertiary) {
        public boolean complete() {
            return missingPrimary <= 0 && missingSecondary <= 0 && missingTertiary <= 0;
        }
    }
}
