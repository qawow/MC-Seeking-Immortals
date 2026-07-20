package com.xunxian.seekingimmortals.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;

/**
 * 试炼圈结构验证（适用于地渊修罗试炼圈等 7×7×1 战斗类结构）。
 * 结构：7×7 战斗圈 + 外圈结界石 + 四角传送点 + 中心祭坛。
 */
public final class TrialRingStructure {
    public static final int BASE_RADIUS = 3;

    private static final List<BlockPos> OUTER_RING_OFFSETS = buildOuterRingOffsets();
    private static final List<BlockPos> CORNER_TELEPORT_OFFSETS = buildCornerTeleportOffsets();
    private static final List<BlockPos> INNER_FLOOR_OFFSETS = buildInnerFloorOffsets();
    private static final BlockPos CENTER = BlockPos.ZERO;

    private TrialRingStructure() {}

    public static CheckResult validate(Level level, BlockPos center, Block ringBlock, Block teleportBlock, Block floorBlock, Block altarBlock) {
        int missingRing = 0;
        for (BlockPos offset : OUTER_RING_OFFSETS) {
            if (!level.getBlockState(center.offset(offset)).is(ringBlock)) {
                missingRing++;
            }
        }

        int missingTeleports = 0;
        for (BlockPos offset : CORNER_TELEPORT_OFFSETS) {
            if (!level.getBlockState(center.offset(offset)).is(teleportBlock)) {
                missingTeleports++;
            }
        }

        int missingFloor = 0;
        for (BlockPos offset : INNER_FLOOR_OFFSETS) {
            if (!level.getBlockState(center.offset(offset)).is(floorBlock)) {
                missingFloor++;
            }
        }

        boolean altarPresent = level.getBlockState(center.offset(CENTER)).is(altarBlock);

        return new CheckResult(missingRing, missingTeleports, missingFloor, altarPresent ? 0 : 1);
    }

    private static List<BlockPos> buildOuterRingOffsets() {
        List<BlockPos> offsets = new ArrayList<>();
        // 外圈结界石（7×7 边缘）
        for (int x = -BASE_RADIUS; x <= BASE_RADIUS; x++) {
            for (int z = -BASE_RADIUS; z <= BASE_RADIUS; z++) {
                if (Math.abs(x) == BASE_RADIUS || Math.abs(z) == BASE_RADIUS) {
                    // 排除四角（留给传送点）
                    if (!(Math.abs(x) == BASE_RADIUS && Math.abs(z) == BASE_RADIUS)) {
                        offsets.add(new BlockPos(x, 0, z));
                    }
                }
            }
        }
        return List.copyOf(offsets);
    }

    private static List<BlockPos> buildCornerTeleportOffsets() {
        List<BlockPos> offsets = new ArrayList<>();
        // 四角传送点
        int[] corners = { -BASE_RADIUS, BASE_RADIUS };
        for (int x : corners) {
            for (int z : corners) {
                offsets.add(new BlockPos(x, 0, z));
            }
        }
        return List.copyOf(offsets);
    }

    private static List<BlockPos> buildInnerFloorOffsets() {
        List<BlockPos> offsets = new ArrayList<>();
        // 内部 5×5 战斗地板（不含中心）
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                if (x != 0 || z != 0) {
                    offsets.add(new BlockPos(x, 0, z));
                }
            }
        }
        return List.copyOf(offsets);
    }

    public record CheckResult(int missingRing, int missingTeleports, int missingFloor, int missingAltar) {
        public boolean complete() {
            return missingRing <= 0 && missingTeleports <= 0 && missingFloor <= 0 && missingAltar <= 0;
        }
    }
}
