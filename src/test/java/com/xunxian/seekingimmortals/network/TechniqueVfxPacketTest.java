package com.xunxian.seekingimmortals.network;

import com.xunxian.seekingimmortals.skill.effect.TechniqueVfxPalette;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TechniqueVfxPacketTest {
    @Test
    void packetRoundTripsAllVisualIntentFields() {
        TechniqueVfxPacket original = new TechniqueVfxPacket(
                TechniqueVfxPacket.Kind.FORMATION,
                TechniqueVfxPalette.Family.THUNDER,
                TechniqueVfxPacket.Motif.FORMATION,
                TechniqueVfxPacket.ParticleStyle.THUNDER_ARC,
                TechniqueVfxPacket.TrailStyle.THUNDER_JAGGED,
                true,
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
                null,
                Double.NaN, Double.POSITIVE_INFINITY, -80_000_000.0D,
                90_000_000.0D, 12.0D, Double.NEGATIVE_INFINITY,
                Float.POSITIVE_INFINITY, Integer.MAX_VALUE, 7L);

        assertEquals(TechniqueVfxPacket.Kind.BURST, packet.kind());
        assertEquals(TechniqueVfxPalette.Family.NEUTRAL, packet.family());
        assertEquals(TechniqueVfxPacket.Motif.GENERIC, packet.motif());
        assertEquals(TechniqueVfxPacket.ParticleStyle.DEFAULT, packet.particleStyle());
        assertEquals(TechniqueVfxPacket.TrailStyle.DEFAULT, packet.trailStyle());
        assertFalse(packet.telegraphed());
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
        buffer.writeByte(255);
        buffer.writeByte(255);
        buffer.writeByte(255);
        buffer.writeBoolean(false);
        for (int i = 0; i < 6; i++) {
            buffer.writeDouble(0.0D);
        }
        buffer.writeFloat(1.0F);
        buffer.writeVarInt(1);
        buffer.writeLong(0L);

        TechniqueVfxPacket decoded = TechniqueVfxPacket.decode(buffer);

        assertEquals(TechniqueVfxPacket.Kind.BURST, decoded.kind());
        assertEquals(TechniqueVfxPalette.Family.NEUTRAL, decoded.family());
        assertEquals(TechniqueVfxPacket.Motif.GENERIC, decoded.motif());
        assertEquals(TechniqueVfxPacket.ParticleStyle.DEFAULT, decoded.particleStyle());
        assertEquals(TechniqueVfxPacket.TrailStyle.DEFAULT, decoded.trailStyle());
        assertFalse(decoded.telegraphed());
        assertTrue(decoded.radius() > 0.0F);
    }

    @Test
    void authorReferencesMapToBoundedWireEnums() {
        assertEquals(TechniqueVfxPacket.ParticleStyle.METAL_SPARK,
                TechniqueVfxPacket.ParticleStyle.fromAuthorRef("metal_spark"));
        assertEquals(TechniqueVfxPacket.ParticleStyle.WATER_MIST_METAL_SPARK,
                TechniqueVfxPacket.ParticleStyle.fromAuthorRef("water_mist+metal_spark"));
        assertEquals(TechniqueVfxPacket.ParticleStyle.DEFAULT,
                TechniqueVfxPacket.ParticleStyle.fromAuthorRef("unknown"));
        assertEquals(TechniqueVfxPacket.TrailStyle.SWORD_THIN,
                TechniqueVfxPacket.TrailStyle.fromAuthorRef("sword_thin"));
        assertEquals(TechniqueVfxPacket.TrailStyle.DEFAULT,
                TechniqueVfxPacket.TrailStyle.fromAuthorRef("unknown"));
    }

    @Test
    void synchronousCaptureScopesAreNestableAndIdempotent() {
        TechniqueVfxPacket.CaptureScope outer = TechniqueVfxPacket.captureSynchronousIntents();
        TechniqueVfxPacket.CaptureScope inner = TechniqueVfxPacket.captureSynchronousIntents();

        inner.close();
        inner.close();
        outer.close();

        assertTrue(inner.packets().isEmpty());
        assertTrue(outer.packets().isEmpty());
    }

    @Test
    void captureCandidatesIncludeFormationActivationButNotStatusLifecycle() {
        TechniqueVfxPacket genericBeam = packet(
                TechniqueVfxPacket.Kind.BEAM, TechniqueVfxPacket.Motif.GENERIC);
        TechniqueVfxPacket formationCast = packet(
                TechniqueVfxPacket.Kind.CAST, TechniqueVfxPacket.Motif.FORMATION);
        TechniqueVfxPacket formationField = packet(
                TechniqueVfxPacket.Kind.FORMATION, TechniqueVfxPacket.Motif.FORMATION);
        TechniqueVfxPacket formationStatus = packet(
                TechniqueVfxPacket.Kind.STATUS, TechniqueVfxPacket.Motif.FORMATION);

        assertTrue(TechniqueVfxPacket.isCaptureCandidate(genericBeam));
        assertTrue(TechniqueVfxPacket.isCaptureCandidate(formationCast));
        assertTrue(TechniqueVfxPacket.isCaptureCandidate(formationField));
        assertFalse(TechniqueVfxPacket.isCaptureCandidate(formationStatus));
    }

    private static TechniqueVfxPacket packet(TechniqueVfxPacket.Kind kind, TechniqueVfxPacket.Motif motif) {
        return new TechniqueVfxPacket(
                kind, TechniqueVfxPalette.Family.NEUTRAL, motif,
                0.0D, 64.0D, 0.0D,
                1.0D, 64.0D, 0.0D,
                1.0F, 16, 1L);
    }
}
