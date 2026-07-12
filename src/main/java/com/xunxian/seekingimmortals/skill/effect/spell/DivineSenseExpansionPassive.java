package com.xunxian.seekingimmortals.skill.effect.spell;

import com.xunxian.seekingimmortals.cultivation.PlayerCultivation;
import com.xunxian.seekingimmortals.skill.CultivationSkill;
import com.xunxian.seekingimmortals.skill.effect.SkillContext;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class DivineSenseExpansionPassive extends SpellEffect {
    public DivineSenseExpansionPassive() {
        super(0, 0, 0.0D);
    }

    @Override
    public boolean execute(ServerPlayer player, PlayerCultivation cultivation, CultivationSkill skill, SkillContext context) {
        player.displayClientMessage(Component.literal("神识扩展已融入识海，探查范围提升五成。"), true);
        return true;
    }
}
