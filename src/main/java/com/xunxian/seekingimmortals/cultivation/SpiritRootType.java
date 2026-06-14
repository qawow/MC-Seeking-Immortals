package com.xunxian.seekingimmortals.cultivation;

/**
 * 灵根类型简化枚举（MVP Phase 1 预留）
 * <p>用于后续系统扩展，当前阶段使用 {@link SpiritualRoot} 枚举</p>
 *
 * @since Phase 1
 */
public enum SpiritRootType {
    /**
     * 天灵根（单属性，极高资质）
     */
    HEAVENLY("天灵根", 1),

    /**
     * 异灵根（单变异属性，稀有）
     */
    MUTATED("异灵根", 1),

    /**
     * 双灵根（两属性，优秀资质）
     */
    DUAL("双灵根", 2),

    /**
     * 三灵根（三属性，中等资质）
     */
    TRIPLE("三灵根", 3),

    /**
     * 伪灵根（四属性，低资质）
     */
    FALSE_ROOT("伪灵根", 4),

    /**
     * 杂灵根（五属性，最低资质）
     */
    MIXED("杂灵根", 5);

    private final String displayName;
    private final int attributeCount;

    SpiritRootType(String displayName, int attributeCount) {
        this.displayName = displayName;
        this.attributeCount = attributeCount;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getAttributeCount() {
        return attributeCount;
    }

    /**
     * 从 SpiritualRoot 转换为 SpiritRootType
     */
    public static SpiritRootType fromSpiritualRoot(SpiritualRoot root) {
        return switch (root) {
            case HEAVENLY -> HEAVENLY;
            case HIDDEN, MUTATED -> MUTATED;
            case DUAL -> DUAL;
            case TRIPLE -> TRIPLE;
            case FALSE_ROOT -> FALSE_ROOT;
            case MIXED -> MIXED;
        };
    }
}
