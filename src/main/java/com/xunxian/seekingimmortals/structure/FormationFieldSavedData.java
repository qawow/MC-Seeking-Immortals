package com.xunxian.seekingimmortals.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Persistent formation field records for Wave49 Phase19 depth.
 * Survives restart; runtime pulse still driven by FormationFieldService.
 */
public final class FormationFieldSavedData extends SavedData {
    private static final String DATA_NAME = "seeking_immortals_formation_fields";

    public record StoredField(String dimensionId, BlockPos corePos, String kind, int remainingTicks) {}

    private final List<StoredField> fields = new ArrayList<>();

    public static FormationFieldSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(FormationFieldSavedData::load, FormationFieldSavedData::new, DATA_NAME);
    }

    public List<StoredField> fields() {
        return List.copyOf(fields);
    }

    public void replaceAll(List<StoredField> next) {
        fields.clear();
        if (next != null) {
            fields.addAll(next);
        }
        setDirty();
    }

    public void upsert(String dimensionId, BlockPos corePos, String kind, int remainingTicks) {
        fields.removeIf(f -> f.dimensionId().equals(dimensionId) && f.corePos().asLong() == corePos.asLong());
        fields.add(new StoredField(dimensionId, corePos.immutable(), kind.toUpperCase(Locale.ROOT), remainingTicks));
        setDirty();
    }

    public void remove(String dimensionId, BlockPos corePos) {
        fields.removeIf(f -> f.dimensionId().equals(dimensionId) && f.corePos().asLong() == corePos.asLong());
        setDirty();
    }

    public static FormationFieldSavedData load(CompoundTag tag) {
        FormationFieldSavedData data = new FormationFieldSavedData();
        ListTag list = tag.getList("Fields", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            data.fields.add(new StoredField(
                    entry.getString("Dim"),
                    BlockPos.of(entry.getLong("Pos")),
                    entry.getString("Kind"),
                    entry.getInt("Ticks")));
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (StoredField field : fields) {
            CompoundTag entry = new CompoundTag();
            entry.putString("Dim", field.dimensionId());
            entry.putLong("Pos", field.corePos().asLong());
            entry.putString("Kind", field.kind());
            entry.putInt("Ticks", field.remainingTicks());
            list.add(entry);
        }
        tag.put("Fields", list);
        return tag;
    }
}
