package com.xunxian.seekingimmortals.worldgen;

import com.mojang.serialization.Codec;
import com.xunxian.seekingimmortals.registry.ModStructures;
import com.xunxian.seekingimmortals.spiritual.SpiritualAuraManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;

import java.util.Optional;

/**
 * Wave493: physical leyline vein structure.
 * Only starts on major hash-leyline chunks (same authority as SpiritualAuraManager).
 * Shape resolves by biome family: mountain / forest / shore / plains.
 */
public class LeylineVeinStructure extends Structure {
    public static final Codec<LeylineVeinStructure> CODEC = simpleCodec(LeylineVeinStructure::new);

    public LeylineVeinStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        ChunkPos chunkPos = context.chunkPos();
        long seed = context.seed();
        if (!SpiritualAuraManager.isMajorLeylineChunk(seed, chunkPos.x, chunkPos.z)) {
            return Optional.empty();
        }

        int x = chunkPos.getMiddleBlockX();
        int z = chunkPos.getMiddleBlockZ();
        int y = context.chunkGenerator().getFirstOccupiedHeight(
                x, z, Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState());
        if (y <= context.chunkGenerator().getMinY() + 8) {
            return Optional.empty();
        }
        BlockPos origin = new BlockPos(x, y, z);
        int shape = resolveShape(context, origin);
        int tier = SpiritualAuraManager.majorLeylineTier(seed, chunkPos.x, chunkPos.z);
        boolean cluster = SpiritualAuraManager.countNearbyMajorVeins(seed, chunkPos.x, chunkPos.z, 2) >= 2;

        return Optional.of(new GenerationStub(origin, builder ->
                generatePieces(builder, origin, shape, tier, cluster)));
    }

    private static void generatePieces(StructurePiecesBuilder builder, BlockPos origin, int shape, int tier, boolean cluster) {
        builder.addPiece(new LeylineVeinPieces.Piece(origin, shape, tier, cluster));
    }

    static int resolveShape(GenerationContext context, BlockPos origin) {
        Holder<Biome> biome = context.chunkGenerator()
                .getBiomeSource()
                .getNoiseBiome(origin.getX() >> 2, origin.getY() >> 2, origin.getZ() >> 2, context.randomState().sampler());
        if (biome.is(BiomeTags.IS_MOUNTAIN) || biome.is(BiomeTags.IS_HILL)) {
            return LeylineVeinPieces.SHAPE_MOUNTAIN;
        }
        if (biome.is(BiomeTags.IS_FOREST) || biome.is(BiomeTags.IS_TAIGA) || biome.is(BiomeTags.IS_JUNGLE)) {
            return LeylineVeinPieces.SHAPE_FOREST;
        }
        if (biome.is(BiomeTags.IS_BEACH) || biome.is(BiomeTags.IS_OCEAN) || biome.is(BiomeTags.IS_RIVER)) {
            return LeylineVeinPieces.SHAPE_SHORE;
        }
        return LeylineVeinPieces.SHAPE_PLAINS;
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.LEYLINE_VEIN.get();
    }
}
