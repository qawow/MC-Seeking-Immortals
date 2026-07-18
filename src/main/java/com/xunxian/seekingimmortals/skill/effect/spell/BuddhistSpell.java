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

public class BuddhistSpell extends SpellEffect {
    private final double range;
    private final double radius;
    private final BuddhistForm form;
    private final String successKey;

    public BuddhistSpell(int cost, int cooldownTicks, double damage, double range, double radius,
                         BuddhistForm form, String successKey) {
        super(cost, cooldownTicks, damage);
        this.range = range;
        this.radius = radius;
        this.form = form;
        this.successKey = successKey;
    }

    @Override
    public boolean execute(ServerPlayer player, PlayerCultivation cultivation, CultivationSkill skill, SkillContext context) {
        return switch (form) {
            case BUDDHA_LIGHT, ZEN_PULSE -> castArea(player, skill);
            case SARIRA_SHIELD -> castShield(player, skill);
            case DEMON_SUBDUE_PALM, VAJRA_PALM -> castPalmLine(player, skill);
            case DAJIN_BUDDHIST_VAJRA -> castVajraStrike(player, skill);
        };
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
        for (LivingEntity target : targets.stream().limit(form == BuddhistForm.ZEN_PULSE ? 10 : 8).toList()) {
            double falloff = Math.max(0.46D, 1.0D - target.position().distanceTo(center) / (radius + 0.9D));
            if (damage > 0.0D) {
                target.hurt(player.damageSources().indirectMagic(player, player), (float)(damage * falloff * form.damageMultiplier(target)));
            }
            form.applyTargetEffects(player, target, center, skill);
            hitCount++;
        }

        form.spawnArea(level, center, radius, targets);
        play(level, form, BlockPos.containing(center));
        player.displayClientMessage(Component.translatable(successKey, hitCount), true);
        return true;
    }

    private boolean castShield(ServerPlayer player, CultivationSkill skill) {
        ServerLevel level = player.serverLevel();
        int duration = scaledTicks(170, skill, 12);
        add(player, MobEffects.ABSORPTION, duration, Math.max(0, skill.getLevel() / 5));
        add(player, MobEffects.DAMAGE_RESISTANCE, duration, 0);
        add(player, MobEffects.FIRE_RESISTANCE, Math.max(90, duration / 2), 0);
        player.removeEffect(MobEffects.WEAKNESS);

        int deflected = deflectProjectiles(player, level, radius);
        int repelled = repelDemonLike(player, level, radius + 0.7D, skill);
        form.spawnShield(level, player.position(), player.getBbHeight(), radius);
        play(level, form, player.blockPosition());
        player.displayClientMessage(Component.translatable(successKey, deflected, repelled), true);
        return true;
    }

    private boolean castPalmLine(ServerPlayer player, CultivationSkill skill) {
        ServerLevel level = player.serverLevel();
        Vec3 start = player.getEyePosition().add(player.getLookAngle().normalize().scale(0.45D));
        Vec3 end = traceEnd(level, player, start, range);
        List<LivingEntity> targets = findLineTargets(level, player, start, end, Math.max(0.8D, radius));
        if (targets.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.spell.target.fail"), true);
            form.spawnLine(level, start, end, Math.max(0.8D, radius));
            return false;
        }

        double damage = calculateDamage(skill.getLevel(), skill.getProficiency());
        int hitCount = 0;
        for (LivingEntity target : targets.stream().limit(form == BuddhistForm.DEMON_SUBDUE_PALM ? 5 : 4).toList()) {
            double along = target.position().distanceTo(start);
            double falloff = Math.max(0.55D, 1.0D - along / (range * 1.45D));
            target.hurt(player.damageSources().indirectMagic(player, player), (float)(damage * falloff * form.damageMultiplier(target)));
            form.applyTargetEffects(player, target, player.position(), skill);
            hitCount++;
        }

        form.spawnLine(level, start, targets.get(0).getEyePosition(), Math.max(0.8D, radius));
        play(level, form, player.blockPosition());
        player.displayClientMessage(Component.translatable(successKey, hitCount), true);
        return true;
    }

