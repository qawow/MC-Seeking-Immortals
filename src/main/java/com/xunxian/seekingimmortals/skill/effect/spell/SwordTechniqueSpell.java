package com.xunxian.seekingimmortals.skill.effect.spell;

import com.xunxian.seekingimmortals.combat.status.StatusRegistry;
import com.xunxian.seekingimmortals.cultivation.PlayerCultivation;
import com.xunxian.seekingimmortals.entity.SwordProjectileEntity;
import com.xunxian.seekingimmortals.registry.ModItems;
import com.xunxian.seekingimmortals.skill.CultivationSkill;
import com.xunxian.seekingimmortals.skill.effect.SkillContext;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class SwordTechniqueSpell extends SpellEffect {
    private final double range;
    private final double radius;
    private final SwordForm form;
    private final String successKey;

    public SwordTechniqueSpell(int cost, int cooldownTicks, double damage, double range, double radius,
                               SwordForm form, String successKey) {
        super(cost, cooldownTicks, damage);
        this.range = range;
        this.radius = radius;
        this.form = form;
        this.successKey = successKey;
    }

    @Override
    public boolean execute(ServerPlayer player, PlayerCultivation cultivation, CultivationSkill skill, SkillContext context) {
        if (form.requiresSwordFocus && !hasSwordFocus(player)) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.spell.sword_focus.fail"), true);
            return false;
        }
        return switch (form) {
            case QINGYUAN_SWORD_RAY, GREEN_BAMBOO_SWORD_QI, INVISIBLE_SWORD -> castBeam(player, skill);
            case FLYING_SWORD_STRIKE -> castFlyingSwordStrike(player, skill);
            case SWORD_SHIELD -> castSwordShield(player, skill);
            case SWORD_ESCAPE -> castSwordEscape(player, skill);
            case THOUSAND_SWORD_ARRAY -> castSwordArray(player, skill);
            case BLOOD_SWORD_SLASH -> castBloodSlash(player, skill);
            case SWORD_MERGE -> castSwordMerge(player, skill);
            case SWORD_DOMAIN -> castSwordDomain(player, skill);
            case DUAL_SWORD_DANCE -> castDualSwordDance(player, skill);
        };
    }

    private boolean castBeam(ServerPlayer player, CultivationSkill skill) {
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
        List<LivingEntity> targets = findBeamTargets(level, player, start, end, radius);
        if (targets.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.spell.target.fail"), true);
            return false;
        }

        double damage = calculateDamage(skill.getLevel(), skill.getProficiency());
        int hitCount = 0;
        for (LivingEntity target : targets) {
            double along = target.position().subtract(start).dot(direction);
            double falloff = Math.max(0.58D, 1.0D - along / (range * 1.35D));
            target.hurt(player.damageSources().indirectMagic(player, player), (float)(damage * falloff));
            form.applyHitEffects(target, skill);
            hitCount++;
        }

        form.spawnBeam(level, start, end);
        level.playSound(null, player.blockPosition(), form.sound, SoundSource.PLAYERS, 0.76F, form.pitch);
        player.displayClientMessage(Component.translatable(successKey, hitCount), true);
        return true;
    }

    private boolean castFlyingSwordStrike(ServerPlayer player, CultivationSkill skill) {
        ServerLevel level = player.serverLevel();
        Vec3 direction = player.getLookAngle();
        if (direction.lengthSqr() < 0.001D) {
            return false;
        }

        direction = direction.normalize();
        double damage = calculateDamage(skill.getLevel(), skill.getProficiency());
        SwordProjectileEntity projectile = new SwordProjectileEntity(level, player, direction, damage, true);
        level.addFreshEntity(projectile);
        form.spawnFlight(level, player.getEyePosition().add(direction.scale(0.45D)), direction, 16);
        level.playSound(null, player.blockPosition(), form.sound, SoundSource.PLAYERS, 0.72F, form.pitch);
        player.displayClientMessage(Component.translatable(successKey, Math.round(damage)), true);
        return true;
    }

    private boolean castSwordShield(ServerPlayer player, CultivationSkill skill) {
        ServerLevel level = player.serverLevel();
        int duration = scaleDuration(160, skill, 12);
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, duration, 0, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, duration, Math.max(0, skill.getLevel() / 6), false, true));
        int deflected = deflectProjectiles(player, level);
        form.spawnShield(level, player, radius);
        level.playSound(null, player.blockPosition(), form.sound, SoundSource.PLAYERS, 0.70F, form.pitch);
        player.displayClientMessage(Component.translatable(successKey, deflected), true);
        return true;
    }

    private boolean castSwordEscape(ServerPlayer player, CultivationSkill skill) {
        ServerLevel level = player.serverLevel();
        Vec3 flat = new Vec3(player.getLookAngle().x, 0.0D, player.getLookAngle().z);
        if (flat.lengthSqr() < 0.001D) {
            return false;
        }
        flat = flat.normalize();

        Vec3 origin = player.position();
        Vec3 destination = findSafeEscapeDestination(level, player, origin, flat);
        if (destination == null) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.spell.sword_escape.fail"), true);
            return false;
        }

        double damage = calculateDamage(skill.getLevel(), skill.getProficiency());
        List<LivingEntity> targets = findLineTargets(level, player, origin.add(0.0D, 0.8D, 0.0D), destination.add(0.0D, 0.8D, 0.0D), radius);
        for (LivingEntity target : targets) {
            target.hurt(player.damageSources().indirectMagic(player, player), (float)damage);
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, scaleDuration(50, skill, 4), 1, false, true));
        }

        form.spawnEscapeTrail(level, origin.add(0.0D, 0.75D, 0.0D), destination.add(0.0D, 0.75D, 0.0D));
        level.playSound(null, player.blockPosition(), form.sound, SoundSource.PLAYERS, 0.70F, form.pitch);
        player.teleportTo(destination.x, destination.y, destination.z);
        player.fallDistance = 0.0F;
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, scaleDuration(60, skill, 4), 0, false, true));
        level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_CLUSTER_STEP, SoundSource.PLAYERS, 0.58F, 1.65F);
        player.displayClientMessage(Component.translatable(successKey, targets.size()), true);
        return true;
    }

    private boolean castSwordArray(ServerPlayer player, CultivationSkill skill) {
        ServerLevel level = player.serverLevel();
        Vec3 center = findLookPoint(level, player, range);
        List<LivingEntity> targets = findAreaTargets(level, player, center, radius);
        if (targets.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.spell.area.fail"), true);
            return false;
        }

        double damage = calculateDamage(skill.getLevel(), skill.getProficiency());
        for (LivingEntity target : targets) {
            double distance = Math.sqrt(target.distanceToSqr(center));
            double falloff = Math.max(0.50D, 1.0D - distance / (radius * 1.65D));
            target.hurt(player.damageSources().indirectMagic(player, player), (float)(damage * falloff));
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, scaleDuration(90, skill, 5), 2, false, true));
        }

        form.spawnSwordRain(level, center, radius);
        level.playSound(null, BlockPos.containing(center), form.sound, SoundSource.PLAYERS, 0.86F, form.pitch);
        player.displayClientMessage(Component.translatable(successKey, targets.size()), true);
        return true;
    }

    private boolean castBloodSlash(ServerPlayer player, CultivationSkill skill) {
        ServerLevel level = player.serverLevel();
        Vec3 direction = player.getLookAngle();
        if (direction.lengthSqr() < 0.001D) {
            return false;
        }
        direction = direction.normalize();
        Vec3 start = player.getEyePosition().add(direction.scale(0.45D));
        Vec3 end = start.add(direction.scale(range));
        List<LivingEntity> targets = findLineTargets(level, player, start, end, radius);
        if (targets.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.spell.target.fail"), true);
            return false;
        }

        double damage = calculateDamage(skill.getLevel(), skill.getProficiency());
        for (LivingEntity target : targets) {
            target.hurt(player.damageSources().indirectMagic(player, player), (float)damage);
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, scaleDuration(70, skill, 5), 0, false, true));
        }
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, scaleDuration(45, skill, 3), 0, false, true));
        form.spawnBeam(level, start, end);
        level.playSound(null, player.blockPosition(), form.sound, SoundSource.PLAYERS, 0.82F, form.pitch);
        player.displayClientMessage(Component.translatable(successKey, targets.size()), true);
        return true;
    }

    private boolean castSwordMerge(ServerPlayer player, CultivationSkill skill) {
        ServerLevel level = player.serverLevel();
        int duration = scaleDuration(180, skill, 12);
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, duration, 0, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, duration, 0, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, duration, 0, false, true));
        StatusRegistry.applyStatus(player, "sword_intent", 0, duration);
        form.spawnMergeAura(level, player, radius);
        level.playSound(null, player.blockPosition(), form.sound, SoundSource.PLAYERS, 0.72F, form.pitch);
        player.displayClientMessage(Component.translatable(successKey, Math.max(1, duration / 20)), true);
        return true;
    }

    private boolean castSwordDomain(ServerPlayer player, CultivationSkill skill) {
        ServerLevel level = player.serverLevel();
        Vec3 center = player.position().add(0.0D, 0.75D, 0.0D);
        List<LivingEntity> targets = findAreaTargets(level, player, center, radius);
        if (targets.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.spell.area.fail"), true);
            return false;
        }

        double damage = calculateDamage(skill.getLevel(), skill.getProficiency());
        for (LivingEntity target : targets) {
            double distance = Math.sqrt(target.distanceToSqr(center));
            double falloff = Math.max(0.55D, 1.0D - distance / (radius * 1.8D));
            target.hurt(player.damageSources().indirectMagic(player, player), (float)(damage * falloff));
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, scaleDuration(95, skill, 6), 2, false, true));
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, scaleDuration(80, skill, 5), 0, false, true));
        }

        form.spawnDomain(level, center, radius);
        level.playSound(null, player.blockPosition(), form.sound, SoundSource.PLAYERS, 0.76F, form.pitch);
        player.displayClientMessage(Component.translatable(successKey, targets.size()), true);
        return true;
    }

    private boolean castDualSwordDance(ServerPlayer player, CultivationSkill skill) {
        ServerLevel level = player.serverLevel();
        Vec3 direction = player.getLookAngle();
        if (direction.lengthSqr() < 0.001D) {
            return false;
        }
        direction = direction.normalize();
        Vec3 side = direction.cross(new Vec3(0.0D, 1.0D, 0.0D));
        if (side.lengthSqr() < 0.001D) {
            side = new Vec3(1.0D, 0.0D, 0.0D);
        } else {
            side = side.normalize();
        }

        Vec3 start = player.getEyePosition().add(direction.scale(0.45D));
        Vec3 leftEnd = start.add(direction.scale(range)).add(side.scale(2.4D));
        Vec3 rightEnd = start.add(direction.scale(range)).subtract(side.scale(2.4D));
        LinkedHashSet<LivingEntity> targets = new LinkedHashSet<>();
        targets.addAll(findLineTargets(level, player, start.subtract(side.scale(0.8D)), leftEnd, radius));
        targets.addAll(findLineTargets(level, player, start.add(side.scale(0.8D)), rightEnd, radius));
        if (targets.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.spell.target.fail"), true);
            return false;
        }

        double damage = calculateDamage(skill.getLevel(), skill.getProficiency());
        for (LivingEntity target : targets) {
            target.hurt(player.damageSources().indirectMagic(player, player), (float)damage);
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, scaleDuration(45, skill, 4), 0, false, true));
        }
        form.spawnDualDance(level, start, leftEnd, rightEnd);
        level.playSound(null, player.blockPosition(), form.sound, SoundSource.PLAYERS, 0.82F, form.pitch);
        player.displayClientMessage(Component.translatable(successKey, targets.size()), true);
        return true;
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

    private List<LivingEntity> findBeamTargets(ServerLevel level, ServerPlayer player, Vec3 start, Vec3 end, double maxRadius) {
        return findLineTargets(level, player, start, end, maxRadius).stream()
                .sorted(Comparator.comparingDouble(entity -> entity.distanceToSqr(player)))
                .toList();
    }

    private List<LivingEntity> findLineTargets(ServerLevel level, ServerPlayer player, Vec3 start, Vec3 end, double maxRadius) {
        Vec3 line = end.subtract(start);
        AABB box = new AABB(start, end).inflate(maxRadius, maxRadius + 0.5D, maxRadius);
        return level.getEntitiesOfClass(LivingEntity.class, box,
                entity -> canTarget(entity, player)
                        && distanceToSegment(entity.position().add(0.0D, entity.getBbHeight() * 0.52D, 0.0D), start, line) <= maxRadius);
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

    private List<LivingEntity> findAreaTargets(ServerLevel level, ServerPlayer player, Vec3 center, double maxRadius) {
        AABB area = new AABB(center, center).inflate(maxRadius, maxRadius * 0.70D, maxRadius);
        return level.getEntitiesOfClass(LivingEntity.class, area,
                entity -> canTarget(entity, player) && entity.distanceToSqr(center) <= maxRadius * maxRadius);
    }

    private int deflectProjectiles(ServerPlayer player, ServerLevel level) {
        AABB area = player.getBoundingBox().inflate(radius, radius * 0.7D, radius);
        List<Projectile> projectiles = level.getEntitiesOfClass(Projectile.class, area,
                projectile -> projectile.isAlive() && projectile.getOwner() != player);
        int changed = 0;
        for (Projectile projectile : projectiles) {
            Vec3 direction = projectile.position().subtract(player.position());
            if (direction.lengthSqr() < 0.001D) {
                direction = player.getLookAngle();
            }
            projectile.setOwner(player);
            projectile.setDeltaMovement(direction.normalize().scale(Math.max(0.48D, projectile.getDeltaMovement().length() + 0.12D)).add(0.0D, 0.04D, 0.0D));
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

    private static boolean hasSwordFocus(ServerPlayer player) {
        return isSwordFocus(player.getMainHandItem()) || isSwordFocus(player.getOffhandItem());
    }

    private static boolean isSwordFocus(ItemStack stack) {
        return stack.is(ModItems.FLYING_SWORD.get())
                || stack.is(ModItems.FLYING_SWORD_LOW.get())
                || stack.is(ModItems.SILVER_GIANT_SWORD.get());
    }

    private static double distanceToSegment(Vec3 point, Vec3 start, Vec3 line) {
        double lengthSqr = line.lengthSqr();
        if (lengthSqr < 0.0001D) {
            return point.distanceTo(start);
        }
        double t = Math.max(0.0D, Math.min(1.0D, point.subtract(start).dot(line) / lengthSqr));
        return point.distanceTo(start.add(line.scale(t)));
    }

    private static int scaleDuration(int baseTicks, CultivationSkill skill, int perLevelTicks) {
        return baseTicks + Math.max(0, skill.getLevel() - 1) * perLevelTicks;
    }

    public enum SwordForm {
        QINGYUAN_SWORD_RAY(true,
                new DustParticleOptions(new Vector3f(0.68F, 1.00F, 0.92F), 0.55F),
                new DustParticleOptions(new Vector3f(0.96F, 0.92F, 0.58F), 0.38F),
                SoundEvents.TRIDENT_THROW, 1.52F),
        FLYING_SWORD_STRIKE(true,
                new DustParticleOptions(new Vector3f(0.88F, 0.95F, 1.00F), 0.48F),
                new DustParticleOptions(new Vector3f(0.54F, 0.76F, 1.00F), 0.36F),
                SoundEvents.TRIDENT_THROW, 1.72F),
        GREEN_BAMBOO_SWORD_QI(false,
                new DustParticleOptions(new Vector3f(0.30F, 0.96F, 0.42F), 0.56F),
                new DustParticleOptions(new Vector3f(0.76F, 1.00F, 0.70F), 0.34F),
                SoundEvents.BAMBOO_BREAK, 1.35F),
        SWORD_SHIELD(false,
                new DustParticleOptions(new Vector3f(0.92F, 0.96F, 1.00F), 0.48F),
                new DustParticleOptions(new Vector3f(0.98F, 0.88F, 0.44F), 0.38F),
                SoundEvents.SHIELD_BLOCK, 1.28F),
        SWORD_ESCAPE(false,
                new DustParticleOptions(new Vector3f(0.74F, 0.92F, 1.00F), 0.44F),
                new DustParticleOptions(new Vector3f(0.98F, 0.98F, 0.72F), 0.36F),
                SoundEvents.PLAYER_ATTACK_SWEEP, 1.45F),
        THOUSAND_SWORD_ARRAY(true,
                new DustParticleOptions(new Vector3f(0.88F, 0.94F, 1.00F), 0.48F),
                new DustParticleOptions(new Vector3f(1.00F, 0.84F, 0.34F), 0.34F),
                SoundEvents.ENCHANTMENT_TABLE_USE, 1.35F),
        BLOOD_SWORD_SLASH(true,
                new DustParticleOptions(new Vector3f(0.86F, 0.05F, 0.08F), 0.56F),
                new DustParticleOptions(new Vector3f(1.00F, 0.38F, 0.26F), 0.36F),
                SoundEvents.PLAYER_ATTACK_SWEEP, 0.88F),
        SWORD_MERGE(true,
                new DustParticleOptions(new Vector3f(0.82F, 0.94F, 1.00F), 0.45F),
                new DustParticleOptions(new Vector3f(1.00F, 0.96F, 0.62F), 0.34F),
                SoundEvents.AMETHYST_CLUSTER_BREAK, 1.55F),
        INVISIBLE_SWORD(true,
                new DustParticleOptions(new Vector3f(0.86F, 0.98F, 1.00F), 0.30F),
                new DustParticleOptions(new Vector3f(1.00F, 1.00F, 0.92F), 0.22F),
                SoundEvents.TRIDENT_THROW, 1.82F),
        SWORD_DOMAIN(true,
                new DustParticleOptions(new Vector3f(0.56F, 0.82F, 1.00F), 0.46F),
                new DustParticleOptions(new Vector3f(1.00F, 0.90F, 0.38F), 0.34F),
                SoundEvents.ENCHANTMENT_TABLE_USE, 1.62F),
        DUAL_SWORD_DANCE(true,
                new DustParticleOptions(new Vector3f(0.76F, 0.92F, 1.00F), 0.46F),
                new DustParticleOptions(new Vector3f(1.00F, 0.82F, 0.36F), 0.34F),
                SoundEvents.PLAYER_ATTACK_SWEEP, 1.28F);

        private final boolean requiresSwordFocus;
        private final DustParticleOptions core;
        private final DustParticleOptions edge;
        private final SoundEvent sound;
        private final float pitch;

        SwordForm(boolean requiresSwordFocus, DustParticleOptions core, DustParticleOptions edge, SoundEvent sound, float pitch) {
            this.requiresSwordFocus = requiresSwordFocus;
            this.core = core;
            this.edge = edge;
            this.sound = sound;
            this.pitch = pitch;
        }

        private void applyHitEffects(LivingEntity target, CultivationSkill skill) {
            int levelBonus = Math.max(0, skill.getLevel() - 1);
            switch (this) {
                case QINGYUAN_SWORD_RAY -> target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 55 + levelBonus * 4, 0, false, true));
                case GREEN_BAMBOO_SWORD_QI -> target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 70 + levelBonus * 5, 1, false, true));
                case INVISIBLE_SWORD -> target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 45 + levelBonus * 3, 0, false, true));
                default -> {
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
                double bladeWave = Math.sin(t * Math.PI * 9.0D) * 0.10D;
                Vec3 edgePoint = point.add(side.scale(bladeWave)).add(up.scale(Math.cos(t * Math.PI * 7.0D) * 0.06D));
                level.sendParticles(core, point.x, point.y, point.z, 1, 0.012D, 0.012D, 0.012D, 0.0D);
                if ((i & 1) == 0) {
                    level.sendParticles(edge, edgePoint.x, edgePoint.y, edgePoint.z, 1, 0.026D, 0.026D, 0.026D, 0.0D);
                }
            }
            level.sendParticles(edge, end.x, end.y, end.z, 18, 0.14D, 0.14D, 0.14D, 0.012D);
        }

        private void spawnFlight(ServerLevel level, Vec3 start, Vec3 direction, int steps) {
            Vec3 side = direction.cross(new Vec3(0.0D, 1.0D, 0.0D));
            if (side.lengthSqr() < 0.0001D) {
                side = new Vec3(1.0D, 0.0D, 0.0D);
            } else {
                side = side.normalize();
            }
            for (int i = 0; i < steps; i++) {
                double t = i / (double)Math.max(1, steps - 1);
                Vec3 point = start.add(direction.scale(t * 3.0D)).add(side.scale(Math.sin(t * Math.PI * 3.0D) * 0.10D));
                level.sendParticles((i & 1) == 0 ? core : edge, point.x, point.y, point.z, 1, 0.025D, 0.025D, 0.025D, 0.0D);
            }
        }

        private void spawnShield(ServerLevel level, ServerPlayer player, double radius) {
            Vec3 base = player.position();
            for (int layer = 0; layer < 3; layer++) {
                ring(level, base.add(0.0D, 0.45D + layer * 0.46D, 0.0D), 0.76D + layer * 0.12D, 50, 0.035D);
            }
            for (int blade = 0; blade < 8; blade++) {
                double angle = Math.PI * 2.0D * blade / 8.0D;
                Vec3 lower = base.add(Math.cos(angle) * 0.92D, 0.35D, Math.sin(angle) * 0.92D);
                Vec3 upper = base.add(Math.cos(angle) * 0.66D, 1.75D, Math.sin(angle) * 0.66D);
                arc(level, lower, upper, blade * 17);
            }
            level.sendParticles(core, base.x, base.y + 1.0D, base.z, 12, radius * 0.10D, 0.42D, radius * 0.10D, 0.01D);
        }

        private void spawnEscapeTrail(ServerLevel level, Vec3 start, Vec3 end) {
            spawnBeam(level, start, end);
            Vec3 line = end.subtract(start);
            int afterimages = Math.max(3, (int)(line.length() / 2.0D));
            for (int i = 0; i <= afterimages; i++) {
                double t = i / (double)Math.max(1, afterimages);
                Vec3 point = start.lerp(end, t);
                ring(level, point, 0.28D + 0.04D * (i % 2), 18, 0.02D);
            }
            level.sendParticles(edge, end.x, end.y + 0.25D, end.z, 20, 0.26D, 0.26D, 0.26D, 0.018D);
        }

        private void spawnSwordRain(ServerLevel level, Vec3 center, double radius) {
            for (int blade = 0; blade < 14; blade++) {
                double angle = Math.PI * 2.0D * blade / 14.0D;
                Vec3 start = center.add(Math.cos(angle) * radius * 0.78D, 2.35D + (blade % 3) * 0.18D, Math.sin(angle) * radius * 0.78D);
                Vec3 end = center.add(Math.cos(angle + 0.55D) * radius * 0.30D, 0.25D, Math.sin(angle + 0.55D) * radius * 0.30D);
                arc(level, start, end, blade * 29);
            }
            for (int layer = 0; layer < 3; layer++) {
                ring(level, center.add(0.0D, 0.22D + layer * 0.30D, 0.0D), radius * (0.45D + layer * 0.18D), 56, 0.045D);
            }
            level.sendParticles(edge, center.x, center.y + 0.45D, center.z, 28, radius * 0.30D, 0.26D, radius * 0.30D, 0.018D);
        }

        private void spawnMergeAura(ServerLevel level, ServerPlayer player, double radius) {
            Vec3 base = player.position();
            for (int layer = 0; layer < 4; layer++) {
                ring(level, base.add(0.0D, 0.35D + layer * 0.38D, 0.0D), 0.45D + layer * radius * 0.10D, 34, 0.028D);
            }
            Vec3 eye = player.getEyePosition();
            Vec3 look = player.getLookAngle().normalize();
            spawnBeam(level, eye.subtract(look.scale(1.4D)), eye.add(look.scale(1.4D)));
            level.sendParticles(core, base.x, base.y + 1.0D, base.z, 16, 0.28D, 0.56D, 0.28D, 0.012D);
        }

        private void spawnDomain(ServerLevel level, Vec3 center, double radius) {
            for (int layer = 0; layer < 4; layer++) {
                ring(level, center.add(0.0D, 0.08D + layer * 0.36D, 0.0D), radius * (0.42D + layer * 0.16D), 72, 0.050D);
            }
            for (int blade = 0; blade < 12; blade++) {
                double angle = Math.PI * 2.0D * blade / 12.0D;
                Vec3 lower = center.add(Math.cos(angle) * radius * 0.92D, 0.12D, Math.sin(angle) * radius * 0.92D);
                Vec3 upper = center.add(Math.cos(angle + 0.24D) * radius * 0.45D, 1.95D, Math.sin(angle + 0.24D) * radius * 0.45D);
                arc(level, lower, upper, blade * 41);
            }
            level.sendParticles(edge, center.x, center.y + 0.65D, center.z, 34, radius * 0.38D, 0.45D, radius * 0.38D, 0.015D);
        }

        private void spawnDualDance(ServerLevel level, Vec3 start, Vec3 leftEnd, Vec3 rightEnd) {
            spawnBeam(level, start, leftEnd);
            spawnBeam(level, start, rightEnd);
            Vec3 center = start.lerp(leftEnd.lerp(rightEnd, 0.5D), 0.55D);
            ring(level, center, 0.55D, 24, 0.025D);
            level.sendParticles(edge, center.x, center.y, center.z, 18, 0.24D, 0.18D, 0.24D, 0.012D);
        }

        private void ring(ServerLevel level, Vec3 center, double radius, int points, double wave) {
            for (int i = 0; i < points; i++) {
                double angle = Math.PI * 2.0D * i / points;
                level.sendParticles((i & 1) == 0 ? core : edge,
                        center.x + Math.cos(angle) * radius,
                        center.y + Math.sin(angle * 3.0D) * wave,
                        center.z + Math.sin(angle) * radius,
                        1, 0.014D, 0.014D, 0.014D, 0.0D);
            }
        }

        private void arc(ServerLevel level, Vec3 start, Vec3 end, int seed) {
            Vec3 path = end.subtract(start);
            int steps = Math.max(6, (int)(path.length() * 5.0D));
            for (int i = 0; i <= steps; i++) {
                double t = i / (double)steps;
                Vec3 point = start.lerp(end, t).add(Math.sin(seed + i * 1.17D) * 0.04D,
                        Math.cos(seed * 0.29D + i) * 0.04D,
                        Math.cos(seed + i * 1.13D) * 0.04D);
                level.sendParticles((i & 1) == 0 ? core : edge,
                        point.x, point.y, point.z, 1, 0.012D, 0.012D, 0.012D, 0.0D);
            }
        }
    }
}
