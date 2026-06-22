package com.xunxian.seekingimmortals.registry;

import com.xunxian.seekingimmortals.SeekingImmortalsMod;
import com.xunxian.seekingimmortals.entity.CushionSeatEntity;
import com.xunxian.seekingimmortals.entity.SwordProjectileEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, SeekingImmortalsMod.MODID);

    public static final RegistryObject<EntityType<CushionSeatEntity>> CUSHION_SEAT = ENTITIES.register("cushion_seat", () -> EntityType.Builder.<CushionSeatEntity>of(CushionSeatEntity::new, MobCategory.MISC)
            .sized(0.01F, 0.01F)
            .clientTrackingRange(8)
            .updateInterval(20)
            .build("cushion_seat"));

    public static final RegistryObject<EntityType<SwordProjectileEntity>> SWORD_PROJECTILE = ENTITIES.register("sword_projectile", () -> EntityType.Builder.<SwordProjectileEntity>of(SwordProjectileEntity::new, MobCategory.MISC)
            .sized(0.35F, 0.35F)
            .clientTrackingRange(64)
            .updateInterval(1)
            .build("sword_projectile"));

    private ModEntities() {}

    public static void register(IEventBus bus) {
        ENTITIES.register(bus);
    }
}
