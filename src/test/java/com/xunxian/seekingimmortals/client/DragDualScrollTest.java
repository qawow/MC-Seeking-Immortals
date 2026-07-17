package com.xunxian.seekingimmortals.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies graph-node freeform drag math and list+detail dual-scroll independence
 * after journal-shell migrations (MethodTree / TechniqueEdit).
 */
class DragDualScrollTest {
    @Test
    void graphNodeDragClampsInsideGraphBounds() {
        int[] inside = MethodTreeScreen.clampGraphNodePosition(130, 70, 100, 50, 120, 80, 40, 16);
        assertArrayEquals(new int[]{130, 70}, inside);

        int[] leftTop = MethodTreeScreen.clampGraphNodePosition(0, 0, 100, 50, 120, 80, 40, 16);
        assertArrayEquals(new int[]{100, 50}, leftTop);

        int[] rightBottom = MethodTreeScreen.clampGraphNodePosition(999, 999, 100, 50, 120, 80, 40, 16);
        assertArrayEquals(new int[]{100 + 120 - 40, 50 + 80 - 16}, rightBottom);
    }

    @Test
    void graphNodeDragOffsetTracksDefaultGridSlot() {
        int[] off = MethodTreeScreen.offsetFromGrid(
                100 + 1 * (42 + 6) + 5,
                50 + 1 * (16 + 4) - 3,
                100, 50,
                4, 3,
                42, 16, 6, 4);
        assertArrayEquals(new int[]{5, -3}, off);
    }

    @Test
    void graphHitTestMatchesInclusiveDragStartBounds() {
        assertTrue(MethodTreeScreen.graphHitContains(10, 20, 40, 16, 10, 20));
        assertTrue(MethodTreeScreen.graphHitContains(10, 20, 40, 16, 50, 36));
        assertFalse(MethodTreeScreen.graphHitContains(10, 20, 40, 16, 50.1, 36));
        assertFalse(MethodTreeScreen.graphHitContains(10, 20, 40, 16, 9.9, 20));
    }

    @Test
    void methodTreeListAndDetailScrollIndependently() {
        // positive delta (wheel up) => direction +1 => scroll decreases;
        // negative delta (wheel down) => direction -1 => scroll increases.
        assertEquals(12, MethodTreeScreen.maxListScroll(20, 8));
        assertEquals(0, MethodTreeScreen.scrollListBy(0, 1, 20, 8));
        assertEquals(1, MethodTreeScreen.scrollListBy(0, -1, 20, 8));
        assertEquals(12, MethodTreeScreen.scrollListBy(12, -1, 20, 8));
        assertEquals(11, MethodTreeScreen.scrollListBy(12, 1, 20, 8));

        assertEquals(200, MethodTreeScreen.maxDetailScroll(300, 100));
        assertEquals(0, MethodTreeScreen.scrollDetailBy(0, 1, 300, 100, 12));
        assertEquals(12, MethodTreeScreen.scrollDetailBy(0, -1, 300, 100, 12));
        assertEquals(200, MethodTreeScreen.scrollDetailBy(200, -1, 300, 100, 12));
        assertEquals(188, MethodTreeScreen.scrollDetailBy(200, 1, 300, 100, 12));

        int list = MethodTreeScreen.scrollListBy(3, -1, 20, 8);
        int detail = MethodTreeScreen.scrollDetailBy(24, 1, 300, 100, 12);
        assertEquals(4, list);
        assertEquals(12, detail);
    }

    @Test
    void methodTreeWideLayoutKeepsSeparateListAndDetailRects() {
        MethodTreeScreen.Layout wide = MethodTreeScreen.calculateLayout(854, 480);
        assertTrue(wide.wide());
        assertTrue(wide.list().width() > 0 && wide.detail().width() > 0);
        assertTrue(wide.list().right() <= wide.detail().x());
        assertEquals(wide.list().y(), wide.detail().y());
    }

    @Test
    void techniqueEditLearnedScrollAndDragBindContract() {
        assertEquals(0, TechniqueEditScreen.maxLearnedScroll(5, 8));
        assertEquals(7, TechniqueEditScreen.maxLearnedScroll(15, 8));
        assertEquals(0, TechniqueEditScreen.scrollLearnedBy(0, -1, 15, 8));
        assertEquals(1, TechniqueEditScreen.scrollLearnedBy(0, 1, 15, 8));
        assertEquals(7, TechniqueEditScreen.scrollLearnedBy(7, 1, 15, 8));

        assertTrue(TechniqueEditScreen.shouldBindOnRelease(3, "fireball"));
        assertFalse(TechniqueEditScreen.shouldBindOnRelease(-1, "fireball"));
        assertFalse(TechniqueEditScreen.shouldBindOnRelease(0, ""));
        assertFalse(TechniqueEditScreen.shouldBindOnRelease(2, null));
    }

    @Test
    void techniqueEditWideLayoutKeepsSlotAndLearnedPanesSeparate() {
        TechniqueEditScreen.Layout wide = TechniqueEditScreen.calculateLayout(854, 480);
        assertTrue(wide.wide());
        assertTrue(wide.slotPane().right() <= wide.learnedPane().x());
        assertTrue(wide.slotPane().height() > 0 && wide.learnedPane().height() > 0);
    }
}
