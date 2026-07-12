package com.xunxian.seekingimmortals.network;

import com.xunxian.seekingimmortals.sect.SectContributionService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record SectActionPacket(String action, String targetId, String extra) {
    private static final int MAX_ACTION = 64;
    private static final int MAX_TARGET_ID = 96;
    private static final int MAX_EXTRA = 160;

    public SectActionPacket(String action, String targetId) {
        this(action, targetId, "");
    }

    public static void encode(SectActionPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.action == null ? "" : packet.action, MAX_ACTION);
        buffer.writeUtf(packet.targetId == null ? "" : packet.targetId, MAX_TARGET_ID);
        buffer.writeUtf(packet.extra == null ? "" : packet.extra, MAX_EXTRA);
    }

    public static SectActionPacket decode(FriendlyByteBuf buffer) {
        return new SectActionPacket(
                buffer.readUtf(MAX_ACTION),
                buffer.readUtf(MAX_TARGET_ID),
                buffer.readUtf(MAX_EXTRA));
    }

    public static void handle(SectActionPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                SectContributionService.handleClientAction(player, packet.action, packet.targetId, packet.extra);
            }
        });
        context.setPacketHandled(true);
    }
}
