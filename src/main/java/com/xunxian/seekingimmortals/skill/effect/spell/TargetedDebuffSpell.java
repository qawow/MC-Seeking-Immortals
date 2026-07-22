package com.xunxian.seekingimmortals.skill.effect.spell;

import com.xunxian.seekingimmortals.cultivation.PlayerCultivation;
import com.xunxian.seekingimmortals.network.TechniqueVfxPacket;
import com.xunxian.seekingimmortals.skill.CultivationSkill;
import com.xunxian.seekingimmortals.skill.effect.ActiveTechniqueEffectVfxService;
import com.xunxian.seekingimmortals.skill.effect.SkillContext;
import com.xunxian.seekingimmortals.skill.effect.TechniqueLifecycleVfxService;
import com.xunxian.seekingimmortals.skill.effect.TechniqueVfxPalette;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class TargetedDebuffSpell extends SpellEffect {
    private final double range;
    private final MobEffect primaryEffect;
    private final int primaryDurationTicks;
    private final int primaryAmplifier;
    private final MobEffect secondaryEffect;
    private final int secondaryDurationTicks;
    private final int secondaryAmplifier;
    private final ParticleOptions particle;
    private final SoundEvent sound;
    private final String successKey;
    private final String failKey;

    public TargetedDebuffSpell(int cost, int cooldownTicks, double damage, double range,
                               MobEffect primaryEffect, int primaryDurationTicks, int primaryAmplifier,
                               MobEffect secondaryEffect, int secondaryDurationTicks, int secondaryAmplifier,
                               ParticleOptions particle, SoundEvent sound,
                               String successKey, String failKey) {
        super(cost, cooldownTicks, damage);
        this.range = range;
        this.primaryEffect = primaryEffect;
        this.primaryDurationTicks = primaryDurationTicks;
        this.primaryAmplifier = primaryAmplifier;
        this.secondaryEffect = secondaryEffect;
        this.secondaryDurationTicks = secondaryDurationTicks;
        this.secondaryAmplifier = secondaryAmplifier;
        this.particle = particle;
        this.sound = sound;
        this.successKey = successKey;
        this.failKey = failKey;
    }

    @Override
    public boolean execute(ServerPlayer player, PlayerCultivation cultivation, CultivationSkill skill, SkillContext context) {
        ServerLevel level = player.serverLevel();
        LivingEntity target = findTarget(level, player);
        if (target == null) {
            player.displayClientMessage(Component.translatable(failKey), true);
            return false;
        }

        double damage = calculateDamage(skill.getLevel(), skill.getProficiency());
        if (damage > 0.0D) {
            target.hurt(player.damageSources().indirectMagic(player, player), (float)damage);
        }
        boolean primaryApplied = applyEffect(
                target, primaryEffect, primaryDurationTicks, primaryAmplifier, skill);
        boolean secondaryApplied = applyEffect(
                target, secondaryEffect, secondaryDurationTicks, secondaryAmplifier, skill);
        ActiveTechniqueEffectVfxService.track(
                target,
                ActiveTechniqueEffectVfxService.semantic(skill, "targeted_debuff"),
                ActiveTechniqueEffectVfxService.familyForSkill(
                        skill, TechniqueVfxPalette.Family.NEUTRAL),
                TechniqueVfxPacket.Motif.SEAL,
                Math.max(0.72D, target.getBbWidth() * 0.72D),
                primaryApplied ? primaryEffect : null,
                secondaryApplied ? secondaryEffect : null);

        level.sendParticles(particle,
                target.getX(), target.getY() + target.getBbHeight() * 0.55D, target.getZ(),
                24, 0.45D, 0.45D, 0.45D, 0.03D);
        Vec3 targetCenter = target.position().add(0.0D, target.getBbHeight() * 0.55D, 0.0D);
        TechniqueLifecycleVfxService.captureGeometry(
                level,
                TechniqueVfxPacket.Kind.BEAM,
                TechniqueVfxPalette.Family.NEUTRAL,
                player.getEyePosition(),
                targetCenter,
                0.42D,
                32,
                player.getId() * 37L ^ target.getId());
        level.playSound(null, target.blockPosition(), sound, SoundSource.PLAYERS, 0.7F, 1.15F);
        player.displayClientMessage(Component.translatable(successKey, target.getDisplayName()), true);
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
        AABB searchBox = new AABB(start, traceEnd).inflate(1.0D);
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(level, player, start, traceEnd, searchBox,
                entity -> canTarget(entity, player));
        if (entityHit == null) {
            return null;
        }
        Entity entity = entityHit.getEntity();
        return entity instanceof LivingEntity living ? living : null;
    }

    private boolean canTarget(Entity entity, ServerPlayer player) {
        return canAffect(player, entity);
    }

    private static boolean applyEffect(LivingEntity target, MobEffect effect, int durationTicks,
                                       int amplifier, CultivationSkill skill) {
        if (effect == null || durationTicks <= 0) {
            return false;
        }
        int scaledDuration = durationTicks + Math.max(0, skill.getLevel() - 1) * 10;
        int scaledAmplifier = amplifier + Math.max(0, skill.getLevel() / 5);
        return target.addEffect(new MobEffectInstance(
                effect, scaledDuration, scaledAmplifier, false, true));
    }
}
