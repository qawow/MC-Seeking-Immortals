package com.xunxian.seekingimmortals.client;

import com.xunxian.seekingimmortals.client.ui.InkScene;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 云笈墨卷 palette validator: every scene ships a complete light-paper/dark-ink
 * token set with no neon and no near-white translucent highlights.
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

    @Test
    void everySceneHasOpaqueInkTextOnLightPaperPanel() {
        for (InkScene scene : InkScene.values()) {
            UiClimate.Palette p = scene.palette();
            assertEquals(0xFF, alpha(p.paper()), scene + " ink text must be opaque");
            assertEquals(0xFF, alpha(p.border()), scene + " ink border must be opaque");
            // Panel is light paper; text token is dark ink — luminance must invert.
            int panelLum = red(p.panel()) + green(p.panel()) + blue(p.panel());
            int inkLum = red(p.paper()) + green(p.paper()) + blue(p.paper());
            assertTrue(panelLum > 480, scene + " panel should be light paper, got " + Integer.toHexString(p.panel()));
            assertTrue(inkLum < 300, scene + " text should be dark ink, got " + Integer.toHexString(p.paper()));
            assertTrue(panelLum - inkLum > 250, scene + " needs strong paper/ink contrast");
        }
    }

    @Test
    void noNeonTokensInAnyScene() {
        for (InkScene scene : InkScene.values()) {
            UiClimate.Palette p = scene.palette();
            int[] tokens = {
                    p.border(), p.borderDim(), p.panel(), p.inner(), p.header(),
                    p.accent(), p.accentText(), p.paper(), p.paperMuted(), p.spirit(),
                    p.cinnabar(), p.cinnabarBright(), p.warning(), p.cultivationFill(),
                    p.cultivationHighlight(), p.hudBorder(), p.hudBacking()
            };
            for (int token : tokens) {
                assertTrue(!isNeon(token),
                        scene + " token " + Integer.toHexString(token) + " reads as neon");
            }
        }
    }

    @Test
    void sceneAccentsStayDistinctPerScene() {
        assertNotEquals(InkScene.QUIET_STUDY.palette().accent(), InkScene.FIELD_NOTES.palette().accent());
        assertNotEquals(InkScene.FIELD_NOTES.palette().panel(), InkScene.LEDGER_HALL.palette().panel());
        assertNotEquals(InkScene.LEDGER_HALL.palette().border(), InkScene.OMEN_RED.palette().border());
        // Shared xianxia semantic colors stay identical across scenes.
        for (InkScene scene : InkScene.values()) {
            assertEquals(0xFF9E3226, scene.palette().cinnabar(), scene + " cinnabar drifted");
            assertEquals(0xFF9A7020, scene.palette().warning(), scene + " warning drifted");
        }
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
