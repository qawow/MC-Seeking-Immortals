package com.xunxian.seekingimmortals.cultivation;

/**
 * 境界属性基准表
 * <p>存储每个境界的灵力基准(manaBase)、神识基准(divSenseBase)、生命基准(hpBase)</p>
 * <p>Phase 1 MVP 实现</p>
 */
public final class RealmStageConfig {
    private static final int MORTAL_MANA_BASE = 0;
    private static final int MORTAL_STAGE_EXP_SPAN = 500;
    private static final int MORTAL_DIVINE_SENSE_BASE = 3;
    private static final int MORTAL_HP_BASE = 0;
    private static final double MORTAL_ATTACK_BASE = 1.0D;
    private static final double MORTAL_DEFENSE_BASE = 0.0D;

    private static final int[] MANA_BASE_BY_REALM = {
            1300, 4200, 18000, 90000, 520000, 3600000, 30000000, 300000000, 360000000, 600000000
    };
    private static final int[] STAGE_EXP_SPAN_BY_REALM = {
            330, 780, 2200, 6800, 24000, 105000, 560000, 3600000, 26000000, 200000000
    };
    private static final int[] DIVINE_SENSE_BASE_BY_REALM = {
            180, 540, 2400, 14000, 95000, 760000, 7200000, 80000000, 320000000, 700000000
    };
    private static final int[] HP_BASE_BY_REALM = {
            90, 230, 950, 5200, 36000, 300000, 3000000, 36000000, 300000000, 700000000
    };
    private static final float[] MANA_RECOVERY_BASE_BY_REALM = {
            6.0F, 16.0F, 80.0F, 460.0F, 3200.0F, 27000.0F, 270000.0F, 3200000.0F, 32000000.0F, 120000000.0F
    };
    private static final float[] CULTIVATION_GAIN_BASE_BY_REALM = {
            1.8F, 4.5F, 18.0F, 92.0F, 560.0F, 4300.0F, 42000.0F, 500000.0F, 5500000.0F, 65000000.0F
    };
    private static final float[] FLYING_SPEED_BASE_BY_REALM = {
            14.0F, 22.0F, 46.0F, 90.0F, 160.0F, 260.0F, 420.0F, 680.0F, 980.0F, 1400.0F
    };
    private static final double[] ATTACK_BASE_BY_REALM = {
            55.0D, 180.0D, 900.0D, 5600.0D, 42000.0D, 360000.0D, 3600000.0D, 45000000.0D, 420000000.0D, 900000000.0D
    };
    private static final double[] DEFENSE_BASE_BY_REALM = {
            36.0D, 130.0D, 700.0D, 4400.0D, 34000.0D, 300000.0D, 3100000.0D, 40000000.0D, 380000000.0D, 850000000.0D
    };
    private static final double CRIT_CHANCE_BASE = 0.08D;
    private static final double CRIT_CHANCE_REALM_GROWTH = 1.15D;
    private static final double CRIT_DAMAGE_BASE = 1.8D;
    private static final double CRIT_DAMAGE_REALM_GROWTH = 1.12D;
    private static final double DODGE_CHANCE_BASE = 0.08D;
    private static final double DODGE_CHANCE_REALM_GROWTH = 1.14D;
    private static final double ACCURACY_BASE = 0.92D;
    private static final double MISS_CHANCE_DECAY = 0.82D;
    private static final double MAGIC_RESIST_BASE = 0.10D;
    private static final double MAGIC_RESIST_REALM_GROWTH = 1.28D;

    private RealmStageConfig() {}

    public static int getRealmGrowthIndex(Realm realm) {
        return Math.max(0, realm.ordinal() - Realm.QI_REFINING.ordinal());
    }

    private static int byRealm(int[] values, Realm realm, int mortalValue) {
        if (realm == Realm.MORTAL) return mortalValue;
        return values[Math.min(values.length - 1, getRealmGrowthIndex(realm))];
    }

    private static float byRealm(float[] values, Realm realm, float mortalValue) {
        if (realm == Realm.MORTAL) return mortalValue;
        return values[Math.min(values.length - 1, getRealmGrowthIndex(realm))];
    }

