package com.xunxian.seekingimmortals.skill.effect.spell;

import com.xunxian.seekingimmortals.cultivation.FlyingAuthority;
import com.xunxian.seekingimmortals.cultivation.PlayerCultivation;
import com.xunxian.seekingimmortals.skill.CultivationSkill;
import com.xunxian.seekingimmortals.skill.effect.SkillContext;
import com.xunxian.seekingimmortals.skill.effect.TechniqueVfxPalette;
import com.xunxian.seekingimmortals.util.PlayerDisplayText;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
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
        TechniqueVfxPalette.Profile vfx = TechniqueVfxPalette.profile("metal");
        vfx.castAt(player.serverLevel(), player);
        vfx.auraAt(player.serverLevel(), player, 0.9D, 24);
        player.displayClientMessage(Component.translatable("message.seeking_immortals.spell.flying_sword_beginner"), true);
        return true;
    }

    public static void stop(ServerPlayer player, String message) {
        CompoundTag data = player.getPersistentData();
        if (!data.getBoolean(ACTIVE_KEY)) return;
        data.remove(ACTIVE_KEY);
        if (player.level() instanceof ServerLevel level) {
            TechniqueVfxPalette.profile("metal").impactAt(level, player.position().add(0.0D, 0.35D, 0.0D));
        }
        FlyingAuthority.revoke(player, FlyingAuthority.SOURCE_QI_FLYING, null, 0.0F);
        player.displayClientMessage(PlayerDisplayText.safeCatalogLiteral(message, "御剑飞行已停止。"), true);
    }
}
