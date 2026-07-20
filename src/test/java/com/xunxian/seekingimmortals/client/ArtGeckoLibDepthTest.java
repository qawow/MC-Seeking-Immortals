package com.xunxian.seekingimmortals.client;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Source/resource contracts for the 0.2.103 art/GeckoLib depth batch.
 * Avoids live Minecraft bootstrap.
 */
class ArtGeckoLibDepthTest {
    @Test
    void servitorHasAttackAndFloatAnimations() throws Exception {
        String anim = Files.readString(Path.of(
                "src", "main", "resources", "assets", "seeking_immortals",
                "animations", "summoned_servitor.animation.json"));
        assertTrue(anim.contains("\"attack\""));
        assertTrue(anim.contains("\"float_idle\""));
        assertTrue(anim.contains("\"walk\""));
        assertTrue(anim.contains("\"idle\""));
    }

    @Test
    void spiritBoatGeoAnimationAndTexturesExist() {
        assertTrue(Files.exists(Path.of(
                "src", "main", "resources", "assets", "seeking_immortals",
                "geo", "spirit_boat.geo.json")));
        assertTrue(Files.exists(Path.of(
                "src", "main", "resources", "assets", "seeking_immortals",
                "animations", "spirit_boat.animation.json")));
        assertTrue(Files.exists(Path.of(
                "src", "main", "resources", "assets", "seeking_immortals",
                "textures", "entity", "spirit_boat.png")));
        assertTrue(Files.exists(Path.of(
                "src", "main", "resources", "assets", "seeking_immortals",
                "textures", "entity", "spirit_boat_cloud.png")));
        for (String name : new String[]{
                "summoned_servitor.png",
                "summoned_servitor_beast.png",
                "summoned_servitor_puppet.png",
                "summoned_servitor_ghost.png"
        }) {
            assertTrue(Files.exists(Path.of(
                    "src", "main", "resources", "assets", "seeking_immortals",
                    "textures", "entity", name)), name);
        }
    }

    @Test
    void skillIconLibraryIsBroad() throws Exception {
        try (Stream<Path> stream = Files.list(Path.of(
                "src", "main", "resources", "assets", "seeking_immortals",
                "textures", "gui", "skill"))) {
            long count = stream.filter(p -> p.getFileName().toString().endsWith(".png")).count();
            assertTrue(count >= 300, "skill icons=" + count);
        }
    }

    @Test
    void runtimeWiresBoatRendererAndServitorAttackController() throws Exception {
        String boat = Files.readString(Path.of(
                "src", "main", "java", "com", "xunxian", "seekingimmortals",
                "entity", "SpiritBoatEntity.java"));
        assertTrue(boat.contains("implements GeoEntity"));
        assertTrue(boat.contains("thenLoop(\"fly\")"));

        String renderer = Files.readString(Path.of(
                "src", "main", "java", "com", "xunxian", "seekingimmortals",
                "client", "SpiritBoatRenderer.java"));
        assertTrue(renderer.contains("spirit_boat.geo.json"));
        assertTrue(renderer.contains("spirit_boat_cloud.png"));

        String events = Files.readString(Path.of(
                "src", "main", "java", "com", "xunxian", "seekingimmortals",
                "client", "ClientEvents.java"));
        assertTrue(events.contains("SpiritBoatRenderer::new"));

        String servitor = Files.readString(Path.of(
                "src", "main", "java", "com", "xunxian", "seekingimmortals",
                "entity", "SummonedServitorEntity.java"));
        assertTrue(servitor.contains("thenPlay(\"attack\")"));
        assertTrue(servitor.contains("float_idle"));

        String skin = Files.readString(Path.of(
                "src", "main", "java", "com", "xunxian", "seekingimmortals",
                "client", "ImmortalUiSkin.java"));
        assertTrue(skin.contains("loadKnownSkillIcons"));
        assertTrue(skin.contains("textures/gui/skill"));
    }
}
