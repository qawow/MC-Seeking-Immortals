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
    private static final int PANEL_W = 420;
    private static final int PANEL_H = 260;
    private static final int ROW_H = 18;

    private int listScroll;
    private int selected = -1;
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
        selected = -1;
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
        // Cap list for UI responsiveness; unlocked first when showing all.
        if ("all".equals(filter) && filtered.size() > 400) {
            List<BeastBestiaryService.BeastEntry> unlocked = new ArrayList<>();
            List<BeastBestiaryService.BeastEntry> locked = new ArrayList<>();
            for (BeastBestiaryService.BeastEntry entry : filtered) {
                if (ClientLoreData.isBeastUnlocked(entry.id())) {
                    unlocked.add(entry);
                } else if (locked.size() < 300) {
                    locked.add(entry);
                }
            }
            unlocked.addAll(locked);
            filtered = unlocked;
        }
        view = List.copyOf(filtered);
        if (selected >= view.size()) {
            selected = view.isEmpty() ? -1 : 0;
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
                        int bg = index == selected ? ImmortalUiSkin.JOURNAL_ROW_SELECTED
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
        } else if (selected < 0 || selected >= view.size()) {
            lines.add(Component.translatable("screen.seeking_immortals.bestiary.pick").getString());
        } else {
            BeastBestiaryService.BeastEntry entry = view.get(selected);
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
            int visible = Math.max(1, layout.list().h() / ROW_H);
            int row = (int) ((mouseY - layout.list().y() - 2) / ROW_H);
            if (row >= 0 && row < visible) {
                int index = listScroll + row;
                if (index >= 0 && index < view.size()) {
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
            int visible = Math.max(1, layout.list().h() / ROW_H);
            listScroll = Mth.clamp(listScroll - (int) Math.round(delta), 0, Math.max(0, view.size() - visible));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    private static String nullToDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
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
        Rect filterAll = new Rect(left + 8, top + 24, 50, 14);
        Rect filterUnlocked = new Rect(left + 62, top + 24, 60, 14);
        Rect filterLocked = new Rect(left + 126, top + 24, 60, 14);
        return new Layout(left, top, PANEL_W, PANEL_H, list, detail, refresh, close, filterAll, filterUnlocked, filterLocked);
    }

    private record Rect(int x, int y, int w, int h) {
        boolean contains(double mx, double my) {
            return mx >= x && my >= y && mx < x + w && my < y + h;
        }
    }

    private record Layout(int left, int top, int width, int height, Rect list, Rect detail,
                          Rect refresh, Rect close, Rect filterAll, Rect filterUnlocked, Rect filterLocked) {}
}
