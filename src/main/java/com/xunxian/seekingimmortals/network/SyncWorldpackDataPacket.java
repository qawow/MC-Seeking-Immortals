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

public record SyncWorldpackDataPacket(String currentRegionId, String currentRegionDisplay,
                                      String activeSecretRealmId, String activeSecretRealmDisplay,
                                      String dailyEventId, String dailyEventDisplay,
                                      long dailyEventRemainingTicks, List<String> dailyEventEffects,
                                      List<RegionData> regions, List<RealmData> realms, boolean openScreen) {
    private static final int MAX_TEXT = 128;
    private static final int MAX_KEY = 192;
    private static final int MAX_EFFECTS = 16;
    private static final int MAX_REGIONS = 32;
    private static final int MAX_REALMS = 64;

    public static void send(ServerPlayer player, SyncWorldpackDataPacket packet) {
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static void encode(SyncWorldpackDataPacket packet, FriendlyByteBuf buffer) {
        write(buffer, packet.currentRegionId, MAX_TEXT);
        write(buffer, packet.currentRegionDisplay, MAX_TEXT);
        write(buffer, packet.activeSecretRealmId, MAX_TEXT);
        write(buffer, packet.activeSecretRealmDisplay, MAX_TEXT);
        write(buffer, packet.dailyEventId, MAX_TEXT);
        write(buffer, packet.dailyEventDisplay, MAX_TEXT);
        buffer.writeVarLong(Math.max(0L, packet.dailyEventRemainingTicks));
        writeStrings(packet.dailyEventEffects, buffer, MAX_EFFECTS, "worldpack effect");
        writeRegions(packet.regions, buffer);
        writeRealms(packet.realms, buffer);
        buffer.writeBoolean(packet.openScreen);
    }

    public static SyncWorldpackDataPacket decode(FriendlyByteBuf buffer) {
        String currentRegionId = buffer.readUtf(MAX_TEXT);
        String currentRegionDisplay = buffer.readUtf(MAX_TEXT);
        String activeSecretRealmId = buffer.readUtf(MAX_TEXT);
        String activeSecretRealmDisplay = buffer.readUtf(MAX_TEXT);
        String dailyEventId = buffer.readUtf(MAX_TEXT);
        String dailyEventDisplay = buffer.readUtf(MAX_TEXT);
        long dailyEventRemainingTicks = buffer.readVarLong();
        List<String> effects = readStrings(buffer, MAX_EFFECTS, "worldpack effect");
        List<RegionData> regions = readRegions(buffer);
        List<RealmData> realms = readRealms(buffer);
        boolean openScreen = buffer.readBoolean();
        return new SyncWorldpackDataPacket(currentRegionId, currentRegionDisplay, activeSecretRealmId,
                activeSecretRealmDisplay, dailyEventId, dailyEventDisplay, dailyEventRemainingTicks,
                effects, regions, realms, openScreen);
    }

    public static void handle(SyncWorldpackDataPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            com.xunxian.seekingimmortals.client.ClientWorldpackData.set(packet);
            if (packet.openScreen()) {
                net.minecraft.client.Minecraft.getInstance().setScreen(new com.xunxian.seekingimmortals.client.WorldpackScreen());
            }
        }));
        context.setPacketHandled(true);
    }

    private static void writeStrings(List<String> values, FriendlyByteBuf buffer, int max, String label) {
        List<String> safeValues = values == null ? List.of() : values;
        if (safeValues.size() > max) {
            throw new IllegalArgumentException(label + " count exceeds " + max);
        }
        buffer.writeVarInt(safeValues.size());
        for (String value : safeValues) {
            write(buffer, value, MAX_TEXT);
        }
    }

    private static List<String> readStrings(FriendlyByteBuf buffer, int max, String label) {
        int count = readCount(buffer, max, label);
        List<String> values = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            values.add(buffer.readUtf(MAX_TEXT));
        }
        return List.copyOf(values);
    }

    private static void writeRegions(List<RegionData> regions, FriendlyByteBuf buffer) {
        List<RegionData> safeRegions = regions == null ? List.of() : regions;
        if (safeRegions.size() > MAX_REGIONS) {
            throw new IllegalArgumentException("worldpack regions exceed " + MAX_REGIONS);
        }
        buffer.writeVarInt(safeRegions.size());
        for (RegionData region : safeRegions) {
            write(buffer, region.id(), MAX_TEXT);
            write(buffer, region.display(), MAX_TEXT);
            write(buffer, region.minRealm(), MAX_TEXT);
            buffer.writeDouble(region.auraMultiplier());
            buffer.writeBoolean(region.anchorReady());
            buffer.writeBoolean(region.current());
        }
    }

    private static List<RegionData> readRegions(FriendlyByteBuf buffer) {
        int count = readCount(buffer, MAX_REGIONS, "worldpack region");
        List<RegionData> regions = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            regions.add(new RegionData(
                    buffer.readUtf(MAX_TEXT),
                    buffer.readUtf(MAX_TEXT),
                    buffer.readUtf(MAX_TEXT),
                    buffer.readDouble(),
                    buffer.readBoolean(),
                    buffer.readBoolean()));
        }
        return List.copyOf(regions);
    }

    private static void writeRealms(List<RealmData> realms, FriendlyByteBuf buffer) {
        List<RealmData> safeRealms = realms == null ? List.of() : realms;
        if (safeRealms.size() > MAX_REALMS) {
            throw new IllegalArgumentException("worldpack realms exceed " + MAX_REALMS);
        }
        buffer.writeVarInt(safeRealms.size());
        for (RealmData realm : safeRealms) {
            write(buffer, realm.id(), MAX_TEXT);
            write(buffer, realm.display(), MAX_TEXT);
            write(buffer, realm.regionId(), MAX_TEXT);
            write(buffer, realm.minRealm(), MAX_TEXT);
            write(buffer, realm.ticketDescriptionId(), MAX_KEY);
            buffer.writeVarLong(Math.max(0L, realm.remainingCooldownTicks()));
            buffer.writeBoolean(realm.anchorReady());
            buffer.writeBoolean(realm.currentRegion());
            buffer.writeBoolean(realm.active());
        }
    }

    private static List<RealmData> readRealms(FriendlyByteBuf buffer) {
        int count = readCount(buffer, MAX_REALMS, "worldpack realm");
        List<RealmData> realms = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            realms.add(new RealmData(
                    buffer.readUtf(MAX_TEXT),
                    buffer.readUtf(MAX_TEXT),
                    buffer.readUtf(MAX_TEXT),
                    buffer.readUtf(MAX_TEXT),
                    buffer.readUtf(MAX_KEY),
                    buffer.readVarLong(),
                    buffer.readBoolean(),
                    buffer.readBoolean(),
                    buffer.readBoolean()));
        }
        return List.copyOf(realms);
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

    public record RegionData(String id, String display, String minRealm, double auraMultiplier,
                             boolean anchorReady, boolean current) {}

    public record RealmData(String id, String display, String regionId, String minRealm,
                            String ticketDescriptionId, long remainingCooldownTicks,
                            boolean anchorReady, boolean currentRegion, boolean active) {}
}
