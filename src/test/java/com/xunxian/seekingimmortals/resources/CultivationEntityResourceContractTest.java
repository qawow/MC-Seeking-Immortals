package com.xunxian.seekingimmortals.resources;

import com.xunxian.seekingimmortals.beast.BeastBestiaryService;
import com.xunxian.seekingimmortals.beast.BeastElementService;
import com.xunxian.seekingimmortals.npc.NamedNpcRegistry;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.imageio.ImageIO;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CultivationEntityResourceContractTest {
    private static final Path ASSETS = Path.of(
            "src", "main", "resources", "assets", "seeking_immortals");
    private static final Path DATA = Path.of(
            "src", "main", "resources", "data", "seeking_immortals");
    private static final Path JAVA = Path.of(
            "src", "main", "java", "com", "xunxian", "seekingimmortals");

    @Test
    void naturalSpawnModifierUsesTheDedicatedBeastType() throws Exception {
        JsonObject modifier = read(DATA.resolve(
                "forge/biome_modifier/add_cultivation_beast_spawns.json"));
        assertEquals("forge:add_spawns", modifier.get("type").getAsString());
        assertEquals("#seeking_immortals:allows_cultivation_beast_spawns",
                modifier.get("biomes").getAsString());

        JsonObject spawner = modifier.getAsJsonObject("spawners");
        assertEquals("seeking_immortals:cultivation_beast", spawner.get("type").getAsString());
        assertTrue(spawner.get("weight").getAsInt() > 0);
        assertTrue(spawner.get("minCount").getAsInt() >= 1);
        assertTrue(spawner.get("maxCount").getAsInt() <= 2);

        JsonObject biomes = read(DATA.resolve(
                "tags/worldgen/biome/allows_cultivation_beast_spawns.json"));
        String values = biomes.getAsJsonArray("values").toString();
        assertTrue(values.contains("#minecraft:is_overworld"));
        for (String id : List.of("secret_mist_cave", "secret_fallen_demon",
                "secret_void_palace", "secret_blood_forbidden")) {
            assertTrue(values.contains("seeking_immortals:" + id), id);
        }
    }

    @Test
    void beastAndNpcRegistrationsUseDedicatedEntityClasses() throws Exception {
        String registry = Files.readString(JAVA.resolve("registry/ModEntities.java"), StandardCharsets.UTF_8);
        assertTrue(registry.contains("EntityType<CultivationBeastEntity>"));
        assertTrue(registry.contains("EntityType<QuestNpcEntity>"));
        assertTrue(registry.contains("CultivationBeastEntity::checkSpawnRules"));

        String npcBase = Files.readString(JAVA.resolve("entity/CultivatorNpcEntity.java"), StandardCharsets.UTF_8);
        assertTrue(npcBase.contains("extends PathfinderMob"));
        assertFalse(npcBase.contains("extends Villager"));

        for (String file : List.of("SectStewardEntity.java", "MarketTraderEntity.java",
                "SpiritStoneBankerEntity.java", "QuestNpcEntity.java")) {
            String source = Files.readString(JAVA.resolve("entity").resolve(file), StandardCharsets.UTF_8);
            assertTrue(source.contains("extends CultivatorNpcEntity"), file);
            assertFalse(source.contains("extends Villager"), file);
        }
    }

    @Test
    void geckoModelsAnimationsAndElementTexturesExist() throws Exception {
        JsonObject npcGeometry = firstGeometry(read(ASSETS.resolve("geo/cultivator_npc.geo.json")));
        assertEquals("geometry.seeking_immortals.cultivator_npc",
                npcGeometry.getAsJsonObject("description").get("identifier").getAsString());
        JsonObject npcAnimations = read(ASSETS.resolve(
                "animations/cultivator_npc.animation.json")).getAsJsonObject("animations");
        assertTrue(npcAnimations.has("idle"));
        assertTrue(npcAnimations.has("walk"));
        for (String role : List.of("steward", "trader", "banker", "quest")) {
            assertTrue(Files.exists(ASSETS.resolve(
                    "textures/entity/cultivator_npc_" + role + ".png")), role);
        }

        JsonObject beastAnimations = read(ASSETS.resolve(
                "animations/cultivation_beast.animation.json")).getAsJsonObject("animations");
        for (String animation : List.of("idle", "walk", "attack")) {
            assertTrue(beastAnimations.has(animation), animation);
        }
        List<String> bodyPlans = List.of("quadruped", "serpent", "insect",
                "avian", "aquatic", "humanoid");
        Set<String> authoredElements = BeastBestiaryService.all().values().stream()
                .map(entry -> BeastElementService.normalize(entry.element(), entry.id()))
                .collect(Collectors.toSet());
        assertEquals(BeastElementService.SUPPORTED_ELEMENTS, authoredElements,
                "every normalized bestiary element must have a dedicated rendered affinity");
        List<String> elements = authoredElements.stream()
                .filter(element -> !"neutral".equals(element))
                .sorted(Comparator.naturalOrder())
                .toList();
        for (String bodyPlan : bodyPlans) {
            JsonObject geometry = firstGeometry(read(ASSETS.resolve(
                    "geo/cultivation_beast_" + bodyPlan + ".geo.json")));
            assertEquals("geometry.seeking_immortals.cultivation_beast." + bodyPlan,
                    geometry.getAsJsonObject("description").get("identifier").getAsString());
            assertTrue(geometry.getAsJsonArray("bones").size() >= 10, bodyPlan);
            assertTexture(ASSETS.resolve("textures/entity/cultivation_beast_" + bodyPlan + ".png"), 64, 64);
            for (String element : elements) {
                assertTexture(ASSETS.resolve("textures/entity/cultivation_beast/"
                        + bodyPlan + "_" + element + ".png"), 64, 64);
            }
        }

        String renderer = Files.readString(JAVA.resolve("client/CultivationBeastRenderer.java"),
                StandardCharsets.UTF_8);
        assertTrue(renderer.contains("CultivationBeastEntity.SUPPORTED_ELEMENTS.contains(value)"));
    }

    @Test
    void entityLootIsOwnedByTheDataDrivenKillService() throws Exception {
        JsonObject loot = read(DATA.resolve("loot_tables/entities/cultivation_beast.json"));
        assertEquals("minecraft:entity", loot.get("type").getAsString());
        assertTrue(loot.getAsJsonArray("pools").isEmpty());

        String events = Files.readString(JAVA.resolve("event/ModEvents.java"), StandardCharsets.UTF_8);
        assertTrue(events.contains("BeastLootService.handleEcologyKill"));
        String beast = Files.readString(JAVA.resolve("entity/CultivationBeastEntity.java"), StandardCharsets.UTF_8);
        assertTrue(beast.contains("configureWild"));
        assertTrue(beast.contains("applyElementalEffect"));
        assertTrue(beast.contains("BeastBossService.tickBossSkills"));
        assertTrue(beast.contains("thenPlay(\"attack\")"));
    }

    @Test
    void everyCatalogBeastAndNamedNpcHasAnEntityLayer() throws Exception {
        assertTrue(BeastBestiaryService.size() >= 1800,
                "the runtime bestiary must remain data-driven rather than a short hand list");
        for (BeastBestiaryService.BeastEntry entry : BeastBestiaryService.all().values()) {
            assertFalse(entry.id().isBlank());
            assertTrue(entry.tier() >= 1 && entry.tier() <= 13, entry.id());
        }
        String beastSource = Files.readString(
                JAVA.resolve("entity/CultivationBeastEntity.java"), StandardCharsets.UTF_8);
        for (String plan : List.of("QUADRUPED", "SERPENT", "INSECT", "AVIAN", "AQUATIC", "HUMANOID")) {
            assertTrue(beastSource.contains("return BodyPlan." + plan), plan);
        }

        assertTrue(NamedNpcRegistry.all().size() >= 179,
                "all authored named NPCs must remain available to the dedicated entity spawner");
        for (NamedNpcRegistry.NamedNpc npc : NamedNpcRegistry.all()) {
            assertFalse(npc.id().isBlank());
            assertFalse(npc.regionId().isBlank(), npc.id());
            assertFalse(npc.role().isBlank(), npc.id());
        }
    }

    private static JsonObject read(Path path) throws Exception {
        try (var reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    private static JsonObject firstGeometry(JsonObject root) {
        assertTrue(root.has("minecraft:geometry"));
        assertFalse(root.getAsJsonArray("minecraft:geometry").isEmpty());
        return root.getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
    }

    private static void assertTexture(Path path, int width, int height) throws Exception {
        assertTrue(Files.exists(path), path.toString());
        var image = ImageIO.read(path.toFile());
        assertTrue(image != null, path.toString());
        assertEquals(width, image.getWidth(), path.toString());
        assertEquals(height, image.getHeight(), path.toString());
    }
}
