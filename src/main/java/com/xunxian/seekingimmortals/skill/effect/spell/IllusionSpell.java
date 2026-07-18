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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class IllusionSpell extends SpellEffect {
    private final double range;
    private final double radius;
    private final IllusionForm form;
    private final String successKey;

    public IllusionSpell(int cost, int cooldownTicks, double damage, double range, double radius,
                         IllusionForm form, String successKey) {
        super(cost, cooldownTicks, damage);
        this.range = range;
        this.radius = radius;
        this.form = form;
        this.successKey = successKey;
    }

    @Override
    public boolean execute(ServerPlayer player, PlayerCultivation cultivation, CultivationSkill skill, SkillContext context) {
        return switch (form) {
            case MIRROR_PHANTOM, MIND_CONFUSION, DREAM_SNARE -> castSingleTarget(player, skill);
            case HUNDRED_ILLUSION, YANYUE_PHANTOM_ARRAY, WANHU_NINE_ILLUSION -> castArea(player, skill);
            case VOID_STEP -> castVoidStep(player, skill);
            case CLONE_IMAGE -> castCloneImage(player, skill);
            case VEIL_OF_MOON, INVISIBILITY_BASIC, ILLUSION_MIST, INVERSE_STAR_VEIL, YANYUE_MOON_ILLUSION -> castStealth(player, skill);
        };
    }

    private boolean castSingleTarget(ServerPlayer player, CultivationSkill skill) {
        ServerLevel level = player.serverLevel();
        LivingEntity target = findTarget(level, player, range, Math.max(0.75D, radius));
        if (target == null) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.spell.target.fail"), true);
            return false;
        }

        double damage = calculateDamage(skill.getLevel(), skill.getProficiency());
        if (damage > 0.0D) {
            target.hurt(player.damageSources().indirectMagic(player, player), (float)damage);
        }
        form.applySingle(target, skill);
        form.spawnSingle(level, player.getEyePosition(), target.position().add(0.0D, target.getBbHeight() * 0.58D, 0.0D));
        play(level, player, form, target.blockPosition());
        player.displayClientMessage(Component.translatable(successKey, target.getDisplayName()), true);
        return true;
    }

    private boolean castArea(ServerPlayer player, CultivationSkill skill) {
        ServerLevel level = player.serverLevel();
        Vec3 center = findLookPoint(level, player, range);
        List<LivingEntity> targets = findAreaTargets(level, player, center, radius);
        if (targets.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.spell.area.fail"), true);
            form.spawnArea(level, center, radius * 0.75D, List.of());
            return false;
        }

        double damage = calculateDamage(skill.getLevel(), skill.getProficiency());
        int hitCount = 0;
        for (LivingEntity target : targets) {
            double falloff = Math.max(0.42D, 1.0D - Math.sqrt(target.distanceToSqr(center)) / (radius * 1.65D));
            if (damage > 0.0D) {
                target.hurt(player.damageSources().indirectMagic(player, player), (float)(damage * falloff));
            }
            form.applyArea(target, center, skill);
            hitCount++;
        }

        form.spawnArea(level, center, radius, targets);
        play(level, player, form, BlockPos.containing(center));
        player.displayClientMessage(Component.translatable(successKey, hitCount), true);
        return true;
    }

    private boolean castVoidStep(ServerPlayer player, CultivationSkill skill) {
        ServerLevel level = player.serverLevel();
        Vec3 flat = new Vec3(player.getLookAngle().x, 0.0D, player.getLookAngle().z);
        if (flat.lengthSqr() < 0.001D) {
            return false;
        }
        flat = flat.normalize();

        Vec3 origin = player.position();
        Vec3 destination = findSafeDestination(level, player, origin, flat, range);
        if (destination == null) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.spell.void_step.fail"), true);
            return false;
        }

        List<LivingEntity> targets = findLineTargets(level, player,
                origin.add(0.0D, 0.75D, 0.0D), destination.add(0.0D, 0.75D, 0.0D), Math.max(1.05D, radius));
        double damage = calculateDamage(skill.getLevel(), skill.getProficiency()) * 0.55D;
        for (LivingEntity target : targets) {
            if (damage > 0.0D) {
                target.hurt(player.damageSources().indirectMagic(player, player), (float)damage);
            }
            add(target, MobEffects.CONFUSION, scaleTicks(70, skill, 5), 0);
            add(target, MobEffects.MOVEMENT_SLOWDOWN, scaleTicks(55, skill, 4), 1);
        }

        form.spawnStep(level, origin.add(0.0D, 0.75D, 0.0D), destination.add(0.0D, 0.75D, 0.0D));
        play(level, player, form, player.blockPosition());
        player.teleportTo(destination.x, destination.y, destination.z);
        player.fallDistance = 0.0F;
        add(player, MobEffects.MOVEMENT_SPEED, scaleTicks(70, skill, 5), 0);
        add(player, MobEffects.INVISIBILITY, scaleTicks(45, skill, 3), 0);
        level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_CLUSTER_STEP, SoundSource.PLAYERS, 0.58F, 1.72F);
        player.displayClientMessage(Component.translatable(successKey, targets.size()), true);
        return true;
    }

    private boolean castCloneImage(ServerPlayer player, CultivationSkill skill) {
        ServerLevel level = player.serverLevel();
        int duration = scaleTicks(135, skill, 9);
        add(player, MobEffects.INVISIBILITY, duration, 0);
        add(player, MobEffects.MOVEMENT_SPEED, duration, 0);
        add(player, MobEffects.ABSORPTION, duration, Math.max(0, skill.getLevel() / 6));

        List<LivingEntity> targets = findAreaTargets(level, player, player.position(), radius);
        for (LivingEntity target : targets.stream().limit(8).toList()) {
            add(target, MobEffects.CONFUSION, scaleTicks(85, skill, 6), 0);
            add(target, MobEffects.WEAKNESS, scaleTicks(65, skill, 5), 0);
        }

        form.spawnClones(level, player.position(), player.getBbHeight(), Math.max(3.0D, radius));
        play(level, player, form, player.blockPosition());
        player.displayClientMessage(Component.translatable(successKey, Math.min(8, targets.size())), true);
        return true;
    }

    private boolean castStealth(ServerPlayer player, CultivationSkill skill) {
        ServerLevel level = player.serverLevel();
        int duration = switch (form) {
            case INVISIBILITY_BASIC -> scaleTicks(150, skill, 10);
            case VEIL_OF_MOON -> scaleTicks(135, skill, 10);
            case ILLUSION_MIST -> scaleTicks(120, skill, 8);
            case INVERSE_STAR_VEIL -> scaleTicks(170, skill, 12);
            case YANYUE_MOON_ILLUSION -> scaleTicks(160, skill, 12);
            default -> scaleTicks(120, skill, 8);
        };

        add(player, MobEffects.INVISIBILITY, duration, 0);
        add(player, MobEffects.MOVEMENT_SPEED, Math.max(60, duration / 2), form == IllusionForm.VOID_STEP ? 1 : 0);
        if (form == IllusionForm.VEIL_OF_MOON || form == IllusionForm.INVERSE_STAR_VEIL || form == IllusionForm.YANYUE_MOON_ILLUSION) {
            add(player, MobEffects.DAMAGE_RESISTANCE, Math.max(70, duration / 2), 0);
        }
        if (form == IllusionForm.INVERSE_STAR_VEIL || form == IllusionForm.YANYUE_MOON_ILLUSION) {
            add(player, MobEffects.ABSORPTION, Math.max(90, duration / 2), 0);
            player.removeEffect(MobEffects.GLOWING);
        }

        int disturbed = 0;
        if (form == IllusionForm.ILLUSION_MIST || form == IllusionForm.YANYUE_MOON_ILLUSION || form == IllusionForm.INVERSE_STAR_VEIL) {
            List<LivingEntity> targets = findAreaTargets(level, player, player.position(), radius);
            for (LivingEntity target : targets.stream().limit(10).toList()) {
                add(target, MobEffects.CONFUSION, scaleTicks(80, skill, 5), 0);
                add(target, MobEffects.MOVEMENT_SLOWDOWN, scaleTicks(65, skill, 4), 1);
                disturbed++;
            }
        }

        form.spawnVeil(level, player.position(), player.getBbHeight(), Math.max(2.2D, radius), disturbed);
        play(level, player, form, player.blockPosition());
        player.displayClientMessage(Component.translatable(successKey, form == IllusionForm.ILLUSION_MIST ? disturbed : duration / 20), true);
        return true;
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
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(level, player, start, traceEnd,
                new AABB(start, traceEnd).inflate(Math.max(0.65D, inflate)),
                entity -> canTarget(entity, player));
        if (entityHit == null || !(entityHit.getEntity() instanceof LivingEntity living)) {
            return null;
        }
        return living;
    }

    private Vec3 findLookPoint(ServerLevel level, ServerPlayer player, double maxRange) {
        Vec3 direction = player.getLookAngle();
        if (direction.lengthSqr() < 0.001D) {
            return player.position();
        }
        direction = direction.normalize();
        Vec3 start = player.getEyePosition();
        Vec3 maxEnd = start.add(direction.scale(maxRange));
        BlockHitResult blockHit = level.clip(new ClipContext(start, maxEnd, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        return blockHit.getType() == HitResult.Type.MISS ? maxEnd : blockHit.getLocation();
    }

    private Vec3 findSafeDestination(ServerLevel level, ServerPlayer player, Vec3 origin, Vec3 flat, double maxRange) {
        Vec3 eye = player.getEyePosition();
        Vec3 maxEnd = eye.add(flat.scale(maxRange));
        BlockHitResult blockHit = level.clip(new ClipContext(eye, maxEnd, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        double maxDistance = blockHit.getType() == HitResult.Type.MISS
                ? maxRange
                : Math.max(2.8D, blockHit.getLocation().subtract(eye).length() - 0.85D);
        for (double distance = maxDistance; distance >= 2.5D; distance -= 0.65D) {
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

    private List<LivingEntity> findAreaTargets(ServerLevel level, ServerPlayer player, Vec3 center, double maxRadius) {
        AABB area = new AABB(center, center).inflate(maxRadius, Math.max(2.6D, maxRadius * 0.65D), maxRadius);
        return level.getEntitiesOfClass(LivingEntity.class, area,
                        entity -> canTarget(entity, player) && entity.distanceToSqr(center) <= maxRadius * maxRadius)
                .stream()
                .sorted(Comparator.comparingDouble(entity -> entity.distanceToSqr(center)))
                .toList();
    }

    private List<LivingEntity> findLineTargets(ServerLevel level, ServerPlayer player, Vec3 start, Vec3 end, double maxRadius) {
        Vec3 line = end.subtract(start);
        AABB area = new AABB(start, end).inflate(maxRadius, maxRadius + 0.4D, maxRadius);
        return level.getEntitiesOfClass(LivingEntity.class, area,
                entity -> canTarget(entity, player)
                        && distanceToSegment(entity.position().add(0.0D, entity.getBbHeight() * 0.52D, 0.0D), start, line) <= maxRadius);
    }

    private boolean canTarget(Entity entity, ServerPlayer player) {
        return canAffect(player, entity);
    }

    private boolean canStandAt(ServerLevel level, BlockPos feet) {
        BlockState feetState = level.getBlockState(feet);
        BlockState headState = level.getBlockState(feet.above());
        BlockState belowState = level.getBlockState(feet.below());
        return belowState.isSolidRender(level, feet.below())
                && feetState.getCollisionShape(level, feet).isEmpty()
                && headState.getCollisionShape(level, feet.above()).isEmpty();
    }

    private static void add(LivingEntity target, MobEffect effect, int durationTicks, int amplifier) {
        if (durationTicks > 0) {
            target.addEffect(new MobEffectInstance(effect, durationTicks, amplifier, false, true));
        }
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

    private static void play(ServerLevel level, ServerPlayer player, IllusionForm form, BlockPos pos) {
        level.playSound(null, pos, form.sound, SoundSource.PLAYERS, 0.72F, form.pitch);
    }

    public enum IllusionForm {
        MIRROR_PHANTOM(new DustParticleOptions(new Vector3f(0.78F, 0.92F, 1.00F), 0.42F),
                new DustParticleOptions(new Vector3f(1.00F, 0.88F, 0.96F), 0.28F),
                SoundEvents.GLASS_PLACE, 1.62F),
        HUNDRED_ILLUSION(new DustParticleOptions(new Vector3f(0.72F, 0.42F, 1.00F), 0.52F),
                new DustParticleOptions(new Vector3f(0.98F, 0.82F, 1.00F), 0.34F),
                SoundEvents.ENCHANTMENT_TABLE_USE, 1.40F),
        MIND_CONFUSION(new DustParticleOptions(new Vector3f(0.92F, 0.46F, 0.96F), 0.50F),
                new DustParticleOptions(new Vector3f(0.48F, 0.88F, 1.00F), 0.32F),
                SoundEvents.AMETHYST_BLOCK_RESONATE, 1.52F),
        VOID_STEP(new DustParticleOptions(new Vector3f(0.42F, 0.32F, 0.76F), 0.52F),
                new DustParticleOptions(new Vector3f(0.86F, 0.94F, 1.00F), 0.28F),
                SoundEvents.ENDERMAN_TELEPORT, 1.82F),
        DREAM_SNARE(new DustParticleOptions(new Vector3f(0.62F, 0.30F, 0.92F), 0.56F),
                new DustParticleOptions(new Vector3f(1.00F, 0.78F, 0.92F), 0.34F),
                SoundEvents.SCULK_CLICKING, 1.22F),
        CLONE_IMAGE(new DustParticleOptions(new Vector3f(0.66F, 0.82F, 1.00F), 0.42F),
                new DustParticleOptions(new Vector3f(1.00F, 0.92F, 0.72F), 0.30F),
                SoundEvents.ARMOR_EQUIP_LEATHER, 1.78F),
        YANYUE_PHANTOM_ARRAY(new DustParticleOptions(new Vector3f(0.62F, 0.70F, 1.00F), 0.58F),
                new DustParticleOptions(new Vector3f(1.00F, 0.82F, 0.98F), 0.36F),
                SoundEvents.BEACON_POWER_SELECT, 1.36F),
        VEIL_OF_MOON(new DustParticleOptions(new Vector3f(0.62F, 0.78F, 1.00F), 0.44F),
                new DustParticleOptions(new Vector3f(1.00F, 0.96F, 0.78F), 0.30F),
                SoundEvents.AMETHYST_BLOCK_CHIME, 1.32F),
        INVISIBILITY_BASIC(new DustParticleOptions(new Vector3f(0.72F, 0.72F, 0.82F), 0.36F),
                new DustParticleOptions(new Vector3f(0.96F, 0.96F, 1.00F), 0.24F),
                SoundEvents.ENCHANTMENT_TABLE_USE, 1.66F),
        ILLUSION_MIST(new DustParticleOptions(new Vector3f(0.58F, 0.74F, 0.92F), 0.46F),
                new DustParticleOptions(new Vector3f(0.92F, 0.82F, 1.00F), 0.30F),
                SoundEvents.BUBBLE_COLUMN_WHIRLPOOL_AMBIENT, 1.72F),
        INVERSE_STAR_VEIL(new DustParticleOptions(new Vector3f(0.32F, 0.44F, 0.94F), 0.50F),
                new DustParticleOptions(new Vector3f(0.98F, 0.94F, 0.54F), 0.32F),
                SoundEvents.RESPAWN_ANCHOR_CHARGE, 1.50F),
        YANYUE_MOON_ILLUSION(new DustParticleOptions(new Vector3f(0.70F, 0.78F, 1.00F), 0.48F),
                new DustParticleOptions(new Vector3f(1.00F, 0.86F, 0.98F), 0.32F),
                SoundEvents.AMETHYST_CLUSTER_HIT, 1.46F),
        WANHU_NINE_ILLUSION(new DustParticleOptions(new Vector3f(0.54F, 0.22F, 0.88F), 0.62F),
                new DustParticleOptions(new Vector3f(1.00F, 0.58F, 0.92F), 0.38F),
                SoundEvents.SCULK_SHRIEKER_SHRIEK, 1.42F);

        private final DustParticleOptions core;
        private final DustParticleOptions edge;
        private final SoundEvent sound;
        private final float pitch;

        IllusionForm(DustParticleOptions core, DustParticleOptions edge, SoundEvent sound, float pitch) {
            this.core = core;
            this.edge = edge;
            this.sound = sound;
            this.pitch = pitch;
        }

        private void applySingle(LivingEntity target, CultivationSkill skill) {
            int bonus = Math.max(0, skill.getLevel() - 1);
            switch (this) {
                case MIRROR_PHANTOM -> {
                    add(target, MobEffects.CONFUSION, 85 + bonus * 5, 0);
                    add(target, MobEffects.GLOWING, 65 + bonus * 4, 0);
                }
                case MIND_CONFUSION -> {
                    add(target, MobEffects.CONFUSION, 120 + bonus * 7, 0);
                    add(target, MobEffects.WEAKNESS, 80 + bonus * 5, 0);
                }
                case DREAM_SNARE -> {
                    add(target, MobEffects.CONFUSION, 100 + bonus * 6, 0);
                    add(target, MobEffects.MOVEMENT_SLOWDOWN, 95 + bonus * 6, 3);
                    add(target, MobEffects.BLINDNESS, 45 + bonus * 3, 0);
                }
                default -> {
                }
            }
        }

        private void applyArea(LivingEntity target, Vec3 center, CultivationSkill skill) {
            int bonus = Math.max(0, skill.getLevel() - 1);
            int confusion = this == WANHU_NINE_ILLUSION ? 135 : this == YANYUE_PHANTOM_ARRAY ? 120 : 95;
            add(target, MobEffects.CONFUSION, confusion + bonus * 7, 0);
            add(target, MobEffects.MOVEMENT_SLOWDOWN, 85 + bonus * 5, this == HUNDRED_ILLUSION ? 1 : 2);
            add(target, MobEffects.WEAKNESS, 75 + bonus * 5, this == WANHU_NINE_ILLUSION ? 1 : 0);
            if (this != HUNDRED_ILLUSION) {
                add(target, MobEffects.GLOWING, 75 + bonus * 5, 0);
            }
            Vec3 push = target.position().subtract(center);
            if (push.lengthSqr() > 0.001D) {
                double pull = this == YANYUE_PHANTOM_ARRAY ? -0.05D : 0.035D;
                target.push(push.normalize().x * pull, 0.03D, push.normalize().z * pull);
                target.hasImpulse = true;
            }
        }

        private void spawnSingle(ServerLevel level, Vec3 start, Vec3 end) {
            if (this == DREAM_SNARE) {
                beam(level, start, end, 0.16D, 8.0D);
                lockRings(level, end, 0.96D);
            } else if (this == MIND_CONFUSION) {
                beam(level, start, end, 0.11D, 6.0D);
                ring(level, end, 0.72D, 46, 0.05D);
            } else {
                mirrorBeam(level, start, end);
                mirrorShards(level, end, 1.05D);
            }
        }

        private void spawnArea(ServerLevel level, Vec3 center, double radius, List<LivingEntity> targets) {
            int layers = this == WANHU_NINE_ILLUSION ? 5 : this == YANYUE_PHANTOM_ARRAY ? 4 : 3;
            for (int layer = 0; layer < layers; layer++) {
                ring(level, center.add(0.0D, 0.18D + layer * 0.34D, 0.0D),
                        radius * (1.0D - layer * 0.12D), 84 - layer * 8, 0.08D);
            }
            int petals = this == WANHU_NINE_ILLUSION ? 9 : this == YANYUE_PHANTOM_ARRAY ? 8 : 6;
            for (int petal = 0; petal < petals; petal++) {
                double angle = Math.PI * 2.0D * petal / petals;
                crescent(level, center, radius, angle);
            }
            int links = Math.min(7, targets.size());
            for (int i = 0; i < links; i++) {
                LivingEntity target = targets.get(i);
                arc(level, center.add(0.0D, 1.05D, 0.0D),
                        target.position().add(0.0D, target.getBbHeight() * 0.58D, 0.0D), i * 29);
            }
        }

        private void spawnStep(ServerLevel level, Vec3 start, Vec3 end) {
            Vec3 line = end.subtract(start);
            int steps = Math.max(10, (int)(line.length() * 4.8D));
            for (int i = 0; i <= steps; i++) {
                double t = i / (double)steps;
                Vec3 point = start.lerp(end, t);
                level.sendParticles((i & 1) == 0 ? core : edge, point.x, point.y, point.z,
                        2, 0.10D, 0.26D, 0.10D, 0.006D);
                if (i % 5 == 0) {
                    silhouette(level, point, 0.85D, i);
                }
            }
        }

        private void spawnClones(ServerLevel level, Vec3 base, double height, double radius) {
            for (int clone = 0; clone < 5; clone++) {
                double angle = Math.PI * 2.0D * clone / 5.0D + 0.35D;
                Vec3 center = base.add(Math.cos(angle) * radius * 0.55D, 0.0D, Math.sin(angle) * radius * 0.55D);
                silhouette(level, center.add(0.0D, Math.max(0.75D, height * 0.45D), 0.0D), Math.max(1.1D, height), clone * 13);
            }
            ring(level, base.add(0.0D, 0.25D, 0.0D), radius * 0.78D, 72, 0.05D);
        }

        private void spawnVeil(ServerLevel level, Vec3 base, double height, double radius, int disturbed) {
            int layers = this == INVERSE_STAR_VEIL ? 4 : 3;
            for (int layer = 0; layer < layers; layer++) {
                ring(level, base.add(0.0D, 0.22D + layer * Math.max(0.32D, height / 4.5D), 0.0D),
                        radius * (0.38D + layer * 0.16D), 54 + layer * 10, 0.05D);
            }
            if (this == ILLUSION_MIST) {
                level.sendParticles(core, base.x, base.y + height * 0.48D, base.z, 42, radius * 0.42D, height * 0.28D, radius * 0.42D, 0.018D);
                level.sendParticles(edge, base.x, base.y + height * 0.55D, base.z, 28, radius * 0.50D, height * 0.22D, radius * 0.50D, 0.012D);
            } else {
                for (int spoke = 0; spoke < 7; spoke++) {
                    double angle = Math.PI * 2.0D * spoke / 7.0D;
                    crescent(level, base.add(0.0D, height * 0.62D, 0.0D), radius * 0.62D, angle);
                }
            }
            if (disturbed > 0) {
                ring(level, base.add(0.0D, 0.85D, 0.0D), radius, 82, 0.08D);
            }
        }

        private void mirrorBeam(ServerLevel level, Vec3 start, Vec3 end) {
            beam(level, start, end, 0.06D, 5.0D);
            Vec3 line = end.subtract(start);
            Vec3 side = side(line);
            Vec3 offset = side.scale(0.20D);
            beam(level, start.add(offset), end.add(offset), 0.02D, 2.0D);
            beam(level, start.subtract(offset), end.subtract(offset), 0.02D, 2.0D);
        }

        private void mirrorShards(ServerLevel level, Vec3 center, double radius) {
            for (int shard = 0; shard < 8; shard++) {
                double angle = Math.PI * 2.0D * shard / 8.0D;
                for (int i = 0; i < 6; i++) {
                    double t = i / 5.0D;
                    level.sendParticles((i & 1) == 0 ? core : edge,
                            center.x + Math.cos(angle) * radius * t,
                            center.y + 0.15D + t * 0.72D,
                            center.z + Math.sin(angle) * radius * t,
                            1, 0.014D, 0.014D, 0.014D, 0.0D);
                }
            }
        }

        private void lockRings(ServerLevel level, Vec3 center, double radius) {
            ring(level, center.add(0.0D, 0.22D, 0.0D), radius, 52, 0.03D);
            ring(level, center.add(0.0D, -0.14D, 0.0D), radius * 0.76D, 44, 0.03D);
        }

        private void beam(ServerLevel level, Vec3 start, Vec3 end, double coilRadius, double turns) {
            Vec3 line = end.subtract(start);
            Vec3 direction = line.lengthSqr() < 0.0001D ? new Vec3(0.0D, 0.0D, 1.0D) : line.normalize();
            Vec3 side = side(line);
            Vec3 up = side.cross(direction).normalize();
            int steps = Math.max(8, (int)(line.length() * 6.0D));
            for (int i = 0; i <= steps; i++) {
                double t = i / (double)steps;
                Vec3 point = start.lerp(end, t);
                double coil = t * Math.PI * turns;
                Vec3 swirl = side.scale(Math.sin(coil) * coilRadius).add(up.scale(Math.cos(coil) * coilRadius));
                level.sendParticles(core, point.x, point.y, point.z, 1, 0.014D, 0.014D, 0.014D, 0.0D);
                if ((i & 1) == 0) {
                    Vec3 edgePoint = point.add(swirl);
                    level.sendParticles(edge, edgePoint.x, edgePoint.y, edgePoint.z, 1, 0.016D, 0.016D, 0.016D, 0.0D);
                }
            }
            level.sendParticles(edge, end.x, end.y, end.z, 16, 0.16D, 0.12D, 0.16D, 0.006D);
        }

        private void crescent(ServerLevel level, Vec3 center, double radius, double angle) {
            Vec3 forward = new Vec3(Math.cos(angle), 0.0D, Math.sin(angle));
            Vec3 side = new Vec3(-forward.z, 0.0D, forward.x);
            for (int i = 0; i < 18; i++) {
                double t = (i / 17.0D - 0.5D) * Math.PI;
                Vec3 point = center.add(forward.scale(Math.cos(t) * radius * 0.38D))
                        .add(side.scale(Math.sin(t) * radius * 0.22D))
                        .add(0.0D, 0.22D + Math.cos(t) * 0.18D, 0.0D);
                level.sendParticles((i & 1) == 0 ? core : edge,
                        point.x, point.y, point.z, 1, 0.018D, 0.018D, 0.018D, 0.0D);
            }
        }

        private void silhouette(ServerLevel level, Vec3 center, double height, int seed) {
            for (int i = 0; i < 26; i++) {
                double t = i / 25.0D;
                double wave = Math.sin(seed + t * Math.PI * 4.0D) * 0.12D;
                level.sendParticles((i & 1) == 0 ? core : edge,
                        center.x + Math.sin(seed * 0.37D + i) * 0.20D + wave,
                        center.y - height * 0.45D + height * t,
                        center.z + Math.cos(seed * 0.41D + i) * 0.20D - wave,
                        1, 0.014D, 0.014D, 0.014D, 0.0D);
            }
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

        private void arc(ServerLevel level, Vec3 start, Vec3 end, int seed) {
            Vec3 path = end.subtract(start);
            int steps = Math.max(6, (int)(path.length() * 4.8D));
            for (int i = 0; i <= steps; i++) {
                double t = i / (double)steps;
                Vec3 point = start.lerp(end, t).add(Math.sin(seed + i * 1.19D) * 0.07D,
                        Math.cos(seed * 0.33D + i * 0.83D) * 0.06D,
                        Math.cos(seed + i * 1.11D) * 0.07D);
                level.sendParticles((i & 1) == 0 ? core : edge,
                        point.x, point.y, point.z, 1, 0.014D, 0.014D, 0.014D, 0.0D);
            }
        }

        private Vec3 side(Vec3 line) {
            Vec3 direction = line.lengthSqr() < 0.0001D ? new Vec3(0.0D, 0.0D, 1.0D) : line.normalize();
            Vec3 side = direction.cross(new Vec3(0.0D, 1.0D, 0.0D));
            return side.lengthSqr() < 0.0001D ? new Vec3(1.0D, 0.0D, 0.0D) : side.normalize();
        }
    }
}
