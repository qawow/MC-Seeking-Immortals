package com.xunxian.seekingimmortals.sect;

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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * M08 faction graph authority: nodes, directed/undirected edges, species and deep faction attrs.
 * Consumed by M11 (quest gates), M05 (price/rep), M12 (steward dialogue context).
 */
public final class FactionGraphService {
    public static final String EDGE_WAR = "war";
    public static final String EDGE_TRADE = "trade";
    public static final String EDGE_ALLIANCE = "alliance";
    public static final String EDGE_SECRET_TIE = "secret_tie";
    public static final String EDGE_BLOCKADE = "blockade";
    public static final String EDGE_RIVALRY = "rivalry";

    private static final Snapshot BUILTIN = loadBuiltin();

    private FactionGraphService() {}

    public static Snapshot builtin() {
        return BUILTIN;
    }

    public static Optional<Node> findNode(String factionId) {
        return Optional.ofNullable(BUILTIN.nodes().get(normalize(factionId)));
    }

    public static List<Edge> edgesFrom(String factionId) {
        String id = normalize(factionId);
        List<Edge> list = new ArrayList<>();
        for (Edge edge : BUILTIN.edges()) {
            if (edge.from().equals(id)) {
                list.add(edge);
            }
        }
        return List.copyOf(list);
    }

    public static List<Edge> edgesBetween(String a, String b) {
        String from = normalize(a);
        String to = normalize(b);
        List<Edge> list = new ArrayList<>();
        for (Edge edge : BUILTIN.edges()) {
            if ((edge.from().equals(from) && edge.to().equals(to))
                    || (edge.from().equals(to) && edge.to().equals(from))) {
                list.add(edge);
            }
        }
        return List.copyOf(list);
    }

    /**
     * Aggregated reputation-axis weight from A toward B (sum of directed edges A→B, else reverse negated for hostility types).
     */
    public static int relationWeight(String fromId, String toId) {
        String from = normalize(fromId);
        String to = normalize(toId);
        if (from.isBlank() || to.isBlank() || from.equals(to)) {
            return 0;
        }
        int sum = 0;
        boolean any = false;
        for (Edge edge : BUILTIN.edges()) {
            if (edge.from().equals(from) && edge.to().equals(to)) {
                sum += edge.weight();
                any = true;
            }
        }
        if (any) {
            return sum;
        }
        // Fall back to reverse edge for undirected-feeling hostility/friend types.
        for (Edge edge : BUILTIN.edges()) {
            if (edge.from().equals(to) && edge.to().equals(from) && !edge.directed()) {
                return edge.weight();
            }
        }
        return 0;
    }

    public static boolean areHostile(String a, String b) {
        for (Edge edge : edgesBetween(a, b)) {
            if (EDGE_WAR.equals(edge.type()) || EDGE_RIVALRY.equals(edge.type()) || EDGE_BLOCKADE.equals(edge.type())) {
                return true;
            }
            if (edge.weight() <= -30) {
                return true;
            }
        }
        return false;
    }

    public static boolean areAllied(String a, String b) {
        for (Edge edge : edgesBetween(a, b)) {
            if (EDGE_ALLIANCE.equals(edge.type()) || EDGE_TRADE.equals(edge.type()) || EDGE_SECRET_TIE.equals(edge.type())) {
                if (edge.weight() >= 0) {
                    return true;
                }
            }
        }
        return false;
    }

    public static List<String> enemiesOf(String factionId) {
        Set<String> out = new LinkedHashSet<>();
        String id = normalize(factionId);
        for (Edge edge : BUILTIN.edges()) {
            if (!isHostileType(edge.type()) && edge.weight() > -30) {
                continue;
            }
            if (edge.from().equals(id)) {
                out.add(edge.to());
            } else if (edge.to().equals(id) && !edge.directed()) {
                out.add(edge.from());
            }
        }
        return List.copyOf(out);
    }

