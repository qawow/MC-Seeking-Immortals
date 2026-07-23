package com.xunxian.seekingimmortals.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.xunxian.seekingimmortals.SeekingImmortalsMod;
import com.xunxian.seekingimmortals.entity.CultivationFireballEntity;
import com.xunxian.seekingimmortals.entity.SwordProjectileEntity;
import com.xunxian.seekingimmortals.network.TechniqueVfxPacket;
import com.xunxian.seekingimmortals.skill.effect.TechniqueVfxPalette;
import com.xunxian.seekingimmortals.visual.AuthoredVisualCatalog;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import org.joml.Vector3f;
import team.lodestar.lodestone.registry.client.LodestoneRenderTypeRegistry;
import team.lodestar.lodestone.systems.rendering.LodestoneRenderType;
import team.lodestar.lodestone.systems.rendering.VFXBuilders;
import team.lodestar.lodestone.systems.rendering.rendeertype.RenderTypeToken;
import team.lodestar.lodestone.systems.rendering.trail.TrailPoint;
import team.lodestar.lodestone.systems.rendering.trail.TrailPointBuilder;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
final class LodestoneWorldGeometry {
    private static final int MAX_TRACKED_PROJECTILES = 96;
    private static final int MAX_TRANSIENT_BEAMS = 40;
    private static final int MAX_TRAIL_POINTS = 16;
    private static final int PROJECTILE_AFTERGLOW_TICKS = 7;
    private static final double MAX_GEOMETRY_DISTANCE_SQR = 96.0D * 96.0D;
    private static final double MIN_SAMPLE_DISTANCE_SQR = 0.035D * 0.035D;
    private static final ResourceLocation BEAM_TEXTURE =
            new ResourceLocation(SeekingImmortalsMod.MODID, "textures/effect/beam_soft.png");
    private static final LodestoneRenderType BEAM_RENDER_TYPE =
            LodestoneRenderTypeRegistry.ADDITIVE_TEXTURE.applyAndCache(
                    RenderTypeToken.createCachedToken(BEAM_TEXTURE));

    private static final Map<Integer, ProjectileTrail> PROJECTILE_TRAILS = new LinkedHashMap<>();
    private static final List<TransientBeam> TRANSIENT_BEAMS = new ArrayList<>();
    private static int projectileTickCursor;
    private static int renderCursor;

    private LodestoneWorldGeometry() {}

    static void track(Entity entity) {
        if (!(entity instanceof CultivationFireballEntity) && !(entity instanceof SwordProjectileEntity)) {
            return;
        }
        if (PROJECTILE_TRAILS.size() >= MAX_TRACKED_PROJECTILES
                && !PROJECTILE_TRAILS.containsKey(entity.getId())) {
            Integer oldest = PROJECTILE_TRAILS.keySet().iterator().next();
            PROJECTILE_TRAILS.remove(oldest);
        }
        PROJECTILE_TRAILS.put(entity.getId(), new ProjectileTrail(
                entity, entity instanceof SwordProjectileEntity));
    }

    static void untrack(Entity entity) {
        ProjectileTrail trail = PROJECTILE_TRAILS.get(entity.getId());
        if (trail != null && trail.entity == entity) {
            trail.detach(entity.position().add(0.0D, entity.getBbHeight() * 0.5D, 0.0D));
        }
    }

    static void addIntent(TechniqueVfxPacket packet, int anticipationTicks, int releaseTicks,
                          int sustainTicks, int afterglowTicks) {
        addProfileIntent(null, packet, 0, anticipationTicks, releaseTicks, sustainTicks, afterglowTicks);
    }

