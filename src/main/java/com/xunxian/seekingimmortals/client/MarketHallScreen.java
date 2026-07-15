package com.xunxian.seekingimmortals.client;

import com.xunxian.seekingimmortals.menu.MarketHallMenu;
import com.xunxian.seekingimmortals.network.ModNetwork;
import com.xunxian.seekingimmortals.network.ShopActionPacket;
import com.xunxian.seekingimmortals.shop.ShopService;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

/** Multi-shop market hall with responsive filters, pagination and row scrolling. */
public class MarketHallScreen extends AbstractContainerScreen<MarketHallMenu> {
    private static final int PANEL_MARGIN = 4;
    private static final int PAGE_SIZE = 6;
    private static final int ROW_HEIGHT = 44;
    private static final int ROW_GAP = 4;

    private int shopIndex;
    private int page;
    private int scrollOffset;
    private int contentHeight;
    private int observedRevision = Integer.MIN_VALUE;

    public MarketHallScreen(MarketHallMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 360;
        this.imageHeight = 236;
    }

    @Override
    protected void init() {
        super.init();
        rebuild();
        syncCurrentShop();
    }

    private List<String> shopIds() {
        List<String> ids = ShopService.marketShopIds();
        return ids == null || ids.isEmpty() ? List.of(menu.shopId()) : ids;
    }

    private String currentShopId() {
        List<String> ids = shopIds();
        shopIndex = Math.max(0, Math.min(shopIndex, ids.size() - 1));
        return ids.get(shopIndex);
    }

    private void syncCurrentShop() {
        ModNetwork.CHANNEL.sendToServer(new ShopActionPacket(
                ShopService.ACTION_SYNC, currentShopId(), ""));
    }

