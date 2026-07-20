package com.xunxian.seekingimmortals.compat.jei;

import com.xunxian.seekingimmortals.artifact.ArtifactDataService;
import com.xunxian.seekingimmortals.artifact.ArtifactRefinementService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JeiRecipeCatalogTest {
    @Test
    void allAuthoredRefinementRecipesHaveCompleteSharedRuntimePlans() {
        List<ArtifactDataService.RefinementRecipe> recipes = List.copyOf(
                ArtifactDataService.builtin().refinementRecipes().values());

        assertEquals(73, recipes.size());
        for (ArtifactDataService.RefinementRecipe recipe : recipes) {
            ArtifactRefinementService.ResolvedPlan plan = ArtifactRefinementService.resolvePlan(recipe);
            assertTrue(plan.missingMappings().isEmpty(),
                    () -> recipe.id() + " has missing mappings " + plan.missingMappings());
            assertFalse(plan.outputItemId().isBlank(), () -> recipe.id() + " has no output item id");
            assertEquals(recipe.materials().size(), plan.materials().size(),
                    () -> recipe.id() + " lost a material while projecting for JEI");
            assertTrue(plan.materials().stream().allMatch(material ->
                            !material.itemId().isBlank() && material.count() > 0),
                    () -> recipe.id() + " has an invalid projected material");
        }
    }

    @Test
    void namespacedIdsPassThroughTheSharedPlanWithoutAliases() {
        ArtifactDataService.RefinementRecipe recipe = new ArtifactDataService.RefinementRecipe(
                "refine_test",
                "minecraft:diamond",
                "Test",
                "low",
                "QI_REFINING",
                1,
                0.75D,
                List.of(
                        new ArtifactDataService.MaterialRequirement("minecraft:iron_ingot", 3),
                        new ArtifactDataService.MaterialRequirement("minecraft:gold_ingot", 2)));

        ArtifactRefinementService.ResolvedPlan plan = ArtifactRefinementService.resolvePlan(recipe);

        assertEquals("minecraft:diamond", plan.outputItemId());
        assertEquals(List.of("minecraft:iron_ingot", "minecraft:gold_ingot"),
                plan.materials().stream().map(ArtifactRefinementService.ResolvedMaterial::itemId).toList());
        assertEquals(List.of(3, 2),
                plan.materials().stream().map(ArtifactRefinementService.ResolvedMaterial::count).toList());
        assertTrue(plan.missingMappings().isEmpty());
    }
}
