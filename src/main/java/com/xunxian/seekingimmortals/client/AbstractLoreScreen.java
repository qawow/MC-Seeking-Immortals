package com.xunxian.seekingimmortals.client;

import com.xunxian.seekingimmortals.network.LoreScreenActionPacket;
import com.xunxian.seekingimmortals.network.ModNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Shared shell for the M16 lore family (bestiary / chronicle / compendium).
 *
 * <p>Owns the common 420x260 journal chrome, left-aligned title strip, refresh/close
 * footer helpers, {@link LoreScreenActionPacket} dispatch, and the narrow-screen
 * chrome-compression math used by the list+detail layouts. Subclasses keep their
 * own public {@code Layout}/{@code Rect} types for ScreenLayoutTest contracts and
 * only supply content-specific widgets and body rendering.</p>
 */
public abstract class AbstractLoreScreen extends AbstractJournalScreen {
    // Single source of truth: InkLayout.Spec.LORE (云笈墨卷 layout engine).
    protected static final int MAX_PANEL_W =
            com.xunxian.seekingimmortals.client.ui.InkLayout.Spec.LORE.maxWidth();
    protected static final int MAX_PANEL_H =
            com.xunxian.seekingimmortals.client.ui.InkLayout.Spec.LORE.maxHeight();
    protected static final int PANEL_MARGIN =
            com.xunxian.seekingimmortals.client.ui.InkLayout.Spec.LORE.margin();
    protected static final int STACKED_BREAKPOINT =
            com.xunxian.seekingimmortals.client.ui.InkLayout.Spec.LORE.stackedBreakpoint();
    protected static final int MIN_BODY_LINE = 10;
    private int detailScroll;
    private String detailSelectionKey = "";

    protected AbstractLoreScreen(Component title) {
        super(title);
    }

    /** Lore family is always bamboo slip (outer-sect notes / bestiary / chronicle). */
    @Override
    protected UiClimate defaultClimate() {
        return UiClimate.BAMBOO_SLIP;
    }

    /** Optional progress suffix drawn after the title (e.g. {@code "  3/12"}). */
    protected String loreTitleProgress() {
        return "";
    }

    /** Current panel bounds for the shared chrome pipeline. */
    protected abstract PanelBounds lorePanelBounds();

    /** Reload client-side mirrors after {@code SyncLoreUnlockPacket}. */
    public abstract void refreshFromSync();

    public record PanelBounds(int left, int top, int width, int height) {}

    @Override
    protected JournalChrome journalChrome() {
        PanelBounds panel = lorePanelBounds();
        // Header/inner-frame intentionally null: lore screens use a fixed title strip
        // and one or more content frames drawn by the subclass body.
        return new JournalChrome(panel.left(), panel.top(), panel.width(), panel.height(), null, null);
    }

    @Override
    protected void renderJournalChrome(GuiGraphics graphics, JournalChrome chrome) {
        ImmortalUiSkin.drawLayeredPanel(graphics, chrome.panelX(), chrome.panelY(),
                chrome.panelWidth(), chrome.panelHeight());
        ImmortalUiSkin.drawTitleBar(graphics, chrome.panelX() + 6, chrome.panelY() + 6,
                chrome.panelWidth() - 12, 16);
        ImmortalUiSkin.drawStringFit(font, graphics, getTitle().getString() + loreTitleProgress(),
                chrome.panelX() + 12, chrome.panelY() + 10, chrome.panelWidth() - 24,
                ImmortalUiSkin.JOURNAL_PAPER, false);
    }

    protected void sendLoreAction(String action) {
        ModNetwork.CHANNEL.sendToServer(new LoreScreenActionPacket(action == null ? "" : action));
    }

    protected void addRefreshAndClose(int refreshX, int refreshY, int refreshW, int refreshH,
                                      int closeX, int closeY, int closeW, int closeH,
                                      String refreshAction) {
        addRenderableWidget(ImmortalButton.secondary(refreshX, refreshY, refreshW, refreshH,
                Component.translatable("screen.seeking_immortals.lore.refresh"),
                button -> sendLoreAction(refreshAction)));
        addRenderableWidget(ImmortalButton.secondary(closeX, closeY, closeW, closeH,
                Component.translatable("gui.done"), button -> onClose()));
    }

