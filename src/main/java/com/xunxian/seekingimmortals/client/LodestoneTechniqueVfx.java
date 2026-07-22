package com.xunxian.seekingimmortals.client;

import com.xunxian.seekingimmortals.entity.CultivationFireballEntity;
import com.xunxian.seekingimmortals.entity.SwordProjectileEntity;
import com.xunxian.seekingimmortals.network.TechniqueVfxPacket;
import com.xunxian.seekingimmortals.skill.effect.TechniqueVfxPalette;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.registries.RegistryObject;
import org.joml.Vector3f;
import team.lodestar.lodestone.registry.common.particle.LodestoneParticleRegistry;
import team.lodestar.lodestone.systems.easing.Easing;
import team.lodestar.lodestone.systems.particle.builder.WorldParticleBuilder;
import team.lodestar.lodestone.systems.particle.data.GenericParticleData;
import team.lodestar.lodestone.systems.particle.data.color.ColorParticleData;
import team.lodestar.lodestone.systems.particle.data.spin.SpinParticleData;
import team.lodestar.lodestone.systems.particle.world.type.LodestoneWorldParticleType;

import java.util.Random;

@OnlyIn(Dist.CLIENT)
public final class LodestoneTechniqueVfx {
    private static final int MAX_PARTICLES_PER_TICK = 192;
    private static final double MAX_VIEW_DISTANCE_SQR = 96.0D * 96.0D;

    private static long budgetTick = Long.MIN_VALUE;
    private static int particlesThisTick;

    private LodestoneTechniqueVfx() {}

