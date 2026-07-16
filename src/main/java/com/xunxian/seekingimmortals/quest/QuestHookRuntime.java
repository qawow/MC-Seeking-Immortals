package com.xunxian.seekingimmortals.quest;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.xunxian.seekingimmortals.SeekingImmortalsMod;
import com.xunxian.seekingimmortals.npc.DialogueBranchService;
import com.xunxian.seekingimmortals.npc.DialogueNodeReachedEvent;
import com.xunxian.seekingimmortals.npc.NpcDialogueFlags;
import com.xunxian.seekingimmortals.region.DailyEventScheduler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
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
    private static final Map<String, List<String>> HOOK_TO_CHAINS = loadHookToChains();
    private static final Map<String, List<String>> EFFECT_TO_QUESTS = loadEffectQuestLinks();
    private static final Map<String, List<String>> STEP_HOOKS_BY_CHAIN = loadStepHooksByChain();
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
    public static void onDialogueNode(DialogueNodeReachedEvent event) {
        if (event == null || event.getPlayer() == null) {
            return;
        }
        ServerPlayer player = event.getPlayer();
        String nodeId = normalize(event.getNodeId());
        String treeId = normalize(event.getTreeId());
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
            handleDialogueEffect(player, effect);
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event == null || event.getEntity() == null || event.getEntity().level().isClientSide) {
            return;
        }
        if (!(event.getSource().getEntity() instanceof ServerPlayer killer)) {
            return;
        }
        // Secret-realm mid/core clear is handled in ModEvents; here we only do generic kill hooks.
        String typeId = "monster";
        try {
            var key = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(event.getEntity().getType());
            if (key != null) {
                typeId = key.getPath();
            }
        } catch (Throwable ignored) {
            // registry may be absent in pure unit tests
        }
        tryAdvanceByHook(killer, "kill_" + normalize(typeId));
        tryAdvanceByHook(killer, "slay_" + normalize(typeId));
        if (typeId.contains("beast") || typeId.contains("wolf") || typeId.contains("spider")) {
            tryAdvanceByHook(killer, "slay_beast_bounty");
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
        if ("core".equals(layerKey) || "boss".equals(layerKey)) {
            tryAdvanceByHook(player, "secret_realm_clear");
            tryStartOrAdvanceChain(player, realm.contains("blood") ? "blood_forbidden_campaign" : "");
        }
    }

    private static void handleDialogueEffect(ServerPlayer player, DialogueBranchService.Effect effect) {
        if (effect == null) {
            return;
        }
        String type = normalize(effect.type());
        if (type.isBlank()) {
            return;
        }
        // Param-driven quest ids.
        List<String> questIds = new ArrayList<>();
        String q = firstNonBlank(effect.param("q"), effect.param("quest"), effect.param("quest_id"), effect.param("id"));
        if (!q.isBlank()) {
            questIds.add(q);
        }
        // quest_ids may be stored as comma string if params are flat.
        String multi = effect.param("quest_ids");
        if (multi != null && !multi.isBlank()) {
            for (String part : multi.split("[,;\\s]+")) {
                if (!part.isBlank()) {
                    questIds.add(part.trim());
                }
            }
        }
        // Effect-link catalog: "offer_quest:zhenyan_outer_lesson" or bare type.
        String effectKey = type;
        if (!q.isBlank()) {
            effectKey = type + ":" + normalize(q);
        }
        questIds.addAll(EFFECT_TO_QUESTS.getOrDefault(normalize(effectKey), List.of()));
        questIds.addAll(EFFECT_TO_QUESTS.getOrDefault(type, List.of()));

        switch (type) {
            case "offer_quest", "open_quest", "open_quest_board" -> {
                for (String questId : questIds) {
                    tryStartOrAdvanceChain(player, questId);
                    // Playable ids may not be in the 62-chain index; mark dialogue flag as soft accept.
                    NpcDialogueFlags.setFlag(player, "quest_offered_" + normalize(questId));
                }
            }
            case "turnin_quests" -> {
                for (String questId : questIds) {
                    tryAdvanceActive(player, questId);
                }
                // If no explicit ids, advance any active chain bound to current dialogue NPC.
                if (questIds.isEmpty()) {
                    advanceActiveNearNpc(player);
                }
            }
            default -> {
                // Other effects may still map via effect catalog.
                for (String questId : questIds) {
                    tryStartOrAdvanceChain(player, questId);
                }
            }
        }
    }

    private static void advanceActiveNearNpc(ServerPlayer player) {
        for (TextQuestChainService.ChainProgress progress : TextQuestChainService.listProgress(player)) {
            if (progress.stage() <= 0 || progress.complete()) {
                continue;
            }
            TextQuestChainService.advance(player, progress.id());
            break;
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
            tryAdvanceActive(player, chainId);
        }
        // Soft flag so dialogue conditions / future UI can see the hook fire.
        NpcDialogueFlags.setFlag(player, "hook_" + hook);
    }

    private static void tryAdvanceActive(ServerPlayer player, String chainId) {
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
            map.put(effect, List.copyOf(quests));
            // Also index bare type prefix before ':'.
            int colon = effect.indexOf(':');
            if (colon > 0) {
                String type = effect.substring(0, colon);
                map.computeIfAbsent(type, k -> new ArrayList<>());
                List<String> merged = new ArrayList<>(map.get(type));
                for (String q : quests) {
                    if (!merged.contains(q)) {
                        merged.add(q);
                    }
                }
                map.put(type, List.copyOf(merged));
            }
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
