package com.xunxian.seekingimmortals.cultivation;

/**
 * 神秘小瓶品阶枚举（Phase 5 预留）
 * <p>用于神秘小瓶升级系统，当前 MVP 阶段不实现</p>
 *
 * @since Phase 5 (预留于 Phase 1)
 */
public enum VialGrade {
    /**
     * 初级小瓶（5份灵液上限，24小时充能1份，蓝色灵液）
     */
    BASIC("初级小瓶", 5, 24, 0x4169E1, 10.0f),

    /**
     * 中级小瓶（10份上限，18小时充能1份，青色灵液）
     */
    INTERMEDIATE("中级小瓶", 10, 18, 0x20B2AA, 15.0f),

    /**
     * 高级小瓶（20份上限，12小时充能1份，紫色灵液）
     */
    ADVANCED("高级小瓶", 20, 12, 0x9370DB, 20.0f),

    /**
     * 完美小瓶（50份上限，6小时充能1份，金色灵液）
     */
    PERFECT("完美小瓶", 50, 6, 0xFFD700, 30.0f);

    private final String displayName;
    private final int maxCharges;
    private final int hoursPerCharge;
    private final int liquidColor;
    private final float growthSpeedMultiplier;

    VialGrade(String displayName, int maxCharges, int hoursPerCharge, int liquidColor, float growthSpeedMultiplier) {
        this.displayName = displayName;
        this.maxCharges = maxCharges;
        this.hoursPerCharge = hoursPerCharge;
        this.liquidColor = liquidColor;
        this.growthSpeedMultiplier = growthSpeedMultiplier;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * 获取灵液最大份数
     */
    public int getMaxCharges() {
        return maxCharges;
    }

    /**
     * 获取充能时间（小时）
     */
    public int getHoursPerCharge() {
        return hoursPerCharge;
    }

    /**
     * 获取灵液颜色（RGB）
     */
    public int getLiquidColor() {
        return liquidColor;
    }

    /**
     * 获取生长速度倍率
     */
    public float getGrowthSpeedMultiplier() {
        return growthSpeedMultiplier;
    }

    /**
     * 根据任务进度升级小瓶品阶
     * <p>Phase 5 实现</p>
     */
    public VialGrade upgrade() {
        return switch (this) {
            case BASIC -> INTERMEDIATE;
            case INTERMEDIATE -> ADVANCED;
            case ADVANCED -> PERFECT;
            case PERFECT -> PERFECT;
        };
    }
}
