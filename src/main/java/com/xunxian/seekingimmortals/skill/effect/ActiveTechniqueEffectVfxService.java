package com.xunxian.seekingimmortals.skill.effect;

import com.xunxian.seekingimmortals.cultivation.SpiritualRootAttribute;
import com.xunxian.seekingimmortals.network.TechniqueVfxPacket;
import com.xunxian.seekingimmortals.skill.CultivationSkill;
import com.xunxian.seekingimmortals.skill.SkillType;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Tracks the visible lifetime of technique-owned MobEffects on loaded living entities. */
public final class ActiveTechniqueEffectVfxService {
    private static final int STATUS_INTERVAL_TICKS = 30;
    private static final int MAX_STATUS_PACKETS_PER_SERVER_TICK = 8;
    private static final int MAX_DISSIPATE_PACKETS_PER_SERVER_TICK = 16;
    private static final int MAX_TRACKED_EFFECTS = 512;
    private static final int MAX_TRACKS_PER_ENTITY = 16;
    private static final int MAX_EFFECTS_PER_TRACK = 4;
    private static final int MAX_SEMANTIC_LENGTH = 96;
    private static final Map<TrackKey, EffectTrack> TRACKS = new LinkedHashMap<>();

    private static int statusCursor;
    private static MinecraftServer lastTickServer;
    private static long lastTickTime = Long.MIN_VALUE;

    private ActiveTechniqueEffectVfxService() {}

    /**
     * Captures only effects that are registered and actually active after gameplay application.
     * Recasting the same semantic on the same entity refreshes its existing visual track.
     */
    public static boolean track(LivingEntity entity,
                                String semantic,
                                TechniqueVfxPalette.Family family,
                                TechniqueVfxPacket.Motif motif,
                                double radius,
                                MobEffect... effects) {
        if (!canTrack(entity)) {
            return false;
        }
        String safeSemantic = normalizeSemantic(semantic);
        ActiveEffects activeEffects = activeEffects(entity, effects);
        if (safeSemantic == null || activeEffects.effectIds().isEmpty()) {
            return false;
        }

        MinecraftServer server = entity.getServer();
        long now = serverTime(server);
        TrackKey key = new TrackKey(entity.getUUID(), safeSemantic);
        TechniqueVfxPalette.Family safeFamily = family == null
                ? TechniqueVfxPalette.Family.NEUTRAL : family;
        TechniqueVfxPacket.Motif safeMotif = motif == null
                ? TechniqueVfxPacket.Motif.GENERIC : motif;
        double safeRadius = Math.max(0.55D, Math.min(12.0D, radius));

        synchronized (TRACKS) {
            EffectTrack previous = TRACKS.get(key);
            if (previous == null
                    && (TRACKS.size() >= MAX_TRACKED_EFFECTS
                    || countEntityTracks(entity.getUUID()) >= MAX_TRACKS_PER_ENTITY)) {
                return false;
            }
            long firstDelay = Math.min(STATUS_INTERVAL_TICKS,
                    Math.max(1, activeEffects.maxRemainingTicks() / 2));
            long nextStatusAt = previous == null
                    ? now + firstDelay
                    : Math.min(previous.nextStatusAt(), now + firstDelay);
            long emissionSequence = previous == null ? 0L : previous.emissionSequence();
            TRACKS.put(key, new EffectTrack(
                    server,
                    entity.level().dimension(),
                    activeEffects.effectIds(),
                    safeFamily,
                    safeMotif,
                    safeRadius,
                    now + activeEffects.maxRemainingTicks(),
                    nextStatusAt,
                    emissionSequence,
                    false,
                    null));
        }
        return true;
    }

