package com.xunxian.seekingimmortals.structure;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 0.2.108 contracts: station catalog ids must not be confused with validator names or item ids.
 */
class StationIdContractTest {

    @Test
    void refinementForgeG1IsCatalogIdNotBareRefinementForge() {
        assertTrue(MultiblockStructureCatalog.builtin().find("refinement_forge_g1").isPresent());
        assertTrue(MultiblockStructureCatalog.builtin().find("refinement_forge").isEmpty());
        assertEquals("refinement_forge",
                MultiblockStructureCatalog.builtin().find("refinement_forge_g1").orElseThrow().pattern().validator());
    }

    @Test
    void highTierRefinementValidatorsExistThroughG6() {
        for (String id : java.util.List.of(
                "refinement_forge_g1", "refinement_forge_g2", "refinement_forge_g3",
                "refinement_forge_g4", "refinement_forge_g5", "refinement_forge_g6")) {
            Optional<MultiblockStructureCatalog.StructureEntry> entry =
                    MultiblockStructureCatalog.builtin().find(id);
            assertTrue(entry.isPresent(), "missing station " + id);
            assertFalse(entry.get().pattern().validator().isBlank(), id + " needs validator");
        }
    }

    @Test
    void teleportStationsUseCatalogIdsNotValidatorNames() {
        assertTrue(MultiblockStructureCatalog.builtin().find("teleport_array_pedestal").isPresent());
        assertTrue(MultiblockStructureCatalog.builtin().find("immortal_teleport_grand_array").isPresent());
        assertTrue(MultiblockStructureCatalog.builtin().find("fixed_teleport_array").isEmpty());
        assertTrue(MultiblockStructureCatalog.builtin().find("long_range_teleport_array").isEmpty());
        assertEquals("fixed_teleport_array",
                MultiblockStructureCatalog.builtin().find("teleport_array_pedestal").orElseThrow().pattern().validator());
        assertEquals("long_range_teleport_array",
                MultiblockStructureCatalog.builtin().find("immortal_teleport_grand_array").orElseThrow().pattern().validator());
    }

    @Test
    void flyingBoatDockUsesSpecializedValidator() {
        MultiblockStructureCatalog.StructureEntry dock =
                MultiblockStructureCatalog.builtin().find("flying_boat_dock").orElseThrow();
        assertEquals("flying_boat_dock", dock.pattern().validator());
        assertTrue(FlyingBoatDockStructure.platformOffsets().size() >= 9);
    }

    @Test
    void commissionAndSoftCraftSourcesUseCatalogStationIds() throws Exception {
        String g1 = Files.readString(Path.of(
                "src", "main", "java", "com", "xunxian", "seekingimmortals",
                "block", "RefinementForgeBlock.java"));
        assertTrue(g1.contains("ensureCommissioned(serverPlayer, \"refinement_forge_g1\""));
        assertFalse(g1.contains("ensureCommissioned(serverPlayer, \"refinement_forge\""));

        String g3 = Files.readString(Path.of(
                "src", "main", "java", "com", "xunxian", "seekingimmortals",
                "block", "RefinementForgeG3Block.java"));
        assertTrue(g3.contains("\"refinement_forge_g3\""));
        assertFalse(g3.contains("\"refinement_forge\", pos"));

        String high = Files.readString(Path.of(
                "src", "main", "java", "com", "xunxian", "seekingimmortals",
                "block", "RefinementForgeHighBlock.java"));
        assertFalse(high.contains("\"refinement_forge\", pos"));

        String soft = Files.readString(Path.of(
                "src", "main", "java", "com", "xunxian", "seekingimmortals",
                "catalog", "CraftWorldSoftService.java"));
        assertTrue(soft.contains("refinement_forge_g4"));
        assertTrue(soft.contains("refinement_forge_g6"));
        assertFalse(soft.contains("\"refinement_forge\", \"refinement_forge_g1\""));

        String dialogue = Files.readString(Path.of(
                "src", "main", "java", "com", "xunxian", "seekingimmortals",
                "npc", "DialogueBranchService.java"));
        assertTrue(dialogue.contains("teleport_array_pedestal"));
        assertTrue(dialogue.contains("immortal_teleport_grand_array"));
        assertFalse(dialogue.contains("isStationFormed(player.level(), \"fixed_teleport_array\""));
        assertFalse(dialogue.contains("isStationFormed(player.level(), \"long_range_teleport_array\""));

        String station = Files.readString(Path.of(
                "src", "main", "java", "com", "xunxian", "seekingimmortals",
                "structure", "MultiblockStationService.java"));
        assertTrue(station.contains("case \"flying_boat_dock\""));
        assertTrue(station.contains("FlyingBoatDockStructure.validate"));
    }
}
