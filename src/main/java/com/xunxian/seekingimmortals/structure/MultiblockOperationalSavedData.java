package com.xunxian.seekingimmortals.structure;

import com.xunxian.seekingimmortals.SeekingImmortalsMod;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Persistent operational state ledger for multiblock stations.
 * Keyed by dimension + stationId + packed origin.
 */
public final class MultiblockOperationalSavedData extends SavedData {
    private static final String DATA_NAME = SeekingImmortalsMod.MODID + "_multiblock_ops";

    public enum OpState {
        INTACT,
        DAMAGED,
        CRITICAL,
        DISABLED;

        public static OpState fromId(String raw) {
            if (raw == null || raw.isBlank()) {
                return INTACT;
            }
            return switch (raw.trim().toLowerCase(Locale.ROOT)) {
                case "damaged" -> DAMAGED;
                case "critical" -> CRITICAL;
                case "disabled", "destroyed" -> DISABLED;
                default -> INTACT;
            };
        }

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public record StationState(String dimensionId, String stationId, long packedOrigin,
                               OpState state, int hp, int maxHp) {
        public StationState {
            dimensionId = dimensionId == null ? "" : dimensionId;
            stationId = stationId == null ? "" : stationId.trim().toLowerCase(Locale.ROOT);
            state = state == null ? OpState.INTACT : state;
            maxHp = Math.max(1, maxHp);
            hp = Math.max(0, Math.min(maxHp, hp));
        }

        public double efficiency() {
            return switch (state) {
                case INTACT -> 1.0D;
                case DAMAGED -> 0.60D;
                case CRITICAL -> 0.20D;
                case DISABLED -> 0.0D;
            };
        }
    }

    private final Map<String, StationState> stations = new LinkedHashMap<>();

    public static MultiblockOperationalSavedData get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(
                MultiblockOperationalSavedData::load,
                MultiblockOperationalSavedData::new,
                DATA_NAME);
    }

    public static MultiblockOperationalSavedData load(CompoundTag tag) {
        MultiblockOperationalSavedData data = new MultiblockOperationalSavedData();
        ListTag list = tag.getList("Stations", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            String dim = entry.getString("Dim");
            String stationId = entry.getString("StationId");
            long packed = entry.getLong("Origin");
            OpState state = OpState.fromId(entry.getString("State"));
            int maxHp = Math.max(1, entry.getInt("MaxHp"));
            int hp = entry.contains("Hp") ? entry.getInt("Hp") : maxHp;
            data.stations.put(key(dim, stationId, packed),
                    new StationState(dim, stationId, packed, state, hp, maxHp));
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (StationState state : stations.values()) {
            CompoundTag entry = new CompoundTag();
            entry.putString("Dim", state.dimensionId());
            entry.putString("StationId", state.stationId());
            entry.putLong("Origin", state.packedOrigin());
            entry.putString("State", state.state().id());
            entry.putInt("Hp", state.hp());
            entry.putInt("MaxHp", state.maxHp());
            list.add(entry);
        }
        tag.put("Stations", list);
        return tag;
    }

    public static String key(String dimensionId, String stationId, long packedOrigin) {
        String dim = dimensionId == null ? "" : dimensionId;
        String id = stationId == null ? "" : stationId.trim().toLowerCase(Locale.ROOT);
        return dim + "|" + id + "|" + packedOrigin;
    }

    public Optional<StationState> find(String dimensionId, String stationId, BlockPos origin) {
        if (origin == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(stations.get(key(dimensionId, stationId, origin.asLong())));
    }

    public StationState upsert(StationState state) {
        if (state == null) {
            return null;
        }
        stations.put(key(state.dimensionId(), state.stationId(), state.packedOrigin()), state);
        setDirty();
        return state;
    }

    public void remove(String dimensionId, String stationId, BlockPos origin) {
        if (origin == null) {
            return;
        }
        if (stations.remove(key(dimensionId, stationId, origin.asLong())) != null) {
            setDirty();
        }
    }

    public int size() {
        return stations.size();
    }
}
