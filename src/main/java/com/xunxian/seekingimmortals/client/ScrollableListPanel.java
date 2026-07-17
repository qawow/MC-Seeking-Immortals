package com.xunxian.seekingimmortals.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.IntFunction;

/**
 * Reusable journal list viewport: private scroll state, scissor clipping, thin scrollbar,
 * optional list-row painting and visible-row ImmortalButton rebuild.
 */
public final class ScrollableListPanel {
    @FunctionalInterface
    public interface ContentRenderer {
        void render(GuiGraphics graphics, int contentX, int contentY, int contentWidth);
    }

    @FunctionalInterface
    public interface RowRenderer {
        void render(GuiGraphics graphics, int rowIndex, UiRect rowBounds,
                    ImmortalUiSkin.InteractionState state, boolean hovered);
    }

    private int x;
    private int y;
    private int width;
    private int height;
    private int contentHeight;
    private int scrollOffset;
    private int scrollStep = 18;
    private int contentInsetLeft = 0;
    private int contentInsetTop = 0;
    private int contentInsetRight = 0;
    private int contentInsetBottom = 0;
    private int scrollbarInsetRight = 3;
    private int scissorInsetLeft;
    private int scissorInsetTop;
    private int scissorInsetRight;
    private int scissorInsetBottom;
    private int scrollHeightReduce;
    private int scrollbarTrackTopInset;
    private int scrollbarTrackBottomInset;
    private int rowHeight = 26;
    private int rowGap = 0;

    public ScrollableListPanel() {
    }

    public ScrollableListPanel setBounds(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = Math.max(0, width);
        this.height = Math.max(0, height);
        return this;
    }

    public ScrollableListPanel setBounds(UiRect bounds) {
        if (bounds == null) {
            return setBounds(0, 0, 0, 0);
        }
        return setBounds(bounds.x(), bounds.y(), bounds.width(), bounds.height());
    }

    public ScrollableListPanel setContentHeight(int contentHeight) {
        this.contentHeight = Math.max(0, contentHeight);
        return this;
    }

    public ScrollableListPanel setScrollStep(int scrollStep) {
        this.scrollStep = Math.max(1, scrollStep);
        return this;
    }

    public ScrollableListPanel setContentInsets(int left, int top, int right, int bottom) {
        this.contentInsetLeft = Math.max(0, left);
        this.contentInsetTop = Math.max(0, top);
        this.contentInsetRight = Math.max(0, right);
        this.contentInsetBottom = Math.max(0, bottom);
        return this;
    }

    public ScrollableListPanel setScrollbarInsetRight(int inset) {
        this.scrollbarInsetRight = Math.max(0, inset);
        return this;
    }

    /**
     * Shrinks the scissor rectangle inside {@link #setBounds} without moving content insets,
     * matching the common journal pattern of a 1px inner clip.
     */
    public ScrollableListPanel setScissorInsets(int left, int top, int right, int bottom) {
        this.scissorInsetLeft = Math.max(0, left);
        this.scissorInsetTop = Math.max(0, top);
        this.scissorInsetRight = Math.max(0, right);
        this.scissorInsetBottom = Math.max(0, bottom);
        return this;
    }

    /**
     * Reduces the height used for scroll clamping and scrollbar thumb math.
     * Used when content has top/bottom padding so max-scroll is contentHeight - (viewport - pad).
     */
    public ScrollableListPanel setScrollHeightReduce(int pixels) {
        this.scrollHeightReduce = Math.max(0, pixels);
        return this;
    }

    /** Insets the thin scrollbar track from the panel top/bottom edges. */
    public ScrollableListPanel setScrollbarTrackInsets(int top, int bottom) {
        this.scrollbarTrackTopInset = Math.max(0, top);
        this.scrollbarTrackBottomInset = Math.max(0, bottom);
        return this;
    }

    public ScrollableListPanel setRowMetrics(int rowHeight, int rowGap) {
        this.rowHeight = Math.max(1, rowHeight);
        this.rowGap = Math.max(0, rowGap);
        return this;
    }

