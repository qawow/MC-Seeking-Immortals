package com.xunxian.seekingimmortals.npc;

import com.xunxian.seekingimmortals.SeekingImmortalsMod;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Persistent world-wide ownership of authored named-NPC placements. */
public final class NamedNpcPlacementSavedData extends SavedData {
    private static final String DATA_NAME = SeekingImmortalsMod.MODID + "_named_npc_placements";
    private final Map<String, Placement> placements = new LinkedHashMap<>();

    public record Placement(String npcId, UUID entityId, String dimensionId, BlockPos pos) {}

    public static NamedNpcPlacementSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                NamedNpcPlacementSavedData::load,
                NamedNpcPlacementSavedData::new,
                DATA_NAME);
    }

    public static NamedNpcPlacementSavedData load(CompoundTag tag) {
        NamedNpcPlacementSavedData data = new NamedNpcPlacementSavedData();
        ListTag entries = tag.getList("Placements", Tag.TAG_COMPOUND);
        for (int i = 0; i < entries.size(); i++) {
            CompoundTag entry = entries.getCompound(i);
            String npcId = normalize(entry.getString("NpcId"));
            if (npcId.isBlank() || !entry.hasUUID("Entity")) {
                continue;
            }
            data.placements.put(npcId, new Placement(
                    npcId,
                    entry.getUUID("Entity"),
                    entry.getString("Dimension"),
                    BlockPos.of(entry.getLong("Pos"))));
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag entries = new ListTag();
        for (Placement placement : placements.values()) {
            CompoundTag entry = new CompoundTag();
            entry.putString("NpcId", placement.npcId());
            entry.putUUID("Entity", placement.entityId());
            entry.putString("Dimension", placement.dimensionId());
            entry.putLong("Pos", placement.pos().asLong());
            entries.add(entry);
        }
        tag.put("Placements", entries);
        return tag;
    }

    public Optional<Placement> find(String npcId) {
        return Optional.ofNullable(placements.get(normalize(npcId)));
    }

    public boolean contains(String npcId) {
        return find(npcId).isPresent();
    }

    public void record(String npcId, UUID entityId, String dimensionId, BlockPos pos) {
        String id = normalize(npcId);
        if (id.isBlank() || entityId == null || pos == null) {
            return;
        }
        Placement next = new Placement(id, entityId, dimensionId == null ? "" : dimensionId, pos.immutable());
        if (!next.equals(placements.put(id, next))) {
            setDirty();
        }
    }

    public void remove(String npcId) {
        if (placements.remove(normalize(npcId)) != null) {
            setDirty();
        }
    }

    public int size() {
        return placements.size();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(java.util.Locale.ROOT);
    }
}
