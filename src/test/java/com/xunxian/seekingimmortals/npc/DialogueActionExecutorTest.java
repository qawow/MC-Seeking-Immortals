package com.xunxian.seekingimmortals.npc;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DialogueActionExecutorTest {
    @Test
    void knownActionConstantsStableForM11() {
        assertEquals("open_shop", DialogueActionExecutor.OPEN_SHOP);
        assertEquals("grant_item", DialogueActionExecutor.GRANT_ITEM);
        assertEquals("teleport", DialogueActionExecutor.TELEPORT);
        assertEquals("turnin_quests", DialogueActionExecutor.TURNIN_QUESTS);
        assertEquals("enter_instance", DialogueActionExecutor.ENTER_INSTANCE);
    }

    @Test
    void publicApiSurfaceForM11() {
        // Compile-time contract: startDialogue / onDialogueNodeReached exist and are callable with null-safe guards.
        assertTrue(!NpcDialogueApi.startDialogue(null, "npc_huangfeng_contribution", null));
        assertTrue(!NpcDialogueApi.onDialogueNodeReached(null, "npc_x", "node_y"));
        assertTrue(NpcDialogueApi.getSession(null).isEmpty());
        assertTrue(NpcDialogueApi.currentView(null).isEmpty());
    }

    @Test
    void shopEffectIsDeferredBehindSyntheticChoice() {
        DialogueBranchService.Effect shop = new DialogueBranchService.Effect("open_shop", Map.of("shop", "market_stall"));
        DialogueBranchService.Effect reward = new DialogueBranchService.Effect("grant_item", Map.of("item", "token"));

        assertTrue(DialogueActionExecutor.isDeferredChoice(shop));
        assertTrue(!DialogueActionExecutor.isDeferredChoice(reward));
        assertEquals("effect:open_shop:0", NpcDialogueApi.effectChoiceId(shop, 0));
    }

    @Test
    void dialogueAnchorUsesEightBlockRadius() {
        assertEquals(64.0D, NpcDialogueApi.distanceSqr(8.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D));
        assertTrue(NpcDialogueApi.distanceSqr(8.01D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D) > 64.0D);
    }

    @Test
    void terminalEffectsClearDialogueSessionInsteadOfResendingView() {
        DialogueBranchService.Effect teleport = new DialogueBranchService.Effect("teleport", Map.of("to", "huangfeng"));
        DialogueBranchService.Effect enter = new DialogueBranchService.Effect("enter_instance", Map.of("realm", "blood_forbidden"));
        DialogueBranchService.Effect end = new DialogueBranchService.Effect("end", Map.of());
        DialogueBranchService.Effect shop = new DialogueBranchService.Effect("open_shop", Map.of("shop", "market_stall"));
        DialogueBranchService.Effect grant = new DialogueBranchService.Effect("grant_item", Map.of("item", "token"));
        DialogueBranchService.Effect travelUi = new DialogueBranchService.Effect("open_travel_ui", Map.of());

        assertTrue(DialogueActionExecutor.isTerminalEffect(teleport));
        assertTrue(DialogueActionExecutor.isTerminalEffect(enter));
        assertTrue(DialogueActionExecutor.isTerminalEffect(end));
        assertTrue(DialogueActionExecutor.isTerminalEffect(shop));
        assertTrue(DialogueActionExecutor.isTerminalEffect(travelUi));
        assertTrue(!DialogueActionExecutor.isTerminalEffect(grant));

        // Deferred shop is terminal once chosen, but immediate node effects ignore it so the page can show the choice.
        assertTrue(!DialogueActionExecutor.hasTerminalEffect(java.util.List.of(shop)));
        assertTrue(DialogueActionExecutor.hasTerminalEffect(java.util.List.of(teleport, grant)));
        assertTrue(!DialogueActionExecutor.hasTerminalEffect(java.util.List.of(grant)));
        assertTrue(!DialogueActionExecutor.hasTerminalEffect(java.util.List.of()));
    }

    @Test
    void shippedNodesHaveAtMostOneImmediateEffectForRetrySafety() {
        for (DialogueBranchService.Tree tree : DialogueBranchService.builtin().trees().values()) {
            for (DialogueBranchService.Node node : tree.nodes().values()) {
                long immediate = node.effects().stream()
                        .filter(effect -> !DialogueActionExecutor.isDeferredChoice(effect))
                        .count();
                assertTrue(immediate <= 1, tree.id() + ":" + node.id() + " immediate=" + immediate);
            }
        }
    }
}
