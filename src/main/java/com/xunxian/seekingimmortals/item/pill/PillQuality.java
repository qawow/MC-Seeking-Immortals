package com.xunxian.seekingimmortals.item.pill;

public enum PillQuality {
    LOW("下品", 1.0, 0x8B7355),
    MEDIUM("中品", 1.5, 0x4A90E2),
    HIGH("上品", 2.5, 0x9B59B6),
    SUPREME("极品", 5.0, 0xF39C12);

    private final String displayName;
    private final double effectMultiplier;
    private final int color;

    PillQuality(String displayName, double effectMultiplier, int color) {
        this.displayName = displayName;
        this.effectMultiplier = effectMultiplier;
        this.color = color;
    }

    public String getDisplayName() { return displayName; }
    public double getEffectMultiplier() { return effectMultiplier; }
    public int getColor() { return color; }
    public double getBreakthroughBonus() {
        return switch (this) {
            case LOW -> 0.05D;
            case MEDIUM -> 0.10D;
            case HIGH -> 0.15D;
            case SUPREME -> 0.20D;
        };
    }
    public int getBreakthroughBonusPercent() { return (int)Math.round(getBreakthroughBonus() * 100.0D); }
}
