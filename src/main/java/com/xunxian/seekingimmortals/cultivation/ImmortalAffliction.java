package com.xunxian.seekingimmortals.cultivation;

public enum ImmortalAffliction {
    SEVERE_INJURY("重伤"),
    HEART_DEMON("心魔"),
    REALM_FALL("跌境"),
    SHATTERED_CORE("碎丹");

    private final String displayName;

    ImmortalAffliction(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
