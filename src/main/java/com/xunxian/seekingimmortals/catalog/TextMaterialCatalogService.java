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
import java.util.Map;
import java.util.Optional;

/**
 * Loads text-material catalog slices shipped under data/seeking_immortals/catalog and worldpack flavor.
 */
public final class TextMaterialCatalogService {
    private static final Snapshot BUILTIN = loadBuiltin();

    private TextMaterialCatalogService() {}

    public static Snapshot builtin() {
        return BUILTIN;
    }

    public record SecretRealmFlavor(String id, String openCondition, String environment,
                                    String layeredExploration, List<String> rareDrops, String note) {}

    /**
     * Wave464: rich fields from text_material manuals_catalog (forge grade / recipe / sources).
     */
    public record ManualEntry(String id, String display, String type, List<String> unlocks, String realmMin,
                              String note, int unlocksForgeGrade, String recipeId, List<String> sources) {
        public ManualEntry {
            unlocks = unlocks == null ? List.of() : List.copyOf(unlocks);
            sources = sources == null ? List.of() : List.copyOf(sources);
            unlocksForgeGrade = Math.max(0, unlocksForgeGrade);
            recipeId = recipeId == null ? "" : recipeId;
            note = note == null ? "" : note;
            realmMin = realmMin == null ? "" : realmMin;
        }
    }

    public record MethodEntry(String id, String display, String realmMin, String school, String attribute) {}

    public record FlightBinding(String id, String display, String realmMin, double speed,
                                String fuel, String carrierItem, String fuelItem, int fuelCount) {}

    public record Snapshot(Map<String, SecretRealmFlavor> secretRealmFlavors,
                           Map<String, ManualEntry> manuals,
                           Map<String, MethodEntry> methods,
                           Map<String, FlightBinding> flightBindings) {
        public Optional<SecretRealmFlavor> findFlavor(String id) {
            return Optional.ofNullable(secretRealmFlavors.get(id == null ? "" : id));
        }

        public Optional<ManualEntry> findManual(String id) {
            return Optional.ofNullable(manuals.get(id == null ? "" : id));
        }

        public Optional<FlightBinding> findFlight(String id) {
            return Optional.ofNullable(flightBindings.get(id == null ? "" : id));
        }

        public Optional<MethodEntry> findMethod(String id) {
            if (id == null || id.isBlank()) {
                return Optional.empty();
            }
            MethodEntry direct = methods.get(id);
            if (direct != null) {
                return Optional.of(direct);
            }
            String key = id.trim().toLowerCase(java.util.Locale.ROOT);
            MethodEntry lower = methods.get(key);
            if (lower != null) {
                return Optional.of(lower);
            }
            for (MethodEntry method : methods.values()) {
                if (method.id() != null && method.id().equalsIgnoreCase(id)) {
                    return Optional.of(method);
                }
            }
            return Optional.empty();
        }
    }

