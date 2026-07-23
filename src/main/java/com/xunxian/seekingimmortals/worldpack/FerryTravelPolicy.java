package com.xunxian.seekingimmortals.worldpack;

import com.xunxian.seekingimmortals.sect.FactionConflictEventService;
import com.xunxian.seekingimmortals.cultivation.CultivationHelper;
import com.xunxian.seekingimmortals.region.DailyEventScheduler;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Locale;

/** Shared server-side gates for authored ferry routes and ferry vehicles. */
public final class FerryTravelPolicy {
    public static final String MESSAGE_DELAYED = "message.seeking_immortals.worldpack.ferry_delayed";

    private FerryTravelPolicy() {}

    public static boolean isDelayed(ServerPlayer player) {
        if (player == null) {
            return false;
        }
        refresh(player);
        boolean daily = DailyEventEffectExecutor.activeEvent(player)
                .map(DailyEventEffectExecutor::isFerryDelayed)
                .orElse(false);
        return daily || FactionConflictEventService.activeFerryDelayed(player);
    }

    /** Sends the denial only when a ferry is currently closed. */
    public static boolean denyIfDelayed(ServerPlayer player) {
        if (!isDelayed(player)) {
            return false;
        }
        player.sendSystemMessage(Component.translatable(MESSAGE_DELAYED));
        return true;
    }

    public static int adjustCost(ServerPlayer player, int baseCost) {
        if (baseCost <= 0) {
            return 0;
        }
        refresh(player);
        return DailyEventEffectExecutor.activeEvent(player)
                .map(event -> DailyEventEffectExecutor.adjustFerryCost(baseCost, event))
                .orElse(baseCost);
    }

    public static boolean isRegionFerryRoute(String currentRegionId, String targetRegionId) {
        String current = normalize(currentRegionId);
        String target = normalize(targetRegionId);
        if (current.isBlank() || target.isBlank() || current.equals(target)) {
            return false;
        }
        return isPair(current, target, "nether_river", "yinming")
                || isPair(current, target, "tiannan", "chaotic_sea")
                || isPair(current, target, "chaotic_sea", "outer_sea_market");
    }

    public static boolean isFerryVehicle(String vehicleId) {
        String id = normalize(vehicleId);
        return id.contains("ferry") || "spirit_boat_chaotic_sea".equals(id);
    }

    public static boolean isFerryRoute(String routeId, String method, String gateId) {
        String combined = normalize(routeId) + " " + normalize(method) + " " + normalize(gateId);
        return combined.contains("ferry")
                || combined.contains("sea_ship")
                || combined.contains("ghost_boat")
                || combined.contains("spirit_boat")
                || combined.contains("pocket_gate");
    }

    private static boolean isPair(String current, String target, String first, String second) {
        return (first.equals(current) && second.equals(target))
                || (second.equals(current) && first.equals(target));
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static void refresh(ServerPlayer player) {
        CultivationHelper.get(player).ifPresent(cultivation -> {
            String region = cultivation.getWorldpackCurrentRegionId();
            DailyEventScheduler.ensurePlayerEvent(player, region);
        });
    }
}
