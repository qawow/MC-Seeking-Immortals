package com.xunxian.seekingimmortals.client;

import net.minecraft.client.ParticleStatus;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Focused source/runtime contract checks for the client visual lifecycle bridge. */
class ClientVisualEngineContractTest {
    private static final Path JAVA_ROOT = Path.of(
            "src", "main", "java", "com", "xunxian", "seekingimmortals");

    @Test
    void sharedBudgetsAndCapsRemainStableAcrossQualityLevels() {
        assertEquals(192, ClientVisualEngine.PARTICLES_ALL);
        assertEquals(112, ClientVisualEngine.PARTICLES_DECREASED);
        assertEquals(48, ClientVisualEngine.PARTICLES_MINIMAL);
        assertEquals(48, ClientVisualEngine.GEOMETRY_ALL);
        assertEquals(24, ClientVisualEngine.GEOMETRY_DECREASED);
        assertEquals(10, ClientVisualEngine.GEOMETRY_MINIMAL);
        assertEquals(96, ClientVisualEngine.VISIBLE_INSTANCE_LIMIT);
        assertEquals(256, ClientVisualEngine.HARD_INSTANCE_LIMIT);
        assertEquals(4, ClientVisualEngine.POST_EFFECT_LIMIT);
        assertEquals(192, ClientVisualEngine.particleLimit(ParticleStatus.ALL));
        assertEquals(112, ClientVisualEngine.particleLimit(ParticleStatus.DECREASED));
        assertEquals(48, ClientVisualEngine.particleLimit(ParticleStatus.MINIMAL));
        assertEquals(48, ClientVisualEngine.geometryLimit(ParticleStatus.ALL));
        assertEquals(24, ClientVisualEngine.geometryLimit(ParticleStatus.DECREASED));
        assertEquals(10, ClientVisualEngine.geometryLimit(ParticleStatus.MINIMAL));
    }

    @Test
    void lifecycleRegistryHandlesPersistentUpdatesStopsAndTtl() throws Exception {
        String engine = read(JAVA_ROOT.resolve(Path.of("client", "ClientVisualEngine.java")));
        assertTrue(engine.contains("Lifecycle.EVENT"));
        assertTrue(engine.contains("Lifecycle.STOP"));
        assertTrue(engine.contains("packet.persistent()"));
        assertTrue(engine.contains("current.update(packet)"));
        assertTrue(engine.contains("INSTANCES.remove(packet.instanceKey())"));
        assertTrue(engine.contains("packet.ageTicks() >= packet.durationTicks()"));
        assertTrue(engine.contains("ANCHOR_GRACE_TICKS = 40"));
        assertTrue(engine.contains("instance.missingAnchorTicks++"));
        assertTrue(engine.contains("instance.missingAnchorTicks > ANCHOR_GRACE_TICKS"));
        assertTrue(engine.contains("instance.age++"));
        assertTrue(engine.contains("private static ClientLevel activeLevel"));
        assertTrue(engine.split("ensureLevel\\(level\\);", -1).length - 1 >= 2);
        assertTrue(engine.contains("if (activeLevel == level)"));
        assertTrue(engine.contains("clearRuntimeState();"));
        assertTrue(engine.contains("activeLevel = null;"));
    }

    @Test
    void unifiedProfileAndExactArgbPrecedeLegacyFacades() throws Exception {
        String engine = read(JAVA_ROOT.resolve(Path.of("client", "ClientVisualEngine.java")));
        int unified = engine.indexOf("VisualProfile unified = resolveUnifiedProfile(packet)");
        int artifact = engine.indexOf("AuthoredArtifactVfxCatalog.find(id)");
        int consumable = engine.indexOf("AuthoredConsumableVfxCatalog.findPill(id)");
        int technique = engine.indexOf("AuthoredTechniqueVfxCatalog.find(id)");
        assertTrue(unified >= 0);
        assertTrue(unified < artifact);
        assertTrue(unified < consumable);
        assertTrue(unified < technique);
        assertTrue(engine.contains("AuthoredVisualCatalog.resolve(domain + \":\" + id)"));
        assertTrue(engine.contains("unified.primaryArgbInt()"));
        assertTrue(engine.contains("style.primaryArgb()"));
        assertTrue(engine.contains("handleProfile(packet.profileKey(), styled, style.primaryArgb())"));

        String techniqueRenderer = read(JAVA_ROOT.resolve(Path.of("client", "LodestoneTechniqueVfx.java")));
        String geometry = read(JAVA_ROOT.resolve(Path.of("client", "LodestoneWorldGeometry.java")));
        assertTrue(techniqueRenderer.contains("handleProfile(ResourceLocation profileKey, TechniqueVfxPacket packet"));
        assertTrue(techniqueRenderer.contains("PaletteColors.fromArgb(primaryArgb)"));
        assertTrue(geometry.contains("addProfileIntent(ResourceLocation profileKey, TechniqueVfxPacket packet"));
        assertTrue(geometry.contains("builder(buffers, beam.family, alpha * quality,"));
        assertTrue(geometry.contains("beam.primaryArgb"));
        assertTrue(geometry.contains("builder(buffers, trail.currentFamily(), alpha,"));
        assertTrue(geometry.contains("trail.currentPrimaryArgb()"));
    }

