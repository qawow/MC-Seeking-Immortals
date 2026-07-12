package com.xunxian.seekingimmortals.network;

import com.xunxian.seekingimmortals.quest.TextQuestDialogueService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.Locale;
import java.util.function.Supplier;

/**
 * Client→server dialogue actions for visual dialogue GUI.
 * PROTOCOL_VERSION 11.
 */
public record DialogueActionPacket(String action, String chainId, String choice) {
    public static final String ACTION_TALK = "talk";
    public static final String ACTION_ACT = "act";
    public static final String ACTION_CLOSE = "close";

    private static final int MAX_ACTION = 32;
    private static final int MAX_ID = 96;
    private static final int MAX_CHOICE = 64;

    public static void encode(DialogueActionPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.action == null ? "" : packet.action, MAX_ACTION);
        buffer.writeUtf(packet.chainId == null ? "" : packet.chainId, MAX_ID);
        buffer.writeUtf(packet.choice == null ? "" : packet.choice, MAX_CHOICE);
    }

    public static DialogueActionPacket decode(FriendlyByteBuf buffer) {
        return new DialogueActionPacket(
                buffer.readUtf(MAX_ACTION),
                buffer.readUtf(MAX_ID),
                buffer.readUtf(MAX_CHOICE));
    }

    public static void handle(DialogueActionPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }
            String action = packet.action == null ? "" : packet.action.trim().toLowerCase(Locale.ROOT);
            String chainId = packet.chainId == null ? "" : packet.chainId.trim();
            String choice = packet.choice == null ? "" : packet.choice.trim();
            switch (action) {
                case ACTION_TALK -> TextQuestDialogueService.talk(player, chainId);
                case ACTION_ACT -> TextQuestDialogueService.act(player, chainId, choice);
                case ACTION_CLOSE -> {
                    // no-op server state
                }
                default -> TextQuestDialogueService.talk(player, chainId);
            }
            // Re-open/refresh GUI after act so client sees updated branch/stage text.
            if (!ACTION_CLOSE.equals(action) && !chainId.isBlank()) {
                OpenDialogueScreenPacket.send(player, chainId);
            }
        });
        context.setPacketHandled(true);
    }
}
