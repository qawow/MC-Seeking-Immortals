package com.xunxian.seekingimmortals.client;

import com.xunxian.seekingimmortals.worldpack.WorldpackGameplayService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScreenLayoutTest {
    @Test
    void sectScreenPanelFitsNarrowScreens() {
        assertPanelFits(120, 90, SectScreen.calculatePanelWidth(120), SectScreen.calculatePanelHeight(90));
        assertPanelFits(200, 140, SectScreen.calculatePanelWidth(200), SectScreen.calculatePanelHeight(140));
        assertPanelFits(320, 180, SectScreen.calculatePanelWidth(320), SectScreen.calculatePanelHeight(180));
    }

    @Test
    void sectShopRowsStayInsidePanelReserve() {
        assertShopRowsFit(SectScreen.calculatePanelWidth(200), SectScreen.calculatePanelHeight(140));
        assertShopRowsFit(SectScreen.calculatePanelWidth(360), SectScreen.calculatePanelHeight(236));
    }

    @Test
    void techniqueEditorPanelFitsNarrowScreens() {
        assertPanelFits(120, 90, TechniqueEditScreen.calculatePanelWidth(120), TechniqueEditScreen.calculatePanelHeight(90));
        assertPanelFits(220, 150, TechniqueEditScreen.calculatePanelWidth(220), TechniqueEditScreen.calculatePanelHeight(150));
        assertPanelFits(420, 260, TechniqueEditScreen.calculatePanelWidth(420), TechniqueEditScreen.calculatePanelHeight(260));
    }

    @Test
    void techniqueSkillBarAnchorsLeftAndFitsCommonScaledHudSizes() {
        assertSkillBarFits(180, 180);
        assertSkillBarFits(320, 180);
        assertSkillBarFits(854, 480);
    }

    @Test
    void cultivationHudLeavesRoomForLeftSkillBar() {
        assertCultivationHudAvoidsSkillBar(180, 180);
        assertCultivationHudAvoidsSkillBar(320, 180);
        assertCultivationHudAvoidsSkillBar(854, 480);
    }

    @Test
    void techniqueSkillBarAvoidsTopLeftHealthWhenHeightAllows() {
        assertSkillBarAvoidsHealth(320, 240);
        assertSkillBarAvoidsHealth(320, 320);
        assertSkillBarAvoidsHealth(854, 480);
    }

    @Test
    void techniqueTooltipOpensRightAndClampsInsideScreen() {
        int barX = TechniqueSkillBarOverlay.calculateBarX(320);
        int wideTooltipX = TechniqueSkillBarOverlay.calculateTooltipX(barX, 120, 320);
        assertTrue(wideTooltipX >= barX + TechniqueSkillBarOverlay.totalBarWidth(),
                "tooltip should open to the right of the left skill bar when space allows");
        assertTrue(wideTooltipX + 120 <= 320, "wide tooltip must stay inside screen width");

        int narrowTooltipX = TechniqueSkillBarOverlay.calculateTooltipX(barX, 80, 90);
        assertTrue(narrowTooltipX >= 0, "narrow tooltip x must be non-negative");
        assertTrue(narrowTooltipX + 80 <= 90, "narrow tooltip must clamp inside screen width");
    }

    @Test
    void cultivationHealthBarFitsTopLeft() {
        assertHealthBarFits(90, 50);
        assertHealthBarFits(180, 90);
        assertHealthBarFits(854, 480);
    }

    @Test
    void cultivationHealthReplacementRequiresRenderableHudState() {
        assertTrue(CultivationHealthOverlay.shouldReplaceVanillaPlayerHealth(false, true, false, true),
                "custom health should replace vanilla hearts only when it will render");
        assertFalse(CultivationHealthOverlay.shouldReplaceVanillaPlayerHealth(true, true, false, true),
                "F1 or hidden GUI must keep vanilla cancellation disabled");
        assertFalse(CultivationHealthOverlay.shouldReplaceVanillaPlayerHealth(false, false, false, true),
                "missing player must keep vanilla cancellation disabled");
        assertFalse(CultivationHealthOverlay.shouldReplaceVanillaPlayerHealth(false, true, true, true),
                "open chat/screens must keep vanilla cancellation disabled");
        assertFalse(CultivationHealthOverlay.shouldReplaceVanillaPlayerHealth(false, true, false, false),
                "non-survival HUD state must keep vanilla cancellation disabled");
    }

    @Test
    void cultivationStatsPanelFitsScaledSizes() {
        assertCultivationStatsLayoutFits(120, 90);
        assertCultivationStatsLayoutFits(180, 120);
        assertCultivationStatsLayoutFits(320, 180);
        assertCultivationStatsLayoutFits(854, 480);
    }

    @Test
    void cultivationStatsControlsStayInsideAndSeparate() {
        assertCultivationStatsControlsFit(120, 90);
        assertCultivationStatsControlsFit(180, 120);
        assertCultivationStatsControlsFit(320, 180);
        assertCultivationStatsControlsFit(854, 480);
    }

    @Test
    void cultivationStatsColumnsCollapseForNarrowScreens() {
        assertFalse(CultivationStatsScreen.calculateLayout(120, 90).twoColumns(),
                "very narrow cultivation stats screens should use one column");
        assertFalse(CultivationStatsScreen.calculateLayout(320, 180).twoColumns(),
                "small scaled cultivation stats screens should use one column");
        assertTrue(CultivationStatsScreen.calculateLayout(854, 480).twoColumns(),
                "wide cultivation stats screens should use two columns");
    }

    @Test
    void cultivationStatsReturnTargetMatchesEntryPoint() {
        assertFalse(new CultivationStatsScreen(null, false).returnsToInventory(),
                "shortcut-opened cultivation stats should close back to the game");
        assertTrue(new CultivationStatsScreen(null, true).returnsToInventory(),
                "inventory-opened cultivation stats should return to inventory");
    }

    @Test
    void worldpackEffectTokensHaveDisplayKeysAndUnknownsFallBack() {
        assertEquals("screen.seeking_immortals.worldpack.effect.aura_plus_5",
                WorldpackScreen.effectDescriptionKey(WorldpackGameplayService.EFFECT_AURA_PLUS_5));
        assertEquals("screen.seeking_immortals.worldpack.effect.spirit_rain_bonus",
                WorldpackScreen.effectDescriptionKey(WorldpackGameplayService.EFFECT_SPIRIT_RAIN_BONUS));
        assertEquals("screen.seeking_immortals.worldpack.effect.herb_shop_bonus",
                WorldpackScreen.effectDescriptionKey(WorldpackGameplayService.EFFECT_HERB_SHOP_BONUS));
        assertEquals("screen.seeking_immortals.worldpack.effect.trade_risk_up",
                WorldpackScreen.effectDescriptionKey(WorldpackGameplayService.EFFECT_TRADE_RISK_UP));
        assertEquals("screen.seeking_immortals.worldpack.effect.secret_realm_ticket_hint",
                WorldpackScreen.effectDescriptionKey(WorldpackGameplayService.EFFECT_SECRET_REALM_TICKET_HINT));
        assertEquals("screen.seeking_immortals.worldpack.effect.sect_contribution_bonus",
                WorldpackScreen.effectDescriptionKey(WorldpackGameplayService.EFFECT_SECT_CONTRIBUTION_BONUS));
        assertEquals("screen.seeking_immortals.worldpack.effect.rare_loot_hint",
                WorldpackScreen.effectDescriptionKey("rare_loot_hint"));
        assertEquals("", WorldpackScreen.effectDescriptionKey("future_effect"));
    }

    @Test
    void cultivationStatsStatusBarHighlightIsNotNearWhite() {
        assertFalse(isNearWhiteOverlay(CultivationStatsScreen.statusBarHighlightColor()),
                "cultivation stats bars should not use white translucent highlight blocks");
    }

    @Test
    void methodTreeDropsSecondaryHintWhenBottomControlsNeedTheSpace() {
        assertFalse(MethodTreeScreen.hasDetailRoomForSchoolHint(228, 254, 9));
        assertTrue(MethodTreeScreen.hasDetailRoomForSchoolHint(200, 254, 9));
    }

    private static void assertPanelFits(int screenWidth, int screenHeight, int panelWidth, int panelHeight) {
        assertTrue(panelWidth > 0, "panel width must stay positive");
        assertTrue(panelHeight > 0, "panel height must stay positive");
        assertTrue(panelWidth <= screenWidth, "panel width must not exceed screen width");
        assertTrue(panelHeight <= screenHeight, "panel height must not exceed screen height");
        assertTrue((screenWidth - panelWidth) / 2 >= 0, "panel left must be non-negative");
        assertTrue((screenHeight - panelHeight) / 2 >= 0, "panel top must be non-negative");
    }

    private static void assertShopRowsFit(int panelWidth, int panelHeight) {
        int offset = SectScreen.shopTopOffset(panelHeight);
        int rows = SectScreen.visibleShopRows(panelWidth, panelHeight);
        int bottomReserve = panelWidth < 300 ? 50 : 30;
        int rowsBottom = offset + 20 + rows * 22;

        assertTrue(offset >= 0, "shop top offset must be non-negative");
        assertTrue(offset < panelHeight, "shop header must start inside the panel");
        assertTrue(rowsBottom <= panelHeight - bottomReserve + 20, "shop rows must leave button reserve space");
    }

    private static void assertSkillBarFits(int screenWidth, int screenHeight) {
        int x = TechniqueSkillBarOverlay.calculateBarX(screenWidth);
        int y = TechniqueSkillBarOverlay.calculateBarY(screenHeight);
        int barWidth = TechniqueSkillBarOverlay.totalBarWidth();
        int barHeight = TechniqueSkillBarOverlay.totalBarHeight();

        assertTrue(x >= 0, "skill bar x must be non-negative");
        assertTrue(y >= 0, "skill bar y must be non-negative");
        assertTrue(x <= 8, "skill bar should stay anchored near the left edge");
        assertTrue(x + barWidth <= screenWidth, "skill bar must stay inside screen width");
        assertTrue(y + barHeight <= screenHeight, "skill bar must stay inside screen height");
    }

    private static void assertCultivationHudAvoidsSkillBar(int screenWidth, int screenHeight) {
        int panelWidth = Math.min(160, CultivationHudOverlay.availablePanelWidth(screenWidth));
        int x = CultivationHudOverlay.calculatePanelX(screenWidth, panelWidth);
        int skillX = TechniqueSkillBarOverlay.calculateBarX(screenWidth);
        int skillRightReserve = skillX + TechniqueSkillBarOverlay.totalBarWidth();
        int leftReserved = TechniqueSkillBarOverlay.leftReservedWidth();

        assertTrue(panelWidth > 0, "cultivation HUD panel width must stay positive");
        assertTrue(x >= 0, "cultivation HUD x must be non-negative");
        assertTrue(x + panelWidth <= screenWidth, "cultivation HUD must stay inside screen width");
        assertTrue(skillRightReserve <= leftReserved, "left skill bar must fit inside its reserved width");
        assertTrue(x >= leftReserved, "cultivation HUD must not overlap the left skill bar reserve");
    }

    private static void assertSkillBarAvoidsHealth(int screenWidth, int screenHeight) {
        int skillY = TechniqueSkillBarOverlay.calculateBarY(screenHeight);
        int healthBottom = CultivationHealthOverlay.calculatePanelY(screenHeight)
                + CultivationHealthOverlay.panelHeight(screenHeight);
        assertTrue(skillY >= healthBottom,
                "left skill bar should start below the top-left health panel when height allows");
    }

    private static void assertHealthBarFits(int screenWidth, int screenHeight) {
        int x = CultivationHealthOverlay.calculatePanelX(screenWidth);
        int y = CultivationHealthOverlay.calculatePanelY(screenHeight);
        int width = CultivationHealthOverlay.panelWidth(screenWidth);
        int height = CultivationHealthOverlay.panelHeight(screenHeight);

        assertTrue(width > 0, "health HUD width must stay positive");
        assertTrue(height > 0, "health HUD height must stay positive");
        assertTrue(x >= 0, "health HUD x must be non-negative");
        assertTrue(y >= 0, "health HUD y must be non-negative");
        assertTrue(x + width <= screenWidth, "health HUD must stay inside screen width");
        assertTrue(y + height <= screenHeight, "health HUD must stay inside screen height");
    }

    private static void assertCultivationStatsLayoutFits(int screenWidth, int screenHeight) {
        CultivationStatsScreen.PanelLayout layout = CultivationStatsScreen.calculateLayout(screenWidth, screenHeight);

        assertPanelFits(screenWidth, screenHeight, layout.panelWidth(), layout.panelHeight());
        assertTrue(layout.contentTop() >= layout.top(), "stats content must start inside the panel");
        assertTrue(layout.contentBottom() <= layout.slider().y(), "stats content must leave room for the slider");
        assertTrue(layout.leftColumnWidth() > 0, "left stats column width must stay positive");
        assertTrue(layout.rightColumnWidth() > 0, "right stats column width must stay positive");
        if (layout.twoColumns()) {
            assertTrue(layout.rightColumnX() >= layout.leftColumnX() + layout.leftColumnWidth(),
                    "two-column stats layout must separate the right column");
        } else {
            assertTrue(layout.rightColumnX() == layout.leftColumnX(),
                    "single-column stats layout should reuse the left column origin");
        }
    }

    private static void assertCultivationStatsControlsFit(int screenWidth, int screenHeight) {
        CultivationStatsScreen.PanelLayout layout = CultivationStatsScreen.calculateLayout(screenWidth, screenHeight);

        assertRectInside(screenWidth, screenHeight, layout.breakthroughButton(), "breakthrough button");
        assertRectInside(screenWidth, screenHeight, layout.methodTreeButton(), "method tree button");
        assertRectInside(screenWidth, screenHeight, layout.closeButton(), "close button");
        assertRectInside(screenWidth, screenHeight, layout.slider(), "movement speed slider");
        assertFalse(layout.breakthroughButton().intersects(layout.closeButton()),
                "cultivation stats buttons must not overlap");
        assertFalse(layout.breakthroughButton().intersects(layout.methodTreeButton()),
                "breakthrough button must not overlap method tree button");
        assertFalse(layout.methodTreeButton().intersects(layout.closeButton()),
                "method tree button must not overlap close button");
        assertFalse(layout.slider().intersects(layout.breakthroughButton()),
                "movement speed slider must not overlap the breakthrough button");
        assertFalse(layout.slider().intersects(layout.methodTreeButton()),
                "movement speed slider must not overlap the method tree button");
        assertFalse(layout.slider().intersects(layout.closeButton()),
                "movement speed slider must not overlap the close button");
    }

    private static void assertRectInside(int screenWidth, int screenHeight, CultivationStatsScreen.UiRect rect, String name) {
        assertTrue(rect.width() > 0, name + " width must stay positive");
        assertTrue(rect.height() > 0, name + " height must stay positive");
        assertTrue(rect.x() >= 0, name + " x must be non-negative");
        assertTrue(rect.y() >= 0, name + " y must be non-negative");
        assertTrue(rect.right() <= screenWidth, name + " must stay inside screen width");
        assertTrue(rect.bottom() <= screenHeight, name + " must stay inside screen height");
    }

    private static boolean isNearWhiteOverlay(int color) {
        int alpha = color >>> 24;
        int red = (color >>> 16) & 0xFF;
        int green = (color >>> 8) & 0xFF;
        int blue = color & 0xFF;
        return alpha > 0 && red >= 220 && green >= 220 && blue >= 220;
    }
}
