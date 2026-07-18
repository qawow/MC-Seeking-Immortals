package com.xunxian.seekingimmortals.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/** Shared native button used by cultivation screens. Climate-aware via active ImmortalUiSkin stack. */
public final class ImmortalButton extends Button {
    public enum Style {
        SECONDARY,
        PRIMARY,
        DANGER
    }

    private final Style style;

    public ImmortalButton(int x, int y, int width, int height, Component message,
                          OnPress onPress, Style style) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
        this.style = style == null ? Style.SECONDARY : style;
    }

    public static ImmortalButton secondary(int x, int y, int width, int height,
                                            Component message, OnPress onPress) {
        return new ImmortalButton(x, y, width, height, message, onPress, Style.SECONDARY);
    }

    public static ImmortalButton primary(int x, int y, int width, int height,
                                          Component message, OnPress onPress) {
        return new ImmortalButton(x, y, width, height, message, onPress, Style.PRIMARY);
    }

    public static ImmortalButton danger(int x, int y, int width, int height,
                                         Component message, OnPress onPress) {
        return new ImmortalButton(x, y, width, height, message, onPress, Style.DANGER);
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        boolean danger = style == Style.DANGER;
        boolean primary = style == Style.PRIMARY || danger;
        if (danger) {
            ImmortalUiSkin.pushClimate(UiClimate.CINNABAR_SEAL);
        }
        try {
            ImmortalUiSkin.drawButtonBackground(graphics, getX(), getY(), width, height,
                    isHoveredOrFocused(), active, primary);

            Minecraft minecraft = Minecraft.getInstance();
            String label = ImmortalUiSkin.fitWidth(minecraft.font, getMessage().getString(),
                    Math.max(0, width - 10));
            int color;
            if (!active) {
                color = ImmortalUiSkin.JOURNAL_PAPER_MUTED;
            } else if (danger) {
                color = ImmortalUiSkin.JOURNAL_CINNABAR_BRIGHT;
            } else if (isHoveredOrFocused()) {
                color = ImmortalUiSkin.JOURNAL_JADE_TEXT;
            } else {
                color = ImmortalUiSkin.JOURNAL_PAPER;
            }
            graphics.drawCenteredString(minecraft.font, label,
                    getX() + width / 2, getY() + (height - 8) / 2, color);
        } finally {
            if (danger) {
                ImmortalUiSkin.popClimate();
            }
        }
    }
}
