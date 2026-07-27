package com.xunxian.seekingimmortals.client;

import com.google.gson.JsonParser;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LodestoneVfxContractTest {
    private static final Path JAVA_ROOT = Path.of(
            "src", "main", "java", "com", "xunxian", "seekingimmortals");
    private static final Path ASSET_ROOT = Path.of(
            "src", "main", "resources", "assets", "seeking_immortals");
    private static final Path AUTHORED_SPELLS = Path.of(
            "src", "main", "resources", "data", "seeking_immortals", "visual",
            "authored_spell_effects.json");

    @Test
    void customParticleDescriptionsResolveTheirTextureSprites() throws Exception {
        Path descriptions = ASSET_ROOT.resolve("particles");
        try (var files = Files.list(descriptions)) {
            List<Path> jsonFiles = files
                    .filter(path -> path.toString().endsWith(".json"))
                    .sorted()
                    .toList();
            assertFalse(jsonFiles.isEmpty());
            for (Path jsonFile : jsonFiles) {
                String id = jsonFile.getFileName().toString().replaceFirst("\\.json$", "");
                var textures = JsonParser.parseString(read(jsonFile))
                        .getAsJsonObject()
                        .getAsJsonArray("textures");
                assertEquals(1, textures.size(), jsonFile.toString());
                assertEquals("seeking_immortals:" + id, textures.get(0).getAsString(), jsonFile.toString());
                assertTrue(Files.isRegularFile(ASSET_ROOT.resolve(Path.of(
                        "textures", "particle", id + ".png"))), id);
            }
        }
    }

    @Test
    void lodestoneRuntimeImportsStayClientOnly() throws Exception {
        try (var sources = Files.walk(JAVA_ROOT)) {
            List<Path> imports = sources
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> read(path).contains("import team.lodestar.lodestone"))
                    .toList();
            assertTrue(imports.size() >= 2, imports.toString());
            Path sharedParticleRegistry = JAVA_ROOT.resolve(Path.of("registry", "ModParticles.java"));
            assertTrue(imports.stream().allMatch(path -> path.startsWith(JAVA_ROOT.resolve("client"))
                            || path.equals(sharedParticleRegistry)),
                    imports.toString());
        }
    }

    @Test
    void clientRendererHasDistanceQualityAndPerTickBudgets() throws Exception {
        String renderer = read(JAVA_ROOT.resolve(Path.of("client", "LodestoneTechniqueVfx.java")));
        assertTrue(renderer.contains("MAX_PARTICLES_PER_TICK"));
        assertTrue(renderer.contains("ClientVisualEngine.remainingParticleBudget"));
        assertTrue(renderer.contains("ModParticles.QI_SOFT"));
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
        assertTrue(renderer.contains("ClientVisualEngine.particlesUsed(level) == particlesBefore"));
        assertTrue(renderer.contains("eventParticleCap"));
        assertTrue(renderer.contains("remainingParticleBudget"));
        assertTrue(renderer.contains("ClientVisualEngine.claimPostEffect()"));
        int thunderBranch = renderer.indexOf(
                "style == TechniqueVfxPacket.ParticleStyle.THUNDER_ARC");
        int thunderReturn = renderer.indexOf("return;", thunderBranch);
        assertTrue(thunderBranch >= 0);
        assertTrue(thunderReturn > thunderBranch);
        assertTrue(renderer.substring(thunderBranch, thunderReturn)
                .contains("ModParticles.THUNDER_ARC"));
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
    void authoredSpellShapesReachDistinctClientGeometry() throws Exception {
        String renderer = read(JAVA_ROOT.resolve(Path.of("client", "LodestoneTechniqueVfx.java")));

        assertTrue(renderer.contains("VisualProgram visualProgram"));
        assertTrue(renderer.contains("emitVisualProgram(level, packet, active"));
        assertTrue(renderer.contains("if (active.authoredProgram)"));
        assertTrue(renderer.contains("layer.primitive().id()"));

        for (String branch : List.of(
                "case \"giant_claw\" -> giantClawShape(",
                "case \"fist_barrage\" -> barrageShape(",
                "case \"sword_rain\", \"projectile_swarm\", \"falling_barrage\" ->",
                "case \"cloud_vortex\" -> vortexShape(",
                "case \"rune_orbit\", \"array_rings\" -> runeOrbitShape(",
                "case \"chain_net\", \"chain_links\" -> chainNetShape(",
                "case \"spirit_avatar\", \"summon_gate\" -> spiritAvatarShape(",
                "case \"serpent_dragon\" -> serpentShape(",
                "case \"ground_field\", \"sphere_field\" ->",
                "case \"seal_cage\", \"barrier_plane\" -> cageShape(")) {
            assertTrue(renderer.contains(branch), branch);
        }
        for (String helper : List.of(
                "giantClawShape", "barrageShape", "vortexShape", "runeOrbitShape",
                "chainNetShape", "spiritAvatarShape", "serpentShape", "cageShape")) {
            assertTrue(renderer.contains("private static void " + helper + "("), helper);
        }
    }

    @Test
    void figureRenderingSharesQuotasWithoutDroppingLateComponents() throws Exception {
        String renderer = read(JAVA_ROOT.resolve(Path.of("client", "LodestoneTechniqueVfx.java")));

        assertTrue(renderer.contains("VfxBudgetPlan.sampledCopies("));
        assertTrue(renderer.contains("minimumCopyBudget(layer.primitive())"));
        assertTrue(renderer.contains("withSubBudget(copyQuota"));
        assertTrue(renderer.contains("VfxBudgetPlan.components("));
        assertTrue(occurrences(renderer, "emitFigureComponents(level") >= 34);
        assertTrue(renderer.contains("int samples = budgetedSamples(points + 1)"));
        assertTrue(renderer.contains("lineSampleProgress(sample, samples)"));
        assertTrue(renderer.contains("status == ParticleStatus.MINIMAL ? 7"));
        assertTrue(renderer.contains("status == ParticleStatus.DECREASED ? 16 : 30"));
        for (String helper : List.of(
                "ritualBowlShape", "magicRulerShape", "giantHammerShape",
                "magicStaffShape", "ritualLampShape", "spiritQinShape",
                "ritualCoffinShape", "talismanBrushShape", "magicFanShape",
                "alchemyFurnaceShape", "magicScrollShape", "formationDiscShape",
                "spikedClubShape")) {
            assertTrue(renderer.contains("private static void " + helper + "("), helper);
        }
        for (String branch : List.of(
                "case RITUAL_BOWL ->", "case MAGIC_RULER ->", "case GIANT_HAMMER ->",
                "case MAGIC_STAFF ->", "case RITUAL_LAMP ->", "case SPIRIT_QIN ->",
                "case RITUAL_COFFIN ->", "case TALISMAN_BRUSH ->", "case MAGIC_FAN ->",
                "case ALCHEMY_FURNACE ->", "case MAGIC_SCROLL ->",
                "case FORMATION_DISC ->", "case SPIKED_CLUB ->")) {
            assertTrue(renderer.contains(branch), branch);
        }
    }

    @Test
    void everyGeneratedSpellShapeHasAnExplicitRendererBranch() throws Exception {
        var profiles = JsonParser.parseString(read(AUTHORED_SPELLS))
                .getAsJsonObject().getAsJsonArray("profiles");
        Set<String> generated = new HashSet<>();
        int auraBursts = 0;
        int singleProjectiles = 0;
        for (var element : profiles) {
            String shape = element.getAsJsonObject().get("shape").getAsString();
            generated.add(shape);
            auraBursts += "aura_burst".equals(shape) ? 1 : 0;
            singleProjectiles += "single_projectile".equals(shape) ? 1 : 0;
        }

        String renderer = read(JAVA_ROOT.resolve(Path.of("client", "LodestoneTechniqueVfx.java")));
        int switchStart = renderer.indexOf("private static void emitAuthoredShape");
        int switchEnd = renderer.indexOf("private static void giantClawShape", switchStart);
        String authoredSwitch = renderer.substring(switchStart, switchEnd);
        Matcher matcher = Pattern.compile("\\\"([a-z_]+)\\\"").matcher(authoredSwitch);
        Set<String> rendered = new HashSet<>();
        while (matcher.find()) {
            rendered.add(matcher.group(1));
        }

        assertEquals(generated, rendered);
        assertTrue(generated.size() >= 50, generated.toString());
        assertTrue(auraBursts < profiles.size() * 0.15D, "aura_burst=" + auraBursts);
        assertTrue(singleProjectiles < profiles.size() * 0.10D,
                "single_projectile=" + singleProjectiles);
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
        assertTrue(geometry.contains("private void refreshVisualIdentity()"));
        assertTrue(geometry.contains("getVisualProfileId()"));
        assertTrue(geometry.contains("textures/effect/beam_soft.png"));
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
    void buildDeclaresMandatoryLodestoneAndProtocolThirty() throws Exception {
        String build = Files.readString(Path.of("build.gradle"));
        String mods = Files.readString(Path.of("src", "main", "resources", "META-INF", "mods.toml"));
        String network = read(JAVA_ROOT.resolve(Path.of("network", "ModNetwork.java")));
        assertTrue(build.contains("maven.modrinth:lodestonelib:${lodestone_version}"));
        assertTrue(mods.contains("modId=\"lodestone\""));
        assertTrue(mods.contains("mandatory=true"));
        assertTrue(network.contains("PROTOCOL_VERSION = \"30\""));
        assertTrue(network.contains("TechniqueVfxPacket.class"));
        assertTrue(network.contains("VisualEventPacket.class"));
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
