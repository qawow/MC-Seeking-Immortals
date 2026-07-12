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
import java.util.Optional;

/**
 * Additional text-material indexes: NPC archetypes, dimensions, skill trees, factions, puppets, etc.
 */
public final class LoreCatalogService {
    private static final Snapshot BUILTIN = loadBuiltin();

    private LoreCatalogService() {}

    public static Snapshot builtin() {
        return BUILTIN;
    }

    public record Entry(String id, String display, String extra) {}

    public record Snapshot(Map<String, Entry> npcArchetypes,
                           Map<String, Entry> dimensions,
                           Map<String, Entry> skillTrees,
                           Map<String, Entry> factionNodes,
                           Map<String, Entry> puppetDefinitions,
                           Map<String, Entry> spawnTables,
                           Map<String, Entry> races,
                           Map<String, Entry> currencies,
                           Map<String, Entry> dimensionRegistry,
                           Map<String, Entry> beasts,
                           Map<String, Entry> lootTables,
                           Map<String, Entry> constitutions,
                           Map<String, Entry> spiritRootGrades,
                           Map<String, Entry> ghostStages,
                           Map<String, Entry> ascensionStages) {
        public int totalEntries() {
            return npcArchetypes.size() + dimensions.size() + skillTrees.size() + factionNodes.size()
                    + puppetDefinitions.size() + spawnTables.size() + races.size() + currencies.size()
                    + dimensionRegistry.size() + beasts.size() + lootTables.size() + constitutions.size()
                    + spiritRootGrades.size() + ghostStages.size() + ascensionStages.size();
        }

        public Optional<Entry> findNpc(String id) {
            return Optional.ofNullable(npcArchetypes.get(id == null ? "" : id));
        }

        public Optional<Entry> findBeast(String id) {
            return Optional.ofNullable(beasts.get(id == null ? "" : id));
        }
    }

    private static Snapshot loadBuiltin() {
        return new Snapshot(
                load("catalog/npc_dialogue_archetypes.json", "archetypes", "tags"),
                load("catalog/dimensions_index.json", "dimensions", "type"),
                load("catalog/skill_trees_index.json", "trees", "layer"),
                load("catalog/faction_graph_index.json", "nodes", "type"),
                load("catalog/puppet_definitions_index.json", "definitions", "tier"),
                load("catalog/spawn_tables_index.json", "tables", "entry_count"),
                load("catalog/playable_races_index.json", "races", "layer"),
                load("catalog/currency_items_index.json", "items", "tier"),
                load("catalog/dimension_registry_index.json", "dimensions", "key"),
                load("catalog/beast_bestiary_index.json", "beasts", "tier"),
                load("catalog/loot_tables_index.json", "tables", "entry_count"),
                load("catalog/constitution_index.json", "constitutions", "tier"),
                load("catalog/spirit_roots_index.json", "grades", "display"),
                load("catalog/ghost_cultivation_path_index.json", "stages", "display"),
                load("catalog/ascension_flow_index.json", "stages", "display")
        );
    }

    private static Map<String, Entry> load(String relative, String arrayKey, String extraKey) {
        Map<String, Entry> map = new LinkedHashMap<>();
        JsonObject root = readJson("data/" + SeekingImmortalsMod.MODID + "/" + relative);
        if (root == null) {
            return Map.of();
        }
        JsonArray array = root.has(arrayKey) && root.get(arrayKey).isJsonArray() ? root.getAsJsonArray(arrayKey) : new JsonArray();
        for (JsonElement element : array) {
            if (!element.isJsonObject()) continue;
            JsonObject o = element.getAsJsonObject();
            String id = str(o, "id");
            if (id.isBlank()) continue;
            map.put(id, new Entry(id, str(o, "display"), str(o, extraKey)));
        }
        return Collections.unmodifiableMap(map);
    }

    private static JsonObject readJson(String path) {
        try (InputStream stream = LoreCatalogService.class.getClassLoader().getResourceAsStream(path)) {
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
            JsonElement e = object.get(key);
            if (e.isJsonPrimitive()) return e.getAsString();
            return e.toString();
        } catch (Exception ignored) {
            return String.valueOf(object.get(key));
        }
    }
}
