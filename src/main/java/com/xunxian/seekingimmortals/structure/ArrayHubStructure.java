package com.xunxian.seekingimmortals.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;

/**
 * 阵法枢纽结构验证（适用于杀阵枢纽、幻阵枢纽等 3×3×1 攻击/控制类阵法）。
 * 结构：3×3 阵盘 + 四角能量节点 + 中心枢纽核心 + 四边导引石。
 */
public final class ArrayHubStructure {
    public static final int BASE_RADIUS = 1;

    private static final List<BlockPos> CORNER_OFFSETS = buildCornerOffsets();
    private static final List<BlockPos> EDGE_OFFSETS = buildEdgeOffsets();
    private static final BlockPos CENTER = BlockPos.ZERO;

    private ArrayHubStructure() {}

    /**
     * 验证阵法枢纽结构
     * @param level 世界
     * @param center 中心位置
     * @param cornerBlock 四角节点方块
     * @param edgeBlock 四边导引方块
     * @param coreBlock 中心核心方块
     */
    public static CheckResult validate(Level level, BlockPos center, Block cornerBlock, Block edgeBlock, Block coreBlock) {
        int missingCorners = 0;
        for (BlockPos offset : CORNER_OFFSETS) {
            if (!level.getBlockState(center.offset(offset)).is(cornerBlock)) {
                missingCorners++;
            }
        }

        int missingEdges = 0;
        for (BlockPos offset : EDGE_OFFSETS) {
            if (!level.getBlockState(center.offset(offset)).is(edgeBlock)) {
                missingEdges++;
            }
        }

        boolean corePresent = level.getBlockState(center.offset(CENTER)).is(coreBlock);

        return new CheckResult(missingCorners, missingEdges, corePresent ? 0 : 1);
    }

    private static List<BlockPos> buildCornerOffsets() {
        List<BlockPos> offsets = new ArrayList<>();
        // 四角能量节点
        int[] corners = { -BASE_RADIUS, BASE_RADIUS };
        for (int x : corners) {
            for (int z : corners) {
                offsets.add(new BlockPos(x, 0, z));
            }
        }
        return List.copyOf(offsets);
    }

    private static List<BlockPos> buildEdgeOffsets() {
        List<BlockPos> offsets = new ArrayList<>();
        // 四边中点导引石
        offsets.add(new BlockPos(-BASE_RADIUS, 0, 0));
        offsets.add(new BlockPos(BASE_RADIUS, 0, 0));
        offsets.add(new BlockPos(0, 0, -BASE_RADIUS));
        offsets.add(new BlockPos(0, 0, BASE_RADIUS));
        return List.copyOf(offsets);
    }

    public record CheckResult(int missingCorners, int missingEdges, int missingCore) {
        public boolean complete() {
            return missingCorners <= 0 && missingEdges <= 0 && missingCore <= 0;
        }
    }
}
