package com.xunxian.seekingimmortals.catalog;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import com.xunxian.seekingimmortals.util.PlayerDisplayText;

/**
 * Soft auction interest tracking: players can mark interest on lots.
 * No bidding settlement; stored in player persistent data.
 */
public final class AuctionInterestService {
    private static final String ROOT = "seeking_immortals_auction_interest";

    private AuctionInterestService() {}

    public static boolean markInterest(ServerPlayer player, String lotId) {
        AuctionSoftService.Snapshot snapshot = AuctionSoftService.builtin();
        Component display = targetDisplay(snapshot, lotId);
        if (snapshot.findLot(lotId).isEmpty() && snapshot.findVenue(lotId).isEmpty()) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.auction.unknown", display), false);
            return false;
        }
        CompoundTag root = player.getPersistentData().getCompound(ROOT).copy();
        root.putBoolean(lotId, true);
        player.getPersistentData().put(ROOT, root);
        player.displayClientMessage(Component.translatable("message.seeking_immortals.auction.interest_marked", display), true);
        return true;
    }

    public static int interestCount(ServerPlayer player) {
        return player.getPersistentData().getCompound(ROOT).getAllKeys().size();
    }

    private static Component targetDisplay(AuctionSoftService.Snapshot snapshot, String id) {
        if (snapshot != null) {
            var lot = snapshot.findLot(id);
            if (lot.isPresent()) {
                return PlayerDisplayText.safeLiteral(lot.get().display(), "text.seeking_immortals.unknown_item");
            }
            if (snapshot.findVenue(id).isPresent()) {
                return Component.translatable("text.seeking_immortals.auction_venue");
            }
        }
        return Component.translatable("text.seeking_immortals.unknown_auction_target");
    }
}
