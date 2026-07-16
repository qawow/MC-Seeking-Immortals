package com.xunxian.seekingimmortals.sect;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.xunxian.seekingimmortals.SeekingImmortalsMod;
import com.xunxian.seekingimmortals.shop.ShopService;

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
 * M08 contribution shop shelves from corpus, instantiated on top of M05 {@link ShopService} currency rules.
 * Redline: contribution/功勋 is never freely convertible to infinite spirit stones in runtime APIs.
 */
public final class SectContributionShopService {
    public static final String CURRENCY_CONTRIBUTION = "sect_contribution";
    public static final String CURRENCY_CONTRIBUTION_POINT = "sect_contribution_point";

    private static final Snapshot BUILTIN = loadBuiltin();

    private SectContributionShopService() {}

    public static Snapshot builtin() {
        return BUILTIN;
    }

    public static Optional<ContributionShop> shopForFaction(String factionId) {
        String id = normalize(SectDefinitionService.canonicalizeSectId(factionId));
        ContributionShop shop = BUILTIN.shopsByFaction().get(id);
        if (shop != null) {
            return Optional.of(shop);
        }
        return Optional.ofNullable(BUILTIN.shopsByFaction().get(normalize(factionId)));
    }

    public static Optional<ShelfSet> shelvesForFaction(String factionId) {
        String id = normalize(factionId);
        ShelfSet set = BUILTIN.shelvesByFaction().get(id);
        if (set != null) {
            return Optional.of(set);
        }
        // Try canonical / short forms.
        String canonical = normalize(SectDefinitionService.canonicalizeSectId(factionId));
        set = BUILTIN.shelvesByFaction().get(canonical);
        if (set != null) {
            return Optional.of(set);
        }
        for (Map.Entry<String, ShelfSet> entry : BUILTIN.shelvesByFaction().entrySet()) {
            if (canonical.contains(entry.getKey()) || entry.getKey().contains(canonical)
                    || id.contains(entry.getKey()) || entry.getKey().contains(id)) {
                return Optional.of(entry.getValue());
            }
        }
        return Optional.empty();
    }

    /**
     * Open shelf tier ids for a reputation value (rep_min gating).
     */
    public static List<Shelf> openShelves(String factionId, int reputation) {
        Optional<ShelfSet> optional = shelvesForFaction(factionId);
        if (optional.isEmpty()) {
            return List.of();
        }
        List<Shelf> open = new ArrayList<>();
        for (Shelf shelf : optional.get().shelves()) {
            if (reputation >= shelf.repMin()) {
                open.add(shelf);
            }
        }
        return List.copyOf(open);
    }

    public static List<ShopOffer> contributionOffers(String factionId) {
        return shopForFaction(factionId).map(ContributionShop::items).orElse(List.of());
    }

    /**
     * Redline guard used by tests and any future convert UI: contribution must not mint spirit stones 1:1 freely.
     * Corpus may list reference rates for pricing validation only; runtime conversion is disabled.
     */
    public static boolean allowsInfiniteContributionToSpiritStoneExchange() {
        return false;
    }

    public static boolean isContributionCurrency(String currency) {
        String c = normalize(currency);
        return CURRENCY_CONTRIBUTION.equals(c)
                || CURRENCY_CONTRIBUTION_POINT.equals(c)
                || "sect_contrib".equals(c)
                || "contribution".equals(c)
                || ShopService.CURRENCY_SECT_CONTRIBUTION.equals(c);
    }

    /**
     * Reference rate for UI/tooling only — not a runtime convert API.
     */
    public static double referenceLowStonePerContribution(String factionId) {
        String id = normalize(SectDefinitionService.canonicalizeSectId(factionId));
        Double rate = BUILTIN.referenceRates().get(id);
        if (rate != null) {
            return rate;
        }
        return BUILTIN.defaultReferenceRate();
    }

