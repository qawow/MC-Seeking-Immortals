package com.xunxian.seekingimmortals.worldpack;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;

/**
 * Named boss encounter spawner for secret-realm cores / high events (Wave52).
 */
public final class BossEncounterService {
    private static final String TAG = "seeking_immortals_boss_spawned";

    private BossEncounterService() {}

    public static boolean spawnIfNeeded(ServerPlayer player, String bossId) {
        if (player == null || bossId == null || bossId.isBlank()) {
            return false;
        }
        String key = TAG + "_" + bossId;
        if (player.getPersistentData().getBoolean(key)) {
            return false;
        }
        if (!(player.level() instanceof ServerLevel level)) {
            return false;
        }
        EntityType<? extends Mob> type = bossId.contains("puppet") ? EntityType.IRON_GOLEM
                : bossId.contains("king") || bossId.contains("dragon") ? EntityType.RAVAGER
                : EntityType.WITHER_SKELETON;
        Mob boss = type.create(level);
        if (boss == null) {
            return false;
        }
        boss.moveTo(player.getX() + 2, player.getY(), player.getZ() + 2, player.getYRot(), 0);
        if (boss.getAttribute(Attributes.MAX_HEALTH) != null) {
            boss.getAttribute(Attributes.MAX_HEALTH).setBaseValue(80.0D);
            boss.setHealth(80.0F);
        }
        if (boss.getAttribute(Attributes.ATTACK_DAMAGE) != null) {
            boss.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(10.0D);
        }
        boss.setCustomName(Component.translatable("entity.seeking_immortals.boss.name", bossId));
        boss.setCustomNameVisible(true);
        boss.setPersistenceRequired();
        if (boss instanceof Monster monster) {
            monster.setTarget(player);
        }
        level.addFreshEntity(boss);
        player.getPersistentData().putBoolean(key, true);
        player.displayClientMessage(Component.translatable("message.seeking_immortals.boss.spawned", bossId), true);
        return true;
    }
}
