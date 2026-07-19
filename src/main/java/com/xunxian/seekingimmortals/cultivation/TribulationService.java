package com.xunxian.seekingimmortals.cultivation;

import com.xunxian.seekingimmortals.network.SyncCultivationDataPacket;
import com.xunxian.seekingimmortals.item.CatalogConsumableService;
import com.xunxian.seekingimmortals.spiritual.SpiritualAuraManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.phys.Vec3;

public final class TribulationService {
    private static final int INITIAL_DELAY_TICKS = 6 * 20;
    private static final int STRIKE_INTERVAL_TICKS = 5 * 20;
    private static final double MAX_TOTAL_REDUCTION = 0.95D;

    private TribulationService() {}

    public static boolean onBreakthroughSuccess(ServerPlayer player, PlayerCultivation cultivation, PlayerCultivation.BreakthroughAttemptResult result) {
        if (!shouldTriggerAfterBreakthrough(result.oldRealm(), result.newRealm())) {
            return false;
        }
        return start(player, cultivation, result.newRealm(), false);
    }

    public static boolean debugStart(ServerPlayer player, PlayerCultivation cultivation, Realm targetRealm) {
        return start(player, cultivation, targetRealm, true);
    }

    public static void tick(ServerPlayer player, PlayerCultivation cultivation) {
        if (!cultivation.isTribulationActive()) return;
        if (player.isDeadOrDying()) return;
        if (!cultivation.tickTribulationCountdown()) return;

        Realm targetRealm = cultivation.getTribulationTargetRealm();
        int strikeNumber = cultivation.getTribulationCurrentStrike() + 1;
        int totalStrikes = cultivation.getTribulationTotalStrikes();
        SpiritualAuraManager.AuraInfo auraInfo = SpiritualAuraManager.getAuraInfo(player.level(), player.blockPosition());
        double reduction = calculateDamageReductionPercent(
                cultivation.getTribRes(),
                cultivation.getBodyRef(),
                cultivation.getDivSense(),
                targetRealm,
                auraInfo.leylineMultiplier(),
                auraInfo.formationBonus(),
                cultivation.getTribulationDamageReductionBonus());
        double damage = calculateStrikeDamage(
                Math.max(20.0D, player.getMaxHealth()),
                targetRealm,
                strikeNumber,
                totalStrikes,
                cultivation.getTribRes(),
                cultivation.getBodyRef(),
                cultivation.getDivSense(),
                auraInfo.leylineMultiplier(),
                auraInfo.formationBonus(),
                cultivation.getTribulationDamageReductionBonus());

        spawnStrikeVisual(player);
        player.displayClientMessage(Component.translatable("message.seeking_immortals.tribulation.strike",
                strikeNumber,
                totalStrikes,
                Math.round(damage * 10.0D) / 10.0D,
                percent(reduction)), false);
        CatalogConsumableService.markTribulationDamage(player);
        boolean damageAccepted;
        try {
            damageAccepted = player.hurt(player.damageSources().magic(), (float)Math.min(Float.MAX_VALUE, damage));
        } finally {
            CatalogConsumableService.clearTribulationDamage(player);
        }
        if (!damageAccepted) {
            cultivation.scheduleTribulationRetry(20);
            SyncCultivationDataPacket.send(player, cultivation);
            return;
        }
        applyDivineSenseInstability(player, cultivation, targetRealm);
        cultivation.recordTribulationStrike(STRIKE_INTERVAL_TICKS);

        if (cultivation.isTribulationActive() && !player.isDeadOrDying() && cultivation.isTribulationComplete()) {
            completeSuccess(player, cultivation);
        } else if (cultivation.isTribulationActive()) {
            SyncCultivationDataPacket.send(player, cultivation);
        }
    }

    public static void handleDeath(ServerPlayer player, PlayerCultivation cultivation) {
        if (!cultivation.isTribulationActive()) return;
        fail(player, cultivation, "message.seeking_immortals.tribulation.failure.death");
    }

    public static void handleDimensionChange(ServerPlayer player, PlayerCultivation cultivation) {
        if (!cultivation.isTribulationActive()) return;
        fail(player, cultivation, "message.seeking_immortals.tribulation.failure.dimension");
    }

    public static boolean shouldTriggerAfterBreakthrough(Realm oldRealm, Realm newRealm) {
        return oldRealm != newRealm && newRealm.ordinal() >= Realm.CORE_FORMATION.ordinal();
    }

    /**
     * Strike counts from {@link TribulationRulesCatalog} (text_material/tribulation_rules.json).
     * Fallback: NS3 / ST5 / VR9 / BI12 / GV18 / TL27.
     */
    public static int getStrikeCount(Realm targetRealm) {
        return TribulationRulesCatalog.strikeCount(targetRealm);
    }

