package com.xunxian.seekingimmortals.beast;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * M10: kill/contract bestiary unlock records (display owned by M16).
 */
public final class BestiaryUnlockService {
    private static final String ROOT = "seeking_immortals_bestiary";
    private static final String KILLS = "Kills";
    private static final String CONTRACTS = "Contracts";
    private static final String SEEN = "Seen";

    public enum UnlockKind {
        KILL,
        CONTRACT,
        SEEN
    }

    private BestiaryUnlockService() {}

    public static boolean unlock(ServerPlayer player, String beastId, UnlockKind kind) {
        if (player == null || beastId == null || beastId.isBlank() || kind == null) {
            return false;
        }
        String id = beastId.trim().toLowerCase(Locale.ROOT);
        // Prefer canonical bestiary id when display / alias given.
        id = BeastBestiaryService.find(id).map(BeastBestiaryService.BeastEntry::id).orElse(id);
        CompoundTag root = player.getPersistentData().getCompound(ROOT).copy();
        String listKey = switch (kind) {
            case KILL -> KILLS;
            case CONTRACT -> CONTRACTS;
            case SEEN -> SEEN;
        };
        Set<String> set = readSet(root, listKey);
        boolean added = set.add(id);
        // SEEN also accumulates from kill/contract.
        if (kind != UnlockKind.SEEN) {
            Set<String> seen = readSet(root, SEEN);
            seen.add(id);
            writeSet(root, SEEN, seen);
        }
        writeSet(root, listKey, set);
        player.getPersistentData().put(ROOT, root);
        return added;
    }

    public static boolean isUnlocked(ServerPlayer player, String beastId) {
        if (player == null || beastId == null) {
            return false;
        }
        String id = beastId.trim().toLowerCase(Locale.ROOT);
        id = BeastBestiaryService.find(id).map(BeastBestiaryService.BeastEntry::id).orElse(id);
        CompoundTag root = player.getPersistentData().getCompound(ROOT);
        return readSet(root, SEEN).contains(id)
                || readSet(root, KILLS).contains(id)
                || readSet(root, CONTRACTS).contains(id);
    }

    public static List<String> unlockedIds(ServerPlayer player) {
        if (player == null) {
            return List.of();
        }
        CompoundTag root = player.getPersistentData().getCompound(ROOT);
        Set<String> all = new LinkedHashSet<>();
        all.addAll(readSet(root, SEEN));
        all.addAll(readSet(root, KILLS));
        all.addAll(readSet(root, CONTRACTS));
        return List.copyOf(all);
    }

    public static int unlockedCount(ServerPlayer player) {
        return unlockedIds(player).size();
    }

    public static int killCount(ServerPlayer player) {
        if (player == null) {
            return 0;
        }
        return readSet(player.getPersistentData().getCompound(ROOT), KILLS).size();
    }

    public static int contractCount(ServerPlayer player) {
        if (player == null) {
            return 0;
        }
        return readSet(player.getPersistentData().getCompound(ROOT), CONTRACTS).size();
    }

    private static Set<String> readSet(CompoundTag root, String key) {
        Set<String> set = new LinkedHashSet<>();
        if (root == null || !root.contains(key, Tag.TAG_LIST)) {
            return set;
        }
        ListTag list = root.getList(key, Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) {
            String v = list.getString(i);
            if (v != null && !v.isBlank()) {
                set.add(v.toLowerCase(Locale.ROOT));
            }
        }
        return set;
    }

    private static void writeSet(CompoundTag root, String key, Set<String> set) {
        ListTag list = new ListTag();
        for (String id : set) {
            list.add(StringTag.valueOf(id));
        }
        root.put(key, list);
    }
}
