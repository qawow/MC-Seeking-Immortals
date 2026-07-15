package com.xunxian.seekingimmortals.client;

import com.xunxian.seekingimmortals.network.SyncAuctionLadderPacket;

import java.util.List;

/** Wave491 client mirror of live auction ladder pages. */
public final class ClientAuctionLadderData {
    private static Snapshot snapshot = Snapshot.empty();

    private ClientAuctionLadderData() {}

    public static void set(SyncAuctionLadderPacket packet) {
        if (packet == null) {
            snapshot = Snapshot.empty();
            return;
        }
        snapshot = new Snapshot(true, packet.page(), packet.pageSize(), packet.totalLots(),
                packet.lots() == null ? List.of() : List.copyOf(packet.lots()));
    }

    public static void reset() {
        snapshot = Snapshot.empty();
    }

    public static Snapshot get() {
        return snapshot;
    }

    public record Snapshot(boolean synced, int page, int pageSize, int totalLots,
                           List<SyncAuctionLadderPacket.LotBid> lots) {
        public static Snapshot empty() {
            return new Snapshot(false, 0, 6, 0, List.of());
        }

        public int maxPage() {
            if (pageSize <= 0 || totalLots <= 0) {
                return 0;
            }
            return Math.max(0, (totalLots - 1) / pageSize);
        }
    }
}