    /**
     * Shared responsive chrome for bestiary/chronicle list+detail screens.
     * Compresses strip (filter/tab) then footer then title when the panel is too short
     * to host {@link #MIN_BODY_LINE} body rows (doubled when stacked).
     */
    protected static ListDetailChrome computeListDetailChrome(int width, int height) {
        int screenWidth = Math.max(1, width);
        int screenHeight = Math.max(1, height);
        int panelWidth = Math.min(MAX_PANEL_W, Math.max(1, screenWidth - Math.min(PANEL_MARGIN * 2, screenWidth - 1)));
        int panelHeight = Math.min(MAX_PANEL_H, Math.max(1, screenHeight - Math.min(PANEL_MARGIN * 2, screenHeight - 1)));
        int left = Math.max(0, (screenWidth - panelWidth) / 2);
        int top = Math.max(0, (screenHeight - panelHeight) / 2);
        int pad = Math.min(8, Math.max(1, panelWidth / 12));
        int innerX = left + pad;
        int innerWidth = Math.max(1, panelWidth - pad * 2);

        boolean stacked = panelWidth < STACKED_BREAKPOINT;
        int minContent = stacked ? MIN_BODY_LINE * 2 + 2 : MIN_BODY_LINE;
        int titleReserve = Math.min(18, Math.max(10, panelHeight / 6));
        int stripHeight = Math.max(1, Math.min(14, panelHeight / 8));
        int footerHeight = Math.max(1, Math.min(16, panelHeight / 7));
        int chrome = titleReserve + stripHeight + footerHeight + 6;
        if (chrome + minContent > panelHeight) {
            int deficit = chrome + minContent - panelHeight;
            int cutStrip = Math.min(Math.max(0, stripHeight - 1), deficit);
            stripHeight -= cutStrip;
            deficit -= cutStrip;
            int cutFooter = Math.min(Math.max(0, footerHeight - 1), deficit);
            footerHeight -= cutFooter;
            deficit -= cutFooter;
            titleReserve -= Math.min(Math.max(0, titleReserve - 8), deficit);
        }
        int stripY = top + titleReserve;
        int footerY = Math.min(top + panelHeight - footerHeight,
                Math.max(stripY + stripHeight + 2 + minContent,
                        top + panelHeight - footerHeight - Math.min(4, panelHeight / 12)));
        int contentY = stripY + stripHeight + 2;
        int contentHeight = Math.max(minContent, footerY - contentY - 2);
        if (contentY + contentHeight > footerY) {
            contentHeight = Math.max(minContent, footerY - contentY);
        }

        int listX;
        int listY;
        int listW;
        int listH;
        int detailX;
        int detailY;
        int detailW;
        int detailH;
        if (stacked) {
            int gap = Math.min(2, Math.max(0, contentHeight - MIN_BODY_LINE * 2));
            int listHeight = Math.max(MIN_BODY_LINE, (contentHeight - gap) / 2);
            int dY = contentY + listHeight + gap;
            int detailHeight = Math.max(MIN_BODY_LINE, contentY + contentHeight - dY);
            listX = innerX;
            listY = contentY;
            listW = innerWidth;
            listH = listHeight;
            detailX = innerX;
            detailY = dY;
            detailW = innerWidth;
            detailH = detailHeight;
        } else {
            int gap = Math.min(8, Math.max(0, innerWidth - 2));
            int listWidth = Math.max(1, Math.min(180, (innerWidth - gap) * 44 / 100));
            int dX = innerX + listWidth + gap;
            int detailWidth = Math.max(1, innerX + innerWidth - dX);
            listX = innerX;
            listY = contentY;
            listW = listWidth;
            listH = Math.max(MIN_BODY_LINE, contentHeight);
            detailX = dX;
            detailY = contentY;
            detailW = detailWidth;
            detailH = Math.max(MIN_BODY_LINE, contentHeight);
        }

        int footerGap = Math.min(4, Math.max(0, innerWidth - 2));
        int footerButtonWidth = Math.max(1, Math.min(70, (innerWidth - footerGap) / 2));
        return new ListDetailChrome(left, top, panelWidth, panelHeight, stacked,
                pad, innerX, innerWidth, stripY, stripHeight, contentY, contentHeight,
                footerY, footerHeight,
                listX, listY, listW, listH,
                detailX, detailY, detailW, detailH,
                innerX, footerY, footerButtonWidth, footerHeight,
                innerX + innerWidth - footerButtonWidth, footerY, footerButtonWidth, footerHeight);
    }