    private void rebuild() {
        clearWidgets();
        MarketLayout layout = calculateLayout(width, height);
        List<String> ids = shopIds();
        String shopId = currentShopId();
        ClientShopData.Snapshot data = ClientShopData.get();
        boolean ready = data.synced() && (data.shopId().isBlank() || data.shopId().equals(shopId));
        List<ClientShopData.Entry> entries = ready ? data.entries() : List.of();
        int maxPage = entries.isEmpty() ? 0 : (entries.size() - 1) / PAGE_SIZE;
        page = Math.max(0, Math.min(page, maxPage));
        observedRevision = 31 * data.hashCode() + 31 * shopIndex + page;

        ImmortalButton previousShop = ImmortalButton.secondary(layout.previousShopButton().x(),
                layout.previousShopButton().y(), layout.previousShopButton().width(),
                layout.previousShopButton().height(), Component.literal("<店"), button -> {
                    shopIndex = Math.max(0, shopIndex - 1);
                    page = 0;
                    scrollOffset = 0;
                    syncCurrentShop();
                    rebuild();
                });
        previousShop.active = shopIndex > 0;
        addRenderableWidget(previousShop);
        ImmortalButton nextShop = ImmortalButton.secondary(layout.nextShopButton().x(),
                layout.nextShopButton().y(), layout.nextShopButton().width(),
                layout.nextShopButton().height(), Component.literal("店>"), button -> {
                    shopIndex = Math.min(ids.size() - 1, shopIndex + 1);
                    page = 0;
                    scrollOffset = 0;
                    syncCurrentShop();
                    rebuild();
                });
        nextShop.active = shopIndex < ids.size() - 1;
        addRenderableWidget(nextShop);
        addRenderableWidget(ImmortalButton.secondary(layout.refreshButton().x(), layout.refreshButton().y(),
                layout.refreshButton().width(), layout.refreshButton().height(),
                Component.translatable("screen.seeking_immortals.shop.refresh"), button -> syncCurrentShop()));
        addRenderableWidget(ImmortalButton.secondary(layout.closeButton().x(), layout.closeButton().y(),
                layout.closeButton().width(), layout.closeButton().height(),
                Component.translatable("screen.seeking_immortals.shop.close"), button -> onClose()));

        ImmortalButton previousPage = ImmortalButton.secondary(layout.previousPageButton().x(),
                layout.previousPageButton().y(), layout.previousPageButton().width(),
                layout.previousPageButton().height(), Component.literal("<"), button -> {
                    page = Math.max(0, page - 1);
                    scrollOffset = 0;
                    rebuild();
                });
        previousPage.active = page > 0;
        addRenderableWidget(previousPage);
        ImmortalButton nextPage = ImmortalButton.secondary(layout.nextPageButton().x(),
                layout.nextPageButton().y(), layout.nextPageButton().width(),
                layout.nextPageButton().height(), Component.literal(">"), button -> {
                    page = Math.min(maxPage, page + 1);
                    scrollOffset = 0;
                    rebuild();
                });
        nextPage.active = page < maxPage;
        addRenderableWidget(nextPage);

        int from = page * PAGE_SIZE;
        int to = Math.min(entries.size(), from + PAGE_SIZE);
        contentHeight = calculateContentHeight(to - from);
        scrollOffset = clampScroll(scrollOffset, contentHeight, layout.viewport().height());
        int rowY = layout.viewport().y() + 4 - scrollOffset;
        for (int i = from; i < to; i++) {
            ClientShopData.Entry entry = entries.get(i);
            UiRect row = new UiRect(layout.viewport().x() + 4,
                    rowY + (i - from) * (ROW_HEIGHT + ROW_GAP),
                    Math.max(1, layout.viewport().width() - 9), ROW_HEIGHT);
            int buttonWidth = Math.min(64, Math.max(1, row.width() / 3));
            int buttonY = row.y() + 23;
            ImmortalButton buy = entry.locked()
                    ? ImmortalButton.secondary(row.right() - buttonWidth - 4, buttonY, buttonWidth, 17,
                            Component.translatable("screen.seeking_immortals.shop.locked"), ignored -> {})
                    : ImmortalButton.primary(row.right() - buttonWidth - 4, buttonY, buttonWidth, 17,
                            Component.translatable("screen.seeking_immortals.shop.buy"), ignored ->
                                    ModNetwork.CHANNEL.sendToServer(new ShopActionPacket(
                                            ShopService.ACTION_BUY, shopId, entry.id())));
            buy.active = entry.remainingStock() != 0 && !entry.locked();
            buy.visible = buttonY >= layout.viewport().y() && buttonY + 17 <= layout.viewport().bottom();
            addRenderableWidget(buy);
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        int revision = 31 * ClientShopData.get().hashCode() + 31 * shopIndex + page;
        if (revision != observedRevision) {
            rebuild();
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        MarketLayout layout = calculateLayout(width, height);
        drawFrame(graphics, layout);
        drawEntries(graphics, layout, mouseX, mouseY);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        MarketLayout layout = calculateLayout(width, height);
        if (layout.viewport().contains(mouseX, mouseY) && contentHeight > layout.viewport().height()) {
            scrollOffset = clampScroll(scrollOffset - (int)Math.round(delta * 20.0D),
                    contentHeight, layout.viewport().height());
            rebuild();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
    }

    static MarketLayout calculateLayout(int screenWidth, int screenHeight) {
        int panelWidth = Math.min(360, Math.max(1, screenWidth - PANEL_MARGIN * 2));
        int panelHeight = Math.min(236, Math.max(1, screenHeight - PANEL_MARGIN * 2));
        int left = Math.max(0, (screenWidth - panelWidth) / 2);
        int top = Math.max(0, (screenHeight - panelHeight) / 2);
        int padding = panelWidth >= 180 ? 8 : 3;
        int headerHeight = 41;
        UiRect header = new UiRect(left + padding, top + 4, Math.max(1, panelWidth - padding * 2), 14);
        int controlsY = top + 21;
        int controlGap = 3;
        int controlWidth = Math.max(1, (panelWidth - padding * 2 - controlGap * 3) / 4);
        UiRect previousShop = new UiRect(left + padding, controlsY, controlWidth, 16);
        UiRect nextShop = new UiRect(previousShop.right() + controlGap, controlsY, controlWidth, 16);
        UiRect refresh = new UiRect(nextShop.right() + controlGap, controlsY, controlWidth, 16);
        UiRect close = new UiRect(refresh.right() + controlGap, controlsY,
                Math.max(1, left + panelWidth - padding - refresh.right() - controlGap), 16);
        UiRect summary = new UiRect(left + padding, top + headerHeight + 3,
                Math.max(1, panelWidth - padding * 2), 13);
        int footerY = Math.max(summary.y() + summary.height() + 4, top + panelHeight - padding - 21);
        UiRect previousPage = new UiRect(left + padding, footerY, 24, 18);
        UiRect nextPage = new UiRect(previousPage.right() + 4, footerY, 24, 18);
        int viewportY = summary.y() + summary.height() + 3;
        UiRect viewport = new UiRect(left + padding, viewportY, Math.max(1, panelWidth - padding * 2),
                Math.max(1, footerY - 4 - viewportY));
        return new MarketLayout(left, top, panelWidth, panelHeight, header, summary, viewport,
                previousShop, nextShop, refresh, close, previousPage, nextPage);
    }

    static int calculateContentHeight(int rowCount) {
        return Math.max(1, rowCount * (ROW_HEIGHT + ROW_GAP) + 8);
    }

    static int clampScroll(int requested, int contentHeight, int viewportHeight) {
        int maximum = Math.max(0, contentHeight - Math.max(1, viewportHeight));
        return Math.max(0, Math.min(requested, maximum));
    }

    private void drawFrame(GuiGraphics graphics, MarketLayout layout) {
        ImmortalUiSkin.drawLayeredPanel(graphics, layout.left(), layout.top(),
                layout.panelWidth(), layout.panelHeight());
        ImmortalUiSkin.drawTitleBar(graphics, layout.header().x(), layout.header().y(),
                layout.header().width(), layout.header().height());
        ClientShopData.Snapshot data = ClientShopData.get();
        Component heading = Component.translatable(data.titleKey().isBlank()
                ? "screen.seeking_immortals.shop.market_title" : data.titleKey());
        ImmortalUiSkin.drawStringFit(font, graphics, heading.getString(), layout.header().x() + 7,
                layout.header().y() + 3, Math.max(1, layout.header().width() - 12),
                ImmortalUiSkin.JOURNAL_BORDER, false);
        String shopId = data.shopId().isBlank() ? currentShopId() : data.shopId();
        String summary = Component.translatable("screen.seeking_immortals.shop.shop_id", shopId).getString()
                + " · " + (shopIndex + 1) + "/" + shopIds().size() + " · p" + (page + 1);
        ImmortalUiSkin.drawStringFit(font, graphics, summary, layout.summary().x() + 3,
                layout.summary().y() + 2, Math.max(1, layout.summary().width() - 6),
                ImmortalUiSkin.JOURNAL_PAPER_MUTED, false);
        ImmortalUiSkin.drawInnerFrame(graphics, layout.viewport().x(), layout.viewport().y(),
                layout.viewport().width(), layout.viewport().height());
    }

    private void drawEntries(GuiGraphics graphics, MarketLayout layout, int mouseX, int mouseY) {
        String shopId = currentShopId();
        ClientShopData.Snapshot data = ClientShopData.get();
        boolean ready = data.synced() && (data.shopId().isBlank() || data.shopId().equals(shopId));
        List<ClientShopData.Entry> entries = ready ? data.entries() : List.of();
        int from = Math.min(entries.size(), page * PAGE_SIZE);
        int to = Math.min(entries.size(), from + PAGE_SIZE);
        int rowY = layout.viewport().y() + 4 - scrollOffset;
        ImmortalUiSkin.withScissor(graphics, layout.viewport().x(), layout.viewport().y(),
                layout.viewport().width(), layout.viewport().height(), () -> {
                    if (!ready) {
                        drawEmpty(graphics, layout.viewport(), "screen.seeking_immortals.shop.waiting");
                        return;
                    }
                    if (entries.isEmpty()) {
                        drawEmpty(graphics, layout.viewport(), "screen.seeking_immortals.shop.empty");
                        return;
                    }
                    for (int i = from; i < to; i++) {
                        ClientShopData.Entry entry = entries.get(i);
                        UiRect row = new UiRect(layout.viewport().x() + 4,
                                rowY + (i - from) * (ROW_HEIGHT + ROW_GAP),
                                Math.max(1, layout.viewport().width() - 9), ROW_HEIGHT);
                        ImmortalUiSkin.drawListRow(graphics, row.x(), row.y(), row.width(), row.height(),
                                entry.locked() ? ImmortalUiSkin.InteractionState.DISABLED
                                        : row.contains(mouseX, mouseY) ? ImmortalUiSkin.InteractionState.HOVERED
                                        : ImmortalUiSkin.InteractionState.NORMAL);
                        String item = Component.translatable(entry.itemDescriptionId()).getString() + " x" + entry.count();
                        ImmortalUiSkin.drawStringFit(font, graphics, item, row.x() + 6, row.y() + 5,
                                Math.max(1, row.width() - 12), ImmortalUiSkin.JOURNAL_PAPER, false);
                        String currency = Component.translatable(entry.currencyDescriptionId()).getString();
                        String stock = entry.remainingStock() < 0
                                ? Component.translatable("screen.seeking_immortals.shop.stock_unlimited").getString()
                                : Integer.toString(entry.remainingStock());
                        String details = entry.cost() + " " + currency + " · "
                                + Component.translatable("screen.seeking_immortals.shop.stock").getString() + " " + stock;
                        if (entry.locked() && !entry.rankMin().isBlank()) {
                            details += " · " + Component.translatable(
                                    "screen.seeking_immortals.shop.rank_required", entry.rankMin()).getString();
                        }
                        int buttonWidth = Math.min(64, Math.max(1, row.width() / 3));
                        ImmortalUiSkin.drawStringFit(font, graphics, details, row.x() + 6, row.y() + 17,
                                Math.max(1, row.width() - buttonWidth - 14),
                                entry.locked() ? ImmortalUiSkin.JOURNAL_WARNING : ImmortalUiSkin.JOURNAL_JADE_TEXT, false);
                    }
                });
        ImmortalUiSkin.drawThinScrollbar(graphics, layout.viewport().right() - 3,
                layout.viewport().y(), layout.viewport().height(), contentHeight,
                layout.viewport().height(), scrollOffset);
    }

    private void drawEmpty(GuiGraphics graphics, UiRect viewport, String key) {
        ImmortalUiSkin.drawStringFit(font, graphics, Component.translatable(key).getString(),
                viewport.x() + 8, viewport.y() + 10, Math.max(1, viewport.width() - 16),
                ImmortalUiSkin.JOURNAL_PAPER_MUTED, false);
    }

    record UiRect(int x, int y, int width, int height) {
        int right() { return x + width; }
        int bottom() { return y + height; }
        boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX < right() && mouseY >= y && mouseY < bottom();
        }
    }

    record MarketLayout(int left, int top, int panelWidth, int panelHeight,
                        UiRect header, UiRect summary, UiRect viewport,
                        UiRect previousShopButton, UiRect nextShopButton,
                        UiRect refreshButton, UiRect closeButton,
                        UiRect previousPageButton, UiRect nextPageButton) {}
}
