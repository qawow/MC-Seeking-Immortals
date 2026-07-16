package com.xunxian.seekingimmortals.client;

import com.xunxian.seekingimmortals.beast.BeastBestiaryService;
import com.xunxian.seekingimmortals.network.LoreScreenActionPacket;
import com.xunxian.seekingimmortals.network.ModNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** M16 read-only bestiary journal driven by unlock records. */
public class BestiaryScreen extends Screen {
    private static final int MAX_PANEL_W = 420;
    private static final int MAX_PANEL_H = 260;
    private static final int PANEL_MARGIN = 4;
    private static final int STACKED_BREAKPOINT = 280;
    private static final int ROW_H = 18;
    private static final int MIN_BODY_LINE = 10;

    private int listScroll;
    private String selectedId = "";
    private String filter = "all";
    private List<BeastBestiaryService.BeastEntry> view = List.of();

    public BestiaryScreen() {
        super(Component.translatable("screen.seeking_immortals.bestiary.title"));
    }

    public void refreshFromSync() {
        rebuildView();
    }

    @Override
    protected void init() {
        super.init();
        rebuildView();
        Layout layout = calculateLayout(width, height);
        addRenderableWidget(ImmortalButton.secondary(layout.refresh().x(), layout.refresh().y(),
                layout.refresh().w(), layout.refresh().h(),
                Component.translatable("screen.seeking_immortals.lore.refresh"),
                b -> ModNetwork.CHANNEL.sendToServer(new LoreScreenActionPacket("bestiary"))));
        addRenderableWidget(ImmortalButton.secondary(layout.close().x(), layout.close().y(),
                layout.close().w(), layout.close().h(),
                Component.translatable("gui.done"), b -> onClose()));
        addRenderableWidget(ImmortalButton.secondary(layout.filterAll().x(), layout.filterAll().y(),
                layout.filterAll().w(), layout.filterAll().h(),
                Component.translatable("screen.seeking_immortals.bestiary.filter_all"),
                b -> setFilter("all")));
        addRenderableWidget(ImmortalButton.secondary(layout.filterUnlocked().x(), layout.filterUnlocked().y(),
                layout.filterUnlocked().w(), layout.filterUnlocked().h(),
                Component.translatable("screen.seeking_immortals.bestiary.filter_unlocked"),
                b -> setFilter("unlocked")));
        addRenderableWidget(ImmortalButton.secondary(layout.filterLocked().x(), layout.filterLocked().y(),
                layout.filterLocked().w(), layout.filterLocked().h(),
                Component.translatable("screen.seeking_immortals.bestiary.filter_locked"),
                b -> setFilter("locked")));
    }

    private void setFilter(String next) {
        filter = next == null ? "all" : next;
        listScroll = 0;
        rebuildView();
    }

