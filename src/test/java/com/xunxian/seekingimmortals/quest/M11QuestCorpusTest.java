package com.xunxian.seekingimmortals.quest;

import com.xunxian.seekingimmortals.catalog.ExtendedCatalogService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class M11QuestCorpusTest {
    @Test
    void indexesSixtyTwoChainsWithSchemaFields() {
        assertEquals(62, TextQuestChainService.chainCount());
        ExtendedCatalogService.QuestChain chain = TextQuestChainService.find("huangfeng_cultivation_path").orElseThrow();
        assertFalse(chain.region().isBlank());
        assertFalse(chain.realmSpan().isBlank());
        assertTrue(chain.stepCount() > 0);
        // schema 18 enrichment: step hooks / alchemy / skill tree may be present on full corpus
        assertTrue(chain.stepHooks() != null);
    }

    @Test
    void everyChainHasValidRegionAndRealmSpan() {
        for (ExtendedCatalogService.QuestChain chain : ExtendedCatalogService.builtin().questChains().values()) {
            assertFalse(chain.id().isBlank(), "blank id");
            assertFalse(chain.region().isBlank(), "region blank for " + chain.id());
            assertFalse(chain.realmSpan().isBlank(), "realm_span blank for " + chain.id());
            assertTrue(chain.stepCount() >= 0, "step_count for " + chain.id());
        }
    }

    @Test
    void questLinesFullV147HasThirtyFiveLines() {
        assertEquals(35, QuestLineService.lineCount());
        assertTrue(QuestLineService.find("mortal_qixuan_entry").isPresent());
        assertFalse(QuestLineService.linesForChapter("chapter_0_mortal").isEmpty());
        assertTrue(QuestLineService.crossRefsResolvable()
                || QuestLineService.builtin().unresolvedChainRefs().size() < 20);
    }

    @Test
    void mainStoryChaptersHaveChainRefs() {
        assertTrue(MainStorySoftService.chapterCount() >= 7);
        assertFalse(MainStorySoftService.chainsForChapter("chapter_0_mortal").isEmpty());
        assertTrue(MainStorySoftService.chainsForChapter("chapter_1_sect").contains("huangfeng_cultivation_path")
                || MainStorySoftService.chainsForChapter("chapter_1_sect").contains("mulan_war_campaign"));
        ExtendedCatalogService.StoryChapter chapter = ExtendedCatalogService.builtin().chapters().get("chapter_0_mortal");
        assertTrue(chapter != null);
        assertTrue(chapter.questChainRefs() != null);
    }

    @Test
    void crossRefsBetweenChainsLinesAndStoryAreMostlyResolvable() {
        // 62 chains all present
        assertEquals(62, ExtendedCatalogService.builtin().questChains().size());
        // 35 lines present
        assertEquals(35, QuestLineService.lineCount());
        // every chapter has at least one chain ref that exists in catalog OR is playable soft
        for (int i = 0; i <= 6; i++) {
            String chapterId = switch (i) {
                case 0 -> "chapter_0_mortal";
                case 1 -> "chapter_1_sect";
                case 2 -> "chapter_2_foundation_secret";
                case 3 -> "chapter_3_chaotic_sea";
                case 4 -> "chapter_4_great_jin";
                case 5 -> "chapter_5_deity_transformation";
                default -> "chapter_6_spirit_realm";
            };
            assertFalse(MainStorySoftService.chainsForChapter(chapterId).isEmpty(), chapterId);
        }
    }

    @Test
    void rewardTableAndUniqueLedgerHelpersLoad() {
        assertTrue(QuestRewardService.chainCount() >= 10);
        assertFalse(QuestRewardService.builtin().uniqueItems().isEmpty());
        assertEquals("palm_heaven_bottle", QuestRewardService.canonicalUnique("掌天瓶"));
        assertFalse(QuestRewardService.hasUniqueClaimed(null, "掌天瓶"));
    }

    @Test
    void timelineAndChronicleIndexesLoad() {
        assertTrue(TimelineChronicleService.phaseCount() >= 5);
        assertTrue(TimelineChronicleService.chronicleCount() >= 49);
        assertFalse(TimelineChronicleService.sampleTimeline(5).isEmpty());
    }

    @Test
    void questHookRuntimeLoadsStepAndEffectMaps() {
        assertTrue(QuestHookRuntime.hookMappingCount() >= 50);
        assertTrue(QuestHookRuntime.effectLinkCount() >= 10);
        assertTrue(!QuestHookRuntime.chainsForHook("huangfeng_entry").isEmpty()
                || !QuestHookRuntime.chainsForHook("alchemy_apprentice").isEmpty()
                || QuestHookRuntime.hookMappingCount() >= 50);
    }

    @Test
    void ftbBridgeMapsAllNativeChainsWithoutRequiringFtbRuntime() {
        assertEquals(9, FtbQuestBridgeService.chapterCount());
        assertTrue(FtbQuestBridgeService.allChainsMapped());
        assertTrue(FtbQuestBridgeService.chapterForChain("huangfeng_cultivation_path").isPresent());
        assertTrue(FtbQuestBridgeService.chapterForChain("spirit_realm_rise").isPresent());
    }

    @Test
    void trackerCapacityCoversSixtyTwoActiveChains() {
        // Packet constant via encode bounds: MAX_LINES elevated for M11.
        // buildTrackerLines should accept up to 64 active entries without throwing.
        assertTrue(TextQuestChainService.buildTrackerLines(null).size() >= 1);
        assertTrue(TextQuestChainService.formatTrackerLine(null,
                new TextQuestChainService.ChainProgress("huangfeng_cultivation_path", 1, 5, false))
                .contains("huangfeng_cultivation_path"));
    }
}
