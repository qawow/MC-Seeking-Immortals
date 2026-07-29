package com.xunxian.seekingimmortals.multiplayer;

import com.xunxian.seekingimmortals.structure.MultiblockOperationalSavedData;
import com.xunxian.seekingimmortals.worldpack.AuctionHouseSavedData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Offline multiplayer authority regressions for master_plan §6.9.
 * Covers auction ladder outbid refunds, station SavedData last-write keys,
 * packet direction/protocol contracts, PvP targeting, and reconnect hooks.
 * Avoids live dedicated-server / two-client bootstrap.
 */
class MultiplayerAuthorityRegressionTest {
    private static final Path JAVA_ROOT = Path.of(
            "src", "main", "java", "com", "xunxian", "seekingimmortals");

    @Test
    void auctionOutbidRefundLedgerRoundTripsAcrossPlayers() {
        UUID alice = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
        UUID bob = UUID.fromString("00000000-0000-0000-0000-0000000000b2");

        AuctionHouseSavedData house = new AuctionHouseSavedData();
        house.placeOrRaise("lot_mp_signoff", alice, 40);
        assertEquals(40, house.currentAmount("lot_mp_signoff"));
        assertEquals(alice, house.getBid("lot_mp_signoff").orElseThrow().bidder());

        // Bob outbids; offline Alice should accrue refund ledger, not share Bob's ledger.
        house.placeOrRaise("lot_mp_signoff", bob, 55);
        house.addPendingRefund(alice, 40);
        assertEquals(55, house.currentAmount("lot_mp_signoff"));
        assertEquals(bob, house.getBid("lot_mp_signoff").orElseThrow().bidder());
        assertEquals(40, house.peekPendingRefund(alice));
        assertEquals(0, house.peekPendingRefund(bob));

        CompoundTag saved = house.save(new CompoundTag());
        AuctionHouseSavedData restored = AuctionHouseSavedData.load(saved);
        assertEquals(55, restored.currentAmount("lot_mp_signoff"));
        assertEquals(bob, restored.getBid("lot_mp_signoff").orElseThrow().bidder());
        assertEquals(40, restored.peekPendingRefund(alice));
        assertEquals(0, restored.peekPendingRefund(bob));

        // take is destructive and per-UUID; Bob still has nothing after Alice claims.
        assertEquals(40, restored.takePendingRefund(alice));
        assertEquals(0, restored.peekPendingRefund(alice));
        assertEquals(0, restored.takePendingRefund(bob));
        assertEquals(0, restored.takePendingRefund(alice));
    }

    @Test
    void auctionRefundLedgerAccumulatesAndSurvivesLegacyMissingSection() {
        UUID offline = UUID.fromString("00000000-0000-0000-0000-0000000000c3");
        AuctionHouseSavedData house = new AuctionHouseSavedData();
        house.addPendingRefund(offline, 12);
        house.addPendingRefund(offline, 8);
        assertEquals(20, house.peekPendingRefund(offline));

        CompoundTag bidsOnly = new CompoundTag();
        ListTag bids = new ListTag();
        CompoundTag bid = new CompoundTag();
        bid.putString("LotId", "lot_legacy");
        bid.putUUID("Bidder", offline);
        bid.putInt("Amount", 3);
        bid.putInt("Raises", 1);
        bid.putBoolean("Settled", false);
        bids.add(bid);
        bidsOnly.put("Bids", bids);

        AuctionHouseSavedData legacy = AuctionHouseSavedData.load(bidsOnly);
        assertEquals(3, legacy.currentAmount("lot_legacy"));
        assertEquals(0, legacy.peekPendingRefund(offline));
        assertTrue(legacy.save(new CompoundTag()).contains("PendingRefunds"));
    }

