package com.xunxian.seekingimmortals.npc;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.xunxian.seekingimmortals.SeekingImmortalsMod;
import com.xunxian.seekingimmortals.catalog.ItemCatalogService;
import com.xunxian.seekingimmortals.util.PlayerDisplayText;
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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * M12 named-NPC loot / dialogue rewards ({@code named_npc_loot_rewards_v97}).
 * Idempotent: each reward entry is granted at most once per player (M11 redline).
 */
public final class NamedNpcRewardService {
    private static final String CLAIMED_ROOT = "seeking_immortals_npc_rewards_claimed";
    private static final Snapshot BUILTIN = loadBuiltin();

    private NamedNpcRewardService() {}

    /** Preserve claimed dialogue/NPC reward ledger across death/clone. */
    public static void copyPersistentData(CompoundTag originalData, CompoundTag clonedData) {
        if (originalData == null || clonedData == null || !originalData.contains(CLAIMED_ROOT)) {
            return;
        }
        if (originalData.get(CLAIMED_ROOT) != null) {
            clonedData.put(CLAIMED_ROOT, originalData.get(CLAIMED_ROOT).copy());
        }
    }

    public static Snapshot builtin() {
        return BUILTIN;
    }

    public static Optional<RewardEntry> find(String rewardId) {
        return Optional.ofNullable(BUILTIN.entries().get(normalize(rewardId)));
    }

    public static int entryCount() {
        return BUILTIN.entries().size();
    }

    public static boolean hasClaimed(ServerPlayer player, String rewardId) {
        if (player == null) {
            return false;
        }
        String key = normalize(rewardId);
        if (key.isBlank()) {
            return false;
        }
        return player.getPersistentData().getCompound(CLAIMED_ROOT).getBoolean(key);
    }

    public static void markClaimed(ServerPlayer player, String rewardId) {
        if (player == null) {
            return;
        }
        String key = normalize(rewardId);
        if (key.isBlank()) {
            return;
        }
        CompoundTag root = player.getPersistentData().getCompound(CLAIMED_ROOT).copy();
        root.putBoolean(key, true);
        player.getPersistentData().put(CLAIMED_ROOT, root);
    }

    /**
     * Grant guaranteed catalog items for a reward entry once. Unresolvable ids are skipped.
     * @return number of item stacks actually granted
     */
    public static int grantIfUnclaimed(ServerPlayer player, String rewardId) {
        if (player == null) {
            return 0;
        }
        String id = normalize(rewardId);
        if (id.isBlank() || hasClaimed(player, id)) {
            return 0;
        }
        Optional<RewardEntry> entry = find(id);
        if (entry.isEmpty()) {
            // Still mark claimed for explicit node reward ids to keep idempotency even if corpus lacks entry.
            markClaimed(player, id);
            return 0;
        }
        int granted = 0;
        for (String itemToken : entry.get().guaranteed()) {
            if (grantCatalogItem(player, itemToken, 1)) {
                granted++;
            }
        }
        markClaimed(player, id);
        if (granted > 0) {
            Component rewardDisplay = PlayerDisplayText.safeCatalogLiteral(
                    entry.get().display(), "未知奖励");
            player.displayClientMessage(Component.literal("[角色奖励] ")
                    .append(rewardDisplay)
                    .append(" ×" + granted), false);
        }
        return granted;
    }

    public static boolean grantCatalogItem(ServerPlayer player, String itemIdOrAlias, int count) {
        if (player == null || count <= 0) {
            return false;
        }
        String resolved = ItemCatalogService.resolveId(itemIdOrAlias);
        if (resolved == null || resolved.isBlank()) {
            // Chinese display tokens in v97 are story-level; map a few common ones, else skip.
            resolved = mapStoryToken(itemIdOrAlias);
        }
        if (resolved == null || resolved.isBlank()) {
            return false;
        }
        Item item = ItemCatalogService.resolveCatalogItem(resolved);
        if (item == null) {
            return false;
        }
        ItemStack stack = new ItemStack(item, Math.min(64, count));
        com.xunxian.seekingimmortals.item.InventoryDeliveryService.giveOrEnqueue(
                player, stack, "named_npc_reward");
        return true;
    }

    private static String mapStoryToken(String token) {
        if (token == null) {
            return null;
        }
        String t = token.trim().toLowerCase(Locale.ROOT);
        return switch (t) {
            case "升仙令", "升仙令牌" -> "ascension_token";
            case "小剑符宝" -> "low_sword_talisman";
            case "血色禁地令", "试炼令", "blood_forbidden_token" -> "blood_forbidden_token";
            case "低阶灵石", "灵石" -> "low_spirit_stone";
            case "中阶灵石" -> "mid_spirit_stone";
            default -> {
                // If already looks like an id, keep it.
                if (t.matches("[a-z0-9_/:.-]+")) {
                    yield t;
                }
                yield null;
            }
        };
    }

    private static Snapshot loadBuiltin() {
        Map<String, RewardEntry> entries = new LinkedHashMap<>();
        JsonObject root = readJson("data/" + SeekingImmortalsMod.MODID + "/text_material/named_npc_loot_rewards_v97.json");
        if (root != null && root.has("entries") && root.get("entries").isJsonArray()) {
            for (JsonElement element : root.getAsJsonArray("entries")) {
                if (!element.isJsonObject()) {
                    continue;
                }
                JsonObject o = element.getAsJsonObject();
                String id = normalize(str(o, "id", ""));
                if (id.isBlank()) {
                    continue;
                }
                entries.put(id, new RewardEntry(
                        id,
                        str(o, "display", id),
                        str(o, "realm", ""),
                        str(o, "context", ""),
                        stringList(o.get("guaranteed")),
                        str(o, "description", "")));
            }
        }
        return new Snapshot(Collections.unmodifiableMap(entries));
    }

    private static List<String> stringList(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return List.of();
        }
        List<String> list = new ArrayList<>();
        if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) {
                if (child != null && child.isJsonPrimitive()) {
                    String value = child.getAsString();
                    if (value != null && !value.isBlank()) {
                        list.add(value.trim());
                    }
                }
            }
        }
        return List.copyOf(list);
    }

    private static JsonObject readJson(String path) {
        try (InputStream stream = NamedNpcRewardService.class.getClassLoader().getResourceAsStream(path)) {
            if (stream == null) {
                return null;
            }
            try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                return JsonParser.parseReader(reader).getAsJsonObject();
            }
        } catch (Exception exception) {
            SeekingImmortalsMod.LOGGER.warn("Failed to load named NPC rewards {}", path, exception);
            return null;
        }
    }

    private static String str(JsonObject object, String key, String fallback) {
        if (object == null || !object.has(key) || object.get(key).isJsonNull()) {
            return fallback;
        }
        try {
            return object.get(key).getAsString();
        } catch (Exception ex) {
            return String.valueOf(object.get(key));
        }
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    public record Snapshot(Map<String, RewardEntry> entries) {}

    public record RewardEntry(
            String id,
            String display,
            String realm,
            String context,
            List<String> guaranteed,
            String description) {}
}
