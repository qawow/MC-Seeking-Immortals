package com.xunxian.seekingimmortals.craft;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.xunxian.seekingimmortals.SeekingImmortalsMod;
import com.xunxian.seekingimmortals.catalog.ItemCatalogService;
import com.xunxian.seekingimmortals.cultivation.CultivationHelper;
import com.xunxian.seekingimmortals.cultivation.PlayerCultivation;
import com.xunxian.seekingimmortals.cultivation.Realm;
import com.xunxian.seekingimmortals.item.InventoryDeliveryService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Talisman crafting driven by the authored corpus in {@code talisman_recipes.json}: 24 blueprints
 * with 24 distinct products. This runtime previously hardcoded a parallel 24-entry list that
 * produced only 5 items and shared just 5 ids with the corpus, so 19 authored talismans had no
 * craft route at all while {@code recipeBlueprintCount()} returned a literal 24 that hid the gap.
 *
 * <p>Blueprints fail closed: one whose product, ink or any material cannot be resolved is omitted
 * from the runtime instead of becoming a placeholder recipe. The three authored {@code recipe_*}
 * stubs carry no materials array, so they are counted as blueprints but never become craftable.</p>
 *
 * <p>Recipes build lazily so unit tests and class-loading do not touch the item registry early.</p>
 */
public final class TalismanCraftService {
    private static final String RECIPE_RESOURCE =
            "data/" + SeekingImmortalsMod.MODID + "/text_material/talisman_recipes.json";

    private static volatile List<Recipe> recipes;
    private static volatile List<JsonObject> authored;

    private TalismanCraftService() {}

    public record Material(Item item, int count) {}

    /** One authored blueprint; {@code yield}, {@code ink} and {@code realmMin} come from the corpus. */
    public record Recipe(String id, String display, List<Material> materials, Item product,
                         double successRate, int yield, Item ink, Realm realmMin) {}

    public record CraftResult(boolean success, Recipe recipe, ItemStack product, String messageKey) {}

    public static List<Recipe> recipes() {
        return ensureRecipes();
    }

    /** Registry-free authored blueprint count: every corpus entry, craftable or not. */
    public static int recipeBlueprintCount() {
        return authoredEntries().size();
    }

    public static Optional<Recipe> findCraftable(ServerPlayer player) {
        for (Recipe recipe : ensureRecipes()) {
            if (meetsRealm(player, recipe) && hasMaterials(player, recipe)) {
                return Optional.of(recipe);
            }
        }
        return Optional.empty();
    }

    public static CraftResult craft(ServerPlayer player) {
        Optional<Recipe> optional = findCraftable(player);
        if (optional.isEmpty()) {
            return new CraftResult(false, null, ItemStack.EMPTY, "message.seeking_immortals.talisman_table.missing_materials");
        }
        return craftRecipe(player, optional.get());
    }

    /** Wave466: craft a specific recipe by id (catalog authority bridge). */
    public static Optional<Recipe> find(String recipeId) {
        String id = recipeId == null ? "" : recipeId.trim().toLowerCase(Locale.ROOT);
        if (id.isBlank()) {
            return Optional.empty();
        }
        for (Recipe recipe : ensureRecipes()) {
            if (recipe.id().equals(id) || recipe.id().equalsIgnoreCase(recipeId)) {
                return Optional.of(recipe);
            }
            // Allow bare names without craft_ prefix.
            if (!id.startsWith("craft_") && recipe.id().equals("craft_" + id)) {
                return Optional.of(recipe);
            }
        }
        return Optional.empty();
    }

    public static CraftResult craftById(ServerPlayer player, String recipeId) {
        Optional<Recipe> optional = find(recipeId);
        if (optional.isEmpty()) {
            return new CraftResult(false, null, ItemStack.EMPTY, "message.seeking_immortals.talisman_table.unknown_recipe");
        }
        // No early material check: craftRecipe's preflight is the single gate, so the id route and the
        // station route report the same reason in the same order (skill, then realm, then materials).
        return craftRecipe(player, optional.get());
    }

