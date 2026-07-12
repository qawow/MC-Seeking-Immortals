package com.xunxian.seekingimmortals.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared outer-edge ring formation layout used by multiple formation_catalog placeables.
 */
public final class RingFormationStructure {
    public static final int DEFAULT_RADIUS = 2;

    private RingFormationStructure() {}

    public static CheckResult validate(Level level, BlockPos center, Block ringBlock, int radius) {
        int missing = 0;
        for (BlockPos offset : ringOffsets(radius)) {
            if (!level.getBlockState(center.offset(offset)).is(ringBlock)) {
                missing++;
            }
        }
        return new CheckResult(missing);
    }

    public static List<BlockPos> ringOffsets(int radius) {
        List<BlockPos> offsets = new ArrayList<>();
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                if (x == 0 && z == 0) {
                    continue;
                }
                if (Math.abs(x) == radius || Math.abs(z) == radius) {
                    offsets.add(new BlockPos(x, 0, z));
                }
            }
        }
        return List.copyOf(offsets);
    }

    public record CheckResult(int missingRing) {
        public boolean complete() {
            return missingRing <= 0;
        }
    }
}
