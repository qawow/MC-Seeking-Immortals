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
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class ConfucianSpell extends SpellEffect {
    private final double range;
    private final double radius;
    private final ConfucianForm form;
    private final String successKey;

    public ConfucianSpell(int cost, int cooldownTicks, double damage, double range, double radius,
                          ConfucianForm form, String successKey) {
        super(cost, cooldownTicks, damage);
        this.range = range;
        this.radius = radius;
        this.form = form;
        this.successKey = successKey;
    }

    @Override
    public boolean execute(ServerPlayer player, PlayerCultivation cultivation, CultivationSkill skill, SkillContext context) {
        return switch (form) {
            case RIGHTEOUS_QI -> castRighteousAura(player, skill);
            case WORD_SUPPRESS -> castSingle(player, skill);
            case SCROLL_STRIKE, CONFUCIAN_RIGHTEOUS_QI -> castLine(player, skill);
            case INK_SEA -> castArea(player, skill);
        };
    }

    private boolean castRighteousAura(ServerPlayer player, CultivationSkill skill) {
        ServerLevel level = player.serverLevel();
        int duration = scaledTicks(150, skill, 10);
        add(player, MobEffects.DAMAGE_RESISTANCE, duration, 0);
        add(player, MobEffects.DAMAGE_BOOST, duration, 0);
        add(player, MobEffects.ABSORPTION, Math.max(100, duration), Math.max(0, skill.getLevel() / 6));
        player.removeEffect(MobEffects.WEAKNESS);
        player.removeEffect(MobEffects.CONFUSION);
        player.removeEffect(MobEffects.BLINDNESS);

        int purified = suppressNearHostiles(player, level, radius, skill);
        int deflected = deflectProjectiles(player, level, Math.max(2.2D, radius * 0.62D));
        form.spawnSelf(level, player.position(), player.getBbHeight(), radius);
        play(level, form, player.blockPosition());
        player.displayClientMessage(Component.translatable(successKey, duration / 20, purified + deflected), true);
        return true;
    }

    private boolean castSingle(ServerPlayer player, CultivationSkill skill) {
        ServerLevel level = player.serverLevel();
        LivingEntity target = findTarget(level, player, range, Math.max(0.75D, radius));
        if (target == null) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.spell.target.fail"), true);
            return false;
        }

        double damage = calculateDamage(skill.getLevel(), skill.getProficiency());
        target.hurt(player.damageSources().indirectMagic(player, player), (float)(damage * form.damageMultiplier(target)));
        form.applyTarget(player, target, target.position(), skill);
        form.spawnSingle(level, player.getEyePosition(), target.position().add(0.0D, target.getBbHeight() * 0.58D, 0.0D));
        play(level, form, target.blockPosition());
        player.displayClientMessage(Component.translatable(successKey, target.getDisplayName()), true);
        return true;
    }

    private boolean castLine(ServerPlayer player, CultivationSkill skill) {
        ServerLevel level = player.serverLevel();
        Vec3 start = player.getEyePosition().add(player.getLookAngle().normalize().scale(0.35D));
        Vec3 end = traceEnd(level, player, start, range);
        List<LivingEntity> targets = findLineTargets(level, player, start, end, Math.max(0.72D, radius));
        if (targets.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.spell.target.fail"), true);
            form.spawnLine(level, start, end, Math.max(0.72D, radius));
            return false;
        }

        double damage = calculateDamage(skill.getLevel(), skill.getProficiency());
        int hitCount = 0;
        for (LivingEntity target : targets.stream().limit(form == ConfucianForm.SCROLL_STRIKE ? 4 : 5).toList()) {
            double distance = target.getEyePosition().distanceTo(start);
            double falloff = Math.max(0.58D, 1.0D - distance / (range * 1.65D));
            target.hurt(player.damageSources().indirectMagic(player, player), (float)(damage * falloff * form.damageMultiplier(target)));
            form.applyTarget(player, target, player.position(), skill);
            hitCount++;
        }

        form.spawnLine(level, start, targets.get(0).getEyePosition(), Math.max(0.72D, radius));
        play(level, form, player.blockPosition());
        player.displayClientMessage(Component.translatable(successKey, hitCount), true);
        return true;
    }

    private boolean castArea(ServerPlayer player, CultivationSkill skill) {
        ServerLevel level = player.serverLevel();
        Vec3 center = traceEnd(level, player, player.getEyePosition(), range);
        List<LivingEntity> targets = findAreaTargets(level, player, center, radius);
        if (targets.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.spell.area.fail"), true);
            form.spawnArea(level, center, radius * 0.72D, List.of());
            return false;
        }

        double damage = calculateDamage(skill.getLevel(), skill.getProficiency());
        int hitCount = 0;
        for (LivingEntity target : targets.stream().limit(9).toList()) {
            double falloff = Math.max(0.44D, 1.0D - target.position().distanceTo(center) / (radius + 1.0D));
            target.hurt(player.damageSources().indirectMagic(player, player), (float)(damage * falloff));
            form.applyTarget(player, target, center, skill);
            hitCount++;
        }

        form.spawnArea(level, center, radius, targets);
        play(level, form, BlockPos.containing(center));
        player.displayClientMessage(Component.translatable(successKey, hitCount), true);
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
        AABB area = new AABB(center, center).inflate(maxRadius, Math.max(2.6D, maxRadius * 0.60D), maxRadius);
        return level.getEntitiesOfClass(LivingEntity.class, area,
                        entity -> canTarget(entity, player) && entity.position().distanceToSqr(center) <= maxRadius * maxRadius)
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

    private static boolean disorderLike(LivingEntity target) {
        return target.getMobType() == MobType.UNDEAD || target instanceof Enemy;
    }

    private static int suppressNearHostiles(ServerPlayer player, ServerLevel level, double radius, CultivationSkill skill) {
        AABB area = player.getBoundingBox().inflate(radius, 2.2D, radius);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, area,
                target -> canTarget(target, player) && disorderLike(target) && target.distanceToSqr(player) <= radius * radius);
        int changed = 0;
        for (LivingEntity target : targets.stream().limit(7).toList()) {
            add(target, MobEffects.GLOWING, scaledTicks(90, skill, 5), 0);
            add(target, MobEffects.WEAKNESS, scaledTicks(90, skill, 6), 1);
            add(target, MobEffects.MOVEMENT_SLOWDOWN, scaledTicks(75, skill, 5), 1);
            Vec3 away = target.position().subtract(player.position());
            if (away.lengthSqr() > 0.001D) {
                Vec3 n = away.normalize();
                target.push(n.x * 0.16D, 0.07D, n.z * 0.16D);
                target.hasImpulse = true;
            }
            changed++;
        }
        return changed;
    }

    private static int deflectProjectiles(ServerPlayer player, ServerLevel level, double radius) {
        AABB area = player.getBoundingBox().inflate(radius, radius * 0.65D, radius);
        List<Projectile> projectiles = level.getEntitiesOfClass(Projectile.class, area,
                projectile -> projectile.isAlive() && projectile.getOwner() != player);
        int changed = 0;
        for (Projectile projectile : projectiles) {
            Vec3 direction = projectile.position().subtract(player.position());
            if (direction.lengthSqr() < 0.001D) {
                direction = player.getLookAngle();
            }
            double speed = Math.max(0.35D, projectile.getDeltaMovement().length() + 0.08D);
            projectile.setDeltaMovement(direction.normalize().scale(speed).add(0.0D, 0.04D, 0.0D));
            projectile.hasImpulse = true;
            changed++;
        }
        return changed;
    }

    private static int scaledTicks(int baseTicks, CultivationSkill skill, int perLevelTicks) {
        return baseTicks + Math.max(0, skill.getLevel() - 1) * perLevelTicks;
    }

    private static void add(LivingEntity target, MobEffect effect, int durationTicks, int amplifier) {
        if (durationTicks > 0) {
            target.addEffect(new MobEffectInstance(effect, durationTicks, amplifier, false, true));
        }
    }

    private static void play(ServerLevel level, ConfucianForm form, BlockPos pos) {
        level.playSound(null, pos, form.sound, SoundSource.PLAYERS, 0.76F, form.pitch);
    }

    public enum ConfucianForm {
        RIGHTEOUS_QI(new DustParticleOptions(new Vector3f(1.00F, 0.88F, 0.34F), 0.62F),
                new DustParticleOptions(new Vector3f(1.00F, 1.00F, 0.86F), 0.32F),
                SoundEvents.BEACON_POWER_SELECT, 1.28F),
        WORD_SUPPRESS(new DustParticleOptions(new Vector3f(1.00F, 0.92F, 0.48F), 0.58F),
                new DustParticleOptions(new Vector3f(0.72F, 0.90F, 1.00F), 0.30F),
                SoundEvents.AMETHYST_BLOCK_RESONATE, 1.12F),
        SCROLL_STRIKE(new DustParticleOptions(new Vector3f(0.92F, 0.78F, 0.48F), 0.60F),
                new DustParticleOptions(new Vector3f(0.98F, 0.98F, 0.86F), 0.30F),
                SoundEvents.BOOK_PAGE_TURN, 1.16F),
        INK_SEA(new DustParticleOptions(new Vector3f(0.05F, 0.06F, 0.10F), 0.70F),
                new DustParticleOptions(new Vector3f(0.20F, 0.34F, 0.48F), 0.36F),
                SoundEvents.SQUID_SQUIRT, 0.88F),
        CONFUCIAN_RIGHTEOUS_QI(new DustParticleOptions(new Vector3f(1.00F, 0.86F, 0.26F), 0.66F),
                new DustParticleOptions(new Vector3f(0.90F, 1.00F, 0.90F), 0.34F),
                SoundEvents.ENCHANTMENT_TABLE_USE, 1.42F);

        private final DustParticleOptions core;
        private final DustParticleOptions edge;
        private final SoundEvent sound;
        private final float pitch;

        ConfucianForm(DustParticleOptions core, DustParticleOptions edge, SoundEvent sound, float pitch) {
            this.core = core;
            this.edge = edge;
            this.sound = sound;
            this.pitch = pitch;
        }

        private double damageMultiplier(LivingEntity target) {
            if (!disorderLike(target)) {
                return 1.0D;
            }
            return switch (this) {
                case RIGHTEOUS_QI -> 1.0D;
                case WORD_SUPPRESS -> 1.18D;
                case SCROLL_STRIKE -> 1.10D;
                case INK_SEA -> 1.08D;
                case CONFUCIAN_RIGHTEOUS_QI -> 1.28D;
            };
        }

        private void applyTarget(ServerPlayer player, LivingEntity target, Vec3 center, CultivationSkill skill) {
            int bonus = Math.max(0, skill.getLevel() - 1);
            if (disorderLike(target)) {
                add(target, MobEffects.GLOWING, 85 + bonus * 6, 0);
                add(target, MobEffects.WEAKNESS, 85 + bonus * 6, 1);
            }
            switch (this) {
                case WORD_SUPPRESS -> {
                    add(target, MobEffects.MOVEMENT_SLOWDOWN, 110 + bonus * 7, 3);
                    add(target, MobEffects.DIG_SLOWDOWN, 100 + bonus * 6, 1);
                    add(target, MobEffects.CONFUSION, 55 + bonus * 4, 0);
                    target.setDeltaMovement(target.getDeltaMovement().multiply(0.25D, 0.20D, 0.25D));
                    target.hasImpulse = true;
                }
                case SCROLL_STRIKE -> {
                    add(target, MobEffects.MOVEMENT_SLOWDOWN, 65 + bonus * 5, 1);
                    pushAway(target, player.position(), 0.26D, 0.08D);
                }
                case INK_SEA -> {
                    add(target, MobEffects.MOVEMENT_SLOWDOWN, 120 + bonus * 8, 2);
                    add(target, MobEffects.BLINDNESS, 50 + bonus * 4, 0);
                    add(target, MobEffects.WEAKNESS, 90 + bonus * 6, 0);
                    pull(target, center, 0.10D, 0.03D);
                }
                case CONFUCIAN_RIGHTEOUS_QI -> {
                    add(target, MobEffects.GLOWING, 95 + bonus * 6, 0);
                    add(target, MobEffects.WEAKNESS, 90 + bonus * 6, 1);
                    add(target, MobEffects.MOVEMENT_SLOWDOWN, 75 + bonus * 5, 1);
                }
                case RIGHTEOUS_QI -> {
                }
            }
        }

        private void spawnSelf(ServerLevel level, Vec3 base, double height, double radius) {
            for (int layer = 0; layer < 4; layer++) {
                ring(level, base.add(0.0D, 0.12D + layer * 0.23D, 0.0D),
                        radius * (0.26D + layer * 0.14D), 58, 0.045D);
            }
            for (int glyph = 0; glyph < 8; glyph++) {
                double angle = Math.PI * 2.0D * glyph / 8.0D;
                Vec3 center = base.add(Math.cos(angle) * radius * 0.46D,
                        height * 0.56D + Math.sin(angle * 2.0D) * 0.12D,
                        Math.sin(angle) * radius * 0.46D);
                glyph(level, center, angle, 0.30D, glyph);
            }
            level.sendParticles(edge, base.x, base.y + height * 0.62D, base.z, 28, radius * 0.22D, 0.32D, radius * 0.22D, 0.012D);
        }

        private void spawnSingle(ServerLevel level, Vec3 start, Vec3 end) {
            arc(level, start, end, 5);
            for (int layer = 0; layer < 4; layer++) {
                glyph(level, end.add(0.0D, -0.28D + layer * 0.22D, 0.0D), layer * Math.PI * 0.24D, 0.50D + layer * 0.05D, layer);
            }
            ring(level, end.add(0.0D, 0.15D, 0.0D), 0.82D, 54, 0.025D);
        }

        private void spawnLine(ServerLevel level, Vec3 start, Vec3 end, double width) {
            if (this == SCROLL_STRIKE) {
                scrollRibbon(level, start, end, width);
            } else {
                righteousBeam(level, start, end, width);
            }
        }

        private void spawnArea(ServerLevel level, Vec3 center, double radius, List<LivingEntity> targets) {
            for (int layer = 0; layer < 5; layer++) {
                ring(level, center.add(0.0D, 0.08D + layer * 0.10D, 0.0D),
                        radius * (0.28D + layer * 0.18D), 76, 0.05D);
            }
            for (int wave = 0; wave < 14; wave++) {
                double angle = Math.PI * 2.0D * wave / 14.0D;
                Vec3 end = center.add(Math.cos(angle) * radius * 0.95D, 0.18D, Math.sin(angle) * radius * 0.95D);
                line(level, center.add(0.0D, 0.18D, 0.0D), end, 12, wave % 2 == 0 ? core : edge);
            }
            int links = Math.min(7, targets.size());
            for (int i = 0; i < links; i++) {
                LivingEntity target = targets.get(i);
                inkArc(level, center.add(0.0D, 0.55D, 0.0D),
                        target.position().add(0.0D, target.getBbHeight() * 0.45D, 0.0D), i * 19);
            }
            level.sendParticles(core, center.x, center.y + 0.28D, center.z, 38, radius * 0.24D, 0.10D, radius * 0.24D, 0.018D);
        }

        private void scrollRibbon(ServerLevel level, Vec3 start, Vec3 end, double width) {
            Vec3 path = end.subtract(start);
            Vec3 direction = path.lengthSqr() < 0.0001D ? new Vec3(0.0D, 0.0D, 1.0D) : path.normalize();
            Vec3 side = side(direction);
            Vec3 up = side.cross(direction).normalize();
            int steps = Math.max(10, (int)(path.length() * 5.6D));
            for (int i = 0; i <= steps; i++) {
                double t = i / (double)steps;
                Vec3 point = start.lerp(end, t);
                double curl = Math.sin(t * Math.PI * 3.0D) * width * 0.24D;
                Vec3 left = point.add(side.scale(-width * 0.20D + curl)).add(up.scale(Math.sin(t * Math.PI) * 0.08D));
                Vec3 right = point.add(side.scale(width * 0.20D + curl)).add(up.scale(Math.sin(t * Math.PI) * 0.08D));
                level.sendParticles(core, point.x, point.y, point.z, 1, 0.012D, 0.012D, 0.012D, 0.0D);
                if ((i & 1) == 0) {
                    level.sendParticles(edge, left.x, left.y, left.z, 1, 0.014D, 0.014D, 0.014D, 0.0D);
                    level.sendParticles(edge, right.x, right.y, right.z, 1, 0.014D, 0.014D, 0.014D, 0.0D);
                }
            }
            for (int mark = 0; mark < 4; mark++) {
                double t = 0.22D + mark * 0.18D;
                glyph(level, start.lerp(end, t).add(0.0D, 0.06D, 0.0D), mark * Math.PI * 0.35D, width * 0.30D, mark);
            }
            level.sendParticles(edge, end.x, end.y, end.z, 18, 0.18D, 0.14D, 0.18D, 0.006D);
        }

        private void righteousBeam(ServerLevel level, Vec3 start, Vec3 end, double width) {
            Vec3 path = end.subtract(start);
            Vec3 direction = path.lengthSqr() < 0.0001D ? new Vec3(0.0D, 0.0D, 1.0D) : path.normalize();
            Vec3 side = side(direction);
            Vec3 up = side.cross(direction).normalize();
            int steps = Math.max(10, (int)(path.length() * 6.0D));
            for (int i = 0; i <= steps; i++) {
                double t = i / (double)steps;
                Vec3 point = start.lerp(end, t);
                double twist = t * Math.PI * 8.0D;
                Vec3 coil = side.scale(Math.sin(twist) * width * 0.12D).add(up.scale(Math.cos(twist) * width * 0.12D));
                level.sendParticles(core, point.x, point.y, point.z, 1, 0.012D, 0.012D, 0.012D, 0.0D);
                if ((i & 1) == 0) {
                    level.sendParticles(edge, point.x + coil.x, point.y + coil.y, point.z + coil.z,
                            1, 0.014D, 0.014D, 0.014D, 0.0D);
                }
            }
            for (int mark = 0; mark < 5; mark++) {
                glyph(level, start.lerp(end, 0.18D + mark * 0.16D), mark * Math.PI * 0.20D, width * 0.32D, mark);
            }
            level.sendParticles(edge, end.x, end.y, end.z, 24, 0.16D, 0.14D, 0.16D, 0.008D);
        }

        private void glyph(ServerLevel level, Vec3 center, double angle, double size, int seed) {
            Vec3 side = new Vec3(Math.cos(angle), 0.0D, Math.sin(angle));
            Vec3 up = new Vec3(0.0D, 1.0D, 0.0D);
            line(level, center.add(side.scale(-size * 0.45D)).add(up.scale(size * 0.34D)),
                    center.add(side.scale(size * 0.45D)).add(up.scale(size * 0.34D)), 6, edge);
            line(level, center.add(side.scale(-size * 0.38D)),
                    center.add(side.scale(size * 0.38D)), 6, (seed & 1) == 0 ? core : edge);
            line(level, center.add(side.scale(-size * 0.16D)).add(up.scale(-size * 0.36D)),
                    center.add(side.scale(size * 0.18D)).add(up.scale(size * 0.42D)), 7, core);
            if ((seed & 1) == 0) {
                line(level, center.add(side.scale(size * 0.28D)).add(up.scale(-size * 0.28D)),
                        center.add(side.scale(size * 0.42D)).add(up.scale(size * 0.22D)), 5, edge);
            } else {
                line(level, center.add(side.scale(-size * 0.34D)).add(up.scale(-size * 0.24D)),
                        center.add(side.scale(-size * 0.44D)).add(up.scale(size * 0.22D)), 5, edge);
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

        private void arc(ServerLevel level, Vec3 start, Vec3 end, int seed) {
            Vec3 path = end.subtract(start);
            int steps = Math.max(6, (int)(path.length() * 5.0D));
            for (int i = 0; i <= steps; i++) {
                double t = i / (double)steps;
                Vec3 point = start.lerp(end, t).add(Math.sin(seed + i * 1.17D) * 0.04D,
                        Math.cos(seed * 0.37D + i * 0.69D) * 0.035D,
                        Math.cos(seed + i * 1.29D) * 0.04D);
                level.sendParticles((i & 1) == 0 ? core : edge,
                        point.x, point.y, point.z, 1, 0.012D, 0.012D, 0.012D, 0.0D);
            }
        }

        private void inkArc(ServerLevel level, Vec3 start, Vec3 end, int seed) {
            Vec3 path = end.subtract(start);
            int steps = Math.max(7, (int)(path.length() * 4.4D));
            for (int i = 0; i <= steps; i++) {
                double t = i / (double)steps;
                Vec3 point = start.lerp(end, t).add(Math.sin(seed + i * 1.77D) * 0.08D,
                        Math.cos(seed * 0.23D + i * 0.91D) * 0.05D,
                        Math.cos(seed + i * 1.51D) * 0.08D);
                level.sendParticles((i & 2) == 0 ? core : edge,
                        point.x, point.y, point.z, 1, 0.016D, 0.016D, 0.016D, 0.0D);
            }
        }

        private void line(ServerLevel level, Vec3 start, Vec3 end, int steps, DustParticleOptions particle) {
            for (int i = 0; i <= steps; i++) {
                double t = i / (double)Math.max(1, steps);
                Vec3 point = start.lerp(end, t);
                level.sendParticles(particle, point.x, point.y, point.z, 1, 0.012D, 0.012D, 0.012D, 0.0D);
            }
        }

        private Vec3 side(Vec3 direction) {
            Vec3 side = direction.cross(new Vec3(0.0D, 1.0D, 0.0D));
            return side.lengthSqr() < 0.0001D ? new Vec3(1.0D, 0.0D, 0.0D) : side.normalize();
        }

        private static void pushAway(LivingEntity target, Vec3 center, double horizontal, double vertical) {
            Vec3 away = target.position().subtract(center);
            if (away.lengthSqr() < 0.001D) {
                return;
            }
            Vec3 n = away.normalize();
            target.push(n.x * horizontal, vertical, n.z * horizontal);
            target.hasImpulse = true;
        }

        private static void pull(LivingEntity target, Vec3 center, double horizontal, double vertical) {
            Vec3 pull = center.subtract(target.position());
            if (pull.lengthSqr() < 0.001D) {
                return;
            }
            Vec3 n = pull.normalize();
            target.push(n.x * horizontal, vertical, n.z * horizontal);
            target.hasImpulse = true;
        }
    }
}
