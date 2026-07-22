package com.xunxian.seekingimmortals.skill.effect.spell;

import com.xunxian.seekingimmortals.cultivation.PlayerCultivation;
import com.xunxian.seekingimmortals.network.TechniqueVfxPacket;
import com.xunxian.seekingimmortals.skill.CultivationSkill;
import com.xunxian.seekingimmortals.skill.effect.ActiveTechniqueEffectVfxService;
import com.xunxian.seekingimmortals.skill.effect.SkillContext;
import com.xunxian.seekingimmortals.skill.effect.TechniqueLifecycleVfxService;
import com.xunxian.seekingimmortals.skill.effect.TechniqueVfxPalette;
import java.util.List;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class AreaDebuffSpell extends SpellEffect {
    private final double range;
    private final double radius;
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

    public AreaDebuffSpell(int cost, int cooldownTicks, double damage, double range, double radius,
                           MobEffect primaryEffect, int primaryDurationTicks, int primaryAmplifier,
                           MobEffect secondaryEffect, int secondaryDurationTicks, int secondaryAmplifier,
                           ParticleOptions particle, SoundEvent sound,
                           String successKey, String failKey) {
        super(cost, cooldownTicks, damage);
        this.range = range;
        this.radius = radius;
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
        Vec3 center = findImpactPoint(level, player);
        AABB area = new AABB(center.x - radius, center.y - 1.0D, center.z - radius,
                center.x + radius, center.y + 2.0D, center.z + radius);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, area,
                entity -> canAffect(player, entity));
        if (targets.isEmpty()) {
            player.displayClientMessage(Component.translatable(failKey), true);
            return false;
        }

        double damage = calculateDamage(skill.getLevel(), skill.getProficiency());
        for (LivingEntity target : targets) {
            if (damage > 0.0D) {
                target.hurt(player.damageSources().indirectMagic(player, player), (float)damage);
            }
            boolean primaryApplied = applyEffect(
                    target, primaryEffect, primaryDurationTicks, primaryAmplifier, skill);
            boolean secondaryApplied = applyEffect(
                    target, secondaryEffect, secondaryDurationTicks, secondaryAmplifier, skill);
            ActiveTechniqueEffectVfxService.track(
                    target,
                    ActiveTechniqueEffectVfxService.semantic(skill, "area_debuff"),
                    ActiveTechniqueEffectVfxService.familyForSkill(
                            skill, TechniqueVfxPalette.Family.NEUTRAL),
                    TechniqueVfxPacket.Motif.DOMAIN,
                    Math.max(0.72D, target.getBbWidth() * 0.72D),
                    primaryApplied ? primaryEffect : null,
                    secondaryApplied ? secondaryEffect : null);
        }

        level.sendParticles(particle, center.x, center.y + 0.1D, center.z,
                48, radius * 0.45D, 0.12D, radius * 0.45D, 0.04D);
        TechniqueLifecycleVfxService.captureGeometry(
                level,
                TechniqueVfxPacket.Kind.FORMATION,
                TechniqueVfxPalette.Family.NEUTRAL,
                center,
                center,
                radius,
                40,
                player.getId() * 41L ^ targets.size());
        level.playSound(null, player.blockPosition(), sound, SoundSource.PLAYERS, 0.75F, 0.9F);
        player.displayClientMessage(Component.translatable(successKey, targets.size()), true);
        return true;
    }

    private Vec3 findImpactPoint(ServerLevel level, ServerPlayer player) {
        Vec3 start = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        if (look.lengthSqr() < 0.001D) {
            return player.position();
        }
        Vec3 end = start.add(look.normalize().scale(range));
        BlockHitResult hit = level.clip(new ClipContext(start, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        return hit.getType() == HitResult.Type.MISS ? end : hit.getLocation();
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
