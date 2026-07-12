package com.xunxian.seekingimmortals.structure;

import com.xunxian.seekingimmortals.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;

import java.util.List;
import java.util.Optional;

public final class SectEarthFireRoomMultiblock {
    public static final int HORIZONTAL_RANGE = 5;
    public static final int VERTICAL_RANGE = 2;

    private static final List<BlockPos> REQUIRED_OFFSETS = List.of(
            BlockPos.ZERO,
            new BlockPos(1, 0, 0),
            new BlockPos(-1, 0, 0),
            new BlockPos(0, 0, 1),
            new BlockPos(0, 0, -1),
            new BlockPos(0, -1, 0)
    );

    private SectEarthFireRoomMultiblock() {}

    public static boolean hasCompleteRoom(ServerLevel level, BlockPos furnacePos) {
        return findCompleteRoomAnchor(level, furnacePos).isPresent();
    }

    public static Optional<BlockPos> findCompleteRoomAnchor(ServerLevel level, BlockPos furnacePos) {
        BlockPos from = furnacePos.offset(-HORIZONTAL_RANGE, -VERTICAL_RANGE, -HORIZONTAL_RANGE);
        BlockPos to = furnacePos.offset(HORIZONTAL_RANGE, VERTICAL_RANGE, HORIZONTAL_RANGE);
        for (BlockPos pos : BlockPos.betweenClosed(from, to)) {
            if (isComplete(level, pos)) {
                return Optional.of(pos.immutable());
            }
        }
        return Optional.empty();
    }

    public static boolean isComplete(LevelReader level, BlockPos anchorPos) {
        return MultiblockPattern.matches(level, anchorPos, requirements());
    }

    public static List<BlockPos> missingOffsets(LevelReader level, BlockPos anchorPos) {
        return MultiblockPattern.missingOffsets(level, anchorPos, requirements());
    }

    public static List<BlockPos> requiredOffsets() {
        return REQUIRED_OFFSETS;
    }

    private static List<MultiblockPattern.BlockRequirement> requirements() {
        return List.of(
                MultiblockPattern.require(0, 0, 0, ModBlocks.SECT_EARTH_FIRE_ROOM),
                MultiblockPattern.require(1, 0, 0, ModBlocks.SPIRIT_GATHERING_ARRAY),
                MultiblockPattern.require(-1, 0, 0, ModBlocks.SPIRIT_GATHERING_ARRAY),
                MultiblockPattern.require(0, 0, 1, ModBlocks.SPIRIT_GATHERING_ARRAY),
                MultiblockPattern.require(0, 0, -1, ModBlocks.SPIRIT_GATHERING_ARRAY),
                MultiblockPattern.require(0, -1, 0, () -> Blocks.MAGMA_BLOCK)
        );
    }
}
