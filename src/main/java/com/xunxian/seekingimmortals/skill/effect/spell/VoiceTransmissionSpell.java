package com.xunxian.seekingimmortals.skill.effect.spell;

import com.xunxian.seekingimmortals.cultivation.PlayerCultivation;
import com.xunxian.seekingimmortals.skill.CultivationSkill;
import com.xunxian.seekingimmortals.skill.effect.SkillContext;
import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

public class VoiceTransmissionSpell extends SpellEffect {
    private final double baseRange;

    public VoiceTransmissionSpell(int cost, int cooldownTicks, double baseRange) {
        super(cost, cooldownTicks, 0.0D);
        this.baseRange = baseRange;
    }

    @Override
    public boolean execute(ServerPlayer player, PlayerCultivation cultivation, CultivationSkill skill, SkillContext context) {
        ServerLevel level = player.serverLevel();
        double range = baseRange + Math.max(0, skill.getLevel() - 1) * 4.0D;
        double rangeSqr = range * range;
        List<ServerPlayer> recipients = level.getPlayers(target -> target != player && target.distanceToSqr(player) <= rangeSqr);

        for (ServerPlayer recipient : recipients) {
            recipient.displayClientMessage(Component.translatable("message.seeking_immortals.spell.voice_transmission.heard", player.getDisplayName()), false);
            level.playSound(null, recipient.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.35F, 1.8F);
        }

        level.sendParticles(ParticleTypes.NOTE, player.getX(), player.getY() + 1.6D, player.getZ(),
                8, 0.25D, 0.2D, 0.25D, 0.02D);
        if (recipients.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.spell.voice_transmission.no_recipient"), true);
        } else {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.spell.voice_transmission.success", recipients.size()), true);
        }
        return true;
    }
}
