package com.xunxian.seekingimmortals.npc;

import com.xunxian.seekingimmortals.catalog.ItemCatalogService;
import com.xunxian.seekingimmortals.cultivation.CultivationHelper;
import com.xunxian.seekingimmortals.cultivation.PlayerCultivation;
import com.xunxian.seekingimmortals.region.RegionRegistry;
import com.xunxian.seekingimmortals.sect.ReputationUnlockService;
import com.xunxian.seekingimmortals.shop.ShopService;
import com.xunxian.seekingimmortals.util.PlayerDisplayText;
import com.xunxian.seekingimmortals.worldpack.DimensionTravelService;
import com.xunxian.seekingimmortals.worldpack.ReputationService;
import com.xunxian.seekingimmortals.worldpack.WorldpackGameplayService;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.BooleanSupplier;

/**
 * Server-side executor for dialogue node embedded actions.
 * Client only sends choice intent; all grants/shops/teleports validate here.
 */
public final class DialogueActionExecutor {
    public static final String OPEN_SHOP = "open_shop";
    public static final String GRANT_ITEM = "grant_item";
    public static final String TELEPORT = "teleport";
    public static final String START_TELEPORT = "start_teleport";
    public static final String START_TRAVEL = "start_travel";
    public static final String ADD_REP = "add_rep";
    public static final String SET_FLAG = "set_flag";
    public static final String UNLOCK = "unlock";
    public static final String TURNIN_QUESTS = "turnin_quests";
    public static final String OFFER_QUEST = "offer_quest";
    public static final String OPEN_QUEST = "open_quest";
    public static final String OPEN_QUEST_BOARD = "open_quest_board";
    public static final String ENTER_INSTANCE = "enter_instance";
    public static final String DENY_SERVICE = "deny_service";
    public static final String END = "end";
    public static final String OPEN_TRAVEL_UI = "open_travel_ui";
    public static final String MARK_STRUCTURE = "mark_structure";
    public static final String HINT = "hint";
    public static final String CALL_GUARD = "call_guard";
    public static final String COMBAT_FLAG = "combat_flag";
    public static final String COMBAT_OR_ARREST = "combat_or_arrest";
    public static final String ADD_SUSPICION = "add_suspicion";
    public static final String ANOMALY_LOG = "anomaly_log";

    private DialogueActionExecutor() {}

    public static boolean executeAll(ServerPlayer player, String npcId, String treeId, String nodeId,
                                     List<DialogueBranchService.Effect> effects) {
        if (player == null || effects == null || effects.isEmpty()) {
            return true;
        }
        for (DialogueBranchService.Effect effect : effects) {
            if (!execute(player, npcId, treeId, nodeId, effect)) {
                return false;
            }
        }
        return true;
    }

    /** Executes node-entry effects while leaving menu-opening effects for an explicit validated choice. */
    public static boolean executeImmediate(ServerPlayer player, String npcId, String treeId, String nodeId,
                                           List<DialogueBranchService.Effect> effects) {
        if (player == null || effects == null || effects.isEmpty()) {
            return true;
        }
        for (DialogueBranchService.Effect effect : effects) {
            if (!isDeferredChoice(effect) && !execute(player, npcId, treeId, nodeId, effect)) {
                return false;
            }
        }
        return true;
    }

    public static boolean isDeferredChoice(DialogueBranchService.Effect effect) {
        return effect != null && OPEN_SHOP.equals(normalize(effect.type()));
    }

    /** Effects that leave or replace the dialogue UI; the session must clear and no new view is sent. */
    public static boolean isTerminalEffect(DialogueBranchService.Effect effect) {
        if (effect == null) {
            return false;
        }
        String type = normalize(effect.type());
        return TELEPORT.equals(type)
                || START_TELEPORT.equals(type)
                || START_TRAVEL.equals(type)
                || OPEN_TRAVEL_UI.equals(type)
                || ENTER_INSTANCE.equals(type)
                || OPEN_SHOP.equals(type)
                || DENY_SERVICE.equals(type)
                || END.equals(type);
    }

    public static boolean hasTerminalEffect(List<DialogueBranchService.Effect> effects) {
        if (effects == null || effects.isEmpty()) {
            return false;
        }
        for (DialogueBranchService.Effect effect : effects) {
            if (isTerminalEffect(effect) && !isDeferredChoice(effect)) {
                return true;
            }
        }
        return false;
    }

