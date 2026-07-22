package com.xunxian.seekingimmortals.network;

import com.xunxian.seekingimmortals.quest.TextQuestChainService;
import com.xunxian.seekingimmortals.quest.TextQuestNpcHookService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.Locale;
import java.util.function.Supplier;

/**
 * Client intent for quest tracker UI.
 * Wave454: action string may encode authority ops without packet schema change:
 *   sync
 *   start:<chainId>
 *   advance:<chainId>
 *   branch:<chainId>:<branch>
 * Wave457: NPC proximity gate for advance/branch; richer tracker lines.
 * Protocol fields/order unchanged (still one UTF action).
 */
public record QuestTrackerActionPacket(String action) {
    public static void encode(QuestTrackerActionPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.action() == null ? "sync" : packet.action(), 96);
    }

    public static QuestTrackerActionPacket decode(FriendlyByteBuf buffer) {
        return new QuestTrackerActionPacket(buffer.readUtf(96));
    }

    public static void handle(QuestTrackerActionPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }
            String raw = packet.action() == null ? "sync" : packet.action().trim();
            String lower = raw.toLowerCase(Locale.ROOT);
            if (lower.startsWith("start:")) {
                String chainId = raw.substring(6).trim();
                TextQuestChainService.start(player, chainId);
            } else if (lower.startsWith("advance:")) {
                String chainId = raw.substring(8).trim();
                if (TextQuestNpcHookService.requireNearbyNpcOrWarn(player, chainId)) {
                    TextQuestChainService.advance(player, chainId);
                }
            } else if (lower.startsWith("branch:")) {
                String body = raw.substring(7).trim();
                int split = body.indexOf(':');
                if (split > 0) {
                    String chainId = body.substring(0, split).trim();
                    String branch = body.substring(split + 1).trim();
                    if (TextQuestNpcHookService.requireNearbyNpcOrWarn(player, chainId)) {
                        TextQuestChainService.chooseBranch(player, chainId, branch);
                    }
                }
            }
            // Every response is a parseable full-catalog snapshot; action errors
            // remain explicit player messages from the authority services.
            SyncQuestTrackerPacket.send(player, TextQuestChainService.buildTrackerLines(player));
        });
        context.setPacketHandled(true);
    }
}
