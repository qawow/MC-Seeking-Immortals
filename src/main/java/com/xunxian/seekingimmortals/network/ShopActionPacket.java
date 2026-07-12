package com.xunxian.seekingimmortals.network;

import com.xunxian.seekingimmortals.shop.ShopService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record ShopActionPacket(String action, String shopId, String entryId) {
    private static final int MAX_ACTION = 64;
    private static final int MAX_SHOP_ID = 96;
    private static final int MAX_ENTRY_ID = 96;

    public static void encode(ShopActionPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.action == null ? "" : packet.action, MAX_ACTION);
        buffer.writeUtf(packet.shopId == null ? "" : packet.shopId, MAX_SHOP_ID);
        buffer.writeUtf(packet.entryId == null ? "" : packet.entryId, MAX_ENTRY_ID);
    }

    public static ShopActionPacket decode(FriendlyByteBuf buffer) {
        return new ShopActionPacket(
                buffer.readUtf(MAX_ACTION),
                buffer.readUtf(MAX_SHOP_ID),
                buffer.readUtf(MAX_ENTRY_ID));
    }

    public static void handle(ShopActionPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                ShopService.handleClientAction(player, packet.action, packet.shopId, packet.entryId);
            }
        });
        context.setPacketHandled(true);
    }
}