    @Test
    void authoredTimelineAndStatesDriveTheProductionScheduler() throws Exception {
        String engine = read(JAVA_ROOT.resolve(Path.of("client", "ClientVisualEngine.java")));
        String planner = read(JAVA_ROOT.resolve(Path.of("visual", "VisualTimelinePlan.java")));

        assertTrue(engine.contains("handleTimelineEvent(level, packet)"));
        assertTrue(engine.contains("VisualTimelinePlan.select(resolveUnifiedProfile(packet), packet.trigger(), looping)"));
        assertTrue(engine.contains("instance.timeline.activeAt(instance.age)"));
        assertTrue(engine.contains("applyTimelineStyle(baseStyle, timelineEvent)"));
        assertTrue(planner.contains("profile.timeline()"));
        assertTrue(planner.contains("profile.states()"));
        assertTrue(planner.contains("profile.hasState(candidate)"));

        assertTrue(engine.contains("makeRoom(packet.priority())"));
        assertTrue(engine.contains("\"@timeline/\" + sequence"));
        assertTrue(engine.contains("INSTANCES.put(key, instance)"));
        assertTrue(engine.contains("transientTimeline ? timeline.expired(age)"));
        assertTrue(engine.contains("if (!instance.timeline.isEmpty())"));
    }

    @Test
    void clientHooksAndAppendOnlyPacketRegistrationStayConnected() throws Exception {
        String handlers = read(JAVA_ROOT.resolve(Path.of("client", "ClientPacketHandlers.java")));
        String events = read(JAVA_ROOT.resolve(Path.of("client", "ClientEvents.java")));
        String network = read(JAVA_ROOT.resolve(Path.of("network", "ModNetwork.java")));
        assertTrue(handlers.contains("handleVisualEvent(VisualEventPacket packet)"));
        assertTrue(handlers.contains("ClientVisualEngine.handle(packet)"));
        assertTrue(events.contains("ClientVisualEngine.tick();"));
        assertTrue(events.contains("ClientVisualEngine.render(event);"));
        assertTrue(events.contains("ClientVisualEngine.reset();"));
        assertTrue(events.contains("AuthoredStatusOverlay.render"));
        String particleProviders = read(JAVA_ROOT.resolve(Path.of("client", "ModParticleProviders.java")));
        assertTrue(particleProviders.contains("value = Dist.CLIENT"));
        int legacy = network.indexOf("TechniqueVfxPacket.class");
        int lifecycle = network.indexOf("VisualEventPacket.class");
        assertTrue(legacy >= 0);
        assertTrue(lifecycle > legacy);
        assertTrue(network.contains("PROTOCOL_VERSION = \"30\""));
    }

    @Test
    void authoredOverlayAndModelAnimationHaveDistinctBoundedRuntimes() throws Exception {
        String engine = read(JAVA_ROOT.resolve(Path.of("client", "ClientVisualEngine.java")));
        String events = read(JAVA_ROOT.resolve(Path.of("client", "ClientEvents.java")));
        String overlay = read(JAVA_ROOT.resolve(Path.of("client", "ClientVisualOverlayRuntime.java")));
        String animation = read(JAVA_ROOT.resolve(Path.of("client", "ClientModelAnimationRuntime.java")));

        assertTrue(engine.contains("VisualAction.SCREEN_OVERLAY"));
        assertTrue(engine.contains("ClientVisualOverlayRuntime.push("));
        assertTrue(engine.contains("VisualAction.MODEL_ANIMATION"));
        assertTrue(engine.contains("ClientModelAnimationRuntime.trigger("));
        assertTrue(engine.contains("ClientVisualOverlayRuntime.reset();"));
        assertTrue(engine.contains("ClientModelAnimationRuntime.reset();"));
        assertTrue(events.contains("registerAboveAll(\"authored_visual_overlay\""));
        assertTrue(events.contains("ClientVisualOverlayRuntime.tick();"));
        assertTrue(overlay.contains("Math.min(200, durationTicks)"));
        assertTrue(overlay.contains("Math.min(96, authoredIntensity)"));
        assertTrue(animation.contains("MAX_STATES = 128"));
        assertTrue(animation.contains("living.swing(InteractionHand.MAIN_HAND, true)"));
    }

    private static String read(Path path) throws Exception {
        return Files.readString(path);
    }
}
