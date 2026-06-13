package com.xunxian.seekingimmortals.skill;

import com.xunxian.seekingimmortals.cultivation.Realm;
import com.xunxian.seekingimmortals.cultivation.SpiritualRootAttribute;

public enum SkillType {
    // 功法系统
    CHANGCHUN_METHOD("长春功", SkillCategory.CULTIVATION_METHOD, Realm.QI_REFINING, "基础修炼功法，适合新手入门"),
    DAYAN_METHOD("大衍诀", SkillCategory.CULTIVATION_METHOD, Realm.FOUNDATION_ESTABLISHMENT, "高阶功法，提升修炼速度 50%"),
    QINGYUAN_SWORD_METHOD("青元剑诀", SkillCategory.CULTIVATION_METHOD, Realm.CORE_FORMATION, "剑修专属功法，攻击力大幅提升"),
    DEVOURING_METHOD("吞噬功法", SkillCategory.CULTIVATION_METHOD, Realm.NASCENT_SOUL, "魔道功法，可吞噬他人修为"),
    BODY_REFINING_METHOD("炼体功法", SkillCategory.CULTIVATION_METHOD, Realm.QI_REFINING, "体修专属，提升防御和体质"),

    // 五行基础法术
    FIREBALL("火球术", SkillCategory.SPELL, Realm.QI_REFINING, "基础火系法术，发射火球攻击", SpiritualRootAttribute.FIRE),
    ICE_CONE("冰锥术", SkillCategory.SPELL, Realm.QI_REFINING, "基础水系法术，发射冰锥减速敌人", SpiritualRootAttribute.WATER, SpiritualRootAttribute.ICE),
    VINE_BIND("藤蔓术", SkillCategory.SPELL, Realm.QI_REFINING, "基础木系法术，束缚敌人", SpiritualRootAttribute.WOOD),
    EARTH_WALL("土墙术", SkillCategory.SPELL, Realm.QI_REFINING, "基础土系法术，召唤防御土墙", SpiritualRootAttribute.EARTH),
    METAL_BLADE("金刃术", SkillCategory.SPELL, Realm.QI_REFINING, "基础金系法术，发射锐利金刃", SpiritualRootAttribute.METAL),

    // 高阶法术
    THUNDER_STRIKE("雷击术", SkillCategory.SPELL, Realm.FOUNDATION_ESTABLISHMENT, "雷系高阶法术，召唤天雷", SpiritualRootAttribute.THUNDER),
    FIVE_ELEMENT_ROTATION("五行轮转", SkillCategory.SPELL, Realm.CORE_FORMATION, "五行大法，连续释放五行法术"),
    SWORD_FORMATION("剑阵", SkillCategory.SPELL, Realm.NASCENT_SOUL, "剑修终极法术，布置飞剑剑阵"),

    // 辅助法术
    LIGHTNESS_SKILL("轻身术", SkillCategory.SPELL, Realm.QI_REFINING, "提升移动速度和跳跃高度"),
    INVISIBILITY("隐身术", SkillCategory.SPELL, Realm.FOUNDATION_ESTABLISHMENT, "短时间内隐身"),
    EARTH_ESCAPE("遁术", SkillCategory.SPELL, Realm.CORE_FORMATION, "瞬间传送到安全位置"),

    // 神识类法术
    SOUL_SEARCH("搜魂术", SkillCategory.SPELL, Realm.NASCENT_SOUL, "搜索目标记忆（PVE）", SpiritualRootAttribute.DARK),
    DETECTION("探测术", SkillCategory.SPELL, Realm.FOUNDATION_ESTABLISHMENT, "探测周围生物和资源"),

    // 生活技能
    ALCHEMY("炼丹术", SkillCategory.CRAFTING, Realm.QI_REFINING, "炼制丹药，等级影响品质"),
    ARTIFACT_REFINING("炼器术", SkillCategory.CRAFTING, Realm.FOUNDATION_ESTABLISHMENT, "炼制法宝和装备"),
    FORMATION("阵法", SkillCategory.CRAFTING, Realm.CORE_FORMATION, "布置灵气阵法"),
    TALISMAN_CRAFTING("符箓绘制", SkillCategory.CRAFTING, Realm.QI_REFINING, "绘制符箓"),

    // 特殊技能
    BEAST_TAMING("驭兽术", SkillCategory.SPECIAL, Realm.FOUNDATION_ESTABLISHMENT, "契约和驯服灵兽"),
    PUPPET_CONTROL("傀儡操控", SkillCategory.SPECIAL, Realm.CORE_FORMATION, "操控战斗傀儡"),
    MULTI_CASTING("分神多用", SkillCategory.SPECIAL, Realm.NASCENT_SOUL, "同时施展多个法术");

    private final String displayName;
    private final SkillCategory category;
    private final Realm requiredRealm;
    private final String description;
    private final SpiritualRootAttribute[] affinityAttributes;

    SkillType(String displayName, SkillCategory category, Realm requiredRealm, String description, SpiritualRootAttribute... affinityAttributes) {
        this.displayName = displayName;
        this.category = category;
        this.requiredRealm = requiredRealm;
        this.description = description;
        this.affinityAttributes = affinityAttributes;
    }

    public String getDisplayName() { return displayName; }
    public SkillCategory getCategory() { return category; }
    public Realm getRequiredRealm() { return requiredRealm; }
    public String getDescription() { return description; }
    public SpiritualRootAttribute[] getAffinityAttributes() { return affinityAttributes; }
    public boolean hasAffinityRequirement() { return affinityAttributes.length > 0; }
}
