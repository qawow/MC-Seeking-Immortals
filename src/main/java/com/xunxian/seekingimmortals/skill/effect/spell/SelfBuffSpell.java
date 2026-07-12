package com.xunxian.seekingimmortals.skill.effect.spell;

import com.xunxian.seekingimmortals.cultivation.PlayerCultivation;
import com.xunxian.seekingimmortals.skill.CultivationSkill;
import com.xunxian.seekingimmortals.skill.effect.SkillContext;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;

public class SelfBuffSpell extends SpellEffect {
    private final MobEffect primaryEffect;
    private final int primaryDurationTicks;
    private final int primaryAmplifier;
    private final MobEffect secondaryEffect;
    private final int secondaryDurationTicks;
    private final int secondaryAmplifier;
    private final ParticleOptions particle;
    private final SoundEvent sound;
    private final String successKey;

    public SelfBuffSpell(int cost, int cooldownTicks,
                         MobEffect primaryEffect, int primaryDurationTicks, int primaryAmplifier,
                         MobEffect secondaryEffect, int secondaryDurationTicks, int secondaryAmplifier,
                         ParticleOptions particle, SoundEvent sound, String successKey) {
        super(cost, cooldownTicks, 0.0D);
        this.primaryEffect = primaryEffect;
        this.primaryDurationTicks = primaryDurationTicks;
        this.primaryAmplifier = primaryAmplifier;
        this.secondaryEffect = secondaryEffect;
        this.secondaryDurationTicks = secondaryDurationTicks;
        this.secondaryAmplifier = secondaryAmplifier;
        this.particle = particle;
        this.sound = sound;
        this.successKey = successKey;
    }

    @Override
    public boolean execute(ServerPlayer player, PlayerCultivation cultivation, CultivationSkill skill, SkillContext context) {
        applyEffect(player, primaryEffect, primaryDurationTicks, primaryAmplifier, skill);
        applyEffect(player, secondaryEffect, secondaryDurationTicks, secondaryAmplifier, skill);
        if (player.level() instanceof ServerLevel level) {
            level.sendParticles(particle,
                    player.getX(), player.getY() + 1.0D, player.getZ(),
                    28, 0.55D, 0.7D, 0.55D, 0.02D);
            level.playSound(null, player.blockPosition(), sound, SoundSource.PLAYERS, 0.65F, 1.2F);
        }
        player.displayClientMessage(Component.translatable(successKey), true);
        return true;
    }

    private static void applyEffect(ServerPlayer player, MobEffect effect, int durationTicks, int amplifier, CultivationSkill skill) {
        if (effect == null || durationTicks <= 0) {
            return;
        }
        int scaledDuration = durationTicks + Math.max(0, skill.getLevel() - 1) * 20;
        int scaledAmplifier = amplifier + Math.max(0, skill.getLevel() / 5);
        player.addEffect(new MobEffectInstance(effect, scaledDuration, scaledAmplifier, false, true));
    }
}
