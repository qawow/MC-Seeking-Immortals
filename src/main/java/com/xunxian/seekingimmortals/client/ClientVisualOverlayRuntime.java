package com.xunxian.seekingimmortals.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.gui.overlay.ForgeGui;

/** Short-lived, budgeted full-screen tint for authored SCREEN_OVERLAY actions. */
@OnlyIn(Dist.CLIENT)
public final class ClientVisualOverlayRuntime {
    private static int remainingTicks;
    private static int totalTicks;
    private static int colorArgb;
    private static int intensity;

    private ClientVisualOverlayRuntime() {}

    public static void push(int authoredArgb, int authoredIntensity, int durationTicks) {
        int duration = Math.max(2, Math.min(200, durationTicks));
        if (remainingTicks > 0 && authoredIntensity < intensity) {
            return;
        }
        colorArgb = authoredArgb | 0xFF000000;
        intensity = Math.max(1, Math.min(96, authoredIntensity));
        remainingTicks = duration;
        totalTicks = duration;
    }

    public static void tick() {
        if (remainingTicks > 0 && !Minecraft.getInstance().isPaused()) {
            remainingTicks--;
        }
    }

    public static void reset() {
        remainingTicks = 0;
        totalTicks = 0;
        intensity = 0;
        colorArgb = 0;
    }

    public static void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int width, int height) {
        Minecraft minecraft = Minecraft.getInstance();
        if (remainingTicks <= 0 || minecraft.player == null || minecraft.options.hideGui) {
            return;
        }
        float life = totalTicks <= 0 ? 0.0F : remainingTicks / (float) totalTicks;
        float fade = Math.min(1.0F, life * 4.0F) * Math.min(1.0F, (1.0F - life) * 6.0F + 0.25F);
        int alpha = Math.max(8, Math.min(88, Math.round((18.0F + intensity * 0.55F) * fade)));
        int rgb = colorArgb & 0x00FFFFFF;
        graphics.fill(0, 0, width, height, (alpha << 24) | rgb);
        int edge = (Math.min(140, alpha + 32) << 24) | rgb;
        int thickness = Math.max(2, Math.min(6, 2 + intensity / 24));
        graphics.fill(0, 0, width, thickness, edge);
        graphics.fill(0, height - thickness, width, height, edge);
        graphics.fill(0, 0, thickness, height, edge);
        graphics.fill(width - thickness, 0, width, height, edge);
    }

    static int remainingTicks() {
        return remainingTicks;
    }
}
