package com.xunxian.seekingimmortals.network;

import net.minecraft.network.FriendlyByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SyncAuctionLadderPacketTest {
    @Test
    void encodeDecodeRoundTrip() {
        SyncAuctionLadderPacket original = new SyncAuctionLadderPacket(
                1, 6, 20,
                List.of(new SyncAuctionLadderPacket.LotBid(
                        "lot_a", "Lot A", 10, 12, 5, 100, "leader", false)));
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        SyncAuctionLadderPacket.encode(original, buffer);
        SyncAuctionLadderPacket decoded = SyncAuctionLadderPacket.decode(buffer);
        assertEquals(1, decoded.page());
        assertEquals(6, decoded.pageSize());
        assertEquals(20, decoded.totalLots());
        assertEquals(1, decoded.lots().size());
        assertEquals("lot_a", decoded.lots().get(0).lotId());
        assertEquals(10, decoded.lots().get(0).current());
        assertTrue(decoded.lots().get(0).next() >= 10);
    }
}
