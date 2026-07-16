package com.xunxian.seekingimmortals.npc;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.xunxian.seekingimmortals.SeekingImmortalsMod;
import com.xunxian.seekingimmortals.catalog.ItemCatalogService;
import com.xunxian.seekingimmortals.cultivation.CultivationHelper;
import com.xunxian.seekingimmortals.cultivation.PlayerCultivation;
import com.xunxian.seekingimmortals.sect.ReputationUnlockService;
import com.xunxian.seekingimmortals.worldpack.ReputationService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * M12 data-driven dialogue branch trees ({@code npc_dialogue_branches_v139}).
 * Condition ops: rep_gte/rep_lt/rep_hostile/has_item/has_token/realm_gte/quest_flag/faction_member/array_state/default.
 */
public final class DialogueBranchService {
    private static final Snapshot BUILTIN = loadBuiltin();

    private DialogueBranchService() {}

    public static Snapshot builtin() {
        return BUILTIN;
    }

    public static Optional<Tree> findTree(String treeId) {
        return Optional.ofNullable(BUILTIN.trees().get(normalize(treeId)));
    }

    public static Optional<Tree> treeForArchetype(String archetypeId) {
        String id = normalize(archetypeId);
        if (id.isBlank()) {
            return Optional.empty();
        }
        for (Tree tree : BUILTIN.trees().values()) {
            if (id.equals(tree.archetype())) {
                return Optional.of(tree);
            }
        }
        return findTree("tree_" + id);
    }

    public static String treeIdForArchetype(String archetypeId) {
        return treeForArchetype(archetypeId).map(Tree::id).orElse("");
    }

    public static Optional<Tree> treeForNpc(String npcId) {
        String id = normalize(npcId);
        if (id.isBlank()) {
            return Optional.empty();
        }
        for (Tree tree : BUILTIN.trees().values()) {
            if (tree.npcIds().contains(id)) {
                return Optional.of(tree);
            }
        }
        return Optional.empty();
    }

    public static int treeCount() {
        return BUILTIN.trees().size();
    }

    public static Optional<Node> node(String treeId, String nodeId) {
        return findTree(treeId).map(tree -> tree.nodes().get(normalize(nodeId)));
    }

    /** Pick the first allowed next node, using a sole {@code default:true} node only as fallback. */
    public static Optional<Node> resolveNext(ServerPlayer player, String npcId, Tree tree, Node current) {
        List<Node> available = availableNext(player, npcId, tree, current);
        return available.isEmpty() ? Optional.empty() : Optional.of(available.get(0));
    }

    /** Returns only direct, currently eligible successors. Unrelated tree nodes are never exposed. */
    static List<Node> availableNext(ServerPlayer player, String npcId, Tree tree, Node current) {
        if (tree == null || current == null || current.next().isEmpty()) {
            return List.of();
        }
        List<Node> allowed = new ArrayList<>();
        Node fallback = null;
        for (String nextId : current.next()) {
            Node candidate = tree.nodes().get(normalize(nextId));
            if (candidate == null) {
                continue;
            }
            if (isDefaultWhen(candidate.when())) {
                if (fallback == null) {
                    fallback = candidate;
                }
            } else if (matches(player, npcId, candidate.when())) {
                allowed.add(candidate);
            }
        }
        if (allowed.isEmpty() && fallback != null) {
            allowed.add(fallback);
        }
        return List.copyOf(allowed);
    }

    static boolean isDirectNext(Node current, String nodeId) {
        if (current == null) {
            return false;
        }
        String target = normalize(nodeId);
        return !target.isBlank() && current.next().stream().anyMatch(id -> target.equals(normalize(id)));
    }

    public static Optional<Node> resolveRoot(ServerPlayer player, String npcId, Tree tree) {
        if (tree == null) {
            return Optional.empty();
        }
        Node root = tree.nodes().get(normalize(tree.root()));
        if (root == null) {
            return tree.nodes().values().stream().findFirst();
        }
        // Only line-less roots are routers. A root with dialogue must be shown before any transition.
        if (root.lines().isEmpty() && !root.next().isEmpty()) {
            Optional<Node> routed = resolveNext(player, npcId, tree, root);
            if (routed.isPresent()) {
                return routed;
            }
        }
        return Optional.of(root);
    }