    public int x() {
        return x;
    }

    public int y() {
        return y;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public int contentHeight() {
        return contentHeight;
    }

    public int scrollOffset() {
        return scrollOffset;
    }

    public int rowHeight() {
        return rowHeight;
    }

    public int rowStride() {
        return rowHeight + rowGap;
    }

    public void setScrollOffset(int scrollOffset) {
        this.scrollOffset = clampScroll(scrollOffset, contentHeight, scrollViewportHeight());
    }

    public void resetScroll() {
        this.scrollOffset = 0;
    }

    public void clampToViewport() {
        this.scrollOffset = clampScroll(scrollOffset, contentHeight, scrollViewportHeight());
    }

    public boolean contains(double mouseX, double mouseY) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    public int contentX() {
        return x + contentInsetLeft;
    }

    public int contentY() {
        return y + contentInsetTop - scrollOffset;
    }

    public int contentWidth() {
        return Math.max(1, width - contentInsetLeft - contentInsetRight);
    }

    /** Height used for max-scroll and scrollbar thumb math (bounds height minus optional pad). */
    public int scrollViewportHeight() {
        return Math.max(1, height - scrollHeightReduce);
    }

    public int maxScroll() {
        return Math.max(0, contentHeight - scrollViewportHeight());
    }

    public int visibleRowCount() {
        if (height <= 0) {
            return 0;
        }
        return Math.max(1, height / Math.max(1, rowStride()));
    }

    public int firstVisibleRow() {
        return Math.max(0, scrollOffset / Math.max(1, rowStride()));
    }

    public void setContentRows(int rowCount) {
        setContentHeight(Math.max(0, rowCount) * rowStride());
    }

    public void setScrollRows(int firstRow) {
        setScrollOffset(Math.max(0, firstRow) * rowStride());
    }

    public int scrollRows() {
        return firstVisibleRow();
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int viewportHeight = scrollViewportHeight();
        if (!contains(mouseX, mouseY) || contentHeight <= viewportHeight || delta == 0.0D) {
            return false;
        }
        int next = clampScroll(scrollOffset - (int) Math.round(delta * scrollStep),
                contentHeight, viewportHeight);
        if (next == scrollOffset) {
            return true;
        }
        scrollOffset = next;
        return true;
    }

    /** Pixel-step scroll used by row lists that step by one row per notch. */
    public boolean mouseScrolledRows(double mouseX, double mouseY, double delta, int itemCount) {
        if (!contains(mouseX, mouseY) || delta == 0.0D) {
            return false;
        }
        int visible = visibleRowCount();
        int maxFirst = Math.max(0, itemCount - visible);
        int next = Mth.clamp(firstVisibleRow() - (int) Math.signum(delta), 0, maxFirst);
        setScrollRows(next);
        return true;
    }

    public void renderContent(GuiGraphics graphics, ContentRenderer renderer) {
        Objects.requireNonNull(renderer, "renderer");
        clampToViewport();
        withScissorRegion(graphics, () ->
                renderer.render(graphics, contentX(), contentY(), contentWidth()));
        drawScrollbar(graphics);
    }

    public void renderRows(GuiGraphics graphics, int itemCount, int mouseX, int mouseY,
                           RowRenderer rowRenderer) {
        Objects.requireNonNull(rowRenderer, "rowRenderer");
        setContentRows(itemCount);
        clampToViewport();
        int visible = visibleRowCount();
        int first = firstVisibleRow();
        int hovered = hoveredRow(mouseX, mouseY, itemCount);
        withScissorRegion(graphics, () -> {
            for (int row = 0; row < visible && first + row < itemCount; row++) {
                int index = first + row;
                UiRect bounds = rowBounds(row);
                boolean isHovered = hovered == row;
                ImmortalUiSkin.InteractionState state = isHovered
                        ? ImmortalUiSkin.InteractionState.HOVERED
                        : ImmortalUiSkin.InteractionState.NORMAL;
                ImmortalUiSkin.drawListRow(graphics, bounds.x(), bounds.y(), bounds.width(), bounds.height(), state);
                rowRenderer.render(graphics, index, bounds, state, isHovered);
            }
        });
        drawScrollbar(graphics);
    }

    private void withScissorRegion(GuiGraphics graphics, Runnable renderer) {
        int scissorX = x + scissorInsetLeft;
        int scissorY = y + scissorInsetTop;
        int scissorW = Math.max(1, width - scissorInsetLeft - scissorInsetRight);
        int scissorH = Math.max(1, height - scissorInsetTop - scissorInsetBottom);
        ImmortalUiSkin.withScissor(graphics, scissorX, scissorY, scissorW, scissorH, renderer);
    }

    public UiRect rowBounds(int visibleRowIndex) {
        int stride = rowStride();
        int rowY = y + visibleRowIndex * stride;
        return new UiRect(x, rowY, width, rowHeight);
    }

    public int hoveredRow(int mouseX, int mouseY, int itemCount) {
        if (!contains(mouseX, mouseY)) {
            return -1;
        }
        int visible = visibleRowCount();
        int first = firstVisibleRow();
        int local = (mouseY - y) / Math.max(1, rowStride());
        if (local < 0 || local >= visible) {
            return -1;
        }
        int index = first + local;
        return index < itemCount ? local : -1;
    }

    /**
     * Rebuilds ImmortalButtons for currently visible rows. Caller owns widget lifecycle
     * (typically clearWidgets first, then attach the returned buttons).
     */
    public List<ImmortalButton> rebuildVisibleButtons(int itemCount,
                                                      IntFunction<Component> labelForIndex,
                                                      IntFunction<Boolean> enabledForIndex,
                                                      IntFunction<Runnable> pressForIndex,
                                                      IntFunction<UiRect> actionBoundsForVisibleRow,
                                                      boolean primary) {
        List<ImmortalButton> buttons = new ArrayList<>();
        int visible = visibleRowCount();
        int first = firstVisibleRow();
        for (int row = 0; row < visible && first + row < itemCount; row++) {
            int index = first + row;
            UiRect bounds = actionBoundsForVisibleRow.apply(row);
            if (bounds == null || bounds.width() <= 0 || bounds.height() <= 0) {
                continue;
            }
            Component label = labelForIndex.apply(index);
            Runnable press = pressForIndex.apply(index);
            ImmortalButton button = primary
                    ? ImmortalButton.primary(bounds.x(), bounds.y(), bounds.width(), bounds.height(),
                    label, ignored -> {
                        if (press != null) {
                            press.run();
                        }
                    })
                    : ImmortalButton.secondary(bounds.x(), bounds.y(), bounds.width(), bounds.height(),
                    label, ignored -> {
                        if (press != null) {
                            press.run();
                        }
                    });
            Boolean enabled = enabledForIndex.apply(index);
            button.active = enabled == null || enabled;
            buttons.add(button);
        }
        return buttons;
    }

    public void attachButtons(Consumer<GuiEventListener> addWidget, List<ImmortalButton> buttons) {
        if (addWidget == null || buttons == null) {
            return;
        }
        for (ImmortalButton button : buttons) {
            addWidget.accept(button);
        }
    }

    public void drawScrollbar(GuiGraphics graphics) {
        int trackY = y + scrollbarTrackTopInset;
        int trackHeight = Math.max(1, height - scrollbarTrackTopInset - scrollbarTrackBottomInset);
        ImmortalUiSkin.drawThinScrollbar(graphics, x + width - scrollbarInsetRight, trackY, trackHeight,
                contentHeight, scrollViewportHeight(), scrollOffset);
    }

    public static int clampScroll(int requested, int contentHeight, int viewportHeight) {
        int maximum = Math.max(0, contentHeight - Math.max(1, viewportHeight));
        return Math.max(0, Math.min(requested, maximum));
    }
}
