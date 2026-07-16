package com.xunxian.seekingimmortals.catalog;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.xunxian.seekingimmortals.SeekingImmortalsMod;

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
import java.util.OptionalDouble;
import java.util.Set;

/**
 * Market price master + item economy tags authority for shop/auction gating and pricing hints.
 * Region slices are economy-side keys for M06; they are NOT Worldpack RegionCard records.
 */
public final class MarketPriceService {
    private static final Set<String> BLOCK_TAGS = Set.of("unique", "no_trade", "account_bound", "no_drop");
    private static final Snapshot BUILTIN = loadBuiltin();

    private MarketPriceService() {}

    public static Snapshot builtin() {
        return BUILTIN;
    }

    public static List<RegionMarket> regions() {
        return BUILTIN.regions();
    }

    public static Optional<RegionMarket> findRegion(String regionId) {
        return BUILTIN.findRegion(regionId);
    }

    public static Optional<ItemEconomyTag> findTag(String itemId) {
        return BUILTIN.findTag(canonicalEconomyId(itemId));
    }

    public static boolean isBlockedFromOpenMarket(String itemId) {
        return findTag(itemId).map(tag -> {
            for (String t : tag.tags()) {
                if (BLOCK_TAGS.contains(t)) {
                    return true;
                }
            }
            return false;
        }).orElse(false);
    }

    public static boolean isAuctionEligible(String itemId) {
        Optional<ItemEconomyTag> tag = findTag(itemId);
        if (tag.isEmpty()) {
            return true;
        }
        for (String t : tag.get().tags()) {
            if (BLOCK_TAGS.contains(t)) {
                return false;
            }
        }
        // Explicit auction=false is not used; if tags include auction or trade, allow.
        return true;
    }

    public static OptionalDouble globalModifier(String key) {
        Double value = BUILTIN.globalModifiers().get(normalize(key));
        return value == null ? OptionalDouble.empty() : OptionalDouble.of(value);
    }

    /**
     * Suggest a shop cost in low-stone units. Falls back to {@code fallback} when unknown.
     */
    public static int suggestedShopCost(String itemId, String regionId, int fallback) {
        String id = shortId(itemId);
        // 1) exact tag price
        Optional<ItemEconomyTag> tag = findTag(id);
        if (tag.isPresent() && tag.get().priceMid() > 0) {
            return Math.max(1, (int) Math.min(Integer.MAX_VALUE, tag.get().priceMid()));
        }
        // 2) region commodity by id/name
        if (regionId != null && !regionId.isBlank()) {
            Optional<RegionMarket> region = findRegion(regionId);
            if (region.isPresent()) {
                for (CommodityPrice commodity : region.get().items()) {
                    if (matches(commodity, id)) {
                        return Math.max(1, (int) Math.min(Integer.MAX_VALUE, commodity.mid()));
                    }
                }
            }
        }
        // 3) commodity master
        for (CommodityPrice commodity : BUILTIN.commodityMaster()) {
            if (matches(commodity, id)) {
                return Math.max(1, (int) Math.min(Integer.MAX_VALUE, commodity.mid()));
            }
        }
        // 4) open market align v144 by Chinese/english name match is weak; skip if no id
        // 5) economy price band guess
        Optional<String> band = EconomyPriceBandService.guessBandForItem(id);
        if (band.isPresent()) {
            return EconomyPriceBandService.suggestedCost(band.get(), fallback);
        }
        return Math.max(1, fallback);
    }

    public static int applyPricing(String itemId, String regionId, int baseCost, double routeMod, double ngMod) {
        int suggested = suggestedShopCost(itemId, regionId, baseCost);
        // Prefer explicit entry cost when suggestion only fell back to it; otherwise blend mid.
        double value = baseCost;
        if (suggested != Math.max(1, baseCost)) {
            // average entry cost with master mid to avoid wild swings on partial catalogs
            value = (baseCost + suggested) / 2.0D;
        }
        value *= routeMod <= 0.0D ? 1.0D : routeMod;
        value *= ngMod <= 0.0D ? 1.0D : ngMod;
        return Math.max(1, (int) Math.round(value));
    }

