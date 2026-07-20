package com.xunxian.seekingimmortals.compat.jei;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JeiIntegrationContractTest {
    private static final Path PLUGIN = Path.of("src/main/java/com/xunxian/seekingimmortals/compat/jei/SeekingImmortalsJeiPlugin.java");
    private static final Path CATALOG = Path.of("src/main/java/com/xunxian/seekingimmortals/compat/jei/JeiRecipeCatalog.java");

    @Test
    void pluginRegistersThreeCategoriesTheirStationsAndAlchemyClickArea() throws Exception {
        String source = Files.readString(PLUGIN);

        assertTrue(source.contains("registration.addRecipes(ALCHEMY, catalog.alchemyRecipes())"));
        assertTrue(source.contains("registration.addRecipes(REFINEMENT, catalog.refinementRecipes())"));
        assertTrue(source.contains("registration.addRecipes(TALISMAN, catalog.talismanRecipes())"));
        assertTrue(source.contains("ModBlocks.REFINEMENT_FORGE_G2.get()"));
        assertTrue(source.contains("ModBlocks.REFINEMENT_FORGE_G3.get()"));
        assertTrue(source.contains("ModBlocks.TALISMAN_TABLE.get(), TALISMAN"));
        assertTrue(source.contains("addRecipeClickArea(AlchemyFurnaceScreen.class"));
        assertFalse(source.contains("ARTIFACT_REPAIR_KIT"),
                "Refinement JEI output must never regress to the repair-kit placeholder");
    }

    @Test
    void catalogProjectsOnlyAuthoritativeRuntimeSources() throws Exception {
        String source = Files.readString(CATALOG);

        assertTrue(source.contains("AlchemyRecipeManager.jeiRecipes()"));
        assertTrue(source.contains("ArtifactDataService.builtin().refinementRecipes().values()"));
        assertTrue(source.contains("TalismanCraftService.recipes()"));
        assertTrue(source.contains("ArtifactRefinementService.resolvePlan(recipe)"));
        assertTrue(source.contains("ArtifactRefinementService::resolveItem"));
        assertTrue(source.contains("TalismanCraftService::materialRequirements"));
        assertTrue(source.contains("omitted.add(recipe.id())"),
                "Unresolvable recipes must fail closed instead of using placeholders");
    }
}
