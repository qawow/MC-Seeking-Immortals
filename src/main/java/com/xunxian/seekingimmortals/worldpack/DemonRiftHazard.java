package com.xunxian.seekingimmortals.worldpack;

import java.util.Locale;

public final class DemonRiftHazard {
    public static final String FALLEN_DEMON_REGION_ID = "fallen_demon_valley";
    public static final String DAJIN_REGION_ID = "dajin";
    public static final String FALLEN_DEMON_SECRET_REALM_ID = "fallen_demon_valley";
    public static final String FALLEN_DEMON_DEPTHS_SECRET_REALM_ID = "fallen_demon_depths";
    public static final String DEMON_RIFT_DIMENSION_ID = "seeking_immortals:demon_rift";

    public static final String DEMON_QI_SURGE_EVENT_ID = "demon_qi_surge";
    public static final String FALLEN_DEMON_QI_SURGE_EVENT_ID = "fallen_demon_qi_surge";
    public static final String DAJIN_DEMON_QI_SURGE_EVENT_ID = "dajin_demon_qi_surge";
    public static final String FALLEN_DEMON_MIASMA_EVENT_ID = "fallen_demon_miasma";
    public static final String ANCIENT_DEMON_SEAL_BREACH_EVENT_ID = "ancient_demon_seal_breach";
    public static final String VOID_RIFT_SIGHTING_EVENT_ID = "void_rift_sighting";
    public static final String TIANYUAN_VOID_RIFT_SIGHTING_EVENT_ID = "tianyuan_void_rift_sighting";

    private static final int BASE_INTERVAL_TICKS = 240;
    private static final int DEMON_RIFT_INTERVAL_TICKS = 180;
    private static final int DEMON_QI_SURGE_INTERVAL_TICKS = 160;
    private static final int MIASMA_INTERVAL_TICKS = 120;
    private static final int SEAL_BREACH_INTERVAL_TICKS = 120;
    private static final int VOID_RIFT_INTERVAL_TICKS = 200;
    private static final int BASE_EFFECT_DURATION_TICKS = 240;
    private static final int DEMON_RIFT_EFFECT_DURATION_TICKS = 320;
    private static final int INTENSE_EFFECT_DURATION_TICKS = 360;
    private static final int MIASMA_EFFECT_DURATION_TICKS = 400;
    private static final int SEAL_BREACH_EFFECT_DURATION_TICKS = 420;
    private static final int MESSAGE_INTERVAL_TICKS = 1200;

    private DemonRiftHazard() {}

