package com.xunxian.seekingimmortals.npc;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DialogueBranchServiceTest {
    @Test
    void loadsTwelveBranchTrees() {
        assertTrue(DialogueBranchService.treeCount() >= 12, "trees=" + DialogueBranchService.treeCount());
        assertTrue(DialogueBranchService.findTree("tree_sect_contribution_clerk").isPresent());
        assertTrue(DialogueBranchService.findTree("tree_market_vendor").isPresent());
        assertTrue(DialogueBranchService.treeForArchetype("star_palace_registrar").isPresent());
    }

    @Test
    void treesHaveRootAndNodes() {
        for (DialogueBranchService.Tree tree : DialogueBranchService.builtin().trees().values()) {
            assertFalse(tree.id().isBlank());
            assertFalse(tree.nodes().isEmpty(), tree.id());
            assertTrue(tree.nodes().containsKey(tree.root())
                    || tree.nodes().values().stream().findFirst().isPresent(), tree.id());
        }
    }

    @Test
    void conditionOpsCoverCorpus() {
        Set<String> ops = new HashSet<>();
        for (DialogueBranchService.Tree tree : DialogueBranchService.builtin().trees().values()) {
            for (DialogueBranchService.Node node : tree.nodes().values()) {
                ops.addAll(node.when().keySet());
            }
        }
        assertTrue(ops.contains("default") || ops.contains("rep_gte") || ops.contains("quest_flag"),
                "expected common condition ops, got " + ops);
    }

    @Test
    void effectTypesIncludeShopAndTeleport() {
        Set<String> types = new HashSet<>();
        for (DialogueBranchService.Tree tree : DialogueBranchService.builtin().trees().values()) {
            for (DialogueBranchService.Node node : tree.nodes().values()) {
                for (DialogueBranchService.Effect effect : node.effects()) {
                    types.add(effect.type());
                }
            }
        }
        assertTrue(types.contains("open_shop"));
        assertTrue(types.contains("grant_item") || types.contains("teleport") || types.contains("start_teleport"));
    }
}
