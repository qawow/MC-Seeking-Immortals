package com.xunxian.seekingimmortals.client;

import com.xunxian.seekingimmortals.network.TechniqueVfxPacket;
import com.xunxian.seekingimmortals.skill.effect.TechniqueVfxPalette;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.registries.RegistryObject;
import org.joml.Vector3f;
import team.lodestar.lodestone.registry.common.particle.LodestoneParticleRegistry;
import team.lodestar.lodestone.systems.easing.Easing;
import team.lodestar.lodestone.systems.particle.builder.WorldParticleBuilder;
import team.lodestar.lodestone.systems.particle.data.GenericParticleData;
import team.lodestar.lodestone.systems.particle.data.color.ColorParticleData;
import team.lodestar.lodestone.systems.particle.data.spin.SpinParticleData;
import team.lodestar.lodestone.systems.particle.render_types.LodestoneWorldParticleRenderType;
import team.lodestar.lodestone.systems.particle.world.behaviors.components.ExtrudingSparkBehaviorComponent;
import team.lodestar.lodestone.systems.particle.world.type.LodestoneWorldParticleType;
import team.lodestar.lodestone.handlers.ScreenshakeHandler;
import team.lodestar.lodestone.systems.screenshake.PositionedScreenshakeInstance;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@OnlyIn(Dist.CLIENT)
public final class LodestoneTechniqueVfx {
    private static final int MAX_PARTICLES_PER_TICK = 192;
    private static final int MAX_SHAKES_PER_TICK = 2;
    private static final int MAX_ACTIVE_VFX = 72;
    private static final double MAX_VIEW_DISTANCE_SQR = 96.0D * 96.0D;
    private static final ParticleRenderType SOFT_GLOW_RENDER_TYPE =
            LodestoneWorldParticleRenderType.LUMITRANSPARENT.withDepthFade();

    private static long budgetTick = Long.MIN_VALUE;
    private static int particlesThisTick;
    private static long shakeBudgetTick = Long.MIN_VALUE;
    private static int shakesThisTick;
    private static int activeVfxCursor;
    private static EmissionBudget activeEmissionBudget;
    private static final List<ActiveVfx> ACTIVE_VFX = new ArrayList<>();
    private static final PaletteColors[] COLOR_CACHE = new PaletteColors[TechniqueVfxPalette.Family.values().length];

    private LodestoneTechniqueVfx() {}