    public record CommodityPrice(String nameOrId, long min, long max) {
        public long mid() {
            if (max > min) {
                return (min + max) / 2L;
            }
            return Math.max(1L, min);
        }
    }

    public record RegionMarket(String id, String display, String band, List<CommodityPrice> items) {}

    public record ItemEconomyTag(String id, String display, Set<String> tags, long priceMin, long priceMax) {
        public long priceMid() {
            if (priceMin <= 0 && priceMax <= 0) {
                return 0L;
            }
            if (priceMax > priceMin) {
                return (priceMin + priceMax) / 2L;
            }
            return Math.max(priceMin, priceMax);
        }
    }

    public record Snapshot(List<RegionMarket> regions, List<CommodityPrice> commodityMaster,
                           Map<String, ItemEconomyTag> tags, Map<String, Double> globalModifiers,
                           Map<String, long[]> openMarketAlign) {
        public Optional<RegionMarket> findRegion(String regionId) {
            String id = normalize(regionId);
            return regions.stream().filter(r -> r.id().equals(id)).findFirst();
        }

        public Optional<ItemEconomyTag> findTag(String itemId) {
            return Optional.ofNullable(tags.get(shortId(itemId)));
        }

        public int taggedItemCount() {
            return tags.size();
        }

        public int blockedItemCount() {
            int count = 0;
            for (ItemEconomyTag tag : tags.values()) {
                for (String t : tag.tags()) {
                    if (BLOCK_TAGS.contains(t)) {
                        count++;
                        break;
                    }
                }
            }
            return count;
        }
    }

    private static boolean matches(CommodityPrice commodity, String itemId) {
        String key = normalize(commodity.nameOrId());
        String id = shortId(itemId);
        return key.equals(id) || key.contains(id) || id.contains(key);
    }

    private static Snapshot loadBuiltin() {
        JsonObject master = readJson(path("text_material/market_price_master_v100.json"));
        JsonObject tagsRoot = readJson(path("text_material/item_economy_tags_v101.json"));
        JsonObject align = readJson(path("text_material/market_shelf_price_align_v144.json"));

        Map<String, Double> modifiers = new LinkedHashMap<>();
        List<RegionMarket> regions = new ArrayList<>();
        List<CommodityPrice> commodities = new ArrayList<>();
        if (master != null) {
            if (master.has("global_modifiers") && master.get("global_modifiers").isJsonObject()) {
                for (Map.Entry<String, JsonElement> entry : master.getAsJsonObject("global_modifiers").entrySet()) {
                    try {
                        modifiers.put(normalize(entry.getKey()), entry.getValue().getAsDouble());
                    } catch (Exception ignored) {
                    }
                }
            }
            for (JsonElement element : array(master, "regions")) {
                if (!element.isJsonObject()) {
                    continue;
                }
                JsonObject o = element.getAsJsonObject();
                String id = normalize(str(o, "id"));
                if (id.isBlank()) {
                    continue;
                }
                List<CommodityPrice> items = new ArrayList<>();
                for (JsonElement itemEl : array(o, "items")) {
                    parseCommodity(itemEl).ifPresent(items::add);
                }
                regions.add(new RegionMarket(id, str(o, "display"), str(o, "band"), List.copyOf(items)));
            }
            for (JsonElement element : array(master, "commodity_master")) {
                parseCommodity(element).ifPresent(commodities::add);
            }
        }

        Map<String, ItemEconomyTag> tags = new LinkedHashMap<>();
        if (tagsRoot != null) {
            for (JsonElement element : array(tagsRoot, "items")) {
                if (!element.isJsonObject()) {
                    continue;
                }
                JsonObject o = element.getAsJsonObject();
                String id = shortId(str(o, "id"));
                if (id.isBlank()) {
                    continue;
                }
                Set<String> tagSet = new LinkedHashSet<>();
                for (JsonElement t : array(o, "tags")) {
                    try {
                        tagSet.add(normalize(t.getAsString()));
                    } catch (Exception ignored) {
                    }
                }
                long min = 0L;
                long max = 0L;
                if (o.has("price") && !o.get("price").isJsonNull()) {
                    JsonElement price = o.get("price");
                    if (price.isJsonArray() && price.getAsJsonArray().size() > 0) {
                        try {
                            min = price.getAsJsonArray().get(0).getAsLong();
                            max = price.getAsJsonArray().size() > 1
                                    ? price.getAsJsonArray().get(1).getAsLong()
                                    : min;
                        } catch (Exception ignored) {
                        }
                    } else if (price.isJsonPrimitive() && price.getAsJsonPrimitive().isNumber()) {
                        min = max = price.getAsLong();
                    }
                }
                tags.put(id, new ItemEconomyTag(id, str(o, "display"), Set.copyOf(tagSet), min, max));
            }
        }

        Map<String, long[]> openAlign = new LinkedHashMap<>();
        if (align != null) {
            for (JsonElement element : array(align, "open_market")) {
                if (!element.isJsonObject()) {
                    continue;
                }
                JsonObject o = element.getAsJsonObject();
                String name = str(o, "name");
                long[] range = priceRange(o.get("price_low_stone"));
                if (!name.isBlank() && range != null) {
                    openAlign.put(normalize(name), range);
                }
            }
        }

        return new Snapshot(List.copyOf(regions), List.copyOf(commodities),
                Collections.unmodifiableMap(tags), Collections.unmodifiableMap(modifiers),
                Collections.unmodifiableMap(openAlign));
    }

