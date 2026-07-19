package com.xunxian.seekingimmortals.worldpack;

import com.xunxian.seekingimmortals.cultivation.CultivationHelper;
import com.xunxian.seekingimmortals.cultivation.ProgressionGateApi;
import com.xunxian.seekingimmortals.cultivation.Realm;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;

import java.util.Locale;
import java.util.Optional;

/**
 * M09 session authority: open checks, enter/leave bookkeeping, timeout/death eject, clear hook.
 */
public final class SecretRealmSessionService {
    private static final String SESSION_CLEAR_ROOT = "seeking_immortals_realm_session_clear";
    public static final String OWNER_UUID = "OwnerUUID";
    public static final String SESSION_ID = "SessionId";
    public static final String REALM_ID = "RealmId";
    public static final String ENCOUNTER_ID = "EncounterId";

    private SecretRealmSessionService() {}

    public static Optional<SecretRealmCatalogService.RealmDef> catalog(String realmId) {
        return SecretRealmCatalogService.find(realmId);
    }

    /**
     * Server-side open validation for gate/enter paths.
     * Returns empty when allowed; otherwise a translation key suffix or plain reason.
     */
    public static Optional<String> validateOpen(ServerPlayer player, String realmId) {
        if (player == null || realmId == null || realmId.isBlank()) {
            return Optional.of("unknown_realm");
        }
        Optional<SecretRealmCatalogService.RealmDef> defOpt = SecretRealmCatalogService.find(realmId);
        if (defOpt.isEmpty()) {
            // Allow non-catalog worldpack-only shells (king territories etc.).
            return Optional.empty();
        }
        SecretRealmCatalogService.RealmDef def = defOpt.get();
        if (!ProgressionGateApi.meetsRealm(player, normalizeRealmMin(def.realmMin()))) {
            return Optional.of("realm_too_low:" + def.realmMin());
        }
        SecretRealmProgressSavedData progress = SecretRealmProgressSavedData.get(player);
        long now = player.server.overworld().getGameTime();
        if (!progress.canJoin(def.id(), def.partyLimit(), now)) {
            return Optional.of("party_full:" + def.partyLimit());
        }
        if (!isOpenWindow(player, def)) {
            return Optional.of("window_closed:" + def.openCondition());
        }
        return Optional.empty();
    }

    public static boolean isOpenWindow(ServerPlayer player, SecretRealmCatalogService.RealmDef def) {
        if (player == null || def == null) {
            return true;
        }
        if (def.openWindowDays() == null || def.openWindowDays().isEmpty()) {
            // Cycle ids without numeric windows stay open; daily ticket hints remain soft.
            return true;
        }
        long day = player.serverLevel().getDayTime() / 24000L;
        int dayOfCycle = (int) (day % 30L) + 1; // soft 30-day cycle window
        for (Integer windowDay : def.openWindowDays()) {
            if (windowDay != null && windowDay == dayOfCycle) {
                return true;
            }
        }
        // If window list is a range-like pair [start,end], accept inclusive span.
        if (def.openWindowDays().size() >= 2) {
            int a = def.openWindowDays().get(0);
            int b = def.openWindowDays().get(1);
            int min = Math.min(a, b);
            int max = Math.max(a, b);
            if (dayOfCycle >= min && dayOfCycle <= max) {
                return true;
            }
        }
        return false;
    }

    public static SecretRealmProgressSavedData.Session onEnter(ServerPlayer player, String realmId) {
        if (player == null || realmId == null || realmId.isBlank()) {
            return null;
        }
        Optional<SecretRealmCatalogService.RealmDef> defOpt = SecretRealmCatalogService.find(realmId);
        int timeLimit = defOpt.map(SecretRealmCatalogService.RealmDef::timeLimitTicks).orElse(20 * 60 * 30);
        int party = defOpt.map(SecretRealmCatalogService.RealmDef::partyLimit).orElse(4);
        SecretRealmProgressSavedData.Session session =
                SecretRealmProgressSavedData.get(player).startSession(player, realmId, timeLimit, party);
        if (session == null) {
            return null;
        }
        // Reset per-session clear latch.
        player.getPersistentData().put(SESSION_CLEAR_ROOT, new net.minecraft.nbt.CompoundTag());
        // Mid-layer traps via M07 free fields.
        int traps = SecretRealmTrapService.activateAllLayerTraps(player, realmId);
        if (traps > 0) {
            player.sendSystemMessage(Component.translatable(
                    "message.seeking_immortals.worldpack.trial_traps_armed", realmId, traps));
        }
        if (defOpt.isPresent() && !defOpt.get().openCondition().isBlank()) {
            player.sendSystemMessage(Component.translatable(
                    "message.seeking_immortals.worldpack.realm_open_condition", defOpt.get().openCondition()));
        }
        return session;
    }

