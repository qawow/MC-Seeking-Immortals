package com.xunxian.seekingimmortals.quest;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.xunxian.seekingimmortals.SeekingImmortalsMod;
import com.xunxian.seekingimmortals.catalog.ItemCatalogService;
import com.xunxian.seekingimmortals.cultivation.CultivationHelper;
import com.xunxian.seekingimmortals.cultivation.Realm;
import com.xunxian.seekingimmortals.item.pill.CatalogPillItem;
import com.xunxian.seekingimmortals.npc.NamedNpcRewardService;
import com.xunxian.seekingimmortals.npc.NpcDialogueFlags;
import com.xunxian.seekingimmortals.registry.ModItems;
import com.xunxian.seekingimmortals.sect.ReputationUnlockService;
import com.xunxian.seekingimmortals.sect.SectDefinitionService;
import com.xunxian.seekingimmortals.worldpack.ReputationService;
import com.xunxian.seekingimmortals.worldpack.SecretRealmCatalogService;
import com.xunxian.seekingimmortals.worldpack.SecretRealmOpenPolicy;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Server-authoritative runtime for the 23 playable v141 chains and their 95 detailed steps. */
public final class DetailedQuestRuntimeService {
    public static final String ROOT_TAG = "seeking_immortals_detailed_quests";
    public static final String REWARD_TAG = "seeking_immortals_detailed_quest_rewards";
    public static final String EVIDENCE_TAG = "seeking_immortals_detailed_quest_evidence";
    private static final int MAX_EVIDENCE = 256;
    private static final Pattern ID_TOKEN = Pattern.compile("[a-z0-9_]+", Pattern.CASE_INSENSITIVE);
    private static final Set<String> PLACE_NOISE = Set.of("or", "board", "and", "null");
    private static final Set<String> SUPPORTED_PREREQUISITES = Set.of(
            "new_game", "outer_or_inner_huangfeng_or_high_rep", "blood_forbidden_token",
            "window_open", "in_blood_forbidden", "reached_water_zone", "ascension_success",
            "arrived_tianyuan", "tianyuan_registered", "in_immortal_realm",
            "rep_zhenyan>=50 or zhenyan_outer_pass", "in_zhuimo", "zhui_mo_ling",
            "in_yinyang_ku", "qianzhu_tower_progress or dayan_page");
    private static final Set<String> SUPPORTED_NEEDS = Set.of(
            "contribution>=quota_threshold", "window_soon", "spirit_stones",
            "quest_cipher_or_intro_letter", "contribution>=500", "risk_accept",
            "kunwu_clue_pieces>=n", "fire_resist_ready", "detox_yin_resist");
    private static final Snapshot BUILTIN = loadBuiltin();
    private static final DetailedQuestProofCatalog.Snapshot PROOF_ROUTES =
            DetailedQuestProofCatalog.loadAndValidate(sourceStepCounts(BUILTIN));
    private static final Set<String> KNOWN_EVIDENCE = buildKnownEvidence();

    private DetailedQuestRuntimeService() {}

    public record Step(int number, String summary, String action, String place,
                       List<String> needs, String failure, JsonObject reward) {
        public Step {
            summary = safe(summary);
            action = safe(action);
            place = safe(place);
            needs = needs == null ? List.of() : List.copyOf(needs);
            failure = safe(failure);
            reward = reward == null ? new JsonObject() : reward.deepCopy();
        }
    }

    public record Chain(String id, String display, String region, List<String> realmSpan,
                        String giverNpc, List<String> prerequisites, List<Step> steps,
                        JsonObject finale, List<String> nextChains) {
        public Chain {
            id = normalize(id);
            display = safe(display);
            region = normalize(region);
            realmSpan = realmSpan == null ? List.of() : List.copyOf(realmSpan);
            giverNpc = normalize(giverNpc);
            prerequisites = prerequisites == null ? List.of() : List.copyOf(prerequisites);
            steps = steps == null ? List.of() : List.copyOf(steps);
            finale = finale == null ? new JsonObject() : finale.deepCopy();
            nextChains = nextChains == null ? List.of() : List.copyOf(nextChains);
        }
    }

