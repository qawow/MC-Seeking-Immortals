package com.xunxian.seekingimmortals.skill.effect.spell;

import com.xunxian.seekingimmortals.catalog.SummonHonestMvpService;
import com.xunxian.seekingimmortals.block.EarthWallBlock;
import com.xunxian.seekingimmortals.cultivation.PlayerCultivation;
import com.xunxian.seekingimmortals.entity.CultivationBeastEntity;
import com.xunxian.seekingimmortals.entity.CultivationFireballEntity;
import com.xunxian.seekingimmortals.entity.SummonedServitorEntity;
import com.xunxian.seekingimmortals.entity.SwordProjectileEntity;
import com.xunxian.seekingimmortals.registry.ModMobEffects;
import com.xunxian.seekingimmortals.registry.ModBlocks;
import com.xunxian.seekingimmortals.skill.CultivationSkill;
import com.xunxian.seekingimmortals.skill.effect.AuthoredSpellEffectCatalog;
import com.xunxian.seekingimmortals.skill.effect.SkillContext;
import com.xunxian.seekingimmortals.skill.effect.TechniqueVfxPalette;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Server-authoritative functional executor backed by one authored spell profile. */
public final class AuthoredSpellEffect extends SpellEffect {
    private static final Set<String> AREA_TYPES = Set.of(
            "aoe", "aoe_control", "aoe_dot", "field", "domain", "wall", "trap",
            "buff_zone", "summon_field");
    private static final Set<String> PERSISTENT_FIELD_TYPES = Set.of(
            "field", "domain", "wall", "trap", "buff_zone", "summon_field");

    private final AuthoredSpellEffectCatalog.Profile profile;

    public AuthoredSpellEffect(AuthoredSpellEffectCatalog.Profile profile) {
        super(profile.functional().cost(), profile.functional().cooldownTicks(), profile.functional().damageBase());
        this.profile = profile;
    }

    public AuthoredSpellEffectCatalog.Profile profile() {
        return profile;
    }

