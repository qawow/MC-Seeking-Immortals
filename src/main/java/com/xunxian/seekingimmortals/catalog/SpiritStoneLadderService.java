package com.xunxian.seekingimmortals.catalog;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.xunxian.seekingimmortals.SeekingImmortalsMod;
import com.xunxian.seekingimmortals.registry.ModItems;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

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
 * Spirit-stone ladder authority from economy_spirit_stone_master + spirit_stone_ladder.
 * Canonical chain: low → mid → high → peak(top) at 1:100:10000:1000000 low-stone equivalents.
 */
public final class SpiritStoneLadderService {
    private static final Snapshot BUILTIN = loadBuiltin();

    private SpiritStoneLadderService() {}

    public static Snapshot builtin() {
        return BUILTIN;
    }

    public static int ratioPerTier() {
        return BUILTIN.ratioPerTier();
    }

    public static List<Tier> tiers() {
        return BUILTIN.tiers();
    }

    public static Optional<Tier> findTier(String tierId) {
        return BUILTIN.findTier(tierId);
    }

    public static Optional<Tier> findByItemId(String itemId) {
        return BUILTIN.findByItemId(itemId);
    }

    public static long toLowEquiv(String tierId, long count) {
        if (count <= 0L) {
            return 0L;
        }
        Optional<Tier> tier = findTier(tierId);
        if (tier.isEmpty()) {
            return 0L;
        }
        return Math.multiplyExact(tier.get().lowEquiv(), count);
    }

    public static Optional<ExchangeStep> nextUpgrade(String fromTierId) {
        return BUILTIN.nextUpgrade(fromTierId);
    }

    /**
     * Server-side adjacent-tier upgrade: {@code ratio} of {@code from} become 1 {@code to}.
     * Also supports 10 shards → 1 low when corpus declares it.
     */
    public static boolean tryUpgrade(Player player) {
        if (player == null) {
            return false;
        }
        // Prefer corpus bulk ladder first, then elemental attribute chains, then shard→low.
        if (tryBulkLadderUpgrade(player)) {
            return true;
        }
        if (tryElementalUpgrade(player)) {
            return true;
        }
        return tryShardToLow(player);
    }

    private static boolean tryBulkLadderUpgrade(Player player) {
        List<Tier> tiers = BUILTIN.tiers();
        int ratio = Math.max(1, BUILTIN.ratioPerTier());
        for (int i = 0; i + 1 < tiers.size(); i++) {
            Tier from = tiers.get(i);
            Tier to = tiers.get(i + 1);
            Item fromItem = resolveItem(from.itemId());
            Item toItem = resolveItem(to.itemId());
            if (fromItem == null || toItem == null || fromItem == Items.AIR || toItem == Items.AIR) {
                continue;
            }
            if (exchange(player, fromItem, toItem, ratio)) {
                return true;
            }
        }
        return false;
    }

