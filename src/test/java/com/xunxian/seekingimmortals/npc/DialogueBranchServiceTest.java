package com.xunxian.seekingimmortals.npc;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DialogueBranchServiceTest {
    @Test
    void loadsRegionalBranchExpansion() {
        assertTrue(DialogueBranchService.treeCount() >= 24, "trees=" + DialogueBranchService.treeCount());
        assertEquals(12, DialogueBranchService.builtin().regionalProfiles().size());
        assertTrue(DialogueBranchService.findTree("tree_sect_contribution_clerk").isPresent());
        assertTrue(DialogueBranchService.findTree("tree_market_vendor").isPresent());
        assertTrue(DialogueBranchService.treeForArchetype("star_palace_registrar").isPresent());
        assertTrue(DialogueBranchService.findTree("tree_tiannan_steward").isPresent());
        assertTrue(DialogueBranchService.findTree("tree_underworld_merchant").isPresent());
    }

    @Test
    void regionalProfilesSelectMerchantAndStewardTreesFromData() {
        assertEquals("tree_tiannan_steward", DialogueBranchService.treeIdForNpcProfile(
                "npc_test", "tiannan", "outer_deacon", "sect_contribution_clerk"));
        assertEquals("tree_north_frontier_merchant", DialogueBranchService.treeIdForNpcProfile(
                "npc_test", "mulan", "quest_giver_main", "market_vendor"));
        assertEquals("tree_chaotic_sea_steward", DialogueBranchService.treeIdForNpcProfile(
                "npc_test", "chaotic_sea", "patrol_captain", "sect_contribution_clerk"));
        assertEquals("tree_dajin_merchant", DialogueBranchService.treeIdForNpcProfile(
                "npc_test", "great_jin_central", "black_market_contact", "market_vendor"));
        assertEquals("tree_spirit_realm_steward", DialogueBranchService.treeIdForNpcProfile(
                "npc_test", "tianyuan", "outer_deacon", "sect_contribution_clerk"));
        assertEquals("tree_underworld_merchant", DialogueBranchService.treeIdForNpcProfile(
                "npc_test", "nether_river", "quest_giver_main", "market_vendor"));
        assertEquals("", DialogueBranchService.treeIdForNpcProfile(
                "npc_test", "unknown", "outer_deacon", "sect_contribution_clerk"));
    }

    @Test
    void regionalProfileTreesAreClosedReachableGraphs() {
        for (DialogueBranchService.RegionalProfile profile
                : DialogueBranchService.builtin().regionalProfiles()) {
            DialogueBranchService.Tree tree = DialogueBranchService.findTree(profile.treeId()).orElseThrow();
            assertTrue(tree.nodes().containsKey(tree.root()), "missing root " + tree.id());
            Set<String> reached = new HashSet<>();
            ArrayDeque<String> queue = new ArrayDeque<>();
            queue.add(tree.root());
            while (!queue.isEmpty()) {
                String id = queue.removeFirst();
                if (!reached.add(id)) {
                    continue;
                }
                DialogueBranchService.Node node = tree.nodes().get(id);
                assertTrue(node != null, tree.id() + " missing node " + id);
                assertTrue(node.next().size() <= 8, tree.id() + ":" + id + " too many choices");
                for (String next : node.next()) {
                    assertTrue(tree.nodes().containsKey(next), tree.id() + " missing next " + next);
                    queue.add(next);
                }
            }
            assertEquals(tree.nodes().keySet(), reached, tree.id() + " has unreachable nodes");
        }
    }

    @Test
    void regionalResolutionIsResourceDrivenRatherThanJavaRegionSwitch() throws Exception {
        String source = Files.readString(Path.of("src", "main", "java", "com", "xunxian",
                "seekingimmortals", "npc", "DialogueBranchService.java"));
        String resource = Files.readString(Path.of("src", "main", "resources", "data",
                "seeking_immortals", "text_material", "npc_dialogue_branches_v139.json"));
        assertTrue(source.contains("regional_profiles"));
        assertTrue(source.contains("profile.matches(npcId, regionId, role, archetypeId)"));
        assertFalse(source.contains("case \"tiannan\""));
        assertTrue(resource.contains("\"profile_tiannan_steward\""));
        assertTrue(resource.contains("\"profile_underworld_merchant\""));
    }

    @Test
    void treesHaveRootAndNodes() {
        for (DialogueBranchService.Tree tree : DialogueBranchService.builtin().trees().values()) {
            assertFalse(tree.id().isBlank());
            assertFalse(tree.nodes().isEmpty(), tree.id());
            assertTrue(tree.nodes().containsKey(tree.root()), tree.id());
            Set<String> reached = new HashSet<>();
            ArrayDeque<String> queue = new ArrayDeque<>();
            queue.add(tree.root());
            while (!queue.isEmpty()) {
                DialogueBranchService.Node node = tree.nodes().get(queue.removeFirst());
                assertTrue(node != null, tree.id() + " has a missing node reference");
                if (!reached.add(node.id())) {
                    continue;
                }
                for (String next : node.next()) {
                    assertTrue(tree.nodes().containsKey(next), tree.id() + " missing next " + next);
                    queue.add(next);
                }
            }
            assertEquals(tree.nodes().keySet(), reached, tree.id() + " has unreachable nodes");
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
    void repairedTravelAndInstanceRoutesExposeAllRuntimeStates() {
        DialogueBranchService.Tree teleport = DialogueBranchService.findTree(
                "tree_teleport_array_keeper").orElseThrow();
        DialogueBranchService.Node greet = teleport.nodes().get("greet");
        assertTrue(greet.next().containsAll(List.of(
                "check_array", "check_array_damaged", "pay_and_go", "no_stones")));
        assertEquals(Map.of("window_open", false), DialogueBranchService.findTree(
                        "tree_secret_realm_guide").orElseThrow().nodes().get("no_window").when());
        assertTrue(DialogueBranchService.findTree("tree_tianyuan_registrar").orElseThrow()
                .nodes().get("portal_fee").next().contains("pay_portal_deny"));
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

    @Test
    void rootWithPlayerFacingLinesIsNotAutoSkipped() {
        DialogueBranchService.Node root = new DialogueBranchService.Node(
                "root", Map.of(), List.of("先听我说。"), List.of("next"), List.of());
        DialogueBranchService.Node next = new DialogueBranchService.Node(
                "next", Map.of("default", true), List.of("下一句"), List.of(), List.of());
        DialogueBranchService.Tree tree = new DialogueBranchService.Tree(
                "tree_test", "test", "root", List.of(), Map.of("root", root, "next", next));

        assertEquals("root", DialogueBranchService.resolveRoot(null, "", tree).orElseThrow().id());
    }

    @Test
    void directNextAllowlistUsesSoleDefaultOnlyAsFallback() {
        DialogueBranchService.Node root = new DialogueBranchService.Node(
                "root", Map.of(), List.of(), List.of("fallback", "allowed"), List.of());
        DialogueBranchService.Node fallback = new DialogueBranchService.Node(
                "fallback", Map.of("default", true), List.of("fallback"), List.of(), List.of());
        DialogueBranchService.Node allowed = new DialogueBranchService.Node(
                "allowed", Map.of(), List.of("allowed"), List.of(), List.of());
        DialogueBranchService.Node unrelated = new DialogueBranchService.Node(
                "unrelated", Map.of(), List.of("unrelated"), List.of(), List.of());
        DialogueBranchService.Tree tree = new DialogueBranchService.Tree(
                "tree_test", "test", "root", List.of(),
                Map.of("root", root, "fallback", fallback, "allowed", allowed, "unrelated", unrelated));

        assertEquals(List.of("allowed"), DialogueBranchService.availableNext(null, "", tree, root)
                .stream().map(DialogueBranchService.Node::id).toList());
        assertTrue(DialogueBranchService.isDirectNext(root, "allowed"));
        assertFalse(DialogueBranchService.isDirectNext(root, "unrelated"));
        assertTrue(DialogueBranchService.isDefaultWhen(Map.of("default", true)));
        assertFalse(DialogueBranchService.isDefaultWhen(Map.of("default", true, "realm_gte", "core")));
    }

    @Test
    void unknownAndHardConditionsFailClosedWithoutPlayer() {
        assertTrue(DialogueBranchService.matches(null, "", Map.of()));
        assertTrue(DialogueBranchService.matches(null, "", Map.of("default", true)));
        assertFalse(DialogueBranchService.matches(null, "", Map.of("default", false)));
        // Unknown ops must not open branches by accident.
        assertFalse(DialogueBranchService.matches(null, "", Map.of("future_op", "x")));
        // Missing inventory fails closed for possession checks.
        assertFalse(DialogueBranchService.matches(null, "", Map.of("has_item", "spirit_permit")));
        // equals:false inverts possession — no inventory means "does not have" succeeds.
        assertTrue(DialogueBranchService.matches(null, "", Map.of("has_item", "spirit_permit", "equals", false)));
        // Boolean has_token:false means "must not hold permit"; without player that is true.
        assertTrue(DialogueBranchService.matches(null, "", Map.of("has_token", false)));
        // Boolean has_token:true requires a permit.
        assertFalse(DialogueBranchService.matches(null, "", Map.of("has_token", true)));
        // need_permit:true requires a permit unless paired with has_token:false.
        assertFalse(DialogueBranchService.matches(null, "", Map.of("need_permit", true)));
        assertTrue(DialogueBranchService.matches(null, "", Map.of("need_permit", true, "has_token", false)));
        // array_state without nearby arrays resolves to disabled.
        assertTrue(DialogueBranchService.matches(null, "", Map.of("array_state", "disabled")));
        assertFalse(DialogueBranchService.matches(null, "", Map.of("array_state", "intact")));
        // window_open defaults closed without session flags/player world.
        assertFalse(DialogueBranchService.matches(null, "", Map.of("window_open", true)));
        assertTrue(DialogueBranchService.matches(null, "", Map.of("window_open", false)));
    }

    @Test
    void contributionConditionsHonorNumericThresholds() {
        assertFalse(DialogueBranchService.hasContributionAtLeast(499, 500));
        assertTrue(DialogueBranchService.hasContributionAtLeast(500, 500));
        assertTrue(DialogueBranchService.hasContributionAtLeast(501, 500));
        assertTrue(DialogueBranchService.hasContributionBelow(499, 500));
        assertFalse(DialogueBranchService.hasContributionBelow(500, 500));
        assertFalse(DialogueBranchService.hasContributionAtLeast(500, 499.9D));
        assertFalse(DialogueBranchService.hasContributionAtLeast(500, -1));
    }

    @Test
    void effectParameterListsPreserveArrayEntries() {
        DialogueBranchService.Effect effect = new DialogueBranchService.Effect(
                "offer_quest", Map.of("quest_ids", List.of("first", "second")));
        assertEquals(List.of("first", "second"), effect.paramList("quest_ids"));
    }
}
