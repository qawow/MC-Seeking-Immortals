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

/**
 * Shared multiblock matcher.
 * Code-driven patterns remain primary for existing stations; M07 adds data-driven helpers
 * that build requirements from catalog station templates (ring / single core).
 */
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

    /**
     * M07 data-driven path: build an outer-edge ring pattern from catalog radius + ring block.
     * Used by MultiblockStationService when validator=ring and for tests that assert index→pattern.
     */
    public static List<BlockRequirement> ringRequirements(int radius, Supplier<? extends Block> ringBlock) {
        Objects.requireNonNull(ringBlock);
        int r = Math.max(1, radius);
        List<BlockRequirement> list = new ArrayList<>();
        for (BlockPos offset : RingFormationStructure.ringOffsets(r)) {
            list.add(require(offset.getX(), offset.getY(), offset.getZ(), ringBlock));
        }
        return List.copyOf(list);
    }

    /** Data-driven single core check bound to one exact registered block. */
    public static List<BlockRequirement> singleCoreRequirements(Supplier<? extends Block> coreBlock) {
        Objects.requireNonNull(coreBlock);
        return List.of(require(0, 0, 0, coreBlock));
    }

    /**
     * Build requirements for a catalog structure id when the pattern is ring/single_core.
     * Returns empty when the station needs a specialized code validator.
     */
    public static List<BlockRequirement> fromCatalogStation(
            MultiblockStructureCatalog.StructureEntry entry,
            Supplier<? extends Block> spiritOre,
            Supplier<? extends Block> spiritArray) {
        if (entry == null || entry.pattern() == null) {
            return List.of();
        }
        MultiblockStructureCatalog.StationPattern pattern = entry.pattern();
        return switch (pattern.validator()) {
            case "ring", "spirit_gathering_formation" -> {
                String role = pattern.ringRole() == null ? "" : pattern.ringRole();
                Supplier<? extends Block> ring = role.contains("gather") || role.contains("array")
                        ? spiritArray : spiritOre;
                int radius = pattern.radius() > 0 ? pattern.radius() : Math.max(1, entry.radius());
                yield ringRequirements(radius, ring);
            }
            // This helper has no registry resolver for station ids, so it must not
            // turn single_core into an arbitrary non-air match.
            case "single_core" -> List.of();
            default -> List.of();
        };
    }
}
