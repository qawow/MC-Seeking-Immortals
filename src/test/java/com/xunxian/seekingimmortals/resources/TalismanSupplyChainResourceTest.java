package com.xunxian.seekingimmortals.resources;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A shipped recipe whose ingredient has no grant position anywhere is dead content: it shows up in
 * JEI and can never be crafted. Before this batch 41 of 84 shipped recipes were in that state, and
 * four ingredients alone accounted for 33 of them — {@code spirit_silk} (13), {@code talisman_paper}
 * (9), {@code fire_feather} (8) and {@code ghost_wood} (3).
 *
 * <p>This test harvests only <em>grant positions</em> — a recipe result, a loot-table entry, a shop
 * entry's delivered item, an alchemy output. An id appearing as an ingredient proves nothing about
 * whether a player can obtain it, which is exactly how the gap stayed invisible.</p>
 */
class TalismanSupplyChainResourceTest {
    private static final Path DATA = Path.of("src", "main", "resources", "data", "seeking_immortals");

    /** Ingredients that gate the authored talisman and paper chains. */
    private static final List<String> TALISMAN_CHAIN_ROOTS = List.of(
            "spirit_silk", "talisman_paper", "fire_feather", "ghost_wood",
            "talisman_ink_bottle", "beast_blood_ink", "star_moon_grass");

    @Test
    void everyTalismanChainRootHasAGrantPosition() throws Exception {
        Set<String> obtainable = grantPositions();
        // Sanity: the harvest must actually find things, or the assertions below are vacuous.
        assertTrue(obtainable.size() > 300, "grant harvest looks broken: " + obtainable.size());
        assertTrue(obtainable.contains("spirit_stone_shard"), "known shop item must be harvested");

        List<String> missing = new ArrayList<>();
        for (String id : TALISMAN_CHAIN_ROOTS) {
            if (!obtainable.contains(id)) {
                missing.add(id);
            }
        }
        assertTrue(missing.isEmpty(),
                "talisman chain roots with no way to obtain them, so every recipe consuming them is "
                        + "dead content: " + missing);
    }

    @Test
    void noShippedRecipeDependsOnAnUnobtainableModIngredient() throws Exception {
        Set<String> obtainable = grantPositions();
        List<String> dead = new ArrayList<>();
        try (Stream<Path> files = Files.walk(DATA.resolve("recipes"))) {
            for (Path path : files.filter(p -> p.toString().endsWith(".json")).toList()) {
                Set<String> missing = new LinkedHashSet<>();
                for (String ingredient : ingredientsOf(read(path))) {
                    if (ingredient.startsWith("minecraft:")) {
                        continue;
                    }
                    String id = bare(ingredient);
                    if (!obtainable.contains(id)) {
                        missing.add(id);
                    }
                }
                if (!missing.isEmpty()) {
                    dead.add(path.getFileName() + " needs " + missing);
                }
            }
        }
        // Held at zero: a shipped recipe nobody can craft is dead content that still shows in JEI.
        // Adding a recipe means adding a way to obtain its ingredients in the same change.
        assertEquals(List.of(), dead,
                "shipped recipes with an unobtainable ingredient: " + dead.size());
    }

    @Test
    void authoredPaperLadderIsFullyCraftable() throws Exception {
        JsonObject catalog = read(DATA.resolve("text_material/talisman_materials_catalog.json"));
        Set<String> obtainable = grantPositions();
        List<String> broken = new ArrayList<>();
        for (JsonElement element : catalog.getAsJsonArray("craft_recipes")) {
            JsonObject recipe = element.getAsJsonObject();
            if (!obtainable.contains(bare(recipe.get("output").getAsString()))) {
                broken.add(recipe.get("id").getAsString() + " output");
            }
            for (JsonElement material : recipe.getAsJsonArray("materials")) {
                String id = bare(material.getAsJsonObject().get("id").getAsString());
                if (!obtainable.contains(id)) {
                    broken.add(recipe.get("id").getAsString() + " needs " + id);
                }
            }
        }
        assertEquals(List.of(), broken, "the authored paper ladder must be walkable end to end");
    }

