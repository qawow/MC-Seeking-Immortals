package com.xunxian.seekingimmortals.client;

import com.xunxian.seekingimmortals.catalog.ManualCatalogService;
import com.xunxian.seekingimmortals.catalog.TextMaterialCatalogService;
import com.xunxian.seekingimmortals.network.MethodActionPacket;
import com.xunxian.seekingimmortals.network.MethodLayoutActionPacket;
import com.xunxian.seekingimmortals.network.ModNetwork;
import com.xunxian.seekingimmortals.skill.MethodLayerTechniqueService;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Interactive cultivation method tree and refinement UI.
 * Wave481: cultivate button for catalog-defined method layers.
 * Wave483: node-link layer chain + school adjacency graph in the detail pane.
 * Wave484: freer multi-node school graph with clickable peers (up to 6).
 * Wave485: freeform drag layout for school-graph nodes (client offsets).
 * Wave486: layout offsets server-persisted via MethodLayoutService / protocol 17.
 * Reuses TextMaterialCatalogService method index + ClientMethodData learned mirror.
 * Refinement goes through MethodActionPacket -> ManualCatalogService (server authority).
 * Unlearned methods are intentionally read-only here and must be acquired from a manual or scroll.
 *
 * <p>Chrome shell uses {@link AbstractJournalScreen}; graph drag / dual-scroll contracts stay local.</p>
 */
public class MethodTreeScreen extends AbstractJournalScreen {
    private static final int MAX_PANEL_W = 520;
    private static final int MAX_PANEL_H = 320;
    private static final int PANEL_MARGIN = 4;
    private static final int WIDE_LAYOUT_WIDTH = 380;
    private static final int LINE = 12;
    private static final int NODE = 12;
    private static final int SCROLL_DRAG_THRESHOLD = 4;
    // Node/link colors read live from ImmortalUiSkin so JADE_SLIP climate rebinds apply.
    private static final int GRAPH_COLS = 3;
    private static final int GRAPH_MAX = 6;

    private final Screen parent;
    private List<TextMaterialCatalogService.MethodEntry> allMethods = List.of();
    private List<TextMaterialCatalogService.MethodEntry> filtered = List.of();
    private List<String> schoolTabs = List.of("all");
    private String activeSchool = "all";
    private int scroll;
    private int detailScroll;
    private int renderedDetailHeight;
    private int selectedIndex = -1;
    private ImmortalButton cultivateButton;
    private ImmortalButton prevSchoolButton;
    private ImmortalButton nextSchoolButton;
    private ImmortalButton resetLayoutButton;
    /** Wave484/485/486: interactive school-graph nodes (screen coords) + method ids. */
    private final List<GraphHit> graphHits = new ArrayList<>();
    /** Wave485/486: freeform layout offsets (server-synced via ClientMethodLayoutData). */
    private final Map<String, int[]> layoutOffsets = new HashMap<>();
    private String draggingMethodId = "";
    private double dragGrabX;
    private double dragGrabY;
    private int graphOriginX;
    private int graphOriginY;
    private int graphWidth;
    private int graphHeight;
    private int graphColumns = GRAPH_COLS;
    private int graphGapX = 6;
    private int graphGapY = 4;
    private int graphNodeWidth = 42;
    private int graphNodeHeight = 16;
    private ScrollDragTarget scrollDragTarget = ScrollDragTarget.NONE;
    private int pendingListIndex = -1;
    private double scrollDragStartY;
    private int scrollAtDragStart;

    private enum ScrollDragTarget {
        NONE,
        LIST_PENDING,
        LIST,
        DETAIL
    }

    private record GraphHit(int x, int y, int w, int h, String methodId) {
        boolean contains(double mx, double my) {
            return mx >= x && mx <= x + w && my >= y && my <= y + h;
        }
    }


    @Override
    protected UiClimate defaultClimate() {
        return UiClimate.JADE_SLIP;
    }

    public MethodTreeScreen() {
        this(null);
    }

