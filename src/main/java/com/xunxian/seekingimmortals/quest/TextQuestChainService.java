package com.xunxian.seekingimmortals.quest;

import com.xunxian.seekingimmortals.catalog.ExtendedCatalogService;
import com.xunxian.seekingimmortals.registry.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Lightweight progress for text-material quest_chains (62).
 * Stored in player persistent data to avoid packet/capability protocol churn.
 * Wave48: stage advances can require and consume registered item costs (authority slice).
 * Still not the full narrative/NPC quest engine.
 */
public final class TextQuestChainService {
    private static final String ROOT_TAG = "seeking_immortals_text_quest_chains";
    private static final String REWARD_TAG = "seeking_immortals_text_quest_rewards";
    private static final String BRANCH_TAG = "seeking_immortals_text_quest_branches";
    private static final String NPC_TAG = "seeking_immortals_text_quest_npc";

    public static final String BRANCH_RIGHTEOUS = "righteous";
    public static final String BRANCH_NEUTRAL = "neutral";
    public static final String BRANCH_DEMONIC = "demonic";

    private TextQuestChainService() {}

    public record ChainProgress(String id, int stage, int stepCount, boolean complete) {}

    public record StageCost(String itemId, int count, String displayKey) {}

    public static int chainCount() {
        return ExtendedCatalogService.builtin().questChains().size();
    }

    public static Optional<ExtendedCatalogService.QuestChain> find(String chainId) {
        return ExtendedCatalogService.builtin().findQuest(chainId);
    }

    public static List<ChainProgress> listProgress(ServerPlayer player) {
        CompoundTag root = player.getPersistentData().getCompound(ROOT_TAG);
        List<ChainProgress> list = new ArrayList<>();
        for (ExtendedCatalogService.QuestChain chain : ExtendedCatalogService.builtin().questChains().values()) {
            int stage = Math.max(0, root.getInt(chain.id()));
            int steps = Math.max(0, chain.stepCount());
            boolean complete = steps > 0 && stage >= steps;
            list.add(new ChainProgress(chain.id(), stage, steps, complete));
        }
        return list;
    }

    public static ChainProgress progressOf(ServerPlayer player, String chainId) {
        Optional<ExtendedCatalogService.QuestChain> optional = find(chainId);
        int steps = optional.map(ExtendedCatalogService.QuestChain::stepCount).orElse(0);
        int stage = Math.max(0, player.getPersistentData().getCompound(ROOT_TAG).getInt(normalize(chainId)));
        boolean complete = steps > 0 && stage >= steps;
        return new ChainProgress(normalize(chainId), stage, steps, complete);
    }

    public static boolean start(ServerPlayer player, String chainId) {
        Optional<ExtendedCatalogService.QuestChain> optional = find(chainId);
        if (optional.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.text_quest.unknown", chainId), false);
            return false;
        }
        ExtendedCatalogService.QuestChain chain = optional.get();
        CompoundTag root = player.getPersistentData().getCompound(ROOT_TAG).copy();
        String id = chain.id();
        if (root.getInt(id) <= 0) {
            root.putInt(id, 1);
            player.getPersistentData().put(ROOT_TAG, root);
            // Wave45: auto-assign a default branch + NPC hook on start.
            if (getBranch(player, id).isBlank()) {
                setBranch(player, id, BRANCH_NEUTRAL);
            }
            touchNpc(player, id);
            com.xunxian.seekingimmortals.worldpack.ReputationService.add(player, factionFor(id), 1);
            player.displayClientMessage(Component.translatable("message.seeking_immortals.text_quest.started",
                    chain.display(), chain.stepCount()), true);
            player.displayClientMessage(Component.translatable("message.seeking_immortals.text_quest.npc_hook",
                    npcFor(id), getBranch(player, id)), false);
            return true;
        }
        player.displayClientMessage(Component.translatable("message.seeking_immortals.text_quest.already",
                chain.display(), root.getInt(id), chain.stepCount()), false);
        return false;
    }

