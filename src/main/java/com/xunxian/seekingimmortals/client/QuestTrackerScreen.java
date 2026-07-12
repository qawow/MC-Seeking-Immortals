package com.xunxian.seekingimmortals.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Lightweight quest tracker screen (Wave49 Phase9 depth).
 * Displays client-side cached tracker lines filled by SyncQuestTrackerPacket.
 */
public class QuestTrackerScreen extends Screen {
    private static final int WIDTH = 320;
    private static final int HEIGHT = 210;

    public QuestTrackerScreen() {
        super(Component.translatable("screen.seeking_immortals.quest_tracker.title"));
    }

    @Override
    protected void init() {
        super.init();
        int left = (this.width - WIDTH) / 2;
        int top = (this.height - HEIGHT) / 2;
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(left + WIDTH - 70, top + HEIGHT - 28, 58, 20)
                .build());
        addRenderableWidget(Button.builder(Component.translatable("screen.seeking_immortals.quest_tracker.refresh"), b ->
                        com.xunxian.seekingimmortals.network.ModNetwork.CHANNEL.sendToServer(
                                new com.xunxian.seekingimmortals.network.QuestTrackerActionPacket("sync")))
                .bounds(left + 12, top + HEIGHT - 28, 70, 20)
                .build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        int left = (this.width - WIDTH) / 2;
        int top = (this.height - HEIGHT) / 2;
        ImmortalUiSkin.drawPanel(graphics, left, top, WIDTH, HEIGHT);
        graphics.drawCenteredString(font, title, left + WIDTH / 2, top + 10, ImmortalUiSkin.COLOR_TITLE);
        List<String> lines = ClientQuestTrackerData.lines();
        int y = top + 30;
        if (lines.isEmpty()) {
            graphics.drawString(font, Component.translatable("screen.seeking_immortals.quest_tracker.empty"),
                    left + 14, y, ImmortalUiSkin.COLOR_TEXT_MUTED, false);
        } else {
            int shown = 0;
            for (String line : lines) {
                ImmortalUiSkin.drawStringFit(font, graphics, line, left + 14, y, WIDTH - 28,
                        ImmortalUiSkin.COLOR_TEXT_NORMAL, false);
                y += 12;
                if (++shown >= 12) {
                    break;
                }
            }
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
