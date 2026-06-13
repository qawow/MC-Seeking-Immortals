package com.xunxian.seekingimmortals.cultivation;

import net.minecraft.util.RandomSource;

public enum SpiritualRootAttribute {
    METAL("金", false, false, 1.00D, "主攻击/锐利，擅长炼器与攻击法术"),
    WOOD("木", false, false, 1.00D, "主生机/治疗，擅长炼丹与治疗法术"),
    WATER("水", false, false, 1.00D, "主柔韧/变化，擅长防御与辅助"),
    FIRE("火", false, false, 1.00D, "主爆发/毁灭，攻击力极强"),
    EARTH("土", false, false, 1.00D, "主厚重/防御，最稳定"),
    WIND("风", true, false, 1.12D, "木系变异，速度极高，极致机动"),
    THUNDER("雷", true, false, 1.20D, "土水变异，攻击与速度极强，附带麻痹和范围伤害"),
    ICE("冰", true, false, 1.15D, "金水变异，控制、防御和持续伤害优秀"),
    DARK("暗", true, false, 1.16D, "暗属性变异，擅长暗杀、诅咒与隐匿"),
    HIDDEN_THUNDER("隐雷", true, true, 1.30D, "隐灵根分支，需机缘觉醒，潜力不逊天灵根"),
    HIDDEN_DARK("隐暗", true, true, 1.28D, "隐灵根分支，需机缘觉醒，潜力不逊天灵根"),
    NONE("无属性", true, false, 0.95D, "无明显属性亲和，当前仅作兼容保留"),
    IMMORTAL("仙", true, true, 1.35D, "仙灵根预留，当前不参与随机出生");

    private final String displayName;
    private final boolean mutated;
    private final boolean hidden;
    private final double breakthroughCoefficient;
    private final String description;

    SpiritualRootAttribute(String displayName, boolean mutated, boolean hidden, double breakthroughCoefficient, String description) {
        this.displayName = displayName;
        this.mutated = mutated;
        this.hidden = hidden;
        this.breakthroughCoefficient = breakthroughCoefficient;
        this.description = description;
    }

    public String getDisplayName() { return displayName; }
    public boolean isSpecial() { return mutated || hidden; }
    public boolean isMutated() { return mutated; }
    public boolean isHidden() { return hidden; }
    public double getBreakthroughCoefficient() { return breakthroughCoefficient; }
    public int getRandomWeight() { return 0; }
    public String getDescription() { return description; }

    public static SpiritualRootAttribute randomFiveElement(RandomSource random, java.util.Set<SpiritualRootAttribute> excluded) {
        SpiritualRootAttribute[] five = {METAL, WOOD, WATER, FIRE, EARTH};
        SpiritualRootAttribute picked;
        do {
            picked = five[random.nextInt(five.length)];
        } while (excluded.contains(picked));
        return picked;
    }

    public static SpiritualRootAttribute randomMutated(RandomSource random) {
        int roll = random.nextInt(100);
        if (roll < 35) return THUNDER;
        if (roll < 60) return ICE;
        if (roll < 82) return WIND;
        return DARK;
    }

    public static SpiritualRootAttribute randomHidden(RandomSource random) {
        return random.nextBoolean() ? HIDDEN_THUNDER : HIDDEN_DARK;
    }
}
