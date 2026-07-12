package com.xunxian.seekingimmortals.worldpack;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SecretRealmDimensionServiceTest {
    @Test
    void mapsDedicatedDimensions() {
        assertTrue(SecretRealmDimensionService.dedicatedDimensionCount() >= 4);
        assertTrue(SecretRealmDimensionService.dimensionIdFor("blood_forbidden").isPresent());
        assertTrue(SecretRealmDimensionService.dimensionIdFor("void_palace").isPresent());
        assertTrue(SecretRealmDimensionService.dimensionIdFor("mist_cave_trial").isPresent());
        assertTrue(SecretRealmDimensionService.dimensionIdFor("fallen_demon_valley").isPresent());
    }
}
