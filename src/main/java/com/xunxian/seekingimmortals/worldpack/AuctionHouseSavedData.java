package com.xunxian.seekingimmortals.worldpack;

import com.xunxian.seekingimmortals.SeekingImmortalsMod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Server-shared auction ladder state (Wave46).
 * Tracks highest bid per lot across players for a lightweight multiplayer auction house.
 */
public class AuctionHouseSavedData extends SavedData {
    private static final String DATA_NAME = SeekingImmortalsMod.MODID + "_auction_house";
    private final Map<String, BidState> highestBids = new HashMap<>();
    /** Wave467: offline outbid refund ledger (player UUID -> pending spirit_stone_shard count). */
    private final Map<UUID, Integer> pendingRefunds = new HashMap<>();

    public static AuctionHouseSavedData get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(
                AuctionHouseSavedData::load,
                AuctionHouseSavedData::new,
                DATA_NAME);
    }

    public static AuctionHouseSavedData get(ServerPlayer player) {
        return get(player.serverLevel());
    }

    public static AuctionHouseSavedData load(CompoundTag tag) {
        AuctionHouseSavedData data = new AuctionHouseSavedData();
        ListTag list = tag.getList("Bids", 10);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag bidTag = list.getCompound(i);
            String lotId = bidTag.getString("LotId");
            if (lotId.isBlank()) continue;
            UUID bidder = bidTag.hasUUID("Bidder") ? bidTag.getUUID("Bidder") : null;
            int amount = Math.max(0, bidTag.getInt("Amount"));
            int raises = Math.max(0, bidTag.getInt("Raises"));
            boolean settled = bidTag.getBoolean("Settled");
            data.highestBids.put(lotId.toLowerCase(Locale.ROOT), new BidState(lotId, bidder, amount, raises, settled));
        }
        ListTag refunds = tag.getList("PendingRefunds", 10);
        for (int i = 0; i < refunds.size(); i++) {
            CompoundTag entry = refunds.getCompound(i);
            if (!entry.hasUUID("Player") || !entry.contains("Amount")) {
                continue;
            }
            UUID playerId = entry.getUUID("Player");
            int amount = Math.max(0, entry.getInt("Amount"));
            if (amount > 0) {
                data.pendingRefunds.put(playerId, amount);
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (BidState state : highestBids.values()) {
            CompoundTag bidTag = new CompoundTag();
            bidTag.putString("LotId", state.lotId());
            if (state.bidder() != null) {
                bidTag.putUUID("Bidder", state.bidder());
            }
            bidTag.putInt("Amount", state.amount());
            bidTag.putInt("Raises", state.raises());
            bidTag.putBoolean("Settled", state.settled());
            list.add(bidTag);
        }
        tag.put("Bids", list);
        ListTag refunds = new ListTag();
        for (Map.Entry<UUID, Integer> entry : pendingRefunds.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null || entry.getValue() <= 0) {
                continue;
            }
            CompoundTag refundTag = new CompoundTag();
            refundTag.putUUID("Player", entry.getKey());
            refundTag.putInt("Amount", entry.getValue());
            refunds.add(refundTag);
        }
        tag.put("PendingRefunds", refunds);
        return tag;
    }

    public void addPendingRefund(UUID playerId, int amount) {
        if (playerId == null || amount <= 0) {
            return;
        }
        pendingRefunds.merge(playerId, amount, Integer::sum);
        setDirty();
    }

    public int takePendingRefund(UUID playerId) {
        if (playerId == null) {
            return 0;
        }
        Integer amount = pendingRefunds.remove(playerId);
        if (amount == null || amount <= 0) {
            return 0;
        }
        setDirty();
        return amount;
    }

    public int peekPendingRefund(UUID playerId) {
        if (playerId == null) {
            return 0;
        }
        return Math.max(0, pendingRefunds.getOrDefault(playerId, 0));
    }

    public Optional<BidState> getBid(String lotId) {
        if (lotId == null || lotId.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(highestBids.get(lotId.trim().toLowerCase(Locale.ROOT)));
    }

    public int currentAmount(String lotId) {
        return getBid(lotId).map(BidState::amount).orElse(0);
    }

    public boolean isSettled(String lotId) {
        return getBid(lotId).map(BidState::settled).orElse(false);
    }

    public BidState placeOrRaise(String lotId, UUID bidder, int newAmount) {
        String id = lotId == null ? "" : lotId.trim().toLowerCase(Locale.ROOT);
        BidState prev = highestBids.get(id);
        int raises = prev == null ? 1 : prev.raises() + 1;
        BidState next = new BidState(id, bidder, Math.max(0, newAmount), raises, prev != null && prev.settled());
        highestBids.put(id, next);
        setDirty();
        return next;
    }

    public BidState markSettled(String lotId) {
        String id = lotId == null ? "" : lotId.trim().toLowerCase(Locale.ROOT);
        BidState prev = highestBids.getOrDefault(id, new BidState(id, null, 0, 0, false));
        BidState next = new BidState(prev.lotId(), prev.bidder(), prev.amount(), prev.raises(), true);
        highestBids.put(id, next);
        setDirty();
        return next;
    }

    public record BidState(String lotId, UUID bidder, int amount, int raises, boolean settled) {}
}
