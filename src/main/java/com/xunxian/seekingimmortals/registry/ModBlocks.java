package com.xunxian.seekingimmortals.registry;

import com.xunxian.seekingimmortals.SeekingImmortalsMod;
import com.xunxian.seekingimmortals.block.LingGenIdentificationSlabBlock;
import com.xunxian.seekingimmortals.block.MeditationCushionBlock;
import com.xunxian.seekingimmortals.block.SpiritGatheringArrayBlock;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, SeekingImmortalsMod.MODID);

    public static final RegistryObject<Block> SPIRIT_ORE = BLOCKS.register("spirit_ore", () -> new DropExperienceBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(3.0F, 3.0F).requiresCorrectToolForDrops().sound(SoundType.STONE),
            UniformInt.of(2, 5)));

    public static final RegistryObject<Block> MEDITATION_CUSHION = BLOCKS.register("meditation_cushion", () -> new MeditationCushionBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(0.4F).sound(SoundType.WOOL).noOcclusion()));

    public static final RegistryObject<Block> LING_GEN_IDENTIFICATION_SLAB = BLOCKS.register("ling_gen_identification_slab", () -> new LingGenIdentificationSlabBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(1.2F, 3.0F).requiresCorrectToolForDrops().sound(SoundType.AMETHYST).noOcclusion()));

    public static final RegistryObject<Block> SPIRIT_GATHERING_ARRAY = BLOCKS.register("spirit_gathering_array", () -> new SpiritGatheringArrayBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN).strength(1.5F, 3.0F).requiresCorrectToolForDrops().sound(SoundType.AMETHYST).noOcclusion()));

    private ModBlocks() {}
    public static void register(IEventBus bus) { BLOCKS.register(bus); }
}
