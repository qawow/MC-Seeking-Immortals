package com.xunxian.seekingimmortals.quest;

import com.xunxian.seekingimmortals.artifact.ArtifactDisplayTexts;
import com.xunxian.seekingimmortals.catalog.ExtendedCatalogService;
import com.xunxian.seekingimmortals.cultivation.CultivationHelper;
import com.xunxian.seekingimmortals.cultivation.ProgressionGateApi;
import com.xunxian.seekingimmortals.item.InventoryDeliveryService;
import com.xunxian.seekingimmortals.network.SyncQuestTrackerPacket;
import com.xunxian.seekingimmortals.npc.NamedNpcRegistry;
import com.xunxian.seekingimmortals.region.RegionRegistry;
import com.xunxian.seekingimmortals.registry.ModItems;
import com.xunxian.seekingimmortals.sect.SectDefinitionService;
import com.xunxian.seekingimmortals.util.PlayerDisplayText;
import com.xunxian.seekingimmortals.worldpack.WorldpackGameplayService;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Server-authoritative progress for text-material quest_chains (62).
 * Stored in player persistent data to avoid packet/capability protocol churn.
 * Wave48+: stage advances consume registered item costs.
 * Wave454: Forge-registry cost resolve, branch lock after non-neutral choice,
 * unified reward ledger (no soft+FTB double grant), tracker sync hook.
 * Wave457: catalog rewards_finale, branch bonus ledger, main-story auto-complete,
 * richer tracker lines (cost/lock/rew) for client authority buttons.
 * Still not the full narrative/NPC quest engine.
 */
public final class TextQuestChainService {
    private static final int MAX_REWARD_COUNT = 4096;
    private static final String ROOT_TAG = "seeking_immortals_text_quest_chains";
    private static final String REWARD_TAG = "seeking_immortals_text_quest_rewards";
    private static final String BRANCH_TAG = "seeking_immortals_text_quest_branches";
    private static final String NPC_TAG = "seeking_immortals_text_quest_npc";
    /** Unified one-time ledger shared with FtbRewardBridgeService. */
    public static final String AUTHORITY_REWARD_TAG = "seeking_immortals_quest_authority_rewards";
    private static final String MID_REWARD_TAG = "seeking_immortals_text_quest_mid_rewards";
    private static final List<String> PERSISTENT_TAGS = List.of(
            ROOT_TAG, REWARD_TAG, BRANCH_TAG, NPC_TAG, MID_REWARD_TAG, AUTHORITY_REWARD_TAG);

    public static final String BRANCH_RIGHTEOUS = "righteous";
    public static final String BRANCH_NEUTRAL = "neutral";
    public static final String BRANCH_DEMONIC = "demonic";

    private TextQuestChainService() {}

    /** Preserve text-quest authority progress across death/clone. Temporary dialogue sessions are excluded. */
    public static void copyPersistentData(CompoundTag originalData, CompoundTag clonedData) {
        if (originalData == null || clonedData == null) {
            return;
        }
        for (String key : PERSISTENT_TAGS) {
            if (originalData.contains(key) && originalData.get(key) != null) {
                clonedData.put(key, originalData.get(key).copy());
            }
        }
    }

    public record ChainProgress(String id, int stage, int stepCount, boolean complete) {}

    public record StageCost(String itemId, int count, String displayKey) {}

    /** Compact state mirrored by the native quest tracker. */
    public enum TrackerState {
        AVAILABLE,
        LOCKED,
        ACTIVE,
        DONE
    }

    /** First failing start requirement. NONE means the chain can be accepted. */
    public enum StartGate {
        NONE,
        REALM,
        REGION,
        FACTION,
        PATH,
        RACE,
        PARENT,
        KARMA,
        PARTY,
        BRANCH,
        PREREQUISITE,
        DATA
    }

    public record StartEligibility(boolean eligible, StartGate gate) {
        private static StartEligibility available() {
            return new StartEligibility(true, StartGate.NONE);
        }

        private static StartEligibility blocked(StartGate gate) {
            return new StartEligibility(false, gate == null ? StartGate.DATA : gate);
        }
    }