    public static void onLeave(ServerPlayer player) {
        if (player == null) {
            return;
        }
        SecretRealmProgressSavedData.get(player).clearSession(player.getUUID());
        player.getPersistentData().remove(SESSION_CLEAR_ROOT);
    }

    /**
     * Mark realm cleared, grant first/repeat bookkeeping, and publish {@link SecretRealmClearedEvent}.
     * Idempotent within a single enter session.
     */
    public static void onRealmCleared(String realmId, ServerPlayer player) {
        if (player == null || realmId == null || realmId.isBlank()) {
            return;
        }
        String id = realmId.trim().toLowerCase(Locale.ROOT);
        if (activeSession(player, id).isEmpty()) {
            return;
        }
        net.minecraft.nbt.CompoundTag sessionClear = player.getPersistentData().getCompound(SESSION_CLEAR_ROOT).copy();
        if (sessionClear.getBoolean(id)) {
            return;
        }
        sessionClear.putBoolean(id, true);
        player.getPersistentData().put(SESSION_CLEAR_ROOT, sessionClear);

        SecretRealmProgressSavedData progress = SecretRealmProgressSavedData.get(player);
        boolean first = !progress.hasFirstCleared(player.getUUID(), id);
        progress.markCleared(player.getUUID(), id);
        int count = progress.clearCount(player.getUUID(), id);
        ReputationService.add(player, "secret_realm_explorer", first ? 5 : 2);
        player.sendSystemMessage(Component.translatable(
                first
                        ? "message.seeking_immortals.worldpack.realm_first_clear"
                        : "message.seeking_immortals.worldpack.realm_repeat_clear",
                id, count));
        MinecraftForge.EVENT_BUS.post(new SecretRealmClearedEvent(player, id, first, count));
    }

    public static void tickSessions(ServerPlayer player) {
        if (player == null) {
            return;
        }
        CultivationHelper.get(player).ifPresent(cultivation -> {
            String active = cultivation.getWorldpackActiveSecretRealmId();
            if (active == null || active.isBlank()) {
                SecretRealmProgressSavedData.get(player).clearSession(player.getUUID());
                return;
            }
            SecretRealmProgressSavedData progress = SecretRealmProgressSavedData.get(player);
            Optional<SecretRealmProgressSavedData.Session> sessionOpt = progress.getSession(player.getUUID());
            long now = player.server.overworld().getGameTime();
            if (sessionOpt.isEmpty()) {
                // Recover session if capability says active but SavedData lost (restart mid-run).
                Optional<SecretRealmCatalogService.RealmDef> def = SecretRealmCatalogService.find(active);
                progress.startSession(
                        player,
                        active,
                        def.map(SecretRealmCatalogService.RealmDef::timeLimitTicks).orElse(20 * 60 * 30),
                        def.map(SecretRealmCatalogService.RealmDef::partyLimit).orElse(4));
                return;
            }
            SecretRealmProgressSavedData.Session session = sessionOpt.get();
            if (!active.equals(session.realmId())) {
                progress.startSession(player, active, 20 * 60 * 30, session.partyLimit());
                return;
            }
            if (session.isTimedOut(now)) {
                player.sendSystemMessage(Component.translatable(
                        "message.seeking_immortals.worldpack.realm_timeout", active));
                WorldpackGameplayService.returnFromSecretRealm(player);
            }
        });
    }

