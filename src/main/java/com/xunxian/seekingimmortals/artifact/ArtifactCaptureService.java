package com.xunxian.seekingimmortals.artifact;

import com.xunxian.seekingimmortals.beast.BeastBestiaryService;
import com.xunxian.seekingimmortals.beast.BeastTierService;
import com.xunxian.seekingimmortals.beast.BestiaryUnlockService;
import com.xunxian.seekingimmortals.catalog.SummonHonestMvpService;
import com.xunxian.seekingimmortals.cultivation.BeastContractService;
import com.xunxian.seekingimmortals.cultivation.CultivationHelper;
import com.xunxian.seekingimmortals.entity.CultivationBeastEntity;
import com.xunxian.seekingimmortals.entity.SummonedServitorEntity;
import com.xunxian.seekingimmortals.worldpack.BossEncounterService;
import com.xunxian.seekingimmortals.worldpack.SecretRealmTrialService;
import com.xunxian.seekingimmortals.util.PlayerDisplayText;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
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
    static final float CAPTURE_HEALTH_RATIO = 0.35F;

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
        LivingEntity best = null;
        int bestTier = Integer.MAX_VALUE;
        boolean bestWeakened = false;
        for (LivingEntity living : player.serverLevel().getEntitiesOfClass(LivingEntity.class, box)) {
            if (living == player || !isCapturableTarget(living)) {
                continue;
            }
            int tier = living.getPersistentData().contains("seeking_immortals_beast_tier")
                    ? living.getPersistentData().getInt("seeking_immortals_beast_tier")
                    : resolveTier(living);
            boolean weakened = isWeakened(living);
            if (best == null || (weakened && !bestWeakened)
                    || (weakened == bestWeakened && tier < bestTier)) {
                best = living;
                bestTier = tier;
                bestWeakened = weakened;
            }
        }
        if (best == null) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.capture.none"), true);
            return false;
        }
        if (!isWeakened(best)) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.capture.health", Math.round(CAPTURE_HEALTH_RATIO * 100.0F)), true);
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
        String id = beastIdOf(best);
        jar.getOrCreateTag().putString(TAG, id);
        jar.getOrCreateTag().putInt(TAG_TIER, tier);
        best.addEffect(new MobEffectInstance(MobEffects.GLOWING, 40, 0));
        best.discard();
        BestiaryUnlockService.unlock(player, id, BestiaryUnlockService.UnlockKind.SEEN);
        // Q-B-4: the capture transaction removed the beast; record the structured capture proof.
        com.xunxian.seekingimmortals.quest.DetailedQuestProofService.recordEntityCaptured(player, id);
        player.displayClientMessage(Component.translatable("message.seeking_immortals.capture.caught",
                beastDisplay(id)), true);
        return true;
    }

    static boolean isCapturableTarget(LivingEntity living) {
        if (living == null || !living.isAlive()) {
            return false;
        }
        if (!living.getPersistentData().getBoolean("seeking_immortals_ecology_beast")) {
            return false;
        }
        if (living instanceof net.minecraft.world.entity.player.Player
                || living instanceof net.minecraft.world.entity.npc.AbstractVillager) {
            return false;
        }
        if (living instanceof net.minecraft.world.entity.Mob mob
                && (BossEncounterService.isBossMob(mob) || SecretRealmTrialService.isTrialMob(mob))) {
            return false;
        }
        if (living instanceof SummonedServitorEntity servitor && !servitor.isHostileTrial()) {
            return false;
        }
        return BeastBestiaryService.find(beastIdOf(living))
                .filter(BeastBestiaryService.BeastEntry::tameable)
                .filter(entry -> !entry.trueSpirit() && !entry.companionOnly())
                .isPresent();
    }

    static boolean isWeakened(LivingEntity living) {
        return living != null && living.getMaxHealth() > 0.0F
                && living.getHealth() <= living.getMaxHealth() * CAPTURE_HEALTH_RATIO;
    }

    private static String beastIdOf(LivingEntity living) {
        if (living instanceof CultivationBeastEntity beast) {
            return beast.getBeastId();
        }
        String id = living.getPersistentData().getString("seeking_immortals_beast_id");
        if (id == null || id.isBlank()) {
            id = living.getType().toShortString();
        }
        return BeastBestiaryService.find(id).map(BeastBestiaryService.BeastEntry::id).orElse(id);
    }

    private static int resolveTier(LivingEntity living) {
        if (living instanceof CultivationBeastEntity beast) {
            return beast.getBeastTier();
        }
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
            player.displayClientMessage(Component.translatable("message.seeking_immortals.capture.sealed",
                    beastDisplay(stored)), true);
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
        boolean ok = SummonHonestMvpService.spawnBeastConfigured(
                player, stored, tier, life, health, damage);
        if (ok) {
            jar.getOrCreateTag().remove(TAG);
            jar.getOrCreateTag().remove(TAG_TIER);
            player.displayClientMessage(Component.translatable("message.seeking_immortals.capture.released",
                    beastDisplay(stored)), true);
        }
        return ok;
    }

    private static Component beastDisplay(String id) {
        return BeastBestiaryService.find(id)
                .map(BeastBestiaryService.BeastEntry::display)
                .map(display -> PlayerDisplayText.safeLiteral(display, "text.seeking_immortals.unknown_item"))
                .orElseGet(() -> Component.translatable("text.seeking_immortals.unknown_item"));
    }
}
