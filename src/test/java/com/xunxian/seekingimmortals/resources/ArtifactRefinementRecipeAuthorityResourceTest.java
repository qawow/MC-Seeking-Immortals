package com.xunxian.seekingimmortals.resources;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArtifactRefinementRecipeAuthorityResourceTest {
    private static final Path DATA_ROOT = Path.of(
            "src", "main", "resources", "data", "seeking_immortals");
    private static final Path RECIPES_ROOT = DATA_ROOT.resolve("recipes");
    private static final Path REFINEMENT_CATALOG_PATH = DATA_ROOT.resolve(
            Path.of("artifacts", "refinement_recipes.json"));
    private static final Path ARTIFACT_CATALOG_PATH = DATA_ROOT.resolve(
            Path.of("artifacts", "artifacts_catalog.json"));
    private static final Path ID_MAP_PATH = DATA_ROOT.resolve(
            Path.of("reference", "text_material_id_map.json"));
    private static final String REFINEMENT_SERIALIZER = "seeking_immortals:refinement";

    @Test
    void vanillaCraftingRecipesCannotProduceAuthoritativeArtifacts() throws Exception {
        Set<String> authoritativeOutputs = authoritativeArtifactOutputs();
        List<String> violations = new ArrayList<>();

        for (Path path : recipeFiles()) {
            JsonObject recipe = readObject(path);
            String type = requiredString(recipe, "type", path);
            if (!type.startsWith("minecraft:crafting_")) {
                continue;
            }
            String output = recipeOutput(recipe, path);
            if (!output.isBlank() && authoritativeOutputs.contains(output)) {
                violations.add(relativeRecipePath(path) + " -> " + output);
            }
        }

        assertTrue(violations.isEmpty(),
                "Vanilla crafting recipes must not produce authoritative artifacts:\n"
                        + String.join("\n", violations));
    }

    @Test
    void allRefineRecipesUseRefinementSerializer() throws Exception {
        List<Path> refineRecipes = recipeFiles().stream()
                .filter(ArtifactRefinementRecipeAuthorityResourceTest::isRefineRecipe)
                .toList();
        assertFalse(refineRecipes.isEmpty(), "No refine_*.json recipes found under " + RECIPES_ROOT);

        List<String> violations = new ArrayList<>();
        for (Path path : refineRecipes) {
            String type = requiredString(readObject(path), "type", path);
            if (!REFINEMENT_SERIALIZER.equals(type)) {
                violations.add(relativeRecipePath(path) + " uses " + type);
            }
        }

        assertTrue(violations.isEmpty(),
                "Every refine_*.json recipe must use " + REFINEMENT_SERIALIZER + ":\n"
                        + String.join("\n", violations));
    }

    @Test
    void retainedRefinementSerializerRecipesMatchArtifactCatalog() throws Exception {
        Map<String, CatalogRecipe> catalog = loadCatalog();
        ItemIdMap itemIdMap = loadItemIdMap();
        Map<String, Path> serializersByCatalogId = new LinkedHashMap<>();
        List<String> mismatches = new ArrayList<>();
        int serializerRecipeCount = 0;

        for (Path path : recipeFiles()) {
            JsonObject recipe = readObject(path);
            if (!REFINEMENT_SERIALIZER.equals(requiredString(recipe, "type", path))) {
                continue;
            }
            serializerRecipeCount++;

            String catalogId = catalogRecipeId(path);
            Path previousSerializer = serializersByCatalogId.putIfAbsent(catalogId, path);
            if (previousSerializer != null) {
                mismatches.add(relativeRecipePath(previousSerializer) + " and "
                        + relativeRecipePath(path) + " both map to catalog recipe " + catalogId);
            }

            CatalogRecipe catalogRecipe = catalog.get(catalogId);
            if (catalogRecipe == null) {
                mismatches.add(relativeRecipePath(path) + " has no catalog recipe " + catalogId);
                continue;
            }

            String expectedOutput = itemIdMap.resolveOutput(catalogRecipe.artifactId());
            compare(mismatches, path, "ingredients",
                    resolveCatalogIngredients(catalogRecipe, itemIdMap),
                    serializerIngredients(recipe, path));
            compare(mismatches, path, "output", expectedOutput, recipeOutput(recipe, path));
            compare(mismatches, path, "realm_min", catalogRecipe.realmMin(),
                    requiredString(recipe, "realm_min", path));
            compare(mismatches, path, "forge_grade", catalogRecipe.forgeGrade(),
                    requiredInt(recipe, "forge_grade", path));

            double successRate = requiredDouble(recipe, "success_rate", path);
            if (Math.abs(catalogRecipe.baseSuccessRate() - successRate) > 1.0E-9D) {
                mismatches.add(relativeRecipePath(path) + " base_success_rate expected "
                        + catalogRecipe.baseSuccessRate() + " but was " + successRate);
            }
        }

        assertTrue(serializerRecipeCount > 0, "No retained refinement serializer recipes found");
        assertTrue(mismatches.isEmpty(),
                "Refinement serializer recipes must match artifact catalog authority:\n"
                        + String.join("\n", mismatches));
    }

    private static Set<String> authoritativeArtifactOutputs() throws IOException {
        ItemIdMap itemIdMap = loadItemIdMap();
        Set<String> outputs = new LinkedHashSet<>();
        for (CatalogRecipe recipe : loadCatalog().values()) {
            outputs.add(itemIdMap.resolveOutput(recipe.artifactId()));
        }
        for (String artifactId : loadArtifactCatalogIds()) {
            outputs.add(itemIdMap.resolveOutput(artifactId));
        }
        assertFalse(outputs.isEmpty(), "Artifact catalogs have no authoritative outputs");
        return Set.copyOf(outputs);
    }

    private static Map<String, CatalogRecipe> loadCatalog() throws IOException {
        JsonObject root = readObject(REFINEMENT_CATALOG_PATH);
        JsonArray recipes = requiredArray(root, "recipes", REFINEMENT_CATALOG_PATH);
        Map<String, CatalogRecipe> catalog = new LinkedHashMap<>();
        for (JsonElement element : recipes) {
            assertTrue(element.isJsonObject(),
                    REFINEMENT_CATALOG_PATH + " recipes entries must be objects");
            JsonObject recipe = element.getAsJsonObject();
            String id = requiredString(recipe, "id", REFINEMENT_CATALOG_PATH);
            CatalogRecipe previous = catalog.put(id, new CatalogRecipe(
                    id,
                    requiredString(recipe, "artifact_id", REFINEMENT_CATALOG_PATH),
                    requiredString(recipe, "realm_min", REFINEMENT_CATALOG_PATH),
                    requiredInt(recipe, "forge_grade", REFINEMENT_CATALOG_PATH),
                    requiredDouble(recipe, "base_success_rate", REFINEMENT_CATALOG_PATH),
                    catalogMaterials(recipe, id)));
            assertTrue(previous == null, "Duplicate artifact refinement catalog id " + id);
        }
        assertFalse(catalog.isEmpty(), "Artifact refinement catalog recipes must not be empty");
        return Map.copyOf(catalog);
    }

    private static Set<String> loadArtifactCatalogIds() throws IOException {
        JsonObject root = readObject(ARTIFACT_CATALOG_PATH);
        JsonArray artifacts = requiredArray(root, "artifacts", ARTIFACT_CATALOG_PATH);
        Set<String> artifactIds = new LinkedHashSet<>();
        for (JsonElement element : artifacts) {
            assertTrue(element.isJsonObject(),
                    ARTIFACT_CATALOG_PATH + " artifacts entries must be objects");
            String id = requiredString(element.getAsJsonObject(), "id", ARTIFACT_CATALOG_PATH);
            assertTrue(artifactIds.add(id), "Duplicate artifact catalog id " + id);
        }
        assertFalse(artifactIds.isEmpty(), "Artifact catalog must not be empty");
        return Set.copyOf(artifactIds);
    }

    private static ItemIdMap loadItemIdMap() throws IOException {
        JsonObject root = readObject(ID_MAP_PATH);
        String namespace = requiredString(root, "namespace", ID_MAP_PATH);
        JsonArray entries = requiredArray(root, "entries", ID_MAP_PATH);
        Map<String, String> canonicalIds = new LinkedHashMap<>();
        Map<String, String> itemIds = new LinkedHashMap<>();
        List<String> conflicts = new ArrayList<>();
        for (JsonElement element : entries) {
            assertTrue(element.isJsonObject(), ID_MAP_PATH + " entries must be objects");
            JsonObject entry = element.getAsJsonObject();
            String sourceId = requiredString(entry, "source_id", ID_MAP_PATH);
            String canonicalId = requiredString(entry, "canonical_id", ID_MAP_PATH);
            String previousCanonicalId = canonicalIds.putIfAbsent(sourceId, canonicalId);
            if (previousCanonicalId != null && !previousCanonicalId.equals(canonicalId)) {
                conflicts.add(sourceId + " maps to both " + previousCanonicalId + " and " + canonicalId);
            }

            String canonicalType = requiredString(entry, "canonical_type", ID_MAP_PATH);
            String status = requiredString(entry, "status", ID_MAP_PATH);
            if (!"item".equals(canonicalType) || !status.startsWith("implemented")) {
                continue;
            }
            itemIds.put(sourceId, canonicalId);
        }
        assertTrue(conflicts.isEmpty(),
                "text_material_id_map.json has conflicting source_id mappings:\n"
                        + String.join("\n", conflicts));
        return new ItemIdMap(namespace, Map.copyOf(itemIds));
    }

    private static Map<String, Integer> catalogMaterials(JsonObject recipe, String recipeId) {
        JsonArray materials = requiredArray(recipe, "materials", REFINEMENT_CATALOG_PATH);
        Map<String, Integer> counts = new TreeMap<>();
        for (JsonElement element : materials) {
            assertTrue(element.isJsonObject(),
                    REFINEMENT_CATALOG_PATH + " recipe " + recipeId + " materials must be objects");
            JsonObject material = element.getAsJsonObject();
            String sourceId = requiredString(material, "id", REFINEMENT_CATALOG_PATH);
            int count = requiredInt(material, "count", REFINEMENT_CATALOG_PATH);
            assertTrue(count > 0,
                    REFINEMENT_CATALOG_PATH + " recipe " + recipeId + " has non-positive material count");
            counts.merge(sourceId, count, Integer::sum);
        }
        assertFalse(counts.isEmpty(),
                REFINEMENT_CATALOG_PATH + " recipe " + recipeId + " has no materials");
        return Map.copyOf(counts);
    }

    private static Map<String, Integer> resolveCatalogIngredients(CatalogRecipe recipe,
                                                                   ItemIdMap itemIdMap) {
        Map<String, Integer> resolved = new TreeMap<>();
        for (Map.Entry<String, Integer> material : recipe.materials().entrySet()) {
            String itemId = itemIdMap.resolveIngredient(material.getKey());
            assertFalse(itemId.isBlank(), REFINEMENT_CATALOG_PATH + " recipe " + recipe.id()
                    + " material " + material.getKey() + " has no implemented item mapping");
            resolved.merge(itemId, material.getValue(), Integer::sum);
        }
        return Map.copyOf(resolved);
    }

    private static Map<String, Integer> serializerIngredients(JsonObject recipe, Path path) {
        JsonArray ingredients = requiredArray(recipe, "ingredients", path);
        Map<String, Integer> counts = new TreeMap<>();
        for (JsonElement element : ingredients) {
            assertTrue(element.isJsonObject(), path + " ingredients must be item objects");
            String itemId = requiredString(element.getAsJsonObject(), "item", path);
            counts.merge(itemId, 1, Integer::sum);
        }
        assertFalse(counts.isEmpty(), path + " ingredients must not be empty");
        return Map.copyOf(counts);
    }

    private static List<Path> recipeFiles() throws IOException {
        assertTrue(Files.isDirectory(RECIPES_ROOT), "Recipes directory missing: " + RECIPES_ROOT);
        try (Stream<Path> stream = Files.walk(RECIPES_ROOT)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".json"))
                    .sorted()
                    .toList();
        }
    }

    private static boolean isRefineRecipe(Path path) {
        String fileName = path.getFileName().toString();
        return fileName.startsWith("refine_") && fileName.endsWith(".json");
    }

    private static String catalogRecipeId(Path path) {
        String id = relativeRecipePath(path);
        id = id.substring(0, id.length() - ".json".length());
        if (id.endsWith("_serializer")) {
            id = id.substring(0, id.length() - "_serializer".length());
        }
        return id.startsWith("refinement_")
                ? "refine_" + id.substring("refinement_".length())
                : id;
    }

    private static String recipeOutput(JsonObject recipe, Path path) {
        JsonElement result = recipe.get("result");
        assertTrue(result != null && !result.isJsonNull(), path + " missing result");
        if (result.isJsonPrimitive()) {
            JsonPrimitive primitive = result.getAsJsonPrimitive();
            assertTrue(primitive.isString(), path + " result must be a string or object");
            return primitive.getAsString();
        }
        assertTrue(result.isJsonObject(), path + " result must be a string or object");
        return requiredString(result.getAsJsonObject(), "item", path);
    }

    private static JsonObject readObject(Path path) throws IOException {
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonElement element = JsonParser.parseReader(reader);
            assertTrue(element.isJsonObject(), path + " root must be a JSON object");
            return element.getAsJsonObject();
        }
    }

    private static JsonArray requiredArray(JsonObject object, String member, Path path) {
        JsonElement element = object.get(member);
        assertTrue(element != null && element.isJsonArray(), path + " missing array " + member);
        return element.getAsJsonArray();
    }

    private static String requiredString(JsonObject object, String member, Path path) {
        JsonElement element = object.get(member);
        assertTrue(element != null && element.isJsonPrimitive()
                        && element.getAsJsonPrimitive().isString(),
                path + " missing string " + member);
        return element.getAsString();
    }

    private static int requiredInt(JsonObject object, String member, Path path) {
        JsonElement element = object.get(member);
        assertTrue(element != null && element.isJsonPrimitive()
                        && element.getAsJsonPrimitive().isNumber(),
                path + " missing integer " + member);
        return element.getAsInt();
    }

    private static double requiredDouble(JsonObject object, String member, Path path) {
        JsonElement element = object.get(member);
        assertTrue(element != null && element.isJsonPrimitive()
                        && element.getAsJsonPrimitive().isNumber(),
                path + " missing number " + member);
        return element.getAsDouble();
    }

    private static void compare(List<String> mismatches, Path path, String field,
                                Object expected, Object actual) {
        if (!expected.equals(actual)) {
            mismatches.add(relativeRecipePath(path) + " " + field + " expected "
                    + expected + " but was " + actual);
        }
    }

    private static String relativeRecipePath(Path path) {
        return RECIPES_ROOT.relativize(path).toString().replace('\\', '/');
    }

    private record CatalogRecipe(String id, String artifactId, String realmMin,
                                 int forgeGrade, double baseSuccessRate,
                                 Map<String, Integer> materials) {}

    private record ItemIdMap(String namespace, Map<String, String> itemIds) {
        String resolveOutput(String sourceId) {
            if (sourceId.contains(":")) {
                return sourceId;
            }
            return itemIds.getOrDefault(sourceId, namespace + ":" + sourceId);
        }

        String resolveIngredient(String sourceId) {
            if (sourceId.contains(":")) {
                return sourceId;
            }
            return itemIds.getOrDefault(sourceId, "");
        }
    }
}
