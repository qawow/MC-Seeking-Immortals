package com.xunxian.seekingimmortals.quest;

import com.xunxian.seekingimmortals.catalog.ItemCatalogService;
import com.xunxian.seekingimmortals.catalog.ManualCatalogService;
import com.xunxian.seekingimmortals.cultivation.CultivationHelper;
import com.xunxian.seekingimmortals.cultivation.PlayerCultivation;
import com.xunxian.seekingimmortals.cultivation.Realm;
import com.xunxian.seekingimmortals.structure.MultiblockOperationalService;
import com.xunxian.seekingimmortals.structure.MultiblockStationService;
import com.xunxian.seekingimmortals.structure.MultiblockStructureCatalog;
import com.xunxian.seekingimmortals.worldpack.SecretRealmProgressSavedData;
import com.xunxian.seekingimmortals.worldpack.SecretRealmSessionService;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Single authority for structured detailed-quest proofs.
 *
 * <p>The service matches an event only against the player's current route step, validates the
 * producer and route parameters, and then delegates the reward transaction to the existing
 * idempotent runtime. A repeated event is harmless and one call can advance at most one step per
 * chain. World proofs (region/dimension/structure) additionally re-check the live server state:
 * the player's actual region/dimension after a transition, the bound secret-realm session for
 * deep layer ids, and the formed-and-commissioned multiblock at the exact recorded origin.</p>
 */
public final class DetailedQuestProofService {
    public static final String LEDGER_TAG = "seeking_immortals_detailed_quest_proof_ledger";
    public static final String HISTORY_TAG = "seeking_immortals_detailed_quest_proof_history";
    private static final int MAX_LEDGER_ENTRIES = 512;

    /** Region ids that only a bound secret-realm session event may prove. */
    static final Set<String> SECRET_REALM_REGIONS = Set.of(
            "blood_forbidden", "bf_outer_mist", "bf_water_jiao", "blood_forbidden_exit_array",
            "island_xutian_window", "dajin_kunwu_approach", "fallen_demon_rift", "zm_inner", "zm_candle",
            "yinyang_cave_gate", "gh_approach");

    /**
     * Q-B-3 authored item tokens -> real canonical items that can prove them. Tokens that are
     * already canonical items (or alias-collapse to one) need no entry here; the identity rule
     * covers them. Only server-observed pickups/crafts/deliveries of these canonical items may
     * produce the corresponding proof.
     */
    static final Map<String, Set<String>> PROOF_ITEM_MAPPINGS = Map.ofEntries(
            Map.entry("spirit_herb", Set.of("blood_forbidden_herb", "spirit_herb_bundle")),
            Map.entry("xutian_map_fragment", Set.of("void_palace_map_fragment")),
            Map.entry("survival_preparation", Set.of(
                    "spirit_recovery_pill", "detox_minor_pill", "escape_talisman", "fire_talisman", "speed_talisman")),
            Map.entry("fire_resist_ready", Set.of("yang_flame_talisman")),
            Map.entry("fire_toad_resistance", Set.of("yang_flame_talisman", "detox_minor_pill")),
            Map.entry("realm_gate_token", Set.of("spirit_realm_gate_pass", "spirit_realm_gate_voucher")));

    /**
     * Q-B-4 authored entity tokens -> real server entity/boss ids that can prove them. The
     * identity rule always applies because the event entity id is a server-observed fact from
     * the killed/captured entity; no client string can inject it.
     */
    static final Map<String, Set<String>> PROOF_ENTITY_MAPPINGS = Map.ofEntries(
            Map.entry("qianzhu_tower_lord", Set.of("puppet_tower_lord")));

    /** Q-B-4 authored npc tokens -> real server npc ids that can prove them. */
    static final Map<String, Set<String>> PROOF_NPC_MAPPINGS = Map.ofEntries(
            Map.entry("qianzhu_teacher", Set.of("npc_qianzhu_mechanic")));

    /** Q-B-5 authored shop tokens -> real market shop ids that can prove them. */
    static final Map<String, Set<String>> PROOF_SHOP_MAPPINGS = Map.ofEntries(
            Map.entry("star_palace_registry", Set.of("star_registration", "star_palace_patrol_supply")),
            Map.entry("inverse_star_smuggle", Set.of("inverse_black", "inverse_star_black_market")),
            Map.entry("reincarnation_trade_desk", Set.of("reincarnation_desk", "nether_ferry_vendor")));

    /** Q-B-5 authored auction tokens -> real auction venue ids that can prove them. */
    static final Map<String, Set<String>> PROOF_AUCTION_MAPPINGS = Map.ofEntries(
            Map.entry("dajin_wanbao_auction", Set.of("wanbao_auction")));

    /** Q-B-5 authored faction tokens -> real reputation ledger keys that can prove them. */
    static final Map<String, Set<String>> PROOF_FACTION_MAPPINGS = Map.ofEntries(
            Map.entry("huangfeng", Set.of("huangfeng", "huangfeng_gu")));

    /**
     * Q-B-5 authored rule-acknowledgement tokens produced by specific server dialogue nodes:
     * choice token -> (treeId, nodeId) source pairs. Only these server-observed node visits may
     * produce the corresponding INFO_ACKNOWLEDGED proof.
     */
    static final Map<String, Set<String>> INFO_CHOICE_SOURCES = Map.ofEntries(
            Map.entry("blood_forbidden_window", Set.of("tree_sect_contribution_clerk:quota_shop")),
            Map.entry("star_palace_rejection_rule", Set.of("tree_star_palace_registrar:inverse_block")),
            Map.entry("fengyuan_gate_contribution_rule", Set.of("tree_tianyuan_registrar:pay_portal")),
            Map.entry("tianyuan_garrison_board", Set.of("tree_tianyuan_registrar:jobs")),
            Map.entry("true_word_lecture", Set.of("tree_zhenyan_lecturer:accept_lesson")),
            Map.entry("reincarnation_backlash_terms", Set.of("tree_reincarnation_clerk:intro_quest")));

    /** Q-B-5 authored choice tokens produced by specific server dialogue nodes. */
    static final Map<String, Set<String>> CHOICE_COMMITTED_SOURCES = Map.ofEntries(
            Map.entry("inverse_star_cipher", Set.of("tree_inverse_star_contact:cipher")),
            Map.entry("true_word_basic_drill", Set.of("tree_zhenyan_lecturer:accept_lesson")));

    public enum Status {
        ACCEPTED,
        DUPLICATE,
        STORED_FOR_REPLAY,
        REJECTED
    }

    public record Result(Status status, int advanced, String reason) {
        public boolean accepted() {
            return status == Status.ACCEPTED || status == Status.DUPLICATE
                    || status == Status.STORED_FOR_REPLAY;
        }
    }

    private DetailedQuestProofService() {}

