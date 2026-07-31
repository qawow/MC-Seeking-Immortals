package com.xunxian.seekingimmortals.quest;

import com.xunxian.seekingimmortals.cultivation.Realm;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Immutable server-side description of one authoritative quest proof.
 *
 * <p>Business code should use the typed factories instead of assembling quest-step tokens. The
 * optional owner/party fields are carried for later multi-player producers; the proof service
 * still re-checks the actual player state before accepting an event.</p>
 *
 * <p>The world-context fields ({@code dimensionId}, {@code currentRegionId},
 * {@code secretRealmId}, {@code sessionId}, {@code phase}, {@code packedPosition}) are trusted
 * server facts attached by the producer. Route parameters remain the only catalog-facing fields;
 * clients can never supply this context.</p>
 */
public record DetailedQuestProofEvent(Type type, String producer, Map<String, String> parameters,
                                      UUID ownerId, UUID actorId, Set<UUID> partyMembers,
                                      String eventKey, Source source, int observedLayer,
                                      String dimensionId, String currentRegionId, String secretRealmId,
                                      String sessionId, String phase, long packedPosition,
                                      boolean hasPosition, String authorityId) {
    public enum Type {
        REGION_ENTER,
        DIMENSION_ENTER,
        STRUCTURE_FORMED,
        NPC_DIALOGUE,
        ITEM_ACQUIRED,
        ITEM_DELIVERED,
        CRAFT_COMPLETED,
        ALCHEMY_COMPLETED,
        ENTITY_KILLED,
        ENTITY_CAPTURED_ALIVE,
        ENCOUNTER_CLEARED,
        ESCORT_COMPLETED,
        METHOD_LAYER_REACHED,
        REALM_REACHED,
        TECHNIQUE_LEARNED,
        SHOP_TRANSACTION,
        AUCTION_TRANSACTION,
        REPUTATION_REACHED,
        CHOICE_COMMITTED,
        INFO_ACKNOWLEDGED,
        SPIRIT_ROOT_TESTED
    }

    public enum Source {
        NATURAL,
        HISTORY,
        ADMIN
    }

    public DetailedQuestProofEvent {
        type = type == null ? Type.INFO_ACKNOWLEDGED : type;
        producer = normalize(producer);
        LinkedHashMap<String, String> normalized = new LinkedHashMap<>();
        if (parameters != null) {
            parameters.forEach((key, value) -> {
                String normalizedKey = normalize(key);
                String normalizedValue = normalize(value);
                if (!normalizedKey.isBlank() && !normalizedValue.isBlank()) {
                    normalized.put(normalizedKey, normalizedValue);
                }
            });
        }
        parameters = Collections.unmodifiableMap(normalized);
        partyMembers = partyMembers == null ? Set.of() : Set.copyOf(partyMembers);
        eventKey = normalize(eventKey);
        source = source == null ? Source.NATURAL : source;
        observedLayer = Math.max(0, observedLayer);
        dimensionId = normalize(dimensionId);
        currentRegionId = normalize(currentRegionId);
        secretRealmId = normalize(secretRealmId);
        sessionId = normalize(sessionId);
        phase = normalize(phase);
        authorityId = normalize(authorityId);
    }

    /** Compatible two-argument constructor used by pre-Q-B-2 call sites and tests. */
    public DetailedQuestProofEvent(Type type, String producer, Map<String, String> parameters,
                                   UUID ownerId, UUID actorId, Set<UUID> partyMembers,
                                   String eventKey, Source source, int observedLayer) {
        this(type, producer, parameters, ownerId, actorId, partyMembers,
                eventKey, source, observedLayer, "", "", "", "", "", 0L, false, "");
    }

    public static DetailedQuestProofEvent methodLayerReached(String methodId, int layer) {
        String method = normalize(methodId);
        return new DetailedQuestProofEvent(Type.METHOD_LAYER_REACHED, "cultivation",
                Map.of("method", method), null, null, Set.of(),
                "method:" + method + ":layer:" + Math.max(1, layer), Source.NATURAL, layer);
    }

    public static DetailedQuestProofEvent realmReached(Realm realm) {
        String id = realm == null ? "" : normalize(realm.getDesignId());
        return new DetailedQuestProofEvent(Type.REALM_REACHED, "cultivation",
                Map.of("realm", id), null, null, Set.of(),
                "realm:" + id, Source.NATURAL, 0);
    }

    public static DetailedQuestProofEvent techniqueLearned(String techniqueId) {
        String id = normalize(techniqueId);
        return new DetailedQuestProofEvent(Type.TECHNIQUE_LEARNED, "cultivation",
                Map.of("technique", id), null, null, Set.of(),
                "technique:" + id, Source.NATURAL, 0);
    }

    /** Strict spiritual-root test proof; produced only by the appraisal stone or the appraisal slab. */
    public static DetailedQuestProofEvent spiritualRootTested() {
        return new DetailedQuestProofEvent(Type.SPIRIT_ROOT_TESTED, "spirit_root",
                Map.of("item", "spirit_root_test"), null, null, Set.of(),
                "spirit_root_test", Source.NATURAL, 0);
    }

    /** Item pickup proof; the id is the server-observed canonical item path. */
    public static DetailedQuestProofEvent itemAcquired(String itemId) {
        String item = normalize(itemId);
        return new DetailedQuestProofEvent(Type.ITEM_ACQUIRED, "item_pickup",
                Map.of("item", item), null, null, Set.of(),
                "item:" + item, Source.NATURAL, 0);
    }

    /** Craft-completion proof; produced only by the server crafting event. */
    public static DetailedQuestProofEvent itemCrafted(String itemId) {
        String item = normalize(itemId);
        return new DetailedQuestProofEvent(Type.CRAFT_COMPLETED, "crafting",
                Map.of("item", item), null, null, Set.of(),
                "craft:" + item, Source.NATURAL, 0);
    }

    /** Item turn-in proof; produced only when the player really holds the item at delivery. */
    public static DetailedQuestProofEvent itemDelivered(String itemId) {
        String item = normalize(itemId);
        return new DetailedQuestProofEvent(Type.ITEM_DELIVERED, "item_delivery",
                Map.of("item", item), null, null, Set.of(),
                "deliver:" + item, Source.NATURAL, 0);
    }

    /** Alchemy-batch completion proof at a real furnace station. */
    public static DetailedQuestProofEvent alchemyCompleted(String stationId) {
        String station = normalize(stationId);
        return new DetailedQuestProofEvent(Type.ALCHEMY_COMPLETED, "alchemy",
                Map.of("station", station), null, null, Set.of(),
                "alchemy:" + station, Source.NATURAL, 0);
    }

    /** Kill proof; the id is the server-observed entity/boss id with correct attribution. */
    public static DetailedQuestProofEvent entityKilled(String entityId) {
        String entity = normalize(entityId);
        return new DetailedQuestProofEvent(Type.ENTITY_KILLED, "living_kill",
                Map.of("entity", entity), null, null, Set.of(),
                "kill:" + entity, Source.NATURAL, 0);
    }

    /** Alive-capture proof; produced only after the capture transaction removed the beast. */
    public static DetailedQuestProofEvent entityCapturedAlive(String entityId) {
        String entity = normalize(entityId);
        return new DetailedQuestProofEvent(Type.ENTITY_CAPTURED_ALIVE, "capture",
                Map.of("entity", entity), null, null, Set.of(),
                "capture:" + entity, Source.NATURAL, 0);
    }

    /** Encounter-clear proof; ordinary encounters carry no secret-realm context. */
    public static DetailedQuestProofEvent encounterCleared(String regionId) {
        String region = normalize(regionId);
        return new DetailedQuestProofEvent(Type.ENCOUNTER_CLEARED, "encounter",
                Map.of("region", region), null, null, Set.of(),
                "encounter:" + region, Source.NATURAL, 0);
    }

    /** Encounter-clear proof bound to a secret-realm layer clear. */
    public static DetailedQuestProofEvent secretRealmEncounterCleared(String regionId, String realmId,
                                                                      String sessionId, String phase) {
        String region = normalize(regionId);
        return new DetailedQuestProofEvent(Type.ENCOUNTER_CLEARED, "encounter",
                Map.of("region", region), null, null, Set.of(),
                "encounter:" + region, Source.NATURAL, 0)
                .withWorld("", region, realmId, sessionId, phase, "", 0L, false);
    }

    /** Escort-completion proof; the region is the live region where the escort ended. */
    public static DetailedQuestProofEvent escortCompleted(String regionId) {
        String region = normalize(regionId);
        return new DetailedQuestProofEvent(Type.ESCORT_COMPLETED, "escort",
                Map.of("region", region), null, null, Set.of(),
                "escort:" + region, Source.NATURAL, 0)
                .withWorld("", region, "", "", "", "", 0L, false);
    }

    /** NPC dialogue proof; the npc id comes from the server dialogue session. */
    public static DetailedQuestProofEvent npcDialogue(String npcId) {
        String npc = normalize(npcId);
        return new DetailedQuestProofEvent(Type.NPC_DIALOGUE, "npc_dialogue",
                Map.of("npc", npc), null, null, Set.of(),
                "npc:" + npc, Source.NATURAL, 0);
    }

    /** Rule-acknowledgement proof; the choice token is produced from a server dialogue node. */
    public static DetailedQuestProofEvent infoAcknowledged(String choiceToken) {
        String choice = normalize(choiceToken);
        return new DetailedQuestProofEvent(Type.INFO_ACKNOWLEDGED, "npc_dialogue",
                Map.of("choice", choice), null, null, Set.of(),
                "info:" + choice, Source.NATURAL, 0);
    }

    /** Dialogue-choice proof; the choice token is produced from a server dialogue node. */
    public static DetailedQuestProofEvent choiceCommitted(String choiceToken) {
        String choice = normalize(choiceToken);
        return new DetailedQuestProofEvent(Type.CHOICE_COMMITTED, "dialogue_choice",
                Map.of("choice", choice), null, null, Set.of(),
                "choice:" + choice, Source.NATURAL, 0);
    }

    /** Shop-transaction proof; the shop id is the server-authoritative market shop id. */
    public static DetailedQuestProofEvent shopTransaction(String shopId) {
        String shop = normalize(shopId);
        return new DetailedQuestProofEvent(Type.SHOP_TRANSACTION, "shop",
                Map.of("shop", shop), null, null, Set.of(),
                "shop:" + shop, Source.NATURAL, 0);
    }

    /** Auction-transaction proof; the venue id comes from the auction snapshot. */
    public static DetailedQuestProofEvent auctionTransaction(String auctionId) {
        String auction = normalize(auctionId);
        return new DetailedQuestProofEvent(Type.AUCTION_TRANSACTION, "auction",
                Map.of("auction", auction), null, null, Set.of(),
                "auction:" + auction, Source.NATURAL, 0);
    }

    /** Reputation proof; the faction key is the server reputation ledger key. */
    public static DetailedQuestProofEvent reputationReached(String factionKey) {
        String faction = normalize(factionKey);
        return new DetailedQuestProofEvent(Type.REPUTATION_REACHED, "reputation",
                Map.of("faction", faction), null, null, Set.of(),
                "rep:" + faction, Source.NATURAL, 0);
    }

    /** Region arrival proven by a successful server-authoritative region transition. */
    public static DetailedQuestProofEvent regionEntered(String regionId) {
        String region = normalize(regionId);
        return new DetailedQuestProofEvent(Type.REGION_ENTER, "region_travel",
                Map.of("region", region), null, null, Set.of(),
                "region:" + region, Source.NATURAL, 0)
                .withWorld("", region, "", "", "", "", 0L, false);
    }

    /** Dimension arrival proven after the post-teleport dimension is verified. */
    public static DetailedQuestProofEvent dimensionEntered(String dimensionId) {
        String dimension = normalize(dimensionId);
        return new DetailedQuestProofEvent(Type.DIMENSION_ENTER, "dimension_travel",
                Map.of("dimension", dimension), null, null, Set.of(),
                "dimension:" + dimension, Source.NATURAL, 0)
                .withWorld(dimension, "", "", "", "", "", 0L, false);
    }

    /** Secret-realm phase region proof; only produced by session-bound realm events. */
    public static DetailedQuestProofEvent secretRealmLayerEntered(String regionId, String realmId,
                                                                  String sessionId, String phase) {
        String region = normalize(regionId);
        return new DetailedQuestProofEvent(Type.REGION_ENTER, "region_travel",
                Map.of("region", region), null, null, Set.of(),
                "region:" + region, Source.NATURAL, 0)
                .withWorld("", realmId, realmId, sessionId, phase, "", 0L, false);
    }

    /** Structure-formation proof bound to the exact dimension and origin that were commissioned. */
    public static DetailedQuestProofEvent structureFormed(String stationId, String dimensionId,
                                                          long packedPosition) {
        String structure = normalize(stationId);
        return new DetailedQuestProofEvent(Type.STRUCTURE_FORMED, "structure_runtime",
                Map.of("structure", structure), null, null, Set.of(),
                "structure:" + structure, Source.NATURAL, 0)
                .withWorld(dimensionId, "", "", "", "", "", packedPosition, true);
    }

    /** Creates an event for a future producer while keeping the route-facing fields explicit. */
    public static DetailedQuestProofEvent of(Type type, String producer, Map<String, String> parameters,
                                             String eventKey) {
        return new DetailedQuestProofEvent(type, producer, parameters, null, null, Set.of(),
                eventKey, Source.NATURAL, 0);
    }

    DetailedQuestProofEvent forOwner(UUID owner) {
        return new DetailedQuestProofEvent(type, producer, parameters, owner, actorId, partyMembers,
                eventKey, source, observedLayer, dimensionId, currentRegionId, secretRealmId,
                sessionId, phase, packedPosition, hasPosition, authorityId);
    }

    DetailedQuestProofEvent asHistory() {
        return new DetailedQuestProofEvent(type, producer, parameters, ownerId, actorId, partyMembers,
                eventKey, Source.HISTORY, observedLayer, dimensionId, currentRegionId, secretRealmId,
                sessionId, phase, packedPosition, hasPosition, authorityId);
    }

    DetailedQuestProofEvent asAdmin() {
        return new DetailedQuestProofEvent(type, producer, parameters, ownerId, actorId, partyMembers,
                eventKey, Source.ADMIN, observedLayer, dimensionId, currentRegionId, secretRealmId,
                sessionId, phase, packedPosition, hasPosition, authorityId);
    }

    /** Attaches trusted server world context; never populated from client input. */
    DetailedQuestProofEvent withWorld(String worldDimensionId, String worldRegionId, String worldRealmId,
                                      String worldSessionId, String worldPhase, String worldAuthorityId,
                                      long worldPackedPosition, boolean worldHasPosition) {
        return new DetailedQuestProofEvent(type, producer, parameters, ownerId, actorId, partyMembers,
                eventKey, source, observedLayer, worldDimensionId, worldRegionId, worldRealmId,
                worldSessionId, worldPhase, worldPackedPosition, worldHasPosition, worldAuthorityId);
    }

    public String parameter(String key) {
        return parameters.getOrDefault(normalize(key), "");
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
