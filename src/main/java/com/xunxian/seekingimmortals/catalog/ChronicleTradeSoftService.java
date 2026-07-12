package com.xunxian.seekingimmortals.catalog;

import com.xunxian.seekingimmortals.catalog.FactionQuestCatalogService.Entry;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Soft browsers for chronicle events and trade routes.
 */
public final class ChronicleTradeSoftService {
    private ChronicleTradeSoftService() {}

    public static int chronicleCount() {
        return FactionQuestCatalogService.builtin().chronicleEvents().size();
    }

    public static int tradeRouteCount() {
        return FactionQuestCatalogService.builtin().tradeRoutes().size();
    }

    public static List<String> sampleChronicle(int limit) {
        return sample(FactionQuestCatalogService.builtin().chronicleEvents(), limit);
    }

    public static List<String> sampleTradeRoutes(int limit) {
        return sample(FactionQuestCatalogService.builtin().tradeRoutes(), limit);
    }

    public static boolean previewChronicle(ServerPlayer player, String id) {
        return preview(player, FactionQuestCatalogService.builtin().chronicleEvents().get(norm(id)),
                "message.seeking_immortals.chronicle.unknown",
                "message.seeking_immortals.chronicle.preview",
                "message.seeking_immortals.chronicle.soft_only",
                id);
    }

    public static boolean previewTradeRoute(ServerPlayer player, String id) {
        return preview(player, FactionQuestCatalogService.builtin().tradeRoutes().get(norm(id)),
                "message.seeking_immortals.trade_route.unknown",
                "message.seeking_immortals.trade_route.preview",
                "message.seeking_immortals.trade_route.soft_only",
                id);
    }

    private static List<String> sample(java.util.Map<String, Entry> map, int limit) {
        List<String> list = new ArrayList<>();
        int i = 0;
        for (Entry entry : map.values()) {
            list.add(entry.id() + " | " + entry.display());
            if (++i >= Math.max(1, limit)) break;
        }
        return list;
    }

    private static boolean preview(ServerPlayer player, Entry entry, String unknownKey, String previewKey, String softKey, String rawId) {
        if (entry == null) {
            player.displayClientMessage(Component.translatable(unknownKey, rawId), false);
            return false;
        }
        player.displayClientMessage(Component.translatable(previewKey, entry.id(), entry.display()), false);
        player.displayClientMessage(Component.translatable(softKey), false);
        return true;
    }

    private static String norm(String id) {
        return id == null ? "" : id.trim().toLowerCase(Locale.ROOT);
    }
}
