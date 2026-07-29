package com.xunxian.seekingimmortals.quest;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.xunxian.seekingimmortals.SeekingImmortalsMod;
import com.xunxian.seekingimmortals.catalog.TextMaterialCatalogService;
import com.xunxian.seekingimmortals.cultivation.Realm;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/** Strict, data-driven proof routes for the playable detailed quest chains. */
public final class DetailedQuestProofCatalog {
    public static final String RESOURCE_PATH = "data/" + SeekingImmortalsMod.MODID
            + "/text_material/detailed_quest_proof_routes.json";
    private static final String SOURCE_PATH = "data/" + SeekingImmortalsMod.MODID
            + "/text_material/quest_chains_playable_v141.json";
    private static final int SCHEMA_VERSION = 1;
    private static final int EXPECTED_CHAIN_COUNT = 23;
    private static final int EXPECTED_STEP_COUNT = 95;
    private static final Pattern ID = Pattern.compile("[a-z][a-z0-9_]*");
    private static final Pattern FAILURE_KEY = Pattern.compile("[a-z][a-z0-9_.]*");

    private static final Set<String> PROOF_TYPES = Set.of(
            "REGION_ENTER", "DIMENSION_ENTER", "STRUCTURE_FORMED", "NPC_DIALOGUE",
            "ITEM_ACQUIRED", "ITEM_DELIVERED", "CRAFT_COMPLETED", "ALCHEMY_COMPLETED",
            "ENTITY_KILLED", "ENTITY_CAPTURED_ALIVE", "ENCOUNTER_CLEARED", "ESCORT_COMPLETED",
            "METHOD_LAYER_REACHED", "REALM_REACHED", "TECHNIQUE_LEARNED", "SHOP_TRANSACTION",
            "AUCTION_TRANSACTION", "REPUTATION_REACHED", "CHOICE_COMMITTED", "INFO_ACKNOWLEDGED");
    private static final Set<String> OWNER_POLICIES = Set.of("PLAYER", "PARTY_LEADER", "PARTY_MEMBER", "SERVER");
    private static final Set<String> PARTY_POLICIES = Set.of("SOLO", "SOLO_OR_PARTY", "PARTY_ONLY");
    private static final Set<String> CONSUME_POLICIES = Set.of("NONE", "ON_SUCCESS", "ON_ACCEPT", "RESERVE_THEN_COMMIT");
    private static final Set<String> REPEAT_POLICIES = Set.of("IDEMPOTENT", "ONE_SHOT", "REJECT_DUPLICATE");
    private static final Set<String> PRODUCERS = Set.of(
            "alchemy", "auction", "capture", "crafting", "cultivation", "dialogue_choice",
            "dimension_travel", "encounter", "escort", "item_delivery", "item_pickup", "living_kill",
            "npc_dialogue", "region_travel", "reputation", "shop", "structure_runtime");
    private static final Set<String> PARAMETER_KEYS = Set.of(
            "auction", "choice", "dimension", "entity", "faction", "item", "method", "npc",
            "realm", "region", "shop", "station", "structure", "technique");
    private static final Map<String, String> EXPECTED_PARAMETER = Map.ofEntries(
            Map.entry("REGION_ENTER", "region"),
            Map.entry("DIMENSION_ENTER", "dimension"),
            Map.entry("STRUCTURE_FORMED", "structure"),
            Map.entry("NPC_DIALOGUE", "npc"),
            Map.entry("ITEM_ACQUIRED", "item"),
            Map.entry("ITEM_DELIVERED", "item"),
            Map.entry("CRAFT_COMPLETED", "item"),
            Map.entry("ALCHEMY_COMPLETED", "station"),
            Map.entry("ENTITY_KILLED", "entity"),
            Map.entry("ENTITY_CAPTURED_ALIVE", "entity"),
            Map.entry("ENCOUNTER_CLEARED", "region"),
            Map.entry("ESCORT_COMPLETED", "region"),
            Map.entry("METHOD_LAYER_REACHED", "method"),
            Map.entry("REALM_REACHED", "realm"),
            Map.entry("TECHNIQUE_LEARNED", "technique"),
            Map.entry("SHOP_TRANSACTION", "shop"),
            Map.entry("AUCTION_TRANSACTION", "auction"),
            Map.entry("REPUTATION_REACHED", "faction"),
            Map.entry("CHOICE_COMMITTED", "choice"),
            Map.entry("INFO_ACKNOWLEDGED", "choice"));
    private static final Map<String, String> EXPECTED_PRODUCER = Map.ofEntries(
            Map.entry("REGION_ENTER", "region_travel"),
            Map.entry("DIMENSION_ENTER", "dimension_travel"),
            Map.entry("STRUCTURE_FORMED", "structure_runtime"),
            Map.entry("NPC_DIALOGUE", "npc_dialogue"),
            Map.entry("ITEM_ACQUIRED", "item_pickup"),
            Map.entry("ITEM_DELIVERED", "item_delivery"),
            Map.entry("CRAFT_COMPLETED", "crafting"),
            Map.entry("ALCHEMY_COMPLETED", "alchemy"),
            Map.entry("ENTITY_KILLED", "living_kill"),
            Map.entry("ENTITY_CAPTURED_ALIVE", "capture"),
            Map.entry("ENCOUNTER_CLEARED", "encounter"),
            Map.entry("ESCORT_COMPLETED", "escort"),
            Map.entry("METHOD_LAYER_REACHED", "cultivation"),
            Map.entry("REALM_REACHED", "cultivation"),
            Map.entry("TECHNIQUE_LEARNED", "cultivation"),
            Map.entry("SHOP_TRANSACTION", "shop"),
            Map.entry("AUCTION_TRANSACTION", "auction"),
            Map.entry("REPUTATION_REACHED", "reputation"),
            Map.entry("CHOICE_COMMITTED", "dialogue_choice"),
            Map.entry("INFO_ACKNOWLEDGED", "npc_dialogue"));

