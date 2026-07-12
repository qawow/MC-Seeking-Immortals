package com.xunxian.seekingimmortals.network;

import com.xunxian.seekingimmortals.catalog.AuctionSoftService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.Locale;
import java.util.function.Supplier;

/**
 * Client→server auction actions. Mirrors ShopActionPacket pattern.
 * PROTOCOL_VERSION must bump when this packet is added.
 */
public record AuctionActionPacket(String action, String id) {
    public static final String ACTION_PREVIEW = "preview";
    public static final String ACTION_BID = "bid";
    public static final String ACTION_SETTLE = "settle";
    public static final String ACTION_LIST = "list";

    private static final int MAX_ACTION = 64;
    private static final int MAX_ID = 96;

    public static void encode(AuctionActionPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.action == null ? "" : packet.action, MAX_ACTION);
        buffer.writeUtf(packet.id == null ? "" : packet.id, MAX_ID);
    }

    public static AuctionActionPacket decode(FriendlyByteBuf buffer) {
        return new AuctionActionPacket(buffer.readUtf(MAX_ACTION), buffer.readUtf(MAX_ID));
    }

    public static void handle(AuctionActionPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }
            String action = packet.action == null ? "" : packet.action.trim().toLowerCase(Locale.ROOT);
            String id = packet.id == null ? "" : packet.id.trim();
            switch (action) {
                case ACTION_BID -> AuctionSoftService.bid(player, id);
                case ACTION_SETTLE -> AuctionSoftService.settle(player, id);
                case ACTION_PREVIEW -> AuctionSoftService.preview(player, id);
                case ACTION_LIST -> {
                    AuctionSoftService.Snapshot snapshot = AuctionSoftService.builtin();
                    player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                            "command.seeking_immortals.catalog.auction.summary",
                            snapshot.venueCount(), snapshot.lotCount(), snapshot.minIncrementPct()), false);
                    int shown = 0;
                    for (AuctionSoftService.Lot lot : snapshot.lots()) {
                        player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                                "lot " + lot.id() + " | " + lot.display()), false);
                        if (++shown >= 8) break;
                    }
                }
                default -> player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                        "message.seeking_immortals.auction.unknown", action), false);
            }
        });
        context.setPacketHandled(true);
    }
}
