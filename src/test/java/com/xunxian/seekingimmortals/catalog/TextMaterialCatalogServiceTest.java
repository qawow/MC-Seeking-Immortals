package com.xunxian.seekingimmortals.catalog;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TextMaterialCatalogServiceTest {
    @Test
    void loadsSecretRealmFlavorsFromTemplate() {
        assertEquals(14, TextMaterialCatalogService.builtin().secretRealmFlavors().size());
        assertTrue(TextMaterialCatalogService.builtin().findFlavor("blood_forbidden").isPresent());
        assertTrue(TextMaterialCatalogService.builtin().findFlavor("void_palace").isPresent());
    }

    @Test
    void loadsManualsMethodsAndFlightBindings() {
        assertEquals(21, ManualCatalogService.manualCount());
        assertEquals(91, ManualCatalogService.methodCount());
        assertEquals(8, FlightVehicleService.vehicleCount());
        assertTrue(FlightVehicleService.find("wind_feather_raft").isPresent());
    }
}