    static void addProfileIntent(ResourceLocation profileKey, TechniqueVfxPacket packet,
                                 int primaryArgb,
                                 int anticipationTicks, int releaseTicks,
                                 int sustainTicks, int afterglowTicks) {
        Vec3 start = new Vec3(packet.x(), packet.y(), packet.z());
        Vec3 end = new Vec3(packet.endX(), packet.endY(), packet.endZ());
        if (!usesWorldBeam(packet) || !finite(start) || !finite(end)
                || start.distanceToSqr(end) < 0.04D) {
            return;
        }
        if (TRANSIENT_BEAMS.size() >= MAX_TRANSIENT_BEAMS) {
            TRANSIENT_BEAMS.remove(0);
        }
        int totalTicks = Mth.clamp(
                anticipationTicks + releaseTicks + sustainTicks + afterglowTicks, 4, 60);
        float trailScale = trailWidthScale(packet.trailStyle());
        long profileSeed = packet.seed() ^ (profileKey == null ? 0L : profileKey.hashCode());
        TRANSIENT_BEAMS.add(new TransientBeam(
                profileKey, primaryArgb, start, end, packet.family(), packet.kind(), packet.motif(),
                Math.max(0.035F, Math.min(0.34F, packet.radius() * 0.055F * trailScale)),
                Math.max(1, anticipationTicks),
                Math.max(1, anticipationTicks + releaseTicks),
                Math.max(1, anticipationTicks + releaseTicks + sustainTicks),
                totalTicks,
                profileSeed));
    }

    static void tick(ClientLevel level) {
        TRANSIENT_BEAMS.removeIf(TransientBeam::tickAndExpired);
        List<Integer> expired = new ArrayList<>();
        for (Map.Entry<Integer, ProjectileTrail> entry : PROJECTILE_TRAILS.entrySet()) {
            ProjectileTrail trail = entry.getValue();
            trail.tick(level);
            if (trail.expired()) {
                expired.add(entry.getKey());
            }
        }
        expired.forEach(PROJECTILE_TRAILS::remove);
        if (PROJECTILE_TRAILS.isEmpty()) {
            projectileTickCursor = 0;
        }
    }

    static List<ProjectileSample> projectileSamples(Minecraft minecraft) {
        if (minecraft.player == null || PROJECTILE_TRAILS.isEmpty()) {
            return List.of();
        }
        ParticleStatus status = minecraft.options.particles().get();
        int limit = status == ParticleStatus.MINIMAL ? 12
                : status == ParticleStatus.DECREASED ? 28 : 56;
        List<ProjectileTrail> trails = new ArrayList<>(PROJECTILE_TRAILS.values());
        List<ProjectileSample> samples = new ArrayList<>(Math.min(limit, trails.size()));
        int size = trails.size();
        int start = Math.floorMod(projectileTickCursor, size);
        int scanned = 0;
        for (int offset = 0; offset < size && samples.size() < limit; offset++) {
            scanned++;
            ProjectileTrail trail = trails.get((start + offset) % size);
            Entity entity = trail.entity;
            if (!trail.detached && entity != null && !entity.isRemoved()
                    && minecraft.player.distanceToSqr(entity) <= 64.0D * 64.0D) {
                samples.add(new ProjectileSample(entity, trail.currentFamily(), trail.currentTrailStyle(),
                        trail.currentProfileKey(), trail.sword));
            }
        }
        projectileTickCursor = (start + Math.max(1, scanned)) % size;
        return samples;
    }