    public static double calculateStrikeDamage(double maxHealth, Realm targetRealm, int strikeNumber, int totalStrikes,
                                               int tribulationResistance, int bodyRefinement, int divineSense,
                                               double leylineMultiplier, int formationBonus) {
        double waveProgress = totalStrikes <= 1 ? 0.0D : (strikeNumber - 1.0D) / (totalStrikes - 1.0D);
        double wavePressure = 0.90D + waveProgress * 0.25D;
        double rawDamage = Math.max(20.0D, maxHealth) * getTargetDamageMultiplier(targetRealm) * wavePressure;
        double reduction = calculateDamageReductionPercent(tribulationResistance, bodyRefinement, divineSense,
                targetRealm, leylineMultiplier, formationBonus);
        return Math.max(1.0D, rawDamage * (1.0D - reduction));
    }

    public static double calculateStrikeDamage(double maxHealth, Realm targetRealm, int strikeNumber, int totalStrikes,
                                               int tribulationResistance, int bodyRefinement, int divineSense,
                                               double leylineMultiplier, int formationBonus, double physiqueReductionBonus) {
        double waveProgress = totalStrikes <= 1 ? 0.0D : (strikeNumber - 1.0D) / (totalStrikes - 1.0D);
        double wavePressure = 0.90D + waveProgress * 0.25D;
        double rawDamage = Math.max(20.0D, maxHealth) * getTargetDamageMultiplier(targetRealm) * wavePressure;
        double reduction = calculateDamageReductionPercent(tribulationResistance, bodyRefinement, divineSense,
                targetRealm, leylineMultiplier, formationBonus, physiqueReductionBonus);
        return Math.max(1.0D, rawDamage * (1.0D - reduction));
    }

    public static double calculateDamageReductionPercent(int tribulationResistance, int bodyRefinement, int divineSense,
                                                         Realm targetRealm, double leylineMultiplier, int formationBonus) {
        double tribulationReduction = clamp01(tribulationResistance / 100.0D);
        double bodyScale = 250.0D + RealmStageConfig.getRealmGrowthIndex(targetRealm) * 350.0D;
        double bodyReduction = 0.24D * (1.0D - Math.exp(-Math.max(0, bodyRefinement) / bodyScale));
        int divineSenseRequirement = getTargetDivineSenseRequirement(targetRealm);
        double divineSenseRatio = divineSenseRequirement <= 0 ? 1.0D : Math.max(0.0D, divineSense / (double)divineSenseRequirement);
        double divineSenseReduction = divineSenseRatio <= 1.0D ? 0.0D : Math.min(0.18D, Math.log1p(divineSenseRatio - 1.0D) * 0.10D);
        double leylineReduction = Math.min(0.12D, Math.max(0.0D, leylineMultiplier - 1.0D) * 0.04D);
        double formationReduction = Math.min(0.08D, Math.max(0, formationBonus) / 1000.0D);
        return Math.min(MAX_TOTAL_REDUCTION, tribulationReduction + bodyReduction + divineSenseReduction + leylineReduction + formationReduction);
    }

    public static double calculateDamageReductionPercent(int tribulationResistance, int bodyRefinement, int divineSense,
                                                         Realm targetRealm, double leylineMultiplier, int formationBonus,
                                                         double physiqueReductionBonus) {
        return Math.min(MAX_TOTAL_REDUCTION, calculateDamageReductionPercent(tribulationResistance, bodyRefinement,
                divineSense, targetRealm, leylineMultiplier, formationBonus) + Math.max(0.0D, physiqueReductionBonus));
    }

    public static int getTargetDivineSenseRequirement(Realm targetRealm) {
        return RealmStageConfig.getDivSenseBase(targetRealm);
    }

    public static int getSuccessResistanceReward(Realm targetRealm) {
        if (targetRealm.ordinal() < Realm.CORE_FORMATION.ordinal()) return 0;
        return Math.min(15, 3 + (targetRealm.ordinal() - Realm.CORE_FORMATION.ordinal() + 1) * 2);
    }

