package com.xunxian.seekingimmortals.artifact;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
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
import java.util.Objects;
import java.util.Optional;

/**
 * M15: shipped artifact corpus authority (217 catalog + tier/synergy/drops/auction/draft).
 * Reads {@code data/seeking_immortals/artifacts/} only.
 */
public final class ArtifactDataService {
    private static final String ROOT = "data/seeking_immortals/artifacts/";
    private static final List<String> SOURCE_FILES = List.of(
            "artifacts_catalog.json",
            "artifact_tier_rules.json",
            "artifact_eleven_tier_map.json",
            "artifact_taxonomy_111.json",
            "artifact_tier_map.json",
            "item_synergy.json",
            "refinement_system.json",
            "refinement_recipes.json",
            "refine_manual_index.json",
            "refinement_failure_loot.json",
            "talisman_treasure_templates.json",
            "ancient_treasure_index.json",
            "artifact_realm_drops.json",
            "artifact_faction_specialty.json",
            "wanbao_auction_artifacts.json",
            "flight_vehicles.json",
            "forge_artifact_priority.json",
            "moditems_artifacts_draft.json"
    );

    private ArtifactDataService() {}

    public static Snapshot builtin() {
        return Holder.BUILTIN;
    }

    public static List<String> sourceFiles() {
        return SOURCE_FILES;
    }

    private static Snapshot loadBuiltin() {
        Map<String, JsonObject> roots = new LinkedHashMap<>();
        Map<String, Integer> entryCounts = new LinkedHashMap<>();
        for (String file : SOURCE_FILES) {
            JsonObject root = readObject(file);
            roots.put(file, root);
            entryCounts.put(file, countEntries(root));
        }

        Map<String, ArtifactDefinition> artifacts = parseArtifacts(roots.get("artifacts_catalog.json"));
        Map<String, TierRule> tierRules = parseTierRules(roots.get("artifact_tier_rules.json"));
        RealmPowerScale realmPowerScale = parseRealmPowerScale(roots.get("artifact_tier_rules.json"));
        Map<String, RefinementRecipe> recipes = parseRecipes(roots.get("refinement_recipes.json"));
        Map<String, FlightVehicle> vehicles = parseVehicles(roots.get("flight_vehicles.json"));
        Map<String, List<String>> priorities = parsePriorities(roots.get("forge_artifact_priority.json"));
        Map<String, TalismanTreasureTemplate> talismanTemplates =
                parseTalismanTemplates(roots.get("talisman_treasure_templates.json"));
        FailureLootTable failureLoot = parseFailureLoot(roots.get("refinement_failure_loot.json"));
        Map<Integer, ElevenTier> elevenTiers = parseElevenTier(roots.get("artifact_eleven_tier_map.json"));
        Map<String, Integer> elevenIdMap = parseElevenIdMap(roots.get("artifact_eleven_tier_map.json"));
        Map<String, TaxonomySection> taxonomy = parseTaxonomy(roots.get("artifact_taxonomy_111.json"));
        Map<Integer, GradeBand> gradeBands = parseGradeBands(roots.get("artifact_tier_map.json"));
        Map<String, int[]> tierTagToGrade = parseTierTagToGrade(roots.get("artifact_tier_map.json"));
        List<SynergyRule> synergies = parseSynergies(roots.get("item_synergy.json"));
        List<ArtifactCombo> combos = parseCombos(roots.get("item_synergy.json"));
        Map<String, List<DropEntry>> realmDrops = parseRealmDrops(roots.get("artifact_realm_drops.json"));
        Map<String, FactionSpecialty> factionSpecialties =
                parseFactionSpecialties(roots.get("artifact_faction_specialty.json"));
        List<AuctionStock> wanbaoStock = parseWanbaoStock(roots.get("wanbao_auction_artifacts.json"));
        List<AuctionLot> auctionLots = parseAuctionLots(roots.get("wanbao_auction_artifacts.json"));
        List<DraftItem> draftItems = parseDraft(roots.get("moditems_artifacts_draft.json"));
        Map<String, AncientEntry> ancientEntries = parseAncient(roots.get("ancient_treasure_index.json"));

        return new Snapshot(artifacts, tierRules, realmPowerScale, recipes, vehicles, priorities,
                talismanTemplates, failureLoot, elevenTiers, elevenIdMap, taxonomy, gradeBands,
                tierTagToGrade, synergies, combos, realmDrops, factionSpecialties, wanbaoStock,
                auctionLots, draftItems, ancientEntries, entryCounts);
    }

