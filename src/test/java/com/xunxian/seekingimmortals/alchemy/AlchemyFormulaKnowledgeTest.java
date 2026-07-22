package com.xunxian.seekingimmortals.alchemy;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AlchemyFormulaKnowledgeTest {
    @Test
    void normalizeTrimsAndLowercases() {
        assertEquals("cultivation_pill", AlchemyFormulaKnowledge.normalize(" Cultivation_Pill "));
        assertEquals("", AlchemyFormulaKnowledge.normalize(null));
        assertEquals("", AlchemyFormulaKnowledge.normalize("  "));
    }

    @Test
    void legacyRecipeIdsResolveToCanonicalRecipes() {
        assertEquals("spirit_recovery_pill",
                AlchemyFormulaKnowledge.canonicalRecipeId("qi_recovery_pill"));
        assertEquals("cultivate_speed_pill",
                AlchemyFormulaKnowledge.canonicalRecipeId("cultivation_pill"));
        assertEquals("jiangchen_pill",
                AlchemyFormulaKnowledge.canonicalRecipeId("breakthrough_pill"));
    }

    @Test
    void copyProgressionDataClonesStudiedTag() {
        CompoundTag source = new CompoundTag();
        CompoundTag studied = new CompoundTag();
        studied.putBoolean("cultivation_pill", true);
        studied.putBoolean("breakthrough_pill", true);
        source.put(AlchemyFormulaKnowledge.STUDIED_FORMULAS_TAG, studied);

        CompoundTag target = new CompoundTag();
        AlchemyFormulaKnowledge.copyProgressionData(source, target);
        assertTrue(target.contains(AlchemyFormulaKnowledge.STUDIED_FORMULAS_TAG));
        assertTrue(target.getCompound(AlchemyFormulaKnowledge.STUDIED_FORMULAS_TAG).getBoolean("cultivation_pill"));

        // mutate source after copy should not affect target
        source.getCompound(AlchemyFormulaKnowledge.STUDIED_FORMULAS_TAG).putBoolean("qi_recovery_pill", true);
        assertFalse(target.getCompound(AlchemyFormulaKnowledge.STUDIED_FORMULAS_TAG).getBoolean("qi_recovery_pill"));
    }

    @Test
    void furnaceAndItemSourcesReferenceKnowledge() throws Exception {
        String furnace = java.nio.file.Files.readString(java.nio.file.Path.of(
                "src/main/java/com/xunxian/seekingimmortals/block/entity/AlchemyFurnaceBlockEntity.java"));
        String formulaItem = java.nio.file.Files.readString(java.nio.file.Path.of(
                "src/main/java/com/xunxian/seekingimmortals/item/alchemy/AlchemyFormulaItem.java"));
        String consumable = java.nio.file.Files.readString(java.nio.file.Path.of(
                "src/main/java/com/xunxian/seekingimmortals/item/CatalogConsumableService.java"));
        String recipeManager = java.nio.file.Files.readString(java.nio.file.Path.of(
                "src/main/java/com/xunxian/seekingimmortals/alchemy/AlchemyRecipeManager.java"));
        assertTrue(furnace.contains("AlchemyFormulaKnowledge.hasStudied"));
        assertTrue(furnace.contains("canonicalRecipeId(tag.getString(\"RecipeId\"))"));
        assertTrue(furnace.contains("canonicalRecipeId(tag.getString(\"KnownFormulaId\"))"));
        assertTrue(recipeManager.contains("AlchemyFormulaKnowledge.canonicalRecipeId(raw)"));
        assertTrue(formulaItem.contains("AlchemyFormulaKnowledge.study"));
        assertTrue(consumable.contains("inscribe_formula"));
        assertTrue(consumable.contains("inscribeFormula"));
    }
}
