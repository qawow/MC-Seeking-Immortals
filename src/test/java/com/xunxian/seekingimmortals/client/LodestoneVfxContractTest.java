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
    void buildDeclaresMandatoryLodestoneAndProtocolTwentySeven() throws Exception {
        String build = Files.readString(Path.of("build.gradle"));
        String mods = Files.readString(Path.of("src", "main", "resources", "META-INF", "mods.toml"));
        String network = read(JAVA_ROOT.resolve(Path.of("network", "ModNetwork.java")));
        assertTrue(build.contains("maven.modrinth:lodestonelib:${lodestone_version}"));
        assertTrue(mods.contains("modId=\"lodestone\""));
        assertTrue(mods.contains("mandatory=true"));
        assertTrue(network.contains("PROTOCOL_VERSION = \"27\""));
        assertTrue(network.contains("TechniqueVfxPacket.class"));
    }

    private static String read(Path path) {
        try {
            return Files.readString(path);
        } catch (Exception exception) {
            throw new IllegalStateException(path.toString(), exception);
        }
    }
}
