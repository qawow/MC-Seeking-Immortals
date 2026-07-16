package com.xunxian.seekingimmortals.client;

import com.xunxian.seekingimmortals.catalog.FactionQuestCatalogService;
import com.xunxian.seekingimmortals.network.LoreScreenActionPacket;
import com.xunxian.seekingimmortals.network.ModNetwork;
import com.xunxian.seekingimmortals.quest.TimelineChronicleService;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

/** M16 read-only chronicle / timeline journal. */
public class ChronicleScreen extends Screen {
    private static final int MAX_PANEL_W = 420;
    private static final int MAX_PANEL_H = 260;
    private static final int PANEL_MARGIN = 4;
    private static final int STACKED_BREAKPOINT = 280;
    private static final int ROW_H = 18;
    private static final int MIN_BODY_LINE = 10;

    private enum Tab { EVENTS, TIMELINE }

    private Tab tab = Tab.EVENTS;
    private int listScroll;
    private int selected = -1;
    private List<FactionQuestCatalogService.Entry> events = List.of();
    private List<TimelineChronicleService.TimelinePhase> phases = List.of();

    public ChronicleScreen() {
        super(Component.translatable("screen.seeking_immortals.chronicle.title"));
    }

    public void refreshFromSync() {
        rebuildLists();
    }

    @Override
    protected void init() {
        super.init();
        rebuildLists();
        rebuildActionWidgets();
    }

    private void rebuildActionWidgets() {
        clearWidgets();
        Layout layout = calculateLayout(width, height);
        addRenderableWidget(ImmortalButton.secondary(layout.refresh().x(), layout.refresh().y(),
                layout.refresh().w(), layout.refresh().h(),
                Component.translatable("screen.seeking_immortals.lore.refresh"),
                b -> ModNetwork.CHANNEL.sendToServer(new LoreScreenActionPacket("chronicle"))));
        addRenderableWidget(ImmortalButton.secondary(layout.close().x(), layout.close().y(),
                layout.close().w(), layout.close().h(),
                Component.translatable("gui.done"), b -> onClose()));
        addRenderableWidget(tabButton(layout.eventsTab(), Tab.EVENTS,
                "screen.seeking_immortals.chronicle.tab_events"));
        addRenderableWidget(tabButton(layout.timelineTab(), Tab.TIMELINE,
                "screen.seeking_immortals.chronicle.tab_timeline"));
    }

    private ImmortalButton tabButton(Rect rect, Tab target, String key) {
        return tab == target
                ? ImmortalButton.primary(rect.x(), rect.y(), rect.w(), rect.h(),
                        Component.translatable(key), b -> setTab(target))
                : ImmortalButton.secondary(rect.x(), rect.y(), rect.w(), rect.h(),
                        Component.translatable(key), b -> setTab(target));
    }

    private void setTab(Tab next) {
        if (tab != next) {
            tab = next;
            listScroll = 0;
            selected = -1;
            rebuildActionWidgets();
        }
    }

