package com.xunxian.seekingimmortals.worldgen;

import com.mojang.serialization.Codec;
import com.xunxian.seekingimmortals.registry.ModStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;

import java.util.Optional;

/**
 * 灵兽巢穴 - 自然生成的妖兽栖息地
 * 包含：灵兽蛋、妖兽核心、灵草、特殊材料
 */
public class SpiritBeastDenStructure extends Structure {
    public static final Codec<SpiritBeastDenStructure> CODEC =
            simpleCodec(SpiritBeastDenStructure::new);

    public SpiritBeastDenStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        int x = context.chunkPos().getMiddleBlockX();
        int z = context.chunkPos().getMiddleBlockZ();

        int surfaceY = context.chunkGenerator().getFirstOccupiedHeight(
                x, z, Heightmap.Types.WORLD_SURFACE_WG,
                context.heightAccessor(), context.randomState());

        if (surfaceY <= context.chunkGenerator().getMinY() + 20) {
            return Optional.empty();
        }

        BlockPos origin = new BlockPos(x, surfaceY, z);
        int beastTier = 1 + context.random().nextInt(3); // 1-3阶妖兽

        long pieceSeed = context.random().nextLong();
        return Optional.of(new GenerationStub(origin, builder ->
                generatePieces(builder, origin, beastTier, pieceSeed)));
    }

    private static void generatePieces(StructurePiecesBuilder builder, BlockPos origin, int beastTier, long seed) {
        builder.addPiece(new SpiritBeastDenPieces.Piece(origin, beastTier, seed));
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.SPIRIT_BEAST_DEN.get();
    }
}
