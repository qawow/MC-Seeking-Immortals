package com.xunxian.seekingimmortals.item.pill;

import com.xunxian.seekingimmortals.cultivation.Realm;

public enum PillType {
    // 修炼类 - 基础
    REJUVENATION("回春丹", PillCategory.CULTIVATION, Realm.QI_REFINING, "恢复灵力并增加少量修为"),
    YELLOW_DRAGON("黄龙丹", PillCategory.CULTIVATION, Realm.QI_REFINING, "增加功力、脱胎换骨，对炼气期十层以下有奇效"),
    GOLDEN_MARROW("金髓丸", PillCategory.CULTIVATION, Realm.QI_REFINING, "增加功力、脱胎换骨"),
    HARMONIZING_QI("合气丹", PillCategory.CULTIVATION, Realm.FOUNDATION_ESTABLISHMENT, "适合筑基期增进修为"),
    SPIRIT_GATHERING("聚灵丹", PillCategory.CULTIVATION, Realm.FOUNDATION_ESTABLISHMENT, "筑基期增进法力和修为"),
    QI_REFINING_POWDER("炼气散", PillCategory.CULTIVATION, Realm.FOUNDATION_ESTABLISHMENT, "筑基期增加法力"),
    TRUE_ESSENCE("真元丹", PillCategory.CULTIVATION, Realm.FOUNDATION_ESTABLISHMENT, "增加筑基期法力"),

    // 修炼类 - 突破
    FOUNDATION_BUILDING("筑基丹", PillCategory.CULTIVATION, Realm.QI_REFINING, "辅助炼气期突破筑基，提升成功率"),
    DUST_DESCENDING("降尘丹", PillCategory.CULTIVATION, Realm.FOUNDATION_ESTABLISHMENT, "增加结丹成功率（珍贵，需稀有材料）"),
    GOLDEN_CORE("结金丹", PillCategory.CULTIVATION, Realm.FOUNDATION_ESTABLISHMENT, "辅助筑基期突破结丹，提升成功率"),
    NASCENT_SOUL("元婴丹", PillCategory.CULTIVATION, Realm.CORE_FORMATION, "辅助结丹期突破元婴，提升成功率"),

    // 疗伤类
    HEALING("疗伤丹", PillCategory.HEALING, Realm.QI_REFINING, "治疗伤势，恢复生命值"),
    ESSENCE_NOURISHING("养精丹", PillCategory.HEALING, Realm.QI_REFINING, "疗内外伤奇效，可保命减轻伤势"),
    BONE_MENDING("续骨丹", PillCategory.HEALING, Realm.FOUNDATION_ESTABLISHMENT, "治疗重伤，消除负面状态"),
    SOUL_RETURN("回魂丹", PillCategory.HEALING, Realm.CORE_FORMATION, "恢复神识，缓解心魔"),
    CLEAR_SPIRIT_POWDER("清灵散", PillCategory.HEALING, Realm.QI_REFINING, "解毒圣药，能解多种剧毒"),

    // 辅助类
    AURA_CONCEALMENT("敛气丹", PillCategory.AUXILIARY, Realm.QI_REFINING, "隐藏境界气息"),
    BEAUTY_PRESERVING("驻颜丹", PillCategory.AUXILIARY, Realm.FOUNDATION_ESTABLISHMENT, "保持容颜，延缓衰老"),
    APPEARANCE_FIXING("定颜丹", PillCategory.AUXILIARY, Realm.QI_REFINING, "永驻容貌、青春长存（年轻时服用有效）"),
    FASTING("辟谷丹", PillCategory.AUXILIARY, Realm.QI_REFINING, "短期无需进食（可坚持一个月左右）"),
    LONGEVITY("增寿丹", PillCategory.AUXILIARY, Realm.CORE_FORMATION, "增加寿元"),
    LONGEVITY_ETERNAL("长生丹", PillCategory.AUXILIARY, Realm.CORE_FORMATION, "增加寿元（用寿元果炼制）"),
    SPIRIT_STABILIZING("定灵丹", PillCategory.AUXILIARY, Realm.NASCENT_SOUL, "增进修为，定心安魂，减轻心魔"),
    HEAVEN_MENDING("补天丹", PillCategory.AUXILIARY, Realm.FOUNDATION_ESTABLISHMENT, "洗炼灵根、弥补不纯，让进阶元婴更容易"),

    // 走火入魔对策
    CALMING("稳神丹", PillCategory.AUXILIARY, Realm.QI_REFINING, "稳定心神，降低 20% 走火入魔风险");

    private final String displayName;
    private final PillCategory category;
    private final Realm minRealm;
    private final String description;

    PillType(String displayName, PillCategory category, Realm minRealm, String description) {
        this.displayName = displayName;
        this.category = category;
        this.minRealm = minRealm;
        this.description = description;
    }

    public String getDisplayName() { return displayName; }
    public PillCategory getCategory() { return category; }
    public Realm getMinRealm() { return minRealm; }
    public String getDescription() { return description; }
}