    private static JsonObject readObject(String file) {
        String path = ROOT + file;
        try (InputStream stream = ArtifactDataService.class.getClassLoader().getResourceAsStream(path)) {
            if (stream == null) {
                throw new IllegalStateException("Missing shipped artifact data file: " + path);
            }
            try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                JsonElement element = JsonParser.parseReader(reader);
                if (!element.isJsonObject()) {
                    throw new IllegalStateException("Artifact data file must be a JSON object: " + path);
                }
                return element.getAsJsonObject();
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read shipped artifact data file: " + path, exception);
        }
    }

    private static Map<String, ArtifactDefinition> parseArtifacts(JsonObject root) {
        Map<String, ArtifactDefinition> artifacts = new LinkedHashMap<>();
        JsonArray array = getArray(root, "artifacts");
        for (JsonElement element : array) {
            JsonObject object = asObject(element, "artifact");
            String id = getString(object, "id", "");
            if (id.isBlank()) {
                continue;
            }
            int gameTier = getInt(object, "game_tier", getInt(object, "artifact_grade", 0));
            String compliance = "";
            if (object.has("compliance")) {
                JsonElement c = object.get("compliance");
                compliance = c.isJsonPrimitive() ? c.getAsString() : c.toString();
            }
            artifacts.put(id, new ArtifactDefinition(
                    id,
                    getString(object, "display", id),
                    getString(object, "tier", ""),
                    getString(object, "type", ""),
                    getString(object, "effect", ""),
                    getString(object, "realm_min", ""),
                    gameTier,
                    stringList(object.get("tags")),
                    getString(object, "element", ""),
                    getString(object, "binds", ""),
                    compliance,
                    object.has("consumable") && object.get("consumable").isJsonPrimitive()
                            && object.get("consumable").getAsBoolean(),
                    getInt(object, "uses", 0)
            ));
        }
        return unmodifiableMap(artifacts);
    }

    private static Map<String, TierRule> parseTierRules(JsonObject root) {
        Map<String, TierRule> rules = new LinkedHashMap<>();
        for (JsonElement element : getArray(root, "tiers")) {
            JsonObject object = asObject(element, "tier rule");
            String id = getString(object, "id", "");
            if (!id.isBlank()) {
                rules.put(id, new TierRule(
                        id,
                        getString(object, "display", id),
                        getString(object, "realm_typical", ""),
                        getString(object, "refine_cost_band", "")
                ));
            }
        }
        return unmodifiableMap(rules);
    }

    private static RealmPowerScale parseRealmPowerScale(JsonObject root) {
        JsonObject object = getObject(root, "realm_power_scale");
        return new RealmPowerScale(
                getDouble(object, "below_realm_min", 0.25D),
                getDouble(object, "at_realm_min", 0.7D),
                getDouble(object, "two_major_above", 1.0D)
        );
    }

    private static Map<String, RefinementRecipe> parseRecipes(JsonObject root) {
        Map<String, RefinementRecipe> recipes = new LinkedHashMap<>();
        for (JsonElement element : getArray(root, "recipes")) {
            JsonObject object = asObject(element, "refinement recipe");
            String id = getString(object, "id", "");
            if (id.isBlank()) {
                continue;
            }
            recipes.put(id, new RefinementRecipe(
                    id,
                    getString(object, "artifact_id", ""),
                    getString(object, "display", id),
                    getString(object, "tier", ""),
                    getString(object, "realm_min", ""),
                    getInt(object, "forge_grade", 0),
                    getDouble(object, "base_success_rate", 0.0D),
                    parseMaterialRequirements(object)
            ));
        }
        return unmodifiableMap(recipes);
    }

