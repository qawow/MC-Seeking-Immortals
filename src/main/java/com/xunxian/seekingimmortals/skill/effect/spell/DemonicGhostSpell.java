package com.xunxian.seekingimmortals.skill.effect.spell;

import com.xunxian.seekingimmortals.cultivation.PlayerCultivation;
import com.xunxian.seekingimmortals.skill.CultivationSkill;
import com.xunxian.seekingimmortals.skill.effect.SkillContext;
import java.util.List;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class DemonicGhostSpell extends SpellEffect {
    private final double range;
    private final double radius;
    private final DemonicGhostForm form;
    private final String successKey;

    public DemonicGhostSpell(int cost, int cooldownTicks, double damage, double range, double radius,
                             DemonicGhostForm form, String successKey) {
        super(cost, cooldownTicks, damage);
        this.range = range;
        this.radius = radius;
        this.form = form;
        this.successKey = successKey;
    }

    @Override
    public boolean execute(ServerPlayer player, PlayerCultivation cultivation, CultivationSkill skill, SkillContext context) {
        return switch (form) {
            case BLOOD_SHADOW_ESCAPE -> castBloodShadowEscape(player, skill);
            case SKY_SUPPORTING_DEMONIC_SKILL -> castSkySupportingSkill(player, skill);
            case MYSTIC_SOUL_GHOST_FIRE -> castGhostFire(player, skill);
            case MYSTIC_SOUL_BONE_CONDENSING_ART -> castBoneCondensingArt(player, skill);
            case BLOOD_LUO_BARRIER -> castBloodLuoBarrier(player, skill);
            case YIN_DEMON_SLASH -> castYinDemonSlash(player, skill);
        };
    }

    private boolean castBloodShadowEscape(ServerPlayer player, CultivationSkill skill) {
        ServerLevel level = player.serverLevel();
        Vec3 flat = new Vec3(player.getLookAngle().x, 0.0D, player.getLookAngle().z);
        if (flat.lengthSqr() < 0.001D) {
            return false;
        }
        flat = flat.normalize();

        Vec3 origin = player.position();
        Vec3 destination = findSafeEscapeDestination(level, player, origin, flat);
        if (destination == null) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.spell.blood_shadow_escape.fail"), true);
            return false;
        }

        List<LivingEntity> targets = findLineTargets(level, player,
                origin.add(0.0D, 0.8D, 0.0D), destination.add(0.0D, 0.8D, 0.0D), Math.max(0.9D, radius));
        double damage = calculateDamage(skill.getLevel(), skill.getProficiency());
        for (LivingEntity target : targets) {
            target.hurt(player.damageSources().indirectMagic(player, player), (float)damage);
            add(target, MobEffects.WITHER, scaled(65, skill, 5), 0);
            add(target, MobEffects.MOVEMENT_SLOWDOWN, scaled(45, skill, 4), 1);
        }

        form.spawnTrail(level, origin.add(0.0D, 0.75D, 0.0D), destination.add(0.0D, 0.75D, 0.0D));
        level.playSound(null, player.blockPosition(), form.sound, SoundSource.PLAYERS, 0.78F, form.pitch);
        player.teleportTo(destination.x, destination.y, destination.z);
        player.fallDistance = 0.0F;
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, scaled(70, skill, 5), 1, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, scaled(35, skill, 3), 0, false, true));
        player.displayClientMessage(Component.translatable(successKey, targets.size()), true);
        return true;
    }

    private boolean castSkySupportingSkill(ServerPlayer player, CultivationSkill skill) {
        ServerLevel level = player.serverLevel();
        int duration = scaled(190, skill, 14);
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, duration, 1, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, duration, Math.max(1, skill.getLevel() / 5), false, true));
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, duration, 0, false, true));
        int deflected = deflectProjectiles(player, level, radius);
        form.spawnDome(level, player.position().add(0.0D, 0.8D, 0.0D), radius, 4);
        level.playSound(null, player.blockPosition(), form.sound, SoundSource.PLAYERS, 0.80F, form.pitch);
        player.displayClientMessage(Component.translatable(successKey, deflected), true);
        return true;
    }

    private boolean castGhostFire(ServerPlayer player, CultivationSkill skill) {
        ServerLevel level = player.serverLevel();
        Vec3 direction = player.getLookAngle();
        if (direction.lengthSqr() < 0.001D) {
            return false;
        }
        direction = direction.normalize();
        Vec3 start = player.getEyePosition().add(direction.scale(0.45D));
        Vec3 end = clipEnd(level, player, start, direction, range);
        List<LivingEntity> targets = findLineTargets(level, player, start, end, Math.max(0.55D, radius));
        if (targets.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.spell.target.fail"), true);
            return false;
        }

        double damage = calculateDamage(skill.getLevel(), skill.getProficiency());
        for (LivingEntity target : targets) {
            double along = target.position().subtract(start).dot(direction);
            double falloff = Math.max(0.50D, 1.0D - along / (Math.max(1.0D, range) * 1.25D));
            target.hurt(player.damageSources().indirectMagic(player, player), (float)(damage * falloff));
            add(target, MobEffects.WITHER, scaled(95, skill, 7), 1);
            add(target, MobEffects.WEAKNESS, scaled(80, skill, 6), 1);
            add(target, MobEffects.BLINDNESS, scaled(40, skill, 4), 0);
        }

        form.spawnBeam(level, start, end);
        level.playSound(null, player.blockPosition(), form.sound, SoundSource.PLAYERS, 0.82F, form.pitch);
        player.displayClientMessage(Component.translatable(successKey, targets.size()), true);
        return true;
    }

    private boolean castBoneCondensingArt(ServerPlayer player, CultivationSkill skill) {
        ServerLevel level = player.serverLevel();
        int duration = scaled(180, skill, 14);
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, duration, 0, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, duration, 0, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, duration, Math.max(0, skill.getLevel() / 6), false, true));
        form.spawnArmor(level, player, radius);
        level.playSound(null, player.blockPosition(), form.sound, SoundSource.PLAYERS, 0.74F, form.pitch);
        player.displayClientMessage(Component.translatable(successKey, duration / 20), true);
        return true;
    }

    private boolean castBloodLuoBarrier(ServerPlayer player, CultivationSkill skill) {
        ServerLevel level = player.serverLevel();
        int duration = scaled(170, skill, 12);
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, duration, 0, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, duration, Math.max(1, skill.getLevel() / 5), false, true));
        int deflected = deflectProjectiles(player, level, radius);
        int repelled = repelNearbyTargets(player, level, skill, radius);
        form.spawnDome(level, player.position().add(0.0D, 0.85D, 0.0D), radius, 3);
        level.playSound(null, player.blockPosition(), form.sound, SoundSource.PLAYERS, 0.78F, form.pitch);
        player.displayClientMessage(Component.translatable(successKey, repelled, deflected), true);
        return true;
    }

    private boolean castYinDemonSlash(ServerPlayer player, CultivationSkill skill) {
        ServerLevel level = player.serverLevel();
        Vec3 direction = player.getLookAngle();
        if (direction.lengthSqr() < 0.001D) {
            return false;
        }
        direction = direction.normalize();
        Vec3 start = player.getEyePosition().add(direction.scale(0.55D));
        Vec3 end = clipEnd(level, player, start, direction, range);
        List<LivingEntity> targets = findLineTargets(level, player, start, end, Math.max(0.75D, radius));
        if (targets.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.spell.target.fail"), true);
            return false;
        }

        double damage = calculateDamage(skill.getLevel(), skill.getProficiency());
        for (LivingEntity target : targets) {
            target.hurt(player.damageSources().indirectMagic(player, player), (float)damage);
            add(target, MobEffects.WEAKNESS, scaled(90, skill, 7), 1);
            add(target, MobEffects.MOVEMENT_SLOWDOWN, scaled(55, skill, 5), 1);
        }

        form.spawnSlash(level, start, end, radius);
        level.playSound(null, player.blockPosition(), form.sound, SoundSource.PLAYERS, 0.84F, form.pitch);
        player.displayClientMessage(Component.translatable(successKey, targets.size()), true);
        return true;
    }

    private Vec3 clipEnd(ServerLevel level, ServerPlayer player, Vec3 start, Vec3 direction, double maxRange) {
        Vec3 maxEnd = start.add(direction.scale(maxRange));
        BlockHitResult blockHit = level.clip(new ClipContext(start, maxEnd, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        return blockHit.getType() == HitResult.Type.MISS ? maxEnd : blockHit.getLocation();
    }

    private Vec3 findSafeEscapeDestination(ServerLevel level, ServerPlayer player, Vec3 origin, Vec3 flat) {
        Vec3 eye = player.getEyePosition();
        Vec3 maxEnd = eye.add(flat.scale(range));
        BlockHitResult blockHit = level.clip(new ClipContext(eye, maxEnd, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        double maxDistance = blockHit.getType() == HitResult.Type.MISS
                ? range
                : Math.max(3.0D, blockHit.getLocation().subtract(eye).length() - 0.85D);
        for (double distance = maxDistance; distance >= 3.0D; distance -= 0.75D) {
            Vec3 target = origin.add(flat.scale(distance));
            BlockPos base = BlockPos.containing(target);
            for (int dy = 2; dy >= -4; dy--) {
                BlockPos feet = base.offset(0, dy, 0);
                if (level.isLoaded(feet) && canStandAt(level, feet)) {
                    return new Vec3(feet.getX() + 0.5D, feet.getY(), feet.getZ() + 0.5D);
                }
            }
        }
        return null;
    }

    private List<LivingEntity> findLineTargets(ServerLevel level, ServerPlayer player, Vec3 start, Vec3 end, double maxRadius) {
        Vec3 line = end.subtract(start);
        AABB box = new AABB(start, end).inflate(maxRadius, maxRadius + 0.5D, maxRadius);
        return level.getEntitiesOfClass(LivingEntity.class, box,
                entity -> canTarget(entity, player)
                        && distanceToSegment(entity.position().add(0.0D, entity.getBbHeight() * 0.52D, 0.0D), start, line) <= maxRadius);
    }

    private int repelNearbyTargets(ServerPlayer player, ServerLevel level, CultivationSkill skill, double maxRadius) {
        AABB area = player.getBoundingBox().inflate(maxRadius, maxRadius * 0.65D, maxRadius);
        double damage = Math.max(4.0D, calculateDamage(skill.getLevel(), skill.getProficiency()) * 0.38D);
        int hit = 0;
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area, entity -> canTarget(entity, player))) {
            Vec3 away = target.position().subtract(player.position());
            if (away.lengthSqr() < 0.001D) {
                away = player.getLookAngle();
            }
            target.hurt(player.damageSources().indirectMagic(player, player), (float)damage);
            target.push(away.normalize().x * 0.72D, 0.18D, away.normalize().z * 0.72D);
            target.hasImpulse = true;
            add(target, MobEffects.MOVEMENT_SLOWDOWN, scaled(45, skill, 4), 1);
            hit++;
        }
        return hit;
    }

    private int deflectProjectiles(ServerPlayer player, ServerLevel level, double maxRadius) {
        AABB area = player.getBoundingBox().inflate(maxRadius, maxRadius * 0.75D, maxRadius);
        List<Projectile> projectiles = level.getEntitiesOfClass(Projectile.class, area,
                projectile -> projectile.isAlive() && projectile.getOwner() != player);
        int changed = 0;
        for (Projectile projectile : projectiles) {
            Vec3 direction = projectile.position().subtract(player.position());
            if (direction.lengthSqr() < 0.001D) {
                direction = player.getLookAngle();
            }
            projectile.setOwner(player);
            projectile.setDeltaMovement(direction.normalize()
                    .scale(Math.max(0.50D, projectile.getDeltaMovement().length() + 0.18D))
                    .add(0.0D, 0.05D, 0.0D));
            projectile.hasImpulse = true;
            changed++;
        }
        return changed;
    }

    private static boolean canTarget(Entity entity, ServerPlayer player) {
        return canAffect(player, entity);
    }

    private static boolean canStandAt(ServerLevel level, BlockPos feet) {
        BlockState feetState = level.getBlockState(feet);
        BlockState headState = level.getBlockState(feet.above());
        BlockState belowState = level.getBlockState(feet.below());
        return belowState.isSolidRender(level, feet.below())
                && feetState.getCollisionShape(level, feet).isEmpty()
                && headState.getCollisionShape(level, feet.above()).isEmpty();
    }

    private static double distanceToSegment(Vec3 point, Vec3 start, Vec3 line) {
        double lengthSqr = line.lengthSqr();
        if (lengthSqr < 0.0001D) {
            return point.distanceTo(start);
        }
        double t = Math.max(0.0D, Math.min(1.0D, point.subtract(start).dot(line) / lengthSqr));
        return point.distanceTo(start.add(line.scale(t)));
    }

    private static void add(LivingEntity target, MobEffect effect, int durationTicks, int amplifier) {
        target.addEffect(new MobEffectInstance(effect, durationTicks, amplifier, false, true));
    }

    private static int scaled(int baseTicks, CultivationSkill skill, int perLevelTicks) {
        return baseTicks + Math.max(0, skill.getLevel() - 1) * perLevelTicks;
    }

    public enum DemonicGhostForm {
        BLOOD_SHADOW_ESCAPE(new DustParticleOptions(new Vector3f(0.82F, 0.03F, 0.05F), 0.58F),
                new DustParticleOptions(new Vector3f(0.18F, 0.02F, 0.03F), 0.82F),
                SoundEvents.WITHER_SHOOT, 1.68F),
        SKY_SUPPORTING_DEMONIC_SKILL(new DustParticleOptions(new Vector3f(0.20F, 0.03F, 0.28F), 0.82F),
                new DustParticleOptions(new Vector3f(0.82F, 0.10F, 0.18F), 0.42F),
                SoundEvents.BEACON_ACTIVATE, 0.58F),
        MYSTIC_SOUL_GHOST_FIRE(new DustParticleOptions(new Vector3f(0.12F, 0.92F, 0.58F), 0.58F),
                new DustParticleOptions(new Vector3f(0.28F, 0.02F, 0.38F), 0.78F),
                SoundEvents.SOUL_ESCAPE, 1.20F),
        MYSTIC_SOUL_BONE_CONDENSING_ART(new DustParticleOptions(new Vector3f(0.82F, 0.86F, 0.78F), 0.50F),
                new DustParticleOptions(new Vector3f(0.18F, 0.14F, 0.20F), 0.68F),
                SoundEvents.BONE_BLOCK_PLACE, 0.78F),
        BLOOD_LUO_BARRIER(new DustParticleOptions(new Vector3f(0.72F, 0.02F, 0.07F), 0.66F),
                new DustParticleOptions(new Vector3f(0.18F, 0.02F, 0.08F), 0.78F),
                SoundEvents.SHIELD_BLOCK, 0.72F),
        YIN_DEMON_SLASH(new DustParticleOptions(new Vector3f(0.36F, 0.02F, 0.42F), 0.62F),
                new DustParticleOptions(new Vector3f(0.88F, 0.04F, 0.15F), 0.45F),
                SoundEvents.PLAYER_ATTACK_SWEEP, 0.62F);

        private final DustParticleOptions core;
        private final DustParticleOptions edge;
        private final SoundEvent sound;
        private final float pitch;

        DemonicGhostForm(DustParticleOptions core, DustParticleOptions edge, SoundEvent sound, float pitch) {
            this.core = core;
            this.edge = edge;
            this.sound = sound;
            this.pitch = pitch;
        }

        private void spawnTrail(ServerLevel level, Vec3 start, Vec3 end) {
            Vec3 line = end.subtract(start);
            int steps = Math.max(8, (int)(line.length() * 5.5D));
            for (int i = 0; i <= steps; i++) {
                double t = i / (double)steps;
                Vec3 point = start.lerp(end, t);
                double sway = Math.sin(t * Math.PI * 8.0D) * 0.18D;
                level.sendParticles((i & 1) == 0 ? core : edge,
                        point.x + sway, point.y + Math.sin(t * Math.PI) * 0.36D, point.z - sway,
                        2, 0.05D, 0.05D, 0.05D, 0.004D);
            }
            level.sendParticles(edge, end.x, end.y, end.z, 24, 0.22D, 0.20D, 0.22D, 0.018D);
        }

        private void spawnBeam(ServerLevel level, Vec3 start, Vec3 end) {
            Vec3 line = end.subtract(start);
            int steps = Math.max(10, (int)(line.length() * 5.0D));
            for (int i = 0; i <= steps; i++) {
                double t = i / (double)steps;
                Vec3 point = start.lerp(end, t);
                level.sendParticles(core, point.x, point.y, point.z, 1, 0.020D, 0.020D, 0.020D, 0.0D);
                if ((i & 1) == 0) {
                    double coil = t * Math.PI * 7.0D;
                    level.sendParticles(edge,
                            point.x + Math.cos(coil) * 0.13D,
                            point.y + Math.sin(coil) * 0.13D,
                            point.z + Math.sin(coil * 0.7D) * 0.13D,
                            1, 0.025D, 0.025D, 0.025D, 0.0D);
                }
            }
            level.sendParticles(edge, end.x, end.y, end.z, 28, 0.18D, 0.16D, 0.18D, 0.012D);
        }

        private void spawnDome(ServerLevel level, Vec3 center, double radius, int layers) {
            for (int layer = 0; layer < layers; layer++) {
                double y = center.y - 0.35D + layer * 0.45D;
                double ringRadius = Math.max(0.75D, radius * (1.0D - layer * 0.13D));
                ring(level, new Vec3(center.x, y, center.z), ringRadius, 58 - layer * 6, 0.10D);
            }
            for (int spoke = 0; spoke < 8; spoke++) {
                double angle = Math.PI * 2.0D * spoke / 8.0D;
                Vec3 top = center.add(0.0D, 1.15D, 0.0D);
                Vec3 base = center.add(Math.cos(angle) * radius, -0.35D, Math.sin(angle) * radius);
                line(level, top, base, 10);
            }
        }

        private void spawnArmor(ServerLevel level, ServerPlayer player, double radius) {
            Vec3 base = player.position();
            for (int layer = 0; layer < 4; layer++) {
                ring(level, base.add(0.0D, 0.28D + layer * 0.42D, 0.0D),
                        Math.max(0.62D, radius * 0.32D - layer * 0.05D), 44, 0.04D);
            }
            for (int rib = 0; rib < 10; rib++) {
                double angle = Math.PI * 2.0D * rib / 10.0D;
                double x = base.x + Math.cos(angle) * 0.72D;
                double z = base.z + Math.sin(angle) * 0.72D;
                line(level, new Vec3(x, base.y + 0.28D, z), new Vec3(x, base.y + 1.62D, z), 6);
            }
        }

        private void spawnSlash(ServerLevel level, Vec3 start, Vec3 end, double width) {
            Vec3 line = end.subtract(start);
            Vec3 direction = line.lengthSqr() < 0.0001D ? new Vec3(0.0D, 0.0D, 1.0D) : line.normalize();
            Vec3 side = direction.cross(new Vec3(0.0D, 1.0D, 0.0D));
            if (side.lengthSqr() < 0.0001D) {
                side = new Vec3(1.0D, 0.0D, 0.0D);
            } else {
                side = side.normalize();
            }
            int steps = Math.max(8, (int)(line.length() * 4.0D));
            for (int i = 0; i <= steps; i++) {
                double t = i / (double)steps;
                Vec3 center = start.lerp(end, t);
                double sweep = Math.sin(t * Math.PI) * width;
                Vec3 left = center.add(side.scale(sweep));
                Vec3 right = center.subtract(side.scale(sweep * 0.72D));
                level.sendParticles(core, left.x, left.y, left.z, 1, 0.018D, 0.018D, 0.018D, 0.0D);
                level.sendParticles(edge, right.x, right.y, right.z, 1, 0.018D, 0.018D, 0.018D, 0.0D);
            }
        }

        private void ring(ServerLevel level, Vec3 center, double radius, int points, double wave) {
            for (int i = 0; i < points; i++) {
                double angle = Math.PI * 2.0D * i / points;
                level.sendParticles((i & 1) == 0 ? core : edge,
                        center.x + Math.cos(angle) * radius,
                        center.y + Math.sin(angle * 3.0D) * wave,
                        center.z + Math.sin(angle) * radius,
                        1, 0.020D, 0.020D, 0.020D, 0.0D);
            }
        }

        private void line(ServerLevel level, Vec3 start, Vec3 end, int points) {
            for (int i = 0; i <= points; i++) {
                Vec3 point = start.lerp(end, i / (double)points);
                level.sendParticles((i & 1) == 0 ? core : edge,
                        point.x, point.y, point.z, 1, 0.014D, 0.014D, 0.014D, 0.0D);
            }
        }
    }
}
