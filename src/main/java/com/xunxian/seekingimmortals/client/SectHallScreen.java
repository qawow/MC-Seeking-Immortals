package com.xunxian.seekingimmortals.client;

import com.xunxian.seekingimmortals.menu.SectHallMenu;
import com.xunxian.seekingimmortals.network.ModNetwork;
import com.xunxian.seekingimmortals.network.SectActionPacket;
import com.xunxian.seekingimmortals.sect.SectContributionService;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

/**
 * Wave492: productized sect hall with legacy tab parity (dialogue/mission/shop/progress).
 */
public class SectHallScreen extends AbstractContainerScreen<SectHallMenu> {
    private Tab tab = Tab.MISSION;

    public SectHallScreen(SectHallMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 360;
        this.imageHeight = 236;
    }

    @Override
    protected void init() {
        super.init();
        rebuild();
    }

    private void rebuild() {
        clearWidgets();
        int left = leftPos;
        int top = topPos;
        int smallWidth = Math.max(54, Math.min(72, (imageWidth - 32) / 4));
        addRenderableWidget(Button.builder(Component.translatable("screen.seeking_immortals.sect.refresh"), button ->
                        ModNetwork.CHANNEL.sendToServer(new SectActionPacket(
                                SectContributionService.ACTION_OPEN, menu.focusSectId(), "")))
                .bounds(left + imageWidth - smallWidth * 2 - 18, top + 8, smallWidth, 18)
                .build());
        addRenderableWidget(Button.builder(Component.translatable("screen.seeking_immortals.sect.close"), button -> onClose())
                .bounds(left + imageWidth - smallWidth - 12, top + 8, smallWidth, 18)
                .build());

        ClientSectData.Snapshot data = ClientSectData.get();
        if (!data.synced()) {
            return;
        }
        if (!data.member()) {
            addCandidateButtons(data);
            return;
        }
        addTabButtons();
        switch (tab) {
            case DIALOGUE -> addDialogueButtons(data);
            case MISSION -> addMissionButtons(data);
            case SHOP -> addShopButtons(data);
            case PROGRESS -> addProgressButtons();
        }
    }

    private void addTabButtons() {
        int y = topPos + 48;
        int gap = 4;
        int buttonWidth = Math.max(1, (imageWidth - 28 - gap * 3) / 4);
        addTab(Tab.DIALOGUE, leftPos + 14, y, buttonWidth);
        addTab(Tab.MISSION, leftPos + 14 + buttonWidth + gap, y, buttonWidth);
        addTab(Tab.SHOP, leftPos + 14 + (buttonWidth + gap) * 2, y, buttonWidth);
        addTab(Tab.PROGRESS, leftPos + 14 + (buttonWidth + gap) * 3, y, buttonWidth);
    }

    private void addTab(Tab target, int x, int y, int width) {
        addRenderableWidget(Button.builder(Component.translatable(target.key), button -> {
            tab = target;
            rebuild();
        }).bounds(x, y, width, 18).build());
    }

    private void addCandidateButtons(ClientSectData.Snapshot data) {
        int y = topPos + 80;
        int buttonWidth = Math.max(100, imageWidth - 40);
        List<ClientSectData.Candidate> candidates = data.candidates();
        for (int i = 0; i < Math.min(candidates.size(), 5); i++) {
            ClientSectData.Candidate candidate = candidates.get(i);
            addRenderableWidget(Button.builder(Component.literal(candidate.displayZh()), button ->
                            ModNetwork.CHANNEL.sendToServer(new SectActionPacket(
                                    SectContributionService.ACTION_APPLY, candidate.id(), "")))
                    .bounds(leftPos + 16, y + i * 22, buttonWidth, 18)
                    .build());
        }
    }