    private static List<MaterialRequirement> parseMaterialRequirements(JsonObject recipe) {
        List<MaterialRequirement> materials = new ArrayList<>();
        for (JsonElement element : getArray(recipe, "materials")) {
            JsonObject object = asObject(element, "refinement material");
            String id = getString(object, "id", "");
            int count = getInt(object, "count", 0);
            if (!id.isBlank() && count > 0) {
                materials.add(new MaterialRequirement(id, count));
            }
        }
        return List.copyOf(materials);
    }

    private static Map<String, FlightVehicle> parseVehicles(JsonObject root) {
        Map<String, FlightVehicle> vehicles = new LinkedHashMap<>();
        for (JsonElement element : getArray(root, "vehicles")) {
            JsonObject object = asObject(element, "flight vehicle");
            String id = getString(object, "id", "");
            if (id.isBlank()) {
                continue;
            }
            vehicles.put(id, new FlightVehicle(
                    id,
                    getString(object, "display", id),
                    getString(object, "tier", ""),
                    getString(object, "realm_min", ""),
                    getDouble(object, "speed", 0.0D),
                    getString(object, "fuel", "")
            ));
        }
        return unmodifiableMap(vehicles);
    }

    private static Map<String, List<String>> parsePriorities(JsonObject root) {
        Map<String, List<String>> priorities = new LinkedHashMap<>();
        for (String tier : stringList(root.get("priority_tiers"))) {
            priorities.put(tier, stringList(root.get(tier)));
        }
        return unmodifiableMap(priorities);
    }

    private static Map<String, TalismanTreasureTemplate> parseTalismanTemplates(JsonObject root) {
        Map<String, TalismanTreasureTemplate> templates = new LinkedHashMap<>();
        int defaultUses = getInt(getObject(root, "rules"), "default_uses", 3);
        for (JsonElement element : getArray(root, "templates")) {
            JsonObject object = asObject(element, "talisman treasure template");
            String id = getString(object, "id", "");
            if (!id.isBlank()) {
                templates.put(id, new TalismanTreasureTemplate(
                        id,
                        getString(object, "display", id),
                        getString(object, "element", ""),
                        defaultUses
                ));
            }
        }
        return unmodifiableMap(templates);
    }

    private static FailureLootTable parseFailureLoot(JsonObject root) {
        List<FailureLootEntry> defaults = parseFailureLootEntries(getArray(root, "default"));
        Map<String, List<FailureLootEntry>> byTier = new LinkedHashMap<>();
        JsonObject tiers = getObject(root, "by_tier");
        for (Map.Entry<String, JsonElement> entry : tiers.entrySet()) {
            if (entry.getValue().isJsonArray()) {
                byTier.put(entry.getKey(), parseFailureLootEntries(entry.getValue().getAsJsonArray()));
            }
        }
        return new FailureLootTable(defaults, byTier);
    }

    private static List<FailureLootEntry> parseFailureLootEntries(JsonArray entries) {
        List<FailureLootEntry> loot = new ArrayList<>();
        for (JsonElement element : entries) {
            JsonObject object = asObject(element, "refinement failure loot");
            String id = getString(object, "id", "");
            int weight = getInt(object, "weight", 0);
            int countMin = getInt(object, "count_min", 0);
            int countMax = getInt(object, "count_max", countMin);
            if (!id.isBlank() && weight > 0 && countMax >= countMin) {
                loot.add(new FailureLootEntry(id, weight, countMin, countMax));
            }
        }
        return List.copyOf(loot);
    }

    private static Map<Integer, ElevenTier> parseElevenTier(JsonObject root) {
        Map<Integer, ElevenTier> map = new LinkedHashMap<>();
        for (JsonElement element : getArray(root, "game_tiers")) {
            JsonObject object = asObject(element, "eleven tier");
            int tier = getInt(object, "tier", 0);
            if (tier <= 0) {
                continue;
            }
            map.put(tier, new ElevenTier(
                    tier,
                    getString(object, "display", "T" + tier),
                    getString(object, "mod_tier", ""),
                    getString(object, "realm_typical", ""),
                    stringList(object.get("examples"))
            ));
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(map));
    }

