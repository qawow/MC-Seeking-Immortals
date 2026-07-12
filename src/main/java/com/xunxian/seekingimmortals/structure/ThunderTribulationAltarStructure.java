package com.xunxian.seekingimmortals.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;

/**
 * Thunder tribulation altar: outer edge ring radius 2 at y=0 (16 blocks, center excluded),
 * four corner pillars at (±2, y, ±2) for y=1..3, and a clear 3x3 aperture for y=1..3.
 */
public final class ThunderTribulationAltarStructure {
    public static final int RING_RADIUS = 2;
    public static final int PILLAR_HEIGHT = 3;

    private static final List<BlockPos> RING_OFFSETS = buildRingOffsets();
    private static final List<BlockPos> PILLAR_OFFSETS = buildPillarOffsets();
    private static final List<BlockPos> APERTURE_OFFSETS = buildApertureOffsets();

    private ThunderTribulationAltarStructure() {}

    public static CheckResult validate(Level level, BlockPos center, Block altarBlock, Block frameBlock) {
        int missingRing = 0;
        for (BlockPos offset : RING_OFFSETS) {
            if (!level.getBlockState(center.offset(offset)).is(altarBlock)) {
                missingRing++;
            }
        }
        int missingPillars = 0;
        for (BlockPos offset : PILLAR_OFFSETS) {
            if (!level.getBlockState(center.offset(offset)).is(frameBlock)) {
                missingPillars++;
            }
        }
        int blockedAperture = 0;
        for (BlockPos offset : APERTURE_OFFSETS) {
            if (!level.getBlockState(center.offset(offset)).isAir()) {
                blockedAperture++;
            }
        }
        return new CheckResult(missingRing, missingPillars, blockedAperture);
    }

    public static List<BlockPos> ringOffsets() {
        return RING_OFFSETS;
    }

    public static List<BlockPos> pillarOffsets() {
        return PILLAR_OFFSETS;
    }

    public static List<BlockPos> apertureOffsets() {
        return APERTURE_OFFSETS;
    }

    private static List<BlockPos> buildRingOffsets() {
        List<BlockPos> offsets = new ArrayList<>();
        for (int x = -RING_RADIUS; x <= RING_RADIUS; x++) {
            for (int z = -RING_RADIUS; z <= RING_RADIUS; z++) {
                if (Math.abs(x) != RING_RADIUS && Math.abs(z) != RING_RADIUS) {
                    continue;
                }
                offsets.add(new BlockPos(x, 0, z));
            }
        }
        return List.copyOf(offsets);
    }

    private static List<BlockPos> buildPillarOffsets() {
        List<BlockPos> offsets = new ArrayList<>();
        int[] corners = { -RING_RADIUS, RING_RADIUS };
        for (int y = 1; y <= PILLAR_HEIGHT; y++) {
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
        for (int y = 1; y <= PILLAR_HEIGHT; y++) {
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    offsets.add(new BlockPos(x, y, z));
                }
            }
        }
        return List.copyOf(offsets);
    }

    public record CheckResult(int missingRing, int missingPillars, int blockedAperture) {
        public boolean complete() {
            return missingRing <= 0 && missingPillars <= 0 && blockedAperture <= 0;
        }
    }
}
