package com.xunxian.seekingimmortals.worldpack;

import com.xunxian.seekingimmortals.network.TechniqueVfxPacket;
import com.xunxian.seekingimmortals.skill.effect.TechniqueVfxPalette;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Server-side lifecycle bridge for ambient secret-realm and regional hazards. */
public final class WorldHazardVfxService {
    private static final Map<UUID, EnumMap<Hazard, VisualState>> STATES = new ConcurrentHashMap<>();

    private WorldHazardVfxService() {}

    public enum Hazard {
        DIYUAN("Diyuan", TechniqueVfxPalette.Family.EARTH, TechniqueVfxPacket.Motif.DOMAIN, 2.4D),
        YIN_UNDERWORLD("YinUnderworld", TechniqueVfxPalette.Family.SOUL, TechniqueVfxPacket.Motif.GHOST, 2.8D),
        DEMON_RIFT("DemonRift", TechniqueVfxPalette.Family.BLOOD, TechniqueVfxPacket.Motif.GHOST, 3.2D);

        private final String stateKey;
        private final TechniqueVfxPalette.Family family;
        private final TechniqueVfxPacket.Motif motif;
        private final double radius;

        Hazard(String stateKey, TechniqueVfxPalette.Family family,
               TechniqueVfxPacket.Motif motif, double radius) {
            this.stateKey = stateKey;
            this.family = family;
            this.motif = motif;
            this.radius = radius;
        }
    }

    public static void transition(ServerPlayer player, Hazard hazard, boolean active, boolean mitigated) {
        if (player == null || hazard == null) {
            return;
        }
        UUID playerId = player.getUUID();
        EnumMap<Hazard, VisualState> states = STATES.get(playerId);
        VisualState previous = states == null ? null : states.get(hazard);
        boolean previousActive = previous != null && previous.active();
        boolean previousMitigated = previous != null && previous.mitigated();
        if (previousActive == active && (!active || previousMitigated == mitigated)) {
            return;
        }
        if (active) {
            STATES.computeIfAbsent(playerId, ignored -> new EnumMap<>(Hazard.class))
                    .put(hazard, new VisualState(true, mitigated));
            emit(player, hazard, TechniqueVfxPacket.Kind.STATUS,
                    mitigated ? TechniqueVfxPacket.Motif.SHIELD : hazard.motif,
                    mitigated ? 22 : 32, hazard.radius);
        } else if (previousActive) {
            states.remove(hazard);
            if (states.isEmpty()) {
                STATES.remove(playerId, states);
            }
            emit(player, hazard, TechniqueVfxPacket.Kind.DISSIPATE,
                    previousMitigated ? TechniqueVfxPacket.Motif.SHIELD : hazard.motif,
                    28, hazard.radius);
        }
    }

    public static void clear(ServerPlayer player) {
        if (player != null) {
            STATES.remove(player.getUUID());
        }
    }

    public static void pulse(ServerPlayer player, Hazard hazard, boolean mitigated, float damage, int severity) {
        if (player == null || hazard == null) {
            return;
        }
        int boundedSeverity = Math.max(0, Math.min(4, severity));
        TechniqueVfxPacket.Kind kind = damage > 0.0F
                ? TechniqueVfxPacket.Kind.IMPACT
                : TechniqueVfxPacket.Kind.STATUS;
        TechniqueVfxPacket.Motif motif = mitigated ? TechniqueVfxPacket.Motif.SHIELD : hazard.motif;
        int intensity = (mitigated ? 16 : 22) + boundedSeverity * 4;
        emit(player, hazard, kind, motif, intensity,
                hazard.radius + boundedSeverity * 0.35D);
    }

    private static void emit(ServerPlayer player, Hazard hazard, TechniqueVfxPacket.Kind kind,
                             TechniqueVfxPacket.Motif motif, int intensity, double radius) {
        ServerLevel level = player.serverLevel();
        Vec3 center = player.position().add(0.0D, 0.18D, 0.0D);
        Vec3 top = center.add(0.0D, Math.max(1.0D, player.getBbHeight() * 0.72D), 0.0D);
        long seed = level.getGameTime() * 31L
                ^ player.getUUID().getMostSignificantBits()
                ^ player.getUUID().getLeastSignificantBits()
                ^ ((long) hazard.ordinal() << 44)
                ^ ((long) kind.ordinal() << 52);
        TechniqueVfxPacket.send(level, kind, hazard.family, motif,
                center, top, radius, intensity, seed);
    }

    private record VisualState(boolean active, boolean mitigated) {}
}
