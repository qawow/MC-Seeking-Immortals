package com.xunxian.seekingimmortals.quest;

import com.xunxian.seekingimmortals.npc.DialogueBranchService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestHookRuntimeTest {
    @Test
    void destinationLinksDoNotCrossContaminateSiblingEffects() {
        DialogueBranchService.Effect offer = new DialogueBranchService.Effect(
                "offer_quest", Map.of("id", "zhenyan_outer_lesson"));
        DialogueBranchService.Effect realm = new DialogueBranchService.Effect(
                "enter_instance", Map.of("realm", "blood_forbidden"));

        assertEquals(List.of("zhenyan_outer_lesson"), QuestHookRuntime.resolveQuestIds(offer));
        assertEquals(List.of("blood_forbidden_run", "nangong_wan_weight_optional"),
                QuestHookRuntime.resolveQuestIds(realm));
    }

    @Test
    void onlyAuthoredBareLinksFallbackAndArrayIdsAreDeduplicated() {
        DialogueBranchService.Effect unknown = new DialogueBranchService.Effect(
                "offer_quest", Map.of("id", "not_authored"));
        DialogueBranchService.Effect travel = new DialogueBranchService.Effect(
                "start_teleport", Map.of());
        DialogueBranchService.Effect array = new DialogueBranchService.Effect(
                "offer_quest", Map.of("id", "zhenyan_outer_lesson",
                        "quest_ids", List.of("zhenyan_outer_lesson", "extra")));

        assertTrue(QuestHookRuntime.resolveQuestIds(unknown).isEmpty());
        assertEquals(List.of("any_travel"), QuestHookRuntime.resolveQuestIds(travel));
        assertEquals(List.of("zhenyan_outer_lesson", "extra"), QuestHookRuntime.resolveQuestIds(array));
    }

    @Test
    void hookAdvanceRequiresCurrentStepMatchInSource() throws Exception {
        String source = java.nio.file.Files.readString(java.nio.file.Path.of(
                "src", "main", "java", "com", "xunxian", "seekingimmortals",
                "quest", "QuestHookRuntime.java"));
        String compact = source.replaceAll("\\s+", "");
        assertTrue(compact.contains("matchesCurrentStepHook(player,id,hookId)"),
                "hook-driven advances must match the current step hook");
        assertTrue(compact.contains("tryAdvanceActive(player,chainId,hook)"),
                "tryAdvanceByHook must pass the fired hook into the active advance path");
        int stageGuard = compact.indexOf("if(progress.stage()<=0){return;}");
        int advance = compact.indexOf("TextQuestChainService.advance(player,id)");
        assertTrue(stageGuard >= 0 && advance > stageGuard,
                "hook advances must require an already-started chain");
    }
}
