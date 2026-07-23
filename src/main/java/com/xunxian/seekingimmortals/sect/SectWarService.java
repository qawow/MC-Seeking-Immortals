package com.xunxian.seekingimmortals.sect;

import com.xunxian.seekingimmortals.entity.SummonedServitorEntity;
import com.xunxian.seekingimmortals.worldpack.ReputationService;
import com.xunxian.seekingimmortals.worldpack.TrialCombatShellService;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Multiplayer sect-war scoreboard (Wave49 Phase22 depth).
 * Server SavedData tracks faction scores during an open window.
 * Wave485: battlefield AI pulse — periodic pressure shells + kill scoring.
 * Wave487: multi-army (optional third faction), elapsed-fraction war phases,
 * dual-enemy pressure shells, and ally reinforcement servitors.
 */
public final class SectWarService {
    public static final String WAR_SHELL_TAG = "seeking_immortals_war_shell";
    public static final String WAR_SHELL_SIDE = "Side";
    public static final String WAR_SHELL_ALLY = "Ally";
    /** Persisted generation token that binds a shell to one war window. */
    public static final String WAR_SHELL_GENERATION = "WarGeneration";
    private static final String WAR_SCOPE_REGION = "ScopeRegion";
    private static final String WAR_SCOPE_DIMENSION = "ScopeDimension";
    private static final String WAR_SCOPES = "Scopes";
    private static final String SCOPE_REGION = "Region";
    private static final String SCOPE_DIMENSION = "Dimension";
    static final int MAX_DAILY_WAR_SCOPES = 32;
    private static final String LAST_PULSE_ROOT = "seeking_immortals_war_ai_pulse";
    private static final int AI_PULSE_INTERVAL_TICKS = 20 * 20; // 20s
    private static final int ALLY_LIFE_TICKS = 20 * 45;
    private static final Set<String> TIANNAN_SEVEN_SECT_IDS = Set.of(
            "huangfeng_valley",
            "yanyue_sect",
            "spirit_beast_mountain",
            "qingxu_gate",
            "huadao_wu",
            "tianque_fort",
            "giant_sword_gate");

    private SectWarService() {}

    record WarScope(String regionId, String dimensionId) {
        WarScope {
            regionId = normalizeOptional(regionId);
            dimensionId = normalizeOptional(dimensionId);
        }

        boolean isValid() {
            return !regionId.isBlank() && !dimensionId.isBlank();
        }
    }

    public static final class WarData extends SavedData {
        private static final String NAME = "seeking_immortals_sect_war";
        // Package-visible for unit tests of phase/army helpers.
        boolean active;
        String factionA = "qinglan";
        String factionB = "lingxiao";
        /** Optional third army id; blank means dual war. */
        String factionC = "";
        int scoreA;
        int scoreB;
        int scoreC;
        long startedAtGameTime;
        long endsAtGameTime;
        long lastAiPulseGameTime;
        /** True only for the daily-event-owned global scoreboard. */
        boolean dailyEventOwned;
        /** Monotonic token; every newly started war receives a new generation. */
        long generation;
        /** Empty for command/manual wars; daily wars are scoped to both values. */
        String scopeRegionId = "";
        String scopeDimensionId = "";
        final LinkedHashSet<WarScope> scopes = new LinkedHashSet<>();

        public static WarData get(ServerLevel overworld) {
            return overworld.getDataStorage().computeIfAbsent(WarData::load, WarData::new, NAME);
        }

