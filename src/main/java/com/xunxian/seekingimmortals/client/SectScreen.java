package com.xunxian.seekingimmortals.client;

import com.xunxian.seekingimmortals.network.ModNetwork;
import com.xunxian.seekingimmortals.network.SectActionPacket;
import com.xunxian.seekingimmortals.sect.SectContributionService;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

public class SectScreen extends Screen {
    private static final int PANEL_WIDTH = 420;
    private static final int PANEL_HEIGHT = 272;
    private static final int LINE = 13;

    private Tab tab = Tab.DIALOGUE;

    public SectScreen() {
        super(Component.translatable("screen.seeking_immortals.sect.title"));
    }

    @Override
    protected void init() {
        super.init();
        ClientSectData.Snapshot data = ClientSectData.get();
        int left = left();
        int top = top();
        int panelWidth = width();
        int panelHeight = height();

        addHeaderButtons(left, top, panelWidth);
        if (!data.member()) {
            addCandidateButtons(left, top, panelWidth, data);
            return;
        }
        addTabButtons(left, top, panelWidth);
        switch (tab) {
            case DIALOGUE -> addDialogueButtons(left, top, panelWidth, panelHeight, data);
            case MISSION -> addMissionButtons(left, top, panelWidth, panelHeight, data);
            case SHOP -> addShopButtons(left, top, panelWidth, panelHeight, data);
            case PROGRESS -> addProgressButtons(left, top, panelWidth, panelHeight);
        }
    }

    private void addHeaderButtons(int left, int top, int panelWidth) {
        int smallWidth = Math.max(54, Math.min(68, (panelWidth - 32) / 5));
        addRenderableWidget(Button.builder(Component.translatable("screen.seeking_immortals.sect.refresh"), button ->
                        ModNetwork.CHANNEL.sendToServer(new SectActionPacket(SectContributionService.ACTION_SYNC, "")))
                .bounds(left + panelWidth - smallWidth * 2 - 18, top + 8, smallWidth, 18)
                .build());
        addRenderableWidget(Button.builder(Component.translatable("screen.seeking_immortals.sect.close"), button -> onClose())
                .bounds(left + panelWidth - smallWidth - 12, top + 8, smallWidth, 18)
                .build());
    }

    private void addCandidateButtons(int left, int top, int panelWidth, ClientSectData.Snapshot data) {
        int y = top + 116;
        int gap = 4;
        int columns = panelWidth >= 360 ? 2 : 1;
        int buttonWidth = Math.max(1, (panelWidth - 28 - gap * (columns - 1)) / columns);
        List<ClientSectData.Candidate> candidates = data.candidates();
        for (int i = 0; i < candidates.size(); i++) {
            ClientSectData.Candidate candidate = candidates.get(i);
            int column = i % columns;
            int row = i / columns;
            int x = left + 14 + column * (buttonWidth + gap);
            int buttonY = y + row * 22;
            addRenderableWidget(Button.builder(Component.literal(candidate.displayZh()), button ->
                            ModNetwork.CHANNEL.sendToServer(new SectActionPacket(SectContributionService.ACTION_APPLY, candidate.id())))
                    .bounds(x, buttonY, buttonWidth, 18)
                    .build());
        }
    }

    private void addTabButtons(int left, int top, int panelWidth) {
        int y = top + 83;
        int gap = 4;
        int buttonWidth = Math.max(1, (panelWidth - 28 - gap * 3) / 4);
        addTabButton(Tab.DIALOGUE, left + 14, y, buttonWidth);
        addTabButton(Tab.MISSION, left + 14 + buttonWidth + gap, y, buttonWidth);
        addTabButton(Tab.SHOP, left + 14 + (buttonWidth + gap) * 2, y, buttonWidth);
        addTabButton(Tab.PROGRESS, left + 14 + (buttonWidth + gap) * 3, y, buttonWidth);
    }

    private void addTabButton(Tab target, int x, int y, int width) {
        addRenderableWidget(Button.builder(Component.translatable(target.key), button -> {
                    tab = target;
                    clearWidgets();
                    init();
                })
                .bounds(x, y, width, 18)
                .build());
    }

