package com.xunxian.seekingimmortals.skill.effect.spell;

import com.xunxian.seekingimmortals.cultivation.PlayerCultivation;
import com.xunxian.seekingimmortals.skill.CultivationSkill;
import com.xunxian.seekingimmortals.skill.effect.SkillContext;
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
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class FoundationElementalUtilitySpell extends SpellEffect {
    private final double range;
    private final double radius;
    private final UtilityElement element;
    private final String successKey;

    public FoundationElementalUtilitySpell(int cost, int cooldownTicks, double damage, double range, double radius,
                                           UtilityElement element, String successKey) {
        super(cost, cooldownTicks, damage);
        this.range = range;
        this.radius = radius;
        this.element = element;
        this.successKey = successKey;
    }

    @Override
    public boolean execute(ServerPlayer player, PlayerCultivation cultivation, CultivationSkill skill, SkillContext context) {
        return switch (element) {
            case ICE_JADE_SHIELD -> castIceJadeShield(player, skill);
            case WOOD_SPIRIT_VINE -> castWoodSpiritVine(player, skill);
            case WATER_MIRROR_REFLECT -> castWaterMirrorReflect(player, skill);
        };
    }

    private boolean castIceJadeShield(ServerPlayer player, CultivationSkill skill) {
        ServerLevel level = player.serverLevel();
        int duration = scaleDuration(180, skill, 15);
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, duration, Math.max(0, skill.getLevel() / 6), false, true));
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, duration, 0, false, true));
        int deflected = deflectProjectiles(player, level, false);
        spawnIceShield(level, player);
        level.playSound(null, player.blockPosition(), element.sound, SoundSource.PLAYERS, 0.78F, element.pitch);
        player.displayClientMessage(Component.translatable(successKey, deflected), true);
        return true;
    }

    private boolean castWoodSpiritVine(ServerPlayer player, CultivationSkill skill) {
        ServerLevel level = player.serverLevel();
        LivingEntity target = findTarget(level, player);
        if (target == null) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.spell.target.fail"), true);
            return false;
        }

        double damage = calculateDamage(skill.getLevel(), skill.getProficiency());
        if (damage > 0.0D) {
            target.hurt(player.damageSources().magic(), (float) damage);
        }
        add(target, MobEffects.MOVEMENT_SLOWDOWN, 130, 4, skill);
        add(target, MobEffects.DIG_SLOWDOWN, 100, 1, skill);
        add(target, MobEffects.WEAKNESS, 90, 0, skill);
        target.setDeltaMovement(target.getDeltaMovement().multiply(0.25D, 0.0D, 0.25D));
        target.hasImpulse = true;

        spawnWoodVines(level, target);
        level.playSound(null, target.blockPosition(), element.sound, SoundSource.PLAYERS, 0.74F, element.pitch);
        player.displayClientMessage(Component.translatable(successKey, target.getDisplayName()), true);
        return true;
    }

    private boolean castWaterMirrorReflect(ServerPlayer player, CultivationSkill skill) {
        ServerLevel level = player.serverLevel();
        int duration = scaleDuration(160, skill, 12);
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, duration, 0, false, true));
        int reflected = deflectProjectiles(player, level, true);
        spawnWaterMirror(level, player);
        level.playSound(null, player.blockPosition(), element.sound, SoundSource.PLAYERS, 0.72F, element.pitch);
        player.displayClientMessage(Component.translatable(successKey, reflected), true);
        return true;
    }

    private LivingEntity findTarget(ServerLevel level, ServerPlayer player) {
        Vec3 start = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        if (look.lengthSqr() < 0.001D) {
            return null;
        }
        Vec3 end = start.add(look.normalize().scale(range));
        BlockHitResult blockHit = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 traceEnd = blockHit.getType() == HitResult.Type.MISS ? end : blockHit.getLocation();
        AABB searchBox = new AABB(start, traceEnd).inflate(radius);
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(level, player, start, traceEnd, searchBox,
                entity -> entity != player && entity instanceof LivingEntity living && living.isAlive() && !entity.isSpectator());
        if (entityHit == null) {
            return null;
        }
        Entity entity = entityHit.getEntity();
        return entity instanceof LivingEntity living ? living : null;
    }

    private int deflectProjectiles(ServerPlayer player, ServerLevel level, boolean reflectTowardOwner) {
        AABB area = player.getBoundingBox().inflate(radius, radius * 0.65D, radius);
        List<Projectile> projectiles = level.getEntitiesOfClass(Projectile.class, area,
                projectile -> projectile.isAlive() && projectile.getOwner() != player);
        int changed = 0;
        for (Projectile projectile : projectiles) {
            Vec3 direction = null;
            Entity owner = projectile.getOwner();
            if (reflectTowardOwner && owner instanceof LivingEntity living && living.isAlive()) {
                direction = living.getEyePosition().subtract(projectile.position());
                projectile.setOwner(player);
            }
            if (direction == null || direction.lengthSqr() < 0.001D) {
                direction = projectile.position().subtract(player.position());
            }
            if (direction.lengthSqr() < 0.001D) {
                direction = player.getLookAngle();
            }
            double speed = Math.max(0.42D, projectile.getDeltaMovement().length() + (reflectTowardOwner ? 0.18D : 0.08D));
            projectile.setDeltaMovement(direction.normalize().scale(speed).add(0.0D, reflectTowardOwner ? 0.02D : 0.08D, 0.0D));
            projectile.hasImpulse = true;
            changed++;
        }
        return changed;
    }

    private void spawnIceShield(ServerLevel level, ServerPlayer player) {
        Vec3 base = player.position();
        for (int layer = 0; layer < 3; layer++) {
            ring(level, base.add(0.0D, 0.45D + layer * 0.55D, 0.0D), 0.85D - layer * 0.08D, 52, 0.04D);
        }
        for (int shard = 0; shard < 14; shard++) {
            double angle = Math.PI * 2.0D * shard / 14.0D;
            double x = base.x + Math.cos(angle) * 0.92D;
            double z = base.z + Math.sin(angle) * 0.92D;
            for (int step = 0; step < 5; step++) {
                level.sendParticles((step & 1) == 0 ? element.core : element.edge,
                        x, base.y + 0.35D + step * 0.32D, z, 1, 0.018D, 0.018D, 0.018D, 0.0D);
            }
        }
    }

    private void spawnWoodVines(ServerLevel level, LivingEntity target) {
        Vec3 base = target.position();
        double height = Math.max(1.2D, target.getBbHeight());
        for (int strand = 0; strand < 4; strand++) {
            double offset = strand * Math.PI * 0.5D;
            for (int i = 0; i < 24; i++) {
                double t = i / 23.0D;
                double angle = offset + t * Math.PI * 3.6D;
                double vineRadius = 0.34D + 0.10D * Math.sin(t * Math.PI);
                level.sendParticles((i & 1) == 0 ? element.core : element.edge,
                        base.x + Math.cos(angle) * vineRadius,
                        base.y + 0.12D + t * height * 0.82D,
                        base.z + Math.sin(angle) * vineRadius,
                        1, 0.025D, 0.025D, 0.025D, 0.0D);
            }
        }
        ring(level, base.add(0.0D, 0.16D, 0.0D), 0.72D, 42, 0.02D);
    }

    private void spawnWaterMirror(ServerLevel level, ServerPlayer player) {
        Vec3 look = player.getLookAngle();
        Vec3 forward = new Vec3(look.x, 0.0D, look.z);
        if (forward.lengthSqr() < 0.001D) {
            forward = new Vec3(0.0D, 0.0D, 1.0D);
        }
        forward = forward.normalize();
        Vec3 right = new Vec3(-forward.z, 0.0D, forward.x);
        Vec3 up = new Vec3(0.0D, 1.0D, 0.0D);
        Vec3 center = player.getEyePosition().add(forward.scale(1.05D)).subtract(0.0D, 0.2D, 0.0D);
        for (int i = 0; i < 72; i++) {
            double angle = Math.PI * 2.0D * i / 72.0D;
            Vec3 point = center.add(right.scale(Math.cos(angle) * 0.82D)).add(up.scale(Math.sin(angle) * 1.02D));
            level.sendParticles((i & 1) == 0 ? element.core : element.edge,
                    point.x, point.y, point.z, 1, 0.018D, 0.018D, 0.018D, 0.0D);
        }
        for (int ripple = 0; ripple < 3; ripple++) {
            double rippleRadius = 0.22D + ripple * 0.18D;
            for (int i = 0; i < 32; i++) {
                double angle = Math.PI * 2.0D * i / 32.0D + ripple * 0.28D;
                Vec3 point = center.add(right.scale(Math.cos(angle) * rippleRadius)).add(up.scale(Math.sin(angle) * rippleRadius * 0.62D));
                level.sendParticles(element.core, point.x, point.y, point.z, 1, 0.012D, 0.012D, 0.012D, 0.0D);
            }
        }
    }

    private void ring(ServerLevel level, Vec3 center, double radius, int points, double wave) {
        for (int i = 0; i < points; i++) {
            double angle = Math.PI * 2.0D * i / points;
            level.sendParticles((i & 1) == 0 ? element.core : element.edge,
                    center.x + Math.cos(angle) * radius,
                    center.y + Math.sin(angle * 3.0D) * wave,
                    center.z + Math.sin(angle) * radius,
                    1, 0.018D, 0.018D, 0.018D, 0.0D);
        }
    }

    private static void add(LivingEntity target, MobEffect effect, int durationTicks, int amplifier, CultivationSkill skill) {
        target.addEffect(new MobEffectInstance(effect, scaleDuration(durationTicks, skill, 10),
                amplifier + Math.max(0, skill.getLevel() / 6), false, true));
    }

    private static int scaleDuration(int baseTicks, CultivationSkill skill, int perLevelTicks) {
        return baseTicks + Math.max(0, skill.getLevel() - 1) * perLevelTicks;
    }

    public enum UtilityElement {
        ICE_JADE_SHIELD(new DustParticleOptions(new Vector3f(0.50F, 0.86F, 1.00F), 0.70F),
                new DustParticleOptions(new Vector3f(0.94F, 1.00F, 1.00F), 0.40F),
                SoundEvents.GLASS_BREAK, 1.45F),
        WOOD_SPIRIT_VINE(new DustParticleOptions(new Vector3f(0.26F, 0.86F, 0.34F), 0.76F),
                new DustParticleOptions(new Vector3f(0.48F, 0.30F, 0.12F), 0.50F),
                SoundEvents.GRASS_BREAK, 0.92F),
        WATER_MIRROR_REFLECT(new DustParticleOptions(new Vector3f(0.26F, 0.62F, 1.00F), 0.66F),
                new DustParticleOptions(new Vector3f(0.82F, 0.96F, 1.00F), 0.36F),
                SoundEvents.BUCKET_FILL, 1.18F);

        private final DustParticleOptions core;
        private final DustParticleOptions edge;
        private final SoundEvent sound;
        private final float pitch;

        UtilityElement(DustParticleOptions core, DustParticleOptions edge, SoundEvent sound, float pitch) {
            this.core = core;
            this.edge = edge;
            this.sound = sound;
            this.pitch = pitch;
        }
    }
}
