package com.xunxian.seekingimmortals.catalog;

import com.xunxian.seekingimmortals.quest.TextQuestChainService;
import com.xunxian.seekingimmortals.quest.TextQuestDialogueService;
import com.xunxian.seekingimmortals.worldpack.ReputationService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TextMaterialSmokeTest {
    @Test
    void auctionCatalogLoadsAndLadderIncreases() {
        AuctionSoftService.Snapshot snapshot = AuctionSoftService.builtin();
        assertTrue(snapshot.lotCount() >= 1);
        assertTrue(snapshot.minIncrementPct() > 0.0D);
        AuctionSoftService.Lot lot = snapshot.lots().get(0);
        int base = AuctionSoftService.nextBidCost(lot, 0, snapshot.minIncrementPct());
        int raised = AuctionSoftService.nextBidCost(lot, base, snapshot.minIncrementPct());
        assertTrue(raised > base);
    }

    @Test
    void questCatalogAndBranchesExist() {
        assertTrue(TextQuestChainService.chainCount() >= 1);
        assertEquals("righteous", TextQuestChainService.BRANCH_RIGHTEOUS);
        assertEquals("neutral", TextQuestChainService.BRANCH_NEUTRAL);
        assertEquals("demonic", TextQuestChainService.BRANCH_DEMONIC);
        assertFalse(TextQuestDialogueService.sampleNpcHooks(5).isEmpty());
    }

    @Test
    void reputationDiscountTableWithoutPlayer() {
        assertEquals(1.0D, ReputationService.shopDiscountMultiplier(null, "market_herbal_stall"), 0.0001D);
        assertEquals("none", ReputationService.discountLabel(null, "market_herbal_stall"));
    }

    @Test
    void summonServiceSurfacePresent() {
        assertTrue(SummonHonestMvpService.puppetDefinitionCount() >= 0);
        assertEquals("BEAST", SummonHonestMvpService.archetypeOf("beast_summon").name());
        assertEquals("PUPPET", SummonHonestMvpService.archetypeOf("summon_wood_puppet").name());
        assertEquals("GHOST", SummonHonestMvpService.archetypeOf("ghost_king_summon").name());
    }

    @Test
    void questStageCostTableHasFinaleAndMid() {
        assertTrue(TextQuestChainService.stageCostFor("ghost_path", 6, 6).isPresent());
        assertTrue(TextQuestChainService.stageCostFor("huangfeng_cultivation_path", 3, 6).isPresent()
                || TextQuestChainService.stageCostFor("huangfeng_cultivation_path", 6, 6).isPresent());
        assertTrue(TextQuestChainService.stageCostFor("any_chain", 1, 4).isEmpty());
    }
}
