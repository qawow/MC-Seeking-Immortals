package com.xunxian.seekingimmortals.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;

/**
 * Spirit gathering formation zone: outer-edge ring radius 2 at y=0 (16 blocks, center excluded).
 * Center is the formation core; ring uses spirit_gathering_array placeable nodes.
 * Text-material / formation_catalog node type: spirit_gather / spirit_gathering_array placeable zone.
 */
public final class SpiritGatheringFormationStructure {
    public static final int RING_RADIUS = 2;

    private static final List<BlockPos> RING_OFFSETS = buildRingOffsets();

    private SpiritGatheringFormationStructure() {}

    public static CheckResult validate(Level level, BlockPos center, Block arrayBlock) {
        int missingRing = 0;
        for (BlockPos offset : RING_OFFSETS) {
            if (!level.getBlockState(center.offset(offset)).is(arrayBlock)) {
                missingRing++;
            }
        }
        return new CheckResult(missingRing);
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
                boolean edge = Math.abs(x) == RING_RADIUS || Math.abs(z) == RING_RADIUS;
                if (edge) {
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
