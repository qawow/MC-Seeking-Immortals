package com.xunxian.seekingimmortals.worldgen;

import com.xunxian.seekingimmortals.SeekingImmortalsMod;
import com.xunxian.seekingimmortals.registry.ModBlocks;
import com.xunxian.seekingimmortals.registry.ModStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
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
 * 灵兽巢穴结构片段
 */
public final class SpiritBeastDenPieces {
    private static final ResourceLocation DEN_LOOT = new ResourceLocation(
            SeekingImmortalsMod.MODID, "chests/spirit_beast_den");

    private SpiritBeastDenPieces() {}

    public static class Piece extends StructurePiece {
        private static final long DEN_FLOOR_SALT = 0x44454E464C4F4F52L;
        private static final long DEN_LOOT_SALT = 0x44454E4C4F4F5421L;
        private static final long DEN_HERB_SALT = 0x44454E4845524253L;
        private static final long DEN_DECOR_SALT = 0x44454E4445434F52L;

        private final int beastTier;
        private final long seed;

        public Piece(BlockPos origin, int beastTier) {
            this(origin, beastTier, origin.asLong() ^ (long) beastTier * 31L);
        }

        public Piece(BlockPos origin, int beastTier, long seed) {
            super(ModStructures.SPIRIT_BEAST_DEN_PIECE.get(), 0, makeBox(origin, beastTier));
            this.beastTier = beastTier;
            this.seed = seed;
            setOrientation(null);
        }

        public Piece(CompoundTag tag) {
            super(ModStructures.SPIRIT_BEAST_DEN_PIECE.get(), tag);
            this.beastTier = tag.getInt("BeastTier");
            this.seed = tag.contains("Seed")
                    ? tag.getLong("Seed")
                    : boundingBox.getCenter().asLong() ^ (long) beastTier * 31L;
        }

        private static BoundingBox makeBox(BlockPos origin, int beastTier) {
            int size = 6 + beastTier * 2;
            return new BoundingBox(
                    origin.getX() - size, origin.getY() - 3, origin.getZ() - size,
                    origin.getX() + size, origin.getY() + 5, origin.getZ() + size);
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
            tag.putInt("BeastTier", beastTier);
            tag.putLong("Seed", seed);
        }

        @Override
        public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator,
                                RandomSource random, BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
            BlockPos center = new BlockPos(
                    (boundingBox.minX() + boundingBox.maxX()) / 2,
                    boundingBox.minY() + 3,
                    (boundingBox.minZ() + boundingBox.maxZ()) / 2);

            // 挖掘巢穴空间
            excavateDen(level, box, center);

            // 放置巢穴地板（苔石和泥土）
            placeFloor(level, phaseRandom(DEN_FLOOR_SALT), box, center);

            // 放置带专用战利品表的巢穴宝箱
            placeNestTreasure(level, phaseRandom(DEN_LOOT_SALT), box, center);

            // 种植灵草
            plantSpiritHerbs(level, phaseRandom(DEN_HERB_SALT), box, center);

            // 添加装饰性骨骼和石块
            addDecorations(level, phaseRandom(DEN_DECOR_SALT), box, center);
        }

        private RandomSource phaseRandom(long salt) {
            return RandomSource.create(seed ^ salt);
        }

        private void excavateDen(WorldGenLevel level, BoundingBox box, BlockPos center) {
            int radius = (boundingBox.maxX() - boundingBox.minX()) / 2;

            for (int y = -2; y <= 4; y++) {
                for (int x = -radius; x <= radius; x++) {
                    for (int z = -radius; z <= radius; z++) {
                        double dist = Math.sqrt(x * x + z * z);
                        double heightFactor = 1.0 - Math.abs(y) / 6.0;
                        if (dist <= radius * heightFactor) {
                            BlockPos pos = center.offset(x, y, z);
                            placeIfInside(level, Blocks.AIR.defaultBlockState(), pos, box);
                        }
                    }
                }
            }
        }