    /** Records one server-created event. Client input must never be passed as an event factory. */
    public static Result record(ServerPlayer player, DetailedQuestProofEvent rawEvent) {
        if (player == null || rawEvent == null) {
            return rejected("missing_player_or_event");
        }
        DetailedQuestProofEvent event = rawEvent.ownerId() == null
                ? rawEvent.forOwner(player.getUUID()) : rawEvent;
        if (event.ownerId() != null && !player.getUUID().equals(event.ownerId())) {
            return rejected("owner_mismatch");
        }
        if (event.source() == DetailedQuestProofEvent.Source.ADMIN) {
            return rejected("admin_source_requires_admin_path");
        }
        List<DetailedQuestProofCatalog.Route> candidates = matchingRoutes(event);
        if (candidates.isEmpty()) {
            return rejected("route_mismatch");
        }
        int advanced = 0;
        boolean stored = false;
        boolean duplicate = false;
        Set<String> handledChains = new LinkedHashSet<>();
        for (DetailedQuestProofCatalog.Route route : candidates) {
            String ledgerKey = ledgerKey(route, event);
            // Ledger check first: an already-proven route must not consume the one-step-per-chain
            // slot, otherwise a later same-chain route (e.g. court_hunt_gray step 2/4 both
            // delivering gray_realm_clue) would be permanently shadowed.
            if (hasLedger(player, ledgerKey)) {
                duplicate = true;
                continue;
            }
            if (!handledChains.add(route.chainId())) {
                continue;
            }
            if (!authoritativeState(player, route, event)) {
                continue;
            }
            DetailedQuestRuntimeService.Progress progress =
                    DetailedQuestRuntimeService.progressOf(player, route.chainId());
            if (!progress.started() || progress.complete() || progress.stage() != route.step()) {
                if (route.allowHistoryReplay()) {
                    stored |= storeHistory(player, route, event);
                }
                continue;
            }
            boolean moved = DetailedQuestRuntimeService.advanceVerifiedRoute(player, route.chainId(), route,
                    DetailedQuestRuntimeService.Evidence.of(route.eventId(), event.eventKey()));
            if (moved) {
                writeLedger(player, ledgerKey, event, false);
                advanced++;
            }
        }
        if (advanced > 0) {
            return new Result(Status.ACCEPTED, advanced, "accepted");
        }
        if (stored) {
            return new Result(Status.STORED_FOR_REPLAY, 0, "history_stored");
        }
        if (duplicate) {
            return new Result(Status.DUPLICATE, 0, "duplicate_event");
        }
        return rejected("state_or_step_mismatch");
    }

    public static Result recordMethodLayerReached(ServerPlayer player, String methodId) {
        if (player == null || methodId == null || methodId.isBlank()) {
            return rejected("missing_method");
        }
        int layer = ManualCatalogService.getMethodLayer(player, methodId);
        if (layer <= 0) {
            return rejected("method_not_learned");
        }
        return record(player, DetailedQuestProofEvent.methodLayerReached(methodId, layer));
    }

    public static Result recordRealmReached(ServerPlayer player, Realm realm) {
        return realm == null ? rejected("missing_realm")
                : record(player, DetailedQuestProofEvent.realmReached(realm));
    }

    public static Result recordTechniqueLearned(ServerPlayer player, String techniqueId) {
        return record(player, DetailedQuestProofEvent.techniqueLearned(techniqueId));
    }

    /** Strict spiritual-root test proof; only the appraisal producer may call this. */
    public static Result recordSpiritualRootTested(ServerPlayer player) {
        return record(player, DetailedQuestProofEvent.spiritualRootTested());
    }

    /** Item pickup proof; only the server-observed pickup event may call this. */
    public static Result recordItemAcquired(ServerPlayer player, String itemId) {
        return recordWithCanonicalItem(player, itemId, "acquire");
    }

    /** Craft-completion proof; only the server crafting event may call this. */
    public static Result recordItemCrafted(ServerPlayer player, String itemId) {
        return recordWithCanonicalItem(player, itemId, "craft");
    }

    /** Alchemy-batch completion proof at a real furnace station. */
    public static Result recordAlchemyCompleted(ServerPlayer player, String stationId) {
        if (player == null) {
            return rejected("missing_player");
        }
        String station = normalize(stationId);
        if (station.isBlank()) {
            return rejected("missing_station");
        }
        return record(player, DetailedQuestProofEvent.alchemyCompleted(station));
    }

    /**
     * Item turn-in proof. The player must really hold an item that proves the delivered route
     * token and the delivering npc must match the chain giver or current step place.
     */
    public static Result recordItemDelivered(ServerPlayer player, String npcId) {
        if (player == null) {
            return rejected("missing_player");
        }
        String npc = normalize(npcId);
        Result last = rejected("no_delivery_route");
        boolean anyAccepted = false;
        int advancedTotal = 0;
        for (DetailedQuestRuntimeService.Progress progress : DetailedQuestRuntimeService.listProgress(player)) {
            if (!progress.started() || progress.complete()) {
                continue;
            }
            DetailedQuestProofCatalog.Route route = DetailedQuestRuntimeService.proofCatalog()
                    .find(progress.id(), progress.stage());
            if (route == null || !"ITEM_DELIVERED".equals(route.proofType())) {
                continue;
            }
            DetailedQuestRuntimeService.Chain chain = DetailedQuestRuntimeService.find(progress.id()).orElse(null);
            if (chain == null || !deliveryNpcMatches(chain, progress.stage(), npc)) {
                continue;
            }
            String held = heldProvingItem(player, route.parameter("item"));
            if (held == null) {
                continue;
            }
            Result result = record(player, DetailedQuestProofEvent.itemDelivered(held));
            if (result.accepted()) {
                anyAccepted = true;
            }
            advancedTotal = Math.max(advancedTotal, result.advanced());
            last = result;
        }
        if (anyAccepted) {
            return new Result(Status.ACCEPTED, advancedTotal, "item_delivered");
        }
        return last;
    }

    private static Result recordWithCanonicalItem(ServerPlayer player, String itemId, String kind) {
        if (player == null) {
            return rejected("missing_player");
        }
        if (itemId == null || itemId.isBlank()) {
            return rejected("missing_item");
        }
        String canonical = ItemCatalogService.resolveId(itemId);
        if (canonical == null || canonical.isBlank()) {
            return rejected("unknown_item");
        }
        DetailedQuestProofEvent event = "craft".equals(kind)
                ? DetailedQuestProofEvent.itemCrafted(canonical)
                : DetailedQuestProofEvent.itemAcquired(canonical);
        return record(player, event);
    }

    /** Kill proof; the id and the attribution are server-observed facts. */
    public static Result recordEntityKilled(ServerPlayer player, String entityId) {
        if (player == null) {
            return rejected("missing_player");
        }
        String entity = normalize(entityId);
        if (entity.isBlank()) {
            return rejected("missing_entity");
        }
        return record(player, DetailedQuestProofEvent.entityKilled(entity));
    }

    /** Alive-capture proof; only the completed capture transaction may call this. */
    public static Result recordEntityCaptured(ServerPlayer player, String entityId) {
        if (player == null) {
            return rejected("missing_player");
        }
        String entity = normalize(entityId);
        if (entity.isBlank()) {
            return rejected("missing_entity");
        }
        return record(player, DetailedQuestProofEvent.entityCapturedAlive(entity));
    }

    /**
     * Encounter-clear proof for a secret-realm layer. Deep encounter regions are only producible
     * while the player holds a live session for the exact realm; ordinary encounter regions have
     * their own server producers.
     */
    public static Result recordEncounterCleared(ServerPlayer player, String realmId, String layer) {
        if (player == null) {
            return rejected("missing_player");
        }
        String realm = normalize(realmId);
        String phase = normalize(layer);
        if (realm.isBlank() || phase.isBlank()) {
            return rejected("missing_context");
        }
        List<String> regionIds = encounterRegionsForPhase(realm, phase);
        if (regionIds.isEmpty()) {
            return rejected("no_encounter_regions");
        }
        Optional<SecretRealmProgressSavedData.Session> session = SecretRealmSessionService.activeSession(player, realm);
        if (session.isEmpty()) {
            return rejected("no_active_session");
        }
        Result last = rejected("no_encounter_regions");
        boolean anyAccepted = false;
        int advancedTotal = 0;
        for (String regionId : regionIds) {
            DetailedQuestProofEvent event = DetailedQuestProofEvent.secretRealmEncounterCleared(
                    regionId, realm, session.get().sessionId(), phase);
            Result result = record(player, event);
            if (result.accepted()) {
                anyAccepted = true;
            }
            advancedTotal = Math.max(advancedTotal, result.advanced());
            last = result;
        }
        if (anyAccepted) {
            return new Result(Status.ACCEPTED, advancedTotal, "encounter_cleared");
        }
        return last;
    }

