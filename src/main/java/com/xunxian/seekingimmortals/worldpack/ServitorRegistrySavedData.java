package com.xunxian.seekingimmortals.worldpack;

import com.xunxian.seekingimmortals.SeekingImmortalsMod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Server-wide ownership index so unloaded or remote servitors still count toward the cap. */
public final class ServitorRegistrySavedData extends SavedData {
    private static final String DATA_NAME = SeekingImmortalsMod.MODID + "_servitor_registry";
    private final Map<UUID, State> servitors = new LinkedHashMap<>();

    public record State(UUID ownerId, UUID entityId, String dimensionId, String stance, boolean dismissed) {}

    public static ServitorRegistrySavedData get(ServerLevel level) {
        return get(level.getServer());
    }

    public static ServitorRegistrySavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                ServitorRegistrySavedData::load,
                ServitorRegistrySavedData::new,
                DATA_NAME);
    }

    public static ServitorRegistrySavedData load(CompoundTag tag) {
        ServitorRegistrySavedData data = new ServitorRegistrySavedData();
        ListTag entries = tag.getList("Servitors", Tag.TAG_COMPOUND);
        for (int i = 0; i < entries.size(); i++) {
            CompoundTag entry = entries.getCompound(i);
            if (!entry.hasUUID("Owner") || !entry.hasUUID("Entity")) {
                continue;
            }
            UUID ownerId = entry.getUUID("Owner");
            UUID entityId = entry.getUUID("Entity");
            data.servitors.put(entityId, new State(
                    ownerId,
                    entityId,
                    entry.getString("Dimension"),
                    cleanStance(entry.getString("Stance")),
                    entry.getBoolean("Dismissed")));
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag entries = new ListTag();
        for (State state : servitors.values()) {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("Owner", state.ownerId());
            entry.putUUID("Entity", state.entityId());
            entry.putString("Dimension", state.dimensionId() == null ? "" : state.dimensionId());
            entry.putString("Stance", cleanStance(state.stance()));
            entry.putBoolean("Dismissed", state.dismissed());
            entries.add(entry);
        }
        tag.put("Servitors", entries);
        return tag;
    }

    /** Registers a new/legacy entity; excess legacy entities become deferred dismissals. */
    public State register(UUID ownerId, UUID entityId, String dimensionId, String stance, int maxActive) {
        if (ownerId == null || entityId == null) {
            return null;
        }
        State current = servitors.get(entityId);
        if (current != null && current.dismissed()) {
            return current;
        }
        boolean dismiss = current == null && countActive(ownerId) >= Math.max(1, maxActive);
        String desiredStance = current == null ? cleanStance(stance) : current.stance();
        State next = new State(ownerId, entityId, dimensionId == null ? "" : dimensionId,
                desiredStance, dismiss);
        if (!next.equals(current)) {
            servitors.put(entityId, next);
            setDirty();
        }
        return next;
    }

    public Optional<State> state(UUID entityId) {
        return Optional.ofNullable(entityId == null ? null : servitors.get(entityId));
    }

    public List<State> activeFor(UUID ownerId) {
        List<State> result = new ArrayList<>();
        if (ownerId == null) {
            return result;
        }
        for (State state : servitors.values()) {
            if (ownerId.equals(state.ownerId()) && !state.dismissed()) {
                result.add(state);
            }
        }
        return result;
    }

    public int countActive(UUID ownerId) {
        return activeFor(ownerId).size();
    }

    public List<State> dismissOldest(UUID ownerId, int count) {
        List<State> changed = new ArrayList<>();
        if (ownerId == null || count <= 0) {
            return changed;
        }
        for (State state : new ArrayList<>(servitors.values())) {
            if (changed.size() >= count) {
                break;
            }
            if (!ownerId.equals(state.ownerId()) || state.dismissed()) {
                continue;
            }
            State dismissed = new State(state.ownerId(), state.entityId(), state.dimensionId(), state.stance(), true);
            servitors.put(state.entityId(), dismissed);
            changed.add(dismissed);
        }
        if (!changed.isEmpty()) {
            setDirty();
        }
        return changed;
    }

    public Optional<State> dismiss(UUID ownerId, UUID entityId) {
        State state = entityId == null ? null : servitors.get(entityId);
        if (ownerId == null || entityId == null) {
            return Optional.empty();
        }
        if (state == null) {
            State tombstone = new State(ownerId, entityId, "", "FOLLOW", true);
            servitors.put(entityId, tombstone);
            setDirty();
            return Optional.of(tombstone);
        }
        if (!ownerId.equals(state.ownerId())) {
            return Optional.empty();
        }
        if (state.dismissed()) {
            return Optional.of(state);
        }
        State dismissed = new State(state.ownerId(), state.entityId(), state.dimensionId(), state.stance(), true);
        servitors.put(entityId, dismissed);
        setDirty();
        return Optional.of(dismissed);
    }

    public List<State> dismissAll(UUID ownerId) {
        return dismissOldest(ownerId, countActive(ownerId));
    }

    public int setStanceAll(UUID ownerId, String stance) {
        if (ownerId == null) {
            return 0;
        }
        String clean = cleanStance(stance);
        int changed = 0;
        for (State state : new ArrayList<>(servitors.values())) {
            if (!ownerId.equals(state.ownerId()) || state.dismissed()) {
                continue;
            }
            State next = new State(state.ownerId(), state.entityId(), state.dimensionId(), clean, false);
            if (!next.equals(state)) {
                servitors.put(state.entityId(), next);
                changed++;
            }
        }
        if (changed > 0) {
            setDirty();
        }
        return countActive(ownerId);
    }

    public void setStance(UUID entityId, String stance) {
        State state = entityId == null ? null : servitors.get(entityId);
        if (state == null || state.dismissed()) {
            return;
        }
        State next = new State(state.ownerId(), state.entityId(), state.dimensionId(), cleanStance(stance), false);
        if (!next.equals(state)) {
            servitors.put(entityId, next);
            setDirty();
        }
    }

    public void remove(UUID entityId) {
        if (entityId != null && servitors.remove(entityId) != null) {
            setDirty();
        }
    }

    private static String cleanStance(String stance) {
        String value = stance == null ? "FOLLOW" : stance.trim().toUpperCase(java.util.Locale.ROOT);
        return switch (value) {
            case "GUARD", "AGGRESSIVE", "STAY" -> value;
            default -> "FOLLOW";
        };
    }
}
