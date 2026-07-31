package com.xunxian.seekingimmortals.quest;

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
            if (!handledChains.add(route.chainId())) {
                continue;
            }
            if (!authoritativeState(player, route, event)) {
                continue;
            }
            DetailedQuestRuntimeService.Progress progress =
                    DetailedQuestRuntimeService.progressOf(player, route.chainId());
            String ledgerKey = ledgerKey(route, event);
            if (hasLedger(player, ledgerKey)) {
                duplicate = true;
                continue;
            }
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
            if (!route.requiredParams().equals(event.parameters())) {
                continue;
            }
            result.add(route);
        }
        return result;
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
