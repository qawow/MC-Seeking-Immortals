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

public class ElementalBeamSpell extends SpellEffect {
    private final double range;
    private final double radius;
    private final String element;
    private final String successKey;

    public ElementalBeamSpell(int cost, int cooldownTicks, double damage, double range, double radius,
                              String element, String successKey) {
        super(cost, cooldownTicks, damage);
        this.range = Math.max(1.0D, range);
        this.radius = Math.max(0.25D, radius);
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
        Vec3 start = player.getEyePosition().add(direction.scale(0.45D));
        Vec3 maxEnd = start.add(direction.scale(range));
        BlockHitResult blockHit = level.clip(new ClipContext(
                start, maxEnd, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 end = blockHit.getType() == HitResult.Type.MISS ? maxEnd : blockHit.getLocation();
        List<LivingEntity> targets = findTargets(level, player, start, end, direction);
        if (targets.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.spell.target.fail"), true);
            return false;
        }

        double damage = calculateDamage(skill.getLevel(), skill.getProficiency(), context);
        TechniqueVfxPalette.Profile vfx = TechniqueVfxPalette.profile(element);
        MobEffect debuff = vfx.primaryDebuff();
        for (LivingEntity target : targets) {
            double distanceAlong = Math.max(0.0D, target.getBoundingBox().getCenter().subtract(start).dot(direction));
            double falloff = Math.max(0.58D, 1.0D - distanceAlong / (range * 1.45D));
            if (target.hurt(player.damageSources().indirectMagic(player, player), (float) (damage * falloff))
                    && debuff != null) {
                target.addEffect(new MobEffectInstance(
                        debuff, 55 + Math.max(0, skill.getLevel() - 1) * 5, 0, false, true));
            }
        }

        vfx.castAt(level, player);
        vfx.beamAt(level, start, end, radius);
        player.displayClientMessage(Component.translatable(
                successKey, String.format(Locale.ROOT, "%.1f", damage)), true);
        return true;
    }

    private List<LivingEntity> findTargets(ServerLevel level, ServerPlayer player, Vec3 start, Vec3 end,
                                            Vec3 direction) {
        Vec3 line = end.subtract(start);
        AABB search = new AABB(start, end).inflate(radius + 1.0D);
        return level.getEntitiesOfClass(LivingEntity.class, search,
                        entity -> canAffect(player, entity)
                                && distanceToSegment(entity.getBoundingBox().getCenter(), start, line)
                                <= radius + entity.getBbWidth() * 0.45D)
                .stream()
                .sorted(Comparator.comparingDouble(
                        entity -> entity.getBoundingBox().getCenter().subtract(start).dot(direction)))
                .toList();
    }

    private static double distanceToSegment(Vec3 point, Vec3 start, Vec3 line) {
        double lengthSqr = line.lengthSqr();
        if (lengthSqr < 0.0001D) {
            return point.distanceTo(start);
        }
        double progress = Math.max(0.0D, Math.min(1.0D, point.subtract(start).dot(line) / lengthSqr));
        return point.distanceTo(start.add(line.scale(progress)));
    }
}
