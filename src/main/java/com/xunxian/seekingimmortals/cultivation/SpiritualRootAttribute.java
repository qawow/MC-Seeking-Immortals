package com.xunxian.seekingimmortals.cultivation;

import net.minecraft.util.RandomSource;

import java.util.Locale;

/**
 * 灵根属性枚举（五行 + 语料 10 元素 + 变异/隐藏分支）。
 *
 * <p>语料 {@code spirit_roots_catalog.elements}：
 * metal/wood/water/fire/earth/wind/thunder/ice/yin/yang。</p>
 *
 * <p>属性对伤害/术法亲和的强度由灵根分类
 * ({@link SpiritualRoot#getAttributeStrengthMultiplier()}) 决定。</p>
 */
public enum SpiritualRootAttribute {
    METAL("金", "metal", false, false, 1.00D, "主攻击/锐利，擅长炼器与攻击法术"),
    WOOD("木", "wood", false, false, 1.00D, "主生机/治疗，擅长炼丹与治疗法术"),
    WATER("水", "water", false, false, 1.00D, "主柔韧/变化，擅长防御与辅助"),
    FIRE("火", "fire", false, false, 1.00D, "主爆发/毁灭，攻击力极强"),
    EARTH("土", "earth", false, false, 1.00D, "主厚重/防御，最稳定"),
    WIND("风", "wind", true, false, 1.12D, "木系变异，速度极高，极致机动"),
    THUNDER("雷", "thunder", true, false, 1.20D, "土水变异，攻击与速度极强，附带麻痹和范围伤害"),
    ICE("冰", "ice", true, false, 1.15D, "金水变异，控制、防御和持续伤害优秀"),
    YIN("阴", "yin", true, false, 1.18D, "阴属性，鬼修/玄阴亲和"),
    YANG("阳", "yang", true, false, 1.18D, "阳属性，纯阳功法亲和"),
    DARK("暗", "dark", true, false, 1.16D, "暗属性变异，擅长暗杀、诅咒与隐匿"),
    HIDDEN_THUNDER("隐雷", "hidden_thunder", true, true, 1.30D, "隐灵根分支，需机缘觉醒，潜力不逊天灵根"),
    HIDDEN_DARK("隐暗", "hidden_dark", true, true, 1.28D, "隐灵根分支，需机缘觉醒，潜力不逊天灵根"),
    NONE("无属性", "none", true, false, 0.95D, "无明显属性亲和，当前仅作兼容保留"),
    IMMORTAL("仙", "immortal", true, true, 1.35D, "仙灵根预留，当前不参与随机出生");

    private final String displayName;
    private final String corpusId;
    private final boolean mutated;
    private final boolean hidden;
    private final double breakthroughCoefficient;
    private final String description;

    SpiritualRootAttribute(String displayName, String corpusId, boolean mutated, boolean hidden,
                           double breakthroughCoefficient, String description) {
        this.displayName = displayName;
        this.corpusId = corpusId;
        this.mutated = mutated;
        this.hidden = hidden;
        this.breakthroughCoefficient = breakthroughCoefficient;
        this.description = description;
    }

    public String getDisplayName() { return displayName; }
    public String getCorpusId() { return corpusId; }
    public boolean isSpecial() { return mutated || hidden; }
    public boolean isMutated() { return mutated; }
    public boolean isHidden() { return hidden; }
    public double getBreakthroughCoefficient() { return breakthroughCoefficient; }
    public int getRandomWeight() { return 0; }
    public String getDescription() { return description; }

    public static SpiritualRootAttribute fromCorpusId(String id) {
        if (id == null || id.isBlank()) return NONE;
        String key = id.trim().toLowerCase(Locale.ROOT).replace('-', '_');
        for (SpiritualRootAttribute attribute : values()) {
            if (attribute.corpusId.equals(key) || attribute.name().equalsIgnoreCase(key)) {
                return attribute;
            }
        }
        return switch (key) {
            case "jin", "gold" -> METAL;
            case "mu" -> WOOD;
            case "shui" -> WATER;
            case "huo" -> FIRE;
            case "tu" -> EARTH;
            case "feng" -> WIND;
            case "lei" -> THUNDER;
            case "bing" -> ICE;
            default -> NONE;
        };
    }

    public static SpiritualRootAttribute randomFiveElement(RandomSource random, java.util.Set<SpiritualRootAttribute> excluded) {
        SpiritualRootAttribute[] five = {METAL, WOOD, WATER, FIRE, EARTH};
        SpiritualRootAttribute picked;
        do {
            picked = five[random.nextInt(five.length)];
        } while (excluded.contains(picked));
        return picked;
    }

    public static SpiritualRootAttribute randomMutated(RandomSource random) {
        // 语料变异示例：ice/thunder/wind/yin/yang；保留 DARK 作为旧兼容分支。
        int roll = random.nextInt(100);
        if (roll < 22) return THUNDER;
        if (roll < 40) return ICE;
        if (roll < 55) return WIND;
        if (roll < 70) return YIN;
        if (roll < 85) return YANG;
        return DARK;
    }

    public static SpiritualRootAttribute randomHidden(RandomSource random) {
        return random.nextBoolean() ? HIDDEN_THUNDER : HIDDEN_DARK;
    }
}