    private static final Snapshot BUILTIN = loadAndValidate(readSourceSteps());

    private DetailedQuestProofCatalog() {}

    public record Route(String chainId, int step, String proofType, String eventId,
                        Map<String, String> requiredParams, String ownerPolicy, String partyPolicy,
                        int minimumLayer, String minimumRealm, String consumePolicy, String repeatPolicy, String failureKey,
                        boolean allowHistoryReplay, String producer) {
        public Route {
            chainId = normalize(chainId);
            proofType = normalize(proofType).toUpperCase(Locale.ROOT);
            eventId = normalize(eventId);
            requiredParams = requiredParams == null
                    ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(requiredParams));
            minimumLayer = Math.max(0, minimumLayer);
            minimumRealm = normalize(minimumRealm);
            ownerPolicy = normalize(ownerPolicy).toUpperCase(Locale.ROOT);
            partyPolicy = normalize(partyPolicy).toUpperCase(Locale.ROOT);
            consumePolicy = normalize(consumePolicy).toUpperCase(Locale.ROOT);
            repeatPolicy = normalize(repeatPolicy).toUpperCase(Locale.ROOT);
            failureKey = normalize(failureKey);
            producer = normalize(producer);
        }

        /** Compatibility constructor for callers that do not declare a numeric cultivation threshold. */
        public Route(String chainId, int step, String proofType, String eventId,
                     Map<String, String> requiredParams, String ownerPolicy, String partyPolicy,
                     String consumePolicy, String repeatPolicy, String failureKey,
                     boolean allowHistoryReplay, String producer) {
            this(chainId, step, proofType, eventId, requiredParams, ownerPolicy, partyPolicy,
                    0, "", consumePolicy, repeatPolicy, failureKey, allowHistoryReplay, producer);
        }

