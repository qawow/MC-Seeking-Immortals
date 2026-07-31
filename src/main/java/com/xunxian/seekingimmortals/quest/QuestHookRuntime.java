package com.xunxian.seekingimmortals.quest;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.xunxian.seekingimmortals.SeekingImmortalsMod;
import com.xunxian.seekingimmortals.cultivation.CultivationHelper;
import com.xunxian.seekingimmortals.cultivation.Realm;
import com.xunxian.seekingimmortals.cultivation.RealmStage;
import com.xunxian.seekingimmortals.npc.DialogueBranchService;
import com.xunxian.seekingimmortals.npc.DialogueNodeReachedEvent;
import com.xunxian.seekingimmortals.npc.NpcDialogueFlags;
import com.xunxian.seekingimmortals.region.DailyEventScheduler;
import com.xunxian.seekingimmortals.worldpack.DailyEventEffectCatalog;
import com.xunxian.seekingimmortals.worldpack.DailyEventEffectExecutor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

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

/**
 * M11 quest hook runtime: wires step hooks + dialogue effects + M06 daily events +
 * M09 secret-realm clears + kill/gather/craft bus into text-chain advances.
 * Safe when FTB is absent (no FTB imports).
 */
public final class QuestHookRuntime {
    private static final String DAILY_ROOT = "seeking_immortals_daily_quest_hooks";
    private static final String DAILY_CLAIMS = "Claims";
    private static final int MAX_DAILY_CLAIMS = 64;
    private static final Map<String, List<String>> HOOK_TO_CHAINS = loadHookToChains();
    private static final Map<String, List<String>> EFFECT_TO_QUESTS = loadEffectQuestLinks();
    private static final Map<String, List<String>> STEP_HOOKS_BY_CHAIN = loadStepHooksByChain();
    private static final Map<String, List<String>> REGION_TO_DETAILED_CHAINS = Map.ofEntries(
            Map.entry("qinglan_mountains", List.of("mortal_qixuan_entry")),
            Map.entry("qixuan_village", List.of("mortal_qixuan_entry")),
            Map.entry("tiannan", List.of("mortal_qixuan_entry")),
            Map.entry("chaotic_sea", List.of("xutian_window_prepare")),
            Map.entry("dajin", List.of("kunwu_clue_assemble", "yinyang_ku_intel")),
            Map.entry("great_jin_central", List.of("kunwu_clue_assemble", "yinyang_ku_intel")),
            Map.entry("kunwu", List.of("kunwu_clue_assemble")),
            Map.entry("fallen_demon_valley", List.of("zhuimo_token")),
            Map.entry("extreme_west", List.of("qianzhu_tower_trial")),
            Map.entry("extreme_west_thousand_bamboo", List.of("qianzhu_tower_trial")),
            Map.entry("tianyuan", List.of("tianyuan_landing_register")));
    private static final Map<String, List<String>> REALM_TO_DETAILED_CHAINS = Map.ofEntries(
            Map.entry("blood_forbidden", List.of("blood_forbidden_run", "nangong_wan_weight_optional")),
            Map.entry("void_palace", List.of("xutian_window_prepare")),
            Map.entry("kunwu_mountain", List.of("kunwu_clue_assemble")),
            Map.entry("fallen_demon_valley", List.of("zhuimo_token", "lingzhu_fruit_run")),
            Map.entry("yinyang_ku", List.of("yinyang_ku_intel", "peiying_material_hunt")),
            Map.entry("thousand_bamboo_puppet_tower", List.of("qianzhu_tower_trial")),
            Map.entry("guanghan_realm", List.of("guanghan_endgame_path")));
    private static boolean registered;

    private QuestHookRuntime() {}

    public static void register() {
        if (registered) {
            return;
        }
        try {
            MinecraftForge.EVENT_BUS.register(QuestHookRuntime.class);
        } catch (Throwable ignored) {
            // unit tests without bus
        }
        try {
            DailyEventScheduler.registerHook(QuestHookRuntime::onDailyEvent);
        } catch (Throwable ignored) {
            // region package may be unavailable in pure unit tests
        }
        registered = true;
        SeekingImmortalsMod.LOGGER.info("Registered M11 QuestHookRuntime (hooks={}, effects={}, chains_with_steps={})",
                HOOK_TO_CHAINS.size(), EFFECT_TO_QUESTS.size(), STEP_HOOKS_BY_CHAIN.size());
    }

