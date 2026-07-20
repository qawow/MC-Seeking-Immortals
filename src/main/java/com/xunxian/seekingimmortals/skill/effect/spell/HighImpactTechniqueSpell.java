package com.xunxian.seekingimmortals.skill.effect.spell;

import com.xunxian.seekingimmortals.cultivation.PlayerCultivation;
import com.xunxian.seekingimmortals.skill.CultivationSkill;
import com.xunxian.seekingimmortals.skill.effect.SkillContext;
import com.xunxian.seekingimmortals.skill.effect.TechniqueVfxPalette;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Dedicated ultimate / secret_art family: high-impact battlefield effects.
 */
public class HighImpactTechniqueSpell extends SpellEffect {
    public enum Form {
        ULTIMATE(1.85D, 1.35D),
        SECRET_ART(1.60D, 1.20D);

        private final double damageMultiplier;
        private final double radiusMultiplier;

        Form(double damageMultiplier, double radiusMultiplier) {
            this.damageMultiplier = damageMultiplier;
            this.radiusMultiplier = radiusMultiplier;
        }
    }

    private final Form form;
    private final double range;
    private final double radius;
    private final String target;
    private final String element;
    private final String effectKey;
    private final Set<String> tags;
    private final String castBias;
    private final String successKey;

    public HighImpactTechniqueSpell(int cost, int cooldownTicks, double damage, double range, double radius,
                                    String target, String element, String effectKey, Set<String> tags,
                                    String castBias, Form form, String successKey) {
        super(cost, cooldownTicks, Math.max(20.0D, damage));
        this.form = form == null ? Form.ULTIMATE : form;
        this.range = Math.max(10.0D, range);
        this.radius = Math.max(3.5D, radius) * this.form.radiusMultiplier;
        this.target = target == null ? "" : target;
        this.element = element == null ? "neutral" : element;
        this.effectKey = effectKey == null ? "" : effectKey;
        this.tags = tags == null ? Set.of() : tags;
        this.castBias = castBias == null ? "" : castBias;
        this.successKey = successKey == null || successKey.isBlank()
                ? (this.form == Form.SECRET_ART
                ? "message.seeking_immortals.spell.generic_secret_art.success"
                : "message.seeking_immortals.spell.generic_ultimate.success")
                : successKey;
    }