    private static CraftResult craftRecipe(ServerPlayer player, Recipe recipe) {
        boolean creative = player.getAbilities().instabuild;
        boolean skillUnlocked = com.xunxian.seekingimmortals.skill.LifeSkillService.meetsLevel(player,
                com.xunxian.seekingimmortals.skill.SkillType.TALISMAN_CRAFTING, 0);
        String preflightFailure = preflightFailure(creative, skillUnlocked, creative || hasMaterials(player, recipe),
                creative || meetsRealm(player, recipe));
        if (!preflightFailure.isBlank()) {
            return new CraftResult(false, recipe, ItemStack.EMPTY, preflightFailure);
        }
        if (!creative && !consumeMaterials(player, recipe)) {
            return new CraftResult(false, recipe, ItemStack.EMPTY,
                    "message.seeking_immortals.talisman_table.missing_materials");
        }
        double rate = com.xunxian.seekingimmortals.skill.LifeSkillService.adjustedSuccessRate(
                player, com.xunxian.seekingimmortals.skill.SkillType.TALISMAN_CRAFTING, recipe.successRate());
        double efficiency = com.xunxian.seekingimmortals.catalog.CraftWorldSoftService
                .nearbyStationEfficiency(player, "talisman_table");
        rate = com.xunxian.seekingimmortals.skill.LifeSkillService.applyStationEfficiency(rate, efficiency);
        RandomSource random = player.getRandom();
        if (random.nextDouble() > rate) {
            // Failed craft keeps materials (same risk model as puppet bench).
            com.xunxian.seekingimmortals.skill.LifeSkillService.grantPractice(player,
                    com.xunxian.seekingimmortals.skill.SkillType.TALISMAN_CRAFTING, 8, 3);
            return new CraftResult(false, recipe, ItemStack.EMPTY, "message.seeking_immortals.talisman_table.failed");
        }
        ItemStack product = new ItemStack(recipe.product(), recipe.yield());
        // Prefer outbox over world drop when the player cannot fully accept the product.
        InventoryDeliveryService.giveOrEnqueue(player, product, "talisman_craft:" + recipe.id());
        com.xunxian.seekingimmortals.skill.LifeSkillService.grantPractice(player,
                com.xunxian.seekingimmortals.skill.SkillType.TALISMAN_CRAFTING, 22, 10);
        return new CraftResult(true, recipe, product, "message.seeking_immortals.talisman_table.activated");
    }

    private static List<Recipe> ensureRecipes() {
        List<Recipe> local = recipes;
        if (local != null) {
            return local;
        }
        synchronized (TalismanCraftService.class) {
            if (recipes == null) {
                recipes = buildRecipes();
            }
            return recipes;
        }
    }

    /** A blueprint below the authored realm gate stays visible but cannot be crafted. */
    private static boolean meetsRealm(ServerPlayer player, Recipe recipe) {
        if (recipe.realmMin() == null || player.getAbilities().instabuild) {
            return true;
        }
        return CultivationHelper.get(player)
                .map(PlayerCultivation::getRealm)
                .map(realm -> realm.ordinal() >= recipe.realmMin().ordinal())
                .orElse(false);
    }

    private static boolean hasMaterials(ServerPlayer player, Recipe recipe) {
        if (player.getAbilities().instabuild) {
            return true;
        }
        Optional<Map<Item, Integer>> requirements = materialRequirements(recipe);
        if (requirements.isEmpty()) {
            return false;
        }
        for (Map.Entry<Item, Integer> requirement : requirements.get().entrySet()) {
            if (count(player, requirement.getKey()) < requirement.getValue()) {
                return false;
            }
        }
        return true;
    }

    private static boolean consumeMaterials(ServerPlayer player, Recipe recipe) {
        // Snapshot-check first so a mid-loop shortage cannot leave a half-consumed inventory.
        if (!hasMaterials(player, recipe)) {
            return false;
        }
        Optional<Map<Item, Integer>> requirements = materialRequirements(recipe);
        if (requirements.isEmpty()) {
            return false;
        }
        for (Map.Entry<Item, Integer> requirement : requirements.get().entrySet()) {
            int remaining = requirement.getValue();
            for (int i = 0; i < player.getInventory().getContainerSize() && remaining > 0; i++) {
                ItemStack stack = player.getInventory().getItem(i);
                if (!stack.is(requirement.getKey())) {
                    continue;
                }
                int take = Math.min(remaining, stack.getCount());
                stack.shrink(take);
                remaining -= take;
            }
            if (remaining > 0) {
                // Should be unreachable after hasMaterials; refund everything and fail closed.
                refundMaterials(player, recipe);
                return false;
            }
        }
        return true;
    }

    private static void refundMaterials(ServerPlayer player, Recipe recipe) {
        if (player == null || recipe == null) {
            return;
        }
        for (Material material : recipe.materials()) {
            InventoryDeliveryService.giveOrEnqueue(player, new ItemStack(material.item(), material.count()), "talisman_craft_refund");
        }
    }

