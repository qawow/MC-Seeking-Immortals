package com.xunxian.seekingimmortals.cultivation;

import net.minecraft.world.entity.player.Player;
import java.util.Optional;

public final class CultivationHelper {
    private CultivationHelper() {}

    public static Optional<PlayerCultivation> get(Player player) {
        return player.getCapability(CultivationProvider.CULTIVATION).resolve();
    }

    /** 便捷转发：境界门槛。 */
    public static boolean meetsRealm(Player player, String minRealmId) {
        return ProgressionGateApi.meetsRealm(player, minRealmId);
    }

    public static boolean meetsRoot(Player player, String rootRequirement) {
        return ProgressionGateApi.meetsRoot(player, rootRequirement);
    }

    public static boolean meetsPath(Player player, String pathId) {
        return ProgressionGateApi.meetsPath(player, pathId);
    }

    public static boolean meetsRace(Player player, String raceId) {
        return ProgressionGateApi.meetsRace(player, raceId);
    }
}
