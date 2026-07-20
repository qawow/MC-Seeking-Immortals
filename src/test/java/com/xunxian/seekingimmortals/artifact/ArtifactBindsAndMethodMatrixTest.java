package com.xunxian.seekingimmortals.artifact;

import com.xunxian.seekingimmortals.skill.MethodLayerTechniqueService;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArtifactBindsAndMethodMatrixTest {
    @Test
    void vehicleAndQuestBindsAreConsumedInActivationService() throws Exception {
        String source = Files.readString(Path.of(
                "src", "main", "java", "com", "xunxian", "seekingimmortals",
                "artifact", "ArtifactActivationService.java"));
        assertTrue(source.contains("resolveVehicleBind"));
        assertTrue(source.contains("applyQuestBind"));
        assertTrue(source.contains("FlightVehicleService.board"));
        assertTrue(source.contains("WorldpackGameplayService.enterSecretRealm"));
        assertTrue(source.contains("artifact.binds()"));
    }

    @Test
    void emptyLifestyleMatricesAreExplicitlyOneLayer() {
        assertEquals(1, MethodLayerTechniqueService.maxLayers("huangfeng_alchemy_scripture"));
        assertEquals(1, MethodLayerTechniqueService.maxLayers("qixuan_mortal_art"));
        assertEquals(1, MethodLayerTechniqueService.maxLayers("sect_specialty_alchemy"));
        assertEquals(1, MethodLayerTechniqueService.maxLayers("juvenile_sect_art"));
        assertEquals(1, MethodLayerTechniqueService.matrixTotalLayers("huangfeng_alchemy_scripture"));
        // 0.2.107: lifestyle/generic matrices now grant executable techniques.
        assertFalse(MethodLayerTechniqueService.techniquesForLayer("huangfeng_alchemy_scripture", 1).isEmpty());
        assertFalse(MethodLayerTechniqueService.techniquesForLayer("qixuan_mortal_art", 1).isEmpty());
        assertFalse(MethodLayerTechniqueService.techniquesForLayer("generic_cultivation_qi_refining", 1).isEmpty());
        assertTrue(MethodLayerTechniqueService.techniquesForLayer("generic_cultivation_qi_refining", 9).size()
                >= MethodLayerTechniqueService.techniquesForLayer("generic_cultivation_qi_refining", 1).size());
    }

    @Test
    void genericCultivationMatricesAreNineLayers() {
        assertEquals(9, MethodLayerTechniqueService.maxLayers("generic_cultivation_qi_refining"));
        assertEquals(9, MethodLayerTechniqueService.maxLayers("generic_cultivation_foundation"));
        assertEquals(9, MethodLayerTechniqueService.maxLayers("generic_cultivation_nascent_soul"));
        assertEquals(9, MethodLayerTechniqueService.matrixTotalLayers("generic_cultivation_core_formation"));
    }

    @Test
    void catalogArtifactsWithBindsStayPresent() {
        var snap = ArtifactDataService.builtin();
        assertEquals("spirit_boat", snap.findArtifact("spirit_boat_model").orElseThrow().binds());
        assertEquals("cloud_sedan", snap.findArtifact("cloud_sedan_token").orElseThrow().binds());
        assertEquals("bone_wind_cart_vehicle", snap.findArtifact("bone_wind_cart").orElseThrow().binds());
        assertEquals("void_palace", snap.findArtifact("void_key").orElseThrow().binds());
        assertEquals("blood_forbidden_side", snap.findArtifact("montain_five_friends_token").orElseThrow().binds());
        assertEquals("great_jin_auction", snap.findArtifact("auction_sealed_hammer").orElseThrow().binds());
    }
}
