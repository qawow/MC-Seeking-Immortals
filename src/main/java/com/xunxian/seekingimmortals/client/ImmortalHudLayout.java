package com.xunxian.seekingimmortals.client;

/** Shared safe-area layout for the four cultivation HUD overlays. */
final class ImmortalHudLayout {
    static final int WIDE_MARGIN = 6;
    static final int COMPACT_MARGIN = 4;
    static final int TINY_MARGIN = 2;
    static final int WIDE_GAP = 4;
    static final int TINY_GAP = 2;

    private static final int HEALTH_WIDTH = 154;
    private static final int HEALTH_HEIGHT = 40;
    private static final int CULTIVATION_WIDTH = 184;
    private static final int CULTIVATION_HEIGHT = 82;
    private static final int BREATHING_WIDTH = 222;
    private static final int BREATHING_HEIGHT = 46;
    private static final int BOTTOM_SAFE_GAP = 50;
    private static final int SLOT_COUNT = 7;

    private ImmortalHudLayout() {}

    static Layout calculate(int screenWidth, int screenHeight) {
        int width = Math.max(1, screenWidth);
        int height = Math.max(1, screenHeight);
        int margin = margin(width, height);
        int gap = margin <= TINY_MARGIN ? TINY_GAP : WIDE_GAP;
        int innerLeft = Math.min(margin, width - 1);
        int innerTop = Math.min(margin, height - 1);
        int innerRight = Math.max(innerLeft + 1, width - margin);
        int innerBottom = Math.max(innerTop + 1, height - margin);
        int innerWidth = Math.max(1, innerRight - innerLeft);
        int innerHeight = Math.max(1, innerBottom - innerTop);
        boolean railMode = width < 300 || height < 140;

        return railMode
                ? compactLayout(width, height, margin, gap, innerLeft, innerTop, innerRight, innerBottom,
                        innerWidth, innerHeight)
                : regularLayout(width, height, margin, gap, innerLeft, innerTop, innerRight, innerBottom,
                        innerWidth, innerHeight);
    }

    static Rect healthRect(int screenWidth, int screenHeight) {
        return calculate(screenWidth, screenHeight).health();
    }

    static Rect cultivationRect(int screenWidth, int screenHeight) {
        return calculate(screenWidth, screenHeight).cultivation();
    }

    static Rect techniqueRect(int screenWidth, int screenHeight) {
        return calculate(screenWidth, screenHeight).techniques();
    }

    static Rect breathingRect(int screenWidth, int screenHeight) {
        return calculate(screenWidth, screenHeight).breathing();
    }

    private static Layout compactLayout(int width, int height, int margin, int gap,
                                        int left, int top, int right, int bottom,
                                        int innerWidth, int innerHeight) {
        TechniqueMetrics metrics = techniqueMetrics(innerHeight, innerWidth, true);
        int techniqueY = top + Math.max(0, (innerHeight - metrics.height()) / 2);
        Rect techniques = fit(new Rect(left, techniqueY, metrics.width(), metrics.height()), width, height);

        int contentLeft = Math.min(right - 1, techniques.right() + gap);
        int contentWidth = Math.max(1, right - contentLeft);
        int columnGap = contentWidth >= 12 ? gap : 0;
        int healthWidth = Math.max(1, (contentWidth - columnGap) / 2);
        int cultivationWidth = Math.max(1, contentWidth - columnGap - healthWidth);

        int breathingHeight = Math.min(36, Math.max(12, innerHeight / 2));
        int topHeight = Math.max(1, innerHeight - gap - breathingHeight);
        if (topHeight < 12 && innerHeight >= 24) {
            topHeight = 12;
            breathingHeight = Math.max(1, innerHeight - gap - topHeight);
        }

        Rect health = fit(new Rect(contentLeft, top, healthWidth, topHeight), width, height);
        Rect cultivation = fit(new Rect(contentLeft + healthWidth + columnGap, top,
                cultivationWidth, topHeight), width, height);
        Rect breathing = fit(new Rect(contentLeft, top + topHeight + gap,
                contentWidth, breathingHeight), width, height);
        return new Layout(width, height, margin, gap, true, health, cultivation, techniques, breathing,
                metrics.slotSize(), metrics.slotGap(), metrics.padding());
    }

    private static Layout regularLayout(int width, int height, int margin, int gap,
                                        int left, int top, int right, int bottom,
                                        int innerWidth, int innerHeight) {
        int healthWidth = Math.min(HEALTH_WIDTH, Math.max(1, (innerWidth - gap) / 2));
        int cultivationWidth = Math.min(CULTIVATION_WIDTH,
                Math.max(1, innerWidth - gap - healthWidth));
        int healthHeight = Math.min(HEALTH_HEIGHT, innerHeight);
        int breathingHeight = Math.min(BREATHING_HEIGHT, Math.max(1, innerHeight - gap));
        int cultivationHeight = Math.min(CULTIVATION_HEIGHT,
                Math.max(1, innerHeight - breathingHeight - gap));

        Rect health = fit(new Rect(left, top, healthWidth, healthHeight), width, height);
        Rect cultivation = fit(new Rect(right - cultivationWidth, top,
                cultivationWidth, cultivationHeight), width, height);

        int techniqueTop = Math.min(bottom - 1, health.bottom() + gap);
        int techniqueAvailableHeight = Math.max(1, bottom - techniqueTop);
        TechniqueMetrics metrics = techniqueMetrics(techniqueAvailableHeight, innerWidth, false);
        Rect techniques = fit(new Rect(left, techniqueTop, metrics.width(), metrics.height()), width, height);

        int breathingWidth = Math.min(BREATHING_WIDTH, innerWidth);
        int breathingX = left + Math.max(0, (innerWidth - breathingWidth) / 2);
        int breathingMinY = Math.min(bottom - breathingHeight,
                Math.max(top, cultivation.bottom() + gap));
        int breathingTargetY = height - breathingHeight - BOTTOM_SAFE_GAP;
        int breathingY = clamp(breathingTargetY, breathingMinY, Math.max(breathingMinY, bottom - breathingHeight));
        Rect breathing = fit(new Rect(breathingX, breathingY, breathingWidth, breathingHeight), width, height);

        return new Layout(width, height, margin, gap, false, health, cultivation, techniques, breathing,
                metrics.slotSize(), metrics.slotGap(), metrics.padding());
    }