    @Test
    void stationOperationalKeysIsolateDimensionAndOriginLastWrite() {
        MultiblockOperationalSavedData data = MultiblockOperationalSavedData.load(new CompoundTag());
        String overworld = "minecraft:overworld";
        String nether = "minecraft:the_nether";
        long originA = 100L;
        long originB = 200L;

        MultiblockOperationalSavedData.StationState a = new MultiblockOperationalSavedData.StationState(
                overworld, "alchemy_furnace_g1", originA,
                MultiblockOperationalSavedData.OpState.DISABLED, 0, 100);
        MultiblockOperationalSavedData.StationState b = new MultiblockOperationalSavedData.StationState(
                overworld, "alchemy_furnace_g1", originB,
                MultiblockOperationalSavedData.OpState.INTACT, 100, 100);
        MultiblockOperationalSavedData.StationState netherCopy = new MultiblockOperationalSavedData.StationState(
                nether, "alchemy_furnace_g1", originA,
                MultiblockOperationalSavedData.OpState.DAMAGED, 40, 100);

        data.upsert(a);
        data.upsert(b);
        data.upsert(netherCopy);

        assertEquals(MultiblockOperationalSavedData.OpState.DISABLED,
                data.find(overworld, "alchemy_furnace_g1",
                        net.minecraft.core.BlockPos.of(originA)).orElseThrow().state());
        assertEquals(MultiblockOperationalSavedData.OpState.INTACT,
                data.find(overworld, "alchemy_furnace_g1",
                        net.minecraft.core.BlockPos.of(originB)).orElseThrow().state());
        assertEquals(MultiblockOperationalSavedData.OpState.DAMAGED,
                data.find(nether, "alchemy_furnace_g1",
                        net.minecraft.core.BlockPos.of(originA)).orElseThrow().state());

        // Concurrent second commission on same key is last-write; form path rejects non-DISABLED.
        MultiblockOperationalSavedData.StationState overwrite = new MultiblockOperationalSavedData.StationState(
                overworld, "alchemy_furnace_g1", originA,
                MultiblockOperationalSavedData.OpState.INTACT, 100, 100);
        data.upsert(overwrite);
        assertEquals(MultiblockOperationalSavedData.OpState.INTACT,
                data.find(overworld, "alchemy_furnace_g1",
                        net.minecraft.core.BlockPos.of(originA)).orElseThrow().state());

        CompoundTag saved = data.save(new CompoundTag());
        MultiblockOperationalSavedData restored = MultiblockOperationalSavedData.load(saved);
        assertEquals(MultiblockOperationalSavedData.OpState.INTACT,
                restored.find(overworld, "alchemy_furnace_g1",
                        net.minecraft.core.BlockPos.of(originA)).orElseThrow().state());
        assertEquals(MultiblockOperationalSavedData.OpState.INTACT,
                restored.find(overworld, "alchemy_furnace_g1",
                        net.minecraft.core.BlockPos.of(originB)).orElseThrow().state());
        assertEquals(MultiblockOperationalSavedData.OpState.DAMAGED,
                restored.find(nether, "alchemy_furnace_g1",
                        net.minecraft.core.BlockPos.of(originA)).orElseThrow().state());
    }

    @Test
    void formRejectsAlreadyCommissionedStationInSource() throws Exception {
        String ops = Files.readString(JAVA_ROOT.resolve(
                Path.of("structure", "MultiblockOperationalService.java")));
        assertTrue(ops.contains("already_commissioned"));
        assertTrue(ops.contains("form_use_overhaul"));
        assertTrue(ops.contains("OpState.INTACT") && ops.contains("state.hp() >= state.maxHp()"));
        // Material reserve → commit → refund on failure (no silent double-spend path).
        assertTrue(ops.contains("tryReserveMaterials"));
        assertTrue(ops.contains("refundStacks(player, reserved)"));
        assertTrue(ops.contains("refundStacks(player, shards)"));
    }

    @Test
    void reconnectHooksClaimOutboxAndAuctionRefundsOnLogin() throws Exception {
        String events = Files.readString(JAVA_ROOT.resolve(Path.of("event", "ModEvents.java")));
        assertTrue(events.contains("PlayerEvent.PlayerLoggedInEvent"));
        assertTrue(events.contains("InventoryDeliveryService.claimQueued"));
        assertTrue(events.contains("AuctionSoftService.claimPendingRefunds")
                || events.contains("claimPendingRefunds"));

        String outbox = Files.readString(JAVA_ROOT.resolve(
                Path.of("item", "DeliveryOutboxSavedData.java")));
        assertTrue(outbox.contains("byPlayer"));
        assertTrue(outbox.contains("claimAll"));
        assertTrue(outbox.contains("enqueue"));
        assertTrue(outbox.contains("MAX_ENTRIES_PER_PLAYER"));
        // Claim is per UUID — one player's claim must not drain another ledger.
        assertTrue(outbox.contains("byPlayer.remove(playerId)"));

        String auction = Files.readString(JAVA_ROOT.resolve(
                Path.of("catalog", "AuctionSoftService.java")));
        assertTrue(auction.contains("addPendingRefund(previousLeader, previousEscrow)"));
        assertTrue(auction.contains("giveShards(previous, previousEscrow)"));
        assertTrue(auction.contains("peekPendingRefund") && auction.contains("takePendingRefund"));
    }

