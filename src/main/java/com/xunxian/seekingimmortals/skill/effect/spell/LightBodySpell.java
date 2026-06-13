package com.xunxian.seekingimmortals.skill.effect.spell;

import com.xunxian.seekingimmortals.cultivation.PlayerCultivation;
import com.xunxian.seekingimmortals.skill.CultivationSkill;
import com.xunxian.seekingimmortals.skill.effect.SkillContext;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

public class LightBodySpell extends SpellEffect {
    public LightBodySpell() {
        super(20, 200, 0);
    }

    @Override
    public boolean execute(ServerPlayer player, PlayerCultivation cultivation, CultivationSkill skill, SkillContext context) {
        if (cultivation.getSpiritualPower() < getSpiritualPowerCost(skill.getLevel())) {
            return false;
        }

        int duration = 200 + skill.getLevel() * 40;
        int amplifier = skill.getLevel() / 3;

        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, duration, amplifier));
        player.addEffect(new MobEffectInstance(MobEffects.JUMP, duration, amplifier));

        player.displayClientMessage(
            net.minecraft.network.chat.Component.literal("轻身术！提升速度和跳跃" + (duration / 20) + "秒"),
            true
        );

        return true;
    }
}
