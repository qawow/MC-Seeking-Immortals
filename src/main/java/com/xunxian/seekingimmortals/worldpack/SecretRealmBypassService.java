package com.xunxian.seekingimmortals.worldpack;

import com.xunxian.seekingimmortals.catalog.ItemCatalogService;
import com.xunxian.seekingimmortals.cultivation.CultivationHelper;
import com.xunxian.seekingimmortals.cultivation.PlayerCultivation;
import com.xunxian.seekingimmortals.quest.DetailedQuestProofService;
import com.xunxian.seekingimmortals.quest.QuestProgress;
import com.xunxian.seekingimmortals.registry.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.AABB;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Y-A-2: authored "pay the toll and skip the fight" branch (`pay_yezha_toll` —
 * 「向夜叉巢绕路/献祭绕行」→「耗资源避战」).
 *
 * <p>The authored walkthrough for the peiying material hunt states step 1 as
 * 「过夜叉 — 过夜叉巢<b>或付代价绕道</b>」, so a paid bypass is a legitimate way to satisfy the
 * step-1 {@code ENCOUNTER_CLEARED(yy_yezha)} proof; without this handler that branch is a dead end.</p>
 *
 * <p>Transaction order is fixed and fail-closed: validate session/layer → reserve the cost
 * atomically → record the proof → clear the layer. Any failure after reservation refunds.</p>
 */
public final class SecretRealmBypassService {
    /** Layers that authors marked as bypassable, mapped to the proof phase they satisfy. */
    private static final Map<String, String> BYPASSABLE = Map.of("yinyang_ku:yy_yezha", "mid");
    private static final String BYPASS_TAG = "seeking_immortals_secret_realm_bypass";
    private static final String LAYER_SPAWN_ROOT = "seeking_immortals_secret_realm_layer_spawns";
    private static final String ENCOUNTER_PREFIX = "trial:layer:";
    /** Sacrifice cost: spirit stones, or contribution when the player carries none. */
    public static final int BYPASS_STONE_COST = 12;
    public static final int BYPASS_CONTRIBUTION_COST = 25;
    private static final double CLEAR_RADIUS = 24.0D;

    private SecretRealmBypassService() {}

    public enum Status {
        SUCCESS,
        NO_ACTIVE_SESSION,
        LAYER_NOT_BYPASSABLE,
        ALREADY_BYPASSED,
        NOT_ENOUGH_RESOURCES,
        PROOF_REJECTED
    }

    public record Result(Status status, int stonesPaid, int contributionPaid) {
        public boolean success() {
            return status == Status.SUCCESS;
        }
    }

    public static boolean isBypassableLayer(String realmId, String layerId) {
        return BYPASSABLE.containsKey(key(realmId, layerId));
    }

    /** Proof phase an authored bypassable layer settles, or empty when the layer is not bypassable. */
    public static String proofPhaseFor(String realmId, String layerId) {
        return BYPASSABLE.getOrDefault(key(realmId, layerId), "");
    }

    public static boolean hasBypassed(ServerPlayer player, String realmId, String layerId) {
        if (player == null) {
            return false;
        }
        Optional<SecretRealmProgressSavedData.Session> session =
                SecretRealmSessionService.activeSession(player, normalize(realmId));
        if (session.isEmpty()) {
            return false;
        }
        return player.getPersistentData().getCompound(BYPASS_TAG)
                .getBoolean(sessionKey(session.get(), realmId, layerId));
    }

    /**
     * Pays the authored toll to skip a bypassable encounter layer.
     * Never spawns anything and never grants combat loot: the whole point is avoiding the fight.
     */
    public static Result bypassLayer(ServerPlayer player, String realmId, String layerId) {
        if (player == null) {
            return new Result(Status.NO_ACTIVE_SESSION, 0, 0);
        }
        String realm = normalize(realmId);
        String layer = normalize(layerId);
        String phase = proofPhaseFor(realm, layer);
        if (phase.isBlank()) {
            // Fail closed: only authored bypassable layers may be skipped for resources.
            return new Result(Status.LAYER_NOT_BYPASSABLE, 0, 0);
        }
        SecretRealmProgressSavedData.Session session =
                SecretRealmSessionService.activeSession(player, realm).orElse(null);
        if (session == null) {
            return new Result(Status.NO_ACTIVE_SESSION, 0, 0);
        }
        String bypassKey = sessionKey(session, realm, layer);
        CompoundTag bypassRoot = player.getPersistentData().getCompound(BYPASS_TAG).copy();
        if (bypassRoot.getBoolean(bypassKey)) {
            return new Result(Status.ALREADY_BYPASSED, 0, 0);
        }

        // Reserve the sacrifice atomically before any world or ledger effect.
        Item stone = ModItems.METAL_SPIRIT_STONE.get();
        Map<Item, Integer> costs = new LinkedHashMap<>();
        costs.put(stone, BYPASS_STONE_COST);
        InventoryReservation reservation = InventoryReservation.consume(player, costs);
        int stonesPaid = reservation == null ? 0 : BYPASS_STONE_COST;
        int contributionPaid = 0;
        QuestProgress progress = null;
        if (reservation == null) {
            // No stones: fall back to sect contribution, same check-then-spend transaction.
            progress = CultivationHelper.get(player)
                    .map(PlayerCultivation::getSevenMysteriesQuest)
                    .orElse(null);
            if (progress == null || progress.getContribution() < BYPASS_CONTRIBUTION_COST
                    || !progress.spendContribution(BYPASS_CONTRIBUTION_COST)) {
                player.displayClientMessage(Component.translatable(
                        "message.seeking_immortals.worldpack.bypass_not_enough",
                        BYPASS_STONE_COST, BYPASS_CONTRIBUTION_COST), true);
                return new Result(Status.NOT_ENOUGH_RESOURCES, 0, 0);
            }
            contributionPaid = BYPASS_CONTRIBUTION_COST;
        }

        // The authored step accepts "过夜叉巢或付代价绕道", so the paid route settles the same proof.
        DetailedQuestProofService.Result proof =
                DetailedQuestProofService.recordEncounterCleared(player, realm, phase);
        if (proof.status() == DetailedQuestProofService.Status.REJECTED) {
            refund(player, reservation, progress, contributionPaid);
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.worldpack.bypass_rejected"), true);
            return new Result(Status.PROOF_REJECTED, 0, 0);
        }

        bypassRoot.putBoolean(bypassKey, true);
        player.getPersistentData().put(BYPASS_TAG, bypassRoot);
        // Suppress the authored roster for this layer and clear anything already standing,
        // so paying really does avoid the fight instead of stacking with it.
        suppressLayerRoster(player, session, realm, layer);
        int cleared = clearLayerMobs(player, realm, layer);
        player.displayClientMessage(Component.translatable(
                "message.seeking_immortals.worldpack.bypass_paid",
                stonesPaid > 0 ? stonesPaid : contributionPaid,
                stonesPaid > 0
                        ? Component.translatable("text.seeking_immortals.bypass_cost_stone")
                        : Component.translatable("text.seeking_immortals.bypass_cost_contribution"),
                cleared), true);
        return new Result(Status.SUCCESS, stonesPaid, contributionPaid);
    }

