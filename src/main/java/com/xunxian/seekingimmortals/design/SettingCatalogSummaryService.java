package com.xunxian.seekingimmortals.design;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.xunxian.seekingimmortals.SeekingImmortalsMod;
import com.xunxian.seekingimmortals.beast.BeastBestiaryService;
import com.xunxian.seekingimmortals.catalog.LoreCatalogService;
import com.xunxian.seekingimmortals.quest.TimelineChronicleService;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Setting-pack / shipped-catalog summary.
 * M16 expands beyond techniques/talismans to full-category counts for HUD/tooltip/encyclopedia.
 */
public final class SettingCatalogSummaryService {
    private static final String TECHNIQUE_INDEX = "techniques/index.json";
    private static final List<String> DESIGN_CATALOGS = List.of(
            "refinement_recipes.json",
            "refinement_system.json",
            "talisman_catalog.json",
            "talisman_grade_map.json",
            "talisman_materials_catalog.json",
            "talisman_recipes.json",
            "pills_catalog.json",
            "artifacts_catalog.json",
            "cultivation_methods.json",
            "secret_realms.json",
            "name_alias_glossary_v103.json",
            "numeric_overview_v103.json",
            "visual_style_v118.json"
    );
    private static final List<String> ENTRY_ARRAY_KEYS = List.of(
            "techniques",
            "recipes",
            "talismans",
            "materials",
            "grades",
            "manuals",
            "entries",
            "pills",
            "artifacts",
            "methods",
            "realms",
            "creatures"
    );

    private static final Map<String, Integer> SHIPPED_CATEGORY_CACHE = new ConcurrentHashMap<>();

    private SettingCatalogSummaryService() {}

    public static SettingCatalogSummary summarize(Path dataRoot) {
        List<SettingCatalogSummary.CatalogFileStatus> statuses = new ArrayList<>();
        ParsedJson index = parse(dataRoot, TECHNIQUE_INDEX);
        statuses.add(index.status());

        int declaredTechniqueCount = 0;
        List<String> techniqueFiles = List.of();
        if (index.valid()) {
            JsonObject object = index.root().getAsJsonObject();
            declaredTechniqueCount = intValue(object, "total_techniques", 0);
            techniqueFiles = stringArray(object.get("files"));
        }

        for (String file : techniqueFiles) {
            statuses.add(parse(dataRoot, "techniques/" + file + ".json").status());
        }
        for (String file : DESIGN_CATALOGS) {
            statuses.add(parse(dataRoot, file).status());
        }

        Map<String, Integer> categories = new LinkedHashMap<>();
        categories.put("techniques", declaredTechniqueCount);
        categories.put("pills", entryCount(dataRoot, "pills_catalog.json"));
        categories.put("artifacts", entryCount(dataRoot, "artifacts_catalog.json"));
        categories.put("talismans", entryCount(dataRoot, "talisman_catalog.json"));
        categories.put("methods", entryCount(dataRoot, "cultivation_methods.json"));
        categories.put("secret_realms", Math.max(
                entryCount(dataRoot, "secret_realms.json"),
                entryCount(dataRoot, "secret_realm_runtime.json")));
        categories.put("sects", entryCount(dataRoot, "sect_templates.json")
                + entryCount(dataRoot, "playable_sects.json"));
        categories.put("beasts", Math.max(
                entryCount(dataRoot, "beast_bestiary_runtime.json"),
                entryCount(dataRoot, "bestiary_summary_v101.json")));
        categories.put("glossary", entryCount(dataRoot, "name_alias_glossary_v103.json"));
        categories.put("numeric", Files.exists(dataRoot.resolve("numeric_overview_v103.json")) ? 1 : 0);
        categories.put("visual", Files.exists(dataRoot.resolve("visual_style_v118.json")) ? 1 : 0);
        categories.put("chronicle", entryCount(dataRoot, "chronicle_events.json"));

        return new SettingCatalogSummary(
                declaredTechniqueCount,
                techniqueFiles.size(),
                techniqueFiles,
                statuses,
                categories
        );
    }

    /**
     * Runtime category counts from shipped resources / live services (for HUD/tooltip).
     * Prefer live service sizes when available so M10/M15 numbers stay authoritative.
     */
    public static int categoryCount(String category) {
        if (category == null || category.isBlank()) {
            return 0;
        }
        String key = category.trim().toLowerCase(java.util.Locale.ROOT);
        return SHIPPED_CATEGORY_CACHE.computeIfAbsent(key, SettingCatalogSummaryService::resolveShippedCategory);
    }

    public static Map<String, Integer> allCategoryCounts() {
        Map<String, Integer> map = new LinkedHashMap<>();
        for (String key : List.of(
                "techniques", "pills", "artifacts", "talismans", "methods",
                "beasts", "sects", "secret_realms", "dimensions", "glossary",
                "numeric", "visual", "chronicle", "lore")) {
            map.put(key, categoryCount(key));
        }
        return map;
    }

