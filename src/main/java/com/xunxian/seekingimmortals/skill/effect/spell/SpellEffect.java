package com.xunxian.seekingimmortals.skill.effect.spell;

import com.xunxian.seekingimmortals.cultivation.PlayerCultivation;
import com.xunxian.seekingimmortals.skill.CultivationSkill;
import com.xunxian.seekingimmortals.skill.effect.SkillContext;
import com.xunxian.seekingimmortals.skill.effect.SkillEffect;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public abstract class SpellEffect implements SkillEffect {
    private static final ThreadLocal<Double> ACTIVE_POWER_SCALE = ThreadLocal.withInitial(() -> 1.0D);

    protected final int baseSpiritualPowerCost;
    protected final int baseCooldownTicks;
    protected final double baseDamage;

    public SpellEffect(int baseSpiritualPowerCost, int baseCooldownTicks, double baseDamage) {
        this.baseSpiritualPowerCost = baseSpiritualPowerCost;
        this.baseCooldownTicks = baseCooldownTicks;
        this.baseDamage = baseDamage;
    }

    /** Temporary combat scale for artifact-mapped casts. Always clear in a finally block. */
    public static void pushPowerScale(double scale) {
        ACTIVE_POWER_SCALE.set(Math.max(0.0D, scale));
    }

    public static void clearPowerScale() {
        ACTIVE_POWER_SCALE.set(1.0D);
    }

    public static double currentPowerScale() {
        Double scale = ACTIVE_POWER_SCALE.get();
        return scale == null ? 1.0D : Math.max(0.0D, scale);
    }

    @Override
    public int getSpiritualPowerCost(int skillLevel) {
        return Math.max(0, baseSpiritualPowerCost);
    }

    @Override
    public int getCooldownTicks(int skillLevel) {
        return Math.max(0, baseCooldownTicks);
    }

    /** Authored/base damage before level and proficiency multipliers. */
    public double getBaseDamage() {
        return baseDamage;
    }

    protected double calculateDamage(int skillLevel, int proficiency) {
        double levelMultiplier = 1.0 + skillLevel * 0.15;
        double proficiencyMultiplier = 1.0 + proficiency / 10000.0;
        return baseDamage * levelMultiplier * proficiencyMultiplier * currentPowerScale();
    }

    protected double calculateDamage(int skillLevel, int proficiency, SkillContext context) {
        double scale = context == null ? currentPowerScale() : context.getPowerScale();
        double levelMultiplier = 1.0 + skillLevel * 0.15;
        double proficiencyMultiplier = 1.0 + proficiency / 10000.0;
        return baseDamage * levelMultiplier * proficiencyMultiplier * Math.max(0.0D, scale);
    }

    protected static boolean canAffect(ServerPlayer caster, Entity entity) {
        if (caster == null || entity == caster || !(entity instanceof LivingEntity living)
                || !living.isAlive() || entity.isSpectator()) {
            return false;
        }
        return !(living instanceof Player targetPlayer) || caster.canHarmPlayer(targetPlayer);
    }
}
