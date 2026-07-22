package com.xunxian.seekingimmortals.network;

import com.xunxian.seekingimmortals.skill.effect.TechniqueVfxPalette;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

/**
 * Server-authoritative visual intent. The client turns this small event into
 * Lodestone particles; no gameplay value is carried by the packet.
 */
public record TechniqueVfxPacket(
        Kind kind,
        TechniqueVfxPalette.Family family,
        double x,
        double y,
        double z,
        double endX,
        double endY,
        double endZ,
        float radius,
        int intensity,
        long seed
) {
    private static final double MAX_COORDINATE = 30_000_000.0D;
    private static final double MAX_RADIUS = 32.0D;
    private static final int MAX_INTENSITY = 96;
    private static final double SEND_DISTANCE = 96.0D;

    public enum Kind {
        CAST,
        BURST,
        PATH,
        AURA,
        SCAN,
        BEAM,
        CONE,
        IMPACT,
        FORMATION
    }

    public TechniqueVfxPacket {
        kind = kind == null ? Kind.BURST : kind;
        family = family == null ? TechniqueVfxPalette.Family.NEUTRAL : family;
        x = clampCoordinate(x);
        y = clampCoordinate(y);
        z = clampCoordinate(z);
        endX = clampCoordinate(endX);
        endY = clampCoordinate(endY);
        endZ = clampCoordinate(endZ);
        radius = (float) clamp(radius, 0.05D, MAX_RADIUS);
        intensity = (int) clamp(intensity, 1, MAX_INTENSITY);
    }

    public static void send(ServerLevel level, Kind kind, TechniqueVfxPalette.Family family,
                            Vec3 start, Vec3 end, double radius, int intensity, long seed) {
        if (level == null || start == null || !finite(start)) {
            return;
        }
        Vec3 safeEnd = end == null || !finite(end) ? start : end;
        TechniqueVfxPacket packet = new TechniqueVfxPacket(
                kind, family,
                start.x, start.y, start.z,
                safeEnd.x, safeEnd.y, safeEnd.z,
                (float) radius, intensity, seed);
        ResourceKey<Level> dimension = level.dimension();
        ModNetwork.CHANNEL.send(
                PacketDistributor.NEAR.with(() -> new PacketDistributor.TargetPoint(
                        packet.x, packet.y, packet.z, SEND_DISTANCE, dimension)),
                packet);
    }

    public static void encode(TechniqueVfxPacket packet, FriendlyByteBuf buffer) {
        buffer.writeByte(packet.kind.ordinal());
        buffer.writeByte(packet.family.ordinal());
        buffer.writeDouble(packet.x);
        buffer.writeDouble(packet.y);
        buffer.writeDouble(packet.z);
        buffer.writeDouble(packet.endX);
        buffer.writeDouble(packet.endY);
        buffer.writeDouble(packet.endZ);
        buffer.writeFloat(packet.radius);
        buffer.writeVarInt(packet.intensity);
        buffer.writeLong(packet.seed);
    }

    public static TechniqueVfxPacket decode(FriendlyByteBuf buffer) {
        Kind kind = enumValue(Kind.values(), buffer.readUnsignedByte(), Kind.BURST);
        TechniqueVfxPalette.Family family = enumValue(
                TechniqueVfxPalette.Family.values(), buffer.readUnsignedByte(), TechniqueVfxPalette.Family.NEUTRAL);
        return new TechniqueVfxPacket(
                kind,
                family,
                buffer.readDouble(), buffer.readDouble(), buffer.readDouble(),
                buffer.readDouble(), buffer.readDouble(), buffer.readDouble(),
                buffer.readFloat(), buffer.readVarInt(), buffer.readLong());
    }

    public static void handle(TechniqueVfxPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientPacketDispatch.invoke("handleTechniqueVfx", packet)));
        context.setPacketHandled(true);
    }

    private static boolean finite(Vec3 value) {
        return Double.isFinite(value.x) && Double.isFinite(value.y) && Double.isFinite(value.z);
    }

    private static double clampCoordinate(double value) {
        return Double.isFinite(value) ? clamp(value, -MAX_COORDINATE, MAX_COORDINATE) : 0.0D;
    }

    private static double clamp(double value, double min, double max) {
        if (!Double.isFinite(value)) {
            return min;
        }
        return Math.max(min, Math.min(max, value));
    }

    private static <T> T enumValue(T[] values, int index, T fallback) {
        return index >= 0 && index < values.length ? values[index] : fallback;
    }
}
