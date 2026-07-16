package com.xunxian.seekingimmortals.network;

import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DialoguePacketTest {
    @Test
    void boundedViewRoundTripsComponentsAndChoices() {
        OpenDialogueScreenPacket packet = new OpenDialogueScreenPacket(
                "nonce-1", "tree_market", "npc_vendor", "offer",
                Component.literal("坊市掌柜"),
                List.of(Component.literal("要买些什么？")),
                List.of(new OpenDialogueScreenPacket.Choice("effect:open_shop:0", Component.literal("打开商店"))));
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());

        OpenDialogueScreenPacket.encode(packet, buffer);
        OpenDialogueScreenPacket decoded = OpenDialogueScreenPacket.decode(buffer);

        assertEquals("nonce-1", decoded.context());
        assertEquals("tree_market", decoded.sourceId());
        assertEquals("坊市掌柜", decoded.speaker().getString());
        assertEquals("要买些什么？", decoded.lines().get(0).getString());
        assertEquals("effect:open_shop:0", decoded.choices().get(0).id());
        assertEquals("打开商店", decoded.choices().get(0).label().getString());
    }

    @Test
    void decodeRejectsNegativeAndOversizedCollectionCounts() {
        FriendlyByteBuf negativeLines = header();
        negativeLines.writeVarInt(-1);
        assertThrows(DecoderException.class, () -> OpenDialogueScreenPacket.decode(negativeLines));

        FriendlyByteBuf tooManyChoices = header();
        tooManyChoices.writeVarInt(0);
        tooManyChoices.writeVarInt(OpenDialogueScreenPacket.MAX_CHOICES + 1);
        assertThrows(DecoderException.class, () -> OpenDialogueScreenPacket.decode(tooManyChoices));
    }

    @Test
    void encodeRejectsMoreThanViewLimits() {
        OpenDialogueScreenPacket packet = new OpenDialogueScreenPacket(
                "nonce", "source", "npc", "node", Component.empty(),
                java.util.Collections.nCopies(OpenDialogueScreenPacket.MAX_LINES + 1, Component.literal("line")),
                List.of());
        assertThrows(IllegalArgumentException.class, () -> OpenDialogueScreenPacket.encode(
                packet, new FriendlyByteBuf(Unpooled.buffer())));
    }

    @Test
    void clientIntentKeepsThreeStringWireLayout() {
        DialogueActionPacket packet = new DialogueActionPacket("act", "nonce-2", "next_node");
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        DialogueActionPacket.encode(packet, buffer);
        assertEquals(packet, DialogueActionPacket.decode(buffer));
    }

    private static FriendlyByteBuf header() {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        buffer.writeUtf("nonce", 64);
        buffer.writeUtf("source", 96);
        buffer.writeUtf("npc", 96);
        buffer.writeUtf("node", 96);
        buffer.writeUtf(Component.Serializer.toJson(Component.empty()), 2_048);
        return buffer;
    }
}
