package com.xunxian.seekingimmortals.skill.effect.spell;

import com.xunxian.seekingimmortals.combat.status.StatusRegistry;
import com.xunxian.seekingimmortals.cultivation.PlayerCultivation;
import com.xunxian.seekingimmortals.registry.ModMobEffects;
import com.xunxian.seekingimmortals.skill.CultivationSkill;
import com.xunxian.seekingimmortals.skill.effect.SkillContext;
import com.xunxian.seekingimmortals.skill.effect.TechniqueVfxPalette;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraftforge.registries.RegistryObject;

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
    private final String statusId;
    private final int statusDurationTicks;
    private final int statusAmplifier;
    private final String lazyEffectType;
    private final String lazyElement;

    public SelfBuffSpell(int cost, int cooldownTicks,
                         MobEffect primaryEffect, int primaryDurationTicks, int primaryAmplifier,
                         MobEffect secondaryEffect, int secondaryDurationTicks, int secondaryAmplifier,
                         ParticleOptions particle, SoundEvent sound, String successKey) {
        this(cost, cooldownTicks,
                primaryEffect, primaryDurationTicks, primaryAmplifier,
                secondaryEffect, secondaryDurationTicks, secondaryAmplifier,
                particle, sound, successKey, "", 0, 0);
    }

    public SelfBuffSpell(int cost, int cooldownTicks,
                         MobEffect primaryEffect, int primaryDurationTicks, int primaryAmplifier,
                         MobEffect secondaryEffect, int secondaryDurationTicks, int secondaryAmplifier,
                         ParticleOptions particle, SoundEvent sound, String successKey,
                         String statusId, int statusDurationTicks, int statusAmplifier) {
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
        this.statusId = statusId == null ? "" : statusId;
        this.statusDurationTicks = statusDurationTicks;
        this.statusAmplifier = statusAmplifier;
        this.lazyEffectType = "";
        this.lazyElement = "";
    }

    public SelfBuffSpell(int cost, int cooldownTicks, String effectType, String element, String successKey) {
        super(cost, cooldownTicks, 0.0D);
        this.primaryEffect = null;
        this.primaryDurationTicks = 160;
        this.primaryAmplifier = "shield".equals(effectType) ? 1 : 0;
        this.secondaryEffect = null;
        this.secondaryDurationTicks = 140;
        this.secondaryAmplifier = 0;
        this.particle = null;
        this.sound = null;
        this.successKey = successKey;
        this.statusId = "";
        this.statusDurationTicks = 0;
        this.statusAmplifier = 0;
        this.lazyEffectType = effectType == null ? "buff" : effectType;
        this.lazyElement = element == null ? "neutral" : element;
    }

    @Override
    public boolean execute(ServerPlayer player, PlayerCultivation cultivation, CultivationSkill skill, SkillContext context) {
        TechniqueVfxPalette.Profile lazyProfile = lazyElement.isBlank()
                ? null
                : TechniqueVfxPalette.profile(lazyElement);
        MobEffect resolvedPrimary = primaryEffect;
        MobEffect resolvedSecondary = secondaryEffect;
        if (lazyProfile != null) {
            resolvedPrimary = switch (lazyEffectType) {
                case "scan", "scout", "inspect" -> registeredEffect("conceal_qi");
                case "shield" -> registeredEffect("shield");
                default -> lazyProfile.buffPrimary();
            };
            resolvedSecondary = switch (lazyEffectType) {
                case "shield" -> registeredEffect("heal_hot");
                default -> lazyProfile.buffSecondary();
            };
        }
        applyEffect(player, resolvedPrimary, primaryDurationTicks, primaryAmplifier, skill);
        applyEffect(player, resolvedSecondary, secondaryDurationTicks, secondaryAmplifier, skill);
        if (!statusId.isBlank() && statusDurationTicks > 0) {
            StatusRegistry.applyStatus(player, statusId, statusAmplifier,
                    scaledStatusDuration(statusDurationTicks, skill.getLevel()));
        }
        if (player.level() instanceof ServerLevel level) {
            if (lazyProfile != null) {
                lazyProfile.castAt(level, player);
                lazyProfile.auraAt(level, player, 0.95D, 24);
            } else if (particle != null && sound != null) {
                level.sendParticles(particle,
                        player.getX(), player.getY() + 1.0D, player.getZ(),
                        28, 0.55D, 0.7D, 0.55D, 0.02D);
                level.playSound(null, player.blockPosition(), sound, SoundSource.PLAYERS, 0.65F, 1.2F);
            }
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

    private static MobEffect registeredEffect(String id) {
        RegistryObject<MobEffect> effect = ModMobEffects.get(id);
        return effect == null ? null : effect.get();
    }

    static int scaledStatusDuration(int baseDurationTicks, int skillLevel) {
        return Math.max(1, baseDurationTicks) + Math.max(0, skillLevel - 1) * 20;
    }
}
