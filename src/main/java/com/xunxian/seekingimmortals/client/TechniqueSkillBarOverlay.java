package com.xunxian.seekingimmortals.client;

import com.xunxian.seekingimmortals.SeekingImmortalsMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber(modid = SeekingImmortalsMod.MODID, value = Dist.CLIENT)
public final class TechniqueSkillBarOverlay {
    private static final int BAR_WIDTH = 30;
    private static final int BAR_PADDING_TOP = 9;
    private static final int BAR_PADDING_BOTTOM = 9;
    private static final int SLOT_SIZE = 18;
    private static final int SLOT_GAP = 3;
    private static final int LEFT_MARGIN = 6;
    private static final int HUD_GAP = 4;
    private static final int VERTICAL_MARGIN = 8;
    public static final int SKILL_SLOT_COUNT = 7;

    private static final int BAR_SHADOW = 0x66000000;
    private static final int BAR_BORDER = 0xCCB99A55;
    private static final int BAR_EDGE = 0x885B4524;
    private static final int BAR_BACKING = 0xAA100E09;
    private static final int BAR_INNER = 0xAA161D14;
    private static final int SLOT_SHADOW = 0x55000000;
    private static final int SLOT_BORDER = 0xDDB99A55;
    private static final int SLOT_EMPTY_BORDER = 0x88B99A55;
    private static final int SLOT_BACKING = 0xCC10140F;
    private static final int SLOT_EMPTY_BACKING = 0x6610140F;
    private static final int JADE_LINE = 0xAA73C79C;
    private static final int PAPER_TAG = 0xAA6E4D27;
    private static final int PAPER_TAG_LIT = 0xDDB88948;

    private TechniqueSkillBarOverlay() {}

