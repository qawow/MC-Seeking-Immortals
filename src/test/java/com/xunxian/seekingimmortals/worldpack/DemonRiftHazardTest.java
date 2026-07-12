package com.xunxian.seekingimmortals.worldpack;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DemonRiftHazardTest {
    @Test
    void detectsFallenDemonAreaFromRegionRealmOrDimension() {
        assertTrue(DemonRiftHazard.isDemonRiftArea("fallen_demon_valley", "", ""));
        assertTrue(DemonRiftHazard.isDemonRiftArea("", "fallen_demon_valley", ""));
        assertTrue(DemonRiftHazard.isDemonRiftArea("", "fallen_demon_depths", ""));
        assertTrue(DemonRiftHazard.isDemonRiftArea("", "", "seeking_immortals:demon_rift"));
        assertFalse(DemonRiftHazard.isDemonRiftArea("tianyuan", "", "minecraft:overworld"));
    }

    @Test
    void baseAndDemonRiftProfilesUseDifferentCadence() {
        DemonRiftHazard.Profile fallenDemon = DemonRiftHazard.profile("fallen_demon_valley", "", "", "");
        DemonRiftHazard.Profile riftDimension = DemonRiftHazard.profile("", "", "seeking_immortals:demon_rift", "");

        assertTrue(fallenDemon.active());
        assertEquals(240, fallenDemon.intervalTicks());
        assertTrue(fallenDemon.shouldApply(240));
        assertFalse(fallenDemon.shouldApply(239));
        assertEquals(1, fallenDemon.qiDeviationRisk());
        assertEquals(180, riftDimension.intervalTicks());
        assertEquals(2, riftDimension.divineConsciousnessDrain());
        assertEquals(320, riftDimension.darknessTicks());
    }

    @Test
    void dailyEventsIntensifyDemonPressure() {
        DemonRiftHazard.Profile demonQi = DemonRiftHazard.profile(
                "fallen_demon_valley", "", "", "demon_qi_surge");
        DemonRiftHazard.Profile miasma = DemonRiftHazard.profile(
                "fallen_demon_valley", "", "", "fallen_demon_miasma");
        DemonRiftHazard.Profile sealBreach = DemonRiftHazard.profile(
                "fallen_demon_valley", "", "", "ancient_demon_seal_breach");
        DemonRiftHazard.Profile voidRift = DemonRiftHazard.profile(
                "fallen_demon_valley", "", "", "void_rift_sighting");

        assertEquals(160, demonQi.intervalTicks());
        assertEquals(1, demonQi.weaknessAmplifier());
        assertEquals(2, demonQi.qiDeviationRisk());
        assertEquals(120, miasma.intervalTicks());
        assertEquals(160, miasma.confusionTicks());
        assertEquals(3, miasma.qiDeviationRisk());
        assertEquals(120, sealBreach.intervalTicks());
        assertEquals(2, sealBreach.weaknessAmplifier());
        assertEquals(3, sealBreach.divineConsciousnessDrain());
        assertEquals(200, voidRift.intervalTicks());
        assertEquals(0, voidRift.slownessAmplifier());
    }

    @Test
    void dajinDemonQiEventIsHazardButOrdinaryDajinIsSafe() {
        assertFalse(DemonRiftHazard.profile("dajin", "", "", "").active());
        DemonRiftHazard.Profile dajinSurge = DemonRiftHazard.profile(
                "dajin", "", "", "dajin_demon_qi_surge");

        assertTrue(dajinSurge.active());
        assertEquals(160, dajinSurge.intervalTicks());
        assertEquals(2, dajinSurge.qiDeviationRisk());
    }

    @Test
    void hazardDamageNeverDropsPlayerBelowFloor() {
        DemonRiftHazard.Profile profile = DemonRiftHazard.profile(
                "", "", "seeking_immortals:demon_rift", "ancient_demon_seal_breach");

        assertEquals(2.0F, profile.safeDamage(20.0F), 0.0001F);
        assertEquals(0.25F, profile.safeDamage(4.25F), 0.0001F);
        assertEquals(0.0F, profile.safeDamage(4.0F), 0.0001F);
    }
}
