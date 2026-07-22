package com.xunxian.seekingimmortals.registry;

import com.xunxian.seekingimmortals.SeekingImmortalsMod;
import com.xunxian.seekingimmortals.entity.CultivatorNpcEntity;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = SeekingImmortalsMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ModEntityAttributes {
    private ModEntityAttributes() {}

    @SubscribeEvent
    public static void register(EntityAttributeCreationEvent event) {
        event.put(ModEntities.SECT_STEWARD.get(), CultivatorNpcEntity.createAttributes().build());
        event.put(ModEntities.MARKET_TRADER.get(), CultivatorNpcEntity.createAttributes().build());
        event.put(ModEntities.SPIRIT_STONE_BANKER.get(), CultivatorNpcEntity.createAttributes().build());
        event.put(ModEntities.QUEST_NPC.get(), CultivatorNpcEntity.createAttributes().build());
    }
}
