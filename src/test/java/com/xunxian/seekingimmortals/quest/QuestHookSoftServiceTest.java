package com.xunxian.seekingimmortals.quest;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestHookSoftServiceTest {
    @Test
    void loadsQuestHooksFromFactionCatalog() {
        assertTrue(QuestHookSoftService.hookCount() >= 50);
        assertFalse(QuestHookSoftService.sampleHooks(5).isEmpty());
    }

    @Test
    void mapsKnownHooksToAuthoritativeTextQuestChains() {
        assertEquals("huangfeng_cultivation_path",
                QuestHookSoftService.mappedChainId("huangfeng_entry").orElse(""));
        assertEquals("kunwu_mountain_expedition",
                QuestHookSoftService.mappedChainId("kunwu_expedition").orElse(""));
        assertTrue(QuestHookSoftService.mappedChainId("mulan_side").isPresent());
        assertTrue(TextQuestChainService.find(
                QuestHookSoftService.mappedChainId("huangfeng_entry").orElseThrow()).isPresent());
    }
}
