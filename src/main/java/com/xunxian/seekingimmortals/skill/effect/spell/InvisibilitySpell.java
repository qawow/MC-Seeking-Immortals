package com.xunxian.seekingimmortals.skill.effect.spell;

import com.xunxian.seekingimmortals.cultivation.PlayerCultivation;
import com.xunxian.seekingimmortals.skill.CultivationSkill;
import com.xunxian.seekingimmortals.skill.effect.SkillContext;
import com.xunxian.seekingimmortals.skill.effect.TechniqueVfxPalette;
import net.minecraft.server.level.ServerLevel;
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
        if (context.getLevel() instanceof ServerLevel level) {
            TechniqueVfxPalette.Profile vfx = TechniqueVfxPalette.profile("illusion");
            vfx.castAt(level, player);
            vfx.auraAt(level, player, 0.9D, 28);
        }

        player.displayClientMessage(
            net.minecraft.network.chat.Component.translatable("message.seeking_immortals.spell.invisibility", duration / 20),
            true
        );

        return true;
    }
}
