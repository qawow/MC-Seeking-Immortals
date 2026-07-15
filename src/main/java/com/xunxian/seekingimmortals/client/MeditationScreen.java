package com.xunxian.seekingimmortals.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.Locale;

/** Responsive, read-only breakdown of the current meditation cycle. */
public class MeditationScreen extends Screen {
    private static final int PANEL_MARGIN = 4;
    private static final int DEFAULT_PANEL_WIDTH = 440;
    private static final int DEFAULT_PANEL_HEIGHT = 310;
    private static final int COLUMN_BREAKPOINT = 340;
    private static final int COLUMN_GAP = 10;
    private static final int ROW_HEIGHT = 13;

    private int scrollOffset;
    private int renderedContentHeight;

    public MeditationScreen() {
        super(Component.translatable("screen.seeking_immortals.meditation.title"));
    }

    @Override
    protected void init() {
        super.init();
        MeditationLayout layout = calculateLayout(width, height);
        addRenderableWidget(ImmortalButton.secondary(
                layout.closeButton().x(), layout.closeButton().y(),
                layout.closeButton().width(), layout.closeButton().height(),
                Component.translatable("gui.done"), button -> onClose()));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        MeditationLayout layout = calculateLayout(width, height);
        drawFrame(graphics, layout);

        UiRect viewport = layout.viewport();
        renderedContentHeight = ClientCultivationData.isSynced()
                ? calculateContentHeight(layout.columns()) : viewport.height();
        scrollOffset = clampScroll(scrollOffset, renderedContentHeight, viewport.height());
        int startY = viewport.y() + 5 - scrollOffset;

        ImmortalUiSkin.withScissor(graphics, viewport.x(), viewport.y(), viewport.width(), viewport.height(), () -> {
            if (!ClientCultivationData.isSynced()) {
                ImmortalUiSkin.drawWrappedText(font, graphics,
                        Component.translatable("screen.seeking_immortals.meditation.waiting_sync"),
                        viewport.x() + 8, viewport.y() + 10, Math.max(1, viewport.width() - 16),
                        Math.max(1, viewport.height() - 16), ImmortalUiSkin.JOURNAL_PAPER_MUTED, false);
                return;
            }
            renderDashboard(graphics, layout, startY);
        });
        ImmortalUiSkin.drawThinScrollbar(graphics, viewport.right() - 3, viewport.y(), viewport.height(),
                renderedContentHeight, viewport.height(), scrollOffset);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        MeditationLayout layout = calculateLayout(width, height);
        if (layout.viewport().contains(mouseX, mouseY) && renderedContentHeight > layout.viewport().height()) {
            scrollOffset = clampScroll(scrollOffset - (int)Math.round(delta * 18.0D),
                    renderedContentHeight, layout.viewport().height());
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    static MeditationLayout calculateLayout(int screenWidth, int screenHeight) {
        int panelWidth = Math.min(DEFAULT_PANEL_WIDTH, Math.max(1, screenWidth - PANEL_MARGIN * 2));
        int panelHeight = Math.min(DEFAULT_PANEL_HEIGHT, Math.max(1, screenHeight - PANEL_MARGIN * 2));
        int left = Math.max(0, (screenWidth - panelWidth) / 2);
        int top = Math.max(0, (screenHeight - panelHeight) / 2);
        int padding = panelWidth >= 180 ? 10 : 3;
        int headerHeight = panelHeight >= 150 ? 32 : panelHeight >= 80 ? 22 : 14;
        int buttonHeight = Math.min(20, Math.max(10, panelHeight / 7));
        int buttonWidth = Math.min(64, Math.max(1, panelWidth - padding * 2));
        int buttonY = Math.max(top, top + panelHeight - padding - buttonHeight);
        int viewportY = Math.min(buttonY, top + headerHeight + 4);
        int viewportBottom = Math.max(viewportY + 1, buttonY - 5);
        UiRect header = new UiRect(left + padding, top + 4,
                Math.max(1, panelWidth - padding * 2), Math.max(1, headerHeight - 4));
        UiRect viewport = new UiRect(left + padding, viewportY,
                Math.max(1, panelWidth - padding * 2), Math.max(1, viewportBottom - viewportY));
        UiRect closeButton = new UiRect(left + panelWidth - padding - buttonWidth, buttonY,
                buttonWidth, Math.min(buttonHeight, Math.max(1, top + panelHeight - buttonY)));
        return new MeditationLayout(left, top, panelWidth, panelHeight,
                viewport.width() >= COLUMN_BREAKPOINT, header, viewport, closeButton);
    }

    static int calculateContentHeight(boolean columns) {
        int stateHeight = 36;
        int efficiencyHeight = 18 + ROW_HEIGHT * 4;
        int reserveHeight = 18 + 23 * 2 + ROW_HEIGHT + 8 + 18 + 23;
        return stateHeight + (columns ? Math.max(efficiencyHeight, reserveHeight) : efficiencyHeight + 8 + reserveHeight) + 10;
    }

    static int clampScroll(int requested, int contentHeight, int viewportHeight) {
        int maximum = Math.max(0, contentHeight - Math.max(1, viewportHeight));
        return Math.max(0, Math.min(requested, maximum));
    }

    private void drawFrame(GuiGraphics graphics, MeditationLayout layout) {
        ImmortalUiSkin.drawLayeredPanel(graphics, layout.left(), layout.top(),
                layout.panelWidth(), layout.panelHeight());
        ImmortalUiSkin.drawTitleBar(graphics, layout.header().x(), layout.header().y(),
                layout.header().width(), layout.header().height());
        graphics.drawCenteredString(font,
                ImmortalUiSkin.fitWidth(font, title.getString(), Math.max(1, layout.header().width() - 16)),
                layout.header().x() + layout.header().width() / 2,
                layout.header().y() + Math.max(2, (layout.header().height() - 8) / 2),
                ImmortalUiSkin.JOURNAL_BORDER);
        ImmortalUiSkin.drawInnerFrame(graphics, layout.viewport().x(), layout.viewport().y(),
                layout.viewport().width(), layout.viewport().height());
    }

    private void renderDashboard(GuiGraphics graphics, MeditationLayout layout, int y) {
        ClientCultivationData.Snapshot data = ClientCultivationData.getSnapshot();
        boolean meditating = ClientCultivationData.effectiveMeditating();
        UiRect viewport = layout.viewport();
        int x = viewport.x() + 5;
        int contentWidth = Math.max(1, viewport.width() - 12);

        ImmortalUiSkin.drawListRow(graphics, x, y, contentWidth, 31,
                meditating ? ImmortalUiSkin.InteractionState.SELECTED : ImmortalUiSkin.InteractionState.NORMAL);
        Component state = Component.translatable("screen.seeking_immortals.meditation.state",
                Component.translatable(meditating
                        ? "screen.seeking_immortals.meditation.active"
                        : "screen.seeking_immortals.meditation.idle"));
        ImmortalUiSkin.drawStringFit(font, graphics, state.getString(), x + 7, y + 5,
                Math.max(1, contentWidth - 14),
                meditating ? ImmortalUiSkin.JOURNAL_JADE_TEXT : ImmortalUiSkin.JOURNAL_PAPER, false);
        ImmortalUiSkin.drawStringFit(font, graphics,
                Component.translatable("screen.seeking_immortals.meditation.settlement",
                        fmt(data.meditationTotalPerSecond())).getString(),
                x + 7, y + 17, Math.max(1, contentWidth - 14), ImmortalUiSkin.JOURNAL_SPIRIT, false);

        int sectionsY = y + 36;
        if (layout.columns()) {
            int columnWidth = Math.max(1, (contentWidth - COLUMN_GAP) / 2);
            renderEfficiency(graphics, x, sectionsY, columnWidth, data);
            renderReserves(graphics, x + columnWidth + COLUMN_GAP, sectionsY,
                    Math.max(1, contentWidth - columnWidth - COLUMN_GAP), data, meditating);
        } else {
            int nextY = renderEfficiency(graphics, x, sectionsY, contentWidth, data);
            renderReserves(graphics, x, nextY + 8, contentWidth, data, meditating);
        }
    }

    private int renderEfficiency(GuiGraphics graphics, int x, int y, int width,
                                 ClientCultivationData.Snapshot data) {
        y = sectionTitle(graphics, x, y, width,
                Component.translatable("screen.seeking_immortals.meditation.section.efficiency"));
        y = row(graphics, x, y, width,
                Component.translatable("screen.seeking_immortals.meditation.efficiency"),
                Component.literal("x" + fmt(data.cultivationSpeedMultiplier())), ImmortalUiSkin.JOURNAL_BORDER);
        y = row(graphics, x, y, width,
                Component.translatable("screen.seeking_immortals.meditation.root_factor"),
                Component.literal("x" + fmt(data.rootCultivationSpeedCoefficient()) + " / x"
                        + fmt(data.meditationRootMultiplier())), ImmortalUiSkin.JOURNAL_JADE_TEXT);
        y = row(graphics, x, y, width,
                Component.translatable("screen.seeking_immortals.meditation.body_method_factor"),
                Component.literal("x" + fmt(data.physiqueCultivationSpeedMultiplier()) + " / x"
                        + fmt(data.meditationTechniqueMultiplier())), ImmortalUiSkin.JOURNAL_PAPER);
        return row(graphics, x, y, width,
                Component.translatable("screen.seeking_immortals.meditation.aura"),
                Component.literal(data.auraConcentration() + " · " + data.auraNature() + " · x"
                        + fmt(data.meditationAuraMultiplier())), ImmortalUiSkin.JOURNAL_SPIRIT);
    }

    private int renderReserves(GuiGraphics graphics, int x, int y, int width,
                               ClientCultivationData.Snapshot data, boolean meditating) {
        y = sectionTitle(graphics, x, y, width,
                Component.translatable("screen.seeking_immortals.meditation.section.reserves"));
        y = meter(graphics, x, y, width,
                Component.translatable("screen.seeking_immortals.meditation.cultivation"),
                data.cultivation() + " / " + data.cultivationMax(),
                fraction(data.cultivation(), data.cultivationMax()), ImmortalUiSkin.StatusBarStyle.CULTIVATION);
        y = meter(graphics, x, y, width,
                Component.translatable("screen.seeking_immortals.meditation.mana"),
                data.mana() + " / " + data.manaMax(),
                fraction(data.mana(), data.manaMax()), ImmortalUiSkin.StatusBarStyle.SPIRIT);
        y = row(graphics, x, y, width,
                Component.translatable("screen.seeking_immortals.meditation.gold_core"),
                Component.literal(data.goldCoreGrade() + " · " + data.goldCoreScore()),
                data.goldCoreScore() > 0 ? ImmortalUiSkin.JOURNAL_BORDER : ImmortalUiSkin.JOURNAL_PAPER_MUTED);
        y += 8;
        y = sectionTitle(graphics, x, y, width,
                Component.translatable("screen.seeking_immortals.meditation.section.cycle"));
        double cycle = meditating && minecraft != null && minecraft.player != null
                ? (minecraft.player.tickCount % 100) / 100.0D : 0.0D;
        return meter(graphics, x, y, width,
                Component.translatable("screen.seeking_immortals.meditation.cycle_progress"),
                Math.round(cycle * 100.0D) + "%", cycle,
                meditating ? ImmortalUiSkin.StatusBarStyle.CULTIVATION : ImmortalUiSkin.StatusBarStyle.NEUTRAL);
    }

    private int sectionTitle(GuiGraphics graphics, int x, int y, int width, Component value) {
        ImmortalUiSkin.drawTitleBar(graphics, x, y, width, 14);
        ImmortalUiSkin.drawStringFit(font, graphics, value.getString(), x + 8, y + 3,
                Math.max(1, width - 12), ImmortalUiSkin.JOURNAL_BORDER, false);
        return y + 18;
    }

    private int row(GuiGraphics graphics, int x, int y, int width, Component label,
                    Component value, int color) {
        ImmortalUiSkin.drawListRow(graphics, x, y, width, ROW_HEIGHT,
                ImmortalUiSkin.InteractionState.NORMAL);
        int labelWidth = Math.min(76, Math.max(38, width / 3));
        ImmortalUiSkin.drawStringFit(font, graphics, label.getString(), x + 5, y + 2,
                Math.max(1, labelWidth - 5), ImmortalUiSkin.JOURNAL_PAPER_MUTED, false);
        ImmortalUiSkin.drawStringFit(font, graphics, value.getString(), x + labelWidth + 3, y + 2,
                Math.max(1, width - labelWidth - 7), color, false);
        return y + ROW_HEIGHT;
    }

    private int meter(GuiGraphics graphics, int x, int y, int width, Component label,
                      String value, double fraction, ImmortalUiSkin.StatusBarStyle style) {
        int valueWidth = Math.min(width / 2, font.width(value));
        ImmortalUiSkin.drawStringFit(font, graphics, label.getString(), x + 2, y,
                Math.max(1, width - valueWidth - 6), ImmortalUiSkin.JOURNAL_PAPER_MUTED, false);
        ImmortalUiSkin.drawStringFit(font, graphics, value, x + width - valueWidth, y,
                Math.max(1, valueWidth), ImmortalUiSkin.JOURNAL_PAPER, false);
        ImmortalUiSkin.drawSemanticStatusBar(graphics, x, y + 11, width, 7, fraction, style);
        return y + 23;
    }

    private static double fraction(long value, long maximum) {
        return maximum <= 0 ? 0.0D : value / (double)maximum;
    }

    private static String fmt(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    record UiRect(int x, int y, int width, int height) {
        int right() {
            return x + width;
        }

        int bottom() {
            return y + height;
        }

        boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX < right() && mouseY >= y && mouseY < y + height;
        }

        boolean intersects(UiRect other) {
            return other != null && x < other.right() && right() > other.x()
                    && y < other.bottom() && bottom() > other.y();
        }
    }

    record MeditationLayout(int left, int top, int panelWidth, int panelHeight, boolean columns,
                            UiRect header, UiRect viewport, UiRect closeButton) {}
}
