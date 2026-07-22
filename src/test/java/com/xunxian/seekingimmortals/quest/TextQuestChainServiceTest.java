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
        assertTrue(line.contains("STATE=ACTIVE"));
        assertTrue(line.contains("GATE=NONE"));
        assertTrue(line.length() <= 160);
    }

    @Test
    void trackerStateCoversAllFourStatesWithoutPlayerData() {
        assertEquals(TextQuestChainService.TrackerState.AVAILABLE,
                TextQuestChainService.trackerState(
                        new TextQuestChainService.ChainProgress("a", 0, 2, false),
                        new TextQuestChainService.StartEligibility(true,
                                TextQuestChainService.StartGate.NONE)));
        assertEquals(TextQuestChainService.TrackerState.LOCKED,
                TextQuestChainService.trackerState(
                        new TextQuestChainService.ChainProgress("a", 0, 2, false),
                        new TextQuestChainService.StartEligibility(false,
                                TextQuestChainService.StartGate.REGION)));
        assertEquals(TextQuestChainService.TrackerState.ACTIVE,
                TextQuestChainService.trackerState(
                        new TextQuestChainService.ChainProgress("a", 1, 2, false),
                        new TextQuestChainService.StartEligibility(false,
                                TextQuestChainService.StartGate.DATA)));
        assertEquals(TextQuestChainService.TrackerState.DONE,
                TextQuestChainService.trackerState(
                        new TextQuestChainService.ChainProgress("a", 2, 2, true),
                        new TextQuestChainService.StartEligibility(false,
                                TextQuestChainService.StartGate.DATA)));
    }

    @Test
    void trackerListsEveryCatalogChainAndBoundsRows() {
        var lines = TextQuestChainService.buildTrackerLines(null);
        assertEquals(62, lines.size());
        assertTrue(lines.stream().allMatch(value -> value.length() <= 160));
        assertTrue(lines.stream().allMatch(value -> value.contains("STATE=LOCKED GATE=DATA")));
    }

    @Test
    void silentEligibilityAndFallbackRewardPreviewAreSafeWithoutForgePlayer() {
        assertEquals(TextQuestChainService.StartGate.DATA,
                TextQuestChainService.startEligibility(null, "qixuan_mortal_path").gate());
        var preview = TextQuestChainService.finaleRewardPreview("qixuan_mortal_path");
        assertFalse(preview.isEmpty());
        assertTrue(preview.stream().allMatch(reward -> reward.itemId().contains(":")));
        assertTrue(preview.stream().allMatch(reward -> reward.count() > 0));
    }

    @Test
    void qixuanTutorialAloneAcceptsTheDefaultQinglanStartRegion() {
        assertTrue(TextQuestChainService.matchesStartRegion(
                "qixuan_mortal_path", "tiannan", "qinglan_mountains"));
        assertFalse(TextQuestChainService.matchesStartRegion(
                "huangfeng_cultivation_path", "tiannan", "qinglan_mountains"));
        assertTrue(TextQuestChainService.matchesStartRegion(
                "huangfeng_cultivation_path", "tiannan", "tiannan"));
    }

    @Test
    void rewardCountsAreBoundedWithoutIntegerOverflow() {
        assertEquals(1, TextQuestChainService.boundedRewardCount("0"));
        assertEquals(12, TextQuestChainService.boundedRewardCount("12"));
        assertEquals(4096, TextQuestChainService.boundedRewardCount("999999999999999999999999"));
        assertEquals(1, TextQuestChainService.boundedRewardCount("12x"));
    }

    @Test
    void authoredStartRequirementsAreLoadedForSafeNativeGates() {
        var ghost = TextQuestChainService.find("yin_luo_ghost_sect").orElseThrow().startRequirements();
        assertEquals("ghost_cultivator", ghost.pathRequired());
        var mulan = TextQuestChainService.find("mulan_fashi_path").orElseThrow().startRequirements();
        assertEquals("mulan_fashi", mulan.raceRequired());
        var depth = TextQuestChainService.find("diyuan_depth_delve").orElseThrow().startRequirements();
        assertEquals("void_great_cultivation_arc", depth.parentChain());
        var demonic = TextQuestChainService.find("demonic_six_path").orElseThrow().startRequirements();
        assertEquals("demonic_karma", demonic.karmaRequired());
    }

    @Test
    void advanceRejectsUnstartedChainsInSource() throws Exception {
        String source = java.nio.file.Files.readString(java.nio.file.Path.of(
                "src", "main", "java", "com", "xunxian", "seekingimmortals",
                "quest", "TextQuestChainService.java"));
        String compact = source.replaceAll("\\s+", "");
        int advance = compact.indexOf("publicstaticbooleanadvance(ServerPlayerplayer,StringchainId)");
        assertTrue(advance >= 0);
        int bodyStart = compact.indexOf('{', advance);
        int depth = 0;
        int bodyEnd = -1;
        for (int i = bodyStart; i < compact.length(); i++) {
            char c = compact.charAt(i);
            if (c == '{') depth++;
            else if (c == '}' && --depth == 0) {
                bodyEnd = i;
                break;
            }
        }
        String body = compact.substring(advance, bodyEnd + 1);
        assertTrue(body.contains("if(stage<=0)"), "advance must inspect unstarted stage");
        assertTrue(body.contains("text_quest.not_started"), "unstarted advance must warn not_started");
        assertTrue(body.contains("returnfalse;"), "unstarted advance must fail closed");
        assertFalse(body.contains("stage=1;") && body.indexOf("stage=1;") < body.indexOf("stage++"),
                "advance must not auto-start stage 1");
    }

    @Test
    void expectedHookMatchesCatalogStepOrder() {
        Optional<String> first = TextQuestChainService.expectedHookForStage("huangfeng_cultivation_path", 1);
        assertTrue(first.isPresent());
        assertEquals("huangfeng_entry", first.get());
        Optional<String> second = TextQuestChainService.expectedHookForStage("huangfeng_cultivation_path", 2);
        assertTrue(second.isPresent());
        assertEquals("alchemy_apprentice", second.get());
        assertTrue(TextQuestChainService.expectedHookForStage("huangfeng_cultivation_path", 0).isEmpty());
    }

    @Test
    void expectedHookAlsoReadsCompactStringSteps() {
        assertEquals("fengyuan_clan_intro",
                TextQuestChainService.expectedHookForStage("human_clan_neutral_intro", 1).orElseThrow());
        assertEquals("clan_guest_register",
                TextQuestChainService.expectedHookForStage("human_clan_neutral_intro", 2).orElseThrow());
    }

    @Test
    void chooseBranchRejectsSameBranchReputationFarmInSource() throws Exception {
        String source = java.nio.file.Files.readString(java.nio.file.Path.of(
                "src", "main", "java", "com", "xunxian", "seekingimmortals",
                "quest", "TextQuestChainService.java"));
        String compact = source.replaceAll("\\s+", "");
        int method = compact.indexOf("publicstaticbooleanchooseBranch(");
        assertTrue(method >= 0);
        int bodyStart = compact.indexOf('{', method);
        int depth = 0;
        int bodyEnd = -1;
        for (int i = bodyStart; i < compact.length(); i++) {
            char c = compact.charAt(i);
            if (c == '{') depth++;
            else if (c == '}' && --depth == 0) {
                bodyEnd = i;
                break;
            }
        }
        String body = compact.substring(method, bodyEnd + 1);
        assertTrue(body.contains("current.equals(normalized)") || body.contains("normalized.equals(current)"));
        assertTrue(body.contains("branch_same"));
        int sameGuard = body.indexOf("branch_same");
        int repAdd = body.indexOf("ReputationService.add(");
        assertTrue(sameGuard >= 0 && repAdd > sameGuard,
                "same-branch rejection must run before reputation grant");
    }
}