    private static Optional<CommodityPrice> parseCommodity(JsonElement element) {
        if (element == null || !element.isJsonObject()) {
            return Optional.empty();
        }
        JsonObject o = element.getAsJsonObject();
        String name = str(o, "name");
        if (name.isBlank()) {
            name = str(o, "id");
        }
        if (name.isBlank()) {
            return Optional.empty();
        }
        long min = 0L;
        long max = 0L;
        if (o.has("price")) {
            JsonElement price = o.get("price");
            if (price.isJsonArray() && price.getAsJsonArray().size() > 0) {
                try {
                    min = price.getAsJsonArray().get(0).getAsLong();
                    max = price.getAsJsonArray().size() > 1 ? price.getAsJsonArray().get(1).getAsLong() : min;
                } catch (Exception ignored) {
                }
            } else if (price.isJsonPrimitive() && price.getAsJsonPrimitive().isNumber()) {
                min = max = price.getAsLong();
            }
        }
        return Optional.of(new CommodityPrice(name, min, max));
    }

    private static long[] priceRange(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return null;
        }
        try {
            if (element.isJsonArray() && element.getAsJsonArray().size() > 0) {
                long min = element.getAsJsonArray().get(0).getAsLong();
                long max = element.getAsJsonArray().size() > 1
                        ? element.getAsJsonArray().get(1).getAsLong()
                        : min;
                return new long[]{min, max};
            }
            if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) {
                long v = element.getAsLong();
                return new long[]{v, v};
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static String path(String relative) {
        return "data/" + SeekingImmortalsMod.MODID + "/" + relative;
    }

    private static JsonObject readJson(String path) {
        try (InputStream stream = MarketPriceService.class.getClassLoader().getResourceAsStream(path)) {
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

    private static String canonicalEconomyId(String itemId) {
        return switch (shortId(itemId)) {
            case "palm_heaven_bottle", "palm_sky_bottle", "heaven_palm_vase" -> "palm_bottle";
            case "green_liquid_drop", "garden_liquid", "lv_ye", "mystic_green_liquid" -> "green_liquid";
            default -> shortId(itemId);
        };
    }
}
