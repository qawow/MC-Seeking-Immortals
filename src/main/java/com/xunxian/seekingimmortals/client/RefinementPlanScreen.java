package com.xunxian.seekingimmortals.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

/** Refinement forge plan browser screen (Wave50 Phase13 depth). */
public class RefinementPlanScreen extends AbstractJournalScreen {
    private static final int DESIRED_WIDTH = 340;
    private static final int DESIRED_HEIGHT = 210;
    private static final int LINE_GAP = 2;

    private final List<String> lines = new ArrayList<>();
    private final ScrollableListPanel listPanel = new ScrollableListPanel();

    public RefinementPlanScreen(List<String> lines) {
        super(Component.translatable("screen.seeking_immortals.refine_plan.title"));
        if (lines != null) {
            this.lines.addAll(lines);
        }
        this.listPanel.setScrollStep(16)
                .setContentInsets(5, 3, 5, 0)
                .setScissorInsets(1, 1, 1, 1)
                .setScrollHeightReduce(6)
                .setScrollbarInsetRight(3)
                .setScrollbarTrackInsets(1, 1);
    }

    @Override
    protected void init() {
        super.init();
        Layout layout = calculateLayout(width, height);
        UiRect done = layout.doneButton();
        addRenderableWidget(ImmortalButton.secondary(done.x(), done.y(), done.width(), done.height(),
                Component.translatable("gui.done"), button -> onClose()));
    }

    @Override
    protected JournalChrome journalChrome() {
        Layout layout = calculateLayout(width, height);
        UiRect panel = layout.panel();
        return new JournalChrome(panel.x(), panel.y(), panel.width(), panel.height(),
                layout.titleBar(), layout.viewport());
    }

    @Override
    protected void renderJournalTitle(GuiGraphics graphics, JournalChrome chrome, UiRect header) {
        ImmortalUiSkin.drawStringFit(font, graphics, getTitle().getString(),
                header.x() + 6, header.y() + Math.max(2, (header.height() - font.lineHeight) / 2),
                Math.max(1, header.width() - 12), ImmortalUiSkin.JOURNAL_BORDER, false);
    }

    @Override
    protected void renderJournalContent(GuiGraphics graphics, JournalChrome chrome,
                                        int mouseX, int mouseY, float partialTick) {
        Layout layout = calculateLayout(width, height);
        UiRect viewport = layout.viewport();
        int contentWidth = Math.max(1, viewport.width() - 10);
        listPanel.setBounds(viewport)
                .setContentHeight(measureContent(contentWidth))
                .renderContent(graphics, (g, contentX, contentY, measuredWidth) ->
                        renderContent(g, contentX, contentY, measuredWidth));
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        Layout layout = calculateLayout(width, height);
        UiRect viewport = layout.viewport();
        int contentWidth = Math.max(1, viewport.width() - 10);
        listPanel.setBounds(viewport).setContentHeight(measureContent(contentWidth));
        if (listPanel.mouseScrolled(mouseX, mouseY, delta)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
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
        UiRect panel = new UiRect(left, top, panelWidth, panelHeight);

        int padding = panelWidth < 160 || panelHeight < 110 ? 5 : 12;
        int gap = panelHeight < 110 ? 3 : 6;
        int buttonHeight = panelHeight < 110 ? 14 : 20;
        int buttonWidth = Math.max(28, Math.min(58, panelWidth - padding * 2));
        int footerY = Math.max(top, panel.bottom() - padding - buttonHeight);
        UiRect done = new UiRect(Math.max(left, panel.right() - padding - buttonWidth), footerY,
                Math.min(buttonWidth, panelWidth), Math.min(buttonHeight, panelHeight));
        int titleHeight = Math.max(12, Math.min(20, panelHeight / 4));
        UiRect titleBar = new UiRect(left + Math.min(4, panelWidth - 1), top + Math.min(4, panelHeight - 1),
                Math.max(1, panelWidth - Math.min(8, panelWidth - 1)), Math.min(titleHeight, panelHeight));
        int viewportY = Math.min(footerY, titleBar.bottom() + gap);
        int viewportBottom = Math.max(viewportY + 1, footerY - gap);
        UiRect viewport = new UiRect(left + Math.min(padding, panelWidth - 1), viewportY,
                Math.max(1, panelWidth - Math.min(padding * 2, panelWidth - 1)),
                Math.max(1, viewportBottom - viewportY));
        return new Layout(panel, titleBar, viewport, done);
    }

    static int clampScroll(int offset, int contentHeight, int viewportHeight) {
        return ScrollableListPanel.clampScroll(offset, contentHeight, viewportHeight);
    }

    record Layout(UiRect panel, UiRect titleBar, UiRect viewport, UiRect doneButton) {}
}
