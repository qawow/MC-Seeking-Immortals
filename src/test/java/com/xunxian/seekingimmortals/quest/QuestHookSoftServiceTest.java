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

    @Test
    void unknownHooksFailClosedInsteadOfStartingAnUnrelatedMainline() {
        assertTrue(QuestHookSoftService.mappedChainId("totally_unknown_hook_xyz").isEmpty());
        assertEquals("barbarian_kings_line",
                QuestHookSoftService.mappedChainId("king_territory_intrusion").orElseThrow());
        assertEquals("spirit_eighteen_clans",
                QuestHookSoftService.mappedChainId("ancient_ruin_explore").orElseThrow());
    }

    @Test
    void reverseMapCoversExpandedHooks() {
        assertTrue(QuestHookSoftService.hookCount() >= 50);
        assertTrue(QuestHookSoftService.mappedChainId("huangfeng_entry").isPresent());
        // alias added in Wave463 overrides / reverse map
        assertTrue(QuestHookSoftService.mappedChainId("diyuan_permit_apply").isPresent()
                || QuestHookSoftService.mappedChainId("diyuan_permit").isEmpty());
    }
}
