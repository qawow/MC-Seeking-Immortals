package com.xunxian.seekingimmortals.client;

import com.xunxian.seekingimmortals.network.ModNetwork;
import com.xunxian.seekingimmortals.network.ShopActionPacket;
import com.xunxian.seekingimmortals.shop.ShopService;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.List;

public class ShopScreen extends AbstractJournalScreen {
    private static final int PANEL_WIDTH = 360;
    private static final int PANEL_HEIGHT = 236;
    private static final int PANEL_MARGIN = 4;
    private static final int LINE = 13;
    private static final int ROW_HEIGHT = 26;

    private int listScroll;

    public ShopScreen() {
        super(Component.translatable("screen.seeking_immortals.shop.market_title"));
    }

    @Override
    protected void init() {
        super.init();
        rebuildActionWidgets();
    }

    private void rebuildActionWidgets() {
        clearWidgets();
        Layout layout = calculateLayout(width, height);
        ClientShopData.Snapshot data = ClientShopData.get();
        String shopId = data.shopId().isBlank() ? ShopService.MARKET_HERBAL_STALL : data.shopId();

        addRenderableWidget(ImmortalButton.secondary(layout.refreshButton().x(), layout.refreshButton().y(),
                layout.refreshButton().width(), layout.refreshButton().height(),
                Component.translatable("screen.seeking_immortals.shop.refresh"), button ->
                        ModNetwork.CHANNEL.sendToServer(new ShopActionPacket(
                                ShopService.ACTION_SYNC, shopId, ""))));
        addRenderableWidget(ImmortalButton.secondary(layout.closeButton().x(), layout.closeButton().y(),
                layout.closeButton().width(), layout.closeButton().height(),
                Component.translatable("screen.seeking_immortals.shop.close"), button -> onClose()));

        if (!data.synced()) {
            listScroll = 0;
            return;
        }
        List<ClientShopData.Entry> entries = data.entries();
        int visible = visibleRows(layout);
        listScroll = Mth.clamp(listScroll, 0, Math.max(0, entries.size() - visible));
        for (int row = 0; row < visible && listScroll + row < entries.size(); row++) {
            ClientShopData.Entry entry = entries.get(listScroll + row);
            Rect action = rowAction(layout, row);
            ImmortalButton button = ImmortalButton.primary(action.x(), action.y(), action.width(), action.height(),
                    Component.translatable(entry.locked()
                            ? "screen.seeking_immortals.shop.locked"
                            : "screen.seeking_immortals.shop.buy"), ignored ->
                    ModNetwork.CHANNEL.sendToServer(new ShopActionPacket(
                            ShopService.ACTION_BUY, data.shopId(), entry.id())));
            button.active = entry.remainingStock() != 0 && !entry.locked();
            addRenderableWidget(button);
        }
    }

    @Override
    protected JournalChrome journalChrome() {
        Layout layout = calculateLayout(width, height);
        // Content paints its own stock frame; skip shared single inner frame.
        return new JournalChrome(layout.left(), layout.top(), layout.panelWidth(), layout.panelHeight(),
                toUi(layout.header()), null);
    }

    @Override
    protected void renderJournalTitle(GuiGraphics graphics, JournalChrome chrome, UiRect header) {
        Layout layout = calculateLayout(width, height);
        ClientShopData.Snapshot data = ClientShopData.get();
        ImmortalUiSkin.drawStringFit(font, graphics, Component.translatable(data.titleKey()).getString(),
                layout.titleArea().x(), layout.titleArea().y() + Math.max(2, (layout.titleArea().height() - 8) / 2),
                layout.titleArea().width(), ImmortalUiSkin.JOURNAL_BORDER, false);
    }