    public static int hookMappingCount() {
        return HOOK_TO_CHAINS.size();
    }

    public static int effectLinkCount() {
        return EFFECT_TO_QUESTS.size();
    }

    public static List<String> chainsForHook(String hookId) {
        return HOOK_TO_CHAINS.getOrDefault(normalize(hookId), List.of());
    }

    public static List<String> questsForEffect(String effectKey) {
        return EFFECT_TO_QUESTS.getOrDefault(normalize(effectKey), List.of());
    }

    static List<String> detailedChainsForRegion(String regionId) {
        return REGION_TO_DETAILED_CHAINS.getOrDefault(normalize(regionId), List.of());
    }

    static List<String> detailedChainsForSecretRealm(String realmId) {
        return REALM_TO_DETAILED_CHAINS.getOrDefault(normalize(realmId), List.of());
    }

    /** Called only after a server-authoritative region transition or resolved login location. */
    public static void onRegionReached(ServerPlayer player, String regionId) {
        if (player == null) {
            return;
        }
        String region = normalize(regionId);
        startDetailedChains(player, detailedChainsForRegion(region),
                DetailedQuestRuntimeService.Evidence.of(region));
        CultivationHelper.get(player).ifPresent(cultivation -> {
            if (isHighRealmPathEligible(cultivation.getRealm(), cultivation.getStage())) {
                startDetailedChains(player, List.of("deity_huoyu_path"),
                        DetailedQuestRuntimeService.Evidence.of(region, cultivation.getRealm().getDesignId()));
            }
        });
    }

    /** Called after the secret-realm session exists; entry evidence is never accepted before teleport success. */
    public static void onSecretRealmEnter(ServerPlayer player, String realmId) {
        if (player == null) {
            return;
        }
        String realm = normalize(realmId);
        List<String> chains = detailedChainsForSecretRealm(realm);
        DetailedQuestRuntimeService.Evidence evidence = DetailedQuestRuntimeService.Evidence.of(realm);
        startDetailedChains(player, chains, evidence);
        DetailedQuestProofService.recordSecretRealmEntry(player, realm);
    }

    static boolean isHighRealmPathEligible(Realm realm, RealmStage stage) {
        if (realm == null) {
            return false;
        }
        return realm == Realm.SOUL_TRANSFORMATION
                || realm == Realm.NASCENT_SOUL && (stage == RealmStage.LATE || stage == RealmStage.PEAK);
    }

    private static void startDetailedChains(ServerPlayer player, List<String> chainIds,
                                            DetailedQuestRuntimeService.Evidence evidence) {
        for (String chainId : chainIds) {
            if (DetailedQuestRuntimeService.canStart(player, chainId, evidence)) {
                DetailedQuestRuntimeService.start(player, chainId, evidence);
            }
        }
    }

    /** M06 daily event subscription. */
    public static void onDailyEvent(String regionId, String eventId) {
        // Daily events do not have a single player context; mark soft region flag only.
        // Player-scoped accept still goes through QuestHookSoftService / commands.
        String hook = normalize(eventId);
        if (hook.isBlank()) {
            return;
        }
        // No-op without player; player path is onPlayerDaily below if needed.
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        CultivationHelper.get(player).ifPresent(cultivation ->
                onRegionReached(player, cultivation.getWorldpackCurrentRegionId()));
    }

    /** Player-scoped authored daily hook with a per-roll idempotency ledger. */
    public static boolean onPlayerDailyEvent(ServerPlayer player, String regionId,
                                             DailyEventEffectCatalog.Event event, long untilTick) {
        if (player == null || event == null || untilTick <= player.level().getGameTime()
                || !event.matchesRegion(regionId) || !DailyEventEffectExecutor.isRealmAllowed(player, event)) {
            return false;
        }
        LinkedHashSet<String> hooks = new LinkedHashSet<>();
        if (!event.questHook().isBlank()) {
            hooks.add(event.questHook());
        }
        if (!event.factionTrigger().isBlank()) {
            hooks.add(event.factionTrigger());
        }
        if (hooks.isEmpty()) {
            return false;
        }

        long now = player.level().getGameTime();
        String claim = dailyClaimKey(regionId, event.id(), untilTick);
        CompoundTag root = player.getPersistentData().getCompound(DAILY_ROOT).copy();
        List<String> claims = readDailyClaims(root, now);
        if (claims.contains(claim)) {
            return false;
        }

        boolean handled = false;
        for (String rawHook : hooks) {
            String hook = normalize(rawHook);
            if (hook.isBlank()) {
                continue;
            }
            LinkedHashSet<String> chains = new LinkedHashSet<>(
                    HOOK_TO_CHAINS.getOrDefault(hook, List.of()));
            QuestHookSoftService.mappedChainId(hook).ifPresent(chains::add);
            for (String chainId : chains) {
                TextQuestChainService.ChainProgress progress = TextQuestChainService.progressOf(player, chainId);
                if (progress.stage() <= 0) {
                    handled |= TextQuestChainService.start(player, chainId);
                } else if (!progress.complete()
                        && TextQuestChainService.matchesCurrentStepHook(player, chainId, hook)) {
                    handled |= TextQuestChainService.advance(player, chainId);
                }
            }
            NpcDialogueFlags.setFlag(player, "hook_" + hook);
            handled = true;
        }

        claims.remove(claim);
        claims.add(claim);
        writeDailyClaims(root, claims);
        player.getPersistentData().put(DAILY_ROOT, root);
        return handled;
    }

