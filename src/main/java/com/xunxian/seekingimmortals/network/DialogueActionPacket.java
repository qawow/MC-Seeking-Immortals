package com.xunxian.seekingimmortals.network;

import com.xunxian.seekingimmortals.npc.NpcDialogueApi;
import com.xunxian.seekingimmortals.quest.TextQuestDialogueService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.Locale;
import java.util.function.Supplier;

/**
 * Client-to-server intent for the current dialogue view.
 * The three-string wire layout is retained; {@code chainId} now carries the server-issued context nonce.
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
            String dialogueContext = packet.chainId == null ? "" : packet.chainId.trim();
            String choice = packet.choice == null ? "" : packet.choice.trim();

            if (NpcDialogueApi.matchesContext(player, dialogueContext)) {
                switch (action) {
                    case ACTION_TALK -> NpcDialogueApi.refresh(player, dialogueContext);
                    case ACTION_ACT -> NpcDialogueApi.selectNext(player, dialogueContext, choice);
                    case ACTION_CLOSE -> NpcDialogueApi.close(player, dialogueContext);
                    default -> {
                    }
                }
                return;
            }

            if (TextQuestDialogueService.matchesContext(player, dialogueContext)) {
                switch (action) {
                    case ACTION_TALK -> TextQuestDialogueService.refresh(player, dialogueContext);
                    case ACTION_ACT -> TextQuestDialogueService.actCurrent(player, dialogueContext, choice);
                    case ACTION_CLOSE -> TextQuestDialogueService.close(player, dialogueContext);
                    default -> {
                    }
                }
            }
        });
        context.setPacketHandled(true);
    }
}
