package com.xunxian.seekingimmortals.client;

import com.xunxian.seekingimmortals.lore.LoreCompendiumService;
import com.xunxian.seekingimmortals.lore.NameAliasGlossaryService;
import com.xunxian.seekingimmortals.lore.NumericOverviewService;
import com.xunxian.seekingimmortals.lore.VisualStyleService;
import com.xunxian.seekingimmortals.util.PlayerDisplayText;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

/** M16 encyclopedia hub: category summary, glossary and numeric/visual quick reference. */
public class LoreCompendiumScreen extends AbstractLoreScreen {
    public enum Tab {
        HUB,
        GLOSSARY,
        NUMERIC,
        VISUAL
    }

    private static final int WIDE_CONTROLS_WIDTH = 380;
    private static final int MEDIUM_CONTROLS_WIDTH = 280;
    private static final int ROW_H = 14;

    private Tab tab;
    private int scroll;
    private List<String> lines = List.of();

    public LoreCompendiumScreen() {
        this(Tab.HUB);
    }

    public LoreCompendiumScreen(Tab tab) {
        super(Component.translatable("screen.seeking_immortals.compendium.title"));
        this.tab = tab == null ? Tab.HUB : tab;
    }

    @Override
    public void refreshFromSync() {
        rebuildLines();
    }

    public boolean isShowing(Tab target) {
        return tab == target;
    }

    @Override
    protected PanelBounds lorePanelBounds() {
        Layout layout = calculateLayout(width, height);
        return new PanelBounds(layout.left(), layout.top(), layout.width(), layout.height());
    }

    @Override
    protected void init() {
        super.init();
        rebuildLines();
        rebuildActionWidgets();
    }

    private void rebuildActionWidgets() {
        clearWidgets();
        Layout layout = calculateLayout(width, height);
        addRefreshAndClose(layout.refresh().x(), layout.refresh().y(), layout.refresh().w(), layout.refresh().h(),
                layout.close().x(), layout.close().y(), layout.close().w(), layout.close().h(),
                tabAction());

        TabBar<Tab> tabs = new TabBar<>(tab);
        tabs.addTab(Tab.HUB, Component.translatable("screen.seeking_immortals.compendium.tab_hub"),
                        toUiRect(layout.hubTab().x(), layout.hubTab().y(), layout.hubTab().w(), layout.hubTab().h()))
                .addTab(Tab.GLOSSARY, Component.translatable("screen.seeking_immortals.compendium.tab_glossary"),
                        toUiRect(layout.glossaryTab().x(), layout.glossaryTab().y(),
                                layout.glossaryTab().w(), layout.glossaryTab().h()))
                .addTab(Tab.NUMERIC, Component.translatable("screen.seeking_immortals.compendium.tab_numeric"),
                        toUiRect(layout.numericTab().x(), layout.numericTab().y(),
                                layout.numericTab().w(), layout.numericTab().h()))
                .addTab(Tab.VISUAL, Component.translatable("screen.seeking_immortals.compendium.tab_visual"),
                        toUiRect(layout.visualTab().x(), layout.visualTab().y(),
                                layout.visualTab().w(), layout.visualTab().h()))
                .setOnSelect(this::setTab);
        for (ImmortalButton button : tabs.attach(null)) {
            addRenderableWidget(button);
        }

        // Inter-screen jumps stay as secondary action buttons (not tabs).
        addRenderableWidget(ImmortalButton.secondary(layout.bestiaryBtn().x(), layout.bestiaryBtn().y(),
                layout.bestiaryBtn().w(), layout.bestiaryBtn().h(),
                Component.translatable("screen.seeking_immortals.compendium.open_bestiary"),
                b -> sendLoreAction("bestiary")));
        addRenderableWidget(ImmortalButton.secondary(layout.chronicleBtn().x(), layout.chronicleBtn().y(),
                layout.chronicleBtn().w(), layout.chronicleBtn().h(),
                Component.translatable("screen.seeking_immortals.compendium.open_chronicle"),
                b -> sendLoreAction("chronicle")));
    }

