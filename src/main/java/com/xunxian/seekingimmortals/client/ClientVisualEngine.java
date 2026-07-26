package com.xunxian.seekingimmortals.client;

import com.xunxian.seekingimmortals.artifact.AuthoredArtifactVfxCatalog;
import com.xunxian.seekingimmortals.item.AuthoredConsumableVfxCatalog;
import com.xunxian.seekingimmortals.network.TechniqueVfxPacket;
import com.xunxian.seekingimmortals.network.VisualEventPacket;
import com.xunxian.seekingimmortals.skill.effect.AuthoredTechniqueVfxCatalog;
import com.xunxian.seekingimmortals.skill.effect.TechniqueVfxOrchestrator;
import com.xunxian.seekingimmortals.skill.effect.TechniqueVfxPalette;
import com.xunxian.seekingimmortals.visual.AuthoredVisualCatalog;
import com.xunxian.seekingimmortals.visual.VisualProfile;
import com.xunxian.seekingimmortals.visual.VisualTimelineEvent;
import com.xunxian.seekingimmortals.visual.VisualTimelinePlan;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderLevelStageEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Client-owned lifecycle registry, anchor resolver, scheduler, and shared VFX budgets. */
@OnlyIn(Dist.CLIENT)
public final class ClientVisualEngine {
    public static final int PARTICLES_ALL = 192;
    public static final int PARTICLES_DECREASED = 112;
    public static final int PARTICLES_MINIMAL = 48;
    public static final int GEOMETRY_ALL = 48;
    public static final int GEOMETRY_DECREASED = 24;
    public static final int GEOMETRY_MINIMAL = 10;
    public static final int VISIBLE_INSTANCE_LIMIT = 96;
    public static final int HARD_INSTANCE_LIMIT = 256;
    public static final int POST_EFFECT_LIMIT = 4;

    private static final int ANCHOR_GRACE_TICKS = 40;
    private static final double MAX_VIEW_DISTANCE_SQR = 96.0D * 96.0D;
    private static final long GOLDEN_SEED = 0x9E3779B97F4A7C15L;
    private static final Map<String, ActiveInstance> INSTANCES = new LinkedHashMap<>();

    private static long nextSequence;
    private static long particleBudgetTick = Long.MIN_VALUE;
    private static int particlesUsed;
    private static int geometryUsed;
    private static int postEffectsUsed;
    private static ClientLevel activeLevel;

    private ClientVisualEngine() {}

    public static void handle(VisualEventPacket packet) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (packet == null || level == null || minecraft.player == null) {
            return;
        }
        ensureLevel(level);
        if (packet.lifecycle() == VisualEventPacket.Lifecycle.EVENT) {
            handleTimelineEvent(level, packet);
            return;
        }
        if (packet.lifecycle() == VisualEventPacket.Lifecycle.STOP) {
            ActiveInstance removed = INSTANCES.remove(packet.instanceKey());
            emitStop(level, packet, removed);
            return;
        }
        if (packet.ageTicks() >= packet.durationTicks()) {
            INSTANCES.remove(packet.instanceKey());
            return;
        }

