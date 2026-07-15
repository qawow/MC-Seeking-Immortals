package com.xunxian.seekingimmortals.quest;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class TextQuestDialogueTreeServiceTest {
    @Test
    void treesCoverAllCatalogChainsAtLeastSixty() {
        assertTrue(TextQuestDialogueTreeService.demoTreeCount() >= 60);
        assertTrue(TextQuestDialogueTreeService.hasTree("huangfeng_cultivation_path"));
        assertTrue(TextQuestDialogueTreeService.hasTree("qixuan_mortal_path"));
        assertTrue(TextQuestDialogueTreeService.hasTree("demonic_six_expanded"));
    }

    @Test
    void nodeSelectionUsesStageAndComplete() {
        var start = TextQuestDialogueTreeService.nodeFor("huangfeng_cultivation_path", 0, false);
        assertTrue(start.isPresent());
        assertTrue(start.get().id().contains("start"));
        var complete = TextQuestDialogueTreeService.nodeFor("huangfeng_cultivation_path", 9, true);
        assertTrue(complete.isPresent());
        assertTrue(complete.get().id().contains("complete"));
    }
}
