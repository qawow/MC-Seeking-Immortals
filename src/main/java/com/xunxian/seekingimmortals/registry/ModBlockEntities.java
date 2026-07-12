package com.xunxian.seekingimmortals.registry;

import com.xunxian.seekingimmortals.SeekingImmortalsMod;
import com.xunxian.seekingimmortals.block.entity.AlchemyFurnaceBlockEntity;
import com.xunxian.seekingimmortals.block.entity.FormationCoreBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, SeekingImmortalsMod.MODID);

    public static final RegistryObject<BlockEntityType<AlchemyFurnaceBlockEntity>> ALCHEMY_FURNACE =
            BLOCK_ENTITIES.register("alchemy_furnace", () -> BlockEntityType.Builder
                    .of(AlchemyFurnaceBlockEntity::new,
                            ModBlocks.ALCHEMY_FURNACE.get(),
                            ModBlocks.ALCHEMY_FURNACE_TIER_2.get(),
                            ModBlocks.ALCHEMY_FURNACE_TIER_3.get(),
                            ModBlocks.ALCHEMY_FURNACE_TIER_4.get(),
                            ModBlocks.ALCHEMY_FURNACE_TIER_5.get())
                    .build(null));

    public static final RegistryObject<BlockEntityType<FormationCoreBlockEntity>> FORMATION_CORE =
            BLOCK_ENTITIES.register("formation_core", () -> BlockEntityType.Builder
                    .of(FormationCoreBlockEntity::new,
                            ModBlocks.SPIRIT_GATHERING_FORMATION_CORE.get(),
                            ModBlocks.DEFENSE_FORMATION_CORE.get(),
                            ModBlocks.KILL_SWORD_FORMATION_CORE.get(),
                            ModBlocks.SEAL_DEMON_FORMATION_CORE.get(),
                            ModBlocks.ILLUSION_MAZE_FORMATION_CORE.get(),
                            ModBlocks.FIVE_ELEMENTS_MOUNTAIN_FORMATION_CORE.get(),
                            ModBlocks.SPIRIT_GATHERING_MINOR_FORMATION_CORE.get(),
                            ModBlocks.SWORD_ARRAY_BAGUA_FORMATION_CORE.get())
                    .build(null));

    private ModBlockEntities() {}

    public static void register(IEventBus bus) {
        BLOCK_ENTITIES.register(bus);
    }
}
