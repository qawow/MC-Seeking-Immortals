package com.xunxian.seekingimmortals.catalog;

import com.xunxian.seekingimmortals.catalog.FactionQuestCatalogService.Entry;
import com.xunxian.seekingimmortals.quest.TextQuestChainService;
import com.xunxian.seekingimmortals.worldpack.ReputationService;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Chronicle / trade-route catalog with authority bridges into text quest chains.
 * Wave461: embark fee + start/advance.
 * Wave466: re-discover advance.
 * Wave479: full chronicle mapping coverage, first-discover rewards/rep,
 * merchant-guild fee discount, caravan settle profit on chain complete.
 */
public final class ChronicleTradeSoftService {
    private static final Map<String, String> ROUTE_TO_CHAIN = buildRouteMap();
    private static final Map<String, List<EmbarkFee>> ROUTE_FEES = buildFeeMap();
    private static final Map<String, String> CHRONICLE_TO_CHAIN = buildChronicleMap();
    public static final String DISCOVERED_TAG = "seeking_immortals_chronicle_discovered";
    public static final String SETTLED_TAG = "seeking_immortals_trade_settled";

    public record EmbarkFee(String itemId, int count, String displayKey) {}

    private ChronicleTradeSoftService() {}

    public static int chronicleCount() {
        return FactionQuestCatalogService.builtin().chronicleEvents().size();
    }

    public static int tradeRouteCount() {
        return FactionQuestCatalogService.builtin().tradeRoutes().size();
    }

    public static List<String> sampleChronicle(int limit) {
        return sample(FactionQuestCatalogService.builtin().chronicleEvents(), limit, false);
    }

    public static List<String> sampleTradeRoutes(int limit) {
        return sample(FactionQuestCatalogService.builtin().tradeRoutes(), limit, true);
    }