        public static WarData load(CompoundTag tag) {
            WarData data = new WarData();
            data.active = tag.getBoolean("Active");
            data.factionA = tag.getString("A");
            data.factionB = tag.getString("B");
            data.factionC = tag.contains("C") ? tag.getString("C") : "";
            data.scoreA = tag.getInt("ScoreA");
            data.scoreB = tag.getInt("ScoreB");
            data.scoreC = tag.getInt("ScoreC");
            data.startedAtGameTime = tag.getLong("Started");
            data.endsAtGameTime = tag.getLong("Ends");
            data.lastAiPulseGameTime = tag.getLong("LastAiPulse");
            data.dailyEventOwned = tag.getBoolean("DailyEventOwned");
            data.generation = tag.getLong("Generation");
            data.scopeRegionId = tag.contains(WAR_SCOPE_REGION) ? tag.getString(WAR_SCOPE_REGION) : "";
            data.scopeDimensionId = tag.contains(WAR_SCOPE_DIMENSION) ? tag.getString(WAR_SCOPE_DIMENSION) : "";
            addDailyScope(data, data.scopeRegionId, data.scopeDimensionId);
            ListTag scopes = tag.getList(WAR_SCOPES, Tag.TAG_COMPOUND);
            for (int i = 0; i < scopes.size() && data.scopes.size() < MAX_DAILY_WAR_SCOPES; i++) {
                CompoundTag scope = scopes.getCompound(i);
                addDailyScope(data, scope.getString(SCOPE_REGION), scope.getString(SCOPE_DIMENSION));
            }
            syncLegacyScope(data);
            // Legacy dual-war saves without Started: approximate from ends window.
            if (data.startedAtGameTime <= 0L && data.endsAtGameTime > 0L) {
                data.startedAtGameTime = Math.max(0L, data.endsAtGameTime - 20L * 60L * 10L);
            }
            // A legacy active save has no token. Give the current window a token
            // so newly spawned shells can be bound while old shells fail closed.
            if (data.active && data.generation <= 0L) {
                data.generation = 1L;
            }
            return data;
        }

        @Override
        public CompoundTag save(CompoundTag tag) {
            tag.putBoolean("Active", active);
            tag.putString("A", factionA);
            tag.putString("B", factionB);
            tag.putString("C", factionC == null ? "" : factionC);
            tag.putInt("ScoreA", scoreA);
            tag.putInt("ScoreB", scoreB);
            tag.putInt("ScoreC", scoreC);
            tag.putLong("Started", startedAtGameTime);
            tag.putLong("Ends", endsAtGameTime);
            tag.putLong("LastAiPulse", lastAiPulseGameTime);
            tag.putBoolean("DailyEventOwned", dailyEventOwned);
            tag.putLong("Generation", generation);
            addDailyScope(this, scopeRegionId, scopeDimensionId);
            syncLegacyScope(this);
            tag.putString(WAR_SCOPE_REGION, scopeRegionId);
            tag.putString(WAR_SCOPE_DIMENSION, scopeDimensionId);
            ListTag scopeList = new ListTag();
            for (WarScope scope : scopes) {
                CompoundTag scopeTag = new CompoundTag();
                scopeTag.putString(SCOPE_REGION, scope.regionId());
                scopeTag.putString(SCOPE_DIMENSION, scope.dimensionId());
                scopeList.add(scopeTag);
            }
            tag.put(WAR_SCOPES, scopeList);
            return tag;
        }

        public boolean hasThirdArmy() {
            return factionC != null && !factionC.isBlank();
        }

        public int armyCount() {
            return hasThirdArmy() ? 3 : 2;
        }
    }

    public static boolean start(MinecraftServer server, String factionA, String factionB, int minutes) {
        return start(server, factionA, factionB, "", minutes);
    }

    public static boolean start(MinecraftServer server, String factionA, String factionB, String factionC, int minutes) {
        if (server == null) {
            return false;
        }
        long now = server.overworld().getGameTime();
        return startAt(server, factionA, factionB, factionC,
                now + Math.max(1, minutes) * 20L * 60L, false, "", "");
    }

    /** Starts the global scoreboard once for a daily conflict without resetting it per player. */
    public static boolean ensureStarted(MinecraftServer server, String factionA, String factionB,
                                        long untilGameTime) {
        return ensureStarted(server, factionA, factionB, untilGameTime, "", "");
    }

    /** Starts one region/dimension-scoped daily conflict without resetting it per player. */
    public static boolean ensureStarted(MinecraftServer server, String factionA, String factionB,
                                        long untilGameTime, String regionId, String dimensionId) {
        if (server == null) {
            return false;
        }
        ServerLevel overworld = server.overworld();
        WarData data = WarData.get(overworld);
        long now = overworld.getGameTime();
        String wantedA = normalize(factionA);
        String wantedB = normalize(factionB);
        String wantedRegion = normalizeOptional(regionId);
        String wantedDimension = normalizeOptional(dimensionId);
        if (data.active && !isExpired(data, now)) {
            int previousScopeCount = data.scopes.size();
            long previousEnd = data.endsAtGameTime;
            boolean ensured = mergeDailyWarScope(data, wantedA, wantedB, wantedRegion, wantedDimension,
                    Math.max(now + 1L, untilGameTime), now);
            if (ensured && (data.scopes.size() != previousScopeCount || data.endsAtGameTime != previousEnd)) {
                data.setDirty();
            }
            // A single global scoreboard cannot represent two simultaneous
            // faction pairs. Keep the first active war intact instead of
            // resetting scores for a later player's refresh.
            return ensured;
        }
        if (isExpired(data, now)) {
            stop(server);
        }
        return startAt(server, wantedA, wantedB, "", Math.max(now + 1L, untilGameTime), true,
                wantedRegion, wantedDimension);
    }

