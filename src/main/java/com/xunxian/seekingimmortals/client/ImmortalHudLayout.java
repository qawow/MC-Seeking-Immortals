package com.xunxian.seekingimmortals.client;

/**
 * Shared safe-area layout for the cultivation HUD overlays.
 *
 * <p>Left-stack layout: left-top status strip (气血 band over 修为/灵力 band),
 * and left skill rail vertically centered in the remaining height under the strip.
 * Compact/rail mode densifies sizes but keeps the same anchors whenever the two
 * surfaces can still separate.</p>
 */
final class ImmortalHudLayout {
    static final int WIDE_MARGIN = 6;
    static final int COMPACT_MARGIN = 4;
    static final int TINY_MARGIN = 2;
    static final int WIDE_GAP = 4;
    static final int TINY_GAP = 2;

    private static final int STATUS_STRIP_WIDTH = 220;
    private static final int STATUS_STRIP_HEIGHT = 108;
    private static final int HEALTH_BAND_HEIGHT = 30;
    private static final int HEALTH_ONLY_HEIGHT = 38;
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

    /** Full left-top status strip chrome bounds (gameplay HUD). */
    static Rect statusStripRect(int screenWidth, int screenHeight) {
        return calculate(screenWidth, screenHeight).statusStrip();
    }

    /** Top health band inside the status strip. */
    static Rect healthBandRect(int screenWidth, int screenHeight) {
        return calculate(screenWidth, screenHeight).healthBand();
    }

    /** Cultivation content band under the health band. */
    static Rect cultivationBandRect(int screenWidth, int screenHeight) {
        return calculate(screenWidth, screenHeight).cultivationBand();
    }

    /**
     * Compact left-top strip used when only 气血 is drawn (open screens / unsynced).
     * Shares x/width/top with the full strip but uses a shorter height.
     */
    static Rect healthOnlyStripRect(int screenWidth, int screenHeight) {
        return calculate(screenWidth, screenHeight).healthOnlyStrip();
    }

    /**
     * Legacy alias for the health content band. Live overlays prefer
     * {@link #healthBandRect} / {@link #healthOnlyStripRect}.
     */
    static Rect healthRect(int screenWidth, int screenHeight) {
        return healthBandRect(screenWidth, screenHeight);
    }

    /** Legacy alias for the cultivation content band. */
    static Rect cultivationRect(int screenWidth, int screenHeight) {
        return cultivationBandRect(screenWidth, screenHeight);
    }

    static Rect techniqueRect(int screenWidth, int screenHeight) {
        return calculate(screenWidth, screenHeight).techniques();
    }

    private static Layout regularLayout(int width, int height, int margin, int gap,
                                        int left, int top, int right, int bottom,
                                        int innerWidth, int innerHeight) {
        int stripMaxHeight = Math.max(1, innerHeight - gap - SLOT_COUNT);
        int stripHeight = Math.min(STATUS_STRIP_HEIGHT, stripMaxHeight);
        int stripWidth = Math.min(STATUS_STRIP_WIDTH, Math.max(1, innerWidth));
        Rect statusStrip = fit(new Rect(left, top, stripWidth, stripHeight), width, height);

        BandPair bands = bandsInside(statusStrip);
        Rect healthOnlyStrip = healthOnlyFromStrip(statusStrip, bands.healthBand());

        int techniqueTop = Math.min(bottom, statusStrip.bottom() + gap);
        int remainingHeight = Math.max(1, bottom - techniqueTop);
        TechniqueMetrics metrics = techniqueMetrics(remainingHeight, innerWidth, false);
        int techniqueY = techniqueTop + Math.max(0, (remainingHeight - metrics.height()) / 2);
        techniqueY = clamp(techniqueY, techniqueTop, Math.max(techniqueTop, bottom - metrics.height()));
        Rect techniques = fit(new Rect(left, techniqueY, metrics.width(), metrics.height()), width, height);

        return new Layout(width, height, margin, gap, false,
                statusStrip, bands.healthBand(), bands.cultivationBand(), healthOnlyStrip,
                techniques,
                metrics.slotSize(), metrics.slotGap(), metrics.padding());
    }

