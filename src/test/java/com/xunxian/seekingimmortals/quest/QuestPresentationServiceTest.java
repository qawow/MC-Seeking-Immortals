package com.xunxian.seekingimmortals.quest;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestPresentationServiceTest {
    @Test
    void joinsChainHookAndHandbookPresentationWithoutIdFallbacks() {
        QuestPresentationService.ChainPresentation qixuan =
                QuestPresentationService.find("qixuan_mortal_path").orElseThrow();

        assertEquals("七玄门凡俗线", qixuan.titleZh());
        assertEquals("Qixuan Mortal Path", qixuan.titleEn());
        assertEquals(4, qixuan.stepCount());
        assertEquals("山村与墨大夫传闻", qixuan.stage(1).orElseThrow().titleZh());
        assertEquals("mortal", qixuan.realmMin());
        assertTrue(qixuan.descriptionZh().contains("山村与墨大夫传闻"));
        assertFalse(qixuan.titleZh().contains("_"));
        assertFalse(qixuan.descriptionZh().contains("qixuan"));
    }

    @Test
    void exposesConcreteNativeCostsAndAuthorityRewards() {
        TextQuestChainService.StageCost midpoint =
                QuestPresentationService.nextStageCost("qixuan_mortal_path", 1).orElseThrow();
        assertEquals("seeking_immortals:spirit_stone_shard", midpoint.itemId());
        assertEquals(2, midpoint.count());

        List<TextQuestChainService.RewardPreview> finale =
                QuestPresentationService.finaleRewards("qixuan_mortal_path");
        assertTrue(finale.stream().anyMatch(reward ->
                reward.itemId().endsWith("spirit_stone_shard") && reward.count() == 12));
        assertEquals(2, QuestPresentationService.midpointRewards("qixuan_mortal_path").get(0).count());
        assertEquals(4, QuestPresentationService.branchRewards("demonic").get(0).count());
    }

    @Test
    void preservesCompactStringStepsAsAuthoredHookObjectives() {
        QuestPresentationService.ChainPresentation clan =
                QuestPresentationService.find("human_clan_neutral_intro").orElseThrow();

        assertEquals("fengyuan_clan_intro", clan.stage(1).orElseThrow().hookId());
        assertEquals("风元世家引荐", clan.stage(1).orElseThrow().titleZh());
        assertEquals("clan_guest_register", clan.stage(2).orElseThrow().hookId());
        assertEquals("世家客卿登记", clan.stage(2).orElseThrow().titleZh());
    }

    @Test
    void resolvesHandbookOnlyMilestonesAcrossTheFullNativeCorpus() {
        assertEquals("逆星走私补给", QuestPresentationService.stageLabel(
                "inverse_star_smuggle_arc", 1, true));
        assertEquals("逆星黑市购货", QuestPresentationService.stageLabel(
                "inverse_star_void_heist", 2, true));

        QuestPresentationService.builtin().chains().values().forEach(chain ->
                chain.stages().forEach(stage -> {
                    assertFalse(stage.titleZh().matches("第\\d+阶段"),
                            chain.id() + " stage " + stage.number() + " still uses a generic label");
                    assertFalse(stage.titleZh().contains("_"),
                            chain.id() + " stage " + stage.number() + " exposes an internal id");
                }));
    }

    @Test
    void exposesAuthoredConditionsWithoutLeakingInternalRequirementIds() {
        QuestPresentationService.ChainPresentation ghost =
                QuestPresentationService.find("yin_luo_ghost_sect").orElseThrow();
        assertTrue(ghost.requirements().stream().anyMatch(value ->
                value.textZh().contains("鬼修道途") && value.enforced()));

        QuestPresentationService.ChainPresentation demonic =
                QuestPresentationService.find("demonic_six_expanded").orElseThrow();
        assertTrue(demonic.requirements().stream().anyMatch(value ->
                value.textZh().contains("魔道因果") && value.enforced()));
        assertTrue(demonic.requirements().stream().noneMatch(value ->
                value.textZh().contains("demonic_karma") || value.textZh().contains("extends_chain")));

        QuestPresentationService.ChainPresentation politics =
                QuestPresentationService.find("chaotic_sea_politics").orElseThrow();
        // One-time branch lock makes mixed rebel/loyalist hard gates impossible; stages
        // must not enforce them.
        assertTrue(politics.stage(3).isEmpty() || politics.stage(3).orElseThrow()
                .requirements().stream().noneMatch(value ->
                        value.textZh().contains("逆星盟") && value.enforced()));
    }
}
