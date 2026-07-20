package com.xunxian.seekingimmortals.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;

/**
 * 货栈装卸台结构验证（4×3×2 物流类结构）。
 * 结构：4×3 平台 + 传送带 + 储物箱位 + 升降机关。
 */
public final class WarehouseLoadingBayStructure {
    public static final int PLATFORM_WIDTH = 4;
    public static final int PLATFORM_DEPTH = 3;
    public static final int BAY_HEIGHT = 2;

    private static final List<BlockPos> PLATFORM_OFFSETS = buildPlatformOffsets();
    private static final List<BlockPos> CONVEYOR_OFFSETS = buildConveyorOffsets();
    private static final List<BlockPos> STORAGE_OFFSETS = buildStorageOffsets();

    private WarehouseLoadingBayStructure() {}

    public static CheckResult validate(Level level, BlockPos center, Block platformBlock, Block conveyorBlock, Block storageBlock) {
        int missingPlatform = 0;
        for (BlockPos offset : PLATFORM_OFFSETS) {
            if (!level.getBlockState(center.offset(offset)).is(platformBlock)) {
                missingPlatform++;
            }
        }

        int missingConveyor = 0;
        for (BlockPos offset : CONVEYOR_OFFSETS) {
            if (!level.getBlockState(center.offset(offset)).is(conveyorBlock)) {
                missingConveyor++;
            }
        }

        int missingStorage = 0;
        for (BlockPos offset : STORAGE_OFFSETS) {
            if (!level.getBlockState(center.offset(offset)).is(storageBlock)) {
                missingStorage++;
            }
        }

        return new CheckResult(missingPlatform, missingConveyor, missingStorage);
    }

    private static List<BlockPos> buildPlatformOffsets() {
        List<BlockPos> offsets = new ArrayList<>();
        // 4×3 底层平台
        for (int x = 0; x < PLATFORM_WIDTH; x++) {
            for (int z = 0; z < PLATFORM_DEPTH; z++) {
                offsets.add(new BlockPos(x, 0, z));
            }
        }
        return List.copyOf(offsets);
    }

    private static List<BlockPos> buildConveyorOffsets() {
        List<BlockPos> offsets = new ArrayList<>();
        // 中央传送带（4格长）
        for (int x = 0; x < PLATFORM_WIDTH; x++) {
            offsets.add(new BlockPos(x, 1, 1));
        }
        return List.copyOf(offsets);
    }

    private static List<BlockPos> buildStorageOffsets() {
        List<BlockPos> offsets = new ArrayList<>();
        // 两侧储物箱位
        offsets.add(new BlockPos(0, 1, 0));
        offsets.add(new BlockPos(0, 1, 2));
        offsets.add(new BlockPos(3, 1, 0));
        offsets.add(new BlockPos(3, 1, 2));
        return List.copyOf(offsets);
    }

    public record CheckResult(int missingPlatform, int missingConveyor, int missingStorage) {
        public boolean complete() {
            return missingPlatform <= 0 && missingConveyor <= 0 && missingStorage <= 0;
        }
    }
}
