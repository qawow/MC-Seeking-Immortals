package com.xunxian.seekingimmortals.structure;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StructureResourceContractTest {
    private static final Path DATA = Path.of(
            "src", "main", "resources", "data", "seeking_immortals");

    @Test
    void everyAuthoredStationValidatorIsImplementedOrExplicitlyFailClosed() throws Exception {
        JsonObject root = read(DATA.resolve("text_material/multiblock_station_patterns.json"));
        Set<String> authored = new HashSet<>();
        for (JsonElement element : root.getAsJsonArray("stations")) {
            JsonObject pattern = element.getAsJsonObject().getAsJsonObject("pattern");
            authored.add(pattern.get("validator").getAsString());
        }

        Set<String> implemented = MultiblockStationService.implementedValidators();
        Set<String> failClosed = MultiblockStationService.failClosedValidators();
        Set<String> overlap = new HashSet<>(implemented);
        overlap.retainAll(failClosed);
        assertTrue(overlap.isEmpty(), "validator cannot be both implemented and fail-closed: " + overlap);

        Set<String> classified = new HashSet<>(implemented);
        classified.addAll(failClosed);
        assertEquals(classified, MultiblockStationService.supportedValidators());
        assertTrue(classified.containsAll(authored),
                "unclassified validators: " + difference(authored, classified));

        assertTrue(implemented.contains("spirit_gathering_formation"));
        assertTrue(implemented.contains("refinement_forge_g3"));
        assertTrue(implemented.contains("long_range_teleport_array"));
        assertTrue(implemented.contains("thunder_tribulation_altar"));
    }

    @Test
    void stationDispatchUsesNewGeometryAndRejectsGenericSingleCore() throws Exception {
        String stationSource = Files.readString(Path.of(
                "src/main/java/com/xunxian/seekingimmortals/structure/MultiblockStationService.java"));
        for (String type : Set.of(
                "AdvancedSpiritGatheringArrayStructure",
                "RefinementFurnaceStructure",
                "TeleportationArrayStructure",
                "TribulationPlatformStructure")) {
            assertTrue(stationSource.contains(type + ".validate("), type + " must be wired into station dispatch");
        }
        assertTrue(stationSource.contains("unrecognized_validator:"));
        assertTrue(stationSource.contains("ForgeRegistries.BLOCKS.containsKey(blockId)"));
        assertTrue(stationSource.contains("level.getBlockState(origin).is(expected)"));

        String patternSource = Files.readString(Path.of(
                "src/main/java/com/xunxian/seekingimmortals/structure/MultiblockPattern.java"));
        assertFalse(patternSource.contains("state -> !state.isAir()"),
                "single_core must not accept an arbitrary solid block");
        assertTrue(patternSource.contains("singleCoreRequirements(Supplier<? extends Block> coreBlock)"));
        assertTrue(patternSource.contains("case \"single_core\" -> List.of()"));
    }

    @Test
    void singleCoreAliasesResolveAndUnknownIdsFailClosed() {
        assertEquals("seeking_immortals:low_spirit_iron_ore",
                MultiblockStationService.singleCoreBlockId("low_spirit_iron_ore").orElseThrow().toString());
        assertEquals("seeking_immortals:yin_essence_ore",
                MultiblockStationService.singleCoreBlockId("yin_essence_ore_block").orElseThrow().toString());
        assertTrue(MultiblockStationService.singleCoreBlockId("unknown_single_core").isEmpty());
    }

    /**
     * A {@code single_core} station whose id has no entry in {@code SINGLE_CORE_BLOCK_IDS} returns
     * {@code missing_core_mapping} forever, so {@code isStationFormed} can never be true and no
     * {@code STRUCTURE_FORMED} proof can ever land. Two of these sit on the opening of the main
     * story: {@code mortal_qixuan_entry} step 5 and {@code huangfeng_blood_quota} step 1.
     */
    @Test
    void everySingleCoreStationResolvesToARegisteredBlock() throws Exception {
        JsonObject root = read(DATA.resolve("text_material/multiblock_station_patterns.json"));
        Set<String> singleCore = new HashSet<>();
        for (JsonElement element : root.getAsJsonArray("stations")) {
            JsonObject station = element.getAsJsonObject();
            if ("single_core".equals(station.getAsJsonObject("pattern").get("validator").getAsString())) {
                singleCore.add(station.get("id").getAsString());
            }
        }
        // Pins the input so a future data edit cannot shrink this contract into vacuous success.
        assertEquals(13, singleCore.size(), "authored single_core stations: " + singleCore);

        // structure_blueprint_table stays a held tool (BaseMaterialItem -> StructureToolService),
        // not a placeable block: giving it a BlockItem would silently kill the projection guide.
        Set<String> exceptions = Set.of("structure_blueprint_table");
        assertEquals(1, exceptions.size(), "every excepted station must be justified in this test");

        String blockSource = stripComments(Files.readString(Path.of(
                "src/main/java/com/xunxian/seekingimmortals/registry/ModBlocks.java")));
        Set<String> registered = new HashSet<>();
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("BLOCKS\\.register\\(\"([a-z0-9_]+)\"").matcher(blockSource);
        while (matcher.find()) {
            registered.add(matcher.group(1));
        }
        assertTrue(registered.contains("low_spirit_iron_ore"), "block id harvest must work");

        for (String stationId : singleCore) {
            if (exceptions.contains(stationId)) {
                continue;
            }
            java.util.Optional<net.minecraft.resources.ResourceLocation> mapped =
                    MultiblockStationService.singleCoreBlockId(stationId);
            assertTrue(mapped.isPresent(),
                    "single_core station has no core block mapping, so it can never form: " + stationId);
            assertEquals("seeking_immortals", mapped.get().getNamespace(), stationId);
            assertTrue(registered.contains(mapped.get().getPath()),
                    "mapped core block is not registered in ModBlocks: "
                            + stationId + " -> " + mapped.get());
        }

        // Reverse direction: a mapping for a station that does not exist is a typo.
        for (String mappedStation : MultiblockStationService.singleCoreStationIds()) {
            assertTrue(singleCore.contains(mappedStation),
                    "mapped id is not an authored single_core station: " + mappedStation);
        }

        // The sentinel must stay: deleting it would hide the symptom instead of fixing the cause.
        String stationSource = stripComments(Files.readString(Path.of(
                "src/main/java/com/xunxian/seekingimmortals/structure/MultiblockStationService.java")));
        assertTrue(stationSource.contains("missing_core_mapping:"),
                "the fail-closed sentinel for an unmapped core must remain");
    }

    @Test
    void naturalStructuresHaveSetsBiomeTagsAndDedicatedLoot() throws Exception {
        assertNaturalStructure(
                "ancient_cultivator_cave",
                "ancient_cultivator_caves",
                "seeking_immortals:ancient_cultivator_cave",
                "underground_structures");
        assertNaturalStructure(
                "spirit_beast_den",
                "spirit_beast_dens",
                "seeking_immortals:spirit_beast_den",
                "surface_structures");

        for (String loot : Set.of("ancient_cultivator_cave", "spirit_beast_den")) {
            JsonObject table = read(DATA.resolve("loot_tables/chests/" + loot + ".json"));
            assertEquals("minecraft:chest", table.get("type").getAsString());
            assertTrue(table.getAsJsonArray("pools").size() > 0);
            assertTrue(hasModLoot(table.getAsJsonArray("pools")), loot + " must contain mod loot");
        }

        String cavePiece = Files.readString(Path.of(
                "src/main/java/com/xunxian/seekingimmortals/worldgen/AncientCultivatorCavePieces.java"));
        String denPiece = Files.readString(Path.of(
                "src/main/java/com/xunxian/seekingimmortals/worldgen/SpiritBeastDenPieces.java"));
        assertTrue(cavePiece.contains("chests/ancient_cultivator_cave"));
        assertTrue(denPiece.contains("chests/spirit_beast_den"));
        assertTrue(cavePiece.contains("createChest("));
        assertTrue(denPiece.contains("createChest("));
        assertFalse(cavePiece.contains("Blocks.BLAST_FURNACE"));
        assertFalse(denPiece.contains("Blocks.DRAGON_EGG"));
        assertFalse(denPiece.contains("Blocks.TALL_GRASS"));
        assertTrue(cavePiece.contains("phaseRandom(CAVE_SHELL_SALT)"));
        assertTrue(cavePiece.contains("phaseRandom(CAVE_LOOT_SALT)"));
        assertTrue(cavePiece.contains("phaseRandom(CAVE_ORE_SALT)"));
        assertTrue(denPiece.contains("phaseRandom(DEN_FLOOR_SALT)"));
        assertTrue(denPiece.contains("phaseRandom(DEN_LOOT_SALT)"));
        assertTrue(denPiece.contains("phaseRandom(DEN_HERB_SALT)"));
        assertTrue(denPiece.contains("phaseRandom(DEN_DECOR_SALT)"));
        assertFalse(cavePiece.contains("RandomSource structureRandom"));
        assertFalse(denPiece.contains("RandomSource structureRandom"));
        assertTrue(cavePiece.contains("RandomSource.create(lootSeed)"));
        assertTrue(denPiece.contains("RandomSource.create(centerLootSeed)"));
        assertTrue(denPiece.contains("RandomSource.create(lootSeed)"));
        assertTrue(denPiece.contains("level.getBlockState(herbPos).isAir()"));
        assertTrue(denPiece.indexOf("BlockState plantState = switch (random.nextInt(4))")
                        < denPiece.indexOf("if (box.isInside(herbPos)"),
                "plant type RNG must be consumed before per-chunk/world-state checks");
        assertTrue(denPiece.indexOf("BlockState decorState = random.nextFloat()")
                        < denPiece.indexOf("if (box.isInside(decorPos)"),
                "decoration RNG must be consumed before per-chunk/world-state checks");
        assertFalse(cavePiece.contains("TODO"));
        assertFalse(denPiece.contains("TODO"));
    }

    @Test
    void authoredStationDimensionsMatchSpecializedValidators() throws Exception {
        JsonArray stations = read(DATA.resolve("text_material/multiblock_station_patterns.json"))
                .getAsJsonArray("stations");
        JsonArray indexEntries = read(DATA.resolve("text_material/multiblock_structure_index.json"))
                .getAsJsonArray("entries");
        JsonObject killHub = station(stations, "kill_array_hub");
        JsonObject illusionHub = station(stations, "illusion_array_hub");
        JsonObject tribulation = station(stations, "tribulation_platform");
        JsonObject indexedAsuraRing = station(indexEntries, "asura_trial_ring");
        JsonObject indexedTribulation = station(indexEntries, "tribulation_platform");

        assertEquals(1, killHub.get("radius").getAsInt());
        assertEquals(1, killHub.getAsJsonObject("pattern").get("radius").getAsInt());
        assertEquals(1, illusionHub.get("radius").getAsInt());
        assertEquals(1, illusionHub.getAsJsonObject("pattern").get("radius").getAsInt());
        assertEquals("9×9×6", tribulation.get("dimensions").getAsString());
        assertEquals(9, tribulation.getAsJsonObject("size").get("w").getAsInt());
        assertEquals(6, tribulation.getAsJsonObject("size").get("h").getAsInt());
        assertEquals(9, tribulation.getAsJsonObject("size").get("d").getAsInt());
        assertEquals(TribulationPlatformStructure.PLATFORM_RADIUS,
                tribulation.get("radius").getAsInt());
        assertEquals("7×7×1", indexedAsuraRing.get("dimensions").getAsString());
        assertEquals("9×9×6", indexedTribulation.get("dimensions").getAsString());

        MultiblockStructureCatalog.StructureEntry runtime = MultiblockStructureCatalog.builtin()
                .find("tribulation_platform").orElseThrow();
        assertEquals("9×9×6", runtime.dimensions());
        assertEquals(9, runtime.sizeW());
        assertEquals(6, runtime.sizeH());
        assertEquals(9, runtime.sizeD());
        assertEquals(TribulationPlatformStructure.PLATFORM_RADIUS, runtime.radius());
    }

    @Test
    void persistentFieldsRememberTheExactCoreBlock() {
        FormationFieldSavedData.StoredField field = new FormationFieldSavedData.StoredField(
                "minecraft:overworld",
                net.minecraft.core.BlockPos.ZERO,
                "DEFENSE",
                100,
                "defense",
                2,
                0,
                "",
                false,
                "seeking_immortals:defense_formation_core");
        assertEquals("seeking_immortals:defense_formation_core", field.coreBlockId());
    }

    @Test
    void catalogFormationIdsResolveExactlyAndActivationResultIsHonored() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/xunxian/seekingimmortals/block/CatalogFormationCoreBlock.java"));
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile(
                "[A-Z_]+\\(\\s*\\\"([a-z0-9_]+)\\\"\\s*,\\s*new MobEffectInstance")
                .matcher(source);
        Set<String> enumIds = new HashSet<>();
        while (matcher.find()) {
            enumIds.add(matcher.group(1));
        }
        assertEquals(10, enumIds.size());

        JsonObject paramsRoot = read(DATA.resolve("text_material/formation_field_params.json"));
        Set<String> parameterIds = new HashSet<>();
        for (JsonElement element : paramsRoot.getAsJsonArray("fields")) {
            JsonObject params = element.getAsJsonObject();
            parameterIds.add(params.get("id").getAsString());
            if (enumIds.contains(params.get("id").getAsString())) {
                boolean spiritRing = params.get("uses_spirit_gathering_ring").getAsBoolean();
                assertEquals("SPIRIT_GATHER".equals(params.get("kind").getAsString()), spiritRing,
                        "ring role must agree with activation kind for " + params.get("id").getAsString());
            }
        }
        assertTrue(parameterIds.containsAll(enumIds),
                "missing formation field params: " + difference(enumIds, parameterIds));
        assertTrue(source.contains("!kind.formationId().equals(params.id())"));
        assertTrue(source.contains("if (!FormationFieldService.activate("));
    }

    private static void assertNaturalStructure(String id, String setId, String type, String step) throws Exception {
        JsonObject structure = read(DATA.resolve("worldgen/structure/" + id + ".json"));
        assertEquals(type, structure.get("type").getAsString());
        assertEquals(step, structure.get("step").getAsString());
        assertEquals("#seeking_immortals:has_structure/" + id,
                structure.get("biomes").getAsString());

        JsonObject set = read(DATA.resolve("worldgen/structure_set/" + setId + ".json"));
        JsonObject placement = set.getAsJsonObject("placement");
        assertEquals("minecraft:random_spread", placement.get("type").getAsString());
        assertTrue(placement.get("spacing").getAsInt() > placement.get("separation").getAsInt());
        JsonArray structures = set.getAsJsonArray("structures");
        assertTrue(structures.size() > 0);
        assertEquals(type, structures.get(0).getAsJsonObject().get("structure").getAsString());

        JsonObject tag = read(DATA.resolve("tags/worldgen/biome/has_structure/" + id + ".json"));
        assertTrue(tag.getAsJsonArray("values").size() > 0);
    }

    private static boolean hasModLoot(JsonArray pools) {
        for (JsonElement poolElement : pools) {
            for (JsonElement entryElement : poolElement.getAsJsonObject().getAsJsonArray("entries")) {
                JsonObject entry = entryElement.getAsJsonObject();
                if (entry.has("name") && entry.get("name").getAsString().startsWith("seeking_immortals:")) {
                    return true;
                }
            }
        }
        return false;
    }

    private static JsonObject station(JsonArray stations, String id) {
        for (JsonElement element : stations) {
            JsonObject station = element.getAsJsonObject();
            if (id.equals(station.get("id").getAsString())) {
                return station;
            }
        }
        throw new AssertionError("missing station: " + id);
    }

    private static Set<String> difference(Set<String> left, Set<String> right) {
        Set<String> result = new HashSet<>(left);
        result.removeAll(right);
        return result;
    }

    private static JsonObject read(Path path) throws Exception {
        return JsonParser.parseString(Files.readString(path)).getAsJsonObject();
    }

    /** Strips block and line comments so a doc reference cannot satisfy a code assertion. */
    private static String stripComments(String source) {
        return source.replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("//[^\n]*", "");
    }
}