    private static boolean startAt(MinecraftServer server, String factionA, String factionB,
                                   String factionC, long endsAtGameTime, boolean dailyEventOwned,
                                   String scopeRegionId, String scopeDimensionId) {
        if (server == null) {
            return false;
        }
        ServerLevel overworld = server.overworld();
        WarData data = WarData.get(overworld);
        data.generation = nextGeneration(data.generation);
        data.active = true;
        data.factionA = normalize(factionA);
        data.factionB = normalize(factionB);
        String c = normalizeOptional(factionC);
        if (!c.isBlank() && (c.equals(data.factionA) || c.equals(data.factionB))) {
            c = "";
        }
        data.factionC = c;
        data.scoreA = 0;
        data.scoreB = 0;
        data.scoreC = 0;
        long now = overworld.getGameTime();
        data.startedAtGameTime = now;
        data.endsAtGameTime = Math.max(now + 1L, endsAtGameTime);
        data.lastAiPulseGameTime = 0L;
        data.dailyEventOwned = dailyEventOwned;
        data.scopeRegionId = dailyEventOwned ? normalizeOptional(scopeRegionId) : "";
        data.scopeDimensionId = dailyEventOwned ? normalizeOptional(scopeDimensionId) : "";
        data.scopes.clear();
        if (dailyEventOwned) {
            addDailyScope(data, data.scopeRegionId, data.scopeDimensionId);
        }
        data.setDirty();
        int minutes = (int) Math.max(1L, (data.endsAtGameTime - now + 1199L) / 1200L);
        if (data.hasThirdArmy()) {
            broadcast(server, Component.translatable("message.seeking_immortals.sect_war.started_triple",
                    data.factionA, data.factionB, data.factionC, minutes));
        } else {
            broadcast(server, Component.translatable("message.seeking_immortals.sect_war.started",
                    data.factionA, data.factionB, minutes));
        }
        return true;
    }

    static boolean sameArmies(WarData data, String factionA, String factionB, String factionC) {
        if (data == null) {
            return false;
        }
        String a = canonicalWarFaction(factionA);
        String b = canonicalWarFaction(factionB);
        String c = canonicalWarFaction(factionC);
        String dataA = canonicalWarFaction(data.factionA);
        String dataB = canonicalWarFaction(data.factionB);
        boolean samePair = (a.equals(dataA) && b.equals(dataB))
                || (a.equals(dataB) && b.equals(dataA));
        return samePair && c.equals(canonicalWarFaction(data.factionC));
    }

    /** A war owns its end tick; equality is already outside the active window. */
    static boolean isExpired(WarData data, long nowGameTime) {
        return data != null && data.active && nowGameTime >= data.endsAtGameTime;
    }

    static long nextGeneration(long currentGeneration) {
        return currentGeneration <= 0L || currentGeneration == Long.MAX_VALUE
                ? 1L : currentGeneration + 1L;
    }

    /**
     * Accepts another scope only for the currently active daily war with the
     * same armies. Scores, start time, and generation are intentionally shared.
     */
    static boolean mergeDailyWarScope(WarData data, String factionA, String factionB,
                                      String regionId, String dimensionId,
                                      long desiredEndGameTime, long nowGameTime) {
        if (data == null || !data.active || isExpired(data, nowGameTime) || !data.dailyEventOwned
                || !sameArmies(data, factionA, factionB, "")) {
            return false;
        }
        boolean legacyUnscoped = data.scopes.isEmpty()
                && normalizeOptional(data.scopeRegionId).isBlank()
                && normalizeOptional(data.scopeDimensionId).isBlank()
                && normalizeOptional(regionId).isBlank()
                && normalizeOptional(dimensionId).isBlank();
        if (!legacyUnscoped && !addDailyScope(data, regionId, dimensionId)) {
            return false;
        }
        if (desiredEndGameTime > data.endsAtGameTime) {
            data.endsAtGameTime = desiredEndGameTime;
        }
        return true;
    }

