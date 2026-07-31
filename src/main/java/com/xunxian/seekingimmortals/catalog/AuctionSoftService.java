package com.xunxian.seekingimmortals.catalog;

import com.xunxian.seekingimmortals.item.InventoryDeliveryService;
import com.xunxian.seekingimmortals.menu.AuctionHallMenu;
import com.xunxian.seekingimmortals.menu.MenuAccessContext;
import com.xunxian.seekingimmortals.registry.ModItems;
import com.xunxian.seekingimmortals.region.RegionDefinition;
import com.xunxian.seekingimmortals.region.RegionRegistry;
import com.xunxian.seekingimmortals.sect.FactionGraphService;
import com.xunxian.seekingimmortals.sect.ReputationUnlockService;
import com.xunxian.seekingimmortals.util.PlayerDisplayText;
import com.xunxian.seekingimmortals.worldpack.AuctionHouseSavedData;
import com.xunxian.seekingimmortals.worldpack.ReputationService;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.function.ObjIntConsumer;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;

/**
 * Live auction with shared multiplayer ladder over economy_auction_bands lots.
 * Wave464: venue rep gate, deposit floor, id/extras reward mapping.
 */
public final class AuctionSoftService {
    private static final String PERSONAL_RAISES = "seeking_immortals_auction_personal_raises";
    private static final String WON_ROOT = "seeking_immortals_auction_won";
    private static final int DEFAULT_REP_MIN = 0;

    /**
     * Wave492: configurable appraisal gate for bidding.
     * HIGH_TIER = lots whose minEquiv >= HIGH_TIER_MIN_EQUIV require appraisal skill/tool.
     * ALL = every lot requires it. OFF = disabled.
     */
    public enum AppraisalGateMode {
        OFF,
        HIGH_TIER,
        ALL
    }

    public static AppraisalGateMode APPRAISAL_GATE_MODE = AppraisalGateMode.HIGH_TIER;
    public static long HIGH_TIER_MIN_EQUIV = 80L;

    private AuctionSoftService() {}

    public static Snapshot builtin() {
        return AuctionCatalogHolder.SNAPSHOT;
    }

    public static final int PAGE_SIZE = 6;

    /** Wave490: productized auction hall MenuType open path. */
    public static void openHall(ServerPlayer player) {
        openHall(player, 0);
    }

    /** Wave491: open hall and push live ladder page. */
    public static void openHall(ServerPlayer player, int page) {
        openHall(player, page, null);
    }

    public static void openHall(ServerPlayer player, int page, Entity source) {
        if (player == null) {
            return;
        }
        player.displayClientMessage(Component.translatable("message.seeking_immortals.auction.live_ready"), true);
        syncLadder(player, page);
        MenuAccessContext access = source == null
                ? MenuAccessContext.atPlayer(player)
                : MenuAccessContext.atEntity(player, source);
        net.minecraftforge.network.NetworkHooks.openScreen(player, new net.minecraft.world.MenuProvider() {
            @Override
            public Component getDisplayName() {
                return Component.translatable("screen.seeking_immortals.auction.title");
            }

            @Override
            public net.minecraft.world.inventory.AbstractContainerMenu createMenu(
                    int id, net.minecraft.world.entity.player.Inventory inv, net.minecraft.world.entity.player.Player p) {
                return new AuctionHallMenu(id, inv, access);
            }
        }, buf -> buf.writeLong(access.token()));
    }

    /** Wave491: server→client live bid ladder page. */
    public static void syncLadder(ServerPlayer player, int page) {
        if (player == null) {
            return;
        }
        Snapshot snapshot = builtin();
        List<Lot> all = snapshot.lots();
        int total = all.size();
        int safePage = Math.max(0, page);
        int maxPage = total <= 0 ? 0 : (total - 1) / PAGE_SIZE;
        if (safePage > maxPage) {
            safePage = maxPage;
        }
        int from = safePage * PAGE_SIZE;
        int to = Math.min(total, from + PAGE_SIZE);
        AuctionHouseSavedData house = AuctionHouseSavedData.get(player);
        List<com.xunxian.seekingimmortals.network.SyncAuctionLadderPacket.LotBid> bids = new ArrayList<>();
        for (int i = from; i < to; i++) {
            Lot lot = all.get(i);
            int current = house.currentAmount(lot.id());
            int next = nextBidCost(lot, current, snapshot.minIncrementPct(), snapshot.depositFloor());
            String leader = house.getBid(lot.id())
                    .map(state -> {
                        if (state.bidder() == null || player.getServer() == null) {
                            return "-";
                        }
                        ServerPlayer online = player.getServer().getPlayerList().getPlayer(state.bidder());
                        if (online != null) {
                            return online.getGameProfile().getName();
                        }
                        return "未知竞买人";
                    })
                    .orElse("-");
            bids.add(new com.xunxian.seekingimmortals.network.SyncAuctionLadderPacket.LotBid(
                    lot.id(),
                    lotDisplayString(lot),
                    current,
                    next,
                    (int) Math.min(Integer.MAX_VALUE, lot.minEquiv()),
                    (int) Math.min(Integer.MAX_VALUE, lot.maxEquiv()),
                    leader,
                    house.isSettled(lot.id())));
        }
        com.xunxian.seekingimmortals.network.SyncAuctionLadderPacket.send(player,
                new com.xunxian.seekingimmortals.network.SyncAuctionLadderPacket(
                        safePage, PAGE_SIZE, total, bids));
    }

