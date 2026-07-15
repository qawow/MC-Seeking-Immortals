package com.xunxian.seekingimmortals.structure;

import com.xunxian.seekingimmortals.block.AlchemyLidBlock;
import com.xunxian.seekingimmortals.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;

/**
 * Multiblock around alchemy furnace controller.
 * Wave500 rules:
 * - ALL tiers require a lid block on top (0,1,0)
 * - T1/T2: no array nodes required
 * - T3+: outer ring radius 2 of array nodes (legacy spirit array still accepted)
 * - T4+: magma under controller
 */
public final class AlchemyFurnaceShellStructure {
    public static final BlockPos LID_OFFSET = new BlockPos(0, 1, 0);

    private AlchemyFurnaceShellStructure() {}

    public static boolean isComplete(LevelReader level, BlockPos furnacePos, int furnaceTier) {
        return MultiblockPattern.matches(level, furnacePos, requirementsFor(furnaceTier));
    }

    public static List<BlockPos> missingOffsets(LevelReader level, BlockPos furnacePos, int furnaceTier) {
        return MultiblockPattern.missingOffsets(level, furnacePos, requirementsFor(furnaceTier));
    }

    public static int requiredCount(int furnaceTier) {
        return requiredOffsets(furnaceTier).size();
    }

    public static int presentCount(LevelReader level, BlockPos furnacePos, int furnaceTier) {
        int required = requiredCount(furnaceTier);
        int missing = missingOffsets(level, furnacePos, furnaceTier).size();
        return Math.max(0, required - missing);
    }

    public static OptionalInt lidTierAt(LevelReader level, BlockPos furnacePos) {
        BlockState state = level.getBlockState(furnacePos.offset(LID_OFFSET));
        if (state.getBlock() instanceof AlchemyLidBlock lid) {
            return OptionalInt.of(lid.tier());
        }
        return OptionalInt.empty();
    }

    /** Pure geometry helper for unit tests (no registry bootstrap). */
    public static List<BlockPos> requiredOffsets(int furnaceTier) {
        int tier = Math.max(1, furnaceTier);
        List<BlockPos> offsets = new ArrayList<>();
        // Lid always required on top of controller.
        offsets.add(LID_OFFSET.immutable());
        // High-tier outer ring only.
        if (tier >= 3) {
            int radius = 2;
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
        }
        if (tier >= 4) {
            offsets.add(new BlockPos(0, -1, 0));
        }
        return List.copyOf(offsets);
    }

    public static List<MultiblockPattern.BlockRequirement> requirementsFor(int furnaceTier) {
        List<MultiblockPattern.BlockRequirement> list = new ArrayList<>();
        for (BlockPos offset : requiredOffsets(furnaceTier)) {
            if (offset.equals(LID_OFFSET)) {
                list.add(new MultiblockPattern.BlockRequirement(offset,
                        state -> state.getBlock() instanceof AlchemyLidBlock));
            } else if (offset.getY() < 0) {
                list.add(MultiblockPattern.require(offset.getX(), offset.getY(), offset.getZ(), () -> Blocks.MAGMA_BLOCK));
            } else {
                list.add(MultiblockPattern.requireAny(
                        offset.getX(), offset.getY(), offset.getZ(),
                        ModBlocks.ALCHEMY_FURNACE_ARRAY_NODE,
                        ModBlocks.SPIRIT_GATHERING_ARRAY));
            }
        }
        return List.copyOf(list);
    }
}
