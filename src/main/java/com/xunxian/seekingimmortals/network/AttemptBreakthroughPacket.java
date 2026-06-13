package com.xunxian.seekingimmortals.network;

import com.xunxian.seekingimmortals.cultivation.BreakthroughService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record AttemptBreakthroughPacket() {
    public static void encode(AttemptBreakthroughPacket packet, FriendlyByteBuf buffer) {}

    public static AttemptBreakthroughPacket decode(FriendlyByteBuf buffer) {
        return new AttemptBreakthroughPacket();
    }

    public static void handle(AttemptBreakthroughPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                BreakthroughService.attempt(player);
            }
        });
        context.setPacketHandled(true);
    }
}