    private void addDialogueButtons(int left, int top, int panelWidth, int panelHeight, ClientSectData.Snapshot data) {
        int y = contentBottomButtonY(top, panelHeight);
        List<ClientSectData.DialogueOption> options = data.dialogue().options();
        int visible = Math.min(3, options.size());
        int gap = 4;
        int buttonWidth = Math.max(1, (panelWidth - 28 - gap * Math.max(0, visible - 1)) / Math.max(1, visible));
        for (int i = 0; i < visible; i++) {
            ClientSectData.DialogueOption option = options.get(i);
            addRenderableWidget(Button.builder(Component.translatable(option.labelKey()), button ->
                            ModNetwork.CHANNEL.sendToServer(new SectActionPacket(SectContributionService.ACTION_DIALOGUE, option.id())))
                    .bounds(left + 14 + i * (buttonWidth + gap), y, buttonWidth, 20)
                    .build());
        }
    }

    private void addMissionButtons(int left, int top, int panelWidth, int panelHeight, ClientSectData.Snapshot data) {
        int y = contentBottomButtonY(top, panelHeight);
        int gap = 4;
        int buttonWidth = Math.max(1, (panelWidth - 28 - gap) / 2);
        addRenderableWidget(Button.builder(Component.translatable("screen.seeking_immortals.sect.mission.accept"), button ->
                        ModNetwork.CHANNEL.sendToServer(new SectActionPacket(SectContributionService.ACTION_ACCEPT_MISSION, data.mission().id())))
                .bounds(left + 14, y, buttonWidth, 20)
                .build());
        addRenderableWidget(Button.builder(Component.translatable("screen.seeking_immortals.sect.mission.turn_in"), button ->
                        ModNetwork.CHANNEL.sendToServer(new SectActionPacket(SectContributionService.ACTION_TURN_IN_MISSION, data.mission().id())))
                .bounds(left + 14 + buttonWidth + gap, y, buttonWidth, 20)
                .build());
    }

    private void addShopButtons(int left, int top, int panelWidth, int panelHeight, ClientSectData.Snapshot data) {
        List<ClientSectData.ShopEntry> entries = data.shopEntries();
        int rowY = top + shopTopOffset(panelHeight) + 20;
        int visibleRows = visibleShopRows(panelWidth, panelHeight);
        int buyWidth = Math.min(54, Math.max(1, panelWidth - 24));
        int buyX = Math.max(left + 4, left + panelWidth - buyWidth - 12);
        for (int i = 0; i < Math.min(entries.size(), visibleRows); i++) {
            ClientSectData.ShopEntry entry = entries.get(i);
            int buttonY = rowY + i * 22 - 4;
            addRenderableWidget(Button.builder(Component.translatable("screen.seeking_immortals.sect.buy"), button ->
                            ModNetwork.CHANNEL.sendToServer(new SectActionPacket(SectContributionService.ACTION_BUY, entry.id())))
                    .bounds(buyX, buttonY, buyWidth, 18)
                    .build());
        }
    }

