package com.xunxian.seekingimmortals.client;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Wave486: client mirror of server freeform method-tree layout offsets.
 */
public final class ClientMethodLayoutData {
    private static Map<String, int[]> offsets = Map.of();
    private static boolean synced;

    private ClientMethodLayoutData() {}

    public static void set(Map<String, int[]> map) {
        if (map == null || map.isEmpty()) {
            offsets = Map.of();
        } else {
            Map<String, int[]> next = new LinkedHashMap<>();
            map.forEach((id, xy) -> {
                if (id == null || id.isBlank() || xy == null || xy.length < 2) {
                    return;
                }
                next.put(id.trim().toLowerCase(Locale.ROOT), new int[]{xy[0], xy[1]});
            });
            offsets = Collections.unmodifiableMap(next);
        }
        synced = true;
    }

    public static void reset() {
        offsets = Map.of();
        synced = false;
    }

    public static boolean isSynced() {
        return synced;
    }

    public static Map<String, int[]> getOffsets() {
        return offsets;
    }

    public static int[] get(String methodId) {
        if (methodId == null || methodId.isBlank()) {
            return null;
        }
        return offsets.get(methodId.trim().toLowerCase(Locale.ROOT));
    }
}
