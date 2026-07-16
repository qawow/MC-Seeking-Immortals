package com.xunxian.seekingimmortals.npc;

import org.junit.jupiter.api.Test;

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
}