    /** Escort-completion proof; the region is the live region where the escort ended. */
    public static Result recordEscortCompleted(ServerPlayer player) {
        if (player == null) {
            return rejected("missing_player");
        }
        String live = normalize(CultivationHelper.get(player)
                .map(cultivation -> cultivation.getWorldpackCurrentRegionId()).orElse(""));
        if (live.isBlank()) {
            return rejected("missing_region");
        }
        return record(player, DetailedQuestProofEvent.escortCompleted(live));
    }

    /**
     * Records every Q-B-5 proof a server dialogue node visit may produce: the NPC dialogue
     * itself plus the authored acknowledgement/choice tokens bound to this (tree, node).
     */
    public static Result recordDialogueNode(ServerPlayer player, String npcId, String treeId, String nodeId) {
        if (player == null) {
            return rejected("missing_player");
        }
        Result last = rejected("no_dialogue_proofs");
        boolean anyAccepted = false;
        int advancedTotal = 0;
        if (!normalize(npcId).isBlank()) {
            Result result = record(player, DetailedQuestProofEvent.npcDialogue(npcId));
            if (result.accepted()) {
                anyAccepted = true;
            }
            advancedTotal = Math.max(advancedTotal, result.advanced());
            last = result;
        }
        String source = normalize(treeId) + ":" + normalize(nodeId);
        if (source.indexOf(':') > 0) {
            for (String choice : acknowledgedChoiceTokens(treeId, nodeId)) {
                Result result = record(player, DetailedQuestProofEvent.infoAcknowledged(choice));
                if (result.accepted()) {
                    anyAccepted = true;
                }
                advancedTotal = Math.max(advancedTotal, result.advanced());
                last = result;
            }
            for (String choice : committedChoiceTokens(treeId, nodeId)) {
                Result result = record(player, DetailedQuestProofEvent.choiceCommitted(choice));
                if (result.accepted()) {
                    anyAccepted = true;
                }
                advancedTotal = Math.max(advancedTotal, result.advanced());
                last = result;
            }
        }
        if (anyAccepted) {
            return new Result(Status.ACCEPTED, advancedTotal, "dialogue_proofs");
        }
        return last;
    }

    /** Shop-transaction proof after a successful server-authoritative purchase. */
    public static Result recordShopTransaction(ServerPlayer player, String shopId) {
        if (player == null) {
            return rejected("missing_player");
        }
        String shop = normalize(shopId);
        if (shop.isBlank()) {
            return rejected("missing_shop");
        }
        return record(player, DetailedQuestProofEvent.shopTransaction(shop));
    }

    /** Auction-transaction proof after a successful server-authoritative bid. */
    public static Result recordAuctionTransaction(ServerPlayer player, String venueId) {
        if (player == null) {
            return rejected("missing_player");
        }
        String venue = normalize(venueId);
        if (venue.isBlank()) {
            return rejected("missing_venue");
        }
        return record(player, DetailedQuestProofEvent.auctionTransaction(venue));
    }

    /** Reputation proof after the reputation ledger key reaches a positive value. */
    public static Result recordReputationReached(ServerPlayer player, String factionKey) {
        if (player == null) {
            return rejected("missing_player");
        }
        String faction = normalize(factionKey);
        if (faction.isBlank()) {
            return rejected("missing_faction");
        }
        return record(player, DetailedQuestProofEvent.reputationReached(faction));
    }

    /** Whether the delivering npc may legitimately accept the current step's delivery. */
    static boolean deliveryNpcMatches(DetailedQuestRuntimeService.Chain chain, int stage, String npcId) {
        if (chain == null || npcId == null || npcId.isBlank() || stage < 1 || stage > chain.steps().size()) {
            return false;
        }
        String npc = normalize(npcId);
        if (npc.equals(normalize(chain.giverNpc()))) {
            return true;
        }
        return DetailedQuestRuntimeService.placeTokens(chain.steps().get(stage - 1).place()).contains(npc);
    }

    /**
     * Ordinary region arrival after a successful server-authoritative transition. The live
     * capability region must equal the reported region; the event never trusts a client string.
     */
    public static Result recordRegionReached(ServerPlayer player, String regionId) {
        if (player == null) {
            return rejected("missing_player");
        }
        String region = normalize(regionId);
        if (region.isBlank()) {
            return rejected("missing_region");
        }
        String live = normalize(CultivationHelper.get(player)
                .map(cultivation -> cultivation.getWorldpackCurrentRegionId()).orElse(""));
        if (!region.equals(live)) {
            return rejected("region_mismatch");
        }
        return record(player, DetailedQuestProofEvent.regionEntered(region));
    }

    /**
     * Dimension arrival. Only the verified post-teleport dimension is recorded; the event
     * compares against {@code player.level().dimension()} again inside validation.
     */
    public static Result recordDimensionEntered(ServerPlayer player) {
        if (player == null) {
            return rejected("missing_player");
        }
        String dimension = normalize(player.level().dimension().location().toString());
        if (dimension.isBlank()) {
            return rejected("missing_dimension");
        }
        return record(player, DetailedQuestProofEvent.dimensionEntered(dimension));
    }

    /**
     * Secret-realm entry proof. Deep layer region ids may only be produced while the player holds
     * a live session for the exact realm; the session id is bound into the event.
     */
    public static Result recordSecretRealmEntry(ServerPlayer player, String realmId) {
        return recordSecretRealmPhase(player, realmId, "entry");
    }

    /** Secret-realm mid/core layer proof; {@code layer} accepts mid/core or the region token itself. */
    public static Result recordSecretRealmLayer(ServerPlayer player, String realmId, String layer) {
        String phase = normalize(layer);
        if ("water_jiao".equals(phase) || "bf_water_jiao".equals(phase)) {
            phase = "mid";
        }
        return recordSecretRealmPhase(player, realmId, phase);
    }

    /** Voluntary exit proof; timeout and death repatriation must never call this. */
    public static Result recordVoluntaryExit(ServerPlayer player, String realmId, String sessionId) {
        return recordSecretRealmPhase(player, realmId, "voluntary_exit", sessionId);
    }

    private static Result recordSecretRealmPhase(ServerPlayer player, String realmId, String phase) {
        return recordSecretRealmPhase(player, realmId, phase, null);
    }

    private static Result recordSecretRealmPhase(ServerPlayer player, String realmId, String phase,
                                                 String expectedSessionId) {
        if (player == null) {
            return rejected("missing_player");
        }
        String realm = normalize(realmId);
        String normalizedPhase = normalize(phase);
        if (realm.isBlank() || normalizedPhase.isBlank()) {
            return rejected("missing_context");
        }
        List<String> regionIds = regionIdsForPhase(realm, normalizedPhase);
        if (regionIds.isEmpty()) {
            return rejected("no_phase_regions");
        }
        Optional<SecretRealmProgressSavedData.Session> session = SecretRealmSessionService.activeSession(player, realm);
        if (session.isEmpty()
                || (expectedSessionId != null && !expectedSessionId.isBlank()
                && !expectedSessionId.equals(session.get().sessionId()))) {
            return rejected("no_active_session");
        }
        Result last = rejected("no_phase_regions");
        boolean anyAccepted = false;
        int advancedTotal = 0;
        for (String regionId : regionIds) {
            DetailedQuestProofEvent event = DetailedQuestProofEvent.secretRealmLayerEntered(
                    regionId, realm, session.get().sessionId(), normalizedPhase);
            Result result = record(player, event);
            if (result.accepted()) {
                anyAccepted = true;
            }
            advancedTotal = Math.max(advancedTotal, result.advanced());
            last = result;
        }
        if (anyAccepted) {
            return new Result(Status.ACCEPTED, advancedTotal, "secret_realm_proof");
        }
        return last;
    }

