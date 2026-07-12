package com.xunxian.seekingimmortals.skill.effect.spell;

import com.xunxian.seekingimmortals.cultivation.PlayerCultivation;
import com.xunxian.seekingimmortals.skill.CultivationSkill;
import com.xunxian.seekingimmortals.skill.effect.SkillContext;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class GoldBeamSpell extends SpellEffect {
    private static final double RANGE = 20.0D;
    private static final double BEAM_RADIUS = 0.72D;
    private static final DustParticleOptions GOLD_EDGE = new DustParticleOptions(new Vector3f(1.00F, 0.80F, 0.18F), 0.78F);
    private static final DustParticleOptions GOLD_CORE = new DustParticleOptions(new Vector3f(1.00F, 1.00F, 0.78F), 0.52F);

    public GoldBeamSpell() {
        super(10, 70, 11.5D);
    }

    @Override
    public boolean execute(ServerPlayer player, PlayerCultivation cultivation, CultivationSkill skill, SkillContext context) {
        ServerLevel level = player.serverLevel();
        Vec3 look = player.getLookAngle();
        if (look.lengthSqr() < 0.001D) {
            return false;
        }

        Vec3 direction = look.normalize();
        Vec3 start = player.getEyePosition().add(direction.scale(0.55D));
        Vec3 maxEnd = start.add(direction.scale(RANGE));
        BlockHitResult blockHit = level.clip(new ClipContext(start, maxEnd, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 end = blockHit.getType() == HitResult.Type.MISS ? maxEnd : blockHit.getLocation();
        List<LivingEntity> targets = findTargets(level, player, start, end);
        if (targets.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.spell.target.fail"), true);
            return false;
        }

        double damage = calculateDamage(skill.getLevel(), skill.getProficiency());
        int hitCount = 0;
        for (LivingEntity target : targets) {
            double distanceAlong = target.position().subtract(start).dot(direction);
            double falloff = Math.max(0.55D, 1.0D - distanceAlong / (RANGE * 1.5D));
            target.hurt(player.damageSources().magic(), (float)(damage * falloff));
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 55 + skill.getLevel() * 4, 0, false, true));
            hitCount++;
        }

        spawnBeam(level, start, end);
        level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_CLUSTER_HIT, SoundSource.PLAYERS, 0.78F, 1.65F);
        player.displayClientMessage(Component.translatable("message.seeking_immortals.spell.gold_beam.success", hitCount), true);
        return true;
    }

    private List<LivingEntity> findTargets(ServerLevel level, ServerPlayer player, Vec3 start, Vec3 end) {
        AABB box = new AABB(start, end).inflate(BEAM_RADIUS);
        Vec3 line = end.subtract(start);
        return level.getEntitiesOfClass(LivingEntity.class, box,
                        entity -> entity != player && entity.isAlive() && !entity.isSpectator()
                                && distanceToSegment(entity.position().add(0.0D, entity.getBbHeight() * 0.5D, 0.0D), start, end, line) <= BEAM_RADIUS)
                .stream()
                .sorted(Comparator.comparingDouble(entity -> entity.distanceToSqr(player)))
                .toList();
    }

    private double distanceToSegment(Vec3 point, Vec3 start, Vec3 end, Vec3 line) {
        double lengthSqr = line.lengthSqr();
        if (lengthSqr < 0.0001D) {
            return point.distanceTo(start);
        }
        double t = Math.max(0.0D, Math.min(1.0D, point.subtract(start).dot(line) / lengthSqr));
        Vec3 projection = start.add(line.scale(t));
        return point.distanceTo(projection);
    }

    private void spawnBeam(ServerLevel level, Vec3 start, Vec3 end) {
        Vec3 line = end.subtract(start);
        int steps = Math.max(8, (int)(line.length() * 5.0D));
        for (int i = 0; i <= steps; i++) {
            double t = i / (double)steps;
            Vec3 point = start.lerp(end, t);
            level.sendParticles(GOLD_CORE, point.x, point.y, point.z, 1, 0.018D, 0.018D, 0.018D, 0.0D);
            if (i % 2 == 0) {
                level.sendParticles(GOLD_EDGE, point.x, point.y, point.z, 1, 0.06D, 0.035D, 0.06D, 0.0D);
            }
        }
        level.sendParticles(GOLD_EDGE, end.x, end.y, end.z, 14, 0.18D, 0.14D, 0.18D, 0.012D);
    }
}
