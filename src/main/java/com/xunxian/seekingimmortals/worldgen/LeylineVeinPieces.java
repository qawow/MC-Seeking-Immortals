package com.xunxian.seekingimmortals.worldgen;

import com.xunxian.seekingimmortals.registry.ModBlocks;
import com.xunxian.seekingimmortals.registry.ModStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/**
 * Wave493: structure piece that materializes a multi-biome leyline body.
 * Uses existing spirit ore / gathering array / surface marker blocks.
 */
public final class LeylineVeinPieces {
    public static final int SHAPE_MOUNTAIN = 0;
    public static final int SHAPE_FOREST = 1;
    public static final int SHAPE_SHORE = 2;
    public static final int SHAPE_PLAINS = 3;
    public static final int ELEMENT_METAL = 0;
    public static final int ELEMENT_WOOD = 1;
    public static final int ELEMENT_WATER = 2;
    public static final int ELEMENT_FIRE = 3;
    public static final int ELEMENT_EARTH = 4;

    private LeylineVeinPieces() {}

    /**
     * Returns the dominant spirit-stone element for a leyline landscape.
     * The pure integer contract keeps the biome bias deterministic and testable without loading registries.
     */
    public static int preferredElementForShape(int shape, int roll) {
        int value = Math.floorMod(roll, 100);
        return switch (shape) {
            case SHAPE_MOUNTAIN -> value < 45 ? ELEMENT_METAL
                    : value < 80 ? ELEMENT_EARTH
                    : value < 93 ? ELEMENT_FIRE : ELEMENT_WATER;
            case SHAPE_FOREST -> value < 55 ? ELEMENT_WOOD
                    : value < 80 ? ELEMENT_EARTH
                    : value < 93 ? ELEMENT_WATER : ELEMENT_METAL;
            case SHAPE_SHORE -> value < 60 ? ELEMENT_WATER
                    : value < 78 ? ELEMENT_WOOD
                    : value < 92 ? ELEMENT_EARTH : ELEMENT_METAL;
            default -> value < 50 ? ELEMENT_EARTH
                    : value < 75 ? ELEMENT_FIRE
                    : value < 89 ? ELEMENT_WOOD : ELEMENT_METAL;
        };
    }

    public static class Piece extends StructurePiece {
        private final int shape;
        private final int tier;
        private final boolean cluster;

        public Piece(BlockPos origin, int shape, int tier, boolean cluster) {
            super(ModStructures.LEYLINE_VEIN_PIECE.get(), 0, makeBox(origin, shape, cluster));
            this.shape = shape;
            this.tier = Math.max(1, Math.min(3, tier));
            this.cluster = cluster;
            setOrientation(null);
        }

        public Piece(CompoundTag tag) {
            super(ModStructures.LEYLINE_VEIN_PIECE.get(), tag);
            this.shape = tag.getInt("Shape");
            this.tier = Math.max(1, tag.getInt("Tier"));
            this.cluster = tag.getBoolean("Cluster");
        }

        private static BoundingBox makeBox(BlockPos origin, int shape, boolean cluster) {
            int r = switch (shape) {
                case SHAPE_MOUNTAIN -> cluster ? 5 : 3;
                case SHAPE_FOREST -> cluster ? 7 : 5;
                case SHAPE_SHORE -> cluster ? 9 : 7;
                default -> cluster ? 6 : 4;
            };
            int h = switch (shape) {
                case SHAPE_MOUNTAIN -> cluster ? 18 : 12;
                case SHAPE_FOREST -> 8;
                case SHAPE_SHORE -> 6;
                default -> 7;
            };
            return new BoundingBox(
                    origin.getX() - r, origin.getY() - 8, origin.getZ() - r,
                    origin.getX() + r, origin.getY() + h, origin.getZ() + r);
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
            tag.putInt("Shape", shape);
            tag.putInt("Tier", tier);
            tag.putBoolean("Cluster", cluster);
        }

        @Override
        public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator,
                                RandomSource random, BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
            BlockPos center = new BlockPos(
                    (boundingBox.minX() + boundingBox.maxX()) / 2,
                    Math.max(boundingBox.minY() + 8, (boundingBox.minY() + boundingBox.maxY()) / 2 - 2),
                    (boundingBox.minZ() + boundingBox.maxZ()) / 2);
            // Re-anchor center Y to local surface for safer placement.
            int surfaceY = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE_WG,
                    center.getX(), center.getZ());
            center = new BlockPos(center.getX(), surfaceY, center.getZ());

