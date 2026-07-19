package com.xunxian.seekingimmortals.worldpack;

import com.xunxian.seekingimmortals.SeekingImmortalsMod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * M09 overworld SavedData for secret-realm progress:
 * first-clear flags, clear counts, active sessions (timer/party), and claimed unique drops.
 */
public final class SecretRealmProgressSavedData extends SavedData {
    private static final String DATA_NAME = SeekingImmortalsMod.MODID + "_secret_realm_progress";

    private final Map<String, Boolean> firstCleared = new HashMap<>();
    private final Map<String, Integer> clearCounts = new HashMap<>();
    private final Map<String, Session> sessions = new HashMap<>();
    private final Set<String> claimedUniqueDrops = new HashSet<>();
    private final Set<String> claimedEncounters = new HashSet<>();

    public static SecretRealmProgressSavedData get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(
                SecretRealmProgressSavedData::load,
                SecretRealmProgressSavedData::new,
                DATA_NAME);
    }

    public static SecretRealmProgressSavedData get(ServerPlayer player) {
        return get(player.server.overworld());
    }

    public static SecretRealmProgressSavedData load(CompoundTag tag) {
        SecretRealmProgressSavedData data = new SecretRealmProgressSavedData();
        CompoundTag clears = tag.getCompound("FirstCleared");
        for (String key : clears.getAllKeys()) {
            if (clears.getBoolean(key)) {
                data.firstCleared.put(key, true);
            }
        }
        CompoundTag counts = tag.getCompound("ClearCounts");
        for (String key : counts.getAllKeys()) {
            data.clearCounts.put(key, Math.max(0, counts.getInt(key)));
        }
        ListTag sessionList = tag.getList("Sessions", 10);
        for (int i = 0; i < sessionList.size(); i++) {
            Session session = Session.load(sessionList.getCompound(i));
            if (!session.playerId().isBlank() && !session.realmId().isBlank()) {
                data.sessions.put(session.playerId(), session);
            }
        }
        ListTag uniques = tag.getList("ClaimedUniques", 8);
        for (int i = 0; i < uniques.size(); i++) {
            String value = uniques.getString(i);
            if (value != null && !value.isBlank()) {
                data.claimedUniqueDrops.add(value);
            }
        }
        ListTag encounters = tag.getList("ClaimedEncounters", 8);
        for (int i = 0; i < encounters.size(); i++) {
            String value = encounters.getString(i);
            if (value != null && !value.isBlank()) {
                data.claimedEncounters.add(value);
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        CompoundTag clears = new CompoundTag();
        firstCleared.forEach((key, value) -> {
            if (Boolean.TRUE.equals(value)) {
                clears.putBoolean(key, true);
            }
        });
        tag.put("FirstCleared", clears);

        CompoundTag counts = new CompoundTag();
        clearCounts.forEach(counts::putInt);
        tag.put("ClearCounts", counts);

        ListTag sessionList = new ListTag();
        sessions.values().stream()
                .sorted((a, b) -> a.playerId().compareTo(b.playerId()))
                .forEach(session -> sessionList.add(session.save()));
        tag.put("Sessions", sessionList);

        ListTag uniques = new ListTag();
        claimedUniqueDrops.stream().sorted().forEach(value -> uniques.add(StringTag.valueOf(value)));
        tag.put("ClaimedUniques", uniques);

        ListTag encounters = new ListTag();
        claimedEncounters.stream().sorted().forEach(value -> encounters.add(StringTag.valueOf(value)));
        tag.put("ClaimedEncounters", encounters);
        return tag;
    }

    public boolean hasFirstCleared(UUID playerId, String realmId) {
        return firstCleared.getOrDefault(key(playerId, realmId), false);
    }

    public int clearCount(UUID playerId, String realmId) {
        return clearCounts.getOrDefault(key(playerId, realmId), 0);
    }

    public void markCleared(UUID playerId, String realmId) {
        String k = key(playerId, realmId);
        if (k.isBlank()) {
            return;
        }
        firstCleared.put(k, true);
        clearCounts.put(k, clearCounts.getOrDefault(k, 0) + 1);
        setDirty();
    }

    public boolean claimUniqueDrop(UUID playerId, String dropId) {
        String k = key(playerId, dropId);
        if (k.isBlank() || claimedUniqueDrops.contains(k)) {
            return false;
        }
        claimedUniqueDrops.add(k);
        setDirty();
        return true;
    }

    public boolean hasClaimedUnique(UUID playerId, String dropId) {
        return claimedUniqueDrops.contains(key(playerId, dropId));
    }

    public Session startSession(ServerPlayer player, String realmId, int timeLimitTicks, int partyLimit) {
        if (player == null || realmId == null || realmId.isBlank()) {
            return null;
        }
        long now = player.server.overworld().getGameTime();
        clearSession(player.getUUID());
        Session session = new Session(
                player.getUUID().toString(),
                UUID.randomUUID().toString(),
                realmId.trim().toLowerCase(Locale.ROOT),
                now,
                now + Math.max(20 * 60, timeLimitTicks),
                Math.max(1, partyLimit),
                false);
        sessions.put(session.playerId(), session);
        setDirty();
        return session;
    }

    public Optional<Session> getSession(UUID playerId) {
        if (playerId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(sessions.get(playerId.toString()));
    }

    public void clearSession(UUID playerId) {
        if (playerId == null) {
            return;
        }
        Session removed = sessions.remove(playerId.toString());
        if (removed != null) {
            String prefix = encounterPrefix(playerId, removed.sessionId());
            claimedEncounters.removeIf(value -> value.startsWith(prefix));
            setDirty();
        }
    }

    public boolean claimEncounter(UUID playerId, String sessionId, String encounterId) {
        String claim = encounterKey(playerId, sessionId, encounterId);
        if (claim.isBlank() || claimedEncounters.contains(claim)) {
            return false;
        }
        claimedEncounters.add(claim);
        setDirty();
        return true;
    }

    public boolean hasClaimedEncounter(UUID playerId, String sessionId, String encounterId) {
        return claimedEncounters.contains(encounterKey(playerId, sessionId, encounterId));
    }

    public int activeCountForRealm(String realmId, long gameTime) {
        if (realmId == null || realmId.isBlank()) {
            return 0;
        }
        String id = realmId.trim().toLowerCase(Locale.ROOT);
        int count = 0;
        for (Session session : sessions.values()) {
            if (id.equals(session.realmId()) && !session.isTimedOut(gameTime)) {
                count++;
            }
        }
        return count;
    }

    public boolean canJoin(String realmId, int partyLimit, long gameTime) {
        return activeCountForRealm(realmId, gameTime) < Math.max(1, partyLimit);
    }

    private static String key(UUID playerId, String id) {
        if (playerId == null || id == null || id.isBlank()) {
            return "";
        }
        return playerId + "|" + id.trim().toLowerCase(Locale.ROOT);
    }

    private static String encounterKey(UUID playerId, String sessionId, String encounterId) {
        if (playerId == null || sessionId == null || sessionId.isBlank()
                || encounterId == null || encounterId.isBlank()) {
            return "";
        }
        return encounterPrefix(playerId, sessionId) + encounterId.trim().toLowerCase(Locale.ROOT);
    }

    private static String encounterPrefix(UUID playerId, String sessionId) {
        if (playerId == null || sessionId == null || sessionId.isBlank()) {
            return "";
        }
        return playerId + "|" + sessionId.trim().toLowerCase(Locale.ROOT) + "|";
    }

    public record Session(
            String playerId,
            String sessionId,
            String realmId,
            long enteredAtTick,
            long expiresAtTick,
            int partyLimit,
            boolean expired) {
        public Session(String playerId, String realmId, long enteredAtTick, long expiresAtTick,
                       int partyLimit, boolean expired) {
            this(playerId, legacySessionId(playerId, realmId, enteredAtTick), realmId,
                    enteredAtTick, expiresAtTick, partyLimit, expired);
        }

        public CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putString("PlayerId", playerId == null ? "" : playerId);
            tag.putString("SessionId", sessionId == null ? "" : sessionId);
            tag.putString("RealmId", realmId == null ? "" : realmId);
            tag.putLong("EnteredAt", enteredAtTick);
            tag.putLong("ExpiresAt", expiresAtTick);
            tag.putInt("PartyLimit", partyLimit);
            tag.putBoolean("Expired", expired);
            return tag;
        }

        public static Session load(CompoundTag tag) {
            String playerId = tag.getString("PlayerId");
            String realmId = tag.getString("RealmId");
            long enteredAt = tag.getLong("EnteredAt");
            String sessionId = tag.getString("SessionId");
            if (sessionId.isBlank()) {
                sessionId = legacySessionId(playerId, realmId, enteredAt);
            }
            return new Session(
                    playerId,
                    sessionId,
                    realmId,
                    enteredAt,
                    tag.getLong("ExpiresAt"),
                    Math.max(1, tag.getInt("PartyLimit")),
                    tag.getBoolean("Expired"));
        }

        public boolean isTimedOut(long gameTime) {
            return expired || gameTime >= expiresAtTick;
        }

        private static String legacySessionId(String playerId, String realmId, long enteredAtTick) {
            String source = String.valueOf(playerId) + "|" + String.valueOf(realmId) + "|" + enteredAtTick;
            return UUID.nameUUIDFromBytes(source.getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString();
        }
    }
}
