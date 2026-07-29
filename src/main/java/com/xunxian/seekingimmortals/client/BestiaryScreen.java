package com.xunxian.seekingimmortals.client;

import com.xunxian.seekingimmortals.beast.BeastBestiaryService;
import com.xunxian.seekingimmortals.util.PlayerDisplayText;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** M16 read-only bestiary journal driven by unlock records. */
public class BestiaryScreen extends AbstractLoreScreen {
    private static final int ROW_H = 18;

    private int listScroll;
    private String selectedId = "";
    private String filter = "all";
    private List<BeastBestiaryService.BeastEntry> view = List.of();

    public BestiaryScreen() {
        super(Component.translatable("screen.seeking_immortals.bestiary.title"));
    }

    @Override
    public void refreshFromSync() {
        rebuildView();
    }

    @Override
    protected String loreTitleProgress() {
        return "  " + ClientLoreData.bestiaryUnlockedCount() + "/" + BeastBestiaryService.size();
    }

    @Override
    protected PanelBounds lorePanelBounds() {
        Layout layout = calculateLayout(width, height);
        return new PanelBounds(layout.left(), layout.top(), layout.width(), layout.height());
    }

    @Override
    protected void init() {
        super.init();
        rebuildView();
        rebuildActionWidgets();
    }

    private void rebuildActionWidgets() {
        clearWidgets();
        Layout layout = calculateLayout(width, height);
        addRefreshAndClose(layout.refresh().x(), layout.refresh().y(), layout.refresh().w(), layout.refresh().h(),
                layout.close().x(), layout.close().y(), layout.close().w(), layout.close().h(),
                "bestiary");
        // Filters use TabBar (ImmortalButton primary/secondary) so the selected filter matches the majority tab style.
        TabBar<String> filters = new TabBar<>(filter);
        filters.addTab("all", Component.translatable("screen.seeking_immortals.bestiary.filter_all"),
                        toUiRect(layout.filterAll().x(), layout.filterAll().y(),
                                layout.filterAll().w(), layout.filterAll().h()))
                .addTab("unlocked", Component.translatable("screen.seeking_immortals.bestiary.filter_unlocked"),
                        toUiRect(layout.filterUnlocked().x(), layout.filterUnlocked().y(),
                                layout.filterUnlocked().w(), layout.filterUnlocked().h()))
                .addTab("locked", Component.translatable("screen.seeking_immortals.bestiary.filter_locked"),
                        toUiRect(layout.filterLocked().x(), layout.filterLocked().y(),
                                layout.filterLocked().w(), layout.filterLocked().h()))
                .setOnSelect(this::setFilter);
        for (ImmortalButton button : filters.attach(null)) {
            addRenderableWidget(button);
        }
    }

