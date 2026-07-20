package com.xunxian.seekingimmortals.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;

/**
 * 防御结构验证（适用于城垣段、箭塔、防御墙等防护类结构）。
 * 结构：连续城墙 + 垛口 + 支撑结构。
 */
public final class DefenseWallStructure {
    public static final int WALL_WIDTH = 5;
    public static final int WALL_HEIGHT = 4;
    public static final int WALL_DEPTH = 2;

    private static final List<BlockPos> FOUNDATION_OFFSETS = buildFoundationOffsets();
    private static final List<BlockPos> WALL_BODY_OFFSETS = buildWallBodyOffsets();
    private static final List<BlockPos> BATTLEMENT_OFFSETS = buildBattlementOffsets();

    private DefenseWallStructure() {}

    public static CheckResult validate(Level level, BlockPos center, Block wallBlock, Block battlementBlock) {
        int missingFoundation = 0;
        for (BlockPos offset : FOUNDATION_OFFSETS) {
            if (!level.getBlockState(center.offset(offset)).is(wallBlock)) {
                missingFoundation++;
            }
        }

        int missingWallBody = 0;
        for (BlockPos offset : WALL_BODY_OFFSETS) {
            if (!level.getBlockState(center.offset(offset)).is(wallBlock)) {
                missingWallBody++;
            }
        }

        int missingBattlements = 0;
        for (BlockPos offset : BATTLEMENT_OFFSETS) {
            if (!level.getBlockState(center.offset(offset)).is(battlementBlock)) {
                missingBattlements++;
            }
        }

        return new CheckResult(missingFoundation, missingWallBody, missingBattlements);
    }

    private static List<BlockPos> buildFoundationOffsets() {
        List<BlockPos> offsets = new ArrayList<>();
        int halfWidth = WALL_WIDTH / 2;
        int halfDepth = WALL_DEPTH / 2;
        for (int x = -halfWidth; x <= halfWidth; x++) {
            for (int z = -halfDepth; z <= halfDepth; z++) {
                offsets.add(new BlockPos(x, 0, z));
            }
        }
        return List.copyOf(offsets);
    }

    private static List<BlockPos> buildWallBodyOffsets() {
        List<BlockPos> offsets = new ArrayList<>();
        int halfWidth = WALL_WIDTH / 2;
        int halfDepth = WALL_DEPTH / 2;
        for (int y = 1; y < WALL_HEIGHT - 1; y++) {
            for (int x = -halfWidth; x <= halfWidth; x++) {
                for (int z = -halfDepth; z <= halfDepth; z++) {
                    offsets.add(new BlockPos(x, y, z));
                }
            }
        }
        return List.copyOf(offsets);
    }

    private static List<BlockPos> buildBattlementOffsets() {
        List<BlockPos> offsets = new ArrayList<>();
        int halfWidth = WALL_WIDTH / 2;
        int halfDepth = WALL_DEPTH / 2;
        int topY = WALL_HEIGHT - 1;

        // 垛口采用间隔式布局
        for (int x = -halfWidth; x <= halfWidth; x += 2) {
            for (int z = -halfDepth; z <= halfDepth; z++) {
                offsets.add(new BlockPos(x, topY, z));
            }
        }
        return List.copyOf(offsets);
    }

    public record CheckResult(int missingFoundation, int missingWallBody, int missingBattlements) {
        public boolean complete() {
            return missingFoundation <= 0 && missingWallBody <= 0 && missingBattlements <= 0;
        }
    }
}