    /**
     * Records a structure-formation proof only after the structure was really formed and formally
     * commissioned (INTACT) at the exact dimension/origin.
     */
    public static Result recordStructureFormed(ServerPlayer player, String stationId,
                                               String dimensionId, long packedPosition) {
        if (player == null || stationId == null || stationId.isBlank()
                || dimensionId == null || dimensionId.isBlank()) {
            return rejected("missing_structure_context");
        }
        return record(player, DetailedQuestProofEvent.structureFormed(stationId, dimensionId, packedPosition));
    }

    /** True when the proof ledger records a region proof for the exact region id. */
    public static boolean hasRegionProof(ServerPlayer player, String regionId) {
        if (player == null) {
            return false;
        }
        String suffix = "|region:" + normalize(regionId);
        CompoundTag ledger = player.getPersistentData().getCompound(LEDGER_TAG);
        for (String key : ledger.getAllKeys()) {
            if (key.endsWith(suffix)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Replays only durable, route-authorized facts. Cultivation facts are reconstructed from the
     * current capability to support old saves; region/dimension facts replay only server-recorded
     * history, and structure facts re-verify the original dimension/origin is still formed and
     * commissioned. No arbitrary client token is accepted.
     */
    public static int replayCurrent(ServerPlayer player) {
        if (player == null) {
            return 0;
        }
        int advanced = 0;
        for (DetailedQuestRuntimeService.Progress progress : DetailedQuestRuntimeService.listProgress(player)) {
            if (!progress.started() || progress.complete()) {
                continue;
            }
            DetailedQuestProofCatalog.Route route = DetailedQuestRuntimeService.proofCatalog()
                    .find(progress.id(), progress.stage());
            if (route == null || !route.allowHistoryReplay()) {
                continue;
            }
            DetailedQuestProofEvent event = historicalEventFor(player, route);
            if (event == null || !authoritativeState(player, route, event)
                    || hasLedger(player, ledgerKey(route, event))) {
                continue;
            }
            boolean hasFact = hasHistory(player, route.eventId())
                    || isCultivationRoute(route) || "spirit_root".equals(route.producer());
            if (!hasFact) {
                continue;
            }
            if (DetailedQuestRuntimeService.advanceVerifiedRoute(player, route.chainId(), route,
                    DetailedQuestRuntimeService.Evidence.of(route.eventId(), event.eventKey()))) {
                writeLedger(player, ledgerKey(route, event), event.asHistory(), false);
                advanced++;
            }
        }
        return advanced;
    }

    /** Permission-2 recovery path. It is deliberately separate from natural event recording. */
    public static Result adminProve(ServerPlayer player, String chainId, int step) {
        if (player == null) {
            return rejected("missing_player");
        }
        if (!player.hasPermissions(2)) {
            return rejected("permission_denied");
        }
        DetailedQuestProofCatalog.Route route = DetailedQuestRuntimeService.proofCatalog().find(chainId, step);
        if (route == null) {
            return rejected("unknown_route");
        }
        DetailedQuestRuntimeService.Progress progress =
                DetailedQuestRuntimeService.progressOf(player, route.chainId());
        if (!progress.started() || progress.complete() || progress.stage() != route.step()) {
            return rejected("wrong_current_step");
        }
        DetailedQuestProofEvent event = DetailedQuestProofEvent.of(
                DetailedQuestProofEvent.Type.valueOf(route.proofType()), route.producer(),
                route.requiredParams(), "admin:" + route.eventId()).forOwner(player.getUUID()).asAdmin();
        if (!DetailedQuestRuntimeService.advanceVerifiedRoute(player, route.chainId(), route,
                DetailedQuestRuntimeService.Evidence.of(route.eventId()))) {
            return rejected("admin_advance_failed");
        }
        writeLedger(player, ledgerKey(route, event), event, true);
        return new Result(Status.ACCEPTED, 1, "admin_proof");
    }

    public static int ledgerEntryCount(ServerPlayer player) {
        return player == null ? 0 : player.getPersistentData().getCompound(LEDGER_TAG).getAllKeys().size();
    }

    public static void copyPersistentData(CompoundTag source, CompoundTag target) {
        if (source == null || target == null) {
            return;
        }
        copyTag(source, target, LEDGER_TAG);
        copyTag(source, target, HISTORY_TAG);
    }

    private static List<DetailedQuestProofCatalog.Route> matchingRoutes(DetailedQuestProofEvent event) {
        List<DetailedQuestProofCatalog.Route> result = new ArrayList<>();
        for (DetailedQuestProofCatalog.Route route : DetailedQuestRuntimeService.proofCatalog().routes()) {
            if (!route.proofType().equals(event.type().name()) || !route.producer().equals(event.producer())) {
                continue;
            }
            if (!paramsMatch(route, event)) {
                continue;
            }
            result.add(route);
        }
        return result;
    }

    /**
     * Route parameters normally match exactly. Item routes additionally accept any canonical
     * item that is a declared prover of the authored route token (e.g. acquired
     * {@code water_pearl} proves the {@code jiao_pearl} token), so alias and concept tokens stay
     * honest without trusting client strings.
     */
    private static boolean paramsMatch(DetailedQuestProofCatalog.Route route,
                                       DetailedQuestProofEvent event) {
        if (route.requiredParams().equals(event.parameters())) {
            return true;
        }
        if (isItemRoute(route.proofType())) {
            String key = route.requiredParams().keySet().iterator().next();
            return routeItemMatches(route.requiredParams().get(key), event.parameters().get(key));
        }
        if (isEntityRoute(route.proofType())) {
            String key = route.requiredParams().keySet().iterator().next();
            return entityTokenMatches(route.requiredParams().get(key), event.parameters().get(key));
        }
        if ("NPC_DIALOGUE".equals(route.proofType())) {
            return npcTokenMatches(route.parameter("npc"), event.parameter("npc"));
        }
        if ("SHOP_TRANSACTION".equals(route.proofType())) {
            return shopTokenMatches(route.parameter("shop"), event.parameter("shop"));
        }
        if ("AUCTION_TRANSACTION".equals(route.proofType())) {
            return auctionTokenMatches(route.parameter("auction"), event.parameter("auction"));
        }
        if ("REPUTATION_REACHED".equals(route.proofType())) {
            return factionTokenMatches(route.parameter("faction"), event.parameter("faction"));
        }
        return false;
    }

    private static boolean isEntityRoute(String proofType) {
        return "ENTITY_KILLED".equals(proofType) || "ENTITY_CAPTURED_ALIVE".equals(proofType);
    }

    /** Pure rule shared by validation and tests: can the event entity id prove the route token? */
    static boolean entityTokenMatches(String routeEntityToken, String eventEntityId) {
        if (routeEntityToken == null || eventEntityId == null) {
            return false;
        }
        String normalized = normalize(eventEntityId);
        return !normalized.isBlank() && entitiesProvingToken(routeEntityToken).contains(normalized);
    }

    /** Server entity/boss ids whose kill/capture proves the route token (identity always included). */
    static Set<String> entitiesProvingToken(String routeEntityToken) {
        String token = normalize(routeEntityToken);
        if (token.isBlank()) {
            return Set.of();
        }
        LinkedHashSet<String> result = new LinkedHashSet<>(PROOF_ENTITY_MAPPINGS.getOrDefault(token, Set.of()));
        result.add(token);
        return Set.copyOf(result);
    }

    /** Pure rule: can the server npc id prove the route npc token? */
    static boolean npcTokenMatches(String routeNpcToken, String eventNpcId) {
        return tokenInSet(routeNpcToken, eventNpcId, PROOF_NPC_MAPPINGS);
    }

    /** Pure rule: can the server market shop id prove the route shop token? */
    static boolean shopTokenMatches(String routeShopToken, String eventShopId) {
        return tokenInSet(routeShopToken, eventShopId, PROOF_SHOP_MAPPINGS);
    }

    /** Pure rule: can the server auction venue id prove the route auction token? */
    static boolean auctionTokenMatches(String routeAuctionToken, String eventAuctionId) {
        return tokenInSet(routeAuctionToken, eventAuctionId, PROOF_AUCTION_MAPPINGS);
    }

    /** Pure rule: can the server reputation key prove the route faction token? */
    static boolean factionTokenMatches(String routeFactionToken, String eventFactionKey) {
        return tokenInSet(routeFactionToken, eventFactionKey, PROOF_FACTION_MAPPINGS);
    }

    private static boolean tokenInSet(String routeToken, String eventValue,
                                      Map<String, Set<String>> mappings) {
        if (routeToken == null || eventValue == null) {
            return false;
        }
        String normalized = normalize(eventValue);
        if (normalized.isBlank()) {
            return false;
        }
        LinkedHashSet<String> provable = new LinkedHashSet<>(
                mappings.getOrDefault(normalize(routeToken), Set.of()));
        provable.add(normalize(routeToken));
        return provable.contains(normalized);
    }

    /** Authored choice tokens a server dialogue node visit acknowledges. */
    static Set<String> acknowledgedChoiceTokens(String treeId, String nodeId) {
        String source = normalize(treeId) + ":" + normalize(nodeId);
        if (source.indexOf(':') <= 0) {
            return Set.of();
        }
        LinkedHashSet<String> choices = new LinkedHashSet<>();
        for (Map.Entry<String, Set<String>> entry : INFO_CHOICE_SOURCES.entrySet()) {
            if (entry.getValue().contains(source)) {
                choices.add(entry.getKey());
            }
        }
        return Set.copyOf(choices);
    }

    /** Authored choice tokens a server dialogue node visit commits. */
    static Set<String> committedChoiceTokens(String treeId, String nodeId) {
        String source = normalize(treeId) + ":" + normalize(nodeId);
        if (source.indexOf(':') <= 0) {
            return Set.of();
        }
        LinkedHashSet<String> choices = new LinkedHashSet<>();
        for (Map.Entry<String, Set<String>> entry : CHOICE_COMMITTED_SOURCES.entrySet()) {
            if (entry.getValue().contains(source)) {
                choices.add(entry.getKey());
            }
        }
        return Set.copyOf(choices);
    }

    /** Pure rule shared by validation and tests: can the acquired/delivered item prove the token? */
    static boolean routeItemMatches(String routeItemToken, String eventItemId) {
        if (routeItemToken == null || eventItemId == null) {
            return false;
        }
        String normalized = normalize(eventItemId);
        if (normalized.isBlank() || itemsProvingToken(routeItemToken).isEmpty()) {
            return false;
        }
        if (itemsProvingToken(routeItemToken).contains(normalized)) {
            return true;
        }
        String canonical = ItemCatalogService.resolveId(normalized);
        return canonical != null && itemsProvingToken(routeItemToken).contains(canonical);
    }

    /** Reputation ledger keys that prove the route faction token (identity always included). */
    static Set<String> factionsProvingToken(String routeFactionToken) {
        String token = normalize(routeFactionToken);
        if (token.isBlank()) {
            return Set.of();
        }
        LinkedHashSet<String> result = new LinkedHashSet<>(
                PROOF_FACTION_MAPPINGS.getOrDefault(token, Set.of()));
        result.add(token);
        return Set.copyOf(result);
    }

    private static boolean isItemRoute(String proofType) {
        return "ITEM_ACQUIRED".equals(proofType) || "CRAFT_COMPLETED".equals(proofType)
                || "ITEM_DELIVERED".equals(proofType);
    }

    /** Canonical items whose server-observed acquisition/craft/delivery proves the route token. */
    static Set<String> itemsProvingToken(String routeItemToken) {
        String token = normalize(routeItemToken);
        if (token.isBlank()) {
            return Set.of();
        }
        LinkedHashSet<String> result = new LinkedHashSet<>(PROOF_ITEM_MAPPINGS.getOrDefault(token, Set.of()));
        String canonical = ItemCatalogService.resolveId(token);
        if (canonical != null && !canonical.isBlank() && isRealCarrier(canonical)) {
            result.add(canonical);
        }
        return Set.copyOf(result);
    }

    /** Bulk carriers plus dedicated ModItems registrations that route tokens may resolve to. */
    private static boolean isRealCarrier(String canonicalId) {
        return ItemCatalogService.findMeta(canonicalId).isPresent()
                || REGISTERED_PROOF_CARRIERS.contains(normalize(canonicalId));
    }

    private static final Set<String> REGISTERED_PROOF_CARRIERS = Set.of(
            "yin_body_protection_charm", "fire_talisman", "speed_talisman");

    /** True when the player holds any item whose canonical id proves the route token. */
    static boolean holdsProvingItem(ServerPlayer player, String routeItemToken) {
        return heldProvingItem(player, routeItemToken) != null;
    }

    /** Returns the canonical id of a held item that proves the token, or null. */
    private static String heldProvingItem(ServerPlayer player, String routeItemToken) {
        if (player == null) {
            return null;
        }
        Set<String> proving = itemsProvingToken(routeItemToken);
        if (proving.isEmpty()) {
            return null;
        }
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            var stack = player.getInventory().getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            String path = itemPath(stack);
            if (path.isBlank()) {
                continue;
            }
            String canonical = ItemCatalogService.resolveId(path);
            if (canonical != null && proving.contains(canonical)) {
                return canonical;
            }
        }
        return null;
    }

    private static String itemPath(ItemStack stack) {
        try {
            var key = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem());
            return key == null ? "" : normalize(key.getPath());
        } catch (Throwable ignored) {
            return "";
        }
    }

    private static boolean authoritativeState(ServerPlayer player,
                                               DetailedQuestProofCatalog.Route route,
                                               DetailedQuestProofEvent event) {
        return switch (route.proofType()) {
            case "METHOD_LAYER_REACHED" -> {
                String method = route.parameter("method");
                int actual = ManualCatalogService.getMethodLayer(player, method);
                boolean realmOk = route.minimumRealm().isBlank()
                        || CultivationHelper.get(player).map(c -> c.getRealm().ordinal()
                        >= Realm.fromDesignId(route.minimumRealm()).ordinal()).orElse(false);
                yield actual >= route.minimumLayer() && realmOk;
            }
            case "REALM_REACHED" -> {
                Realm expected = Realm.fromDesignId(route.parameter("realm"));
                yield expected != null && CultivationHelper.get(player)
                        .map(c -> !c.isTribulationActive() && c.getRealm().ordinal() >= expected.ordinal())
                        .orElse(false);
            }
            case "TECHNIQUE_LEARNED" -> CultivationHelper.get(player)
                    .map(c -> c.hasLearnedTechnique(route.parameter("technique"))).orElse(false);
            case "SPIRIT_ROOT_TESTED" -> CultivationHelper.get(player)
                    .map(PlayerCultivation::isSpiritualRootTested).orElse(false);
            case "REGION_ENTER" -> regionProofAuthoritative(player, route, event);
            case "DIMENSION_ENTER" -> dimensionProofAuthoritative(player, route, event);
            case "STRUCTURE_FORMED" -> structureProofAuthoritative(player, route, event);
            case "ITEM_ACQUIRED", "CRAFT_COMPLETED", "ITEM_DELIVERED" -> {
                // Replay uses the server-recorded history fact; natural events require the
                // player to actually hold an item proving the route token right now.
                if (event.source() == DetailedQuestProofEvent.Source.HISTORY) {
                    yield !event.parameter("item").isBlank();
                }
                yield holdsProvingItem(player, route.parameter("item"));
            }
            case "ENTITY_KILLED", "ENTITY_CAPTURED_ALIVE" -> {
                // The entity id and the kill/capture attribution are server-observed facts.
                if (event.source() == DetailedQuestProofEvent.Source.HISTORY) {
                    yield !event.parameter("entity").isBlank();
                }
                yield entityTokenMatches(route.parameter("entity"), event.parameter("entity"));
            }
            case "ENCOUNTER_CLEARED" -> encounterProofAuthoritative(player, route, event);
            case "ESCORT_COMPLETED" -> {
                if (event.source() == DetailedQuestProofEvent.Source.HISTORY) {
                    yield !event.parameter("region").isBlank();
                }
                String live = normalize(CultivationHelper.get(player)
                        .map(cultivation -> cultivation.getWorldpackCurrentRegionId()).orElse(""));
                yield !live.isBlank() && event.currentRegionId().equals(live)
                        && trustedRegionAliases(live).contains(route.parameter("region"));
            }
            case "NPC_DIALOGUE" -> {
                if (event.source() == DetailedQuestProofEvent.Source.HISTORY) {
                    yield !event.parameter("npc").isBlank();
                }
                yield npcTokenMatches(route.parameter("npc"), event.parameter("npc"));
            }
            case "INFO_ACKNOWLEDGED", "CHOICE_COMMITTED" -> {
                // The choice token is produced by the server dialogue mapping; only the exact
                // authored token may match the route.
                if (event.source() == DetailedQuestProofEvent.Source.HISTORY) {
                    yield !event.parameter("choice").isBlank();
                }
                yield route.parameter("choice").equals(event.parameter("choice"));
            }
            case "SHOP_TRANSACTION" -> {
                if (event.source() == DetailedQuestProofEvent.Source.HISTORY) {
                    yield !event.parameter("shop").isBlank();
                }
                yield shopTokenMatches(route.parameter("shop"), event.parameter("shop"));
            }
            case "AUCTION_TRANSACTION" -> {
                if (event.source() == DetailedQuestProofEvent.Source.HISTORY) {
                    yield !event.parameter("auction").isBlank();
                }
                yield auctionTokenMatches(route.parameter("auction"), event.parameter("auction"));
            }
            case "REPUTATION_REACHED" -> {
                if (event.source() == DetailedQuestProofEvent.Source.HISTORY) {
                    yield !event.parameter("faction").isBlank();
                }
                // The live reputation ledger is the server truth; any positive value for a key
                // that proves the route faction counts as reached.
                yield factionsProvingToken(route.parameter("faction")).stream()
                        .anyMatch(faction -> com.xunxian.seekingimmortals.worldpack.ReputationService
                                .get(player, faction) >= 1);
            }
            case "ALCHEMY_COMPLETED" -> {
                if (event.parameter("station").isBlank()) {
                    yield false;
                }
                String expected = ItemCatalogService.resolveId(route.parameter("station"));
                String actual = ItemCatalogService.resolveId(event.parameter("station"));
                yield expected != null && expected.equals(actual);
            }
            default -> true;
        };
    }

    /**
     * Region facts are only accepted from the live region or a bound secret-realm session.
     * Deep ids (realm layers) can never be forged through the ordinary region path.
     */
    private static boolean regionProofAuthoritative(ServerPlayer player,
                                                    DetailedQuestProofCatalog.Route route,
                                                    DetailedQuestProofEvent event) {
        String routeRegion = route.parameter("region");
        if (event.source() == DetailedQuestProofEvent.Source.HISTORY) {
            return !event.currentRegionId().isBlank() || !event.secretRealmId().isBlank();
        }
        if (event.secretRealmId().isBlank()) {
            String live = normalize(CultivationHelper.get(player)
                    .map(cultivation -> cultivation.getWorldpackCurrentRegionId()).orElse(""));
            if (event.currentRegionId().isBlank() || !event.currentRegionId().equals(live)) {
                return false;
            }
            return trustedRegionAliases(event.currentRegionId()).contains(routeRegion);
        }
        return isSecretRealmRegion(routeRegion)
                && regionIdsForPhase(event.secretRealmId(), event.phase()).contains(routeRegion)
                && SecretRealmSessionService.activeSession(player, event.secretRealmId())
                .map(session -> session.sessionId().equals(event.sessionId()))
                .orElse(false);
    }

    /**
     * Encounter clears are only accepted from a bound secret-realm layer or the live ordinary
     * region. Deep encounter regions can never be forged through the ordinary path.
     */
    private static boolean encounterProofAuthoritative(ServerPlayer player,
                                                       DetailedQuestProofCatalog.Route route,
                                                       DetailedQuestProofEvent event) {
        if (event.source() == DetailedQuestProofEvent.Source.HISTORY) {
            return !event.parameter("region").isBlank();
        }
        if (event.secretRealmId().isBlank()) {
            String live = normalize(CultivationHelper.get(player)
                    .map(cultivation -> cultivation.getWorldpackCurrentRegionId()).orElse(""));
            return !live.isBlank() && event.currentRegionId().equals(live)
                    && trustedRegionAliases(live).contains(route.parameter("region"));
        }
        return encounterRegionsForPhase(event.secretRealmId(), event.phase())
                .contains(route.parameter("region"))
                && SecretRealmSessionService.activeSession(player, event.secretRealmId())
                .map(session -> session.sessionId().equals(event.sessionId()))
                .orElse(false);
    }

    /**
     * Trusted secret-realm layer -> encounter-clear region ids. Regions without a realm-phase
     * source (wuxing_shallow_trial, gray_realm_border, heifeng_sea) have no producer yet and
     * stay fail-closed until their content layer exists.
     */
    static List<String> encounterRegionsForPhase(String realmId, String phase) {
        String key = normalize(realmId) + ":" + normalize(phase);
        return switch (key) {
            case "blood_forbidden:mid" -> List.of("bf_water_jiao");
            case "fallen_demon_valley:core" -> List.of("zm_candle");
            case "thousand_bamboo_puppet_tower:mid" -> List.of("qz_l2");
            case "thousand_bamboo_puppet_tower:core" -> List.of("qz_l3");
            case "yinyang_ku:mid" -> List.of("yy_yezha");
            case "guanghan_realm:core" -> List.of("gh_inner");
            default -> List.of();
        };
    }

    private static boolean dimensionProofAuthoritative(ServerPlayer player,
                                                       DetailedQuestProofCatalog.Route route,
                                                       DetailedQuestProofEvent event) {
        if (event.dimensionId().isBlank()) {
            return false;
        }
        if (event.source() == DetailedQuestProofEvent.Source.HISTORY) {
            return trustedDimensionAliases(dimensionPath(event.dimensionId()))
                    .contains(route.parameter("dimension"));
        }
        String actual = normalize(player.level().dimension().location().toString());
        if (!actual.equals(event.dimensionId())) {
            return false;
        }
        return trustedDimensionAliases(dimensionPath(event.dimensionId()))
                .contains(route.parameter("dimension"));
    }

    /**
     * Structure proofs require the catalog entry, a real formed shell and a formally commissioned
     * (non-disabled) operational state at the exact recorded dimension/origin. A weak
     * {@code single_core:any_solid} match alone is never enough because the commissioned state
     * only exists after a completed {@code form()} transaction.
     */
    private static boolean structureProofAuthoritative(ServerPlayer player,
                                                       DetailedQuestProofCatalog.Route route,
                                                       DetailedQuestProofEvent event) {
        if (!event.hasPosition() || event.dimensionId().isBlank()
                || MultiblockStructureCatalog.builtin().find(route.parameter("structure")).isEmpty()) {
            return false;
        }
        if (event.source() != DetailedQuestProofEvent.Source.HISTORY
                && !normalize(player.level().dimension().location().toString()).equals(event.dimensionId())) {
            return false;
        }
        ServerLevel level = levelFor(player, event.dimensionId());
        if (level == null) {
            return false;
        }
        BlockPos origin = BlockPos.of(event.packedPosition());
        return MultiblockStationService.isStationFormed(level, route.parameter("structure"), origin)
                && MultiblockOperationalService.isCommissioned(level, route.parameter("structure"), origin);
    }

    private static DetailedQuestProofEvent historicalEventFor(ServerPlayer player,
                                                               DetailedQuestProofCatalog.Route route) {
        CompoundTag history = player.getPersistentData().getCompound(HISTORY_TAG);
        DetailedQuestProofEvent fromHistory = eventFromHistory(route, history);
        if (fromHistory != null) {
            return fromHistory;
        }
        return switch (route.proofType()) {
            case "METHOD_LAYER_REACHED" -> DetailedQuestProofEvent.methodLayerReached(
                    route.parameter("method"), ManualCatalogService.getMethodLayer(player, route.parameter("method")));
            case "REALM_REACHED" -> {
                Realm realm = Realm.fromDesignId(route.parameter("realm"));
                yield realm == null ? null : DetailedQuestProofEvent.realmReached(realm);
            }
            case "TECHNIQUE_LEARNED" -> DetailedQuestProofEvent.techniqueLearned(route.parameter("technique"));
            case "SPIRIT_ROOT_TESTED" -> DetailedQuestProofEvent.spiritualRootTested();
            default -> null;
        };
    }

    private static boolean isCultivationRoute(DetailedQuestProofCatalog.Route route) {
        return "cultivation".equals(route.producer());
    }

    static String ledgerKey(DetailedQuestProofCatalog.Route route, DetailedQuestProofEvent event) {
        String eventKey = event.eventKey().isBlank() ? "event" : event.eventKey();
        return route.eventId() + "|" + eventKey;
    }

    private static boolean hasLedger(ServerPlayer player, String key) {
        return player.getPersistentData().getCompound(LEDGER_TAG).contains(key);
    }

    private static boolean hasHistory(ServerPlayer player, String routeEventId) {
        return hasHistoryEntry(player.getPersistentData().getCompound(HISTORY_TAG), routeEventId);
    }

    private static boolean storeHistory(ServerPlayer player, DetailedQuestProofCatalog.Route route,
                                        DetailedQuestProofEvent event) {
        CompoundTag history = player.getPersistentData().getCompound(HISTORY_TAG).copy();
        if (hasHistoryEntry(history, route.eventId())) {
            return false;
        }
        if (history.getAllKeys().size() >= MAX_LEDGER_ENTRIES) {
            return false;
        }
        history.put(route.eventId(), historyEntry(route, event));
        player.getPersistentData().put(HISTORY_TAG, history);
        return true;
    }

    /** Serializes one history record with event type, key, layer and trusted world context. */
    static CompoundTag historyEntry(DetailedQuestProofCatalog.Route route, DetailedQuestProofEvent event) {
        CompoundTag entry = new CompoundTag();
        entry.putString("Type", event.type().name());
        entry.putString("EventKey", event.eventKey());
        entry.putInt("Layer", event.observedLayer());
        if (!event.dimensionId().isBlank()) {
            entry.putString("Dimension", event.dimensionId());
        }
        if (!event.currentRegionId().isBlank()) {
            entry.putString("Region", event.currentRegionId());
        }
        if (!event.secretRealmId().isBlank()) {
            entry.putString("SecretRealm", event.secretRealmId());
        }
        if (!event.sessionId().isBlank()) {
            entry.putString("Session", event.sessionId());
        }
        if (!event.phase().isBlank()) {
            entry.putString("Phase", event.phase());
        }
        if (!event.authorityId().isBlank()) {
            entry.putString("Authority", event.authorityId());
        }
        if (event.hasPosition()) {
            entry.putLong("Pos", event.packedPosition());
        }
        if (isItemRoute(event.type().name()) && !event.parameter("item").isBlank()) {
            entry.putString("Item", event.parameter("item"));
        }
        if ("ALCHEMY_COMPLETED".equals(event.type().name()) && !event.parameter("station").isBlank()) {
            entry.putString("Station", event.parameter("station"));
        }
        if (isEntityRoute(event.type().name()) && !event.parameter("entity").isBlank()) {
            entry.putString("Entity", event.parameter("entity"));
        }
        if (("ENCOUNTER_CLEARED".equals(event.type().name()) || "ESCORT_COMPLETED".equals(event.type().name()))
                && !event.parameter("region").isBlank()) {
            entry.putString("Region", event.parameter("region"));
        }
        if ("NPC_DIALOGUE".equals(event.type().name()) && !event.parameter("npc").isBlank()) {
            entry.putString("Npc", event.parameter("npc"));
        }
        if (("INFO_ACKNOWLEDGED".equals(event.type().name()) || "CHOICE_COMMITTED".equals(event.type().name()))
                && !event.parameter("choice").isBlank()) {
            entry.putString("Choice", event.parameter("choice"));
        }
        if ("SHOP_TRANSACTION".equals(event.type().name()) && !event.parameter("shop").isBlank()) {
            entry.putString("Shop", event.parameter("shop"));
        }
        if ("AUCTION_TRANSACTION".equals(event.type().name()) && !event.parameter("auction").isBlank()) {
            entry.putString("Auction", event.parameter("auction"));
        }
        if ("REPUTATION_REACHED".equals(event.type().name()) && !event.parameter("faction").isBlank()) {
            entry.putString("Faction", event.parameter("faction"));
        }
        return entry;
    }

    /** Accepts both legacy boolean facts and the structured compound records. */
    static boolean hasHistoryEntry(CompoundTag history, String eventId) {
        if (history == null || eventId == null || eventId.isBlank() || !history.contains(eventId)) {
            return false;
        }
        Tag value = history.get(eventId);
        if (value instanceof CompoundTag tag) {
            return !tag.getString("Type").isBlank();
        }
        return value instanceof net.minecraft.nbt.ByteTag || value instanceof net.minecraft.nbt.NumericTag;
    }

    /** Reconstructs a replay event from a stored history record; null when absent. */
    static DetailedQuestProofEvent eventFromHistory(DetailedQuestProofCatalog.Route route,
                                                    CompoundTag history) {
        if (route == null || history == null) {
            return null;
        }
        Tag value = history.get(route.eventId());
        if (!(value instanceof CompoundTag tag) || tag.getString("Type").isBlank()) {
            return null;
        }
        DetailedQuestProofEvent base = switch (route.proofType()) {
            case "REGION_ENTER" -> DetailedQuestProofEvent.regionEntered(route.parameter("region"));
            case "DIMENSION_ENTER" -> DetailedQuestProofEvent.dimensionEntered(tag.getString("Dimension"));
            case "STRUCTURE_FORMED" -> DetailedQuestProofEvent.structureFormed(
                    route.parameter("structure"), tag.getString("Dimension"), tag.getLong("Pos"));
            case "ITEM_ACQUIRED" -> DetailedQuestProofEvent.itemAcquired(tag.getString("Item"));
            case "CRAFT_COMPLETED" -> DetailedQuestProofEvent.itemCrafted(tag.getString("Item"));
            case "ITEM_DELIVERED" -> DetailedQuestProofEvent.itemDelivered(tag.getString("Item"));
            case "ALCHEMY_COMPLETED" -> DetailedQuestProofEvent.alchemyCompleted(tag.getString("Station"));
            case "ENTITY_KILLED" -> DetailedQuestProofEvent.entityKilled(tag.getString("Entity"));
            case "ENTITY_CAPTURED_ALIVE" -> DetailedQuestProofEvent.entityCapturedAlive(tag.getString("Entity"));
            case "ENCOUNTER_CLEARED" -> DetailedQuestProofEvent.encounterCleared(
                    tag.getString("Region").isBlank() ? route.parameter("region") : tag.getString("Region"));
            case "ESCORT_COMPLETED" -> DetailedQuestProofEvent.escortCompleted(
                    tag.getString("Region").isBlank() ? route.parameter("region") : tag.getString("Region"));
            case "NPC_DIALOGUE" -> DetailedQuestProofEvent.npcDialogue(tag.getString("Npc"));
            case "INFO_ACKNOWLEDGED" -> DetailedQuestProofEvent.infoAcknowledged(tag.getString("Choice"));
            case "CHOICE_COMMITTED" -> DetailedQuestProofEvent.choiceCommitted(tag.getString("Choice"));
            case "SHOP_TRANSACTION" -> DetailedQuestProofEvent.shopTransaction(tag.getString("Shop"));
            case "AUCTION_TRANSACTION" -> DetailedQuestProofEvent.auctionTransaction(tag.getString("Auction"));
            case "REPUTATION_REACHED" -> DetailedQuestProofEvent.reputationReached(tag.getString("Faction"));
            case "METHOD_LAYER_REACHED" -> DetailedQuestProofEvent.methodLayerReached(
                    route.parameter("method"), Math.max(1, tag.getInt("Layer")));
            case "REALM_REACHED" -> {
                Realm realm = Realm.fromDesignId(route.parameter("realm"));
                yield realm == null ? null : DetailedQuestProofEvent.realmReached(realm);
            }
            case "TECHNIQUE_LEARNED" -> DetailedQuestProofEvent.techniqueLearned(route.parameter("technique"));
            case "SPIRIT_ROOT_TESTED" -> DetailedQuestProofEvent.spiritualRootTested();
            default -> null;
        };
        if (base == null) {
            return null;
        }
        return base.withWorld(tag.getString("Dimension"), tag.getString("Region"),
                tag.getString("SecretRealm"), tag.getString("Session"), tag.getString("Phase"),
                tag.getString("Authority"), tag.getLong("Pos"), tag.contains("Pos")).asHistory();
    }

    private static void writeLedger(ServerPlayer player, String key, DetailedQuestProofEvent event,
                                    boolean admin) {
        CompoundTag ledger = player.getPersistentData().getCompound(LEDGER_TAG).copy();
        if (ledger.contains(key)) {
            return;
        }
        if (ledger.getAllKeys().size() >= MAX_LEDGER_ENTRIES) {
            String oldest = ledger.getAllKeys().stream().sorted().findFirst().orElse("");
            if (!oldest.isBlank()) {
                ledger.remove(oldest);
            }
        }
        CompoundTag entry = new CompoundTag();
        entry.putString("EventKey", event.eventKey());
        entry.putString("Source", admin ? DetailedQuestProofEvent.Source.ADMIN.name() : event.source().name());
        entry.putBoolean("Admin", admin);
        if (!event.dimensionId().isBlank()) {
            entry.putString("Dimension", event.dimensionId());
        }
        if (!event.secretRealmId().isBlank()) {
            entry.putString("SecretRealm", event.secretRealmId());
        }
        if (!event.sessionId().isBlank()) {
            entry.putString("Session", event.sessionId());
        }
        ledger.put(key, entry);
        player.getPersistentData().put(LEDGER_TAG, ledger);
    }

    /** Trusted secret-realm phase -> proof region ids. Deep ids never come from ordinary travel. */
    static List<String> regionIdsForPhase(String realmId, String phase) {
        String key = normalize(realmId) + ":" + normalize(phase);
        return switch (key) {
            case "blood_forbidden:entry" -> List.of("blood_forbidden", "bf_outer_mist");
            case "blood_forbidden:mid" -> List.of("bf_water_jiao");
            case "blood_forbidden:voluntary_exit" -> List.of("blood_forbidden_exit_array");
            case "void_palace:entry" -> List.of("island_xutian_window");
            case "kunwu_mountain:entry" -> List.of("dajin_kunwu_approach");
            case "fallen_demon_valley:entry" -> List.of("fallen_demon_rift");
            case "fallen_demon_valley:mid" -> List.of("zm_inner");
            case "fallen_demon_valley:core" -> List.of("zm_candle");
            case "yinyang_ku:entry" -> List.of("yinyang_cave_gate");
            case "guanghan_realm:entry" -> List.of("gh_approach");
            default -> List.of();
        };
    }

    /**
     * Ordinary region ids a player may legitimately prove after arriving at the given live
     * region. huangfeng_outer is only reachable through a real tiannan arrival.
     */
    static Set<String> trustedRegionAliases(String currentRegionId) {
        return switch (normalize(currentRegionId)) {
            case "tiannan" -> Set.of("tiannan", "huangfeng_outer");
            case "chaotic_sea" -> Set.of("chaotic_sea");
            case "huangfeng_outer" -> Set.of("huangfeng_outer");
            default -> {
                String region = normalize(currentRegionId);
                yield region.isBlank() ? Set.of() : Set.of(region);
            }
        };
    }

    /** Strict dimension alias mapping: actual dimension path -> provable route dimension ids. */
    static Set<String> trustedDimensionAliases(String dimensionPath) {
        return switch (normalize(dimensionPath)) {
            case "tianyuan" -> Set.of("tianyuan", "spirit_realm");
            case "spirit_fengyuan" -> Set.of("fengyuan", "spirit_realm");
            case "secret_realm_thousand_bamboo_puppet_tower" -> Set.of("qianzhu_tower");
            default -> {
                String path = normalize(dimensionPath);
                yield path.isBlank() ? Set.of() : Set.of(path);
            }
        };
    }

    static boolean isSecretRealmRegion(String regionId) {
        return SECRET_REALM_REGIONS.contains(normalize(regionId));
    }

    /** Pure routing rule shared by validation and tests: can the event context prove the region? */
    static boolean regionProofMatchesContext(String routeRegion, DetailedQuestProofEvent event) {
        String region = normalize(routeRegion);
        if (isSecretRealmRegion(region)) {
            return !event.secretRealmId().isBlank()
                    && regionIdsForPhase(event.secretRealmId(), event.phase()).contains(region);
        }
        return !event.currentRegionId().isBlank()
                && trustedRegionAliases(event.currentRegionId()).contains(region);
    }

    private static ServerLevel levelFor(ServerPlayer player, String dimensionId) {
        if (player == null || player.server == null || dimensionId == null || dimensionId.isBlank()) {
            return null;
        }
        ResourceLocation location = ResourceLocation.tryParse(dimensionId);
        if (location == null) {
            return null;
        }
        return player.server.getLevel(ResourceKey.create(Registries.DIMENSION, location));
    }

    private static String dimensionPath(String dimensionId) {
        String normalized = normalize(dimensionId);
        int split = normalized.indexOf(':');
        return split >= 0 && split < normalized.length() - 1 ? normalized.substring(split + 1) : normalized;
    }

    private static Result rejected(String reason) {
        return new Result(Status.REJECTED, 0, reason == null ? "rejected" : reason.toLowerCase(Locale.ROOT));
    }

    private static void copyTag(CompoundTag source, CompoundTag target, String key) {
        if (source.contains(key)) {
            target.put(key, source.get(key).copy());
        }
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