    private void addProgressButtons(int left, int top, int panelWidth, int panelHeight) {
        int y = contentBottomButtonY(top, panelHeight);
        int buttonWidth = Math.max(1, panelWidth - 28);
        addRenderableWidget(Button.builder(Component.translatable("screen.seeking_immortals.sect.advance"), button ->
                        ModNetwork.CHANNEL.sendToServer(new SectActionPacket(SectContributionService.ACTION_ADVANCE, "")))
                .bounds(left + 14, y, buttonWidth, 20)
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
        int panelWidth = width();
        int panelHeight = height();
        ClientSectData.Snapshot data = ClientSectData.get();
        ImmortalUiSkin.drawPanel(graphics, left, top, panelWidth, panelHeight);
        Component heading = data.member()
                ? Component.literal(safe(data.currentSectDisplay()))
                : Component.literal(safe(data.sectDisplay().isBlank() ? data.currentSectDisplay() : data.sectDisplay()));
        graphics.drawCenteredString(font, heading, left + panelWidth / 2, top + 12, ImmortalUiSkin.COLOR_TITLE);

        int y = top + 34;
        drawLine(graphics, left, y, Component.translatable("screen.seeking_immortals.sect.current",
                safe(data.currentSectDisplay()), safe(data.role().isBlank() ? "-" : data.role())));
        y += LINE;
        drawLine(graphics, left, y, Component.translatable("screen.seeking_immortals.sect.contribution", data.contribution()));
        y += LINE;
        drawLine(graphics, left, y, Component.translatable("screen.seeking_immortals.sect.gates",
                bool(data.sevenMysteriesComplete()), bool(data.yueArrived())));
        y += LINE;
        drawLine(graphics, left, y, Component.translatable("screen.seeking_immortals.sect.stage", Component.translatable(data.stageKey())));
        y += LINE + 2;
        drawWrapped(graphics, left + 14, y, panelWidth - 28, Component.translatable(data.objectiveKey()), 2);

        if (!data.member()) {
            renderCandidates(graphics, left, top, panelWidth, data);
            return;
        }

        switch (tab) {
            case DIALOGUE -> renderDialogue(graphics, left, top, panelWidth, panelHeight, data);
            case MISSION -> renderMission(graphics, left, top, panelWidth, data);
            case SHOP -> renderShop(graphics, left, top, panelWidth, panelHeight, data);
            case PROGRESS -> renderProgress(graphics, left, top, panelWidth, data);
        }
    }

    private void renderCandidates(GuiGraphics graphics, int left, int top, int panelWidth, ClientSectData.Snapshot data) {
        int y = top + 92;
        graphics.drawString(font, Component.translatable("screen.seeking_immortals.sect.candidates"), left + 14, y, ImmortalUiSkin.COLOR_TITLE, false);
        if (data.candidates().isEmpty()) {
            drawWrapped(graphics, left + 14, y + 18, panelWidth - 28,
                    Component.translatable("screen.seeking_immortals.sect.candidates_empty"), 3);
            return;
        }
        int textY = y + 18;
        int maxRows = Math.min(6, data.candidates().size());
        for (int i = 0; i < maxRows; i++) {
            ClientSectData.Candidate candidate = data.candidates().get(i);
            String text = candidate.displayZh() + " / " + candidate.focusKey();
            ImmortalUiSkin.drawStringFit(font, graphics, text, left + 16, textY + i * LINE,
                    Math.max(1, panelWidth - 34), ImmortalUiSkin.COLOR_TEXT_NORMAL, false);
        }
    }

    private void renderDialogue(GuiGraphics graphics, int left, int top, int panelWidth, int panelHeight, ClientSectData.Snapshot data) {
        int y = top + 112;
        graphics.drawString(font, Component.translatable(data.dialogue().titleKey()), left + 14, y, ImmortalUiSkin.COLOR_TITLE, false);
        drawWrapped(graphics, left + 14, y + 18, panelWidth - 28, Component.translatable(data.dialogue().textKey()),
                Math.max(1, (contentBottomButtonY(top, panelHeight) - y - 22) / LINE));
    }

    private void renderMission(GuiGraphics graphics, int left, int top, int panelWidth, ClientSectData.Snapshot data) {
        int y = top + 112;
        graphics.drawString(font, Component.translatable("screen.seeking_immortals.sect.mission"), left + 14, y, ImmortalUiSkin.COLOR_TITLE, false);
        if (!data.mission().available()) {
            graphics.drawString(font, Component.translatable("screen.seeking_immortals.sect.mission_empty"), left + 14, y + 18,
                    ImmortalUiSkin.COLOR_TEXT_MUTED, false);
            return;
        }
        graphics.drawString(font, Component.translatable(data.mission().titleKey()), left + 14, y + 18, ImmortalUiSkin.COLOR_TEXT_NORMAL, false);
        drawWrapped(graphics, left + 14, y + 34, panelWidth - 28, Component.translatable(data.mission().objectiveKey(),
                data.mission().target(), Component.translatable(data.mission().itemDescriptionId())), 3);
        String statusKey = data.mission().completed()
                ? "screen.seeking_immortals.sect.mission.status.completed"
                : data.mission().accepted()
                ? "screen.seeking_immortals.sect.mission.status.accepted"
                : "screen.seeking_immortals.sect.mission.status.available";
        drawLine(graphics, left, y + 78, Component.translatable(statusKey, data.mission().rewardContribution()));
    }

    private void renderShop(GuiGraphics graphics, int left, int top, int panelWidth, int panelHeight, ClientSectData.Snapshot data) {
        int shopTop = top + shopTopOffset(panelHeight);
        graphics.drawString(font, Component.translatable("screen.seeking_immortals.sect.shop"), left + 14, shopTop, ImmortalUiSkin.COLOR_TITLE, false);
        List<ClientSectData.ShopEntry> entries = data.shopEntries();
        if (entries.isEmpty()) {
            graphics.drawString(font, Component.translatable("screen.seeking_immortals.sect.shop_empty"), left + 14, shopTop + 18, ImmortalUiSkin.COLOR_TEXT_MUTED, false);
            return;
        }
        int rowY = shopTop + 20;
        for (int i = 0; i < Math.min(entries.size(), visibleShopRows(panelWidth, panelHeight)); i++) {
            ClientSectData.ShopEntry entry = entries.get(i);
            Component itemName = Component.translatable(entry.itemDescriptionId());
            String text = entry.id() + " / " + itemName.getString() + " x" + entry.count() + " / " + entry.cost();
            ImmortalUiSkin.drawStringFit(font, graphics, text, left + 16, rowY + i * 22, Math.max(1, panelWidth - 98),
                    data.contribution() >= entry.cost() ? ImmortalUiSkin.COLOR_TEXT_NORMAL : ImmortalUiSkin.COLOR_TEXT_MUTED, false);
        }
    }

    private void renderProgress(GuiGraphics graphics, int left, int top, int panelWidth, ClientSectData.Snapshot data) {
        int y = top + 112;
        graphics.drawString(font, Component.translatable("screen.seeking_immortals.sect.progress"), left + 14, y, ImmortalUiSkin.COLOR_TITLE, false);
        y += 18;
        drawWrapped(graphics, left + 14, y, panelWidth - 28, Component.translatable(data.objectiveKey()), 4);
        y += LINE * 5;
        drawLine(graphics, left, y, Component.translatable("screen.seeking_immortals.sect.stage", Component.translatable(data.stageKey())));
    }

    private void drawLine(GuiGraphics graphics, int left, int y, Component text) {
        ImmortalUiSkin.drawStringFit(font, graphics, text.getString(), left + 14, y, Math.max(1, width() - 28),
                ImmortalUiSkin.COLOR_TEXT_NORMAL, false);
    }

    private void drawWrapped(GuiGraphics graphics, int x, int y, int maxWidth, Component text, int maxLines) {
        int lines = 0;
        for (net.minecraft.util.FormattedCharSequence line : font.split(text, Math.max(1, maxWidth))) {
            if (lines >= maxLines) {
                break;
            }
            graphics.drawString(font, line, x, y, ImmortalUiSkin.COLOR_TEXT_MUTED, false);
            y += LINE;
            lines++;
        }
    }

    private String bool(boolean value) {
        return Component.translatable(value ? "message.seeking_immortals.sect.yes" : "message.seeking_immortals.sect.no").getString();
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private int left() {
        return Math.max(0, (this.width - width()) / 2);
    }

    private int top() {
        return Math.max(0, (this.height - height()) / 2);
    }

    private int width() {
        return calculatePanelWidth(this.width);
    }

    private int height() {
        return calculatePanelHeight(this.height);
    }

    private static int contentBottomButtonY(int top, int panelHeight) {
        return Math.max(top + 102, top + panelHeight - 28);
    }

    static int calculatePanelWidth(int screenWidth) {
        if (screenWidth <= 0) return 1;
        int margin = screenWidth >= 260 ? 24 : Math.min(8, Math.max(0, screenWidth / 10));
        return Math.max(1, Math.min(PANEL_WIDTH, screenWidth - margin));
    }

    static int calculatePanelHeight(int screenHeight) {
        if (screenHeight <= 0) return 1;
        int margin = screenHeight >= 190 ? 24 : Math.min(8, Math.max(0, screenHeight / 10));
        return Math.max(1, Math.min(PANEL_HEIGHT, screenHeight - margin));
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
