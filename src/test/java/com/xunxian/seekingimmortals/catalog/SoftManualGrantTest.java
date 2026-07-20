package com.xunxian.seekingimmortals.catalog;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 0.2.111: soft bulk manuals must grant methods / forge grades / formulas.
 */
class SoftManualGrantTest {

    @Test
    void softMethodGrantTableCoversHighValueDeadManuals() throws Exception {
        String source = Files.readString(Path.of(
                "src", "main", "java", "com", "xunxian", "seekingimmortals",
                "catalog", "ManualCatalogService.java"));
        for (String id : java.util.List.of(
                "alchemy_manual_low",
                "refinement_manual_high",
                "manual_ancient_puppet_method",
                "beast_taming_manual",
                "talisman_recipe",
                "formation_scroll_mid",
                "artifact_identify_scroll",
                "ghost_cultivation_manual")) {
            assertTrue(source.contains("\"" + id + "\""), "missing soft grant for " + id);
        }
        assertTrue(source.contains("softMethodGrants"));
        assertTrue(source.contains("softForgeGrade"));
        assertTrue(source.contains("softAlchemyRecipeId"));
        assertTrue(source.contains("AlchemyFormulaKnowledge.study"));
        assertTrue(source.contains("SOFT_FORGE_GRADES_TAG"));
    }

    @Test
    void manualsCatalogHighValueRowsHaveUnlocks() throws Exception {
        String json = Files.readString(Path.of(
                "src", "main", "resources", "data", "seeking_immortals",
                "text_material", "manuals_catalog.json"));
        assertTrue(json.contains("\"id\": \"refinement_manual_low\""));
        assertTrue(json.contains("artifact_refining_basic"));
        assertTrue(json.contains("ghost_nether_art"));
        assertTrue(json.contains("huangfeng_alchemy_scripture"));
        assertFalse(json.contains("\"id\": \"refinement_manual_low\",\n    \"display\": \"炼器入门篇\",\n    \"type\": \"refinement\",\n    \"unlocks_forge_grade\": 1,\n    \"source\"")
                && !json.contains("artifact_refining_basic"));
    }
}
