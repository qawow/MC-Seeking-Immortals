package com.xunxian.seekingimmortals.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;

/**
 * Blood sacrifice altar: 3x3 base floor (radius 1) excluding center,
 * four corner pillars at (±1, y, ±1) for y=1..2, and a clear vertical aperture at (0,1..2,0).
 */
public final class BloodSacrificeAltarStructure {
    public static final int RING_RADIUS = 1;
    public static final int PILLAR_HEIGHT = 2;

    private static final List<BlockPos> BASE_OFFSETS = buildBaseOffsets();
    private static final List<BlockPos> PILLAR_OFFSETS = buildPillarOffsets();
    private static final List<BlockPos> APERTURE_OFFSETS = buildApertureOffsets();

    private BloodSacrificeAltarStructure() {}

    public static CheckResult validate(Level level, BlockPos center, Block altarBlock, Block frameBlock) {
        int missingBase = 0;
        for (BlockPos offset : BASE_OFFSETS) {
            if (!level.getBlockState(center.offset(offset)).is(altarBlock)) {
                missingBase++;
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
        return new CheckResult(missingBase, missingPillars, blockedAperture);
    }

    public static List<BlockPos> baseOffsets() {
        return BASE_OFFSETS;
    }

    public static List<BlockPos> pillarOffsets() {
        return PILLAR_OFFSETS;
    }

    public static List<BlockPos> apertureOffsets() {
        return APERTURE_OFFSETS;
    }

    private static List<BlockPos> buildBaseOffsets() {
        List<BlockPos> offsets = new ArrayList<>();
        for (int x = -RING_RADIUS; x <= RING_RADIUS; x++) {
            for (int z = -RING_RADIUS; z <= RING_RADIUS; z++) {
                if (x == 0 && z == 0) {
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
            offsets.add(new BlockPos(0, y, 0));
        }
        return List.copyOf(offsets);
    }

    public record CheckResult(int missingBase, int missingPillars, int blockedAperture) {
        public boolean complete() {
            return missingBase <= 0 && missingPillars <= 0 && blockedAperture <= 0;
        }
    }
}
