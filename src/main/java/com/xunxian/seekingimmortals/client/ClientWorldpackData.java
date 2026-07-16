package com.xunxian.seekingimmortals.client;

import com.xunxian.seekingimmortals.network.SyncWorldpackDataPacket;

import java.util.List;

public final class ClientWorldpackData {
    private static final long NANOS_PER_TICK = 50_000_000L;
    private static long nextRevision;
    private static Snapshot snapshot = Snapshot.empty(0L);

    private ClientWorldpackData() {}

    public static void set(SyncWorldpackDataPacket packet) {
        long revision = ++nextRevision;
        long receivedAtNanos = System.nanoTime();
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
                true,
                revision,
                receivedAtNanos);
    }

    public static void reset() {
        snapshot = Snapshot.empty(++nextRevision);
    }

    public static Snapshot get() {
        return snapshot;
    }

    public record Snapshot(String currentRegionId, String currentRegionDisplay,
                           String activeSecretRealmId, String activeSecretRealmDisplay,
                           String dailyEventId, String dailyEventDisplay, long dailyEventRemainingTicks,
                           List<String> dailyEventEffects, List<Region> regions,
                           List<SecretRealm> realms, boolean synced, long revision, long receivedAtNanos) {
        public long currentDailyEventRemainingTicks() {
            return remainingTicks(dailyEventRemainingTicks, receivedAtNanos, System.nanoTime());
        }

        public long currentRealmCooldownTicks(SecretRealm realm) {
            return realm == null ? 0L
                    : remainingTicks(realm.remainingCooldownTicks(), receivedAtNanos, System.nanoTime());
        }

        static long remainingTicks(long initialTicks, long receivedAtNanos, long nowNanos) {
            long safeInitial = Math.max(0L, initialTicks);
            long elapsedNanos = nowNanos - receivedAtNanos;
            if (elapsedNanos <= 0L) {
                return safeInitial;
            }
            long elapsedTicks = elapsedNanos / NANOS_PER_TICK;
            return elapsedTicks >= safeInitial ? 0L : safeInitial - elapsedTicks;
        }

        private static Snapshot empty(long revision) {
            return new Snapshot("", "-", "", "", "", "", 0L, List.of(), List.of(), List.of(),
                    false, revision, System.nanoTime());
        }
    }

    public record Region(String id, String display, String minRealm, double auraMultiplier,
                         boolean anchorReady, boolean current) {}

    public record SecretRealm(String id, String display, String regionId, String minRealm,
                              String ticketDescriptionId, long remainingCooldownTicks,
                              boolean anchorReady, boolean currentRegion, boolean active) {}
}