    private static double byRealm(double[] values, Realm realm, double mortalValue) {
        if (realm == Realm.MORTAL) return mortalValue;
        return values[Math.min(values.length - 1, getRealmGrowthIndex(realm))];
    }

    private static double scaledDouble(double base, double growth, Realm realm, double mortalValue) {
        if (realm == Realm.MORTAL) return mortalValue;
        return base * Math.pow(growth, getRealmGrowthIndex(realm));
    }

    /**
     * 获取指定境界的灵力基准值
     * @param realm 境界
     * @return 灵力基准值（未乘阶段倍率）
     */
    public static int getManaBase(Realm realm) {
        return byRealm(MANA_BASE_BY_REALM, realm, MORTAL_MANA_BASE);
    }

    public static int getStageExpSpan(Realm realm) {
        return byRealm(STAGE_EXP_SPAN_BY_REALM, realm, MORTAL_STAGE_EXP_SPAN);
    }

    /**
     * 获取指定境界的神识基准值
     * @param realm 境界
     * @return 神识基准值
     */
    public static int getDivSenseBase(Realm realm) {
        return byRealm(DIVINE_SENSE_BASE_BY_REALM, realm, MORTAL_DIVINE_SENSE_BASE);
    }

    /**
     * 获取指定境界的生命基准值
     * @param realm 境界
     * @return 生命基准值（HP）
     */
    public static int getHpBase(Realm realm) {
        return byRealm(HP_BASE_BY_REALM, realm, MORTAL_HP_BASE);
    }

    /**
     * 获取灵力回速基准（点/秒）
     * @param realm 境界
     * @return 灵力回速基准
     */
    public static float getManaRecoveryBase(Realm realm) {
        return byRealm(MANA_RECOVERY_BASE_BY_REALM, realm, 0.0F);
    }

    /**
     * 获取修为回速基准（点/秒）
     * @param realm 境界
     * @return 修为回速基准
     */
    public static float getCultivationGainBase(Realm realm) {
        return byRealm(CULTIVATION_GAIN_BASE_BY_REALM, realm, 0.05F);
    }

    /**
     * 获取飞行速度基准（方块/秒）
     * @param realm 境界
     * @return 飞行速度基准
     */
    public static float getFlyingSpeedBase(Realm realm) {
        return byRealm(FLYING_SPEED_BASE_BY_REALM, realm, 0.0F);
    }

    public static double getAttackBase(Realm realm) {
        return byRealm(ATTACK_BASE_BY_REALM, realm, MORTAL_ATTACK_BASE);
    }

    public static double getDefenseBase(Realm realm) {
        return byRealm(DEFENSE_BASE_BY_REALM, realm, MORTAL_DEFENSE_BASE);
    }

    public static double getCritChanceBase(Realm realm) {
        return Math.min(0.75D, scaledDouble(CRIT_CHANCE_BASE, CRIT_CHANCE_REALM_GROWTH, realm, 0.0D));
    }

    public static double getCritDamageBase(Realm realm) {
        return scaledDouble(CRIT_DAMAGE_BASE, CRIT_DAMAGE_REALM_GROWTH, realm, 1.0D);
    }

    public static double getDodgeChanceBase(Realm realm) {
        return Math.min(0.50D, scaledDouble(DODGE_CHANCE_BASE, DODGE_CHANCE_REALM_GROWTH, realm, 0.0D));
    }

    public static double getAccuracyBase(Realm realm) {
        if (realm == Realm.MORTAL) return 0.85D;
        double missChance = (1.0D - ACCURACY_BASE) * Math.pow(MISS_CHANCE_DECAY, getRealmGrowthIndex(realm));
        return Math.min(0.99D, 1.0D - missChance);
    }

    public static double getMagicResistanceBase(Realm realm) {
        if (realm == Realm.MORTAL || realm == Realm.QI_REFINING) return 0.0D;
        int indexFromFoundation = Math.max(0, realm.ordinal() - Realm.FOUNDATION_ESTABLISHMENT.ordinal());
        return Math.min(0.70D, MAGIC_RESIST_BASE * Math.pow(MAGIC_RESIST_REALM_GROWTH, indexFromFoundation));
    }
}
