package com.xunxian.seekingimmortals.npc;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

class NamedNpcRegistryTest {
    @Test
    void loadsNamedNpcsFromCorpus() {
        assertTrue(NamedNpcRegistry.count() >= 160, "expected >=160 named npcs, got " + NamedNpcRegistry.count());
        assertTrue(NamedNpcRegistry.find("npc_huangfeng_valley_sect_master").isPresent()
                || NamedNpcRegistry.find("npc_huangfeng_contribution").isPresent());
    }

    @Test
    void seedAndBindingNpcsResolveTrees() {
        // Bound contribution clerk must have a dialogue tree.
        var clerk = NamedNpcRegistry.find("npc_huangfeng_contribution");
        assertTrue(clerk.isPresent(), "seed/binding npc_huangfeng_contribution");
        assertFalse(clerk.get().dialogueTreeId().isBlank());
        assertTrue(DialogueBranchService.findTree(clerk.get().dialogueTreeId()).isPresent()
                || DialogueBranchService.treeForArchetype(clerk.get().archetype()).isPresent());
    }

    @Test
    void regionAndRoleQueriesWork() {
        List<NamedNpcRegistry.NamedNpc> tiannan = NamedNpcRegistry.byRegion("tiannan");
        assertFalse(tiannan.isEmpty());
        List<NamedNpcRegistry.NamedNpc> deacons = NamedNpcRegistry.byRole("outer_deacon");
        assertFalse(deacons.isEmpty());
    }

    @Test
    void hardBindingTreesResolve() {
        // Explicit template bindings / seed trees must all resolve (M12 acceptance).
        List<String> issues = NamedNpcRegistry.validateReferences().stream()
                .filter(s -> s.startsWith("tree_unresolved:"))
                .toList();
        assertEquals(List.of(), issues, "binding/seed trees must resolve: " + issues);
    }

    @Test
    void everyNamedNpcGetsAResolvableDialogueTree() {
        for (NamedNpcRegistry.NamedNpc npc : NamedNpcRegistry.all()) {
            assertFalse(npc.dialogueTreeId().isBlank(), "tree missing for " + npc.id());
            assertTrue(DialogueBranchService.findTree(npc.dialogueTreeId()).isPresent(),
                    "tree unresolved for " + npc.id() + " -> " + npc.dialogueTreeId());
        }
    }

    @Test
    void allPublishedNpcRegionTreeAndShopReferencesResolve() {
        assertEquals(List.of(), NamedNpcRegistry.validateReferences());
    }

    @Test
    void representativeRegionsUseTheirAuthoredProfiles() {
        assertEquals("tree_tiannan_steward", tree("npc_huangfeng_valley_outer_deacon"));
        assertEquals("tree_north_frontier_steward", tree("npc_tianmo_sect_outer_deacon"));
        assertEquals("tree_chaotic_sea_steward", tree("npc_star_palace_censor"));
        assertEquals("tree_dajin_merchant", tree("npc_dajin_auctioneer"));
        assertEquals("tree_spirit_realm_steward", tree("npc_tianyuan_clerk"));
        assertEquals("tree_underworld_merchant", tree("npc_ghost_spirit_ferry"));
        assertEquals("tree_sect_contribution_clerk", tree("npc_huangfeng_contribution"));
    }

    @Test
    void sectStewardsUseTheirOwnAuthoredContributionHalls() {
        assertEquals("yanyue_contribution_hall", NamedNpcRegistry.find(
                "npc_yanyue_sect_outer_deacon").orElseThrow().shopId());
        assertEquals("tianmo_sect_contribution_hall", NamedNpcRegistry.find(
                "npc_tianmo_sect_outer_deacon").orElseThrow().shopId());
        assertEquals("star_palace_merit_hall", NamedNpcRegistry.find(
                "npc_star_palace_sect_master").orElseThrow().shopId());
    }

    @Test
    void idsAreUnique() {
        Set<String> ids = new HashSet<>();
        for (NamedNpcRegistry.NamedNpc npc : NamedNpcRegistry.all()) {
            assertTrue(ids.add(npc.id()), "duplicate npc id " + npc.id());
        }
    }

    private static String tree(String npcId) {
        return NamedNpcRegistry.find(npcId).orElseThrow().dialogueTreeId();
    }
}
