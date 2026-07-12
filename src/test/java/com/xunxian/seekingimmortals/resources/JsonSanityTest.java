package com.xunxian.seekingimmortals.resources;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.xunxian.seekingimmortals.cultivation.Realm;
import com.xunxian.seekingimmortals.sect.SectContributionService;
import com.xunxian.seekingimmortals.sect.SectDefinitionService;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class JsonSanityTest {
    private static final Path SHIPPED_RESOURCES_ROOT = Path.of("src", "main", "resources");
    private static final Path TEXT_MATERIAL_ROOT = Path.of("\u6587\u672c\u6750\u6599", "data");

    @Test
    void allShippedModJsonParses() throws Exception {
        assertJsonObjectTreeParses(SHIPPED_RESOURCES_ROOT);
    }

    @Test
    void allTextMaterialJsonParses() throws Exception {
        assertJsonTreeParses(TEXT_MATERIAL_ROOT);
    }

    @Test
    void shippedAlchemyRecipesHaveSaneSchema() throws Exception {
        Path root = SHIPPED_RESOURCES_ROOT.resolve(Path.of(
                "data", "seeking_immortals", "alchemy", "recipes"));
        List<Path> recipeFiles = jsonFiles(root);
        assertFalse(recipeFiles.isEmpty(), "No shipped alchemy recipe JSON files found");

        Set<String> recipeIds = new HashSet<>();
        for (Path path : recipeFiles) {
            JsonObject recipe = readObject(path);
            String id = requiredString(recipe, "id", path);
            assertEquals(fileNameWithoutExtension(path), id, path + " id must match file name");
            assertTrue(recipeIds.add(id), "Duplicate alchemy recipe id " + id);

            JsonObject outputs = requiredObject(recipe, "output_items", path);
            for (String quality : List.of("low", "medium", "high", "supreme")) {
                assertNamespacedId(requiredString(outputs, quality, path), path, "output_items." + quality);
            }

            assertPositiveInt(recipe, "output_count", path);
            assertNonNegativeInt(recipe, "mana_cost", path);
            assertPositiveInt(recipe, "cook_ticks", path);
            assertRange(recipe, "success_rate", 0.0D, 1.0D, path);
            assertRange(recipe, "explosion_chance", 0.0D, 1.0D, path);
            assertIntRange(recipe, "required_furnace_tier", 1, 5, path);
            assertIntRange(recipe, "ideal_fire_tier", 1, 5, path);
            String minControlRealm = requiredString(recipe, "min_control_realm", path);
            assertDoesNotThrow(() -> Realm.valueOf(minControlRealm),
                    path + " min_control_realm must match a Realm enum constant");

            JsonArray ingredients = requiredArray(recipe, "ingredients", path);
            assertFalse(ingredients.isEmpty(), path + " must have at least one ingredient");
            for (JsonElement ingredientElement : ingredients) {
                JsonObject ingredient = requireObjectElement(ingredientElement, path, "ingredients");
                assertNamespacedId(requiredString(ingredient, "item", path), path, "ingredient.item");
                assertPositiveInt(ingredient, "count", path);
            }
        }
    }

    @Test
    void shippedShopJsonHasSaneSchema() throws Exception {
        Path root = SHIPPED_RESOURCES_ROOT.resolve(Path.of("data", "seeking_immortals", "shops"));
        List<Path> shopFiles = jsonFiles(root);
        assertFalse(shopFiles.isEmpty(), "No shipped shop JSON files found");

        Set<String> shopIds = new HashSet<>();
        for (Path path : shopFiles) {
            JsonObject shop = readObject(path);
            String id = requiredString(shop, "id", path);
            assertTrue(shopIds.add(id), "Duplicate shop id " + id);
            requiredString(shop, "currency", path);

            JsonArray entries = requiredArray(shop, "entries", path);
            assertFalse(entries.isEmpty(), path + " must have at least one shop entry");
            Set<String> entryIds = new HashSet<>();
            for (JsonElement entryElement : entries) {
                JsonObject entry = requireObjectElement(entryElement, path, "entries");
                String entryId = requiredString(entry, "id", path);
                assertTrue(entryIds.add(entryId), path + " has duplicate entry id " + entryId);
                assertShippedItemModelExists(requiredString(entry, "item", path), path, "entry.item");
                assertPositiveInt(entry, "count", path);
                assertPositiveInt(entry, "cost", path);
                if (entry.has("stock")) {
                    assertTrue(requiredInt(entry, "stock", path) >= -1, path + " entry.stock must be -1 or greater");
                }
                if (entry.has("refresh_ticks")) {
                    assertNonNegativeInt(entry, "refresh_ticks", path);
                }
                if (entry.has("rank_min") && !entry.get("rank_min").isJsonNull()) {
                    assertValidShopRank(requiredString(entry, "rank_min", path), path, "entry.rank_min");
                }
                if ("item".equals(entry.has("currency") ? entry.get("currency").getAsString() : shop.get("currency").getAsString())) {
                    String currencyItem = entry.has("currency_item")
                            ? requiredString(entry, "currency_item", path)
                            : requiredString(shop, "currency_item", path);
                    assertShippedItemModelExists(currencyItem, path, "currency_item");
                }
            }
        }
    }

    @Test
    void shippedSectContentJsonHasSaneSchema() throws Exception {
        Path dialogueRoot = SHIPPED_RESOURCES_ROOT.resolve(Path.of("data", "seeking_immortals", "sects", "dialogues"));
        Path missionRoot = SHIPPED_RESOURCES_ROOT.resolve(Path.of("data", "seeking_immortals", "sects", "missions"));

        for (SectDefinitionService.SectDefinition definition : SectDefinitionService.playableDefinitions()) {
            Path dialoguePath = dialogueRoot.resolve(definition.id() + ".json");
            JsonObject dialogue = readObject(dialoguePath);
            JsonArray nodes = requiredArray(dialogue, "nodes", dialoguePath);
            assertFalse(nodes.isEmpty(), dialoguePath + " nodes must not be empty");
            Set<String> nodeIds = new HashSet<>();
            for (JsonElement nodeElement : nodes) {
                JsonObject node = requireObjectElement(nodeElement, dialoguePath, "nodes");
                String id = requiredString(node, "id", dialoguePath);
                assertTrue(nodeIds.add(id), dialoguePath + " duplicate node " + id);
                requiredString(node, "title_key", dialoguePath);
                requiredString(node, "text_key", dialoguePath);
                JsonArray options = requiredArray(node, "options", dialoguePath);
                Set<String> optionIds = new HashSet<>();
                for (JsonElement optionElement : options) {
                    JsonObject option = requireObjectElement(optionElement, dialoguePath, "options");
                    String optionId = requiredString(option, "id", dialoguePath);
                    assertTrue(optionIds.add(optionId), dialoguePath + " duplicate option " + optionId);
                    requiredString(option, "label_key", dialoguePath);
                    requiredString(option, "action", dialoguePath);
                }
            }
            for (String node : List.of("knocking", "outer", "foundation", "inner", "complete")) {
                assertTrue(nodeIds.contains(node), dialoguePath + " missing node " + node);
            }

            Path missionPath = missionRoot.resolve(definition.id() + ".json");
            JsonObject missionsFile = readObject(missionPath);
            JsonArray missions = requiredArray(missionsFile, "missions", missionPath);
            assertFalse(missions.isEmpty(), missionPath + " missions must not be empty");
            Set<String> missionIds = new HashSet<>();
            for (JsonElement missionElement : missions) {
                JsonObject mission = requireObjectElement(missionElement, missionPath, "missions");
                String id = requiredString(mission, "id", missionPath);
                assertTrue(missionIds.add(id), missionPath + " duplicate mission " + id);
                requiredString(mission, "title_key", missionPath);
                requiredString(mission, "objective_key", missionPath);
                assertShippedItemModelExists(requiredString(mission, "item", missionPath), missionPath, "mission.item");
                assertPositiveInt(mission, "target", missionPath);
                assertPositiveInt(mission, "reward_contribution", missionPath);
                assertIntRange(mission, "min_stage", SectContributionService.STAGE_KNOCKING, SectContributionService.STAGE_PHASE10_COMPLETE, missionPath);
            }
        }
    }

    @Test
    void shippedSectWorldgenJsonReferencesExist() {
        Path dataRoot = SHIPPED_RESOURCES_ROOT.resolve(Path.of("data", "seeking_immortals"));
        for (SectDefinitionService.SectDefinition definition : SectDefinitionService.playableDefinitions()) {
            String structureName = definition.structureId().substring("seeking_immortals:".length());
            Path structurePath = dataRoot.resolve(Path.of("worldgen", "structure", structureName + ".json"));
            JsonObject structure = readObject(structurePath);
            assertEquals("minecraft:jigsaw", requiredString(structure, "type", structurePath));
            assertEquals("#seeking_immortals:has_structure/" + structureName, requiredString(structure, "biomes", structurePath));
            String startPool = requiredString(structure, "start_pool", structurePath);

            Path poolPath = namespacedWorldgenPath(dataRoot, startPool, "template_pool");
            JsonObject pool = readObject(poolPath);
            JsonArray elements = requiredArray(pool, "elements", poolPath);
            assertFalse(elements.isEmpty(), poolPath + " elements must not be empty");
            JsonObject firstElement = requiredObject(requireObjectElement(elements.get(0), poolPath, "elements"), "element", poolPath);
            String location = requiredString(firstElement, "location", poolPath);
            Path nbtPath = namespacedStructurePath(dataRoot, location);
            assertTrue(Files.exists(nbtPath), "Missing structure template " + nbtPath);

            Path setPath = dataRoot.resolve(Path.of("worldgen", "structure_set", structureName + ".json"));
            JsonObject structureSet = readObject(setPath);
            JsonArray structures = requiredArray(structureSet, "structures", setPath);
            assertFalse(structures.isEmpty(), setPath + " structures must not be empty");
            assertEquals(definition.structureId(), requiredString(requireObjectElement(structures.get(0), setPath, "structures"), "structure", setPath));
            requiredObject(structureSet, "placement", setPath);

            Path tagPath = dataRoot.resolve(Path.of("tags", "worldgen", "biome", "has_structure", structureName + ".json"));
            JsonObject tag = readObject(tagPath);
            assertFalse(requiredArray(tag, "values", tagPath).isEmpty(), tagPath + " values must not be empty");
        }
    }

    @Test
    void shippedWorldpackJsonHasSaneSchema() throws Exception {
        Path root = SHIPPED_RESOURCES_ROOT.resolve(Path.of("data", "seeking_immortals", "worldpack"));
        JsonObject regionsFile = readObject(root.resolve("regions.json"));
        JsonObject realmsFile = readObject(root.resolve("secret_realms.json"));
        JsonObject eventsFile = readObject(root.resolve("daily_events.json"));

        assertPositiveInt(regionsFile, "schema_version", root.resolve("regions.json"));
        assertPositiveInt(realmsFile, "schema_version", root.resolve("secret_realms.json"));
        assertPositiveInt(eventsFile, "schema_version", root.resolve("daily_events.json"));

        Set<String> regionIds = new HashSet<>();
        for (JsonElement regionElement : requiredArray(regionsFile, "regions", root.resolve("regions.json"))) {
            JsonObject region = requireObjectElement(regionElement, root.resolve("regions.json"), "regions");
            String id = requiredString(region, "id", root.resolve("regions.json"));
            assertTrue(regionIds.add(id), "Duplicate worldpack region id " + id);
            requiredString(region, "display_zh", root.resolve("regions.json"));
            requiredString(region, "display_en", root.resolve("regions.json"));
            assertRange(region, "aura_multiplier", 0.0D, 10.0D, root.resolve("regions.json"));
            requiredString(region, "min_realm", root.resolve("regions.json"));
            requiredString(region, "travel_anchor", root.resolve("regions.json"));
        }

        Set<String> realmIds = new HashSet<>();
        for (JsonElement realmElement : requiredArray(realmsFile, "secret_realms", root.resolve("secret_realms.json"))) {
            JsonObject realm = requireObjectElement(realmElement, root.resolve("secret_realms.json"), "secret_realms");
            String id = requiredString(realm, "id", root.resolve("secret_realms.json"));
            assertTrue(realmIds.add(id), "Duplicate worldpack secret realm id " + id);
            assertTrue(regionIds.contains(requiredString(realm, "region_id", root.resolve("secret_realms.json"))),
                    "Secret realm " + id + " must reference a shipped region");
            requiredString(realm, "display_zh", root.resolve("secret_realms.json"));
            requiredString(realm, "display_en", root.resolve("secret_realms.json"));
            requiredString(realm, "min_realm", root.resolve("secret_realms.json"));
            assertShippedItemModelExists(requiredString(realm, "ticket_item", root.resolve("secret_realms.json")),
                    root.resolve("secret_realms.json"), "ticket_item");
            assertNonNegativeInt(realm, "cooldown_ticks", root.resolve("secret_realms.json"));
            requiredString(realm, "return_policy", root.resolve("secret_realms.json"));
        }

        Set<String> eventIds = new HashSet<>();
        for (JsonElement eventElement : requiredArray(eventsFile, "daily_events", root.resolve("daily_events.json"))) {
            JsonObject event = requireObjectElement(eventElement, root.resolve("daily_events.json"), "daily_events");
            String id = requiredString(event, "id", root.resolve("daily_events.json"));
            assertTrue(eventIds.add(id), "Duplicate worldpack daily event id " + id);
            assertTrue(regionIds.contains(requiredString(event, "region_id", root.resolve("daily_events.json"))),
                    "Daily event " + id + " must reference a shipped region");
            requiredString(event, "display_zh", root.resolve("daily_events.json"));
            requiredString(event, "display_en", root.resolve("daily_events.json"));
            assertPositiveInt(event, "weight", root.resolve("daily_events.json"));
            assertNonNegativeInt(event, "duration_ticks", root.resolve("daily_events.json"));
            requiredArray(event, "effects", root.resolve("daily_events.json"));
        }
    }

    @Test
    void shippedCultivationTechniqueJsonHasSaneSchema() throws Exception {
        Path root = SHIPPED_RESOURCES_ROOT.resolve(Path.of("data", "seeking_immortals", "cultivation"));
        List<Path> techniqueFiles = jsonFiles(root);
        assertFalse(techniqueFiles.isEmpty(), "No shipped cultivation JSON files found");

        Set<String> techniqueIds = new HashSet<>();
        for (Path path : techniqueFiles) {
            JsonObject file = readObject(path);
            requiredString(file, "realm", path);
            requiredString(file, "realm_name", path);
            JsonArray techniques = requiredArray(file, "techniques", path);
            assertFalse(techniques.isEmpty(), path + " must have at least one technique");

            Set<String> idsInFile = new HashSet<>();
            for (JsonElement techniqueElement : techniques) {
                JsonObject technique = requireObjectElement(techniqueElement, path, "techniques");
                String id = requiredString(technique, "id", path);
                assertTrue(idsInFile.add(id), path + " has duplicate technique id " + id);
                assertTrue(techniqueIds.add(id), "Duplicate technique id across shipped files: " + id);
                requiredString(technique, "name", path);
                requiredString(technique, "type", path);
                requiredString(technique, "attribute", path);
                requiredString(technique, "source", path);
                requiredString(technique, "summary", path);
                if (technique.has("cost")) {
                    assertNonNegativeInt(technique, "cost", path);
                }
                if (technique.has("cooldown_ticks")) {
                    assertNonNegativeInt(technique, "cooldown_ticks", path);
                }
            }
        }
    }

    @Test
    void textMaterialIndexesAreCoherent() throws Exception {
        JsonObject manifest = readObject(TEXT_MATERIAL_ROOT.resolve("data_manifest.json"));
        assertPositiveInt(manifest, "schema_version", TEXT_MATERIAL_ROOT.resolve("data_manifest.json"));
        requiredString(manifest, "pack_id", TEXT_MATERIAL_ROOT.resolve("data_manifest.json"));

        JsonObject categories = requiredObject(manifest, "categories", TEXT_MATERIAL_ROOT.resolve("data_manifest.json"));
        assertFalse(categories.entrySet().isEmpty(), "data_manifest categories must not be empty");
        for (String category : categories.keySet()) {
            JsonArray references = requiredArray(categories, category, TEXT_MATERIAL_ROOT.resolve("data_manifest.json"));
            assertFalse(references.isEmpty(), "data_manifest category " + category + " must not be empty");
            for (JsonElement referenceElement : references) {
                String reference = requireStringElement(referenceElement, TEXT_MATERIAL_ROOT.resolve("data_manifest.json"), category);
                assertFalse(reference.isBlank(), "Blank manifest reference in category " + category);
                assertManifestReferenceExists(reference);
            }
        }

        assertTechniqueIndexIsCoherent();
        assertRegionCardIndexIsCoherent();
    }

    @Test
    void textMaterialAlchemyRecipesHaveSaneSchema() {
        Path path = TEXT_MATERIAL_ROOT.resolve("alchemy_recipes.json");
        JsonObject file = readObject(path);
        assertPositiveInt(file, "schema_version", path);

        JsonArray recipes = requiredArray(file, "recipes", path);
        assertFalse(recipes.isEmpty(), "text-material alchemy recipes must not be empty");
        Set<String> ids = new HashSet<>();
        for (JsonElement recipeElement : recipes) {
            JsonObject recipe = requireObjectElement(recipeElement, path, "recipes");
            String id = requiredString(recipe, "id", path);
            assertTrue(ids.add(id), "Duplicate text-material alchemy recipe id " + id);
            requiredString(recipe, "pill_id", path);
            if (recipe.has("display")) {
                requiredString(recipe, "display", path);
            }
            if (recipe.has("medium")) {
                requiredString(recipe, "medium", path);
            }
            if (recipe.has("realm_min")) {
                requiredString(recipe, "realm_min", path);
            }
            if (recipe.has("furnace_min_grade")) {
                assertPositiveInt(recipe, "furnace_min_grade", path);
            }
            if (recipe.has("furnace_grade")) {
                assertPositiveInt(recipe, "furnace_grade", path);
            }
            if (recipe.has("ideal_fire_tier")) {
                assertPositiveInt(recipe, "ideal_fire_tier", path);
            }
            if (recipe.has("base_success_rate")) {
                assertRange(recipe, "base_success_rate", 0.0D, 1.0D, path);
            }

            JsonArray materials = requiredArray(recipe, "materials", path);
            assertFalse(materials.isEmpty(), path + " recipe " + id + " must have materials");
            for (JsonElement materialElement : materials) {
                JsonObject material = requireObjectElement(materialElement, path, "materials");
                requiredString(material, "id", path);
                assertPositiveInt(material, "count", path);
            }
        }
    }

    @Test
    void shippedTextMaterialIdMapHasSaneSchema() {
        Path path = SHIPPED_RESOURCES_ROOT.resolve(Path.of(
                "data", "seeking_immortals", "reference", "text_material_id_map.json"));
        JsonObject file = readObject(path);
        assertEquals(1, requiredInt(file, "schema_version", path));
        assertEquals("seeking_immortals", requiredString(file, "namespace", path));

        JsonArray entries = requiredArray(file, "entries", path);
        assertFalse(entries.isEmpty(), path + " entries must not be empty");

        Set<String> allowedStatuses = Set.of("implemented", "implemented_partial", "deferred", "blocked");
        Set<String> allowedTypes = Set.of(
                "item",
                "future_item",
                "realm",
                "shop",
                "future_shop",
                "virtual_currency",
                "future_virtual_currency",
                "future_loader");
        Set<String> sourceKeys = new HashSet<>();
        Set<String> realmIds = new HashSet<>();
        for (Realm realm : Realm.values()) {
            realmIds.add(realm.name());
        }

        for (JsonElement entryElement : entries) {
            JsonObject entry = requireObjectElement(entryElement, path, "entries");
            String sourceCategory = requiredString(entry, "source_category", path);
            String sourceId = requiredString(entry, "source_id", path);
            assertTrue(sourceKeys.add(sourceCategory + "\u0000" + sourceId),
                    "Duplicate text-material id-map source key " + sourceCategory + ":" + sourceId);

            JsonArray sourceFiles = requiredArray(entry, "source_files", path);
            assertFalse(sourceFiles.isEmpty(), path + " source_files must not be empty for " + sourceCategory + ":" + sourceId);
            for (JsonElement sourceFileElement : sourceFiles) {
                requireStringElement(sourceFileElement, path, "source_files");
            }

            String canonicalType = requiredString(entry, "canonical_type", path);
            assertTrue(allowedTypes.contains(canonicalType), path + " invalid canonical_type " + canonicalType);
            String canonicalId = requiredString(entry, "canonical_id", path);

            String status = requiredString(entry, "status", path);
            assertTrue(allowedStatuses.contains(status), path + " invalid status " + status);
            requiredString(entry, "note", path);

            if (status.startsWith("implemented") && "item".equals(canonicalType)) {
                assertShippedItemModelExists(canonicalId, path, "canonical_id");
            }
            if (status.startsWith("implemented") && "realm".equals(canonicalType)) {
                assertTrue(realmIds.contains(canonicalId),
                        path + " implemented realm canonical_id must match Realm enum name: " + canonicalId);
            }
        }
    }

    private static void assertJsonTreeParses(Path root) throws Exception {
        List<Path> files = jsonFiles(root);
        assertFalse(files.isEmpty(), "No JSON files found under " + root);
        for (Path path : files) {
            assertDoesNotThrow(() -> readJson(path), "Failed JSON parse check for " + path);
        }
    }

    private static void assertJsonObjectTreeParses(Path root) throws Exception {
        List<Path> files = jsonFiles(root);
        assertFalse(files.isEmpty(), "No JSON files found under " + root);
        for (Path path : files) {
            assertDoesNotThrow(() -> readObject(path), "Failed JSON parse/root check for " + path);
        }
    }

    private static void assertTechniqueIndexIsCoherent() throws Exception {
        Path indexPath = TEXT_MATERIAL_ROOT.resolve(Path.of("techniques", "index.json"));
        JsonObject index = readObject(indexPath);
        JsonObject bySchool = requiredObject(index, "by_school", indexPath);
        JsonArray files = requiredArray(index, "files", indexPath);
        int expectedTotal = requiredInt(index, "total_techniques", indexPath);
        int bySchoolTotal = bySchool.entrySet().stream()
                .mapToInt(entry -> entry.getValue().getAsInt())
                .sum();
        assertEquals(expectedTotal, bySchoolTotal, "techniques/index total_techniques must match by_school sum");

        Set<String> fileNames = new HashSet<>();
        for (JsonElement fileElement : files) {
            String fileName = requireStringElement(fileElement, indexPath, "files");
            assertTrue(fileNames.add(fileName), "Duplicate techniques/index file " + fileName);
            Path techniquePath = TEXT_MATERIAL_ROOT.resolve(Path.of("techniques", fileName + ".json"));
            assertTrue(Files.exists(techniquePath), "Missing techniques file " + techniquePath);
            JsonArray techniques = requiredArray(readObject(techniquePath), "techniques", techniquePath);
            if (bySchool.has(fileName)) {
                assertEquals(bySchool.get(fileName).getAsInt(), techniques.size(),
                        techniquePath + " technique count must match techniques/index");
            }
        }
    }

    private static void assertRegionCardIndexIsCoherent() {
        Path indexPath = TEXT_MATERIAL_ROOT.resolve(Path.of("region_cards", "index.json"));
        JsonArray cards = requiredArray(readObject(indexPath), "cards", indexPath);
        assertFalse(cards.isEmpty(), "region_cards/index cards must not be empty");

        Set<String> cardIds = new HashSet<>();
        for (JsonElement cardElement : cards) {
            JsonObject card = requireObjectElement(cardElement, indexPath, "cards");
            String id = requiredString(card, "id", indexPath);
            assertTrue(cardIds.add(id), "Duplicate region card id " + id);
            requiredString(card, "display", indexPath);
            if (card.has("file")) {
                String fileName = requiredString(card, "file", indexPath);
                Path cardPath = TEXT_MATERIAL_ROOT.resolve(Path.of("region_cards", fileName));
                assertTrue(Files.exists(cardPath), "Missing region card file " + cardPath);
                assertEquals(id, requiredString(readObject(cardPath), "id", cardPath),
                        cardPath + " id must match region_cards/index");
            }
        }
    }

    private static Path namespacedWorldgenPath(Path dataRoot, String id, String registryFolder) {
        assertNamespacedId(id, dataRoot, registryFolder);
        String path = id.substring("seeking_immortals:".length());
        return dataRoot.resolve(Path.of("worldgen", registryFolder, path + ".json"));
    }

    private static Path namespacedStructurePath(Path dataRoot, String id) {
        assertNamespacedId(id, dataRoot, "structure_template");
        String path = id.substring("seeking_immortals:".length());
        return dataRoot.resolve(Path.of("structures", path + ".nbt"));
    }

    private static void assertManifestReferenceExists(String reference) throws Exception {
        if (reference.startsWith("../")) {
            return;
        }
        if (reference.contains("*")) {
            assertWildcardReferenceExists(reference);
            return;
        }
        Path path = TEXT_MATERIAL_ROOT.resolve(reference);
        assertTrue(Files.exists(path), "Missing manifest reference " + path);
    }

    private static void assertWildcardReferenceExists(String reference) throws IOException {
        int wildcardIndex = reference.indexOf('*');
        Path directory = TEXT_MATERIAL_ROOT.resolve(reference.substring(0, wildcardIndex)).normalize();
        String suffix = reference.substring(wildcardIndex + 1);
        assertTrue(Files.isDirectory(directory), "Missing manifest wildcard directory " + directory);
        try (Stream<Path> paths = Files.list(directory)) {
            boolean matched = paths
                    .filter(Files::isRegularFile)
                    .anyMatch(path -> path.getFileName().toString().endsWith(suffix));
            assertTrue(matched, "Manifest wildcard matched no files: " + reference);
        }
    }

    private static void assertValidShopRank(String rank, Path path, String field) {
        assertTrue(Set.of("outer_disciple", "inner_disciple", "core_disciple").contains(rank),
                path + " invalid " + field + " " + rank);
    }

    private static List<Path> jsonFiles(Path root) throws IOException {
        assertTrue(Files.exists(root), "Missing JSON root " + root);
        try (Stream<Path> paths = Files.walk(root)) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".json"))
                    .sorted()
                    .toList();
        }
    }

    private static JsonObject readObject(Path path) {
        JsonElement element = readJson(path);
        assertTrue(element.isJsonObject(), path + " must have a JSON object root");
        return element.getAsJsonObject();
    }

    private static JsonElement readJson(Path path) {
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonElement element = JsonParser.parseReader(reader);
            assertFalse(element == null || element.isJsonNull(), path + " must not be a null JSON root");
            return element;
        } catch (IOException | JsonParseException exception) {
            throw new AssertionError("Failed to read JSON " + path, exception);
        }
    }

    private static JsonObject requiredObject(JsonObject object, String field, Path path) {
        JsonElement element = requiredElement(object, field, path);
        assertTrue(element.isJsonObject(), path + " field " + field + " must be an object");
        return element.getAsJsonObject();
    }

    private static JsonArray requiredArray(JsonObject object, String field, Path path) {
        JsonElement element = requiredElement(object, field, path);
        assertTrue(element.isJsonArray(), path + " field " + field + " must be an array");
        return element.getAsJsonArray();
    }

    private static JsonElement requiredElement(JsonObject object, String field, Path path) {
        assertTrue(object.has(field), path + " missing required field " + field);
        JsonElement element = object.get(field);
        assertFalse(element.isJsonNull(), path + " field " + field + " must not be null");
        return element;
    }

    private static JsonObject requireObjectElement(JsonElement element, Path path, String field) {
        assertTrue(element.isJsonObject(), path + " field " + field + " must contain objects");
        return element.getAsJsonObject();
    }

    private static String requiredString(JsonObject object, String field, Path path) {
        JsonElement element = requiredElement(object, field, path);
        assertTrue(element.isJsonPrimitive() && element.getAsJsonPrimitive().isString(),
                path + " field " + field + " must be a string");
        String value = element.getAsString();
        assertFalse(value.isBlank(), path + " field " + field + " must not be blank");
        return value;
    }

    private static String requireStringElement(JsonElement element, Path path, String field) {
        assertTrue(element.isJsonPrimitive() && element.getAsJsonPrimitive().isString(),
                path + " field " + field + " must contain strings");
        String value = element.getAsString();
        assertFalse(value.isBlank(), path + " field " + field + " must not contain blank strings");
        return value;
    }

    private static int requiredInt(JsonObject object, String field, Path path) {
        JsonElement element = requiredElement(object, field, path);
        assertTrue(element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber(),
                path + " field " + field + " must be a number");
        return element.getAsInt();
    }

    private static void assertPositiveInt(JsonObject object, String field, Path path) {
        assertTrue(requiredInt(object, field, path) > 0, path + " field " + field + " must be positive");
    }

    private static void assertNonNegativeInt(JsonObject object, String field, Path path) {
        assertTrue(requiredInt(object, field, path) >= 0, path + " field " + field + " must be non-negative");
    }

    private static void assertIntRange(JsonObject object, String field, int min, int max, Path path) {
        int value = requiredInt(object, field, path);
        assertTrue(value >= min && value <= max,
                path + " field " + field + " must be in range " + min + ".." + max);
    }

    private static void assertRange(JsonObject object, String field, double min, double max, Path path) {
        JsonElement element = requiredElement(object, field, path);
        assertTrue(element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber(),
                path + " field " + field + " must be a number");
        double value = element.getAsDouble();
        assertTrue(value >= min && value <= max,
                path + " field " + field + " must be in range " + min + ".." + max);
    }

    private static void assertNamespacedId(String value, Path path, String field) {
        assertTrue(value.matches("[a-z0-9_.-]+:[a-z0-9_./-]+"),
                path + " field " + field + " must be a namespaced id: " + value);
    }

    private static void assertShippedItemModelExists(String value, Path path, String field) {
        assertNamespacedId(value, path, field);
        if (!value.startsWith("seeking_immortals:")) {
            return;
        }
        String itemPath = value.substring("seeking_immortals:".length());
        Path modelPath = SHIPPED_RESOURCES_ROOT.resolve(Path.of(
                "assets", "seeking_immortals", "models", "item", itemPath + ".json"));
        assertTrue(Files.exists(modelPath), path + " field " + field + " references item without shipped model: " + value);
    }

    private static String fileNameWithoutExtension(Path path) {
        String fileName = path.getFileName().toString();
        int extensionStart = fileName.lastIndexOf('.');
        return extensionStart >= 0 ? fileName.substring(0, extensionStart) : fileName;
    }
}
