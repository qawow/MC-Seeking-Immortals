package com.xunxian.seekingimmortals.region;

/**
 * Server-side toggle for daily region event scheduling.
 * No Forge config class in this codebase; command + saved flag is the control surface.
 */
public final class RegionEventConfig {
    private static volatile boolean dailyEventsEnabled = true;

    private RegionEventConfig() {}

    public static boolean isDailyEventsEnabled() {
        return dailyEventsEnabled;
    }

    public static void setDailyEventsEnabled(boolean enabled) {
        dailyEventsEnabled = enabled;
    }
}
