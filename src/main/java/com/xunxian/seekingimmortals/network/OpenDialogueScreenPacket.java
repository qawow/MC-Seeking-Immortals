package com.xunxian.seekingimmortals.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

/**
 * Server→client: open text-quest dialogue GUI.
 * PROTOCOL_VERSION 11.
 */
public record OpenDialogueScreenPacket(String chainId) {
    private static final int MAX_ID = 96;

    public static void send(ServerPlayer player, String chainId) {
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new OpenDialogueScreenPacket(chainId == null ? "" : chainId));
    }

    public static void encode(OpenDialogueScreenPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.chainId == null ? "" : packet.chainId, MAX_ID);
    }

    public static OpenDialogueScreenPacket decode(FriendlyByteBuf buffer) {
        return new OpenDialogueScreenPacket(buffer.readUtf(MAX_ID));
    }

    public static void handle(OpenDialogueScreenPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                net.minecraft.client.Minecraft.getInstance().setScreen(
                        new com.xunxian.seekingimmortals.client.DialogueScreen(packet.chainId()))));
        context.setPacketHandled(true);
    }
}
