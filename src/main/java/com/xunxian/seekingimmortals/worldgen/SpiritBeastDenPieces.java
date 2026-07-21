package com.xunxian.seekingimmortals.worldgen;

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
 * 灵兽巢穴结构片段
 */
public final class SpiritBeastDenPieces {
    private SpiritBeastDenPieces() {}

    public static class Piece extends StructurePiece {
        private final int beastTier;

        public Piece(BlockPos origin, int beastTier) {
            super(ModStructures.SPIRIT_BEAST_DEN_PIECE.get(), 0, makeBox(origin, beastTier));
            this.beastTier = beastTier;
            setOrientation(null);
        }

        public Piece(CompoundTag tag) {
            super(ModStructures.SPIRIT_BEAST_DEN_PIECE.get(), tag);
            this.beastTier = tag.getInt("BeastTier");
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
        }

        @Override
        public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator,
                                RandomSource random, BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
            BlockPos center = new BlockPos(
                    (boundingBox.minX() + boundingBox.maxX()) / 2,
                    boundingBox.minY() + 3,
                    (boundingBox.minZ() + boundingBox.maxZ()) / 2);

            // 挖掘巢穴空间
            excavateDen(level, random, box, center);

            // 放置巢穴地板（苔石和泥土）
            placeFloor(level, random, box, center);

            // 放置灵兽蛋/宝箱
            placeEggsAndTreasure(level, random, box, center);

            // 种植灵草
            plantSpiritHerbs(level, random, box, center);

            // 添加装饰性骨骼和石块
            addDecorations(level, random, box, center);
        }

        private void excavateDen(WorldGenLevel level, RandomSource random, BoundingBox box, BlockPos center) {
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

        private void placeEggsAndTreasure(WorldGenLevel level, RandomSource random, BoundingBox box, BlockPos center) {
            // 中心放置灵兽蛋（用龙蛋代替）或宝箱
            BlockState centerBlock = random.nextFloat() < 0.3f ?
                    Blocks.DRAGON_EGG.defaultBlockState() :
                    Blocks.CHEST.defaultBlockState();
            placeIfInside(level, centerBlock, center.offset(0, -2, 0), box);

            // 周围随机放置 1-2 个额外宝箱
            int extraChests = beastTier >= 2 ? random.nextInt(2) + 1 : random.nextInt(2);
            for (int i = 0; i < extraChests; i++) {
                int angle = random.nextInt(360);
                double rad = Math.toRadians(angle);
                int distance = 3 + random.nextInt(3);
                int x = (int) (distance * Math.cos(rad));
                int z = (int) (distance * Math.sin(rad));
                BlockPos chestPos = center.offset(x, -2, z);
                placeIfInside(level, Blocks.CHEST.defaultBlockState(), chestPos, box);
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

                if (level.getBlockState(groundPos).isSolid()) {
                    // 随机放置各种植物
                    BlockState plantState = switch (random.nextInt(4)) {
                        case 0 -> Blocks.FERN.defaultBlockState();
                        case 1 -> Blocks.TALL_GRASS.defaultBlockState();
                        case 2 -> Blocks.GRASS.defaultBlockState();
                        default -> Blocks.FLOWERING_AZALEA.defaultBlockState();
                    };
                    placeIfInside(level, plantState, herbPos, box);
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

                if (level.getBlockState(decorPos).isAir()) {
                    BlockState decorState = random.nextFloat() < 0.6f ?
                            Blocks.COBBLESTONE.defaultBlockState() :
                            Blocks.BONE_BLOCK.defaultBlockState();
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
