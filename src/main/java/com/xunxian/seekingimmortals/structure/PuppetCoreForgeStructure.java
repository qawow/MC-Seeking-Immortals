package com.xunxian.seekingimmortals.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;

/**
 * Puppet core forge: center core block + radius-1 spirit-ore ring (8 blocks).
 * Stronger than bare ring: core must match the forge heart block.
 */
public final class PuppetCoreForgeStructure {
    public static final int RING_RADIUS = 1;

    private static final List<BlockPos> RING_OFFSETS = buildRingOffsets();

    private PuppetCoreForgeStructure() {}

    public static CheckResult validate(Level level, BlockPos center, Block coreBlock, Block ringBlock) {
        int missingCore = level.getBlockState(center).is(coreBlock) ? 0 : 1;
        int missingRing = 0;
        for (BlockPos offset : RING_OFFSETS) {
            if (!level.getBlockState(center.offset(offset)).is(ringBlock)) {
                missingRing++;
            }
        }
        return new CheckResult(missingCore, missingRing);
    }

    public static List<BlockPos> ringOffsets() {
        return RING_OFFSETS;
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

    public record CheckResult(int missingCore, int missingRing) {
        public boolean complete() {
            return missingCore <= 0 && missingRing <= 0;
        }
    }
}
