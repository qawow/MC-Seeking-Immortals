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

    public record ManualEntry(String id, String display, String type, List<String> unlocks, String realmMin, String note) {}

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
        JsonObject manualRoot = readJson("data/" + SeekingImmortalsMod.MODID + "/catalog/manuals_catalog.json");
        if (manualRoot != null) {
            for (JsonElement element : array(manualRoot, "manuals")) {
                if (!element.isJsonObject()) continue;
                JsonObject o = element.getAsJsonObject();
                String id = str(o, "id");
                if (id.isBlank()) continue;
                manuals.put(id, new ManualEntry(id, str(o, "display"), str(o, "type"),
                        stringList(o.get("unlocks")), str(o, "realm_min"), str(o, "note")));
            }
        }

        Map<String, MethodEntry> methods = new LinkedHashMap<>();
        JsonObject methodRoot = readJson("data/" + SeekingImmortalsMod.MODID + "/catalog/cultivation_methods_index.json");
        if (methodRoot != null) {
            for (JsonElement element : array(methodRoot, "methods")) {
                if (!element.isJsonObject()) continue;
                JsonObject o = element.getAsJsonObject();
                String id = str(o, "id");
                if (id.isBlank()) continue;
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
