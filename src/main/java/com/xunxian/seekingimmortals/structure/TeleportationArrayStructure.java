package com.xunxian.seekingimmortals.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;

/**
 * 传送法阵结构验证 - 7×7 复杂阵法
 * 结构：外圈符文石 + 中圈灵石 + 内圈传送核心 + 四个方位标记
 * 功能：长距离定点传送、跨维度传送
 */
public final class TeleportationArrayStructure {
    public static final int OUTER_RADIUS = 3;
    public static final int MIDDLE_RADIUS = 2;
    public static final int INNER_RADIUS = 1;

    private static final List<BlockPos> OUTER_CIRCLE = buildOuterCircle();
    private static final List<BlockPos> MIDDLE_CIRCLE = buildMiddleCircle();
    private static final List<BlockPos> INNER_CIRCLE = buildInnerCircle();
    private static final List<BlockPos> CARDINAL_MARKERS = buildCardinalMarkers();

    private TeleportationArrayStructure() {}

    /**
     * @param center 中心传送点
     * @param runeBlock 符文石方块（外圈）
     * @param spiritStoneBlock 灵石方块（中圈）
     * @param coreBlock 传送核心方块（内圈）
     * @param markerBlock 方位标记方块（四个方位柱）
     * @return 验证结果
     */
    public static CheckResult validate(Level level, BlockPos center, Block runeBlock,
                                      Block spiritStoneBlock, Block coreBlock, Block markerBlock) {
        // 检查外圈符文石
        int missingOuter = 0;
        for (BlockPos offset : OUTER_CIRCLE) {
            if (!level.getBlockState(center.offset(offset)).is(runeBlock)) {
                missingOuter++;
            }
        }

        // 检查中圈灵石
        int missingMiddle = 0;
        for (BlockPos offset : MIDDLE_CIRCLE) {
            if (!level.getBlockState(center.offset(offset)).is(spiritStoneBlock)) {
                missingMiddle++;
            }
        }

        // 检查内圈传送核心
        int missingInner = 0;
        for (BlockPos offset : INNER_CIRCLE) {
            if (!level.getBlockState(center.offset(offset)).is(coreBlock)) {
                missingInner++;
            }
        }

        // 检查中心核心
        if (!level.getBlockState(center).is(coreBlock)) {
            missingInner++;
        }

        // 检查四个方位标记柱（3格高）
        int missingMarkers = 0;
        int completeMarkers = 0;
        for (BlockPos baseOffset : CARDINAL_MARKERS) {
            boolean markerComplete = true;
            for (int y = 1; y <= 3; y++) {
                if (!level.getBlockState(center.offset(baseOffset).above(y)).is(markerBlock)) {
                    markerComplete = false;
                    missingMarkers++;
                    break;
                }
            }
            if (markerComplete) {
                completeMarkers++;
            }
        }

        boolean fullyActive = missingOuter == 0 && missingMiddle == 0 &&
                            missingInner == 0 && completeMarkers == 4;
        return new CheckResult(missingOuter, missingMiddle, missingInner,
                             completeMarkers, fullyActive);
    }

    private static List<BlockPos> buildOuterCircle() {
        List<BlockPos> offsets = new ArrayList<>();
        // 7×7 最外圈
        for (int x = -OUTER_RADIUS; x <= OUTER_RADIUS; x++) {
            for (int z = -OUTER_RADIUS; z <= OUTER_RADIUS; z++) {
                double dist = Math.sqrt(x * x + z * z);
                // 距离在 2.5 到 3.5 之间形成圆环
                if (dist >= 2.5 && dist <= 3.5) {
                    offsets.add(new BlockPos(x, 0, z));
                }
            }
        }
        return List.copyOf(offsets);
    }

    private static List<BlockPos> buildMiddleCircle() {
        List<BlockPos> offsets = new ArrayList<>();
        // 中圈
        for (int x = -MIDDLE_RADIUS; x <= MIDDLE_RADIUS; x++) {
            for (int z = -MIDDLE_RADIUS; z <= MIDDLE_RADIUS; z++) {
                double dist = Math.sqrt(x * x + z * z);
                // 距离在 1.5 到 2.4 之间
                if (dist >= 1.5 && dist <= 2.4) {
                    offsets.add(new BlockPos(x, 0, z));
                }
            }
        }
        return List.copyOf(offsets);
    }

    private static List<BlockPos> buildInnerCircle() {
        List<BlockPos> offsets = new ArrayList<>();
        // 内圈（不包括中心）
        for (int x = -INNER_RADIUS; x <= INNER_RADIUS; x++) {
            for (int z = -INNER_RADIUS; z <= INNER_RADIUS; z++) {
                if (x != 0 || z != 0) { // 排除中心
                    offsets.add(new BlockPos(x, 0, z));
                }
            }
        }
        return List.copyOf(offsets);
    }

    private static List<BlockPos> buildCardinalMarkers() {
        List<BlockPos> offsets = new ArrayList<>();
        // 四个方位
        offsets.add(new BlockPos(OUTER_RADIUS + 1, 0, 0));      // 东
        offsets.add(new BlockPos(-OUTER_RADIUS - 1, 0, 0));     // 西
        offsets.add(new BlockPos(0, 0, OUTER_RADIUS + 1));      // 南
        offsets.add(new BlockPos(0, 0, -OUTER_RADIUS - 1));     // 北
        return List.copyOf(offsets);
    }

    public record CheckResult(int missingOuter, int missingMiddle, int missingInner,
                             int completeMarkers, boolean fullyActive) {
        public boolean complete() {
            return fullyActive;
        }

        public double efficiency() {
            if (fullyActive) return 1.0;

            double outerScore = Math.max(0, 1.0 - (missingOuter * 0.04));
            double middleScore = Math.max(0, 1.0 - (missingMiddle * 0.06));
            double innerScore = Math.max(0, 1.0 - (missingInner * 0.1));
            double markerScore = completeMarkers / 4.0;

            return outerScore * middleScore * innerScore * markerScore * 0.75;
        }

        public int maxDistance() {
            if (fullyActive) return 10000; // 完整结构：10000格
            return (int) (5000 * efficiency()); // 部分结构：降低距离
        }

        public boolean canCrossDimension() {
            return fullyActive && completeMarkers == 4;
        }
    }
}
