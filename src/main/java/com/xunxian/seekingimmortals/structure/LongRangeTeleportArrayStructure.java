package com.xunxian.seekingimmortals.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;

/**
 * Legendary long-range teleport array: 9x9 outer ring (radius 4) of pedestals,
 * four corner spirit-ore pillars y=1..5, and a clear 7x7x5 aperture.
 * Text-material formation id: teleport_array_long_range.
 */
public final class LongRangeTeleportArrayStructure {
    public static final int RING_RADIUS = 4;
    public static final int APERTURE_RADIUS = 3;
    public static final int APERTURE_HEIGHT = 5;
    public static final int FRAME_HEIGHT = 5;

    private static final List<BlockPos> RING_OFFSETS = buildRingOffsets();
    private static final List<BlockPos> FRAME_OFFSETS = buildFrameOffsets();
    private static final List<BlockPos> APERTURE_OFFSETS = buildApertureOffsets();

    private LongRangeTeleportArrayStructure() {}

    public static CheckResult validate(Level level, BlockPos center, Block ringBlock, Block frameBlock) {
        int missingRing = 0;
        for (BlockPos offset : RING_OFFSETS) {
            if (!level.getBlockState(center.offset(offset)).is(ringBlock)) {
                missingRing++;
            }
        }
        int missingFrame = 0;
        for (BlockPos offset : FRAME_OFFSETS) {
            if (!level.getBlockState(center.offset(offset)).is(frameBlock)) {
                missingFrame++;
            }
        }
        int blockedAperture = 0;
        for (BlockPos offset : APERTURE_OFFSETS) {
            if (!level.getBlockState(center.offset(offset)).isAir()) {
                blockedAperture++;
            }
        }
        return new CheckResult(missingRing, missingFrame, blockedAperture);
    }

    public static List<BlockPos> ringOffsets() {
        return RING_OFFSETS;
    }

    public static List<BlockPos> frameOffsets() {
        return FRAME_OFFSETS;
    }

    public static List<BlockPos> apertureOffsets() {
        return APERTURE_OFFSETS;
    }

    private static List<BlockPos> buildRingOffsets() {
        List<BlockPos> offsets = new ArrayList<>();
        for (int x = -RING_RADIUS; x <= RING_RADIUS; x++) {
            for (int z = -RING_RADIUS; z <= RING_RADIUS; z++) {
                if (x == 0 && z == 0) {
                    continue;
                }
                if (Math.abs(x) == RING_RADIUS || Math.abs(z) == RING_RADIUS) {
                    offsets.add(new BlockPos(x, 0, z));
                }
            }
        }
        return List.copyOf(offsets);
    }

    private static List<BlockPos> buildFrameOffsets() {
        List<BlockPos> offsets = new ArrayList<>();
        int[] corners = {-RING_RADIUS, RING_RADIUS};
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

    public record CheckResult(int missingRing, int missingFrame, int blockedAperture) {
        public boolean complete() {
            return missingRing <= 0 && missingFrame <= 0 && blockedAperture <= 0;
        }
    }
}