    private void setTab(Tab next) {
        if (tab != next) {
            tab = next;
            scroll = 0;
            rebuildLines();
            rebuildActionWidgets();
        }
    }

    private String tabAction() {
        return actionForTab(tab);
    }

    static String actionForTab(Tab tab) {
        return switch (tab == null ? Tab.HUB : tab) {
            case GLOSSARY -> "glossary";
            case NUMERIC -> "numeric";
            case VISUAL -> "visual";
            case HUB -> "compendium";
        };
    }

    private void rebuildLines() {
        List<String> out = new ArrayList<>();
        switch (tab) {
            case HUB -> {
                LoreCompendiumService.HubSummary hub = LoreCompendiumService.hub();
                out.add(Component.translatable("screen.seeking_immortals.compendium.hub_header").getString());
                out.addAll(hub.lines());
                out.add("");
                out.add(Component.translatable("screen.seeking_immortals.compendium.progress").getString());
                out.add(Component.translatable("screen.seeking_immortals.compendium.progress_bestiary",
                        ClientLoreData.bestiaryUnlockedCount()).getString());
                out.add(Component.translatable("screen.seeking_immortals.compendium.progress_chronicle",
                        ClientLoreData.chronicleDiscoveredCount()).getString());
                out.add(Component.translatable("screen.seeking_immortals.compendium.progress_timeline",
                        ClientLoreData.timelinePhaseCount()).getString());
            }
            case GLOSSARY -> {
                out.add(Component.translatable("screen.seeking_immortals.compendium.glossary_header",
                        NameAliasGlossaryService.size()).getString());
                for (String tip : NameAliasGlossaryService.builtin().searchTips()) {
                    if (PlayerDisplayText.isSafe(tip)) {
                        out.add(tip);
                    }
                }
                int n = 0;
                for (NameAliasGlossaryService.GlossaryEntry entry : NameAliasGlossaryService.all()) {
                    if (!PlayerDisplayText.isSafe(entry.primary())) {
                        continue;
                    }
                    out.add(entry.primary());
                    if (++n >= 120) {
                        out.add("...");
                        break;
                    }
                }
            }
            case NUMERIC -> {
                NumericOverviewService.Snapshot snap = NumericOverviewService.builtin();
                if (!snap.present()) {
                    out.add(Component.translatable("screen.seeking_immortals.compendium.missing_numeric").getString());
                } else {
                    out.add(Component.translatable("screen.seeking_immortals.compendium.numeric_header").getString());
                    out.add(Component.translatable("screen.seeking_immortals.compendium.section.currency").getString());
                    out.addAll(snap.currencyLines());
                    out.add(Component.translatable("screen.seeking_immortals.compendium.section.breakthrough").getString());
                    out.addAll(snap.breakthroughLines());
                    out.add(Component.translatable("screen.seeking_immortals.compendium.section.threat").getString());
                    out.addAll(snap.threatLines());
                    out.add(Component.translatable("screen.seeking_immortals.compendium.section.snapshot").getString());
                    out.addAll(snap.summaryLines());
                }
            }
            case VISUAL -> {
                VisualStyleService.Snapshot snap = VisualStyleService.builtin();
                if (!snap.present()) {
                    out.add(Component.translatable("screen.seeking_immortals.compendium.missing_visual").getString());
                } else {
                    out.add(Component.translatable("screen.seeking_immortals.compendium.visual_header").getString());
                    String description = snap.displayDescription();
                    if (!description.isBlank()) {
                        out.add(description);
                    }
                    out.add(Component.translatable("screen.seeking_immortals.compendium.section.palette").getString());
                    out.addAll(snap.paletteLines());
                    out.add(Component.translatable("screen.seeking_immortals.compendium.section.counts").getString());
                    out.addAll(snap.countLines());
                }
            }
        }
        lines = List.copyOf(out);
    }

