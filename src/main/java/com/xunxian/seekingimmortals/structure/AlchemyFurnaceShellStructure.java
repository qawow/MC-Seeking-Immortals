package com.xunxian.seekingimmortals.structure;

import com.xunxian.seekingimmortals.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.List;

/**
 * Multiblock shell around alchemy furnace controller.
 * G1/G2: four cardinal SPIRIT_GATHERING_ARRAY + optional magma under.
 * G3+: full outer ring radius 2 of SPIRIT_GATHERING_ARRAY.
 * Uses MultiblockPattern exact-offset matching.
 */
public final class AlchemyFurnaceShellStructure {
    private AlchemyFurnaceShellStructure() {}

    public static boolean isComplete(LevelReader level, BlockPos furnacePos, int furnaceTier) {
        return MultiblockPattern.matches(level, furnacePos, requirementsFor(furnaceTier));
    }

    public static List<BlockPos> missingOffsets(LevelReader level, BlockPos furnacePos, int furnaceTier) {
        return MultiblockPattern.missingOffsets(level, furnacePos, requirementsFor(furnaceTier));
    }

    public static List<MultiblockPattern.BlockRequirement> requirementsFor(int furnaceTier) {
        List<MultiblockPattern.BlockRequirement> list = new ArrayList<>();
        if (furnaceTier >= 3) {
            int radius = 2;
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    if (x == 0 && z == 0) continue;
                    if (Math.abs(x) == radius || Math.abs(z) == radius) {
                        list.add(MultiblockPattern.require(x, 0, z, ModBlocks.SPIRIT_GATHERING_ARRAY));
                    }
                }
            }
        } else {
            list.add(MultiblockPattern.require(1, 0, 0, ModBlocks.SPIRIT_GATHERING_ARRAY));
            list.add(MultiblockPattern.require(-1, 0, 0, ModBlocks.SPIRIT_GATHERING_ARRAY));
            list.add(MultiblockPattern.require(0, 0, 1, ModBlocks.SPIRIT_GATHERING_ARRAY));
            list.add(MultiblockPattern.require(0, 0, -1, ModBlocks.SPIRIT_GATHERING_ARRAY));
        }
        // Magma under is soft-required only for tier 3+ earth-fire style furnaces.
        if (furnaceTier >= 4) {
            list.add(MultiblockPattern.require(0, -1, 0, () -> Blocks.MAGMA_BLOCK));
        }
        return List.copyOf(list);
    }
}
