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

    @Test
    void preservesMethodLayerAndPrerequisiteMetadata() {
        TextMaterialCatalogService.MethodEntry changchun = TextMaterialCatalogService.builtin()
                .findMethod("changchun_gong").orElseThrow();
        assertEquals(13, changchun.explicitMaxLayers());
        assertEquals("QI_REFINING", changchun.realmMaxLearn());
        assertTrue(changchun.requiredSpiritRoots().contains("wood"));
        assertEquals("huangfeng_valley", changchun.requiredFaction());
        assertEquals("FOUNDATION", changchun.mustConvertAfter());

        TextMaterialCatalogService.MethodEntry qingyuan = TextMaterialCatalogService.builtin()
                .findMethod("qingyuan_sword_art").orElseThrow();
        assertTrue(qingyuan.prerequisiteMethods().contains("changchun_gong"));
        assertEquals(13, qingyuan.prerequisiteMethodLayers().get("changchun_gong"));
        assertTrue(qingyuan.requiredSpiritRoots().contains("metal")
                || qingyuan.requiredSpiritRoots().contains("wood"));
        assertTrue(qingyuan.requiredItems().contains("flying_sword_low")
                || qingyuan.suggestedItems().contains("flying_sword_low"));

        assertEquals(2, TextMaterialCatalogService.builtin().findMethod("artifact_refining_basic")
                .orElseThrow().explicitMaxLayers());
        assertEquals(2, TextMaterialCatalogService.builtin().findMethod("treasure_appraisal_art")
                .orElseThrow().explicitMaxLayers());
    }
}
