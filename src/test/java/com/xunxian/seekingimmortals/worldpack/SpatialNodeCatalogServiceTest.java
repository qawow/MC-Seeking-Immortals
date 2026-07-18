package com.xunxian.seekingimmortals.worldpack;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpatialNodeCatalogServiceTest {
    @Test
    void loadsSpatialNodesIndex() {
        assertTrue(SpatialNodeCatalogService.builtin().size() >= 20);
        assertTrue(SpatialNodeCatalogService.builtin().find("node_tiannan_huangfeng").isPresent());
        assertTrue(SpatialNodeCatalogService.builtin().sample(5).size() >= 1);
    }

    @Test
    void validatesAuthoredSourceDimension() {
        SpatialNodeCatalogService.Node mortalNode = new SpatialNodeCatalogService.Node(
                "mortal", "Mortal", "fixed_teleport_array", "tiannan", List.of(), 0,
                DimensionRegistryService.MORTAL_WORLD, DimensionRegistryService.MORTAL_WORLD, false);
        assertTrue(SpatialNodeCatalogService.sourceDimensionMatches(DimensionRegistryService.OVERWORLD, mortalNode));
        assertFalse(SpatialNodeCatalogService.sourceDimensionMatches(DimensionRegistryService.TIANYUAN, mortalNode));
    }
}
