package com.xunxian.seekingimmortals.network;

import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SyncQuestTrackerPacketTest {
    @Test
    void encodeDecodeRoundTrip() {
        SyncQuestTrackerPacket packet = new SyncQuestTrackerPacket(List.of(
                "OK sync",
                "mortal_path 1/3 branch=neutral cost=-:0 own=0 LOCK=0 REW=0"));
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());

        SyncQuestTrackerPacket.encode(packet, buffer);

        assertEquals(packet, SyncQuestTrackerPacket.decode(buffer));
    }

    @Test
    void decodeRejectsOversizedLineCount() {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        buffer.writeVarInt(SyncQuestTrackerPacket.MAX_LINES + 1);

        assertThrows(DecoderException.class, () -> SyncQuestTrackerPacket.decode(buffer));
    }

    @Test
    void encodeRejectsOversizedLineCount() {
        SyncQuestTrackerPacket packet = new SyncQuestTrackerPacket(
                Collections.nCopies(SyncQuestTrackerPacket.MAX_LINES + 1, "line"));
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());

        assertThrows(IllegalArgumentException.class, () -> SyncQuestTrackerPacket.encode(packet, buffer));
    }
}
