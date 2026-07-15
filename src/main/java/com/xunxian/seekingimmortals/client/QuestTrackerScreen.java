package com.xunxian.seekingimmortals.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;
import java.util.Optional;

/** Server-authoritative quest tracker with a scrollable journal view. */
public class QuestTrackerScreen extends Screen {
    private static final int DESIRED_WIDTH = 340;
    private static final int DESIRED_HEIGHT = 230;
    private static final int LINE_GAP = 2;

    private Button advanceButton;
    private Button righteousButton;
    private Button neutralButton;
    private Button demonicButton;
    private int scrollOffset;
    private int renderedContentHeight;

    public QuestTrackerScreen() {
        super(Component.translatable("screen.seeking_immortals.quest_tracker.title"));
    }

    @Override
    protected void init() {
        super.init();
        rebuildButtons();
    }

    /** Called when tracker data refreshes while this screen is open. */
    public void refreshWidgets() {
        clearWidgets();
        scrollOffset = 0;
        rebuildButtons();
    }

    private void rebuildButtons() {
        Layout layout = calculateLayout(width, height);
        List<Rect> buttons = layout.buttons();
        Optional<ClientQuestTrackerData.ChainLine> active = ClientQuestTrackerData.firstActiveChain();
        String chainId = active.map(ClientQuestTrackerData.ChainLine::id).orElse("");
        boolean canAct = active.isPresent() && !active.get().complete() && !chainId.isBlank();
        boolean locked = active.map(ClientQuestTrackerData.ChainLine::branchLocked).orElse(false);
        boolean canAfford = active.map(line -> line.costNeed() <= 0 || line.owned() >= line.costNeed()).orElse(false);

        addButton(buttons.get(0), Component.translatable("screen.seeking_immortals.quest_tracker.refresh"),
                button -> sendAction("sync"), false);
        advanceButton = addButton(buttons.get(1),
                Component.translatable("screen.seeking_immortals.quest_tracker.advance"), button -> {
                    if (!chainId.isBlank()) sendAction("advance:" + chainId);
                }, true);
        advanceButton.active = canAct && canAfford;
        righteousButton = branchButton(buttons.get(2), "righteous", chainId,
                Component.translatable("screen.seeking_immortals.quest_tracker.branch_righteous"), canAct && !locked);
        neutralButton = branchButton(buttons.get(3), "neutral", chainId,
                Component.translatable("screen.seeking_immortals.quest_tracker.branch_neutral"), canAct);
        demonicButton = branchButton(buttons.get(4), "demonic", chainId,
                Component.translatable("screen.seeking_immortals.quest_tracker.branch_demonic"), canAct && !locked);
        addButton(buttons.get(5), Component.translatable("gui.done"), button -> onClose(), false);
    }

    private Button branchButton(Rect rect, String branch, String chainId, Component label, boolean active) {
        Button button = addButton(rect, label, pressed -> {
            if (!chainId.isBlank()) sendAction("branch:" + chainId + ":" + branch);
        }, false);
        button.active = active && !chainId.isBlank();
        return button;
    }

    private Button addButton(Rect rect, Component label, Button.OnPress onPress, boolean primary) {
        Button button = primary
                ? ImmortalButton.primary(rect.x(), rect.y(), rect.width(), rect.height(), label, onPress)
                : ImmortalButton.secondary(rect.x(), rect.y(), rect.width(), rect.height(), label, onPress);
        addRenderableWidget(button);
        return button;
    }

