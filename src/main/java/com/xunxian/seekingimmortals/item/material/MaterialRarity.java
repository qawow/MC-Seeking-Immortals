package com.xunxian.seekingimmortals.item.material;

public enum MaterialRarity {
    COMMON("普通", 1.0, 0xFFFFFF),
    UNCOMMON("罕见", 1.5, 0x55FF55),
    RARE("稀有", 2.0, 0x5555FF),
    EPIC("史诗", 3.0, 0xAA00AA),
    LEGENDARY("传说", 5.0, 0xFFAA00);

    private final String displayName;
    private final double valueMultiplier;
    private final int color;

    MaterialRarity(String displayName, double valueMultiplier, int color) {
        this.displayName = displayName;
        this.valueMultiplier = valueMultiplier;
        this.color = color;
    }

    public String getDisplayName() { return displayName; }
    public double getValueMultiplier() { return valueMultiplier; }
    public int getColor() { return color; }
}
