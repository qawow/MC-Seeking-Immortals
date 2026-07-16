package com.xunxian.seekingimmortals.catalog;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TextMaterialCatalogServiceTest {
    @Test
    void loadsSecretRealmFlavorsFromTemplate() {
        // M09 expands flavor coverage to author 19 (+ optional yinming_pocket).
        assertTrue(TextMaterialCatalogService.builtin().secretRealmFlavors().size() >= 19);
        assertTrue(TextMaterialCatalogService.builtin().findFlavor("blood_forbidden").isPresent());
        assertTrue(TextMaterialCatalogService.builtin().findFlavor("void_palace").isPresent());
        assertTrue(TextMaterialCatalogService.builtin().findFlavor("seven_meridian_cave").isPresent());
    }

    @Test
    void loadsManualsMethodsAndFlightBindings() {
        assertEquals(21, ManualCatalogService.manualCount());
        assertEquals(136, ManualCatalogService.methodCount());
        assertEquals(8, FlightVehicleService.vehicleCount());
        assertTrue(FlightVehicleService.find("wind_feather_raft").isPresent());
    }
}
