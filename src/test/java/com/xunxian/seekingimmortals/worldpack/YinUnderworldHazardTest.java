package com.xunxian.seekingimmortals.worldpack;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class YinUnderworldHazardTest {
    @Test
    void detectsYinmingAndNetherRiverFromRegionRealmOrDimension() {
        assertTrue(YinUnderworldHazard.isUnderworld("yinming", "", ""));
        assertTrue(YinUnderworldHazard.isUnderworld("", "yinming_pocket", ""));
        assertTrue(YinUnderworldHazard.isUnderworld("", "", "seeking_immortals:yin_ming_pocket"));
        assertTrue(YinUnderworldHazard.isUnderworld("nether_river", "", ""));
        assertTrue(YinUnderworldHazard.isUnderworld("", "nether_river_land", ""));
        assertTrue(YinUnderworldHazard.isUnderworld("", "wild_ancient_tomb", ""));
        assertTrue(YinUnderworldHazard.isUnderworld("", "", "seeking_immortals:nether_river_pocket"));
        assertFalse(YinUnderworldHazard.isUnderworld("tianyuan", "", "minecraft:overworld"));
    }

    @Test
    void baseProfilesApplyOnTheirOwnIntervals() {
        YinUnderworldHazard.Profile yinming = YinUnderworldHazard.profile("yinming", "", "", "");
        YinUnderworldHazard.Profile netherRiver = YinUnderworldHazard.profile("nether_river", "", "", "");

        assertTrue(yinming.active());
        assertEquals(240, yinming.intervalTicks());
        assertTrue(yinming.shouldApply(240));
        assertFalse(yinming.shouldApply(239));
        assertEquals(220, netherRiver.intervalTicks());
        assertTrue(netherRiver.shouldApply(440));
    }

    @Test
    void dailyEventsIntensifyYinPressure() {
        YinUnderworldHazard.Profile corruption = YinUnderworldHazard.profile(
                "yinming", "", "", "yin_corruption_warning");
        YinUnderworldHazard.Profile ghostWail = YinUnderworldHazard.profile(
                "nether_river", "", "", "nether_river_ghost_wail_night");
        YinUnderworldHazard.Profile fog = YinUnderworldHazard.profile(
                "nether_river", "", "", "nether_river_fog");

        assertEquals(160, corruption.intervalTicks());
        assertEquals(1, corruption.slownessAmplifier());
        assertEquals(2, corruption.divineConsciousnessDrain());
        assertEquals(120, ghostWail.intervalTicks());
        assertEquals(1, ghostWail.weaknessAmplifier());
        assertEquals(120, ghostWail.nauseaTicks());
        assertEquals(200, fog.intervalTicks());
        assertEquals(1, fog.slownessAmplifier());
    }

    @Test
    void hazardDamageNeverDropsPlayerBelowFloor() {
        YinUnderworldHazard.Profile profile = YinUnderworldHazard.profile(
                "yinming", "", "", "yin_corruption_warning");

        assertEquals(1.0F, profile.safeDamage(20.0F), 0.0001F);
        assertEquals(0.25F, profile.safeDamage(4.25F), 0.0001F);
        assertEquals(0.0F, profile.safeDamage(4.0F), 0.0001F);
    }

    @Test
    void yinProtectionMitigatesHazardCadenceAndEffects() {
        YinUnderworldHazard.Profile profile = YinUnderworldHazard.profile(
                "nether_river", "", "", "nether_river_ghost_wail_night");
        YinUnderworldHazard.Profile protectedProfile = profile.mitigatedByYinProtection();

        assertEquals(profile.intervalTicks() * 2, protectedProfile.intervalTicks());
        assertFalse(protectedProfile.shouldApply(profile.intervalTicks()));
        assertTrue(protectedProfile.shouldApply(profile.intervalTicks() * 2L));
        assertEquals(profile.effectDurationTicks() / 2, protectedProfile.effectDurationTicks());
        assertEquals(-1, protectedProfile.weaknessAmplifier());
        assertEquals(0, protectedProfile.nauseaTicks());
        assertTrue(protectedProfile.divineConsciousnessDrain() < profile.divineConsciousnessDrain());
        assertEquals(profile.damage() * 0.5F, protectedProfile.damage(), 0.0001F);
    }
}
