package com.xunxian.seekingimmortals.skill.effect;

import com.xunxian.seekingimmortals.entity.SummonedServitorEntity;
import com.xunxian.seekingimmortals.network.TechniqueVfxPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Server-side lifecycle intents for transient technique visuals. */
public final class TechniqueLifecycleVfxService {
    private static final int SELF_BUFF_STATUS_INTERVAL_TICKS = 40;
    private static final int MAX_SELF_BUFF_STATUS_PULSES_PER_TICK = 4;
    private static final int MAX_SELF_BUFF_STATUS_PULSES_PER_SERVER_TICK = 16;
    private static final int MAX_SELF_BUFF_DISSIPATES_PER_SERVER_TICK = 16;
    private static final int MAX_TRACKED_SELF_BUFFS = 512;
    private static final int MAX_SELF_BUFFS_PER_PLAYER = 64;
    private static final int MAX_PENDING_SELF_BUFF_STATUSES = MAX_TRACKED_SELF_BUFFS;
    private static final int MAX_PENDING_SELF_BUFF_DISSIPATES = MAX_TRACKED_SELF_BUFFS;
    private static final int MAX_EFFECT_ID_LENGTH = 128;
    private static final String SELF_BUFF_DATA_KEY = "SeekingImmortalsSelfBuffVfx";
    private static final String TRACKS_TAG = "Tracks";
    private static final String SEMANTIC_TAG = "Semantic";
    private static final String EFFECT_TAG = "Effect";
    private static final String FAMILY_TAG = "Family";
    private static final String MOTIF_TAG = "Motif";
    private static final String CAPTURED_REMAINING_TAG = "CapturedRemaining";
    private static final String ENDS_AT_TAG = "EndsAt";
    private static final Map<SelfBuffKey, SelfBuffTrack> SELF_BUFFS = new LinkedHashMap<>();
    private static final Set<SelfBuffKey> PENDING_SELF_BUFF_STATUSES = new LinkedHashSet<>();
    private static final Deque<PendingSelfBuffDissipate> PENDING_SELF_BUFF_DISSIPATES = new ArrayDeque<>();

    private static MinecraftServer lastTickServer;
    private static long lastTickTime = Long.MIN_VALUE;

    private TechniqueLifecycleVfxService() {}

    public static void captureGeometry(ServerLevel level,
                                       TechniqueVfxPacket.Kind kind,
                                       TechniqueVfxPalette.Family family,
                                       Vec3 start,
                                       Vec3 end,
                                       double radius,
                                       int intensity,
                                       long seedSalt) {
        send(level, kind, family, TechniqueVfxPacket.Motif.GENERIC,
                start, end, radius, intensity, seedSalt);
    }

    public static void projectileImpact(ServerLevel level,
                                        TechniqueVfxPalette.Family family,
                                        TechniqueVfxPacket.Motif motif,
                                        Vec3 position,
                                        double radius,
                                        int intensity,
                                        long seedSalt) {
        send(level, TechniqueVfxPacket.Kind.IMPACT, family, motif,
                position, position, radius, intensity, seedSalt);
    }

    public static void projectileDissipate(ServerLevel level,
                                           TechniqueVfxPalette.Family family,
                                           TechniqueVfxPacket.Motif motif,
                                           Vec3 position,
                                           double radius,
                                           long seedSalt) {
        send(level, TechniqueVfxPacket.Kind.DISSIPATE, family, motif,
                position, position, radius, 24, seedSalt);
    }

    public static void summon(SummonedServitorEntity servitor) {
        if (!(servitor.level() instanceof ServerLevel level)) {
            return;
        }
        TechniqueVfxPalette.Family family = servitorFamily(servitor.getArchetype());
        Vec3 center = servitor.position();
        send(level, TechniqueVfxPacket.Kind.BURST, family, TechniqueVfxPacket.Motif.SUMMON,
                center, center.add(0.0D, Math.max(1.0D, servitor.getBbHeight()), 0.0D),
                Math.max(1.0D, servitor.getBbWidth() * 1.4D), 36, servitor.getId());
    }

