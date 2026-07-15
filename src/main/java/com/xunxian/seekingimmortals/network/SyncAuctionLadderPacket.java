package com.xunxian.seekingimmortals.network;

import io.netty.handler.codec.DecoderException;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Wave491 protocol 18: server→client live auction ladder snapshot for hall pagination UI.
 */
public record SyncAuctionLadderPacket(int page, int pageSize, int totalLots, List<LotBid> lots) {
    private static final int MAX_TEXT = 96;
    private static final int MAX_LOTS = 32;

    public static void send(ServerPlayer player, SyncAuctionLadderPacket packet) {
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static void encode(SyncAuctionLadderPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(Math.max(0, packet.page));
        buffer.writeVarInt(Math.max(1, packet.pageSize));
        buffer.writeVarInt(Math.max(0, packet.totalLots));
        List<LotBid> lots = packet.lots == null ? List.of() : packet.lots;
        if (lots.size() > MAX_LOTS) {
            throw new IllegalArgumentException("auction ladder lots exceed " + MAX_LOTS);
        }
        buffer.writeVarInt(lots.size());
        for (LotBid lot : lots) {
            buffer.writeUtf(lot.lotId() == null ? "" : lot.lotId(), MAX_TEXT);
            buffer.writeUtf(lot.display() == null ? "" : lot.display(), MAX_TEXT);
            buffer.writeVarInt(Math.max(0, lot.current()));
            buffer.writeVarInt(Math.max(0, lot.next()));
            buffer.writeVarInt(Math.max(0, lot.minEquiv()));
            buffer.writeVarInt(Math.max(0, lot.maxEquiv()));
            buffer.writeUtf(lot.leaderName() == null ? "" : lot.leaderName(), MAX_TEXT);
            buffer.writeBoolean(lot.settled());
        }
    }

    public static SyncAuctionLadderPacket decode(FriendlyByteBuf buffer) {
        int page = buffer.readVarInt();
        int pageSize = buffer.readVarInt();
        int totalLots = buffer.readVarInt();
        int count = buffer.readVarInt();
        if (count < 0 || count > MAX_LOTS) {
            throw new DecoderException("auction ladder lot count " + count + " exceeds " + MAX_LOTS);
        }
        List<LotBid> lots = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            lots.add(new LotBid(
                    buffer.readUtf(MAX_TEXT),
                    buffer.readUtf(MAX_TEXT),
                    buffer.readVarInt(),
                    buffer.readVarInt(),
                    buffer.readVarInt(),
                    buffer.readVarInt(),
                    buffer.readUtf(MAX_TEXT),
                    buffer.readBoolean()));
        }
        return new SyncAuctionLadderPacket(page, pageSize, totalLots, List.copyOf(lots));
    }

    public static void handle(SyncAuctionLadderPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                com.xunxian.seekingimmortals.client.ClientAuctionLadderData.set(packet)));
        context.setPacketHandled(true);
    }

    public record LotBid(String lotId, String display, int current, int next, int minEquiv, int maxEquiv,
                         String leaderName, boolean settled) {}
}
