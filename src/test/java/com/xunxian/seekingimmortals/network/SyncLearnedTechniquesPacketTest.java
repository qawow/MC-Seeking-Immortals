package com.xunxian.seekingimmortals.network;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class SyncLearnedTechniquesPacketTest {
    @Test
    void remainingCooldownsUseProvidedGlobalTimebase() {
        Map<String, Integer> remaining = SyncLearnedTechniquesPacket.remainingCooldownTicks(
                Map.of(
                        "big_dipper_sword_array", 1_160L,
                        "ready_spell", 900L),
                1_000L);

        assertEquals(160, remaining.get("big_dipper_sword_array"));
        assertFalse(remaining.containsKey("ready_spell"));
    }

    @Test
    void remainingCooldownsClampHugeDurations() {
        Map<String, Integer> remaining = SyncLearnedTechniquesPacket.remainingCooldownTicks(
                Map.of("ancient_seal", Long.MAX_VALUE),
                0L);

        assertEquals(Integer.MAX_VALUE, remaining.get("ancient_seal"));
    }
}
