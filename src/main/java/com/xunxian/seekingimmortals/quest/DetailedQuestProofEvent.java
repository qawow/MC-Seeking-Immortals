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
 */
public record DetailedQuestProofEvent(Type type, String producer, Map<String, String> parameters,
                                      UUID ownerId, UUID actorId, Set<UUID> partyMembers,
                                      String eventKey, Source source, int observedLayer) {
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
        INFO_ACKNOWLEDGED
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

    /** Creates an event for a future producer while keeping the route-facing fields explicit. */
    public static DetailedQuestProofEvent of(Type type, String producer, Map<String, String> parameters,
                                             String eventKey) {
        return new DetailedQuestProofEvent(type, producer, parameters, null, null, Set.of(),
                eventKey, Source.NATURAL, 0);
    }

    DetailedQuestProofEvent forOwner(UUID owner) {
        return new DetailedQuestProofEvent(type, producer, parameters, owner, actorId, partyMembers,
                eventKey, source, observedLayer);
    }

    DetailedQuestProofEvent asHistory() {
        return new DetailedQuestProofEvent(type, producer, parameters, ownerId, actorId, partyMembers,
                eventKey, Source.HISTORY, observedLayer);
    }

    DetailedQuestProofEvent asAdmin() {
        return new DetailedQuestProofEvent(type, producer, parameters, ownerId, actorId, partyMembers,
                eventKey, Source.ADMIN, observedLayer);
    }

    public String parameter(String key) {
        return parameters.getOrDefault(normalize(key), "");
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
