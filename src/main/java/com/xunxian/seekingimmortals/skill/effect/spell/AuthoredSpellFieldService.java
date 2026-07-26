package com.xunxian.seekingimmortals.skill.effect.spell;

import com.xunxian.seekingimmortals.entity.CultivationBeastEntity;
import com.xunxian.seekingimmortals.entity.SummonedServitorEntity;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/** Short-lived server-authoritative fields with PvP and ownership filtering. */
public final class AuthoredSpellFieldService {
    private static final int PULSE_INTERVAL = 20;
    private static final int EFFECT_DURATION = 30;
    private static final int MAX_FIELDS_PER_SERVER = 128;
    private static final List<ActiveField> ACTIVE = new ArrayList<>();

    private AuthoredSpellFieldService() {}

    public static void activate(ServerPlayer caster, String profileId, Vec3 center,
                                double radius, int durationTicks, int maxTargets,
                                boolean beneficial, double pulseDamage,
                                MobEffect primary, MobEffect secondary, boolean ignite) {
        if (caster == null || center == null || caster.getServer() == null) {
            return;
        }
        MinecraftServer server = caster.getServer();
        long now = caster.serverLevel().getGameTime();
        ACTIVE.removeIf(field -> field.server == server
                && field.casterId.equals(caster.getUUID())
                && field.dimension == caster.level().dimension()
                && field.profileId.equals(profileId));
        while (countFor(server) >= MAX_FIELDS_PER_SERVER) {
            removeOldest(server);
        }
        ACTIVE.add(new ActiveField(server, caster.level().dimension(), caster.getUUID(), profileId,
                center, Math.max(0.5D, Math.min(8.0D, radius)),
                now + Math.max(PULSE_INTERVAL, durationTicks), now,
                Math.max(1, Math.min(32, maxTargets)), beneficial,
                Math.max(0.0D, pulseDamage), primary, secondary, ignite));
    }

    public static void serverTick(MinecraftServer server) {
        if (server == null || server.getTickCount() % PULSE_INTERVAL != 0) {
            return;
        }
        Iterator<ActiveField> iterator = ACTIVE.iterator();
        while (iterator.hasNext()) {
            ActiveField field = iterator.next();
            if (field.server != server) {
                continue;
            }
            ServerLevel level = server.getLevel(field.dimension);
            long now = level == null ? Long.MAX_VALUE : level.getGameTime();
            ServerPlayer caster = server.getPlayerList().getPlayer(field.casterId);
            if (level == null || caster == null || caster.serverLevel() != level
                    || !caster.isAlive() || now >= field.expiresAt) {
                iterator.remove();
                continue;
            }
            if (now < field.nextPulseAt) {
                continue;
            }
            field.nextPulseAt = now + PULSE_INTERVAL;
            pulse(level, caster, field);
        }
    }

    public static void clearLevel(ServerLevel level) {
        if (level != null) {
            ACTIVE.removeIf(field -> field.server == level.getServer()
                    && field.dimension == level.dimension());
        }
    }

    public static void clearAll() {
        ACTIVE.clear();
    }

    static int activeCount() {
        return ACTIVE.size();
    }

    private static void pulse(ServerLevel level, ServerPlayer caster, ActiveField field) {
        AABB area = new AABB(field.center, field.center)
                .inflate(field.radius, Math.max(2.0D, field.radius * 0.65D), field.radius);
        level.getEntitiesOfClass(LivingEntity.class, area,
                        entity -> field.beneficial ? canBenefit(caster, entity) : canHarm(caster, entity))
                .stream().filter(entity -> entity.position().distanceToSqr(field.center)
                        <= square(field.radius + entity.getBbWidth()))
                .sorted(Comparator.comparingDouble(entity -> entity.distanceToSqr(field.center)))
                .limit(field.maxTargets).forEach(target -> apply(caster, target, field));
    }

    private static void apply(ServerPlayer caster, LivingEntity target, ActiveField field) {
        if (!field.beneficial && field.pulseDamage > 0.0D) {
            target.hurt(caster.damageSources().indirectMagic(caster, caster), (float) field.pulseDamage);
        }
        if (field.primary != null) {
            target.addEffect(new MobEffectInstance(field.primary, EFFECT_DURATION, 0, false, true));
        }
        if (field.secondary != null && field.secondary != field.primary) {
            target.addEffect(new MobEffectInstance(field.secondary, EFFECT_DURATION, 0, false, true));
        }
        if (!field.beneficial && field.ignite) {
            target.setSecondsOnFire(2);
        }
    }

    private static boolean canHarm(ServerPlayer caster, LivingEntity target) {
        if (canBenefit(caster, target) || !target.isAlive() || target.isSpectator()) {
            return false;
        }
        return !(target instanceof Player player) || caster.canHarmPlayer(player);
    }

    private static boolean canBenefit(ServerPlayer caster, LivingEntity target) {
        if (!target.isAlive() || target.isSpectator()) {
            return false;
        }
        if (target == caster || caster.isAlliedTo(target)) {
            return true;
        }
        UUID casterId = caster.getUUID();
        if (target instanceof SummonedServitorEntity servitor) {
            return servitor.getOwnerUUID().map(casterId::equals).orElse(false);
        }
        if (target instanceof CultivationBeastEntity beast) {
            return beast.isCompanion() && beast.getOwnerUUID().map(casterId::equals).orElse(false);
        }
        return false;
    }

    private static int countFor(MinecraftServer server) {
        int count = 0;
        for (ActiveField field : ACTIVE) {
            if (field.server == server) {
                count++;
            }
        }
        return count;
    }

    private static void removeOldest(MinecraftServer server) {
        for (Iterator<ActiveField> iterator = ACTIVE.iterator(); iterator.hasNext();) {
            if (iterator.next().server == server) {
                iterator.remove();
                return;
            }
        }
    }

    private static double square(double value) {
        return value * value;
    }

    private static final class ActiveField {
        private final MinecraftServer server;
        private final ResourceKey<Level> dimension;
        private final UUID casterId;
        private final String profileId;
        private final Vec3 center;
        private final double radius;
        private final long expiresAt;
        private long nextPulseAt;
        private final int maxTargets;
        private final boolean beneficial;
        private final double pulseDamage;
        private final MobEffect primary;
        private final MobEffect secondary;
        private final boolean ignite;

        private ActiveField(MinecraftServer server, ResourceKey<Level> dimension, UUID casterId,
                            String profileId, Vec3 center, double radius, long expiresAt,
                            long nextPulseAt, int maxTargets, boolean beneficial, double pulseDamage,
                            MobEffect primary, MobEffect secondary, boolean ignite) {
            this.server = server;
            this.dimension = dimension;
            this.casterId = casterId;
            this.profileId = profileId == null ? "" : profileId;
            this.center = center;
            this.radius = radius;
            this.expiresAt = expiresAt;
            this.nextPulseAt = nextPulseAt;
            this.maxTargets = maxTargets;
            this.beneficial = beneficial;
            this.pulseDamage = pulseDamage;
            this.primary = primary;
            this.secondary = secondary;
            this.ignite = ignite;
        }
    }
}