        ActiveInstance current = INSTANCES.get(packet.instanceKey());
        if (current == null) {
            if (!makeRoom(packet.priority())) {
                return;
            }
            INSTANCES.put(packet.instanceKey(), new ActiveInstance(packet, nextSequence++));
        } else {
            current.update(packet);
        }
    }

    private static void handleTimelineEvent(ClientLevel level, VisualEventPacket packet) {
        VisualTimelinePlan.Plan plan = timelinePlan(packet, false);
        if (plan.isEmpty()) {
            emit(level, packet, packet.ageTicks(), packetPosition(packet));
            return;
        }
        if (!makeRoom(packet.priority())) {
            return;
        }
        long sequence = nextSequence++;
        ActiveInstance instance = new ActiveInstance(packet, sequence, true, plan);
        String key = "@timeline/" + sequence;
        Vec3 anchor = resolveInstanceAnchor(level, instance);
        if (anchor != null) {
            emitInstance(level, instance, anchor, Minecraft.getInstance().options.particles().get());
        }
        instance.age++;
        if (!instance.expired()) {
            INSTANCES.put(key, instance);
        }
    }

    public static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || minecraft.player == null) {
            return;
        }
        ensureLevel(level);
        if (minecraft.isPaused()) {
            return;
        }
        prepareParticleBudget(level.getGameTime());
        if (INSTANCES.isEmpty()) {
            return;
        }

        Vec3 viewer = minecraft.player.position();
        List<VisibleInstance> visible = new ArrayList<>();
        List<String> expired = new ArrayList<>();
        for (Map.Entry<String, ActiveInstance> entry : INSTANCES.entrySet()) {
            ActiveInstance instance = entry.getValue();
            if (instance.expired()) {
                expired.add(entry.getKey());
                continue;
            }
            Vec3 anchor = resolveInstanceAnchor(level, instance);
            if (anchor == null) {
                instance.missingAnchorTicks++;
                if (instance.missingAnchorTicks > ANCHOR_GRACE_TICKS) {
                    expired.add(entry.getKey());
                }
                continue;
            }
            instance.missingAnchorTicks = 0;
            double distanceSqr = viewer.distanceToSqr(anchor);
            if (distanceSqr <= MAX_VIEW_DISTANCE_SQR) {
                visible.add(new VisibleInstance(instance, anchor, distanceSqr));
            }
        }

        visible.sort(Comparator
                .comparingInt((VisibleInstance value) -> value.instance.packet.priority()).reversed()
                .thenComparingDouble(VisibleInstance::distanceSqr)
                .thenComparingLong(value -> value.instance.sequence));
        ParticleStatus status = minecraft.options.particles().get();
        int count = Math.min(VISIBLE_INSTANCE_LIMIT, visible.size());
        for (int index = 0; index < count; index++) {
            VisibleInstance value = visible.get(index);
            ActiveInstance instance = value.instance;
            emitInstance(level, instance, value.anchor, status);
        }
        for (ActiveInstance instance : INSTANCES.values()) {
            instance.age++;
        }
        expired.forEach(INSTANCES::remove);
    }

    /** Starts per-frame geometry/post budgets before the Lodestone world pass. */
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            geometryUsed = 0;
            postEffectsUsed = 0;
        }
    }

    public static void reset() {
        clearRuntimeState();
        activeLevel = null;
    }

    private static void ensureLevel(ClientLevel level) {
        if (activeLevel == level) {
            return;
        }
        clearRuntimeState();
        activeLevel = level;
    }

    private static void clearRuntimeState() {
        INSTANCES.clear();
        nextSequence = 0L;
        particleBudgetTick = Long.MIN_VALUE;
        particlesUsed = 0;
        geometryUsed = 0;
        postEffectsUsed = 0;
        ClientVisualOverlayRuntime.reset();
        ClientModelAnimationRuntime.reset();
    }

    static int remainingParticleBudget(ClientLevel level) {
        prepareParticleBudget(level.getGameTime());
        return Math.max(0, particleLimit(Minecraft.getInstance().options.particles().get()) - particlesUsed);
    }

    static int particlesUsed(ClientLevel level) {
        prepareParticleBudget(level.getGameTime());
        return particlesUsed;
    }

    static boolean claimParticle(ClientLevel level) {
        prepareParticleBudget(level.getGameTime());
        if (particlesUsed >= particleLimit(Minecraft.getInstance().options.particles().get())) {
            return false;
        }
        particlesUsed++;
        return true;
    }

    static boolean claimGeometry(ParticleStatus status) {
        if (geometryUsed >= geometryLimit(status)) {
            return false;
        }
        geometryUsed++;
        return true;
    }

    static boolean geometryAvailable(ParticleStatus status) {
        return geometryUsed < geometryLimit(status);
    }

    static boolean claimPostEffect() {
        if (postEffectsUsed >= POST_EFFECT_LIMIT) {
            return false;
        }
        postEffectsUsed++;
        return true;
    }

    static int particleLimit(ParticleStatus status) {
        return status == ParticleStatus.MINIMAL ? PARTICLES_MINIMAL
                : status == ParticleStatus.DECREASED ? PARTICLES_DECREASED : PARTICLES_ALL;
    }

    static int geometryLimit(ParticleStatus status) {
        return status == ParticleStatus.MINIMAL ? GEOMETRY_MINIMAL
                : status == ParticleStatus.DECREASED ? GEOMETRY_DECREASED : GEOMETRY_ALL;
    }

    static int activeInstanceCount() {
        return INSTANCES.size();
    }

    private static void prepareParticleBudget(long tick) {
        if (particleBudgetTick != tick) {
            particleBudgetTick = tick;
            particlesUsed = 0;
        }
    }

    private static boolean makeRoom(int incomingPriority) {
        if (INSTANCES.size() < HARD_INSTANCE_LIMIT) {
            return true;
        }
        Map.Entry<String, ActiveInstance> candidate = INSTANCES.entrySet().stream()
                .min(Comparator
                        .comparingInt((Map.Entry<String, ActiveInstance> entry) ->
                                entry.getValue().packet.priority())
                        .thenComparingLong(entry -> entry.getValue().sequence))
                .orElse(null);
        if (candidate == null || candidate.getValue().packet.priority() > incomingPriority) {
            return false;
        }
        INSTANCES.remove(candidate.getKey());
        return true;
    }

    private static void emitStop(ClientLevel level, VisualEventPacket stop, ActiveInstance removed) {
        VisualEventPacket source = removed == null ? stop : removed.packet;
        Vec3 anchor = resolveAnchor(level, stop);
        if (anchor == null) {
            anchor = resolveAnchor(level, source);
        }
        if (anchor == null) {
            anchor = packetPosition(stop);
        }
        if (anchor == null) {
            return;
        }
        VisualEventPacket dissipate = new VisualEventPacket(
                source.domain(), source.profileKey(), VisualEventPacket.Lifecycle.STOP, "dissipate",
                stop.anchorType(), stop.entityId(), stop.blockPos(),
                anchor.x, anchor.y, anchor.z,
                stop.targetX(), stop.targetY(), stop.targetZ(), stop.instanceKey(),
                Math.max(1, source.durationTicks()), 0, source.scale(),
                Math.max(8, source.intensity() / 2), stop.seed(), source.priority());
        emit(level, dissipate, 0, anchor);
    }

    private static void emit(ClientLevel level, VisualEventPacket packet, int age) {
        Vec3 anchor = resolveAnchor(level, packet);
        if (anchor != null) {
            emit(level, packet, age, anchor);
        }
    }

    private static Vec3 packetPosition(VisualEventPacket packet) {
        if (packet == null || !Double.isFinite(packet.x())
                || !Double.isFinite(packet.y()) || !Double.isFinite(packet.z())) {
            return null;
        }
        return new Vec3(packet.x(), packet.y(), packet.z());
    }

    private static Vec3 resolveInstanceAnchor(ClientLevel level, ActiveInstance instance) {
        Vec3 anchor = resolveAnchor(level, instance.packet);
        if (anchor == null && instance.transientTimeline) {
            return packetPosition(instance.packet);
        }
        return anchor;
    }

    private static void emitInstance(ClientLevel level, ActiveInstance instance,
                                     Vec3 anchor, ParticleStatus status) {
        List<VisualTimelinePlan.Entry> active = instance.timeline.activeAt(instance.age);
        if (!instance.timeline.isEmpty()) {
            for (VisualTimelinePlan.Entry entry : active) {
                if (instance.shouldEmit(entry, status)) {
                    emit(level, instance.packet, instance.age, anchor, entry.event());
                    instance.markEmitted(entry);
                }
            }
            return;
        }
        if (!instance.transientTimeline && instance.shouldEmit(status)) {
            emit(level, instance.packet, instance.age, anchor);
            instance.lastEmissionAge = instance.age;
        }
    }

    private static VisualTimelinePlan.Plan timelinePlan(VisualEventPacket packet, boolean looping) {
        return VisualTimelinePlan.select(resolveUnifiedProfile(packet), packet.trigger(), looping);
    }

    private static void emit(ClientLevel level, VisualEventPacket packet, int age, Vec3 anchor) {
        emit(level, packet, age, anchor, null);
    }

    private static void emit(ClientLevel level, VisualEventPacket packet, int age, Vec3 anchor,
                             VisualTimelineEvent timelineEvent) {
        if (anchor == null) {
            return;
        }
        ResolvedStyle baseStyle = resolveStyle(packet);
        ResolvedStyle style = timelineEvent == null
                ? baseStyle : applyTimelineStyle(baseStyle, timelineEvent);
        if (timelineEvent != null && timelineEvent.action()
                == com.xunxian.seekingimmortals.visual.VisualAction.SCREEN_OVERLAY) {
            ClientVisualOverlayRuntime.push(
                    style.primaryArgb(), timelineEvent.intensity(), timelineEvent.durationTicks());
        } else if (timelineEvent != null && timelineEvent.action()
                == com.xunxian.seekingimmortals.visual.VisualAction.MODEL_ANIMATION) {
            ClientModelAnimationRuntime.trigger(level, packet, timelineEvent);
        }
        Vec3 target = new Vec3(packet.targetX(), packet.targetY(), packet.targetZ());
        if (!finite(target)) {
            target = anchor;
        }
        if (timelineEvent != null && "TARGET".equals(timelineEvent.anchor()) && finite(target)) {
            anchor = target;
        }
        int interval = emissionInterval(Minecraft.getInstance().options.particles().get(), packet.priority());
        long pulse = Math.max(0, age) / Math.max(1, interval);
        int intensity = packet.intensity();
        if (timelineEvent != null) {
            int authoredBase = Math.max(1, baseStyle.authoredIntensity());
            intensity = Math.max(1, Math.min(VisualEventPacket.MAX_INTENSITY,
                    Math.round(packet.intensity() * (timelineEvent.intensity() / (float) authoredBase))));
        }
        TechniqueVfxPacket styled = new TechniqueVfxPacket(
                style.kind(), style.family(), style.motif(), style.particle(), style.trail(),
                style.telegraphed(),
                anchor.x, anchor.y, anchor.z,
                target.x, target.y, target.z,
                (float) Math.min(32.0D, packet.scale() * style.radiusScale()),
                intensity, packet.seed() ^ (GOLDEN_SEED * (pulse + 1L))
                        ^ (timelineEvent == null ? 0L : ((long) timelineEvent.ordinal() << 32)));
        int eventOrdinal = timelineEvent == null ? -1 : timelineEvent.ordinal();
        LodestoneTechniqueVfx.handleProfile(packet.profileKey(), styled, style.primaryArgb(), eventOrdinal);
    }

    private static ResolvedStyle applyTimelineStyle(ResolvedStyle base,
                                                    VisualTimelineEvent event) {
        TechniqueVfxPacket.Kind kind = switch (event.action()) {
            case AURA -> TechniqueVfxPacket.Kind.AURA;
            case RIBBON -> TechniqueVfxPacket.Kind.PATH;
            case FLASH -> TechniqueVfxPacket.Kind.IMPACT;
            case DISSIPATE -> TechniqueVfxPacket.Kind.DISSIPATE;
            case BURST -> TechniqueVfxPacket.Kind.BURST;
            case SCREEN_OVERLAY -> TechniqueVfxPacket.Kind.SCAN;
            case EMITTER -> switch (event.trigger()) {
                case FORMATION -> TechniqueVfxPacket.Kind.FORMATION;
                case STATE -> TechniqueVfxPacket.Kind.STATUS;
                default -> TechniqueVfxPacket.Kind.BURST;
            };
            case STATE_TRANSITION -> TechniqueVfxPacket.Kind.STATUS;
            case MODEL_ANIMATION -> TechniqueVfxPacket.Kind.STATUS;
        };
        return new ResolvedStyle(
                base.family(), base.motif(), kind,
                TechniqueVfxPacket.ParticleStyle.fromAuthorRef(event.particle()),
                TechniqueVfxPacket.TrailStyle.fromAuthorRef(event.trail()),
                base.telegraphed(), (float) event.radius(), base.primaryArgb(),
                base.authoredIntensity());
    }

    private static Vec3 resolveAnchor(ClientLevel level, VisualEventPacket packet) {
        return switch (packet.anchorType()) {
            case WORLD -> new Vec3(packet.x(), packet.y(), packet.z());
            case BLOCK -> Vec3.atCenterOf(BlockPos.of(packet.blockPos()));
            case ENTITY -> {
                Entity entity = level.getEntity(packet.entityId());
                yield entity == null || entity.isRemoved() ? null
                        : entity.position().add(0.0D, entity.getBbHeight() * 0.5D, 0.0D);
            }
        };
    }

    private static ResolvedStyle resolveStyle(VisualEventPacket packet) {
        String id = packet.profileKey().getPath();
        String domain = normalizeDomain(packet.domain());
        VisualProfile unified = resolveUnifiedProfile(packet);
        if (unified != null) {
            TechniqueVfxPalette.Family family = enumValue(
                    TechniqueVfxPalette.Family.class, unified.family(), TechniqueVfxPalette.Family.NEUTRAL);
            TechniqueVfxPacket.Motif motif = enumValue(
                    TechniqueVfxPacket.Motif.class, unified.motif(), TechniqueVfxPacket.Motif.GENERIC);
            return new ResolvedStyle(family, motif, kindFor(packet, defaultKind(packet, motif)),
                    TechniqueVfxPacket.ParticleStyle.fromAuthorRef(unified.particle()),
                    TechniqueVfxPacket.TrailStyle.fromAuthorRef(unified.trail()),
                    unified.telegraphed(), (float) unified.radius(), unified.primaryArgbInt(),
                    unified.intensity());
        }
        if ("artifact".equals(domain)) {
            AuthoredArtifactVfxCatalog.Profile profile = AuthoredArtifactVfxCatalog.find(id).orElse(null);
            if (profile != null) {
                return new ResolvedStyle(profile.family(), profile.motif(),
                        kindFor(packet, TechniqueVfxPacket.Kind.STATUS),
                        profile.particle(), profile.trail(), profile.telegraphed(), 1.0F, 0,
                        packet.intensity());
            }
        }
        if ("pill".equals(domain) || "consumable".equals(domain)) {
            AuthoredConsumableVfxCatalog.Profile profile = "pill".equals(domain)
                    ? AuthoredConsumableVfxCatalog.findPill(id).orElse(null)
                    : AuthoredConsumableVfxCatalog.findConsumable(id).orElse(null);
            if (profile != null) {
                return new ResolvedStyle(profile.family(), profile.motif(),
                        kindFor(packet, profile.vfxKind()), profile.particle(), profile.trail(),
                        profile.telegraphed(), (float) profile.radius(), 0, packet.intensity());
            }
        }
        if ("technique".equals(domain)) {
            AuthoredTechniqueVfxCatalog.Profile profile = AuthoredTechniqueVfxCatalog.find(id).orElse(null);
            if (profile != null) {
                TechniqueVfxOrchestrator.VisualPlan plan = TechniqueVfxOrchestrator.plan(
                        profile.id(), profile.effectType(), profile.element(), "", Set.of(),
                        "", "", "", profile.shape(), profile.school(), false);
                return new ResolvedStyle(plan.family(), plan.motif(), kindFor(packet, plan.kind()),
                        plan.particleStyle(), plan.trailStyle(), plan.telegraphed(), 1.0F, 0,
                        packet.intensity());
            }
        }
        TechniqueVfxPacket.Motif motif = switch (domain) {
            case "formation" -> TechniqueVfxPacket.Motif.FORMATION;
            case "hazard" -> TechniqueVfxPacket.Motif.DOMAIN;
            case "entity" -> TechniqueVfxPacket.Motif.PROJECTILE;
            default -> TechniqueVfxPacket.Motif.GENERIC;
        };
        TechniqueVfxPacket.Kind fallback = packet.persistent()
                ? TechniqueVfxPacket.Kind.STATUS : TechniqueVfxPacket.Kind.BURST;
        return new ResolvedStyle(TechniqueVfxPalette.Family.NEUTRAL, motif,
                kindFor(packet, fallback), TechniqueVfxPacket.ParticleStyle.DEFAULT,
                TechniqueVfxPacket.TrailStyle.DEFAULT, false, 1.0F, 0, packet.intensity());
    }

    private static VisualProfile resolveUnifiedProfile(VisualEventPacket packet) {
        String id = packet.profileKey().getPath();
        String domain = normalizeDomain(packet.domain());
        VisualProfile profile = AuthoredVisualCatalog.resolve(domain + ":" + id).orElse(null);
        return profile != null ? profile
                : AuthoredVisualCatalog.resolve(packet.profileKey().toString()).orElse(null);
    }

    private static TechniqueVfxPacket.Kind defaultKind(VisualEventPacket packet,
                                                       TechniqueVfxPacket.Motif motif) {
        if (packet.persistent()) {
            return motif == TechniqueVfxPacket.Motif.FORMATION
                    || motif == TechniqueVfxPacket.Motif.DOMAIN
                    ? TechniqueVfxPacket.Kind.FORMATION : TechniqueVfxPacket.Kind.STATUS;
        }
        return TechniqueVfxPacket.Kind.BURST;
    }

    private static <E extends Enum<E>> E enumValue(Class<E> type, String value, E fallback) {
        try {
            return Enum.valueOf(type, value == null ? "" : value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }

    private static TechniqueVfxPacket.Kind kindFor(VisualEventPacket packet,
                                                    TechniqueVfxPacket.Kind fallback) {
        if (packet.lifecycle() == VisualEventPacket.Lifecycle.STOP) {
            return TechniqueVfxPacket.Kind.DISSIPATE;
        }
        String trigger = packet.trigger().trim().toUpperCase(Locale.ROOT)
                .replace('-', '_').replace(' ', '_');
        try {
            return TechniqueVfxPacket.Kind.valueOf(trigger);
        } catch (IllegalArgumentException ignored) {
            return switch (trigger) {
                case "HIT", "COLLIDE" -> TechniqueVfxPacket.Kind.IMPACT;
                case "OPEN", "ACTIVATE", "AWAKEN", "RELEASE" -> TechniqueVfxPacket.Kind.BURST;
                case "IDLE", "ACTIVE", "SUSTAIN", "APPLY" -> TechniqueVfxPacket.Kind.STATUS;
                default -> fallback == null ? TechniqueVfxPacket.Kind.BURST : fallback;
            };
        }
    }

    private static String normalizeDomain(String domain) {
        String value = domain == null ? "" : domain.trim().toLowerCase(Locale.ROOT);
        int separator = value.lastIndexOf(':');
        return separator >= 0 && separator + 1 < value.length() ? value.substring(separator + 1) : value;
    }

    private static int emissionInterval(ParticleStatus status, int priority) {
        int base = status == ParticleStatus.MINIMAL ? 20
                : status == ParticleStatus.DECREASED ? 12 : 8;
        return priority >= 2 ? Math.max(4, base / 2) : base;
    }

    private static boolean finite(Vec3 value) {
        return Double.isFinite(value.x) && Double.isFinite(value.y) && Double.isFinite(value.z);
    }

    private static final class ActiveInstance {
        private VisualEventPacket packet;
        private final long sequence;
        private final boolean transientTimeline;
        private VisualTimelinePlan.Plan timeline;
        private final Map<Integer, Integer> timelineEmissionAges = new LinkedHashMap<>();
        private int age;
        private int lastEmissionAge = Integer.MIN_VALUE;
        private int missingAnchorTicks;

        private ActiveInstance(VisualEventPacket packet, long sequence) {
            this(packet, sequence, false, timelinePlan(packet, true));
        }

        private ActiveInstance(VisualEventPacket packet, long sequence,
                               boolean transientTimeline, VisualTimelinePlan.Plan timeline) {
            this.packet = packet;
            this.sequence = sequence;
            this.transientTimeline = transientTimeline;
            this.timeline = timeline == null
                    ? VisualTimelinePlan.Plan.empty(!transientTimeline) : timeline;
            this.age = transientTimeline ? 0 : packet.ageTicks();
        }

        private void update(VisualEventPacket update) {
            boolean triggerChanged = !packet.trigger().equals(update.trigger())
                    || !packet.profileKey().equals(update.profileKey());
            boolean ageReset = update.ageTicks() < age;
            VisualTimelinePlan.Plan nextTimeline = timelinePlan(update, true);
            boolean timelineChanged = !timeline.equals(nextTimeline);
            packet = update;
            timeline = nextTimeline;
            age = update.ageTicks();
            missingAnchorTicks = 0;
            if (triggerChanged || ageReset || timelineChanged) {
                lastEmissionAge = Integer.MIN_VALUE;
                timelineEmissionAges.clear();
            }
        }

        private boolean shouldEmit(VisualTimelinePlan.Entry entry, ParticleStatus status) {
            Integer lastAge = timelineEmissionAges.get(entry.event().ordinal());
            return lastAge == null
                    || age - lastAge >= emissionInterval(status, packet.priority());
        }

        private void markEmitted(VisualTimelinePlan.Entry entry) {
            timelineEmissionAges.put(entry.event().ordinal(), age);
        }

        private boolean shouldEmit(ParticleStatus status) {
            if (lastEmissionAge == Integer.MIN_VALUE) {
                return true;
            }
            return age - lastEmissionAge >= emissionInterval(status, packet.priority());
        }

        private boolean expired() {
            return transientTimeline ? timeline.expired(age) : age >= packet.durationTicks();
        }
    }

    private record VisibleInstance(ActiveInstance instance, Vec3 anchor, double distanceSqr) {}

    private record ResolvedStyle(
            TechniqueVfxPalette.Family family,
            TechniqueVfxPacket.Motif motif,
            TechniqueVfxPacket.Kind kind,
            TechniqueVfxPacket.ParticleStyle particle,
            TechniqueVfxPacket.TrailStyle trail,
            boolean telegraphed,
            float radiusScale,
            int primaryArgb,
            int authoredIntensity
    ) {}
}
