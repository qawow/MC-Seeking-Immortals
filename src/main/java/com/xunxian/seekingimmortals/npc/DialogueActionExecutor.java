package com.xunxian.seekingimmortals.npc;

import com.xunxian.seekingimmortals.catalog.ItemCatalogService;
import com.xunxian.seekingimmortals.cultivation.CultivationHelper;
import com.xunxian.seekingimmortals.cultivation.PlayerCultivation;
import com.xunxian.seekingimmortals.region.RegionRegistry;
import com.xunxian.seekingimmortals.sect.ReputationUnlockService;
import com.xunxian.seekingimmortals.shop.ShopService;
import com.xunxian.seekingimmortals.worldpack.ReputationService;
import com.xunxian.seekingimmortals.worldpack.SecretRealmDimensionService;
import com.xunxian.seekingimmortals.worldpack.WorldpackGameplayService;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

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
        boolean ok = true;
        for (DialogueBranchService.Effect effect : effects) {
            if (!execute(player, npcId, treeId, nodeId, effect)) {
                ok = false;
            }
        }
        return ok;
    }

    /** Executes node-entry effects while leaving menu-opening effects for an explicit validated choice. */
    public static boolean executeImmediate(ServerPlayer player, String npcId, String treeId, String nodeId,
                                           List<DialogueBranchService.Effect> effects) {
        if (player == null || effects == null || effects.isEmpty()) {
            return true;
        }
        boolean ok = true;
        for (DialogueBranchService.Effect effect : effects) {
            if (!isDeferredChoice(effect) && !execute(player, npcId, treeId, nodeId, effect)) {
                ok = false;
            }
        }
        return ok;
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
                // M11: soft flag + favor, then authority settlement via QuestHookRuntime effect path.
                NpcDialogueFlags.setFlag(player, "dialogue_" + normalize(nodeId) + "_" + type);
                NpcFavorService.add(player, npcId, 1);
                settleQuestEffect(player, type, effect);
                yield true;
            }
            case ENTER_INSTANCE -> enterInstance(player, effect);
            case DENY_SERVICE, END -> {
                player.displayClientMessage(Component.literal("[对话] 对方拒绝继续交谈。"), false);
                yield true;
            }
            case MARK_STRUCTURE -> {
                String structure = firstNonBlank(effect.param("structure"), effect.param("id"), "marked_structure");
                NpcDialogueFlags.setFlag(player, "mark_" + normalize(structure));
                player.displayClientMessage(Component.literal("[对话] 记下了地点：" + structure), false);
                yield true;
            }
            case HINT, ANOMALY_LOG -> {
                player.displayClientMessage(Component.literal("[对话] ……"), false);
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
                player.displayClientMessage(Component.literal("[对话] 气氛骤然紧张。"), false);
                yield true;
            }
            default -> {
                // Unknown effects are soft-accepted for forward compatibility with M09/M11 consumers.
                NpcDialogueFlags.setFlag(player, "effect_" + type);
                yield true;
            }
        };
    }

    private static void settleQuestEffect(ServerPlayer player, String type, DialogueBranchService.Effect effect) {
        if (player == null || effect == null) {
            return;
        }
        List<String> questIds = new ArrayList<>();
        String q = firstNonBlank(effect.param("q"), effect.param("quest"), effect.param("quest_id"), effect.param("id"));
        if (!q.isBlank()) {
            questIds.add(q);
        }
        String multi = effect.param("quest_ids");
        if (multi != null && !multi.isBlank()) {
            for (String part : multi.split("[,;\\s]+")) {
                if (!part.isBlank()) {
                    questIds.add(part.trim());
                }
            }
        }
        // Catalog effect links (offer_quest:xxx etc.).
        String effectKey = normalize(type) + (q.isBlank() ? "" : ":" + normalize(q));
        questIds.addAll(com.xunxian.seekingimmortals.quest.QuestHookRuntime.questsForEffect(effectKey));
        questIds.addAll(com.xunxian.seekingimmortals.quest.QuestHookRuntime.questsForEffect(type));
        String action = normalize(type);
        for (String questId : questIds) {
            if (questId == null || questId.isBlank()) {
                continue;
            }
            String id = questId.trim();
            if ("turnin_quests".equals(action)) {
                if (com.xunxian.seekingimmortals.quest.TextQuestChainService.find(id).isPresent()) {
                    var progress = com.xunxian.seekingimmortals.quest.TextQuestChainService.progressOf(player, id);
                    if (progress.stage() > 0 && !progress.complete()) {
                        com.xunxian.seekingimmortals.quest.TextQuestChainService.advance(player, id);
                    }
                } else {
                    NpcDialogueFlags.setFlag(player, "quest_turnin_" + normalize(id));
                }
            } else {
                if (com.xunxian.seekingimmortals.quest.TextQuestChainService.find(id).isPresent()) {
                    var progress = com.xunxian.seekingimmortals.quest.TextQuestChainService.progressOf(player, id);
                    if (progress.stage() <= 0) {
                        com.xunxian.seekingimmortals.quest.TextQuestChainService.start(player, id);
                    }
                } else {
                    NpcDialogueFlags.setFlag(player, "quest_offered_" + normalize(id));
                }
            }
        }
        if (questIds.isEmpty()) {
            player.displayClientMessage(Component.literal("[对话] 任务相关动作已记录。"), false);
        } else {
            player.displayClientMessage(Component.literal("[对话] 任务结算：" + String.join(",", questIds)), false);
        }
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
        ShopService.openMarket(player, shopId);
        player.displayClientMessage(Component.literal("[对话] 打开商店：" + shopId), true);
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
                if (!player.getInventory().add(stack)) {
                    player.drop(stack, false);
                }
                ok = true;
            }
        }
        if (ok) {
            player.displayClientMessage(Component.literal("[对话] 获得物品：" + itemId + " ×" + count), false);
        } else {
            player.displayClientMessage(Component.literal("[对话] 物品暂不可发放：" + itemId), false);
        }
        return ok;
    }

    private static boolean teleportOrTravel(ServerPlayer player, DialogueBranchService.Effect effect) {
        String dest = firstNonBlank(effect.param("to"), effect.param("region"), effect.param("route"), effect.param("target"));
        int cost = effect.paramInt("cost_contribution", 0);
        if (cost > 0) {
            Optional<PlayerCultivation> cultivation = CultivationHelper.get(player);
            if (cultivation.isEmpty() || !cultivation.get().getSevenMysteriesQuest().spendContribution(cost)) {
                player.displayClientMessage(Component.literal("[对话] 贡献不足，无法传送。"), false);
                return false;
            }
        }
        if (dest.isBlank()) {
            // Soft travel: use portal array path if available.
            try {
                WorldpackGameplayService.usePortalArray(player);
                return true;
            } catch (Throwable ignored) {
                player.displayClientMessage(Component.literal("[对话] 传送阵尚未连通。"), false);
                return false;
            }
        }
        String region = normalize(dest);
        if (RegionRegistry.isKnown(region)) {
            // Mark travel intent; full spatial teleport is owned by M13/worldpack anchors.
            NpcDialogueFlags.setFlag(player, "travel_to_" + region);
            ReputationService.onPortalTravel(player, region);
            player.displayClientMessage(Component.literal("[对话] 记下了前往 " + region + " 的路径。"), false);
            return true;
        }
        NpcDialogueFlags.setFlag(player, "travel_route_" + region);
        player.displayClientMessage(Component.literal("[对话] 行程已备：" + dest), false);
        return true;
    }

    private static boolean enterInstance(ServerPlayer player, DialogueBranchService.Effect effect) {
        String realmId = firstNonBlank(effect.param("instance"), effect.param("realm"), effect.param("id"), "blood_forbidden");
        try {
            if (SecretRealmDimensionService.teleportInto(player, realmId)) {
                return true;
            }
        } catch (Throwable ignored) {
            // M09 may not fully wire every instance id yet.
        }
        NpcDialogueFlags.setFlag(player, "enter_instance_" + normalize(realmId));
        player.displayClientMessage(Component.literal("[对话] 秘境入口已标记：" + realmId), false);
        return true;
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
        player.displayClientMessage(Component.literal("[对话] 声望变化 " + key + " " + (delta >= 0 ? "+" : "") + delta), false);
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
