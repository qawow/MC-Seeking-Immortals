package com.xunxian.seekingimmortals.registry;

import com.xunxian.seekingimmortals.SeekingImmortalsMod;
import com.xunxian.seekingimmortals.worldgen.AncientCultivatorCavePieces;
import com.xunxian.seekingimmortals.worldgen.AncientCultivatorCaveStructure;
import com.xunxian.seekingimmortals.worldgen.LeylineVeinPieces;
import com.xunxian.seekingimmortals.worldgen.LeylineVeinStructure;
import com.xunxian.seekingimmortals.worldgen.SpiritBeastDenPieces;
import com.xunxian.seekingimmortals.worldgen.SpiritBeastDenStructure;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/**
 * Wave493: multi-biome physical leyline structure registration.
 * Hash aura in SpiritualAuraManager remains authority; structures are presentation/body.
 */
public final class ModStructures {
    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES =
            DeferredRegister.create(Registries.STRUCTURE_TYPE, SeekingImmortalsMod.MODID);
    public static final DeferredRegister<StructurePieceType> STRUCTURE_PIECES =
            DeferredRegister.create(Registries.STRUCTURE_PIECE, SeekingImmortalsMod.MODID);

    public static final RegistryObject<StructureType<LeylineVeinStructure>> LEYLINE_VEIN =
            STRUCTURE_TYPES.register("leyline_vein", () -> explicitType(LeylineVeinStructure.CODEC));

    public static final RegistryObject<StructurePieceType> LEYLINE_VEIN_PIECE =
            STRUCTURE_PIECES.register("leyline_vein", () -> (StructurePieceType.ContextlessType) LeylineVeinPieces.Piece::new);

    public static final RegistryObject<StructureType<AncientCultivatorCaveStructure>> ANCIENT_CULTIVATOR_CAVE =
            STRUCTURE_TYPES.register("ancient_cultivator_cave", () -> explicitType(AncientCultivatorCaveStructure.CODEC));

    public static final RegistryObject<StructurePieceType> ANCIENT_CULTIVATOR_CAVE_PIECE =
            STRUCTURE_PIECES.register("ancient_cultivator_cave", () -> (StructurePieceType.ContextlessType) AncientCultivatorCavePieces.Piece::new);

    public static final RegistryObject<StructureType<SpiritBeastDenStructure>> SPIRIT_BEAST_DEN =
            STRUCTURE_TYPES.register("spirit_beast_den", () -> explicitType(SpiritBeastDenStructure.CODEC));

    public static final RegistryObject<StructurePieceType> SPIRIT_BEAST_DEN_PIECE =
            STRUCTURE_PIECES.register("spirit_beast_den", () -> (StructurePieceType.ContextlessType) SpiritBeastDenPieces.Piece::new);

    private ModStructures() {}

    private static <S extends Structure> StructureType<S> explicitType(com.mojang.serialization.Codec<S> codec) {
        return () -> codec;
    }

    public static void register(IEventBus bus) {
        STRUCTURE_TYPES.register(bus);
        STRUCTURE_PIECES.register(bus);
    }
}