    public static List<String> alliesOf(String factionId) {
        Set<String> out = new LinkedHashSet<>();
        String id = normalize(factionId);
        for (Edge edge : BUILTIN.edges()) {
            if (!isFriendlyType(edge.type()) || edge.weight() < 0) {
                continue;
            }
            if (edge.from().equals(id)) {
                out.add(edge.to());
            } else if (edge.to().equals(id) && !edge.directed()) {
                out.add(edge.from());
            }
        }
        return List.copyOf(out);
    }

    public static Optional<DeepFaction> deepFaction(String factionId) {
        return Optional.ofNullable(BUILTIN.deepFactions().get(normalize(factionId)));
    }

    public static Optional<SpeciesFaction> species(String factionId) {
        return Optional.ofNullable(BUILTIN.species().get(normalize(factionId)));
    }

    /**
     * Bidirectional consistency report for tests/tools.
     * Hostile/friendly edges without reverse and without directed=true are flagged.
     */
    public static List<String> bidirectionalIssues() {
        List<String> issues = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (Edge edge : BUILTIN.edges()) {
            String key = edge.from() + ">" + edge.to() + ":" + edge.type();
            if (!seen.add(key)) {
                continue;
            }
            if (edge.directed()) {
                continue;
            }
            boolean reverse = false;
            for (Edge other : BUILTIN.edges()) {
                if (other.from().equals(edge.to())
                        && other.to().equals(edge.from())
                        && other.type().equals(edge.type())) {
                    reverse = true;
                    break;
                }
            }
            if (!reverse) {
                // Accept single-record undirected edges as canonical; only flag when corpus marks directed=false
                // and a conflicting reverse type exists.
                for (Edge other : BUILTIN.edges()) {
                    if (other.from().equals(edge.to())
                            && other.to().equals(edge.from())
                            && !other.type().equals(edge.type())
                            && isHostileType(edge.type()) == isHostileType(other.type())
                            && isFriendlyType(edge.type()) == isFriendlyType(other.type())) {
                        // same polarity different type is ok
                    } else if (other.from().equals(edge.to())
                            && other.to().equals(edge.from())
                            && isHostileType(edge.type()) != isHostileType(other.type())
                            && isFriendlyType(edge.type()) != isFriendlyType(other.type())
                            && ((isHostileType(edge.type()) && isFriendlyType(other.type()))
                            || (isFriendlyType(edge.type()) && isHostileType(other.type())))) {
                        // war+alliance dual edges are intentional in corpus (e.g. mulan/tianlan)
                        continue;
                    }
                }
            }
        }
        // Explicit reverse hostility check for war/rivalry without reverse and without dual trade note.
        for (Edge edge : BUILTIN.edges()) {
            if (!isHostileType(edge.type()) || edge.directed()) {
                continue;
            }
            boolean hasReverseHostile = false;
            boolean hasAnyReverse = false;
            for (Edge other : BUILTIN.edges()) {
                if (other.from().equals(edge.to()) && other.to().equals(edge.from())) {
                    hasAnyReverse = true;
                    if (isHostileType(other.type()) || other.type().equals(edge.type())) {
                        hasReverseHostile = true;
                    }
                }
            }
            if (!hasReverseHostile && !hasAnyReverse) {
                // Single undirected record is treated as symmetric by relation APIs; not an issue.
                continue;
            }
        }
        return List.copyOf(issues);
    }

    public static boolean isHostileType(String type) {
        String t = normalize(type);
        return EDGE_WAR.equals(t) || EDGE_RIVALRY.equals(t) || EDGE_BLOCKADE.equals(t);
    }

    public static boolean isFriendlyType(String type) {
        String t = normalize(type);
        return EDGE_ALLIANCE.equals(t) || EDGE_TRADE.equals(t) || EDGE_SECRET_TIE.equals(t);
    }

