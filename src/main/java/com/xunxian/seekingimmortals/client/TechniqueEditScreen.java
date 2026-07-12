package com.xunxian.seekingimmortals.client;

import com.xunxian.seekingimmortals.network.ModNetwork;
import com.xunxian.seekingimmortals.network.SetTechniqueSlotPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

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
    private static final int SCROLLBAR_WIDTH = 3;

    private String draggingTechniqueId = "";
    private int learnedScrollOffset = 0;

    public TechniqueEditScreen() {
        super(Component.translatable("screen.seeking_immortals.technique_edit.title"));
    }

    @Override
    protected void init() {
        super.init();
        int left = panelLeft();
        int top = panelTop();
        int width = panelWidth();
        int height = panelHeight();
        int buttonWidth = Math.min(66, Math.max(1, width - 24));
        int buttonY = Math.max(top + 4, top + height - 26);
        addRenderableWidget(Button.builder(Component.translatable("screen.seeking_immortals.cultivation_stats.close"), button -> onClose())
                .bounds(left + width - 12 - buttonWidth, buttonY, buttonWidth, 20)
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

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (ClientTechniqueData.isSynced() && isInsideLearnedList(mouseX, mouseY)) {
            List<String> techniques = ClientTechniqueData.getLearnedTechniques();
            int maxRows = maxLearnedRows(panelTop(), panelHeight());
            int maxScroll = maxLearnedScroll(techniques.size(), maxRows);
            if (maxScroll > 0) {
                int direction = delta > 0.0D ? -1 : 1;
                learnedScrollOffset = Mth.clamp(learnedScrollOffset + direction, 0, maxScroll);
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    private void renderPanel(GuiGraphics graphics, int mouseX, int mouseY) {
        int left = panelLeft();
        int top = panelTop();
        int width = panelWidth();
        int height = panelHeight();
        ImmortalUiSkin.drawPanel(graphics, left, top, width, height);
        graphics.drawCenteredString(font, title, left + width / 2, top + 12, 0xFFE6D59A);
        ImmortalUiSkin.drawStringFit(font, graphics,
                Component.translatable("screen.seeking_immortals.technique_edit.instruction").getString(),
                left + 14, top + 32, Math.max(1, width - 28), 0xFFB8F5A2, false);

        List<String> techniques = ClientTechniqueData.isSynced() ? ClientTechniqueData.getLearnedTechniques() : List.of();
        List<String> slots = ClientTechniqueData.isSynced() ? ClientTechniqueData.getTechniqueSlots() : List.of();
        renderSlots(graphics, left, top, slots, mouseX, mouseY);
        renderLearnedList(graphics, left, top, height, techniques, mouseX, mouseY);
    }

    private void renderSlots(GuiGraphics graphics, int left, int top, List<String> slots, int mouseX, int mouseY) {
        int x = left + SLOT_X_OFFSET;
        int y = top + SLOT_START_Y_OFFSET;
        int learnedOffset = learnedXOffset();
        ClientCultivationData.Snapshot data = ClientCultivationData.getSnapshot();
        graphics.drawString(font, Component.translatable("screen.seeking_immortals.technique_edit.slots"), x, y - 16, 0xFFE6D59A, false);
        for (int i = 0; i < SLOT_COUNT; i++) {
            String techniqueId = i < slots.size() ? slots.get(i) : "";
            ClientTechniqueData.TechniqueSummary summary = techniqueId.isBlank() ? null : ClientTechniqueData.getTechniqueSummary(techniqueId);
            boolean canRelease = summary != null && ClientTechniqueData.canRelease(techniqueId, data);
            int rowY = y + i * SLOT_ROW_HEIGHT;
            boolean hovered = hoveredSlot(mouseX, mouseY) == i;
            if (hovered) {
                graphics.fill(x - 3, rowY - 5, left + learnedOffset - 10, rowY - 5 + SLOT_ROW_HEIGHT, 0x332F8F45);
            }
            ImmortalUiSkin.drawSkillSlot(graphics, x, rowY - 3, 18, summary != null);
            if (summary != null && ImmortalUiSkin.hasSkillIcon(techniqueId)) {
                ImmortalUiSkin.drawSkillIcon(graphics, x + 1, rowY - 2, 16, techniqueId);
                if (!canRelease) {
                    graphics.fill(x + 1, rowY - 2, x + 17, rowY + 14, 0x88000000);
                }
            }
            graphics.drawString(font, Integer.toString(i + 1), x + 6, rowY + 2, 0xFFFFFFFF, true);
            int cooldownSeconds = techniqueId.isBlank() ? 0 : (int)Math.ceil(ClientTechniqueData.getCooldownRemainingTicks(techniqueId) / 20.0D);
            String cooldownText = cooldownSeconds > 0
                    ? " / " + Component.translatable("screen.seeking_immortals.technique_edit.cooldown", cooldownSeconds).getString()
                    : "";
            String text = summary == null
                    ? Component.translatable("screen.seeking_immortals.technique_edit.empty_slot").getString()
                    : Component.translatable("screen.seeking_immortals.technique_edit.slot_summary", summary.name(), summary.cost()).getString() + cooldownText;
            int color = summary == null ? 0xFFBFAF8A : (canRelease ? 0xFFEFE4C2 : 0xFFFFB0A0);
            ImmortalUiSkin.drawStringFit(font, graphics,
                    Component.translatable("screen.seeking_immortals.technique_edit.slot_label", i + 1, text).getString(),
                    x + 26, rowY + 1, Math.max(1, learnedOffset - SLOT_X_OFFSET - 38), color, false);
        }
    }

    private void renderLearnedList(GuiGraphics graphics, int left, int top, int height, List<String> techniques, int mouseX, int mouseY) {
        int x = left + learnedXOffset();
        int y = top + LIST_START_Y_OFFSET;
        graphics.drawString(font, Component.translatable("screen.seeking_immortals.technique_edit.learned"), x, y - 16, 0xFFE6D59A, false);
        if (!ClientTechniqueData.isSynced()) {
            graphics.drawString(font, Component.translatable("screen.seeking_immortals.technique_edit.waiting_sync"), x, y, 0xFFBFAF8A, false);
            return;
        }
        if (techniques.isEmpty()) {
            graphics.drawString(font, Component.translatable("screen.seeking_immortals.technique_edit.empty_learned"), x, y, 0xFFBFAF8A, false);
            return;
        }

        int maxRows = maxLearnedRows(top, height);
        renderScrollableLearnedRows(graphics, left, x, y, maxRows, techniques, mouseX, mouseY);
    }

    private void renderScrollableLearnedRows(GuiGraphics graphics, int left, int x, int y, int maxRows, List<String> techniques, int mouseX, int mouseY) {
        learnedScrollOffset = Mth.clamp(learnedScrollOffset, 0, maxLearnedScroll(techniques.size(), maxRows));
        int hoveredIndex = hoveredLearnedIndex(mouseX, mouseY);
        int visibleRows = Math.min(maxRows, techniques.size() - learnedScrollOffset);
        int rowRight = learnedListRight(left);
        for (int i = 0; i < visibleRows; i++) {
            int techniqueIndex = learnedScrollOffset + i;
            ClientTechniqueData.TechniqueSummary summary = ClientTechniqueData.getTechniqueSummary(techniques.get(techniqueIndex));
            int rowY = y + i * LINE_HEIGHT;
            if (hoveredIndex == techniqueIndex) {
                graphics.fill(x - 3, rowY - 2, rowRight, rowY + LINE_HEIGHT - 1, 0x332F8F45);
            }
            graphics.drawString(font, (techniqueIndex + 1) + ". " + summary.name() + " / " + summary.attribute(), x, rowY, 0xFFEFE4C2, false);
        }
        renderLearnedScrollbar(graphics, left, y, maxRows, techniques.size());
    }

    private void renderLearnedScrollbar(GuiGraphics graphics, int left, int y, int maxRows, int totalRows) {
        int maxScroll = maxLearnedScroll(totalRows, maxRows);
        if (maxScroll <= 0) return;

        int trackHeight = maxRows * LINE_HEIGHT;
        int trackX = left + panelWidth() - 18;
        graphics.fill(trackX, y, trackX + SCROLLBAR_WIDTH, y + trackHeight, 0x55302216);

        int thumbHeight = Math.max(10, trackHeight * maxRows / totalRows);
        int thumbTravel = Math.max(1, trackHeight - thumbHeight);
        int thumbY = y + thumbTravel * learnedScrollOffset / maxScroll;
        graphics.fill(trackX, thumbY, trackX + SCROLLBAR_WIDTH, thumbY + thumbHeight, 0xCCB8F5A2);
    }

    private void renderDraggedTechnique(GuiGraphics graphics, int mouseX, int mouseY) {
        if (draggingTechniqueId.isBlank()) return;
        ClientTechniqueData.TechniqueSummary summary = ClientTechniqueData.getTechniqueSummary(draggingTechniqueId);
        String text = Component.translatable("screen.seeking_immortals.technique_edit.dragging", summary.name()).getString();
        int boxWidth = Math.max(1, Math.min(Math.max(1, width - 4), font.width(text) + 12));
        int x = Math.max(0, Math.min(mouseX + 10, width - boxWidth - 4));
        int y = Math.max(0, Math.min(mouseY + 10, height - 20));
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
            if (mouseX >= x - 3 && mouseX < left + learnedXOffset() - 10 && mouseY >= rowY - 5 && mouseY < rowY - 5 + SLOT_ROW_HEIGHT) {
                return i;
            }
        }
        return -1;
    }

    private int hoveredLearnedIndex(double mouseX, double mouseY) {
        if (!ClientTechniqueData.isSynced()) return -1;
        List<String> techniques = ClientTechniqueData.getLearnedTechniques();
        if (techniques.isEmpty()) return -1;

        int left = panelLeft();
        int top = panelTop();
        int x = left + learnedXOffset();
        int y = top + LIST_START_Y_OFFSET;
        int maxRows = maxLearnedRows(top, panelHeight());
        if (mouseX >= x - 3 && mouseX < learnedListRight(left) && mouseY >= y && mouseY < y + maxRows * LINE_HEIGHT) {
            int visibleIndex = (int)((mouseY - y) / LINE_HEIGHT);
            int learnedIndex = learnedScrollOffset + visibleIndex;
            return learnedIndex < techniques.size() ? learnedIndex : -1;
        }
        return -1;
    }

    private int maxLearnedRows(int top, int height) {
        return Math.max(1, (top + height - 44 - (top + LIST_START_Y_OFFSET)) / LINE_HEIGHT);
    }

    private boolean isInsideLearnedList(double mouseX, double mouseY) {
        int left = panelLeft();
        int top = panelTop();
        int x = left + learnedXOffset();
        int y = top + LIST_START_Y_OFFSET;
        int maxRows = maxLearnedRows(top, panelHeight());
        return mouseX >= x - 3 && mouseX < learnedListRight(left) && mouseY >= y && mouseY < y + maxRows * LINE_HEIGHT;
    }

    private int maxLearnedScroll(int totalRows, int visibleRows) {
        return Math.max(0, totalRows - visibleRows);
    }

    private int learnedListRight(int left) {
        return left + panelWidth() - 24;
    }

    private int panelLeft() {
        return Math.max(0, (width - panelWidth()) / 2);
    }

    private int panelTop() {
        return Math.max(0, (height - panelHeight()) / 2);
    }

    private int panelWidth() {
        return calculatePanelWidth(width);
    }

    private int panelHeight() {
        return calculatePanelHeight(height);
    }

    private int learnedXOffset() {
        return Math.min(LEARNED_X_OFFSET, Math.max(56, panelWidth() / 2));
    }

    static int calculatePanelWidth(int screenWidth) {
        if (screenWidth <= 0) return 1;
        int margin = screenWidth >= 260 ? 24 : Math.min(8, Math.max(0, screenWidth / 10));
        return Math.max(1, Math.min(PANEL_WIDTH, screenWidth - margin));
    }

    static int calculatePanelHeight(int screenHeight) {
        if (screenHeight <= 0) return 1;
        int margin = screenHeight >= 180 ? 24 : Math.min(8, Math.max(0, screenHeight / 10));
        return Math.max(1, Math.min(PANEL_HEIGHT, screenHeight - margin));
    }
}
