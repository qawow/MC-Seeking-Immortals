package com.xunxian.seekingimmortals.skill.effect.spell;

import com.xunxian.seekingimmortals.cultivation.PlayerCultivation;
import com.xunxian.seekingimmortals.skill.CultivationSkill;
import com.xunxian.seekingimmortals.skill.effect.SkillContext;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class FormationSpell extends SpellEffect {
    private final double range;
    private final double radius;
    private final FormationForm form;
    private final String successKey;

    public FormationSpell(int cost, int cooldownTicks, double damage, double range, double radius,
                          FormationForm form, String successKey) {
        super(cost, cooldownTicks, damage);
        this.range = range;
        this.radius = radius;
        this.form = form;
        this.successKey = successKey;
    }

    @Override
    public boolean execute(ServerPlayer player, PlayerCultivation cultivation, CultivationSkill skill, SkillContext context) {
        return switch (form) {
            case SPIRIT_GATHER_ARRAY -> castSpiritGather(player, cultivation, skill);
            case DEFENSE_FORMATION -> castDefense(player, skill);
            case STAR_PALACE_PATROL_BEACON -> castPatrolBeacon(player, skill);
            default -> castArea(player, skill);
        };
    }

    private boolean castSpiritGather(ServerPlayer player, PlayerCultivation cultivation, CultivationSkill skill) {
        ServerLevel level = player.serverLevel();
        int duration = scaledTicks(170, skill, 10);
        int restore = Math.max(6, 12 + skill.getLevel() * 2);
        cultivation.addSpiritualPower(restore);
        cultivation.addQiDeviationRisk(-2);
        add(player, MobEffects.REGENERATION, duration / 2, 0);
        add(player, MobEffects.DAMAGE_RESISTANCE, duration, 0);
        form.spawnSelfField(level, player.position(), radius, player.getBbHeight());
        play(level, form, player.blockPosition());
        player.displayClientMessage(Component.translatable(successKey, restore), true);
        return true;
    }

    private boolean castDefense(ServerPlayer player, CultivationSkill skill) {
        ServerLevel level = player.serverLevel();
        int duration = scaledTicks(170, skill, 12);
        add(player, MobEffects.DAMAGE_RESISTANCE, duration, 0);
        add(player, MobEffects.ABSORPTION, duration, Math.max(0, skill.getLevel() / 5));
        int deflected = deflectProjectiles(player, level, radius);
        form.spawnSelfField(level, player.position(), radius, player.getBbHeight());
        play(level, form, player.blockPosition());
        player.displayClientMessage(Component.translatable(successKey, deflected), true);
        return true;
    }

    private boolean castPatrolBeacon(ServerPlayer player, CultivationSkill skill) {
        ServerLevel level = player.serverLevel();
        int duration = scaledTicks(150, skill, 10);
        add(player, MobEffects.NIGHT_VISION, duration, 0);
        add(player, MobEffects.MOVEMENT_SPEED, duration, 0);
        List<LivingEntity> targets = findAreaTargets(level, player, player.position(), radius + 2.2D);
        int marked = 0;
        for (LivingEntity target : targets.stream().limit(12).toList()) {
            add(target, MobEffects.GLOWING, duration, 0);
            if (target instanceof Enemy) {
                add(target, MobEffects.MOVEMENT_SLOWDOWN, duration / 2, 0);
            }
            marked++;
        }
        form.spawnSelfField(level, player.position(), radius + 1.2D, player.getBbHeight());
        play(level, form, player.blockPosition());
        player.displayClientMessage(Component.translatable(successKey, marked), true);
        return true;
    }

    private boolean castArea(ServerPlayer player, CultivationSkill skill) {
        ServerLevel level = player.serverLevel();
        Vec3 center = traceEnd(level, player, player.getEyePosition(), range);
        List<LivingEntity> targets = findAreaTargets(level, player, center, radius);
        if (targets.isEmpty()) {
            form.spawnArea(level, center, radius * 0.72D, List.of());
            player.displayClientMessage(Component.translatable("message.seeking_immortals.spell.area.fail"), true);
            return false;
        }

        double damage = calculateDamage(skill.getLevel(), skill.getProficiency());
        int hitCount = 0;
        for (LivingEntity target : targets.stream().limit(form.targetLimit()).toList()) {
            double falloff = Math.max(0.42D, 1.0D - target.position().distanceTo(center) / (radius + 1.0D));
            double amount = damage * falloff * form.damageMultiplier(target);
            if (amount > 0.0D) {
                target.hurt(player.damageSources().indirectMagic(player, player), (float)amount);
            }
            form.applyTarget(player, target, center, skill);
            hitCount++;
        }

        form.spawnArea(level, center, radius, targets);
        play(level, form, BlockPos.containing(center));
        player.displayClientMessage(Component.translatable(successKey, hitCount), true);
        return true;
    }

    private Vec3 traceEnd(ServerLevel level, ServerPlayer player, Vec3 start, double maxRange) {
        Vec3 look = player.getLookAngle();
        if (look.lengthSqr() < 0.001D) {
            return start;
        }
        Vec3 end = start.add(look.normalize().scale(maxRange));
        BlockHitResult blockHit = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        return blockHit.getType() == HitResult.Type.MISS ? end : blockHit.getLocation();
    }

    private List<LivingEntity> findAreaTargets(ServerLevel level, ServerPlayer player, Vec3 center, double maxRadius) {
        AABB area = new AABB(center, center).inflate(maxRadius, Math.max(2.6D, maxRadius * 0.62D), maxRadius);
        return level.getEntitiesOfClass(LivingEntity.class, area,
                        entity -> canTarget(entity, player) && entity.position().distanceToSqr(center) <= maxRadius * maxRadius)
                .stream()
                .sorted(Comparator.comparingDouble(entity -> entity.distanceToSqr(center)))
                .toList();
    }

    private static boolean canTarget(Entity entity, ServerPlayer player) {
        return canAffect(player, entity);
    }

    private static int deflectProjectiles(ServerPlayer player, ServerLevel level, double radius) {
        AABB area = player.getBoundingBox().inflate(radius, radius * 0.70D, radius);
        List<Projectile> projectiles = level.getEntitiesOfClass(Projectile.class, area,
                projectile -> projectile.isAlive() && projectile.getOwner() != player);
        int changed = 0;
        for (Projectile projectile : projectiles) {
            Vec3 direction = projectile.position().subtract(player.position());
            if (direction.lengthSqr() < 0.001D) {
                direction = player.getLookAngle();
            }
            double speed = Math.max(0.38D, projectile.getDeltaMovement().length() + 0.10D);
            projectile.setDeltaMovement(direction.normalize().scale(speed).add(0.0D, 0.05D, 0.0D));
            projectile.hasImpulse = true;
            changed++;
        }
        return changed;
    }

    private static int scaledTicks(int baseTicks, CultivationSkill skill, int perLevelTicks) {
        return baseTicks + Math.max(0, skill.getLevel() - 1) * perLevelTicks;
    }

    private static void add(LivingEntity target, MobEffect effect, int durationTicks, int amplifier) {
        if (durationTicks > 0) {
            target.addEffect(new MobEffectInstance(effect, durationTicks, amplifier, false, true));
        }
    }

    private static void play(ServerLevel level, FormationForm form, BlockPos pos) {
        level.playSound(null, pos, form.sound, SoundSource.PLAYERS, 0.78F, form.pitch);
    }

    public enum FormationForm {
        SMALL_SWORD_ARRAY(new DustParticleOptions(new Vector3f(0.72F, 0.92F, 1.00F), 0.50F),
                new DustParticleOptions(new Vector3f(0.92F, 1.00F, 0.92F), 0.30F),
                SoundEvents.TRIDENT_THROW, 1.36F),
        ILLUSION_FORMATION(new DustParticleOptions(new Vector3f(0.74F, 0.50F, 1.00F), 0.56F),
                new DustParticleOptions(new Vector3f(0.92F, 0.84F, 1.00F), 0.30F),
                SoundEvents.AMETHYST_BLOCK_RESONATE, 1.08F),
        SPIRIT_GATHER_ARRAY(new DustParticleOptions(new Vector3f(0.46F, 1.00F, 0.70F), 0.54F),
                new DustParticleOptions(new Vector3f(0.86F, 1.00F, 0.78F), 0.32F),
                SoundEvents.BEACON_POWER_SELECT, 1.22F),
        THUNDER_TRAP_ARRAY(new DustParticleOptions(new Vector3f(0.52F, 0.76F, 1.00F), 0.60F),
                new DustParticleOptions(new Vector3f(0.94F, 0.96F, 1.00F), 0.34F),
                SoundEvents.TRIDENT_THUNDER, 1.52F),
        SEAL_ARRAY(new DustParticleOptions(new Vector3f(1.00F, 0.80F, 0.34F), 0.54F),
                new DustParticleOptions(new Vector3f(0.94F, 0.98F, 0.80F), 0.30F),
                SoundEvents.ENCHANTMENT_TABLE_USE, 1.02F),
        KILL_SWORD_FORMATION(new DustParticleOptions(new Vector3f(1.00F, 0.38F, 0.24F), 0.60F),
                new DustParticleOptions(new Vector3f(0.98F, 0.96F, 0.86F), 0.32F),
                SoundEvents.TRIDENT_HIT, 0.86F),
        DEFENSE_FORMATION(new DustParticleOptions(new Vector3f(0.82F, 0.62F, 0.38F), 0.56F),
                new DustParticleOptions(new Vector3f(1.00F, 0.92F, 0.68F), 0.30F),
                SoundEvents.SHIELD_BLOCK, 1.08F),
        SEA_LOCK_ARRAY(new DustParticleOptions(new Vector3f(0.24F, 0.74F, 1.00F), 0.56F),
                new DustParticleOptions(new Vector3f(0.72F, 0.96F, 1.00F), 0.32F),
                SoundEvents.CONDUIT_ACTIVATE, 0.92F),
        STAR_PALACE_PATROL_BEACON(new DustParticleOptions(new Vector3f(0.42F, 0.66F, 1.00F), 0.52F),
                new DustParticleOptions(new Vector3f(1.00F, 0.94F, 0.56F), 0.32F),
                SoundEvents.BEACON_ACTIVATE, 1.18F),
        FORMATION_TRAP_BASIC(new DustParticleOptions(new Vector3f(0.54F, 0.88F, 0.72F), 0.52F),
                new DustParticleOptions(new Vector3f(0.92F, 1.00F, 0.82F), 0.30F),
                SoundEvents.CHAIN_PLACE, 1.16F),
        STAR_PALACE_SEAL(new DustParticleOptions(new Vector3f(0.24F, 0.58F, 1.00F), 0.58F),
                new DustParticleOptions(new Vector3f(1.00F, 0.94F, 0.54F), 0.34F),
                SoundEvents.CONDUIT_ATTACK_TARGET, 1.04F),
        KUNWU_SEAL_STRIKE(new DustParticleOptions(new Vector3f(1.00F, 0.76F, 0.28F), 0.62F),
                new DustParticleOptions(new Vector3f(0.64F, 0.96F, 1.00F), 0.34F),
                SoundEvents.RESPAWN_ANCHOR_CHARGE, 1.28F),
        STAR_PALACE_TIDAL_LOCK(new DustParticleOptions(new Vector3f(0.18F, 0.68F, 1.00F), 0.58F),
                new DustParticleOptions(new Vector3f(0.86F, 0.98F, 1.00F), 0.32F),
                SoundEvents.CONDUIT_AMBIENT, 1.36F);

        private final DustParticleOptions core;
        private final DustParticleOptions edge;
        private final SoundEvent sound;
        private final float pitch;

        FormationForm(DustParticleOptions core, DustParticleOptions edge, SoundEvent sound, float pitch) {
            this.core = core;
            this.edge = edge;
            this.sound = sound;
            this.pitch = pitch;
        }

        private int targetLimit() {
            return switch (this) {
                case KILL_SWORD_FORMATION, THUNDER_TRAP_ARRAY, STAR_PALACE_SEAL, KUNWU_SEAL_STRIKE -> 9;
                case STAR_PALACE_TIDAL_LOCK, SEA_LOCK_ARRAY -> 10;
                default -> 8;
            };
        }

        private double damageMultiplier(LivingEntity target) {
            if (!(target instanceof Enemy)) {
                return 1.0D;
            }
            return switch (this) {
                case KILL_SWORD_FORMATION, STAR_PALACE_SEAL, KUNWU_SEAL_STRIKE -> 1.18D;
                case THUNDER_TRAP_ARRAY, SEA_LOCK_ARRAY, STAR_PALACE_TIDAL_LOCK -> 1.12D;
                default -> 1.0D;
            };
        }

        private void applyTarget(ServerPlayer player, LivingEntity target, Vec3 center, CultivationSkill skill) {
            int bonus = Math.max(0, skill.getLevel() - 1);
            switch (this) {
                case SMALL_SWORD_ARRAY -> {
                    add(target, MobEffects.MOVEMENT_SLOWDOWN, 80 + bonus * 5, 1);
                    add(target, MobEffects.WEAKNESS, 70 + bonus * 5, 0);
                    pushAway(target, center, 0.10D, 0.04D);
                }
                case ILLUSION_FORMATION -> {
                    add(target, MobEffects.CONFUSION, 95 + bonus * 6, 0);
                    add(target, MobEffects.MOVEMENT_SLOWDOWN, 80 + bonus * 5, 1);
                    add(target, MobEffects.GLOWING, 70 + bonus * 4, 0);
                    target.removeEffect(MobEffects.INVISIBILITY);
                }
                case THUNDER_TRAP_ARRAY -> {
                    add(target, MobEffects.MOVEMENT_SLOWDOWN, 90 + bonus * 5, 2);
                    add(target, MobEffects.DIG_SLOWDOWN, 90 + bonus * 5, 1);
                    add(target, MobEffects.GLOWING, 60 + bonus * 4, 0);
                    jolt(target, center, 0.08D);
                }
                case SEAL_ARRAY, FORMATION_TRAP_BASIC -> {
                    add(target, MobEffects.MOVEMENT_SLOWDOWN, 120 + bonus * 7, 3);
                    add(target, MobEffects.DIG_SLOWDOWN, 105 + bonus * 6, 1);
                    add(target, MobEffects.WEAKNESS, 95 + bonus * 6, 0);
                    target.setDeltaMovement(target.getDeltaMovement().multiply(0.35D, 0.30D, 0.35D));
                    target.hasImpulse = true;
                }
                case KILL_SWORD_FORMATION -> {
                    add(target, MobEffects.MOVEMENT_SLOWDOWN, 85 + bonus * 5, 1);
                    add(target, MobEffects.WEAKNESS, 100 + bonus * 6, 1);
                    pushAway(target, player.position(), 0.20D, 0.06D);
                }
                case SEA_LOCK_ARRAY, STAR_PALACE_TIDAL_LOCK -> {
                    add(target, MobEffects.MOVEMENT_SLOWDOWN, 125 + bonus * 7, 2);
                    add(target, MobEffects.WEAKNESS, 90 + bonus * 6, 0);
                    add(target, MobEffects.DIG_SLOWDOWN, 80 + bonus * 5, 0);
                    pull(target, center, 0.10D, 0.03D);
                }
                case STAR_PALACE_SEAL -> {
                    add(target, MobEffects.MOVEMENT_SLOWDOWN, 120 + bonus * 7, 3);
                    add(target, MobEffects.WEAKNESS, 105 + bonus * 6, 1);
                    add(target, MobEffects.GLOWING, 85 + bonus * 5, 0);
                    pull(target, center, 0.12D, 0.04D);
                }
                case KUNWU_SEAL_STRIKE -> {
                    add(target, MobEffects.MOVEMENT_SLOWDOWN, 110 + bonus * 7, 2);
                    add(target, MobEffects.DIG_SLOWDOWN, 100 + bonus * 6, 1);
                    add(target, MobEffects.WEAKNESS, 100 + bonus * 6, 1);
                    target.setDeltaMovement(target.getDeltaMovement().multiply(0.42D, 0.24D, 0.42D));
                    target.hasImpulse = true;
                }
                case SPIRIT_GATHER_ARRAY, DEFENSE_FORMATION, STAR_PALACE_PATROL_BEACON -> {
                }
            }
        }

        private void spawnSelfField(ServerLevel level, Vec3 base, double radius, double height) {
            for (int layer = 0; layer < 4; layer++) {
                polygon(level, base.add(0.0D, 0.14D + layer * 0.16D, 0.0D), radius * (0.34D + layer * 0.16D), 8 + layer, layer * 0.17D);
            }
            for (int spoke = 0; spoke < 12; spoke++) {
                double angle = Math.PI * 2.0D * spoke / 12.0D;
                Vec3 end = base.add(Math.cos(angle) * radius * 0.78D, 0.34D, Math.sin(angle) * radius * 0.78D);
                line(level, base.add(0.0D, 0.34D, 0.0D), end, 9, spoke % 3 == 0 ? edge : core);
            }
            level.sendParticles(edge, base.x, base.y + height * 0.58D, base.z, 32, radius * 0.22D, 0.30D, radius * 0.22D, 0.012D);
        }

        private void spawnArea(ServerLevel level, Vec3 center, double radius, List<LivingEntity> targets) {
            switch (this) {
                case SMALL_SWORD_ARRAY -> swordArray(level, center, radius, false, targets);
                case KILL_SWORD_FORMATION -> swordArray(level, center, radius, true, targets);
                case THUNDER_TRAP_ARRAY -> thunderArray(level, center, radius, targets);
                case ILLUSION_FORMATION -> illusionArray(level, center, radius, targets);
                case SEA_LOCK_ARRAY, STAR_PALACE_TIDAL_LOCK -> tidalArray(level, center, radius, targets);
                case KUNWU_SEAL_STRIKE, STAR_PALACE_SEAL, SEAL_ARRAY, FORMATION_TRAP_BASIC -> sealArray(level, center, radius, targets);
                case SPIRIT_GATHER_ARRAY, DEFENSE_FORMATION, STAR_PALACE_PATROL_BEACON -> spawnSelfField(level, center, radius, 1.8D);
            }
        }

        private void swordArray(ServerLevel level, Vec3 center, double radius, boolean lethal, List<LivingEntity> targets) {
            for (int ring = 0; ring < 3; ring++) {
                polygon(level, center.add(0.0D, 0.16D + ring * 0.10D, 0.0D), radius * (0.42D + ring * 0.24D), lethal ? 9 : 7, ring * 0.18D);
            }
            int blades = lethal ? 18 : 12;
            for (int i = 0; i < blades; i++) {
                double angle = Math.PI * 2.0D * i / blades;
                Vec3 foot = center.add(Math.cos(angle) * radius * 0.78D, 0.18D, Math.sin(angle) * radius * 0.78D);
                Vec3 top = foot.add(0.0D, lethal ? 2.8D : 2.1D, 0.0D);
                line(level, top, foot, 12, i % 2 == 0 ? core : edge);
                slashMark(level, foot.add(0.0D, 0.15D, 0.0D), angle, lethal ? 0.58D : 0.42D);
            }
            linkTargets(level, center, targets, 6);
        }

        private void thunderArray(ServerLevel level, Vec3 center, double radius, List<LivingEntity> targets) {
            for (int ring = 0; ring < 4; ring++) {
                polygon(level, center.add(0.0D, 0.14D + ring * 0.12D, 0.0D), radius * (0.30D + ring * 0.19D), 8, ring * 0.28D);
            }
            for (int bolt = 0; bolt < 10; bolt++) {
                double angle = Math.PI * 2.0D * bolt / 10.0D;
                Vec3 base = center.add(Math.cos(angle) * radius * 0.66D, 0.18D, Math.sin(angle) * radius * 0.66D);
                jaggedLine(level, base.add(0.0D, 2.6D, 0.0D), base, bolt * 11);
            }
            linkTargets(level, center.add(0.0D, 1.8D, 0.0D), targets, 7);
        }

        private void illusionArray(ServerLevel level, Vec3 center, double radius, List<LivingEntity> targets) {
            for (int ring = 0; ring < 5; ring++) {
                polygon(level, center.add(0.0D, 0.20D + ring * 0.16D, 0.0D), radius * (0.22D + ring * 0.17D), 9, ring * 0.41D);
            }
            for (int veil = 0; veil < 14; veil++) {
                double angle = Math.PI * 2.0D * veil / 14.0D;
                Vec3 a = center.add(Math.cos(angle) * radius * 0.30D, 0.52D, Math.sin(angle) * radius * 0.30D);
                Vec3 b = center.add(Math.cos(angle + 0.9D) * radius * 0.92D, 0.90D, Math.sin(angle + 0.9D) * radius * 0.92D);
                line(level, a, b, 9, veil % 2 == 0 ? core : edge);
            }
            linkTargets(level, center.add(0.0D, 1.2D, 0.0D), targets, 6);
        }

        private void sealArray(ServerLevel level, Vec3 center, double radius, List<LivingEntity> targets) {
            for (int ring = 0; ring < 4; ring++) {
                polygon(level, center.add(0.0D, 0.16D + ring * 0.12D, 0.0D), radius * (0.32D + ring * 0.20D), 8, Math.PI / 8.0D + ring * 0.18D);
            }
            for (int trigram = 0; trigram < 8; trigram++) {
                double angle = Math.PI * 2.0D * trigram / 8.0D;
                sealGlyph(level, center.add(Math.cos(angle) * radius * 0.70D, 0.52D, Math.sin(angle) * radius * 0.70D), angle, 0.42D);
            }
            linkTargets(level, center.add(0.0D, 1.0D, 0.0D), targets, 8);
        }

        private void tidalArray(ServerLevel level, Vec3 center, double radius, List<LivingEntity> targets) {
            for (int wave = 0; wave < 5; wave++) {
                polygon(level, center.add(0.0D, 0.12D + wave * 0.10D, 0.0D), radius * (0.24D + wave * 0.18D), 14, wave * 0.29D);
            }
            for (int crest = 0; crest < 16; crest++) {
                double angle = Math.PI * 2.0D * crest / 16.0D;
                Vec3 a = center.add(Math.cos(angle) * radius * 0.24D, 0.30D, Math.sin(angle) * radius * 0.24D);
                Vec3 b = center.add(Math.cos(angle) * radius, 0.52D + Math.sin(angle * 2.0D) * 0.12D, Math.sin(angle) * radius);
                line(level, a, b, 8, crest % 2 == 0 ? core : edge);
            }
            linkTargets(level, center.add(0.0D, 0.9D, 0.0D), targets, 8);
        }

        private void linkTargets(ServerLevel level, Vec3 center, List<LivingEntity> targets, int limit) {
            int links = Math.min(limit, targets.size());
            for (int i = 0; i < links; i++) {
                LivingEntity target = targets.get(i);
                Vec3 targetCenter = target.position().add(0.0D, target.getBbHeight() * 0.55D, 0.0D);
                arc(level, center, targetCenter, i * 17);
            }
        }

        private void polygon(ServerLevel level, Vec3 center, double radius, int sides, double rotate) {
            Vec3 prev = null;
            Vec3 first = null;
            for (int i = 0; i <= sides; i++) {
                double angle = rotate + Math.PI * 2.0D * i / sides;
                Vec3 point = center.add(Math.cos(angle) * radius, Math.sin(angle * 3.0D) * 0.035D, Math.sin(angle) * radius);
                if (first == null) {
                    first = point;
                }
                if (prev != null) {
                    line(level, prev, point, 5, i % 2 == 0 ? core : edge);
                }
                prev = point;
            }
            if (prev != null && first != null) {
                line(level, prev, first, 5, edge);
            }
        }

        private void sealGlyph(ServerLevel level, Vec3 center, double angle, double size) {
            Vec3 side = new Vec3(Math.cos(angle), 0.0D, Math.sin(angle));
            Vec3 cross = new Vec3(-side.z, 0.0D, side.x);
            line(level, center.add(side.scale(-size)), center.add(side.scale(size)), 7, edge);
            line(level, center.add(cross.scale(-size * 0.70D)).add(0.0D, 0.12D, 0.0D),
                    center.add(cross.scale(size * 0.70D)).add(0.0D, 0.12D, 0.0D), 7, core);
            line(level, center.add(side.scale(-size * 0.55D)).add(0.0D, -size * 0.28D, 0.0D),
                    center.add(side.scale(size * 0.55D)).add(0.0D, size * 0.34D, 0.0D), 6, core);
        }

        private void slashMark(ServerLevel level, Vec3 center, double angle, double size) {
            Vec3 side = new Vec3(Math.cos(angle), 0.0D, Math.sin(angle));
            Vec3 cross = new Vec3(-side.z, 0.0D, side.x);
            line(level, center.add(cross.scale(-size)).add(0.0D, -0.04D, 0.0D),
                    center.add(cross.scale(size)).add(0.0D, 0.24D, 0.0D), 8, core);
        }

        private void jaggedLine(ServerLevel level, Vec3 start, Vec3 end, int seed) {
            Vec3 path = end.subtract(start);
            int steps = Math.max(8, (int)(path.length() * 6.0D));
            for (int i = 0; i <= steps; i++) {
                double t = i / (double)steps;
                Vec3 point = start.lerp(end, t).add(Math.sin(seed + i * 1.91D) * 0.08D,
                        Math.cos(seed * 0.23D + i * 0.77D) * 0.04D,
                        Math.cos(seed + i * 1.47D) * 0.08D);
                level.sendParticles(i % 2 == 0 ? core : edge, point.x, point.y, point.z, 1, 0.012D, 0.012D, 0.012D, 0.0D);
            }
        }

        private void arc(ServerLevel level, Vec3 start, Vec3 end, int seed) {
            Vec3 path = end.subtract(start);
            int steps = Math.max(6, (int)(path.length() * 4.6D));
            for (int i = 0; i <= steps; i++) {
                double t = i / (double)steps;
                Vec3 point = start.lerp(end, t).add(Math.sin(seed + i * 1.13D) * 0.05D,
                        Math.cos(seed * 0.31D + i * 0.83D) * 0.04D,
                        Math.cos(seed + i * 1.09D) * 0.05D);
                level.sendParticles(i % 2 == 0 ? core : edge, point.x, point.y, point.z, 1, 0.014D, 0.014D, 0.014D, 0.0D);
            }
        }

        private void line(ServerLevel level, Vec3 start, Vec3 end, int steps, DustParticleOptions particle) {
            for (int i = 0; i <= steps; i++) {
                double t = i / (double)Math.max(1, steps);
                Vec3 point = start.lerp(end, t);
                level.sendParticles(particle, point.x, point.y, point.z, 1, 0.012D, 0.012D, 0.012D, 0.0D);
            }
        }

        private static void pushAway(LivingEntity target, Vec3 center, double horizontal, double vertical) {
            Vec3 away = target.position().subtract(center);
            if (away.lengthSqr() < 0.001D) {
                return;
            }
            Vec3 n = away.normalize();
            target.push(n.x * horizontal, vertical, n.z * horizontal);
            target.hasImpulse = true;
        }

        private static void pull(LivingEntity target, Vec3 center, double horizontal, double vertical) {
            Vec3 pull = center.subtract(target.position());
            if (pull.lengthSqr() < 0.001D) {
                return;
            }
            Vec3 n = pull.normalize();
            target.push(n.x * horizontal, vertical, n.z * horizontal);
            target.hasImpulse = true;
        }

        private static void jolt(LivingEntity target, Vec3 center, double power) {
            Vec3 away = target.position().subtract(center);
            if (away.lengthSqr() < 0.001D) {
                away = new Vec3(0.0D, 0.0D, 1.0D);
            }
            Vec3 n = away.normalize();
            target.push(n.x * power, 0.12D, n.z * power);
            target.hasImpulse = true;
        }
    }
}
