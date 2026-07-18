package com.xunxian.seekingimmortals.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Shared journal Screen base that owns the standard chrome pipeline:
 * {@code renderBackground → drawLayeredPanel → drawTitleBar + title → drawInnerFrame → content → super.render}.
 *
 * <p>Subclasses supply layout and content hooks; scrolling, tabs and row buttons are optional
 * collaborators ({@link ScrollableListPanel}, {@link TabBar}) rather than hard-coded here.
 * Material climate defaults to bamboo and may be overridden via {@link #defaultClimate()}.</p>
 */
public abstract class AbstractJournalScreen extends Screen {
    protected AbstractJournalScreen(Component title) {
        super(title);
    }

    /**
     * Panel + optional header/inner-frame rectangles used by the shared chrome pipeline.
     *
     * @param panelX      panel left
     * @param panelY      panel top
     * @param panelWidth  panel width
     * @param panelHeight panel height
     * @param header      title bar bounds; null skips title bar
     * @param innerFrame  content frame bounds; null skips inner frame
     * @param titleText   title drawn into the header; null uses {@link #getTitle()}
     */
    public record JournalChrome(int panelX, int panelY, int panelWidth, int panelHeight,
                                UiRect header, UiRect innerFrame, Component titleText) {
        public JournalChrome(int panelX, int panelY, int panelWidth, int panelHeight,
                             UiRect header, UiRect innerFrame) {
            this(panelX, panelY, panelWidth, panelHeight, header, innerFrame, null);
        }
    }

    /** Material climate for this screen family. Override per semantic routing. */
    protected UiClimate defaultClimate() {
        return UiClimate.BAMBOO_SLIP;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        ImmortalUiSkin.pushClimate(defaultClimate());
        try {
            renderBackground(graphics);
            JournalChrome chrome = journalChrome();
            if (chrome != null) {
                renderJournalChrome(graphics, chrome);
                renderJournalContent(graphics, chrome, mouseX, mouseY, partialTick);
            }
            super.render(graphics, mouseX, mouseY, partialTick);
            renderAfterWidgets(graphics, chrome, mouseX, mouseY, partialTick);
        } finally {
            ImmortalUiSkin.popClimate();
        }
    }

    /** Current chrome geometry for this frame. */
    protected abstract JournalChrome journalChrome();

    /** Screen-specific body drawn after chrome and before widgets. */
    protected abstract void renderJournalContent(GuiGraphics graphics, JournalChrome chrome,
                                                 int mouseX, int mouseY, float partialTick);

    /** Optional post-widget pass (tooltips, floating labels). Default is no-op. */
    protected void renderAfterWidgets(GuiGraphics graphics, JournalChrome chrome,
                                      int mouseX, int mouseY, float partialTick) {
    }

    protected void renderJournalChrome(GuiGraphics graphics, JournalChrome chrome) {
        ImmortalUiSkin.drawLayeredPanel(graphics, chrome.panelX(), chrome.panelY(),
                chrome.panelWidth(), chrome.panelHeight());
        if (chrome.header() != null) {
            UiRect header = chrome.header();
            ImmortalUiSkin.drawTitleBar(graphics, header.x(), header.y(), header.width(), header.height());
            renderJournalTitle(graphics, chrome, header);
        }
        if (chrome.innerFrame() != null) {
            UiRect frame = chrome.innerFrame();
            ImmortalUiSkin.drawInnerFrame(graphics, frame.x(), frame.y(), frame.width(), frame.height());
        }
    }

    protected void renderJournalTitle(GuiGraphics graphics, JournalChrome chrome, UiRect header) {
        Component title = chrome.titleText() != null ? chrome.titleText() : getTitle();
        String fitted = ImmortalUiSkin.fitWidth(font, title.getString(), Math.max(1, header.width() - 14));
        graphics.drawCenteredString(font, fitted,
                header.x() + header.width() / 2,
                header.y() + Math.max(2, (header.height() - 8) / 2),
                ImmortalUiSkin.JOURNAL_PAPER);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
