package com.xunxian.seekingimmortals.worldpack;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Monster;

import java.util.Locale;

/**
 * Daily event encounter spawner (Wave51 content fidelity).
 * Spawns lightweight hostiles when certain daily event tokens are active.
 */
public final class DailyEventEncounterService {
    private static final String TAG = "seeking_immortals_daily_spawned";

    private DailyEventEncounterService() {}

    public static void maybeSpawn(ServerPlayer player, String eventId) {
        if (player == null || eventId == null || eventId.isBlank()) {
            return;
        }
        String id = eventId.toLowerCase(Locale.ROOT);
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        String key = TAG + "_" + id;
        if (player.getPersistentData().getBoolean(key)) {
            return;
        }
        EntityType<? extends Mob> type = null;
        int count = 1;
        if (id.contains("bandit") || id.contains("rogue") || id.contains("raid")) {
            type = EntityType.PILLAGER;
            count = 2;
        } else if (id.contains("beast") || id.contains("migration") || id.contains("tide")) {
            type = EntityType.WOLF;
            count = 3;
        } else if (id.contains("demon") || id.contains("qi") || id.contains("corruption")) {
            type = EntityType.VEX;
            count = 2;
        } else if (id.contains("merchant") || id.contains("caravan")) {
            // merchant events do not spawn hostiles
            player.getPersistentData().putBoolean(key, true);
            player.displayClientMessage(Component.translatable("message.seeking_immortals.daily_event.merchant", id), true);
            return;
        } else {
            return;
        }
        for (int i = 0; i < count; i++) {
            Mob mob = type.create(level);
            if (mob == null) continue;
            mob.moveTo(player.getX() + (i - 1), player.getY(), player.getZ() + 1.5D + i * 0.4D,
                    player.getYRot(), 0.0F);
            if (mob instanceof Monster monster) {
                monster.setTarget(player);
            }
            mob.setPersistenceRequired();
            level.addFreshEntity(mob);
        }
        player.getPersistentData().putBoolean(key, true);
        player.displayClientMessage(Component.translatable("message.seeking_immortals.daily_event.spawn", id, count), true);
    }
}
