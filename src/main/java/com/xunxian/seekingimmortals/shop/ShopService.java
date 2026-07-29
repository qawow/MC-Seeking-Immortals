package com.xunxian.seekingimmortals.shop;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.xunxian.seekingimmortals.SeekingImmortalsMod;
import com.xunxian.seekingimmortals.catalog.MarketPriceService;
import com.xunxian.seekingimmortals.catalog.NewGamePlusEconomyService;
import com.xunxian.seekingimmortals.catalog.TradeRouteEconomyService;
import com.xunxian.seekingimmortals.cultivation.CultivationHelper;
import com.xunxian.seekingimmortals.menu.MarketHallMenu;
import com.xunxian.seekingimmortals.menu.MenuAccessContext;
import com.xunxian.seekingimmortals.network.SyncShopDataPacket;
import com.xunxian.seekingimmortals.quest.QuestProgress;
import com.xunxian.seekingimmortals.sect.SectContributionService;
import com.xunxian.seekingimmortals.sect.SectSpecialtyGameplayService;
import com.xunxian.seekingimmortals.util.PlayerDisplayText;
import com.xunxian.seekingimmortals.worldpack.WorldpackGameplayService;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class ShopService {
    public static final String QINGLAN_CONTRIBUTION_HALL = "qinglan_contribution_hall";
    public static final String MARKET_HERBAL_STALL = "market_herbal_stall";
    public static final String TIANNAN_DEMONIC_DUAL_MARKET = "tiannan_demonic_dual_market";
    public static final String CHAOTIC_SEA_ISLAND_GENERAL = "chaotic_sea_island_general";
    public static final String INVERSE_STAR_BLACK_MARKET = "inverse_star_black_market";
    public static final String TIANNAN_REFINEMENT_FORGE = "tiannan_refinement_forge";
    public static final String STAR_PALACE_PATROL_SUPPLY = "star_palace_patrol_supply";
    public static final String QIXUAN_VILLAGE_STALL = "qixuan_village_stall";
    public static final String OUTER_SEA_PUBLIC_STALL = "outer_sea_public_stall";
    public static final String TIANYUAN_MERIT_EXCHANGE = "tianyuan_merit_exchange";
    public static final String TIANNAN_TALISMAN_LANE = "tiannan_talisman_lane";
    public static final String TIANNAN_PUPPET_LANE = "tiannan_puppet_lane";
    public static final String TIANNAN_FORMATION_LANE = "tiannan_formation_lane";
    public static final String NETHER_FERRY_VENDOR = "nether_ferry_vendor";
    public static final String DAJIN_WANBAO_PAVILION = "dajin_wanbao_pavilion";
    public static final String HUANGFENG_CONTRIBUTION_HALL = "huangfeng_contribution_hall";
    public static final String MULAN_FASHI_SUPPLY = "mulan_fashi_supply";
    public static final String QIANZHU_PUPPET_HALL = "qianzhu_puppet_hall";
    public static final String CHAOTIC_SEA_BLACK_MARKET = "chaotic_sea_black_market";
    public static final String CURRENCY_SECT_CONTRIBUTION = "sect_contribution";
    public static final String CURRENCY_ITEM = "item";
    public static final int UNLIMITED_STOCK = -1;
    public static final String ACTION_SYNC = "sync";
    public static final String ACTION_BUY = "buy";
    public static final String RANK_OUTER_DISCIPLE = "outer_disciple";
    public static final String RANK_INNER_DISCIPLE = "inner_disciple";
    public static final String RANK_CORE_DISCIPLE = "core_disciple";
    public static final PurchaseStatus BLOCKED_ITEM_STATUS = PurchaseStatus.BAD_ITEM;

    private static final Map<String, Shop> CACHE = new ConcurrentHashMap<>();
    private static final Map<String, StockState> STOCK_CACHE = new ConcurrentHashMap<>();
    private static final Set<String> CONTRIBUTION_SHOPS = Set.of(
            QINGLAN_CONTRIBUTION_HALL,
            "cangming_isle_contribution_hall",
            "danxia_valley_contribution_hall",
            "lingxiao_sword_sect_contribution_hall",
            "luoyun_contribution_hall",
            "star_palace_merit_hall",
            "wuyue_hall_contribution_hall",
            "yanyue_contribution_hall",
            "yuling_pavilion_contribution_hall");
    private static final List<String> MARKET_SHOPS = loadMarketShopIds();

    private ShopService() {}

    public static void clearRuntimeStockCache() {
        STOCK_CACHE.clear();
    }

    public static Shop getShop(String shopId) {
        return CACHE.computeIfAbsent(shopId, ShopService::loadBuiltinShop);
    }

    public static List<Entry> entries(String shopId) {
        return getShop(shopId).entries();
    }

    public static void openMarket(ServerPlayer player) {
        openMarket(player, MARKET_HERBAL_STALL);
    }

    public static void openMarket(ServerPlayer player, String shopId) {
        openMarket(player, shopId, null);
    }

    public static void openMarket(ServerPlayer player, String shopId, Entity source) {
        String normalizedShop = normalizeShopId(shopId);
        if (!isMarketShop(normalizedShop)) {
            normalizedShop = MARKET_HERBAL_STALL;
        }
        syncMarket(player, normalizedShop, false);
        openMarketHall(player, normalizedShop, source);
    }

    public static void syncMarket(ServerPlayer player, boolean openScreen) {
        syncMarket(player, MARKET_HERBAL_STALL, openScreen);
    }

    public static void syncMarket(ServerPlayer player, String shopId, boolean openScreen) {
        String normalizedShop = normalizeShopId(shopId);
        if (!isMarketShop(normalizedShop)) {
            normalizedShop = MARKET_HERBAL_STALL;
        }
        // Wave490: data sync never opens the legacy ShopScreen; hall MenuType is opened via NetworkHooks.
        MarketSnapshot snapshot = snapshot(player, normalizedShop, WorldpackGameplayService.marketCostModifier(player), false);
        SyncShopDataPacket.send(player, toPacket(snapshot));
        sendMarketListing(player, snapshot);
        if (openScreen) {
            openMarketHall(player, normalizedShop);
        }
    }

    /** Wave490: productized market hall MenuType open path. */
    public static void openMarketHall(ServerPlayer player, String shopId) {
        openMarketHall(player, shopId, null);
    }

    public static void openMarketHall(ServerPlayer player, String shopId, Entity source) {
        if (player == null) {
            return;
        }
        String normalizedShop = normalizeShopId(shopId);
        if (!isMarketShop(normalizedShop)) {
            normalizedShop = MARKET_HERBAL_STALL;
        }
        final String openId = normalizedShop;
        MenuAccessContext access = source == null
                ? MenuAccessContext.atPlayer(player)
                : MenuAccessContext.atEntity(player, source);
        net.minecraftforge.network.NetworkHooks.openScreen(player, new net.minecraft.world.MenuProvider() {
            @Override
            public net.minecraft.network.chat.Component getDisplayName() {
                return net.minecraft.network.chat.Component.translatable("screen.seeking_immortals.shop.market_title");
            }

            @Override
            public net.minecraft.world.inventory.AbstractContainerMenu createMenu(
                    int id, net.minecraft.world.entity.player.Inventory inv, net.minecraft.world.entity.player.Player p) {
                return new MarketHallMenu(id, inv, openId, access);
            }
        }, buf -> {
            buf.writeUtf(openId, 128);
            buf.writeLong(access.token());
        });
    }

    public static void handleClientAction(ServerPlayer player, String action, String shopId, String entryId,
                                          long accessToken) {
        String normalizedAction = action == null ? "" : action.trim().toLowerCase(Locale.ROOT);
        String normalizedShop = normalizeShopId(shopId);
        if (!(player.containerMenu instanceof MarketHallMenu menu)
                || !menu.authorizes(player, normalizedShop, accessToken)) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.menu.invalid_context"), true);
            return;
        }
        if (!isMarketShop(normalizedShop)) {
            player.sendSystemMessage(Component.translatable(
                    "message.seeking_immortals.market.unknown_shop_generic"));
            syncMarket(player, false);
            return;
        }
        if (ACTION_SYNC.equals(normalizedAction)) {
            syncMarket(player, normalizedShop, false);
            return;
        }
        if (!ACTION_BUY.equals(normalizedAction)) {
            player.sendSystemMessage(Component.translatable(
                    "message.seeking_immortals.market.unknown_action_generic"));
            syncMarket(player, normalizedShop, false);
            return;
        }
        PurchaseResult result = buyWithItemCurrency(player, normalizedShop, entryId, WorldpackGameplayService.marketCostModifier(player));
        sendMarketResult(player, result);
        if (result != null && result.success()) {
            com.xunxian.seekingimmortals.worldpack.ReputationService.onShopPurchase(player, normalizedShop);
        }
        syncMarket(player, normalizedShop, false);
    }

    public static boolean isMarketShop(String shopId) {
        return MARKET_SHOPS.contains(normalizeShopId(shopId));
    }

    public static int marketShopCount() {
        return MARKET_SHOPS.size();
    }

    private static List<String> loadMarketShopIds() {
        LinkedHashSet<String> ids = new LinkedHashSet<>();
        // Keep legacy seed order first for stable defaults.
        ids.add(MARKET_HERBAL_STALL);
        ids.add(TIANNAN_DEMONIC_DUAL_MARKET);
        ids.add(CHAOTIC_SEA_ISLAND_GENERAL);
        ids.add(INVERSE_STAR_BLACK_MARKET);
        ids.add(TIANNAN_REFINEMENT_FORGE);
        ids.add(STAR_PALACE_PATROL_SUPPLY);
        ids.add(QIXUAN_VILLAGE_STALL);
        ids.add(OUTER_SEA_PUBLIC_STALL);
        ids.add(TIANYUAN_MERIT_EXCHANGE);
        ids.add(TIANNAN_TALISMAN_LANE);
        ids.add(TIANNAN_PUPPET_LANE);
        ids.add(TIANNAN_FORMATION_LANE);
        ids.add(NETHER_FERRY_VENDOR);
        ids.add(DAJIN_WANBAO_PAVILION);
        ids.add(HUANGFENG_CONTRIBUTION_HALL);
        ids.add(MULAN_FASHI_SUPPLY);
        ids.add(QIANZHU_PUPPET_HALL);
        ids.add(CHAOTIC_SEA_BLACK_MARKET);

        String path = "data/" + SeekingImmortalsMod.MODID + "/catalog/merchant_shops_runtime.json";
        try (InputStream stream = ShopService.class.getClassLoader().getResourceAsStream(path)) {
            if (stream != null) {
                try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                    JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                    JsonArray shops = root.getAsJsonArray("shops");
                    if (shops != null) {
                        for (JsonElement element : shops) {
                            String id = element.getAsString().trim().toLowerCase(Locale.ROOT);
                            if (!id.isBlank() && !CONTRIBUTION_SHOPS.contains(id)) {
                                ids.add(id);
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {
            // Fall back to seed list only.
        }
        return List.copyOf(ids);
    }

    public static String canonicalMarketShopId(String shopId) {
        String normalized = normalizeShopId(shopId);
        return MARKET_SHOPS.contains(normalized) ? normalized : "";
    }

    public static List<String> marketShopIds() {
        return MARKET_SHOPS;
    }

    private static void sendMarketResult(ServerPlayer player, PurchaseResult result) {
        if (result == null || result.status() == null) {
            player.sendSystemMessage(Component.translatable(
                    "message.seeking_immortals.market.purchase_unavailable"));
            return;
        }
        switch (result.status()) {
            case SUCCESS -> player.sendSystemMessage(Component.translatable(
                    "message.seeking_immortals.market.buy_success",
                    result.entry().count(),
                    itemName(result.entry()),
                    result.paidCost(),
                    currencyName(result.entry()),
                    stockText(result.remainingStock())));
            case UNKNOWN_ENTRY -> player.sendSystemMessage(Component.translatable(
                    "message.seeking_immortals.market.unknown_entry_generic"));
            case UNSUPPORTED_CURRENCY -> player.sendSystemMessage(Component.translatable(
                    "message.seeking_immortals.market.unsupported_currency",
                    currencyName(result.entry())));
            case BAD_ITEM -> player.sendSystemMessage(Component.translatable(
                    "message.seeking_immortals.market.bad_shop_item",
                    itemName(result.entry())));
            case BAD_CURRENCY_ITEM -> player.sendSystemMessage(Component.translatable(
                    "message.seeking_immortals.market.bad_currency_item",
                    currencyName(result.entry())));
            case NOT_ENOUGH_CURRENCY -> player.sendSystemMessage(Component.translatable(
                    "message.seeking_immortals.market.not_enough_currency",
                    result.paidCost(),
                    currencyName(result.entry())));
            case RANK_TOO_LOW -> player.sendSystemMessage(Component.translatable(
                    "message.seeking_immortals.sect.rank_too_low",
                    itemName(result.entry()),
                    Component.translatable(rankDescriptionId(result.entry().rankMin()))));
            case REALM_LOCKED -> player.sendSystemMessage(Component.translatable(
                    "message.seeking_immortals.market.realm_locked"));
            case REPUTATION_LOCKED -> player.sendSystemMessage(Component.translatable(
                    "message.seeking_immortals.market.reputation_locked"));
            case ACCESS_DENIED -> player.sendSystemMessage(Component.translatable(
                    "message.seeking_immortals.market.access_denied"));
            case OUT_OF_STOCK -> player.sendSystemMessage(Component.translatable(
                    "message.seeking_immortals.market.out_of_stock",
                    itemName(result.entry())));
        }
    }

    public static MarketSnapshot snapshot(ServerPlayer player, String shopId, CostModifier costModifier, boolean openScreen) {
        Shop shop = getShop(shopId);
        long gameTime = player.serverLevel().getGameTime();
        int sectStage = CultivationHelper.get(player)
                .map(cultivation -> cultivation.getSevenMysteriesQuest().getSectQuestStage())
                .orElse(0);
        List<ShopEntryData> entries = shop.entries().stream()
                .map(entry -> {
                    StockPreview stock = stockPreview(player, shopId, entry, gameTime);
                    boolean blocked = MarketPriceService.isBlockedFromOpenMarket(entry.itemId());
                    boolean locked = blocked
                            || (entry.hasRankRequirement() && !meetsRankRequirement(entry, sectStage))
                            || policyStatus(player, shopId, entry) != PurchaseStatus.SUCCESS;
                    return new ShopEntryData(
                            entry.id(),
                            itemDescriptionId(entry),
                            entry.count(),
                            adjustedCost(player, shop.id(), entry, costModifier),
                            entry.currency(),
                            currencyDescriptionId(entry),
                            stock.remainingStock(),
                            stock.nextRefreshTicks(),
                            entry.rankMin() == null ? "" : entry.rankMin(),
                            locked);
                })
                .toList();
        return new MarketSnapshot(shop.id(), marketTitleKey(shop.id()), entries, openScreen);
    }

    public static PurchaseResult buyWithSectContribution(ServerPlayer player, QuestProgress progress, String shopId, String entryId) {
        // M08 redline: ghost-path shop bans are server-enforced.
        if (com.xunxian.seekingimmortals.sect.GhostSectBanService.isShopDenied(player, shopId)) {
            return new PurchaseResult(PurchaseStatus.RANK_TOO_LOW, null, null, progress.getContribution());
        }
        Optional<Entry> entryOptional = getShop(shopId).find(entryId);
        if (entryOptional.isEmpty()) {
            return new PurchaseResult(PurchaseStatus.UNKNOWN_ENTRY, null, null, progress.getContribution());
        }
        Entry entry = entryOptional.get();
        PurchaseStatus policy = policyStatus(player, shopId, entry);
        if (policy != PurchaseStatus.SUCCESS) {
            return new PurchaseResult(policy, entry, null, progress.getContribution());
        }
        if (!CURRENCY_SECT_CONTRIBUTION.equals(entry.currency())) {
            return new PurchaseResult(PurchaseStatus.UNSUPPORTED_CURRENCY, entry, null, progress.getContribution());
        }
        // Never sell unique redline items through contribution path.
        if (com.xunxian.seekingimmortals.sect.SectContributionShopService.isNeverListItem(entry.itemId())
                || MarketPriceService.isBlockedFromOpenMarket(entry.itemId())) {
            return new PurchaseResult(BLOCKED_ITEM_STATUS, entry, null, progress.getContribution());
        }
        Item item = resolveItem(entry.itemId());
        if (item == null || item == Items.AIR) {
            return new PurchaseResult(PurchaseStatus.BAD_ITEM, entry, null, progress.getContribution());
        }
        int adjustedCost = SectSpecialtyGameplayService.contributionCost(
                progress.getSectId(), shopId, progress.getSectQuestStage(), entry.cost());
        if (progress.getContribution() < adjustedCost) {
            return new PurchaseResult(PurchaseStatus.NOT_ENOUGH_CURRENCY, entry, item,
                    progress.getContribution(), UNLIMITED_STOCK, adjustedCost);
        }
        if (!meetsRankRequirement(entry, progress.getSectQuestStage())) {
            return new PurchaseResult(PurchaseStatus.RANK_TOO_LOW, entry, item, progress.getContribution());
        }
        if (!ShopQuotaService.canBuy(player, shopId, entryId, 5)) {
            return new PurchaseResult(PurchaseStatus.OUT_OF_STOCK, entry, item, progress.getContribution());
        }
        StockReservation stockReservation = reserveStock(player, shopId, entry);
        if (stockReservation.status() != PurchaseStatus.SUCCESS) {
            return new PurchaseResult(stockReservation.status(), entry, item, progress.getContribution(), stockReservation.remainingStock());
        }
        if (!progress.spendContribution(adjustedCost)) {
            releaseStock(player, shopId, entry);
            return new PurchaseResult(PurchaseStatus.NOT_ENOUGH_CURRENCY, entry, item,
                    progress.getContribution(), UNLIMITED_STOCK, adjustedCost);
        }
        giveItem(player, item, entry.count());
        ShopQuotaService.recordBuy(player, shopId, entryId);
        MerchantShopPolicyCatalog.settleRisk(player, shopId, entry.itemId());
        return new PurchaseResult(PurchaseStatus.SUCCESS, entry, item, progress.getContribution(),
                stockReservation.remainingStock(), adjustedCost);
    }

    public static PurchaseResult buyWithItemCurrency(ServerPlayer player, String shopId, String entryId) {
        return buyWithItemCurrency(player, shopId, entryId, CostModifier.NONE);
    }

    public static PurchaseResult buyWithItemCurrency(ServerPlayer player, String shopId, String entryId, CostModifier costModifier) {
        // 与贡献购买路径保持一致：鬼修坊市封禁同样拦截物品货币路径。
        if (com.xunxian.seekingimmortals.sect.GhostSectBanService.isShopDenied(player, shopId)) {
            return new PurchaseResult(PurchaseStatus.ACCESS_DENIED, null, null, -1);
        }
        Optional<Entry> entryOptional = getShop(shopId).find(entryId);
        if (entryOptional.isEmpty()) {
            return new PurchaseResult(PurchaseStatus.UNKNOWN_ENTRY, null, null, -1);
        }
        Entry entry = entryOptional.get();
        PurchaseStatus policy = policyStatus(player, shopId, entry);
        if (policy != PurchaseStatus.SUCCESS) {
            return new PurchaseResult(policy, entry, null, -1);
        }
        if (MarketPriceService.isBlockedFromOpenMarket(entry.itemId())) {
            return new PurchaseResult(BLOCKED_ITEM_STATUS, entry, null, -1);
        }
        int adjustedCost = adjustedCost(player, shopId, entry, costModifier);
        if (!CURRENCY_ITEM.equals(entry.currency())) {
            return new PurchaseResult(PurchaseStatus.UNSUPPORTED_CURRENCY, entry, null, -1);
        }
        Item item = resolveItem(entry.itemId());
        if (item == null || item == Items.AIR) {
            return new PurchaseResult(PurchaseStatus.BAD_ITEM, entry, null, -1);
        }
        Item currencyItem = resolveItem(entry.currencyItemId());
        if (currencyItem == null || currencyItem == Items.AIR) {
            return new PurchaseResult(PurchaseStatus.BAD_CURRENCY_ITEM, entry, item, -1);
        }
        if (!hasItem(player, currencyItem, adjustedCost)) {
            return new PurchaseResult(PurchaseStatus.NOT_ENOUGH_CURRENCY, entry, item, -1, UNLIMITED_STOCK, adjustedCost);
        }
        // 与贡献购买路径保持一致：物品货币路径同样受每日限购约束。
        if (!ShopQuotaService.canBuy(player, shopId, entryId, 5)) {
            return new PurchaseResult(PurchaseStatus.OUT_OF_STOCK, entry, item, -1);
        }
        StockReservation stockReservation = reserveStock(player, shopId, entry);
        if (stockReservation.status() != PurchaseStatus.SUCCESS) {
            return new PurchaseResult(stockReservation.status(), entry, item, -1, stockReservation.remainingStock(), adjustedCost);
        }
        if (!consumeItems(player, currencyItem, adjustedCost)) {
            releaseStock(player, shopId, entry);
            return new PurchaseResult(PurchaseStatus.NOT_ENOUGH_CURRENCY, entry, item, -1, UNLIMITED_STOCK, adjustedCost);
        }
        giveItem(player, item, entry.count());
        ShopQuotaService.recordBuy(player, shopId, entryId);
        MerchantShopPolicyCatalog.settleRisk(player, shopId, entry.itemId());
        return new PurchaseResult(PurchaseStatus.SUCCESS, entry, item, -1, stockReservation.remainingStock(), adjustedCost);
    }

    private static PurchaseStatus policyStatus(ServerPlayer player, String shopId, Entry entry) {
        return switch (MerchantShopPolicyCatalog.evaluate(player, shopId, entry == null ? "" : entry.itemId())) {
            case OPEN -> PurchaseStatus.SUCCESS;
            case REALM_LOCKED -> PurchaseStatus.REALM_LOCKED;
            case REPUTATION_LOCKED -> PurchaseStatus.REPUTATION_LOCKED;
            case ACCESS_DENIED -> PurchaseStatus.ACCESS_DENIED;
        };
    }

    public static Component itemName(Entry entry) {
        if (entry == null) {
            return Component.translatable("text.seeking_immortals.unknown_market_entry");
        }
        Item item = resolveItem(entry.itemId());
        if (item == null || item == Items.AIR) {
            return PlayerDisplayText.itemName(entry.itemId());
        }
        return PlayerDisplayText.itemName(item);
    }

    private static Component currencyName(Entry entry) {
        if (entry == null) {
            return Component.translatable("text.seeking_immortals.unknown_currency");
        }
        if (CURRENCY_SECT_CONTRIBUTION.equals(entry.currency())) {
            return Component.translatable("screen.seeking_immortals.shop.currency.sect_contribution");
        }
        Item item = resolveItem(entry.currencyItemId());
        return item == null || item == Items.AIR
                ? Component.translatable("text.seeking_immortals.unknown_currency")
                : PlayerDisplayText.itemName(item);
    }

    public static String itemDescriptionId(Entry entry) {
        Item item = resolveItem(entry.itemId());
        if (item == null || item == Items.AIR) {
            return "text.seeking_immortals.unknown_item";
        }
        String descriptionId = item.getDescriptionId();
        return PlayerDisplayText.hasTranslation(descriptionId)
                ? descriptionId : "text.seeking_immortals.unknown_item";
    }

    public static String currencyDescriptionId(Entry entry) {
        if (CURRENCY_SECT_CONTRIBUTION.equals(entry.currency())) {
            return "screen.seeking_immortals.shop.currency.sect_contribution";
        }
        Item item = resolveItem(entry.currencyItemId());
        if (item == null || item == Items.AIR) {
            return "text.seeking_immortals.unknown_currency";
        }
        String descriptionId = item.getDescriptionId();
        return PlayerDisplayText.hasTranslation(descriptionId)
                ? descriptionId : "text.seeking_immortals.unknown_currency";
    }

    public static int adjustedCost(String shopId, Entry entry, CostModifier modifier) {
        return adjustedCost(null, shopId, entry, modifier);
    }

    public static int adjustedCost(ServerPlayer player, String shopId, Entry entry, CostModifier modifier) {
        if (entry == null) {
            return 1;
        }
        CostModifier safeModifier = modifier == null ? CostModifier.NONE : modifier;
        String region = TradeRouteEconomyService.shopRegion(shopId).orElse("");
        double routeMod = TradeRouteEconomyService.priceModifier(region, entry.itemId());
        double ngMod = player == null ? 1.0D : NewGamePlusEconomyService.priceModFor(player);
        int economyCost;
        if (usesLowSpiritStone(entry.currencyItemId())) {
            economyCost = MarketPriceService.applyPricing(entry.itemId(), region, entry.cost(), routeMod, ngMod);
        } else {
            economyCost = Math.max(1, (int) Math.round(entry.cost() * routeMod * ngMod));
        }
        return Math.max(1, safeModifier.adjustCost(shopId == null ? "" : shopId, entry, economyCost));
    }

    private static boolean usesLowSpiritStone(String rawId) {
        if (rawId == null || rawId.isBlank()) {
            return false;
        }
        String id = rawId.toLowerCase(Locale.ROOT);
        int colon = id.indexOf(':');
        if (colon >= 0) {
            id = id.substring(colon + 1);
        }
        return id.equals("low_spirit_stone")
                || id.equals("metal_spirit_stone")
                || id.equals("wood_spirit_stone")
                || id.equals("water_spirit_stone")
                || id.equals("fire_element_spirit_stone")
                || id.equals("earth_spirit_stone");
    }

    public static int adjustedCost(Entry entry, CostModifier modifier) {
        return adjustedCost(null, "", entry, modifier);
    }

    public static int adjustedCostForTest(Entry entry, CostModifier modifier) {
        return adjustedCost(entry, modifier);
    }

    /** Region slug for a market/sect shop (merchant_shops corpus). Stable for M06 consumers. */
    public static Optional<String> shopRegionId(String shopId) {
        return TradeRouteEconomyService.shopRegion(shopId);
    }

    public static boolean meetsRankRequirement(Entry entry, int sectStage) {
        return sectStage >= requiredRankStage(entry == null ? "" : entry.rankMin());
    }

    public static String rankDescriptionId(String rankMin) {
        return switch (normalizeRank(rankMin)) {
            case RANK_OUTER_DISCIPLE -> "screen.seeking_immortals.sect.rank.outer";
            case RANK_INNER_DISCIPLE -> "screen.seeking_immortals.sect.rank.inner";
            case RANK_CORE_DISCIPLE -> "screen.seeking_immortals.sect.rank.core";
            default -> "screen.seeking_immortals.sect.stage.locked";
        };
    }

    private static SyncShopDataPacket toPacket(MarketSnapshot snapshot) {
        return new SyncShopDataPacket(
                snapshot.shopId(),
                snapshot.titleKey(),
                snapshot.entries().stream()
                        .map(entry -> new SyncShopDataPacket.EntryData(
                                entry.id(),
                                entry.itemDescriptionId(),
                                entry.count(),
                                entry.cost(),
                                entry.currency(),
                                entry.currencyDescriptionId(),
                                entry.remainingStock(),
                                entry.nextRefreshTicks(),
                                entry.rankMin(),
                                entry.locked()))
                        .toList(),
                snapshot.openScreen());
    }

    public static Shop parseShopForTest(Reader reader) {
        return parse(JsonParser.parseReader(reader).getAsJsonObject(), "test_shop");
    }

    private static Shop loadBuiltinShop(String shopId) {
        String path = "data/" + SeekingImmortalsMod.MODID + "/shops/" + shopId + ".json";
        try (InputStream stream = ShopService.class.getClassLoader().getResourceAsStream(path)) {
            if (stream != null) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                    return parse(JsonParser.parseReader(reader).getAsJsonObject(), shopId);
                }
            }
        } catch (Exception exception) {
            SeekingImmortalsMod.LOGGER.warn("Failed to load shop {} from {}", shopId, path, exception);
        }
        if (QINGLAN_CONTRIBUTION_HALL.equals(shopId)) {
            return qinglanFallback();
        }
        return new Shop(shopId, List.of());
    }

    private static Shop parse(JsonObject root, String fallbackId) {
        String shopId = string(root, "id", fallbackId);
        String defaultCurrency = string(root, "currency", CURRENCY_SECT_CONTRIBUTION);
        String defaultCurrencyItem = string(root, "currency_item", "");
        int defaultStock = stockInt(root, "stock", UNLIMITED_STOCK);
        int defaultRefreshTicks = nonNegativeInt(root, "refresh_ticks", 0);
        String defaultRankMin = optionalRank(root, "rank_min", "");
        JsonArray entries = root.getAsJsonArray("entries");
        List<Entry> parsed = new ArrayList<>();
        if (entries != null) {
            for (JsonElement element : entries) {
                JsonObject object = element.getAsJsonObject();
                String id = string(object, "id", "");
                String itemId = normalizeItemId(string(object, "item", ""));
                int count = positiveInt(object, "count", 1);
                int cost = positiveInt(object, "cost", 1);
                String currency = string(object, "currency", defaultCurrency).toLowerCase(Locale.ROOT);
                String currencyItem = normalizeItemId(string(object, "currency_item", defaultCurrencyItem));
                int stock = stockInt(object, "stock", defaultStock);
                int refreshTicks = nonNegativeInt(object, "refresh_ticks", defaultRefreshTicks);
                String rankMin = optionalRank(object, "rank_min", defaultRankMin);
                if (!id.isBlank() && !itemId.isBlank()) {
                    parsed.add(new Entry(id, itemId, count, cost, currency, currencyItem, stock, refreshTicks, rankMin));
                }
            }
        }
        return new Shop(shopId, List.copyOf(parsed));
    }

    private static Shop qinglanFallback() {
        return new Shop(QINGLAN_CONTRIBUTION_HALL, List.of(
                new Entry("foundation_formula", SeekingImmortalsMod.MODID + ":alchemy_formula_foundation_building_pill_sect", 1, 120, CURRENCY_SECT_CONTRIBUTION, "", UNLIMITED_STOCK, 0),
                new Entry("longevity_formula", SeekingImmortalsMod.MODID + ":alchemy_formula_longevity_pill_sect", 1, 600, CURRENCY_SECT_CONTRIBUTION, "", UNLIMITED_STOCK, 0),
                new Entry("return_yang_true_water_formula", SeekingImmortalsMod.MODID + ":alchemy_formula_return_yang_true_water_sect", 1, 900, CURRENCY_SECT_CONTRIBUTION, "", UNLIMITED_STOCK, 0)
        ));
    }

    private static Item resolveItem(String itemId) {
        ResourceLocation location = ResourceLocation.tryParse(normalizeItemId(itemId));
        return location == null ? null : ForgeRegistries.ITEMS.getValue(location);
    }

    private static String normalizeItemId(String itemId) {
        if (itemId == null || itemId.isBlank()) {
            return "";
        }
        return itemId.indexOf(':') >= 0 ? itemId : SeekingImmortalsMod.MODID + ":" + itemId;
    }

    private static String normalizeShopId(String shopId) {
        return shopId == null || shopId.isBlank()
                ? MARKET_HERBAL_STALL
                : shopId.trim().toLowerCase(Locale.ROOT);
    }

    private static String marketTitleKey(String shopId) {
        return switch (shopId) {
            case TIANNAN_DEMONIC_DUAL_MARKET -> "screen.seeking_immortals.shop.market_title.tiannan_demonic_dual_market";
            case CHAOTIC_SEA_ISLAND_GENERAL -> "screen.seeking_immortals.shop.market_title.chaotic_sea_island_general";
            case INVERSE_STAR_BLACK_MARKET -> "screen.seeking_immortals.shop.market_title.inverse_star_black_market";
            case TIANNAN_REFINEMENT_FORGE -> "screen.seeking_immortals.shop.market_title.tiannan_refinement_forge";
            case STAR_PALACE_PATROL_SUPPLY -> "screen.seeking_immortals.shop.market_title.star_palace_patrol_supply";
            case QIXUAN_VILLAGE_STALL -> "screen.seeking_immortals.shop.market_title.qixuan_village_stall";
            case OUTER_SEA_PUBLIC_STALL -> "screen.seeking_immortals.shop.market_title.outer_sea_public_stall";
            case TIANYUAN_MERIT_EXCHANGE -> "screen.seeking_immortals.shop.market_title.tianyuan_merit_exchange";
            default -> "screen.seeking_immortals.shop.market_title";
        };
    }

    private static String string(JsonObject object, String key, String fallback) {
        return object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsString() : fallback;
    }

    private static int positiveInt(JsonObject object, String key, int fallback) {
        int value = object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsInt() : fallback;
        return Math.max(1, value);
    }

    private static int nonNegativeInt(JsonObject object, String key, int fallback) {
        int value = object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsInt() : fallback;
        return Math.max(0, value);
    }

    private static int stockInt(JsonObject object, String key, int fallback) {
        int value = object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsInt() : fallback;
        return value < 0 ? UNLIMITED_STOCK : value;
    }

    private static String optionalRank(JsonObject object, String key, String fallback) {
        if (!object.has(key)) {
            return normalizeRank(fallback);
        }
        JsonElement value = object.get(key);
        return value == null || value.isJsonNull() ? "" : normalizeRank(value.getAsString());
    }

    private static String normalizeRank(String rankMin) {
        if (rankMin == null || rankMin.isBlank()) {
            return "";
        }
        String value = rankMin.trim().toLowerCase(Locale.ROOT);
        return switch (value) {
            case "none", "null" -> "";
            case "outer", RANK_OUTER_DISCIPLE -> RANK_OUTER_DISCIPLE;
            case "inner", RANK_INNER_DISCIPLE -> RANK_INNER_DISCIPLE;
            case "core", RANK_CORE_DISCIPLE -> RANK_CORE_DISCIPLE;
            default -> value;
        };
    }

    private static int requiredRankStage(String rankMin) {
        return switch (normalizeRank(rankMin)) {
            case RANK_OUTER_DISCIPLE -> SectContributionService.STAGE_OUTER_DISCIPLE;
            case RANK_INNER_DISCIPLE -> SectContributionService.STAGE_INNER_DISCIPLE;
            case RANK_CORE_DISCIPLE -> SectContributionService.STAGE_PHASE10_COMPLETE;
            case "" -> SectContributionService.STAGE_LOCKED;
            default -> Integer.MAX_VALUE;
        };
    }

    private static void giveItem(ServerPlayer player, Item item, int count) {
        int remaining = count;
        int maxStackSize = new ItemStack(item).getMaxStackSize();
        while (remaining > 0) {
            int batch = Math.min(maxStackSize, remaining);
            giveStack(player, new ItemStack(item, batch));
            remaining -= batch;
        }
    }

    private static void giveStack(ServerPlayer player, ItemStack stack) {
        com.xunxian.seekingimmortals.item.InventoryDeliveryService.giveOrEnqueue(
                player, stack, "shop_purchase");
    }

    private static void sendMarketListing(ServerPlayer player, MarketSnapshot snapshot) {
        player.sendSystemMessage(Component.translatable(
                "message.seeking_immortals.market.listing_header",
                Component.translatable(snapshot.titleKey())));
        for (ShopEntryData entry : snapshot.entries()) {
            player.sendSystemMessage(Component.translatable(
                    "message.seeking_immortals.market.listing_entry",
                    Component.translatable(entry.itemDescriptionId()),
                    entry.count(),
                    entry.cost(),
                    Component.translatable(entry.currencyDescriptionId()),
                    stockText(entry.remainingStock())));
        }
    }

    private static boolean hasItem(ServerPlayer player, Item item, int count) {
        int total = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(item)) {
                total += stack.getCount();
                if (total >= count) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean consumeItems(ServerPlayer player, Item item, int count) {
        if (player.getAbilities().instabuild) {
            return true;
        }
        if (!hasItem(player, item, count)) {
            return false;
        }
        int remaining = count;
        for (ItemStack stack : player.getInventory().items) {
            if (!stack.is(item)) {
                continue;
            }
            int remove = Math.min(remaining, stack.getCount());
            stack.shrink(remove);
            remaining -= remove;
            if (remaining <= 0) {
                return true;
            }
        }
        return true;
    }

    private static StockReservation reserveStock(ServerPlayer player, String shopId, Entry entry) {
        if (entry.stock() == UNLIMITED_STOCK) {
            return new StockReservation(PurchaseStatus.SUCCESS, UNLIMITED_STOCK);
        }
        StockContext context = stockContext(player, shopId, entry);
        String key = context.key();
        StockState state = STOCK_CACHE.computeIfAbsent(key,
                ignored -> loadStockState(player.serverLevel(), key, context.maximum()));
        long gameTime = player.serverLevel().getGameTime();
        synchronized (state) {
            boolean refreshed = state.refreshIfNeeded(entry, context.maximum(), gameTime);
            if (state.remaining <= 0) {
                if (refreshed) persistStock(player.serverLevel(), key, state);
                return new StockReservation(PurchaseStatus.OUT_OF_STOCK, state.remaining);
            }
            state.remaining--;
            if (entry.refreshTicks() > 0 && state.nextRefreshGameTime == Long.MAX_VALUE) {
                state.nextRefreshGameTime = gameTime + entry.refreshTicks();
            }
            persistStock(player.serverLevel(), key, state);
            return new StockReservation(PurchaseStatus.SUCCESS, state.remaining);
        }
    }

    private static StockPreview stockPreview(ServerPlayer player, String shopId, Entry entry, long gameTime) {
        if (entry.stock() == UNLIMITED_STOCK) {
            return new StockPreview(UNLIMITED_STOCK, 0L);
        }
        ServerLevel level = player.serverLevel();
        StockContext context = stockContext(player, shopId, entry);
        String key = context.key();
        StockState state = STOCK_CACHE.computeIfAbsent(key,
                ignored -> loadStockState(level, key, context.maximum()));
        synchronized (state) {
            if (state.refreshIfNeeded(entry, context.maximum(), gameTime)) {
                persistStock(level, key, state);
            }
            long nextRefreshTicks = state.nextRefreshGameTime == Long.MAX_VALUE
                    ? 0L
                    : Math.max(0L, state.nextRefreshGameTime - gameTime);
            return new StockPreview(state.remaining, nextRefreshTicks);
        }
    }

    private static StockContext stockContext(ServerPlayer player, String shopId, Entry entry) {
        boolean pearlBoost = entry.itemId().endsWith(":pearl_raw")
                && com.xunxian.seekingimmortals.worldpack.DailyEventEffectExecutor
                .hasActiveToken(player, "shop_pearl_raw_stock");
        return pearlBoost
                ? new StockContext(shopId + "/" + entry.id() + "#daily_pearl", Math.max(1, entry.stock() * 2))
                : new StockContext(shopId + "/" + entry.id(), entry.stock());
    }

    /** 从持久化存储加载库存状态；不存在则按默认库存创建。 */
    private static StockState loadStockState(ServerLevel level, String key, int defaultStock) {
        ShopStockSavedData.StockRecord record = ShopStockSavedData.get(level).get(key);
        if (record == null) {
            return new StockState(defaultStock);
        }
        StockState state = new StockState(record.remaining());
        state.nextRefreshGameTime = record.nextRefreshGameTime();
        return state;
    }

    /** 将库存状态持久化到 overworld SavedData。 */
    private static void persistStock(ServerLevel level, String key, StockState state) {
        ShopStockSavedData.get(level).put(key, state.remaining, state.nextRefreshGameTime);
    }

    private static Component stockText(int stock) {
        return stock == UNLIMITED_STOCK
                ? Component.translatable("command.seeking_immortals.market.stock.unlimited")
                : Component.literal(Integer.toString(Math.max(0, stock)));
    }

    private static void releaseStock(ServerPlayer player, String shopId, Entry entry) {
        if (entry.stock() == UNLIMITED_STOCK) {
            return;
        }
        StockContext context = stockContext(player, shopId, entry);
        StockState state = STOCK_CACHE.get(context.key());
        if (state == null) {
            return;
        }
        synchronized (state) {
            state.remaining = Math.min(context.maximum(), state.remaining + 1);
            persistStock(player.serverLevel(), context.key(), state);
        }
    }

    public record Shop(String id, List<Entry> entries) {
        public Optional<Entry> find(String entryId) {
            return entries.stream().filter(entry -> entry.id().equals(entryId)).findFirst();
        }
    }

    public record MarketSnapshot(String shopId, String titleKey, List<ShopEntryData> entries, boolean openScreen) {}

    public record ShopEntryData(String id, String itemDescriptionId, int count, int cost, String currency,
                                String currencyDescriptionId, int remainingStock, long nextRefreshTicks,
                                String rankMin, boolean locked) {
        public ShopEntryData(String id, String itemDescriptionId, int count, int cost, String currency,
                             String currencyDescriptionId, int remainingStock, long nextRefreshTicks) {
            this(id, itemDescriptionId, count, cost, currency, currencyDescriptionId, remainingStock, nextRefreshTicks, "", false);
        }
    }

    public record Entry(String id, String itemId, int count, int cost, String currency, String currencyItemId,
                        int stock, int refreshTicks, String rankMin) {
        public Entry(String id, String itemId, int count, int cost, String currency, String currencyItemId,
                     int stock, int refreshTicks) {
            this(id, itemId, count, cost, currency, currencyItemId, stock, refreshTicks, "");
        }

        public Entry {
            rankMin = normalizeRank(rankMin);
        }

        public boolean hasLimitedStock() {
            return stock != UNLIMITED_STOCK;
        }

        public boolean hasRankRequirement() {
            return !rankMin.isBlank();
        }
    }

    public record PurchaseResult(PurchaseStatus status, Entry entry, Item item, int remainingContribution, int remainingStock, int paidCost) {
        public PurchaseResult(PurchaseStatus status, Entry entry, Item item, int remainingContribution) {
            this(status, entry, item, remainingContribution, UNLIMITED_STOCK, entry == null ? 0 : entry.cost());
        }

        public PurchaseResult(PurchaseStatus status, Entry entry, Item item, int remainingContribution, int remainingStock) {
            this(status, entry, item, remainingContribution, remainingStock, entry == null ? 0 : entry.cost());
        }

        public boolean success() {
            return status == PurchaseStatus.SUCCESS;
        }
    }

    public static PurchaseResult result(PurchaseStatus status, Entry entry, Item item, int remainingContribution, int remainingStock) {
        return new PurchaseResult(status, entry, item, remainingContribution, remainingStock, entry == null ? 0 : entry.cost());
    }

    public interface CostModifier {
        CostModifier NONE = (shopId, entry, baseCost) -> baseCost;

        int adjustCost(String shopId, Entry entry, int baseCost);
    }

    public enum PurchaseStatus {
        SUCCESS,
        UNKNOWN_ENTRY,
        UNSUPPORTED_CURRENCY,
        BAD_ITEM,
        BAD_CURRENCY_ITEM,
        NOT_ENOUGH_CURRENCY,
        RANK_TOO_LOW,
        REALM_LOCKED,
        REPUTATION_LOCKED,
        ACCESS_DENIED,
        OUT_OF_STOCK
    }

    private record StockReservation(PurchaseStatus status, int remainingStock) {}

    private record StockPreview(int remainingStock, long nextRefreshTicks) {}

    private record StockContext(String key, int maximum) {}

    private static final class StockState {
        private int remaining;
        private long nextRefreshGameTime = Long.MAX_VALUE;

        private StockState(int stock) {
            this.remaining = stock;
        }

        /** 若到达刷新时间则补满库存；返回是否发生了刷新。 */
        private boolean refreshIfNeeded(Entry entry, int maximum, long gameTime) {
            if (entry.refreshTicks() <= 0 || gameTime < nextRefreshGameTime) {
                return false;
            }
            remaining = maximum;
            nextRefreshGameTime = gameTime + entry.refreshTicks();
            return true;
        }
    }
}
