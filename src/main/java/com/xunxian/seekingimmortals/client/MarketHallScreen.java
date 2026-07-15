package com.xunxian.seekingimmortals.client;

import com.xunxian.seekingimmortals.menu.MarketHallMenu;
import com.xunxian.seekingimmortals.network.ModNetwork;
import com.xunxian.seekingimmortals.network.ShopActionPacket;
import com.xunxian.seekingimmortals.shop.ShopService;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

/**
 * Wave492: market hall with multi-shop tab strip + entry pagination parity.
 */
public class MarketHallScreen extends AbstractContainerScreen<MarketHallMenu> {
    private int shopIndex;
    private int page;

    public MarketHallScreen(MarketHallMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 360;
        this.imageHeight = 236;
        this.shopIndex = 0;
        this.page = 0;
    }

    @Override
    protected void init() {
        super.init();
        rebuild();
        // Ensure data for current shop.
        String shopId = currentShopId();
        ModNetwork.CHANNEL.sendToServer(new ShopActionPacket(ShopService.ACTION_SYNC, shopId, ""));
    }

    private List<String> shopIds() {
        List<String> ids = ShopService.marketShopIds();
        return ids == null || ids.isEmpty() ? List.of(menu.shopId()) : ids;
    }

    private String currentShopId() {
        List<String> ids = shopIds();
        if (shopIndex < 0 || shopIndex >= ids.size()) {
            shopIndex = 0;
        }
        return ids.get(shopIndex);
    }

    private void rebuild() {
        clearWidgets();
        int left = leftPos;
        int top = topPos;
        int smallWidth = Math.max(50, Math.min(68, (imageWidth - 32) / 5));
        String shopId = currentShopId();
        addRenderableWidget(Button.builder(Component.translatable("screen.seeking_immortals.shop.refresh"), button ->
                        ModNetwork.CHANNEL.sendToServer(new ShopActionPacket(ShopService.ACTION_SYNC, shopId, "")))
                .bounds(left + imageWidth - smallWidth * 2 - 18, top + 8, smallWidth, 18)
                .build());
        addRenderableWidget(Button.builder(Component.translatable("screen.seeking_immortals.shop.close"), button -> onClose())
                .bounds(left + imageWidth - smallWidth - 12, top + 8, smallWidth, 18)
                .build());
        addRenderableWidget(Button.builder(Component.literal("<店"), button -> {
            shopIndex = Math.max(0, shopIndex - 1);
            page = 0;
            ModNetwork.CHANNEL.sendToServer(new ShopActionPacket(ShopService.ACTION_SYNC, currentShopId(), ""));
            rebuild();
        }).bounds(left + 12, top + 8, 36, 18).build());
        addRenderableWidget(Button.builder(Component.literal("店>"), button -> {
            List<String> ids = shopIds();
            shopIndex = Math.min(ids.size() - 1, shopIndex + 1);
            page = 0;
            ModNetwork.CHANNEL.sendToServer(new ShopActionPacket(ShopService.ACTION_SYNC, currentShopId(), ""));
            rebuild();
        }).bounds(left + 52, top + 8, 36, 18).build());
        addRenderableWidget(Button.builder(Component.literal("<"), button -> {
            page = Math.max(0, page - 1);
            rebuild();
        }).bounds(left + 12, top + imageHeight - 28, 24, 18).build());
        addRenderableWidget(Button.builder(Component.literal(">"), button -> {
            page = page + 1;
            rebuild();
        }).bounds(left + 40, top + imageHeight - 28, 24, 18).build());

        ClientShopData.Snapshot data = ClientShopData.get();
        if (!data.synced()) {
            return;
        }
        List<ClientShopData.Entry> entries = data.entries();
        int pageSize = 6;
        int maxPage = entries.isEmpty() ? 0 : (entries.size() - 1) / pageSize;
        if (page > maxPage) {
            page = maxPage;
        }
        int from = page * pageSize;
        int to = Math.min(entries.size(), from + pageSize);
        int buyWidth = Math.min(54, Math.max(1, imageWidth - 24));
        int buyX = Math.max(left + 4, left + imageWidth - buyWidth - 12);
        int rowY = top + 56;
        for (int i = from; i < to; i++) {
            ClientShopData.Entry entry = entries.get(i);
            int row = i - from;
            Button button = Button.builder(Component.translatable(
                            entry.locked() ? "screen.seeking_immortals.shop.locked" : "screen.seeking_immortals.shop.buy"), ignored ->
                            ModNetwork.CHANNEL.sendToServer(new ShopActionPacket(ShopService.ACTION_BUY, shopId, entry.id())))
                    .bounds(buyX, rowY + row * 24 - 4, buyWidth, 18)
                    .build();
            button.active = entry.remainingStock() != 0 && !entry.locked();
            addRenderableWidget(button);
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        ImmortalUiSkin.drawPanel(graphics, leftPos, topPos, imageWidth, imageHeight);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        ClientShopData.Snapshot data = ClientShopData.get();
        graphics.drawCenteredString(font, Component.translatable(data.titleKey().isBlank()
                        ? "screen.seeking_immortals.shop.market_title" : data.titleKey()),
                leftPos + imageWidth / 2, topPos + 12, ImmortalUiSkin.COLOR_TITLE);
        String shopId = data.shopId().isBlank() ? currentShopId() : data.shopId();
        ImmortalUiSkin.drawStringFit(font, graphics,
                Component.translatable("screen.seeking_immortals.shop.shop_id", shopId).getString()
                        + "  [" + (shopIndex + 1) + "/" + shopIds().size() + "] p" + (page + 1),
                leftPos + 96, topPos + 12, imageWidth - 180, ImmortalUiSkin.COLOR_TEXT_MUTED, false);
        if (!data.synced()) {
            ImmortalUiSkin.drawStringFit(font, graphics,
                    Component.translatable("screen.seeking_immortals.shop.waiting").getString(),
                    leftPos + 14, topPos + 60, imageWidth - 28, ImmortalUiSkin.COLOR_TEXT_MUTED, false);
            return;
        }
        List<ClientShopData.Entry> entries = data.entries();
        int pageSize = 6;
        int from = page * pageSize;
        int to = Math.min(entries.size(), from + pageSize);
        for (int i = from; i < to; i++) {
            ClientShopData.Entry entry = entries.get(i);
            int row = i - from;
            String line = entry.itemDescriptionId() + " x" + entry.count()
                    + "  " + entry.cost() + "  stock=" + entry.remainingStock()
                    + (entry.locked() ? " [LOCK]" : "");
            ImmortalUiSkin.drawStringFit(font, graphics, line,
                    leftPos + 16, topPos + 56 + row * 24, imageWidth - 140,
                    ImmortalUiSkin.COLOR_TEXT_NORMAL, false);
        }
        if (entries.isEmpty()) {
            ImmortalUiSkin.drawStringFit(font, graphics,
                    Component.translatable("screen.seeking_immortals.shop.empty").getString(),
                    leftPos + 14, topPos + 60, imageWidth - 28, ImmortalUiSkin.COLOR_TEXT_MUTED, false);
        }
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
    }
}