    public static void copyPersistentData(CompoundTag source, CompoundTag target) {
        if (source == null || target == null) {
            return;
        }
        Tag stored = source.get(DAILY_ROOT);
        if (stored != null) {
            target.put(DAILY_ROOT, stored.copy());
        }
    }

    static String dailyClaimKey(String regionId, String eventId, long untilTick) {
        return normalize(regionId) + "|" + normalize(eventId) + "|" + Math.max(0L, untilTick);
    }

    private static List<String> readDailyClaims(CompoundTag root, long now) {
        LinkedHashSet<String> claims = new LinkedHashSet<>();
        ListTag stored = root.getList(DAILY_CLAIMS, Tag.TAG_STRING);
        for (int i = 0; i < stored.size(); i++) {
            String claim = stored.getString(i);
            int split = claim.lastIndexOf('|');
            if (split <= 0 || split >= claim.length() - 1) {
                continue;
            }
            try {
                if (Long.parseLong(claim.substring(split + 1)) > now) {
                    claims.add(claim);
                }
            } catch (NumberFormatException ignored) {
                // Discard malformed entries.
            }
        }
        List<String> ordered = new ArrayList<>(claims);
        int from = Math.max(0, ordered.size() - MAX_DAILY_CLAIMS);
        return new ArrayList<>(ordered.subList(from, ordered.size()));
    }

    private static void writeDailyClaims(CompoundTag root, List<String> claims) {
        ListTag stored = new ListTag();
        int from = Math.max(0, claims.size() - MAX_DAILY_CLAIMS);
        for (int i = from; i < claims.size(); i++) {
            stored.add(StringTag.valueOf(claims.get(i)));
        }
        root.put(DAILY_CLAIMS, stored);
    }

    @SubscribeEvent
    public static void onDialogueNode(DialogueNodeReachedEvent event) {
        if (event == null || event.getPlayer() == null) {
            return;
        }
        ServerPlayer player = event.getPlayer();
        String nodeId = normalize(event.getNodeId());
        String treeId = normalize(event.getTreeId());
        // Q-B-5: structured dialogue proofs from the server dialogue session.
        DetailedQuestProofService.recordDialogueNode(player, event.getNpcId(), treeId, nodeId);
        // 1) Node id as hook / chain id.
        tryAdvanceByHook(player, nodeId);
        tryStartOrAdvanceChain(player, nodeId);
        // 2) Tree-scoped composite keys.
        if (!treeId.isBlank() && !nodeId.isBlank()) {
            tryAdvanceByHook(player, treeId + "_" + nodeId);
            tryAdvanceByHook(player, treeId + ":" + nodeId);
        }
        // 3) Node effects: offer_quest / open_quest / turnin_quests with q / quest_ids.
        Optional<DialogueBranchService.Node> node = DialogueBranchService.node(event.getTreeId(), event.getNodeId());
        if (node.isEmpty()) {
            return;
        }
        for (DialogueBranchService.Effect effect : node.get().effects()) {
            handleDialogueEffect(player, event.getNpcId(), event.getTreeId(), event.getNodeId(), effect);
        }
    }

