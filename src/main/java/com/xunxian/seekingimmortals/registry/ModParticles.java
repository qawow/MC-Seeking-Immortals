package com.xunxian.seekingimmortals.registry;

import com.xunxian.seekingimmortals.SeekingImmortalsMod;
import net.minecraft.core.particles.ParticleType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import team.lodestar.lodestone.systems.particle.world.type.LodestoneWorldParticleType;

/**
 * Mod-owned particle identities. Lodestone still supplies the particle implementation,
 * but textures and resource ownership stay in this mod's namespace.
 */
public final class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLES =
            DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, SeekingImmortalsMod.MODID);

    public static final RegistryObject<LodestoneWorldParticleType> QI_SOFT = register("qi_soft");
    public static final RegistryObject<LodestoneWorldParticleType> FIRE_EMBER = register("fire_ember");
    public static final RegistryObject<LodestoneWorldParticleType> WATER_MIST = register("water_mist");
    public static final RegistryObject<LodestoneWorldParticleType> WOOD_POLLEN = register("wood_pollen");
    public static final RegistryObject<LodestoneWorldParticleType> METAL_SPARK = register("metal_spark");
    public static final RegistryObject<LodestoneWorldParticleType> EARTH_DUST = register("earth_dust");
    public static final RegistryObject<LodestoneWorldParticleType> THUNDER_ARC = register("thunder_arc");
    public static final RegistryObject<LodestoneWorldParticleType> YIN_SMOKE = register("yin_smoke");
    public static final RegistryObject<LodestoneWorldParticleType> SOUL_WISPS = register("soul_wisps");
    public static final RegistryObject<LodestoneWorldParticleType> BLOOD_MIST = register("blood_mist");
    public static final RegistryObject<LodestoneWorldParticleType> HEAL_MOTES = register("heal_motes");
    public static final RegistryObject<LodestoneWorldParticleType> SPACE_GLITCH = register("space_glitch");

    private ModParticles() {}

    private static RegistryObject<LodestoneWorldParticleType> register(String name) {
        return PARTICLES.register(name, LodestoneWorldParticleType::new);
    }

    public static void register(IEventBus bus) {
        PARTICLES.register(bus);
    }
}
