package com.xunxian.seekingimmortals.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

public final class MultiblockPattern {
    private MultiblockPattern() {}

    public record BlockRequirement(BlockPos offset, Supplier<? extends Block> block) {}

    public static BlockRequirement require(int x, int y, int z, Supplier<? extends Block> block) {
        return new BlockRequirement(new BlockPos(x, y, z), Objects.requireNonNull(block));
    }

    public static boolean matches(LevelReader level, BlockPos origin, Collection<BlockRequirement> requirements) {
        for (BlockRequirement requirement : requirements) {
            if (!level.getBlockState(origin.offset(requirement.offset())).is(requirement.block().get())) {
                return false;
            }
        }
        return true;
    }

    public static List<BlockPos> missingOffsets(LevelReader level, BlockPos origin, Collection<BlockRequirement> requirements) {
        return requirements.stream()
                .filter(requirement -> !level.getBlockState(origin.offset(requirement.offset())).is(requirement.block().get()))
                .map(BlockRequirement::offset)
                .toList();
    }

    public static List<BlockPos> offsets(Collection<BlockRequirement> requirements) {
        return requirements.stream()
                .map(BlockRequirement::offset)
                .toList();
    }
}
