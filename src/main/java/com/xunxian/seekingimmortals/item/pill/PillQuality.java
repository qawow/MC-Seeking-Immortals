package com.xunxian.seekingimmortals.item.pill;

import java.util.Locale;

/**
 * Pill quality tiers aligned to text-material {@code pill_quality.json}.
 * <p>Canonical names match M04 brief: LOW / MIDDLE / HIGH / PERFECT.
 * Legacy corpus aliases (medium/standard/supreme/peerless/inferior) resolve via {@link #fromId(String)}.
 */
public enum PillQuality {
    LOW("下品", 0.7, 0x8B7355),
    MIDDLE("中品", 1.0, 0x4A90E2),
    HIGH("上品", 1.25, 0x9B59B6),
    PERFECT("极品", 1.5, 0xF39C12);

    /** @deprecated use {@link #MIDDLE} — kept for binary-compat call sites during transition. */
    @Deprecated
    public static final PillQuality MEDIUM = MIDDLE;
    /** @deprecated use {@link #PERFECT}. */
    @Deprecated
    public static final PillQuality SUPREME = PERFECT;

    private final String displayName;
    private final double effectMultiplier;
    private final int color;

    PillQuality(String displayName, double effectMultiplier, int color) {
        this.displayName = displayName;
        this.effectMultiplier = effectMultiplier;
        this.color = color;
    }

    public String getDisplayName() {
        return displayName;
    }

    public double getEffectMultiplier() {
        return effectMultiplier;
    }

    public int getColor() {
        return color;
    }

    public double getBreakthroughBonus() {
        return switch (this) {
            case LOW -> 0.05D;
            case MIDDLE -> 0.10D;
            case HIGH -> 0.15D;
            case PERFECT -> 0.20D;
        };
    }

    public int getBreakthroughBonusPercent() {
        return (int) Math.round(getBreakthroughBonus() * 100.0D);
    }

    /** Design id used in JSON / tooltips: low/middle/high/perfect. */
    public String designId() {
        return name().toLowerCase(Locale.ROOT);
    }

    /**
     * Resolve quality from corpus or runtime id.
     * Accepts LOW/MIDDLE/HIGH/PERFECT plus legacy medium/supreme/inferior/standard/superior/peerless.
     */
    public static PillQuality fromId(String raw) {
        if (raw == null || raw.isBlank()) {
            return LOW;
        }
        String id = raw.trim().toLowerCase(Locale.ROOT);
        return switch (id) {
            case "low", "inferior", "下品" -> LOW;
            case "middle", "medium", "standard", "mid", "中品" -> MIDDLE;
            case "high", "superior", "上品" -> HIGH;
            case "perfect", "supreme", "peerless", "极品" -> PERFECT;
            default -> {
                try {
                    yield PillQuality.valueOf(id.toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException ex) {
                    yield LOW;
                }
            }
        };
    }

    /** Map quality roll score [0,1+] onto a tier (used by alchemy furnace). */
    public static PillQuality fromQualityScore(double qualityScore) {
        if (qualityScore >= 0.95D) {
            return PERFECT;
        }
        if (qualityScore >= 0.75D) {
            return HIGH;
        }
        if (qualityScore >= 0.50D) {
            return MIDDLE;
        }
        return LOW;
    }
}
