package com.xunxian.seekingimmortals.cultivation;

import net.minecraft.util.RandomSource;

import java.util.Locale;
import java.util.Optional;

/**
 * 特殊体质枚举（历史运行时模型）。
 * <p>M01 起优先使用 {@link #toConstitutionId(SpecialPhysique)} 映射到语料
 * {@code constitution_catalog.json} id；新体质以数据驱动 id 存于
 * {@link PlayerCultivation#getConstitutionId()}。</p>
 */
public enum SpecialPhysique {
    NONE("无特殊体质", "none", 1.0D, 1.0D, false, "无额外体质"),
    HIDDEN_THUNDER_ROOT("隐雷灵根", "fire_spirit_root_variant", 1.15D, 1.10D, false, "隐性雷灵根，可辅助抵挡雷劫"),
    DRAGON_CHANT_BODY("龙吟之体", "dragon_chant", 0.70D, 0.80D, true, "阴阳失衡，前期危险"),
    JADE_PHOENIX_MARROW("通玉凤髓之身", "tongyu_fengsui", 1.05D, 1.05D, false, "筑基后产生通灵之气"),
    ICE_MARROW_BODY("冰髓之体", "spirit_body_mild", 1.30D, 1.20D, true, "寒毒体质，适合冰髓寒魄神通"),
    GOLD_FORGING_BODY("锻金之体", "gold_forge", 1.10D, 1.15D, false, "金属性亲和"),
    MOLTEN_GOLD_BODY("熔金之体", "gold_forge", 1.05D, 1.25D, false, "适合炼体术"),
    FIVE_THUNDER_BODY("五雷之体", "dragon_chant", 1.35D, 1.25D, false, "可操纵五种雷电之力"),
    NINE_SPIRIT_SWORD_BODY("九灵剑体", "nine_spirit_sword", 1.25D, 1.35D, false, "剑修极致体质"),
    SEVEN_STAR_MOON_BODY("七星月体", "void_shadow", 1.25D, 1.10D, false, "合体后进阶大乘概率更高"),
    CHASTE_YIN_BODY("姹女素阴体", "yin_yang_unbalanced", 1.10D, 1.20D, false, "魔功修炼事半功倍"),
    HEAVENLY_YIN_BODY("天阴之体", "yin_yang_unbalanced", 1.20D, 1.10D, false, "可助抵挡天劫"),
    THREE_YANG_BODY("三阳之体", "spirit_body_mild", 1.10D, 1.15D, false, "阳性体质"),
    CHARMING_BODY("天生媚体", "self_govern", 1.05D, 1.10D, false, "魅惑亲和"),
    MYSTIC_YIN_BODY("玄阴差女体", "yin_yang_unbalanced", 1.15D, 1.15D, false, "玄阴分支体质"),
    // 语料新增映射入口（枚举侧保留运行时兜底倍率）
    SELF_GOVERN_BODY("自治之体", "self_govern", 1.05D, 1.10D, false, "减益抗性"),
    UNDYING_BODY("不灭之体", "undying", 1.20D, 1.15D, false, "死劫保命"),
    VAJRA_BODY("金刚不坏", "vajra_undamage", 1.10D, 1.20D, false, "物抗"),
    VOID_SHADOW_BODY("虚影之体", "void_shadow", 1.15D, 1.10D, false, "虚影遁形"),
    TRUE_SPIRIT_BLOODLINE("真灵血脉", "true_spirit_bloodline_generic", 1.20D, 1.25D, false, "真灵血脉"),
    SWORD_INTENT_BODY("剑意灵躯", "sword_intent_body", 1.20D, 1.30D, false, "剑意凝躯");

    private final String displayName;
    private final String constitutionId;
    private final double breakthroughMultiplier;
    private final double cultivationMultiplier;
    private final boolean hasDefect;
    private final String description;

    SpecialPhysique(String displayName, String constitutionId, double breakthroughMultiplier,
                    double cultivationMultiplier, boolean hasDefect, String description) {
        this.displayName = displayName;
        this.constitutionId = constitutionId;
        this.breakthroughMultiplier = breakthroughMultiplier;
        this.cultivationMultiplier = cultivationMultiplier;
        this.hasDefect = hasDefect;
        this.description = description;
    }

    public String getDisplayName() { return displayName; }
    public String getConstitutionId() { return constitutionId; }
    public double getBreakthroughMultiplier() { return breakthroughMultiplier; }
    public double getCultivationMultiplier() { return cultivationMultiplier; }
    public boolean hasDefect() { return hasDefect; }
    public String getDescription() { return description; }

    public static String toConstitutionId(SpecialPhysique physique) {
        if (physique == null || physique == NONE) return "none";
        return physique.constitutionId;
    }

    public static SpecialPhysique fromConstitutionId(String id) {
        if (id == null || id.isBlank() || "none".equalsIgnoreCase(id)) return NONE;
        String key = id.trim().toLowerCase(Locale.ROOT);
        for (SpecialPhysique physique : values()) {
            if (physique.constitutionId.equals(key) || physique.name().equalsIgnoreCase(key)) {
                return physique;
            }
        }
        return switch (key) {
            case "tongyu_fengsui", "jade_phoenix_marrow" -> JADE_PHOENIX_MARROW;
            case "dragon_chant" -> DRAGON_CHANT_BODY;
            case "self_govern" -> SELF_GOVERN_BODY;
            case "undying" -> UNDYING_BODY;
            case "vajra_undamage" -> VAJRA_BODY;
            case "gold_forge" -> GOLD_FORGING_BODY;
            case "nine_spirit_sword" -> NINE_SPIRIT_SWORD_BODY;
            case "void_shadow" -> VOID_SHADOW_BODY;
            case "true_spirit_bloodline_generic" -> TRUE_SPIRIT_BLOODLINE;
            case "spirit_body_mild" -> ICE_MARROW_BODY;
            case "fire_spirit_root_variant" -> HIDDEN_THUNDER_ROOT;
            case "sword_intent_body" -> SWORD_INTENT_BODY;
            case "yin_yang_unbalanced" -> CHASTE_YIN_BODY;
            default -> NONE;
        };
    }

    public static Optional<SpecialPhysique> fromName(String name) {
        if (name == null || name.isBlank()) return Optional.empty();
        try {
            return Optional.of(valueOf(name.trim().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException ignored) {
            SpecialPhysique mapped = fromConstitutionId(name);
            return mapped == NONE ? Optional.empty() : Optional.of(mapped);
        }
    }

    public static SpecialPhysique random(RandomSource random) {
        // 约 2% 概率获得特殊体质
        if (random.nextInt(1000) >= 20) return NONE;
        SpecialPhysique[] values = values();
        // 跳过 NONE
        return values[1 + random.nextInt(values.length - 1)];
    }
}
