package com.xunxian.seekingimmortals.cultivation;

/**
 * 灵根分类枚举。
 * 六大分类：天灵根、异灵根(隐/变异)、双灵根、三灵根、伪灵根、杂灵根。
 * HIDDEN 和 MUTATED 归入"异灵根"大类，使用异灵根加成数值。
 */
public enum SpiritualRoot {
    // 天灵根：修炼速度 ×5.0，灵力回复 ×2.0，突破 +25%
    HEAVENLY("天灵根", "天灵根", 5.0D, 2.0D, 0.25D, 1.0D, 0.0D, 1, 5, false,
            "单一纯净五行属性，修炼速度极快，突破成功率极高"),
    // 异灵根(隐)：隐灵根归入异灵根分类
    HIDDEN("隐灵根", "异灵根", 4.0D, 1.8D, 0.18D, 1.0D, 0.0D, 1, 6, true,
            "隐藏属性，需特殊血脉或机缘觉醒；归入异灵根分类"),
    // 异灵根(变异)：变异灵根归入异灵根分类
    MUTATED("变异灵根", "异灵根", 4.0D, 1.8D, 0.18D, 1.0D, 0.0D, 1, 4, false,
            "雷、冰、风、暗等变异属性，归入异灵根分类"),
    // 双灵根：修炼速度 ×3.0，灵力回复 ×1.5，突破 +10%
    DUAL("双灵根", "双灵根", 3.0D, 1.5D, 0.10D, 1.0D, 0.0D, 2, 3, false,
            "两种属性，可修两系法术，修炼速度与突破较均衡"),
    // 三灵根：修炼速度 ×2.0，灵力回复 ×1.2，突破无加成
    TRIPLE("三灵根", "三灵根", 2.0D, 1.2D, 0.0D, 1.0D, 0.0D, 3, 2, false,
            "三种属性，法术变化多但修炼速度一般"),
    // 伪灵根：修炼速度 ×1.0，灵力回复 ×1.0，突破 -10%，丹药吸收 ×1.15
    FALSE_ROOT("伪灵根", "伪灵根", 1.0D, 1.0D, -0.10D, 1.15D, 0.05D, 4, 1, false,
            "四属性法力杂乱，修炼缓慢，但丹药吸收率较高，更易获得青玉小瓶"),
    // 杂灵根：修炼速度 ×0.8，灵力回复 ×0.8，突破 -18%，丹药吸收 ×1.25
    MIXED("杂灵根", "杂灵根", 0.8D, 0.8D, -0.18D, 1.25D, 0.08D, 5, 0, false,
            "五行俱全，前期最差，但丹药吸收率最高，青玉小瓶获取率最高");

    private final String displayName;
    private final String categoryName;
    private final double cultivationSpeedMultiplier;
    private final double qiRecoveryMultiplier;
    private final double breakthroughBonus;        // 加法加成（正数为加成，负数为惩罚）
    private final double pillAbsorptionMultiplier;  // 丹药吸收率倍数
    private final double jadeVialDropChance;        // 青玉小瓶额外获取概率（预留）
    private final int attributeCount;
    private final int starLevel;
    private final boolean hidden;
    private final String description;

    SpiritualRoot(String displayName, String categoryName,
                  double cultivationSpeedMultiplier, double qiRecoveryMultiplier,
                  double breakthroughBonus, double pillAbsorptionMultiplier,
                  double jadeVialDropChance,
                  int attributeCount, int starLevel, boolean hidden, String description) {
        this.displayName = displayName;
        this.categoryName = categoryName;
        this.cultivationSpeedMultiplier = cultivationSpeedMultiplier;
        this.qiRecoveryMultiplier = qiRecoveryMultiplier;
        this.breakthroughBonus = breakthroughBonus;
        this.pillAbsorptionMultiplier = pillAbsorptionMultiplier;
        this.jadeVialDropChance = jadeVialDropChance;
        this.attributeCount = attributeCount;
        this.starLevel = starLevel;
        this.hidden = hidden;
        this.description = description;
    }

    public String getDisplayName() { return displayName; }
    /** 返回灵根所属大分类名（天灵根、异灵根、双灵根、三灵根、伪灵根、杂灵根） */
    public String getCategoryName() { return categoryName; }
    /** 修炼速度倍率（天灵根 5.0，杂灵根 0.8） */
    public double getCultivationSpeedCoefficient() { return cultivationSpeedMultiplier; }
    /** 灵力/法力回复倍率 */
    public double getQiRecoveryMultiplier() { return qiRecoveryMultiplier; }
    /** 突破成功率加法加成（+0.25 表示 +25%） */
    public double getBreakthroughBonus() { return breakthroughBonus; }
    /**
     * 旧版兼容：返回突破乘法系数（保留给可能的遗留调用）。
     * 新逻辑请使用 getBreakthroughBonus()。
     */
    public double getBreakthroughCoefficient() { return 1.0D + breakthroughBonus; }
    public double getTalentCoefficient() { return getBreakthroughCoefficient(); }
    /** 丹药效果吸收倍率（伪灵根 1.15，杂灵根 1.25） */
    public double getPillAbsorptionMultiplier() { return pillAbsorptionMultiplier; }
    /** 青玉小瓶额外获取概率（预留接口） */
    public double getJadeVialDropChance() { return jadeVialDropChance; }
    public int getAttributeCount() { return attributeCount; }
    public int getStarLevel() { return starLevel; }
    public boolean isHidden() { return hidden; }
    public String getDescription() { return description; }
    /** 是否为异灵根大类（包含隐灵根和变异灵根） */
    public boolean isVariantCategory() { return this == HIDDEN || this == MUTATED; }
    /** 是否为低资质灵根（伪灵根或杂灵根），更依赖丹药与机缘 */
    public boolean isLowTalent() { return this == FALSE_ROOT || this == MIXED; }
    /** 旧版兼容：五行合一潜力 */
    public boolean hasLateGameFiveElementsPotential() { return this == FALSE_ROOT || this == MIXED; }

    /**
     * 根据灵根分类返回属性强度倍数（简化纯度后，替代原纯度计算）。
     * 天灵根/异灵根属性强度最高，杂灵根最低。
     */
    public double getAttributeStrengthMultiplier() {
        return switch (this) {
            case HEAVENLY -> 1.0D;
            case HIDDEN, MUTATED -> 0.90D;
            case DUAL -> 0.75D;
            case TRIPLE -> 0.60D;
            case FALSE_ROOT -> 0.40D;
            case MIXED -> 0.25D;
        };
    }

    /** 从字符串安全解析枚举，支持旧版枚举名兼容 */
    public static SpiritualRoot fromName(String name) {
        if (name == null || name.isBlank()) return TRIPLE;
        // 旧版枚举名映射
        if ("PSEUDO".equals(name)) return FALSE_ROOT;
        if ("FIVE_ELEMENTS".equals(name)) return MIXED;
        try {
            return valueOf(name);
        } catch (IllegalArgumentException e) {
            return TRIPLE;
        }
    }
}
