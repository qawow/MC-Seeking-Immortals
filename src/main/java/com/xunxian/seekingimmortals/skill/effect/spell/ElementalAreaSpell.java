package com.xunxian.seekingimmortals.skill.effect.spell;

import com.xunxian.seekingimmortals.cultivation.PlayerCultivation;
import com.xunxian.seekingimmortals.network.TechniqueVfxPacket;
import com.xunxian.seekingimmortals.skill.CultivationSkill;
import com.xunxian.seekingimmortals.skill.effect.ActiveTechniqueEffectVfxService;
import com.xunxian.seekingimmortals.skill.effect.SkillContext;
import com.xunxian.seekingimmortals.skill.effect.TechniqueLifecycleVfxService;
import com.xunxian.seekingimmortals.skill.effect.TechniqueVfxPalette;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class ElementalAreaSpell extends SpellEffect {
    private final double range;
    private final double radius;
    private final AreaElement element;
    private final String successKey;

    public ElementalAreaSpell(int cost, int cooldownTicks, double damage, double range, double radius,
                              AreaElement element, String successKey) {
        super(cost, cooldownTicks, damage);
        this.range = range;
        this.radius = radius;
        this.element = element;
        this.successKey = successKey;
    }

    @Override
    public boolean execute(ServerPlayer player, PlayerCultivation cultivation, CultivationSkill skill, SkillContext context) {
        ServerLevel level = player.serverLevel();
        Vec3 center = findImpactPoint(level, player);
        List<LivingEntity> targets = findTargets(level, player, center);
        if (targets.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.spell.area.fail"), true);
            return false;
        }

        double damage = calculateDamage(skill.getLevel(), skill.getProficiency());
        int hitCount = 0;
        for (LivingEntity target : targets) {
            double distance = target.position().distanceTo(center);
            double falloff = Math.max(0.42D, 1.0D - distance / (radius + 0.5D));
            target.hurt(player.damageSources().indirectMagic(player, player), (float)(damage * falloff));
            MobEffect[] appliedEffects = element.applyEffects(target, skill);
            ActiveTechniqueEffectVfxService.track(
                    target,
                    ActiveTechniqueEffectVfxService.semantic(skill, element.name()),
                    TechniqueVfxPalette.familyOf(element.name()),
                    TechniqueVfxPacket.Motif.DOMAIN,
                    Math.max(0.72D, target.getBbWidth() * 0.72D),
                    appliedEffects);
            hitCount++;
        }

        element.spawnVisual(level, center, radius, targets);
        TechniqueLifecycleVfxService.captureGeometry(
                level,
                TechniqueVfxPacket.Kind.IMPACT,
                TechniqueVfxPalette.familyOf(element.name()),
                center,
                center,
                radius,
                42,
                player.getId() * 31L ^ element.ordinal());
        level.playSound(null, center.x, center.y, center.z, element.sound, SoundSource.PLAYERS, 0.78F, element.pitch);
        player.displayClientMessage(Component.translatable(successKey, hitCount), true);
        return true;
    }

    private Vec3 findImpactPoint(ServerLevel level, ServerPlayer player) {
        Vec3 start = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        if (look.lengthSqr() < 0.001D) {
            return player.position();
        }
        Vec3 end = start.add(look.normalize().scale(range));
        BlockHitResult hit = level.clip(new ClipContext(start, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        return hit.getType() == HitResult.Type.MISS ? end : hit.getLocation();
    }

    private List<LivingEntity> findTargets(ServerLevel level, ServerPlayer player, Vec3 center) {
        AABB area = new AABB(center, center).inflate(radius, 1.75D, radius);
        return level.getEntitiesOfClass(LivingEntity.class, area,
                        entity -> canAffect(player, entity)
                                && entity.position().distanceToSqr(center) <= (radius + entity.getBbWidth()) * (radius + entity.getBbWidth()))
                .stream()
                .sorted(Comparator.comparingDouble(entity -> entity.distanceToSqr(center)))
                .toList();
    }

    public enum AreaElement {
        LAVA(new DustParticleOptions(new Vector3f(1.00F, 0.23F, 0.04F), 0.94F),
                new DustParticleOptions(new Vector3f(1.00F, 0.78F, 0.16F), 0.56F),
                SoundEvents.BLAZE_SHOOT, 0.82F),
        MIST_RAIN(new DustParticleOptions(new Vector3f(0.32F, 0.72F, 1.00F), 0.58F),
                new DustParticleOptions(new Vector3f(0.82F, 0.94F, 1.00F), 0.34F),
                SoundEvents.BUCKET_EMPTY, 1.28F),
        SAND_STORM(new DustParticleOptions(new Vector3f(0.78F, 0.58F, 0.25F), 0.72F),
                new DustParticleOptions(new Vector3f(0.95F, 0.80F, 0.42F), 0.46F),
                SoundEvents.SAND_BREAK, 0.78F),
        BLIZZARD(new DustParticleOptions(new Vector3f(0.55F, 0.86F, 1.00F), 0.58F),
                new DustParticleOptions(new Vector3f(0.93F, 0.98F, 1.00F), 0.36F),
                SoundEvents.GLASS_BREAK, 1.45F),
        CYCLONE(new DustParticleOptions(new Vector3f(0.58F, 0.96F, 0.78F), 0.62F),
                new DustParticleOptions(new Vector3f(0.92F, 1.00F, 0.96F), 0.34F),
                SoundEvents.TRIDENT_RIPTIDE_1, 1.62F),
        CHAIN_THUNDER(new DustParticleOptions(new Vector3f(0.72F, 0.92F, 1.00F), 0.72F),
                new DustParticleOptions(new Vector3f(0.28F, 0.46F, 1.00F), 0.46F),
                SoundEvents.LIGHTNING_BOLT_THUNDER, 1.72F),
        METAL_SHARD(new DustParticleOptions(new Vector3f(0.78F, 0.84F, 0.96F), 0.80F),
                new DustParticleOptions(new Vector3f(1.00F, 1.00F, 1.00F), 0.42F),
                SoundEvents.ANVIL_LAND, 1.55F),
        WOOD_BLOOM(new DustParticleOptions(new Vector3f(0.22F, 0.72F, 0.28F), 0.78F),
                new DustParticleOptions(new Vector3f(0.62F, 0.95F, 0.48F), 0.42F),
                SoundEvents.GRASS_BREAK, 1.20F),
        LIGHT_BURST(new DustParticleOptions(new Vector3f(1.00F, 0.90F, 0.35F), 0.82F),
                new DustParticleOptions(new Vector3f(1.00F, 1.00F, 0.88F), 0.42F),
                SoundEvents.AMETHYST_BLOCK_CHIME, 1.45F),
        SOUL_REND(new DustParticleOptions(new Vector3f(0.20F, 0.82F, 0.86F), 0.78F),
                new DustParticleOptions(new Vector3f(0.08F, 0.42F, 0.48F), 0.44F),
                SoundEvents.SOUL_ESCAPE, 1.10F),
        BLOOD_MIST(new DustParticleOptions(new Vector3f(0.72F, 0.05F, 0.08F), 0.88F),
                new DustParticleOptions(new Vector3f(0.42F, 0.02F, 0.04F), 0.50F),
                SoundEvents.WARDEN_HEARTBEAT, 0.85F),
        VOID_RIFT(new DustParticleOptions(new Vector3f(0.34F, 0.08F, 0.58F), 0.86F),
                new DustParticleOptions(new Vector3f(0.72F, 0.42F, 1.00F), 0.48F),
                SoundEvents.ENDERMAN_TELEPORT, 0.80F),
        ILLUSION_HAZE(new DustParticleOptions(new Vector3f(0.78F, 0.42F, 0.96F), 0.72F),
                new DustParticleOptions(new Vector3f(0.96F, 0.78F, 1.00F), 0.40F),
                SoundEvents.ILLUSIONER_CAST_SPELL, 1.25F);

        private final DustParticleOptions core;
        private final DustParticleOptions edge;
        private final SoundEvent sound;
        private final float pitch;

        AreaElement(DustParticleOptions core, DustParticleOptions edge, SoundEvent sound, float pitch) {
            this.core = core;
            this.edge = edge;
            this.sound = sound;
            this.pitch = pitch;
        }

        private MobEffect[] applyEffects(LivingEntity target, CultivationSkill skill) {
            List<MobEffect> appliedEffects = new ArrayList<>(2);
            int levelBonus = Math.max(0, skill.getLevel() - 1);
            switch (this) {
                case LAVA -> {
                    target.setSecondsOnFire(4);
                    add(appliedEffects, target, MobEffects.WEAKNESS, 70 + levelBonus * 6, 0);
                }
                case MIST_RAIN -> {
                    target.clearFire();
                    add(appliedEffects, target, MobEffects.MOVEMENT_SLOWDOWN, 80 + levelBonus * 6, 1);
                    add(appliedEffects, target, MobEffects.WEAKNESS, 60 + levelBonus * 5, 0);
                }
                case SAND_STORM -> {
                    add(appliedEffects, target, MobEffects.MOVEMENT_SLOWDOWN, 95 + levelBonus * 7, 2);
                    add(appliedEffects, target, MobEffects.BLINDNESS, 45 + levelBonus * 4, 0);
                }
                case BLIZZARD -> {
                    add(appliedEffects, target, MobEffects.MOVEMENT_SLOWDOWN, 110 + levelBonus * 8, 2);
                    add(appliedEffects, target, MobEffects.DIG_SLOWDOWN, 90 + levelBonus * 6, 1);
                }
                case CYCLONE -> {
                    add(appliedEffects, target, MobEffects.MOVEMENT_SLOWDOWN, 75 + levelBonus * 5, 1);
                    add(appliedEffects, target, MobEffects.WEAKNESS, 55 + levelBonus * 4, 0);
                    target.push(0.0D, 0.18D, 0.0D);
                }
                case CHAIN_THUNDER -> {
                    add(appliedEffects, target, MobEffects.MOVEMENT_SLOWDOWN, 55 + levelBonus * 4, 1);
                    add(appliedEffects, target, MobEffects.WEAKNESS, 55 + levelBonus * 4, 0);
                }
                case METAL_SHARD -> {
                    add(appliedEffects, target, MobEffects.WEAKNESS, 85 + levelBonus * 6, 1);
                    add(appliedEffects, target, MobEffects.DIG_SLOWDOWN, 70 + levelBonus * 5, 0);
                }
                case WOOD_BLOOM -> {
                    add(appliedEffects, target, MobEffects.POISON, 80 + levelBonus * 6, 0);
                    add(appliedEffects, target, MobEffects.MOVEMENT_SLOWDOWN, 70 + levelBonus * 5, 1);
                }
                case LIGHT_BURST -> {
                    add(appliedEffects, target, MobEffects.GLOWING, 100 + levelBonus * 6, 0);
                    add(appliedEffects, target, MobEffects.WEAKNESS, 60 + levelBonus * 4, 0);
                }
                case SOUL_REND -> {
                    add(appliedEffects, target, MobEffects.WITHER, 70 + levelBonus * 5, 0);
                    add(appliedEffects, target, MobEffects.WEAKNESS, 70 + levelBonus * 5, 0);
                }
                case BLOOD_MIST -> {
                    add(appliedEffects, target, MobEffects.WITHER, 75 + levelBonus * 5, 1);
                    add(appliedEffects, target, MobEffects.HUNGER, 90 + levelBonus * 6, 0);
                }
                case VOID_RIFT -> {
                    add(appliedEffects, target, MobEffects.LEVITATION, 25 + levelBonus * 2, 0);
                    add(appliedEffects, target, MobEffects.BLINDNESS, 40 + levelBonus * 3, 0);
                }
                case ILLUSION_HAZE -> {
                    add(appliedEffects, target, MobEffects.CONFUSION, 100 + levelBonus * 6, 0);
                    add(appliedEffects, target, MobEffects.BLINDNESS, 50 + levelBonus * 4, 0);
                }
            }
            return appliedEffects.toArray(MobEffect[]::new);
        }

        private void spawnVisual(ServerLevel level, Vec3 center, double radius, List<LivingEntity> targets) {
            switch (this) {
                case LAVA -> spawnLavaBurst(level, center, radius);
                case MIST_RAIN -> spawnMistRain(level, center, radius);
                case SAND_STORM -> spawnSandStorm(level, center, radius);
                case BLIZZARD -> spawnBlizzard(level, center, radius);
                case CYCLONE -> spawnCyclone(level, center, radius);
                case CHAIN_THUNDER -> spawnChainThunder(level, center, targets);
                case METAL_SHARD -> spawnMetalShard(level, center, radius);
                case WOOD_BLOOM -> spawnWoodBloom(level, center, radius);
                case LIGHT_BURST -> spawnLightBurst(level, center, radius);
                case SOUL_REND -> spawnSoulRend(level, center, radius);
                case BLOOD_MIST -> spawnBloodMist(level, center, radius);
                case VOID_RIFT -> spawnVoidRift(level, center, radius);
                case ILLUSION_HAZE -> spawnIllusionHaze(level, center, radius);
            }
        }

        private void spawnLavaBurst(ServerLevel level, Vec3 center, double radius) {
            ring(level, center.add(0.0D, 0.08D, 0.0D), radius, 84, 0.18D);
            for (int i = 0; i < 30; i++) {
                double angle = i * Math.PI * 2.0D / 30.0D;
                double spread = radius * (0.18D + (i % 5) * 0.11D);
                level.sendParticles(edge, center.x + Math.cos(angle) * spread, center.y + 0.3D + (i % 4) * 0.18D,
                        center.z + Math.sin(angle) * spread, 1, 0.08D, 0.10D, 0.08D, 0.01D);
            }
            level.sendParticles(core, center.x, center.y + 0.38D, center.z, 36, 0.42D, 0.34D, 0.42D, 0.04D);
        }

        private void spawnMistRain(ServerLevel level, Vec3 center, double radius) {
            ring(level, center.add(0.0D, 2.15D, 0.0D), radius * 0.72D, 64, 0.08D);
            for (int i = 0; i < 52; i++) {
                double angle = i * 2.399963D;
                double spread = radius * Math.sqrt((i % 26) / 26.0D);
                double x = center.x + Math.cos(angle) * spread;
                double z = center.z + Math.sin(angle) * spread;
                for (int drop = 0; drop < 3; drop++) {
                    level.sendParticles(core, x, center.y + 2.1D - drop * 0.55D, z, 1, 0.025D, 0.025D, 0.025D, 0.0D);
                }
            }
            level.sendParticles(edge, center.x, center.y + 1.05D, center.z, 34, radius * 0.34D, 0.45D, radius * 0.34D, 0.0D);
        }

        private void spawnSandStorm(ServerLevel level, Vec3 center, double radius) {
            for (int i = 0; i < 96; i++) {
                double height = 0.08D + (i % 24) * 0.085D;
                double turn = i * 0.42D;
                double swirl = radius * (0.25D + (i % 16) / 22.0D);
                level.sendParticles((i & 1) == 0 ? core : edge,
                        center.x + Math.cos(turn) * swirl, center.y + height,
                        center.z + Math.sin(turn) * swirl, 1, 0.08D, 0.06D, 0.08D, 0.0D);
            }
        }

        private void spawnBlizzard(ServerLevel level, Vec3 center, double radius) {
            ring(level, center.add(0.0D, 0.38D, 0.0D), radius, 76, 0.12D);
            for (int i = 0; i < 88; i++) {
                double angle = i * 0.73D;
                double spread = radius * (0.18D + (i % 20) / 24.0D);
                double y = center.y + 0.25D + (i % 10) * 0.16D;
                level.sendParticles((i % 3) == 0 ? edge : core,
                        center.x + Math.cos(angle) * spread, y,
                        center.z + Math.sin(angle) * spread, 1, 0.06D, 0.05D, 0.06D, 0.0D);
            }
        }

        private void spawnCyclone(ServerLevel level, Vec3 center, double radius) {
            for (int i = 0; i < 112; i++) {
                double height = 0.05D + (i % 28) * 0.105D;
                double turn = i * 0.44D;
                double swirl = radius * (0.18D + height / 4.2D);
                DustParticleOptions particle = (i & 1) == 0 ? core : edge;
                level.sendParticles(particle,
                        center.x + Math.cos(turn) * swirl, center.y + height,
                        center.z + Math.sin(turn) * swirl, 1, 0.035D, 0.04D, 0.035D, 0.0D);
            }
        }

        private void spawnChainThunder(ServerLevel level, Vec3 center, List<LivingEntity> targets) {
            Vec3 previous = center.add(0.0D, 0.9D, 0.0D);
            int chains = Math.min(5, targets.size());
            for (int i = 0; i < chains; i++) {
                LivingEntity target = targets.get(i);
                Vec3 next = target.position().add(0.0D, target.getBbHeight() * 0.58D, 0.0D);
                arc(level, previous, next, i * 31);
                level.sendParticles(edge, next.x, next.y, next.z, 12, 0.24D, 0.18D, 0.24D, 0.02D);
                previous = next;
            }
            level.sendParticles(core, center.x, center.y + 0.65D, center.z, 24, 0.35D, 0.25D, 0.35D, 0.02D);
        }

        private void spawnMetalShard(ServerLevel level, Vec3 center, double radius) {
            ring(level, center.add(0.0D, 0.2D, 0.0D), radius, 72, 0.08D);
            for (int i = 0; i < 48; i++) {
                double angle = i * 0.55D;
                double spread = radius * (0.2D + (i % 12) / 16.0D);
                level.sendParticles((i & 1) == 0 ? core : edge,
                        center.x + Math.cos(angle) * spread, center.y + 0.4D + (i % 6) * 0.12D,
                        center.z + Math.sin(angle) * spread, 1, 0.04D, 0.04D, 0.04D, 0.01D);
            }
            level.sendParticles(core, center.x, center.y + 0.5D, center.z, 20, 0.25D, 0.2D, 0.25D, 0.03D);
        }

        private void spawnWoodBloom(ServerLevel level, Vec3 center, double radius) {
            ring(level, center.add(0.0D, 0.15D, 0.0D), radius * 0.9D, 60, 0.1D);
            for (int i = 0; i < 56; i++) {
                double angle = i * 2.399963D;
                double spread = radius * Math.sqrt((i % 20) / 20.0D);
                level.sendParticles((i % 3) == 0 ? edge : core,
                        center.x + Math.cos(angle) * spread, center.y + 0.2D + (i % 8) * 0.14D,
                        center.z + Math.sin(angle) * spread, 1, 0.03D, 0.05D, 0.03D, 0.0D);
            }
        }

        private void spawnLightBurst(ServerLevel level, Vec3 center, double radius) {
            ring(level, center.add(0.0D, 0.45D, 0.0D), radius, 80, 0.05D);
            level.sendParticles(core, center.x, center.y + 0.6D, center.z, 40, 0.35D, 0.4D, 0.35D, 0.02D);
            for (int i = 0; i < 24; i++) {
                double angle = i * Math.PI * 2.0D / 24.0D;
                level.sendParticles(edge,
                        center.x + Math.cos(angle) * radius * 0.35D, center.y + 0.8D,
                        center.z + Math.sin(angle) * radius * 0.35D, 2, 0.02D, 0.12D, 0.02D, 0.0D);
            }
        }

        private void spawnSoulRend(ServerLevel level, Vec3 center, double radius) {
            for (int i = 0; i < 64; i++) {
                double height = 0.1D + (i % 16) * 0.12D;
                double turn = i * 0.38D;
                double swirl = radius * (0.2D + (i % 10) / 18.0D);
                level.sendParticles((i & 1) == 0 ? core : edge,
                        center.x + Math.cos(turn) * swirl, center.y + height,
                        center.z + Math.sin(turn) * swirl, 1, 0.04D, 0.05D, 0.04D, 0.0D);
            }
            level.sendParticles(core, center.x, center.y + 0.7D, center.z, 18, 0.3D, 0.35D, 0.3D, 0.01D);
        }

        private void spawnBloodMist(ServerLevel level, Vec3 center, double radius) {
            ring(level, center.add(0.0D, 0.12D, 0.0D), radius, 70, 0.12D);
            for (int i = 0; i < 70; i++) {
                double angle = i * 0.61D;
                double spread = radius * (0.15D + (i % 14) / 20.0D);
                level.sendParticles((i % 4) == 0 ? edge : core,
                        center.x + Math.cos(angle) * spread, center.y + 0.15D + (i % 7) * 0.1D,
                        center.z + Math.sin(angle) * spread, 1, 0.05D, 0.04D, 0.05D, 0.0D);
            }
        }

        private void spawnVoidRift(ServerLevel level, Vec3 center, double radius) {
            for (int i = 0; i < 48; i++) {
                double t = i / 48.0D;
                double y = center.y + t * 2.4D;
                double r = radius * (0.15D + Math.sin(t * Math.PI) * 0.55D);
                double angle = i * 0.9D;
                level.sendParticles(core,
                        center.x + Math.cos(angle) * r, y,
                        center.z + Math.sin(angle) * r, 1, 0.03D, 0.03D, 0.03D, 0.0D);
                if ((i & 1) == 0) {
                    level.sendParticles(edge,
                            center.x + Math.cos(angle + 0.4D) * r * 0.7D, y,
                            center.z + Math.sin(angle + 0.4D) * r * 0.7D, 1, 0.04D, 0.04D, 0.04D, 0.0D);
                }
            }
            level.sendParticles(edge, center.x, center.y + 1.0D, center.z, 24, 0.2D, 0.5D, 0.2D, 0.02D);
        }

        private void spawnIllusionHaze(ServerLevel level, Vec3 center, double radius) {
            ring(level, center.add(0.0D, 0.35D, 0.0D), radius, 64, 0.16D);
            for (int i = 0; i < 50; i++) {
                double angle = i * 1.13D;
                double spread = radius * (0.25D + (i % 9) / 14.0D);
                double y = center.y + 0.3D + Math.sin(i * 0.7D) * 0.45D;
                level.sendParticles((i & 1) == 0 ? core : edge,
                        center.x + Math.cos(angle) * spread, y,
                        center.z + Math.sin(angle) * spread, 1, 0.06D, 0.06D, 0.06D, 0.0D);
            }
        }

        private void ring(ServerLevel level, Vec3 center, double radius, int points, double wave) {
            for (int i = 0; i < points; i++) {
                double angle = Math.PI * 2.0D * i / points;
                double y = center.y + Math.sin(angle * 3.0D) * wave;
                DustParticleOptions particle = (i & 1) == 0 ? core : edge;
                level.sendParticles(particle, center.x + Math.cos(angle) * radius, y,
                        center.z + Math.sin(angle) * radius, 1, 0.035D, 0.035D, 0.035D, 0.0D);
            }
        }

        private void arc(ServerLevel level, Vec3 start, Vec3 end, int seed) {
            Vec3 path = end.subtract(start);
            int steps = Math.max(6, (int)(path.length() * 5.0D));
            for (int i = 0; i <= steps; i++) {
                double t = i / (double)steps;
                double sway = Math.sin(seed + i * 1.73D) * 0.12D;
                Vec3 point = start.lerp(end, t).add(Math.cos(seed + i) * 0.08D, sway, Math.sin(seed * 0.5D + i) * 0.08D);
                level.sendParticles(core, point.x, point.y, point.z, 1, 0.02D, 0.02D, 0.02D, 0.0D);
                if ((i & 1) == 0) {
                    level.sendParticles(edge, point.x, point.y, point.z, 1, 0.06D, 0.035D, 0.06D, 0.0D);
                }
            }
        }

        private static void add(List<MobEffect> appliedEffects, LivingEntity target,
                                MobEffect effect, int durationTicks, int amplifier) {
            if (target.addEffect(new MobEffectInstance(effect, durationTicks, amplifier, false, true))) {
                appliedEffects.add(effect);
            }
        }
    }
}
