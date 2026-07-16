package com.xunxian.seekingimmortals.registry;

import com.xunxian.seekingimmortals.SeekingImmortalsMod;
import net.minecraft.world.entity.npc.Villager;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = SeekingImmortalsMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ModEntityAttributes {
    private ModEntityAttributes() {}

    @SubscribeEvent
    public static void register(EntityAttributeCreationEvent event) {
        event.put(ModEntities.SECT_STEWARD.get(), Villager.createAttributes().build());
        event.put(ModEntities.MARKET_TRADER.get(), Villager.createAttributes().build());
        event.put(ModEntities.SPIRIT_STONE_BANKER.get(), Villager.createAttributes().build());
    }
}