    public static boolean previewChronicle(ServerPlayer player, String id) {
        Entry entry = FactionQuestCatalogService.builtin().chronicleEvents().get(norm(id));
        if (entry == null) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.chronicle.unknown", id), false);
            return false;
        }
        player.displayClientMessage(Component.translatable("message.seeking_immortals.chronicle.preview",
                entry.id(), entry.display()), false);
        Optional<String> mapped = mappedChronicleChainId(entry.id());
        if (mapped.isPresent()) {
            TextQuestChainService.ChainProgress progress = TextQuestChainService.progressOf(player, mapped.get());
            player.displayClientMessage(Component.translatable("message.seeking_immortals.chronicle.mapped",
                    mapped.get(), progress.stage(), progress.stepCount()), false);
            player.displayClientMessage(Component.translatable("message.seeking_immortals.chronicle.discover_hint"), false);
            if (hasDiscovered(player, entry.id())) {
                player.displayClientMessage(Component.translatable("message.seeking_immortals.chronicle.already_discovered"), false);
            }
        } else {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.chronicle.soft_only"), false);
        }
        return true;
    }

    public static Optional<String> mappedChronicleChainId(String eventId) {
        String id = norm(eventId);
        if (id.isBlank()) {
            return Optional.empty();
        }
        String direct = CHRONICLE_TO_CHAIN.get(id);
        if (direct != null) {
            return firstPresent(direct);
        }
        // Wave479: prefix / keyword coverage for remaining chronicle ids.
        if (id.startsWith("m1") || id.startsWith("m2") || id.startsWith("m3") || id.contains("five_realm")
                || id.contains("true_spirit") || id.contains("xiaolu") || id.contains("mortal_cap")
                || id.startsWith("p") || id.contains("present") || id.contains("player_entry")) {
            return firstPresent("qixuan_mortal_path", "huangfeng_cultivation_path", "spirit_realm_rise");
        }
        if (id.contains("mulan") || id.contains("tianlan") || id.contains("holy_bird") || id.contains("saint_beast")
                || id.startsWith("m4") || id.startsWith("m5") || id.startsWith("k3") || id.startsWith("k9")
                || id.startsWith("e_mulan") || id.startsWith("e_tianlan")) {
            return firstPresent("mulan_tianlan_war", "mulan_war_campaign", "tianlan_defense_line");
        }
        if (id.contains("void") || id.contains("palace") || id.startsWith("a4") || id.startsWith("p6")
                || id.startsWith("e_void") || id.contains("void_realm")) {
            return firstPresent("void_palace_campaign", "chain_void_palace_expedition");
        }
        if (id.contains("demon") || id.contains("fengmo") || id.startsWith("d1") || id.startsWith("d4")
                || id.startsWith("d6") || id.startsWith("d7") || id.startsWith("a6") || id.startsWith("y2")
                || id.startsWith("y3") || id.contains("seal") || id.contains("zhuimo") || id.contains("mojin")
                || id.startsWith("e_ancient") || id.contains("blood_forbidden") || id.startsWith("a5")
                || id.startsWith("cycle_blood")) {
            return firstPresent("ancient_demon_line", "fallen_demon_campaign", "fallen_demon_expedition");
        }
        if (id.contains("yin") || id.contains("nether") || id.startsWith("y1") || id.startsWith("l4")
                || id.contains("minghe")) {
            return firstPresent("yin_luo_ghost_sect", "yin_cluster_pilgrim", "ghost_path");
        }
        if (id.contains("tianyuan") || id.contains("spirit") || id.startsWith("l1") || id.startsWith("l3")
                || id.startsWith("p4") || id.contains("diyuan") || id.contains("ascension")
                || id.contains("guanghai") || id.startsWith("l5") || id.contains("spirit_vein")
                || id.startsWith("d2") || id.startsWith("d3") || id.startsWith("p1") || id.startsWith("p2")
                || id.startsWith("p3")) {
            return firstPresent("spirit_realm_rise", "tianyuan_merit_path", "chain_ascension_spirit_world",
                    "spirit_realm_border");
        }
        if (id.contains("auction") || id.contains("wanbao") || id.contains("dajin") || id.contains("treasure_fair")
                || id.startsWith("l2") || id.startsWith("cycle_treasure") || id.startsWith("a3")
                || id.startsWith("a7") || id.startsWith("k6") || id.startsWith("k7") || id.startsWith("b_")
                || id.contains("buddhist") || id.contains("clan") || id.contains("dayan")
                || id.contains("peiying")) {
            return firstPresent("dajin_wanbao_route", "dajin_kunwu_line", "kunwu_mountain_expedition");
        }
        if (id.contains("star") || id.contains("chaotic") || id.contains("inverse") || id.startsWith("k4")
                || id.startsWith("k5") || id.contains("guanghai")) {
            return firstPresent("chaotic_sea_politics", "star_palace_internal_politics", "inverse_star_smuggle_arc");
        }
        if (id.contains("kunwu") || id.startsWith("a1") || id.startsWith("a2") || id.contains("puppet")
                || id.contains("stone_puppet")) {
            return firstPresent("kunwu_mountain_expedition", "dajin_kunwu_line");
        }
        if (id.contains("sect") || id.startsWith("k1") || id.startsWith("k2") || id.startsWith("k8")
                || id.startsWith("k10") || id.contains("qixuan") || id.contains("luoyun")
                || id.contains("seven_sect") || id.contains("six_demon") || id.startsWith("l6")) {
            return firstPresent("chain_seven_sect_outer_to_inner", "huangfeng_cultivation_path",
                    "qixuan_mortal_path", "demonic_six_path");
        }
        return Optional.empty();
    }

    public static boolean discoverChronicle(ServerPlayer player, String eventId) {
        String key = norm(eventId);
        Entry entry = FactionQuestCatalogService.builtin().chronicleEvents().get(key);
        if (entry == null) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.chronicle.unknown", eventId), false);
            return false;
        }
        Optional<String> mapped = mappedChronicleChainId(key);
        com.xunxian.seekingimmortals.phase.SoftPhaseShellService.mark(player, "chronicle_" + key, false);

        boolean first = !hasDiscovered(player, key);
        if (first) {
            markDiscovered(player, key);
            grantDiscoverReward(player, key);
            applyChronicleReputation(player, key);
            player.displayClientMessage(Component.translatable("message.seeking_immortals.chronicle.first_discover",
                    entry.display()), true);
        }

        if (mapped.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.chronicle.discovered_soft", entry.display()), true);
            return true;
        }
        TextQuestChainService.ChainProgress progress = TextQuestChainService.progressOf(player, mapped.get());
        boolean ok = true;
        if (progress.stage() <= 0) {
            ok = TextQuestChainService.start(player, mapped.get());
        } else if (!progress.complete()) {
            // Wave466: re-discover advances an already-started mapped chain once.
            ok = TextQuestChainService.advance(player, mapped.get());
        } else {
            ReputationService.onQuestComplete(player, mapped.get());
        }
        if (ok) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.chronicle.discovered",
                    entry.display(), mapped.get()), true);
        }
        return ok;
    }

    public static boolean hasDiscovered(ServerPlayer player, String eventId) {
        if (player == null || eventId == null || eventId.isBlank()) {
            return false;
        }
        return player.getPersistentData().getCompound(DISCOVERED_TAG).getBoolean(norm(eventId));
    }

    public static int discoveredCount(ServerPlayer player) {
        if (player == null) {
            return 0;
        }
        CompoundTag tag = player.getPersistentData().getCompound(DISCOVERED_TAG);
        int n = 0;
        for (String key : tag.getAllKeys()) {
            if (tag.getBoolean(key)) {
                n++;
            }
        }
        return n;
    }

    private static void markDiscovered(ServerPlayer player, String eventId) {
        CompoundTag tag = player.getPersistentData().getCompound(DISCOVERED_TAG).copy();
        tag.putBoolean(norm(eventId), true);
        player.getPersistentData().put(DISCOVERED_TAG, tag);
    }

    private static void grantDiscoverReward(ServerPlayer player, String eventId) {
        if (player == null || player.getAbilities().instabuild) {
            return;
        }
        int amount = 1;
        String id = norm(eventId);
        if (id.startsWith("k") || id.startsWith("a") || id.startsWith("d") || id.startsWith("cycle")
                || id.startsWith("e_")) {
            amount = 2;
        }
        if (id.contains("void") || id.contains("demon") || id.contains("war") || id.contains("seal")) {
            amount = Math.max(amount, 3);
        }
        Item shard = TextQuestChainService.resolveItem("seeking_immortals:spirit_stone_shard");
        if (shard == null) {
            return;
        }
        ItemStack stack = new ItemStack(shard, amount);
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
        player.displayClientMessage(Component.translatable("message.seeking_immortals.chronicle.reward",
                amount), false);
    }

    private static void applyChronicleReputation(ServerPlayer player, String eventId) {
        String id = norm(eventId);
        ReputationService.add(player, "mortal_realm", 1);
        if (id.contains("mulan") || id.contains("tianlan") || id.contains("war")) {
            ReputationService.add(player, "mulan", 2);
            ReputationService.add(player, "tianlan", 1);
        } else if (id.contains("demon") || id.contains("yin") || id.contains("nether") || id.contains("ghost")
                || id.contains("seal") || id.startsWith("y")) {
            ReputationService.add(player, "demonic_path", 2);
        } else if (id.contains("star") || id.contains("chaotic") || id.contains("void") || id.contains("inverse")) {
            ReputationService.add(player, "chaotic_sea", 2);
        } else if (id.contains("dajin") || id.contains("kunwu") || id.contains("sect") || id.contains("clan")
                || id.contains("buddhist") || id.contains("auction") || id.contains("treasure")) {
            ReputationService.add(player, "dajin", 2);
        } else if (id.contains("tianyuan") || id.contains("spirit") || id.contains("diyuan")
                || id.contains("ascension")) {
            ReputationService.add(player, "tianyuan", 2);
        }
    }

    public static boolean previewTradeRoute(ServerPlayer player, String id) {
        Entry entry = FactionQuestCatalogService.builtin().tradeRoutes().get(norm(id));
        if (entry == null) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.trade_route.unknown", id), false);
            return false;
        }
        player.displayClientMessage(Component.translatable("message.seeking_immortals.trade_route.preview",
                entry.id(), entry.display()), false);
        Optional<String> mapped = mappedChainId(entry.id());
        List<EmbarkFee> fees = discountedFees(player, feeFor(entry.id()));
        if (!fees.isEmpty()) {
            double mult = ReputationService.shopDiscountMultiplier(player, "merchant_guild");
            if (mult < 1.0D) {
                player.displayClientMessage(Component.translatable("message.seeking_immortals.trade_route.tax_discount",
                        ReputationService.discountLabel(player, "merchant_guild")), false);
            }
            for (EmbarkFee fee : fees) {
                int owned = countOwned(player, fee);
                player.displayClientMessage(Component.translatable("message.seeking_immortals.trade_route.fee",
                        fee.displayKey(), fee.count(), owned), false);
            }
        }
        if (mapped.isPresent()) {
            TextQuestChainService.ChainProgress progress = TextQuestChainService.progressOf(player, mapped.get());
            player.displayClientMessage(Component.translatable("message.seeking_immortals.trade_route.mapped",
                    mapped.get(), progress.stage(), progress.stepCount()), false);
            player.displayClientMessage(Component.translatable("message.seeking_immortals.trade_route.embark_hint"), false);
        } else {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.trade_route.soft_only"), false);
        }
        return true;
    }

    public static Optional<String> mappedChainId(String routeId) {
        String id = norm(routeId);
        if (id.isBlank()) {
            return Optional.empty();
        }
        String direct = ROUTE_TO_CHAIN.get(id);
        if (direct != null) {
            return firstPresent(direct);
        }
        if (TextQuestChainService.find(id).isPresent()) {
            return Optional.of(id);
        }
        if (id.contains("chaotic") || id.contains("sea")) {
            return firstPresent("chaotic_sea_politics", "dajin_wanbao_route");
        }
        if (id.contains("tianyuan") || id.contains("merit") || id.contains("fengyuan")) {
            return firstPresent("tianyuan_merit_path", "spirit_realm_border", "chain_tianyuan_enlist");
        }
        if (id.contains("mulan") || id.contains("smuggle")) {
            return firstPresent("mulan_tianlan_war", "inverse_star_smuggle_arc");
        }
        if (id.contains("nether") || id.contains("yin") || id.contains("ferry")) {
            return firstPresent("yin_luo_ghost_sect", "yin_cluster_pilgrim");
        }
        if (id.contains("dajin") || id.contains("wanbao") || id.contains("barbarian")) {
            return firstPresent("dajin_wanbao_route", "dajin_kunwu_line", "barbarian_kings_line");
        }
        return Optional.empty();
    }

    public static List<EmbarkFee> feeFor(String routeId) {
        return ROUTE_FEES.getOrDefault(norm(routeId), List.of(
                new EmbarkFee("seeking_immortals:spirit_stone_shard", 4, "spirit_stone_shard")));
    }

    /**
     * Wave479: merchant_guild reputation reduces embark fee counts (floor 1 per fee line).
     */
    public static List<EmbarkFee> discountedFees(ServerPlayer player, List<EmbarkFee> base) {
        if (base == null || base.isEmpty()) {
            return List.of();
        }
        double mult = player == null ? 1.0D : ReputationService.shopDiscountMultiplier(player, "merchant_guild");
        if (mult >= 0.999D) {
            return base;
        }
        List<EmbarkFee> out = new ArrayList<>(base.size());
        for (EmbarkFee fee : base) {
            int count = Math.max(1, (int) Math.ceil(fee.count() * mult));
            out.add(new EmbarkFee(fee.itemId(), count, fee.displayKey()));
        }
        return List.copyOf(out);
    }

    /**
     * Pay embark fee then start or advance the mapped text quest chain.
     * Wave479: rep-discounted fees + caravan settle profit when chain completes.
     */
    public static boolean embark(ServerPlayer player, String routeId) {
        String key = norm(routeId);
        Entry entry = FactionQuestCatalogService.builtin().tradeRoutes().get(key);
        if (entry == null) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.trade_route.unknown", routeId), false);
            return false;
        }
        Optional<String> mapped = mappedChainId(key);
        if (mapped.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.trade_route.unmapped", key), false);
            return false;
        }
        TextQuestChainService.ChainProgress progress = TextQuestChainService.progressOf(player, mapped.get());
        if (progress.complete()) {
            // Wave479: allow settle claim if not yet settled.
            if (trySettle(player, key, mapped.get(), entry.display())) {
                return true;
            }
            player.displayClientMessage(Component.translatable("message.seeking_immortals.trade_route.complete",
                    entry.display(), mapped.get()), false);
            return false;
        }
        List<EmbarkFee> fees = discountedFees(player, feeFor(key));
        if (progress.stage() <= 0 && !TextQuestChainService.canStart(player, mapped.get())) {
            return false;
        }
        Optional<TextQuestChainService.StageCost> reservedStageCost =
                TextQuestChainService.nextStageCostFor(player, mapped.get());
        if (!player.getAbilities().instabuild && !payFees(player, fees, reservedStageCost)) {
            return false;
        }
        boolean ok;
        if (progress.stage() <= 0) {
            ok = TextQuestChainService.start(player, mapped.get());
        } else {
            ok = TextQuestChainService.advance(player, mapped.get());
        }
        if (ok) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.trade_route.embarked",
                    entry.display(), mapped.get()), true);
            ReputationService.add(player, "merchant_guild", 1);
            if (key.contains("smuggle") || key.contains("mulan")) {
                ReputationService.add(player, "mulan", -1);
                ReputationService.add(player, "tianlan", -1);
            } else if (key.contains("tianyuan") || key.contains("merit") || key.contains("fengyuan")) {
                ReputationService.add(player, "tianyuan", 1);
            } else if (key.contains("dajin") || key.contains("wanbao") || key.contains("barbarian")) {
                ReputationService.add(player, "dajin", 1);
            } else if (key.contains("chaotic") || key.contains("sea")) {
                ReputationService.add(player, "chaotic_sea", 1);
            }
            TextQuestChainService.ChainProgress after = TextQuestChainService.progressOf(player, mapped.get());
            if (after.complete()) {
                trySettle(player, key, mapped.get(), entry.display());
            }
        }
        return ok;
    }

    /**
     * Wave479: one-time caravan profit when the mapped trade chain is complete.
     */
    public static boolean trySettle(ServerPlayer player, String routeId, String chainId, String routeDisplay) {
        if (player == null) {
            return false;
        }
        String key = norm(routeId);
        if (hasSettled(player, key)) {
            return false;
        }
        TextQuestChainService.ChainProgress progress = TextQuestChainService.progressOf(player, chainId);
        if (!progress.complete()) {
            return false;
        }
        markSettled(player, key);
        int profit = Math.max(2, feeFor(key).stream().mapToInt(EmbarkFee::count).sum());
        // Honored merchants earn more.
        if (ReputationService.get(player, "merchant_guild") >= ReputationService.HONORED_THRESHOLD) {
            profit = (int) Math.ceil(profit * 1.5D);
        }
        Item shard = TextQuestChainService.resolveItem("seeking_immortals:spirit_stone_shard");
        if (shard != null && !player.getAbilities().instabuild) {
            ItemStack stack = new ItemStack(shard, profit);
            if (!player.getInventory().add(stack)) {
                player.drop(stack, false);
            }
        }
        ReputationService.add(player, "merchant_guild", 3);
        ReputationService.onQuestComplete(player, chainId);
        player.displayClientMessage(Component.translatable("message.seeking_immortals.trade_route.settled",
                routeDisplay == null ? key : routeDisplay, profit), true);
        return true;
    }

    public static boolean hasSettled(ServerPlayer player, String routeId) {
        if (player == null || routeId == null || routeId.isBlank()) {
            return false;
        }
        return player.getPersistentData().getCompound(SETTLED_TAG).getBoolean(norm(routeId));
    }

    private static void markSettled(ServerPlayer player, String routeId) {
        CompoundTag tag = player.getPersistentData().getCompound(SETTLED_TAG).copy();
        tag.putBoolean(norm(routeId), true);
        player.getPersistentData().put(SETTLED_TAG, tag);
    }

    private static boolean payFees(ServerPlayer player, List<EmbarkFee> fees,
                                   Optional<TextQuestChainService.StageCost> reservedStageCost) {
        Map<Item, Integer> totalRequired = new LinkedHashMap<>();
        Map<Item, String> displayKeys = new LinkedHashMap<>();
        for (EmbarkFee fee : fees) {
            Item item = TextQuestChainService.resolveItem(fee.itemId());
            if (item == null) {
                player.displayClientMessage(Component.translatable("message.seeking_immortals.trade_route.missing_fee",
                        fee.displayKey(), fee.count(), 0), false);
                return false;
            }
            totalRequired.merge(item, Math.max(0, fee.count()), Integer::sum);
            displayKeys.putIfAbsent(item, fee.displayKey());
        }
        if (reservedStageCost != null && reservedStageCost.isPresent()) {
            TextQuestChainService.StageCost cost = reservedStageCost.get();
            Item item = TextQuestChainService.resolveItem(cost.itemId());
            if (item == null) {
                player.displayClientMessage(Component.translatable("message.seeking_immortals.trade_route.missing_fee",
                        cost.displayKey(), cost.count(), 0), false);
                return false;
            }
            totalRequired.merge(item, Math.max(0, cost.count()), Integer::sum);
            displayKeys.putIfAbsent(item, cost.displayKey());
        }
        for (Map.Entry<Item, Integer> required : totalRequired.entrySet()) {
            int owned = countItem(player, required.getKey());
            if (owned < required.getValue()) {
                player.displayClientMessage(Component.translatable("message.seeking_immortals.trade_route.missing_fee",
                        displayKeys.getOrDefault(required.getKey(), "-"), required.getValue(), owned), false);
                return false;
            }
        }
        for (EmbarkFee fee : fees) {
            Item item = TextQuestChainService.resolveItem(fee.itemId());
            if (item != null) {
                consumeItem(player, item, fee.count());
            }
        }
        return true;
    }

    static int combinedRequiredCount(String itemId, List<EmbarkFee> fees,
                                     Optional<TextQuestChainService.StageCost> stageCost) {
        String wanted = norm(itemId);
        int total = 0;
        if (fees != null) {
            for (EmbarkFee fee : fees) {
                if (fee != null && norm(fee.itemId()).equals(wanted)) {
                    total += Math.max(0, fee.count());
                }
            }
        }
        if (stageCost != null && stageCost.isPresent()
                && norm(stageCost.get().itemId()).equals(wanted)) {
            total += Math.max(0, stageCost.get().count());
        }
        return total;
    }

    private static int countOwned(ServerPlayer player, EmbarkFee fee) {
        Item item = TextQuestChainService.resolveItem(fee.itemId());
        return item == null ? 0 : countItem(player, item);
    }

    private static int countItem(ServerPlayer player, Item item) {
        int total = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(item)) total += stack.getCount();
        }
        for (ItemStack stack : player.getInventory().offhand) {
            if (stack.is(item)) total += stack.getCount();
        }
        return total;
    }

    private static void consumeItem(ServerPlayer player, Item item, int count) {
        int remaining = Math.max(0, count);
        remaining = consumeFrom(player.getInventory().items, item, remaining);
        consumeFrom(player.getInventory().offhand, item, remaining);
        player.containerMenu.broadcastChanges();
    }

    private static int consumeFrom(List<ItemStack> stacks, Item item, int remaining) {
        for (ItemStack stack : stacks) {
            if (remaining <= 0) break;
            if (!stack.is(item)) continue;
            int used = Math.min(remaining, stack.getCount());
            stack.shrink(used);
            remaining -= used;
        }
        return remaining;
    }

    private static List<String> sample(Map<String, Entry> map, int limit, boolean trade) {
        List<String> list = new ArrayList<>();
        int i = 0;
        for (Entry entry : map.values()) {
            if (trade) {
                String chain = mappedChainId(entry.id()).orElse("-");
                list.add(entry.id() + " | " + entry.display() + " -> " + chain);
            } else {
                String chain = mappedChronicleChainId(entry.id()).orElse("-");
                list.add(entry.id() + " | " + entry.display() + " -> " + chain);
            }
            if (++i >= Math.max(1, limit)) break;
        }
        return list;
    }

    private static Optional<String> firstPresent(String... ids) {
        for (String id : ids) {
            if (TextQuestChainService.find(id).isPresent()) {
                return Optional.of(id);
            }
        }
        return Optional.empty();
    }

    private static Map<String, String> buildChronicleMap() {
        Map<String, String> map = new LinkedHashMap<>();
        // Explicit high-value events (Wave479 expanded from 7 → core set; rest via heuristics).
        map.put("mulan_tianlan_war_minor", "mulan_tianlan_war");
        map.put("cycle_void_palace", "void_palace_campaign");
        map.put("cycle_blood_forbidden", "blood_forbidden_campaign");
        map.put("cycle_treasure_fair", "dajin_wanbao_route");
        map.put("d1_demon_invasion", "ancient_demon_line");
        map.put("a4_void_palace_built", "void_palace_campaign");
        map.put("a3_dayan_sage", "dajin_kunwu_line");
        map.put("a1_kunwu_peak", "kunwu_mountain_expedition");
        map.put("a2_stone_puppet", "kunwu_mountain_expedition");
        map.put("a5_blood_forbidden_hermits", "blood_forbidden_campaign");
        map.put("a6_first_demon_war_kunwu", "ancient_demon_line");
        map.put("a7_peiying_formula_peak", "dajin_kunwu_line");
        map.put("m4_holy_bird_mulan", "mulan_tianlan_war");
        map.put("m5_tianlan_oath", "tianlan_defense_line");
        map.put("k3_mulan_tianlan_war_1", "mulan_tianlan_war");
        map.put("k9_mulan_tianlan_escalation", "mulan_war_campaign");
        map.put("k4_star_palace", "star_palace_internal_politics");
        map.put("k5_inverse_star", "inverse_star_smuggle_arc");
        map.put("k1_seven_sects", "chain_seven_sect_outer_to_inner");
        map.put("k2_six_demon_sects", "demonic_six_path");
        map.put("k6_dajin_clans", "dajin_kunwu_line");
        map.put("k8_luoyun_sect", "huangfeng_cultivation_path");
        map.put("k10_qixuan_decline", "qixuan_mortal_path");
        map.put("l1_tianyuan_founded", "tianyuan_merit_path");
        map.put("l2_treasure_fair", "dajin_wanbao_route");
        map.put("l3_diyuan", "spirit_realm_border");
        map.put("l4_minghe", "yin_luo_ghost_sect");
        map.put("p4_tianyuan_open", "tianyuan_merit_path");
        map.put("p6_void_palace_next", "void_palace_campaign");
        map.put("e_mulan_invasion_wave", "mulan_war_campaign");
        map.put("e_tianlan_saint_beast", "tianlan_defense_line");
        map.put("e_void_palace_cycle", "void_palace_campaign");
        map.put("e_ancient_demon_seal_weak", "ancient_demon_line");
        map.put("y1_yinsi_realm", "yin_luo_ghost_sect");
        map.put("y2_ancient_demon_realm", "ancient_demon_line");
        map.put("b_buddhist_dajin_spread", "dajin_kunwu_line");
        return map;
    }

    private static Map<String, String> buildRouteMap() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("tiannan_to_chaotic_sea", "chaotic_sea_politics");
        map.put("route_tiannan_to_chaotic_sea", "chaotic_sea_politics");
        map.put("chaotic_sea_to_dajin", "dajin_wanbao_route");
        map.put("dajin_to_tianyuan", "tianyuan_merit_path");
        map.put("tianyuan_to_spirit_fengyuan", "spirit_realm_border");
        map.put("tiannan_internal", "chain_seven_sect_outer_to_inner");
        map.put("mulan_tianlan_smuggle", "mulan_tianlan_war");
        map.put("route_tiannan_to_mulan_smuggle", "inverse_star_smuggle_arc");
        map.put("nether_river_ferry", "yin_luo_ghost_sect");
        map.put("route_tianyuan_merit_convoy", "chain_tianyuan_enlist");
        map.put("dajin_wanbao_spine", "dajin_wanbao_route");
        map.put("route_dajin_barbarian_caravan", "barbarian_kings_line");
        map.put("route_tiannan_dajin_land", "dajin_kunwu_line");
        return map;
    }

    private static Map<String, List<EmbarkFee>> buildFeeMap() {
        Map<String, List<EmbarkFee>> map = new LinkedHashMap<>();
        map.put("tiannan_to_chaotic_sea", List.of(fee("spirit_stone_shard", 5)));
        map.put("route_tiannan_to_chaotic_sea", List.of(fee("spirit_stone_shard", 5)));
        map.put("chaotic_sea_to_dajin", List.of(fee("spirit_stone_shard", 16)));
        map.put("dajin_to_tianyuan", List.of(
                fee("wind_feather_raft_ticket", 1),
                fee("spirit_stone_shard", 32)));
        map.put("tianyuan_to_spirit_fengyuan", List.of(fee("alliance_merit_token", 2)));
        map.put("tiannan_internal", List.of(fee("spirit_stone_shard", 1)));
        map.put("mulan_tianlan_smuggle", List.of(fee("spirit_stone_shard", 8)));
        map.put("route_tiannan_to_mulan_smuggle", List.of(fee("spirit_stone_shard", 8)));
        map.put("nether_river_ferry", List.of(fee("yin_stone", 30)));
        map.put("route_tianyuan_merit_convoy", List.of(fee("alliance_merit_token", 4)));
        map.put("dajin_wanbao_spine", List.of(fee("immortal_jade", 1)));
        map.put("route_dajin_barbarian_caravan", List.of(fee("spirit_stone_shard", 12)));
        map.put("route_tiannan_dajin_land", List.of(fee("spirit_stone_shard", 8)));
        return map;
    }

    private static EmbarkFee fee(String shortId, int count) {
        return new EmbarkFee("seeking_immortals:" + shortId, count, shortId);
    }

    private static String norm(String id) {
        return id == null ? "" : id.trim().toLowerCase(Locale.ROOT);
    }
}