    @Override
    protected void renderJournalContent(GuiGraphics graphics, JournalChrome chrome,
                                         int mouseX, int mouseY, float partialTick) {
        Layout layout = calculateLayout(width, height);
        ClientShopData.Snapshot data = ClientShopData.get();

        if (layout.info().height() >= 9) {
            ImmortalUiSkin.drawStringFit(font, graphics,
                    Component.translatable("screen.seeking_immortals.shop.shop_id",
                            data.shopId().isBlank() ? "-" : data.shopId()).getString(),
                    layout.info().x(), layout.info().y() + 2, layout.info().width(),
                    ImmortalUiSkin.JOURNAL_PAPER_MUTED, false);
        }

        ImmortalUiSkin.drawInnerFrame(graphics, layout.content().x(), layout.content().y(),
                layout.content().width(), layout.content().height());
        if (layout.content().height() >= 18) {
            ImmortalUiSkin.drawStringFit(font, graphics,
                    Component.translatable("screen.seeking_immortals.shop.stock").getString(),
                    layout.content().x() + 5, layout.content().y() + 4,
                    Math.max(1, layout.content().width() - 10), ImmortalUiSkin.JOURNAL_BORDER, false);
        }

        Rect viewport = listViewport(layout);
        if (!data.synced()) {
            drawNotice(graphics, viewport,
                    Component.translatable("screen.seeking_immortals.shop.waiting"));
            return;
        }
        if (data.entries().isEmpty()) {
            drawNotice(graphics, viewport,
                    Component.translatable("screen.seeking_immortals.shop.empty"));
            return;
        }

        int visible = visibleRows(layout);
        int hoveredRow = hoveredRow(layout, mouseX, mouseY);
        listScroll = Mth.clamp(listScroll, 0, Math.max(0, data.entries().size() - visible));
        ImmortalUiSkin.withScissor(graphics, viewport.x(), viewport.y(), viewport.width(), viewport.height(), () -> {
            for (int row = 0; row < visible && listScroll + row < data.entries().size(); row++) {
                ClientShopData.Entry entry = data.entries().get(listScroll + row);
                Rect rowRect = rowRect(layout, row);
                boolean disabled = entry.locked() || entry.remainingStock() == 0;
                ImmortalUiSkin.InteractionState state = disabled
                        ? ImmortalUiSkin.InteractionState.DISABLED
                        : hoveredRow == row ? ImmortalUiSkin.InteractionState.HOVERED
                        : ImmortalUiSkin.InteractionState.NORMAL;
                ImmortalUiSkin.drawListRow(graphics, rowRect.x(), rowRect.y(), rowRect.width(), rowRect.height(), state);
                renderEntry(graphics, layout, rowRect, entry, disabled);
            }
        });
        ImmortalUiSkin.drawThinScrollbar(graphics, layout.content().right() - 3,
                viewport.y(), viewport.height(), data.entries().size() * layout.rowHeight(),
                viewport.height(), listScroll * layout.rowHeight());
    }

    private static UiRect toUi(Rect rect) {
        return new UiRect(rect.x(), rect.y(), rect.width(), rect.height());
    }

    private void renderEntry(GuiGraphics graphics, Layout layout, Rect row,
                             ClientShopData.Entry entry, boolean disabled) {
        Rect action = rowAction(layout, (row.y() - listViewport(layout).y()) / layout.rowHeight());
        int textWidth = Math.max(1, action.x() - row.x() - 8);
        Component itemName = Component.translatable(entry.itemDescriptionId());
        Component currencyName = Component.translatable(entry.currencyDescriptionId());
        String stock = entry.remainingStock() < 0
                ? Component.translatable("screen.seeking_immortals.shop.stock_unlimited").getString()
                : Integer.toString(Math.max(0, entry.remainingStock()));
        String lock = entry.locked()
                ? " [" + Component.translatable("screen.seeking_immortals.shop.rank_required",
                entry.rankMin().isBlank() ? "?" : entry.rankMin()).getString() + "]"
                : "";
        String summary = itemName.getString() + " x" + entry.count() + " / "
                + currencyName.getString() + " x" + entry.cost() + " / " + stock + lock;
        int color = disabled ? ImmortalUiSkin.JOURNAL_PAPER_MUTED : ImmortalUiSkin.JOURNAL_PAPER;
        ImmortalUiSkin.drawStringFit(font, graphics, summary, row.x() + 4, row.y() + 3,
                textWidth, color, false);
        if (layout.rowHeight() >= 24) {
            ImmortalUiSkin.drawStringFit(font, graphics,
                    Component.translatable("screen.seeking_immortals.shop.entry_id", entry.id()).getString(),
                    row.x() + 4, row.y() + LINE, textWidth,
                    disabled ? ImmortalUiSkin.JOURNAL_PAPER_MUTED : ImmortalUiSkin.JOURNAL_JADE_TEXT, false);
        }
    }

