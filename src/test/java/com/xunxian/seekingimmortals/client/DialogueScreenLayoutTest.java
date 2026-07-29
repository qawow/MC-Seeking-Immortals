package com.xunxian.seekingimmortals.client;

import org.junit.jupiter.api.Test;

import com.xunxian.seekingimmortals.network.OpenDialogueScreenPacket;
import net.minecraft.network.chat.Component;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DialogueScreenLayoutTest {
    @Test
    void eightDynamicChoicesFitScaledWindows() {
        for (int[] size : new int[][]{{120, 90}, {320, 180}, {854, 480}}) {
            DialogueScreen.Layout layout = DialogueScreen.calculateLayout(size[0], size[1], 8);
            assertEquals(8, layout.choiceButtons().size());
            assertTrue(layout.panel().inside(size[0], size[1]));
            assertTrue(layout.promptViewport().inside(size[0], size[1]));
            assertTrue(layout.refresh().inside(size[0], size[1]));
            assertTrue(layout.close().inside(size[0], size[1]));
            assertTrue(layout.choiceButtons().stream().allMatch(rect -> rect.inside(size[0], size[1])));
            assertTrue(layout.choiceButtons().stream().noneMatch(layout.promptViewport()::intersects));
        }
    }

    @Test
    void emptyChoiceViewKeepsContentAreaAndNoPhantomButtons() {
        DialogueScreen.Layout layout = DialogueScreen.calculateLayout(320, 180, 0);
        assertTrue(layout.choiceButtons().isEmpty());
        assertTrue(layout.promptViewport().height() > 0);
        assertFalse(layout.promptViewport().intersects(layout.close()));
    }

    @Test
    void actionLatchRecoversOnlyAfterBoundedTimeout() {
        assertFalse(DialogueScreen.actionTimedOut(DialogueScreen.ACTION_TIMEOUT_TICKS - 1));
        assertTrue(DialogueScreen.actionTimedOut(DialogueScreen.ACTION_TIMEOUT_TICKS));
        assertTrue(DialogueScreen.actionTimedOut(DialogueScreen.ACTION_TIMEOUT_TICKS + 20));
    }

    @Test
    void dialoguePacketsDoNotCoverContainerScreens() {
        assertFalse(ClientPacketHandlers.canReplaceDialogue(true, false));
        assertTrue(ClientPacketHandlers.canReplaceDialogue(true, true));
        assertTrue(ClientPacketHandlers.canReplaceDialogue(false, false));
    }

    @Test
    void sameSessionCanCarryGreetingLatchAcrossScreenRebuild() {
        OpenDialogueScreenPacket packet = new OpenDialogueScreenPacket(
                "session-1", "source", "npc", "node", Component.literal("说话人"),
                List.of(Component.literal("正文")), List.of());
        DialogueScreen first = new DialogueScreen(packet);
        DialogueScreen rebuilt = new DialogueScreen(packet, first.greetingPlayed());

        assertFalse(first.greetingPlayed());
        assertFalse(rebuilt.greetingPlayed());
        DialogueScreen carried = new DialogueScreen(packet, true);
        assertTrue(carried.greetingPlayed());
        assertEquals("session-1", carried.sessionContext());
    }

    @Test
    void loreDetailScrollClampsToContentBounds() {
        assertEquals(0, AbstractLoreScreen.clampDetailScroll(-5, 100, 40));
        assertEquals(60, AbstractLoreScreen.clampDetailScroll(100, 100, 40));
        assertEquals(0, AbstractLoreScreen.clampDetailScroll(10, 20, 40));
    }

    @Test
    void undiscoveredChronicleNamesUseOnlyLockedLabel() {
        assertNotEquals("secret_phase", ChronicleScreen.displayName("secret_phase", false));
        assertEquals("secret_phase", ChronicleScreen.displayName("secret_phase", true));
    }
}
