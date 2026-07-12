package com.xunxian.seekingimmortals.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

/**
 * Server→client: open auction GUI. PROTOCOL_VERSION 10.
 */
public record OpenAuctionScreenPacket() {
    public static void send(ServerPlayer player) {
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new OpenAuctionScreenPacket());
    }

    public static void encode(OpenAuctionScreenPacket packet, FriendlyByteBuf buffer) {
        // no payload
    }

    public static OpenAuctionScreenPacket decode(FriendlyByteBuf buffer) {
        return new OpenAuctionScreenPacket();
    }

    public static void handle(OpenAuctionScreenPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                net.minecraft.client.Minecraft.getInstance().setScreen(new com.xunxian.seekingimmortals.client.AuctionScreen())));
        context.setPacketHandled(true);
    }
}
