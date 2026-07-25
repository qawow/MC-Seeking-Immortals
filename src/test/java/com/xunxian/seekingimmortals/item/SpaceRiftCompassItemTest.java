package com.xunxian.seekingimmortals.item;

import com.xunxian.seekingimmortals.artifact.ArtifactActivationService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpaceRiftCompassItemTest {
    @Test
    void resolvesEightWayDirections() {
        assertTrue(ArtifactActivationService.hasActivation("space_rift_compass"));
        assertEquals("direction.seeking_immortals.east", SpaceRiftCompassDirection.key(10, 0));
        assertEquals("direction.seeking_immortals.south", SpaceRiftCompassDirection.key(0, 10));
        assertEquals("direction.seeking_immortals.west", SpaceRiftCompassDirection.key(-10, 0));
        assertEquals("direction.seeking_immortals.north", SpaceRiftCompassDirection.key(0, -10));
        assertEquals("direction.seeking_immortals.southeast", SpaceRiftCompassDirection.key(10, 10));
        assertEquals("direction.seeking_immortals.northwest", SpaceRiftCompassDirection.key(-10, -10));
    }
}
