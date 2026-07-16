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
    void idsAreUnique() {
        Set<String> ids = new HashSet<>();
        for (NamedNpcRegistry.NamedNpc npc : NamedNpcRegistry.all()) {
            assertTrue(ids.add(npc.id()), "duplicate npc id " + npc.id());
        }
    }
}
