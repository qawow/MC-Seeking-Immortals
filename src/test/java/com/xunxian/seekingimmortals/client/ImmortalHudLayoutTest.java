package com.xunxian.seekingimmortals.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImmortalHudLayoutTest {
    @Test
    void widthsBelowThreeHundredUseSeparatedRailLayout() {
        for (int width : new int[]{240, 260, 289, 299}) {
            ImmortalHudLayout.Layout layout = ImmortalHudLayout.calculate(width, 180);
            assertTrue(layout.railMode(), "rail layout expected at width " + width);
            assertTrue(layout.allInside(), "HUD must stay inside at width " + width);
            assertTrue(layout.panelsSeparated(), "HUD panels must not overlap at width " + width);
        }
    }

    @Test
    void widthThreeHundredCanUseRegularLayout() {
        ImmortalHudLayout.Layout layout = ImmortalHudLayout.calculate(300, 180);

        assertFalse(layout.railMode());
        assertTrue(layout.allInside());
        assertTrue(layout.panelsSeparated());
    }
}
