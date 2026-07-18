package com.xunxian.seekingimmortals.quest;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.xunxian.seekingimmortals.SeekingImmortalsMod;
import com.xunxian.seekingimmortals.catalog.ItemCatalogService;
import com.xunxian.seekingimmortals.item.InventoryDeliveryService;
import com.xunxian.seekingimmortals.registry.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
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

/**
 * M11 reward authority for main_quest_rewards_v101 + unique story items.
 * Redline: unique plot items are granted at most once per player (global ledger).
 */
public final class QuestRewardService {
    public static final String UNIQUE_LEDGER = "seeking_immortals_unique_story_rewards";

    /** Chinese / alias tokens that must never double-grant. */
    private static final Map<String, String> UNIQUE_ALIASES = Map.ofEntries(
            Map.entry("掌天瓶", "palm_heaven_bottle"),
            Map.entry("palm_heaven_bottle", "palm_heaven_bottle"),
            Map.entry("palm_sky_bottle", "palm_heaven_bottle"),
            Map.entry("heaven_palm_vase", "palm_heaven_bottle"),
            Map.entry("绿液", "green_liquid"),
            Map.entry("green_liquid", "green_liquid"),
            Map.entry("lv_ye", "green_liquid"),
            Map.entry("mystic_green_liquid", "green_liquid"),
            Map.entry("真魂丹", "true_soul_pill"),
            Map.entry("八灵尺", "eight_spirit_ruler"),
            Map.entry("炼神术", "spirit_refine_art"),
            Map.entry("虚天鼎默认", "void_heaven_cauldron"),
            Map.entry("回阳合成标记", "huiyang_synth_mark"),
            Map.entry("造化体验标记", "zaohua_trial_mark"),
            Map.entry("详尽节点", "detailed_node_token")
    );

    private static final Snapshot BUILTIN = loadBuiltin();

    private QuestRewardService() {}

    public record RewardChain(String id, String display, String realm, String prereq,
                              List<String> uniqueItems, int stepCount) {}

    public record Snapshot(Map<String, RewardChain> chains, Set<String> uniqueItems) {
        public int chainCount() {
            return chains.size();
        }
    }

    public static Snapshot builtin() {
        return BUILTIN;
    }

    public static int chainCount() {
        return BUILTIN.chainCount();
    }

    public static Optional<RewardChain> find(String chainId) {
        return Optional.ofNullable(BUILTIN.chains().get(normalize(chainId)));
    }

    public static boolean hasUniqueClaimed(ServerPlayer player, String uniqueToken) {
        if (player == null) {
            return false;
        }
        String key = canonicalUnique(uniqueToken);
        if (key.isBlank()) {
            return false;
        }
        return player.getPersistentData().getCompound(UNIQUE_LEDGER).getBoolean(key)
                || TextQuestChainService.hasAuthorityReward(player, "unique:" + key);
    }

    public static void markUniqueClaimed(ServerPlayer player, String uniqueToken) {
        if (player == null) {
            return;
        }
        String key = canonicalUnique(uniqueToken);
        if (key.isBlank()) {
            return;
        }
        CompoundTag tag = player.getPersistentData().getCompound(UNIQUE_LEDGER).copy();
        tag.putBoolean(key, true);
        player.getPersistentData().put(UNIQUE_LEDGER, tag);
        TextQuestChainService.markAuthorityReward(player, "unique:" + key);
    }