    @Override
    public boolean execute(ServerPlayer player, PlayerCultivation cultivation, CultivationSkill skill, SkillContext context) {
        ServerLevel level = player.serverLevel();
        boolean beam = isBeam();
        Vec3 center = beam ? player.getEyePosition().add(player.getLookAngle().scale(Math.min(range, 16.0D)))
                : findImpactPoint(level, player);
        double effectiveRadius = beam ? Math.max(2.2D, radius * 0.55D) : radius;
        List<LivingEntity> targets = findTargets(level, player, center, effectiveRadius, beam);

        double damage = calculateDamage(skill.getLevel(), skill.getProficiency()) * form.damageMultiplier;
        int hitCount = 0;
        for (LivingEntity entity : targets) {
            double falloff = Math.max(0.50D, 1.0D - entity.position().distanceTo(center) / (effectiveRadius + 0.8D));
            entity.hurt(player.damageSources().indirectMagic(player, player), (float) (damage * falloff));
            applyControl(entity, skill);
            applyElementExtra(entity, skill);
            hitCount++;
        }

        int selfTicks = 80 + skill.getLevel() * 6;
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, selfTicks, Math.max(0, skill.getLevel() / 6), false, true));
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, selfTicks,
                form == Form.ULTIMATE ? 1 : 0, false, true));
        if (tags.contains("curse") || tags.contains("demonic") || element.toLowerCase(Locale.ROOT).contains("blood")) {
            player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, selfTicks, 1, false, true));
        }

        spawnVisual(level, center, effectiveRadius, beam);
        level.playSound(null, player.blockPosition(),
                beam ? SoundEvents.TRIDENT_THUNDER : SoundEvents.GENERIC_EXPLODE,
                SoundSource.PLAYERS, 0.85F, form == Form.ULTIMATE ? 0.85F : 1.05F);
        player.displayClientMessage(Component.translatable(successKey, hitCount,
                String.format(Locale.ROOT, "%.1f", damage)), true);
        return true;
    }

    private boolean isBeam() {
        String blob = (castBias + " " + effectKey + " " + String.join(" ", tags) + " " + target)
                .toLowerCase(Locale.ROOT);
        return blob.contains("beam") || blob.contains("flash") || blob.contains("slash");
    }

    private Vec3 findImpactPoint(ServerLevel level, ServerPlayer player) {
        Vec3 start = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        if (look.lengthSqr() < 0.001D) {
            return player.position();
        }
        Vec3 end = start.add(look.normalize().scale(range));
        BlockHitResult hit = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, player));
        if (hit.getType() == HitResult.Type.MISS) {
            return end;
        }
        return hit.getLocation();
    }

    private List<LivingEntity> findTargets(ServerLevel level, ServerPlayer player, Vec3 center,
                                           double effectiveRadius, boolean beam) {
        if (beam) {
            Vec3 start = player.getEyePosition();
            Vec3 look = player.getLookAngle().normalize();
            AABB box = player.getBoundingBox().expandTowards(look.scale(range)).inflate(effectiveRadius);
            return level.getEntitiesOfClass(LivingEntity.class, box, entity -> canAffect(player, entity))
                    .stream()
                    .filter(entity -> {
                        Vec3 to = entity.getEyePosition().subtract(start);
                        double proj = to.dot(look);
                        if (proj < 0.0D || proj > range) {
                            return false;
                        }
                        Vec3 closest = start.add(look.scale(proj));
                        return entity.position().distanceToSqr(closest) <= (effectiveRadius + entity.getBbWidth())
                                * (effectiveRadius + entity.getBbWidth());
                    })
                    .sorted(Comparator.comparingDouble(entity -> entity.distanceToSqr(player)))
                    .limit(14)
                    .toList();
        }
        AABB area = new AABB(center, center).inflate(effectiveRadius, 2.8D, effectiveRadius);
        return level.getEntitiesOfClass(LivingEntity.class, area,
                        entity -> canAffect(player, entity)
                                && entity.position().distanceToSqr(center)
                                <= (effectiveRadius + entity.getBbWidth()) * (effectiveRadius + entity.getBbWidth()))
                .stream()
                .sorted(Comparator.comparingDouble(entity -> entity.distanceToSqr(center)))
                .limit(16)
                .toList();
    }

    private void applyControl(LivingEntity target, CultivationSkill skill) {
        int bonus = Math.max(0, skill.getLevel() - 1);
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 90 + bonus * 5, 3, false, true));
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80 + bonus * 4, 1, false, true));
        if (form == Form.ULTIMATE) {
            target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 35 + bonus * 2, 0, false, true));
        }
    }

    private void applyElementExtra(LivingEntity target, CultivationSkill skill) {
        TechniqueVfxPalette.Profile vfx = TechniqueVfxPalette.profile(element);
        int bonus = Math.max(0, skill.getLevel() / 3);
        switch (vfx.family()) {
            case FIRE -> target.setSecondsOnFire(5 + bonus);
            case ICE -> target.setTicksFrozen(Math.min(260, target.getTicksFrozen() + 100 + bonus * 8));
            case THUNDER, LIGHT -> target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 60 + bonus * 4, 0, false, true));
            case BLOOD, DARK, SOUL -> target.addEffect(new MobEffectInstance(vfx.primaryDebuff(), 70 + bonus * 4, 1, false, true));
            case VOID -> target.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 20 + bonus, 0, false, true));
            case WOOD -> target.addEffect(new MobEffectInstance(MobEffects.POISON, 70 + bonus * 4, 0, false, true));
            case ILLUSION -> target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 80 + bonus * 4, 0, false, true));
            default -> {
                if (tags.contains("curse") || tags.contains("demonic")) {
                    target.addEffect(new MobEffectInstance(MobEffects.WITHER, 70, 1, false, true));
                }
            }
        }
    }

    private void spawnVisual(ServerLevel level, Vec3 center, double effectiveRadius, boolean beam) {
        TechniqueVfxPalette.Profile vfx = TechniqueVfxPalette.profile(element);
        vfx.burst(level, center, effectiveRadius, form == Form.ULTIMATE ? 96 : 72);
        level.sendParticles(ParticleTypes.FLASH, center.x, center.y + 0.5D, center.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        if (beam) {
            level.sendParticles(vfx.accent(), center.x, center.y + 0.3D, center.z, 28, 0.2D, 0.2D, 0.2D, 0.05D);
            level.sendParticles(ParticleTypes.END_ROD, center.x, center.y + 0.35D, center.z, 12, 0.15D, 0.15D, 0.15D, 0.03D);
        } else {
            level.sendParticles(ParticleTypes.EXPLOSION, center.x, center.y + 0.2D, center.z, 2, 0.1D, 0.1D, 0.1D, 0.0D);
            level.sendParticles(vfx.core(), center.x, center.y + 0.25D, center.z, 36,
                    effectiveRadius * 0.3D, 0.35D, effectiveRadius * 0.3D, 0.02D);
        }
        level.playSound(null, center.x, center.y, center.z, vfx.impactSound(),
                SoundSource.PLAYERS, 0.8F, form == Form.ULTIMATE ? vfx.impactPitch() * 0.9F : vfx.impactPitch());
    }
}
