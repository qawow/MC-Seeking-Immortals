package com.xunxian.seekingimmortals.craft;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The authored talisman corpus and the runtime used to be two unrelated lists: {@code
 * talisman_recipes.json} ships 24 blueprints with 24 distinct products, while {@code buildRecipes()}
 * hardcoded a parallel 24-entry list yielding only 5 products. Only 5 ids overlapped, so 19 authored
 * talismans had no craft route — and {@code recipeBlueprintCount()} returned a literal {@code 24}
 * that made the gap invisible to every count-based pin.
 *
 * <p>These assertions read source text and authored JSON rather than the item registry, so they run
 * without Forge bootstrap.</p>
 */
class TalismanAuthoredRecipeTest {
    private static final Path SERVICE = Path.of("src", "main", "java", "com", "xunxian",
            "seekingimmortals", "craft", "TalismanCraftService.java");
    private static final Path CORPUS = Path.of("src", "main", "resources", "data",
            "seeking_immortals", "text_material", "talisman_recipes.json");

    /** Authored stubs with no materials array; craftable blueprints must never invent inputs for them. */
    private static final Set<String> MATERIAL_LESS_STUBS = Set.of(
            "recipe_invisibility_talisman", "recipe_binding_talisman", "recipe_gold_armor_talisman");

    @Test
    void recipesAreNotHardcodedInJava() throws Exception {
        String source = Files.readString(SERVICE);
        assertFalse(source.contains("ModItems.TALISMAN_PAPER_MORTAL"),
                "talisman recipes must come from talisman_recipes.json, not a hardcoded ModItems list");
        assertTrue(source.contains("text_material/talisman_recipes.json"),
                "the service must name the authored corpus it reads");
        // The count must be derived. A literal return is exactly what hid 19 missing products.
        assertFalse(source.replaceAll("\\s+", " ").contains("recipeBlueprintCount() { return 24"),
                "recipeBlueprintCount must count the corpus, not return a literal");
    }

    @Test
    void blueprintCountMatchesTheAuthoredCorpus() throws Exception {
        assertEquals(authored().size(), TalismanCraftService.recipeBlueprintCount(),
                "blueprint count must track the authored corpus so adding a recipe cannot go unnoticed");
        assertEquals(24, TalismanCraftService.recipeBlueprintCount(),
                "the authored corpus ships 24 blueprints");
    }

    @Test
    void everyAuthoredProductIsDistinctAndFullyDescribed() throws Exception {
        List<JsonObject> entries = authored();
        Set<String> products = new LinkedHashSet<>();
        List<String> underspecified = new ArrayList<>();
        for (JsonObject entry : entries) {
            String id = entry.get("id").getAsString();
            assertTrue(products.add(entry.get("talisman_id").getAsString()),
                    "two blueprints share a product, so one of them is unreachable content: " + id);
            if (MATERIAL_LESS_STUBS.contains(id)) {
                assertFalse(entry.has("materials"),
                        "stub " + id + " gained materials; move it out of the stub set and let it craft");
                continue;
            }
            if (!entry.has("materials") || entry.getAsJsonArray("materials").isEmpty()) {
                underspecified.add(id + " has no materials");
            }
            if (!entry.has("ink")) {
                underspecified.add(id + " has no ink");
            }
        }
        // 24 blueprints, 24 distinct products: the old runtime produced 5.
        assertEquals(24, products.size(), "the corpus must describe 24 distinct products");
        assertEquals(List.of(), underspecified,
                "a craftable blueprint missing materials or ink would be omitted at runtime: " + underspecified);
    }

    @Test
    void authoredInkIsHonouredPerRecipeInsteadOfOneGlobalBottle() throws Exception {
        String source = Files.readString(SERVICE);
        // Every authored recipe names its own ink; charging one hardcoded bottle ignored that field
        // and, while talisman_ink_bottle had no source at all, closed the whole system in the data layer.
        assertFalse(source.contains("resolveCatalogItem(\"talisman_ink_bottle\")"),
                "ink must come from the recipe's own ink field, not one hardcoded bottle");
        assertTrue(source.contains("requirements.merge(recipe.ink(), requiredInkCount()"),
                "the transactional input set must charge the recipe's authored ink");
        Set<String> inks = new LinkedHashSet<>();
        for (JsonObject entry : authored()) {
            if (entry.has("ink")) {
                inks.add(entry.get("ink").getAsString());
            }
        }
        assertEquals(Set.of("beast_blood_ink", "spirit_plant_ink", "essence_herb_ink"), inks,
                "the authored ink set changed; confirm each one still has a craft route");
    }

    @Test
    void authoredYieldAndRealmGateReachTheRuntime() throws Exception {
        String source = Files.readString(SERVICE);
        // Author sets yield 1-3; the old runtime always produced exactly one.
        assertTrue(source.contains("new ItemStack(recipe.product(), recipe.yield())"),
                "the authored yield must be delivered, not silently reduced to one");
        assertTrue(source.contains("Realm.fromDesignId(text(entry, \"realm_min\"))"),
                "the authored realm_min must reach the runtime");
        // The realm gate has to precede the material commit, or a low-realm player pays and gets nothing.
        assertEquals("message.seeking_immortals.talisman_table.realm_too_low",
                TalismanCraftService.preflightFailure(false, true, true, false));
        Set<String> realms = new LinkedHashSet<>();
        for (JsonObject entry : authored()) {
            if (entry.has("realm_min")) {
                realms.add(entry.get("realm_min").getAsString());
            }
        }
        assertEquals(Set.of("QI_REFINING", "FOUNDATION", "CORE_FORMATION", "NASCENT_SOUL"), realms,
                "authored realm gates changed; every value must resolve via Realm.fromDesignId");
    }

    private static List<JsonObject> authored() throws Exception {
        JsonObject root = JsonParser.parseString(Files.readString(CORPUS)).getAsJsonObject();
        List<JsonObject> entries = new ArrayList<>();
        for (JsonElement element : root.getAsJsonArray("recipes")) {
            entries.add(element.getAsJsonObject());
        }
        return entries;
    }
}
