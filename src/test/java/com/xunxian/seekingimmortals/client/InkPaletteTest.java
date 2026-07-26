package com.xunxian.seekingimmortals.client;

import com.xunxian.seekingimmortals.client.ui.InkScene;
import com.xunxian.seekingimmortals.client.ui.UiTheme;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Palette validator for all UI themes: every theme × scene combination
 * ships a complete, opaque, high-contrast token set with no neon and with
 * stable within-theme semantic colors (cinnabar/warning). Contrast direction
 * depends on theme brightness class:
 * <ul>
 *   <li>light paper (云笈墨卷 / 符箓黄纸 / 水墨山水 / 黄枫秋色 / 青元剑光): light panel, dark ink</li>
 *   <li>mid stone (洞府石刻): mid panel, light ink</li>
 *   <li>dark ground (玄夜星图 / 青铜鼎彝 / 掌天瓶露 / 血色禁地 / 噬金虫甲): dark panel, light ink</li>
 * </ul>
 */
class InkPaletteTest {

    private static int alpha(int argb) {
        return (argb >>> 24) & 0xFF;
    }

    private static int red(int argb) {
        return (argb >> 16) & 0xFF;
    }

    private static int green(int argb) {
        return (argb >> 8) & 0xFF;
    }

    private static int blue(int argb) {
        return argb & 0xFF;
    }

    private static int lum(int argb) {
        return red(argb) + green(argb) + blue(argb);
    }

    private static boolean isNeon(int argb) {
        int r = red(argb);
        int g = green(argb);
        int b = blue(argb);
        // Reject saturated cyan / magenta / electric green — foreign to ink-on-paper.
        boolean cyan = r < 0x40 && g > 0xC0 && b > 0xC0;
        boolean magenta = r > 0xC0 && g < 0x40 && b > 0xC0;
        boolean electricGreen = r < 0x40 && g > 0xD0 && b < 0x40;
        return cyan || magenta || electricGreen;
    }

    private static boolean isLightTheme(UiTheme theme) {
        return theme == UiTheme.INKSCROLL
                || theme == UiTheme.TALISMAN_PAPER
                || theme == UiTheme.INKWASH_LANDSCAPE
                || theme == UiTheme.MAPLE_VALLEY
                || theme == UiTheme.AZURE_SWORD;
    }

    private static boolean isMidTheme(UiTheme theme) {
        return theme == UiTheme.CAVE_INSCRIPTION;
    }

    @Test
    void everyThemeSceneHasOpaqueTextWithStrongContrast() {
        for (UiTheme theme : UiTheme.values()) {
            for (InkScene scene : InkScene.values()) {
                UiClimate.Palette p = theme.paletteFor(scene);
                String label = theme + "/" + scene;
                assertEquals(0xFF, alpha(p.paper()), label + " text must be opaque");
                assertEquals(0xFF, alpha(p.border()), label + " border must be opaque");
                int panelLum = lum(p.panel());
                int inkLum = lum(p.paper());
                if (isLightTheme(theme)) {
                    // Light paper, dark ink.
                    assertTrue(panelLum > 480, label + " panel should be light paper, got " + Integer.toHexString(p.panel()));
                    assertTrue(inkLum < 300, label + " text should be dark ink, got " + Integer.toHexString(p.paper()));
                    assertTrue(panelLum - inkLum > 250, label + " needs strong paper/ink contrast");
                } else if (isMidTheme(theme)) {
                    // Mid-tone stone, light chiseled text.
                    assertTrue(panelLum >= 100 && panelLum <= 400, label + " panel should be mid stone, got " + Integer.toHexString(p.panel()));
                    assertTrue(inkLum > 500, label + " text should be light, got " + Integer.toHexString(p.paper()));
                    assertTrue(inkLum - panelLum > 220, label + " needs strong stone/text contrast");
                } else {
                    // Dark ground, light text.
                    assertTrue(panelLum < 300, label + " panel should be dark, got " + Integer.toHexString(p.panel()));
                    assertTrue(inkLum > 480, label + " text should be light, got " + Integer.toHexString(p.paper()));
                    assertTrue(inkLum - panelLum > 250, label + " needs strong ground/text contrast");
                }
            }
        }
    }

