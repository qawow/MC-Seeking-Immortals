package com.xunxian.seekingimmortals.quest;

import com.xunxian.seekingimmortals.catalog.ExtendedCatalogService;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Wave491/492: multi-node dialogue tables for text-quest chains.
 * Wave492: auto-covers all ExtendedCatalogService quest chains (62),
 * with richer templates for the original 10 demo lines.
 */
public final class TextQuestDialogueTreeService {
    private static final Map<String, List<Node>> TREES = buildAllTrees();

    private TextQuestDialogueTreeService() {}

    public record Node(String id, String textKey, List<Choice> choices) {}

    public record Choice(String id, String labelKey, String action) {}

    public static boolean hasTree(String chainId) {
        return TREES.containsKey(normalize(chainId));
    }

    public static int demoTreeCount() {
        return TREES.size();
    }

    public static Optional<Node> nodeFor(String chainId, int stage, boolean complete) {
        List<Node> nodes = TREES.get(normalize(chainId));
        if (nodes == null || nodes.isEmpty()) {
            return Optional.empty();
        }
        if (complete) {
            return Optional.of(nodes.get(nodes.size() - 1));
        }
        if (stage <= 0) {
            return Optional.of(nodes.get(0));
        }
        int idx = Math.min(nodes.size() - 1, Math.max(0, stage));
        return Optional.of(nodes.get(idx));
    }

    public static List<String> demoChainIds() {
        return List.copyOf(TREES.keySet());
    }

    private static Map<String, List<Node>> buildAllTrees() {
        Map<String, List<Node>> map = new LinkedHashMap<>();
        // Seed richer templates for the first demo set.
        for (String id : List.of(
                "huangfeng_cultivation_path",
                "chaotic_sea_politics",
                "mulan_tianlan_war",
                "ghost_path",
                "high_realm_endgame",
                "spirit_realm_rise",
                "craft_master",
                "dajin_kunwu_line",
                "ancient_demon_line",
                "qixuan_mortal_path")) {
            map.put(id, tree(id, true));
        }
        // Wave492: generate trees for every catalog chain id.
        for (ExtendedCatalogService.QuestChain chain : ExtendedCatalogService.builtin().questChains().values()) {
            String id = normalize(chain.id());
            if (id.isBlank() || map.containsKey(id)) {
                continue;
            }
            boolean branch = !id.contains("bridge") && !id.contains("intro") && !id.contains("endgame");
            map.put(id, tree(id, branch));
        }
        return Map.copyOf(map);
    }

    private static List<Node> tree(String chainId, boolean hasBranch) {
        String prefix = chainId.replaceAll("[^a-z0-9_]", "_");
        List<Node> nodes = new ArrayList<>();
        nodes.add(new Node(prefix + "_start",
                "message.seeking_immortals.text_quest.dialogue.node.start",
                List.of(
                        new Choice("start", "message.seeking_immortals.text_quest.dialogue.choice.start", "start"),
                        new Choice("talk", "message.seeking_immortals.text_quest.dialogue.choice.talk", "talk"))));
        nodes.add(new Node(prefix + "_mid1",
                "message.seeking_immortals.text_quest.dialogue.node.mid1",
                List.of(
                        new Choice("advance", "message.seeking_immortals.text_quest.dialogue.choice.advance", "advance"),
                        new Choice("talk", "message.seeking_immortals.text_quest.dialogue.choice.talk", "talk"))));
        nodes.add(new Node(prefix + "_mid2",
                "message.seeking_immortals.text_quest.dialogue.node.mid2",
                hasBranch
                        ? List.of(
                        new Choice("advance", "message.seeking_immortals.text_quest.dialogue.choice.advance", "advance"),
                        new Choice("righteous", "message.seeking_immortals.text_quest.dialogue.choice.righteous", "righteous"),
                        new Choice("demonic", "message.seeking_immortals.text_quest.dialogue.choice.demonic", "demonic"),
                        new Choice("neutral", "message.seeking_immortals.text_quest.dialogue.choice.neutral", "neutral"))
                        : List.of(
                        new Choice("advance", "message.seeking_immortals.text_quest.dialogue.choice.advance", "advance"))));
        nodes.add(new Node(prefix + "_finale",
                "message.seeking_immortals.text_quest.dialogue.node.finale",
                List.of(
                        new Choice("advance", "message.seeking_immortals.text_quest.dialogue.choice.advance", "advance"),
                        new Choice("talk", "message.seeking_immortals.text_quest.dialogue.choice.talk", "talk"))));
        nodes.add(new Node(prefix + "_complete",
                "message.seeking_immortals.text_quest.dialogue.node.complete",
                List.of(new Choice("talk", "message.seeking_immortals.text_quest.dialogue.choice.talk", "talk"))));
        return List.copyOf(nodes);
    }

    private static String normalize(String id) {
        return id == null ? "" : id.trim().toLowerCase(Locale.ROOT);
    }
}
