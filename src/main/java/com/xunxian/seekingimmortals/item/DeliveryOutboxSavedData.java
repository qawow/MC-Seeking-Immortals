package com.xunxian.seekingimmortals.item;

import com.xunxian.seekingimmortals.SeekingImmortalsMod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Persistent undelivered item ledger. Used when inventory cannot accept a reward now
 * and world drops would be lossy across restarts/disconnects.
 */
public final class DeliveryOutboxSavedData extends SavedData {
    private static final String DATA_NAME = SeekingImmortalsMod.MODID + "_delivery_outbox";
    private static final int MAX_ENTRIES_PER_PLAYER = 64;

    private final Map<UUID, List<Entry>> byPlayer = new LinkedHashMap<>();

    public record Entry(String reason, ItemStack stack) {
        public Entry {
            reason = reason == null ? "" : reason;
            stack = stack == null ? ItemStack.EMPTY : stack.copy();
        }
    }

    public static DeliveryOutboxSavedData get(ServerLevel level) {
        return get(level.getServer());
    }

    public static DeliveryOutboxSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                DeliveryOutboxSavedData::load,
                DeliveryOutboxSavedData::new,
                DATA_NAME);
    }

    public static DeliveryOutboxSavedData load(CompoundTag tag) {
        DeliveryOutboxSavedData data = new DeliveryOutboxSavedData();
        ListTag players = tag.getList("Players", Tag.TAG_COMPOUND);
        for (int i = 0; i < players.size(); i++) {
            CompoundTag playerTag = players.getCompound(i);
            if (!playerTag.hasUUID("Player")) {
                continue;
            }
            UUID playerId = playerTag.getUUID("Player");
            ListTag items = playerTag.getList("Items", Tag.TAG_COMPOUND);
            List<Entry> entries = new ArrayList<>();
            for (int j = 0; j < items.size(); j++) {
                CompoundTag itemTag = items.getCompound(j);
                ItemStack stack = ItemStack.of(itemTag.getCompound("Stack"));
                if (stack.isEmpty()) {
                    continue;
                }
                entries.add(new Entry(itemTag.getString("Reason"), stack));
            }
            if (!entries.isEmpty()) {
                data.byPlayer.put(playerId, entries);
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag players = new ListTag();
        for (Map.Entry<UUID, List<Entry>> entry : byPlayer.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null || entry.getValue().isEmpty()) {
                continue;
            }
            CompoundTag playerTag = new CompoundTag();
            playerTag.putUUID("Player", entry.getKey());
            ListTag items = new ListTag();
            for (Entry item : entry.getValue()) {
                if (item.stack().isEmpty()) {
                    continue;
                }
                CompoundTag itemTag = new CompoundTag();
                itemTag.putString("Reason", item.reason());
                itemTag.put("Stack", item.stack().save(new CompoundTag()));
                items.add(itemTag);
            }
            if (items.isEmpty()) {
                continue;
            }
            playerTag.put("Items", items);
            players.add(playerTag);
        }
        tag.put("Players", players);
        return tag;
    }

    public void enqueue(UUID playerId, ItemStack stack, String reason) {
        if (playerId == null || stack == null || stack.isEmpty()) {
            return;
        }
        List<Entry> entries = byPlayer.computeIfAbsent(playerId, ignored -> new ArrayList<>());
        if (entries.size() >= MAX_ENTRIES_PER_PLAYER) {
            entries.remove(0);
        }
        entries.add(new Entry(reason, stack));
        setDirty();
    }

    public int pendingCount(UUID playerId) {
        if (playerId == null) {
            return 0;
        }
        List<Entry> entries = byPlayer.get(playerId);
        return entries == null ? 0 : entries.size();
    }

    /** Claim all pending entries for the player. Caller delivers and may re-enqueue remainders. */
    public List<Entry> claimAll(UUID playerId) {
        if (playerId == null) {
            return List.of();
        }
        List<Entry> entries = byPlayer.remove(playerId);
        if (entries == null || entries.isEmpty()) {
            return List.of();
        }
        setDirty();
        return List.copyOf(entries);
    }

    public static int claimFor(ServerPlayer player) {
        if (player == null || player.getServer() == null) {
            return 0;
        }
        DeliveryOutboxSavedData data = get(player.getServer());
        List<Entry> pending = data.claimAll(player.getUUID());
        int delivered = 0;
        for (Entry entry : pending) {
            if (entry.stack().isEmpty()) {
                continue;
            }
            if (InventoryDeliveryService.canFullyAccept(player, entry.stack())) {
                InventoryDeliveryService.giveOrDrop(player, entry.stack());
                delivered++;
            } else {
                // Inventory still full — keep the remainder durable instead of world-dropping.
                data.enqueue(player.getUUID(), entry.stack(), entry.reason());
            }
        }
        return delivered;
    }
}
