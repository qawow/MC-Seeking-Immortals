package com.xunxian.seekingimmortals.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * Spirit herb planter plot: radius-1 soil ring (dirt/grass/farmland/moss or spirit_gathering_array).
 * Text-material block_items_catalog id: spirit_herb_planter.
 */
public final class SpiritHerbPlanterStructure {
    public static final int RADIUS = 1;

    private static final List<BlockPos> RING_OFFSETS = buildRingOffsets();

    private SpiritHerbPlanterStructure() {}

    public static CheckResult validate(Level level, BlockPos center, Block preferredArrayBlock) {
        int missingSoil = 0;
        for (BlockPos offset : RING_OFFSETS) {
            BlockState state = level.getBlockState(center.offset(offset));
            if (!isSoil(state, preferredArrayBlock)) {
                missingSoil++;
            }
        }
        return new CheckResult(missingSoil);
    }

    public static List<BlockPos> ringOffsets() {
        return RING_OFFSETS;
    }

    public static boolean isSoil(BlockState state, Block preferredArrayBlock) {
        if (state.is(preferredArrayBlock)) {
            return true;
        }
        if (state.is(Blocks.DIRT) || state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.FARMLAND)
                || state.is(Blocks.MOSS_BLOCK) || state.is(Blocks.ROOTED_DIRT) || state.is(Blocks.PODZOL)
                || state.is(Blocks.COARSE_DIRT) || state.is(Blocks.MYCELIUM)) {
            return true;
        }
        return state.is(BlockTags.DIRT);
    }

    private static List<BlockPos> buildRingOffsets() {
        List<BlockPos> offsets = new ArrayList<>();
        for (int x = -RADIUS; x <= RADIUS; x++) {
            for (int z = -RADIUS; z <= RADIUS; z++) {
                if (x == 0 && z == 0) {
                    continue;
                }
                if (Math.abs(x) == RADIUS || Math.abs(z) == RADIUS) {
                    offsets.add(new BlockPos(x, 0, z));
                }
            }
        }
        return List.copyOf(offsets);
    }

    public record CheckResult(int missingSoil) {
        public boolean complete() {
            return missingSoil <= 0;
        }
    }
}
