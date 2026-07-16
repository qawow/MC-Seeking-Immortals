package com.xunxian.seekingimmortals.region;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.xunxian.seekingimmortals.SeekingImmortalsMod;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * travel_routes_v102 + trade_routes connectivity graph.
 * Consumed by M05 trade route pricing and M12 NPC placement.
 */
public final class TravelRouteGraph {
    private static final Snapshot BUILTIN = loadBuiltin();

    private TravelRouteGraph() {}

    public static Snapshot builtin() {
        return BUILTIN;
    }

    public record Hub(String id, String display, String realmBand, String regionLabel) {
        public Hub {
            id = id == null ? "" : id;
            display = display == null ? "" : display;
            realmBand = realmBand == null ? "" : realmBand;
            regionLabel = regionLabel == null ? "" : regionLabel;
        }
    }

    public record RouteEdge(String id, String from, String to, List<String> modes,
                            int minDays, int maxDays, int feeLowStone, List<String> riskEvents,
                            String note, String source) {
        public RouteEdge {
            id = id == null ? "" : id;
            from = from == null ? "" : from;
            to = to == null ? "" : to;
            modes = modes == null ? List.of() : List.copyOf(modes);
            minDays = Math.max(0, minDays);
            maxDays = Math.max(minDays, maxDays);
            feeLowStone = Math.max(0, feeLowStone);
            riskEvents = riskEvents == null ? List.of() : List.copyOf(riskEvents);
            note = note == null ? "" : note;
            source = source == null ? "" : source;
        }
    }

    public record Snapshot(List<Hub> hubs, List<RouteEdge> routes,
                           Map<String, List<RouteEdge>> adjacency) {
        public Snapshot {
            hubs = hubs == null ? List.of() : List.copyOf(hubs);
            routes = routes == null ? List.of() : List.copyOf(routes);
            Map<String, List<RouteEdge>> copy = new LinkedHashMap<>();
            if (adjacency != null) {
                adjacency.forEach((key, value) -> copy.put(key, List.copyOf(value)));
            }
            adjacency = Collections.unmodifiableMap(copy);
        }

        public Optional<Hub> findHub(String hubId) {
            if (hubId == null || hubId.isBlank()) {
                return Optional.empty();
            }
            for (Hub hub : hubs) {
                if (hub.id().equals(hubId) || hub.id().equalsIgnoreCase(hubId)) {
                    return Optional.of(hub);
                }
            }
            return Optional.empty();
        }

        public List<RouteEdge> routesFrom(String nodeId) {
            if (nodeId == null || nodeId.isBlank()) {
                return List.of();
            }
            List<RouteEdge> direct = adjacency.get(nodeId);
            if (direct != null) {
                return direct;
            }
            for (Map.Entry<String, List<RouteEdge>> entry : adjacency.entrySet()) {
                if (entry.getKey().equalsIgnoreCase(nodeId)) {
                    return entry.getValue();
                }
            }
            return List.of();
        }

        public List<RouteEdge> routesBetween(String from, String to) {
            if (from == null || to == null || from.isBlank() || to.isBlank()) {
                return List.of();
            }
            List<RouteEdge> result = new ArrayList<>();
            for (RouteEdge edge : routes) {
                if ((edge.from().equalsIgnoreCase(from) && edge.to().equalsIgnoreCase(to))
                        || (edge.from().equalsIgnoreCase(to) && edge.to().equalsIgnoreCase(from))) {
                    result.add(edge);
                }
            }
            return List.copyOf(result);
        }

        public boolean isConnected(String from, String to) {
            return !routesBetween(from, to).isEmpty() || hasPath(from, to, 6);
        }

        private boolean hasPath(String from, String to, int maxDepth) {
            if (from == null || to == null || from.isBlank() || to.isBlank()) {
                return false;
            }
            if (from.equalsIgnoreCase(to)) {
                return true;
            }
            LinkedHashSet<String> visited = new LinkedHashSet<>();
            ArrayList<String> queue = new ArrayList<>();
            queue.add(from);
            visited.add(from.toLowerCase(Locale.ROOT));
            int depth = 0;
            int layerEnd = 1;
            for (int i = 0; i < queue.size() && depth <= maxDepth; i++) {
                String current = queue.get(i);
                for (RouteEdge edge : routesFrom(current)) {
                    String next = edge.to().equalsIgnoreCase(current) ? edge.from() : edge.to();
                    if (next.isBlank()) {
                        continue;
                    }
                    String key = next.toLowerCase(Locale.ROOT);
                    if (key.equals(to.toLowerCase(Locale.ROOT))) {
                        return true;
                    }
                    if (visited.add(key)) {
                        queue.add(next);
                    }
                }
                if (i + 1 == layerEnd) {
                    depth++;
                    layerEnd = queue.size();
                }
            }
            return false;
        }

        public int hubCount() {
            return hubs.size();
        }

        public int routeCount() {
            return routes.size();
        }

        public Set<String> nodeIds() {
            LinkedHashSet<String> nodes = new LinkedHashSet<>();
            hubs.forEach(hub -> nodes.add(hub.id()));
            routes.forEach(edge -> {
                if (!edge.from().isBlank()) {
                    nodes.add(edge.from());
                }
                if (!edge.to().isBlank()) {
                    nodes.add(edge.to());
                }
            });
            return Collections.unmodifiableSet(nodes);
        }
    }

