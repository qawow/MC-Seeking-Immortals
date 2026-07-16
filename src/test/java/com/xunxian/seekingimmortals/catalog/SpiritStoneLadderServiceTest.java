package com.xunxian.seekingimmortals.catalog;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpiritStoneLadderServiceTest {
    @Test
    void loadsFourTierWithHundredRatioChain() {
        SpiritStoneLadderService.Snapshot snapshot = SpiritStoneLadderService.builtin();
        assertEquals(100, snapshot.ratioPerTier());
        assertTrue(snapshot.tiers().size() >= 4);
        assertEquals(1L, SpiritStoneLadderService.toLowEquiv("low", 1));
        assertEquals(100L, SpiritStoneLadderService.toLowEquiv("mid", 1));
        assertEquals(10000L, SpiritStoneLadderService.toLowEquiv("high", 1));
        assertEquals(1000000L, SpiritStoneLadderService.toLowEquiv("peak", 1));
        assertEquals(1000000L, SpiritStoneLadderService.toLowEquiv("top", 1));
    }

    @Test
    void chainItemIdsMatchMasterContract() {
        SpiritStoneLadderService.Snapshot snapshot = SpiritStoneLadderService.builtin();
        assertEquals("low_spirit_stone", snapshot.findTier("low").orElseThrow().itemId());
        assertEquals("mid_spirit_stone", snapshot.findTier("mid").orElseThrow().itemId());
        assertEquals("high_spirit_stone", snapshot.findTier("high").orElseThrow().itemId());
        assertEquals("top_spirit_stone", snapshot.findTier("peak").orElseThrow().itemId());
        var step = snapshot.nextUpgrade("low").orElseThrow();
        assertEquals("mid", step.toTier());
        assertEquals(100, step.ratio());
        assertFalse(snapshot.nextUpgrade("peak").isPresent());
    }

    @Test
    void shardToLowRateFromCurrencyItems() {
        assertTrue(SpiritStoneLadderService.builtin().shardToLowRate() >= 10);
    }
}
