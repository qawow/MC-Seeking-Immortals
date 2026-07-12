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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Faction/quest-network/chronicle indexes from text materials.
 */
public final class FactionQuestCatalogService {
    private static final Snapshot BUILTIN = loadBuiltin();

    private FactionQuestCatalogService() {}

    public static Snapshot builtin() {
        return BUILTIN;
    }

    public record Entry(String id, String display) {}

    public record Snapshot(Map<String, Entry> tiannan,
                           Map<String, Entry> dajin,
                           Map<String, Entry> mulan,
                           Map<String, Entry> chaoticSea,
                           Map<String, Entry> spiritRealmClans,
                           Map<String, Entry> humanClanNetwork,
                           Map<String, Entry> inverseStarNetwork,
                           Map<String, Entry> chronicleEvents,
                           Map<String, Entry> factionConflicts,
                           Map<String, Entry> dailyQuestTemplates,
                           Map<String, Entry> questHooks,
                           Map<String, Entry> tradeRoutes,
                           Map<String, Entry> merchantShops) {
        public int totalEntries() {
            return tiannan.size() + dajin.size() + mulan.size() + chaoticSea.size() + spiritRealmClans.size()
                    + humanClanNetwork.size() + inverseStarNetwork.size() + chronicleEvents.size()
                    + factionConflicts.size() + dailyQuestTemplates.size() + questHooks.size()
                    + tradeRoutes.size() + merchantShops.size();
        }
    }

    private static Snapshot loadBuiltin() {
        return new Snapshot(
                load("catalog/tiannan_faction_quests_index.json"),
                load("catalog/dajin_faction_quests_index.json"),
                load("catalog/mulan_faction_quests_index.json"),
                load("catalog/chaotic_sea_faction_quests_index.json"),
                load("catalog/spirit_realm_clan_quests_index.json"),
                load("catalog/human_clan_quest_network_index.json"),
                load("catalog/inverse_star_quest_network_index.json"),
                load("catalog/chronicle_events_index.json"),
                load("catalog/faction_conflict_events_index.json"),
                load("catalog/daily_quest_templates_index.json"),
                load("catalog/quest_hooks_index.json"),
                load("catalog/trade_routes_index.json"),
                load("catalog/merchant_shops_index.json")
        );
    }

    private static Map<String, Entry> load(String relative) {
        Map<String, Entry> map = new LinkedHashMap<>();
        JsonObject root = readJson("data/" + SeekingImmortalsMod.MODID + "/" + relative);
        if (root == null) {
            return Map.of();
        }
        JsonArray array = root.has("entries") && root.get("entries").isJsonArray() ? root.getAsJsonArray("entries") : new JsonArray();
        for (JsonElement element : array) {
            if (!element.isJsonObject()) continue;
            JsonObject o = element.getAsJsonObject();
            String id = str(o, "id");
            if (id.isBlank()) continue;
            map.put(id, new Entry(id, str(o, "display")));
        }
        return Collections.unmodifiableMap(map);
    }

    private static JsonObject readJson(String path) {
        try (InputStream stream = FactionQuestCatalogService.class.getClassLoader().getResourceAsStream(path)) {
            if (stream == null) return null;
            try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                return JsonParser.parseReader(reader).getAsJsonObject();
            }
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String str(JsonObject object, String key) {
        if (object == null || !object.has(key) || object.get(key).isJsonNull()) return "";
        try {
            return object.get(key).getAsString();
        } catch (Exception ignored) {
            return String.valueOf(object.get(key));
        }
    }
}
