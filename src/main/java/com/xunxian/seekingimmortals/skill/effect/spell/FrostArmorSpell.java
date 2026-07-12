package com.xunxian.seekingimmortals.skill.effect.spell;

import com.xunxian.seekingimmortals.cultivation.PlayerCultivation;
import com.xunxian.seekingimmortals.skill.CultivationSkill;
import com.xunxian.seekingimmortals.skill.effect.SkillContext;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import org.joml.Vector3f;

public class FrostArmorSpell extends SpellEffect {
    private static final DustParticleOptions FROST_EDGE = new DustParticleOptions(new Vector3f(0.50F, 0.86F, 1.00F), 0.82F);
    private static final DustParticleOptions FROST_CORE = new DustParticleOptions(new Vector3f(0.90F, 1.00F, 1.00F), 0.55F);

    public FrostArmorSpell() {
        super(8, 180, 0.0D);
    }

    @Override
    public boolean execute(ServerPlayer player, PlayerCultivation cultivation, CultivationSkill skill, SkillContext context) {
        int duration = 150 + Math.max(0, skill.getLevel() - 1) * 18;
        int resistanceAmplifier = Math.max(0, skill.getLevel() / 6);
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, duration, resistanceAmplifier, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, Math.max(80, duration - 30), 0, false, true));
        if (player.level() instanceof ServerLevel level) {
            spawnFrostArmor(level, player);
            level.playSound(null, player.blockPosition(), SoundEvents.GLASS_PLACE, SoundSource.PLAYERS, 0.62F, 1.45F);
        }
        player.displayClientMessage(Component.translatable("message.seeking_immortals.spell.frost_armor.success"), true);
        return true;
    }

    private void spawnFrostArmor(ServerLevel level, ServerPlayer player) {
        double centerX = player.getX();
        double centerY = player.getY() + 0.95D;
        double centerZ = player.getZ();
        for (int ring = 0; ring < 3; ring++) {
            double y = centerY + (ring - 1) * 0.48D;
            double radius = 0.72D - ring * 0.06D;
            for (int i = 0; i < 28; i++) {
                double angle = (Math.PI * 2.0D * i) / 28.0D + ring * 0.42D;
                double x = centerX + Math.cos(angle) * radius;
                double z = centerZ + Math.sin(angle) * radius;
                level.sendParticles(FROST_EDGE, x, y, z, 1, 0.02D, 0.02D, 0.02D, 0.0D);
            }
        }
        level.sendParticles(FROST_CORE, centerX, centerY, centerZ, 22, 0.42D, 0.82D, 0.42D, 0.015D);
    }
}