    private void rebuildLists() {
        events = List.copyOf(FactionQuestCatalogService.builtin().chronicleEvents().values());
        phases = List.copyOf(TimelineChronicleService.builtin().phases());
        if (selected >= (tab == Tab.EVENTS ? events.size() : phases.size())) {
            selected = -1;
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        Layout layout = calculateLayout(width, height);
        ImmortalUiSkin.drawLayeredPanel(graphics, layout.left(), layout.top(), layout.width(), layout.height());
        ImmortalUiSkin.drawTitleBar(graphics, layout.left() + 6, layout.top() + 6, layout.width() - 12, 16);
        String progress = tab == Tab.EVENTS
                ? (ClientLoreData.chronicleDiscoveredCount() + "/" + events.size())
                : (ClientLoreData.timelinePhaseCount() + "/" + phases.size());
        ImmortalUiSkin.drawStringFit(font, graphics, title.getString() + "  " + progress,
                layout.left() + 12, layout.top() + 10, layout.width() - 24, ImmortalUiSkin.JOURNAL_BORDER, false);

        ImmortalUiSkin.drawInnerFrame(graphics, layout.list().x(), layout.list().y(), layout.list().w(), layout.list().h());
        ImmortalUiSkin.drawInnerFrame(graphics, layout.detail().x(), layout.detail().y(), layout.detail().w(), layout.detail().h());

        int total = tab == Tab.EVENTS ? events.size() : phases.size();
        int visible = Math.max(1, layout.list().h() / ROW_H);
        listScroll = Mth.clamp(listScroll, 0, Math.max(0, total - visible));
        ImmortalUiSkin.withScissor(graphics, layout.list().x() + 1, layout.list().y() + 1,
                Math.max(1, layout.list().w() - 2), Math.max(1, layout.list().h() - 2), () -> {
                    for (int i = 0; i < visible && listScroll + i < total; i++) {
                        int index = listScroll + i;
                        int y = layout.list().y() + 2 + i * ROW_H;
                        int bg = index == selected ? ImmortalUiSkin.JOURNAL_ROW_SELECTED
                                : (i % 2 == 0 ? ImmortalUiSkin.JOURNAL_ROW : 0x00000000);
                        if (bg != 0) {
                            graphics.fill(layout.list().x() + 2, y, layout.list().x() + layout.list().w() - 2, y + ROW_H - 1, bg);
                        }
                        String label;
                        int color;
                        if (tab == Tab.EVENTS) {
                            FactionQuestCatalogService.Entry entry = events.get(index);
                            boolean unlocked = ClientLoreData.isChronicleDiscovered(entry.id());
                            label = unlocked ? entry.display() : Component.translatable("screen.seeking_immortals.chronicle.locked").getString();
                            color = unlocked ? ImmortalUiSkin.JOURNAL_PAPER : ImmortalUiSkin.JOURNAL_PAPER_MUTED;
                        } else {
                            TimelineChronicleService.TimelinePhase phase = phases.get(index);
                            boolean unlocked = ClientLoreData.hasTimelinePhase(phase.phase());
                            label = (unlocked ? "● " : "○ ") + phase.phase() + " / " + phase.realm();
                            color = unlocked ? ImmortalUiSkin.JOURNAL_JADE_TEXT : ImmortalUiSkin.JOURNAL_PAPER_MUTED;
                        }
                        ImmortalUiSkin.drawStringFit(font, graphics, label,
                                layout.list().x() + 6, y + 4, layout.list().w() - 12, color, false);
                    }
                });
        ImmortalUiSkin.drawThinScrollbar(graphics, layout.list().x() + layout.list().w() - 3, layout.list().y() + 1,
                Math.max(1, layout.list().h() - 2), total * ROW_H, layout.list().h(), listScroll * ROW_H);

        renderDetail(graphics, layout);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderDetail(GuiGraphics graphics, Layout layout) {
        List<String> lines = new ArrayList<>();
        if (!ClientLoreData.isSynced()) {
            lines.add(Component.translatable("screen.seeking_immortals.lore.not_synced").getString());
        } else if (tab == Tab.EVENTS) {
            if (selected < 0 || selected >= events.size()) {
                lines.add(Component.translatable("screen.seeking_immortals.chronicle.pick").getString());
            } else {
                FactionQuestCatalogService.Entry entry = events.get(selected);
                boolean unlocked = ClientLoreData.isChronicleDiscovered(entry.id());
                lines.add(entry.display() + " [" + entry.id() + "]");
                lines.add(unlocked
                        ? Component.translatable("screen.seeking_immortals.chronicle.discovered").getString()
                        : Component.translatable("screen.seeking_immortals.chronicle.undiscovered").getString());
                lines.add(Component.translatable("screen.seeking_immortals.chronicle.hint").getString());
            }
        } else {
            if (selected < 0 || selected >= phases.size()) {
                lines.add(Component.translatable("screen.seeking_immortals.chronicle.pick_phase").getString());
            } else {
                TimelineChronicleService.TimelinePhase phase = phases.get(selected);
                boolean unlocked = ClientLoreData.hasTimelinePhase(phase.phase());
                lines.add(phase.phase());
                lines.add("realm=" + phase.realm() + " nodes=" + phase.nodeCount());
                lines.add(unlocked
                        ? Component.translatable("screen.seeking_immortals.chronicle.phase_unlocked").getString()
                        : Component.translatable("screen.seeking_immortals.chronicle.phase_locked").getString());
                List<String> mainline = TimelineChronicleService.builtin().mainlineOrder();
                if (!mainline.isEmpty()) {
                    lines.add("mainline:");
                    for (int i = 0; i < Math.min(8, mainline.size()); i++) {
                        lines.add("  - " + mainline.get(i));
                    }
                }
            }
        }
        ImmortalUiSkin.withScissor(graphics, layout.detail().x() + 2, layout.detail().y() + 2,
                Math.max(1, layout.detail().w() - 4), Math.max(1, layout.detail().h() - 4), () -> {
                    int y = layout.detail().y() + 6;
                    for (String line : lines) {
                        List<net.minecraft.util.FormattedCharSequence> wrapped =
                                font.split(Component.literal(line == null ? "" : line),
                                        Math.max(1, layout.detail().w() - 12));
                        for (net.minecraft.util.FormattedCharSequence seq : wrapped) {
                            graphics.drawString(font, seq, layout.detail().x() + 6, y, ImmortalUiSkin.JOURNAL_PAPER, false);
                            y += font.lineHeight + 2;
                        }
                    }
                });
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        Layout layout = calculateLayout(width, height);
        if (layout.list().contains(mouseX, mouseY)) {
            int total = tab == Tab.EVENTS ? events.size() : phases.size();
            int visible = Math.max(1, layout.list().h() / ROW_H);
            int row = (int) ((mouseY - layout.list().y() - 2) / ROW_H);
            if (row >= 0 && row < visible) {
                int index = listScroll + row;
                if (index >= 0 && index < total) {
                    selected = index;
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        Layout layout = calculateLayout(width, height);
        if (layout.list().contains(mouseX, mouseY)) {
            int total = tab == Tab.EVENTS ? events.size() : phases.size();
            int visible = Math.max(1, layout.list().h() / ROW_H);
            listScroll = Mth.clamp(listScroll - (int) Math.round(delta), 0, Math.max(0, total - visible));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
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

        boolean stacked = panelWidth < STACKED_BREAKPOINT;
        int minContent = stacked ? MIN_BODY_LINE * 2 + 2 : MIN_BODY_LINE;
        int titleReserve = Math.min(18, Math.max(10, panelHeight / 6));
        int tabHeight = Math.max(1, Math.min(14, panelHeight / 8));
        int footerHeight = Math.max(1, Math.min(16, panelHeight / 7));
        int chrome = titleReserve + tabHeight + footerHeight + 6;
        if (chrome + minContent > panelHeight) {
            int deficit = chrome + minContent - panelHeight;
            int cutTab = Math.min(Math.max(0, tabHeight - 1), deficit);
            tabHeight -= cutTab;
            deficit -= cutTab;
            int cutFooter = Math.min(Math.max(0, footerHeight - 1), deficit);
            footerHeight -= cutFooter;
            deficit -= cutFooter;
            titleReserve -= Math.min(Math.max(0, titleReserve - 8), deficit);
        }
        int tabY = top + titleReserve;
        int footerY = Math.min(top + panelHeight - footerHeight,
                Math.max(tabY + tabHeight + 2 + minContent, top + panelHeight - footerHeight - Math.min(4, panelHeight / 12)));
        int contentY = tabY + tabHeight + 2;
        int contentHeight = Math.max(minContent, footerY - contentY - 2);
        if (contentY + contentHeight > footerY) {
            contentHeight = Math.max(minContent, footerY - contentY);
        }

        Rect list;
        Rect detail;
        if (stacked) {
            int gap = Math.min(2, Math.max(0, contentHeight - MIN_BODY_LINE * 2));
            int listHeight = Math.max(MIN_BODY_LINE, (contentHeight - gap) / 2);
            int detailY = contentY + listHeight + gap;
            int detailHeight = Math.max(MIN_BODY_LINE, contentY + contentHeight - detailY);
            list = new Rect(innerX, contentY, innerWidth, listHeight);
            detail = new Rect(innerX, detailY, innerWidth, detailHeight);
        } else {
            int gap = Math.min(8, Math.max(0, innerWidth - 2));
            int listWidth = Math.max(1, Math.min(180, (innerWidth - gap) * 44 / 100));
            int detailX = innerX + listWidth + gap;
            int detailWidth = Math.max(1, innerX + innerWidth - detailX);
            list = new Rect(innerX, contentY, listWidth, Math.max(MIN_BODY_LINE, contentHeight));
            detail = new Rect(detailX, contentY, detailWidth, Math.max(MIN_BODY_LINE, contentHeight));
        }

        int footerGap = Math.min(4, Math.max(0, innerWidth - 2));
        int footerButtonWidth = Math.max(1, Math.min(70, (innerWidth - footerGap) / 2));
        Rect refresh = new Rect(innerX, footerY, footerButtonWidth, footerHeight);
        Rect close = new Rect(innerX + innerWidth - footerButtonWidth, footerY, footerButtonWidth, footerHeight);

        int tabGap = Math.min(2, Math.max(0, innerWidth - 2));
        int tabWidth = Math.max(1, (innerWidth - tabGap) / 2);
        Rect eventsTab = new Rect(innerX, tabY, tabWidth, tabHeight);
        Rect timelineTab = new Rect(innerX + innerWidth - tabWidth, tabY, tabWidth, tabHeight);
        return new Layout(left, top, panelWidth, panelHeight, stacked, list, detail,
                refresh, close, eventsTab, timelineTab);
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

    record Layout(int left, int top, int width, int height, boolean stacked, Rect list, Rect detail,
                          Rect refresh, Rect close, Rect eventsTab, Rect timelineTab) {}
}
