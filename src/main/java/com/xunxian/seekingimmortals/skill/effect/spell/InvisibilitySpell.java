package com.xunxian.seekingimmortals.skill.effect.spell;

import com.xunxian.seekingimmortals.cultivation.PlayerCultivation;
import com.xunxian.seekingimmortals.skill.CultivationSkill;
import com.xunxian.seekingimmortals.skill.effect.SkillContext;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

public class InvisibilitySpell extends SpellEffect {
    public InvisibilitySpell() {
        super(50, 400, 0);
    }

    @Override
    public boolean execute(ServerPlayer player, PlayerCultivation cultivation, CultivationSkill skill, SkillContext context) {
        if (cultivation.getSpiritualPower() < getSpiritualPowerCost(skill.getLevel())) {
            return false;
        }

        int duration = 100 + skill.getLevel() * 20;

        player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, duration, 0));

        player.displayClientMessage(
            net.minecraft.network.chat.Component.literal("隐身术！隐身" + (duration / 20) + "秒"),
            true
        );

        return true;
    }
}
