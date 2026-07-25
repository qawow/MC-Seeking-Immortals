package com.xunxian.seekingimmortals.shop;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.xunxian.seekingimmortals.SeekingImmortalsMod;
import com.xunxian.seekingimmortals.cultivation.CultivationHelper;
import com.xunxian.seekingimmortals.cultivation.Realm;
import com.xunxian.seekingimmortals.npc.NpcDialogueFlags;
import com.xunxian.seekingimmortals.worldpack.ReputationService;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/** Authoritative gates and post-purchase risks declared by merchant_shops.json. */
public final class MerchantShopPolicyCatalog {
    private static final Map<String, ShopPolicy> POLICIES = loadBuiltin();

    public enum GateResult {
        OPEN,
        REALM_LOCKED,
        REPUTATION_LOCKED,
        ACCESS_DENIED
    }

    public record ReputationGate(String faction, int threshold) {}

    public record EntryPolicy(
            String itemId,
            String realmMin,
            String realmMax,
            Map<String, Integer> reputationMinimums,
            boolean illegal) {}

    public record ShopPolicy(
            String shopId,
            String realmMin,
            List<ReputationGate> reputationGates,
            List<String> accessAny,
            List<String> riskEvents,
            Map<String, EntryPolicy> entries) {
        public Optional<EntryPolicy> entry(String itemId) {
            return Optional.ofNullable(entries.get(normalizeItem(itemId)));
        }
    }

    private MerchantShopPolicyCatalog() {}

    public static Optional<ShopPolicy> find(String shopId) {
        return Optional.ofNullable(POLICIES.get(normalize(shopId)));
    }

    public static Map<String, ShopPolicy> all() {
        return POLICIES;
    }

    public static GateResult evaluate(ServerPlayer player, String shopId, String itemId) {
        ShopPolicy policy = POLICIES.get(normalize(shopId));
        if (policy == null) {
            return GateResult.OPEN;
        }
        Realm current = CultivationHelper.get(player).map(cultivation -> cultivation.getRealm()).orElse(null);
        if (!meetsMinimum(current, policy.realmMin())) {
            return GateResult.REALM_LOCKED;
        }
        EntryPolicy entry = policy.entries().get(normalizeItem(itemId));
        if (entry != null && (!meetsMinimum(current, entry.realmMin()) || !meetsMaximum(current, entry.realmMax()))) {
            return GateResult.REALM_LOCKED;
        }
        for (ReputationGate gate : policy.reputationGates()) {
            int actual = ReputationService.get(player, gate.faction());
            if ((gate.threshold() >= 0 && actual < gate.threshold())
                    || (gate.threshold() < 0 && actual > gate.threshold())) {
                return GateResult.REPUTATION_LOCKED;
            }
        }
        if (entry != null) {
            for (Map.Entry<String, Integer> gate : entry.reputationMinimums().entrySet()) {
                if (ReputationService.get(player, gate.getKey()) < gate.getValue()) {
                    return GateResult.REPUTATION_LOCKED;
                }
            }
        }
        if (!policy.accessAny().isEmpty() && policy.accessAny().stream().noneMatch(token -> meetsAccess(player, token))) {
            return GateResult.ACCESS_DENIED;
        }
        return GateResult.OPEN;
    }

    public static void settleRisk(ServerPlayer player, String shopId, String itemId) {
        ShopPolicy policy = POLICIES.get(normalize(shopId));
        if (player == null || policy == null) {
            return;
        }
        EntryPolicy entry = policy.entries().get(normalizeItem(itemId));
        if (entry != null && entry.illegal()) {
            NpcDialogueFlags.setFlag(player, "illegal_shop_purchase");
            ReputationService.add(player, "star_palace", -1);
        }
        for (String risk : policy.riskEvents()) {
            if (player.getRandom().nextFloat() >= riskChance(risk)) {
                continue;
            }
            NpcDialogueFlags.setFlag(player, "shop_risk_" + normalize(risk));
            if (risk.contains("star_palace")) {
                ReputationService.add(player, "star_palace", -5);
            }
        }
    }

    private static boolean meetsAccess(ServerPlayer player, String token) {
        String key = normalize(token);
        if (key.matches("[a-z0-9_]+_rep_[0-9]+")) {
            int marker = key.lastIndexOf("_rep_");
            return ReputationService.get(player, key.substring(0, marker))
                    >= parseInt(key.substring(marker + 5), Integer.MAX_VALUE);
        }
        if ("pirate_loot_turnin".equals(key)) {
            return NpcDialogueFlags.hasFlag(player, key) || hasItem(player, "pirate_loot_bundle");
        }
        ReputationService.ParsedRep parsed = ReputationService.parse(key);
        if (parsed != null) {
            return ReputationService.get(player, parsed.faction()) >= parsed.minValue();
        }
        return NpcDialogueFlags.hasFlag(player, key);
    }

    private static boolean hasItem(ServerPlayer player, String itemId) {
        ResourceLocation id = ResourceLocation.tryParse(normalizeItem(itemId));
        Item item = id == null ? null : ForgeRegistries.ITEMS.getValue(id);
        return item != null && player.getInventory().contains(new net.minecraft.world.item.ItemStack(item));
    }

