package com.xunxian.seekingimmortals.artifact;

import com.xunxian.seekingimmortals.beast.BeastBestiaryService;
import com.xunxian.seekingimmortals.beast.BeastTierService;
import com.xunxian.seekingimmortals.beast.BestiaryUnlockService;
import com.xunxian.seekingimmortals.catalog.SummonHonestMvpService;
import com.xunxian.seekingimmortals.cultivation.BeastContractService;
import com.xunxian.seekingimmortals.cultivation.CultivationHelper;
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
 * M10: thirteen-tier capture caps aligned to player realm; ecology beast capture preferred.
 */
public final class ArtifactCaptureService {
    private static final String TAG = "SeekingImmortalsCapturedId";
    private static final String TAG_TIER = "SeekingImmortalsCapturedTier";
    /** Capture gap: player tier may lag target by at most this. */
    private static final int CAPTURE_TIER_GAP = 1;

    private ArtifactCaptureService() {}

    public static String storedId(ItemStack stack) {
        if (stack == null || !stack.hasTag()) {
            return "";
        }
        return stack.getTag().getString(TAG);
    }

    public static int storedTier(ItemStack stack) {
        if (stack == null || !stack.hasTag()) {
            return 1;
        }
        int tier = stack.getTag().getInt(TAG_TIER);
        return tier <= 0 ? 1 : BeastTierService.clampTier(tier);
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
        // Prefer ecology-tagged beasts (M10).
        LivingEntity best = null;
        int bestTier = Integer.MAX_VALUE;
        for (LivingEntity living : player.serverLevel().getEntitiesOfClass(LivingEntity.class, box)) {
            if (living == player || !living.isAlive()) {
                continue;
            }
            boolean ecology = living.getPersistentData().getBoolean("seeking_immortals_ecology_beast")
                    || living.getPersistentData().contains("seeking_immortals_beast_id");
            boolean monster = living instanceof Monster;
            if (!ecology && !monster) {
                continue;
            }
            int tier = living.getPersistentData().contains("seeking_immortals_beast_tier")
                    ? living.getPersistentData().getInt("seeking_immortals_beast_tier")
                    : resolveTier(living);
            if (tier < bestTier || (tier == bestTier && ecology)) {
                best = living;
                bestTier = tier;
            }
        }
        if (best == null) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.capture.none"), true);
            return false;
        }
        int tier = bestTier <= 0 ? 1 : BeastTierService.clampTier(bestTier);
        int playerTier = playerCaptureTier(player);
        if (!BeastTierService.canSuppress(playerTier, tier, CAPTURE_TIER_GAP)) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.capture.tier_cap", tier, playerTier), true);
            return false;
        }
        // Absolute hard cap: never capture tier beyond player realm band + gap, max 13.
        int maxCapturable = Math.min(13, playerTier + CAPTURE_TIER_GAP);
        if (tier > maxCapturable) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.capture.tier_cap", tier, maxCapturable), true);
            return false;
        }
        String id = best.getPersistentData().contains("seeking_immortals_beast_id")
                ? best.getPersistentData().getString("seeking_immortals_beast_id")
                : best.getType().toShortString();
        if (id == null || id.isBlank()) {
            id = best.getType().toShortString();
        }
        id = BeastBestiaryService.find(id).map(BeastBestiaryService.BeastEntry::id).orElse(id);
        jar.getOrCreateTag().putString(TAG, id);
        jar.getOrCreateTag().putInt(TAG_TIER, tier);
        best.addEffect(new MobEffectInstance(MobEffects.GLOWING, 40, 0));
        best.discard();
        BestiaryUnlockService.unlock(player, id, BestiaryUnlockService.UnlockKind.SEEN);
        player.displayClientMessage(Component.translatable("message.seeking_immortals.capture.caught", id), true);
        return true;
    }

    private static int resolveTier(LivingEntity living) {
        if (living instanceof SummonedServitorEntity servitor) {
            String sid = servitor.getSummonId();
            String beastId = BeastContractService.beastIdFromSummonId(sid);
            if (!beastId.isBlank()) {
                return BeastBestiaryService.find(beastId).map(BeastBestiaryService.BeastEntry::tier).orElse(1);
            }
        }
        return 1;
    }

    private static int playerCaptureTier(ServerPlayer player) {
        return CultivationHelper.get(player).map(c -> switch (c.getRealm()) {
            case MORTAL -> 0;
            case QI_REFINING -> 4;
            case FOUNDATION_ESTABLISHMENT -> 8;
            case CORE_FORMATION -> 10;
            case NASCENT_SOUL -> 12;
            default -> 13;
        }).orElse(0);
    }

    private static boolean sealContract(ServerPlayer player, ItemStack jar, String stored) {
        // Captured quality starts with better affinity; tier already gated at capture time.
        boolean ok = BeastContractService.contract(player, stored, 5, 0);
        if (ok) {
            jar.getOrCreateTag().remove(TAG);
            jar.getOrCreateTag().remove(TAG_TIER);
            player.displayClientMessage(Component.translatable("message.seeking_immortals.capture.sealed", stored), true);
        }
        return ok;
    }

    private static boolean releaseBeast(ServerPlayer player, ItemStack jar, String stored) {
        int tier = storedTier(jar);
        BeastTierService.ScaledStats stats = BeastTierService.scaleStats(tier);
        double health = stats.health() * 0.7D;
        double damage = stats.damage() * 0.7D;
        int life = stats.lifeTicks() / 2;
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
            jar.getOrCreateTag().remove(TAG_TIER);
            player.displayClientMessage(Component.translatable("message.seeking_immortals.capture.released", stored), true);
        }
        return ok;
    }
}
