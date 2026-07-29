package com.xunxian.seekingimmortals.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScrollableListPanelInteractionTest {
    @Test
    void rowClickIsCommittedOnlyOnReleaseInTheSameRow() {
        ScrollableListPanel panel = rowPanel(8);
        int rowY = panel.rowBounds(1).y() + 3;

        assertTrue(panel.mouseClicked(30, rowY, 0));
        assertEquals(ScrollableListPanel.PointerState.PENDING_ROW, panel.pointerState());
        ScrollableListPanel.ReleaseResult release = panel.mouseReleasedResult(30, rowY, 0);

        assertTrue(release.consumed());
        assertEquals(1, release.clickedRow());
        assertEquals(ScrollableListPanel.PointerState.IDLE, panel.pointerState());

        assertTrue(panel.mouseClicked(30, rowY, 0));
        ScrollableListPanel.ReleaseResult movedRelease = panel.mouseReleasedResult(30, rowY + 3, 0);
        assertTrue(movedRelease.consumed());
        assertEquals(1, movedRelease.clickedRow());

        assertTrue(panel.mouseClicked(30, rowY, 0));
        ScrollableListPanel.ReleaseResult differentRow = panel.mouseReleasedResult(30,
                panel.rowBounds(2).y() + 2, 0);
        assertTrue(differentRow.consumed());
        assertEquals(-1, differentRow.clickedRow());
    }

    @Test
    void fourPixelMovementPromotesPendingRowToContentDrag() {
        ScrollableListPanel panel = rowPanel(20);
        int startY = panel.rowBounds(0).y() + 3;

        assertTrue(panel.mouseClicked(30, startY, 0));
        assertTrue(panel.mouseDragged(30, startY - 3, 0, 0, -3));
        assertEquals(ScrollableListPanel.PointerState.PENDING_ROW, panel.pointerState());
        assertTrue(panel.mouseDragged(30, startY - 4, 0, 0, -1));
        assertEquals(ScrollableListPanel.PointerState.DRAG_CONTENT, panel.pointerState());
        assertEquals(-1, panel.mouseReleasedResult(30, startY - 4, 0).clickedRow());

        assertTrue(panel.mouseClicked(30, startY, 0));
        assertTrue(panel.mouseDragged(30, startY - 30, 0, 0, -30));
        int firstAfterDrag = panel.firstVisibleRow();
        assertTrue(firstAfterDrag > 0);
        assertEquals(-1, panel.mouseReleasedResult(30, startY - 30, 0).clickedRow());
    }

    @Test
    void contentInsetsAndRowGapsAreNotRows() {
        ScrollableListPanel panel = rowPanel(8);
        UiRect first = panel.rowBounds(0);

        assertEquals(14, first.x());
        assertEquals(15, first.y());
        assertEquals(-1, panel.hoveredRow(13, first.y() + 2, 8));
        assertEquals(-1, panel.hoveredRow(first.x() + 2, first.bottom() + 1, 8));
        assertEquals(1, panel.hoveredRow(first.x() + 2, first.bottom() + 2, 8));
        assertEquals(-1, panel.hoveredRow(first.x() + 2, panel.y() + panel.height() - 2, 8));
    }

    @Test
    void nonDivisibleViewportCanReachTheLastRowWithoutSelectingTrailingBlankSpace() {
        ScrollableListPanel panel = rowPanel(5);
        panel.setScrollRows(panel.maxRowScroll());

        assertEquals(1, panel.maxRowScroll());
        int last = panel.rowBounds(panel.visibleRowCount() - 1).y();
        assertEquals(3, panel.hoveredRow(30, last + 2, 5));
        assertEquals(4, panel.firstVisibleRow() + panel.hoveredRow(30, last + 2, 5));
        assertEquals(-1, panel.hoveredRow(30, last + panel.rowHeight() + 1, 5));
    }

    @Test
    void thumbDragUsesTrackGeometryAndReachesTheLastRow() {
        ScrollableListPanel panel = rowPanel(24).setBounds(10, 10, 100, 100);
        UiRect track = panel.scrollbarTrackBounds();
        UiRect thumb = panel.scrollbarThumbBounds();
        assertTrue(track.width() > 0);
        assertTrue(thumb.height() > 0);

        double thumbX = track.x() + 0.5D;
        double thumbY = thumb.y() + Math.max(0.0D, thumb.height() / 2.0D);
        assertTrue(panel.mouseClicked(thumbX, thumbY, 0));
        assertEquals(ScrollableListPanel.PointerState.PENDING_TRACK, panel.pointerState());
        assertTrue(panel.mouseDragged(thumbX, track.bottom() + 20, 0, 0, 20));
        assertEquals(ScrollableListPanel.PointerState.DRAG_THUMB, panel.pointerState());
        assertEquals(panel.maxRowScroll(), panel.firstVisibleRow());
        assertTrue(panel.mouseReleasedResult(thumbX, track.bottom() + 20, 0).consumed());
    }

    @Test
    void clickingTrackPagesWithoutProducingAHiddenRowClick() {
        ScrollableListPanel panel = rowPanel(24).setBounds(10, 10, 100, 100);
        UiRect track = panel.scrollbarTrackBounds();
        UiRect thumb = panel.scrollbarThumbBounds();
        double y = thumb.bottom() + 1;

        assertTrue(track.contains(track.x() + 0.5D, y));
        assertTrue(panel.mouseClicked(track.x() + 0.5D, y, 0));
        assertEquals(ScrollableListPanel.PointerState.PENDING_TRACK, panel.pointerState());
        assertTrue(panel.firstVisibleRow() > 0);
        ScrollableListPanel.ReleaseResult release = panel.mouseReleasedResult(track.x() + 0.5D, y, 0);
        assertTrue(release.consumed());
        assertFalse(release.hasRowClick());
    }

    @Test
    void wheelCancelsPendingClickAndNonLeftButtonsAreIgnored() {
        ScrollableListPanel panel = rowPanel(20);
        int rowY = panel.rowBounds(0).y() + 3;
        assertFalse(panel.mouseClicked(30, rowY, 1));
        assertEquals(ScrollableListPanel.PointerState.IDLE, panel.pointerState());
        assertFalse(panel.mouseReleasedResult(30, rowY, 1).consumed());

        assertTrue(panel.mouseClicked(30, rowY, 0));
        assertTrue(panel.mouseScrolled(30, rowY, -1));
        assertEquals(ScrollableListPanel.PointerState.IDLE, panel.pointerState());
        assertFalse(panel.mouseReleasedResult(30, rowY, 0).consumed());
    }

    @Test
    void continuousContentDragRetainsPixelPrecision() {
        ScrollableListPanel panel = new ScrollableListPanel()
                .setBounds(0, 0, 100, 80)
                .setContentHeight(300);
        assertTrue(panel.mouseClicked(40, 30, 0));
        assertTrue(panel.mouseDragged(40, 10, 0, 0, -20));
        assertEquals(ScrollableListPanel.PointerState.DRAG_CONTENT, panel.pointerState());
        assertEquals(20, panel.scrollOffset());
        assertNotEquals(0, panel.scrollOffset());
        assertTrue(panel.mouseReleasedResult(40, 10, 0).consumed());
    }

    private static ScrollableListPanel rowPanel(int rows) {
        ScrollableListPanel panel = new ScrollableListPanel()
                .setBounds(10, 10, 100, 60)
                .setContentInsets(4, 5, 6, 5)
                .setRowMetrics(10, 2);
        panel.setContentRows(rows);
        return panel;
    }
}
