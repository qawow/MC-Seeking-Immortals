package com.xunxian.seekingimmortals.client;

import com.xunxian.seekingimmortals.menu.SectHallMenu;
import com.xunxian.seekingimmortals.network.ModNetwork;
import com.xunxian.seekingimmortals.network.SectActionPacket;
import com.xunxian.seekingimmortals.sect.SectContributionService;
import com.xunxian.seekingimmortals.util.PlayerDisplayText;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;

/**
 * Productized sect hall with dialogue, mission, shop and progress parity.
 *
 * <p>Journal shell: {@link AbstractJournalContainerScreen} + {@link TabBar} + {@link ScrollableListPanel}.
 * Tabs default to {@link Tab#MISSION}. Non-members hide tabs and show a join-candidate list.
 * Only candidates and SHOP use row scrolling; DIALOGUE/MISSION/PROGRESS are static text + footer actions.</p>
 */
public class SectHallScreen extends AbstractJournalContainerScreen<SectHallMenu> {
    private static final int MAX_PANEL_WIDTH = 360;
    private static final int MAX_PANEL_HEIGHT = 236;
    private static final int PANEL_MARGIN = 4;
    private static final int LINE = 13;
    private static final int ROW_HEIGHT = 22;

    private final ScrollableListPanel listPanel = new ScrollableListPanel();
    private final TabBar<Tab> tabBar = new TabBar<>(Tab.MISSION);

    private Tab tab = Tab.MISSION;

    public SectHallScreen(SectHallMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        // Keep the container origin contract intact even though the current menu has no slots.
        this.imageWidth = 360;
        this.imageHeight = 236;
        this.listPanel.setScrollStep(ROW_HEIGHT)
                .setRowMetrics(ROW_HEIGHT, 0)
                .setScrollbarInsetRight(3);
        this.tabBar.setOnSelect(this::setTab);
    }

    @Override
    protected void init() {
        super.init();
        rebuildActionWidgets();
    }

    // -------------------------------------------------------------------------
    // Widget rebuild (shared chrome controls + tab-specific action buttons)
    // -------------------------------------------------------------------------

    private void rebuildActionWidgets() {
        clearWidgets();
        Layout layout = calculateLayout(width, height);
        ClientSectData.Snapshot data = ClientSectData.get();
        addChromeButtons(layout);

        if (!data.synced()) {
            listPanel.resetScroll();
            return;
        }
        if (!data.member()) {
            addCandidateButtons(layout, data);
            return;
        }
        attachTabs(layout);
        switch (tab) {
            case DIALOGUE -> addDialogueButtons(layout, data);
            case MISSION -> addMissionButtons(layout);
            case SHOP -> addShopButtons(layout, data);
            case PROGRESS -> addProgressButtons(layout);
        }
    }

    private void addChromeButtons(Layout layout) {
        addRenderableWidget(ImmortalButton.secondary(layout.refreshButton().x(), layout.refreshButton().y(),
                layout.refreshButton().width(), layout.refreshButton().height(),
                Component.translatable("screen.seeking_immortals.sect.refresh"), button ->
                        ModNetwork.CHANNEL.sendToServer(new SectActionPacket(
                                SectContributionService.ACTION_OPEN, menu.focusSectId(), "",
                                menu.accessToken()))));
        addRenderableWidget(ImmortalButton.secondary(layout.closeButton().x(), layout.closeButton().y(),
                layout.closeButton().width(), layout.closeButton().height(),
                Component.translatable("screen.seeking_immortals.sect.close"), button -> onClose()));
    }

    private void attachTabs(Layout layout) {
        tabBar.clearTabs()
                .setSelected(tab)
                .addTab(Tab.DIALOGUE, Component.translatable(Tab.DIALOGUE.key), toUi(layout.dialogueTab()))
                .addTab(Tab.MISSION, Component.translatable(Tab.MISSION.key), toUi(layout.missionTab()))
                .addTab(Tab.SHOP, Component.translatable(Tab.SHOP.key), toUi(layout.shopTab()))
                .addTab(Tab.PROGRESS, Component.translatable(Tab.PROGRESS.key), toUi(layout.progressTab()));
        for (ImmortalButton button : tabBar.attach(null)) {
            addRenderableWidget(button);
        }
    }

