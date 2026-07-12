package com.xunxian.seekingimmortals.sect;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class SectContentServiceTest {
    @Test
    void everySectHasDialogueNodesAndMissionPool() {
        for (SectDefinitionService.SectDefinition definition : SectDefinitionService.playableDefinitions()) {
            SectContentService.DialogueTree dialogue = SectContentService.dialogueForTest(definition.id());

            assertFalse(dialogue.nodes().isEmpty(), definition.id() + " dialogue nodes missing");
            assertFalse(SectContentService.nodeForStage(definition.id(), SectContributionService.STAGE_KNOCKING).options().isEmpty());
            assertFalse(SectContentService.nodeForStage(definition.id(), SectContributionService.STAGE_OUTER_DISCIPLE).options().isEmpty());
            assertFalse(SectContentService.nodeForStage(definition.id(), SectContributionService.STAGE_FOUNDATION_DILEMMA).options().isEmpty());
            assertFalse(SectContentService.nodeForStage(definition.id(), SectContributionService.STAGE_INNER_DISCIPLE).options().isEmpty());
            assertFalse(SectContentService.missionsForTest(definition.id()).isEmpty(), definition.id() + " mission pool missing");
        }
    }

    @Test
    void missionGenerationIsDeterministicForDayAndSect() {
        for (SectDefinitionService.SectDefinition definition : SectDefinitionService.playableDefinitions()) {
            SectContentService.MissionDefinition first = SectContentService.missionForDay(
                    definition.id(), SectContributionService.STAGE_OUTER_DISCIPLE, 42L);
            SectContentService.MissionDefinition second = SectContentService.missionForDay(
                    definition.id(), SectContributionService.STAGE_OUTER_DISCIPLE, 42L);

            assertEquals(first, second, definition.id() + " mission generation must be stable");
        }
    }
}
