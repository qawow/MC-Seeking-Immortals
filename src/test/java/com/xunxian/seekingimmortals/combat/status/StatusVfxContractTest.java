package com.xunxian.seekingimmortals.combat.status;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class StatusVfxContractTest {
    @Test
    void customStatusesCoverApplyPulseRemoveAndExpiry() throws Exception {
        String events = Files.readString(Path.of(
                "src", "main", "java", "com", "xunxian", "seekingimmortals",
                "event", "StatusVfxEvents.java"));
        assertTrue(events.contains("MobEffectEvent.Added"));
        assertTrue(events.contains("MobEffectEvent.Remove"));
        assertTrue(events.contains("MobEffectEvent.Expired"));
        assertTrue(events.contains("emitApplied"));
        assertTrue(events.contains("emitDissipate"));

        String effect = Files.readString(Path.of(
                "src", "main", "java", "com", "xunxian", "seekingimmortals",
                "combat", "status", "SeekingStatusEffect.java"));
        assertTrue(effect.contains("StatusVfxService.emitPulse"));
        assertTrue(effect.contains("AMBIENT_VFX_INTERVAL_TICKS = 60"));

        String service = Files.readString(Path.of(
                "src", "main", "java", "com", "xunxian", "seekingimmortals",
                "combat", "status", "StatusVfxService.java"));
        assertTrue(service.contains("MAX_EMISSIONS_PER_TICK"));
        assertTrue(service.contains("throttlePulse"));
    }
}