            switch (shape) {
                case SHAPE_MOUNTAIN -> placeMountain(level, random, box, center);
                case SHAPE_FOREST -> placeForest(level, random, box, center);
                case SHAPE_SHORE -> placeShore(level, random, box, center);
                default -> placePlains(level, random, box, center);
            }
        }

        private void placeMountain(WorldGenLevel level, RandomSource random, BoundingBox box, BlockPos center) {
            int height = 8 + tier * 3 + (cluster ? 4 : 0);
            for (int dy = -6; dy <= height; dy++) {
                int radius = dy < 0 ? 2 : Math.max(0, 2 - dy / 5);
                for (int dx = -radius; dx <= radius; dx++) {
                    for (int dz = -radius; dz <= radius; dz++) {
                        if (dx * dx + dz * dz > radius * radius + 1) {
                            continue;
                        }
                        BlockPos pos = center.offset(dx, dy, dz);
                        if (!box.isInside(pos)) {
                            continue;
                        }
                        if (dy < 0 || (dx == 0 && dz == 0)) {
                            place(level, pos, oreState(random, shape), box);
                        } else if (dy == height) {
                            place(level, pos, markerState(), box);
                        }
                    }
                }
            }
            place(level, center.above(height), markerState(), box);
            if (cluster) {
                place(level, center.above(1), arrayState(), box);
            }
        }

        private void placeForest(WorldGenLevel level, RandomSource random, BoundingBox box, BlockPos center) {
            int radius = 3 + tier + (cluster ? 2 : 0);
            // Underground ore disc.
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx * dx + dz * dz > radius * radius) {
                        continue;
                    }
                    for (int dy = -5; dy <= -1; dy++) {
                        BlockPos pos = center.offset(dx, dy, dz);
                        if (box.isInside(pos) && random.nextFloat() < 0.55F) {
                            place(level, pos, oreState(random, shape), box);
                        }
                    }
                }
            }
            // Surface ring of markers.
            for (int i = 0; i < 8 + tier * 2; i++) {
                double ang = (Math.PI * 2.0D * i) / (8.0D + tier * 2.0D);
                int dx = (int) Math.round(Math.cos(ang) * radius);
                int dz = (int) Math.round(Math.sin(ang) * radius);
                BlockPos pos = center.offset(dx, 0, dz);
                if (box.isInside(pos)) {
                    place(level, pos, markerState(), box);
                    if (random.nextBoolean()) {
                        place(level, pos.below(), oreState(random, shape), box);
                    }
                }
            }
            place(level, center, arrayState(), box);
        }

        private void placeShore(WorldGenLevel level, RandomSource random, BoundingBox box, BlockPos center) {
            int length = 5 + tier * 2 + (cluster ? 3 : 0);
            for (int i = -length; i <= length; i++) {
                for (int w = -1; w <= 1; w++) {
                    BlockPos pos = center.offset(i, -1 + (Math.abs(i) % 2 == 0 ? 0 : -1), w);
                    if (!box.isInside(pos)) {
                        continue;
                    }
                    place(level, pos, oreState(random, shape), box);
                    if (Math.abs(i) % 3 == 0 && w == 0) {
                        place(level, pos.above(), markerState(), box);
                    }
                }
            }
            place(level, center, arrayState(), box);
        }

        private void placePlains(WorldGenLevel level, RandomSource random, BoundingBox box, BlockPos center) {
            int radius = 2 + tier + (cluster ? 1 : 0);
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    int dist2 = dx * dx + dz * dz;
                    if (dist2 > radius * radius) {
                        continue;
                    }
                    int rise = dist2 == 0 ? 1 : 0;
                    for (int dy = -4; dy <= rise; dy++) {
                        BlockPos pos = center.offset(dx, dy, dz);
                        if (!box.isInside(pos)) {
                            continue;
                        }
                        if (dy < 0) {
                            if (random.nextFloat() < 0.7F) {
                                place(level, pos, oreState(random, shape), box);
                            }
                        } else if (dist2 <= 1) {
                            place(level, pos, markerState(), box);
                        }
                    }
                }
            }
            place(level, center, arrayState(), box);
            place(level, center.above(), markerState(), box);
        }

        private BlockState oreState(RandomSource random, int shape) {
            // Keep rare legacy materials in generated veins while making the main body element-aware.
            float rareRoll = random.nextFloat();
            if (rareRoll < 0.14F) {
                return ModBlocks.LOW_SPIRIT_IRON_ORE.get().defaultBlockState();
            }
            if (rareRoll < 0.20F) {
                return ModBlocks.YIN_ESSENCE_ORE.get().defaultBlockState();
            }
            int element = preferredElementForShape(shape, random.nextInt(100) + tier * 11);
            return switch (element) {
                case ELEMENT_METAL -> ModBlocks.METAL_SPIRIT_ORE.get().defaultBlockState();
                case ELEMENT_WOOD -> ModBlocks.WOOD_SPIRIT_ORE.get().defaultBlockState();
                case ELEMENT_WATER -> ModBlocks.WATER_SPIRIT_ORE.get().defaultBlockState();
                case ELEMENT_FIRE -> ModBlocks.FIRE_SPIRIT_ORE.get().defaultBlockState();
                case ELEMENT_EARTH -> ModBlocks.EARTH_SPIRIT_ORE.get().defaultBlockState();
                default -> ModBlocks.SPIRIT_ORE.get().defaultBlockState();
            };
        }

        private BlockState markerState() {
            return ModBlocks.LEYLINE_SURFACE_MARKER.get().defaultBlockState();
        }

        private BlockState arrayState() {
            return ModBlocks.SPIRIT_GATHERING_ARRAY.get().defaultBlockState();
        }

        private void place(WorldGenLevel level, BlockPos pos, BlockState state, BoundingBox box) {
            if (!box.isInside(pos)) {
                return;
            }
            BlockState existing = level.getBlockState(pos);
            if (existing.is(Blocks.BEDROCK) || existing.is(Blocks.WATER) || existing.is(Blocks.LAVA)) {
                return;
            }
            // Keep air only for surface markers/array; ore may replace soft ground/stone.
            if (state.is(ModBlocks.LEYLINE_SURFACE_MARKER.get()) || state.is(ModBlocks.SPIRIT_GATHERING_ARRAY.get())) {
                if (!existing.isAir() && !existing.canBeReplaced()) {
                    // place on top if blocked
                    BlockPos up = pos.above();
                    if (box.isInside(up) && (level.getBlockState(up).isAir() || level.getBlockState(up).canBeReplaced())) {
                        placeBlock(level, state, up.getX(), up.getY(), up.getZ(), box);
                    }
                    return;
                }
            }
            placeBlock(level, state, pos.getX(), pos.getY(), pos.getZ(), box);
        }
    }
}
