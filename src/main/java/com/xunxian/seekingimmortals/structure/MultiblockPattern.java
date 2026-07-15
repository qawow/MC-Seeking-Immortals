package com.xunxian.seekingimmortals.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.Supplier;

public final class MultiblockPattern {
    private MultiblockPattern() {}

    public record BlockRequirement(BlockPos offset, Predicate<BlockState> matcher) {
        public BlockRequirement {
            Objects.requireNonNull(offset);
            Objects.requireNonNull(matcher);
        }

        public boolean matches(LevelReader level, BlockPos origin) {
            return matcher.test(level.getBlockState(origin.offset(offset)));
        }
    }

    public static BlockRequirement require(int x, int y, int z, Supplier<? extends Block> block) {
        Objects.requireNonNull(block);
        return new BlockRequirement(new BlockPos(x, y, z), state -> state.is(block.get()));
    }

    /** Accept any of the supplied blocks at the offset (Wave499 shell node OR legacy array). */
    @SafeVarargs
    public static BlockRequirement requireAny(int x, int y, int z, Supplier<? extends Block>... blocks) {
        Objects.requireNonNull(blocks);
        if (blocks.length == 0) {
            throw new IllegalArgumentException("requireAny needs at least one block supplier");
        }
        return new BlockRequirement(new BlockPos(x, y, z), state -> {
            for (Supplier<? extends Block> supplier : blocks) {
                if (supplier != null && state.is(supplier.get())) {
                    return true;
                }
            }
            return false;
        });
    }

    public static boolean matches(LevelReader level, BlockPos origin, Collection<BlockRequirement> requirements) {
        for (BlockRequirement requirement : requirements) {
            if (!requirement.matches(level, origin)) {
                return false;
            }
        }
        return true;
    }

    public static List<BlockPos> missingOffsets(LevelReader level, BlockPos origin, Collection<BlockRequirement> requirements) {
        List<BlockPos> missing = new ArrayList<>();
        for (BlockRequirement requirement : requirements) {
            if (!requirement.matches(level, origin)) {
                missing.add(requirement.offset());
            }
        }
        return List.copyOf(missing);
    }

    public static List<BlockPos> offsets(Collection<BlockRequirement> requirements) {
        return requirements.stream()
                .map(BlockRequirement::offset)
                .toList();
    }
}
