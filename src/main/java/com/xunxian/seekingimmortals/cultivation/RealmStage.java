package com.xunxian.seekingimmortals.cultivation;

public enum RealmStage {
    MORTAL("凡人", 1.00f, "MORTAL"),
    LAYER_1("1层", 1.00f, "QI_1"),
    LAYER_2("2层", 1.05f, "QI_2"),
    LAYER_3("3层", 1.10f, "QI_3"),
    LAYER_4("4层", 1.15f, "QI_4"),
    LAYER_5("5层", 1.20f, "QI_5"),
    LAYER_6("6层", 1.25f, "QI_6"),
    LAYER_7("7层", 1.30f, "QI_7"),
    LAYER_8("8层", 1.35f, "QI_8"),
    LAYER_9("9层", 1.40f, "QI_9"),
    LAYER_10("10层", 1.45f, "QI_10"),
    LAYER_11("11层", 1.50f, "QI_11"),
    LAYER_12("12层", 1.60f, "QI_12"),
    LAYER_13("13层（大圆满）", 1.80f, "QI_13"),
    EARLY("初期", 2.00f, "FOUNDATION_EARLY"),
    MIDDLE("中期", 2.40f, "FOUNDATION_MID"),
    LATE("后期", 3.00f, "FOUNDATION_LATE"),
    PEAK("圆满", 3.60f, "FOUNDATION_PEAK");

    private final String displayName;
    private final float maxSpiritualPowerMultiplier;
    private final String designId;

    RealmStage(String displayName, float maxSpiritualPowerMultiplier, String designId) {
        this.displayName = displayName;
        this.maxSpiritualPowerMultiplier = maxSpiritualPowerMultiplier;
        this.designId = designId;
    }

    public String getDisplayName() { return displayName; }
    public float getMaxSpiritualPowerMultiplier() { return maxSpiritualPowerMultiplier; }
    public float getMaxQiMultiplier() { return maxSpiritualPowerMultiplier; }
    public String getDesignId() { return designId; }
    public String getDesignKey() { return designId; }

    public boolean isQiRefiningLayer() {
        return this.ordinal() >= LAYER_1.ordinal() && this.ordinal() <= LAYER_13.ordinal();
    }

    public boolean isFoundationStage() {
        return this == EARLY || this == MIDDLE || this == LATE || this == PEAK;
    }

    public static RealmStage fromDesignId(String designId) {
        if (designId == null || designId.isBlank()) return MORTAL;
        for (RealmStage stage : values()) {
            if (stage.designId.equalsIgnoreCase(designId) || stage.name().equalsIgnoreCase(designId)) {
                return stage;
            }
        }
        if ("FOUNDATION_MIDDLE".equalsIgnoreCase(designId)) return MIDDLE;
        return MORTAL;
    }
}
