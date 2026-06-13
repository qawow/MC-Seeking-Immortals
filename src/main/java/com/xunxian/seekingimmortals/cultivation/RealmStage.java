package com.xunxian.seekingimmortals.cultivation;

public enum RealmStage {
    LAYER_1("1层", 1.00f),
    LAYER_2("2层", 1.05f),
    LAYER_3("3层", 1.10f),
    LAYER_4("4层", 1.15f),
    LAYER_5("5层", 1.20f),
    LAYER_6("6层", 1.25f),
    LAYER_7("7层", 1.30f),
    LAYER_8("8层", 1.35f),
    LAYER_9("9层", 1.40f),
    LAYER_10("10层", 1.45f),
    LAYER_11("11层", 1.50f),
    LAYER_12("12层", 1.60f),
    LAYER_13("13层（大圆满）", 1.80f),
    EARLY("初期", 2.00f),
    MIDDLE("中期", 2.40f),
    LATE("后期", 3.00f);

    private final String displayName;
    private final float maxSpiritualPowerMultiplier;

    RealmStage(String displayName, float maxSpiritualPowerMultiplier) {
        this.displayName = displayName;
        this.maxSpiritualPowerMultiplier = maxSpiritualPowerMultiplier;
    }

    public String getDisplayName() { return displayName; }
    public float getMaxSpiritualPowerMultiplier() { return maxSpiritualPowerMultiplier; }
    public float getMaxQiMultiplier() { return maxSpiritualPowerMultiplier; }

    public boolean isQiRefiningLayer() {
        return ordinal() <= LAYER_13.ordinal();
    }
}
