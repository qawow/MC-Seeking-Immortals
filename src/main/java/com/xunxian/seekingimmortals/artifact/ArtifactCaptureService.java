package com.xunxian.seekingimmortals.artifact;

import com.xunxian.seekingimmortals.catalog.SummonHonestMvpService;
import com.xunxian.seekingimmortals.cultivation.BeastContractService;
import com.xunxian.seekingimmortals.entity.SummonedServitorEntity;
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
 * Wave458: filled jar can seal into BeastContract (sneak) or release as BEAST archetype.
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
        return releaseOrCapture(player, jar, player.isShiftKeyDown());
    }

    public static boolean releaseOrCapture(ServerPlayer player, ItemStack jar, boolean sneakSeal) {
        String stored = storedId(jar);
        if (stored != null && !stored.isBlank()) {
            if (sneakSeal) {
                return sealContract(player, jar, stored);
            }
            return releaseBeast(player, jar, stored);
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

    private static boolean sealContract(ServerPlayer player, ItemStack jar, String stored) {
        // Captured quality starts with better affinity.
        boolean ok = BeastContractService.contract(player, stored, 5, 0);
        if (ok) {
            jar.getOrCreateTag().remove(TAG);
            player.displayClientMessage(Component.translatable("message.seeking_immortals.capture.sealed", stored), true);
        }
        return ok;
    }

    private static boolean releaseBeast(ServerPlayer player, ItemStack jar, String stored) {
        double health = 30.0D;
        double damage = 5.5D;
        int life = 20 * 45;
        // If already contracted, scale from affinity/growth.
        if (BeastContractService.hasContract(player, stored)) {
            for (BeastContractService.Contract contract : BeastContractService.list(player)) {
                if (contract.id().equalsIgnoreCase(stored)) {
                    health = 24.0D + contract.affinity() * 0.4D + contract.growth() * 2.0D;
                    damage = 4.0D + contract.affinity() * 0.05D + contract.growth() * 0.4D;
                    life = 20 * (25 + contract.growth() * 2);
                    break;
                }
            }
        }
        boolean ok = SummonHonestMvpService.spawnConfigured(
                player, "beast_" + stored, life, health, damage, SummonedServitorEntity.Archetype.BEAST);
        if (ok) {
            jar.getOrCreateTag().remove(TAG);
            player.displayClientMessage(Component.translatable("message.seeking_immortals.capture.released", stored), true);
        }
        return ok;
    }
}
