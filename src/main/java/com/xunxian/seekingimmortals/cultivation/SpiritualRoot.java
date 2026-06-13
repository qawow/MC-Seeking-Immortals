package com.xunxian.seekingimmortals.cultivation;

public enum SpiritualRoot {
    HEAVENLY("天灵根", 2.60D, 2.50D, 1, 5, false, "单一纯净五行属性，修炼速度约为普通灵根的 2-3 倍，结丹瓶颈极低，适合专修本属性顶级功法"),
    HIDDEN("隐灵根", 2.80D, 2.70D, 1, 6, true, "隐藏属性，需特殊血脉、验灵秘法或机缘觉醒；暂不参与随机出生"),
    MUTATED("变异灵根", 1.85D, 1.90D, 1, 4, false, "雷、冰、风、暗等变异属性，速度接近天灵根，擅长对应变异功法"),
    DUAL("双灵根", 1.35D, 1.25D, 2, 3, false, "真灵根，两种属性，可修两系法术，筑基成功率较高"),
    TRIPLE("三灵根", 1.05D, 1.00D, 3, 2, false, "真灵根，三种属性，法术变化多但资源需求更大"),
    PSEUDO("四灵根", 0.55D, 0.55D, 4, 1, false, "伪灵根，四属性法力杂乱，筑基艰难，需补天丹或机缘改善"),
    FIVE_ELEMENTS("五灵根", 0.25D, 0.30D, 5, 0, false, "五行俱全，前期最差，筑基概率极低；后期五行合一另有潜力");

    private final String displayName;
    private final double breakthroughCoefficient;
    private final double cultivationSpeedCoefficient;
    private final int attributeCount;
    private final int starLevel;
    private final boolean hidden;
    private final String description;

    SpiritualRoot(String displayName, double breakthroughCoefficient, double cultivationSpeedCoefficient, int attributeCount, int starLevel, boolean hidden, String description) {
        this.displayName = displayName;
        this.breakthroughCoefficient = breakthroughCoefficient;
        this.cultivationSpeedCoefficient = cultivationSpeedCoefficient;
        this.attributeCount = attributeCount;
        this.starLevel = starLevel;
        this.hidden = hidden;
        this.description = description;
    }

    public String getDisplayName() { return displayName; }
    public double getTalentCoefficient() { return breakthroughCoefficient; }
    public double getBreakthroughCoefficient() { return breakthroughCoefficient; }
    public double getCultivationSpeedCoefficient() { return cultivationSpeedCoefficient; }
    public int getAttributeCount() { return attributeCount; }
    public int getStarLevel() { return starLevel; }
    public boolean isHidden() { return hidden; }
    public String getDescription() { return description; }

    public boolean hasLateGameFiveElementsPotential() {
        return this == PSEUDO || this == FIVE_ELEMENTS;
    }
}
