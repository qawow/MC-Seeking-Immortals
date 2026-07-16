package com.xunxian.seekingimmortals.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;

/**
 * M07-style formed check for flying_boat_dock (M13 travel consumption).
 * Pattern: 3x3 platform of dock blocks + two mast pillars + clear deck aperture.
 */
public final class FlyingBoatDockStructure {
    public static final int PLATFORM_RADIUS = 1;
    public static final int MAST_HEIGHT = 3;

    private static final List<BlockPos> PLATFORM_OFFSETS = buildPlatform();
    private static final List<BlockPos> MAST_OFFSETS = buildMasts();
    private static final List<BlockPos> DECK_AIR = buildDeckAir();

    private FlyingBoatDockStructure() {}

    public static CheckResult validate(Level level, BlockPos center, Block dockBlock, Block mastBlock) {
        int missingPlatform = 0;
        for (BlockPos offset : PLATFORM_OFFSETS) {
            if (!level.getBlockState(center.offset(offset)).is(dockBlock)) {
                missingPlatform++;
            }
        }
        int missingMast = 0;
        for (BlockPos offset : MAST_OFFSETS) {
            if (!level.getBlockState(center.offset(offset)).is(mastBlock)) {
                missingMast++;
            }
        }
        int blocked = 0;
        for (BlockPos offset : DECK_AIR) {
            if (!level.getBlockState(center.offset(offset)).isAir()) {
                blocked++;
            }
        }
        return new CheckResult(missingPlatform, missingMast, blocked);
    }

    public static List<BlockPos> platformOffsets() {
        return PLATFORM_OFFSETS;
    }

    public static List<BlockPos> mastOffsets() {
        return MAST_OFFSETS;
    }

    private static List<BlockPos> buildPlatform() {
        List<BlockPos> offsets = new ArrayList<>();
        for (int x = -PLATFORM_RADIUS; x <= PLATFORM_RADIUS; x++) {
            for (int z = -PLATFORM_RADIUS; z <= PLATFORM_RADIUS; z++) {
                offsets.add(new BlockPos(x, 0, z));
            }
        }
        return List.copyOf(offsets);
    }

    private static List<BlockPos> buildMasts() {
        List<BlockPos> offsets = new ArrayList<>();
        int[][] corners = {{-PLATFORM_RADIUS, -PLATFORM_RADIUS}, {PLATFORM_RADIUS, PLATFORM_RADIUS}};
        for (int[] corner : corners) {
            for (int y = 1; y <= MAST_HEIGHT; y++) {
                offsets.add(new BlockPos(corner[0], y, corner[1]));
            }
        }
        return List.copyOf(offsets);
    }

    private static List<BlockPos> buildDeckAir() {
        List<BlockPos> offsets = new ArrayList<>();
        for (int y = 1; y <= 2; y++) {
            for (int x = -PLATFORM_RADIUS; x <= PLATFORM_RADIUS; x++) {
                for (int z = -PLATFORM_RADIUS; z <= PLATFORM_RADIUS; z++) {
                    if (Math.abs(x) == PLATFORM_RADIUS && Math.abs(z) == PLATFORM_RADIUS) {
                        continue; // mast columns
                    }
                    offsets.add(new BlockPos(x, y, z));
                }
            }
        }
        return List.copyOf(offsets);
    }

    public record CheckResult(int missingPlatformBlocks, int missingMastBlocks, int blockedDeckBlocks) {
        public boolean complete() {
            return missingPlatformBlocks <= 0 && missingMastBlocks <= 0 && blockedDeckBlocks <= 0;
        }
    }
}
