package com.xunxian.seekingimmortals.resources;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorldgenFeatureResourceContractTest {
    private static final Path DATA = Path.of("src", "main", "resources", "data");
    private static final Path MOD_DATA = DATA.resolve("seeking_immortals");

    private static final Set<String> ELEMENTAL_ORES = Set.of(
            "spirit_ore", "metal_spirit_ore", "wood_spirit_ore", "water_spirit_ore",
            "fire_spirit_ore", "earth_spirit_ore");

    @Test
    void elementalOreChainIsCompleteAndKeepsItsGenerationBudget() throws Exception {
        int totalCount = 0;
        for (String oreId : ELEMENTAL_ORES) {
            JsonObject configured = read(MOD_DATA.resolve("worldgen/configured_feature/" + oreId + ".json"));
            assertEquals("minecraft:ore", configured.get("type").getAsString(), oreId);
            JsonArray targets = configured.getAsJsonObject("config").getAsJsonArray("targets");
            Set<String> targetTags = targetTags(targets);
            assertTrue(targetTags.contains("minecraft:stone_ore_replaceables"), oreId + " must target stone");
            assertTrue(targetTags.contains("minecraft:deepslate_ore_replaceables"), oreId + " must target deepslate");
            for (JsonElement targetElement : targets) {
                assertEquals("seeking_immortals:" + oreId,
                        targetElement.getAsJsonObject().getAsJsonObject("state").get("Name").getAsString());
            }

            JsonObject placed = read(MOD_DATA.resolve("worldgen/placed_feature/" + oreId + ".json"));
            assertEquals("seeking_immortals:" + oreId, placed.get("feature").getAsString());
            int count = placementCount(placed);
            totalCount += count;
            assertEquals("spirit_ore".equals(oreId) ? 3 : 1, count,
                    oreId + " has an unexpected per-chunk generation budget");

            JsonObject modifier = read(MOD_DATA.resolve("forge/biome_modifier/add_" + oreId + ".json"));
            assertEquals("forge:add_features", modifier.get("type").getAsString());
            assertEquals("#minecraft:is_overworld", modifier.get("biomes").getAsString());
            assertEquals("seeking_immortals:" + oreId, modifier.get("features").getAsString());
            assertEquals("underground_ores", modifier.get("step").getAsString());
        }
        assertEquals(8, totalCount, "old compatibility ore plus five elemental ores must keep the original budget");
    }

    @Test
    void legacyOresHaveDeepstoneTargetsToolTagsAndFortuneLoot() throws Exception {
        for (String oreId : Set.of("spirit_ore", "low_spirit_iron_ore", "yin_essence_ore")) {
            JsonObject configured = read(MOD_DATA.resolve("worldgen/configured_feature/" + oreId + ".json"));
            Set<String> targetTags = targetTags(configured.getAsJsonObject("config").getAsJsonArray("targets"));
            assertTrue(targetTags.contains("minecraft:stone_ore_replaceables"), oreId);
            assertTrue(targetTags.contains("minecraft:deepslate_ore_replaceables"), oreId);
            assertTrue(tagValues(DATA.resolve("minecraft/tags/blocks/mineable/pickaxe.json")).contains("seeking_immortals:" + oreId));
            assertTrue(tagValues(DATA.resolve("minecraft/tags/blocks/needs_iron_tool.json")).contains("seeking_immortals:" + oreId));
            String loot = Files.readString(MOD_DATA.resolve("loot_tables/blocks/" + oreId + ".json"));
            assertTrue(loot.contains("minecraft:silk_touch"), oreId + " must support Silk Touch");
            assertTrue(loot.contains("minecraft:fortune"), oreId + " must support Fortune");
        }
        String spiritLoot = Files.readString(MOD_DATA.resolve("loot_tables/blocks/spirit_ore.json"));
        for (String stone : Set.of("metal_spirit_stone", "wood_spirit_stone", "water_spirit_stone",
                "fire_element_spirit_stone", "earth_spirit_stone")) {
            assertTrue(spiritLoot.contains("seeking_immortals:" + stone),
                    "compatibility spirit ore must be able to yield " + stone);
        }
    }

    @Test
    void elementalOreLootAndTagsMatchTheirSpiritStone() throws Exception {
        Map<String, String> drops = Map.of(
                "metal_spirit_ore", "metal_spirit_stone",
                "wood_spirit_ore", "wood_spirit_stone",
                "water_spirit_ore", "water_spirit_stone",
                "fire_spirit_ore", "fire_element_spirit_stone",
                "earth_spirit_ore", "earth_spirit_stone");
        Set<String> pickaxe = tagValues(DATA.resolve("minecraft/tags/blocks/mineable/pickaxe.json"));
        Set<String> ironTool = tagValues(DATA.resolve("minecraft/tags/blocks/needs_iron_tool.json"));
        Set<String> forgeBlocks = tagValues(DATA.resolve("forge/tags/blocks/ores.json"));
        Set<String> forgeItems = tagValues(DATA.resolve("forge/tags/items/ores.json"));

        for (Map.Entry<String, String> entry : drops.entrySet()) {
            String oreId = entry.getKey();
            String oreName = "seeking_immortals:" + oreId;
            assertTrue(pickaxe.contains(oreName), oreId + " must be mineable with a pickaxe");
            assertTrue(ironTool.contains(oreName), oreId + " must require an iron-tier tool");
            assertTrue(forgeBlocks.contains(oreName), oreId + " must be in the Forge block ore tag");
            assertTrue(forgeItems.contains(oreName), oreId + " must be in the Forge item ore tag");

            JsonObject loot = read(MOD_DATA.resolve("loot_tables/blocks/" + oreId + ".json"));
            Set<String> lootNames = new HashSet<>();
            collectNamedStrings(loot, "name", lootNames);
            assertEquals(Set.of(oreName, "seeking_immortals:" + entry.getValue()), lootNames,
                    oreId + " must drop only itself with Silk Touch or its matching spirit stone");
            String encodedLoot = loot.toString();
            assertTrue(encodedLoot.contains("minecraft:silk_touch"), oreId + " must support Silk Touch");
            assertTrue(encodedLoot.contains("minecraft:fortune"), oreId + " must support Fortune");
        }
    }

    @Test
    void yinEssenceUsesEnvironmentSpecificHostRocks() throws Exception {
        JsonObject overworld = read(MOD_DATA.resolve("worldgen/configured_feature/yin_essence_ore.json"));
        Set<String> overworldTags = targetTags(overworld.getAsJsonObject("config").getAsJsonArray("targets"));
        assertTrue(overworldTags.contains("minecraft:stone_ore_replaceables"));
        assertTrue(overworldTags.contains("minecraft:deepslate_ore_replaceables"));
        assertTrue(!overworldTags.contains("minecraft:base_stone_nether"));

        JsonObject nether = read(MOD_DATA.resolve("worldgen/configured_feature/yin_essence_ore_nether.json"));
        Set<String> netherTags = targetTags(nether.getAsJsonObject("config").getAsJsonArray("targets"));
        assertEquals(Set.of("minecraft:base_stone_nether"), netherTags);
        assertEquals("seeking_immortals:yin_essence_ore_nether",
                read(MOD_DATA.resolve("worldgen/placed_feature/yin_essence_ore_nether.json")).get("feature").getAsString());
        JsonObject netherModifier = read(MOD_DATA.resolve("forge/biome_modifier/add_yin_essence_ore_nether.json"));
        assertEquals("#minecraft:is_nether", netherModifier.get("biomes").getAsString());
        assertEquals("seeking_immortals:yin_essence_ore_nether", netherModifier.get("features").getAsString());
    }

    @Test
    void oreRegistrationsAndForgeOreTagStayInSync() throws Exception {
        String blockSource = Files.readString(Path.of(
                "src/main/java/com/xunxian/seekingimmortals/registry/ModBlocks.java"));
        String itemSource = Files.readString(Path.of(
                "src/main/java/com/xunxian/seekingimmortals/registry/ModItems.java"));
        Set<String> forgeOres = tagValues(DATA.resolve("forge/tags/blocks/ores.json"));
        for (String oreId : Set.of("spirit_ore", "metal_spirit_ore", "wood_spirit_ore", "water_spirit_ore",
                "fire_spirit_ore", "earth_spirit_ore", "low_spirit_iron_ore", "yin_essence_ore")) {
            assertTrue(blockSource.contains("\"" + oreId + "\""), "missing block registration " + oreId);
            assertTrue(itemSource.contains("\"" + oreId + "\""), "missing item registration " + oreId);
            assertTrue(forgeOres.contains("seeking_immortals:" + oreId), "missing Forge ore tag " + oreId);
        }
    }

    private static Set<String> targetTags(JsonArray targets) {
        Set<String> result = new HashSet<>();
        for (JsonElement element : targets) {
            result.add(element.getAsJsonObject().getAsJsonObject("target").get("tag").getAsString());
        }
        return result;
    }

    private static int placementCount(JsonObject placed) {
        for (JsonElement element : placed.getAsJsonArray("placement")) {
            JsonObject placement = element.getAsJsonObject();
            if ("minecraft:count".equals(placement.get("type").getAsString())) {
                return placement.get("count").getAsInt();
            }
        }
        throw new AssertionError("placed feature has no minecraft:count placement");
    }

    private static Set<String> tagValues(Path path) throws Exception {
        Set<String> result = new HashSet<>();
        for (JsonElement element : read(path).getAsJsonArray("values")) {
            result.add(element.getAsString());
        }
        return result;
    }

    private static void collectNamedStrings(JsonElement element, String key, Set<String> output) {
        if (element.isJsonObject()) {
            for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
                if (key.equals(entry.getKey()) && entry.getValue().isJsonPrimitive()) {
                    output.add(entry.getValue().getAsString());
                } else {
                    collectNamedStrings(entry.getValue(), key, output);
                }
            }
        } else if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) {
                collectNamedStrings(child, key, output);
            }
        }
    }

    private static JsonObject read(Path path) throws Exception {
        return JsonParser.parseString(Files.readString(path)).getAsJsonObject();
    }
}
