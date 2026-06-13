package com.xunxian.seekingimmortals.network;

import com.xunxian.seekingimmortals.cultivation.CultivationHelper;
import com.xunxian.seekingimmortals.entity.CushionSeatEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record SetMeditatingPacket(boolean meditating) {
    public static void encode(SetMeditatingPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.meditating);
    }

    public static SetMeditatingPacket decode(FriendlyByteBuf buffer) {
        return new SetMeditatingPacket(buffer.readBoolean());
    }

    public static void handle(SetMeditatingPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            CultivationHelper.get(player).ifPresent(cultivation -> {
                if (!packet.meditating && player.getVehicle() instanceof CushionSeatEntity seat) {
                    net.minecraft.core.BlockPos cushionPos = seat.getCushionPos();
                    player.stopRiding();
                    player.setPos(cushionPos.getX() + 0.5D, cushionPos.getY() + 6.0D / 16.0D, cushionPos.getZ() + 0.5D);
                }
                cultivation.setMeditating(packet.meditating);
                player.displayClientMessage(Component.translatable(
                        packet.meditating ? "message.seeking_immortals.meditation.start" : "message.seeking_immortals.meditation.stop"), true);
                SyncCultivationDataPacket.send(player, cultivation);
            });
        });
        context.setPacketHandled(true);
    }
}
