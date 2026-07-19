package com.xunxian.seekingimmortals.menu;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class MenuAccessContext {
    private static final double MAX_DISTANCE_SQR = 64.0D;

    private final ResourceKey<Level> dimension;
    private final Vec3 anchor;
    private final UUID sourceEntityId;
    private final long token;
    private final boolean clientView;

    private MenuAccessContext(ResourceKey<Level> dimension, Vec3 anchor, UUID sourceEntityId,
                              long token, boolean clientView) {
        this.dimension = dimension;
        this.anchor = anchor;
        this.sourceEntityId = sourceEntityId;
        this.token = token;
        this.clientView = clientView;
    }

    public static MenuAccessContext atPlayer(ServerPlayer player) {
        return new MenuAccessContext(player.level().dimension(), player.position(), null, newToken(), false);
    }

    public static MenuAccessContext atEntity(ServerPlayer player, Entity source) {
        if (player == null || source == null || source.level() != player.level()) {
            return denied();
        }
        return new MenuAccessContext(player.level().dimension(), source.position(), source.getUUID(),
                newToken(), false);
    }

    public static MenuAccessContext client(long token) {
        return new MenuAccessContext(null, Vec3.ZERO, null, token, true);
    }

    public long token() {
        return token;
    }

    public boolean authorizes(Player player, long presentedToken) {
        return token != 0L && token == presentedToken && isValid(player);
    }

    public boolean isValid(Player player) {
        if (player == null || !player.isAlive()) {
            return false;
        }
        if (clientView) {
            return player.level().isClientSide;
        }
        if (!(player instanceof ServerPlayer serverPlayer)
                || dimension == null
                || !dimension.equals(serverPlayer.level().dimension())) {
            return false;
        }
        if (sourceEntityId == null) {
            return serverPlayer.distanceToSqr(anchor) <= MAX_DISTANCE_SQR;
        }
        Entity source = serverPlayer.serverLevel().getEntity(sourceEntityId);
        return source != null
                && source.isAlive()
                && source.level() == serverPlayer.level()
                && source.distanceToSqr(anchor) <= MAX_DISTANCE_SQR
                && serverPlayer.distanceToSqr(anchor) <= MAX_DISTANCE_SQR
                && serverPlayer.distanceToSqr(source) <= MAX_DISTANCE_SQR;
    }

    private static MenuAccessContext denied() {
        return new MenuAccessContext(null, Vec3.ZERO, null, 0L, false);
    }

    private static long newToken() {
        long value;
        do {
            value = ThreadLocalRandom.current().nextLong();
        } while (value == 0L);
        return value;
    }
}
