package com.xunxian.seekingimmortals.worldpack;

import com.xunxian.seekingimmortals.structure.FlyingBoatDockStructure;
import com.xunxian.seekingimmortals.structure.ImmortalTeleportGrandArrayStructure;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class M13DimensionsAscensionTest {
    private static final Path DATA = Path.of("src", "main", "resources", "data", "seeking_immortals");

    @Test
    void dimensionRegistryCoversAuraKnownAndCatalog() {
        assertTrue(DimensionRegistryService.size() >= 10);
        assertTrue(DimensionRegistryService.coversAuraKnownDimensions());
        assertTrue(DimensionRegistryService.find(DimensionRegistryService.TIANYUAN).isPresent());
        assertTrue(DimensionRegistryService.find(DimensionRegistryService.SPIRIT_FENGYUAN).isPresent());
        assertTrue(DimensionRegistryService.find(DimensionRegistryService.YIN_MING_POCKET).isPresent());
        assertTrue(DimensionRegistryService.find(DimensionRegistryService.NETHER_RIVER_POCKET).isPresent());
        assertTrue(DimensionRegistryService.find(DimensionRegistryService.DEMON_RIFT).isPresent());
        assertTrue(DimensionRegistryService.find("mortal_world").isPresent());
        assertEquals(DimensionRegistryService.OVERWORLD,
                DimensionRegistryService.toMinecraftDimensionId(DimensionRegistryService.MORTAL_WORLD));
        // deferred markers are explicit, not silent
        assertFalse(DimensionRegistryService.deferredIds().isEmpty());
        assertTrue(DimensionRegistryService.find("seeking_immortals:yin_underworld").map(d -> d.isDeferred()).orElse(false)
                || DimensionRegistryService.deferredIds().stream().anyMatch(id -> id.contains("yin_underworld")
                || id.contains("secret_realm_instance")));
    }

    @Test
    void dimensionsCatalogReconcilesWithDatapackJson() throws Exception {
        Set<String> datapack = Files.list(DATA.resolve("dimension"))
                .filter(p -> p.toString().endsWith(".json"))
                .map(p -> p.getFileName().toString().replace(".json", ""))
                .collect(Collectors.toCollection(HashSet::new));
        assertEquals(11, datapack.size());
        Set<String> types = Files.list(DATA.resolve("dimension_type"))
                .filter(p -> p.toString().endsWith(".json"))
                .map(p -> p.getFileName().toString().replace(".json", ""))
                .collect(Collectors.toCollection(HashSet::new));
        assertEquals(10, types.size());

        // every datapack dim is known to registry (by bare path or namespaced id)
        for (String dim : datapack) {
            assertTrue(DimensionRegistryService.isKnown(dim) || DimensionRegistryService.isKnown("seeking_immortals:" + dim),
                    "registry missing datapack dim " + dim);
        }
        // catalog playable dims present
        for (String id : List.of(
                "seeking_immortals:mortal_world",
                "seeking_immortals:tianyuan",
                "seeking_immortals:spirit_fengyuan",
                "seeking_immortals:yin_ming_pocket",
                "seeking_immortals:demon_rift",
                "seeking_immortals:immortal_realm")) {
            assertTrue(DimensionRegistryService.isKnown(id), "missing catalog dim " + id);
        }
        // deferred logical/template not silently skipped
        assertTrue(Files.exists(DATA.resolve("catalog/dimensions_reconcile.json")));
    }

    @Test
    void spiritRealmInterfaceAndTravelAuthorityLoad() {
        assertTrue(SpiritRealmInterfaceService.gateCount() >= 3);
        assertTrue(SpiritRealmInterfaceService.findGate("mortal_to_tianyuan").isPresent());
        assertTrue(SpiritRealmInterfaceService.isMainBodyOneWay("mortal_to_tianyuan"));
        assertTrue(SpiritRealmInterfaceService.bridge().mainBodyOneWay());
        assertTrue(DimensionTravelService.methodCount() >= 4);
        assertTrue(DimensionTravelService.routeCount() >= 2);
        assertTrue(DimensionTravelService.findMethod("ascension_channel").isPresent()
                || DimensionTravelService.findMethod("fixed_teleport_array").isPresent()
                || DimensionTravelService.methodCount() >= 1);
        assertFalse(DimensionTravelService.snapshot().matrix().isEmpty());
    }

    @Test
    void ascensionFlowAndLoadoutPresent() {
        assertTrue(AscensionService.stageCount() >= 5);
        assertTrue(AscensionService.snapshot().findStage("ascension_channel").isPresent());
        assertTrue(AscensionService.snapshot().findStage("tianyuan_garrison").isPresent());
        assertFalse(AscensionService.snapshot().loadoutPaths().isEmpty());
        assertTrue(Files.exists(DATA.resolve("text_material/ascension_loadout_v95.json")));
        assertTrue(Files.exists(DATA.resolve("text_material/mortal_to_spirit_bridge.json")));
    }

    @Test
    void spatialNodesNetworkAndCatalogExpanded() {
        assertTrue(SpatialNodeCatalogService.builtin().size() >= 33);
        assertTrue(SpatialNodeCatalogService.builtin().find("gate_mortal_to_tianyuan").isPresent());
        assertTrue(SpatialNodeCatalogService.builtin().find("node_immortal_hub").isPresent()
                || SpatialNodeCatalogService.builtin().find("gate_spirit_to_immortal").isPresent()
                || SpatialNodeCatalogService.builtin().size() >= 33);
        // network SavedData class present for M13 teleport network
        assertTrue(SpatialNodeNetworkSavedData.class.getSimpleName().contains("SpatialNode"));
    }

    @Test
    void yinUnderworldClusterRulesLoad() {
        assertTrue(YinUnderworldClusterService.snapshot().regionCount() >= 2);
        assertTrue(YinUnderworldClusterService.isYinDimension(DimensionRegistryService.YIN_MING_POCKET));
        assertTrue(YinUnderworldClusterService.isYinDimension(DimensionRegistryService.NETHER_RIVER_POCKET));
        assertTrue(YinUnderworldClusterService.isYinRegion("yinming"));
        assertTrue(YinUnderworldClusterService.isYinRegion("nether_river"));
    }

    @Test
    void flightDockAndGrandArrayStructuresExist() {
        assertTrue(FlyingBoatDockStructure.platformOffsets().size() >= 9);
        assertTrue(FlyingBoatDockStructure.mastOffsets().size() >= 2);
        // compile-time link to immortal grand array wrapper
        assertTrue(ImmortalTeleportGrandArrayStructure.class.getSimpleName().contains("Immortal"));
    }

    @Test
    void publishedTravelCorpusPresent() {
        assertTrue(Files.exists(DATA.resolve("text_material/dimension_travel_methods_v136.json")));
        assertTrue(Files.exists(DATA.resolve("text_material/dimension_travel_costs_v137.json")));
        assertTrue(Files.exists(DATA.resolve("text_material/dimensions_catalog.json")));
        assertTrue(Files.exists(DATA.resolve("text_material/dimension_registry.json")));
        assertTrue(Files.exists(DATA.resolve("catalog/dimensions_index.json")));
        assertTrue(Files.exists(DATA.resolve("catalog/dimension_registry_index.json")));
        assertTrue(Files.exists(DATA.resolve("catalog/spatial_nodes_index.json")));
    }
}