    private static Layout compactLayout(int width, int height, int margin, int gap,
                                        int left, int top, int right, int bottom,
                                        int innerWidth, int innerHeight) {
        int minimumTechniqueHeight = Math.min(SLOT_COUNT, Math.max(1, innerHeight / 3));
        int stripHeight = Math.min(STATUS_STRIP_HEIGHT,
                Math.max(1, innerHeight - gap - minimumTechniqueHeight));
        int stripWidth = Math.min(STATUS_STRIP_WIDTH, Math.max(1, innerWidth));
        Rect statusStrip = fit(new Rect(left, top, stripWidth, stripHeight), width, height);
        BandPair bands = bandsInside(statusStrip);
        Rect healthOnlyStrip = healthOnlyFromStrip(statusStrip, bands.healthBand());

        int techniqueTop = Math.min(bottom, statusStrip.bottom() + gap);
        int remainingHeight = Math.max(1, bottom - techniqueTop);
        TechniqueMetrics metrics = techniqueMetrics(remainingHeight, innerWidth, true);
        int techniqueY = techniqueTop + Math.max(0, (remainingHeight - metrics.height()) / 2);
        techniqueY = clamp(techniqueY, techniqueTop, Math.max(techniqueTop, bottom - metrics.height()));
        Rect techniques = fit(new Rect(left, techniqueY, metrics.width(), metrics.height()), width, height);

        // Pathological tiny screens: if left-stack collides, fall back to left rail + content column.
        Layout stacked = new Layout(width, height, margin, gap, true,
                statusStrip, bands.healthBand(), bands.cultivationBand(), healthOnlyStrip,
                techniques,
                metrics.slotSize(), metrics.slotGap(), metrics.padding());
        if (stacked.panelsSeparated() && stacked.allInside() && stacked.bandsValid()) {
            return stacked;
        }

        TechniqueMetrics railMetrics = techniqueMetrics(innerHeight, innerWidth, true);
        int railY = top + Math.max(0, (innerHeight - railMetrics.height()) / 2);
        Rect railTechniques = fit(new Rect(left, railY, railMetrics.width(), railMetrics.height()), width, height);
        int contentLeft = Math.min(right - 1, railTechniques.right() + gap);
        int contentWidth = Math.max(1, right - contentLeft);
        Rect fallbackStrip = fit(new Rect(contentLeft, top, contentWidth, innerHeight), width, height);
        BandPair fallbackBands = bandsInside(fallbackStrip);
        Rect fallbackHealthOnly = healthOnlyFromStrip(fallbackStrip, fallbackBands.healthBand());
        return new Layout(width, height, margin, gap, true,
                fallbackStrip, fallbackBands.healthBand(), fallbackBands.cultivationBand(), fallbackHealthOnly,
                railTechniques,
                railMetrics.slotSize(), railMetrics.slotGap(), railMetrics.padding());
    }

    private static BandPair bandsInside(Rect statusStrip) {
        int pad = statusStrip.width() >= 80 && statusStrip.height() >= 40 ? 4
                : statusStrip.height() >= 20 ? 3 : 2;
        int innerX = statusStrip.x() + pad;
        int innerY = statusStrip.y() + pad;
        int innerW = Math.max(1, statusStrip.width() - pad * 2);
        int innerH = Math.max(1, statusStrip.height() - pad * 2);

        int desiredHealth = Math.min(HEALTH_BAND_HEIGHT, Math.max(12, innerH / 3));
        int healthH = Math.min(desiredHealth, Math.max(1, innerH - 1));
        if (innerH >= 28) {
            healthH = Math.min(healthH, innerH - 12);
        }
        int cultH = Math.max(1, innerH - healthH);
        // Keep a one-pixel visual divider between bands when both fit.
        if (innerH > healthH + 1) {
            cultH = Math.max(1, innerH - healthH);
        }
        Rect healthBand = new Rect(innerX, innerY, innerW, healthH);
        Rect cultivationBand = new Rect(innerX, innerY + healthH, innerW, cultH);
        return new BandPair(healthBand, cultivationBand);
    }

    private static Rect healthOnlyFromStrip(Rect statusStrip, Rect healthBand) {
        int bottomPad = Math.max(2, statusStrip.bottom() - healthBand.bottom());
        int height = Math.min(HEALTH_ONLY_HEIGHT,
                Math.max(healthBand.height() + bottomPad + (healthBand.y() - statusStrip.y()),
                        Math.min(statusStrip.height(), healthBand.height() + 8)));
        height = Math.max(1, Math.min(height, statusStrip.height()));
        return new Rect(statusStrip.x(), statusStrip.y(), statusStrip.width(), height);
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
                  Rect statusStrip, Rect healthBand, Rect cultivationBand, Rect healthOnlyStrip,
                  Rect techniques,
                  int techniqueSlotSize, int techniqueSlotGap, int techniquePadding) {
        /** Nested health band — alias for tests that still ask for "health". */
        Rect health() {
            return healthBand;
        }

        /** Nested cultivation band — alias for tests that still ask for "cultivation". */
        Rect cultivation() {
            return cultivationBand;
        }

        boolean allInside() {
            return statusStrip.inside(screenWidth, screenHeight)
                    && techniques.inside(screenWidth, screenHeight)
                    && healthBand.inside(screenWidth, screenHeight)
                    && cultivationBand.inside(screenWidth, screenHeight)
                    && healthOnlyStrip.inside(screenWidth, screenHeight);
        }

        /**
         * Collision peers are the two free-floating surfaces only.
         * Health/cultivation bands nest inside the status strip and are not peers.
         */
        boolean panelsSeparated() {
            return !statusStrip.intersects(techniques);
        }

        boolean bandsValid() {
            return contains(statusStrip, healthBand)
                    && contains(statusStrip, cultivationBand)
                    && contains(statusStrip, healthOnlyStrip)
                    && !healthBand.intersects(cultivationBand)
                    && healthOnlyStrip.width() == statusStrip.width()
                    && healthOnlyStrip.x() == statusStrip.x()
                    && healthOnlyStrip.y() == statusStrip.y()
                    && healthOnlyStrip.height() <= statusStrip.height();
        }

        private static boolean contains(Rect outer, Rect inner) {
            return inner.x() >= outer.x()
                    && inner.y() >= outer.y()
                    && inner.right() <= outer.right()
                    && inner.bottom() <= outer.bottom();
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

    private record BandPair(Rect healthBand, Rect cultivationBand) {}
}
