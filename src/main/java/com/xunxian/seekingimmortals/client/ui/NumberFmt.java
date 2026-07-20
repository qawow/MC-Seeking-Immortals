package com.xunxian.seekingimmortals.client.ui;

import java.util.Locale;

/**
 * 云笈墨卷 shared number formatting — one canonical 万/亿/兆 register.
 *
 * <p>Replaces the divergent {@code shortNumber} copies (one used B/M units,
 * another 万/亿/兆) across CultivationStatsScreen and the HUD overlays.</p>
 */
public final class NumberFmt {
    private NumberFmt() {}

    /** Xianxia units: ≥兆(1e12) → 兆, ≥亿(1e8) → 亿, ≥万(1e4) → 万, else raw. */
    public static String cjk(long value) {
        double abs = Math.abs((double) value);
        if (abs >= 1_000_000_000_000D) {
            return unit(value, 1_000_000_000_000D, "兆");
        }
        if (abs >= 100_000_000D) {
            return unit(value, 100_000_000D, "亿");
        }
        if (abs >= 10_000D) {
            return unit(value, 10_000D, "万");
        }
        return Long.toString(value);
    }

    /** "current/max" pair in the cjk register. */
    public static String cjkPair(long current, long max) {
        return cjk(current) + "/" + cjk(max);
    }

    public static String percent(double fraction) {
        return String.format(Locale.ROOT, "%.0f%%", clamp01(fraction) * 100.0D);
    }

    public static String two(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    public static double clamp01(double value) {
        return Math.max(0.0D, Math.min(1.0D, value));
    }

    private static String unit(long value, double unitValue, String suffix) {
        double scaled = value / unitValue;
        String pattern = Math.abs(scaled) >= 100.0D ? "%.0f%s" : "%.1f%s";
        return String.format(Locale.ROOT, pattern, scaled, suffix).replace(".0", "");
    }
}
