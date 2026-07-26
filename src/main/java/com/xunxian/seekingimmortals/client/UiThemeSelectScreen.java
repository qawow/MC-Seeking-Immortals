package com.xunxian.seekingimmortals.client;

import com.xunxian.seekingimmortals.client.ui.InkScene;
import com.xunxian.seekingimmortals.client.ui.UiTheme;
import com.xunxian.seekingimmortals.client.ui.UiThemeConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * List-style UI theme picker opened from the cultivation panel's theme button.
 * Each row shows the theme's 静室 ground/accent/cinnabar swatches plus its name;
 * clicking a row applies the theme instantly (live palette rebind) and persists
 * it through {@link UiThemeConfig#select}. The whole screen repaints in the
 * newly chosen theme, acting as its own preview.
 */
public class UiThemeSelectScreen extends AbstractJournalScreen {
    private static final int MAX_PANEL_WIDTH = 300;
    private static final int MAX_PANEL_HEIGHT = 330;
    private static final int PANEL_MARGIN = 4;
    private static final int ROW_HEIGHT = 20;
    private static final int ROW_GAP = 2;

    private final Screen parent;
    private final ScrollableListPanel listPanel = new ScrollableListPanel();

    public UiThemeSelectScreen(Screen parent) {
        super(Component.translatable("screen.seeking_immortals.ui_theme.title"));
        this.parent = parent;
        listPanel.setScrollStep(ROW_HEIGHT + ROW_GAP)
                .setRowMetrics(ROW_HEIGHT, ROW_GAP)
                .setScissorInsets(1, 1, 1, 1)
                .setScrollbarInsetRight(3)
                .setScrollbarTrackInsets(1, 1);
    }

    @Override
    protected UiClimate defaultClimate() {
        return UiClimate.JADE_SLIP;
    }

    @Override
    protected void init() {
        super.init();
        Layout layout = calculateLayout(width, height);
        addRenderableWidget(ImmortalButton.secondary(layout.done().x(), layout.done().y(),
                layout.done().width(), layout.done().height(),
                Component.translatable("gui.done"), button -> onClose()));
    }

    @Override
    protected JournalChrome journalChrome() {
        Layout layout = calculateLayout(width, height);
        return new JournalChrome(layout.panel().x(), layout.panel().y(),
                layout.panel().width(), layout.panel().height(), layout.header(), null);
    }

    @Override
    protected void renderJournalContent(GuiGraphics graphics, JournalChrome chrome,
                                        int mouseX, int mouseY, float partialTick) {
        Layout layout = calculateLayout(width, height);
        ImmortalUiSkin.drawStringFit(font, graphics,
                Component.translatable("screen.seeking_immortals.ui_theme.hint").getString(),
                layout.hint().x(), layout.hint().y(), layout.hint().width(),
                ImmortalUiSkin.JOURNAL_PAPER_MUTED, false);
        ImmortalUiSkin.drawInnerFrame(graphics, layout.list().x() - 2, layout.list().y() - 2,
                layout.list().width() + 4, layout.list().height() + 4);

        UiTheme[] themes = UiTheme.values();
        listPanel.setBounds(layout.list()).setContentRows(themes.length);
        listPanel.renderRows(graphics, themes.length, mouseX, mouseY, (g, index, bounds, state, hovered) -> {
            UiTheme theme = themes[index];
            boolean current = theme == UiTheme.active();
            if (current) {
                ImmortalUiSkin.drawListRow(g, bounds.x(), bounds.y(), bounds.width(), bounds.height(),
                        ImmortalUiSkin.InteractionState.SELECTED);
            }
            renderThemeRow(g, theme, bounds, current);
        });
    }

    private void renderThemeRow(GuiGraphics graphics, UiTheme theme, UiRect bounds, boolean current) {
        UiClimate.Palette preview = theme.paletteFor(InkScene.QUIET_STUDY);
        int chipY = bounds.y() + (bounds.height() - 8) / 2;
        int chipX = bounds.x() + 5;
        drawSwatch(graphics, chipX, chipY, preview.panel(), preview.border());
        drawSwatch(graphics, chipX + 10, chipY, preview.accent(), preview.border());
        drawSwatch(graphics, chipX + 20, chipY, preview.cinnabar(), preview.border());

        String tag = current
                ? Component.translatable("screen.seeking_immortals.ui_theme.current").getString()
                : "";
        int tagWidth = tag.isEmpty() ? 0 : font.width(tag) + 6;
        int textX = chipX + 32;
        int textY = bounds.y() + (bounds.height() - 8) / 2;
        ImmortalUiSkin.drawStringFit(font, graphics,
                Component.translatable(theme.displayNameKey()).getString(), textX, textY,
                Math.max(1, bounds.right() - 8 - tagWidth - textX),
                ImmortalUiSkin.JOURNAL_PAPER, false);
        if (!tag.isEmpty()) {
            graphics.drawString(font, tag, bounds.right() - 8 - font.width(tag), textY,
                    ImmortalUiSkin.JOURNAL_SPIRIT, false);
        }
    }

    private static void drawSwatch(GuiGraphics graphics, int x, int y, int fill, int border) {
        graphics.fill(x, y, x + 8, y + 8, border);
        graphics.fill(x + 1, y + 1, x + 7, y + 7, fill);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            Layout layout = calculateLayout(width, height);
            UiTheme[] themes = UiTheme.values();
            if (layout.list().contains(mouseX, mouseY)) {
                listPanel.setBounds(layout.list()).setContentRows(themes.length);
                int local = listPanel.hoveredRow((int) mouseX, (int) mouseY, themes.length);
                if (local >= 0) {
                    int index = listPanel.firstVisibleRow() + local;
                    if (index >= 0 && index < themes.length) {
                        UiThemeConfig.select(themes[index]);
                        return true;
                    }
                }
            }
        }
        if (listPanel.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        Layout layout = calculateLayout(width, height);
        UiTheme[] themes = UiTheme.values();
        listPanel.setBounds(layout.list()).setContentRows(themes.length);
        if (listPanel.mouseScrolledRows(mouseX, mouseY, delta, themes.length)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (listPanel.mouseDragged(mouseX, mouseY, button, dragX, dragY)) {
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (listPanel.mouseReleased(mouseX, mouseY, button)) {
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void onClose() {
        if (minecraft != null && parent != null) {
            minecraft.setScreen(parent);
            return;
        }
        super.onClose();
    }

    private Layout calculateLayout(int screenWidth, int screenHeight) {
        int panelWidth = Math.min(MAX_PANEL_WIDTH, screenWidth - PANEL_MARGIN * 2);
        int panelHeight = Math.min(MAX_PANEL_HEIGHT, screenHeight - PANEL_MARGIN * 2);
        int left = (screenWidth - panelWidth) / 2;
        int top = (screenHeight - panelHeight) / 2;
        UiRect panel = new UiRect(left, top, panelWidth, panelHeight);
        UiRect header = new UiRect(left + 6, top + 6, panelWidth - 12, 18);
        UiRect hint = new UiRect(left + 10, header.bottom() + 4, panelWidth - 20, 10);
        UiRect done = new UiRect(left + (panelWidth - 96) / 2, top + panelHeight - 24, 96, 18);
        int listTop = hint.bottom() + 4;
        UiRect list = new UiRect(left + 8, listTop, panelWidth - 16,
                Math.max(ROW_HEIGHT, done.y() - 6 - listTop));
        return new Layout(panel, header, hint, list, done);
    }

    private record Layout(UiRect panel, UiRect header, UiRect hint, UiRect list, UiRect done) {
    }
}
