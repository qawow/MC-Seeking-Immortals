package com.xunxian.seekingimmortals.structure;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MultiblockOperationalServiceTest {
    @Test
    void efficiencyCurveMatchesOperationalStates() {
        assertEquals(1.0D, new MultiblockOperationalSavedData.StationState(
                "overworld", "alchemy_furnace_g1", 0L,
                MultiblockOperationalSavedData.OpState.INTACT, 100, 100).efficiency(), 1e-9);
        assertEquals(0.60D, new MultiblockOperationalSavedData.StationState(
                "overworld", "alchemy_furnace_g1", 0L,
                MultiblockOperationalSavedData.OpState.DAMAGED, 50, 100).efficiency(), 1e-9);
        assertEquals(0.20D, new MultiblockOperationalSavedData.StationState(
                "overworld", "alchemy_furnace_g1", 0L,
                MultiblockOperationalSavedData.OpState.CRITICAL, 10, 100).efficiency(), 1e-9);
        assertEquals(0.0D, new MultiblockOperationalSavedData.StationState(
                "overworld", "alchemy_furnace_g1", 0L,
                MultiblockOperationalSavedData.OpState.DISABLED, 0, 100).efficiency(), 1e-9);
    }

    @Test
    void repairCostsScaleWithDamageState() {
        MultiblockOperationalSavedData.StationState intact =
                new MultiblockOperationalSavedData.StationState("d", "s", 0L,
                        MultiblockOperationalSavedData.OpState.INTACT, 100, 100);
        MultiblockOperationalSavedData.StationState damaged =
                new MultiblockOperationalSavedData.StationState("d", "s", 0L,
                        MultiblockOperationalSavedData.OpState.DAMAGED, 50, 100);
        MultiblockOperationalSavedData.StationState critical =
                new MultiblockOperationalSavedData.StationState("d", "s", 0L,
                        MultiblockOperationalSavedData.OpState.CRITICAL, 10, 100);
        MultiblockOperationalSavedData.StationState disabled =
                new MultiblockOperationalSavedData.StationState("d", "s", 0L,
                        MultiblockOperationalSavedData.OpState.DISABLED, 0, 100);
        assertTrue(MultiblockOperationalService.repairCostShards(damaged)
                > MultiblockOperationalService.repairCostShards(intact));
        assertTrue(MultiblockOperationalService.repairCostShards(critical)
                > MultiblockOperationalService.repairCostShards(damaged));
        assertTrue(MultiblockOperationalService.repairCostShards(disabled)
                > MultiblockOperationalService.repairCostShards(critical));
    }

    @Test
    void repairReservesMaterialsBeforeCommitInSource() throws Exception {
        String source = Files.readString(Path.of(
                "src", "main", "java", "com", "xunxian", "seekingimmortals",
                "structure", "MultiblockOperationalService.java"));
        String compact = source.replaceAll("\\s+", "");
        assertTrue(compact.contains("tryReserveShards(player,cost)"));
        assertTrue(compact.contains("refundStacks(player,taken)"));
        int reserve = compact.indexOf("tryReserveShards(player,cost)");
        int apply = compact.indexOf("applyRepairStep(level,stationId,origin)");
        assertTrue(reserve >= 0 && apply > reserve,
                "repair must reserve shards before applying state changes");
        assertTrue(compact.contains("bestNearbyEfficiency"));
    }

    @Test
    void softCraftRequiresOperationalEfficiencyInSource() throws Exception {
        String source = Files.readString(Path.of(
                "src", "main", "java", "com", "xunxian", "seekingimmortals",
                "catalog", "CraftWorldSoftService.java"));
        String compact = source.replaceAll("\\s+", "");
        assertTrue(compact.contains("MultiblockOperationalService.bestNearbyEfficiency(player,stationIds)>0.0D")
                        || compact.contains("bestNearbyEfficiency(player, stationIds) > 0.0D")
                        || compact.contains("bestNearbyEfficiency(player,stationIds)>0.0D"),
                "soft craft station gate must require operational efficiency > 0");
    }

    @Test
    void materialCatalogIndexesStructureSamples() {
        assertTrue(MultiblockMaterialCatalog.builtin().structureCount() >= 50);
        assertTrue(MultiblockMaterialCatalog.builtin().materialsFor("alchemy_furnace_g1").size() >= 1);
        assertTrue(MultiblockMaterialCatalog.builtin().materialsFor("talisman_table").size() >= 1);
    }

    @Test
    void overhaulReservesStructureMaterialsBeforeCommit() throws Exception {
        String source = Files.readString(Path.of(
                "src", "main", "java", "com", "xunxian", "seekingimmortals",
                "structure", "MultiblockOperationalService.java"));
        String compact = source.replaceAll("\\s+", "");
        assertTrue(compact.contains("tryReserveMaterials(player,materials,1)"));
        int mats = compact.indexOf("tryReserveMaterials(player,materials,1)");
        int shards = compact.indexOf("tryReserveShards(player,shardCost)");
        int force = compact.indexOf("forceIntact(level,stationId,origin)");
        assertTrue(mats >= 0 && shards > mats && force > shards,
                "overhaul must reserve structure materials then shards before forceIntact");
        assertTrue(compact.contains("refundStacks(player,reserved)"));
    }
}
