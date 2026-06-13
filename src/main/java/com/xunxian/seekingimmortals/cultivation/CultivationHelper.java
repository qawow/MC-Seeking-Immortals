package com.xunxian.seekingimmortals.cultivation;

import net.minecraft.world.entity.player.Player;
import java.util.Optional;

public final class CultivationHelper {
    private CultivationHelper() {}

    public static Optional<PlayerCultivation> get(Player player) {
        return player.getCapability(CultivationProvider.CULTIVATION).resolve();
    }
}