    private static int resolveShippedCategory(String key) {
        return switch (key) {
            case "techniques" -> shippedArraySize("text_material/techniques/index.json", "total_techniques");
            case "pills" -> shippedArraySize("text_material/pills_catalog.json", "pills")
                    + shippedArraySize("catalog/pills_index.json", "entries");
            case "artifacts" -> {
                int n = shippedArraySize("text_material/artifacts_catalog.json", "artifacts");
                if (n <= 0) {
                    n = shippedArraySize("catalog/artifacts_index.json", "entries");
                }
                yield n;
            }
            case "talismans" -> shippedArraySize("text_material/talisman_catalog.json", "talismans");
            case "methods" -> shippedArraySize("text_material/cultivation_methods.json", "methods");
            case "beasts" -> BeastBestiaryService.size();
            case "sects" -> shippedArraySize("catalog/playable_sects_index.json", "entries")
                    + shippedArraySize("catalog/sect_templates_index.json", "entries");
            case "secret_realms" -> {
                int n = shippedArraySize("text_material/secret_realms.json", "realms");
                if (n <= 0) {
                    n = shippedArraySize("text_material/secret_realm_runtime.json", "realms");
                }
                if (n <= 0) {
                    n = shippedArraySize("catalog/secret_realms_index.json", "entries");
                }
                yield n;
            }
            case "dimensions" -> LoreCatalogService.builtin().dimensions().size()
                    + LoreCatalogService.builtin().dimensionRegistry().size();
            case "glossary" -> shippedArraySize("text_material/name_alias_glossary_v103.json", "entries");
            case "numeric" -> resourceExists("text_material/numeric_overview_v103.json") ? 1 : 0;
            case "visual" -> resourceExists("text_material/visual_style_v118.json") ? 1 : 0;
            case "chronicle" -> TimelineChronicleService.chronicleCount();
            case "lore" -> LoreCatalogService.builtin().totalEntries();
            default -> 0;
        };
    }

    private static int entryCount(Path dataRoot, String relativePath) {
        return parse(dataRoot, relativePath).status().entryCount();
    }

    private static int shippedArraySize(String relative, String preferredKey) {
        JsonObject root = readResource("data/" + SeekingImmortalsMod.MODID + "/" + relative);
        if (root == null) {
            return 0;
        }
        if (preferredKey != null && root.has(preferredKey)) {
            JsonElement element = root.get(preferredKey);
            if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) {
                return element.getAsInt();
            }
            if (element.isJsonArray()) {
                return element.getAsJsonArray().size();
            }
        }
        return countEntries(root);
    }

    private static boolean resourceExists(String relative) {
        String path = "data/" + SeekingImmortalsMod.MODID + "/" + relative;
        try (InputStream stream = SettingCatalogSummaryService.class.getClassLoader().getResourceAsStream(path)) {
            return stream != null;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static JsonObject readResource(String path) {
        try (InputStream stream = SettingCatalogSummaryService.class.getClassLoader().getResourceAsStream(path)) {
            if (stream == null) {
                return null;
            }
            try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                JsonElement root = JsonParser.parseReader(reader);
                return root != null && root.isJsonObject() ? root.getAsJsonObject() : null;
            }
        } catch (Exception ignored) {
            return null;
        }
    }

    private static ParsedJson parse(Path dataRoot, String relativePath) {
        Path path = dataRoot.resolve(relativePath);
        if (!Files.exists(path)) {
            return ParsedJson.invalid(SettingCatalogSummary.CatalogFileStatus.missing(relativePath));
        }
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonElement root = JsonParser.parseReader(reader);
            int entryCount = countEntries(root);
            return new ParsedJson(root, SettingCatalogSummary.CatalogFileStatus.valid(relativePath, entryCount));
        } catch (JsonParseException | IOException | IllegalStateException exception) {
            return ParsedJson.invalid(SettingCatalogSummary.CatalogFileStatus.invalid(relativePath, sanitize(exception)));
        }
    }

    private static int countEntries(JsonElement root) {
        if (root == null || root.isJsonNull()) {
            return 0;
        }
        if (root.isJsonArray()) {
            return root.getAsJsonArray().size();
        }
        if (!root.isJsonObject()) {
            return 1;
        }

        JsonObject object = root.getAsJsonObject();
        for (String key : ENTRY_ARRAY_KEYS) {
            JsonElement element = object.get(key);
            if (element != null && element.isJsonArray()) {
                return element.getAsJsonArray().size();
            }
        }
        JsonElement components = object.get("components");
        if (components != null && components.isJsonObject()) {
            return components.getAsJsonObject().size();
        }
        JsonElement bySchool = object.get("by_school");
        if (bySchool != null && bySchool.isJsonObject()) {
            return sumObjectInts(bySchool.getAsJsonObject());
        }
        JsonElement creatures = object.get("creatures");
        if (creatures != null && creatures.isJsonArray()) {
            return creatures.getAsJsonArray().size();
        }
        if (object.has("total") && object.get("total").isJsonPrimitive() && object.get("total").getAsJsonPrimitive().isNumber()) {
            return object.get("total").getAsInt();
        }
        return object.size();
    }

    private static int sumObjectInts(JsonObject object) {
        int total = 0;
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            if (entry.getValue().isJsonPrimitive() && entry.getValue().getAsJsonPrimitive().isNumber()) {
                total += entry.getValue().getAsInt();
            }
        }
        return total;
    }

    private static int intValue(JsonObject object, String key, int fallback) {
        JsonElement element = object.get(key);
        return element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()
                ? element.getAsInt()
                : fallback;
    }

    private static List<String> stringArray(JsonElement element) {
        if (element == null || !element.isJsonArray()) {
            return List.of();
        }
        JsonArray array = element.getAsJsonArray();
        List<String> values = new ArrayList<>();
        for (JsonElement value : array) {
            if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
                values.add(value.getAsString());
            }
        }
        return List.copyOf(values);
    }

    private static String sanitize(Exception exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            message = exception.getClass().getSimpleName();
        }
        message = message.replace('\r', ' ').replace('\n', ' ').trim();
        return message.length() <= 220 ? message : message.substring(0, 217) + "...";
    }

    private record ParsedJson(JsonElement root, SettingCatalogSummary.CatalogFileStatus status) {
        static ParsedJson invalid(SettingCatalogSummary.CatalogFileStatus status) {
            return new ParsedJson(null, status);
        }

        boolean valid() {
            return status.valid();
        }
    }
}