    /** Returns true when the normalized scope already exists or was appended. */
    static boolean addDailyScope(WarData data, String regionId, String dimensionId) {
        if (data == null) {
            return false;
        }
        migrateLegacyScope(data);
        WarScope scope = new WarScope(regionId, dimensionId);
        if (!scope.isValid()) {
            return false;
        }
        if (data.scopes.contains(scope)) {
            return true;
        }
        if (data.scopes.size() >= MAX_DAILY_WAR_SCOPES) {
            return false;
        }
        data.scopes.add(scope);
        syncLegacyScope(data);
        return true;
    }

    private static void migrateLegacyScope(WarData data) {
        if (data == null || !data.scopes.isEmpty()) {
            return;
        }
        WarScope legacy = new WarScope(data.scopeRegionId, data.scopeDimensionId);
        if (legacy.isValid()) {
            data.scopes.add(legacy);
        }
    }

    private static void syncLegacyScope(WarData data) {
        if (data == null || data.scopes.isEmpty()) {
            return;
        }
        WarScope first = data.scopes.iterator().next();
        data.scopeRegionId = first.regionId();
        data.scopeDimensionId = first.dimensionId();
    }

    static boolean sameDailyScope(WarData data, String regionId, String dimensionId) {
        if (data == null) {
            return false;
        }
        WarScope wanted = new WarScope(regionId, dimensionId);
        if (!wanted.isValid()) {
            return false;
        }
        if (!data.scopes.isEmpty()) {
            return data.scopes.contains(wanted);
        }
        return wanted.regionId().equals(normalizeOptional(data.scopeRegionId))
                && wanted.dimensionId().equals(normalizeOptional(data.scopeDimensionId));
    }

    /** Manual wars are global; daily wars fail closed outside their saved scope. */
    static boolean isInWarScope(WarData data, String regionId, String dimensionId) {
        if (data == null) {
            return false;
        }
        if (!data.dailyEventOwned) {
            return true;
        }
        WarScope current = new WarScope(regionId, dimensionId);
        if (!current.isValid()) {
            return false;
        }
        if (!data.scopes.isEmpty()) {
            return data.scopes.contains(current);
        }
        String expectedRegion = normalizeOptional(data.scopeRegionId);
        String expectedDimension = normalizeOptional(data.scopeDimensionId);
        return (expectedRegion.isBlank() || expectedRegion.equals(current.regionId()))
                && (expectedDimension.isBlank() || expectedDimension.equals(current.dimensionId()));
    }

    static boolean warShellGenerationMatches(CompoundTag shellTag, WarData data) {
        return shellTag != null && data != null && data.active && data.generation > 0L
                && shellTag.contains(WAR_SHELL_GENERATION)
                && shellTag.getLong(WAR_SHELL_GENERATION) == data.generation;
    }

    public static boolean stop(MinecraftServer server) {
        ServerLevel overworld = server.overworld();
        WarData data = WarData.get(overworld);
        if (!data.active) {
            return false;
        }
        data.active = false;
        data.dailyEventOwned = false;
        data.scopeRegionId = "";
        data.scopeDimensionId = "";
        data.scopes.clear();
        data.setDirty();
        String winner = winnerOf(data);
        if (data.hasThirdArmy()) {
            broadcast(server, Component.translatable("message.seeking_immortals.sect_war.ended_triple",
                    data.factionA, data.scoreA, data.factionB, data.scoreB, data.factionC, data.scoreC, winner));
        } else {
            broadcast(server, Component.translatable("message.seeking_immortals.sect_war.ended",
                    data.factionA, data.scoreA, data.factionB, data.scoreB, winner));
        }
        return true;
    }

    /** Stops only the scoreboard created by daily-event conflict semantics. */
    public static boolean stopDailyEventWar(MinecraftServer server) {
        if (server == null) {
            return false;
        }
        WarData data = WarData.get(server.overworld());
        if (!data.active || !data.dailyEventOwned) {
            return false;
        }
        return stop(server);
    }

