package com.xunxian.seekingimmortals.skill;

public enum SkillCategory {
    CULTIVATION_METHOD("功法", "修炼功法，影响修炼速度和境界突破"),
    SPELL("法术", "五行法术、神识法术、辅助法术"),
    CRAFTING("生活技能", "炼丹、炼器、阵法、符箓"),
    SPECIAL("特殊技能", "驭兽、傀儡、分神多用");

    private final String displayName;
    private final String description;

    SkillCategory(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
}
