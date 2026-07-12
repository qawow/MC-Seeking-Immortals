package com.xunxian.seekingimmortals.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;

/**
 * Hidden rift / inverse-star hideout: small radius-1 ring + short pillars + tiny aperture.
 * Text-material node type: hidden_rift.
 */
public final class HiddenRiftGateStructure {
    public static final int RING_RADIUS = 1;
    public static final int APERTURE_RADIUS = 0;
    public static final int APERTURE_HEIGHT = 2;
    public static final int FRAME_HEIGHT = 2;

    private static final List<BlockPos> RING_OFFSETS = buildRingOffsets();
    private static final List<BlockPos> FRAME_OFFSETS = buildFrameOffsets();
    private static final List<BlockPos> APERTURE_OFFSETS = buildApertureOffsets();

    private HiddenRiftGateStructure() {}

    public static CheckResult validate(Level level, BlockPos center, Block ringBlock, Block frameBlock) {
        int missingRing = 0;
        for (BlockPos offset : RING_OFFSETS) {
            if (!level.getBlockState(center.offset(offset)).is(ringBlock)) missingRing++;
        }
        int missingFrame = 0;
        for (BlockPos offset : FRAME_OFFSETS) {
            if (!level.getBlockState(center.offset(offset)).is(frameBlock)) missingFrame++;
        }
        int blockedAperture = 0;
        for (BlockPos offset : APERTURE_OFFSETS) {
            if (!level.getBlockState(center.offset(offset)).isAir()) blockedAperture++;
        }
        return new CheckResult(missingRing, missingFrame, blockedAperture);
    }

    public record CheckResult(int missingRing, int missingFrame, int blockedAperture) {
        public boolean complete() {
            return missingRing <= 0 && missingFrame <= 0 && blockedAperture <= 0;
        }
    }

    private static List<BlockPos> buildRingOffsets() {
        List<BlockPos> offsets = new ArrayList<>();
        for (int x = -RING_RADIUS; x <= RING_RADIUS; x++) {
            for (int z = -RING_RADIUS; z <= RING_RADIUS; z++) {
                if (x == 0 && z == 0) continue;
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
            offsets.add(new BlockPos(0, y, 0));
        }
        return List.copyOf(offsets);
    }
}
