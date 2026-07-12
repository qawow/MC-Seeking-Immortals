package com.xunxian.seekingimmortals.quest;

import com.xunxian.seekingimmortals.catalog.ExtendedCatalogService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TextQuestChainServiceTest {
    @Test
    void indexesAllSixtyTwoTextQuestChains() {
        assertEquals(62, TextQuestChainService.chainCount());
        assertTrue(TextQuestChainService.find("huangfeng_cultivation_path").isPresent());
        assertTrue(ExtendedCatalogService.builtin().questChains().values().stream()
                .anyMatch(chain -> chain.stepCount() >= 0));
    }

    @Test
    void exposesNpcBindingTableForAuthorityHooks() {
        assertEquals("npc_mo_lao", TextQuestChainService.npcFor("huangfeng_cultivation_path"));
        assertEquals("npc_text_quest_guide", TextQuestChainService.npcFor("craft_master"));
    }
}
