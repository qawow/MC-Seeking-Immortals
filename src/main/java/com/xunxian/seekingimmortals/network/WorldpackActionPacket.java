package com.xunxian.seekingimmortals.network;

import com.xunxian.seekingimmortals.worldpack.WorldpackGameplayService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record WorldpackActionPacket(String action, String targetId) {
    private static final int MAX_ACTION = 64;
    private static final int MAX_TARGET_ID = 96;

    public static void encode(WorldpackActionPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.action == null ? "" : packet.action, MAX_ACTION);
        buffer.writeUtf(packet.targetId == null ? "" : packet.targetId, MAX_TARGET_ID);
    }

    public static WorldpackActionPacket decode(FriendlyByteBuf buffer) {
        return new WorldpackActionPacket(
                buffer.readUtf(MAX_ACTION),
                buffer.readUtf(MAX_TARGET_ID));
    }

    public static void handle(WorldpackActionPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                WorldpackGameplayService.handleClientAction(player, packet.action, packet.targetId);
            }
        });
        context.setPacketHandled(true);
    }
}
