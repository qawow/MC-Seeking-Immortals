package com.xunxian.seekingimmortals.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.Locale;

/**
 * Dedicated meditation breakdown screen (Wave49 Phase2 depth).
 * Uses already-synced ClientCultivationData; no new packets.
 */
public class MeditationScreen extends Screen {
    private static final int WIDTH = 280;
    private static final int HEIGHT = 200;

    public MeditationScreen() {
        super(Component.translatable("screen.seeking_immortals.meditation.title"));
    }

    @Override
    protected void init() {
        super.init();
        int left = (this.width - WIDTH) / 2;
        int top = (this.height - HEIGHT) / 2;
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(left + WIDTH - 70, top + HEIGHT - 28, 58, 20)
                .build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        int left = (this.width - WIDTH) / 2;
        int top = (this.height - HEIGHT) / 2;
        ImmortalUiSkin.drawPanel(graphics, left, top, WIDTH, HEIGHT);
        graphics.drawCenteredString(font, title, left + WIDTH / 2, top + 10, ImmortalUiSkin.COLOR_TITLE);

        ClientCultivationData.Snapshot data = ClientCultivationData.getSnapshot();
        boolean meditating = ClientCultivationData.effectiveMeditating();
        int y = top + 30;
        draw(graphics, left + 14, y, Component.translatable("screen.seeking_immortals.meditation.state",
                meditating ? Component.translatable("screen.seeking_immortals.meditation.active")
                        : Component.translatable("screen.seeking_immortals.meditation.idle")));
        y += 14;
        draw(graphics, left + 14, y, Component.literal("效率 x" + fmt(data.cultivationSpeedMultiplier())
                + " | 结算 " + fmt(data.meditationTotalPerSecond()) + "/s"));
        y += 12;
        draw(graphics, left + 14, y, Component.literal("灵根 x" + fmt(data.rootCultivationSpeedCoefficient())
                + " | 打坐灵根 " + fmt(data.meditationRootMultiplier())));
        y += 12;
        draw(graphics, left + 14, y, Component.literal("体质/功法 x" + fmt(data.physiqueCultivationSpeedMultiplier())
                + " | 吐纳功法 " + fmt(data.meditationTechniqueMultiplier())));
        y += 12;
        draw(graphics, left + 14, y, Component.literal("灵气 " + data.auraConcentration()
                + " (" + data.auraNature() + ") x" + fmt(data.meditationAuraMultiplier())));
        y += 12;
        draw(graphics, left + 14, y, Component.literal("修为 " + data.cultivation() + " / " + data.cultivationMax()));
        y += 12;
        draw(graphics, left + 14, y, Component.literal("灵力 " + data.mana() + " / " + data.manaMax()));
        y += 12;
        draw(graphics, left + 14, y, Component.literal("金丹 " + data.goldCoreGrade() + " (" + data.goldCoreScore() + ")"));
        y += 16;
        double progress = meditating && minecraft != null && minecraft.player != null
                ? (minecraft.player.tickCount % 100) / 100.0D : 0.0D;
        ImmortalUiSkin.drawCultivationProgressBar(graphics, left + 14, y, WIDTH - 28, 10, progress);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void draw(GuiGraphics graphics, int x, int y, Component text) {
        graphics.drawString(font, text, x, y, ImmortalUiSkin.COLOR_TEXT_NORMAL, false);
    }

    private static String fmt(double v) {
        return String.format(Locale.ROOT, "%.2f", v);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
