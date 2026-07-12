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
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class ArtifactDataService {
    private static final String ROOT = "data/seeking_immortals/artifacts/";
    private static final List<String> SOURCE_FILES = List.of(
            "artifacts_catalog.json",
            "artifact_tier_rules.json",
            "artifact_eleven_tier_map.json",
            "artifact_taxonomy_111.json",
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

        return new Snapshot(artifacts, tierRules, realmPowerScale, recipes, vehicles, priorities,
                talismanTemplates, failureLoot, entryCounts);
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
            artifacts.put(id, new ArtifactDefinition(
                    id,
                    getString(object, "display", id),
                    getString(object, "tier", ""),
                    getString(object, "type", ""),
                    getString(object, "effect", ""),
                    getString(object, "realm_min", ""),
                    gameTier,
                    stringList(object.get("tags"))
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

    private static int countEntries(JsonObject root) {
        for (String key : List.of("artifacts", "recipes", "game_tiers", "templates", "entries", "factions",
                "vehicles", "items", "grade_mismatch_rules")) {
            JsonElement element = root.get(key);
            if (element != null && element.isJsonArray()) {
                return element.getAsJsonArray().size();
            }
        }
        int total = 0;
        for (String key : List.of("tiers", "sections", "realms", "barbarian_king_territories",
                "diyuan_by_layer", "wanbao_pavilion_stock", "great_jin_auction_lots", "components",
                "by_tier")) {
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
            Map<String, Integer> sourceFileEntryCounts
    ) {
        public Snapshot {
            artifacts = unmodifiableMap(artifacts);
            tierRules = unmodifiableMap(tierRules);
            refinementRecipes = unmodifiableMap(refinementRecipes);
            flightVehicles = unmodifiableMap(flightVehicles);
            priorityIds = unmodifiableMap(priorityIds);
            talismanTreasureTemplates = unmodifiableMap(talismanTreasureTemplates);
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
    }

    public record ArtifactDefinition(
            String id,
            String display,
            String tier,
            String type,
            String effect,
            String realmMin,
            int gameTier,
            List<String> tags
    ) {
        public ArtifactDefinition {
            tags = List.copyOf(tags);
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
}
