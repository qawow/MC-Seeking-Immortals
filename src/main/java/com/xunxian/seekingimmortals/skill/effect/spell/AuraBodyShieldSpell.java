package com.xunxian.seekingimmortals.skill.effect.spell;

import com.xunxian.seekingimmortals.cultivation.PlayerCultivation;
import com.xunxian.seekingimmortals.skill.CultivationSkill;
import com.xunxian.seekingimmortals.skill.effect.SkillContext;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

public class AuraBodyShieldSpell extends SpellEffect {
    public static final String ACTIVE_KEY = "SeekingImmortalsAuraBodyShieldActive";

    public AuraBodyShieldSpell() {
        super(50, 300, 0.0D);
    }

    @Override
    public boolean execute(ServerPlayer player, PlayerCultivation cultivation, CultivationSkill skill, SkillContext context) {
        player.getPersistentData().putBoolean(ACTIVE_KEY, true);
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 200, 1, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 200, 0, false, true));
        if (player.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.ENCHANT,
                    player.getX(), player.getY() + 1.0D, player.getZ(),
                    24, 0.55D, 0.75D, 0.55D, 0.02D);
            level.playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.7F, 1.2F);
        }
        player.displayClientMessage(Component.translatable("message.seeking_immortals.spell.aura_body_shield"), true);
        return true;
    }
}
