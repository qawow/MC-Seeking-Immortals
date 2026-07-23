package com.xunxian.seekingimmortals.skill.effect.spell;

import com.xunxian.seekingimmortals.cultivation.PlayerCultivation;
import com.xunxian.seekingimmortals.entity.CultivationFireballEntity;
import com.xunxian.seekingimmortals.skill.CultivationSkill;
import com.xunxian.seekingimmortals.skill.effect.SkillContext;
import java.util.Comparator;
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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Dedicated talisman_consume family.
 * Packet path already consumes the talisman item; this spell must not re-consume.
 */
public class TalismanConsumeSpell extends SpellEffect {
    private final double range;
    private final double radius;
    private final String element;
    private final String effectKey;
    private final Set<String> tags;
    private final String successKey;

    public TalismanConsumeSpell(int cost, int cooldownTicks, double damage, double range, double radius,
                                String element, String effectKey, Set<String> tags, String successKey) {
        super(cost, cooldownTicks, Math.max(8.0D, damage));
        this.range = Math.max(8.0D, range);
        this.radius = Math.max(2.5D, radius);
        this.element = element == null ? "neutral" : element;
        this.effectKey = effectKey == null ? "" : effectKey.toLowerCase(Locale.ROOT);
        this.tags = tags == null ? Set.of() : tags;
        this.successKey = successKey == null || successKey.isBlank()
                ? "message.seeking_immortals.spell.generic_talisman_consume.success"
                : successKey;
    }

    @Override
    public boolean execute(ServerPlayer player, PlayerCultivation cultivation, CultivationSkill skill, SkillContext context) {
        Mode mode = resolveMode();
        return switch (mode) {
            case PROJECTILE -> castProjectile(player, skill);
            case AOE -> castAoe(player, skill);
            case BUFF -> castBuff(player, cultivation, skill);
            case CONTROL -> castControl(player, skill);
            case MOVEMENT -> castMovement(player, skill);
        };
    }

    private Mode resolveMode() {
        return classifyMode(effectKey + " " + String.join(" ", tags) + " " + element);
    }

    /** Visible for corpus-audit tests: keyword blob → cast mode. */
    static Mode classifyMode(String rawBlob) {
        String blob = rawBlob == null ? "" : rawBlob.toLowerCase(Locale.ROOT);
        if (blob.contains("aoe") || blob.contains("burst") || blob.contains("storm") || blob.contains("explosion")
                || blob.contains("thunder_strike")) {
            return Mode.AOE;
        }
        if (blob.contains("slow") || blob.contains("seal") || blob.contains("bind") || blob.contains("lock")
                || blob.contains("anchor") || blob.contains("repulse") || blob.contains("prevent")) {
            return Mode.CONTROL;
        }
        if (blob.contains("escape") || blob.contains("teleport") || blob.contains("invis") || blob.contains("hide")
                || blob.contains("mask") || blob.contains("speed")) {
            return Mode.MOVEMENT;
        }
        if (blob.contains("buff") || blob.contains("protect") || blob.contains("armor") || blob.contains("resist")
                || blob.contains("shield") || blob.contains("gather") || blob.contains("resurrect")
                || blob.contains("contract") || blob.contains("fix") || blob.contains("illusion")
                || blob.contains("wall") || blob.contains("spirit") || blob.contains("boost")) {
            return Mode.BUFF;
        }
        if (blob.contains("projectile") || blob.contains("fire") || blob.contains("bolt")) {
            return Mode.PROJECTILE;
        }
        return Mode.AOE;
    }

    private boolean castProjectile(ServerPlayer player, CultivationSkill skill) {
        double damage = calculateDamage(skill.getLevel(), skill.getProficiency());
        CultivationFireballEntity.SpellElement spellElement = mapProjectileElement();
        CultivationFireballEntity projectile = new CultivationFireballEntity(
                player.level(), player, player.getLookAngle(), damage, 1.2D, spellElement);
        player.level().addFreshEntity(projectile);
        player.displayClientMessage(Component.translatable(successKey, modeDisplay("projectile"),
                String.format(Locale.ROOT, "%.1f", damage)), true);
        return true;
    }