    /** Read-only finale reward description. It never grants or marks a reward. */
    public record RewardPreview(String itemId, int count) {}

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
            player.displayClientMessage(Component.translatable("message.seeking_immortals.text_quest.unknown",
                    questDisplay(chainId)), false);
            return false;
        }
        ExtendedCatalogService.QuestChain chain = optional.get();
        if (!meetsStartRequirements(player, chain, true)) {
            return false;
        }
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
                    questDisplay(chain), chain.stepCount()), true);
            player.displayClientMessage(Component.translatable("message.seeking_immortals.text_quest.npc_hook",
                    npcDisplay(npcFor(id)), branchDisplay(getBranch(player, id))), false);
            syncTracker(player);
            return true;
        }
        player.displayClientMessage(Component.translatable("message.seeking_immortals.text_quest.already",
                questDisplay(chain), root.getInt(id), chain.stepCount()), false);
        return false;
    }

    /** Server-side preflight used by direct quest starts and composite trade transactions. */
    public static boolean canStart(ServerPlayer player, String chainId) {
        Optional<ExtendedCatalogService.QuestChain> optional = find(chainId);
        if (player == null || optional.isEmpty()) {
            if (player != null) {
                player.displayClientMessage(Component.translatable(
                        "message.seeking_immortals.text_quest.unknown", questDisplay(chainId)), false);
            }
            return false;
        }
        if (progressOf(player, chainId).stage() > 0) {
            return false;
        }
        return meetsStartRequirements(player, optional.get(), true);
    }

    /**
     * Silent start preflight used by tracker snapshots. The first failed gate is
     * returned without sending chat/action-bar messages or mutating player data.
     */
    public static StartEligibility startEligibility(ServerPlayer player, String chainId) {
        return find(chainId)
                .map(chain -> startEligibility(player, chain))
                .orElseGet(() -> StartEligibility.blocked(StartGate.DATA));
    }

    private static StartEligibility startEligibility(ServerPlayer player,
                                                      ExtendedCatalogService.QuestChain chain) {
        if (player == null || chain == null) {
            return StartEligibility.blocked(StartGate.DATA);
        }
        if (player.getAbilities().instabuild) {
            return StartEligibility.available();
        }
        var cultivation = CultivationHelper.get(player).orElse(null);
        if (cultivation == null) {
            return StartEligibility.blocked(StartGate.DATA);
        }
        ExtendedCatalogService.QuestStartRequirements requirements = chain.startRequirements();
        if (requirements == null) {
            return authorityStartEligibility(player, chain);
        }
        String currentRegion = normalize(cultivation.getWorldpackCurrentRegionId());
        if (!requirements.realmMin().isBlank()
                && !WorldpackGameplayService.meetsMinRealm(cultivation.getRealm(), requirements.realmMin())) {
            return StartEligibility.blocked(StartGate.REALM);
        }
        String requiredRegion = normalize(requirements.region());
        if (!matchesStartRegion(chain.id(), requiredRegion, currentRegion)) {
            return StartEligibility.blocked(StartGate.REGION);
        }
        String requiredFaction = SectDefinitionService.canonicalizeSectId(requirements.faction());
        String currentFaction = SectDefinitionService.canonicalizeSectId(
                cultivation.getSevenMysteriesQuest().getSectId());
        if (!requiredFaction.isBlank() && !requiredFaction.equals(currentFaction)) {
            return StartEligibility.blocked(StartGate.FACTION);
        }
        String requiredPath = normalize(requirements.pathRequired());
        if (!requiredPath.isBlank() && !ProgressionGateApi.meetsPath(cultivation, requiredPath)) {
            return StartEligibility.blocked(StartGate.PATH);
        }
        String requiredRace = normalize(requirements.raceRequired());
        if (!requiredRace.isBlank() && !ProgressionGateApi.meetsRace(cultivation, requiredRace)) {
            return StartEligibility.blocked(StartGate.RACE);
        }
        String parentChain = normalize(requirements.parentChain());
        if (!parentChain.isBlank()) {
            Optional<ExtendedCatalogService.QuestChain> parent = find(parentChain);
            if (parent.isEmpty() || !progressOf(player, parentChain).complete()) {
                return StartEligibility.blocked(StartGate.PARENT);
            }
        }
        return authorityStartEligibility(player, chain);
    }

    private static StartEligibility authorityStartEligibility(
            ServerPlayer player, ExtendedCatalogService.QuestChain chain) {
        return switch (QuestAuthorityCatalog.startGate(player, chain.id())) {
            case OPEN -> StartEligibility.available();
            case REALM -> StartEligibility.blocked(StartGate.REALM);
            case PARENT -> StartEligibility.blocked(StartGate.PARENT);
            case KARMA -> StartEligibility.blocked(StartGate.KARMA);
            case PARTY -> StartEligibility.blocked(StartGate.PARTY);
            case BRANCH -> StartEligibility.blocked(StartGate.BRANCH);
            case PREREQUISITE -> StartEligibility.blocked(StartGate.PREREQUISITE);
            case FACTION -> StartEligibility.blocked(StartGate.FACTION);
            case LOAD_FAILED -> StartEligibility.blocked(StartGate.DATA);
        };
    }

    static boolean matchesStartRegion(String chainId, String requiredRegion, String currentRegion) {
        String required = normalize(requiredRegion);
        String current = normalize(currentRegion);
        if (required.isBlank() || required.equals(current)) {
            return true;
        }
        return "qixuan_mortal_path".equals(normalize(chainId))
                && "tiannan".equals(required)
                && "qinglan_mountains".equals(current);
    }

    /**
     * Silent authority preflight for an explicitly targeted one-stage transition.
     * This does not evaluate or replay authored hooks; the caller must provide its
     * own server-side proof for the requested target stage.
     */
    static boolean canTransitionExact(ServerPlayer player, String chainId, int targetStage) {
        if (player == null) {
            return false;
        }
        Optional<ExtendedCatalogService.QuestChain> optional = find(chainId);
        if (optional.isEmpty()) {
            return false;
        }
        ExtendedCatalogService.QuestChain chain = optional.get();
        if (targetStage <= 0 || targetStage > chain.stepCount()) {
            return false;
        }
        ChainProgress progress = progressOf(player, chain.id());
        if (progress.complete() || progress.stage() + 1 != targetStage) {
            return false;
        }
        if (targetStage == 1) {
            return progress.stage() == 0 && meetsStartRequirements(player, chain, false);
        }
        if (QuestAuthorityCatalog.stageGate(player, chain.id(), targetStage)
                != QuestAuthorityCatalog.Gate.OPEN) {
            return false;
        }
        Optional<StageCost> cost = stageCostFor(chain.id(), targetStage, chain.stepCount());
        return cost.isEmpty() || player.getAbilities().instabuild
                || countOwned(player, cost.get()) >= cost.get().count();
    }

    /**
     * Applies exactly one preflighted native transition and verifies its final
     * stage. Normal start/advance code remains the only owner of costs, rewards,
     * reputation, branch setup, and tracker synchronization.
     */
    static boolean transitionExact(ServerPlayer player, String chainId, int targetStage) {
        if (!canTransitionExact(player, chainId, targetStage)) {
            return false;
        }
        String id = normalize(chainId);
        boolean accepted = targetStage == 1 ? start(player, id) : advance(player, id);
        return accepted && progressOf(player, id).stage() == targetStage;
    }

    private static boolean meetsStartRequirements(ServerPlayer player,
                                                  ExtendedCatalogService.QuestChain chain,
                                                  boolean warn) {
        if (player == null || chain == null) {
            return false;
        }
        StartEligibility eligibility = startEligibility(player, chain);
        if (eligibility.eligible()) {
            return true;
        }
        if (!warn) {
            return false;
        }
        var cultivation = CultivationHelper.get(player).orElse(null);
        ExtendedCatalogService.QuestStartRequirements requirements = chain.startRequirements();
        switch (eligibility.gate()) {
            case REALM -> player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.text_quest.start_realm_too_low",
                    questDisplay(chain), ArtifactDisplayTexts.realm(requirements.realmMin()),
                    ArtifactDisplayTexts.realm(cultivation.getRealm().name())), true);
            case REGION -> {
                String requiredRegion = normalize(requirements.region());
                String currentRegion = normalize(cultivation.getWorldpackCurrentRegionId());
                player.displayClientMessage(Component.translatable(
                        "message.seeking_immortals.text_quest.start_wrong_region",
                        questDisplay(chain), regionDisplay(requiredRegion), regionDisplay(currentRegion)), true);
            }
            case FACTION -> {
                String requiredFaction = SectDefinitionService.canonicalizeSectId(requirements.faction());
                String currentFaction = SectDefinitionService.canonicalizeSectId(
                        cultivation.getSevenMysteriesQuest().getSectId());
                player.displayClientMessage(Component.translatable(
                        "message.seeking_immortals.text_quest.start_wrong_faction",
                        questDisplay(chain), factionDisplay(requiredFaction), factionDisplay(currentFaction)), true);
            }
            case PATH -> player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.text_quest.start_wrong_path",
                    questDisplay(chain), pathDisplay(requirements.pathRequired())), true);
            case RACE -> player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.text_quest.start_wrong_race",
                    questDisplay(chain), raceDisplay(requirements.raceRequired())), true);
            case PARENT -> player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.text_quest.start_parent_incomplete",
                    questDisplay(chain), questDisplay(effectiveParent(requirements))), true);
            case KARMA -> player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.text_quest.karma_required"), true);
            case PARTY -> player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.text_quest.party_too_large"), true);
            case BRANCH -> player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.text_quest.stage_branch_required"), true);
            case PREREQUISITE -> player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.text_quest.stage_prerequisite_required"), true);
            case DATA -> player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.text_quest.start_no_data"), true);
            case NONE -> {
                // No warning is needed for an eligible chain.
            }
        }
        return false;
    }

    private static String effectiveParent(ExtendedCatalogService.QuestStartRequirements requirements) {
        if (requirements == null) {
            return "";
        }
        return requirements.parentChain().isBlank() ? requirements.extendsChain() : requirements.parentChain();
    }

    public static boolean advance(ServerPlayer player, String chainId) {
        Optional<ExtendedCatalogService.QuestChain> optional = find(chainId);
        if (optional.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.text_quest.unknown",
                    questDisplay(chainId)), false);
            return false;
        }
        ExtendedCatalogService.QuestChain chain = optional.get();
        CompoundTag root = player.getPersistentData().getCompound(ROOT_TAG).copy();
        String id = chain.id();
        int stage = Math.max(0, root.getInt(id));
        if (stage <= 0) {
            // Must start() first so realm/region/faction gates cannot be skipped.
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.text_quest.not_started", questDisplay(chain)), false);
            return false;
        }
        if (chain.stepCount() <= 0) {
            // Zero-step chains can never complete; refuse to farm stages/reputation.
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.text_quest.start_no_data"), false);
            return false;
        }
        if (stage > chain.stepCount()) {
            // Stale save over-shifted by a data shrink: settle the finale exactly once.
            stage = chain.stepCount();
            root.putInt(id, stage);
            player.getPersistentData().put(ROOT_TAG, root);
            return finishChain(player, chain, id);
        }
        if (stage >= chain.stepCount()) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.text_quest.complete",
                    questDisplay(chain)), false);
            return false;
        }
        QuestAuthorityCatalog.Gate stageGate = QuestAuthorityCatalog.stageGate(player, id, stage + 1);
        if (stageGate != QuestAuthorityCatalog.Gate.OPEN) {
            warnStageGate(player, stageGate);
            return false;
        }
        // Wave48: consume stage cost before advancing beyond current stage.
        if (!payStageCost(player, id, stage + 1, chain.stepCount())) {
            return false;
        }
        stage++;
        root.putInt(id, stage);
        player.getPersistentData().put(ROOT_TAG, root);
        if (stage >= chain.stepCount()) {
            return finishChain(player, chain, id);
        }
        player.displayClientMessage(Component.translatable("message.seeking_immortals.text_quest.advanced",
                questDisplay(chain), stage, Math.max(chain.stepCount(), stage)), true);
        // Wave41: mid-stage soft grant every half-chain (once per stage mark).
        grantMidStageReward(player, id, stage, chain.stepCount());
        // Branch choice window around 1/3 progress.
        maybeOfferBranchChoice(player, id, stage, chain.stepCount());
        com.xunxian.seekingimmortals.worldpack.ReputationService.add(player, factionFor(id), 1);
        syncTracker(player);
        return true;
    }

    /** Single-authority chain finale: rewards, main-story flag, reputation and sync. */
    private static boolean finishChain(ServerPlayer player, ExtendedCatalogService.QuestChain chain, String id) {
        player.displayClientMessage(Component.translatable("message.seeking_immortals.text_quest.finished",
                questDisplay(chain)), true);
        // Wave454: single authority finale path (ledger prevents double grant with FTB bridge).
        grantAuthorityFinaleReward(player, id);
        grantBranchFinaleBonus(player, id);
        QuestRewardService.onTextChainFinished(player, id);
        FtbRewardBridgeService.onTextQuestFinished(player, id);
        // Wave457: finishing a chain with main_chapter_ref auto-completes that chapter flag.
        maybeCompleteMainStory(player, chain);
        com.xunxian.seekingimmortals.worldpack.ReputationService.add(player, factionFor(id), 5);
        com.xunxian.seekingimmortals.worldpack.ReputationService.onQuestComplete(player, id);
        syncTracker(player);
        return true;
    }

    private static void warnStageGate(ServerPlayer player, QuestAuthorityCatalog.Gate gate) {
        String key = switch (gate) {
            case BRANCH -> "message.seeking_immortals.text_quest.stage_branch_required";
            case PREREQUISITE -> "message.seeking_immortals.text_quest.stage_prerequisite_required";
            case FACTION -> "message.seeking_immortals.text_quest.stage_faction_required";
            case PARTY -> "message.seeking_immortals.text_quest.party_too_large";
            case KARMA -> "message.seeking_immortals.text_quest.karma_required";
            case LOAD_FAILED -> "message.seeking_immortals.text_quest.start_no_data";
            default -> "message.seeking_immortals.text_quest.start_no_data";
        };
        player.displayClientMessage(Component.translatable(key), true);
    }

    /**
     * Wave48 authoritative stage cost. Returns empty for free stages.
     * Pure data: item ids are string paths so unit tests do not need Forge registries.
     */
    /**
     * Expected hook for the player's current 1-based stage.
     * Empty means the chain has no authored step hook for that stage (dialogue advance allowed).
     */
    public static Optional<String> expectedHookForStage(String chainId, int stage) {
        if (stage <= 0) {
            return Optional.empty();
        }
        return find(chainId).flatMap(chain -> {
            List<String> hooks = chain.stepHooks();
            if (hooks == null || hooks.isEmpty() || stage > hooks.size()) {
                return Optional.empty();
            }
            String hook = hooks.get(stage - 1);
            if (hook == null || hook.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(normalize(hook));
        });
    }

    /** True when the fired hook matches the current stage, or the stage has no authored hook. */
    public static boolean matchesCurrentStepHook(ServerPlayer player, String chainId, String hookId) {
        if (player == null) {
            return false;
        }
        ChainProgress progress = progressOf(player, chainId);
        if (progress.stage() <= 0 || progress.complete()) {
            return false;
        }
        Optional<String> expected = expectedHookForStage(chainId, progress.stage());
        if (expected.isEmpty()) {
            return true;
        }
        return expected.get().equals(normalize(hookId));
    }

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

    public static Optional<StageCost> nextStageCostFor(ServerPlayer player, String chainId) {
        if (player == null) {
            return Optional.empty();
        }
        ChainProgress progress = progressOf(player, chainId);
        if (progress.complete() || progress.stage() <= 0) {
            return Optional.empty();
        }
        return stageCostFor(progress.id(), progress.stage() + 1, progress.stepCount());
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
                    "message.seeking_immortals.text_quest.cost_waived", costDisplay(cost), cost.count()), false);
            return true;
        }
        Item item = resolveItem(cost.itemId());
        if (item == null) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.text_quest.cost_missing",
                    costDisplay(cost), cost.count(), 0), false);
            return false;
        }
        int available = countItem(player, item);
        if (available < cost.count()) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.text_quest.cost_missing",
                    costDisplay(cost), cost.count(), available), false);
            return false;
        }
        consumeItem(player, item, cost.count());
        player.displayClientMessage(Component.translatable(
                "message.seeking_immortals.text_quest.cost_paid",
                costDisplay(cost), cost.count(), targetStage), true);
        return true;
    }

    /**
     * Wave454: resolve any registered item id (including bulk carriers) via Forge registry.
     * Pure string path so unit tests can still call stageCostFor without registries.
     */
    public static Item resolveItem(String itemId) {
        if (itemId == null || itemId.isBlank()) {
            return null;
        }
        String raw = itemId.trim().toLowerCase(Locale.ROOT);
        if (!raw.contains(":")) {
            raw = "seeking_immortals:" + raw;
        }
        ResourceLocation rl = ResourceLocation.tryParse(raw);
        if (rl == null) {
            return null;
        }
        Item item = ForgeRegistries.ITEMS.getValue(rl);
        // Forge returns air for unknown ids on some mappings; treat air as missing.
        if (item == null || item == net.minecraft.world.item.Items.AIR) {
            return null;
        }
        return item;
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
            player.displayClientMessage(Component.translatable("message.seeking_immortals.text_quest.unknown",
                    questDisplay(chainId)), false);
            return false;
        }
        String id = normalize(chainId);
        String normalized = normalizeBranch(branch);
        if (normalized.isBlank()) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.text_quest.branch_unknown",
                    Component.translatable("text.seeking_immortals.unknown_branch")), false);
            return false;
        }
        ChainProgress progress = progressOf(player, id);
        if (progress.stage() <= 0) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.text_quest.branch_not_started",
                    questDisplay(id)), false);
            return false;
        }
        if (progress.complete()) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.text_quest.branch_locked",
                    questDisplay(id)), false);
            return false;
        }
        String current = getBranch(player, id);
        // Wave454: once a non-neutral branch is chosen, lock it (neutral may still switch once).
        if (!current.isBlank() && !BRANCH_NEUTRAL.equals(current) && !current.equals(normalized)) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.text_quest.branch_already_locked",
                    questDisplay(id), branchDisplay(current)), false);
            return false;
        }
        if (current.equals(normalized)) {
            // Same branch re-select must not farm reputation.
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.text_quest.branch_same", questDisplay(optional.get()),
                    branchDisplay(normalized)), false);
            return false;
        }
        setBranch(player, id, normalized);
        touchNpc(player, id);
        com.xunxian.seekingimmortals.worldpack.ReputationService.add(player, factionForBranch(normalized), 2);
        player.displayClientMessage(Component.translatable("message.seeking_immortals.text_quest.branch_chosen",
                questDisplay(optional.get()), branchDisplay(normalized), npcDisplay(npcFor(id))), true);
        syncTracker(player);
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
                questDisplay(chainId), branchDisplay(BRANCH_RIGHTEOUS), branchDisplay(BRANCH_NEUTRAL),
                branchDisplay(BRANCH_DEMONIC)), false);
    }

    private static void grantBranchFinaleBonus(ServerPlayer player, String chainId) {
        String id = normalize(chainId);
        String branch = normalizeBranch(getBranch(player, chainId));
        if (branch.isBlank()) {
            branch = BRANCH_NEUTRAL;
        }
        // Wave457: one-time branch bonus under shared authority ledger (key id#branch).
        String ledgerKey = id + "#" + branch;
        if (hasAuthorityReward(player, ledgerKey)) {
            return;
        }
        ItemStack bonus = switch (branch) {
            case BRANCH_RIGHTEOUS -> new ItemStack(ModItems.ALLIANCE_MERIT_TOKEN.get(), 1);
            case BRANCH_DEMONIC -> new ItemStack(ModItems.YIN_STONE.get(), 4);
            default -> new ItemStack(ModItems.SPIRIT_STONE_SHARD.get(), 2);
        };
        InventoryDeliveryService.giveOrEnqueue(player, bonus, "quest_chain");
        markAuthorityReward(player, ledgerKey);
        player.displayClientMessage(Component.translatable("message.seeking_immortals.text_quest.branch_bonus",
                branchDisplay(branch), itemDisplay(bonus)), true);
    }

    private static void maybeCompleteMainStory(ServerPlayer player, ExtendedCatalogService.QuestChain chain) {
        if (player == null || chain == null) {
            return;
        }
        String chapterRef = chain.mainChapterRef();
        if (chapterRef == null || chapterRef.isBlank()) {
            return;
        }
        MainStorySoftService.completeQuiet(player, chapterRef);
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
        InventoryDeliveryService.giveOrEnqueue(player, reward, "quest_chain");
        mid.putBoolean(key, true);
        player.getPersistentData().put(MID_REWARD_TAG, mid);
        player.displayClientMessage(Component.translatable(
                "message.seeking_immortals.text_quest.mid_reward", questDisplay(chainId), milestone), true);
    }

    /**
     * Wave454 authority finale: one-time grant per chain via shared ledger.
     * FtbRewardBridgeService checks the same ledger so soft+FTB cannot double-pay.
     */
    private static void grantAuthorityFinaleReward(ServerPlayer player, String chainId) {
        String id = normalize(chainId);
        if (hasAuthorityReward(player, id)) {
            return;
        }
        List<ItemStack> stacks = authorityRewardsFor(id);
        if (stacks.isEmpty()) {
            markAuthorityReward(player, id);
            return;
        }
        for (ItemStack stack : stacks) {
            InventoryDeliveryService.giveOrEnqueue(player,
                    com.xunxian.seekingimmortals.worldpack.DailyEventEffectExecutor.adjustMeritStack(player, stack),
                    "quest_chain");
        }
        markAuthorityReward(player, id);
        // Keep legacy soft-reward tag for older clients/docs that still read it.
        CompoundTag legacy = player.getPersistentData().getCompound(REWARD_TAG).copy();
        legacy.putBoolean(id, true);
        player.getPersistentData().put(REWARD_TAG, legacy);
        player.displayClientMessage(Component.translatable("message.seeking_immortals.text_quest.authority_reward",
                questDisplay(id)), true);
    }

    public static boolean hasAuthorityReward(ServerPlayer player, String chainId) {
        if (player == null) {
            return false;
        }
        String id = normalize(chainId);
        if (player.getPersistentData().getCompound(AUTHORITY_REWARD_TAG).getBoolean(id)) {
            return true;
        }
        // Migrate older soft/FTB tags into the unified ledger on first check.
        if (player.getPersistentData().getCompound(REWARD_TAG).getBoolean(id)
                || player.getPersistentData().getCompound(FtbRewardBridgeService.ROOT_TAG).getBoolean(id)) {
            markAuthorityReward(player, id);
            return true;
        }
        return false;
    }

    public static void markAuthorityReward(ServerPlayer player, String chainId) {
        if (player == null) {
            return;
        }
        String id = normalize(chainId);
        CompoundTag tag = player.getPersistentData().getCompound(AUTHORITY_REWARD_TAG).copy();
        tag.putBoolean(id, true);
        player.getPersistentData().put(AUTHORITY_REWARD_TAG, tag);
    }

    private static List<ItemStack> authorityRewardsFor(String chainId) {
        List<ItemStack> stacks = new ArrayList<>();
        for (RewardPreview preview : finaleRewardPreview(chainId)) {
            Item item;
            try {
                item = resolveItem(preview.itemId());
            } catch (Throwable ignored) {
                continue;
            }
            if (item != null) {
                stacks.add(new ItemStack(item, preview.count()));
            }
        }
        return List.copyOf(stacks);
    }

    /**
     * Resolve catalog rewards_finale ids into stacks. Unknown/non-item tokens are skipped.
     * Supports optional "id*count" / "id:count" suffixes.
     */
    public static List<ItemStack> catalogFinaleRewards(String chainId) {
        List<ItemStack> stacks = new ArrayList<>();
        for (RewardPreview preview : catalogFinaleRewardPreview(chainId)) {
            Item item;
            try {
                item = resolveItem(preview.itemId());
            } catch (Throwable ignored) {
                continue;
            }
            if (item != null) {
                stacks.add(new ItemStack(item, preview.count()));
            }
        }
        return List.copyOf(stacks);
    }

    /**
     * Catalog-first finale preview shared with the authority grant path. If no
     * catalog token resolves to a registered item, the legacy fallback table is
     * returned as pure ids so headless documentation/tests remain safe.
     */
    public static List<RewardPreview> finaleRewardPreview(String chainId) {
        List<RewardPreview> catalog = catalogFinaleRewardPreview(chainId);
        return catalog.isEmpty() ? fallbackFinaleRewardPreview(normalize(chainId)) : catalog;
    }

    private static List<RewardPreview> catalogFinaleRewardPreview(String chainId) {
        Optional<ExtendedCatalogService.QuestChain> optional = find(chainId);
        if (optional.isEmpty()) {
            return List.of();
        }
        List<String> finale = optional.get().rewardsFinale();
        if (finale == null || finale.isEmpty()) {
            return List.of();
        }
        List<RewardPreview> previews = new ArrayList<>();
        for (String raw : finale) {
            ParsedReward parsed = parseRewardToken(raw);
            if (parsed == null) {
                continue;
            }
            Item item;
            try {
                // Unit tests without Forge bootstrap cannot touch ForgeRegistries.
                item = resolveItem(parsed.itemId());
            } catch (Throwable ignored) {
                continue;
            }
            if (item == null) {
                continue;
            }
            previews.add(new RewardPreview(namespacedItemId(parsed.itemId()), parsed.count()));
        }
        return List.copyOf(previews);
    }

    private record ParsedReward(String itemId, int count) {}

    private static ParsedReward parseRewardToken(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String token = raw.trim();
        int count = 1;
        String itemId = token;
        int star = token.lastIndexOf('*');
        int colon = token.lastIndexOf(':');
        int separator = star > 0 ? star : colon;
        if (separator > 0 && separator < token.length() - 1) {
            String maybe = token.substring(separator + 1).trim();
            String prefix = token.substring(0, separator).trim();
            if (!prefix.isEmpty() && !maybe.isEmpty()
                    && maybe.chars().allMatch(Character::isDigit)) {
                count = boundedRewardCount(maybe);
                itemId = prefix;
            }
        }
        if (itemId.isBlank()) {
            return null;
        }
        return new ParsedReward(itemId, count);
    }

    static int boundedRewardCount(String digits) {
        if (digits == null || digits.isBlank()) {
            return 1;
        }
        int value = 0;
        for (int index = 0; index < digits.length(); index++) {
            char digit = digits.charAt(index);
            if (digit < '0' || digit > '9') {
                return 1;
            }
            int next = digit - '0';
            if (value > (MAX_REWARD_COUNT - next) / 10) {
                return MAX_REWARD_COUNT;
            }
            value = value * 10 + next;
        }
        return Math.max(1, value);
    }

    private static String namespacedItemId(String itemId) {
        String id = normalize(itemId);
        return id.contains(":") ? id : "seeking_immortals:" + id;
    }

    /** Authority fallback table, shared by grants and read-only previews. */
    private static List<RewardPreview> fallbackFinaleRewardPreview(String chainId) {
        return switch (chainId) {
            case "huangfeng_cultivation_path" -> List.of(
                    reward("foundation_building_pill_low", 1),
                    reward("alliance_merit_token", 2));
            case "ghost_path" -> List.of(
                    reward("yin_stone", 8),
                    reward("soul_fragment", 2));
            case "dajin_kunwu_line" -> List.of(
                    reward("immortal_jade", 1),
                    reward("void_crystal", 1));
            case "chaotic_sea_politics" -> List.of(
                    reward("star_palace_tax_receipt", 1),
                    reward("spirit_stone_shard", 16));
            case "spirit_realm_rise" -> List.of(
                    reward("alliance_merit_token", 3),
                    reward("immortal_jade", 1));
            // Wave34: expand top mainline soft rewards to 10 real quest_chains ids.
            case "mulan_tianlan_war" -> List.of(
                    reward("war_contribution_token", 2),
                    reward("spirit_stone_shard", 12));
            case "chain_seven_sect_outer_to_inner" -> List.of(
                    reward("jade_slip_blank", 1),
                    reward("alliance_merit_token", 2));
            case "yin_cluster_pilgrim" -> List.of(
                    reward("yin_stone", 12),
                    reward("soul_gathering_stone", 1));
            case "inverse_star_recruit" -> List.of(
                    reward("void_marrow", 1),
                    reward("spirit_stone_shard", 20));
            case "chain_ascension_spirit_world" -> List.of(
                    reward("immortal_jade", 1),
                    reward("jiangchen_pill", 1));
            // Wave39: expand soft rewards to 15 real quest_chains ids.
            case "qixuan_mortal_path" -> List.of(
                    reward("spirit_stone_shard", 12),
                    reward("spirit_recovery_pill", 2));
            case "blood_forbidden_campaign" -> List.of(
                    reward("demonic_blood_coral", 1),
                    reward("spirit_stone_shard", 16));
            case "fallen_demon_campaign" -> List.of(
                    reward("demonic_blood_coral", 1),
                    reward("yin_stone", 8));
            case "void_palace_campaign" -> List.of(
                    reward("void_crystal", 1),
                    reward("void_marrow", 1));
            case "tianyuan_merit_path" -> List.of(
                    reward("alliance_merit_token", 4),
                    reward("spirit_stone_shard", 20));
            // Wave40: remaining chains get a generic one-time soft reward.
            default -> List.of(reward("spirit_stone_shard", 4));
        };
    }

    private static RewardPreview reward(String itemId, int count) {
        return new RewardPreview(namespacedItemId(itemId), Math.max(1, count));
    }

    public static Map<String, Integer> activeMap(ServerPlayer player) {
        CompoundTag root = player.getPersistentData().getCompound(ROOT_TAG);
        Map<String, Integer> map = new LinkedHashMap<>();
        for (String key : root.getAllKeys()) {
            map.put(key, root.getInt(key));
        }
        return map;
    }

    /** Sync compact tracker lines to client (no protocol field change). */
    public static void syncTracker(ServerPlayer player) {
        if (player == null) {
            return;
        }
        SyncQuestTrackerPacket.send(player, buildTrackerLines(player));
    }

    /**
     * Machine-readable full quest catalog for client authority buttons.
     * Legacy fields are retained; STATE/GATE are optional to older clients.
     * Format: {@code <id> <stage>/<steps> [DONE] branch=<b> cost=<item>:<need> own=<n> LOCK=<0|1> REW=<0|1> STATE=<state> GATE=<gate>}
     */
    public static List<String> buildTrackerLines(ServerPlayer player) {
        List<String> lines = new ArrayList<>();
        List<ChainProgress> progress = player == null ? emptyProgress() : listProgress(player);
        for (ChainProgress chain : progress) {
            lines.add(formatTrackerLine(player, chain));
        }
        return List.copyOf(lines);
    }

    private static List<ChainProgress> emptyProgress() {
        List<ChainProgress> progress = new ArrayList<>();
        for (ExtendedCatalogService.QuestChain chain : ExtendedCatalogService.builtin().questChains().values()) {
            progress.add(new ChainProgress(chain.id(), 0, Math.max(0, chain.stepCount()), false));
        }
        return progress;
    }

    static TrackerState trackerState(ChainProgress progress, StartEligibility eligibility) {
        if (progress.complete() || (progress.stepCount() > 0 && progress.stage() >= progress.stepCount())) {
            return TrackerState.DONE;
        }
        if (progress.stage() > 0) {
            return TrackerState.ACTIVE;
        }
        return eligibility != null && eligibility.eligible()
                ? TrackerState.AVAILABLE : TrackerState.LOCKED;
    }

    public static String formatTrackerLine(ServerPlayer player, ChainProgress chain) {
        StartEligibility eligibility = chain.stage() <= 0 && !chain.complete()
                ? startEligibility(player, chain.id()) : StartEligibility.available();
        TrackerState state = trackerState(chain, eligibility);
        StartGate gate = state == TrackerState.LOCKED ? eligibility.gate() : StartGate.NONE;
        boolean done = state == TrackerState.DONE;
        String branch = player == null ? BRANCH_NEUTRAL : getBranch(player, chain.id());
        if (branch == null || branch.isBlank()) {
            branch = BRANCH_NEUTRAL;
        }
        boolean locked = !BRANCH_NEUTRAL.equals(normalizeBranch(branch)) && !branch.isBlank();
        boolean rewarded = player != null && hasAuthorityReward(player, chain.id());
        String costPart = "cost=-:0 own=0";
        if (!done) {
            int target = Math.max(1, chain.stage() + 1);
            Optional<StageCost> cost = stageCostFor(chain.id(), target, chain.stepCount());
            if (cost.isPresent()) {
                StageCost c = cost.get();
                int owned = player == null ? 0 : countOwned(player, c);
                costPart = "cost=" + shortItemId(c.itemId()) + ":" + c.count() + " own=" + owned;
            }
        }
        String line = chain.id() + " " + chain.stage() + "/" + chain.stepCount()
                + (done ? " DONE" : "")
                + " branch=" + branch
                + " " + costPart
                + " LOCK=" + (locked ? 1 : 0)
                + " REW=" + (rewarded ? 1 : 0)
                + " STATE=" + state.name()
                + " GATE=" + gate.name();
        return limitTrackerLine(line, state, gate);
    }

    private static String limitTrackerLine(String line, TrackerState state, StartGate gate) {
        final int maxLength = SyncQuestTrackerPacket.MAX_LINE_LENGTH;
        if (line.length() <= maxLength) {
            return line;
        }
        String suffix = " STATE=" + state.name() + " GATE=" + gate.name();
        int stateMarker = line.indexOf(" STATE=");
        String prefix = stateMarker >= 0 ? line.substring(0, stateMarker) : line;
        int prefixLength = Math.max(0, maxLength - suffix.length());
        return prefix.substring(0, Math.min(prefix.length(), prefixLength)).trim() + suffix;
    }

    private static String shortItemId(String itemId) {
        if (itemId == null) {
            return "-";
        }
        int idx = itemId.indexOf(':');
        return idx >= 0 ? itemId.substring(idx + 1) : itemId;
    }

    private static Component questDisplay(String chainId) {
        return find(chainId)
                .map(TextQuestChainService::questDisplay)
                .orElseGet(() -> Component.translatable("text.seeking_immortals.unknown_quest"));
    }

    private static Component questDisplay(ExtendedCatalogService.QuestChain chain) {
        return chain == null
                ? Component.translatable("text.seeking_immortals.unknown_quest")
                : PlayerDisplayText.safeLiteral(chain.display(), "text.seeking_immortals.unknown_quest");
    }

    private static Component regionDisplay(String regionId) {
        return RegionRegistry.find(regionId)
                .map(region -> PlayerDisplayText.safeLiteral(
                        region.display(), "text.seeking_immortals.unknown_region"))
                .orElseGet(() -> Component.translatable("text.seeking_immortals.unknown_region"));
    }

    private static Component factionDisplay(String factionId) {
        String id = normalize(factionId);
        Optional<SectDefinitionService.SectDefinition> sect = SectDefinitionService.find(id);
        if (sect.isPresent()) {
            return PlayerDisplayText.safeLiteral(
                    sect.get().displayZh(), "text.seeking_immortals.unknown_faction");
        }
        Optional<ExtendedCatalogService.SectEntry> catalog = ExtendedCatalogService.builtin().findSect(id);
        if (catalog.isPresent()) {
            return PlayerDisplayText.safeLiteral(
                    catalog.get().display(), "text.seeking_immortals.unknown_faction");
        }
        return switch (id) {
            case "mortal_realm" -> Component.translatable("text.seeking_immortals.faction.mortal_realm");
            case "chaotic_sea" -> Component.translatable("text.seeking_immortals.faction.chaotic_sea");
            case "dajin" -> Component.translatable("text.seeking_immortals.faction.dajin");
            case "demonic_path" -> Component.translatable("text.seeking_immortals.faction.demonic_path");
            case "tianyuan" -> Component.translatable("text.seeking_immortals.faction.tianyuan");
            case "mulan" -> Component.translatable("text.seeking_immortals.faction.mulan");
            case "righteous_alliance" -> Component.translatable("text.seeking_immortals.faction.righteous_alliance");
            case "merchant_guild" -> Component.translatable("text.seeking_immortals.faction.merchant_guild");
            case "clan_array_mo" -> Component.translatable("text.seeking_immortals.faction.clan_array_mo");
            case "clan_refinement_yu" -> Component.translatable("text.seeking_immortals.faction.clan_refinement_yu");
            case "clan_alchemy_gu" -> Component.translatable("text.seeking_immortals.faction.clan_alchemy_gu");
            case "clan_talisman_ning" -> Component.translatable("text.seeking_immortals.faction.clan_talisman_ning");
            case "tiannan_seven" -> Component.translatable("text.seeking_immortals.faction.tiannan_seven");
            case "tianfu_gate" -> Component.translatable("text.seeking_immortals.faction.tianfu_gate");
            default -> Component.translatable("text.seeking_immortals.unknown_faction");
        };
    }

    private static Component pathDisplay(String pathId) {
        return switch (normalize(pathId)) {
            case "ghost", "ghost_cultivator" ->
                    Component.translatable("text.seeking_immortals.quest_path.ghost");
            default -> Component.translatable("text.seeking_immortals.quest_path.specific");
        };
    }

    private static Component raceDisplay(String raceId) {
        return switch (normalize(raceId)) {
            case "mulan_fashi" -> Component.translatable("text.seeking_immortals.quest_race.mulan_fashi");
            default -> Component.translatable("text.seeking_immortals.quest_race.specific");
        };
    }

    private static Component branchDisplay(String branch) {
        return switch (normalizeBranch(branch)) {
            case BRANCH_RIGHTEOUS -> Component.translatable("screen.seeking_immortals.dialogue.righteous");
            case BRANCH_NEUTRAL -> Component.translatable("screen.seeking_immortals.dialogue.neutral");
            case BRANCH_DEMONIC -> Component.translatable("screen.seeking_immortals.dialogue.demonic");
            default -> Component.translatable("text.seeking_immortals.unknown_branch");
        };
    }

    private static Component npcDisplay(String npcId) {
        return NamedNpcRegistry.find(npcId)
                .map(npc -> PlayerDisplayText.safeLiteral(
                        npc.display(), "text.seeking_immortals.quest_guide"))
                .orElseGet(() -> Component.translatable("text.seeking_immortals.quest_guide"));
    }

    private static Component costDisplay(StageCost cost) {
        if (cost == null) {
            return Component.translatable("text.seeking_immortals.unknown_item");
        }
        Item item = resolveItem(cost.itemId());
        return item == null
                ? PlayerDisplayText.itemName(cost.itemId())
                : PlayerDisplayText.itemName(item);
    }

    private static Component itemDisplay(ItemStack stack) {
        return stack == null || stack.isEmpty()
                ? Component.translatable("text.seeking_immortals.unknown_item")
                : PlayerDisplayText.itemName(stack.getItem());
    }

    private static String normalize(String id) {
        return id == null ? "" : id.trim().toLowerCase(Locale.ROOT);
    }
}
