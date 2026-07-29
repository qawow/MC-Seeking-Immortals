package com.xunxian.seekingimmortals.catalog;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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

    @Test
    void settleClaimsLedgerBeforeDeliveringRewards() throws Exception {
        String source = java.nio.file.Files.readString(java.nio.file.Path.of(
                "src", "main", "java", "com", "xunxian", "seekingimmortals",
                "catalog", "AuctionSoftService.java"));
        String compact = source.replaceAll("\\s+", "");
        int settleStart = compact.indexOf("privatestaticbooleansettle(ServerPlayerplayer,Lotlot)");
        assertTrue(settleStart >= 0, "private settle(Lot) must exist");
        int bodyStart = compact.indexOf('{', settleStart);
        int depth = 0;
        int bodyEnd = -1;
        for (int i = bodyStart; i < compact.length(); i++) {
            char c = compact.charAt(i);
            if (c == '{') {
                depth++;
            } else if (c == '}' && --depth == 0) {
                bodyEnd = i;
                break;
            }
        }
        assertTrue(bodyEnd > bodyStart);
        String settle = compact.substring(settleStart, bodyEnd + 1);

        int claimSettled = settle.indexOf("house.markSettled(lot.id())");
        int setDirty = settle.indexOf("house.setDirty()");
        int claimWon = settle.indexOf("won.putBoolean(lot.id(),true)");
        int persistWon = settle.indexOf("player.getPersistentData().put(WON_ROOT,won)");
        int deliver = settle.indexOf("InventoryDeliveryService.giveOrEnqueue(player,reward");
        assertTrue(claimSettled >= 0 && setDirty > claimSettled,
                "settle must markSettled and setDirty before delivery for idempotency");
        assertTrue(deliver > setDirty, "reward delivery must follow persistent ledger write");
        assertTrue(claimWon > deliver && persistWon > claimWon,
                "player won NBT must be written AFTER delivery as soft guard only");
        assertTrue(settle.contains("already_won") && settle.indexOf("house.markSettled(lot.id())")
                        != settle.lastIndexOf("house.markSettled(lot.id())"),
                "already-won drift must heal house settled flag without re-delivery");
        assertTrue(settle.contains("rewardItemFor(lot)") && settle.contains("Items.AIR"),
                "unresolvable reward items must fail closed before claim");
    }

    @Test
    void authoredRewardItemFailsClosedWhenUnresolved() throws Exception {
        String source = java.nio.file.Files.readString(java.nio.file.Path.of(
                "src", "main", "java", "com", "xunxian", "seekingimmortals",
                "catalog", "AuctionSoftService.java"));
        String reward = source.replaceAll("\\s+", "");
        int method = reward.indexOf("privatestaticItemrewardItemFor(Lotlot)");
        assertTrue(method >= 0);
        int bodyStart = reward.indexOf('{', method);
        int depth = 0;
        int bodyEnd = -1;
        for (int i = bodyStart; i < reward.length(); i++) {
            char c = reward.charAt(i);
            if (c == '{') {
                depth++;
            } else if (c == '}' && --depth == 0) {
                bodyEnd = i;
                break;
            }
        }
        String body = reward.substring(method, bodyEnd + 1);
        assertTrue(body.contains("lot.rewardItem()") || body.contains("authoredReward"),
                "reward resolution must inspect authored reward_item");
        assertTrue(body.contains("returnnull;"),
                "authored but unresolved reward ids must fail closed instead of fuzzy fallback");
    }

    @Test
    void currentLeaderReplayIsRecognizedBeforeAnyShardReservation() throws Exception {
        UUID leader = UUID.randomUUID();
        assertTrue(AuctionSoftService.isCurrentLeader(leader, leader));
        assertFalse(AuctionSoftService.isCurrentLeader(leader, UUID.randomUUID()));
        assertFalse(AuctionSoftService.isCurrentLeader(null, leader));
        assertFalse(AuctionSoftService.shouldAutoSettle(4, 50_000L, 1L, 9));
        assertTrue(AuctionSoftService.shouldAutoSettle(5, 50_000L, 1L, 9));
        assertTrue(AuctionSoftService.shouldAutoSettle(1, 50_000L, 1L, 10));

        String source = java.nio.file.Files.readString(java.nio.file.Path.of(
                "src", "main", "java", "com", "xunxian", "seekingimmortals",
                "catalog", "AuctionSoftService.java")).replaceAll("\\s+", "");
        int leaderGate = source.indexOf("if(isCurrentLeader(currentLeader,player.getUUID()))");
        int consume = source.indexOf("consumeShards(player,delta)");
        int place = source.indexOf("house.placeOrRaise(lot.id(),player.getUUID(),next)");
        assertTrue(leaderGate >= 0 && consume > leaderGate,
                "the current leader must be rejected before shard consumption");
        assertTrue(place > consume, "no auction state may advance before payment succeeds");
    }
}
