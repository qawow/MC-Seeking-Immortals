package com.xunxian.seekingimmortals.skill.effect.spell;

import com.xunxian.seekingimmortals.cultivation.PlayerCultivation;
import com.xunxian.seekingimmortals.skill.CultivationSkill;
import com.xunxian.seekingimmortals.skill.effect.SkillContext;
import com.xunxian.seekingimmortals.skill.effect.SkillEffect;
import net.minecraft.server.level.ServerPlayer;

public abstract class SpellEffect implements SkillEffect {
    protected final int baseSpiritualPowerCost;
    protected final int baseCooldownTicks;
    protected final double baseDamage;

    public SpellEffect(int baseSpiritualPowerCost, int baseCooldownTicks, double baseDamage) {
        this.baseSpiritualPowerCost = baseSpiritualPowerCost;
        this.baseCooldownTicks = baseCooldownTicks;
        this.baseDamage = baseDamage;
    }

    @Override
    public int getSpiritualPowerCost(int skillLevel) {
        return Math.max(1, baseSpiritualPowerCost - skillLevel * 2);
    }

    @Override
    public int getCooldownTicks(int skillLevel) {
        return Math.max(20, baseCooldownTicks - skillLevel * 10);
    }

    protected double calculateDamage(int skillLevel, int proficiency) {
        double levelMultiplier = 1.0 + skillLevel * 0.15;
        double proficiencyMultiplier = 1.0 + proficiency / 10000.0;
        return baseDamage * levelMultiplier * proficiencyMultiplier;
    }
}
