package com.xunxian.seekingimmortals.combat;

import com.xunxian.seekingimmortals.combat.status.StatusRegistry;
import com.xunxian.seekingimmortals.cultivation.CultivationProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;

import java.util.Optional;

public class CombatCalculator {

    /**
     * 计算伤害结果
     * @param attacker 攻击者
     * @param defender 防御者
     * @param baseDamage 基础伤害
     * @param random 随机源
     * @return 伤害结果
     */
    public static DamageResult calculateDamage(ServerPlayer attacker, ServerPlayer defender,
                                              double baseDamage, RandomSource random) {
        return calculateDamage(attacker, defender, baseDamage, random, true);
    }

    public static DamageResult calculateDamage(ServerPlayer attacker, ServerPlayer defender,
                                              double baseDamage, RandomSource random,
                                              boolean includeCultivationAttack) {
        if (attacker == null || defender == null) {
            return new DamageResult(Math.max(baseDamage, 0.0D), false, false, false, baseDamage, 0);
        }
        RandomSource rng = random != null ? random : attacker.getRandom();

        // 前置钩子（M15/M07）；禁止回调内 hurt 递归
        DamagePipelineHooks.DamageContext pre =
                DamagePipelineHooks.firePre(attacker, defender, Math.max(baseDamage, 0.0D));
        if (pre.isCanceled()) {
            DamageResult canceled = new DamageResult(0, false, false, true, baseDamage, 0);
            pre.setMissed(true);
            DamagePipelineHooks.firePost(pre);
            return canceled;
        }
        double pipelineBase = pre.amount();

        Optional<CombatStats> attackerStats = getCombatStats(attacker);
        Optional<CombatStats> defenderStats = getCombatStats(defender);
        // 风险③：能力缺失时安全回退，不 NPE
        if (attackerStats.isEmpty() || defenderStats.isEmpty()) {
            DamageResult fallback = new DamageResult(Math.max(pipelineBase, 0.0D), false, false, false, pipelineBase, 0);
            pre.setAmount(fallback.getFinalDamage());
            DamagePipelineHooks.firePost(pre);
            return fallback;
        }

        CombatStats attack = attackerStats.get();
        CombatStats defenseStats = defenderStats.get();

        // 1. 命中判定
        double accuracy = adjustedAccuracy(attack.getAccuracy(), StatusRegistry.accuracyDelta(attacker));
        if (rng.nextDouble() > accuracy) {
            DamageResult miss = new DamageResult(0, false, false, true, pipelineBase, 0);
            pre.setMissed(true);
            pre.setAmount(0);
            DamagePipelineHooks.firePost(pre);
            return miss;
        }

        // 2. 闪避判定
        if (rng.nextDouble() < defenseStats.getDodgeChance()) {
            DamageResult dodge = new DamageResult(0, false, true, false, pipelineBase, 0);
            pre.setDodged(true);
            pre.setAmount(0);
            DamagePipelineHooks.firePost(pre);
            return dodge;
        }

        // 3. 计算原始伤害（基础伤害 + 攻击力）
        double rawDamage = Math.max(pipelineBase, 0.0D) + (includeCultivationAttack ? attack.getBaseAttack() : 0.0D);

        // 4. 暴击判定
        boolean isCrit = rng.nextDouble() < attack.getCritChance();
        if (isCrit) {
            rawDamage *= attack.getCritDamage();
        }

        // 5. 防御减免计算：减免 = 防御值 / (防御值 + 100)
        double defense = defenseStats.getBaseDefense();
        double damageReduction = defense / (defense + 100.0);
        double mitigatedDamage = rawDamage * damageReduction;
        double finalDamage = rawDamage - mitigatedDamage;

        // 6. 最小伤害保证
        finalDamage = Math.max(finalDamage, 1.0);

        DamageResult result = new DamageResult(finalDamage, isCrit, false, false, rawDamage, mitigatedDamage);
        pre.setAmount(finalDamage);
        pre.setCrit(isCrit);
        DamagePipelineHooks.firePost(pre);
        return result;
    }

    /**
     * 获取玩家战斗属性。能力缺失返回 empty，调用方必须判空。
     */
    public static Optional<CombatStats> getCombatStats(ServerPlayer player) {
        if (player == null) {
            return Optional.empty();
        }
        return player.getCapability(CultivationProvider.CULTIVATION)
            .map(CombatStats::new);
    }

    static double adjustedAccuracy(double baseAccuracy, double statusDelta) {
        return Math.max(0.05D, Math.min(0.99D, baseAccuracy + statusDelta));
    }

    /**
     * 显示战斗信息。实际伤害由事件中的 setAmount/cancel 负责，避免再次 hurt 造成递归。
     */
    public static void showDamageFeedback(ServerPlayer attacker, ServerPlayer defender, DamageResult result) {
        if (attacker == null || defender == null || result == null) {
            return;
        }
        if (result.isMissed()) {
            attacker.displayClientMessage(
                Component.translatable("message.seeking_immortals.combat.miss"),
                true
            );
            return;
        }

        if (result.isDodged()) {
            attacker.displayClientMessage(
                Component.translatable("message.seeking_immortals.combat.dodged_by", defender.getName()),
                true
            );
            defender.displayClientMessage(
                Component.translatable("message.seeking_immortals.combat.dodged"),
                true
            );
            return;
        }

        // 显示伤害信息（伤害数值与暴击前缀作为嵌套组件，由客户端按玩家语言解析）
        Component damageComponent = Component.literal(String.format("§c%.1f", result.getFinalDamage()));
        if (result.isCrit()) {
            damageComponent = Component.translatable("message.seeking_immortals.combat.crit", damageComponent);
        }

        attacker.displayClientMessage(
            Component.translatable("message.seeking_immortals.combat.dealt", defender.getName(), damageComponent),
            true
        );

        defender.displayClientMessage(
            Component.translatable("message.seeking_immortals.combat.received", damageComponent),
            true
        );
    }
}
