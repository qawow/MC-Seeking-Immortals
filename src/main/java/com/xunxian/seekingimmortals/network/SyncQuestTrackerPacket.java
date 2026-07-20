package com.xunxian.seekingimmortals.network;

import io.netty.handler.codec.DecoderException;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public record SyncQuestTrackerPacket(List<String> lines) {
    /** M11: 62 chains plus status and mainline headers require a bounded 72-line snapshot. */
    public static final int MAX_LINES = 72;
    public static final int MAX_LINE_LENGTH = 160;

    public static void send(ServerPlayer player, List<String> lines) {
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new SyncQuestTrackerPacket(lines == null ? List.of() : lines));
    }

    public static void encode(SyncQuestTrackerPacket packet, FriendlyByteBuf buffer) {
        List<String> list = packet.lines() == null ? List.of() : packet.lines();
        if (list.size() > MAX_LINES) {
            throw new IllegalArgumentException("quest tracker line count " + list.size() + " exceeds " + MAX_LINES);
        }
        buffer.writeVarInt(list.size());
        for (String line : list) {
            buffer.writeUtf(line == null ? "" : line, MAX_LINE_LENGTH);
        }
    }

    public static SyncQuestTrackerPacket decode(FriendlyByteBuf buffer) {
        int count = buffer.readVarInt();
        if (count < 0 || count > MAX_LINES) {
            throw new DecoderException("quest tracker line count " + count + " exceeds " + MAX_LINES);
        }
        List<String> lines = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            lines.add(buffer.readUtf(MAX_LINE_LENGTH));
        }
        return new SyncQuestTrackerPacket(List.copyOf(lines));
    }

    public static void handle(SyncQuestTrackerPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientPacketDispatch.invoke("handleSyncQuestTracker", packet)));
        context.setPacketHandled(true);
    }
}
