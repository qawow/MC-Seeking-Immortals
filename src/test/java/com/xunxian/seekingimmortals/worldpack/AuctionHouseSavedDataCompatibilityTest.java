package com.xunxian.seekingimmortals.worldpack;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * AuctionHouseSavedData must remain readable for older worlds that only have Bids.
 */
class AuctionHouseSavedDataCompatibilityTest {
    @Test
    void loadsLegacyBidsWithoutPendingRefunds() {
        CompoundTag tag = new CompoundTag();
        ListTag bids = new ListTag();
        CompoundTag bid = new CompoundTag();
        bid.putString("LotId", "lot_foundation_pill");
        UUID bidder = UUID.fromString("00000000-0000-0000-0000-000000000001");
        bid.putUUID("Bidder", bidder);
        bid.putInt("Amount", 12);
        bid.putInt("Raises", 2);
        bid.putBoolean("Settled", false);
        bids.add(bid);
        tag.put("Bids", bids);
        // intentionally omit PendingRefunds for legacy save compatibility

        AuctionHouseSavedData data = AuctionHouseSavedData.load(tag);
        assertEquals(12, data.currentAmount("lot_foundation_pill"));
        assertFalse(data.isSettled("lot_foundation_pill"));
        assertTrue(data.getBid("lot_foundation_pill").isPresent());
        assertEquals(0, data.peekPendingRefund(bidder));

        CompoundTag saved = data.save(new CompoundTag());
        assertTrue(saved.contains("Bids"));
        assertTrue(saved.contains("PendingRefunds"));
    }
}
