package com.xunxian.seekingimmortals.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;

/**
 * 通用平台结构验证（适用于真言授业台、妙音共鸣台、拍卖会展台等功能平台）。
 * 结构：5×5 基座 + 中心高台 + 四角装饰柱。
 */
public final class PlatformStructure {
    public static final int BASE_RADIUS = 2;
    public static final int PLATFORM_HEIGHT = 1;
    public static final int DECORATION_HEIGHT = 2;

    private static final List<BlockPos> BASE_OFFSETS = buildBaseOffsets();
    private static final List<BlockPos> PLATFORM_OFFSETS = buildPlatformOffsets();
    private static final List<BlockPos> DECORATION_OFFSETS = buildDecorationOffsets();

    private PlatformStructure() {}

    public static CheckResult validate(Level level, BlockPos center, Block baseBlock, Block decorationBlock) {
        int missingBase = 0;
        for (BlockPos offset : BASE_OFFSETS) {
            if (!level.getBlockState(center.offset(offset)).is(baseBlock)) {
                missingBase++;
            }
        }
        int missingPlatform = 0;
        for (BlockPos offset : PLATFORM_OFFSETS) {
            if (!level.getBlockState(center.offset(offset)).is(baseBlock)) {
                missingPlatform++;
            }
        }
        int missingDecoration = 0;
        for (BlockPos offset : DECORATION_OFFSETS) {
            if (!level.getBlockState(center.offset(offset)).is(decorationBlock)) {
                missingDecoration++;
            }
        }
        return new CheckResult(missingBase, missingPlatform, missingDecoration);
    }

    private static List<BlockPos> buildBaseOffsets() {
        List<BlockPos> offsets = new ArrayList<>();
        for (int x = -BASE_RADIUS; x <= BASE_RADIUS; x++) {
            for (int z = -BASE_RADIUS; z <= BASE_RADIUS; z++) {
                offsets.add(new BlockPos(x, 0, z));
            }
        }
        return List.copyOf(offsets);
    }

    private static List<BlockPos> buildPlatformOffsets() {
        List<BlockPos> offsets = new ArrayList<>();
        // 中心 3×3 高台
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                offsets.add(new BlockPos(x, PLATFORM_HEIGHT, z));
            }
        }
        return List.copyOf(offsets);
    }

    private static List<BlockPos> buildDecorationOffsets() {
        List<BlockPos> offsets = new ArrayList<>();
        int[] corners = { -BASE_RADIUS, BASE_RADIUS };
        for (int y = 1; y <= DECORATION_HEIGHT; y++) {
            for (int x : corners) {
                for (int z : corners) {
                    offsets.add(new BlockPos(x, y, z));
                }
            }
        }
        return List.copyOf(offsets);
    }

    public record CheckResult(int missingBase, int missingPlatform, int missingDecoration) {
        public boolean complete() {
            return missingBase <= 0 && missingPlatform <= 0 && missingDecoration <= 0;
        }
    }
}