    private static Map<String, Integer> parseElevenIdMap(JsonObject root) {
        Map<String, Integer> map = new LinkedHashMap<>();
        JsonObject object = getObject(root, "artifact_id_map");
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            if (entry.getValue() != null && entry.getValue().isJsonPrimitive()
                    && entry.getValue().getAsJsonPrimitive().isNumber()) {
                map.put(entry.getKey(), entry.getValue().getAsInt());
            }
        }
        return unmodifiableMap(map);
    }

    private static Map<String, TaxonomySection> parseTaxonomy(JsonObject root) {
        Map<String, TaxonomySection> map = new LinkedHashMap<>();
        JsonObject sections = getObject(root, "sections");
        for (Map.Entry<String, JsonElement> entry : sections.entrySet()) {
            if (!entry.getValue().isJsonObject()) {
                continue;
            }
            JsonObject object = entry.getValue().getAsJsonObject();
            map.put(entry.getKey(), new TaxonomySection(
                    entry.getKey(),
                    getString(object, "display", entry.getKey()),
                    stringList(object.get("types")),
                    stringList(object.get("examples"))
            ));
        }
        return unmodifiableMap(map);
    }

    private static Map<Integer, GradeBand> parseGradeBands(JsonObject root) {
        Map<Integer, GradeBand> map = new LinkedHashMap<>();
        for (JsonElement element : getArray(root, "grades")) {
            JsonObject object = asObject(element, "grade band");
            int grade = getInt(object, "grade", 0);
            if (grade <= 0) {
                continue;
            }
            map.put(grade, new GradeBand(
                    grade,
                    getString(object, "display", "G" + grade),
                    getString(object, "realm_equiv", ""),
                    getString(object, "loot_band", "")
            ));
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(map));
    }

    private static Map<String, int[]> parseTierTagToGrade(JsonObject root) {
        Map<String, int[]> map = new LinkedHashMap<>();
        JsonObject object = getObject(root, "tier_tag_to_grade");
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            if (!entry.getValue().isJsonArray()) {
                continue;
            }
            JsonArray arr = entry.getValue().getAsJsonArray();
            int min = arr.size() > 0 && arr.get(0).isJsonPrimitive() ? arr.get(0).getAsInt() : 0;
            int max = arr.size() > 1 && arr.get(1).isJsonPrimitive() ? arr.get(1).getAsInt() : min;
            map.put(entry.getKey(), new int[]{min, max});
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(map));
    }

    private static List<SynergyRule> parseSynergies(JsonObject root) {
        List<SynergyRule> list = new ArrayList<>();
        for (JsonElement element : getArray(root, "synergies")) {
            JsonObject object = asObject(element, "synergy");
            List<String> items = stringList(object.get("items"));
            if (items.size() < 2) {
                continue;
            }
            list.add(new SynergyRule(items, getString(object, "relation", ""), getString(object, "note", "")));
        }
        return List.copyOf(list);
    }

    private static List<ArtifactCombo> parseCombos(JsonObject root) {
        List<ArtifactCombo> list = new ArrayList<>();
        for (JsonElement element : getArray(root, "artifact_combos")) {
            JsonObject object = asObject(element, "artifact combo");
            List<String> arts = stringList(object.get("artifacts"));
            if (arts.size() < 2) {
                continue;
            }
            list.add(new ArtifactCombo(arts, getString(object, "bonus", ""), getString(object, "note", "")));
        }
        return List.copyOf(list);
    }

    private static Map<String, List<DropEntry>> parseRealmDrops(JsonObject root) {
        Map<String, List<DropEntry>> map = new LinkedHashMap<>();
        JsonObject realms = getObject(root, "realms");
        for (Map.Entry<String, JsonElement> entry : realms.entrySet()) {
            if (!entry.getValue().isJsonObject()) {
                continue;
            }
            List<DropEntry> pool = new ArrayList<>();
            for (JsonElement el : getArray(entry.getValue().getAsJsonObject(), "artifact_pool")) {
                JsonObject object = asObject(el, "drop");
                String id = getString(object, "id", "");
                if (!id.isBlank()) {
                    pool.add(new DropEntry(id, getInt(object, "weight", 1), getString(object, "tier", "")));
                }
            }
            map.put(entry.getKey(), List.copyOf(pool));
        }
        // diyuan layers + barbarian territories also contribute pools
        JsonObject diyuan = getObject(root, "diyuan_by_layer");
        for (Map.Entry<String, JsonElement> entry : diyuan.entrySet()) {
            if (!entry.getValue().isJsonObject()) {
                continue;
            }
            List<DropEntry> pool = new ArrayList<>();
            for (JsonElement el : getArray(entry.getValue().getAsJsonObject(), "artifact_pool")) {
                JsonObject object = asObject(el, "drop");
                String id = getString(object, "id", "");
                if (!id.isBlank()) {
                    pool.add(new DropEntry(id, getInt(object, "weight", 1), getString(object, "tier", "")));
                }
            }
            if (!pool.isEmpty()) {
                map.put("diyuan_" + entry.getKey(), List.copyOf(pool));
            }
        }
        return unmodifiableMap(map);
    }

    private static Map<String, FactionSpecialty> parseFactionSpecialties(JsonObject root) {
        Map<String, FactionSpecialty> map = new LinkedHashMap<>();
        for (JsonElement element : getArray(root, "factions")) {
            JsonObject object = asObject(element, "faction specialty");
            String id = getString(object, "faction_id", "");
            if (id.isBlank()) {
                continue;
            }
            map.put(id, new FactionSpecialty(
                    id,
                    getString(object, "display", id),
                    getString(object, "specialty", ""),
                    stringList(object.get("artifact_bias")),
                    stringList(object.get("shop_artifacts"))
            ));
        }
        return unmodifiableMap(map);
    }

    private static List<AuctionStock> parseWanbaoStock(JsonObject root) {
        List<AuctionStock> list = new ArrayList<>();
        for (JsonElement element : getArray(root, "wanbao_pavilion_stock")) {
            JsonObject object = asObject(element, "wanbao stock");
            String id = getString(object, "artifact_id", "");
            if (!id.isBlank()) {
                list.add(new AuctionStock(
                        id,
                        getString(object, "display", id),
                        getString(object, "price_band", ""),
                        getString(object, "realm_gate", "")
                ));
            }
        }
        return List.copyOf(list);
    }

    private static List<AuctionLot> parseAuctionLots(JsonObject root) {
        List<AuctionLot> list = new ArrayList<>();
        for (JsonElement element : getArray(root, "great_jin_auction_lots")) {
            JsonObject object = asObject(element, "auction lot");
            String id = getString(object, "artifact_id", "");
            if (!id.isBlank()) {
                list.add(new AuctionLot(
                        id,
                        getString(object, "lot_tier", ""),
                        getInt(object, "start_bid_mid_stone", 0)
                ));
            }
        }
        return List.copyOf(list);
    }

    private static List<DraftItem> parseDraft(JsonObject root) {
        List<DraftItem> list = new ArrayList<>();
        for (JsonElement element : getArray(root, "items")) {
            JsonObject object = asObject(element, "draft item");
            String registry = getString(object, "registry", "");
            if (!registry.isBlank()) {
                list.add(new DraftItem(
                        registry,
                        getString(object, "class", ""),
                        getString(object, "tier", ""),
                        getInt(object, "game_tier", 0),
                        getInt(object, "max_stack", 1)
                ));
            }
        }
        return List.copyOf(list);
    }

    private static Map<String, AncientEntry> parseAncient(JsonObject root) {
        Map<String, AncientEntry> map = new LinkedHashMap<>();
        for (JsonElement element : getArray(root, "entries")) {
            JsonObject object = asObject(element, "ancient entry");
            String id = getString(object, "id", "");
            if (!id.isBlank()) {
                map.put(id, new AncientEntry(
                        id,
                        getString(object, "kind", ""),
                        getString(object, "effect", "")
                ));
            }
        }
        return unmodifiableMap(map);
    }

    private static int countEntries(JsonObject root) {
        for (String key : List.of("artifacts", "recipes", "game_tiers", "templates", "entries", "factions",
                "vehicles", "items", "synergies", "artifact_combos", "grade_mismatch_rules", "grades")) {
            JsonElement element = root.get(key);
            if (element != null && element.isJsonArray()) {
                return element.getAsJsonArray().size();
            }
        }
        int total = 0;
        for (String key : List.of("tiers", "sections", "realms", "barbarian_king_territories",
                "diyuan_by_layer", "wanbao_pavilion_stock", "great_jin_auction_lots", "components",
                "by_tier", "artifact_id_map", "tier_tag_to_grade")) {
            JsonElement element = root.get(key);
            if (element == null) {
                continue;
            }
            if (element.isJsonArray()) {
                total += element.getAsJsonArray().size();
            } else if (element.isJsonObject()) {
                total += element.getAsJsonObject().size();
            }
        }
        return total == 0 ? root.size() : total;
    }

    private static JsonArray getArray(JsonObject object, String key) {
        JsonElement element = object.get(key);
        return element != null && element.isJsonArray() ? element.getAsJsonArray() : new JsonArray();
    }

    private static JsonObject getObject(JsonObject object, String key) {
        JsonElement element = object.get(key);
        return element != null && element.isJsonObject() ? element.getAsJsonObject() : new JsonObject();
    }

    private static JsonObject asObject(JsonElement element, String name) {
        if (!element.isJsonObject()) {
            throw new IllegalStateException("Expected JSON object for " + name);
        }
        return element.getAsJsonObject();
    }

    private static String getString(JsonObject object, String key, String fallback) {
        JsonElement element = object.get(key);
        return element != null && element.isJsonPrimitive() ? element.getAsString() : fallback;
    }

    private static int getInt(JsonObject object, String key, int fallback) {
        JsonElement element = object.get(key);
        return element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()
                ? element.getAsInt()
                : fallback;
    }

    private static double getDouble(JsonObject object, String key, double fallback) {
        JsonElement element = object.get(key);
        return element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()
                ? element.getAsDouble()
                : fallback;
    }

    private static List<String> stringList(JsonElement element) {
        if (element == null || !element.isJsonArray()) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        for (JsonElement value : element.getAsJsonArray()) {
            if (value.isJsonPrimitive()) {
                values.add(value.getAsString());
            }
        }
        return List.copyOf(values);
    }

    private static <T> Map<String, T> unmodifiableMap(Map<String, T> values) {
        return Collections.unmodifiableMap(new LinkedHashMap<>(values));
    }

    private static <K, T> Map<K, T> unmodifiableMap(Map<K, T> values, boolean ignored) {
        return Collections.unmodifiableMap(new LinkedHashMap<>(values));
    }

    private static <T> Map<String, List<T>> unmodifiableListMap(Map<String, List<T>> values) {
        Map<String, List<T>> copy = new LinkedHashMap<>();
        values.forEach((key, list) -> copy.put(key, List.copyOf(list)));
        return Collections.unmodifiableMap(copy);
    }

    private static final class Holder {
        private static final Snapshot BUILTIN = loadBuiltin();
    }

    public record Snapshot(
            Map<String, ArtifactDefinition> artifacts,
            Map<String, TierRule> tierRules,
            RealmPowerScale realmPowerScale,
            Map<String, RefinementRecipe> refinementRecipes,
            Map<String, FlightVehicle> flightVehicles,
            Map<String, List<String>> priorityIds,
            Map<String, TalismanTreasureTemplate> talismanTreasureTemplates,
            FailureLootTable refinementFailureLoot,
            Map<Integer, ElevenTier> elevenTiers,
            Map<String, Integer> elevenIdMap,
            Map<String, TaxonomySection> taxonomy,
            Map<Integer, GradeBand> gradeBands,
            Map<String, int[]> tierTagToGrade,
            List<SynergyRule> synergies,
            List<ArtifactCombo> artifactCombos,
            Map<String, List<DropEntry>> realmDrops,
            Map<String, FactionSpecialty> factionSpecialties,
            List<AuctionStock> wanbaoStock,
            List<AuctionLot> auctionLots,
            List<DraftItem> draftItems,
            Map<String, AncientEntry> ancientEntries,
            Map<String, Integer> sourceFileEntryCounts
    ) {
        public Snapshot {
            artifacts = unmodifiableMap(artifacts);
            tierRules = unmodifiableMap(tierRules);
            refinementRecipes = unmodifiableMap(refinementRecipes);
            flightVehicles = unmodifiableMap(flightVehicles);
            priorityIds = unmodifiableMap(priorityIds);
            talismanTreasureTemplates = unmodifiableMap(talismanTreasureTemplates);
            elevenTiers = Collections.unmodifiableMap(new LinkedHashMap<>(elevenTiers));
            elevenIdMap = unmodifiableMap(elevenIdMap);
            taxonomy = unmodifiableMap(taxonomy);
            gradeBands = Collections.unmodifiableMap(new LinkedHashMap<>(gradeBands));
            tierTagToGrade = Collections.unmodifiableMap(new LinkedHashMap<>(tierTagToGrade));
            synergies = List.copyOf(synergies);
            artifactCombos = List.copyOf(artifactCombos);
            realmDrops = unmodifiableListMap(realmDrops);
            factionSpecialties = unmodifiableMap(factionSpecialties);
            wanbaoStock = List.copyOf(wanbaoStock);
            auctionLots = List.copyOf(auctionLots);
            draftItems = List.copyOf(draftItems);
            ancientEntries = unmodifiableMap(ancientEntries);
            sourceFileEntryCounts = unmodifiableMap(sourceFileEntryCounts);
        }

        public Optional<ArtifactDefinition> findArtifact(String id) {
            return Optional.ofNullable(artifacts.get(id));
        }

        public Optional<RefinementRecipe> findRecipe(String id) {
            return Optional.ofNullable(refinementRecipes.get(id));
        }

        public Optional<RefinementRecipe> findRecipeByArtifact(String artifactId) {
            return refinementRecipes.values().stream()
                    .filter(recipe -> recipe.artifactId().equals(artifactId))
                    .findFirst();
        }

        public Optional<FlightVehicle> findVehicle(String id) {
            return Optional.ofNullable(flightVehicles.get(id));
        }

        public String tierDisplay(String tier) {
            TierRule rule = tierRules.get(tier);
            return rule == null ? tier : rule.display();
        }

        public List<String> priorityIds(String tier) {
            return priorityIds.getOrDefault(tier, List.of());
        }

        public List<ArtifactDefinition> priorityArtifacts(String tier) {
            return priorityIds(tier).stream()
                    .map(artifacts::get)
                    .filter(Objects::nonNull)
                    .toList();
        }

        public int resolvedGameTier(String artifactId) {
            ArtifactDefinition def = artifacts.get(artifactId);
            if (def != null && def.gameTier() > 0) {
                return def.gameTier();
            }
            Integer mapped = elevenIdMap.get(artifactId);
            return mapped == null ? 0 : mapped;
        }

        public boolean isUniqueRestricted(String artifactId) {
            ArtifactDefinition def = artifacts.get(artifactId);
            if (def == null) {
                return false;
            }
            String tier = def.tier() == null ? "" : def.tier().toLowerCase(Locale.ROOT);
            if ("spirit_treasure".equals(tier) || "ancient_treasure".equals(tier) || "legendary".equals(tier)) {
                return true;
            }
            if (def.gameTier() >= 10) {
                return true;
            }
            String compliance = def.compliance() == null ? "" : def.compliance().toLowerCase(Locale.ROOT);
            return compliance.contains("unique") || compliance.contains("canonical");
        }

        public boolean isFlyingCapable(String artifactId) {
            ArtifactDefinition def = artifacts.get(artifactId);
            if (def == null) {
                return false;
            }
            String type = def.type() == null ? "" : def.type().toLowerCase(Locale.ROOT);
            if (type.contains("flying") || "movement".equals(type) || type.contains("vehicle")) {
                return true;
            }
            String id = artifactId == null ? "" : artifactId.toLowerCase(Locale.ROOT);
            return id.contains("flying") || id.contains("cloud_boots") || id.contains("wind_escape")
                    || id.contains("wheels") || id.contains("sail");
        }

        public List<DropEntry> dropsForRealm(String realmId) {
            return realmDrops.getOrDefault(realmId, List.of());
        }
    }

    public record ArtifactDefinition(
            String id,
            String display,
            String tier,
            String type,
            String effect,
            String realmMin,
            int gameTier,
            List<String> tags,
            String element,
            String binds,
            String compliance,
            boolean consumable,
            int uses
    ) {
        public ArtifactDefinition {
            tags = List.copyOf(tags);
        }

        /** Backward-compatible compact constructor used by older call sites/tests. */
        public ArtifactDefinition(String id, String display, String tier, String type, String effect,
                                  String realmMin, int gameTier, List<String> tags) {
            this(id, display, tier, type, effect, realmMin, gameTier, tags, "", "", "", false, 0);
        }
    }

    public record TierRule(String id, String display, String realmTypical, String refineCostBand) {}

    public record RealmPowerScale(double belowRealmMin, double atRealmMin, double twoMajorAbove) {}

    public record RefinementRecipe(
            String id,
            String artifactId,
            String display,
            String tier,
            String realmMin,
            int forgeGrade,
            double baseSuccessRate,
            List<MaterialRequirement> materials
    ) {
        public RefinementRecipe {
            materials = List.copyOf(materials);
        }
    }

    public record MaterialRequirement(String id, int count) {}

    public record FlightVehicle(String id, String display, String tier, String realmMin, double speed, String fuel) {}

    public record TalismanTreasureTemplate(String id, String display, String element, int defaultUses) {}

    public record FailureLootTable(List<FailureLootEntry> defaults, Map<String, List<FailureLootEntry>> byTier) {
        public FailureLootTable {
            defaults = List.copyOf(defaults);
            byTier = unmodifiableListMap(byTier);
        }

        public List<FailureLootEntry> entriesForTier(String tier) {
            List<FailureLootEntry> entries = byTier.get(tier);
            return entries == null || entries.isEmpty() ? defaults : entries;
        }
    }

    public record FailureLootEntry(String id, int weight, int countMin, int countMax) {}

    public record ElevenTier(int tier, String display, String modTier, String realmTypical, List<String> examples) {
        public ElevenTier {
            examples = List.copyOf(examples);
        }
    }

    public record TaxonomySection(String id, String display, List<String> types, List<String> examples) {
        public TaxonomySection {
            types = List.copyOf(types);
            examples = List.copyOf(examples);
        }
    }

    public record GradeBand(int grade, String display, String realmEquiv, String lootBand) {}

    public record SynergyRule(List<String> items, String relation, String note) {
        public SynergyRule {
            items = List.copyOf(items);
        }
    }

    public record ArtifactCombo(List<String> artifacts, String bonus, String note) {
        public ArtifactCombo {
            artifacts = List.copyOf(artifacts);
        }
    }

    public record DropEntry(String id, int weight, String tier) {}

    public record FactionSpecialty(String factionId, String display, String specialty,
                                   List<String> artifactBias, List<String> shopArtifacts) {
        public FactionSpecialty {
            artifactBias = List.copyOf(artifactBias);
            shopArtifacts = List.copyOf(shopArtifacts);
        }
    }

    public record AuctionStock(String artifactId, String display, String priceBand, String realmGate) {}

    public record AuctionLot(String artifactId, String lotTier, int startBidMidStone) {}

    public record DraftItem(String registry, String itemClass, String tier, int gameTier, int maxStack) {}

    public record AncientEntry(String id, String kind, String effect) {}
}
