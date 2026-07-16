package com.xunxian.seekingimmortals.client;

import com.xunxian.seekingimmortals.lore.LoreCompendiumService;
import com.xunxian.seekingimmortals.lore.NameAliasGlossaryService;
import com.xunxian.seekingimmortals.lore.NumericOverviewService;
import com.xunxian.seekingimmortals.lore.VisualStyleService;
import com.xunxian.seekingimmortals.network.LoreScreenActionPacket;
import com.xunxian.seekingimmortals.network.ModNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

/** M16 encyclopedia hub: category summary, glossary and numeric/visual quick reference. */
public class LoreCompendiumScreen extends Screen {
    public enum Tab {
        HUB,
        GLOSSARY,
        NUMERIC,
        VISUAL
    }

    private static final int PANEL_W = 420;
    private static final int PANEL_H = 260;
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

    public void refreshFromSync() {
        rebuildLines();
    }

    @Override
    protected void init() {
        super.init();
        rebuildLines();
        Layout layout = calculateLayout(width, height);
        addRenderableWidget(ImmortalButton.secondary(layout.refresh().x(), layout.refresh().y(),
                layout.refresh().w(), layout.refresh().h(),
                Component.translatable("screen.seeking_immortals.lore.refresh"),
                b -> ModNetwork.CHANNEL.sendToServer(new LoreScreenActionPacket(tabAction()))));
        addRenderableWidget(ImmortalButton.secondary(layout.close().x(), layout.close().y(),
                layout.close().w(), layout.close().h(),
                Component.translatable("gui.done"), b -> onClose()));
        addRenderableWidget(tabButton(layout.hubTab(), Tab.HUB, "screen.seeking_immortals.compendium.tab_hub"));
        addRenderableWidget(tabButton(layout.glossaryTab(), Tab.GLOSSARY, "screen.seeking_immortals.compendium.tab_glossary"));
        addRenderableWidget(tabButton(layout.numericTab(), Tab.NUMERIC, "screen.seeking_immortals.compendium.tab_numeric"));
        addRenderableWidget(tabButton(layout.visualTab(), Tab.VISUAL, "screen.seeking_immortals.compendium.tab_visual"));
        addRenderableWidget(ImmortalButton.secondary(layout.bestiaryBtn().x(), layout.bestiaryBtn().y(),
                layout.bestiaryBtn().w(), layout.bestiaryBtn().h(),
                Component.translatable("screen.seeking_immortals.compendium.open_bestiary"),
                b -> ModNetwork.CHANNEL.sendToServer(new LoreScreenActionPacket("bestiary"))));
        addRenderableWidget(ImmortalButton.secondary(layout.chronicleBtn().x(), layout.chronicleBtn().y(),
                layout.chronicleBtn().w(), layout.chronicleBtn().h(),
                Component.translatable("screen.seeking_immortals.compendium.open_chronicle"),
                b -> ModNetwork.CHANNEL.sendToServer(new LoreScreenActionPacket("chronicle"))));
    }

    private ImmortalButton tabButton(Rect rect, Tab target, String key) {
        ImmortalButton button = tab == target
                ? ImmortalButton.primary(rect.x(), rect.y(), rect.w(), rect.h(), Component.translatable(key), b -> setTab(target))
                : ImmortalButton.secondary(rect.x(), rect.y(), rect.w(), rect.h(), Component.translatable(key), b -> setTab(target));
        return button;
    }

    private void setTab(Tab next) {
        if (tab != next) {
            tab = next;
            scroll = 0;
            rebuildLines();
            clearWidgets();
            init();
        }
    }

