package com.xunxian.seekingimmortals.craft;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TalismanCraftServiceTest {
    @Test
    void skillGatePrecedesRealmGateThenMaterialCommit() {
        assertEquals(1, TalismanCraftService.requiredInkCount());
        // Skill lock reports first even when realm and materials are also missing.
        assertEquals("message.seeking_immortals.talisman_table.skill_locked",
                TalismanCraftService.preflightFailure(false, false, false, false));
        // Realm gate reports before the material commit, so a low-realm player is never charged.
        assertEquals("message.seeking_immortals.talisman_table.realm_too_low",
                TalismanCraftService.preflightFailure(false, true, true, false));
        assertEquals("message.seeking_immortals.talisman_table.missing_materials",
                TalismanCraftService.preflightFailure(false, true, false, true));
        assertEquals("", TalismanCraftService.preflightFailure(false, true, true, true));
    }
}
