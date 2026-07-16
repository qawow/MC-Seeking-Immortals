package com.xunxian.seekingimmortals.network;

import com.xunxian.seekingimmortals.beast.BeastBestiaryService;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SyncLoreUnlockPacketTest {
    @Test
    void fullBestiarySnapshotRoundTripsWithoutTruncation() {
        List<String> bestiary = List.copyOf(BeastBestiaryService.all().keySet());
        List<String> chronicle = ids("chronicle", 200);
        List<String> timeline = ids("phase", 80);
        SyncLoreUnlockPacket original = new SyncLoreUnlockPacket(
                bestiary, chronicle, timeline, "visual");
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());

        SyncLoreUnlockPacket.encode(original, buffer);
        SyncLoreUnlockPacket decoded = SyncLoreUnlockPacket.decode(buffer);

        assertTrue(bestiary.size() > 512, "catalog must exercise the former truncation boundary");
        assertTrue(bestiary.stream().allMatch(id -> id.length() <= 64));
        assertEquals(bestiary, decoded.bestiaryUnlocked());
        assertEquals(chronicle, decoded.chronicleDiscovered());
        assertEquals(timeline, decoded.timelinePhases());
        assertEquals("visual", decoded.openScreen());
    }

    @Test
    void encodeRejectsEachOversizedCollection() {
        assertThrows(IllegalArgumentException.class, () -> encode(new SyncLoreUnlockPacket(
                ids("beast", 4_097), List.of(), List.of(), "")));
        assertThrows(IllegalArgumentException.class, () -> encode(new SyncLoreUnlockPacket(
                List.of(), ids("chronicle", 257), List.of(), "")));
        assertThrows(IllegalArgumentException.class, () -> encode(new SyncLoreUnlockPacket(
                List.of(), List.of(), ids("phase", 129), "")));
    }

    @Test
    void exactCollectionLimitsRoundTrip() {
        SyncLoreUnlockPacket packet = new SyncLoreUnlockPacket(
                ids("beast", 4_096), ids("chronicle", 256), ids("phase", 128), "");
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());

        SyncLoreUnlockPacket.encode(packet, buffer);
        SyncLoreUnlockPacket decoded = SyncLoreUnlockPacket.decode(buffer);

        assertEquals(4_096, decoded.bestiaryUnlocked().size());
        assertEquals(256, decoded.chronicleDiscovered().size());
        assertEquals(128, decoded.timelinePhases().size());
    }

    @Test
    void decodeRejectsInvalidCountsBeforeReadingEntries() {
        FriendlyByteBuf negativeBestiary = new FriendlyByteBuf(Unpooled.buffer());
        negativeBestiary.writeVarInt(-1);
        assertThrows(DecoderException.class, () -> SyncLoreUnlockPacket.decode(negativeBestiary));

        FriendlyByteBuf oversizedBestiary = new FriendlyByteBuf(Unpooled.buffer());
        oversizedBestiary.writeVarInt(4_097);
        assertThrows(DecoderException.class, () -> SyncLoreUnlockPacket.decode(oversizedBestiary));

        FriendlyByteBuf oversizedChronicle = new FriendlyByteBuf(Unpooled.buffer());
        oversizedChronicle.writeVarInt(0);
        oversizedChronicle.writeVarInt(257);
        assertThrows(DecoderException.class, () -> SyncLoreUnlockPacket.decode(oversizedChronicle));

        FriendlyByteBuf oversizedTimeline = new FriendlyByteBuf(Unpooled.buffer());
        oversizedTimeline.writeVarInt(0);
        oversizedTimeline.writeVarInt(0);
        oversizedTimeline.writeVarInt(129);
        assertThrows(DecoderException.class, () -> SyncLoreUnlockPacket.decode(oversizedTimeline));
    }

    private static void encode(SyncLoreUnlockPacket packet) {
        SyncLoreUnlockPacket.encode(packet, new FriendlyByteBuf(Unpooled.buffer()));
    }

    private static List<String> ids(String prefix, int count) {
        return IntStream.range(0, count)
                .mapToObj(index -> prefix + "_" + index)
                .toList();
    }
}
