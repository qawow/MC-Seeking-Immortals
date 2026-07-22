package com.xunxian.seekingimmortals.craft;

import com.xunxian.seekingimmortals.catalog.ItemCatalogService;
import com.xunxian.seekingimmortals.catalog.SummonHonestMvpService;
import com.xunxian.seekingimmortals.item.InventoryDeliveryService;
import com.xunxian.seekingimmortals.registry.ModItems;
import com.xunxian.seekingimmortals.util.PlayerDisplayText;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Text-material puppet_craft_recipes runtime (7).
 * Wave44: successful craft also spawns a real SummonedServitorEntity via SummonHonestMvpService.
 */
public final class PuppetCraftService {
    private static volatile List<Recipe> recipes;

    private PuppetCraftService() {}

    public record Material(Item item, int count) {}

    public record Recipe(String id, String display, List<Material> materials, int strengthAmp, int resistAmp, int durationTicks, double successRate) {}

    public record CraftResult(boolean success, Recipe recipe, String messageKey) {}

    public static List<Recipe> recipes() {
        return ensureRecipes();
    }

    /** Registry-free count for unit tests / preflight. */
    public static int recipeBlueprintCount() {
        return 7;
    }

    public static Optional<Recipe> findCraftable(ServerPlayer player) {
        for (Recipe recipe : ensureRecipes()) {
            if (hasRequiredBlueprint(player, recipe) && hasMaterials(player, recipe)) {
                return Optional.of(recipe);
            }
        }
        return Optional.empty();
    }

    public static CraftResult craft(ServerPlayer player) {
        Optional<Recipe> optional = findCraftable(player);
        if (optional.isEmpty()) {
            return new CraftResult(false, null, "message.seeking_immortals.puppet_assembly_bench.missing_materials");
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
            if (!id.startsWith("assemble_") && !id.startsWith("upgrade_")
                    && (recipe.id().equals("assemble_" + id) || recipe.id().equals("upgrade_" + id))) {
                return Optional.of(recipe);
            }
        }
        return Optional.empty();
    }

    public static CraftResult craftById(ServerPlayer player, String recipeId) {
        Optional<Recipe> optional = find(recipeId);
        if (optional.isEmpty()) {
            return new CraftResult(false, null, "message.seeking_immortals.puppet_assembly_bench.unknown_recipe");
        }
        Recipe recipe = optional.get();
        return craftRecipe(player, recipe);
    }

