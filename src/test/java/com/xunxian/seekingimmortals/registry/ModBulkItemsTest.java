package com.xunxian.seekingimmortals.registry;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pure resource-level checks for the bulk catalog pipeline.
 * Avoids touching {@link ModBulkItems} class init (Forge DeferredRegister) in unit tests.
 */
class ModBulkItemsTest {
    private static final Set<String> UNIQUE_FORBIDDEN = Set.of(
            "palm_heaven_bottle",
            "palm_sky_bottle",
            "heaven_palm_vase",
            "green_liquid",
            "lv_ye",
            "garden_liquid",
            "little_green_bottle",
            "mystic_green_liquid"
    );

    @Test
    void catalogBulkItemsJsonCoversExpandedCarriers() throws Exception {
        JsonObject root;
        try (InputStream in = ModBulkItemsTest.class.getClassLoader()
                .getResourceAsStream("assets/seeking_immortals/catalog_bulk_items.json")) {
            assertNotNull(in, "catalog_bulk_items.json missing on classpath");
            root = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        }
        JsonArray items = root.getAsJsonArray("items");
        assertNotNull(items);
        assertTrue(items.size() >= 1100, "bulk size=" + items.size());

        Set<String> ids = new HashSet<>();
        boolean hasDingshenGrade = false;
        for (JsonElement el : items) {
            JsonObject o = el.getAsJsonObject();
            String id = o.get("id").getAsString().toLowerCase(Locale.ROOT);
            ids.add(id);
            assertFalse(UNIQUE_FORBIDDEN.contains(id), "unique forbidden in bulk: " + id);
            if ("dingshen_fu".equals(id)) {
                assertTrue(o.has("grade") && !o.get("grade").getAsString().isBlank());
                hasDingshenGrade = true;
            }
        }
        assertTrue(ids.contains("altar_stone"));
        assertTrue(ids.contains("array_disk_basic"));
        assertTrue(ids.contains("dingshen_fu"));
        assertTrue(ids.contains("blood_escape_fu"));
        assertTrue(ids.contains("palm_heaven_bottle_stand"));
        assertTrue(ids.contains("market_stall_counter"));
        assertTrue(hasDingshenGrade);
    }
}