    public static void handle(TechniqueVfxPacket packet) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || minecraft.player == null || packet == null) {
            return;
        }
        Vec3 start = new Vec3(packet.x(), packet.y(), packet.z());
        if (minecraft.player.distanceToSqr(start) > MAX_VIEW_DISTANCE_SQR) {
            return;
        }
        Vec3 end = new Vec3(packet.endX(), packet.endY(), packet.endZ());
        float lod = lodScale(minecraft, minecraft.player.distanceToSqr(start));
        int intensity = Math.max(1, Math.round(packet.intensity() * lod));
        Random random = new Random(packet.seed());

        switch (packet.kind()) {
            case CAST -> cast(level, packet.family(), start, end, packet.radius(), intensity, random);
            case BURST -> burst(level, packet.family(), start, packet.radius(), intensity, random);
            case PATH -> path(level, packet.family(), start, end, intensity, random, false);
            case AURA -> aura(level, packet.family(), start, packet.radius(), intensity, random);
            case SCAN -> scan(level, packet.family(), start, packet.radius(), intensity, random);
            case BEAM -> path(level, packet.family(), start, end, intensity, random, true);
            case CONE -> cone(level, packet.family(), start, end, packet.radius(), intensity, random);
            case IMPACT -> impact(level, packet.family(), start, packet.radius(), intensity, random);
            case FORMATION -> formation(level, packet.family(), start, packet.radius(), intensity, random, packet.seed());
        }
    }

    public static void tickProjectiles() {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || minecraft.player == null || minecraft.isPaused()) {
            return;
        }
        ParticleStatus status = minecraft.options.particles().get();
        int interval = status == ParticleStatus.MINIMAL ? 3 : status == ParticleStatus.DECREASED ? 2 : 1;
        for (Entity entity : level.entitiesForRendering()) {
            if (entity.tickCount % interval != 0 || minecraft.player.distanceToSqr(entity) > 64.0D * 64.0D) {
                continue;
            }
            if (entity instanceof CultivationFireballEntity fireball) {
                projectileTrail(level, fireball, TechniqueVfxPalette.familyOf(fireball.getElement().name()), false);
            } else if (entity instanceof SwordProjectileEntity sword) {
                projectileTrail(level, sword, TechniqueVfxPalette.Family.METAL, true);
            }
        }
    }

    private static void cast(ClientLevel level, TechniqueVfxPalette.Family family, Vec3 start, Vec3 end,
                             float radius, int intensity, Random random) {
        int points = Math.min(24, Math.max(8, intensity / 2));
        ring(level, family, start, Math.max(0.45D, radius), points, random, 0.18F, 18);
        Vec3 direction = normalized(end.subtract(start), new Vec3(0.0D, 0.15D, 1.0D));
        for (int i = 0; i < Math.min(10, intensity / 3 + 2); i++) {
            Vec3 point = start.add(direction.scale(0.08D * i)).add(randomOffset(random, 0.08D));
            spawn(level, LodestoneParticleRegistry.SPARKLE_PARTICLE, family, point,
                    direction.scale(0.025D), 0.20F, 0.92F, 15, random.nextFloat() * 0.8F);
        }
        spawn(level, LodestoneParticleRegistry.STAR_PARTICLE, family, start,
                Vec3.ZERO, 0.42F, 0.95F, 14, random.nextFloat());
    }

    private static void burst(ClientLevel level, TechniqueVfxPalette.Family family, Vec3 center,
                              float radius, int intensity, Random random) {
        int count = Math.min(40, Math.max(8, intensity));
        double spread = Math.max(0.35D, radius);
        for (int i = 0; i < count; i++) {
            Vec3 direction = randomDirection(random);
            Vec3 point = center.add(direction.scale(random.nextDouble() * spread * 0.28D));
            spawn(level, i % 4 == 0 ? LodestoneParticleRegistry.EXTRUDING_SPARK_PARTICLE
                            : LodestoneParticleRegistry.WISP_PARTICLE,
                    family, point, direction.scale(0.035D + random.nextDouble() * 0.055D),
                    i % 4 == 0 ? 0.16F : 0.26F, 0.9F, 18 + random.nextInt(9), random.nextFloat());
        }
        ring(level, family, center, spread * 0.72D, Math.min(28, count), random, 0.16F, 17);
    }

    private static void path(ClientLevel level, TechniqueVfxPalette.Family family, Vec3 start, Vec3 end,
                             int intensity, Random random, boolean beam) {
        Vec3 delta = end.subtract(start);
        double length = delta.length();
        if (length < 0.05D) {
            return;
        }
        Vec3 direction = delta.scale(1.0D / length);
        Vec3 side = perpendicular(direction);
        int points = Math.min(48, Math.max(6, Math.min(intensity, (int) Math.ceil(length * (beam ? 2.8D : 1.8D)))));
        for (int i = 0; i <= points; i++) {
            double progress = i / (double) points;
            Vec3 point = start.add(delta.scale(progress));
            double wave = beam ? Math.sin(progress * Math.PI * 6.0D) * 0.075D : 0.0D;
            spawn(level, beam && (i & 1) == 0 ? LodestoneParticleRegistry.THIN_EXTRUDING_SPARK_PARTICLE
                            : LodestoneParticleRegistry.SPARKLE_PARTICLE,
                    family, point.add(side.scale(wave)), Vec3.ZERO,
                    beam ? 0.13F : 0.17F, 0.88F, beam ? 12 : 16, random.nextFloat());
            if (beam && i % 3 == 0) {
                spawn(level, LodestoneParticleRegistry.WISP_PARTICLE, family,
                        point.add(side.scale(-wave)), direction.scale(0.015D),
                        0.19F, 0.72F, 13, random.nextFloat());
            }
        }
    }

    private static void aura(ClientLevel level, TechniqueVfxPalette.Family family, Vec3 center,
                             float radius, int intensity, Random random) {
        int points = Math.min(36, Math.max(12, intensity));
        ring(level, family, center, Math.max(0.5D, radius), points, random, 0.17F, 24);
        int spirals = Math.min(16, Math.max(6, intensity / 2));
        for (int i = 0; i < spirals; i++) {
            double angle = i * 2.399963229728653D + random.nextDouble() * 0.2D;
            double height = 0.12D + i * 0.11D;
            Vec3 point = center.add(Math.cos(angle) * radius * 0.55D, height, Math.sin(angle) * radius * 0.55D);
            spawn(level, LodestoneParticleRegistry.WISP_PARTICLE, family, point,
                    new Vec3(0.0D, 0.018D, 0.0D), 0.22F, 0.78F, 25, (float) angle);
        }
    }

    private static void scan(ClientLevel level, TechniqueVfxPalette.Family family, Vec3 center,
                             float radius, int intensity, Random random) {
        double safeRadius = Math.max(1.0D, radius);
        int points = Math.min(32, Math.max(12, intensity / 2));
        ring(level, family, center, safeRadius * 0.45D, points, random, 0.12F, 18);
        ring(level, family, center.add(0.0D, 0.04D, 0.0D), safeRadius, points, random, 0.16F, 22);
        for (int i = 0; i < 8; i++) {
            double angle = Math.PI * 2.0D * i / 8.0D;
            Vec3 node = center.add(Math.cos(angle) * safeRadius, 0.12D, Math.sin(angle) * safeRadius);
            spawn(level, LodestoneParticleRegistry.STAR_PARTICLE, family, node,
                    new Vec3(0.0D, 0.008D, 0.0D), 0.24F, 0.9F, 22, (float) angle);
        }
    }

    private static void cone(ClientLevel level, TechniqueVfxPalette.Family family, Vec3 start, Vec3 end,
                             float radius, int intensity, Random random) {
        Vec3 delta = end.subtract(start);
        Vec3 direction = normalized(delta, new Vec3(0.0D, 0.0D, 1.0D));
        Vec3 side = perpendicular(direction);
        Vec3 up = normalized(side.cross(direction), new Vec3(0.0D, 1.0D, 0.0D));
        int slices = Math.min(7, Math.max(3, intensity / 8));
        for (int slice = 1; slice <= slices; slice++) {
            double progress = slice / (double) slices;
            Vec3 center = start.add(delta.scale(progress));
            double sliceRadius = Math.max(0.25D, radius * progress);
            int points = Math.min(12, 4 + slice);
            for (int i = 0; i < points; i++) {
                double angle = Math.PI * 2.0D * i / points;
                Vec3 point = center.add(side.scale(Math.cos(angle) * sliceRadius))
                        .add(up.scale(Math.sin(angle) * sliceRadius));
                spawn(level, LodestoneParticleRegistry.THIN_EXTRUDING_SPARK_PARTICLE, family,
                        point, direction.scale(0.02D), 0.12F, 0.82F, 14, (float) angle);
            }
        }
    }

    private static void impact(ClientLevel level, TechniqueVfxPalette.Family family, Vec3 center,
                               float radius, int intensity, Random random) {
        double safeRadius = Math.max(0.65D, radius);
        ring(level, family, center.add(0.0D, 0.05D, 0.0D), safeRadius, Math.min(32, intensity), random, 0.17F, 18);
        int rays = Math.min(24, Math.max(8, intensity / 2));
        for (int i = 0; i < rays; i++) {
            Vec3 direction = randomDirection(random);
            if (direction.y < -0.15D) {
                direction = new Vec3(direction.x, Math.abs(direction.y) * 0.55D, direction.z).normalize();
            }
            spawn(level, LodestoneParticleRegistry.EXTRUDING_SPARK_PARTICLE, family,
                    center.add(0.0D, 0.18D, 0.0D), direction.scale(0.07D + random.nextDouble() * 0.09D),
                    0.18F, 0.96F, 14 + random.nextInt(8), random.nextFloat());
        }
        spawn(level, LodestoneParticleRegistry.STAR_PARTICLE, family, center.add(0.0D, 0.2D, 0.0D),
                Vec3.ZERO, 0.52F, 1.0F, 12, random.nextFloat());
    }

    private static void formation(ClientLevel level, TechniqueVfxPalette.Family family, Vec3 center,
                                  float radius, int intensity, Random random, long seed) {
        double safeRadius = Mth.clamp(radius, 1.5F, 32.0F);
        double phase = level.getGameTime() * 0.055D + Math.floorMod(seed, 360L) * Math.PI / 180.0D;
        int outerPoints = Math.min(48, Math.max(20, intensity));
        ring(level, family, center.add(0.0D, 0.10D, 0.0D), safeRadius, outerPoints, random, 0.13F, 30);
        rotatingRing(level, family, center.add(0.0D, 0.16D, 0.0D), safeRadius * 0.58D,
                Math.min(28, outerPoints), phase, random);

        for (int i = 0; i < 8; i++) {
            double angle = phase + Math.PI * 2.0D * i / 8.0D;
            Vec3 node = center.add(Math.cos(angle) * safeRadius, 0.15D, Math.sin(angle) * safeRadius);
            spawn(level, (i & 1) == 0 ? LodestoneParticleRegistry.STAR_PARTICLE
                            : LodestoneParticleRegistry.TWINKLE_PARTICLE,
                    family, node, new Vec3(0.0D, 0.012D, 0.0D),
                    0.25F, 0.9F, 30, (float) angle);
        }
        for (int i = 0; i < 7; i++) {
            Vec3 point = center.add(randomOffset(random, safeRadius * 0.15D)).add(0.0D, 0.25D + i * 0.34D, 0.0D);
            spawn(level, LodestoneParticleRegistry.WISP_PARTICLE, family, point,
                    new Vec3(0.0D, 0.018D, 0.0D), 0.24F + i * 0.012F, 0.72F, 34, phaseAsFloat(phase + i));
        }
        if (family == TechniqueVfxPalette.Family.METAL || family == TechniqueVfxPalette.Family.THUNDER) {
            for (int i = 0; i < 8; i++) {
                double angle = phase + Math.PI * 2.0D * i / 8.0D;
                Vec3 from = center.add(Math.cos(angle) * safeRadius * 0.28D, 0.22D,
                        Math.sin(angle) * safeRadius * 0.28D);
                Vec3 to = center.add(Math.cos(angle) * safeRadius * 0.88D, 0.22D,
                        Math.sin(angle) * safeRadius * 0.88D);
                shortLine(level, family, from, to, 3, random);
            }
        } else if (family == TechniqueVfxPalette.Family.VOID
                || family == TechniqueVfxPalette.Family.ILLUSION
                || family == TechniqueVfxPalette.Family.SOUL) {
            rotatingRing(level, family, center.add(0.0D, 0.36D, 0.0D), safeRadius * 0.34D,
                    18, -phase * 1.35D, random);
        }
    }

    private static void projectileTrail(ClientLevel level, Entity entity, TechniqueVfxPalette.Family family,
                                        boolean sword) {
        Vec3 movement = entity.getDeltaMovement();
        Vec3 direction = normalized(movement, new Vec3(0.0D, 0.0D, 1.0D));
        Vec3 center = entity.position().add(0.0D, entity.getBbHeight() * 0.5D, 0.0D);
        double angle = (entity.tickCount + entity.getId() * 3L) * 0.48D;
        Vec3 side = perpendicular(direction);
        Vec3 up = normalized(side.cross(direction), new Vec3(0.0D, 1.0D, 0.0D));
        Vec3 coil = side.scale(Math.cos(angle) * (sword ? 0.08D : 0.13D))
                .add(up.scale(Math.sin(angle) * (sword ? 0.08D : 0.13D)));
        Vec3 tail = center.subtract(direction.scale(sword ? 0.38D : 0.22D)).add(coil);
        Random random = new Random(entity.getId() * 31L + entity.tickCount);
        spawn(level, sword ? LodestoneParticleRegistry.THIN_EXTRUDING_SPARK_PARTICLE
                        : LodestoneParticleRegistry.WISP_PARTICLE,
                family, tail, direction.scale(-0.018D), sword ? 0.12F : 0.22F,
                0.78F, sword ? 10 : 16, (float) angle);
        if (!sword && (entity.tickCount & 1) == 0) {
            spawn(level, LodestoneParticleRegistry.SPARKLE_PARTICLE, family, center.add(coil.scale(-0.7D)),
                    direction.scale(-0.01D), 0.12F, 0.88F, 11, random.nextFloat());
        }
    }

    private static void ring(ClientLevel level, TechniqueVfxPalette.Family family, Vec3 center,
                             double radius, int points, Random random, float scale, int lifetime) {
        for (int i = 0; i < points; i++) {
            double angle = Math.PI * 2.0D * i / points;
            Vec3 point = center.add(Math.cos(angle) * radius, 0.0D, Math.sin(angle) * radius);
            spawn(level, i % 5 == 0 ? LodestoneParticleRegistry.TWINKLE_PARTICLE
                            : LodestoneParticleRegistry.SPARKLE_PARTICLE,
                    family, point, new Vec3(0.0D, 0.004D, 0.0D), scale, 0.78F, lifetime, (float) angle);
        }
    }

    private static void rotatingRing(ClientLevel level, TechniqueVfxPalette.Family family, Vec3 center,
                                     double radius, int points, double phase, Random random) {
        for (int i = 0; i < points; i++) {
            double angle = phase + Math.PI * 2.0D * i / points;
            Vec3 point = center.add(Math.cos(angle) * radius, 0.0D, Math.sin(angle) * radius);
            spawn(level, LodestoneParticleRegistry.WISP_PARTICLE, family, point,
                    new Vec3(-Math.sin(angle) * 0.006D, 0.004D, Math.cos(angle) * 0.006D),
                    0.17F, 0.68F, 30, phaseAsFloat(angle));
        }
    }

    private static void shortLine(ClientLevel level, TechniqueVfxPalette.Family family, Vec3 start, Vec3 end,
                                  int points, Random random) {
        Vec3 delta = end.subtract(start);
        for (int i = 0; i <= points; i++) {
            Vec3 point = start.add(delta.scale(i / (double) points));
            spawn(level, LodestoneParticleRegistry.THIN_EXTRUDING_SPARK_PARTICLE, family, point,
                    Vec3.ZERO, 0.11F, 0.72F, 18, random.nextFloat());
        }
    }

    private static void spawn(ClientLevel level,
                              RegistryObject<? extends LodestoneWorldParticleType> particle,
                              TechniqueVfxPalette.Family family,
                              Vec3 position,
                              Vec3 motion,
                              float scale,
                              float alpha,
                              int lifetime,
                              float spinOffset) {
        if (!claimBudget(level)) {
            return;
        }
        TechniqueVfxPalette.Profile profile = TechniqueVfxPalette.profile(family.name());
        Vector3f start = profile.core().getColor();
        Vector3f end = profile.edge().getColor();
        WorldParticleBuilder.create(particle)
                .setColorData(ColorParticleData.create(
                                start.x(), start.y(), start.z(), end.x(), end.y(), end.z())
                        .setEasing(Easing.QUAD_OUT)
                        .build())
                .setScaleData(GenericParticleData.create(scale, scale * 0.72F, 0.0F)
                        .setEasing(Easing.QUAD_OUT, Easing.CUBIC_IN)
                        .build())
                .setTransparencyData(GenericParticleData.create(alpha, alpha * 0.72F, 0.0F)
                        .setEasing(Easing.QUAD_OUT, Easing.CUBIC_IN)
                        .build())
                .setSpinData(SpinParticleData.create(0.08F, 0.02F, 0.0F)
                        .setSpinOffset(spinOffset)
                        .setEasing(Easing.SINE_OUT, Easing.QUAD_IN)
                        .build())
                .setLifetime(Mth.clamp(lifetime, 6, 40))
                .setMotion(motion)
                .setFrictionStrength(0.94F)
                .setGravityStrength(0.0F)
                .setFullBrightLighting()
                .enableNoClip()
                .spawn(level, position.x, position.y, position.z);
    }

    private static boolean claimBudget(ClientLevel level) {
        long tick = level.getGameTime();
        if (budgetTick != tick) {
            budgetTick = tick;
            particlesThisTick = 0;
        }
        ParticleStatus status = Minecraft.getInstance().options.particles().get();
        int cap = status == ParticleStatus.MINIMAL ? 48
                : status == ParticleStatus.DECREASED ? 112
                : MAX_PARTICLES_PER_TICK;
        if (particlesThisTick >= cap) {
            return false;
        }
        particlesThisTick++;
        return true;
    }

    private static float lodScale(Minecraft minecraft, double distanceSqr) {
        float preference = switch (minecraft.options.particles().get()) {
            case ALL -> 1.0F;
            case DECREASED -> 0.62F;
            case MINIMAL -> 0.30F;
        };
        float distance = distanceSqr <= 24.0D * 24.0D ? 1.0F
                : distanceSqr <= 48.0D * 48.0D ? 0.68F
                : 0.38F;
        return preference * distance;
    }

    private static Vec3 randomDirection(Random random) {
        double x = random.nextDouble() * 2.0D - 1.0D;
        double y = random.nextDouble() * 2.0D - 1.0D;
        double z = random.nextDouble() * 2.0D - 1.0D;
        return normalized(new Vec3(x, y, z), new Vec3(0.0D, 1.0D, 0.0D));
    }

    private static Vec3 randomOffset(Random random, double radius) {
        return randomDirection(random).scale(random.nextDouble() * radius);
    }

    private static Vec3 perpendicular(Vec3 direction) {
        Vec3 side = direction.cross(new Vec3(0.0D, 1.0D, 0.0D));
        return normalized(side, new Vec3(1.0D, 0.0D, 0.0D));
    }

    private static Vec3 normalized(Vec3 value, Vec3 fallback) {
        return value.lengthSqr() < 1.0E-6D ? fallback : value.normalize();
    }

    private static float phaseAsFloat(double value) {
        return (float) (value % (Math.PI * 2.0D));
    }
}