    private boolean castAoe(ServerPlayer player, CultivationSkill skill) {
        ServerLevel level = player.serverLevel();
        Vec3 center = findImpact(level, player);
        AABB area = new AABB(center, center).inflate(radius, 2.2D, radius);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, area,
                        entity -> canAffect(player, entity))
                .stream()
                .sorted(Comparator.comparingDouble(entity -> entity.distanceToSqr(center)))
                .limit(12)
                .toList();
        double damage = calculateDamage(skill.getLevel(), skill.getProficiency());
        int hit = 0;
        for (LivingEntity target : targets) {
            target.hurt(player.damageSources().indirectMagic(player, player), (float) damage);
            if (effectKey.contains("fire") || element.toLowerCase(Locale.ROOT).contains("fire")) {
                target.setSecondsOnFire(4);
            }
            if (effectKey.contains("ice") || effectKey.contains("slow")) {
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 90, 2, false, true));
            }
            if (effectKey.contains("thunder")) {
                target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 50, 0, false, true));
            }
            hit++;
        }
        level.sendParticles(ParticleTypes.FLAME, center.x, center.y + 0.2D, center.z,
                40, radius * 0.4D, 0.3D, radius * 0.4D, 0.02D);
        level.playSound(null, player.blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.8F, 1.0F);
        player.displayClientMessage(Component.translatable(successKey, modeDisplay("aoe"), hit), true);
        return true;
    }

    private boolean castBuff(ServerPlayer player, PlayerCultivation cultivation, CultivationSkill skill) {
        ServerLevel level = player.serverLevel();
        int ticks = 120 + skill.getLevel() * 8;
        if (effectKey.contains("armor") || effectKey.contains("protect") || effectKey.contains("resist")
                || effectKey.contains("shield") || effectKey.contains("wall")) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, ticks, 1, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, ticks, 1, false, true));
        } else if (effectKey.contains("gather") || effectKey.contains("spirit")) {
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, ticks, 0, false, true));
            cultivation.addSpiritualPower(6 + skill.getLevel());
        } else if (effectKey.contains("invis") || effectKey.contains("hide") || effectKey.contains("mask")
                || effectKey.contains("illusion")) {
            player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, Math.max(60, ticks / 2), 0, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, ticks, 0, false, true));
        } else if (effectKey.contains("resurrect") || effectKey.contains("life")) {
            player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, ticks * 2, 2, false, true));
            player.heal(6.0F + skill.getLevel());
        } else {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, ticks, 0, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, ticks, 0, false, true));
        }
        level.sendParticles(ParticleTypes.ENCHANT, player.getX(), player.getY() + 1.0D, player.getZ(),
                28, 0.4D, 0.5D, 0.4D, 0.0D);
        level.playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.7F, 1.2F);
        player.displayClientMessage(Component.translatable(successKey, modeDisplay("buff"), 1), true);
        return true;
    }

    private boolean castControl(ServerPlayer player, CultivationSkill skill) {
        ServerLevel level = player.serverLevel();
        Vec3 center = findImpact(level, player);
        AABB area = new AABB(center, center).inflate(Math.max(2.0D, radius * 0.8D), 2.0D, Math.max(2.0D, radius * 0.8D));
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, area, entity -> canAffect(player, entity));
        double damage = calculateDamage(skill.getLevel(), skill.getProficiency()) * 0.65D;
        int hit = 0;
        for (LivingEntity target : targets) {
            if (damage > 0.0D) {
                target.hurt(player.damageSources().indirectMagic(player, player), (float) damage);
            }
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100 + skill.getLevel() * 4, 4, false, true));
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80 + skill.getLevel() * 3, 1, false, true));
            hit++;
        }
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 0, false, true));
        level.sendParticles(ParticleTypes.SOUL, center.x, center.y + 0.3D, center.z, 24, 0.4D, 0.3D, 0.4D, 0.01D);
        level.playSound(null, player.blockPosition(), SoundEvents.SCULK_SHRIEKER_SHRIEK, SoundSource.PLAYERS, 0.55F, 1.3F);
        player.displayClientMessage(Component.translatable(successKey, modeDisplay("control"), hit), true);
        return true;
    }

    private boolean castMovement(ServerPlayer player, CultivationSkill skill) {
        ServerLevel level = player.serverLevel();
        int ticks = 100 + skill.getLevel() * 6;
        if (effectKey.contains("teleport") || effectKey.contains("escape") || effectKey.contains("array")) {
            Vec3 look = player.getLookAngle().normalize();
            Vec3 dest = player.position().add(look.scale(Math.min(10.0D, 6.0D + skill.getLevel() * 0.4D)));
            player.teleportTo(dest.x, dest.y, dest.z);
            player.fallDistance = 0.0F;
        }
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, ticks, 1, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.JUMP, ticks, 0, false, true));
        if (effectKey.contains("invis") || effectKey.contains("hide") || effectKey.contains("ghost")
                || effectKey.contains("mask")) {
            player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, Math.max(40, ticks / 2), 0, false, true));
        }
        level.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 0.2D, player.getZ(),
                24, 0.4D, 0.2D, 0.4D, 0.02D);
        level.playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.6F, 1.35F);
        player.displayClientMessage(Component.translatable(successKey, modeDisplay("movement"), 1), true);
        return true;
    }

    private static Component modeDisplay(String mode) {
        String normalized = mode == null ? "" : mode.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "projectile", "aoe", "buff", "control", "movement" ->
                    Component.translatable("tooltip.seeking_immortals.catalog_talisman.mode." + normalized);
            default -> Component.translatable("message.seeking_immortals.spell.talisman.unknown_mode");
        };
    }

    private Vec3 findImpact(ServerLevel level, ServerPlayer player) {
        Vec3 start = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        if (look.lengthSqr() < 0.001D) {
            return player.position();
        }
        Vec3 end = start.add(look.normalize().scale(range));
        BlockHitResult hit = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, player));
        return hit.getType() == HitResult.Type.MISS ? end : hit.getLocation();
    }

    private CultivationFireballEntity.SpellElement mapProjectileElement() {
        String blob = (effectKey + " " + element).toLowerCase(Locale.ROOT);
        if (blob.contains("ice") || blob.contains("water")) {
            return CultivationFireballEntity.SpellElement.ICE;
        }
        if (blob.contains("thunder") || blob.contains("lightning")) {
            return CultivationFireballEntity.SpellElement.THUNDER;
        }
        if (blob.contains("wood") || blob.contains("wind")) {
            return CultivationFireballEntity.SpellElement.WIND;
        }
        if (blob.contains("earth") || blob.contains("metal")) {
            return CultivationFireballEntity.SpellElement.EARTH;
        }
        return CultivationFireballEntity.SpellElement.FIRE;
    }

    enum Mode {
        PROJECTILE, AOE, BUFF, CONTROL, MOVEMENT
    }
}