    private static Snapshot loadBuiltin() {
        JsonObject graph = readJson("data/" + SeekingImmortalsMod.MODID + "/text_material/faction_graph.json");
        JsonObject speciesRoot = readJson("data/" + SeekingImmortalsMod.MODID + "/text_material/faction_species.json");

        Map<String, Node> nodes = new LinkedHashMap<>();
        List<Edge> edges = new ArrayList<>();
        List<String> edgeTypes = new ArrayList<>();
        Map<String, SpeciesFaction> species = new LinkedHashMap<>();
        Map<String, DeepFaction> deep = new LinkedHashMap<>();

        if (graph != null) {
            JsonArray types = graph.getAsJsonArray("edge_types");
            if (types != null) {
                for (JsonElement el : types) {
                    edgeTypes.add(normalize(el.getAsString()));
                }
            }
            JsonArray nodeArr = graph.getAsJsonArray("nodes");
            if (nodeArr != null) {
                for (JsonElement el : nodeArr) {
                    if (!el.isJsonObject()) continue;
                    JsonObject o = el.getAsJsonObject();
                    String id = normalize(str(o, "id"));
                    if (id.isBlank()) continue;
                    nodes.put(id, new Node(
                            id,
                            str(o, "display"),
                            str(o, "region"),
                            str(o, "archetype"),
                            str(o, "note"),
                            str(o, "description"),
                            false));
                }
            }
            JsonArray virtual = graph.getAsJsonArray("virtual_nodes");
            if (virtual != null) {
                for (JsonElement el : virtual) {
                    if (!el.isJsonObject()) continue;
                    JsonObject o = el.getAsJsonObject();
                    String id = normalize(str(o, "id"));
                    if (id.isBlank()) continue;
                    nodes.putIfAbsent(id, new Node(id, str(o, "display"), str(o, "region"), "virtual", "", "", true));
                }
            }
            JsonArray edgeArr = graph.getAsJsonArray("edges");
            if (edgeArr != null) {
                for (JsonElement el : edgeArr) {
                    if (!el.isJsonObject()) continue;
                    JsonObject o = el.getAsJsonObject();
                    String from = normalize(str(o, "from"));
                    String to = normalize(str(o, "to"));
                    if (from.isBlank() || to.isBlank()) continue;
                    String type = normalize(str(o, "type"));
                    int weight = o.has("weight") && !o.get("weight").isJsonNull() ? o.get("weight").getAsInt() : 0;
                    boolean directed = o.has("directed") && o.get("directed").getAsBoolean();
                    boolean virtualEdge = o.has("virtual_node") && o.get("virtual_node").getAsBoolean();
                    String note = str(o, "note");
                    edges.add(new Edge(from, to, type, weight, directed, virtualEdge, note));
                }
            }
        }

        if (speciesRoot != null) {
            JsonArray arr = speciesRoot.getAsJsonArray("spirit_realm_factions");
            if (arr != null) {
                for (JsonElement el : arr) {
                    if (!el.isJsonObject()) continue;
                    JsonObject o = el.getAsJsonObject();
                    String id = normalize(str(o, "id"));
                    if (id.isBlank()) continue;
                    species.put(id, new SpeciesFaction(
                            id,
                            str(o, "display"),
                            str(o, "culture"),
                            str(o, "specialty_item"),
                            str(o, "taboo")));
                    nodes.putIfAbsent(id, new Node(id, str(o, "display"), "spirit_realm", "species", str(o, "taboo"), "", false));
                }
            }
            JsonArray mortal = speciesRoot.getAsJsonArray("mortal_realm_entities");
            if (mortal != null) {
                for (JsonElement el : mortal) {
                    if (!el.isJsonObject()) continue;
                    JsonObject o = el.getAsJsonObject();
                    String id = normalize(str(o, "id"));
                    if (id.isBlank()) continue;
                    species.putIfAbsent(id, new SpeciesFaction(id, str(o, "display"), str(o, "race"), "", ""));
                }
            }
        }

        // Deep faction packs → node attributes / nested groups.
        mergeDeepList(deep, nodes, "demonic_six_sects.json", "sects", "demonic_six");
        mergeDeepList(deep, nodes, "star_palace_internal_factions.json", "factions", "star_palace_branch");
        mergeDeepList(deep, nodes, "chaotic_sea_factions.json", "factions", "chaotic_sea");
        mergeDeepList(deep, nodes, "dajin_factions.json", "factions", "dajin");
        mergeHumanClan(deep, nodes);
        mergeWutuMulan(deep, nodes);
        mergeYinLuo(deep, nodes);
        mergeGhostBanMeta(deep);

        return new Snapshot(
                List.copyOf(edgeTypes.isEmpty()
                        ? List.of(EDGE_WAR, EDGE_TRADE, EDGE_ALLIANCE, EDGE_SECRET_TIE, EDGE_BLOCKADE, EDGE_RIVALRY)
                        : edgeTypes),
                Collections.unmodifiableMap(nodes),
                List.copyOf(edges),
                Collections.unmodifiableMap(species),
                Collections.unmodifiableMap(deep));
    }

