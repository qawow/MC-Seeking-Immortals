package com.xunxian.seekingimmortals.catalog;

import com.xunxian.seekingimmortals.network.SyncMethodLayoutPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Wave486: server-authoritative freeform method-tree layout offsets.
 * Stored in player persistent NBT and synced to client for MethodTreeScreen.
 */
public final class MethodLayoutService {
    public static final String LAYOUT_TAG = "seeking_immortals_method_layout";
    public static final int MAX_OFFSET = 240;

    private MethodLayoutService() {}

    public static Map<String, int[]> layoutOf(ServerPlayer player) {
        Map<String, int[]> map = new LinkedHashMap<>();
        if (player == null) {
            return map;
        }
        CompoundTag tag = player.getPersistentData().getCompound(LAYOUT_TAG);
        for (String key : tag.getAllKeys()) {
            if (map.size() >= SyncMethodLayoutPacket.MAX_ENTRIES || !isValidMethodId(key)) {
                continue;
            }
            int[] xy = parse(tag.getString(key));
            if (xy != null) {
                map.put(key.trim().toLowerCase(Locale.ROOT), xy);
            }
        }
        return map;
    }

    public static void setOffset(ServerPlayer player, String methodId, int x, int y) {
        if (player == null || !isValidMethodId(methodId)) {
            return;
        }
        String key = methodId.trim().toLowerCase(Locale.ROOT);
        int cx = clamp(x);
        int cy = clamp(y);
        CompoundTag tag = player.getPersistentData().getCompound(LAYOUT_TAG).copy();
        if (!tag.contains(key) && validEntryCount(tag) >= SyncMethodLayoutPacket.MAX_ENTRIES) {
            return;
        }
        if (cx == 0 && cy == 0) {
            tag.remove(key);
        } else {
            tag.putString(key, cx + "," + cy);
        }
        player.getPersistentData().put(LAYOUT_TAG, tag);
    }

    public static void clear(ServerPlayer player) {
        if (player == null) {
            return;
        }
        player.getPersistentData().remove(LAYOUT_TAG);
    }

    public static void sync(ServerPlayer player) {
        SyncMethodLayoutPacket.send(player);
    }

    public static boolean isValidMethodId(String methodId) {
        if (methodId == null) {
            return false;
        }
        String key = methodId.trim().toLowerCase(Locale.ROOT);
        return !key.isBlank()
                && key.length() <= SyncMethodLayoutPacket.MAX_ID_LENGTH
                && TextMaterialCatalogService.builtin().findMethod(key).isPresent();
    }

    /** Clone only bounded catalog-backed entries so malformed legacy data cannot poison respawn sync. */
    public static void copyLayoutData(CompoundTag originalData, CompoundTag clonedData) {
        if (originalData == null || clonedData == null || !originalData.contains(LAYOUT_TAG)) {
            return;
        }
        CompoundTag source = originalData.getCompound(LAYOUT_TAG);
        CompoundTag sanitized = new CompoundTag();
        int copied = 0;
        for (String key : source.getAllKeys()) {
            if (copied >= SyncMethodLayoutPacket.MAX_ENTRIES || !isValidMethodId(key)) {
                continue;
            }
            int[] xy = parse(source.getString(key));
            if (xy != null && (xy[0] != 0 || xy[1] != 0)) {
                sanitized.putString(key.trim().toLowerCase(Locale.ROOT), xy[0] + "," + xy[1]);
                copied++;
            }
        }
        if (!sanitized.isEmpty()) {
            clonedData.put(LAYOUT_TAG, sanitized);
        }
    }

    private static int validEntryCount(CompoundTag tag) {
        int count = 0;
        for (String key : tag.getAllKeys()) {
            if (isValidMethodId(key) && ++count >= SyncMethodLayoutPacket.MAX_ENTRIES) {
                return count;
            }
        }
        return count;
    }

    private static int clamp(int v) {
        return Math.max(-MAX_OFFSET, Math.min(MAX_OFFSET, v));
    }

    private static int[] parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String[] parts = raw.split(",");
        if (parts.length != 2) {
            return null;
        }
        try {
            return new int[]{clamp(Integer.parseInt(parts[0].trim())), clamp(Integer.parseInt(parts[1].trim()))};
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
