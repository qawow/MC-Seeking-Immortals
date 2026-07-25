package com.xunxian.seekingimmortals.worldpack;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecretRealmOpenPolicyTest {
    @Test
    void authoredCyclesUseTheirDeclaredYearCounts() {
        assertEquals(5 * 360, SecretRealmOpenPolicy.cycleLengthDays("cycle_blood_forbidden"));
        assertEquals(300 * 360, SecretRealmOpenPolicy.cycleLengthDays("cycle_void_palace"));
        assertEquals(0, SecretRealmOpenPolicy.cycleLengthDays("cycle_kunwu_open"));
        assertEquals(0, SecretRealmOpenPolicy.cycleLengthDays("cycle_guanghan_fragment"));
    }

    @Test
    void cycleWindowsRepeatWithinTheFullAuthoredPeriod() {
        int bloodCycle = SecretRealmOpenPolicy.cycleLengthDays("cycle_blood_forbidden");
        assertFalse(SecretRealmOpenPolicy.isCycleWindowOpen(0L, bloodCycle, List.of(10, 30)));
        assertTrue(SecretRealmOpenPolicy.isCycleWindowOpen(9L, bloodCycle, List.of(10, 30)));
        assertTrue(SecretRealmOpenPolicy.isCycleWindowOpen(29L, bloodCycle, List.of(10, 30)));
        assertFalse(SecretRealmOpenPolicy.isCycleWindowOpen(30L, bloodCycle, List.of(10, 30)));
        assertTrue(SecretRealmOpenPolicy.isCycleWindowOpen(
                bloodCycle + 9L, bloodCycle, List.of(10, 30)));
    }

    @Test
    void unknownCycleNeverSilentlyDefaultsToThirtyDays() {
        assertTrue(SecretRealmOpenPolicy.validateCycle(
                "cycle_kunwu_open", List.of(), 10L).isPresent());
    }

    @Test
    void voidPalaceKeyIsOwnedByTheAtomicGateReservation() throws Exception {
        String policy = Files.readString(Path.of("src", "main", "java", "com", "xunxian",
                "seekingimmortals", "worldpack", "SecretRealmOpenPolicy.java"));
        assertFalse(policy.contains("void_palace_key_fragment"),
                "the policy runs after the gate has already reserved the fragment");

        String nodes = Files.readString(Path.of("src", "main", "resources", "data",
                "seeking_immortals", "catalog", "spatial_nodes_index.json"));
        int gate = nodes.indexOf("\"type\": \"cycle_gate\"");
        assertTrue(gate >= 0);
        assertTrue(nodes.substring(gate, Math.min(nodes.length(), gate + 500))
                .contains("void_palace_key_fragment"));
    }
}
