package com.xunxian.seekingimmortals.artifact;

import com.xunxian.seekingimmortals.network.TechniqueVfxPacket;
import com.xunxian.seekingimmortals.skill.effect.TechniqueVfxPalette;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArtifactVfxContractTest {
    @Test
    void everyGenericFallbackArtifactHasSemanticVisuals() {
        int fallbackCount = 0;
        List<String> neutralFamilies = new ArrayList<>();
        for (ArtifactDataService.ArtifactDefinition artifact
                : ArtifactDataService.builtin().artifacts().values()) {
            ArtifactActivationService.ActivationInfo info =
                    ArtifactActivationService.activationInfo(artifact.id()).orElse(null);
            if (info == null || ArtifactActiveSkillService.hasTechniqueMapping(artifact.id())) {
                continue;
            }
            fallbackCount++;
            assertNotEquals(TechniqueVfxPacket.Motif.GENERIC,
                    ArtifactActivationService.artifactVfxMotif(
                            info.kind(), artifact.id(), artifact.effect()), artifact.id());
            if (ArtifactActivationService.artifactVfxFamily(
                    info.kind(), artifact.id(), artifact.effect()) == TechniqueVfxPalette.Family.NEUTRAL) {
                neutralFamilies.add(artifact.id());
            }
        }
        assertTrue(fallbackCount >= 50, "expected the known generic fallback corpus, got " + fallbackCount);
        assertTrue(neutralFamilies.isEmpty(), "neutral fallback artifacts: " + neutralFamilies);
    }

    @Test
    void artifactAndRefinementSemanticsChooseDistinctGeometry() {
        assertEquals(TechniqueVfxPacket.Kind.PATH,
                ArtifactActivationService.artifactVfxKind("teleport_protection"));
        assertEquals(TechniqueVfxPacket.Motif.TELEPORT,
                ArtifactActivationService.artifactVfxMotif(
                        "teleport_protection", "great_shift_token", "space_shift"));
        assertEquals(TechniqueVfxPalette.Family.VOID,
                ArtifactActivationService.artifactVfxFamily(
                        "teleport_protection", "great_shift_token", "space_shift"));
        assertEquals(TechniqueVfxPacket.Motif.CHANNEL,
                ArtifactActivationService.artifactVfxMotif(
                        "sound", "sound_attack_bell", "sound_stun_aoe"));
        assertEquals(TechniqueVfxPacket.Kind.BURST,
                ArtifactActivationService.artifactVfxKind("sound"));
        assertEquals(TechniqueVfxPacket.Kind.FORMATION,
                ArtifactActivationService.artifactVfxKind("magnet"));
        assertEquals(TechniqueVfxPacket.Kind.BEAM,
                ArtifactActivationService.artifactVfxKind("capture"));
        assertEquals(TechniqueVfxPacket.Kind.IMPACT,
                ArtifactActivationService.artifactVfxKind("soul_destroy"));
        assertEquals(4.0D,
                ArtifactActivationService.artifactVfxAimDistance("soul_destroy", 8));
        assertTrue(ArtifactActivationService.artifactVfxRadius("magnet", 8) > 7.0D);
        assertEquals(TechniqueVfxPalette.Family.METAL,
                ArtifactActivationService.artifactVfxFamily(
                        "offense", "generic_treasure_grade_4", ""));
        assertEquals(TechniqueVfxPacket.Motif.RAIN,
                ArtifactActivationService.artifactVfxMotif(
                        "offense", "generic_treasure_grade_7", ""));

        ArtifactDataService.RefinementRecipe thunderRod = new ArtifactDataService.RefinementRecipe(
                "refine_thunder_rod", "thunder_rod", "", "", "", 4, 0.6D, List.of());
        ArtifactDataService.RefinementRecipe spiritMirror = new ArtifactDataService.RefinementRecipe(
                "refine_soul_mirror", "soul_mirror", "", "", "", 4, 0.6D, List.of());
        assertEquals(TechniqueVfxPalette.Family.THUNDER,
                ArtifactRefinementService.refinementFamily(thunderRod));
        assertEquals(TechniqueVfxPalette.Family.SOUL,
                ArtifactRefinementService.refinementFamily(spiritMirror));
        assertEquals(TechniqueVfxPacket.Motif.ILLUSION,
                ArtifactRefinementService.refinementMotif(spiritMirror));
    }

    @Test
    void genericActivationAndRefinementEmitOnlyAfterResolvedOutcome() throws Exception {
        String activation = Files.readString(Path.of(
                "src", "main", "java", "com", "xunxian", "seekingimmortals",
                "artifact", "ArtifactActivationService.java"));
        String refinement = Files.readString(Path.of(
                "src", "main", "java", "com", "xunxian", "seekingimmortals",
                "artifact", "ArtifactRefinementService.java"));

        assertTrue(activation.indexOf("emitGenericActivationVfx(player, artifact, info, activationStart)")
                > activation.indexOf("applyActivation(player, cultivation, artifact, info, powerScale)"));
        assertTrue(refinement.contains("playFeedback(player, true, recipe)"));
        assertTrue(refinement.contains("playFeedback(player, false, recipe)"));
        assertTrue(refinement.contains("TechniqueVfxPacket.Kind.DISSIPATE"));
    }
}