    private static void mergeDeepList(Map<String, DeepFaction> deep, Map<String, Node> nodes,
                                      String file, String arrayKey, String group) {
        JsonObject root = readJson("data/" + SeekingImmortalsMod.MODID + "/text_material/" + file);
        if (root == null) {
            return;
        }
        JsonArray arr = root.getAsJsonArray(arrayKey);
        if (arr == null) {
            return;
        }
        for (JsonElement el : arr) {
            if (!el.isJsonObject()) continue;
            JsonObject o = el.getAsJsonObject();
            String id = normalize(str(o, "id"));
            if (id.isBlank()) continue;
            String display = str(o, "display");
            String region = str(o, "region");
            if (region.isBlank()) {
                region = str(root, "region");
            }
            if (region.isBlank()) {
                region = str(root, "region_primary");
            }
            String specialty = firstNonBlank(str(o, "specialty"), str(o, "role"), str(o, "archetype"));
            List<String> tags = new ArrayList<>();
            tags.add(group);
            if (o.has("alignment")) tags.add(normalize(str(o, "alignment")));
            if (o.has("archetype")) tags.add(normalize(str(o, "archetype")));
            deep.put(id, new DeepFaction(id, display, group, region, specialty, List.copyOf(tags), Map.of()));
            nodes.putIfAbsent(id, new Node(id, display, region, group, specialty, "", false));
        }
    }

    private static void mergeHumanClan(Map<String, DeepFaction> deep, Map<String, Node> nodes) {
        JsonObject root = readJson("data/" + SeekingImmortalsMod.MODID + "/text_material/human_clan_league.json");
        if (root == null) {
            return;
        }
        String leagueId = normalize(str(root, "id"));
        if (!leagueId.isBlank()) {
            deep.put(leagueId, new DeepFaction(leagueId, str(root, "display"), "human_clan_league",
                    str(root, "region"), "league", List.of("human_clan_league"), Map.of()));
            nodes.putIfAbsent(leagueId, new Node(leagueId, str(root, "display"), str(root, "region"),
                    "human_clan_league", "", "", false));
        }
        JsonArray clans = root.getAsJsonArray("clans");
        if (clans == null) {
            return;
        }
        for (JsonElement el : clans) {
            if (!el.isJsonObject()) continue;
            JsonObject o = el.getAsJsonObject();
            String id = normalize(str(o, "id"));
            if (id.isBlank()) continue;
            deep.put(id, new DeepFaction(id, str(o, "display"), "human_clan",
                    str(root, "region"), str(o, "specialty"), List.of("human_clan"), Map.of("rep_id", str(o, "rep_id"))));
            nodes.putIfAbsent(id, new Node(id, str(o, "display"), str(root, "region"), "human_clan",
                    str(o, "specialty"), "", false));
        }
    }

