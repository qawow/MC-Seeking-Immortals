package com.xunxian.seekingimmortals.client;

import com.xunxian.seekingimmortals.beast.BeastBestiaryService;
import com.xunxian.seekingimmortals.worldpack.WorldpackGameplayService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScreenLayoutTest {
    @Test
    void techniqueEditorPanelFitsNarrowScreens() {
        assertPanelFits(120, 90, TechniqueEditScreen.calculatePanelWidth(120), TechniqueEditScreen.calculatePanelHeight(90));
        assertPanelFits(220, 150, TechniqueEditScreen.calculatePanelWidth(220), TechniqueEditScreen.calculatePanelHeight(150));
        assertPanelFits(420, 260, TechniqueEditScreen.calculatePanelWidth(420), TechniqueEditScreen.calculatePanelHeight(260));
    }

    @Test
    void cultivationCoreScreensFitAndKeepScrollableContent() {
        assertSkillTreeLayoutFits(120, 90);
        assertSkillTreeLayoutFits(320, 180);
        assertSkillTreeLayoutFits(854, 480);

        assertTrue(LifeSkillTreeScreen.calculateContentHeight(false)
                        >= LifeSkillTreeScreen.calculateContentHeight(true),
                "single-column skill content should be at least as tall as two-column content");
        assertEquals(418, LifeSkillTreeScreen.calculateContentHeight(true));
        assertEquals(662, LifeSkillTreeScreen.calculateContentHeight(false));
        assertEquals(0, LifeSkillTreeScreen.clampScroll(-10, 200, 80));
        assertEquals(120, LifeSkillTreeScreen.clampScroll(999, 200, 80));
    }

    @Test
    void sharedCultivationHudLayoutStaysInsideAndSeparated() {
        assertSharedHudLayout(90, 50);
        assertSharedHudLayout(180, 90);
        assertSharedHudLayout(320, 180);
        assertSharedHudLayout(854, 480);
    }

    @Test
    void methodAndTechniqueWorkspacesUseResponsiveGeometry() {
        assertMethodTreeLayout(120, 90);
        assertMethodTreeLayout(320, 180);
        assertMethodTreeLayout(854, 480);
        assertTechniqueEditorLayout(120, 90);
        assertTechniqueEditorLayout(320, 180);
        assertTechniqueEditorLayout(854, 480);
    }

    @Test
    void inventoryCultivationEntryStaysOnScreen() {
        for (int[] size : new int[][]{{120, 90}, {320, 180}, {854, 480}}) {
            ClientEvents.InventoryEntryLayout layout = ClientEvents.inventoryEntryLayout(size[0], size[1]);
            assertTrue(layout.x() >= 0 && layout.y() >= 0);
            assertTrue(layout.width() > 0 && layout.height() > 0);
            assertTrue(layout.right() <= size[0]);
            assertTrue(layout.bottom() <= size[1]);
        }
    }

    @Test
    void inventoryCultivationEntryFollowsVanillaGuiOrigin() {
        ClientEvents.InventoryEntryLayout first = ClientEvents.inventoryEntryLayout(220, 150, 854, 480);
        ClientEvents.InventoryEntryLayout shifted = ClientEvents.inventoryEntryLayout(300, 175, 854, 480);
        assertEquals(220, first.x());
        assertEquals(130, first.y());
        assertEquals(300, shifted.x());
        assertEquals(155, shifted.y());
    }

    @Test
    void operationalJournalScreensFitScaledWindows() {
        int[][] sizes = { {120, 90}, {320, 180}, {854, 480} };
        for (int[] size : sizes) {
            assertOperationalLayouts(size[0], size[1]);
        }
    }

    @Test
    void questTrackerPointsAtTheNextMilestoneAndCapsTheFinale() {
        ClientQuestTrackerData.ChainLine available = new ClientQuestTrackerData.ChainLine(
                "qixuan_mortal_path", 0, 4, false, "neutral", "-", 0, 0, false, false);
        ClientQuestTrackerData.ChainLine active = new ClientQuestTrackerData.ChainLine(
                "qixuan_mortal_path", 1, 4, false, "neutral", "-", 0, 0, false, false);
        ClientQuestTrackerData.ChainLine finale = new ClientQuestTrackerData.ChainLine(
                "qixuan_mortal_path", 3, 4, false, "neutral", "-", 0, 0, false, false);
        ClientQuestTrackerData.ChainLine done = new ClientQuestTrackerData.ChainLine(
                "qixuan_mortal_path", 4, 4, true, "neutral", "-", 0, 0, false, true);

        assertEquals(1, QuestTrackerScreen.objectiveStage(available));
        assertEquals(2, QuestTrackerScreen.objectiveStage(active));
        assertEquals(4, QuestTrackerScreen.objectiveStage(finale));
        assertEquals(4, QuestTrackerScreen.objectiveStage(done));
    }

    @Test
    void loreJournalScreensFitAndSwitchAtPanelWidthBreakpoints() {
        int[][] sizes = {
                {120, 90}, {287, 180}, {288, 180}, {320, 180},
                {387, 220}, {388, 220}, {854, 480}
        };
        for (int[] size : sizes) {
            assertBestiaryLayout(size[0], size[1]);
            assertChronicleLayout(size[0], size[1]);
            assertCompendiumLayout(size[0], size[1]);
        }
    }

    @Test
    void bestiarySelectionTracksCanonicalIdAcrossReordering() {
        List<BeastBestiaryService.BeastEntry> entries =
                BeastBestiaryService.all().values().stream().limit(2).toList();
        assertEquals(2, entries.size());
        String selectedId = entries.get(0).id();

        assertEquals(0, BestiaryScreen.findSelectedIndex(entries, selectedId));
        assertEquals(1, BestiaryScreen.findSelectedIndex(
                List.of(entries.get(1), entries.get(0)), selectedId));
    }

    @Test
    void visualCompendiumRefreshTargetsVisualPage() {
        assertEquals("visual", LoreCompendiumScreen.actionForTab(LoreCompendiumScreen.Tab.VISUAL));
        assertEquals("compendium", LoreCompendiumScreen.actionForTab(LoreCompendiumScreen.Tab.HUB));
    }

    @Test
    void fixedContainersPreserveSlotPlaneAndExposeVisibleFrame() {
        int[][] sizes = { {120, 90}, {320, 180}, {854, 480} };
        for (int[] size : sizes) {
            AlchemyFurnaceScreen.FurnaceLayout furnace = AlchemyFurnaceScreen.calculateLayout(size[0], size[1]);
            assertTrue(furnace.visiblePanel().x() >= 0 && furnace.visiblePanel().y() >= 0);
            assertTrue(furnace.visiblePanel().right() <= size[0]);
            assertTrue(furnace.visiblePanel().bottom() <= size[1]);

            StorageBraceletScreenMenu.ContainerLayout storage =
                    StorageBraceletScreenMenu.calculateLayout(size[0], size[1], 27);
            assertTrue(storage.visibleFrame().inside(size[0], size[1]));
            assertEquals(176, storage.fullFrame().width());
            assertEquals(3, storage.rows());
            assertEquals(9 * 18, storage.storageSlotPlane().width());
            assertEquals(9 * 18, storage.playerSlotPlane().width());
        }
    }

    @Test
    void alchemyFurnaceProgressUsesIdleZeroAndClampedCraftingFraction() {
        assertEquals(0.0D, AlchemyFurnaceScreen.progressFraction(0, 0, false), 0.0001D);
        assertEquals(0.0D, AlchemyFurnaceScreen.progressFraction(20, 100, false), 0.0001D);
        assertEquals(0.0D, AlchemyFurnaceScreen.progressFraction(100, 100, true), 0.0001D);
        assertEquals(0.5D, AlchemyFurnaceScreen.progressFraction(50, 100, true), 0.0001D);
        assertEquals(1.0D, AlchemyFurnaceScreen.progressFraction(0, 100, true), 0.0001D);
        assertEquals(0.0D, AlchemyFurnaceScreen.progressFraction(120, 100, true), 0.0001D);
        assertEquals("screen.seeking_immortals.alchemy_menu.idle",
                AlchemyFurnaceScreen.progressTextKey(false));
        assertEquals("screen.seeking_immortals.alchemy_menu.progress",
                AlchemyFurnaceScreen.progressTextKey(true));
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
    void techniqueSkillBarStaysSeparatedFromLeftTopStatusStrip() {
        assertSkillBarAvoidsStatusStrip(320, 240);
        assertSkillBarAvoidsStatusStrip(320, 320);
        assertSkillBarAvoidsStatusStrip(854, 480);
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
        assertTrue(CultivationHealthOverlay.shouldReplaceVanillaPlayerHealth(false, true, true),
                "custom health should replace vanilla hearts whenever survival HUD would draw");
        assertFalse(CultivationHealthOverlay.shouldReplaceVanillaPlayerHealth(true, true, true),
                "F1 or hidden GUI must keep vanilla cancellation disabled");
        assertFalse(CultivationHealthOverlay.shouldReplaceVanillaPlayerHealth(false, false, true),
                "missing player must keep vanilla cancellation disabled");
        assertFalse(CultivationHealthOverlay.shouldReplaceVanillaPlayerHealth(false, true, false),
                "non-survival HUD state must keep vanilla cancellation disabled");
    }

    @Test
    void cultivationHealthIgnoresOpenScreensForReplacement() {
        // Open screens no longer participate in the pure predicate; inventory/pause/chat/full UI
        // all keep custom 气血 + vanilla heart cancel as long as survival HUD would draw.
        assertTrue(CultivationHealthOverlay.shouldReplaceVanillaPlayerHealth(false, true, true),
                "any open screen must still replace vanilla hearts under survival HUD");
        assertFalse(CultivationHealthOverlay.shouldReplaceVanillaPlayerHealth(true, true, true),
                "F1 still disables both custom health and vanilla cancellation");
        assertFalse(CultivationHealthOverlay.shouldReplaceVanillaPlayerHealth(false, true, false),
                "creative/non-survival still disables both custom health and vanilla cancellation");
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
    void cultivationStatsPortraitRailOnlyAppearsOnWideScreens() {
        assertFalse(CultivationStatsScreen.calculateLayout(120, 90).wide(),
                "very narrow cultivation journals should hide the portrait rail");
        assertFalse(CultivationStatsScreen.calculateLayout(320, 180).wide(),
                "small scaled cultivation journals should hide the portrait rail");
        assertTrue(CultivationStatsScreen.calculateLayout(854, 480).wide(),
                "wide cultivation journals should show the portrait rail");
    }

    @Test
    void cultivationStatsScrollClampsToPageBounds() {
        assertEquals(0, CultivationStatsScreen.clampScroll(-20, 400, 200));
        assertEquals(80, CultivationStatsScreen.clampScroll(80, 400, 200));
        assertEquals(200, CultivationStatsScreen.clampScroll(500, 400, 200));
        assertEquals(0, CultivationStatsScreen.clampScroll(40, 120, 200));
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
        assertEquals("screen.seeking_immortals.worldpack.effect.ferry_delayed",
                WorldpackScreen.effectDescriptionKey("ferry_delay"));
        assertEquals("screen.seeking_immortals.worldpack.effect.merit_double",
                WorldpackScreen.effectDescriptionKey("merit_mult_2"));
        assertEquals("screen.seeking_immortals.worldpack.effect.cultivation_bonus",
                WorldpackScreen.effectDescriptionKey("cultivation_speed_1.2_3day"));
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

    private static void assertSkillTreeLayoutFits(int screenWidth, int screenHeight) {
        LifeSkillTreeScreen.SkillTreeLayout layout = LifeSkillTreeScreen.calculateLayout(screenWidth, screenHeight);
        assertPanelFits(screenWidth, screenHeight, layout.panelWidth(), layout.panelHeight());
        assertTrue(layout.header().x() >= 0 && layout.header().right() <= screenWidth);
        assertTrue(layout.viewport().y() >= 0 && layout.viewport().bottom() <= screenHeight);
        assertTrue(layout.closeButton().x() >= 0 && layout.closeButton().right() <= screenWidth);
        assertFalse(layout.viewport().intersects(layout.closeButton()),
                "skill viewport must leave room for its close button");
    }

    private static void assertSharedHudLayout(int screenWidth, int screenHeight) {
        ImmortalHudLayout.Layout layout = ImmortalHudLayout.calculate(screenWidth, screenHeight);
        assertTrue(layout.allInside(), "all cultivation HUD panels must stay inside the screen");
        assertTrue(layout.panelsSeparated(), "cultivation HUD panels must not overlap");
        assertTrue(layout.bandsValid(), "health/cultivation bands must nest inside the status strip");
        assertTrue(layout.techniqueSlotSize() > 0, "technique slots must remain visible");
    }

    private static void assertMethodTreeLayout(int screenWidth, int screenHeight) {
        MethodTreeScreen.Layout layout = MethodTreeScreen.calculateLayout(screenWidth, screenHeight);
        assertPanelFits(screenWidth, screenHeight, layout.panelWidth(), layout.panelHeight());
        assertMethodRectInside(screenWidth, screenHeight, layout.header());
        assertMethodRectInside(screenWidth, screenHeight, layout.list());
        assertMethodRectInside(screenWidth, screenHeight, layout.detail());
        assertMethodRectInside(screenWidth, screenHeight, layout.cultivateButton());
        assertMethodRectInside(screenWidth, screenHeight, layout.prevSchoolButton());
        assertMethodRectInside(screenWidth, screenHeight, layout.nextSchoolButton());
        assertMethodRectInside(screenWidth, screenHeight, layout.resetLayoutButton());
        assertMethodRectInside(screenWidth, screenHeight, layout.doneButton());
        assertTrue(layout.list().bottom() <= layout.detail().y()
                        || layout.list().right() <= layout.detail().x(),
                "method list and detail panes must not overlap");
        MethodTreeScreen.Rect[] actions = {
                layout.cultivateButton(), layout.prevSchoolButton(), layout.nextSchoolButton(),
                layout.resetLayoutButton(), layout.doneButton()
        };
        for (int i = 0; i < actions.length; i++) {
            for (int j = i + 1; j < actions.length; j++) {
                assertFalse(intersects(actions[i], actions[j]), "method tree actions must not overlap");
            }
        }
        int firstActionY = java.util.Arrays.stream(actions).mapToInt(MethodTreeScreen.Rect::y).min().orElse(0);
        assertTrue(layout.list().bottom() <= firstActionY && layout.detail().bottom() <= firstActionY,
                "method panes must leave room for every footer action row");
    }

    private static boolean intersects(MethodTreeScreen.Rect first, MethodTreeScreen.Rect second) {
        return first.x() < second.right() && first.right() > second.x()
                && first.y() < second.bottom() && first.bottom() > second.y();
    }

    private static void assertTechniqueEditorLayout(int screenWidth, int screenHeight) {
        TechniqueEditScreen.Layout layout = TechniqueEditScreen.calculateLayout(screenWidth, screenHeight);
        assertPanelFits(screenWidth, screenHeight, layout.panelWidth(), layout.panelHeight());
        assertTechniqueRectInside(screenWidth, screenHeight, layout.header());
        assertTechniqueRectInside(screenWidth, screenHeight, layout.slotPane());
        assertTechniqueRectInside(screenWidth, screenHeight, layout.learnedPane());
        assertTechniqueRectInside(screenWidth, screenHeight, layout.closeButton());
        assertTrue(layout.slotPane().bottom() <= layout.learnedPane().y()
                        || layout.slotPane().right() <= layout.learnedPane().x(),
                "technique slot and learned panes must not overlap");
        assertTrue(layout.slotSize() > 0, "technique slot size must remain positive");
        TechniqueEditScreen.Rect viewport = TechniqueEditScreen.learnedViewport(layout);
        assertTechniqueRectInside(screenWidth, screenHeight, viewport);
        if (TechniqueEditScreen.hasSearchBox(layout)) {
            TechniqueEditScreen.Rect search = TechniqueEditScreen.searchBoxRect(layout);
            assertTechniqueRectInside(screenWidth, screenHeight, search);
            assertTrue(search.bottom() <= viewport.y(),
                    "technique search field must stay above learned result rows");
        }
    }

    private static void assertMethodRectInside(int width, int height, MethodTreeScreen.Rect rect) {
        assertTrue(rect.width() > 0 && rect.height() > 0);
        assertTrue(rect.x() >= 0 && rect.y() >= 0);
        assertTrue(rect.right() <= width && rect.bottom() <= height);
    }

    private static void assertTechniqueRectInside(int width, int height, TechniqueEditScreen.Rect rect) {
        assertTrue(rect.width() > 0 && rect.height() > 0);
        assertTrue(rect.x() >= 0 && rect.y() >= 0);
        assertTrue(rect.right() <= width && rect.bottom() <= height);
    }

    private static void assertOperationalLayouts(int screenWidth, int screenHeight) {
        AlchemyStatusScreen.StatusLayout alchemy = AlchemyStatusScreen.calculateLayout(screenWidth, screenHeight);
        assertPanelFits(screenWidth, screenHeight, alchemy.panelWidth(), alchemy.panelHeight());
        assertFalse(alchemy.viewport().intersects(alchemy.closeButton()));

        AuctionHallScreen.HallLayout auctionHall = AuctionHallScreen.calculateLayout(screenWidth, screenHeight);
        assertPanelFits(screenWidth, screenHeight, auctionHall.panelWidth(), auctionHall.panelHeight());
        assertFalse(auctionHall.viewport().intersects(auctionHall.previousButton()));

        MarketHallScreen.MarketLayout market = MarketHallScreen.calculateLayout(screenWidth, screenHeight);
        assertPanelFits(screenWidth, screenHeight, market.panelWidth(), market.panelHeight());
        assertTrue(market.viewport().bottom() <= market.previousPageButton().y(),
                "market viewport must leave room for page controls");

        DialogueScreen.Layout dialogue = DialogueScreen.calculateLayout(screenWidth, screenHeight);
        assertTrue(dialogue.panel().inside(screenWidth, screenHeight));
        assertTrue(dialogue.promptViewport().inside(screenWidth, screenHeight));
        assertTrue(dialogue.close().inside(screenWidth, screenHeight));

        QuestTrackerScreen.Layout quest = QuestTrackerScreen.calculateLayout(screenWidth, screenHeight);
        assertTrue(quest.panel().inside(screenWidth, screenHeight));
        assertTrue(quest.titleBar().inside(screenWidth, screenHeight));
        assertTrue(quest.list().inside(screenWidth, screenHeight));
        assertTrue(quest.detail().inside(screenWidth, screenHeight));
        assertTrue(quest.filters().size() == 5);
        assertTrue(quest.filters().stream().allMatch(rect -> rect.inside(screenWidth, screenHeight)));
        assertEquals(6, quest.buttons().size());
        assertTrue(quest.buttons().stream().allMatch(rect -> rect.inside(screenWidth, screenHeight)));
        assertFalse(quest.list().intersects(quest.detail()),
                "quest list and detail panes must not overlap");
        for (int i = 0; i < quest.filters().size(); i++) {
            assertTrue(quest.filters().get(i).bottom() <= quest.list().y()
                            && quest.filters().get(i).bottom() <= quest.detail().y(),
                    "quest filter tabs must stay above both body panes");
            for (int j = i + 1; j < quest.filters().size(); j++) {
                assertFalse(quest.filters().get(i).intersects(quest.filters().get(j)),
                        "quest filter tabs must not overlap");
            }
        }
        for (int i = 0; i < quest.buttons().size(); i++) {
            for (int j = i + 1; j < quest.buttons().size(); j++) {
                assertFalse(quest.buttons().get(i).intersects(quest.buttons().get(j)),
                        "quest footer buttons must not overlap");
            }
        }
        assertTrue(quest.list().bottom() <= quest.buttons().get(0).y()
                        && quest.detail().bottom() <= quest.buttons().get(0).y(),
                "quest body must leave room for footer controls");

        RefinementPlanScreen.Layout refinement = RefinementPlanScreen.calculateLayout(screenWidth, screenHeight);
        assertTrue(refinement.panel().inside(screenWidth, screenHeight));
        assertTrue(refinement.viewport().inside(screenWidth, screenHeight));
        assertTrue(refinement.doneButton().inside(screenWidth, screenHeight));

        StorageBraceletScreen.Layout preview = StorageBraceletScreen.calculateLayout(screenWidth, screenHeight);
        assertTrue(preview.panel().inside(screenWidth, screenHeight));
        assertTrue(preview.viewport().inside(screenWidth, screenHeight));

        SectHallScreen.Layout sectHall = SectHallScreen.calculateLayout(screenWidth, screenHeight);
        assertPanelFits(screenWidth, screenHeight, sectHall.panelWidth(), sectHall.panelHeight());
        assertTrue(sectHall.content().width() > 0 && sectHall.content().height() > 0);

        WorldpackScreen.Layout worldpack = WorldpackScreen.calculateLayout(screenWidth, screenHeight);
        assertPanelFits(screenWidth, screenHeight, worldpack.panelWidth(), worldpack.panelHeight());
        assertTrue(worldpack.content().width() > 0 && worldpack.content().height() > 0);
    }

    private static void assertBestiaryLayout(int screenWidth, int screenHeight) {
        BestiaryScreen.Layout layout = BestiaryScreen.calculateLayout(screenWidth, screenHeight);
        assertPanelFits(screenWidth, screenHeight, layout.width(), layout.height());
        assertEquals(layout.width() < 280, layout.stacked());
        List<BestiaryScreen.Rect> rects = List.of(
                layout.list(), layout.detail(), layout.refresh(), layout.close(),
                layout.filterAll(), layout.filterUnlocked(), layout.filterLocked());
        assertTrue(rects.stream().allMatch(rect -> rect.inside(screenWidth, screenHeight)));
        assertFalse(layout.list().intersects(layout.detail()));
        assertFalse(layout.refresh().intersects(layout.close()));
        assertFalse(layout.filterAll().intersects(layout.filterUnlocked()));
        assertFalse(layout.filterUnlocked().intersects(layout.filterLocked()));
        assertFalse(layout.filterAll().intersects(layout.list()));
        assertFalse(layout.filterUnlocked().intersects(layout.list()));
        assertFalse(layout.filterLocked().intersects(layout.list()));
        // At least one body line must remain visible even at 120x90.
        assertTrue(layout.list().h() >= 10, "bestiary list body too short at " + screenWidth + "x" + screenHeight);
        assertTrue(layout.detail().h() >= 10, "bestiary detail body too short at " + screenWidth + "x" + screenHeight);
        assertTrue(layout.list().w() >= 1 && layout.detail().w() >= 1);
    }

    private static void assertChronicleLayout(int screenWidth, int screenHeight) {
        ChronicleScreen.Layout layout = ChronicleScreen.calculateLayout(screenWidth, screenHeight);
        assertPanelFits(screenWidth, screenHeight, layout.width(), layout.height());
        assertEquals(layout.width() < 280, layout.stacked());
        List<ChronicleScreen.Rect> rects = List.of(
                layout.list(), layout.detail(), layout.refresh(), layout.close(),
                layout.eventsTab(), layout.timelineTab());
        assertTrue(rects.stream().allMatch(rect -> rect.inside(screenWidth, screenHeight)));
        assertFalse(layout.list().intersects(layout.detail()));
        assertFalse(layout.refresh().intersects(layout.close()));
        assertFalse(layout.eventsTab().intersects(layout.timelineTab()));
        assertFalse(layout.eventsTab().intersects(layout.list()));
        assertFalse(layout.timelineTab().intersects(layout.list()));
        assertTrue(layout.list().h() >= 10, "chronicle list body too short at " + screenWidth + "x" + screenHeight);
        assertTrue(layout.detail().h() >= 10, "chronicle detail body too short at " + screenWidth + "x" + screenHeight);
        assertTrue(layout.list().w() >= 1 && layout.detail().w() >= 1);
    }

    private static void assertCompendiumLayout(int screenWidth, int screenHeight) {
        LoreCompendiumScreen.Layout layout = LoreCompendiumScreen.calculateLayout(screenWidth, screenHeight);
        assertPanelFits(screenWidth, screenHeight, layout.width(), layout.height());
        int expectedColumns = layout.width() >= 380 ? 6 : layout.width() >= 280 ? 3 : 2;
        assertEquals(expectedColumns, layout.controlColumns());
        List<LoreCompendiumScreen.Rect> controls = List.of(
                layout.hubTab(), layout.glossaryTab(), layout.numericTab(), layout.visualTab(),
                layout.bestiaryBtn(), layout.chronicleBtn());
        assertTrue(controls.stream().allMatch(rect -> rect.inside(screenWidth, screenHeight)));
        for (int i = 0; i < controls.size(); i++) {
            assertFalse(controls.get(i).intersects(layout.viewport()),
                    "compendium controls must leave room for content");
            for (int j = i + 1; j < controls.size(); j++) {
                assertFalse(controls.get(i).intersects(controls.get(j)),
                        "compendium controls must not overlap");
            }
        }
        assertTrue(layout.viewport().inside(screenWidth, screenHeight));
        assertTrue(layout.refresh().inside(screenWidth, screenHeight));
        assertTrue(layout.close().inside(screenWidth, screenHeight));
        assertFalse(layout.viewport().intersects(layout.refresh()));
        assertFalse(layout.viewport().intersects(layout.close()));
        assertFalse(layout.refresh().intersects(layout.close()));
        assertTrue(layout.viewport().h() >= 10,
                "compendium viewport body too short at " + screenWidth + "x" + screenHeight);
        assertTrue(layout.viewport().w() >= 1);
    }

    private static void assertSkillBarFits(int screenWidth, int screenHeight) {
        ImmortalHudLayout.Layout layout = ImmortalHudLayout.calculate(screenWidth, screenHeight);
        ImmortalHudLayout.Rect techniques = layout.techniques();

        assertTrue(techniques.x() >= 0, "skill bar x must be non-negative");
        assertTrue(techniques.y() >= 0, "skill bar y must be non-negative");
        assertTrue(techniques.x() <= layout.margin() + 2,
                "skill bar should stay anchored near the left edge");
        assertTrue(techniques.right() <= screenWidth, "skill bar must stay inside screen width");
        assertTrue(techniques.bottom() <= screenHeight, "skill bar must stay inside screen height");
        if (!layout.railMode() && screenHeight >= 180) {
            assertTrue(techniques.y() >= layout.statusStrip().bottom(),
                    "regular skill bar should sit under the left-top status strip");
        }
    }

    private static void assertCultivationHudAvoidsSkillBar(int screenWidth, int screenHeight) {
        ImmortalHudLayout.Layout layout = ImmortalHudLayout.calculate(screenWidth, screenHeight);
        ImmortalHudLayout.Rect strip = layout.statusStrip();
        ImmortalHudLayout.Rect techniques = layout.techniques();

        assertTrue(strip.width() > 0, "status strip width must stay positive");
        assertTrue(strip.x() >= 0, "status strip x must be non-negative");
        assertTrue(strip.right() <= screenWidth, "status strip must stay inside screen width");
        assertFalse(strip.intersects(techniques),
                "merged status strip must not overlap the left skill rail");
        assertTrue(layout.panelsSeparated(),
                "status strip and skill rail must stay separated");
        if (!layout.railMode()) {
            assertTrue(techniques.y() >= strip.bottom() || strip.x() >= techniques.right(),
                    "regular layout stacks skill rail under the left-top strip or keeps them x-separated");
        }
    }

    private static void assertSkillBarAvoidsStatusStrip(int screenWidth, int screenHeight) {
        ImmortalHudLayout.Layout layout = ImmortalHudLayout.calculate(screenWidth, screenHeight);
        assertFalse(layout.techniques().intersects(layout.statusStrip()),
                "left skill rail must not overlap the left-top status strip");
        assertTrue(layout.bandsValid(), "nested health/cultivation bands must stay valid");
        if (!layout.railMode()) {
            assertTrue(layout.statusStrip().x() <= layout.margin() + 2,
                    "regular status strip should stay near the left edge");
        }
    }

    private static void assertHealthBarFits(int screenWidth, int screenHeight) {
        ImmortalHudLayout.Layout layout = ImmortalHudLayout.calculate(screenWidth, screenHeight);
        ImmortalHudLayout.Rect strip = layout.healthOnlyStrip();

        assertTrue(strip.width() > 0, "health HUD width must stay positive");
        assertTrue(strip.height() > 0, "health HUD height must stay positive");
        assertTrue(strip.x() >= 0, "health HUD x must be non-negative");
        assertTrue(strip.y() >= 0, "health HUD y must be non-negative");
        assertTrue(strip.right() <= screenWidth, "health HUD must stay inside screen width");
        assertTrue(strip.bottom() <= screenHeight, "health HUD must stay inside screen height");
        if (!layout.railMode() && screenWidth >= 180) {
            assertTrue(strip.x() <= layout.margin() + 2,
                    "health strip should stay near the left edge on regular layouts");
            assertTrue(strip.y() <= layout.margin() + 2,
                    "health strip should stay near the top edge on regular layouts");
        }
    }

    private static void assertCultivationStatsLayoutFits(int screenWidth, int screenHeight) {
        CultivationStatsScreen.PanelLayout layout = CultivationStatsScreen.calculateLayout(screenWidth, screenHeight);

        assertPanelFits(screenWidth, screenHeight, layout.panelWidth(), layout.panelHeight());
        assertRectInside(screenWidth, screenHeight, layout.header(), "journal header");
        assertRectInside(screenWidth, screenHeight, layout.content(), "journal content");
        assertRectInside(screenWidth, screenHeight, layout.foundationTab(), "foundation tab");
        assertRectInside(screenWidth, screenHeight, layout.combatTab(), "combat tab");
        assertRectInside(screenWidth, screenHeight, layout.studyTab(), "study tab");
        assertTrue(layout.foundationTab().bottom() <= layout.content().y(),
                "journal tabs must stay above the content viewport");
        assertTrue(layout.content().bottom() <= layout.breakthroughButton().y(),
                "journal content must leave room for footer actions");
        assertFalse(layout.foundationTab().intersects(layout.combatTab()),
                "foundation and combat tabs must not overlap");
        assertFalse(layout.combatTab().intersects(layout.studyTab()),
                "combat and study tabs must not overlap");
        assertTrue(layout.pageViewport(CultivationStatsScreen.StatsTab.FOUNDATION).height() > 0,
                "foundation page viewport must remain usable");
        assertTrue(layout.pageViewport(CultivationStatsScreen.StatsTab.COMBAT).height() > 0,
                "combat page viewport must remain usable");
        assertTrue(layout.pageViewport(CultivationStatsScreen.StatsTab.STUDY).height() > 0,
                "study page viewport must remain usable");
        if (layout.wide()) {
            assertRectInside(screenWidth, screenHeight, layout.profile(), "portrait rail");
            assertTrue(layout.profile().right() <= layout.content().x(),
                    "wide portrait rail must not overlap journal content");
        } else {
            assertEquals(0, layout.profile().width(),
                    "compact journals must reclaim portrait rail width");
        }
        if (layout.showsMovementSlider()) {
            assertTrue(layout.pageViewport(CultivationStatsScreen.StatsTab.COMBAT).bottom() <= layout.slider().y(),
                    "combat content must leave room for the movement slider");
        }
    }

    private static void assertCultivationStatsControlsFit(int screenWidth, int screenHeight) {
        CultivationStatsScreen.PanelLayout layout = CultivationStatsScreen.calculateLayout(screenWidth, screenHeight);

        assertRectInside(screenWidth, screenHeight, layout.breakthroughButton(), "breakthrough button");
        assertRectInside(screenWidth, screenHeight, layout.methodTreeButton(), "method tree button");
        assertRectInside(screenWidth, screenHeight, layout.skillTreeButton(), "skill tree button");
        assertRectInside(screenWidth, screenHeight, layout.closeButton(), "close button");
        if (layout.showsMovementSlider()) {
            assertRectInside(screenWidth, screenHeight, layout.slider(), "movement speed slider");
        }
        assertFalse(layout.breakthroughButton().intersects(layout.closeButton()),
                "cultivation stats buttons must not overlap");
        assertFalse(layout.breakthroughButton().intersects(layout.methodTreeButton()),
                "breakthrough button must not overlap method tree button");
        assertFalse(layout.methodTreeButton().intersects(layout.skillTreeButton()),
                "method tree button must not overlap skill tree button");
        assertFalse(layout.skillTreeButton().intersects(layout.closeButton()),
                "skill tree button must not overlap close button");
        assertFalse(layout.methodTreeButton().intersects(layout.closeButton()),
                "method tree button must not overlap close button");
        assertFalse(layout.slider().intersects(layout.breakthroughButton()),
                "movement speed slider must not overlap the breakthrough button");
        assertFalse(layout.slider().intersects(layout.methodTreeButton()),
                "movement speed slider must not overlap the method tree button");
        assertFalse(layout.slider().intersects(layout.skillTreeButton()),
                "movement speed slider must not overlap the skill tree button");
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
