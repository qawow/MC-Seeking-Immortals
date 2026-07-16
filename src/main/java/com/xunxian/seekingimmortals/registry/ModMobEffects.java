package com.xunxian.seekingimmortals.registry;

import com.xunxian.seekingimmortals.SeekingImmortalsMod;
import com.xunxian.seekingimmortals.combat.status.SeekingStatusEffect;
import com.xunxian.seekingimmortals.combat.status.StatusCatalogService;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * M14 统一状态 MobEffect 注册。
 * <p>注册 id 与 {@code text_material/status_effects.json} 的 {@code effects[].id} 逐字一致。</p>
 */
public final class ModMobEffects {
    public static final DeferredRegister<MobEffect> MOB_EFFECTS =
            DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, SeekingImmortalsMod.MODID);

    private static final Map<String, RegistryObject<MobEffect>> BY_ID = new LinkedHashMap<>();

    static {
        for (StatusCatalogService.StatusDefinition def : StatusCatalogService.builtin().effects()) {
            RegistryObject<MobEffect> object = MOB_EFFECTS.register(def.id(),
                    () -> new SeekingStatusEffect(
                            def.id(),
                            def.beneficial() ? MobEffectCategory.BENEFICIAL : MobEffectCategory.HARMFUL,
                            def.colorRgb(),
                            def.tickDamage(),
                            def.tickHeal(),
                            def.tickInterval(),
                            def.movementMul(),
                            def.outgoingDamageMul(),
                            def.defenseMul(),
                            def.blocksTechnique(),
                            def.hidesRealm()));
            BY_ID.put(def.id(), object);
        }
    }

    private ModMobEffects() {}

    public static void register(IEventBus bus) {
        MOB_EFFECTS.register(bus);
    }

    public static Map<String, RegistryObject<MobEffect>> byId() {
        return Collections.unmodifiableMap(BY_ID);
    }

    public static RegistryObject<MobEffect> get(String statusId) {
        if (statusId == null || statusId.isBlank()) {
            return null;
        }
        return BY_ID.get(statusId.trim().toLowerCase());
    }
}
