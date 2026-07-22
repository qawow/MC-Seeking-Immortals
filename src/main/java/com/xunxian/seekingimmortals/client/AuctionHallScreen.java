package com.xunxian.seekingimmortals.client;

import com.xunxian.seekingimmortals.menu.AuctionHallMenu;
import com.xunxian.seekingimmortals.network.AuctionActionPacket;
import com.xunxian.seekingimmortals.network.ModNetwork;
import com.xunxian.seekingimmortals.network.SyncAuctionLadderPacket;
import com.xunxian.seekingimmortals.util.PlayerDisplayText;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

/** Live, server-paged auction ladder with an in-page scroll viewport. */
public class AuctionHallScreen extends AbstractJournalContainerScreen<AuctionHallMenu> {
    private static final int PANEL_MARGIN = 4;
    private static final int ROW_HEIGHT = 50;
    private static final int ROW_GAP = 4;

    private final ScrollableListPanel listPanel = new ScrollableListPanel();
    private int observedRevision = Integer.MIN_VALUE;
    private int observedPage = -1;

    public AuctionHallScreen(AuctionHallMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 360;
        this.imageHeight = 236;
        this.listPanel.setScrollStep(20)
                .setContentInsets(4, 4, 5, 0)
                .setRowMetrics(ROW_HEIGHT, ROW_GAP)
                .setScrollbarInsetRight(3);
    }

    @Override
    protected void init() {
        super.init();
        ModNetwork.CHANNEL.sendToServer(new AuctionActionPacket(
                AuctionActionPacket.ACTION_PAGE, "0", menu.accessToken()));
        rebuildButtons();
    }

    private void rebuildButtons() {
        clearWidgets();
        HallLayout layout = calculateLayout(width, height);
        ClientAuctionLadderData.Snapshot data = ClientAuctionLadderData.get();
        int page = data.page();
        if (observedPage != -1 && observedPage != page) {
            listPanel.resetScroll();
        }
        observedPage = page;
        observedRevision = data.hashCode();

        addRenderableWidget(ImmortalButton.secondary(layout.refreshButton().x(), layout.refreshButton().y(),
                layout.refreshButton().width(), layout.refreshButton().height(),
                Component.translatable("screen.seeking_immortals.auction.refresh"), button ->
                        send(AuctionActionPacket.ACTION_PAGE, Integer.toString(page))));
        addRenderableWidget(ImmortalButton.secondary(layout.closeButton().x(), layout.closeButton().y(),
                layout.closeButton().width(), layout.closeButton().height(),
                Component.translatable("screen.seeking_immortals.auction.close"), button -> onClose()));
        ImmortalButton previous = ImmortalButton.secondary(layout.previousButton().x(), layout.previousButton().y(),
                layout.previousButton().width(), layout.previousButton().height(), Component.literal("<"), button ->
                        send(AuctionActionPacket.ACTION_PAGE, Integer.toString(Math.max(0, page - 1))));
        previous.active = canPagePrevious(page);
        addRenderableWidget(previous);
        ImmortalButton next = ImmortalButton.secondary(layout.nextButton().x(), layout.nextButton().y(),
                layout.nextButton().width(), layout.nextButton().height(), Component.literal(">"), button ->
                        send(AuctionActionPacket.ACTION_PAGE, Integer.toString(page + 1)));
        next.active = canPageNext(data.synced(), page, data.maxPage());
        addRenderableWidget(next);

        List<SyncAuctionLadderPacket.LotBid> lots = data.lots();
        listPanel.setBounds(layout.viewport())
                .setContentHeight(calculateContentHeight(lots.size()));
        listPanel.clampToViewport();
        int scrollOffset = listPanel.scrollOffset();
        int rowY = layout.viewport().y() + 4 - scrollOffset;
        for (int i = 0; i < lots.size(); i++) {
            SyncAuctionLadderPacket.LotBid lot = lots.get(i);
            UiRect row = new UiRect(layout.viewport().x() + 4,
                    rowY + i * (ROW_HEIGHT + ROW_GAP),
                    Math.max(1, layout.viewport().width() - 9), ROW_HEIGHT);
            addLotButtons(layout.viewport(), row, lot);
        }
    }

