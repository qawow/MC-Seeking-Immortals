package com.xunxian.seekingimmortals.resources;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.xunxian.seekingimmortals.catalog.ItemCatalogService;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Repo-checked census of item acquisition. Counting orphans by hand went wrong twice — once at ~646
 * (bulk items only, loose notion of obtainable) and once at 1128 before every channel was known — so
 * the census lives here instead of in a notebook.
 *
 * <p>Two rules make the number trustworthy. Only <em>grant positions</em> count: a recipe result, a
 * loot entry, a shop's delivered {@code item}, an alchemy output. An id appearing as an ingredient
 * proves nothing. And every channel must contribute, because a channel that silently stops matching
 * makes items look unobtainable that are not — the error that inflated 0.2.268's dead-recipe count
 * from 10 to 55 four separate times.</p>
 */
class ItemAcquisitionAuditTest {
    private static final Path DATA = Path.of("src", "main", "resources", "data", "seeking_immortals");
    private static final Path ASSETS = Path.of("src", "main", "resources", "assets", "seeking_immortals");
    private static final Path JAVA = Path.of("src", "main", "java", "com", "xunxian", "seekingimmortals");

    /**
     * Registered ids with no grant position. Held as a ceiling: completing an item's acquisition
     * lowers it, and adding an unreachable item raises it and fails.
     */
    private static final int ORPHAN_CEILING = 1128;

    /** Every id with an item model is registered — hand-written, bulk catalog, or enum-driven pills. */
    private static final int REGISTERED_ITEMS = 1667;

    @Test
    void everyRegisteredItemIsCountedAndTheOrphanCeilingHolds() throws Exception {
        Set<String> registered = registeredItems();
        assertEquals(REGISTERED_ITEMS, registered.size(),
                "the registered-item universe moved; re-run the census before changing the ceiling");

        Set<String> obtainable = canonical(grantPositions());
        List<String> orphans = new ArrayList<>();
        for (String id : new TreeSet<>(registered)) {
            if (!obtainable.contains(ItemCatalogService.resolveId(id))) {
                orphans.add(id);
            }
        }
        assertTrue(orphans.size() <= ORPHAN_CEILING,
                "items with no way to obtain them rose above the ceiling (" + ORPHAN_CEILING + "): "
                        + orphans.size() + "; a new item needs a grant position in the same change");
    }

    @Test
    void everyGrantChannelStillContributes() throws Exception {
        Map<String, Set<String>> channels = grantChannels();
        List<String> silent = new ArrayList<>();
        channels.forEach((name, ids) -> {
            if (ids.isEmpty()) {
                silent.add(name);
            }
        });
        // A channel that stops matching makes items look unobtainable that are not. This is the exact
        // failure that inflated 0.2.268's dead-recipe count from the true 10 to 55, four times over.
        assertEquals(List.of(), silent, "grant channels that matched nothing: " + silent);
        assertEquals(17, channels.size(), "a grant channel was added or removed; confirm the census");
    }

    /**
     * Shop entries label with {@code id} but deliver {@code item}, so a label can name an id nobody
     * can obtain. That is how the 0.2.268 gap hid, and it is still true for nine pill labels whose
     * delivered item is the graded ladder entry instead.
     */
    @Test
    void shopLabelsThatDifferFromTheDeliveredItemStayKnown() throws Exception {
        List<String> mismatched = new ArrayList<>();
        try (Stream<Path> files = Files.walk(DATA.resolve("shops"))) {
            for (Path path : files.filter(p -> p.toString().endsWith(".json")).toList()) {
                JsonArray entries = read(path).getAsJsonArray("entries");
                if (entries == null) {
                    continue;
                }
                for (JsonElement element : entries) {
                    JsonObject entry = element.getAsJsonObject();
                    String label = bare(optString(entry, "id"));
                    String delivered = bare(optString(entry, "item"));
                    if (!label.isBlank() && !delivered.isBlank() && !label.equals(delivered)) {
                        mismatched.add(path.getFileName() + ":" + label + "->" + delivered);
                    }
                }
            }
        }
        // Held as a ceiling, not zero: most are deliberate grade aliases (foundation_pill ->
        // foundation_building_pill_low). Growth means a new label advertises an id nobody can receive.
        assertTrue(mismatched.size() <= 134,
                "shop labels diverging from the delivered item rose to " + mismatched.size() + ": "
                        + mismatched);
        // The nine pill labels whose delivered item is the graded ladder entry: the label id itself has
        // no grant position anywhere, which is why it reads as unobtainable in the census above.
        assertTrue(mismatched.contains("market_herbal_stall.json:bigu_pill->fasting_pill_low"),
                "the known pill-label aliases must stay visible to this audit");
    }