    private void setTab(Tab target) {
        if (tab != target) {
            tab = target;
            listPanel.resetScroll();
            rebuildActionWidgets();
        }
    }

    // ---- Tab button hooks ---------------------------------------------------

    private void addCandidateButtons(Layout layout, ClientSectData.Snapshot data) {
        Rect viewport = candidateViewport(layout);
        List<ClientSectData.Candidate> candidates = data.candidates().stream()
                .filter(candidate -> menu.focusSectId().isBlank()
                        || menu.focusSectId().equals(candidate.id()))
                .toList();
        bindListViewport(viewport, layout.rowHeight(), candidates.size());
        int listScroll = listPanel.scrollRows();
        int visible = listPanel.visibleRowCount();
        for (int row = 0; row < visible && listScroll + row < candidates.size(); row++) {
            ClientSectData.Candidate candidate = candidates.get(listScroll + row);
            Rect action = rowAction(viewport, layout.rowHeight(), row);
            addRenderableWidget(ImmortalButton.primary(action.x(), action.y(), action.width(), action.height(),
                    Component.translatable("screen.seeking_immortals.sect.join"), button ->
                            ModNetwork.CHANNEL.sendToServer(new SectActionPacket(
                                    SectContributionService.ACTION_APPLY, candidate.id(), "",
                                    menu.accessToken()))));
        }
    }

    private void addDialogueButtons(Layout layout, ClientSectData.Snapshot data) {
        List<ClientSectData.DialogueOption> options = data.dialogue().options();
        int count = Math.min(3, options.size());
        List<Rect> buttons = footerButtons(layout, count);
        for (int i = 0; i < count; i++) {
            ClientSectData.DialogueOption option = options.get(i);
            Rect bounds = buttons.get(i);
            addRenderableWidget(ImmortalButton.primary(bounds.x(), bounds.y(), bounds.width(), bounds.height(),
                    PlayerDisplayText.translatedOr(option.labelKey(),
                            "screen.seeking_immortals.sect.dialogue.option.unknown"), button ->
                            ModNetwork.CHANNEL.sendToServer(new SectActionPacket(
                                    SectContributionService.ACTION_DIALOGUE, option.id(), "",
                                    menu.accessToken()))));
        }
    }

    private void addMissionButtons(Layout layout) {
        List<Rect> buttons = footerButtons(layout, 3);
        Rect accept = buttons.get(0);
        Rect turnIn = buttons.get(1);
        Rect donate = buttons.get(2);
        addRenderableWidget(ImmortalButton.primary(accept.x(), accept.y(), accept.width(), accept.height(),
                Component.translatable("screen.seeking_immortals.sect.mission.accept"), button ->
                        ModNetwork.CHANNEL.sendToServer(new SectActionPacket(
                                SectContributionService.ACTION_ACCEPT_MISSION, "", "",
                                menu.accessToken()))));
        addRenderableWidget(ImmortalButton.primary(turnIn.x(), turnIn.y(), turnIn.width(), turnIn.height(),
                Component.translatable("screen.seeking_immortals.sect.mission.turn_in"), button ->
                        ModNetwork.CHANNEL.sendToServer(new SectActionPacket(
                                SectContributionService.ACTION_TURN_IN_MISSION, "", "",
                                menu.accessToken()))));
        addRenderableWidget(ImmortalButton.primary(donate.x(), donate.y(), donate.width(), donate.height(),
                Component.translatable("screen.seeking_immortals.sect.donate"), button ->
                        ModNetwork.CHANNEL.sendToServer(new SectActionPacket(
                                SectContributionService.ACTION_DONATE_SPIRIT_GRASS, "", "",
                                menu.accessToken()))));
    }

    private void addShopButtons(Layout layout, ClientSectData.Snapshot data) {
        Rect viewport = shopViewport(layout);
        List<ClientSectData.ShopEntry> entries = data.shopEntries();
        bindListViewport(viewport, layout.rowHeight(), entries.size());
        int listScroll = listPanel.scrollRows();
        int visible = listPanel.visibleRowCount();
        for (int row = 0; row < visible && listScroll + row < entries.size(); row++) {
            ClientSectData.ShopEntry entry = entries.get(listScroll + row);
            Rect action = rowAction(viewport, layout.rowHeight(), row);
            addRenderableWidget(ImmortalButton.primary(action.x(), action.y(), action.width(), action.height(),
                    Component.translatable("screen.seeking_immortals.sect.buy"), button ->
                            ModNetwork.CHANNEL.sendToServer(new SectActionPacket(
                                    SectContributionService.ACTION_BUY, entry.id(), "",
                                    menu.accessToken()))));
        }
    }

