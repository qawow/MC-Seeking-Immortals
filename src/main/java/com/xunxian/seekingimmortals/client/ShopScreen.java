package com.xunxian.seekingimmortals.client;

import com.xunxian.seekingimmortals.network.ModNetwork;
import com.xunxian.seekingimmortals.network.ShopActionPacket;
import com.xunxian.seekingimmortals.shop.ShopService;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

public class ShopScreen extends Screen {
    private static final int PANEL_WIDTH = 360;
    private static final int PANEL_HEIGHT = 236;
    private static final int LINE = 13;

    public ShopScreen() {
        super(Component.translatable("screen.seeking_immortals.shop.market_title"));
    }

    @Override
    protected void init() {
        super.init();
        ClientShopData.Snapshot data = ClientShopData.get();
        int left = left();
        int top = top();
        int panelWidth = panelWidth();
        int panelHeight = panelHeight();

        addHeaderButtons(left, top, panelWidth);
        if (!data.synced()) {
            return;
        }
        List<ClientShopData.Entry> entries = data.entries();
        int rowY = top + listTopOffset(panelHeight) + 20;
        int buyWidth = Math.min(54, Math.max(1, panelWidth - 24));
        int buyX = Math.max(left + 4, left + panelWidth - buyWidth - 12);
        int rows = Math.min(entries.size(), visibleRows(panelWidth, panelHeight));
        for (int i = 0; i < rows; i++) {
            ClientShopData.Entry entry = entries.get(i);
            Button button = Button.builder(Component.translatable(
                            entry.locked() ? "screen.seeking_immortals.shop.locked" : "screen.seeking_immortals.shop.buy"), ignored ->
                            ModNetwork.CHANNEL.sendToServer(new ShopActionPacket(ShopService.ACTION_BUY, data.shopId(), entry.id())))
                    .bounds(buyX, rowY + i * 24 - 4, buyWidth, 18)
                    .build();
            button.active = entry.remainingStock() != 0 && !entry.locked();
            addRenderableWidget(button);
        }
    }

    private void addHeaderButtons(int left, int top, int panelWidth) {
        int smallWidth = Math.max(54, Math.min(68, (panelWidth - 32) / 4));
        ClientShopData.Snapshot data = ClientShopData.get();
        String shopId = data.shopId().isBlank() ? ShopService.MARKET_HERBAL_STALL : data.shopId();
        addRenderableWidget(Button.builder(Component.translatable("screen.seeking_immortals.shop.refresh"), button ->
                        ModNetwork.CHANNEL.sendToServer(new ShopActionPacket(ShopService.ACTION_SYNC, shopId, "")))
                .bounds(left + panelWidth - smallWidth * 2 - 18, top + 8, smallWidth, 18)
                .build());
        addRenderableWidget(Button.builder(Component.translatable("screen.seeking_immortals.shop.close"), button -> onClose())
                .bounds(left + panelWidth - smallWidth - 12, top + 8, smallWidth, 18)
                .build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        renderPanel(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void renderPanel(GuiGraphics graphics) {
        int left = left();
        int top = top();
        int panelWidth = panelWidth();
        int panelHeight = panelHeight();
        ClientShopData.Snapshot data = ClientShopData.get();

        ImmortalUiSkin.drawPanel(graphics, left, top, panelWidth, panelHeight);
        graphics.drawCenteredString(font, Component.translatable(data.titleKey()), left + panelWidth / 2, top + 12,
                ImmortalUiSkin.COLOR_TITLE);
        drawLine(graphics, left, top + 36, Component.translatable("screen.seeking_immortals.shop.shop_id",
                data.shopId().isBlank() ? "-" : data.shopId()));

        int listTop = top + listTopOffset(panelHeight);
        graphics.drawString(font, Component.translatable("screen.seeking_immortals.shop.stock"), left + 14, listTop,
                ImmortalUiSkin.COLOR_TITLE, false);
        if (!data.synced()) {
            drawLine(graphics, left, listTop + 20, Component.translatable("screen.seeking_immortals.shop.waiting"));
            return;
        }
        if (data.entries().isEmpty()) {
            drawLine(graphics, left, listTop + 20, Component.translatable("screen.seeking_immortals.shop.empty"));
            return;
        }
        int rows = Math.min(data.entries().size(), visibleRows(panelWidth, panelHeight));
        int rowY = listTop + 20;
        for (int i = 0; i < rows; i++) {
            ClientShopData.Entry entry = data.entries().get(i);
            Component itemName = Component.translatable(entry.itemDescriptionId());
            Component currencyName = Component.translatable(entry.currencyDescriptionId());
            String stock = entry.remainingStock() < 0
                    ? Component.translatable("screen.seeking_immortals.shop.stock_unlimited").getString()
                    : Integer.toString(Math.max(0, entry.remainingStock()));
            String lock = entry.locked()
                    ? " [" + Component.translatable("screen.seeking_immortals.shop.rank_required",
                    entry.rankMin().isBlank() ? "?" : entry.rankMin()).getString() + "]"
                    : "";
            String text = itemName.getString() + " x" + entry.count() + " / "
                    + currencyName.getString() + " x" + entry.cost() + " / " + stock + lock;
            int color = entry.locked() || entry.remainingStock() == 0
                    ? ImmortalUiSkin.COLOR_TEXT_MUTED : ImmortalUiSkin.COLOR_TEXT_NORMAL;
            ImmortalUiSkin.drawStringFit(font, graphics, text, left + 16, rowY + i * 24,
                    Math.max(1, panelWidth - 94), color, false);
            ImmortalUiSkin.drawStringFit(font, graphics,
                    Component.translatable("screen.seeking_immortals.shop.entry_id", entry.id()).getString(),
                    left + 16, rowY + i * 24 + LINE, Math.max(1, panelWidth - 94),
                    ImmortalUiSkin.COLOR_TEXT_MUTED, false);
        }
    }

    private void drawLine(GuiGraphics graphics, int left, int y, Component text) {
        ImmortalUiSkin.drawStringFit(font, graphics, text.getString(), left + 14, y, Math.max(1, panelWidth() - 28),
                ImmortalUiSkin.COLOR_TEXT_NORMAL, false);
    }

    private int left() {
        return Math.max(0, (this.width - panelWidth()) / 2);
    }

    private int top() {
        return Math.max(0, (this.height - panelHeight()) / 2);
    }

    private int panelWidth() {
        return calculatePanelWidth(this.width);
    }

    private int panelHeight() {
        return calculatePanelHeight(this.height);
    }

    static int calculatePanelWidth(int screenWidth) {
        if (screenWidth <= 0) return 1;
        int margin = screenWidth >= 260 ? 24 : Math.min(8, Math.max(0, screenWidth / 10));
        return Math.max(1, Math.min(PANEL_WIDTH, screenWidth - margin));
    }

    static int calculatePanelHeight(int screenHeight) {
        if (screenHeight <= 0) return 1;
        int margin = screenHeight >= 180 ? 24 : Math.min(8, Math.max(0, screenHeight / 10));
        return Math.max(1, Math.min(PANEL_HEIGHT, screenHeight - margin));
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
}
