package com.xunxian.seekingimmortals.network;

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
    /** M11: 62 chains + status/mainline headers; protocol 22 covers the expanded encoded count range. */
    private static final int MAX_LINES = 72;
    private static final int MAX_LEN = 160;

    public static void send(ServerPlayer player, List<String> lines) {
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new SyncQuestTrackerPacket(lines == null ? List.of() : lines));
    }

    public static void encode(SyncQuestTrackerPacket packet, FriendlyByteBuf buffer) {
        List<String> list = packet.lines() == null ? List.of() : packet.lines();
        buffer.writeVarInt(Math.min(MAX_LINES, list.size()));
        int written = 0;
        for (String line : list) {
            if (written >= MAX_LINES) break;
            buffer.writeUtf(line == null ? "" : line, MAX_LEN);
            written++;
        }
    }

    public static SyncQuestTrackerPacket decode(FriendlyByteBuf buffer) {
        int count = buffer.readVarInt();
        List<String> lines = new ArrayList<>();
        for (int i = 0; i < count && i < MAX_LINES; i++) {
            lines.add(buffer.readUtf(MAX_LEN));
        }
        return new SyncQuestTrackerPacket(List.copyOf(lines));
    }

    public static void handle(SyncQuestTrackerPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            com.xunxian.seekingimmortals.client.ClientQuestTrackerData.set(packet);
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            // Wave457: never steal focus from dialogue or other screens.
            // Only open tracker when no screen is open, or refresh if already tracker.
            if (mc.screen == null) {
                mc.setScreen(new com.xunxian.seekingimmortals.client.QuestTrackerScreen());
            } else if (mc.screen instanceof com.xunxian.seekingimmortals.client.QuestTrackerScreen tracker) {
                tracker.refreshWidgets();
            }
        }));
        context.setPacketHandled(true);
    }
}
