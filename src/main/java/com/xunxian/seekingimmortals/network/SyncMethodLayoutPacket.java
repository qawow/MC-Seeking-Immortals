package com.xunxian.seekingimmortals.network;

import com.xunxian.seekingimmortals.catalog.MethodLayoutService;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Wave486 (protocol 17): server -> client freeform method-tree layout offsets.
 */
public record SyncMethodLayoutPacket(List<Entry> entries) {
    public static final int MAX_ENTRIES = 256;
    public static final int MAX_ID_LENGTH = 128;

    public record Entry(String id, int x, int y) {}

    public static SyncMethodLayoutPacket from(ServerPlayer player) {
        Map<String, int[]> layout = MethodLayoutService.layoutOf(player);
        List<Entry> list = new ArrayList<>(layout.size());
        layout.forEach((id, xy) -> list.add(new Entry(id, xy[0], xy[1])));
        return new SyncMethodLayoutPacket(List.copyOf(list));
    }

    public static void send(ServerPlayer player) {
        if (player == null) {
            return;
        }
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), from(player));
    }

    public static void encode(SyncMethodLayoutPacket packet, FriendlyByteBuf buffer) {
        List<Entry> list = packet.entries() == null ? List.of() : packet.entries();
        if (list.size() > MAX_ENTRIES) {
            throw new IllegalArgumentException("layout entries " + list.size() + " exceeds " + MAX_ENTRIES);
        }
        buffer.writeVarInt(list.size());
        for (Entry entry : list) {
            buffer.writeUtf(entry.id() == null ? "" : entry.id(), MAX_ID_LENGTH);
            buffer.writeVarInt(entry.x());
            buffer.writeVarInt(entry.y());
        }
    }

    public static SyncMethodLayoutPacket decode(FriendlyByteBuf buffer) {
        int size = buffer.readVarInt();
        if (size < 0 || size > MAX_ENTRIES) {
            throw new DecoderException("layout entries " + size + " exceeds " + MAX_ENTRIES);
        }
        List<Entry> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            list.add(new Entry(buffer.readUtf(MAX_ID_LENGTH), buffer.readVarInt(), buffer.readVarInt()));
        }
        return new SyncMethodLayoutPacket(List.copyOf(list));
    }

    public static void handle(SyncMethodLayoutPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            Map<String, int[]> map = new LinkedHashMap<>();
            if (packet.entries() != null) {
                for (Entry entry : packet.entries()) {
                    if (entry == null || entry.id() == null || entry.id().isBlank()) {
                        continue;
                    }
                    map.put(entry.id().trim().toLowerCase(Locale.ROOT), new int[]{entry.x(), entry.y()});
                }
            }
            com.xunxian.seekingimmortals.client.ClientMethodLayoutData.set(map);
        }));
        context.setPacketHandled(true);
    }
}
