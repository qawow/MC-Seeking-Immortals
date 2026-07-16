package com.xunxian.seekingimmortals.spiritual;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpiritualAuraManagerTest {
    @Test
    void auraInfoRecordIncludesClusterFlag() {
        SpiritualAuraManager.AuraInfo info = new SpiritualAuraManager.AuraInfo(
                100, 1.0D, 1.0D, 3.0D, 0, SpiritualAuraManager.AuraNature.NORMAL, true, true);
        assertTrue(info.leyline());
        assertTrue(info.cluster());
        assertEquals(100, info.concentration());
        assertEquals(1.0D, info.regionMultiplier(), 1e-9);
    }

    @Test
    void auraInfoRecordIncludesRegionFields() {
        SpiritualAuraManager.AuraInfo info = new SpiritualAuraManager.AuraInfo(
                180, 1.0D, 1.0D, 1.0D, 0, SpiritualAuraManager.AuraNature.SPIRIT_REALM, false, false,
                "tianyuan", 2.0D);
        assertEquals("tianyuan", info.regionId());
        assertEquals(2.0D, info.regionMultiplier(), 1e-9);
        assertEquals(180, info.concentration());
    }

    @Test
    void regionMultiplierUsesRegistryAndClamps() {
        double tianyuan = SpiritualAuraManager.getRegionMultiplier("tianyuan");
        assertTrue(tianyuan >= 1.5D);
        assertTrue(tianyuan <= 2.5D);
        assertEquals(1.0D, SpiritualAuraManager.getRegionMultiplier(""), 1e-9);
    }

    @Test
    void majorLeylineSeedHelpersAreStable() {
        long seed = 123456789L;
        int majors = 0;
        for (int x = -32; x <= 32; x++) {
            for (int z = -32; z <= 32; z++) {
                if (SpiritualAuraManager.isMajorLeylineChunk(seed, x, z)) {
                    majors++;
                    assertTrue(SpiritualAuraManager.majorLeylineTier(seed, x, z) >= 1);
                    assertTrue(SpiritualAuraManager.getLeylineCoreMultiplier(seed, x, z) >= 3.0D);
                } else {
                    assertEquals(0, SpiritualAuraManager.majorLeylineTier(seed, x, z));
                    assertEquals(1.0D, SpiritualAuraManager.getLeylineCoreMultiplier(seed, x, z), 1e-9);
                }
            }
        }
        // ~2% of chunks are major cores before cluster bonus.
        assertTrue(majors > 20);
        assertTrue(majors < 400);
        assertFalse(SpiritualAuraManager.isMajorLeylineChunk(seed, 0, 0)
                && SpiritualAuraManager.majorLeylineTier(seed, 0, 0) == 0);
    }
}
