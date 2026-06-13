package com.xunxian.seekingimmortals.client;

import com.xunxian.seekingimmortals.SeekingImmortalsMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber(modid = SeekingImmortalsMod.MODID, value = Dist.CLIENT)
public final class TechniqueSkillBarOverlay {
    private static final int FRAME_WIDTH = 22;
    private static final int FRAME_HEIGHT = 172;
    private static final int FRAME_SLOT_TOP_PADDING = 22;
    private static final int SLOT_SIZE = 18;
    private static final int SLOT_GAP = 2;
    private static final int LEFT_MARGIN = 1;
    private static final int TOP_OFFSET = 44;
    public static final int SKILL_SLOT_COUNT = 7;

    private TechniqueSkillBarOverlay() {}

    public static void renderOverlay(ForgeGui gui, GuiGraphics graphics, float partialTick, int screenWidth, int screenHeight) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options.hideGui || minecraft.player == null || minecraft.screen != null) return;

        List<String> techniqueSlots = ClientTechniqueData.isSynced() ? ClientTechniqueData.getTechniqueSlots() : List.of();
        int top = Math.max(4, Math.min(TOP_OFFSET, screenHeight - totalBarHeight() - 4));
        renderNativeSkillBar(graphics, LEFT_MARGIN, top, techniqueSlots);
    }

    /**
     * Native Forge overlay renderer for the left technique bar.
     *
     * <p>This is invoked from Forge's HUD overlay event. The
     * old imported full-height decorative frame is intentionally no longer
     * drawn because it created an extra pattern near the player's upper-left
     * HUD area; the 7 interactive slots remain in the same vertical layout.</p>
     */
    public static void renderNativeSkillBar(GuiGraphics graphics, int x, int y, List<String> techniqueSlots) {
        Minecraft minecraft = Minecraft.getInstance();
        int mouseX = (int) (minecraft.mouseHandler.xpos() * minecraft.getWindow().getGuiScaledWidth() / minecraft.getWindow().getScreenWidth());
        int mouseY = (int) (minecraft.mouseHandler.ypos() * minecraft.getWindow().getGuiScaledHeight() / minecraft.getWindow().getScreenHeight());
        String hoveredTechnique = null;
        int hoveredY = y;

        int slotX = slotX(x);
        for (int i = 0; i < SKILL_SLOT_COUNT; i++) {
            int slotY = slotY(y, i);
            String techniqueId = i < techniqueSlots.size() ? techniqueSlots.get(i) : null;
            drawSlot(graphics, slotX, slotY, i, techniqueId);
            if (techniqueId != null && isInside(mouseX, mouseY, slotX, slotY, SLOT_SIZE, SLOT_SIZE)) {
                hoveredTechnique = techniqueId;
                hoveredY = slotY;
            }
        }

        if (hoveredTechnique != null) {
            drawTechniqueTooltip(graphics, x + FRAME_WIDTH + 4, hoveredY, hoveredTechnique);
        }
    }

    private static int slotX(int frameX) {
        return frameX + (FRAME_WIDTH - SLOT_SIZE) / 2;
    }

    private static int slotY(int frameY, int index) {
        return frameY + FRAME_SLOT_TOP_PADDING + index * (SLOT_SIZE + SLOT_GAP);
    }

    private static void drawSlot(GuiGraphics graphics, int x, int y, int index, String techniqueId) {
        Minecraft minecraft = Minecraft.getInstance();
        boolean hasTechnique = techniqueId != null && !techniqueId.isBlank();

        ImmortalUiSkin.drawSkillSlot(graphics, x, y, SLOT_SIZE, hasTechnique);
        if (hasTechnique) {
            drawTechniqueIconPlaceholder(graphics, x, y, techniqueId);
        }

        String label = Integer.toString(index + 1);
        graphics.drawString(minecraft.font, label, x + 2, y + 2, hasTechnique ? 0xFFFFFFFF : 0x99FFFFFF, true);
    }

    private static void drawTechniqueTooltip(GuiGraphics graphics, int x, int y, String techniqueId) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientCultivationData.Snapshot data = ClientCultivationData.getSnapshot();
        ClientTechniqueData.TechniqueSummary summary = ClientTechniqueData.getTechniqueSummary(techniqueId);
        boolean canRelease = ClientTechniqueData.canRelease(techniqueId, data);
        int cooldownSeconds = (int)Math.ceil(ClientTechniqueData.getCooldownRemainingTicks(techniqueId) / 20.0D);
        List<String> lines = List.of(
                "技能名：" + summary.name(),
                "所属功法：" + summary.source(),
                "属性：" + summary.attribute(),
                "消耗：" + summary.cost() + " 灵力",
                "冷却：" + (cooldownSeconds > 0 ? cooldownSeconds + " 秒" : "就绪"),
                "是否可释放：" + (canRelease ? "可释放" : "不可释放"));
        int width = 0;
        for (String line : lines) {
            width = Math.max(width, minecraft.font.width(line));
        }
        int height = lines.size() * 11 + 8;
        ImmortalUiSkin.drawTooltipPanel(graphics, x, y, width + 12, height);
        int textY = y + 5;
        for (int i = 0; i < lines.size(); i++) {
            int color = i == lines.size() - 1 ? (canRelease ? 0xFFB8F5A2 : 0xFFFF8A8A) : 0xFFEFE4C2;
            graphics.drawString(minecraft.font, lines.get(i), x + 6, textY, color, false);
            textY += 11;
        }
    }

    private static int totalBarHeight() {
        return FRAME_HEIGHT;
    }

    private static boolean isInside(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private static void drawTechniqueIconPlaceholder(GuiGraphics graphics, int x, int y, String techniqueId) {
        Minecraft minecraft = Minecraft.getInstance();
        int colorSeed = Math.abs(techniqueId.hashCode());
        int fillColor = 0xAA000000 | (colorSeed & 0x003F3F3F) | 0x00202020;
        ImmortalUiSkin.drawSkillIconBacking(graphics, x + 3, y + 3, SLOT_SIZE - 6, SLOT_SIZE - 6, fillColor);
        ClientTechniqueData.TechniqueSummary summary = ClientTechniqueData.getTechniqueSummary(techniqueId);
        graphics.drawString(minecraft.font, getInitial(summary.name()), x + SLOT_SIZE - 8, y + SLOT_SIZE - 10, 0xFFE6D59A, true);
    }

    private static String getInitial(String value) {
        if (value == null || value.isBlank()) return "?";
        return value.substring(0, 1).toUpperCase();
    }
}
