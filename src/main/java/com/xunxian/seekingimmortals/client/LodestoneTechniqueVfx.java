package com.xunxian.seekingimmortals.client;

import com.xunxian.seekingimmortals.network.TechniqueVfxPacket;
import com.xunxian.seekingimmortals.registry.ModParticles;
import com.xunxian.seekingimmortals.skill.effect.TechniqueVfxPalette;
import com.xunxian.seekingimmortals.visual.AuthoredVisualCatalog;
import com.xunxian.seekingimmortals.visual.VisualProfile;
import com.xunxian.seekingimmortals.visual.VisualProgram;
import com.xunxian.seekingimmortals.visual.VisualProgramLayer;
import com.xunxian.seekingimmortals.visual.VisualPrimitive;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.resources.ResourceLocation;
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
import java.util.Locale;
import java.util.Random;

@OnlyIn(Dist.CLIENT)
public final class LodestoneTechniqueVfx {
    private static final int MAX_PARTICLES_PER_TICK = ClientVisualEngine.PARTICLES_ALL;
    private static final int MAX_SHAKES_PER_TICK = 2;
    private static final int MAX_ACTIVE_VFX = ClientVisualEngine.VISIBLE_INSTANCE_LIMIT;
    // Base geometry, semantic motif, and at most one authored particle + one authored trail
    // are the four visual systems allowed for a single packet.
    private static final int MAX_AUTHORED_SYSTEMS_PER_EVENT = 2;
    private static final double MAX_VIEW_DISTANCE_SQR = 96.0D * 96.0D;
    private static final ParticleRenderType SOFT_GLOW_RENDER_TYPE =
            LodestoneWorldParticleRenderType.LUMITRANSPARENT.withDepthFade();

    private static long shakeBudgetTick = Long.MIN_VALUE;
    private static int shakesThisTick;
    private static int activeVfxCursor;
    private static EmissionBudget activeEmissionBudget;
    private static PaletteColors activePaletteOverride;
    private static final List<ActiveVfx> ACTIVE_VFX = new ArrayList<>();
    private static final PaletteColors[] COLOR_CACHE = new PaletteColors[TechniqueVfxPalette.Family.values().length];

    private LodestoneTechniqueVfx() {}

    public static void handle(TechniqueVfxPacket packet) {
        handleProfile(null, packet);
    }

    /** Bridges a resolved authored profile into the existing Lodestone timeline. */
    public static void handleProfile(ResourceLocation profileKey, TechniqueVfxPacket packet) {
        int primaryArgb = profileKey == null ? 0
                : AuthoredVisualCatalog.resolve(profileKey.toString())
                .map(profile -> profile.primaryArgbInt()).orElse(0);
        handleProfile(profileKey, packet, primaryArgb, -1);
    }

    /** Same bridge with a pre-resolved exact authored palette to avoid duplicate catalog lookup. */
    public static void handleProfile(ResourceLocation profileKey, TechniqueVfxPacket packet,
                                     int primaryArgb) {
        handleProfile(profileKey, packet, primaryArgb, -1);
    }

