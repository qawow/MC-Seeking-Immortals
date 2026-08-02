package com.xunxian.seekingimmortals.npc;

import com.xunxian.seekingimmortals.entity.SummonedServitorEntity;
import com.xunxian.seekingimmortals.structure.MultiblockOperationalService;
import com.xunxian.seekingimmortals.structure.MultiblockStationService;
import com.xunxian.seekingimmortals.structure.MultiblockStructureCatalog;
import com.xunxian.seekingimmortals.worldpack.ReputationService;
import com.xunxian.seekingimmortals.worldpack.TrialCombatShellService;
import com.xunxian.seekingimmortals.worldpack.WorldpackSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;

import java.util.Locale;
import java.util.Optional;

/** Persistent, bounded world consequences for authored dialogue actions. */
public final class DialogueWorldActionService {
    public static final String MARKERS_TAG = "seeking_immortals_dialogue_markers";
    public static final String HINTS_TAG = "seeking_immortals_dialogue_hints";
    public static final String ANOMALIES_TAG = "seeking_immortals_dialogue_anomalies";
    public static final String SUSPICION_TAG = "seeking_immortals_dialogue_suspicion";
    public static final String COMBAT_TAG = "seeking_immortals_dialogue_combat";
    public static final String HOSTILE_MARKER = "seeking_immortals_dialogue_hostile";
    public static final String HOSTILE_PLAYER = "seeking_immortals_dialogue_hostile_player";
    public static final String HOSTILE_ACTION = "seeking_immortals_dialogue_hostile_action";
    private static final int STRUCTURE_SCAN_RADIUS = 10;
    private static final int STRUCTURE_SCAN_Y = 4;
    private static final int MAX_SUSPICION = 100;
    private static final int MAX_LOG_ENTRIES = 48;
    private static final int MAX_BOUND_HOSTILES = 2;
    private static final long COMBAT_COOLDOWN_TICKS = 20L * 60L * 5L;

    private DialogueWorldActionService() {}

    public static void copyPersistentData(CompoundTag source, CompoundTag target) {
        if (source == null || target == null) {
            return;
        }
        copyTag(source, target, MARKERS_TAG);
        copyTag(source, target, HINTS_TAG);
        copyTag(source, target, ANOMALIES_TAG);
        copyTag(source, target, SUSPICION_TAG);
        copyTag(source, target, COMBAT_TAG);
    }

