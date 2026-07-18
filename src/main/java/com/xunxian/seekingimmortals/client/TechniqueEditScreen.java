package com.xunxian.seekingimmortals.client;

import com.xunxian.seekingimmortals.network.ModNetwork;
import com.xunxian.seekingimmortals.network.SetTechniqueSlotPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.List;

public class TechniqueEditScreen extends AbstractJournalScreen {
    private static final int PANEL_WIDTH = 420;
    private static final int PANEL_HEIGHT = 260;
    private static final int PANEL_MARGIN = 4;
    private static final int WIDE_LAYOUT_WIDTH = 360;
    private static final int WIDE_LAYOUT_HEIGHT = 200;
    private static final int SLOT_COUNT = 7;
    private static final int LINE_HEIGHT = 14;
    private static final int WIDE_SLOT_ROW_HEIGHT = 22;

    private String draggingTechniqueId = "";
    private int learnedScrollOffset = 0;

    public TechniqueEditScreen() {
        super(Component.translatable("screen.seeking_immortals.technique_edit.title"));
    }

    @Override
    protected void init() {
        super.init();
        Layout layout = calculateLayout(width, height);
        addRenderableWidget(ImmortalButton.secondary(layout.closeButton().x(), layout.closeButton().y(),
                layout.closeButton().width(), layout.closeButton().height(),
                Component.translatable("screen.seeking_immortals.cultivation_stats.close"), button -> onClose()));
    }

    @Override
    protected JournalChrome journalChrome() {
        Layout layout = calculateLayout(width, height);
        return new JournalChrome(layout.left(), layout.top(), layout.panelWidth(), layout.panelHeight(),
                toUi(layout.header()), null);
    }

    @Override
    protected void renderJournalTitle(GuiGraphics graphics, JournalChrome chrome, UiRect header) {
        // Keep the original fixed y+4 centered title (not the base fit/vertical-center helper).
        graphics.drawCenteredString(font, getTitle(), header.x() + header.width() / 2,
                header.y() + 4, ImmortalUiSkin.JOURNAL_PAPER);
        if (header.height() >= 28) {
            ImmortalUiSkin.drawStringFit(font, graphics,
                    Component.translatable("screen.seeking_immortals.technique_edit.instruction").getString(),
                    header.x() + 8, header.y() + 18,
                    Math.max(1, header.width() - 16), ImmortalUiSkin.JOURNAL_JADE_TEXT, false);
        }
    }

    @Override
    protected void renderJournalContent(GuiGraphics graphics, JournalChrome chrome,
                                        int mouseX, int mouseY, float partialTick) {
        Layout layout = calculateLayout(width, height);
        ImmortalUiSkin.drawInnerFrame(graphics, layout.slotPane().x(), layout.slotPane().y(),
                layout.slotPane().width(), layout.slotPane().height());
        ImmortalUiSkin.drawInnerFrame(graphics, layout.learnedPane().x(), layout.learnedPane().y(),
                layout.learnedPane().width(), layout.learnedPane().height());

        List<String> techniques = ClientTechniqueData.isSynced() ? ClientTechniqueData.getLearnedTechniques() : List.of();
        List<String> slots = ClientTechniqueData.isSynced() ? ClientTechniqueData.getTechniqueSlots() : List.of();
        renderSlots(graphics, layout, slots, mouseX, mouseY);
        renderLearnedList(graphics, layout, techniques, mouseX, mouseY);
    }

