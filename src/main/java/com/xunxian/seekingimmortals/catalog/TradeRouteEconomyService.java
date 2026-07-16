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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Trade-route price-spread authority for shops and future M06 region connectivity consumers.
 * Field contract (stable for M06):
 * <ul>
 *   <li>{@code regionId} — snake_case corpus region slug ({@code tiannan}, {@code chaotic_sea}, ...)</li>
 *   <li>{@code TradeRoute.fromRegion}/{@code toRegion} — endpoints</li>
 *   <li>{@code feeLowStone} — embark fee in low-stone units</li>
 *   <li>{@code priceModifier(regionId, goodsHint)} — import premium heuristic for open-market shelves</li>
 * </ul>
 */
public final class TradeRouteEconomyService {
    private static final Snapshot BUILTIN = loadBuiltin();

    private TradeRouteEconomyService() {}

    public static Snapshot builtin() {
        return BUILTIN;
    }

    public static List<TradeRoute> routes() {
        return BUILTIN.routes();
    }

    public static List<AuctionHub> auctionHubs() {
        return BUILTIN.auctionHubs();
    }

    public static Optional<TradeRoute> find(String routeId) {
        return BUILTIN.find(routeId);
    }

    public static List<TradeRoute> from(String regionId) {
        String id = normalize(regionId);
        List<TradeRoute> list = new ArrayList<>();
        for (TradeRoute route : BUILTIN.routes()) {
            if (route.fromRegion().equals(id) || route.toRegion().equals(id)) {
                list.add(route);
            }
        }
        return List.copyOf(list);
    }

    public static int embarkFeeLowStone(String routeId) {
        return find(routeId).map(TradeRoute::feeLowStone).orElse(0);
    }

    /**
     * Heuristic shelf price modifier for a shop region.
     * Import-heavy border hubs get a small premium; pure internal routes stay near 1.0.
     */
    public static double priceModifier(String shopRegionId, String goodsHint) {
        String region = normalize(shopRegionId);
        if (region.isBlank()) {
            return 1.0D;
        }
        double mod = 1.0D;
        String goods = normalize(goodsHint);
        int touch = 0;
        int importHits = 0;
        int exportHits = 0;
        int feeSum = 0;
        for (TradeRoute route : BUILTIN.routes()) {
            boolean fromHere = route.fromRegion().equals(region);
            boolean toHere = route.toRegion().equals(region);
            if (!fromHere && !toHere) {
                continue;
            }
            touch++;
            feeSum += Math.max(0, route.feeLowStone());
            if (!goods.isBlank()) {
                if (toHere && containsToken(route.goodsImport(), goods)) {
                    importHits++;
                }
                if (fromHere && containsToken(route.goodsExport(), goods)) {
                    exportHits++;
                }
            }
        }
        if (touch <= 0) {
            return 1.0D;
        }
        // Longer / costlier routes imply higher local shelf prices for imports.
        double avgFee = feeSum / (double) touch;
        mod += Math.min(0.25D, avgFee / 400.0D);
        if (importHits > exportHits) {
            mod += 0.08D * Math.min(3, importHits - exportHits);
        } else if (exportHits > importHits) {
            mod -= 0.04D * Math.min(3, exportHits - importHits);
        }
        // Known high-cost hubs.
        if ("dajin".equals(region) || "tianyuan".equals(region)) {
            mod += 0.05D;
        }
        if ("chaotic_sea".equals(region)) {
            mod += 0.03D;
        }
        return Math.max(0.75D, Math.min(1.75D, mod));
    }

    public static Optional<String> shopRegion(String shopId) {
        return Optional.ofNullable(BUILTIN.shopRegions().get(normalize(shopId)));
    }

    public record TradeRoute(String id, String display, String fromRegion, String toRegion,
                             List<String> transport, int durationDays, int feeLowStone,
                             List<String> riskEvents, List<String> goodsExport, List<String> goodsImport) {}

    public record AuctionHub(String region, String name, String tier) {}

    public record Snapshot(List<TradeRoute> routes, List<AuctionHub> auctionHubs, Map<String, String> shopRegions) {
        public Optional<TradeRoute> find(String routeId) {
            String id = normalize(routeId);
            return routes.stream().filter(r -> r.id().equals(id)).findFirst();
        }

        public int routeCount() {
            return routes.size();
        }
    }

    private static boolean containsToken(List<String> list, String token) {
        for (String value : list) {
            String v = normalize(value);
            if (v.contains(token) || token.contains(v)) {
                return true;
            }
        }
        return false;
    }

    private static Snapshot loadBuiltin() {
        JsonObject root = readJson(path("text_material/trade_routes.json"));
        List<TradeRoute> routes = new ArrayList<>();
        List<AuctionHub> hubs = new ArrayList<>();
        if (root != null) {
            for (JsonElement element : array(root, "routes")) {
                if (!element.isJsonObject()) {
                    continue;
                }
                JsonObject o = element.getAsJsonObject();
                String id = normalize(str(o, "id"));
                if (id.isBlank()) {
                    continue;
                }
                routes.add(new TradeRoute(
                        id,
                        str(o, "display"),
                        normalize(str(o, "from")),
                        normalize(str(o, "to")),
                        stringList(o.get("transport")),
                        asInt(o, "duration_days"),
                        asInt(o, "fee_low_stone"),
                        stringList(o.get("risk_events")),
                        stringList(o.get("goods_export")),
                        stringList(o.get("goods_import"))));
            }
            for (JsonElement element : array(root, "auction_hubs")) {
                if (!element.isJsonObject()) {
                    continue;
                }
                JsonObject o = element.getAsJsonObject();
                hubs.add(new AuctionHub(normalize(str(o, "region")), str(o, "name"), str(o, "tier")));
            }
        }

        Map<String, String> shopRegions = new LinkedHashMap<>();
        JsonObject shopsRoot = readJson(path("text_material/merchant_shops.json"));
        if (shopsRoot != null) {
            for (JsonElement element : array(shopsRoot, "shops")) {
                if (!element.isJsonObject()) {
                    continue;
                }
                JsonObject o = element.getAsJsonObject();
                String id = normalize(str(o, "id"));
                String region = normalize(str(o, "region"));
                if (!id.isBlank() && !region.isBlank()) {
                    shopRegions.put(id, region);
                }
            }
        }

        return new Snapshot(List.copyOf(routes), List.copyOf(hubs), Collections.unmodifiableMap(shopRegions));
    }

    private static List<String> stringList(JsonElement element) {
        if (element == null || !element.isJsonArray()) {
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

    private static String path(String relative) {
        return "data/" + SeekingImmortalsMod.MODID + "/" + relative;
    }

    private static JsonObject readJson(String path) {
        try (InputStream stream = TradeRouteEconomyService.class.getClassLoader().getResourceAsStream(path)) {
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

    private static int asInt(JsonObject o, String key) {
        if (o == null || !o.has(key) || !o.get(key).isJsonPrimitive()) {
            return 0;
        }
        try {
            return o.get(key).getAsInt();
        } catch (Exception ignored) {
            return 0;
        }
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
