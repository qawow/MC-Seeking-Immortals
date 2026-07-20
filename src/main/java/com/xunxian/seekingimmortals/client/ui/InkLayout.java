package com.xunxian.seekingimmortals.client.ui;

import com.xunxian.seekingimmortals.client.UiRect;

/**
 * 云笈墨卷 shared responsive layout engine.
 *
 * <p>Centralizes the panel math previously copy-pasted across ~21 screens.
 * Screens keep their public {@code calculateLayout(w,h)} + {@code Layout}/{@code Rect}
 * records (ScreenLayoutTest contract) as thin wrappers that project a {@link Panel}
 * into their own record shape.</p>
 */
public final class InkLayout {
    private InkLayout() {}

    /**
     * Layout request. {@code listSplitRatio} is the list-pane share of the body
     * width when not stacked; 0 disables the list/detail split.
     */
    public record Spec(int minWidth, int maxWidth, int minHeight, int maxHeight,
                       int margin, int stackedBreakpoint, int headerHeight,
                       int footerHeight, double listSplitRatio) {

        /** Single-pane journal panel (stats/method/quest style). */
        public static final Spec JOURNAL = new Spec(220, 360, 160, 236, 4, 0, 22, 22, 0.0D);
        /** Lore family list+detail (bestiary/chronicle/compendium). */
        public static final Spec LORE = new Spec(220, 420, 150, 260, 4, 280, 22, 20, 0.42D);
        /** Container screens sized to a fixed slot plane. */
        public static final Spec CONTAINER = new Spec(360, 360, 236, 236, 4, 0, 22, 22, 0.0D);
    }

    /** Resolved panel anatomy. Panes may be zero-width when the spec disables them. */
    public record Panel(UiRect outer, UiRect header, UiRect body,
                        UiRect listPane, UiRect detailPane, UiRect footer,
                        boolean stacked, boolean wide) {}

    /** Computes a centered, clamped panel plus header/body/footer/list/detail bands. */
    public static Panel panel(int screenWidth, int screenHeight, Spec spec) {
        int margin = Math.max(0, spec.margin());
        int width = clamp(screenWidth - margin * 2, spec.minWidth(), spec.maxWidth());
        int height = clamp(screenHeight - margin * 2, spec.minHeight(), spec.maxHeight());
        width = Math.min(width, Math.max(1, screenWidth - margin * 2));
        height = Math.min(height, Math.max(1, screenHeight - margin * 2));
        int x = Math.max(margin, (screenWidth - width) / 2);
        int y = Math.max(margin, (screenHeight - height) / 2);
        UiRect outer = new UiRect(x, y, width, height);

        int headerH = Math.min(spec.headerHeight(), height / 3);
        UiRect header = new UiRect(x + 6, y + 6, width - 12, Math.max(0, headerH));

        int footerH = Math.min(spec.footerHeight(), height / 3);
        UiRect footer = footerH <= 0
                ? new UiRect(x + 6, y + height - 6, width - 12, 0)
                : new UiRect(x + 6, y + height - footerH - 6, width - 12, footerH);

        int bodyTop = header.bottom() + 4;
        int bodyBottom = footerH <= 0 ? y + height - 6 : footer.y() - 4;
        UiRect body = new UiRect(x + 6, bodyTop, width - 12, Math.max(0, bodyBottom - bodyTop));

        boolean stacked = spec.stackedBreakpoint() > 0 && width < spec.stackedBreakpoint();
        boolean wide = screenWidth >= 700;

        UiRect listPane;
        UiRect detailPane;
        if (spec.listSplitRatio() <= 0.0D) {
            listPane = new UiRect(body.x(), body.y(), 0, body.height());
            detailPane = body;
        } else if (stacked) {
            int listH = Math.max(24, (int) (body.height() * 0.45D));
            listPane = new UiRect(body.x(), body.y(), body.width(), listH);
            detailPane = new UiRect(body.x(), body.y() + listH + 3,
                    body.width(), Math.max(0, body.height() - listH - 3));
        } else {
            int listW = Math.max(60, (int) (body.width() * spec.listSplitRatio()));
            listPane = new UiRect(body.x(), body.y(), listW, body.height());
            detailPane = new UiRect(body.x() + listW + 4, body.y(),
                    Math.max(0, body.width() - listW - 4), body.height());
        }
        return new Panel(outer, header, body, listPane, detailPane, footer, stacked, wide);
    }

    /** Clamps a scroll offset so the viewport never overruns content. */
    public static int clampScroll(int offset, int contentHeight, int viewportHeight) {
        int max = Math.max(0, contentHeight - viewportHeight);
        return Math.max(0, Math.min(offset, max));
    }

    /** Evenly divides {@code width} into {@code count} tab cells with 2px gaps. */
    public static UiRect tabCell(UiRect strip, int count, int index) {
        if (count <= 0) {
            return strip;
        }
        int gap = 2;
        int cellW = (strip.width() - gap * (count - 1)) / count;
        int x = strip.x() + index * (cellW + gap);
        return new UiRect(x, strip.y(), cellW, strip.height());
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
