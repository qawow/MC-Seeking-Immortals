package com.xunxian.seekingimmortals.craft;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TalismanCraftServiceTest {
    @Test
    void skillGatePrecedesMaterialCommitAndEveryRecipeRequiresInk() {
        assertEquals(1, TalismanCraftService.requiredInkCount());
        assertEquals("message.seeking_immortals.talisman_table.skill_locked",
                TalismanCraftService.preflightFailure(false, false, false));
        assertEquals("message.seeking_immortals.talisman_table.missing_materials",
                TalismanCraftService.preflightFailure(false, true, false));
        assertEquals("", TalismanCraftService.preflightFailure(false, true, true));
    }
}
