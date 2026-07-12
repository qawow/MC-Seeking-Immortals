package com.xunxian.seekingimmortals.craft;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pure structural coverage without resolving Forge item registry objects.
 */
class CraftCatalogCoverageTest {
    @Test
    void talismanRecipeBlueprintCountIsTwentyFour() {
        assertEquals(24, TalismanCraftService.recipeBlueprintCount());
        assertTrue(TalismanCraftService.recipeBlueprintCount() > 0);
    }

    @Test
    void puppetRecipeBlueprintCountIsSeven() {
        assertEquals(7, PuppetCraftService.recipeBlueprintCount());
    }
}