    static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || minecraft.player == null) {
            return;
        }
        ParticleStatus status = minecraft.options.particles().get();
        int geometryLimit = ClientVisualEngine.geometryLimit(status);
        int trailPointLimit = status == ParticleStatus.MINIMAL ? 5
                : status == ParticleStatus.DECREASED ? 9 : MAX_TRAIL_POINTS;
        Vec3 camera = event.getCamera().getPosition();
        float partialTick = event.getPartialTick();
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        poseStack.pushPose();
        int rendered = 0;
        try {
            List<RenderableGeometry> geometry = collectRenderableGeometry(camera);
            if (!geometry.isEmpty()) {
                int start = Math.floorMod(renderCursor, geometry.size());
                for (int offset = 0; offset < geometry.size() && rendered < geometryLimit
                        && ClientVisualEngine.geometryAvailable(status); offset++) {
                    RenderableGeometry renderable = geometry.get((start + offset) % geometry.size());
                    if (renderable.render(poseStack, buffers, camera, partialTick,
                            trailPointLimit, status)) {
                        if (ClientVisualEngine.claimGeometry(status)) {
                            rendered++;
                        }
                    }
                }
                renderCursor = (start + Math.max(1, rendered)) % geometry.size();
            } else {
                renderCursor = 0;
            }
        } finally {
            poseStack.popPose();
            if (rendered > 0) {
                buffers.endBatch(BEAM_RENDER_TYPE);
            }
        }
    }

    static void reset() {
        PROJECTILE_TRAILS.clear();
        TRANSIENT_BEAMS.clear();
        projectileTickCursor = 0;
        renderCursor = 0;
    }

    private static List<RenderableGeometry> collectRenderableGeometry(Vec3 camera) {
        List<RenderableGeometry> geometry = new ArrayList<>(
                TRANSIENT_BEAMS.size() + PROJECTILE_TRAILS.size());
        for (TransientBeam beam : TRANSIENT_BEAMS) {
            if (LodestoneVfxMath.distanceToSegmentSqr(camera, beam.start, beam.end)
                    <= MAX_GEOMETRY_DISTANCE_SQR) {
                geometry.add(new BeamGeometry(beam));
            }
        }
        for (ProjectileTrail trail : PROJECTILE_TRAILS.values()) {
            List<TrailPoint> points = trail.points.getTrailPoints();
            if (points.size() >= 2
                    && LodestoneVfxMath.distanceToSegmentSqr(camera,
                    points.get(0).getPosition(), points.get(points.size() - 1).getPosition())
                    <= MAX_GEOMETRY_DISTANCE_SQR) {
                geometry.add(new TrailGeometry(trail));
            }
        }
        return geometry;
    }

    private static boolean usesWorldBeam(TechniqueVfxPacket packet) {
        if (packet.kind() == TechniqueVfxPacket.Kind.BEAM
                || packet.kind() == TechniqueVfxPacket.Kind.PATH
                || packet.kind() == TechniqueVfxPacket.Kind.CONE) {
            return true;
        }
        return switch (packet.motif()) {
            case PROJECTILE, BLADE, TELEPORT, CHAIN, CHANNEL, CONFUCIAN, GHOST, TALISMAN -> true;
            default -> false;
        };
    }

    private static float trailWidthScale(TechniqueVfxPacket.TrailStyle style) {
        if (style == null) {
            return 1.0F;
        }
        return switch (style) {
            case SWORD_THIN -> 0.72F;
            case HEAVY_WEAPON -> 1.30F;
            case FLYING_SWORD_ORBIT -> 0.88F;
            case TALISMAN_ASH -> 0.78F;
            case BLOOD_RIBBON -> 1.05F;
            case THUNDER_JAGGED -> 1.12F;
            case SOUL_AFTERIMAGE -> 0.82F;
            case MOVEMENT_WIND -> 0.90F;
            case DEFAULT, NONE -> 1.0F;
        };
    }

    private static List<TrailPoint> downsample(List<TrailPoint> points, int limit) {
        if (points.size() <= limit) {
            return points;
        }
        List<TrailPoint> sampled = new ArrayList<>(limit);
        for (int i = 0; i < limit; i++) {
            int index = Math.round(i * (points.size() - 1.0F) / (limit - 1.0F));
            sampled.add(points.get(index));
        }
        return sampled;
    }

    private static VFXBuilders.WorldVFXBuilder builder(MultiBufferSource buffers,
                                                        TechniqueVfxPalette.Family family,
                                                        float alpha,
                                                        int primaryArgb) {
        TechniqueVfxPalette.Profile profile = TechniqueVfxPalette.profile(family.name());
        Vector3f color = profile.core().getColor();
        float red = color.x();
        float green = color.y();
        float blue = color.z();
        if (primaryArgb != 0) {
            red = ((primaryArgb >>> 16) & 0xFF) / 255.0F;
            green = ((primaryArgb >>> 8) & 0xFF) / 255.0F;
            blue = (primaryArgb & 0xFF) / 255.0F;
        }
        return VFXBuilders.createWorld()
                .replaceBufferSource(buffers)
                .setRenderType(BEAM_RENDER_TYPE)
                .setColorRaw(red, green, blue)
                .setAlpha(Mth.clamp(alpha, 0.0F, 1.0F))
                .setLight(0xF000F0)
                .setUV(0.0F, 0.0F, 1.0F, 1.0F);
    }

    private static boolean finite(Vec3 value) {
        return Double.isFinite(value.x) && Double.isFinite(value.y) && Double.isFinite(value.z);
    }

    record ProjectileSample(Entity entity, TechniqueVfxPalette.Family family,
                            TechniqueVfxPacket.TrailStyle trailStyle, String profileKey,
                            boolean sword) {}

    private interface RenderableGeometry {
        boolean render(PoseStack poseStack, MultiBufferSource buffers, Vec3 camera,
                       float partialTick, int trailPointLimit, ParticleStatus status);
    }

    private record BeamGeometry(TransientBeam beam) implements RenderableGeometry {
        @Override
        public boolean render(PoseStack poseStack, MultiBufferSource buffers, Vec3 camera,
                              float partialTick, int trailPointLimit, ParticleStatus status) {
            float alpha = beam.alpha();
            float quality = status == ParticleStatus.MINIMAL ? 0.62F
                    : status == ParticleStatus.DECREASED ? 0.82F : 1.0F;
            float pulse = 0.82F + 0.18F * Mth.sin((beam.age + beam.seed * 0.01F) * 0.72F);
            float width = beam.width * quality * pulse;
            Vec3 localStart = beam.start.subtract(camera);
            Vec3 localEnd = beam.end.subtract(camera);
            Vec3 facingReference = LodestoneVfxMath.beamFacingReference(
                    localStart, localEnd, Vec3.ZERO);
            VFXBuilders.WorldVFXBuilder builder = builder(buffers, beam.family, alpha * quality,
                    beam.primaryArgb);
            builder.renderBeam(poseStack.last().pose(), localStart, localEnd, width, facingReference);
            if (status == ParticleStatus.ALL && width >= 0.065F) {
                TechniqueVfxPalette.Profile profile = TechniqueVfxPalette.profile(beam.family.name());
                Vector3f edge = profile.edge().getColor();
                float edgeR = edge.x();
                float edgeG = edge.y();
                float edgeB = edge.z();
                if (beam.primaryArgb != 0) {
                    edgeR = Math.min(1.0F, (((beam.primaryArgb >>> 16) & 0xFF) / 255.0F) * 0.68F + 0.32F);
                    edgeG = Math.min(1.0F, (((beam.primaryArgb >>> 8) & 0xFF) / 255.0F) * 0.68F + 0.32F);
                    edgeB = Math.min(1.0F, ((beam.primaryArgb & 0xFF) / 255.0F) * 0.68F + 0.32F);
                }
                VFXBuilders.createWorld()
                        .replaceBufferSource(buffers)
                        .setRenderType(BEAM_RENDER_TYPE)
                        .setColorRaw(edgeR, edgeG, edgeB)
                        .setAlpha(alpha * 0.46F)
                        .setLight(0xF000F0)
                        .setUV(0.0F, 0.0F, 1.0F, 1.0F)
                        .renderBeam(poseStack.last().pose(), localStart, localEnd,
                                width * 1.75F, facingReference);
            }
            return true;
        }
    }

    private record TrailGeometry(ProjectileTrail trail) implements RenderableGeometry {
        @Override
        public boolean render(PoseStack poseStack, MultiBufferSource buffers, Vec3 camera,
                              float partialTick, int trailPointLimit, ParticleStatus status) {
            List<TrailPoint> points = trail.renderPoints(partialTick, camera,
                    Math.max(3, trailPointLimit));
            if (points.size() < 2) {
                return false;
            }
            float quality = status == ParticleStatus.MINIMAL ? 0.58F
                    : status == ParticleStatus.DECREASED ? 0.78F : 1.0F;
            float styleWidth = trailWidthScale(trail.currentTrailStyle());
            float baseWidth = (trail.sword ? 0.09F : 0.16F) * styleWidth * quality;
            float alpha = trail.fadeAlpha() * (trail.sword ? 0.82F : 0.72F) * quality;
            VFXBuilders.WorldVFXBuilder builder = builder(buffers, trail.currentFamily(), alpha,
                    trail.currentPrimaryArgb());
            builder.renderTrail(poseStack, points,
                    progress -> baseWidth * (0.18F + progress * 0.82F),
                    progress -> builder.setAlpha(alpha * (0.12F + progress * 0.88F)));
            return true;
        }
    }

    private static final class ProjectileTrail {
        private Entity entity;
        private TechniqueVfxPalette.Family family;
        private TechniqueVfxPacket.TrailStyle trailStyle;
        private String profileKey;
        private int primaryArgb;
        private final boolean sword;
        private final TrailPointBuilder points = TrailPointBuilder.create(MAX_TRAIL_POINTS);
        private Vec3 lastPoint;
        private boolean detached;
        private int fadeTicks = PROJECTILE_AFTERGLOW_TICKS;

        private ProjectileTrail(Entity entity, boolean sword) {
            this.entity = entity;
            this.sword = sword;
            refreshVisualIdentity();
            sample(entity.position().add(0.0D, entity.getBbHeight() * 0.5D, 0.0D));
        }

        private void tick(ClientLevel level) {
            points.tickTrailPoints();
            if (!detached && (entity == null || entity.isRemoved() || entity.level() != level)) {
                detach(lastPoint);
            }
            if (detached) {
                fadeTicks--;
                return;
            }
            refreshVisualIdentity();
            Vec3 center = entity.position().add(0.0D, entity.getBbHeight() * 0.5D, 0.0D);
            sample(center);
        }

        private TechniqueVfxPalette.Family currentFamily() {
            refreshVisualIdentity();
            return family;
        }

        private TechniqueVfxPacket.TrailStyle currentTrailStyle() {
            refreshVisualIdentity();
            return trailStyle == null ? TechniqueVfxPacket.TrailStyle.DEFAULT : trailStyle;
        }

        private String currentProfileKey() {
            refreshVisualIdentity();
            return profileKey;
        }

        private int currentPrimaryArgb() {
            refreshVisualIdentity();
            return primaryArgb;
        }

        private void refreshVisualIdentity() {
            if (entity instanceof CultivationFireballEntity fireball) {
                family = fireball.getVisualFamily();
                trailStyle = fireball.getVisualTrailStyle();
                profileKey = fireball.getVisualProfileId();
            } else if (entity instanceof SwordProjectileEntity swordProjectile) {
                family = swordProjectile.getVisualFamily();
                trailStyle = swordProjectile.getVisualTrailStyle();
                profileKey = swordProjectile.getVisualProfileId();
            } else {
                if (family == null) {
                    family = TechniqueVfxPalette.Family.METAL;
                }
                if (trailStyle == null) {
                    trailStyle = sword
                            ? TechniqueVfxPacket.TrailStyle.SWORD_THIN
                            : TechniqueVfxPacket.TrailStyle.DEFAULT;
                }
            }
            primaryArgb = profileKey == null ? 0
                    : AuthoredVisualCatalog.resolve(profileKey).map(profile -> profile.primaryArgbInt()).orElse(0);
        }

        private void sample(Vec3 point) {
            if (point == null || !finite(point)) {
                return;
            }
            if (lastPoint == null || lastPoint.distanceToSqr(point) >= MIN_SAMPLE_DISTANCE_SQR) {
                points.addTrailPoint(point);
                lastPoint = point;
            }
        }

        private void detach(Vec3 finalPoint) {
            sample(finalPoint);
            detached = true;
            entity = null;
            fadeTicks = PROJECTILE_AFTERGLOW_TICKS;
        }

        private List<TrailPoint> renderPoints(float partialTick, Vec3 camera, int limit) {
            List<TrailPoint> sampled = downsample(points.getTrailPoints(), limit);
            List<TrailPoint> rendered = new ArrayList<>(sampled.size());
            for (int i = 0; i < sampled.size(); i++) {
                TrailPoint point = sampled.get(i);
                Vec3 position = point.getPosition();
                if (i == sampled.size() - 1 && !detached && entity != null && !entity.isRemoved()) {
                    position = entity.getPosition(Mth.clamp(partialTick, 0.0F, 1.0F))
                            .add(0.0D, entity.getBbHeight() * 0.5D, 0.0D);
                }
                Vec3 local = position.subtract(camera);
                if (!rendered.isEmpty()
                        && rendered.get(rendered.size() - 1).getPosition().distanceToSqr(local) < 1.0E-8D) {
                    continue;
                }
                rendered.add(new TrailPoint(local, point.getTimeActive()));
            }
            return rendered;
        }

        private boolean expired() {
            return detached && (fadeTicks <= 0 || points.getTrailPoints().size() < 2);
        }

        private float fadeAlpha() {
            return detached ? Mth.clamp(fadeTicks / (float) PROJECTILE_AFTERGLOW_TICKS, 0.0F, 1.0F) : 1.0F;
        }
    }

    private static final class TransientBeam {
        private final ResourceLocation profileKey;
        private final int primaryArgb;
        private final Vec3 start;
        private final Vec3 end;
        private final TechniqueVfxPalette.Family family;
        private final TechniqueVfxPacket.Kind kind;
        private final TechniqueVfxPacket.Motif motif;
        private final float width;
        private final int releaseStart;
        private final int sustainStart;
        private final int afterglowStart;
        private final int lifetime;
        private final long seed;
        private int age;

        private TransientBeam(ResourceLocation profileKey, int primaryArgb,
                              Vec3 start, Vec3 end,
                              TechniqueVfxPalette.Family family,
                              TechniqueVfxPacket.Kind kind, TechniqueVfxPacket.Motif motif,
                              float width, int releaseStart, int sustainStart,
                              int afterglowStart, int lifetime, long seed) {
            this.profileKey = profileKey;
            this.primaryArgb = primaryArgb;
            this.start = start;
            this.end = end;
            this.family = family;
            this.kind = kind;
            this.motif = motif;
            this.width = width;
            this.releaseStart = releaseStart;
            this.sustainStart = sustainStart;
            this.afterglowStart = afterglowStart;
            this.lifetime = lifetime;
            this.seed = seed;
        }

        private boolean tickAndExpired() {
            age++;
            return age >= lifetime;
        }

        private float alpha() {
            if (age < releaseStart) {
                return Mth.clamp((age + 1.0F) / releaseStart, 0.10F, 0.48F);
            }
            if (age < sustainStart) {
                return kind == TechniqueVfxPacket.Kind.BEAM || motif == TechniqueVfxPacket.Motif.CHANNEL
                        ? 0.96F : 0.82F;
            }
            if (age < afterglowStart) {
                return kind == TechniqueVfxPacket.Kind.BEAM ? 0.84F : 0.68F;
            }
            int fadeLength = Math.max(1, lifetime - afterglowStart);
            return Mth.clamp((lifetime - age) / (float) fadeLength, 0.0F, 0.66F);
        }
    }
}
