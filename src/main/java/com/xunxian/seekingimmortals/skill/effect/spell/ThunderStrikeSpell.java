package com.xunxian.seekingimmortals.skill.effect.spell;

import com.xunxian.seekingimmortals.cultivation.PlayerCultivation;
import com.xunxian.seekingimmortals.skill.CultivationSkill;
import com.xunxian.seekingimmortals.skill.effect.SkillContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.List;

public class ThunderStrikeSpell extends SpellEffect {
    private static final double MIN_RANGE = 12.0D;
    private static final double MAX_RANGE = 32.0D;
    private static final double SPLASH_RADIUS = 2.25D;
    private static final DustParticleOptions THUNDER_CORE = new DustParticleOptions(new Vector3f(0.70F, 0.90F, 1.00F), 0.92F);
    private static final DustParticleOptions THUNDER_EDGE = new DustParticleOptions(new Vector3f(0.30F, 0.46F, 1.00F), 0.62F);

    public ThunderStrikeSpell() {
        super(12, 60, 8.0D);
    }

    @Override
    public boolean execute(ServerPlayer player, PlayerCultivation cultivation, CultivationSkill skill, SkillContext context) {
        ServerLevel level = player.serverLevel();
        Vec3 start = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        if (look.lengthSqr() < 0.001D) {
            return false;
        }

        double range = Math.min(MAX_RANGE, Math.max(MIN_RANGE, cultivation.getDivSense()));
        Vec3 end = start.add(look.normalize().scale(range));
        BlockHitResult blockHit = level.clip(new ClipContext(start, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        Vec3 traceEnd = blockHit.getType() == HitResult.Type.MISS ? end : blockHit.getLocation();
        EntityHitResult entityHit = findTarget(level, player, start, traceEnd);
        LivingEntity directTarget = entityHit != null && entityHit.getEntity() instanceof LivingEntity living ? living : null;
        Vec3 strikePos = directTarget == null
                ? traceEnd
                : directTarget.position().add(0.0D, directTarget.getBbHeight() * 0.55D, 0.0D);

        double damage = calculateDamage(skill.getLevel(), skill.getProficiency());
        int hitCount = strikeTargets(level, player, strikePos, directTarget, damage, skill);
        spawnThunderColumn(level, strikePos);
        level.playSound(null, BlockPos.containing(strikePos), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 0.65F, 1.35F);
        player.displayClientMessage(Component.translatable("message.seeking_immortals.spell.thunder_strike.success", hitCount), true);
        return true;
    }

    private EntityHitResult findTarget(ServerLevel level, ServerPlayer player, Vec3 start, Vec3 traceEnd) {
        AABB searchBox = new AABB(start, traceEnd).inflate(1.25D);
        return ProjectileUtil.getEntityHitResult(level, player, start, traceEnd, searchBox,
                entity -> canAffect(player, entity));
    }

    private int strikeTargets(ServerLevel level, ServerPlayer player, Vec3 center, LivingEntity directTarget,
                              double damage, CultivationSkill skill) {
        AABB area = new AABB(center, center).inflate(SPLASH_RADIUS, 1.75D, SPLASH_RADIUS);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, area,
                entity -> canAffect(player, entity));
        int hitCount = 0;
        for (LivingEntity target : targets) {
            double distance = Math.sqrt(target.distanceToSqr(center));
            double falloff = target == directTarget ? 1.0D : Math.max(0.35D, 1.0D - distance / SPLASH_RADIUS);
            float scaledDamage = (float)(damage * (target == directTarget ? 1.0D : 0.55D) * falloff);
            if (scaledDamage <= 0.0F) {
                continue;
            }
            target.hurt(player.damageSources().indirectMagic(player, player), scaledDamage);
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 45 + skill.getLevel() * 4, 0, false, true));
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 50 + skill.getLevel() * 4, 0, false, true));
            hitCount++;
        }
        return hitCount;
    }

    private void spawnThunderColumn(ServerLevel level, Vec3 center) {
        int seed = Math.abs(BlockPos.containing(center).hashCode());
        for (int i = 0; i < 16; i++) {
            double y = center.y + i * 0.36D;
            double sway = seed * 0.031D + i * 1.73D;
            double x = center.x + Math.sin(sway) * 0.20D;
            double z = center.z + Math.cos(sway * 0.83D) * 0.20D;
            level.sendParticles(THUNDER_CORE, x, y, z, 1, 0.035D, 0.035D, 0.035D, 0.0D);
            if ((i & 1) == 0) {
                level.sendParticles(THUNDER_EDGE, x, y, z, 2, 0.11D, 0.06D, 0.11D, 0.01D);
            }
        }
        level.sendParticles(THUNDER_EDGE, center.x, center.y + 0.12D, center.z, 36, 0.85D, 0.08D, 0.85D, 0.035D);
        level.sendParticles(THUNDER_CORE, center.x, center.y + 0.55D, center.z, 18, 0.42D, 0.30D, 0.42D, 0.02D);
    }
}
