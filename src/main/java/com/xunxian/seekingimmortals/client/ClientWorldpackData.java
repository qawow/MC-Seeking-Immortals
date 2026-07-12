package com.xunxian.seekingimmortals.client;

import com.xunxian.seekingimmortals.network.SyncWorldpackDataPacket;

import java.util.List;

public final class ClientWorldpackData {
    private static Snapshot snapshot = Snapshot.empty();

    private ClientWorldpackData() {}

    public static void set(SyncWorldpackDataPacket packet) {
        snapshot = new Snapshot(
                packet.currentRegionId(),
                packet.currentRegionDisplay(),
                packet.activeSecretRealmId(),
                packet.activeSecretRealmDisplay(),
                packet.dailyEventId(),
                packet.dailyEventDisplay(),
                packet.dailyEventRemainingTicks(),
                List.copyOf(packet.dailyEventEffects()),
                packet.regions().stream()
                        .map(region -> new Region(
                                region.id(),
                                region.display(),
                                region.minRealm(),
                                region.auraMultiplier(),
                                region.anchorReady(),
                                region.current()))
                        .toList(),
                packet.realms().stream()
                        .map(realm -> new SecretRealm(
                                realm.id(),
                                realm.display(),
                                realm.regionId(),
                                realm.minRealm(),
                                realm.ticketDescriptionId(),
                                realm.remainingCooldownTicks(),
                                realm.anchorReady(),
                                realm.currentRegion(),
                                realm.active()))
                        .toList(),
                true);
    }

    public static void reset() {
        snapshot = Snapshot.empty();
    }

    public static Snapshot get() {
        return snapshot;
    }

    public record Snapshot(String currentRegionId, String currentRegionDisplay,
                           String activeSecretRealmId, String activeSecretRealmDisplay,
                           String dailyEventId, String dailyEventDisplay, long dailyEventRemainingTicks,
                           List<String> dailyEventEffects, List<Region> regions,
                           List<SecretRealm> realms, boolean synced) {
        private static Snapshot empty() {
            return new Snapshot("", "-", "", "", "", "", 0L, List.of(), List.of(), List.of(), false);
        }
    }

    public record Region(String id, String display, String minRealm, double auraMultiplier,
                         boolean anchorReady, boolean current) {}

    public record SecretRealm(String id, String display, String regionId, String minRealm,
                              String ticketDescriptionId, long remainingCooldownTicks,
                              boolean anchorReady, boolean currentRegion, boolean active) {}
}
