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
 * Wave49: EntryData includes rankMin + locked for shop rank-lock UI.
 * Protocol bump required (11 -> 12).
 */
public record SyncShopDataPacket(String shopId, String titleKey, List<EntryData> entries, boolean openScreen) {
    private static final int MAX_TEXT = 128;
    private static final int MAX_KEY = 192;
    private static final int MAX_ENTRIES = 64;

    public static void send(ServerPlayer player, SyncShopDataPacket packet) {
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static void encode(SyncShopDataPacket packet, FriendlyByteBuf buffer) {
        write(buffer, packet.shopId, MAX_TEXT);
        write(buffer, packet.titleKey, MAX_KEY);
        writeEntries(packet.entries, buffer);
        buffer.writeBoolean(packet.openScreen);
    }

    public static SyncShopDataPacket decode(FriendlyByteBuf buffer) {
        String shopId = buffer.readUtf(MAX_TEXT);
        String titleKey = buffer.readUtf(MAX_KEY);
        List<EntryData> entries = readEntries(buffer);
        boolean openScreen = buffer.readBoolean();
        return new SyncShopDataPacket(shopId, titleKey, entries, openScreen);
    }

    public static void handle(SyncShopDataPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            com.xunxian.seekingimmortals.client.ClientShopData.set(packet);
            if (packet.openScreen()) {
                net.minecraft.client.Minecraft.getInstance().setScreen(new com.xunxian.seekingimmortals.client.ShopScreen());
            }
        }));
        context.setPacketHandled(true);
    }

    private static void writeEntries(List<EntryData> entries, FriendlyByteBuf buffer) {
        List<EntryData> safeEntries = entries == null ? List.of() : entries;
        if (safeEntries.size() > MAX_ENTRIES) {
            throw new IllegalArgumentException("shop entries exceed " + MAX_ENTRIES);
        }
        buffer.writeVarInt(safeEntries.size());
        for (EntryData entry : safeEntries) {
            write(buffer, entry.id(), MAX_TEXT);
            write(buffer, entry.itemDescriptionId(), MAX_KEY);
            buffer.writeVarInt(Math.max(1, entry.count()));
            buffer.writeVarInt(Math.max(1, entry.cost()));
            write(buffer, entry.currency(), MAX_TEXT);
            write(buffer, entry.currencyDescriptionId(), MAX_KEY);
            buffer.writeVarInt(entry.remainingStock());
            buffer.writeVarLong(Math.max(0L, entry.nextRefreshTicks()));
            write(buffer, entry.rankMin(), MAX_TEXT);
            buffer.writeBoolean(entry.locked());
        }
    }

    private static List<EntryData> readEntries(FriendlyByteBuf buffer) {
        int count = readCount(buffer, MAX_ENTRIES, "shop entry");
        List<EntryData> entries = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            entries.add(new EntryData(
                    buffer.readUtf(MAX_TEXT),
                    buffer.readUtf(MAX_KEY),
                    buffer.readVarInt(),
                    buffer.readVarInt(),
                    buffer.readUtf(MAX_TEXT),
                    buffer.readUtf(MAX_KEY),
                    buffer.readVarInt(),
                    buffer.readVarLong(),
                    buffer.readUtf(MAX_TEXT),
                    buffer.readBoolean()));
        }
        return List.copyOf(entries);
    }

    private static int readCount(FriendlyByteBuf buffer, int max, String label) {
        int count = buffer.readVarInt();
        if (count < 0 || count > max) {
            throw new DecoderException(label + " count " + count + " exceeds " + max);
        }
        return count;
    }

    private static void write(FriendlyByteBuf buffer, String value, int maxLength) {
        buffer.writeUtf(value == null ? "" : value, maxLength);
    }

    public record EntryData(String id, String itemDescriptionId, int count, int cost, String currency,
                            String currencyDescriptionId, int remainingStock, long nextRefreshTicks,
                            String rankMin, boolean locked) {
        public EntryData(String id, String itemDescriptionId, int count, int cost, String currency,
                         String currencyDescriptionId, int remainingStock, long nextRefreshTicks) {
            this(id, itemDescriptionId, count, cost, currency, currencyDescriptionId, remainingStock, nextRefreshTicks, "", false);
        }
    }
}
