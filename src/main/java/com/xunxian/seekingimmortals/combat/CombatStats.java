package com.xunxian.seekingimmortals.combat;

import com.xunxian.seekingimmortals.cultivation.PlayerCultivation;
import com.xunxian.seekingimmortals.cultivation.Realm;

public class CombatStats {
    // 基础属性
    private final double baseAttack;
    private final double baseDefense;
    private final double critChance;
    private final double critDamage;
    private final double dodgeChance;
    private final double accuracy;

    public CombatStats(PlayerCultivation cultivation) {
        Realm realm = cultivation.getRealm();
        int realmOrdinal = realm.ordinal();

        // 基础攻击：境界 × 10
        this.baseAttack = (realmOrdinal + 1) * 10.0;

        // 基础防御：境界 × 5
        this.baseDefense = (realmOrdinal + 1) * 5.0;

        // 暴击率：5% + 境界 × 1%
        this.critChance = 0.05 + realmOrdinal * 0.01;

        // 暴击伤害：150% + 境界 × 10%
        this.critDamage = 1.5 + realmOrdinal * 0.1;

        // 闪避率：5% + 境界 × 0.5%
        this.dodgeChance = 0.05 + realmOrdinal * 0.005;

        // 命中率：90% + 境界 × 1%
        this.accuracy = 0.90 + realmOrdinal * 0.01;
    }

    public double getBaseAttack() { return baseAttack; }
    public double getBaseDefense() { return baseDefense; }
    public double getCritChance() { return Math.min(critChance, 0.75); }
    public double getCritDamage() { return critDamage; }
    public double getDodgeChance() { return Math.min(dodgeChance, 0.50); }
    public double getAccuracy() { return Math.min(accuracy, 0.99); }
}