    @Override
    protected void renderAfterWidgets(GuiGraphics graphics, JournalChrome chrome,
                                      int mouseX, int mouseY, float partialTick) {
        // Drag ghost must paint above widgets, matching pre-migration super.render → ghost order.
        renderDraggedTechnique(graphics, mouseX, mouseY);
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
            int maxRows = maxLearnedRows(calculateLayout(width, height));
            int maxScroll = maxLearnedScroll(techniques.size(), maxRows);
            if (maxScroll > 0) {
                int direction = delta > 0.0D ? -1 : 1;
                learnedScrollOffset = scrollLearnedBy(learnedScrollOffset, direction, techniques.size(), maxRows);
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    private void renderSlots(GuiGraphics graphics, Layout layout, List<String> slots, int mouseX, int mouseY) {
        Rect pane = layout.slotPane();
        ClientCultivationData.Snapshot data = ClientCultivationData.getSnapshot();
        ImmortalUiSkin.withScissor(graphics, pane.x() + 1, pane.y() + 1,
                Math.max(1, pane.width() - 2), Math.max(1, pane.height() - 2), () -> {
            if (pane.height() >= layout.slotSize() + 16) {
                ImmortalUiSkin.drawStringFit(font, graphics,
                        Component.translatable("screen.seeking_immortals.technique_edit.slots").getString(),
                        pane.x() + 5, pane.y() + 4, Math.max(1, pane.width() - 10),
                        ImmortalUiSkin.JOURNAL_PAPER, false);
            }
            int hoveredSlot = hoveredSlot(mouseX, mouseY);
            for (int i = 0; i < SLOT_COUNT; i++) {
                String techniqueId = i < slots.size() ? slots.get(i) : "";
                ClientTechniqueData.TechniqueSummary summary = techniqueId.isBlank()
                        ? null : ClientTechniqueData.getTechniqueSummary(techniqueId);
                boolean canRelease = summary != null && ClientTechniqueData.canRelease(techniqueId, data);
                Rect hit = slotHitRect(layout, i);
                Rect icon = slotIconRect(layout, i);
                ImmortalUiSkin.drawListRow(graphics, hit.x(), hit.y(), hit.width(), hit.height(),
                        hoveredSlot == i ? ImmortalUiSkin.InteractionState.HOVERED
                                : ImmortalUiSkin.InteractionState.NORMAL);
                ImmortalUiSkin.drawJadeSlipSlot(graphics, icon.x(), icon.y(), icon.width(), summary != null);
                int iconInset = icon.width() >= 10 ? 1 : 0;
                int iconSize = Math.max(1, icon.width() - iconInset * 2);
                if (summary != null && ImmortalUiSkin.hasSkillIcon(techniqueId)) {
                    ImmortalUiSkin.drawSkillIcon(graphics, icon.x() + iconInset, icon.y() + iconInset,
                            iconSize, techniqueId);
                    if (!canRelease) {
                        graphics.fill(icon.x() + iconInset, icon.y() + iconInset,
                                icon.x() + iconInset + iconSize, icon.y() + iconInset + iconSize, ImmortalUiSkin.JOURNAL_SHADOW);
                    }
                }
                if (icon.width() >= 10) {
                    graphics.drawString(font, Integer.toString(i + 1), icon.x() + 2, icon.y() + 2,
                            ImmortalUiSkin.JOURNAL_PAPER, true);
                }
                int cooldownSeconds = techniqueId.isBlank() ? 0
                        : (int)Math.ceil(ClientTechniqueData.getCooldownRemainingTicks(techniqueId) / 20.0D);
                if (!layout.wide()) {
                    if (cooldownSeconds > 0 && icon.width() >= 14) {
                        String seconds = Integer.toString(cooldownSeconds);
                        graphics.drawCenteredString(font, seconds, icon.x() + icon.width() / 2,
                                icon.y() + Math.max(2, icon.height() - 9), ImmortalUiSkin.JOURNAL_WARNING);
                    }
                    continue;
                }
                String cooldownText = cooldownSeconds > 0
                        ? " / " + Component.translatable("screen.seeking_immortals.technique_edit.cooldown",
                        cooldownSeconds).getString() : "";
                String text = summary == null
                        ? Component.translatable("screen.seeking_immortals.technique_edit.empty_slot").getString()
                        : Component.translatable("screen.seeking_immortals.technique_edit.slot_summary",
                        summary.name(), summary.cost()).getString() + cooldownText;
                int color = summary == null ? ImmortalUiSkin.JOURNAL_PAPER_MUTED
                        : canRelease ? ImmortalUiSkin.JOURNAL_PAPER : ImmortalUiSkin.JOURNAL_CINNABAR_BRIGHT;
                ImmortalUiSkin.drawStringFit(font, graphics,
                        Component.translatable("screen.seeking_immortals.technique_edit.slot_label", i + 1, text).getString(),
                        icon.right() + 6, hit.y() + Math.max(1, (hit.height() - 8) / 2),
                        Math.max(1, hit.right() - icon.right() - 9), color, false);
            }
        });
    }

    private void renderLearnedList(GuiGraphics graphics, Layout layout, List<String> techniques,
                                   int mouseX, int mouseY) {
        Rect pane = layout.learnedPane();
        if (pane.height() >= 28) {
            ImmortalUiSkin.drawStringFit(font, graphics,
                    Component.translatable("screen.seeking_immortals.technique_edit.learned").getString(),
                    pane.x() + 5, pane.y() + 4, Math.max(1, pane.width() - 10),
                    ImmortalUiSkin.JOURNAL_PAPER, false);
        }
        Rect viewport = learnedViewport(layout);
        if (!ClientTechniqueData.isSynced()) {
            ImmortalUiSkin.drawStringFit(font, graphics,
                    Component.translatable("screen.seeking_immortals.technique_edit.waiting_sync").getString(),
                    viewport.x(), viewport.y(), viewport.width(), ImmortalUiSkin.JOURNAL_PAPER_MUTED, false);
            return;
        }
        if (techniques.isEmpty()) {
            ImmortalUiSkin.drawStringFit(font, graphics,
                    Component.translatable("screen.seeking_immortals.technique_edit.empty_learned").getString(),
                    viewport.x(), viewport.y(), viewport.width(), ImmortalUiSkin.JOURNAL_PAPER_MUTED, false);
            return;
        }

        int maxRows = maxLearnedRows(layout);
        renderScrollableLearnedRows(graphics, layout, viewport, maxRows, techniques, mouseX, mouseY);
    }

    private void renderScrollableLearnedRows(GuiGraphics graphics, Layout layout, Rect viewport,
                                             int maxRows, List<String> techniques, int mouseX, int mouseY) {
        learnedScrollOffset = Mth.clamp(learnedScrollOffset, 0, maxLearnedScroll(techniques.size(), maxRows));
        int hoveredIndex = hoveredLearnedIndex(mouseX, mouseY);
        int visibleRows = Math.min(maxRows, techniques.size() - learnedScrollOffset);
        ImmortalUiSkin.withScissor(graphics, viewport.x(), viewport.y(), viewport.width(), viewport.height(), () -> {
            for (int i = 0; i < visibleRows; i++) {
                int techniqueIndex = learnedScrollOffset + i;
                ClientTechniqueData.TechniqueSummary summary = ClientTechniqueData.getTechniqueSummary(
                        techniques.get(techniqueIndex));
                int rowY = viewport.y() + i * LINE_HEIGHT;
                ImmortalUiSkin.drawListRow(graphics, viewport.x(), rowY, viewport.width(), LINE_HEIGHT,
                        hoveredIndex == techniqueIndex ? ImmortalUiSkin.InteractionState.HOVERED
                                : ImmortalUiSkin.InteractionState.NORMAL);
                ImmortalUiSkin.drawStringFit(font, graphics,
                        (techniqueIndex + 1) + ". " + summary.name() + " / " + summary.attribute(),
                        viewport.x() + 4, rowY + 2, Math.max(1, viewport.width() - 9),
                        ImmortalUiSkin.JOURNAL_PAPER, false);
            }
        });
        ImmortalUiSkin.drawThinScrollbar(graphics, layout.learnedPane().right() - 3,
                viewport.y(), viewport.height(), techniques.size() * LINE_HEIGHT,
                viewport.height(), learnedScrollOffset * LINE_HEIGHT);
    }

    private void renderDraggedTechnique(GuiGraphics graphics, int mouseX, int mouseY) {
        if (draggingTechniqueId.isBlank()) return;
        ClientTechniqueData.TechniqueSummary summary = ClientTechniqueData.getTechniqueSummary(draggingTechniqueId);
        String text = Component.translatable("screen.seeking_immortals.technique_edit.dragging", summary.name()).getString();
        int boxWidth = Math.max(1, Math.min(Math.max(1, width - 4), font.width(text) + 12));
        int x = Math.max(0, Math.min(mouseX + 10, width - boxWidth - 4));
        int y = Math.max(0, Math.min(mouseY + 10, height - 20));
        ImmortalUiSkin.drawTooltipPanel(graphics, x, y, boxWidth, 18);
        ImmortalUiSkin.drawStringFit(font, graphics, text, x + 6, y + 5,
                Math.max(1, boxWidth - 12), ImmortalUiSkin.JOURNAL_JADE_TEXT, false);
    }

    private int hoveredSlot(double mouseX, double mouseY) {
        Layout layout = calculateLayout(width, height);
        for (int i = 0; i < SLOT_COUNT; i++) {
            if (slotHitRect(layout, i).contains(mouseX, mouseY)) {
                return i;
            }
        }
        return -1;
    }

    private int hoveredLearnedIndex(double mouseX, double mouseY) {
        if (!ClientTechniqueData.isSynced()) return -1;
        List<String> techniques = ClientTechniqueData.getLearnedTechniques();
        if (techniques.isEmpty()) return -1;

        Layout layout = calculateLayout(width, height);
        Rect viewport = learnedViewport(layout);
        int maxRows = maxLearnedRows(layout);
        if (viewport.contains(mouseX, mouseY)) {
            int visibleIndex = (int)((mouseY - viewport.y()) / LINE_HEIGHT);
            int learnedIndex = learnedScrollOffset + visibleIndex;
            return visibleIndex < maxRows && learnedIndex < techniques.size() ? learnedIndex : -1;
        }
        return -1;
    }

    private int maxLearnedRows(Layout layout) {
        return Math.max(1, learnedViewport(layout).height() / LINE_HEIGHT);
    }

    private boolean isInsideLearnedList(double mouseX, double mouseY) {
        return learnedViewport(calculateLayout(width, height)).contains(mouseX, mouseY);
    }

    static int maxLearnedScroll(int totalRows, int visibleRows) {
        return Math.max(0, totalRows - Math.max(1, visibleRows));
    }

    /** Package-visible: learned-list wheel step used by drag-source pane. */
    static int scrollLearnedBy(int current, int direction, int totalRows, int visibleRows) {
        return Mth.clamp(current + direction, 0, maxLearnedScroll(totalRows, visibleRows));
    }

    /**
     * Package-visible drag contract: releasing over a valid slot binds the technique;
     * releasing outside clears the drag without sending a packet (slot &lt; 0).
     */
    static boolean shouldBindOnRelease(int hoveredSlot, String draggingTechniqueId) {
        return hoveredSlot >= 0 && draggingTechniqueId != null && !draggingTechniqueId.isBlank();
    }

    private static UiRect toUi(Rect rect) {
        return new UiRect(rect.x(), rect.y(), rect.width(), rect.height());
    }

    static int calculatePanelWidth(int screenWidth) {
        return Math.min(PANEL_WIDTH, Math.max(1, screenWidth - PANEL_MARGIN * 2));
    }

    static int calculatePanelHeight(int screenHeight) {
        return Math.min(PANEL_HEIGHT, Math.max(1, screenHeight - PANEL_MARGIN * 2));
    }

    static Layout calculateLayout(int screenWidth, int screenHeight) {
        int panelWidth = calculatePanelWidth(screenWidth);
        int panelHeight = calculatePanelHeight(screenHeight);
        int left = Math.max(0, (screenWidth - panelWidth) / 2);
        int top = Math.max(0, (screenHeight - panelHeight) / 2);
        int padding = panelWidth >= 220 ? 10 : 5;
        int headerHeight = panelHeight >= 200 ? 44 : panelHeight >= 130 ? 32 : 20;
        int buttonHeight = panelHeight >= 140 ? 20 : panelHeight >= 100 ? 16 : 14;
        int footerInset = panelHeight >= 140 ? 7 : 4;
        int footerY = top + panelHeight - buttonHeight - footerInset;
        int contentTop = top + headerHeight + 4;
        int contentBottom = Math.max(contentTop + 1, footerY - 5);
        int innerX = left + padding;
        int innerWidth = Math.max(1, panelWidth - padding * 2);
        int contentHeight = Math.max(1, contentBottom - contentTop);
        boolean wide = panelWidth >= WIDE_LAYOUT_WIDTH && panelHeight >= WIDE_LAYOUT_HEIGHT
                && contentHeight >= 150;

        Rect header = new Rect(innerX, top + 4, innerWidth, Math.max(12, headerHeight - 4));
        Rect slotPane;
        Rect learnedPane;
        int slotSize;
        int slotGap;
        int slotRowHeight;
        if (wide) {
            int gap = 8;
            int slotWidth = Math.min(174, Math.max(142, innerWidth * 2 / 5));
            slotPane = new Rect(innerX, contentTop, slotWidth, contentHeight);
            learnedPane = new Rect(slotPane.right() + gap, contentTop,
                    Math.max(1, innerX + innerWidth - slotPane.right() - gap), contentHeight);
            slotSize = 18;
            slotGap = 0;
            slotRowHeight = Math.max(18, Math.min(WIDE_SLOT_ROW_HEIGHT,
                    Math.max(1, (contentHeight - 18) / SLOT_COUNT)));
        } else {
            slotGap = innerWidth >= 42 ? 2 : 1;
            slotSize = Math.max(8, Math.min(18, (innerWidth - slotGap * (SLOT_COUNT - 1)) / SLOT_COUNT));
            slotRowHeight = slotSize;
            int desiredSlotHeight = slotSize + (contentHeight >= 50 ? 18 : 4);
            int paneGap = contentHeight >= 20 ? 3 : 1;
            int slotHeight = Math.min(desiredSlotHeight, Math.max(1, contentHeight - paneGap - 1));
            slotPane = new Rect(innerX, contentTop, innerWidth, slotHeight);
            learnedPane = new Rect(innerX, slotPane.bottom() + paneGap, innerWidth,
                    Math.max(1, contentBottom - slotPane.bottom() - paneGap));
        }

        int closeWidth = Math.min(74, Math.max(1, innerWidth));
        Rect closeButton = new Rect(innerX + Math.max(0, innerWidth - closeWidth), footerY,
                closeWidth, buttonHeight);
        return new Layout(left, top, panelWidth, panelHeight, wide, header, slotPane, learnedPane,
                closeButton, slotSize, slotGap, slotRowHeight);
    }

    private static Rect slotHitRect(Layout layout, int index) {
        Rect pane = layout.slotPane();
        if (layout.wide()) {
            int y = pane.y() + 17 + index * layout.slotRowHeight();
            return new Rect(pane.x() + 3, y, Math.max(1, pane.width() - 6), layout.slotRowHeight());
        }
        int totalWidth = layout.slotSize() * SLOT_COUNT + layout.slotGap() * (SLOT_COUNT - 1);
        int startX = pane.x() + Math.max(1, (pane.width() - totalWidth) / 2);
        int header = pane.height() >= layout.slotSize() + 16 ? 16 : 2;
        return new Rect(startX + index * (layout.slotSize() + layout.slotGap()), pane.y() + header,
                layout.slotSize(), layout.slotSize());
    }

    private static Rect slotIconRect(Layout layout, int index) {
        Rect hit = slotHitRect(layout, index);
        if (!layout.wide()) return hit;
        int size = Math.min(layout.slotSize(), Math.max(1, hit.height() - 2));
        return new Rect(hit.x() + 2, hit.y() + Math.max(0, (hit.height() - size) / 2), size, size);
    }

    private static Rect learnedViewport(Layout layout) {
        Rect pane = layout.learnedPane();
        int header = pane.height() >= 28 ? 18 : 2;
        return new Rect(pane.x() + 4, pane.y() + header, Math.max(1, pane.width() - 10),
                Math.max(1, pane.height() - header - 3));
    }

    record Rect(int x, int y, int width, int height) {
        int right() {
            return x + width;
        }

        int bottom() {
            return y + height;
        }

        boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX < right() && mouseY >= y && mouseY < bottom();
        }
    }

    record Layout(int left, int top, int panelWidth, int panelHeight, boolean wide,
                  Rect header, Rect slotPane, Rect learnedPane, Rect closeButton,
                  int slotSize, int slotGap, int slotRowHeight) {
    }
}