    private void addProgressButtons(Layout layout) {
        Rect bounds = footerButtons(layout, 1).get(0);
        addRenderableWidget(ImmortalButton.primary(bounds.x(), bounds.y(), bounds.width(), bounds.height(),
                Component.translatable("screen.seeking_immortals.sect.advance"), button ->
                        ModNetwork.CHANNEL.sendToServer(new SectActionPacket(
                                SectContributionService.ACTION_ADVANCE, "", "",
                                menu.accessToken()))));
    }

    // -------------------------------------------------------------------------
    // Journal chrome / body
    // -------------------------------------------------------------------------

    @Override
    protected UiRect journalTitleBar() {
        Layout layout = calculateLayout(width, height);
        return toUi(layout.header());
    }

    @Override
    protected void renderJournalChrome(GuiGraphics graphics) {
        Layout layout = calculateLayout(width, height);
        ImmortalUiSkin.drawLayeredPanel(graphics, layout.left(), layout.top(),
                layout.panelWidth(), layout.panelHeight());
        UiRect header = toUi(layout.header());
        ImmortalUiSkin.drawTitleBar(graphics, header.x(), header.y(), header.width(), header.height());
        renderJournalTitle(graphics, header);
    }

    @Override
    protected void renderJournalTitle(GuiGraphics graphics, UiRect header) {
        Layout layout = calculateLayout(width, height);
        ImmortalUiSkin.drawStringFit(font, graphics,
                Component.translatable("screen.seeking_immortals.sect.title").getString(),
                layout.titleArea().x(),
                layout.titleArea().y() + Math.max(2, (layout.titleArea().height() - 8) / 2),
                layout.titleArea().width(), ImmortalUiSkin.JOURNAL_PAPER, false);
    }

    @Override
    protected void renderJournalBody(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        Layout layout = calculateLayout(width, height);
        ClientSectData.Snapshot data = ClientSectData.get();
        renderSummary(graphics, layout.summary(), data);

        Rect content = data.member() && tab != Tab.SHOP
                ? layout.content() : expandedContent(layout, data.member());
        ImmortalUiSkin.drawInnerFrame(graphics, content.x(), content.y(), content.width(), content.height());
        if (!data.synced()) {
            Rect viewport = inset(content, 5);
            ImmortalUiSkin.drawWrappedText(font, graphics,
                    Component.translatable("screen.seeking_immortals.sect.candidates_empty"),
                    viewport.x(), viewport.y(), viewport.width(), viewport.height(),
                    ImmortalUiSkin.JOURNAL_PAPER_MUTED, false);
            return;
        }
        if (!data.member()) {
            renderCandidates(graphics, layout, content, data, mouseX, mouseY);
            return;
        }
        switch (tab) {
            case DIALOGUE -> renderDialogue(graphics, content, data);
            case MISSION -> renderMission(graphics, content, data);
            case SHOP -> renderShop(graphics, layout, content, data, mouseX, mouseY);
            case PROGRESS -> renderProgress(graphics, content, data);
        }
    }

    // ---- Shared summary -----------------------------------------------------

    private void renderSummary(GuiGraphics graphics, Rect summary, ClientSectData.Snapshot data) {
        if (summary.height() < 9) return;
        int y = summary.y() + 1;
        int bottom = summary.bottom();
        y = summaryLine(graphics, summary, y, bottom,
                Component.translatable("screen.seeking_immortals.sect.current",
                        PlayerDisplayText.safeLiteral(data.currentSectDisplay(),
                                "text.seeking_immortals.unknown_faction"),
                        PlayerDisplayText.safeLiteral(data.role(),
                                "text.seeking_immortals.unknown_affiliation")), ImmortalUiSkin.JOURNAL_PAPER);
        y = summaryLine(graphics, summary, y, bottom,
                Component.translatable("screen.seeking_immortals.sect.contribution", data.contribution()),
                ImmortalUiSkin.JOURNAL_JADE_TEXT);
        summaryLine(graphics, summary, y, bottom,
                Component.translatable("screen.seeking_immortals.sect.stage",
                        PlayerDisplayText.translatedOr(data.stageKey(),
                                "text.seeking_immortals.unknown_phase")), ImmortalUiSkin.JOURNAL_SPIRIT);
    }