    /** Ids a player can actually receive: recipe results, loot entries, shop deliveries, alchemy outputs. */
    private static Set<String> grantPositions() throws IOException {
        Set<String> granted = new HashSet<>();
        walkJson(DATA.resolve("recipes"), root -> {
            JsonElement result = root.get("result");
            if (result != null) {
                granted.add(bare(result.isJsonObject()
                        ? result.getAsJsonObject().get("item").getAsString()
                        : result.getAsString()));
            }
        });
        walkJson(DATA.resolve("loot_tables"), root -> collectLoot(root, granted));
        walkJson(DATA.resolve("shops"), root -> {
            JsonArray entries = root.getAsJsonArray("entries");
            if (entries != null) {
                for (JsonElement entry : entries) {
                    JsonElement item = entry.getAsJsonObject().get("item");
                    if (item != null) {
                        granted.add(bare(item.getAsString()));
                    }
                }
            }
        });
        // Alchemy outputs live under output_items keyed low/medium/high/supreme.
        walkJson(DATA.resolve("alchemy/recipes"), root -> {
            JsonElement outputs = root.get("output_items");
            if (outputs != null && outputs.isJsonObject()) {
                for (var entry : outputs.getAsJsonObject().entrySet()) {
                    if (entry.getValue().isJsonPrimitive()) {
                        granted.add(bare(entry.getValue().getAsString()));
                    }
                }
            }
        });
        // Beast tier drops are a real runtime grant channel (BeastLootService reads this file).
        JsonObject tiers = read(DATA.resolve("text_material/beast_loot_tiers.json"));
        for (JsonElement tier : tiers.getAsJsonArray("tiers")) {
            for (JsonElement drop : tier.getAsJsonObject().getAsJsonArray("drops")) {
                granted.add(bare(drop.getAsJsonObject().get("item").getAsString()));
            }
        }
        // Spirit stones ladder up through SpiritStoneLadderService, so every exchange OUTPUT is a
        // grant position even though no data file lists it. Harvested from source so the set cannot
        // drift from the implementation.
        granted.addAll(spiritStoneExchangeOutputs());
        // Boss drops likewise: BossLootService reads worldpack/boss_loot_runtime.json first and
        // only falls back to the authored text_material table when the runtime file is absent.
        for (JsonElement table : read(DATA.resolve("worldpack/boss_loot_runtime.json"))
                .getAsJsonArray("tables")) {
            JsonArray drops = table.getAsJsonObject().getAsJsonArray("drops");
            if (drops == null) {
                continue;
            }
            for (JsonElement drop : drops) {
                JsonElement item = drop.getAsJsonObject().get("item");
                if (item != null) {
                    granted.add(bare(item.getAsString()));
                }
            }
        }
        return granted;
    }

    /**
     * Exchange outputs from {@code SpiritStoneLadderService.tryElementalUpgrade}, harvested from the
     * source text. Every {@code exchange(player, FROM, TO, ratio)} makes TO obtainable.
     */
    private static Set<String> spiritStoneExchangeOutputs() {
        String source;
        try {
            source = Files.readString(Path.of("src/main/java/com/xunxian/seekingimmortals/catalog"
                    + "/SpiritStoneLadderService.java"));
        } catch (IOException exception) {
            throw new AssertionError("SpiritStoneLadderService moved; update this harvest", exception);
        }
        Set<String> outputs = new HashSet<>();
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("exchange\\(player,\\s*ModItems\\.[A-Z0-9_]+\\.get\\(\\),\\s*ModItems\\.([A-Z0-9_]+)\\.get\\(\\)")
                .matcher(source.replaceAll("\\s+", " "));
        while (matcher.find()) {
            outputs.add(matcher.group(1).toLowerCase(java.util.Locale.ROOT));
        }
        assertTrue(outputs.contains("metal_spirit_stone_mid"),
                "the spirit stone ladder harvest must find the elemental upgrade chain");
        return outputs;
    }

    private static void collectLoot(JsonObject root, Set<String> granted) {
        JsonArray pools = root.getAsJsonArray("pools");
        if (pools == null) {
            return;
        }
        for (JsonElement pool : pools) {
            JsonArray entries = pool.getAsJsonObject().getAsJsonArray("entries");
            if (entries == null) {
                continue;
            }
            for (JsonElement entry : entries) {
                collectLootEntry(entry.getAsJsonObject(), granted);
            }
        }
    }

    private static void collectLootEntry(JsonObject entry, Set<String> granted) {
        JsonElement name = entry.get("name");
        if ("minecraft:item".equals(optString(entry, "type")) && name != null) {
            granted.add(bare(name.getAsString()));
        }
        JsonArray children = entry.getAsJsonArray("children");
        if (children != null) {
            for (JsonElement child : children) {
                collectLootEntry(child.getAsJsonObject(), granted);
            }
        }
    }

    private static List<String> ingredientsOf(JsonObject root) {
        List<String> ingredients = new ArrayList<>();
        JsonArray declared = root.getAsJsonArray("ingredients");
        if (declared != null) {
            for (JsonElement element : declared) {
                if (element.isJsonObject() && element.getAsJsonObject().has("item")) {
                    ingredients.add(element.getAsJsonObject().get("item").getAsString());
                }
            }
        }
        JsonElement key = root.get("key");
        if (key != null && key.isJsonObject()) {
            for (var entry : key.getAsJsonObject().entrySet()) {
                if (entry.getValue().isJsonObject() && entry.getValue().getAsJsonObject().has("item")) {
                    ingredients.add(entry.getValue().getAsJsonObject().get("item").getAsString());
                }
            }
        }
        return ingredients;
    }

    private static void walkJson(Path root, java.util.function.Consumer<JsonObject> consumer)
            throws IOException {
        if (!Files.isDirectory(root)) {
            return;
        }
        try (Stream<Path> files = Files.walk(root)) {
            for (Path path : files.filter(p -> p.toString().endsWith(".json")).toList()) {
                consumer.accept(read(path));
            }
        }
    }

    private static String optString(JsonObject object, String field) {
        JsonElement value = object.get(field);
        return value == null || !value.isJsonPrimitive() ? "" : value.getAsString();
    }

    private static String bare(String id) {
        return id == null ? "" : id.substring(id.indexOf(':') + 1);
    }

    private static JsonObject read(Path path) {
        try {
            return JsonParser.parseString(Files.readString(path)).getAsJsonObject();
        } catch (IOException exception) {
            throw new AssertionError("unreadable json: " + path, exception);
        }
    }
}