    private void addLotButtons(UiRect viewport, UiRect row, SyncAuctionLadderPacket.LotBid lot) {
        int gap = 3;
        int buttonWidth = Math.max(1, (row.width() - gap * 2 - 8) / 3);
        int buttonX = row.x() + 4;
        int buttonY = row.y() + 29;
        ImmortalButton preview = ImmortalButton.secondary(buttonX, buttonY, buttonWidth, 17,
                Component.translatable("screen.seeking_immortals.auction.preview"), ignored ->
                        send(AuctionActionPacket.ACTION_PREVIEW, lot.lotId()));
        ImmortalButton settle = ImmortalButton.secondary(buttonX + buttonWidth + gap, buttonY,
                buttonWidth, 17, Component.translatable("screen.seeking_immortals.auction.settle"), ignored ->
                        send(AuctionActionPacket.ACTION_SETTLE, lot.lotId()));
        ImmortalButton bid = ImmortalButton.primary(buttonX + (buttonWidth + gap) * 2, buttonY,
                Math.max(1, row.right() - 4 - (buttonX + (buttonWidth + gap) * 2)), 17,
                Component.translatable("screen.seeking_immortals.auction.bid"), ignored ->
                        send(AuctionActionPacket.ACTION_BID, lot.lotId()));
        boolean visible = buttonY >= viewport.y() && buttonY + 17 <= viewport.bottom();
        preview.visible = visible;
        settle.visible = visible;
        bid.visible = visible;
        settle.active = !lot.settled();
        bid.active = !lot.settled();
        addRenderableWidget(preview);
        addRenderableWidget(settle);
        addRenderableWidget(bid);
    }

    private void send(String action, String payload) {
        ModNetwork.CHANNEL.sendToServer(new AuctionActionPacket(action, payload, menu.accessToken()));
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        ClientAuctionLadderData.Snapshot data = ClientAuctionLadderData.get();
        if (data.hashCode() != observedRevision) {
            rebuildButtons();
        }
    }

    @Override
    protected UiRect journalTitleBar() {
        HallLayout layout = calculateLayout(width, height);
        return layout.header();
    }

    @Override
    protected void renderJournalTitle(GuiGraphics graphics, UiRect header) {
        HallLayout layout = calculateLayout(width, height);
        int titleWidth = layout.panelWidth() < 240 ? header.width()
                : Math.max(1, layout.refreshButton().x() - header.x() - 5);
        ImmortalUiSkin.drawStringFit(font, graphics,
                Component.translatable("screen.seeking_immortals.auction.title").getString(),
                header.x() + 7, header.y() + Math.max(2, (header.height() - 8) / 2),
                titleWidth, ImmortalUiSkin.JOURNAL_PAPER, false);
    }

    @Override
    protected void renderJournalChrome(GuiGraphics graphics) {
        HallLayout layout = calculateLayout(width, height);
        ImmortalUiSkin.drawLayeredPanel(graphics, layout.left(), layout.top(),
                layout.panelWidth(), layout.panelHeight());
        UiRect header = layout.header();
        ImmortalUiSkin.drawTitleBar(graphics, header.x(), header.y(), header.width(), header.height());
        renderJournalTitle(graphics, header);
    }

