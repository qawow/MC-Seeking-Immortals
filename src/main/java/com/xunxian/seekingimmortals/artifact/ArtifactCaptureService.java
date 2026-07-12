package com.xunxian.seekingimmortals.artifact;

import com.xunxian.seekingimmortals.catalog.SummonHonestMvpService;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

/**
 * Capture container runtime (Wave51).
 * Empty jar captures nearby monster id; filled jar releases a timed servitor proxy.
 */
public final class ArtifactCaptureService {
    private static final String TAG = "SeekingImmortalsCapturedId";

    private ArtifactCaptureService() {}

    public static String storedId(ItemStack stack) {
        if (stack == null || !stack.hasTag()) {
            return "";
        }
        return stack.getTag().getString(TAG);
    }

    public static boolean releaseOrCapture(ServerPlayer player, ItemStack jar) {
        String stored = storedId(jar);
        if (stored != null && !stored.isBlank()) {
            boolean ok = SummonHonestMvpService.spawnConfigured(player, "captured_" + stored, 20 * 45, 30.0D, 5.5D);
            if (ok) {
                jar.getOrCreateTag().remove(TAG);
                player.displayClientMessage(Component.translatable("message.seeking_immortals.capture.released", stored), true);
            }
            return ok;
        }
        AABB box = player.getBoundingBox().inflate(4.0D);
        for (LivingEntity living : player.serverLevel().getEntitiesOfClass(LivingEntity.class, box)) {
            if (living instanceof Monster monster && living.isAlive() && living != player) {
                String id = monster.getType().toShortString();
                jar.getOrCreateTag().putString(TAG, id);
                monster.addEffect(new MobEffectInstance(MobEffects.GLOWING, 40, 0));
                monster.discard();
                player.displayClientMessage(Component.translatable("message.seeking_immortals.capture.caught", id), true);
                return true;
            }
        }
        player.displayClientMessage(Component.translatable("message.seeking_immortals.capture.none"), true);
        return false;
    }
}
