package com.xunxian.seekingimmortals.client;

import com.xunxian.seekingimmortals.catalog.AuctionSoftService;
import com.xunxian.seekingimmortals.network.AuctionActionPacket;
import com.xunxian.seekingimmortals.network.ModNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Lightweight auction house GUI over AuctionSoftService.
 * Bid/preview go through AuctionActionPacket (server authoritative).
 */
public class AuctionScreen extends Screen {
    private static final int PANEL_WIDTH = 360;
    private static final int PANEL_HEIGHT = 236;

    public AuctionScreen() {
        super(Component.translatable("screen.seeking_immortals.auction.title"));
    }

    @Override
    protected void init() {
        super.init();
        int left = left();
        int top = top();
        int panelWidth = panelWidth();
        int smallWidth = Math.max(54, Math.min(72, (panelWidth - 32) / 4));

        addRenderableWidget(Button.builder(Component.translatable("screen.seeking_immortals.auction.refresh"), button ->
                        ModNetwork.CHANNEL.sendToServer(new AuctionActionPacket(AuctionActionPacket.ACTION_LIST, "")))
                .bounds(left + panelWidth - smallWidth * 2 - 18, top + 8, smallWidth, 18)
                .build());
        addRenderableWidget(Button.builder(Component.translatable("screen.seeking_immortals.auction.close"), button -> onClose())
                .bounds(left + panelWidth - smallWidth - 12, top + 8, smallWidth, 18)
                .build());

        List<AuctionSoftService.Lot> lots = AuctionSoftService.builtin().lots();
        int rows = Math.min(lots.size(), 6);
        int rowY = top + 56;
        int buyWidth = Math.min(54, Math.max(1, panelWidth - 24));
        int buyX = Math.max(left + 4, left + panelWidth - buyWidth - 12);
        for (int i = 0; i < rows; i++) {
            AuctionSoftService.Lot lot = lots.get(i);
            addRenderableWidget(Button.builder(Component.translatable("screen.seeking_immortals.auction.bid"), ignored ->
                            ModNetwork.CHANNEL.sendToServer(new AuctionActionPacket(AuctionActionPacket.ACTION_BID, lot.id())))
                    .bounds(buyX, rowY + i * 24 - 4, buyWidth, 18)
                    .build());
            addRenderableWidget(Button.builder(Component.translatable("screen.seeking_immortals.auction.settle"), ignored ->
                            ModNetwork.CHANNEL.sendToServer(new AuctionActionPacket(AuctionActionPacket.ACTION_SETTLE, lot.id())))
                    .bounds(buyX - buyWidth - 4, rowY + i * 24 - 4, buyWidth, 18)
                    .build());
            addRenderableWidget(Button.builder(Component.translatable("screen.seeking_immortals.auction.preview"), ignored ->
                            ModNetwork.CHANNEL.sendToServer(new AuctionActionPacket(AuctionActionPacket.ACTION_PREVIEW, lot.id())))
                    .bounds(buyX - buyWidth * 2 - 8, rowY + i * 24 - 4, buyWidth, 18)
                    .build());
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        int left = left();
        int top = top();
        int panelWidth = panelWidth();
        int panelHeight = panelHeight();
        ImmortalUiSkin.drawPanel(graphics, left, top, panelWidth, panelHeight);
        graphics.drawCenteredString(font, Component.translatable("screen.seeking_immortals.auction.title"),
                left + panelWidth / 2, top + 12, ImmortalUiSkin.COLOR_TITLE);

        AuctionSoftService.Snapshot snapshot = AuctionSoftService.builtin();
        ImmortalUiSkin.drawStringFit(font, graphics,
                Component.translatable("screen.seeking_immortals.auction.summary",
                        snapshot.venueCount(), snapshot.lotCount()).getString(),
                left + 14, top + 34, Math.max(1, panelWidth - 28), ImmortalUiSkin.COLOR_TEXT_NORMAL, false);

        List<AuctionSoftService.Lot> lots = snapshot.lots();
        int rows = Math.min(lots.size(), 6);
        int rowY = top + 56;
        for (int i = 0; i < rows; i++) {
            AuctionSoftService.Lot lot = lots.get(i);
            String text = lot.display() + " [" + lot.id() + "] " + lot.minEquiv() + "-" + lot.maxEquiv();
            ImmortalUiSkin.drawStringFit(font, graphics, text, left + 16, rowY + i * 24,
                    Math.max(1, panelWidth - 140), ImmortalUiSkin.COLOR_TEXT_NORMAL, false);
        }
        if (lots.isEmpty()) {
            ImmortalUiSkin.drawStringFit(font, graphics,
                    Component.translatable("screen.seeking_immortals.auction.empty").getString(),
                    left + 14, top + 60, Math.max(1, panelWidth - 28), ImmortalUiSkin.COLOR_TEXT_MUTED, false);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private int left() {
        return Math.max(0, (this.width - panelWidth()) / 2);
    }

    private int top() {
        return Math.max(0, (this.height - panelHeight()) / 2);
    }

    private int panelWidth() {
        return Math.max(1, Math.min(PANEL_WIDTH, this.width - 24));
    }

    private int panelHeight() {
        return Math.max(1, Math.min(PANEL_HEIGHT, this.height - 24));
    }
}
