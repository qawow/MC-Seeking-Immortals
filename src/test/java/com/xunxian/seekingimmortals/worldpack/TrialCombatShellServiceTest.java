package com.xunxian.seekingimmortals.worldpack;

import com.xunxian.seekingimmortals.entity.SummonedServitorEntity;
import org.junit.jupiter.api.Test;

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
}
