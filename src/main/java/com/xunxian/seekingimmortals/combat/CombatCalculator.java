package com.xunxian.seekingimmortals.combat;

import com.xunxian.seekingimmortals.cultivation.CultivationProvider;
import com.xunxian.seekingimmortals.cultivation.PlayerCultivation;
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
        Optional<CombatStats> attackerStats = getCombatStats(attacker);
        Optional<CombatStats> defenderStats = getCombatStats(defender);
        if (attackerStats.isEmpty() || defenderStats.isEmpty()) {
            return new DamageResult(Math.max(baseDamage, 0.0D), false, false, false, baseDamage, 0);
        }

        CombatStats attack = attackerStats.get();
        CombatStats defenseStats = defenderStats.get();

        // 1. 命中判定
        if (random.nextDouble() > attack.getAccuracy()) {
            return new DamageResult(0, false, false, true, baseDamage, 0);
        }

        // 2. 闪避判定
        if (random.nextDouble() < defenseStats.getDodgeChance()) {
            return new DamageResult(0, false, true, false, baseDamage, 0);
        }

        // 3. 计算原始伤害（基础伤害 + 攻击力）
        double rawDamage = baseDamage + attack.getBaseAttack();

        // 4. 暴击判定
        boolean isCrit = random.nextDouble() < attack.getCritChance();
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

        return new DamageResult(finalDamage, isCrit, false, false, rawDamage, mitigatedDamage);
    }

    /**
     * 获取玩家战斗属性
     */
    private static Optional<CombatStats> getCombatStats(ServerPlayer player) {
        return player.getCapability(CultivationProvider.CULTIVATION)
            .map(CombatStats::new);
    }

    /**
     * 显示战斗信息。实际伤害由事件中的 setAmount/cancel 负责，避免再次 hurt 造成递归。
     */
    public static void showDamageFeedback(ServerPlayer attacker, ServerPlayer defender, DamageResult result) {
        if (result.isMissed()) {
            attacker.displayClientMessage(
                Component.literal("§7未命中！"),
                true
            );
            return;
        }

        if (result.isDodged()) {
            attacker.displayClientMessage(
                Component.literal("§e" + defender.getName().getString() + " 闪避了攻击！"),
                true
            );
            defender.displayClientMessage(
                Component.literal("§a成功闪避攻击！"),
                true
            );
            return;
        }

        // 显示伤害信息
        String damageText = String.format("§c%.1f", result.getFinalDamage());
        if (result.isCrit()) {
            damageText = "§6§l暴击！ " + damageText;
        }

        attacker.displayClientMessage(
            Component.literal("对 " + defender.getName().getString() + " 造成 " + damageText + " 伤害"),
            true
        );

        defender.displayClientMessage(
            Component.literal("受到 " + damageText + " 伤害"),
            true
        );
    }
}
