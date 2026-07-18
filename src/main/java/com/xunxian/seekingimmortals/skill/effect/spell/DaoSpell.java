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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class DaoSpell extends SpellEffect {
    private final double range;
    private final double radius;
    private final DaoForm form;
    private final String successKey;

    public DaoSpell(int cost, int cooldownTicks, double damage, double range, double radius,
                    DaoForm form, String successKey) {
        super(cost, cooldownTicks, damage);
        this.range = range;
        this.radius = radius;
        this.form = form;
        this.successKey = successKey;
    }

    @Override
    public boolean execute(ServerPlayer player, PlayerCultivation cultivation, CultivationSkill skill, SkillContext context) {
        return switch (form) {
            case FIVE_THUNDER, BAGUA_SEAL -> castArea(player, skill);
            case PURE_YANG_SWORD -> castBeam(player, skill);
            case TAOIST_SEAL, IMMORTAL_ROPE -> castSingle(player, skill);
            case CLOUD_WALK -> castCloudWalk(player, skill);
            case DAO_NATURE_BREATH -> castNatureBreath(player, cultivation, skill);
        };
    }

    private boolean castArea(ServerPlayer player, CultivationSkill skill) {
        ServerLevel level = player.serverLevel();
        Vec3 center = findLookPoint(level, player, range);
        List<LivingEntity> targets = findAreaTargets(level, player, center, radius);
        if (targets.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.spell.area.fail"), true);
            form.spawnArea(level, center, radius * 0.72D, List.of());
            return false;
        }

        double damage = calculateDamage(skill.getLevel(), skill.getProficiency());
        int hitCount = 0;
        for (LivingEntity target : targets) {
            double falloff = Math.max(0.45D, 1.0D - target.position().distanceTo(center) / (radius + 1.2D));
            if (damage > 0.0D) {
                target.hurt(player.damageSources().indirectMagic(player, player), (float)(damage * falloff));
            }
            form.applyArea(target, center, skill);
            hitCount++;
        }

        form.spawnArea(level, center, radius, targets);
        play(level, form, BlockPos.containing(center));
        player.displayClientMessage(Component.translatable(successKey, hitCount), true);
        return true;
    }

    private boolean castBeam(ServerPlayer player, CultivationSkill skill) {
        ServerLevel level = player.serverLevel();
        Vec3 start = player.getEyePosition();
        Vec3 end = traceEnd(level, player, start, range);
        List<LivingEntity> targets = findLineTargets(level, player, start, end, Math.max(0.58D, radius));
        if (targets.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.spell.target.fail"), true);
            form.spawnBeam(level, start, end);
            return false;
        }

        double damage = calculateDamage(skill.getLevel(), skill.getProficiency());
        int hitCount = 0;
        for (LivingEntity target : targets.stream().limit(4).toList()) {
            double distance = target.getEyePosition().distanceTo(start);
            double falloff = Math.max(0.62D, 1.0D - distance / (range * 1.8D));
            target.hurt(player.damageSources().indirectMagic(player, player), (float)(damage * falloff));
            form.applySingle(target, skill);
            hitCount++;
        }

        form.spawnBeam(level, start, targets.get(0).getEyePosition());
        play(level, form, player.blockPosition());
        player.displayClientMessage(Component.translatable(successKey, hitCount), true);
        return true;
    }

    private boolean castSingle(ServerPlayer player, CultivationSkill skill) {
        ServerLevel level = player.serverLevel();
        LivingEntity target = findTarget(level, player, range, Math.max(0.72D, radius));
        if (target == null) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.spell.target.fail"), true);
            return false;
        }

        double damage = calculateDamage(skill.getLevel(), skill.getProficiency());
        if (damage > 0.0D) {
            target.hurt(player.damageSources().indirectMagic(player, player), (float)damage);
        }
        form.applySingle(target, skill);
        Vec3 targetCenter = target.position().add(0.0D, target.getBbHeight() * 0.58D, 0.0D);
        form.spawnSingle(level, player.getEyePosition(), targetCenter);
        play(level, form, target.blockPosition());
        player.displayClientMessage(Component.translatable(successKey, target.getDisplayName()), true);
        return true;
    }

    private boolean castCloudWalk(ServerPlayer player, CultivationSkill skill) {
        ServerLevel level = player.serverLevel();
        int duration = scaleTicks(150, skill, 10);
        add(player, MobEffects.MOVEMENT_SPEED, duration, 1);
        add(player, MobEffects.JUMP, duration, 0);
        add(player, MobEffects.SLOW_FALLING, Math.max(70, duration / 2), 0);

        Vec3 look = player.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0.0D, look.z);
        if (flat.lengthSqr() > 0.001D) {
            Vec3 impulse = flat.normalize().scale(0.72D).add(0.0D, 0.18D, 0.0D);
            player.push(impulse.x, impulse.y, impulse.z);
            player.hasImpulse = true;
        }

        form.spawnSelf(level, player.position(), player.getBbHeight(), Math.max(1.8D, radius));
        play(level, form, player.blockPosition());
        player.displayClientMessage(Component.translatable(successKey, duration / 20), true);
        return true;
    }

    private boolean castNatureBreath(ServerPlayer player, PlayerCultivation cultivation, CultivationSkill skill) {
        ServerLevel level = player.serverLevel();
        int restore = Math.max(6, (int)Math.round(calculateDamage(skill.getLevel(), skill.getProficiency()) * 0.62D));
        cultivation.addSpiritualPower(restore);
        cultivation.addQiDeviationRisk(-(1 + Math.max(0, skill.getLevel() / 5)));
        player.heal(Math.max(1.0F, restore * 0.12F));
        add(player, MobEffects.REGENERATION, scaleTicks(55, skill, 5), 0);
        add(player, MobEffects.NIGHT_VISION, scaleTicks(95, skill, 8), 0);

        form.spawnSelf(level, player.position(), player.getBbHeight(), Math.max(2.4D, radius));
        play(level, form, player.blockPosition());
        player.displayClientMessage(Component.translatable(successKey, restore), true);
        return true;
    }

    private LivingEntity findTarget(ServerLevel level, ServerPlayer player, double maxRange, double inflate) {
        Vec3 start = player.getEyePosition();
        Vec3 end = traceEnd(level, player, start, maxRange);
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(level, player, start, end,
                new AABB(start, end).inflate(inflate), entity -> canTarget(entity, player));
        if (entityHit == null || !(entityHit.getEntity() instanceof LivingEntity living)) {
            return null;
        }
        return living;
    }

    private Vec3 findLookPoint(ServerLevel level, ServerPlayer player, double maxRange) {
        return traceEnd(level, player, player.getEyePosition(), maxRange);
    }

    private Vec3 traceEnd(ServerLevel level, ServerPlayer player, Vec3 start, double maxRange) {
        Vec3 look = player.getLookAngle();
        if (look.lengthSqr() < 0.001D) {
            return start;
        }
        Vec3 end = start.add(look.normalize().scale(maxRange));
        BlockHitResult blockHit = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        return blockHit.getType() == HitResult.Type.MISS ? end : blockHit.getLocation();
    }

    private List<LivingEntity> findAreaTargets(ServerLevel level, ServerPlayer player, Vec3 center, double maxRadius) {
        AABB area = new AABB(center, center).inflate(maxRadius, Math.max(2.3D, maxRadius * 0.62D), maxRadius);
        return level.getEntitiesOfClass(LivingEntity.class, area,
                        entity -> canTarget(entity, player) && entity.distanceToSqr(center) <= maxRadius * maxRadius)
                .stream()
                .sorted(Comparator.comparingDouble(entity -> entity.distanceToSqr(center)))
                .toList();
    }

    private List<LivingEntity> findLineTargets(ServerLevel level, ServerPlayer player, Vec3 start, Vec3 end, double maxRadius) {
        Vec3 line = end.subtract(start);
        AABB area = new AABB(start, end).inflate(maxRadius, maxRadius, maxRadius);
        return level.getEntitiesOfClass(LivingEntity.class, area,
                        entity -> canTarget(entity, player)
                                && distanceToSegment(entity.getEyePosition(), start, line) <= maxRadius)
                .stream()
                .sorted(Comparator.comparingDouble(entity -> entity.distanceToSqr(start)))
                .toList();
    }

    private static boolean canTarget(Entity entity, ServerPlayer player) {
        return canAffect(player, entity);
    }

    private static double distanceToSegment(Vec3 point, Vec3 start, Vec3 line) {
        double lengthSqr = line.lengthSqr();
        if (lengthSqr < 0.0001D) {
            return point.distanceTo(start);
        }
        double t = Math.max(0.0D, Math.min(1.0D, point.subtract(start).dot(line) / lengthSqr));
        return point.distanceTo(start.add(line.scale(t)));
    }

    private static int scaleTicks(int baseTicks, CultivationSkill skill, int perLevelTicks) {
        return baseTicks + Math.max(0, skill.getLevel() - 1) * perLevelTicks;
    }

    private static void add(LivingEntity target, MobEffect effect, int durationTicks, int amplifier) {
        if (durationTicks > 0) {
            target.addEffect(new MobEffectInstance(effect, durationTicks, amplifier, false, true));
        }
    }

    private static void play(ServerLevel level, DaoForm form, BlockPos pos) {
        level.playSound(null, pos, form.sound, SoundSource.PLAYERS, 0.74F, form.pitch);
    }

    public enum DaoForm {
        FIVE_THUNDER(new DustParticleOptions(new Vector3f(0.36F, 0.74F, 1.00F), 0.78F),
                new DustParticleOptions(new Vector3f(1.00F, 0.92F, 0.38F), 0.44F),
                SoundEvents.BEACON_POWER_SELECT, 1.58F),
        PURE_YANG_SWORD(new DustParticleOptions(new Vector3f(1.00F, 0.78F, 0.24F), 0.62F),
                new DustParticleOptions(new Vector3f(1.00F, 0.98F, 0.72F), 0.34F),
                SoundEvents.TRIDENT_THROW, 1.42F),
        TAOIST_SEAL(new DustParticleOptions(new Vector3f(0.42F, 0.88F, 0.72F), 0.60F),
                new DustParticleOptions(new Vector3f(0.95F, 1.00F, 0.62F), 0.34F),
                SoundEvents.AMETHYST_BLOCK_RESONATE, 1.34F),
        CLOUD_WALK(new DustParticleOptions(new Vector3f(0.72F, 0.92F, 1.00F), 0.50F),
                new DustParticleOptions(new Vector3f(0.96F, 1.00F, 1.00F), 0.28F),
                SoundEvents.AMETHYST_BLOCK_CHIME, 1.70F),
        IMMORTAL_ROPE(new DustParticleOptions(new Vector3f(0.92F, 0.72F, 0.30F), 0.62F),
                new DustParticleOptions(new Vector3f(1.00F, 0.94F, 0.62F), 0.34F),
                SoundEvents.LEASH_KNOT_PLACE, 1.26F),
        BAGUA_SEAL(new DustParticleOptions(new Vector3f(0.28F, 0.92F, 0.82F), 0.64F),
                new DustParticleOptions(new Vector3f(1.00F, 0.84F, 0.32F), 0.38F),
                SoundEvents.RESPAWN_ANCHOR_CHARGE, 1.42F),
        DAO_NATURE_BREATH(new DustParticleOptions(new Vector3f(0.36F, 0.92F, 0.48F), 0.64F),
                new DustParticleOptions(new Vector3f(0.74F, 1.00F, 0.76F), 0.36F),
                SoundEvents.ENCHANTMENT_TABLE_USE, 1.48F);

        private final DustParticleOptions core;
        private final DustParticleOptions edge;
        private final SoundEvent sound;
        private final float pitch;

        DaoForm(DustParticleOptions core, DustParticleOptions edge, SoundEvent sound, float pitch) {
            this.core = core;
            this.edge = edge;
            this.sound = sound;
            this.pitch = pitch;
        }

        private void applySingle(LivingEntity target, CultivationSkill skill) {
            int bonus = Math.max(0, skill.getLevel() - 1);
            switch (this) {
                case PURE_YANG_SWORD -> {
                    add(target, MobEffects.GLOWING, 80 + bonus * 5, 0);
                    add(target, MobEffects.WEAKNESS, 70 + bonus * 4, 0);
                    target.setSecondsOnFire(2);
                }
                case TAOIST_SEAL -> {
                    add(target, MobEffects.GLOWING, 110 + bonus * 7, 0);
                    add(target, MobEffects.MOVEMENT_SLOWDOWN, 90 + bonus * 5, 1);
                    add(target, MobEffects.WEAKNESS, 90 + bonus * 5, 0);
                }
                case IMMORTAL_ROPE -> {
                    add(target, MobEffects.MOVEMENT_SLOWDOWN, 125 + bonus * 8, 4);
                    add(target, MobEffects.DIG_SLOWDOWN, 105 + bonus * 7, 1);
                    add(target, MobEffects.WEAKNESS, 80 + bonus * 5, 0);
                    target.setDeltaMovement(target.getDeltaMovement().multiply(0.28D, 0.15D, 0.28D));
                    target.hasImpulse = true;
                }
                default -> {
                }
            }
        }

        private void applyArea(LivingEntity target, Vec3 center, CultivationSkill skill) {
            int bonus = Math.max(0, skill.getLevel() - 1);
            if (this == FIVE_THUNDER) {
                add(target, MobEffects.MOVEMENT_SLOWDOWN, 85 + bonus * 6, 1);
                add(target, MobEffects.WEAKNESS, 75 + bonus * 5, 0);
                add(target, MobEffects.GLOWING, 55 + bonus * 4, 0);
                target.setSecondsOnFire(1);
            } else if (this == BAGUA_SEAL) {
                add(target, MobEffects.MOVEMENT_SLOWDOWN, 105 + bonus * 7, 2);
                add(target, MobEffects.WEAKNESS, 95 + bonus * 6, 1);
                add(target, MobEffects.CONFUSION, 70 + bonus * 5, 0);
                Vec3 pull = center.subtract(target.position());
                if (pull.lengthSqr() > 0.001D) {
                    target.push(pull.normalize().x * 0.10D, 0.05D, pull.normalize().z * 0.10D);
                    target.hasImpulse = true;
                }
            }
        }

        private void spawnArea(ServerLevel level, Vec3 center, double radius, List<LivingEntity> targets) {
            if (this == FIVE_THUNDER) {
                spawnThunderArray(level, center, radius, targets);
            } else if (this == BAGUA_SEAL) {
                spawnBagua(level, center, radius, targets);
            }
        }

        private void spawnBeam(ServerLevel level, Vec3 start, Vec3 end) {
            Vec3 line = end.subtract(start);
            int steps = Math.max(10, (int)(line.length() * 6.0D));
            Vec3 side = side(line);
            Vec3 up = side.cross(line.lengthSqr() < 0.0001D ? new Vec3(0.0D, 0.0D, 1.0D) : line.normalize()).normalize();
            for (int i = 0; i <= steps; i++) {
                double t = i / (double)steps;
                Vec3 point = start.lerp(end, t);
                double blade = Math.sin(t * Math.PI);
                level.sendParticles(core, point.x, point.y, point.z, 1, 0.012D, 0.012D, 0.012D, 0.0D);
                if ((i & 1) == 0) {
                    Vec3 edgePoint = point.add(side.scale(blade * 0.12D)).add(up.scale(Math.cos(t * Math.PI * 2.0D) * 0.05D));
                    level.sendParticles(edge, edgePoint.x, edgePoint.y, edgePoint.z, 1, 0.014D, 0.014D, 0.014D, 0.0D);
                }
            }
            level.sendParticles(edge, end.x, end.y, end.z, 18, 0.18D, 0.12D, 0.18D, 0.006D);
        }

        private void spawnSingle(ServerLevel level, Vec3 start, Vec3 end) {
            if (this == IMMORTAL_ROPE) {
                arc(level, start, end, 9);
                for (int layer = 0; layer < 4; layer++) {
                    ring(level, end.add(0.0D, -0.34D + layer * 0.28D, 0.0D), 0.56D + layer * 0.08D, 46, 0.035D);
                }
            } else {
                arc(level, start, end, 3);
                talismanSeal(level, end, 0.92D);
            }
        }

        private void spawnSelf(ServerLevel level, Vec3 base, double height, double radius) {
            if (this == CLOUD_WALK) {
                for (int layer = 0; layer < 4; layer++) {
                    ring(level, base.add(0.0D, 0.12D + layer * 0.18D, 0.0D), radius * (0.32D + layer * 0.12D), 48, 0.04D);
                }
                level.sendParticles(core, base.x, base.y + 0.18D, base.z, 36, radius * 0.34D, 0.12D, radius * 0.34D, 0.018D);
            } else {
                for (int i = 0; i < 72; i++) {
                    double t = i / 71.0D;
                    double angle = t * Math.PI * 8.0D;
                    double swirl = radius * (0.18D + Math.sin(t * Math.PI) * 0.26D);
                    level.sendParticles((i & 1) == 0 ? core : edge,
                            base.x + Math.cos(angle) * swirl,
                            base.y + 0.16D + t * Math.max(1.2D, height),
                            base.z + Math.sin(angle) * swirl,
                            1, 0.014D, 0.014D, 0.014D, 0.0D);
                }
                ring(level, base.add(0.0D, 0.22D, 0.0D), radius * 0.62D, 64, 0.04D);
            }
        }

        private void spawnThunderArray(ServerLevel level, Vec3 center, double radius, List<LivingEntity> targets) {
            ring(level, center.add(0.0D, 0.16D, 0.0D), radius, 84, 0.08D);
            for (int bolt = 0; bolt < 5; bolt++) {
                double angle = Math.PI * 2.0D * bolt / 5.0D + 0.22D;
                Vec3 top = center.add(Math.cos(angle) * radius * 0.42D, 3.2D, Math.sin(angle) * radius * 0.42D);
                Vec3 bottom = center.add(Math.cos(angle + 0.4D) * radius * 0.58D, 0.22D, Math.sin(angle + 0.4D) * radius * 0.58D);
                lightningArc(level, top, bottom, bolt * 17);
            }
            int links = Math.min(5, targets.size());
            for (int i = 0; i < links; i++) {
                LivingEntity target = targets.get(i);
                lightningArc(level, center.add(0.0D, 2.2D, 0.0D),
                        target.position().add(0.0D, target.getBbHeight() * 0.62D, 0.0D), i * 23);
            }
            level.sendParticles(edge, center.x, center.y + 0.32D, center.z, 34, radius * 0.25D, 0.16D, radius * 0.25D, 0.02D);
        }

        private void spawnBagua(ServerLevel level, Vec3 center, double radius, List<LivingEntity> targets) {
            for (int layer = 0; layer < 3; layer++) {
                octagon(level, center.add(0.0D, 0.18D + layer * 0.20D, 0.0D), radius * (1.0D - layer * 0.16D));
            }
            for (int trigram = 0; trigram < 8; trigram++) {
                double angle = Math.PI * 2.0D * trigram / 8.0D;
                trigram(level, center, radius * 0.72D, angle, trigram);
            }
            int links = Math.min(6, targets.size());
            for (int i = 0; i < links; i++) {
                LivingEntity target = targets.get(i);
                arc(level, center.add(0.0D, 0.92D, 0.0D),
                        target.position().add(0.0D, target.getBbHeight() * 0.55D, 0.0D), i * 31);
            }
        }

        private void talismanSeal(ServerLevel level, Vec3 center, double radius) {
            octagon(level, center, radius);
            for (int stroke = 0; stroke < 4; stroke++) {
                double angle = Math.PI * 0.25D + stroke * Math.PI * 0.5D;
                Vec3 a = center.add(Math.cos(angle) * radius * 0.72D, 0.0D, Math.sin(angle) * radius * 0.72D);
                Vec3 b = center.add(-Math.cos(angle) * radius * 0.72D, 0.0D, -Math.sin(angle) * radius * 0.72D);
                arc(level, a, b, stroke * 11);
            }
        }

        private void ring(ServerLevel level, Vec3 center, double radius, int points, double wave) {
            for (int i = 0; i < points; i++) {
                double angle = Math.PI * 2.0D * i / points;
                level.sendParticles((i & 1) == 0 ? core : edge,
                        center.x + Math.cos(angle) * radius,
                        center.y + Math.sin(angle * 3.0D) * wave,
                        center.z + Math.sin(angle) * radius,
                        1, 0.016D, 0.016D, 0.016D, 0.0D);
            }
        }

        private void octagon(ServerLevel level, Vec3 center, double radius) {
            for (int sideIndex = 0; sideIndex < 8; sideIndex++) {
                double a0 = Math.PI * 2.0D * sideIndex / 8.0D + Math.PI / 8.0D;
                double a1 = Math.PI * 2.0D * (sideIndex + 1) / 8.0D + Math.PI / 8.0D;
                Vec3 start = center.add(Math.cos(a0) * radius, 0.0D, Math.sin(a0) * radius);
                Vec3 end = center.add(Math.cos(a1) * radius, 0.0D, Math.sin(a1) * radius);
                arc(level, start, end, sideIndex * 7);
            }
        }

        private void trigram(ServerLevel level, Vec3 center, double radius, double angle, int seed) {
            Vec3 outward = new Vec3(Math.cos(angle), 0.0D, Math.sin(angle));
            Vec3 side = new Vec3(-outward.z, 0.0D, outward.x);
            Vec3 base = center.add(outward.scale(radius)).add(0.0D, 0.36D, 0.0D);
            for (int line = 0; line < 3; line++) {
                double offset = (line - 1) * 0.16D;
                Vec3 a = base.add(side.scale(-0.34D)).add(0.0D, offset, 0.0D);
                Vec3 b = base.add(side.scale(0.34D)).add(0.0D, offset, 0.0D);
                if (((seed + line) & 1) == 0) {
                    arc(level, a, b, seed * 13 + line);
                } else {
                    Vec3 mid = a.lerp(b, 0.5D);
                    arc(level, a, mid.add(side.scale(-0.06D)), seed * 13 + line);
                    arc(level, mid.add(side.scale(0.06D)), b, seed * 17 + line);
                }
            }
        }

        private void arc(ServerLevel level, Vec3 start, Vec3 end, int seed) {
            Vec3 path = end.subtract(start);
            int steps = Math.max(6, (int)(path.length() * 5.0D));
            for (int i = 0; i <= steps; i++) {
                double t = i / (double)steps;
                Vec3 point = start.lerp(end, t).add(Math.sin(seed + i * 1.23D) * 0.035D,
                        Math.cos(seed * 0.31D + i * 0.73D) * 0.025D,
                        Math.cos(seed + i * 1.11D) * 0.035D);
                level.sendParticles((i & 1) == 0 ? core : edge,
                        point.x, point.y, point.z, 1, 0.012D, 0.012D, 0.012D, 0.0D);
            }
        }

        private void lightningArc(ServerLevel level, Vec3 start, Vec3 end, int seed) {
            Vec3 path = end.subtract(start);
            int steps = Math.max(8, (int)(path.length() * 6.0D));
            for (int i = 0; i <= steps; i++) {
                double t = i / (double)steps;
                Vec3 point = start.lerp(end, t).add(Math.sin(seed + i * 2.31D) * 0.13D,
                        Math.cos(seed * 0.43D + i * 1.17D) * 0.08D,
                        Math.cos(seed + i * 1.91D) * 0.13D);
                level.sendParticles((i & 1) == 0 ? core : edge,
                        point.x, point.y, point.z, 1, 0.018D, 0.018D, 0.018D, 0.0D);
            }
        }

        private Vec3 side(Vec3 line) {
            Vec3 direction = line.lengthSqr() < 0.0001D ? new Vec3(0.0D, 0.0D, 1.0D) : line.normalize();
            Vec3 side = direction.cross(new Vec3(0.0D, 1.0D, 0.0D));
            return side.lengthSqr() < 0.0001D ? new Vec3(1.0D, 0.0D, 0.0D) : side.normalize();
        }
    }
}