    /**
     * Exact transactional input set, including the authored per-recipe ink cost.
     * Shared with recipe viewers so display and server consumption cannot drift.
     */
    public static Optional<Map<Item, Integer>> materialRequirements(Recipe recipe) {
        if (recipe == null || recipe.materials().isEmpty() || recipe.ink() == null) {
            return Optional.empty();
        }
        Map<Item, Integer> requirements = new LinkedHashMap<>();
        for (Material material : recipe.materials()) {
            requirements.merge(material.item(), material.count(), Integer::sum);
        }
        requirements.merge(recipe.ink(), requiredInkCount(), Integer::sum);
        return Optional.of(requirements);
    }

    static String preflightFailure(boolean creative, boolean skillUnlocked, boolean materialsAvailable,
                                   boolean realmMet) {
        if (!skillUnlocked) {
            return "message.seeking_immortals.talisman_table.skill_locked";
        }
        if (!realmMet) {
            return "message.seeking_immortals.talisman_table.realm_too_low";
        }
        if (!creative && !materialsAvailable) {
            return "message.seeking_immortals.talisman_table.missing_materials";
        }
        return "";
    }

    public static int requiredInkCount() {
        return 1;
    }

    private static int count(ServerPlayer player, Item item) {
        int total = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(item)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    /**
     * Project the authored corpus onto registered items. An entry whose product, ink or any material
     * cannot be resolved is dropped rather than shipped as a half-valid recipe, and the three
     * authored {@code recipe_*} stubs have no materials array so they never become craftable.
     */
    private static List<Recipe> buildRecipes() {
        List<Recipe> list = new ArrayList<>();
        for (JsonObject entry : authoredEntries()) {
            String id = text(entry, "id");
            Item product = ItemCatalogService.resolveCatalogItem(text(entry, "talisman_id"));
            Item ink = ItemCatalogService.resolveCatalogItem(text(entry, "ink"));
            if (id.isBlank() || product == null || ink == null || !entry.has("materials")) {
                continue;
            }
            List<Material> materials = new ArrayList<>();
            boolean resolved = true;
            for (JsonElement element : entry.getAsJsonArray("materials")) {
                if (!element.isJsonObject()) {
                    resolved = false;
                    break;
                }
                JsonObject material = element.getAsJsonObject();
                Item item = ItemCatalogService.resolveCatalogItem(text(material, "id"));
                int count = material.has("count") ? material.get("count").getAsInt() : 1;
                if (item == null || count <= 0) {
                    resolved = false;
                    break;
                }
                materials.add(new Material(item, count));
            }
            if (!resolved || materials.isEmpty()) {
                SeekingImmortalsMod.LOGGER.warn("Talisman blueprint {} has unresolvable materials; omitted", id);
                continue;
            }
            String display = text(entry, "display");
            list.add(new Recipe(id, display.isBlank() ? id : display, List.copyOf(materials), product,
                    entry.has("base_success_rate") ? entry.get("base_success_rate").getAsDouble() : 0.5D,
                    Math.max(1, entry.has("yield") ? entry.get("yield").getAsInt() : 1),
                    ink, Realm.fromDesignId(text(entry, "realm_min"))));
        }
        return List.copyOf(list);
    }

    /** Registry-free corpus read, so blueprint counting works in pure unit environments. */
    private static List<JsonObject> authoredEntries() {
        List<JsonObject> local = authored;
        if (local != null) {
            return local;
        }
        synchronized (TalismanCraftService.class) {
            if (authored == null) {
                authored = loadAuthored();
            }
            return authored;
        }
    }

    private static List<JsonObject> loadAuthored() {
        try (InputStream stream = TalismanCraftService.class.getClassLoader()
                .getResourceAsStream(RECIPE_RESOURCE)) {
            if (stream == null) {
                SeekingImmortalsMod.LOGGER.error("Talisman recipe corpus missing: {}", RECIPE_RESOURCE);
                return List.of();
            }
            JsonObject root = JsonParser.parseReader(
                    new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            List<JsonObject> entries = new ArrayList<>();
            for (JsonElement element : root.getAsJsonArray("recipes")) {
                if (element.isJsonObject()) {
                    entries.add(element.getAsJsonObject());
                }
            }
            return List.copyOf(entries);
        } catch (Exception exception) {
            SeekingImmortalsMod.LOGGER.error("Failed loading talisman recipe corpus {}", RECIPE_RESOURCE, exception);
            return List.of();
        }
    }

    private static String text(JsonObject object, String field) {
        JsonElement value = object.get(field);
        return value == null || !value.isJsonPrimitive() ? "" : value.getAsString();
    }
}
