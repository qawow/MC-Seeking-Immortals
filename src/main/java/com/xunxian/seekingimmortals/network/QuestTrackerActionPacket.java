package com.xunxian.seekingimmortals.network;

import com.xunxian.seekingimmortals.cultivation.CultivationHelper;
import com.xunxian.seekingimmortals.quest.TextQuestChainService;
import com.xunxian.seekingimmortals.quest.TextQuestNpcHookService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
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
            String status = "OK sync";
            if (lower.startsWith("start:")) {
                String chainId = raw.substring(6).trim();
                boolean ok = TextQuestChainService.start(player, chainId);
                status = (ok ? "OK" : "ERR") + " start " + chainId;
            } else if (lower.startsWith("advance:")) {
                String chainId = raw.substring(8).trim();
                if (!TextQuestNpcHookService.requireNearbyNpcOrWarn(player, chainId)) {
                    status = "ERR need_npc " + chainId;
                } else {
                    boolean ok = TextQuestChainService.advance(player, chainId);
                    status = (ok ? "OK" : "ERR") + " advance " + chainId;
                }
            } else if (lower.startsWith("branch:")) {
                String body = raw.substring(7).trim();
                int split = body.indexOf(':');
                if (split > 0) {
                    String chainId = body.substring(0, split).trim();
                    String branch = body.substring(split + 1).trim();
                    if (!TextQuestNpcHookService.requireNearbyNpcOrWarn(player, chainId)) {
                        status = "ERR need_npc " + chainId;
                    } else {
                        boolean ok = TextQuestChainService.chooseBranch(player, chainId, branch);
                        status = (ok ? "OK" : "ERR") + " branch " + chainId + " " + branch;
                    }
                } else {
                    status = "ERR branch_format";
                }
            }
            // Always sync after intent (including plain sync).
            List<String> lines = new ArrayList<>();
            lines.add(status);
            CultivationHelper.get(player).ifPresent(cultivation -> {
                var progress = cultivation.getSevenMysteriesQuest();
                lines.add("mainline stage=" + progress.getStage()
                        + " sect=" + progress.getSectId()
                        + " rankStage=" + progress.getSectQuestStage()
                        + " contrib=" + progress.getContribution());
            });
            for (String line : TextQuestChainService.buildTrackerLines(player)) {
                // M11: align with SyncQuestTrackerPacket.MAX_LINES (72).
                if (lines.size() >= 72) {
                    break;
                }
                // Skip empty placeholder when we already have mainline + status.
                if ("(no active text quest chains)".equals(line) && lines.size() > 1) {
                    continue;
                }
                lines.add(line);
            }
            if (lines.size() <= 2) {
                lines.add("(no active text quest chains)");
            }
            SyncQuestTrackerPacket.send(player, lines);
        });
        context.setPacketHandled(true);
    }
}
