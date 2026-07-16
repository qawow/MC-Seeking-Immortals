package com.xunxian.seekingimmortals.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
}