    static boolean meetsMinimum(Realm current, String id) {
        if (id == null || id.isBlank()) {
            return true;
        }
        Realm required = Realm.fromDesignId(id);
        return required != null && current != null && current.ordinal() >= required.ordinal();
    }

    static boolean meetsMaximum(Realm current, String id) {
        if (id == null || id.isBlank()) {
            return true;
        }
        Realm maximum = Realm.fromDesignId(id);
        return maximum != null && current != null && current.ordinal() <= maximum.ordinal();
    }

    private static double riskChance(String risk) {
        return risk.contains("raid") ? 0.20D : 0.10D;
    }

    private static Map<String, ShopPolicy> loadBuiltin() {
        String path = "data/" + SeekingImmortalsMod.MODID + "/text_material/merchant_shops.json";
        try (InputStream stream = MerchantShopPolicyCatalog.class.getClassLoader().getResourceAsStream(path)) {
            if (stream == null) {
                return Map.of();
            }
            try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                return parse(JsonParser.parseReader(reader).getAsJsonObject());
            }
        } catch (Exception exception) {
            SeekingImmortalsMod.LOGGER.warn("Failed to load merchant shop policies from {}", path, exception);
            return Map.of();
        }
    }

    static Map<String, ShopPolicy> parse(JsonObject root) {
        Map<String, ShopPolicy> policies = new LinkedHashMap<>();
        JsonArray shops = root == null ? null : root.getAsJsonArray("shops");
        if (shops == null) {
            return Map.of();
        }
        for (JsonElement element : shops) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject shop = element.getAsJsonObject();
            String id = normalize(str(shop, "id"));
            if (id.isBlank()) {
                continue;
            }
            List<ReputationGate> reputationGates = new ArrayList<>();
            JsonObject reputation = object(shop, "reputation_gate");
            if (reputation != null) {
                for (Map.Entry<String, JsonElement> gate : reputation.entrySet()) {
                    reputationGates.add(new ReputationGate(normalize(gate.getKey()), integer(gate.getValue(), 0)));
                }
            }
            List<String> accessAny = strings(object(shop, "access"), "any_of");
            JsonObject learnEnter = object(object(shop, "learn_requirements"), "enter");
            if (learnEnter != null) {
                accessAny = merge(accessAny, strings(learnEnter, "reputation_any"));
            }
            Map<String, EntryPolicy> entries = new LinkedHashMap<>();
            JsonArray stock = shop.getAsJsonArray("stock");
            if (stock != null) {
                for (JsonElement stockElement : stock) {
                    if (!stockElement.isJsonObject()) {
                        continue;
                    }
                    JsonObject item = stockElement.getAsJsonObject();
                    String itemId = normalizeItem(str(item, "item"));
                    if (itemId.isBlank()) {
                        continue;
                    }
                    Map<String, Integer> itemRep = new LinkedHashMap<>();
                    for (Map.Entry<String, JsonElement> field : item.entrySet()) {
                        if (field.getKey().startsWith("rep_") && field.getValue().isJsonPrimitive()) {
                            itemRep.put(normalize(field.getKey().substring(4)), integer(field.getValue(), 0));
                        }
                    }
                    entries.put(itemId, new EntryPolicy(
                            itemId,
                            str(item, "realm_min"),
                            str(item, "realm_max"),
                            Collections.unmodifiableMap(itemRep),
                            bool(item, "illegal")));
                }
            }
            String realmMin = str(shop, "realm_min");
            if (realmMin.isBlank() && learnEnter != null) {
                realmMin = str(learnEnter, "realm_min");
            }
            policies.put(id, new ShopPolicy(
                    id,
                    realmMin,
                    List.copyOf(reputationGates),
                    List.copyOf(accessAny),
                    strings(shop, "risk_events"),
                    Collections.unmodifiableMap(entries)));
        }
        return Collections.unmodifiableMap(policies);
    }

    private static JsonObject object(JsonObject parent, String key) {
        if (parent == null || !parent.has(key) || !parent.get(key).isJsonObject()) {
            return null;
        }
        return parent.getAsJsonObject(key);
    }

    private static List<String> strings(JsonObject parent, String key) {
        if (parent == null || !parent.has(key) || !parent.get(key).isJsonArray()) {
            return List.of();
        }
        List<String> out = new ArrayList<>();
        for (JsonElement value : parent.getAsJsonArray(key)) {
            String normalized = normalize(value.getAsString());
            if (!normalized.isBlank()) {
                out.add(normalized);
            }
        }
        return List.copyOf(out);
    }

    private static List<String> merge(List<String> first, List<String> second) {
        List<String> merged = new ArrayList<>(first);
        for (String value : second) {
            if (!merged.contains(value)) {
                merged.add(value);
            }
        }
        return merged;
    }

    private static String str(JsonObject object, String key) {
        if (object == null || !object.has(key) || object.get(key).isJsonNull()) {
            return "";
        }
        return object.get(key).getAsString();
    }

    private static boolean bool(JsonObject object, String key) {
        return object != null && object.has(key) && object.get(key).getAsBoolean();
    }

    private static int integer(JsonElement element, int fallback) {
        try {
            return element.getAsInt();
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizeItem(String value) {
        String id = normalize(value);
        return id.isBlank() || id.indexOf(':') >= 0 ? id : SeekingImmortalsMod.MODID + ":" + id;
    }
}