    private int summaryLine(GuiGraphics graphics, Rect summary, int y, int bottom,
                            Component text, int color) {
        if (y + 8 > bottom) return y;
        ImmortalUiSkin.drawStringFit(font, graphics, text.getString(), summary.x(), y,
                summary.width(), color, false);
        return y + LINE;
    }

    // ---- Tab content hooks --------------------------------------------------

    private void renderCandidates(GuiGraphics graphics, Layout layout, Rect content,
                                  ClientSectData.Snapshot data, int mouseX, int mouseY) {
        Rect viewport = candidateViewport(layout);
        if (content.height() >= 18) {
            ImmortalUiSkin.drawStringFit(font, graphics,
                    Component.translatable("screen.seeking_immortals.sect.candidates").getString(),
                    content.x() + 5, content.y() + 4, Math.max(1, content.width() - 10),
                    ImmortalUiSkin.JOURNAL_PAPER, false);
        }
        if (data.candidates().isEmpty()) {
            ImmortalUiSkin.drawWrappedText(font, graphics,
                    Component.translatable("screen.seeking_immortals.sect.candidates_empty"),
                    viewport.x() + 2, viewport.y() + 2, Math.max(1, viewport.width() - 4),
                    viewport.height(), ImmortalUiSkin.JOURNAL_PAPER_MUTED, false);
            return;
        }
        bindListViewport(viewport, layout.rowHeight(), data.candidates().size());
        int listScroll = listPanel.scrollRows();
        int visible = listPanel.visibleRowCount();
        int hovered = listPanel.hoveredRow(mouseX, mouseY, data.candidates().size());
        ImmortalUiSkin.withScissor(graphics, viewport.x(), viewport.y(), viewport.width(), viewport.height(), () -> {
            for (int row = 0; row < visible && listScroll + row < data.candidates().size(); row++) {
                ClientSectData.Candidate candidate = data.candidates().get(listScroll + row);
                Rect item = rowRect(viewport, layout.rowHeight(), row);
                Rect action = rowAction(viewport, layout.rowHeight(), row);
                ImmortalUiSkin.drawListRow(graphics, item.x(), item.y(), item.width(), item.height(),
                        hovered == row ? ImmortalUiSkin.InteractionState.HOVERED
                                : ImmortalUiSkin.InteractionState.NORMAL);
                ImmortalUiSkin.drawStringFit(font, graphics,
                        candidateDisplay(candidate).getString(), item.x() + 4, item.y() + 4,
                        Math.max(1, action.x() - item.x() - 8), ImmortalUiSkin.JOURNAL_PAPER, false);
            }
        });
        listPanel.drawScrollbar(graphics);
    }

    private void renderDialogue(GuiGraphics graphics, Rect content, ClientSectData.Snapshot data) {
        Rect viewport = inset(content, 5);
        ImmortalUiSkin.drawStringFit(font, graphics,
                PlayerDisplayText.translatedOr(data.dialogue().titleKey(),
                        "text.seeking_immortals.unknown_faction").getString(), viewport.x(), viewport.y(),
                viewport.width(), ImmortalUiSkin.JOURNAL_PAPER, false);
        ImmortalUiSkin.drawWrappedText(font, graphics,
                PlayerDisplayText.translatedOr(data.dialogue().textKey(),
                        "message.seeking_immortals.dialogue.line_unavailable"),
                viewport.x(), viewport.y() + 16, viewport.width(), Math.max(1, viewport.height() - 16),
                ImmortalUiSkin.JOURNAL_PAPER_MUTED, false);
    }

