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

public class SpiritFlameBeamSpell extends SpellEffect {
    private static final double RANGE = 21.0D;
    private static final double BEAM_RADIUS = 0.82D;
    private static final DustParticleOptions FLAME_EDGE = new DustParticleOptions(new Vector3f(1.00F, 0.32F, 0.08F), 0.72F);
    private static final DustParticleOptions FLAME_CORE = new DustParticleOptions(new Vector3f(1.00F, 0.86F, 0.36F), 0.48F);
    private static final DustParticleOptions ALCHIMIC_SPARK = new DustParticleOptions(new Vector3f(0.95F, 0.42F, 0.72F), 0.38F);

    public SpiritFlameBeamSpell() {
        super(18, 150, 36.0D);
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
            double falloff = Math.max(0.58D, 1.0D - distanceAlong / (RANGE * 1.4D));
            target.hurt(player.damageSources().indirectMagic(player, player), (float)(damage * falloff));
            target.setSecondsOnFire(3);
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 70 + skill.getLevel() * 4, 0, false, true));
            hitCount++;
        }

        spawnBeam(level, start, end);
        level.playSound(null, BlockPos.containing(start), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 0.68F, 1.22F);
        level.playSound(null, BlockPos.containing(end), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.42F, 1.58F);
        player.displayClientMessage(Component.translatable("message.seeking_immortals.spell.luoyun_spirit_flame.success", hitCount), true);
        return true;
    }

    private List<LivingEntity> findTargets(ServerLevel level, ServerPlayer player, Vec3 start, Vec3 end) {
        AABB box = new AABB(start, end).inflate(BEAM_RADIUS);
        Vec3 line = end.subtract(start);
        return level.getEntitiesOfClass(LivingEntity.class, box,
                        entity -> canAffect(player, entity)
                                && distanceToSegment(entity.position().add(0.0D, entity.getBbHeight() * 0.5D, 0.0D), start, line) <= BEAM_RADIUS)
                .stream()
                .sorted(Comparator.comparingDouble(entity -> entity.distanceToSqr(player)))
                .toList();
    }

    private double distanceToSegment(Vec3 point, Vec3 start, Vec3 line) {
        double lengthSqr = line.lengthSqr();
        if (lengthSqr < 0.0001D) {
            return point.distanceTo(start);
        }
        double t = Math.max(0.0D, Math.min(1.0D, point.subtract(start).dot(line) / lengthSqr));
        return point.distanceTo(start.add(line.scale(t)));
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
        int steps = Math.max(10, (int)(line.length() * 5.5D));
        for (int i = 0; i <= steps; i++) {
            double t = i / (double)steps;
            Vec3 point = start.lerp(end, t);
            double coil = t * Math.PI * 9.0D;
            Vec3 swirl = side.scale(Math.sin(coil) * 0.10D).add(up.scale(Math.cos(coil) * 0.10D));
            Vec3 edge = point.add(swirl);
            level.sendParticles(FLAME_CORE, point.x, point.y, point.z, 1, 0.018D, 0.018D, 0.018D, 0.0D);
            if ((i & 1) == 0) {
                level.sendParticles(FLAME_EDGE, edge.x, edge.y, edge.z, 1, 0.045D, 0.035D, 0.045D, 0.0D);
            }
            if (i % 5 == 0) {
                Vec3 spark = point.add(swirl.scale(-0.8D));
                level.sendParticles(ALCHIMIC_SPARK, spark.x, spark.y, spark.z, 1, 0.035D, 0.035D, 0.035D, 0.0D);
            }
        }
        level.sendParticles(FLAME_EDGE, end.x, end.y, end.z, 24, 0.22D, 0.16D, 0.22D, 0.016D);
        level.sendParticles(ALCHIMIC_SPARK, end.x, end.y + 0.08D, end.z, 10, 0.16D, 0.12D, 0.16D, 0.012D);
    }
}
