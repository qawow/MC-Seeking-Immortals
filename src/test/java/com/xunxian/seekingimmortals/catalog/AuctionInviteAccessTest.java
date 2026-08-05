package com.xunxian.seekingimmortals.catalog;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * QA-prep: the auction leg of the release sign-off must be runnable without admin commands.
 *
 * <p>Before this batch {@code AuctionHallMenu} was the only authorisation gate for
 * {@code bid}/{@code settle}, and the only caller of {@link AuctionSoftService#openHall} was the
 * permission-2 {@code /seeking_immortals catalog auction open}. 拍卖请柬 is authored as the
 * credential in three independent places — sold at 乱星海岛杂货 for 30 灵石, carrying
 * {@code effect: open_auction_invite}, and named as {@code auction_invite_or_reputation} on
 * {@code node_dajin_wanbao} — but using it only granted reputation and printed a hint.</p>
 *
 * <p>That reputation grant was itself unbounded: the invite is deliberately exempt from
 * consumption, so it could be right-clicked forever for permanent shop discounts and a free
 * reputation quest proof.</p>
 */
class AuctionInviteAccessTest {
    private static final Path JAVA_ROOT = Path.of(
            "src", "main", "java", "com", "xunxian", "seekingimmortals");

    @Test
    void inviteRegionsComeFromTheAuthoredVenuesAndFailClosed() {
        // The authority is the venue list in economy_auction_bands.json, not a hardcoded set.
        assertTrue(AuctionSoftService.hostsVenue("dajin"), "wanbao_auction sits in 大晋");
        assertTrue(AuctionSoftService.hostsVenue("chaotic_sea"), "chaotic_sea_inner sits in 乱星海");
        assertTrue(AuctionSoftService.hostsVenue("  DAJIN  "), "region ids are normalised");

        assertFalse(AuctionSoftService.hostsVenue("qinglan_mountains"),
                "the starting region hosts no auction venue");
        assertFalse(AuctionSoftService.hostsVenue("yinyang_ku"));
        assertFalse(AuctionSoftService.hostsVenue(""), "a blank region must never open the hall");
        assertFalse(AuctionSoftService.hostsVenue(null));
    }

    @Test
    void theInviteIsANonAdminEntryIntoTheAuctionHall() throws Exception {
        String consumable = Files.readString(JAVA_ROOT.resolve("item/CatalogConsumableService.java"));
        String invite = compact(methodSource(consumable, "private static boolean openAuctionInvite("));

        assertTrue(invite.contains("AuctionSoftService.openHall(player"),
                "using the authored credential must open the hall; otherwise the auction leg of the "
                        + "release sign-off can only be run with a permission-2 command");
        assertTrue(invite.contains("hostsVenue("),
                "the hall must only open where the author placed a venue");
    }

    @Test
    void theIntroductionReputationIsGrantedOnceInsteadOfEveryRightClick() throws Exception {
        String consumable = Files.readString(JAVA_ROOT.resolve("item/CatalogConsumableService.java"));
        String invite = compact(methodSource(consumable, "private static boolean openAuctionInvite("));

        assertTrue(consumable.contains("AUCTION_INVITE_INTRODUCED_KEY"),
                "a reusable pass needs a persistent latch, not a per-use grant");
        assertTrue(invite.contains("AUCTION_INVITE_INTRODUCED_KEY"),
                "the merchant_guild grant must be gated by that latch");
        int latch = invite.indexOf("AUCTION_INVITE_INTRODUCED_KEY");
        int grant = invite.indexOf("ReputationService.add(player,\"merchant_guild\"");
        assertTrue(grant < 0 || latch < grant,
                "the latch must be checked before reputation is added");
    }

    private static String methodSource(String source, String declaration) {
        int start = source.indexOf(declaration);
        assertTrue(start >= 0, "missing source method: " + declaration);
        int openingBrace = source.indexOf('{', start);
        int depth = 0;
        for (int index = openingBrace; index < source.length(); index++) {
            char current = source.charAt(index);
            if (current == '{') {
                depth++;
            } else if (current == '}' && --depth == 0) {
                return source.substring(start, index + 1);
            }
        }
        throw new AssertionError("unterminated source method: " + declaration);
    }

    private static String compact(String source) {
        return source.replaceAll("\\s+", "");
    }
}
