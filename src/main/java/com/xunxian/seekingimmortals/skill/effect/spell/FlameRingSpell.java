package com.xunxian.seekingimmortals.skill.effect.spell;

import com.xunxian.seekingimmortals.cultivation.PlayerCultivation;
import com.xunxian.seekingimmortals.skill.CultivationSkill;
import com.xunxian.seekingimmortals.skill.effect.SkillContext;
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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class FlameRingSpell extends SpellEffect {
    private static final double RADIUS = 3.35D;
    private static final DustParticleOptions FLAME_EDGE = new DustParticleOptions(new Vector3f(1.00F, 0.34F, 0.07F), 0.92F);
    private static final DustParticleOptions FLAME_CORE = new DustParticleOptions(new Vector3f(1.00F, 0.88F, 0.24F), 0.58F);

    public FlameRingSpell() {
        super(11, 100, 8.5D);
    }

    @Override
    public boolean execute(ServerPlayer player, PlayerCultivation cultivation, CultivationSkill skill, SkillContext context) {
        ServerLevel level = player.serverLevel();
        Vec3 center = player.position().add(0.0D, 0.35D, 0.0D);
        AABB area = new AABB(center, center).inflate(RADIUS, 1.15D, RADIUS);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, area,
                entity -> canAffect(player, entity));
        if (targets.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.spell.area.fail"), true);
            return false;
        }

        double damage = calculateDamage(skill.getLevel(), skill.getProficiency());
        int hitCount = 0;
        for (LivingEntity target : targets) {
            double distance = Math.sqrt(target.distanceToSqr(center));
            if (distance > RADIUS + 0.35D) {
                continue;
            }
            double falloff = Math.max(0.45D, 1.0D - distance / (RADIUS + 0.1D));
            target.hurt(player.damageSources().indirectMagic(player, player), (float)(damage * falloff));
            target.setSecondsOnFire(3);
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 45 + skill.getLevel() * 3, 0, false, true));
            hitCount++;
        }
        if (hitCount == 0) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.spell.area.fail"), true);
            return false;
        }

        spawnFlameRing(level, center);
        level.playSound(null, player.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 0.65F, 1.05F);
        player.displayClientMessage(Component.translatable("message.seeking_immortals.spell.flame_ring.success", hitCount), true);
        return true;
    }

    private void spawnFlameRing(ServerLevel level, Vec3 center) {
        for (int i = 0; i < 72; i++) {
            double angle = (Math.PI * 2.0D * i) / 72.0D;
            double x = center.x + Math.cos(angle) * RADIUS;
            double z = center.z + Math.sin(angle) * RADIUS;
            double y = center.y + Math.sin(angle * 3.0D) * 0.12D;
            level.sendParticles(FLAME_EDGE, x, y, z, 1, 0.05D, 0.05D, 0.05D, 0.0D);
            if (i % 3 == 0) {
                level.sendParticles(FLAME_CORE, x * 0.94D + center.x * 0.06D, y + 0.12D, z * 0.94D + center.z * 0.06D,
                        1, 0.04D, 0.04D, 0.04D, 0.0D);
            }
        }
        level.sendParticles(FLAME_CORE, center.x, center.y + 0.45D, center.z, 18, 0.45D, 0.22D, 0.45D, 0.025D);
    }
}