    private void rebuildView() {
        List<BeastBestiaryService.BeastEntry> all = new ArrayList<>(BeastBestiaryService.all().values());
        all.sort(Comparator
                .comparingInt(BeastBestiaryService.BeastEntry::tier)
                .thenComparing(BeastBestiaryService.BeastEntry::display, String.CASE_INSENSITIVE_ORDER));
        List<BeastBestiaryService.BeastEntry> filtered = new ArrayList<>();
        for (BeastBestiaryService.BeastEntry entry : all) {
            boolean unlocked = ClientLoreData.isBeastUnlocked(entry.id());
            if ("unlocked".equals(filter) && !unlocked) {
                continue;
            }
            if ("locked".equals(filter) && unlocked) {
                continue;
            }
            filtered.add(entry);
        }
        view = List.copyOf(filtered);
        if (findSelectedIndex(view, selectedId) < 0) {
            selectedId = "";
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        Layout layout = calculateLayout(width, height);
        ImmortalUiSkin.drawLayeredPanel(graphics, layout.left(), layout.top(), layout.width(), layout.height());
        ImmortalUiSkin.drawTitleBar(graphics, layout.left() + 6, layout.top() + 6, layout.width() - 12, 16);
        ImmortalUiSkin.drawStringFit(font, graphics, title.getString()
                        + "  " + ClientLoreData.bestiaryUnlockedCount() + "/" + BeastBestiaryService.size(),
                layout.left() + 12, layout.top() + 10, layout.width() - 24, ImmortalUiSkin.JOURNAL_BORDER, false);

        ImmortalUiSkin.drawInnerFrame(graphics, layout.list().x(), layout.list().y(), layout.list().w(), layout.list().h());
        ImmortalUiSkin.drawInnerFrame(graphics, layout.detail().x(), layout.detail().y(), layout.detail().w(), layout.detail().h());

        int visible = Math.max(1, layout.list().h() / ROW_H);
        listScroll = Mth.clamp(listScroll, 0, Math.max(0, view.size() - visible));
        ImmortalUiSkin.withScissor(graphics, layout.list().x() + 1, layout.list().y() + 1,
                Math.max(1, layout.list().w() - 2), Math.max(1, layout.list().h() - 2), () -> {
                    for (int i = 0; i < visible && listScroll + i < view.size(); i++) {
                        int index = listScroll + i;
                        BeastBestiaryService.BeastEntry entry = view.get(index);
                        boolean unlocked = ClientLoreData.isBeastUnlocked(entry.id());
                        int y = layout.list().y() + 2 + i * ROW_H;
                        int bg = entry.id().equals(selectedId) ? ImmortalUiSkin.JOURNAL_ROW_SELECTED
                                : (i % 2 == 0 ? ImmortalUiSkin.JOURNAL_ROW : 0x00000000);
                        if (bg != 0) {
                            graphics.fill(layout.list().x() + 2, y, layout.list().x() + layout.list().w() - 2, y + ROW_H - 1, bg);
                        }
                        String label = unlocked
                                ? ("T" + entry.tier() + " " + entry.display())
                                : Component.translatable("screen.seeking_immortals.bestiary.locked").getString();
                        int color = unlocked ? ImmortalUiSkin.JOURNAL_PAPER : ImmortalUiSkin.JOURNAL_PAPER_MUTED;
                        ImmortalUiSkin.drawStringFit(font, graphics, label,
                                layout.list().x() + 6, y + 4, layout.list().w() - 12, color, false);
                    }
                });
        ImmortalUiSkin.drawThinScrollbar(graphics, layout.list().x() + layout.list().w() - 3, layout.list().y() + 1,
                Math.max(1, layout.list().h() - 2), view.size() * ROW_H, layout.list().h(), listScroll * ROW_H);

        renderDetail(graphics, layout);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderDetail(GuiGraphics graphics, Layout layout) {
        List<String> lines = new ArrayList<>();
        if (!ClientLoreData.isSynced()) {
            lines.add(Component.translatable("screen.seeking_immortals.lore.not_synced").getString());
        } else if (findSelectedIndex(view, selectedId) < 0) {
            lines.add(Component.translatable("screen.seeking_immortals.bestiary.pick").getString());
        } else {
            BeastBestiaryService.BeastEntry entry = view.get(findSelectedIndex(view, selectedId));
            boolean unlocked = ClientLoreData.isBeastUnlocked(entry.id());
            if (!unlocked) {
                lines.add(Component.translatable("screen.seeking_immortals.bestiary.locked_detail").getString());
            } else {
                lines.add(entry.display() + " [" + entry.id() + "]");
                lines.add("tier=" + entry.tier() + (entry.tierMax() > entry.tier() ? "-" + entry.tierMax() : "")
                        + " threat=" + entry.threat());
                lines.add("element=" + nullToDash(entry.element()) + " habitat=" + nullToDash(entry.habitat()));
                lines.add("category=" + nullToDash(entry.category()));
                lines.add("tameable=" + entry.tameable() + " true_spirit=" + entry.trueSpirit()
                        + " companion_only=" + entry.companionOnly());
                if (entry.regions() != null && !entry.regions().isEmpty()) {
                    lines.add("regions=" + String.join(",", entry.regions()));
                }
                if (entry.drops() != null && !entry.drops().isEmpty()) {
                    lines.add("drops=" + String.join(",", entry.drops()));
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
            int visible = Math.max(1, layout.list().h() / ROW_H);
            int row = (int) ((mouseY - layout.list().y() - 2) / ROW_H);
            if (row >= 0 && row < visible) {
                int index = listScroll + row;
                if (index >= 0 && index < view.size()) {
                    selectedId = view.get(index).id();
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
            int visible = Math.max(1, layout.list().h() / ROW_H);
            listScroll = Mth.clamp(listScroll - (int) Math.round(delta), 0, Math.max(0, view.size() - visible));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    private static String nullToDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    static int findSelectedIndex(List<BeastBestiaryService.BeastEntry> entries, String id) {
        if (entries == null || id == null || id.isBlank()) {
            return -1;
        }
        for (int i = 0; i < entries.size(); i++) {
            if (id.equals(entries.get(i).id())) {
                return i;
            }
        }
        return -1;
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
        int filterHeight = Math.max(1, Math.min(14, panelHeight / 8));
        int footerHeight = Math.max(1, Math.min(16, panelHeight / 7));
        int chrome = titleReserve + filterHeight + footerHeight + 6;
        if (chrome + minContent > panelHeight) {
            int deficit = chrome + minContent - panelHeight;
            int cutFilter = Math.min(Math.max(0, filterHeight - 1), deficit);
            filterHeight -= cutFilter;
            deficit -= cutFilter;
            int cutFooter = Math.min(Math.max(0, footerHeight - 1), deficit);
            footerHeight -= cutFooter;
            deficit -= cutFooter;
            titleReserve -= Math.min(Math.max(0, titleReserve - 8), deficit);
        }
        int filterY = top + titleReserve;
        int footerY = Math.min(top + panelHeight - footerHeight,
                Math.max(filterY + filterHeight + 2 + minContent, top + panelHeight - footerHeight - Math.min(4, panelHeight / 12)));
        int contentY = filterY + filterHeight + 2;
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

        int filterGap = Math.min(2, Math.max(0, (innerWidth - 3) / 2));
        int filterSpace = Math.max(3, innerWidth - filterGap * 2);
        int firstWidth = Math.max(1, filterSpace / 3);
        int secondWidth = Math.max(1, (filterSpace - firstWidth) / 2);
        int thirdWidth = Math.max(1, filterSpace - firstWidth - secondWidth);
        Rect filterAll = new Rect(innerX, filterY, firstWidth, filterHeight);
        Rect filterUnlocked = new Rect(filterAll.x() + filterAll.w() + filterGap,
                filterY, secondWidth, filterHeight);
        Rect filterLocked = new Rect(filterUnlocked.x() + filterUnlocked.w() + filterGap,
                filterY, thirdWidth, filterHeight);
        return new Layout(left, top, panelWidth, panelHeight, stacked, list, detail,
                refresh, close, filterAll, filterUnlocked, filterLocked);
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
                          Rect refresh, Rect close, Rect filterAll, Rect filterUnlocked, Rect filterLocked) {}
}
