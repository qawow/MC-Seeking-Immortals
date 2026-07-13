package com.xunxian.seekingimmortals.network;

import com.xunxian.seekingimmortals.catalog.ManualCatalogService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.Locale;
import java.util.function.Supplier;

/**
 * Wave478: client intent for interactive method-tree UI.
 * Wave481: cultivate:<methodId> raises method layer.
 * action encodings (single UTF, no field reordering on other packets):
 *   sync
 *   learn:<methodId>
 *   cultivate:<methodId>
 * Server authority via ManualCatalogService; always re-sync learned methods/layers.
 */
public record MethodActionPacket(String action) {
    public static final int MAX_ACTION_LENGTH = 160;

    public static void encode(MethodActionPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.action() == null ? "sync" : packet.action(), MAX_ACTION_LENGTH);
    }

    public static MethodActionPacket decode(FriendlyByteBuf buffer) {
        return new MethodActionPacket(buffer.readUtf(MAX_ACTION_LENGTH));
    }

    public static void handle(MethodActionPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }
            String raw = packet.action() == null ? "sync" : packet.action().trim();
            String lower = raw.toLowerCase(Locale.ROOT);
            if (lower.startsWith("learn:")) {
                String methodId = raw.substring(6).trim();
                if (!methodId.isBlank()) {
                    ManualCatalogService.learnMethod(player, methodId);
                }
            } else if (lower.startsWith("cultivate:")) {
                String methodId = raw.substring(10).trim();
                if (!methodId.isBlank()) {
                    ManualCatalogService.cultivateMethod(player, methodId);
                }
            }
            // Always re-sync learned methods after any intent (including plain sync).
            ManualCatalogService.syncLearnedMethods(player);
        });
        context.setPacketHandled(true);
    }
}