    private static TechniqueMetrics techniqueMetrics(int availableHeight, int availableWidth, boolean railMode) {
        int safeHeight = Math.max(1, availableHeight);
        int slotGap = safeHeight >= 70 ? 1 : 0;
        if (!railMode && safeHeight >= 120) {
            slotGap = 2;
        }
        if (!railMode && safeHeight >= 162) {
            slotGap = 3;
        }
        int padding = safeHeight >= 100 ? 6 : safeHeight >= 50 ? 3 : 2;
        if (!railMode && safeHeight >= 162) {
            padding = 9;
        }
        int slotsSpace = Math.max(SLOT_COUNT, safeHeight - padding * 2 - slotGap * (SLOT_COUNT - 1));
        int slotSize = clamp(slotsSpace / SLOT_COUNT, 1, 18);
        int height = padding * 2 + slotSize * SLOT_COUNT + slotGap * (SLOT_COUNT - 1);
        while (height > safeHeight && padding > 0) {
            padding--;
            height = padding * 2 + slotSize * SLOT_COUNT + slotGap * (SLOT_COUNT - 1);
        }
        while (height > safeHeight && slotGap > 0) {
            slotGap--;
            height = padding * 2 + slotSize * SLOT_COUNT + slotGap * (SLOT_COUNT - 1);
        }
        while (height > safeHeight && slotSize > 1) {
            slotSize--;
            height = padding * 2 + slotSize * SLOT_COUNT + slotGap * (SLOT_COUNT - 1);
        }
        height = Math.min(safeHeight, Math.max(1, height));
        int desiredWidth = slotSize + (railMode ? 8 : 12);
        int width = Math.min(Math.max(1, availableWidth), Math.max(slotSize, Math.min(30, desiredWidth)));
        return new TechniqueMetrics(width, height, slotSize, slotGap, padding);
    }

    private static int margin(int width, int height) {
        if (width >= 300 && height >= 160) {
            return Math.min(WIDE_MARGIN, Math.min((width - 1) / 2, (height - 1) / 2));
        }
        if (width >= 120 && height >= 70) {
            return Math.min(COMPACT_MARGIN, Math.min((width - 1) / 2, (height - 1) / 2));
        }
        return Math.min(TINY_MARGIN, Math.min((width - 1) / 2, (height - 1) / 2));
    }

    private static Rect fit(Rect rect, int width, int height) {
        int safeWidth = Math.max(1, Math.min(rect.width(), width));
        int safeHeight = Math.max(1, Math.min(rect.height(), height));
        int x = clamp(rect.x(), 0, Math.max(0, width - safeWidth));
        int y = clamp(rect.y(), 0, Math.max(0, height - safeHeight));
        return new Rect(x, y, safeWidth, safeHeight);
    }

    private static int clamp(int value, int min, int max) {
        if (max < min) {
            return min;
        }
        return Math.max(min, Math.min(max, value));
    }

    record Layout(int screenWidth, int screenHeight, int margin, int gap, boolean railMode,
                  Rect health, Rect cultivation, Rect techniques, Rect breathing,
                  int techniqueSlotSize, int techniqueSlotGap, int techniquePadding) {
        boolean allInside() {
            return health.inside(screenWidth, screenHeight)
                    && cultivation.inside(screenWidth, screenHeight)
                    && techniques.inside(screenWidth, screenHeight)
                    && breathing.inside(screenWidth, screenHeight);
        }

        boolean panelsSeparated() {
            Rect[] panels = { health, cultivation, techniques, breathing };
            for (int i = 0; i < panels.length; i++) {
                for (int j = i + 1; j < panels.length; j++) {
                    if (panels[i].intersects(panels[j])) {
                        return false;
                    }
                }
            }
            return true;
        }
    }

    record Rect(int x, int y, int width, int height) {
        int right() {
            return x + width;
        }

        int bottom() {
            return y + height;
        }

        boolean inside(int screenWidth, int screenHeight) {
            return width > 0 && height > 0 && x >= 0 && y >= 0
                    && right() <= screenWidth && bottom() <= screenHeight;
        }

        boolean intersects(Rect other) {
            return other != null && x < other.right() && right() > other.x
                    && y < other.bottom() && bottom() > other.y;
        }
    }

    private record TechniqueMetrics(int width, int height, int slotSize, int slotGap, int padding) {}
}