    public static void handlePlayerDeath(ServerPlayer player) {
        if (player == null) {
            return;
        }
        CultivationHelper.get(player).ifPresent(cultivation -> {
            if (cultivation.getWorldpackActiveSecretRealmId() == null
                    || cultivation.getWorldpackActiveSecretRealmId().isBlank()) {
                return;
            }
            // Death ejects from instance after respawn path by clearing active id via return.
            player.sendSystemMessage(Component.translatable(
                    "message.seeking_immortals.worldpack.realm_death_eject",
                    cultivation.getWorldpackActiveSecretRealmId()));
            WorldpackGameplayService.returnFromSecretRealm(player);
        });
    }

    public static String normalizeRealmMin(String realmMin) {
        if (realmMin == null || realmMin.isBlank()) {
            return "";
        }
        String key = realmMin.trim();
        Realm realm = Realm.fromDesignId(key);
        if (realm != null) {
            return realm.getDesignId() == null ? key : realm.getDesignId();
        }
        // Accept UPPER_SNAKE design-ish values from text material.
        return key.toLowerCase(Locale.ROOT);
    }

    public static Optional<SecretRealmProgressSavedData.Session> activeSession(
            ServerPlayer player, String expectedRealmId) {
        if (player == null) {
            return Optional.empty();
        }
        String expected = expectedRealmId == null ? "" : expectedRealmId.trim().toLowerCase(Locale.ROOT);
        String activeRealm = CultivationHelper.get(player)
                .map(cultivation -> cultivation.getWorldpackActiveSecretRealmId())
                .orElse("");
        if (activeRealm == null || activeRealm.isBlank()) {
            return Optional.empty();
        }
        activeRealm = activeRealm.trim().toLowerCase(Locale.ROOT);
        if (!expected.isBlank() && !expected.equals(activeRealm)) {
            return Optional.empty();
        }
        long now = player.server.overworld().getGameTime();
        String requiredRealm = expected.isBlank() ? activeRealm : expected;
        return SecretRealmProgressSavedData.get(player).getSession(player.getUUID())
                .filter(session -> player.getUUID().toString().equals(session.playerId()))
                .filter(session -> requiredRealm.equals(session.realmId()))
                .filter(session -> !session.sessionId().isBlank())
                .filter(session -> !session.isTimedOut(now));
    }

    public static void bindEncounter(CompoundTag tag, ServerPlayer owner,
                                     SecretRealmProgressSavedData.Session session,
                                     String realmId, String encounterId) {
        if (tag == null || owner == null || session == null) {
            return;
        }
        tag.putString(OWNER_UUID, owner.getUUID().toString());
        tag.putString(SESSION_ID, session.sessionId());
        tag.putString(REALM_ID, normalizeId(realmId));
        tag.putString(ENCOUNTER_ID, normalizeId(encounterId));
    }

    public static boolean matchesEncounter(ServerPlayer player, CompoundTag tag) {
        if (player == null || !hasEncounterBinding(tag)) {
            return false;
        }
        String ownerId = tag.getString(OWNER_UUID);
        String sessionId = tag.getString(SESSION_ID);
        String realmId = normalizeId(tag.getString(REALM_ID));
        if (!player.getUUID().toString().equals(ownerId)) {
            return false;
        }
        return activeSession(player, realmId)
                .map(session -> sessionId.equals(session.sessionId()))
                .orElse(false);
    }

    public static boolean hasEncounterBinding(CompoundTag tag) {
        return tag != null
                && !tag.getString(OWNER_UUID).isBlank()
                && !tag.getString(SESSION_ID).isBlank()
                && !normalizeId(tag.getString(REALM_ID)).isBlank()
                && !normalizeId(tag.getString(ENCOUNTER_ID)).isBlank();
    }

    public static boolean claimEncounter(ServerPlayer player, CompoundTag tag) {
        if (!matchesEncounter(player, tag)) {
            return false;
        }
        return SecretRealmProgressSavedData.get(player).claimEncounter(
                player.getUUID(), tag.getString(SESSION_ID), tag.getString(ENCOUNTER_ID));
    }

    public static String boundRealmId(CompoundTag tag) {
        return tag == null ? "" : normalizeId(tag.getString(REALM_ID));
    }

    private static String normalizeId(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