    @Test
    void techniqueCooldownPersistsInCapabilityNbtForReconnect() throws Exception {
        // Pure capability path is covered by CultivationAuthorityRegressionTest;
        // here we lock the multiplayer reconnect contract: global tick cooldowns in NBT.
        String cultivation = Files.readString(JAVA_ROOT.resolve(
                Path.of("cultivation", "PlayerCultivation.java")));
        assertTrue(cultivation.contains("TechniqueCooldownUntilTicks"));
        assertTrue(cultivation.contains("setTechniqueCooldown"));
        assertTrue(cultivation.contains("getTechniqueCooldownUntilTick"));
        assertTrue(cultivation.contains("CultivationNbtVersion"));
    }

    @Test
    void pvpSpellTargetingHonorsCanHarmPlayer() throws Exception {
        String spell = Files.readString(JAVA_ROOT.resolve(
                Path.of("skill", "effect", "spell", "SpellEffect.java")));
        assertTrue(spell.contains("protected static boolean canAffect"));
        assertTrue(spell.contains("caster.canHarmPlayer(targetPlayer)"));
        assertFalse(spell.contains("return true; // always hit players"));

        String network = Files.readString(JAVA_ROOT.resolve(Path.of("network", "ModNetwork.java")));
        assertTrue(network.contains("PROTOCOL_VERSION = \"31\"")
                || network.contains("PROTOCOL_VERSION=\"31\"")
                || network.contains("private static final String PROTOCOL_VERSION = \"31\""));
    }

    @Test
    void multiplayerSequenceCatalogIsPresentationOnly() throws Exception {
        String catalog = Files.readString(JAVA_ROOT.resolve(
                Path.of("structure", "MultiblockSequenceDisplayCatalog.java")));
        assertTrue(catalog.contains("does not enforce multiplayer locks")
                || catalog.contains("presentation data only"));
        // Runtime authority lives in OperationalService/SavedData, not display catalog.
        String ops = Files.readString(JAVA_ROOT.resolve(
                Path.of("structure", "MultiblockOperationalService.java")));
        assertTrue(ops.contains("ServerLevel"));
        assertTrue(ops.contains("MultiblockOperationalSavedData.get(level)"));
    }

    @Test
    void liveSmokeMultiplayerSurfacesExist() throws Exception {
        String smoke = Files.readString(JAVA_ROOT.resolve(
                Path.of("command", "LiveSmokeChecklistService.java")));
        assertTrue(smoke.contains("MANUAL_MP_STEPS") || smoke.contains("manual_mp")
                || smoke.contains("multiplayer_pvp")
                || smoke.contains("mp_pvp"),
                "live smoke should expose multiplayer manual steps");
        assertTrue(smoke.contains("auction_outbid_ledger")
                        || smoke.contains("outbox_uuid_isolation")
                        || smoke.contains("protocol_version"),
                "live smoke should auto-probe multiplayer authority surfaces");
    }

    @Test
    void networkPacketsDoNotEmbedClientScreenTypes() throws Exception {
        Path network = JAVA_ROOT.resolve("network");
        try (var paths = Files.list(network)) {
            for (Path path : paths.filter(p -> p.toString().endsWith(".java")).toList()) {
                String source = Files.readString(path);
                assertFalse(source.contains("net.minecraft.client.Minecraft"),
                        path.getFileName() + " must not reference Minecraft client");
                assertFalse(source.contains("net.minecraft.client.gui"),
                        path.getFileName() + " must not reference client.gui");
                assertFalse(source.contains(".setScreen("),
                        path.getFileName() + " must not call setScreen");
                assertFalse(source.contains("client.SectScreen")
                                || source.contains("client.ShopScreen")
                                || source.contains("client.DialogueScreen")
                                || source.contains("client.AuctionScreen")
                                || source.contains("client.WorldpackScreen")
                                || source.contains("client.BestiaryScreen")
                                || source.contains("client.LoreCompendiumScreen")
                                || source.contains("client.AlchemyStatusScreen")
                                || source.contains("client.StorageBraceletScreen")
                                || source.contains("client.RefinementPlanScreen")
                                || source.contains("client.QuestTrackerScreen"),
                        path.getFileName() + " must not hard-reference client Screen classes");
            }
        }
        String dispatch = Files.readString(network.resolve("ClientPacketDispatch.java"));
        assertTrue(dispatch.contains("Class.forName"));
        assertTrue(dispatch.contains("ClientPacketHandlers"));
        String handlers = Files.readString(JAVA_ROOT.resolve(
                Path.of("client", "ClientPacketHandlers.java")));
        assertTrue(handlers.contains("handleSyncSect"));
        assertTrue(handlers.contains("handleOpenDialogue"));
        assertFalse(handlers.contains("handleOpenAuction"));
    }
}
