package com.xunxian.seekingimmortals.worldpack;

import com.xunxian.seekingimmortals.cultivation.CultivationHelper;
import com.xunxian.seekingimmortals.cultivation.ProgressionGateApi;
import com.xunxian.seekingimmortals.cultivation.Realm;
import com.xunxian.seekingimmortals.npc.NpcDialogueFlags;
import com.xunxian.seekingimmortals.util.PlayerDisplayText;
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
        if ("yinyang_ku".equals(realmId.trim().toLowerCase(Locale.ROOT))
                && !NpcDialogueFlags.hasFlag(player, "yinyang_ku_entry")) {
            return Optional.of("quest_locked");
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
        Optional<String> policyDenied = SecretRealmOpenPolicy.validate(player, def);
        if (policyDenied.isPresent()) {
            return Optional.of(policyDenied.get() + ":" + def.openCondition());
        }
        return Optional.empty();
    }

    public static boolean isOpenWindow(ServerPlayer player, SecretRealmCatalogService.RealmDef def) {
        if (player == null || def == null) {
            return true;
        }
        return SecretRealmOpenPolicy.validate(player, def).isEmpty();
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
        com.xunxian.seekingimmortals.quest.QuestHookRuntime.onSecretRealmEnter(player, realmId);
        // Mid-layer traps via M07 free fields.
        int traps = SecretRealmTrapService.activateAllLayerTraps(player, realmId);
        if (traps > 0) {
            player.sendSystemMessage(Component.translatable(
                    "message.seeking_immortals.worldpack.trial_traps_armed", realmDisplay(realmId), traps));
        }
        if (defOpt.isPresent() && !defOpt.get().openCondition().isBlank()) {
            player.sendSystemMessage(Component.translatable(
                    "message.seeking_immortals.worldpack.realm_open_condition",
                    PlayerDisplayText.safeLiteral(
                            defOpt.get().openCondition(), "text.seeking_immortals.unknown_requirement")));
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
                realmDisplay(id), count));
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
                        "message.seeking_immortals.worldpack.realm_timeout", realmDisplay(active)));
                WorldpackGameplayService.returnFromSecretRealm(player, false);
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
                    realmDisplay(cultivation.getWorldpackActiveSecretRealmId())));
            WorldpackGameplayService.returnFromSecretRealm(player, false);
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
            // Security: reject non-owner access to encounter rewards
            com.xunxian.seekingimmortals.SeekingImmortalsMod.LOGGER.warn(
                    "Secret realm encounter owner mismatch: player {} attempted to claim encounter bound to {}",
                    player.getUUID(), ownerId);
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

    /**
     * Defense-in-depth: verify that the claiming player is the session owner.
     * This check is redundant with matchesEncounter's UUID validation but provides
     * an explicit guard for reward paths that bypass tag-based encounter binding.
     *
     * @param player The player attempting to claim rewards
     * @param session The active secret-realm session
     * @return true if player UUID matches session owner UUID
     */
    public static boolean isSessionOwner(ServerPlayer player, SecretRealmProgressSavedData.Session session) {
        if (player == null || session == null) {
            return false;
        }
        return player.getUUID().toString().equals(session.playerId());
    }

    public static String boundRealmId(CompoundTag tag) {
        return tag == null ? "" : normalizeId(tag.getString(REALM_ID));
    }

    /** Y-A-2: the bound encounter id, so a layer-scoped consumer can filter its own mobs. */
    public static String boundEncounterId(CompoundTag tag) {
        return tag == null ? "" : normalizeId(tag.getString(ENCOUNTER_ID));
    }

    private static Component realmDisplay(String realmId) {
        Optional<SecretRealmCatalogService.RealmDef> runtime = SecretRealmCatalogService.find(realmId);
        if (runtime.isPresent()) {
            return PlayerDisplayText.safeLiteral(
                    runtime.get().display(), "text.seeking_immortals.unknown_secret_realm");
        }
        return WorldpackDataService.builtin().findSecretRealm(realmId)
                .map(realm -> !realm.displayZh().isBlank() ? realm.displayZh() : realm.displayEn())
                .filter(PlayerDisplayText::isSafe)
                .map(value -> Component.literal(value.trim()))
                .orElseGet(() -> Component.translatable("text.seeking_immortals.unknown_secret_realm"));
    }

    private static String normalizeId(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
