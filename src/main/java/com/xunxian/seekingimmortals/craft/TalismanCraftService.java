package com.xunxian.seekingimmortals.craft;

import com.xunxian.seekingimmortals.catalog.ItemCatalogService;
import com.xunxian.seekingimmortals.item.InventoryDeliveryService;
import com.xunxian.seekingimmortals.registry.ModItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Text-material talisman_recipes runtime (24). Materials remap to existing carriers.
 * Recipes are built lazily so unit tests and class-loading do not touch the item registry early.
 */
public final class TalismanCraftService {
    private static volatile List<Recipe> recipes;

    private TalismanCraftService() {}

    public record Material(Item item, int count) {}

    public record Recipe(String id, String display, List<Material> materials, Item product, double successRate) {}

    public record CraftResult(boolean success, Recipe recipe, ItemStack product, String messageKey) {}

    public static List<Recipe> recipes() {
        return ensureRecipes();
    }

    /** Registry-free count for unit tests / preflight. */
    public static int recipeBlueprintCount() {
        return 24;
    }

    public static Optional<Recipe> findCraftable(ServerPlayer player) {
        for (Recipe recipe : ensureRecipes()) {
            if (hasMaterials(player, recipe)) {
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
        String id = recipeId == null ? "" : recipeId.trim().toLowerCase(java.util.Locale.ROOT);
        if (id.isBlank()) {
            return Optional.empty();
        }
        for (Recipe recipe : ensureRecipes()) {
            if (recipe.id().equals(id) || recipe.id().equalsIgnoreCase(recipeId)) {
                return Optional.of(recipe);
            }
            // Allow bare names without craft_ prefix.
            if (id.startsWith("craft_") && recipe.id().equals(id)) {
                return Optional.of(recipe);
            }
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
        Recipe recipe = optional.get();
        if (!player.getAbilities().instabuild && !hasMaterials(player, recipe)) {
            return new CraftResult(false, recipe, ItemStack.EMPTY, "message.seeking_immortals.talisman_table.missing_materials");
        }
        return craftRecipe(player, recipe);
    }

    private static CraftResult craftRecipe(ServerPlayer player, Recipe recipe) {
        boolean creative = player.getAbilities().instabuild;
        boolean skillUnlocked = com.xunxian.seekingimmortals.skill.LifeSkillService.meetsLevel(player,
                com.xunxian.seekingimmortals.skill.SkillType.TALISMAN_CRAFTING, 0);
        String preflightFailure = preflightFailure(creative, skillUnlocked, creative || hasMaterials(player, recipe));
        if (!preflightFailure.isBlank()) {
            return new CraftResult(false, recipe, ItemStack.EMPTY, preflightFailure);
        }
        if (!creative && !consumeMaterials(player, recipe)) {
            return new CraftResult(false, recipe, ItemStack.EMPTY,
                    "message.seeking_immortals.talisman_table.missing_materials");
        }
        double rate = com.xunxian.seekingimmortals.skill.LifeSkillService.adjustedSuccessRate(
                player, com.xunxian.seekingimmortals.skill.SkillType.TALISMAN_CRAFTING, recipe.successRate());
        RandomSource random = player.getRandom();
        if (random.nextDouble() > rate) {
            // Failed craft keeps materials (same risk model as puppet bench).
            com.xunxian.seekingimmortals.skill.LifeSkillService.grantPractice(player,
                    com.xunxian.seekingimmortals.skill.SkillType.TALISMAN_CRAFTING, 8, 3);
            return new CraftResult(false, recipe, ItemStack.EMPTY, "message.seeking_immortals.talisman_table.failed");
        }
        ItemStack product = new ItemStack(recipe.product());
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

    private static Optional<Map<Item, Integer>> materialRequirements(Recipe recipe) {
        Map<Item, Integer> requirements = new LinkedHashMap<>();
        for (Material material : recipe.materials()) {
            requirements.merge(material.item(), material.count(), Integer::sum);
        }
        Item ink = ItemCatalogService.resolveCatalogItem("talisman_ink_bottle");
        if (ink == null) {
            return Optional.empty();
        }
        requirements.merge(ink, requiredInkCount(), Integer::sum);
        return Optional.of(requirements);
    }

    static String preflightFailure(boolean creative, boolean skillUnlocked, boolean materialsAvailable) {
        if (!skillUnlocked) {
            return "message.seeking_immortals.talisman_table.skill_locked";
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

    private static List<Recipe> buildRecipes() {
        List<Recipe> list = new ArrayList<>();
        list.add(recipe("craft_fire_burst", "爆炎符", mats(m(ModItems.TALISMAN_PAPER_MORTAL, 1), m(ModItems.TRUE_DRAGON_BLOOD, 1), m(ModItems.PHOENIX_FEATHER, 1)), ModItems.FIRE_TALISMAN, 0.70D));
        list.add(recipe("craft_armor_ward", "护体符", mats(m(ModItems.TALISMAN_PAPER_MORTAL, 1), m(ModItems.SPIRIT_IRON, 1), m(ModItems.SPIRIT_STONE_SHARD, 1)), ModItems.ARMOR_TALISMAN, 0.70D));
        list.add(recipe("craft_speed_wind", "疾风符", mats(m(ModItems.TALISMAN_PAPER_MORTAL, 1), m(ModItems.PHOENIX_FEATHER, 1), m(ModItems.SPIRIT_STONE_SHARD, 1)), ModItems.SPEED_TALISMAN, 0.70D));
        list.add(recipe("craft_mirage_heart", "幻心符", mats(m(ModItems.TALISMAN_PAPER_MORTAL, 2), m(ModItems.TIME_SAND, 1), m(ModItems.TRUE_DRAGON_BLOOD, 1)), ModItems.SPEED_TALISMAN, 0.55D));
        list.add(recipe("craft_soul_scatter", "散魂符", mats(m(ModItems.TALISMAN_PAPER_MORTAL, 2), m(ModItems.SOUL_FRAGMENT, 2)), ModItems.YIN_BODY_PROTECTION_CHARM, 0.45D));
        list.add(recipe("craft_demon_suppress", "镇魔符", mats(m(ModItems.TALISMAN_PAPER_MORTAL, 2), m(ModItems.DEMON_SUPPRESS_TALISMAN_BLANK, 1), m(ModItems.SPIRIT_STONE_SHARD, 2)), ModItems.ARMOR_TALISMAN, 0.50D));
        list.add(recipe("craft_yin_ward", "阴护符", mats(m(ModItems.TALISMAN_PAPER_MORTAL, 1), m(ModItems.YIN_STONE, 2), m(ModItems.SOUL_FRAGMENT, 1)), ModItems.YIN_BODY_PROTECTION_CHARM, 0.60D));
        list.add(recipe("craft_pressure_resist", "抗压符", mats(m(ModItems.TALISMAN_PAPER_MORTAL, 1), m(ModItems.SPIRIT_IRON, 2)), ModItems.PRESSURE_RESIST_CHARM, 0.65D));
        list.add(recipe("craft_spirit_burst", "灵爆符", mats(m(ModItems.TALISMAN_PAPER_MORTAL, 1), m(ModItems.SPIRIT_STONE_SHARD, 3)), ModItems.FIRE_TALISMAN, 0.75D));
        list.add(recipe("craft_cold_seal", "寒封符", mats(m(ModItems.TALISMAN_PAPER_MORTAL, 1), m(ModItems.COLD_JADE, 1), m(ModItems.SPIRIT_STONE_SHARD, 1)), ModItems.ARMOR_TALISMAN, 0.55D));
        list.add(recipe("craft_void_step", "虚步符", mats(m(ModItems.TALISMAN_PAPER_MORTAL, 2), m(ModItems.VOID_CRYSTAL, 1)), ModItems.SPEED_TALISMAN, 0.40D));
        list.add(recipe("craft_beast_bind", "缚兽符", mats(m(ModItems.TALISMAN_PAPER_MORTAL, 1), m(ModItems.BEAST_CORE, 1), m(ModItems.SPIRIT_STONE_SHARD, 1)), ModItems.ARMOR_TALISMAN, 0.55D));
        list.add(recipe("craft_heal_light", "回春符", mats(m(ModItems.TALISMAN_PAPER_MORTAL, 1), m(ModItems.SPIRIT_GRASS, 2)), ModItems.ARMOR_TALISMAN, 0.70D));
        list.add(recipe("craft_thunder_mark", "雷纹符", mats(m(ModItems.TALISMAN_PAPER_MORTAL, 1), m(ModItems.STAR_METEORITE, 1), m(ModItems.SPIRIT_STONE_SHARD, 1)), ModItems.FIRE_TALISMAN, 0.50D));
        list.add(recipe("craft_blood_lock", "血锁符", mats(m(ModItems.TALISMAN_PAPER_MORTAL, 2), m(ModItems.TRUE_DRAGON_BLOOD, 2)), ModItems.FIRE_TALISMAN, 0.45D));
        list.add(recipe("craft_soul_calm", "安魂符", mats(m(ModItems.TALISMAN_PAPER_MORTAL, 1), m(ModItems.SOUL_FRAGMENT, 1), m(ModItems.COLD_JADE, 1)), ModItems.YIN_BODY_PROTECTION_CHARM, 0.60D));
        list.add(recipe("craft_earth_wall", "土壁符", mats(m(ModItems.TALISMAN_PAPER_MORTAL, 1), m(ModItems.SPIRIT_IRON, 1), m(ModItems.IRONWOOD, 1)), ModItems.ARMOR_TALISMAN, 0.65D));
        list.add(recipe("craft_wind_blade", "风刃符", mats(m(ModItems.TALISMAN_PAPER_MORTAL, 1), m(ModItems.PHOENIX_FEATHER, 2)), ModItems.FIRE_TALISMAN, 0.60D));
        list.add(recipe("craft_metal_needle", "金针符", mats(m(ModItems.TALISMAN_PAPER_MORTAL, 1), m(ModItems.SPIRIT_IRON, 2)), ModItems.FIRE_TALISMAN, 0.65D));
        list.add(recipe("craft_wood_vine", "藤缚符", mats(m(ModItems.TALISMAN_PAPER_MORTAL, 1), m(ModItems.IRONWOOD, 2), m(ModItems.SPIRIT_GRASS, 1)), ModItems.ARMOR_TALISMAN, 0.60D));
        list.add(recipe("craft_water_mist", "水雾符", mats(m(ModItems.TALISMAN_PAPER_MORTAL, 1), m(ModItems.COLD_JADE, 1), m(ModItems.SPIRIT_GRASS, 1)), ModItems.SPEED_TALISMAN, 0.60D));
        list.add(recipe("craft_star_guide", "星引符", mats(m(ModItems.TALISMAN_PAPER_MORTAL, 2), m(ModItems.STAR_METEORITE, 1), m(ModItems.IMMORTAL_JADE, 1)), ModItems.SPEED_TALISMAN, 0.40D));
        list.add(recipe("craft_chaos_ward", "混沌护符", mats(m(ModItems.TALISMAN_PAPER_MORTAL, 2), m(ModItems.CHAOS_GOLD, 1), m(ModItems.VOID_CRYSTAL, 1)), ModItems.ARMOR_TALISMAN, 0.35D));
        list.add(recipe("craft_primordial_seal", "混元印符", mats(m(ModItems.TALISMAN_PAPER_MORTAL, 3), m(ModItems.PRIMORDIAL_ESSENCE, 1)), ModItems.ARMOR_TALISMAN, 0.30D));
        return List.copyOf(list);
    }

    private static Recipe recipe(String id, String display, List<Material> materials, RegistryObject<? extends Item> product, double successRate) {
        return new Recipe(id, display, materials, product.get(), successRate);
    }

    @SafeVarargs
    private static List<Material> mats(Supplier<Material>... parts) {
        List<Material> list = new ArrayList<>(parts.length);
        for (Supplier<Material> part : parts) {
            list.add(part.get());
        }
        return List.copyOf(list);
    }

    private static Supplier<Material> m(RegistryObject<? extends Item> item, int count) {
        return () -> new Material(item.get(), count);
    }
}