    public record Venue(String id, String region, String faction, String currencyAlt, int repMin) {}

    public record Lot(String id, String display, long minEquiv, long maxEquiv, String sourceNote,
                      String venueId, String rewardItem, List<String> extras) {}

    public static Component playerLotDisplay(Lot lot) {
        return lotDisplay(lot);
    }

    public static Component playerVenueDisplay(Venue venue) {
        return venueDisplay(venue);
    }

    public static Component playerRegionDisplay(String regionId) {
        return regionDisplay(regionId);
    }

    public static Component playerFactionDisplay(String factionId) {
        return factionDisplay(factionId);
    }

    public record Snapshot(List<Venue> venues, List<Lot> lots, double minIncrementPct, int depositFloor) {
        public int venueCount() { return venues.size(); }
        public int lotCount() { return lots.size(); }
        public java.util.Optional<Venue> findVenue(String id) {
            return venues.stream().filter(v -> v.id().equals(id)).findFirst();
        }
        public java.util.Optional<Lot> findLot(String id) {
            return lots.stream().filter(l -> l.id().equals(id)).findFirst();
        }
        public java.util.Optional<Venue> venueForLot(Lot lot) {
            if (lot == null) {
                return java.util.Optional.empty();
            }
            if (lot.venueId() != null && !lot.venueId().isBlank()) {
                return findVenue(lot.venueId());
            }
            // Fallback: single-venue catalogs map all lots to first venue.
            return venues.isEmpty() ? java.util.Optional.empty() : java.util.Optional.of(venues.get(0));
        }
    }

    public static boolean preview(ServerPlayer player, String venueOrLotId) {
        Snapshot snapshot = builtin();
        var venue = snapshot.findVenue(venueOrLotId);
        if (venue.isPresent()) {
            Venue v = venue.get();
            int rep = ReputationService.get(player, v.faction());
            player.displayClientMessage(Component.translatable("message.seeking_immortals.auction.venue",
                    venueDisplay(v), regionDisplay(v.region()), factionDisplay(v.faction())), false);
            player.displayClientMessage(Component.translatable("message.seeking_immortals.auction.rep_gate",
                    v.repMin(), rep), false);
            player.displayClientMessage(Component.translatable("message.seeking_immortals.auction.live_ready"), false);
            return true;
        }
        var lot = snapshot.findLot(venueOrLotId);
        if (lot.isPresent()) {
            Lot l = lot.get();
            AuctionHouseSavedData data = AuctionHouseSavedData.get(player);
            int current = data.currentAmount(l.id());
            int next = nextBidCost(l, current, snapshot.minIncrementPct(), snapshot.depositFloor());
            String leader = data.getBid(l.id())
                    .map(s -> s.bidder() == null ? "-" : "未知竞买人")
                    .orElse("-");
            player.displayClientMessage(Component.translatable("message.seeking_immortals.auction.lot",
                    lotDisplay(l), l.minEquiv(), l.maxEquiv()), false);
            player.displayClientMessage(Component.translatable("message.seeking_immortals.auction.shared_hint",
                    current, next, leader, data.isSettled(l.id()) ? "已结算" : "开放"), false);
            snapshot.venueForLot(l).ifPresent(v -> {
                int rep = ReputationService.get(player, v.faction());
                player.displayClientMessage(Component.translatable("message.seeking_immortals.auction.rep_gate",
                        v.repMin(), rep), false);
            });
            return true;
        }
        player.displayClientMessage(Component.translatable("message.seeking_immortals.auction.unknown",
                Component.literal("未知拍卖目标")), false);
        return false;
    }