    private static CraftResult craftRecipe(ServerPlayer player, Recipe recipe) {
        boolean creative = player.getAbilities().instabuild;
        boolean skillUnlocked = com.xunxian.seekingimmortals.skill.LifeSkillService.meetsLevel(player,
                com.xunxian.seekingimmortals.skill.SkillType.PUPPET_CONTROL, 0);
        String preflightFailure = preflightFailure(
                creative,
                skillUnlocked,
                creative || hasRequiredBlueprint(player, recipe),
                creative || hasMaterials(player, recipe));
        if (!preflightFailure.isBlank()) {
            return new CraftResult(false, recipe, preflightFailure);
        }
        if (!creative && !consumeMaterials(player, recipe)) {
            return new CraftResult(false, recipe,
                    "message.seeking_immortals.puppet_assembly_bench.missing_materials");
        }
        double rate = com.xunxian.seekingimmortals.skill.LifeSkillService.adjustedSuccessRate(
                player, com.xunxian.seekingimmortals.skill.SkillType.PUPPET_CONTROL, recipe.successRate());
        double efficiency = com.xunxian.seekingimmortals.catalog.CraftWorldSoftService
                .nearbyStationEfficiency(player, "puppet_assembly_bench");
        rate = com.xunxian.seekingimmortals.skill.LifeSkillService.applyStationEfficiency(rate, efficiency);
        RandomSource random = player.getRandom();
        if (random.nextDouble() > rate) {
            com.xunxian.seekingimmortals.skill.LifeSkillService.grantPractice(player,
                    com.xunxian.seekingimmortals.skill.SkillType.PUPPET_CONTROL, 8, 3);
            return new CraftResult(false, recipe, "message.seeking_immortals.puppet_assembly_bench.failed");
        }
        int skillLv = com.xunxian.seekingimmortals.skill.LifeSkillService.level(player,
                com.xunxian.seekingimmortals.skill.SkillType.PUPPET_CONTROL);
        double health = 28.0D + recipe.strengthAmp() * 10.0D + recipe.resistAmp() * 5.0D + skillLv * 1.5D;
        double damage = 5.0D + recipe.strengthAmp() * 1.8D + skillLv * 0.25D;
        int life = Math.max(20 * 600, recipe.durationTicks() * 4) + skillLv * 40;
        boolean spawned = SummonHonestMvpService.spawnConfigured(
                player, "puppet_" + recipe.id(), life, health, damage,
                com.xunxian.seekingimmortals.entity.SummonedServitorEntity.Archetype.PUPPET, true);
        if (!spawned) {
            if (!creative) {
                refundMaterials(player, recipe);
            }
            return new CraftResult(false, recipe,
                    "message.seeking_immortals.puppet_assembly_bench.spawn_failed");
        }
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST,
                Math.min(200, recipe.durationTicks()), recipe.strengthAmp()));
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE,
                Math.min(200, recipe.durationTicks()), recipe.resistAmp()));
        com.xunxian.seekingimmortals.skill.LifeSkillService.grantPractice(player,
                com.xunxian.seekingimmortals.skill.SkillType.PUPPET_CONTROL, 24, 12);
        var growth = com.xunxian.seekingimmortals.beast.PuppetGrowthService
                .recordAssembly(player, "puppet_" + recipe.id());
        if (growth.update().levelsGained() > 0) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.puppet.growth",
                    PlayerDisplayText.safeLiteral(recipe.display(), "text.seeking_immortals.unknown_puppet"),
                    growth.after().level(), growth.after().experience()), false);
        }
        if (growth.update().evolutionBlocked()) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.puppet.core_forge_required"), false);
        }
        player.displayClientMessage(Component.translatable("message.seeking_immortals.puppet.repair_hint"), false);
        return new CraftResult(true, recipe, "message.seeking_immortals.puppet_assembly_bench.activated");
    }

    private static List<Recipe> ensureRecipes() {
        List<Recipe> local = recipes;
        if (local != null) {
            return local;
        }
        synchronized (PuppetCraftService.class) {
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
        for (Material material : recipe.materials()) {
            if (count(player, material.item()) < material.count()) {
                return false;
            }
        }
        return true;
    }

    private static boolean hasRequiredBlueprint(ServerPlayer player, Recipe recipe) {
        Optional<String> blueprintId = blueprintIdForRecipe(recipe.id());
        if (blueprintId.isEmpty()) {
            return true;
        }
        Item blueprint = ItemCatalogService.resolveCatalogItem(blueprintId.get());
        return blueprint != null && count(player, blueprint) > 0;
    }

    private static boolean consumeMaterials(ServerPlayer player, Recipe recipe) {
        if (!hasMaterials(player, recipe)) {
            return false;
        }
        for (Material material : recipe.materials()) {
            int remaining = material.count();
            for (int i = 0; i < player.getInventory().getContainerSize() && remaining > 0; i++) {
                ItemStack stack = player.getInventory().getItem(i);
                if (!stack.is(material.item())) {
                    continue;
                }
                int take = Math.min(remaining, stack.getCount());
                stack.shrink(take);
                remaining -= take;
            }
            if (remaining > 0) {
                return false;
            }
        }
        return true;
    }

    private static void refundMaterials(ServerPlayer player, Recipe recipe) {
        for (Material material : recipe.materials()) {
            InventoryDeliveryService.giveOrEnqueue(player, new ItemStack(material.item(), material.count()), "puppet_craft_refund");
        }
    }

    public static Optional<String> blueprintIdForRecipe(String recipeId) {
        String id = recipeId == null ? "" : recipeId.trim().toLowerCase(java.util.Locale.ROOT);
        return switch (id) {
            case "assemble_basic_wood" -> Optional.of("basic_wood_puppet_blueprint");
            case "assemble_giant_ape" -> Optional.of("giant_ape_puppet_blueprint");
            default -> Optional.empty();
        };
    }

    static String preflightFailure(boolean creative, boolean skillUnlocked,
                                   boolean blueprintAvailable, boolean materialsAvailable) {
        if (!skillUnlocked) {
            return "message.seeking_immortals.puppet_assembly_bench.skill_locked";
        }
        if (!creative && !blueprintAvailable) {
            return "message.seeking_immortals.puppet_assembly_bench.missing_blueprint";
        }
        if (!creative && !materialsAvailable) {
            return "message.seeking_immortals.puppet_assembly_bench.missing_materials";
        }
        return "";
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
        list.add(recipe("assemble_basic_wood", "木人傀儡", mats(m(ModItems.IRONWOOD, 2), m(ModItems.PUPPET_CORE_BLANK, 1), m(ModItems.SPIRIT_STONE_SHARD, 3)), 0, 0, 400, 0.85D));
        list.add(recipe("assemble_giant_ape", "巨猿傀儡", mats(m(ModItems.IRONWOOD, 8), m(ModItems.BEAST_CORE, 1), m(ModItems.PUPPET_CORE_BLANK, 1)), 1, 0, 500, 0.70D));
        list.add(recipe("assemble_giant_turtle", "巨龟傀儡", mats(m(ModItems.PUPPET_CORE_BLANK, 1), m(ModItems.SPIRIT_BEAST_BONE, 2), m(ModItems.IRONWOOD, 4)), 0, 1, 500, 0.70D));
        list.add(recipe("assemble_stone_spirit", "石灵傀儡", mats(m(ModItems.PUPPET_CORE_BLANK, 1), m(ModItems.SOUL_FRAGMENT, 1), m(ModItems.IRONWOOD, 4)), 1, 1, 520, 0.65D));
        list.add(recipe("assemble_stone_guard", "石卫傀儡", mats(m(ModItems.SPIRIT_IRON, 6), m(ModItems.PUPPET_CORE_BLANK, 1), m(ModItems.METAL_SPIRIT_STONE, 2)), 1, 1, 560, 0.60D));
        list.add(recipe("assemble_fire_spear", "火矛傀儡", mats(m(ModItems.PHOENIX_FEATHER, 3), m(ModItems.PUPPET_CORE_BLANK, 1), m(ModItems.IRONWOOD, 4)), 2, 0, 540, 0.55D));
        list.add(recipe("upgrade_hunyuan_core", "混元傀儡核", mats(m(ModItems.PUPPET_CORE_BLANK, 1), m(ModItems.PRIMORDIAL_ESSENCE, 1), m(ModItems.VOID_CRYSTAL, 1)), 2, 1, 700, 0.35D));
        return List.copyOf(list);
    }

    private static Recipe recipe(String id, String display, List<Material> materials, int strengthAmp, int resistAmp, int duration, double rate) {
        return new Recipe(id, display, materials, strengthAmp, resistAmp, duration, rate);
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