    private void drawNotice(GuiGraphics graphics, Rect viewport, Component text) {
        ImmortalUiSkin.drawStringFit(font, graphics, text.getString(), viewport.x() + 2, viewport.y() + 2,
                Math.max(1, viewport.width() - 4), ImmortalUiSkin.JOURNAL_PAPER_MUTED, false);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        Layout layout = calculateLayout(width, height);
        if (listViewport(layout).contains(mouseX, mouseY) && delta != 0.0D) {
            int max = Math.max(0, ClientShopData.get().entries().size() - visibleRows(layout));
            int next = Mth.clamp(listScroll - (int)Math.signum(delta), 0, max);
            if (next != listScroll) {
                listScroll = next;
                rebuildActionWidgets();
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    static Layout calculateLayout(int screenWidth, int screenHeight) {
        int panelWidth = calculatePanelWidth(screenWidth);
        int panelHeight = calculatePanelHeight(screenHeight);
        int left = Math.max(0, (screenWidth - panelWidth) / 2);
        int top = Math.max(0, (screenHeight - panelHeight) / 2);
        int padding = panelWidth >= 220 ? 10 : 5;
        int innerX = left + padding;
        int innerWidth = Math.max(1, panelWidth - padding * 2);
        int headerHeight = panelHeight >= 150 ? 36 : 20;
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
        int infoHeight = panelHeight >= 120 ? 20 : panelHeight >= 88 ? 10 : 0;
        Rect info = new Rect(innerX, header.bottom() + 2, innerWidth, infoHeight);
        int contentY = info.bottom() + (infoHeight > 0 ? 2 : 1);
        int contentBottom = top + panelHeight - 5;
        Rect content = new Rect(innerX, contentY, innerWidth, Math.max(1, contentBottom - contentY));
        int rowHeight = content.height() >= 52 ? ROW_HEIGHT : 20;
        return new Layout(left, top, panelWidth, panelHeight, header, titleArea, info, content,
                refresh, close, rowHeight);
    }

    private static Rect listViewport(Layout layout) {
        Rect content = layout.content();
        int heading = content.height() >= 18 ? 17 : 2;
        return new Rect(content.x() + 3, content.y() + heading,
                Math.max(1, content.width() - 8), Math.max(1, content.height() - heading - 3));
    }

    private static int visibleRows(Layout layout) {
        return Math.max(1, listViewport(layout).height() / layout.rowHeight());
    }

    private static Rect rowRect(Layout layout, int row) {
        Rect viewport = listViewport(layout);
        return new Rect(viewport.x(), viewport.y() + row * layout.rowHeight(),
                viewport.width(), layout.rowHeight());
    }

    private static Rect rowAction(Layout layout, int row) {
        Rect item = rowRect(layout, row);
        int width = Math.max(24, Math.min(54, item.width() / 3));
        int height = Math.max(12, Math.min(18, item.height() - 4));
        return new Rect(item.right() - width - 3, item.y() + Math.max(1, (item.height() - height) / 2),
                width, height);
    }

    private static int hoveredRow(Layout layout, double mouseX, double mouseY) {
        Rect viewport = listViewport(layout);
        if (!viewport.contains(mouseX, mouseY)) return -1;
        int row = (int)((mouseY - viewport.y()) / layout.rowHeight());
        return row < visibleRows(layout) ? row : -1;
    }

    static int calculatePanelWidth(int screenWidth) {
        return Math.min(PANEL_WIDTH, Math.max(1, screenWidth - PANEL_MARGIN * 2));
    }

    static int calculatePanelHeight(int screenHeight) {
        return Math.min(PANEL_HEIGHT, Math.max(1, screenHeight - PANEL_MARGIN * 2));
    }

    static int listTopOffset(int panelHeight) {
        int preferred = Math.min(72, Math.max(52, panelHeight - 110));
        int maxOffset = Math.max(18, panelHeight - 54);
        int minOffset = Math.min(panelHeight < 120 ? 34 : 52, maxOffset);
        return Math.max(18, Math.max(minOffset, Math.min(preferred, maxOffset)));
    }

    static int visibleRows(int panelWidth, int panelHeight) {
        int available = panelHeight - listTopOffset(panelHeight) - (panelWidth < 300 ? 42 : 28);
        return Math.max(0, Math.min(6, available / 24));
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
                  Rect titleArea, Rect info, Rect content, Rect refreshButton,
                  Rect closeButton, int rowHeight) {
    }
}
