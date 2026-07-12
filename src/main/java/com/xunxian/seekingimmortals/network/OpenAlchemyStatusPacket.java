package com.xunxian.seekingimmortals.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

public record OpenAlchemyStatusPacket(int skillLevel, int skillExp, String message) {
    public static void send(ServerPlayer player, int level, int exp, String message) {
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new OpenAlchemyStatusPacket(level, exp, message == null ? "" : message));
    }

    public static void encode(OpenAlchemyStatusPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.skillLevel());
        buffer.writeVarInt(packet.skillExp());
        buffer.writeUtf(packet.message() == null ? "" : packet.message(), 192);
    }

    public static OpenAlchemyStatusPacket decode(FriendlyByteBuf buffer) {
        return new OpenAlchemyStatusPacket(buffer.readVarInt(), buffer.readVarInt(), buffer.readUtf(192));
    }

    public static void handle(OpenAlchemyStatusPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                net.minecraft.client.Minecraft.getInstance().setScreen(
                        new com.xunxian.seekingimmortals.client.AlchemyStatusScreen(
                                packet.skillLevel(), packet.skillExp(), packet.message()))));
        context.setPacketHandled(true);
    }
}
