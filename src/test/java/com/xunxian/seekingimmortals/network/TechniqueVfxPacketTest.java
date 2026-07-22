package com.xunxian.seekingimmortals.network;

import com.xunxian.seekingimmortals.skill.effect.TechniqueVfxPalette;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TechniqueVfxPacketTest {
    @Test
    void packetRoundTripsAllVisualIntentFields() {
        TechniqueVfxPacket original = new TechniqueVfxPacket(
                TechniqueVfxPacket.Kind.FORMATION,
                TechniqueVfxPalette.Family.THUNDER,
                1.25D, 64.5D, -3.75D,
                8.0D, 66.0D, 12.0D,
                9.5F, 72, 0x1234ABCDL);
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());

        TechniqueVfxPacket.encode(original, buffer);
        TechniqueVfxPacket decoded = TechniqueVfxPacket.decode(buffer);

        assertEquals(original, decoded);
    }

    @Test
    void constructorBoundsMalformedVisualPayload() {
        TechniqueVfxPacket packet = new TechniqueVfxPacket(
                null,
                null,
                Double.NaN, Double.POSITIVE_INFINITY, -80_000_000.0D,
                90_000_000.0D, 12.0D, Double.NEGATIVE_INFINITY,
                Float.POSITIVE_INFINITY, Integer.MAX_VALUE, 7L);

        assertEquals(TechniqueVfxPacket.Kind.BURST, packet.kind());
        assertEquals(TechniqueVfxPalette.Family.NEUTRAL, packet.family());
        assertEquals(0.0D, packet.x());
        assertEquals(0.0D, packet.y());
        assertEquals(-30_000_000.0D, packet.z());
        assertEquals(30_000_000.0D, packet.endX());
        assertEquals(0.0D, packet.endZ());
        assertEquals(0.05F, packet.radius());
        assertEquals(96, packet.intensity());
    }

    @Test
    void invalidEnumOrdinalsDecodeToFallbacks() {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        buffer.writeByte(255);
        buffer.writeByte(255);
        for (int i = 0; i < 6; i++) {
            buffer.writeDouble(0.0D);
        }
        buffer.writeFloat(1.0F);
        buffer.writeVarInt(1);
        buffer.writeLong(0L);

        TechniqueVfxPacket decoded = TechniqueVfxPacket.decode(buffer);

        assertEquals(TechniqueVfxPacket.Kind.BURST, decoded.kind());
        assertEquals(TechniqueVfxPalette.Family.NEUTRAL, decoded.family());
        assertTrue(decoded.radius() > 0.0F);
    }
}