    private String tabAction() {
        return switch (tab) {
            case GLOSSARY -> "glossary";
            case NUMERIC -> "numeric";
            case VISUAL, HUB -> "compendium";
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
                out.add("bestiary " + ClientLoreData.bestiaryUnlockedCount());
                out.add("chronicle " + ClientLoreData.chronicleDiscoveredCount());
                out.add("timeline " + ClientLoreData.timelinePhaseCount());
            }
            case GLOSSARY -> {
                out.add(Component.translatable("screen.seeking_immortals.compendium.glossary_header",
                        NameAliasGlossaryService.size()).getString());
                for (String tip : NameAliasGlossaryService.builtin().searchTips()) {
                    out.add("* " + tip);
                }
                int n = 0;
                for (NameAliasGlossaryService.GlossaryEntry entry : NameAliasGlossaryService.all()) {
                    String aliases = entry.aliases().isEmpty() ? "-" : String.join("/", entry.aliases());
                    out.add(entry.primary() + " [" + entry.id() + "/" + entry.type() + "] " + aliases);
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
                    out.add("-- currency --");
                    out.addAll(snap.currencyLines());
                    out.add("-- breakthrough --");
                    out.addAll(snap.breakthroughLines());
                    out.add("-- threat --");
                    out.addAll(snap.threatLines());
                    out.add("-- snapshot --");
                    out.addAll(snap.summaryLines());
                }
            }
            case VISUAL -> {
                VisualStyleService.Snapshot snap = VisualStyleService.builtin();
                if (!snap.present()) {
                    out.add(Component.translatable("screen.seeking_immortals.compendium.missing_visual").getString());
                } else {
                    out.add(Component.translatable("screen.seeking_immortals.compendium.visual_header",
                            snap.styleGuideId()).getString());
                    if (!snap.description().isBlank()) {
                        out.add(snap.description());
                    }
                    out.add("-- palette --");
                    out.addAll(snap.paletteLines());
                    out.add("-- counts --");
                    out.addAll(snap.countLines());
                }
            }
        }
        lines = List.copyOf(out);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        Layout layout = calculateLayout(width, height);
        ImmortalUiSkin.drawLayeredPanel(graphics, layout.left(), layout.top(), layout.width(), layout.height());
        ImmortalUiSkin.drawTitleBar(graphics, layout.left() + 6, layout.top() + 6, layout.width() - 12, 16);
        ImmortalUiSkin.drawStringFit(font, graphics, title.getString(),
                layout.left() + 12, layout.top() + 10, layout.width() - 24, ImmortalUiSkin.JOURNAL_BORDER, false);
        ImmortalUiSkin.drawInnerFrame(graphics, layout.viewport().x(), layout.viewport().y(),
                layout.viewport().w(), layout.viewport().h());

        int contentHeight = Math.max(1, lines.size() * ROW_H);
        int visible = Math.max(1, layout.viewport().h());
        scroll = Mth.clamp(scroll, 0, Math.max(0, contentHeight - visible));
        ImmortalUiSkin.withScissor(graphics, layout.viewport().x() + 1, layout.viewport().y() + 1,
                Math.max(1, layout.viewport().w() - 2), Math.max(1, layout.viewport().h() - 2), () -> {
                    int y = layout.viewport().y() + 4 - scroll;
                    for (String line : lines) {
                        graphics.drawString(font, line == null ? "" : line,
                                layout.viewport().x() + 6, y, ImmortalUiSkin.JOURNAL_PAPER, false);
                        y += ROW_H;
                    }
                });
        ImmortalUiSkin.drawThinScrollbar(graphics, layout.viewport().x() + layout.viewport().w() - 3,
                layout.viewport().y() + 1, Math.max(1, layout.viewport().h() - 2),
                contentHeight, visible, scroll);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        Layout layout = calculateLayout(width, height);
        if (layout.viewport().contains(mouseX, mouseY)) {
            int contentHeight = Math.max(1, lines.size() * ROW_H);
            int visible = Math.max(1, layout.viewport().h());
            scroll = Mth.clamp(scroll - (int) Math.round(delta * ROW_H), 0, Math.max(0, contentHeight - visible));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    private static Layout calculateLayout(int width, int height) {
        int left = Math.max(4, (width - PANEL_W) / 2);
        int top = Math.max(4, (height - PANEL_H) / 2);
        Rect viewport = new Rect(left + 8, top + 44, PANEL_W - 16, PANEL_H - 84);
        int btnY = top + PANEL_H - 24;
        Rect refresh = new Rect(left + 8, btnY, 70, 16);
        Rect close = new Rect(left + PANEL_W - 78, btnY, 70, 16);
        Rect hubTab = new Rect(left + 8, top + 24, 50, 14);
        Rect glossaryTab = new Rect(left + 62, top + 24, 50, 14);
        Rect numericTab = new Rect(left + 116, top + 24, 50, 14);
        Rect visualTab = new Rect(left + 170, top + 24, 50, 14);
        Rect bestiaryBtn = new Rect(left + 230, top + 24, 70, 14);
        Rect chronicleBtn = new Rect(left + 304, top + 24, 70, 14);
        return new Layout(left, top, PANEL_W, PANEL_H, viewport, refresh, close,
                hubTab, glossaryTab, numericTab, visualTab, bestiaryBtn, chronicleBtn);
    }

    private record Rect(int x, int y, int w, int h) {
        boolean contains(double mx, double my) {
            return mx >= x && my >= y && mx < x + w && my < y + h;
        }
    }

    private record Layout(int left, int top, int width, int height, Rect viewport, Rect refresh, Rect close,
                          Rect hubTab, Rect glossaryTab, Rect numericTab, Rect visualTab,
                          Rect bestiaryBtn, Rect chronicleBtn) {}
}
