package com.xunxian.seekingimmortals.artifact;

import com.xunxian.seekingimmortals.cultivation.PlayerCultivation;
import com.xunxian.seekingimmortals.cultivation.ProgressionGateApi;
import com.xunxian.seekingimmortals.cultivation.Realm;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * M15 品阶→属性缩放 / 越阶压制。服务端强制：低于 realm_min 时按
 * {@link ArtifactDataService.RealmPowerScale} 衰减主动技与伤害倍率。
 */
public final class ArtifactPowerService {
    private ArtifactPowerService() {}

    public static double powerScale(ServerPlayer player, ArtifactDataService.ArtifactDefinition artifact) {
        if (player == null || artifact == null) {
            return 0.0D;
        }
        return powerScale(com.xunxian.seekingimmortals.cultivation.CultivationHelper.get(player).orElse(null), artifact);
    }

    public static double powerScale(PlayerCultivation cultivation, ArtifactDataService.ArtifactDefinition artifact) {
        ArtifactDataService.RealmPowerScale scale = ArtifactDataService.builtin().realmPowerScale();
        if (cultivation == null || artifact == null) {
            return scale.belowRealmMin();
        }
        Realm required = resolveRequiredRealm(artifact);
        Realm current = cultivation.getRealm();
        if (required == null || required == Realm.MORTAL) {
            return scale.twoMajorAbove();
        }
        int gap = current.ordinal() - required.ordinal();
        if (gap < 0) {
            return scale.belowRealmMin();
        }
        if (gap == 0) {
            return scale.atRealmMin();
        }
        if (gap >= 2) {
            return scale.twoMajorAbove();
        }
        // 高一境：线性插值 at → full
        return scale.atRealmMin() + (scale.twoMajorAbove() - scale.atRealmMin()) * 0.5D;
    }

    public static boolean isSuppressed(ServerPlayer player, ArtifactDataService.ArtifactDefinition artifact) {
        return powerScale(player, artifact) < ArtifactDataService.builtin().realmPowerScale().atRealmMin() - 1.0e-6D;
    }

    public static boolean meetsMinRealm(ServerPlayer player, ArtifactDataService.ArtifactDefinition artifact) {
        if (artifact == null) {
            return false;
        }
        String realmMin = artifact.realmMin();
        if (realmMin == null || realmMin.isBlank()) {
            return true;
        }
        return ProgressionGateApi.meetsRealm(player, realmMin);
    }

    public static int scaledSpiritualCost(int baseCost, double scale) {
        if (baseCost <= 0) {
            return 0;
        }
        // 越阶使用更费灵力
        double factor = scale < 0.5D ? 1.75D : scale < 0.9D ? 1.25D : 1.0D;
        return Math.max(1, (int) Math.round(baseCost * factor));
    }

    public static int scaledCooldown(int baseCooldown, double scale) {
        if (baseCooldown <= 0) {
            return 0;
        }
        double factor = scale < 0.5D ? 1.5D : scale < 0.9D ? 1.2D : 1.0D;
        return Math.max(20, (int) Math.round(baseCooldown * factor));
    }

    public static double scaledDamage(double baseDamage, double scale) {
        return Math.max(0.0D, baseDamage * Math.max(0.0D, scale));
    }

    /**
     * 十一阶 → 典型境界（用于无 realm_min 的条目）。
     */
    public static Realm realmForGameTier(int gameTier) {
        ArtifactDataService.Snapshot snap = ArtifactDataService.builtin();
        ArtifactDataService.ElevenTier eleven = snap.elevenTiers().get(Math.max(1, Math.min(11, gameTier)));
        if (eleven != null && eleven.realmTypical() != null && !eleven.realmTypical().isBlank()) {
            Realm r = Realm.fromDesignId(eleven.realmTypical());
            if (r != null) {
                return r;
            }
        }
        ArtifactDataService.GradeBand band = snap.gradeBands().get(Math.max(1, Math.min(11, gameTier)));
        if (band != null) {
            Realm r = Realm.fromDesignId(stripStage(band.realmEquiv()));
            if (r != null) {
                return r;
            }
        }
        int t = Math.max(1, gameTier);
        if (t <= 4) return Realm.QI_REFINING;
        if (t <= 7) return Realm.FOUNDATION_ESTABLISHMENT;
        if (t <= 8) return Realm.CORE_FORMATION;
        if (t <= 9) return Realm.NASCENT_SOUL;
        if (t <= 10) return Realm.SOUL_TRANSFORMATION;
        return Realm.VOID_REFINEMENT;
    }

    public static Realm resolveRequiredRealm(ArtifactDataService.ArtifactDefinition artifact) {
        if (artifact == null) {
            return Realm.MORTAL;
        }
        if (artifact.realmMin() != null && !artifact.realmMin().isBlank()) {
            return Realm.fromDesignIdOrMortal(artifact.realmMin());
        }
        return realmForGameTier(artifact.gameTier());
    }

    /** 祭炼层数与器灵对倍率的小幅加成（不突破 1.15）。 */
    public static double refineBonus(ItemStack stack) {
        double bonus = 1.0D;
        int layer = ArtifactOwnershipService.refinementLayer(stack);
        bonus += Math.min(0.09D, layer * 0.01D);
        if (ArtifactOwnershipService.isSpiritAwakened(stack)) {
            bonus += 0.06D;
        }
        return Math.min(1.15D, bonus);
    }

    public static double effectiveScale(ServerPlayer player, ItemStack stack,
                                        ArtifactDataService.ArtifactDefinition artifact) {
        return Math.min(1.15D, powerScale(player, artifact) * refineBonus(stack));
    }

    private static String stripStage(String realmEquiv) {
        if (realmEquiv == null) {
            return "";
        }
        String raw = realmEquiv.trim().toUpperCase(java.util.Locale.ROOT);
        return raw
                .replace("_EARLY", "")
                .replace("_MID", "")
                .replace("_LATE", "")
                .replace("_PEAK", "");
    }
}
