package com.xunxian.seekingimmortals.client;

import com.xunxian.seekingimmortals.network.ModNetwork;
import com.xunxian.seekingimmortals.network.SetTechniqueSlotPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

public class TechniqueEditScreen extends Screen {
    private static final int PANEL_WIDTH = 420;
    private static final int PANEL_HEIGHT = 260;
    private static final int SLOT_COUNT = 7;
    private static final int LINE_HEIGHT = 14;
    private static final int SLOT_X_OFFSET = 18;
    private static final int SLOT_START_Y_OFFSET = 58;
    private static final int SLOT_ROW_HEIGHT = 22;
    private static final int LEARNED_X_OFFSET = 190;
    private static final int LIST_START_Y_OFFSET = 58;

    private String draggingTechniqueId = "";

    public TechniqueEditScreen() {
        super(Component.translatable("screen.seeking_immortals.technique_edit.title"));
    }

    @Override
    protected void init() {
        super.init();
        int left = panelLeft();
        int top = panelTop();
        addRenderableWidget(Button.builder(Component.translatable("screen.seeking_immortals.cultivation_stats.close"), button -> onClose())
                .bounds(left + panelWidth() - 78, top + panelHeight() - 26, 66, 20)
                .build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        renderPanel(graphics, mouseX, mouseY);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderDraggedTechnique(graphics, mouseX, mouseY);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (ClientTechniqueData.isSynced()) {
            int slot = hoveredSlot(mouseX, mouseY);
            if (slot >= 0) {
                if (button == 1) {
                    ModNetwork.CHANNEL.sendToServer(new SetTechniqueSlotPacket(slot, ""));
                    return true;
                }
                if (button == 0) {
                    List<String> techniques = ClientTechniqueData.getLearnedTechniques();
                    String techniqueId = slot < techniques.size() ? techniques.get(slot) : "";
                    if (!techniqueId.isBlank()) {
                        ModNetwork.CHANNEL.sendToServer(new SetTechniqueSlotPacket(slot, techniqueId));
                        return true;
                    }
                }
            }

            if (button == 0) {
                int learnedIndex = hoveredLearnedIndex(mouseX, mouseY);
                List<String> techniques = ClientTechniqueData.getLearnedTechniques();
                if (learnedIndex >= 0 && learnedIndex < techniques.size()) {
                    draggingTechniqueId = techniques.get(learnedIndex);
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return !draggingTechniqueId.isBlank() || super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && !draggingTechniqueId.isBlank()) {
            int slot = hoveredSlot(mouseX, mouseY);
            if (slot >= 0) {
                ModNetwork.CHANNEL.sendToServer(new SetTechniqueSlotPacket(slot, draggingTechniqueId));
            }
            draggingTechniqueId = "";
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private void renderPanel(GuiGraphics graphics, int mouseX, int mouseY) {
        int left = panelLeft();
        int top = panelTop();
        int width = panelWidth();
        int height = panelHeight();
        ImmortalUiSkin.drawPanel(graphics, left, top, width, height);
        graphics.drawCenteredString(font, title, left + width / 2, top + 12, 0xFFE6D59A);
        graphics.drawString(font, "拖拽右侧已学技能到左侧槽位绑定；右键槽位清空。左键槽位仍绑定同序号技能。", left + 14, top + 32, 0xFFB8F5A2, false);

        List<String> techniques = ClientTechniqueData.isSynced() ? ClientTechniqueData.getLearnedTechniques() : List.of();
        List<String> slots = ClientTechniqueData.isSynced() ? ClientTechniqueData.getTechniqueSlots() : List.of();
        renderSlots(graphics, left, top, slots, mouseX, mouseY);
        renderLearnedList(graphics, left, top, height, techniques, mouseX, mouseY);
    }

    private void renderSlots(GuiGraphics graphics, int left, int top, List<String> slots, int mouseX, int mouseY) {
        int x = left + SLOT_X_OFFSET;
        int y = top + SLOT_START_Y_OFFSET;
        graphics.drawString(font, "技能槽位", x, y - 16, 0xFFE6D59A, false);
        for (int i = 0; i < SLOT_COUNT; i++) {
            String techniqueId = i < slots.size() ? slots.get(i) : "";
            ClientTechniqueData.TechniqueSummary summary = techniqueId.isBlank() ? null : ClientTechniqueData.getTechniqueSummary(techniqueId);
            int rowY = y + i * SLOT_ROW_HEIGHT;
            boolean hovered = hoveredSlot(mouseX, mouseY) == i;
            if (hovered) {
                graphics.fill(x - 3, rowY - 5, left + LEARNED_X_OFFSET - 10, rowY - 5 + SLOT_ROW_HEIGHT, 0x332F8F45);
            }
            ImmortalUiSkin.drawSkillSlot(graphics, x, rowY - 3, 18, summary != null);
            graphics.drawString(font, Integer.toString(i + 1), x + 6, rowY + 2, 0xFFFFFFFF, true);
            int cooldownSeconds = techniqueId.isBlank() ? 0 : (int)Math.ceil(ClientTechniqueData.getCooldownRemainingTicks(techniqueId) / 20.0D);
            String cooldownText = cooldownSeconds > 0 ? " · 冷却 " + cooldownSeconds + " 秒" : "";
            String text = summary == null ? "空槽" : summary.name() + " · 消耗 " + summary.cost() + cooldownText;
            graphics.drawString(font, "槽 " + (i + 1) + "：" + text, x + 26, rowY + 1, summary == null ? 0xFFBFAF8A : 0xFFEFE4C2, false);
        }
    }

    private void renderLearnedList(GuiGraphics graphics, int left, int top, int height, List<String> techniques, int mouseX, int mouseY) {
        int x = left + LEARNED_X_OFFSET;
        int y = top + LIST_START_Y_OFFSET;
        graphics.drawString(font, "已学技能（按住左键拖拽）", x, y - 16, 0xFFE6D59A, false);
        if (!ClientTechniqueData.isSynced()) {
            graphics.drawString(font, "等待服务端同步技能数据...", x, y, 0xFFBFAF8A, false);
            return;
        }
        if (techniques.isEmpty()) {
            graphics.drawString(font, "暂无已学技能。", x, y, 0xFFBFAF8A, false);
            return;
        }

        int maxRows = maxLearnedRows(top, height);
        int hoveredIndex = hoveredLearnedIndex(mouseX, mouseY);
        for (int i = 0; i < Math.min(maxRows, techniques.size()); i++) {
            ClientTechniqueData.TechniqueSummary summary = ClientTechniqueData.getTechniqueSummary(techniques.get(i));
            int rowY = y + i * LINE_HEIGHT;
            if (hoveredIndex == i) {
                graphics.fill(x - 3, rowY - 2, left + panelWidth() - 14, rowY + LINE_HEIGHT - 1, 0x332F8F45);
            }
            graphics.drawString(font, (i + 1) + ". " + summary.name() + "（" + summary.attribute() + "）", x, rowY, 0xFFEFE4C2, false);
        }
        if (techniques.size() > maxRows) {
            graphics.drawString(font, "+" + (techniques.size() - maxRows) + " 个未显示", x, y + maxRows * LINE_HEIGHT, 0xFFE6D59A, false);
        }
    }

    private void renderDraggedTechnique(GuiGraphics graphics, int mouseX, int mouseY) {
        if (draggingTechniqueId.isBlank()) return;
        ClientTechniqueData.TechniqueSummary summary = ClientTechniqueData.getTechniqueSummary(draggingTechniqueId);
        String text = "拖拽绑定：《" + summary.name() + "》→ 释放到左侧槽位";
        int boxWidth = font.width(text) + 12;
        int x = Math.min(mouseX + 10, width - boxWidth - 4);
        int y = Math.min(mouseY + 10, height - 20);
        ImmortalUiSkin.drawTooltipPanel(graphics, x, y, boxWidth, 18);
        graphics.drawString(font, text, x + 6, y + 5, 0xFFB8F5A2, false);
    }

    private int hoveredSlot(double mouseX, double mouseY) {
        int left = panelLeft();
        int top = panelTop();
        int x = left + SLOT_X_OFFSET;
        int y = top + SLOT_START_Y_OFFSET;
        for (int i = 0; i < SLOT_COUNT; i++) {
            int rowY = y + i * SLOT_ROW_HEIGHT;
            if (mouseX >= x - 3 && mouseX < left + LEARNED_X_OFFSET - 10 && mouseY >= rowY - 5 && mouseY < rowY - 5 + SLOT_ROW_HEIGHT) {
                return i;
            }
        }
        return -1;
    }

    private int hoveredLearnedIndex(double mouseX, double mouseY) {
        int left = panelLeft();
        int top = panelTop();
        int x = left + LEARNED_X_OFFSET;
        int y = top + LIST_START_Y_OFFSET;
        int maxRows = maxLearnedRows(top, panelHeight());
        if (mouseX >= x - 3 && mouseX < left + panelWidth() - 14 && mouseY >= y && mouseY < y + maxRows * LINE_HEIGHT) {
            return (int)((mouseY - y) / LINE_HEIGHT);
        }
        return -1;
    }

    private int maxLearnedRows(int top, int height) {
        return Math.max(1, (top + height - 44 - (top + LIST_START_Y_OFFSET)) / LINE_HEIGHT);
    }

    private int panelLeft() {
        return (width - panelWidth()) / 2;
    }

    private int panelTop() {
        return (height - panelHeight()) / 2;
    }

    private int panelWidth() {
        return Math.max(300, Math.min(PANEL_WIDTH, width - 24));
    }

    private int panelHeight() {
        return Math.max(220, Math.min(PANEL_HEIGHT, height - 24));
    }
}
