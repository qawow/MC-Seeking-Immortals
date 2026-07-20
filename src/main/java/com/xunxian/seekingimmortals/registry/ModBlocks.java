package com.xunxian.seekingimmortals.registry;

import com.xunxian.seekingimmortals.SeekingImmortalsMod;
import com.xunxian.seekingimmortals.block.AlchemyFurnaceBlock;
import com.xunxian.seekingimmortals.block.AlchemyLidBlock;
import com.xunxian.seekingimmortals.block.EarthWallBlock;
import com.xunxian.seekingimmortals.block.LingGenIdentificationSlabBlock;
import com.xunxian.seekingimmortals.block.MeditationCushionBlock;
import com.xunxian.seekingimmortals.block.SpiritGatheringArrayBlock;
import com.xunxian.seekingimmortals.block.TeleportArrayPedestalBlock;
import com.xunxian.seekingimmortals.block.SectGateArrayBlock;
import com.xunxian.seekingimmortals.block.BloodSacrificeAltarBlock;
import com.xunxian.seekingimmortals.block.ThunderTribulationAltarBlock;
import com.xunxian.seekingimmortals.block.SpiritGatheringFormationCoreBlock;
import com.xunxian.seekingimmortals.block.DefenseFormationCoreBlock;
import com.xunxian.seekingimmortals.block.RefinementForgeBlock;
import com.xunxian.seekingimmortals.block.SealDemonFormationCoreBlock;
import com.xunxian.seekingimmortals.block.IllusionMazeFormationCoreBlock;
import com.xunxian.seekingimmortals.block.KillSwordFormationCoreBlock;
import com.xunxian.seekingimmortals.block.AscensionGateBlock;
import com.xunxian.seekingimmortals.block.BloodForbiddenGateBlock;
import com.xunxian.seekingimmortals.block.AncientRiftGateBlock;
import com.xunxian.seekingimmortals.block.CycleGateBlock;
import com.xunxian.seekingimmortals.block.HiddenRiftGateBlock;
import com.xunxian.seekingimmortals.block.KingTerritoryGateBlock;
import com.xunxian.seekingimmortals.block.NetherFerryGateBlock;
import com.xunxian.seekingimmortals.block.RefinementForgeG2Block;
import com.xunxian.seekingimmortals.block.CatalogFormationCoreBlock;
import com.xunxian.seekingimmortals.block.LongRangeTeleportArrayBlock;
import com.xunxian.seekingimmortals.block.PuppetAssemblyBenchBlock;
import com.xunxian.seekingimmortals.block.RefinementForgeG3Block;
import com.xunxian.seekingimmortals.block.RefinementForgeHighBlock;
import com.xunxian.seekingimmortals.block.SpiritHerbPlanterBlock;
import com.xunxian.seekingimmortals.block.TalismanTableBlock;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, SeekingImmortalsMod.MODID);

    public static final RegistryObject<Block> SPIRIT_ORE = BLOCKS.register("spirit_ore", () -> new DropExperienceBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(3.0F, 3.0F).requiresCorrectToolForDrops().sound(SoundType.STONE),
            UniformInt.of(2, 5)));

    public static final RegistryObject<Block> MEDITATION_CUSHION = BLOCKS.register("meditation_cushion", () -> new MeditationCushionBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(0.4F).sound(SoundType.WOOL).noOcclusion()));

    public static final RegistryObject<Block> LING_GEN_IDENTIFICATION_SLAB = BLOCKS.register("ling_gen_identification_slab", () -> new LingGenIdentificationSlabBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(1.2F, 3.0F).requiresCorrectToolForDrops().sound(SoundType.AMETHYST).noOcclusion()));

    public static final RegistryObject<Block> SPIRIT_GATHERING_ARRAY = BLOCKS.register("spirit_gathering_array", () -> new SpiritGatheringArrayBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN).strength(1.5F, 3.0F).requiresCorrectToolForDrops().sound(SoundType.AMETHYST).noOcclusion()));

    public static final RegistryObject<Block> TELEPORT_ARRAY_PEDESTAL = BLOCKS.register("teleport_array_pedestal", () -> new TeleportArrayPedestalBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE).strength(2.0F, 6.0F).requiresCorrectToolForDrops().sound(SoundType.STONE).noOcclusion()));

    public static final RegistryObject<Block> SECT_GATE_ARRAY = BLOCKS.register("sect_gate_array", () -> new SectGateArrayBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(2.5F, 8.0F).requiresCorrectToolForDrops().sound(SoundType.STONE).noOcclusion()));

    public static final RegistryObject<Block> BLOOD_SACRIFICE_ALTAR = BLOCKS.register("blood_sacrifice_altar", () -> new BloodSacrificeAltarBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(2.0F, 6.0F).requiresCorrectToolForDrops().sound(SoundType.STONE).noOcclusion()));

    public static final RegistryObject<Block> THUNDER_TRIBULATION_ALTAR = BLOCKS.register("thunder_tribulation_altar", () -> new ThunderTribulationAltarBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_YELLOW).strength(2.5F, 8.0F).requiresCorrectToolForDrops().sound(SoundType.METAL).noOcclusion()));

    public static final RegistryObject<Block> SPIRIT_GATHERING_FORMATION_CORE = BLOCKS.register("spirit_gathering_formation_core", () -> new SpiritGatheringFormationCoreBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN).strength(1.8F, 4.0F).requiresCorrectToolForDrops().sound(SoundType.AMETHYST).noOcclusion()));

    public static final RegistryObject<Block> DEFENSE_FORMATION_CORE = BLOCKS.register("defense_formation_core", () -> new DefenseFormationCoreBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BROWN).strength(2.2F, 8.0F).requiresCorrectToolForDrops().sound(SoundType.STONE).noOcclusion()));

    public static final RegistryObject<Block> REFINEMENT_FORGE = BLOCKS.register("refinement_forge", () -> new RefinementForgeBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.5F, 10.0F).requiresCorrectToolForDrops().sound(SoundType.ANVIL).noOcclusion()));

    public static final RegistryObject<Block> SEAL_DEMON_FORMATION_CORE = BLOCKS.register("seal_demon_formation_core", () -> new SealDemonFormationCoreBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(2.5F, 9.0F).requiresCorrectToolForDrops().sound(SoundType.STONE).noOcclusion()));

    public static final RegistryObject<Block> ILLUSION_MAZE_FORMATION_CORE = BLOCKS.register("illusion_maze_formation_core", () -> new IllusionMazeFormationCoreBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(1.8F, 4.0F).requiresCorrectToolForDrops().sound(SoundType.AMETHYST).noOcclusion()));

    public static final RegistryObject<Block> KILL_SWORD_FORMATION_CORE = BLOCKS.register("kill_sword_formation_core", () -> new KillSwordFormationCoreBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(2.5F, 8.0F).requiresCorrectToolForDrops().sound(SoundType.METAL).noOcclusion()));

    public static final RegistryObject<Block> ASCENSION_GATE = BLOCKS.register("ascension_gate", () -> new AscensionGateBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.GOLD).strength(4.0F, 12.0F).requiresCorrectToolForDrops().sound(SoundType.AMETHYST).noOcclusion()));

    public static final RegistryObject<Block> FIVE_ELEMENTS_MOUNTAIN_FORMATION_CORE = BLOCKS.register("five_elements_mountain_formation_core",
            () -> new CatalogFormationCoreBlock(BlockBehaviour.Properties.of().mapColor(MapColor.DIRT).strength(2.5F, 8.0F).requiresCorrectToolForDrops().sound(SoundType.STONE).noOcclusion(),
                    CatalogFormationCoreBlock.FormationKind.FIVE_ELEMENTS_MOUNTAIN));
    public static final RegistryObject<Block> NINE_DRAGON_FLAME_BARRIER_FORMATION_CORE = BLOCKS.register("nine_dragon_flame_barrier_formation_core",
            () -> new CatalogFormationCoreBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE).strength(2.5F, 8.0F).requiresCorrectToolForDrops().sound(SoundType.METAL).noOcclusion(),
                    CatalogFormationCoreBlock.FormationKind.NINE_DRAGON_FLAME_BARRIER));
    public static final RegistryObject<Block> INVERTED_FIVE_ELEMENTS_FORMATION_CORE = BLOCKS.register("inverted_five_elements_formation_core",
            () -> new CatalogFormationCoreBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(2.2F, 7.0F).requiresCorrectToolForDrops().sound(SoundType.AMETHYST).noOcclusion(),
                    CatalogFormationCoreBlock.FormationKind.INVERTED_FIVE_ELEMENTS));
    public static final RegistryObject<Block> VAJRA_PRISON_FORMATION_CORE = BLOCKS.register("vajra_prison_formation_core",
            () -> new CatalogFormationCoreBlock(BlockBehaviour.Properties.of().mapColor(MapColor.GOLD).strength(3.0F, 10.0F).requiresCorrectToolForDrops().sound(SoundType.ANVIL).noOcclusion(),
                    CatalogFormationCoreBlock.FormationKind.VAJRA_PRISON));
    public static final RegistryObject<Block> MULAN_WIND_RIDE_FORMATION_CORE = BLOCKS.register("mulan_wind_ride_formation_core",
            () -> new CatalogFormationCoreBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE).strength(1.8F, 5.0F).requiresCorrectToolForDrops().sound(SoundType.WOOL).noOcclusion(),
                    CatalogFormationCoreBlock.FormationKind.MULAN_WIND_RIDE));
    public static final RegistryObject<Block> BARRIER_SECT_PROTECTION_FORMATION_CORE = BLOCKS.register("barrier_sect_protection_formation_core",
            () -> new CatalogFormationCoreBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN).strength(3.5F, 12.0F).requiresCorrectToolForDrops().sound(SoundType.STONE).noOcclusion(),
                    CatalogFormationCoreBlock.FormationKind.BARRIER_SECT_PROTECTION));
    public static final RegistryObject<Block> SPIRIT_GATHERING_MINOR_FORMATION_CORE = BLOCKS.register("spirit_gathering_minor_formation_core",
            () -> new CatalogFormationCoreBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE).strength(1.4F, 3.5F).requiresCorrectToolForDrops().sound(SoundType.AMETHYST).noOcclusion(),
                    CatalogFormationCoreBlock.FormationKind.SPIRIT_GATHERING_MINOR));
    public static final RegistryObject<Block> DEMON_SEAL_PILLAR_FORMATION_CORE = BLOCKS.register("demon_seal_pillar_formation_core",
            () -> new CatalogFormationCoreBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(2.8F, 10.0F).requiresCorrectToolForDrops().sound(SoundType.DEEPSLATE).noOcclusion(),
                    CatalogFormationCoreBlock.FormationKind.DEMON_SEAL_PILLAR));
    public static final RegistryObject<Block> SWORD_ARRAY_BAGUA_FORMATION_CORE = BLOCKS.register("sword_array_bagua_formation_core",
            () -> new CatalogFormationCoreBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2.6F, 9.0F).requiresCorrectToolForDrops().sound(SoundType.METAL).noOcclusion(),
                    CatalogFormationCoreBlock.FormationKind.SWORD_ARRAY_BAGUA));
    public static final RegistryObject<Block> THUNDER_TRIBULATION_ARRAY_FORMATION_CORE = BLOCKS.register("thunder_tribulation_array_formation_core",
            () -> new CatalogFormationCoreBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_YELLOW).strength(2.4F, 8.0F).requiresCorrectToolForDrops().sound(SoundType.COPPER).noOcclusion(),
                    CatalogFormationCoreBlock.FormationKind.THUNDER_TRIBULATION_ARRAY));
    public static final RegistryObject<Block> LONG_RANGE_TELEPORT_ARRAY = BLOCKS.register("long_range_teleport_array",
            () -> new LongRangeTeleportArrayBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(3.5F, 12.0F).requiresCorrectToolForDrops().sound(SoundType.AMETHYST).noOcclusion()));
    public static final RegistryObject<Block> BLOOD_FORBIDDEN_GATE = BLOCKS.register("blood_forbidden_gate",
            () -> new BloodForbiddenGateBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(3.2F, 11.0F).requiresCorrectToolForDrops().sound(SoundType.NETHERRACK).noOcclusion()));
    public static final RegistryObject<Block> NETHER_FERRY_GATE = BLOCKS.register("nether_ferry_gate",
            () -> new NetherFerryGateBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(3.0F, 10.0F).requiresCorrectToolForDrops().sound(SoundType.DEEPSLATE).noOcclusion()));
    public static final RegistryObject<Block> ANCIENT_RIFT_GATE = BLOCKS.register("ancient_rift_gate",
            () -> new AncientRiftGateBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE).strength(3.4F, 12.0F).requiresCorrectToolForDrops().sound(SoundType.NETHERRACK).noOcclusion()));
    public static final RegistryObject<Block> CYCLE_GATE = BLOCKS.register("cycle_gate",
            () -> new CycleGateBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(3.8F, 14.0F).requiresCorrectToolForDrops().sound(SoundType.AMETHYST).noOcclusion()));
    public static final RegistryObject<Block> HIDDEN_RIFT_GATE = BLOCKS.register("hidden_rift_gate",
            () -> new HiddenRiftGateBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(2.4F, 8.0F).requiresCorrectToolForDrops().sound(SoundType.DEEPSLATE).noOcclusion()));
    public static final RegistryObject<Block> KING_TERRITORY_GATE = BLOCKS.register("king_territory_gate",
            () -> new KingTerritoryGateBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BROWN).strength(3.0F, 10.0F).requiresCorrectToolForDrops().sound(SoundType.STONE).noOcclusion()));
    public static final RegistryObject<Block> REFINEMENT_FORGE_G2 = BLOCKS.register("refinement_forge_g2",
            () -> new RefinementForgeG2Block(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.8F, 11.0F).requiresCorrectToolForDrops().sound(SoundType.ANVIL).noOcclusion()));
    public static final RegistryObject<Block> TALISMAN_TABLE = BLOCKS.register("talisman_table",
            () -> new TalismanTableBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.0F, 4.0F).requiresCorrectToolForDrops().sound(SoundType.WOOD).noOcclusion()));
    public static final RegistryObject<Block> PUPPET_ASSEMBLY_BENCH = BLOCKS.register("puppet_assembly_bench",
            () -> new PuppetAssemblyBenchBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BROWN).strength(2.5F, 6.0F).requiresCorrectToolForDrops().sound(SoundType.WOOD).noOcclusion()));
    public static final RegistryObject<Block> REFINEMENT_FORGE_G3 = BLOCKS.register("refinement_forge_g3",
            () -> new RefinementForgeG3Block(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(4.0F, 12.0F).requiresCorrectToolForDrops().sound(SoundType.ANVIL).noOcclusion()));
    public static final RegistryObject<Block> REFINEMENT_FORGE_G4 = BLOCKS.register("refinement_forge_g4",
            () -> new RefinementForgeHighBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(4.5F, 13.0F).requiresCorrectToolForDrops().sound(SoundType.ANVIL).noOcclusion(), 4));
    public static final RegistryObject<Block> REFINEMENT_FORGE_G5 = BLOCKS.register("refinement_forge_g5",
            () -> new RefinementForgeHighBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(5.0F, 14.0F).requiresCorrectToolForDrops().sound(SoundType.ANVIL).noOcclusion(), 5));
    public static final RegistryObject<Block> REFINEMENT_FORGE_G6 = BLOCKS.register("refinement_forge_g6",
            () -> new RefinementForgeHighBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(5.5F, 15.0F).requiresCorrectToolForDrops().sound(SoundType.ANVIL).noOcclusion(), 6));
    public static final RegistryObject<Block> SPIRIT_HERB_PLANTER = BLOCKS.register("spirit_herb_planter",
            () -> new SpiritHerbPlanterBlock(BlockBehaviour.Properties.of().mapColor(MapColor.GRASS).strength(0.8F).sound(SoundType.GRASS).noOcclusion()));
    public static final RegistryObject<Block> LOW_SPIRIT_IRON_ORE = BLOCKS.register("low_spirit_iron_ore",
            () -> new DropExperienceBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.5F, 4.0F).requiresCorrectToolForDrops().sound(SoundType.STONE),
                    UniformInt.of(1, 3)));
    public static final RegistryObject<Block> YIN_ESSENCE_ORE = BLOCKS.register("yin_essence_ore",
            () -> new DropExperienceBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(3.2F, 4.0F).requiresCorrectToolForDrops().sound(SoundType.DEEPSLATE),
                    UniformInt.of(1, 4)));
    // Wave491: sparse surface marker for leyline presentation (hash aura remains authority).
    public static final RegistryObject<Block> LEYLINE_SURFACE_MARKER = BLOCKS.register("leyline_surface_marker",
            () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN).strength(1.2F, 2.0F)
                    .sound(SoundType.AMETHYST).lightLevel(state -> 7).noOcclusion()));

    public static final RegistryObject<Block> ALCHEMY_FURNACE = registerAlchemyFurnace("alchemy_furnace", 1, 3.0F, 6.0F);
    public static final RegistryObject<Block> ALCHEMY_FURNACE_TIER_2 = registerAlchemyFurnace("alchemy_furnace_tier_2", 2, 4.0F, 8.0F);
    public static final RegistryObject<Block> ALCHEMY_FURNACE_TIER_3 = registerAlchemyFurnace("alchemy_furnace_tier_3", 3, 5.0F, 10.0F);
    public static final RegistryObject<Block> ALCHEMY_FURNACE_TIER_4 = registerAlchemyFurnace("alchemy_furnace_tier_4", 4, 6.0F, 12.0F);
    public static final RegistryObject<Block> ALCHEMY_FURNACE_TIER_5 = registerAlchemyFurnace("alchemy_furnace_tier_5", 5, 7.0F, 14.0F);

    public static final RegistryObject<Block> SECT_EARTH_FIRE_ROOM = BLOCKS.register("sect_earth_fire_room", () -> new Block(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE).strength(4.0F, 9.0F).requiresCorrectToolForDrops().sound(SoundType.DEEPSLATE)));

    /** Wave499: dedicated alchemy furnace shell node (preferred over reusing spirit_gathering_array). */
    public static final RegistryObject<Block> ALCHEMY_FURNACE_ARRAY_NODE = BLOCKS.register("alchemy_furnace_array_node", () -> new Block(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE).strength(1.5F, 3.0F).requiresCorrectToolForDrops().sound(SoundType.AMETHYST).noOcclusion()));

    /** Wave500: lids are placeable multiblock parts above the furnace controller. */
    public static final RegistryObject<Block> ALCHEMY_LID_LOW = registerAlchemyLid("alchemy_lid_low", 1);
    public static final RegistryObject<Block> ALCHEMY_LID_MID = registerAlchemyLid("alchemy_lid_mid", 2);
    public static final RegistryObject<Block> ALCHEMY_LID_HIGH = registerAlchemyLid("alchemy_lid_high", 3);
    public static final RegistryObject<Block> ALCHEMY_LID_TIER_4 = registerAlchemyLid("alchemy_lid_tier_4", 4);
    public static final RegistryObject<Block> ALCHEMY_LID_TIER_5 = registerAlchemyLid("alchemy_lid_tier_5", 5);

    public static final RegistryObject<Block> EARTH_WALL = BLOCKS.register("earth_wall", () -> new EarthWallBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.DIRT).strength(1.5F, 3.0F).sound(SoundType.GRAVEL)));

    private ModBlocks() {}

    private static RegistryObject<Block> registerAlchemyFurnace(String name, int tier, float hardness, float resistance) {
        return BLOCKS.register(name, () -> new AlchemyFurnaceBlock(
                BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(hardness, resistance).requiresCorrectToolForDrops().sound(SoundType.METAL),
                tier));
    }

    private static RegistryObject<Block> registerAlchemyLid(String name, int tier) {
        return BLOCKS.register(name, () -> new AlchemyLidBlock(
                BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(1.2F, 3.0F).requiresCorrectToolForDrops().sound(SoundType.METAL).noOcclusion(),
                tier));
    }

    public static void register(IEventBus bus) { BLOCKS.register(bus); }
}
