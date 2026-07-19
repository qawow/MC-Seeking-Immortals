package com.xunxian.seekingimmortals.item;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

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
        assertTrue(count >= 1190, "count=" + count);
        assertTrue(placeholders >= 800, "placeholders=" + placeholders);
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
    }
}
