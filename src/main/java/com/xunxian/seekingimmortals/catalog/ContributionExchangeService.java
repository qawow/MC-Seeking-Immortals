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
 * Contribution / merit exchange framework for M08 instantiation.
 * Red line: contribution/merit is NEVER infinitely bidirectional with spirit stones.
 * This service provides valuation hints and non-stone currency catalogs only.
 */
public final class ContributionExchangeService {
    private static final Snapshot BUILTIN = loadBuiltin();

    private ContributionExchangeService() {}

    public static Snapshot builtin() {
        return BUILTIN;
    }

    /**
     * Always false. DESIGNER_HANDBOOK §5: no infinite contribution↔stone swap loop.
     */
    public static boolean isInfiniteStoneSwapAllowed() {
        return false;
    }

    public static double lowStonePerContribution(String factionId) {
        return BUILTIN.findRate(factionId)
                .map(FactionRate::lowStonePerContribution)
                .orElse(BUILTIN.defaultLowStonePerContribution());
    }

    /** Valuation only — does not mutate inventories or contribution balances. */
    public static long estimateLowStoneValue(String factionId, int contributionPoints) {
        if (contributionPoints <= 0) {
            return 0L;
        }
        double rate = lowStonePerContribution(factionId);
        return Math.max(0L, Math.round(contributionPoints * rate));
    }

    public static Optional<Long> itemEquivMid(String itemKey) {
        long[] range = BUILTIN.itemEquiv().get(normalize(itemKey));
        if (range == null) {
            return Optional.empty();
        }
        long mid = range[1] > range[0] ? (range[0] + range[1]) / 2L : Math.max(range[0], range[1]);
        return Optional.of(Math.max(0L, mid));
    }

    public static List<MeritCatalogItem> catalogFor(String currencyOrFaction) {
        String key = normalize(currencyOrFaction);
        if (key.isBlank()) {
            return List.of();
        }
        List<MeritCatalogItem> direct = BUILTIN.catalogs().get(key);
        if (direct != null) {
            return direct;
        }
        // faction aliases → currency catalogs
        if (key.contains("tianyuan")) {
            return BUILTIN.catalogs().getOrDefault("merit_points", List.of());
        }
        if (key.contains("star_palace") || key.contains("patrol")) {
            return BUILTIN.catalogs().getOrDefault("patrol_merit", List.of());
        }
        if (key.contains("inverse") || key.contains("smuggle")) {
            return BUILTIN.catalogs().getOrDefault("smuggle_credit", List.of());
        }
        return List.of();
    }

    public static List<FactionRate> factionRates() {
        return BUILTIN.factionRates();
    }

    public record FactionRate(String factionId, double lowStonePerContribution, String note) {}

    public record MeritCatalogItem(String id, int cost, String currency) {}

    public record Snapshot(double defaultLowStonePerContribution, List<FactionRate> factionRates,
                           Map<String, long[]> itemEquiv, Map<String, List<MeritCatalogItem>> catalogs) {
        public Optional<FactionRate> findRate(String factionId) {
            String id = normalize(factionId);
            return factionRates.stream().filter(r -> r.factionId().equals(id)).findFirst();
        }
    }

    private static Snapshot loadBuiltin() {
        JsonObject root = readJson(path("text_material/economy_contribution_exchange.json"));
        if (root == null) {
            root = readJson(path("catalog/economy_contribution_exchange.json"));
        }
        double defaultRate = 1.0D;
        List<FactionRate> rates = new ArrayList<>();
        Map<String, long[]> itemEquiv = new LinkedHashMap<>();
        Map<String, List<MeritCatalogItem>> catalogs = new LinkedHashMap<>();
        if (root != null) {
            if (root.has("default_rate") && root.get("default_rate").isJsonObject()) {
                JsonObject dr = root.getAsJsonObject("default_rate");
                // corpus: contribution_per_low_stone ≈ 1.0 means 1 contrib ~ 1 low stone
                double cpls = asDouble(dr, "contribution_per_low_stone", 1.0D);
                defaultRate = cpls <= 0.0D ? 1.0D : (1.0D / cpls);
            }
            for (JsonElement element : array(root, "faction_rates")) {
                if (!element.isJsonObject()) {
                    continue;
                }
                JsonObject o = element.getAsJsonObject();
                String id = normalize(str(o, "faction_id"));
                if (id.isBlank()) {
                    continue;
                }
                rates.add(new FactionRate(id, asDouble(o, "low_stone_per_contribution", defaultRate), str(o, "note")));
            }
            if (root.has("item_equiv_low_stone") && root.get("item_equiv_low_stone").isJsonObject()) {
                for (Map.Entry<String, JsonElement> entry : root.getAsJsonObject("item_equiv_low_stone").entrySet()) {
                    if (!entry.getValue().isJsonObject()) {
                        continue;
                    }
                    JsonObject o = entry.getValue().getAsJsonObject();
                    long min = asLong(o, "min");
                    long max = asLong(o, "max");
                    itemEquiv.put(normalize(entry.getKey()), new long[]{min, max});
                }
            }
            putCatalog(catalogs, root, "tianyuan", "merit_points");
            putCatalog(catalogs, root, "star_palace", "patrol_merit");
            putCatalog(catalogs, root, "inverse_star", "smuggle_credit");
        }
        return new Snapshot(defaultRate, List.copyOf(rates), Collections.unmodifiableMap(itemEquiv),
                Collections.unmodifiableMap(catalogs));
    }

    private static void putCatalog(Map<String, List<MeritCatalogItem>> catalogs, JsonObject root,
                                   String sectionKey, String defaultCurrency) {
        if (!root.has(sectionKey) || !root.get(sectionKey).isJsonObject()) {
            return;
        }
        JsonObject section = root.getAsJsonObject(sectionKey);
        String currency = str(section, "currency");
        if (currency.isBlank()) {
            currency = defaultCurrency;
        }
        List<MeritCatalogItem> items = new ArrayList<>();
        for (JsonElement element : array(section, "items")) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject o = element.getAsJsonObject();
            String id = str(o, "id");
            if (id.isBlank()) {
                continue;
            }
            int cost = asInt(o, "cost");
            if (cost <= 0) {
                cost = asInt(o, "cost_patrol_merit");
            }
            if (cost <= 0) {
                cost = asInt(o, "cost_merit");
            }
            if (cost <= 0) {
                continue;
            }
            items.add(new MeritCatalogItem(id, cost, currency));
        }
        catalogs.put(normalize(currency), List.copyOf(items));
        catalogs.put(normalize(sectionKey), List.copyOf(items));
    }

    private static String path(String relative) {
        return "data/" + SeekingImmortalsMod.MODID + "/" + relative;
    }

    private static JsonObject readJson(String path) {
        try (InputStream stream = ContributionExchangeService.class.getClassLoader().getResourceAsStream(path)) {
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

    private static long asLong(JsonObject o, String key) {
        if (o == null || !o.has(key) || !o.get(key).isJsonPrimitive()) {
            return 0L;
        }
        try {
            return o.get(key).getAsLong();
        } catch (Exception ignored) {
            return 0L;
        }
    }

    private static double asDouble(JsonObject o, String key, double fallback) {
        if (o == null || !o.has(key) || !o.get(key).isJsonPrimitive()) {
            return fallback;
        }
        try {
            return o.get(key).getAsDouble();
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
