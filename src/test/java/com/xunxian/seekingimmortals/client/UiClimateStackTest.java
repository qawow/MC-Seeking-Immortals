package com.xunxian.seekingimmortals.client;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Pure climate-stack / palette rebind / qi-dev risk token tests.
 * Does not touch GuiGraphics or Minecraft client runtime.
 */
class UiClimateStackTest {

    @AfterEach
    void resetClimateStack() {
        ImmortalUiSkin.forceResetClimateForTest();
    }

    @Test
    void defaultClimateIsBambooAndPaperMatchesPalette() {
        assertSame(UiClimate.BAMBOO_SLIP, ImmortalUiSkin.currentClimate());
        assertEquals(UiClimate.BAMBOO_SLIP.palette().paper(), ImmortalUiSkin.JOURNAL_PAPER);
        assertEquals(UiClimate.BAMBOO_SLIP.palette().border(), ImmortalUiSkin.JOURNAL_BORDER);
        assertEquals(UiClimate.BAMBOO_SLIP.palette().accentText(), ImmortalUiSkin.JOURNAL_JADE_TEXT);
    }

    @Test
    void pushJadeRebindsPaperAndBorderThenPopRestoresBamboo() {
        int bambooPaper = ImmortalUiSkin.JOURNAL_PAPER;
        int bambooBorder = ImmortalUiSkin.JOURNAL_BORDER;

        ImmortalUiSkin.pushClimate(UiClimate.JADE_SLIP);
        try {
            assertSame(UiClimate.JADE_SLIP, ImmortalUiSkin.currentClimate());
            assertEquals(UiClimate.JADE_SLIP.palette().paper(), ImmortalUiSkin.JOURNAL_PAPER);
            assertEquals(UiClimate.JADE_SLIP.palette().border(), ImmortalUiSkin.JOURNAL_BORDER);
            assertEquals(UiClimate.JADE_SLIP.palette().accentText(), ImmortalUiSkin.JOURNAL_JADE_TEXT);
            assertNotEquals(bambooPaper, ImmortalUiSkin.JOURNAL_PAPER);
            assertNotEquals(bambooBorder, ImmortalUiSkin.JOURNAL_BORDER);
        } finally {
            ImmortalUiSkin.popClimate();
        }

        assertSame(UiClimate.BAMBOO_SLIP, ImmortalUiSkin.currentClimate());
        assertEquals(bambooPaper, ImmortalUiSkin.JOURNAL_PAPER);
        assertEquals(bambooBorder, ImmortalUiSkin.JOURNAL_BORDER);
        assertEquals(0, ImmortalUiSkin.climateStackDepthForTest());
    }

    @Test
    void withClimateRestoresEvenWhenActionThrows() {
        int bambooPaper = ImmortalUiSkin.JOURNAL_PAPER;
        try {
            ImmortalUiSkin.withClimate(UiClimate.WARM_LACQUER, () -> {
                assertSame(UiClimate.WARM_LACQUER, ImmortalUiSkin.currentClimate());
                assertEquals(UiClimate.WARM_LACQUER.palette().paper(), ImmortalUiSkin.JOURNAL_PAPER);
                throw new IllegalStateException("forced");
            });
        } catch (IllegalStateException ignored) {
            // expected
        }
        assertSame(UiClimate.BAMBOO_SLIP, ImmortalUiSkin.currentClimate());
        assertEquals(bambooPaper, ImmortalUiSkin.JOURNAL_PAPER);
        assertEquals(0, ImmortalUiSkin.climateStackDepthForTest());
    }

    @Test
    void nestedPushOrderRestoresOuterClimate() {
        ImmortalUiSkin.pushClimate(UiClimate.JADE_SLIP);
        try {
            ImmortalUiSkin.pushClimate(UiClimate.CINNABAR_SEAL);
            try {
                assertSame(UiClimate.CINNABAR_SEAL, ImmortalUiSkin.currentClimate());
                assertEquals(UiClimate.CINNABAR_SEAL.palette().cinnabarBright(),
                        ImmortalUiSkin.JOURNAL_CINNABAR_BRIGHT);
                assertEquals(2, ImmortalUiSkin.climateStackDepthForTest());
            } finally {
                ImmortalUiSkin.popClimate();
            }
            assertSame(UiClimate.JADE_SLIP, ImmortalUiSkin.currentClimate());
            assertEquals(UiClimate.JADE_SLIP.palette().paper(), ImmortalUiSkin.JOURNAL_PAPER);
            assertEquals(1, ImmortalUiSkin.climateStackDepthForTest());
        } finally {
            ImmortalUiSkin.popClimate();
        }
        assertSame(UiClimate.BAMBOO_SLIP, ImmortalUiSkin.currentClimate());
        assertEquals(0, ImmortalUiSkin.climateStackDepthForTest());
    }

    @Test
    void nullClimatePushFallsBackToBamboo() {
        ImmortalUiSkin.pushClimate(null);
        try {
            assertSame(UiClimate.BAMBOO_SLIP, ImmortalUiSkin.currentClimate());
            assertEquals(1, ImmortalUiSkin.climateStackDepthForTest());
        } finally {
            ImmortalUiSkin.popClimate();
        }
        assertEquals(0, ImmortalUiSkin.climateStackDepthForTest());
    }

    @Test
    void qiDevRiskColorUsesSharedThresholds() {
        int calm = 0xFFE4E8DC;
        assertEquals(calm, ImmortalUiSkin.qiDevRiskColor(0, calm));
        assertEquals(calm, ImmortalUiSkin.qiDevRiskColor(49, calm));
        assertEquals(ImmortalUiSkin.JOURNAL_WARNING,
                ImmortalUiSkin.qiDevRiskColor(ImmortalUiSkin.QI_DEV_WARN_THRESHOLD, calm));
        assertEquals(ImmortalUiSkin.JOURNAL_WARNING, ImmortalUiSkin.qiDevRiskColor(69, calm));
        assertEquals(ImmortalUiSkin.JOURNAL_CINNABAR_BRIGHT,
                ImmortalUiSkin.qiDevRiskColor(ImmortalUiSkin.QI_DEV_DANGER_THRESHOLD, calm));
        assertEquals(ImmortalUiSkin.JOURNAL_CINNABAR_BRIGHT, ImmortalUiSkin.qiDevRiskColor(100, calm));
        assertEquals(ImmortalUiSkin.JOURNAL_PAPER, ImmortalUiSkin.qiDevRiskColor(0));
    }

    @Test
    void lacquerClimateRebindsCompatAliases() {
        ImmortalUiSkin.withClimate(UiClimate.WARM_LACQUER, () -> {
            assertEquals(UiClimate.WARM_LACQUER.palette().paper(), ImmortalUiSkin.COLOR_TEXT_NORMAL);
            assertEquals(UiClimate.WARM_LACQUER.palette().paperMuted(), ImmortalUiSkin.COLOR_TEXT_MUTED);
            assertEquals(UiClimate.WARM_LACQUER.palette().border() & 0x00FFFFFF,
                    ImmortalUiSkin.PANEL_BORDER & 0x00FFFFFF);
            assertEquals(UiClimate.WARM_LACQUER.palette().hudBorder(), ImmortalUiSkin.HUD_BORDER);
        });
        assertEquals(0, ImmortalUiSkin.climateStackDepthForTest());
    }
}
