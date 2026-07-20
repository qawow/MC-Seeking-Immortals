package com.xunxian.seekingimmortals.craft;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PuppetCraftServiceTest {
    @Test
    void blueprintAndSkillGatesPrecedeMaterialCommit() {
        assertEquals("basic_wood_puppet_blueprint",
                PuppetCraftService.blueprintIdForRecipe("assemble_basic_wood").orElseThrow());
        assertEquals("giant_ape_puppet_blueprint",
                PuppetCraftService.blueprintIdForRecipe("assemble_giant_ape").orElseThrow());
        assertTrue(PuppetCraftService.blueprintIdForRecipe("assemble_giant_turtle").isEmpty());
        assertEquals("message.seeking_immortals.puppet_assembly_bench.skill_locked",
                PuppetCraftService.preflightFailure(false, false, false, false));
        assertEquals("message.seeking_immortals.puppet_assembly_bench.missing_blueprint",
                PuppetCraftService.preflightFailure(false, true, false, true));
        assertEquals("message.seeking_immortals.puppet_assembly_bench.missing_materials",
                PuppetCraftService.preflightFailure(false, true, true, false));
        assertEquals("", PuppetCraftService.preflightFailure(false, true, true, true));
    }

    @Test
    void successfulSpawnRecordsAssemblyGrowthAfterEntityAcceptance() throws Exception {
        String source = Files.readString(Path.of("src", "main", "java", "com", "xunxian",
                "seekingimmortals", "craft", "PuppetCraftService.java"));
        int spawn = source.indexOf("boolean spawned = SummonHonestMvpService.spawnConfigured");
        int failure = source.indexOf("if (!spawned)", spawn);
        int growth = source.indexOf("PuppetGrowthService", failure);
        assertTrue(spawn >= 0 && failure > spawn && growth > failure,
                "assembly growth must be committed only after the entity spawn succeeds");
    }
}
