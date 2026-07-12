package com.xunxian.seekingimmortals.worldpack;

import java.util.Locale;

public final class YinUnderworldHazard {
    public static final String YINMING_REGION_ID = "yinming";
    public static final String NETHER_RIVER_REGION_ID = "nether_river";
    public static final String YINMING_SECRET_REALM_ID = "yinming_pocket";
    public static final String NETHER_RIVER_SECRET_REALM_ID = "nether_river_land";
    public static final String WILD_ANCIENT_TOMB_SECRET_REALM_ID = "wild_ancient_tomb";
    public static final String YINMING_DIMENSION_ID = "seeking_immortals:yin_ming_pocket";
    public static final String NETHER_RIVER_DIMENSION_ID = "seeking_immortals:nether_river_pocket";

    public static final String YIN_CORRUPTION_EVENT_ID = "yin_corruption_warning";
    public static final String YINMING_CORRUPTION_EVENT_ID = "yinming_corruption_warning";
    public static final String YIN_WIND_EVENT_ID = "yin_wind_howl";
    public static final String YIN_LUO_PATROL_EVENT_ID = "yin_luo_patrol";
    public static final String GHOST_WAIL_EVENT_ID = "ghost_wail_night";
    public static final String NETHER_RIVER_GHOST_WAIL_EVENT_ID = "nether_river_ghost_wail_night";
    public static final String NETHER_RIVER_FOG_EVENT_ID = "nether_river_fog";

    private static final int BASE_INTERVAL_TICKS = 240;
    private static final int NETHER_RIVER_INTERVAL_TICKS = 220;
    private static final int YIN_SURGE_INTERVAL_TICKS = 160;
    private static final int GHOST_WAIL_INTERVAL_TICKS = 120;
    private static final int NETHER_FOG_INTERVAL_TICKS = 200;
    private static final int BASE_EFFECT_DURATION_TICKS = 280;
    private static final int INTENSE_EFFECT_DURATION_TICKS = 360;
    private static final int MESSAGE_INTERVAL_TICKS = 1200;

    private YinUnderworldHazard() {}

    public static Profile profile(String currentRegionId, String activeSecretRealmId, String dimensionId,
                                  String activeDailyEventId) {
        boolean yinming = isYinming(currentRegionId, activeSecretRealmId, dimensionId);
        boolean netherRiver = isNetherRiver(currentRegionId, activeSecretRealmId, dimensionId);
        if (!yinming && !netherRiver) {
            return Profile.NONE;
        }

        String eventId = normalize(activeDailyEventId);
        int interval = netherRiver ? NETHER_RIVER_INTERVAL_TICKS : BASE_INTERVAL_TICKS;
        int duration = BASE_EFFECT_DURATION_TICKS;
        int slownessAmplifier = 0;
        int weaknessAmplifier = -1;
        int nauseaTicks = 0;
        int divineDrain = 1;
        float damage = netherRiver ? 0.75F : 0.5F;
        float minimumHealth = 4.0F;

        if (isYinSurgeEvent(eventId)) {
            interval = Math.min(interval, YIN_SURGE_INTERVAL_TICKS);
            duration = INTENSE_EFFECT_DURATION_TICKS;
            slownessAmplifier = 1;
            weaknessAmplifier = 0;
            divineDrain++;
            damage += 0.5F;
        }
        if (isGhostWailEvent(eventId)) {
            interval = Math.min(interval, GHOST_WAIL_INTERVAL_TICKS);
            duration = INTENSE_EFFECT_DURATION_TICKS;
            weaknessAmplifier = Math.max(weaknessAmplifier, 1);
            nauseaTicks = 120;
            divineDrain++;
        }
        if (NETHER_RIVER_FOG_EVENT_ID.equals(eventId)) {
            interval = Math.min(interval, NETHER_FOG_INTERVAL_TICKS);
            duration = INTENSE_EFFECT_DURATION_TICKS;
            slownessAmplifier = 1;
        }

        return new Profile(true, interval, duration, slownessAmplifier, weaknessAmplifier,
                nauseaTicks, divineDrain, damage, minimumHealth, MESSAGE_INTERVAL_TICKS);
    }

    public static boolean isUnderworld(String currentRegionId, String activeSecretRealmId, String dimensionId) {
        return isYinming(currentRegionId, activeSecretRealmId, dimensionId)
                || isNetherRiver(currentRegionId, activeSecretRealmId, dimensionId);
    }

    static boolean isYinming(String currentRegionId, String activeSecretRealmId, String dimensionId) {
        return YINMING_REGION_ID.equals(normalize(currentRegionId))
                || YINMING_SECRET_REALM_ID.equals(normalize(activeSecretRealmId))
                || YINMING_DIMENSION_ID.equals(normalize(dimensionId));
    }

    static boolean isNetherRiver(String currentRegionId, String activeSecretRealmId, String dimensionId) {
        String realmId = normalize(activeSecretRealmId);
        return NETHER_RIVER_REGION_ID.equals(normalize(currentRegionId))
                || NETHER_RIVER_SECRET_REALM_ID.equals(realmId)
                || WILD_ANCIENT_TOMB_SECRET_REALM_ID.equals(realmId)
                || NETHER_RIVER_DIMENSION_ID.equals(normalize(dimensionId));
    }

    static boolean isYinSurgeEvent(String eventId) {
        String id = normalize(eventId);
        return YIN_CORRUPTION_EVENT_ID.equals(id)
                || YINMING_CORRUPTION_EVENT_ID.equals(id)
                || YIN_WIND_EVENT_ID.equals(id)
                || YIN_LUO_PATROL_EVENT_ID.equals(id);
    }

    static boolean isGhostWailEvent(String eventId) {
        String id = normalize(eventId);
        return GHOST_WAIL_EVENT_ID.equals(id)
                || NETHER_RIVER_GHOST_WAIL_EVENT_ID.equals(id);
    }

    private static String normalize(String id) {
        return id == null ? "" : id.trim().toLowerCase(Locale.ROOT);
    }

    public record Profile(boolean active, int intervalTicks, int effectDurationTicks, int slownessAmplifier,
                          int weaknessAmplifier, int nauseaTicks, int divineConsciousnessDrain, float damage,
                          float minimumHealth, int messageIntervalTicks) {
        public static final Profile NONE = new Profile(false, 0, 0, 0, -1, 0, 0, 0.0F, 0.0F, 0);

        public boolean shouldApply(long tickCount) {
            return active && intervalTicks > 0 && tickCount % intervalTicks == 0;
        }

        public boolean shouldMessage(long tickCount) {
            return active && messageIntervalTicks > 0 && tickCount % messageIntervalTicks == 0;
        }

        public float safeDamage(float health) {
            if (!active || damage <= 0.0F || health <= minimumHealth) {
                return 0.0F;
            }
            return Math.min(damage, health - minimumHealth);
        }

        public Profile mitigatedByYinProtection() {
            if (!active) {
                return this;
            }
            int mitigatedInterval = Math.max(intervalTicks + 1, intervalTicks * 2);
            int mitigatedDuration = Math.max(40, effectDurationTicks / 2);
            int mitigatedSlowness = slownessAmplifier <= 0 ? -1 : slownessAmplifier - 1;
            int mitigatedDrain = Math.max(0, divineConsciousnessDrain - 1);
            return new Profile(true, mitigatedInterval, mitigatedDuration, mitigatedSlowness, -1,
                    0, mitigatedDrain, damage * 0.5F, minimumHealth, messageIntervalTicks);
        }
    }
}
