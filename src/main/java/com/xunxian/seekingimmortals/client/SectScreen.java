package com.xunxian.seekingimmortals.client;

import com.xunxian.seekingimmortals.network.ModNetwork;
import com.xunxian.seekingimmortals.network.SectActionPacket;
import com.xunxian.seekingimmortals.sect.SectContributionService;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.List;

public class SectScreen extends AbstractJournalScreen {
    private static final int PANEL_WIDTH = 420;
    private static final int PANEL_HEIGHT = 272;
    private static final int PANEL_MARGIN = 4;
    private static final int LINE = 13;
    private static final int ROW_HEIGHT = 24;

    private Tab tab = Tab.DIALOGUE;
    private int listScroll;

    public SectScreen() {
        super(Component.translatable("screen.seeking_immortals.sect.title"));
    }

    @Override
    protected void init() {
        super.init();
        rebuildActionWidgets();
    }

    private void rebuildActionWidgets() {
        clearWidgets();
        Layout layout = calculateLayout(width, height);
        ClientSectData.Snapshot data = ClientSectData.get();
        addRenderableWidget(ImmortalButton.secondary(layout.refreshButton().x(), layout.refreshButton().y(),
                layout.refreshButton().width(), layout.refreshButton().height(),
                Component.translatable("screen.seeking_immortals.sect.refresh"), button ->
                        ModNetwork.CHANNEL.sendToServer(new SectActionPacket(
                                SectContributionService.ACTION_SYNC, ""))));
        addRenderableWidget(ImmortalButton.secondary(layout.closeButton().x(), layout.closeButton().y(),
                layout.closeButton().width(), layout.closeButton().height(),
                Component.translatable("screen.seeking_immortals.sect.close"), button -> onClose()));

        if (!data.member()) {
            addCandidateButtons(layout, data);
            return;
        }
        addTabButtons(layout);
        switch (tab) {
            case DIALOGUE -> addDialogueButtons(layout, data);
            case MISSION -> addMissionButtons(layout, data);
            case SHOP -> addShopButtons(layout, data);
            case PROGRESS -> addProgressButtons(layout);
        }
    }

    private void addTabButtons(Layout layout) {
        addTabButton(layout.dialogueTab(), Tab.DIALOGUE);
        addTabButton(layout.missionTab(), Tab.MISSION);
        addTabButton(layout.shopTab(), Tab.SHOP);
        addTabButton(layout.progressTab(), Tab.PROGRESS);
    }

    private void addTabButton(Rect bounds, Tab target) {
        ImmortalButton button = target == tab
                ? ImmortalButton.primary(bounds.x(), bounds.y(), bounds.width(), bounds.height(),
                Component.translatable(target.key), ignored -> setTab(target))
                : ImmortalButton.secondary(bounds.x(), bounds.y(), bounds.width(), bounds.height(),
                Component.translatable(target.key), ignored -> setTab(target));
        addRenderableWidget(button);
    }

    private void setTab(Tab target) {
        if (tab != target) {
            tab = target;
            listScroll = 0;
            rebuildActionWidgets();
        }
    }

