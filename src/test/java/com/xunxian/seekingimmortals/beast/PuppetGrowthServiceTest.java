package com.xunxian.seekingimmortals.beast;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PuppetGrowthServiceTest {
    @Test
    void allPublishedCraftRecipesResolveToPuppetDefinitions() {
        assertEquals(7, PuppetGrowthService.recipePuppets().size());
        PuppetGrowthService.recipePuppets().forEach((recipe, puppet) ->
                assertTrue(PuppetDefinitionService.find(puppet).isPresent(), recipe + " -> " + puppet));
        assertEquals("basic_wood_puppet",
                PuppetGrowthService.puppetIdFromSummonId("puppet_assemble_basic_wood"));
        assertEquals("hunyuan_bowl_core_puppet",
                PuppetGrowthService.puppetIdFromSummonId("upgrade_hunyuan_core"));
        assertEquals("giant_ape_puppet",
                PuppetGrowthService.puppetIdFromSummonId("giant_ape_puppet"));
    }

    @Test
    void runtimeHooksPersistBothGrowthLoopsAndUseExistingStations() throws Exception {
        String beast = Files.readString(Path.of("src", "main", "java", "com", "xunxian",
                "seekingimmortals", "cultivation", "BeastContractService.java"));
        String summon = Files.readString(Path.of("src", "main", "java", "com", "xunxian",
                "seekingimmortals", "catalog", "SummonHonestMvpService.java"));
        String entity = Files.readString(Path.of("src", "main", "java", "com", "xunxian",
                "seekingimmortals", "entity", "SummonedServitorEntity.java"));
        assertTrue(beast.contains("GrowthExperience"));
        assertTrue(beast.contains("EvolutionStage"));
        assertTrue(beast.contains("spirit_beast_evolution_pool"));
        assertTrue(beast.contains("legacyProgress(entry.getInt(\"Growth\")"));
        assertTrue(beast.indexOf("boolean waitingAtThreshold")
                        < beast.indexOf("feedKind = consumeFeedItems(player)"),
                "a full-affinity threshold retry must fail before feed consumption");
        assertTrue(summon.contains("PuppetGrowthService.recordRepair"));
        assertTrue(summon.contains("puppet_core_forge"));
        assertTrue(summon.contains("PuppetGrowthService.statMultiplier(player, summonId)"));
        assertTrue(entity.contains("PuppetGrowthService.recordCombatCredit"));
    }
}
