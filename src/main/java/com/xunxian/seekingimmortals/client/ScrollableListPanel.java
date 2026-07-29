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
    private static final int DRAG_THRESHOLD = 4;

    @FunctionalInterface
    public interface ContentRenderer {
        void render(GuiGraphics graphics, int contentX, int contentY, int contentWidth);
    }

    @FunctionalInterface
    public interface RowRenderer {
        void render(GuiGraphics graphics, int rowIndex, UiRect rowBounds,
                    ImmortalUiSkin.InteractionState state, boolean hovered);
    }

    /** Input lifecycle for content drags, scrollbar paging, and thumb dragging. */
    public enum PointerState {
        IDLE,
        PENDING_ROW,
        PENDING_TRACK,
        DRAG_CONTENT,
        DRAG_THUMB
    }

    /** Result of a left-button release. Row indexes are absolute list indexes. */
    public record ReleaseResult(boolean consumed, int clickedRow) {
        static ReleaseResult ignored() {
            return new ReleaseResult(false, -1);
        }

        public boolean hasRowClick() {
            return clickedRow >= 0;
        }
    }

    private int x;
    private int y;
    private int width;
    private int height;
    private int contentHeight;
    private int scrollOffset;
    private int rowCount = -1;
    private int rowScroll;
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
    private PointerState pointerState = PointerState.IDLE;
    private double pressX;
    private double pressY;
    private int scrollOffsetAtPress;
    private int pendingRow = -1;
    private int thumbGrabOffset;
    private boolean pressedThumb;

    public ScrollableListPanel() {
    }

    public ScrollableListPanel setBounds(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = Math.max(0, width);
        this.height = Math.max(0, height);
        clampToViewport();
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
        this.rowCount = -1;
        this.rowScroll = 0;
        clampToViewport();
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
        clampToViewport();
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
        clampToViewport();
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
        if (isRowMode()) {
            this.contentHeight = rowCount * rowStride();
        }
        clampToViewport();
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

    public PointerState pointerState() {
        return pointerState;
    }

    public int rowHeight() {
        return rowHeight;
    }

    public int rowStride() {
        return rowHeight + rowGap;
    }

    public void setScrollOffset(int scrollOffset) {
        if (isRowMode()) {
            setScrollRows((int) Math.round(scrollOffset / (double) Math.max(1, rowStride())));
            return;
        }
        this.scrollOffset = clampScroll(scrollOffset, contentHeight, scrollViewportHeight());
    }

    public void resetScroll() {
        this.scrollOffset = 0;
        this.rowScroll = 0;
        clearPointerState();
    }

    public void clampToViewport() {
        if (isRowMode()) {
            rowScroll = Mth.clamp(rowScroll, 0, maxRowScroll());
            scrollOffset = rowScroll * rowStride();
            return;
        }
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
        if (isRowMode()) {
            return maxRowScroll() * rowStride();
        }
        return Math.max(0, contentHeight - scrollViewportHeight());
    }

    public int visibleRowCount() {
        if (height <= 0) {
            return 0;
        }
        return Math.max(1, rowViewportHeight() / Math.max(1, rowStride()));
    }

    public int firstVisibleRow() {
        if (isRowMode()) {
            return rowScroll;
        }
        return Math.max(0, scrollOffset / Math.max(1, rowStride()));
    }

    public void setContentRows(int rowCount) {
        this.rowCount = Math.max(0, rowCount);
        this.contentHeight = this.rowCount * rowStride();
        clampToViewport();
    }

    public void setScrollRows(int firstRow) {
        if (!isRowMode()) {
            setScrollOffset(Math.max(0, firstRow) * rowStride());
            return;
        }
        rowScroll = Mth.clamp(Math.max(0, firstRow), 0, maxRowScroll());
        scrollOffset = rowScroll * rowStride();
    }

    public int maxRowScroll() {
        if (!isRowMode()) {
            return Math.max(0, contentHeight / Math.max(1, rowStride()) - visibleRowCount());
        }
        return Math.max(0, rowCount - visibleRowCount());
    }

    private boolean isRowMode() {
        return rowCount >= 0;
    }

    private int rowViewportHeight() {
        return Math.max(1, height - contentInsetTop - contentInsetBottom);
    }

    private int scrollbarContentHeight() {
        if (!isRowMode()) {
            return contentHeight;
        }
        return scrollbarViewportHeight() + maxRowScroll() * rowStride();
    }

    private int scrollbarViewportHeight() {
        return isRowMode() ? rowViewportHeight() : scrollViewportHeight();
    }

    private boolean canScroll() {
        return maxScroll() > 0;
    }

    private void clearPointerState() {
        pointerState = PointerState.IDLE;
        pressX = 0.0D;
        pressY = 0.0D;
        scrollOffsetAtPress = 0;
        pendingRow = -1;
        thumbGrabOffset = 0;
        pressedThumb = false;
    }

    private void capturePress(double mouseX, double mouseY, int row) {
        pressX = mouseX;
        pressY = mouseY;
        scrollOffsetAtPress = scrollOffset;
        pendingRow = row;
    }

    private boolean crossedDragThreshold(double mouseX, double mouseY) {
        return Math.max(Math.abs(mouseX - pressX), Math.abs(mouseY - pressY)) >= DRAG_THRESHOLD;
    }

    private void dragContentTo(double mouseY) {
        int target = scrollOffsetAtPress + (int) Math.round(pressY - mouseY);
        setScrollOffset(target);
    }

    private void dragThumbTo(double mouseY) {
        UiRect track = scrollbarTrackBounds();
        UiRect thumb = scrollbarThumbBounds();
        int travel = Math.max(0, track.height() - thumb.height());
        if (track.width() <= 0 || travel <= 0) {
            return;
        }
        int thumbY = Mth.clamp((int) Math.round(mouseY) - thumbGrabOffset,
                track.y(), track.bottom() - thumb.height());
        double progress = (thumbY - track.y()) / (double) travel;
        setScrollOffset((int) Math.round(progress * maxScroll()));
    }

    private void pageTrack(double mouseY, UiRect thumb) {
        int direction = mouseY < thumb.y() ? -1 : 1;
        setScrollOffset(scrollOffset + direction * scrollbarViewportHeight());
    }

    public UiRect scrollbarTrackBounds() {
        if (!canScroll()) {
            return new UiRect(0, 0, 0, 0);
        }
        int outerY = y + scrollbarTrackTopInset;
        int outerHeight = Math.max(1, height - scrollbarTrackTopInset - scrollbarTrackBottomInset);
        int padding = outerHeight >= 8 ? 2 : 0;
        return new UiRect(x + width - scrollbarInsetRight, outerY + padding, 2,
                Math.max(1, outerHeight - padding * 2));
    }

    public UiRect scrollbarThumbBounds() {
        UiRect track = scrollbarTrackBounds();
        int visualContentHeight = scrollbarContentHeight();
        int viewportHeight = scrollbarViewportHeight();
        if (track.width() <= 0 || visualContentHeight <= viewportHeight) {
            return new UiRect(0, 0, 0, 0);
        }
        int thumbHeight = Math.max(Math.min(12, track.height()),
                (int) Math.round(track.height() * (viewportHeight / (double) visualContentHeight)));
        thumbHeight = Math.min(track.height(), thumbHeight);
        int travel = Math.max(0, track.height() - thumbHeight);
        int thumbY = track.y() + (int) Math.round(travel * (scrollOffset / (double) Math.max(1, maxScroll())));
        return new UiRect(track.x(), thumbY, track.width(), thumbHeight);
    }

    private int rowIndexAt(double mouseX, double mouseY, int itemCount) {
        if (itemCount <= 0 || mouseX < contentX() || mouseX >= contentX() + contentWidth()) {
            return -1;
        }
        int contentTop = y + contentInsetTop;
        int contentBottom = y + height - contentInsetBottom;
        if (mouseY < contentTop || mouseY >= contentBottom) {
            return -1;
        }
        int localY = (int) Math.floor(mouseY) - contentTop;
        int logicalY = isRowMode() ? localY : localY + scrollOffset;
        int stride = Math.max(1, rowStride());
        int localRow = logicalY / stride;
        if (logicalY % stride >= rowHeight) {
            return -1;
        }
        int index = isRowMode() ? rowScroll + localRow : localRow;
        return index >= 0 && index < itemCount ? index : -1;
    }

    public int scrollRows() {
        return firstVisibleRow();
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (!contains(mouseX, mouseY) || delta == 0.0D) {
            return false;
        }
        clearPointerState();
        if (!canScroll()) {
            return false;
        }
        int next = Mth.clamp(scrollOffset - (int) Math.round(delta * scrollStep), 0, maxScroll());
        setScrollOffset(next);
        return true;
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || !contains(mouseX, mouseY)) {
            return false;
        }
        UiRect track = scrollbarTrackBounds();
        if (track.contains(mouseX, mouseY)) {
            UiRect thumb = scrollbarThumbBounds();
            capturePress(mouseX, mouseY, -1);
            pointerState = PointerState.PENDING_TRACK;
            pressedThumb = thumb.contains(mouseX, mouseY);
            if (pressedThumb) {
                thumbGrabOffset = (int) Math.floor(mouseY) - thumb.y();
            } else {
                pageTrack(mouseY, thumb);
            }
            return true;
        }
        int row = isRowMode() ? rowIndexAt(mouseX, mouseY, rowCount) : -1;
        if (row < 0 && !canScroll()) {
            return false;
        }
        capturePress(mouseX, mouseY, row);
        pointerState = PointerState.PENDING_ROW;
        return true;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button != 0 || pointerState == PointerState.IDLE) {
            return false;
        }
        if (pointerState == PointerState.PENDING_ROW) {
            if (crossedDragThreshold(mouseX, mouseY)) {
                pointerState = PointerState.DRAG_CONTENT;
                dragContentTo(mouseY);
            }
            return true;
        }
        if (pointerState == PointerState.DRAG_CONTENT) {
            dragContentTo(mouseY);
            return true;
        }
        if (pointerState == PointerState.PENDING_TRACK) {
            if (pressedThumb && crossedDragThreshold(mouseX, mouseY)) {
                pointerState = PointerState.DRAG_THUMB;
                dragThumbTo(mouseY);
            }
            return true;
        }
        if (pointerState == PointerState.DRAG_THUMB) {
            dragThumbTo(mouseY);
            return true;
        }
        return false;
    }

    public ReleaseResult mouseReleasedResult(double mouseX, double mouseY, int button) {
        if (button != 0 || pointerState == PointerState.IDLE) {
            return ReleaseResult.ignored();
        }
        int clickedRow = -1;
        if (pointerState == PointerState.PENDING_ROW && pendingRow >= 0 && isRowMode()
                && rowIndexAt(mouseX, mouseY, rowCount) == pendingRow) {
            clickedRow = pendingRow;
        }
        clearPointerState();
        return new ReleaseResult(true, clickedRow);
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return mouseReleasedResult(mouseX, mouseY, button).consumed();
    }

    /** Pixel-step scroll used by row lists that step by one row per notch. */
    public boolean mouseScrolledRows(double mouseX, double mouseY, double delta, int itemCount) {
        if (!contains(mouseX, mouseY) || delta == 0.0D) {
            return false;
        }
        setContentRows(itemCount);
        clearPointerState();
        int next = Mth.clamp(rowScroll - (int) Math.signum(delta), 0, maxRowScroll());
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
        int rowY = y + contentInsetTop + visibleRowIndex * stride;
        return new UiRect(contentX(), rowY, contentWidth(), rowHeight);
    }

    public int hoveredRow(int mouseX, int mouseY, int itemCount) {
        int index = rowIndexAt(mouseX, mouseY, itemCount);
        int local = index < 0 ? -1 : index - firstVisibleRow();
        return local >= 0 && local < visibleRowCount() ? local : -1;
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
        UiRect track = scrollbarTrackBounds();
        if (track.width() <= 0) {
            return;
        }
        ImmortalUiSkin.drawThinScrollbar(graphics, track.x(), y + scrollbarTrackTopInset,
                Math.max(1, height - scrollbarTrackTopInset - scrollbarTrackBottomInset),
                scrollbarContentHeight(), scrollbarViewportHeight(), scrollOffset);
    }

    public static int clampScroll(int requested, int contentHeight, int viewportHeight) {
        int maximum = Math.max(0, contentHeight - Math.max(1, viewportHeight));
        return Math.max(0, Math.min(requested, maximum));
    }
}
