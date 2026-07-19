package com.xunxian.seekingimmortals.network;

import com.xunxian.seekingimmortals.catalog.AuctionSoftService;
import com.xunxian.seekingimmortals.menu.AuctionHallMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.Locale;
import java.util.function.Supplier;

/**
 * Client→server auction actions. Mirrors ShopActionPacket pattern.
 * PROTOCOL_VERSION must bump when this packet is added.
 */
public record AuctionActionPacket(String action, String id, long accessToken) {
    public static final String ACTION_PREVIEW = "preview";
    public static final String ACTION_BID = "bid";
    public static final String ACTION_SETTLE = "settle";
    public static final String ACTION_LIST = "list";
    /** Wave491: refresh live ladder page; id carries page number. */
    public static final String ACTION_PAGE = "page";

    private static final int MAX_ACTION = 64;
    private static final int MAX_ID = 96;

    public AuctionActionPacket(String action, String id) {
        this(action, id, 0L);
    }

    public static void encode(AuctionActionPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.action == null ? "" : packet.action, MAX_ACTION);
        buffer.writeUtf(packet.id == null ? "" : packet.id, MAX_ID);
        buffer.writeLong(packet.accessToken);
    }

    public static AuctionActionPacket decode(FriendlyByteBuf buffer) {
        return new AuctionActionPacket(buffer.readUtf(MAX_ACTION), buffer.readUtf(MAX_ID), buffer.readLong());
    }

    public static void handle(AuctionActionPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }
            if (!(player.containerMenu instanceof AuctionHallMenu menu)
                    || !menu.authorizes(player, packet.accessToken)) {
                player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                        "message.seeking_immortals.menu.invalid_context"), true);
                return;
            }
            String action = packet.action == null ? "" : packet.action.trim().toLowerCase(Locale.ROOT);
            String id = packet.id == null ? "" : packet.id.trim();
            switch (action) {
                case ACTION_BID -> AuctionSoftService.bid(player, id);
                case ACTION_SETTLE -> AuctionSoftService.settle(player, id);
                case ACTION_PREVIEW -> AuctionSoftService.preview(player, id);
                case ACTION_PAGE -> {
                    int page = 0;
                    try {
                        page = Integer.parseInt(id == null || id.isBlank() ? "0" : id.trim());
                    } catch (NumberFormatException ignored) {
                        page = 0;
                    }
                    AuctionSoftService.syncLadder(player, page);
                }
                case ACTION_LIST -> {
                    AuctionSoftService.syncLadder(player, 0);
                    AuctionSoftService.Snapshot snapshot = AuctionSoftService.builtin();
                    player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                            "command.seeking_immortals.catalog.auction.summary",
                            snapshot.venueCount(), snapshot.lotCount(), snapshot.minIncrementPct()), false);
                }
                default -> player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                        "message.seeking_immortals.auction.unknown", action), false);
            }
        });
        context.setPacketHandled(true);
    }
}
