package com.xunxian.seekingimmortals.skill.effect.spell;

import com.xunxian.seekingimmortals.cultivation.PlayerCultivation;
import com.xunxian.seekingimmortals.skill.CultivationSkill;
import com.xunxian.seekingimmortals.skill.effect.SkillContext;
import com.xunxian.seekingimmortals.skill.effect.TechniqueVfxPalette;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class ElementalConeSpell extends SpellEffect {
    private final double range;
    private final double endRadius;
    private final String element;
    private final String successKey;

    public ElementalConeSpell(int cost, int cooldownTicks, double damage, double range, double endRadius,
                              String element, String successKey) {
        super(cost, cooldownTicks, damage);
        this.range = Math.max(1.0D, range);
        this.endRadius = Math.max(0.75D, endRadius);
        this.element = element == null ? "neutral" : element;
        this.successKey = successKey;
    }

    @Override
    public boolean execute(ServerPlayer player, PlayerCultivation cultivation, CultivationSkill skill,
                           SkillContext context) {
        ServerLevel level = player.serverLevel();
        Vec3 direction = player.getLookAngle();
        if (direction.lengthSqr() < 0.001D) {
            return false;
        }

        direction = direction.normalize();
        Vec3 start = player.getEyePosition().add(direction.scale(0.35D));
        Vec3 maxEnd = start.add(direction.scale(range));
        BlockHitResult centerHit = level.clip(new ClipContext(
                start, maxEnd, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 end = centerHit.getType() == HitResult.Type.MISS ? maxEnd : centerHit.getLocation();
        double effectiveRange = Math.max(0.25D, start.distanceTo(end));
        List<LivingEntity> targets = findTargets(level, player, start, direction, effectiveRange);
        if (targets.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.spell.target.fail"), true);
            return false;
        }

        double damage = calculateDamage(skill.getLevel(), skill.getProficiency(), context);
        TechniqueVfxPalette.Profile vfx = TechniqueVfxPalette.profile(element);
        MobEffect debuff = vfx.primaryDebuff();
        for (LivingEntity target : targets) {
            double distanceAlong = target.getBoundingBox().getCenter().subtract(start).dot(direction);
            double falloff = Math.max(0.62D, 1.0D - distanceAlong / (effectiveRange * 1.8D));
            if (target.hurt(player.damageSources().indirectMagic(player, player), (float) (damage * falloff))
                    && debuff != null) {
                target.addEffect(new MobEffectInstance(
                        debuff, 70 + Math.max(0, skill.getLevel() - 1) * 6, 0, false, true));
            }
        }

        vfx.castAt(level, player);
        vfx.coneAt(level, start, direction, effectiveRange, endRadius);
        player.displayClientMessage(Component.translatable(
                successKey, String.format(Locale.ROOT, "%.1f", damage)), true);
        return true;
    }

    private List<LivingEntity> findTargets(ServerLevel level, ServerPlayer player, Vec3 start,
                                            Vec3 direction, double effectiveRange) {
        Vec3 end = start.add(direction.scale(effectiveRange));
        AABB search = new AABB(start, end).inflate(endRadius + 1.0D);
        return level.getEntitiesOfClass(LivingEntity.class, search,
                        entity -> canAffect(player, entity)
                                && insideCone(entity, start, direction, effectiveRange)
                                && hasLineOfSight(level, player, start, entity.getBoundingBox().getCenter()))
                .stream()
                .sorted(Comparator.comparingDouble(entity -> entity.distanceToSqr(player)))
                .toList();
    }

    private boolean insideCone(LivingEntity entity, Vec3 start, Vec3 direction, double effectiveRange) {
        Vec3 offset = entity.getBoundingBox().getCenter().subtract(start);
        double distanceAlong = offset.dot(direction);
        if (distanceAlong <= 0.0D || distanceAlong > effectiveRange + entity.getBbWidth()) {
            return false;
        }
        double radialDistanceSqr = Math.max(0.0D, offset.lengthSqr() - distanceAlong * distanceAlong);
        double widthAtDistance = Math.max(0.65D, endRadius * distanceAlong / effectiveRange)
                + entity.getBbWidth() * 0.5D;
        return radialDistanceSqr <= widthAtDistance * widthAtDistance;
    }

    private static boolean hasLineOfSight(ServerLevel level, ServerPlayer player, Vec3 start, Vec3 target) {
        BlockHitResult hit = level.clip(new ClipContext(
                start, target, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        return hit.getType() == HitResult.Type.MISS
                || start.distanceToSqr(hit.getLocation()) + 0.25D >= start.distanceToSqr(target);
    }
}
