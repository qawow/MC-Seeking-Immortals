package com.xunxian.seekingimmortals.worldpack;

import com.xunxian.seekingimmortals.entity.SummonedServitorEntity;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrialCombatShellServiceTest {
    @Test
    void archetypeMapsGhostRealms() {
        assertEquals(SummonedServitorEntity.Archetype.GHOST,
                TrialCombatShellService.archetypeFor("yin_ming_pocket"));
        assertEquals(SummonedServitorEntity.Archetype.GHOST,
                TrialCombatShellService.archetypeFor("void_palace"));
        assertEquals(SummonedServitorEntity.Archetype.GHOST,
                TrialCombatShellService.archetypeFor("nether_river"));
    }

    @Test
    void archetypeMapsPuppetRealms() {
        assertEquals(SummonedServitorEntity.Archetype.PUPPET,
                TrialCombatShellService.archetypeFor("thousand_bamboo_puppet_tower"));
        assertEquals(SummonedServitorEntity.Archetype.PUPPET,
                TrialCombatShellService.archetypeFor("kunwu_mountain"));
        assertEquals(SummonedServitorEntity.Archetype.PUPPET,
                TrialCombatShellService.archetypeFor("diyuan"));
    }

    @Test
    void archetypeMapsBeastRealms() {
        assertEquals(SummonedServitorEntity.Archetype.BEAST,
                TrialCombatShellService.archetypeFor("king_fox_mist"));
        assertEquals(SummonedServitorEntity.Archetype.BEAST,
                TrialCombatShellService.archetypeFor("fallen_demon_valley"));
        assertEquals(SummonedServitorEntity.Archetype.BEAST,
                TrialCombatShellService.archetypeFor("blood_forbidden"));
    }

    @Test
    void archetypeFallbackGeneric() {
        assertEquals(SummonedServitorEntity.Archetype.GENERIC,
                TrialCombatShellService.archetypeFor("mist_cave_trial"));
        assertEquals(SummonedServitorEntity.Archetype.GENERIC,
                TrialCombatShellService.archetypeFor(""));
        assertEquals(SummonedServitorEntity.Archetype.GENERIC,
                TrialCombatShellService.archetypeFor(null));
    }

    @Test
    void hostileShellDetectionRequiresInstance() {
        assertFalse(TrialCombatShellService.isHostileShell(null));
    }

    @Test
    void dedicatedBeastIdentityRequiresExplicitBestiaryOrBossEvidence() {
        assertEquals("red_leopard_beast",
                TrialCombatShellService.explicitBeastId("red_leopard_beast"));
        assertEquals("abyss_jiao",
                TrialCombatShellService.explicitBeastId("boss_abyss_jiao"));
        assertEquals("blood_jiao_guardian",
                TrialCombatShellService.explicitBeastId("boss_blood_jiao_guardian"));
        assertEquals("ice_fire_demon",
                TrialCombatShellService.explicitBeastId("ice_fire_demon"));

        assertFalse(TrialCombatShellService.isExplicitBeastId("guardian_blood_forbidden"));
        assertFalse(TrialCombatShellService.isExplicitBeastId("patrol_fallen_demon_valley"));
        assertFalse(TrialCombatShellService.isExplicitBeastId("kunwu_puppet_king"));
        assertFalse(TrialCombatShellService.isExplicitBeastId("stone_spirit_puppet"));
        assertFalse(TrialCombatShellService.isExplicitBeastId("ancient_demon_projection"));
        assertFalse(TrialCombatShellService.isExplicitBeastId("mulan_fashi_captain"));
        assertFalse(TrialCombatShellService.isExplicitBeastId("ghost_cultivator"));
        assertFalse(TrialCombatShellService.isExplicitBeastId("peak_trial_spirit"));
        assertFalse(TrialCombatShellService.isExplicitBeastId("void_palace_lord"));
    }

    @Test
    void dailyBeastEventsNeverFallbackToVanillaWolfOrFox() throws Exception {
        String source = Files.readString(Path.of(
                "src", "main", "java", "com", "xunxian", "seekingimmortals",
                "worldpack", "DailyEventEncounterService.java"));
        assertTrue(source.contains("BeastSpawnTableService.spawnNearPlayer(player, region, 3)"));
        assertFalse(source.contains("EntityType.WOLF"));
        assertFalse(source.contains("EntityType.FOX"));
    }

    @Test
    void bossCatalogSpawnIsGuardedByExplicitBeastIdentity() throws Exception {
        String source = Files.readString(Path.of(
                "src", "main", "java", "com", "xunxian", "seekingimmortals",
                "worldpack", "BossEncounterService.java"));
        int identityGuard = source.indexOf("TrialCombatShellService.isExplicitBeastId(id)");
        int catalogSpawn = source.indexOf("BeastBossService.spawnCatalogBoss(player, id)");
        assertTrue(identityGuard >= 0 && catalogSpawn > identityGuard,
                "dedicated catalog boss spawning must remain behind explicit beast identity");
    }
}
