package com.xunxian.seekingimmortals.skill.effect.spell;

import com.xunxian.seekingimmortals.cultivation.PlayerCultivation;
import com.xunxian.seekingimmortals.skill.CultivationSkill;
import com.xunxian.seekingimmortals.skill.effect.SkillContext;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class CoreElementalAreaSpell extends SpellEffect {
    private final double range;
    private final double radius;
    private final CoreElement element;
    private final String successKey;

    public CoreElementalAreaSpell(int cost, int cooldownTicks, double damage, double range, double radius,
                                  CoreElement element, String successKey) {
        super(cost, cooldownTicks, damage);
        this.range = range;
        this.radius = radius;
        this.element = element;
        this.successKey = successKey;
    }

    @Override
    public boolean execute(ServerPlayer player, PlayerCultivation cultivation, CultivationSkill skill, SkillContext context) {
        ServerLevel level = player.serverLevel();
        Vec3 center = findImpactPoint(level, player);
        List<LivingEntity> targets = findTargets(level, player, center);
        int bentProjectiles = element == CoreElement.PRIMORDIAL_MAGNET
                ? bendProjectiles(level, player, center, radius + 1.2D)
                : 0;
        if (targets.isEmpty() && bentProjectiles == 0) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.spell.area.fail"), true);
            return false;
        }

        double damage = calculateDamage(skill.getLevel(), skill.getProficiency());
        int hitCount = 0;
        for (LivingEntity target : targets) {
            double distance = target.position().distanceTo(center);
            double falloff = Math.max(0.48D, 1.0D - distance / (radius + 0.75D));
            if (damage > 0.0D) {
                target.hurt(player.damageSources().magic(), (float)(damage * falloff));
            }
            element.applyEffects(target, center, skill);
            hitCount++;
        }

        element.spawnVisual(level, center, radius, targets);
        level.playSound(null, center.x, center.y, center.z, element.sound, SoundSource.PLAYERS, 0.82F, element.pitch);
        player.displayClientMessage(Component.translatable(successKey, hitCount), true);
        return true;
    }

    private Vec3 findImpactPoint(ServerLevel level, ServerPlayer player) {
        Vec3 start = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        if (look.lengthSqr() < 0.001D) {
            return player.position();
        }
        Vec3 end = start.add(look.normalize().scale(range));
        BlockHitResult hit = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        return hit.getType() == HitResult.Type.MISS ? end : hit.getLocation();
    }

    private List<LivingEntity> findTargets(ServerLevel level, ServerPlayer player, Vec3 center) {
        AABB area = new AABB(center, center).inflate(radius, 2.25D, radius);
        double maxDistance = radius + 0.75D;
        return level.getEntitiesOfClass(LivingEntity.class, area,
                        entity -> entity != player && entity.isAlive() && !entity.isSpectator()
                                && entity.position().distanceToSqr(center) <= maxDistance * maxDistance)
                .stream()
                .sorted(Comparator.comparingDouble(entity -> entity.distanceToSqr(center)))
                .toList();
    }

    private int bendProjectiles(ServerLevel level, ServerPlayer player, Vec3 center, double magnetRadius) {
        AABB area = new AABB(center, center).inflate(magnetRadius, magnetRadius * 0.7D, magnetRadius);
        List<Projectile> projectiles = level.getEntitiesOfClass(Projectile.class, area,
                projectile -> projectile.isAlive() && projectile.getOwner() != player);
        int bent = 0;
        Vec3 focus = center.add(0.0D, 0.85D, 0.0D);
        for (Projectile projectile : projectiles) {
            Vec3 pull = focus.subtract(projectile.position());
            if (pull.lengthSqr() < 0.001D) {
                continue;
            }
            double speed = Math.max(0.32D, projectile.getDeltaMovement().length());
            projectile.setDeltaMovement(pull.normalize().scale(speed * 0.72D));
            projectile.hasImpulse = true;
            bent++;
        }
        return bent;
    }

    public enum CoreElement {
        PRIMORDIAL_MAGNET(new DustParticleOptions(new Vector3f(0.58F, 0.78F, 1.00F), 0.72F),
                new DustParticleOptions(new Vector3f(0.98F, 0.88F, 0.36F), 0.48F),
                SoundEvents.BEACON_POWER_SELECT, 1.42F),
        FLAME_SERPENT_STORM(new DustParticleOptions(new Vector3f(1.00F, 0.18F, 0.04F), 0.82F),
                new DustParticleOptions(new Vector3f(1.00F, 0.72F, 0.16F), 0.48F),
                SoundEvents.BLAZE_SHOOT, 0.88F),
        EARTH_MOUNTAIN_PRESS(new DustParticleOptions(new Vector3f(0.56F, 0.40F, 0.20F), 0.86F),
                new DustParticleOptions(new Vector3f(0.86F, 0.70F, 0.36F), 0.52F),
                SoundEvents.ANVIL_LAND, 0.72F),
        XUANTIAN_ICE_PRISON(new DustParticleOptions(new Vector3f(0.48F, 0.82F, 1.00F), 0.72F),
                new DustParticleOptions(new Vector3f(0.92F, 1.00F, 1.00F), 0.42F),
                SoundEvents.GLASS_BREAK, 1.36F);

        private final DustParticleOptions core;
        private final DustParticleOptions edge;
        private final SoundEvent sound;
        private final float pitch;

        CoreElement(DustParticleOptions core, DustParticleOptions edge, SoundEvent sound, float pitch) {
            this.core = core;
            this.edge = edge;
            this.sound = sound;
            this.pitch = pitch;
        }

        private void applyEffects(LivingEntity target, Vec3 center, CultivationSkill skill) {
            int levelBonus = Math.max(0, skill.getLevel() - 1);
            switch (this) {
                case PRIMORDIAL_MAGNET -> {
                    add(target, MobEffects.MOVEMENT_SLOWDOWN, 100 + levelBonus * 8, 3);
                    add(target, MobEffects.WEAKNESS, 95 + levelBonus * 7, 1);
                    pullToward(target, center, 0.46D);
                }
                case FLAME_SERPENT_STORM -> {
                    target.setSecondsOnFire(6);
                    add(target, MobEffects.WEAKNESS, 80 + levelBonus * 6, 0);
                }
                case EARTH_MOUNTAIN_PRESS -> {
                    add(target, MobEffects.MOVEMENT_SLOWDOWN, 120 + levelBonus * 9, 3);
                    add(target, MobEffects.DIG_SLOWDOWN, 95 + levelBonus * 7, 1);
                    target.setDeltaMovement(target.getDeltaMovement().multiply(0.45D, 0.0D, 0.45D));
                    target.hasImpulse = true;
                }
                case XUANTIAN_ICE_PRISON -> {
                    add(target, MobEffects.MOVEMENT_SLOWDOWN, 145 + levelBonus * 10, 5);
                    add(target, MobEffects.DIG_SLOWDOWN, 125 + levelBonus * 8, 2);
                    add(target, MobEffects.WEAKNESS, 90 + levelBonus * 6, 0);
                    target.setTicksFrozen(Math.max(target.getTicksFrozen(), 160 + levelBonus * 10));
                }
            }
        }

        private static void pullToward(LivingEntity target, Vec3 center, double strength) {
            Vec3 pull = center.subtract(target.position());
            if (pull.lengthSqr() < 0.001D) {
                return;
            }
            target.push(pull.x * strength / Math.max(1.0D, pull.length()), 0.08D,
                    pull.z * strength / Math.max(1.0D, pull.length()));
            target.hasImpulse = true;
        }

        private void spawnVisual(ServerLevel level, Vec3 center, double radius, List<LivingEntity> targets) {
            switch (this) {
                case PRIMORDIAL_MAGNET -> spawnMagnetSphere(level, center, radius, targets);
                case FLAME_SERPENT_STORM -> spawnFlameSerpentStorm(level, center, radius);
                case EARTH_MOUNTAIN_PRESS -> spawnMountainPress(level, center, radius);
                case XUANTIAN_ICE_PRISON -> spawnIcePrison(level, center, radius);
            }
        }

        private void spawnMagnetSphere(ServerLevel level, Vec3 center, double radius, List<LivingEntity> targets) {
            ring(level, center.add(0.0D, 0.45D, 0.0D), radius, 72, 0.12D);
            ring(level, center.add(0.0D, 1.45D, 0.0D), radius * 0.72D, 56, 0.10D);
            for (int i = 0; i < 64; i++) {
                double angle = i * 2.399963D;
                double spread = radius * Math.sqrt((i % 32) / 32.0D);
                double y = center.y + 0.25D + (i % 14) * 0.12D;
                level.sendParticles((i & 1) == 0 ? core : edge,
                        center.x + Math.cos(angle) * spread, y,
                        center.z + Math.sin(angle) * spread, 1, 0.035D, 0.035D, 0.035D, 0.0D);
            }
            Vec3 focus = center.add(0.0D, 1.05D, 0.0D);
            int links = Math.min(6, targets.size());
            for (int i = 0; i < links; i++) {
                LivingEntity target = targets.get(i);
                arc(level, target.position().add(0.0D, target.getBbHeight() * 0.55D, 0.0D), focus, i * 19);
            }
        }

        private void spawnFlameSerpentStorm(ServerLevel level, Vec3 center, double radius) {
            for (int strand = 0; strand < 4; strand++) {
                double offset = strand * Math.PI * 0.5D;
                for (int i = 0; i < 44; i++) {
                    double t = i / 43.0D;
                    double angle = offset + t * Math.PI * 4.2D;
                    double swirl = radius * (0.22D + t * 0.62D);
                    double y = center.y + 0.2D + Math.sin(t * Math.PI) * 1.45D;
                    level.sendParticles((i & 1) == 0 ? core : edge,
                            center.x + Math.cos(angle) * swirl, y,
                            center.z + Math.sin(angle) * swirl, 1, 0.055D, 0.045D, 0.055D, 0.004D);
                }
            }
            level.sendParticles(edge, center.x, center.y + 0.75D, center.z, 36, radius * 0.32D, 0.45D, radius * 0.32D, 0.02D);
        }

        private void spawnMountainPress(ServerLevel level, Vec3 center, double radius) {
            for (int layer = 0; layer < 5; layer++) {
                double y = center.y + 3.1D - layer * 0.62D;
                double layerRadius = radius * (0.30D + layer * 0.14D);
                ring(level, new Vec3(center.x, y, center.z), layerRadius, 56, 0.04D);
            }
            ring(level, center.add(0.0D, 0.12D, 0.0D), radius, 84, 0.10D);
            level.sendParticles(core, center.x, center.y + 0.35D, center.z, 42, radius * 0.28D, 0.20D, radius * 0.28D, 0.018D);
        }

        private void spawnIcePrison(ServerLevel level, Vec3 center, double radius) {
            double cageRadius = Math.max(1.4D, radius * 0.58D);
            ring(level, center.add(0.0D, 0.18D, 0.0D), cageRadius, 64, 0.02D);
            ring(level, center.add(0.0D, 2.35D, 0.0D), cageRadius * 0.82D, 56, 0.02D);
            for (int bar = 0; bar < 12; bar++) {
                double angle = Math.PI * 2.0D * bar / 12.0D;
                double x = center.x + Math.cos(angle) * cageRadius;
                double z = center.z + Math.sin(angle) * cageRadius;
                for (int step = 0; step < 9; step++) {
                    double y = center.y + 0.25D + step * 0.24D;
                    level.sendParticles((step & 1) == 0 ? core : edge, x, y, z, 1, 0.018D, 0.018D, 0.018D, 0.0D);
                }
            }
            level.sendParticles(edge, center.x, center.y + 1.0D, center.z, 28, cageRadius * 0.26D, 0.62D, cageRadius * 0.26D, 0.0D);
        }

        private void ring(ServerLevel level, Vec3 center, double radius, int points, double wave) {
            for (int i = 0; i < points; i++) {
                double angle = Math.PI * 2.0D * i / points;
                DustParticleOptions particle = (i & 1) == 0 ? core : edge;
                level.sendParticles(particle, center.x + Math.cos(angle) * radius,
                        center.y + Math.sin(angle * 3.0D) * wave,
                        center.z + Math.sin(angle) * radius, 1, 0.025D, 0.025D, 0.025D, 0.0D);
            }
        }

        private void arc(ServerLevel level, Vec3 start, Vec3 end, int seed) {
            Vec3 path = end.subtract(start);
            int steps = Math.max(6, (int)(path.length() * 5.0D));
            for (int i = 0; i <= steps; i++) {
                double t = i / (double)steps;
                Vec3 point = start.lerp(end, t).add(Math.sin(seed + i * 1.37D) * 0.08D,
                        Math.cos(seed * 0.4D + i) * 0.05D,
                        Math.cos(seed + i * 1.19D) * 0.08D);
                level.sendParticles((i & 1) == 0 ? core : edge, point.x, point.y, point.z,
                        1, 0.018D, 0.018D, 0.018D, 0.0D);
            }
        }

        private static void add(LivingEntity target, MobEffect effect, int durationTicks, int amplifier) {
            target.addEffect(new MobEffectInstance(effect, durationTicks, amplifier, false, true));
        }
    }
}
