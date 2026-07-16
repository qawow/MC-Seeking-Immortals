package com.xunxian.seekingimmortals.artifact;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.xunxian.seekingimmortals.item.FlyingArtifactItem;
import org.junit.jupiter.api.Test;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArtifactDataServiceTest {
    private static final List<String> PRIORITY_CARRIER_IDS = List.of(
            "flying_sword_low",
            "cloud_boots",
            "spirit_gathering_bead",
            "yellow_umbrella",
            "qingye_leaf_fan",
            "storage_bracelet_low",
            "snake_pearl",
            "flying_needle_set",
            "black_gold_shield",
            "bedrock_shield",
            "artifact_repair_kit",
            "silver_giant_sword",
            "gold_demon_chain",
            "evil_illusion_mirror",
            "qingning_mirror",
            "gold_light_brick",
            "beast_taming_whip",
            "spirit_beast_bridle",
            "wind_escape_sail",
            "moon_shadow_disk",
            "talisman_treasure_soul_charm",
            "void_palace_cold_jade_pendant",
            "xuanguang_mirror",
            "xuanhuang_mirror",
            "nine_dragon_cauldron_replica",
            "void_refining_bell",
            "talisman_treasure_demon_seal",
            "natal_sword_embryo",
            "four_symbols_ruler_replica",
            "seven_flame_fan_replica",
            "three_flame_fan_replica");

    @Test
    void loadsShippedArtifactCatalogs() {
        ArtifactDataService.Snapshot snapshot = ArtifactDataService.builtin();

        assertEquals(18, ArtifactDataService.sourceFiles().size());
        assertEquals(217, snapshot.artifacts().size());
        assertEquals(73, snapshot.refinementRecipes().size());
        assertEquals(8, snapshot.flightVehicles().size());
        assertEquals(10, snapshot.talismanTreasureTemplates().size());
        assertEquals(2, snapshot.refinementFailureLoot().defaults().size());
        assertEquals(2, snapshot.refinementFailureLoot().entriesForTier("low").size());
        assertEquals(snapshot.refinementFailureLoot().defaults(),
                snapshot.refinementFailureLoot().entriesForTier("unknown_tier"));
        assertEquals(217, snapshot.sourceFileEntryCounts().get("artifacts_catalog.json"));
        assertEquals(73, snapshot.sourceFileEntryCounts().get("refinement_recipes.json"));
    }

    @Test
    void exposesFlyingArtifactMetadataForExistingItems() {
        ArtifactDataService.Snapshot snapshot = ArtifactDataService.builtin();

        ArtifactDataService.ArtifactDefinition flyingSword = snapshot
                .findArtifact(FlyingArtifactItem.FLYING_SWORD_ARTIFACT_ID)
                .orElseThrow();
        assertEquals("flying_sword_low", flyingSword.id());
        assertEquals("low", flyingSword.tier());
        assertEquals("flying_sword", flyingSword.type());
        assertEquals("QI_REFINING", flyingSword.realmMin());
        assertEquals(2, flyingSword.gameTier());
        assertEquals("法器（低阶）", snapshot.tierDisplay(flyingSword.tier()));

        ArtifactDataService.ArtifactDefinition windArtifact = snapshot
                .findArtifact(FlyingArtifactItem.FLYING_ARTIFACT_ID)
                .orElseThrow();
        assertEquals("wind_escape_sail", windArtifact.id());
        assertEquals("mid", windArtifact.tier());
        assertEquals("movement", windArtifact.type());
        assertEquals("FOUNDATION", windArtifact.realmMin());
        assertEquals(4, windArtifact.gameTier());
    }

    @Test
    void exposesRefinementRulesAndPriorityLists() {
        ArtifactDataService.Snapshot snapshot = ArtifactDataService.builtin();

        assertEquals(0.25D, snapshot.realmPowerScale().belowRealmMin());
        assertEquals(0.7D, snapshot.realmPowerScale().atRealmMin());
        assertEquals(1.0D, snapshot.realmPowerScale().twoMajorAbove());

        assertTrue(snapshot.priorityIds("P0_launch").contains("flying_sword_low"));
        assertTrue(snapshot.priorityIds("P0_launch").contains("artifact_repair_kit"));
        assertFalse(snapshot.priorityIds("P0_launch").contains("xuanguang_mirror"));

        ArtifactDataService.RefinementRecipe recipe = snapshot.findRecipeByArtifact("wind_escape_sail").orElseThrow();
        assertEquals("refine_wind_escape_sail", recipe.id());
        assertEquals(2, recipe.forgeGrade());
        assertEquals(0.45D, recipe.baseSuccessRate());
    }

    @Test
    void resolvesPriorityArtifactDefinitions() {
        ArtifactDataService.Snapshot snapshot = ArtifactDataService.builtin();

        List<ArtifactDataService.ArtifactDefinition> p0 = snapshot.priorityArtifacts("P0_launch");

        assertEquals(snapshot.priorityIds("P0_launch").size(), p0.size());
        assertFalse(p0.isEmpty());
        assertEquals("flying_sword_low", p0.get(0).id());
        assertTrue(p0.stream().anyMatch(artifact -> artifact.id().equals("artifact_repair_kit")));
    }

    @Test
    void firstP0ArtifactCarrierIdsExistInCatalog() {
        ArtifactDataService.Snapshot snapshot = ArtifactDataService.builtin();

        assertTrue(snapshot.findArtifact("cloud_boots").isPresent());
        assertTrue(snapshot.findArtifact("spirit_gathering_bead").isPresent());
        assertTrue(snapshot.findArtifact("artifact_repair_kit").isPresent());
        assertEquals("extra_slots_8", snapshot.findArtifact("storage_bracelet_low").orElseThrow().effect());
    }

    @Test
    void priorityArtifactCarrierResourcesExist() throws Exception {
        ArtifactDataService.Snapshot snapshot = ArtifactDataService.builtin();
        JsonObject zhCn = readJson(Path.of("src/main/resources/assets/seeking_immortals/lang/zh_cn.json"));
        JsonObject enUs = readJson(Path.of("src/main/resources/assets/seeking_immortals/lang/en_us.json"));

        for (String id : PRIORITY_CARRIER_IDS) {
            assertTrue(snapshot.findArtifact(id).isPresent(), "Missing catalog entry for " + id);
            assertTrue(Files.exists(Path.of("src/main/resources/assets/seeking_immortals/models/item", id + ".json")),
                    "Missing item model for " + id);
            assertTrue(zhCn.has("item.seeking_immortals." + id), "Missing zh_cn item name for " + id);
            assertTrue(enUs.has("item.seeking_immortals." + id), "Missing en_us item name for " + id);
        }
    }

    private static JsonObject readJson(Path path) throws Exception {
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }
}
