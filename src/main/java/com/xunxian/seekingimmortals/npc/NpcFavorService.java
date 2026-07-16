package com.xunxian.seekingimmortals.npc;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * M12 NPC favor / relationship values persisted on the player.
 * Keyed by npcId (or faction track when only track is known).
 */
public final class NpcFavorService {
    private static final String ROOT = "seeking_immortals_npc_favor";
    public static final int MIN = -100;
    public static final int MAX = 100;

    private NpcFavorService() {}

    public static int get(ServerPlayer player, String npcId) {
        if (player == null) {
            return 0;
        }
        String key = normalize(npcId);
        if (key.isBlank()) {
            return 0;
        }
        return player.getPersistentData().getCompound(ROOT).getInt(key);
    }

    public static int set(ServerPlayer player, String npcId, int value) {
        if (player == null) {
            return 0;
        }
        String key = normalize(npcId);
        if (key.isBlank()) {
            return 0;
        }
        int clamped = Math.max(MIN, Math.min(MAX, value));
        CompoundTag root = player.getPersistentData().getCompound(ROOT).copy();
        root.putInt(key, clamped);
        player.getPersistentData().put(ROOT, root);
        return clamped;
    }

    public static int add(ServerPlayer player, String npcId, int delta) {
        return set(player, npcId, get(player, npcId) + delta);
    }

    public static Map<String, Integer> snapshot(ServerPlayer player) {
        Map<String, Integer> map = new LinkedHashMap<>();
        if (player == null) {
            return map;
        }
        CompoundTag root = player.getPersistentData().getCompound(ROOT);
        for (String key : root.getAllKeys()) {
            map.put(key, root.getInt(key));
        }
        return map;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
