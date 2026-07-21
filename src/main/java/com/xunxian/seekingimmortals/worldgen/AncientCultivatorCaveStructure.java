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
 * 古修士洞府遗迹 - 自然生成的废弃修炼场所
 * 包含：破损的炼丹炉、残破的聚灵阵、古老的功法残页、丹药遗留
 */
public class AncientCultivatorCaveStructure extends Structure {
    public static final Codec<AncientCultivatorCaveStructure> CODEC =
            simpleCodec(AncientCultivatorCaveStructure::new);

    public AncientCultivatorCaveStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        int x = context.chunkPos().getMiddleBlockX();
        int z = context.chunkPos().getMiddleBlockZ();

        // 在地表下 20-40 格生成
        int surfaceY = context.chunkGenerator().getFirstOccupiedHeight(
                x, z, Heightmap.Types.WORLD_SURFACE_WG,
                context.heightAccessor(), context.randomState());

        int y = surfaceY - 20 - context.random().nextInt(20);
        if (y <= context.chunkGenerator().getMinY() + 15) {
            return Optional.empty();
        }

        BlockPos origin = new BlockPos(x, y, z);
        int variant = context.random().nextInt(3); // 0=小型, 1=中型, 2=大型

        return Optional.of(new GenerationStub(origin, builder ->
                generatePieces(builder, origin, variant, context.random().nextLong())));
    }

    private static void generatePieces(StructurePiecesBuilder builder, BlockPos origin,
                                      int variant, long seed) {
        builder.addPiece(new AncientCultivatorCavePieces.Piece(origin, variant, seed));
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.ANCIENT_CULTIVATOR_CAVE.get();
    }
}
