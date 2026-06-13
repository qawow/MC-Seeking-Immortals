package com.xunxian.seekingimmortals.item.pill;

public enum PillCategory {
    CULTIVATION("修炼类", "提升修为或辅助突破"),
    HEALING("疗伤类", "恢复伤势或治疗状态"),
    AUXILIARY("辅助类", "各种辅助效果");

    private final String displayName;
    private final String description;

    PillCategory(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
}
