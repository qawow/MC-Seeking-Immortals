package com.xunxian.seekingimmortals.skill.effect.spell;

import com.xunxian.seekingimmortals.cultivation.PlayerCultivation;
import com.xunxian.seekingimmortals.skill.CultivationSkill;
import com.xunxian.seekingimmortals.skill.effect.SkillContext;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class QiGuidingPassive extends SpellEffect {
    public QiGuidingPassive() {
        super(0, 0, 0.0D);
    }

    @Override
    public boolean execute(ServerPlayer player, PlayerCultivation cultivation, CultivationSkill skill, SkillContext context) {
        player.displayClientMessage(Component.translatable("message.seeking_immortals.spell.qi_guiding"), true);
        return true;
    }
}
