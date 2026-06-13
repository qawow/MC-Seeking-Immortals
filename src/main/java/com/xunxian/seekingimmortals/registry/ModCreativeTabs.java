package com.xunxian.seekingimmortals.registry;

import com.xunxian.seekingimmortals.SeekingImmortalsMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, SeekingImmortalsMod.MODID);

    public static final RegistryObject<CreativeModeTab> SEEKING_IMMORTALS_TAB = TABS.register("seeking_immortals_tab", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.seeking_immortals"))
            .icon(() -> new ItemStack(ModItems.SPIRIT_STONE.get()))
            .displayItems((parameters, output) -> {
                // 货币
                output.accept(ModItems.SPIRIT_STONE.get());
                output.accept(ModItems.SPIRIT_STONE_MID.get());
                output.accept(ModItems.SPIRIT_STONE_HIGH.get());
                output.accept(ModItems.SPIRIT_STONE_SUPERIOR.get());
                output.accept(ModItems.IMMORTAL_JADE.get());

                // 五行灵石
                output.accept(ModItems.METAL_SPIRIT_STONE.get());
                output.accept(ModItems.METAL_SPIRIT_STONE_MID.get());
                output.accept(ModItems.METAL_SPIRIT_STONE_HIGH.get());
                output.accept(ModItems.METAL_SPIRIT_STONE_SUPERIOR.get());
                output.accept(ModItems.WOOD_SPIRIT_STONE.get());
                output.accept(ModItems.WOOD_SPIRIT_STONE_MID.get());
                output.accept(ModItems.WOOD_SPIRIT_STONE_HIGH.get());
                output.accept(ModItems.WOOD_SPIRIT_STONE_SUPERIOR.get());
                output.accept(ModItems.WATER_SPIRIT_STONE.get());
                output.accept(ModItems.WATER_SPIRIT_STONE_MID.get());
                output.accept(ModItems.WATER_SPIRIT_STONE_HIGH.get());
                output.accept(ModItems.WATER_SPIRIT_STONE_SUPERIOR.get());
                output.accept(ModItems.FIRE_ELEMENT_SPIRIT_STONE.get());
                output.accept(ModItems.FIRE_ELEMENT_SPIRIT_STONE_MID.get());
                output.accept(ModItems.FIRE_ELEMENT_SPIRIT_STONE_HIGH.get());
                output.accept(ModItems.FIRE_ELEMENT_SPIRIT_STONE_SUPERIOR.get());
                output.accept(ModItems.EARTH_SPIRIT_STONE.get());
                output.accept(ModItems.EARTH_SPIRIT_STONE_MID.get());
                output.accept(ModItems.EARTH_SPIRIT_STONE_HIGH.get());
                output.accept(ModItems.EARTH_SPIRIT_STONE_SUPERIOR.get());

                // 方块
                output.accept(ModItems.SPIRIT_ORE.get());
                output.accept(ModItems.MEDITATION_CUSHION.get());
                output.accept(ModItems.SPIRIT_GATHERING_ARRAY.get());

                // 丹药
                output.accept(ModItems.QI_RECOVERY_PILL.get());
                output.accept(ModItems.CULTIVATION_PILL.get());
                output.accept(ModItems.BREAKTHROUGH_PILL.get());
                output.accept(ModItems.REJUVENATION_PILL_LOW.get());
                output.accept(ModItems.FOUNDATION_BUILDING_PILL_LOW.get());
                output.accept(ModItems.HEALING_PILL_LOW.get());
                output.accept(ModItems.CLEAR_SPIRIT_POWDER_LOW.get());
                output.accept(ModItems.FASTING_PILL_LOW.get());

                // 材料 - 灵草
                output.accept(ModItems.SPIRIT_GRASS.get());
                output.accept(ModItems.CLOUD_MUSHROOM.get());
                output.accept(ModItems.PHOENIX_FEATHER_FLOWER.get());
                output.accept(ModItems.DRAGON_BLOOD_GRASS.get());
                output.accept(ModItems.IMMORTAL_GINSENG.get());

                // 材料 - 妖兽
                output.accept(ModItems.BEAST_CORE.get());
                output.accept(ModItems.SPIRIT_BEAST_BONE.get());
                output.accept(ModItems.DRAGON_SCALE.get());
                output.accept(ModItems.PHOENIX_FEATHER.get());
                output.accept(ModItems.TRUE_DRAGON_BLOOD.get());

                // 材料 - 矿石
                output.accept(ModItems.SPIRIT_IRON.get());
                output.accept(ModItems.COLD_JADE.get());
                output.accept(ModItems.STAR_METEORITE.get());
                output.accept(ModItems.CELESTIAL_CRYSTAL.get());
                output.accept(ModItems.CHAOS_GOLD.get());

                // 材料 - 特殊
                output.accept(ModItems.SOUL_FRAGMENT.get());
                output.accept(ModItems.VOID_CRYSTAL.get());
                output.accept(ModItems.TIME_SAND.get());
                output.accept(ModItems.PRIMORDIAL_ESSENCE.get());

                // 符箓、法宝和工具
                output.accept(ModItems.SPIRIT_CHARM.get());
                output.accept(ModItems.FLYING_SWORD.get());
                output.accept(ModItems.FLYING_ARTIFACT.get());
                output.accept(ModItems.FIRE_TALISMAN.get());
                output.accept(ModItems.ARMOR_TALISMAN.get());
                output.accept(ModItems.SPEED_TALISMAN.get());
                output.accept(ModItems.LING_GEN_TEST_STONE.get());
                output.accept(ModItems.SPIRIT_DETECTOR.get());
                output.accept(ModItems.LEYLINE_COMPASS.get());

                // 功法传承
                output.accept(ModItems.TECHNIQUE_MANUAL_AZURE_ORIGIN_SWORD_ART.get());
                output.accept(ModItems.TECHNIQUE_MANUAL_AZURE_ORIGIN_SWORD_SUPPORT.get());
                output.accept(ModItems.TECHNIQUE_MANUAL_SIX_PATHS_SAGE_CREATED.get());
                output.accept(ModItems.TECHNIQUE_MANUAL_DEMONIC.get());
                output.accept(ModItems.TECHNIQUE_MANUAL_YAO_BIRD_CULTIVATOR.get());
                output.accept(ModItems.TECHNIQUE_MANUAL_FIVE_ELEMENTS_ESCAPE.get());
                output.accept(ModItems.TECHNIQUE_MANUAL_THOUSAND_ILLUSION_SECT.get());
                output.accept(ModItems.TECHNIQUE_MANUAL_NANGONG_WAN_MAIN.get());
                output.accept(ModItems.TECHNIQUE_MANUAL_YAO.get());
                output.accept(ModItems.TECHNIQUE_MANUAL_GHOST.get());
                output.accept(ModItems.TECHNIQUE_MANUAL_PURPLE_LUO_MYSTIC_SKILL.get());
                output.accept(ModItems.TECHNIQUE_MANUAL_ORTHODOX.get());
                output.accept(ModItems.TECHNIQUE_MANUAL_AZURE_ORIGIN_SWORD_DERIVATIVE.get());
                output.accept(ModItems.TECHNIQUE_MANUAL_COMMON.get());
                output.accept(ModItems.TECHNIQUE_MANUAL_SPIRIT_TAMING_BASIC.get());
                output.accept(ModItems.TECHNIQUE_MANUAL_COMMON_TRICKS.get());
                output.accept(ModItems.TECHNIQUE_MANUAL_AZURE_ORIGIN_SWORD_SUPPORT_SKILL.get());
                output.accept(ModItems.TECHNIQUE_MANUAL_MYSTIC_YIN_APPENDIX.get());
                output.accept(ModItems.TECHNIQUE_MANUAL_FORMATION.get());
                output.accept(ModItems.TECHNIQUE_MANUAL_ANCIENT_SWORD_SECT.get());
                output.accept(ModItems.TECHNIQUE_MANUAL_SPIRIT_TAMING_SECT.get());
                output.accept(ModItems.TECHNIQUE_MANUAL_AZURE_ORIGIN_SWORD_SPIRIT_REALM_PRE.get());
                output.accept(ModItems.TECHNIQUE_MANUAL_AZURE_ORIGIN_SWORD_SPIRIT_REALM.get());
                output.accept(ModItems.TECHNIQUE_MANUAL_THOUSAND_BAMBOO_HERITAGE.get());
                output.accept(ModItems.TECHNIQUE_MANUAL_GREAT_DEVELOPMENT_FORMULA.get());
                output.accept(ModItems.TECHNIQUE_MANUAL_GREAT_DEVELOPMENT_MASTER.get());
                output.accept(ModItems.TECHNIQUE_MANUAL_HAN_LI_SELF_CREATED.get());
                output.accept(ModItems.TECHNIQUE_MANUAL_CHAOTIC_STAR_SEA_DEMONIC.get());
                output.accept(ModItems.TECHNIQUE_MANUAL_TOP_DEMONIC.get());
                output.accept(ModItems.TECHNIQUE_MANUAL_MYSTIC_HERDER_NASCENT_APPENDIX.get());
                output.accept(ModItems.TECHNIQUE_MANUAL_CHAOTIC_STAR_SEA.get());
                output.accept(ModItems.TECHNIQUE_MANUAL_HEAVENLY_LAN_TEMPLE.get());
                output.accept(ModItems.TECHNIQUE_MANUAL_ANCIENT_SECRET_ART.get());
                output.accept(ModItems.TECHNIQUE_MANUAL_SUPREME_DEMONIC.get());
                output.accept(ModItems.TECHNIQUE_MANUAL_NASCENT_SOUL_COMMON.get());
                output.accept(ModItems.TECHNIQUE_MANUAL_EVERGREEN_APPENDIX.get());
                output.accept(ModItems.TECHNIQUE_MANUAL_COMMON_LOW.get());
                output.accept(ModItems.TECHNIQUE_MANUAL_MORTAL_MARTIAL.get());
                output.accept(ModItems.TECHNIQUE_MANUAL_SEVEN_MYSTERIES_SECT.get());
                output.accept(ModItems.TECHNIQUE_MANUAL_LOST_TRUE_IMMORTAL_ART.get());
                output.accept(ModItems.TECHNIQUE_MANUAL_NASCENT_SOUL_LATE_PLUS.get());
                output.accept(ModItems.TECHNIQUE_MANUAL_YUANCHA_SAINT_ANCESTOR.get());
                output.accept(ModItems.TECHNIQUE_MANUAL_ANCIENT_DEMON_SECRET.get());
                output.accept(ModItems.TECHNIQUE_MANUAL_ANCIENT_DEMON.get());
                output.accept(ModItems.TECHNIQUE_MANUAL_BRAHMA_SACRED_FRAGMENT.get());
                output.accept(ModItems.TECHNIQUE_MANUAL_BUDDHIST.get());
                output.accept(ModItems.TECHNIQUE_MANUAL_ANCIENT_DEMONIC_SKILL.get());
                output.accept(ModItems.TECHNIQUE_MANUAL_GREAT_JIN.get());
                output.accept(ModItems.TECHNIQUE_MANUAL_BLACK_WIND_FLAG_SPIRIT.get());
                output.accept(ModItems.TECHNIQUE_MANUAL_LITTLE_POLE_PALACE.get());
                output.accept(ModItems.TECHNIQUE_MANUAL_GOLD_MAGNETIC_SPIRIT_WOOD.get());
                output.accept(ModItems.TECHNIQUE_MANUAL_SELF_CREATED.get());
                output.accept(ModItems.TECHNIQUE_MANUAL_KUNPENG_RED_CLOUD_CREATED.get());
                output.accept(ModItems.TECHNIQUE_MANUAL_IMMORTAL_THUNDER_ORIGIN.get());
                output.accept(ModItems.TECHNIQUE_MANUAL_DEMON_DOMAIN_BODY_REFINING.get());
                output.accept(ModItems.TECHNIQUE_MANUAL_IMMORTAL_REALM_SKILL.get());
                output.accept(ModItems.TECHNIQUE_MANUAL_DEMON_RACE_SECRET.get());
                output.accept(ModItems.TECHNIQUE_MANUAL_TRUE_WORD_SECT_HERITAGE.get());
                output.accept(ModItems.TECHNIQUE_MANUAL_AZURE_SEA_TRUE_LORD_SKILL.get());
                output.accept(ModItems.TECHNIQUE_MANUAL_GRAY_IMMORTAL_HERITAGE.get());
            }).build());

    private ModCreativeTabs() {}
    public static void register(IEventBus bus) { TABS.register(bus); }
}
