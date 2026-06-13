package com.xunxian.seekingimmortals.item.material;

public enum MaterialType {
    // 灵草灵药
    SPIRIT_GRASS("灵草", MaterialCategory.SPIRITUAL_HERB, MaterialRarity.COMMON, "基础炼丹材料"),
    CLOUD_MUSHROOM("云雾菇", MaterialCategory.SPIRITUAL_HERB, MaterialRarity.UNCOMMON, "筑基期炼丹材料"),
    PHOENIX_FEATHER_FLOWER("凤羽花", MaterialCategory.SPIRITUAL_HERB, MaterialRarity.RARE, "结丹期炼丹材料"),
    DRAGON_BLOOD_GRASS("龙血草", MaterialCategory.SPIRITUAL_HERB, MaterialRarity.EPIC, "元婴期炼丹材料"),
    IMMORTAL_GINSENG("仙参", MaterialCategory.SPIRITUAL_HERB, MaterialRarity.LEGENDARY, "化神期炼丹材料"),

    // 妖兽材料
    BEAST_CORE("妖兽内丹", MaterialCategory.BEAST_MATERIAL, MaterialRarity.COMMON, "妖兽核心能量"),
    SPIRIT_BEAST_BONE("灵兽骨", MaterialCategory.BEAST_MATERIAL, MaterialRarity.UNCOMMON, "炼器材料"),
    DRAGON_SCALE("龙鳞", MaterialCategory.BEAST_MATERIAL, MaterialRarity.RARE, "高级炼器材料"),
    PHOENIX_FEATHER("凤凰羽", MaterialCategory.BEAST_MATERIAL, MaterialRarity.EPIC, "顶级炼器材料"),
    TRUE_DRAGON_BLOOD("真龙血", MaterialCategory.BEAST_MATERIAL, MaterialRarity.LEGENDARY, "炼体圣药"),

    // 矿石材料
    SPIRIT_IRON("灵铁", MaterialCategory.MINERAL, MaterialRarity.COMMON, "基础炼器材料"),
    COLD_JADE("寒玉", MaterialCategory.MINERAL, MaterialRarity.UNCOMMON, "炼器、阵法材料"),
    STAR_METEORITE("星陨铁", MaterialCategory.MINERAL, MaterialRarity.RARE, "高级炼器材料"),
    CELESTIAL_CRYSTAL("天晶石", MaterialCategory.MINERAL, MaterialRarity.EPIC, "顶级炼器材料"),
    CHAOS_GOLD("混沌金", MaterialCategory.MINERAL, MaterialRarity.LEGENDARY, "仙器材料"),

    // 特殊材料
    SOUL_FRAGMENT("魂魄碎片", MaterialCategory.SPECIAL, MaterialRarity.UNCOMMON, "炼制灵魂类法宝"),
    VOID_CRYSTAL("虚空结晶", MaterialCategory.SPECIAL, MaterialRarity.RARE, "空间法宝材料"),
    TIME_SAND("时光之砂", MaterialCategory.SPECIAL, MaterialRarity.EPIC, "时间类秘宝材料"),
    PRIMORDIAL_ESSENCE("先天本源", MaterialCategory.SPECIAL, MaterialRarity.LEGENDARY, "至宝炼制材料");

    private final String displayName;
    private final MaterialCategory category;
    private final MaterialRarity rarity;
    private final String description;

    MaterialType(String displayName, MaterialCategory category, MaterialRarity rarity, String description) {
        this.displayName = displayName;
        this.category = category;
        this.rarity = rarity;
        this.description = description;
    }

    public String getDisplayName() { return displayName; }
    public MaterialCategory getCategory() { return category; }
    public MaterialRarity getRarity() { return rarity; }
    public String getDescription() { return description; }
}
