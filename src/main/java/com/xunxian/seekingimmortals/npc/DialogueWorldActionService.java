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
    public static final String ARRESTS_TAG = "seeking_immortals_dialogue_arrests";
    public static final String HOSTILE_MARKER = "seeking_immortals_dialogue_hostile";
    public static final String HOSTILE_PLAYER = "seeking_immortals_dialogue_hostile_player";
    public static final String HOSTILE_ACTION = "seeking_immortals_dialogue_hostile_action";
    private static final String GUARD_MARKER = "seeking_immortals_dialogue_guard";
    private static final int STRUCTURE_SCAN_RADIUS = 10;
    private static final int STRUCTURE_SCAN_Y = 4;
    private static final int MAX_SUSPICION = 100;
    private static final int MAX_LOG_ENTRIES = 48;
    private static final int MAX_BOUND_HOSTILES = 2;
    private static final int MAX_GUARDS_PER_FACTION = 1;
    private static final long GUARD_LIFE_TICKS = 20L * 60L * 20L;
    private static final long COMBAT_COOLDOWN_TICKS = 20L * 60L * 5L;
    /** How much a faction suspicion bucket decays per game-hour (settlement point). */
    private static final int SUSPICION_DECAY_PER_HOUR = 12;
    private static final long TICKS_PER_HOUR = 20L * 60L * 60L;
    /** Suspicion at/above this level escalates dialogue enforcement to a fine/warning. */
    public static final int WARN_SUSPICION_THRESHOLD = 30;
    /** Suspicion at/above this level escalates dialogue enforcement to an arrest. */
    public static final int ARREST_SUSPICION_THRESHOLD = 60;

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
        copyTag(source, target, ARRESTS_TAG);
    }

    /** Marks only a known structure backed by a world anchor or a nearby formed shell. */
    public static boolean markStructure(ServerPlayer player, String structureId) {
        return markStructure(player, structureId, "", "");
    }

    /**
     * D-A: marks a structure only when it matches every intended dimension of the dialogue
     * context (structure type, optional authored dimension) and is the current detailed-quest
     * step. A bare coordinate anchor or unrelated lookalike structure is never enough.
     */
    public static boolean markStructure(ServerPlayer player, String structureId,
                                        String authorType, String authorDimension) {
        String id = normalize(structureId);
        if (player == null || MultiblockStructureCatalog.builtin().find(id).isEmpty()) {
            return false;
        }
        MultiblockStructureCatalog.StructureEntry entry = MultiblockStructureCatalog.builtin().find(id).get();
        if (!authorType.isBlank() && !entry.type().equalsIgnoreCase(authorType)) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.dialogue.structure_type_mismatch", id, authorType), false);
            return false;
        }
        LocatedStructure located = findWorldAnchor(player, id)
                .orElseGet(() -> findNearbyFormed(player, id).orElse(null));
        if (located == null) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.dialogue.location_unverified"), false);
            return false;
        }
        if (!authorDimension.isBlank() && !normalize(located.dimension()).contains(normalize(authorDimension))) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.dialogue.structure_dimension_mismatch",
                    id, located.dimension(), authorDimension), false);
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
        boolean alreadyMarked = markers.contains(id);
        CompoundTag marker = new CompoundTag();
        marker.putString("Dimension", located.dimension());
        marker.putLong("Pos", located.pos().asLong());
        marker.putLong("MarkedAt", player.serverLevel().getGameTime());
        markers.put(id, marker);
        player.getPersistentData().put(MARKERS_TAG, markers);
        NpcDialogueFlags.setFlag(player, "structure_marked_" + id);
        if (!alreadyMarked) {
            com.xunxian.seekingimmortals.quest.DetailedQuestProofService.recordStructureFormed(
                    player, id, located.dimension(), located.pos().asLong());
        }
        player.displayClientMessage(Component.translatable(
                "message.seeking_immortals.dialogue.location_marked"), false);
        return true;
    }

    /**
     * Records a hint bound to its dialogue source. Returns true only on first record; repeated
     * visits to the same node never double-record or double-advance.
     */
    public static boolean recordHint(ServerPlayer player, String hintId, String npcId, String nodeId) {
        String id = normalize(hintId);
        if (player == null || id.isBlank()) {
            return false;
        }
        CompoundTag hints = player.getPersistentData().getCompound(HINTS_TAG).copy();
        boolean added = !hints.contains(id);
        if (added) {
            hints.put(id, hintEntry(npcId, nodeId, player));
        }
        trimOldestByAge(hints);
        player.getPersistentData().put(HINTS_TAG, hints);
        if (added) {
            NpcDialogueFlags.setFlag(player, "hint_" + id);
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.dialogue.hint_recorded", id), false);
        }
        return added;
    }

    private static CompoundTag hintEntry(String npcId, String nodeId, ServerPlayer player) {
        CompoundTag entry = new CompoundTag();
        entry.putLong("At", player.serverLevel().getGameTime());
        if (npcId != null && !npcId.isBlank()) {
            entry.putString("Npc", normalize(npcId));
        }
        if (nodeId != null && !nodeId.isBlank()) {
            entry.putString("Node", normalize(nodeId));
        }
        return entry;
    }

    private static void trimOldestByAge(CompoundTag root) {
        while (root.getAllKeys().size() > MAX_LOG_ENTRIES) {
            String evict = null;
            long oldest = Long.MAX_VALUE;
            for (String key : root.getAllKeys()) {
                long at = root.getCompound(key).getLong("At");
                if (at < oldest) {
                    oldest = at;
                    evict = key;
                }
            }
            if (evict != null) {
                root.remove(evict);
            } else {
                break;
            }
        }
    }

    public static boolean recordAnomaly(ServerPlayer player, String npcId, String treeId, String nodeId) {
        if (player == null) {
            return false;
        }
        // Bucket by enforcement authority (NPC/faction) first so repeated anomalies from the
        // same party settle together instead of scattering per raw node id.
        String id = firstNonBlank(normalize(npcId), normalize(treeId) + ":" + normalize(nodeId));
        if (id.replace(":", "").isBlank()) {
            return false;
        }
        addBoundedEntry(player, ANOMALIES_TAG, id, npcId, nodeId);
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
        long now = player.serverLevel().getGameTime();
        CompoundTag root = player.getPersistentData().getCompound(SUSPICION_TAG).copy();
        CompoundTag entry = root.getCompound(id);
        int current = decayedSuspicion(entry.getInt("Value"), entry.getLong("LastAt"), now);
        int next = Math.max(0, Math.min(MAX_SUSPICION, current + Math.max(0, amount)));
        CompoundTag updated = new CompoundTag();
        updated.putInt("Value", next);
        updated.putLong("LastAt", now);
        root.put(id, updated);
        player.getPersistentData().put(SUSPICION_TAG, root);
        NpcDialogueFlags.setFlag(player, "suspicion_recorded");
        player.displayClientMessage(Component.translatable(
                "message.seeking_immortals.dialogue.suspicion_changed", next, MAX_SUSPICION), false);
        return next;
    }

    /** Current suspicion for an authority, time-decayed to now (read-only settlement view). */
    public static int suspicion(ServerPlayer player, String authorityId) {
        if (player == null) {
            return 0;
        }
        CompoundTag root = player.getPersistentData().getCompound(SUSPICION_TAG);
        CompoundTag entry = root.getCompound(normalize(authorityId));
        return decayedSuspicion(entry.getInt("Value"), entry.getLong("LastAt"),
                player.serverLevel().getGameTime());
    }

    /**
     * D-A suspicion level: 0 = clear, 1 = warn/fine territory, 2 = arrest territory.
     * Consumed by {@link #combatOrArrest} to choose warning vs fine vs arrest.
     */
    public static int suspectLevel(ServerPlayer player, String authorityId) {
        int value = suspicion(player, authorityId);
        if (value >= ARREST_SUSPICION_THRESHOLD) {
            return 2;
        }
        if (value >= WARN_SUSPICION_THRESHOLD) {
            return 1;
        }
        return 0;
    }

    private static int decayedSuspicion(int value, long lastAt, long now) {
        if (value <= 0 || lastAt <= 0 || now <= lastAt) {
            return value;
        }
        long elapsed = now - lastAt;
        int hours = (int) (elapsed / TICKS_PER_HOUR);
        if (hours <= 0) {
            return value;
        }
        return Math.max(0, value - hours * SUSPICION_DECAY_PER_HOUR);
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
        trimOldestCombat(combat);
        player.getPersistentData().put(COMBAT_TAG, combat);
        NpcDialogueFlags.setFlag(player, "dialogue_combat_" + normalize(actionType));
        applyHostilityPenalty(player, npcId);
        return true;
    }

    /**
     * D-A: territory-bound guard. Summons an owner-bound guard that stands GUARD at the spot
     * and retaliates against attackers; it never attacks the protected player and is capped
     * per faction so repeated dialogue visits do not farm guards.
     */
    public static boolean callGuard(ServerPlayer player, String npcId, String treeId) {
        if (player == null || player.serverLevel().getDifficulty() == Difficulty.PEACEFUL) {
            return false;
        }
        String faction = normalize(firstNonBlank(npcId, treeId, "world"));
        if (faction.isBlank()) {
            faction = "world";
        }
        ServerLevel level = player.serverLevel();
        AABB area = player.getBoundingBox().inflate(24.0D, 16.0D, 24.0D);
        int guardCount = 0;
        for (Mob mob : level.getEntitiesOfClass(Mob.class, area)) {
            CompoundTag tag = mob.getPersistentData();
            if (tag.getBoolean(GUARD_MARKER) && player.getUUID().equals(tag.getUUID("GuardOwner"))) {
                guardCount++;
            }
        }
        if (guardCount >= MAX_GUARDS_PER_FACTION) {
            return true;
        }
        com.xunxian.seekingimmortals.registry.ModEntities.SUMMONED_SERVITOR.get();
        SummonedServitorEntity guard = com.xunxian.seekingimmortals.registry.ModEntities.SUMMONED_SERVITOR.get().create(level);
        if (guard == null) {
            return false;
        }
        guard.setPos(player.getX() + 2.0D, player.getY(), player.getZ() + 2.0D);
        guard.configure(player, "dialogue_guard_" + faction, (int) GUARD_LIFE_TICKS, 48.0D, 7.0D,
                TrialCombatShellService.archetypeFor(faction));
        guard.setStance(SummonedServitorEntity.Stance.GUARD);
        CompoundTag guardTag = guard.getPersistentData();
        guardTag.putBoolean(GUARD_MARKER, true);
        guardTag.putUUID("GuardOwner", player.getUUID());
        guardTag.putString("GuardFaction", faction);
        guard.setPersistenceRequired();
        if (level.addFreshEntity(guard)) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.dialogue.guard_called", factionDisplay(faction)), false);
            return true;
        }
        return false;
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
        trimOldestCombat(combat);
        player.getPersistentData().put(COMBAT_TAG, combat);
        NpcDialogueFlags.setFlag(player, "dialogue_combat_flag");
        applyHostilityPenalty(player, npcId);
        player.displayClientMessage(Component.translatable(
                "message.seeking_immortals.dialogue.tension"), false);
        return true;
    }

    /**
     * D-A: combat vs arrest decision point driven by suspicion level.
     * <ul>
     *   <li>clear (0) → warning, reputation/favor penalty only</li>
     *   <li>warn (1) → fine (spirit stones/contribution) plus penalty</li>
     *   <li>arrest (2) → arrest: persistent marker, a stationed guard, recoverable spot,
     *       explicit release via settle on the same authority</li>
     *   <li>already-arrested or hostile-leaning → existing combat shell</li>
     * </ul>
     */
    public static boolean combatOrArrest(ServerPlayer player, String npcId, String treeId) {
        if (player == null) {
            return false;
        }
        String authority = normalize(firstNonBlank(npcId, treeId, "world"));
        if (authority.isBlank()) {
            authority = "world";
        }
        int level = suspectLevel(player, authority);
        if (level >= 2 || isArrested(player, authority)) {
            if (isArrested(player, authority)) {
                player.displayClientMessage(Component.translatable(
                        "message.seeking_immortals.dialogue.already_arrested"), true);
                return true;
            }
            arrestPlayer(player, authority);
            return true;
        }
        if (level == 1) {
            finePlayer(player, authority);
            return true;
        }
        // Clear suspicion: a warning and a small penalty, or combat if a combat flag already stands.
        if (player.getPersistentData().getCompound(COMBAT_TAG)
                .getLong(normalize(authority) + ":combat_flag") > 0L) {
            return triggerCombat(player, npcId, treeId, "combat_or_arrest");
        }
        warnPlayer(player, authority);
        return true;
    }

    private static void warnPlayer(ServerPlayer player, String authority) {
        applyHostilityPenalty(player, authority);
        player.displayClientMessage(Component.translatable(
                "message.seeking_immortals.dialogue.warned", factionDisplay(authority)), true);
    }

    private static void finePlayer(ServerPlayer player, String authority) {
        com.xunxian.seekingimmortals.quest.QuestProgress progress = null;
        var cultivation = com.xunxian.seekingimmortals.cultivation.CultivationHelper.get(player).orElse(null);
        if (cultivation != null) {
            progress = cultivation.getSevenMysteriesQuest();
        }
        int fine = 0;
        if (progress != null) {
            int available = progress.getContribution();
            fine = Math.min(20, available);
            if (fine > 0) {
                progress.spendContribution(fine);
            }
        }
        if (fine <= 0) {
            finePlayerStones(player, 15);
        }
        applyHostilityPenalty(player, authority);
        player.displayClientMessage(Component.translatable(
                "message.seeking_immortals.dialogue.fined", fine), true);
        clearSuspicion(player, authority);
    }

    /** D-A: arrest marks the player for an authority with a recoverable spot and a guard. */
    public static boolean arrestPlayer(ServerPlayer player, String authority) {
        if (player == null) {
            return false;
        }
        CompoundTag arrests = player.getPersistentData().getCompound(ARRESTS_TAG).copy();
        CompoundTag entry = new CompoundTag();
        entry.putString("Authority", normalize(authority));
        entry.putLong("At", player.serverLevel().getGameTime());
        entry.putString("Dimension", player.level().dimension().location().toString());
        entry.putLong("RecoverX", player.getBlockX());
        entry.putLong("RecoverY", player.getBlockY());
        entry.putLong("RecoverZ", player.getBlockZ());
        arrests.put(normalize(authority), entry);
        player.getPersistentData().put(ARRESTS_TAG, arrests);
        callGuard(player, authority, "arrest");
        clearSuspicion(player, authority);
        player.displayClientMessage(Component.translatable(
                "message.seeking_immortals.dialogue.arrested", factionDisplay(authority)), true);
        return true;
    }

    /** D-A: explicit arrest release; settles the marker so a later encounter can start fresh. */
    public static boolean settleArrest(ServerPlayer player, String authority) {
        if (player == null) {
            return false;
        }
        String id = normalize(authority);
        CompoundTag arrests = player.getPersistentData().getCompound(ARRESTS_TAG).copy();
        if (!arrests.contains(id)) {
            return false;
        }
        CompoundTag entry = arrests.getCompound(id);
        if (entry.contains("RecoverX")) {
            try {
                var dimension = net.minecraft.resources.ResourceKey.create(
                        net.minecraft.core.registries.Registries.DIMENSION,
                        net.minecraft.resources.ResourceLocation.tryParse(entry.getString("Dimension")));
                net.minecraft.server.level.ServerLevel level =
                        player.getServer() == null ? null : player.getServer().getLevel(dimension);
                if (level != null) {
                    player.teleportTo(level, entry.getLong("RecoverX") + 0.5D,
                            entry.getLong("RecoverY"), entry.getLong("RecoverZ") + 0.5D,
                            java.util.EnumSet.noneOf(net.minecraft.world.entity.RelativeMovement.class),
                            player.getYRot(), player.getXRot());
                }
            } catch (RuntimeException ignored) {
                // stay in place if the recorded dimension is unavailable
            }
        }
        arrests.remove(id);
        player.getPersistentData().put(ARRESTS_TAG, arrests);
        player.displayClientMessage(Component.translatable(
                "message.seeking_immortals.dialogue.arrest_released", factionDisplay(authority)), true);
        return true;
    }

    public static boolean isArrested(ServerPlayer player, String authority) {
        return player != null && player.getPersistentData().getCompound(ARRESTS_TAG)
                .contains(normalize(authority));
    }

    private static void clearSuspicion(ServerPlayer player, String authority) {
        CompoundTag root = player.getPersistentData().getCompound(SUSPICION_TAG).copy();
        root.remove(normalize(authority));
        player.getPersistentData().put(SUSPICION_TAG, root);
    }

    private static void finePlayerStones(ServerPlayer player, int amount) {
        var item = com.xunxian.seekingimmortals.registry.ModItems.METAL_SPIRIT_STONE.get();
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            var stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.is(item)) {
                int remove = Math.min(amount, stack.getCount());
                stack.shrink(remove);
                amount -= remove;
                if (amount <= 0) {
                    break;
                }
            }
        }
    }

    private static String factionDisplay(String authority) {
        return switch (normalize(authority)) {
            case "heavenly_inspector" -> "天庭巡查";
            case "market_vendor" -> "坊市管事";
            case "tianyuan_registrar" -> "天渊执事";
            case "inverse_star_contact" -> "逆星接引";
            default -> authority;
        };
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

    private static void addBoundedEntry(ServerPlayer player, String rootKey, String id,
                                        String npcId, String nodeId) {
        CompoundTag root = player.getPersistentData().getCompound(rootKey).copy();
        root.put(id, hintEntry(npcId, nodeId, player));
        trimOldestByAge(root);
        player.getPersistentData().put(rootKey, root);
    }

    private static void trimOldestCombat(CompoundTag root) {
        while (root.getAllKeys().size() > MAX_LOG_ENTRIES) {
            String evict = null;
            long oldest = Long.MAX_VALUE;
            for (String key : root.getAllKeys()) {
                long at = root.getLong(key);
                if (at < oldest) {
                    oldest = at;
                    evict = key;
                }
            }
            if (evict != null) {
                root.remove(evict);
            } else {
                break;
            }
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
