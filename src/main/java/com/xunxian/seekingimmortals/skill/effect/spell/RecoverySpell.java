package com.xunxian.seekingimmortals.skill.effect.spell;

import com.xunxian.seekingimmortals.cultivation.PlayerCultivation;
import com.xunxian.seekingimmortals.skill.CultivationSkill;
import com.xunxian.seekingimmortals.skill.effect.SkillContext;
import java.util.List;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class RecoverySpell extends SpellEffect {
    private final double radius;
    private final RecoveryForm form;
    private final String successKey;

    public RecoverySpell(int cost, int cooldownTicks, double baseAmount, double radius,
                         RecoveryForm form, String successKey) {
        super(cost, cooldownTicks, baseAmount);
        this.radius = radius;
        this.form = form;
        this.successKey = successKey;
    }

    @Override
    public boolean execute(ServerPlayer player, PlayerCultivation cultivation, CultivationSkill skill, SkillContext context) {
        return switch (form) {
            case HEAL_QI -> castHealQi(player, cultivation, skill);
            case DETOXIFY -> castDetoxify(player, cultivation, skill);
            case SPIRIT_RECOVERY -> castSpiritRecovery(player, cultivation, skill);
            case BODY_REPAIR -> castBodyRepair(player, skill);
            case GROUP_HEAL -> castGroupHeal(player, skill);
            case REVIVE_WEAK -> castReviveWeak(player, skill);
            case SPIRIT_SHIELD -> castSpiritShield(player, skill);
            case TRIBULATION_THUNDER_WARD -> castTribulationThunderWard(player, cultivation, skill);
        };
    }

    private boolean castHealQi(ServerPlayer player, PlayerCultivation cultivation, CultivationSkill skill) {
        ServerLevel level = player.serverLevel();
        int qiRestore = amount(skill);
        int healed = heal(player, Math.max(2.0D, qiRestore * 0.18D));
        cultivation.addSpiritualPower(qiRestore);
        add(player, MobEffects.REGENERATION, scaledTicks(65, skill, 6), 0);
        form.spawnSpiral(level, player.position(), player.getBbHeight());
        play(level, player, form);
        player.displayClientMessage(Component.translatable(successKey, qiRestore, healed), true);
        return true;
    }

    private boolean castDetoxify(ServerPlayer player, PlayerCultivation cultivation, CultivationSkill skill) {
        ServerLevel level = player.serverLevel();
        int removed = cleanse(player);
        int riskDrop = 2 + Math.max(0, skill.getLevel() / 3);
        cultivation.addQiDeviationRisk(-riskDrop);
        player.setRemainingFireTicks(0);
        add(player, MobEffects.REGENERATION, scaledTicks(45, skill, 5), 0);
        form.spawnCleanse(level, player.position(), player.getBbHeight());
        play(level, player, form);
        player.displayClientMessage(Component.translatable(successKey, removed), true);
        return true;
    }

    private boolean castSpiritRecovery(ServerPlayer player, PlayerCultivation cultivation, CultivationSkill skill) {
        ServerLevel level = player.serverLevel();
        int before = cultivation.getDivineConsciousness();
        int restore = amount(skill);
        cultivation.addDivineConsciousness(restore);
        int restored = Math.max(0, cultivation.getDivineConsciousness() - before);
        cultivation.addQiDeviationRisk(-(1 + Math.max(0, skill.getLevel() / 4)));
        player.removeEffect(MobEffects.CONFUSION);
        player.removeEffect(MobEffects.DARKNESS);
        add(player, MobEffects.NIGHT_VISION, scaledTicks(120, skill, 8), 0);
        form.spawnSpiral(level, player.position(), player.getBbHeight());
        play(level, player, form);
        player.displayClientMessage(Component.translatable(successKey, restored), true);
        return true;
    }

    private boolean castBodyRepair(ServerPlayer player, CultivationSkill skill) {
        ServerLevel level = player.serverLevel();
        int healed = heal(player, amount(skill));
        add(player, MobEffects.REGENERATION, scaledTicks(90, skill, 8), Math.max(0, skill.getLevel() / 6));
        add(player, MobEffects.DAMAGE_RESISTANCE, scaledTicks(70, skill, 6), 0);
        form.spawnSpiral(level, player.position(), player.getBbHeight());
        play(level, player, form);
        player.displayClientMessage(Component.translatable(successKey, healed), true);
        return true;
    }

    private boolean castGroupHeal(ServerPlayer player, CultivationSkill skill) {
        ServerLevel level = player.serverLevel();
        Vec3 center = player.position();
        AABB area = new AABB(center, center).inflate(radius, 2.8D, radius);
        List<ServerPlayer> targets = level.getEntitiesOfClass(ServerPlayer.class, area,
                target -> target.isAlive() && !target.isSpectator()
                        && target.distanceToSqr(center) <= (radius + 0.6D) * (radius + 0.6D));
        if (targets.isEmpty()) {
            targets = List.of(player);
        }

        double healAmount = Math.max(2.0D, amount(skill) * 0.72D);
        for (ServerPlayer target : targets.stream().limit(8).toList()) {
            heal(target, healAmount);
            add(target, MobEffects.REGENERATION, scaledTicks(75, skill, 6), 0);
        }
        form.spawnGroupPulse(level, center, radius, targets);
        play(level, player, form);
        player.displayClientMessage(Component.translatable(successKey, Math.min(8, targets.size())), true);
        return true;
    }

    private boolean castReviveWeak(ServerPlayer player, CultivationSkill skill) {
        ServerLevel level = player.serverLevel();
        double healthRatio = player.getHealth() / Math.max(1.0F, player.getMaxHealth());
        int duration = scaledTicks(130, skill, 10);
        double baseHeal = amount(skill) * (healthRatio <= 0.35D ? 1.65D : 0.85D);
        int healed = heal(player, Math.max(3.0D, baseHeal));
        player.setRemainingFireTicks(0);
        add(player, MobEffects.REGENERATION, duration, healthRatio <= 0.35D ? 1 : 0);
        add(player, MobEffects.ABSORPTION, duration, 1);
        add(player, MobEffects.DAMAGE_RESISTANCE, Math.max(70, duration / 2), 0);
        form.spawnSpiral(level, player.position(), player.getBbHeight());
        play(level, player, form);
        player.displayClientMessage(Component.translatable(successKey, healed, duration / 20), true);
        return true;
    }

    private boolean castSpiritShield(ServerPlayer player, CultivationSkill skill) {
        ServerLevel level = player.serverLevel();
        int duration = scaledTicks(165, skill, 12);
        add(player, MobEffects.ABSORPTION, duration, Math.max(0, skill.getLevel() / 5));
        add(player, MobEffects.DAMAGE_RESISTANCE, duration, 0);
        int deflected = deflectProjectiles(player, level, radius, false);
        form.spawnShield(level, player.position(), player.getBbHeight(), radius);
        play(level, player, form);
        player.displayClientMessage(Component.translatable(successKey, deflected), true);
        return true;
    }

    private boolean castTribulationThunderWard(ServerPlayer player, PlayerCultivation cultivation, CultivationSkill skill) {
        ServerLevel level = player.serverLevel();
        int duration = scaledTicks(260, skill, 16);
        int gained = 0;
        if (cultivation.isTribulationActive()) {
            int before = cultivation.getTribulationResistance();
            cultivation.addTribulationResistance(1 + Math.max(0, skill.getLevel() / 4));
            gained = Math.max(0, cultivation.getTribulationResistance() - before);
        }
        add(player, MobEffects.DAMAGE_RESISTANCE, duration, 1);
        add(player, MobEffects.FIRE_RESISTANCE, duration, 0);
        add(player, MobEffects.SLOW_FALLING, duration, 0);
        add(player, MobEffects.ABSORPTION, duration, 1);
        form.spawnShield(level, player.position(), player.getBbHeight(), radius);
        play(level, player, form);
        player.displayClientMessage(Component.translatable(successKey, duration / 20, gained), true);
        return true;
    }

    private int amount(CultivationSkill skill) {
        return Math.max(0, (int)Math.round(calculateDamage(skill.getLevel(), skill.getProficiency())));
    }

    private static int heal(LivingEntity target, double amount) {
        float before = target.getHealth();
        target.heal((float)Math.max(0.0D, amount));
        return Math.max(0, Math.round(target.getHealth() - before));
    }

    private static int cleanse(LivingEntity target) {
        List<MobEffect> harmful = target.getActiveEffects().stream()
                .map(MobEffectInstance::getEffect)
                .filter(effect -> effect.getCategory() == MobEffectCategory.HARMFUL)
                .toList();
        int removed = 0;
        for (MobEffect effect : harmful) {
            if (target.removeEffect(effect)) {
                removed++;
            }
        }
        return removed;
    }

    private static void add(LivingEntity target, MobEffect effect, int durationTicks, int amplifier) {
        if (durationTicks > 0) {
            target.addEffect(new MobEffectInstance(effect, durationTicks, amplifier, false, true));
        }
    }

    private static int scaledTicks(int baseTicks, CultivationSkill skill, int perLevelTicks) {
        return baseTicks + Math.max(0, skill.getLevel() - 1) * perLevelTicks;
    }

    private static int deflectProjectiles(ServerPlayer player, ServerLevel level, double radius, boolean reflect) {
        AABB area = player.getBoundingBox().inflate(radius, radius * 0.65D, radius);
        List<Projectile> projectiles = level.getEntitiesOfClass(Projectile.class, area,
                projectile -> projectile.isAlive() && projectile.getOwner() != player);
        int changed = 0;
        for (Projectile projectile : projectiles) {
            Vec3 direction = projectile.position().subtract(player.position());
            Entity owner = projectile.getOwner();
            if (reflect && owner instanceof LivingEntity living && living.isAlive()) {
                direction = living.getEyePosition().subtract(projectile.position());
                projectile.setOwner(player);
            }
            if (direction.lengthSqr() < 0.001D) {
                direction = player.getLookAngle();
            }
            double speed = Math.max(0.38D, projectile.getDeltaMovement().length() + 0.08D);
            projectile.setDeltaMovement(direction.normalize().scale(speed).add(0.0D, 0.05D, 0.0D));
            projectile.hasImpulse = true;
            changed++;
        }
        return changed;
    }

    private static void play(ServerLevel level, ServerPlayer player, RecoveryForm form) {
        level.playSound(null, player.blockPosition(), form.sound, SoundSource.PLAYERS, 0.72F, form.pitch);
    }

    public enum RecoveryForm {
        HEAL_QI(new DustParticleOptions(new Vector3f(0.36F, 0.92F, 0.42F), 0.70F),
                new DustParticleOptions(new Vector3f(0.62F, 1.00F, 0.86F), 0.42F),
                SoundEvents.AMETHYST_BLOCK_CHIME, 1.28F),
        DETOXIFY(new DustParticleOptions(new Vector3f(0.20F, 0.78F, 0.34F), 0.70F),
                new DustParticleOptions(new Vector3f(0.94F, 1.00F, 0.58F), 0.38F),
                SoundEvents.BREWING_STAND_BREW, 1.38F),
        SPIRIT_RECOVERY(new DustParticleOptions(new Vector3f(0.58F, 0.72F, 1.00F), 0.66F),
                new DustParticleOptions(new Vector3f(0.92F, 0.90F, 1.00F), 0.40F),
                SoundEvents.ENCHANTMENT_TABLE_USE, 1.34F),
        BODY_REPAIR(new DustParticleOptions(new Vector3f(0.36F, 0.88F, 0.72F), 0.76F),
                new DustParticleOptions(new Vector3f(0.78F, 1.00F, 0.92F), 0.42F),
                SoundEvents.AMETHYST_CLUSTER_HIT, 1.18F),
        GROUP_HEAL(new DustParticleOptions(new Vector3f(0.42F, 0.96F, 0.56F), 0.70F),
                new DustParticleOptions(new Vector3f(0.82F, 1.00F, 0.72F), 0.40F),
                SoundEvents.BEACON_POWER_SELECT, 1.20F),
        REVIVE_WEAK(new DustParticleOptions(new Vector3f(0.96F, 0.80F, 0.36F), 0.74F),
                new DustParticleOptions(new Vector3f(0.36F, 0.96F, 0.72F), 0.46F),
                SoundEvents.TOTEM_USE, 1.08F),
        SPIRIT_SHIELD(new DustParticleOptions(new Vector3f(0.42F, 0.82F, 1.00F), 0.72F),
                new DustParticleOptions(new Vector3f(0.90F, 1.00F, 1.00F), 0.42F),
                SoundEvents.SHIELD_BLOCK, 1.32F),
        TRIBULATION_THUNDER_WARD(new DustParticleOptions(new Vector3f(0.38F, 0.74F, 1.00F), 0.78F),
                new DustParticleOptions(new Vector3f(1.00F, 0.86F, 0.34F), 0.48F),
                SoundEvents.LIGHTNING_BOLT_THUNDER, 1.55F);

        private final DustParticleOptions core;
        private final DustParticleOptions edge;
        private final SoundEvent sound;
        private final float pitch;

        RecoveryForm(DustParticleOptions core, DustParticleOptions edge, SoundEvent sound, float pitch) {
            this.core = core;
            this.edge = edge;
            this.sound = sound;
            this.pitch = pitch;
        }

        private void spawnSpiral(ServerLevel level, Vec3 base, double height) {
            int steps = 64;
            for (int i = 0; i < steps; i++) {
                double t = i / (double)(steps - 1);
                double angle = t * Math.PI * 7.2D;
                double radius = 0.34D + Math.sin(t * Math.PI) * 0.48D;
                level.sendParticles((i & 1) == 0 ? core : edge,
                        base.x + Math.cos(angle) * radius,
                        base.y + 0.16D + t * Math.max(1.2D, height),
                        base.z + Math.sin(angle) * radius,
                        1, 0.018D, 0.018D, 0.018D, 0.0D);
            }
            level.sendParticles(core, base.x, base.y + height * 0.52D, base.z, 18, 0.32D, 0.46D, 0.32D, 0.01D);
        }

        private void spawnCleanse(ServerLevel level, Vec3 base, double height) {
            for (int layer = 0; layer < 4; layer++) {
                ring(level, base.add(0.0D, 0.20D + layer * height * 0.23D, 0.0D), 0.62D + layer * 0.16D, 44, 0.04D);
            }
            level.sendParticles(edge, base.x, base.y + height * 0.60D, base.z, 32, 0.36D, 0.48D, 0.36D, 0.018D);
        }

        private void spawnGroupPulse(ServerLevel level, Vec3 center, double radius, List<ServerPlayer> targets) {
            for (int layer = 0; layer < 3; layer++) {
                ring(level, center.add(0.0D, 0.25D + layer * 0.36D, 0.0D), radius * (0.42D + layer * 0.23D), 76, 0.06D);
            }
            int links = Math.min(8, targets.size());
            for (int i = 0; i < links; i++) {
                arc(level, center.add(0.0D, 0.95D, 0.0D),
                        targets.get(i).position().add(0.0D, targets.get(i).getBbHeight() * 0.58D, 0.0D), i * 19);
            }
        }

        private void spawnShield(ServerLevel level, Vec3 base, double height, double radius) {
            double shieldRadius = Math.max(0.95D, radius * 0.42D);
            for (int layer = 0; layer < 5; layer++) {
                double y = base.y + 0.22D + layer * Math.max(0.26D, height / 4.3D);
                double r = shieldRadius * Math.sin((layer + 1) * Math.PI / 6.0D);
                ring(level, new Vec3(base.x, y, base.z), Math.max(0.18D, r), 58, 0.03D);
            }
            for (int spoke = 0; spoke < 8; spoke++) {
                double angle = Math.PI * 2.0D * spoke / 8.0D;
                for (int i = 0; i < 16; i++) {
                    double t = i / 15.0D;
                    double x = base.x + Math.cos(angle) * shieldRadius * Math.sin(t * Math.PI);
                    double y = base.y + 0.18D + t * Math.max(1.45D, height + 0.34D);
                    double z = base.z + Math.sin(angle) * shieldRadius * Math.sin(t * Math.PI);
                    level.sendParticles((i & 1) == 0 ? core : edge, x, y, z, 1, 0.012D, 0.012D, 0.012D, 0.0D);
                }
            }
        }

        private void ring(ServerLevel level, Vec3 center, double radius, int points, double wave) {
            for (int i = 0; i < points; i++) {
                double angle = Math.PI * 2.0D * i / points;
                level.sendParticles((i & 1) == 0 ? core : edge,
                        center.x + Math.cos(angle) * radius,
                        center.y + Math.sin(angle * 3.0D) * wave,
                        center.z + Math.sin(angle) * radius,
                        1, 0.018D, 0.018D, 0.018D, 0.0D);
            }
        }

        private void arc(ServerLevel level, Vec3 start, Vec3 end, int seed) {
            Vec3 path = end.subtract(start);
            int steps = Math.max(6, (int)(path.length() * 4.6D));
            for (int i = 0; i <= steps; i++) {
                double t = i / (double)steps;
                Vec3 point = start.lerp(end, t).add(Math.sin(seed + i * 1.17D) * 0.06D,
                        Math.cos(seed * 0.31D + i * 0.79D) * 0.05D,
                        Math.cos(seed + i * 1.07D) * 0.06D);
                level.sendParticles((i & 1) == 0 ? core : edge,
                        point.x, point.y, point.z, 1, 0.014D, 0.014D, 0.014D, 0.0D);
            }
        }
    }
}
