package com.xunxian.seekingimmortals.item;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CatalogItemDescriptionServiceTest {
    @Test
    void everyBulkCatalogItemHasConcretePurposeAndInteractionPolicy() throws Exception {
        int count = 0;
        int placeholders = 0;
        try (InputStream input = getClass().getClassLoader()
                .getResourceAsStream("assets/seeking_immortals/catalog_bulk_items.json")) {
            assertNotNull(input);
            JsonObject root = JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8))
                    .getAsJsonObject();
            for (JsonElement element : root.getAsJsonArray("items")) {
                JsonObject item = element.getAsJsonObject();
                String id = item.get("id").getAsString();
                String category = item.get("category").getAsString();
                String description = item.get("description").getAsString();
                CatalogItemDescriptionService.Profile profile = CatalogItemDescriptionService.profile(id, category);
                assertFalse(profile.purposeKey().isBlank(), id);
                assertFalse(profile.interactionKey().isBlank(), id);
                assertNotNull(profile.detailKey(), id);
                assertFalse(profile.purposeKey().endsWith("catalog_carrier"), id);
                if (CatalogItemDescriptionService.isPlaceholder(description)) {
                    placeholders++;
                }
                count++;
            }
        }
        // Three legacy generic pills were intentionally removed from the bulk catalog.
        assertEquals(1185, count, "bulk catalog count after legacy pill removal");
        assertTrue(placeholders >= 796, "placeholders=" + placeholders);
    }

    @Test
    void auditedCurrencyAndFormationItemsExposeExactPolicies() {
        CatalogItemDescriptionService.Profile token = CatalogItemDescriptionService.profile(
                "sect_contribution_token", "consumable");
        assertTrue(token.purposeKey().endsWith(".currency"));
        assertTrue(token.interactionKey().endsWith(".consume"));
        assertTrue(token.detailKey().endsWith(".sect_contribution_token"));

        CatalogItemDescriptionService.Profile disk = CatalogItemDescriptionService.profile(
                "spirit_gathering_array_disk", "consumable");
        assertTrue(disk.interactionKey().endsWith(".formation_activate"));
        assertTrue(disk.detailKey().endsWith(".spirit_gathering_array_disk"));

        CatalogItemDescriptionService.Profile component = CatalogItemDescriptionService.profile(
                "formation_flag_post", "artifact");
        assertTrue(component.purposeKey().endsWith(".formation"));
        assertTrue(component.interactionKey().endsWith(".material"));

        CatalogItemDescriptionService.Profile structureToken = CatalogItemDescriptionService.profile(
                "time_acceleration_array", "artifact");
        assertTrue(structureToken.detailKey().endsWith(".structure_token"));
        assertTrue(structureToken.interactionKey().endsWith(".material"));

        CatalogItemDescriptionService.Profile blueprint = CatalogItemDescriptionService.profile(
                "array_blueprint_scroll", "manual");
        assertTrue(blueprint.detailKey().endsWith(".array_blueprint_scroll"));
    }

    @Test
    void structureTokenCarrierDetectionCoversHighRiskIds() {
        assertTrue(CatalogItemDescriptionService.isStructureTokenCarrier("time_acceleration_array"));
        assertFalse(CatalogItemDescriptionService.isStructureTokenCarrier("structure_repair_bench"));
        assertFalse(CatalogItemDescriptionService.isStructureTokenCarrier("structure_blueprint_table"));
        assertTrue(CatalogItemDescriptionService.isStructureTokenCarrier("capture_point_obelisk"));
        assertTrue(CatalogItemDescriptionService.isStructureTokenCarrier("immortal_teleport_grand_array"));
        assertFalse(CatalogItemDescriptionService.isStructureTokenCarrier("spirit_gathering_array_disk"));
        assertFalse(CatalogItemDescriptionService.isStructureTokenCarrier("array_blueprint_scroll"));
    }

    @Test
    void catalogManualItemFallsThroughToFormationAfterStudy() throws Exception {
        String source = java.nio.file.Files.readString(java.nio.file.Path.of(
                "src", "main", "java", "com", "xunxian", "seekingimmortals",
                "item", "CatalogManualItem.java"));
        assertTrue(source.contains("FormationItemService.tryUse"));
        assertTrue(source.contains("hasStudied"));
        assertTrue(source.contains("FormationItemService.builtin().find(manualId).isEmpty()"));
    }
}
