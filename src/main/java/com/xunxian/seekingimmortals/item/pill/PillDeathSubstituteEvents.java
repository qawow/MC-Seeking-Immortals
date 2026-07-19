package com.xunxian.seekingimmortals.item.pill;

import com.xunxian.seekingimmortals.SeekingImmortalsMod;
import com.xunxian.seekingimmortals.cultivation.CultivationHelper;
import com.xunxian.seekingimmortals.network.SyncCultivationDataPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = SeekingImmortalsMod.MODID)
public final class PillDeathSubstituteEvents {
    private PillDeathSubstituteEvents() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || player.getPersistentData().getBoolean("SeekingImmortalsLifespanDeath")) {
            return;
        }
        if (trySubstitute(player)) {
            event.setCanceled(true);
        }
    }

    public static boolean trySubstitute(ServerPlayer player) {
        return CultivationHelper.get(player).map(cultivation -> {
            if (!cultivation.consumeDeathSubstitute()) return false;
            player.setHealth(Math.max(1.0F, player.getMaxHealth() * 0.35F));
            player.clearFire();
            player.removeEffect(MobEffects.POISON);
            player.removeEffect(MobEffects.WITHER);
            player.removeEffect(MobEffects.BLINDNESS);
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 20 * 12, 1, false, false));
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 20 * 8, 1, false, false));
            SyncCultivationDataPacket.send(player, cultivation);
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.catalog_pill.death_substitute_saved"), false);
            return true;
        }).orElse(false);
    }
}
