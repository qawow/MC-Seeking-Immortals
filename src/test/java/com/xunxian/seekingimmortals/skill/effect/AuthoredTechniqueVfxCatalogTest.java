package com.xunxian.seekingimmortals.skill.effect;

import com.xunxian.seekingimmortals.cultivation.TechniqueDataManager;
import com.xunxian.seekingimmortals.network.TechniqueVfxPacket;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthoredTechniqueVfxCatalogTest {
    @Test
    void authoredProfilesCoverEveryFrameSheetTechniqueWithoutUnknownReferences() {
        assertEquals(344, AuthoredTechniqueVfxCatalog.profiles().size());
        Set<String> runtimeIds = TechniqueDataManager.builtinTechniques().keySet();
        assertTrue(AuthoredTechniqueVfxCatalog.profiles().keySet().stream().allMatch(runtimeIds::contains));
        AuthoredTechniqueVfxCatalog.profiles().values().forEach(profile -> {
            assertNotNull(profile.particle());
            assertNotNull(profile.trail());
            assertNotEquals(TechniqueVfxPacket.ParticleStyle.DEFAULT, profile.particle(), profile.id());
            assertNotEquals(TechniqueVfxPacket.TrailStyle.DEFAULT, profile.trail(), profile.id());
            assertTrue(profile.frameCount() >= 5 && profile.frameCount() <= 6,
                    profile.id() + " frame count");
        });
        assertEquals(40, AuthoredTechniqueVfxCatalog.profiles().values().stream()
                .filter(AuthoredTechniqueVfxCatalog.Profile::telegraphed)
                .count());
    }

    @Test
    void duplicateFrameSheetsUseSpecificSchoolResolution() {
        AuthoredTechniqueVfxCatalog.Profile inverse = AuthoredTechniqueVfxCatalog.find("inverse_star_veil").orElseThrow();
        assertEquals(TechniqueVfxPacket.ParticleStyle.QI_SOFT, inverse.particle());
        assertEquals(TechniqueVfxPacket.TrailStyle.NONE, inverse.trail());

        AuthoredTechniqueVfxCatalog.Profile armor =
                AuthoredTechniqueVfxCatalog.find("golden_armor_talisman_cast").orElseThrow();
        assertEquals(TechniqueVfxPacket.ParticleStyle.METAL_SPARK, armor.particle());
        assertEquals("talisman", armor.school());
    }

    @Test
    void missingTechniqueFallsBackWithoutInventingAnAuthoredProfile() {
        assertFalse(AuthoredTechniqueVfxCatalog.find("not_in_author_corpus").isPresent());
    }
}