    public static boolean execute(ServerPlayer player, String npcId, String treeId, String nodeId,
                                  DialogueBranchService.Effect effect) {
        if (player == null || effect == null) {
            return false;
        }
        String type = normalize(effect.type());
        return switch (type) {
            case OPEN_SHOP -> openShop(player, npcId, effect);
            case GRANT_ITEM -> grantItem(player, effect);
            case TELEPORT, START_TELEPORT, START_TRAVEL, OPEN_TRAVEL_UI -> teleportOrTravel(player, effect);
            case ADD_REP -> addRep(player, npcId, effect);
            case SET_FLAG, UNLOCK -> {
                String flag = firstNonBlank(effect.param("flag"), effect.param("id"), effect.param("token"), type);
                NpcDialogueFlags.setFlag(player, flag);
                yield true;
            }
            case TURNIN_QUESTS, OFFER_QUEST, OPEN_QUEST, OPEN_QUEST_BOARD -> {
                // QuestHookRuntime consumes the post-commit DialogueNodeReachedEvent exactly once.
                NpcDialogueFlags.setFlag(player, "dialogue_" + normalize(nodeId) + "_" + type);
                yield true;
            }
            case ENTER_INSTANCE -> enterInstance(player, effect);
            case DENY_SERVICE, END -> {
                player.displayClientMessage(Component.translatable(
                        "message.seeking_immortals.dialogue.service_denied"), false);
                yield true;
            }
            case MARK_STRUCTURE -> {
                String structure = firstNonBlank(effect.param("structure"), effect.param("id"), "marked_structure");
                NpcDialogueFlags.setFlag(player, "mark_" + normalize(structure));
                player.displayClientMessage(Component.translatable(
                        "message.seeking_immortals.dialogue.location_marked"), false);
                yield true;
            }
            case HINT, ANOMALY_LOG -> {
                player.displayClientMessage(Component.translatable(
                        "message.seeking_immortals.dialogue.ellipsis"), false);
                yield true;
            }
            case CALL_GUARD, COMBAT_FLAG, COMBAT_OR_ARREST, ADD_SUSPICION -> {
                NpcFavorService.add(player, npcId, -5);
                String rep = NamedNpcRegistry.find(npcId)
                        .map(NamedNpcRegistry.NamedNpc::reputationTrack)
                        .orElse("");
                if (!rep.isBlank()) {
                    ReputationService.add(player, ReputationUnlockService.reputationKey(rep), -3);
                }
                player.displayClientMessage(Component.translatable(
                        "message.seeking_immortals.dialogue.tension"), false);
                yield true;
            }
            default -> {
                // Unknown effects are soft-accepted for forward compatibility with M09/M11 consumers.
                NpcDialogueFlags.setFlag(player, "effect_" + type);
                yield true;
            }
        };
    }

    private static boolean openShop(ServerPlayer player, String npcId, DialogueBranchService.Effect effect) {
        String shopId = firstNonBlank(
                effect.param("shop"),
                effect.param("shop_id"),
                NamedNpcRegistry.find(npcId).map(NamedNpcRegistry.NamedNpc::shopId).orElse(""));
        shopId = canonicalizeShop(shopId);
        if (shopId.isBlank()) {
            shopId = ShopService.MARKET_HERBAL_STALL;
        }
        ShopService.openMarket(player, shopId, NpcDialogueApi.currentSourceEntity(player).orElse(null));
        player.displayClientMessage(Component.translatable(
                "message.seeking_immortals.dialogue.shop_opened"), true);
        return true;
    }

    private static String canonicalizeShop(String shopId) {
        String id = normalize(shopId);
        return switch (id) {
            case "blood_forbidden_quota" -> ShopService.HUANGFENG_CONTRIBUTION_HALL;
            case "star_registration" -> ShopService.STAR_PALACE_PATROL_SUPPLY;
            case "inverse_black" -> ShopService.INVERSE_STAR_BLACK_MARKET;
            case "market_stall" -> ShopService.MARKET_HERBAL_STALL;
            case "reincarnation_desk" -> ShopService.NETHER_FERRY_VENDOR;
            default -> id;
        };
    }

