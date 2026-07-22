package com.xunxian.seekingimmortals.client;

import com.xunxian.seekingimmortals.catalog.AuctionSoftService;
import com.xunxian.seekingimmortals.network.AuctionActionPacket;
import com.xunxian.seekingimmortals.network.ModNetwork;
import com.xunxian.seekingimmortals.util.PlayerDisplayText;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.List;

/** Responsive catalogue auction view; all actions remain server authoritative. */
public class AuctionScreen extends AbstractJournalScreen {
    private static final int PANEL_MARGIN = 4;
    private static final int PANEL_WIDTH = 360;
    private static final int PANEL_HEIGHT = 236;
    private static final int ROW_HEIGHT = 50;
    private static final int ROW_GAP = 4;

    private int scrollOffset;
    private int contentHeight;


    @Override
    protected UiClimate defaultClimate() {
        return UiClimate.WARM_LACQUER;
    }

    public AuctionScreen() {
        super(Component.translatable("screen.seeking_immortals.auction.title"));
    }

    @Override
    protected void init() {
        super.init();
        rebuildActionWidgets();
    }

    private void rebuildActionWidgets() {
        clearWidgets();
        AuctionLayout layout = calculateLayout(width, height);
        addRenderableWidget(ImmortalButton.secondary(layout.refreshButton().x(), layout.refreshButton().y(),
                layout.refreshButton().width(), layout.refreshButton().height(),
                Component.translatable("screen.seeking_immortals.auction.refresh"), button ->
                        ModNetwork.CHANNEL.sendToServer(new AuctionActionPacket(AuctionActionPacket.ACTION_LIST, ""))));
        addRenderableWidget(ImmortalButton.secondary(layout.closeButton().x(), layout.closeButton().y(),
                layout.closeButton().width(), layout.closeButton().height(),
                Component.translatable("screen.seeking_immortals.auction.close"), button -> onClose()));

        List<AuctionSoftService.Lot> lots = AuctionSoftService.builtin().lots();
        contentHeight = calculateContentHeight(lots.size());
        scrollOffset = clampScroll(scrollOffset, contentHeight, layout.viewport().height());
        int rowY = layout.viewport().y() + 4 - scrollOffset;
        for (int i = 0; i < lots.size(); i++) {
            AuctionSoftService.Lot lot = lots.get(i);
            int y = rowY + i * (ROW_HEIGHT + ROW_GAP);
            Rect row = new Rect(layout.viewport().x() + 4, y,
                    Math.max(1, layout.viewport().width() - 9), ROW_HEIGHT);
            addLotButtons(layout.viewport(), row, lot);
        }
    }

    private void addLotButtons(Rect viewport, Rect row, AuctionSoftService.Lot lot) {
        int gap = 3;
        int buttonWidth = Math.max(1, (row.width() - gap * 2 - 8) / 3);
        int buttonY = row.y() + 29;
        int buttonX = row.x() + 4;
        ImmortalButton preview = ImmortalButton.secondary(buttonX, buttonY, buttonWidth, 17,
                Component.translatable("screen.seeking_immortals.auction.preview"), ignored ->
                        send(AuctionActionPacket.ACTION_PREVIEW, lot.id()));
        ImmortalButton settle = ImmortalButton.secondary(buttonX + buttonWidth + gap, buttonY,
                buttonWidth, 17, Component.translatable("screen.seeking_immortals.auction.settle"), ignored ->
                        send(AuctionActionPacket.ACTION_SETTLE, lot.id()));
        ImmortalButton bid = ImmortalButton.primary(buttonX + (buttonWidth + gap) * 2, buttonY,
                Math.max(1, row.right() - 4 - (buttonX + (buttonWidth + gap) * 2)), 17,
                Component.translatable("screen.seeking_immortals.auction.bid"), ignored ->
                        send(AuctionActionPacket.ACTION_BID, lot.id()));
        boolean visible = buttonY >= viewport.y() && buttonY + 17 <= viewport.bottom();
        preview.visible = visible;
        settle.visible = visible;
        bid.visible = visible;
        addRenderableWidget(preview);
        addRenderableWidget(settle);
        addRenderableWidget(bid);
    }

