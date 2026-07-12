package com.xunxian.seekingimmortals.skill.effect.spell;

import com.xunxian.seekingimmortals.cultivation.PlayerCultivation;
import com.xunxian.seekingimmortals.skill.CultivationSkill;
import com.xunxian.seekingimmortals.skill.effect.SkillContext;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class ObjectControlSpell extends SpellEffect {
    private final double range;

    public ObjectControlSpell(int cost, int cooldownTicks, double range) {
        super(cost, cooldownTicks, 2.0D);
        this.range = range;
    }

    @Override
    public boolean execute(ServerPlayer player, PlayerCultivation cultivation, CultivationSkill skill, SkillContext context) {
        ServerLevel level = player.serverLevel();
        Entity target = findTarget(level, player);
        if (target == null) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.spell.target.fail"), true);
            return false;
        }

        if (target instanceof ItemEntity item) {
            pullItem(player, item);
            level.sendParticles(ParticleTypes.END_ROD, item.getX(), item.getY() + 0.25D, item.getZ(),
                    12, 0.2D, 0.2D, 0.2D, 0.02D);
            level.playSound(null, item.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.55F, 1.4F);
            player.displayClientMessage(Component.translatable("message.seeking_immortals.spell.object_control.item_success", item.getItem().getHoverName()), true);
            return true;
        }

        if (target instanceof LivingEntity living) {
            double damage = calculateDamage(skill.getLevel(), skill.getProficiency());
            living.hurt(player.damageSources().magic(), (float)damage);
            living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 70 + skill.getLevel() * 10, Math.max(0, skill.getLevel() / 5), false, true));
            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40 + skill.getLevel() * 6, 0, false, true));
            level.sendParticles(ParticleTypes.END_ROD, living.getX(), living.getY() + living.getBbHeight() * 0.55D, living.getZ(),
                    18, 0.35D, 0.35D, 0.35D, 0.03D);
            level.playSound(null, living.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.65F, 1.25F);
            player.displayClientMessage(Component.translatable("message.seeking_immortals.spell.object_control.target_success", living.getDisplayName()), true);
            return true;
        }

        return false;
    }

    private Entity findTarget(ServerLevel level, ServerPlayer player) {
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
                entity -> entity != player && entity.isAlive() && !entity.isSpectator() && (entity instanceof ItemEntity || entity instanceof LivingEntity));
        return entityHit == null ? null : entityHit.getEntity();
    }

    private static void pullItem(ServerPlayer player, ItemEntity item) {
        Vec3 pull = player.position().add(0.0D, 1.0D, 0.0D).subtract(item.position());
        if (pull.lengthSqr() < 0.05D) {
            item.setDeltaMovement(Vec3.ZERO);
        } else {
            item.setDeltaMovement(pull.normalize().scale(0.75D));
        }
        item.hasImpulse = true;
    }
}