    private static boolean grantItem(ServerPlayer player, DialogueBranchService.Effect effect) {
        String itemId = firstNonBlank(effect.param("item"), effect.param("item_id"));
        int count = Math.max(1, effect.paramInt("count", 1));
        if (itemId.isBlank()) {
            return false;
        }
        // Prefer reward-service mapping (handles story tokens + catalog ids).
        boolean ok = NamedNpcRewardService.grantCatalogItem(player, itemId, count);
        if (!ok) {
            // Try direct catalog resolve.
            Item item = ItemCatalogService.resolveCatalogItem(itemId);
            if (item != null) {
                ItemStack stack = new ItemStack(item, Math.min(64, count));
                com.xunxian.seekingimmortals.item.InventoryDeliveryService.giveOrEnqueue(
                        player, stack, "dialogue_reward");
                ok = true;
            }
        }
        if (ok) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.dialogue.item_granted",
                    PlayerDisplayText.itemName(itemId), count), false);
        } else {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.dialogue.item_unavailable"), false);
        }
        return ok;
    }

    private static boolean teleportOrTravel(ServerPlayer player, DialogueBranchService.Effect effect) {
        String dest = firstNonBlank(effect.param("to"), effect.param("region"), effect.param("route"), effect.param("target"));
        int cost = effect.paramInt("cost_contribution", 0);
        if (dest.isBlank()) {
            return executeContributionTravel(player, cost, () -> WorldpackGameplayService.usePortalArray(player));
        }
        String region = normalize(dest);
        Optional<DimensionTravelService.RouteDef> route = DimensionTravelService.findRouteForTravel(
                player.level().dimension().location().toString(),
                region,
                cost > 0 ? DimensionTravelService.METHOD_REGULATED : "");
        if (route.isPresent()) {
            int configuredCost = DimensionTravelService.contributionCost(route.get().id());
            if (cost > 0 && configuredCost != cost) {
                player.displayClientMessage(Component.literal("[对话] 通行费用配置不一致，传送已取消。"), false);
                return false;
            }
            boolean success = DimensionTravelService.travelByRoute(player, route.get().id());
            if (success) {
                NpcDialogueFlags.setFlag(player, "travel_to_" + region);
                ReputationService.onPortalTravel(player, region);
            }
            return success;
        }
        if (RegionRegistry.isKnown(region)) {
            boolean success = executeContributionTravel(player, cost,
                    () -> WorldpackGameplayService.travel(player, region));
            if (success) {
                NpcDialogueFlags.setFlag(player, "travel_to_" + region);
            }
            return success;
        }
        NpcDialogueFlags.setFlag(player, "travel_route_" + region);
        player.displayClientMessage(Component.translatable(
                "message.seeking_immortals.dialogue.travel_prepared"), false);
        return true;
    }

    private static boolean executeContributionTravel(ServerPlayer player, int cost, BooleanSupplier travel) {
        Optional<PlayerCultivation> cultivation = CultivationHelper.get(player);
        var progress = cultivation.map(PlayerCultivation::getSevenMysteriesQuest).orElse(null);
        int current = progress == null ? 0 : progress.getContribution();
        if (cost > 0 && (progress == null || current < cost || !progress.spendContribution(cost))) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.sect.not_enough_contribution", cost, current), false);
            return false;
        }
        boolean success;
        try {
            success = travel != null && travel.getAsBoolean();
        } catch (Throwable ignored) {
            success = false;
        }
        if (!success && cost > 0 && progress != null) {
            progress.addContribution(cost);
        }
        return success;
    }

    private static boolean enterInstance(ServerPlayer player, DialogueBranchService.Effect effect) {
        String realmId = firstNonBlank(effect.param("instance"), effect.param("realm"), effect.param("id"), "blood_forbidden");
        return WorldpackGameplayService.enterSecretRealm(player, realmId);
    }

    private static boolean addRep(ServerPlayer player, String npcId, DialogueBranchService.Effect effect) {
        String rep = firstNonBlank(effect.param("rep"), effect.param("faction"),
                NamedNpcRegistry.find(npcId).map(NamedNpcRegistry.NamedNpc::reputationTrack).orElse(""));
        int delta = effect.paramInt("delta", 1);
        if (rep.isBlank()) {
            NpcFavorService.add(player, npcId, delta);
            return true;
        }
        String key = ReputationUnlockService.reputationKey(rep);
        if (key == null || key.isBlank()) {
            key = normalize(rep).replaceFirst("^rep_", "");
        }
        ReputationService.add(player, key, delta);
        NpcFavorService.add(player, npcId, Math.max(1, delta / 2));
        player.displayClientMessage(Component.translatable(
                "message.seeking_immortals.dialogue.reputation_changed", delta), false);
        return true;
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
