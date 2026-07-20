package com.xunxian.seekingimmortals.item;

import com.xunxian.seekingimmortals.item.pill.PillEffectCatalog;
import com.xunxian.seekingimmortals.registry.BulkItemClassifier;
import com.xunxian.seekingimmortals.registry.BulkItemKind;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * P0 item routing fixes: ferry pass consumable, ferry fee tokens not vehicles,
 * alias pills classify as PILL.
 */
class ItemFerryAndPillRoutingTest {
    @Test
    void ferryPassIsExecutableConsumableAndRegisteredAsCatalogConsumable() throws Exception {
        assertTrue(BulkItemClassifier.consumable("ferry_pass").isPresent());
        assertEquals("travel_nether_ferry",
                BulkItemClassifier.consumable("ferry_pass").orElseThrow().effect());
        String source = Files.readString(Path.of(
                "src", "main", "java", "com", "xunxian", "seekingimmortals",
                "registry", "ModItems.java"));
        assertTrue(source.contains("FERRY_PASS"));
        assertTrue(source.contains("CatalogConsumableItem"));
        assertTrue(source.contains("ferry_pass"));
        assertFalse(source.contains("FERRY_PASS = ITEMS.register(\"ferry_pass\", () -> new Item("));
    }

    @Test
    void netherFerryFeeTokensAreNotBoardableEquipment() {
        assertFalse(BulkItemClassifier.isEquipment("nether_ferry_coin", "material"));
        assertFalse(BulkItemClassifier.isEquipment("nether_ferry_token", "material"));
        assertEquals(BulkItemKind.CARRIER, BulkItemClassifier.classify("nether_ferry_coin", "material"));
        assertEquals(BulkItemKind.CARRIER, BulkItemClassifier.classify("nether_ferry_token", "material"));
        assertEquals(CatalogEquipmentService.Mode.UNKNOWN,
                CatalogEquipmentService.resolveMode("nether_ferry_coin"));
        assertEquals(CatalogEquipmentService.Mode.UNKNOWN,
                CatalogEquipmentService.resolveMode("nether_ferry_token"));
        // Real vehicles still classify.
        assertTrue(BulkItemClassifier.isEquipment("spirit_boat_low", "equipment")
                || BulkItemClassifier.isEquipment("spirit_boat", "equipment")
                || BulkItemClassifier.classify("cloud_sedan", "equipment") == BulkItemKind.EQUIPMENT);
        assertEquals(CatalogEquipmentService.Mode.VEHICLE,
                CatalogEquipmentService.resolveMode("spirit_boat_low"));
    }

    @Test
    void netherFerrySpatialNodeAcceptsCoinAndTokenAlternatives() throws Exception {
        String json = Files.readString(Path.of(
                "src", "main", "resources", "data", "seeking_immortals",
                "catalog", "spatial_nodes_index.json"));
        assertTrue(json.contains("node_nether_ferry"));
        assertTrue(json.contains("yin_stone|nether_ferry_coin") || json.contains("nether_ferry_coin|yin_stone"));
        assertTrue(json.contains("ferry_pass|nether_ferry_token") || json.contains("nether_ferry_token|ferry_pass"));
        // Registry fallback must resolve bulk fee carriers by id.
        String mapper = Files.readString(Path.of(
                "src", "main", "java", "com", "xunxian", "seekingimmortals",
                "worldpack", "SpatialNodeRequiresService.java"));
        assertTrue(mapper.contains("default ->") || mapper.contains("ResourceLocation.tryParse"));
    }

    @Test
    void lifeMethodLayersMaxMatchesMatrixDepth() throws Exception {
        com.google.gson.JsonObject methods = com.google.gson.JsonParser.parseString(Files.readString(Path.of(
                "src", "main", "resources", "data", "seeking_immortals",
                "text_material", "cultivation_methods.json"))).getAsJsonObject();
        com.google.gson.JsonObject matrix = com.google.gson.JsonParser.parseString(Files.readString(Path.of(
                "src", "main", "resources", "data", "seeking_immortals",
                "text_material", "method_layer_technique_matrix_v130.json"))).getAsJsonObject();
        java.util.Map<String, Integer> totals = new java.util.HashMap<>();
        for (com.google.gson.JsonElement element : matrix.getAsJsonArray("method_layer_tables")) {
            com.google.gson.JsonObject table = element.getAsJsonObject();
            totals.put(table.get("method_id").getAsString(), table.get("total_layers").getAsInt());
        }
        for (String methodId : java.util.List.of("artifact_refining_basic", "treasure_appraisal_art")) {
            com.google.gson.JsonObject method = null;
            for (com.google.gson.JsonElement element : methods.getAsJsonArray("methods")) {
                com.google.gson.JsonObject candidate = element.getAsJsonObject();
                if (methodId.equals(candidate.get("id").getAsString())) {
                    method = candidate;
                    break;
                }
            }
            assertTrue(method != null, "missing method " + methodId);
            int layersMax = method.getAsJsonObject("setting").get("layers_max").getAsInt();
            assertEquals(totals.get(methodId).intValue(), layersMax, methodId);
        }
    }

    @Test
    void aliasPillsResolveThroughPillEffectCatalog() {
        assertTrue(PillEffectCatalog.findByPillId("appearance_lock_pill").isPresent());
        assertTrue(PillEffectCatalog.findByPillId("marrow_drain_pill").isPresent());
        assertTrue(PillEffectCatalog.findByPillId("qingxu_pill").isPresent());
        assertEquals("freeze_appearance",
                PillEffectCatalog.findByPillId("appearance_lock_pill").orElseThrow().effect());
        assertEquals("power_now_lifespan_debt",
                PillEffectCatalog.findByPillId("marrow_drain_pill").orElseThrow().effect());
        assertEquals("pet_mind_clarity",
                PillEffectCatalog.findByPillId("qingxu_pill").orElseThrow().effect());
        assertEquals(BulkItemKind.PILL, BulkItemClassifier.classify("appearance_lock_pill", "pill"));
        assertEquals(BulkItemKind.PILL, BulkItemClassifier.classify("marrow_drain_pill", "pill"));
        assertEquals(BulkItemKind.PILL, BulkItemClassifier.classify("qingxu_pill", "pill"));
    }
}