    public static void onKill(ServerPlayer killer, ServerPlayer victim) {
        if (killer == null || victim == null || killer.level().isClientSide) {
            return;
        }
        contribute(killer, 1, true);
    }

    /**
     * Wave479: authority contribution from conflict side-pick / war actions (not only PvP kills).
     * sideToken matches factionA/B/C ids (or contains them); falls back to player sect membership.
     */
    public static boolean contributeForFaction(ServerPlayer player, String sideToken, int points) {
        if (player == null || player.level().isClientSide || points <= 0) {
            return false;
        }
        MinecraftServer server = player.server;
        WarData data = WarData.get(server.overworld());
        if (!data.active) {
            return false;
        }
        if (isExpired(data, server.overworld().getGameTime())) {
            stop(server);
            return false;
        }
        if (!isPlayerInWarScope(player, data)) {
            return false;
        }
        String token = normalizeOptional(sideToken);
        String side = sideFromToken(token, data);
        if (side.isBlank()) {
            side = sideOf(player, data);
        }
        if (side.isBlank()) {
            return false;
        }
        addScore(data, side, points);
        data.setDirty();
        ReputationService.add(player, factionOf(data, side), Math.max(1, points));
        sendScoreMessage(player, data);
        return true;
    }

    public static boolean isActive(MinecraftServer server) {
        if (server == null) {
            return false;
        }
        WarData data = WarData.get(server.overworld());
        if (!data.active) {
            return false;
        }
        if (isExpired(data, server.overworld().getGameTime())) {
            stop(server);
            return false;
        }
        return true;
    }

    private static void contribute(ServerPlayer killer, int points, boolean fromKill) {
        if (killer == null || killer.level().isClientSide) {
            return;
        }
        MinecraftServer server = killer.server;
        WarData data = WarData.get(server.overworld());
        if (!data.active) {
            return;
        }
        if (isExpired(data, server.overworld().getGameTime())) {
            stop(server);
            return;
        }
        if (!isPlayerInWarScope(killer, data)) {
            return;
        }
        String side = sideOf(killer, data);
        if (side.isBlank()) {
            return;
        }
        addScore(data, side, Math.max(1, points));
        data.setDirty();
        ReputationService.add(killer, factionOf(data, side), fromKill ? 2 : Math.max(1, points));
        sendScoreMessage(killer, data);
    }

    public static String status(MinecraftServer server) {
        if (!isActive(server)) {
            return "inactive";
        }
        WarData data = WarData.get(server.overworld());
        long remain = Math.max(0L, data.endsAtGameTime - server.overworld().getGameTime());
        int phase = warPhase(data, server.overworld().getGameTime());
        String base = data.factionA + "=" + data.scoreA + " vs " + data.factionB + "=" + data.scoreB;
        if (data.hasThirdArmy()) {
            base += " vs " + data.factionC + "=" + data.scoreC;
        }
        return base + " | phase=" + phase + " | remainTicks=" + remain;
    }

