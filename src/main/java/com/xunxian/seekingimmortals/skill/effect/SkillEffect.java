package com.xunxian.seekingimmortals.skill.effect;

import com.xunxian.seekingimmortals.cultivation.PlayerCultivation;
import com.xunxian.seekingimmortals.skill.CultivationSkill;
import net.minecraft.server.level.ServerPlayer;

public interface SkillEffect {
    boolean execute(ServerPlayer player, PlayerCultivation cultivation, CultivationSkill skill, SkillContext context);

    int getSpiritualPowerCost(int skillLevel);

    default int getDivineConsciousnessCost(int skillLevel) { return 0; }

    int getCooldownTicks(int skillLevel);

    default boolean canExecute(ServerPlayer player, PlayerCultivation cultivation) { return true; }
}