    public record Snapshot(Map<String, Chain> chains, int stepCount,
                           Set<String> unsupportedPrerequisites, Set<String> unsupportedNeeds) {
        public Snapshot {
            chains = Collections.unmodifiableMap(new LinkedHashMap<>(chains));
            unsupportedPrerequisites = Set.copyOf(unsupportedPrerequisites);
            unsupportedNeeds = Set.copyOf(unsupportedNeeds);
        }
    }

    public record Progress(String id, int stage, int stepCount, boolean started, boolean complete) {}

    public record Evidence(Set<String> tokens) {
        public Evidence {
            LinkedHashSet<String> normalized = new LinkedHashSet<>();
            if (tokens != null) {
                for (String token : tokens) {
                    String value = normalize(token);
                    if (!value.isBlank()) {
                        normalized.add(value);
                    }
                }
            }
            tokens = Set.copyOf(normalized);
        }

        public static Evidence of(String... values) {
            LinkedHashSet<String> tokens = new LinkedHashSet<>();
            if (values != null) {
                Collections.addAll(tokens, values);
            }
            return new Evidence(tokens);
        }

        public boolean contains(String token) {
            return tokens.contains(normalize(token));
        }
    }

    public static Snapshot builtin() {
        return BUILTIN;
    }

    /** Strict proof routes consumed by the future structured event producers. */
    public static DetailedQuestProofCatalog.Snapshot proofCatalog() {
        return PROOF_ROUTES;
    }

    public static Optional<Chain> find(String chainId) {
        return Optional.ofNullable(BUILTIN.chains().get(normalize(chainId)));
    }

    public static int chainCount() {
        return BUILTIN.chains().size();
    }

    public static int stepCount() {
        return BUILTIN.stepCount();
    }

    public static void copyPersistentData(CompoundTag source, CompoundTag target) {
        if (source == null || target == null) {
            return;
        }
        copyTag(source, target, ROOT_TAG);
        copyTag(source, target, REWARD_TAG);
        copyTag(source, target, EVIDENCE_TAG);
        DetailedQuestProofService.copyPersistentData(source, target);
    }

    public static List<Progress> listProgress(ServerPlayer player) {
        List<Progress> result = new ArrayList<>();
        for (Chain chain : BUILTIN.chains().values()) {
            result.add(progressOf(player, chain.id()));
        }
        return List.copyOf(result);
    }

    public static Progress progressOf(ServerPlayer player, String chainId) {
        Chain chain = find(chainId).orElse(null);
        if (chain == null || player == null) {
            return new Progress(normalize(chainId), 0, chain == null ? 0 : chain.steps().size(), false, false);
        }
        CompoundTag state = player.getPersistentData().getCompound(ROOT_TAG).getCompound(chain.id());
        int stage = Math.max(0, state.getInt("Stage"));
        boolean complete = state.getBoolean("Complete");
        return new Progress(chain.id(), stage, chain.steps().size(), stage > 0 || complete, complete);
    }

    /** Side-effect-free start check used by world-entry hooks to avoid noisy prerequisite failures. */
    public static boolean canStart(ServerPlayer player, String chainId, Evidence evidence) {
        Chain chain = find(chainId).orElse(null);
        if (player == null || chain == null || progressOf(player, chain.id()).started()) {
            return false;
        }
        return prerequisitesMet(player, chain, evidence == null ? Evidence.of() : evidence);
    }