    /**
     * Wave485/487: battlefield AI pulse for a participating player.
     * Spawns enemy pressure shells from every opposing army and occasionally
     * deploys an ally reinforcement servitor.
     */
    public static void tickBattlefieldAi(ServerPlayer player) {
        if (player == null || player.level().isClientSide || player.server == null) {
            return;
        }
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        MinecraftServer server = player.server;
        if (!isActive(server)) {
            return;
        }
        WarData data = WarData.get(server.overworld());
        if (!isPlayerInWarScope(player, data)) {
            return;
        }
        String side = sideOf(player, data);
        if (side.isBlank()) {
            return;
        }
        long now = server.overworld().getGameTime();
        if (data.lastAiPulseGameTime > 0L && now - data.lastAiPulseGameTime < AI_PULSE_INTERVAL_TICKS) {
            long lastPersonal = player.getPersistentData().getLong(LAST_PULSE_ROOT);
            if (now - lastPersonal < AI_PULSE_INTERVAL_TICKS) {
                return;
            }
        } else {
            data.lastAiPulseGameTime = now;
            data.setDirty();
        }
        long lastPersonal = player.getPersistentData().getLong(LAST_PULSE_ROOT);
        if (now - lastPersonal < AI_PULSE_INTERVAL_TICKS) {
            return;
        }
        player.getPersistentData().putLong(LAST_PULSE_ROOT, now);

        int phase = warPhase(data, now); // 1 early, 2 mid, 3 late
        List<String> enemies = enemyFactions(data, side);
        if (enemies.isEmpty()) {
            return;
        }
        double health = 24.0D + phase * 8.0D;
        double damage = 4.0D + phase * 1.5D;
        int spawned = 0;
        // Multi-army pressure: each opposing army contributes shells; late war denser.
        int perEnemy = Math.max(1, phase - (enemies.size() > 1 ? 0 : 0));
        if (enemies.size() == 1) {
            perEnemy = phase; // keep dual-war intensity
        } else if (phase >= 3) {
            perEnemy = 2;
        }
        int shellIndex = 0;
        for (String enemyToken : enemies) {
            for (int i = 0; i < perEnemy; i++) {
                int ox = 3 + level.random.nextInt(4 + phase);
                int oz = 3 + level.random.nextInt(4 + phase);
                if (level.random.nextBoolean()) {
                    ox = -ox;
                }
                if (level.random.nextBoolean()) {
                    oz = -oz;
                }
                BlockPos pos = player.blockPosition().offset(ox, 0, oz);
                SummonedServitorEntity.Archetype archetype = TrialCombatShellService.archetypeFor(enemyToken);
                if (phase >= 3 && shellIndex == 0) {
                    archetype = SummonedServitorEntity.Archetype.PUPPET;
                } else if (phase >= 2 && i == perEnemy - 1) {
                    archetype = SummonedServitorEntity.Archetype.GHOST;
                }
                Mob shell = TrialCombatShellService.spawnHostile(
                        level, pos, player.getYRot(), "war_" + enemyToken + "_p" + phase, health, damage, archetype);
                if (shell == null) {
                    continue;
                }
                shell.setCustomName(Component.translatable("entity.seeking_immortals.war_shell.name", enemyToken));
                shell.setCustomNameVisible(true);
                shell.setTarget(player);
                CompoundTag tag = shell.getPersistentData().getCompound(WAR_SHELL_TAG).copy();
                tag.putString(WAR_SHELL_SIDE, side);
                tag.putLong(WAR_SHELL_GENERATION, data.generation);
                tag.putString("EnemyFaction", enemyToken);
                tag.putInt("Phase", phase);
                shell.getPersistentData().put(WAR_SHELL_TAG, tag);
                spawned++;
                shellIndex++;
            }
        }

        // Ally reinforcement: mid/late war, one owned servitor that fights for the player.
        if (phase >= 2 && level.random.nextFloat() < (phase >= 3 ? 0.65F : 0.4F)) {
            if (spawnAllyReinforcement(level, player, data, side, phase)) {
                player.displayClientMessage(Component.translatable(
                        "message.seeking_immortals.sect_war.ally_pulse", factionOf(data, side), phase), true);
            }
        }

        if (spawned > 0) {
            String enemyLabel = String.join("/", enemies);
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.sect_war.ai_pulse_phase", enemyLabel, spawned, phase), true);
            if (enemies.size() > 1) {
                player.displayClientMessage(Component.translatable(
                        "message.seeking_immortals.sect_war.multi_army", data.armyCount(), enemies.size()), true);
            }
        }
    }