    /**
     * Grant a unique story reward once. Returns false if already claimed or unresolvable.
     * Unique forbidden bulk items are represented by mortal_quest_token proxy + ledger mark.
     */
    public static boolean grantUniqueOnce(ServerPlayer player, String uniqueToken) {
        if (player == null) {
            return false;
        }
        String key = canonicalUnique(uniqueToken);
        if (key.isBlank()) {
            return false;
        }
        if (hasUniqueClaimed(player, key)) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.quest_reward.unique_already", uniqueToken), false);
            return false;
        }
        ItemStack stack = resolveUniqueStack(key, uniqueToken);
        if (!stack.isEmpty()) {
            InventoryDeliveryService.giveOrDrop(player, stack);
        }
        markUniqueClaimed(player, key);
        player.displayClientMessage(Component.translatable(
                "message.seeking_immortals.quest_reward.unique_granted", uniqueToken), true);
        return true;
    }

    /**
     * Apply main_quest_rewards step uniques when a mapped text chain finishes.
     */
    public static void onTextChainFinished(ServerPlayer player, String chainId) {
        if (player == null || chainId == null || chainId.isBlank()) {
            return;
        }
        String id = normalize(chainId);
        // Direct reward-table id match.
        RewardChain direct = BUILTIN.chains().get(id);
        if (direct != null) {
            for (String unique : direct.uniqueItems()) {
                grantUniqueOnce(player, unique);
            }
        }
        // Soft map common campaign ids → reward table rows.
        for (String mapped : mappedRewardRows(id)) {
            RewardChain row = BUILTIN.chains().get(mapped);
            if (row == null) {
                continue;
            }
            for (String unique : row.uniqueItems()) {
                grantUniqueOnce(player, unique);
            }
        }
    }

    public static List<String> sample(int limit) {
        List<String> out = new ArrayList<>();
        int i = 0;
        for (RewardChain chain : BUILTIN.chains().values()) {
            out.add(chain.id() + " | " + chain.display()
                    + (chain.uniqueItems().isEmpty() ? "" : " unique=" + String.join(",", chain.uniqueItems())));
            if (++i >= Math.max(1, limit)) {
                break;
            }
        }
        return out;
    }

    private static List<String> mappedRewardRows(String chainId) {
        List<String> rows = new ArrayList<>();
        if (chainId.contains("qixuan") || chainId.contains("mortal")) {
            rows.add("mortal_to_immortal");
        }
        if (chainId.contains("huangfeng") || chainId.contains("blood")) {
            rows.add("huangfeng_qi");
        }
        if (chainId.contains("foundation") || chainId.contains("blood_forbidden")) {
            rows.add("foundation_path");
        }
        return rows;
    }

    private static ItemStack resolveUniqueStack(String canonical, String displayToken) {
        // Never resolve through bulk catalog for unique-forbidden ids.
        if (ItemCatalogService.isUniqueForbidden(canonical)) {
            // Proxy token so inventory has a visible marker without registering the forbidden bulk id.
            try {
                return new ItemStack(ModItems.JADE_SLIP_BLANK.get(), 1);
            } catch (Throwable ignored) {
                return ItemStack.EMPTY;
            }
        }
        Item item = null;
        try {
            item = ItemCatalogService.resolveCatalogItem(canonical);
        } catch (Throwable ignored) {
            item = null;
        }
        if (item != null) {
            return new ItemStack(item, 1);
        }
        // Fallback proxy for Chinese lore-only uniques (ledger is the authority).
        try {
            return new ItemStack(ModItems.JADE_SLIP_BLANK.get(), 1);
        } catch (Throwable ignored) {
            return ItemStack.EMPTY;
        }
    }

    public static String canonicalUnique(String token) {
        if (token == null || token.isBlank()) {
            return "";
        }
        String raw = token.trim();
        String lower = raw.toLowerCase(Locale.ROOT);
        String alias = UNIQUE_ALIASES.get(raw);
        if (alias == null) {
            alias = UNIQUE_ALIASES.get(lower);
        }
        if (alias != null) {
            return alias;
        }
        try {
            if (ItemCatalogService.isUniqueForbidden(lower)) {
                String id = ItemCatalogService.resolveId(lower);
                return id == null || id.isBlank() ? lower : id;
            }
        } catch (Throwable ignored) {
            // catalog may be mid-init in pure unit tests
        }
        return lower;
    }

    private static Snapshot loadBuiltin() {
        Map<String, RewardChain> chains = new LinkedHashMap<>();
        Set<String> uniques = new LinkedHashSet<>();
        JsonObject root = readJson(path("catalog/main_quest_rewards_index.json"));
        if (root != null) {
            for (JsonElement element : array(root, "chains")) {
                if (!element.isJsonObject()) {
                    continue;
                }
                JsonObject o = element.getAsJsonObject();
                String id = str(o, "id");
                if (id.isBlank()) {
                    continue;
                }
                List<String> unique = stringList(o.get("unique"));
                for (String u : unique) {
                    uniques.add(canonicalUnique(u));
                }
                chains.put(id, new RewardChain(id, str(o, "display"), str(o, "realm"), str(o, "prereq"),
                        unique, asInt(o, "step_count")));
            }
            for (JsonElement element : array(root, "unique_items")) {
                try {
                    uniques.add(canonicalUnique(element.getAsString()));
                } catch (Exception ignored) {
                    // skip
                }
            }
        }
        // Also parse full rewards file for step-level uniques if index is thin.
        JsonObject full = readJson(path("text_material/main_quest_rewards_v101.json"));
        if (full != null) {
            for (JsonElement element : array(full, "chains")) {
                if (!element.isJsonObject()) {
                    continue;
                }
                JsonObject o = element.getAsJsonObject();
                String id = str(o, "id");
                if (id.isBlank()) {
                    continue;
                }
                List<String> unique = new ArrayList<>();
                for (JsonElement stepEl : array(o, "steps")) {
                    if (!stepEl.isJsonObject()) {
                        continue;
                    }
                    for (String u : stringList(stepEl.getAsJsonObject().get("unique"))) {
                        unique.add(u);
                        uniques.add(canonicalUnique(u));
                    }
                }
                RewardChain existing = chains.get(id);
                if (existing == null) {
                    chains.put(id, new RewardChain(id, str(o, "display"), str(o, "realm"), str(o, "prereq"),
                            List.copyOf(unique), array(o, "steps").size()));
                } else if (existing.uniqueItems().isEmpty() && !unique.isEmpty()) {
                    chains.put(id, new RewardChain(existing.id(), existing.display(), existing.realm(),
                            existing.prereq(), List.copyOf(unique), existing.stepCount()));
                }
            }
        }
        return new Snapshot(Collections.unmodifiableMap(chains), Collections.unmodifiableSet(uniques));
    }

    private static String path(String relative) {
        return "data/" + SeekingImmortalsMod.MODID + "/" + relative;
    }

    private static JsonObject readJson(String path) {
        try (InputStream stream = QuestRewardService.class.getClassLoader().getResourceAsStream(path)) {
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

    private static int asInt(JsonObject object, String key) {
        if (object == null || !object.has(key) || !object.get(key).isJsonPrimitive()) {
            return 0;
        }
        try {
            return object.get(key).getAsInt();
        } catch (Exception ignored) {
            return 0;
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

    private static String normalize(String id) {
        return id == null ? "" : id.trim().toLowerCase(Locale.ROOT);
    }
}
