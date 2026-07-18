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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class XuanYinSpell extends SpellEffect {
    private final double range;
    private final double radius;
    private final XuanYinForm form;
    private final String successKey;

    public XuanYinSpell(int cost, int cooldownTicks, double damage, double range, double radius,
                        XuanYinForm form, String successKey) {
        super(cost, cooldownTicks, damage);
        this.range = range;
        this.radius = radius;
        this.form = form;
        this.successKey = successKey;
    }

    @Override
    public boolean execute(ServerPlayer player, PlayerCultivation cultivation, CultivationSkill skill, SkillContext context) {
        return switch (form) {
            case SOUL_DEVOURING_CLOUD -> castSoulCloud(player, skill);
            case YIN_SOUL_CHAIN -> castSoulChain(player, skill);
            case UNDERWORLD_FLAME -> castUnderworldFlame(player, skill);
            case CORPSE_ARMOR -> castCorpseArmor(player, skill);
        };
    }

    private boolean castSoulCloud(ServerPlayer player, CultivationSkill skill) {
        ServerLevel level = player.serverLevel();
        Vec3 center = findImpactPoint(level, player, range);
        List<LivingEntity> targets = findAreaTargets(level, player, center, radius);
        if (targets.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.spell.area.fail"), true);
            return false;
        }

        double damage = calculateDamage(skill.getLevel(), skill.getProficiency());
        int hitCount = 0;
        for (LivingEntity target : targets) {
            double falloff = Math.max(0.42D, 1.0D - target.position().distanceTo(center) / (radius + 0.8D));
            target.hurt(player.damageSources().indirectMagic(player, player), (float)(damage * falloff));
            applyYinRot(target, skill, target.getMobType() == MobType.UNDEAD ? 0 : 1);
            hitCount++;
        }

        form.spawnCloud(level, center, radius, targets);
        level.playSound(null, center.x, center.y, center.z, form.sound, SoundSource.PLAYERS, 0.74F, form.pitch);
        player.displayClientMessage(Component.translatable(successKey, hitCount), true);
        return true;
    }

    private boolean castSoulChain(ServerPlayer player, CultivationSkill skill) {
        ServerLevel level = player.serverLevel();
        LivingEntity target = findTarget(level, player, range, radius);
        if (target == null) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.spell.target.fail"), true);
            return false;
        }

        target.hurt(player.damageSources().indirectMagic(player, player), (float)calculateDamage(skill.getLevel(), skill.getProficiency()));
        add(target, MobEffects.MOVEMENT_SLOWDOWN, scaled(120, skill, 10), 4);
        add(target, MobEffects.WEAKNESS, scaled(100, skill, 8), 1);
        Vec3 pull = player.position().subtract(target.position());
        if (pull.lengthSqr() > 0.001D) {
            target.push(pull.x * 0.10D, 0.05D, pull.z * 0.10D);
            target.hasImpulse = true;
        }

        form.spawnChain(level, player.getEyePosition().subtract(0.0D, 0.25D, 0.0D),
                target.position().add(0.0D, target.getBbHeight() * 0.55D, 0.0D));
        level.playSound(null, target.blockPosition(), form.sound, SoundSource.PLAYERS, 0.76F, form.pitch);
        player.displayClientMessage(Component.translatable(successKey, target.getDisplayName()), true);
        return true;
    }

    private boolean castUnderworldFlame(ServerPlayer player, CultivationSkill skill) {
        ServerLevel level = player.serverLevel();
        Vec3 direction = player.getLookAngle();
        if (direction.lengthSqr() < 0.001D) {
            return false;
        }

        direction = direction.normalize();
        Vec3 start = player.getEyePosition().add(direction.scale(0.45D));
        Vec3 maxEnd = start.add(direction.scale(range));
        BlockHitResult blockHit = level.clip(new ClipContext(start, maxEnd, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 end = blockHit.getType() == HitResult.Type.MISS ? maxEnd : blockHit.getLocation();
        List<LivingEntity> targets = findBeamTargets(level, player, start, end, Math.max(0.55D, radius));
        if (targets.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.spell.target.fail"), true);
            return false;
        }

        double damage = calculateDamage(skill.getLevel(), skill.getProficiency());
        int hitCount = 0;
        for (LivingEntity target : targets) {
            double along = target.position().subtract(start).dot(direction);
            double falloff = Math.max(0.50D, 1.0D - along / (range * 1.25D));
            target.hurt(player.damageSources().indirectMagic(player, player), (float)(damage * falloff));
            applyYinRot(target, skill, 1);
            add(target, MobEffects.BLINDNESS, scaled(55, skill, 5), 0);
            hitCount++;
        }

        form.spawnBeam(level, start, end);
        level.playSound(null, player.blockPosition(), form.sound, SoundSource.PLAYERS, 0.78F, form.pitch);
        player.displayClientMessage(Component.translatable(successKey, hitCount), true);
        return true;
    }

    private boolean castCorpseArmor(ServerPlayer player, CultivationSkill skill) {
        ServerLevel level = player.serverLevel();
        int duration = scaled(180, skill, 12);
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, duration, 0, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, duration, Math.max(0, skill.getLevel() / 6), false, true));
        player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, Math.min(duration, 120), 0, false, true));
        form.spawnArmor(level, player, radius);
        level.playSound(null, player.blockPosition(), form.sound, SoundSource.PLAYERS, 0.72F, form.pitch);
        player.displayClientMessage(Component.translatable(successKey, duration / 20), true);
        return true;
    }

    private Vec3 findImpactPoint(ServerLevel level, ServerPlayer player, double maxRange) {
        Vec3 start = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        if (look.lengthSqr() < 0.001D) {
            return player.position();
        }
        Vec3 end = start.add(look.normalize().scale(maxRange));
        BlockHitResult hit = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        return hit.getType() == HitResult.Type.MISS ? end : hit.getLocation();
    }

    private LivingEntity findTarget(ServerLevel level, ServerPlayer player, double maxRange, double inflate) {
        Vec3 start = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        if (look.lengthSqr() < 0.001D) {
            return null;
        }
        Vec3 end = start.add(look.normalize().scale(maxRange));
        BlockHitResult blockHit = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 traceEnd = blockHit.getType() == HitResult.Type.MISS ? end : blockHit.getLocation();
        AABB searchBox = new AABB(start, traceEnd).inflate(Math.max(0.8D, inflate));
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(level, player, start, traceEnd, searchBox,
                entity -> canTarget(entity, player));
        if (entityHit == null) {
            return null;
        }
        Entity entity = entityHit.getEntity();
        return entity instanceof LivingEntity living ? living : null;
    }

    private List<LivingEntity> findAreaTargets(ServerLevel level, ServerPlayer player, Vec3 center, double maxRadius) {
        double maxDistance = maxRadius + 0.75D;
        AABB area = new AABB(center, center).inflate(maxDistance, 2.2D, maxDistance);
        return level.getEntitiesOfClass(LivingEntity.class, area,
                        entity -> canTarget(entity, player) && entity.position().distanceToSqr(center) <= maxDistance * maxDistance)
                .stream()
                .sorted(Comparator.comparingDouble(entity -> entity.distanceToSqr(center)))
                .toList();
    }

    private List<LivingEntity> findBeamTargets(ServerLevel level, ServerPlayer player, Vec3 start, Vec3 end, double maxRadius) {
        Vec3 line = end.subtract(start);
        AABB area = new AABB(start, end).inflate(maxRadius);
        return level.getEntitiesOfClass(LivingEntity.class, area,
                        entity -> canTarget(entity, player)
                                && distanceToSegment(entity.position().add(0.0D, entity.getBbHeight() * 0.5D, 0.0D), start, line) <= maxRadius)
                .stream()
                .sorted(Comparator.comparingDouble(entity -> entity.distanceToSqr(player)))
                .toList();
    }

    private boolean canTarget(Entity entity, ServerPlayer player) {
        return canAffect(player, entity);
    }

    private double distanceToSegment(Vec3 point, Vec3 start, Vec3 line) {
        double lengthSqr = line.lengthSqr();
        if (lengthSqr < 0.0001D) {
            return point.distanceTo(start);
        }
        double t = Math.max(0.0D, Math.min(1.0D, point.subtract(start).dot(line) / lengthSqr));
        return point.distanceTo(start.add(line.scale(t)));
    }

    private static void applyYinRot(LivingEntity target, CultivationSkill skill, int amplifierBonus) {
        add(target, MobEffects.WITHER, scaled(80, skill, 7), amplifierBonus);
        add(target, MobEffects.MOVEMENT_SLOWDOWN, scaled(70, skill, 6), 1);
    }

    private static void add(LivingEntity target, MobEffect effect, int durationTicks, int amplifier) {
        target.addEffect(new MobEffectInstance(effect, durationTicks, amplifier, false, true));
    }

    private static int scaled(int baseTicks, CultivationSkill skill, int perLevelTicks) {
        return baseTicks + Math.max(0, skill.getLevel() - 1) * perLevelTicks;
    }

    public enum XuanYinForm {
        SOUL_DEVOURING_CLOUD(new DustParticleOptions(new Vector3f(0.18F, 0.04F, 0.24F), 0.82F),
                new DustParticleOptions(new Vector3f(0.42F, 0.86F, 0.68F), 0.42F),
                SoundEvents.SOUL_ESCAPE, 0.78F),
        YIN_SOUL_CHAIN(new DustParticleOptions(new Vector3f(0.30F, 0.08F, 0.42F), 0.72F),
                new DustParticleOptions(new Vector3f(0.78F, 0.94F, 0.88F), 0.34F),
                SoundEvents.BEACON_POWER_SELECT, 0.58F),
        UNDERWORLD_FLAME(new DustParticleOptions(new Vector3f(0.10F, 0.92F, 0.62F), 0.60F),
                new DustParticleOptions(new Vector3f(0.36F, 0.04F, 0.52F), 0.76F),
                SoundEvents.SOUL_ESCAPE, 1.18F),
        CORPSE_ARMOR(new DustParticleOptions(new Vector3f(0.22F, 0.20F, 0.26F), 0.84F),
                new DustParticleOptions(new Vector3f(0.60F, 0.92F, 0.76F), 0.38F),
                SoundEvents.ANVIL_LAND, 1.62F);

        private final DustParticleOptions core;
        private final DustParticleOptions edge;
        private final SoundEvent sound;
        private final float pitch;

        XuanYinForm(DustParticleOptions core, DustParticleOptions edge, SoundEvent sound, float pitch) {
            this.core = core;
            this.edge = edge;
            this.sound = sound;
            this.pitch = pitch;
        }

        private void spawnCloud(ServerLevel level, Vec3 center, double radius, List<LivingEntity> targets) {
            for (int layer = 0; layer < 3; layer++) {
                ring(level, center.add(0.0D, 0.28D + layer * 0.46D, 0.0D),
                        radius * (1.0D - layer * 0.16D), 70 - layer * 10, 0.16D);
            }
            for (int strand = 0; strand < 7; strand++) {
                double offset = strand * Math.PI * 2.0D / 7.0D;
                for (int i = 0; i < 28; i++) {
                    double t = i / 27.0D;
                    double angle = offset + t * Math.PI * 3.2D;
                    double spread = radius * (0.16D + 0.68D * t);
                    level.sendParticles((i & 1) == 0 ? core : edge,
                            center.x + Math.cos(angle) * spread,
                            center.y + 0.28D + Math.sin(t * Math.PI) * 1.25D,
                            center.z + Math.sin(angle) * spread,
                            1, 0.04D, 0.05D, 0.04D, 0.002D);
                }
            }
            int links = Math.min(5, targets.size());
            for (int i = 0; i < links; i++) {
                LivingEntity target = targets.get(i);
                arc(level, center.add(0.0D, 0.95D, 0.0D),
                        target.position().add(0.0D, target.getBbHeight() * 0.55D, 0.0D), i * 29);
            }
        }

        private void spawnChain(ServerLevel level, Vec3 start, Vec3 end) {
            arc(level, start, end, 13);
            arc(level, start.add(0.0D, -0.10D, 0.0D), end.add(0.0D, 0.12D, 0.0D), 31);
            level.sendParticles(edge, end.x, end.y, end.z, 22, 0.24D, 0.22D, 0.24D, 0.014D);
        }

        private void spawnBeam(ServerLevel level, Vec3 start, Vec3 end) {
            Vec3 line = end.subtract(start);
            Vec3 direction = line.lengthSqr() < 0.0001D ? new Vec3(0.0D, 0.0D, 1.0D) : line.normalize();
            Vec3 side = direction.cross(new Vec3(0.0D, 1.0D, 0.0D));
            if (side.lengthSqr() < 0.0001D) {
                side = new Vec3(1.0D, 0.0D, 0.0D);
            } else {
                side = side.normalize();
            }
            Vec3 up = side.cross(direction).normalize();
            int steps = Math.max(10, (int)(line.length() * 5.0D));
            for (int i = 0; i <= steps; i++) {
                double t = i / (double)steps;
                Vec3 point = start.lerp(end, t);
                double coil = t * Math.PI * 8.0D;
                Vec3 swirl = side.scale(Math.sin(coil) * 0.12D).add(up.scale(Math.cos(coil) * 0.12D));
                level.sendParticles(core, point.x, point.y, point.z, 1, 0.018D, 0.018D, 0.018D, 0.0D);
                if ((i & 1) == 0) {
                    Vec3 edgePoint = point.add(swirl);
                    level.sendParticles(edge, edgePoint.x, edgePoint.y, edgePoint.z, 1, 0.032D, 0.032D, 0.032D, 0.0D);
                }
            }
            level.sendParticles(edge, end.x, end.y, end.z, 28, 0.18D, 0.16D, 0.18D, 0.012D);
        }

        private void spawnArmor(ServerLevel level, ServerPlayer player, double radius) {
            Vec3 base = player.position();
            for (int layer = 0; layer < 4; layer++) {
                ring(level, base.add(0.0D, 0.30D + layer * 0.42D, 0.0D),
                        Math.max(0.72D, radius * 0.34D - layer * 0.05D), 48, 0.04D);
            }
            for (int rib = 0; rib < 10; rib++) {
                double angle = Math.PI * 2.0D * rib / 10.0D;
                double x = base.x + Math.cos(angle) * 0.78D;
                double z = base.z + Math.sin(angle) * 0.78D;
                for (int step = 0; step < 6; step++) {
                    level.sendParticles((step & 1) == 0 ? core : edge,
                            x, base.y + 0.28D + step * 0.26D, z,
                            1, 0.014D, 0.014D, 0.014D, 0.0D);
                }
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

        private void arc(ServerLevel level, Vec3 start, Vec3 end, int seed) {
            Vec3 path = end.subtract(start);
            int steps = Math.max(6, (int)(path.length() * 5.0D));
            for (int i = 0; i <= steps; i++) {
                double t = i / (double)steps;
                Vec3 point = start.lerp(end, t).add(Math.sin(seed + i * 1.19D) * 0.07D,
                        Math.cos(seed * 0.35D + i * 0.9D) * 0.05D,
                        Math.cos(seed + i * 1.31D) * 0.07D);
                level.sendParticles((i & 1) == 0 ? core : edge,
                        point.x, point.y, point.z, 1, 0.014D, 0.014D, 0.014D, 0.0D);
            }
        }
    }
}
