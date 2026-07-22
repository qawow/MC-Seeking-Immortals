package com.xunxian.seekingimmortals.skill.effect.spell;

import com.xunxian.seekingimmortals.cultivation.PlayerCultivation;
import com.xunxian.seekingimmortals.entity.CultivationBeastEntity;
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

/**
 * Dedicated craft_gate family: short crafting assist + reinforce nearby owned puppets and beasts.
 * Always succeeds with a clear message even without nearby targets.
 */
public class CraftGateTechniqueSpell extends SpellEffect {
    private final double range;
    private final double radius;
    private final Set<String> tags;
    private final String successKey;

    public CraftGateTechniqueSpell(int cost, int cooldownTicks, double damage, double range, double radius,
                                   Set<String> tags, String successKey) {
        super(cost, cooldownTicks, Math.max(0.0D, damage));
        this.range = Math.max(6.0D, range);
        this.radius = Math.max(3.5D, radius);
        this.tags = tags == null ? Set.of() : tags;
        this.successKey = successKey == null || successKey.isBlank()
                ? "message.seeking_immortals.spell.generic_craft_gate.success"
                : successKey;
    }

    @Override
    public boolean execute(ServerPlayer player, PlayerCultivation cultivation, CultivationSkill skill, SkillContext context) {
        ServerLevel level = player.serverLevel();
        int craftTicks = 160 + skill.getLevel() * 10;
        player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, craftTicks, 1 + Math.max(0, skill.getLevel() / 6), false, true));
        player.addEffect(new MobEffectInstance(MobEffects.LUCK, craftTicks, 0, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, craftTicks, 0, false, true));

        double scan = Math.max(radius, Math.min(range, 16.0D));
        AABB area = player.getBoundingBox().inflate(scan, 3.5D, scan);
        List<SummonedServitorEntity> owned = level.getEntitiesOfClass(SummonedServitorEntity.class, area,
                entity -> entity.isAlive()
                        && entity.getOwnerUUID().map(id -> id.equals(player.getUUID())).orElse(false));
        List<CultivationBeastEntity> ownedBeasts = level.getEntitiesOfClass(CultivationBeastEntity.class, area,
                entity -> entity.isAlive() && entity.isCompanion()
                        && entity.getOwnerUUID().map(id -> id.equals(player.getUUID())).orElse(false));

        int duration = 140 + skill.getLevel() * 8;
        int bound = 0;
        boolean preferBeast = tags.stream().anyMatch(tag -> tag.contains("beast") || tag.contains("puppet"));
        for (SummonedServitorEntity servitor : owned) {
            SummonedServitorEntity.Archetype archetype = servitor.getArchetype();
            boolean puppetLike = archetype == SummonedServitorEntity.Archetype.PUPPET
                    || archetype == SummonedServitorEntity.Archetype.BEAST
                    || !preferBeast;
            if (!puppetLike && preferBeast) {
                continue;
            }
            servitor.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, duration, 1, false, true));
            servitor.addEffect(new MobEffectInstance(MobEffects.REGENERATION, duration, 0, false, true));
            servitor.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, duration, 0, false, true));
            bound++;
        }
        for (CultivationBeastEntity beast : ownedBeasts) {
            beast.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, duration, 1, false, true));
            beast.addEffect(new MobEffectInstance(MobEffects.REGENERATION, duration, 0, false, true));
            beast.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, duration, 0, false, true));
            bound++;
        }

        cultivation.addSpiritualPower(Math.max(2, 3 + skill.getLevel() / 2));
        level.sendParticles(ParticleTypes.CRIT, player.getX(), player.getY() + 1.0D, player.getZ(),
                28, 0.5D, 0.4D, 0.5D, 0.02D);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, player.getX(), player.getY() + 0.8D, player.getZ(),
                16, 0.5D, 0.3D, 0.5D, 0.0D);
        level.playSound(null, player.blockPosition(), SoundEvents.ANVIL_USE, SoundSource.PLAYERS, 0.45F, 1.4F);

        if (bound > 0) {
            player.displayClientMessage(Component.translatable(successKey, bound), true);
        } else {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.spell.generic_craft_gate.no_target"), true);
        }
        return true;
    }
}