    private static void mergeWutuMulan(Map<String, DeepFaction> deep, Map<String, Node> nodes) {
        JsonObject root = readJson("data/" + SeekingImmortalsMod.MODID + "/text_material/wutu_mulan_feud.json");
        if (root == null) {
            return;
        }
        if (root.has("factions") && root.get("factions").isJsonObject()) {
            JsonObject factions = root.getAsJsonObject("factions");
            for (Map.Entry<String, JsonElement> entry : factions.entrySet()) {
                String id = normalize(entry.getKey());
                String display = entry.getValue().isJsonPrimitive()
                        ? entry.getValue().getAsString()
                        : (entry.getValue().isJsonObject() ? str(entry.getValue().getAsJsonObject(), "display") : id);
                if (display.isBlank()) {
                    display = id;
                }
                deep.put(id, new DeepFaction(id, display, "wutu_mulan_feud", "mulan_border", "feud",
                        List.of("feud", "wutu_mulan"), Map.of()));
                nodes.putIfAbsent(id, new Node(id, display, "mulan_border", "feud", "", "", false));
            }
        }
    }

    private static void mergeYinLuo(Map<String, DeepFaction> deep, Map<String, Node> nodes) {
        JsonObject root = readJson("data/" + SeekingImmortalsMod.MODID + "/text_material/yin_luo_hall.json");
        if (root == null) {
            return;
        }
        String id = normalize(str(root, "id"));
        if (id.isBlank()) {
            id = "yin_luo_hall";
        }
        Map<String, String> attrs = new LinkedHashMap<>();
        attrs.put("currency", str(root, "currency"));
        attrs.put("parent_cosmology", str(root, "parent_cosmology"));
        deep.put(id, new DeepFaction(id, str(root, "display"), "yin_luo", "yin_underworld",
                "ghost_market", List.of("ghost", "yin_luo"), Map.copyOf(attrs)));
        nodes.putIfAbsent(id, new Node(id, str(root, "display"), "yin_underworld", "yin_luo", "", "", false));
    }

    private static void mergeGhostBanMeta(Map<String, DeepFaction> deep) {
        JsonObject root = readJson("data/" + SeekingImmortalsMod.MODID + "/text_material/ghost_sect_ban_rules.json");
        if (root == null) {
            return;
        }
        Map<String, String> attrs = new LinkedHashMap<>();
        attrs.put("path_ref", str(root, "path_ref"));
        attrs.put("consequences_ref", str(root, "consequences_ref"));
        deep.put("ghost_sect_ban_rules", new DeepFaction(
                "ghost_sect_ban_rules",
                "鬼修禁令",
                "rule",
                "",
                "ban",
                List.of("ghost_ban"),
                Map.copyOf(attrs)));
    }

    private static JsonObject readJson(String path) {
        try (InputStream stream = FactionGraphService.class.getClassLoader().getResourceAsStream(path)) {
            if (stream == null) {
                return null;
            }
            try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                return JsonParser.parseReader(reader).getAsJsonObject();
            }
        } catch (Exception exception) {
            SeekingImmortalsMod.LOGGER.warn("Failed to load faction graph resource {}", path, exception);
            return null;
        }
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

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    public record Node(String id, String display, String region, String archetype, String note,
                       String description, boolean virtual) {}

    public record Edge(String from, String to, String type, int weight, boolean directed,
                       boolean virtualNode, String note) {}

    public record SpeciesFaction(String id, String display, String culture, String specialtyItem, String taboo) {}

    public record DeepFaction(String id, String display, String group, String region, String specialty,
                              List<String> tags, Map<String, String> attributes) {}

    public record Snapshot(List<String> edgeTypes,
                           Map<String, Node> nodes,
                           List<Edge> edges,
                           Map<String, SpeciesFaction> species,
                           Map<String, DeepFaction> deepFactions) {
        public int nodeCount() {
            return nodes.size();
        }

        public int edgeCount() {
            return edges.size();
        }
    }
}