    @Override
    protected void renderJournalContent(GuiGraphics graphics, JournalChrome chrome,
                                        int mouseX, int mouseY, float partialTick) {
        Layout layout = calculateLayout(width, height);
        ImmortalUiSkin.drawInnerFrame(graphics, layout.viewport().x(), layout.viewport().y(),
                layout.viewport().w(), layout.viewport().h());

        List<FormattedCharSequence> visualLines = wrappedLines(layout.viewport().w() - 12);
        int contentHeight = Math.max(1, visualLines.size() * ROW_H);
        int visible = Math.max(1, layout.viewport().h());
        scroll = Mth.clamp(scroll, 0, Math.max(0, contentHeight - visible));
        ImmortalUiSkin.withScissor(graphics, layout.viewport().x() + 1, layout.viewport().y() + 1,
                Math.max(1, layout.viewport().w() - 2), Math.max(1, layout.viewport().h() - 2), () -> {
                    int y = layout.viewport().y() + 4 - scroll;
                    for (FormattedCharSequence line : visualLines) {
                        graphics.drawString(font, line,
                                layout.viewport().x() + 6, y, ImmortalUiSkin.JOURNAL_PAPER, false);
                        y += ROW_H;
                    }
                });
        ImmortalUiSkin.drawThinScrollbar(graphics, layout.viewport().x() + layout.viewport().w() - 3,
                layout.viewport().y() + 1, Math.max(1, layout.viewport().h() - 2),
                contentHeight, visible, scroll);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        Layout layout = calculateLayout(width, height);
        if (layout.viewport().contains(mouseX, mouseY)) {
            int contentHeight = Math.max(1, wrappedLines(layout.viewport().w() - 12).size() * ROW_H);
            int visible = Math.max(1, layout.viewport().h());
            scroll = Mth.clamp(scroll - (int) Math.round(delta * ROW_H), 0, Math.max(0, contentHeight - visible));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    private List<FormattedCharSequence> wrappedLines(int availableWidth) {
        int wrapWidth = Math.max(1, availableWidth);
        List<FormattedCharSequence> wrapped = new ArrayList<>();
        for (String line : lines) {
            List<FormattedCharSequence> split = font.split(
                    Component.literal(line == null ? "" : line), wrapWidth);
            if (split.isEmpty()) {
                wrapped.add(Component.empty().getVisualOrderText());
            } else {
                wrapped.addAll(split);
            }
        }
        return List.copyOf(wrapped);
    }

    static Layout calculateLayout(int width, int height) {
        int screenWidth = Math.max(1, width);
        int screenHeight = Math.max(1, height);
        int panelWidth = Math.min(MAX_PANEL_W, Math.max(1, screenWidth - Math.min(PANEL_MARGIN * 2, screenWidth - 1)));
        int panelHeight = Math.min(MAX_PANEL_H, Math.max(1, screenHeight - Math.min(PANEL_MARGIN * 2, screenHeight - 1)));
        int left = Math.max(0, (screenWidth - panelWidth) / 2);
        int top = Math.max(0, (screenHeight - panelHeight) / 2);
        int pad = Math.min(8, Math.max(1, panelWidth / 12));
        int innerX = left + pad;
        int innerWidth = Math.max(1, panelWidth - pad * 2);
        int controlColumns = panelWidth >= WIDE_CONTROLS_WIDTH
                ? 6
                : panelWidth >= MEDIUM_CONTROLS_WIDTH ? 3 : 2;

        int titleReserve = Math.min(18, Math.max(10, panelHeight / 6));
        int footerHeight = Math.max(1, Math.min(16, panelHeight / 7));
        int controlRows = 6 / controlColumns;
        int controlGap = Math.min(2, Math.max(0, innerWidth / 12));
        int controlHeight = Math.max(1, Math.min(14, panelHeight / (8 + controlRows)));
        int minViewport = MIN_BODY_LINE;
        int controlsBlock = controlRows * controlHeight + (controlRows - 1) * controlGap;
        int chrome = titleReserve + controlsBlock + footerHeight + 6;
        if (chrome + minViewport > panelHeight) {
            int deficit = chrome + minViewport - panelHeight;
            int cutControl = Math.min(Math.max(0, controlHeight - 1), deficit / Math.max(1, controlRows));
            controlHeight -= cutControl;
            deficit -= cutControl * controlRows;
            int cutFooter = Math.min(Math.max(0, footerHeight - 1), deficit);
            footerHeight -= cutFooter;
            deficit -= cutFooter;
            titleReserve -= Math.min(Math.max(0, titleReserve - 8), deficit);
            controlsBlock = controlRows * controlHeight + (controlRows - 1) * controlGap;
        }
        int controlY = top + titleReserve;
        int footerY = Math.min(top + panelHeight - footerHeight,
                Math.max(controlY + controlsBlock + 2 + minViewport,
                        top + panelHeight - footerHeight - Math.min(4, panelHeight / 12)));
        Rect[] controls = new Rect[6];
        int controlSpaceWidth = Math.max(controlColumns, innerWidth - controlGap * (controlColumns - 1));
        int controlWidth = Math.max(1, controlSpaceWidth / controlColumns);
        for (int i = 0; i < controls.length; i++) {
            int column = i % controlColumns;
            int row = i / controlColumns;
            int x = innerX + column * (controlWidth + controlGap);
            int buttonWidth = column == controlColumns - 1
                    ? Math.max(1, innerX + innerWidth - x)
                    : controlWidth;
            int y = controlY + row * (controlHeight + controlGap);
            controls[i] = new Rect(x, y, buttonWidth, controlHeight);
        }
        Rect hubTab = controls[0];
        Rect glossaryTab = controls[1];
        Rect numericTab = controls[2];
        Rect visualTab = controls[3];
        Rect bestiaryBtn = controls[4];
        Rect chronicleBtn = controls[5];
        int controlsBottom = controlY + controlRows * controlHeight
                + (controlRows - 1) * controlGap;

        int viewportY = Math.min(controlsBottom + 2, Math.max(top, footerY - minViewport));
        int viewportHeight = Math.max(minViewport, footerY - viewportY - 2);
        if (viewportY + viewportHeight > footerY) {
            viewportHeight = Math.max(minViewport, footerY - viewportY);
        }
        Rect viewport = new Rect(innerX, viewportY, innerWidth, viewportHeight);

        int footerGap = Math.min(4, Math.max(0, innerWidth - 2));
        int footerButtonWidth = Math.max(1, Math.min(70, (innerWidth - footerGap) / 2));
        Rect refresh = new Rect(innerX, footerY, footerButtonWidth, footerHeight);
        Rect close = new Rect(innerX + innerWidth - footerButtonWidth, footerY,
                footerButtonWidth, footerHeight);
        return new Layout(left, top, panelWidth, panelHeight, controlColumns, viewport, refresh, close,
                hubTab, glossaryTab, numericTab, visualTab, bestiaryBtn, chronicleBtn);
    }

    record Rect(int x, int y, int w, int h) {
        boolean contains(double mx, double my) {
            return mx >= x && my >= y && mx < x + w && my < y + h;
        }

        int right() {
            return x + w;
        }

        int bottom() {
            return y + h;
        }

        boolean inside(int screenWidth, int screenHeight) {
            return x >= 0 && y >= 0 && w > 0 && h > 0
                    && right() <= screenWidth && bottom() <= screenHeight;
        }

        boolean intersects(Rect other) {
            return x < other.right() && right() > other.x
                    && y < other.bottom() && bottom() > other.y;
        }
    }

    record Layout(int left, int top, int width, int height, int controlColumns,
                          Rect viewport, Rect refresh, Rect close,
                          Rect hubTab, Rect glossaryTab, Rect numericTab, Rect visualTab,
                          Rect bestiaryBtn, Rect chronicleBtn) {}
}
