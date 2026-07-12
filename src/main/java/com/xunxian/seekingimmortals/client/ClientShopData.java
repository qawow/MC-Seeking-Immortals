package com.xunxian.seekingimmortals.client;

import com.xunxian.seekingimmortals.network.SyncShopDataPacket;

import java.util.List;

public final class ClientShopData {
    private static Snapshot snapshot = Snapshot.empty();

    private ClientShopData() {}

    public static void set(SyncShopDataPacket packet) {
        snapshot = new Snapshot(
                packet.shopId(),
                packet.titleKey(),
                packet.entries().stream()
                        .map(entry -> new Entry(
                                entry.id(),
                                entry.itemDescriptionId(),
                                entry.count(),
                                entry.cost(),
                                entry.currency(),
                                entry.currencyDescriptionId(),
                                entry.remainingStock(),
                                entry.nextRefreshTicks(),
                                entry.rankMin() == null ? "" : entry.rankMin(),
                                entry.locked()))
                        .toList(),
                true);
    }

    public static void reset() {
        snapshot = Snapshot.empty();
    }

    public static Snapshot get() {
        return snapshot;
    }

    public record Snapshot(String shopId, String titleKey, List<Entry> entries, boolean synced) {
        private static Snapshot empty() {
            return new Snapshot("", "screen.seeking_immortals.shop.market_title", List.of(), false);
        }
    }

    public record Entry(String id, String itemDescriptionId, int count, int cost, String currency,
                        String currencyDescriptionId, int remainingStock, long nextRefreshTicks,
                        String rankMin, boolean locked) {}
}
