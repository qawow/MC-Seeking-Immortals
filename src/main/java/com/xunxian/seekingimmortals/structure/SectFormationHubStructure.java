package com.xunxian.seekingimmortals.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;

/**
 * 护宗大阵枢纽结构验证（5×5×3 防御类阵法）。
 * 结构：多层基座 + 枢纽核心 + 四周护盾柱 + 顶部能量聚合点。
 */
public final class SectFormationHubStructure {
    public static final int BASE_RADIUS = 2;
    public static final int HUB_HEIGHT = 3;

    private static final List<BlockPos> BASE_OFFSETS = buildBaseOffsets();
    private static final List<BlockPos> SHIELD_PILLAR_OFFSETS = buildShieldPillarOffsets();
    private static final BlockPos CORE = new BlockPos(0, 1, 0);
    private static final List<BlockPos> ENERGY_NODES = buildEnergyNodes();

    private SectFormationHubStructure() {}

    public static CheckResult validate(Level level, BlockPos center, Block baseBlock, Block pillarBlock, Block coreBlock, Block nodeBlock) {
        int missingBase = 0;
        for (BlockPos offset : BASE_OFFSETS) {
            if (!level.getBlockState(center.offset(offset)).is(baseBlock)) {
                missingBase++;
            }
        }

        int missingPillars = 0;
        for (BlockPos offset : SHIELD_PILLAR_OFFSETS) {
            if (!level.getBlockState(center.offset(offset)).is(pillarBlock)) {
                missingPillars++;
            }
        }

        boolean corePresent = level.getBlockState(center.offset(CORE)).is(coreBlock);

        int missingNodes = 0;
        for (BlockPos offset : ENERGY_NODES) {
            if (!level.getBlockState(center.offset(offset)).is(nodeBlock)) {
                missingNodes++;
            }
        }

        return new CheckResult(missingBase, missingPillars, corePresent ? 0 : 1, missingNodes);
    }

    private static List<BlockPos> buildBaseOffsets() {
        List<BlockPos> offsets = new ArrayList<>();
        // 5×5 双层基座
        for (int y = 0; y <= 0; y++) {
            for (int x = -BASE_RADIUS; x <= BASE_RADIUS; x++) {
                for (int z = -BASE_RADIUS; z <= BASE_RADIUS; z++) {
                    offsets.add(new BlockPos(x, y, z));
                }
            }
        }
        return List.copyOf(offsets);
    }

    private static List<BlockPos> buildShieldPillarOffsets() {
        List<BlockPos> offsets = new ArrayList<>();
        // 四角护盾柱
        int[] corners = { -BASE_RADIUS, BASE_RADIUS };
        for (int y = 1; y <= HUB_HEIGHT - 1; y++) {
            for (int x : corners) {
                for (int z : corners) {
                    offsets.add(new BlockPos(x, y, z));
                }
            }
        }
        return List.copyOf(offsets);
    }

    private static List<BlockPos> buildEnergyNodes() {
        List<BlockPos> offsets = new ArrayList<>();
        // 顶部四个方向的能量节点
        offsets.add(new BlockPos(-1, HUB_HEIGHT - 1, 0));
        offsets.add(new BlockPos(1, HUB_HEIGHT - 1, 0));
        offsets.add(new BlockPos(0, HUB_HEIGHT - 1, -1));
        offsets.add(new BlockPos(0, HUB_HEIGHT - 1, 1));
        return List.copyOf(offsets);
    }

    public record CheckResult(int missingBase, int missingPillars, int missingCore, int missingNodes) {
        public boolean complete() {
            return missingBase <= 0 && missingPillars <= 0 && missingCore <= 0 && missingNodes <= 0;
        }
    }
}
