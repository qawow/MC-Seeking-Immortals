package com.xunxian.seekingimmortals.visual;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VisualDomainRuntimeEventsTest {
    @Test
    void refreshThrottleOnlyBecomesDueAfterTheFullInterval() {
        assertTrue(VisualDomainRuntimeEvents.emissionDue(null, 100L, 20));
        assertFalse(VisualDomainRuntimeEvents.emissionDue(100L, 101L, 20));
        assertFalse(VisualDomainRuntimeEvents.emissionDue(100L, 119L, 20));
        assertTrue(VisualDomainRuntimeEvents.emissionDue(100L, 120L, 20));
        assertTrue(VisualDomainRuntimeEvents.emissionDue(120L, 10L, 20));
        assertFalse(VisualDomainRuntimeEvents.emissionDue(100L, 100L, 0));
        assertTrue(VisualDomainRuntimeEvents.emissionDue(100L, 101L, 0));
    }

    @Test
    void deadEntitiesDoNotReplayTheTerminalStopOnLevelLeave() throws Exception {
        String source = Files.readString(Path.of(
                "src", "main", "java", "com", "xunxian", "seekingimmortals",
                "visual", "VisualDomainRuntimeEvents.java"));
        int leaveHandler = source.indexOf("public static void onEntityLeave");
        int deathGuard = source.indexOf("living.isDeadOrDying()", leaveHandler);
        int bossStop = source.indexOf("if (entity instanceof Mob mob", deathGuard);
        assertTrue(leaveHandler >= 0);
        assertTrue(deathGuard > leaveHandler);
        assertTrue(bossStop > deathGuard);
    }
}
