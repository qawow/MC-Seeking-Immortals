package com.xunxian.seekingimmortals.catalog;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuctionSoftServiceTest {
    @Test
    void loadsAuctionVenuesAndLots() {
        AuctionSoftService.Snapshot snapshot = AuctionSoftService.builtin();
        assertTrue(snapshot.venueCount() >= 1);
        assertTrue(snapshot.lotCount() >= 1);
        assertTrue(snapshot.minIncrementPct() > 0.0D);
        AuctionSoftService.Venue wanbao = snapshot.findVenue("wanbao_auction").orElseThrow();
        assertEquals(0, wanbao.repMin(), "a fresh player must be able to place the first Wanbao bid");
    }

    @Test
    void mergesWanbaoFrameworkPool() {
        AuctionSoftService.Snapshot snapshot = AuctionSoftService.builtin();
        // economy_auction_bands has 4 lots; wanbao stock+lots add more framework entries
        assertTrue(snapshot.lotCount() >= 8,
                "expected bands+wanbao merged lots, got " + snapshot.lotCount());
        assertTrue(snapshot.lots().stream().anyMatch(lot -> lot.id().startsWith("wanbao_")));
    }

    @Test
    void insufficientShardBalanceDoesNotMutateAnyStack() {
        List<int[]> stacks = new ArrayList<>();
        stacks.add(new int[] { 3, 1 });
        stacks.add(new int[] { 64, 0 });
        stacks.add(new int[] { 4, 1 });

        assertFalse(AuctionSoftService.consumeMatchingEntries(stacks, stack -> stack[1] == 1,
                stack -> stack[0], (stack, count) -> stack[0] -= count, 8));
        assertEquals(3, stacks.get(0)[0]);
        assertEquals(64, stacks.get(1)[0]);
        assertEquals(4, stacks.get(2)[0]);
    }

    @Test
    void sufficientShardBalanceConsumesExactlyAcrossStacks() {
        List<int[]> stacks = new ArrayList<>();
        stacks.add(new int[] { 3, 1 });
        stacks.add(new int[] { 64, 0 });
        stacks.add(new int[] { 5, 1 });

        assertTrue(AuctionSoftService.consumeMatchingEntries(stacks, stack -> stack[1] == 1,
                stack -> stack[0], (stack, count) -> stack[0] -= count, 6));
        assertEquals(0, stacks.get(0)[0]);
        assertEquals(64, stacks.get(1)[0]);
        assertEquals(2, stacks.get(2)[0]);
    }

    @Test
    void duplicateStackReferencesAreCountedOnlyOnce() {
        int[] sharedStack = new int[] { 5, 1 };
        List<int[]> stacks = List.of(sharedStack, sharedStack);

        assertFalse(AuctionSoftService.consumeMatchingEntries(stacks, stack -> stack[1] == 1,
                stack -> stack[0], (stack, count) -> stack[0] -= count, 8));
        assertEquals(5, sharedStack[0]);
    }

    @Test
    void negativeRequestsFailWithoutMutation() {
        int[] stack = new int[] { 5, 1 };

        assertFalse(AuctionSoftService.consumeMatchingEntries(List.of(stack), value -> value[1] == 1,
                value -> value[0], (value, count) -> value[0] -= count, -1));
        assertEquals(5, stack[0]);
    }
}
