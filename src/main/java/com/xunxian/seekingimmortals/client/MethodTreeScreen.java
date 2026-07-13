package com.xunxian.seekingimmortals.client;

import com.xunxian.seekingimmortals.catalog.ManualCatalogService;
import com.xunxian.seekingimmortals.catalog.TextMaterialCatalogService;
import com.xunxian.seekingimmortals.network.MethodActionPacket;
import com.xunxian.seekingimmortals.network.MethodLayoutActionPacket;
import com.xunxian.seekingimmortals.network.ModNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
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
 * Wave478: interactive cultivation method tree / learn UI.
 * Wave481: cultivate button for method layers (1-9).
 * Wave483: node-link layer chain + school adjacency graph in the detail pane.
 * Wave484: freer multi-node school graph with clickable peers (up to 6).
 * Wave485: freeform drag layout for school-graph nodes (client offsets).
 * Wave486: layout offsets server-persisted via MethodLayoutService / protocol 17.
 * Reuses TextMaterialCatalogService method index + ClientMethodData learned mirror.
 * Learn/cultivate/sync go through MethodActionPacket → ManualCatalogService (server authority).
 */
public class MethodTreeScreen extends Screen {
    private static final int PANEL_W = 460;
    private static final int PANEL_H = 280;
    private static final int LINE = 12;
    private static final int LIST_VISIBLE = 12;
    private static final int NODE = 12;
    private static final int LINK = 0x88E6D59A;
    private static final int NODE_EMPTY = 0xFF3B2F18;
    private static final int NODE_REACHED = 0xFF2F8F45;
    private static final int NODE_CURRENT = 0xFFE6D59A;
    private static final int NODE_LOCKED = 0xFF6A5A3A;
    private static final int GRAPH_COLS = 3;
    private static final int GRAPH_MAX = 6;

    private final Screen parent;
    private List<TextMaterialCatalogService.MethodEntry> allMethods = List.of();
    private List<TextMaterialCatalogService.MethodEntry> filtered = List.of();
    private List<String> schoolTabs = List.of("all");
    private String activeSchool = "all";
    private int scroll;
    private int selectedIndex = -1;
    private Button learnButton;
    private Button cultivateButton;
    private Button prevSchoolButton;
    private Button nextSchoolButton;
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

    private record GraphHit(int x, int y, int w, int h, String methodId) {
        boolean contains(double mx, double my) {
            return mx >= x && mx <= x + w && my >= y && my <= y + h;
        }
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
        reloadCatalog();
        int left = panelLeft();
        int top = panelTop();

        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(left + PANEL_W - 70, top + PANEL_H - 26, 58, 18)
                .build());
        addRenderableWidget(Button.builder(Component.translatable("screen.seeking_immortals.method_tree.sync"), b -> {
                    ModNetwork.CHANNEL.sendToServer(new MethodActionPacket("sync"));
                })
                .bounds(left + 12, top + PANEL_H - 26, 52, 18)
                .build());
        learnButton = Button.builder(Component.translatable("screen.seeking_immortals.method_tree.learn"), b -> {
                    TextMaterialCatalogService.MethodEntry selected = selectedMethod();
                    if (selected != null) {
                        ModNetwork.CHANNEL.sendToServer(new MethodActionPacket("learn:" + selected.id()));
                    }
                })
                .bounds(left + 70, top + PANEL_H - 26, 48, 18)
                .build();
        addRenderableWidget(learnButton);
        // Wave481: cultivate raises method layer when already learned.
        cultivateButton = Button.builder(Component.translatable("screen.seeking_immortals.method_tree.cultivate"), b -> {
                    TextMaterialCatalogService.MethodEntry selected = selectedMethod();
                    if (selected != null) {
                        ModNetwork.CHANNEL.sendToServer(new MethodActionPacket("cultivate:" + selected.id()));
                    }
                })
                .bounds(left + 122, top + PANEL_H - 26, 48, 18)
                .build();
        addRenderableWidget(cultivateButton);

