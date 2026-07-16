package com.xunxian.seekingimmortals.quest;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TextQuestDialogueViewTest {
    @Test
    void onlyIdsPresentInCurrentViewAreAccepted() {
        List<TextQuestDialogueService.DialogueLine> lines = List.of(
                new TextQuestDialogueService.DialogueLine("npc", "line.body", ""),
                new TextQuestDialogueService.DialogueLine("npc", "line.advance", "advance"));

        assertTrue(TextQuestDialogueService.isChoiceAllowed(lines, "advance"));
        assertFalse(TextQuestDialogueService.isChoiceAllowed(lines, "righteous"));
        assertFalse(TextQuestDialogueService.isChoiceAllowed(lines, ""));
    }

    @Test
    void legacyDialogueUsesSameEightBlockAnchor() {
        assertTrue(TextQuestDialogueService.distanceSqr(0.0D, 0.0D, 8.0D,
                0.0D, 0.0D, 0.0D) <= 64.0D);
        assertFalse(TextQuestDialogueService.distanceSqr(0.0D, 0.0D, 8.1D,
                0.0D, 0.0D, 0.0D) <= 64.0D);
    }
}
