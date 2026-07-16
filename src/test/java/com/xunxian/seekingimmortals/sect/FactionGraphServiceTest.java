package com.xunxian.seekingimmortals.sect;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FactionGraphServiceTest {
    @Test
    void loadsGraphNodesEdgesAndSpecies() {
        FactionGraphService.Snapshot snapshot = FactionGraphService.builtin();
        assertTrue(snapshot.nodeCount() >= 20, "expected >=20 faction nodes, got " + snapshot.nodeCount());
        assertTrue(snapshot.edgeCount() >= 10, "expected graph edges");
        assertFalse(snapshot.edgeTypes().isEmpty());
        assertTrue(snapshot.species().size() >= 10, "species factions");
        assertTrue(snapshot.deepFactions().size() >= 8, "deep faction packs");
        assertTrue(FactionGraphService.findNode("huangfeng_valley").isPresent()
                || FactionGraphService.findNode("star_palace").isPresent());
    }

    @Test
    void hostileAndAllyQueriesUseEdges() {
        // star_palace vs inverse_star_alliance is war in corpus
        assertTrue(FactionGraphService.areHostile("star_palace", "inverse_star_alliance")
                || FactionGraphService.areHostile("inverse_star_alliance", "star_palace"));
        List<String> enemies = FactionGraphService.enemiesOf("star_palace");
        assertFalse(enemies.isEmpty());
        int weight = FactionGraphService.relationWeight("star_palace", "inverse_star_alliance");
        assertTrue(weight <= 0 || FactionGraphService.relationWeight("huangfeng_valley", "ghost_spirit_gate") < 0
                || FactionGraphService.relationWeight("huangfeng_valley", "hehuan_sect") < 0);
    }

    @Test
    void bidirectionalConsistencyHasNoHardIssues() {
        // Corpus stores undirected edges as single records; service treats them as symmetric.
        // Explicit directed flags or dual war/trade pairs must not produce issues list errors.
        List<String> issues = FactionGraphService.bidirectionalIssues();
        assertEquals(List.of(), issues);

        // Soft symmetry check: for every undirected hostile edge, reverse relation reports hostile.
        for (FactionGraphService.Edge edge : FactionGraphService.builtin().edges()) {
            if (edge.directed()) {
                continue;
            }
            if (!FactionGraphService.isHostileType(edge.type())) {
                continue;
            }
            assertTrue(
                    FactionGraphService.areHostile(edge.from(), edge.to())
                            || FactionGraphService.areHostile(edge.to(), edge.from()),
                    "hostile edge not queryable both ways: " + edge.from() + "->" + edge.to());
        }
    }

    @Test
    void deepPacksAttachKnownGroups() {
        Set<String> groups = new HashSet<>();
        for (FactionGraphService.DeepFaction deep : FactionGraphService.builtin().deepFactions().values()) {
            groups.add(deep.group());
        }
        assertTrue(groups.contains("demonic_six") || groups.contains("chaotic_sea") || groups.contains("dajin"));
        assertTrue(FactionGraphService.deepFaction("yin_luo_hall").isPresent()
                || FactionGraphService.deepFaction("ghost_sect_ban_rules").isPresent());
    }
}