    public static boolean matches(ServerPlayer player, String npcId, Map<String, Object> when) {
        if (when == null || when.isEmpty()) {
            return true;
        }
        if (when.size() == 1 && when.containsKey("default")) {
            return asBool(when.get("default"));
        }
        // equals:false inverts sibling has_item/has_token/has_stones checks for corpus forms like
        // {has_item:"token", equals:false}. equals alone is not a free-standing condition.
        boolean invertPossession = when.containsKey("equals") && !asBool(when.get("equals"));
        boolean anyHard = false;
        boolean allHard = true;
        for (Map.Entry<String, Object> entry : when.entrySet()) {
            String op = normalize(entry.getKey());
            if ("default".equals(op) || "equals".equals(op)) {
                continue;
            }
            anyHard = true;
            if (!matchOp(player, npcId, op, entry.getValue(), when, invertPossession)) {
                allHard = false;
                break;
            }
        }
        return !anyHard || allHard;
    }

    static boolean isDefaultWhen(Map<String, Object> when) {
        if (when == null || when.size() != 1) {
            return false;
        }
        return when.containsKey("default") && asBool(when.get("default")) && when.size() == 1;
    }

    private static boolean matchOp(ServerPlayer player, String npcId, String op, Object raw,
                                   Map<String, Object> when, boolean invertPossession) {
        return switch (op) {
            case "rep_gte" -> {
                RepThreshold thr = parseRepThreshold(raw);
                yield thr != null && reputation(player, thr.rep()) >= thr.n();
            }
            case "rep_lt" -> {
                RepThreshold thr = parseRepThreshold(raw);
                yield thr != null && reputation(player, thr.rep()) < thr.n();
            }
            case "rep_hostile" -> {
                String rep = parseRepKey(raw);
                int value = reputation(player, rep);
                yield value < 0 || value <= ReputationService.NEUTRAL_THRESHOLD - 10;
            }
            case "has_item", "has_token" -> {
                // Boolean form: has_token:false means "player must NOT hold a permit/token".
                // String form: has_item:"id" checks inventory, optionally inverted by equals:false.
                if (raw instanceof Boolean bool) {
                    boolean hasPermit = hasAnyPermit(player);
                    yield bool == hasPermit;
                }
                boolean has = hasItem(player, stringOf(raw));
                yield invertPossession ? !has : has;
            }
            case "has_stones" -> {
                boolean has = hasItem(player, "low_spirit_stone") || hasItem(player, "mid_spirit_stone")
                        || hasItem(player, "seeking_immortals:low_spirit_stone")
                        || hasItem(player, "seeking_immortals:mid_spirit_stone");
                if (raw instanceof Boolean bool) {
                    yield bool == has;
                }
                yield invertPossession ? !has : has;
            }
            case "has_contribution", "has_contribution_currency" -> contribution(player) > 0;
            case "has_contribution_lt" -> contribution(player) < asInt(raw, Integer.MAX_VALUE);
            case "realm_gte" -> CultivationHelper.meetsRealm(player, stripRealmQualifier(stringOf(raw)));
            case "realm_lt" -> !CultivationHelper.meetsRealm(player, stripRealmQualifier(stringOf(raw)));
            case "quest_flag", "or_quest" -> NpcDialogueFlags.hasFlag(player, stringOf(raw))
                    || NpcDialogueFlags.hasFlag(player, normalize(stringOf(raw)));
            case "not_flag" -> !NpcDialogueFlags.hasFlag(player, stringOf(raw));
            case "faction_member", "not_member" -> {
                boolean member = isFactionMember(player, npcId);
                yield "faction_member".equals(op) == member;
            }
            case "array_state" -> matchArrayState(player, stringOf(raw));
            case "window_open" -> {
                boolean expected = asBool(raw);
                yield expected == isAnySecretRealmWindowOpen(player);
            }
            case "need_permit" -> {
                // need_permit:true requires a permit item unless paired with has_token:false
                // (the no_permit branch), which is already handled by has_token.
                if (!asBool(raw)) {
                    yield true;
                }
                if (when != null && when.containsKey("has_token") && when.get("has_token") instanceof Boolean token
                        && !token) {
                    // Sibling has_token:false owns the inverted permit check.
                    yield true;
                }
                yield hasAnyPermit(player);
            }
            default -> false; // unknown ops fail closed
        };
    }

    private static boolean matchArrayState(ServerPlayer player, String expectedRaw) {
        String expected = normalize(expectedRaw);
        if (expected.isBlank()) {
            return false;
        }
        String actual = resolveNearbyArrayState(player);
        return expected.equals(actual);
    }