        private void placeFloor(WorldGenLevel level, RandomSource random, BoundingBox box, BlockPos center) {
            int radius = (boundingBox.maxX() - boundingBox.minX()) / 2;

            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    double dist = Math.sqrt(x * x + z * z);
                    if (dist <= radius) {
                        BlockPos pos = center.offset(x, -3, z);
                        BlockState floorState = random.nextFloat() < 0.4f ?
                                Blocks.MOSSY_COBBLESTONE.defaultBlockState() :
                                random.nextFloat() < 0.5f ?
                                        Blocks.COARSE_DIRT.defaultBlockState() :
                                        Blocks.DIRT.defaultBlockState();
                        placeIfInside(level, floorState, pos, box);
                    }
                }
            }
        }

        private void placeNestTreasure(WorldGenLevel level, RandomSource random, BoundingBox box, BlockPos center) {
            BlockPos centerChest = center.offset(0, -2, 0);
            long centerLootSeed = random.nextLong();
            createChest(level, box, RandomSource.create(centerLootSeed), centerChest,
                    DEN_LOOT, Blocks.CHEST.defaultBlockState());

            // 灵性阵基标识巢穴核心，不再借用末影龙蛋占位。
            for (BlockPos offset : new BlockPos[] {
                    new BlockPos(1, -2, 0), new BlockPos(-1, -2, 0),
                    new BlockPos(0, -2, 1), new BlockPos(0, -2, -1)
            }) {
                placeIfInside(level, ModBlocks.SPIRIT_GATHERING_ARRAY.get().defaultBlockState(),
                        center.offset(offset), box);
            }

            // 周围随机放置 1-2 个额外宝箱
            int extraChests = beastTier >= 2 ? random.nextInt(2) + 1 : random.nextInt(2);
            for (int i = 0; i < extraChests; i++) {
                int angle = random.nextInt(360);
                double rad = Math.toRadians(angle);
                int distance = 3 + random.nextInt(3);
                int x = (int) (distance * Math.cos(rad));
                int z = (int) (distance * Math.sin(rad));
                BlockPos chestPos = center.offset(x, -2, z);
                long lootSeed = random.nextLong();
                createChest(level, box, RandomSource.create(lootSeed), chestPos,
                        DEN_LOOT, Blocks.CHEST.defaultBlockState());
            }
        }

        private void plantSpiritHerbs(WorldGenLevel level, RandomSource random, BoundingBox box, BlockPos center) {
            int herbCount = 5 + beastTier * 3;
            int radius = (boundingBox.maxX() - boundingBox.minX()) / 2 - 1;

            for (int i = 0; i < herbCount; i++) {
                int x = random.nextInt(radius * 2) - radius;
                int z = random.nextInt(radius * 2) - radius;
                BlockPos herbPos = center.offset(x, -2, z);
                BlockPos groundPos = herbPos.below();
                BlockState plantState = switch (random.nextInt(4)) {
                    case 0 -> Blocks.FERN.defaultBlockState();
                    case 1 -> Blocks.DANDELION.defaultBlockState();
                    case 2 -> Blocks.GRASS.defaultBlockState();
                    default -> Blocks.FLOWERING_AZALEA.defaultBlockState();
                };

                if (box.isInside(herbPos)
                        && level.getBlockState(herbPos).isAir()
                        && level.getBlockState(groundPos).isSolid()) {
                    if (plantState.canSurvive(level, herbPos)) {
                        placeIfInside(level, plantState, herbPos, box);
                    }
                }
            }
        }

        private void addDecorations(WorldGenLevel level, RandomSource random, BoundingBox box, BlockPos center) {
            int decorCount = 3 + beastTier * 2;
            int radius = (boundingBox.maxX() - boundingBox.minX()) / 2 - 1;

            for (int i = 0; i < decorCount; i++) {
                int x = random.nextInt(radius * 2) - radius;
                int z = random.nextInt(radius * 2) - radius;
                int y = random.nextInt(4) - 2;
                BlockPos decorPos = center.offset(x, y, z);
                BlockState decorState = random.nextFloat() < 0.6f
                        ? Blocks.COBBLESTONE.defaultBlockState()
                        : Blocks.BONE_BLOCK.defaultBlockState();

                if (box.isInside(decorPos) && level.getBlockState(decorPos).isAir()) {
                    placeIfInside(level, decorState, decorPos, box);
                }
            }
        }

        private void placeIfInside(WorldGenLevel level, BlockState state, BlockPos pos, BoundingBox box) {
            if (box.isInside(pos)) {
                level.setBlock(pos, state, 2);
            }
        }
    }
}