    private void setFilter(String next) {
        String safe = next == null ? "all" : next;
        if (safe.equals(filter)) {
            return;
        }
        filter = safe;
        listScroll = 0;
        rebuildView();
        rebuildActionWidgets();
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
    protected void renderJournalContent(GuiGraphics graphics, JournalChrome chrome,
                                        int mouseX, int mouseY, float partialTick) {
        Layout layout = calculateLayout(width, height);
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
                        ImmortalUiSkin.InteractionState rowState = entry.id().equals(selectedId)
                                ? ImmortalUiSkin.InteractionState.SELECTED
                                : ImmortalUiSkin.InteractionState.NORMAL;
                        ImmortalUiSkin.drawListRow(graphics, layout.list().x() + 2, y,
                                Math.max(1, layout.list().w() - 4), ROW_H - 1, rowState);
                        String label = unlocked
                                ? Component.translatable("screen.seeking_immortals.bestiary.list_entry",
                                        entry.tier(), beastDisplay(entry)).getString()
                                : Component.translatable("screen.seeking_immortals.bestiary.locked").getString();
                        int color = unlocked ? ImmortalUiSkin.JOURNAL_PAPER : ImmortalUiSkin.JOURNAL_PAPER_MUTED;
                        ImmortalUiSkin.drawStringFit(font, graphics, label,
                                layout.list().x() + 6, y + 4, layout.list().w() - 12, color, false);
                    }
                });
        ImmortalUiSkin.drawThinScrollbar(graphics, layout.list().x() + layout.list().w() - 3, layout.list().y() + 1,
                Math.max(1, layout.list().h() - 2), view.size() * ROW_H, layout.list().h(), listScroll * ROW_H);

        renderDetail(graphics, layout);
    }

    private void renderDetail(GuiGraphics graphics, Layout layout) {
        List<String> lines = detailLines();
        setDetailSelectionKey("bestiary:" + filter + ":" + selectedId);
        renderWrappedDetail(graphics, layout.detail().x(), layout.detail().y(),
                layout.detail().w(), layout.detail().h(), lines);
    }

    private List<String> detailLines() {
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
                lines.add(beastDisplay(entry).getString());
                String tier = entry.tierMax() > entry.tier()
                        ? entry.tier() + "-" + entry.tierMax() : Integer.toString(entry.tier());
                lines.add(Component.translatable("screen.seeking_immortals.bestiary.tier_threat",
                        tier, entry.threat()).getString());
                lines.add(Component.translatable("screen.seeking_immortals.bestiary.element_category",
                        codeDisplay("element", entry.element()),
                        codeDisplay("category", entry.category())).getString());
                lines.add(Component.translatable("screen.seeking_immortals.bestiary.traits",
                        yesNo(entry.tameable()), yesNo(entry.trueSpirit()), yesNo(entry.companionOnly())).getString());
                lines.add(Component.translatable("screen.seeking_immortals.bestiary.catalog_counts",
                        entry.regions() == null ? 0 : entry.regions().size(),
                        entry.drops() == null ? 0 : entry.drops().size()).getString());
            }
        }
        return List.copyOf(lines);
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
        if (scrollLoreDetail(mouseX, mouseY,
                new UiRect(layout.detail().x(), layout.detail().y(), layout.detail().w(), layout.detail().h()),
                detailLines(), delta)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    private static Component codeDisplay(String kind, String code) {
        String normalized = PlayerDisplayText.normalizeId(code);
        String key = "screen.seeking_immortals.bestiary." + kind + "." + normalized;
        return PlayerDisplayText.translatedOr(key,
                "screen.seeking_immortals.bestiary." + kind + ".unknown");
    }

    private static Component beastDisplay(BeastBestiaryService.BeastEntry entry) {
        return PlayerDisplayText.safeLiteral(entry == null ? "" : entry.display(),
                "text.seeking_immortals.unknown_beast");
    }

    private static Component yesNo(boolean value) {
        return Component.translatable(value
                ? "screen.seeking_immortals.common.yes" : "screen.seeking_immortals.common.no");
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
        ListDetailChrome chrome = computeListDetailChrome(width, height);
        int filterGap = Math.min(2, Math.max(0, (chrome.innerWidth() - 3) / 2));
        int filterSpace = Math.max(3, chrome.innerWidth() - filterGap * 2);
        int firstWidth = Math.max(1, filterSpace / 3);
        int secondWidth = Math.max(1, (filterSpace - firstWidth) / 2);
        int thirdWidth = Math.max(1, filterSpace - firstWidth - secondWidth);
        Rect filterAll = new Rect(chrome.innerX(), chrome.stripY(), firstWidth, chrome.stripHeight());
        Rect filterUnlocked = new Rect(filterAll.x() + filterAll.w() + filterGap,
                chrome.stripY(), secondWidth, chrome.stripHeight());
        Rect filterLocked = new Rect(filterUnlocked.x() + filterUnlocked.w() + filterGap,
                chrome.stripY(), thirdWidth, chrome.stripHeight());
        return new Layout(chrome.left(), chrome.top(), chrome.panelWidth(), chrome.panelHeight(), chrome.stacked(),
                new Rect(chrome.listX(), chrome.listY(), chrome.listW(), chrome.listH()),
                new Rect(chrome.detailX(), chrome.detailY(), chrome.detailW(), chrome.detailH()),
                new Rect(chrome.refreshX(), chrome.refreshY(), chrome.refreshW(), chrome.refreshH()),
                new Rect(chrome.closeX(), chrome.closeY(), chrome.closeW(), chrome.closeH()),
                filterAll, filterUnlocked, filterLocked);
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
