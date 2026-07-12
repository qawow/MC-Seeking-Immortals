package com.xunxian.seekingimmortals.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Alchemy status / skill screen (Wave50 Phase5 depth).
 * Full slot MenuType still deferred; this exposes skill level and craft workflow guidance.
 */
public class AlchemyStatusScreen extends Screen {
    private static final int W = 300;
    private static final int H = 170;
    private final int skillLevel;
    private final int skillExp;
    private final String lastMessage;

    public AlchemyStatusScreen(int skillLevel, int skillExp, String lastMessage) {
        super(Component.translatable("screen.seeking_immortals.alchemy.title"));
        this.skillLevel = skillLevel;
        this.skillExp = skillExp;
        this.lastMessage = lastMessage == null ? "" : lastMessage;
    }

    @Override
    protected void init() {
        super.init();
        int left = (width - W) / 2;
        int top = (height - H) / 2;
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(left + W - 70, top + H - 28, 58, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        int left = (width - W) / 2;
        int top = (height - H) / 2;
        ImmortalUiSkin.drawPanel(graphics, left, top, W, H);
        graphics.drawCenteredString(font, title, left + W / 2, top + 10, ImmortalUiSkin.COLOR_TITLE);
        int y = top + 34;
        graphics.drawString(font, Component.translatable("screen.seeking_immortals.alchemy.level", skillLevel, 10),
                left + 14, y, ImmortalUiSkin.COLOR_TEXT_NORMAL, false);
        y += 14;
        graphics.drawString(font, Component.translatable("screen.seeking_immortals.alchemy.exp", skillExp),
                left + 14, y, ImmortalUiSkin.COLOR_TEXT_NORMAL, false);
        y += 14;
        graphics.drawString(font, Component.translatable("screen.seeking_immortals.alchemy.bonus",
                String.format("%.0f%%", Math.min(0.20D, skillLevel * 0.02D) * 100.0D)),
                left + 14, y, ImmortalUiSkin.COLOR_TEXT_NORMAL, false);
        y += 16;
        ImmortalUiSkin.drawStringFit(font, graphics,
                Component.translatable("screen.seeking_immortals.alchemy.howto").getString(),
                left + 14, y, W - 28, ImmortalUiSkin.COLOR_TEXT_MUTED, false);
        y += 24;
        if (!lastMessage.isBlank()) {
            ImmortalUiSkin.drawStringFit(font, graphics, lastMessage, left + 14, y, W - 28,
                    ImmortalUiSkin.COLOR_TEXT_NORMAL, false);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
