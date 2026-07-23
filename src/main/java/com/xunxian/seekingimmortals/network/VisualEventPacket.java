package com.xunxian.seekingimmortals.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.Locale;
import java.util.function.Supplier;

/**
 * Bounded, server-authored lifecycle intent for persistent client visuals.
 *
 * <p>The packet carries no gameplay state.  It identifies an authored profile,
 * an anchor, and a deterministic timeline so a client can reconstruct the
 * presentation without trusting client-provided costs or effects.</p>
 */
public record VisualEventPacket(
        String domain,
        ResourceLocation profileKey,
        Lifecycle lifecycle,
        String trigger,
        AnchorType anchorType,
        int entityId,
        long blockPos,
        double x,
        double y,
        double z,
        double targetX,
        double targetY,
        double targetZ,
        String instanceKey,
        int durationTicks,
        int ageTicks,
        float scale,
        int intensity,
        long seed,
        int priority
) {
    public static final int MAX_DOMAIN_LENGTH = 32;
    public static final int MAX_PROFILE_LENGTH = 96;
    public static final int MAX_TRIGGER_LENGTH = 64;
    public static final int MAX_INSTANCE_LENGTH = 96;
    public static final double MAX_COORDINATE = 30_000_000.0D;
    public static final int MAX_ENTITY_ID = 16_777_215;
    public static final int MAX_DURATION_TICKS = 72_000;
    public static final float MAX_SCALE = 16.0F;
    public static final int MAX_INTENSITY = 96;
    public static final int MAX_PRIORITY = 3;

    private static final ResourceLocation FALLBACK_PROFILE =
            new ResourceLocation("seeking_immortals", "generic");

    public enum Lifecycle {
        START,
        UPDATE,
        STOP,
        SNAPSHOT,
        EVENT
    }

    public enum AnchorType {
        WORLD,
        ENTITY,
        BLOCK
    }

    public VisualEventPacket {
        domain = boundedToken(domain, "generic", MAX_DOMAIN_LENGTH);
        profileKey = boundedProfile(profileKey);
        lifecycle = lifecycle == null ? Lifecycle.EVENT : lifecycle;
        trigger = boundedText(trigger, "event", MAX_TRIGGER_LENGTH);
        anchorType = anchorType == null ? AnchorType.WORLD : anchorType;
        entityId = Math.max(-1, Math.min(MAX_ENTITY_ID, entityId));
        blockPos = normalizeBlockPos(blockPos);
        x = clampCoordinate(x);
        y = clampCoordinate(y);
        z = clampCoordinate(z);
        targetX = clampCoordinate(targetX);
        targetY = clampCoordinate(targetY);
        targetZ = clampCoordinate(targetZ);
        durationTicks = clamp(durationTicks, lifecycle == Lifecycle.EVENT ? 0 : 1,
                MAX_DURATION_TICKS);
        ageTicks = clamp(ageTicks, 0, Math.max(0, durationTicks));
        scale = (float) clamp(scale, 0.05D, MAX_SCALE);
        intensity = clamp(intensity, 1, MAX_INTENSITY);
        priority = clamp(priority, 0, MAX_PRIORITY);
        instanceKey = normalizeInstanceKey(instanceKey, lifecycle, domain, profileKey, anchorType, seed);
    }

    /** Sends a cosmetic intent to clients near the packet's world anchor. */
    public static void send(ServerLevel level, VisualEventPacket packet) {
        if (level == null || packet == null) {
            return;
        }
        try {
            ModNetwork.CHANNEL.send(
                    PacketDistributor.NEAR.with(() -> new PacketDistributor.TargetPoint(
                            packet.x(), packet.y(), packet.z(), 96.0D, level.dimension())),
                    packet);
        } catch (RuntimeException ignored) {
            // Cosmetic dispatch must never roll back or block authoritative gameplay.
        }
    }

    public static void encode(VisualEventPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.domain(), MAX_DOMAIN_LENGTH);
        buffer.writeUtf(packet.profileKey().toString(), MAX_PROFILE_LENGTH);
        buffer.writeByte(packet.lifecycle().ordinal());
        buffer.writeUtf(packet.trigger(), MAX_TRIGGER_LENGTH);
        buffer.writeByte(packet.anchorType().ordinal());
        buffer.writeVarInt(packet.entityId());
        buffer.writeLong(packet.blockPos());
        buffer.writeDouble(packet.x());
        buffer.writeDouble(packet.y());
        buffer.writeDouble(packet.z());
        buffer.writeDouble(packet.targetX());
        buffer.writeDouble(packet.targetY());
        buffer.writeDouble(packet.targetZ());
        buffer.writeUtf(packet.instanceKey(), MAX_INSTANCE_LENGTH);
        buffer.writeVarInt(packet.durationTicks());
        buffer.writeVarInt(packet.ageTicks());
        buffer.writeFloat(packet.scale());
        buffer.writeVarInt(packet.intensity());
        buffer.writeLong(packet.seed());
        buffer.writeByte(packet.priority());
    }

    /**
     * Decodes defensively.  A malformed cosmetic payload degrades to a harmless
     * event rather than allowing an unbounded string, enum, or coordinate through.
     */
    public static VisualEventPacket decode(FriendlyByteBuf buffer) {
        try {
            String domain = buffer.readUtf(MAX_DOMAIN_LENGTH);
            ResourceLocation profile = ResourceLocation.tryParse(buffer.readUtf(MAX_PROFILE_LENGTH));
            Lifecycle lifecycle = enumValue(Lifecycle.values(), buffer.readUnsignedByte(), Lifecycle.EVENT);
            String trigger = buffer.readUtf(MAX_TRIGGER_LENGTH);
            AnchorType anchor = enumValue(AnchorType.values(), buffer.readUnsignedByte(), AnchorType.WORLD);
            int entityId = buffer.readVarInt();
            long blockPos = buffer.readLong();
            double x = buffer.readDouble();
            double y = buffer.readDouble();
            double z = buffer.readDouble();
            double targetX = buffer.readDouble();
            double targetY = buffer.readDouble();
            double targetZ = buffer.readDouble();
            String instanceKey = buffer.readUtf(MAX_INSTANCE_LENGTH);
            int duration = buffer.readVarInt();
            int age = buffer.readVarInt();
            float scale = buffer.readFloat();
            int intensity = buffer.readVarInt();
            long seed = buffer.readLong();
            int priority = buffer.readUnsignedByte();
            return new VisualEventPacket(domain, profile, lifecycle, trigger, anchor, entityId, blockPos,
                    x, y, z, targetX, targetY, targetZ, instanceKey, duration, age,
                    scale, intensity, seed, priority);
        } catch (RuntimeException ignored) {
            return empty();
        }
    }

    public static void handle(VisualEventPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientPacketDispatch.invoke("handleVisualEvent", packet)));
        context.setPacketHandled(true);
    }

    public boolean persistent() {
        return lifecycle != Lifecycle.EVENT;
    }

    public String profileId() {
        return profileKey.toString();
    }

    public static VisualEventPacket empty() {
        return new VisualEventPacket("generic", FALLBACK_PROFILE, Lifecycle.EVENT, "event",
                AnchorType.WORLD, -1, 0L, 0.0D, 0.0D, 0.0D,
                0.0D, 0.0D, 0.0D, "", 0, 0, 1.0F, 1, 0L, 0);
    }

    private static String boundedToken(String value, String fallback, int maxLength) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT)
                .replace(' ', '_').replace('-', '_');
        if (normalized.isBlank()) {
            normalized = fallback;
        }
        return normalized.length() <= maxLength ? normalized : normalized.substring(0, maxLength);
    }

    private static String boundedText(String value, String fallback, int maxLength) {
        String normalized = value == null ? "" : value.trim().replace('\n', '_').replace('\r', '_');
        if (normalized.isBlank()) {
            normalized = fallback;
        }
        return normalized.length() <= maxLength ? normalized : normalized.substring(0, maxLength);
    }

    private static String normalizeInstanceKey(String value, Lifecycle lifecycle, String domain,
                                               ResourceLocation profile, AnchorType anchor, long seed) {
        String normalized = boundedText(value, "", MAX_INSTANCE_LENGTH);
        if (!normalized.isBlank() || lifecycle == Lifecycle.EVENT) {
            return normalized;
        }
        String generated = domain + ":" + profile + ":" + anchor.name().toLowerCase(Locale.ROOT)
                + ":" + Long.toUnsignedString(seed, 36);
        return generated.length() <= MAX_INSTANCE_LENGTH
                ? generated : generated.substring(0, MAX_INSTANCE_LENGTH);
    }

    private static long normalizeBlockPos(long value) {
        try {
            return net.minecraft.core.BlockPos.of(value).asLong();
        } catch (RuntimeException ignored) {
            return 0L;
        }
    }

    private static ResourceLocation boundedProfile(ResourceLocation value) {
        if (value == null) {
            return FALLBACK_PROFILE;
        }
        String raw = value.toString();
        if (raw.length() <= MAX_PROFILE_LENGTH) {
            return value;
        }
        String namespace = value.getNamespace();
        if (namespace.length() + 2 > MAX_PROFILE_LENGTH) {
            return FALLBACK_PROFILE;
        }
        int pathLength = Math.max(1, MAX_PROFILE_LENGTH - namespace.length() - 1);
        ResourceLocation truncated = ResourceLocation.tryBuild(namespace,
                value.getPath().substring(0, Math.min(pathLength, value.getPath().length())));
        return truncated == null || truncated.toString().length() > MAX_PROFILE_LENGTH
                ? FALLBACK_PROFILE : truncated;
    }

    private static double clampCoordinate(double value) {
        return Double.isFinite(value) ? clamp(value, -MAX_COORDINATE, MAX_COORDINATE) : 0.0D;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
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
