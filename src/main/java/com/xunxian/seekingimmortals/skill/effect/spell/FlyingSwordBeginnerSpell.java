package com.xunxian.seekingimmortals.skill.effect.spell;

import com.xunxian.seekingimmortals.cultivation.FlyingAuthority;
import com.xunxian.seekingimmortals.cultivation.PlayerCultivation;
import com.xunxian.seekingimmortals.skill.CultivationSkill;
import com.xunxian.seekingimmortals.skill.effect.SkillContext;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class FlyingSwordBeginnerSpell extends SpellEffect {
    public static final String ACTIVE_KEY = "SeekingImmortalsQiFlyingActive";
    public static final float SPEED = 0.040F;
    public static final int COST_PER_SECOND = 5;

    public FlyingSwordBeginnerSpell() {
        super(0, 20, 0.0D);
    }

    @Override
    public boolean execute(ServerPlayer player, PlayerCultivation cultivation, CultivationSkill skill, SkillContext context) {
        CompoundTag data = player.getPersistentData();
        if (data.getBoolean(ACTIVE_KEY)) {
            stop(player, "御剑飞行已收束。");
            return true;
        }
        if (cultivation.getSpiritualPower() < COST_PER_SECOND) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.not_enough_qi"), true);
            return false;
        }
        data.putBoolean(ACTIVE_KEY, true);
        FlyingAuthority.grant(player, FlyingAuthority.SOURCE_QI_FLYING, SPEED);
        player.displayClientMessage(Component.literal("御剑飞行初启动，每秒消耗5点灵力。"), true);
        return true;
    }

    public static void stop(ServerPlayer player, String message) {
        CompoundTag data = player.getPersistentData();
        if (!data.getBoolean(ACTIVE_KEY)) return;
        data.remove(ACTIVE_KEY);
        FlyingAuthority.revoke(player, FlyingAuthority.SOURCE_QI_FLYING, null, 0.0F);
        player.displayClientMessage(Component.literal(message), true);
    }
}