    /** Ids a player can actually receive, one entry per channel so a dead channel is visible. */
    private static Map<String, Set<String>> grantChannels() throws IOException {
        Map<String, Set<String>> channels = new LinkedHashMap<>();
        Set<String> recipes = new LinkedHashSet<>();
        walkJson(DATA.resolve("recipes"), root -> {
            JsonElement result = root.get("result");
            if (result != null) {
                recipes.add(bare(result.isJsonObject()
                        ? optString(result.getAsJsonObject(), "item")
                        : result.getAsString()));
            }
        });
        channels.put("recipe", recipes);

        Set<String> loot = new LinkedHashSet<>();
        walkJson(DATA.resolve("loot_tables"), root -> collectLoot(root, loot));
        channels.put("loot", loot);

        Set<String> shops = new LinkedHashSet<>();
        walkJson(DATA.resolve("shops"), root -> {
            JsonArray entries = root.getAsJsonArray("entries");
            if (entries != null) {
                for (JsonElement entry : entries) {
                    shops.add(bare(optString(entry.getAsJsonObject(), "item")));
                }
            }
        });
        channels.put("shop", shops);

        Set<String> alchemy = new LinkedHashSet<>();
        walkJson(DATA.resolve("alchemy/recipes"), root -> {
            JsonElement outputs = root.get("output_items");
            if (outputs != null && outputs.isJsonObject()) {
                for (Map.Entry<String, JsonElement> output : outputs.getAsJsonObject().entrySet()) {
                    if (output.getValue().isJsonPrimitive()) {
                        alchemy.add(bare(output.getValue().getAsString()));
                    }
                }
            }
        });
        channels.put("alchemy", alchemy);

        Set<String> beast = new LinkedHashSet<>();
        for (JsonElement tier : read(DATA.resolve("text_material/beast_loot_tiers.json"))
                .getAsJsonArray("tiers")) {
            for (JsonElement drop : tier.getAsJsonObject().getAsJsonArray("drops")) {
                beast.add(bare(optString(drop.getAsJsonObject(), "item")));
            }
        }
        channels.put("beast_tier", beast);

        // BossLootService reads the runtime file first; the authored table is only a fallback.
        channels.put("boss_runtime", dropsFrom(DATA.resolve("worldpack/boss_loot_runtime.json")));
        channels.put("boss_authored", itemsUnder(DATA.resolve("text_material/boss_loot_tables.json")));
        channels.put("quest_reward", itemsUnder(DATA.resolve("text_material/main_quest_rewards_v101.json")));
        channels.put("npc_reward", itemsUnder(DATA.resolve("text_material/named_npc_loot_rewards_v97.json")));
        channels.put("ascension", itemsUnder(DATA.resolve("text_material/ascension_loadout_v95.json")));
        channels.put("auction", itemsUnder(DATA.resolve("text_material/wanbao_auction_artifacts.json")));
        channels.put("refine_failure", itemsUnder(DATA.resolve("artifacts/refinement_failure_loot.json")));
        channels.put("puppet_craft", itemsUnder(DATA.resolve("text_material/puppet_craft_recipes.json")));

        Set<String> refinement = new LinkedHashSet<>();
        for (JsonElement recipe : read(DATA.resolve("artifacts/refinement_recipes.json"))
                .getAsJsonArray("recipes")) {
            refinement.add(bare(optString(recipe.getAsJsonObject(), "artifact_id")));
        }
        channels.put("refinement", refinement);

        // Talisman crafting projects the authored corpus; a blueprint without materials grants nothing.
        Set<String> talismans = new LinkedHashSet<>();
        for (JsonElement recipe : read(DATA.resolve("text_material/talisman_recipes.json"))
                .getAsJsonArray("recipes")) {
            JsonObject entry = recipe.getAsJsonObject();
            JsonArray materials = entry.getAsJsonArray("materials");
            if (materials != null && !materials.isEmpty()) {
                talismans.add(bare(optString(entry, "talisman_id")));
            }
        }
        channels.put("talisman_craft", talismans);

        channels.put("stone_ladder", constantIds(
                JAVA.resolve("catalog/SpiritStoneLadderService.java"),
                "exchange\\(player,\\s*ModItems\\.[A-Z0-9_]+\\.get\\(\\),\\s*ModItems\\.([A-Z0-9_]+)\\.get\\(\\)"));
        // Services that hand out a hardcoded ItemStack are a grant channel too: breakthrough aids,
        // quest hand-outs, planter harvests. Omitting them made 6 obtainable items look unreachable.
        channels.put("hardcoded", hardcodedGrants());

        channels.replaceAll((name, ids) -> {
            ids.remove("");
            return ids;
        });
        return channels;
    }

    private static Set<String> grantPositions() throws IOException {
        Set<String> granted = new LinkedHashSet<>();
        grantChannels().values().forEach(granted::addAll);
        return granted;
    }

