package com.xunxian.seekingimmortals.network;

import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VisualEventPacketTest {
    @Test
    void lifecyclePacketRoundTripsAllTransportFields() {
        VisualEventPacket original = new VisualEventPacket(
                "technique", new ResourceLocation("technique", "sword_domain"),
                VisualEventPacket.Lifecycle.SNAPSHOT, "formation", VisualEventPacket.AnchorType.ENTITY,
                42, new BlockPos(12, 64, -8).asLong(),
                12.5D, 64.0D, -8.5D, 22.0D, 65.0D, -8.5D,
                "technique:player:domain", 240, 37, 1.25F, 48, 0x1234ABCDL, 2);
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());

        VisualEventPacket.encode(original, buffer);
        VisualEventPacket decoded = VisualEventPacket.decode(buffer);

        assertEquals(original, decoded);
        assertTrue(decoded.persistent());
        assertEquals("technique:sword_domain", decoded.profileId());
    }

    @Test
    void constructorBoundsCoordinatesStringsAndTimelineValues() {
        VisualEventPacket packet = new VisualEventPacket(
                "  TECHNIQUE  ", null, null,
                "\n" + "x".repeat(200), VisualEventPacket.AnchorType.BLOCK,
                Integer.MAX_VALUE, Long.MAX_VALUE,
                Double.NaN, Double.POSITIVE_INFINITY, -80_000_000.0D,
                80_000_000.0D, 2.0D, Double.NEGATIVE_INFINITY,
                "", Integer.MAX_VALUE,
                Integer.MAX_VALUE, Float.POSITIVE_INFINITY, Integer.MAX_VALUE, 7L,
                Integer.MAX_VALUE);

        assertEquals("technique", packet.domain());
        assertNotNull(packet.profileKey());
        assertEquals(VisualEventPacket.Lifecycle.EVENT, packet.lifecycle());
        assertEquals(0.0D, packet.x());
        assertEquals(-VisualEventPacket.MAX_COORDINATE, packet.z());
        assertEquals(VisualEventPacket.MAX_COORDINATE, packet.targetX());
        assertEquals(VisualEventPacket.MAX_DURATION_TICKS, packet.durationTicks());
        assertEquals(VisualEventPacket.MAX_DURATION_TICKS, packet.ageTicks());
        assertEquals(0.05F, packet.scale());
        assertEquals(VisualEventPacket.MAX_INTENSITY, packet.intensity());
        assertEquals(VisualEventPacket.MAX_PRIORITY, packet.priority());
        assertTrue(packet.instanceKey().isBlank());

        ResourceLocation longNamespace = ResourceLocation.tryBuild(
                "n".repeat(VisualEventPacket.MAX_PROFILE_LENGTH + 8), "profile");
        VisualEventPacket namespaceBounded = new VisualEventPacket(
                "technique", longNamespace, VisualEventPacket.Lifecycle.EVENT, "event",
                VisualEventPacket.AnchorType.WORLD, -1, 0L,
                0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D,
                "", 0, 0, 1.0F, 1, 0L, 0);
        assertTrue(namespaceBounded.profileKey().toString().length()
                <= VisualEventPacket.MAX_PROFILE_LENGTH);
    }

    @Test
    void invalidEnumOrdinalsAndMalformedPayloadFailClosed() {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        buffer.writeUtf("technique", VisualEventPacket.MAX_DOMAIN_LENGTH);
        buffer.writeUtf("technique:bad", VisualEventPacket.MAX_PROFILE_LENGTH);
        buffer.writeByte(255);
        buffer.writeUtf("event", VisualEventPacket.MAX_TRIGGER_LENGTH);
        buffer.writeByte(255);
        buffer.writeVarInt(-1);
        buffer.writeLong(0L);
        for (int index = 0; index < 6; index++) {
            buffer.writeDouble(0.0D);
        }
        buffer.writeUtf("", VisualEventPacket.MAX_INSTANCE_LENGTH);
        buffer.writeVarInt(0);
        buffer.writeVarInt(0);
        buffer.writeFloat(1.0F);
        buffer.writeVarInt(1);
        buffer.writeLong(0L);
        buffer.writeByte(255);

        VisualEventPacket decoded = VisualEventPacket.decode(buffer);
        assertEquals(VisualEventPacket.Lifecycle.EVENT, decoded.lifecycle());
        assertEquals(VisualEventPacket.AnchorType.WORLD, decoded.anchorType());
        assertEquals(VisualEventPacket.MAX_PRIORITY, decoded.priority());

        FriendlyByteBuf truncated = new FriendlyByteBuf(Unpooled.buffer());
        truncated.writeUtf("technique", VisualEventPacket.MAX_DOMAIN_LENGTH);
        VisualEventPacket fallback = VisualEventPacket.decode(truncated);
        assertEquals(VisualEventPacket.Lifecycle.EVENT, fallback.lifecycle());
        assertEquals("generic", fallback.domain());
        assertFalse(fallback.persistent());
    }
}