    /** Call once at the end of each logical server tick. */
    public static void serverTick(MinecraftServer server) {
        if (server == null) {
            return;
        }
        long now = serverTime(server);
        List<Emission> statusEmissions = new ArrayList<>();
        List<Emission> dissipateEmissions = new ArrayList<>();

        synchronized (TRACKS) {
            if (lastTickServer == server && lastTickTime == now) {
                return;
            }
            lastTickServer = server;
            lastTickTime = now;
            if (TRACKS.isEmpty()) {
                statusCursor = 0;
                return;
            }

            List<TrackKey> keys = new ArrayList<>(TRACKS.keySet());
            int start = Math.floorMod(statusCursor, keys.size());
            int statusBudget = MAX_STATUS_PACKETS_PER_SERVER_TICK;
            int dissipateBudget = MAX_DISSIPATE_PACKETS_PER_SERVER_TICK;
            int lastStatusOffset = -1;
            int lastDissipateOffset = -1;
            for (int offset = 0; offset < keys.size(); offset++) {
                TrackKey key = keys.get((start + offset) % keys.size());
                EffectTrack track = TRACKS.get(key);
                if (track == null) {
                    continue;
                }
                if (track.server() != server) {
                    TRACKS.remove(key);
                    continue;
                }

                ServerLevel level = server.getLevel(track.dimension());
                if (track.pendingDissipate()) {
                    if (level == null) {
                        TRACKS.remove(key);
                    } else if (dissipateBudget > 0) {
                        TRACKS.remove(key);
                        dissipateEmissions.add(emission(level, track.pendingCenter(), key, track, true));
                        dissipateBudget--;
                        lastDissipateOffset = offset;
                    }
                    continue;
                }
                Entity resolved = level == null ? null : level.getEntity(key.entityId());
                if (!(resolved instanceof LivingEntity living) || living.isRemoved()) {
                    // Unload and dimension transfer have no reliable same-dimension endpoint.
                    TRACKS.remove(key);
                    continue;
                }
                if (!living.isAlive() || living.isDeadOrDying()) {
                    Vec3 center = effectCenter(living);
                    if (dissipateBudget > 0) {
                        TRACKS.remove(key);
                        dissipateEmissions.add(emission(level, center, key, track, true));
                        dissipateBudget--;
                        lastDissipateOffset = offset;
                    } else {
                        TRACKS.put(key, track.withPendingDissipate(center));
                    }
                    continue;
                }

                ActiveEffects active = activeEffects(living, track.effectIds());
                if (now >= track.observedEndsAt() || active.effectIds().isEmpty()) {
                    Vec3 center = effectCenter(living);
                    if (dissipateBudget > 0) {
                        TRACKS.remove(key);
                        dissipateEmissions.add(emission(level, center, key, track, true));
                        dissipateBudget--;
                        lastDissipateOffset = offset;
                    } else {
                        TRACKS.put(key, track.withPendingDissipate(center));
                    }
                    continue;
                }

                EffectTrack observed = track.withObservedEffects(active.effectIds());
                if (statusBudget > 0 && now >= observed.nextStatusAt()) {
                    observed = observed.withStatusPulse(now + STATUS_INTERVAL_TICKS);
                    statusEmissions.add(emission(level, effectCenter(living), key, observed, false));
                    statusBudget--;
                    lastStatusOffset = offset;
                }
                TRACKS.put(key, observed);
            }

            if (TRACKS.isEmpty()) {
                statusCursor = 0;
            } else {
                int lastEmissionOffset = Math.max(lastStatusOffset, lastDissipateOffset);
                int advance = lastEmissionOffset >= 0 ? lastEmissionOffset + 1 : 1;
                statusCursor = Math.floorMod(start + advance, TRACKS.size());
            }
        }

        statusEmissions.forEach(ActiveTechniqueEffectVfxService::sendStatus);
        dissipateEmissions.forEach(ActiveTechniqueEffectVfxService::sendDissipate);
    }

    /** Removes tracks for a level that is unloading; no packet is sent into an unloading dimension. */
    public static void clearLevel(ServerLevel level) {
        if (level == null) {
            return;
        }
        synchronized (TRACKS) {
            TRACKS.entrySet().removeIf(entry -> entry.getValue().server() == level.getServer()
                    && entry.getValue().dimension().equals(level.dimension()));
            if (TRACKS.isEmpty()) {
                statusCursor = 0;
            }
        }
    }

    /** Removes all tracks for an entity, intended for explicit leave-level cleanup. */
    public static void clearEntity(LivingEntity entity) {
        if (entity == null) {
            return;
        }
        synchronized (TRACKS) {
            TRACKS.keySet().removeIf(key -> key.entityId().equals(entity.getUUID()));
            if (TRACKS.isEmpty()) {
                statusCursor = 0;
            }
        }
    }