    @SubscribeEvent(receiveCanceled = true)
    public static void onLivingDrops(LivingDropsEvent event) {
        if (event == null || event.getEntity() == null || event.getEntity().level().isClientSide) {
            return;
        }
        if (!(event.getSource().getEntity() instanceof ServerPlayer killer)) {
            return;
        }
        try {
            // Secret-realm mid/core clear is handled in ModEvents; here we only do generic kill hooks.
            String typeId = "monster";
            var key = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(event.getEntity().getType());
            if (key != null) {
                typeId = key.getPath();
            }
            DetailedQuestProofService.recordEntityKilled(killer, typeId);
            tryAdvanceByHook(killer, "kill_" + normalize(typeId));
            tryAdvanceByHook(killer, "slay_" + normalize(typeId));
            if (typeId.contains("beast") || typeId.contains("wolf") || typeId.contains("spider")) {
                tryAdvanceByHook(killer, "slay_beast_bounty");
            }
        } catch (RuntimeException exception) {
            SeekingImmortalsMod.LOGGER.error("Failed to apply committed quest kill hooks", exception);
        }
    }

    @SubscribeEvent
    public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        if (event == null || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        ItemStack stack = event.getCrafting();
        if (stack == null || stack.isEmpty()) {
            return;
        }
        String itemId = itemPath(stack);
        if (itemId.isBlank()) {
            return;
        }
        tryAdvanceByHook(player, "craft_" + itemId);
        tryAdvanceByHook(player, "refine_" + itemId);
        DetailedQuestProofService.recordItemCrafted(player, itemId);
        if (itemId.contains("pill") || itemId.contains("dan")) {
            tryAdvanceByHook(player, "alchemy_apprentice");
            tryAdvanceByHook(player, "alchemy_loop");
        }
        if (itemId.contains("talisman") || itemId.contains("fu")) {
            tryAdvanceByHook(player, "talisman_craft");
        }
    }

