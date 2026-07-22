package com.xunxian.seekingimmortals.artifact;

import com.xunxian.seekingimmortals.network.TechniqueVfxPacket;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthoredArtifactVfxCatalogTest {
    @Test
    void authoredLayersCoverTheRuntimeArtifactCatalog() {
        assertEquals(217, AuthoredArtifactVfxCatalog.profiles().size());
        assertEquals(217, ArtifactDataService.builtin().artifacts().size());
        assertTrue(ArtifactDataService.builtin().artifacts().keySet()
                .equals(AuthoredArtifactVfxCatalog.profiles().keySet()));

        AuthoredArtifactVfxCatalog.profiles().values().forEach(profile -> {
            assertNotEquals(TechniqueVfxPacket.ParticleStyle.DEFAULT, profile.particle(), profile.id());
            assertNotEquals(TechniqueVfxPacket.TrailStyle.DEFAULT, profile.trail(), profile.id());
            assertTrue(profile.states().keySet().containsAll(
                    java.util.Set.of("sheathed", "idle_bound", "active", "impact", "damaged", "broken")),
                    profile.id());
            assertEquals(profile.hasOrbit(), profile.states().containsKey("orbit"), profile.id());
            assertEquals(profile.hasLaunch(), profile.states().containsKey("launch"), profile.id());
            assertEquals(profile.hasOpen(), profile.states().containsKey("open"), profile.id());
            assertEquals(profile.hasReflect(), profile.states().containsKey("reflect"), profile.id());
        });
        assertEquals(10, AuthoredArtifactVfxCatalog.profiles().values().stream()
                .filter(AuthoredArtifactVfxCatalog.Profile::hasOrbit).count());
        assertEquals(10, AuthoredArtifactVfxCatalog.profiles().values().stream()
                .filter(AuthoredArtifactVfxCatalog.Profile::hasLaunch).count());
        assertEquals(4, AuthoredArtifactVfxCatalog.profiles().values().stream()
                .filter(AuthoredArtifactVfxCatalog.Profile::hasOpen).count());
        assertEquals(1, AuthoredArtifactVfxCatalog.profiles().values().stream()
                .filter(AuthoredArtifactVfxCatalog.Profile::hasReflect).count());
    }

    @Test
    void materialAndDeferredProfilesNeverOverrideActiveSkillGeometry() {
        long material = AuthoredArtifactVfxCatalog.profiles().values().stream()
                .filter(AuthoredArtifactVfxCatalog.Profile::materialOnly).count();
        long deferred = AuthoredArtifactVfxCatalog.profiles().values().stream()
                .filter(AuthoredArtifactVfxCatalog.Profile::deferred).count();
        assertEquals(18, material);
        assertEquals(1, deferred);
        assertTrue(AuthoredArtifactVfxCatalog.find("void_heaven_cauldron_shard").orElseThrow().materialOnly());
        assertTrue(AuthoredArtifactVfxCatalog.find("space_rift_compass").orElseThrow().deferred());
        assertFalse(AuthoredArtifactVfxOrchestratorShim.hasOverride("void_heaven_cauldron_shard"));
        assertFalse(AuthoredArtifactVfxOrchestratorShim.hasOverride("space_rift_compass"));
        assertTrue(AuthoredArtifactVfxOrchestratorShim.hasOverride("flying_sword_low"));
    }

    @Test
    void specialCauldronProvenanceAndStateHooksRemainExplicit() throws Exception {
        String generated = Files.readString(Path.of(
                "src", "main", "resources", "data", "seeking_immortals", "visual",
                "authored_artifact_vfx_profiles.json"));
        assertTrue(generated.contains("vis_ultra_void_cauldron"));
        String activation = Files.readString(Path.of(
                "src", "main", "java", "com", "xunxian", "seekingimmortals", "artifact",
                "ArtifactActivationService.java"));
        assertTrue(activation.contains("consumeIntegrityWithVfx"));
        assertTrue(activation.contains("ArtifactVfxOrchestrator.State.REPAIRED"));
        String ownership = Files.readString(Path.of(
                "src", "main", "java", "com", "xunxian", "seekingimmortals", "artifact",
                "ArtifactOwnershipService.java"));
        assertTrue(ownership.contains("ArtifactVfxOrchestrator.State.IDLE_BOUND"));
        assertTrue(ownership.contains("ArtifactVfxOrchestrator.State.AWAKENED"));
    }

    /** Keeps the test independent of a full server bootstrap while checking the public routing contract. */
    private static final class AuthoredArtifactVfxOrchestratorShim {
        private static boolean hasOverride(String id) {
            return ArtifactVfxOrchestrator.overrideFor(id) != null;
        }
    }
}
