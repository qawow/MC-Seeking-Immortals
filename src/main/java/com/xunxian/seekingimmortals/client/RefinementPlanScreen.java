package com.xunxian.seekingimmortals.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** Refinement forge plan browser screen (Wave50 Phase13 depth). */
public class RefinementPlanScreen extends Screen {
    private static final int DESIRED_WIDTH = 340;
    private static final int DESIRED_HEIGHT = 210;
    private static final int LINE_GAP = 2;

    private final List<String> lines = new ArrayList<>();
    private int scrollOffset;
    private int renderedContentHeight;

    public RefinementPlanScreen(List<String> lines) {
        super(Component.translatable("screen.seeking_immortals.refine_plan.title"));
        if (lines != null) this.lines.addAll(lines);
    }

    @Override
    protected void init() {
        super.init();
        Layout layout = calculateLayout(width, height);
        Rect done = layout.doneButton();
        addRenderableWidget(ImmortalButton.secondary(done.x(), done.y(), done.width(), done.height(),
                Component.translatable("gui.done"), button -> onClose()));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        Layout layout = calculateLayout(width, height);
        Rect panel = layout.panel();
        Rect titleBar = layout.titleBar();
        Rect viewport = layout.viewport();

        ImmortalUiSkin.drawLayeredPanel(graphics, panel.x(), panel.y(), panel.width(), panel.height());
        ImmortalUiSkin.drawTitleBar(graphics, titleBar.x(), titleBar.y(), titleBar.width(), titleBar.height());
        ImmortalUiSkin.drawStringFit(font, graphics, title.getString(),
                titleBar.x() + 6, titleBar.y() + Math.max(2, (titleBar.height() - font.lineHeight) / 2),
                Math.max(1, titleBar.width() - 12), ImmortalUiSkin.JOURNAL_BORDER, false);
        ImmortalUiSkin.drawInnerFrame(graphics, viewport.x(), viewport.y(), viewport.width(), viewport.height());

        int contentWidth = Math.max(1, viewport.width() - 10);
        renderedContentHeight = measureContent(contentWidth);
        scrollOffset = clampScroll(scrollOffset, renderedContentHeight, Math.max(1, viewport.height() - 6));
        int startX = viewport.x() + 5;
        int startY = viewport.y() + 3 - scrollOffset;
        ImmortalUiSkin.withScissor(graphics, viewport.x() + 1, viewport.y() + 1,
                Math.max(1, viewport.width() - 2), Math.max(1, viewport.height() - 2),
                () -> renderContent(graphics, startX, startY, contentWidth));
        ImmortalUiSkin.drawThinScrollbar(graphics, viewport.right() - 3, viewport.y() + 1,
                Math.max(1, viewport.height() - 2), renderedContentHeight,
                Math.max(1, viewport.height() - 6), scrollOffset);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        Layout layout = calculateLayout(width, height);
        Rect viewport = layout.viewport();
        int visibleHeight = Math.max(1, viewport.height() - 6);
        int maxScroll = Math.max(0, renderedContentHeight - visibleHeight);
        if (viewport.contains(mouseX, mouseY) && maxScroll > 0) {
            scrollOffset = clampScroll(scrollOffset - (int)Math.round(delta * 16.0D),
                    renderedContentHeight, visibleHeight);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private int measureContent(int contentWidth) {
        if (lines.isEmpty()) {
            return font.lineHeight;
        }
        int height = 0;
        for (String line : lines) {
            height += Math.max(1, font.split(Component.literal(line == null ? "" : line), contentWidth).size())
                    * (font.lineHeight + LINE_GAP);
        }
        return Math.max(1, height - LINE_GAP);
    }

    private int renderContent(GuiGraphics graphics, int x, int y, int contentWidth) {
        if (lines.isEmpty()) {
            ImmortalUiSkin.drawStringFit(font, graphics,
                    Component.translatable("screen.seeking_immortals.refine_plan.empty").getString(),
                    x, y, contentWidth, ImmortalUiSkin.JOURNAL_PAPER_MUTED, false);
            return y + font.lineHeight;
        }
        int cursorY = y;
        for (String line : lines) {
            List<FormattedCharSequence> wrapped = font.split(
                    Component.literal(line == null ? "" : line), contentWidth);
            for (FormattedCharSequence sequence : wrapped) {
                graphics.drawString(font, sequence, x, cursorY, ImmortalUiSkin.JOURNAL_PAPER, false);
                cursorY += font.lineHeight + LINE_GAP;
            }
        }
        return cursorY;
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

        int padding = panelWidth < 160 || panelHeight < 110 ? 5 : 12;
        int gap = panelHeight < 110 ? 3 : 6;
        int buttonHeight = panelHeight < 110 ? 14 : 20;
        int buttonWidth = Math.max(28, Math.min(58, panelWidth - padding * 2));
        int footerY = Math.max(top, panel.bottom() - padding - buttonHeight);
        Rect done = new Rect(Math.max(left, panel.right() - padding - buttonWidth), footerY,
                Math.min(buttonWidth, panelWidth), Math.min(buttonHeight, panelHeight));
        int titleHeight = Math.max(12, Math.min(20, panelHeight / 4));
        Rect titleBar = new Rect(left + Math.min(4, panelWidth - 1), top + Math.min(4, panelHeight - 1),
                Math.max(1, panelWidth - Math.min(8, panelWidth - 1)), Math.min(titleHeight, panelHeight));
        int viewportY = Math.min(footerY, titleBar.bottom() + gap);
        int viewportBottom = Math.max(viewportY + 1, footerY - gap);
        Rect viewport = new Rect(left + Math.min(padding, panelWidth - 1), viewportY,
                Math.max(1, panelWidth - Math.min(padding * 2, panelWidth - 1)),
                Math.max(1, viewportBottom - viewportY));
        return new Layout(panel, titleBar, viewport, done);
    }

    static int clampScroll(int offset, int contentHeight, int viewportHeight) {
        return Math.max(0, Math.min(offset, Math.max(0, contentHeight - Math.max(1, viewportHeight))));
    }

    record Layout(Rect panel, Rect titleBar, Rect viewport, Rect doneButton) {}

    record Rect(int x, int y, int width, int height) {
        int right() { return x + width; }
        int bottom() { return y + height; }
        boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX < right() && mouseY >= y && mouseY < bottom();
        }
        boolean inside(int screenWidth, int screenHeight) {
            return width > 0 && height > 0 && x >= 0 && y >= 0
                    && right() <= screenWidth && bottom() <= screenHeight;
        }
    }
}
