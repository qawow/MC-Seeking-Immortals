package com.xunxian.seekingimmortals.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.Locale;
import java.util.function.Supplier;

/**
 * M16: client requests a lore screen open; server replies with unlock snapshot (read-only).
 */
public record LoreScreenActionPacket(String action) {
    private static final int MAX_ACTION = 24;

    public static void encode(LoreScreenActionPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.action() == null ? "" : packet.action(), MAX_ACTION);
    }

    public static LoreScreenActionPacket decode(FriendlyByteBuf buffer) {
        return new LoreScreenActionPacket(buffer.readUtf(MAX_ACTION));
    }

    public static void handle(LoreScreenActionPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }
            String action = packet.action() == null ? "" : packet.action().trim().toLowerCase(Locale.ROOT);
            if (action.isBlank()) {
                action = "compendium";
            }
            com.xunxian.seekingimmortals.lore.LoreSyncService.syncAndOpen(player, action);
        });
        context.setPacketHandled(true);
    }
}
