package com.xunxian.seekingimmortals.catalog;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Soft browsers for refinement recipes and formation catalog indexes.
 */
public final class CraftWorldSoftService {
    private CraftWorldSoftService() {}

    public static int refinementRecipeCount() {
        return size("refinement_recipes_index");
    }

    public static int formationCount() {
        return size("formation_catalog_index");
    }

    public static int talismanRecipeCount() {
        return size("talisman_recipes_index");
    }

    public static int puppetRecipeCount() {
        return size("puppet_craft_recipes_index");
    }

    public static List<String> sample(String indexName, int limit) {
        Optional<BulkCatalogIndexService.IndexFile> optional = BulkCatalogIndexService.builtin().find(indexName);
        if (optional.isEmpty()) {
            return List.of();
        }
        List<String> list = new ArrayList<>();
        int i = 0;
        for (BulkCatalogIndexService.Entry entry : optional.get().entries().values()) {
            list.add(entry.id() + " | " + entry.display());
            if (++i >= Math.max(1, limit)) break;
        }
        return list;
    }

    public static boolean preview(ServerPlayer player, String indexName, String id, String unknownKey, String previewKey, String softKey) {
        Optional<BulkCatalogIndexService.IndexFile> optional = BulkCatalogIndexService.builtin().find(indexName);
        if (optional.isEmpty()) {
            player.displayClientMessage(Component.translatable(unknownKey, id), false);
            return false;
        }
        String key = id == null ? "" : id.trim().toLowerCase(Locale.ROOT);
        BulkCatalogIndexService.Entry entry = optional.get().entries().get(key);
        if (entry == null) {
            // fallback linear search case-insensitive
            for (BulkCatalogIndexService.Entry e : optional.get().entries().values()) {
                if (e.id().equalsIgnoreCase(id)) {
                    entry = e;
                    break;
                }
            }
        }
        if (entry == null) {
            player.displayClientMessage(Component.translatable(unknownKey, id), false);
            return false;
        }
        player.displayClientMessage(Component.translatable(previewKey, entry.id(), entry.display()), false);
        player.displayClientMessage(Component.translatable(softKey), false);
        return true;
    }

    private static int size(String indexName) {
        return BulkCatalogIndexService.builtin().find(indexName).map(BulkCatalogIndexService.IndexFile::size).orElse(0);
    }
}
