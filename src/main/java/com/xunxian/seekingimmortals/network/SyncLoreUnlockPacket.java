package com.xunxian.seekingimmortals.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Supplier;

/**
 * M16: sync unlocked beast ids + discovered chronicle ids + timeline phases for read-only encyclopedia screens.
 * Does not write gameplay state; display only.
 */
public record SyncLoreUnlockPacket(
        List<String> bestiaryUnlocked,
        List<String> chronicleDiscovered,
        List<String> timelinePhases,
        String openScreen
) {
    private static final int MAX_IDS = 512;
    private static final int MAX_LEN = 64;
    private static final int MAX_SCREEN = 24;

    public static void send(ServerPlayer player, List<String> bestiary, List<String> chronicle,
                            List<String> timeline, String openScreen) {
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new SyncLoreUnlockPacket(
                        sanitize(bestiary),
                        sanitize(chronicle),
                        sanitize(timeline),
                        openScreen == null ? "" : openScreen));
    }

    public static void encode(SyncLoreUnlockPacket packet, FriendlyByteBuf buffer) {
        writeList(buffer, packet.bestiaryUnlocked());
        writeList(buffer, packet.chronicleDiscovered());
        writeList(buffer, packet.timelinePhases());
        buffer.writeUtf(packet.openScreen() == null ? "" : packet.openScreen(), MAX_SCREEN);
    }

    public static SyncLoreUnlockPacket decode(FriendlyByteBuf buffer) {
        return new SyncLoreUnlockPacket(
                readList(buffer),
                readList(buffer),
                readList(buffer),
                buffer.readUtf(MAX_SCREEN));
    }

    public static void handle(SyncLoreUnlockPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            com.xunxian.seekingimmortals.client.ClientLoreData.set(
                    packet.bestiaryUnlocked(),
                    packet.chronicleDiscovered(),
                    packet.timelinePhases());
            String open = packet.openScreen() == null ? "" : packet.openScreen().trim().toLowerCase(Locale.ROOT);
            if (!open.isBlank()) {
                net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
                if (mc.screen == null
                        || mc.screen instanceof com.xunxian.seekingimmortals.client.BestiaryScreen
                        || mc.screen instanceof com.xunxian.seekingimmortals.client.ChronicleScreen
                        || mc.screen instanceof com.xunxian.seekingimmortals.client.LoreCompendiumScreen) {
                    switch (open) {
                        case "bestiary" -> mc.setScreen(new com.xunxian.seekingimmortals.client.BestiaryScreen());
                        case "chronicle" -> mc.setScreen(new com.xunxian.seekingimmortals.client.ChronicleScreen());
                        case "compendium", "hub", "lore" ->
                                mc.setScreen(new com.xunxian.seekingimmortals.client.LoreCompendiumScreen());
                        case "glossary" ->
                                mc.setScreen(new com.xunxian.seekingimmortals.client.LoreCompendiumScreen(
                                        com.xunxian.seekingimmortals.client.LoreCompendiumScreen.Tab.GLOSSARY));
                        case "numeric" ->
                                mc.setScreen(new com.xunxian.seekingimmortals.client.LoreCompendiumScreen(
                                        com.xunxian.seekingimmortals.client.LoreCompendiumScreen.Tab.NUMERIC));
                        default -> {
                        }
                    }
                } else if (mc.screen instanceof com.xunxian.seekingimmortals.client.BestiaryScreen bestiary) {
                    bestiary.refreshFromSync();
                } else if (mc.screen instanceof com.xunxian.seekingimmortals.client.ChronicleScreen chronicle) {
                    chronicle.refreshFromSync();
                } else if (mc.screen instanceof com.xunxian.seekingimmortals.client.LoreCompendiumScreen lore) {
                    lore.refreshFromSync();
                }
            }
        }));
        context.setPacketHandled(true);
    }

    private static void writeList(FriendlyByteBuf buffer, List<String> values) {
        List<String> list = values == null ? List.of() : values;
        int count = Math.min(MAX_IDS, list.size());
        buffer.writeVarInt(count);
        int written = 0;
        for (String value : list) {
            if (written >= count) {
                break;
            }
            buffer.writeUtf(value == null ? "" : value, MAX_LEN);
            written++;
        }
    }

    private static List<String> readList(FriendlyByteBuf buffer) {
        int count = buffer.readVarInt();
        List<String> values = new ArrayList<>();
        for (int i = 0; i < count && i < MAX_IDS; i++) {
            values.add(buffer.readUtf(MAX_LEN));
        }
        return List.copyOf(values);
    }

    private static List<String> sanitize(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        Set<String> set = new LinkedHashSet<>();
        for (String value : values) {
            if (value == null || value.isBlank()) {
                continue;
            }
            set.add(value.trim().toLowerCase(Locale.ROOT));
            if (set.size() >= MAX_IDS) {
                break;
            }
        }
        return List.copyOf(set);
    }
}
