package com.xunxian.seekingimmortals.network;

import com.xunxian.seekingimmortals.catalog.ManualCatalogService;
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
import java.util.Map;
import java.util.function.Supplier;

/**
 * Wave477: server-authoritative learned cultivation methods sync.
 * Wave481 (protocol 16): each entry includes a server-authoritative method layer.
 */
public record SyncLearnedMethodsPacket(List<Entry> learnedMethods) {
    public static final int MAX_METHOD_ID_LENGTH = 128;
    public static final int MAX_LEARNED_METHODS = 256;

    public record Entry(String id, int layer) {
        public Entry {
            id = id == null ? "" : id;
            layer = Math.max(0, Math.min(ManualCatalogService.maxMethodLayer(id), layer));
        }
    }

    public static SyncLearnedMethodsPacket from(ServerPlayer player) {
        Map<String, Integer> layers = ManualCatalogService.learnedMethodLayers(player);
        List<Entry> entries = new ArrayList<>(layers.size());
        for (Map.Entry<String, Integer> e : layers.entrySet()) {
            entries.add(new Entry(e.getKey(), e.getValue()));
        }
        return new SyncLearnedMethodsPacket(List.copyOf(entries));
    }

    public static void send(ServerPlayer player) {
        if (player == null) {
            return;
        }
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), from(player));
    }

    public static void encode(SyncLearnedMethodsPacket packet, FriendlyByteBuf buffer) {
        List<Entry> methods = packet.learnedMethods == null ? List.of() : packet.learnedMethods;
        if (methods.size() > MAX_LEARNED_METHODS) {
            throw new IllegalArgumentException("learned methods count " + methods.size() + " exceeds " + MAX_LEARNED_METHODS);
        }
        buffer.writeVarInt(methods.size());
        for (Entry entry : methods) {
            buffer.writeUtf(entry.id() == null ? "" : entry.id(), MAX_METHOD_ID_LENGTH);
            buffer.writeVarInt(entry.layer());
        }
    }

    public static SyncLearnedMethodsPacket decode(FriendlyByteBuf buffer) {
        int size = buffer.readVarInt();
        if (size < 0 || size > MAX_LEARNED_METHODS) {
            throw new DecoderException("learned methods count " + size + " exceeds " + MAX_LEARNED_METHODS);
        }
        List<Entry> methods = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            String id = buffer.readUtf(MAX_METHOD_ID_LENGTH);
            int layer = buffer.readVarInt();
            methods.add(new Entry(id, layer));
        }
        return new SyncLearnedMethodsPacket(List.copyOf(methods));
    }

    public static void handle(SyncLearnedMethodsPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            Map<String, Integer> map = new LinkedHashMap<>();
            if (packet.learnedMethods != null) {
                for (Entry entry : packet.learnedMethods) {
                    if (entry == null || entry.id() == null || entry.id().isBlank()) {
                        continue;
                    }
                    map.put(entry.id().trim().toLowerCase(java.util.Locale.ROOT), Math.max(1, entry.layer()));
                }
            }
            com.xunxian.seekingimmortals.client.ClientMethodData.setLearnedMethods(map);
        }));
        context.setPacketHandled(true);
    }
}
