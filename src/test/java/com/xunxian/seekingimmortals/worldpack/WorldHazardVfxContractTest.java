package com.xunxian.seekingimmortals.worldpack;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class WorldHazardVfxContractTest {
    @Test
    void hazardsExposeTransitionPulseAndExitVisuals() throws Exception {
        String service = Files.readString(Path.of(
                "src", "main", "java", "com", "xunxian", "seekingimmortals",
                "worldpack", "WorldHazardVfxService.java"));
        for (String hazard : new String[]{"DIYUAN", "YIN_UNDERWORLD", "DEMON_RIFT"}) {
            assertTrue(service.contains(hazard), hazard);
        }
        assertTrue(service.contains("Kind.STATUS"));
        assertTrue(service.contains("Kind.IMPACT"));
        assertTrue(service.contains("Kind.DISSIPATE"));
        assertTrue(service.contains("previousActive"));
        assertTrue(service.contains("Map<UUID, EnumMap<Hazard, VisualState>> STATES"));
        assertTrue(service.contains("public static void clear(ServerPlayer player)"));
        assertTrue(!service.contains("getPersistentData()"));
        assertTrue(service.contains("AuthoredVisualCatalog.resolve(\"status:\" + profileId)"));
        assertTrue(service.contains("VisualEventDispatcher.entity(level, \"status\", profileId"));
        assertTrue(service.contains("VisualEventPacket.Lifecycle.START"));
        assertTrue(service.contains("VisualEventPacket.Lifecycle.STOP"));

        String events = Files.readString(Path.of(
                "src", "main", "java", "com", "xunxian", "seekingimmortals",
                "event", "ModEvents.java"));
        assertTrue(events.contains("WorldHazardVfxService.transition("));
        assertTrue(events.contains("WorldHazardVfxService.pulse("));
    }
}
