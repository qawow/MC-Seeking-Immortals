package com.xunxian.seekingimmortals.skill;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.xunxian.seekingimmortals.SeekingImmortalsMod;
import com.xunxian.seekingimmortals.catalog.ManualCatalogService;
import com.xunxian.seekingimmortals.cultivation.PlayerCultivation;
import net.minecraft.server.level.ServerPlayer;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * M02: loads skill_trees.json (90 trees) and exposes method→spell/secret unlock queries
 * for LifeSkillService / SpecialSkillService aligned skill-tree screens and gates.
 */
public final class SkillTreeCatalogService {
    private static final Snapshot BUILTIN = loadBuiltin();

    private SkillTreeCatalogService() {}

    public record Tree(String id, String display, String methodRoot, String element,
                       List<String> spells, List<String> secrets, List<String> prerequisiteMethods) {
        public Tree {
            spells = spells == null ? List.of() : List.copyOf(spells);
            secrets = secrets == null ? List.of() : List.copyOf(secrets);
            prerequisiteMethods = prerequisiteMethods == null ? List.of() : List.copyOf(prerequisiteMethods);
        }
    }

    public record Snapshot(Map<String, Tree> trees, Map<String, String> techniqueToTree) {
        public Optional<Tree> find(String id) {
            if (id == null || id.isBlank()) {
                return Optional.empty();
            }
            Tree direct = trees.get(id);
            if (direct != null) {
                return Optional.of(direct);
            }
            return Optional.ofNullable(trees.get(id.trim().toLowerCase(Locale.ROOT)));
        }
    }

    public static Snapshot builtin() {
        return BUILTIN;
    }

    public static int treeCount() {
        return BUILTIN.trees().size();
    }

    public static Optional<Tree> findByTechnique(String techniqueId) {
        if (techniqueId == null || techniqueId.isBlank()) {
            return Optional.empty();
        }
        String treeId = BUILTIN.techniqueToTree().get(techniqueId.trim().toLowerCase(Locale.ROOT));
        return treeId == null ? Optional.empty() : BUILTIN.find(treeId);
    }

    /**
     * True when the player has learned the method_root (or any prerequisite is not required
     * because the tree has no method root).
     */
    public static boolean isTreeUnlocked(ServerPlayer player, Tree tree) {
        if (player == null || tree == null) {
            return false;
        }
        if (player.getAbilities().instabuild) {
            return true;
        }
        if (tree.methodRoot() == null || tree.methodRoot().isBlank()) {
            return true;
        }
        return ManualCatalogService.hasLearnedMethod(player, tree.methodRoot());
    }

    public static boolean isTechniqueUnlockedByTree(ServerPlayer player, String techniqueId) {
        Optional<Tree> tree = findByTechnique(techniqueId);
        return tree.isEmpty() || isTreeUnlocked(player, tree.get());
    }

    public static List<String> unlockedSpells(ServerPlayer player, PlayerCultivation cultivation) {
        List<String> out = new ArrayList<>();
        for (Tree tree : BUILTIN.trees().values()) {
            if (!isTreeUnlocked(player, tree)) {
                continue;
            }
            out.addAll(tree.spells());
            out.addAll(tree.secrets());
        }
        return List.copyOf(out);
    }

    private static Snapshot loadBuiltin() {
        Map<String, Tree> trees = new LinkedHashMap<>();
        Map<String, String> techniqueToTree = new LinkedHashMap<>();
        JsonObject root = readJson("data/" + SeekingImmortalsMod.MODID + "/text_material/skill_trees.json");
        if (root == null) {
            root = readJson("data/" + SeekingImmortalsMod.MODID + "/catalog/skill_trees_index.json");
        }
        if (root != null) {
            JsonArray array = root.has("trees") && root.get("trees").isJsonArray()
                    ? root.getAsJsonArray("trees") : new JsonArray();
            for (JsonElement element : array) {
                if (!element.isJsonObject()) {
                    continue;
                }
                JsonObject o = element.getAsJsonObject();
                String id = first(o, "tree_id", "id");
                if (id.isBlank()) {
                    continue;
                }
                List<String> spells = stringList(o.get("spells"));
                List<String> secrets = stringList(o.get("secrets"));
                Tree tree = new Tree(id, first(o, "display", "name"),
                        first(o, "method_root", "method"),
                        first(o, "element", "layer"),
                        spells, secrets, stringList(o.get("prerequisite_methods")));
                trees.put(id, tree);
                trees.putIfAbsent(id.toLowerCase(Locale.ROOT), tree);
                for (String spell : spells) {
                    techniqueToTree.putIfAbsent(spell.toLowerCase(Locale.ROOT), id);
                }
                for (String secret : secrets) {
                    techniqueToTree.putIfAbsent(secret.toLowerCase(Locale.ROOT), id);
                }
            }
        }
        return new Snapshot(Collections.unmodifiableMap(trees), Collections.unmodifiableMap(techniqueToTree));
    }

    private static String first(JsonObject object, String... keys) {
        for (String key : keys) {
            if (object.has(key) && object.get(key).isJsonPrimitive()) {
                String value = object.get(key).getAsString();
                if (value != null && !value.isBlank()) {
                    return value;
                }
            }
        }
        return "";
    }

    private static List<String> stringList(JsonElement element) {
        if (element == null || !element.isJsonArray()) {
            return List.of();
        }
        List<String> out = new ArrayList<>();
        for (JsonElement child : element.getAsJsonArray()) {
            if (child != null && child.isJsonPrimitive()) {
                String value = child.getAsString();
                if (value != null && !value.isBlank()) {
                    out.add(value);
                }
            }
        }
        return out;
    }

    private static JsonObject readJson(String path) {
        ClassLoader loader = SkillTreeCatalogService.class.getClassLoader();
        try (InputStream stream = loader.getResourceAsStream(path)) {
            if (stream == null) {
                return null;
            }
            try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                JsonElement element = JsonParser.parseReader(reader);
                return element != null && element.isJsonObject() ? element.getAsJsonObject() : null;
            }
        } catch (Exception exception) {
            SeekingImmortalsMod.LOGGER.warn("Failed to load skill trees from {}", path, exception);
            return null;
        }
    }
}
