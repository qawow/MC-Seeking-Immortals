package com.xunxian.seekingimmortals.network;

import com.xunxian.seekingimmortals.client.ClientSkillData;
import com.xunxian.seekingimmortals.skill.SkillType;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SyncSkillDataPacketTest {
    @AfterEach
    void resetClientData() {
        ClientSkillData.reset();
    }

    @Test
    void encodeDecodeRoundTrip() {
        SyncSkillDataPacket packet = new SyncSkillDataPacket(List.of(
                new SyncSkillDataPacket.SkillData("ALCHEMY", true, 4, 125, 3200),
                new SyncSkillDataPacket.SkillData("FORMATION", false, 0, 0, 0)));
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());

        SyncSkillDataPacket.encode(packet, buffer);
        SyncSkillDataPacket decoded = SyncSkillDataPacket.decode(buffer);

        assertEquals(packet, decoded);
    }

    @Test
    void decodeRejectsOversizedSkillCount() {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        buffer.writeVarInt(SyncSkillDataPacket.MAX_SKILLS + 1);

        assertThrows(DecoderException.class, () -> SyncSkillDataPacket.decode(buffer));
    }

    @Test
    void clientDataNormalizesUntrustedEntries() {
        ClientSkillData.setSkillData(List.of(
                new SyncSkillDataPacket.SkillData("lava_burst", true, -5, -10, 20000),
                new SyncSkillDataPacket.SkillData("NOT_A_SKILL", true, 3, 4, 5),
                new SyncSkillDataPacket.SkillData("ALCHEMY", true, 2, 3, 4),
                new SyncSkillDataPacket.SkillData("alchemy", true, 99, 12, 34),
                new SyncSkillDataPacket.SkillData("FORMATION", false, 9, 88, 999)));

        ClientSkillData.SkillSnapshot lavaBurst = ClientSkillData.get(SkillType.LAVA_BURST);
        assertTrue(lavaBurst.unlocked());
        assertEquals(1, lavaBurst.level());
        assertEquals(0, lavaBurst.experience());
        assertEquals(10000, lavaBurst.proficiency());

        ClientSkillData.SkillSnapshot alchemy = ClientSkillData.get(SkillType.ALCHEMY);
        assertTrue(alchemy.unlocked());
        assertEquals(10, alchemy.level());
        assertEquals(12, alchemy.experience());
        assertEquals(34, alchemy.proficiency());

        ClientSkillData.SkillSnapshot formation = ClientSkillData.get(SkillType.FORMATION);
        assertFalse(formation.unlocked());
        assertEquals(0, formation.level());
        assertEquals(0, formation.experience());
        assertEquals(0, formation.proficiency());

        assertTrue(ClientSkillData.isSynced());
        assertEquals(SkillType.values().length, ClientSkillData.all().size());
        assertEquals(ClientSkillData.SkillSnapshot.locked(), ClientSkillData.get(SkillType.MIST_RAIN));
    }
}
