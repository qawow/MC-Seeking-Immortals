package com.xunxian.seekingimmortals;

import com.mojang.logging.LogUtils;
import com.xunxian.seekingimmortals.network.ModNetwork;
import com.xunxian.seekingimmortals.registry.ModBlocks;
import com.xunxian.seekingimmortals.registry.ModCreativeTabs;
import com.xunxian.seekingimmortals.registry.ModEntities;
import com.xunxian.seekingimmortals.registry.ModItems;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(SeekingImmortalsMod.MODID)
public class SeekingImmortalsMod {
    public static final String MODID = "seeking_immortals";
    public static final Logger LOGGER = LogUtils.getLogger();

    public SeekingImmortalsMod() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModItems.register(modBus);
        ModBlocks.register(modBus);
        ModEntities.register(modBus);
        ModCreativeTabs.register(modBus);
        ModNetwork.register();
    }
}
