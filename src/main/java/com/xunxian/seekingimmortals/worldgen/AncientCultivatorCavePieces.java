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
 * 古修士洞府遗迹结构片段
 */
public final class AncientCultivatorCavePieces {
    private static final ResourceLocation CAVE_LOOT = new ResourceLocation(
            SeekingImmortalsMod.MODID, "chests/ancient_cultivator_cave");

    private AncientCultivatorCavePieces() {}

    public static class Piece extends StructurePiece {
        private static final long CAVE_SHELL_SALT = 0x434156455348454CL;
        private static final long CAVE_LOOT_SALT = 0x434156454C4F4F54L;
        private static final long CAVE_ORE_SALT = 0x434156454F524521L;

        private final int variant;
        private final long seed;

        public Piece(BlockPos origin, int variant, long seed) {
            super(ModStructures.ANCIENT_CULTIVATOR_CAVE_PIECE.get(), 0, makeBox(origin, variant));
            this.variant = variant;
            this.seed = seed;
            setOrientation(null);
        }

        public Piece(CompoundTag tag) {
            super(ModStructures.ANCIENT_CULTIVATOR_CAVE_PIECE.get(), tag);
            this.variant = tag.getInt("Variant");
            this.seed = tag.getLong("Seed");
        }

        private static BoundingBox makeBox(BlockPos origin, int variant) {
            int size = switch (variant) {
                case 0 -> 8;  // 小型 8x6x8
                case 1 -> 12; // 中型 12x8x12
                default -> 16; // 大型 16x10x16
            };
            int height = 6 + variant * 2;
            return new BoundingBox(
                    origin.getX() - size / 2, origin.getY(), origin.getZ() - size / 2,
                    origin.getX() + size / 2, origin.getY() + height, origin.getZ() + size / 2);
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
            tag.putInt("Variant", variant);
            tag.putLong("Seed", seed);
        }

        @Override
        public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator,
                                RandomSource random, BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
            BlockPos center = new BlockPos(
                    (boundingBox.minX() + boundingBox.maxX()) / 2,
                    boundingBox.minY(),
                    (boundingBox.minZ() + boundingBox.maxZ()) / 2);

            // 生成洞穴空间
            generateCaveSpace(level, phaseRandom(CAVE_SHELL_SALT), box, center);

            // 放置破损的聚灵阵（中心）
            if (variant >= 1) {
                BlockPos arrayPos = center.offset(0, 1, 0);
                placeIfInside(level, ModBlocks.SPIRIT_GATHERING_ARRAY.get().defaultBlockState(), arrayPos, box);
            }

            // 放置宝箱（含丹药和材料）
            placeChests(level, phaseRandom(CAVE_LOOT_SALT), box, center);

            // 大型洞府保留破损丹炉阵节点，避免生成可直接工作的原版熔炉。
            if (variant >= 2) {
                BlockPos furnacePos = center.offset(4, 1, 4);
                placeIfInside(level, ModBlocks.ALCHEMY_FURNACE_ARRAY_NODE.get().defaultBlockState(), furnacePos, box);
                placeIfInside(level, Blocks.CRACKED_STONE_BRICKS.defaultBlockState(), furnacePos.below(), box);
            }

            // 放置灵石矿脉
            placeSpiritOreVeins(level, phaseRandom(CAVE_ORE_SALT), box, center);
        }

        private RandomSource phaseRandom(long salt) {
            return RandomSource.create(seed ^ salt);
        }

        private void generateCaveSpace(WorldGenLevel level, RandomSource random, BoundingBox box, BlockPos center) {
            int radius = (boundingBox.maxX() - boundingBox.minX()) / 2;
            int height = boundingBox.maxY() - boundingBox.minY();

            for (int y = 0; y <= height; y++) {
                for (int x = -radius; x <= radius; x++) {
                    for (int z = -radius; z <= radius; z++) {
                        double dist = Math.sqrt(x * x + z * z);
                        if (dist <= radius) {
                            BlockPos pos = center.offset(x, y, z);
                            if (y == 0) {
                                // 地板 - 石砖
                                placeIfInside(level, Blocks.STONE_BRICKS.defaultBlockState(), pos, box);
                            } else if (y == height) {
                                // 天花板 - 石头
                                placeIfInside(level, Blocks.STONE.defaultBlockState(), pos, box);
                            } else {
                                // 清空内部空间
                                placeIfInside(level, Blocks.AIR.defaultBlockState(), pos, box);
                            }
                        }
                    }
                }
            }

            // 墙壁 - 碎裂的石砖
            for (int y = 1; y < height; y++) {
                for (int angle = 0; angle < 360; angle += 15) {
                    double rad = Math.toRadians(angle);
                    int x = (int) (radius * Math.cos(rad));
                    int z = (int) (radius * Math.sin(rad));
                    BlockPos wallPos = center.offset(x, y, z);
                    BlockState wallState = random.nextFloat() < 0.3f ?
                            Blocks.CRACKED_STONE_BRICKS.defaultBlockState() :
                            Blocks.STONE_BRICKS.defaultBlockState();
                    placeIfInside(level, wallState, wallPos, box);
                }
            }
        }

        private void placeChests(WorldGenLevel level, RandomSource random, BoundingBox box, BlockPos center) {
            int chestCount = variant + 1; // 小型1个，中型2个，大型3个
            for (int i = 0; i < chestCount; i++) {
                int angle = i * (360 / chestCount);
                double rad = Math.toRadians(angle);
                int radius = (boundingBox.maxX() - boundingBox.minX()) / 2 - 2;
                int x = (int) (radius * Math.cos(rad));
                int z = (int) (radius * Math.sin(rad));
                BlockPos chestPos = center.offset(x, 1, z);
                long lootSeed = random.nextLong();
                createChest(level, box, RandomSource.create(lootSeed), chestPos,
                        CAVE_LOOT, Blocks.CHEST.defaultBlockState());
            }
        }

        private void placeSpiritOreVeins(WorldGenLevel level, RandomSource random, BoundingBox box, BlockPos center) {
            int veinCount = 3 + variant * 2;
            for (int i = 0; i < veinCount; i++) {
                int x = random.nextInt(boundingBox.maxX() - boundingBox.minX()) + boundingBox.minX() - center.getX();
                int y = random.nextInt(boundingBox.maxY() - boundingBox.minY()) + boundingBox.minY() - center.getY();
                int z = random.nextInt(boundingBox.maxZ() - boundingBox.minZ()) + boundingBox.minZ() - center.getZ();
                BlockPos orePos = center.offset(x, y, z);
                if (box.isInside(orePos)
                        && (level.getBlockState(orePos).is(Blocks.STONE)
                        || level.getBlockState(orePos).is(Blocks.STONE_BRICKS))) {
                    placeIfInside(level, ModBlocks.SPIRIT_ORE.get().defaultBlockState(), orePos, box);
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