    /**
     * Resolve nearby fixed/long-range teleport array completeness.
     * intact = formed complete, damaged = partial structure present, disabled = nothing nearby.
     */
    static String resolveNearbyArrayState(ServerPlayer player) {
        if (player == null || player.level() == null) {
            return "disabled";
        }
        // Prefer explicit dialogue flags first so tests and quest scripts can drive states.
        if (NpcDialogueFlags.hasFlag(player, "array_state_intact")
                || NpcDialogueFlags.hasFlag(player, "array_intact")) {
            return "intact";
        }
        if (NpcDialogueFlags.hasFlag(player, "array_state_damaged")
                || NpcDialogueFlags.hasFlag(player, "array_damaged")) {
            return "damaged";
        }
        if (NpcDialogueFlags.hasFlag(player, "array_state_disabled")
                || NpcDialogueFlags.hasFlag(player, "array_disabled")) {
            return "disabled";
        }
        try {
            net.minecraft.core.BlockPos origin = player.blockPosition();
            boolean fixed = com.xunxian.seekingimmortals.structure.MultiblockStationService
                    .isStationFormed(player.level(), "fixed_teleport_array", origin)
                    || com.xunxian.seekingimmortals.structure.MultiblockStationService
                    .isStationFormed(player.level(), "long_range_teleport_array", origin);
            if (fixed) {
                return "intact";
            }
            // Soft proximity scan: any teleport pedestal/array block nearby counts as damaged
            // (present but not fully formed).
            for (int dx = -4; dx <= 4; dx++) {
                for (int dy = -2; dy <= 2; dy++) {
                    for (int dz = -4; dz <= 4; dz++) {
                        net.minecraft.world.level.block.state.BlockState state =
                                player.level().getBlockState(origin.offset(dx, dy, dz));
                        if (state.is(com.xunxian.seekingimmortals.registry.ModBlocks.TELEPORT_ARRAY_PEDESTAL.get())
                                || state.is(com.xunxian.seekingimmortals.registry.ModBlocks.LONG_RANGE_TELEPORT_ARRAY.get())
                                || state.is(com.xunxian.seekingimmortals.registry.ModBlocks.SPIRIT_GATHERING_ARRAY.get())) {
                            return "damaged";
                        }
                    }
                }
            }
        } catch (Throwable ignored) {
            // Unit tests / missing registries fall through to disabled.
        }
        return "disabled";
    }

    static boolean isAnySecretRealmWindowOpen(ServerPlayer player) {
        if (player == null) {
            return false;
        }
        if (NpcDialogueFlags.hasFlag(player, "window_open")
                || NpcDialogueFlags.hasFlag(player, "secret_realm_window_open")) {
            return true;
        }
        if (NpcDialogueFlags.hasFlag(player, "window_closed")
                || NpcDialogueFlags.hasFlag(player, "blood_forbidden_closed")) {
            return false;
        }
        try {
            for (com.xunxian.seekingimmortals.worldpack.SecretRealmCatalogService.RealmDef def
                    : com.xunxian.seekingimmortals.worldpack.SecretRealmCatalogService.snapshot().all()) {
                if (com.xunxian.seekingimmortals.worldpack.SecretRealmSessionService.isOpenWindow(player, def)) {
                    return true;
                }
            }
        } catch (Throwable ignored) {
            // Tests without server clock treat window as closed unless flagged open.
        }
        return false;
    }

    static boolean hasAnyPermit(ServerPlayer player) {
        if (player == null) {
            return false;
        }
        return hasItem(player, "sect_permit")
                || hasItem(player, "diyuan_permit")
                || hasItem(player, "chaotic_sea_teleport_permit")
                || hasItem(player, "seeking_immortals:sect_permit")
                || hasItem(player, "seeking_immortals:diyuan_permit")
                || hasItem(player, "seeking_immortals:chaotic_sea_teleport_permit")
                || NpcDialogueFlags.hasFlag(player, "has_teleport_permit")
                || NpcDialogueFlags.hasFlag(player, "teleport_permit");
    }

    private static boolean isFactionMember(ServerPlayer player, String npcId) {
        Optional<NamedNpcRegistry.NamedNpc> npc = NamedNpcRegistry.find(npcId);
        String sectId = npc.map(NamedNpcRegistry.NamedNpc::sectId).orElse("");
        if (sectId.isBlank()) {
            sectId = npc.map(NamedNpcRegistry.NamedNpc::factionId).orElse("");
        }
        if (sectId.isBlank()) {
            return false;
        }
        String finalSect = sectId;
        return CultivationHelper.get(player)
                .map(PlayerCultivation::getSevenMysteriesQuest)
                .map(progress -> {
                    String current = normalize(progress.getSectId());
                    return !current.isBlank() && (current.equals(normalize(finalSect))
                            || current.contains(normalize(finalSect))
                            || normalize(finalSect).contains(current));
                })
                .orElse(false);
    }