    private static Snapshot loadBuiltin() {
        Map<String, SecretRealmFlavor> flavors = new LinkedHashMap<>();
        JsonObject flavorRoot = readJson("data/" + SeekingImmortalsMod.MODID + "/worldpack/secret_realm_flavor.json");
        if (flavorRoot != null) {
            for (JsonElement element : array(flavorRoot, "realms")) {
                if (!element.isJsonObject()) continue;
                JsonObject o = element.getAsJsonObject();
                String id = str(o, "id");
                if (id.isBlank()) continue;
                flavors.put(id, new SecretRealmFlavor(id, str(o, "open_condition"), str(o, "environment"),
                        str(o, "layered_exploration"), stringList(o.get("rare_drops")), str(o, "note")));
            }
        }

        Map<String, ManualEntry> manuals = new LinkedHashMap<>();
        // Wave464: prefer rich text_material manuals, fallback to flat catalog index.
        JsonObject manualRoot = readJson("data/" + SeekingImmortalsMod.MODID + "/text_material/manuals_catalog.json");
        if (manualRoot == null) {
            manualRoot = readJson("data/" + SeekingImmortalsMod.MODID + "/catalog/manuals_catalog.json");
        }
        if (manualRoot != null) {
            for (JsonElement element : array(manualRoot, "manuals")) {
                if (!element.isJsonObject()) continue;
                JsonObject o = element.getAsJsonObject();
                String id = str(o, "id");
                if (id.isBlank()) continue;
                manuals.put(id, parseManualEntry(o));
            }
            // If text_material used a different array key.
            if (manuals.isEmpty()) {
                for (JsonElement element : array(manualRoot, "entries")) {
                    if (!element.isJsonObject()) continue;
                    JsonObject o = element.getAsJsonObject();
                    String id = str(o, "id");
                    if (id.isBlank()) continue;
                    manuals.put(id, parseManualEntry(o));
                }
            }
        }

        Map<String, MethodEntry> methods = new LinkedHashMap<>();
        // M02: prefer full text_material cultivation_methods (136), then enrich/fallback to catalog index.
        JsonObject methodRoot = readJson("data/" + SeekingImmortalsMod.MODID + "/text_material/cultivation_methods.json");
        if (methodRoot != null) {
            for (JsonElement element : array(methodRoot, "methods")) {
                if (!element.isJsonObject()) continue;
                JsonObject o = element.getAsJsonObject();
                String id = str(o, "id");
                if (id.isBlank()) continue;
                String realmMin = str(o, "realm_min");
                if (realmMin.isBlank() && o.has("learn_requirements") && o.get("learn_requirements").isJsonObject()) {
                    realmMin = str(o.getAsJsonObject("learn_requirements"), "realm_min");
                }
                String school = str(o, "school");
                if (school.isBlank()) school = str(o, "combat_school");
                if (school.isBlank()) school = str(o, "unlocks_techniques_school");
                String attribute = str(o, "element");
                if (attribute.isBlank()) attribute = str(o, "element_required");
                methods.put(id, new MethodEntry(id, str(o, "display"), realmMin, school, attribute));
            }
        }
        JsonObject methodIndex = readJson("data/" + SeekingImmortalsMod.MODID + "/catalog/cultivation_methods_index.json");
        if (methodIndex != null) {
            for (JsonElement element : array(methodIndex, "methods")) {
                if (!element.isJsonObject()) continue;
                JsonObject o = element.getAsJsonObject();
                String id = str(o, "id");
                if (id.isBlank() || methods.containsKey(id)) continue;
                methods.put(id, new MethodEntry(id, str(o, "display"), str(o, "realm_min"),
                        str(o, "school"), str(o, "attribute")));
            }
        }

        Map<String, FlightBinding> flights = new LinkedHashMap<>();
        JsonObject flightRoot = readJson("data/" + SeekingImmortalsMod.MODID + "/catalog/flight_vehicle_bindings.json");
        if (flightRoot != null) {
            for (JsonElement element : array(flightRoot, "vehicles")) {
                if (!element.isJsonObject()) continue;
                JsonObject o = element.getAsJsonObject();
                String id = str(o, "id");
                if (id.isBlank()) continue;
                flights.put(id, new FlightBinding(id, str(o, "display"), str(o, "realm_min"),
                        o.has("speed") && o.get("speed").isJsonPrimitive() ? o.get("speed").getAsDouble() : 0.0D,
                        str(o, "fuel"), str(o, "carrier_item"), str(o, "fuel_item"),
                        o.has("fuel_count") && o.get("fuel_count").isJsonPrimitive() ? o.get("fuel_count").getAsInt() : 1));
            }
        }

        return new Snapshot(Collections.unmodifiableMap(flavors),
                Collections.unmodifiableMap(manuals),
                Collections.unmodifiableMap(methods),
                Collections.unmodifiableMap(flights));
    }

    private static ManualEntry parseManualEntry(JsonObject o) {
        String realmMin = str(o, "realm_min");
        String note = str(o, "note");
        // text_material shape: learn_requirements.study.{realm_min,note}
        if (o.has("learn_requirements") && o.get("learn_requirements").isJsonObject()) {
            JsonObject learn = o.getAsJsonObject("learn_requirements");
            if (learn.has("study") && learn.get("study").isJsonObject()) {
                JsonObject study = learn.getAsJsonObject("study");
                if (realmMin.isBlank()) {
                    realmMin = str(study, "realm_min");
                }
                if (note.isBlank()) {
                    note = str(study, "note");
                }
            }
        }
        int forgeGrade = 0;
        if (o.has("unlocks_forge_grade") && o.get("unlocks_forge_grade").isJsonPrimitive()) {
            try {
                forgeGrade = o.get("unlocks_forge_grade").getAsInt();
            } catch (Exception ignored) {
                forgeGrade = 0;
            }
        }
        List<String> unlocks = stringList(o.get("unlocks"));
        if (unlocks.isEmpty() && forgeGrade > 0) {
            unlocks = List.of("forge_grade_" + forgeGrade);
        }
        return new ManualEntry(
                str(o, "id"),
                str(o, "display"),
                str(o, "type"),
                unlocks,
                realmMin,
                note,
                forgeGrade,
                str(o, "recipe_id"),
                stringList(o.get("source")));
    }

    private static JsonObject readJson(String path) {
        try (InputStream stream = TextMaterialCatalogService.class.getClassLoader().getResourceAsStream(path)) {
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

    private static String str(JsonObject object, String key) {
        if (object == null || !object.has(key) || object.get(key).isJsonNull()) {
            return "";
        }
        try {
            return object.get(key).getAsString();
        } catch (Exception ignored) {
            return String.valueOf(object.get(key));
        }
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
}
