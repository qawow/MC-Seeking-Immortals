package com.xunxian.seekingimmortals.design;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class SettingCatalogSummaryService {
    private static final String TECHNIQUE_INDEX = "techniques/index.json";
    private static final List<String> DESIGN_CATALOGS = List.of(
            "refinement_recipes.json",
            "refinement_system.json",
            "talisman_catalog.json",
            "talisman_grade_map.json",
            "talisman_materials_catalog.json",
            "talisman_recipes.json"
    );
    private static final List<String> ENTRY_ARRAY_KEYS = List.of(
            "techniques",
            "recipes",
            "talismans",
            "materials",
            "grades",
            "manuals",
            "entries"
    );

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

        return new SettingCatalogSummary(
                declaredTechniqueCount,
                techniqueFiles.size(),
                techniqueFiles,
                statuses
        );
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
