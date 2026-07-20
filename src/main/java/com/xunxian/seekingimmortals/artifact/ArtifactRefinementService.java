package com.xunxian.seekingimmortals.artifact;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.xunxian.seekingimmortals.SeekingImmortalsMod;
import com.xunxian.seekingimmortals.cultivation.CultivationHelper;
import com.xunxian.seekingimmortals.cultivation.PlayerCultivation;
import com.xunxian.seekingimmortals.cultivation.Realm;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ArtifactRefinementService {
    private static final String ID_MAP_PATH = "data/seeking_immortals/reference/text_material_id_map.json";

    private ArtifactRefinementService() {}

    /**
     * Prefer a recipe the player can currently afford (materials present),
     * otherwise fall back to the first catalog recipe id.
     */
    public static String selectRecipeId(ServerPlayer player) {
        return selectRecipeId(player, Integer.MAX_VALUE);
    }

    /**
     * Wave459: grade-aware selection — only recipes with forgeGrade &lt;= workstation grade.
     * Wave464: studied manuals may raise effective grade ceiling (never below workstation grade,
     * and grade-1 recipes always remain selectable).
     */
    public static String selectRecipeId(ServerPlayer player, int forgeGrade) {
        ArtifactDataService.Snapshot snapshot = ArtifactDataService.builtin();
        Map<String, String> map = itemIdMap();
        String fallback = "";
        int grade = effectiveForgeGrade(player, forgeGrade);
        for (ArtifactDataService.RefinementRecipe recipe : snapshot.refinementRecipes().values()) {
            if (recipe == null || recipe.id() == null || recipe.id().isBlank()) {
                continue;
            }
            if (Math.max(1, recipe.forgeGrade()) > grade) {
                continue;
            }
            if (fallback.isBlank()) {
                fallback = recipe.id();
            }
            ResolvedPlan plan = resolvePlan(recipe, map, SeekingImmortalsMod.MODID);
            if (!plan.missingMappings().isEmpty()) {
                continue;
            }
            List<ResolvedIngredient> ingredients = new ArrayList<>();
            boolean badItem = false;
            for (ResolvedMaterial material : plan.materials()) {
                Item item = resolveItem(material.itemId());
                if (item == null) {
                    badItem = true;
                    break;
                }
                ingredients.add(new ResolvedIngredient(material, item));
            }
            if (badItem) {
                continue;
            }
            if (missingRequirements(player, ingredients).isEmpty()) {
                return recipe.id();
            }
        }
        return fallback;
    }

    public static boolean refine(ServerPlayer player, String recipeId) {
        return refine(player, recipeId, 1);
    }

    /**
     * Wave464: workstation grade + studied manual forge ceiling.
     * Never drops below workstation grade; grade-1 always available via max(1, ...).
     * Reuses ManualCatalogService.maxUnlockedForgeGrade only.
     */
    public static int effectiveForgeGrade(ServerPlayer player, int forgeGrade) {
        int base = Math.max(1, forgeGrade);
        int manual = 1;
        try {
            manual = Math.max(1, com.xunxian.seekingimmortals.catalog.ManualCatalogService.maxUnlockedForgeGrade(player));
        } catch (Throwable ignored) {
            manual = 1;
        }
        int bonus = Math.max(0, manual - 1);
        return Math.max(base, Math.min(base + bonus, Math.max(base, manual)));
    }


    /**
     * Wave456: forge-grade aware refine.
     * @param forgeGrade workstation grade (1/2/3+). Recipe requiring higher grade is rejected.
     * Higher-grade forges grant +5% success per extra grade (capped +15%).
     */
    public static boolean refine(ServerPlayer player, String recipeId, int forgeGrade) {
        ArtifactDataService.Snapshot snapshot = ArtifactDataService.builtin();
        ArtifactDataService.RefinementRecipe recipe = snapshot.findRecipe(recipeId).orElse(null);
        if (recipe == null) {
            player.sendSystemMessage(Component.translatable(
                    "message.seeking_immortals.artifact.refine.unknown_recipe", recipeId));
            return false;
        }

        PlayerCultivation cultivation = CultivationHelper.get(player).orElse(null);
        if (cultivation == null) {
            player.sendSystemMessage(Component.translatable(
                    "message.seeking_immortals.artifact.refine.no_cultivation"));
            return false;
        }

        Realm requiredRealm = realmFromDesignId(recipe.realmMin());
        if (cultivation.getRealm().ordinal() < requiredRealm.ordinal()) {
            player.sendSystemMessage(Component.translatable(
                    "message.seeking_immortals.artifact.refine.realm_too_low",
                    recipe.display(), requiredRealm.getDisplayName()));
            return false;
        }

        // Wave464: workstation grade is base; studied manuals can raise effective ceiling.
        // Grade-1 recipes always pass when workstation is at least 1.
        int grade = effectiveForgeGrade(player, forgeGrade);
        int requiredGrade = Math.max(1, recipe.forgeGrade());
        if (grade < requiredGrade) {
            player.sendSystemMessage(Component.translatable(
                    "message.seeking_immortals.artifact.refine.forge_too_low",
                    recipe.display(), requiredGrade, grade));
            return false;
        }

        ResolvedPlan plan = resolvePlan(recipe, itemIdMap(), SeekingImmortalsMod.MODID);
        if (!plan.missingMappings().isEmpty()) {
            player.sendSystemMessage(Component.translatable(
                    "message.seeking_immortals.artifact.refine.missing_mapping",
                    recipe.id(), String.join(", ", plan.missingMappings())));
            return false;
        }

        Item outputItem = resolveItem(plan.outputItemId());
        if (outputItem == null) {
            player.sendSystemMessage(Component.translatable(
                    "message.seeking_immortals.artifact.refine.bad_item",
                    recipe.artifactId(), plan.outputItemId()));
            return false;
        }

        List<ResolvedIngredient> ingredients = new ArrayList<>();
        for (ResolvedMaterial material : plan.materials()) {
            Item item = resolveItem(material.itemId());
            if (item == null) {
                player.sendSystemMessage(Component.translatable(
                        "message.seeking_immortals.artifact.refine.bad_item",
                        material.sourceId(), material.itemId()));
                return false;
            }
            ingredients.add(new ResolvedIngredient(material, item));
        }

        List<MissingRequirement> missing = missingRequirements(player, ingredients);
        if (!missing.isEmpty()) {
            player.sendSystemMessage(Component.translatable(
                    "message.seeking_immortals.artifact.refine.not_enough_materials",
                    recipe.display(), missingRequirementSummary(missing)));
            return false;
        }

        if (!player.getAbilities().instabuild) {
            consumeMaterials(player, ingredients);
        }

        // Wave489: ARTIFACT_REFINING life skill bonus stacked on forge grade.
        double skillBonus = com.xunxian.seekingimmortals.skill.LifeSkillService.successBonus(
                player, com.xunxian.seekingimmortals.skill.SkillType.ARTIFACT_REFINING);
        double adjustedRate = Math.min(0.95D,
                adjustedSuccessRate(recipe.baseSuccessRate(), grade, requiredGrade) + skillBonus);
        double efficiency = com.xunxian.seekingimmortals.catalog.CraftWorldSoftService.nearbyStationEfficiency(
                player, "refinement_forge", "refinement_forge_g1", "refinement_forge_g3");
        adjustedRate = com.xunxian.seekingimmortals.skill.LifeSkillService
                .applyStationEfficiency(adjustedRate, efficiency);
        boolean success = succeeds(player.getRandom().nextDouble(), adjustedRate);
        if (success) {
            ItemStack output = new ItemStack(outputItem);
            // M15: 祭炼成功写入主人 UUID 与祭炼层数起点。
            output.getOrCreateTag().putUUID(ArtifactOwnershipService.OWNER_UUID_TAG, player.getUUID());
            output.getOrCreateTag().putString(ArtifactOwnershipService.OWNER_NAME_TAG,
                    player.getGameProfile().getName());
            ArtifactOwnershipService.addRefinementLayer(output, 1);
            Component outputName = output.getHoverName();
            com.xunxian.seekingimmortals.item.InventoryDeliveryService.giveOrEnqueue(
                    player, output, "artifact_refine:" + recipe.display());
            player.containerMenu.broadcastChanges();
            playFeedback(player, true);
            // Wave459: refining bound natal artifact grows it.
            if (recipe.artifactId() != null
                    && recipe.artifactId().equals(NatalBindingService.boundId(player))) {
                NatalBindingService.grow(player);
            }
            com.xunxian.seekingimmortals.skill.LifeSkillService.grantPractice(player,
                    com.xunxian.seekingimmortals.skill.SkillType.ARTIFACT_REFINING, 28, 12);
            player.sendSystemMessage(Component.translatable(
                    "message.seeking_immortals.artifact.refine.success",
                    recipe.display(), outputName, successPercent(adjustedRate)));
            return true;
        }

        player.containerMenu.broadcastChanges();
        playFeedback(player, false);
        com.xunxian.seekingimmortals.skill.LifeSkillService.grantPractice(player,
                com.xunxian.seekingimmortals.skill.SkillType.ARTIFACT_REFINING, 10, 4);
        List<ItemStack> failureLoot = grantFailureLoot(player, recipe, grade, requiredGrade);
        player.sendSystemMessage(Component.translatable(
                "message.seeking_immortals.artifact.refine.failure",
                recipe.display(), successPercent(adjustedRate), failureLootSummary(failureLoot)));
        return false;
    }

    public static double adjustedSuccessRate(double baseRate, int forgeGrade, int requiredGrade) {
        double base = Math.max(0.0D, Math.min(1.0D, baseRate));
        int bonusSteps = Math.max(0, Math.min(3, forgeGrade - Math.max(1, requiredGrade)));
        return Math.max(0.0D, Math.min(0.95D, base + bonusSteps * 0.05D));
    }

    public static ResolvedPlan resolvePlan(ArtifactDataService.RefinementRecipe recipe,
                                           Map<String, String> canonicalItemIds,
                                           String fallbackNamespace) {
        String outputItemId = itemIdFor(recipe.artifactId(), canonicalItemIds, fallbackNamespace, true);
        List<ResolvedMaterial> materials = new ArrayList<>();
        List<String> missingMappings = new ArrayList<>();
        for (ArtifactDataService.MaterialRequirement material : recipe.materials()) {
            String itemId = itemIdFor(material.id(), canonicalItemIds, fallbackNamespace, false);
            if (itemId.isBlank()) {
                missingMappings.add(material.id());
                continue;
            }
            materials.add(new ResolvedMaterial(material.id(), itemId, material.count()));
        }
        return new ResolvedPlan(outputItemId, materials, missingMappings);
    }

    public static boolean succeeds(double roll, double successRate) {
        return roll >= 0.0D && roll < Math.max(0.0D, Math.min(1.0D, successRate));
    }

    public static ResolvedFailureLoot rollFailureLoot(ArtifactDataService.RefinementRecipe recipe,
                                                      RandomSource random,
                                                      Map<String, String> canonicalItemIds,
                                                      String fallbackNamespace) {
        int totalWeight = totalFailureWeight(recipe);
        if (totalWeight <= 0) {
            return ResolvedFailureLoot.empty();
        }
        return selectFailureLoot(recipe, random.nextInt(totalWeight), random.nextInt(Integer.MAX_VALUE),
                canonicalItemIds, fallbackNamespace);
    }

    public static ResolvedFailureLoot selectFailureLoot(ArtifactDataService.RefinementRecipe recipe,
                                                       int weightRoll,
                                                       int countRoll,
                                                       Map<String, String> canonicalItemIds,
                                                       String fallbackNamespace) {
        List<ArtifactDataService.FailureLootEntry> entries = ArtifactDataService.builtin()
                .refinementFailureLoot()
                .entriesForTier(recipe.tier());
        int totalWeight = totalWeight(entries);
        if (totalWeight <= 0) {
            return ResolvedFailureLoot.empty();
        }

        int selectedRoll = Math.floorMod(weightRoll, totalWeight);
        ArtifactDataService.FailureLootEntry selected = entries.get(entries.size() - 1);
        int cursor = 0;
        for (ArtifactDataService.FailureLootEntry entry : entries) {
            cursor += entry.weight();
            if (selectedRoll < cursor) {
                selected = entry;
                break;
            }
        }

        int range = Math.max(1, selected.countMax() - selected.countMin() + 1);
        int count = selected.countMin() + Math.floorMod(countRoll, range);
        String itemId = itemIdFor(selected.id(), canonicalItemIds, fallbackNamespace, false);
        return new ResolvedFailureLoot(selected.id(), itemId, count, itemId.isBlank());
    }

    public static Realm realmFromDesignId(String id) {
        if (id == null || id.isBlank()) {
            return Realm.MORTAL;
        }
        return switch (id.toUpperCase(Locale.ROOT)) {
            case "QI_REFINING" -> Realm.QI_REFINING;
            case "FOUNDATION", "FOUNDATION_ESTABLISHMENT" -> Realm.FOUNDATION_ESTABLISHMENT;
            case "CORE_FORMATION" -> Realm.CORE_FORMATION;
            case "NASCENT_SOUL" -> Realm.NASCENT_SOUL;
            case "DEITY_TRANSFORMATION", "SOUL_TRANSFORMATION", "SPIRIT_SEVERANCE" -> Realm.SOUL_TRANSFORMATION;
            case "VOID_REFINEMENT" -> Realm.VOID_REFINEMENT;
            case "BODY_INTEGRATION", "UNITY" -> Realm.UNITY;
            case "GREAT_VEHICLE", "MAHAYANA" -> Realm.MAHAYANA;
            case "TRIBULATION", "TRIBULATION_LAND" -> Realm.TRIBULATION;
            case "TRUE_IMMORTAL" -> Realm.TRUE_IMMORTAL;
            default -> Realm.MORTAL;
        };
    }

    private static Map<String, String> itemIdMap() {
        return Holder.ITEM_ID_MAP;
    }

    private static Map<String, String> loadItemIdMap() {
        try (InputStream stream = ArtifactRefinementService.class.getClassLoader().getResourceAsStream(ID_MAP_PATH)) {
            if (stream == null) {
                return Map.of();
            }
            try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                JsonArray entries = root.getAsJsonArray("entries");
                Map<String, String> itemIds = new LinkedHashMap<>();
                for (JsonElement element : entries) {
                    if (!element.isJsonObject()) {
                        continue;
                    }
                    JsonObject entry = element.getAsJsonObject();
                    String canonicalType = getString(entry, "canonical_type");
                    String status = getString(entry, "status");
                    if (!"item".equals(canonicalType) || !status.startsWith("implemented")) {
                        continue;
                    }
                    String sourceId = getString(entry, "source_id");
                    String canonicalId = getString(entry, "canonical_id");
                    if (!sourceId.isBlank() && !canonicalId.isBlank()) {
                        itemIds.put(sourceId, canonicalId);
                    }
                }
                return Map.copyOf(itemIds);
            }
        } catch (IOException | IllegalStateException exception) {
            return Map.of();
        }
    }

    private static String itemIdFor(String sourceId, Map<String, String> canonicalItemIds,
                                    String fallbackNamespace, boolean allowFallback) {
        if (sourceId == null || sourceId.isBlank()) {
            return "";
        }
        if (sourceId.contains(":")) {
            return sourceId;
        }
        String mapped = canonicalItemIds.get(sourceId);
        if (mapped != null && !mapped.isBlank()) {
            return mapped;
        }
        return allowFallback ? fallbackNamespace + ":" + sourceId : "";
    }

    private static Item resolveItem(String itemId) {
        ResourceLocation location = ResourceLocation.tryParse(itemId == null ? "" : itemId);
        if (location == null || ForgeRegistries.ITEMS == null) {
            return null;
        }
        try {
            Item item = ForgeRegistries.ITEMS.getValue(location);
            return item == null || item == Items.AIR ? null : item;
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private static List<MissingRequirement> missingRequirements(ServerPlayer player,
                                                                List<ResolvedIngredient> ingredients) {
        List<MissingRequirement> missing = new ArrayList<>();
        for (ResolvedIngredient ingredient : ingredients) {
            int available = countItem(player, ingredient.item());
            if (available < ingredient.material().count()) {
                missing.add(new MissingRequirement(
                        ingredient.material().sourceId(),
                        ingredient.material().itemId(),
                        ingredient.material().count(),
                        available));
            }
        }
        return missing;
    }

    private static int countItem(ServerPlayer player, Item item) {
        int total = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(item)) {
                total += stack.getCount();
            }
        }
        for (ItemStack stack : player.getInventory().offhand) {
            if (stack.is(item)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    private static void consumeMaterials(ServerPlayer player, List<ResolvedIngredient> ingredients) {
        for (ResolvedIngredient ingredient : ingredients) {
            int remaining = ingredient.material().count();
            remaining = consumeFromStacks(player.getInventory().items, ingredient.item(), remaining);
            consumeFromStacks(player.getInventory().offhand, ingredient.item(), remaining);
        }
    }

    private static int consumeFromStacks(List<ItemStack> stacks, Item item, int remaining) {
        for (ItemStack stack : stacks) {
            if (remaining <= 0) {
                break;
            }
            if (!stack.is(item)) {
                continue;
            }
            int consumed = Math.min(remaining, stack.getCount());
            stack.shrink(consumed);
            remaining -= consumed;
        }
        return remaining;
    }

    private static String missingRequirementSummary(List<MissingRequirement> missing) {
        List<String> parts = new ArrayList<>();
        for (MissingRequirement requirement : missing) {
            parts.add(requirement.sourceId() + " " + requirement.available() + "/" + requirement.required());
        }
        return String.join(", ", parts);
    }

    private static List<ItemStack> grantFailureLoot(ServerPlayer player, ArtifactDataService.RefinementRecipe recipe) {
        return grantFailureLoot(player, recipe, 1, Math.max(1, recipe.forgeGrade()));
    }

    /**
     * Wave459: public salvage helper for workstation serializer failures + grade bonus rolls.
     */
    public static List<ItemStack> grantFailureLoot(ServerPlayer player, ArtifactDataService.RefinementRecipe recipe,
                                                   int forgeGrade, int requiredGrade) {
        int extraRolls = Math.max(0, Math.min(2, Math.max(1, forgeGrade) - Math.max(1, requiredGrade)));
        List<ItemStack> granted = new ArrayList<>();
        for (int i = 0; i < 1 + extraRolls; i++) {
            ResolvedFailureLoot loot = rollFailureLoot(recipe, player.getRandom(), itemIdMap(), SeekingImmortalsMod.MODID);
            if (loot.isEmpty()) {
                continue;
            }
            Item item = resolveItem(loot.itemId());
            if (item == null) {
                continue;
            }
            int count = loot.count() + (extraRolls > 0 && i == 0 ? 1 : 0);
            ItemStack stack = new ItemStack(item, Math.max(1, count));
            ItemStack summaryStack = stack.copy();
            com.xunxian.seekingimmortals.item.InventoryDeliveryService.giveOrEnqueue(
                    player, stack, "artifact_refine_fail_loot");
            granted.add(summaryStack);
        }
        if (!granted.isEmpty()) {
            player.containerMenu.broadcastChanges();
        }
        return granted;
    }

    /** Wave459: default salvage when only RecipeSerializer path fails (no catalog recipe). */
    public static List<ItemStack> grantDefaultFailureLoot(ServerPlayer player, String tierHint) {
        String tier = tierHint == null || tierHint.isBlank() ? "low" : tierHint;
        for (ArtifactDataService.RefinementRecipe recipe : ArtifactDataService.builtin().refinementRecipes().values()) {
            if (recipe != null && tier.equalsIgnoreCase(recipe.tier())) {
                return grantFailureLoot(player, recipe, 1, 1);
            }
        }
        // Fall back to any known recipe for loot table lookup.
        for (ArtifactDataService.RefinementRecipe recipe : ArtifactDataService.builtin().refinementRecipes().values()) {
            if (recipe != null) {
                return grantFailureLoot(player, recipe, 1, 1);
            }
        }
        return List.of();
    }

    private static Component failureLootSummary(List<ItemStack> loot) {
        if (loot.isEmpty()) {
            return Component.translatable("message.seeking_immortals.artifact.refine.failure.no_loot");
        }
        List<String> parts = new ArrayList<>();
        for (ItemStack stack : loot) {
            parts.add(stack.getCount() + "x " + stack.getHoverName().getString());
        }
        return Component.literal(String.join(", ", parts));
    }

    private static int totalFailureWeight(ArtifactDataService.RefinementRecipe recipe) {
        return totalWeight(ArtifactDataService.builtin().refinementFailureLoot().entriesForTier(recipe.tier()));
    }

    private static int totalWeight(List<ArtifactDataService.FailureLootEntry> entries) {
        int total = 0;
        for (ArtifactDataService.FailureLootEntry entry : entries) {
            total += Math.max(0, entry.weight());
        }
        return total;
    }

    private static String successPercent(double rate) {
        return String.format(Locale.ROOT, "%.0f%%", Math.max(0.0D, Math.min(1.0D, rate)) * 100.0D);
    }

    private static void playFeedback(ServerPlayer player, boolean success) {
        ServerLevel level = player.serverLevel();
        level.sendParticles(success ? ParticleTypes.END_ROD : ParticleTypes.SMOKE,
                player.getX(), player.getY() + 1.0D, player.getZ(),
                success ? 18 : 10, 0.45D, 0.45D, 0.45D, success ? 0.03D : 0.01D);
        level.playSound(null, player.blockPosition(),
                success ? SoundEvents.AMETHYST_BLOCK_CHIME : SoundEvents.FIRE_EXTINGUISH,
                SoundSource.PLAYERS, success ? 0.6F : 0.45F, success ? 1.35F : 0.85F);
    }

    private static String getString(JsonObject object, String key) {
        JsonElement element = object.get(key);
        return element != null && element.isJsonPrimitive() ? element.getAsString() : "";
    }

    private static final class Holder {
        private static final Map<String, String> ITEM_ID_MAP = loadItemIdMap();
    }

    public record ResolvedPlan(
            String outputItemId,
            List<ResolvedMaterial> materials,
            List<String> missingMappings
    ) {
        public ResolvedPlan {
            materials = List.copyOf(materials);
            missingMappings = List.copyOf(missingMappings);
        }
    }

    public record ResolvedMaterial(String sourceId, String itemId, int count) {}

    public record MissingRequirement(String sourceId, String itemId, int required, int available) {}

    public record ResolvedFailureLoot(String sourceId, String itemId, int count, boolean missingMapping) {
        public static ResolvedFailureLoot empty() {
            return new ResolvedFailureLoot("", "", 0, false);
        }

        public boolean isEmpty() {
            return count <= 0 || itemId.isBlank() || missingMapping;
        }
    }

    private record ResolvedIngredient(ResolvedMaterial material, Item item) {}
}
