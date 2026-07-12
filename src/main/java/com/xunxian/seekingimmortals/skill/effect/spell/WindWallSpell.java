package com.xunxian.seekingimmortals.skill.effect.spell;

import com.xunxian.seekingimmortals.cultivation.PlayerCultivation;
import com.xunxian.seekingimmortals.skill.CultivationSkill;
import com.xunxian.seekingimmortals.skill.effect.SkillContext;
import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;

public class WindWallSpell extends SpellEffect {
    private final int durationTicks;
    private final double radius;

    public WindWallSpell(int cost, int cooldownTicks, int durationTicks, double radius) {
        super(cost, cooldownTicks, 0.0D);
        this.durationTicks = durationTicks;
        this.radius = radius;
    }

    @Override
    public boolean execute(ServerPlayer player, PlayerCultivation cultivation, CultivationSkill skill, SkillContext context) {
        ServerLevel level = player.serverLevel();
        int duration = durationTicks + Math.max(0, skill.getLevel() - 1) * 15;
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, duration, Math.max(0, skill.getLevel() / 6), false, true));

        List<Projectile> projectiles = level.getEntitiesOfClass(Projectile.class, player.getBoundingBox().inflate(radius),
                projectile -> projectile.isAlive() && projectile.getOwner() != player);
        int deflected = 0;
        for (Projectile projectile : projectiles) {
            if (deflectProjectile(player, projectile)) {
                deflected++;
            }
        }

        level.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 1.0D, player.getZ(),
                44, radius * 0.35D, 0.8D, radius * 0.35D, 0.06D);
        level.playSound(null, player.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 0.8F, 1.35F);
        player.displayClientMessage(Component.translatable("message.seeking_immortals.spell.wind_wall.success", deflected), true);
        return true;
    }

    private static boolean deflectProjectile(ServerPlayer player, Projectile projectile) {
        Entity owner = projectile.getOwner();
        if (owner == player) {
            return false;
        }
        Vec3 away = projectile.position().subtract(player.position());
        if (away.lengthSqr() < 0.001D) {
            away = player.getLookAngle().reverse();
        }
        double speed = Math.max(0.45D, projectile.getDeltaMovement().length() + 0.25D);
        projectile.setDeltaMovement(away.normalize().scale(speed).add(0.0D, 0.08D, 0.0D));
        projectile.hasImpulse = true;
        return true;
    }
}