        public String parameter(String key) {
            return requiredParams.getOrDefault(normalize(key), "");
        }
    }

    public record Snapshot(List<Route> routes, Map<String, Route> byStep,
                           Set<String> chainIds, int stepCount) {
        public Snapshot {
            routes = routes == null ? List.of() : List.copyOf(routes);
            byStep = byStep == null ? Map.of()
                    : Collections.unmodifiableMap(new LinkedHashMap<>(byStep));
            chainIds = chainIds == null ? Set.of() : Set.copyOf(chainIds);
        }

        public int routeCount() {
            return routes.size();
        }

        public Route find(String chainId, int step) {
            return byStep.get(key(chainId, step));
        }

        public boolean covers(String chainId, int step) {
            return find(chainId, step) != null;
        }
    }

    public static Snapshot builtin() {
        return BUILTIN;
    }

    /**
     * Loads and strictly validates the route resource against the runtime chain step counts.
     * This overload is used by {@link DetailedQuestRuntimeService} so the route table cannot
     * silently drift from the chain snapshot used for gameplay.
     */
    public static Snapshot loadAndValidate(Map<String, Integer> sourceSteps) {
        JsonObject root = readObject(RESOURCE_PATH);
        List<String> errors = new java.util.ArrayList<>();
        if (root == null) {
            throw new IllegalStateException("Missing detailed quest proof route resource: " + RESOURCE_PATH);
        }
        if (integer(root, "schema_version", -1) != SCHEMA_VERSION) {
            errors.add("schema_version must be " + SCHEMA_VERSION);
        }
        if (!SOURCE_PATH.substring(SOURCE_PATH.lastIndexOf('/') + 1).equals(string(root, "source"))) {
            errors.add("source must name " + SOURCE_PATH.substring(SOURCE_PATH.lastIndexOf('/') + 1));
        }

        List<Route> routes = new java.util.ArrayList<>();
        Map<String, Route> byStep = new LinkedHashMap<>();
        JsonArray routeArray = array(root, "routes");
        for (int index = 0; index < routeArray.size(); index++) {
            JsonElement element = routeArray.get(index);
            if (!element.isJsonObject()) {
                errors.add("routes[" + index + "] must be an object");
                continue;
            }
            Route route = parseRoute(element.getAsJsonObject(), index, errors);
            if (route == null) {
                continue;
            }
            String key = key(route.chainId(), route.step());
            if (byStep.putIfAbsent(key, route) != null) {
                errors.add("duplicate route " + key);
            } else {
                routes.add(route);
            }
        }
        validateCoverage(sourceSteps, routes, byStep, errors);
        if (routes.size() != EXPECTED_STEP_COUNT) {
            errors.add("expected " + EXPECTED_STEP_COUNT + " valid routes, got " + routes.size());
        }
        if (sourceSteps.size() != EXPECTED_CHAIN_COUNT) {
            errors.add("expected " + EXPECTED_CHAIN_COUNT + " source chains, got " + sourceSteps.size());
        }
        if (!errors.isEmpty()) {
            throw new IllegalStateException("Invalid detailed quest proof routes: " + String.join("; ", errors));
        }
        return new Snapshot(routes, byStep, new LinkedHashSet<>(sourceSteps.keySet()), routes.size());
    }

    private static Route parseRoute(JsonObject object, int index, List<String> errors) {
        String chainId = string(object, "chain_id");
        int step = integer(object, "step", -1);
        String proofType = string(object, "proof_type").toUpperCase(Locale.ROOT);
        String eventId = string(object, "event_id");
        String ownerPolicy = string(object, "owner_policy").toUpperCase(Locale.ROOT);
        String partyPolicy = string(object, "party_policy").toUpperCase(Locale.ROOT);
        String consumePolicy = string(object, "consume_policy").toUpperCase(Locale.ROOT);
        String repeatPolicy = string(object, "repeat_policy").toUpperCase(Locale.ROOT);
        String failureKey = string(object, "failure_key");
        String producer = string(object, "producer");
        String location = "routes[" + index + "]";
        boolean valid = true;
        if (!validId(chainId)) {
            errors.add(location + " has invalid chain_id");
            valid = false;
        }
        if (step < 1) {
            errors.add(location + " step must be positive");
            valid = false;
        }
        if (!PROOF_TYPES.contains(proofType)) {
            errors.add(location + " has unknown proof_type " + proofType);
            valid = false;
        }
        if (!OWNER_POLICIES.contains(ownerPolicy) || "ADMIN_ONLY".equals(ownerPolicy)) {
            errors.add(location + " has illegal owner_policy " + ownerPolicy);
            valid = false;
        }
        if (!PARTY_POLICIES.contains(partyPolicy) || "ADMIN_ONLY".equals(partyPolicy)) {
            errors.add(location + " has illegal party_policy " + partyPolicy);
            valid = false;
        }
        if (!CONSUME_POLICIES.contains(consumePolicy)) {
            errors.add(location + " has illegal consume_policy " + consumePolicy);
            valid = false;
        }
        if (!REPEAT_POLICIES.contains(repeatPolicy)) {
            errors.add(location + " has illegal repeat_policy " + repeatPolicy);
            valid = false;
        }
        if (!FAILURE_KEY.matcher(failureKey).matches()) {
            errors.add(location + " has invalid failure_key");
            valid = false;
        }
        if (!PRODUCERS.contains(producer)) {
            errors.add(location + " has unknown producer " + producer);
            valid = false;
        }

        Map<String, String> params = parseParams(object, location, errors);
        if (PROOF_TYPES.contains(proofType)) {
            String expectedKey = EXPECTED_PARAMETER.get(proofType);
            if (!params.keySet().equals(Set.of(expectedKey))) {
                errors.add(location + " " + proofType + " requires only parameter " + expectedKey);
                valid = false;
            }
            if (!EXPECTED_PRODUCER.get(proofType).equals(producer)) {
                errors.add(location + " " + proofType + " requires producer " + EXPECTED_PRODUCER.get(proofType));
                valid = false;
            }
            String expectedEvent = proofType.toLowerCase(Locale.ROOT) + ":" + chainId + ":step_" + step;
            if (!eventId.equals(expectedEvent)) {
                errors.add(location + " event_id must be " + expectedEvent);
                valid = false;
            }
        }
        int minimumLayer = integer(object, "minimum_layer", 0);
        String minimumRealm = normalize(string(object, "minimum_realm"));
        if ("METHOD_LAYER_REACHED".equals(proofType)) {
            if (minimumLayer < 1) {
                errors.add(location + " METHOD_LAYER_REACHED requires minimum_layer >= 1");
                valid = false;
            }
            String methodId = params.get("method");
            if (methodId == null || TextMaterialCatalogService.builtin().findMethod(methodId).isEmpty()) {
                errors.add(location + " references unknown method " + methodId);
                valid = false;
            }
            if (!minimumRealm.isBlank() && Realm.fromDesignId(minimumRealm) == null) {
                errors.add(location + " references unknown minimum_realm " + minimumRealm);
                valid = false;
            }
        } else if ("REALM_REACHED".equals(proofType)
                && Realm.fromDesignId(params.get("realm")) == null) {
            errors.add(location + " references unknown realm " + params.get("realm"));
            valid = false;
        } else if (minimumLayer != 0 || !minimumRealm.isBlank()) {
            errors.add(location + " minimum_layer/minimum_realm are only valid for METHOD_LAYER_REACHED");
            valid = false;
        }
        if (!object.has("allow_history_replay") || !object.get("allow_history_replay").isJsonPrimitive()
                || !object.getAsJsonPrimitive("allow_history_replay").isBoolean()) {
            errors.add(location + " allow_history_replay must be boolean");
            valid = false;
        }
        if (!valid) {
            return null;
        }
        return new Route(chainId, step, proofType, eventId, params, ownerPolicy, partyPolicy,
                minimumLayer, minimumRealm, consumePolicy, repeatPolicy, failureKey,
                object.get("allow_history_replay").getAsBoolean(), producer);
    }

    private static Map<String, String> parseParams(JsonObject object, String location, List<String> errors) {
        Map<String, String> params = new LinkedHashMap<>();
        JsonElement element = object.get("required_params");
        if (element == null || !element.isJsonObject()) {
            errors.add(location + " required_params must be an object");
            return params;
        }
        for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
            String key = normalize(entry.getKey());
            JsonElement value = entry.getValue();
            if (!PARAMETER_KEYS.contains(key)) {
                errors.add(location + " has unknown parameter key " + key);
                continue;
            }
            if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()
                    || !validId(value.getAsString())) {
                errors.add(location + " parameter " + key + " must be a lowercase id string");
                continue;
            }
            if (params.put(key, normalize(value.getAsString())) != null) {
                errors.add(location + " has duplicate parameter " + key);
            }
        }
        return params;
    }

    private static void validateCoverage(Map<String, Integer> sourceSteps, List<Route> routes,
                                         Map<String, Route> byStep, List<String> errors) {
        if (sourceSteps == null) {
            errors.add("source step map is null");
            return;
        }
        for (Map.Entry<String, Integer> entry : sourceSteps.entrySet()) {
            String chainId = normalize(entry.getKey());
            int count = entry.getValue() == null ? 0 : entry.getValue();
            for (int step = 1; step <= count; step++) {
                if (!byStep.containsKey(key(chainId, step))) {
                    errors.add("missing route " + key(chainId, step));
                }
            }
        }
        for (Route route : routes) {
            Integer count = sourceSteps.get(route.chainId());
            if (count == null) {
                errors.add("route references unknown chain " + route.chainId());
            } else if (route.step() > count) {
                errors.add("route step exceeds source chain " + key(route.chainId(), route.step()));
            }
        }
    }

    private static Map<String, Integer> readSourceSteps() {
        JsonObject root = readObject(SOURCE_PATH);
        if (root == null) {
            throw new IllegalStateException("Missing detailed quest source resource: " + SOURCE_PATH);
        }
        Map<String, Integer> result = new LinkedHashMap<>();
        JsonArray chains = array(root, "chains");
        for (JsonElement element : chains) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject chain = element.getAsJsonObject();
            String id = normalize(string(chain, "id"));
            if (id.isBlank()) {
                continue;
            }
            int count = 0;
            for (JsonElement step : array(chain, "steps")) {
                if (step.isJsonObject()) {
                    count++;
                }
            }
            if (result.put(id, count) != null) {
                throw new IllegalStateException("Duplicate detailed quest source chain " + id);
            }
        }
        return result;
    }

    private static JsonObject readObject(String path) {
        try (InputStream stream = DetailedQuestProofCatalog.class.getClassLoader().getResourceAsStream(path)) {
            if (stream == null) {
                return null;
            }
            try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                JsonElement element = JsonParser.parseReader(reader);
                return element != null && element.isJsonObject() ? element.getAsJsonObject() : null;
            }
        } catch (Exception exception) {
            SeekingImmortalsMod.LOGGER.error("Failed to load detailed quest proof catalog {}", path, exception);
            return null;
        }
    }

    private static JsonArray array(JsonObject object, String key) {
        return object != null && object.has(key) && object.get(key).isJsonArray()
                ? object.getAsJsonArray(key) : new JsonArray();
    }

    private static String string(JsonObject object, String key) {
        if (object == null || !object.has(key) || object.get(key).isJsonNull()) {
            return "";
        }
        try {
            return object.get(key).getAsString().trim();
        } catch (RuntimeException ignored) {
            return "";
        }
    }

    private static int integer(JsonObject object, String key, int fallback) {
        if (object == null || !object.has(key)) {
            return fallback;
        }
        try {
            return object.get(key).getAsInt();
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private static boolean validId(String value) {
        return value != null && ID.matcher(value.trim().toLowerCase(Locale.ROOT)).matches();
    }

    private static String key(String chainId, int step) {
        return normalize(chainId) + ":" + step;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
