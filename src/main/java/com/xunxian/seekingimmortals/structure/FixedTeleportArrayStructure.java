package com.xunxian.seekingimmortals.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;

/**
 * Fixed teleport array: 5x5 pedestal ring (radius 2) around a core pedestal,
 * with a clear 3x3x3 aperture above the center.
 * Text-material node type: fixed_teleport_array.
 */
public final class FixedTeleportArrayStructure {
    public static final int RING_RADIUS = 2;
    public static final int APERTURE_RADIUS = 1;
    public static final int APERTURE_HEIGHT = 3;

    private static final List<BlockPos> RING_OFFSETS = buildRingOffsets();
    private static final List<BlockPos> APERTURE_OFFSETS = buildApertureOffsets();

    private FixedTeleportArrayStructure() {}

    public static CheckResult validate(Level level, BlockPos center, Block pedestalBlock) {
        int missingRing = 0;
        for (BlockPos offset : RING_OFFSETS) {
            if (!level.getBlockState(center.offset(offset)).is(pedestalBlock)) {
                missingRing++;
            }
        }
        int blockedAperture = 0;
        for (BlockPos offset : APERTURE_OFFSETS) {
            if (!level.getBlockState(center.offset(offset)).isAir()) {
                blockedAperture++;
            }
        }
        return new CheckResult(missingRing, blockedAperture);
    }

    public static List<BlockPos> ringOffsets() {
        return RING_OFFSETS;
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
                boolean edge = Math.abs(x) == RING_RADIUS || Math.abs(z) == RING_RADIUS;
                if (edge) {
                    offsets.add(new BlockPos(x, 0, z));
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

    public record CheckResult(int missingRingBlocks, int blockedApertureBlocks) {
        public boolean complete() {
            return missingRingBlocks <= 0 && blockedApertureBlocks <= 0;
        }
    }
}