    private static boolean start(ServerPlayer player, PlayerCultivation cultivation, Realm targetRealm, boolean debug) {
        int strikeCount = getStrikeCount(targetRealm);
        if (strikeCount <= 0) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.tribulation.no_target", targetRealm.getDisplayName()), false);
            return false;
        }
        if (cultivation.isTribulationActive()) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.tribulation.already_active",
                    cultivation.getTribulationTargetRealm().getDisplayName(),
                    cultivation.getTribulationCurrentStrike(),
                    cultivation.getTribulationTotalStrikes()), false);
            return true;
        }
        cultivation.startTribulation(targetRealm, strikeCount, debug ? 20 : INITIAL_DELAY_TICKS);
        player.displayClientMessage(Component.translatable(debug
                ? "message.seeking_immortals.tribulation.debug_started"
                : "message.seeking_immortals.tribulation.breakthrough_triggered",
                targetRealm.getDisplayName(), strikeCount), false);
        SyncCultivationDataPacket.send(player, cultivation);
        return true;
    }

    private static void completeSuccess(ServerPlayer player, PlayerCultivation cultivation) {
        Realm targetRealm = cultivation.getTribulationTargetRealm();
        int reward = getSuccessResistanceReward(targetRealm);
        cultivation.clearTribulation();
        cultivation.addTribulationResistance(reward);
        // Wave43: durable flag for spatial/event requires (tribulation_success).
        player.getPersistentData().putBoolean("seeking_immortals_tribulation_success", true);
        player.getPersistentData().putString("seeking_immortals_last_tribulation_realm",
                targetRealm == null ? "" : targetRealm.name());
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 15 * 20, 1, false, false));
        player.displayClientMessage(Component.translatable("message.seeking_immortals.tribulation.success",
                targetRealm.getDisplayName(), reward, cultivation.getTribRes()), false);
        SyncCultivationDataPacket.send(player, cultivation);
    }

    public static boolean hasPassedTribulation(ServerPlayer player) {
        return player != null && player.getPersistentData().getBoolean("seeking_immortals_tribulation_success");
    }

    private static void fail(ServerPlayer player, PlayerCultivation cultivation, String messageKey) {
        Realm targetRealm = cultivation.getTribulationTargetRealm();
        int bodyLoss = cultivation.failTribulationPenalty();
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30 * 20, 2, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60 * 20, 2, false, false));
        player.displayClientMessage(Component.translatable(messageKey,
                targetRealm.getDisplayName(),
                cultivation.getQiDevRisk(),
                bodyLoss,
                cultivation.getRealm().getDisplayName(),
                cultivation.getStage().getDisplayName()), false);
        SyncCultivationDataPacket.send(player, cultivation);
    }

    private static void applyDivineSenseInstability(ServerPlayer player, PlayerCultivation cultivation, Realm targetRealm) {
        int requirement = getTargetDivineSenseRequirement(targetRealm);
        if (requirement <= 0 || cultivation.getDivSense() >= requirement) return;
        double shortage = 1.0D - cultivation.getDivSense() / (double)requirement;
        int risk = Math.max(1, (int)Math.ceil(shortage * 5.0D));
        cultivation.addQiDeviationRisk(risk);
        player.displayClientMessage(Component.translatable("message.seeking_immortals.tribulation.unstable_divine_sense",
                requirement, cultivation.getDivSense(), risk, cultivation.getQiDevRisk()), true);
    }

    private static void spawnStrikeVisual(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        BlockPos pos = player.blockPosition();
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt != null) {
            bolt.moveTo(Vec3.atBottomCenterOf(pos));
            bolt.setCause(player);
            bolt.setVisualOnly(true);
            level.addFreshEntity(bolt);
        }
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                player.getX(), player.getY() + 1.0D, player.getZ(),
                48, 0.6D, 0.9D, 0.6D, 0.08D);
        level.playSound(null, pos, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 1.2F, 0.9F);
        level.playSound(null, pos, SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.WEATHER, 0.9F, 1.1F);
    }

    private static double getTargetDamageMultiplier(Realm targetRealm) {
        // 优先用语料 damage_per_wave_base 归一化到 maxHealth 倍率；无数据时回退旧表。
        double base = TribulationRulesCatalog.damagePerWaveBase(targetRealm);
        if (base > 0.0D) {
            // 语料基值约 40~300；映射到 0.26~0.90 的 maxHealth 倍率区间。
            return Math.min(0.90D, Math.max(0.20D, base / 250.0D));
        }
        return switch (targetRealm) {
            case CORE_FORMATION -> 0.26D;
            case NASCENT_SOUL -> 0.32D;
            case SOUL_TRANSFORMATION -> 0.38D;
            case VOID_REFINEMENT -> 0.45D;
            case UNITY -> 0.52D;
            case MAHAYANA -> 0.60D;
            case TRIBULATION -> 0.70D;
            case TRUE_IMMORTAL -> 0.82D;
            default -> 0.0D;
        };
    }

    private static double clamp01(double value) {
        return Math.max(0.0D, Math.min(1.0D, value));
    }

    private static int percent(double value) {
        return (int)Math.round(value * 100.0D);
    }
}
