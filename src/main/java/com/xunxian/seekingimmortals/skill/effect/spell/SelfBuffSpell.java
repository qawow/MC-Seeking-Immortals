package com.xunxian.seekingimmortals.skill.effect.spell;

import com.xunxian.seekingimmortals.combat.status.StatusRegistry;
import com.xunxian.seekingimmortals.cultivation.PlayerCultivation;
import com.xunxian.seekingimmortals.network.TechniqueVfxPacket;
import com.xunxian.seekingimmortals.registry.ModMobEffects;
import com.xunxian.seekingimmortals.skill.CultivationSkill;
import com.xunxian.seekingimmortals.skill.effect.ActiveTechniqueEffectVfxService;
import com.xunxian.seekingimmortals.skill.effect.SkillContext;
import com.xunxian.seekingimmortals.skill.effect.TechniqueLifecycleVfxService;
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
    private final String authoredSemantic;
    private final String authoredEffectType;
    private final String authoredElement;

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
        this(cost, cooldownTicks,
                primaryEffect, primaryDurationTicks, primaryAmplifier,
                secondaryEffect, secondaryDurationTicks, secondaryAmplifier,
                particle, sound, successKey, statusId, statusDurationTicks, statusAmplifier,
                "", "", "", "", "");
    }

    public SelfBuffSpell(int cost, int cooldownTicks,
                         MobEffect primaryEffect, int primaryDurationTicks, int primaryAmplifier,
                         MobEffect secondaryEffect, int secondaryDurationTicks, int secondaryAmplifier,
                         ParticleOptions particle, SoundEvent sound, String successKey,
                         String authoredSemantic, String authoredEffectType, String authoredElement) {
        this(cost, cooldownTicks,
                primaryEffect, primaryDurationTicks, primaryAmplifier,
                secondaryEffect, secondaryDurationTicks, secondaryAmplifier,
                particle, sound, successKey, "", 0, 0,
                "", "", authoredSemantic, authoredEffectType, authoredElement);
    }

    private SelfBuffSpell(int cost, int cooldownTicks,
                          MobEffect primaryEffect, int primaryDurationTicks, int primaryAmplifier,
                          MobEffect secondaryEffect, int secondaryDurationTicks, int secondaryAmplifier,
                          ParticleOptions particle, SoundEvent sound, String successKey,
                          String statusId, int statusDurationTicks, int statusAmplifier,
                          String lazyEffectType, String lazyElement,
                          String authoredSemantic, String authoredEffectType, String authoredElement) {
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
        this.lazyEffectType = lazyEffectType == null ? "" : lazyEffectType;
        this.lazyElement = lazyElement == null ? "" : lazyElement;
        this.authoredSemantic = authoredSemantic == null ? "" : authoredSemantic;
        this.authoredEffectType = authoredEffectType == null ? "" : authoredEffectType;
        this.authoredElement = authoredElement == null ? "" : authoredElement;
    }

    public SelfBuffSpell(int cost, int cooldownTicks, String effectType, String element, String successKey) {
        this(cost, cooldownTicks, effectType, element, successKey, "");
    }

    public SelfBuffSpell(int cost, int cooldownTicks, String effectType, String element,
                         String successKey, String authoredSemantic) {
        this(cost, cooldownTicks,
                null, 160, "shield".equals(effectType) ? 1 : 0,
                null, 140, 0,
                null, null, successKey, "", 0, 0,
                effectType == null ? "buff" : effectType,
                element == null ? "neutral" : element,
                authoredSemantic, effectType, element);
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
        TechniqueVfxPalette.Family fallbackFamily = lazyProfile == null
                ? TechniqueVfxPalette.familyOf(authoredElement + " " + statusId + " " + successKey)
                : lazyProfile.family();
        String semantic = resolveVisualSemantic(
                skill, authoredSemantic, lazyEffectType + " " + successKey);
        TechniqueVfxPalette.Family family = resolveVisualFamily(
                skill, authoredElement, fallbackFamily);
        String motifType = authoredEffectType.isBlank() ? lazyEffectType : authoredEffectType;
        TechniqueVfxPacket.Motif motif = TechniqueLifecycleVfxService.selfBuffMotif(
                semantic + " " + motifType + " " + successKey, statusId);
        int primaryVisualDuration = scaledEffectDuration(primaryDurationTicks, skill.getLevel());
        int secondaryVisualDuration = scaledEffectDuration(secondaryDurationTicks, skill.getLevel());
        boolean primaryApplied = applyEffect(player, resolvedPrimary,
                primaryDurationTicks, primaryAmplifier, skill);
        boolean secondaryApplied = applyEffect(player, resolvedSecondary,
                secondaryDurationTicks, secondaryAmplifier, skill);
        MobEffect resolvedStatus = null;
        boolean statusApplied = false;
        int statusVisualDuration = 0;
        if (!statusId.isBlank() && statusDurationTicks > 0) {
            statusVisualDuration = scaledStatusDuration(statusDurationTicks, skill.getLevel());
            statusApplied = StatusRegistry.applyStatus(player, statusId, statusAmplifier,
                    statusVisualDuration);
            if (statusApplied) {
                resolvedStatus = StatusRegistry.resolve(statusId).orElse(null);
            }
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
        if (primaryApplied) {
            TechniqueLifecycleVfxService.trackSelfBuff(
                    player, semantic, resolvedPrimary, primaryVisualDuration, family, motif);
        }
        if (secondaryApplied) {
            TechniqueLifecycleVfxService.trackSelfBuff(
                    player, semantic, resolvedSecondary, secondaryVisualDuration, family, motif);
        }
        if (statusApplied && resolvedStatus != null) {
            TechniqueLifecycleVfxService.trackSelfBuff(
                    player, semantic, resolvedStatus, statusVisualDuration, family, motif);
        }
        player.displayClientMessage(Component.translatable(successKey), true);
        return true;
    }

    private static boolean applyEffect(ServerPlayer player, MobEffect effect, int durationTicks, int amplifier,
                                       CultivationSkill skill) {
        if (effect == null || durationTicks <= 0) {
            return false;
        }
        int scaledDuration = scaledEffectDuration(durationTicks, skill.getLevel());
        int scaledAmplifier = amplifier + Math.max(0, skill.getLevel() / 5);
        boolean applied = player.addEffect(
                new MobEffectInstance(effect, scaledDuration, scaledAmplifier, false, true));
        return applied && player.hasEffect(effect);
    }

    private static MobEffect registeredEffect(String id) {
        RegistryObject<MobEffect> effect = ModMobEffects.get(id);
        return effect == null ? null : effect.get();
    }

    static int scaledStatusDuration(int baseDurationTicks, int skillLevel) {
        return Math.max(1, baseDurationTicks) + Math.max(0, skillLevel - 1) * 20;
    }

    static String resolveVisualSemantic(CultivationSkill skill,
                                        String authoredSemantic,
                                        String fallback) {
        return authoredSemantic == null || authoredSemantic.isBlank()
                ? ActiveTechniqueEffectVfxService.semantic(skill, fallback)
                : ActiveTechniqueEffectVfxService.semantic(null, authoredSemantic);
    }

    static TechniqueVfxPalette.Family resolveVisualFamily(
            CultivationSkill skill,
            String authoredElement,
            TechniqueVfxPalette.Family fallback) {
        return authoredElement == null || authoredElement.isBlank()
                ? ActiveTechniqueEffectVfxService.familyForSkill(skill, fallback)
                : TechniqueVfxPalette.familyOf(authoredElement);
    }

    private static int scaledEffectDuration(int baseDurationTicks, int skillLevel) {
        return Math.max(0, baseDurationTicks) + Math.max(0, skillLevel - 1) * 20;
    }
}
