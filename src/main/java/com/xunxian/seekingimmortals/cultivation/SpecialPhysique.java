package com.xunxian.seekingimmortals.cultivation;

import net.minecraft.util.RandomSource;

public enum SpecialPhysique {
    NONE("无特殊体质", 1.0D, 1.0D, false, "无额外体质"),
    HIDDEN_THUNDER_ROOT("隐雷灵根", 1.15D, 1.10D, false, "隐性雷灵根，可辅助抵挡雷劫"),
    DRAGON_CHANT_BODY("龙吟之体", 0.70D, 0.80D, true, "阴阳失衡，前期危险"),
    JADE_PHOENIX_MARROW("通玉凤髓之身", 1.05D, 1.05D, false, "筑基后产生通灵之气"),
    ICE_MARROW_BODY("冰髓之体", 1.30D, 1.20D, true, "寒毒体质，适合冰髓寒魄神通"),
    GOLD_FORGING_BODY("锻金之体", 1.10D, 1.15D, false, "金属性亲和"),
    MOLTEN_GOLD_BODY("熔金之体", 1.05D, 1.25D, false, "适合炼体术"),
    FIVE_THUNDER_BODY("五雷之体", 1.35D, 1.25D, false, "可操纵五种雷电之力"),
    NINE_SPIRIT_SWORD_BODY("九灵剑体", 1.25D, 1.35D, false, "剑修极致体质"),
    SEVEN_STAR_MOON_BODY("七星月体", 1.25D, 1.10D, false, "合体后进阶大乘概率更高"),
    CHASTE_YIN_BODY("姹女素阴体", 1.10D, 1.20D, false, "魔功修炼事半功倍"),
    HEAVENLY_YIN_BODY("天阴之体", 1.20D, 1.10D, false, "可助抵挡天劫"),
    THREE_YANG_BODY("三阳之体", 1.10D, 1.15D, false, "阳性体质"),
    CHARMING_BODY("天生媚体", 1.05D, 1.10D, false, "魅惑亲和"),
    MYSTIC_YIN_BODY("玄阴差女体", 1.15D, 1.15D, false, "玄阴分支体质");

    private final String displayName;
    private final double breakthroughMultiplier;
    private final double cultivationMultiplier;
    private final boolean hasDefect;
    private final String description;

    SpecialPhysique(String displayName, double breakthroughMultiplier, double cultivationMultiplier, boolean hasDefect, String description) {
        this.displayName = displayName;
        this.breakthroughMultiplier = breakthroughMultiplier;
        this.cultivationMultiplier = cultivationMultiplier;
        this.hasDefect = hasDefect;
        this.description = description;
    }

    public String getDisplayName() { return displayName; }
    public double getBreakthroughMultiplier() { return breakthroughMultiplier; }
    public double getCultivationMultiplier() { return cultivationMultiplier; }
    public boolean hasDefect() { return hasDefect; }
    public String getDescription() { return description; }

    public static SpecialPhysique random(RandomSource random) {
        // 约 2% 概率获得特殊体质，其中大多数体质概率相同；可后续数据化。
        if (random.nextInt(1000) >= 20) return NONE;
        SpecialPhysique[] values = values();
        return values[1 + random.nextInt(values.length - 1)];
    }
}
