package com.xunxian.seekingimmortals.skill.effect.spell;

import com.xunxian.seekingimmortals.cultivation.PlayerCultivation;
import com.xunxian.seekingimmortals.entity.SummonedServitorEntity;
import com.xunxian.seekingimmortals.skill.CultivationSkill;
import com.xunxian.seekingimmortals.skill.effect.SkillContext;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Dedicated command family: reinforce nearby owned summons/puppets, or self focus.
 */
public class CommandTechniqueSpell extends SpellEffect {
    private final double range;
    private final double radius;
    private final String element;
    private final Set<String> tags;
    private final String successKey;

    public CommandTechniqueSpell(int cost, int cooldownTicks, double damage, double range, double radius,
                                 String element, Set<String> tags, String successKey) {
        super(cost, cooldownTicks, Math.max(0.0D, damage));
        this.range = Math.max(8.0D, range);
        this.radius = Math.max(4.0D, radius);
        this.element = element == null ? "neutral" : element;
        this.tags = tags == null ? Set.of() : tags;
        this.successKey = successKey == null || successKey.isBlank()
                ? "message.seeking_immortals.spell.generic_command.success"
                : successKey;
    }

    @Override
    public boolean execute(ServerPlayer player, PlayerCultivation cultivation, CultivationSkill skill, SkillContext context) {
        ServerLevel level = player.serverLevel();
        double scan = Math.max(radius, Math.min(range, 18.0D));
        AABB area = player.getBoundingBox().inflate(scan, 4.0D, scan);
        List<SummonedServitorEntity> owned = level.getEntitiesOfClass(SummonedServitorEntity.class, area,
                entity -> entity.isAlive()
                        && entity.getOwnerUUID().map(id -> id.equals(player.getUUID())).orElse(false));

        int duration = 120 + skill.getLevel() * 8;
        int amp = Math.max(0, skill.getLevel() / 5);
        int reinforced = 0;
        double pulseDamage = Math.max(0.0D, calculateDamage(skill.getLevel(), skill.getProficiency()) * 0.35D);
        for (SummonedServitorEntity servitor : owned) {
            servitor.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, duration, 1 + amp, false, true));
            servitor.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, duration, amp, false, true));
            servitor.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, duration, Math.min(2, amp), false, true));
            if (tags.contains("flying_sword") || tags.contains("natal")
                    || element.toLowerCase(Locale.ROOT).contains("metal")) {
                servitor.addEffect(new MobEffectInstance(MobEffects.GLOWING, duration, 0, false, true));
            }
            if (pulseDamage > 0.0D && servitor.getTarget() != null && canAffect(player, servitor.getTarget())) {
                servitor.getTarget().hurt(player.damageSources().indirectMagic(player, player), (float) pulseDamage);
            }
            reinforced++;
        }

        if (reinforced == 0) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, duration, 0, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, duration, 0, false, true));
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.spell.generic_command.self_focus"), true);
        } else {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 0, false, true));
            player.displayClientMessage(Component.translatable(successKey, reinforced), true);
        }

        Vec3 center = player.position().add(0.0D, 1.0D, 0.0D);
        level.sendParticles(ParticleTypes.ENCHANT, center.x, center.y, center.z, 36, 0.8D, 0.5D, 0.8D, 0.0D);
        level.sendParticles(ParticleTypes.END_ROD, center.x, center.y, center.z, 12, 0.5D, 0.4D, 0.5D, 0.02D);
        level.playSound(null, player.blockPosition(), SoundEvents.NOTE_BLOCK_CHIME.value(),
                SoundSource.PLAYERS, 0.7F, 1.35F);
        return true;
    }
}
