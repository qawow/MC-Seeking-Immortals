package com.xunxian.seekingimmortals.registry;

import com.xunxian.seekingimmortals.SeekingImmortalsMod;
import com.xunxian.seekingimmortals.entity.CultivationFireballEntity;
import com.xunxian.seekingimmortals.entity.CultivationBeastEntity;
import com.xunxian.seekingimmortals.entity.CushionSeatEntity;
import com.xunxian.seekingimmortals.entity.MarketTraderEntity;
import com.xunxian.seekingimmortals.entity.QuestNpcEntity;
import com.xunxian.seekingimmortals.entity.SectStewardEntity;
import com.xunxian.seekingimmortals.entity.SpiritBoatEntity;
import com.xunxian.seekingimmortals.entity.SpiritStoneBankerEntity;
import com.xunxian.seekingimmortals.entity.SummonedServitorEntity;
import com.xunxian.seekingimmortals.entity.SwordProjectileEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
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

    public static final RegistryObject<EntityType<CultivationFireballEntity>> CULTIVATION_FIREBALL = ENTITIES.register("cultivation_fireball", () -> EntityType.Builder.<CultivationFireballEntity>of(CultivationFireballEntity::new, MobCategory.MISC)
            .sized(0.45F, 0.45F)
            .clientTrackingRange(64)
            .updateInterval(1)
            .build("cultivation_fireball"));

    public static final RegistryObject<EntityType<SectStewardEntity>> SECT_STEWARD = ENTITIES.register("sect_steward", () -> EntityType.Builder.<SectStewardEntity>of(SectStewardEntity::new, MobCategory.CREATURE)
            .sized(0.6F, 1.95F)
            .clientTrackingRange(10)
            .updateInterval(3)
            .build("sect_steward"));

    public static final RegistryObject<EntityType<MarketTraderEntity>> MARKET_TRADER = ENTITIES.register("market_trader", () -> EntityType.Builder.<MarketTraderEntity>of(MarketTraderEntity::new, MobCategory.CREATURE)
            .sized(0.6F, 1.95F)
            .clientTrackingRange(10)
            .updateInterval(3)
            .build("market_trader"));

    public static final RegistryObject<EntityType<SpiritStoneBankerEntity>> SPIRIT_STONE_BANKER = ENTITIES.register("spirit_stone_banker", () -> EntityType.Builder.<SpiritStoneBankerEntity>of(SpiritStoneBankerEntity::new, MobCategory.CREATURE)
            .sized(0.6F, 1.95F)
            .clientTrackingRange(10)
            .updateInterval(3)
            .build("spirit_stone_banker"));

    public static final RegistryObject<EntityType<QuestNpcEntity>> QUEST_NPC = ENTITIES.register("quest_npc", () -> EntityType.Builder.<QuestNpcEntity>of(QuestNpcEntity::new, MobCategory.CREATURE)
            .sized(0.6F, 1.95F)
            .clientTrackingRange(10)
            .updateInterval(3)
            .build("quest_npc"));

    public static final RegistryObject<EntityType<CultivationBeastEntity>> CULTIVATION_BEAST = ENTITIES.register("cultivation_beast", () -> EntityType.Builder.<CultivationBeastEntity>of(CultivationBeastEntity::new, MobCategory.MONSTER)
            .sized(0.95F, 1.25F)
            .clientTrackingRange(12)
            .updateInterval(2)
            .build("cultivation_beast"));

    public static final RegistryObject<EntityType<SummonedServitorEntity>> SUMMONED_SERVITOR = ENTITIES.register("summoned_servitor", () -> EntityType.Builder.<SummonedServitorEntity>of(SummonedServitorEntity::new, MobCategory.CREATURE)
            .sized(0.7F, 1.9F)
            .clientTrackingRange(12)
            .updateInterval(3)
            .build("summoned_servitor"));

    public static final RegistryObject<EntityType<SpiritBoatEntity>> SPIRIT_BOAT = ENTITIES.register("spirit_boat", () -> EntityType.Builder.<SpiritBoatEntity>of(SpiritBoatEntity::new, MobCategory.MISC)
            .sized(1.4F, 0.6F)
            .clientTrackingRange(10)
            .updateInterval(3)
            .build("spirit_boat"));

    private ModEntities() {}

    public static void register(IEventBus bus) {
        ENTITIES.register(bus);
        bus.addListener(ModEntities::onAttributeCreation);
        bus.addListener(ModEntities::onSpawnPlacementRegister);
    }

    private static void onAttributeCreation(EntityAttributeCreationEvent event) {
        event.put(SUMMONED_SERVITOR.get(), SummonedServitorEntity.createAttributes().build());
        event.put(CULTIVATION_BEAST.get(), CultivationBeastEntity.createAttributes().build());
    }

    private static void onSpawnPlacementRegister(SpawnPlacementRegisterEvent event) {
        event.register(
                CULTIVATION_BEAST.get(),
                SpawnPlacements.Type.NO_RESTRICTIONS,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                CultivationBeastEntity::checkSpawnRules,
                SpawnPlacementRegisterEvent.Operation.REPLACE);
    }
}
