package com.xunxian.seekingimmortals.quest;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TextQuestNpcHookServiceTest {
    @Test
    void bindsChainKeywordsToStableNpcIds() {
        assertEquals("npc_mo_lao", TextQuestNpcHookService.npcIdForChain("huangfeng_cultivation_path"));
        assertEquals("npc_mulan_envoy", TextQuestNpcHookService.npcIdForChain("mulan_tianlan_war"));
        assertEquals("npc_yinluo_steward", TextQuestNpcHookService.npcIdForChain("ghost_path"));
        assertEquals("npc_star_palace_broker", TextQuestNpcHookService.npcIdForChain("star_palace_internal_politics"));
        assertEquals("npc_kunwu_steward", TextQuestNpcHookService.npcIdForChain("dajin_kunwu_line"));
    }

    @Test
    void resolvesNpcAliasToAKnownChain() {
        assertTrue(TextQuestNpcHookService.chainForNpcId("npc_mo_lao").isPresent());
        assertTrue(TextQuestNpcHookService.chainForNpcId("墨老先生").isPresent());
        assertEquals("npc_mo_lao",
                TextQuestNpcHookService.npcIdForChain(
                        TextQuestNpcHookService.chainForNpcId("npc_mo_lao").orElseThrow()));
        assertFalse(TextQuestNpcHookService.sampleBindings(5).isEmpty());
    }
}