    private static boolean tryElementalUpgrade(Player player) {
        int ratio = Math.max(1, BUILTIN.ratioPerTier());
        return exchange(player, ModItems.METAL_SPIRIT_STONE_HIGH.get(), ModItems.METAL_SPIRIT_STONE_SUPERIOR.get(), ratio)
                || exchange(player, ModItems.METAL_SPIRIT_STONE_MID.get(), ModItems.METAL_SPIRIT_STONE_HIGH.get(), ratio)
                || exchange(player, ModItems.METAL_SPIRIT_STONE.get(), ModItems.METAL_SPIRIT_STONE_MID.get(), ratio)
                || exchange(player, ModItems.WOOD_SPIRIT_STONE_HIGH.get(), ModItems.WOOD_SPIRIT_STONE_SUPERIOR.get(), ratio)
                || exchange(player, ModItems.WOOD_SPIRIT_STONE_MID.get(), ModItems.WOOD_SPIRIT_STONE_HIGH.get(), ratio)
                || exchange(player, ModItems.WOOD_SPIRIT_STONE.get(), ModItems.WOOD_SPIRIT_STONE_MID.get(), ratio)
                || exchange(player, ModItems.WATER_SPIRIT_STONE_HIGH.get(), ModItems.WATER_SPIRIT_STONE_SUPERIOR.get(), ratio)
                || exchange(player, ModItems.WATER_SPIRIT_STONE_MID.get(), ModItems.WATER_SPIRIT_STONE_HIGH.get(), ratio)
                || exchange(player, ModItems.WATER_SPIRIT_STONE.get(), ModItems.WATER_SPIRIT_STONE_MID.get(), ratio)
                || exchange(player, ModItems.FIRE_ELEMENT_SPIRIT_STONE_HIGH.get(), ModItems.FIRE_ELEMENT_SPIRIT_STONE_SUPERIOR.get(), ratio)
                || exchange(player, ModItems.FIRE_ELEMENT_SPIRIT_STONE_MID.get(), ModItems.FIRE_ELEMENT_SPIRIT_STONE_HIGH.get(), ratio)
                || exchange(player, ModItems.FIRE_ELEMENT_SPIRIT_STONE.get(), ModItems.FIRE_ELEMENT_SPIRIT_STONE_MID.get(), ratio)
                || exchange(player, ModItems.EARTH_SPIRIT_STONE_HIGH.get(), ModItems.EARTH_SPIRIT_STONE_SUPERIOR.get(), ratio)
                || exchange(player, ModItems.EARTH_SPIRIT_STONE_MID.get(), ModItems.EARTH_SPIRIT_STONE_HIGH.get(), ratio)
                || exchange(player, ModItems.EARTH_SPIRIT_STONE.get(), ModItems.EARTH_SPIRIT_STONE_MID.get(), ratio);
    }

    private static boolean tryShardToLow(Player player) {
        int rate = BUILTIN.shardToLowRate();
        if (rate <= 0) {
            return false;
        }
        Item shard = ModItems.SPIRIT_STONE_SHARD.get();
        String lowItemId = "low_spirit_stone";
        Optional<Tier> lowTier = findTier("low");
        if (lowTier.isPresent()) {
            lowItemId = lowTier.get().itemId();
        }
        Item low = resolveItem(lowItemId);
        if (low == null || low == Items.AIR) {
            // Fall back to metal low stone if bulk low is unavailable at runtime.
            low = ModItems.METAL_SPIRIT_STONE.get();
        }
        return exchange(player, shard, low, rate);
    }

