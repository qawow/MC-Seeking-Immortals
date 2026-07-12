package com.xunxian.seekingimmortals.catalog;

import com.xunxian.seekingimmortals.catalog.FactionQuestCatalogService.Entry;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Soft browser for faction conflict events.
 */
public final class FactionConflictSoftService {
    private FactionConflictSoftService() {}

    public static int count() {
        return FactionQuestCatalogService.builtin().factionConflicts().size();
    }

    public static List<String> sample(int limit) {
        List<String> list = new ArrayList<>();
        int i = 0;
        for (Entry entry : FactionQuestCatalogService.builtin().factionConflicts().values()) {
            list.add(entry.id() + " | " + entry.display());
            if (++i >= Math.max(1, limit)) break;
        }
        return list;
    }

    public static boolean preview(ServerPlayer player, String id) {
        String key = id == null ? "" : id.trim().toLowerCase(Locale.ROOT);
        Entry entry = FactionQuestCatalogService.builtin().factionConflicts().get(key);
        if (entry == null) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.faction_conflict.unknown", id), false);
            return false;
        }
        player.displayClientMessage(Component.translatable("message.seeking_immortals.faction_conflict.preview",
                entry.id(), entry.display()), false);
        player.displayClientMessage(Component.translatable("message.seeking_immortals.faction_conflict.soft_only"), false);
        return true;
    }
}
