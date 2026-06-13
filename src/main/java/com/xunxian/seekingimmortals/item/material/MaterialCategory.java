package com.xunxian.seekingimmortals.item.material;

public enum MaterialCategory {
    SPIRITUAL_HERB("灵草灵药", "种植或野外采集"),
    BEAST_MATERIAL("妖兽材料", "击败妖兽掉落"),
    MINERAL("矿石材料", "挖矿获得"),
    SPECIAL("特殊材料", "秘境、遗迹获得");

    private final String displayName;
    private final String description;

    MaterialCategory(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
}
