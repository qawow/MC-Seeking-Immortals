package com.xunxian.seekingimmortals.client;

import com.xunxian.seekingimmortals.SeekingImmortalsMod;
import com.xunxian.seekingimmortals.registry.ModParticles;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import team.lodestar.lodestone.systems.particle.world.type.LodestoneWorldParticleType;

/** Client providers for the mod-owned Lodestone particle identities. */
@Mod.EventBusSubscriber(modid = SeekingImmortalsMod.MODID, value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ModParticleProviders {
    private ModParticleProviders() {}

    @SubscribeEvent
    public static void register(RegisterParticleProvidersEvent event) {
        register(event, ModParticles.QI_SOFT.get());
        register(event, ModParticles.FIRE_EMBER.get());
        register(event, ModParticles.WATER_MIST.get());
        register(event, ModParticles.WOOD_POLLEN.get());
        register(event, ModParticles.METAL_SPARK.get());
        register(event, ModParticles.EARTH_DUST.get());
        register(event, ModParticles.THUNDER_ARC.get());
        register(event, ModParticles.YIN_SMOKE.get());
        register(event, ModParticles.SOUL_WISPS.get());
        register(event, ModParticles.BLOOD_MIST.get());
        register(event, ModParticles.HEAL_MOTES.get());
        register(event, ModParticles.SPACE_GLITCH.get());
    }

    private static void register(RegisterParticleProvidersEvent event, LodestoneWorldParticleType type) {
        event.registerSpriteSet(type, LodestoneWorldParticleType.Factory::new);
    }
}
