package com.xunxian.seekingimmortals.quest;

import com.xunxian.seekingimmortals.catalog.ManualCatalogService;
import com.xunxian.seekingimmortals.cultivation.CultivationHelper;
import com.xunxian.seekingimmortals.cultivation.PlayerCultivation;
import com.xunxian.seekingimmortals.cultivation.Realm;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Single authority for structured detailed-quest proofs.
 *
 * <p>The service matches an event only against the player's current route step, validates the
 * producer and route parameters, and then delegates the reward transaction to the existing
 * idempotent runtime. A repeated event is harmless and one call can advance at most one step per
 * chain.</p>
 */
public final class DetailedQuestProofService {
    public static final String LEDGER_TAG = "seeking_immortals_detailed_quest_proof_ledger";
    public static final String HISTORY_TAG = "seeking_immortals_detailed_quest_proof_history";
    private static final int MAX_LEDGER_ENTRIES = 512;

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

    /**
     * Replays only durable, route-authorized facts. Cultivation facts are reconstructed from the
     * current capability to support old saves; no arbitrary client token is accepted.
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
            boolean hasFact = hasHistory(player, route.eventId()) || isCultivationRoute(route);
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
            default -> true;
        };
    }

    private static DetailedQuestProofEvent historicalEventFor(ServerPlayer player,
                                                               DetailedQuestProofCatalog.Route route) {
        return switch (route.proofType()) {
            case "METHOD_LAYER_REACHED" -> DetailedQuestProofEvent.methodLayerReached(
                    route.parameter("method"), ManualCatalogService.getMethodLayer(player, route.parameter("method")));
            case "REALM_REACHED" -> {
                Realm realm = Realm.fromDesignId(route.parameter("realm"));
                yield realm == null ? null : DetailedQuestProofEvent.realmReached(realm);
            }
            case "TECHNIQUE_LEARNED" -> DetailedQuestProofEvent.techniqueLearned(route.parameter("technique"));
            default -> null;
        };
    }

    private static boolean isCultivationRoute(DetailedQuestProofCatalog.Route route) {
        return "cultivation".equals(route.producer());
    }

    private static String ledgerKey(DetailedQuestProofCatalog.Route route, DetailedQuestProofEvent event) {
        String eventKey = event.eventKey().isBlank() ? "event" : event.eventKey();
        return route.eventId() + "|" + eventKey;
    }

    private static boolean hasLedger(ServerPlayer player, String key) {
        return player.getPersistentData().getCompound(LEDGER_TAG).contains(key);
    }

    private static boolean hasHistory(ServerPlayer player, String routeEventId) {
        return player.getPersistentData().getCompound(HISTORY_TAG).getBoolean(routeEventId);
    }

    private static boolean storeHistory(ServerPlayer player, DetailedQuestProofCatalog.Route route,
                                        DetailedQuestProofEvent event) {
        CompoundTag history = player.getPersistentData().getCompound(HISTORY_TAG).copy();
        if (history.getBoolean(route.eventId())) {
            return false;
        }
        if (history.getAllKeys().size() >= MAX_LEDGER_ENTRIES) {
            return false;
        }
        history.putBoolean(route.eventId(), true);
        player.getPersistentData().put(HISTORY_TAG, history);
        return true;
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
        ledger.put(key, entry);
        player.getPersistentData().put(LEDGER_TAG, ledger);
    }

    private static Result rejected(String reason) {
        return new Result(Status.REJECTED, 0, reason == null ? "rejected" : reason.toLowerCase(Locale.ROOT));
    }

    private static void copyTag(CompoundTag source, CompoundTag target, String key) {
        if (source.contains(key)) {
            target.put(key, source.get(key).copy());
        }
    }
}