    private void renderMission(GuiGraphics graphics, Rect content, ClientSectData.Snapshot data) {
        Rect viewport = inset(content, 5);
        ImmortalUiSkin.drawStringFit(font, graphics,
                Component.translatable("screen.seeking_immortals.sect.mission").getString(),
                viewport.x(), viewport.y(), viewport.width(), ImmortalUiSkin.JOURNAL_PAPER, false);
        if (data.mission() != null && data.mission().available()) {
            String text = PlayerDisplayText.translatedOr(data.mission().titleKey(),
                    "text.seeking_immortals.unknown_quest").getString()
                    + " / +" + data.mission().rewardContribution();
            ImmortalUiSkin.drawStringFit(font, graphics, text, viewport.x(), viewport.y() + 16,
                    viewport.width(), ImmortalUiSkin.JOURNAL_PAPER, false);
        } else {
            ImmortalUiSkin.drawStringFit(font, graphics,
                    Component.translatable("screen.seeking_immortals.sect.mission_empty").getString(),
                    viewport.x(), viewport.y() + 16, viewport.width(),
                    ImmortalUiSkin.JOURNAL_PAPER_MUTED, false);
        }
    }

    private void renderShop(GuiGraphics graphics, Layout layout, Rect content,
                            ClientSectData.Snapshot data, int mouseX, int mouseY) {
        Rect viewport = shopViewport(layout);
        if (content.height() >= 18) {
            ImmortalUiSkin.drawStringFit(font, graphics,
                    Component.translatable("screen.seeking_immortals.sect.shop").getString(),
                    content.x() + 5, content.y() + 4, Math.max(1, content.width() - 10),
                    ImmortalUiSkin.JOURNAL_PAPER, false);
        }
        if (data.shopEntries().isEmpty()) {
            ImmortalUiSkin.drawStringFit(font, graphics,
                    Component.translatable("screen.seeking_immortals.shop.empty").getString(),
                    viewport.x() + 2, viewport.y() + 2, Math.max(1, viewport.width() - 4),
                    ImmortalUiSkin.JOURNAL_PAPER_MUTED, false);
            return;
        }
        bindListViewport(viewport, layout.rowHeight(), data.shopEntries().size());
        int listScroll = listPanel.scrollRows();
        int visible = listPanel.visibleRowCount();
        int hovered = listPanel.hoveredRow(mouseX, mouseY, data.shopEntries().size());
        ImmortalUiSkin.withScissor(graphics, viewport.x(), viewport.y(), viewport.width(), viewport.height(), () -> {
            for (int row = 0; row < visible && listScroll + row < data.shopEntries().size(); row++) {
                ClientSectData.ShopEntry entry = data.shopEntries().get(listScroll + row);
                Rect item = rowRect(viewport, layout.rowHeight(), row);
                Rect action = rowAction(viewport, layout.rowHeight(), row);
                ImmortalUiSkin.drawListRow(graphics, item.x(), item.y(), item.width(), item.height(),
                        hovered == row ? ImmortalUiSkin.InteractionState.HOVERED
                                : ImmortalUiSkin.InteractionState.NORMAL);
                ImmortalUiSkin.drawStringFit(font, graphics,
                        PlayerDisplayText.translatedOr(entry.itemDescriptionId(),
                                "text.seeking_immortals.unknown_item").getString() + " x" + entry.count()
                                + " / " + entry.cost(), item.x() + 4, item.y() + 4,
                        Math.max(1, action.x() - item.x() - 8), ImmortalUiSkin.JOURNAL_PAPER, false);
            }
        });
        listPanel.drawScrollbar(graphics);
    }

    private void renderProgress(GuiGraphics graphics, Rect content, ClientSectData.Snapshot data) {
        Rect viewport = inset(content, 5);
        ImmortalUiSkin.drawStringFit(font, graphics,
                Component.translatable("screen.seeking_immortals.sect.progress").getString(),
                viewport.x(), viewport.y(), viewport.width(), ImmortalUiSkin.JOURNAL_PAPER, false);
        int y = ImmortalUiSkin.drawWrappedText(font, graphics,
                PlayerDisplayText.translatedOr(data.objectiveKey(),
                        "text.seeking_immortals.unknown_quest"), viewport.x(), viewport.y() + 16,
                viewport.width(), Math.max(1, viewport.height() - 28),
                ImmortalUiSkin.JOURNAL_PAPER_MUTED, false);
        if (y + 8 <= viewport.bottom()) {
            ImmortalUiSkin.drawStringFit(font, graphics,
                    Component.translatable("screen.seeking_immortals.sect.stage",
                            PlayerDisplayText.translatedOr(data.stageKey(),
                                    "text.seeking_immortals.unknown_phase")).getString(),
                    viewport.x(), y, viewport.width(), ImmortalUiSkin.JOURNAL_SPIRIT, false);
        }
    }