    public static boolean isNeverListItem(String itemDisplayOrId) {
        if (isHardNeverItem(itemDisplayOrId)) {
            return true;
        }
        String key = normalize(itemDisplayOrId);
        if (key.isBlank()) {
            return false;
        }
        // Avoid touching BUILTIN during its own static init.
        Snapshot snapshot = BUILTIN;
        if (snapshot == null) {
            return false;
        }
        for (String banned : snapshot.neverList()) {
            String b = normalize(banned);
            if (key.equals(b) || key.contains(b) || b.contains(key)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isHardNeverItem(String itemDisplayOrId) {
        String key = normalize(itemDisplayOrId);
        if (key.isBlank()) {
            return false;
        }
        return key.contains("palm_heaven") || key.contains("掌天瓶")
                || key.contains("green_liquid") || key.contains("绿液")
                || key.contains("通天完整") || key.contains("真魂丹") || key.contains("炼神术");
    }

    private static Snapshot loadBuiltin() {
        Map<String, ContributionShop> shops = new LinkedHashMap<>();
        Map<String, ShelfSet> shelves = new LinkedHashMap<>();
        Map<String, Double> rates = new LinkedHashMap<>();
        Set<String> never = new LinkedHashSet<>();
        double defaultRate = 1.0D;

        JsonObject shopRoot = readJson("data/" + SeekingImmortalsMod.MODID + "/text_material/sect_contribution_shop.json");
        if (shopRoot != null) {
            JsonArray arr = shopRoot.getAsJsonArray("shops");
            if (arr != null) {
                for (JsonElement el : arr) {
                    if (!el.isJsonObject()) continue;
                    JsonObject o = el.getAsJsonObject();
                    String faction = normalize(SectDefinitionService.canonicalizeSectId(str(o, "faction_id")));
                    if (faction.isBlank()) continue;
                    List<ShopOffer> items = new ArrayList<>();
                    JsonArray itemArr = o.getAsJsonArray("items");
                    if (itemArr != null) {
                        for (JsonElement iEl : itemArr) {
                            if (!iEl.isJsonObject()) continue;
                            JsonObject item = iEl.getAsJsonObject();
                            String id = str(item, "id");
                            if (id.isBlank() || isHardNeverItem(id)) continue;
                            int cost = intOr(item, "cost", 1);
                            items.add(new ShopOffer(
                                    id,
                                    cost,
                                    CURRENCY_CONTRIBUTION,
                                    str(item, "realm_gate"),
                                    str(item, "rank_min"),
                                    intOr(item, "monthly_limit", 0),
                                    str(item, "type")));
                        }
                    }
                    ContributionShop shop = new ContributionShop(faction, str(o, "display"), List.copyOf(items));
                    shops.put(faction, shop);
                    shops.putIfAbsent(normalize(str(o, "faction_id")), shop);
                }
            }
            // huangfeng_valley nested block
            if (shopRoot.has("huangfeng_valley") && shopRoot.get("huangfeng_valley").isJsonObject()) {
                JsonObject hv = shopRoot.getAsJsonObject("huangfeng_valley");
                List<ShopOffer> items = new ArrayList<>();
                JsonArray itemArr = hv.getAsJsonArray("items");
                if (itemArr != null) {
                    for (JsonElement iEl : itemArr) {
                        if (!iEl.isJsonObject()) continue;
                        JsonObject item = iEl.getAsJsonObject();
                        String id = firstNonBlank(str(item, "id"), str(item, "item"));
                        if (id.isBlank() || isHardNeverItem(id)) continue;
                        items.add(new ShopOffer(id, intOr(item, "cost", 1), CURRENCY_CONTRIBUTION,
                                str(item, "realm_gate"), str(item, "rank_min"), intOr(item, "monthly_limit", 0), str(item, "type")));
                    }
                }
                if (!items.isEmpty()) {
                    shops.putIfAbsent("huangfeng_valley", new ContributionShop("huangfeng_valley", "黄枫谷贡献堂", List.copyOf(items)));
                }
            }
        }

        loadShelves(shelves, never, "sect_shelves_v106.json");
        loadShelves(shelves, never, "sect_shelves_more_v107.json");

        JsonObject exchange = readJson("data/" + SeekingImmortalsMod.MODID + "/text_material/economy_contribution_exchange.json");
        if (exchange != null) {
            if (exchange.has("default_rate") && exchange.get("default_rate").isJsonObject()) {
                JsonObject dr = exchange.getAsJsonObject("default_rate");
                // contribution_per_low_stone ≈ inverse of low_stone_per_contribution
                double cPerStone = doubleOr(dr, "contribution_per_low_stone", 1.0D);
                if (cPerStone > 0) {
                    defaultRate = 1.0D / cPerStone;
                }
            }
            JsonArray fr = exchange.getAsJsonArray("faction_rates");
            if (fr != null) {
                for (JsonElement el : fr) {
                    if (!el.isJsonObject()) continue;
                    JsonObject o = el.getAsJsonObject();
                    String faction = normalize(SectDefinitionService.canonicalizeSectId(str(o, "faction_id")));
                    rates.put(faction, doubleOr(o, "low_stone_per_contribution", defaultRate));
                }
            }
        }

        return new Snapshot(
                Collections.unmodifiableMap(shops),
                Collections.unmodifiableMap(shelves),
                Collections.unmodifiableMap(rates),
                Collections.unmodifiableSet(never),
                defaultRate);
    }

    private static void loadShelves(Map<String, ShelfSet> shelves, Set<String> never, String file) {
        JsonObject root = readJson("data/" + SeekingImmortalsMod.MODID + "/text_material/" + file);
        if (root == null) {
            return;
        }
        if (root.has("global_rules") && root.get("global_rules").isJsonObject()) {
            JsonObject gr = root.getAsJsonObject("global_rules");
            never.addAll(stringList(gr.get("never_list")));
        }
        JsonArray arr = root.getAsJsonArray("sects");
        if (arr == null) {
            return;
        }
        for (JsonElement el : arr) {
            if (!el.isJsonObject()) continue;
            JsonObject o = el.getAsJsonObject();
            String id = normalize(str(o, "id"));
            if (id.isBlank()) continue;
            String repFaction = firstNonBlank(str(o, "rep_faction"), id);
            List<String> currencies = stringList(o.get("currency"));
            List<Shelf> shelfList = new ArrayList<>();
            JsonArray shelfArr = o.getAsJsonArray("shelves");
            if (shelfArr != null) {
                for (JsonElement sEl : shelfArr) {
                    if (!sEl.isJsonObject()) continue;
                    JsonObject s = sEl.getAsJsonObject();
                    List<ShelfWare> wares = new ArrayList<>();
                    JsonArray wareArr = s.getAsJsonArray("wares");
                    if (wareArr != null) {
                        for (JsonElement wEl : wareArr) {
                            if (!wEl.isJsonObject()) continue;
                            JsonObject w = wEl.getAsJsonObject();
                            String wareId = str(w, "id");
                            if (wareId.isBlank() || isNeverListed(never, wareId) || isNeverListed(never, str(w, "display"))) {
                                continue;
                            }
                            wares.add(new ShelfWare(wareId, str(w, "display"), priceLabel(w.get("price")), str(w, "note")));
                        }
                    }
                    shelfList.add(new Shelf(str(s, "id"), str(s, "display"), intOr(s, "rep_min", 0), List.copyOf(wares)));
                }
            }
            ShelfSet set = new ShelfSet(id, str(o, "display"), repFaction, currencies, List.copyOf(shelfList));
            shelves.put(id, set);
            shelves.putIfAbsent(normalize(SectDefinitionService.canonicalizeSectId(id)), set);
            // map common aliases
            if ("huangfeng".equals(id)) {
                shelves.putIfAbsent("huangfeng_valley", set);
            }
            if ("guiling".equals(id) || "guiling_men".equals(id)) {
                shelves.putIfAbsent("guiling_gate", set);
            }
        }
    }

    private static boolean isNeverListed(Set<String> never, String value) {
        String key = normalize(value);
        if (key.isBlank()) return false;
        for (String banned : never) {
            String b = normalize(banned);
            if (key.equals(b) || key.contains(b) || b.contains(key)) {
                return true;
            }
        }
        return key.contains("掌天瓶") || key.contains("绿液") || key.contains("palm_heaven") || key.contains("green_liquid");
    }

    private static String priceLabel(JsonElement price) {
        if (price == null || price.isJsonNull()) {
            return "";
        }
        if (price.isJsonPrimitive()) {
            return price.getAsString();
        }
        if (price.isJsonArray()) {
            JsonArray arr = price.getAsJsonArray();
            if (arr.size() == 0) return "";
            if (arr.size() == 1) return arr.get(0).toString();
            return arr.get(0) + "-" + arr.get(1);
        }
        return price.toString();
    }

    private static List<String> stringList(JsonElement element) {
        List<String> list = new ArrayList<>();
        if (element == null || element.isJsonNull()) {
            return list;
        }
        if (element.isJsonArray()) {
            for (JsonElement el : element.getAsJsonArray()) {
                if (el.isJsonPrimitive()) {
                    list.add(el.getAsString());
                }
            }
        } else if (element.isJsonPrimitive()) {
            list.add(element.getAsString());
        }
        return list;
    }

    private static JsonObject readJson(String path) {
        try (InputStream stream = SectContributionShopService.class.getClassLoader().getResourceAsStream(path)) {
            if (stream == null) {
                return null;
            }
            try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                return JsonParser.parseReader(reader).getAsJsonObject();
            }
        } catch (Exception exception) {
            SeekingImmortalsMod.LOGGER.warn("Failed to load contribution shop {}", path, exception);
            return null;
        }
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

    private static String firstNonBlank(String... values) {
        if (values == null) return "";
        for (String value : values) {
            if (value != null && !value.isBlank()) return value;
        }
        return "";
    }

    private static int intOr(JsonObject object, String key, int fallback) {
        if (object == null || !object.has(key) || object.get(key).isJsonNull()) return fallback;
        try {
            return object.get(key).getAsInt();
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static double doubleOr(JsonObject object, String key, double fallback) {
        if (object == null || !object.has(key) || object.get(key).isJsonNull()) return fallback;
        try {
            return object.get(key).getAsDouble();
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    public record ShopOffer(String itemId, int cost, String currency, String realmGate, String rankMin,
                            int monthlyLimit, String type) {}

    public record ContributionShop(String factionId, String display, List<ShopOffer> items) {}

    public record ShelfWare(String id, String display, String priceLabel, String note) {}

    public record Shelf(String id, String display, int repMin, List<ShelfWare> wares) {}

    public record ShelfSet(String factionId, String display, String repFaction, List<String> currencies,
                           List<Shelf> shelves) {}

    public record Snapshot(Map<String, ContributionShop> shopsByFaction,
                           Map<String, ShelfSet> shelvesByFaction,
                           Map<String, Double> referenceRates,
                           Set<String> neverList,
                           double defaultReferenceRate) {
        public int shopCount() {
            return shopsByFaction.size();
        }

        public int shelfFactionCount() {
            return shelvesByFaction.size();
        }
    }
}
