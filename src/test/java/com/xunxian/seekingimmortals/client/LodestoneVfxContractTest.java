package com.xunxian.seekingimmortals.client;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LodestoneVfxContractTest {
    private static final Path JAVA_ROOT = Path.of(
            "src", "main", "java", "com", "xunxian", "seekingimmortals");

    @Test
    void lodestoneRuntimeImportsStayClientOnly() throws Exception {
        try (var sources = Files.walk(JAVA_ROOT)) {
            List<Path> imports = sources
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> read(path).contains("import team.lodestar.lodestone"))
                    .toList();
            assertEquals(1, imports.size(), imports.toString());
            assertTrue(imports.get(0).startsWith(JAVA_ROOT.resolve("client")), imports.toString());
        }
    }

    @Test
    void clientRendererHasDistanceQualityAndPerTickBudgets() throws Exception {
        String renderer = read(JAVA_ROOT.resolve(Path.of("client", "LodestoneTechniqueVfx.java")));
        assertTrue(renderer.contains("MAX_PARTICLES_PER_TICK"));
        assertTrue(renderer.contains("MAX_VIEW_DISTANCE_SQR"));
        assertTrue(renderer.contains("ParticleStatus.MINIMAL"));
        assertTrue(renderer.contains("lodScale("));
        assertTrue(renderer.contains("tickProjectiles("));
        assertTrue(renderer.contains("WorldParticleBuilder.create"));
        assertTrue(renderer.contains("LUMITRANSPARENT.withDepthFade()"));
        assertTrue(renderer.contains("LodestoneWorldParticleRenderType.ADDITIVE"));
        assertTrue(renderer.contains("ExtrudingSparkBehaviorComponent"));
        assertTrue(renderer.contains("PositionedScreenshakeInstance"));
        assertTrue(renderer.contains("getBoundingBox().inflate(64.0D)"));
        assertTrue(renderer.contains("MAX_SHAKES_PER_TICK"));
        assertTrue(renderer.contains("COLOR_CACHE"));
        assertTrue(renderer.contains("0.74F"));
        assertTrue(renderer.contains("8.0F, 32.0F"));
        assertTrue(renderer.contains("setIntensity(strength, 0.0F)"));
        assertTrue(renderer.contains("Math.min(intensity, 35)"));
        assertTrue(renderer.indexOf("embellish(level, packet.kind()")
                < renderer.indexOf("switch (packet.kind())"));
    }

    @Test
    void sharedTechniqueAndEveryFormationPulseEmitOneVisualIntent() throws Exception {
        String palette = read(JAVA_ROOT.resolve(Path.of("skill", "effect", "TechniqueVfxPalette.java")));
        for (String kind : List.of("CAST", "BURST", "PATH", "AURA", "SCAN", "BEAM", "CONE", "IMPACT")) {
            assertTrue(palette.contains("TechniqueVfxPacket.Kind." + kind), kind);
        }

        String formations = read(JAVA_ROOT.resolve(Path.of("structure", "FormationFieldService.java")));
        assertTrue(formations.contains("emitFormationVfx(level, field);"));
        assertTrue(formations.contains("TechniqueVfxPacket.Kind.FORMATION"));
        assertTrue(formations.indexOf("emitFormationVfx(level, field);")
                > formations.indexOf("case CATALOG_GENERIC"));
    }

    @Test
    void buildDeclaresMandatoryLodestoneAndProtocolTwentyEight() throws Exception {
        String build = Files.readString(Path.of("build.gradle"));
        String mods = Files.readString(Path.of("src", "main", "resources", "META-INF", "mods.toml"));
        String network = read(JAVA_ROOT.resolve(Path.of("network", "ModNetwork.java")));
        assertTrue(build.contains("maven.modrinth:lodestonelib:${lodestone_version}"));
        assertTrue(mods.contains("modId=\"lodestone\""));
        assertTrue(mods.contains("mandatory=true"));
        assertTrue(network.contains("PROTOCOL_VERSION = \"28\""));
        assertTrue(network.contains("TechniqueVfxPacket.class"));
    }

    @Test
    void allSuccessfulTechniquePathsUseSemanticOrchestrator() throws Exception {
        String release = read(JAVA_ROOT.resolve(Path.of("network", "ReleaseTechniquePacket.java")));
        assertEquals(2, occurrences(release, "TechniqueVfxOrchestrator.emitSuccessfulCast("));
        assertEquals(2, occurrences(release, "TechniqueVfxPacket.captureSynchronousIntents()"));
        assertTrue(release.contains("vfxCapture.packets(), false"));
        assertTrue(release.contains("vfxCapture.packets(), true"));

        String artifact = read(JAVA_ROOT.resolve(Path.of("artifact", "ArtifactActiveSkillService.java")));
        assertTrue(artifact.contains("TechniqueVfxPacket.captureSynchronousIntents()"));
        assertTrue(artifact.contains("vfxCapture.packets()"));

        String packet = read(JAVA_ROOT.resolve(Path.of("network", "TechniqueVfxPacket.java")));
        assertTrue(packet.contains("STATUS"));
        assertTrue(packet.contains("DISSIPATE"));
        for (String motif : List.of("BLADE", "SHIELD", "DOMAIN", "TELEPORT", "SUMMON",
                "WALL", "CHAIN", "RAIN", "HEAL", "SEAL", "FORMATION", "ILLUSION")) {
            assertTrue(packet.contains(motif), motif);
        }
        assertTrue(packet.contains("isCaptureCandidate(packet)"));
        assertTrue(packet.contains("catch (RuntimeException ignored)"));

        String orchestrator = read(JAVA_ROOT.resolve(Path.of(
                "skill", "effect", "TechniqueVfxOrchestrator.java")));
        assertTrue(orchestrator.contains("capturedIntents"));
        assertTrue(orchestrator.contains("selectSemantic"));
    }

    private static String read(Path path) {
        try {
            return Files.readString(path);
        } catch (Exception exception) {
            throw new IllegalStateException(path.toString(), exception);
        }
    }

    private static int occurrences(String source, String needle) {
        int count = 0;
        int cursor = 0;
        while ((cursor = source.indexOf(needle, cursor)) >= 0) {
            count++;
            cursor += needle.length();
        }
        return count;
    }
}