    private void sendAction(String action) {
        com.xunxian.seekingimmortals.network.ModNetwork.CHANNEL.sendToServer(
                new com.xunxian.seekingimmortals.network.QuestTrackerActionPacket(action));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        Layout layout = calculateLayout(width, height);
        Rect panel = layout.panel();
        Rect titleBar = layout.titleBar();
        Rect viewport = layout.viewport();
        Rect hint = layout.hint();

        ImmortalUiSkin.drawLayeredPanel(graphics, panel.x(), panel.y(), panel.width(), panel.height());
        ImmortalUiSkin.drawTitleBar(graphics, titleBar.x(), titleBar.y(), titleBar.width(), titleBar.height());
        ImmortalUiSkin.drawStringFit(font, graphics, title.getString(),
                titleBar.x() + 6, titleBar.y() + Math.max(2, (titleBar.height() - font.lineHeight) / 2),
                Math.max(1, titleBar.width() - 12), ImmortalUiSkin.JOURNAL_BORDER, false);
        ImmortalUiSkin.drawInnerFrame(graphics, viewport.x(), viewport.y(), viewport.width(), viewport.height());

        int contentWidth = Math.max(1, viewport.width() - 10);
        List<String> lines = ClientQuestTrackerData.lines();
        renderedContentHeight = measureLines(lines, contentWidth);
        int visibleHeight = Math.max(1, viewport.height() - 6);
        scrollOffset = clampScroll(scrollOffset, renderedContentHeight, visibleHeight);
        ImmortalUiSkin.withScissor(graphics, viewport.x() + 1, viewport.y() + 1,
                Math.max(1, viewport.width() - 2), Math.max(1, viewport.height() - 2),
                () -> renderLines(graphics, lines, viewport.x() + 5,
                        viewport.y() + 3 - scrollOffset, contentWidth));
        ImmortalUiSkin.drawThinScrollbar(graphics, viewport.right() - 3, viewport.y() + 1,
                Math.max(1, viewport.height() - 2), renderedContentHeight, visibleHeight, scrollOffset);

        String hintText = activeHint();
        if (!hintText.isBlank()) {
            ImmortalUiSkin.withScissor(graphics, hint.x(), hint.y(), hint.width(), hint.height(), () ->
                    ImmortalUiSkin.drawWrappedText(font, graphics, hintText,
                            hint.x(), hint.y(), hint.width(), hint.height(),
                            ImmortalUiSkin.JOURNAL_PAPER_MUTED, false));
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        Rect viewport = calculateLayout(width, height).viewport();
        int visibleHeight = Math.max(1, viewport.height() - 6);
        if (viewport.contains(mouseX, mouseY) && renderedContentHeight > visibleHeight) {
            scrollOffset = clampScroll(scrollOffset - (int)Math.round(delta * 16.0D),
                    renderedContentHeight, visibleHeight);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    private int measureLines(List<String> lines, int contentWidth) {
        if (lines.isEmpty()) return font.lineHeight;
        int height = 0;
        for (String line : lines) {
            height += Math.max(1, font.split(Component.literal(line == null ? "" : line), contentWidth).size())
                    * (font.lineHeight + LINE_GAP);
        }
        return Math.max(1, height - LINE_GAP);
    }

    private void renderLines(GuiGraphics graphics, List<String> lines, int x, int y, int contentWidth) {
        if (lines.isEmpty()) {
            ImmortalUiSkin.drawStringFit(font, graphics,
                    Component.translatable("screen.seeking_immortals.quest_tracker.empty").getString(),
                    x, y, contentWidth, ImmortalUiSkin.JOURNAL_PAPER_MUTED, false);
            return;
        }
        int cursorY = y;
        for (String line : lines) {
            String safeLine = line == null ? "" : line;
            int color = safeLine.startsWith("OK ") ? ImmortalUiSkin.JOURNAL_JADE_TEXT
                    : safeLine.startsWith("ERR ") ? ImmortalUiSkin.JOURNAL_CINNABAR_BRIGHT
                    : ImmortalUiSkin.JOURNAL_PAPER;
            for (FormattedCharSequence sequence : font.split(Component.literal(safeLine), contentWidth)) {
                graphics.drawString(font, sequence, x, cursorY, color, false);
                cursorY += font.lineHeight + LINE_GAP;
            }
        }
    }

    private String activeHint() {
        Optional<ClientQuestTrackerData.ChainLine> active = ClientQuestTrackerData.firstActiveChain();
        if (active.isEmpty()) return "";
        ClientQuestTrackerData.ChainLine line = active.get();
        if (line.complete()) {
            return Component.translatable("screen.seeking_immortals.quest_tracker.hint_done", line.id()).getString();
        }
        if (line.costNeed() > 0 && line.owned() < line.costNeed()) {
            return Component.translatable("screen.seeking_immortals.quest_tracker.hint_cost",
                    line.costItem(), line.owned(), line.costNeed()).getString();
        }
        if (line.branchLocked()) {
            return Component.translatable("screen.seeking_immortals.quest_tracker.hint_locked", line.branch()).getString();
        }
        return Component.translatable("screen.seeking_immortals.quest_tracker.hint_ready", line.id()).getString();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    static Layout calculateLayout(int screenWidth, int screenHeight) {
        int safeWidth = Math.max(1, screenWidth);
        int safeHeight = Math.max(1, screenHeight);
        int margin = safeWidth < 160 || safeHeight < 110 ? 4 : 12;
        margin = Math.min(margin, Math.min((safeWidth - 1) / 2, (safeHeight - 1) / 2));
        int panelWidth = Math.max(1, Math.min(DESIRED_WIDTH, safeWidth - margin * 2));
        int panelHeight = Math.max(1, Math.min(DESIRED_HEIGHT, safeHeight - margin * 2));
        int left = Math.max(0, (safeWidth - panelWidth) / 2);
        int top = Math.max(0, (safeHeight - panelHeight) / 2);
        Rect panel = new Rect(left, top, panelWidth, panelHeight);
        int padding = panelWidth < 160 || panelHeight < 110 ? 4 : 10;
        int gap = panelHeight < 110 ? 2 : 6;
        int titleHeight = panelHeight < 110 ? 14 : 20;
        int frameInset = Math.min(4, Math.max(0, panelWidth - 1));
        Rect titleBar = new Rect(left + frameInset, top + Math.min(4, Math.max(0, panelHeight - 1)),
                Math.max(1, panelWidth - frameInset * 2), Math.min(titleHeight, panelHeight));

        boolean twoRows = panelWidth < 260;
        int buttonHeight = panelHeight < 110 ? 12 : 18;
        int buttonGap = twoRows ? Math.min(3, gap) : 4;
        int rows = twoRows ? 2 : 1;
        int footerHeight = rows * buttonHeight + (rows - 1) * buttonGap;
        int footerY = Math.max(top, panel.bottom() - padding - footerHeight);
        int hintHeight = Math.min(panelHeight < 110 ? 9 : 20,
                Math.max(1, footerY - titleBar.bottom() - gap * 2));
        Rect hint = new Rect(left + padding, Math.max(titleBar.bottom(), footerY - gap - hintHeight),
                Math.max(1, panelWidth - padding * 2), hintHeight);
        int viewportY = Math.min(hint.y(), titleBar.bottom() + gap);
        Rect viewport = new Rect(left + padding, viewportY,
                Math.max(1, panelWidth - padding * 2), Math.max(1, hint.y() - gap - viewportY));

        java.util.ArrayList<Rect> buttonRects = new java.util.ArrayList<>(6);
        if (twoRows) {
            int columnWidth = Math.max(1, (panelWidth - padding * 2 - buttonGap * 2) / 3);
            for (int index = 0; index < 6; index++) {
                int row = index / 3;
                int col = index % 3;
                int x = left + padding + col * (columnWidth + buttonGap);
                int width = col == 2
                        ? Math.max(1, panel.right() - padding - x) : columnWidth;
                buttonRects.add(new Rect(x, footerY + row * (buttonHeight + buttonGap), width, buttonHeight));
            }
        } else {
            int columnWidth = Math.max(1, (panelWidth - padding * 2 - buttonGap * 5) / 6);
            for (int index = 0; index < 6; index++) {
                int x = left + padding + index * (columnWidth + buttonGap);
                int width = index == 5
                        ? Math.max(1, panel.right() - padding - x) : columnWidth;
                buttonRects.add(new Rect(x, footerY, width, buttonHeight));
            }
        }
        return new Layout(panel, titleBar, viewport, hint, List.copyOf(buttonRects));
    }

    static int clampScroll(int offset, int contentHeight, int viewportHeight) {
        return Math.max(0, Math.min(offset, Math.max(0, contentHeight - Math.max(1, viewportHeight))));
    }

    record Layout(Rect panel, Rect titleBar, Rect viewport, Rect hint, List<Rect> buttons) {}

    record Rect(int x, int y, int width, int height) {
        int right() { return x + width; }
        int bottom() { return y + height; }
        boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX < right() && mouseY >= y && mouseY < bottom();
        }
        boolean intersects(Rect other) {
            return other != null && x < other.right() && right() > other.x
                    && y < other.bottom() && bottom() > other.y;
        }
        boolean inside(int screenWidth, int screenHeight) {
            return width > 0 && height > 0 && x >= 0 && y >= 0
                    && right() <= screenWidth && bottom() <= screenHeight;
        }
    }
}