    /** Starts only an exact playable chain after all authored prerequisites pass. */
    public static boolean start(ServerPlayer player, String chainId, Evidence evidence) {
        Chain chain = find(chainId).orElse(null);
        if (player == null || chain == null) {
            return false;
        }
        Progress progress = progressOf(player, chain.id());
        if (progress.started()) {
            return false;
        }
        if (!canStart(player, chain.id(), evidence)) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.detailed_quest.prerequisite_failed", chain.display()), false);
            return false;
        }
        CompoundTag root = player.getPersistentData().getCompound(ROOT_TAG).copy();
        CompoundTag state = new CompoundTag();
        state.putInt("Stage", 1);
        state.putBoolean("Complete", false);
        root.put(chain.id(), state);
        player.getPersistentData().put(ROOT_TAG, root);
        player.displayClientMessage(Component.translatable(
                "message.seeking_immortals.detailed_quest.started", chain.display(), chain.steps().size()), false);
        showCurrentStep(player, chain.id());
        return true;
    }

    /** Advances one exact step only when its structured place/need proof is satisfied. */
    public static boolean advance(ServerPlayer player, String chainId, Evidence evidence) {
        return advanceInternal(player, chainId, evidence, false, null);
    }

    /**
     * Advances an exact route after {@link DetailedQuestProofService} has validated the producer
     * and the live player state. This method intentionally remains package-visible so ordinary
     * callers cannot bypass the structured proof authority.
     */
    static boolean advanceVerifiedRoute(ServerPlayer player, String chainId,
                                         DetailedQuestProofCatalog.Route route, Evidence evidence) {
        if (route == null || !route.chainId().equals(normalize(chainId))) {
            return false;
        }
        DetailedQuestProofCatalog.Route catalogRoute = PROOF_ROUTES.find(route.chainId(), route.step());
        if (catalogRoute == null || !catalogRoute.equals(route)) {
            return false;
        }
        Progress progress = progressOf(player, route.chainId());
        if (!progress.started() || progress.complete() || progress.stage() != route.step()) {
            return false;
        }
        return advanceInternal(player, chainId, evidence, true, route);
    }

    private static boolean advanceInternal(ServerPlayer player, String chainId, Evidence evidence,
                                            boolean verifiedRoute,
                                            DetailedQuestProofCatalog.Route verified) {
        Chain chain = find(chainId).orElse(null);
        if (player == null || chain == null) {
            return false;
        }
        Progress progress = progressOf(player, chain.id());
        if (!progress.started() || progress.complete() || progress.stage() > chain.steps().size()) {
            return false;
        }
        if (verifiedRoute && (verified == null || verified.step() != progress.stage())) {
            return false;
        }
        Step step = chain.steps().get(progress.stage() - 1);
        Evidence proof = evidence == null ? Evidence.of() : evidence;
        if (!verifiedRoute && !stepSatisfied(player, chain, step, proof)) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.detailed_quest.proof_failed", chain.display(), step.number()), false);
            return false;
        }
        if (!rewardPreflight(step.reward()) || progress.stage() == chain.steps().size()
                && !rewardPreflight(chain.finale())) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.detailed_quest.reward_unavailable", chain.display()), false);
            return false;
        }

        int spentContribution = contributionCost(chain, step);
        if (spentContribution > 0 && !spendContribution(player, spentContribution)) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.detailed_quest.contribution_failed", spentContribution), false);
            return false;
        }

        applyRewardOnce(player, chain.id() + ":step:" + step.number(), step.reward());
        boolean completes = progress.stage() >= chain.steps().size();
        if (completes) {
            applyRewardOnce(player, chain.id() + ":finale", chain.finale());
        }
        CompoundTag root = player.getPersistentData().getCompound(ROOT_TAG).copy();
        CompoundTag state = root.getCompound(chain.id()).copy();
        if (completes) {
            state.putBoolean("Complete", true);
            state.putInt("Stage", chain.steps().size());
        } else {
            state.putInt("Stage", progress.stage() + 1);
        }
        root.put(chain.id(), state);
        player.getPersistentData().put(ROOT_TAG, root);

        if (completes) {
            ReputationService.onQuestComplete(player, chain.id());
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.detailed_quest.completed", chain.display()), false);
            Evidence nextEvidence = Evidence.of(chain.id());
            for (String nextChain : chain.nextChains()) {
                if (canStart(player, nextChain, nextEvidence)) {
                    start(player, nextChain, nextEvidence);
                }
            }
        } else {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.detailed_quest.advanced", chain.display(), progress.stage(),
                    chain.steps().size()), false);
            showCurrentStep(player, chain.id());
        }
        return true;
    }

    /** Stores only catalog-known evidence and advances at most one current step per chain. */
    public static int recordAndAdvance(ServerPlayer player, String token) {
        String normalized = normalize(token);
        if (player == null || normalized.isBlank() || !KNOWN_EVIDENCE.contains(normalized)) {
            return 0;
        }
        recordEvidence(player, normalized);
        int advanced = 0;
        for (Progress progress : listProgress(player)) {
            if (!progress.started() || progress.complete()) {
                continue;
            }
            Chain chain = BUILTIN.chains().get(progress.id());
            Step step = chain.steps().get(progress.stage() - 1);
            Evidence proof = Evidence.of(normalized);
            if (stepSatisfied(player, chain, step, proof) && advance(player, progress.id(), proof)) {
                advanced++;
            }
        }
        return advanced;
    }

    /** NPC turn-in is exact: explicit ids first, otherwise only giver/current-place matches. */
    public static int turnIn(ServerPlayer player, List<String> questIds, String npcId, Evidence evidence) {
        if (player == null) {
            return 0;
        }
        LinkedHashSet<String> candidates = new LinkedHashSet<>();
        if (questIds != null) {
            for (String questId : questIds) {
                if (find(questId).isPresent()) {
                    candidates.add(normalize(questId));
                }
            }
        }
        String npc = normalize(npcId);
        if (candidates.isEmpty() && !npc.isBlank()) {
            for (Chain chain : BUILTIN.chains().values()) {
                Progress progress = progressOf(player, chain.id());
                if (!progress.started() || progress.complete()) {
                    continue;
                }
                Step step = chain.steps().get(progress.stage() - 1);
                if (npc.equals(chain.giverNpc()) || placeTokens(step.place()).contains(npc)) {
                    candidates.add(chain.id());
                }
            }
        }
        int advanced = 0;
        Evidence proof = mergeEvidence(evidence, npc);
        for (String chainId : candidates) {
            if (advance(player, chainId, proof)) {
                advanced++;
            }
        }
        return advanced;
    }

    public static boolean showCurrentStep(ServerPlayer player, String chainId) {
        Chain chain = find(chainId).orElse(null);
        Progress progress = progressOf(player, chainId);
        if (player == null || chain == null || !progress.started()) {
            return false;
        }
        if (progress.complete()) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.detailed_quest.completed", chain.display()), false);
            return true;
        }
        Step step = chain.steps().get(progress.stage() - 1);
        player.displayClientMessage(Component.translatable(
                "message.seeking_immortals.detailed_quest.current", chain.display(), step.number(),
                chain.steps().size(), step.summary()), false);
        player.displayClientMessage(Component.translatable(
                "message.seeking_immortals.detailed_quest.objective", step.action()), false);
        if (!step.place().isBlank()) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.detailed_quest.place", step.place()), false);
        }
        if (!step.needs().isEmpty()) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.detailed_quest.needs", String.join(", ", step.needs())), false);
        }
        if (!step.failure().isBlank()) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.detailed_quest.failure", step.failure()), false);
        }
        return true;
    }

    static boolean prereqTokenSupported(String token) {
        return SUPPORTED_PREREQUISITES.contains(normalize(token));
    }

    static boolean needTokenSupported(String token) {
        return SUPPORTED_NEEDS.contains(normalize(token));
    }

    static List<String> placeTokens(String place) {
        if (place == null || place.isBlank()) {
            return List.of();
        }
        LinkedHashSet<String> ids = new LinkedHashSet<>();
        Matcher matcher = ID_TOKEN.matcher(place.toLowerCase(Locale.ROOT));
        while (matcher.find()) {
            String id = normalize(matcher.group());
            if (!id.isBlank() && !PLACE_NOISE.contains(id)) {
                ids.add(id);
            }
        }
        return List.copyOf(ids);
    }

    private static boolean prerequisitesMet(ServerPlayer player, Chain chain, Evidence evidence) {
        for (String raw : chain.prerequisites()) {
            String token = normalize(raw);
            if (!SUPPORTED_PREREQUISITES.contains(token) || !matchesPrerequisite(player, token, evidence)) {
                return false;
            }
        }
        return true;
    }

    private static boolean matchesPrerequisite(ServerPlayer player, String token, Evidence evidence) {
        return switch (token) {
            case "new_game" -> noDetailedProgress(player);
            case "outer_or_inner_huangfeng_or_high_rep" -> hasFlag(player, "outer_disciple")
                    || sect(player).contains("huangfeng") || ReputationService.get(player, "huangfeng_gu") >= 10;
            case "blood_forbidden_token", "zhui_mo_ling" -> hasItem(player, token);
            case "window_open" -> windowOpen(player, "blood_forbidden");
            case "in_blood_forbidden" -> atRegion(player, "blood_forbidden");
            case "reached_water_zone" -> hasEvidence(player, "bf_water_jiao")
                    || hasFlag(player, "reached_water_zone");
            case "ascension_success" -> hasFlag(player, "ascension_success")
                    || player.getPersistentData().getBoolean("seeking_immortals_tribulation_success");
            case "arrived_tianyuan" -> atRegion(player, "tianyuan");
            case "tianyuan_registered" -> hasFlag(player, "tianyuan_registered");
            case "in_immortal_realm" -> atRegion(player, "xianjie") || CultivationHelper.get(player)
                    .map(c -> c.getRealm().ordinal() >= Realm.TRUE_IMMORTAL.ordinal()).orElse(false);
            case "rep_zhenyan>=50 or zhenyan_outer_pass" -> ReputationService.get(player, "zhenyan") >= 50
                    || hasFlag(player, "zhenyan_outer_pass");
            case "in_zhuimo" -> atRegion(player, "zhuimo") || atRegion(player, "fallen_demon");
            case "in_yinyang_ku" -> atRegion(player, "yinyang_ku");
            case "qianzhu_tower_progress or dayan_page" -> hasEvidence(player, "qianzhu_control_console")
                    || hasFlag(player, "console_used") || hasItem(player, "dayan_fragment");
            default -> evidence.contains(token) || hasEvidence(player, token);
        };
    }

    private static boolean stepSatisfied(ServerPlayer player, Chain chain, Step step, Evidence evidence) {
        List<String> places = placeTokens(step.place());
        if (!places.isEmpty() && places.stream().noneMatch(place -> matchesPlace(player, place, evidence))) {
            return false;
        }
        for (String need : step.needs()) {
            String token = normalize(need);
            if (!SUPPORTED_NEEDS.contains(token) || !matchesNeed(player, token, evidence)) {
                return false;
            }
        }
        if (places.isEmpty() && step.needs().isEmpty()) {
            String exact = stepEvidenceKey(chain.id(), step.number());
            return evidence.contains(exact) || hasEvidence(player, exact);
        }
        return true;
    }

    private static boolean matchesPlace(ServerPlayer player, String place, Evidence evidence) {
        return evidence.contains(place) || hasEvidence(player, place)
                || hasFlag(player, "mark_" + place) || hasFlag(player, "structure_marked_" + place)
                || hasFlag(player, "visited_" + place) || atRegion(player, place);
    }

    private static boolean matchesNeed(ServerPlayer player, String token, Evidence evidence) {
        return switch (token) {
            case "contribution>=quota_threshold" -> contribution(player) >= 25;
            case "window_soon" -> windowOpen(player, "blood_forbidden") || hasFlag(player, "window_soon");
            case "spirit_stones" -> hasItem(player, "low_spirit_stone")
                    || hasItem(player, "mid_spirit_stone") || hasItem(player, "high_spirit_stone");
            case "quest_cipher_or_intro_letter" -> hasItem(player, "inverse_star_cipher_token")
                    || hasFlag(player, "quest_cipher_or_intro_letter");
            case "contribution>=500" -> contribution(player) >= 500;
            case "risk_accept" -> hasFlag(player, "risk_accept") || evidence.contains("risk_accept");
            case "kunwu_clue_pieces>=n" -> countItem(player, "kunwu_seal_fragment") >= 3
                    || hasFlag(player, "kunwu_clues_done");
            case "fire_resist_ready" -> player.hasEffect(MobEffects.FIRE_RESISTANCE)
                    || hasFlag(player, "fire_resist_ready");
            case "detox_yin_resist" -> hasItem(player, "yin_body_protection_charm")
                    || player.getPersistentData().getInt(CatalogPillItem.PRESSURE_RESIST_TICKS_KEY) > 0
                    || hasFlag(player, "detox_yin_resist");
            default -> false;
        };
    }

    private static boolean rewardPreflight(JsonObject reward) {
        if (reward == null || !reward.has("item") || !reward.get("item").isJsonPrimitive()) {
            return true;
        }
        String itemId = reward.get("item").getAsString();
        return ItemCatalogService.resolveCatalogItem(itemId) != null;
    }

    private static void applyRewardOnce(ServerPlayer player, String ledgerKey, JsonObject reward) {
        CompoundTag ledger = player.getPersistentData().getCompound(REWARD_TAG).copy();
        String key = normalize(ledgerKey);
        if (key.isBlank() || ledger.getBoolean(key)) {
            return;
        }
        if (reward != null) {
            for (Map.Entry<String, JsonElement> entry : reward.entrySet()) {
                if ("item".equals(normalize(entry.getKey())) && !passesRewardChance(player, key, reward)) {
                    continue;
                }
                applyRewardField(player, normalize(entry.getKey()), entry.getValue());
            }
        }
        ledger.putBoolean(key, true);
        player.getPersistentData().put(REWARD_TAG, ledger);
    }

    private static void applyRewardField(ServerPlayer player, String key, JsonElement value) {
        if (key.startsWith("rep_") && value != null && value.isJsonPrimitive()
                && value.getAsJsonPrimitive().isNumber()) {
            String faction = ReputationUnlockService.reputationKey(key);
            ReputationService.add(player, faction == null || faction.isBlank()
                    ? key.substring("rep_".length()) : faction, value.getAsInt());
            return;
        }
        switch (key) {
            case "item" -> NamedNpcRewardService.grantCatalogItem(player, value.getAsString(), 1);
            case "flag", "unlock", "unlock_shop", "method", "method_fragment", "method_progress",
                    "technique_unlock_hint", "realm", "buff", "item_progress", "teleport", "next" ->
                    setRewardFlags(player, key, value);
            case "tech_hint", "possible", "unique" -> setRewardFlags(player, key, value);
            case "or_flag", "unique_item_possible", "item_possible" -> setRewardFlags(player, key, value);
            default -> {
                // Narrative, chance, loot-weight, numeric-band, and failure metadata remain preserved only.
            }
        }
    }

    static boolean deterministicChance(java.util.UUID playerId, String ledgerKey, double chance) {
        if (chance <= 0.0D) {
            return false;
        }
        if (chance >= 1.0D) {
            return true;
        }
        long seed = playerId.getMostSignificantBits() ^ Long.rotateLeft(playerId.getLeastSignificantBits(), 17)
                ^ normalize(ledgerKey).hashCode() * 0x9E3779B97F4A7C15L;
        double roll = Math.floorMod(seed, 1_000_000L) / 1_000_000.0D;
        return roll < chance;
    }

    private static boolean passesRewardChance(ServerPlayer player, String ledgerKey, JsonObject reward) {
        if (reward == null || !reward.has("chance") || !reward.get("chance").isJsonPrimitive()) {
            return true;
        }
        try {
            return deterministicChance(player.getUUID(), ledgerKey, reward.get("chance").getAsDouble());
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private static void setRewardFlags(ServerPlayer player, String kind, JsonElement value) {
        if (value == null || value.isJsonNull()) {
            return;
        }
        if (value.isJsonArray()) {
            for (JsonElement child : value.getAsJsonArray()) {
                if (child.isJsonPrimitive()) {
                    setRewardFlag(player, kind, child.getAsString());
                }
            }
        } else if (value.isJsonPrimitive()) {
            setRewardFlag(player, kind, value.getAsString());
        }
    }

    private static void setRewardFlag(ServerPlayer player, String kind, String value) {
        String token = normalize(value);
        if (token.isBlank() || "true".equals(token) || "false".equals(token)) {
            return;
        }
        NpcDialogueFlags.setFlag(player, token);
        NpcDialogueFlags.setFlag(player, "quest_reward_" + normalize(kind) + "_" + token);
    }

    private static int contributionCost(Chain chain, Step step) {
        if (step.needs().stream().map(DetailedQuestRuntimeService::normalize)
                .anyMatch("contribution>=500"::equals)) {
            return 500;
        }
        if ("huangfeng_blood_quota".equals(chain.id()) && step.number() == chain.steps().size()) {
            return 25;
        }
        return 0;
    }

    private static boolean spendContribution(ServerPlayer player, int amount) {
        return CultivationHelper.get(player)
                .map(c -> c.getSevenMysteriesQuest().spendContribution(amount)).orElse(false);
    }

    private static int contribution(ServerPlayer player) {
        return CultivationHelper.get(player)
                .map(c -> c.getSevenMysteriesQuest().getContribution()).orElse(0);
    }

    private static String sect(ServerPlayer player) {
        return CultivationHelper.get(player).map(c -> SectDefinitionService.canonicalizeSectId(
                c.getSevenMysteriesQuest().getSectId())).orElse("");
    }

    private static boolean atRegion(ServerPlayer player, String expected) {
        String token = normalize(expected);
        if (player == null || token.isBlank()) {
            return false;
        }
        String region = CultivationHelper.get(player)
                .map(c -> normalize(c.getWorldpackCurrentRegionId())).orElse("");
        String dimension = normalize(player.serverLevel().dimension().location().getPath());
        return region.equals(token) || region.contains(token) || token.contains(region) && !region.isBlank()
                || dimension.equals(token) || dimension.contains(token);
    }

    private static boolean windowOpen(ServerPlayer player, String realmId) {
        return SecretRealmCatalogService.find(realmId)
                .map(realm -> SecretRealmOpenPolicy.validate(player, realm).isEmpty()).orElse(false);
    }

    private static boolean hasFlag(ServerPlayer player, String flag) {
        return NpcDialogueFlags.hasFlag(player, normalize(flag));
    }

    private static boolean hasItem(ServerPlayer player, String itemId) {
        return countItem(player, itemId) > 0;
    }

    private static int countItem(ServerPlayer player, String itemId) {
        if (player == null) {
            return 0;
        }
        Item item = ItemCatalogService.resolveCatalogItem(itemId);
        if (item == null) {
            if ("yin_body_protection_charm".equals(normalize(itemId))) {
                item = ModItems.YIN_BODY_PROTECTION_CHARM.get();
            } else {
                return 0;
            }
        }
        int total = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.is(item)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    private static boolean noDetailedProgress(ServerPlayer player) {
        return player.getPersistentData().getCompound(ROOT_TAG).isEmpty();
    }

    private static Evidence mergeEvidence(Evidence evidence, String token) {
        LinkedHashSet<String> merged = new LinkedHashSet<>();
        if (evidence != null) {
            merged.addAll(evidence.tokens());
        }
        merged.add(token);
        return new Evidence(merged);
    }

    private static String stepEvidenceKey(String chainId, int step) {
        return "quest_step_" + normalize(chainId) + "_" + Math.max(1, step);
    }

    private static void recordEvidence(ServerPlayer player, String token) {
        CompoundTag evidence = player.getPersistentData().getCompound(EVIDENCE_TAG).copy();
        if (!evidence.contains(token) && evidence.getAllKeys().size() >= MAX_EVIDENCE) {
            evidence.getAllKeys().stream().sorted().findFirst().ifPresent(evidence::remove);
        }
        evidence.putBoolean(token, true);
        player.getPersistentData().put(EVIDENCE_TAG, evidence);
    }

    private static boolean hasEvidence(ServerPlayer player, String token) {
        return player != null && player.getPersistentData().getCompound(EVIDENCE_TAG)
                .getBoolean(normalize(token));
    }

    private static Set<String> buildKnownEvidence() {
        LinkedHashSet<String> tokens = new LinkedHashSet<>();
        for (Chain chain : BUILTIN.chains().values()) {
            tokens.add(chain.id());
            tokens.add(chain.giverNpc());
            tokens.add(chain.region());
            tokens.addAll(chain.prerequisites().stream().map(DetailedQuestRuntimeService::normalize).toList());
            for (Step step : chain.steps()) {
                tokens.addAll(placeTokens(step.place()));
                tokens.addAll(step.needs().stream().map(DetailedQuestRuntimeService::normalize).toList());
                tokens.add(stepEvidenceKey(chain.id(), step.number()));
            }
        }
        tokens.remove("");
        return Set.copyOf(tokens);
    }

    private static Snapshot loadBuiltin() {
        Map<String, Chain> chains = new LinkedHashMap<>();
        LinkedHashSet<String> unsupportedPrerequisites = new LinkedHashSet<>();
        LinkedHashSet<String> unsupportedNeeds = new LinkedHashSet<>();
        int totalSteps = 0;
        JsonObject root = readJson("data/" + SeekingImmortalsMod.MODID
                + "/text_material/quest_chains_playable_v141.json");
        for (JsonElement element : array(root, "chains")) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject object = element.getAsJsonObject();
            String id = normalize(string(object, "id"));
            if (id.isBlank()) {
                continue;
            }
            List<String> prerequisites = stringList(object.get("prereq"));
            for (String prerequisite : prerequisites) {
                if (!prereqTokenSupported(prerequisite)) {
                    unsupportedPrerequisites.add(id + ":" + prerequisite);
                }
            }
            List<Step> steps = new ArrayList<>();
            for (JsonElement stepElement : array(object, "steps")) {
                if (!stepElement.isJsonObject()) {
                    continue;
                }
                JsonObject step = stepElement.getAsJsonObject();
                List<String> needs = stringList(step.get("need"));
                for (String need : needs) {
                    if (!needTokenSupported(need)) {
                        unsupportedNeeds.add(id + ":" + need);
                    }
                }
                steps.add(new Step(integer(step, "step", steps.size() + 1), string(step, "summary"),
                        string(step, "do"), string(step, "place"), needs, string(step, "fail"),
                        object(step, "reward")));
            }
            Chain chain = new Chain(id, string(object, "display"), string(object, "region"),
                    stringList(object.get("realm_span")), string(object, "giver_npc"), prerequisites,
                    steps, object(object, "rewards_finale"), stringList(object.get("next_chains")));
            chains.put(id, chain);
            totalSteps += steps.size();
        }
        return new Snapshot(chains, totalSteps, unsupportedPrerequisites, unsupportedNeeds);
    }

    private static Map<String, Integer> sourceStepCounts(Snapshot snapshot) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        if (snapshot != null) {
            for (Chain chain : snapshot.chains().values()) {
                counts.put(chain.id(), chain.steps().size());
            }
        }
        return counts;
    }

    private static JsonObject readJson(String path) {
        try (InputStream stream = DetailedQuestRuntimeService.class.getClassLoader().getResourceAsStream(path)) {
            if (stream == null) {
                return null;
            }
            try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                return JsonParser.parseReader(reader).getAsJsonObject();
            }
        } catch (Exception exception) {
            SeekingImmortalsMod.LOGGER.error("Failed to load detailed quest runtime {}", path, exception);
            return null;
        }
    }

    private static JsonArray array(JsonObject object, String key) {
        return object != null && object.has(key) && object.get(key).isJsonArray()
                ? object.getAsJsonArray(key) : new JsonArray();
    }

    private static JsonObject object(JsonObject object, String key) {
        return object != null && object.has(key) && object.get(key).isJsonObject()
                ? object.getAsJsonObject(key).deepCopy() : new JsonObject();
    }

    private static String string(JsonObject object, String key) {
        if (object == null || !object.has(key) || object.get(key).isJsonNull()) {
            return "";
        }
        try {
            return object.get(key).getAsString();
        } catch (Exception ignored) {
            return "";
        }
    }

    private static int integer(JsonObject object, String key, int fallback) {
        try {
            return object.get(key).getAsInt();
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static List<String> stringList(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) {
                if (child.isJsonPrimitive()) {
                    values.add(child.getAsString());
                }
            }
        } else if (element.isJsonPrimitive()) {
            values.add(element.getAsString());
        }
        return List.copyOf(values);
    }

    private static void copyTag(CompoundTag source, CompoundTag target, String key) {
        if (source.contains(key) && source.get(key) != null) {
            target.put(key, source.get(key).copy());
        }
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