    /** Handles leave-level cleanup without losing terminal visuals or crossing dimensions. */
    public static void onEntityLeave(LivingEntity entity) {
        if (entity == null) {
            return;
        }
        Entity.RemovalReason reason = entity.getRemovalReason();
        if (reason == Entity.RemovalReason.CHANGED_DIMENSION && entity instanceof ServerPlayer) {
            return;
        }
        if (reason != null && reason.shouldDestroy() && entity.level() instanceof ServerLevel level) {
            Vec3 center = effectCenter(entity);
            synchronized (TRACKS) {
                for (Map.Entry<TrackKey, EffectTrack> entry : TRACKS.entrySet()) {
                    EffectTrack track = entry.getValue();
                    if (entry.getKey().entityId().equals(entity.getUUID())
                            && track.server() == level.getServer()
                            && track.dimension().equals(level.dimension())) {
                        entry.setValue(track.withPendingDissipate(center));
                    }
                }
            }
            return;
        }
        clearEntity(entity);
    }

    /** Rebinds still-active player tracks after a completed dimension transfer. */
    public static int relocateEntity(ServerPlayer player) {
        if (!canTrack(player)) {
            return 0;
        }
        MinecraftServer server = player.getServer();
        long now = serverTime(server);
        int relocated = 0;
        int pulseOffset = 0;
        synchronized (TRACKS) {
            var iterator = TRACKS.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<TrackKey, EffectTrack> entry = iterator.next();
                if (!entry.getKey().entityId().equals(player.getUUID())) {
                    continue;
                }
                EffectTrack track = entry.getValue();
                ActiveEffects active = activeEffects(player, track.effectIds());
                if (track.server() != server || now >= track.observedEndsAt()
                        || active.effectIds().isEmpty()) {
                    iterator.remove();
                    continue;
                }
                long nextStatusAt = now + 1L + pulseOffset % STATUS_INTERVAL_TICKS;
                entry.setValue(track.withRelocation(
                        player.serverLevel().dimension(),
                        active.effectIds(),
                        nextStatusAt));
                relocated++;
                pulseOffset++;
            }
            if (TRACKS.isEmpty()) {
                statusCursor = 0;
            }
        }
        return relocated;
    }

    /** Clears static runtime state when a logical server stops. */
    public static void clearAll() {
        synchronized (TRACKS) {
            TRACKS.clear();
            statusCursor = 0;
            lastTickServer = null;
            lastTickTime = Long.MIN_VALUE;
        }
    }

    public static String semantic(CultivationSkill skill, String fallback) {
        if (skill != null && skill.getSkillType() != null) {
            SkillType type = skill.getSkillType();
            String techniqueId = type.getTechniqueId();
            String normalized = normalizeSemantic(techniqueId);
            if (normalized != null) {
                return normalized;
            }
            normalized = normalizeSemantic(type.name());
            if (normalized != null) {
                return normalized;
            }
        }
        String normalizedFallback = normalizeSemantic(fallback);
        return normalizedFallback == null ? "technique_effect" : normalizedFallback;
    }

    public static TechniqueVfxPalette.Family familyForSkill(
            CultivationSkill skill,
            TechniqueVfxPalette.Family fallback) {
        TechniqueVfxPalette.Family safeFallback = fallback == null
                ? TechniqueVfxPalette.Family.NEUTRAL : fallback;
        if (skill == null || skill.getSkillType() == null) {
            return safeFallback;
        }
        SkillType type = skill.getSkillType();
        for (SpiritualRootAttribute attribute : type.getAffinityAttributes()) {
            TechniqueVfxPalette.Family family = familyForAttribute(attribute);
            if (family != TechniqueVfxPalette.Family.NEUTRAL) {
                return family;
            }
        }
        TechniqueVfxPalette.Family inferred = TechniqueVfxPalette.familyOf(
                type.name() + " " + type.getTechniqueId() + " "
                        + type.getDisplayName() + " " + type.getDescription());
        return inferred == TechniqueVfxPalette.Family.NEUTRAL ? safeFallback : inferred;
    }

    static int statusIntervalTicks() {
        return STATUS_INTERVAL_TICKS;
    }

    static int maxStatusPacketsPerServerTick() {
        return MAX_STATUS_PACKETS_PER_SERVER_TICK;
    }

    static int maxTrackedEffects() {
        return MAX_TRACKED_EFFECTS;
    }

    static int maxDissipatePacketsPerServerTick() {
        return MAX_DISSIPATE_PACKETS_PER_SERVER_TICK;
    }

    static int maxTracksPerEntity() {
        return MAX_TRACKS_PER_ENTITY;
    }

    static String normalizeSemantic(String semantic) {
        if (semantic == null) {
            return null;
        }
        String source = semantic.trim().toLowerCase(Locale.ROOT);
        if (source.isEmpty() || source.length() > MAX_SEMANTIC_LENGTH) {
            return null;
        }
        StringBuilder normalized = new StringBuilder(source.length());
        boolean previousSeparator = false;
        for (int i = 0; i < source.length(); i++) {
            char value = source.charAt(i);
            boolean allowed = value >= 'a' && value <= 'z'
                    || value >= '0' && value <= '9'
                    || value == '.' || value == '/' || value == ':' || value == '-';
            if (allowed) {
                normalized.append(value);
                previousSeparator = false;
            } else if (!previousSeparator) {
                normalized.append('_');
                previousSeparator = true;
            }
        }
        String result = normalized.toString();
        return result.isBlank() ? null : result;
    }

    private static boolean canTrack(LivingEntity entity) {
        return entity != null
                && entity.getServer() != null
                && entity.level() instanceof ServerLevel
                && !entity.isRemoved()
                && entity.isAlive()
                && !entity.isDeadOrDying();
    }

    private static long serverTime(MinecraftServer server) {
        return server.overworld().getGameTime();
    }

    private static int countEntityTracks(UUID entityId) {
        int count = 0;
        for (TrackKey key : TRACKS.keySet()) {
            if (key.entityId().equals(entityId)) {
                count++;
            }
        }
        return count;
    }

    private static ActiveEffects activeEffects(LivingEntity entity, MobEffect... effects) {
        Set<ResourceLocation> effectIds = new LinkedHashSet<>();
        int maxRemainingTicks = 0;
        if (effects == null) {
            return new ActiveEffects(List.of(), 0);
        }
        for (MobEffect effect : effects) {
            if (effectIds.size() >= MAX_EFFECTS_PER_TRACK || effect == null) {
                continue;
            }
            ResourceLocation effectId = registeredEffectId(effect);
            MobEffectInstance active = effectId == null ? null : entity.getEffect(effect);
            if (active != null && active.getDuration() > 0 && effectIds.add(effectId)) {
                maxRemainingTicks = Math.max(maxRemainingTicks, active.getDuration());
            }
        }
        return new ActiveEffects(List.copyOf(effectIds), maxRemainingTicks);
    }

    private static ActiveEffects activeEffects(LivingEntity entity, List<ResourceLocation> trackedEffectIds) {
        Set<ResourceLocation> activeIds = new LinkedHashSet<>();
        int maxRemainingTicks = 0;
        for (ResourceLocation effectId : trackedEffectIds) {
            if (activeIds.size() >= MAX_EFFECTS_PER_TRACK) {
                break;
            }
            MobEffect effect = ForgeRegistries.MOB_EFFECTS.getValue(effectId);
            MobEffectInstance active = effect == null ? null : entity.getEffect(effect);
            if (active != null && active.getDuration() > 0 && activeIds.add(effectId)) {
                maxRemainingTicks = Math.max(maxRemainingTicks, active.getDuration());
            }
        }
        return new ActiveEffects(List.copyOf(activeIds), maxRemainingTicks);
    }

    private static ResourceLocation registeredEffectId(MobEffect effect) {
        ResourceLocation effectId = ForgeRegistries.MOB_EFFECTS.getKey(effect);
        return effectId != null && ForgeRegistries.MOB_EFFECTS.getValue(effectId) == effect
                ? effectId : null;
    }

    private static TechniqueVfxPalette.Family familyForAttribute(SpiritualRootAttribute attribute) {
        if (attribute == null) {
            return TechniqueVfxPalette.Family.NEUTRAL;
        }
        return switch (attribute) {
            case METAL -> TechniqueVfxPalette.Family.METAL;
            case WOOD -> TechniqueVfxPalette.Family.WOOD;
            case WATER -> TechniqueVfxPalette.Family.WATER;
            case FIRE -> TechniqueVfxPalette.Family.FIRE;
            case EARTH -> TechniqueVfxPalette.Family.EARTH;
            case WIND -> TechniqueVfxPalette.Family.WIND;
            case THUNDER, HIDDEN_THUNDER -> TechniqueVfxPalette.Family.THUNDER;
            case ICE -> TechniqueVfxPalette.Family.ICE;
            case YIN, DARK, HIDDEN_DARK -> TechniqueVfxPalette.Family.DARK;
            case YANG, IMMORTAL -> TechniqueVfxPalette.Family.LIGHT;
            case NONE -> TechniqueVfxPalette.Family.NEUTRAL;
        };
    }

    private static Vec3 effectCenter(LivingEntity entity) {
        return entity.position().add(0.0D, 0.08D, 0.0D);
    }

    private static Emission emission(ServerLevel level,
                                     Vec3 center,
                                     TrackKey key,
                                     EffectTrack track,
                                     boolean dissipate) {
        long seed = key.entityId().getMostSignificantBits()
                ^ key.entityId().getLeastSignificantBits()
                ^ ((long) key.semantic().hashCode() << 32)
                ^ track.emissionSequence() * 0x9E3779B97F4A7C15L
                ^ (dissipate ? 0xD1B54A32D192ED03L : 0L);
        return new Emission(level, center, track.family(), track.motif(), track.radius(), seed);
    }

    private static void sendStatus(Emission emission) {
        TechniqueVfxPacket.send(
                emission.level(),
                TechniqueVfxPacket.Kind.STATUS,
                emission.family(),
                emission.motif(),
                emission.center(),
                emission.center(),
                emission.radius(),
                16,
                emission.seed());
    }

    private static void sendDissipate(Emission emission) {
        TechniqueVfxPacket.send(
                emission.level(),
                TechniqueVfxPacket.Kind.DISSIPATE,
                emission.family(),
                emission.motif(),
                emission.center(),
                emission.center(),
                emission.radius(),
                24,
                emission.seed());
    }

    private record TrackKey(UUID entityId, String semantic) {}

    private record ActiveEffects(List<ResourceLocation> effectIds, int maxRemainingTicks) {}

    private record EffectTrack(
            MinecraftServer server,
            ResourceKey<Level> dimension,
            List<ResourceLocation> effectIds,
            TechniqueVfxPalette.Family family,
            TechniqueVfxPacket.Motif motif,
            double radius,
            long observedEndsAt,
            long nextStatusAt,
            long emissionSequence,
            boolean pendingDissipate,
            Vec3 pendingCenter
    ) {
        private EffectTrack {
            effectIds = List.copyOf(effectIds);
        }

        private EffectTrack withObservedEffects(List<ResourceLocation> activeEffectIds) {
            return new EffectTrack(server, dimension, activeEffectIds, family, motif, radius,
                    observedEndsAt, nextStatusAt, emissionSequence, false, null);
        }

        private EffectTrack withStatusPulse(long nextPulseAt) {
            return new EffectTrack(server, dimension, effectIds, family, motif, radius,
                    observedEndsAt, nextPulseAt, emissionSequence + 1L, false, null);
        }

        private EffectTrack withRelocation(ResourceKey<Level> targetDimension,
                                           List<ResourceLocation> activeEffectIds,
                                           long nextPulseAt) {
            return new EffectTrack(server, targetDimension, activeEffectIds, family, motif, radius,
                    observedEndsAt, nextPulseAt, emissionSequence, false, null);
        }

        private EffectTrack withPendingDissipate(Vec3 center) {
            return new EffectTrack(server, dimension, List.of(), family, motif, radius,
                    observedEndsAt, nextStatusAt, emissionSequence, true, center);
        }
    }

    private record Emission(
            ServerLevel level,
            Vec3 center,
            TechniqueVfxPalette.Family family,
            TechniqueVfxPacket.Motif motif,
            double radius,
            long seed
    ) {}
}
