package com.xunxian.seekingimmortals.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** Refinement forge plan browser screen (Wave50 Phase13 depth). */
public class RefinementPlanScreen extends Screen {
    private static final int W = 340;
    private static final int H = 210;
    private final List<String> lines = new ArrayList<>();

    public RefinementPlanScreen(List<String> lines) {
        super(Component.translatable("screen.seeking_immortals.refine_plan.title"));
        if (lines != null) this.lines.addAll(lines);
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
        int y = top + 30;
        if (lines.isEmpty()) {
            graphics.drawString(font, Component.translatable("screen.seeking_immortals.refine_plan.empty"),
                    left + 14, y, ImmortalUiSkin.COLOR_TEXT_MUTED, false);
        } else {
            int shown = 0;
            for (String line : lines) {
                ImmortalUiSkin.drawStringFit(font, graphics, line, left + 14, y, W - 28,
                        ImmortalUiSkin.COLOR_TEXT_NORMAL, false);
                y += 12;
                if (++shown >= 12) break;
            }
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