    private void addDialogueButtons(ClientSectData.Snapshot data) {
        int y = topPos + imageHeight - 28;
        List<ClientSectData.DialogueOption> options = data.dialogue().options();
        int visible = Math.min(3, options.size());
        int gap = 4;
        int buttonWidth = Math.max(1, (imageWidth - 28 - gap * Math.max(0, visible - 1)) / Math.max(1, visible));
        for (int i = 0; i < visible; i++) {
            ClientSectData.DialogueOption option = options.get(i);
            addRenderableWidget(Button.builder(Component.translatable(option.labelKey()), button ->
                            ModNetwork.CHANNEL.sendToServer(new SectActionPacket(
                                    SectContributionService.ACTION_DIALOGUE, option.id(), "")))
                    .bounds(leftPos + 14 + i * (buttonWidth + gap), y, buttonWidth, 18)
                    .build());
        }
    }

    private void addMissionButtons(ClientSectData.Snapshot data) {
        int y = topPos + imageHeight - 28;
        int gap = 4;
        int buttonWidth = Math.max(1, (imageWidth - 28 - gap * 2) / 3);
        addRenderableWidget(Button.builder(Component.translatable("screen.seeking_immortals.sect.mission.accept"), button ->
                        ModNetwork.CHANNEL.sendToServer(new SectActionPacket(
                                SectContributionService.ACTION_ACCEPT_MISSION, "", "")))
                .bounds(leftPos + 14, y, buttonWidth, 18)
                .build());
        addRenderableWidget(Button.builder(Component.translatable("screen.seeking_immortals.sect.mission.turn_in"), button ->
                        ModNetwork.CHANNEL.sendToServer(new SectActionPacket(
                                SectContributionService.ACTION_TURN_IN_MISSION, "", "")))
                .bounds(leftPos + 14 + buttonWidth + gap, y, buttonWidth, 18)
                .build());
        addRenderableWidget(Button.builder(Component.translatable("screen.seeking_immortals.sect.donate"), button ->
                        ModNetwork.CHANNEL.sendToServer(new SectActionPacket(
                                SectContributionService.ACTION_DONATE_SPIRIT_GRASS, "", "")))
                .bounds(leftPos + 14 + (buttonWidth + gap) * 2, y, buttonWidth, 18)
                .build());
    }

    private void addShopButtons(ClientSectData.Snapshot data) {
        List<ClientSectData.ShopEntry> entries = data.shopEntries();
        int rowY = topPos + 90;
        int buyWidth = Math.min(54, Math.max(1, imageWidth - 24));
        int buyX = Math.max(leftPos + 4, leftPos + imageWidth - buyWidth - 12);
        for (int i = 0; i < Math.min(entries.size(), 5); i++) {
            ClientSectData.ShopEntry entry = entries.get(i);
            addRenderableWidget(Button.builder(Component.translatable("screen.seeking_immortals.sect.buy"), button ->
                            ModNetwork.CHANNEL.sendToServer(new SectActionPacket(
                                    SectContributionService.ACTION_BUY, entry.id(), "")))
                    .bounds(buyX, rowY + i * 22 - 4, buyWidth, 18)
                    .build());
        }
    }

