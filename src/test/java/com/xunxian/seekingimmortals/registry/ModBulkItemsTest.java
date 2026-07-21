package com.xunxian.seekingimmortals.registry;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.xunxian.seekingimmortals.alchemy.AlchemyFormulaSource;
import com.xunxian.seekingimmortals.item.pill.PillEffectCatalog;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
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

    @Test
    void classifierUsesAuthoritativeKindsAndRecipeOutputs() {
        assertEquals(BulkItemKind.FORMULA,
                BulkItemClassifier.classify("recipe_bigu", "consumable"));
        assertEquals("bigu_pill", BulkItemClassifier.recipeOutput("recipe_bigu").orElseThrow());
        assertEquals(BulkItemKind.ARTIFACT,
                BulkItemClassifier.classify("flying_sword_low", "artifact"));
        assertEquals(BulkItemKind.CONSUMABLE,
                BulkItemClassifier.classify("spirit_rice_bowl", "consumable"));
        assertEquals(BulkItemKind.CONSUMABLE,
                BulkItemClassifier.classify("storage_pouch_low", "consumable"));
        assertEquals(BulkItemKind.CONSUMABLE,
                BulkItemClassifier.classify("detox_minor_pill", "consumable"));
        assertEquals("detox_minor", BulkItemClassifier.consumable("detox_minor_pill").orElseThrow().effect());
        assertEquals("talisman_craft_material",
                BulkItemClassifier.consumable("talisman_ink_bottle").orElseThrow().effect());
        assertEquals("array_fuel",
                BulkItemClassifier.consumable("spirit_sand_pouch").orElseThrow().effect());
        assertEquals("corpse_control",
                BulkItemClassifier.consumable("yin_coffin_nail").orElseThrow().effect());
        // ModItems startup registration calls orElseThrow() on these three; a missing
        // definition crashes the game during item registry events (2026-07-21 crash).
        assertEquals("puppet_repair",
                BulkItemClassifier.consumable("puppet_repair_kit").orElseThrow().effect());
        assertEquals("pet_loyalty_plus",
                BulkItemClassifier.consumable("spirit_beast_feed").orElseThrow().effect());
        assertEquals("pet_loyalty_plus",
                BulkItemClassifier.consumable("beast_feed_spirit").orElseThrow().effect());
        assertEquals("vehicle_craft",
                BulkItemClassifier.consumable("wind_feather_raft_blueprint").orElseThrow().effect());
        assertEquals(BulkItemKind.CONSUMABLE,
                BulkItemClassifier.classify("sect_contribution_token", "consumable"));
        assertEquals("sect_contribution_redeem",
                BulkItemClassifier.consumable("sect_contribution_token").orElseThrow().effect());
        assertEquals(BulkItemKind.PILL,
                BulkItemClassifier.classify("appearance_lock_pill", "pill"));
        assertEquals(BulkItemKind.PILL,
                BulkItemClassifier.classify("beast_taming_pill_low", "pill"));
        assertEquals(BulkItemKind.CONSUMABLE,
                BulkItemClassifier.classify("spirit_pill_voucher", "pill"));
        assertEquals("redeem_spirit_pill_voucher",
                BulkItemClassifier.consumable("spirit_pill_voucher").orElseThrow().effect());
        assertEquals(BulkItemKind.CONSUMABLE,
                BulkItemClassifier.classify("teleport_array_ticket", "access_item"));
        assertEquals("board_teleport_array",
                BulkItemClassifier.consumable("teleport_array_ticket").orElseThrow().effect());
        assertTrue(PillEffectCatalog.findByPillId("appearance_lock_pill").isPresent());
        assertTrue(PillEffectCatalog.findByPillId("beast_taming_pill_low").isPresent());
        assertTrue(PillEffectCatalog.findByPillId("marrow_drain_pill").isPresent());
        assertTrue(PillEffectCatalog.findByPillId("qingxu_pill").isPresent());
        assertEquals(BulkItemKind.MANUAL,
                BulkItemClassifier.classify("alchemy_manual_low", "manual"));
        assertEquals(BulkItemKind.MANUAL,
                BulkItemClassifier.classify("beast_taming_manual", "manual"));
        assertEquals(BulkItemKind.CONSUMABLE,
                BulkItemClassifier.classify("jade_slip_blank", "craft"));
        assertEquals(BulkItemKind.TALISMAN,
                BulkItemClassifier.classify("fire_burst_talisman", "talisman"));
        assertEquals(BulkItemKind.CARRIER,
                BulkItemClassifier.classify("talisman_paper", "talisman"));
        assertEquals(BulkItemKind.EQUIPMENT,
                BulkItemClassifier.classify("spirit_boat_low", "equipment"));
        assertEquals(BulkItemKind.EQUIPMENT,
                BulkItemClassifier.classify("basic_wood_puppet", "equipment"));


        assertEquals(BulkItemKind.CARRIER,
                BulkItemClassifier.classify("recipe_binding_talisman", "talisman"));
        assertEquals(BulkItemKind.CARRIER,
                BulkItemClassifier.classify("recipe_gold_armor_talisman", "talisman"));
        assertEquals(BulkItemKind.CARRIER,
                BulkItemClassifier.classify("recipe_invisibility_talisman", "talisman"));
        assertEquals(BulkItemKind.CARRIER,
                BulkItemClassifier.classify("evil_mirage_mirror_shard", "artifact"));
        assertEquals(BulkItemKind.CARRIER,
                BulkItemClassifier.classify("natal_artifact_embryo", "artifact"));
        assertEquals("restore_mana",
                PillEffectCatalog.findByPillId("spirit_recovery_pill_high").orElseThrow().effect());
        assertEquals("restore_mana_50pct",
                PillEffectCatalog.findByPillId("spirit_recovery_pill").orElseThrow().effect());
    }

    @Test
    void everyShippedAlchemyRecipeCarrierResolvesToARealOutput() throws Exception {
        Set<String> bulkIds = new HashSet<>();
        try (InputStream in = ModBulkItemsTest.class.getClassLoader()
                .getResourceAsStream("assets/seeking_immortals/catalog_bulk_items.json")) {
            assertNotNull(in);
            JsonObject root = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            for (JsonElement element : root.getAsJsonArray("items")) {
                bulkIds.add(element.getAsJsonObject().get("id").getAsString().toLowerCase(Locale.ROOT));
            }
        }
        try (InputStream in = ModBulkItemsTest.class.getClassLoader()
                .getResourceAsStream("data/seeking_immortals/catalog/alchemy_recipes_index.json")) {
            assertNotNull(in);
            JsonObject root = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            for (JsonElement element : root.getAsJsonArray("recipes")) {
                JsonObject recipe = element.getAsJsonObject();
                String id = recipe.get("id").getAsString().toLowerCase(Locale.ROOT);
                if (!bulkIds.contains(id)) {
                    continue;
                }
                assertEquals(BulkItemKind.FORMULA,
                        BulkItemClassifier.classify(id, "consumable"), id);
                assertEquals(recipe.get("output").getAsString().toLowerCase(Locale.ROOT),
                        BulkItemClassifier.recipeOutput(id).orElseThrow(), id);
            }
        }
    }

    @Test
    void formulaSourcesFollowShippedMediumSchema() throws Exception {
        int jade = 0;
        int sectSecret = 0;
        try (InputStream in = ModBulkItemsTest.class.getClassLoader()
                .getResourceAsStream("data/seeking_immortals/text_material/alchemy_recipes.json")) {
            assertNotNull(in);
            JsonObject root = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8))
                    .getAsJsonObject();
            for (JsonElement element : root.getAsJsonArray("recipes")) {
                JsonObject recipe = element.getAsJsonObject();
                String id = recipe.get("id").getAsString();
                String medium = recipe.has("medium") ? recipe.get("medium").getAsString() : "";
                AlchemyFormulaSource expected = switch (medium) {
                    case "jade_slip" -> {
                        jade++;
                        yield AlchemyFormulaSource.JADE;
                    }
                    case "sect_secret_scroll" -> {
                        sectSecret++;
                        yield AlchemyFormulaSource.SECT_SECRET;
                    }
                    case "", "paper_formula" -> AlchemyFormulaSource.PAPER;
                    default -> throw new IllegalStateException("unknown medium=" + medium);
                };
                assertEquals(expected, BulkItemClassifier.formulaSource(id), id);
            }
        }
        assertEquals(23, jade);
        assertEquals(14, sectSecret);
        assertEquals(AlchemyFormulaSource.PAPER,
                BulkItemClassifier.formulaSource("recipe_solid_essence_pill"));
    }
}