    private static void send(String action, String lotId) {
        ModNetwork.CHANNEL.sendToServer(new AuctionActionPacket(action, lotId));
    }

    @Override
    protected JournalChrome journalChrome() {
        AuctionLayout layout = calculateLayout(width, height);
        return new JournalChrome(layout.left(), layout.top(), layout.panelWidth(), layout.panelHeight(),
                toUi(layout.header()), null);
    }

    private static UiRect toUi(Rect rect) {
        return new UiRect(rect.x(), rect.y(), rect.width(), rect.height());
    }

    @Override
    protected void renderJournalTitle(GuiGraphics graphics, JournalChrome chrome, UiRect header) {
        AuctionLayout layout = calculateLayout(width, height);
        int titleWidth = layout.panelWidth() < 240 ? layout.header().width()
                : Math.max(1, layout.refreshButton().x() - layout.header().x() - 5);
        ImmortalUiSkin.drawStringFit(font, graphics, title.getString(), layout.header().x() + 7,
                layout.header().y() + Math.max(2, (layout.header().height() - 8) / 2), titleWidth,
                ImmortalUiSkin.JOURNAL_PAPER, false);
    }

    @Override
    protected void renderJournalContent(GuiGraphics graphics, JournalChrome chrome,
                                         int mouseX, int mouseY, float partialTick) {
        AuctionLayout layout = calculateLayout(width, height);
        AuctionSoftService.Snapshot snapshot = AuctionSoftService.builtin();
        ImmortalUiSkin.drawStringFit(font, graphics,
                Component.translatable("screen.seeking_immortals.auction.summary",
                        snapshot.venueCount(), snapshot.lotCount()).getString(),
                layout.summary().x() + 3, layout.summary().y() + 2,
                Math.max(1, layout.summary().width() - 6), ImmortalUiSkin.JOURNAL_PAPER_MUTED, false);
        ImmortalUiSkin.drawInnerFrame(graphics, layout.viewport().x(), layout.viewport().y(),
                layout.viewport().width(), layout.viewport().height());
        drawLots(graphics, layout, mouseX, mouseY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        AuctionLayout layout = calculateLayout(width, height);
        if (layout.viewport().contains(mouseX, mouseY) && contentHeight > layout.viewport().height()) {
            scrollOffset = clampScroll(scrollOffset - (int)Math.round(delta * 20.0D),
                    contentHeight, layout.viewport().height());
            rebuildActionWidgets();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    static AuctionLayout calculateLayout(int screenWidth, int screenHeight) {
        int panelWidth = Math.min(PANEL_WIDTH, Math.max(1, screenWidth - PANEL_MARGIN * 2));
        int panelHeight = Math.min(PANEL_HEIGHT, Math.max(1, screenHeight - PANEL_MARGIN * 2));
        int left = Math.max(0, (screenWidth - panelWidth) / 2);
        int top = Math.max(0, (screenHeight - panelHeight) / 2);
        int padding = panelWidth >= 180 ? 8 : 3;
        boolean compact = panelWidth < 240;
        int headerHeight = compact ? 40 : 27;
        int buttonHeight = compact ? 16 : 18;
        int buttonWidth = compact
                ? Math.max(1, (panelWidth - padding * 3) / 2)
                : Math.min(64, Math.max(1, (panelWidth - padding * 3) / 2));
        int buttonY = top + (compact ? 21 : 7);
        Rect header = new Rect(left + padding, top + 4, Math.max(1, panelWidth - padding * 2),
                compact ? 14 : 21);
        Rect refresh = new Rect(compact ? left + padding : left + panelWidth - padding * 2 - buttonWidth * 2,
                buttonY, buttonWidth, buttonHeight);
        Rect close = new Rect(refresh.right() + padding, buttonY, buttonWidth, buttonHeight);
        int summaryY = top + headerHeight + 3;
        Rect summary = new Rect(left + padding, summaryY, Math.max(1, panelWidth - padding * 2), 13);
        int viewportY = summary.y() + summary.height() + 3;
        Rect viewport = new Rect(left + padding, viewportY, Math.max(1, panelWidth - padding * 2),
                Math.max(1, top + panelHeight - padding - viewportY));
        return new AuctionLayout(left, top, panelWidth, panelHeight, header, summary, viewport, refresh, close);
    }

    static int calculateContentHeight(int rowCount) {
        return Math.max(1, rowCount * (ROW_HEIGHT + ROW_GAP) + 8);
    }

    static int clampScroll(int requested, int contentHeight, int viewportHeight) {
        int maximum = Math.max(0, contentHeight - Math.max(1, viewportHeight));
        return Math.max(0, Math.min(requested, maximum));
    }

    private void drawLots(GuiGraphics graphics, AuctionLayout layout, int mouseX, int mouseY) {
        List<AuctionSoftService.Lot> lots = AuctionSoftService.builtin().lots();
        int rowY = layout.viewport().y() + 4 - scrollOffset;
        ImmortalUiSkin.withScissor(graphics, layout.viewport().x(), layout.viewport().y(),
                layout.viewport().width(), layout.viewport().height(), () -> {
                    if (lots.isEmpty()) {
                        ImmortalUiSkin.drawStringFit(font, graphics,
                                Component.translatable("screen.seeking_immortals.auction.empty").getString(),
                                layout.viewport().x() + 8, layout.viewport().y() + 10,
                                Math.max(1, layout.viewport().width() - 16),
                                ImmortalUiSkin.JOURNAL_PAPER_MUTED, false);
                        return;
                    }
                    for (int i = 0; i < lots.size(); i++) {
                        AuctionSoftService.Lot lot = lots.get(i);
                        Rect row = new Rect(layout.viewport().x() + 4,
                                rowY + i * (ROW_HEIGHT + ROW_GAP),
                                Math.max(1, layout.viewport().width() - 9), ROW_HEIGHT);
                        boolean hovered = row.contains(mouseX, mouseY);
                        ImmortalUiSkin.drawListRow(graphics, row.x(), row.y(), row.width(), row.height(),
                                hovered ? ImmortalUiSkin.InteractionState.HOVERED
                                        : ImmortalUiSkin.InteractionState.NORMAL);
                        ImmortalUiSkin.drawStringFit(font, graphics,
                                PlayerDisplayText.safeLiteral(lot.display(),
                                        "text.seeking_immortals.unknown_auction_target").getString(),
                                row.x() + 6, row.y() + 5,
                                Math.max(1, row.width() - 12), ImmortalUiSkin.JOURNAL_PAPER, false);
                        ImmortalUiSkin.drawStringFit(font, graphics,
                                Component.translatable("screen.seeking_immortals.auction.price_range",
                                        lot.minEquiv(), lot.maxEquiv()).getString(),
                                row.x() + 6, row.y() + 16, Math.max(1, row.width() - 12),
                                ImmortalUiSkin.JOURNAL_PAPER_MUTED, false);
                    }
                });
        ImmortalUiSkin.drawThinScrollbar(graphics, layout.viewport().right() - 3,
                layout.viewport().y(), layout.viewport().height(), contentHeight,
                layout.viewport().height(), scrollOffset);
    }

    record Rect(int x, int y, int width, int height) {
        int right() { return x + width; }
        int bottom() { return y + height; }
        boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX < right() && mouseY >= y && mouseY < bottom();
        }
        boolean intersects(Rect other) {
            return other != null && x < other.right() && right() > other.x()
                    && y < other.bottom() && bottom() > other.y();
        }
    }

    record AuctionLayout(int left, int top, int panelWidth, int panelHeight,
                         Rect header, Rect summary, Rect viewport,
                         Rect refreshButton, Rect closeButton) {}
}
