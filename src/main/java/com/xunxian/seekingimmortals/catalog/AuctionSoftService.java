package com.xunxian.seekingimmortals.catalog;

import com.xunxian.seekingimmortals.registry.ModItems;
import com.xunxian.seekingimmortals.worldpack.AuctionHouseSavedData;
import com.xunxian.seekingimmortals.worldpack.ReputationService;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

/**
 * Live auction with shared multiplayer ladder (Wave46) over economy_auction_bands lots.
 * Highest bid is stored in AuctionHouseSavedData; winner settles for the lot reward.
 */
public final class AuctionSoftService {
    private static final String PERSONAL_RAISES = "seeking_immortals_auction_personal_raises";
    private static final String WON_ROOT = "seeking_immortals_auction_won";

    private AuctionSoftService() {}

    public static Snapshot builtin() {
        return AuctionCatalogHolder.SNAPSHOT;
    }

    public record Venue(String id, String region, String faction, String currencyAlt) {}
    public record Lot(String id, String display, long minEquiv, long maxEquiv, String sourceNote) {}

    public record Snapshot(java.util.List<Venue> venues, java.util.List<Lot> lots, double minIncrementPct) {
        public int venueCount() { return venues.size(); }
        public int lotCount() { return lots.size(); }
        public java.util.Optional<Venue> findVenue(String id) {
            return venues.stream().filter(v -> v.id().equals(id)).findFirst();
        }
        public java.util.Optional<Lot> findLot(String id) {
            return lots.stream().filter(l -> l.id().equals(id)).findFirst();
        }
    }

    public static boolean preview(ServerPlayer player, String venueOrLotId) {
        Snapshot snapshot = builtin();
        var venue = snapshot.findVenue(venueOrLotId);
        if (venue.isPresent()) {
            Venue v = venue.get();
            player.displayClientMessage(Component.translatable("message.seeking_immortals.auction.venue",
                    v.id(), v.region(), v.faction()), false);
            player.displayClientMessage(Component.translatable("message.seeking_immortals.auction.live_ready"), false);
            return true;
        }
        var lot = snapshot.findLot(venueOrLotId);
        if (lot.isPresent()) {
            Lot l = lot.get();
            AuctionHouseSavedData data = AuctionHouseSavedData.get(player);
            int current = data.currentAmount(l.id());
            int next = nextBidCost(l, current, snapshot.minIncrementPct());
            String leader = data.getBid(l.id())
                    .map(s -> s.bidder() == null ? "-" : s.bidder().toString().substring(0, 8))
                    .orElse("-");
            player.displayClientMessage(Component.translatable("message.seeking_immortals.auction.lot",
                    l.display(), l.minEquiv(), l.maxEquiv()), false);
            player.displayClientMessage(Component.translatable("message.seeking_immortals.auction.shared_hint",
                    current, next, leader, data.isSettled(l.id()) ? "SETTLED" : "OPEN"), false);
            return true;
        }
        player.displayClientMessage(Component.translatable("message.seeking_immortals.auction.unknown", venueOrLotId), false);
        return false;
    }

    public static boolean bid(ServerPlayer player, String lotId) {
        Snapshot snapshot = builtin();
        var lotOpt = snapshot.findLot(lotId);
        if (lotOpt.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.auction.unknown", lotId), false);
            return false;
        }
        Lot lot = lotOpt.get();
        AuctionHouseSavedData house = AuctionHouseSavedData.get(player);
        if (house.isSettled(lot.id())) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.auction.lot_settled", lot.display()), false);
            return false;
        }

        int current = house.currentAmount(lot.id());
        int next = nextBidCost(lot, current, snapshot.minIncrementPct());
        int delta = Math.max(1, next - Math.max(currentPersonalEscrow(player, lot.id()), 0));
        // When raising over another player, pay full next amount (previous loser keeps paid shards as sunk cost).
        if (current > 0) {
            UUID leader = house.getBid(lot.id()).map(AuctionHouseSavedData.BidState::bidder).orElse(null);
            if (leader != null && !leader.equals(player.getUUID())) {
                delta = next; // outbid pays full new top
            } else if (leader != null && leader.equals(player.getUUID())) {
                delta = Math.max(1, next - current);
            }
        }