    public static void handle(TechniqueVfxPacket packet) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || minecraft.player == null || packet == null) {
            return;
        }
        Vec3 start = new Vec3(packet.x(), packet.y(), packet.z());
        Vec3 end = new Vec3(packet.endX(), packet.endY(), packet.endZ());
        double distanceSqr = LodestoneVfxMath.distanceToSegmentSqr(
                minecraft.player.position(), start, end);
        if (distanceSqr > MAX_VIEW_DISTANCE_SQR) {
            return;
        }
        Rhythm rhythm = rhythmFor(packet.kind(), packet.motif());
        if (ACTIVE_VFX.size() >= MAX_ACTIVE_VFX) {
            ACTIVE_VFX.remove(0);
            activeVfxCursor = Math.max(0, activeVfxCursor - 1);
        }
        ACTIVE_VFX.add(new ActiveVfx(packet, start, end, rhythm));
        LodestoneWorldGeometry.addIntent(packet, rhythm.anticipationTicks(), rhythm.releaseTicks(),
                rhythm.sustainTicks(), rhythm.afterglowTicks());
    }

    public static void tickProjectiles() {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || minecraft.player == null || minecraft.isPaused()) {
            return;
        }
        tickActiveVfx(minecraft, level);
        LodestoneWorldGeometry.tick(level);
        ParticleStatus status = minecraft.options.particles().get();
        int interval = status == ParticleStatus.MINIMAL ? 3 : status == ParticleStatus.DECREASED ? 2 : 1;
        int projectileQuota = status == ParticleStatus.MINIMAL ? 1 : 2;
        for (LodestoneWorldGeometry.ProjectileSample sample :
                LodestoneWorldGeometry.projectileSamples(minecraft)) {
            Entity entity = sample.entity();
            if (entity.tickCount % interval != 0 || minecraft.player.distanceToSqr(entity) > 64.0D * 64.0D) {
                continue;
            }
            withEventBudget(projectileQuota,
                    () -> projectileTrail(level, entity, sample.family(), sample.sword()));
        }
    }

    public static void trackProjectile(Entity entity) {
        LodestoneWorldGeometry.track(entity);
    }

    public static void untrackProjectile(Entity entity) {
        LodestoneWorldGeometry.untrack(entity);
    }

    public static void renderWorldGeometry(RenderLevelStageEvent event) {
        LodestoneWorldGeometry.render(event);
    }

    public static void reset() {
        ACTIVE_VFX.clear();
        activeVfxCursor = 0;
        activeEmissionBudget = null;
        budgetTick = Long.MIN_VALUE;
        particlesThisTick = 0;
        shakeBudgetTick = Long.MIN_VALUE;
        shakesThisTick = 0;
        LodestoneWorldGeometry.reset();
    }

    private static void tickActiveVfx(Minecraft minecraft, ClientLevel level) {
        if (ACTIVE_VFX.isEmpty()) {
            return;
        }
        List<ActiveVfx> snapshot = List.copyOf(ACTIVE_VFX);
        int size = snapshot.size();
        int start = Math.floorMod(activeVfxCursor, size);
        int perEventCap = eventParticleCap(minecraft.options.particles().get());
        int emittedEvents = 0;
        for (int offset = 0; offset < size; offset++) {
            ActiveVfx active = snapshot.get((start + offset) % size);
            int remainingEvents = size - offset;
            int remaining = remainingParticleBudget(level);
            int fairShare = remaining <= 0 ? 0
                    : Math.max(1, remaining / Math.max(1, remainingEvents));
            int quota = Math.min(perEventCap, fairShare);
            int particlesBefore = particlesThisTick;
            withEventBudget(quota, () -> active.tick(minecraft, level));
            if (particlesThisTick > particlesBefore) {
                emittedEvents++;
            }
        }
        ACTIVE_VFX.removeIf(ActiveVfx::expired);
        activeVfxCursor = ACTIVE_VFX.isEmpty() ? 0
                : (start + Math.max(1, emittedEvents)) % ACTIVE_VFX.size();
    }

    private static void withEventBudget(int quota, Runnable action) {
        EmissionBudget previous = activeEmissionBudget;
        activeEmissionBudget = new EmissionBudget(Math.max(0, quota));
        try {
            action.run();
        } finally {
            activeEmissionBudget = previous;
        }
    }

    private static Rhythm rhythmFor(TechniqueVfxPacket.Kind kind, TechniqueVfxPacket.Motif motif) {
        Rhythm base = switch (kind) {
            case CAST -> new Rhythm(3, 2, 2, 5, 2);
            case BURST -> new Rhythm(2, 1, 1, 5, 2);
            case PATH -> new Rhythm(2, 1, 3, 4, 2);
            case AURA -> new Rhythm(3, 2, 8, 6, 2);
            case SCAN -> new Rhythm(3, 1, 5, 5, 2);
            case BEAM -> new Rhythm(3, 2, 8, 5, 1);
            case CONE -> new Rhythm(2, 2, 3, 4, 2);
            case IMPACT -> new Rhythm(1, 1, 1, 6, 2);
            case FORMATION -> new Rhythm(4, 2, 10, 7, 2);
            case STATUS -> new Rhythm(2, 1, 8, 6, 2);
            case DISSIPATE -> new Rhythm(1, 1, 2, 8, 2);
        };
        return switch (motif) {
            case CHANNEL -> base.adjust(1, 0, 6, 1, -1);
            case DOMAIN, FORMATION -> base.adjust(1, 0, 4, 2, 0);
            case SHIELD, WALL, SEAL -> base.adjust(1, 0, 3, 1, 0);
            case RAIN, SUMMON -> base.adjust(1, 1, 3, 1, 0);
            case TELEPORT, ILLUSION -> base.adjust(1, 1, 1, 2, 0);
            case PROJECTILE, BLADE, CHAIN -> base.adjust(-1, 0, 0, -1, 0);
            case HEAL, CLEANSE -> base.adjust(0, 0, 3, 2, 0);
            case GENERIC, BUDDHIST, CONFUCIAN, DAO, GHOST, TALISMAN -> base;
        };
    }

    private static int eventParticleCap(ParticleStatus status) {
        return status == ParticleStatus.MINIMAL ? 7
                : status == ParticleStatus.DECREASED ? 16 : 30;
    }

    private static int remainingParticleBudget(ClientLevel level) {
        long tick = level.getGameTime();
        if (budgetTick != tick) {
            budgetTick = tick;
            particlesThisTick = 0;
        }
        ParticleStatus status = Minecraft.getInstance().options.particles().get();
        int cap = status == ParticleStatus.MINIMAL ? 48
                : status == ParticleStatus.DECREASED ? 112 : MAX_PARTICLES_PER_TICK;
        return Math.max(0, cap - particlesThisTick);
    }

    private static boolean budgetAvailable(ClientLevel level) {
        return remainingParticleBudget(level) > 0
                && (activeEmissionBudget == null || activeEmissionBudget.remaining > 0);
    }

    private static void emitPacket(Minecraft minecraft, ClientLevel level, ActiveVfx active,
                                   int intensity, Random random, boolean shake) {
        TechniqueVfxPacket packet = active.packet;
        // The authored semantic motif is emitted once at release; the timeline supplies the
        // quieter pulses before and after it.
        embellish(level, packet.kind(), packet.motif(), packet.family(), active.start, active.end,
                packet.radius(), intensity, random);
        switch (packet.kind()) {
            case CAST -> cast(level, packet.family(), active.start, active.end,
                    packet.radius(), intensity, random);
            case BURST -> burst(level, packet.family(), active.start, packet.radius(), intensity, random);
            case PATH -> path(level, packet.family(), active.start, active.end, intensity, random, false);
            case AURA -> aura(level, packet.family(), active.start, packet.radius(), intensity, random);
            case SCAN -> scan(level, packet.family(), active.start, packet.radius(), intensity, random);
            case BEAM -> path(level, packet.family(), active.start, active.end, intensity, random, true);
            case CONE -> cone(level, packet.family(), active.start, active.end,
                    packet.radius(), intensity, random);
            case IMPACT -> impact(level, packet.family(), active.start, packet.radius(), intensity, random);
            case FORMATION -> formation(level, packet.family(), active.start, packet.radius(),
                    intensity, random, packet.seed());
            case STATUS -> status(level, packet.family(), active.start, packet.radius(), intensity, random);
            case DISSIPATE -> dissipate(level, packet.family(), active.start, packet.radius(), intensity, random);
        }
        if (shake && (packet.kind() == TechniqueVfxPacket.Kind.IMPACT
                || packet.kind() == TechniqueVfxPacket.Kind.DISSIPATE) && intensity >= 32) {
            addScreenshake(minecraft, level, active.start, intensity,
                    packet.kind() == TechniqueVfxPacket.Kind.DISSIPATE);
        }
    }

    private enum Phase {
        ANTICIPATION,
        RELEASE,
        SUSTAIN,
        AFTERGLOW
    }

    private record Rhythm(int anticipationTicks, int releaseTicks, int sustainTicks,
                          int afterglowTicks, int sustainInterval) {
        private Rhythm {
            anticipationTicks = Math.max(1, anticipationTicks);
            releaseTicks = Math.max(1, releaseTicks);
            sustainTicks = Math.max(1, sustainTicks);
            afterglowTicks = Math.max(1, afterglowTicks);
            sustainInterval = Math.max(1, sustainInterval);
        }

        private Rhythm adjust(int anticipation, int release, int sustain, int afterglow, int interval) {
            return new Rhythm(anticipationTicks + anticipation, releaseTicks + release,
                    sustainTicks + sustain, afterglowTicks + afterglow,
                    Math.max(1, sustainInterval + interval));
        }

        private int totalTicks() {
            return anticipationTicks + releaseTicks + sustainTicks + afterglowTicks;
        }

        private Phase phaseAt(int age) {
            if (age < anticipationTicks) {
                return Phase.ANTICIPATION;
            }
            if (age < anticipationTicks + releaseTicks) {
                return Phase.RELEASE;
            }
            if (age < anticipationTicks + releaseTicks + sustainTicks) {
                return Phase.SUSTAIN;
            }
            return Phase.AFTERGLOW;
        }

        private int localAge(int age, Phase phase) {
            return switch (phase) {
                case ANTICIPATION -> age;
                case RELEASE -> age - anticipationTicks;
                case SUSTAIN -> age - anticipationTicks - releaseTicks;
                case AFTERGLOW -> age - anticipationTicks - releaseTicks - sustainTicks;
            };
        }
    }

    private static final class ActiveVfx {
        private final TechniqueVfxPacket packet;
        private final Vec3 start;
        private final Vec3 end;
        private final Rhythm rhythm;
        private int age;

        private ActiveVfx(TechniqueVfxPacket packet, Vec3 start, Vec3 end, Rhythm rhythm) {
            this.packet = packet;
            this.start = start;
            this.end = end;
            this.rhythm = rhythm;
        }

        private void tick(Minecraft minecraft, ClientLevel level) {
            Phase phase = rhythm.phaseAt(age);
            int localAge = rhythm.localAge(age, phase);
            float lod = lodScale(minecraft,
                    LodestoneVfxMath.distanceToSegmentSqr(minecraft.player.position(), start, end));
            int authoredIntensity = Math.max(1, Math.round(packet.intensity() * lod));
            Random random = new Random(packet.seed() ^ (0x9E3779B97F4A7C15L * (age + 1L)));
            int particlesBefore = particlesThisTick;
            if (budgetAvailable(level)) {
                switch (phase) {
                    case ANTICIPATION -> {
                        if (localAge == 0 || localAge == rhythm.anticipationTicks() - 1) {
                            int pulse = Math.max(1, authoredIntensity / 3);
                            embellish(level, packet.kind(), packet.motif(), packet.family(), start, end,
                                    packet.radius(), pulse, random);
                            cast(level, packet.family(), start, end, packet.radius(), pulse, random);
                        }
                    }
                    case RELEASE -> {
                        if (localAge == 0) {
                            emitPacket(minecraft, level, this, authoredIntensity, random, true);
                            if (particlesThisTick == particlesBefore && budgetAvailable(level)) {
                                releaseAnchor(level, packet, start, end, random);
                            }
                        } else if (packet.kind() == TechniqueVfxPacket.Kind.BEAM
                                || packet.kind() == TechniqueVfxPacket.Kind.PATH
                                || packet.motif() == TechniqueVfxPacket.Motif.CHANNEL) {
                            path(level, packet.family(), start, end,
                                    Math.max(1, authoredIntensity / 3), random,
                                    packet.kind() == TechniqueVfxPacket.Kind.BEAM);
                        }
                    }
                    case SUSTAIN -> {
                        if (localAge % rhythm.sustainInterval() == 0) {
                            int pulse = Math.max(1, authoredIntensity / 3);
                            sustain(level, packet, start, end, pulse, random);
                        }
                    }
                    case AFTERGLOW -> {
                        if (localAge == 0 || (localAge & 1) == 0) {
                            dissipate(level, packet.family(), start, packet.radius(),
                                    Math.max(1, authoredIntensity / 4), random);
                        }
                    }
                }
            }
            if (phase == Phase.RELEASE && localAge == 0 && particlesThisTick == particlesBefore) {
                return;
            }
            age++;
        }

        private boolean expired() {
            return age >= rhythm.totalTicks();
        }
    }

    private static void sustain(ClientLevel level, TechniqueVfxPacket packet, Vec3 start, Vec3 end,
                                int intensity, Random random) {
        switch (packet.kind()) {
            case AURA -> aura(level, packet.family(), start, packet.radius(), intensity, random);
            case FORMATION -> formation(level, packet.family(), start, packet.radius(), intensity, random,
                    packet.seed());
            case STATUS -> status(level, packet.family(), start, packet.radius(), intensity, random);
            case BEAM -> path(level, packet.family(), start, end, intensity, random, true);
            case PATH, CONE -> path(level, packet.family(), start, end, intensity, random, false);
            case SCAN -> scan(level, packet.family(), start, packet.radius(), intensity, random);
            case BURST, IMPACT, CAST, DISSIPATE -> embellish(level, packet.kind(), packet.motif(),
                    packet.family(), start, end, packet.radius(), intensity, random);
        }
    }

    private static void releaseAnchor(ClientLevel level, TechniqueVfxPacket packet,
                                      Vec3 start, Vec3 end, Random random) {
        Vec3 anchor = switch (packet.kind()) {
            case IMPACT, FORMATION, STATUS, DISSIPATE, AURA, BURST, SCAN -> start;
            case CAST, PATH, BEAM, CONE -> start.lerp(end, 0.5D);
        };
        spawn(level, LodestoneParticleRegistry.STAR_PARTICLE, packet.family(), anchor,
                Vec3.ZERO, 0.28F, 0.9F, 12, random.nextFloat());
    }

    private static void cast(ClientLevel level, TechniqueVfxPalette.Family family, Vec3 start, Vec3 end,
                             float radius, int intensity, Random random) {
        int points = Math.min(24, Math.max(8, intensity / 2));
        ring(level, family, start, Math.max(0.45D, radius), points, random, 0.18F, 18);
        Vec3 direction = normalized(end.subtract(start), new Vec3(0.0D, 0.15D, 1.0D));
        for (int i = 0; i < Math.min(10, intensity / 3 + 2) && budgetAvailable(level); i++) {
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
        for (int i = 0; i < count && budgetAvailable(level); i++) {
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
        for (int i = 0; i <= points && budgetAvailable(level); i++) {
            double progress = i / (double) points;
            Vec3 point = start.add(delta.scale(progress));
            double wave = beam ? Math.sin(progress * Math.PI * 6.0D) * 0.075D : 0.0D;
            spawn(level, beam && (i & 1) == 0 ? LodestoneParticleRegistry.THIN_EXTRUDING_SPARK_PARTICLE
                            : LodestoneParticleRegistry.SPARKLE_PARTICLE,
                    family, point.add(side.scale(wave)), direction.scale(0.003D),
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
        for (int i = 0; i < spirals && budgetAvailable(level); i++) {
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
        for (int i = 0; i < 8 && budgetAvailable(level); i++) {
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
        for (int slice = 1; slice <= slices && budgetAvailable(level); slice++) {
            double progress = slice / (double) slices;
            Vec3 center = start.add(delta.scale(progress));
            double sliceRadius = Math.max(0.25D, radius * progress);
            int points = Math.min(12, 4 + slice);
            for (int i = 0; i < points && budgetAvailable(level); i++) {
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
        for (int i = 0; i < rays && budgetAvailable(level); i++) {
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

        for (int i = 0; i < 8 && budgetAvailable(level); i++) {
            double angle = phase + Math.PI * 2.0D * i / 8.0D;
            Vec3 node = center.add(Math.cos(angle) * safeRadius, 0.15D, Math.sin(angle) * safeRadius);
            spawn(level, (i & 1) == 0 ? LodestoneParticleRegistry.STAR_PARTICLE
                            : LodestoneParticleRegistry.TWINKLE_PARTICLE,
                    family, node, new Vec3(0.0D, 0.012D, 0.0D),
                    0.25F, 0.9F, 30, (float) angle);
        }
        for (int i = 0; i < 7 && budgetAvailable(level); i++) {
            Vec3 point = center.add(randomOffset(random, safeRadius * 0.15D)).add(0.0D, 0.25D + i * 0.34D, 0.0D);
            spawn(level, LodestoneParticleRegistry.WISP_PARTICLE, family, point,
                    new Vec3(0.0D, 0.018D, 0.0D), 0.24F + i * 0.012F, 0.72F, 34, phaseAsFloat(phase + i));
        }
        if (family == TechniqueVfxPalette.Family.METAL || family == TechniqueVfxPalette.Family.THUNDER) {
            for (int i = 0; i < 8 && budgetAvailable(level); i++) {
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

    private static void status(ClientLevel level, TechniqueVfxPalette.Family family, Vec3 center,
                               float radius, int intensity, Random random) {
        double safeRadius = Math.max(0.55D, radius);
        int points = Math.min(20, Math.max(8, intensity / 2));
        double phase = level.getGameTime() * 0.09D;
        rotatingRing(level, family, center.add(0.0D, 0.12D, 0.0D), safeRadius, points, phase, random);
        for (int i = 0; i < Math.min(10, Math.max(4, intensity / 5)) && budgetAvailable(level); i++) {
            double angle = phase + i * 2.399963229728653D;
            Vec3 point = center.add(Math.cos(angle) * safeRadius * 0.62D,
                    0.25D + (i % 5) * 0.27D,
                    Math.sin(angle) * safeRadius * 0.62D);
            spawn(level, LodestoneParticleRegistry.WISP_PARTICLE, family, point,
                    new Vec3(0.0D, 0.014D, 0.0D), 0.18F, 0.62F, 24, (float) angle);
        }
    }

    private static void dissipate(ClientLevel level, TechniqueVfxPalette.Family family, Vec3 center,
                                  float radius, int intensity, Random random) {
        double safeRadius = Math.max(0.65D, radius);
        int count = Math.min(36, Math.max(12, intensity));
        for (int i = 0; i < count && budgetAvailable(level); i++) {
            Vec3 direction = randomDirection(random);
            Vec3 point = center.add(direction.scale(safeRadius * (0.65D + random.nextDouble() * 0.35D)));
            spawn(level, i % 3 == 0 ? LodestoneParticleRegistry.TWINKLE_PARTICLE
                            : LodestoneParticleRegistry.WISP_PARTICLE,
                    family, point, direction.scale(-0.025D - random.nextDouble() * 0.025D),
                    i % 3 == 0 ? 0.16F : 0.24F, 0.72F, 22 + random.nextInt(10), random.nextFloat());
        }
        ring(level, family, center.add(0.0D, 0.08D, 0.0D), safeRadius,
                Math.min(28, count), random, 0.13F, 20);
    }

    private static void embellish(ClientLevel level, TechniqueVfxPacket.Kind kind, TechniqueVfxPacket.Motif motif,
                                  TechniqueVfxPalette.Family family, Vec3 start, Vec3 end,
                                  float radius, int intensity, Random random) {
        switch (motif) {
            case GENERIC -> {
            }
            case PROJECTILE -> projectileMotif(level, family, start, end, intensity, random);
            case BLADE -> bladeMotif(level, family, start, end, radius, intensity, random);
            case SHIELD -> shieldMotif(level, family, start, radius, intensity, random);
            case DOMAIN -> domainMotif(level, family, start, radius, intensity, random, false);
            case TELEPORT -> teleportMotif(level, family, start, end, radius, intensity, random);
            case SUMMON -> summonMotif(level, family, start, radius, intensity, random);
            case WALL -> wallMotif(level, family, start, end, radius, intensity, random);
            case CHAIN -> chainMotif(level, family, start, end, intensity, random);
            case CHANNEL -> channelMotif(level, family, start, end, intensity, random);
            case RAIN -> rainMotif(level, family, start, radius, intensity, random);
            case HEAL -> healMotif(level, family, start, radius, intensity, random, false);
            case CLEANSE -> healMotif(level, family, start, radius, intensity, random, true);
            case SEAL -> sealMotif(level, family, start, radius, intensity, random);
            case FORMATION -> domainMotif(level, family, start, radius, intensity, random, true);
            case BUDDHIST -> buddhistMotif(level, family, start, radius, intensity, random);
            case CONFUCIAN -> confucianMotif(level, family, start, end, radius, intensity, random);
            case DAO -> daoMotif(level, family, start, radius, intensity, random);
            case GHOST -> ghostMotif(level, family, start, end, radius, intensity, random);
            case TALISMAN -> talismanMotif(level, family, start, end, radius, intensity, random);
            case ILLUSION -> illusionMotif(level, family, start, radius, intensity, random);
        }
    }

    private static void projectileMotif(ClientLevel level, TechniqueVfxPalette.Family family, Vec3 start, Vec3 end,
                                        int intensity, Random random) {
        if (start.distanceToSqr(end) < 0.04D) {
            return;
        }
        helix(level, family, start, end, 0.11D, Math.min(24, Math.max(8, intensity / 2)), random);
    }

    private static void bladeMotif(ClientLevel level, TechniqueVfxPalette.Family family, Vec3 start, Vec3 end,
                                   float radius, int intensity, Random random) {
        Vec3 direction = normalized(end.subtract(start), new Vec3(0.0D, 0.0D, 1.0D));
        Vec3 side = perpendicular(direction);
        double width = Math.max(0.18D, Math.min(0.75D, radius * 0.35D));
        shortLine(level, family, start.add(side.scale(width)), end.subtract(side.scale(width)),
                Math.min(18, Math.max(5, intensity / 4)), random);
        shortLine(level, family, start.subtract(side.scale(width)), end.add(side.scale(width)),
                Math.min(18, Math.max(5, intensity / 4)), random);
        int blades = Math.min(10, Math.max(4, intensity / 8));
        for (int i = 0; i < blades && budgetAvailable(level); i++) {
            double angle = Math.PI * 2.0D * i / blades;
            Vec3 base = start.add(Math.cos(angle) * radius, 0.16D, Math.sin(angle) * radius);
            Vec3 tip = base.add(-Math.sin(angle) * 0.28D, 0.38D, Math.cos(angle) * 0.28D);
            shortLine(level, family, base, tip, 3, random);
        }
    }

    private static void shieldMotif(ClientLevel level, TechniqueVfxPalette.Family family, Vec3 center,
                                    float radius, int intensity, Random random) {
        double safeRadius = Math.max(0.8D, radius);
        int points = Math.min(20, Math.max(10, intensity / 3));
        verticalRing(level, family, center.add(0.0D, 0.95D, 0.0D), new Vec3(1.0D, 0.0D, 0.0D),
                safeRadius, points, random, 0.14F);
        verticalRing(level, family, center.add(0.0D, 0.95D, 0.0D), new Vec3(0.0D, 0.0D, 1.0D),
                safeRadius, points, random, 0.14F);
        ring(level, family, center.add(0.0D, 0.95D, 0.0D), safeRadius,
                points, random, 0.13F, 26);
    }

    private static void domainMotif(ClientLevel level, TechniqueVfxPalette.Family family, Vec3 center,
                                    float radius, int intensity, Random random, boolean formation) {
        double safeRadius = Math.max(1.2D, radius);
        int points = formation
                ? Math.min(8, Math.max(6, intensity / 4))
                : Math.min(28, Math.max(12, intensity / 2));
        ring(level, family, center.add(0.0D, 0.06D, 0.0D), safeRadius * 0.42D,
                points, random, 0.11F, 28);
        ring(level, family, center.add(0.0D, 0.10D, 0.0D), safeRadius * 0.72D,
                points, random, 0.12F, 30);
        if (formation) {
            sealMotif(level, family, center, (float) safeRadius, Math.min(intensity, 35), random);
        }
    }

    private static void teleportMotif(ClientLevel level, TechniqueVfxPalette.Family family, Vec3 start, Vec3 end,
                                      float radius, int intensity, Random random) {
        Vec3 normal = normalized(end.subtract(start), new Vec3(0.0D, 0.0D, 1.0D));
        double portalRadius = Math.max(0.65D, radius * 0.62D);
        int points = Math.min(24, Math.max(12, intensity / 2));
        verticalRing(level, family, start.add(0.0D, 0.9D, 0.0D), normal,
                portalRadius, points, random, 0.15F);
        verticalRing(level, family, end.add(0.0D, 0.9D, 0.0D), normal,
                portalRadius, points, random, 0.18F);
        helix(level, family, start.add(0.0D, 0.9D, 0.0D), end.add(0.0D, 0.9D, 0.0D),
                portalRadius * 0.18D, points, random);
    }

    private static void summonMotif(ClientLevel level, TechniqueVfxPalette.Family family, Vec3 center,
                                    float radius, int intensity, Random random) {
        double safeRadius = Math.max(1.0D, radius);
        int pillars = Math.min(8, Math.max(4, intensity / 8));
        for (int i = 0; i < pillars && budgetAvailable(level); i++) {
            double angle = Math.PI * 2.0D * i / pillars;
            Vec3 base = center.add(Math.cos(angle) * safeRadius * 0.68D, 0.1D,
                    Math.sin(angle) * safeRadius * 0.68D);
            shortLine(level, family, base, base.add(0.0D, 1.8D + safeRadius * 0.2D, 0.0D), 5, random);
        }
        ring(level, family, center.add(0.0D, 0.12D, 0.0D), safeRadius,
                Math.min(28, Math.max(12, intensity / 2)), random, 0.14F, 30);
    }

    private static void wallMotif(ClientLevel level, TechniqueVfxPalette.Family family, Vec3 start, Vec3 end,
                                  float radius, int intensity, Random random) {
        Vec3 direction = normalized(end.subtract(start), new Vec3(0.0D, 0.0D, 1.0D));
        Vec3 side = perpendicular(direction);
        double halfWidth = Math.max(1.0D, radius);
        Vec3 center = start.add(end).scale(0.5D);
        int columns = Math.min(9, Math.max(5, intensity / 7));
        for (int i = 0; i < columns && budgetAvailable(level); i++) {
            double offset = -halfWidth + halfWidth * 2.0D * i / (columns - 1.0D);
            Vec3 base = center.add(side.scale(offset));
            shortLine(level, family, base, base.add(0.0D, 2.2D, 0.0D), 5, random);
        }
        shortLine(level, family, center.add(side.scale(-halfWidth)).add(0.0D, 1.1D, 0.0D),
                center.add(side.scale(halfWidth)).add(0.0D, 1.1D, 0.0D), columns, random);
    }

    private static void chainMotif(ClientLevel level, TechniqueVfxPalette.Family family, Vec3 start, Vec3 end,
                                   int intensity, Random random) {
        Vec3 delta = end.subtract(start);
        if (delta.lengthSqr() < 0.04D) {
            return;
        }
        Vec3 side = perpendicular(delta.normalize());
        int links = Math.min(16, Math.max(6, intensity / 4));
        Vec3 previous = start;
        for (int i = 1; i <= links && budgetAvailable(level); i++) {
            double progress = i / (double) links;
            double zigzag = (i & 1) == 0 ? -0.16D : 0.16D;
            Vec3 next = start.add(delta.scale(progress)).add(side.scale(zigzag * (1.0D - progress * 0.35D)));
            shortLine(level, family, previous, next, 2, random);
            previous = next;
        }
    }

    private static void channelMotif(ClientLevel level, TechniqueVfxPalette.Family family, Vec3 start, Vec3 end,
                                     int intensity, Random random) {
        helix(level, family, start, end, 0.18D,
                Math.min(30, Math.max(12, intensity / 2)), random);
        helix(level, family, start, end, -0.18D,
                Math.min(30, Math.max(12, intensity / 2)), random);
    }

    private static void rainMotif(ClientLevel level, TechniqueVfxPalette.Family family, Vec3 center,
                                  float radius, int intensity, Random random) {
        double safeRadius = Math.max(1.5D, radius);
        int streaks = Math.min(18, Math.max(6, intensity / 3));
        for (int i = 0; i < streaks && budgetAvailable(level); i++) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            double distance = Math.sqrt(random.nextDouble()) * safeRadius;
            Vec3 top = center.add(Math.cos(angle) * distance, 2.4D + random.nextDouble(), Math.sin(angle) * distance);
            shortLine(level, family, top, top.add(0.0D, -1.5D, 0.0D), 3, random);
        }
    }

    private static void healMotif(ClientLevel level, TechniqueVfxPalette.Family family, Vec3 center,
                                  float radius, int intensity, Random random, boolean cleanse) {
        double safeRadius = Math.max(0.75D, radius);
        int count = Math.min(18, Math.max(8, intensity / 3));
        for (int i = 0; i < count && budgetAvailable(level); i++) {
            double angle = i * 2.399963229728653D;
            Vec3 point = center.add(Math.cos(angle) * safeRadius * 0.58D,
                    0.12D + (i % 6) * 0.24D,
                    Math.sin(angle) * safeRadius * 0.58D);
            spawn(level, cleanse ? LodestoneParticleRegistry.TWINKLE_PARTICLE : LodestoneParticleRegistry.WISP_PARTICLE,
                    family, point, new Vec3(0.0D, cleanse ? 0.035D : 0.022D, 0.0D),
                    cleanse ? 0.17F : 0.22F, 0.82F, 26, (float) angle);
        }
        ring(level, family, center.add(0.0D, 0.08D, 0.0D), safeRadius,
                Math.min(24, count + 4), random, 0.13F, 25);
    }

    private static void sealMotif(ClientLevel level, TechniqueVfxPalette.Family family, Vec3 center,
                                  float radius, int intensity, Random random) {
        double safeRadius = Math.max(0.9D, radius);
        int nodes = 8;
        Vec3[] points = new Vec3[nodes];
        for (int i = 0; i < nodes && budgetAvailable(level); i++) {
            double angle = Math.PI * 2.0D * i / nodes;
            points[i] = center.add(Math.cos(angle) * safeRadius, 0.13D, Math.sin(angle) * safeRadius);
            spawn(level, LodestoneParticleRegistry.STAR_PARTICLE, family, points[i], Vec3.ZERO,
                    0.20F, 0.9F, 25, (float) angle);
        }
        for (int i = 0; i < nodes && budgetAvailable(level); i++) {
            shortLine(level, family, points[i], points[(i + 3) % nodes],
                    Math.min(4, Math.max(2, intensity / 18)), random);
        }
    }

    private static void buddhistMotif(ClientLevel level, TechniqueVfxPalette.Family family, Vec3 center,
                                      float radius, int intensity, Random random) {
        double safeRadius = Math.max(0.9D, radius);
        ring(level, family, center.add(0.0D, 1.75D, 0.0D), safeRadius * 0.72D,
                Math.min(24, Math.max(12, intensity / 2)), random, 0.18F, 28);
        for (int i = 0; i < 6 && budgetAvailable(level); i++) {
            double angle = Math.PI * 2.0D * i / 6.0D;
            Vec3 base = center.add(Math.cos(angle) * safeRadius, 0.15D, Math.sin(angle) * safeRadius);
            shortLine(level, family, base, base.add(0.0D, 1.35D, 0.0D), 4, random);
        }
    }

    private static void confucianMotif(ClientLevel level, TechniqueVfxPalette.Family family, Vec3 start, Vec3 end,
                                       float radius, int intensity, Random random) {
        Vec3 direction = normalized(end.subtract(start), new Vec3(0.0D, 0.0D, 1.0D));
        Vec3 side = perpendicular(direction);
        Vec3 up = normalized(side.cross(direction), new Vec3(0.0D, 1.0D, 0.0D));
        double size = Math.max(0.55D, radius * 0.55D);
        Vec3 center = start.add(direction.scale(0.5D));
        Vec3 a = center.add(side.scale(size)).add(up.scale(size));
        Vec3 b = center.subtract(side.scale(size)).add(up.scale(size));
        Vec3 c = center.subtract(side.scale(size)).subtract(up.scale(size));
        Vec3 d = center.add(side.scale(size)).subtract(up.scale(size));
        shortLine(level, family, a, b, 4, random);
        shortLine(level, family, b, c, 4, random);
        shortLine(level, family, c, d, 4, random);
        shortLine(level, family, d, a, 4, random);
        shortLine(level, family, b.lerp(c, 0.5D), a.lerp(d, 0.5D), 5, random);
    }

    private static void daoMotif(ClientLevel level, TechniqueVfxPalette.Family family, Vec3 center,
                                 float radius, int intensity, Random random) {
        sealMotif(level, family, center, Math.max(1.0F, radius), intensity, random);
        ring(level, family, center.add(0.0D, 0.18D, 0.0D), Math.max(0.5D, radius * 0.46D),
                Math.min(24, Math.max(12, intensity / 2)), random, 0.12F, 28);
    }

    private static void ghostMotif(ClientLevel level, TechniqueVfxPalette.Family family, Vec3 start, Vec3 end,
                                   float radius, int intensity, Random random) {
        Vec3 target = start.distanceToSqr(end) < 0.04D ? start.add(0.0D, 2.2D, 0.0D) : end;
        helix(level, family, start.add(0.0D, 0.2D, 0.0D), target,
                Math.max(0.14D, radius * 0.18D), Math.min(28, Math.max(12, intensity / 2)), random);
        for (int i = 0; i < Math.min(8, Math.max(4, intensity / 8)) && budgetAvailable(level); i++) {
            Vec3 point = start.add(randomOffset(random, Math.max(0.5D, radius * 0.6D))).add(0.0D, 0.5D, 0.0D);
            spawn(level, LodestoneParticleRegistry.WISP_PARTICLE, family, point,
                    new Vec3(0.0D, 0.025D, 0.0D), 0.26F, 0.58F, 30, random.nextFloat());
        }
    }

    private static void talismanMotif(ClientLevel level, TechniqueVfxPalette.Family family, Vec3 start, Vec3 end,
                                      float radius, int intensity, Random random) {
        Vec3 direction = normalized(end.subtract(start), new Vec3(0.0D, 0.0D, 1.0D));
        Vec3 side = perpendicular(direction);
        Vec3 up = normalized(side.cross(direction), new Vec3(0.0D, 1.0D, 0.0D));
        Vec3 center = start.add(direction.scale(0.42D));
        double size = Math.max(0.35D, radius * 0.45D);
        shortLine(level, family, center.add(up.scale(size)), center.subtract(up.scale(size)), 6, random);
        shortLine(level, family, center.add(side.scale(size * 0.7D)), center.subtract(side.scale(size * 0.7D)), 5, random);
        shortLine(level, family, center.add(up.scale(size * 0.45D)).add(side.scale(size * 0.45D)),
                center.subtract(up.scale(size * 0.45D)).subtract(side.scale(size * 0.45D)), 4, random);
    }

    private static void illusionMotif(ClientLevel level, TechniqueVfxPalette.Family family, Vec3 center,
                                      float radius, int intensity, Random random) {
        double safeRadius = Math.max(0.85D, radius);
        double phase = level.getGameTime() * 0.12D;
        rotatingRing(level, family, center.add(0.0D, 0.55D, 0.0D), safeRadius,
                Math.min(24, Math.max(12, intensity / 2)), phase, random);
        rotatingRing(level, family, center.add(0.0D, 1.2D, 0.0D), safeRadius * 0.72D,
                Math.min(20, Math.max(10, intensity / 3)), -phase * 1.4D, random);
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
        for (int i = 0; i < points && budgetAvailable(level); i++) {
            double angle = Math.PI * 2.0D * i / points;
            Vec3 point = center.add(Math.cos(angle) * radius, 0.0D, Math.sin(angle) * radius);
            spawn(level, i % 5 == 0 ? LodestoneParticleRegistry.TWINKLE_PARTICLE
                            : LodestoneParticleRegistry.SPARKLE_PARTICLE,
                    family, point, new Vec3(0.0D, 0.004D, 0.0D), scale, 0.78F, lifetime, (float) angle);
        }
    }

    private static void rotatingRing(ClientLevel level, TechniqueVfxPalette.Family family, Vec3 center,
                                     double radius, int points, double phase, Random random) {
        for (int i = 0; i < points && budgetAvailable(level); i++) {
            double angle = phase + Math.PI * 2.0D * i / points;
            Vec3 point = center.add(Math.cos(angle) * radius, 0.0D, Math.sin(angle) * radius);
            spawn(level, LodestoneParticleRegistry.WISP_PARTICLE, family, point,
                    new Vec3(-Math.sin(angle) * 0.006D, 0.004D, Math.cos(angle) * 0.006D),
                    0.17F, 0.68F, 30, phaseAsFloat(angle));
        }
    }

    private static void verticalRing(ClientLevel level, TechniqueVfxPalette.Family family, Vec3 center,
                                     Vec3 normal, double radius, int points, Random random, float scale) {
        Vec3 safeNormal = normalized(normal, new Vec3(0.0D, 0.0D, 1.0D));
        Vec3 axisA = perpendicular(safeNormal);
        Vec3 axisB = normalized(safeNormal.cross(axisA), new Vec3(0.0D, 1.0D, 0.0D));
        for (int i = 0; i < points && budgetAvailable(level); i++) {
            double angle = Math.PI * 2.0D * i / points;
            Vec3 point = center.add(axisA.scale(Math.cos(angle) * radius))
                    .add(axisB.scale(Math.sin(angle) * radius));
            spawn(level, i % 5 == 0 ? LodestoneParticleRegistry.STAR_PARTICLE
                            : LodestoneParticleRegistry.SPARKLE_PARTICLE,
                    family, point, Vec3.ZERO, scale, 0.82F, 24, (float) angle);
        }
    }

    private static void helix(ClientLevel level, TechniqueVfxPalette.Family family, Vec3 start, Vec3 end,
                              double radius, int points, Random random) {
        Vec3 delta = end.subtract(start);
        if (delta.lengthSqr() < 0.0025D) {
            return;
        }
        Vec3 direction = delta.normalize();
        Vec3 side = perpendicular(direction);
        Vec3 up = normalized(side.cross(direction), new Vec3(0.0D, 1.0D, 0.0D));
        double helixRadius = Math.abs(radius);
        double directionSign = Math.signum(radius) == 0.0D ? 1.0D : Math.signum(radius);
        for (int i = 0; i <= points && budgetAvailable(level); i++) {
            double progress = i / (double) points;
            double angle = directionSign * progress * Math.PI * 6.0D;
            Vec3 coil = side.scale(Math.cos(angle) * helixRadius)
                    .add(up.scale(Math.sin(angle) * helixRadius));
            Vec3 point = start.add(delta.scale(progress)).add(coil);
            spawn(level, i % 4 == 0 ? LodestoneParticleRegistry.TWINKLE_PARTICLE
                            : LodestoneParticleRegistry.SPARKLE_PARTICLE,
                    family, point, direction.scale(0.008D), 0.12F, 0.76F, 18, (float) angle);
        }
    }

    private static void shortLine(ClientLevel level, TechniqueVfxPalette.Family family, Vec3 start, Vec3 end,
                                  int points, Random random) {
        Vec3 delta = end.subtract(start);
        Vec3 motion = normalized(delta, new Vec3(0.0D, 0.0D, 1.0D)).scale(0.003D);
        for (int i = 0; i <= points && budgetAvailable(level); i++) {
            Vec3 point = start.add(delta.scale(i / (double) points));
            spawn(level, LodestoneParticleRegistry.THIN_EXTRUDING_SPARK_PARTICLE, family, point,
                    motion, 0.11F, 0.72F, 18, random.nextFloat());
        }
    }

    private static void addScreenshake(Minecraft minecraft, ClientLevel level, Vec3 position,
                                       int intensity, boolean collapse) {
        if (minecraft.player == null || minecraft.options.particles().get() == ParticleStatus.MINIMAL) {
            return;
        }
        double distanceSqr = minecraft.player.distanceToSqr(position);
        if (distanceSqr > 32.0D * 32.0D || !claimShake(level)) {
            return;
        }
        float scale = Mth.clamp((intensity - 32) / 64.0F, 0.0F, 1.0F);
        float strength = collapse
                ? 0.88F + scale * 0.06F
                : 0.74F + scale * 0.08F;
        PositionedScreenshakeInstance instance = new PositionedScreenshakeInstance(
                collapse ? 10 : 7, position, 8.0F, 32.0F);
        instance.setIntensity(strength, 0.0F)
                .setEasing(Easing.QUAD_OUT);
        ScreenshakeHandler.addScreenshake(instance);
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
        PaletteColors colors = paletteColors(family);
        ParticleRenderType renderType = particle.getId().getPath().contains("wisp")
                ? SOFT_GLOW_RENDER_TYPE
                : LodestoneWorldParticleRenderType.ADDITIVE;
        WorldParticleBuilder builder = particle.getId().getPath().contains("extruding_spark")
                ? WorldParticleBuilder.create(particle, new ExtrudingSparkBehaviorComponent())
                : WorldParticleBuilder.create(particle);
        builder
                .setColorData(ColorParticleData.create(
                                colors.startR(), colors.startG(), colors.startB(),
                                colors.endR(), colors.endG(), colors.endB())
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
                .setRenderType(renderType)
                .setMotion(motion)
                .setFrictionStrength(0.94F)
                .setGravityStrength(0.0F)
                .setFullBrightLighting()
                .enableNoClip()
                .spawn(level, position.x, position.y, position.z);
    }

    private static boolean claimBudget(ClientLevel level) {
        if (!budgetAvailable(level)) {
            return false;
        }
        particlesThisTick++;
        if (activeEmissionBudget != null) {
            activeEmissionBudget.remaining--;
        }
        return true;
    }

    private static boolean claimShake(ClientLevel level) {
        long tick = level.getGameTime();
        if (shakeBudgetTick != tick) {
            shakeBudgetTick = tick;
            shakesThisTick = 0;
        }
        if (shakesThisTick >= MAX_SHAKES_PER_TICK) {
            return false;
        }
        shakesThisTick++;
        return true;
    }

    private static PaletteColors paletteColors(TechniqueVfxPalette.Family family) {
        TechniqueVfxPalette.Family safeFamily = family == null
                ? TechniqueVfxPalette.Family.NEUTRAL
                : family;
        int index = safeFamily.ordinal();
        PaletteColors cached = COLOR_CACHE[index];
        if (cached != null) {
            return cached;
        }
        TechniqueVfxPalette.Profile profile = TechniqueVfxPalette.profile(safeFamily.name());
        Vector3f start = profile.core().getColor();
        Vector3f end = profile.edge().getColor();
        PaletteColors created = new PaletteColors(
                start.x(), start.y(), start.z(), end.x(), end.y(), end.z());
        COLOR_CACHE[index] = created;
        return created;
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

    private static final class EmissionBudget {
        private int remaining;

        private EmissionBudget(int remaining) {
            this.remaining = remaining;
        }
    }

    private record PaletteColors(float startR, float startG, float startB,
                                 float endR, float endG, float endB) {}
}