    public static boolean advance(ServerPlayer player, String chainId) {
        Optional<ExtendedCatalogService.QuestChain> optional = find(chainId);
        if (optional.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.text_quest.unknown", chainId), false);
            return false;
        }
        ExtendedCatalogService.QuestChain chain = optional.get();
        CompoundTag root = player.getPersistentData().getCompound(ROOT_TAG).copy();
        String id = chain.id();
        int stage = Math.max(0, root.getInt(id));
        if (stage <= 0) {
            stage = 1;
        } else if (chain.stepCount() > 0 && stage >= chain.stepCount()) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.text_quest.complete",
                    chain.display()), false);
            return false;
        } else {
            // Wave48: consume stage cost before advancing beyond current stage.
            if (!payStageCost(player, id, stage + 1, chain.stepCount())) {
                return false;
            }
            stage++;
        }
        root.putInt(id, stage);
        player.getPersistentData().put(ROOT_TAG, root);
        if (chain.stepCount() > 0 && stage >= chain.stepCount()) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.text_quest.finished",
                    chain.display()), true);
            grantSoftFinaleReward(player, id);
            grantBranchFinaleBonus(player, id);
            FtbRewardBridgeService.onTextQuestFinished(player, id);
            com.xunxian.seekingimmortals.worldpack.ReputationService.add(player, factionFor(id), 5);
            com.xunxian.seekingimmortals.worldpack.ReputationService.onQuestComplete(player, id);
        } else {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.text_quest.advanced",
                    chain.display(), stage, Math.max(chain.stepCount(), stage)), true);
            // Wave41: mid-stage soft grant every half-chain (once per stage mark).
            grantMidStageReward(player, id, stage, chain.stepCount());
            // Branch choice window around 1/3 progress.
            maybeOfferBranchChoice(player, id, stage, chain.stepCount());
            com.xunxian.seekingimmortals.worldpack.ReputationService.add(player, factionFor(id), 1);
        }
        return true;
    }

    /**
     * Wave48 authoritative stage cost. Returns empty for free stages.
     * Pure data: item ids are string paths so unit tests do not need Forge registries.
     */
    public static Optional<StageCost> stageCostFor(String chainId, int targetStage, int stepCount) {
        String id = normalize(chainId);
        if (targetStage <= 1 || stepCount <= 0) {
            return Optional.empty();
        }
        // Mid milestone cost (half chain).
        int mid = Math.max(2, stepCount / 2);
        // Finale cost (last step).
        int finale = stepCount;
        if (targetStage == mid) {
            return Optional.of(midCost(id));
        }
        if (targetStage == finale) {
            return Optional.of(finaleCost(id));
        }
        // Every third stage after start asks for a small generic cost.
        if (targetStage > 1 && targetStage % 3 == 0) {
            return Optional.of(new StageCost("seeking_immortals:spirit_stone_shard", 1, "spirit_stone_shard"));
        }
        return Optional.empty();
    }

    private static StageCost midCost(String id) {
        if (id.contains("ghost") || id.contains("yin")) {
            return new StageCost("seeking_immortals:yin_stone", 2, "yin_stone");
        }
        if (id.contains("mulan") || id.contains("war")) {
            return new StageCost("seeking_immortals:beast_core", 1, "beast_core");
        }
        if (id.contains("void") || id.contains("star") || id.contains("chaotic")) {
            return new StageCost("seeking_immortals:spirit_stone_shard", 4, "spirit_stone_shard");
        }
        if (id.contains("dajin") || id.contains("kunwu") || id.contains("sect")) {
            return new StageCost("seeking_immortals:jade_slip_blank", 1, "jade_slip_blank");
        }
        if (id.contains("spirit") || id.contains("tianyuan") || id.contains("ascension")) {
            return new StageCost("seeking_immortals:alliance_merit_token", 1, "alliance_merit_token");
        }
        return new StageCost("seeking_immortals:spirit_stone_shard", 2, "spirit_stone_shard");
    }

    private static StageCost finaleCost(String id) {
        if (id.contains("ghost") || id.contains("yin")) {
            return new StageCost("seeking_immortals:soul_fragment", 1, "soul_fragment");
        }
        if (id.contains("mulan") || id.contains("war")) {
            return new StageCost("seeking_immortals:war_contribution_token", 1, "war_contribution_token");
        }
        if (id.contains("void") || id.contains("star") || id.contains("chaotic")) {
            return new StageCost("seeking_immortals:immortal_jade", 1, "immortal_jade");
        }
        if (id.contains("dajin") || id.contains("kunwu") || id.contains("sect")) {
            return new StageCost("seeking_immortals:spirit_stone_shard", 8, "spirit_stone_shard");
        }
        if (id.contains("spirit") || id.contains("tianyuan") || id.contains("ascension")) {
            return new StageCost("seeking_immortals:alliance_merit_token", 2, "alliance_merit_token");
        }
        return new StageCost("seeking_immortals:spirit_stone_shard", 4, "spirit_stone_shard");
    }

    private static boolean payStageCost(ServerPlayer player, String chainId, int targetStage, int stepCount) {
        Optional<StageCost> optional = stageCostFor(chainId, targetStage, stepCount);
        if (optional.isEmpty()) {
            return true;
        }
        StageCost cost = optional.get();
        if (player.getAbilities().instabuild) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.text_quest.cost_waived", cost.displayKey(), cost.count()), false);
            return true;
        }
        Item item = resolveItem(cost.itemId());
        if (item == null) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.text_quest.cost_missing",
                    cost.displayKey(), cost.count(), 0), false);
            return false;
        }
        int available = countItem(player, item);
        if (available < cost.count()) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.text_quest.cost_missing",
                    cost.displayKey(), cost.count(), available), false);
            return false;
        }
        consumeItem(player, item, cost.count());
        player.displayClientMessage(Component.translatable(
                "message.seeking_immortals.text_quest.cost_paid",
                cost.displayKey(), cost.count(), targetStage), true);
        return true;
    }

    private static Item resolveItem(String itemId) {
        if (itemId == null || itemId.isBlank()) {
            return null;
        }
        return switch (itemId) {
            case "seeking_immortals:spirit_stone_shard" -> ModItems.SPIRIT_STONE_SHARD.get();
            case "seeking_immortals:yin_stone" -> ModItems.YIN_STONE.get();
            case "seeking_immortals:beast_core" -> ModItems.BEAST_CORE.get();
            case "seeking_immortals:jade_slip_blank" -> ModItems.JADE_SLIP_BLANK.get();
            case "seeking_immortals:alliance_merit_token" -> ModItems.ALLIANCE_MERIT_TOKEN.get();
            case "seeking_immortals:soul_fragment" -> ModItems.SOUL_FRAGMENT.get();
            case "seeking_immortals:war_contribution_token" -> ModItems.WAR_CONTRIBUTION_TOKEN.get();
            case "seeking_immortals:immortal_jade" -> ModItems.IMMORTAL_JADE.get();
            default -> null;
        };
    }

    public static int countOwned(ServerPlayer player, StageCost cost) {
        if (player == null || cost == null) {
            return 0;
        }
        Item item = resolveItem(cost.itemId());
        return item == null ? 0 : countItem(player, item);
    }

    private static int countItem(ServerPlayer player, Item item) {
        int total = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(item)) {
                total += stack.getCount();
            }
        }
        for (ItemStack stack : player.getInventory().offhand) {
            if (stack.is(item)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    private static void consumeItem(ServerPlayer player, Item item, int count) {
        int remaining = Math.max(0, count);
        remaining = consumeFrom(player.getInventory().items, item, remaining);
        consumeFrom(player.getInventory().offhand, item, remaining);
        player.containerMenu.broadcastChanges();
    }

    private static int consumeFrom(java.util.List<ItemStack> stacks, Item item, int remaining) {
        for (ItemStack stack : stacks) {
            if (remaining <= 0) {
                break;
            }
            if (!stack.is(item)) {
                continue;
            }
            int used = Math.min(remaining, stack.getCount());
            stack.shrink(used);
            remaining -= used;
        }
        return remaining;
    }

    /**
     * Wave45: lightweight branch authority for text quest chains.
     * Branches: righteous / neutral / demonic. Affects finale bonus + reputation.
     */
    public static boolean chooseBranch(ServerPlayer player, String chainId, String branch) {
        Optional<ExtendedCatalogService.QuestChain> optional = find(chainId);
        if (optional.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.text_quest.unknown", chainId), false);
            return false;
        }
        String id = normalize(chainId);
        String normalized = normalizeBranch(branch);
        if (normalized.isBlank()) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.text_quest.branch_unknown", branch), false);
            return false;
        }
        ChainProgress progress = progressOf(player, id);
        if (progress.stage() <= 0) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.text_quest.branch_not_started", id), false);
            return false;
        }
        if (progress.complete()) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.text_quest.branch_locked", id), false);
            return false;
        }
        setBranch(player, id, normalized);
        touchNpc(player, id);
        com.xunxian.seekingimmortals.worldpack.ReputationService.add(player, factionForBranch(normalized), 2);
        player.displayClientMessage(Component.translatable("message.seeking_immortals.text_quest.branch_chosen",
                optional.get().display(), normalized, npcFor(id)), true);
        return true;
    }

    public static String getBranch(ServerPlayer player, String chainId) {
        return player.getPersistentData().getCompound(BRANCH_TAG).getString(normalize(chainId));
    }

    public static String getNpc(ServerPlayer player, String chainId) {
        String npc = player.getPersistentData().getCompound(NPC_TAG).getString(normalize(chainId));
        return npc.isBlank() ? npcFor(normalize(chainId)) : npc;
    }

    private static void setBranch(ServerPlayer player, String chainId, String branch) {
        CompoundTag root = player.getPersistentData().getCompound(BRANCH_TAG).copy();
        root.putString(normalize(chainId), normalizeBranch(branch));
        player.getPersistentData().put(BRANCH_TAG, root);
    }

    private static void touchNpc(ServerPlayer player, String chainId) {
        CompoundTag root = player.getPersistentData().getCompound(NPC_TAG).copy();
        root.putString(normalize(chainId), npcFor(normalize(chainId)));
        player.getPersistentData().put(NPC_TAG, root);
    }

    private static void maybeOfferBranchChoice(ServerPlayer player, String chainId, int stage, int stepCount) {
        if (stepCount < 3) {
            return;
        }
        int window = Math.max(2, stepCount / 3);
        if (stage != window) {
            return;
        }
        if (!getBranch(player, chainId).isBlank() && !BRANCH_NEUTRAL.equals(getBranch(player, chainId))) {
            return;
        }
        player.displayClientMessage(Component.translatable("message.seeking_immortals.text_quest.branch_offer",
                chainId, BRANCH_RIGHTEOUS, BRANCH_NEUTRAL, BRANCH_DEMONIC), false);
    }

    private static void grantBranchFinaleBonus(ServerPlayer player, String chainId) {
        String branch = getBranch(player, chainId);
        ItemStack bonus = switch (normalizeBranch(branch)) {
            case BRANCH_RIGHTEOUS -> new ItemStack(ModItems.ALLIANCE_MERIT_TOKEN.get(), 1);
            case BRANCH_DEMONIC -> new ItemStack(ModItems.YIN_STONE.get(), 4);
            default -> new ItemStack(ModItems.SPIRIT_STONE_SHARD.get(), 2);
        };
        if (!player.getInventory().add(bonus.copy())) {
            player.drop(bonus.copy(), false);
        }
        player.displayClientMessage(Component.translatable("message.seeking_immortals.text_quest.branch_bonus",
                branch, bonus.getHoverName()), true);
    }

    private static String normalizeBranch(String branch) {
        String b = branch == null ? "" : branch.trim().toLowerCase(Locale.ROOT);
        return switch (b) {
            case BRANCH_RIGHTEOUS, "zheng", "good", "dao" -> BRANCH_RIGHTEOUS;
            case BRANCH_DEMONIC, "mo", "evil", "xie" -> BRANCH_DEMONIC;
            case BRANCH_NEUTRAL, "zhong", "balance" -> BRANCH_NEUTRAL;
            default -> "";
        };
    }

    private static String factionFor(String chainId) {
        String id = normalize(chainId);
        if (id.contains("mulan") || id.contains("tianlan") || id.contains("war")) {
            return "mulan";
        }
        if (id.contains("ghost") || id.contains("yin") || id.contains("demon")) {
            return "demonic_path";
        }
        if (id.contains("chaotic") || id.contains("star") || id.contains("void")) {
            return "chaotic_sea";
        }
        if (id.contains("dajin") || id.contains("kunwu") || id.contains("sect")) {
            return "dajin";
        }
        if (id.contains("spirit") || id.contains("tianyuan") || id.contains("ascension")) {
            return "tianyuan";
        }
        return "mortal_realm";
    }

    private static String factionForBranch(String branch) {
        return switch (normalizeBranch(branch)) {
            case BRANCH_RIGHTEOUS -> "righteous_alliance";
            case BRANCH_DEMONIC -> "demonic_path";
            default -> "mortal_realm";
        };
    }

    /** Public so world NPC hooks / dialogue can share the same binding table. */
    public static String npcFor(String chainId) {
        String id = normalize(chainId);
        if (id.contains("huangfeng") || id.contains("qixuan")) {
            return "npc_mo_lao";
        }
        if (id.contains("mulan")) {
            return "npc_mulan_envoy";
        }
        if (id.contains("ghost") || id.contains("yin")) {
            return "npc_yinluo_steward";
        }
        if (id.contains("chaotic") || id.contains("star")) {
            return "npc_star_palace_broker";
        }
        if (id.contains("dajin") || id.contains("kunwu")) {
            return "npc_kunwu_steward";
        }
        return "npc_text_quest_guide";
    }

    private static final String MID_REWARD_TAG = "seeking_immortals_text_quest_mid_rewards";

    private static void grantMidStageReward(ServerPlayer player, String chainId, int stage, int stepCount) {
        if (stepCount < 4 || stage <= 1) {
            return;
        }
        int milestone = Math.max(2, stepCount / 2);
        if (stage != milestone) {
            return;
        }
        CompoundTag mid = player.getPersistentData().getCompound(MID_REWARD_TAG).copy();
        String key = normalize(chainId) + "@" + milestone;
        if (mid.getBoolean(key)) {
            return;
        }
        ItemStack reward = new ItemStack(ModItems.SPIRIT_STONE_SHARD.get(), 2);
        if (!player.getInventory().add(reward.copy())) {
            player.drop(reward.copy(), false);
        }
        mid.putBoolean(key, true);
        player.getPersistentData().put(MID_REWARD_TAG, mid);
        player.displayClientMessage(Component.translatable(
                "message.seeking_immortals.text_quest.mid_reward", chainId, milestone), true);
    }

    /**
     * Soft finale rewards for top mainline chains. One-time grant per chain.
     * Uses existing registered items only.
     */
    private static void grantSoftFinaleReward(ServerPlayer player, String chainId) {
        CompoundTag rewards = player.getPersistentData().getCompound(REWARD_TAG).copy();
        String id = normalize(chainId);
        if (rewards.getBoolean(id)) {
            return;
        }
        List<ItemStack> stacks = softRewardsFor(id);
        if (stacks.isEmpty()) {
            return;
        }
        for (ItemStack stack : stacks) {
            if (!player.getInventory().add(stack.copy())) {
                player.drop(stack.copy(), false);
            }
        }
        rewards.putBoolean(id, true);
        player.getPersistentData().put(REWARD_TAG, rewards);
        player.displayClientMessage(Component.translatable("message.seeking_immortals.text_quest.soft_reward", id), true);
    }

    private static List<ItemStack> softRewardsFor(String chainId) {
        return switch (chainId) {
            case "huangfeng_cultivation_path" -> List.of(
                    stack(ModItems.FOUNDATION_BUILDING_PILL_LOW, 1),
                    stack(ModItems.ALLIANCE_MERIT_TOKEN, 2));
            case "ghost_path" -> List.of(
                    stack(ModItems.YIN_STONE, 8),
                    stack(ModItems.SOUL_FRAGMENT, 2));
            case "dajin_kunwu_line" -> List.of(
                    stack(ModItems.IMMORTAL_JADE, 1),
                    stack(ModItems.VOID_CRYSTAL, 1));
            case "chaotic_sea_politics" -> List.of(
                    stack(ModItems.STAR_PALACE_TAX_RECEIPT, 1),
                    stack(ModItems.SPIRIT_STONE_SHARD, 16));
            case "spirit_realm_rise" -> List.of(
                    stack(ModItems.ALLIANCE_MERIT_TOKEN, 3),
                    stack(ModItems.IMMORTAL_JADE, 1));
            // Wave34: expand top mainline soft rewards to 10 real quest_chains ids.
            case "mulan_tianlan_war" -> List.of(
                    stack(ModItems.WAR_CONTRIBUTION_TOKEN, 2),
                    stack(ModItems.SPIRIT_STONE_SHARD, 12));
            case "chain_seven_sect_outer_to_inner" -> List.of(
                    stack(ModItems.JADE_SLIP_BLANK, 1),
                    stack(ModItems.ALLIANCE_MERIT_TOKEN, 2));
            case "yin_cluster_pilgrim" -> List.of(
                    stack(ModItems.YIN_STONE, 12),
                    stack(ModItems.SOUL_GATHERING_STONE, 1));
            case "inverse_star_recruit" -> List.of(
                    stack(ModItems.VOID_MARROW, 1),
                    stack(ModItems.SPIRIT_STONE_SHARD, 20));
            case "chain_ascension_spirit_world" -> List.of(
                    stack(ModItems.IMMORTAL_JADE, 1),
                    stack(ModItems.BREAKTHROUGH_PILL, 1));
            // Wave39: expand soft rewards to 15 real quest_chains ids.
            case "qixuan_mortal_path" -> List.of(
                    stack(ModItems.SPIRIT_STONE_SHARD, 12),
                    stack(ModItems.QI_RECOVERY_PILL, 2));
            case "blood_forbidden_campaign" -> List.of(
                    stack(ModItems.DEMONIC_BLOOD_CORAL, 1),
                    stack(ModItems.SPIRIT_STONE_SHARD, 16));
            case "fallen_demon_campaign" -> List.of(
                    stack(ModItems.DEMONIC_BLOOD_CORAL, 1),
                    stack(ModItems.YIN_STONE, 8));
            case "void_palace_campaign" -> List.of(
                    stack(ModItems.VOID_CRYSTAL, 1),
                    stack(ModItems.VOID_MARROW, 1));
            case "tianyuan_merit_path" -> List.of(
                    stack(ModItems.ALLIANCE_MERIT_TOKEN, 4),
                    stack(ModItems.SPIRIT_STONE_SHARD, 20));
            // Wave40: remaining chains get a generic one-time soft reward.
            default -> List.of(stack(ModItems.SPIRIT_STONE_SHARD, 4));
        };
    }

    private static ItemStack stack(RegistryObject<? extends Item> item, int count) {
        return new ItemStack(item.get(), Math.max(1, count));
    }

    public static Map<String, Integer> activeMap(ServerPlayer player) {
        CompoundTag root = player.getPersistentData().getCompound(ROOT_TAG);
        Map<String, Integer> map = new LinkedHashMap<>();
        for (String key : root.getAllKeys()) {
            map.put(key, root.getInt(key));
        }
        return map;
    }

    private static String normalize(String id) {
        return id == null ? "" : id.trim().toLowerCase(Locale.ROOT);
    }
}