    private void addProgressButtons() {
        int y = topPos + imageHeight - 28;
        addRenderableWidget(Button.builder(Component.translatable("screen.seeking_immortals.sect.advance"), button ->
                        ModNetwork.CHANNEL.sendToServer(new SectActionPacket(
                                SectContributionService.ACTION_ADVANCE, "", "")))
                .bounds(leftPos + 14, y, imageWidth - 28, 18)
                .build());
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        ImmortalUiSkin.drawPanel(graphics, leftPos, topPos, imageWidth, imageHeight);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        ClientSectData.Snapshot data = ClientSectData.get();
        graphics.drawCenteredString(font, Component.translatable("screen.seeking_immortals.sect.title"),
                leftPos + imageWidth / 2, topPos + 12, ImmortalUiSkin.COLOR_TITLE);
        if (!data.synced()) {
            ImmortalUiSkin.drawStringFit(font, graphics,
                    Component.translatable("screen.seeking_immortals.sect.candidates_empty").getString(),
                    leftPos + 14, topPos + 40, imageWidth - 28, ImmortalUiSkin.COLOR_TEXT_MUTED, false);
            return;
        }
        ImmortalUiSkin.drawStringFit(font, graphics,
                Component.translatable("screen.seeking_immortals.sect.current",
                        data.currentSectDisplay(), data.role()).getString(),
                leftPos + 14, topPos + 30, imageWidth - 28, ImmortalUiSkin.COLOR_TEXT_NORMAL, false);
        ImmortalUiSkin.drawStringFit(font, graphics,
                Component.translatable("screen.seeking_immortals.sect.contribution", data.contribution()).getString(),
                leftPos + 14, topPos + 42, imageWidth - 28, ImmortalUiSkin.COLOR_TEXT_MUTED, false);

        if (!data.member()) {
            ImmortalUiSkin.drawStringFit(font, graphics,
                    Component.translatable("screen.seeking_immortals.sect.candidates_empty").getString(),
                    leftPos + 14, topPos + 64, imageWidth - 28, ImmortalUiSkin.COLOR_TEXT_MUTED, false);
            renderTooltip(graphics, mouseX, mouseY);
            return;
        }

        switch (tab) {
            case DIALOGUE -> {
                ImmortalUiSkin.drawStringFit(font, graphics,
                        Component.translatable(data.dialogue().titleKey()).getString(),
                        leftPos + 14, topPos + 74, imageWidth - 28, ImmortalUiSkin.COLOR_TEXT_NORMAL, false);
                ImmortalUiSkin.drawStringFit(font, graphics,
                        Component.translatable(data.dialogue().textKey()).getString(),
                        leftPos + 14, topPos + 90, imageWidth - 28, ImmortalUiSkin.COLOR_TEXT_MUTED, false);
            }
            case MISSION -> {
                if (data.mission() != null && data.mission().available()) {
                    ImmortalUiSkin.drawStringFit(font, graphics,
                            Component.translatable("screen.seeking_immortals.sect.mission").getString()
                                    + ": " + data.mission().id()
                                    + " / " + Component.translatable(data.mission().titleKey()).getString()
                                    + " / +" + data.mission().rewardContribution(),
                            leftPos + 14, topPos + 80, imageWidth - 28, ImmortalUiSkin.COLOR_TEXT_NORMAL, false);
                } else {
                    ImmortalUiSkin.drawStringFit(font, graphics,
                            Component.translatable("screen.seeking_immortals.sect.mission_empty").getString(),
                            leftPos + 14, topPos + 80, imageWidth - 28, ImmortalUiSkin.COLOR_TEXT_MUTED, false);
                }
            }
            case SHOP -> {
                List<ClientSectData.ShopEntry> entries = data.shopEntries();
                for (int i = 0; i < Math.min(entries.size(), 5); i++) {
                    ClientSectData.ShopEntry entry = entries.get(i);
                    ImmortalUiSkin.drawStringFit(font, graphics,
                            entry.itemDescriptionId() + " x" + entry.count() + "  " + entry.cost(),
                            leftPos + 16, topPos + 90 + i * 22, imageWidth - 90, ImmortalUiSkin.COLOR_TEXT_NORMAL, false);
                }
                if (entries.isEmpty()) {
                    ImmortalUiSkin.drawStringFit(font, graphics,
                            Component.translatable("screen.seeking_immortals.shop.empty").getString(),
                            leftPos + 14, topPos + 90, imageWidth - 28, ImmortalUiSkin.COLOR_TEXT_MUTED, false);
                }
            }
            case PROGRESS -> ImmortalUiSkin.drawStringFit(font, graphics,
                    Component.translatable("screen.seeking_immortals.sect.stage",
                            Component.translatable(data.stageKey()).getString()).getString()
                            + " — " + Component.translatable(data.objectiveKey()).getString(),
                    leftPos + 14, topPos + 80, imageWidth - 28, ImmortalUiSkin.COLOR_TEXT_NORMAL, false);
        }
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
    }

    private enum Tab {
        DIALOGUE("screen.seeking_immortals.sect.tab.dialogue"),
        MISSION("screen.seeking_immortals.sect.tab.mission"),
        SHOP("screen.seeking_immortals.sect.tab.shop"),
        PROGRESS("screen.seeking_immortals.sect.tab.progress");
        private final String key;
        Tab(String key) { this.key = key; }
    }
}
