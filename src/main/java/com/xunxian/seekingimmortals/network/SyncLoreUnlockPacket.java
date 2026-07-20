package com.xunxian.seekingimmortals.network;

import io.netty.handler.codec.DecoderException;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Supplier;

/**
 * M16: sync unlocked beast ids + discovered chronicle ids + timeline phases for read-only encyclopedia screens.
 * Does not write gameplay state; display only.
 */
public record SyncLoreUnlockPacket(
        List<String> bestiaryUnlocked,
        List<String> chronicleDiscovered,
        List<String> timelinePhases,
        String openScreen
) {
    private static final int MAX_BESTIARY_IDS = 4096;
    private static final int MAX_CHRONICLE_IDS = 256;
    private static final int MAX_TIMELINE_PHASES = 128;
    private static final int MAX_LEN = 64;
    private static final int MAX_SCREEN = 24;

    public static void send(ServerPlayer player, List<String> bestiary, List<String> chronicle,
                            List<String> timeline, String openScreen) {
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new SyncLoreUnlockPacket(
                        sanitize(bestiary, MAX_BESTIARY_IDS, "bestiary"),
                        sanitize(chronicle, MAX_CHRONICLE_IDS, "chronicle"),
                        sanitize(timeline, MAX_TIMELINE_PHASES, "timeline"),
                        openScreen == null ? "" : openScreen));
    }

    public static void encode(SyncLoreUnlockPacket packet, FriendlyByteBuf buffer) {
        writeList(buffer, packet.bestiaryUnlocked(), MAX_BESTIARY_IDS, "bestiary");
        writeList(buffer, packet.chronicleDiscovered(), MAX_CHRONICLE_IDS, "chronicle");
        writeList(buffer, packet.timelinePhases(), MAX_TIMELINE_PHASES, "timeline");
        buffer.writeUtf(packet.openScreen() == null ? "" : packet.openScreen(), MAX_SCREEN);
    }

    public static SyncLoreUnlockPacket decode(FriendlyByteBuf buffer) {
        return new SyncLoreUnlockPacket(
                readList(buffer, MAX_BESTIARY_IDS, "bestiary"),
                readList(buffer, MAX_CHRONICLE_IDS, "chronicle"),
                readList(buffer, MAX_TIMELINE_PHASES, "timeline"),
                buffer.readUtf(MAX_SCREEN));
    }

    public static void handle(SyncLoreUnlockPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientPacketDispatch.invoke("handleSyncLore", packet)));
        context.setPacketHandled(true);
    }

    private static void writeList(FriendlyByteBuf buffer, List<String> values, int max, String label) {
        List<String> list = values == null ? List.of() : values;
        if (list.size() > max) {
            throw new IllegalArgumentException(label + " id count exceeds " + max);
        }
        buffer.writeVarInt(list.size());
        for (String value : list) {
            buffer.writeUtf(value == null ? "" : value, MAX_LEN);
        }
    }

    private static List<String> readList(FriendlyByteBuf buffer, int max, String label) {
        int count = buffer.readVarInt();
        if (count < 0 || count > max) {
            throw new DecoderException(label + " id count " + count + " exceeds " + max);
        }
        List<String> values = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            values.add(buffer.readUtf(MAX_LEN));
        }
        return List.copyOf(values);
    }

    private static List<String> sanitize(List<String> values, int max, String label) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        Set<String> set = new LinkedHashSet<>();
        for (String value : values) {
            if (value == null || value.isBlank()) {
                continue;
            }
            set.add(value.trim().toLowerCase(Locale.ROOT));
            if (set.size() > max) {
                throw new IllegalArgumentException(label + " id count exceeds " + max);
            }
        }
        return List.copyOf(set);
    }
}
