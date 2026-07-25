package com.xunxian.seekingimmortals.quest;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DetailedQuestDialogueAuthorityTest {
    @Test
    void dialogueTurnInHasNoArbitraryActiveQuestFallback() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/xunxian/seekingimmortals/quest/QuestHookRuntime.java"));

        assertFalse(source.contains("advanceActiveNearNpc"));
        assertTrue(source.contains("DetailedQuestRuntimeService.turnIn(player, questIds, npcId, evidence)"));
        assertTrue(source.contains("advanceSingleCanonicalForNpc(player, questIds, npcId)"));
        assertTrue(source.contains("if (matches.size() == 1)"));
    }

    @Test
    void offerEffectsStartButDoNotAdvanceDetailedChains() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/xunxian/seekingimmortals/quest/QuestHookRuntime.java"));
        int offerCase = source.indexOf("case \"offer_quest\", \"open_quest\", \"open_quest_board\"");
        int turninCase = source.indexOf("case \"turnin_quests\"", offerCase);
        String offerBlock = source.substring(offerCase, turninCase);

        assertTrue(offerBlock.contains("DetailedQuestRuntimeService.start"));
        assertFalse(offerBlock.contains("DetailedQuestRuntimeService.advance"));
    }
}
