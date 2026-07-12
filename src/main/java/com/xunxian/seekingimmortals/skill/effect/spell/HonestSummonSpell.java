package com.xunxian.seekingimmortals.skill.effect.spell;

import com.xunxian.seekingimmortals.catalog.SummonHonestMvpService;
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

/**
 * Honest summon spell base: no real entity. Applies temporary combat proxy buffs and
 * routes through SummonHonestMvpService messaging.
 */
public class HonestSummonSpell extends SpellEffect {
    private final String summonId;
    private final int strengthAmp;
    private final int resistAmp;
    private final int durationTicks;
    private final String successKey;

    public HonestSummonSpell(int cost, int cooldown, String summonId, int strengthAmp, int resistAmp, int durationTicks, String successKey) {
        super(cost, cooldown, 0.0D);
        this.summonId = summonId == null ? "summon" : summonId;
        this.strengthAmp = Math.max(0, strengthAmp);
        this.resistAmp = Math.max(0, resistAmp);
        this.durationTicks = Math.max(40, durationTicks);
        this.successKey = successKey == null || successKey.isBlank()
                ? "message.seeking_immortals.summon.honest_mvp"
                : successKey;
    }

    @Override
    public boolean execute(ServerPlayer player, PlayerCultivation cultivation, CultivationSkill skill, SkillContext context) {
        int levelBonus = Math.max(0, skill.getLevel() - 1);
        int scaledDuration = durationTicks + levelBonus * 20;
        int scaledStrength = strengthAmp + Math.max(0, skill.getLevel() / 5);
        int scaledResist = resistAmp + Math.max(0, skill.getLevel() / 7);
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, scaledDuration, scaledStrength, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, scaledDuration, scaledResist, false, true));
        if (scaledStrength >= 1) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, scaledDuration, 0, false, true));
        }
        if (player.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.SOUL, player.getX(), player.getY() + 1.0D, player.getZ(),
                    24, 0.5D, 0.4D, 0.5D, 0.02D);
            level.playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.7F, 0.9F);
        }
        SummonHonestMvpService.summonProxy(player, summonId);
        player.displayClientMessage(Component.translatable(successKey, summonId), true);
        return true;
    }
}
