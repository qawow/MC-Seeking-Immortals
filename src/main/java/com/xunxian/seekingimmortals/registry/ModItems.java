package com.xunxian.seekingimmortals.registry;

import com.xunxian.seekingimmortals.SeekingImmortalsMod;
import com.xunxian.seekingimmortals.alchemy.AlchemyFormulaSource;
import com.xunxian.seekingimmortals.cultivation.Realm;
import com.xunxian.seekingimmortals.cultivation.SpiritualRootAttribute;
import com.xunxian.seekingimmortals.item.*;
import com.xunxian.seekingimmortals.item.CatalogManualItem;
import com.xunxian.seekingimmortals.item.alchemy.AlchemyFormulaItem;
import com.xunxian.seekingimmortals.item.alchemy.AlchemyTieredItem;
import com.xunxian.seekingimmortals.item.pill.CatalogPillItem;
import com.xunxian.seekingimmortals.item.pill.CatalogPillType;
import com.xunxian.seekingimmortals.item.pill.PillQuality;
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
    public static final RegistryObject<Item> SPIRIT_STONE_SHARD = ITEMS.register("spirit_stone_shard", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> YIN_STONE = ITEMS.register("yin_stone", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> YIN_COFFIN_NAIL = ITEMS.register("yin_coffin_nail", () -> new YinCoffinNailItem(new Item.Properties().stacksTo(16)));
    public static final RegistryObject<Item> ALLIANCE_MERIT_TOKEN = ITEMS.register("alliance_merit_token", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> WAR_CONTRIBUTION_TOKEN = ITEMS.register("war_contribution_token", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> CAPTURE_JAR = ITEMS.register("capture_jar", () -> new com.xunxian.seekingimmortals.item.CaptureJarItem(new Item.Properties()));
    public static final RegistryObject<Item> JADE_SLIP_BLANK = ITEMS.register("jade_slip_blank",
            () -> new CatalogConsumableItem(
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.UNCOMMON).stacksTo(16),
                    com.xunxian.seekingimmortals.item.material.MaterialCategory.SPECIAL,
                    com.xunxian.seekingimmortals.item.material.MaterialRarity.UNCOMMON,
                    "空白玉简",
                    BulkItemClassifier.consumable("jade_slip_blank").orElseThrow()));
    public static final RegistryObject<Item> PAPER_FORMULA_SCROLL = ITEMS.register("paper_formula_scroll",
            () -> new CatalogConsumableItem(
                    new Item.Properties().stacksTo(16),
                    com.xunxian.seekingimmortals.item.material.MaterialCategory.SPECIAL,
                    com.xunxian.seekingimmortals.item.material.MaterialRarity.COMMON,
                    "空白纸方",
                    BulkItemClassifier.consumable("paper_formula_scroll").orElseThrow()));
    public static final RegistryObject<Item> TALISMAN_PAPER_MORTAL = ITEMS.register("talisman_paper_mortal", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> PEARL_RAW = ITEMS.register("pearl_raw", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> IRONWOOD = ITEMS.register("ironwood",
            () -> new com.xunxian.seekingimmortals.item.material.BaseMaterialItem(
                    new Item.Properties(),
                    com.xunxian.seekingimmortals.item.material.MaterialCategory.SPECIAL,
                    com.xunxian.seekingimmortals.item.material.MaterialRarity.COMMON,
                    "Tiannan refinement and puppet material"));
    public static final RegistryObject<Item> SPIRIT_SILK = ITEMS.register("spirit_silk",
            () -> new com.xunxian.seekingimmortals.item.material.BaseMaterialItem(
                    new Item.Properties(),
                    com.xunxian.seekingimmortals.item.material.MaterialCategory.BEAST_MATERIAL,
                    com.xunxian.seekingimmortals.item.material.MaterialRarity.UNCOMMON,
                    "Robe, talisman, and low-tier artifact refinement material"));
    public static final RegistryObject<Item> SOUL_GATHERING_STONE = ITEMS.register("soul_gathering_stone",
            () -> new com.xunxian.seekingimmortals.item.material.BaseMaterialItem(
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.UNCOMMON),
                    com.xunxian.seekingimmortals.item.material.MaterialCategory.SPECIAL,
                    com.xunxian.seekingimmortals.item.material.MaterialRarity.RARE,
                    "Nether River soul material for pills, talismans, and artifacts"));
    public static final RegistryObject<Item> KUNWU_COPPER = ITEMS.register("kunwu_copper",
            () -> new com.xunxian.seekingimmortals.item.material.BaseMaterialItem(
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE),
                    com.xunxian.seekingimmortals.item.material.MaterialCategory.MINERAL,
                    com.xunxian.seekingimmortals.item.material.MaterialRarity.RARE,
                    "Kunwu refinement copper for mid-tier artifact forging"));
    public static final RegistryObject<Item> GOLD_SEAM_STONE = ITEMS.register("gold_seam_stone",
            () -> new com.xunxian.seekingimmortals.item.material.BaseMaterialItem(
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE),
                    com.xunxian.seekingimmortals.item.material.MaterialCategory.MINERAL,
                    com.xunxian.seekingimmortals.item.material.MaterialRarity.RARE,
                    "Dajin gold-seam ore for metal artifact refinement"));
    // Wave55: decompress high-conflict material aliases into independent carriers.
    public static final RegistryObject<Item> DEMON_CORE_LOW = ITEMS.register("demon_core_low",
            () -> new com.xunxian.seekingimmortals.item.material.BaseMaterialItem(
                    new Item.Properties(),
                    com.xunxian.seekingimmortals.item.material.MaterialCategory.BEAST_MATERIAL,
                    com.xunxian.seekingimmortals.item.material.MaterialRarity.COMMON,
                    "Low-tier demon core for refinement and beast-path recipes"));
    public static final RegistryObject<Item> DEMON_CORE_MID = ITEMS.register("demon_core_mid",
            () -> new com.xunxian.seekingimmortals.item.material.BaseMaterialItem(
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.UNCOMMON),
                    com.xunxian.seekingimmortals.item.material.MaterialCategory.BEAST_MATERIAL,
                    com.xunxian.seekingimmortals.item.material.MaterialRarity.UNCOMMON,
                    "Mid-tier demon core for refinement and beast-path recipes"));
    public static final RegistryObject<Item> DEMON_CORE_HIGH = ITEMS.register("demon_core_high",
            () -> new com.xunxian.seekingimmortals.item.material.BaseMaterialItem(
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE),
                    com.xunxian.seekingimmortals.item.material.MaterialCategory.BEAST_MATERIAL,
                    com.xunxian.seekingimmortals.item.material.MaterialRarity.RARE,
                    "High-tier demon core for refinement and beast-path recipes"));
    public static final RegistryObject<Item> DEMON_CORE_FRAGMENT = ITEMS.register("demon_core_fragment",
            () -> new com.xunxian.seekingimmortals.item.material.BaseMaterialItem(
                    new Item.Properties(),
                    com.xunxian.seekingimmortals.item.material.MaterialCategory.BEAST_MATERIAL,
                    com.xunxian.seekingimmortals.item.material.MaterialRarity.COMMON,
                    "Broken demon-core fragment for low refinement costs"));
    public static final RegistryObject<Item> BEAST_SOUL_ESSENCE = ITEMS.register("beast_soul_essence",
            () -> new com.xunxian.seekingimmortals.item.material.BaseMaterialItem(
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE),
                    com.xunxian.seekingimmortals.item.material.MaterialCategory.BEAST_MATERIAL,
                    com.xunxian.seekingimmortals.item.material.MaterialRarity.RARE,
                    "Condensed beast soul essence for spirit-beast arts"));
    public static final RegistryObject<Item> LOW_SPIRIT_IRON = ITEMS.register("low_spirit_iron",
            () -> new com.xunxian.seekingimmortals.item.material.BaseMaterialItem(
                    new Item.Properties(),
                    com.xunxian.seekingimmortals.item.material.MaterialCategory.MINERAL,
                    com.xunxian.seekingimmortals.item.material.MaterialRarity.COMMON,
                    "Low-grade spirit iron for early artifact refinement"));
    public static final RegistryObject<Item> DEEP_SEA_COLD_IRON = ITEMS.register("deep_sea_cold_iron",
            () -> new com.xunxian.seekingimmortals.item.material.BaseMaterialItem(
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE),
                    com.xunxian.seekingimmortals.item.material.MaterialCategory.MINERAL,
                    com.xunxian.seekingimmortals.item.material.MaterialRarity.RARE,
                    "Deep-sea cold iron for water/ice artifact refinement"));
    public static final RegistryObject<Item> SCRAP_SPIRIT_IRON = ITEMS.register("scrap_spirit_iron",
            () -> new com.xunxian.seekingimmortals.item.material.BaseMaterialItem(
                    new Item.Properties(),
                    com.xunxian.seekingimmortals.item.material.MaterialCategory.MINERAL,
                    com.xunxian.seekingimmortals.item.material.MaterialRarity.COMMON,
                    "Scrap spirit iron recovered from broken artifacts"));
    public static final RegistryObject<Item> SPIRIT_IRON_INGOT_MID = ITEMS.register("spirit_iron_ingot_mid",
            () -> new com.xunxian.seekingimmortals.item.material.BaseMaterialItem(
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.UNCOMMON),
                    com.xunxian.seekingimmortals.item.material.MaterialCategory.MINERAL,
                    com.xunxian.seekingimmortals.item.material.MaterialRarity.UNCOMMON,
                    "Mid-grade spirit iron ingot for mid-tier refinement"));
    public static final RegistryObject<Item> GHOST_WOOD = ITEMS.register("ghost_wood",
            () -> new com.xunxian.seekingimmortals.item.material.BaseMaterialItem(
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.UNCOMMON),
                    com.xunxian.seekingimmortals.item.material.MaterialCategory.SPECIAL,
                    com.xunxian.seekingimmortals.item.material.MaterialRarity.UNCOMMON,
                    "Yin-path ghost wood for soul artifacts and ghost craft"));
    public static final RegistryObject<Item> JIAO_SCALE = ITEMS.register("jiao_scale",
            () -> new com.xunxian.seekingimmortals.item.material.BaseMaterialItem(
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE),
                    com.xunxian.seekingimmortals.item.material.MaterialCategory.BEAST_MATERIAL,
                    com.xunxian.seekingimmortals.item.material.MaterialRarity.RARE,
                    "Jiao scale distinct from true dragon scale material"));
    public static final RegistryObject<Item> SOUL_MOSS = ITEMS.register("soul_moss",
            () -> new com.xunxian.seekingimmortals.item.material.BaseMaterialItem(
                    new Item.Properties(),
                    com.xunxian.seekingimmortals.item.material.MaterialCategory.SPIRITUAL_HERB,
                    com.xunxian.seekingimmortals.item.material.MaterialRarity.COMMON,
                    "Soul moss used in ghost-path anchors and pills"));
    public static final RegistryObject<Item> GENG_GOLD_INLAY = ITEMS.register("geng_gold_inlay",
            () -> new com.xunxian.seekingimmortals.item.material.BaseMaterialItem(
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE),
                    com.xunxian.seekingimmortals.item.material.MaterialCategory.MINERAL,
                    com.xunxian.seekingimmortals.item.material.MaterialRarity.RARE,
                    "Geng-metal inlay pieces for metal artifact embossing"));
    public static final RegistryObject<Item> FIRE_FEATHER = ITEMS.register("fire_feather",
            () -> new com.xunxian.seekingimmortals.item.material.BaseMaterialItem(
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.UNCOMMON),
                    com.xunxian.seekingimmortals.item.material.MaterialCategory.BEAST_MATERIAL,
                    com.xunxian.seekingimmortals.item.material.MaterialRarity.UNCOMMON,
                    "Common fire feather, distinct from true phoenix feather"));
    public static final RegistryObject<Item> SPACE_CRYSTAL = ITEMS.register("space_crystal",
            () -> new com.xunxian.seekingimmortals.item.material.BaseMaterialItem(
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE),
                    com.xunxian.seekingimmortals.item.material.MaterialCategory.SPECIAL,
                    com.xunxian.seekingimmortals.item.material.MaterialRarity.RARE,
                    "Space crystal for storage and void-path refinement"));
    // Wave 0.1.441: decompress remaining vanilla material aliases.
    public static final RegistryObject<Item> WIND_FEATHER = ITEMS.register("wind_feather",
            () -> new com.xunxian.seekingimmortals.item.material.BaseMaterialItem(
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.UNCOMMON),
                    com.xunxian.seekingimmortals.item.material.MaterialCategory.BEAST_MATERIAL,
                    com.xunxian.seekingimmortals.item.material.MaterialRarity.UNCOMMON,
                    "Wind-aspect feather for sails and light artifacts"));
    public static final RegistryObject<Item> BEAST_HIDE = ITEMS.register("beast_hide",
            () -> new com.xunxian.seekingimmortals.item.material.BaseMaterialItem(
                    new Item.Properties(),
                    com.xunxian.seekingimmortals.item.material.MaterialCategory.BEAST_MATERIAL,
                    com.xunxian.seekingimmortals.item.material.MaterialRarity.COMMON,
                    "Tanned beast hide for armor and bridles"));
    public static final RegistryObject<Item> TURTLE_SHELL = ITEMS.register("turtle_shell",
            () -> new com.xunxian.seekingimmortals.item.material.BaseMaterialItem(
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.UNCOMMON),
                    com.xunxian.seekingimmortals.item.material.MaterialCategory.BEAST_MATERIAL,
                    com.xunxian.seekingimmortals.item.material.MaterialRarity.UNCOMMON,
                    "Hard turtle shell plate for puppet cores"));
    public static final RegistryObject<Item> POISON_SAC = ITEMS.register("poison_sac",
            () -> new com.xunxian.seekingimmortals.item.material.BaseMaterialItem(
                    new Item.Properties(),
                    com.xunxian.seekingimmortals.item.material.MaterialCategory.BEAST_MATERIAL,
                    com.xunxian.seekingimmortals.item.material.MaterialRarity.COMMON,
                    "Beast poison sac for needle and toxin recipes"));

    // Wave 0.1.442: decompress refinement soft-alias materials.
    public static final RegistryObject<Item> BEAST_BLOOD_VIAL = ITEMS.register("beast_blood_vial",
            () -> new com.xunxian.seekingimmortals.item.material.BaseMaterialItem(
                    new Item.Properties(),
                    com.xunxian.seekingimmortals.item.material.MaterialCategory.BEAST_MATERIAL,
                    com.xunxian.seekingimmortals.item.material.MaterialRarity.COMMON,
                    "Low-tier beast blood vial for refinement"));
    public static final RegistryObject<Item> TRUE_SPIRIT_BLOOD_DROP = ITEMS.register("true_spirit_blood_drop",
            () -> new com.xunxian.seekingimmortals.item.material.BaseMaterialItem(
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE),
                    com.xunxian.seekingimmortals.item.material.MaterialCategory.BEAST_MATERIAL,
                    com.xunxian.seekingimmortals.item.material.MaterialRarity.RARE,
                    "True-spirit blood drop for high refinement"));
    public static final RegistryObject<Item> BEAST_BONE_BLOCK = ITEMS.register("beast_bone_block",
            () -> new com.xunxian.seekingimmortals.item.material.BaseMaterialItem(
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.UNCOMMON),
                    com.xunxian.seekingimmortals.item.material.MaterialCategory.BEAST_MATERIAL,
                    com.xunxian.seekingimmortals.item.material.MaterialRarity.UNCOMMON,
                    "Bound beast bone block for bulk refinement"));
    public static final RegistryObject<Item> DEMON_CORRUPTION_FUNGUS = ITEMS.register("demon_corruption_fungus",
            () -> new com.xunxian.seekingimmortals.item.material.BaseMaterialItem(
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.UNCOMMON),
                    com.xunxian.seekingimmortals.item.material.MaterialCategory.SPIRITUAL_HERB,
                    com.xunxian.seekingimmortals.item.material.MaterialRarity.UNCOMMON,
                    "Demon-corruption fungus for dark artifacts"));
    public static final RegistryObject<Item> EARTH_SPINE_ROOT = ITEMS.register("earth_spine_root",
            () -> new com.xunxian.seekingimmortals.item.material.BaseMaterialItem(
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE),
                    com.xunxian.seekingimmortals.item.material.MaterialCategory.SPIRITUAL_HERB,
                    com.xunxian.seekingimmortals.item.material.MaterialRarity.RARE,
                    "Diyuan earth-spine root for shield refinement"));
    public static final RegistryObject<Item> SPACE_CRYSTAL_FRAGMENT = ITEMS.register("space_crystal_fragment",
            () -> new com.xunxian.seekingimmortals.item.material.BaseMaterialItem(
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.UNCOMMON),
                    com.xunxian.seekingimmortals.item.material.MaterialCategory.SPECIAL,
                    com.xunxian.seekingimmortals.item.material.MaterialRarity.UNCOMMON,
                    "Space crystal fragment for storage artifacts"));
    public static final RegistryObject<Item> STAR_SAND = ITEMS.register("star_sand",
            () -> new com.xunxian.seekingimmortals.item.material.BaseMaterialItem(
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.UNCOMMON),
                    com.xunxian.seekingimmortals.item.material.MaterialCategory.MINERAL,
                    com.xunxian.seekingimmortals.item.material.MaterialRarity.UNCOMMON,
                    "Star sand for star-path refinement"));
    public static final RegistryObject<Item> PUPPET_CORE_BLANK = ITEMS.register("puppet_core_blank",
            () -> new com.xunxian.seekingimmortals.item.material.BaseMaterialItem(
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.UNCOMMON),
                    com.xunxian.seekingimmortals.item.material.MaterialCategory.SPECIAL,
                    com.xunxian.seekingimmortals.item.material.MaterialRarity.UNCOMMON,
                    "Blank puppet core for puppet crafting and refinement"));
    public static final RegistryObject<Item> THUNDER_BAMBOO = ITEMS.register("thunder_bamboo",
            () -> new com.xunxian.seekingimmortals.item.material.BaseMaterialItem(
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE),
                    com.xunxian.seekingimmortals.item.material.MaterialCategory.SPIRITUAL_HERB,
                    com.xunxian.seekingimmortals.item.material.MaterialRarity.RARE,
                    "Thunder-aspect bamboo for talisman treasure refinement"));
    public static final RegistryObject<Item> ICE_FIRE_CRYSTAL = ITEMS.register("ice_fire_crystal",
            () -> new com.xunxian.seekingimmortals.item.material.BaseMaterialItem(
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.EPIC),
                    com.xunxian.seekingimmortals.item.material.MaterialCategory.MINERAL,
                    com.xunxian.seekingimmortals.item.material.MaterialRarity.EPIC,
                    "Chaotic Sea ice-fire crystal for high-tier artifact cores"));
    public static final RegistryObject<Item> VOID_MARROW = ITEMS.register("void_marrow",
            () -> new com.xunxian.seekingimmortals.item.material.BaseMaterialItem(
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.EPIC),
                    com.xunxian.seekingimmortals.item.material.MaterialCategory.SPECIAL,
                    com.xunxian.seekingimmortals.item.material.MaterialRarity.EPIC,
                    "Spirit Realm void marrow for alchemy and Void Refinement artifacts"));
    public static final RegistryObject<Item> XUANGUANG_MIRROR_SHARD = ITEMS.register("xuanguang_mirror_shard",
            () -> new com.xunxian.seekingimmortals.item.material.BaseMaterialItem(
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE).stacksTo(16),
                    com.xunxian.seekingimmortals.item.material.MaterialCategory.SPECIAL,
                    com.xunxian.seekingimmortals.item.material.MaterialRarity.RARE,
                    "Ancient Xuanguang Mirror shard for mirror reforging"));
    public static final RegistryObject<Item> XUANHUANG_MIRROR_SHARD = ITEMS.register("xuanhuang_mirror_shard",
            () -> new com.xunxian.seekingimmortals.item.material.BaseMaterialItem(
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE).stacksTo(16),
                    com.xunxian.seekingimmortals.item.material.MaterialCategory.SPECIAL,
                    com.xunxian.seekingimmortals.item.material.MaterialRarity.RARE,
                    "Ancient Xuanhuang Mirror shard for high-tier mirror reforging"));
    public static final RegistryObject<Item> NINE_DRAGON_CAULDRON_SHARD = ITEMS.register("nine_dragon_cauldron_shard",
            () -> new com.xunxian.seekingimmortals.item.material.BaseMaterialItem(
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.EPIC).stacksTo(16),
                    com.xunxian.seekingimmortals.item.material.MaterialCategory.SPECIAL,
                    com.xunxian.seekingimmortals.item.material.MaterialRarity.EPIC,
                    "Nine-Dragon Divine Fire Shroud shard for ancient treasure replicas"));
    public static final RegistryObject<Item> VOID_BELL_FRAGMENT = ITEMS.register("void_bell_fragment",
            () -> new com.xunxian.seekingimmortals.item.material.BaseMaterialItem(
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.EPIC).stacksTo(16),
                    com.xunxian.seekingimmortals.item.material.MaterialCategory.SPECIAL,
                    com.xunxian.seekingimmortals.item.material.MaterialRarity.EPIC,
                    "Void-Refining Bell fragment for late-realm bell reforging"));
    public static final RegistryObject<Item> DEMON_SUPPRESS_TALISMAN_BLANK = ITEMS.register("demon_suppress_talisman_blank",
            () -> new com.xunxian.seekingimmortals.item.material.BaseMaterialItem(
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE).stacksTo(16),
                    com.xunxian.seekingimmortals.item.material.MaterialCategory.SPECIAL,
                    com.xunxian.seekingimmortals.item.material.MaterialRarity.RARE,
                    "Blank talisman-treasure base for demon-suppressing seals"));
    public static final RegistryObject<Item> NATAL_ARTIFACT_EMBRYO = ITEMS.register("natal_artifact_embryo",
            () -> new com.xunxian.seekingimmortals.item.material.BaseMaterialItem(
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.EPIC).stacksTo(4),
                    com.xunxian.seekingimmortals.item.material.MaterialCategory.SPECIAL,
                    com.xunxian.seekingimmortals.item.material.MaterialRarity.EPIC,
                    "Core Formation natal artifact embryo for personalized artifact refinement"));
    public static final RegistryObject<Item> EIGHT_SPIRIT_RULER_SHARD = ITEMS.register("eight_spirit_ruler_shard",
            () -> new com.xunxian.seekingimmortals.item.material.BaseMaterialItem(
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.EPIC).stacksTo(16),
                    com.xunxian.seekingimmortals.item.material.MaterialCategory.SPECIAL,
                    com.xunxian.seekingimmortals.item.material.MaterialRarity.LEGENDARY,
                    "Eight-Spirit Ruler shard for Spirit Realm treasure replicas"));
    public static final RegistryObject<Item> DEMONIC_BLOOD_CORAL = ITEMS.register("demonic_blood_coral",
            () -> new com.xunxian.seekingimmortals.item.material.BaseMaterialItem(
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE),
                    com.xunxian.seekingimmortals.item.material.MaterialCategory.SPECIAL,
                    com.xunxian.seekingimmortals.item.material.MaterialRarity.RARE,
                    "Demonic dual-cultivation alchemy material"));

    public static final RegistryObject<Item> WASTE_PILL = ITEMS.register("waste_pill", () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> REJUVENATION_PILL_LOW = ITEMS.register("rejuvenation_pill_low", () -> new com.xunxian.seekingimmortals.item.pill.RejuvenationPill(new Item.Properties(), com.xunxian.seekingimmortals.item.pill.PillQuality.LOW));
    public static final RegistryObject<Item> FOUNDATION_BUILDING_PILL_LOW = ITEMS.register("foundation_building_pill_low", () -> new com.xunxian.seekingimmortals.item.pill.FoundationBuildingPill(new Item.Properties(), com.xunxian.seekingimmortals.item.pill.PillQuality.LOW));
    public static final RegistryObject<Item> FOUNDATION_BUILDING_PILL_MID = ITEMS.register("foundation_building_pill_mid", () -> new com.xunxian.seekingimmortals.item.pill.FoundationBuildingPill(new Item.Properties(), com.xunxian.seekingimmortals.item.pill.PillQuality.MEDIUM));
    public static final RegistryObject<Item> FOUNDATION_BUILDING_PILL_HIGH = ITEMS.register("foundation_building_pill_high", () -> new com.xunxian.seekingimmortals.item.pill.FoundationBuildingPill(new Item.Properties(), com.xunxian.seekingimmortals.item.pill.PillQuality.HIGH));
    public static final RegistryObject<Item> FOUNDATION_BUILDING_PILL_SUPREME = ITEMS.register("foundation_building_pill_supreme", () -> new com.xunxian.seekingimmortals.item.pill.FoundationBuildingPill(new Item.Properties(), com.xunxian.seekingimmortals.item.pill.PillQuality.SUPREME));
    public static final RegistryObject<Item> HEALING_PILL_LOW = ITEMS.register("healing_pill_low", () -> new com.xunxian.seekingimmortals.item.pill.HealingPill(new Item.Properties(), com.xunxian.seekingimmortals.item.pill.PillQuality.LOW));
    public static final RegistryObject<Item> CLEAR_SPIRIT_POWDER_LOW = ITEMS.register("clear_spirit_powder_low", () -> new com.xunxian.seekingimmortals.item.pill.ClearSpiritPowder(new Item.Properties(), com.xunxian.seekingimmortals.item.pill.PillQuality.LOW));
    public static final RegistryObject<Item> FASTING_PILL_LOW = ITEMS.register("fasting_pill_low", () -> new com.xunxian.seekingimmortals.item.pill.FastingPill(new Item.Properties(), com.xunxian.seekingimmortals.item.pill.PillQuality.LOW));
    public static final RegistryObject<Item> FASTING_PILL_MID = ITEMS.register("fasting_pill_mid", () -> new com.xunxian.seekingimmortals.item.pill.FastingPill(new Item.Properties(), com.xunxian.seekingimmortals.item.pill.PillQuality.MEDIUM));
    public static final RegistryObject<Item> FASTING_PILL_HIGH = ITEMS.register("fasting_pill_high", () -> new com.xunxian.seekingimmortals.item.pill.FastingPill(new Item.Properties(), com.xunxian.seekingimmortals.item.pill.PillQuality.HIGH));
    public static final RegistryObject<Item> FASTING_PILL_SUPREME = ITEMS.register("fasting_pill_supreme", () -> new com.xunxian.seekingimmortals.item.pill.FastingPill(new Item.Properties(), com.xunxian.seekingimmortals.item.pill.PillQuality.SUPREME));
    public static final RegistryObject<Item> CALMING_PILL_LOW = ITEMS.register("calming_pill_low", () -> new com.xunxian.seekingimmortals.item.pill.CalmingPill(new Item.Properties(), com.xunxian.seekingimmortals.item.pill.PillQuality.LOW));
    public static final RegistryObject<Item> CALMING_PILL_MID = ITEMS.register("calming_pill_mid", () -> new com.xunxian.seekingimmortals.item.pill.CalmingPill(new Item.Properties(), com.xunxian.seekingimmortals.item.pill.PillQuality.MEDIUM));
    public static final RegistryObject<Item> CALMING_PILL_HIGH = ITEMS.register("calming_pill_high", () -> new com.xunxian.seekingimmortals.item.pill.CalmingPill(new Item.Properties(), com.xunxian.seekingimmortals.item.pill.PillQuality.HIGH));
    public static final RegistryObject<Item> CALMING_PILL_SUPREME = ITEMS.register("calming_pill_supreme", () -> new com.xunxian.seekingimmortals.item.pill.CalmingPill(new Item.Properties(), com.xunxian.seekingimmortals.item.pill.PillQuality.SUPREME));
    public static final RegistryObject<Item> QINGXIN_PILL = ITEMS.register("qingxin_pill", () -> new com.xunxian.seekingimmortals.item.pill.CalmingPill(new Item.Properties(), com.xunxian.seekingimmortals.item.pill.PillQuality.LOW));
    public static final RegistryObject<Item> PRESSURE_RESIST_PILL = registerCatalogPill(CatalogPillType.PRESSURE_RESIST);
    public static final RegistryObject<Item> SPIRIT_REALM_CONDENSE_PILL = registerCatalogPill(CatalogPillType.SPIRIT_REALM_CONDENSE);
    public static final RegistryObject<Item> SPIRIT_GATHERING_PILL = registerCatalogPill(CatalogPillType.SPIRIT_GATHERING);
    public static final RegistryObject<Item> SPIRIT_GATHERING_PILL_MID = registerCatalogPill(CatalogPillType.SPIRIT_GATHERING, "mid", PillQuality.MEDIUM);
    public static final RegistryObject<Item> SPIRIT_GATHERING_PILL_HIGH = registerCatalogPill(CatalogPillType.SPIRIT_GATHERING, "high", PillQuality.HIGH);
    public static final RegistryObject<Item> SPIRIT_GATHERING_PILL_SUPREME = registerCatalogPill(CatalogPillType.SPIRIT_GATHERING, "supreme", PillQuality.SUPREME);
    public static final RegistryObject<Item> FIRE_ORIGIN_PILL = registerCatalogPill(CatalogPillType.FIRE_ORIGIN);
    public static final RegistryObject<Item> FIRE_ORIGIN_PILL_MID = registerCatalogPill(CatalogPillType.FIRE_ORIGIN, "mid", PillQuality.MEDIUM);
    public static final RegistryObject<Item> FIRE_ORIGIN_PILL_HIGH = registerCatalogPill(CatalogPillType.FIRE_ORIGIN, "high", PillQuality.HIGH);
    public static final RegistryObject<Item> FIRE_ORIGIN_PILL_SUPREME = registerCatalogPill(CatalogPillType.FIRE_ORIGIN, "supreme", PillQuality.SUPREME);
    public static final RegistryObject<Item> ICE_FIRE_PILL = registerCatalogPill(CatalogPillType.ICE_FIRE);
    public static final RegistryObject<Item> ICE_FIRE_PILL_MID = registerCatalogPill(CatalogPillType.ICE_FIRE, "mid", PillQuality.MEDIUM);
    public static final RegistryObject<Item> ICE_FIRE_PILL_HIGH = registerCatalogPill(CatalogPillType.ICE_FIRE, "high", PillQuality.HIGH);
    public static final RegistryObject<Item> ICE_FIRE_PILL_SUPREME = registerCatalogPill(CatalogPillType.ICE_FIRE, "supreme", PillQuality.SUPREME);
    public static final RegistryObject<Item> MARROW_CLEANSING_PILL = registerCatalogPill(CatalogPillType.MARROW_CLEANSING);
    public static final RegistryObject<Item> MARROW_CLEANSING_PILL_MID = registerCatalogPill(CatalogPillType.MARROW_CLEANSING, "mid", PillQuality.MEDIUM);
    public static final RegistryObject<Item> MARROW_CLEANSING_PILL_HIGH = registerCatalogPill(CatalogPillType.MARROW_CLEANSING, "high", PillQuality.HIGH);
    public static final RegistryObject<Item> MARROW_CLEANSING_PILL_SUPREME = registerCatalogPill(CatalogPillType.MARROW_CLEANSING, "supreme", PillQuality.SUPREME);
    public static final RegistryObject<Item> BODY_TEMPERING_PILL = registerCatalogPill(CatalogPillType.BODY_TEMPERING);
    public static final RegistryObject<Item> BODY_TEMPERING_PILL_MID = registerCatalogPill(CatalogPillType.BODY_TEMPERING, "mid", PillQuality.MEDIUM);
    public static final RegistryObject<Item> BODY_TEMPERING_PILL_HIGH = registerCatalogPill(CatalogPillType.BODY_TEMPERING, "high", PillQuality.HIGH);
    public static final RegistryObject<Item> BODY_TEMPERING_PILL_SUPREME = registerCatalogPill(CatalogPillType.BODY_TEMPERING, "supreme", PillQuality.SUPREME);
    public static final RegistryObject<Item> ESSENCE_CONDENSING_PILL = registerCatalogPill(CatalogPillType.ESSENCE_CONDENSING);
    public static final RegistryObject<Item> ESSENCE_CONDENSING_PILL_MID = registerCatalogPill(CatalogPillType.ESSENCE_CONDENSING, "mid", PillQuality.MEDIUM);
    public static final RegistryObject<Item> ESSENCE_CONDENSING_PILL_HIGH = registerCatalogPill(CatalogPillType.ESSENCE_CONDENSING, "high", PillQuality.HIGH);
    public static final RegistryObject<Item> ESSENCE_CONDENSING_PILL_SUPREME = registerCatalogPill(CatalogPillType.ESSENCE_CONDENSING, "supreme", PillQuality.SUPREME);
    public static final RegistryObject<Item> SOUL_GATHERING_PILL = registerCatalogPill(CatalogPillType.SOUL_GATHERING);
    public static final RegistryObject<Item> SOUL_GATHERING_PILL_MID = registerCatalogPill(CatalogPillType.SOUL_GATHERING, "mid", PillQuality.MEDIUM);
    public static final RegistryObject<Item> SOUL_GATHERING_PILL_HIGH = registerCatalogPill(CatalogPillType.SOUL_GATHERING, "high", PillQuality.HIGH);
    public static final RegistryObject<Item> SOUL_GATHERING_PILL_SUPREME = registerCatalogPill(CatalogPillType.SOUL_GATHERING, "supreme", PillQuality.SUPREME);
    public static final RegistryObject<Item> MARROW_REPAIR_PILL = registerCatalogPill(CatalogPillType.MARROW_REPAIR);
    public static final RegistryObject<Item> MARROW_REPAIR_PILL_MID = registerCatalogPill(CatalogPillType.MARROW_REPAIR, "mid", PillQuality.MEDIUM);
    public static final RegistryObject<Item> MARROW_REPAIR_PILL_HIGH = registerCatalogPill(CatalogPillType.MARROW_REPAIR, "high", PillQuality.HIGH);
    public static final RegistryObject<Item> MARROW_REPAIR_PILL_SUPREME = registerCatalogPill(CatalogPillType.MARROW_REPAIR, "supreme", PillQuality.SUPREME);
    public static final RegistryObject<Item> CLEAR_VOID_PILL = registerCatalogPill(CatalogPillType.CLEAR_VOID);
    public static final RegistryObject<Item> CLEAR_VOID_PILL_MID = registerCatalogPill(CatalogPillType.CLEAR_VOID, "mid", PillQuality.MEDIUM);
    public static final RegistryObject<Item> CLEAR_VOID_PILL_HIGH = registerCatalogPill(CatalogPillType.CLEAR_VOID, "high", PillQuality.HIGH);
    public static final RegistryObject<Item> CLEAR_VOID_PILL_SUPREME = registerCatalogPill(CatalogPillType.CLEAR_VOID, "supreme", PillQuality.SUPREME);
    public static final RegistryObject<Item> FORGET_DUST_PILL = registerCatalogPill(CatalogPillType.FORGET_DUST);
    public static final RegistryObject<Item> FORGET_DUST_PILL_MID = registerCatalogPill(CatalogPillType.FORGET_DUST, "mid", PillQuality.MEDIUM);
    public static final RegistryObject<Item> FORGET_DUST_PILL_HIGH = registerCatalogPill(CatalogPillType.FORGET_DUST, "high", PillQuality.HIGH);
    public static final RegistryObject<Item> FORGET_DUST_PILL_SUPREME = registerCatalogPill(CatalogPillType.FORGET_DUST, "supreme", PillQuality.SUPREME);
    public static final RegistryObject<Item> APPEARANCE_FIXING_PILL = registerCatalogPill(CatalogPillType.APPEARANCE_FIXING);
    public static final RegistryObject<Item> APPEARANCE_FIXING_PILL_MID = registerCatalogPill(CatalogPillType.APPEARANCE_FIXING, "mid", PillQuality.MEDIUM);
    public static final RegistryObject<Item> APPEARANCE_FIXING_PILL_HIGH = registerCatalogPill(CatalogPillType.APPEARANCE_FIXING, "high", PillQuality.HIGH);
    public static final RegistryObject<Item> APPEARANCE_FIXING_PILL_SUPREME = registerCatalogPill(CatalogPillType.APPEARANCE_FIXING, "supreme", PillQuality.SUPREME);
    public static final RegistryObject<Item> LONGEVITY_PILL = registerCatalogPill(CatalogPillType.LONGEVITY);
    public static final RegistryObject<Item> LONGEVITY_PILL_MID = registerCatalogPill(CatalogPillType.LONGEVITY, "mid", PillQuality.MEDIUM);
    public static final RegistryObject<Item> LONGEVITY_PILL_HIGH = registerCatalogPill(CatalogPillType.LONGEVITY, "high", PillQuality.HIGH);
    public static final RegistryObject<Item> LONGEVITY_PILL_SUPREME = registerCatalogPill(CatalogPillType.LONGEVITY, "supreme", PillQuality.SUPREME);
    public static final RegistryObject<Item> BLOOD_QI_PILL = registerCatalogPill(CatalogPillType.BLOOD_QI);
    public static final RegistryObject<Item> BLOOD_QI_PILL_MID = registerCatalogPill(CatalogPillType.BLOOD_QI, "mid", PillQuality.MEDIUM);
    public static final RegistryObject<Item> BLOOD_QI_PILL_HIGH = registerCatalogPill(CatalogPillType.BLOOD_QI, "high", PillQuality.HIGH);
    public static final RegistryObject<Item> BLOOD_QI_PILL_SUPREME = registerCatalogPill(CatalogPillType.BLOOD_QI, "supreme", PillQuality.SUPREME);
    public static final RegistryObject<Item> RETURN_YANG_TRUE_WATER = registerCatalogPill(CatalogPillType.RETURN_YANG_TRUE_WATER);
    public static final RegistryObject<Item> RETURN_YANG_TRUE_WATER_MID = registerCatalogPill(CatalogPillType.RETURN_YANG_TRUE_WATER, "mid", PillQuality.MEDIUM);
    public static final RegistryObject<Item> RETURN_YANG_TRUE_WATER_HIGH = registerCatalogPill(CatalogPillType.RETURN_YANG_TRUE_WATER, "high", PillQuality.HIGH);
    public static final RegistryObject<Item> RETURN_YANG_TRUE_WATER_SUPREME = registerCatalogPill(CatalogPillType.RETURN_YANG_TRUE_WATER, "supreme", PillQuality.SUPREME);
    public static final RegistryObject<Item> MARROW_EXTRACTING_PILL = registerCatalogPill(CatalogPillType.MARROW_EXTRACTING);
    public static final RegistryObject<Item> MARROW_EXTRACTING_PILL_MID = registerCatalogPill(CatalogPillType.MARROW_EXTRACTING, "mid", PillQuality.MEDIUM);
    public static final RegistryObject<Item> MARROW_EXTRACTING_PILL_HIGH = registerCatalogPill(CatalogPillType.MARROW_EXTRACTING, "high", PillQuality.HIGH);
    public static final RegistryObject<Item> MARROW_EXTRACTING_PILL_SUPREME = registerCatalogPill(CatalogPillType.MARROW_EXTRACTING, "supreme", PillQuality.SUPREME);
    public static final RegistryObject<Item> SOUL_BREAKING_PILL = registerCatalogPill(CatalogPillType.SOUL_BREAKING);
    public static final RegistryObject<Item> SOUL_BREAKING_PILL_MID = registerCatalogPill(CatalogPillType.SOUL_BREAKING, "mid", PillQuality.MEDIUM);
    public static final RegistryObject<Item> SOUL_BREAKING_PILL_HIGH = registerCatalogPill(CatalogPillType.SOUL_BREAKING, "high", PillQuality.HIGH);
    public static final RegistryObject<Item> SOUL_BREAKING_PILL_SUPREME = registerCatalogPill(CatalogPillType.SOUL_BREAKING, "supreme", PillQuality.SUPREME);
    public static final RegistryObject<Item> POISON_DRAGON_PEARL = registerCatalogPill(CatalogPillType.POISON_DRAGON_PEARL);
    public static final RegistryObject<Item> POISON_DRAGON_PEARL_MID = registerCatalogPill(CatalogPillType.POISON_DRAGON_PEARL, "mid", PillQuality.MEDIUM);
    public static final RegistryObject<Item> POISON_DRAGON_PEARL_HIGH = registerCatalogPill(CatalogPillType.POISON_DRAGON_PEARL, "high", PillQuality.HIGH);
    public static final RegistryObject<Item> POISON_DRAGON_PEARL_SUPREME = registerCatalogPill(CatalogPillType.POISON_DRAGON_PEARL, "supreme", PillQuality.SUPREME);

    public static final RegistryObject<Item> SPIRIT_CHARM = ITEMS.register("spirit_charm", () -> new SpiritCharmItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> FLYING_SWORD = ITEMS.register("flying_sword", () -> new FlyingArtifactItem(new Item.Properties(), true));
    public static final RegistryObject<Item> FLYING_ARTIFACT = ITEMS.register("flying_artifact", () -> new FlyingArtifactItem(new Item.Properties(), false));
    public static final RegistryObject<Item> FLYING_SWORD_LOW = registerArtifact("flying_sword_low");
    public static final RegistryObject<Item> CLOUD_BOOTS = registerArtifact("cloud_boots");
    public static final RegistryObject<Item> SPIRIT_GATHERING_BEAD = registerArtifact("spirit_gathering_bead");
    public static final RegistryObject<Item> YELLOW_UMBRELLA = registerArtifact("yellow_umbrella");
    public static final RegistryObject<Item> QINGYE_LEAF_FAN = registerArtifact("qingye_leaf_fan");
    public static final RegistryObject<Item> STORAGE_BRACELET_LOW = registerArtifact("storage_bracelet_low");
    public static final RegistryObject<Item> SNAKE_PEARL = registerArtifact("snake_pearl");
    public static final RegistryObject<Item> FLYING_NEEDLE_SET = registerArtifact("flying_needle_set");
    public static final RegistryObject<Item> BLACK_GOLD_SHIELD = registerArtifact("black_gold_shield");
    public static final RegistryObject<Item> BEDROCK_SHIELD = registerArtifact("bedrock_shield");
    public static final RegistryObject<Item> ARTIFACT_REPAIR_KIT = ITEMS.register("artifact_repair_kit",
            () -> new ArtifactCatalogItem(new Item.Properties().stacksTo(16), "artifact_repair_kit", false));
    public static final RegistryObject<Item> SILVER_GIANT_SWORD = registerArtifact("silver_giant_sword");
    public static final RegistryObject<Item> GOLD_DEMON_CHAIN = registerArtifact("gold_demon_chain");
    public static final RegistryObject<Item> EVIL_ILLUSION_MIRROR = registerArtifact("evil_illusion_mirror");
    public static final RegistryObject<Item> QINGNING_MIRROR = registerArtifact("qingning_mirror");
    public static final RegistryObject<Item> GOLD_LIGHT_BRICK = registerArtifact("gold_light_brick");
    public static final RegistryObject<Item> BEAST_TAMING_WHIP = registerArtifact("beast_taming_whip");
    public static final RegistryObject<Item> SPIRIT_BEAST_BRIDLE = registerArtifact("spirit_beast_bridle");
    public static final RegistryObject<Item> WIND_ESCAPE_SAIL = registerArtifact("wind_escape_sail");
    public static final RegistryObject<Item> MOON_SHADOW_DISK = registerArtifact("moon_shadow_disk");
    public static final RegistryObject<Item> TALISMAN_TREASURE_SOUL_CHARM = registerArtifact("talisman_treasure_soul_charm");
    public static final RegistryObject<Item> VOID_PALACE_COLD_JADE_PENDANT = registerArtifact("void_palace_cold_jade_pendant");
    public static final RegistryObject<Item> XUANGUANG_MIRROR = registerArtifact("xuanguang_mirror");
    public static final RegistryObject<Item> XUANHUANG_MIRROR = registerArtifact("xuanhuang_mirror");
    public static final RegistryObject<Item> NINE_DRAGON_CAULDRON_REPLICA = registerArtifact("nine_dragon_cauldron_replica");
    public static final RegistryObject<Item> VOID_REFINING_BELL = registerArtifact("void_refining_bell");
    public static final RegistryObject<Item> TALISMAN_TREASURE_DEMON_SEAL = registerArtifact("talisman_treasure_demon_seal");
    public static final RegistryObject<Item> NATAL_SWORD_EMBRYO = registerArtifact("natal_sword_embryo");
    public static final RegistryObject<Item> FOUR_SYMBOLS_RULER_REPLICA = registerArtifact("four_symbols_ruler_replica");
    public static final RegistryObject<Item> SEVEN_FLAME_FAN_REPLICA = registerArtifact("seven_flame_fan_replica");
    public static final RegistryObject<Item> THREE_FLAME_FAN_REPLICA = registerArtifact("three_flame_fan_replica");
    // Wave 0.1.441: missing refinement output carriers.
    public static final RegistryObject<Item> AZURE_ICE_SWORD = registerArtifact("azure_ice_sword");
    public static final RegistryObject<Item> AZURE_ROPE_NET = registerArtifact("azure_rope_net");
    public static final RegistryObject<Item> BEAST_SOUL_BELL = registerArtifact("beast_soul_bell");
    public static final RegistryObject<Item> BLACK_BOOTS = registerArtifact("black_boots");
    public static final RegistryObject<Item> BONE_WIND_CART = registerArtifact("bone_wind_cart");
    public static final RegistryObject<Item> DARK_IRON_RING = registerArtifact("dark_iron_ring");
    public static final RegistryObject<Item> DEMON_APE_ARMOR = registerArtifact("demon_ape_armor");
    public static final RegistryObject<Item> DRAGON_SCALE_ARMOR = registerArtifact("dragon_scale_armor");
    public static final RegistryObject<Item> FIRE_CROW_FAN = registerArtifact("fire_crow_fan");
    public static final RegistryObject<Item> FIRE_RAIN_NEEDLES = registerArtifact("fire_rain_needles");
    public static final RegistryObject<Item> FLAT_CROWN_REPLICA = registerArtifact("flat_crown_replica");
    public static final RegistryObject<Item> GIANT_APE_PUPPET_TOKEN = registerArtifact("giant_ape_puppet_token");
    public static final RegistryObject<Item> GIANT_TURTLE_PUPPET_CORE = registerArtifact("giant_turtle_puppet_core");
    public static final RegistryObject<Item> GLAZED_GUARD_SHIELD = registerArtifact("glazed_guard_shield");
    public static final RegistryObject<Item> GREEN_BAMBOO_CLOUD_SWORD = registerArtifact("green_bamboo_cloud_sword");
    public static final RegistryObject<Item> GREEN_BAMBOO_LEAF_SWORD = registerArtifact("green_bamboo_leaf_sword");
    public static final RegistryObject<Item> HUANGSI_ROBE_ARTIFACT = registerArtifact("huangsi_robe_artifact");
    public static final RegistryObject<Item> HUNYUAN_BOWL = registerArtifact("hunyuan_bowl");
    public static final RegistryObject<Item> HUNYUAN_BOWL_REPLICA = registerArtifact("hunyuan_bowl_replica");
    public static final RegistryObject<Item> ICE_FIRE_DUAL_ORB = registerArtifact("ice_fire_dual_orb");
    public static final RegistryObject<Item> INVISIBLE_NEEDLE_SET = registerArtifact("invisible_needle_set");
    public static final RegistryObject<Item> LENGYUE_BLADE = registerArtifact("lengyue_blade");
    public static final RegistryObject<Item> LIEYANG_SHORT_SWORD = registerArtifact("lieyang_short_sword");
    public static final RegistryObject<Item> PEERLESS_FLYING_KNIVES = registerArtifact("peerless_flying_knives");
    public static final RegistryObject<Item> PHOENIX_FEATHER_FAN = registerArtifact("phoenix_feather_fan");
    public static final RegistryObject<Item> POLUO_BEADS = registerArtifact("poluo_beads");
    public static final RegistryObject<Item> POTIAN_SHOVEL = registerArtifact("potian_shovel");
    public static final RegistryObject<Item> RED_THREAD_NEEDLES_REPLICA = registerArtifact("red_thread_needles_replica");
    public static final RegistryObject<Item> SCARLET_DRAGON_BLADE = registerArtifact("scarlet_dragon_blade");
    public static final RegistryObject<Item> SEVEN_STAR_DISK = registerArtifact("seven_star_disk");
    public static final RegistryObject<Item> SILVER_SPIRIT_MIRROR = registerArtifact("silver_spirit_mirror");
    public static final RegistryObject<Item> SOUL_CAPTURING_BELL = registerArtifact("soul_capturing_bell");
    public static final RegistryObject<Item> SOUL_GATHERING_BOWL = registerArtifact("soul_gathering_bowl");
    public static final RegistryObject<Item> SOUL_SUMMON_BELL = registerArtifact("soul_summon_bell");
    public static final RegistryObject<Item> TALISMAN_TREASURE_FIRE_SPEAR = registerArtifact("talisman_treasure_fire_spear");
    public static final RegistryObject<Item> TALISMAN_TREASURE_GOLDEN_WHEEL = registerArtifact("talisman_treasure_golden_wheel");
    public static final RegistryObject<Item> TALISMAN_TREASURE_ICE_SHIELD = registerArtifact("talisman_treasure_ice_shield");
    public static final RegistryObject<Item> TALISMAN_TREASURE_THUNDER_ROD = registerArtifact("talisman_treasure_thunder_rod");
    public static final RegistryObject<Item> THOUSAND_BEE_NEEDLES = registerArtifact("thousand_bee_needles");
    public static final RegistryObject<Item> THUNDER_PEARL_TALISMAN = registerArtifact("thunder_pearl_talisman");
    public static final RegistryObject<Item> VAJRA_SHIELD = registerArtifact("vajra_shield");
    public static final RegistryObject<Item> XUANGUANG_MIRROR_REPLICA = registerArtifact("xuanguang_mirror_replica");
    public static final RegistryObject<Item> XUANTIE_FLYING_SHIELD = registerArtifact("xuantie_flying_shield");
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
    public static final RegistryObject<Item> TELEPORT_ARRAY_PEDESTAL = ITEMS.register("teleport_array_pedestal", () -> new BlockItem(ModBlocks.TELEPORT_ARRAY_PEDESTAL.get(), new Item.Properties()));
    public static final RegistryObject<Item> SECT_GATE_ARRAY = ITEMS.register("sect_gate_array", () -> new BlockItem(ModBlocks.SECT_GATE_ARRAY.get(), new Item.Properties()));
    public static final RegistryObject<Item> BLOOD_SACRIFICE_ALTAR = ITEMS.register("blood_sacrifice_altar", () -> new BlockItem(ModBlocks.BLOOD_SACRIFICE_ALTAR.get(), new Item.Properties()));
    public static final RegistryObject<Item> THUNDER_TRIBULATION_ALTAR = ITEMS.register("thunder_tribulation_altar", () -> new BlockItem(ModBlocks.THUNDER_TRIBULATION_ALTAR.get(), new Item.Properties()));
    public static final RegistryObject<Item> SPIRIT_GATHERING_FORMATION_CORE = ITEMS.register("spirit_gathering_formation_core", () -> new BlockItem(ModBlocks.SPIRIT_GATHERING_FORMATION_CORE.get(), new Item.Properties()));
    public static final RegistryObject<Item> DEFENSE_FORMATION_CORE = ITEMS.register("defense_formation_core", () -> new BlockItem(ModBlocks.DEFENSE_FORMATION_CORE.get(), new Item.Properties()));
    public static final RegistryObject<Item> REFINEMENT_FORGE = ITEMS.register("refinement_forge", () -> new BlockItem(ModBlocks.REFINEMENT_FORGE.get(), new Item.Properties()));
    public static final RegistryObject<Item> SEAL_DEMON_FORMATION_CORE = ITEMS.register("seal_demon_formation_core", () -> new BlockItem(ModBlocks.SEAL_DEMON_FORMATION_CORE.get(), new Item.Properties()));
    public static final RegistryObject<Item> ILLUSION_MAZE_FORMATION_CORE = ITEMS.register("illusion_maze_formation_core", () -> new BlockItem(ModBlocks.ILLUSION_MAZE_FORMATION_CORE.get(), new Item.Properties()));
    public static final RegistryObject<Item> KILL_SWORD_FORMATION_CORE = ITEMS.register("kill_sword_formation_core", () -> new BlockItem(ModBlocks.KILL_SWORD_FORMATION_CORE.get(), new Item.Properties()));
    public static final RegistryObject<Item> ASCENSION_GATE = ITEMS.register("ascension_gate", () -> new BlockItem(ModBlocks.ASCENSION_GATE.get(), new Item.Properties()));
    public static final RegistryObject<Item> FIVE_ELEMENTS_MOUNTAIN_FORMATION_CORE = ITEMS.register("five_elements_mountain_formation_core", () -> new BlockItem(ModBlocks.FIVE_ELEMENTS_MOUNTAIN_FORMATION_CORE.get(), new Item.Properties()));
    public static final RegistryObject<Item> NINE_DRAGON_FLAME_BARRIER_FORMATION_CORE = ITEMS.register("nine_dragon_flame_barrier_formation_core", () -> new BlockItem(ModBlocks.NINE_DRAGON_FLAME_BARRIER_FORMATION_CORE.get(), new Item.Properties()));
    public static final RegistryObject<Item> INVERTED_FIVE_ELEMENTS_FORMATION_CORE = ITEMS.register("inverted_five_elements_formation_core", () -> new BlockItem(ModBlocks.INVERTED_FIVE_ELEMENTS_FORMATION_CORE.get(), new Item.Properties()));
    public static final RegistryObject<Item> VAJRA_PRISON_FORMATION_CORE = ITEMS.register("vajra_prison_formation_core", () -> new BlockItem(ModBlocks.VAJRA_PRISON_FORMATION_CORE.get(), new Item.Properties()));
    public static final RegistryObject<Item> MULAN_WIND_RIDE_FORMATION_CORE = ITEMS.register("mulan_wind_ride_formation_core", () -> new BlockItem(ModBlocks.MULAN_WIND_RIDE_FORMATION_CORE.get(), new Item.Properties()));
    public static final RegistryObject<Item> BARRIER_SECT_PROTECTION_FORMATION_CORE = ITEMS.register("barrier_sect_protection_formation_core", () -> new BlockItem(ModBlocks.BARRIER_SECT_PROTECTION_FORMATION_CORE.get(), new Item.Properties()));
    public static final RegistryObject<Item> SPIRIT_GATHERING_MINOR_FORMATION_CORE = ITEMS.register("spirit_gathering_minor_formation_core", () -> new BlockItem(ModBlocks.SPIRIT_GATHERING_MINOR_FORMATION_CORE.get(), new Item.Properties()));
    public static final RegistryObject<Item> DEMON_SEAL_PILLAR_FORMATION_CORE = ITEMS.register("demon_seal_pillar_formation_core", () -> new BlockItem(ModBlocks.DEMON_SEAL_PILLAR_FORMATION_CORE.get(), new Item.Properties()));
    public static final RegistryObject<Item> SWORD_ARRAY_BAGUA_FORMATION_CORE = ITEMS.register("sword_array_bagua_formation_core", () -> new BlockItem(ModBlocks.SWORD_ARRAY_BAGUA_FORMATION_CORE.get(), new Item.Properties()));
    public static final RegistryObject<Item> THUNDER_TRIBULATION_ARRAY_FORMATION_CORE = ITEMS.register("thunder_tribulation_array_formation_core", () -> new BlockItem(ModBlocks.THUNDER_TRIBULATION_ARRAY_FORMATION_CORE.get(), new Item.Properties()));
    public static final RegistryObject<Item> LONG_RANGE_TELEPORT_ARRAY = ITEMS.register("long_range_teleport_array", () -> new BlockItem(ModBlocks.LONG_RANGE_TELEPORT_ARRAY.get(), new Item.Properties()));
    public static final RegistryObject<Item> BLOOD_FORBIDDEN_GATE = ITEMS.register("blood_forbidden_gate", () -> new BlockItem(ModBlocks.BLOOD_FORBIDDEN_GATE.get(), new Item.Properties()));
    public static final RegistryObject<Item> NETHER_FERRY_GATE = ITEMS.register("nether_ferry_gate", () -> new BlockItem(ModBlocks.NETHER_FERRY_GATE.get(), new Item.Properties()));
    public static final RegistryObject<Item> ANCIENT_RIFT_GATE = ITEMS.register("ancient_rift_gate", () -> new BlockItem(ModBlocks.ANCIENT_RIFT_GATE.get(), new Item.Properties()));
    public static final RegistryObject<Item> CYCLE_GATE = ITEMS.register("cycle_gate", () -> new BlockItem(ModBlocks.CYCLE_GATE.get(), new Item.Properties()));
    public static final RegistryObject<Item> HIDDEN_RIFT_GATE = ITEMS.register("hidden_rift_gate", () -> new BlockItem(ModBlocks.HIDDEN_RIFT_GATE.get(), new Item.Properties()));
    public static final RegistryObject<Item> KING_TERRITORY_GATE = ITEMS.register("king_territory_gate", () -> new BlockItem(ModBlocks.KING_TERRITORY_GATE.get(), new Item.Properties()));
    public static final RegistryObject<Item> REFINEMENT_FORGE_G2 = ITEMS.register("refinement_forge_g2", () -> new BlockItem(ModBlocks.REFINEMENT_FORGE_G2.get(), new Item.Properties()));
    public static final RegistryObject<Item> TALISMAN_TABLE = ITEMS.register("talisman_table", () -> new BlockItem(ModBlocks.TALISMAN_TABLE.get(), new Item.Properties()));
    public static final RegistryObject<Item> PUPPET_ASSEMBLY_BENCH = ITEMS.register("puppet_assembly_bench", () -> new BlockItem(ModBlocks.PUPPET_ASSEMBLY_BENCH.get(), new Item.Properties()));
    public static final RegistryObject<Item> REFINEMENT_FORGE_G3 = ITEMS.register("refinement_forge_g3", () -> new BlockItem(ModBlocks.REFINEMENT_FORGE_G3.get(), new Item.Properties()));
    public static final RegistryObject<Item> REFINEMENT_FORGE_G4 = ITEMS.register("refinement_forge_g4", () -> new BlockItem(ModBlocks.REFINEMENT_FORGE_G4.get(), new Item.Properties()));
    public static final RegistryObject<Item> REFINEMENT_FORGE_G5 = ITEMS.register("refinement_forge_g5", () -> new BlockItem(ModBlocks.REFINEMENT_FORGE_G5.get(), new Item.Properties()));
    public static final RegistryObject<Item> REFINEMENT_FORGE_G6 = ITEMS.register("refinement_forge_g6", () -> new BlockItem(ModBlocks.REFINEMENT_FORGE_G6.get(), new Item.Properties()));
    public static final RegistryObject<Item> SPIRIT_HERB_PLANTER = ITEMS.register("spirit_herb_planter", () -> new BlockItem(ModBlocks.SPIRIT_HERB_PLANTER.get(), new Item.Properties()));
    public static final RegistryObject<Item> LOW_SPIRIT_IRON_ORE = ITEMS.register("low_spirit_iron_ore", () -> new BlockItem(ModBlocks.LOW_SPIRIT_IRON_ORE.get(), new Item.Properties()));
    public static final RegistryObject<Item> YIN_ESSENCE_ORE = ITEMS.register("yin_essence_ore", () -> new BlockItem(ModBlocks.YIN_ESSENCE_ORE.get(), new Item.Properties()));
    public static final RegistryObject<Item> LEYLINE_SURFACE_MARKER = ITEMS.register("leyline_surface_marker",
            () -> new BlockItem(ModBlocks.LEYLINE_SURFACE_MARKER.get(), new Item.Properties()));
    public static final RegistryObject<Item> ALCHEMY_FURNACE = ITEMS.register("alchemy_furnace", () -> new BlockItem(ModBlocks.ALCHEMY_FURNACE.get(), new Item.Properties()));
    public static final RegistryObject<Item> ALCHEMY_FURNACE_TIER_2 = ITEMS.register("alchemy_furnace_tier_2", () -> new BlockItem(ModBlocks.ALCHEMY_FURNACE_TIER_2.get(), new Item.Properties()));
    public static final RegistryObject<Item> ALCHEMY_FURNACE_TIER_3 = ITEMS.register("alchemy_furnace_tier_3", () -> new BlockItem(ModBlocks.ALCHEMY_FURNACE_TIER_3.get(), new Item.Properties()));
    public static final RegistryObject<Item> ALCHEMY_FURNACE_TIER_4 = ITEMS.register("alchemy_furnace_tier_4", () -> new BlockItem(ModBlocks.ALCHEMY_FURNACE_TIER_4.get(), new Item.Properties()));
    public static final RegistryObject<Item> ALCHEMY_FURNACE_TIER_5 = ITEMS.register("alchemy_furnace_tier_5", () -> new BlockItem(ModBlocks.ALCHEMY_FURNACE_TIER_5.get(), new Item.Properties()));
    public static final RegistryObject<Item> SECT_EARTH_FIRE_ROOM = ITEMS.register("sect_earth_fire_room", () -> new BlockItem(ModBlocks.SECT_EARTH_FIRE_ROOM.get(), new Item.Properties()));
    public static final RegistryObject<Item> ALCHEMY_FURNACE_ARRAY_NODE = ITEMS.register("alchemy_furnace_array_node",
            () -> new BlockItem(ModBlocks.ALCHEMY_FURNACE_ARRAY_NODE.get(), new Item.Properties()));
    public static final RegistryObject<Item> ALCHEMY_LID_LOW = ITEMS.register("alchemy_lid_low",
            () -> new BlockItem(ModBlocks.ALCHEMY_LID_LOW.get(), new Item.Properties().stacksTo(16)));
    public static final RegistryObject<Item> ALCHEMY_LID_MID = ITEMS.register("alchemy_lid_mid",
            () -> new BlockItem(ModBlocks.ALCHEMY_LID_MID.get(), new Item.Properties().stacksTo(16)));
    public static final RegistryObject<Item> ALCHEMY_LID_HIGH = ITEMS.register("alchemy_lid_high",
            () -> new BlockItem(ModBlocks.ALCHEMY_LID_HIGH.get(), new Item.Properties().stacksTo(16)));
    public static final RegistryObject<Item> ALCHEMY_LID_TIER_4 = ITEMS.register("alchemy_lid_tier_4",
            () -> new BlockItem(ModBlocks.ALCHEMY_LID_TIER_4.get(), new Item.Properties().stacksTo(16)));
    public static final RegistryObject<Item> ALCHEMY_LID_TIER_5 = ITEMS.register("alchemy_lid_tier_5",
            () -> new BlockItem(ModBlocks.ALCHEMY_LID_TIER_5.get(), new Item.Properties().stacksTo(16)));
    public static final RegistryObject<Item> DAN_FIRE_LOW = registerDanFire("dan_fire_low", 1, Realm.MORTAL, false);
    public static final RegistryObject<Item> DAN_FIRE_MID = registerDanFire("dan_fire_mid", 2, Realm.QI_REFINING, false);
    public static final RegistryObject<Item> DAN_FIRE_HIGH = registerDanFire("dan_fire_high", 3, Realm.FOUNDATION_ESTABLISHMENT, false);
    public static final RegistryObject<Item> EARTH_FIRE = registerDanFire("earth_fire", 4, Realm.CORE_FORMATION, true);
    public static final RegistryObject<Item> NASCENT_SOUL_FIRE = registerDanFire("nascent_soul_fire", 5, Realm.NASCENT_SOUL, false);
    public static final RegistryObject<Item> ALCHEMY_FORMULA_CULTIVATION_PILL_PAPER = registerAlchemyFormula("alchemy_formula_cultivation_pill_paper", "cultivate_speed_pill", AlchemyFormulaSource.PAPER);
    public static final RegistryObject<Item> ALCHEMY_FORMULA_QI_RECOVERY_PILL_JADE = registerAlchemyFormula("alchemy_formula_qi_recovery_pill_jade", "spirit_recovery_pill", AlchemyFormulaSource.JADE);
    public static final RegistryObject<Item> ALCHEMY_FORMULA_CALMING_PILL_JADE = registerAlchemyFormula("alchemy_formula_calming_pill_jade", "calming_pill_low", AlchemyFormulaSource.JADE);
    public static final RegistryObject<Item> ALCHEMY_FORMULA_QINGXIN_PILL_PAPER = registerAlchemyFormula("alchemy_formula_qingxin_pill_paper", "qingxin_pill", AlchemyFormulaSource.PAPER);
    public static final RegistryObject<Item> ALCHEMY_FORMULA_FOUNDATION_BUILDING_PILL_PAPER = registerAlchemyFormula("alchemy_formula_foundation_building_pill_paper", "foundation_building_pill_low", AlchemyFormulaSource.PAPER);
    public static final RegistryObject<Item> ALCHEMY_FORMULA_FOUNDATION_BUILDING_PILL_SECT = registerAlchemyFormula("alchemy_formula_foundation_building_pill_sect", "foundation_building_pill_low", AlchemyFormulaSource.SECT_SECRET);
    public static final RegistryObject<Item> ALCHEMY_FORMULA_SPIRIT_GATHERING_PILL_PAPER = registerAlchemyFormula("alchemy_formula_spirit_gathering_pill_paper", "spirit_gathering_pill", AlchemyFormulaSource.PAPER);
    public static final RegistryObject<Item> ALCHEMY_FORMULA_FIRE_ORIGIN_PILL_JADE = registerAlchemyFormula("alchemy_formula_fire_origin_pill_jade", "fire_origin_pill", AlchemyFormulaSource.JADE);
    public static final RegistryObject<Item> ALCHEMY_FORMULA_ICE_FIRE_PILL_JADE = registerAlchemyFormula("alchemy_formula_ice_fire_pill_jade", "ice_fire_pill", AlchemyFormulaSource.JADE);
    public static final RegistryObject<Item> ALCHEMY_FORMULA_MARROW_CLEANSING_PILL_JADE = registerAlchemyFormula("alchemy_formula_marrow_cleansing_pill_jade", "marrow_cleansing_pill", AlchemyFormulaSource.JADE);
    public static final RegistryObject<Item> ALCHEMY_FORMULA_BODY_TEMPERING_PILL_JADE = registerAlchemyFormula("alchemy_formula_body_tempering_pill_jade", "body_tempering_pill", AlchemyFormulaSource.JADE);
    public static final RegistryObject<Item> ALCHEMY_FORMULA_FASTING_PILL_PAPER = registerAlchemyFormula("alchemy_formula_fasting_pill_paper", "fasting_pill_low", AlchemyFormulaSource.PAPER);
    public static final RegistryObject<Item> ALCHEMY_FORMULA_ESSENCE_CONDENSING_PILL_JADE = registerAlchemyFormula("alchemy_formula_essence_condensing_pill_jade", "essence_condensing_pill", AlchemyFormulaSource.JADE);
    public static final RegistryObject<Item> ALCHEMY_FORMULA_SOUL_GATHERING_PILL_JADE = registerAlchemyFormula("alchemy_formula_soul_gathering_pill_jade", "soul_gathering_pill", AlchemyFormulaSource.JADE);
    public static final RegistryObject<Item> ALCHEMY_FORMULA_MARROW_REPAIR_PILL_JADE = registerAlchemyFormula("alchemy_formula_marrow_repair_pill_jade", "marrow_repair_pill", AlchemyFormulaSource.JADE);
    public static final RegistryObject<Item> ALCHEMY_FORMULA_CLEAR_VOID_PILL_PAPER = registerAlchemyFormula("alchemy_formula_clear_void_pill_paper", "clear_void_pill", AlchemyFormulaSource.PAPER);
    public static final RegistryObject<Item> ALCHEMY_FORMULA_FORGET_DUST_PILL_PAPER = registerAlchemyFormula("alchemy_formula_forget_dust_pill_paper", "forget_dust_pill", AlchemyFormulaSource.PAPER);
    public static final RegistryObject<Item> ALCHEMY_FORMULA_APPEARANCE_FIXING_PILL_JADE = registerAlchemyFormula("alchemy_formula_appearance_fixing_pill_jade", "appearance_fixing_pill", AlchemyFormulaSource.JADE);
    public static final RegistryObject<Item> ALCHEMY_FORMULA_LONGEVITY_PILL_SECT = registerAlchemyFormula("alchemy_formula_longevity_pill_sect", "longevity_pill", AlchemyFormulaSource.SECT_SECRET);
    public static final RegistryObject<Item> ALCHEMY_FORMULA_BLOOD_QI_PILL_JADE = registerAlchemyFormula("alchemy_formula_blood_qi_pill_jade", "blood_qi_pill", AlchemyFormulaSource.JADE);
    public static final RegistryObject<Item> ALCHEMY_FORMULA_RETURN_YANG_TRUE_WATER_SECT = registerAlchemyFormula("alchemy_formula_return_yang_true_water_sect", "return_yang_true_water", AlchemyFormulaSource.SECT_SECRET);
    public static final RegistryObject<Item> ALCHEMY_FORMULA_MARROW_EXTRACTING_PILL_PAPER = registerAlchemyFormula("alchemy_formula_marrow_extracting_pill_paper", "marrow_extracting_pill", AlchemyFormulaSource.PAPER);
    public static final RegistryObject<Item> ALCHEMY_FORMULA_SOUL_BREAKING_PILL_PAPER = registerAlchemyFormula("alchemy_formula_soul_breaking_pill_paper", "soul_breaking_pill", AlchemyFormulaSource.PAPER);
    public static final RegistryObject<Item> ALCHEMY_FORMULA_POISON_DRAGON_PEARL_JADE = registerAlchemyFormula("alchemy_formula_poison_dragon_pearl_jade", "poison_dragon_pearl", AlchemyFormulaSource.JADE);
    public static final RegistryObject<Item> ALCHEMY_FORMULA_PRESSURE_RESIST_PILL_SECT = registerAlchemyFormula("alchemy_formula_pressure_resist_pill_sect", "pressure_resist_pill", AlchemyFormulaSource.SECT_SECRET);
    public static final RegistryObject<Item> ALCHEMY_FORMULA_SPIRIT_REALM_CONDENSE_PILL_SECT = registerAlchemyFormula("alchemy_formula_spirit_realm_condense_pill_sect", "spirit_realm_condense_pill", AlchemyFormulaSource.SECT_SECRET);
    public static final RegistryObject<Item> MYSTIC_VIAL = ITEMS.register("mystic_vial", () -> new MysticVialItem(new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE)));
    public static final RegistryObject<Item> SEVEN_MYSTERIES_EVIDENCE = ITEMS.register("seven_mysteries_evidence", () -> new Item(new Item.Properties().stacksTo(16)));
    public static final RegistryObject<Item> STAR_PALACE_TAX_RECEIPT = ITEMS.register("star_palace_tax_receipt", () -> new StarPalaceTaxReceiptItem(new Item.Properties()));
    public static final RegistryObject<Item> DIYUAN_PERMIT = ITEMS.register("diyuan_permit", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> WIND_FEATHER_RAFT_TICKET = ITEMS.register("wind_feather_raft_ticket", () -> new Item(new Item.Properties()));
    // Wave42: spatial-node ticket/permit proxies for text-material requires hard gates.
    public static final RegistryObject<Item> CHAOTIC_SEA_TELEPORT_PERMIT = ITEMS.register("chaotic_sea_teleport_permit", () -> new Item(new Item.Properties().stacksTo(16)));
    public static final RegistryObject<Item> AUCTION_INVITE = ITEMS.register("auction_invite", () -> new Item(new Item.Properties().stacksTo(16)));
    public static final RegistryObject<Item> MULAN_PASS = ITEMS.register("mulan_pass", () -> new Item(new Item.Properties().stacksTo(16)));
    public static final RegistryObject<Item> FERRY_PASS = ITEMS.register("ferry_pass",
            () -> new CatalogConsumableItem(
                    new Item.Properties().stacksTo(16),
                    com.xunxian.seekingimmortals.item.material.MaterialCategory.SPECIAL,
                    com.xunxian.seekingimmortals.item.material.MaterialRarity.UNCOMMON,
                    "阴冥渡口符",
                    BulkItemClassifier.consumable("ferry_pass").orElseThrow()));
    public static final RegistryObject<Item> VOID_PALACE_KEY_FRAGMENT = ITEMS.register("void_palace_key_fragment", () -> new Item(new Item.Properties().stacksTo(16).rarity(net.minecraft.world.item.Rarity.UNCOMMON)));
    public static final RegistryObject<Item> SPACE_RIFT_COMPASS = ITEMS.register("space_rift_compass", () -> new Item(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> BORDER_MERIT_TOKEN = ITEMS.register("border_merit_token", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final RegistryObject<Item> SECT_PERMIT = ITEMS.register("sect_permit", () -> new Item(new Item.Properties().stacksTo(16)));
    public static final RegistryObject<Item> INVERSE_STAR_CONTACT = ITEMS.register("inverse_star_contact", () -> new Item(new Item.Properties().stacksTo(16)));
    public static final RegistryObject<Item> PRESSURE_RESIST_CHARM = ITEMS.register("pressure_resist_charm", () -> new PressureResistCharmItem(new Item.Properties().rarity(net.minecraft.world.item.Rarity.UNCOMMON)));
    public static final RegistryObject<Item> YIN_BODY_PROTECTION_CHARM = ITEMS.register("yin_body_protection_charm", () -> new YinProtectionCharmItem(new Item.Properties().rarity(net.minecraft.world.item.Rarity.UNCOMMON)));
    public static final RegistryObject<Item> TECHNIQUE_MANUAL_HUANGLONG_METHOD = registerTechniqueManual("technique_manual_huanglong_method", "\u9ec4\u9f99\u529f");
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
    // Wave 0.1.443: manuals_catalog physical carriers.
    public static final RegistryObject<Item> REFINEMENT_MANUAL_LOW = ITEMS.register("refinement_manual_low",
            () -> new CatalogManualItem(new Item.Properties().stacksTo(16), "refinement_manual_low"));
    public static final RegistryObject<Item> REFINEMENT_MANUAL_MID = ITEMS.register("refinement_manual_mid",
            () -> new CatalogManualItem(new Item.Properties().stacksTo(16), "refinement_manual_mid"));
    public static final RegistryObject<Item> REFINEMENT_MANUAL_ANCIENT = ITEMS.register("refinement_manual_ancient",
            () -> new CatalogManualItem(new Item.Properties().stacksTo(16), "refinement_manual_ancient"));
    public static final RegistryObject<Item> RECIPE_REFINE_FLYING_SWORD = ITEMS.register("recipe_refine_flying_sword",
            () -> new CatalogManualItem(new Item.Properties().stacksTo(16), "recipe_refine_flying_sword"));
    public static final RegistryObject<Item> RECIPE_REFINE_EVIL_MIRROR = ITEMS.register("recipe_refine_evil_mirror",
            () -> new CatalogManualItem(new Item.Properties().stacksTo(16), "recipe_refine_evil_mirror"));
    public static final RegistryObject<Item> RECIPE_REFINE_GIANT_TURTLE = ITEMS.register("recipe_refine_giant_turtle",
            () -> new CatalogManualItem(new Item.Properties().stacksTo(16), "recipe_refine_giant_turtle"));
    public static final RegistryObject<Item> ANCIENT_PUPPET_METHOD = ITEMS.register("ancient_puppet_method",
            () -> new CatalogManualItem(new Item.Properties().stacksTo(16), "ancient_puppet_method"));
    public static final RegistryObject<Item> GHOST_CULTIVATION_MANUAL = ITEMS.register("ghost_cultivation_manual",
            () -> new CatalogManualItem(new Item.Properties().stacksTo(16), "ghost_cultivation_manual"));
    public static final RegistryObject<Item> FASHI_ARRAY_MANUAL = ITEMS.register("fashi_array_manual",
            () -> new CatalogManualItem(new Item.Properties().stacksTo(16), "fashi_array_manual"));
    public static final RegistryObject<Item> ARRAY_BLUEPRINT_SCROLL = ITEMS.register("array_blueprint_scroll",
            () -> new CatalogManualItem(new Item.Properties().stacksTo(16), "array_blueprint_scroll"));
    public static final RegistryObject<Item> RECIPE_FOUNDATION = ITEMS.register("recipe_foundation",
            () -> new CatalogManualItem(new Item.Properties().stacksTo(16), "recipe_foundation"));
    public static final RegistryObject<Item> RECIPE_BU_TIAN = ITEMS.register("recipe_bu_tian",
            () -> new CatalogManualItem(new Item.Properties().stacksTo(16), "recipe_bu_tian"));
    public static final RegistryObject<Item> ILLUSION_TALISMAN_SCROLL = ITEMS.register("illusion_talisman_scroll",
            () -> new CatalogManualItem(new Item.Properties().stacksTo(16), "illusion_talisman_scroll"));
    public static final RegistryObject<Item> DAYAN_SOLUTION_FRAGMENT = ITEMS.register("dayan_solution_fragment",
            () -> new CatalogManualItem(new Item.Properties().stacksTo(16), "dayan_solution_fragment"));
    public static final RegistryObject<Item> STOLEN_JADE_SLIP = ITEMS.register("stolen_jade_slip",
            () -> new CatalogManualItem(new Item.Properties().stacksTo(16), "stolen_jade_slip"));
    public static final RegistryObject<Item> MANUAL_DAYAN_TRUE_SOLUTION = ITEMS.register("manual_dayan_true_solution",
            () -> new CatalogManualItem(new Item.Properties().stacksTo(16), "manual_dayan_true_solution"));
    public static final RegistryObject<Item> MANUAL_ANCIENT_PUPPET_ART = ITEMS.register("manual_ancient_puppet_art",
            () -> new CatalogManualItem(new Item.Properties().stacksTo(16), "manual_ancient_puppet_art"));

    // 傀儡与灵兽系统
    public static final RegistryObject<Item> PUPPET_REPAIR_KIT = ITEMS.register("puppet_repair_kit",
            () -> new CatalogConsumableItem(
                    new Item.Properties().stacksTo(16),
                    com.xunxian.seekingimmortals.item.material.MaterialCategory.SPECIAL,
                    com.xunxian.seekingimmortals.item.material.MaterialRarity.COMMON,
                    "傀儡修缮包",
                    BulkItemClassifier.consumable("puppet_repair_kit").orElseThrow()));
    public static final RegistryObject<Item> SPIRIT_BEAST_FEED = ITEMS.register("spirit_beast_feed",
            () -> new CatalogConsumableItem(
                    new Item.Properties().stacksTo(64),
                    com.xunxian.seekingimmortals.item.material.MaterialCategory.SPECIAL,
                    com.xunxian.seekingimmortals.item.material.MaterialRarity.COMMON,
                    "灵兽饵",
                    BulkItemClassifier.consumable("spirit_beast_feed").orElseThrow()));
    public static final RegistryObject<Item> BEAST_FEED_SPIRIT = ITEMS.register("beast_feed_spirit",
            () -> new CatalogConsumableItem(
                    new Item.Properties().stacksTo(64),
                    com.xunxian.seekingimmortals.item.material.MaterialCategory.SPECIAL,
                    com.xunxian.seekingimmortals.item.material.MaterialRarity.COMMON,
                    "灵兽饵",
                    BulkItemClassifier.consumable("beast_feed_spirit").orElseThrow()));

    // 材料系统
    public static final RegistryObject<Item> SPIRIT_GRASS = registerMaterial("spirit_grass", com.xunxian.seekingimmortals.item.material.MaterialType.SPIRIT_GRASS);
    public static final RegistryObject<Item> BIYUN_GRASS = ITEMS.register("biyun_grass", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> WANNIAN_SPIRIT_GRASS = ITEMS.register("wannian_spirit_grass", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> BLOOD_LINGZHI = ITEMS.register("blood_lingzhi", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> TIAN_LING_GRASS = ITEMS.register("tian_ling_grass", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> DING_SHEN_GRASS = ITEMS.register("ding_shen_grass", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> HUI_LING_GRASS = ITEMS.register("hui_ling_grass", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> YU_LU = ITEMS.register("yu_lu", () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> CLOUD_MUSHROOM = registerMaterial("cloud_mushroom", com.xunxian.seekingimmortals.item.material.MaterialType.CLOUD_MUSHROOM);
    public static final RegistryObject<Item> PHOENIX_FEATHER_FLOWER = registerMaterial("phoenix_feather_flower", com.xunxian.seekingimmortals.item.material.MaterialType.PHOENIX_FEATHER_FLOWER);
    public static final RegistryObject<Item> DIYUAN_PRESSURE_MOSS = ITEMS.register("diyuan_pressure_moss",
            () -> new com.xunxian.seekingimmortals.item.material.BaseMaterialItem(
                    new Item.Properties(),
                    com.xunxian.seekingimmortals.item.material.MaterialCategory.SPIRITUAL_HERB,
                    com.xunxian.seekingimmortals.item.material.MaterialRarity.RARE,
                    "Diyuan pressure-resist alchemy material"));
    public static final RegistryObject<Item> FENGYUAN_CLAN_GINSENG = ITEMS.register("fengyuan_clan_ginseng",
            () -> new com.xunxian.seekingimmortals.item.material.BaseMaterialItem(
                    new Item.Properties(),
                    com.xunxian.seekingimmortals.item.material.MaterialCategory.SPIRITUAL_HERB,
                    com.xunxian.seekingimmortals.item.material.MaterialRarity.RARE,
                    "Spirit Fengyuan clan ginseng alchemy material"));
    public static final RegistryObject<Item> DRAGON_BLOOD_GRASS = registerMaterial("dragon_blood_grass", com.xunxian.seekingimmortals.item.material.MaterialType.DRAGON_BLOOD_GRASS);
    public static final RegistryObject<Item> IMMORTAL_GINSENG = registerMaterial("immortal_ginseng", com.xunxian.seekingimmortals.item.material.MaterialType.IMMORTAL_GINSENG);
    public static final RegistryObject<Item> BEAST_CORE = registerMaterial("beast_core", com.xunxian.seekingimmortals.item.material.MaterialType.BEAST_CORE);
    public static final RegistryObject<Item> SPIRIT_BEAST_BONE = registerMaterial("spirit_beast_bone", com.xunxian.seekingimmortals.item.material.MaterialType.SPIRIT_BEAST_BONE);
    public static final RegistryObject<Item> DRAGON_SCALE = registerMaterial("dragon_scale", com.xunxian.seekingimmortals.item.material.MaterialType.DRAGON_SCALE);
    public static final RegistryObject<Item> PHOENIX_FEATHER = registerMaterial("phoenix_feather", com.xunxian.seekingimmortals.item.material.MaterialType.PHOENIX_FEATHER);
    public static final RegistryObject<Item> TRUE_DRAGON_BLOOD = registerMaterial("true_dragon_blood", com.xunxian.seekingimmortals.item.material.MaterialType.TRUE_DRAGON_BLOOD);
    public static final RegistryObject<Item> SPIRIT_IRON = registerMaterial("spirit_iron", com.xunxian.seekingimmortals.item.material.MaterialType.SPIRIT_IRON);
    public static final RegistryObject<Item> COLD_JADE = registerMaterial("cold_jade", com.xunxian.seekingimmortals.item.material.MaterialType.COLD_JADE);
    public static final RegistryObject<Item> HUNDRED_YEAR_ICE = ITEMS.register("hundred_year_ice", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> VOID_PALACE_COLD_JADE = ITEMS.register("void_palace_cold_jade", () -> new Item(new Item.Properties()));

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

    private static RegistryObject<Item> registerDanFire(String name, int tier, Realm minRealm, boolean requiresEarthFireRoom) {
        return ITEMS.register(name, () -> new AlchemyTieredItem(new Item.Properties().stacksTo(16), AlchemyTieredItem.ComponentType.FIRE, tier, minRealm, requiresEarthFireRoom));
    }

    private static RegistryObject<Item> registerAlchemyFormula(String name, String recipeId, AlchemyFormulaSource source) {
        return ITEMS.register(name, () -> new AlchemyFormulaItem(new Item.Properties().stacksTo(1), recipeId, source));
    }

    private static RegistryObject<Item> registerArtifact(String name) {
        return ITEMS.register(name, () -> new ArtifactCatalogItem(new Item.Properties().stacksTo(1), name));
    }

    private static RegistryObject<Item> registerCatalogPill(CatalogPillType type) {
        return ITEMS.register(type.id(), () -> new CatalogPillItem(new Item.Properties().rarity(type.rarity()), type, PillQuality.LOW));
    }

    private static RegistryObject<Item> registerCatalogPill(CatalogPillType type, String suffix, PillQuality quality) {
        return ITEMS.register(type.id() + "_" + suffix, () -> new CatalogPillItem(new Item.Properties().rarity(type.rarity()), type, quality));
    }

    private ModItems() {}
    public static void register(IEventBus bus) { ITEMS.register(bus); }
}