    @Override
    protected void renderJournalBody(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        HallLayout layout = calculateLayout(width, height);
        ClientAuctionLadderData.Snapshot data = ClientAuctionLadderData.get();
        ImmortalUiSkin.drawStringFit(font, graphics,
                Component.translatable("screen.seeking_immortals.auction.page",
                        data.page() + 1, data.maxPage() + 1, data.totalLots()).getString(),
                layout.summary().x() + 3, layout.summary().y() + 2,
                Math.max(1, layout.summary().width() - 6), ImmortalUiSkin.JOURNAL_PAPER_MUTED, false);
        ImmortalUiSkin.drawInnerFrame(graphics, layout.viewport().x(), layout.viewport().y(),
                layout.viewport().width(), layout.viewport().height());
        drawLots(graphics, layout, mouseX, mouseY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        HallLayout layout = calculateLayout(width, height);
        ClientAuctionLadderData.Snapshot data = ClientAuctionLadderData.get();
        listPanel.setBounds(layout.viewport())
                .setContentHeight(calculateContentHeight(data.lots().size()));
        if (listPanel.mouseScrolled(mouseX, mouseY, delta)) {
            rebuildButtons();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    static HallLayout calculateLayout(int screenWidth, int screenHeight) {
        int panelWidth = Math.min(360, Math.max(1, screenWidth - PANEL_MARGIN * 2));
        int panelHeight = Math.min(236, Math.max(1, screenHeight - PANEL_MARGIN * 2));
        int left = Math.max(0, (screenWidth - panelWidth) / 2);
        int top = Math.max(0, (screenHeight - panelHeight) / 2);
        int padding = panelWidth >= 180 ? 8 : 3;
        boolean compact = panelWidth < 240;
        int headerHeight = compact ? 40 : 27;
        int buttonHeight = compact ? 16 : 18;
        int buttonWidth = compact ? Math.max(1, (panelWidth - padding * 3) / 2)
                : Math.min(64, Math.max(1, (panelWidth - padding * 3) / 2));
        int buttonY = top + (compact ? 21 : 7);
        UiRect header = new UiRect(left + padding, top + 4, Math.max(1, panelWidth - padding * 2),
                compact ? 14 : 21);
        UiRect refresh = new UiRect(compact ? left + padding : left + panelWidth - padding * 2 - buttonWidth * 2,
                buttonY, buttonWidth, buttonHeight);
        UiRect close = new UiRect(refresh.right() + padding, buttonY, buttonWidth, buttonHeight);
        int summaryY = top + headerHeight + 3;
        UiRect summary = new UiRect(left + padding, summaryY, Math.max(1, panelWidth - padding * 2), 13);
        int footerHeight = 21;
        int footerY = Math.max(summary.y() + summary.height() + 4,
                top + panelHeight - padding - footerHeight);
        UiRect previous = new UiRect(left + padding, footerY, 24, Math.min(18, footerHeight));
        UiRect next = new UiRect(previous.right() + 4, footerY, 24, Math.min(18, footerHeight));
        int viewportY = summary.y() + summary.height() + 3;
        UiRect viewport = new UiRect(left + padding, viewportY, Math.max(1, panelWidth - padding * 2),
                Math.max(1, footerY - 4 - viewportY));
        return new HallLayout(left, top, panelWidth, panelHeight, header, summary, viewport,
                refresh, close, previous, next);
    }

    static int calculateContentHeight(int rowCount) {
        return Math.max(1, rowCount * (ROW_HEIGHT + ROW_GAP) + 8);
    }

    static int clampScroll(int requested, int contentHeight, int viewportHeight) {
        return ScrollableListPanel.clampScroll(requested, contentHeight, viewportHeight);
    }

    /** Server-side page controls: previous is only active past page 0. */
    static boolean canPagePrevious(int page) {
        return page > 0;
    }

    /**
     * Next stays enabled while unsynced (so refresh/page requests can still be sent)
     * or when the current server page is below maxPage.
     */
    static boolean canPageNext(boolean synced, int page, int maxPage) {
        return !synced || page < maxPage;
    }

    /** Requested page payload for previous/next buttons. */
    static int previousPage(int page) {
        return Math.max(0, page - 1);
    }

    static int nextPage(int page) {
        return Math.max(0, page + 1);
    }

    private void drawLots(GuiGraphics graphics, HallLayout layout, int mouseX, int mouseY) {
        ClientAuctionLadderData.Snapshot data = ClientAuctionLadderData.get();
        listPanel.setBounds(layout.viewport())
                .setContentHeight(calculateContentHeight(data.lots().size()));
        listPanel.clampToViewport();
        int scrollOffset = listPanel.scrollOffset();
        int rowY = layout.viewport().y() + 4 - scrollOffset;
        ImmortalUiSkin.withScissor(graphics, layout.viewport().x(), layout.viewport().y(),
                layout.viewport().width(), layout.viewport().height(), () -> {
                    if (!data.synced()) {
                        drawEmpty(graphics, layout.viewport(), "screen.seeking_immortals.auction.waiting");
                        return;
                    }
                    if (data.lots().isEmpty()) {
                        drawEmpty(graphics, layout.viewport(), "screen.seeking_immortals.auction.empty");
                        return;
                    }
                    for (int i = 0; i < data.lots().size(); i++) {
                        SyncAuctionLadderPacket.LotBid lot = data.lots().get(i);
                        UiRect row = new UiRect(layout.viewport().x() + 4,
                                rowY + i * (ROW_HEIGHT + ROW_GAP),
                                Math.max(1, layout.viewport().width() - 9), ROW_HEIGHT);
                        ImmortalUiSkin.drawListRow(graphics, row.x(), row.y(), row.width(), row.height(),
                                row.contains(mouseX, mouseY) ? ImmortalUiSkin.InteractionState.HOVERED
                                        : ImmortalUiSkin.InteractionState.NORMAL);
                        ImmortalUiSkin.drawStringFit(font, graphics,
                                PlayerDisplayText.safeLiteral(lot.display(),
                                        "text.seeking_immortals.unknown_auction_target").getString(),
                                row.x() + 6, row.y() + 5,
                                Math.max(1, row.width() - 12), ImmortalUiSkin.JOURNAL_PAPER, false);
                        Component status = Component.translatable("screen.seeking_immortals.auction.lot_status",
                                lot.current(), lot.next(), lot.leaderName());
                        String statusText = status.getString() + (lot.settled()
                                ? " · " + Component.translatable("screen.seeking_immortals.auction.settled").getString() : "");
                        ImmortalUiSkin.drawStringFit(font, graphics, statusText, row.x() + 6, row.y() + 16,
                                Math.max(1, row.width() - 12), lot.settled()
                                        ? ImmortalUiSkin.JOURNAL_PAPER_MUTED : ImmortalUiSkin.JOURNAL_JADE_TEXT, false);
                    }
                });
        listPanel.drawScrollbar(graphics);
    }

    private void drawEmpty(GuiGraphics graphics, UiRect viewport, String key) {
        ImmortalUiSkin.drawStringFit(font, graphics, Component.translatable(key).getString(),
                viewport.x() + 8, viewport.y() + 10, Math.max(1, viewport.width() - 16),
                ImmortalUiSkin.JOURNAL_PAPER_MUTED, false);
    }

    record HallLayout(int left, int top, int panelWidth, int panelHeight,
                      UiRect header, UiRect summary, UiRect viewport,
                      UiRect refreshButton, UiRect closeButton,
                      UiRect previousButton, UiRect nextButton) {}
}
