package com.xunxian.seekingimmortals.compat.jei;

import com.xunxian.seekingimmortals.alchemy.AlchemyRecipe;
import com.xunxian.seekingimmortals.alchemy.AlchemyRecipeManager;
import com.xunxian.seekingimmortals.artifact.ArtifactDataService;
import com.xunxian.seekingimmortals.artifact.ArtifactRefinementService;
import com.xunxian.seekingimmortals.craft.TalismanCraftService;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * JEI-neutral projection of the three authoritative crafting recipe sources.
 * Invalid author entries fail closed instead of becoming placeholder recipes.
 */
public final class JeiRecipeCatalog {
    private JeiRecipeCatalog() {}

    public static Snapshot snapshot() {
        BuildResult<RefinementDisplayRecipe> refinement = buildRefinementRecipes(
                ArtifactDataService.builtin().refinementRecipes().values(),
                ArtifactRefinementService::resolveItem);
        BuildResult<TalismanDisplayRecipe> talismans = buildTalismanRecipes(
                TalismanCraftService.recipes(),
                TalismanCraftService::materialRequirements);
        return new Snapshot(
                AlchemyRecipeManager.jeiRecipes(),
                refinement.recipes(),
                talismans.recipes(),
                refinement.omittedIds(),
                talismans.omittedIds());
    }

    static BuildResult<RefinementDisplayRecipe> buildRefinementRecipes(
            Collection<ArtifactDataService.RefinementRecipe> recipes,
            Function<String, Item> itemResolver) {
        List<RefinementDisplayRecipe> displays = new ArrayList<>();
        List<String> omitted = new ArrayList<>();
        if (recipes == null || itemResolver == null) {
            return new BuildResult<>(displays, List.of("<catalog>"));
        }
        for (ArtifactDataService.RefinementRecipe recipe : recipes) {
            if (recipe == null || recipe.id() == null || recipe.id().isBlank()) {
                omitted.add("<blank>");
                continue;
            }
            ArtifactRefinementService.ResolvedPlan plan = ArtifactRefinementService.resolvePlan(recipe);
            if (!plan.missingMappings().isEmpty()) {
                omitted.add(recipe.id());
                continue;
            }
            Item output = itemResolver.apply(plan.outputItemId());
            if (output == null) {
                omitted.add(recipe.id());
                continue;
            }
            List<ItemStack> inputs = new ArrayList<>();
            boolean unresolved = false;
            for (ArtifactRefinementService.ResolvedMaterial material : plan.materials()) {
                Item item = itemResolver.apply(material.itemId());
                if (item == null || material.count() <= 0) {
                    unresolved = true;
                    break;
                }
                inputs.add(new ItemStack(item, material.count()));
            }
            if (unresolved || inputs.isEmpty()) {
                omitted.add(recipe.id());
                continue;
            }
            displays.add(new RefinementDisplayRecipe(recipe, inputs, new ItemStack(output)));
        }
        return new BuildResult<>(displays, omitted);
    }

    static BuildResult<TalismanDisplayRecipe> buildTalismanRecipes(
            Collection<TalismanCraftService.Recipe> recipes,
            Function<TalismanCraftService.Recipe, Optional<Map<Item, Integer>>> requirementResolver) {
        List<TalismanDisplayRecipe> displays = new ArrayList<>();
        List<String> omitted = new ArrayList<>();
        if (recipes == null || requirementResolver == null) {
            return new BuildResult<>(displays, List.of("<catalog>"));
        }
        for (TalismanCraftService.Recipe recipe : recipes) {
            if (recipe == null || recipe.id() == null || recipe.id().isBlank() || recipe.product() == null) {
                omitted.add("<blank>");
                continue;
            }
            Optional<Map<Item, Integer>> requirements = requirementResolver.apply(recipe);
            if (requirements.isEmpty() || requirements.get().isEmpty()
                    || requirements.get().entrySet().stream().anyMatch(entry -> entry.getKey() == null || entry.getValue() <= 0)) {
                omitted.add(recipe.id());
                continue;
            }
            List<ItemStack> inputs = requirements.get().entrySet().stream()
                    .map(entry -> new ItemStack(entry.getKey(), entry.getValue()))
                    .toList();
            displays.add(new TalismanDisplayRecipe(recipe, inputs, new ItemStack(recipe.product())));
        }
        return new BuildResult<>(displays, omitted);
    }

    public record Snapshot(
            List<AlchemyRecipe> alchemyRecipes,
            List<RefinementDisplayRecipe> refinementRecipes,
            List<TalismanDisplayRecipe> talismanRecipes,
            List<String> omittedRefinementIds,
            List<String> omittedTalismanIds
    ) {
        public Snapshot {
            alchemyRecipes = List.copyOf(alchemyRecipes);
            refinementRecipes = List.copyOf(refinementRecipes);
            talismanRecipes = List.copyOf(talismanRecipes);
            omittedRefinementIds = List.copyOf(omittedRefinementIds);
            omittedTalismanIds = List.copyOf(omittedTalismanIds);
        }
    }

    public record RefinementDisplayRecipe(
            ArtifactDataService.RefinementRecipe source,
            List<ItemStack> inputs,
            ItemStack output
    ) {
        public RefinementDisplayRecipe {
            inputs = List.copyOf(inputs);
        }
    }

    public record TalismanDisplayRecipe(
            TalismanCraftService.Recipe source,
            List<ItemStack> inputs,
            ItemStack output
    ) {
        public TalismanDisplayRecipe {
            inputs = List.copyOf(inputs);
        }
    }

    record BuildResult<T>(List<T> recipes, List<String> omittedIds) {
        BuildResult {
            recipes = List.copyOf(recipes);
            omittedIds = List.copyOf(omittedIds);
        }
    }
}
