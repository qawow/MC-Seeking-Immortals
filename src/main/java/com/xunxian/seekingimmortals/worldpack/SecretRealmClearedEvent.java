package com.xunxian.seekingimmortals.worldpack;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.Event;

/**
 * M09 → M11/M06 hook: fired when a secret realm is cleared by a player.
 * Consumers should treat this as server-authoritative progress signal.
 */
public final class SecretRealmClearedEvent extends Event {
    private final ServerPlayer player;
    private final String realmId;
    private final boolean firstClear;
    private final int clearCount;

    public SecretRealmClearedEvent(ServerPlayer player, String realmId, boolean firstClear, int clearCount) {
        this.player = player;
        this.realmId = realmId == null ? "" : realmId;
        this.firstClear = firstClear;
        this.clearCount = Math.max(0, clearCount);
    }

    public ServerPlayer getPlayer() {
        return player;
    }

    public String getRealmId() {
        return realmId;
    }

    public boolean isFirstClear() {
        return firstClear;
    }

    public int getClearCount() {
        return clearCount;
    }
}
