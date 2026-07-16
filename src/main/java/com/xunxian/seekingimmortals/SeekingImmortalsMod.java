package com.xunxian.seekingimmortals;

import com.mojang.logging.LogUtils;
import com.xunxian.seekingimmortals.combat.AttributeLimitUnlocker;
import com.xunxian.seekingimmortals.network.ModNetwork;
import com.xunxian.seekingimmortals.quest.FtbCustomTaskHooks;
import com.xunxian.seekingimmortals.quest.FtbQuestDefaults;
import com.xunxian.seekingimmortals.registry.ModBlocks;
import com.xunxian.seekingimmortals.registry.ModBlockEntities;
import com.xunxian.seekingimmortals.registry.ModCreativeTabs;
import com.xunxian.seekingimmortals.registry.ModEntities;
import com.xunxian.seekingimmortals.registry.ModItems;
import com.xunxian.seekingimmortals.registry.ModBulkItems;
import com.xunxian.seekingimmortals.registry.ModMenus;
import com.xunxian.seekingimmortals.registry.ModMobEffects;
import com.xunxian.seekingimmortals.registry.ModRecipes;
import com.xunxian.seekingimmortals.registry.ModSounds;
import com.xunxian.seekingimmortals.registry.ModStructures;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(SeekingImmortalsMod.MODID)
public class SeekingImmortalsMod {
    public static final String MODID = "seeking_immortals";
    public static final Logger LOGGER = LogUtils.getLogger();

    public SeekingImmortalsMod() {
        AttributeLimitUnlocker.unlockCombatAttributeCaps();
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModItems.register(modBus);
        ModBulkItems.register(modBus);
        ModMobEffects.register(modBus);
        ModBlocks.register(modBus);
        ModBlockEntities.register(modBus);
        ModEntities.register(modBus);
        ModMenus.register(modBus);
        ModRecipes.register(modBus);
        ModSounds.register(modBus);
        ModStructures.register(modBus);
        ModCreativeTabs.register(modBus);
        ModNetwork.register();
        FtbQuestDefaults.bootstrapDefaultPack();
        // Wave488: bind FTB custom tasks to sect-war / reputation authority checks.
        FtbCustomTaskHooks.register();
    }
}
