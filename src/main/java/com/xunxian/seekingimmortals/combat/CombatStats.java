package com.xunxian.seekingimmortals.combat;

import com.xunxian.seekingimmortals.cultivation.PlayerCultivation;
import com.xunxian.seekingimmortals.cultivation.RealmStageConfig;

public class CombatStats {
    private final double baseAttack;
    private final double baseDefense;
    private final double critChance;
    private final double critDamage;
    private final double dodgeChance;
    private final double accuracy;

    public CombatStats(PlayerCultivation cultivation) {
        this.baseAttack = cultivation.getMeleeAttackPower();
        this.baseDefense = cultivation.getDefensePower();
        this.critChance = cultivation.getCriticalRate();
        this.dodgeChance = cultivation.getDodgeRate();
        this.accuracy = cultivation.getAccuracyRate();
        var realm = cultivation.getRealm();
        this.critDamage = RealmStageConfig.getCritDamageBase(realm);
    }

    public double getBaseAttack() { return baseAttack; }
    public double getBaseDefense() { return baseDefense; }
    // 上限与 PlayerCultivation.getCriticalRate/getDodgeRate 保持一致（0.80/0.75），
    // 避免此处二次夹取静默覆盖修炼层的调参
    public double getCritChance() { return Math.min(critChance, 0.80); }
    public double getCritDamage() { return critDamage; }
    public double getDodgeChance() { return Math.min(dodgeChance, 0.75); }
    public double getAccuracy() { return Math.min(accuracy, 0.99); }
}
