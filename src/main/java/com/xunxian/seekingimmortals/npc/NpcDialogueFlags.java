package com.xunxian.seekingimmortals.npc;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

import java.util.Locale;

/**
 * M12 soft quest/dialogue flags stored on player persistent NBT.
 * Shared with condition ops ({@code quest_flag}/{@code set_flag}) and reward gates.
 */
public final class NpcDialogueFlags {
    private static final String ROOT = "seeking_immortals_npc_dialogue_flags";

    private NpcDialogueFlags() {}

    public static boolean hasFlag(ServerPlayer player, String flag) {
        if (player == null) {
            return false;
        }
        String key = normalize(flag);
        if (key.isBlank()) {
            return false;
        }
        return player.getPersistentData().getCompound(ROOT).getBoolean(key);
    }

    public static void setFlag(ServerPlayer player, String flag, boolean value) {
        if (player == null) {
            return;
        }
        String key = normalize(flag);
        if (key.isBlank()) {
            return;
        }
        CompoundTag root = player.getPersistentData().getCompound(ROOT).copy();
        if (value) {
            root.putBoolean(key, true);
        } else {
            root.remove(key);
        }
        player.getPersistentData().put(ROOT, root);
    }

    public static void setFlag(ServerPlayer player, String flag) {
        setFlag(player, flag, true);
    }

    private static String normalize(String flag) {
        return flag == null ? "" : flag.trim().toLowerCase(Locale.ROOT);
    }
}