    @SubscribeEvent
    public static void onItemPickup(PlayerEvent.ItemPickupEvent event) {
        if (event == null || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        ItemStack stack = event.getStack();
        if (stack == null || stack.isEmpty()) {
            return;
        }
        String itemId = itemPath(stack);
        if (itemId.isBlank()) {
            return;
        }
        tryAdvanceByHook(player, "gather_" + itemId);
        tryAdvanceByHook(player, "collect_" + itemId);
        DetailedQuestProofService.recordItemAcquired(player, itemId);
        if (itemId.contains("herb") || itemId.contains("grass") || itemId.contains("spirit_grass")) {
            tryAdvanceByHook(player, "gather_spirit_herb");
        }
    }

    /** Called from ModEvents after secret-realm mid/core clear. */
    public static void onSecretRealmClear(ServerPlayer player, String realmId, String layer) {
        if (player == null) {
            return;
        }
        String realm = normalize(realmId);
        String layerKey = normalize(layer);
        tryAdvanceByHook(player, "realm_" + realm);
        tryAdvanceByHook(player, "secret_" + realm);
        tryAdvanceByHook(player, realm + "_" + layerKey);
        tryAdvanceByHook(player, "trial_" + layerKey);
        if ("blood_forbidden".equals(realm) && "mid".equals(layerKey)) {
            DetailedQuestRuntimeService.Evidence waterEvidence =
                    DetailedQuestRuntimeService.Evidence.of("bf_water_jiao");
            startDetailedChains(player, List.of("nangong_wan_weight_optional"), waterEvidence);
        }
        DetailedQuestProofService.recordSecretRealmLayer(player, realm, layerKey);
        DetailedQuestProofService.recordEncounterCleared(player, realm, layerKey);
        if ("core".equals(layerKey) || "boss".equals(layerKey)) {
            tryAdvanceByHook(player, "secret_realm_clear");
            tryStartOrAdvanceChain(player, realm.contains("blood") ? "blood_forbidden_campaign" : "");
        }
    }

    private static void handleDialogueEffect(ServerPlayer player, String npcId, String treeId, String nodeId,
                                             DialogueBranchService.Effect effect) {
        if (effect == null) {
            return;
        }
        String type = normalize(effect.type());
        if (type.isBlank()) {
            return;
        }
        List<String> questIds = resolveQuestIds(effect);
        String destination = effectDestination(type, effect);
        DetailedQuestRuntimeService.Evidence evidence = DetailedQuestRuntimeService.Evidence.of(
                npcId, treeId, nodeId, destination, type + ":" + destination);

        switch (type) {
            case "offer_quest", "open_quest", "open_quest_board" -> {
                for (String questId : questIds) {
                    if (DetailedQuestRuntimeService.find(questId).isPresent()) {
                        DetailedQuestRuntimeService.start(player, questId, evidence);
                    } else {
                        tryStartChain(player, questId);
                    }
                    NpcDialogueFlags.setFlag(player, "quest_offered_" + normalize(questId));
                }
            }
            case "turnin_quests" -> {
                // Structured delivery proofs first: the player must really hold the delivered
                // item and hand it to the giver/place npc.
                DetailedQuestProofService.recordItemDelivered(player, npcId);
                for (String questId : questIds) {
                    if (DetailedQuestRuntimeService.find(questId).isEmpty()) {
                        tryAdvanceActive(player, questId);
                    }
                }
                DetailedQuestRuntimeService.turnIn(player, questIds, npcId, evidence);
                advanceSingleCanonicalForNpc(player, questIds, npcId);
            }
            default -> {
                for (String questId : questIds) {
                    if (DetailedQuestRuntimeService.find(questId).isPresent()) {
                        boolean active = DetailedQuestRuntimeService.progressOf(player, questId).started();
                        if (!active) {
                            DetailedQuestRuntimeService.start(player, questId, evidence);
                        } else {
                            DetailedQuestRuntimeService.advance(player, questId, evidence);
                        }
                    } else {
                        tryAdvanceActive(player, questId);
                    }
                }
            }
        }
    }

    static List<String> resolveQuestIds(DialogueBranchService.Effect effect) {
        if (effect == null) {
            return List.of();
        }
        String type = normalize(effect.type());
        if (type.isBlank()) {
            return List.of();
        }
        LinkedHashSet<String> questIds = new LinkedHashSet<>();
        addQuestId(questIds, firstNonBlank(
                effect.param("q"), effect.param("quest"), effect.param("quest_id")));
        for (String questId : effect.paramList("quest_ids")) {
            addQuestId(questIds, questId);
        }
        String destination = effectDestination(type, effect);
        String exactKey = destination.isBlank() ? "" : type + ":" + normalize(destination);
        List<String> linked;
        if (!exactKey.isBlank() && EFFECT_TO_QUESTS.containsKey(exactKey)) {
            linked = EFFECT_TO_QUESTS.get(exactKey);
        } else {
            linked = EFFECT_TO_QUESTS.getOrDefault(type, List.of());
        }
        for (String questId : linked) {
            addQuestId(questIds, questId);
        }
        return List.copyOf(questIds);
    }

    private static String effectDestination(String type, DialogueBranchService.Effect effect) {
        return switch (type) {
            case "offer_quest", "open_quest", "turnin_quests" -> firstNonBlank(
                    effect.param("q"), effect.param("quest"), effect.param("quest_id"), effect.param("id"));
            case "open_quest_board" -> firstNonBlank(
                    effect.param("board"), effect.param("q"), effect.param("quest"), effect.param("id"));
            case "enter_instance" -> firstNonBlank(
                    effect.param("instance"), effect.param("realm"), effect.param("id"));
            case "open_shop" -> firstNonBlank(effect.param("shop"), effect.param("shop_id"), effect.param("id"));
            case "grant_item" -> firstNonBlank(effect.param("item"), effect.param("item_id"), effect.param("id"));
            case "teleport", "start_teleport", "start_travel" -> firstNonBlank(
                    effect.param("route"), effect.param("to"), effect.param("region"), effect.param("target"));
            case "mark_structure" -> firstNonBlank(effect.param("structure"), effect.param("id"));
            case "set_flag", "unlock" -> firstNonBlank(
                    effect.param("flag"), effect.param("id"), effect.param("token"),
                    effect.paramList("gates").stream().findFirst().orElse(""));
            default -> firstNonBlank(effect.param("id"), effect.param("target"));
        };
    }

    private static void addQuestId(LinkedHashSet<String> questIds, String questId) {
        String normalized = normalize(questId);
        if (!normalized.isBlank()) {
            questIds.add(normalized);
        }
    }

    private static void advanceSingleCanonicalForNpc(ServerPlayer player, List<String> explicitIds, String npcId) {
        if (explicitIds != null && !explicitIds.isEmpty()) {
            return;
        }
        String npc = normalize(npcId);
        if (npc.isBlank()) {
            return;
        }
        List<String> matches = new ArrayList<>();
        for (TextQuestChainService.ChainProgress progress : TextQuestChainService.listProgress(player)) {
            if (progress.stage() > 0 && !progress.complete()
                    && npc.equals(normalize(TextQuestChainService.getNpc(player, progress.id())))) {
                matches.add(progress.id());
            }
        }
        if (matches.size() == 1) {
            tryAdvanceActive(player, matches.get(0));
        }
    }

    private static void tryAdvanceByHook(ServerPlayer player, String hookId) {
        String hook = normalize(hookId);
        if (hook.isBlank()) {
            return;
        }
        List<String> chains = HOOK_TO_CHAINS.getOrDefault(hook, List.of());
        if (chains.isEmpty()) {
            // Fallback: if hook equals a known chain step-hook list membership.
            for (Map.Entry<String, List<String>> e : STEP_HOOKS_BY_CHAIN.entrySet()) {
                if (e.getValue().contains(hook)) {
                    chains = List.of(e.getKey());
                    break;
                }
            }
        }
        for (String chainId : chains) {
            tryAdvanceActive(player, chainId, hook);
        }
        DetailedQuestRuntimeService.recordAndAdvance(player, hook);
        // Soft flag so dialogue conditions / future UI can see the hook fire.
        NpcDialogueFlags.setFlag(player, "hook_" + hook);
    }

    private static void tryAdvanceActive(ServerPlayer player, String chainId) {
        tryAdvanceActive(player, chainId, "");
    }

    private static void tryAdvanceActive(ServerPlayer player, String chainId, String hookId) {
        String id = normalize(chainId);
        if (id.isBlank()) {
            return;
        }
        if (TextQuestChainService.find(id).isEmpty()) {
            // Not a catalog chain — keep soft flag only.
            NpcDialogueFlags.setFlag(player, "quest_progress_" + id);
            return;
        }
        TextQuestChainService.ChainProgress progress = TextQuestChainService.progressOf(player, id);
        if (progress.stage() <= 0) {
            return;
        }
        if (progress.complete()) {
            return;
        }
        // Only the authored current-step hook may advance; empty expected hooks stay open for dialogue.
        if (!hookId.isBlank() && !TextQuestChainService.matchesCurrentStepHook(player, id, hookId)) {
            return;
        }
        TextQuestChainService.advance(player, id);
    }

    private static void tryStartOrAdvanceChain(ServerPlayer player, String chainId) {
        String id = normalize(chainId);
        if (id.isBlank()) {
            return;
        }
        if (TextQuestChainService.find(id).isEmpty()) {
            NpcDialogueFlags.setFlag(player, "quest_soft_" + id);
            return;
        }
        TextQuestChainService.ChainProgress progress = TextQuestChainService.progressOf(player, id);
        if (progress.stage() <= 0) {
            TextQuestChainService.start(player, id);
        } else if (!progress.complete()) {
            TextQuestChainService.advance(player, id);
        }
    }

    private static void tryStartChain(ServerPlayer player, String chainId) {
        String id = normalize(chainId);
        if (id.isBlank() || TextQuestChainService.find(id).isEmpty()) {
            return;
        }
        if (TextQuestChainService.progressOf(player, id).stage() <= 0) {
            TextQuestChainService.start(player, id);
        }
    }

    private static Map<String, List<String>> loadHookToChains() {
        Map<String, List<String>> map = new LinkedHashMap<>();
        // From quest_chains steps[].hook
        JsonObject questRoot = readJson(path("text_material/quest_chains.json"));
        for (JsonElement element : array(questRoot, "chains")) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject chain = element.getAsJsonObject();
            String chainId = str(chain, "id");
            if (chainId.isBlank()) {
                continue;
            }
            JsonElement stepsEl = chain.get("steps");
            if (stepsEl == null || !stepsEl.isJsonArray()) {
                continue;
            }
            for (JsonElement stepEl : stepsEl.getAsJsonArray()) {
                if (!stepEl.isJsonObject()) {
                    continue;
                }
                String hook = normalize(str(stepEl.getAsJsonObject(), "hook"));
                if (hook.isBlank()) {
                    continue;
                }
                map.computeIfAbsent(hook, k -> new ArrayList<>());
                if (!map.get(hook).contains(chainId)) {
                    map.get(hook).add(chainId);
                }
            }
        }
        // Soft service explicit map is consulted at runtime via QuestHookSoftService.mappedChainId as well.
        // Freeze lists.
        Map<String, List<String>> frozen = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> e : map.entrySet()) {
            frozen.put(e.getKey(), List.copyOf(e.getValue()));
        }
        return Collections.unmodifiableMap(frozen);
    }

    private static Map<String, List<String>> loadStepHooksByChain() {
        Map<String, List<String>> map = new LinkedHashMap<>();
        JsonObject questRoot = readJson(path("text_material/quest_chains.json"));
        for (JsonElement element : array(questRoot, "chains")) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject chain = element.getAsJsonObject();
            String chainId = str(chain, "id");
            if (chainId.isBlank()) {
                continue;
            }
            List<String> hooks = new ArrayList<>();
            JsonElement stepsEl = chain.get("steps");
            if (stepsEl != null && stepsEl.isJsonArray()) {
                for (JsonElement stepEl : stepsEl.getAsJsonArray()) {
                    if (!stepEl.isJsonObject()) {
                        continue;
                    }
                    String hook = normalize(str(stepEl.getAsJsonObject(), "hook"));
                    if (!hook.isBlank()) {
                        hooks.add(hook);
                    }
                }
            }
            map.put(chainId, List.copyOf(hooks));
        }
        // Also use catalog index step_hooks if present.
        JsonObject index = readJson(path("catalog/quest_chains_index.json"));
        for (JsonElement element : array(index, "chains")) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject o = element.getAsJsonObject();
            String id = str(o, "id");
            if (id.isBlank() || map.containsKey(id)) {
                continue;
            }
            List<String> hooks = new ArrayList<>();
            for (String h : stringList(o.get("step_hooks"))) {
                hooks.add(normalize(h));
            }
            map.put(id, List.copyOf(hooks));
        }
        return Collections.unmodifiableMap(map);
    }

    private static Map<String, List<String>> loadEffectQuestLinks() {
        Map<String, List<String>> map = new LinkedHashMap<>();
        JsonObject root = readJson(path("catalog/dialogue_effect_quest_links_index.json"));
        if (root == null) {
            root = readJson(path("text_material/dialogue_effect_quest_links_v140.json"));
        }
        JsonArray links = array(root, "links");
        for (JsonElement element : links) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject o = element.getAsJsonObject();
            String effect = normalize(str(o, "effect"));
            if (effect.isBlank()) {
                continue;
            }
            List<String> quests = new ArrayList<>(stringList(o.get("quest_ids")));
            LinkedHashSet<String> merged = new LinkedHashSet<>(map.getOrDefault(effect, List.of()));
            for (String quest : quests) {
                String normalized = normalize(quest);
                if (!normalized.isBlank()) {
                    merged.add(normalized);
                }
            }
            map.put(effect, List.copyOf(merged));
        }
        return Collections.unmodifiableMap(map);
    }

    private static String itemPath(ItemStack stack) {
        try {
            var key = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem());
            return key == null ? "" : normalize(key.getPath());
        } catch (Throwable ignored) {
            return "";
        }
    }

    private static String path(String relative) {
        return "data/" + SeekingImmortalsMod.MODID + "/" + relative;
    }

    private static JsonObject readJson(String path) {
        try (InputStream stream = QuestHookRuntime.class.getClassLoader().getResourceAsStream(path)) {
            if (stream == null) {
                return null;
            }
            try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                return JsonParser.parseReader(reader).getAsJsonObject();
            }
        } catch (Exception ignored) {
            return null;
        }
    }

    private static JsonArray array(JsonObject root, String key) {
        if (root == null || !root.has(key) || !root.get(key).isJsonArray()) {
            return new JsonArray();
        }
        return root.getAsJsonArray(key);
    }

    private static String str(JsonObject object, String key) {
        if (object == null || !object.has(key) || object.get(key).isJsonNull()) {
            return "";
        }
        try {
            return object.get(key).getAsString();
        } catch (Exception ignored) {
            return String.valueOf(object.get(key));
        }
    }

    private static List<String> stringList(JsonElement element) {
        if (element == null) {
            return List.of();
        }
        if (element.isJsonPrimitive()) {
            return List.of(element.getAsString());
        }
        if (!element.isJsonArray()) {
            return List.of();
        }
        List<String> list = new ArrayList<>();
        for (JsonElement child : element.getAsJsonArray()) {
            try {
                list.add(child.getAsString());
            } catch (Exception ignored) {
                list.add(String.valueOf(child));
            }
        }
        return List.copyOf(list);
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private static String normalize(String id) {
        return id == null ? "" : id.trim().toLowerCase(Locale.ROOT);
    }
}