    private static void refund(ServerPlayer player, InventoryReservation reservation,
                               QuestProgress progress, int contributionPaid) {
        if (reservation != null) {
            reservation.refund(player);
        }
        if (progress != null && contributionPaid > 0) {
            progress.addContribution(contributionPaid);
        }
    }

    /** Marks the layer roster as already handled so re-entry does not spawn what was paid off. */
    private static void suppressLayerRoster(ServerPlayer player,
                                            SecretRealmProgressSavedData.Session session,
                                            String realmId, String layerId) {
        CompoundTag root = player.getPersistentData().getCompound(LAYER_SPAWN_ROOT).copy();
        root.putBoolean(session.sessionId() + "|" + realmId + "|" + ENCOUNTER_PREFIX + layerId, true);
        player.getPersistentData().put(LAYER_SPAWN_ROOT, root);
    }

    /** Removes this layer's already-spawned session-bound mobs; grants no kill credit or loot. */
    private static int clearLayerMobs(ServerPlayer player, String realmId, String layerId) {
        if (!(player.level() instanceof ServerLevel level)) {
            return 0;
        }
        String encounterId = ENCOUNTER_PREFIX + layerId;
        AABB box = player.getBoundingBox().inflate(CLEAR_RADIUS);
        int cleared = 0;
        List<Mob> mobs = level.getEntitiesOfClass(Mob.class, box, mob ->
                SecretRealmTrialService.isTrialMob(mob)
                        && realmId.equals(SecretRealmTrialService.trialRealm(mob)));
        for (Mob mob : mobs) {
            CompoundTag trial = mob.getPersistentData()
                    .getCompound(SecretRealmTrialService.TRIAL_TAG);
            if (!encounterId.equals(SecretRealmSessionService.boundEncounterId(trial))) {
                continue;
            }
            mob.discard();
            cleared++;
        }
        return cleared;
    }

    /** Authored toll broker: the 鬼道散修 who sells the detour at the nest mouth. */
    public static final String TOLL_BROKER_NPC = "npc_yinyang_ghost_rogue";
    public static final String TOLL_BROKER_TREE = "tree_yinyang_toll_broker";
    private static final String BROKER_TAG = "seeking_immortals_bypass_broker";

    /**
     * Places the authored toll broker near the entry anchor so the paid detour is reachable by
     * normal play (no admin command). Once per session per realm.
     */
    public static boolean ensureTollBroker(ServerPlayer player, String realmId) {
        if (player == null || !(player.level() instanceof ServerLevel level)) {
            return false;
        }
        String realm = normalize(realmId);
        boolean bypassable = BYPASSABLE.keySet().stream()
                .anyMatch(key -> key.startsWith(realm + ":"));
        if (!bypassable) {
            return false;
        }
        SecretRealmProgressSavedData.Session session =
                SecretRealmSessionService.activeSession(player, realm).orElse(null);
        if (session == null) {
            return false;
        }
        CompoundTag root = player.getPersistentData().getCompound(BROKER_TAG).copy();
        String key = session.sessionId() + "|" + realm;
        if (root.getBoolean(key)) {
            return false;
        }
        var spawned = com.xunxian.seekingimmortals.npc.NpcSpawnService.spawnQuestNpc(
                level, player.blockPosition().above().offset(3, 0, 3),
                TOLL_BROKER_NPC, "鬼道散修", player.getYRot());
        if (spawned.isEmpty()) {
            return false;
        }
        // The broker is not in the named-NPC roster, so bind its tree explicitly.
        spawned.get().setDialogueTreeId(TOLL_BROKER_TREE);
        spawned.get().setPersistenceRequired();
        root.putBoolean(key, true);
        player.getPersistentData().put(BROKER_TAG, root);
        player.sendSystemMessage(Component.translatable(
                "message.seeking_immortals.worldpack.bypass_broker_present"));
        return true;
    }

    private static String key(String realmId, String layerId) {
        return normalize(realmId) + ":" + normalize(layerId);
    }

    private static String sessionKey(SecretRealmProgressSavedData.Session session,
                                     String realmId, String layerId) {
        return session.sessionId() + "|" + key(realmId, layerId);
    }

    private static String normalize(String raw) {
        return raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
    }
}
