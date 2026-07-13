package com.xunxian.seekingimmortals.quest;

import com.xunxian.seekingimmortals.catalog.ExtendedCatalogService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

    @Test
    void stageCostsAreDeterministicAndFreeOnStart() {
        Optional<TextQuestChainService.StageCost> free = TextQuestChainService.stageCostFor("huangfeng_cultivation_path", 1, 6);
        assertTrue(free.isEmpty());

        Optional<TextQuestChainService.StageCost> mid = TextQuestChainService.stageCostFor("huangfeng_cultivation_path", 3, 6);
        assertTrue(mid.isPresent());
        assertEquals("seeking_immortals:spirit_stone_shard", mid.get().itemId());
        assertTrue(mid.get().count() >= 1);

        Optional<TextQuestChainService.StageCost> ghostFinale = TextQuestChainService.stageCostFor("ghost_path", 8, 8);
        assertTrue(ghostFinale.isPresent());
        assertEquals("seeking_immortals:soul_fragment", ghostFinale.get().itemId());
    }

    @Test
    void everyThirdStageRequestsGenericShardCost() {
        Optional<TextQuestChainService.StageCost> cost = TextQuestChainService.stageCostFor("craft_master", 3, 9);
        assertTrue(cost.isPresent());
        assertEquals("seeking_immortals:spirit_stone_shard", cost.get().itemId());
        assertEquals(1, cost.get().count());
    }

    @Test
    void authorityRewardLedgerHelpersAreStable() {
        // Pure static helpers: null player must not throw and must report unpaid.
        assertFalse(TextQuestChainService.hasAuthorityReward(null, "huangfeng_cultivation_path"));
    }

    @Test
    void catalogFinaleAndChapterRefsAreIndexed() {
        Optional<ExtendedCatalogService.QuestChain> chain = TextQuestChainService.find("huangfeng_cultivation_path");
        assertTrue(chain.isPresent());
        assertFalse(chain.get().rewardsFinale().isEmpty());
        assertEquals("chapter_1_sect", chain.get().mainChapterRef());
        // Pure parse path: catalogFinaleRewards must not throw without Forge bootstrap.
        assertTrue(TextQuestChainService.catalogFinaleRewards("huangfeng_cultivation_path") != null);
    }

    @Test
    void trackerLineFormatIsMachineReadable() {
        TextQuestChainService.ChainProgress progress =
                new TextQuestChainService.ChainProgress("huangfeng_cultivation_path", 2, 5, false);
        // Null player path uses countOwned=0; format still includes tags.
        String line = TextQuestChainService.formatTrackerLine(null, progress);
        assertTrue(line.startsWith("huangfeng_cultivation_path 2/5"));
        assertTrue(line.contains("branch="));
        assertTrue(line.contains("LOCK="));
        assertTrue(line.contains("REW="));
        assertTrue(line.contains("cost="));
    }
}
