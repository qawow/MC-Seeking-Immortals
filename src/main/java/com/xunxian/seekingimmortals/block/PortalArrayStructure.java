package com.xunxian.seekingimmortals.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;

public final class PortalArrayStructure {
    public static final int BASE_RADIUS = 6;
    public static final int APERTURE_RADIUS = 5;
    public static final int APERTURE_HEIGHT = 9;
    public static final int FRAME_HEIGHT = 10;

    private static final List<BlockPos> BASE_OFFSETS = buildBaseOffsets();
    private static final List<BlockPos> FRAME_OFFSETS = buildFrameOffsets();
    private static final List<BlockPos> APERTURE_OFFSETS = buildApertureOffsets();

    private PortalArrayStructure() {}

    public static CheckResult validate(Level level, BlockPos center, Block arrayBlock, Block frameBlock) {
        int missingBaseBlocks = 0;
        for (BlockPos offset : BASE_OFFSETS) {
            if (!level.getBlockState(center.offset(offset)).is(arrayBlock)) {
                missingBaseBlocks++;
            }
        }

        int missingFrameBlocks = 0;
        for (BlockPos offset : FRAME_OFFSETS) {
            if (!level.getBlockState(center.offset(offset)).is(frameBlock)) {
                missingFrameBlocks++;
            }
        }

        int blockedApertureBlocks = 0;
        for (BlockPos offset : APERTURE_OFFSETS) {
            if (!level.getBlockState(center.offset(offset)).isAir()) {
                blockedApertureBlocks++;
            }
        }
        return new CheckResult(missingBaseBlocks, missingFrameBlocks, blockedApertureBlocks);
    }

    static List<BlockPos> baseOffsets() {
        return BASE_OFFSETS;
    }

    public static List<BlockPos> frameOffsets() {
        return FRAME_OFFSETS;
    }

    static List<BlockPos> apertureOffsets() {
        return APERTURE_OFFSETS;
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

    private static List<BlockPos> buildFrameOffsets() {
        List<BlockPos> offsets = new ArrayList<>();
        int[] corners = { -BASE_RADIUS, BASE_RADIUS };
        for (int y = 1; y <= FRAME_HEIGHT; y++) {
            for (int x : corners) {
                for (int z : corners) {
                    offsets.add(new BlockPos(x, y, z));
                }
            }
        }
        return List.copyOf(offsets);
    }

    private static List<BlockPos> buildApertureOffsets() {
        List<BlockPos> offsets = new ArrayList<>();
        for (int y = 1; y <= APERTURE_HEIGHT; y++) {
            for (int x = -APERTURE_RADIUS; x <= APERTURE_RADIUS; x++) {
                for (int z = -APERTURE_RADIUS; z <= APERTURE_RADIUS; z++) {
                    offsets.add(new BlockPos(x, y, z));
                }
            }
        }
        return List.copyOf(offsets);
    }

    public record CheckResult(int missingBaseBlocks, int missingFrameBlocks, int blockedApertureBlocks) {
        public boolean complete() {
            return missingBaseBlocks <= 0 && missingFrameBlocks <= 0 && blockedApertureBlocks <= 0;
        }
    }
}
