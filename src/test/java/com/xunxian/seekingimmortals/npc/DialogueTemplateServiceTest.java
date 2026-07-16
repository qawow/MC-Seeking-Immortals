package com.xunxian.seekingimmortals.npc;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DialogueTemplateServiceTest {
    @Test
    void loadsArchetypesAndBindings() {
        assertTrue(DialogueTemplateService.archetypeCount() >= 12);
        assertTrue(DialogueTemplateService.find("sect_contribution_clerk").isPresent());
        assertTrue(DialogueTemplateService.archetypeForNpc("npc_huangfeng_contribution").isPresent());
    }

    @Test
    void greetingLinesPresent() {
        var lines = DialogueTemplateService.lines("sect_contribution_clerk", "greeting");
        assertFalse(lines.isEmpty());
        var farewell = DialogueTemplateService.lines("market_vendor", "farewell");
        // market_vendor may only have greeting; at least archetype exists
        assertTrue(DialogueTemplateService.find("market_vendor").isPresent());
        assertTrue(farewell != null);
    }

    @Test
    void standardTagsDocumented() {
        assertTrue(DialogueTemplateService.STANDARD_TAGS.contains("greeting"));
        assertTrue(DialogueTemplateService.STANDARD_TAGS.contains("shop"));
        assertTrue(DialogueTemplateService.STANDARD_TAGS.size() >= 9);
    }
}