    /** Resolves the local authored event ordinal without adding fields to the wire packet. */
    public static void handleProfile(ResourceLocation profileKey, TechniqueVfxPacket packet,
                                     int primaryArgb, int eventOrdinal) {
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
        Rhythm rhythm = rhythmFor(packet.kind(), packet.motif(), packet.telegraphed());
        if (ACTIVE_VFX.size() >= MAX_ACTIVE_VFX) {
            ACTIVE_VFX.remove(0);
            activeVfxCursor = Math.max(0, activeVfxCursor - 1);
        }
        VisualProfile profile = profileKey == null ? null
                : AuthoredVisualCatalog.resolve(profileKey.toString()).orElse(null);
        VisualProgram visualProgram = profile == null ? VisualProgram.empty() : profile.visualProgram();
        List<VisualProgramLayer> programLayers = visualProgram.forEvent(eventOrdinal);
        ACTIVE_VFX.add(new ActiveVfx(packet, start, end, rhythm,
                PaletteColors.fromArgb(primaryArgb), profile == null ? "" : profile.shape(),
                visualProgram.executable(), programLayers));
        LodestoneWorldGeometry.addProfileIntent(profileKey, packet, primaryArgb,
                rhythm.anticipationTicks(), rhythm.releaseTicks(), rhythm.sustainTicks(),
                rhythm.afterglowTicks());
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
                    () -> projectileTrail(level, entity, sample.family(), sample.trailStyle(),
                            sample.profileKey(), sample.sword()));
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
        activePaletteOverride = null;
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
            int particlesBefore = ClientVisualEngine.particlesUsed(level);
            withEventBudget(quota, () -> active.tick(minecraft, level));
            if (ClientVisualEngine.particlesUsed(level) > particlesBefore) {
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

    private static void withSubBudget(int quota, Runnable action) {
        EmissionBudget parent = activeEmissionBudget;
        int allowed = parent == null ? Math.max(0, quota) : Math.min(Math.max(0, quota), parent.remaining);
        EmissionBudget child = new EmissionBudget(allowed);
        activeEmissionBudget = child;
        try {
            action.run();
        } finally {
            int spent = allowed - child.remaining;
            activeEmissionBudget = parent;
            if (parent != null) {
                parent.remaining = Math.max(0, parent.remaining - spent);
            }
        }
    }

    private static Rhythm rhythmFor(TechniqueVfxPacket.Kind kind, TechniqueVfxPacket.Motif motif,
                                    boolean telegraphed) {
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
        Rhythm semantic = switch (motif) {
            case CHANNEL -> base.adjust(1, 0, 6, 1, -1);
            case DOMAIN, FORMATION -> base.adjust(1, 0, 4, 2, 0);
            case SHIELD, WALL, SEAL -> base.adjust(1, 0, 3, 1, 0);
            case RAIN, SUMMON -> base.adjust(1, 1, 3, 1, 0);
            case TELEPORT, ILLUSION -> base.adjust(1, 1, 1, 2, 0);
            case PROJECTILE, BLADE, CHAIN, MARTIAL -> base.adjust(-1, 0, 0, -1, 0);
            case HEAL, CLEANSE -> base.adjust(0, 0, 3, 2, 0);
            case GENERIC, BUDDHIST, CONFUCIAN, DAO, GHOST, TALISMAN -> base;
        };
        return telegraphed ? semantic.adjust(4, 1, 1, 1, 0) : semantic;
    }

    private static int eventParticleCap(ParticleStatus status) {
        return status == ParticleStatus.MINIMAL ? 7
                : status == ParticleStatus.DECREASED ? 16 : 30;
    }

    private static int remainingParticleBudget(ClientLevel level) {
        return ClientVisualEngine.remainingParticleBudget(level);
    }

    private static boolean budgetAvailable(ClientLevel level) {
        return remainingParticleBudget(level) > 0
                && (activeEmissionBudget == null || activeEmissionBudget.remaining > 0);
    }

    private static void emitPacket(Minecraft minecraft, ClientLevel level, ActiveVfx active,
                                   int intensity, Random random, boolean shake) {
        TechniqueVfxPacket packet = active.packet;
        if (active.authoredProgram) {
            emitAuthoredLayers(level, packet, active.start, active.end, intensity, random, Phase.RELEASE);
            emitVisualProgram(level, packet, active, intensity, random, Phase.RELEASE);
            if (shake && packet.kind() == TechniqueVfxPacket.Kind.IMPACT && intensity >= 32) {
                addScreenshake(minecraft, level, active.start, intensity, false);
            }
            return;
        }
        emitAuthoredLayers(level, packet, active.start, active.end, intensity, random, Phase.RELEASE);
        emitAuthoredShape(level, active.shape, packet.family(), active.start, active.end,
                packet.radius(), intensity, random);
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
        private final PaletteColors paletteOverride;
        private final String shape;
        private final boolean authoredProgram;
        private final List<VisualProgramLayer> programLayers;
        private int age;

        private ActiveVfx(TechniqueVfxPacket packet, Vec3 start, Vec3 end, Rhythm rhythm,
                          PaletteColors paletteOverride, String shape,
                          boolean authoredProgram, List<VisualProgramLayer> programLayers) {
            this.packet = packet;
            this.start = start;
            this.end = end;
            this.rhythm = rhythm;
            this.paletteOverride = paletteOverride;
            this.shape = shape == null ? "" : shape.trim().toLowerCase(Locale.ROOT);
            this.authoredProgram = authoredProgram;
            this.programLayers = programLayers == null ? List.of() : List.copyOf(programLayers);
        }

        private void tick(Minecraft minecraft, ClientLevel level) {
            PaletteColors previous = activePaletteOverride;
            activePaletteOverride = paletteOverride;
            try {
                tickWithPalette(minecraft, level);
            } finally {
                activePaletteOverride = previous;
            }
        }

        private void tickWithPalette(Minecraft minecraft, ClientLevel level) {
            Phase phase = rhythm.phaseAt(age);
            int localAge = rhythm.localAge(age, phase);
            float lod = lodScale(minecraft,
                    LodestoneVfxMath.distanceToSegmentSqr(minecraft.player.position(), start, end));
            int authoredIntensity = Math.max(1, Math.round(packet.intensity() * lod));
            Random random = new Random(packet.seed() ^ (0x9E3779B97F4A7C15L * (age + 1L)));
            int particlesBefore = ClientVisualEngine.particlesUsed(level);
            if (budgetAvailable(level)) {
                switch (phase) {
                    case ANTICIPATION -> {
                        if (localAge == 0 || localAge == rhythm.anticipationTicks() - 1) {
                            int pulse = Math.max(1, authoredIntensity / 3);
                            emitAuthoredLayers(level, packet, start, end, pulse, random, Phase.ANTICIPATION);
                            if (!authoredProgram) {
                                embellish(level, packet.kind(), packet.motif(), packet.family(), start, end,
                                        packet.radius(), pulse, random);
                                cast(level, packet.family(), start, end, packet.radius(), pulse, random);
                            }
                        }
                    }
                    case RELEASE -> {
                        if (localAge == 0) {
                            emitPacket(minecraft, level, this, authoredIntensity, random, true);
                            if (ClientVisualEngine.particlesUsed(level) == particlesBefore
                                    && budgetAvailable(level)) {
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
                            emitAuthoredLayers(level, packet, start, end, pulse, random, Phase.SUSTAIN);
                            if (authoredProgram) {
                                emitVisualProgram(level, packet, this, pulse, random, Phase.SUSTAIN);
                            } else {
                                sustain(level, packet, start, end, pulse, random);
                            }
                        }
                    }
                    case AFTERGLOW -> {
                        if (localAge == 0 || (localAge & 1) == 0) {
                            emitAuthoredLayers(level, packet, start, end,
                                    Math.max(1, authoredIntensity / 4), random, Phase.AFTERGLOW);
                            if (authoredProgram) {
                                emitVisualProgram(level, packet, this,
                                        Math.max(1, authoredIntensity / 4), random, Phase.AFTERGLOW);
                            } else {
                                dissipate(level, packet.family(), start, packet.radius(),
                                        Math.max(1, authoredIntensity / 4), random);
                            }
                        }
                    }
                }
            }
            if (phase == Phase.RELEASE && localAge == 0
                    && ClientVisualEngine.particlesUsed(level) == particlesBefore) {
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
            case MARTIAL -> martialMotif(level, family, start, radius, intensity, random);
        }
    }

    private static void emitAuthoredShape(ClientLevel level, String shape,
                                          TechniqueVfxPalette.Family family,
                                          Vec3 start, Vec3 end, float radius,
                                          int intensity, Random random) {
        switch (shape) {
            case "aura_burst" -> auraBurstShape(level, family, start, radius, intensity, random);
            case "single_projectile" -> singleProjectileShape(level, family, start, end, radius, intensity, random);
            case "giant_claw" -> giantClawShape(level, family, start, end, radius, intensity, random);
            case "giant_hand" -> giantHandShape(level, family, start, end, radius, intensity, random);
            case "fist_barrage" -> barrageShape(level, family, start, end, radius, intensity, random, false);
            case "sword_rain", "projectile_swarm", "falling_barrage" ->
                    barrageShape(level, family, start, end, radius, intensity, random, true);
            case "cloud_vortex" -> vortexShape(level, family, start, radius, intensity, random);
            case "rune_orbit", "array_rings" -> runeOrbitShape(level, family, start, radius, intensity, random);
            case "chain_net", "chain_links" -> chainNetShape(level, family, start, end, radius, intensity, random);
            case "spirit_avatar", "summon_gate" -> spiritAvatarShape(level, family, start, radius, intensity, random);
            case "serpent_dragon" -> serpentShape(level, family, start, end, radius, intensity, random);
            case "eye_gaze" -> eyeGazeShape(level, family, start, end, radius, intensity, random);
            case "sound_wave" -> soundWaveShape(level, family, start, end, radius, intensity, random);
            case "lotus_mandala" -> lotusShape(level, family, start, radius, intensity, random);
            case "mountain_meteor" -> mountainShape(level, family, start, end, radius, intensity, random);
            case "mirror_disc" -> mirrorShape(level, family, start, end, radius, intensity, random);
            case "mist_veil" -> mistVeilShape(level, family, start, end, radius, intensity, random);
            case "flame_bird" -> flameBirdShape(level, family, start, end, radius, intensity, random);
            case "beast_phantom" -> beastPhantomShape(level, family, start, end, radius, intensity, random);
            case "spatial_rift" -> spatialRiftShape(level, family, start, end, radius, intensity, random);
            case "ice_prison" -> icePrisonShape(level, family, start, end, radius, intensity, random);
            case "blood_sea" -> bloodSeaShape(level, family, start, radius, intensity, random);
            case "tree_avatar" -> treeAvatarShape(level, family, start, radius, intensity, random);
            case "scripture_glyph" -> scriptureGlyphShape(level, family, start, end, radius, intensity, random);
            case "magnetic_field" -> magneticFieldShape(level, family, start, radius, intensity, random);
            case "lightning_storm" -> lightningStormShape(level, family, start, end, radius, intensity, random);
            case "wheel_disc" -> wheelDiscShape(level, family, start, end, radius, intensity, random);
            case "spear_spike" -> spearSpikeShape(level, family, start, end, radius, intensity, random);
            case "wing_fan" -> wingFanShape(level, family, start, end, radius, intensity, random);
            case "insect_swarm" -> insectSwarmShape(level, family, start, end, radius, intensity, random);
            case "tidal_wave" -> tidalWaveShape(level, family, start, end, radius, intensity, random);
            case "orb_projectile" -> orbProjectileShape(level, family, start, end, radius, intensity, random);
            case "ground_field", "sphere_field" ->
                    domainMotif(level, family, start, radius, intensity, random, true);
            case "seal_cage", "barrier_plane" -> cageShape(level, family, start, end, radius, intensity, random);
            case "body_aura", "body_shell" -> shieldMotif(level, family, start, radius, intensity, random);
            case "afterimage_path", "layered_afterimages" ->
                    teleportMotif(level, family, start, end, radius, intensity, random);
            case "beam_lance", "channel_stream" ->
                    channelMotif(level, family, start, end, intensity, random);
            case "blade_arc", "impact_arcs" ->
                    bladeMotif(level, family, start, end, radius, intensity, random);
            case "rising_motes" -> healMotif(level, family, start, radius, intensity, random, false);
            case "cleansing_ring" -> healMotif(level, family, start, radius, intensity, random, true);
            case "burning_talisman" -> talismanMotif(level, family, start, end, radius, intensity, random);
            default -> {
            }
        }
    }

    private static void giantClawShape(ClientLevel level, TechniqueVfxPalette.Family family,
                                       Vec3 start, Vec3 end, float radius,
                                       int intensity, Random random) {
        Vec3 center = start.distanceToSqr(end) < 0.04D ? start : end;
        double size = Math.max(0.9D, Math.min(4.0D, radius));
        Vec3 palm = center.add(0.0D, 1.2D + size * 0.45D, 0.0D);
        List<Runnable> fingers = new ArrayList<>();
        for (int finger = 0; finger < 5; finger++) {
            double lateral = (finger - 2.0D) * size * 0.2D;
            double forward = (finger % 2 == 0 ? 0.22D : -0.1D) * size;
            Vec3 knuckle = palm.add(lateral, 0.05D, forward);
            Vec3 mid = center.add(lateral * 1.05D, 0.35D, forward + size * 0.18D);
            Vec3 tip = center.add(lateral * 1.45D, 0.08D, forward + size * 0.42D);
            fingers.add(() -> {
                spawn(level, LodestoneParticleRegistry.SPARKLE_PARTICLE, family, tip,
                        new Vec3(0.0D, -0.01D, 0.0D), 0.12F, 0.75F, 14, random.nextFloat());
                shortLine(level, family, knuckle, mid,
                        Math.min(5, Math.max(3, intensity / 8)), random);
                shortLine(level, family, mid, tip,
                        Math.min(6, Math.max(3, intensity / 7)), random);
            });
        }
        emitFigureComponents(level, 11,
                List.of(() -> ring(level, family, palm, size * 0.38D,
                        Math.min(18, Math.max(8, intensity / 3)), random, 0.18F, 18)),
                fingers);
    }

    private static void barrageShape(ClientLevel level, TechniqueVfxPalette.Family family,
                                     Vec3 start, Vec3 end, float radius,
                                     int intensity, Random random, boolean falling) {
        Vec3 center = start.distanceToSqr(end) < 0.04D ? start : end;
        Vec3 direction = falling ? new Vec3(0.0D, -1.0D, 0.0D)
                : normalized(end.subtract(start), new Vec3(0.0D, 0.0D, 1.0D));
        int streaks = Math.min(12, Math.max(4, intensity / 4));
        double spread = Math.max(0.8D, Math.min(4.5D, radius));
        for (int i = 0; i < streaks && budgetAvailable(level); i++) {
            Vec3 offset = randomOffset(random, spread);
            Vec3 tip = falling ? center.add(offset.x, 2.0D + random.nextDouble() * 1.8D, offset.z)
                    : start.add(offset.scale(0.22D));
            Vec3 tail = falling ? tip.add(direction.scale(1.2D + random.nextDouble()))
                    : end.add(offset.scale(0.65D));
            shortLine(level, family, tip, tail, 3, random);
        }
    }

    private static void vortexShape(ClientLevel level, TechniqueVfxPalette.Family family,
                                    Vec3 center, float radius, int intensity, Random random) {
        double safeRadius = Math.max(1.0D, Math.min(5.0D, radius));
        int layers = Math.min(5, Math.max(3, intensity / 10));
        for (int layer = 0; layer < layers && budgetAvailable(level); layer++) {
            double progress = layer / (double) Math.max(1, layers - 1);
            rotatingRing(level, family, center.add(0.0D, 0.16D + layer * 0.38D, 0.0D),
                    safeRadius * (1.0D - progress * 0.62D),
                    Math.min(18, Math.max(8, intensity / 3)),
                    level.getGameTime() * 0.18D + layer * 0.72D, random);
        }
    }

    private static void runeOrbitShape(ClientLevel level, TechniqueVfxPalette.Family family,
                                       Vec3 center, float radius, int intensity, Random random) {
        double safeRadius = Math.max(0.8D, Math.min(5.0D, radius));
        double phase = level.getGameTime() * 0.14D;
        rotatingRing(level, family, center.add(0.0D, 0.12D, 0.0D), safeRadius,
                Math.min(20, Math.max(10, intensity / 2)), phase, random);
        rotatingRing(level, family, center.add(0.0D, 0.2D, 0.0D), safeRadius * 0.55D,
                Math.min(16, Math.max(8, intensity / 3)), -phase * 1.35D, random);
        for (int node = 0; node < 6 && budgetAvailable(level); node++) {
            double angle = phase + Math.PI * 2.0D * node / 6.0D;
            Vec3 point = center.add(Math.cos(angle) * safeRadius, 0.24D,
                    Math.sin(angle) * safeRadius);
            spawn(level, LodestoneParticleRegistry.STAR_PARTICLE, family, point, Vec3.ZERO,
                    0.20F, 0.94F, 22, (float) angle);
        }
    }

    private static void chainNetShape(ClientLevel level, TechniqueVfxPalette.Family family,
                                      Vec3 start, Vec3 end, float radius,
                                      int intensity, Random random) {
        Vec3 center = start.distanceToSqr(end) < 0.04D ? start : start.add(end).scale(0.5D);
        Vec3 forward = normalized(end.subtract(start), new Vec3(0.0D, 0.0D, 1.0D));
        Vec3 side = perpendicular(forward);
        double half = Math.max(0.8D, Math.min(4.0D, radius));
        int strands = Math.min(5, Math.max(3, intensity / 10));
        for (int i = 0; i < strands && budgetAvailable(level); i++) {
            double offset = -half + half * 2.0D * i / Math.max(1.0D, strands - 1.0D);
            shortLine(level, family, center.add(side.scale(offset)).add(0.0D, -half * 0.45D, 0.0D),
                    center.add(side.scale(offset)).add(0.0D, half * 0.45D, 0.0D), 4, random);
            shortLine(level, family, center.add(side.scale(-half)).add(0.0D, offset * 0.45D, 0.0D),
                    center.add(side.scale(half)).add(0.0D, offset * 0.45D, 0.0D), 4, random);
        }
    }

    private static void spiritAvatarShape(ClientLevel level, TechniqueVfxPalette.Family family,
                                          Vec3 center, float radius, int intensity, Random random) {
        double size = Math.max(0.9D, Math.min(3.2D, radius * 1.05D));
        emitFigureComponents(level, 23, List.of(
                () -> shortLine(level, family, center, center.add(0.0D, size * 1.6D, 0.0D),
                        Math.min(14, Math.max(7, intensity / 3)), random),
                () -> ring(level, family, center.add(0.0D, size * 1.75D, 0.0D), size * 0.22D,
                        Math.min(12, Math.max(6, intensity / 4)), random, 0.14F, 20)),
                List.of(
                        () -> shortLine(level, family,
                                center.add(-size * 0.55D, size * 1.15D, 0.0D),
                                center.add(size * 0.55D, size * 1.15D, 0.0D), 6, random),
                        () -> shortLine(level, family, center.add(-size * 0.5D, size * 1.1D, 0.0D),
                                center.add(-size * 0.95D, size * 0.55D, 0.15D), 5, random),
                        () -> shortLine(level, family, center.add(size * 0.5D, size * 1.1D, 0.0D),
                                center.add(size * 0.95D, size * 0.55D, 0.15D), 5, random),
                        () -> rotatingRing(level, family, center.add(0.0D, size * 0.7D, 0.0D),
                                size * 0.7D, Math.min(16, Math.max(8, intensity / 3)),
                                level.getGameTime() * 0.07D, random),
                        () -> {
                            for (int i = 0; i < 4 && budgetAvailable(level); i++) {
                                spawn(level, LodestoneParticleRegistry.WISP_PARTICLE, family,
                                        center.add(randomOffset(random, size * 0.3D))
                                                .add(0.0D, size * 0.9D, 0.0D),
                                        new Vec3(0.0D, 0.01D, 0.0D),
                                        0.16F, 0.55F, 24, random.nextFloat());
                            }
                        }));
    }

    private static void serpentShape(ClientLevel level, TechniqueVfxPalette.Family family,
                                     Vec3 start, Vec3 end, float radius,
                                     int intensity, Random random) {
        Vec3 direction = normalized(end.subtract(start), new Vec3(0.0D, 0.0D, 1.0D));
        Vec3 tip = start.distanceToSqr(end) < 0.04D ? start.add(direction.scale(2.4D)) : end;
        Vec3 side = perpendicular(direction);
        Vec3 up = normalized(side.cross(direction), new Vec3(0.0D, 1.0D, 0.0D));
        int segments = Math.min(18, Math.max(10, intensity / 2));
        double amp = Math.max(0.25D, Math.min(1.1D, radius * 0.35D));
        emitFigureComponents(level, 37, List.of(
                () -> {
                    int samples = budgetedSamples(segments + 1);
                    for (int sample = 0; sample < samples && budgetAvailable(level); sample++) {
                        double progress = lineSampleProgress(sample, samples);
                        double wave = Math.sin(progress * Math.PI * 3.0D) * amp;
                        Vec3 point = start.lerp(tip, progress)
                                .add(side.scale(wave))
                                .add(up.scale(Math.sin(progress * Math.PI) * amp * 0.35D));
                        spawn(level, LodestoneParticleRegistry.THIN_EXTRUDING_SPARK_PARTICLE,
                                family, point, direction.scale(0.008D),
                                0.13F, 0.76F, 18, random.nextFloat());
                    }
                },
                () -> verticalRing(level, family, tip, direction, Math.max(0.2D, amp * 0.55D),
                        Math.min(12, Math.max(6, intensity / 5)), random, 0.14F)),
                List.of(
                        () -> spawn(level, LodestoneParticleRegistry.STAR_PARTICLE, family, tip,
                                direction.scale(0.02D), 0.18F, 0.9F, 18, random.nextFloat()),
                        () -> {
                            int samples = budgetedSamples(4);
                            for (int sample = 0; sample < samples && budgetAvailable(level); sample++) {
                                double progress = (sample + 1.0D) / (samples + 1.0D);
                                double wave = Math.sin(progress * Math.PI * 3.0D) * amp;
                                Vec3 point = start.lerp(tip, progress).add(side.scale(wave))
                                        .add(up.scale(Math.sin(progress * Math.PI) * amp * 0.35D));
                                spawn(level, LodestoneParticleRegistry.WISP_PARTICLE, family, point,
                                        direction.scale(0.01D), 0.14F, 0.7F, 16, random.nextFloat());
                            }
                        }));
    }

    private static void cageShape(ClientLevel level, TechniqueVfxPalette.Family family,
                                  Vec3 start, Vec3 end, float radius,
                                  int intensity, Random random) {
        Vec3 center = start.distanceToSqr(end) < 0.04D ? start : end;
        double safeRadius = Math.max(0.9D, Math.min(4.0D, radius));
        int bars = Math.min(10, Math.max(6, intensity / 5));
        List<Runnable> barComponents = new ArrayList<>();
        for (int i = 0; i < bars; i++) {
            double angle = Math.PI * 2.0D * i / bars;
            Vec3 base = center.add(Math.cos(angle) * safeRadius, 0.05D,
                    Math.sin(angle) * safeRadius);
            barComponents.add(() -> shortLine(level, family, base,
                    base.add(0.0D, 2.0D + safeRadius * 0.25D, 0.0D), 5, random));
        }
        emitFigureComponents(level, 47, List.of(
                () -> ring(level, family, center.add(0.0D, 0.05D, 0.0D), safeRadius,
                        Math.min(20, Math.max(10, intensity / 2)), random, 0.13F, 18),
                () -> ring(level, family,
                        center.add(0.0D, 2.0D + safeRadius * 0.25D, 0.0D), safeRadius,
                        Math.min(20, Math.max(10, intensity / 2)), random, 0.15F, 20)),
                barComponents);
    }

    private static void auraBurstShape(ClientLevel level, TechniqueVfxPalette.Family family,
                                       Vec3 center, float radius, int intensity, Random random) {
        double safeRadius = Math.max(0.7D, Math.min(4.2D, radius));
        int rays = Math.min(12, Math.max(6, intensity / 4));
        ring(level, family, center.add(0.0D, 0.12D, 0.0D), safeRadius,
                Math.min(22, Math.max(10, intensity / 2)), random, 0.15F, 18);
        for (int i = 0; i < rays && budgetAvailable(level); i++) {
            double angle = Math.PI * 2.0D * i / rays;
            Vec3 direction = new Vec3(Math.cos(angle), 0.16D + (i % 3) * 0.08D, Math.sin(angle)).normalize();
            shortLine(level, family, center.add(0.0D, 0.18D, 0.0D),
                    center.add(0.0D, 0.18D, 0.0D).add(direction.scale(safeRadius)), 3, random);
        }
    }

    private static void singleProjectileShape(ClientLevel level, TechniqueVfxPalette.Family family,
                                              Vec3 start, Vec3 end, float radius,
                                              int intensity, Random random) {
        Vec3 direction = normalized(end.subtract(start), new Vec3(0.0D, 0.0D, 1.0D));
        Vec3 tip = start.distanceToSqr(end) < 0.04D ? start.add(direction.scale(1.2D)) : end;
        shortLine(level, family, start, tip, Math.min(16, Math.max(6, intensity / 3)), random);
        verticalRing(level, family, tip, direction, Math.max(0.16D, Math.min(0.5D, radius * 0.22D)),
                Math.min(12, Math.max(6, intensity / 5)), random, 0.16F);
    }

    private static void eyeGazeShape(ClientLevel level, TechniqueVfxPalette.Family family,
                                     Vec3 start, Vec3 end, float radius,
                                     int intensity, Random random) {
        Vec3 direction = normalized(end.subtract(start), new Vec3(0.0D, 0.0D, 1.0D));
        Vec3 center = start.add(0.0D, 1.25D, 0.0D).add(direction.scale(0.35D));
        Vec3 side = perpendicular(direction);
        Vec3 up = normalized(direction.cross(side), new Vec3(0.0D, 1.0D, 0.0D));
        double width = Math.max(0.45D, Math.min(1.3D, radius * 0.45D));
        Vec3 left = center.subtract(side.scale(width));
        Vec3 right = center.add(side.scale(width));
        emitFigureComponents(level, 17, List.of(
                () -> verticalRing(level, family, center, direction, width,
                        Math.min(18, Math.max(10, intensity / 3)), random, 0.14F),
                () -> verticalRing(level, family, center, direction, width * 0.24D,
                        Math.min(12, Math.max(7, intensity / 4)), random, 0.18F)),
                List.of(
                        () -> shortLine(level, family, left,
                                center.add(up.scale(width * 0.42D)), 5, random),
                        () -> shortLine(level, family,
                                center.add(up.scale(width * 0.42D)), right, 5, random),
                        () -> shortLine(level, family, right,
                                center.subtract(up.scale(width * 0.42D)), 5, random),
                        () -> shortLine(level, family,
                                center.subtract(up.scale(width * 0.42D)), left, 5, random),
                        () -> {
                            if (start.distanceToSqr(end) >= 0.04D) {
                                shortLine(level, family, center, end,
                                        Math.min(18, Math.max(6, intensity / 3)), random);
                            }
                        }));
    }

    private static void soundWaveShape(ClientLevel level, TechniqueVfxPalette.Family family,
                                       Vec3 start, Vec3 end, float radius,
                                       int intensity, Random random) {
        Vec3 direction = normalized(end.subtract(start), new Vec3(0.0D, 0.0D, 1.0D));
        double length = Math.max(1.4D, Math.min(6.0D, start.distanceTo(end)));
        int waves = Math.min(5, Math.max(3, intensity / 10));
        for (int i = 0; i < waves && budgetAvailable(level); i++) {
            double progress = (i + 1.0D) / (waves + 1.0D);
            Vec3 center = start.add(0.0D, 1.1D, 0.0D).add(direction.scale(length * progress));
            verticalRing(level, family, center, direction,
                    Math.max(0.3D, radius * (0.18D + progress * 0.32D)),
                    Math.min(16, Math.max(8, intensity / 4)), random, 0.14F);
        }
    }

    private static void lotusShape(ClientLevel level, TechniqueVfxPalette.Family family,
                                   Vec3 center, float radius, int intensity, Random random) {
        double size = Math.max(0.85D, Math.min(3.6D, radius * 1.05D));
        int petals = Math.min(16, Math.max(10, intensity / 3));
        emitFigureComponents(level, 19, List.of(
                () -> ring(level, family, center.add(0.0D, 0.16D, 0.0D),
                        size * 0.32D, petals, random, 0.15F, 22),
                () -> ring(level, family, center.add(0.0D, 0.22D, 0.0D),
                        size * 0.48D, Math.min(18, petals + 2), random, 0.13F, 24)),
                List.of(
                        () -> ring(level, family, center.add(0.0D, 0.25D, 0.0D),
                                size * 0.92D, petals, random, 0.11F, 18),
                        () -> spawn(level, LodestoneParticleRegistry.STAR_PARTICLE, family,
                                center.add(0.0D, 0.4D, 0.0D), new Vec3(0.0D, 0.01D, 0.0D),
                                0.2F, 0.9F, 26, random.nextFloat()),
                        () -> {
                            for (int i = 0; i < 5 && budgetAvailable(level); i++) {
                                spawn(level, LodestoneParticleRegistry.TWINKLE_PARTICLE, family,
                                        center.add(randomOffset(random, size * 0.15D))
                                                .add(0.0D, 0.45D, 0.0D),
                                        new Vec3(0.0D, 0.008D, 0.0D),
                                        0.12F, 0.7F, 18, random.nextFloat());
                            }
                        }));
    }

    private static void giantHandShape(ClientLevel level, TechniqueVfxPalette.Family family,
                                       Vec3 start, Vec3 end, float radius,
                                       int intensity, Random random) {
        Vec3 center = start.distanceToSqr(end) < 0.04D ? start.add(0.0D, 1.4D, 0.0D) : end;
        double size = Math.max(0.9D, Math.min(3.8D, radius));
        List<Runnable> fingers = new ArrayList<>();
        for (int finger = 0; finger < 5; finger++) {
            double x = (finger - 2.0D) * size * 0.17D;
            Vec3 base = center.add(x, size * 0.18D, 0.0D);
            Vec3 tip = center.add(x * 1.15D, size * (0.72D + (2 - Math.abs(finger - 2)) * 0.08D), 0.0D);
            fingers.add(() -> {
                spawn(level, LodestoneParticleRegistry.SPARKLE_PARTICLE, family, tip,
                        Vec3.ZERO, 0.12F, 0.76F, 16, random.nextFloat());
                shortLine(level, family, base, tip, 5, random);
            });
        }
        emitFigureComponents(level, 29,
                List.of(() -> verticalRing(level, family, center,
                        new Vec3(0.0D, 0.0D, 1.0D), size * 0.42D,
                        Math.min(18, Math.max(9, intensity / 3)), random, 0.2F)),
                fingers);
    }

    private static void mountainShape(ClientLevel level, TechniqueVfxPalette.Family family,
                                      Vec3 start, Vec3 end, float radius,
                                      int intensity, Random random) {
        Vec3 center = start.distanceToSqr(end) < 0.04D ? start : end;
        double size = Math.max(0.9D, Math.min(4.5D, radius));
        Vec3 apex = center.add(0.0D, 1.4D + size * 0.55D, 0.0D);
        Vec3[] base = {center.add(size, 0.05D, 0.0D), center.add(-size, 0.05D, 0.0D),
                center.add(0.0D, 0.05D, size), center.add(0.0D, 0.05D, -size)};
        List<Runnable> slopes = new ArrayList<>();
        for (Vec3 point : base) {
            slopes.add(() -> shortLine(level, family, point, apex,
                    Math.min(8, Math.max(4, intensity / 6)), random));
        }
        emitFigureComponents(level, 31,
                List.of(() -> ring(level, family, center.add(0.0D, 0.06D, 0.0D), size,
                        Math.min(20, Math.max(10, intensity / 3)), random, 0.14F, 20)),
                slopes);
    }

    private static void mirrorShape(ClientLevel level, TechniqueVfxPalette.Family family,
                                    Vec3 start, Vec3 end, float radius,
                                    int intensity, Random random) {
        Vec3 direction = normalized(end.subtract(start), new Vec3(0.0D, 0.0D, 1.0D));
        Vec3 center = start.add(0.0D, 1.0D, 0.0D).add(direction.scale(0.7D));
        double size = Math.max(0.55D, Math.min(1.8D, radius * 0.55D));
        Vec3 side = perpendicular(direction);
        Vec3 up = normalized(direction.cross(side), new Vec3(0.0D, 1.0D, 0.0D));
        emitFigureComponents(level, 43,
                List.of(() -> verticalRing(level, family, center, direction, size,
                        Math.min(22, Math.max(12, intensity / 3)), random, 0.17F)),
                List.of(
                        () -> shortLine(level, family, center.subtract(side.scale(size * 0.75D)),
                                center.add(side.scale(size * 0.75D)), 7, random),
                        () -> shortLine(level, family, center.subtract(up.scale(size * 0.75D)),
                                center.add(up.scale(size * 0.75D)), 7, random)));
    }

    private static void mistVeilShape(ClientLevel level, TechniqueVfxPalette.Family family,
                                      Vec3 start, Vec3 end, float radius,
                                      int intensity, Random random) {
        Vec3 center = start.distanceToSqr(end) < 0.04D ? start.add(0.0D, 0.9D, 0.0D)
                : start.add(end).scale(0.5D);
        double spread = Math.max(0.8D, Math.min(4.2D, radius));
        int count = Math.min(28, Math.max(10, intensity / 2));
        for (int i = 0; i < count && budgetAvailable(level); i++) {
            Vec3 offset = randomOffset(random, spread).multiply(1.0D, 0.38D, 1.0D);
            spawn(level, LodestoneParticleRegistry.WISP_PARTICLE, family, center.add(offset),
                    new Vec3(0.0D, 0.006D + random.nextDouble() * 0.012D, 0.0D),
                    0.25F, 0.48F, 28 + random.nextInt(12), random.nextFloat());
        }
    }

    private static void flameBirdShape(ClientLevel level, TechniqueVfxPalette.Family family,
                                       Vec3 start, Vec3 end, float radius,
                                       int intensity, Random random) {
        Vec3 direction = normalized(end.subtract(start), new Vec3(0.0D, 0.0D, 1.0D));
        Vec3 center = start.distanceToSqr(end) < 0.04D ? start.add(0.0D, 1.0D, 0.0D)
                : start.lerp(end, 0.45D).add(0.0D, 0.8D, 0.0D);
        Vec3 side = perpendicular(direction);
        Vec3 up = normalized(side.cross(direction), new Vec3(0.0D, 1.0D, 0.0D));
        double size = Math.max(0.8D, Math.min(3.0D, radius));
        Vec3 wingRoot = center.add(direction.scale(-0.1D * size));
        Vec3 leftTip = wingRoot.add(side.scale(-size * 0.9D)).add(up.scale(size * 0.05D))
                .subtract(direction.scale(size * 0.48D));
        Vec3 rightTip = wingRoot.add(side.scale(size * 0.9D)).add(up.scale(size * 0.05D))
                .subtract(direction.scale(size * 0.48D));
        emitFigureComponents(level, 41, List.of(
                () -> shortLine(level, family, center.subtract(direction.scale(size * 0.4D)),
                        center.add(direction.scale(size * 0.5D)), 7, random),
                () -> shortLine(level, family, wingRoot, leftTip, 5, random),
                () -> shortLine(level, family, wingRoot, rightTip, 5, random)),
                List.of(
                        () -> {
                            int samples = budgetedSamples(4);
                            for (int sample = 0; sample < samples && budgetAvailable(level); sample++) {
                                double progress = lineSampleProgress(sample, samples);
                                Vec3 tail = center.subtract(direction.scale(size * (0.52D + progress * 0.45D)))
                                        .add(side.scale((progress - 0.5D) * size * 0.2D));
                                spawn(level, LodestoneParticleRegistry.WISP_PARTICLE, family, tail,
                                        direction.scale(-0.015D).add(0.0D, 0.01D, 0.0D),
                                        0.16F, 0.7F, 20, random.nextFloat());
                            }
                        },
                        () -> spawn(level, LodestoneParticleRegistry.STAR_PARTICLE, family,
                                center.add(direction.scale(size * 0.5D)), direction.scale(0.02D),
                                0.17F, 0.88F, 16, random.nextFloat()),
                        () -> {
                            Vec3 innerLeft = wingRoot.add(side.scale(-size * 0.55D))
                                    .subtract(direction.scale(size * 0.2D));
                            Vec3 innerRight = wingRoot.add(side.scale(size * 0.55D))
                                    .subtract(direction.scale(size * 0.2D));
                            shortLine(level, family, wingRoot, innerLeft, 4, random);
                            shortLine(level, family, wingRoot, innerRight, 4, random);
                        }));
    }

    private static void beastPhantomShape(ClientLevel level, TechniqueVfxPalette.Family family,
                                          Vec3 start, Vec3 end, float radius,
                                          int intensity, Random random) {
        Vec3 center = start.distanceToSqr(end) < 0.04D ? start : end;
        double size = Math.max(0.8D, Math.min(3.0D, radius * 0.7D));
        Vec3 bodyStart = center.add(-size * 0.65D, 0.75D, 0.0D);
        Vec3 bodyEnd = center.add(size * 0.55D, 0.8D, 0.0D);
        List<Runnable> limbs = new ArrayList<>();
        for (double x : new double[]{-0.45D, 0.35D}) {
            Vec3 root = center.add(x * size, 0.7D, 0.0D);
            limbs.add(() -> shortLine(level, family, root,
                    center.add(x * size, 0.05D, size * 0.18D), 4, random));
            limbs.add(() -> shortLine(level, family, root,
                    center.add(x * size, 0.05D, -size * 0.18D), 4, random));
        }
        limbs.add(() -> shortLine(level, family, bodyStart,
                bodyStart.add(-size * 0.55D, 0.45D, 0.0D), 5, random));
        emitFigureComponents(level, 49, List.of(
                () -> shortLine(level, family, bodyStart, bodyEnd, 8, random),
                () -> verticalRing(level, family, bodyEnd.add(size * 0.28D, 0.18D, 0.0D),
                        new Vec3(0.0D, 0.0D, 1.0D), size * 0.24D,
                        Math.min(12, Math.max(7, intensity / 4)), random, 0.17F)), limbs);
    }

    private static void spatialRiftShape(ClientLevel level, TechniqueVfxPalette.Family family,
                                         Vec3 start, Vec3 end, float radius,
                                         int intensity, Random random) {
        Vec3 direction = normalized(end.subtract(start), new Vec3(0.0D, 0.0D, 1.0D));
        Vec3 center = start.distanceToSqr(end) < 0.04D ? start.add(0.0D, 1.1D, 0.0D)
                : start.add(end).scale(0.5D);
        Vec3 side = perpendicular(direction);
        double height = Math.max(1.1D, Math.min(3.6D, radius));
        Vec3 previousLeft = center.add(side.scale(-0.12D)).add(0.0D, -height, 0.0D);
        Vec3 previousRight = center.add(side.scale(0.12D)).add(0.0D, -height, 0.0D);
        int segments = Math.min(10, Math.max(6, intensity / 5));
        for (int i = 1; i <= segments && budgetAvailable(level); i++) {
            double y = -height + height * 2.0D * i / segments;
            double jag = (i % 2 == 0 ? 1.0D : -1.0D) * (0.12D + random.nextDouble() * 0.18D);
            Vec3 left = center.add(side.scale(jag - 0.14D)).add(0.0D, y, 0.0D);
            Vec3 right = center.add(side.scale(jag + 0.14D)).add(0.0D, y, 0.0D);
            shortLine(level, family, previousLeft, left, 3, random);
            shortLine(level, family, previousRight, right, 3, random);
            previousLeft = left;
            previousRight = right;
        }
    }

    private static void icePrisonShape(ClientLevel level, TechniqueVfxPalette.Family family,
                                       Vec3 start, Vec3 end, float radius,
                                       int intensity, Random random) {
        cageShape(level, family, start, end, radius, intensity, random);
        Vec3 center = start.distanceToSqr(end) < 0.04D ? start : end;
        double size = Math.max(0.8D, Math.min(3.6D, radius));
        int spikes = Math.min(10, Math.max(6, intensity / 5));
        for (int i = 0; i < spikes && budgetAvailable(level); i++) {
            double angle = Math.PI * 2.0D * i / spikes;
            Vec3 base = center.add(Math.cos(angle) * size, 0.05D, Math.sin(angle) * size);
            Vec3 tip = base.add(Math.cos(angle) * 0.35D, 1.1D + (i % 3) * 0.35D,
                    Math.sin(angle) * 0.35D);
            shortLine(level, family, base, tip, 4, random);
        }
    }

    private static void bloodSeaShape(ClientLevel level, TechniqueVfxPalette.Family family,
                                      Vec3 center, float radius, int intensity, Random random) {
        double size = Math.max(1.0D, Math.min(5.2D, radius));
        double phase = level.getGameTime() * 0.16D;
        rotatingRing(level, family, center.add(0.0D, 0.08D, 0.0D), size,
                Math.min(24, Math.max(12, intensity / 2)), phase, random);
        rotatingRing(level, family, center.add(0.0D, 0.22D, 0.0D), size * 0.66D,
                Math.min(20, Math.max(10, intensity / 3)), -phase * 1.4D, random);
        for (int i = 0; i < Math.min(10, Math.max(5, intensity / 5)) && budgetAvailable(level); i++) {
            Vec3 offset = randomOffset(random, size);
            shortLine(level, family, center.add(offset.x, 0.02D, offset.z),
                    center.add(offset.x, 0.35D + random.nextDouble() * 0.75D, offset.z), 3, random);
        }
    }

    private static void treeAvatarShape(ClientLevel level, TechniqueVfxPalette.Family family,
                                        Vec3 center, float radius, int intensity, Random random) {
        double size = Math.max(1.0D, Math.min(4.0D, radius));
        Vec3 trunkBase = center.add(0.0D, 0.05D, 0.0D);
        Vec3 crown = center.add(0.0D, 1.5D + size * 0.45D, 0.0D);
        shortLine(level, family, trunkBase, crown, 9, random);
        int branches = Math.min(10, Math.max(6, intensity / 5));
        for (int i = 0; i < branches && budgetAvailable(level); i++) {
            double angle = Math.PI * 2.0D * i / branches;
            Vec3 root = center.add(0.0D, 0.8D + (i % 3) * size * 0.18D, 0.0D);
            Vec3 tip = root.add(Math.cos(angle) * size * 0.72D, size * 0.25D,
                    Math.sin(angle) * size * 0.72D);
            shortLine(level, family, root, tip, 4, random);
        }
        ring(level, family, trunkBase, size * 0.82D,
                Math.min(18, Math.max(9, intensity / 3)), random, 0.13F, 22);
    }

    private static void scriptureGlyphShape(ClientLevel level, TechniqueVfxPalette.Family family,
                                            Vec3 start, Vec3 end, float radius,
                                            int intensity, Random random) {
        Vec3 direction = normalized(end.subtract(start), new Vec3(0.0D, 0.0D, 1.0D));
        Vec3 center = start.add(0.0D, 1.1D, 0.0D).add(direction.scale(0.5D));
        Vec3 side = perpendicular(direction);
        Vec3 up = normalized(direction.cross(side), new Vec3(0.0D, 1.0D, 0.0D));
        double size = Math.max(0.55D, Math.min(1.7D, radius * 0.5D));
        Vec3 left = center.subtract(side.scale(size));
        Vec3 right = center.add(side.scale(size));
        shortLine(level, family, left.add(up.scale(size)), right.add(up.scale(size)), 7, random);
        shortLine(level, family, left.subtract(up.scale(size)), right.subtract(up.scale(size)), 7, random);
        shortLine(level, family, left.add(up.scale(size)), left.subtract(up.scale(size)), 7, random);
        shortLine(level, family, right.add(up.scale(size)), right.subtract(up.scale(size)), 7, random);
        for (int row = -1; row <= 1; row++) {
            shortLine(level, family, center.add(side.scale(-size * 0.55D)).add(up.scale(row * size * 0.45D)),
                    center.add(side.scale(size * 0.55D)).add(up.scale(row * size * 0.45D)), 5, random);
        }
    }

    private static void magneticFieldShape(ClientLevel level, TechniqueVfxPalette.Family family,
                                           Vec3 center, float radius, int intensity, Random random) {
        double size = Math.max(0.9D, Math.min(4.0D, radius));
        double phase = level.getGameTime() * 0.18D;
        rotatingRing(level, family, center.add(0.0D, 0.75D, 0.0D), size,
                Math.min(22, Math.max(10, intensity / 2)), phase, random);
        verticalRing(level, family, center.add(0.0D, 0.75D, 0.0D), new Vec3(1.0D, 0.0D, 0.0D),
                size * 0.72D, Math.min(18, Math.max(9, intensity / 3)), random, 0.15F);
        shortLine(level, family, center.add(-size, 0.75D, 0.0D),
                center.add(size, 0.75D, 0.0D), Math.min(14, Math.max(6, intensity / 4)), random);
    }

    private static void lightningStormShape(ClientLevel level, TechniqueVfxPalette.Family family,
                                            Vec3 start, Vec3 end, float radius,
                                            int intensity, Random random) {
        Vec3 center = start.distanceToSqr(end) < 0.04D ? start : end;
        double size = Math.max(1.0D, Math.min(4.8D, radius));
        vortexShape(level, family, center.add(0.0D, 2.2D, 0.0D), radius, Math.min(intensity, 30), random);
        int bolts = Math.min(10, Math.max(4, intensity / 5));
        for (int i = 0; i < bolts && budgetAvailable(level); i++) {
            Vec3 offset = randomOffset(random, size);
            Vec3 top = center.add(offset.x, 2.3D + random.nextDouble(), offset.z);
            Vec3 mid = center.add(offset.x + (random.nextDouble() - 0.5D) * 0.5D, 1.1D, offset.z);
            Vec3 bottom = center.add(offset.x, 0.05D, offset.z);
            shortLine(level, family, top, mid, 3, random);
            shortLine(level, family, mid, bottom, 3, random);
        }
    }

    private static void wheelDiscShape(ClientLevel level, TechniqueVfxPalette.Family family,
                                       Vec3 start, Vec3 end, float radius,
                                       int intensity, Random random) {
        Vec3 direction = normalized(end.subtract(start), new Vec3(0.0D, 0.0D, 1.0D));
        Vec3 center = start.add(0.0D, 1.0D, 0.0D).add(direction.scale(0.55D));
        Vec3 side = perpendicular(direction);
        Vec3 up = normalized(direction.cross(side), new Vec3(0.0D, 1.0D, 0.0D));
        double size = Math.max(0.7D, Math.min(2.6D, radius * 0.7D));
        verticalRing(level, family, center, direction, size,
                Math.min(24, Math.max(12, intensity / 2)), random, 0.17F);
        verticalRing(level, family, center, direction, size * 0.42D,
                Math.min(16, Math.max(8, intensity / 3)), random, 0.15F);
        for (int i = 0; i < 8 && budgetAvailable(level); i++) {
            double angle = Math.PI * 2.0D * i / 8.0D;
            Vec3 tip = center.add(side.scale(Math.cos(angle) * size)).add(up.scale(Math.sin(angle) * size));
            shortLine(level, family, center, tip, 4, random);
        }
    }

    private static void spearSpikeShape(ClientLevel level, TechniqueVfxPalette.Family family,
                                        Vec3 start, Vec3 end, float radius,
                                        int intensity, Random random) {
        Vec3 direction = normalized(end.subtract(start), new Vec3(0.0D, 0.0D, 1.0D));
        Vec3 tip = start.distanceToSqr(end) < 0.04D ? start.add(direction.scale(2.0D)) : end;
        Vec3 side = perpendicular(direction);
        double head = Math.max(0.18D, Math.min(0.65D, radius * 0.24D));
        shortLine(level, family, start, tip, Math.min(18, Math.max(8, intensity / 3)), random);
        Vec3 neck = tip.subtract(direction.scale(head * 1.4D));
        shortLine(level, family, tip, neck.add(side.scale(head)), 4, random);
        shortLine(level, family, tip, neck.subtract(side.scale(head)), 4, random);
    }

    private static void wingFanShape(ClientLevel level, TechniqueVfxPalette.Family family,
                                     Vec3 start, Vec3 end, float radius,
                                     int intensity, Random random) {
        Vec3 direction = normalized(end.subtract(start), new Vec3(0.0D, 0.0D, 1.0D));
        Vec3 center = start.add(0.0D, 1.0D, 0.0D);
        Vec3 side = perpendicular(direction);
        double size = Math.max(0.8D, Math.min(3.5D, radius));
        List<Runnable> innerFeathers = new ArrayList<>();
        Vec3 leftTip = center.add(side.scale(size)).add(direction.scale(size * 0.34D));
        Vec3 rightTip = center.subtract(side.scale(size)).add(direction.scale(size * 0.34D));
        for (int feather = 1; feather < 6; feather++) {
            double progress = feather / 6.0D;
            Vec3 root = center.subtract(direction.scale(progress * size * 0.35D));
            Vec3 forward = direction.scale(size * (0.12D + progress * 0.22D));
            innerFeathers.add(() -> shortLine(level, family, root,
                    root.add(side.scale(size * progress)).add(forward), 4, random));
            innerFeathers.add(() -> shortLine(level, family, root,
                    root.subtract(side.scale(size * progress)).add(forward), 4, random));
        }
        emitFigureComponents(level, 137, List.of(
                () -> shortLine(level, family, center.subtract(direction.scale(size * 0.4D)),
                        center.add(direction.scale(size * 0.45D)), 7, random),
                () -> shortLine(level, family, center, leftTip, 5, random),
                () -> shortLine(level, family, center, rightTip, 5, random)),
                innerFeathers);
    }

    private static void insectSwarmShape(ClientLevel level, TechniqueVfxPalette.Family family,
                                         Vec3 start, Vec3 end, float radius,
                                         int intensity, Random random) {
        Vec3 center = start.distanceToSqr(end) < 0.04D ? start.add(0.0D, 1.0D, 0.0D)
                : start.add(end).scale(0.5D);
        double spread = Math.max(0.7D, Math.min(3.8D, radius));
        int count = Math.min(30, Math.max(12, intensity / 2));
        for (int i = 0; i < count && budgetAvailable(level); i++) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            Vec3 offset = randomOffset(random, spread).multiply(1.0D, 0.55D, 1.0D);
            Vec3 velocity = new Vec3(Math.cos(angle), (random.nextDouble() - 0.5D) * 0.03D,
                    Math.sin(angle)).scale(0.018D);
            spawn(level, i % 4 == 0 ? LodestoneParticleRegistry.STAR_PARTICLE
                            : LodestoneParticleRegistry.WISP_PARTICLE,
                    family, center.add(offset), velocity, 0.12F, 0.78F,
                    18 + random.nextInt(12), (float) angle);
        }
    }

    private static void tidalWaveShape(ClientLevel level, TechniqueVfxPalette.Family family,
                                       Vec3 start, Vec3 end, float radius,
                                       int intensity, Random random) {
        Vec3 direction = normalized(end.subtract(start), new Vec3(0.0D, 0.0D, 1.0D));
        Vec3 side = perpendicular(direction);
        Vec3 center = start.distanceToSqr(end) < 0.04D ? start : start.add(end).scale(0.5D);
        double width = Math.max(1.0D, Math.min(4.8D, radius));
        int segments = Math.min(18, Math.max(10, intensity / 3));
        Vec3 previous = center.subtract(side.scale(width));
        for (int i = 1; i <= segments && budgetAvailable(level); i++) {
            double progress = i / (double) segments;
            double height = Math.sin(progress * Math.PI) * (0.8D + width * 0.3D);
            Vec3 point = center.add(side.scale(-width + width * 2.0D * progress))
                    .add(direction.scale(Math.sin(progress * Math.PI * 2.0D) * 0.25D))
                    .add(0.0D, height, 0.0D);
            shortLine(level, family, previous, point, 3, random);
            previous = point;
        }
    }

    private static void orbProjectileShape(ClientLevel level, TechniqueVfxPalette.Family family,
                                           Vec3 start, Vec3 end, float radius,
                                           int intensity, Random random) {
        Vec3 direction = normalized(end.subtract(start), new Vec3(0.0D, 0.0D, 1.0D));
        Vec3 center = start.distanceToSqr(end) < 0.04D ? start.add(direction.scale(0.8D)) : end;
        double size = Math.max(0.22D, Math.min(0.9D, radius * 0.28D));
        verticalRing(level, family, center, direction, size,
                Math.min(18, Math.max(9, intensity / 3)), random, 0.19F);
        ring(level, family, center, size * 0.86D,
                Math.min(16, Math.max(8, intensity / 4)), random, 0.16F, 20);
        if (start.distanceToSqr(end) >= 0.04D) {
            helix(level, family, start, end, size * 0.24D,
                    Math.min(18, Math.max(8, intensity / 3)), random);
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

    private static void martialMotif(ClientLevel level, TechniqueVfxPalette.Family family, Vec3 center,
                                      float radius, int intensity, Random random) {
        double safeRadius = Math.max(0.75D, Math.min(3.5D, radius));
        int points = Math.min(24, Math.max(8, intensity / 2));
        ring(level, family, center.add(0.0D, 0.06D, 0.0D), safeRadius, points, random, 0.14F, 16);
        for (int i = 0; i < Math.min(12, Math.max(4, intensity / 4)) && budgetAvailable(level); i++) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            Vec3 direction = new Vec3(Math.cos(angle), 0.15D + random.nextDouble() * 0.35D,
                    Math.sin(angle)).normalize();
            spawn(level, LodestoneParticleRegistry.WISP_PARTICLE,
                    family, center.add(0.0D, 0.08D, 0.0D), direction.scale(0.045D),
                    0.18F, 0.62F, 12 + random.nextInt(8), random.nextFloat());
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

    /** Executes source-derived layers; the old shape/motif switch is intentionally bypassed. */
    private static void emitVisualProgram(ClientLevel level, TechniqueVfxPacket packet,
                                          ActiveVfx active, int intensity, Random random,
                                          Phase phase) {
        if (active.programLayers.isEmpty()) {
            return;
        }
        int layerCount = active.programLayers.size();
        for (int index = 0; index < layerCount && budgetAvailable(level); index++) {
            VisualProgramLayer layer = active.programLayers.get(index);
            int remainingLayers = layerCount - index;
            int remaining = activeEmissionBudget == null ? 8 : activeEmissionBudget.remaining;
            int quota = Math.max(1, Math.min(8, remaining / Math.max(1, remainingLayers)));
            withSubBudget(quota, () -> emitProgramLayer(level, packet, active, layer,
                    intensity, random, phase));
        }
    }

    private static void emitProgramLayer(ClientLevel level, TechniqueVfxPacket packet,
                                         ActiveVfx active, VisualProgramLayer layer,
                                         int intensity, Random random, Phase phase) {
        if (!budgetAvailable(level)) {
            return;
        }
        // Faithful copies: stratify named figures across the layer and rotate that sample
        // over time. Each selected copy receives its own sub-budget, so one detailed figure
        // cannot consume the quota reserved for every other named copy.
        int quota = activeEmissionBudget == null ? 8 : Math.max(1, activeEmissionBudget.remaining);
        int namedCopies = Math.max(1, Math.min(72, layer.copies()));
        int samplePhase = (int) (level.getGameTime() + active.age
                + layer.primitive().ordinal() * 31L);
        List<Integer> sampledCopies = VfxBudgetPlan.sampledCopies(
                namedCopies, quota, namedCopies == 1 ? 1 : minimumCopyBudget(layer.primitive()),
                samplePhase);
        int layerIntensity = Math.max(2, intensity / Math.max(1, sampledCopies.size()));
        PaletteColors previous = activePaletteOverride;
        // primary→secondary gradient: secondaryArgb is the authored END color, never a
        // synthetic fade of primary (semantic_layers_v3 fidelity fix).
        PaletteColors layerPalette = PaletteColors.fromArgb(
                (int) layer.primaryArgb(), (int) layer.secondaryArgb());
        if (layerPalette != null) {
            activePaletteOverride = layerPalette;
        }
        try {
            for (int index = 0; index < sampledCopies.size() && budgetAvailable(level); index++) {
                int copy = sampledCopies.get(index);
                int remainingCopies = sampledCopies.size() - index;
                int remaining = activeEmissionBudget == null ? quota : activeEmissionBudget.remaining;
                int copyQuota = Math.max(1, remaining / Math.max(1, remainingCopies));
                withSubBudget(copyQuota, () -> {
                    ProgramCoordinates coordinates = programCoordinates(
                            level, active, layer, copy, random, phase);
                    float radius = (float) Math.max(0.1D, packet.radius() * layer.radiusScale()
                            * motionRadius(layer.motion(), phase));
                    // Layer-scoped figure dispatch for v3 silhouettes. These never enter the
                    // emitAuthoredShape switch (which is contract-pinned to the 52 spell shapes).
                    if (!emitFigurePrimitive(level, layer.primitive(), packet.family(),
                            coordinates.start(), coordinates.end(), radius, layerIntensity, random)) {
                        emitAuthoredShape(level, layer.primitive().id(), packet.family(),
                                coordinates.start(), coordinates.end(), radius,
                                layerIntensity, random);
                    }
                });
            }
        } finally {
            activePaletteOverride = previous;
        }
    }

    private static int minimumCopyBudget(VisualPrimitive primitive) {
        return switch (primitive) {
            case GIANT_CLAW, GIANT_HAND, SPIRIT_AVATAR, SUMMON_GATE, SERPENT_DRAGON,
                    EYE_GAZE, LOTUS_MANDALA, MOUNTAIN_METEOR, MIRROR_DISC, FLAME_BIRD,
                    BEAST_PHANTOM, WING_FAN, SEAL_CAGE, BARRIER_PLANE,
                    CAULDRON_VESSEL, BELL_CHIME, GOURD_VESSEL, LIGHT_CURTAIN,
                    HALO_RING, BANNER_STREAMER, SEAL_STAMP, FLYING_SWORD,
                    FORMATION_BANNER, PAGODA_TOWER, JADE_SLIP, FIRE_PLUME,
                    GHOST_HEAD, SHIELD_PLATE, FLYING_BLADE, GIANT_AXE,
                    RITUAL_BOWL, MAGIC_RULER, GIANT_HAMMER, MAGIC_STAFF,
                    RITUAL_LAMP, SPIRIT_QIN, RITUAL_COFFIN, TALISMAN_BRUSH,
                    MAGIC_FAN, ALCHEMY_FURNACE, MAGIC_SCROLL, FORMATION_DISC,
                    SPIKED_CLUB, COMMAND_TOKEN, MAGIC_SCISSORS, MAGIC_BRICK,
                    MAGIC_UMBRELLA, MAGIC_BOW -> 2;
            default -> 1;
        };
    }

    /**
     * Renders semantic_layers_v3 figure silhouettes before the contract-pinned shape switch.
     * Returns true when the primitive was handled so emitAuthoredShape is skipped.
     */
    private static boolean emitFigurePrimitive(ClientLevel level, VisualPrimitive primitive,
                                               TechniqueVfxPalette.Family family,
                                               Vec3 start, Vec3 end, float radius,
                                               int intensity, Random random) {
        return switch (primitive) {
            case CAULDRON_VESSEL -> {
                cauldronVesselShape(level, family, start, end, radius, intensity, random);
                yield true;
            }
            case ALCHEMY_FURNACE -> {
                alchemyFurnaceShape(level, family, start, end, radius, intensity, random);
                yield true;
            }
            case BELL_CHIME -> {
                bellChimeShape(level, family, start, end, radius, intensity, random);
                yield true;
            }
            case GOURD_VESSEL -> {
                gourdVesselShape(level, family, start, end, radius, intensity, random);
                yield true;
            }
            case RITUAL_BOWL -> {
                ritualBowlShape(level, family, start, end, radius, intensity, random);
                yield true;
            }
            case RITUAL_LAMP -> {
                ritualLampShape(level, family, start, end, radius, intensity, random);
                yield true;
            }
            case RITUAL_COFFIN -> {
                ritualCoffinShape(level, family, start, end, radius, intensity, random);
                yield true;
            }
            case LIGHT_CURTAIN -> {
                lightCurtainShape(level, family, start, end, radius, intensity, random);
                yield true;
            }
            case HALO_RING -> {
                haloRingShape(level, family, start, end, radius, intensity, random);
                yield true;
            }
            case MAGIC_RULER -> {
                magicRulerShape(level, family, start, end, radius, intensity, random);
                yield true;
            }
            case MAGIC_STAFF -> {
                magicStaffShape(level, family, start, end, radius, intensity, random);
                yield true;
            }
            case MAGIC_FAN -> {
                magicFanShape(level, family, start, end, radius, intensity, random);
                yield true;
            }
            case BANNER_STREAMER -> {
                bannerStreamerShape(level, family, start, end, radius, intensity, random);
                yield true;
            }
            case SEAL_STAMP -> {
                sealStampShape(level, family, start, end, radius, intensity, random);
                yield true;
            }
            case BRIDGE_ARC -> {
                bridgeArcShape(level, family, start, end, radius, intensity, random);
                yield true;
            }
            case FLYING_SWORD -> {
                flyingSwordShape(level, family, start, end, radius, intensity, random);
                yield true;
            }
            case FORMATION_BANNER -> {
                formationBannerShape(level, family, start, end, radius, intensity, random);
                yield true;
            }
            case FORMATION_DISC -> {
                formationDiscShape(level, family, start, end, radius, intensity, random);
                yield true;
            }
            case PAGODA_TOWER -> {
                pagodaTowerShape(level, family, start, end, radius, intensity, random);
                yield true;
            }
            case BLOOD_THREAD -> {
                bloodThreadShape(level, family, start, end, radius, intensity, random);
                yield true;
            }
            case JADE_SLIP -> {
                jadeSlipShape(level, family, start, end, radius, intensity, random);
                yield true;
            }
            case MAGIC_SCROLL -> {
                magicScrollShape(level, family, start, end, radius, intensity, random);
                yield true;
            }
            case FIRE_PLUME -> {
                firePlumeShape(level, family, start, end, radius, intensity, random);
                yield true;
            }
            case TALISMAN_BRUSH -> {
                talismanBrushShape(level, family, start, end, radius, intensity, random);
                yield true;
            }
            case SPIRIT_QIN -> {
                spiritQinShape(level, family, start, end, radius, intensity, random);
                yield true;
            }
            case GHOST_HEAD -> {
                ghostHeadShape(level, family, start, end, radius, intensity, random);
                yield true;
            }
            case SHIELD_PLATE -> {
                shieldPlateShape(level, family, start, end, radius, intensity, random);
                yield true;
            }
            case FLYING_BLADE -> {
                flyingBladeShape(level, family, start, end, radius, intensity, random);
                yield true;
            }
            case GIANT_AXE -> {
                giantAxeShape(level, family, start, end, radius, intensity, random);
                yield true;
            }
            case GIANT_HAMMER -> {
                giantHammerShape(level, family, start, end, radius, intensity, random);
                yield true;
            }
            case SPIKED_CLUB -> {
                spikedClubShape(level, family, start, end, radius, intensity, random);
                yield true;
            }
            case COMMAND_TOKEN -> {
                commandTokenShape(level, family, start, end, radius, intensity, random);
                yield true;
            }
            case MAGIC_SCISSORS -> {
                magicScissorsShape(level, family, start, end, radius, intensity, random);
                yield true;
            }
            case MAGIC_BRICK -> {
                magicBrickShape(level, family, start, end, radius, intensity, random);
                yield true;
            }
            case MAGIC_UMBRELLA -> {
                magicUmbrellaShape(level, family, start, end, radius, intensity, random);
                yield true;
            }
            case MAGIC_BOW -> {
                magicBowShape(level, family, start, end, radius, intensity, random);
                yield true;
            }
            case MAGIC_GONG -> {
                magicGongShape(level, family, start, end, radius, intensity, random);
                yield true;
            }
            case MAGIC_MASK -> {
                magicMaskShape(level, family, start, end, radius, intensity, random);
                yield true;
            }
            case MAGIC_CLOTH -> {
                magicClothShape(level, family, start, end, radius, intensity, random);
                yield true;
            }
            case RUNE_PILLAR -> {
                runePillarShape(level, family, start, end, radius, intensity, random);
                yield true;
            }
            case SPIRIT_ARMOR -> {
                spiritArmorShape(level, family, start, end, radius, intensity, random);
                yield true;
            }
            default -> false;
        };
    }

    private static void emitFigureComponents(ClientLevel level, int phaseSalt,
                                             List<Runnable> coreComponents,
                                             List<Runnable> detailComponents) {
        if (!budgetAvailable(level)) {
            return;
        }
        int available = activeEmissionBudget == null ? 8 : activeEmissionBudget.remaining;
        int phase = (int) (level.getGameTime() + phaseSalt);
        for (VfxBudgetPlan.Allocation allocation : VfxBudgetPlan.components(
                available, coreComponents.size(), detailComponents.size(), phase)) {
            Runnable component = allocation.componentIndex() < coreComponents.size()
                    ? coreComponents.get(allocation.componentIndex())
                    : detailComponents.get(allocation.componentIndex() - coreComponents.size());
            withSubBudget(allocation.particles(), component);
        }
    }

    /** 鼎 silhouette: bowl body + three legs + rising vapor. */
    private static void cauldronVesselShape(ClientLevel level, TechniqueVfxPalette.Family family,
                                            Vec3 start, Vec3 end, float radius,
                                            int intensity, Random random) {
        Vec3 center = start.distanceToSqr(end) < 0.04D ? start : end;
        double size = Math.max(0.55D, Math.min(2.4D, radius * 0.7D));
        List<Runnable> details = new ArrayList<>();
        for (int leg = 0; leg < 3; leg++) {
            double angle = Math.PI * 2.0D * leg / 3.0D + Math.PI / 6.0D;
            Vec3 base = center.add(Math.cos(angle) * size * 0.42D, 0.02D,
                    Math.sin(angle) * size * 0.42D);
            details.add(() -> shortLine(level, family, base,
                    base.add(0.0D, size * 0.55D, 0.0D), 4, random));
        }
        details.add(() -> {
            for (int i = 0; i < Math.min(10, Math.max(4, intensity / 5))
                    && budgetAvailable(level); i++) {
                Vec3 vapor = center.add(randomOffset(random, size * 0.28D))
                        .add(0.0D, size * 1.05D, 0.0D);
                spawn(level, LodestoneParticleRegistry.WISP_PARTICLE, family, vapor,
                        new Vec3((random.nextDouble() - 0.5D) * 0.01D,
                                0.014D + random.nextDouble() * 0.012D,
                                (random.nextDouble() - 0.5D) * 0.01D),
                        0.18F, 0.62F, 28, random.nextFloat());
            }
        });
        details.add(() -> ring(level, family, center.add(0.0D, size * 0.95D, 0.0D),
                size * 0.68D, Math.min(12, Math.max(6, intensity / 5)),
                random, 0.11F, 16));
        emitFigureComponents(level, 53, List.of(
                () -> ring(level, family, center.add(0.0D, size * 0.55D, 0.0D),
                        size * 0.55D, Math.min(18, Math.max(8, intensity / 3)),
                        random, 0.16F, 22),
                () -> ring(level, family, center.add(0.0D, size * 0.95D, 0.0D),
                        size * 0.62D, Math.min(16, Math.max(8, intensity / 3)),
                        random, 0.14F, 20)), details);
    }

    /** 钟 silhouette: dome body + hanging rim + outward chime rings. */
    private static void bellChimeShape(ClientLevel level, TechniqueVfxPalette.Family family,
                                       Vec3 start, Vec3 end, float radius,
                                       int intensity, Random random) {
        Vec3 center = start.distanceToSqr(end) < 0.04D ? start : end;
        double size = Math.max(0.45D, Math.min(2.0D, radius * 0.55D));
        Vec3 apex = center.add(0.0D, size * 1.4D, 0.0D);
        emitFigureComponents(level, 59, List.of(
                () -> ring(level, family, center.add(0.0D, size * 0.55D, 0.0D),
                        size * 0.55D, Math.min(16, Math.max(8, intensity / 3)),
                        random, 0.15F, 20),
                () -> ring(level, family, center.add(0.0D, size * 0.15D, 0.0D),
                        size * 0.72D, Math.min(18, Math.max(8, intensity / 3)),
                        random, 0.13F, 22)),
                List.of(
                        () -> ring(level, family, apex.subtract(0.0D, size * 0.15D, 0.0D),
                                size * 0.22D, Math.min(12, Math.max(6, intensity / 4)),
                                random, 0.14F, 18),
                        () -> shortLine(level, family, apex,
                                center.add(0.0D, size * 0.15D, 0.0D), 5, random),
                        () -> rotatingRing(level, family,
                                center.add(0.0D, size * 0.35D, 0.0D), size * 1.15D,
                                Math.min(14, Math.max(6, intensity / 4)),
                                level.getGameTime() * 0.12D, random)));
    }

    /** 葫芦 silhouette: dual bulb body + neck spout + rising mist. */
    private static void gourdVesselShape(ClientLevel level, TechniqueVfxPalette.Family family,
                                         Vec3 start, Vec3 end, float radius,
                                         int intensity, Random random) {
        Vec3 center = start.distanceToSqr(end) < 0.04D ? start : end;
        double size = Math.max(0.4D, Math.min(1.8D, radius * 0.5D));
        emitFigureComponents(level, 61, List.of(
                () -> ring(level, family, center.add(0.0D, size * 0.35D, 0.0D),
                        size * 0.48D, Math.min(14, Math.max(6, intensity / 4)),
                        random, 0.15F, 20),
                () -> ring(level, family, center.add(0.0D, size * 0.95D, 0.0D),
                        size * 0.32D, Math.min(12, Math.max(6, intensity / 4)),
                        random, 0.14F, 18)),
                List.of(
                        () -> shortLine(level, family,
                                center.add(0.0D, size * 1.15D, 0.0D),
                                center.add(0.0D, size * 1.55D, 0.0D), 4, random),
                        () -> {
                            for (int i = 0; i < Math.min(5, Math.max(2, intensity / 8))
                                    && budgetAvailable(level); i++) {
                                Vec3 mist = center.add(randomOffset(random, size * 0.2D))
                                        .add(0.0D, size * 1.55D, 0.0D);
                                spawn(level, LodestoneParticleRegistry.WISP_PARTICLE, family, mist,
                                        new Vec3((random.nextDouble() - 0.5D) * 0.01D, 0.015D,
                                                (random.nextDouble() - 0.5D) * 0.01D),
                                        0.16F, 0.58F, 26, random.nextFloat());
                            }
                        }));
    }

    /** 光幕 silhouette: vertical planar curtain of bars + rim. */
    private static void lightCurtainShape(ClientLevel level, TechniqueVfxPalette.Family family,
                                          Vec3 start, Vec3 end, float radius,
                                          int intensity, Random random) {
        Vec3 center = start.distanceToSqr(end) < 0.04D ? start : end;
        Vec3 direction = normalized(end.subtract(start), new Vec3(0.0D, 0.0D, 1.0D));
        Vec3 side = perpendicular(direction);
        double width = Math.max(1.0D, Math.min(4.5D, radius * 1.4D));
        double height = Math.max(1.4D, Math.min(3.6D, radius * 1.1D));
        int bars = Math.min(16, Math.max(8, intensity / 3));
        List<Runnable> details = new ArrayList<>();
        for (int i = 0; i < bars; i++) {
            double t = (i / (double) Math.max(1, bars - 1)) * 2.0D - 1.0D;
            Vec3 base = center.add(side.scale(t * width * 0.5D));
            details.add(() -> shortLine(level, family, base,
                    base.add(0.0D, height, 0.0D), 6, random));
        }
        details.add(() -> ring(level, family, center.add(0.0D, height * 0.5D, 0.0D),
                width * 0.55D, Math.min(18, Math.max(10, intensity / 3)),
                random, 0.12F, 18));
        emitFigureComponents(level, 67, List.of(
                () -> shortLine(level, family,
                        center.add(side.scale(-width * 0.5D)).add(0.0D, height, 0.0D),
                        center.add(side.scale(width * 0.5D)).add(0.0D, height, 0.0D), 6, random),
                () -> shortLine(level, family, center.add(side.scale(-width * 0.5D)),
                        center.add(side.scale(width * 0.5D)), 6, random)), details);
    }

    /** 光环 silhouette: single controlled annular disc (not multi-ring burst). */
    private static void haloRingShape(ClientLevel level, TechniqueVfxPalette.Family family,
                                      Vec3 start, Vec3 end, float radius,
                                      int intensity, Random random) {
        Vec3 center = start.distanceToSqr(end) < 0.04D ? start : end;
        double size = Math.max(0.9D, Math.min(4.5D, radius * 1.35D));
        double y = 0.4D;
        List<Runnable> details = new ArrayList<>();
        details.add(() -> ring(level, family, center.add(0.0D, y, 0.0D), size * 1.05D,
                Math.min(20, Math.max(10, intensity / 3)), random, 0.12F, 22));
        details.add(() -> ring(level, family, center.add(0.0D, y, 0.0D), size * 1.18D,
                Math.min(14, Math.max(8, intensity / 4)), random, 0.1F, 18));
        for (int i = 0; i < 6; i++) {
            double angle = Math.PI * 2.0D * i / 6.0D;
            Vec3 from = center.add(Math.cos(angle) * size, y, Math.sin(angle) * size);
            details.add(() -> shortLine(level, family, from,
                    from.add(0.0D, -0.55D, 0.0D), 3, random));
        }
        emitFigureComponents(level, 71, List.of(
                () -> rotatingRing(level, family, center.add(0.0D, y, 0.0D), size,
                        Math.min(32, Math.max(16, intensity / 2)),
                        level.getGameTime() * 0.1D, random),
                () -> ring(level, family, center.add(0.0D, y, 0.0D), size * 0.88D,
                        Math.min(24, Math.max(12, intensity / 2)),
                        random, 0.15F, 26)), details);
    }

    /** 幡 silhouette: pole + hanging banner plane + streamer tips. */
    private static void bannerStreamerShape(ClientLevel level, TechniqueVfxPalette.Family family,
                                            Vec3 start, Vec3 end, float radius,
                                            int intensity, Random random) {
        Vec3 center = start.distanceToSqr(end) < 0.04D ? start : end;
        Vec3 direction = normalized(end.subtract(start), new Vec3(0.0D, 0.0D, 1.0D));
        Vec3 side = perpendicular(direction);
        double height = Math.max(1.2D, Math.min(3.5D, radius * 1.3D));
        double width = Math.max(0.4D, Math.min(1.6D, radius * 0.55D));
        Vec3 base = center;
        Vec3 top = center.add(0.0D, height, 0.0D);
        emitFigureComponents(level, 73, List.of(
                () -> shortLine(level, family, base, top,
                        Math.min(10, Math.max(5, intensity / 4)), random),
                () -> shortLine(level, family, top, top.add(side.scale(width)), 5, random)),
                List.of(
                        () -> shortLine(level, family, top.add(side.scale(width * 0.15D)),
                                top.add(side.scale(width)).add(0.0D, -height * 0.55D, 0.0D), 6, random),
                        () -> shortLine(level, family, top.add(side.scale(width * 0.55D)),
                                top.add(side.scale(width * 1.1D)).add(0.0D, -height * 0.7D, 0.0D), 5, random),
                        () -> {
                            for (int i = 0; i < Math.min(4, Math.max(2, intensity / 10))
                                    && budgetAvailable(level); i++) {
                                Vec3 tip = top.add(side.scale(width * (0.4D + i * 0.2D)))
                                        .add(0.0D, -height * (0.4D + i * 0.12D), 0.0D);
                                spawn(level, LodestoneParticleRegistry.WISP_PARTICLE, family, tip,
                                        side.scale(0.008D).add(0.0D, -0.006D, 0.0D),
                                        0.14F, 0.66F, 22, random.nextFloat());
                            }
                        }));
    }

    /** 印 silhouette: square stamp face + downward press ray. */
    private static void sealStampShape(ClientLevel level, TechniqueVfxPalette.Family family,
                                       Vec3 start, Vec3 end, float radius,
                                       int intensity, Random random) {
        Vec3 center = start.distanceToSqr(end) < 0.04D ? start : end;
        double size = Math.max(0.35D, Math.min(1.6D, radius * 0.45D));
        Vec3 face = center.add(0.0D, size * 1.2D, 0.0D);
        // Square face as four edges.
        Vec3 a = face.add(-size, 0.0D, -size);
        Vec3 b = face.add(size, 0.0D, -size);
        Vec3 c = face.add(size, 0.0D, size);
        Vec3 d = face.add(-size, 0.0D, size);
        emitFigureComponents(level, 79, List.of(
                () -> ring(level, family, face, size * 1.25D, 4, random, 0.15F, 20),
                () -> shortLine(level, family, face, center,
                        Math.min(8, Math.max(4, intensity / 5)), random)),
                List.of(
                        () -> shortLine(level, family, a, b, 4, random),
                        () -> shortLine(level, family, b, c, 4, random),
                        () -> shortLine(level, family, c, d, 4, random),
                        () -> shortLine(level, family, d, a, 4, random),
                        () -> ring(level, family, center.add(0.0D, 0.05D, 0.0D),
                                size * 0.85D, Math.min(14, Math.max(6, intensity / 4)),
                                random, 0.13F, 16)));
    }

    /** 虹桥 silhouette: arched path of particles from start to end. */
    private static void bridgeArcShape(ClientLevel level, TechniqueVfxPalette.Family family,
                                       Vec3 start, Vec3 end, float radius,
                                       int intensity, Random random) {
        Vec3 from = start;
        Vec3 to = start.distanceToSqr(end) < 0.04D ? start.add(2.5D, 0.0D, 0.0D) : end;
        Vec3 delta = to.subtract(from);
        double rise = Math.max(0.8D, Math.min(3.0D, radius * 0.9D + delta.length() * 0.18D));
        int points = Math.min(18, Math.max(8, intensity / 3));
        int samples = budgetedSamples(points + 1);
        for (int sample = 0; sample < samples && budgetAvailable(level); sample++) {
            double t = lineSampleProgress(sample, samples);
            double arch = Math.sin(Math.PI * t) * rise;
            Vec3 point = from.add(delta.scale(t)).add(0.0D, arch, 0.0D);
            spawn(level, sample % 3 == 0 ? LodestoneParticleRegistry.TWINKLE_PARTICLE
                            : LodestoneParticleRegistry.SPARKLE_PARTICLE,
                    family, point, new Vec3(0.0D, 0.004D, 0.0D), 0.14F, 0.78F, 22, (float) t);
        }
    }


    /** 飞剑 silhouette: slender blade streak with tip spark and trailing afterimage. */
    private static void flyingSwordShape(ClientLevel level, TechniqueVfxPalette.Family family,
                                         Vec3 start, Vec3 end, float radius,
                                         int intensity, Random random) {
        Vec3 direction = normalized(end.subtract(start), new Vec3(0.0D, 0.0D, 1.0D));
        Vec3 tip = start.distanceToSqr(end) < 0.04D ? start.add(direction.scale(1.6D + radius * 0.4D)) : end;
        Vec3 side = perpendicular(direction);
        emitFigureComponents(level, 83, List.of(
                () -> shortLine(level, family, start, tip,
                        Math.min(18, Math.max(8, intensity / 2)), random),
                () -> shortLine(level, family,
                        tip.subtract(direction.scale(0.35D)).add(side.scale(0.12D)), tip, 3, random),
                () -> shortLine(level, family,
                        tip.subtract(direction.scale(0.35D)).subtract(side.scale(0.12D)), tip, 3, random)),
                List.of(
                        () -> shortLine(level, family, start.add(side.scale(0.04D)),
                                tip.add(side.scale(0.02D)), 6, random),
                        () -> spawn(level, LodestoneParticleRegistry.STAR_PARTICLE, family, tip,
                                direction.scale(0.02D), 0.16F, 0.88F, 16, random.nextFloat()),
                        () -> {
                            for (int i = 0; i < 3 && budgetAvailable(level); i++) {
                                double t = 0.25D + i * 0.22D;
                                Vec3 point = start.lerp(tip, t).add(randomOffset(random, 0.05D));
                                spawn(level, LodestoneParticleRegistry.SPARKLE_PARTICLE, family, point,
                                        direction.scale(-0.008D), 0.10F, 0.55F, 12, random.nextFloat());
                            }
                        }));
    }

    /** 阵旗 silhouette: pole + rectangular flag plane + flutter tips. */
    private static void formationBannerShape(ClientLevel level, TechniqueVfxPalette.Family family,
                                             Vec3 start, Vec3 end, float radius,
                                             int intensity, Random random) {
        Vec3 center = start.distanceToSqr(end) < 0.04D ? start : end;
        Vec3 direction = normalized(end.subtract(start), new Vec3(0.0D, 0.0D, 1.0D));
        Vec3 side = perpendicular(direction);
        double height = Math.max(1.4D, Math.min(3.8D, radius * 1.4D));
        double width = Math.max(0.5D, Math.min(1.8D, radius * 0.6D));
        Vec3 top = center.add(0.0D, height, 0.0D);
        emitFigureComponents(level, 89, List.of(
                () -> shortLine(level, family, center, top,
                        Math.min(12, Math.max(6, intensity / 4)), random),
                () -> shortLine(level, family, top, top.add(side.scale(width)), 5, random)),
                List.of(
                        () -> shortLine(level, family, top.add(side.scale(width * 0.1D)),
                                top.add(side.scale(width)).add(0.0D, -height * 0.45D, 0.0D), 6, random),
                        () -> shortLine(level, family, top.add(side.scale(width * 0.55D)),
                                top.add(side.scale(width * 1.05D)).add(0.0D, -height * 0.65D, 0.0D),
                                5, random),
                        () -> ring(level, family, center.add(0.0D, 0.05D, 0.0D),
                                0.18D, 6, random, 0.12F, 14)));
    }

    /** 宝塔 silhouette: stacked square tiers tapering upward. */
    private static void pagodaTowerShape(ClientLevel level, TechniqueVfxPalette.Family family,
                                         Vec3 start, Vec3 end, float radius,
                                         int intensity, Random random) {
        Vec3 center = start.distanceToSqr(end) < 0.04D ? start : end;
        double base = Math.max(0.5D, Math.min(2.2D, radius * 0.65D));
        int tiers = Math.min(5, Math.max(3, intensity / 10));
        List<Runnable> tierComponents = new ArrayList<>();
        for (int t = 0; t < tiers; t++) {
            double progress = t / (double) Math.max(1, tiers - 1);
            double size = base * (1.0D - progress * 0.55D);
            double y = 0.15D + t * (base * 0.55D);
            Vec3 face = center.add(0.0D, y, 0.0D);
            tierComponents.add(() -> ring(level, family, face, size,
                    8, random, 0.13F, 20));
        }
        emitFigureComponents(level, 97,
                List.of(() -> shortLine(level, family, center,
                        center.add(0.0D, base * tiers * 0.55D + 0.4D, 0.0D), 5, random)),
                tierComponents);
    }

    /** 血丝 silhouette: thin converging threads toward target. */
    private static void bloodThreadShape(ClientLevel level, TechniqueVfxPalette.Family family,
                                         Vec3 start, Vec3 end, float radius,
                                         int intensity, Random random) {
        Vec3 tip = start.distanceToSqr(end) < 0.04D ? start.add(0.0D, 0.2D, 1.2D) : end;
        int threads = Math.min(10, Math.max(4, intensity / 4));
        List<Runnable> threadComponents = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            Vec3 origin = start.add(randomOffset(random, Math.max(0.3D, radius * 0.45D)));
            threadComponents.add(() -> shortLine(level, family, origin, tip, 4, random));
        }
        emitFigureComponents(level, 101,
                List.of(threadComponents.remove(0)), threadComponents);
    }

    /** 玉简 silhouette: flat rectangular slip with edge glow. */
    private static void jadeSlipShape(ClientLevel level, TechniqueVfxPalette.Family family,
                                      Vec3 start, Vec3 end, float radius,
                                      int intensity, Random random) {
        Vec3 center = start.distanceToSqr(end) < 0.04D ? start.add(0.0D, 1.0D, 0.0D)
                : start.lerp(end, 0.5D).add(0.0D, 0.6D, 0.0D);
        Vec3 direction = normalized(end.subtract(start), new Vec3(0.0D, 0.0D, 1.0D));
        Vec3 side = perpendicular(direction);
        Vec3 up = normalized(side.cross(direction), new Vec3(0.0D, 1.0D, 0.0D));
        double w = Math.max(0.18D, Math.min(0.55D, radius * 0.22D));
        double h = Math.max(0.35D, Math.min(1.0D, radius * 0.4D));
        Vec3 a = center.add(side.scale(-w)).add(up.scale(-h));
        Vec3 b = center.add(side.scale(w)).add(up.scale(-h));
        Vec3 c = center.add(side.scale(w)).add(up.scale(h));
        Vec3 d = center.add(side.scale(-w)).add(up.scale(h));
        emitFigureComponents(level, 103, List.of(
                () -> shortLine(level, family, b, c, 4, random),
                () -> shortLine(level, family, d, a, 4, random)),
                List.of(
                        () -> shortLine(level, family, a, b, 3, random),
                        () -> shortLine(level, family, c, d, 3, random),
                        () -> shortLine(level, family, a.lerp(b, 0.5D),
                                d.lerp(c, 0.5D), 3, random),
                        () -> spawn(level, LodestoneParticleRegistry.TWINKLE_PARTICLE, family, center,
                                Vec3.ZERO, 0.14F, 0.8F, 18, random.nextFloat())));
    }


    /** 火焰 silhouette: rising plume column with ember scatter. */
    private static void firePlumeShape(ClientLevel level, TechniqueVfxPalette.Family family,
                                       Vec3 start, Vec3 end, float radius,
                                       int intensity, Random random) {
        Vec3 center = start.distanceToSqr(end) < 0.04D ? start : end;
        double size = Math.max(0.6D, Math.min(2.8D, radius * 0.85D));
        int tongues = Math.min(14, Math.max(7, intensity / 3));
        List<Runnable> tongueComponents = new ArrayList<>();
        for (int i = 0; i < tongues; i++) {
            double angle = Math.PI * 2.0D * i / tongues + random.nextDouble() * 0.2D;
            double radial = size * (0.08D + (i % 3) * 0.06D);
            Vec3 base = center.add(Math.cos(angle) * radial, 0.05D,
                    Math.sin(angle) * radial);
            Vec3 tip = base.add((random.nextDouble() - 0.5D) * 0.3D,
                    size * (0.85D + random.nextDouble() * 0.65D),
                    (random.nextDouble() - 0.5D) * 0.3D);
            tongueComponents.add(() -> shortLine(level, family, base, tip, 6, random));
        }
        List<Runnable> coreTongues = List.copyOf(tongueComponents.subList(0, 3));
        List<Runnable> details = new ArrayList<>(tongueComponents.subList(3, tongueComponents.size()));
        details.add(() -> ring(level, family, center.add(0.0D, 0.08D, 0.0D),
                size * 0.35D, Math.min(14, Math.max(6, intensity / 4)),
                random, 0.15F, 16));
        details.add(() -> {
            for (int i = 0; i < Math.min(8, Math.max(3, intensity / 6))
                    && budgetAvailable(level); i++) {
                Vec3 ember = center.add(randomOffset(random, size * 0.4D))
                        .add(0.0D, size * (0.3D + random.nextDouble() * 0.8D), 0.0D);
                spawn(level, LodestoneParticleRegistry.WISP_PARTICLE, family, ember,
                        new Vec3((random.nextDouble() - 0.5D) * 0.02D,
                                0.02D + random.nextDouble() * 0.02D,
                                (random.nextDouble() - 0.5D) * 0.02D),
                        0.18F, 0.72F, 22, random.nextFloat());
            }
        });
        emitFigureComponents(level, 107, coreTongues, details);
    }

    /** 鬼首 silhouette: four-point skull, paired eyes, and jaw. */
    private static void ghostHeadShape(ClientLevel level, TechniqueVfxPalette.Family family,
                                       Vec3 start, Vec3 end, float radius,
                                       int intensity, Random random) {
        Vec3 direction = normalized(end.subtract(start), new Vec3(0.0D, 0.0D, 1.0D));
        Vec3 side = perpendicular(direction);
        Vec3 up = normalized(side.cross(direction), new Vec3(0.0D, 1.0D, 0.0D));
        double size = Math.max(0.45D, Math.min(1.8D, radius * 0.62D));
        Vec3 center = (start.distanceToSqr(end) < 0.04D ? start : start.lerp(end, 0.55D))
                .add(up.scale(size * 0.8D));
        emitFigureComponents(level, 109, List.of(
                () -> verticalRing(level, family, center, direction,
                        size * 0.55D, 4, random, 0.14F),
                () -> shortLine(level, family,
                        center.subtract(side.scale(size * 0.22D)).subtract(up.scale(size * 0.28D)),
                        center.add(side.scale(size * 0.22D)).subtract(up.scale(size * 0.28D)),
                        1, random)),
                List.of(
                        () -> spawn(level, LodestoneParticleRegistry.STAR_PARTICLE, family,
                                center.add(side.scale(size * 0.2D)).add(up.scale(size * 0.1D)),
                                Vec3.ZERO, 0.13F, 0.9F, 18, random.nextFloat()),
                        () -> spawn(level, LodestoneParticleRegistry.STAR_PARTICLE, family,
                                center.subtract(side.scale(size * 0.2D)).add(up.scale(size * 0.1D)),
                                Vec3.ZERO, 0.13F, 0.9F, 18, random.nextFloat())));
    }

    /** 盾牌 silhouette: frontal rim, central boss, and reinforcing bar. */
    private static void shieldPlateShape(ClientLevel level, TechniqueVfxPalette.Family family,
                                         Vec3 start, Vec3 end, float radius,
                                         int intensity, Random random) {
        Vec3 direction = normalized(end.subtract(start), new Vec3(0.0D, 0.0D, 1.0D));
        Vec3 side = perpendicular(direction);
        Vec3 up = normalized(side.cross(direction), new Vec3(0.0D, 1.0D, 0.0D));
        double size = Math.max(0.55D, Math.min(2.2D, radius * 0.72D));
        Vec3 center = (start.distanceToSqr(end) < 0.04D ? start : end).add(up.scale(size * 0.75D));
        emitFigureComponents(level, 113, List.of(
                () -> verticalRing(level, family, center, direction,
                        size * 0.62D, 4, random, 0.15F),
                () -> shortLine(level, family, center.subtract(up.scale(size * 0.42D)),
                        center.add(up.scale(size * 0.42D)), 1, random)),
                List.of(
                        () -> spawn(level, LodestoneParticleRegistry.TWINKLE_PARTICLE, family, center,
                                Vec3.ZERO, 0.18F, 0.92F, 20, random.nextFloat()),
                        () -> spawn(level, LodestoneParticleRegistry.SPARKLE_PARTICLE, family,
                                center.subtract(up.scale(size * 0.58D)), Vec3.ZERO,
                                0.11F, 0.72F, 16, random.nextFloat())));
    }

    /** 飞刀 silhouette: broad triangular blade with a luminous point. */
    private static void flyingBladeShape(ClientLevel level, TechniqueVfxPalette.Family family,
                                         Vec3 start, Vec3 end, float radius,
                                         int intensity, Random random) {
        Vec3 direction = normalized(end.subtract(start), new Vec3(0.0D, 0.0D, 1.0D));
        Vec3 side = perpendicular(direction);
        double length = Math.max(1.0D, Math.min(3.5D, radius * 1.15D));
        double width = Math.max(0.18D, Math.min(0.65D, radius * 0.24D));
        Vec3 heel = start;
        Vec3 tip = start.distanceToSqr(end) < 0.04D ? start.add(direction.scale(length)) : end;
        emitFigureComponents(level, 127, List.of(
                () -> shortLine(level, family, heel, tip, 2, random),
                () -> shortLine(level, family, heel.add(side.scale(width)), tip, 1, random),
                () -> shortLine(level, family, heel.subtract(side.scale(width)), tip, 1, random)),
                List.of(() -> spawn(level, LodestoneParticleRegistry.STAR_PARTICLE, family, tip,
                        direction.scale(0.02D), 0.16F, 0.9F, 18, random.nextFloat())));
    }

    /** 巨斧 silhouette: long haft and a two-edge axe head. */
    private static void giantAxeShape(ClientLevel level, TechniqueVfxPalette.Family family,
                                      Vec3 start, Vec3 end, float radius,
                                      int intensity, Random random) {
        Vec3 direction = normalized(end.subtract(start), new Vec3(0.0D, 1.0D, 0.0D));
        Vec3 side = perpendicular(direction);
        double size = Math.max(0.8D, Math.min(3.0D, radius));
        Vec3 center = start.distanceToSqr(end) < 0.04D ? start.add(0.0D, size * 0.65D, 0.0D)
                : start.lerp(end, 0.5D);
        Vec3 butt = center.subtract(direction.scale(size * 0.75D));
        Vec3 head = center.add(direction.scale(size * 0.55D));
        Vec3 bladeTip = head.add(side.scale(size * 0.62D));
        emitFigureComponents(level, 131, List.of(
                () -> shortLine(level, family, butt, head, 2, random),
                () -> shortLine(level, family,
                        head.add(direction.scale(size * 0.2D)), bladeTip, 1, random),
                () -> shortLine(level, family,
                        head.subtract(direction.scale(size * 0.2D)), bladeTip, 1, random)),
                List.of(() -> spawn(level, LodestoneParticleRegistry.TWINKLE_PARTICLE, family, bladeTip,
                        side.scale(0.015D), 0.17F, 0.9F, 20, random.nextFloat())));
    }

    /** Ritual bowl silhouette: open rim, suspended basin and rotating inner contents. */
    private static void ritualBowlShape(ClientLevel level, TechniqueVfxPalette.Family family,
                                        Vec3 start, Vec3 end, float radius,
                                        int intensity, Random random) {
        Vec3 center = start.distanceToSqr(end) < 0.04D ? start : end;
        Vec3 direction = normalized(end.subtract(start), new Vec3(0.0D, 0.0D, 1.0D));
        Vec3 side = perpendicular(direction);
        double size = Math.max(0.55D, Math.min(3.2D, radius * 0.72D));
        Vec3 rim = center.add(0.0D, size * 0.68D, 0.0D);
        Vec3 left = rim.subtract(side.scale(size * 0.68D));
        Vec3 right = rim.add(side.scale(size * 0.68D));
        Vec3 bottom = center.add(0.0D, size * 0.06D, 0.0D);
        emitFigureComponents(level, 137, List.of(
                () -> ring(level, family, rim, size * 0.68D,
                        Math.min(18, Math.max(8, intensity / 3)), random, 0.14F, 24),
                () -> shortLine(level, family, left, bottom, 4, random),
                () -> shortLine(level, family, bottom, right, 4, random)),
                List.of(
                        () -> rotatingRing(level, family, rim.add(0.0D, 0.03D, 0.0D),
                                size * 0.43D, Math.min(12, Math.max(6, intensity / 4)),
                                level.getGameTime() * 0.11D, random),
                        () -> shortLine(level, family,
                                bottom.subtract(side.scale(size * 0.24D)),
                                bottom.add(side.scale(size * 0.24D)), 3, random),
                        () -> shortLine(level, family, rim.add(0.0D, size * 0.08D, 0.0D),
                                rim.add(0.0D, size * 0.82D, 0.0D), 4, random),
                        () -> spawn(level, LodestoneParticleRegistry.STAR_PARTICLE, family,
                                rim.add(0.0D, size * 0.32D, 0.0D),
                                new Vec3(0.0D, 0.012D, 0.0D), 0.17F, 0.86F, 24,
                                random.nextFloat())));
    }

    /** Magic ruler silhouette: parallel silver edges, end caps and measured tick marks. */
    private static void magicRulerShape(ClientLevel level, TechniqueVfxPalette.Family family,
                                        Vec3 start, Vec3 end, float radius,
                                        int intensity, Random random) {
        Vec3 direction = normalized(end.subtract(start), new Vec3(0.0D, 1.0D, 0.0D));
        Vec3 side = perpendicular(direction);
        double size = Math.max(0.9D, Math.min(4.0D, radius * 1.15D));
        Vec3 center = start.distanceToSqr(end) < 0.04D
                ? start.add(0.0D, size * 0.8D, 0.0D)
                : start.lerp(end, 0.5D);
        Vec3 butt = center.subtract(direction.scale(size * 0.78D));
        Vec3 tip = center.add(direction.scale(size * 0.78D));
        double halfWidth = size * 0.14D;
        Vec3 left = side.scale(halfWidth);
        List<Runnable> details = new ArrayList<>();
        details.add(() -> shortLine(level, family, butt.subtract(left), butt.add(left), 3, random));
        details.add(() -> shortLine(level, family, tip.subtract(left), tip.add(left), 3, random));
        details.add(() -> shortLine(level, family, butt, tip, 5, random));
        for (int tick = 1; tick <= 5; tick++) {
            Vec3 mark = butt.lerp(tip, tick / 6.0D);
            double width = halfWidth * (tick == 3 ? 1.0D : 0.68D);
            details.add(() -> shortLine(level, family,
                    mark.subtract(side.scale(width)), mark.add(side.scale(width)), 2, random));
        }
        details.add(() -> spawn(level, LodestoneParticleRegistry.TWINKLE_PARTICLE, family, tip,
                direction.scale(0.018D), 0.16F, 0.9F, 20, random.nextFloat()));
        emitFigureComponents(level, 139, List.of(
                () -> shortLine(level, family, butt.subtract(left), tip.subtract(left), 7, random),
                () -> shortLine(level, family, butt.add(left), tip.add(left), 7, random)), details);
    }

    /** Giant hammer silhouette: long haft, broad head and eight rotating skull-flame studs. */
    private static void giantHammerShape(ClientLevel level, TechniqueVfxPalette.Family family,
                                         Vec3 start, Vec3 end, float radius,
                                         int intensity, Random random) {
        Vec3 direction = normalized(end.subtract(start), new Vec3(0.0D, 1.0D, 0.0D));
        Vec3 side = perpendicular(direction);
        double size = Math.max(0.9D, Math.min(3.4D, radius * 1.05D));
        Vec3 center = start.distanceToSqr(end) < 0.04D
                ? start.add(0.0D, size * 0.72D, 0.0D)
                : start.lerp(end, 0.5D);
        Vec3 butt = center.subtract(direction.scale(size * 0.86D));
        Vec3 headCenter = center.add(direction.scale(size * 0.48D));
        Vec3 headLeft = headCenter.subtract(side.scale(size * 0.68D));
        Vec3 headRight = headCenter.add(side.scale(size * 0.68D));
        List<Runnable> details = new ArrayList<>();
        details.add(() -> shortLine(level, family,
                headLeft.subtract(direction.scale(size * 0.17D)),
                headRight.subtract(direction.scale(size * 0.17D)), 5, random));
        details.add(() -> shortLine(level, family,
                headLeft.add(direction.scale(size * 0.17D)),
                headRight.add(direction.scale(size * 0.17D)), 5, random));
        for (int skull = 0; skull < 8; skull++) {
            double across = -0.54D + (skull % 4) * 0.36D;
            double along = skull < 4 ? -0.14D : 0.14D;
            Vec3 stud = headCenter.add(side.scale(size * across))
                    .add(direction.scale(size * along));
            details.add(() -> spawn(level, LodestoneParticleRegistry.WISP_PARTICLE, family, stud,
                    new Vec3(0.0D, 0.018D, 0.0D), 0.16F, 0.86F, 24,
                    random.nextFloat()));
        }
        emitFigureComponents(level, 149, List.of(
                () -> shortLine(level, family, butt, headCenter, 7, random),
                () -> shortLine(level, family, headLeft, headRight, 7, random)), details);
    }

    /** Staff silhouette: full-length shaft, crown bar, collars and a restrained halo. */
    private static void magicStaffShape(ClientLevel level, TechniqueVfxPalette.Family family,
                                        Vec3 start, Vec3 end, float radius,
                                        int intensity, Random random) {
        Vec3 direction = normalized(end.subtract(start), new Vec3(0.0D, 1.0D, 0.0D));
        Vec3 side = perpendicular(direction);
        double size = Math.max(1.0D, Math.min(4.2D, radius * 1.18D));
        Vec3 center = start.distanceToSqr(end) < 0.04D
                ? start.add(0.0D, size * 0.92D, 0.0D)
                : start.lerp(end, 0.5D);
        Vec3 butt = center.subtract(direction.scale(size * 0.92D));
        Vec3 crown = center.add(direction.scale(size * 0.92D));
        Vec3 crownLeft = crown.subtract(side.scale(size * 0.34D));
        Vec3 crownRight = crown.add(side.scale(size * 0.34D));
        List<Runnable> details = new ArrayList<>();
        details.add(() -> shortLine(level, family, crownLeft, crownRight, 5, random));
        details.add(() -> verticalRing(level, family,
                crown.subtract(direction.scale(size * 0.12D)), direction,
                size * 0.2D, Math.min(12, Math.max(6, intensity / 4)), random, 0.14F));
        details.add(() -> shortLine(level, family,
                crownLeft, crownLeft.add(direction.scale(size * 0.28D)), 3, random));
        details.add(() -> shortLine(level, family,
                crownRight, crownRight.add(direction.scale(size * 0.28D)), 3, random));
        details.add(() -> rotatingRing(level, family, center, size * 0.38D,
                Math.min(14, Math.max(7, intensity / 4)),
                level.getGameTime() * 0.09D, random));
        details.add(() -> spawn(level, LodestoneParticleRegistry.STAR_PARTICLE, family,
                crown.add(direction.scale(size * 0.28D)), direction.scale(0.012D),
                0.18F, 0.9F, 24, random.nextFloat()));
        emitFigureComponents(level, 151,
                List.of(() -> shortLine(level, family, butt, crown, 9, random)), details);
    }

    /** Ancient lamp silhouette: pedestal, suspended body, handle and living flame. */
    private static void ritualLampShape(ClientLevel level, TechniqueVfxPalette.Family family,
                                        Vec3 start, Vec3 end, float radius,
                                        int intensity, Random random) {
        Vec3 center = start.distanceToSqr(end) < 0.04D ? start : end;
        Vec3 direction = normalized(end.subtract(start), new Vec3(0.0D, 0.0D, 1.0D));
        Vec3 side = perpendicular(direction);
        double size = Math.max(0.45D, Math.min(1.7D, radius * 0.52D));
        Vec3 base = center.add(0.0D, size * 0.12D, 0.0D);
        Vec3 bowl = center.add(0.0D, size * 0.58D, 0.0D);
        Vec3 flame = center.add(0.0D, size * 1.45D, 0.0D);
        List<Runnable> details = new ArrayList<>();
        details.add(() -> ring(level, family, base, size * 0.34D,
                Math.min(12, Math.max(6, intensity / 4)), random, 0.13F, 20));
        details.add(() -> verticalRing(level, family, bowl, direction, size * 0.46D,
                Math.min(14, Math.max(7, intensity / 4)), random, 0.15F));
        details.add(() -> shortLine(level, family,
                bowl.subtract(side.scale(size * 0.44D)),
                bowl.add(side.scale(size * 0.44D)), 4, random));
        details.add(() -> verticalRing(level, family,
                bowl.add(0.0D, size * 0.42D, 0.0D), direction,
                size * 0.42D, Math.min(12, Math.max(6, intensity / 5)), random, 0.11F));
        details.add(() -> spawn(level, LodestoneParticleRegistry.WISP_PARTICLE, family, flame,
                new Vec3(0.0D, 0.018D, 0.0D), 0.19F, 0.86F, 26,
                random.nextFloat()));
        emitFigureComponents(level, 157,
                List.of(() -> shortLine(level, family, base, flame, 7, random)), details);
    }

    /** White spirit qin silhouette: long soundboard, strings and expanding sound fronts. */
    private static void spiritQinShape(ClientLevel level, TechniqueVfxPalette.Family family,
                                       Vec3 start, Vec3 end, float radius,
                                       int intensity, Random random) {
        Vec3 direction = normalized(end.subtract(start), new Vec3(0.0D, 0.0D, 1.0D));
        Vec3 side = perpendicular(direction);
        Vec3 center = (start.distanceToSqr(end) < 0.04D ? start : start.lerp(end, 0.42D))
                .add(0.0D, 0.72D, 0.0D);
        double size = Math.max(0.9D, Math.min(3.2D, radius * 0.78D));
        double depth = size * 0.24D;
        Vec3 left = center.subtract(side.scale(size * 0.78D));
        Vec3 right = center.add(side.scale(size * 0.78D));
        List<Runnable> details = new ArrayList<>();
        details.add(() -> shortLine(level, family,
                left.add(direction.scale(depth)), right.add(direction.scale(depth)), 7, random));
        details.add(() -> shortLine(level, family,
                left.subtract(direction.scale(depth)), left.add(direction.scale(depth)), 3, random));
        details.add(() -> shortLine(level, family,
                right.subtract(direction.scale(depth)), right.add(direction.scale(depth)), 3, random));
        for (int string = -1; string <= 1; string++) {
            double offset = depth * string * 0.48D;
            details.add(() -> shortLine(level, family,
                    left.add(direction.scale(offset)), right.add(direction.scale(offset)), 6, random));
        }
        for (int wave = 1; wave <= 3; wave++) {
            double distance = size * (0.45D + wave * 0.34D);
            double waveRadius = size * (0.16D + wave * 0.1D);
            details.add(() -> verticalRing(level, family,
                    center.add(direction.scale(distance)), direction, waveRadius,
                    Math.min(14, Math.max(7, intensity / 4)), random, 0.13F));
        }
        emitFigureComponents(level, 163, List.of(() -> shortLine(level, family,
                left.subtract(direction.scale(depth)), right.subtract(direction.scale(depth)),
                8, random)), details);
    }

    /** Ritual coffin silhouette: long sealed body, tapered rails and emerging soul motes. */
    private static void ritualCoffinShape(ClientLevel level, TechniqueVfxPalette.Family family,
                                          Vec3 start, Vec3 end, float radius,
                                          int intensity, Random random) {
        Vec3 castDirection = normalized(end.subtract(start), new Vec3(0.0D, 0.0D, 1.0D));
        Vec3 side = perpendicular(castDirection);
        Vec3 center = (start.distanceToSqr(end) < 0.04D ? start : end)
                .add(0.0D, 0.18D, 0.0D);
        double size = Math.max(0.9D, Math.min(3.4D, radius * 0.86D));
        Vec3 foot = center;
        Vec3 head = center.add(0.0D, size * 1.85D, 0.0D);
        double halfWidth = size * 0.38D;
        Vec3 leftFoot = foot.subtract(side.scale(halfWidth * 0.72D));
        Vec3 rightFoot = foot.add(side.scale(halfWidth * 0.72D));
        Vec3 leftHead = head.subtract(side.scale(halfWidth));
        Vec3 rightHead = head.add(side.scale(halfWidth));
        List<Runnable> details = new ArrayList<>();
        details.add(() -> shortLine(level, family, leftFoot, leftHead, 7, random));
        details.add(() -> shortLine(level, family, rightFoot, rightHead, 7, random));
        details.add(() -> shortLine(level, family, leftFoot, rightFoot, 4, random));
        details.add(() -> shortLine(level, family, leftHead, rightHead, 4, random));
        details.add(() -> shortLine(level, family,
                center.add(0.0D, size * 0.4D, 0.0D),
                center.add(0.0D, size * 1.45D, 0.0D), 5, random));
        details.add(() -> rotatingRing(level, family,
                head.add(0.0D, size * 0.12D, 0.0D), halfWidth * 0.7D,
                Math.min(12, Math.max(6, intensity / 5)),
                level.getGameTime() * 0.08D, random));
        details.add(() -> spawn(level, LodestoneParticleRegistry.WISP_PARTICLE, family,
                head.add(randomOffset(random, halfWidth * 0.35D)),
                new Vec3(0.0D, 0.018D, 0.0D), 0.17F, 0.7F, 28,
                random.nextFloat()));
        emitFigureComponents(level, 167,
                List.of(() -> shortLine(level, family, foot, head, 9, random)), details);
    }

    /** Talisman brush silhouette: lacquered shaft, ferrule, bristles and emitted glyph sparks. */
    private static void talismanBrushShape(ClientLevel level, TechniqueVfxPalette.Family family,
                                           Vec3 start, Vec3 end, float radius,
                                           int intensity, Random random) {
        Vec3 direction = normalized(end.subtract(start), new Vec3(0.0D, 1.0D, 0.0D));
        Vec3 side = perpendicular(direction);
        double size = Math.max(0.65D, Math.min(2.5D, radius * 0.82D));
        Vec3 center = start.distanceToSqr(end) < 0.04D
                ? start.add(0.0D, size * 0.65D, 0.0D)
                : start.lerp(end, 0.34D);
        Vec3 butt = center.subtract(direction.scale(size * 0.72D));
        Vec3 ferrule = center.add(direction.scale(size * 0.48D));
        Vec3 tip = center.add(direction.scale(size * 0.9D));
        List<Runnable> details = new ArrayList<>();
        details.add(() -> verticalRing(level, family, ferrule, direction, size * 0.12D,
                Math.min(10, Math.max(5, intensity / 5)), random, 0.13F));
        details.add(() -> shortLine(level, family,
                ferrule.subtract(side.scale(size * 0.12D)), tip, 4, random));
        details.add(() -> shortLine(level, family,
                ferrule.add(side.scale(size * 0.12D)), tip, 4, random));
        for (int glyph = 1; glyph <= 4; glyph++) {
            double distance = size * (0.92D + glyph * 0.24D);
            Vec3 glyphPoint = center.add(direction.scale(distance))
                    .add(side.scale((glyph % 2 == 0 ? 1.0D : -1.0D) * size * 0.12D));
            details.add(() -> spawn(level, LodestoneParticleRegistry.STAR_PARTICLE, family,
                    glyphPoint, direction.scale(0.014D), 0.14F, 0.88F, 22,
                    random.nextFloat()));
        }
        emitFigureComponents(level, 173,
                List.of(() -> shortLine(level, family, butt, tip, 8, random)), details);
    }

    /** Folding fan silhouette: pivot, radial ribs, curved rim and trailing handle. */
    private static void magicFanShape(ClientLevel level, TechniqueVfxPalette.Family family,
                                      Vec3 start, Vec3 end, float radius,
                                      int intensity, Random random) {
        Vec3 direction = normalized(end.subtract(start), new Vec3(0.0D, 0.0D, 1.0D));
        Vec3 side = perpendicular(direction);
        Vec3 up = normalized(side.cross(direction), new Vec3(0.0D, 1.0D, 0.0D));
        Vec3 pivot = (start.distanceToSqr(end) < 0.04D ? start : start.lerp(end, 0.3D))
                .add(0.0D, 0.35D, 0.0D);
        double size = Math.max(0.75D, Math.min(2.8D, radius * 0.72D));
        List<Vec3> tips = new ArrayList<>();
        for (int rib = 0; rib < 7; rib++) {
            double angle = -Math.PI * 0.38D + Math.PI * 0.76D * rib / 6.0D;
            tips.add(pivot.add(side.scale(Math.sin(angle) * size))
                    .add(up.scale(Math.cos(angle) * size))
                    .add(direction.scale(size * 0.08D)));
        }
        List<Runnable> details = new ArrayList<>();
        for (int rib = 0; rib < tips.size(); rib++) {
            if (rib != tips.size() / 2) {
                Vec3 tip = tips.get(rib);
                details.add(() -> shortLine(level, family, pivot, tip, 5, random));
            }
            if (rib > 0) {
                Vec3 previous = tips.get(rib - 1);
                Vec3 tip = tips.get(rib);
                details.add(() -> shortLine(level, family, previous, tip, 3, random));
            }
        }
        details.add(() -> shortLine(level, family, pivot,
                pivot.subtract(up.scale(size * 0.42D)), 4, random));
        details.add(() -> spawn(level, LodestoneParticleRegistry.STAR_PARTICLE, family,
                pivot, Vec3.ZERO, 0.16F, 0.88F, 22, random.nextFloat()));
        Vec3 centerTip = tips.get(tips.size() / 2);
        emitFigureComponents(level, 179,
                List.of(() -> shortLine(level, family, pivot, centerTip, 7, random)), details);
    }

    /** Alchemy furnace silhouette: squat chamber, fire mouth, lid and vapor chimney. */
    private static void alchemyFurnaceShape(ClientLevel level, TechniqueVfxPalette.Family family,
                                            Vec3 start, Vec3 end, float radius,
                                            int intensity, Random random) {
        Vec3 center = start.distanceToSqr(end) < 0.04D ? start : end;
        Vec3 direction = normalized(end.subtract(start), new Vec3(0.0D, 0.0D, 1.0D));
        double size = Math.max(0.65D, Math.min(2.7D, radius * 0.72D));
        Vec3 base = center.add(0.0D, size * 0.08D, 0.0D);
        Vec3 chamber = center.add(0.0D, size * 0.62D, 0.0D);
        Vec3 lid = center.add(0.0D, size * 1.2D, 0.0D);
        Vec3 chimney = center.add(0.0D, size * 1.72D, 0.0D);
        List<Runnable> details = new ArrayList<>();
        details.add(() -> ring(level, family, base, size * 0.58D,
                Math.min(14, Math.max(7, intensity / 4)), random, 0.13F, 22));
        details.add(() -> ring(level, family, chamber, size * 0.68D,
                Math.min(16, Math.max(8, intensity / 3)), random, 0.15F, 24));
        details.add(() -> ring(level, family, lid, size * 0.52D,
                Math.min(14, Math.max(7, intensity / 4)), random, 0.13F, 22));
        details.add(() -> verticalRing(level, family,
                chamber.subtract(direction.scale(size * 0.58D)), direction,
                size * 0.24D, Math.min(12, Math.max(6, intensity / 5)), random, 0.16F));
        details.add(() -> ring(level, family, chimney, size * 0.22D,
                Math.min(10, Math.max(5, intensity / 5)), random, 0.12F, 20));
        details.add(() -> spawn(level, LodestoneParticleRegistry.WISP_PARTICLE, family,
                chimney.add(randomOffset(random, size * 0.18D)),
                new Vec3(0.0D, 0.018D, 0.0D), 0.18F, 0.68F, 28,
                random.nextFloat()));
        emitFigureComponents(level, 181,
                List.of(() -> shortLine(level, family, base, chimney, 9, random)), details);
    }

    /** Unfurled scroll silhouette: central sheet, rods, side edges and inscribed glyphs. */
    private static void magicScrollShape(ClientLevel level, TechniqueVfxPalette.Family family,
                                         Vec3 start, Vec3 end, float radius,
                                         int intensity, Random random) {
        Vec3 direction = normalized(end.subtract(start), new Vec3(0.0D, 0.0D, 1.0D));
        Vec3 side = perpendicular(direction);
        Vec3 up = normalized(side.cross(direction), new Vec3(0.0D, 1.0D, 0.0D));
        Vec3 center = (start.distanceToSqr(end) < 0.04D ? start : end)
                .add(0.0D, 0.9D, 0.0D);
        double size = Math.max(0.65D, Math.min(2.5D, radius * 0.64D));
        double halfWidth = size * 0.52D;
        double halfHeight = size * 0.72D;
        Vec3 top = center.add(up.scale(halfHeight));
        Vec3 bottom = center.subtract(up.scale(halfHeight));
        Vec3 topLeft = top.subtract(side.scale(halfWidth));
        Vec3 topRight = top.add(side.scale(halfWidth));
        Vec3 bottomLeft = bottom.subtract(side.scale(halfWidth));
        Vec3 bottomRight = bottom.add(side.scale(halfWidth));
        List<Runnable> details = new ArrayList<>();
        details.add(() -> shortLine(level, family, topLeft, bottomLeft, 6, random));
        details.add(() -> shortLine(level, family, topRight, bottomRight, 6, random));
        details.add(() -> shortLine(level, family,
                topLeft.subtract(side.scale(size * 0.18D)),
                topRight.add(side.scale(size * 0.18D)), 6, random));
        details.add(() -> shortLine(level, family,
                bottomLeft.subtract(side.scale(size * 0.18D)),
                bottomRight.add(side.scale(size * 0.18D)), 6, random));
        for (int glyph = -1; glyph <= 1; glyph++) {
            Vec3 glyphCenter = center.add(up.scale(glyph * size * 0.32D));
            details.add(() -> shortLine(level, family,
                    glyphCenter.subtract(side.scale(size * 0.22D)),
                    glyphCenter.add(side.scale(size * 0.22D)), 3, random));
        }
        details.add(() -> spawn(level, LodestoneParticleRegistry.TWINKLE_PARTICLE, family,
                center, Vec3.ZERO, 0.16F, 0.84F, 22, random.nextFloat()));
        emitFigureComponents(level, 191,
                List.of(() -> shortLine(level, family, top, bottom, 8, random)), details);
    }

    /** Formation disc silhouette: counter-rotating rings, axial marks and rune nodes. */
    private static void formationDiscShape(ClientLevel level, TechniqueVfxPalette.Family family,
                                           Vec3 start, Vec3 end, float radius,
                                           int intensity, Random random) {
        Vec3 center = (start.distanceToSqr(end) < 0.04D ? start : end)
                .add(0.0D, 0.24D, 0.0D);
        Vec3 direction = normalized(end.subtract(start), new Vec3(0.0D, 0.0D, 1.0D));
        Vec3 side = perpendicular(direction);
        double size = Math.max(0.65D, Math.min(3.2D, radius * 0.82D));
        List<Runnable> details = new ArrayList<>();
        details.add(() -> rotatingRing(level, family, center, size * 0.58D,
                Math.min(16, Math.max(8, intensity / 3)),
                -level.getGameTime() * 0.13D, random));
        details.add(() -> shortLine(level, family,
                center.subtract(side.scale(size * 0.82D)),
                center.add(side.scale(size * 0.82D)), 6, random));
        details.add(() -> shortLine(level, family,
                center.add(0.0D, 0.0D, -size * 0.82D),
                center.add(0.0D, 0.0D, size * 0.82D), 6, random));
        for (int node = 0; node < 8; node++) {
            double angle = Math.PI * 2.0D * node / 8.0D;
            Vec3 point = center.add(Math.cos(angle) * size * 0.82D, 0.04D,
                    Math.sin(angle) * size * 0.82D);
            details.add(() -> spawn(level, LodestoneParticleRegistry.STAR_PARTICLE, family,
                    point, Vec3.ZERO, 0.14F, 0.86F, 22, (float) angle));
        }
        emitFigureComponents(level, 193,
                List.of(() -> rotatingRing(level, family, center, size * 0.82D,
                        Math.min(20, Math.max(10, intensity / 2)),
                        level.getGameTime() * 0.1D, random)), details);
    }

    /** Spiked club silhouette: long haft, reinforced head and rotating tooth points. */
    private static void spikedClubShape(ClientLevel level, TechniqueVfxPalette.Family family,
                                        Vec3 start, Vec3 end, float radius,
                                        int intensity, Random random) {
        Vec3 direction = normalized(end.subtract(start), new Vec3(0.0D, 1.0D, 0.0D));
        Vec3 side = perpendicular(direction);
        Vec3 up = normalized(side.cross(direction), new Vec3(0.0D, 0.0D, 1.0D));
        double size = Math.max(0.9D, Math.min(3.7D, radius * 1.08D));
        Vec3 center = start.distanceToSqr(end) < 0.04D
                ? start.add(0.0D, size * 0.82D, 0.0D)
                : start.lerp(end, 0.5D);
        Vec3 butt = center.subtract(direction.scale(size * 0.82D));
        Vec3 headBase = center.add(direction.scale(size * 0.34D));
        Vec3 headTip = center.add(direction.scale(size * 0.92D));
        List<Runnable> details = new ArrayList<>();
        details.add(() -> verticalRing(level, family,
                headBase.lerp(headTip, 0.5D), direction, size * 0.24D,
                Math.min(14, Math.max(7, intensity / 4)), random, 0.15F));
        for (int tooth = 0; tooth < 8; tooth++) {
            double angle = Math.PI * 2.0D * tooth / 8.0D;
            Vec3 radial = side.scale(Math.cos(angle)).add(up.scale(Math.sin(angle)));
            Vec3 root = headBase.lerp(headTip, 0.25D + (tooth % 3) * 0.22D);
            details.add(() -> shortLine(level, family, root,
                    root.add(radial.scale(size * 0.3D)), 2, random));
        }
        details.add(() -> spawn(level, LodestoneParticleRegistry.STAR_PARTICLE, family,
                headTip, direction.scale(0.012D), 0.17F, 0.88F, 22,
                random.nextFloat()));
        emitFigureComponents(level, 197,
                List.of(() -> shortLine(level, family, butt, headTip, 9, random)), details);
    }

    /** Command token silhouette: bordered tablet, suspension eye and inscribed seal. */
    private static void commandTokenShape(ClientLevel level, TechniqueVfxPalette.Family family,
                                          Vec3 start, Vec3 end, float radius,
                                          int intensity, Random random) {
        Vec3 direction = normalized(end.subtract(start), new Vec3(0.0D, 0.0D, 1.0D));
        Vec3 side = perpendicular(direction);
        Vec3 up = normalized(side.cross(direction), new Vec3(0.0D, 1.0D, 0.0D));
        Vec3 center = (start.distanceToSqr(end) < 0.04D ? start : end)
                .add(0.0D, 0.72D, 0.0D);
        double size = Math.max(0.55D, Math.min(2.2D, radius * 0.62D));
        double halfWidth = size * 0.38D;
        double halfHeight = size * 0.68D;
        Vec3 top = center.add(up.scale(halfHeight));
        Vec3 bottom = center.subtract(up.scale(halfHeight));
        Vec3 topLeft = top.subtract(side.scale(halfWidth));
        Vec3 topRight = top.add(side.scale(halfWidth));
        Vec3 bottomLeft = bottom.subtract(side.scale(halfWidth));
        Vec3 bottomRight = bottom.add(side.scale(halfWidth));
        List<Runnable> details = new ArrayList<>();
        details.add(() -> shortLine(level, family, topLeft, topRight, 4, random));
        details.add(() -> shortLine(level, family, bottomLeft, bottomRight, 4, random));
        details.add(() -> shortLine(level, family, topLeft, bottomLeft, 6, random));
        details.add(() -> shortLine(level, family, topRight, bottomRight, 6, random));
        details.add(() -> shortLine(level, family,
                center.subtract(side.scale(halfWidth * 0.55D)),
                center.add(side.scale(halfWidth * 0.55D)), 4, random));
        details.add(() -> shortLine(level, family,
                center.add(up.scale(halfHeight * 0.4D)),
                center.subtract(up.scale(halfHeight * 0.42D)), 4, random));
        details.add(() -> verticalRing(level, family,
                top.add(up.scale(size * 0.1D)), direction, size * 0.12D,
                Math.min(10, Math.max(5, intensity / 5)), random, 0.13F));
        emitFigureComponents(level, 199, List.of(
                () -> shortLine(level, family, topLeft, bottomRight, 7, random),
                () -> shortLine(level, family, topRight, bottomLeft, 7, random)), details);
    }

    /** Open scissors silhouette: paired blades, hinge spark and two finger loops. */
    private static void magicScissorsShape(ClientLevel level, TechniqueVfxPalette.Family family,
                                            Vec3 start, Vec3 end, float radius,
                                            int intensity, Random random) {
        Vec3 direction = normalized(end.subtract(start), new Vec3(0.0D, 0.0D, 1.0D));
        Vec3 side = perpendicular(direction);
        Vec3 hinge = (start.distanceToSqr(end) < 0.04D ? start : start.lerp(end, 0.36D))
                .add(0.0D, 0.55D, 0.0D);
        double size = Math.max(0.8D, Math.min(4.2D, radius * 1.08D));
        Vec3 leftTip = hinge.add(direction.scale(size)).subtract(side.scale(size * 0.28D));
        Vec3 rightTip = hinge.add(direction.scale(size)).add(side.scale(size * 0.28D));
        Vec3 handleBase = hinge.subtract(direction.scale(size * 0.46D));
        Vec3 leftLoop = handleBase.subtract(side.scale(size * 0.22D));
        Vec3 rightLoop = handleBase.add(side.scale(size * 0.22D));
        List<Runnable> details = new ArrayList<>();
        details.add(() -> shortLine(level, family, hinge, leftLoop, 4, random));
        details.add(() -> shortLine(level, family, hinge, rightLoop, 4, random));
        details.add(() -> verticalRing(level, family, leftLoop, direction, size * 0.18D,
                Math.min(12, Math.max(6, intensity / 4)), random, 0.13F));
        details.add(() -> verticalRing(level, family, rightLoop, direction, size * 0.18D,
                Math.min(12, Math.max(6, intensity / 4)), random, 0.13F));
        details.add(() -> spawn(level, LodestoneParticleRegistry.STAR_PARTICLE, family,
                hinge, Vec3.ZERO, 0.17F, 0.9F, 22, random.nextFloat()));
        emitFigureComponents(level, 211, List.of(
                () -> shortLine(level, family, hinge, leftTip, 8, random),
                () -> shortLine(level, family, hinge, rightTip, 8, random)), details);
    }

    /** Brick relic silhouette: compact cuboid with luminous edges and a central seal. */
    private static void magicBrickShape(ClientLevel level, TechniqueVfxPalette.Family family,
                                        Vec3 start, Vec3 end, float radius,
                                        int intensity, Random random) {
        Vec3 direction = normalized(end.subtract(start), new Vec3(0.0D, 0.0D, 1.0D));
        Vec3 side = perpendicular(direction);
        Vec3 up = normalized(side.cross(direction), new Vec3(0.0D, 1.0D, 0.0D));
        Vec3 center = (start.distanceToSqr(end) < 0.04D ? start : start.lerp(end, 0.48D))
                .add(0.0D, 0.45D, 0.0D);
        double size = Math.max(0.65D, Math.min(2.7D, radius * 0.74D));
        Vec3 back = center.subtract(direction.scale(size * 0.62D));
        Vec3 front = center.add(direction.scale(size * 0.62D));
        double halfWidth = size * 0.38D;
        double halfHeight = size * 0.24D;
        Vec3 backLowLeft = back.subtract(side.scale(halfWidth)).subtract(up.scale(halfHeight));
        Vec3 backLowRight = back.add(side.scale(halfWidth)).subtract(up.scale(halfHeight));
        Vec3 backHighLeft = back.subtract(side.scale(halfWidth)).add(up.scale(halfHeight));
        Vec3 backHighRight = back.add(side.scale(halfWidth)).add(up.scale(halfHeight));
        Vec3 frontLowLeft = front.subtract(side.scale(halfWidth)).subtract(up.scale(halfHeight));
        Vec3 frontLowRight = front.add(side.scale(halfWidth)).subtract(up.scale(halfHeight));
        Vec3 frontHighLeft = front.subtract(side.scale(halfWidth)).add(up.scale(halfHeight));
        Vec3 frontHighRight = front.add(side.scale(halfWidth)).add(up.scale(halfHeight));
        List<Runnable> details = new ArrayList<>();
        details.add(() -> shortLine(level, family, backLowLeft, backLowRight, 4, random));
        details.add(() -> shortLine(level, family, backHighLeft, backHighRight, 4, random));
        details.add(() -> shortLine(level, family, frontLowLeft, frontLowRight, 4, random));
        details.add(() -> shortLine(level, family, frontHighLeft, frontHighRight, 4, random));
        details.add(() -> shortLine(level, family, backLowLeft, backHighLeft, 3, random));
        details.add(() -> shortLine(level, family, frontLowRight, frontHighRight, 3, random));
        details.add(() -> spawn(level, LodestoneParticleRegistry.TWINKLE_PARTICLE, family,
                front, direction.scale(0.01D), 0.16F, 0.86F, 22, random.nextFloat()));
        emitFigureComponents(level, 223, List.of(
                () -> shortLine(level, family, backLowLeft, frontLowLeft, 7, random),
                () -> shortLine(level, family, backHighRight, frontHighRight, 7, random)), details);
    }

    /** Jade umbrella silhouette: domed rim, radial ribs, central shaft and cut glint. */
    private static void magicUmbrellaShape(ClientLevel level, TechniqueVfxPalette.Family family,
                                           Vec3 start, Vec3 end, float radius,
                                           int intensity, Random random) {
        Vec3 direction = normalized(end.subtract(start), new Vec3(0.0D, 0.0D, 1.0D));
        Vec3 side = perpendicular(direction);
        Vec3 up = new Vec3(0.0D, 1.0D, 0.0D);
        Vec3 center = (start.distanceToSqr(end) < 0.04D ? start : end)
                .add(0.0D, 0.72D, 0.0D);
        double size = Math.max(0.75D, Math.min(3.0D, radius * 0.82D));
        Vec3 rimCenter = center.add(up.scale(size * 0.18D));
        Vec3 apex = rimCenter.add(up.scale(size * 0.58D));
        Vec3 handle = rimCenter.subtract(up.scale(size * 1.0D));
        List<Runnable> details = new ArrayList<>();
        for (int rib = 0; rib < 8; rib++) {
            double angle = Math.PI * 2.0D * rib / 8.0D;
            Vec3 point = rimCenter.add(side.scale(Math.cos(angle) * size * 0.72D))
                    .add(direction.scale(Math.sin(angle) * size * 0.72D));
            details.add(() -> shortLine(level, family, apex, point, 5, random));
        }
        details.add(() -> shortLine(level, family,
                rimCenter.subtract(side.scale(size * 0.72D)),
                rimCenter.add(side.scale(size * 0.72D)), 6, random));
        details.add(() -> spawn(level, LodestoneParticleRegistry.STAR_PARTICLE, family,
                rimCenter, side.scale(0.012D), 0.18F, 0.9F, 22,
                random.nextFloat()));
        emitFigureComponents(level, 227, List.of(
                () -> shortLine(level, family, apex, handle, 8, random),
                () -> ring(level, family, rimCenter, size * 0.72D,
                        Math.min(20, Math.max(10, intensity / 2)), random, 0.14F, 22)), details);
    }

    /** Drawn bow silhouette: curved limbs, taut string and a nocked forward arrow. */
    private static void magicBowShape(ClientLevel level, TechniqueVfxPalette.Family family,
                                      Vec3 start, Vec3 end, float radius,
                                      int intensity, Random random) {
        Vec3 direction = normalized(end.subtract(start), new Vec3(0.0D, 0.0D, 1.0D));
        Vec3 side = perpendicular(direction);
        Vec3 up = normalized(side.cross(direction), new Vec3(0.0D, 1.0D, 0.0D));
        Vec3 grip = (start.distanceToSqr(end) < 0.04D ? start : start.lerp(end, 0.24D))
                .add(0.0D, 0.72D, 0.0D);
        double size = Math.max(0.85D, Math.min(3.3D, radius * 0.92D));
        Vec3 top = grip.add(up.scale(size));
        Vec3 bottom = grip.subtract(up.scale(size));
        Vec3 upperBend = grip.add(up.scale(size * 0.5D)).add(side.scale(size * 0.3D));
        Vec3 lowerBend = grip.subtract(up.scale(size * 0.5D)).add(side.scale(size * 0.3D));
        Vec3 nock = grip.subtract(direction.scale(size * 0.42D));
        Vec3 arrowTip = grip.add(direction.scale(size * 1.3D));
        List<Runnable> details = new ArrayList<>();
        details.add(() -> shortLine(level, family, top, nock, 6, random));
        details.add(() -> shortLine(level, family, nock, bottom, 6, random));
        details.add(() -> shortLine(level, family, nock, arrowTip, 9, random));
        details.add(() -> shortLine(level, family,
                arrowTip.subtract(direction.scale(size * 0.16D)).add(side.scale(size * 0.12D)),
                arrowTip, 3, random));
        details.add(() -> shortLine(level, family,
                arrowTip.subtract(direction.scale(size * 0.16D)).subtract(side.scale(size * 0.12D)),
                arrowTip, 3, random));
        details.add(() -> spawn(level, LodestoneParticleRegistry.STAR_PARTICLE, family,
                arrowTip, direction.scale(0.018D), 0.17F, 0.9F, 22,
                random.nextFloat()));
        emitFigureComponents(level, 229, List.of(
                () -> {
                    shortLine(level, family, top, upperBend, 5, random);
                    shortLine(level, family, upperBend, grip, 5, random);
                },
                () -> {
                    shortLine(level, family, grip, lowerBend, 5, random);
                    shortLine(level, family, lowerBend, bottom, 5, random);
                }), details);
    }

    /** Giant gong silhouette: suspended rim, inner face, central boss and striker. */
    private static void magicGongShape(ClientLevel level, TechniqueVfxPalette.Family family,
                                       Vec3 start, Vec3 end, float radius,
                                       int intensity, Random random) {
        Vec3 normal = normalized(end.subtract(start), new Vec3(0.0D, 0.0D, 1.0D));
        Vec3 side = perpendicular(normal);
        Vec3 up = normalized(side.cross(normal), new Vec3(0.0D, 1.0D, 0.0D));
        Vec3 center = (start.distanceToSqr(end) < 0.04D ? start : end)
                .add(up.scale(1.05D));
        double size = Math.max(0.8D, Math.min(3.4D, radius * 0.92D));
        Vec3 hanger = center.add(up.scale(size * 0.92D));
        Vec3 leftLug = center.add(up.scale(size * 0.65D)).subtract(side.scale(size * 0.36D));
        Vec3 rightLug = center.add(up.scale(size * 0.65D)).add(side.scale(size * 0.36D));
        Vec3 strikerHead = center.add(side.scale(size * 1.0D)).add(up.scale(size * 0.18D));
        Vec3 strikerGrip = strikerHead.add(up.scale(size * 0.72D));
        List<Runnable> details = new ArrayList<>();
        details.add(() -> verticalRing(level, family, center, normal,
                size * 0.48D, Math.min(16, Math.max(8, intensity / 3)), random, 0.14F));
        details.add(() -> shortLine(level, family, hanger, leftLug, 4, random));
        details.add(() -> shortLine(level, family, hanger, rightLug, 4, random));
        details.add(() -> shortLine(level, family,
                center.subtract(side.scale(size * 0.58D)),
                center.add(side.scale(size * 0.58D)), 5, random));
        details.add(() -> shortLine(level, family, strikerHead, strikerGrip, 5, random));
        details.add(() -> spawn(level, LodestoneParticleRegistry.TWINKLE_PARTICLE, family,
                center, normal.scale(0.012D), 0.2F, 0.94F, 24, random.nextFloat()));
        emitFigureComponents(level, 233, List.of(
                () -> verticalRing(level, family, center, normal,
                        size * 0.78D, Math.min(20, Math.max(10, intensity / 2)), random, 0.16F),
                () -> verticalRing(level, family, center, normal,
                        size * 0.18D, Math.min(10, Math.max(5, intensity / 5)), random, 0.16F)),
                details);
    }

    /** Mask silhouette: facial rim, eye slits, brow seal and tapered jaw. */
    private static void magicMaskShape(ClientLevel level, TechniqueVfxPalette.Family family,
                                       Vec3 start, Vec3 end, float radius,
                                       int intensity, Random random) {
        Vec3 normal = normalized(end.subtract(start), new Vec3(0.0D, 0.0D, 1.0D));
        Vec3 side = perpendicular(normal);
        Vec3 up = normalized(side.cross(normal), new Vec3(0.0D, 1.0D, 0.0D));
        Vec3 center = (start.distanceToSqr(end) < 0.04D ? start : end)
                .add(up.scale(1.42D));
        double size = Math.max(0.48D, Math.min(1.65D, radius * 0.5D));
        Vec3 leftEye = center.subtract(side.scale(size * 0.24D)).add(up.scale(size * 0.12D));
        Vec3 rightEye = center.add(side.scale(size * 0.24D)).add(up.scale(size * 0.12D));
        Vec3 chin = center.subtract(up.scale(size * 0.72D));
        Vec3 leftCheek = center.subtract(side.scale(size * 0.48D)).subtract(up.scale(size * 0.2D));
        Vec3 rightCheek = center.add(side.scale(size * 0.48D)).subtract(up.scale(size * 0.2D));
        List<Runnable> details = new ArrayList<>();
        details.add(() -> shortLine(level, family,
                leftEye.subtract(side.scale(size * 0.16D)),
                leftEye.add(side.scale(size * 0.13D)), 3, random));
        details.add(() -> shortLine(level, family,
                rightEye.subtract(side.scale(size * 0.13D)),
                rightEye.add(side.scale(size * 0.16D)), 3, random));
        details.add(() -> shortLine(level, family, leftCheek, chin, 4, random));
        details.add(() -> shortLine(level, family, rightCheek, chin, 4, random));
        details.add(() -> shortLine(level, family,
                center.add(up.scale(size * 0.44D)),
                center.subtract(up.scale(size * 0.2D)), 3, random));
        details.add(() -> spawn(level, LodestoneParticleRegistry.STAR_PARTICLE, family,
                center.add(up.scale(size * 0.46D)), normal.scale(0.008D),
                0.14F, 0.88F, 20, random.nextFloat()));
        emitFigureComponents(level, 239, List.of(
                () -> verticalRing(level, family, center, normal,
                        size * 0.62D, Math.min(18, Math.max(9, intensity / 3)), random, 0.14F),
                () -> shortLine(level, family, leftEye, rightEye, 5, random)), details);
    }

    /** Flying cloth silhouette: rippling rectangular weave, folds and a luminous seal. */
    private static void magicClothShape(ClientLevel level, TechniqueVfxPalette.Family family,
                                        Vec3 start, Vec3 end, float radius,
                                        int intensity, Random random) {
        Vec3 normal = normalized(end.subtract(start), new Vec3(0.0D, 0.0D, 1.0D));
        Vec3 side = perpendicular(normal);
        Vec3 up = normalized(side.cross(normal), new Vec3(0.0D, 1.0D, 0.0D));
        Vec3 center = (start.distanceToSqr(end) < 0.04D ? start : start.lerp(end, 0.58D))
                .add(up.scale(0.88D));
        double size = Math.max(0.7D, Math.min(3.0D, radius * 0.8D));
        double halfWidth = size * 0.72D;
        double halfHeight = size * 0.52D;
        double wave = Math.sin(level.getGameTime() * 0.18D) * size * 0.12D;
        Vec3 topLeft = center.subtract(side.scale(halfWidth)).add(up.scale(halfHeight));
        Vec3 topRight = center.add(side.scale(halfWidth)).add(up.scale(halfHeight))
                .add(normal.scale(wave));
        Vec3 bottomLeft = center.subtract(side.scale(halfWidth)).subtract(up.scale(halfHeight))
                .subtract(normal.scale(wave));
        Vec3 bottomRight = center.add(side.scale(halfWidth)).subtract(up.scale(halfHeight));
        Vec3 foldTop = center.add(up.scale(halfHeight)).add(normal.scale(wave * 0.5D));
        Vec3 foldBottom = center.subtract(up.scale(halfHeight)).subtract(normal.scale(wave * 0.5D));
        List<Runnable> details = new ArrayList<>();
        details.add(() -> shortLine(level, family, topLeft, bottomLeft, 5, random));
        details.add(() -> shortLine(level, family, topRight, bottomRight, 5, random));
        details.add(() -> shortLine(level, family, foldTop, foldBottom, 5, random));
        details.add(() -> shortLine(level, family,
                topLeft.lerp(bottomLeft, 0.5D), topRight.lerp(bottomRight, 0.5D), 5, random));
        details.add(() -> shortLine(level, family, topLeft, bottomRight, 6, random));
        details.add(() -> spawn(level, LodestoneParticleRegistry.TWINKLE_PARTICLE, family,
                center, normal.scale(0.01D), 0.16F, 0.88F, 22, random.nextFloat()));
        emitFigureComponents(level, 241, List.of(
                () -> shortLine(level, family, topLeft, topRight, 7, random),
                () -> shortLine(level, family, bottomLeft, bottomRight, 7, random)), details);
    }

    /** Rune pillar silhouette: tall shaft, base/capital rings, side edges and rune orbit. */
    private static void runePillarShape(ClientLevel level, TechniqueVfxPalette.Family family,
                                        Vec3 start, Vec3 end, float radius,
                                        int intensity, Random random) {
        Vec3 direction = normalized(end.subtract(start), new Vec3(0.0D, 0.0D, 1.0D));
        Vec3 side = perpendicular(direction);
        Vec3 base = start.distanceToSqr(end) < 0.04D ? start : end;
        double height = Math.max(1.8D, Math.min(6.5D, radius * 2.3D));
        double width = Math.max(0.28D, Math.min(1.25D, radius * 0.34D));
        Vec3 top = base.add(0.0D, height, 0.0D);
        Vec3 middle = base.lerp(top, 0.52D);
        List<Runnable> details = new ArrayList<>();
        details.add(() -> ring(level, family, top, width,
                Math.min(16, Math.max(8, intensity / 3)), random, 0.14F, 24));
        details.add(() -> rotatingRing(level, family, middle, width * 1.18D,
                Math.min(16, Math.max(8, intensity / 3)),
                level.getGameTime() * 0.12D, random));
        details.add(() -> shortLine(level, family,
                base.subtract(side.scale(width * 0.72D)),
                top.subtract(side.scale(width * 0.72D)), 8, random));
        details.add(() -> shortLine(level, family,
                base.add(side.scale(width * 0.72D)),
                top.add(side.scale(width * 0.72D)), 8, random));
        details.add(() -> shortLine(level, family,
                middle.subtract(side.scale(width * 0.48D)).subtract(0.0D, height * 0.1D, 0.0D),
                middle.add(side.scale(width * 0.48D)).add(0.0D, height * 0.1D, 0.0D),
                4, random));
        details.add(() -> spawn(level, LodestoneParticleRegistry.STAR_PARTICLE, family,
                top, new Vec3(0.0D, 0.012D, 0.0D), 0.18F, 0.9F, 24,
                random.nextFloat()));
        emitFigureComponents(level, 251, List.of(
                () -> shortLine(level, family, base, top, 10, random),
                () -> ring(level, family, base, width * 1.12D,
                        Math.min(18, Math.max(9, intensity / 3)), random, 0.15F, 24)), details);
    }

    /** Spirit armor silhouette: breastplate, pauldrons, waist, helm and chest seal. */
    private static void spiritArmorShape(ClientLevel level, TechniqueVfxPalette.Family family,
                                         Vec3 start, Vec3 end, float radius,
                                         int intensity, Random random) {
        Vec3 normal = normalized(end.subtract(start), new Vec3(0.0D, 0.0D, 1.0D));
        Vec3 side = perpendicular(normal);
        Vec3 up = normalized(side.cross(normal), new Vec3(0.0D, 1.0D, 0.0D));
        Vec3 center = (start.distanceToSqr(end) < 0.04D ? start : end)
                .add(up.scale(1.05D));
        double size = Math.max(0.72D, Math.min(2.5D, radius * 0.74D));
        Vec3 leftShoulder = center.subtract(side.scale(size * 0.72D)).add(up.scale(size * 0.42D));
        Vec3 rightShoulder = center.add(side.scale(size * 0.72D)).add(up.scale(size * 0.42D));
        Vec3 leftWaist = center.subtract(side.scale(size * 0.4D)).subtract(up.scale(size * 0.56D));
        Vec3 rightWaist = center.add(side.scale(size * 0.4D)).subtract(up.scale(size * 0.56D));
        Vec3 helm = center.add(up.scale(size * 0.98D));
        List<Runnable> details = new ArrayList<>();
        details.add(() -> shortLine(level, family, leftShoulder, rightShoulder, 7, random));
        details.add(() -> shortLine(level, family, leftWaist, rightWaist, 5, random));
        details.add(() -> shortLine(level, family, leftShoulder,
                leftShoulder.subtract(side.scale(size * 0.3D)).subtract(up.scale(size * 0.18D)),
                3, random));
        details.add(() -> shortLine(level, family, rightShoulder,
                rightShoulder.add(side.scale(size * 0.3D)).subtract(up.scale(size * 0.18D)),
                3, random));
        details.add(() -> verticalRing(level, family, helm, normal,
                size * 0.28D, Math.min(12, Math.max(6, intensity / 4)), random, 0.14F));
        details.add(() -> shortLine(level, family,
                center.subtract(side.scale(size * 0.28D)),
                center.add(side.scale(size * 0.28D)), 4, random));
        details.add(() -> spawn(level, LodestoneParticleRegistry.STAR_PARTICLE, family,
                center, normal.scale(0.01D), 0.17F, 0.9F, 22, random.nextFloat()));
        emitFigureComponents(level, 257, List.of(
                () -> shortLine(level, family, leftShoulder, leftWaist, 7, random),
                () -> shortLine(level, family, rightShoulder, rightWaist, 7, random)), details);
    }

    private static float motionRadius(VisualProgramLayer.Motion motion, Phase phase) {
        return switch (motion) {
            case MATERIALIZE -> phase == Phase.AFTERGLOW ? 0.72F : 1.0F;
            case DISSOLVE -> phase == Phase.AFTERGLOW ? 1.18F : 1.0F;
            case PULSE -> phase == Phase.SUSTAIN ? 1.12F : 1.0F;
            default -> 1.0F;
        };
    }

    private static ProgramCoordinates programCoordinates(ClientLevel level, ActiveVfx active,
                                                         VisualProgramLayer layer, int copy,
                                                         Random random, Phase phase) {
        Vec3 start = active.start;
        Vec3 end = active.end;
        Vec3 direction = normalized(end.subtract(start), new Vec3(0.0D, 0.0D, 1.0D));
        Vec3 side = perpendicular(direction);
        Vec3 up = normalized(side.cross(direction), new Vec3(0.0D, 1.0D, 0.0D));
        Vec3 base = switch (layer.anchor()) {
            case TARGET -> end;
            case MIDPOINT -> start.lerp(end, 0.5D);
            case PATH -> start.lerp(end, 0.35D);
            case SCREEN, CASTER -> start;
        };
        base = base.add(0.0D, layer.verticalOffset(), 0.0D);
        double copies = Math.max(1, layer.copies());
        double angle = Math.toRadians(layer.rotationDegrees())
                + level.getGameTime() * layer.speed() * 0.08D
                + (Math.PI * 2.0D * copy / copies);
        Vec3 radial = side.scale(Math.cos(angle)).add(up.scale(Math.sin(angle)));
        double spread = Math.toRadians(layer.spreadDegrees());
        if (spread > 0.0D && layer.copies() > 1) {
            radial = side.scale(Math.cos(angle * spread / Math.PI))
                    .add(up.scale(Math.sin(angle * spread / Math.PI)));
        }
        if (layer.jitter() > 0.0D) {
            base = base.add(randomOffset(random, layer.jitter() * 0.35D));
        }
        double radius = Math.max(0.1D, active.packet.radius() * layer.radiusScale());
        Vec3 source = start;
        Vec3 target = end;
        switch (layer.path()) {
            case STATIC -> source = target = base;
            case CONVERGE -> {
                source = base.add(radial.scale(radius * 0.65D));
                target = base;
            }
            case EXPAND -> {
                source = base;
                target = base.add(radial.scale(radius * layer.lengthScale()));
            }
            case RISE -> {
                source = base;
                target = base.add(up.scale(layer.heightScale() * radius));
            }
            case FALL -> {
                source = base.add(up.scale(layer.heightScale() * radius));
                target = base;
            }
            case ORBIT -> source = target = base.add(radial.scale(radius * layer.radiusScale()));
            case SPIRAL -> {
                source = start.add(radial.scale(radius * 0.3D));
                target = end.add(radial.scale(radius * layer.lengthScale() * 0.45D));
            }
            case SCATTER -> {
                source = base;
                target = base.add(randomDirection(random).scale(radius * layer.lengthScale()));
            }
            case WAVE -> {
                double wave = Math.sin(angle * 2.0D) * radius * 0.22D;
                source = start.add(radial.scale(wave));
                target = end.add(radial.scale(wave));
            }
            case DIRECT, TRACK -> {
                source = start;
                target = end;
            }
        }
        if (layer.lengthScale() != 1.0D && layer.path() != VisualProgramLayer.Path.STATIC) {
            target = source.add(target.subtract(source).scale(layer.lengthScale()));
        }
        if (phase == Phase.AFTERGLOW && layer.motion() == VisualProgramLayer.Motion.DISSOLVE) {
            source = source.lerp(target, 0.15D);
        }
        return new ProgramCoordinates(source, target);
    }

    private record ProgramCoordinates(Vec3 start, Vec3 end) {}

    private static void emitAuthoredLayers(ClientLevel level, TechniqueVfxPacket packet,
                                           Vec3 start, Vec3 end, int intensity, Random random,
                                           Phase phase) {
        boolean particleActive = packet.particleStyle() != TechniqueVfxPacket.ParticleStyle.DEFAULT;
        boolean trailActive = (phase == Phase.RELEASE || phase == Phase.SUSTAIN)
                && packet.trailStyle() != TechniqueVfxPacket.TrailStyle.DEFAULT
                && packet.trailStyle() != TechniqueVfxPacket.TrailStyle.NONE;
        int systems = (particleActive ? 1 : 0) + (trailActive ? 1 : 0);
        if (systems == 0 || systems > MAX_AUTHORED_SYSTEMS_PER_EVENT) {
            return;
        }
        int available = activeEmissionBudget == null ? 6 : activeEmissionBudget.remaining;
        int perSystem = Math.min(4, Math.max(0, available / (systems + 1)));
        if (perSystem <= 0) {
            return;
        }
        if (particleActive) {
            withSubBudget(perSystem,
                    () -> authoredParticleLayer(level, packet, start, end, intensity, random, phase));
        }
        if (trailActive) {
            withSubBudget(perSystem,
                    () -> authoredTrailLayer(level, packet, start, end, intensity, random, phase));
        }
    }

    /** Emits the single authored particle system selected by the v121 frame sheet. */
    private static void authoredParticleLayer(ClientLevel level, TechniqueVfxPacket packet,
                                              Vec3 start, Vec3 end, int intensity, Random random,
                                              Phase phase) {
        TechniqueVfxPacket.ParticleStyle style = packet.particleStyle();
        if (style == TechniqueVfxPacket.ParticleStyle.DEFAULT) {
            return;
        }
        int count = authoredCount(intensity, phase);
        Vec3 anchor = phase == Phase.AFTERGLOW ? end : start;
        if (style == TechniqueVfxPacket.ParticleStyle.WATER_MIST_METAL_SPARK) {
            for (int i = 0; i < count && budgetAvailable(level); i++) {
                authoredParticleSystem(level, packet.family(), anchor, start, end,
                        (i & 1) == 0
                                ? TechniqueVfxPacket.ParticleStyle.WATER_MIST
                                : TechniqueVfxPacket.ParticleStyle.METAL_SPARK,
                        1, random);
            }
            return;
        }
        authoredParticleSystem(level, packet.family(), anchor, start, end, style, count, random);
    }

    private static int authoredCount(int intensity, Phase phase) {
        int phaseCap = switch (phase) {
            case ANTICIPATION -> 6;
            case RELEASE -> 16;
            case SUSTAIN -> 8;
            case AFTERGLOW -> 5;
        };
        return Math.max(2, Math.min(phaseCap, Math.max(2, intensity / 3)));
    }

    private static void authoredParticleSystem(ClientLevel level, TechniqueVfxPalette.Family family,
                                               Vec3 anchor, Vec3 start, Vec3 end,
                                               TechniqueVfxPacket.ParticleStyle style,
                                               int count, Random random) {
        if (count <= 0) {
            return;
        }
        if (style == TechniqueVfxPacket.ParticleStyle.THUNDER_ARC
                && start.distanceToSqr(end) > 0.04D) {
            int available = activeEmissionBudget == null ? count : activeEmissionBudget.remaining;
            authoredJaggedLine(level, family, start, end,
                    Math.max(1, Math.min(5, available - 1)), ModParticles.THUNDER_ARC, random);
            return;
        }
        for (int i = 0; i < count && budgetAvailable(level); i++) {
            ParticleEmission emission = switch (style) {
                case QI_SOFT -> {
                    Vec3 point = anchor.add(randomOffset(random, 0.26D));
                    Vec3 motion = new Vec3(0.0D, 0.018D + random.nextDouble() * 0.018D, 0.0D);
                    yield new ParticleEmission(ModParticles.QI_SOFT,
                            point, motion, 0.16F, 0.58F, 20 + random.nextInt(10));
                }
                case FIRE_EMBER -> {
                    Vec3 point = anchor.add(randomOffset(random, 0.18D));
                    Vec3 motion = new Vec3((random.nextDouble() - 0.5D) * 0.025D,
                            0.035D + random.nextDouble() * 0.045D,
                            (random.nextDouble() - 0.5D) * 0.025D);
                    yield new ParticleEmission(ModParticles.FIRE_EMBER,
                            point, motion, 0.13F, 0.82F, 8 + random.nextInt(9));
                }
                case WATER_MIST -> {
                    Vec3 point = anchor.add(randomOffset(random, 0.32D));
                    Vec3 motion = new Vec3((random.nextDouble() - 0.5D) * 0.018D,
                            -0.006D + random.nextDouble() * 0.012D,
                            (random.nextDouble() - 0.5D) * 0.018D);
                    yield new ParticleEmission(ModParticles.WATER_MIST,
                            point, motion, 0.22F, 0.48F, 22 + random.nextInt(12));
                }
                case WOOD_POLLEN -> {
                    Vec3 point = anchor.add(randomOffset(random, 0.38D));
                    Vec3 motion = new Vec3((random.nextDouble() - 0.5D) * 0.012D,
                            0.004D + random.nextDouble() * 0.009D,
                            (random.nextDouble() - 0.5D) * 0.012D);
                    yield new ParticleEmission(ModParticles.WOOD_POLLEN,
                            point, motion, 0.12F, 0.62F, 28 + random.nextInt(12));
                }
                case METAL_SPARK -> {
                    Vec3 point = anchor.add(randomOffset(random, 0.12D));
                    Vec3 motion = randomDirection(random).scale(0.055D + random.nextDouble() * 0.065D);
                    yield new ParticleEmission(ModParticles.METAL_SPARK,
                            point, motion, 0.10F, 0.86F, 6 + random.nextInt(6));
                }
                case EARTH_DUST -> {
                    double angle = random.nextDouble() * Math.PI * 2.0D;
                    double distance = 0.18D + random.nextDouble() * 0.62D;
                    Vec3 point = anchor.add(Math.cos(angle) * distance, 0.04D + random.nextDouble() * 0.12D,
                            Math.sin(angle) * distance);
                    Vec3 motion = new Vec3(Math.cos(angle) * 0.018D, -0.004D,
                            Math.sin(angle) * 0.018D);
                    yield new ParticleEmission(ModParticles.EARTH_DUST,
                            point, motion, 0.20F, 0.48F, 14 + random.nextInt(10));
                }
                case THUNDER_ARC -> {
                    Vec3 point = anchor.add(randomOffset(random, 0.18D));
                    Vec3 motion = randomDirection(random).scale(0.025D);
                    yield new ParticleEmission(ModParticles.THUNDER_ARC,
                            point, motion, 0.11F, 0.9F, 5 + random.nextInt(4));
                }
                case YIN_SMOKE -> {
                    Vec3 point = anchor.add(randomOffset(random, 0.35D));
                    Vec3 motion = new Vec3((random.nextDouble() - 0.5D) * 0.012D,
                            -0.014D - random.nextDouble() * 0.012D,
                            (random.nextDouble() - 0.5D) * 0.012D);
                    yield new ParticleEmission(ModParticles.YIN_SMOKE,
                            point, motion, 0.25F, 0.52F, 26 + random.nextInt(14));
                }
                case SOUL_WISPS -> {
                    Vec3 point = anchor.add(randomOffset(random, 0.44D));
                    Vec3 motion = randomDirection(random).scale(0.012D);
                    yield new ParticleEmission(ModParticles.SOUL_WISPS,
                            point, motion, 0.20F, 0.5F, 18 + random.nextInt(16));
                }
                case BLOOD_MIST -> {
                    Vec3 point = anchor.add(randomOffset(random, 0.25D));
                    Vec3 motion = new Vec3((random.nextDouble() - 0.5D) * 0.012D,
                            -0.022D - random.nextDouble() * 0.014D,
                            (random.nextDouble() - 0.5D) * 0.012D);
                    yield new ParticleEmission(ModParticles.BLOOD_MIST,
                            point, motion, 0.22F, 0.58F, 14 + random.nextInt(10));
                }
                case HEAL_MOTES -> {
                    Vec3 point = anchor.add(randomOffset(random, 0.3D));
                    yield new ParticleEmission(ModParticles.HEAL_MOTES,
                            point, new Vec3(0.0D, -0.018D, 0.0D),
                            0.14F, 0.72F, 18 + random.nextInt(10));
                }
                case SPACE_GLITCH -> {
                    Vec3 point = anchor.add(randomOffset(random, 0.42D));
                    Vec3 motion = randomDirection(random).scale(0.01D);
                    yield new ParticleEmission(ModParticles.SPACE_GLITCH,
                            point, motion, 0.08F, 0.44F, 4 + random.nextInt(5));
                }
                case DEFAULT, WATER_MIST_METAL_SPARK -> null;
            };
            if (emission == null) {
                return;
            }
            spawn(level, emission.particle(), family, emission.point(), emission.motion(),
                    emission.scale(), emission.alpha(), emission.lifetime(), random.nextFloat());
        }
    }

    /** Adds only the authored ribbon language; NONE intentionally emits no extra geometry. */
    private static void authoredTrailLayer(ClientLevel level, TechniqueVfxPacket packet,
                                           Vec3 start, Vec3 end, int intensity, Random random,
                                           Phase phase) {
        if (phase == Phase.AFTERGLOW || packet.trailStyle() == TechniqueVfxPacket.TrailStyle.DEFAULT
                || packet.trailStyle() == TechniqueVfxPacket.TrailStyle.NONE) {
            return;
        }
        TechniqueVfxPalette.Family family = packet.family();
        int available = activeEmissionBudget == null ? 8 : activeEmissionBudget.remaining;
        int points = Math.min(18, Math.max(1, Math.min(Math.max(1, intensity / 3), available - 1)));
        boolean emitted = switch (packet.trailStyle()) {
            case SWORD_THIN -> {
                shortLine(level, family, start, end, points, random);
                yield true;
            }
            case HEAVY_WEAPON -> {
                if (start.distanceToSqr(end) > 0.04D) {
                    shortLine(level, family, start, end, points, random);
                }
                yield true;
            }
            case FLYING_SWORD_ORBIT -> {
                rotatingRing(level, family, start.add(0.0D, 0.55D, 0.0D),
                        Math.max(0.35D, packet.radius() * 0.42D), points, level.getGameTime() * 0.12D, random);
                yield true;
            }
            case TALISMAN_ASH -> {
                ashTrail(level, family, start, end, points, random);
                yield true;
            }
            case BLOOD_RIBBON -> {
                helix(level, family, start, end, Math.max(0.12D, packet.radius() * 0.14D), points, random);
                yield true;
            }
            case THUNDER_JAGGED -> {
                authoredJaggedLine(level, family, start, end, Math.min(7, points / 2), random);
                yield true;
            }
            case SOUL_AFTERIMAGE -> {
                afterimageTrail(level, family, start, end, points, random);
                yield true;
            }
            case MOVEMENT_WIND -> {
                movementWindTrail(level, family, start, end, packet.radius(), points, random);
                yield true;
            }
            case DEFAULT, NONE -> {
                yield false;
            }
        };
        if (!emitted) {
            return;
        }
    }

    private static void movementWindTrail(ClientLevel level, TechniqueVfxPalette.Family family,
                                          Vec3 start, Vec3 end, float radius, int points, Random random) {
        Vec3 normal = normalized(end.subtract(start), new Vec3(0.0D, 0.0D, 1.0D));
        int available = activeEmissionBudget == null ? points * 2 : activeEmissionBudget.remaining;
        int each = Math.max(1, available / 2);
        withSubBudget(each, () -> verticalRing(level, family, start.add(0.0D, 0.85D, 0.0D), normal,
                Math.max(0.5D, radius * 0.45D), Math.min(points, each), random, 0.12F));
        withSubBudget(each, () -> verticalRing(level, family, end.add(0.0D, 0.85D, 0.0D), normal,
                Math.max(0.5D, radius * 0.45D), Math.min(points, each), random, 0.12F));
    }

    private static void ashTrail(ClientLevel level, TechniqueVfxPalette.Family family,
                                 Vec3 start, Vec3 end, int points, Random random) {
        Vec3 delta = end.subtract(start);
        for (int i = 0; i <= points && budgetAvailable(level); i++) {
            Vec3 point = start.add(delta.scale(i / (double) points)).add(randomOffset(random, 0.08D));
            spawn(level, LodestoneParticleRegistry.WISP_PARTICLE, family, point,
                    new Vec3(0.0D, -0.018D, 0.0D), 0.14F, 0.48F, 18, random.nextFloat());
        }
    }

    private static void afterimageTrail(ClientLevel level, TechniqueVfxPalette.Family family,
                                        Vec3 start, Vec3 end, int points, Random random) {
        Vec3 delta = end.subtract(start);
        int samples = Math.max(2, Math.min(points + 1,
                activeEmissionBudget == null ? points + 1 : activeEmissionBudget.remaining));
        for (int i = 0; i < samples && budgetAvailable(level); i++) {
            double progress = i / (double) (samples - 1);
            Vec3 point = start.add(delta.scale(progress)).add(0.0D, 0.35D, 0.0D);
            spawn(level, LodestoneParticleRegistry.WISP_PARTICLE, family, point,
                    new Vec3(0.0D, 0.006D, 0.0D), 0.22F, 0.34F, 12, random.nextFloat());
        }
    }

    private static void authoredJaggedLine(ClientLevel level, TechniqueVfxPalette.Family family,
                                           Vec3 start, Vec3 end, int segments, Random random) {
        authoredJaggedLine(level, family, start, end, segments,
                LodestoneParticleRegistry.THIN_EXTRUDING_SPARK_PARTICLE, random);
    }

    private static void authoredJaggedLine(ClientLevel level, TechniqueVfxPalette.Family family,
                                           Vec3 start, Vec3 end, int segments,
                                           RegistryObject<? extends LodestoneWorldParticleType> particle,
                                           Random random) {
        if (segments <= 0 || start.distanceToSqr(end) < 0.04D) {
            return;
        }
        Vec3 delta = end.subtract(start);
        Vec3 side = perpendicular(delta.normalize());
        int available = activeEmissionBudget == null ? segments + 1 : activeEmissionBudget.remaining;
        int vertices = Math.min(segments + 1, available);
        if (vertices < 2) {
            return;
        }
        for (int i = 0; i < vertices && budgetAvailable(level); i++) {
            double progress = i / (double) (vertices - 1);
            double offset = i == 0 || i == vertices - 1
                    ? 0.0D : (random.nextDouble() - 0.5D) * 0.42D;
            Vec3 point = start.add(delta.scale(progress)).add(side.scale(offset));
            Vec3 motion = i + 1 < vertices
                    ? end.subtract(point).normalize().scale(0.004D) : Vec3.ZERO;
            spawn(level, particle,
                    family, point, motion, 0.11F, 0.84F, 6, random.nextFloat());
        }
    }

    private static void projectileTrail(ClientLevel level, Entity entity,
                                        TechniqueVfxPalette.Family family,
                                        TechniqueVfxPacket.TrailStyle trailStyle,
                                        String profileKey,
                                        boolean sword) {
        TechniqueVfxPacket.TrailStyle style = trailStyle == null
                ? TechniqueVfxPacket.TrailStyle.DEFAULT : trailStyle;
        if (style == TechniqueVfxPacket.TrailStyle.NONE) {
            return;
        }
        RegistryObject<? extends LodestoneWorldParticleType> particle = switch (style) {
            case SWORD_THIN, FLYING_SWORD_ORBIT -> ModParticles.METAL_SPARK;
            case HEAVY_WEAPON -> ModParticles.EARTH_DUST;
            case TALISMAN_ASH -> ModParticles.FIRE_EMBER;
            case BLOOD_RIBBON -> ModParticles.BLOOD_MIST;
            case THUNDER_JAGGED -> ModParticles.THUNDER_ARC;
            case SOUL_AFTERIMAGE -> ModParticles.SOUL_WISPS;
            case MOVEMENT_WIND -> ModParticles.QI_SOFT;
            case DEFAULT -> sword ? ModParticles.METAL_SPARK : ModParticles.FIRE_EMBER;
            case NONE -> ModParticles.QI_SOFT;
        };
        float trailScale = switch (style) {
            case SWORD_THIN -> 0.72F;
            case HEAVY_WEAPON -> 1.30F;
            case BLOOD_RIBBON, THUNDER_JAGGED -> 1.12F;
            default -> 1.0F;
        };
        Vec3 movement = entity.getDeltaMovement();
        Vec3 direction = normalized(movement, new Vec3(0.0D, 0.0D, 1.0D));
        Vec3 center = entity.position().add(0.0D, entity.getBbHeight() * 0.5D, 0.0D);
        int profileHash = profileKey == null ? 0 : profileKey.hashCode();
        double angle = (entity.tickCount + entity.getId() * 3L + profileHash * 0.001D) * 0.48D;
        Vec3 side = perpendicular(direction);
        Vec3 up = normalized(side.cross(direction), new Vec3(0.0D, 1.0D, 0.0D));
        double coilRadius = (sword ? 0.08D : 0.13D) * trailScale;
        Vec3 coil = side.scale(Math.cos(angle) * coilRadius)
                .add(up.scale(Math.sin(angle) * coilRadius));
        Vec3 tail = center.subtract(direction.scale((sword ? 0.38D : 0.22D) * trailScale)).add(coil);
        Random random = new Random(entity.getId() * 31L + entity.tickCount + profileHash);
        spawn(level, particle, family, tail, direction.scale(-0.018D),
                (sword ? 0.12F : 0.22F) * trailScale,
                0.78F, sword ? 10 : 16, (float) angle);
        if (!sword && (entity.tickCount & 1) == 0) {
            spawn(level, LodestoneParticleRegistry.SPARKLE_PARTICLE, family, center.add(coil.scale(-0.7D)),
                    direction.scale(-0.01D), 0.12F, 0.88F, 11, random.nextFloat());
        }
    }

    private static void ring(ClientLevel level, TechniqueVfxPalette.Family family, Vec3 center,
                             double radius, int points, Random random, float scale, int lifetime) {
        int samples = budgetedSamples(points);
        for (int sample = 0; sample < samples && budgetAvailable(level); sample++) {
            int index = ringSampleIndex(level, sample, samples, points);
            double angle = Math.PI * 2.0D * index / points;
            Vec3 point = center.add(Math.cos(angle) * radius, 0.0D, Math.sin(angle) * radius);
            spawn(level, index % 5 == 0 ? LodestoneParticleRegistry.TWINKLE_PARTICLE
                            : LodestoneParticleRegistry.SPARKLE_PARTICLE,
                    family, point, new Vec3(0.0D, 0.004D, 0.0D), scale, 0.78F, lifetime, (float) angle);
        }
    }

    private static void rotatingRing(ClientLevel level, TechniqueVfxPalette.Family family, Vec3 center,
                                     double radius, int points, double phase, Random random) {
        int samples = budgetedSamples(points);
        for (int sample = 0; sample < samples && budgetAvailable(level); sample++) {
            int index = ringSampleIndex(level, sample, samples, points);
            double angle = phase + Math.PI * 2.0D * index / points;
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
        int samples = budgetedSamples(points);
        for (int sample = 0; sample < samples && budgetAvailable(level); sample++) {
            int index = ringSampleIndex(level, sample, samples, points);
            double angle = Math.PI * 2.0D * index / points;
            Vec3 point = center.add(axisA.scale(Math.cos(angle) * radius))
                    .add(axisB.scale(Math.sin(angle) * radius));
            spawn(level, index % 5 == 0 ? LodestoneParticleRegistry.STAR_PARTICLE
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
        int samples = budgetedSamples(points + 1);
        for (int sample = 0; sample < samples && budgetAvailable(level); sample++) {
            double progress = lineSampleProgress(sample, samples);
            double angle = directionSign * progress * Math.PI * 6.0D;
            Vec3 coil = side.scale(Math.cos(angle) * helixRadius)
                    .add(up.scale(Math.sin(angle) * helixRadius));
            Vec3 point = start.add(delta.scale(progress)).add(coil);
            spawn(level, sample % 4 == 0 ? LodestoneParticleRegistry.TWINKLE_PARTICLE
                            : LodestoneParticleRegistry.SPARKLE_PARTICLE,
                    family, point, direction.scale(0.008D), 0.12F, 0.76F, 18, (float) angle);
        }
    }

    private static void shortLine(ClientLevel level, TechniqueVfxPalette.Family family, Vec3 start, Vec3 end,
                                  int points, Random random) {
        Vec3 delta = end.subtract(start);
        Vec3 motion = normalized(delta, new Vec3(0.0D, 0.0D, 1.0D)).scale(0.003D);
        int samples = budgetedSamples(Math.max(2, points + 1));
        for (int sample = 0; sample < samples && budgetAvailable(level); sample++) {
            Vec3 point = start.add(delta.scale(lineSampleProgress(sample, samples)));
            spawn(level, LodestoneParticleRegistry.THIN_EXTRUDING_SPARK_PARTICLE, family, point,
                    motion, 0.11F, 0.72F, 18, random.nextFloat());
        }
    }

    private static int budgetedSamples(int requested) {
        int available = activeEmissionBudget == null ? requested : activeEmissionBudget.remaining;
        return Math.max(0, Math.min(Math.max(0, requested), available));
    }

    private static int ringSampleIndex(ClientLevel level, int sample, int samples, int points) {
        int offsetRange = Math.max(1, (points + samples - 1) / Math.max(1, samples));
        int offset = Math.floorMod((int) level.getGameTime(), offsetRange);
        return Math.floorMod(sample * points / Math.max(1, samples) + offset, points);
    }

    private static double lineSampleProgress(int sample, int samples) {
        return samples <= 1 ? 0.5D : sample / (double) (samples - 1);
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
        if (!ClientVisualEngine.claimParticle(level)) {
            return false;
        }
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
        if (!ClientVisualEngine.claimPostEffect()) {
            return false;
        }
        shakesThisTick++;
        return true;
    }

    private static PaletteColors paletteColors(TechniqueVfxPalette.Family family) {
        if (activePaletteOverride != null) {
            return activePaletteOverride;
        }
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

    private record ParticleEmission(
            RegistryObject<? extends LodestoneWorldParticleType> particle,
            Vec3 point,
            Vec3 motion,
            float scale,
            float alpha,
            int lifetime
    ) {}

    private record PaletteColors(float startR, float startG, float startB,
                                 float endR, float endG, float endB) {
        private static PaletteColors fromArgb(int argb) {
            return fromArgb(argb, 0);
        }

        /**
         * Builds a start→end gradient. When {@code secondaryArgb} is non-zero it is used
         * as the END color (authored secondary); otherwise END is a soft fade of START
         * (legacy single-color path).
         */
        private static PaletteColors fromArgb(int primaryArgb, int secondaryArgb) {
            if (primaryArgb == 0) {
                return null;
            }
            float r = ((primaryArgb >>> 16) & 0xFF) / 255.0F;
            float g = ((primaryArgb >>> 8) & 0xFF) / 255.0F;
            float b = (primaryArgb & 0xFF) / 255.0F;
            if (secondaryArgb != 0 && secondaryArgb != primaryArgb) {
                float er = ((secondaryArgb >>> 16) & 0xFF) / 255.0F;
                float eg = ((secondaryArgb >>> 8) & 0xFF) / 255.0F;
                float eb = (secondaryArgb & 0xFF) / 255.0F;
                return new PaletteColors(r, g, b, er, eg, eb);
            }
            return new PaletteColors(r, g, b,
                    Math.min(1.0F, r * 0.68F + 0.32F),
                    Math.min(1.0F, g * 0.68F + 0.32F),
                    Math.min(1.0F, b * 0.68F + 0.32F));
        }
    }
}