    /**
     * Raw list+detail chrome numbers. Subclasses pack these into their public Layout records
     * so ScreenLayoutTest field access stays stable.
     */
    public record ListDetailChrome(
            int left, int top, int panelWidth, int panelHeight, boolean stacked,
            int pad, int innerX, int innerWidth,
            int stripY, int stripHeight,
            int contentY, int contentHeight,
            int footerY, int footerHeight,
            int listX, int listY, int listW, int listH,
            int detailX, int detailY, int detailW, int detailH,
            int refreshX, int refreshY, int refreshW, int refreshH,
            int closeX, int closeY, int closeW, int closeH) {
    }

    /** Resets detail scrolling only when the selected lore entry actually changes. */
    protected void setDetailSelectionKey(String key) {
        String safe = key == null ? "" : key;
        if (!Objects.equals(detailSelectionKey, safe)) {
            detailSelectionKey = safe;
            detailScroll = 0;
        }
    }

    /** Handles wheel input for a detail pane and keeps the offset within measured content. */
    protected boolean scrollLoreDetail(double mouseX, double mouseY, UiRect detail,
                                       List<String> lines, double delta) {
        if (detail == null || !detail.contains(mouseX, mouseY)) {
            return false;
        }
        int contentHeight = measureWrappedDetail(lines, Math.max(1, detail.width() - 12));
        int visibleHeight = Math.max(1, detail.height() - 4);
        detailScroll = clampDetailScroll(detailScroll - (int) Math.round(delta * (font.lineHeight + 2)),
                contentHeight, visibleHeight);
        return true;
    }

    static int clampDetailScroll(int offset, int contentHeight, int viewportHeight) {
        return Math.max(0, Math.min(offset, Math.max(0, contentHeight - Math.max(1, viewportHeight))));
    }

    /** Draws a scrollable multi-line detail pane with the lore family's scissor insets. */
    protected void renderWrappedDetail(GuiGraphics graphics, int x, int y, int w, int h, List<String> lines) {
        List<FormattedCharSequence> wrapped = wrappedDetailLines(lines, Math.max(1, w - 12));
        int contentHeight = measureWrappedDetail(wrapped);
        int visibleHeight = Math.max(1, h - 4);
        detailScroll = clampDetailScroll(detailScroll, contentHeight, visibleHeight);
        ImmortalUiSkin.withScissor(graphics, x + 2, y + 2, Math.max(1, w - 4), Math.max(1, h - 4), () -> {
            int cursorY = y + 6 - detailScroll;
            for (FormattedCharSequence sequence : wrapped) {
                graphics.drawString(font, sequence, x + 6, cursorY, ImmortalUiSkin.JOURNAL_PAPER, false);
                cursorY += font.lineHeight + 2;
            }
        });
        ImmortalUiSkin.drawThinScrollbar(graphics, x + w - 3, y + 1, Math.max(1, h - 2),
                contentHeight, visibleHeight, detailScroll);
    }

    private int measureWrappedDetail(List<String> lines, int availableWidth) {
        return measureWrappedDetail(wrappedDetailLines(lines, availableWidth));
    }

    private int measureWrappedDetail(List<FormattedCharSequence> wrapped) {
        return Math.max(1, wrapped.size() * (font.lineHeight + 2));
    }

    private List<FormattedCharSequence> wrappedDetailLines(List<String> lines, int availableWidth) {
        List<FormattedCharSequence> wrapped = new ArrayList<>();
        if (lines != null) {
            for (String line : lines) {
                List<FormattedCharSequence> split = font.split(
                        Component.literal(line == null ? "" : line), Math.max(1, availableWidth));
                if (split.isEmpty()) {
                    wrapped.add(Component.empty().getVisualOrderText());
                } else {
                    wrapped.addAll(split);
                }
            }
        }
        if (wrapped.isEmpty()) {
            wrapped.add(Component.empty().getVisualOrderText());
        }
        return List.copyOf(wrapped);
    }

    protected static UiRect toUiRect(int x, int y, int w, int h) {
        return new UiRect(x, y, w, h);
    }
}
