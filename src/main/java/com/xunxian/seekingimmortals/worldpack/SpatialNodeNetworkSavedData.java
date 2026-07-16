package com.xunxian.seekingimmortals.worldpack;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * M13 spatial teleport network nodes persisted on the overworld SavedData.
 * Structure formed checks (M07) feed into register/activate; travel execution lives here/M13.
 */
public final class SpatialNodeNetworkSavedData extends SavedData {
    private static final String DATA_NAME = "seeking_immortals_spatial_node_network";

    private final Map<String, NetworkNode> nodes = new LinkedHashMap<>();

    public record NetworkNode(
            String id,
            String catalogNodeId,
            String dimensionId,
            int x,
            int y,
            int z,
            String type,
            boolean formed,
            long lastUsedGameTime) {}

    public SpatialNodeNetworkSavedData() {}

    public static SpatialNodeNetworkSavedData get(ServerLevel level) {
        ServerLevel overworld = level.getServer().overworld();
        return overworld.getDataStorage().computeIfAbsent(
                SpatialNodeNetworkSavedData::load, SpatialNodeNetworkSavedData::new, DATA_NAME);
    }

    public static SpatialNodeNetworkSavedData load(CompoundTag tag) {
        SpatialNodeNetworkSavedData data = new SpatialNodeNetworkSavedData();
        ListTag list = tag.getList("Nodes", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag n = list.getCompound(i);
            String id = n.getString("Id");
            if (id.isBlank()) {
                continue;
            }
            data.nodes.put(id.toLowerCase(Locale.ROOT), new NetworkNode(
                    id,
                    n.getString("Catalog"),
                    n.getString("Dimension"),
                    n.getInt("X"),
                    n.getInt("Y"),
                    n.getInt("Z"),
                    n.getString("Type"),
                    n.getBoolean("Formed"),
                    n.getLong("LastUsed")));
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (NetworkNode node : nodes.values()) {
            CompoundTag n = new CompoundTag();
            n.putString("Id", node.id());
            n.putString("Catalog", node.catalogNodeId());
            n.putString("Dimension", node.dimensionId());
            n.putInt("X", node.x());
            n.putInt("Y", node.y());
            n.putInt("Z", node.z());
            n.putString("Type", node.type());
            n.putBoolean("Formed", node.formed());
            n.putLong("LastUsed", node.lastUsedGameTime());
            list.add(n);
        }
        tag.put("Nodes", list);
        return tag;
    }

    public Optional<NetworkNode> find(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(nodes.get(id.trim().toLowerCase(Locale.ROOT)));
    }

    public Collection<NetworkNode> all() {
        return Collections.unmodifiableCollection(nodes.values());
    }

    public int size() {
        return nodes.size();
    }

    public void upsert(NetworkNode node) {
        if (node == null || node.id() == null || node.id().isBlank()) {
            return;
        }
        nodes.put(node.id().toLowerCase(Locale.ROOT), node);
        setDirty();
    }

    public void markUsed(String id, long gameTime) {
        find(id).ifPresent(node -> {
            nodes.put(node.id().toLowerCase(Locale.ROOT), new NetworkNode(
                    node.id(), node.catalogNodeId(), node.dimensionId(),
                    node.x(), node.y(), node.z(), node.type(), node.formed(), gameTime));
            setDirty();
        });
    }

    public void setFormed(String id, boolean formed) {
        find(id).ifPresent(node -> {
            nodes.put(node.id().toLowerCase(Locale.ROOT), new NetworkNode(
                    node.id(), node.catalogNodeId(), node.dimensionId(),
                    node.x(), node.y(), node.z(), node.type(), formed, node.lastUsedGameTime()));
            setDirty();
        });
    }
}
