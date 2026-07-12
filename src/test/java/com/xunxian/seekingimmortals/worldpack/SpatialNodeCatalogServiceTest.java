package com.xunxian.seekingimmortals.worldpack;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SpatialNodeCatalogServiceTest {
    @Test
    void loadsSpatialNodesIndex() {
        assertTrue(SpatialNodeCatalogService.builtin().size() >= 20);
        assertTrue(SpatialNodeCatalogService.builtin().find("node_tiannan_huangfeng").isPresent());
        assertTrue(SpatialNodeCatalogService.builtin().sample(5).size() >= 1);
    }
}
