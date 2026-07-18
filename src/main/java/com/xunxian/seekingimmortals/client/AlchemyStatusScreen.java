package com.xunxian.seekingimmortals.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.Locale;

/** Compact journal view of the player's alchemy progression. */
public class AlchemyStatusScreen extends AbstractJournalScreen {
    private static final int PANEL_MARGIN = 4;
    private static final int DEFAULT_WIDTH = 300;
    private static final int DEFAULT_HEIGHT = 190;
    private static final int CONTENT_HEIGHT = 142;

    private final int skillLevel;
    private final int skillExp;
    private final String lastMessage;
    private final ScrollableListPanel listPanel = new ScrollableListPanel();


    @Override
    protected UiClimate defaultClimate() {
        return UiClimate.BAMBOO_SLIP;
    }

    public AlchemyStatusScreen(int skillLevel, int skillExp, String lastMessage) {
        super(Component.translatable("screen.seeking_immortals.alchemy.title"));
        this.skillLevel = skillLevel;
        this.skillExp = skillExp;
        this.lastMessage = lastMessage == null ? "" : lastMessage;
        this.listPanel.setScrollStep(18)
                .setContentInsets(7, 6, 7, 0)
                .setScrollbarInsetRight(3)
                .setContentHeight(CONTENT_HEIGHT);
    }

    @Override
    protected void init() {
        super.init();
        StatusLayout layout = calculateLayout(width, height);
        addRenderableWidget(ImmortalButton.secondary(layout.closeButton().x(), layout.closeButton().y(),
                layout.closeButton().width(), layout.closeButton().height(),
                Component.translatable("gui.done"), button -> onClose()));
    }

    @Override
    protected JournalChrome journalChrome() {
        StatusLayout layout = calculateLayout(width, height);
        return new JournalChrome(layout.left(), layout.top(), layout.panelWidth(), layout.panelHeight(),
                layout.header(), layout.viewport());
    }

    @Override
    protected void renderJournalContent(GuiGraphics graphics, JournalChrome chrome,
                                        int mouseX, int mouseY, float partialTick) {
        StatusLayout layout = calculateLayout(width, height);
        listPanel.setBounds(layout.viewport())
                .setContentHeight(CONTENT_HEIGHT)
                .renderContent(graphics, (g, contentX, contentY, contentWidth) ->
                        renderContent(g, contentX, contentY, contentWidth));
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        StatusLayout layout = calculateLayout(width, height);
        listPanel.setBounds(layout.viewport()).setContentHeight(CONTENT_HEIGHT);
        if (listPanel.mouseScrolled(mouseX, mouseY, delta)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    static StatusLayout calculateLayout(int screenWidth, int screenHeight) {
        int panelWidth = Math.min(DEFAULT_WIDTH, Math.max(1, screenWidth - PANEL_MARGIN * 2));
        int panelHeight = Math.min(DEFAULT_HEIGHT, Math.max(1, screenHeight - PANEL_MARGIN * 2));
        int left = Math.max(0, (screenWidth - panelWidth) / 2);
        int top = Math.max(0, (screenHeight - panelHeight) / 2);
        int padding = panelWidth >= 160 ? 9 : 3;
        int headerHeight = panelHeight >= 100 ? 28 : 16;
        int buttonHeight = Math.min(20, Math.max(10, panelHeight / 7));
        int buttonWidth = Math.min(62, Math.max(1, panelWidth - padding * 2));
        int buttonY = Math.max(top, top + panelHeight - padding - buttonHeight);
        int viewportY = Math.min(buttonY, top + headerHeight + 4);
        int viewportBottom = Math.max(viewportY + 1, buttonY - 5);
        UiRect header = new UiRect(left + padding, top + 4, Math.max(1, panelWidth - padding * 2),
                Math.max(1, headerHeight - 4));
        UiRect viewport = new UiRect(left + padding, viewportY, Math.max(1, panelWidth - padding * 2),
                Math.max(1, viewportBottom - viewportY));
        UiRect close = new UiRect(left + panelWidth - padding - buttonWidth, buttonY, buttonWidth,
                Math.min(buttonHeight, Math.max(1, top + panelHeight - buttonY)));
        return new StatusLayout(left, top, panelWidth, panelHeight, header, viewport, close);
    }

    static int clampScroll(int requested, int contentHeight, int viewportHeight) {
        return ScrollableListPanel.clampScroll(requested, contentHeight, viewportHeight);
    }

    private void renderContent(GuiGraphics graphics, int x, int y, int width) {
        double levelFraction = skillLevel / 10.0D;
        y = meter(graphics, x, y, width,
                Component.translatable("screen.seeking_immortals.alchemy.level", skillLevel, 10),
                levelFraction, ImmortalUiSkin.StatusBarStyle.CULTIVATION);
        int nextExperience = 100 + Math.max(0, skillLevel) * 50;
        y = meter(graphics, x, y, width,
                Component.translatable("screen.seeking_immortals.alchemy.exp", skillExp),
                nextExperience <= 0 ? 0.0D : skillExp / (double)nextExperience,
                ImmortalUiSkin.StatusBarStyle.SPIRIT);
        double bonus = Math.min(0.20D, Math.max(0, skillLevel) * 0.02D);
        y = meter(graphics, x, y, width,
                Component.translatable("screen.seeking_immortals.alchemy.bonus",
                        String.format(Locale.ROOT, "%.0f%%", bonus * 100.0D)),
                bonus / 0.20D, ImmortalUiSkin.StatusBarStyle.WARNING);
        y += 4;
        ImmortalUiSkin.drawHorizontalDivider(graphics, x, y, width);
        y += 6;
        y = ImmortalUiSkin.drawWrappedText(font, graphics,
                Component.translatable("screen.seeking_immortals.alchemy.howto"),
                x, y, width, 28, ImmortalUiSkin.JOURNAL_PAPER_MUTED, false);
        if (!lastMessage.isBlank()) {
            y += 5;
            ImmortalUiSkin.drawListRow(graphics, x, y, width, 30,
                    ImmortalUiSkin.InteractionState.SELECTED);
            ImmortalUiSkin.drawWrappedText(font, graphics, lastMessage,
                    x + 5, y + 5, Math.max(1, width - 10), 20,
                    ImmortalUiSkin.JOURNAL_JADE_TEXT, false);
        }
    }

    private int meter(GuiGraphics graphics, int x, int y, int width, Component label,
                      double fraction, ImmortalUiSkin.StatusBarStyle style) {
        ImmortalUiSkin.drawStringFit(font, graphics, label.getString(), x + 2, y,
                Math.max(1, width - 4), ImmortalUiSkin.JOURNAL_PAPER, false);
        ImmortalUiSkin.drawSemanticStatusBar(graphics, x, y + 12, width, 7, fraction, style);
        return y + 25;
    }

    record StatusLayout(int left, int top, int panelWidth, int panelHeight,
                        UiRect header, UiRect viewport, UiRect closeButton) {}
}
