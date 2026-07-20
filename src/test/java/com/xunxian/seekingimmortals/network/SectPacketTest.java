package com.xunxian.seekingimmortals.network;

import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SectPacketTest {
    @Test
    void syncSectDataRoundTrips() {
        List<SyncSectDataPacket.CandidateData> candidates = IntStream.range(0, 30)
                .mapToObj(index -> new SyncSectDataPacket.CandidateData(
                        "sect_" + index, "Sect " + index, "Sect " + index,
                        "focus_" + index, "seeking_immortals:sect_outpost_generic", true))
                .toList();
        SyncSectDataPacket packet = new SyncSectDataPacket(
                "qinglan_sect",
                "Qinglan",
                "Qinglan",
                "outer",
                120,
                true,
                true,
                true,
                false,
                2,
                "screen.stage",
                "screen.objective",
                candidates,
                new SyncSectDataPacket.DialogueNodeData("outer", "dialogue.title", "dialogue.text",
                        List.of(new SyncSectDataPacket.DialogueOptionData("mission", "dialogue.option", "accept_mission"))),
                new SyncSectDataPacket.MissionData("mission_a", "mission.title", "mission.objective", "item.test",
                        10, 20, true, false, true),
                List.of(new SyncSectDataPacket.ShopEntryData("foundation_formula", "item.test", 1, 120, "sect_contribution")),
                true);
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());

        SyncSectDataPacket.encode(packet, buffer);
        SyncSectDataPacket decoded = SyncSectDataPacket.decode(buffer);

        assertEquals(packet.sectId(), decoded.sectId());
        assertEquals(packet.contribution(), decoded.contribution());
        assertTrue(decoded.yueArrived());
        assertTrue(decoded.member());
        assertTrue(decoded.openScreen());
        assertEquals(30, decoded.candidates().size());
        assertEquals("sect_0", decoded.candidates().get(0).id());
        assertEquals("outer", decoded.dialogue().id());
        assertEquals("mission_a", decoded.mission().id());
        assertTrue(decoded.mission().canTurnIn());
        assertEquals(1, decoded.shopEntries().size());
        assertEquals("foundation_formula", decoded.shopEntries().get(0).id());
        assertEquals(1, decoded.shopEntries().get(0).count());
    }

    @Test
    void sectActionRoundTrips() {
        SectActionPacket packet = new SectActionPacket(
                "buy", "foundation_formula", "extra_payload", 0x1122334455667788L);
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());

        SectActionPacket.encode(packet, buffer);
        SectActionPacket decoded = SectActionPacket.decode(buffer);

        assertEquals("buy", decoded.action());
        assertEquals("foundation_formula", decoded.targetId());
        assertEquals("extra_payload", decoded.extra());
        assertEquals(0x1122334455667788L, decoded.accessToken());
        assertFalse(decoded.targetId().isBlank());
    }

    @Test
    void syncSectDataRejectsOversizedCandidateCount() {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        writeMinimalSyncHeader(buffer);
        buffer.writeVarInt(33);

        assertThrows(DecoderException.class, () -> SyncSectDataPacket.decode(buffer));
    }

    @Test
    void syncSectDataRejectsOversizedShopCount() {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        writeMinimalSyncHeader(buffer);
        writeEmptyCandidates(buffer);
        writeEmptyDialogue(buffer);
        writeEmptyMission(buffer);
        buffer.writeVarInt(65);

        assertThrows(DecoderException.class, () -> SyncSectDataPacket.decode(buffer));
    }

    @Test
    void sectActionRejectsOversizedAction() {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        buffer.writeUtf("a".repeat(65), 128);
        buffer.writeUtf("", 96);
        buffer.writeUtf("", 160);

        assertThrows(DecoderException.class, () -> SectActionPacket.decode(buffer));
    }

    @Test
    void sectActionRejectsOversizedTargetId() {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        buffer.writeUtf("buy", 64);
        buffer.writeUtf("e".repeat(97), 128);
        buffer.writeUtf("", 160);

        assertThrows(DecoderException.class, () -> SectActionPacket.decode(buffer));
    }

    @Test
    void sectActionRejectsOversizedExtra() {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        buffer.writeUtf("dialogue", 64);
        buffer.writeUtf("mission", 96);
        buffer.writeUtf("x".repeat(161), 192);

        assertThrows(DecoderException.class, () -> SectActionPacket.decode(buffer));
    }

    private static void writeMinimalSyncHeader(FriendlyByteBuf buffer) {
        buffer.writeUtf("qinglan_sect", 128);
        buffer.writeUtf("Qinglan", 128);
        buffer.writeUtf("Qinglan", 128);
        buffer.writeUtf("outer", 128);
        buffer.writeVarInt(0);
        buffer.writeBoolean(true);
        buffer.writeBoolean(true);
        buffer.writeBoolean(true);
        buffer.writeBoolean(false);
        buffer.writeVarInt(2);
        buffer.writeUtf("screen.stage", 192);
        buffer.writeUtf("screen.objective", 192);
    }

    private static void writeEmptyCandidates(FriendlyByteBuf buffer) {
        buffer.writeVarInt(0);
    }

    private static void writeEmptyDialogue(FriendlyByteBuf buffer) {
        buffer.writeUtf("", 128);
        buffer.writeUtf("", 192);
        buffer.writeUtf("", 192);
        buffer.writeVarInt(0);
    }

    private static void writeEmptyMission(FriendlyByteBuf buffer) {
        buffer.writeUtf("", 128);
        buffer.writeUtf("", 192);
        buffer.writeUtf("", 192);
        buffer.writeUtf("", 192);
        buffer.writeVarInt(0);
        buffer.writeVarInt(0);
        buffer.writeBoolean(false);
        buffer.writeBoolean(false);
        buffer.writeBoolean(false);
    }
}
