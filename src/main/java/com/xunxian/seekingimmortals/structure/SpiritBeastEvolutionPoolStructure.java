package com.xunxian.seekingimmortals.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;

/**
 * Spirit beast evolution pool: radius-2 gathering-array ring + four spirit-ore corner posts (y=1)
 * with clear water/air deck above the center.
 */
public final class SpiritBeastEvolutionPoolStructure {
    public static final int RING_RADIUS = 2;

    private static final List<BlockPos> RING_OFFSETS = buildRingOffsets();
    private static final List<BlockPos> POST_OFFSETS = buildPostOffsets();
    private static final List<BlockPos> DECK_AIR = buildDeckAir();

    private SpiritBeastEvolutionPoolStructure() {}

    public static CheckResult validate(Level level, BlockPos center, Block ringBlock, Block postBlock) {
        int missingRing = 0;
        for (BlockPos offset : RING_OFFSETS) {
            if (!level.getBlockState(center.offset(offset)).is(ringBlock)) {
                missingRing++;
            }
        }
        int missingPosts = 0;
        for (BlockPos offset : POST_OFFSETS) {
            if (!level.getBlockState(center.offset(offset)).is(postBlock)) {
                missingPosts++;
            }
        }
        int blockedDeck = 0;
        for (BlockPos offset : DECK_AIR) {
            if (!level.getBlockState(center.offset(offset)).isAir()) {
                blockedDeck++;
            }
        }
        return new CheckResult(missingRing, missingPosts, blockedDeck);
    }

    public static List<BlockPos> ringOffsets() {
        return RING_OFFSETS;
    }

    public static List<BlockPos> postOffsets() {
        return POST_OFFSETS;
    }

    public static List<BlockPos> deckAirOffsets() {
        return DECK_AIR;
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

    private static List<BlockPos> buildPostOffsets() {
        List<BlockPos> offsets = new ArrayList<>();
        int[] corners = {-RING_RADIUS, RING_RADIUS};
        for (int x : corners) {
            for (int z : corners) {
                offsets.add(new BlockPos(x, 1, z));
            }
        }
        return List.copyOf(offsets);
    }

    private static List<BlockPos> buildDeckAir() {
        List<BlockPos> offsets = new ArrayList<>();
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                offsets.add(new BlockPos(x, 1, z));
            }
        }
        return List.copyOf(offsets);
    }

    public record CheckResult(int missingRing, int missingPosts, int blockedDeck) {
        public boolean complete() {
            return missingRing <= 0 && missingPosts <= 0 && blockedDeck <= 0;
        }
    }
}
