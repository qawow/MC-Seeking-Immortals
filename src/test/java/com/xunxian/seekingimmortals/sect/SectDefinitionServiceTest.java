package com.xunxian.seekingimmortals.sect;

import com.xunxian.seekingimmortals.quest.QuestProgress;
import com.xunxian.seekingimmortals.quest.SevenMysteriesQuest;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SectDefinitionServiceTest {
    @Test
    void thirtySectDefinitionsHaveUniqueIds() {
        Set<String> ids = new HashSet<>();

        for (SectDefinitionService.SectDefinition definition : SectDefinitionService.playableDefinitions()) {
            assertFalse(definition.id().isBlank());
            assertTrue(ids.add(definition.id()), "Duplicate sect id " + definition.id());
            assertFalse(definition.displayZh().isBlank());
            assertFalse(definition.displayEn().isBlank());
            assertFalse(definition.shopId().isBlank());
            assertFalse(definition.stewardName().isBlank());
            assertTrue(definition.structureId().startsWith("seeking_immortals:"));
            assertEquals(SectContributionService.STAGE_KNOCKING, definition.initialStage());
            assertTrue(definition.playable());
        }

        assertEquals(30, ids.size());
        assertTrue(ids.contains(SectContributionService.SECT_ID));
        assertTrue(SectDefinitionService.definitions().size() >= 6);
        assertTrue(SectDefinitionService.catalogSectCount() >= 20);
    }

    @Test
    void candidatesRequireSevenMysteriesCompletionAndYueArrival() {
        QuestProgress progress = new QuestProgress();

        assertTrue(SectDefinitionService.candidates(progress).isEmpty());

        progress.setStage(SevenMysteriesQuest.STAGE_COMPLETE);
        progress.setYueArrived(true);

        assertEquals(30, SectDefinitionService.candidates(progress).size());
    }

    @Test
    void applyWritesExistingQuestProgressSectFields() {
        QuestProgress progress = new QuestProgress();
        progress.setStage(SevenMysteriesQuest.STAGE_COMPLETE);
        progress.setYueArrived(true);

        SectDefinitionService.ApplyResult result = SectDefinitionService.apply(progress, "danxia_valley");

        assertTrue(result.success());
        assertEquals("danxia_valley", progress.getSectId());
        assertEquals(SectDefinitionService.CANDIDATE_ROLE, progress.getSectRole());
        assertEquals(SectContributionService.STAGE_KNOCKING, progress.getSectQuestStage());
    }

    @Test
    void applyRejectsUnknownLockedAndSecondSect() {
        QuestProgress progress = new QuestProgress();

        SectDefinitionService.ApplyResult unknown = SectDefinitionService.apply(progress, "missing");
        assertEquals(SectDefinitionService.ApplyStatus.UNKNOWN_SECT, unknown.status());
        assertNull(unknown.definition());

        SectDefinitionService.ApplyResult locked = SectDefinitionService.apply(progress, "qinglan_sect");
        assertEquals(SectDefinitionService.ApplyStatus.LOCKED, locked.status());

        progress.setStage(SevenMysteriesQuest.STAGE_COMPLETE);
        progress.setYueArrived(true);
        assertTrue(SectDefinitionService.apply(progress, "qinglan_sect").success());

        SectDefinitionService.ApplyResult second = SectDefinitionService.apply(progress, "danxia_valley");
        assertEquals(SectDefinitionService.ApplyStatus.OTHER_SECT, second.status());
    }

    @Test
    void canonicalizeAliasesCollapseToPlayableSects() {
        assertEquals("guiling_gate", SectDefinitionService.canonicalizeSectId("ghost_spirit_gate"));
        assertEquals("qianzhu_sect", SectDefinitionService.canonicalizeSectId("qianzhu_teach"));
        assertEquals("yuling_pavilion", SectDefinitionService.canonicalizeSectId("yuling_sect"));
        assertEquals("yuling_pavilion", SectDefinitionService.canonicalizeSectId("yuling_sect_secret"));
        assertTrue(SectDefinitionService.find("ghost_spirit_gate").isPresent());
        assertEquals("guiling_gate", SectDefinitionService.find("ghost_spirit_gate").orElseThrow().id());
    }
}
