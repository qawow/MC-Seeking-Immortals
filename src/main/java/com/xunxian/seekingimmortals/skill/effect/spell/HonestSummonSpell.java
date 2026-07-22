package com.xunxian.seekingimmortals.skill.effect.spell;

import com.xunxian.seekingimmortals.catalog.SummonHonestMvpService;
import com.xunxian.seekingimmortals.cultivation.PlayerCultivation;
import com.xunxian.seekingimmortals.entity.SummonedServitorEntity;
import com.xunxian.seekingimmortals.skill.CultivationSkill;
import com.xunxian.seekingimmortals.skill.effect.SkillContext;
import com.xunxian.seekingimmortals.util.PlayerDisplayText;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/**
 * Summon spell base: spawns a real servitor or dedicated cultivation beast.
 * A failed entity insertion is a failed cast; it never turns into a player buff.
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
                ? "message.seeking_immortals.summon.entity_spawned"
                : successKey;
    }

    @Override
    public boolean execute(ServerPlayer player, PlayerCultivation cultivation, CultivationSkill skill, SkillContext context) {
        int levelBonus = Math.max(0, skill.getLevel() - 1);
        int scaledDuration = durationTicks + levelBonus * 20;
        int scaledStrength = strengthAmp + Math.max(0, skill.getLevel() / 5);
        int scaledResist = resistAmp + Math.max(0, skill.getLevel() / 7);
        double health = 24.0D + scaledStrength * 8.0D + scaledResist * 4.0D;
        double damage = 4.0D + scaledStrength * 1.6D + Math.max(0, skill.getLevel() / 4) * 0.5D;
        SummonedServitorEntity.Archetype archetype = SummonHonestMvpService.archetypeOf(summonId);

        boolean spawned = SummonHonestMvpService.spawnConfigured(
                player, summonId, scaledDuration, health, damage, archetype);

        if (!spawned) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.summon.entity_failed", summonDisplay(archetype)), true);
            return false;
        }
        if (player.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.SOUL, player.getX(), player.getY() + 1.0D, player.getZ(),
                    24, 0.5D, 0.4D, 0.5D, 0.02D);
            level.playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.7F, 0.9F);
        }
        // Brief focus buff only after a real entity exists; combat authority remains the entity.
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, 0, false, true));
        player.displayClientMessage(Component.translatable(successKey, summonDisplay(archetype)), true);
        player.displayClientMessage(Component.translatable(
                "message.seeking_immortals.summon.archetype", archetypeDisplay(archetype)), false);
        return true;
    }

    private Component summonDisplay(SummonedServitorEntity.Archetype archetype) {
        String itemKey = "item.seeking_immortals." + PlayerDisplayText.normalizeId(summonId);
        if (PlayerDisplayText.hasTranslation(itemKey)) {
            return Component.translatable(itemKey);
        }
        return SummonHonestMvpService.findPuppet(summonId)
                .filter(entry -> PlayerDisplayText.isSafe(entry.display()))
                .map(entry -> (Component) Component.literal(entry.display().trim()))
                .orElseGet(() -> archetypeDisplay(archetype));
    }

    private static Component archetypeDisplay(SummonedServitorEntity.Archetype archetype) {
        String suffix = switch (archetype == null ? SummonedServitorEntity.Archetype.GENERIC : archetype) {
            case BEAST -> "beast";
            case PUPPET -> "puppet";
            case GHOST -> "ghost";
            case GENERIC -> "generic";
        };
        return Component.translatable("message.seeking_immortals.summon.archetype." + suffix);
    }
}