    /**
     * The universe of registered items. Item models are the one surface every registration path
     * reaches: hand-written {@code ModItems} literals, the seven helper factories, the bulk catalog
     * and the enum-driven graded pills that carry no id literal at all.
     */
    private static Set<String> registeredItems() throws IOException {
        Set<String> ids = new LinkedHashSet<>();
        try (Stream<Path> files = Files.list(ASSETS.resolve("models/item"))) {
            for (Path path : files.filter(p -> p.toString().endsWith(".json")).toList()) {
                String name = path.getFileName().toString();
                ids.add(name.substring(0, name.length() - ".json".length()));
            }
        }
        // Guard the proxy: every bulk-catalog id must have a model, or models are not the full universe.
        List<String> missingModel = new ArrayList<>();
        for (JsonElement item : read(ASSETS.resolve("catalog_bulk_items.json")).getAsJsonArray("items")) {
            String id = optString(item.getAsJsonObject(), "id");
            if (!ids.contains(id)) {
                missingModel.add(id);
            }
        }
        assertEquals(List.of(), missingModel,
                "bulk items without an item model, so the model set is not the registered universe");
        return ids;
    }

    private static Set<String> canonical(Set<String> ids) {
        Set<String> resolved = new LinkedHashSet<>();
        for (String id : ids) {
            String canonical = ItemCatalogService.resolveId(id);
            resolved.add(canonical == null || canonical.isBlank() ? id : canonical);
        }
        assertFalse(resolved.isEmpty(), "alias resolution collapsed the grant set");
        return resolved;
    }

    /**
     * Resolve {@code ModItems.CONSTANT} references to the ids they register. The constant name is not
     * always the id (helper factories and graded pills rename), so the mapping is read from the
     * registration source rather than lower-cased.
     */
    private static Set<String> constantIds(Path source, String regex) throws IOException {
        Map<String, String> byConstant = registeredConstants();
        Set<String> ids = new LinkedHashSet<>();
        Matcher matcher = Pattern.compile(regex)
                .matcher(Files.readString(source).replaceAll("\\s+", " "));
        while (matcher.find()) {
            String id = byConstant.get(matcher.group(1));
            if (id != null) {
                ids.add(id);
            }
        }
        return ids;
    }

    private static Map<String, String> registeredConstants() throws IOException {
        Map<String, String> byConstant = new LinkedHashMap<>();
        String source = Files.readString(JAVA.resolve("registry/ModItems.java")).replaceAll("\\s+", " ");
        Matcher matcher = Pattern.compile(
                "RegistryObject<[^>]+> ([A-Z0-9_]+) = (?:ITEMS\\.register|register\\w+)\\( ?\"([a-z0-9_]+)\"")
                .matcher(source);
        while (matcher.find()) {
            byConstant.put(matcher.group(1), matcher.group(2));
        }
        assertTrue(byConstant.size() > 350,
                "ModItems constant harvest looks broken: " + byConstant.size());
        return byConstant;
    }

    /** Ids handed out as a literal {@code ItemStack} by any service that delivers to a player. */
    private static Set<String> hardcodedGrants() throws IOException {
        Map<String, String> byConstant = registeredConstants();
        Pattern delivers = Pattern.compile("giveOrEnqueue|addItem\\(new ItemStack|drop\\(new ItemStack");
        Pattern reference = Pattern.compile("ModItems\\.([A-Z0-9_]+)\\.get\\(\\)");
        Set<String> ids = new LinkedHashSet<>();
        try (Stream<Path> files = Files.walk(JAVA)) {
            for (Path path : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                String source = Files.readString(path);
                if (!delivers.matcher(source).find()) {
                    continue;
                }
                Matcher matcher = reference.matcher(source);
                while (matcher.find()) {
                    String id = byConstant.get(matcher.group(1));
                    if (id != null) {
                        ids.add(id);
                    }
                }
            }
        }
        return ids;
    }

    private static Set<String> dropsFrom(Path path) {
        Set<String> ids = new LinkedHashSet<>();
        for (JsonElement table : read(path).getAsJsonArray("tables")) {
            JsonArray drops = table.getAsJsonObject().getAsJsonArray("drops");
            if (drops == null) {
                continue;
            }
            for (JsonElement drop : drops) {
                ids.add(bare(optString(drop.getAsJsonObject(), "item")));
            }
        }
        return ids;
    }

    /** Reward corpora nest item ids at varying depths, so collect known item-bearing keys recursively. */
    private static Set<String> itemsUnder(Path path) {
        Set<String> ids = new LinkedHashSet<>();
        if (Files.exists(path)) {
            collectItems(read(path), ids);
        }
        return ids;
    }

    private static void collectItems(JsonElement element, Set<String> ids) {
        if (element.isJsonObject()) {
            for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
                if (entry.getValue().isJsonPrimitive() && isItemKey(entry.getKey())) {
                    ids.add(bare(entry.getValue().getAsString()));
                }
                collectItems(entry.getValue(), ids);
            }
        } else if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) {
                collectItems(child, ids);
            }
        }
    }

    private static boolean isItemKey(String key) {
        return switch (key) {
            case "item", "item_id", "id", "reward_item", "output", "artifact_id", "loot_item" -> true;
            default -> false;
        };
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
        if ("minecraft:item".equals(optString(entry, "type"))) {
            granted.add(bare(optString(entry, "name")));
        }
        JsonArray children = entry.getAsJsonArray("children");
        if (children != null) {
            for (JsonElement child : children) {
                collectLootEntry(child.getAsJsonObject(), granted);
            }
        }
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