    public static Profile profile(String currentRegionId, String activeSecretRealmId, String dimensionId,
                                  String activeDailyEventId) {
        String regionId = normalize(currentRegionId);
        String eventId = normalize(activeDailyEventId);
        boolean demonRiftDimension = DEMON_RIFT_DIMENSION_ID.equals(normalize(dimensionId));
        boolean fallenDemonArea = isFallenDemonArea(regionId, activeSecretRealmId, dimensionId);
        boolean dajinDemonQi = DAJIN_REGION_ID.equals(regionId) && isDemonQiEvent(eventId);
        if (!fallenDemonArea && !demonRiftDimension && !dajinDemonQi) {
            return Profile.NONE;
        }

        int interval = demonRiftDimension ? DEMON_RIFT_INTERVAL_TICKS : BASE_INTERVAL_TICKS;
        int duration = demonRiftDimension ? DEMON_RIFT_EFFECT_DURATION_TICKS : BASE_EFFECT_DURATION_TICKS;
        int darknessTicks = demonRiftDimension ? duration : 0;
        int slownessAmplifier = -1;
        int weaknessAmplifier = 0;
        int confusionTicks = 0;
        int divineDrain = demonRiftDimension ? 2 : 1;
        int qiDeviationRisk = demonRiftDimension ? 2 : 1;
        float damage = demonRiftDimension ? 1.25F : 0.75F;
        float minimumHealth = 4.0F;

        if (isDemonQiEvent(eventId)) {
            interval = Math.min(interval, DEMON_QI_SURGE_INTERVAL_TICKS);
            duration = Math.max(duration, INTENSE_EFFECT_DURATION_TICKS);
            darknessTicks = Math.max(darknessTicks, duration);
            weaknessAmplifier = Math.max(weaknessAmplifier, 1);
            confusionTicks = Math.max(confusionTicks, 80);
            divineDrain++;
            qiDeviationRisk++;
            damage += 0.5F;
        }
        if (FALLEN_DEMON_MIASMA_EVENT_ID.equals(eventId)) {
            interval = Math.min(interval, MIASMA_INTERVAL_TICKS);
            duration = Math.max(duration, MIASMA_EFFECT_DURATION_TICKS);
            darknessTicks = Math.max(darknessTicks, duration);
            slownessAmplifier = Math.max(slownessAmplifier, 0);
            weaknessAmplifier = Math.max(weaknessAmplifier, 1);
            confusionTicks = Math.max(confusionTicks, 160);
            divineDrain++;
            qiDeviationRisk += 2;
            damage += 0.5F;
        }
        if (ANCIENT_DEMON_SEAL_BREACH_EVENT_ID.equals(eventId)) {
            interval = Math.min(interval, SEAL_BREACH_INTERVAL_TICKS);
            duration = Math.max(duration, SEAL_BREACH_EFFECT_DURATION_TICKS);
            darknessTicks = Math.max(darknessTicks, duration);
            weaknessAmplifier = Math.max(weaknessAmplifier, 2);
            confusionTicks = Math.max(confusionTicks, 120);
            divineDrain += 2;
            qiDeviationRisk += 2;
            damage += 0.75F;
        }
        if (isVoidRiftEvent(eventId)) {
            interval = Math.min(interval, VOID_RIFT_INTERVAL_TICKS);
            duration = Math.max(duration, DEMON_RIFT_EFFECT_DURATION_TICKS);
            slownessAmplifier = Math.max(slownessAmplifier, 0);
            confusionTicks = Math.max(confusionTicks, 60);
            damage += 0.25F;
        }

        return new Profile(true, interval, duration, darknessTicks, slownessAmplifier, weaknessAmplifier,
                confusionTicks, divineDrain, qiDeviationRisk, damage, minimumHealth, MESSAGE_INTERVAL_TICKS);
    }

    public static boolean isDemonRiftArea(String currentRegionId, String activeSecretRealmId, String dimensionId) {
        return isFallenDemonArea(normalize(currentRegionId), activeSecretRealmId, dimensionId)
                || DEMON_RIFT_DIMENSION_ID.equals(normalize(dimensionId));
    }

    static boolean isFallenDemonArea(String currentRegionId, String activeSecretRealmId, String dimensionId) {
        String realmId = normalize(activeSecretRealmId);
        return FALLEN_DEMON_REGION_ID.equals(normalize(currentRegionId))
                || FALLEN_DEMON_SECRET_REALM_ID.equals(realmId)
                || FALLEN_DEMON_DEPTHS_SECRET_REALM_ID.equals(realmId)
                || DEMON_RIFT_DIMENSION_ID.equals(normalize(dimensionId));
    }

    static boolean isDemonQiEvent(String eventId) {
        String id = normalize(eventId);
        return DEMON_QI_SURGE_EVENT_ID.equals(id)
                || FALLEN_DEMON_QI_SURGE_EVENT_ID.equals(id)
                || DAJIN_DEMON_QI_SURGE_EVENT_ID.equals(id);
    }

    static boolean isVoidRiftEvent(String eventId) {
        String id = normalize(eventId);
        return VOID_RIFT_SIGHTING_EVENT_ID.equals(id)
                || TIANYUAN_VOID_RIFT_SIGHTING_EVENT_ID.equals(id);
    }

    private static String normalize(String id) {
        return id == null ? "" : id.trim().toLowerCase(Locale.ROOT);
    }

    public record Profile(boolean active, int intervalTicks, int effectDurationTicks, int darknessTicks,
                          int slownessAmplifier, int weaknessAmplifier, int confusionTicks,
                          int divineConsciousnessDrain, int qiDeviationRisk, float damage,
                          float minimumHealth, int messageIntervalTicks) {
        public static final Profile NONE = new Profile(false, 0, 0, 0, -1, -1, 0, 0, 0, 0.0F, 0.0F, 0);

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
    }
}
