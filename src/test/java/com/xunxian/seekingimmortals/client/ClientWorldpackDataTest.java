package com.xunxian.seekingimmortals.client;

import com.xunxian.seekingimmortals.network.SyncWorldpackDataPacket;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientWorldpackDataTest {
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
                60L, received, received + 20L * 50_000_000L));
        assertEquals(0L, ClientWorldpackData.Snapshot.remainingTicks(
                60L, received, received + 90L * 50_000_000L));
        assertEquals(60L, ClientWorldpackData.Snapshot.remainingTicks(
                60L, received, received - 1L));
    }

    private static SyncWorldpackDataPacket packet(long eventTicks, long cooldownTicks) {
        return new SyncWorldpackDataPacket(
                "region", "Region", "", "", "event", "Event", eventTicks, List.of(),
                List.of(),
                List.of(new SyncWorldpackDataPacket.RealmData(
                        "realm", "Realm", "region", "qi_refining", "ticket",
                        cooldownTicks, true, true, false)),
                false);
    }
}
