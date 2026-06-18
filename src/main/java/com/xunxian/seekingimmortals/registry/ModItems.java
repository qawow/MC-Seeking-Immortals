package com.xunxian.seekingimmortals.registry;

import com.xunxian.seekingimmortals.SeekingImmortalsMod;
import com.xunxian.seekingimmortals.cultivation.SpiritualRootAttribute;
import com.xunxian.seekingimmortals.item.*;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, SeekingImmortalsMod.MODID);

    // 五行灵石
    public static final RegistryObject<Item> METAL_SPIRIT_STONE = registerSpiritStone("metal_spirit_stone", 500, 25, 1, SpiritualRootAttribute.METAL);
    public static final RegistryObject<Item> METAL_SPIRIT_STONE_MID = registerSpiritStone("metal_spirit_stone_mid", 5000, 250, 4, SpiritualRootAttribute.METAL);
    public static final RegistryObject<Item> METAL_SPIRIT_STONE_HIGH = registerSpiritStone("metal_spirit_stone_high", 50000, 2500, 8, SpiritualRootAttribute.METAL);
    public static final RegistryObject<Item> METAL_SPIRIT_STONE_SUPERIOR = registerSpiritStone("metal_spirit_stone_superior", 500000, 25000, 12, SpiritualRootAttribute.METAL);
    public static final RegistryObject<Item> WOOD_SPIRIT_STONE = registerSpiritStone("wood_spirit_stone", 500, 25, 1, SpiritualRootAttribute.WOOD);
    public static final RegistryObject<Item> WOOD_SPIRIT_STONE_MID = registerSpiritStone("wood_spirit_stone_mid", 5000, 250, 4, SpiritualRootAttribute.WOOD);
    public static final RegistryObject<Item> WOOD_SPIRIT_STONE_HIGH = registerSpiritStone("wood_spirit_stone_high", 50000, 2500, 8, SpiritualRootAttribute.WOOD);
    public static final RegistryObject<Item> WOOD_SPIRIT_STONE_SUPERIOR = registerSpiritStone("wood_spirit_stone_superior", 500000, 25000, 12, SpiritualRootAttribute.WOOD);
    public static final RegistryObject<Item> WATER_SPIRIT_STONE = registerSpiritStone("water_spirit_stone", 500, 25, 1, SpiritualRootAttribute.WATER);
    public static final RegistryObject<Item> WATER_SPIRIT_STONE_MID = registerSpiritStone("water_spirit_stone_mid", 5000, 250, 4, SpiritualRootAttribute.WATER);
    public static final RegistryObject<Item> WATER_SPIRIT_STONE_HIGH = registerSpiritStone("water_spirit_stone_high", 50000, 2500, 8, SpiritualRootAttribute.WATER);
    public static final RegistryObject<Item> WATER_SPIRIT_STONE_SUPERIOR = registerSpiritStone("water_spirit_stone_superior", 500000, 25000, 12, SpiritualRootAttribute.WATER);
    public static final RegistryObject<Item> FIRE_ELEMENT_SPIRIT_STONE = registerSpiritStone("fire_element_spirit_stone", 500, 25, 1, SpiritualRootAttribute.FIRE);
    public static final RegistryObject<Item> FIRE_ELEMENT_SPIRIT_STONE_MID = registerSpiritStone("fire_element_spirit_stone_mid", 5000, 250, 4, SpiritualRootAttribute.FIRE);
    public static final RegistryObject<Item> FIRE_ELEMENT_SPIRIT_STONE_HIGH = registerSpiritStone("fire_element_spirit_stone_high", 50000, 2500, 8, SpiritualRootAttribute.FIRE);
    public static final RegistryObject<Item> FIRE_ELEMENT_SPIRIT_STONE_SUPERIOR = registerSpiritStone("fire_element_spirit_stone_superior", 500000, 25000, 12, SpiritualRootAttribute.FIRE);
    public static final RegistryObject<Item> EARTH_SPIRIT_STONE = registerSpiritStone("earth_spirit_stone", 500, 25, 1, SpiritualRootAttribute.EARTH);
    public static final RegistryObject<Item> EARTH_SPIRIT_STONE_MID = registerSpiritStone("earth_spirit_stone_mid", 5000, 250, 4, SpiritualRootAttribute.EARTH);
    public static final RegistryObject<Item> EARTH_SPIRIT_STONE_HIGH = registerSpiritStone("earth_spirit_stone_high", 50000, 2500, 8, SpiritualRootAttribute.EARTH);
    public static final RegistryObject<Item> EARTH_SPIRIT_STONE_SUPERIOR = registerSpiritStone("earth_spirit_stone_superior", 500000, 25000, 12, SpiritualRootAttribute.EARTH);

    public static final RegistryObject<Item> IMMORTAL_JADE = ITEMS.register("immortal_jade", () -> new ImmortalJadeItem(new Item.Properties()));

    public static final RegistryObject<Item> QI_RECOVERY_PILL = ITEMS.register("qi_recovery_pill", () -> new QiRecoveryPillItem(new Item.Properties(), 80));
    public static final RegistryObject<Item> CULTIVATION_PILL = ITEMS.register("cultivation_pill", () -> new CultivationPillItem(new Item.Properties(), 80));
    public static final RegistryObject<Item> BREAKTHROUGH_PILL = ITEMS.register("breakthrough_pill", () -> new BreakthroughPillItem(new Item.Properties()));

    public static final RegistryObject<Item> REJUVENATION_PILL_LOW = ITEMS.register("rejuvenation_pill_low", () -> new com.xunxian.seekingimmortals.item.pill.RejuvenationPill(new Item.Properties(), com.xunxian.seekingimmortals.item.pill.PillQuality.LOW));
    public static final RegistryObject<Item> FOUNDATION_BUILDING_PILL_LOW = ITEMS.register("foundation_building_pill_low", () -> new com.xunxian.seekingimmortals.item.pill.FoundationBuildingPill(new Item.Properties(), com.xunxian.seekingimmortals.item.pill.PillQuality.LOW));
    public static final RegistryObject<Item> HEALING_PILL_LOW = ITEMS.register("healing_pill_low", () -> new com.xunxian.seekingimmortals.item.pill.HealingPill(new Item.Properties(), com.xunxian.seekingimmortals.item.pill.PillQuality.LOW));
    public static final RegistryObject<Item> CLEAR_SPIRIT_POWDER_LOW = ITEMS.register("clear_spirit_powder_low", () -> new com.xunxian.seekingimmortals.item.pill.ClearSpiritPowder(new Item.Properties(), com.xunxian.seekingimmortals.item.pill.PillQuality.LOW));
    public static final RegistryObject<Item> FASTING_PILL_LOW = ITEMS.register("fasting_pill_low", () -> new com.xunxian.seekingimmortals.item.pill.FastingPill(new Item.Properties(), com.xunxian.seekingimmortals.item.pill.PillQuality.LOW));
    public static final RegistryObject<Item> CALMING_PILL_LOW = ITEMS.register("calming_pill_low", () -> new com.xunxian.seekingimmortals.item.pill.CalmingPill(new Item.Properties(), com.xunxian.seekingimmortals.item.pill.PillQuality.LOW));

    public static final RegistryObject<Item> SPIRIT_CHARM = ITEMS.register("spirit_charm", () -> new SpiritCharmItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> FLYING_SWORD = ITEMS.register("flying_sword", () -> new FlyingArtifactItem(new Item.Properties(), true));
    public static final RegistryObject<Item> FLYING_ARTIFACT = ITEMS.register("flying_artifact", () -> new FlyingArtifactItem(new Item.Properties(), false));
    public static final RegistryObject<Item> FIRE_TALISMAN = ITEMS.register("fire_talisman", () -> new FireTalismanItem(new Item.Properties().stacksTo(16)));
    public static final RegistryObject<Item> ARMOR_TALISMAN = ITEMS.register("armor_talisman", () -> new ArmorTalismanItem(new Item.Properties().stacksTo(16)));
    public static final RegistryObject<Item> SPEED_TALISMAN = ITEMS.register("speed_talisman", () -> new SpeedTalismanItem(new Item.Properties().stacksTo(16)));
    public static final RegistryObject<Item> LING_GEN_TEST_STONE = ITEMS.register("ling_gen_test_stone", () -> new LingGenTestStoneItem(new Item.Properties()));
    public static final RegistryObject<Item> SPIRIT_DETECTOR = ITEMS.register("spirit_detector", () -> new SpiritDetectorItem(new Item.Properties()));
    public static final RegistryObject<Item> LEYLINE_COMPASS = ITEMS.register("leyline_compass", () -> new LeylineCompassItem(new Item.Properties()));
    public static final RegistryObject<Item> SPIRIT_ORE = ITEMS.register("spirit_ore", () -> new BlockItem(ModBlocks.SPIRIT_ORE.get(), new Item.Properties()));
    public static final RegistryObject<Item> MEDITATION_CUSHION = ITEMS.register("meditation_cushion", () -> new BlockItem(ModBlocks.MEDITATION_CUSHION.get(), new Item.Properties()));
    public static final RegistryObject<Item> LING_GEN_IDENTIFICATION_SLAB = ITEMS.register("ling_gen_identification_slab", () -> new BlockItem(ModBlocks.LING_GEN_IDENTIFICATION_SLAB.get(), new Item.Properties()));
    public static final RegistryObject<Item> SPIRIT_GATHERING_ARRAY = ITEMS.register("spirit_gathering_array", () -> new BlockItem(ModBlocks.SPIRIT_GATHERING_ARRAY.get(), new Item.Properties()));
    public static final RegistryObject<Item> TECHNIQUE_MANUAL_AZURE_ORIGIN_SWORD_ART = registerTechniqueManual("technique_manual_azure_origin_sword_art", "青元剑诀");
    public static final RegistryObject<Item> TECHNIQUE_MANUAL_AZURE_ORIGIN_SWORD_SUPPORT = registerTechniqueManual("technique_manual_azure_origin_sword_support", "青元剑诀辅助");
    public static final RegistryObject<Item> TECHNIQUE_MANUAL_SIX_PATHS_SAGE_CREATED = registerTechniqueManual("technique_manual_six_paths_sage_created", "六道极圣所创");
    public static final RegistryObject<Item> TECHNIQUE_MANUAL_DEMONIC = registerTechniqueManual("technique_manual_demonic", "魔道");
    public static final RegistryObject<Item> TECHNIQUE_MANUAL_YAO_BIRD_CULTIVATOR = registerTechniqueManual("technique_manual_yao_bird_cultivator", "妖族禽修");
    public static final RegistryObject<Item> TECHNIQUE_MANUAL_FIVE_ELEMENTS_ESCAPE = registerTechniqueManual("technique_manual_five_elements_escape", "五行遁术");
    public static final RegistryObject<Item> TECHNIQUE_MANUAL_THOUSAND_ILLUSION_SECT = registerTechniqueManual("technique_manual_thousand_illusion_sect", "千幻宗");
    public static final RegistryObject<Item> TECHNIQUE_MANUAL_NANGONG_WAN_MAIN = registerTechniqueManual("technique_manual_nangong_wan_main", "南宫婉主修");
    public static final RegistryObject<Item> TECHNIQUE_MANUAL_YAO = registerTechniqueManual("technique_manual_yao", "妖族");
    public static final RegistryObject<Item> TECHNIQUE_MANUAL_GHOST = registerTechniqueManual("technique_manual_ghost", "鬼道");
    public static final RegistryObject<Item> TECHNIQUE_MANUAL_PURPLE_LUO_MYSTIC_SKILL = registerTechniqueManual("technique_manual_purple_luo_mystic_skill", "紫罗玄功");
    public static final RegistryObject<Item> TECHNIQUE_MANUAL_ORTHODOX = registerTechniqueManual("technique_manual_orthodox", "正道");
    public static final RegistryObject<Item> TECHNIQUE_MANUAL_AZURE_ORIGIN_SWORD_DERIVATIVE = registerTechniqueManual("technique_manual_azure_origin_sword_derivative", "青元剑诀衍生");
    public static final RegistryObject<Item> TECHNIQUE_MANUAL_COMMON = registerTechniqueManual("technique_manual_common", "通用");
    public static final RegistryObject<Item> TECHNIQUE_MANUAL_SPIRIT_TAMING_BASIC = registerTechniqueManual("technique_manual_spirit_taming_basic", "御灵宗基础");
    public static final RegistryObject<Item> TECHNIQUE_MANUAL_COMMON_TRICKS = registerTechniqueManual("technique_manual_common_tricks", "通用小技巧");
    public static final RegistryObject<Item> TECHNIQUE_MANUAL_AZURE_ORIGIN_SWORD_SUPPORT_SKILL = registerTechniqueManual("technique_manual_azure_origin_sword_support_skill", "青元剑诀辅助功法");
    public static final RegistryObject<Item> TECHNIQUE_MANUAL_MYSTIC_YIN_APPENDIX = registerTechniqueManual("technique_manual_mystic_yin_appendix", "玄阴经附属");
    public static final RegistryObject<Item> TECHNIQUE_MANUAL_FORMATION = registerTechniqueManual("technique_manual_formation", "阵法类");
    public static final RegistryObject<Item> TECHNIQUE_MANUAL_ANCIENT_SWORD_SECT = registerTechniqueManual("technique_manual_ancient_sword_sect", "古剑门");
    public static final RegistryObject<Item> TECHNIQUE_MANUAL_SPIRIT_TAMING_SECT = registerTechniqueManual("technique_manual_spirit_taming_sect", "御灵宗");
    public static final RegistryObject<Item> TECHNIQUE_MANUAL_AZURE_ORIGIN_SWORD_SPIRIT_REALM_PRE = registerTechniqueManual("technique_manual_azure_origin_sword_spirit_realm_pre", "青元剑诀·灵界篇前置");
    public static final RegistryObject<Item> TECHNIQUE_MANUAL_AZURE_ORIGIN_SWORD_SPIRIT_REALM = registerTechniqueManual("technique_manual_azure_origin_sword_spirit_realm", "青元剑诀·灵界篇");
    public static final RegistryObject<Item> TECHNIQUE_MANUAL_THOUSAND_BAMBOO_HERITAGE = registerTechniqueManual("technique_manual_thousand_bamboo_heritage", "千竹教传承");
    public static final RegistryObject<Item> TECHNIQUE_MANUAL_GREAT_DEVELOPMENT_FORMULA = registerTechniqueManual("technique_manual_great_development_formula", "大衍诀");
    public static final RegistryObject<Item> TECHNIQUE_MANUAL_GREAT_DEVELOPMENT_MASTER = registerTechniqueManual("technique_manual_great_development_master", "大衍神君");
    public static final RegistryObject<Item> TECHNIQUE_MANUAL_HAN_LI_SELF_CREATED = registerTechniqueManual("technique_manual_han_li_self_created", "韩立自创");
    public static final RegistryObject<Item> TECHNIQUE_MANUAL_CHAOTIC_STAR_SEA_DEMONIC = registerTechniqueManual("technique_manual_chaotic_star_sea_demonic", "乱星海魔修");
    public static final RegistryObject<Item> TECHNIQUE_MANUAL_TOP_DEMONIC = registerTechniqueManual("technique_manual_top_demonic", "魔道顶阶");
    public static final RegistryObject<Item> TECHNIQUE_MANUAL_MYSTIC_HERDER_NASCENT_APPENDIX = registerTechniqueManual("technique_manual_mystic_herder_nascent_appendix", "玄牧化婴附属");
    public static final RegistryObject<Item> TECHNIQUE_MANUAL_CHAOTIC_STAR_SEA = registerTechniqueManual("technique_manual_chaotic_star_sea", "乱星海");
    public static final RegistryObject<Item> TECHNIQUE_MANUAL_HEAVENLY_LAN_TEMPLE = registerTechniqueManual("technique_manual_heavenly_lan_temple", "天澜圣殿");
    public static final RegistryObject<Item> TECHNIQUE_MANUAL_ANCIENT_SECRET_ART = registerTechniqueManual("technique_manual_ancient_secret_art", "上古秘术");
    public static final RegistryObject<Item> TECHNIQUE_MANUAL_SUPREME_DEMONIC = registerTechniqueManual("technique_manual_supreme_demonic", "魔道无上");
    public static final RegistryObject<Item> TECHNIQUE_MANUAL_NASCENT_SOUL_COMMON = registerTechniqueManual("technique_manual_nascent_soul_common", "元婴修士通用");
    public static final RegistryObject<Item> TECHNIQUE_MANUAL_EVERGREEN_APPENDIX = registerTechniqueManual("technique_manual_evergreen_appendix", "长春功附载");
    public static final RegistryObject<Item> TECHNIQUE_MANUAL_COMMON_LOW = registerTechniqueManual("technique_manual_common_low", "通用低阶");
    public static final RegistryObject<Item> TECHNIQUE_MANUAL_MORTAL_MARTIAL = registerTechniqueManual("technique_manual_mortal_martial", "世俗武林");
    public static final RegistryObject<Item> TECHNIQUE_MANUAL_SEVEN_MYSTERIES_SECT = registerTechniqueManual("technique_manual_seven_mysteries_sect", "七玄门");
    public static final RegistryObject<Item> TECHNIQUE_MANUAL_LOST_TRUE_IMMORTAL_ART = registerTechniqueManual("technique_manual_lost_true_immortal_art", "上古失传真仙术");
    public static final RegistryObject<Item> TECHNIQUE_MANUAL_NASCENT_SOUL_LATE_PLUS = registerTechniqueManual("technique_manual_nascent_soul_late_plus", "元婴后期以上");
    public static final RegistryObject<Item> TECHNIQUE_MANUAL_YUANCHA_SAINT_ANCESTOR = registerTechniqueManual("technique_manual_yuancha_saint_ancestor", "元刹圣祖");
    public static final RegistryObject<Item> TECHNIQUE_MANUAL_ANCIENT_DEMON_SECRET = registerTechniqueManual("technique_manual_ancient_demon_secret", "古魔秘术");
    public static final RegistryObject<Item> TECHNIQUE_MANUAL_ANCIENT_DEMON = registerTechniqueManual("technique_manual_ancient_demon", "古魔");
    public static final RegistryObject<Item> TECHNIQUE_MANUAL_BRAHMA_SACRED_FRAGMENT = registerTechniqueManual("technique_manual_brahma_sacred_fragment", "梵圣真片");
    public static final RegistryObject<Item> TECHNIQUE_MANUAL_BUDDHIST = registerTechniqueManual("technique_manual_buddhist", "佛门");
    public static final RegistryObject<Item> TECHNIQUE_MANUAL_ANCIENT_DEMONIC_SKILL = registerTechniqueManual("technique_manual_ancient_demonic_skill", "上古魔功");
    public static final RegistryObject<Item> TECHNIQUE_MANUAL_GREAT_JIN = registerTechniqueManual("technique_manual_great_jin", "大晋");
    public static final RegistryObject<Item> TECHNIQUE_MANUAL_BLACK_WIND_FLAG_SPIRIT = registerTechniqueManual("technique_manual_black_wind_flag_spirit", "黑风旗器灵");
    public static final RegistryObject<Item> TECHNIQUE_MANUAL_LITTLE_POLE_PALACE = registerTechniqueManual("technique_manual_little_pole_palace", "小极宫");
    public static final RegistryObject<Item> TECHNIQUE_MANUAL_GOLD_MAGNETIC_SPIRIT_WOOD = registerTechniqueManual("technique_manual_gold_magnetic_spirit_wood", "金磁灵木");
    public static final RegistryObject<Item> TECHNIQUE_MANUAL_SELF_CREATED = registerTechniqueManual("technique_manual_self_created", "自创");
    public static final RegistryObject<Item> TECHNIQUE_MANUAL_KUNPENG_RED_CLOUD_CREATED = registerTechniqueManual("technique_manual_kunpeng_red_cloud_created", "鲲鹏族红云老祖所创");
    public static final RegistryObject<Item> TECHNIQUE_MANUAL_IMMORTAL_THUNDER_ORIGIN = registerTechniqueManual("technique_manual_immortal_thunder_origin", "仙界雷法本源");
    public static final RegistryObject<Item> TECHNIQUE_MANUAL_DEMON_DOMAIN_BODY_REFINING = registerTechniqueManual("technique_manual_demon_domain_body_refining", "魔域顶级炼体功");
    public static final RegistryObject<Item> TECHNIQUE_MANUAL_IMMORTAL_REALM_SKILL = registerTechniqueManual("technique_manual_immortal_realm_skill", "仙界功法");
    public static final RegistryObject<Item> TECHNIQUE_MANUAL_DEMON_RACE_SECRET = registerTechniqueManual("technique_manual_demon_race_secret", "魔族秘传");
    public static final RegistryObject<Item> TECHNIQUE_MANUAL_TRUE_WORD_SECT_HERITAGE = registerTechniqueManual("technique_manual_true_word_sect_heritage", "真言门传承");
    public static final RegistryObject<Item> TECHNIQUE_MANUAL_AZURE_SEA_TRUE_LORD_SKILL = registerTechniqueManual("technique_manual_azure_sea_true_lord_skill", "碧海真君成名功法");
    public static final RegistryObject<Item> TECHNIQUE_MANUAL_GRAY_IMMORTAL_HERITAGE = registerTechniqueManual("technique_manual_gray_immortal_heritage", "灰仙传承");

    // 材料系统
    public static final RegistryObject<Item> SPIRIT_GRASS = registerMaterial("spirit_grass", com.xunxian.seekingimmortals.item.material.MaterialType.SPIRIT_GRASS);
    public static final RegistryObject<Item> CLOUD_MUSHROOM = registerMaterial("cloud_mushroom", com.xunxian.seekingimmortals.item.material.MaterialType.CLOUD_MUSHROOM);
    public static final RegistryObject<Item> PHOENIX_FEATHER_FLOWER = registerMaterial("phoenix_feather_flower", com.xunxian.seekingimmortals.item.material.MaterialType.PHOENIX_FEATHER_FLOWER);
    public static final RegistryObject<Item> DRAGON_BLOOD_GRASS = registerMaterial("dragon_blood_grass", com.xunxian.seekingimmortals.item.material.MaterialType.DRAGON_BLOOD_GRASS);
    public static final RegistryObject<Item> IMMORTAL_GINSENG = registerMaterial("immortal_ginseng", com.xunxian.seekingimmortals.item.material.MaterialType.IMMORTAL_GINSENG);
    public static final RegistryObject<Item> BEAST_CORE = registerMaterial("beast_core", com.xunxian.seekingimmortals.item.material.MaterialType.BEAST_CORE);
    public static final RegistryObject<Item> SPIRIT_BEAST_BONE = registerMaterial("spirit_beast_bone", com.xunxian.seekingimmortals.item.material.MaterialType.SPIRIT_BEAST_BONE);
    public static final RegistryObject<Item> DRAGON_SCALE = registerMaterial("dragon_scale", com.xunxian.seekingimmortals.item.material.MaterialType.DRAGON_SCALE);
    public static final RegistryObject<Item> PHOENIX_FEATHER = registerMaterial("phoenix_feather", com.xunxian.seekingimmortals.item.material.MaterialType.PHOENIX_FEATHER);
    public static final RegistryObject<Item> TRUE_DRAGON_BLOOD = registerMaterial("true_dragon_blood", com.xunxian.seekingimmortals.item.material.MaterialType.TRUE_DRAGON_BLOOD);
    public static final RegistryObject<Item> SPIRIT_IRON = registerMaterial("spirit_iron", com.xunxian.seekingimmortals.item.material.MaterialType.SPIRIT_IRON);
    public static final RegistryObject<Item> COLD_JADE = registerMaterial("cold_jade", com.xunxian.seekingimmortals.item.material.MaterialType.COLD_JADE);
    public static final RegistryObject<Item> STAR_METEORITE = registerMaterial("star_meteorite", com.xunxian.seekingimmortals.item.material.MaterialType.STAR_METEORITE);
    public static final RegistryObject<Item> CELESTIAL_CRYSTAL = registerMaterial("celestial_crystal", com.xunxian.seekingimmortals.item.material.MaterialType.CELESTIAL_CRYSTAL);
    public static final RegistryObject<Item> CHAOS_GOLD = registerMaterial("chaos_gold", com.xunxian.seekingimmortals.item.material.MaterialType.CHAOS_GOLD);
    public static final RegistryObject<Item> SOUL_FRAGMENT = registerMaterial("soul_fragment", com.xunxian.seekingimmortals.item.material.MaterialType.SOUL_FRAGMENT);
    public static final RegistryObject<Item> VOID_CRYSTAL = registerMaterial("void_crystal", com.xunxian.seekingimmortals.item.material.MaterialType.VOID_CRYSTAL);
    public static final RegistryObject<Item> TIME_SAND = registerMaterial("time_sand", com.xunxian.seekingimmortals.item.material.MaterialType.TIME_SAND);
    public static final RegistryObject<Item> PRIMORDIAL_ESSENCE = registerMaterial("primordial_essence", com.xunxian.seekingimmortals.item.material.MaterialType.PRIMORDIAL_ESSENCE);

    private static RegistryObject<Item> registerTechniqueManual(String name, String source) {
        return ITEMS.register(name, () -> new TechniqueManualItem(new Item.Properties().stacksTo(1), source));
    }

    private static RegistryObject<Item> registerSpiritStone(String name, int maxStoredPower, int absorbPerSecond, int passiveBonus, SpiritualRootAttribute attribute) {
        return ITEMS.register(name, () -> new SpiritStoneItem(new Item.Properties(), maxStoredPower, absorbPerSecond, passiveBonus, attribute));
    }

    private static RegistryObject<Item> registerMaterial(String name, com.xunxian.seekingimmortals.item.material.MaterialType type) {
        return ITEMS.register(name, () -> new com.xunxian.seekingimmortals.item.material.BaseMaterialItem(
            new Item.Properties(), type.getCategory(), type.getRarity(), type.getDescription()));
    }

    private ModItems() {}
    public static void register(IEventBus bus) { ITEMS.register(bus); }
}