    /** Marks only a known structure backed by a world anchor or a nearby formed shell. */
    public static boolean markStructure(ServerPlayer player, String structureId) {
        String id = normalize(structureId);
        if (player == null || MultiblockStructureCatalog.builtin().find(id).isEmpty()) {
            return false;
        }
        LocatedStructure located = findWorldAnchor(player, id)
                .orElseGet(() -> findNearbyFormed(player, id).orElse(null));
        if (located == null) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.dialogue.location_unverified"), false);
            return false;
        }
        ServerLevel level = player.server.getLevel(ResourceKey.create(Registries.DIMENSION,
                ResourceLocation.tryParse(located.dimension())));
        // Only a really formed and formally commissioned structure at the located dimension
        // may be marked; a mere coordinate anchor or a bare single-core shell is never enough.
        if (level == null || !MultiblockStationService.isStationFormed(level, id, located.pos())
                || !MultiblockOperationalService.isCommissioned(level, id, located.pos())) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.dialogue.location_unverified"), false);
            return false;
        }
        CompoundTag markers = player.getPersistentData().getCompound(MARKERS_TAG).copy();
        CompoundTag marker = new CompoundTag();
        marker.putString("Dimension", located.dimension());
        marker.putLong("Pos", located.pos().asLong());
        marker.putLong("MarkedAt", player.serverLevel().getGameTime());
        markers.put(id, marker);
        player.getPersistentData().put(MARKERS_TAG, markers);
        NpcDialogueFlags.setFlag(player, "structure_marked_" + id);
        com.xunxian.seekingimmortals.quest.DetailedQuestProofService.recordStructureFormed(
                player, id, located.dimension(), located.pos().asLong());
        player.displayClientMessage(Component.translatable(
                "message.seeking_immortals.dialogue.location_marked"), false);
        return true;
    }

    public static boolean recordHint(ServerPlayer player, String hintId) {
        String id = normalize(hintId);
        if (player == null || id.isBlank()) {
            return false;
        }
        putBoundedBoolean(player, HINTS_TAG, id);
        NpcDialogueFlags.setFlag(player, "hint_" + id);
        player.displayClientMessage(Component.translatable(
                "message.seeking_immortals.dialogue.hint_recorded", id), false);
        return true;
    }

    public static boolean recordAnomaly(ServerPlayer player, String npcId, String treeId, String nodeId) {
        if (player == null) {
            return false;
        }
        String id = firstNonBlank(normalize(treeId) + ":" + normalize(nodeId), normalize(npcId));
        if (id.replace(":", "").isBlank()) {
            return false;
        }
        putBoundedBoolean(player, ANOMALIES_TAG, id);
        NpcDialogueFlags.setFlag(player, "anomaly_" + normalize(nodeId));
        player.displayClientMessage(Component.translatable(
                "message.seeking_immortals.dialogue.anomaly_recorded"), false);
        return true;
    }

    public static int addSuspicion(ServerPlayer player, String authorityId, int amount) {
        if (player == null) {
            return 0;
        }
        String id = normalize(authorityId);
        if (id.isBlank()) {
            id = "world";
        }
        CompoundTag root = player.getPersistentData().getCompound(SUSPICION_TAG).copy();
        int next = Math.max(0, Math.min(MAX_SUSPICION, root.getInt(id) + Math.max(0, amount)));
        root.putInt(id, next);
        player.getPersistentData().put(SUSPICION_TAG, root);
        NpcDialogueFlags.setFlag(player, "suspicion_recorded");
        player.displayClientMessage(Component.translatable(
                "message.seeking_immortals.dialogue.suspicion_changed", next, MAX_SUSPICION), false);
        return next;
    }

    public static int suspicion(ServerPlayer player, String authorityId) {
        return player == null ? 0 : player.getPersistentData().getCompound(SUSPICION_TAG)
                .getInt(normalize(authorityId));
    }

    /** Spawns at most one shell per action and two dialogue hostiles per player in the area. */
    public static boolean triggerCombat(ServerPlayer player, String npcId, String treeId, String actionType) {
        if (player == null || player.serverLevel().getDifficulty() == Difficulty.PEACEFUL) {
            return false;
        }
        String action = normalize(firstNonBlank(treeId, npcId, "dialogue")) + ":" + normalize(actionType);
        ServerLevel level = player.serverLevel();
        AABB area = player.getBoundingBox().inflate(48.0D, 16.0D, 48.0D);
        int boundCount = 0;
        for (Mob mob : level.getEntitiesOfClass(Mob.class, area, DialogueWorldActionService::isDialogueHostile)) {
            CompoundTag data = mob.getPersistentData();
            if (!data.hasUUID(HOSTILE_PLAYER) || !player.getUUID().equals(data.getUUID(HOSTILE_PLAYER))) {
                continue;
            }
            boundCount++;
            if (action.equals(data.getString(HOSTILE_ACTION))) {
                if (!player.isCreative() && !player.isSpectator()) {
                    mob.setTarget(player);
                }
                return true;
            }
        }
        long now = level.getGameTime();
        CompoundTag combat = player.getPersistentData().getCompound(COMBAT_TAG).copy();
        long previous = combat.getLong(action);
        if (previous > 0L && now >= previous && now - previous < COMBAT_COOLDOWN_TICKS) {
            return true;
        }
        if (boundCount >= MAX_BOUND_HOSTILES || player.isCreative() || player.isSpectator()) {
            return false;
        }

        Mob hostile = TrialCombatShellService.spawnHostile(level, player.blockPosition().offset(2, 0, 2),
                player.getYRot(), "dialogue_" + normalize(actionType), 32.0D, 6.0D,
                SummonedServitorEntity.Archetype.GENERIC);
        if (hostile == null) {
            return false;
        }
        hostile.getPersistentData().putBoolean(HOSTILE_MARKER, true);
        hostile.getPersistentData().putUUID(HOSTILE_PLAYER, player.getUUID());
        hostile.getPersistentData().putString(HOSTILE_ACTION, action);
        hostile.setPersistenceRequired();
        hostile.setTarget(player);
        combat.putLong(action, now);
        trimOldest(combat);
        player.getPersistentData().put(COMBAT_TAG, combat);
        NpcDialogueFlags.setFlag(player, "dialogue_combat_" + normalize(actionType));
        applyHostilityPenalty(player, npcId);
        return true;
    }

    /**
     * D-A: territory-bound guard. Currently delegates to the hostile shell path so behaviour
     * is unchanged; D-A-4 replaces this with a guard entity bound to faction territory and
     * enforcement target.
     */
    public static boolean callGuard(ServerPlayer player, String npcId, String treeId) {
        return triggerCombat(player, npcId, treeId, "call_guard");
    }

    /**
     * D-A: combat_flag only establishes hostile consequences (ledger + penalty + marker).
     * It never fabricates an arrest or spawns an entity by itself.
     */
    public static boolean combatFlag(ServerPlayer player, String npcId, String treeId) {
        if (player == null) {
            return false;
        }
        String action = normalize(firstNonBlank(treeId, npcId, "dialogue")) + ":combat_flag";
        CompoundTag combat = player.getPersistentData().getCompound(COMBAT_TAG).copy();
        combat.putLong(action, player.serverLevel().getGameTime());
        trimOldest(combat);
        player.getPersistentData().put(COMBAT_TAG, combat);
        NpcDialogueFlags.setFlag(player, "dialogue_combat_flag");
        applyHostilityPenalty(player, npcId);
        player.displayClientMessage(Component.translatable(
                "message.seeking_immortals.dialogue.tension"), false);
        return true;
    }

    /**
     * D-A: combat vs arrest decision point. Currently delegates to the hostile shell path so
     * behaviour is unchanged; D-A-4 adds the warn/fine/arrest branches driven by suspicion.
     */
    public static boolean combatOrArrest(ServerPlayer player, String npcId, String treeId) {
        return triggerCombat(player, npcId, treeId, "combat_or_arrest");
    }

    public static void applyHostilityPenalty(ServerPlayer player, String npcId) {
        if (player == null) {
            return;
        }
        NpcFavorService.add(player, npcId, -5);
        NamedNpcRegistry.find(npcId).map(NamedNpcRegistry.NamedNpc::reputationTrack)
                .filter(value -> !value.isBlank())
                .ifPresent(rep -> {
                    String key = com.xunxian.seekingimmortals.sect.ReputationUnlockService.reputationKey(rep);
                    ReputationService.add(player, key == null || key.isBlank() ? normalize(rep) : key, -3);
                });
    }

    static boolean isDialogueHostile(Mob mob) {
        return mob != null && mob.isAlive() && mob.getPersistentData().getBoolean(HOSTILE_MARKER);
    }

    private static Optional<LocatedStructure> findWorldAnchor(ServerPlayer player, String structureId) {
        WorldpackSavedData.Anchor anchor = WorldpackSavedData.get(player.serverLevel())
                .getAnchor(structureId).orElse(null);
        if (anchor == null) {
            return Optional.empty();
        }
        ResourceLocation dimensionId = ResourceLocation.tryParse(anchor.dimension());
        if (dimensionId == null || player.getServer() == null) {
            return Optional.empty();
        }
        ServerLevel level = player.getServer().getLevel(ResourceKey.create(Registries.DIMENSION, dimensionId));
        BlockPos pos = BlockPos.containing(anchor.x(), anchor.y(), anchor.z());
        return level == null || !level.isInWorldBounds(pos)
                ? Optional.empty() : Optional.of(new LocatedStructure(anchor.dimension(), pos));
    }

    private static Optional<LocatedStructure> findNearbyFormed(ServerPlayer player, String structureId) {
        Entity source = NpcDialogueApi.currentSourceEntity(player).orElse(player);
        BlockPos center = source.blockPosition();
        ServerLevel level = player.serverLevel();
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        for (int dx = -STRUCTURE_SCAN_RADIUS; dx <= STRUCTURE_SCAN_RADIUS; dx++) {
            for (int dy = -STRUCTURE_SCAN_Y; dy <= STRUCTURE_SCAN_Y; dy++) {
                for (int dz = -STRUCTURE_SCAN_RADIUS; dz <= STRUCTURE_SCAN_RADIUS; dz++) {
                    BlockPos candidate = center.offset(dx, dy, dz);
                    double distance = candidate.distSqr(center);
                    if (distance >= bestDistance || !level.hasChunkAt(candidate)) {
                        continue;
                    }
                    if (MultiblockStationService.isStationFormed(level, structureId, candidate)) {
                        best = candidate.immutable();
                        bestDistance = distance;
                    }
                }
            }
        }
        return best == null ? Optional.empty() : Optional.of(new LocatedStructure(
                level.dimension().location().toString(), best));
    }

    private static void putBoundedBoolean(ServerPlayer player, String rootKey, String id) {
        CompoundTag root = player.getPersistentData().getCompound(rootKey).copy();
        root.putBoolean(id, true);
        trimOldest(root);
        player.getPersistentData().put(rootKey, root);
    }

    private static void trimOldest(CompoundTag root) {
        while (root.getAllKeys().size() > MAX_LOG_ENTRIES) {
            root.getAllKeys().stream().sorted().findFirst().ifPresent(root::remove);
        }
    }

    private static void copyTag(CompoundTag source, CompoundTag target, String key) {
        if (source.contains(key) && source.get(key) != null) {
            target.put(key, source.get(key).copy());
        }
    }

    private static String firstNonBlank(String... values) {
        if (values != null) {
            for (String value : values) {
                if (value != null && !value.isBlank()) {
                    return value.trim();
                }
            }
        }
        return "";
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private record LocatedStructure(String dimension, BlockPos pos) {}
}
