package com.xunxian.seekingimmortals.cultivation;

/**
 * 金丹品阶枚举（Phase 2 预留）
 * <p>用于结丹期金丹品质系统，当前 MVP 阶段不实现</p>
 *
 * @since Phase 2 (预留于 Phase 1)
 */
public enum GoldCoreGrade {
    /**
     * 下品金丹（灰色）
     */
    LOW("下品金丹", 0xA0A0A0, 1.0f),

    /**
     * 中品金丹（银色）
     */
    MIDDLE("中品金丹", 0xC0C0C0, 1.2f),

    /**
     * 上品金丹（金色）
     */
    HIGH("上品金丹", 0xFFD700, 1.5f),

    /**
     * 极品金丹（紫色，隐藏技能）
     */
    PERFECT("极品金丹", 0x9370DB, 2.0f);

    private final String displayName;
    private final int color;
    private final float attributeMultiplier;

    GoldCoreGrade(String displayName, int color, float attributeMultiplier) {
        this.displayName = displayName;
        this.color = color;
        this.attributeMultiplier = attributeMultiplier;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getColor() {
        return color;
    }

    /**
     * 获取属性加成倍率
     * <p>下品1.0，中品1.2，上品1.5，极品2.0</p>
     */
    public float getAttributeMultiplier() {
        return attributeMultiplier;
    }

    /**
     * 根据突破时的修为进度、灵根、丹药品质等计算金丹品阶
     * <p>Phase 2 实现</p>
     */
    public static GoldCoreGrade calculateGrade(double cultivationProgress, SpiritualRoot root, boolean usedPerfectPill) {
        // TODO: Phase 2 实现完整计算逻辑
        return LOW;
    }
}
