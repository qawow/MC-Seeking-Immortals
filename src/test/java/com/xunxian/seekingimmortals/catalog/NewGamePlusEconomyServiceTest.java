package com.xunxian.seekingimmortals.catalog;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NewGamePlusEconomyServiceTest {
    @Test
    void loadsDifficultyPriceMods() {
        NewGamePlusEconomyService.Snapshot snapshot = NewGamePlusEconomyService.builtin();
        assertTrue(snapshot.presets().size() >= 4);
        assertEquals(1.0D, NewGamePlusEconomyService.priceMod("standard"), 0.001D);
        assertEquals(0.8D, NewGamePlusEconomyService.priceMod("story"), 0.001D);
        assertEquals(1.3D, NewGamePlusEconomyService.priceMod("hard"), 0.001D);
        assertTrue(snapshot.economyFuseIds().size() >= 1);
        assertTrue(snapshot.modes().size() >= 1);
    }
}
