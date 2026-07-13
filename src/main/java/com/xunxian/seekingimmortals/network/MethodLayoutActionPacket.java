package com.xunxian.seekingimmortals.network;

import com.xunxian.seekingimmortals.catalog.MethodLayoutService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.Locale;
import java.util.function.Supplier;

/**
 * Wave486 (protocol 17): client intent for freeform method-tree layout.
 * Actions:
 *   sync
 *   clear
 *   set:<methodId>:<x>:<y>
 */
public record MethodLayoutActionPacket(String action) {
    public static final int MAX_ACTION_LENGTH = 192;

    public static void encode(MethodLayoutActionPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.action() == null ? "sync" : packet.action(), MAX_ACTION_LENGTH);
    }

    public static MethodLayoutActionPacket decode(FriendlyByteBuf buffer) {
        return new MethodLayoutActionPacket(buffer.readUtf(MAX_ACTION_LENGTH));
    }

    public static void handle(MethodLayoutActionPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }
            String raw = packet.action() == null ? "sync" : packet.action().trim();
            String lower = raw.toLowerCase(Locale.ROOT);
            if (lower.equals("clear")) {
                MethodLayoutService.clear(player);
            } else if (lower.startsWith("set:")) {
                String body = raw.substring(4);
                String[] parts = body.split(":");
                if (parts.length >= 3) {
                    String id = parts[0].trim();
                    try {
                        int x = Integer.parseInt(parts[1].trim());
                        int y = Integer.parseInt(parts[2].trim());
                        MethodLayoutService.setOffset(player, id, x, y);
                    } catch (NumberFormatException ignored) {
                        // ignore malformed client payload
                    }
                }
            }
            MethodLayoutService.sync(player);
        });
        context.setPacketHandled(true);
    }
}