        prevSchoolButton = Button.builder(Component.literal("<"), b -> cycleSchool(-1))
                .bounds(left + 176, top + PANEL_H - 26, 18, 18)
                .build();
        nextSchoolButton = Button.builder(Component.literal(">"), b -> cycleSchool(1))
                .bounds(left + 286, top + PANEL_H - 26, 18, 18)
                .build();
        addRenderableWidget(prevSchoolButton);
        addRenderableWidget(nextSchoolButton);
        // Wave485/486: reset freeform node offsets (server-cleared + resync).
        addRenderableWidget(Button.builder(Component.translatable("screen.seeking_immortals.method_tree.reset_layout"), b -> {
                    layoutOffsets.clear();
                    draggingMethodId = "";
                    ModNetwork.CHANNEL.sendToServer(new MethodLayoutActionPacket("clear"));
                })
                .bounds(left + 310, top + PANEL_H - 26, 52, 18)
                .build());

        // Pull any already-synced layout into local working map.
        hydrateLayoutFromClientMirror();
        updateLearnButton();
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
            String da = a.display() == null || a.display().isBlank() ? a.id() : a.display();
            String db = b.display() == null || b.display().isBlank() ? b.id() : b.display();
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
        updateLearnButton();
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
        return Math.max(0, filtered.size() - LIST_VISIBLE);
    }

    private TextMaterialCatalogService.MethodEntry selectedMethod() {
        if (selectedIndex < 0 || selectedIndex >= filtered.size()) {
            return null;
        }
        return filtered.get(selectedIndex);
    }

    private void updateLearnButton() {
        TextMaterialCatalogService.MethodEntry selected = selectedMethod();
        boolean hasSelection = selected != null;
        boolean learned = hasSelection && ClientMethodData.hasLearned(selected.id());
        int layer = hasSelection ? ClientMethodData.getLayer(selected.id()) : 0;
        if (learnButton != null) {
            learnButton.active = hasSelection && !learned;
        }
        if (cultivateButton != null) {
            cultivateButton.active = learned && layer > 0 && layer < ManualCatalogService.MAX_METHOD_LAYER;
        }
    }

    private int panelLeft() {
        return (width - PANEL_W) / 2;
    }

    private int panelTop() {
        return (height - PANEL_H) / 2;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        int left = panelLeft();
        int top = panelTop();
        ImmortalUiSkin.drawPanel(graphics, left, top, PANEL_W, PANEL_H);

        graphics.drawCenteredString(font, title, left + PANEL_W / 2, top + 8, ImmortalUiSkin.COLOR_TITLE);

        int learnedCount = ClientMethodData.getLearnedMethodCount();
        String header = Component.translatable("screen.seeking_immortals.method_tree.header",
                filtered.size(), allMethods.size(), learnedCount).getString();
        ImmortalUiSkin.drawStringFit(font, graphics, header, left + 12, top + 22, PANEL_W - 24,
                ImmortalUiSkin.COLOR_TEXT_MUTED, false);

        String schoolLabel = Component.translatable("screen.seeking_immortals.method_tree.school_filter",
                schoolDisplay(activeSchool)).getString();
        ImmortalUiSkin.drawStringFit(font, graphics, schoolLabel, left + 198, top + PANEL_H - 21, 84,
                ImmortalUiSkin.COLOR_TEXT_NORMAL, false);

        // Left list
        int listX = left + 12;
        int listY = top + 36;
        int listW = 188;
        int listH = LIST_VISIBLE * LINE + 4;
        graphics.fill(listX - 2, listY - 2, listX + listW + 2, listY + listH, 0x66130C05);
        if (filtered.isEmpty()) {
            ImmortalUiSkin.drawStringFit(font, graphics,
                    Component.translatable("screen.seeking_immortals.method_tree.empty").getString(),
                    listX, listY, listW, ImmortalUiSkin.COLOR_TEXT_MUTED, false);
        } else {
            int end = Math.min(filtered.size(), scroll + LIST_VISIBLE);
            for (int i = scroll; i < end; i++) {
                TextMaterialCatalogService.MethodEntry method = filtered.get(i);
                int rowY = listY + (i - scroll) * LINE;
                boolean selected = i == selectedIndex;
                boolean learned = ClientMethodData.hasLearned(method.id());
                int layer = ClientMethodData.getLayer(method.id());
                if (selected) {
                    graphics.fill(listX - 1, rowY - 1, listX + listW + 1, rowY + LINE - 1, ImmortalUiSkin.COLOR_HOVER_BG);
                }
                String mark = learned ? "◆ " : "◇ ";
                String name = method.display() == null || method.display().isBlank() ? method.id() : method.display();
                if (learned) {
                    name = name + " L" + layer;
                }
                int color = learned ? ImmortalUiSkin.COLOR_TEXT_SUCCESS
                        : selected ? ImmortalUiSkin.COLOR_TITLE : ImmortalUiSkin.COLOR_TEXT_NORMAL;
                ImmortalUiSkin.drawStringFit(font, graphics, mark + name, listX, rowY, listW, color, false);
            }
        }

        // Right detail
        int detailX = left + 212;
        int detailY = top + 36;
        int detailW = PANEL_W - 224;
        int detailBoxH = listH + 18;
        graphics.fill(detailX - 2, detailY - 2, detailX + detailW + 2, detailY + detailBoxH, 0x66130C05);
        TextMaterialCatalogService.MethodEntry selected = selectedMethod();
        if (selected == null) {
            ImmortalUiSkin.drawStringFit(font, graphics,
                    Component.translatable("screen.seeking_immortals.method_tree.select_hint").getString(),
                    detailX, detailY, detailW, ImmortalUiSkin.COLOR_TEXT_MUTED, false);
        } else {
            boolean learned = ClientMethodData.hasLearned(selected.id());
            int layer = ClientMethodData.getLayer(selected.id());
            String name = selected.display() == null || selected.display().isBlank() ? selected.id() : selected.display();
            ImmortalUiSkin.drawStringFit(font, graphics, name, detailX, detailY, detailW, ImmortalUiSkin.COLOR_TITLE, false);
            int y = detailY + LINE + 2;
            y = detailLine(graphics, detailX, y, detailW, "id", selected.id());
            y = detailLine(graphics, detailX, y, detailW, "school",
                    selected.school() == null || selected.school().isBlank() ? "-" : selected.school());
            y = detailLine(graphics, detailX, y, detailW, "realm",
                    selected.realmMin() == null || selected.realmMin().isBlank() ? "-" : selected.realmMin());
            y = detailLine(graphics, detailX, y, detailW, "attr",
                    selected.attribute() == null || selected.attribute().isBlank() ? "-" : selected.attribute());
            if (learned) {
                y = detailLine(graphics, detailX, y, detailW, "layer",
                        layer + " / " + ManualCatalogService.MAX_METHOD_LAYER);
                if (layer < ManualCatalogService.MAX_METHOD_LAYER) {
                    y = detailLine(graphics, detailX, y, detailW, "cost_sp",
                            Integer.toString(ManualCatalogService.cultivateSpiritualCost(layer)));
                    y = detailLine(graphics, detailX, y, detailW, "cost_exp",
                            Integer.toString(ManualCatalogService.cultivateCultivationCost(layer)));
                }
            }
            // Wave483: layer node chain (1..9) + school adjacency graph.
            y += 4;
            ImmortalUiSkin.drawStringFit(font, graphics,
                    Component.translatable("screen.seeking_immortals.method_tree.layer_graph").getString(),
                    detailX, y, detailW, ImmortalUiSkin.COLOR_TITLE, false);
            y += LINE;
            y = drawLayerNodeChain(graphics, detailX, y, detailW, learned ? layer : 0);
            y += 4;
            ImmortalUiSkin.drawStringFit(font, graphics,
                    Component.translatable("screen.seeking_immortals.method_tree.school_graph").getString(),
                    detailX, y, detailW, ImmortalUiSkin.COLOR_TITLE, false);
            y += LINE;
            y = drawSchoolAdjacencyGraph(graphics, detailX, y, detailW, selected);
            int controlsTop = top + PANEL_H - 26;
            if (hasDetailRoomForSchoolHint(y, controlsTop, font.lineHeight)) {
                ImmortalUiSkin.drawStringFit(font, graphics,
                        Component.translatable("screen.seeking_immortals.method_tree.school_graph_hint").getString(),
                        detailX, y, detailW, ImmortalUiSkin.COLOR_TEXT_MUTED, false);
                y += LINE;
            }
            String statusKey = learned
                    ? (layer >= ManualCatalogService.MAX_METHOD_LAYER
                    ? "screen.seeking_immortals.method_tree.status_max"
                    : "screen.seeking_immortals.method_tree.status_learned")
                    : "screen.seeking_immortals.method_tree.status_locked";
            ImmortalUiSkin.drawStringFit(font, graphics,
                    Component.translatable(statusKey).getString(),
                    detailX, y + 2, detailW,
                    learned ? ImmortalUiSkin.COLOR_TEXT_SUCCESS : ImmortalUiSkin.COLOR_TEXT_BLUE, false);
            y += LINE + 4;
            y = Math.min(y, controlsTop - font.lineHeight - 1);
            ImmortalUiSkin.drawStringFit(font, graphics,
                    Component.translatable(learned
                            ? "screen.seeking_immortals.method_tree.cultivate_hint"
                            : "screen.seeking_immortals.method_tree.learn_hint").getString(),
                    detailX, y, detailW, ImmortalUiSkin.COLOR_TEXT_MUTED, false);
        }

        if (!ClientMethodData.isSynced()) {
            ImmortalUiSkin.drawStringFit(font, graphics,
                    Component.translatable("screen.seeking_immortals.method_tree.waiting_sync").getString(),
                    left + 12, top + PANEL_H - 40, PANEL_W - 24, ImmortalUiSkin.COLOR_TEXT_MUTED, false);
        }

        updateLearnButton();
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private int detailLine(GuiGraphics graphics, int x, int y, int width, String labelKey, String value) {
        String label = Component.translatable("screen.seeking_immortals.method_tree." + labelKey).getString();
        ImmortalUiSkin.drawStringFit(font, graphics, label + ": " + value, x, y, width,
                ImmortalUiSkin.COLOR_TEXT_NORMAL, false);
        return y + LINE;
    }

    static boolean hasDetailRoomForSchoolHint(int graphBottomY, int controlsTopY, int lineHeight) {
        int statusAndFinalHeight = LINE + 4 + Math.max(1, lineHeight);
        return graphBottomY + LINE + statusAndFinalHeight <= controlsTopY;
    }

    /**
     * Wave483: horizontal 1..MAX layer nodes with links; filled through current layer.
     */
    private int drawLayerNodeChain(GuiGraphics graphics, int x, int y, int width, int currentLayer) {
        int max = ManualCatalogService.MAX_METHOD_LAYER;
        int gap = Math.max(4, (width - max * NODE) / Math.max(1, max - 1));
        int total = max * NODE + (max - 1) * gap;
        int startX = x + Math.max(0, (width - total) / 2);
        int cy = y + NODE / 2;
        for (int i = 1; i <= max; i++) {
            int nx = startX + (i - 1) * (NODE + gap);
            if (i < max) {
                int lx1 = nx + NODE;
                int lx2 = nx + NODE + gap;
                graphics.fill(lx1, cy - 1, lx2, cy + 1, LINK);
            }
            int color;
            if (currentLayer <= 0) {
                color = NODE_LOCKED;
            } else if (i < currentLayer) {
                color = NODE_REACHED;
            } else if (i == currentLayer) {
                color = NODE_CURRENT;
            } else {
                color = NODE_EMPTY;
            }
            graphics.fill(nx, y, nx + NODE, y + NODE, color);
            graphics.fill(nx + 1, y + 1, nx + NODE - 1, y + NODE - 1, 0x66130C05);
            String num = Integer.toString(i);
            int tw = font.width(num);
            graphics.drawString(font, num, nx + (NODE - tw) / 2, y + 2, ImmortalUiSkin.COLOR_TEXT_NORMAL, false);
        }
        return y + NODE + 2;
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
        int cols = GRAPH_COLS;
        int rows = Math.max(1, (window.size() + cols - 1) / cols);
        int gapX = 6;
        int gapY = 4;
        int nodeW = Math.max(42, (width - gapX * (cols - 1)) / cols);
        int nodeH = 16;
        graphOriginX = x;
        graphOriginY = y;
        graphWidth = width;
        graphHeight = rows * (nodeH + gapY) + 8;

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
            drawGraphNode(graphics, xs[i], ys[i], nodeW, method, isFocus);
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
            graphics.fill(minX, y1 - 1, maxX, y1 + 1, LINK);
            graphics.fill(x2 - 1, minY, x2 + 1, maxY, LINK);
        } else {
            graphics.fill(x1 - 1, minY, x1 + 1, maxY, LINK);
            graphics.fill(minX, y2 - 1, maxX, y2 + 1, LINK);
        }
    }

    private void drawGraphNode(GuiGraphics graphics, int x, int y, int w,
                               TextMaterialCatalogService.MethodEntry method, boolean focus) {
        boolean present = method != null;
        boolean learned = present && ClientMethodData.hasLearned(method.id());
        int border = focus ? NODE_CURRENT : (learned ? NODE_REACHED : NODE_EMPTY);
        graphics.fill(x, y, x + w, y + 16, border);
        graphics.fill(x + 1, y + 1, x + w - 1, y + 15, 0xEE130C05);
        String label = present
                ? (method.display() == null || method.display().isBlank() ? method.id() : method.display())
                : "·";
        if (present && learned) {
            label = label + " L" + ClientMethodData.getLayer(method.id());
        }
        ImmortalUiSkin.drawStringFit(font, graphics, label, x + 3, y + 4, w - 6,
                focus ? ImmortalUiSkin.COLOR_TITLE
                        : (learned ? ImmortalUiSkin.COLOR_TEXT_SUCCESS : ImmortalUiSkin.COLOR_TEXT_MUTED),
                false);
    }

    private String schoolDisplay(String school) {
        if (school == null || school.isBlank() || "all".equals(school)) {
            return Component.translatable("screen.seeking_immortals.method_tree.school_all").getString();
        }
        if ("misc".equals(school)) {
            return Component.translatable("screen.seeking_immortals.method_tree.school_misc").getString();
        }
        return school;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            // Wave484/485: click/drag school-graph nodes.
            for (GraphHit hit : graphHits) {
                if (hit.contains(mouseX, mouseY) && hit.methodId() != null && !hit.methodId().isBlank()) {
                    selectMethodById(hit.methodId());
                    draggingMethodId = hit.methodId();
                    dragGrabX = mouseX - hit.x();
                    dragGrabY = mouseY - hit.y();
                    return true;
                }
            }
            int left = panelLeft();
            int top = panelTop();
            int listX = left + 12;
            int listY = top + 36;
            int listW = 188;
            int listH = LIST_VISIBLE * LINE;
            if (mouseX >= listX && mouseX <= listX + listW && mouseY >= listY && mouseY <= listY + listH) {
                int row = (int) ((mouseY - listY) / LINE);
                int index = scroll + row;
                if (index >= 0 && index < filtered.size()) {
                    selectedIndex = index;
                    updateLearnButton();
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0 && draggingMethodId != null && !draggingMethodId.isBlank()) {
            int nodeW = Math.max(42, (graphWidth - 6 * (GRAPH_COLS - 1)) / GRAPH_COLS);
            int nodeH = 16;
            int targetX = (int) Math.round(mouseX - dragGrabX);
            int targetY = (int) Math.round(mouseY - dragGrabY);
            targetX = Mth.clamp(targetX, graphOriginX, graphOriginX + Math.max(0, graphWidth - nodeW));
            targetY = Mth.clamp(targetY, graphOriginY, graphOriginY + Math.max(0, graphHeight - nodeH));

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
                int col = idxInWindow % GRAPH_COLS;
                int row = idxInWindow / GRAPH_COLS;
                int gapX = 6;
                int gapY = 4;
                int baseX = graphOriginX + col * (nodeW + gapX);
                int baseY = graphOriginY + row * (nodeH + gapY);
                layoutOffsets.put(draggingMethodId.toLowerCase(Locale.ROOT),
                        new int[]{targetX - baseX, targetY - baseY});
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
            draggingMethodId = "";
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
                if (selectedIndex < scroll) {
                    scroll = selectedIndex;
                } else if (selectedIndex >= scroll + LIST_VISIBLE) {
                    scroll = selectedIndex - LIST_VISIBLE + 1;
                }
                updateLearnButton();
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
                break;
            }
        }
        updateLearnButton();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int left = panelLeft();
        int top = panelTop();
        int listX = left + 12;
        int listY = top + 36;
        int listW = 188;
        int listH = LIST_VISIBLE * LINE + 4;
        if (mouseX >= listX - 2 && mouseX <= listX + listW + 2
                && mouseY >= listY - 2 && mouseY <= listY + listH) {
            scroll = Mth.clamp(scroll - (int) Math.signum(delta), 0, maxScroll());
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

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
