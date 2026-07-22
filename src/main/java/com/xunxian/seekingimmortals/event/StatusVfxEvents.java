package com.xunxian.seekingimmortals.event;

import com.xunxian.seekingimmortals.SeekingImmortalsMod;
import com.xunxian.seekingimmortals.combat.status.SeekingStatusEffect;
import com.xunxian.seekingimmortals.combat.status.StatusVfxService;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Bridges custom MobEffect lifecycle changes to bounded status VFX intents. */
@Mod.EventBusSubscriber(modid = SeekingImmortalsMod.MODID)
public final class StatusVfxEvents {
    private StatusVfxEvents() {}

    @SubscribeEvent
    public static void onAdded(MobEffectEvent.Added event) {
        if (!serverEntity(event.getEntity()) || event.getOldEffectInstance() != null) {
            return;
        }
        MobEffectInstance instance = event.getEffectInstance();
        if (instance.getEffect() instanceof SeekingStatusEffect effect) {
            StatusVfxService.emitApplied(event.getEntity(), effect, instance.getAmplifier());
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRemoved(MobEffectEvent.Remove event) {
        if (!serverEntity(event.getEntity())) {
            return;
        }
        MobEffectInstance instance = event.getEffectInstance();
        if (instance != null && instance.getEffect() instanceof SeekingStatusEffect effect) {
            StatusVfxService.emitDissipate(event.getEntity(), effect, instance.getAmplifier());
        }
    }

    @SubscribeEvent
    public static void onExpired(MobEffectEvent.Expired event) {
        if (!serverEntity(event.getEntity())) {
            return;
        }
        MobEffectInstance instance = event.getEffectInstance();
        if (instance != null && instance.getEffect() instanceof SeekingStatusEffect effect) {
            StatusVfxService.emitDissipate(event.getEntity(), effect, instance.getAmplifier());
        }
    }

    private static boolean serverEntity(LivingEntity entity) {
        return entity != null && entity.level() instanceof ServerLevel;
    }
}
