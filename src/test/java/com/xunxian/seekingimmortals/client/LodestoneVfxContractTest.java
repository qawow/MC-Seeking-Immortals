package com.xunxian.seekingimmortals.client;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
            assertTrue(imports.size() >= 2, imports.toString());
            assertTrue(imports.stream().allMatch(path -> path.startsWith(JAVA_ROOT.resolve("client"))),
                    imports.toString());
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
        assertFalse(renderer.contains("getEntities((Entity) null"));
        assertTrue(renderer.contains("MAX_SHAKES_PER_TICK"));
        assertTrue(renderer.contains("MAX_ACTIVE_VFX"));
        assertTrue(renderer.contains("ANTICIPATION"));
        assertTrue(renderer.contains("RELEASE"));
        assertTrue(renderer.contains("SUSTAIN"));
        assertTrue(renderer.contains("AFTERGLOW"));
        assertTrue(renderer.contains("activeVfxCursor"));
        assertTrue(renderer.contains("emittedEvents"));
        assertTrue(renderer.contains("releaseAnchor("));
        assertTrue(renderer.contains("phase == Phase.RELEASE && localAge == 0"));
        assertTrue(renderer.contains("particlesThisTick == particlesBefore"));
        assertTrue(renderer.contains("eventParticleCap"));
        assertTrue(renderer.contains("remainingParticleBudget"));
        assertTrue(renderer.contains("distanceToSegmentSqr"));
        assertTrue(renderer.contains("COLOR_CACHE"));
        assertTrue(renderer.contains("0.74F"));
        assertTrue(renderer.contains("8.0F, 32.0F"));
        assertTrue(renderer.contains("setIntensity(strength, 0.0F)"));
        assertTrue(renderer.contains("Math.min(intensity, 35)"));
        assertTrue(renderer.indexOf("embellish(level, packet.kind()")
                < renderer.indexOf("switch (packet.kind())"));
    }

    @Test
    void lodestoneWorldGeometryUsesBoundedEventTrackedBeamsAndTrails() throws Exception {
        String geometry = read(JAVA_ROOT.resolve(Path.of("client", "LodestoneWorldGeometry.java")));
        String events = read(JAVA_ROOT.resolve(Path.of("client", "ClientEvents.java")));

        assertTrue(geometry.contains("MAX_TRACKED_PROJECTILES"));
        assertTrue(geometry.contains("MAX_TRANSIENT_BEAMS"));
        assertTrue(geometry.contains("PROJECTILE_AFTERGLOW_TICKS"));
        assertTrue(geometry.contains("VFXBuilders.createWorld()"));
        assertTrue(geometry.contains("TrailPointBuilder.create"));
        assertTrue(geometry.contains("renderBeam("));
        assertTrue(geometry.contains("renderTrail("));
        assertTrue(geometry.contains("LodestoneVfxMath.beamFacingReference("));
        assertTrue(geometry.contains("getPartialTick()"));
        assertTrue(geometry.contains("entity.getPosition(Mth.clamp(partialTick"));
        assertTrue(geometry.contains("position.subtract(camera)"));
        assertTrue(geometry.contains("trail.entity == entity"));
        assertTrue(geometry.contains("projectileTickCursor = (start + Math.max(1, scanned))"));
        assertTrue(geometry.contains("private void refreshFamily()"));
        assertTrue(geometry.contains("ParticleStatus.MINIMAL"));
        assertTrue(read(JAVA_ROOT.resolve(Path.of("client", "LodestoneVfxMath.java")))
                .contains("distanceToSegmentSqr"));
        assertTrue(events.contains("EntityJoinLevelEvent"));
        assertTrue(events.contains("EntityLeaveLevelEvent"));
        assertTrue(events.contains("RenderLevelStageEvent.Stage.AFTER_PARTICLES"));
    }

    @Test
    void geometryMathKeepsSegmentLodAndAxialBeamsStable() {
        assertEquals(4.0D, LodestoneVfxMath.distanceToSegmentSqr(
                new Vec3(3.0D, 2.0D, 0.0D), Vec3.ZERO, new Vec3(6.0D, 0.0D, 0.0D)), 1.0E-8D);

        Vec3 start = Vec3.ZERO;
        Vec3 end = new Vec3(0.0D, 0.0D, 6.0D);
        Vec3 camera = new Vec3(0.0D, 0.0D, -1.0D);
        Vec3 reference = LodestoneVfxMath.beamFacingReference(start, end, camera);
        assertTrue(start.subtract(reference).cross(end.subtract(start)).lengthSqr() > 1.0E-6D);
    }

    @Test
    void sharedTechniqueAndFormationPulsesUseBoundedVisualIntents() throws Exception {
        String palette = read(JAVA_ROOT.resolve(Path.of("skill", "effect", "TechniqueVfxPalette.java")));
        for (String kind : List.of("CAST", "BURST", "PATH", "AURA", "SCAN", "BEAM", "CONE", "IMPACT")) {
            assertTrue(palette.contains("TechniqueVfxPacket.Kind." + kind), kind);
        }

        String formations = read(JAVA_ROOT.resolve(Path.of("structure", "FormationFieldService.java")));
        assertTrue(formations.contains("MAX_PULSE_TARGET_VFX = 8"));
        assertTrue(formations.contains("MAX_VFX_PACKETS_PER_DIMENSION_TICK = 48"));
        assertTrue(formations.contains(
                "MAX_PENDING_VFX_PER_DIMENSION = MAX_VFX_PACKETS_PER_DIMENSION_TICK * 8"));
        assertTrue(formations.contains("flushPendingVfx(level);"));
        assertTrue(formations.contains("emitPulseVisuals(level, dim, pulseVisuals);"));
        assertTrue(formations.contains("emitFormationVfx(level, visual.field());"));
        assertTrue(formations.contains("TechniqueVfxPacket.Kind.FORMATION"));
        assertTrue(formations.indexOf("emitFormationVfx(level, visual.field());")
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
