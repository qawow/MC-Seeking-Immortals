package com.xunxian.seekingimmortals.client;

import com.xunxian.seekingimmortals.network.SyncWorldpackDataPacket;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientWorldpackDataTest {
    private static final long NANOS_PER_TICK = 50_000_000L;

    @AfterEach
    void resetSnapshot() {
        ClientWorldpackData.reset();
    }

    @Test
    void snapshotRevisionAdvancesForEveryServerRefresh() {
        long before = ClientWorldpackData.get().revision();
        SyncWorldpackDataPacket packet = packet(200L, 100L);

        ClientWorldpackData.set(packet);
        long first = ClientWorldpackData.get().revision();
        ClientWorldpackData.set(packet);
        long second = ClientWorldpackData.get().revision();

        assertTrue(first > before);
        assertTrue(second > first);
    }

    @Test
    void countdownUsesElapsedClientTimeAndClampsAtZero() {
        long received = 1_000_000_000L;

        assertEquals(40L, ClientWorldpackData.Snapshot.remainingTicks(
                60L, received, received + 20L * NANOS_PER_TICK));
        assertEquals(0L, ClientWorldpackData.Snapshot.remainingTicks(
                60L, received, received + 90L * NANOS_PER_TICK));
        assertEquals(60L, ClientWorldpackData.Snapshot.remainingTicks(
                60L, received, received - 1L));
    }

    @Test
    void countdownIsWallClockBasedSoPausedGameDoesNotFreezeOrDriftFromTickCount() {
        // remainingTicks is pure wall-clock math (receivedAtNanos vs nowNanos).
        // Single-player pause freezes server ticks but System.nanoTime keeps advancing,
        // so the client still decrements from the last received snapshot without needing
        // screen ticks — and without inventing "pause-time" offsets.
        long received = 5_000_000_000L;
        long initial = 120L;

        long afterOneSecond = ClientWorldpackData.Snapshot.remainingTicks(
                initial, received, received + 20L * NANOS_PER_TICK);
        long afterTwoSeconds = ClientWorldpackData.Snapshot.remainingTicks(
                initial, received, received + 40L * NANOS_PER_TICK);
        long afterExact = ClientWorldpackData.Snapshot.remainingTicks(
                initial, received, received + 120L * NANOS_PER_TICK);

        assertEquals(100L, afterOneSecond);
        assertEquals(80L, afterTwoSeconds);
        assertEquals(0L, afterExact);
        // Equal wall-clock spans yield equal decrements regardless of "paused" framing.
        assertEquals(afterOneSecond - afterTwoSeconds, 20L);
    }

    @Test
    void actionStateChangesWhenCooldownBecomesReadyOrRealmActivityFlips() {
        ClientWorldpackData.set(packetWithCooldownAndActive("", 100L));
        ClientWorldpackData.Snapshot cooling = ClientWorldpackData.get();
        int coolingState = WorldpackScreen.actionState(cooling);

        // Same packet shape with cooldown already 0 should produce a different fingerprint
        // so WorldpackScreen.tick rebuilds row buttons when enter becomes available.
        ClientWorldpackData.set(packetWithCooldownAndActive("", 0L));
        ClientWorldpackData.Snapshot ready = ClientWorldpackData.get();
        int readyState = WorldpackScreen.actionState(ready);
        assertNotEquals(coolingState, readyState);

        ClientWorldpackData.set(packetWithCooldownAndActive("secret_a", 0L));
        ClientWorldpackData.Snapshot inRealm = ClientWorldpackData.get();
        int inRealmState = WorldpackScreen.actionState(inRealm);
        assertNotEquals(readyState, inRealmState);
    }

    @Test
    void travelAndEnterButtonReadinessFollowsRevisionSnapshotFlags() {
        ClientWorldpackData.set(packetWithRegionsAndRealms(
                /*activeRealm*/ "",
                /*regionCurrent*/ false,
                /*regionAnchor*/ true,
                /*realmCurrentRegion*/ true,
                /*realmActive*/ false,
                /*cooldown*/ 0L));
        ClientWorldpackData.Snapshot data = ClientWorldpackData.get();
        ClientWorldpackData.Region region = data.regions().get(0);
        ClientWorldpackData.SecretRealm realm = data.realms().get(0);

        assertTrue(WorldpackScreen.canTravelRegion(data, region));
        assertTrue(WorldpackScreen.canEnterRealm(data, realm));

        // Current region cannot be re-traveled.
        ClientWorldpackData.set(packetWithRegionsAndRealms("", true, true, true, false, 0L));
        data = ClientWorldpackData.get();
        assertFalse(WorldpackScreen.canTravelRegion(data, data.regions().get(0)));

        // Cooldown still running blocks enter.
        ClientWorldpackData.set(packetWithRegionsAndRealms("", false, true, true, false, 200L));
        data = ClientWorldpackData.get();
        assertFalse(WorldpackScreen.canEnterRealm(data, data.realms().get(0)));

        // Already inside a secret realm blocks both travel and enter.
        ClientWorldpackData.set(packetWithRegionsAndRealms("secret_a", false, true, true, false, 0L));
        data = ClientWorldpackData.get();
        assertFalse(WorldpackScreen.canTravelRegion(data, data.regions().get(0)));
        assertFalse(WorldpackScreen.canEnterRealm(data, data.realms().get(0)));
    }

    private static SyncWorldpackDataPacket packet(long eventTicks, long cooldownTicks) {
        return packetWithCooldownAndActive("", cooldownTicks, eventTicks);
    }

    private static SyncWorldpackDataPacket packetWithCooldownAndActive(String activeRealm, long cooldownTicks) {
        return packetWithCooldownAndActive(activeRealm, cooldownTicks, 200L);
    }

    private static SyncWorldpackDataPacket packetWithCooldownAndActive(
            String activeRealm, long cooldownTicks, long eventTicks) {
        return packetWithRegionsAndRealms(activeRealm, false, true, true, false, cooldownTicks, eventTicks);
    }

    private static SyncWorldpackDataPacket packetWithRegionsAndRealms(
            String activeRealm,
            boolean regionCurrent,
            boolean regionAnchor,
            boolean realmCurrentRegion,
            boolean realmActive,
            long cooldownTicks) {
        return packetWithRegionsAndRealms(activeRealm, regionCurrent, regionAnchor,
                realmCurrentRegion, realmActive, cooldownTicks, 200L);
    }

    private static SyncWorldpackDataPacket packetWithRegionsAndRealms(
            String activeRealm,
            boolean regionCurrent,
            boolean regionAnchor,
            boolean realmCurrentRegion,
            boolean realmActive,
            long cooldownTicks,
            long eventTicks) {
        return new SyncWorldpackDataPacket(
                "region", "Region",
                activeRealm == null ? "" : activeRealm,
                activeRealm == null || activeRealm.isBlank() ? "" : "Active",
                "event", "Event", eventTicks, List.of(),
                List.of(new SyncWorldpackDataPacket.RegionData(
                        "region", "Region", "qi_refining", 1.0D, regionAnchor, regionCurrent)),
                List.of(new SyncWorldpackDataPacket.RealmData(
                        "realm", "Realm", "region", "qi_refining", "ticket",
                        cooldownTicks, true, realmCurrentRegion, realmActive)),
                false);
    }
}