    @Test
    void noNeonTokensInAnyThemeScene() {
        for (UiTheme theme : UiTheme.values()) {
            for (InkScene scene : InkScene.values()) {
                UiClimate.Palette p = theme.paletteFor(scene);
                int[] tokens = {
                        p.border(), p.borderDim(), p.panel(), p.inner(), p.header(),
                        p.accent(), p.accentText(), p.paper(), p.paperMuted(), p.spirit(),
                        p.cinnabar(), p.cinnabarBright(), p.warning(), p.cultivationFill(),
                        p.cultivationHighlight(), p.hudBorder(), p.hudBacking()
                };
                for (int token : tokens) {
                    assertTrue(!isNeon(token),
                            theme + "/" + scene + " token " + Integer.toHexString(token) + " reads as neon");
                }
            }
        }
    }

    @Test
    void sceneAccentsStayDistinctPerSceneWithinEveryTheme() {
        for (UiTheme theme : UiTheme.values()) {
            assertNotEquals(theme.paletteFor(InkScene.QUIET_STUDY).accent(),
                    theme.paletteFor(InkScene.FIELD_NOTES).accent(), theme + " accent not scene-distinct");
            assertNotEquals(theme.paletteFor(InkScene.FIELD_NOTES).panel(),
                    theme.paletteFor(InkScene.LEDGER_HALL).panel(), theme + " panel not scene-distinct");
            assertNotEquals(theme.paletteFor(InkScene.LEDGER_HALL).border(),
                    theme.paletteFor(InkScene.OMEN_RED).border(), theme + " border not scene-distinct");
            // Shared xianxia semantic colors stay identical across scenes within one theme.
            int cinnabar = theme.paletteFor(InkScene.FIELD_NOTES).cinnabar();
            int warning = theme.paletteFor(InkScene.FIELD_NOTES).warning();
            for (InkScene scene : InkScene.values()) {
                assertEquals(cinnabar, theme.paletteFor(scene).cinnabar(), theme + "/" + scene + " cinnabar drifted");
                assertEquals(warning, theme.paletteFor(scene).warning(), theme + "/" + scene + " warning drifted");
            }
        }
        // Baseline theme keeps its historical semantic hexes.
        assertEquals(0xFF9E3226, UiTheme.INKSCROLL.paletteFor(InkScene.FIELD_NOTES).cinnabar(), "baseline cinnabar drifted");
        assertEquals(0xFF9A7020, UiTheme.INKSCROLL.paletteFor(InkScene.FIELD_NOTES).warning(), "baseline warning drifted");
    }

    @Test
    void themeLookupAndCycleAreTotal() {
        assertSame(UiTheme.INKSCROLL, UiTheme.byId(null));
        assertSame(UiTheme.INKSCROLL, UiTheme.byId("unknown-theme"));
        assertSame(UiTheme.NIGHT_STARCHART, UiTheme.byId(" Night_Starchart "));
        UiTheme cursor = UiTheme.INKSCROLL;
        for (int i = 0; i < UiTheme.values().length; i++) {
            cursor = cursor.next();
        }
        assertSame(UiTheme.INKSCROLL, cursor, "next() must cycle through all themes back to baseline");
    }

    @Test
    void retiredClimatesMapOntoScenes() {
        assertSame(InkScene.QUIET_STUDY, InkScene.fromClimate(UiClimate.JADE_SLIP));
        assertSame(InkScene.FIELD_NOTES, InkScene.fromClimate(UiClimate.BAMBOO_SLIP));
        assertSame(InkScene.LEDGER_HALL, InkScene.fromClimate(UiClimate.WARM_LACQUER));
        assertSame(InkScene.OMEN_RED, InkScene.fromClimate(UiClimate.CINNABAR_SEAL));
        assertSame(InkScene.FIELD_NOTES, InkScene.fromClimate(null));
        assertSame(InkScene.FIELD_NOTES, InkScene.safe(null));
    }
}
