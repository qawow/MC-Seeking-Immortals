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
    void methodTreeListAndDetailSupportLeftDragScrolling() {
        assertEquals(5, MethodTreeScreen.dragListScroll(3, 100, 76, 20, 8));
        assertEquals(1, MethodTreeScreen.dragListScroll(3, 100, 124, 20, 8));
        assertEquals(0, MethodTreeScreen.dragListScroll(0, 100, 180, 20, 8));
        assertEquals(12, MethodTreeScreen.dragListScroll(12, 100, 20, 20, 8));

        assertEquals(48, MethodTreeScreen.dragDetailScroll(24, 100, 76, 300, 100));
        assertEquals(0, MethodTreeScreen.dragDetailScroll(24, 100, 160, 300, 100));
        assertEquals(200, MethodTreeScreen.dragDetailScroll(190, 100, 70, 300, 100));

        assertFalse(MethodTreeScreen.crossedScrollDragThreshold(100, 97));
        assertTrue(MethodTreeScreen.crossedScrollDragThreshold(100, 96));
    }

    @Test
    void methodTreeWideLayoutKeepsSeparateListAndDetailRects() {
        MethodTreeScreen.Layout wide = MethodTreeScreen.calculateLayout(854, 480);
        assertTrue(wide.wide());
        assertTrue(wide.list().width() > 0 && wide.detail().width() > 0);
        assertTrue(wide.list().right() <= wide.detail().x());
        assertEquals(wide.list().y(), wide.detail().y());
        assertEquals(Math.max(1, wide.detail().height() - 6),
                MethodTreeScreen.detailViewportHeight(wide));
        assertEquals(306 - MethodTreeScreen.detailViewportHeight(wide),
                MethodTreeScreen.maxDetailScroll(306, MethodTreeScreen.detailViewportHeight(wide)));
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

        assertFalse(TechniqueEditScreen.crossedDragThreshold(2, 2));
        assertTrue(TechniqueEditScreen.crossedDragThreshold(4, 0));
        assertTrue(TechniqueEditScreen.shouldPanLearnedList(1, 8, true, true));
        assertFalse(TechniqueEditScreen.shouldPanLearnedList(8, 1, true, true));
        assertTrue(TechniqueEditScreen.shouldPanLearnedList(8, 1, true, false));
        assertFalse(TechniqueEditScreen.shouldPanLearnedList(1, 8, false, true));

        TechniqueEditScreen.Layout wide = TechniqueEditScreen.calculateLayout(854, 480);
        double slotX = wide.slotPane().x() + 1;
        double slotY = wide.slotPane().y() + 1;
        assertTrue(TechniqueEditScreen.shouldPromoteTechniqueDrag(wide, slotX, slotY, true, true));
        assertFalse(TechniqueEditScreen.shouldPromoteTechniqueDrag(wide, slotX, slotY, false, true));
        assertFalse(TechniqueEditScreen.shouldPromoteTechniqueDrag(
                wide, wide.learnedPane().x() + 1, wide.learnedPane().y() + 1, true, true));
        assertFalse(TechniqueEditScreen.shouldPromoteTechniqueDrag(wide, slotX, slotY, true, false));
    }

    @Test
    void compactTechniqueDragCanMoveUpFromLearnedListIntoSlots() {
        TechniqueEditScreen.Layout compact = TechniqueEditScreen.calculateLayout(320, 180);
        TechniqueEditScreen.Rect viewport = TechniqueEditScreen.learnedViewport(compact);

        assertFalse(compact.wide());
        assertTrue(compact.slotPane().bottom() <= compact.learnedPane().y());
        double distanceY = compact.slotPane().y() - viewport.y();
        assertTrue(TechniqueEditScreen.crossedDragThreshold(0, distanceY));
        assertTrue(TechniqueEditScreen.shouldPromoteTechniqueDrag(
                compact,
                compact.slotPane().x() + compact.slotPane().width() / 2.0D,
                compact.slotPane().y() + compact.slotPane().height() / 2.0D,
                true,
                true));
    }

    @Test
    void techniqueEditWideLayoutKeepsSlotAndLearnedPanesSeparate() {
        TechniqueEditScreen.Layout wide = TechniqueEditScreen.calculateLayout(854, 480);
        assertTrue(wide.wide());
        assertTrue(wide.slotPane().right() <= wide.learnedPane().x());
        assertTrue(wide.slotPane().height() > 0 && wide.learnedPane().height() > 0);
        TechniqueEditScreen.Rect search = TechniqueEditScreen.searchBoxRect(wide);
        TechniqueEditScreen.Rect viewport = TechniqueEditScreen.learnedViewport(wide);
        assertTrue(search.bottom() <= viewport.y());
    }

    @Test
    void techniqueSearchCannotRemainActiveWhenItsFieldIsHidden() {
        TechniqueEditScreen.Layout wide = TechniqueEditScreen.calculateLayout(854, 480);
        TechniqueEditScreen.Layout narrow = TechniqueEditScreen.calculateLayout(120, 90);
        assertEquals("fire", TechniqueEditScreen.searchValueForLayout(wide, "fire"));
        assertEquals("", TechniqueEditScreen.searchValueForLayout(narrow, "fire"));
        assertEquals("", TechniqueEditScreen.searchValueForLayout(wide, null));
    }

    @Test
    void cultivationDisplayTextDoesNotExposeTechniqueCodes() {
        ClientTechniqueData.TechniqueSummary missing =
                ClientTechniqueData.TechniqueSummary.fallback("raw_technique_id");
        assertFalse(CultivationDisplayTexts.techniqueName(missing).contains("raw_technique_id"));

        ClientTechniqueData.TechniqueSummary mixed = new ClientTechniqueData.TechniqueSummary(
                "elemental_burst_fire", "fire系爆发术（通称）", "elemental", "fire", 15, 100);
        assertFalse(CultivationDisplayTexts.techniqueName(mixed).contains("fire"));

        ClientTechniqueData.TechniqueSummary cloak = new ClientTechniqueData.TechniqueSummary(
                "yin_luo_ghost_cloak", "阴罗鬼cloak", "ghost", "dark", 15, 100);
        assertFalse(CultivationDisplayTexts.techniqueName(cloak).toLowerCase().contains("cloak"));

        assertEquals("", CultivationDisplayTexts.visibleSourceText("\u8865\u5168\u5360\u4f4d/\u539f\u8457\u6269\u5c55"));
        assertEquals("", CultivationDisplayTexts.visibleSourceText("\u6587\u672c\u6750\u6599\u6279\u91cf\u63a5\u7ebf"));
        assertEquals("", CultivationDisplayTexts.visibleSourceText("text_material wiring"));
    }
}
