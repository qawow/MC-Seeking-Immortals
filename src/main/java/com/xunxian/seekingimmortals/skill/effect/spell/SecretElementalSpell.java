package com.xunxian.seekingimmortals.skill.effect.spell;

import com.xunxian.seekingimmortals.cultivation.PlayerCultivation;
import com.xunxian.seekingimmortals.skill.CultivationSkill;
import com.xunxian.seekingimmortals.skill.effect.SkillContext;
import java.util.Comparator;
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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class SecretElementalSpell extends SpellEffect {
    private final double range;
    private final double radius;
    private final SecretElement element;
    private final String successKey;

    public SecretElementalSpell(int cost, int cooldownTicks, double damage, double range, double radius,
                                SecretElement element, String successKey) {
        super(cost, cooldownTicks, damage);
        this.range = range;
        this.radius = radius;
        this.element = element;
        this.successKey = successKey;
    }

    @Override
    public boolean execute(ServerPlayer player, PlayerCultivation cultivation, CultivationSkill skill, SkillContext context) {
        return element.beam ? executeBeam(player, skill) : executeArea(player, skill);
    }

    private boolean executeBeam(ServerPlayer player, CultivationSkill skill) {
        ServerLevel level = player.serverLevel();
        Vec3 direction = player.getLookAngle();
        if (direction.lengthSqr() < 0.001D) {
            return false;
        }

        direction = direction.normalize();
        Vec3 start = player.getEyePosition().add(direction.scale(0.55D));
        Vec3 maxEnd = start.add(direction.scale(range));
        BlockHitResult blockHit = level.clip(new ClipContext(start, maxEnd, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 end = blockHit.getType() == HitResult.Type.MISS ? maxEnd : blockHit.getLocation();
        List<LivingEntity> targets = findBeamTargets(level, player, start, end);
        if (targets.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.spell.target.fail"), true);
            return false;
        }

        double damage = calculateDamage(skill.getLevel(), skill.getProficiency());
        int hitCount = 0;
        for (LivingEntity target : targets) {
            double distanceAlong = target.position().subtract(start).dot(direction);
            double falloff = Math.max(0.55D, 1.0D - distanceAlong / (range * 1.35D));
            target.hurt(player.damageSources().indirectMagic(player, player), (float)(damage * falloff * element.damageMultiplier(target)));
            element.applyEffects(target, skill);
            hitCount++;
        }

        element.spawnBeam(level, start, end);
        level.playSound(null, BlockPos.containing(start), element.sound, SoundSource.PLAYERS, 0.78F, element.pitch);
        level.playSound(null, BlockPos.containing(end), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.46F, 1.64F);
        player.displayClientMessage(Component.translatable(successKey, hitCount), true);
        return true;
    }

    private boolean executeArea(ServerPlayer player, CultivationSkill skill) {
        ServerLevel level = player.serverLevel();
        Vec3 center = findImpactPoint(level, player);
        List<LivingEntity> targets = findAreaTargets(level, player, center);
        if (targets.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.spell.area.fail"), true);
            return false;
        }

        double damage = calculateDamage(skill.getLevel(), skill.getProficiency());
        int hitCount = 0;
        for (LivingEntity target : targets) {
            double distance = target.position().distanceTo(center);
            double falloff = Math.max(0.48D, 1.0D - distance / (radius + 0.85D));
            target.hurt(player.damageSources().indirectMagic(player, player), (float)(damage * falloff * element.damageMultiplier(target)));
            element.applyEffects(target, skill);
            hitCount++;
        }

        element.spawnArea(level, center, radius, targets);
        level.playSound(null, center.x, center.y, center.z, element.sound, SoundSource.PLAYERS, 0.86F, element.pitch);
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

    private List<LivingEntity> findAreaTargets(ServerLevel level, ServerPlayer player, Vec3 center) {
        double maxDistance = radius + 0.8D;
        AABB area = new AABB(center, center).inflate(maxDistance, 2.4D, maxDistance);
        return level.getEntitiesOfClass(LivingEntity.class, area,
                        entity -> canAffect(player, entity)
                                && entity.position().distanceToSqr(center) <= maxDistance * maxDistance)
                .stream()
                .sorted(Comparator.comparingDouble(entity -> entity.distanceToSqr(center)))
                .toList();
    }

    private List<LivingEntity> findBeamTargets(ServerLevel level, ServerPlayer player, Vec3 start, Vec3 end) {
        AABB box = new AABB(start, end).inflate(radius);
        Vec3 line = end.subtract(start);
        return level.getEntitiesOfClass(LivingEntity.class, box,
                        entity -> canAffect(player, entity)
                                && distanceToSegment(entity.position().add(0.0D, entity.getBbHeight() * 0.5D, 0.0D), start, line) <= radius)
                .stream()
                .sorted(Comparator.comparingDouble(entity -> entity.distanceToSqr(player)))
                .toList();
    }

    private double distanceToSegment(Vec3 point, Vec3 start, Vec3 line) {
        double lengthSqr = line.lengthSqr();
        if (lengthSqr < 0.0001D) {
            return point.distanceTo(start);
        }
        double t = Math.max(0.0D, Math.min(1.0D, point.subtract(start).dot(line) / lengthSqr));
        return point.distanceTo(start.add(line.scale(t)));
    }

    public enum SecretElement {
        LIFE_FIRE(true,
                new DustParticleOptions(new Vector3f(1.00F, 0.68F, 0.16F), 0.72F),
                new DustParticleOptions(new Vector3f(1.00F, 0.16F, 0.04F), 0.52F),
                SoundEvents.BLAZE_SHOOT, 0.74F),
        TRUE_FIRE_HEAVEN(false,
                new DustParticleOptions(new Vector3f(1.00F, 0.20F, 0.04F), 0.86F),
                new DustParticleOptions(new Vector3f(0.72F, 0.08F, 1.00F), 0.46F),
                SoundEvents.BLAZE_SHOOT, 0.62F),
        FIVE_ELEMENT_FUSION(false,
                new DustParticleOptions(new Vector3f(0.98F, 0.88F, 0.32F), 0.72F),
                new DustParticleOptions(new Vector3f(0.32F, 0.84F, 1.00F), 0.48F),
                SoundEvents.BEACON_POWER_SELECT, 1.22F);

        private static final DustParticleOptions WOOD = new DustParticleOptions(new Vector3f(0.32F, 1.00F, 0.42F), 0.52F);
        private static final DustParticleOptions FIRE = new DustParticleOptions(new Vector3f(1.00F, 0.24F, 0.06F), 0.58F);
        private static final DustParticleOptions EARTH = new DustParticleOptions(new Vector3f(0.76F, 0.54F, 0.22F), 0.56F);
        private static final DustParticleOptions METAL = new DustParticleOptions(new Vector3f(0.96F, 0.92F, 0.72F), 0.46F);

        private final boolean beam;
        private final DustParticleOptions core;
        private final DustParticleOptions edge;
        private final SoundEvent sound;
        private final float pitch;

        SecretElement(boolean beam, DustParticleOptions core, DustParticleOptions edge, SoundEvent sound, float pitch) {
            this.beam = beam;
            this.core = core;
            this.edge = edge;
            this.sound = sound;
            this.pitch = pitch;
        }

        private double damageMultiplier(LivingEntity target) {
            if ((this == LIFE_FIRE || this == TRUE_FIRE_HEAVEN) && target.getMobType() == MobType.UNDEAD) {
                return 1.22D;
            }
            return 1.0D;
        }

        private void applyEffects(LivingEntity target, CultivationSkill skill) {
            int levelBonus = Math.max(0, skill.getLevel() - 1);
            switch (this) {
                case LIFE_FIRE -> {
                    target.setSecondsOnFire(8);
                    add(target, MobEffects.WEAKNESS, 105 + levelBonus * 8, 1);
                    if (target.getMobType() == MobType.UNDEAD) {
                        add(target, MobEffects.GLOWING, 90 + levelBonus * 6, 0);
                    }
                }
                case TRUE_FIRE_HEAVEN -> {
                    target.setSecondsOnFire(9);
                    add(target, MobEffects.WEAKNESS, 120 + levelBonus * 9, 1);
                    add(target, MobEffects.MOVEMENT_SLOWDOWN, 80 + levelBonus * 6, 1);
                }
                case FIVE_ELEMENT_FUSION -> {
                    add(target, MobEffects.MOVEMENT_SLOWDOWN, 105 + levelBonus * 8, 2);
                    add(target, MobEffects.WEAKNESS, 95 + levelBonus * 7, 1);
                    target.setDeltaMovement(target.getDeltaMovement().multiply(0.58D, 0.45D, 0.58D));
                    target.hasImpulse = true;
                }
            }
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
            int steps = Math.max(12, (int)(line.length() * 6.0D));
            for (int i = 0; i <= steps; i++) {
                double t = i / (double)steps;
                Vec3 point = start.lerp(end, t);
                double coil = t * Math.PI * 11.0D;
                Vec3 swirl = side.scale(Math.sin(coil) * 0.14D).add(up.scale(Math.cos(coil) * 0.14D));
                level.sendParticles(core, point.x, point.y, point.z, 1, 0.018D, 0.018D, 0.018D, 0.0D);
                if ((i & 1) == 0) {
                    Vec3 edgePoint = point.add(swirl);
                    level.sendParticles(edge, edgePoint.x, edgePoint.y, edgePoint.z, 1, 0.045D, 0.035D, 0.045D, 0.0D);
                }
            }
            level.sendParticles(edge, end.x, end.y, end.z, 28, 0.22D, 0.16D, 0.22D, 0.018D);
        }

        private void spawnArea(ServerLevel level, Vec3 center, double radius, List<LivingEntity> targets) {
            switch (this) {
                case TRUE_FIRE_HEAVEN -> spawnTrueFire(level, center, radius);
                case FIVE_ELEMENT_FUSION -> spawnFiveElementBurst(level, center, radius, targets);
                case LIFE_FIRE -> spawnBeam(level, center, center.add(0.0D, 2.0D, 0.0D));
            }
        }

        private void spawnTrueFire(ServerLevel level, Vec3 center, double radius) {
            ring(level, center.add(0.0D, 0.16D, 0.0D), radius, 96, 0.16D, core, edge);
            ring(level, center.add(0.0D, 1.18D, 0.0D), radius * 0.66D, 72, 0.10D, edge, core);
            for (int strand = 0; strand < 6; strand++) {
                double offset = strand * Math.PI / 3.0D;
                for (int i = 0; i < 34; i++) {
                    double t = i / 33.0D;
                    double angle = offset + t * Math.PI * 3.4D;
                    double spread = radius * (0.18D + 0.54D * t);
                    double y = center.y + 0.25D + Math.sin(t * Math.PI) * 1.75D;
                    level.sendParticles((i & 1) == 0 ? core : edge,
                            center.x + Math.cos(angle) * spread, y,
                            center.z + Math.sin(angle) * spread, 1, 0.052D, 0.046D, 0.052D, 0.004D);
                }
            }
        }

        private void spawnFiveElementBurst(ServerLevel level, Vec3 center, double radius, List<LivingEntity> targets) {
            DustParticleOptions[] cycle = { METAL, WOOD, edge, FIRE, EARTH };
            for (int ring = 0; ring < 3; ring++) {
                ring(level, center.add(0.0D, 0.18D + ring * 0.46D, 0.0D), radius * (1.0D - ring * 0.18D),
                        90 - ring * 14, 0.10D, cycle[ring], cycle[(ring + 2) % cycle.length]);
            }
            for (int spoke = 0; spoke < 5; spoke++) {
                double angle = spoke * Math.PI * 2.0D / 5.0D;
                DustParticleOptions particle = cycle[spoke];
                for (int i = 0; i < 26; i++) {
                    double t = i / 25.0D;
                    double wave = Math.sin(t * Math.PI * 2.0D + spoke) * 0.16D;
                    double x = center.x + Math.cos(angle) * radius * t + Math.cos(angle + Math.PI * 0.5D) * wave;
                    double z = center.z + Math.sin(angle) * radius * t + Math.sin(angle + Math.PI * 0.5D) * wave;
                    level.sendParticles(particle, x, center.y + 0.32D + t * 0.78D, z, 1, 0.024D, 0.024D, 0.024D, 0.0D);
                }
            }
            int links = Math.min(5, targets.size());
            for (int i = 0; i < links; i++) {
                LivingEntity target = targets.get(i);
                arc(level, center.add(0.0D, 1.0D, 0.0D),
                        target.position().add(0.0D, target.getBbHeight() * 0.55D, 0.0D),
                        i * 23, cycle[i % cycle.length]);
            }
            level.sendParticles(core, center.x, center.y + 0.9D, center.z, 38, radius * 0.24D, 0.28D, radius * 0.24D, 0.018D);
        }

        private void ring(ServerLevel level, Vec3 center, double radius, int points, double wave,
                          DustParticleOptions first, DustParticleOptions second) {
            for (int i = 0; i < points; i++) {
                double angle = Math.PI * 2.0D * i / points;
                DustParticleOptions particle = (i & 1) == 0 ? first : second;
                level.sendParticles(particle, center.x + Math.cos(angle) * radius,
                        center.y + Math.sin(angle * 3.0D) * wave,
                        center.z + Math.sin(angle) * radius, 1, 0.026D, 0.026D, 0.026D, 0.0D);
            }
        }

        private void arc(ServerLevel level, Vec3 start, Vec3 end, int seed, DustParticleOptions particle) {
            Vec3 path = end.subtract(start);
            int steps = Math.max(7, (int)(path.length() * 5.0D));
            for (int i = 0; i <= steps; i++) {
                double t = i / (double)steps;
                Vec3 point = start.lerp(end, t).add(Math.sin(seed + i * 1.27D) * 0.08D,
                        Math.cos(seed * 0.3D + i) * 0.06D,
                        Math.cos(seed + i * 1.11D) * 0.08D);
                level.sendParticles(particle, point.x, point.y, point.z, 1, 0.018D, 0.018D, 0.018D, 0.0D);
            }
        }

        private static void add(LivingEntity target, MobEffect effect, int durationTicks, int amplifier) {
            target.addEffect(new MobEffectInstance(effect, durationTicks, amplifier, false, true));
        }
    }
}
