package com.xunxian.seekingimmortals.client;

import com.xunxian.seekingimmortals.client.ui.InkLayout;
import com.xunxian.seekingimmortals.client.ui.NumberFmt;
import com.xunxian.seekingimmortals.client.ui.widget.InkPaging;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InkUiCoreTest {
    @Test
    void cjkNumberUnitsAreCanonical() {
        assertEquals("9999", NumberFmt.cjk(9_999));
        assertEquals("1万", NumberFmt.cjk(10_000));
        assertEquals("1.5万", NumberFmt.cjk(15_000));
        assertEquals("1亿", NumberFmt.cjk(100_000_000));
        assertEquals("2.5亿", NumberFmt.cjk(250_000_000));
        assertEquals("1兆", NumberFmt.cjk(1_000_000_000_000L));
        assertEquals("-1.5万", NumberFmt.cjk(-15_000));
        assertEquals("120万", NumberFmt.cjk(1_200_000));
        // No western B/M units anywhere in the register.
        assertFalse(NumberFmt.cjk(2_000_000_000L).contains("B"));
        assertFalse(NumberFmt.cjk(2_000_000L).contains("M"));
    }

    @Test
    void layoutPanelFitsAndBandsDoNotOverlap() {
        for (int[] size : new int[][]{{160, 120}, {320, 200}, {427, 240}, {854, 480}}) {
            InkLayout.Panel panel = InkLayout.panel(size[0], size[1], InkLayout.Spec.LORE);
            assertTrue(panel.outer().inside(size[0], size[1]),
                    size[0] + "x" + size[1] + " panel must fit");
            assertTrue(panel.header().bottom() <= panel.body().y());
            assertTrue(panel.body().bottom() <= panel.footer().y() + panel.footer().height() + 8);
            if (!panel.stacked() && panel.listPane().width() > 0) {
                assertFalse(panel.listPane().intersects(panel.detailPane()));
            }
        }
    }

    @Test
    void layoutStacksBelowBreakpoint() {
        InkLayout.Panel narrow = InkLayout.panel(240, 200, InkLayout.Spec.LORE);
        assertTrue(narrow.stacked());
        InkLayout.Panel wide = InkLayout.panel(500, 260, InkLayout.Spec.LORE);
        assertFalse(wide.stacked());
    }

    @Test
    void scrollClampAndPagingMatchLegacySemantics() {
        assertEquals(0, InkLayout.clampScroll(-5, 100, 50));
        assertEquals(50, InkLayout.clampScroll(99, 100, 50));
        assertEquals(0, InkLayout.clampScroll(10, 40, 50));

        assertEquals(0, InkPaging.clampPage(-3, 2));
        assertEquals(2, InkPaging.clampPage(99, 2));
        assertEquals(2, InkPaging.maxPage(13, 6));
        assertEquals(6, InkPaging.pageStart(1, 6));
        assertEquals(12, InkPaging.pageEnd(1, 13, 6));
        assertEquals(13, InkPaging.pageEnd(2, 13, 6));
        assertEquals(0, InkPaging.maxPage(0, 6));
    }
}
