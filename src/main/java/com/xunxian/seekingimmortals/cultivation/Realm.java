package com.xunxian.seekingimmortals.cultivation;

public enum Realm {
    // 凡人：未引气入体的起点境界。灵力上限 0、神识 3 格、HP 基准 0，修为积累到上限后可突破进入练气1。
    MORTAL("凡人", "MORTAL", 0, 500, 80, false),
    QI_REFINING("炼气", "QI_REFINING", 100, 100, 100, true),
    FOUNDATION_ESTABLISHMENT("筑基", "FOUNDATION", 250, 300, 200, false),
    CORE_FORMATION("结丹", "CORE_FORMATION", 600, 900, 500, false),
    NASCENT_SOUL("元婴", "NASCENT_SOUL", 1200, 2000, 1000, false),
    SOUL_TRANSFORMATION("化神", "SOUL_TRANSFORMATION", 2500, 5000, 2000, false),
    VOID_REFINEMENT("炼虚", "VOID_REFINEMENT", 5000, 10000, 4000, false),
    UNITY("合体", "UNITY", 10000, 20000, 8000, false),
    MAHAYANA("大乘", "MAHAYANA", 20000, 40000, 16000, false),
    TRIBULATION("渡劫", "TRIBULATION", 40000, 80000, 32000, false),
    TRUE_IMMORTAL("真仙", "TRUE_IMMORTAL", 80000, 160000, 100000, false);

    private final String displayName;
    private final String designId;
    private final int baseMaxSpiritualPower;
    private final int stageExpSpan;
    private final int lifespanYears;
    private final boolean layerBased;

    Realm(String displayName, String designId, int baseMaxSpiritualPower, int stageExpSpan, int lifespanYears, boolean layerBased) {
        this.displayName = displayName;
        this.designId = designId;
        this.baseMaxSpiritualPower = baseMaxSpiritualPower;
        this.stageExpSpan = stageExpSpan;
        this.lifespanYears = lifespanYears;
        this.layerBased = layerBased;
    }

    public String getDisplayName() { return displayName; }
    public String getDesignId() { return designId; }
    public String getDesignKey() { return designId; }
    public int getBaseMaxSpiritualPower() { return baseMaxSpiritualPower; }
    public int getBaseMaxQi() { return baseMaxSpiritualPower; }
    public int getStageExpSpan() { return stageExpSpan; }
    public int getExpToNextStage() { return stageExpSpan; }
    public int getLifespanYears() { return lifespanYears; }
    public boolean isLayerBased() { return layerBased; }
    public boolean isPhase1Realm() { return this == QI_REFINING || this == FOUNDATION_ESTABLISHMENT; }

    public Realm next() {
        int index = ordinal() + 1;
        return index >= values().length ? this : values()[index];
    }
}