    private static int contribution(ServerPlayer player) {
        return CultivationHelper.get(player)
                .map(PlayerCultivation::getSevenMysteriesQuest)
                .map(progress -> progress.getContribution())
                .orElse(0);
    }

    private static int reputation(ServerPlayer player, String repKey) {
        String key = ReputationUnlockService.reputationKey(repKey);
        if (key == null || key.isBlank()) {
            key = normalize(repKey).replaceFirst("^rep_", "");
        }
        return ReputationService.get(player, key);
    }

    private static boolean hasItem(ServerPlayer player, String itemId) {
        String id = normalize(itemId);
        if (id.isBlank() || player == null) {
            return false;
        }
        Item item = ItemCatalogService.resolveCatalogItem(id);
        if (item == null) {
            // soft true for story tokens that are not registered yet, so trees remain navigable in tests/dev
            return false;
        }
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.is(item)) {
                return true;
            }
        }
        return false;
    }

    private static String stripRealmQualifier(String raw) {
        String value = normalize(raw);
        value = value.replace("_late", "").replace("_early", "").replace("_peak", "")
                .replace("_mid", "").replace("_plus", "");
        return value;
    }

    private static RepThreshold parseRepThreshold(Object raw) {
        if (raw instanceof Map<?, ?> map) {
            Object rep = map.get("rep");
            Object n = map.get("n");
            if (rep == null) {
                rep = map.get("faction");
            }
            return new RepThreshold(parseRepKey(rep), asInt(n, 0));
        }
        if (raw instanceof JsonObject obj) {
            String rep = str(obj, "rep", str(obj, "faction", ""));
            int n = obj.has("n") ? obj.get("n").getAsInt() : 0;
            return new RepThreshold(parseRepKey(rep), n);
        }
        String key = parseRepKey(raw);
        return key.isBlank() ? null : new RepThreshold(key, 0);
    }

    private static String parseRepKey(Object raw) {
        if (raw == null) {
            return "";
        }
        if (raw instanceof Map<?, ?> map) {
            Object rep = map.get("rep");
            if (rep == null) {
                rep = map.get("faction");
            }
            return parseRepKey(rep);
        }
        return normalize(String.valueOf(raw)).replaceFirst("^rep_", "");
    }

    private static Snapshot loadBuiltin() {
        Map<String, Tree> trees = new LinkedHashMap<>();
        JsonObject root = readJson("data/" + SeekingImmortalsMod.MODID + "/text_material/npc_dialogue_branches_v139.json");
        if (root != null && root.has("trees") && root.get("trees").isJsonArray()) {
            for (JsonElement element : root.getAsJsonArray("trees")) {
                if (!element.isJsonObject()) {
                    continue;
                }
                Tree tree = parseTree(element.getAsJsonObject());
                if (!tree.id().isBlank()) {
                    trees.put(tree.id(), tree);
                }
            }
        }
        return new Snapshot(Collections.unmodifiableMap(trees));
    }

    private static Tree parseTree(JsonObject object) {
        String id = normalize(str(object, "id", ""));
        String archetype = normalize(str(object, "archetype", ""));
        String root = normalize(str(object, "root", ""));
        List<String> npcIds = stringList(object.get("npc_ids"));
        Map<String, Node> nodes = new LinkedHashMap<>();
        if (object.has("nodes") && object.get("nodes").isJsonArray()) {
            for (JsonElement element : object.getAsJsonArray("nodes")) {
                if (!element.isJsonObject()) {
                    continue;
                }
                Node node = parseNode(element.getAsJsonObject());
                if (!node.id().isBlank()) {
                    nodes.put(node.id(), node);
                }
            }
        }
        return new Tree(id, archetype, root, npcIds, Collections.unmodifiableMap(nodes));
    }

    private static Node parseNode(JsonObject object) {
        String id = normalize(str(object, "id", ""));
        Map<String, Object> when = parseWhen(object.get("when"));
        List<String> lines = stringList(object.get("lines"));
        List<String> next = stringList(object.get("next"));
        List<Effect> effects = new ArrayList<>();
        if (object.has("effects") && object.get("effects").isJsonArray()) {
            for (JsonElement element : object.getAsJsonArray("effects")) {
                if (element != null && element.isJsonObject()) {
                    effects.add(parseEffect(element.getAsJsonObject()));
                } else if (element != null && element.isJsonPrimitive()) {
                    effects.add(new Effect(normalize(element.getAsString()), Map.of()));
                }
            }
        }
        return new Node(id, when, lines, next, List.copyOf(effects));
    }

    private static Map<String, Object> parseWhen(JsonElement element) {
        if (element == null || element.isJsonNull() || !element.isJsonObject()) {
            return Map.of();
        }
        JsonObject object = element.getAsJsonObject();
        Map<String, Object> map = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            map.put(normalize(entry.getKey()), jsonToJava(entry.getValue()));
        }
        return Map.copyOf(map);
    }

    private static Effect parseEffect(JsonObject object) {
        String type = normalize(str(object, "type", ""));
        Map<String, Object> params = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            if ("type".equals(normalize(entry.getKey()))) {
                continue;
            }
            params.put(normalize(entry.getKey()), jsonToJava(entry.getValue()));
        }
        return new Effect(type, Map.copyOf(params));
    }

    private static Object jsonToJava(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return null;
        }
        if (element.isJsonPrimitive()) {
            if (element.getAsJsonPrimitive().isBoolean()) {
                return element.getAsBoolean();
            }
            if (element.getAsJsonPrimitive().isNumber()) {
                return element.getAsInt();
            }
            return element.getAsString();
        }
        if (element.isJsonArray()) {
            List<Object> list = new ArrayList<>();
            for (JsonElement child : element.getAsJsonArray()) {
                list.add(jsonToJava(child));
            }
            return List.copyOf(list);
        }
        if (element.isJsonObject()) {
            Map<String, Object> map = new LinkedHashMap<>();
            for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
                map.put(entry.getKey(), jsonToJava(entry.getValue()));
            }
            return Map.copyOf(map);
        }
        return element.toString();
    }

    private static List<String> stringList(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return List.of();
        }
        List<String> list = new ArrayList<>();
        if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) {
                if (child != null && child.isJsonPrimitive()) {
                    String value = child.getAsString();
                    if (value != null && !value.isBlank()) {
                        list.add(value.trim());
                    }
                }
            }
        } else if (element.isJsonPrimitive()) {
            String value = element.getAsString();
            if (value != null && !value.isBlank()) {
                list.add(value.trim());
            }
        }
        return List.copyOf(list);
    }

    private static JsonObject readJson(String path) {
        try (InputStream stream = DialogueBranchService.class.getClassLoader().getResourceAsStream(path)) {
            if (stream == null) {
                return null;
            }
            try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                return JsonParser.parseReader(reader).getAsJsonObject();
            }
        } catch (Exception exception) {
            SeekingImmortalsMod.LOGGER.warn("Failed to load dialogue branches {}", path, exception);
            return null;
        }
    }

    private static String str(JsonObject object, String key, String fallback) {
        if (object == null || !object.has(key) || object.get(key).isJsonNull()) {
            return fallback;
        }
        try {
            return object.get(key).getAsString();
        } catch (Exception ex) {
            return String.valueOf(object.get(key));
        }
    }

    private static String stringOf(Object raw) {
        return raw == null ? "" : String.valueOf(raw);
    }

    private static boolean asBool(Object raw) {
        if (raw instanceof Boolean bool) {
            return bool;
        }
        if (raw == null) {
            return false;
        }
        String text = String.valueOf(raw).trim().toLowerCase(Locale.ROOT);
        return "true".equals(text) || "1".equals(text) || "yes".equals(text);
    }

    private static int asInt(Object raw, int fallback) {
        if (raw instanceof Number number) {
            return number.intValue();
        }
        if (raw == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(String.valueOf(raw).trim());
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private record RepThreshold(String rep, int n) {}

    public record Snapshot(Map<String, Tree> trees) {}

    public record Tree(String id, String archetype, String root, List<String> npcIds, Map<String, Node> nodes) {}

    public record Node(String id, Map<String, Object> when, List<String> lines, List<String> next, List<Effect> effects) {}

    public record Effect(String type, Map<String, Object> params) {
        public String param(String key) {
            Object value = params.get(normalize(key));
            return value == null ? "" : String.valueOf(value);
        }

        public int paramInt(String key, int fallback) {
            Object value = params.get(normalize(key));
            if (value instanceof Number number) {
                return number.intValue();
            }
            if (value == null) {
                return fallback;
            }
            try {
                return Integer.parseInt(String.valueOf(value).trim());
            } catch (NumberFormatException ex) {
                return fallback;
            }
        }
    }
}