    private static boolean spawnAllyReinforcement(
            ServerLevel level, ServerPlayer player, WarData data, String side, int phase) {
        SummonedServitorEntity ally = com.xunxian.seekingimmortals.registry.ModEntities.SUMMONED_SERVITOR.get().create(level);
        if (ally == null) {
            return false;
        }
        int ox = 1 + level.random.nextInt(2);
        int oz = 1 + level.random.nextInt(2);
        if (level.random.nextBoolean()) {
            ox = -ox;
        }
        BlockPos pos = player.blockPosition().offset(ox, 0, oz);
        ally.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, player.getYRot(), 0.0F);
        SummonedServitorEntity.Archetype archetype = phase >= 3
                ? SummonedServitorEntity.Archetype.PUPPET
                : SummonedServitorEntity.Archetype.BEAST;
        double health = 20.0D + phase * 6.0D;
        double damage = 3.5D + phase;
        ally.configure(player, "war_ally_" + factionOf(data, side), ALLY_LIFE_TICKS, health, damage, archetype);
        ally.setCustomName(Component.translatable("entity.seeking_immortals.war_ally.name", factionOf(data, side)));
        ally.setCustomNameVisible(true);
        CompoundTag tag = ally.getPersistentData().getCompound(WAR_SHELL_TAG).copy();
        tag.putString(WAR_SHELL_SIDE, side);
        tag.putLong(WAR_SHELL_GENERATION, data.generation);
        tag.putBoolean(WAR_SHELL_ALLY, true);
        tag.putInt("Phase", phase);
        ally.getPersistentData().put(WAR_SHELL_TAG, tag);
        level.addFreshEntity(ally);
        return true;
    }

    /**
     * 1 early / 2 mid / 3 late based on elapsed fraction of the war window.
     * Falls back to remaining-time buckets for legacy saves without a reliable start stamp.
     */
    public static int warPhase(WarData data, long nowGameTime) {
        if (data == null || !data.active) {
            return 1;
        }
        long start = data.startedAtGameTime;
        long end = data.endsAtGameTime;
        if (start > 0L && end > start) {
            double progress = (double) (nowGameTime - start) / (double) (end - start);
            if (progress < 0.34D) {
                return 1;
            }
            if (progress < 0.70D) {
                return 2;
            }
            return 3;
        }
        long remain = Math.max(0L, end - nowGameTime);
        if (remain > 20L * 60L * 6L) {
            return 1;
        }
        if (remain > 20L * 60L * 2L) {
            return 2;
        }
        return 3;
    }

    public static boolean isWarShell(Mob mob) {
        return mob != null && mob.getPersistentData().contains(WAR_SHELL_TAG)
                && !mob.getPersistentData().getCompound(WAR_SHELL_TAG).getBoolean(WAR_SHELL_ALLY);
    }

    public static void onWarShellKilled(ServerPlayer killer, Mob shell) {
        if (killer == null || shell == null || !isWarShell(shell) || killer.server == null) {
            return;
        }
        if (!isActive(killer.server)) {
            return;
        }
        CompoundTag shellTag = shell.getPersistentData().getCompound(WAR_SHELL_TAG);
        WarData data = WarData.get(killer.server.overworld());
        if (!warShellGenerationMatches(shellTag, data) || !isPlayerInWarScope(killer, data)) {
            return;
        }
        int phase = Math.max(1, shellTag.getInt("Phase"));
        String side = sideOf(killer, data);
        if (side.isBlank()) {
            return;
        }
        // Wave486: kill-streak bonus (resets after 15s idle).
        long now = killer.server.overworld().getGameTime();
        long lastKill = killer.getPersistentData().getLong("seeking_immortals_war_last_kill");
        int streak = killer.getPersistentData().getInt("seeking_immortals_war_streak");
        if (lastKill <= 0L || now - lastKill > 20L * 15L) {
            streak = 1;
        } else {
            streak = Math.min(8, streak + 1);
        }
        killer.getPersistentData().putLong("seeking_immortals_war_last_kill", now);
        killer.getPersistentData().putInt("seeking_immortals_war_streak", streak);
        // Wave487: multi-army bonus when shell came from a named third-party enemy.
        String enemyFaction = shellTag.getString("EnemyFaction");
        int multiBonus = 0;
        if (data.hasThirdArmy() && !enemyFaction.isBlank()) {
            multiBonus = 1;
        }
        int points = 1 + (phase - 1) + (streak >= 3 ? 1 : 0) + (streak >= 5 ? 1 : 0) + multiBonus;

        addScore(data, side, points);
        ReputationService.add(killer, factionOf(data, side), points);
        data.setDirty();
        sendScoreMessage(killer, data);
        if (streak >= 3) {
            killer.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.sect_war.streak", streak, points), true);
        }
    }

    private static List<String> enemyFactions(WarData data, String side) {
        List<String> enemies = new ArrayList<>(2);
        if (!"A".equals(side)) {
            enemies.add(data.factionA);
        }
        if (!"B".equals(side)) {
            enemies.add(data.factionB);
        }
        if (data.hasThirdArmy() && !"C".equals(side)) {
            enemies.add(data.factionC);
        }
        return enemies;
    }

    private static void addScore(WarData data, String side, int points) {
        int p = Math.max(1, points);
        switch (side) {
            case "A" -> data.scoreA += p;
            case "B" -> data.scoreB += p;
            case "C" -> data.scoreC += p;
            default -> {
            }
        }
    }

    private static String factionOf(WarData data, String side) {
        return switch (side) {
            case "A" -> data.factionA;
            case "B" -> data.factionB;
            case "C" -> data.factionC;
            default -> "";
        };
    }

    private static String winnerOf(WarData data) {
        int best = data.scoreA;
        String winner = data.factionA;
        boolean draw = false;
        if (data.scoreB > best) {
            best = data.scoreB;
            winner = data.factionB;
            draw = false;
        } else if (data.scoreB == best) {
            draw = true;
        }
        if (data.hasThirdArmy()) {
            if (data.scoreC > best) {
                return data.factionC;
            }
            if (data.scoreC == best) {
                draw = true;
            }
        }
        return draw ? "draw" : winner;
    }

    private static void sendScoreMessage(ServerPlayer player, WarData data) {
        if (data.hasThirdArmy()) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.sect_war.score_triple",
                    data.factionA, data.scoreA, data.factionB, data.scoreB, data.factionC, data.scoreC), true);
        } else {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.sect_war.score",
                    data.factionA, data.scoreA, data.factionB, data.scoreB), true);
        }
    }

    private static String sideFromToken(String token, WarData data) {
        if (token == null || token.isBlank()) {
            return "";
        }
        if (factionMatches(token, data.factionA)) {
            return "A";
        }
        if (factionMatches(token, data.factionB)) {
            return "B";
        }
        if (data.hasThirdArmy() && factionMatches(token, data.factionC)) {
            return "C";
        }
        return "";
    }

    private static String sideOf(ServerPlayer player, WarData data) {
        String[] sect = {""};
        com.xunxian.seekingimmortals.cultivation.CultivationHelper.get(player).ifPresent(cultivation ->
                sect[0] = cultivation.getSevenMysteriesQuest().getSectId());
        if (sect[0] == null || sect[0].isBlank()) {
            return "";
        }
        String s = sect[0].toLowerCase(Locale.ROOT);
        if (factionMatches(s, data.factionA)) {
            return "A";
        }
        if (factionMatches(s, data.factionB)) {
            return "B";
        }
        if (data.hasThirdArmy() && factionMatches(s, data.factionC)) {
            return "C";
        }
        return "";
    }

    static boolean factionMatches(String participantFaction, String warFaction) {
        String participant = canonicalWarFaction(participantFaction);
        String faction = canonicalWarFaction(warFaction);
        if (participant.isBlank() || faction.isBlank()) {
            return false;
        }
        if ("tiannan_seven_sects".equals(faction)) {
            return TIANNAN_SEVEN_SECT_IDS.contains(participant);
        }
        if ("tiannan_seven_sects".equals(participant)) {
            return TIANNAN_SEVEN_SECT_IDS.contains(faction);
        }
        if (participant.equals(faction)) {
            return true;
        }
        // Preserve compatibility with older qualified faction tokens after
        // explicit aliases and aggregate armies have been resolved.
        return participant.contains(faction) || faction.contains(participant);
    }

    private static String canonicalWarFaction(String id) {
        String normalized = normalizeOptional(id);
        if (normalized.isBlank()) {
            return "";
        }
        if ("mulan_council".equals(normalized) || "mulan_fashi".equals(normalized)) {
            return "mulan_fashi_council";
        }
        if ("tiannan_alliance".equals(normalized)) {
            return "tiannan_seven_sects";
        }
        return SectDefinitionService.canonicalizeSectId(normalized);
    }

    private static boolean isPlayerInWarScope(ServerPlayer player, WarData data) {
        if (player == null || data == null || !data.dailyEventOwned) {
            return player != null && data != null;
        }
        String regionId = com.xunxian.seekingimmortals.cultivation.CultivationHelper.get(player)
                .map(cultivation -> cultivation.getWorldpackCurrentRegionId())
                .orElse("");
        String dimensionId = player.level().dimension().location().toString();
        return isInWarScope(data, regionId, dimensionId);
    }

    private static void broadcast(MinecraftServer server, Component message) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.displayClientMessage(message, false);
        }
    }

    private static String normalize(String id) {
        return id == null || id.isBlank() ? "qinglan" : id.trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizeOptional(String id) {
        return id == null ? "" : id.trim().toLowerCase(Locale.ROOT);
    }
}
