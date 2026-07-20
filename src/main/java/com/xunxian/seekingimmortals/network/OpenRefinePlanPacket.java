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

public record OpenRefinePlanPacket(List<String> lines) {
    private static final int MAX = 24;

    public static void send(ServerPlayer player, List<String> lines) {
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new OpenRefinePlanPacket(lines == null ? List.of() : lines));
    }

    public static void encode(OpenRefinePlanPacket packet, FriendlyByteBuf buffer) {
        List<String> list = packet.lines() == null ? List.of() : packet.lines();
        buffer.writeVarInt(Math.min(MAX, list.size()));
        int n = 0;
        for (String line : list) {
            if (n++ >= MAX) break;
            buffer.writeUtf(line == null ? "" : line, 160);
        }
    }

    public static OpenRefinePlanPacket decode(FriendlyByteBuf buffer) {
        int count = buffer.readVarInt();
        List<String> lines = new ArrayList<>();
        for (int i = 0; i < count && i < MAX; i++) lines.add(buffer.readUtf(160));
        return new OpenRefinePlanPacket(List.copyOf(lines));
    }

    public static void handle(OpenRefinePlanPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientPacketDispatch.invoke("handleOpenRefinePlan", packet)));
        context.setPacketHandled(true);
    }
}