    public static boolean exchange(Player player, Item input, Item output, int ratio) {
        if (player == null || input == null || output == null || ratio <= 0) {
            return false;
        }
        Inventory inventory = player.getInventory();
        int count = 0;
        for (ItemStack stack : inventory.items) {
            if (stack.is(input)) {
                count += stack.getCount();
            }
        }
        if (count < ratio) {
            return false;
        }
        int remaining = ratio;
        for (ItemStack stack : inventory.items) {
            if (!stack.is(input)) {
                continue;
            }
            int remove = Math.min(remaining, stack.getCount());
            stack.shrink(remove);
            remaining -= remove;
            if (remaining <= 0) {
                break;
            }
        }
        ItemStack out = new ItemStack(output);
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            com.xunxian.seekingimmortals.item.InventoryDeliveryService.giveOrEnqueue(
                    serverPlayer, out, "spirit_stone_ladder");
        } else if (!inventory.add(out)) {
            player.drop(out, false);
        }
        return true;
    }

    public static Item resolveItem(String itemId) {
        if (itemId == null || itemId.isBlank()) {
            return null;
        }
        String id = itemId.trim().toLowerCase(Locale.ROOT);
        if (!id.contains(":")) {
            id = SeekingImmortalsMod.MODID + ":" + id;
        }
        ResourceLocation location = ResourceLocation.tryParse(id);
        if (location == null) {
            return null;
        }
        Item item = ForgeRegistries.ITEMS.getValue(location);
        if (item == null || item == Items.AIR) {
            return null;
        }
        return item;
    }

    public record Tier(String id, String display, String itemId, long lowEquiv) {}

    public record ExchangeStep(String fromTier, String toTier, int ratio, String fromItemId, String toItemId) {}

    public record Snapshot(int ratioPerTier, List<Tier> tiers, Map<String, Long> lowEquivByTier,
                           Map<String, String> itemIdsByTier, int shardToLowRate, boolean highToLowReluctance) {
        public Optional<Tier> findTier(String tierId) {
            String id = normalize(tierId);
            if ("top".equals(id) || "spirit".equals(id)) {
                id = "peak";
            }
            String finalId = id;
            return tiers.stream().filter(t -> t.id().equals(finalId)).findFirst();
        }

        public Optional<Tier> findByItemId(String itemId) {
            String id = shortId(itemId);
            return tiers.stream().filter(t -> shortId(t.itemId()).equals(id)).findFirst();
        }

        public Optional<ExchangeStep> nextUpgrade(String fromTierId) {
            String from = normalize(fromTierId);
            if ("top".equals(from)) {
                from = "peak";
            }
            for (int i = 0; i + 1 < tiers.size(); i++) {
                if (tiers.get(i).id().equals(from)) {
                    Tier a = tiers.get(i);
                    Tier b = tiers.get(i + 1);
                    return Optional.of(new ExchangeStep(a.id(), b.id(), ratioPerTier, a.itemId(), b.itemId()));
                }
            }
            return Optional.empty();
        }

        public List<String> chainItemIds() {
            List<String> ids = new ArrayList<>();
            for (Tier tier : tiers) {
                ids.add(tier.itemId());
            }
            return List.copyOf(ids);
        }
    }

    private static Snapshot loadBuiltin() {
        JsonObject master = readJson(path("text_material/economy_spirit_stone_master.json"));
        JsonObject ladder = readJson(path("text_material/spirit_stone_ladder.json"));
        JsonObject currency = readJson(path("text_material/currency_items.json"));

        int ratio = 100;
        boolean reluctance = true;
        Map<String, String> itemIds = new LinkedHashMap<>();
        Map<String, Long> lowEquiv = new LinkedHashMap<>();
        if (master != null && master.has("exchange") && master.get("exchange").isJsonObject()) {
            JsonObject exchange = master.getAsJsonObject("exchange");
            if (exchange.has("ratio_per_tier") && exchange.get("ratio_per_tier").isJsonPrimitive()) {
                try {
                    ratio = Math.max(1, exchange.get("ratio_per_tier").getAsInt());
                } catch (Exception ignored) {
                    ratio = 100;
                }
            }
            if (exchange.has("high_to_low_reluctance") && exchange.get("high_to_low_reluctance").isJsonPrimitive()) {
                try {
                    reluctance = exchange.get("high_to_low_reluctance").getAsBoolean();
                } catch (Exception ignored) {
                    reluctance = true;
                }
            }
            if (exchange.has("item_ids") && exchange.get("item_ids").isJsonObject()) {
                for (Map.Entry<String, JsonElement> entry : exchange.getAsJsonObject("item_ids").entrySet()) {
                    try {
                        itemIds.put(normalize(entry.getKey()), entry.getValue().getAsString());
                    } catch (Exception ignored) {
                    }
                }
            }
            if (exchange.has("low_equiv_multiplier") && exchange.get("low_equiv_multiplier").isJsonObject()) {
                for (Map.Entry<String, JsonElement> entry : exchange.getAsJsonObject("low_equiv_multiplier").entrySet()) {
                    try {
                        lowEquiv.put(normalize(entry.getKey()), entry.getValue().getAsLong());
                    } catch (Exception ignored) {
                    }
                }
            }
        }
        if (ladder != null && ladder.has("exchange_ratio") && ladder.get("exchange_ratio").isJsonObject()) {
            JsonObject er = ladder.getAsJsonObject("exchange_ratio");
            if (er.has("low_to_mid") && er.get("low_to_mid").isJsonPrimitive()) {
                try {
                    ratio = Math.max(1, er.get("low_to_mid").getAsInt());
                } catch (Exception ignored) {
                }
            }
        }

        // Defaults if corpus missing.
        if (itemIds.isEmpty()) {
            itemIds.put("low", "low_spirit_stone");
            itemIds.put("mid", "mid_spirit_stone");
            itemIds.put("high", "high_spirit_stone");
            itemIds.put("peak", "top_spirit_stone");
        }
        if (!itemIds.containsKey("peak") && itemIds.containsKey("top")) {
            itemIds.put("peak", itemIds.get("top"));
        }
        if (lowEquiv.isEmpty()) {
            lowEquiv.put("low", 1L);
            lowEquiv.put("mid", 100L);
            lowEquiv.put("high", 10000L);
            lowEquiv.put("peak", 1000000L);
        }
        if (!lowEquiv.containsKey("peak") && lowEquiv.containsKey("top")) {
            lowEquiv.put("peak", lowEquiv.get("top"));
        }

        List<Tier> tiers = new ArrayList<>();
        if (ladder != null) {
            for (JsonElement element : array(ladder, "tiers")) {
                if (!element.isJsonObject()) {
                    continue;
                }
                JsonObject o = element.getAsJsonObject();
                String id = normalize(str(o, "id"));
                if ("top".equals(id) || "spirit".equals(id)) {
                    id = "peak";
                }
                if (id.isBlank()) {
                    continue;
                }
                String itemId = str(o, "item_id");
                if (itemId.isBlank()) {
                    itemId = itemIds.getOrDefault(id, id + "_spirit_stone");
                }
                long equiv = lowEquiv.getOrDefault(id, 1L);
                tiers.add(new Tier(id, str(o, "display"), shortId(itemId), equiv));
            }
        }
        if (tiers.isEmpty()) {
            for (String id : List.of("low", "mid", "high", "peak")) {
                tiers.add(new Tier(id, id, itemIds.getOrDefault(id, id + "_spirit_stone"), lowEquiv.getOrDefault(id, 1L)));
            }
        }

        int shardRate = 10;
        if (currency != null) {
            for (JsonElement element : array(currency, "items")) {
                if (!element.isJsonObject()) {
                    continue;
                }
                JsonObject o = element.getAsJsonObject();
                if (!"spirit_stone_shard".equals(shortId(str(o, "id")))) {
                    continue;
                }
                if (o.has("exchange") && o.get("exchange").isJsonObject()) {
                    JsonObject ex = o.getAsJsonObject("exchange");
                    if (ex.has("rate") && ex.get("rate").isJsonPrimitive()) {
                        try {
                            shardRate = Math.max(1, ex.get("rate").getAsInt());
                        } catch (Exception ignored) {
                        }
                    }
                }
            }
        }

        return new Snapshot(ratio, List.copyOf(tiers), Collections.unmodifiableMap(lowEquiv),
                Collections.unmodifiableMap(itemIds), shardRate, reluctance);
    }

    private static String path(String relative) {
        return "data/" + SeekingImmortalsMod.MODID + "/" + relative;
    }

    private static JsonObject readJson(String path) {
        try (InputStream stream = SpiritStoneLadderService.class.getClassLoader().getResourceAsStream(path)) {
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

    private static String str(JsonObject o, String key) {
        if (o == null || !o.has(key) || o.get(key).isJsonNull()) {
            return "";
        }
        try {
            return o.get(key).getAsString();
        } catch (Exception ignored) {
            return String.valueOf(o.get(key));
        }
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static String shortId(String itemId) {
        String id = normalize(itemId);
        int idx = id.indexOf(':');
        return idx >= 0 ? id.substring(idx + 1) : id;
    }
}
