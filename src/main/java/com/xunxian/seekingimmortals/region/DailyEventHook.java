package com.xunxian.seekingimmortals.region;

/**
 * Subscription surface for M08 (faction conflict) / M11 (quest hooks).
 * Implementations must be server-side only.
 */
@FunctionalInterface
public interface DailyEventHook {
    void onDailyEvent(String regionId, String eventId);
}
