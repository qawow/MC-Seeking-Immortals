package com.xunxian.seekingimmortals.artifact;

import com.xunxian.seekingimmortals.network.TechniqueVfxPacket;
import com.xunxian.seekingimmortals.skill.effect.TechniqueVfxOrchestrator;
import com.xunxian.seekingimmortals.skill.effect.TechniqueVfxPalette;
import com.xunxian.seekingimmortals.visual.VisualEventDispatcher;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/** Applies authored artifact materials to existing server-authoritative VFX geometry. */
public final class ArtifactVfxOrchestrator {
    private ArtifactVfxOrchestrator() {}

    public enum State {
        IDLE_BOUND,
        AWAKENED,
        OPEN,
        REPAIRED,
        DAMAGED,
        BROKEN
    }

    /** Particle/trail skin for a mapped active skill; semantic skill geometry remains authoritative. */
    public static TechniqueVfxOrchestrator.VisualOverride overrideFor(String artifactId) {
        return AuthoredArtifactVfxCatalog.find(artifactId)
                .filter(profile -> !profile.materialOnly() && !profile.deferred())
                .map(profile -> new TechniqueVfxOrchestrator.VisualOverride(
                        TechniqueVfxPalette.Family.NEUTRAL,
                        TechniqueVfxPacket.Motif.GENERIC,
                        profile.particle(),
                        profile.trail(),
                        profile.telegraphed()))
                .orElse(null);
    }

    public static void send(ServerLevel level,
                            String artifactId,
                            TechniqueVfxPacket.Kind kind,
                            TechniqueVfxPalette.Family fallbackFamily,
                            TechniqueVfxPacket.Motif fallbackMotif,
                            Vec3 start,
                            Vec3 end,
                            double radius,
                            int intensity,
                            long seed) {
        if (level == null || start == null) {
            return;
        }
        AuthoredArtifactVfxCatalog.Profile profile = AuthoredArtifactVfxCatalog.find(artifactId).orElse(null);
        TechniqueVfxPalette.Family family = fallbackFamily == null
                ? TechniqueVfxPalette.Family.NEUTRAL : fallbackFamily;
        TechniqueVfxPacket.Motif motif = fallbackMotif == null
                ? TechniqueVfxPacket.Motif.GENERIC : fallbackMotif;
        if (profile != null && family == TechniqueVfxPalette.Family.NEUTRAL) {
            family = profile.family();
        }
        if (profile != null && motif == TechniqueVfxPacket.Motif.GENERIC) {
            motif = profile.motif();
        }
        TechniqueVfxPacket.ParticleStyle particle = profile == null
                ? TechniqueVfxPacket.ParticleStyle.DEFAULT : profile.particle();
        TechniqueVfxPacket.TrailStyle trail = profile == null
                ? TechniqueVfxPacket.TrailStyle.DEFAULT : profile.trail();
        boolean telegraphed = profile != null && profile.telegraphed()
                && (kind == TechniqueVfxPacket.Kind.CAST
                || kind == TechniqueVfxPacket.Kind.PATH
                || kind == TechniqueVfxPacket.Kind.BEAM
                || kind == TechniqueVfxPacket.Kind.FORMATION);
        VisualEventDispatcher.event(level, "artifact", artifactId, kind.name(),
                start, end == null ? start : end, radius, intensity, seed,
                telegraphed ? 2 : 1);
    }

    public static void emitState(ServerPlayer player, String artifactId, State state) {
        if (player == null || artifactId == null || state == null) {
            return;
        }
        AuthoredArtifactVfxCatalog.Profile profile = AuthoredArtifactVfxCatalog.find(artifactId).orElse(null);
        if (profile == null || profile.materialOnly() || profile.deferred()) {
            return;
        }
        Vec3 center = player.position().add(0.0D, 0.85D, 0.0D);
        TechniqueVfxPacket.Kind kind;
        TechniqueVfxPalette.Family family = profile.family();
        TechniqueVfxPacket.Motif motif = profile.motif();
        double radius;
        int intensity;
        switch (state) {
            case IDLE_BOUND -> {
                kind = TechniqueVfxPacket.Kind.STATUS;
                radius = 0.78D;
                intensity = 18;
            }
            case AWAKENED -> {
                kind = TechniqueVfxPacket.Kind.AURA;
                radius = 1.25D + Math.min(2.5D, profile.gameTier() * 0.06D);
                intensity = 36;
            }
            case OPEN -> {
                kind = TechniqueVfxPacket.Kind.BURST;
                radius = 1.0D;
                intensity = 20;
            }
            case REPAIRED -> {
                kind = TechniqueVfxPacket.Kind.STATUS;
                motif = TechniqueVfxPacket.Motif.CLEANSE;
                radius = 0.95D;
                intensity = 24;
            }
            case DAMAGED -> {
                kind = TechniqueVfxPacket.Kind.STATUS;
                radius = 0.9D;
                intensity = 22;
            }
            case BROKEN -> {
                kind = TechniqueVfxPacket.Kind.DISSIPATE;
                radius = 1.1D;
                intensity = 30;
            }
            default -> {
                return;
            }
        }
        sendWithStateSkin(player.serverLevel(), profile, kind, family, motif,
                center, center, radius, intensity,
                player.blockPosition().asLong() ^ ((long) state.ordinal() << 44)
                        ^ player.serverLevel().getGameTime());
    }

    public static void emitIntegrityTransition(ServerPlayer player,
                                               String artifactId,
                                               int before,
                                               int after,
                                               int maximum) {
        if (player == null || maximum <= 0 || before <= after) {
            return;
        }
        if (after <= 0) {
            emitState(player, artifactId, State.BROKEN);
            return;
        }
        boolean enteredDamagedBand = before > Math.round(maximum * 0.70D)
                && after <= Math.round(maximum * 0.70D);
        boolean enteredCriticalBand = before > Math.round(maximum * 0.30D)
                && after <= Math.round(maximum * 0.30D);
        if (enteredDamagedBand || enteredCriticalBand) {
            emitState(player, artifactId, State.DAMAGED);
        }
    }

    private static void sendWithStateSkin(ServerLevel level,
                                          AuthoredArtifactVfxCatalog.Profile profile,
                                          TechniqueVfxPacket.Kind kind,
                                          TechniqueVfxPalette.Family family,
                                          TechniqueVfxPacket.Motif motif,
                                          Vec3 start,
                                          Vec3 end,
                                          double radius,
                                          int intensity,
                                          long seed) {
        VisualEventDispatcher.event(level, "artifact", profile.id(), kind.name(),
                start, end, radius, intensity, seed, kind == TechniqueVfxPacket.Kind.DISSIPATE ? 2 : 1);
    }
}
