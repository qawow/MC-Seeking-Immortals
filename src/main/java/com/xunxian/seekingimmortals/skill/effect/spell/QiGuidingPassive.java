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
        player.displayClientMessage(Component.literal("引气入体已融入周天运转，可进行打坐吐纳。"), true);
        return true;
    }
}