    public MethodTreeScreen(Screen parent) {
        super(Component.translatable("screen.seeking_immortals.method_tree.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        resetPointerDrag();
        reloadCatalog();
        Layout layout = calculateLayout(width, height);

        addRenderableWidget(ImmortalButton.secondary(layout.doneButton().x(), layout.doneButton().y(),
                layout.doneButton().width(), layout.doneButton().height(),
                Component.translatable("gui.done"), b -> onClose()));

        cultivateButton = ImmortalButton.primary(layout.cultivateButton().x(), layout.cultivateButton().y(),
                layout.cultivateButton().width(), layout.cultivateButton().height(),
                Component.translatable("screen.seeking_immortals.method_tree.cultivate"), b -> {
            TextMaterialCatalogService.MethodEntry selected = selectedMethod();
            if (selected != null) {
                int currentLayer = ClientMethodData.getLayer(selected.id());
                if (currentLayer > 0) {
                    // 仅精进已学功法
                    ModNetwork.CHANNEL.sendToServer(new MethodActionPacket("cultivate:" + selected.id()));
                }
            }
        });
        addRenderableWidget(cultivateButton);

        prevSchoolButton = ImmortalButton.secondary(layout.prevSchoolButton().x(), layout.prevSchoolButton().y(),
                layout.prevSchoolButton().width(), layout.prevSchoolButton().height(),
                Component.literal("<"), b -> cycleSchool(-1));
        nextSchoolButton = ImmortalButton.secondary(layout.nextSchoolButton().x(), layout.nextSchoolButton().y(),
                layout.nextSchoolButton().width(), layout.nextSchoolButton().height(),
                Component.literal(">"), b -> cycleSchool(1));
        addRenderableWidget(prevSchoolButton);
        addRenderableWidget(nextSchoolButton);

        resetLayoutButton = ImmortalButton.secondary(layout.resetLayoutButton().x(), layout.resetLayoutButton().y(),
                layout.resetLayoutButton().width(), layout.resetLayoutButton().height(),
                Component.translatable("screen.seeking_immortals.method_tree.reset_layout"), b -> resetLayout());
        addRenderableWidget(resetLayoutButton);

        hydrateLayoutFromClientMirror();
        updateCultivateButton();
    }

    private void resetLayout() {
        layoutOffsets.clear();
        ClientMethodLayoutData.set(Map.of());
        resetPointerDrag();
        ModNetwork.CHANNEL.sendToServer(new MethodLayoutActionPacket("clear"));
        updateCultivateButton();
    }

    private void hydrateLayoutFromClientMirror() {
        layoutOffsets.clear();
        Map<String, int[]> synced = ClientMethodLayoutData.getOffsets();
        if (synced != null) {
            synced.forEach((id, xy) -> {
                if (id != null && xy != null && xy.length >= 2) {
                    layoutOffsets.put(id, new int[]{xy[0], xy[1]});
                }
            });
        }
    }

    private void reloadCatalog() {
        List<TextMaterialCatalogService.MethodEntry> list = new ArrayList<>(
                TextMaterialCatalogService.builtin().methods().values());
        list.sort((a, b) -> {
            String sa = a.school() == null ? "" : a.school();
            String sb = b.school() == null ? "" : b.school();
            int c = sa.compareToIgnoreCase(sb);
            if (c != 0) {
                return c;
            }
            String da = CultivationDisplayTexts.methodName(a);
            String db = CultivationDisplayTexts.methodName(b);
            return da.compareToIgnoreCase(db);
        });
        allMethods = List.copyOf(list);

        Set<String> schools = new LinkedHashSet<>();
        schools.add("all");
        for (TextMaterialCatalogService.MethodEntry method : allMethods) {
            String school = method.school() == null ? "" : method.school().trim().toLowerCase(Locale.ROOT);
            if (!school.isBlank()) {
                schools.add(school);
            }
        }
        // Ungrouped bucket when blank schools exist.
        boolean hasBlank = allMethods.stream().anyMatch(m -> m.school() == null || m.school().isBlank());
        if (hasBlank) {
            schools.add("misc");
        }
        schoolTabs = List.copyOf(schools);
        if (!schoolTabs.contains(activeSchool)) {
            activeSchool = "all";
        }
        applyFilter();
    }

    private void applyFilter() {
        List<TextMaterialCatalogService.MethodEntry> next = new ArrayList<>();
        for (TextMaterialCatalogService.MethodEntry method : allMethods) {
            String school = method.school() == null ? "" : method.school().trim().toLowerCase(Locale.ROOT);
            if ("all".equals(activeSchool)
                    || ("misc".equals(activeSchool) && school.isBlank())
                    || activeSchool.equals(school)) {
                next.add(method);
            }
        }
        filtered = List.copyOf(next);
        scroll = Mth.clamp(scroll, 0, maxScroll());
        if (selectedIndex >= filtered.size()) {
            selectedIndex = filtered.isEmpty() ? -1 : 0;
        }
        if (selectedIndex < 0 && !filtered.isEmpty()) {
            selectedIndex = 0;
        }
        updateCultivateButton();
    }

    private void cycleSchool(int delta) {
        if (schoolTabs.isEmpty()) {
            return;
        }
        int idx = schoolTabs.indexOf(activeSchool);
        if (idx < 0) {
            idx = 0;
        }
        idx = Math.floorMod(idx + delta, schoolTabs.size());
        activeSchool = schoolTabs.get(idx);
        scroll = 0;
        selectedIndex = filtered.isEmpty() ? -1 : 0;
        applyFilter();
    }

    private int maxScroll() {
        return Math.max(0, filtered.size() - visibleListRows(calculateLayout(width, height)));
    }

    private TextMaterialCatalogService.MethodEntry selectedMethod() {
        if (selectedIndex < 0 || selectedIndex >= filtered.size()) {
            return null;
        }
        return filtered.get(selectedIndex);
    }

    private void updateCultivateButton() {
        TextMaterialCatalogService.MethodEntry selected = selectedMethod();
        boolean hasSelection = selected != null;
        if (cultivateButton != null) {
            if (!hasSelection) {
                cultivateButton.active = false;
                cultivateButton.setMessage(Component.translatable("screen.seeking_immortals.method_tree.cultivate"));
            } else {
                boolean learned = ClientMethodData.hasLearned(selected.id());
                int layer = ClientMethodData.getLayer(selected.id());
                int maxLayer = ManualCatalogService.maxMethodLayer(selected.id());

                if (!learned || layer == 0) {
                    // 未修习状态 - 禁用按钮，提示需要使用卷轴学习
                    cultivateButton.active = false;
                    cultivateButton.setMessage(Component.translatable("screen.seeking_immortals.method_tree.not_learned"));
                } else if (layer >= maxLayer) {
                    // 已满层
                    cultivateButton.active = false;
                    cultivateButton.setMessage(Component.translatable("screen.seeking_immortals.method_tree.max_layer"));
                } else {
                    // 可精进
                    cultivateButton.active = true;
                    cultivateButton.setMessage(Component.translatable("screen.seeking_immortals.method_tree.cultivate"));
                }
            }
        }
        if (resetLayoutButton != null) {
            resetLayoutButton.active = !layoutOffsets.isEmpty();
        }
    }

    static int calculatePanelWidth(int screenWidth) {
        return Math.min(MAX_PANEL_W, Math.max(1, screenWidth - PANEL_MARGIN * 2));
    }

    static int calculatePanelHeight(int screenHeight) {
        return Math.min(MAX_PANEL_H, Math.max(1, screenHeight - PANEL_MARGIN * 2));
    }

    static Layout calculateLayout(int screenWidth, int screenHeight) {
        int panelWidth = calculatePanelWidth(screenWidth);
        int panelHeight = calculatePanelHeight(screenHeight);
        int left = Math.max(0, (screenWidth - panelWidth) / 2);
        int top = Math.max(0, (screenHeight - panelHeight) / 2);
        int padding = panelWidth >= 220 ? 10 : 5;
        int innerX = left + padding;
        int innerWidth = Math.max(1, panelWidth - padding * 2);
        int headerHeight = panelHeight >= 220 ? 40 : panelHeight >= 140 ? 30 : 20;
        int buttonHeight = panelHeight >= 140 ? 18 : panelHeight >= 100 ? 16 : 14;
        int footerInset = panelHeight >= 140 ? 7 : 4;
        int buttonGap = innerWidth >= 56 ? 3 : innerWidth >= 28 ? 1 : 0;
        boolean stackedActions = innerWidth < 180;
        int actionRowGap = stackedActions ? 2 : 0;
        int footerHeight = buttonHeight * (stackedActions ? 2 : 1) + actionRowGap;
        int footerY = top + panelHeight - footerHeight - footerInset;
        int contentTop = top + headerHeight + 4;
        int contentBottom = Math.max(contentTop + 1, footerY - 5);
        int contentHeight = Math.max(1, contentBottom - contentTop);
        boolean wide = panelWidth >= WIDE_LAYOUT_WIDTH && contentHeight >= 96;

        Rect header = new Rect(innerX, top + 4, innerWidth, Math.max(12, headerHeight - 4));
        Rect list;
        Rect detail;
        if (wide) {
            int gap = 8;
            int listWidth = Math.min(210, Math.max(120, innerWidth * 2 / 5));
            list = new Rect(innerX, contentTop, listWidth, contentHeight);
            detail = new Rect(list.right() + gap, contentTop,
                    Math.max(1, innerX + innerWidth - list.right() - gap), contentHeight);
        } else {
            int gap = contentHeight >= 20 ? 3 : 1;
            int listHeight = Math.max(Math.min(LINE, contentHeight),
                    Math.min(72, Math.max(1, (contentHeight - gap) / 3)));
            listHeight = Math.min(listHeight, Math.max(1, contentHeight - gap - 1));
            list = new Rect(innerX, contentTop, innerWidth, listHeight);
            detail = new Rect(innerX, list.bottom() + gap, innerWidth,
                    Math.max(1, contentBottom - list.bottom() - gap));
        }

        Rect cultivate;
        Rect previous;
        Rect next;
        Rect reset;
        Rect done;
        if (stackedActions) {
            int topWidth = Math.max(1, (innerWidth - buttonGap) / 2);
            cultivate = new Rect(innerX, footerY, topWidth, buttonHeight);
            reset = new Rect(innerX + topWidth + buttonGap, footerY,
                    Math.max(1, innerWidth - topWidth - buttonGap), buttonHeight);
            int secondY = footerY + buttonHeight + actionRowGap;
            int navWidth = Math.max(1, Math.min(22, (innerWidth - buttonGap * 2) / 4));
            previous = new Rect(innerX, secondY, navWidth, buttonHeight);
            next = new Rect(previous.right() + buttonGap, secondY, navWidth, buttonHeight);
            done = new Rect(next.right() + buttonGap, secondY,
                    Math.max(1, innerX + innerWidth - next.right() - buttonGap), buttonHeight);
        } else {
            int buttonWidth = Math.max(1, (innerWidth - buttonGap * 4) / 5);
            int totalButtonsWidth = buttonWidth * 5 + buttonGap * 4;
            int buttonX = innerX + Math.max(0, (innerWidth - totalButtonsWidth) / 2);
            Rect[] actions = new Rect[5];
            for (int i = 0; i < actions.length; i++) {
                actions[i] = new Rect(buttonX + i * (buttonWidth + buttonGap), footerY,
                        buttonWidth, buttonHeight);
            }
            cultivate = actions[0];
            previous = actions[1];
            next = actions[2];
            reset = actions[3];
            done = actions[4];
        }
        return new Layout(left, top, panelWidth, panelHeight, wide, header, list, detail,
                cultivate, previous, next, reset, done);
    }

    static int visibleListRows(Layout layout) {
        return Math.max(1, Math.max(1, layout.list().height() - 4) / LINE);
    }

    private int maxDetailScroll(Layout layout) {
        return maxDetailScroll(renderedDetailHeight, detailViewportHeight(layout));
    }

    static int detailViewportHeight(Layout layout) {
        return Math.max(1, layout.detail().height() - 6);
    }

    /** Package-visible for dual-scroll tests: detail pane max offset in pixels. */
    static int maxDetailScroll(int contentHeight, int viewportHeight) {
        return Math.max(0, contentHeight - Math.max(1, viewportHeight));
    }

    /** Package-visible for dual-scroll tests: list pane max first-row offset. */
    static int maxListScroll(int itemCount, int visibleRows) {
        return Math.max(0, itemCount - Math.max(1, visibleRows));
    }

    static int scrollListBy(int current, int direction, int itemCount, int visibleRows) {
        int max = maxListScroll(itemCount, visibleRows);
        return Mth.clamp(current - direction, 0, max);
    }

    static int scrollDetailBy(int current, int direction, int contentHeight, int viewportHeight, int step) {
        int max = maxDetailScroll(contentHeight, viewportHeight);
        return Mth.clamp(current - direction * Math.max(1, step), 0, max);
    }

    static int dragListScroll(int startScroll, double startY, double currentY,
                              int itemCount, int visibleRows) {
        int rowDelta = (int)Math.round((startY - currentY) / LINE);
        return Mth.clamp(startScroll + rowDelta, 0, maxListScroll(itemCount, visibleRows));
    }

    static int dragDetailScroll(int startScroll, double startY, double currentY,
                                int contentHeight, int viewportHeight) {
        int pixelDelta = (int)Math.round(startY - currentY);
        return Mth.clamp(startScroll + pixelDelta, 0, maxDetailScroll(contentHeight, viewportHeight));
    }

    static boolean crossedScrollDragThreshold(double startY, double currentY) {
        return Math.abs(currentY - startY) >= SCROLL_DRAG_THRESHOLD;
    }

    /**
     * Package-visible for graph-drag tests: clamp absolute node top-left inside the graph bounds.
     */
    static int[] clampGraphNodePosition(int targetX, int targetY,
                                        int graphOriginX, int graphOriginY,
                                        int graphWidth, int graphHeight,
                                        int nodeW, int nodeH) {
        int x = Mth.clamp(targetX, graphOriginX, graphOriginX + Math.max(0, graphWidth - nodeW));
        int y = Mth.clamp(targetY, graphOriginY, graphOriginY + Math.max(0, graphHeight - nodeH));
        return new int[]{x, y};
    }

    /**
     * Package-visible for graph-drag tests: convert absolute node position back into
     * freeform offset from the default grid slot (col/row).
     */
    static int[] offsetFromGrid(int absoluteX, int absoluteY,
                                int graphOriginX, int graphOriginY,
                                int indexInWindow, int columns,
                                int nodeW, int nodeH, int gapX, int gapY) {
        int col = Math.max(0, indexInWindow) % Math.max(1, columns);
        int row = Math.max(0, indexInWindow) / Math.max(1, columns);
        int baseX = graphOriginX + col * (nodeW + gapX);
        int baseY = graphOriginY + row * (nodeH + gapY);
        return new int[]{absoluteX - baseX, absoluteY - baseY};
    }

    /** Hit-test used by click/drag start; package-visible for tests. */
    static boolean graphHitContains(int x, int y, int w, int h, double mx, double my) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    @Override
    protected JournalChrome journalChrome() {
        Layout layout = calculateLayout(width, height);
        // Dual list/detail frames are painted in content; skip single shared inner frame.
        return new JournalChrome(layout.left(), layout.top(), layout.panelWidth(), layout.panelHeight(),
                toUi(layout.header()), null);
    }

    @Override
    protected void renderJournalTitle(GuiGraphics graphics, JournalChrome chrome, UiRect header) {
        Layout layout = calculateLayout(width, height);
        graphics.drawCenteredString(font, title, layout.header().x() + layout.header().width() / 2,
                layout.header().y() + 4, ImmortalUiSkin.JOURNAL_PAPER);
        int learnedCount = ClientMethodData.getLearnedMethodCount();
        String headerText = Component.translatable("screen.seeking_immortals.method_tree.header",
                filtered.size(), allMethods.size(), learnedCount).getString();
        String schoolLabel = Component.translatable("screen.seeking_immortals.method_tree.school_filter",
                schoolDisplay(activeSchool)).getString();
        if (layout.header().height() >= 26) {
            int half = Math.max(1, layout.header().width() / 2);
            ImmortalUiSkin.drawStringFit(font, graphics, headerText,
                    layout.header().x() + 7, layout.header().y() + 18,
                    Math.max(1, half - 10), ImmortalUiSkin.JOURNAL_PAPER_MUTED, false);
            ImmortalUiSkin.drawStringFit(font, graphics, schoolLabel,
                    layout.header().x() + half, layout.header().y() + 18,
                    Math.max(1, layout.header().width() - half - 7),
                    ImmortalUiSkin.JOURNAL_JADE_TEXT, false);
        }
    }

    @Override
    protected void renderJournalContent(GuiGraphics graphics, JournalChrome chrome,
                                         int mouseX, int mouseY, float partialTick) {
        Layout layout = calculateLayout(width, height);
        ImmortalUiSkin.drawInnerFrame(graphics, layout.list().x(), layout.list().y(),
                layout.list().width(), layout.list().height());
        ImmortalUiSkin.drawInnerFrame(graphics, layout.detail().x(), layout.detail().y(),
                layout.detail().width(), layout.detail().height());
        renderMethodList(graphics, layout, mouseX, mouseY);
        renderMethodDetail(graphics, layout);
        updateCultivateButton();
    }

    private static UiRect toUi(Rect rect) {
        return new UiRect(rect.x(), rect.y(), rect.width(), rect.height());
    }

    private void renderMethodList(GuiGraphics graphics, Layout layout, int mouseX, int mouseY) {
        Rect list = layout.list();
        Rect viewport = new Rect(list.x() + 3, list.y() + 2,
                Math.max(1, list.width() - 8), Math.max(1, list.height() - 4));
        int visibleRows = visibleListRows(layout);
        scroll = Mth.clamp(scroll, 0, maxScroll());
        if (filtered.isEmpty()) {
            ImmortalUiSkin.drawStringFit(font, graphics,
                    Component.translatable("screen.seeking_immortals.method_tree.empty").getString(),
                    viewport.x(), viewport.y(), viewport.width(), ImmortalUiSkin.JOURNAL_PAPER_MUTED, false);
            return;
        }

        int hovered = hoveredListIndex(layout, mouseX, mouseY);
        int end = Math.min(filtered.size(), scroll + visibleRows);
        ImmortalUiSkin.withScissor(graphics, viewport.x(), viewport.y(), viewport.width(), viewport.height(), () -> {
            for (int i = scroll; i < end; i++) {
                TextMaterialCatalogService.MethodEntry method = filtered.get(i);
                int rowY = viewport.y() + (i - scroll) * LINE;
                boolean selected = i == selectedIndex;
                boolean learned = ClientMethodData.hasLearned(method.id());
                int layer = ClientMethodData.getLayer(method.id());
                ImmortalUiSkin.InteractionState state = selected
                        ? ImmortalUiSkin.InteractionState.SELECTED
                        : hovered == i ? ImmortalUiSkin.InteractionState.HOVERED
                        : ImmortalUiSkin.InteractionState.NORMAL;
                ImmortalUiSkin.drawListRow(graphics, viewport.x(), rowY, viewport.width(), LINE, state);
                String mark = learned ? "◆ " : "◇ ";
                String name = CultivationDisplayTexts.methodName(method);
                if (learned) {
                    name += " · " + CultivationDisplayTexts.level(layer).getString();
                }
                int color = learned ? ImmortalUiSkin.JOURNAL_JADE_TEXT
                        : selected ? ImmortalUiSkin.JOURNAL_JADE_TEXT : ImmortalUiSkin.JOURNAL_PAPER;
                ImmortalUiSkin.drawStringFit(font, graphics, mark + name,
                        viewport.x() + 4, rowY + 2, Math.max(1, viewport.width() - 8), color, false);
            }
        });
        ImmortalUiSkin.drawThinScrollbar(graphics, list.right() - 3, viewport.y(), viewport.height(),
                filtered.size() * LINE, viewport.height(), scroll * LINE);
    }

    private void renderMethodDetail(GuiGraphics graphics, Layout layout) {
        Rect detail = layout.detail();
        Rect viewport = new Rect(detail.x() + 4, detail.y() + 3,
                Math.max(1, detail.width() - 11), detailViewportHeight(layout));
        detailScroll = Mth.clamp(detailScroll, 0, maxDetailScroll(layout));
        int startY = viewport.y() - detailScroll;
        graphHits.clear();
        final int[] endY = {startY};
        ImmortalUiSkin.withScissor(graphics, viewport.x(), viewport.y(), viewport.width(), viewport.height(), () ->
                endY[0] = renderDetailContent(graphics, viewport.x(), startY, viewport.width()));
        renderedDetailHeight = Math.max(0, endY[0] - startY + 2);
        detailScroll = Mth.clamp(detailScroll, 0, maxDetailScroll(layout));
        ImmortalUiSkin.drawThinScrollbar(graphics, detail.right() - 3, viewport.y(), viewport.height(),
                renderedDetailHeight, viewport.height(), detailScroll);
    }

    private int renderDetailContent(GuiGraphics graphics, int detailX, int detailY, int detailW) {
        TextMaterialCatalogService.MethodEntry selected = selectedMethod();
        if (selected == null) {
            ImmortalUiSkin.drawStringFit(font, graphics,
                    Component.translatable("screen.seeking_immortals.method_tree.select_hint").getString(),
                    detailX, detailY, detailW, ImmortalUiSkin.JOURNAL_PAPER_MUTED, false);
            return detailY + LINE;
        }

        boolean learned = ClientMethodData.hasLearned(selected.id());
        int layer = ClientMethodData.getLayer(selected.id());
        int maxLayer = ManualCatalogService.maxMethodLayer(selected.id());
        String name = CultivationDisplayTexts.methodName(selected);
        ImmortalUiSkin.drawStringFit(font, graphics, name, detailX, detailY, detailW,
                ImmortalUiSkin.JOURNAL_PAPER, false);
        int y = detailY + LINE + 2;
        y = detailLine(graphics, detailX, y, detailW, "school",
                CultivationDisplayTexts.schoolText(selected.school()));
        y = detailLine(graphics, detailX, y, detailW, "realm",
                CultivationDisplayTexts.realmText(selected.realmMin()));
        y = detailLine(graphics, detailX, y, detailW, "attr",
                CultivationDisplayTexts.attributeText(selected.attribute()));
        if (learned) {
            y = detailLine(graphics, detailX, y, detailW, "layer",
                    layer + " / " + maxLayer);
            String layerName = MethodLayerTechniqueService.layerNameForLayer(selected.id(), layer);
            if (!layerName.isBlank()) {
                y = detailLine(graphics, detailX, y, detailW, "layer_name",
                        CultivationDisplayTexts.safeText(layerName));
            }
            if (layer < maxLayer) {
                y = detailLine(graphics, detailX, y, detailW, "cost_sp",
                        Integer.toString(ManualCatalogService.cultivateSpiritualCost(selected.id(), layer)));
                y = detailLine(graphics, detailX, y, detailW, "cost_exp",
                        Integer.toString(ManualCatalogService.cultivateCultivationCost(selected.id(), layer)));
                String nextRealm = MethodLayerTechniqueService.requiredRealmForLayer(selected.id(), layer + 1);
                if (!nextRealm.isBlank()) {
                    y = detailLine(graphics, detailX, y, detailW, "next_realm",
                            com.xunxian.seekingimmortals.artifact.ArtifactDisplayTexts.realm(nextRealm).getString());
                }
            }
        }

        y += 4;
        ImmortalUiSkin.drawStringFit(font, graphics,
                Component.translatable("screen.seeking_immortals.method_tree.layer_graph").getString(),
                detailX, y, detailW, ImmortalUiSkin.JOURNAL_JADE_TEXT, false);
        y += LINE;
        ImmortalUiSkin.drawSemanticStatusBar(graphics, detailX, y, detailW, 5,
                learned ? layer / (double)maxLayer : 0.0D,
                learned ? ImmortalUiSkin.StatusBarStyle.CULTIVATION : ImmortalUiSkin.StatusBarStyle.NEUTRAL);
        y += 8;
        y = drawLayerNodeChain(graphics, detailX, y, detailW, maxLayer, learned ? layer : 0);
        y += 4;
        ImmortalUiSkin.drawStringFit(font, graphics,
                Component.translatable("screen.seeking_immortals.method_tree.school_graph").getString(),
                detailX, y, detailW, ImmortalUiSkin.JOURNAL_JADE_TEXT, false);
        y += LINE;
        y = drawSchoolAdjacencyGraph(graphics, detailX, y, detailW, selected);
        ImmortalUiSkin.drawStringFit(font, graphics,
                Component.translatable("screen.seeking_immortals.method_tree.school_graph_hint").getString(),
                detailX, y, detailW, ImmortalUiSkin.JOURNAL_PAPER_MUTED, false);
        y += LINE;
        String statusKey = learned
                ? (layer >= maxLayer
                ? "screen.seeking_immortals.method_tree.status_max"
                : "screen.seeking_immortals.method_tree.status_learned")
                : "screen.seeking_immortals.method_tree.status_locked";
        ImmortalUiSkin.drawStringFit(font, graphics, Component.translatable(statusKey).getString(),
                detailX, y + 2, detailW,
                learned ? ImmortalUiSkin.JOURNAL_JADE_TEXT : ImmortalUiSkin.JOURNAL_SPIRIT, false);
        y += LINE + 4;
        ImmortalUiSkin.drawStringFit(font, graphics,
                Component.translatable(learned
                        ? "screen.seeking_immortals.method_tree.cultivate_hint"
                        : "screen.seeking_immortals.method_tree.learn_hint").getString(),
                detailX, y, detailW, ImmortalUiSkin.JOURNAL_PAPER_MUTED, false);
        y += LINE;
        if (!ClientMethodData.isSynced()) {
            ImmortalUiSkin.drawStringFit(font, graphics,
                    Component.translatable("screen.seeking_immortals.method_tree.waiting_sync").getString(),
                    detailX, y, detailW, ImmortalUiSkin.JOURNAL_WARNING, false);
            y += LINE;
        }
        return y;
    }

    private int detailLine(GuiGraphics graphics, int x, int y, int width, String labelKey, String value) {
        String label = Component.translatable("screen.seeking_immortals.method_tree." + labelKey).getString();
        ImmortalUiSkin.drawStringFit(font, graphics, label + ": " + value, x, y, width,
                ImmortalUiSkin.JOURNAL_PAPER, false);
        return y + LINE;
    }

    static boolean hasDetailRoomForSchoolHint(int graphBottomY, int controlsTopY, int lineHeight) {
        int statusAndFinalHeight = LINE + 4 + Math.max(1, lineHeight);
        return graphBottomY + LINE + statusAndFinalHeight <= controlsTopY;
    }

    /**
     * Wave483: horizontal dynamic layer nodes with links; filled through current layer.
     */
    private int drawLayerNodeChain(GuiGraphics graphics, int x, int y, int width,
                                   int maxLayer, int currentLayer) {
        int max = Math.max(1, maxLayer);
        int nodeSize = Math.max(1, Math.min(NODE, width));
        int gap = width >= NODE + 4 ? 4 : 1;
        int columns = Math.max(1, Math.min(max, (width + gap) / Math.max(1, nodeSize + gap)));
        int rows = (max + columns - 1) / columns;
        int[] xs = new int[max];
        int[] ys = new int[max];
        for (int i = 0; i < max; i++) {
            int row = i / columns;
            int column = i % columns;
            int nodesInRow = Math.min(columns, max - row * columns);
            int rowWidth = nodesInRow * nodeSize + Math.max(0, nodesInRow - 1) * gap;
            xs[i] = x + Math.max(0, (width - rowWidth) / 2) + column * (nodeSize + gap);
            ys[i] = y + row * (nodeSize + gap);
        }
        for (int i = 1; i < max; i++) {
            drawLink(graphics, xs[i - 1] + nodeSize / 2, ys[i - 1] + nodeSize / 2,
                    xs[i] + nodeSize / 2, ys[i] + nodeSize / 2);
        }
        for (int i = 1; i <= max; i++) {
            int nx = xs[i - 1];
            int ny = ys[i - 1];
            int color;
            if (currentLayer <= 0) {
                color = ImmortalUiSkin.JOURNAL_NODE_LOCKED;
            } else if (i < currentLayer) {
                color = ImmortalUiSkin.JOURNAL_JADE;
            } else if (i == currentLayer) {
                color = ImmortalUiSkin.JOURNAL_BORDER;
            } else {
                color = ImmortalUiSkin.JOURNAL_NODE_EMPTY;
            }
            graphics.fill(nx, ny, nx + nodeSize, ny + nodeSize, color);
            if (nodeSize > 2) {
                graphics.fill(nx + 1, ny + 1, nx + nodeSize - 1, ny + nodeSize - 1,
                        ImmortalUiSkin.JOURNAL_INNER);
            }
            if (nodeSize >= 8) {
                ImmortalUiSkin.drawStringFit(font, graphics, Integer.toString(i), nx + 1, ny + 2,
                        Math.max(1, nodeSize - 2), ImmortalUiSkin.JOURNAL_PAPER, false);
            }
        }
        return y + rows * nodeSize + Math.max(0, rows - 1) * gap + 2;
    }

    /**
     * Wave483/484/485: freer school graph — up to 6 peers in a 2x3 grid with links;
     * clickable + freeform drag offsets (client-only).
     */
    private int drawSchoolAdjacencyGraph(GuiGraphics graphics, int x, int y, int width,
                                         TextMaterialCatalogService.MethodEntry selected) {
        graphHits.clear();
        List<TextMaterialCatalogService.MethodEntry> schoolPeers = new ArrayList<>();
        String school = selected.school() == null ? "" : selected.school().trim().toLowerCase(Locale.ROOT);
        for (TextMaterialCatalogService.MethodEntry method : allMethods) {
            String s = method.school() == null ? "" : method.school().trim().toLowerCase(Locale.ROOT);
            if (school.equals(s)) {
                schoolPeers.add(method);
            }
        }
        if (schoolPeers.isEmpty()) {
            schoolPeers.add(selected);
        }
        int focus = 0;
        for (int i = 0; i < schoolPeers.size(); i++) {
            if (schoolPeers.get(i).id().equalsIgnoreCase(selected.id())) {
                focus = i;
                break;
            }
        }
        int start = Math.max(0, Math.min(focus - 1, Math.max(0, schoolPeers.size() - GRAPH_MAX)));
        int end = Math.min(schoolPeers.size(), start + GRAPH_MAX);
        List<TextMaterialCatalogService.MethodEntry> window = schoolPeers.subList(start, end);
        int cols = width >= 150 ? GRAPH_COLS : width >= 86 ? 2 : 1;
        cols = Math.max(1, Math.min(cols, window.size()));
        int rows = Math.max(1, (window.size() + cols - 1) / cols);
        int gapX = cols <= 1 ? 0 : width >= 150 ? 6 : 4;
        int gapY = 4;
        int nodeW = Math.max(1, (width - gapX * (cols - 1)) / cols);
        int nodeH = 16;
        graphOriginX = x;
        graphOriginY = y;
        graphWidth = width;
        graphHeight = rows * (nodeH + gapY) + 8;
        graphColumns = cols;
        graphGapX = gapX;
        graphGapY = gapY;
        graphNodeWidth = nodeW;
        graphNodeHeight = nodeH;

        int[] xs = new int[window.size()];
        int[] ys = new int[window.size()];
        for (int i = 0; i < window.size(); i++) {
            int col = i % cols;
            int row = i / cols;
            int baseX = x + col * (nodeW + gapX);
            int baseY = y + row * (nodeH + gapY);
            int[] off = layoutOffsets.get(window.get(i).id().toLowerCase(Locale.ROOT));
            int ox = off == null ? 0 : off[0];
            int oy = off == null ? 0 : off[1];
            xs[i] = Mth.clamp(baseX + ox, x, x + Math.max(0, width - nodeW));
            ys[i] = Mth.clamp(baseY + oy, y, y + Math.max(0, graphHeight - nodeH));
        }
        // Draw links between neighbors by index (grid topology), using freeform positions.
        for (int i = 0; i < window.size(); i++) {
            int col = i % cols;
            int row = i / cols;
            if (col + 1 < cols && i + 1 < window.size() && (i + 1) / cols == row) {
                int x1 = xs[i] + nodeW / 2;
                int y1 = ys[i] + nodeH / 2;
                int x2 = xs[i + 1] + nodeW / 2;
                int y2 = ys[i + 1] + nodeH / 2;
                drawLink(graphics, x1, y1, x2, y2);
            }
            int below = i + cols;
            if (below < window.size()) {
                int x1 = xs[i] + nodeW / 2;
                int y1 = ys[i] + nodeH / 2;
                int x2 = xs[below] + nodeW / 2;
                int y2 = ys[below] + nodeH / 2;
                drawLink(graphics, x1, y1, x2, y2);
            }
        }
        for (int i = 0; i < window.size(); i++) {
            TextMaterialCatalogService.MethodEntry method = window.get(i);
            boolean isFocus = method.id().equalsIgnoreCase(selected.id());
            drawGraphNode(graphics, xs[i], ys[i], nodeW, nodeH, method, isFocus);
            graphHits.add(new GraphHit(xs[i], ys[i], nodeW, nodeH, method.id()));
        }
        return y + graphHeight;
    }

    private void drawLink(GuiGraphics graphics, int x1, int y1, int x2, int y2) {
        int minX = Math.min(x1, x2);
        int maxX = Math.max(x1, x2);
        int minY = Math.min(y1, y2);
        int maxY = Math.max(y1, y2);
        if (Math.abs(x2 - x1) >= Math.abs(y2 - y1)) {
            graphics.fill(minX, y1 - 1, maxX, y1 + 1, ImmortalUiSkin.JOURNAL_BORDER_DIM);
            graphics.fill(x2 - 1, minY, x2 + 1, maxY, ImmortalUiSkin.JOURNAL_BORDER_DIM);
        } else {
            graphics.fill(x1 - 1, minY, x1 + 1, maxY, ImmortalUiSkin.JOURNAL_BORDER_DIM);
            graphics.fill(minX, y2 - 1, maxX, y2 + 1, ImmortalUiSkin.JOURNAL_BORDER_DIM);
        }
    }

    private void drawGraphNode(GuiGraphics graphics, int x, int y, int w, int h,
                               TextMaterialCatalogService.MethodEntry method, boolean focus) {
        boolean present = method != null;
        boolean learned = present && ClientMethodData.hasLearned(method.id());
        int border = focus ? ImmortalUiSkin.JOURNAL_BORDER : (learned ? ImmortalUiSkin.JOURNAL_JADE : ImmortalUiSkin.JOURNAL_NODE_EMPTY);
        graphics.fill(x, y, x + w, y + h, border);
        if (w > 2 && h > 2) {
            graphics.fill(x + 1, y + 1, x + w - 1, y + h - 1, ImmortalUiSkin.JOURNAL_CONTROL);
        }
        String label = present ? CultivationDisplayTexts.methodName(method) : "·";
        if (present && learned) {
            label = label + " · " + CultivationDisplayTexts.level(ClientMethodData.getLayer(method.id())).getString();
        }
        ImmortalUiSkin.drawStringFit(font, graphics, label, x + 3, y + Math.max(2, (h - 8) / 2),
                Math.max(1, w - 6), focus ? ImmortalUiSkin.JOURNAL_PAPER
                        : (learned ? ImmortalUiSkin.JOURNAL_JADE_TEXT : ImmortalUiSkin.JOURNAL_PAPER_MUTED),
                false);
    }

    private String schoolDisplay(String school) {
        if (school == null || school.isBlank() || "all".equals(school)) {
            return Component.translatable("screen.seeking_immortals.method_tree.school_all").getString();
        }
        if ("misc".equals(school)) {
            return Component.translatable("screen.seeking_immortals.method_tree.school_misc").getString();
        }
        return CultivationDisplayTexts.schoolText(school);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            Layout layout = calculateLayout(width, height);
            // Wave484/485: click/drag school-graph nodes.
            if (layout.detail().contains(mouseX, mouseY)) {
                for (GraphHit hit : graphHits) {
                    if (hit.contains(mouseX, mouseY) && hit.methodId() != null && !hit.methodId().isBlank()) {
                        selectMethodById(hit.methodId());
                        draggingMethodId = hit.methodId();
                        scrollDragTarget = ScrollDragTarget.NONE;
                        dragGrabX = mouseX - hit.x();
                        dragGrabY = mouseY - hit.y();
                        return true;
                    }
                }
            }
            int index = hoveredListIndex(layout, mouseX, mouseY);
            if (layout.list().contains(mouseX, mouseY)) {
                pendingListIndex = index;
                beginScrollDrag(ScrollDragTarget.LIST_PENDING, mouseY, scroll);
                return true;
            }
            if (layout.detail().contains(mouseX, mouseY)) {
                beginScrollDrag(ScrollDragTarget.DETAIL, mouseY, detailScroll);
                return true;
            }
        }
        resetPointerDrag();
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void beginScrollDrag(ScrollDragTarget target, double mouseY, int currentScroll) {
        scrollDragTarget = target;
        scrollDragStartY = mouseY;
        scrollAtDragStart = currentScroll;
    }

    private void resetPointerDrag() {
        scrollDragTarget = ScrollDragTarget.NONE;
        pendingListIndex = -1;
        draggingMethodId = "";
        scrollDragStartY = 0.0D;
        scrollAtDragStart = 0;
    }

    private int hoveredListIndex(Layout layout, double mouseX, double mouseY) {
        Rect list = layout.list();
        Rect viewport = new Rect(list.x() + 3, list.y() + 2,
                Math.max(1, list.width() - 8), Math.max(1, list.height() - 4));
        if (!viewport.contains(mouseX, mouseY)) {
            return -1;
        }
        int row = (int)((mouseY - viewport.y()) / LINE);
        if (row < 0 || row >= visibleListRows(layout)) {
            return -1;
        }
        int index = scroll + row;
        return index >= 0 && index < filtered.size() ? index : -1;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0 && draggingMethodId != null && !draggingMethodId.isBlank()) {
            int nodeW = graphNodeWidth;
            int nodeH = graphNodeHeight;
            int targetX = (int) Math.round(mouseX - dragGrabX);
            int targetY = (int) Math.round(mouseY - dragGrabY);
            int[] clamped = clampGraphNodePosition(targetX, targetY,
                    graphOriginX, graphOriginY, graphWidth, graphHeight, nodeW, nodeH);
            targetX = clamped[0];
            targetY = clamped[1];

            // Convert absolute position back into offset from default grid slot.
            int idxInWindow = -1;
            List<TextMaterialCatalogService.MethodEntry> window = currentGraphWindow();
            for (int i = 0; i < window.size(); i++) {
                if (draggingMethodId.equalsIgnoreCase(window.get(i).id())) {
                    idxInWindow = i;
                    break;
                }
            }
            if (idxInWindow >= 0) {
                layoutOffsets.put(draggingMethodId.toLowerCase(Locale.ROOT),
                        offsetFromGrid(targetX, targetY, graphOriginX, graphOriginY,
                                idxInWindow, graphColumns, nodeW, nodeH, graphGapX, graphGapY));
            }
            return true;
        }
        if (button == 0 && scrollDragTarget != ScrollDragTarget.NONE) {
            Layout layout = calculateLayout(width, height);
            if (scrollDragTarget == ScrollDragTarget.LIST_PENDING) {
                if (!crossedScrollDragThreshold(scrollDragStartY, mouseY)) {
                    return true;
                }
                scrollDragTarget = ScrollDragTarget.LIST;
                pendingListIndex = -1;
            }
            if (scrollDragTarget == ScrollDragTarget.LIST) {
                scroll = dragListScroll(scrollAtDragStart, scrollDragStartY, mouseY,
                        filtered.size(), visibleListRows(layout));
            } else {
                detailScroll = dragDetailScroll(scrollAtDragStart, scrollDragStartY, mouseY,
                        renderedDetailHeight, detailViewportHeight(layout));
            }
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && draggingMethodId != null && !draggingMethodId.isBlank()) {
            // Wave486: persist final offset to server.
            int[] off = layoutOffsets.get(draggingMethodId.toLowerCase(Locale.ROOT));
            int x = off == null ? 0 : off[0];
            int y = off == null ? 0 : off[1];
            ModNetwork.CHANNEL.sendToServer(new MethodLayoutActionPacket(
                    "set:" + draggingMethodId + ":" + x + ":" + y));
            resetPointerDrag();
            updateCultivateButton();
            return true;
        }
        if (button == 0 && scrollDragTarget != ScrollDragTarget.NONE) {
            int clickedIndex = scrollDragTarget == ScrollDragTarget.LIST_PENDING ? pendingListIndex : -1;
            resetPointerDrag();
            if (clickedIndex >= 0 && clickedIndex < filtered.size()) {
                selectedIndex = clickedIndex;
                detailScroll = 0;
                updateCultivateButton();
            }
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private List<TextMaterialCatalogService.MethodEntry> currentGraphWindow() {
        TextMaterialCatalogService.MethodEntry selected = selectedMethod();
        if (selected == null) {
            return List.of();
        }
        List<TextMaterialCatalogService.MethodEntry> schoolPeers = new ArrayList<>();
        String school = selected.school() == null ? "" : selected.school().trim().toLowerCase(Locale.ROOT);
        for (TextMaterialCatalogService.MethodEntry method : allMethods) {
            String s = method.school() == null ? "" : method.school().trim().toLowerCase(Locale.ROOT);
            if (school.equals(s)) {
                schoolPeers.add(method);
            }
        }
        if (schoolPeers.isEmpty()) {
            schoolPeers.add(selected);
        }
        int focus = 0;
        for (int i = 0; i < schoolPeers.size(); i++) {
            if (schoolPeers.get(i).id().equalsIgnoreCase(selected.id())) {
                focus = i;
                break;
            }
        }
        int start = Math.max(0, Math.min(focus - 1, Math.max(0, schoolPeers.size() - GRAPH_MAX)));
        int end = Math.min(schoolPeers.size(), start + GRAPH_MAX);
        return schoolPeers.subList(start, end);
    }

    private void selectMethodById(String methodId) {
        if (methodId == null || methodId.isBlank()) {
            return;
        }
        // Prefer selecting inside current filter; otherwise switch school tab to method school.
        for (int i = 0; i < filtered.size(); i++) {
            if (methodId.equalsIgnoreCase(filtered.get(i).id())) {
                selectedIndex = i;
                // Keep selected row visible.
                int visibleRows = visibleListRows(calculateLayout(width, height));
                if (selectedIndex < scroll) {
                    scroll = selectedIndex;
                } else if (selectedIndex >= scroll + visibleRows) {
                    scroll = selectedIndex - visibleRows + 1;
                }
                detailScroll = 0;
                updateCultivateButton();
                return;
            }
        }
        TextMaterialCatalogService.MethodEntry found = null;
        for (TextMaterialCatalogService.MethodEntry method : allMethods) {
            if (methodId.equalsIgnoreCase(method.id())) {
                found = method;
                break;
            }
        }
        if (found == null) {
            return;
        }
        String school = found.school() == null || found.school().isBlank()
                ? "misc"
                : found.school().trim().toLowerCase(Locale.ROOT);
        if (!schoolTabs.contains(school)) {
            school = "all";
        }
        activeSchool = school;
        applyFilter();
        for (int i = 0; i < filtered.size(); i++) {
            if (methodId.equalsIgnoreCase(filtered.get(i).id())) {
                selectedIndex = i;
                scroll = Math.max(0, Math.min(selectedIndex, maxScroll()));
                detailScroll = 0;
                break;
            }
        }
        updateCultivateButton();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        Layout layout = calculateLayout(width, height);
        int direction = (int)Math.signum(delta);
        if (direction != 0 && layout.list().contains(mouseX, mouseY)) {
            scroll = scrollListBy(scroll, direction, filtered.size(), visibleListRows(layout));
            return true;
        }
        if (direction != 0 && layout.detail().contains(mouseX, mouseY)) {
            detailScroll = scrollDetailBy(detailScroll, direction,
                    renderedDetailHeight, detailViewportHeight(layout), LINE);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.setScreen(parent);
            return;
        }
        super.onClose();
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
                  Rect header, Rect list, Rect detail, Rect cultivateButton,
                  Rect prevSchoolButton, Rect nextSchoolButton, Rect resetLayoutButton,
                  Rect doneButton) {
    }
}