    private static Component candidateDisplay(ClientSectData.Candidate candidate) {
        return PlayerDisplayText.safeLiteral(candidate == null ? "" : candidate.displayZh(),
                "text.seeking_immortals.unknown_faction");
    }

    // -------------------------------------------------------------------------
    // Input
    // -------------------------------------------------------------------------

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (delta == 0.0D) return super.mouseScrolled(mouseX, mouseY, delta);
        Layout layout = calculateLayout(width, height);
        ClientSectData.Snapshot data = ClientSectData.get();
        Rect viewport;
        int total;
        if (!data.member()) {
            viewport = candidateViewport(layout);
            total = data.candidates().size();
        } else if (tab == Tab.SHOP) {
            viewport = shopViewport(layout);
            total = data.shopEntries().size();
        } else {
            return super.mouseScrolled(mouseX, mouseY, delta);
        }
        bindListViewport(viewport, layout.rowHeight(), total);
        int before = listPanel.scrollRows();
        if (listPanel.mouseScrolledRows(mouseX, mouseY, delta, total)) {
            if (listPanel.scrollRows() != before) {
                rebuildActionWidgets();
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    // -------------------------------------------------------------------------
    // Layout helpers (public API preserved for ScreenLayoutTest)
    // -------------------------------------------------------------------------

    private void bindListViewport(Rect viewport, int rowHeight, int itemCount) {
        listPanel.setBounds(toUi(viewport))
                .setRowMetrics(rowHeight, 0)
                .setContentRows(itemCount);
        listPanel.clampToViewport();
        int visible = listPanel.visibleRowCount();
        int clamped = Mth.clamp(listPanel.scrollRows(), 0, Math.max(0, itemCount - visible));
        listPanel.setScrollRows(clamped);
    }

    private static UiRect toUi(Rect rect) {
        return new UiRect(rect.x(), rect.y(), rect.width(), rect.height());
    }

    static Layout calculateLayout(int screenWidth, int screenHeight) {
        int panelWidth = Math.min(MAX_PANEL_WIDTH, Math.max(1, screenWidth - PANEL_MARGIN * 2));
        int panelHeight = Math.min(MAX_PANEL_HEIGHT, Math.max(1, screenHeight - PANEL_MARGIN * 2));
        int left = Math.max(0, (screenWidth - panelWidth) / 2);
        int top = Math.max(0, (screenHeight - panelHeight) / 2);
        int padding = panelWidth >= 220 ? 10 : 5;
        int innerX = left + padding;
        int innerWidth = Math.max(1, panelWidth - padding * 2);
        int headerHeight = panelHeight >= 160 ? 34 : 20;
        Rect header = new Rect(innerX, top + 4, innerWidth,
                Math.min(headerHeight, Math.max(1, panelHeight - 8)));
        int buttonGap = innerWidth >= 80 ? 3 : 1;
        int buttonWidth = Math.max(1, Math.min(60, (innerWidth - 28 - buttonGap) / 2));
        int buttonHeight = Math.max(12, Math.min(18, header.height() - 4));
        int buttonY = header.y() + Math.max(1, (header.height() - buttonHeight) / 2);
        Rect close = new Rect(header.right() - buttonWidth - 3, buttonY, buttonWidth, buttonHeight);
        Rect refresh = new Rect(close.x() - buttonGap - buttonWidth, buttonY, buttonWidth, buttonHeight);
        Rect titleArea = new Rect(header.x() + 5, header.y(),
                Math.max(1, refresh.x() - header.x() - 8), header.height());

        int summaryHeight = panelHeight >= 190 ? 42 : panelHeight >= 130 ? 26
                : panelHeight >= 100 ? 14 : 0;
        Rect summary = new Rect(innerX, header.bottom() + 2, innerWidth, summaryHeight);
        int tabHeight = panelHeight >= 120 ? 18 : 14;
        int tabY = summary.bottom() + (summaryHeight > 0 ? 2 : 1);
        int tabGap = innerWidth >= 80 ? 3 : 1;
        int tabWidth = Math.max(1, (innerWidth - tabGap * 3) / 4);
        Rect dialogueTab = new Rect(innerX, tabY, tabWidth, tabHeight);
        Rect missionTab = new Rect(dialogueTab.right() + tabGap, tabY, tabWidth, tabHeight);
        Rect shopTab = new Rect(missionTab.right() + tabGap, tabY, tabWidth, tabHeight);
        Rect progressTab = new Rect(shopTab.right() + tabGap, tabY,
                Math.max(1, innerX + innerWidth - shopTab.right() - tabGap), tabHeight);

        int footerHeight = panelHeight >= 120 ? 18 : 14;
        Rect footer = new Rect(innerX, top + panelHeight - footerHeight - 5, innerWidth, footerHeight);
        int contentY = dialogueTab.bottom() + 3;
        Rect content = new Rect(innerX, contentY, innerWidth,
                Math.max(1, footer.y() - contentY - 3));
        int rowHeight = content.height() >= 44 ? ROW_HEIGHT : 20;
        return new Layout(left, top, panelWidth, panelHeight, header, titleArea, summary,
                dialogueTab, missionTab, shopTab, progressTab, content, footer,
                refresh, close, rowHeight);
    }

    private static Rect expandedContent(Layout layout, boolean member) {
        int y = member ? layout.content().y() : layout.dialogueTab().y();
        return new Rect(layout.content().x(), y, layout.content().width(),
                Math.max(1, layout.footer().bottom() - y));
    }

    private static Rect candidateViewport(Layout layout) {
        Rect content = expandedContent(layout, false);
        int heading = content.height() >= 18 ? 17 : 2;
        return new Rect(content.x() + 3, content.y() + heading,
                Math.max(1, content.width() - 8), Math.max(1, content.height() - heading - 3));
    }

    private static Rect shopViewport(Layout layout) {
        Rect content = expandedContent(layout, true);
        int heading = content.height() >= 18 ? 17 : 2;
        return new Rect(content.x() + 3, content.y() + heading,
                Math.max(1, content.width() - 8), Math.max(1, content.height() - heading - 3));
    }

    private static Rect inset(Rect rect, int amount) {
        int inset = Math.min(amount, Math.max(0, Math.min(rect.width(), rect.height()) / 3));
        return new Rect(rect.x() + inset, rect.y() + inset,
                Math.max(1, rect.width() - inset * 2), Math.max(1, rect.height() - inset * 2));
    }

    private static Rect rowRect(Rect viewport, int rowHeight, int row) {
        return new Rect(viewport.x(), viewport.y() + row * rowHeight, viewport.width(), rowHeight);
    }

    private static Rect rowAction(Rect viewport, int rowHeight, int row) {
        Rect item = rowRect(viewport, rowHeight, row);
        int width = Math.max(22, Math.min(56, item.width() / 3));
        int height = Math.max(12, Math.min(18, item.height() - 4));
        return new Rect(item.right() - width - 3, item.y() + Math.max(1, (item.height() - height) / 2),
                width, height);
    }

    private static List<Rect> footerButtons(Layout layout, int count) {
        if (count <= 0) return List.of();
        int gap = layout.footer().width() >= 60 ? 4 : 1;
        int buttonWidth = Math.max(1, (layout.footer().width() - gap * (count - 1)) / count);
        int buttonHeight = Math.max(12, Math.min(18, layout.footer().height()));
        List<Rect> result = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            result.add(new Rect(layout.footer().x() + i * (buttonWidth + gap), layout.footer().y(),
                    buttonWidth, buttonHeight));
        }
        return result;
    }

    record Rect(int x, int y, int width, int height) {
        int right() {
            return x + width;
        }

        int bottom() {
            return y + height;
        }

        boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX < right() && mouseY >= y && mouseY < bottom();
        }
    }

    record Layout(int left, int top, int panelWidth, int panelHeight, Rect header,
                  Rect titleArea, Rect summary, Rect dialogueTab, Rect missionTab,
                  Rect shopTab, Rect progressTab, Rect content, Rect footer,
                  Rect refreshButton, Rect closeButton, int rowHeight) {
    }

    private enum Tab {
        DIALOGUE("screen.seeking_immortals.sect.tab.dialogue"),
        MISSION("screen.seeking_immortals.sect.tab.mission"),
        SHOP("screen.seeking_immortals.sect.tab.shop"),
        PROGRESS("screen.seeking_immortals.sect.tab.progress");

        private final String key;

        Tab(String key) {
            this.key = key;
        }
    }
}