    public static void renderOverlay(ForgeGui gui, GuiGraphics graphics, float partialTick, int screenWidth, int screenHeight) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options.hideGui || minecraft.player == null || minecraft.screen != null) return;

        List<String> techniqueSlots = ClientTechniqueData.isSynced() ? ClientTechniqueData.getTechniqueSlots() : List.of();
        int x = calculateBarX(screenWidth);
        int top = calculateBarY(screenHeight);
        renderNativeSkillBar(graphics, x, top, techniqueSlots);
    }

    /**
     * Native Forge overlay renderer for the left technique bar.
     *
     * <p>The bar keeps the existing 7-slot behavior while presenting the
     * slots as a compact jade-slip/talisman stack at the left screen edge.</p>
     */
    public static void renderNativeSkillBar(GuiGraphics graphics, int x, int y, List<String> techniqueSlots) {
        Minecraft minecraft = Minecraft.getInstance();
        int mouseX = (int) (minecraft.mouseHandler.xpos() * minecraft.getWindow().getGuiScaledWidth() / minecraft.getWindow().getScreenWidth());
        int mouseY = (int) (minecraft.mouseHandler.ypos() * minecraft.getWindow().getGuiScaledHeight() / minecraft.getWindow().getScreenHeight());
        String hoveredTechnique = null;
        int hoveredY = y;

        drawSkillBarFrame(graphics, x, y);
        int slotX = slotX(x);
        for (int i = 0; i < SKILL_SLOT_COUNT; i++) {
            int slotY = slotY(y, i);
            String techniqueId = i < techniqueSlots.size() ? techniqueSlots.get(i) : null;
            drawSlot(graphics, slotX, slotY, i, techniqueId);
            if (techniqueId != null && !techniqueId.isBlank() && isInside(mouseX, mouseY, slotX, slotY, SLOT_SIZE, SLOT_SIZE)) {
                hoveredTechnique = techniqueId;
                hoveredY = slotY;
            }
        }

        if (hoveredTechnique != null) {
            drawTechniqueTooltip(graphics, x, hoveredY, hoveredTechnique,
                    minecraft.getWindow().getGuiScaledWidth(), minecraft.getWindow().getGuiScaledHeight());
        }
    }

    private static int slotX(int frameX) {
        return frameX + (BAR_WIDTH - SLOT_SIZE) / 2;
    }

    private static int slotY(int frameY, int index) {
        return frameY + BAR_PADDING_TOP + index * (SLOT_SIZE + SLOT_GAP);
    }

    private static void drawSlot(GuiGraphics graphics, int x, int y, int index, String techniqueId) {
        Minecraft minecraft = Minecraft.getInstance();
        boolean hasTechnique = techniqueId != null && !techniqueId.isBlank();

        drawTechniqueSlotFrame(graphics, x, y, hasTechnique);
        if (hasTechnique) {
            drawTechniqueIconPlaceholder(graphics, x, y, techniqueId);
            ClientCultivationData.Snapshot data = ClientCultivationData.getSnapshot();
            ClientTechniqueData.TechniqueSummary summary = ClientTechniqueData.getTechniqueSummary(techniqueId);
            boolean canRelease = ClientTechniqueData.canRelease(techniqueId, data);
            int cooldownTicks = ClientTechniqueData.getCooldownRemainingTicks(techniqueId);
            if (!canRelease) {
                graphics.fill(x + 1, y + 1, x + SLOT_SIZE - 1, y + SLOT_SIZE - 1, 0x66120D0A);
            }
            if (cooldownTicks > 0) {
                drawCooldownOverlay(graphics, x, y, ClientTechniqueData.getCooldownFraction(techniqueId));
                String seconds = Integer.toString((int)Math.ceil(cooldownTicks / 20.0D));
                graphics.drawString(minecraft.font, seconds, x + (SLOT_SIZE - minecraft.font.width(seconds)) / 2, y + 6, 0xFFFFFFFF, true);
            } else if (data.spiritualPower() < summary.cost()) {
                drawLowManaMark(graphics, x, y);
            }
        }

        String label = Integer.toString(index + 1);
        graphics.drawString(minecraft.font, label, x + 2, y + 2, hasTechnique ? 0xFFFFFFFF : 0x99FFFFFF, true);
    }

    private static void drawTechniqueTooltip(GuiGraphics graphics, int barX, int slotY, String techniqueId, int screenWidth, int screenHeight) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientCultivationData.Snapshot data = ClientCultivationData.getSnapshot();
        ClientTechniqueData.TechniqueSummary summary = ClientTechniqueData.getTechniqueSummary(techniqueId);
        boolean canRelease = ClientTechniqueData.canRelease(techniqueId, data);
        int cooldownSeconds = (int)Math.ceil(ClientTechniqueData.getCooldownRemainingTicks(techniqueId) / 20.0D);
        List<String> lines = List.of(
                Component.translatable("screen.seeking_immortals.technique.tooltip.name", summary.name()).getString(),
                Component.translatable("screen.seeking_immortals.technique.tooltip.source", summary.source()).getString(),
                Component.translatable("screen.seeking_immortals.technique.tooltip.attribute", summary.attribute()).getString(),
                Component.translatable("screen.seeking_immortals.technique.tooltip.cost", summary.cost()).getString(),
                Component.translatable("screen.seeking_immortals.technique.tooltip.cooldown",
                        cooldownSeconds > 0
                                ? Component.translatable("screen.seeking_immortals.technique.tooltip.seconds", cooldownSeconds).getString()
                                : Component.translatable("screen.seeking_immortals.technique.tooltip.ready").getString()).getString(),
                Component.translatable("screen.seeking_immortals.technique.tooltip.releasable",
                        Component.translatable(canRelease
                                ? "screen.seeking_immortals.technique.tooltip.yes"
                                : "screen.seeking_immortals.technique.tooltip.no").getString()).getString());
        int width = 0;
        for (String line : lines) {
            width = Math.max(width, minecraft.font.width(line));
        }
        int height = lines.size() * 11 + 8;
        int panelWidth = width + 12;
        int x = calculateTooltipX(barX, panelWidth, screenWidth);
        int y = clampInt(slotY, 4, Math.max(4, screenHeight - height - 4));
        ImmortalUiSkin.drawTooltipPanel(graphics, x, y, panelWidth, height);
        int textY = y + 5;
        for (int i = 0; i < lines.size(); i++) {
            int color = i == lines.size() - 1 ? (canRelease ? 0xFFB8F5A2 : 0xFFFF8A8A) : 0xFFEFE4C2;
            graphics.drawString(minecraft.font, lines.get(i), x + 6, textY, color, false);
            textY += 11;
        }
    }

    static int calculateBarX(int screenWidth) {
        return Math.max(0, Math.min(LEFT_MARGIN, screenWidth - totalBarWidth()));
    }

    static int calculateBarY(int screenHeight) {
        int totalHeight = totalBarHeight();
        if (screenHeight <= totalHeight + VERTICAL_MARGIN * 2) {
            return Math.max(0, (screenHeight - totalHeight) / 2);
        }
        int minY = VERTICAL_MARGIN;
        int maxY = screenHeight - totalHeight - VERTICAL_MARGIN;
        int y = clampInt((screenHeight - totalHeight) / 2, minY, maxY);
        int healthBottom = CultivationHealthOverlay.calculatePanelY(screenHeight)
                + CultivationHealthOverlay.panelHeight(screenHeight)
                + HUD_GAP;
        if (screenHeight >= healthBottom + totalHeight + VERTICAL_MARGIN) {
            y = clampInt(Math.max(y, healthBottom), healthBottom, maxY);
        }
        return y;
    }

    static int calculateTooltipX(int barX, int panelWidth, int screenWidth) {
        return clampInt(barX + totalBarWidth() + 8, 4, Math.max(4, screenWidth - panelWidth - 4));
    }

    static int totalBarWidth() {
        return BAR_WIDTH;
    }

    static int leftReservedWidth() {
        return totalBarWidth() + LEFT_MARGIN + HUD_GAP;
    }

    static int totalBarHeight() {
        return BAR_PADDING_TOP + SKILL_SLOT_COUNT * SLOT_SIZE + (SKILL_SLOT_COUNT - 1) * SLOT_GAP + BAR_PADDING_BOTTOM;
    }

    private static boolean isInside(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private static void drawSkillBarFrame(GuiGraphics graphics, int x, int y) {
        int height = totalBarHeight();
        graphics.fill(x + 2, y + 3, x + BAR_WIDTH + 2, y + height + 3, BAR_SHADOW);
        graphics.fill(x, y, x + BAR_WIDTH, y + height, BAR_BORDER);
        graphics.fill(x + 1, y + 1, x + BAR_WIDTH - 1, y + height - 1, BAR_BACKING);
        graphics.fill(x + 3, y + 3, x + BAR_WIDTH - 3, y + height - 3, BAR_INNER);
        graphics.fill(x + 2, y + 2, x + BAR_WIDTH - 2, y + 3, BAR_EDGE);
        graphics.fill(x + 2, y + height - 3, x + BAR_WIDTH - 2, y + height - 2, BAR_EDGE);
        graphics.fill(x + BAR_WIDTH - 5, y + 6, x + BAR_WIDTH - 3, y + height - 6, JADE_LINE);
        graphics.fill(x + 5, y + 5, x + BAR_WIDTH - 8, y + 6, 0x55E6D59A);
        graphics.fill(x + 5, y + height - 6, x + BAR_WIDTH - 8, y + height - 5, 0x444B2F13);
    }

    private static void drawTechniqueSlotFrame(GuiGraphics graphics, int x, int y, boolean filled) {
        graphics.fill(x + 1, y + 1, x + SLOT_SIZE + 1, y + SLOT_SIZE + 1, SLOT_SHADOW);
        graphics.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, filled ? SLOT_BORDER : SLOT_EMPTY_BORDER);
        graphics.fill(x + 1, y + 1, x + SLOT_SIZE - 1, y + SLOT_SIZE - 1, filled ? SLOT_BACKING : SLOT_EMPTY_BACKING);
        graphics.fill(x + 2, y + 2, x + SLOT_SIZE - 2, y + 3, filled ? 0x66E6D59A : 0x33856A3A);
        graphics.fill(x + SLOT_SIZE - 4, y + 4, x + SLOT_SIZE - 2, y + SLOT_SIZE - 4, filled ? PAPER_TAG_LIT : PAPER_TAG);
        graphics.fill(x + 3, y + SLOT_SIZE - 3, x + SLOT_SIZE - 4, y + SLOT_SIZE - 2, 0x66302216);
    }

    private static void drawCooldownOverlay(GuiGraphics graphics, int x, int y, double fraction) {
        int overlayHeight = Math.max(1, (int)Math.round((SLOT_SIZE - 2) * Math.max(0.0D, Math.min(1.0D, fraction))));
        graphics.fill(x + 1, y + 1, x + SLOT_SIZE - 1, y + 1 + overlayHeight, 0xCC211008);
        graphics.fill(x + 1, y + overlayHeight, x + SLOT_SIZE - 1, y + overlayHeight + 1, 0xAAE07B38);
    }

    private static void drawLowManaMark(GuiGraphics graphics, int x, int y) {
        graphics.fill(x + SLOT_SIZE - 6, y + SLOT_SIZE - 6, x + SLOT_SIZE - 2, y + SLOT_SIZE - 2, 0xDD7E2B24);
        graphics.fill(x + SLOT_SIZE - 5, y + SLOT_SIZE - 5, x + SLOT_SIZE - 3, y + SLOT_SIZE - 3, 0xFFFFC0A0);
    }

    private static void drawTechniqueIconPlaceholder(GuiGraphics graphics, int x, int y, String techniqueId) {
        Minecraft minecraft = Minecraft.getInstance();
        if (ImmortalUiSkin.hasSkillIcon(techniqueId)) {
            ImmortalUiSkin.drawSkillIcon(graphics, x + 1, y + 1, SLOT_SIZE - 2, techniqueId);
            return;
        }
        int colorSeed = Math.abs(techniqueId.hashCode());
        int fillColor = 0xAA000000 | (colorSeed & 0x003F3F3F) | 0x00202020;
        ImmortalUiSkin.drawSkillIconBacking(graphics, x + 3, y + 3, SLOT_SIZE - 6, SLOT_SIZE - 6, fillColor);
        ClientTechniqueData.TechniqueSummary summary = ClientTechniqueData.getTechniqueSummary(techniqueId);
        graphics.drawString(minecraft.font, getInitial(summary.name()), x + SLOT_SIZE - 8, y + SLOT_SIZE - 10, 0xFFE6D59A, true);
    }

    private static int clampInt(int value, int min, int max) {
        if (max < min) return min;
        return Math.max(min, Math.min(max, value));
    }

    private static String getInitial(String value) {
        if (value == null || value.isBlank()) return "?";
        return value.substring(0, 1).toUpperCase();
    }
}
