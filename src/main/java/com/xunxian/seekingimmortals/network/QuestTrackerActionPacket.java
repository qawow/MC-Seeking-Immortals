package com.xunxian.seekingimmortals.network;

import com.xunxian.seekingimmortals.cultivation.CultivationHelper;
import com.xunxian.seekingimmortals.quest.TextQuestChainService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public record QuestTrackerActionPacket(String action) {
    public static void encode(QuestTrackerActionPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.action() == null ? "sync" : packet.action(), 32);
    }

    public static QuestTrackerActionPacket decode(FriendlyByteBuf buffer) {
        return new QuestTrackerActionPacket(buffer.readUtf(32));
    }

    public static void handle(QuestTrackerActionPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }
            List<String> lines = new ArrayList<>();
            CultivationHelper.get(player).ifPresent(cultivation -> {
                var progress = cultivation.getSevenMysteriesQuest();
                lines.add("mainline stage=" + progress.getStage()
                        + " sect=" + progress.getSectId()
                        + " rankStage=" + progress.getSectQuestStage()
                        + " contrib=" + progress.getContribution());
            });
            int shown = 0;
            for (TextQuestChainService.ChainProgress chain : TextQuestChainService.listProgress(player)) {
                if (chain.stage() <= 0 && !chain.complete()) {
                    continue;
                }
                lines.add(chain.id() + " " + chain.stage() + "/" + chain.stepCount()
                        + (chain.complete() ? " DONE" : "")
                        + " branch=" + TextQuestChainService.getBranch(player, chain.id()));
                if (++shown >= 20) {
                    break;
                }
            }
            if (lines.size() == 1) {
                lines.add("(no active text quest chains)");
            }
            SyncQuestTrackerPacket.send(player, lines);
        });
        context.setPacketHandled(true);
    }
}