        if (!player.getAbilities().instabuild && !consumeShards(player, delta)) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.auction.missing_funds", delta), true);
            return false;
        }

        AuctionHouseSavedData.BidState state = house.placeOrRaise(lot.id(), player.getUUID(), next);
        setPersonalEscrow(player, lot.id(), next);
        AuctionInterestService.markInterest(player, lot.id());
        ReputationService.add(player, "auction_house", 1);
        player.displayClientMessage(Component.translatable("message.seeking_immortals.auction.raised",
                lot.display(), next, state.raises()), true);

        // Shared ladder auto-opens settlement for current leader after 5 total raises.
        if (state.raises() >= 5) {
            return settle(player, lot);
        }
        return true;
    }

    public static boolean settle(ServerPlayer player, String lotId) {
        Snapshot snapshot = builtin();
        var lotOpt = snapshot.findLot(lotId);
        if (lotOpt.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.auction.unknown", lotId), false);
            return false;
        }
        return settle(player, lotOpt.get());
    }

    private static boolean settle(ServerPlayer player, Lot lot) {
        AuctionHouseSavedData house = AuctionHouseSavedData.get(player);
        if (house.isSettled(lot.id())) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.auction.lot_settled", lot.display()), false);
            return false;
        }
        AuctionHouseSavedData.BidState state = house.getBid(lot.id()).orElse(null);
        if (state == null || state.amount() <= 0 || state.bidder() == null) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.auction.no_bid", lot.display()), false);
            return false;
        }
        if (!state.bidder().equals(player.getUUID()) && !player.getAbilities().instabuild) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.auction.not_leader", lot.display()), true);
            return false;
        }

        CompoundTag won = player.getPersistentData().getCompound(WON_ROOT).copy();
        if (won.getBoolean(lot.id())) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.auction.already_won", lot.display()), false);
            return false;
        }

        ItemStack reward = new ItemStack(rewardItemFor(lot), 1);
        if (!player.getInventory().add(reward.copy())) {
            player.drop(reward.copy(), false);
        }
        house.markSettled(lot.id());
        won.putBoolean(lot.id(), true);
        player.getPersistentData().put(WON_ROOT, won);
        ReputationService.add(player, "auction_house", 5);
        player.displayClientMessage(Component.translatable("message.seeking_immortals.auction.won",
                lot.display(), state.amount(), reward.getHoverName()), true);
        return true;
    }

    public static int currentBid(ServerPlayer player, String lotId) {
        return AuctionHouseSavedData.get(player).currentAmount(lotId);
    }

    public static int nextBidCost(Lot lot, int currentBid, double minIncrementPct) {
        int base = baseBidCost(lot);
        if (currentBid <= 0) {
            return base;
        }
        double pct = minIncrementPct <= 0.0D ? 0.05D : minIncrementPct;
        int step = Math.max(1, (int) Math.ceil(currentBid * pct));
        return currentBid + step;
    }

    private static int baseBidCost(Lot lot) {
        long mid = Math.max(lot.minEquiv(), 1L);
        if (lot.maxEquiv() > lot.minEquiv()) {
            mid = (lot.minEquiv() + lot.maxEquiv()) / 2L;
        }
        long scaled = Math.max(8L, Math.min(256L, mid / 5000L));
        return (int) scaled;
    }

    private static int currentPersonalEscrow(ServerPlayer player, String lotId) {
        return player.getPersistentData().getCompound(PERSONAL_RAISES).getInt(lotId == null ? "" : lotId);
    }

    private static void setPersonalEscrow(ServerPlayer player, String lotId, int amount) {
        CompoundTag tag = player.getPersistentData().getCompound(PERSONAL_RAISES).copy();
        tag.putInt(lotId == null ? "" : lotId, amount);
        player.getPersistentData().put(PERSONAL_RAISES, tag);
    }

    private static Item rewardItemFor(Lot lot) {
        String id = (lot.id() + " " + lot.display()).toLowerCase(java.util.Locale.ROOT);
        if (id.contains("jade") || id.contains("immortal")) {
            return ModItems.IMMORTAL_JADE.get();
        }
        if (id.contains("void") || id.contains("crystal") || id.contains("space")) {
            return ModItems.VOID_CRYSTAL.get();
        }
        if (id.contains("scale") || id.contains("beast")) {
            return ModItems.BEAST_CORE.get();
        }
        if (id.contains("pill") || id.contains("dan")) {
            return ModItems.FOUNDATION_BUILDING_PILL_LOW.get();
        }
        return ModItems.SPIRIT_STONE_SHARD.get();
    }

    private static boolean consumeShards(ServerPlayer player, int count) {
        Item shard = ModItems.SPIRIT_STONE_SHARD.get();
        int remaining = count;
        for (int i = 0; i < player.getInventory().getContainerSize() && remaining > 0; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.is(shard)) {
                continue;
            }
            int take = Math.min(remaining, stack.getCount());
            stack.shrink(take);
            remaining -= take;
        }
        return remaining <= 0;
    }

    private static final class AuctionCatalogHolder {
        private static final Snapshot SNAPSHOT = loadBuiltin();

        private static Snapshot loadBuiltin() {
            com.google.gson.JsonObject root = readJson("data/" + com.xunxian.seekingimmortals.SeekingImmortalsMod.MODID + "/text_material/economy_auction_bands.json");
            if (root == null) {
                return new Snapshot(java.util.List.of(), java.util.List.of(), 0.05D);
            }
            double increment = 0.05D;
            if (root.has("bid_rules") && root.get("bid_rules").isJsonObject()) {
                com.google.gson.JsonObject rules = root.getAsJsonObject("bid_rules");
                if (rules.has("min_increment_pct") && rules.get("min_increment_pct").isJsonPrimitive()) {
                    try { increment = rules.get("min_increment_pct").getAsDouble(); } catch (Exception ignored) {}
                }
            }
            java.util.List<Venue> venues = new java.util.ArrayList<>();
            for (com.google.gson.JsonElement element : array(root, "venues")) {
                if (!element.isJsonObject()) continue;
                com.google.gson.JsonObject o = element.getAsJsonObject();
                String id = str(o, "id");
                if (id.isBlank()) continue;
                venues.add(new Venue(id, str(o, "region"), str(o, "faction"), str(o, "currency_alt")));
            }
            java.util.List<Lot> lots = new java.util.ArrayList<>();
            for (com.google.gson.JsonElement element : array(root, "lots")) {
                if (!element.isJsonObject()) continue;
                com.google.gson.JsonObject o = element.getAsJsonObject();
                String id = str(o, "id");
                if (id.isBlank()) continue;
                lots.add(new Lot(id, str(o, "display"), asLong(o, "low_stone_equiv_min"), asLong(o, "low_stone_equiv_max"), str(o, "source_note")));
            }
            return new Snapshot(java.util.Collections.unmodifiableList(venues), java.util.Collections.unmodifiableList(lots), increment);
        }

        private static com.google.gson.JsonObject readJson(String path) {
            try (java.io.InputStream stream = AuctionSoftService.class.getClassLoader().getResourceAsStream(path)) {
                if (stream == null) return null;
                try (java.io.Reader reader = new java.io.InputStreamReader(stream, java.nio.charset.StandardCharsets.UTF_8)) {
                    return com.google.gson.JsonParser.parseReader(reader).getAsJsonObject();
                }
            } catch (Exception ignored) {
                return null;
            }
        }

        private static com.google.gson.JsonArray array(com.google.gson.JsonObject root, String key) {
            if (root == null || !root.has(key) || !root.get(key).isJsonArray()) return new com.google.gson.JsonArray();
            return root.getAsJsonArray(key);
        }

        private static String str(com.google.gson.JsonObject o, String key) {
            if (o == null || !o.has(key) || o.get(key).isJsonNull()) return "";
            try { return o.get(key).getAsString(); } catch (Exception ignored) { return String.valueOf(o.get(key)); }
        }

        private static long asLong(com.google.gson.JsonObject o, String key) {
            if (o == null || !o.has(key) || !o.get(key).isJsonPrimitive()) return 0L;
            try { return o.get(key).getAsLong(); } catch (Exception ignored) { return 0L; }
        }
    }
}