    public static void servitorStatus(SummonedServitorEntity servitor) {
        if (!(servitor.level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 center = servitor.position().add(0.0D, 0.08D, 0.0D);
        send(level, TechniqueVfxPacket.Kind.STATUS, servitorFamily(servitor.getArchetype()),
                servitorMotif(servitor.getArchetype()), center, center,
                Math.max(0.7D, servitor.getBbWidth()), 14, servitor.getId() * 17L);
    }

    public static void servitorImpact(SummonedServitorEntity servitor, Vec3 targetCenter) {
        if (!(servitor.level() instanceof ServerLevel level) || targetCenter == null) {
            return;
        }
        Vec3 start = servitor.position().add(0.0D, servitor.getBbHeight() * 0.55D, 0.0D);
        send(level, TechniqueVfxPacket.Kind.IMPACT, servitorFamily(servitor.getArchetype()),
                servitorMotif(servitor.getArchetype()), targetCenter, start,
                0.8D, 24, servitor.getId() * 31L);
    }

    public static void servitorDissipate(SummonedServitorEntity servitor) {
        if (!(servitor.level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 center = servitor.position().add(0.0D, servitor.getBbHeight() * 0.4D, 0.0D);
        send(level, TechniqueVfxPacket.Kind.DISSIPATE, servitorFamily(servitor.getArchetype()),
                servitorMotif(servitor.getArchetype()), center, center,
                Math.max(0.9D, servitor.getBbWidth() * 1.2D), 30, servitor.getId() * 47L);
    }

    public static boolean trackSelfBuff(ServerPlayer player,
                                        String semantic,
                                        MobEffect effect,
                                        int sourceDurationTicks,
                                        TechniqueVfxPalette.Family family,
                                        TechniqueVfxPacket.Motif motif) {
        if (!canTrack(player) || effect == null) {
            return false;
        }
        String safeSemantic = ActiveTechniqueEffectVfxService.normalizeSemantic(semantic);
        ResourceLocation effectId = registeredEffectId(effect);
        MobEffectInstance activeEffect = player.getEffect(effect);
        if (safeSemantic == null || !isValidEffectId(effectId)
                || activeEffect == null || activeEffect.getDuration() <= 0
                || sourceDurationTicks <= 0) {
            return false;
        }

        UUID playerId = player.getUUID();
        SelfBuffKey key = new SelfBuffKey(playerId, safeSemantic, effectId);
        TechniqueVfxPalette.Family safeFamily = family == null
                ? TechniqueVfxPalette.Family.NEUTRAL : family;
        TechniqueVfxPacket.Motif safeMotif = motif == null
                ? TechniqueVfxPacket.Motif.DAO : motif;
        long now = serverTime(player);
        int capturedRemainingTicks = Math.min(sourceDurationTicks, activeEffect.getDuration());
        long endsAt = boundedSelfBuffEndsAt(
                now, capturedRemainingTicks, activeEffect.getDuration(), Long.MAX_VALUE);
        synchronized (SELF_BUFFS) {
            SelfBuffTrack previous = SELF_BUFFS.get(key);
            if (previous == null
                    && (countPlayerTracks(playerId) >= MAX_SELF_BUFFS_PER_PLAYER
                    || SELF_BUFFS.size() >= MAX_TRACKED_SELF_BUFFS)) {
                return false;
            }
            long nextPulseAt = previous == null
                    ? now + SELF_BUFF_STATUS_INTERVAL_TICKS
                    : Math.min(previous.nextPulseAt(), now + SELF_BUFF_STATUS_INTERVAL_TICKS);
            SELF_BUFFS.put(key, new SelfBuffTrack(
                    player.serverLevel().dimension(), safeFamily, safeMotif,
                    capturedRemainingTicks, endsAt, nextPulseAt));
            PENDING_SELF_BUFF_DISSIPATES.removeIf(pending ->
                    pending.playerId().equals(playerId)
                            && pending.family() == safeFamily
                            && pending.motif() == safeMotif);
        }
        persistPlayerTracks(player);
        return true;
    }

    public static void tickSelfBuff(ServerPlayer player) {
        if (!canTrack(player)) {
            return;
        }
        UUID playerId = player.getUUID();
        List<Map.Entry<SelfBuffKey, SelfBuffTrack>> tracks = new ArrayList<>();
        synchronized (SELF_BUFFS) {
            for (Map.Entry<SelfBuffKey, SelfBuffTrack> entry : SELF_BUFFS.entrySet()) {
                if (entry.getKey().playerId().equals(playerId)) {
                    tracks.add(Map.entry(entry.getKey(), entry.getValue()));
                }
            }
        }
        if (tracks.isEmpty()) {
            return;
        }
        if (player.isDeadOrDying()) {
            clearSelfBuff(player, true);
            return;
        }

        long now = serverTime(player);
        int pulseBudget = MAX_SELF_BUFF_STATUS_PULSES_PER_TICK;
        boolean persistentStateChanged = false;
        List<SelfBuffEmission> removed = new ArrayList<>();
        Set<SelfBuffVisualKey> handledPulseVisuals = new HashSet<>();
        synchronized (SELF_BUFFS) {
            for (Map.Entry<SelfBuffKey, SelfBuffTrack> entry : tracks) {
                SelfBuffTrack track = SELF_BUFFS.get(entry.getKey());
                if (track == null) {
                    continue;
                }
                if (!track.dimension().equals(player.serverLevel().dimension())) {
                    track = track.withDimension(player.serverLevel().dimension());
                    SELF_BUFFS.put(entry.getKey(), track);
                    persistentStateChanged = true;
                }
                if (now >= track.endsAt()
                        || !hasActiveEffect(player, entry.getKey().effectId())) {
                    removed.add(new SelfBuffEmission(entry.getKey(), track));
                    SELF_BUFFS.remove(entry.getKey());
                    PENDING_SELF_BUFF_STATUSES.remove(entry.getKey());
                    persistentStateChanged = true;
                } else if (now >= track.nextPulseAt()) {
                    SelfBuffVisualKey visualKey = visualKey(track);
                    if (!handledPulseVisuals.add(visualKey)) {
                        continue;
                    }
                    if (hasPendingSelfBuffStatus(playerId, visualKey)) {
                        alignSelfBuffStatusPulses(playerId, visualKey, now);
                    } else if (pulseBudget > 0
                            && PENDING_SELF_BUFF_STATUSES.size() < MAX_PENDING_SELF_BUFF_STATUSES) {
                        PENDING_SELF_BUFF_STATUSES.add(entry.getKey());
                        alignSelfBuffStatusPulses(playerId, visualKey, now);
                        pulseBudget--;
                    }
                }
            }
        }
        if (persistentStateChanged) {
            persistPlayerTracks(player);
        }
        Vec3 center = player.position().add(0.0D, 0.08D, 0.0D);
        emitSelfBuffDissipate(player, removed, center);
    }

    /** Drains globally bounded self-buff lifecycle intents once per logical server tick. */
    public static void serverTick(MinecraftServer server) {
        if (server == null) {
            return;
        }
        long now = serverTime(server);
        List<SelfBuffStatusEmission> statusEmissions = new ArrayList<>();
        List<PendingSelfBuffDissipate> dissipateEmissions = new ArrayList<>();
        synchronized (SELF_BUFFS) {
            if (lastTickServer == server && lastTickTime == now) {
                return;
            }
            lastTickServer = server;
            lastTickTime = now;

            int statusBudget = MAX_SELF_BUFF_STATUS_PULSES_PER_SERVER_TICK;
            Set<SelfBuffOwnerVisualKey> emittedStatusVisuals = new HashSet<>();
            var statusIterator = PENDING_SELF_BUFF_STATUSES.iterator();
            while (statusIterator.hasNext() && statusBudget > 0) {
                SelfBuffKey key = statusIterator.next();
                statusIterator.remove();
                SelfBuffTrack track = SELF_BUFFS.get(key);
                ServerPlayer player = server.getPlayerList().getPlayer(key.playerId());
                if (track == null || player == null || player.getServer() != server
                        || player.isDeadOrDying() || now >= track.endsAt()
                        || !hasActiveEffect(player, key.effectId())) {
                    continue;
                }
                SelfBuffOwnerVisualKey ownerVisualKey = new SelfBuffOwnerVisualKey(
                        key.playerId(), track.family(), track.motif());
                if (!emittedStatusVisuals.add(ownerVisualKey)) {
                    continue;
                }
                statusEmissions.add(new SelfBuffStatusEmission(player, key, track));
                statusBudget--;
            }
            if (!emittedStatusVisuals.isEmpty()) {
                PENDING_SELF_BUFF_STATUSES.removeIf(key -> {
                    SelfBuffTrack track = SELF_BUFFS.get(key);
                    return track == null || emittedStatusVisuals.contains(new SelfBuffOwnerVisualKey(
                            key.playerId(), track.family(), track.motif()));
                });
            }

            int dissipateBudget = MAX_SELF_BUFF_DISSIPATES_PER_SERVER_TICK;
            while (!PENDING_SELF_BUFF_DISSIPATES.isEmpty() && dissipateBudget > 0) {
                PendingSelfBuffDissipate pending = PENDING_SELF_BUFF_DISSIPATES.removeFirst();
                if (pending.server() != server || server.getLevel(pending.dimension()) == null) {
                    continue;
                }
                dissipateEmissions.add(pending);
                dissipateBudget--;
            }
        }

        for (SelfBuffStatusEmission emission : statusEmissions) {
            ServerPlayer player = emission.player();
            Vec3 center = player.position().add(0.0D, 0.08D, 0.0D);
            send(player.serverLevel(), TechniqueVfxPacket.Kind.STATUS,
                    emission.track().family(), emission.track().motif(),
                    center, center, 0.92D, 16,
                    player.getId() * 59L ^ emission.key().semantic().hashCode()
                            ^ emission.key().effectId().hashCode());
        }
        for (PendingSelfBuffDissipate emission : dissipateEmissions) {
            ServerLevel level = server.getLevel(emission.dimension());
            if (level != null) {
                send(level, TechniqueVfxPacket.Kind.DISSIPATE,
                        emission.family(), emission.motif(),
                        emission.center(), emission.center(), 0.98D, 24, emission.seedSalt());
            }
        }
    }

    /** Restores persisted visual tracks after the player's MobEffect list has loaded. */
    public static int restoreSelfBuffs(ServerPlayer player) {
        if (!canTrack(player) || player.isDeadOrDying()) {
            return 0;
        }
        removeRuntimeTracks(player.getUUID());
        CompoundTag persistentData = player.getPersistentData();
        if (!persistentData.contains(SELF_BUFF_DATA_KEY, Tag.TAG_COMPOUND)) {
            if (persistentData.contains(SELF_BUFF_DATA_KEY)) {
                persistentData.remove(SELF_BUFF_DATA_KEY);
            }
            return 0;
        }

        CompoundTag root = persistentData.getCompound(SELF_BUFF_DATA_KEY);
        if (!root.contains(TRACKS_TAG, Tag.TAG_LIST)) {
            persistentData.remove(SELF_BUFF_DATA_KEY);
            return 0;
        }
        ListTag savedTracks = root.getList(TRACKS_TAG, Tag.TAG_COMPOUND);
        Set<SelfBuffKey> restoredKeys = new HashSet<>();
        long now = serverTime(player);
        int restored = 0;
        int entryLimit = Math.min(savedTracks.size(), MAX_SELF_BUFFS_PER_PLAYER);
        synchronized (SELF_BUFFS) {
            for (int i = 0; i < entryLimit && SELF_BUFFS.size() < MAX_TRACKED_SELF_BUFFS; i++) {
                CompoundTag saved = savedTracks.getCompound(i);
                String semantic = ActiveTechniqueEffectVfxService.normalizeSemantic(
                        saved.getString(SEMANTIC_TAG));
                if (semantic == null) {
                    semantic = "legacy_self_buff";
                }
                ResourceLocation effectId = parseEffectId(saved.getString(EFFECT_TAG));
                TechniqueVfxPalette.Family family = parseEnum(
                        TechniqueVfxPalette.Family.class, saved.getString(FAMILY_TAG));
                TechniqueVfxPacket.Motif motif = parseEnum(
                        TechniqueVfxPacket.Motif.class, saved.getString(MOTIF_TAG));
                int capturedRemainingTicks = saved.contains(CAPTURED_REMAINING_TAG, Tag.TAG_INT)
                        ? saved.getInt(CAPTURED_REMAINING_TAG) : 0;
                long savedEndsAt = saved.contains(ENDS_AT_TAG, Tag.TAG_LONG)
                        ? saved.getLong(ENDS_AT_TAG) : Long.MIN_VALUE;
                int activeRemainingTicks = activeEffectDuration(player, effectId);
                long endsAt = boundedSelfBuffEndsAt(
                        now, capturedRemainingTicks, activeRemainingTicks, savedEndsAt);
                if (!isValidEffectId(effectId) || family == null || motif == null
                        || capturedRemainingTicks <= 0 || endsAt <= now
                        || activeRemainingTicks <= 0) {
                    continue;
                }
                SelfBuffKey key = new SelfBuffKey(player.getUUID(), semantic, effectId);
                if (!restoredKeys.add(key)) {
                    continue;
                }
                long nextPulseAt = now + 1L + restored % SELF_BUFF_STATUS_INTERVAL_TICKS;
                SELF_BUFFS.put(key, new SelfBuffTrack(
                        player.serverLevel().dimension(), family, motif,
                        capturedRemainingTicks, endsAt, nextPulseAt));
                restored++;
            }
        }
        persistPlayerTracks(player);
        return restored;
    }

    /** Preserves valid metadata while removing runtime state for an offline player. */
    public static void pauseSelfBuffs(ServerPlayer player) {
        if (!canTrack(player)) {
            return;
        }
        if (countPlayerTracksSafely(player.getUUID()) == 0
                && player.getPersistentData().contains(SELF_BUFF_DATA_KEY, Tag.TAG_COMPOUND)) {
            restoreSelfBuffs(player);
        }
        pauseRuntimeTracks(player);
        persistPlayerTracks(player);
        removeRuntimeTracks(player.getUUID());
    }

    /** Rebinds live tracks to the player's new server dimension without ending active effects. */
    public static int relocateSelfBuffs(ServerPlayer player) {
        if (!canTrack(player) || player.isDeadOrDying()) {
            return 0;
        }
        if (countPlayerTracksSafely(player.getUUID()) == 0) {
            return restoreSelfBuffs(player);
        }

        long now = serverTime(player);
        int relocated = 0;
        int pulseOffset = 0;
        List<SelfBuffEmission> removed = new ArrayList<>();
        synchronized (SELF_BUFFS) {
            var iterator = SELF_BUFFS.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<SelfBuffKey, SelfBuffTrack> entry = iterator.next();
                if (!entry.getKey().playerId().equals(player.getUUID())) {
                    continue;
                }
                if (now >= entry.getValue().endsAt()
                        || !hasActiveEffect(player, entry.getKey().effectId())) {
                    removed.add(new SelfBuffEmission(entry.getKey(), entry.getValue()));
                    iterator.remove();
                    PENDING_SELF_BUFF_STATUSES.remove(entry.getKey());
                    continue;
                }
                long nextPulseAt = now + 1L + pulseOffset % SELF_BUFF_STATUS_INTERVAL_TICKS;
                entry.setValue(entry.getValue()
                        .withDimension(player.serverLevel().dimension())
                        .withNextPulse(nextPulseAt));
                relocated++;
                pulseOffset++;
            }
        }
        persistPlayerTracks(player);
        emitSelfBuffDissipate(player, removed, player.position().add(0.0D, 0.08D, 0.0D));
        return relocated;
    }

    public static void clearSelfBuff(ServerPlayer player) {
        clearSelfBuff(player, false);
    }

    public static void clearSelfBuff(ServerPlayer player, boolean emitDissipate) {
        if (player == null) {
            return;
        }
        List<SelfBuffEmission> removed = new ArrayList<>();
        synchronized (SELF_BUFFS) {
            var iterator = SELF_BUFFS.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<SelfBuffKey, SelfBuffTrack> entry = iterator.next();
                if (entry.getKey().playerId().equals(player.getUUID())) {
                    removed.add(new SelfBuffEmission(entry.getKey(), entry.getValue()));
                    PENDING_SELF_BUFF_STATUSES.remove(entry.getKey());
                    iterator.remove();
                }
            }
        }
        player.getPersistentData().remove(SELF_BUFF_DATA_KEY);
        if (!emitDissipate || removed.isEmpty()) {
            return;
        }
        emitSelfBuffDissipate(player, removed, player.position().add(0.0D, 0.08D, 0.0D));
    }

    /** Drops session-only tracks when a logical server stops; player metadata remains persisted. */
    public static void clearRuntimeState() {
        synchronized (SELF_BUFFS) {
            SELF_BUFFS.clear();
            PENDING_SELF_BUFF_STATUSES.clear();
            PENDING_SELF_BUFF_DISSIPATES.clear();
            lastTickServer = null;
            lastTickTime = Long.MIN_VALUE;
        }
    }

    static TechniqueVfxPalette.Family servitorFamily(SummonedServitorEntity.Archetype archetype) {
        return switch (archetype == null ? SummonedServitorEntity.Archetype.GENERIC : archetype) {
            case BEAST -> TechniqueVfxPalette.Family.WOOD;
            case PUPPET -> TechniqueVfxPalette.Family.METAL;
            case GHOST -> TechniqueVfxPalette.Family.SOUL;
            case GENERIC -> TechniqueVfxPalette.Family.NEUTRAL;
        };
    }

    static TechniqueVfxPacket.Motif servitorMotif(SummonedServitorEntity.Archetype archetype) {
        return switch (archetype == null ? SummonedServitorEntity.Archetype.GENERIC : archetype) {
            case BEAST -> TechniqueVfxPacket.Motif.DAO;
            case PUPPET -> TechniqueVfxPacket.Motif.BLADE;
            case GHOST -> TechniqueVfxPacket.Motif.GHOST;
            case GENERIC -> TechniqueVfxPacket.Motif.SUMMON;
        };
    }

    public static TechniqueVfxPacket.Motif selfBuffMotif(String effectType, String statusId) {
        String key = ((effectType == null ? "" : effectType) + " "
                + (statusId == null ? "" : statusId)).toLowerCase(Locale.ROOT);
        if (contains(key, "shield", "barrier", "guard", "armor")) {
            return TechniqueVfxPacket.Motif.SHIELD;
        }
        if (contains(key, "cleanse", "purify", "detox")) {
            return TechniqueVfxPacket.Motif.CLEANSE;
        }
        if (contains(key, "heal", "recover", "regeneration")) {
            return TechniqueVfxPacket.Motif.HEAL;
        }
        if (contains(key, "scan", "scout", "inspect", "sense")) {
            return TechniqueVfxPacket.Motif.CHANNEL;
        }
        if (contains(key, "movement", "dash", "escape", "teleport", "walk", "ride", "flight")) {
            return TechniqueVfxPacket.Motif.TELEPORT;
        }
        return TechniqueVfxPacket.Motif.DAO;
    }

    static int selfBuffStatusIntervalTicks() {
        return SELF_BUFF_STATUS_INTERVAL_TICKS;
    }

    static int maxTrackedSelfBuffs() {
        return MAX_TRACKED_SELF_BUFFS;
    }

    static int maxSelfBuffsPerPlayer() {
        return MAX_SELF_BUFFS_PER_PLAYER;
    }

    static int maxSelfBuffStatusPulsesPerTick() {
        return MAX_SELF_BUFF_STATUS_PULSES_PER_TICK;
    }

    static int maxSelfBuffStatusPulsesPerServerTick() {
        return MAX_SELF_BUFF_STATUS_PULSES_PER_SERVER_TICK;
    }

    static int maxSelfBuffDissipatesPerServerTick() {
        return MAX_SELF_BUFF_DISSIPATES_PER_SERVER_TICK;
    }

    static int maxPendingSelfBuffStatuses() {
        return MAX_PENDING_SELF_BUFF_STATUSES;
    }

    static int maxPendingSelfBuffDissipates() {
        return MAX_PENDING_SELF_BUFF_DISSIPATES;
    }

    static String selfBuffPersistentDataKey() {
        return SELF_BUFF_DATA_KEY;
    }

    private static boolean canTrack(ServerPlayer player) {
        return player != null && player.getServer() != null && !player.level().isClientSide;
    }

    private static long serverTime(ServerPlayer player) {
        return player.getServer().overworld().getGameTime();
    }

    private static long serverTime(MinecraftServer server) {
        return server.overworld().getGameTime();
    }

    static long boundedSelfBuffEndsAt(long now,
                                      int capturedRemainingTicks,
                                      int activeRemainingTicks,
                                      long persistedEndsAt) {
        if (capturedRemainingTicks <= 0 || activeRemainingTicks <= 0 || persistedEndsAt <= now) {
            return now;
        }
        long remainingTicks = Math.min(capturedRemainingTicks, activeRemainingTicks);
        long observedEndsAt = now > Long.MAX_VALUE - remainingTicks
                ? Long.MAX_VALUE : now + remainingTicks;
        return Math.min(observedEndsAt, persistedEndsAt);
    }

    static int boundedSelfBuffRemainingTicks(long now,
                                             int capturedRemainingTicks,
                                             int activeRemainingTicks,
                                             long endsAt) {
        if (capturedRemainingTicks <= 0 || activeRemainingTicks <= 0 || endsAt <= now) {
            return 0;
        }
        long deadlineRemaining = endsAt == Long.MAX_VALUE
                ? capturedRemainingTicks : endsAt - now;
        return (int)Math.min(Math.min(capturedRemainingTicks, activeRemainingTicks), deadlineRemaining);
    }

    private static ResourceLocation registeredEffectId(MobEffect effect) {
        ResourceLocation effectId = ForgeRegistries.MOB_EFFECTS.getKey(effect);
        return effectId != null && ForgeRegistries.MOB_EFFECTS.getValue(effectId) == effect ? effectId : null;
    }

    private static ResourceLocation parseEffectId(String rawId) {
        if (rawId == null || rawId.isBlank() || rawId.length() > MAX_EFFECT_ID_LENGTH) {
            return null;
        }
        return ResourceLocation.tryParse(rawId);
    }

    private static boolean isValidEffectId(ResourceLocation effectId) {
        return effectId != null
                && effectId.toString().length() <= MAX_EFFECT_ID_LENGTH
                && ForgeRegistries.MOB_EFFECTS.containsKey(effectId);
    }

    private static boolean hasActiveEffect(ServerPlayer player, ResourceLocation effectId) {
        return activeEffectDuration(player, effectId) > 0;
    }

    private static int activeEffectDuration(ServerPlayer player, ResourceLocation effectId) {
        if (player == null || !isValidEffectId(effectId)) {
            return 0;
        }
        MobEffect effect = ForgeRegistries.MOB_EFFECTS.getValue(effectId);
        MobEffectInstance active = effect == null ? null : player.getEffect(effect);
        return active == null ? 0 : Math.max(0, active.getDuration());
    }

    private static <E extends Enum<E>> E parseEnum(Class<E> type, String value) {
        if (type == null || value == null || value.isBlank() || value.length() > 32) {
            return null;
        }
        try {
            return Enum.valueOf(type, value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static int countPlayerTracks(UUID playerId) {
        int count = 0;
        for (SelfBuffKey key : SELF_BUFFS.keySet()) {
            if (key.playerId().equals(playerId)) {
                count++;
            }
        }
        return count;
    }

    private static int countPlayerTracksSafely(UUID playerId) {
        synchronized (SELF_BUFFS) {
            return countPlayerTracks(playerId);
        }
    }

    private static void removeRuntimeTracks(UUID playerId) {
        synchronized (SELF_BUFFS) {
            SELF_BUFFS.keySet().removeIf(key -> key.playerId().equals(playerId));
            PENDING_SELF_BUFF_STATUSES.removeIf(key -> key.playerId().equals(playerId));
        }
    }

    private static void pauseRuntimeTracks(ServerPlayer player) {
        long now = serverTime(player);
        synchronized (SELF_BUFFS) {
            var iterator = SELF_BUFFS.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<SelfBuffKey, SelfBuffTrack> entry = iterator.next();
                if (!entry.getKey().playerId().equals(player.getUUID())) {
                    continue;
                }
                SelfBuffTrack track = entry.getValue();
                int remainingTicks = boundedSelfBuffRemainingTicks(
                        now,
                        track.capturedRemainingTicks(),
                        activeEffectDuration(player, entry.getKey().effectId()),
                        track.endsAt());
                if (remainingTicks <= 0) {
                    PENDING_SELF_BUFF_STATUSES.remove(entry.getKey());
                    iterator.remove();
                } else {
                    entry.setValue(track.withPausedRemaining(remainingTicks));
                }
            }
        }
    }

    private static boolean hasPendingSelfBuffStatus(UUID playerId, SelfBuffVisualKey visualKey) {
        for (SelfBuffKey pendingKey : PENDING_SELF_BUFF_STATUSES) {
            SelfBuffTrack pendingTrack = SELF_BUFFS.get(pendingKey);
            if (pendingKey.playerId().equals(playerId)
                    && pendingTrack != null
                    && visualKey.equals(visualKey(pendingTrack))) {
                return true;
            }
        }
        return false;
    }

    private static void alignSelfBuffStatusPulses(UUID playerId,
                                                   SelfBuffVisualKey visualKey,
                                                   long now) {
        long nextPulseAt = now + SELF_BUFF_STATUS_INTERVAL_TICKS;
        for (Map.Entry<SelfBuffKey, SelfBuffTrack> entry : SELF_BUFFS.entrySet()) {
            if (entry.getKey().playerId().equals(playerId)
                    && visualKey.equals(visualKey(entry.getValue()))) {
                entry.setValue(entry.getValue().withNextPulse(nextPulseAt));
            }
        }
    }

    private static SelfBuffVisualKey visualKey(SelfBuffTrack track) {
        return new SelfBuffVisualKey(track.family(), track.motif());
    }

    private static boolean hasLiveVisualTrack(UUID playerId, SelfBuffVisualKey visualKey) {
        for (Map.Entry<SelfBuffKey, SelfBuffTrack> entry : SELF_BUFFS.entrySet()) {
            if (entry.getKey().playerId().equals(playerId)
                    && visualKey.equals(visualKey(entry.getValue()))) {
                return true;
            }
        }
        return false;
    }

    private static void persistPlayerTracks(ServerPlayer player) {
        if (player == null) {
            return;
        }
        List<Map.Entry<SelfBuffKey, SelfBuffTrack>> tracks = new ArrayList<>();
        synchronized (SELF_BUFFS) {
            for (Map.Entry<SelfBuffKey, SelfBuffTrack> entry : SELF_BUFFS.entrySet()) {
                if (entry.getKey().playerId().equals(player.getUUID())
                        && tracks.size() < MAX_SELF_BUFFS_PER_PLAYER) {
                    tracks.add(Map.entry(entry.getKey(), entry.getValue()));
                }
            }
        }
        CompoundTag persistentData = player.getPersistentData();
        if (tracks.isEmpty()) {
            persistentData.remove(SELF_BUFF_DATA_KEY);
            return;
        }
        ListTag savedTracks = new ListTag();
        for (Map.Entry<SelfBuffKey, SelfBuffTrack> entry : tracks) {
            CompoundTag saved = new CompoundTag();
            saved.putString(SEMANTIC_TAG, entry.getKey().semantic());
            saved.putString(EFFECT_TAG, entry.getKey().effectId().toString());
            saved.putString(FAMILY_TAG, entry.getValue().family().name());
            saved.putString(MOTIF_TAG, entry.getValue().motif().name());
            saved.putInt(CAPTURED_REMAINING_TAG, entry.getValue().capturedRemainingTicks());
            saved.putLong(ENDS_AT_TAG, entry.getValue().endsAt());
            savedTracks.add(saved);
        }
        CompoundTag root = new CompoundTag();
        root.put(TRACKS_TAG, savedTracks);
        persistentData.put(SELF_BUFF_DATA_KEY, root);
    }

    private static void emitSelfBuffDissipate(ServerPlayer player,
                                               List<SelfBuffEmission> removed,
                                               Vec3 center) {
        if (player == null || removed == null || removed.isEmpty() || center == null) {
            return;
        }
        Set<SelfBuffVisualKey> emitted = new HashSet<>();
        synchronized (SELF_BUFFS) {
            for (SelfBuffEmission emission : removed) {
                SelfBuffVisualKey visualKey = new SelfBuffVisualKey(
                        emission.track().family(), emission.track().motif());
                if (!emitted.add(visualKey)
                        || hasLiveVisualTrack(player.getUUID(), visualKey)
                        || hasPendingSelfBuffDissipate(player.getUUID(), visualKey)
                        || PENDING_SELF_BUFF_DISSIPATES.size() >= MAX_PENDING_SELF_BUFF_DISSIPATES) {
                    continue;
                }
                PENDING_SELF_BUFF_DISSIPATES.addLast(new PendingSelfBuffDissipate(
                        player.getServer(), player.getUUID(), player.serverLevel().dimension(),
                        emission.track().family(), emission.track().motif(), center,
                        player.getId() * 71L ^ emission.key().semantic().hashCode()
                                ^ emission.key().effectId().hashCode()));
            }
        }
    }

    private static boolean hasPendingSelfBuffDissipate(UUID playerId, SelfBuffVisualKey visualKey) {
        for (PendingSelfBuffDissipate pending : PENDING_SELF_BUFF_DISSIPATES) {
            if (pending.playerId().equals(playerId)
                    && pending.family() == visualKey.family()
                    && pending.motif() == visualKey.motif()) {
                return true;
            }
        }
        return false;
    }

    private static void send(ServerLevel level,
                             TechniqueVfxPacket.Kind kind,
                             TechniqueVfxPalette.Family family,
                             TechniqueVfxPacket.Motif motif,
                             Vec3 start,
                             Vec3 end,
                             double radius,
                             int intensity,
                             long seedSalt) {
        if (level == null || start == null) {
            return;
        }
        Vec3 safeEnd = end == null ? start : end;
        long seed = level.getGameTime() * 31L
                ^ Double.doubleToLongBits(start.x * 0.73D + start.y * 0.37D + start.z)
                ^ seedSalt;
        TechniqueVfxPacket.send(level, kind, family, motif,
                start, safeEnd, radius, intensity, seed);
    }

    private static boolean contains(String value, String... needles) {
        for (String needle : needles) {
            if (value.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private record SelfBuffKey(
            UUID playerId,
            String semantic,
            ResourceLocation effectId
    ) {}

    private record SelfBuffTrack(
            ResourceKey<Level> dimension,
            TechniqueVfxPalette.Family family,
            TechniqueVfxPacket.Motif motif,
            int capturedRemainingTicks,
            long endsAt,
            long nextPulseAt
    ) {
        private SelfBuffTrack withDimension(ResourceKey<Level> dimension) {
            return new SelfBuffTrack(
                    dimension, family, motif, capturedRemainingTicks, endsAt, nextPulseAt);
        }

        private SelfBuffTrack withNextPulse(long nextPulseAt) {
            return new SelfBuffTrack(
                    dimension, family, motif, capturedRemainingTicks, endsAt, nextPulseAt);
        }

        private SelfBuffTrack withPausedRemaining(int remainingTicks) {
            return new SelfBuffTrack(
                    dimension, family, motif, remainingTicks, Long.MAX_VALUE, nextPulseAt);
        }
    }

    private record SelfBuffEmission(SelfBuffKey key, SelfBuffTrack track) {}

    private record SelfBuffVisualKey(
            TechniqueVfxPalette.Family family,
            TechniqueVfxPacket.Motif motif
    ) {}

    private record SelfBuffOwnerVisualKey(
            UUID playerId,
            TechniqueVfxPalette.Family family,
            TechniqueVfxPacket.Motif motif
    ) {}

    private record SelfBuffStatusEmission(
            ServerPlayer player,
            SelfBuffKey key,
            SelfBuffTrack track
    ) {}

    private record PendingSelfBuffDissipate(
            MinecraftServer server,
            UUID playerId,
            ResourceKey<Level> dimension,
            TechniqueVfxPalette.Family family,
            TechniqueVfxPacket.Motif motif,
            Vec3 center,
            long seedSalt
    ) {}
}
