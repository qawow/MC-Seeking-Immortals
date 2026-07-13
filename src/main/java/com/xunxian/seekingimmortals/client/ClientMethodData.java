package com.xunxian.seekingimmortals.client;

import com.xunxian.seekingimmortals.catalog.ManualCatalogService;
import com.xunxian.seekingimmortals.catalog.TextMaterialCatalogService;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Wave477: client mirror of server learned cultivation methods.
 * Wave481: stores method layers (1-9).
 */
public final class ClientMethodData {
    private static Map<String, Integer> learnedMethods = Map.of();
    private static boolean synced;

    private ClientMethodData() {}

    public static void setLearnedMethods(Map<String, Integer> methods) {
        if (methods == null || methods.isEmpty()) {
            learnedMethods = Map.of();
        } else {
            Map<String, Integer> next = new LinkedHashMap<>();
            methods.forEach((id, layer) -> {
                if (id == null || id.isBlank()) {
                    return;
                }
                String key = id.trim().toLowerCase(Locale.ROOT);
                int lv = layer == null ? 1 : Math.max(1, Math.min(ManualCatalogService.MAX_METHOD_LAYER, layer));
                next.put(key, lv);
            });
            learnedMethods = Map.copyOf(next);
        }
        synced = true;
    }

    /** Back-compat: list of ids only (layer ignored). */
    public static void setLearnedMethods(List<String> methods) {
        if (methods == null || methods.isEmpty()) {
            learnedMethods = Map.of();
            synced = true;
            return;
        }
        Map<String, Integer> map = new LinkedHashMap<>();
        for (String id : methods) {
            if (id == null || id.isBlank()) {
                continue;
            }
            map.put(id.trim().toLowerCase(Locale.ROOT), 1);
        }
        learnedMethods = Map.copyOf(map);
        synced = true;
    }

    public static void reset() {
        learnedMethods = Map.of();
        synced = false;
    }

    public static boolean isSynced() {
        return synced;
    }

    public static List<String> getLearnedMethods() {
        return List.copyOf(learnedMethods.keySet());
    }

    public static Map<String, Integer> getLearnedMethodLayers() {
        return learnedMethods;
    }

    public static int getLearnedMethodCount() {
        return learnedMethods.size();
    }

    public static boolean hasLearned(String methodId) {
        if (methodId == null || methodId.isBlank()) {
            return false;
        }
        return learnedMethods.containsKey(methodId.trim().toLowerCase(Locale.ROOT));
    }

    public static int getLayer(String methodId) {
        if (methodId == null || methodId.isBlank()) {
            return 0;
        }
        return learnedMethods.getOrDefault(methodId.trim().toLowerCase(Locale.ROOT), 0);
    }

    public static List<String> displayLines(int limit) {
        int max = Math.max(0, limit);
        List<String> lines = new ArrayList<>();
        int shown = 0;
        for (Map.Entry<String, Integer> entry : learnedMethods.entrySet()) {
            if (shown >= max) {
                break;
            }
            String id = entry.getKey();
            String display = TextMaterialCatalogService.builtin().findMethod(id)
                    .map(TextMaterialCatalogService.MethodEntry::display)
                    .filter(s -> s != null && !s.isBlank())
                    .orElse(id);
            lines.add(display + " · Lv." + entry.getValue());
            shown++;
        }
        return lines;
    }
}