    private boolean castVajraStrike(ServerPlayer player, CultivationSkill skill) {
        ServerLevel level = player.serverLevel();
        LivingEntity target = findTarget(level, player, range, Math.max(0.95D, radius));
        if (target == null) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.spell.target.fail"), true);
            return false;
        }

        double damage = calculateDamage(skill.getLevel(), skill.getProficiency());
        target.hurt(player.damageSources().indirectMagic(player, player), (float)(damage * form.damageMultiplier(target)));
        form.applyTargetEffects(player, target, player.position(), skill);
        Vec3 center = target.position().add(0.0D, target.getBbHeight() * 0.52D, 0.0D);
        form.spawnStrike(level, player.getEyePosition(), center);
        play(level, form, target.blockPosition());
        player.displayClientMessage(Component.translatable(successKey, target.getDisplayName()), true);
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
        AABB area = new AABB(center, center).inflate(maxRadius, Math.max(2.6D, maxRadius * 0.62D), maxRadius);
        return level.getEntitiesOfClass(LivingEntity.class, area,
                        entity -> canTarget(entity, player) && entity.position().distanceToSqr(center) <= maxRadius * maxRadius)
                .stream()
                .sorted(Comparator.comparingDouble(entity -> entity.distanceToSqr(center)))
                .toList();
    }

    private List<LivingEntity> findLineTargets(ServerLevel level, ServerPlayer player, Vec3 start, Vec3 end, double maxRadius) {
        Vec3 line = end.subtract(start);
        AABB area = new AABB(start, end).inflate(maxRadius);
        return level.getEntitiesOfClass(LivingEntity.class, area,
                        entity -> canTarget(entity, player)
                                && distanceToSegment(entity.position().add(0.0D, entity.getBbHeight() * 0.55D, 0.0D), start, line) <= maxRadius)
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

    private static boolean demonLike(LivingEntity target) {
        return target.getMobType() == MobType.UNDEAD || target instanceof Enemy;
    }

    private static int deflectProjectiles(ServerPlayer player, ServerLevel level, double radius) {
        AABB area = player.getBoundingBox().inflate(radius, radius * 0.72D, radius);
        List<Projectile> projectiles = level.getEntitiesOfClass(Projectile.class, area,
                projectile -> projectile.isAlive() && projectile.getOwner() != player);
        int changed = 0;
        for (Projectile projectile : projectiles) {
            Vec3 direction = projectile.position().subtract(player.position());
            if (direction.lengthSqr() < 0.001D) {
                direction = player.getLookAngle();
            }
            double speed = Math.max(0.42D, projectile.getDeltaMovement().length() + 0.10D);
            projectile.setDeltaMovement(direction.normalize().scale(speed).add(0.0D, 0.06D, 0.0D));
            projectile.hasImpulse = true;
            changed++;
        }
        return changed;
    }

    private static int repelDemonLike(ServerPlayer player, ServerLevel level, double radius, CultivationSkill skill) {
        AABB area = player.getBoundingBox().inflate(radius, 2.4D, radius);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, area,
                target -> canTarget(target, player) && demonLike(target) && target.distanceToSqr(player) <= radius * radius);
        int changed = 0;
        for (LivingEntity target : targets.stream().limit(6).toList()) {
            Vec3 away = target.position().subtract(player.position());
            if (away.lengthSqr() > 0.001D) {
                target.push(away.normalize().x * 0.22D, 0.10D, away.normalize().z * 0.22D);
                target.hasImpulse = true;
            }
            add(target, MobEffects.WEAKNESS, scaledTicks(80, skill, 5), 0);
            add(target, MobEffects.MOVEMENT_SLOWDOWN, scaledTicks(70, skill, 5), 1);
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

    private static void play(ServerLevel level, BuddhistForm form, BlockPos pos) {
        level.playSound(null, pos, form.sound, SoundSource.PLAYERS, 0.78F, form.pitch);
    }

    public enum BuddhistForm {
        BUDDHA_LIGHT(new DustParticleOptions(new Vector3f(1.00F, 0.86F, 0.24F), 0.76F),
                new DustParticleOptions(new Vector3f(1.00F, 1.00F, 0.76F), 0.40F),
                SoundEvents.BEACON_POWER_SELECT, 1.12F),
        SARIRA_SHIELD(new DustParticleOptions(new Vector3f(1.00F, 0.90F, 0.42F), 0.68F),
                new DustParticleOptions(new Vector3f(0.98F, 1.00F, 0.92F), 0.34F),
                SoundEvents.AMETHYST_BLOCK_CHIME, 1.42F),
        DEMON_SUBDUE_PALM(new DustParticleOptions(new Vector3f(1.00F, 0.66F, 0.18F), 0.72F),
                new DustParticleOptions(new Vector3f(1.00F, 0.96F, 0.50F), 0.42F),
                SoundEvents.ANVIL_LAND, 1.78F),
        ZEN_PULSE(new DustParticleOptions(new Vector3f(0.98F, 0.82F, 0.34F), 0.56F),
                new DustParticleOptions(new Vector3f(0.72F, 0.94F, 1.00F), 0.34F),
                SoundEvents.NOTE_BLOCK_CHIME.value(), 0.82F),
        VAJRA_PALM(new DustParticleOptions(new Vector3f(1.00F, 0.78F, 0.22F), 0.68F),
                new DustParticleOptions(new Vector3f(0.90F, 0.96F, 1.00F), 0.36F),
                SoundEvents.TRIDENT_THROW, 1.34F),
        DAJIN_BUDDHIST_VAJRA(new DustParticleOptions(new Vector3f(1.00F, 0.72F, 0.16F), 0.76F),
                new DustParticleOptions(new Vector3f(1.00F, 0.98F, 0.62F), 0.42F),
                SoundEvents.TRIDENT_HIT, 1.10F);

        private final DustParticleOptions core;
        private final DustParticleOptions edge;
        private final SoundEvent sound;
        private final float pitch;

        BuddhistForm(DustParticleOptions core, DustParticleOptions edge, SoundEvent sound, float pitch) {
            this.core = core;
            this.edge = edge;
            this.sound = sound;
            this.pitch = pitch;
        }

        private double damageMultiplier(LivingEntity target) {
            if (!demonLike(target)) {
                return 1.0D;
            }
            return switch (this) {
                case DEMON_SUBDUE_PALM, DAJIN_BUDDHIST_VAJRA -> 1.36D;
                case BUDDHA_LIGHT, VAJRA_PALM -> 1.25D;
                case ZEN_PULSE -> 1.18D;
                case SARIRA_SHIELD -> 1.0D;
            };
        }

        private void applyTargetEffects(ServerPlayer player, LivingEntity target, Vec3 center, CultivationSkill skill) {
            int bonus = Math.max(0, skill.getLevel() - 1);
            if (demonLike(target)) {
                add(target, MobEffects.GLOWING, 95 + bonus * 6, 0);
                add(target, MobEffects.WEAKNESS, 95 + bonus * 7, 1);
            }
            switch (this) {
                case BUDDHA_LIGHT -> {
                    add(target, MobEffects.MOVEMENT_SLOWDOWN, 85 + bonus * 6, 1);
                    add(target, MobEffects.WEAKNESS, 75 + bonus * 5, 0);
                    target.removeEffect(MobEffects.INVISIBILITY);
                    pull(target, center, 0.10D, 0.04D);
                }
                case ZEN_PULSE -> {
                    add(target, MobEffects.CONFUSION, 70 + bonus * 4, 0);
                    add(target, MobEffects.DIG_SLOWDOWN, 90 + bonus * 6, 1);
                    add(target, MobEffects.MOVEMENT_SLOWDOWN, 70 + bonus * 5, 1);
                    pushAway(target, player.position(), 0.12D, 0.05D);
                }
                case DEMON_SUBDUE_PALM -> {
                    add(target, MobEffects.MOVEMENT_SLOWDOWN, 95 + bonus * 7, 2);
                    add(target, MobEffects.WEAKNESS, 90 + bonus * 6, 1);
                    pushAway(target, player.position(), demonLike(target) ? 0.48D : 0.30D, 0.16D);
                }
                case VAJRA_PALM -> {
                    add(target, MobEffects.MOVEMENT_SLOWDOWN, 85 + bonus * 6, 1);
                    add(target, MobEffects.WEAKNESS, 75 + bonus * 6, 0);
                    add(target, MobEffects.GLOWING, 70 + bonus * 5, 0);
                }
                case DAJIN_BUDDHIST_VAJRA -> {
                    add(target, MobEffects.MOVEMENT_SLOWDOWN, 125 + bonus * 8, 3);
                    add(target, MobEffects.DIG_SLOWDOWN, 105 + bonus * 7, 1);
                    add(target, MobEffects.WEAKNESS, 115 + bonus * 7, 1);
                    target.setDeltaMovement(target.getDeltaMovement().multiply(0.40D, 0.28D, 0.40D));
                    target.hasImpulse = true;
                }
                case SARIRA_SHIELD -> {
                }
            }
        }

        private void spawnArea(ServerLevel level, Vec3 center, double radius, List<LivingEntity> targets) {
            if (this == BUDDHA_LIGHT) {
                spawnBuddhaLight(level, center, radius, targets);
            } else if (this == ZEN_PULSE) {
                spawnZenPulse(level, center, radius, targets);
            }
        }

        private void spawnShield(ServerLevel level, Vec3 base, double height, double radius) {
            double shell = Math.max(1.15D, radius * 0.40D);
            for (int layer = 0; layer < 6; layer++) {
                double t = layer / 5.0D;
                double y = base.y + 0.14D + t * Math.max(1.55D, height + 0.42D);
                double r = Math.max(0.16D, shell * Math.sin((layer + 1) * Math.PI / 7.0D));
                ring(level, new Vec3(base.x, y, base.z), r, 62, 0.035D);
            }
            for (int bead = 0; bead < 8; bead++) {
                double angle = Math.PI * 2.0D * bead / 8.0D;
                Vec3 beadCenter = base.add(Math.cos(angle) * shell * 0.74D, height * 0.58D, Math.sin(angle) * shell * 0.74D);
                sphere(level, beadCenter, 0.16D, 14);
            }
            level.sendParticles(edge, base.x, base.y + height * 0.58D, base.z, 34, shell * 0.28D, 0.36D, shell * 0.28D, 0.012D);
        }

        private void spawnLine(ServerLevel level, Vec3 start, Vec3 end, double width) {
            if (this == DEMON_SUBDUE_PALM) {
                spawnPalm(level, start, end, width);
            } else {
                spawnVajraBeam(level, start, end, width);
            }
        }

        private void spawnStrike(ServerLevel level, Vec3 start, Vec3 center) {
            arc(level, start, center, 19);
            Vec3 top = center.add(0.0D, 2.7D, 0.0D);
            Vec3 bottom = center.add(0.0D, -0.45D, 0.0D);
            for (int shaft = 0; shaft < 4; shaft++) {
                double angle = shaft * Math.PI * 0.5D;
                Vec3 offset = new Vec3(Math.cos(angle) * 0.12D, 0.0D, Math.sin(angle) * 0.12D);
                line(level, top.add(offset), bottom.add(offset), 22, (shaft & 1) == 0 ? core : edge);
            }
            for (int ring = 0; ring < 4; ring++) {
                ring(level, center.add(0.0D, -0.20D + ring * 0.34D, 0.0D), 0.72D - ring * 0.08D, 54, 0.03D);
            }
            level.sendParticles(edge, center.x, center.y + 0.15D, center.z, 26, 0.24D, 0.30D, 0.24D, 0.012D);
        }

        private void spawnBuddhaLight(ServerLevel level, Vec3 center, double radius, List<LivingEntity> targets) {
            for (int layer = 0; layer < 3; layer++) {
                ring(level, center.add(0.0D, 0.18D + layer * 0.28D, 0.0D), radius * (0.54D + layer * 0.21D), 84, 0.06D);
            }
            for (int petal = 0; petal < 8; petal++) {
                double angle = Math.PI * 2.0D * petal / 8.0D;
                lotusPetal(level, center, radius * 0.62D, angle);
            }
            int links = Math.min(8, targets.size());
            for (int i = 0; i < links; i++) {
                LivingEntity target = targets.get(i);
                Vec3 targetCenter = target.position().add(0.0D, target.getBbHeight() * 0.60D, 0.0D);
                arc(level, center.add(0.0D, 2.6D, 0.0D), targetCenter, i * 17);
            }
            level.sendParticles(edge, center.x, center.y + 1.15D, center.z, 36, radius * 0.20D, 0.30D, radius * 0.20D, 0.012D);
        }

        private void spawnZenPulse(ServerLevel level, Vec3 center, double radius, List<LivingEntity> targets) {
            for (int layer = 0; layer < 4; layer++) {
                ring(level, center.add(0.0D, 0.18D + layer * 0.22D, 0.0D), radius * (0.35D + layer * 0.22D), 78, 0.09D);
            }
            for (int spoke = 0; spoke < 12; spoke++) {
                double angle = spoke * Math.PI * 2.0D / 12.0D;
                Vec3 end = center.add(Math.cos(angle) * radius, 0.55D, Math.sin(angle) * radius);
                line(level, center.add(0.0D, 0.55D, 0.0D), end, 12, spoke % 3 == 0 ? edge : core);
            }
            int links = Math.min(6, targets.size());
            for (int i = 0; i < links; i++) {
                LivingEntity target = targets.get(i);
                arc(level, center.add(0.0D, 0.85D, 0.0D),
                        target.position().add(0.0D, target.getBbHeight() * 0.55D, 0.0D), i * 13);
            }
        }

        private void spawnPalm(ServerLevel level, Vec3 start, Vec3 end, double width) {
            Vec3 line = end.subtract(start);
            Vec3 direction = line.lengthSqr() < 0.0001D ? new Vec3(0.0D, 0.0D, 1.0D) : line.normalize();
            Vec3 side = side(direction);
            Vec3 up = side.cross(direction).normalize();
            int steps = Math.max(8, (int)(line.length() * 4.8D));
            for (int i = 0; i <= steps; i++) {
                double t = i / (double)steps;
                Vec3 point = start.lerp(end, t);
                double spread = Math.sin(t * Math.PI) * width * 0.58D;
                level.sendParticles(core, point.x, point.y, point.z, 1, 0.022D, 0.022D, 0.022D, 0.0D);
                for (int finger = -2; finger <= 2; finger++) {
                    Vec3 offset = side.scale(finger * spread * 0.28D).add(up.scale(Math.abs(finger) * 0.035D));
                    level.sendParticles(edge, point.x + offset.x, point.y + offset.y, point.z + offset.z,
                            1, 0.018D, 0.018D, 0.018D, 0.0D);
                }
            }
            palmPrint(level, end, side, up, width * 0.72D);
        }

        private void spawnVajraBeam(ServerLevel level, Vec3 start, Vec3 end, double width) {
            Vec3 line = end.subtract(start);
            Vec3 direction = line.lengthSqr() < 0.0001D ? new Vec3(0.0D, 0.0D, 1.0D) : line.normalize();
            Vec3 side = side(direction);
            Vec3 up = side.cross(direction).normalize();
            int steps = Math.max(10, (int)(line.length() * 6.0D));
            for (int i = 0; i <= steps; i++) {
                double t = i / (double)steps;
                Vec3 point = start.lerp(end, t);
                double twist = t * Math.PI * 10.0D;
                Vec3 coil = side.scale(Math.sin(twist) * width * 0.15D).add(up.scale(Math.cos(twist) * width * 0.15D));
                level.sendParticles(core, point.x, point.y, point.z, 1, 0.014D, 0.014D, 0.014D, 0.0D);
                if ((i & 1) == 0) {
                    level.sendParticles(edge, point.x + coil.x, point.y + coil.y, point.z + coil.z,
                            1, 0.016D, 0.016D, 0.016D, 0.0D);
                }
            }
            for (int band = 0; band < 3; band++) {
                double t = 0.30D + band * 0.22D;
                vajraBand(level, start.lerp(end, t), direction, side, up, width * 0.34D);
            }
            level.sendParticles(edge, end.x, end.y, end.z, 22, 0.18D, 0.16D, 0.18D, 0.008D);
        }

        private void lotusPetal(ServerLevel level, Vec3 center, double radius, double angle) {
            Vec3 outward = new Vec3(Math.cos(angle), 0.0D, Math.sin(angle));
            Vec3 side = new Vec3(-outward.z, 0.0D, outward.x);
            Vec3 base = center.add(outward.scale(radius * 0.55D)).add(0.0D, 0.30D, 0.0D);
            for (int i = 0; i < 18; i++) {
                double t = i / 17.0D;
                double curl = Math.sin(t * Math.PI) * radius * 0.22D;
                Vec3 point = base.add(outward.scale(radius * 0.42D * t)).add(side.scale(Math.sin(t * Math.PI * 2.0D) * curl * 0.34D));
                level.sendParticles((i & 1) == 0 ? core : edge, point.x, point.y + curl * 0.16D, point.z,
                        1, 0.014D, 0.014D, 0.014D, 0.0D);
            }
        }

        private void palmPrint(ServerLevel level, Vec3 center, Vec3 side, Vec3 up, double size) {
            for (int finger = -2; finger <= 2; finger++) {
                Vec3 fingerBase = center.add(side.scale(finger * size * 0.18D)).add(up.scale(size * 0.05D));
                Vec3 fingerTip = fingerBase.add(up.scale(size * (0.42D + (2 - Math.abs(finger)) * 0.04D)));
                line(level, fingerBase, fingerTip, 8, edge);
            }
            for (int i = 0; i < 28; i++) {
                double angle = Math.PI * 2.0D * i / 28.0D;
                double x = Math.cos(angle) * size * 0.42D;
                double y = Math.sin(angle) * size * 0.30D - size * 0.18D;
                Vec3 point = center.add(side.scale(x)).add(up.scale(y));
                level.sendParticles(core, point.x, point.y, point.z, 1, 0.014D, 0.014D, 0.014D, 0.0D);
            }
        }

        private void vajraBand(ServerLevel level, Vec3 center, Vec3 direction, Vec3 side, Vec3 up, double size) {
            for (int arm = 0; arm < 4; arm++) {
                double angle = Math.PI * 0.5D * arm;
                Vec3 offset = side.scale(Math.cos(angle) * size).add(up.scale(Math.sin(angle) * size));
                line(level, center.subtract(offset), center.add(offset), 7, (arm & 1) == 0 ? core : edge);
            }
            line(level, center.subtract(direction.scale(size * 0.55D)), center.add(direction.scale(size * 0.55D)), 6, edge);
        }

        private void ring(ServerLevel level, Vec3 center, double radius, int points, double wave) {
            for (int i = 0; i < points; i++) {
                double angle = Math.PI * 2.0D * i / points;
                level.sendParticles((i & 1) == 0 ? core : edge,
                        center.x + Math.cos(angle) * radius,
                        center.y + Math.sin(angle * 3.0D) * wave,
                        center.z + Math.sin(angle) * radius,
                        1, 0.018D, 0.018D, 0.018D, 0.0D);
            }
        }

        private void sphere(ServerLevel level, Vec3 center, double radius, int points) {
            for (int i = 0; i < points; i++) {
                double theta = Math.PI * 2.0D * i / points;
                double phi = Math.PI * (i % 7) / 7.0D;
                level.sendParticles(edge,
                        center.x + Math.cos(theta) * Math.sin(phi) * radius,
                        center.y + Math.cos(phi) * radius,
                        center.z + Math.sin(theta) * Math.sin(phi) * radius,
                        1, 0.008D, 0.008D, 0.008D, 0.0D);
            }
        }

        private void arc(ServerLevel level, Vec3 start, Vec3 end, int seed) {
            Vec3 path = end.subtract(start);
            int steps = Math.max(6, (int)(path.length() * 4.8D));
            for (int i = 0; i <= steps; i++) {
                double t = i / (double)steps;
                Vec3 point = start.lerp(end, t).add(Math.sin(seed + i * 1.11D) * 0.05D,
                        Math.cos(seed * 0.29D + i * 0.83D) * 0.04D,
                        Math.cos(seed + i * 1.07D) * 0.05D);
                level.sendParticles((i & 1) == 0 ? core : edge,
                        point.x, point.y, point.z, 1, 0.014D, 0.014D, 0.014D, 0.0D);
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
