package com.xunxian.seekingimmortals.worldpack;

import com.xunxian.seekingimmortals.util.PlayerDisplayText;
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
        boolean beastEcology = false;
        if (id.contains("bandit") || id.contains("rogue") || id.contains("raid")) {
            type = EntityType.PILLAGER;
            count = 2;
        } else if (id.contains("beast") || id.contains("migration") || id.contains("tide")) {
            // Wave491: prefer spawn_tables runtime consumer (servitor proxies + cluster weight).
            String region = "tiannan";
            if (id.contains("mulan")) {
                region = "mulan";
            } else if (id.contains("sea") || id.contains("chaotic")) {
                region = "chaotic_sea";
            } else if (id.contains("dajin") || id.contains("kunwu")) {
                region = "dajin";
            }
            int spawned = BeastSpawnTableService.spawnNearPlayer(player, region, 3);
            player.getPersistentData().putBoolean(key, true);
            if (spawned <= 0) {
                // Fallback to legacy wolf densify if tables empty.
                type = EntityType.WOLF;
                count = 3;
                beastEcology = true;
            } else {
                return;
            }
        } else if (id.contains("demon") || id.contains("qi") || id.contains("corruption")) {
            type = EntityType.VEX;
            count = 2;
        } else if (id.contains("merchant") || id.contains("caravan")) {
            // merchant events do not spawn hostiles
            player.getPersistentData().putBoolean(key, true);
            player.displayClientMessage(Component.translatable("message.seeking_immortals.daily_event.merchant",
                    displayName(id)), true);
            return;
        } else {
            return;
        }

        // Wave490: dense leyline clusters densify beast ecology packs.
        boolean cluster = beastEcology && com.xunxian.seekingimmortals.spiritual.SpiritualAuraManager
                .isLeylineCluster(level, new net.minecraft.world.level.ChunkPos(player.blockPosition()));
        if (cluster) {
            count += 2;
            // Prefer fox packs near clusters as a second ecology flavor.
            if (player.getRandom().nextBoolean()) {
                type = EntityType.FOX;
            }
        }

        for (int i = 0; i < count; i++) {
            Mob mob = type.create(level);
            if (mob == null) continue;
            mob.moveTo(player.getX() + (i - 1), player.getY(), player.getZ() + 1.5D + i * 0.4D,
                    player.getYRot(), 0.0F);
            if (mob instanceof Monster monster) {
                monster.setTarget(player);
            }
            // Tag ecology spawns for later bestiary/contract hooks.
            if (beastEcology) {
                mob.getPersistentData().putBoolean("seeking_immortals_ecology_beast", true);
                if (cluster) {
                    mob.getPersistentData().putBoolean("seeking_immortals_leyline_cluster", true);
                }
            }
            mob.setPersistenceRequired();
            level.addFreshEntity(mob);
        }
        player.getPersistentData().putBoolean(key, true);
        if (cluster) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.daily_event.spawn_cluster", displayName(id), count), true);
        } else {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.daily_event.spawn", displayName(id), count), true);
        }
    }

    /** Event ids are retained for scheduling; only authored Chinese names reach chat. */
    private static Component displayName(String eventId) {
        String id = eventId == null ? "" : eventId.trim().toLowerCase(Locale.ROOT);
        String authored = WorldpackDataService.builtin().findDailyEvent(id)
                .map(WorldpackDataService.DailyEvent::displayZh)
                .orElse("");
        if (PlayerDisplayText.isSafe(authored)) {
            return Component.literal(authored.trim());
        }
        if (id.contains("merchant") || id.contains("caravan")) {
            return Component.literal("商队异象");
        }
        if (id.contains("beast") || id.contains("migration") || id.contains("tide")) {
            return Component.literal("灵兽异动");
        }
        if (id.contains("bandit") || id.contains("rogue") || id.contains("raid")) {
            return Component.literal("劫修来袭");
        }
        if (id.contains("demon") || id.contains("qi") || id.contains("corruption")) {
            return Component.literal("魔气侵染");
        }
        return Component.literal("未知异象");
    }
}