    private void addCandidateButtons(Layout layout, ClientSectData.Snapshot data) {
        Rect viewport = candidateViewport(layout);
        int visible = visibleRows(viewport, layout.rowHeight());
        List<ClientSectData.Candidate> candidates = data.candidates();
        listScroll = Mth.clamp(listScroll, 0, Math.max(0, candidates.size() - visible));
        for (int row = 0; row < visible && listScroll + row < candidates.size(); row++) {
            ClientSectData.Candidate candidate = candidates.get(listScroll + row);
            Rect action = rowAction(viewport, layout.rowHeight(), row);
            addRenderableWidget(ImmortalButton.primary(action.x(), action.y(), action.width(), action.height(),
                    Component.translatable("screen.seeking_immortals.sect.join"), button ->
                            ModNetwork.CHANNEL.sendToServer(new SectActionPacket(
                                    SectContributionService.ACTION_APPLY, candidate.id()))));
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
                    Component.translatable(option.labelKey()), button ->
                            ModNetwork.CHANNEL.sendToServer(new SectActionPacket(
                                    SectContributionService.ACTION_DIALOGUE, option.id()))));
        }
    }

    private void addMissionButtons(Layout layout, ClientSectData.Snapshot data) {
        List<Rect> buttons = footerButtons(layout, 2);
        Rect accept = buttons.get(0);
        Rect turnIn = buttons.get(1);
        addRenderableWidget(ImmortalButton.primary(accept.x(), accept.y(), accept.width(), accept.height(),
                Component.translatable("screen.seeking_immortals.sect.mission.accept"), button ->
                        ModNetwork.CHANNEL.sendToServer(new SectActionPacket(
                                SectContributionService.ACTION_ACCEPT_MISSION, data.mission().id()))));
        addRenderableWidget(ImmortalButton.primary(turnIn.x(), turnIn.y(), turnIn.width(), turnIn.height(),
                Component.translatable("screen.seeking_immortals.sect.mission.turn_in"), button ->
                        ModNetwork.CHANNEL.sendToServer(new SectActionPacket(
                                SectContributionService.ACTION_TURN_IN_MISSION, data.mission().id()))));
    }

    private void addShopButtons(Layout layout, ClientSectData.Snapshot data) {
        Rect viewport = shopViewport(layout);
        int visible = visibleRows(viewport, layout.rowHeight());
        List<ClientSectData.ShopEntry> entries = data.shopEntries();
        listScroll = Mth.clamp(listScroll, 0, Math.max(0, entries.size() - visible));
        for (int row = 0; row < visible && listScroll + row < entries.size(); row++) {
            ClientSectData.ShopEntry entry = entries.get(listScroll + row);
            Rect action = rowAction(viewport, layout.rowHeight(), row);
            ImmortalButton button = ImmortalButton.primary(action.x(), action.y(), action.width(), action.height(),
                    Component.translatable("screen.seeking_immortals.sect.buy"), ignored ->
                    ModNetwork.CHANNEL.sendToServer(new SectActionPacket(
                            SectContributionService.ACTION_BUY, entry.id())));
            addRenderableWidget(button);
        }
    }

    private void addProgressButtons(Layout layout) {
        Rect bounds = footerButtons(layout, 1).get(0);
        addRenderableWidget(ImmortalButton.primary(bounds.x(), bounds.y(), bounds.width(), bounds.height(),
                Component.translatable("screen.seeking_immortals.sect.advance"), button ->
                        ModNetwork.CHANNEL.sendToServer(new SectActionPacket(
                                SectContributionService.ACTION_ADVANCE, ""))));
    }

    @Override
    protected JournalChrome journalChrome() {
        Layout layout = calculateLayout(width, height);
        // Content paints its own frame (expanded for candidates/shop); skip shared inner frame.
        return new JournalChrome(layout.left(), layout.top(), layout.panelWidth(), layout.panelHeight(),
                toUi(layout.header()), null);
    }

    @Override
    protected void renderJournalTitle(GuiGraphics graphics, JournalChrome chrome, UiRect header) {
        Layout layout = calculateLayout(width, height);
        ClientSectData.Snapshot data = ClientSectData.get();
        String heading = data.member() ? safe(data.currentSectDisplay())
                : safe(data.sectDisplay().isBlank() ? data.currentSectDisplay() : data.sectDisplay());
        if ("-".equals(heading)) heading = title.getString();
        ImmortalUiSkin.drawStringFit(font, graphics, heading, layout.titleArea().x(),
                layout.titleArea().y() + Math.max(2, (layout.titleArea().height() - 8) / 2),
                layout.titleArea().width(), ImmortalUiSkin.JOURNAL_PAPER, false);
    }

    @Override
    protected void renderJournalContent(GuiGraphics graphics, JournalChrome chrome,
                                         int mouseX, int mouseY, float partialTick) {
        Layout layout = calculateLayout(width, height);
        ClientSectData.Snapshot data = ClientSectData.get();
        renderSummary(graphics, layout.summary(), data);

        Rect content = data.member() && tab != Tab.SHOP
                ? layout.content() : expandedContent(layout, data.member());
        ImmortalUiSkin.drawInnerFrame(graphics, content.x(), content.y(), content.width(), content.height());
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

    private static UiRect toUi(Rect rect) {
        return new UiRect(rect.x(), rect.y(), rect.width(), rect.height());
    }

    private void renderSummary(GuiGraphics graphics, Rect summary, ClientSectData.Snapshot data) {
        if (summary.height() < 9) return;
        int y = summary.y() + 1;
        int bottom = summary.bottom();
        y = summaryLine(graphics, summary, y, bottom,
                Component.translatable("screen.seeking_immortals.sect.current",
                        safe(data.currentSectDisplay()), safe(data.role())), ImmortalUiSkin.JOURNAL_PAPER);
        y = summaryLine(graphics, summary, y, bottom,
                Component.translatable("screen.seeking_immortals.sect.contribution", data.contribution()),
                ImmortalUiSkin.JOURNAL_JADE_TEXT);
        y = summaryLine(graphics, summary, y, bottom,
                Component.translatable("screen.seeking_immortals.sect.gates",
                        bool(data.sevenMysteriesComplete()), bool(data.yueArrived())),
                ImmortalUiSkin.JOURNAL_PAPER_MUTED);
        y = summaryLine(graphics, summary, y, bottom,
                Component.translatable("screen.seeking_immortals.sect.stage",
                        Component.translatable(data.stageKey())), ImmortalUiSkin.JOURNAL_SPIRIT);
        if (y + 8 <= bottom) {
            ImmortalUiSkin.drawStringFit(font, graphics,
                    Component.translatable(data.objectiveKey()).getString(), summary.x(), y,
                    summary.width(), ImmortalUiSkin.JOURNAL_PAPER_MUTED, false);
        }
    }

    private int summaryLine(GuiGraphics graphics, Rect summary, int y, int bottom,
                            Component text, int color) {
        if (y + 8 > bottom) return y;
        ImmortalUiSkin.drawStringFit(font, graphics, text.getString(), summary.x(), y,
                summary.width(), color, false);
        return y + LINE;
    }

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
        int visible = visibleRows(viewport, layout.rowHeight());
        int hovered = hoveredRow(viewport, layout.rowHeight(), mouseX, mouseY);
        listScroll = Mth.clamp(listScroll, 0, Math.max(0, data.candidates().size() - visible));
        ImmortalUiSkin.withScissor(graphics, viewport.x(), viewport.y(), viewport.width(), viewport.height(), () -> {
            for (int row = 0; row < visible && listScroll + row < data.candidates().size(); row++) {
                ClientSectData.Candidate candidate = data.candidates().get(listScroll + row);
                Rect item = rowRect(viewport, layout.rowHeight(), row);
                ImmortalUiSkin.drawListRow(graphics, item.x(), item.y(), item.width(), item.height(),
                        hovered == row ? ImmortalUiSkin.InteractionState.HOVERED
                                : ImmortalUiSkin.InteractionState.NORMAL);
                Rect action = rowAction(viewport, layout.rowHeight(), row);
                ImmortalUiSkin.drawStringFit(font, graphics,
                        candidate.displayZh() + " / " + candidate.focusKey(), item.x() + 4, item.y() + 5,
                        Math.max(1, action.x() - item.x() - 8), ImmortalUiSkin.JOURNAL_PAPER, false);
            }
        });
        drawScrollbar(graphics, content, viewport, data.candidates().size(), layout.rowHeight());
    }

    private void renderDialogue(GuiGraphics graphics, Rect content, ClientSectData.Snapshot data) {
        Rect viewport = inset(content, 5);
        ImmortalUiSkin.withScissor(graphics, viewport.x(), viewport.y(), viewport.width(), viewport.height(), () -> {
            ImmortalUiSkin.drawStringFit(font, graphics,
                    Component.translatable(data.dialogue().titleKey()).getString(), viewport.x(), viewport.y(),
                    viewport.width(), ImmortalUiSkin.JOURNAL_PAPER, false);
            ImmortalUiSkin.drawWrappedText(font, graphics,
                    Component.translatable(data.dialogue().textKey()), viewport.x(), viewport.y() + 16,
                    viewport.width(), Math.max(1, viewport.height() - 16),
                    ImmortalUiSkin.JOURNAL_PAPER_MUTED, false);
        });
    }

    private void renderMission(GuiGraphics graphics, Rect content, ClientSectData.Snapshot data) {
        Rect viewport = inset(content, 5);
        ImmortalUiSkin.withScissor(graphics, viewport.x(), viewport.y(), viewport.width(), viewport.height(), () -> {
            ImmortalUiSkin.drawStringFit(font, graphics,
                    Component.translatable("screen.seeking_immortals.sect.mission").getString(),
                    viewport.x(), viewport.y(), viewport.width(), ImmortalUiSkin.JOURNAL_PAPER, false);
            if (!data.mission().available()) {
                ImmortalUiSkin.drawStringFit(font, graphics,
                        Component.translatable("screen.seeking_immortals.sect.mission_empty").getString(),
                        viewport.x(), viewport.y() + 16, viewport.width(),
                        ImmortalUiSkin.JOURNAL_PAPER_MUTED, false);
                return;
            }
            int y = viewport.y() + 16;
            ImmortalUiSkin.drawStringFit(font, graphics,
                    Component.translatable(data.mission().titleKey()).getString(), viewport.x(), y,
                    viewport.width(), ImmortalUiSkin.JOURNAL_PAPER, false);
            y += LINE;
            y = ImmortalUiSkin.drawWrappedText(font, graphics,
                    Component.translatable(data.mission().objectiveKey(), data.mission().target(),
                            Component.translatable(data.mission().itemDescriptionId())),
                    viewport.x(), y, viewport.width(), Math.max(1, viewport.bottom() - y - LINE),
                    ImmortalUiSkin.JOURNAL_PAPER_MUTED, false);
            String statusKey = data.mission().completed()
                    ? "screen.seeking_immortals.sect.mission.status.completed"
                    : data.mission().accepted()
                    ? "screen.seeking_immortals.sect.mission.status.accepted"
                    : "screen.seeking_immortals.sect.mission.status.available";
            if (y + 8 <= viewport.bottom()) {
                ImmortalUiSkin.drawStringFit(font, graphics,
                        Component.translatable(statusKey, data.mission().rewardContribution()).getString(),
                        viewport.x(), y, viewport.width(), ImmortalUiSkin.JOURNAL_JADE_TEXT, false);
            }
        });
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
                    Component.translatable("screen.seeking_immortals.sect.shop_empty").getString(),
                    viewport.x() + 2, viewport.y() + 2, Math.max(1, viewport.width() - 4),
                    ImmortalUiSkin.JOURNAL_PAPER_MUTED, false);
            return;
        }
        int visible = visibleRows(viewport, layout.rowHeight());
        int hovered = hoveredRow(viewport, layout.rowHeight(), mouseX, mouseY);
        listScroll = Mth.clamp(listScroll, 0, Math.max(0, data.shopEntries().size() - visible));
        ImmortalUiSkin.withScissor(graphics, viewport.x(), viewport.y(), viewport.width(), viewport.height(), () -> {
            for (int row = 0; row < visible && listScroll + row < data.shopEntries().size(); row++) {
                ClientSectData.ShopEntry entry = data.shopEntries().get(listScroll + row);
                Rect item = rowRect(viewport, layout.rowHeight(), row);
                boolean disabled = data.contribution() < entry.cost();
                ImmortalUiSkin.drawListRow(graphics, item.x(), item.y(), item.width(), item.height(),
                        disabled ? ImmortalUiSkin.InteractionState.DISABLED
                                : hovered == row ? ImmortalUiSkin.InteractionState.HOVERED
                                : ImmortalUiSkin.InteractionState.NORMAL);
                Rect action = rowAction(viewport, layout.rowHeight(), row);
                String text = entry.id() + " / " + Component.translatable(entry.itemDescriptionId()).getString()
                        + " x" + entry.count() + " / " + entry.cost();
                ImmortalUiSkin.drawStringFit(font, graphics, text, item.x() + 4, item.y() + 5,
                        Math.max(1, action.x() - item.x() - 8),
                        disabled ? ImmortalUiSkin.JOURNAL_PAPER_MUTED : ImmortalUiSkin.JOURNAL_PAPER, false);
            }
        });
        drawScrollbar(graphics, content, viewport, data.shopEntries().size(), layout.rowHeight());
    }

    private void renderProgress(GuiGraphics graphics, Rect content, ClientSectData.Snapshot data) {
        Rect viewport = inset(content, 5);
        ImmortalUiSkin.drawStringFit(font, graphics,
                Component.translatable("screen.seeking_immortals.sect.progress").getString(),
                viewport.x(), viewport.y(), viewport.width(), ImmortalUiSkin.JOURNAL_PAPER, false);
        int y = ImmortalUiSkin.drawWrappedText(font, graphics,
                Component.translatable(data.objectiveKey()), viewport.x(), viewport.y() + 16,
                viewport.width(), Math.max(1, viewport.height() - 30),
                ImmortalUiSkin.JOURNAL_PAPER_MUTED, false);
        if (y + 8 <= viewport.bottom()) {
            ImmortalUiSkin.drawStringFit(font, graphics,
                    Component.translatable("screen.seeking_immortals.sect.stage",
                            Component.translatable(data.stageKey())).getString(),
                    viewport.x(), y, viewport.width(), ImmortalUiSkin.JOURNAL_SPIRIT, false);
        }
    }

    private void drawScrollbar(GuiGraphics graphics, Rect content, Rect viewport,
                               int total, int rowHeight) {
        ImmortalUiSkin.drawThinScrollbar(graphics, content.right() - 3,
                viewport.y(), viewport.height(), total * rowHeight, viewport.height(), listScroll * rowHeight);
    }

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
        if (viewport.contains(mouseX, mouseY)) {
            int next = Mth.clamp(listScroll - (int)Math.signum(delta), 0,
                    Math.max(0, total - visibleRows(viewport, layout.rowHeight())));
            if (next != listScroll) {
                listScroll = next;
                rebuildActionWidgets();
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    private String bool(boolean value) {
        return Component.translatable(value
                ? "message.seeking_immortals.sect.yes"
                : "message.seeking_immortals.sect.no").getString();
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    static Layout calculateLayout(int screenWidth, int screenHeight) {
        int panelWidth = calculatePanelWidth(screenWidth);
        int panelHeight = calculatePanelHeight(screenHeight);
        int left = Math.max(0, (screenWidth - panelWidth) / 2);
        int top = Math.max(0, (screenHeight - panelHeight) / 2);
        int padding = panelWidth >= 220 ? 10 : 5;
        int innerX = left + padding;
        int innerWidth = Math.max(1, panelWidth - padding * 2);
        int headerHeight = panelHeight >= 170 ? 36 : 20;
        Rect header = new Rect(innerX, top + 4, innerWidth,
                Math.min(headerHeight, Math.max(1, panelHeight - 8)));
        int buttonGap = innerWidth >= 80 ? 3 : 1;
        int buttonWidth = Math.max(1, Math.min(62, (innerWidth - 28 - buttonGap) / 2));
        int buttonHeight = Math.max(12, Math.min(18, header.height() - 4));
        int buttonY = header.y() + Math.max(1, (header.height() - buttonHeight) / 2);
        Rect close = new Rect(header.right() - buttonWidth - 3, buttonY, buttonWidth, buttonHeight);
        Rect refresh = new Rect(close.x() - buttonGap - buttonWidth, buttonY, buttonWidth, buttonHeight);
        Rect titleArea = new Rect(header.x() + 5, header.y(),
                Math.max(1, refresh.x() - header.x() - 8), header.height());

        int summaryHeight = panelHeight >= 220 ? 58 : panelHeight >= 150 ? 38
                : panelHeight >= 105 ? 18 : 0;
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

        int footerHeight = panelHeight >= 120 ? 20 : 14;
        Rect footer = new Rect(innerX, top + panelHeight - footerHeight - 5, innerWidth, footerHeight);
        int contentY = dialogueTab.bottom() + 3;
        Rect content = new Rect(innerX, contentY, innerWidth,
                Math.max(1, footer.y() - contentY - 3));
        int rowHeight = content.height() >= 48 ? ROW_HEIGHT : 20;
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

    private static int visibleRows(Rect viewport, int rowHeight) {
        return Math.max(1, viewport.height() / rowHeight);
    }

    private static Rect rowRect(Rect viewport, int rowHeight, int row) {
        return new Rect(viewport.x(), viewport.y() + row * rowHeight, viewport.width(), rowHeight);
    }

    private static Rect rowAction(Rect viewport, int rowHeight, int row) {
        Rect item = rowRect(viewport, rowHeight, row);
        int width = Math.max(22, Math.min(58, item.width() / 3));
        int height = Math.max(12, Math.min(18, item.height() - 4));
        return new Rect(item.right() - width - 3, item.y() + Math.max(1, (item.height() - height) / 2),
                width, height);
    }

    private static int hoveredRow(Rect viewport, int rowHeight, double mouseX, double mouseY) {
        if (!viewport.contains(mouseX, mouseY)) return -1;
        int row = (int)((mouseY - viewport.y()) / rowHeight);
        return row < visibleRows(viewport, rowHeight) ? row : -1;
    }

    private static List<Rect> footerButtons(Layout layout, int count) {
        if (count <= 0) return List.of();
        int gap = layout.footer().width() >= 60 ? 4 : 1;
        int buttonWidth = Math.max(1, (layout.footer().width() - gap * (count - 1)) / count);
        int buttonHeight = Math.max(12, Math.min(20, layout.footer().height()));
        List<Rect> result = new java.util.ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            result.add(new Rect(layout.footer().x() + i * (buttonWidth + gap), layout.footer().y(),
                    buttonWidth, buttonHeight));
        }
        return result;
    }

    static int calculatePanelWidth(int screenWidth) {
        return Math.min(PANEL_WIDTH, Math.max(1, screenWidth - PANEL_MARGIN * 2));
    }

    static int calculatePanelHeight(int screenHeight) {
        return Math.min(PANEL_HEIGHT, Math.max(1, screenHeight - PANEL_MARGIN * 2));
    }

    static int shopTopOffset(int panelHeight) {
        int preferred = Math.min(112, Math.max(102, panelHeight - 134));
        int maxOffset = Math.max(28, panelHeight - 54);
        int minOffset = Math.min(panelHeight < 150 ? 60 : 96, maxOffset);
        return Math.max(28, Math.max(minOffset, Math.min(preferred, maxOffset)));
    }

    static int visibleShopRows(int panelWidth, int panelHeight) {
        int bottomReserve = 34;
        int available = panelHeight - shopTopOffset(panelHeight) - bottomReserve;
        return Math.max(0, Math.min(5, available / 22));
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
