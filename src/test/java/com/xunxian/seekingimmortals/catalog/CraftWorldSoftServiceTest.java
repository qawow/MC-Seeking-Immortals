package com.xunxian.seekingimmortals.catalog;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CraftWorldSoftServiceTest {
    @Test
    void loadsRefinementFormationTalismanPuppetIndexes() {
        assertTrue(CraftWorldSoftService.refinementRecipeCount() >= 20);
        assertTrue(CraftWorldSoftService.formationCount() >= 10);
        assertTrue(CraftWorldSoftService.talismanRecipeCount() >= 10);
        assertTrue(CraftWorldSoftService.puppetRecipeCount() >= 5);
        assertFalse(CraftWorldSoftService.sample("formation_catalog_index", 3).isEmpty());
    }
}