    private static Snapshot loadBuiltin() {
        List<Hub> hubs = new ArrayList<>();
        List<RouteEdge> routes = new ArrayList<>();
        Map<String, List<RouteEdge>> adjacency = new LinkedHashMap<>();

        JsonObject travel = readJson("data/" + SeekingImmortalsMod.MODID + "/text_material/travel_routes.json");
        if (travel != null) {
            for (JsonElement element : array(travel, "hubs")) {
                if (!element.isJsonObject()) {
                    continue;
                }
                JsonObject object = element.getAsJsonObject();
                String id = str(object, "id");
                if (id.isBlank()) {
                    continue;
                }
                hubs.add(new Hub(id, str(object, "display"), str(object, "realm_band"), str(object, "region")));
            }
            int index = 0;
            for (JsonElement element : array(travel, "routes")) {
                if (!element.isJsonObject()) {
                    continue;
                }
                JsonObject object = element.getAsJsonObject();
                String from = str(object, "from");
                String to = str(object, "to");
                if (from.isBlank() || to.isBlank()) {
                    continue;
                }
                int minDays = 0;
                int maxDays = 0;
                JsonArray days = array(object, "base_days");
                if (days.size() >= 1 && days.get(0).isJsonPrimitive() && days.get(0).getAsJsonPrimitive().isNumber()) {
                    minDays = days.get(0).getAsInt();
                }
                if (days.size() >= 2 && days.get(1).isJsonPrimitive() && days.get(1).getAsJsonPrimitive().isNumber()) {
                    maxDays = days.get(1).getAsInt();
                } else {
                    maxDays = minDays;
                }
                String id = "travel_" + from + "_to_" + to + "_" + index++;
                RouteEdge edge = new RouteEdge(id, from, to, strings(object, "modes"),
                        minDays, maxDays, 0, List.of(), str(object, "note"), "travel_routes");
                routes.add(edge);
                adjacency.computeIfAbsent(from, ignored -> new ArrayList<>()).add(edge);
                adjacency.computeIfAbsent(to, ignored -> new ArrayList<>()).add(edge);
            }
        }

        JsonObject trade = readJson("data/" + SeekingImmortalsMod.MODID + "/text_material/trade_routes.json");
        if (trade != null) {
            for (JsonElement element : array(trade, "routes")) {
                if (!element.isJsonObject()) {
                    continue;
                }
                JsonObject object = element.getAsJsonObject();
                String id = str(object, "id");
                String from = str(object, "from");
                String to = str(object, "to");
                List<String> regionChain = strings(object, "regions");
                List<String> modes = strings(object, "transport");
                if (modes.isEmpty()) {
                    modes = strings(object, "modes");
                }
                List<String> risks = strings(object, "risk_events");
                if (risks.isEmpty()) {
                    risks = strings(object, "risks");
                }
                int fee = intValue(object, "fee_low_stone", 0);
                int days = intValue(object, "duration_days", 0);
                if (!from.isBlank() && !to.isBlank()) {
                    if (id.isBlank()) {
                        id = "trade_" + from + "_to_" + to;
                    }
                    RouteEdge edge = new RouteEdge(id, from, to, modes, days, days, fee, risks, str(object, "display"), "trade_routes");
                    routes.add(edge);
                    adjacency.computeIfAbsent(from, ignored -> new ArrayList<>()).add(edge);
                    adjacency.computeIfAbsent(to, ignored -> new ArrayList<>()).add(edge);
                } else if (regionChain.size() >= 2) {
                    for (int i = 0; i < regionChain.size() - 1; i++) {
                        String a = regionChain.get(i);
                        String b = regionChain.get(i + 1);
                        String edgeId = (id.isBlank() ? "trade_chain" : id) + "_" + a + "_to_" + b;
                        RouteEdge edge = new RouteEdge(edgeId, a, b, modes, days, days, fee, risks, str(object, "display"), "trade_routes");
                        routes.add(edge);
                        adjacency.computeIfAbsent(a, ignored -> new ArrayList<>()).add(edge);
                        adjacency.computeIfAbsent(b, ignored -> new ArrayList<>()).add(edge);
                    }
                }
            }
        }

        Map<String, List<RouteEdge>> frozen = new LinkedHashMap<>();
        adjacency.forEach((key, value) -> frozen.put(key, List.copyOf(value)));
        return new Snapshot(hubs, routes, frozen);
    }

    private static JsonObject readJson(String path) {
        try (InputStream stream = TravelRouteGraph.class.getClassLoader().getResourceAsStream(path)) {
            if (stream == null) {
                SeekingImmortalsMod.LOGGER.warn("Missing travel route resource {}", path);
                return null;
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                return JsonParser.parseReader(reader).getAsJsonObject();
            }
        } catch (Exception exception) {
            SeekingImmortalsMod.LOGGER.warn("Failed to load travel route resource {}", path, exception);
            return null;
        }
    }

    private static JsonArray array(JsonObject object, String key) {
        return object != null && object.has(key) && object.get(key).isJsonArray()
                ? object.getAsJsonArray(key) : new JsonArray();
    }

    private static List<String> strings(JsonObject object, String key) {
        List<String> values = new ArrayList<>();
        for (JsonElement element : array(object, key)) {
            if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
                String value = element.getAsString();
                if (!value.isBlank()) {
                    values.add(value);
                }
            }
        }
        return List.copyOf(values);
    }

    private static String str(JsonObject object, String key) {
        return object != null && object.has(key) && object.get(key).isJsonPrimitive()
                ? object.get(key).getAsString() : "";
    }

    private static int intValue(JsonObject object, String key, int fallback) {
        return object != null && object.has(key) && object.get(key).isJsonPrimitive()
                && object.get(key).getAsJsonPrimitive().isNumber()
                ? object.get(key).getAsInt() : fallback;
    }
}