    public static boolean bid(ServerPlayer player, String lotId) {
        Snapshot snapshot = builtin();
        var lotOpt = snapshot.findLot(lotId);
        if (lotOpt.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.auction.unknown",
                    Component.literal("未知拍品")), false);
            return false;
        }
        Lot lot = lotOpt.get();
        AuctionHouseSavedData house = AuctionHouseSavedData.get(player);
        if (house.isSettled(lot.id())) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.auction.lot_settled", lotDisplay(lot)), false);
            return false;
        }

        // M05: unique / no_trade items never enter the auction channel.
        if (!MarketPriceService.isAuctionEligible(lot.rewardItem())
                || MarketPriceService.isBlockedFromOpenMarket(lot.rewardItem())
                || MarketPriceService.isBlockedFromOpenMarket(lot.id())) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.auction.unknown",
                    Component.literal("未知拍品")), false);
            return false;
        }
        if (!meetsLotAccess(player, lot)) {
            return false;
        }

        // Wave492: configurable appraisal gate (high-tier / all lots).
        if (!player.getAbilities().instabuild && requiresAppraisal(lot) && !playerMeetsAppraisalGate(player)) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.auction.appraisal_required", lotDisplay(lot)), true);
            return false;
        }

        // A1: venue faction reputation gate.
        var venueOpt = snapshot.venueForLot(lot);
        if (venueOpt.isPresent() && !player.getAbilities().instabuild) {
            Venue venue = venueOpt.get();
            int rep = ReputationService.get(player, venue.faction());
            if (rep < venue.repMin()) {
                player.displayClientMessage(Component.translatable("message.seeking_immortals.auction.rep_too_low",
                        factionDisplay(venue.faction()), venue.repMin(), rep), true);
                return false;
            }
        }

        int current = house.currentAmount(lot.id());
        UUID currentLeader = house.getBid(lot.id()).map(AuctionHouseSavedData.BidState::bidder).orElse(null);
        if (isCurrentLeader(currentLeader, player.getUUID())) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.auction.already_leader", lotDisplay(lot)), true);
            syncLadder(player, 0);
            return false;
        }
        int next = nextBidCost(lot, current, snapshot.minIncrementPct(), snapshot.depositFloor());
        int delta = Math.max(1, next - Math.max(currentPersonalEscrow(player, lot.id()), 0));
        UUID previousLeader = null;
        int previousEscrow = 0;
        if (current > 0) {
            UUID leader = currentLeader;
            if (leader != null && !leader.equals(player.getUUID())) {
                delta = next;
                previousLeader = leader;
                previousEscrow = current;
            }
        }

        if (!player.getAbilities().instabuild && !consumeShards(player, delta)) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.auction.missing_funds", delta), true);
            return false;
        }

        AuctionHouseSavedData.BidState state = house.placeOrRaise(lot.id(), player.getUUID(), next);
        setPersonalEscrow(player, lot.id(), next);
        // Q-B-5: a successful server-authoritative bid records the auction proof with the venue id.
        venueOpt.ifPresent(venue -> com.xunxian.seekingimmortals.quest.DetailedQuestProofService
                .recordAuctionTransaction(player, venue.id()));
        // Wave466/467: refund previous leader's escrow when outbid (online now, offline ledger).
        if (previousLeader != null && previousEscrow > 0 && player.getServer() != null) {
            ServerPlayer previous = player.getServer().getPlayerList().getPlayer(previousLeader);
            if (previous != null) {
                giveShards(previous, previousEscrow);
                setPersonalEscrow(previous, lot.id(), 0);
                previous.displayClientMessage(Component.translatable(
                        "message.seeking_immortals.auction.outbid_refund", lotDisplay(lot), previousEscrow), true);
            } else {
                house.addPendingRefund(previousLeader, previousEscrow);
            }
        }
        AuctionInterestService.markInterest(player, lot.id());
        ReputationService.add(player, "auction_house", 1);
        venueOpt.ifPresent(v -> ReputationService.add(player, v.faction(), 1));
        player.displayClientMessage(Component.translatable("message.seeking_immortals.auction.raised",
                lotDisplay(lot), next, state.raises()), true);

        // Wave466: auto-settle when bid reaches catalog maxEquiv floor.
        if (shouldAutoSettle(state.raises(), lot.maxEquiv(), lot.minEquiv(), next)) {
            boolean settled = settle(player, lot);
            syncLadder(player, 0);
            return settled;
        }
        // Wave491: refresh live ladder for bidder after raise.
        syncLadder(player, 0);
        return true;
    }

    public static boolean settle(ServerPlayer player, String lotId) {
        Snapshot snapshot = builtin();
        var lotOpt = snapshot.findLot(lotId);
        if (lotOpt.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.auction.unknown",
                    Component.literal("未知拍品")), false);
            return false;
        }
        return settle(player, lotOpt.get());
    }

    private static boolean settle(ServerPlayer player, Lot lot) {
        AuctionHouseSavedData house = AuctionHouseSavedData.get(player);
        if (house.isSettled(lot.id())) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.auction.lot_settled", lotDisplay(lot)), false);
            return false;
        }
        AuctionHouseSavedData.BidState state = house.getBid(lot.id()).orElse(null);
        if (state == null || state.amount() <= 0 || state.bidder() == null) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.auction.no_bid", lotDisplay(lot)), false);
            return false;
        }
        if (!state.bidder().equals(player.getUUID()) && !player.getAbilities().instabuild) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.auction.not_leader", lotDisplay(lot)), true);
            return false;
        }

        CompoundTag won = player.getPersistentData().getCompound(WON_ROOT).copy();
        if (won.getBoolean(lot.id())) {
            // Heal house/player ledger drift without re-delivering rewards.
            house.markSettled(lot.id());
            player.displayClientMessage(Component.translatable("message.seeking_immortals.auction.already_won", lotDisplay(lot)), false);
            return false;
        }

        Item rewardItem = rewardItemFor(lot);
        if (rewardItem == null || rewardItem == Items.AIR) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.auction.unknown", lotDisplay(lot)), false);
            return false;
        }

        // Fix TOCTOU idempotency: write persistent ledger FIRST before delivering rewards.
        // Order: 1) markSettled + setDirty (persistent), 2) deliver rewards, 3) player NBT (soft guard).
        // This ensures crash during delivery cannot allow duplicate settlement.
        house.markSettled(lot.id());
        house.setDirty();  // Force immediate write to disk.

        ItemStack reward = new ItemStack(rewardItem, rewardCountFor(lot));
        // Wave492: won auction lots arrive pre-appraised for economy honesty.
        markAppraisedReward(reward, lot);
        String lotKey = lot.id() == null ? "" : lot.id();
        InventoryDeliveryService.giveOrEnqueue(player, reward, "auction_win:" + lotKey);
        // Grant first valid extra if present.
        for (String extra : lot.extras()) {
            Item extraItem = resolveItem(extra);
            if (extraItem != null && extraItem != Items.AIR) {
                ItemStack extraStack = new ItemStack(extraItem, 1);
                InventoryDeliveryService.giveOrEnqueue(player, extraStack, "auction_win_extra:" + lotKey);
                break;
            }
        }

        // Player NBT won flag as soft protection (secondary check, not authoritative).
        won.putBoolean(lot.id(), true);
        player.getPersistentData().put(WON_ROOT, won);
        ReputationService.add(player, "auction_house", 5);
        try {
            com.xunxian.seekingimmortals.phase.SoftPhaseShellService.mark(player, "phase14_tiannan_auction", false);
        } catch (Throwable ignored) {
            // optional
        }
        player.displayClientMessage(Component.translatable("message.seeking_immortals.auction.won",
                lotDisplay(lot), state.amount(), reward.getHoverName()), true);
        // Wave491: refresh live ladder after settle.
        syncLadder(player, 0);
        return true;
    }

    public static int currentBid(ServerPlayer player, String lotId) {
        return AuctionHouseSavedData.get(player).currentAmount(lotId);
    }

    public static boolean requiresAppraisal(Lot lot) {
        if (lot == null || APPRAISAL_GATE_MODE == AppraisalGateMode.OFF) {
            return false;
        }
        if (APPRAISAL_GATE_MODE == AppraisalGateMode.ALL) {
            return true;
        }
        // HIGH_TIER
        return Math.max(lot.minEquiv(), lot.maxEquiv()) >= HIGH_TIER_MIN_EQUIV;
    }

    public static boolean playerMeetsAppraisalGate(ServerPlayer player) {
        if (player == null) {
            return false;
        }
        if (player.getAbilities().instabuild) {
            return true;
        }
        int refineLv = com.xunxian.seekingimmortals.skill.LifeSkillService.level(
                player, com.xunxian.seekingimmortals.skill.SkillType.ARTIFACT_REFINING);
        if (refineLv >= 1) {
            return true;
        }
        // Holding any appraisal/identify tool also qualifies.
        for (ItemStack stack : player.getInventory().items) {
            if (stack.isEmpty()) {
                continue;
            }
            String id = ForgeRegistries.ITEMS.getKey(stack.getItem()) == null
                    ? "" : ForgeRegistries.ITEMS.getKey(stack.getItem()).getPath();
            if (com.xunxian.seekingimmortals.artifact.ArtifactAppraisalService.isAppraisalTool(id)) {
                return true;
            }
            if (stack.getItem() instanceof com.xunxian.seekingimmortals.item.ArtifactCatalogItem catalog
                    && com.xunxian.seekingimmortals.artifact.ArtifactAppraisalService.isAppraisalTool(catalog.artifactId())) {
                return true;
            }
        }
        return false;
    }

    private static void markAppraisedReward(ItemStack reward, Lot lot) {
        if (reward == null || reward.isEmpty()) {
            return;
        }
        var tag = reward.getOrCreateTag();
        tag.putBoolean(com.xunxian.seekingimmortals.artifact.ArtifactAppraisalService.TAG_APPRAISED, true);
        int tier = 1;
        if (lot != null && lot.maxEquiv() >= HIGH_TIER_MIN_EQUIV * 4L) {
            tier = 4;
        } else if (lot != null && lot.maxEquiv() >= HIGH_TIER_MIN_EQUIV * 2L) {
            tier = 3;
        } else if (lot != null && lot.maxEquiv() >= HIGH_TIER_MIN_EQUIV) {
            tier = 2;
        }
        tag.putInt(com.xunxian.seekingimmortals.artifact.ArtifactAppraisalService.TAG_APPRAISED_TIER, tier);
        tag.putString(com.xunxian.seekingimmortals.artifact.ArtifactAppraisalService.TAG_APPRAISED_TYPE, "auction");
        tag.putString(com.xunxian.seekingimmortals.artifact.ArtifactAppraisalService.TAG_APPRAISED_EFFECT, "wanbao_settled");
        tag.putInt(com.xunxian.seekingimmortals.artifact.ArtifactAppraisalService.TAG_APPRAISED_VALUE,
                lot == null ? 10 : (int) Math.min(Integer.MAX_VALUE, Math.max(lot.minEquiv(), lot.maxEquiv() / 10L)));
    }

    public static int nextBidCost(Lot lot, int currentBid, double minIncrementPct) {
        return nextBidCost(lot, currentBid, minIncrementPct, builtin().depositFloor());
    }

    static boolean isCurrentLeader(UUID leader, UUID bidder) {
        return leader != null && bidder != null && leader.equals(bidder);
    }

    static boolean shouldAutoSettle(int raises, long maxEquiv, long minEquiv, int nextBid) {
        long ceiling = Math.max(maxEquiv, minEquiv);
        return raises >= 5 || (ceiling > 0L && nextBid >= Math.max(1L, ceiling / 5000L));
    }

    public static int nextBidCost(Lot lot, int currentBid, double minIncrementPct, int depositFloor) {
        int base = Math.max(baseBidCost(lot), Math.max(1, depositFloor));
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

    private static int rewardCountFor(Lot lot) {
        String id = lot.id() == null ? "" : lot.id().toLowerCase(Locale.ROOT);
        if (id.contains("bundle") || id.contains("pill")) {
            return 3;
        }
        return 1;
    }

    private static Item rewardItemFor(Lot lot) {
        // A5: exact reward_item first. Authored but unresolved ids fail closed.
        String authoredReward = lot.rewardItem();
        if (authoredReward != null && !authoredReward.isBlank()) {
            Item explicit = resolveItem(authoredReward);
            if (explicit != null && explicit != Items.AIR) {
                return explicit;
            }
            return null;
        }
        // id table
        String id = lot.id() == null ? "" : lot.id().toLowerCase(Locale.ROOT);
        if (id.contains("deity_pill") || id.contains("foundation_pill") || id.contains("pill")) {
            return ModItems.FOUNDATION_BUILDING_PILL_LOW.get();
        }
        if (id.contains("beast_scale") || id.contains("beast")) {
            return ModItems.BEAST_CORE.get();
        }
        if (id.contains("ancient_treasure") || id.contains("jade")) {
            return ModItems.IMMORTAL_JADE.get();
        }
        if (id.contains("void") || id.contains("crystal")) {
            return ModItems.VOID_CRYSTAL.get();
        }
        // extras first resolvable item
        for (String extra : lot.extras()) {
            Item item = resolveItem(extra);
            if (item != null && item != Items.AIR) {
                return item;
            }
        }
        // keyword fuzzy last
        String blob = (lot.id() + " " + lot.display() + " " + lot.sourceNote()).toLowerCase(Locale.ROOT);
        if (blob.contains("jade") || blob.contains("immortal")) {
            return ModItems.IMMORTAL_JADE.get();
        }
        if (blob.contains("void") || blob.contains("crystal") || blob.contains("space")) {
            return ModItems.VOID_CRYSTAL.get();
        }
        if (blob.contains("scale") || blob.contains("beast")) {
            return ModItems.BEAST_CORE.get();
        }
        if (blob.contains("pill") || blob.contains("dan")) {
            return ModItems.FOUNDATION_BUILDING_PILL_LOW.get();
        }
        return ModItems.SPIRIT_STONE_SHARD.get();
    }

    private static Item resolveItem(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String id = raw.trim().toLowerCase(Locale.ROOT);
        if (!id.contains(":")) {
            id = "seeking_immortals:" + id;
        }
        ResourceLocation rl = ResourceLocation.tryParse(id);
        if (rl == null) {
            return null;
        }
        Item item = ForgeRegistries.ITEMS.getValue(rl);
        if (item == null || item == Items.AIR) {
            return null;
        }
        return item;
    }

    private static boolean consumeShards(ServerPlayer player, int count) {
        Item shard = ModItems.SPIRIT_STONE_SHARD.get();
        List<ItemStack> stacks = new ArrayList<>(player.getInventory().getContainerSize());
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            stacks.add(player.getInventory().getItem(i));
        }
        return consumeItemStacks(stacks, shard, count);
    }

    static boolean consumeItemStacks(List<ItemStack> stacks, Item item, int count) {
        if (count == 0) {
            return true;
        }
        if (count < 0 || stacks == null || item == null || item == Items.AIR) {
            return false;
        }
        return consumeMatchingEntries(stacks, stack -> stack != null && stack.is(item),
                ItemStack::getCount, ItemStack::shrink, count);
    }

    static <T> boolean consumeMatchingEntries(List<T> entries, Predicate<T> matches,
                                               ToIntFunction<T> countOf, ObjIntConsumer<T> consume,
                                               int requested) {
        if (requested == 0) {
            return true;
        }
        if (requested < 0 || entries == null || matches == null || countOf == null || consume == null) {
            return false;
        }
        long available = 0L;
        List<DebitEntry<T>> debitEntries = new ArrayList<>();
        Set<T> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        for (T entry : entries) {
            if (entry != null && seen.add(entry) && matches.test(entry)) {
                int entryCount = Math.max(0, countOf.applyAsInt(entry));
                if (entryCount > 0) {
                    debitEntries.add(new DebitEntry<>(entry, entryCount));
                    available += entryCount;
                }
            }
        }
        if (available < requested) {
            return false;
        }
        int remaining = requested;
        for (DebitEntry<T> debitEntry : debitEntries) {
            if (remaining <= 0) {
                break;
            }
            int take = Math.min(remaining, debitEntry.available());
            consume.accept(debitEntry.entry(), take);
            remaining -= take;
        }
        return remaining == 0;
    }

    private record DebitEntry<T>(T entry, int available) {}

    private static void giveShards(ServerPlayer player, int count) {
        if (player == null || count <= 0) {
            return;
        }
        ItemStack stack = new ItemStack(ModItems.SPIRIT_STONE_SHARD.get(), count);
        // Prefer durable outbox over world drop when inventory cannot accept the refund.
        InventoryDeliveryService.giveOrEnqueue(player, stack, "auction_refund");
    }

    /** Wave467: claim offline outbid refunds on login. */
    public static int claimPendingRefunds(ServerPlayer player) {
        if (player == null) {
            return 0;
        }
        AuctionHouseSavedData house = AuctionHouseSavedData.get(player);
        int amount = house.peekPendingRefund(player.getUUID());
        if (amount <= 0) {
            return 0;
        }
        // Deliver/enqueue first, then clear the refund ledger so crash cannot swallow the amount.
        giveShards(player, amount);
        int taken = house.takePendingRefund(player.getUUID());
        if (taken > 0) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.auction.pending_refund_claim", taken), true);
        }
        return taken;
    }

    private static boolean meetsLotAccess(ServerPlayer player, Lot lot) {
        String note = lot.sourceNote() == null ? "" : lot.sourceNote();
        String realmId = metadataValue(note, "realm_gate");
        if (!realmId.isBlank() && !player.getAbilities().instabuild) {
            com.xunxian.seekingimmortals.cultivation.Realm required =
                    com.xunxian.seekingimmortals.cultivation.Realm.fromDesignId(realmId);
            var cultivation = com.xunxian.seekingimmortals.cultivation.CultivationHelper.get(player).orElse(null);
            if (required != null && (cultivation == null || cultivation.getRealm().ordinal() < required.ordinal())) {
                player.displayClientMessage(Component.translatable(
                        "message.seeking_immortals.auction.realm_too_low", required.getDisplayName()), true);
                return false;
            }
        }
        if (note.contains("spirit_realm_only=true") && !player.getAbilities().instabuild) {
            String dimension = player.level().dimension().location().toString().toLowerCase(Locale.ROOT);
            boolean inSpiritRealm = dimension.contains("tianyuan") || dimension.contains("spirit_fengyuan")
                    || player.getPersistentData().getBoolean(
                    com.xunxian.seekingimmortals.worldpack.AscensionService.FLAG_ASCENDED);
            if (!inSpiritRealm) {
                player.displayClientMessage(Component.translatable(
                        "message.seeking_immortals.auction.spirit_realm_only"), true);
                return false;
            }
        }
        return true;
    }

    private static Component lotDisplay(Lot lot) {
        return Component.literal(lotDisplayString(lot));
    }

    private static String lotDisplayString(Lot lot) {
        if (lot == null) {
            return "未知拍品";
        }
        if (PlayerDisplayText.isSafe(lot.display())) {
            return lot.display().trim();
        }
        String itemName = itemDisplayString(lot.rewardItem());
        if (!itemName.isBlank()) {
            return itemName;
        }
        for (String extra : lot.extras()) {
            itemName = itemDisplayString(extra);
            if (!itemName.isBlank()) {
                return itemName;
            }
        }
        return switch (normalizeDisplayId(lot.id())) {
            case "lot_deity_pill_bundle" -> "化神修为丹药";
            case "lot_ancient_beast_scale" -> "古兽鳞片";
            case "lot_foundation_pill" -> "筑基丹";
            case "lot_low_ancient_treasure" -> "低阶古宝";
            default -> "未知拍品";
        };
    }

    private static String itemDisplayString(String itemId) {
        Item item = resolveItem(itemId);
        if (item == null || item == Items.AIR) {
            return "";
        }
        String display = PlayerDisplayText.itemName(item).getString();
        return PlayerDisplayText.isSafe(display) ? display.trim() : "";
    }

    private static Component venueDisplay(Venue venue) {
        String id = venue == null ? "" : normalizeDisplayId(venue.id());
        return Component.literal(switch (id) {
            case "wanbao_auction" -> "万宝楼拍卖场";
            case "chaotic_sea_inner" -> "乱星海内海拍卖场";
            case "tianyuan_garrison" -> "天渊城驻地拍卖场";
            default -> "拍卖场";
        });
    }

    private static Component regionDisplay(String regionId) {
        String authored = RegionRegistry.find(regionId).map(RegionDefinition::display).orElse("");
        if (PlayerDisplayText.isSafe(authored)) {
            return Component.literal(authored.trim());
        }
        return Component.literal(switch (normalizeDisplayId(regionId)) {
            case "dajin" -> "大晋";
            case "chaotic_sea" -> "乱星海";
            case "tianyuan", "tianyuan_city" -> "天渊城";
            case "tiannan" -> "天南";
            case "mortal_realm" -> "人界";
            case "spirit_realm" -> "灵界";
            default -> "未知地域";
        });
    }

    private static Component factionDisplay(String factionId) {
        String authored = FactionGraphService.findNode(factionId).map(FactionGraphService.Node::display)
                .or(() -> ReputationUnlockService.find(factionId).map(ReputationUnlockService.FactionUnlocks::display))
                .orElse("");
        if (PlayerDisplayText.isSafe(authored)) {
            return Component.literal(authored.trim());
        }
        return Component.literal(switch (normalizeDisplayId(factionId)) {
            case "wanbao_pavilion", "wanbao" -> "万宝楼";
            case "star_palace", "xinggong" -> "星宫";
            case "inverse_star", "inverse_star_alliance" -> "逆星盟";
            case "tianyuan", "tianyuan_city" -> "天渊城";
            case "auction_house" -> "拍卖行";
            default -> "未知势力";
        });
    }

    private static String normalizeDisplayId(String id) {
        return PlayerDisplayText.normalizeId(id);
    }

    private static String metadataValue(String note, String key) {
        String prefix = key + "=";
        for (String token : note.split(";")) {
            String value = token.trim();
            if (value.startsWith(prefix)) {
                return value.substring(prefix.length()).trim();
            }
        }
        return "";
    }

    private static final class AuctionCatalogHolder {
        private static final Snapshot SNAPSHOT = loadBuiltin();

        private static Snapshot loadBuiltin() {
            com.google.gson.JsonObject root = readJson("data/" + com.xunxian.seekingimmortals.SeekingImmortalsMod.MODID + "/text_material/economy_auction_bands.json");
            if (root == null) {
                return new Snapshot(List.of(), List.of(), 0.05D, 5);
            }
            double increment = 0.05D;
            int depositFloor = 5;
            if (root.has("bid_rules") && root.get("bid_rules").isJsonObject()) {
                com.google.gson.JsonObject rules = root.getAsJsonObject("bid_rules");
                if (rules.has("min_increment_pct") && rules.get("min_increment_pct").isJsonPrimitive()) {
                    try { increment = rules.get("min_increment_pct").getAsDouble(); } catch (Exception ignored) {}
                }
                if (rules.has("deposit_band") && rules.get("deposit_band").isJsonObject()) {
                    com.google.gson.JsonObject deposit = rules.getAsJsonObject("deposit_band");
                    long min = asLong(deposit, "low_stone_min");
                    // Scale deposit stones into shard floor used by gameplay bids.
                    depositFloor = (int) Math.max(5L, Math.min(50L, Math.max(1L, min / 10L)));
                }
            }
            List<Venue> venues = new ArrayList<>();
            for (com.google.gson.JsonElement element : array(root, "venues")) {
                if (!element.isJsonObject()) continue;
                com.google.gson.JsonObject o = element.getAsJsonObject();
                String id = str(o, "id");
                if (id.isBlank()) continue;
                int repMin = o.has("rep_min") && o.get("rep_min").isJsonPrimitive()
                        ? o.get("rep_min").getAsInt() : DEFAULT_REP_MIN;
                venues.add(new Venue(id, str(o, "region"), str(o, "faction"), str(o, "currency_alt"), Math.max(0, repMin)));
            }
            List<Lot> lots = new ArrayList<>();
            java.util.LinkedHashSet<String> seenLots = new java.util.LinkedHashSet<>();
            for (com.google.gson.JsonElement element : array(root, "lots")) {
                if (!element.isJsonObject()) continue;
                com.google.gson.JsonObject o = element.getAsJsonObject();
                String id = str(o, "id");
                if (id.isBlank()) continue;
                List<String> extras = stringList(o.get("extras"));
                String reward = str(o, "reward_item");
                if (MarketPriceService.isBlockedFromOpenMarket(reward) || MarketPriceService.isBlockedFromOpenMarket(id)) {
                    continue;
                }
                lots.add(new Lot(
                        id,
                        str(o, "display"),
                        asLong(o, "low_stone_equiv_min"),
                        asLong(o, "low_stone_equiv_max"),
                        str(o, "source_note"),
                        str(o, "venue_id"),
                        reward,
                        extras));
                seenLots.add(id.toLowerCase(java.util.Locale.ROOT));
            }
            // M05: merge wanbao pavilion / great-jin auction framework pool (artifact detail = M15).
            mergeWanbaoLots(lots, seenLots, venues);
            return new Snapshot(java.util.Collections.unmodifiableList(venues),
                    java.util.Collections.unmodifiableList(lots), increment, depositFloor);
        }

        private static void mergeWanbaoLots(List<Lot> lots, java.util.Set<String> seenLots, List<Venue> venues) {
            com.google.gson.JsonObject wanbao = readJson("data/" + com.xunxian.seekingimmortals.SeekingImmortalsMod.MODID
                    + "/text_material/wanbao_auction_artifacts.json");
            if (wanbao == null) {
                return;
            }
            String defaultVenue = venues.stream()
                    .map(Venue::id)
                    .filter(id -> id != null && id.contains("wanbao"))
                    .findFirst()
                    .orElse(venues.isEmpty() ? "wanbao_auction" : venues.get(0).id());
            for (com.google.gson.JsonElement element : array(wanbao, "wanbao_pavilion_stock")) {
                addWanbaoLot(lots, seenLots, element, defaultVenue, false);
            }
            for (com.google.gson.JsonElement element : array(wanbao, "great_jin_auction_lots")) {
                addWanbaoLot(lots, seenLots, element, defaultVenue, true);
            }
        }

        private static void addWanbaoLot(List<Lot> lots, java.util.Set<String> seenLots,
                                         com.google.gson.JsonElement element, String defaultVenue, boolean auctionLot) {
            if (element == null || !element.isJsonObject()) {
                return;
            }
            com.google.gson.JsonObject o = element.getAsJsonObject();
            String artifactId = str(o, "artifact_id");
            if (artifactId.isBlank()) {
                artifactId = str(o, "id");
            }
            if (artifactId.isBlank()) {
                return;
            }
            String lotId = (auctionLot ? "wanbao_lot_" : "wanbao_stock_") + artifactId;
            String key = lotId.toLowerCase(java.util.Locale.ROOT);
            if (seenLots.contains(key)) {
                return;
            }
            if (MarketPriceService.isBlockedFromOpenMarket(artifactId)) {
                return;
            }
            long[] band = parsePriceBand(str(o, "price_band"), asLong(o, "start_bid_mid_stone"));
            String display = str(o, "display");
            if (display.isBlank()) {
                display = artifactId;
            }
            String note = auctionLot ? "wanbao_great_jin_lot" : "wanbao_pavilion_stock";
            String realm = str(o, "realm_gate");
            if (!realm.isBlank()) {
                note = note + ";realm_gate=" + realm;
            }
            if (asBool(o, "spirit_realm_only")) {
                note = note + ";spirit_realm_only=true";
            }
            lots.add(new Lot(lotId, display, band[0], band[1], note, defaultVenue, artifactId, List.of()));
            seenLots.add(key);
        }

        private static long[] parsePriceBand(String priceBand, long startBidMidStone) {
            String band = priceBand == null ? "" : priceBand.toLowerCase(java.util.Locale.ROOT);
            // mid_stone_X_Y → low-stone using ladder ratio 100
            if (band.startsWith("mid_stone_")) {
                String[] parts = band.substring("mid_stone_".length()).split("_");
                if (parts.length >= 2) {
                    try {
                        long min = Long.parseLong(parts[0]) * 100L;
                        long max = Long.parseLong(parts[1]) * 100L;
                        return new long[]{Math.max(1L, min), Math.max(min, max)};
                    } catch (Exception ignored) {
                    }
                }
            }
            if (band.startsWith("low_stone_")) {
                String[] parts = band.substring("low_stone_".length()).split("_");
                if (parts.length >= 1) {
                    try {
                        long min = Long.parseLong(parts[0]);
                        long max = parts.length >= 2 ? Long.parseLong(parts[1]) : min;
                        return new long[]{Math.max(1L, min), Math.max(min, max)};
                    } catch (Exception ignored) {
                    }
                }
            }
            if (startBidMidStone > 0L) {
                long min = startBidMidStone * 100L;
                return new long[]{min, min * 2L};
            }
            if (band.contains("auction_only")) {
                return new long[]{5000L, 20000L};
            }
            return new long[]{800L, 5000L};
        }

        private static List<String> stringList(com.google.gson.JsonElement element) {
            if (element == null || !element.isJsonArray()) {
                return List.of();
            }
            List<String> list = new ArrayList<>();
            for (com.google.gson.JsonElement child : element.getAsJsonArray()) {
                try {
                    list.add(child.getAsString());
                } catch (Exception ignored) {
                    list.add(String.valueOf(child));
                }
            }
            return List.copyOf(list);
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

        private static boolean asBool(com.google.gson.JsonObject o, String key) {
            if (o == null || !o.has(key) || !o.get(key).isJsonPrimitive()) return false;
            try { return o.get(key).getAsBoolean(); } catch (Exception ignored) { return false; }
        }
    }
}
