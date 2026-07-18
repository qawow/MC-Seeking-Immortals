package com.xunxian.seekingimmortals.client;

import com.xunxian.seekingimmortals.SeekingImmortalsMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * Left vertical semi-transparent jade-slip rail for the seven technique release slots.
 * Anchored left and vertically centered under the left-top status strip.
 */
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

    private TechniqueSkillBarOverlay() {}

    public static void renderOverlay(ForgeGui gui, GuiGraphics graphics, float partialTick,
                                     int screenWidth, int screenHeight) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options.hideGui || minecraft.player == null || minecraft.screen != null) return;

        List<String> techniqueSlots = ClientTechniqueData.isSynced()
                ? ClientTechniqueData.getTechniqueSlots() : List.of();
        ImmortalHudLayout.Layout layout = ImmortalHudLayout.calculate(screenWidth, screenHeight);
        renderSkillBar(graphics, layout.techniques(), techniqueSlots,
                layout.techniqueSlotSize(), layout.techniqueSlotGap(), layout.techniquePadding());
    }

    /** Keeps the existing public renderer for previews that use the full-size seven-slot bar. */
    public static void renderNativeSkillBar(GuiGraphics graphics, int x, int y, List<String> techniqueSlots) {
        renderSkillBar(graphics, new ImmortalHudLayout.Rect(x, y, BAR_WIDTH, totalBarHeight()),
                techniqueSlots, SLOT_SIZE, SLOT_GAP, BAR_PADDING_TOP);
    }

    private static void renderSkillBar(GuiGraphics graphics, ImmortalHudLayout.Rect frame,
                                       List<String> techniqueSlots, int slotSize, int slotGap, int padding) {
        Minecraft minecraft = Minecraft.getInstance();
        int windowWidth = Math.max(1, minecraft.getWindow().getScreenWidth());
        int windowHeight = Math.max(1, minecraft.getWindow().getScreenHeight());
        int mouseX = (int)(minecraft.mouseHandler.xpos()
                * minecraft.getWindow().getGuiScaledWidth() / windowWidth);
        int mouseY = (int)(minecraft.mouseHandler.ypos()
                * minecraft.getWindow().getGuiScaledHeight() / windowHeight);
        String hoveredTechnique = null;
        int hoveredY = frame.y();

        ImmortalUiSkin.drawTranslucentJadeSlipRail(graphics, frame.x(), frame.y(), frame.width(), frame.height());
        int safeSlotSize = Math.max(1, slotSize);
        int slotX = frame.x() + Math.max(0, (frame.width() - safeSlotSize) / 2);
        List<String> slots = techniqueSlots == null ? List.of() : techniqueSlots;
        for (int i = 0; i < SKILL_SLOT_COUNT; i++) {
            int slotY = frame.y() + Math.max(0, padding) + i * (safeSlotSize + Math.max(0, slotGap));
            if (slotY + safeSlotSize > frame.bottom()) {
                break;
            }
            String techniqueId = i < slots.size() ? slots.get(i) : null;
            drawSlot(graphics, slotX, slotY, safeSlotSize, i, techniqueId);
            if (techniqueId != null && !techniqueId.isBlank()
                    && isInside(mouseX, mouseY, slotX, slotY, safeSlotSize, safeSlotSize)) {
                hoveredTechnique = techniqueId;
                hoveredY = slotY;
            }
        }

        if (hoveredTechnique != null) {
            drawTechniqueTooltip(graphics, frame, hoveredY, hoveredTechnique,
                    minecraft.getWindow().getGuiScaledWidth(), minecraft.getWindow().getGuiScaledHeight());
        }
    }

    private static void drawSlot(GuiGraphics graphics, int x, int y, int size, int index, String techniqueId) {
        Minecraft minecraft = Minecraft.getInstance();
        boolean hasTechnique = techniqueId != null && !techniqueId.isBlank();

        ImmortalUiSkin.drawTranslucentJadeSlipSlot(graphics, x, y, size, hasTechnique);
        if (hasTechnique) {
            drawTechniqueIcon(graphics, x, y, size, techniqueId);
            ClientCultivationData.Snapshot data = ClientCultivationData.getSnapshot();
            ClientTechniqueData.TechniqueSummary summary = ClientTechniqueData.getTechniqueSummary(techniqueId);
            boolean canRelease = ClientTechniqueData.canRelease(techniqueId, data);
            int cooldownTicks = ClientTechniqueData.getCooldownRemainingTicks(techniqueId);
            if (!canRelease && size > 2) {
                graphics.fill(x + 1, y + 1, x + size - 1, y + size - 1, ImmortalUiSkin.HUD_SKILL_DISABLED_OVERLAY);
            }
            if (cooldownTicks > 0) {
                drawCooldownOverlay(graphics, x, y, size,
                        ClientTechniqueData.getCooldownFraction(techniqueId));
                String seconds = Integer.toString((int)Math.ceil(cooldownTicks / 20.0D));
                if (size >= minecraft.font.lineHeight + 2
                        && minecraft.font.width(seconds) <= size - 2) {
                    graphics.drawString(minecraft.font, seconds,
                            x + (size - minecraft.font.width(seconds)) / 2,
                            y + Math.max(1, (size - minecraft.font.lineHeight) / 2),
                            ImmortalUiSkin.COLOR_TEXT_NORMAL, true);
                }
            } else if (data.spiritualPower() < summary.cost()) {
                drawLowManaMark(graphics, x, y, size);
            }
        }

        String label = Integer.toString(index + 1);
        if (size >= minecraft.font.lineHeight && minecraft.font.width(label) <= size - 2) {
            graphics.drawString(minecraft.font, label, x + 1, y + 1,
                    hasTechnique ? ImmortalUiSkin.JOURNAL_PAPER : ImmortalUiSkin.JOURNAL_PAPER_MUTED, true);
        }
    }

    private static void drawTechniqueTooltip(GuiGraphics graphics, ImmortalHudLayout.Rect frame,
                                             int slotY, String techniqueId,
                                             int screenWidth, int screenHeight) {
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

        int widestLine = 1;
        for (String line : lines) {
            widestLine = Math.max(widestLine, minecraft.font.width(line));
        }
        int margin = screenWidth < 120 || screenHeight < 70 ? 2 : 4;
        int panelWidth = Math.max(1, Math.min(widestLine + 12, Math.max(1, screenWidth - margin * 2)));
        int desiredHeight = lines.size() * (minecraft.font.lineHeight + 2) + 8;
        int panelHeight = Math.max(1, Math.min(desiredHeight, Math.max(1, screenHeight - margin * 2)));
        int x = calculateTooltipX(frame.x(), frame.width(), panelWidth, screenWidth, margin);
        int y = clampInt(slotY, margin, Math.max(margin, screenHeight - panelHeight - margin));
        ImmortalUiSkin.drawStatusStripChrome(graphics, x, y, panelWidth, panelHeight, false);

        int textX = x + Math.min(6, Math.max(2, panelWidth / 12));
        int textWidth = Math.max(1, x + panelWidth - textX - 3);
        int textY = y + 4;
        int textBottom = y + panelHeight - 3;
        for (int i = 0; i < lines.size(); i++) {
            if (textY + minecraft.font.lineHeight > textBottom) {
                break;
            }
            int color = i == lines.size() - 1
                    ? (canRelease ? ImmortalUiSkin.JOURNAL_JADE_TEXT : ImmortalUiSkin.JOURNAL_CINNABAR_BRIGHT)
                    : i == 3 ? ImmortalUiSkin.JOURNAL_SPIRIT : ImmortalUiSkin.JOURNAL_PAPER;
            ImmortalUiSkin.drawStringFit(minecraft.font, graphics, lines.get(i),
                    textX, textY, textWidth, color, false);
            textY += minecraft.font.lineHeight + 2;
        }
    }

    static int calculateBarX(int screenWidth) {
        ImmortalHudLayout.Layout layout = ImmortalHudLayout.calculate(screenWidth, 480);
        return layout.techniques().x();
    }

    static int calculateBarY(int screenHeight) {
        ImmortalHudLayout.Layout layout = ImmortalHudLayout.calculate(854, screenHeight);
        return layout.techniques().y();
    }

    static int calculateTooltipX(int barX, int panelWidth, int screenWidth) {
        return calculateTooltipX(barX, totalBarWidth(), panelWidth, screenWidth, 4);
    }

    private static int calculateTooltipX(int barX, int barWidth, int panelWidth,
                                         int screenWidth, int margin) {
        int preferred = barX + barWidth + 8;
        int maxX = Math.max(margin, screenWidth - panelWidth - margin);
        return clampInt(preferred, margin, maxX);
    }

    static int totalBarWidth() {
        return BAR_WIDTH;
    }

    static int leftReservedWidth() {
        return totalBarWidth() + LEFT_MARGIN + HUD_GAP;
    }

    static int totalBarHeight() {
        return BAR_PADDING_TOP + SKILL_SLOT_COUNT * SLOT_SIZE
                + (SKILL_SLOT_COUNT - 1) * SLOT_GAP + BAR_PADDING_BOTTOM;
    }

    private static boolean isInside(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private static void drawCooldownOverlay(GuiGraphics graphics, int x, int y, int size, double fraction) {
        if (size <= 0) return;
        int inset = size > 2 ? 1 : 0;
        int innerHeight = Math.max(1, size - inset * 2);
        int overlayHeight = Math.max(1, (int)Math.round(innerHeight
                * Math.max(0.0D, Math.min(1.0D, fraction))));
        graphics.fill(x + inset, y + inset, x + size - inset,
                Math.min(y + size - inset, y + inset + overlayHeight),
                ImmortalUiSkin.HUD_COOLDOWN_OVERLAY);
        int edgeY = Math.min(y + size - 1, y + inset + overlayHeight - 1);
        graphics.fill(x + inset, edgeY, x + size - inset, edgeY + 1,
                ImmortalUiSkin.JOURNAL_CINNABAR_BRIGHT);
    }

    private static void drawLowManaMark(GuiGraphics graphics, int x, int y, int size) {
        if (size <= 1) return;
        int mark = Math.max(1, Math.min(4, size / 3));
        int left = x + Math.max(0, size - mark - 1);
        int top = y + Math.max(0, size - mark - 1);
        graphics.fill(left, top, x + size - 1, y + size - 1, ImmortalUiSkin.JOURNAL_CINNABAR);
        if (mark >= 3) {
            graphics.fill(left + 1, top + 1, x + size - 2, y + size - 2,
                    ImmortalUiSkin.JOURNAL_SPIRIT);
        }
    }

    private static void drawTechniqueIcon(GuiGraphics graphics, int x, int y, int size, String techniqueId) {
        Minecraft minecraft = Minecraft.getInstance();
        int inset = size >= 4 ? 1 : 0;
        int iconSize = Math.max(1, size - inset * 2);
        if (ImmortalUiSkin.hasSkillIcon(techniqueId)) {
            ImmortalUiSkin.drawSkillIcon(graphics, x + inset, y + inset, iconSize, techniqueId);
            return;
        }
        if (size >= 4) {
            int fillColor = ImmortalUiSkin.skillPlaceholderColor(techniqueId);
            int backingInset = Math.max(1, size / 5);
            ImmortalUiSkin.drawSkillIconBacking(graphics, x + backingInset, y + backingInset,
                    Math.max(1, size - backingInset * 2), Math.max(1, size - backingInset * 2), fillColor);
        }
        ClientTechniqueData.TechniqueSummary summary = ClientTechniqueData.getTechniqueSummary(techniqueId);
        String initial = getInitial(summary.name());
        if (size >= minecraft.font.lineHeight + 2 && minecraft.font.width(initial) <= size - 2) {
            graphics.drawString(minecraft.font, initial,
                    x + Math.max(1, size - minecraft.font.width(initial) - 1),
                    y + Math.max(1, size - minecraft.font.lineHeight - 1),
                    ImmortalUiSkin.JOURNAL_PAPER, true);
        }
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
