package com.xunxian.seekingimmortals.cultivation;

public enum Realm {
    QI_REFINING("炼气", 100, 100, 100, true),
    FOUNDATION_ESTABLISHMENT("筑基", 250, 300, 200, false),
    CORE_FORMATION("结丹", 600, 900, 500, false),
    NASCENT_SOUL("元婴", 1200, 2000, 1000, false),
    SOUL_TRANSFORMATION("化神", 2500, 5000, 2000, false),
    VOID_REFINEMENT("炼虚", 5000, 10000, 4000, false),
    UNITY("合体", 10000, 20000, 8000, false),
    MAHAYANA("大乘", 20000, 40000, 16000, false),
    TRIBULATION("渡劫", 40000, 80000, 32000, false),
    TRUE_IMMORTAL("真仙", 80000, 160000, 100000, false);

    private final String displayName;
    private final int baseMaxSpiritualPower;
    private final int stageExpSpan;
    private final int lifespanYears;
    private final boolean layerBased;

    Realm(String displayName, int baseMaxSpiritualPower, int stageExpSpan, int lifespanYears, boolean layerBased) {
        this.displayName = displayName;
        this.baseMaxSpiritualPower = baseMaxSpiritualPower;
        this.stageExpSpan = stageExpSpan;
        this.lifespanYears = lifespanYears;
        this.layerBased = layerBased;
    }

    public String getDisplayName() { return displayName; }
    public int getBaseMaxSpiritualPower() { return baseMaxSpiritualPower; }
    public int getBaseMaxQi() { return baseMaxSpiritualPower; }
    public int getStageExpSpan() { return stageExpSpan; }
    public int getExpToNextStage() { return stageExpSpan; }
    public int getLifespanYears() { return lifespanYears; }
    public boolean isLayerBased() { return layerBased; }

    public Realm next() {
        int index = ordinal() + 1;
        return index >= values().length ? this : values()[index];
    }
}
