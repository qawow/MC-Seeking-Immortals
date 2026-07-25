package com.xunxian.seekingimmortals.worldpack;

import com.xunxian.seekingimmortals.SeekingImmortalsMod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.LinkedHashSet;
import java.util.Set;

/** One-time generation ledger for authored secret-realm arrival scenes. */
public final class SecretRealmSceneSavedData extends SavedData {
    private static final String DATA_NAME = SeekingImmortalsMod.MODID + "_secret_realm_scenes";
    private final Set<String> generated = new LinkedHashSet<>();

    public static SecretRealmSceneSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                SecretRealmSceneSavedData::load,
                SecretRealmSceneSavedData::new,
                DATA_NAME);
    }

    public static SecretRealmSceneSavedData load(CompoundTag tag) {
        SecretRealmSceneSavedData data = new SecretRealmSceneSavedData();
        ListTag entries = tag.getList("Generated", Tag.TAG_STRING);
        for (int i = 0; i < entries.size(); i++) {
            String key = entries.getString(i);
            if (!key.isBlank()) {
                data.generated.add(key);
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag entries = new ListTag();
        generated.stream().sorted().map(StringTag::valueOf).forEach(entries::add);
        tag.put("Generated", entries);
        return tag;
    }

    public boolean isGenerated(String key) {
        return key != null && generated.contains(key);
    }

    public void markGenerated(String key) {
        if (key != null && !key.isBlank() && generated.add(key)) {
            setDirty();
        }
    }
}