    @Override
    public boolean execute(ServerPlayer player, PlayerCultivation cultivation,
                           CultivationSkill skill, SkillContext context) {
        if (player == null || cultivation == null || skill == null || !player.isAlive()) {
            return false;
        }
        String type = profile.functional().type();
        boolean success = switch (profile.mechanics().operation()) {
            case RESTORE, RESTORE_SPIRIT, CLEANSE -> executeRecovery(player, cultivation, skill, type);
            case MOVE -> executeMovement(player, skill);
            case SUMMON -> executeSummon(player, skill, "summon_field".equals(type));
            case COMMAND -> executeCommand(player, skill);
            case DEFEND -> executeDefend(player, skill, type);
            case DETECT -> executeDetection(player, skill);
            case CONCEAL -> executeConcealment(player, skill);
            case TRANSFORM -> executeTransformation(player, skill);
            case TERRAIN -> executeTerrain(player, skill);
            case DRAIN -> executeDrain(player, cultivation, skill, context, type);
            case CRAFT -> executeCraftingFocus(player, skill);
            case CULTIVATE -> executeCultivationSupport(player, cultivation, skill);
            case COMMUNICATE -> executeCommunication(player, skill);
            case ATTACK, SEAL -> profile.mechanics().delivery() == AuthoredSpellEffectCatalog.Delivery.PROJECTILE
                    ? executeProjectile(player, skill)
                    : executeOffensive(player, skill, context, type) > 0;
        };
        if (success && profile.mechanics().operation() != AuthoredSpellEffectCatalog.Operation.COMMUNICATE) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.spell.authored.success", displayName()), true);
        }
        return success;
    }

    private boolean executeRecovery(ServerPlayer player, PlayerCultivation cultivation,
                                    CultivationSkill skill, String type) {
        int level = Math.max(1, skill.getLevel());
        if ("heal_spirit".equals(type)) {
            int amount = Math.max(4, profile.functional().cost() / 2 + level * 2);
            cultivation.addSpiritualPower(amount);
        } else if ("cleanse".equals(type)) {
            List<MobEffect> harmful = player.getActiveEffects().stream()
                    .map(MobEffectInstance::getEffect)
                    .filter(effect -> effect.getCategory() == MobEffectCategory.HARMFUL)
                    .toList();
            harmful.forEach(player::removeEffect);
        } else {
            float amount = (float) Math.max(2.0D,
                    profile.functional().damageBase() + 2.0D + level * 0.75D);
            player.heal(amount);
        }
        applyStatus(player, profile.functional().primaryStatus(), duration(skill, 120), amplifier(skill, false));
        applyStatus(player, profile.functional().secondaryStatus(), duration(skill, 80), amplifier(skill, true));
        return true;
    }

    private boolean executeMovement(ServerPlayer player, CultivationSkill skill) {
        ServerLevel level = player.serverLevel();
        Vec3 look = normalized(player.getLookAngle());
        double distance = Math.max(2.0D, Math.min(16.0D, profile.functional().range()));
        Vec3 start = player.position();
        Vec3 eye = player.getEyePosition();
        Vec3 traceEnd = eye.add(look.scale(distance));
        BlockHitResult hit = level.clip(new ClipContext(
                eye, traceEnd, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 destination = hit.getType() == HitResult.Type.MISS
                ? start.add(look.scale(distance))
                : hit.getLocation().subtract(look.scale(0.85D)).subtract(0.0D, player.getEyeHeight(), 0.0D);
        Vec3 delta = destination.subtract(start);
        AABB moved = player.getBoundingBox().move(delta);
        if (!level.noCollision(player, moved)) {
            double shorter = Math.max(1.0D, delta.length() - 1.5D);
            destination = start.add(normalized(delta).scale(shorter));
            moved = player.getBoundingBox().move(destination.subtract(start));
        }
        if (!level.noCollision(player, moved)) {
            return false;
        }
        player.teleportTo(destination.x, destination.y, destination.z);
        player.fallDistance = 0.0F;
        applyStatus(player, "slowness".equals(profile.functional().primaryStatus())
                ? "resistance" : profile.functional().primaryStatus(), duration(skill, 50), 0);
        return true;
    }

    private boolean executeSummon(ServerPlayer player, CultivationSkill skill, boolean withField) {
        int level = Math.max(1, skill.getLevel());
        double damage = Math.max(4.0D, scaledDamage(skill));
        double health = Math.max(24.0D, 30.0D + damage * 2.0D + level * 4.0D);
        SummonedServitorEntity.Archetype archetype = switch (profile.mechanics().summonArchetype()) {
            case BEAST -> SummonedServitorEntity.Archetype.BEAST;
            case PUPPET -> SummonedServitorEntity.Archetype.PUPPET;
            case GHOST -> SummonedServitorEntity.Archetype.GHOST;
            case GENERIC -> SummonedServitorEntity.Archetype.GENERIC;
        };
        boolean spawned = SummonHonestMvpService.spawnConfigured(player, profile.id(),
                Math.max(160, profile.mechanics().durationTicks()), health, damage, archetype);
        if (spawned && withField) {
            spawnPersistentField(player, player.position(), skill, true);
        }
        return spawned;
    }

    private boolean executeCommand(ServerPlayer player, CultivationSkill skill) {
        List<LivingEntity> servants = new ArrayList<>();
        servants.addAll(SummonHonestMvpService.listOwnedServitors(player));
        servants.addAll(SummonHonestMvpService.listOwnedCompanionBeasts(player));
        if (servants.isEmpty()) {
            return false;
        }
        for (LivingEntity servant : servants) {
            servant.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, duration(skill, 120),
                    amplifier(skill, false), false, true));
            servant.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, duration(skill, 120),
                    0, false, true));
        }
        return true;
    }

    private boolean executeProjectile(ServerPlayer player, CultivationSkill skill) {
        ServerLevel level = player.serverLevel();
        int count = profile.mechanics().projectileCount();
        double damage = scaledDamage(skill) / Math.sqrt(Math.max(1, count));
        Vec3 look = normalized(player.getLookAngle());
        Vec3 side = normalized(look.cross(new Vec3(0.0D, 1.0D, 0.0D)));
        if (Math.abs(side.dot(look)) > 0.1D) {
            side = new Vec3(1.0D, 0.0D, 0.0D);
        }
        boolean falling = Set.of("sword_rain", "falling_barrage").contains(profile.shape());
        Vec3 impact = falling ? impactPoint(level, player) : Vec3.ZERO;
        int spawned = 0;
        for (int index = 0; index < count; index++) {
            double offset = count == 1 ? 0.0D : (index - (count - 1) * 0.5D) * 0.085D;
            Vec3 direction = falling ? new Vec3(0.0D, -1.0D, 0.0D)
                    : normalized(look.add(side.scale(offset))
                    .add(0.0D, ((index % 3) - 1) * 0.025D, 0.0D));
            Entity projectile;
            if (profile.motif() == com.xunxian.seekingimmortals.network.TechniqueVfxPacket.Motif.BLADE
                    || profile.shape().contains("sword") || profile.shape().contains("blade")) {
                projectile = new SwordProjectileEntity(level, player, direction, damage,
                        profile.family() == TechniqueVfxPalette.Family.ICE,
                        profile.id(), profile.family(), profile.trail());
            } else {
                projectile = new CultivationFireballEntity(level, player, direction, damage,
                        1.05D + profile.scaleTier() * 0.07D, projectileElement(),
                        profile.id(), profile.family(), profile.trail());
            }
            if (falling) {
                double spread = Math.max(0.5D, profile.functional().radius());
                Vec3 start = impact.add((player.getRandom().nextDouble() - 0.5D) * spread * 2.0D,
                        4.5D + player.getRandom().nextDouble() * 2.5D,
                        (player.getRandom().nextDouble() - 0.5D) * spread * 2.0D);
                projectile.setPos(start);
                projectile.setDeltaMovement(direction.scale(1.15D));
            }
            if (level.addFreshEntity(projectile)) {
                spawned++;
            }
        }
        return spawned > 0;
    }

    private CultivationFireballEntity.SpellElement projectileElement() {
        return switch (profile.family()) {
            case FIRE -> CultivationFireballEntity.SpellElement.FIRE;
            case WATER -> CultivationFireballEntity.SpellElement.WATER;
            case METAL -> CultivationFireballEntity.SpellElement.METAL;
            case WOOD -> CultivationFireballEntity.SpellElement.WOOD;
            case EARTH -> CultivationFireballEntity.SpellElement.EARTH;
            case WIND, NEUTRAL -> CultivationFireballEntity.SpellElement.WIND;
            case ICE -> CultivationFireballEntity.SpellElement.ICE;
            case THUNDER -> CultivationFireballEntity.SpellElement.THUNDER;
            case LIGHT -> CultivationFireballEntity.SpellElement.LIGHT;
            case DARK, SOUL, BLOOD, VOID, ILLUSION -> CultivationFireballEntity.SpellElement.DARK;
        };
    }

    private boolean executeDefend(ServerPlayer player, CultivationSkill skill, String type) {
        MobEffect primary = beneficialStatus(profile.functional().primaryStatus(), MobEffects.DAMAGE_RESISTANCE);
        MobEffect secondary = beneficialStatus(profile.functional().secondaryStatus(), MobEffects.ABSORPTION);
        int ticks = duration(skill, 140);
        applyEffect(player, primary, ticks, amplifier(skill, false));
        applyEffect(player, secondary, ticks, amplifier(skill, true));
        if (profile.mechanics().delivery() == AuthoredSpellEffectCatalog.Delivery.FIELD
                || "buff_zone".equals(type)) {
            spawnPersistentField(player, player.position(), skill, true);
        }
        return true;
    }

    private boolean executeDetection(ServerPlayer player, CultivationSkill skill) {
        revealNearby(player, profile.functional().range(), duration(skill, 80));
        player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, duration(skill, 160),
                0, false, true));
        return true;
    }

    private boolean executeConcealment(ServerPlayer player, CultivationSkill skill) {
        int ticks = duration(skill, 120);
        player.removeEffect(MobEffects.GLOWING);
        player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, ticks, 0, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, Math.max(60, ticks / 2),
                amplifier(skill, true), false, true));
        return true;
    }

    private boolean executeTransformation(ServerPlayer player, CultivationSkill skill) {
        int ticks = duration(skill, 160);
        applyEffect(player, MobEffects.DAMAGE_BOOST, ticks, amplifier(skill, false));
        applyEffect(player, MobEffects.DAMAGE_RESISTANCE, ticks, amplifier(skill, true));
        applyEffect(player, MobEffects.MOVEMENT_SPEED, Math.max(80, ticks / 2), amplifier(skill, true));
        MobEffect authored = beneficialStatus(profile.functional().primaryStatus(), null);
        applyEffect(player, authored, ticks, amplifier(skill, false));
        return true;
    }

    private boolean executeTerrain(ServerPlayer player, CultivationSkill skill) {
        Vec3 center = impactPoint(player.serverLevel(), player);
        MobEffect primary = null;
        MobEffect secondary = null;
        boolean ignite = false;
        switch (profile.mechanics().terrainMode()) {
            case SAND -> {
                primary = MobEffects.MOVEMENT_SLOWDOWN;
                secondary = MobEffects.BLINDNESS;
            }
            case ICE -> {
                primary = MobEffects.MOVEMENT_SLOWDOWN;
                secondary = customStatus("frozen");
            }
            case FIRE -> {
                primary = MobEffects.WEAKNESS;
                secondary = null;
                ignite = true;
            }
            case VINE -> {
                primary = customStatus("stun");
                secondary = MobEffects.POISON;
            }
            case ROCK -> {
                primary = MobEffects.MOVEMENT_SLOWDOWN;
                secondary = MobEffects.WEAKNESS;
            }
            case WATER -> {
                primary = MobEffects.MOVEMENT_SLOWDOWN;
                secondary = MobEffects.WEAKNESS;
            }
            case WIND -> {
                primary = MobEffects.LEVITATION;
                secondary = MobEffects.WEAKNESS;
            }
            case NONE -> {
                primary = statusEffect(profile.functional().primaryStatus());
                secondary = statusEffect(profile.functional().secondaryStatus());
            }
        }
        AuthoredSpellFieldService.activate(player, profile.id(), center,
                profile.functional().radius(), profile.mechanics().durationTicks(),
                profile.mechanics().maxTargets(), false, 0.0D,
                primary, secondary, ignite);
        return true;
    }

    private boolean executeDrain(ServerPlayer player, PlayerCultivation cultivation,
                                 CultivationSkill skill, SkillContext context, String type) {
        int hits = executeOffensive(player, skill, context, type);
        if (hits <= 0) {
            return false;
        }
        float healing = (float) Math.min(profile.functional().cost(),
                Math.max(1.0D, scaledDamage(skill) * 0.22D * Math.sqrt(hits)));
        player.heal(healing);
        addSpiritualPowerCapped(cultivation, Math.max(1,
                Math.min(profile.functional().cost() / 2, hits + profile.scaleTier())));
        return true;
    }

    private boolean executeCraftingFocus(ServerPlayer player, CultivationSkill skill) {
        int ticks = duration(skill, 160);
        applyEffect(player, MobEffects.DIG_SPEED, ticks, 1 + amplifier(skill, false));
        applyEffect(player, MobEffects.LUCK, ticks, amplifier(skill, true));
        applyEffect(player, MobEffects.NIGHT_VISION, ticks, 0);
        double rangeSqr = square(Math.max(6.0D, Math.min(24.0D, profile.functional().range())));
        List<LivingEntity> owned = new ArrayList<>();
        owned.addAll(SummonHonestMvpService.listOwnedServitors(player));
        owned.addAll(SummonHonestMvpService.listOwnedCompanionBeasts(player));
        owned.stream().filter(entity -> entity.level() == player.level()
                        && entity.distanceToSqr(player) <= rangeSqr)
                .limit(profile.mechanics().maxTargets()).forEach(entity -> {
                    applyEffect(entity, MobEffects.DAMAGE_RESISTANCE, ticks, amplifier(skill, true));
                    applyEffect(entity, MobEffects.REGENERATION, ticks, 0);
                });
        return true;
    }

    private boolean executeCultivationSupport(ServerPlayer player, PlayerCultivation cultivation,
                                               CultivationSkill skill) {
        int ticks = duration(skill, 160);
        applyEffect(player, MobEffects.REGENERATION, ticks, amplifier(skill, true));
        applyEffect(player, MobEffects.DAMAGE_RESISTANCE, Math.max(80, ticks / 2), 0);
        int refundCap = Math.max(1, profile.functional().cost() / 3);
        addSpiritualPowerCapped(cultivation,
                Math.min(refundCap, Math.max(1, profile.scaleTier() + skill.getLevel())));
        return true;
    }

    private boolean executeCommunication(ServerPlayer player, CultivationSkill skill) {
        double range = Math.max(8.0D, Math.min(48.0D,
                profile.functional().range() + Math.max(0, skill.getLevel() - 1) * 2.0D));
        List<ServerPlayer> recipients = player.serverLevel().getPlayers(target ->
                        target != player && target.distanceToSqr(player) <= square(range)).stream()
                .sorted(Comparator.comparingDouble(target -> target.distanceToSqr(player)))
                .limit(profile.mechanics().maxTargets()).toList();
        for (ServerPlayer recipient : recipients) {
            recipient.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.spell.voice_transmission.heard", player.getDisplayName()), false);
            player.serverLevel().playSound(null, recipient.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME,
                    SoundSource.PLAYERS, 0.35F, 1.8F);
        }
        player.displayClientMessage(Component.translatable(recipients.isEmpty()
                        ? "message.seeking_immortals.spell.voice_transmission.no_recipient"
                        : "message.seeking_immortals.spell.voice_transmission.success",
                recipients.isEmpty() ? new Object[0] : new Object[]{recipients.size()}), true);
        return true;
    }

    private int executeOffensive(ServerPlayer player, CultivationSkill skill,
                                 SkillContext context, String type) {
        ServerLevel level = player.serverLevel();
        Vec3 center = impactPoint(level, player);
        List<LivingEntity> targets;
        switch (profile.mechanics().delivery()) {
            case CONE -> targets = coneTargets(level, player);
            case CHAIN -> targets = chainTargets(level, player);
            case AREA, FIELD -> targets = areaTargets(level, player, center, profile.functional().radius());
            default -> {
                LivingEntity target = context != null && context.getTargetEntity() instanceof LivingEntity living
                    && canTarget(player, living) ? living : rayTarget(level, player);
                targets = target == null ? List.of() : List.of(target);
                if (target != null) {
                    center = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
                }
            }
        }

        double damage = scaledDamage(skill);
        int hitCount = 0;
        for (LivingEntity target : targets) {
            double falloff = AREA_TYPES.contains(type)
                    ? Math.max(0.45D, 1.0D - target.position().distanceTo(center)
                            / (profile.functional().radius() + 0.5D))
                    : 1.0D;
            if (damage > 0.0D) {
                target.hurt(player.damageSources().indirectMagic(player, player), (float) (damage * falloff));
            }
            applyStatus(target, profile.functional().primaryStatus(), duration(skill, 100), amplifier(skill, false));
            applyStatus(target, profile.functional().secondaryStatus(), duration(skill, 70), amplifier(skill, true));
            applyElementalImpulse(target, player, type);
            hitCount++;
        }

        if (PERSISTENT_FIELD_TYPES.contains(type)) {
            if ("wall".equals(type)) {
                placeTemporaryWall(player, center);
            }
            spawnPersistentField(player, center, skill, "buff_zone".equals(type));
            return Math.max(1, hitCount);
        }
        return hitCount;
    }

    private void spawnPersistentField(ServerPlayer player, Vec3 center,
                                      CultivationSkill skill, boolean beneficial) {
        MobEffect primary = beneficial
                ? beneficialStatus(profile.functional().primaryStatus(), MobEffects.DAMAGE_RESISTANCE)
                : statusEffect(profile.functional().primaryStatus());
        MobEffect secondary = beneficial
                ? beneficialStatus(profile.functional().secondaryStatus(), MobEffects.REGENERATION)
                : statusEffect(profile.functional().secondaryStatus());
        double pulseDamage = beneficial ? 0.0D
                : scaledDamage(skill) / Math.max(4.0D, profile.mechanics().durationTicks() / 20.0D);
        AuthoredSpellFieldService.activate(player, profile.id(), center,
                profile.functional().radius(), profile.mechanics().durationTicks(),
                profile.mechanics().maxTargets(), beneficial, pulseDamage,
                primary, secondary, "burning".equals(normalizeStatus(profile.functional().primaryStatus())));
    }

    private void revealNearby(ServerPlayer player, double range, int duration) {
        double radius = Math.max(4.0D, Math.min(32.0D, range));
        for (LivingEntity target : player.serverLevel().getEntitiesOfClass(
                LivingEntity.class, player.getBoundingBox().inflate(radius),
                entity -> canTarget(player, entity))) {
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING, duration, 0, false, true));
        }
    }

    private Vec3 impactPoint(ServerLevel level, ServerPlayer player) {
        Vec3 start = player.getEyePosition();
        Vec3 end = start.add(normalized(player.getLookAngle()).scale(profile.functional().range()));
        BlockHitResult hit = level.clip(new ClipContext(
                start, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        return hit.getType() == HitResult.Type.MISS ? end : hit.getLocation();
    }

    private LivingEntity rayTarget(ServerLevel level, ServerPlayer player) {
        Vec3 start = player.getEyePosition();
        Vec3 end = start.add(normalized(player.getLookAngle()).scale(profile.functional().range()));
        BlockHitResult blockHit = level.clip(new ClipContext(
                start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 traceEnd = blockHit.getType() == HitResult.Type.MISS ? end : blockHit.getLocation();
        AABB search = player.getBoundingBox().expandTowards(traceEnd.subtract(start)).inflate(1.2D);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(
                level, player, start, traceEnd, search, entity -> canTarget(player, entity));
        return hit != null && hit.getEntity() instanceof LivingEntity living ? living : null;
    }

    private List<LivingEntity> areaTargets(ServerLevel level, ServerPlayer player,
                                           Vec3 center, double radius) {
        return level.getEntitiesOfClass(LivingEntity.class,
                        new AABB(center, center).inflate(radius, Math.max(2.0D, radius * 0.6D), radius),
                        entity -> canTarget(player, entity)
                                && entity.position().distanceToSqr(center)
                                <= square(radius + entity.getBbWidth()))
                .stream().sorted(Comparator.comparingDouble(entity -> entity.distanceToSqr(center)))
                .limit(profile.mechanics().maxTargets()).toList();
    }

    private List<LivingEntity> coneTargets(ServerLevel level, ServerPlayer player) {
        Vec3 start = player.getEyePosition();
        Vec3 look = normalized(player.getLookAngle());
        double range = profile.functional().range();
        AABB search = player.getBoundingBox().expandTowards(look.scale(range))
                .inflate(profile.functional().radius());
        return level.getEntitiesOfClass(LivingEntity.class, search, entity -> canTarget(player, entity))
                .stream().filter(entity -> {
                    Vec3 offset = entity.getEyePosition().subtract(start);
                    return offset.lengthSqr() <= square(range + entity.getBbWidth())
                            && normalized(offset).dot(look) >= 0.55D;
                }).sorted(Comparator.comparingDouble(entity -> entity.distanceToSqr(player)))
                .limit(profile.mechanics().maxTargets()).toList();
    }

    private List<LivingEntity> chainTargets(ServerLevel level, ServerPlayer player) {
        LivingEntity first = rayTarget(level, player);
        if (first == null) {
            return List.of();
        }
        List<LivingEntity> result = new ArrayList<>();
        result.add(first);
        double radius = Math.max(3.0D, profile.functional().radius() * 1.6D);
        level.getEntitiesOfClass(LivingEntity.class, first.getBoundingBox().inflate(radius),
                        entity -> canTarget(player, entity) && entity != first)
                .stream().sorted(Comparator.comparingDouble(entity -> entity.distanceToSqr(first)))
                .limit(Math.max(0, profile.mechanics().maxTargets() - 1L)).forEach(result::add);
        return List.copyOf(result);
    }

    private void applyElementalImpulse(LivingEntity target, ServerPlayer caster, String type) {
        if (profile.family() == TechniqueVfxPalette.Family.FIRE) {
            target.setSecondsOnFire(Math.max(2, profile.scaleTier() + 2));
        }
        if (profile.family() == TechniqueVfxPalette.Family.WIND || "cone".equals(type)) {
            Vec3 push = normalized(target.position().subtract(caster.position())).scale(0.35D + profile.scaleTier() * 0.08D);
            target.push(push.x, Math.max(0.08D, push.y + 0.08D), push.z);
        }
    }

    private void placeTemporaryWall(ServerPlayer player, Vec3 center) {
        ServerLevel level = player.serverLevel();
        Direction side = player.getDirection().getClockWise();
        BlockPos base = BlockPos.containing(center.x, player.getY(), center.z);
        int halfWidth = Math.min(4, Math.max(1, (int) Math.ceil(profile.functional().radius() * 0.75D)));
        int height = Math.min(4, Math.max(2, 2 + profile.scaleTier() / 2));
        int removalTicks = Math.max(40, Math.min(400, profile.mechanics().durationTicks()));
        BlockState wall = ModBlocks.EARTH_WALL.get().defaultBlockState();
        for (int offset = -halfWidth; offset <= halfWidth; offset++) {
            for (int y = 0; y < height; y++) {
                BlockPos pos = base.relative(side, offset).above(y);
                if (!level.getBlockState(pos).isAir()) {
                    continue;
                }
                level.setBlock(pos, wall, 3);
                level.scheduleTick(pos, ModBlocks.EARTH_WALL.get(), removalTicks);
            }
        }
    }

    private boolean canTarget(ServerPlayer caster, Entity entity) {
        if (!canAffect(caster, entity) || caster.isAlliedTo(entity)) {
            return false;
        }
        if (entity instanceof SummonedServitorEntity servitor) {
            return servitor.getOwnerUUID().filter(caster.getUUID()::equals).isEmpty();
        }
        if (entity instanceof CultivationBeastEntity beast) {
            return !beast.isCompanion()
                    || beast.getOwnerUUID().filter(caster.getUUID()::equals).isEmpty();
        }
        return true;
    }

    private void applyStatus(LivingEntity target, String status, int duration, int amplifier) {
        String normalized = normalizeStatus(status);
        if ("burning".equals(normalized)) {
            target.setSecondsOnFire(Math.max(2, duration / 20));
            return;
        }
        MobEffect effect = statusEffect(normalized);
        if (effect != null) {
            target.addEffect(new MobEffectInstance(effect, Math.max(1, duration), Math.max(0, amplifier),
                    false, true));
        }
    }

    private void applyEffect(LivingEntity target, MobEffect effect, int duration, int amplifier) {
        if (effect != null) {
            target.addEffect(new MobEffectInstance(effect, Math.max(1, duration), Math.max(0, amplifier),
                    false, true));
        }
    }

    private MobEffect beneficialStatus(String status, MobEffect fallback) {
        MobEffect effect = statusEffect(status);
        return effect != null && effect.getCategory() == MobEffectCategory.BENEFICIAL ? effect : fallback;
    }

    private void addSpiritualPowerCapped(PlayerCultivation cultivation, int maximumGain) {
        int before = cultivation.getSpiritualPower();
        int cap = Math.max(0, maximumGain);
        cultivation.addSpiritualPower(cap);
        if (cultivation.getSpiritualPower() > before + cap) {
            cultivation.setSpiritualPower(before + cap);
        }
    }

    private MobEffect statusEffect(String status) {
        return switch (normalizeStatus(status)) {
            case "regeneration" -> MobEffects.REGENERATION;
            case "absorption" -> MobEffects.ABSORPTION;
            case "resistance" -> MobEffects.DAMAGE_RESISTANCE;
            case "damage_boost" -> MobEffects.DAMAGE_BOOST;
            case "weakness", "armor_break" -> MobEffects.WEAKNESS;
            case "slowness", "rooted" -> MobEffects.MOVEMENT_SLOWDOWN;
            case "poison" -> MobEffects.POISON;
            case "levitation" -> MobEffects.LEVITATION;
            case "glowing" -> MobEffects.GLOWING;
            case "blindness" -> MobEffects.BLINDNESS;
            case "wither" -> MobEffects.WITHER;
            case "confusion" -> MobEffects.CONFUSION;
            case "bleeding" -> customStatus("bleed");
            case "frozen" -> customStatus("frozen");
            case "stunned" -> customStatus("stun");
            default -> null;
        };
    }

    private MobEffect customStatus(String id) {
        RegistryObject<MobEffect> object = ModMobEffects.get(id);
        return object != null && object.isPresent() ? object.get() : null;
    }

    private double scaledDamage(CultivationSkill skill) {
        return calculateDamage(skill.getLevel(), skill.getProficiency());
    }

    private int duration(CultivationSkill skill, int base) {
        return Math.max(base, profile.mechanics().durationTicks())
                + Math.max(0, skill.getLevel() - 1) * 8 + profile.scaleTier() * 10;
    }

    private int amplifier(CultivationSkill skill, boolean secondary) {
        int value = profile.scaleTier() / 2 + Math.max(0, skill.getLevel() - 1) / 5;
        return Math.max(0, secondary ? value - 1 : value);
    }

    private Component displayName() {
        return profile.display().isBlank() ? Component.literal(profile.id()) : Component.literal(profile.display());
    }

    private static String normalizeStatus(String status) {
        return status == null ? "" : status.trim().toLowerCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
    }

    private static Vec3 normalized(Vec3 vector) {
        return vector == null || vector.lengthSqr() < 1.0E-6D ? new Vec3(0.0D, 0.0D, 1.0D) : vector.normalize();
    }

    private static double square(double value) {
        return value * value;
    }

}
