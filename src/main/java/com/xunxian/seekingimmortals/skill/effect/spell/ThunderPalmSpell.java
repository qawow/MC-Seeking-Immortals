package com.xunxian.seekingimmortals.skill.effect.spell;

import com.xunxian.seekingimmortals.cultivation.PlayerCultivation;
import com.xunxian.seekingimmortals.skill.CultivationSkill;
import com.xunxian.seekingimmortals.skill.effect.SkillContext;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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

public class ThunderPalmSpell extends SpellEffect {
    private static final double RANGE = 4.25D;
    private static final DustParticleOptions THUNDER_CORE = new DustParticleOptions(new Vector3f(0.72F, 0.92F, 1.00F), 0.78F);
    private static final DustParticleOptions THUNDER_EDGE = new DustParticleOptions(new Vector3f(0.30F, 0.48F, 1.00F), 0.52F);

    public ThunderPalmSpell() {
        super(9, 55, 10.0D);
    }

    @Override
    public boolean execute(ServerPlayer player, PlayerCultivation cultivation, CultivationSkill skill, SkillContext context) {
        ServerLevel level = player.serverLevel();
        LivingEntity target = findTarget(level, player);
        if (target == null) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.spell.target.fail"), true);
            return false;
        }

        double damage = calculateDamage(skill.getLevel(), skill.getProficiency());
        target.hurt(player.damageSources().indirectMagic(player, player), (float)damage);
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 50 + skill.getLevel() * 4, 1, false, true));
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 45 + skill.getLevel() * 4, 0, false, true));
        spawnPalmArc(level, player.getEyePosition().subtract(0.0D, 0.35D, 0.0D),
                target.position().add(0.0D, target.getBbHeight() * 0.55D, 0.0D));
        level.playSound(null, target.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 0.55F, 1.55F);
        player.displayClientMessage(Component.translatable("message.seeking_immortals.spell.thunder_palm.success",
                target.getDisplayName(), String.format(Locale.ROOT, "%.1f", damage)), true);
        return true;
    }

    private LivingEntity findTarget(ServerLevel level, ServerPlayer player) {
        Vec3 start = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        if (look.lengthSqr() < 0.001D) {
            return null;
        }
        Vec3 end = start.add(look.normalize().scale(RANGE));
        BlockHitResult blockHit = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 traceEnd = blockHit.getType() == HitResult.Type.MISS ? end : blockHit.getLocation();
        AABB searchBox = new AABB(start, traceEnd).inflate(0.9D);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(level, player, start, traceEnd, searchBox,
                entity -> canTarget(entity, player));
        if (hit == null || !(hit.getEntity() instanceof LivingEntity living)) {
            return null;
        }
        return living;
    }

    private boolean canTarget(Entity entity, ServerPlayer player) {
        return canAffect(player, entity);
    }

    private void spawnPalmArc(ServerLevel level, Vec3 start, Vec3 end) {
        Vec3 path = end.subtract(start);
        int steps = Math.max(6, (int)(path.length() * 5.0D));
        int seed = Math.abs(BlockPos.containing(end).hashCode());
        for (int i = 0; i <= steps; i++) {
            double t = i / (double)steps;
            double sway = Math.sin(seed * 0.019D + i * 1.41D) * 0.10D;
            Vec3 point = start.lerp(end, t).add(0.0D, sway, Math.cos(seed * 0.013D + i) * 0.08D);
            level.sendParticles(THUNDER_CORE, point.x, point.y, point.z, 1, 0.025D, 0.025D, 0.025D, 0.0D);
            if ((i & 1) == 0) {
                level.sendParticles(THUNDER_EDGE, point.x, point.y, point.z, 2, 0.08D, 0.04D, 0.08D, 0.01D);
            }
        }
        level.sendParticles(THUNDER_EDGE, end.x, end.y, end.z, 20, 0.34D, 0.28D, 0.34D, 0.025D);
    }
}
