package com.xunxian.seekingimmortals.cultivation;

import java.util.Locale;

/**
 * 修炼境界枚举。
 * <p>Java 常量名保持历史 NBT 兼容；{@link #designId} 对齐语料
 * {@code cultivation_progression.json} / {@code realm_breakthrough_v98.json} 的 id。</p>
 * <p>寿元取自语料 lifespan_years 表（凡人取凡人起点 80）。</p>
 */
public enum Realm {
    // 凡人：未引气入体的起点境界。
    MORTAL("凡人", "MORTAL", 0, 500, 80, false, 0),
    QI_REFINING("炼气", "QI_REFINING", 100, 100, 120, true, 13),
    FOUNDATION_ESTABLISHMENT("筑基", "FOUNDATION", 250, 300, 250, false, 4),
    CORE_FORMATION("结丹", "CORE_FORMATION", 600, 900, 600, false, 3),
    NASCENT_SOUL("元婴", "NASCENT_SOUL", 1200, 2000, 1200, false, 3),
    SOUL_TRANSFORMATION("化神", "DEITY_TRANSFORMATION", 2500, 5000, 2500, false, 3),
    VOID_REFINEMENT("炼虚", "VOID_REFINEMENT", 5000, 10000, 5000, false, 3),
    UNITY("合体", "BODY_INTEGRATION", 10000, 20000, 10000, false, 3),
    MAHAYANA("大乘", "GREAT_VEHICLE", 20000, 40000, 100000, false, 3),
    TRIBULATION("渡劫", "TRIBULATION_LAND", 40000, 80000, 100000, false, 1),
    TRUE_IMMORTAL("真仙", "TRUE_IMMORTAL", 80000, 160000, 100000, false, 3);

    private final String displayName;
    private final String designId;
    private final int baseMaxSpiritualPower;
    private final int stageExpSpan;
    private final int lifespanYears;
    private final boolean layerBased;
    private final int subStages;

    Realm(String displayName, String designId, int baseMaxSpiritualPower, int stageExpSpan,
          int lifespanYears, boolean layerBased, int subStages) {
        this.displayName = displayName;
        this.designId = designId;
        this.baseMaxSpiritualPower = baseMaxSpiritualPower;
        this.stageExpSpan = stageExpSpan;
        this.lifespanYears = lifespanYears;
        this.layerBased = layerBased;
        this.subStages = subStages;
    }

    public String getDisplayName() { return displayName; }
    public String getDesignId() { return designId; }
    public String getDesignKey() { return designId; }
    public int getBaseMaxSpiritualPower() { return RealmStageConfig.getManaBase(this); }
    public int getBaseMaxQi() { return getBaseMaxSpiritualPower(); }
    public int getStageExpSpan() { return RealmStageConfig.getStageExpSpan(this); }
    public int getExpToNextStage() { return getStageExpSpan(); }
    public int getLifespanYears() { return lifespanYears; }
    public boolean isLayerBased() { return layerBased; }
    public boolean isPhase1Realm() { return this == QI_REFINING || this == FOUNDATION_ESTABLISHMENT; }
    /** 语料 sub_stages：炼气 13、筑基 4、其余大境 3、渡劫 1、凡人 0。 */
    public int getSubStages() { return subStages; }

    public Realm next() {
        int index = ordinal() + 1;
        return index >= values().length ? this : values()[index];
    }

    /**
     * 解析语料/配置中的境界 id。兼容历史 Java 常量名与语料 design id。
     * @return 无法识别时返回 null
     */
    public static Realm fromDesignId(String id) {
        if (id == null || id.isBlank()) {
            return null;
        }
        String normalized = stripStageSuffix(id.trim().toUpperCase(Locale.ROOT)
                .replace('-', '_')
                .replace(' ', '_'));
        return switch (normalized) {
            case "MORTAL", "FANREN" -> MORTAL;
            case "QI_REFINING", "QI", "LIANQI" -> QI_REFINING;
            case "FOUNDATION", "FOUNDATION_ESTABLISHMENT", "ZHUJI" -> FOUNDATION_ESTABLISHMENT;
            case "CORE_FORMATION", "JIEDAN", "GOLD_CORE" -> CORE_FORMATION;
            case "NASCENT_SOUL", "YUANYING" -> NASCENT_SOUL;
            case "DEITY_TRANSFORMATION", "SOUL_TRANSFORMATION", "SPIRIT_SEVERANCE", "SPIRIT_SEVERING", "HUASHEN" -> SOUL_TRANSFORMATION;
            case "VOID_REFINEMENT", "VOID_REFINING", "LIANXU", "VOID" -> VOID_REFINEMENT;
            case "BODY_INTEGRATION", "UNITY", "HETI" -> UNITY;
            case "GREAT_VEHICLE", "MAHAYANA", "DACHENG" -> MAHAYANA;
            case "TRIBULATION", "TRIBULATION_LAND", "DUJIE" -> TRIBULATION;
            case "TRUE_IMMORTAL", "ZHENXIAN", "IMMORTAL" -> TRUE_IMMORTAL;
            default -> {
                for (Realm realm : values()) {
                    if (realm.name().equals(normalized)
                            || realm.designId.equalsIgnoreCase(normalized)
                            || realm.displayName.equals(id.trim())) {
                        yield realm;
                    }
                }
                yield null;
            }
        };
    }

    private static String stripStageSuffix(String id) {
        for (String suffix : new String[]{"_EARLY", "_MIDDLE", "_MID", "_LATE", "_PEAK"}) {
            if (id.endsWith(suffix)) {
                return id.substring(0, id.length() - suffix.length());
            }
        }
        return id;
    }

    /** 与 {@link #fromDesignId(String)} 相同，无法识别时回退 {@link #MORTAL}。 */
    public static Realm fromDesignIdOrMortal(String id) {
        Realm realm = fromDesignId(id);
        return realm == null ? MORTAL : realm;
    }
}
