package com.xunxian.seekingimmortals.catalog;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EconomyPriceBandServiceTest {
    @Test
    void loadsBandsAndSuggestsCosts() {
        assertTrue(EconomyPriceBandService.bandCount() >= 10);
        assertTrue(EconomyPriceBandService.find("talisman_low").isPresent());
        assertTrue(EconomyPriceBandService.suggestedCost("talisman_low", 1) >= 1);
        assertEquals("herb_common", EconomyPriceBandService.guessBandForItem("seeking_immortals:spirit_grass").orElse(""));
        assertTrue(EconomyPriceBandService.clampToBand("talisman_low", 1) >= 1);
    }
}
