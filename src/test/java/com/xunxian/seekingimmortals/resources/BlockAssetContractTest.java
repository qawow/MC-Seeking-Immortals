package com.xunxian.seekingimmortals.resources;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlockAssetContractTest {
    private static final Path ASSETS = Path.of(
            "src", "main", "resources", "assets", "seeking_immortals");
    private static final Path BLOCKSTATES = ASSETS.resolve("blockstates");
    private static final Path BLOCK_MODELS = ASSETS.resolve("models/block");
    private static final Path ITEM_MODELS = ASSETS.resolve("models/item");
    private static final Path BLOCK_TEXTURES = ASSETS.resolve("textures/block");

    private static final Map<String, Integer> TEMPLATE_HEIGHTS = Map.of(
            "alchemy_furnace", 14,
            "alchemy_lid", 4,
            "formation_core", 8,
            "refinement_forge", 12,
            "spirit_gathering_array", 3,
            "array_pedestal", 10,
            "workstation", 12);

    private static final Map<String, List<String>> MODELED_BLOCKS = Map.of(
            "alchemy_furnace", List.of(
                    "alchemy_furnace", "alchemy_furnace_formed",
                    "alchemy_furnace_tier_2", "alchemy_furnace_tier_2_formed",
                    "alchemy_furnace_tier_3", "alchemy_furnace_tier_3_formed",
                    "alchemy_furnace_tier_4", "alchemy_furnace_tier_4_formed",
                    "alchemy_furnace_tier_5", "alchemy_furnace_tier_5_formed"),
            "alchemy_lid", List.of(
                    "alchemy_lid_low", "alchemy_lid_mid", "alchemy_lid_high",
                    "alchemy_lid_tier_4", "alchemy_lid_tier_5"),
            "formation_core", List.of(
                    "spirit_gathering_formation_core", "defense_formation_core",
                    "seal_demon_formation_core", "illusion_maze_formation_core",
                    "kill_sword_formation_core", "five_elements_mountain_formation_core",
                    "nine_dragon_flame_barrier_formation_core", "inverted_five_elements_formation_core",
                    "vajra_prison_formation_core", "mulan_wind_ride_formation_core",
                    "barrier_sect_protection_formation_core", "spirit_gathering_minor_formation_core",
                    "demon_seal_pillar_formation_core", "sword_array_bagua_formation_core",
                    "thunder_tribulation_array_formation_core"),
            "refinement_forge", List.of(
                    "refinement_forge", "refinement_forge_g2", "refinement_forge_g3",
                    "refinement_forge_g4", "refinement_forge_g5", "refinement_forge_g6"),
            "spirit_gathering_array", List.of("spirit_gathering_array"),
            "array_pedestal", List.of(
                    "ascension_gate", "blood_forbidden_gate", "long_range_teleport_array"),
            "workstation", List.of("talisman_table", "puppet_assembly_bench"));

    @Test
    void blockstatesModelsParentsAndTexturesResolve() throws Exception {
        try (Stream<Path> paths = Files.walk(BLOCKSTATES)) {
            for (Path blockstate : paths.filter(path -> path.toString().endsWith(".json")).toList()) {
                Set<String> models = new HashSet<>();
                collectNamedStrings(read(blockstate), "model", models);
                assertFalse(models.isEmpty(), blockstate + " must reference at least one model");
                for (String model : models) {
                    if (model.startsWith("seeking_immortals:block/")) {
                        assertTrue(Files.isRegularFile(localModelPath(model)),
                                blockstate + " references missing model " + model);
                    }
                }
            }
        }

        Set<Path> checkedTextures = new HashSet<>();
        try (Stream<Path> paths = Files.walk(BLOCK_MODELS)) {
            for (Path modelPath : paths.filter(path -> path.toString().endsWith(".json")).toList()) {
                JsonObject model = read(modelPath).getAsJsonObject();
                if (model.has("parent")) {
                    String parent = model.get("parent").getAsString();
                    if (parent.startsWith("seeking_immortals:block/")) {
                        assertTrue(Files.isRegularFile(localModelPath(parent)),
                                modelPath + " references missing parent " + parent);
                    }
                }
                Set<String> textures = new HashSet<>();
                collectLocalTextures(model, textures);
                for (String texture : textures) {
                    Path texturePath = BLOCK_TEXTURES.resolve(texture + ".png");
                    assertTrue(Files.isRegularFile(texturePath),
                            modelPath + " references missing texture " + texture);
                    if (checkedTextures.add(texturePath)) {
                        assertOpaqueSixteenPixelTexture(texturePath);
                    }
                }
            }
        }
    }

    @Test
    void modeledMultiblockControllersUseBoundedSharedGeometry() throws Exception {
        for (Map.Entry<String, Integer> entry : TEMPLATE_HEIGHTS.entrySet()) {
            Path templatePath = BLOCK_MODELS.resolve("templates/" + entry.getKey() + ".json");
            JsonObject template = read(templatePath).getAsJsonObject();
            assertEquals("minecraft:block/block", template.get("parent").getAsString(),
                    templatePath + " must inherit standard BlockItem display transforms");
            JsonArray elements = template.getAsJsonArray("elements");
            assertNotNull(elements, templatePath + " must define elements");
            assertFalse(elements.isEmpty(), templatePath + " must define visible geometry");
            double maxY = 0.0D;
            for (JsonElement element : elements) {
                JsonObject cube = element.getAsJsonObject();
                JsonArray from = cube.getAsJsonArray("from");
                JsonArray to = cube.getAsJsonArray("to");
                for (int axis = 0; axis < 3; axis++) {
                    assertTrue(from.get(axis).getAsDouble() >= 0.0D, templatePath + " extends below model bounds");
                    assertTrue(to.get(axis).getAsDouble() <= 16.0D, templatePath + " extends beyond model bounds");
                    assertTrue(from.get(axis).getAsDouble() < to.get(axis).getAsDouble(),
                            templatePath + " has a zero-sized element");
                }
                maxY = Math.max(maxY, to.get(1).getAsDouble());
            }
            assertEquals(entry.getValue().doubleValue(), maxY, 0.0D,
                    templatePath + " must match the Java VoxelShape height");

            Set<String> requiredTextureVariables = new HashSet<>();
            collectTextureVariables(template, requiredTextureVariables);
            for (String blockId : MODELED_BLOCKS.get(entry.getKey())) {
                Path childPath = BLOCK_MODELS.resolve(blockId + ".json");
                JsonObject child = read(childPath).getAsJsonObject();
                assertEquals("seeking_immortals:block/templates/" + entry.getKey(),
                        child.get("parent").getAsString(), childPath + " must use the shared elements model");
                Set<String> supplied = child.getAsJsonObject("textures").keySet();
                assertTrue(supplied.containsAll(requiredTextureVariables),
                        childPath + " is missing texture variables " + difference(requiredTextureVariables, supplied));
                assertFalse(child.get("parent").getAsString().contains("cube_all"),
                        childPath + " must not regress to a cube model");

                if (!blockId.endsWith("_formed")) {
                    Path blockstatePath = BLOCKSTATES.resolve(blockId + ".json");
                    Set<String> blockstateModels = new HashSet<>();
                    collectNamedStrings(read(blockstatePath), "model", blockstateModels);
                    assertTrue(blockstateModels.contains("seeking_immortals:block/" + blockId),
                            blockstatePath + " must expose the modeled block state");

                    Path itemModelPath = ITEM_MODELS.resolve(blockId + ".json");
                    JsonObject itemModel = read(itemModelPath).getAsJsonObject();
                    assertEquals("seeking_immortals:block/" + blockId,
                            itemModel.get("parent").getAsString(),
                            itemModelPath + " must render the same modeled block");
                }
            }
        }
    }

    @Test
    void spiritOreBlockItemsAreThreeDimensionalAndPlayerVisible() throws Exception {
        Set<String> oreIds = Set.of(
                "spirit_ore", "metal_spirit_ore", "wood_spirit_ore", "water_spirit_ore",
                "fire_spirit_ore", "earth_spirit_ore", "low_spirit_iron_ore", "yin_essence_ore");
        String creativeSource = Files.readString(Path.of(
                "src/main/java/com/xunxian/seekingimmortals/registry/ModCreativeTabs.java"));
        String itemSource = Files.readString(Path.of(
                "src/main/java/com/xunxian/seekingimmortals/registry/ModItems.java"));
        String zh = Files.readString(ASSETS.resolve("lang/zh_cn.json"));
        String en = Files.readString(ASSETS.resolve("lang/en_us.json"));

        for (String oreId : oreIds) {
            Path itemModelPath = ITEM_MODELS.resolve(oreId + ".json");
            JsonObject itemModel = read(itemModelPath).getAsJsonObject();
            assertEquals("seeking_immortals:block/" + oreId, itemModel.get("parent").getAsString(),
                    oreId + " BlockItem must render through its block model");
            String constant = oreId.toUpperCase();
            assertTrue(itemSource.contains(" " + constant + " = ITEMS.register(\"" + oreId + "\""),
                    "missing BlockItem registration for " + oreId);
            assertTrue(creativeSource.contains("ModItems." + constant + ".get()"),
                    "missing creative tab entry for " + oreId);
            assertTrue(zh.contains("block.seeking_immortals." + oreId), "missing zh_cn block name for " + oreId);
            assertTrue(en.contains("block.seeking_immortals." + oreId), "missing en_us block name for " + oreId);
        }
    }

    private static JsonElement read(Path path) throws Exception {
        return JsonParser.parseString(Files.readString(path));
    }

    private static Path localModelPath(String modelId) {
        return BLOCK_MODELS.resolve(modelId.substring("seeking_immortals:block/".length()) + ".json");
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

    private static void collectLocalTextures(JsonElement element, Set<String> output) {
        if (element.isJsonObject()) {
            for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
                collectLocalTextures(entry.getValue(), output);
            }
        } else if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) {
                collectLocalTextures(child, output);
            }
        } else if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
            String value = element.getAsString();
            if (value.startsWith("seeking_immortals:block/")) {
                String texture = value.substring("seeking_immortals:block/".length());
                if (!texture.startsWith("templates/")) {
                    output.add(texture);
                }
            }
        }
    }

    private static void collectTextureVariables(JsonElement element, Set<String> output) {
        if (element.isJsonObject()) {
            for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
                collectTextureVariables(entry.getValue(), output);
            }
        } else if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) {
                collectTextureVariables(child, output);
            }
        } else if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
            String value = element.getAsString();
            if (value.startsWith("#")) {
                output.add(value.substring(1));
            }
        }
    }

    private static void assertOpaqueSixteenPixelTexture(Path path) throws Exception {
        BufferedImage image = ImageIO.read(path.toFile());
        assertNotNull(image, path + " must be a readable PNG");
        assertEquals(16, image.getWidth(), path + " width");
        assertEquals(16, image.getHeight(), path + " height");
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                assertEquals(255, image.getRGB(x, y) >>> 24, path + " must be fully opaque");
            }
        }
    }

    private static Set<String> difference(Set<String> left, Set<String> right) {
        Set<String> result = new HashSet<>(left);
        result.removeAll(right);
        return result;
    }
}
