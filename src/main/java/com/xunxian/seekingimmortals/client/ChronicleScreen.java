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
    private static final int PANEL_W = 420;
    private static final int PANEL_H = 260;
    private static final int ROW_H = 18;

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
        Layout layout = calculateLayout(width, height);
        addRenderableWidget(ImmortalButton.secondary(layout.refresh().x(), layout.refresh().y(),
                layout.refresh().w(), layout.refresh().h(),
                Component.translatable("screen.seeking_immortals.lore.refresh"),
                b -> ModNetwork.CHANNEL.sendToServer(new LoreScreenActionPacket("chronicle"))));
        addRenderableWidget(ImmortalButton.secondary(layout.close().x(), layout.close().y(),
                layout.close().w(), layout.close().h(),
                Component.translatable("gui.done"), b -> onClose()));
        addRenderableWidget(ImmortalButton.primary(layout.eventsTab().x(), layout.eventsTab().y(),
                layout.eventsTab().w(), layout.eventsTab().h(),
                Component.translatable("screen.seeking_immortals.chronicle.tab_events"),
                b -> setTab(Tab.EVENTS)));
        addRenderableWidget(ImmortalButton.secondary(layout.timelineTab().x(), layout.timelineTab().y(),
                layout.timelineTab().w(), layout.timelineTab().h(),
                Component.translatable("screen.seeking_immortals.chronicle.tab_timeline"),
                b -> setTab(Tab.TIMELINE)));
    }

    private void setTab(Tab next) {
        if (tab != next) {
            tab = next;
            listScroll = 0;
            selected = -1;
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
                                font.split(Component.literal(line == null ? "" : line), layout.detail().w() - 12);
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

    private static Layout calculateLayout(int width, int height) {
        int left = Math.max(4, (width - PANEL_W) / 2);
        int top = Math.max(4, (height - PANEL_H) / 2);
        int listX = left + 8;
        int listY = top + 40;
        int listW = 180;
        int listH = PANEL_H - 72;
        int detailX = listX + listW + 8;
        int detailW = left + PANEL_W - 8 - detailX;
        Rect list = new Rect(listX, listY, listW, listH);
        Rect detail = new Rect(detailX, listY, detailW, listH);
        int btnY = top + PANEL_H - 24;
        Rect refresh = new Rect(left + 8, btnY, 70, 16);
        Rect close = new Rect(left + PANEL_W - 78, btnY, 70, 16);
        Rect eventsTab = new Rect(left + 8, top + 24, 70, 14);
        Rect timelineTab = new Rect(left + 82, top + 24, 70, 14);
        return new Layout(left, top, PANEL_W, PANEL_H, list, detail, refresh, close, eventsTab, timelineTab);
    }

    private record Rect(int x, int y, int w, int h) {
        boolean contains(double mx, double my) {
            return mx >= x && my >= y && mx < x + w && my < y + h;
        }
    }

    private record Layout(int left, int top, int width, int height, Rect list, Rect detail,
                          Rect refresh, Rect close, Rect eventsTab, Rect timelineTab) {}
}
