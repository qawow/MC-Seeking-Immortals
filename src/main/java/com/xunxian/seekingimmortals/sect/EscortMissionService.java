package com.xunxian.seekingimmortals.sect;

import com.xunxian.seekingimmortals.catalog.SummonHonestMvpService;
import com.xunxian.seekingimmortals.entity.SectStewardEntity;
import com.xunxian.seekingimmortals.entity.SummonedServitorEntity;
import com.xunxian.seekingimmortals.registry.ModEntities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.UUID;

/**
 * Wave491: true escort mission using SummonedServitor FOLLOW stance.
 * Spawn escort → follow player → complete when both near a sect steward.
 */
public final class EscortMissionService {
    private static final String ROOT = "seeking_immortals_escort_mission";
    private static final String TAG_ENTITY = "EscortEntity";
    private static final String TAG_ACTIVE = "Active";
    private static final String ENTITY_TAG_ESCORT = "seeking_immortals_escort";
    private static final String ENTITY_TAG_OWNER = "seeking_immortals_escort_owner";
    private static final double ARRIVAL_RANGE = 5.0D;

    private EscortMissionService() {}

    public static boolean startEscort(ServerPlayer player) {
        if (player == null || !(player.level() instanceof ServerLevel level)) {
            return false;
        }
        clearEscort(player, true);
        SummonedServitorEntity escort = ModEntities.SUMMONED_SERVITOR.get().create(level);
        if (escort == null) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.escort.spawn_failed"), true);
            return false;
        }
        escort.moveTo(player.getX() + 1.2D, player.getY(), player.getZ() + 0.5D, player.getYRot(), 0.0F);
        escort.configure(player, "sect_escort_charge", 20 * 60 * 8, 36.0D, 4.0D,
                SummonedServitorEntity.Archetype.GENERIC);
        escort.setStance(SummonedServitorEntity.Stance.FOLLOW);
        escort.getPersistentData().putBoolean(ENTITY_TAG_ESCORT, true);
        escort.getPersistentData().putUUID(ENTITY_TAG_OWNER, player.getUUID());
        if (!level.addFreshEntity(escort)) {
            escort.discard();
            player.displayClientMessage(Component.translatable("message.seeking_immortals.escort.spawn_failed"), true);
            return false;
        }

        CompoundTag tag = new CompoundTag();
        tag.putBoolean(TAG_ACTIVE, true);
        tag.putUUID(TAG_ENTITY, escort.getUUID());
        player.getPersistentData().put(ROOT, tag);
        player.displayClientMessage(Component.translatable("message.seeking_immortals.escort.started"), false);
        return true;
    }

    public static boolean isActive(ServerPlayer player) {
        return player != null && player.getPersistentData().getCompound(ROOT).getBoolean(TAG_ACTIVE);
    }

    public static boolean onStewardContact(ServerPlayer player, SectStewardEntity steward) {
        if (!isActive(player) || steward == null || !steward.isAlive()
                || !(player.level() instanceof ServerLevel level) || steward.level() != level) {
            return false;
        }
        CompoundTag tag = player.getPersistentData().getCompound(ROOT);
        if (!tag.hasUUID(TAG_ENTITY)) {
            failEscort(player);
            return false;
        }
        UUID id = tag.getUUID(TAG_ENTITY);
        Entity entity = level.getEntity(id);
        if (!(entity instanceof SummonedServitorEntity escort) || !escort.isAlive()
                || !escort.getPersistentData().getBoolean(ENTITY_TAG_ESCORT)
                || !escort.getPersistentData().hasUUID(ENTITY_TAG_OWNER)
                || !player.getUUID().equals(escort.getPersistentData().getUUID(ENTITY_TAG_OWNER))) {
            failEscort(player);
            return false;
        }
        double maxDistanceSqr = ARRIVAL_RANGE * ARRIVAL_RANGE;
        if (escort.distanceToSqr(player) > maxDistanceSqr || escort.distanceToSqr(steward) > maxDistanceSqr) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.escort.too_far"), true);
            return false;
        }
        clearEscort(player, true);
        return true;
    }

    public static void tick(ServerPlayer player) {
        if (!isActive(player) || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        CompoundTag tag = player.getPersistentData().getCompound(ROOT);
        if (!tag.hasUUID(TAG_ENTITY)) {
            failEscort(player);
            return;
        }
        Entity entity = level.getEntity(tag.getUUID(TAG_ENTITY));
        if (!(entity instanceof SummonedServitorEntity escort) || !escort.isAlive()) {
            failEscort(player);
            return;
        }
        // Keep follow stance while active.
        if (escort.getStance() != SummonedServitorEntity.Stance.FOLLOW) {
            escort.setStance(SummonedServitorEntity.Stance.FOLLOW);
        }
    }

    public static void clearEscort(ServerPlayer player, boolean dismissEntity) {
        if (player == null) {
            return;
        }
        CompoundTag tag = player.getPersistentData().getCompound(ROOT);
        if (dismissEntity && tag.hasUUID(TAG_ENTITY) && player.level() instanceof ServerLevel level) {
            Entity entity = level.getEntity(tag.getUUID(TAG_ENTITY));
            if (entity != null) {
                entity.discard();
            } else {
                // Fallback: discard nearby escort-tagged servitors owned by player.
                AABB box = player.getBoundingBox().inflate(24.0D);
                List<SummonedServitorEntity> list = level.getEntitiesOfClass(SummonedServitorEntity.class, box,
                        e -> e.getPersistentData().getBoolean(ENTITY_TAG_ESCORT)
                                && e.getPersistentData().hasUUID(ENTITY_TAG_OWNER)
                                && player.getUUID().equals(e.getPersistentData().getUUID(ENTITY_TAG_OWNER)));
                for (SummonedServitorEntity e : list) {
                    e.discard();
                }
            }
        }
        player.getPersistentData().remove(ROOT);
    }

    private static void failEscort(ServerPlayer player) {
        player.displayClientMessage(Component.translatable("message.seeking_immortals.escort.lost"), true);
        clearEscort(player, true);
    }

    /** Reuse SummonHonestMvpService ownership cap awareness when spawning via other paths. */
    public static int activeOwnedServitors(ServerPlayer player) {
        return SummonHonestMvpService.countOwnedServitors(player);
    }
}
