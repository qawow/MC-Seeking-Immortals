package com.xunxian.seekingimmortals.sect;

import com.xunxian.seekingimmortals.entity.SummonedServitorEntity;
import com.xunxian.seekingimmortals.worldpack.ReputationService;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.Locale;

/**
 * Lightweight multiplayer sect-war scoreboard (Wave49 Phase22 depth).
 * Server SavedData tracks two faction scores during an open window.
 * Wave485: battlefield AI pulse — periodic pressure shells + kill scoring.
 */
public final class SectWarService {
    public static final String WAR_SHELL_TAG = "seeking_immortals_war_shell";
    public static final String WAR_SHELL_SIDE = "Side";
    private static final String LAST_PULSE_ROOT = "seeking_immortals_war_ai_pulse";
    private static final int AI_PULSE_INTERVAL_TICKS = 20 * 20; // 20s

    private SectWarService() {}

    public static final class WarData extends SavedData {
        private static final String NAME = "seeking_immortals_sect_war";
        private boolean active;
        private String factionA = "qinglan";
        private String factionB = "lingxiao";
        private int scoreA;
        private int scoreB;
        private long endsAtGameTime;
        private long lastAiPulseGameTime;

        public static WarData get(ServerLevel overworld) {
            return overworld.getDataStorage().computeIfAbsent(WarData::load, WarData::new, NAME);
        }

        public static WarData load(CompoundTag tag) {
            WarData data = new WarData();
            data.active = tag.getBoolean("Active");
            data.factionA = tag.getString("A");
            data.factionB = tag.getString("B");
            data.scoreA = tag.getInt("ScoreA");
            data.scoreB = tag.getInt("ScoreB");
            data.endsAtGameTime = tag.getLong("Ends");
            data.lastAiPulseGameTime = tag.getLong("LastAiPulse");
            return data;
        }

        @Override
        public CompoundTag save(CompoundTag tag) {
            tag.putBoolean("Active", active);
            tag.putString("A", factionA);
            tag.putString("B", factionB);
            tag.putInt("ScoreA", scoreA);
            tag.putInt("ScoreB", scoreB);
            tag.putLong("Ends", endsAtGameTime);
            tag.putLong("LastAiPulse", lastAiPulseGameTime);
            return tag;
        }
    }

    public static boolean start(MinecraftServer server, String factionA, String factionB, int minutes) {
        ServerLevel overworld = server.overworld();
        WarData data = WarData.get(overworld);
        data.active = true;
        data.factionA = normalize(factionA);
        data.factionB = normalize(factionB);
        data.scoreA = 0;
        data.scoreB = 0;
        data.endsAtGameTime = overworld.getGameTime() + Math.max(1, minutes) * 20L * 60L;
        data.lastAiPulseGameTime = 0L;
        data.setDirty();
        broadcast(server, Component.translatable("message.seeking_immortals.sect_war.started",
                data.factionA, data.factionB, minutes));
        return true;
    }

    public static boolean stop(MinecraftServer server) {
        ServerLevel overworld = server.overworld();
        WarData data = WarData.get(overworld);
        if (!data.active) {
            return false;
        }
        data.active = false;
        data.setDirty();
        String winner = data.scoreA == data.scoreB ? "draw"
                : (data.scoreA > data.scoreB ? data.factionA : data.factionB);
        broadcast(server, Component.translatable("message.seeking_immortals.sect_war.ended",
                data.factionA, data.scoreA, data.factionB, data.scoreB, winner));
        return true;
    }

    public static void onKill(ServerPlayer killer, ServerPlayer victim) {
        if (killer == null || victim == null || killer.level().isClientSide) {
            return;
        }
        contribute(killer, 1, true);
    }

    /**
     * Wave479: authority contribution from conflict side-pick / war actions (not only PvP kills).
     * sideToken matches factionA/factionB ids (or contains them); falls back to player sect membership.
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
        if (server.overworld().getGameTime() > data.endsAtGameTime) {
            stop(server);
            return false;
        }
        String token = normalize(sideToken);
        String side = "";
        if (!token.isBlank()) {
            if (token.contains(data.factionA) || data.factionA.contains(token)) {
                side = "A";
            } else if (token.contains(data.factionB) || data.factionB.contains(token)) {
                side = "B";
            }
        }
        if (side.isBlank()) {
            side = sideOf(player, data);
        }
        if (side.isBlank()) {
            return false;
        }
        if (side.equals("A")) {
            data.scoreA += points;
        } else {
            data.scoreB += points;
        }
        data.setDirty();
        ReputationService.add(player, side.equals("A") ? data.factionA : data.factionB, Math.max(1, points));
        player.displayClientMessage(Component.translatable("message.seeking_immortals.sect_war.score",
                data.factionA, data.scoreA, data.factionB, data.scoreB), true);
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
        if (server.overworld().getGameTime() > data.endsAtGameTime) {
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
        if (server.overworld().getGameTime() > data.endsAtGameTime) {
            stop(server);
            return;
        }
        String side = sideOf(killer, data);
        if (side.isBlank()) {
            return;
        }
        if (side.equals("A")) {
            data.scoreA += Math.max(1, points);
        } else {
            data.scoreB += Math.max(1, points);
        }
        data.setDirty();
        ReputationService.add(killer, side.equals("A") ? data.factionA : data.factionB, fromKill ? 2 : Math.max(1, points));
        killer.displayClientMessage(Component.translatable("message.seeking_immortals.sect_war.score",
                data.factionA, data.scoreA, data.factionB, data.scoreB), true);
    }

    public static String status(MinecraftServer server) {
        WarData data = WarData.get(server.overworld());
        if (!data.active) {
            return "inactive";
        }
        long remain = Math.max(0L, data.endsAtGameTime - server.overworld().getGameTime());
        return data.factionA + "=" + data.scoreA + " vs " + data.factionB + "=" + data.scoreB
                + " | remainTicks=" + remain;
    }

    /**
     * Wave485/486: battlefield AI pulse for a participating player.
     * Phase scales with war progress (early/mid/late) and awards kill-streak bonuses.
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
        String enemyToken = side.equals("A") ? data.factionB : data.factionA;
        int count = phase; // 1/2/3 shells
        double health = 24.0D + phase * 8.0D;
        double damage = 4.0D + phase * 1.5D;
        int spawned = 0;
        for (int i = 0; i < count; i++) {
            int ox = 3 + level.random.nextInt(4 + phase);
            int oz = 3 + level.random.nextInt(4 + phase);
            if (level.random.nextBoolean()) {
                ox = -ox;
            }
            if (level.random.nextBoolean()) {
                oz = -oz;
            }
            BlockPos pos = player.blockPosition().offset(ox, 0, oz);
            SummonedServitorEntity.Archetype archetype =
                    com.xunxian.seekingimmortals.worldpack.TrialCombatShellService.archetypeFor(enemyToken);
            // Late war mixes archetypes for pressure variety.
            if (phase >= 3 && i == 0) {
                archetype = SummonedServitorEntity.Archetype.PUPPET;
            } else if (phase >= 2 && i == count - 1) {
                archetype = SummonedServitorEntity.Archetype.GHOST;
            }
            Mob shell = com.xunxian.seekingimmortals.worldpack.TrialCombatShellService.spawnHostile(
                    level, pos, player.getYRot(), "war_" + enemyToken + "_p" + phase, health, damage, archetype);
            if (shell == null) {
                continue;
            }
            shell.setCustomName(Component.translatable("entity.seeking_immortals.war_shell.name", enemyToken));
            shell.setCustomNameVisible(true);
            shell.setTarget(player);
            CompoundTag tag = shell.getPersistentData().getCompound(WAR_SHELL_TAG).copy();
            tag.putString(WAR_SHELL_SIDE, side);
            tag.putInt("Phase", phase);
            shell.getPersistentData().put(WAR_SHELL_TAG, tag);
            spawned++;
        }
        if (spawned > 0) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.sect_war.ai_pulse_phase", enemyToken, spawned, phase), true);
        }
    }

    /** 1 early / 2 mid / 3 late based on remaining war time fraction. */
    public static int warPhase(WarData data, long nowGameTime) {
        if (data == null || !data.active) {
            return 1;
        }
        long remain = Math.max(0L, data.endsAtGameTime - nowGameTime);
        // Approximate total window from remaining + elapsed is unknown; use absolute remain buckets.
        if (remain > 20L * 60L * 6L) { // >6 min left
            return 1;
        }
        if (remain > 20L * 60L * 2L) { // 2-6 min
            return 2;
        }
        return 3;
    }

    public static boolean isWarShell(Mob mob) {
        return mob != null && mob.getPersistentData().contains(WAR_SHELL_TAG);
    }

    public static void onWarShellKilled(ServerPlayer killer, Mob shell) {
        if (killer == null || shell == null || !isWarShell(shell) || killer.server == null) {
            return;
        }
        if (!isActive(killer.server)) {
            return;
        }
        CompoundTag shellTag = shell.getPersistentData().getCompound(WAR_SHELL_TAG);
        int phase = Math.max(1, shellTag.getInt("Phase"));
        WarData data = WarData.get(killer.server.overworld());
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
        int points = 1 + (phase - 1) + (streak >= 3 ? 1 : 0) + (streak >= 5 ? 1 : 0);

        if ("A".equals(side)) {
            data.scoreA += points;
            ReputationService.add(killer, data.factionA, points);
        } else {
            data.scoreB += points;
            ReputationService.add(killer, data.factionB, points);
        }
        data.setDirty();
        killer.displayClientMessage(Component.translatable("message.seeking_immortals.sect_war.score",
                data.factionA, data.scoreA, data.factionB, data.scoreB), true);
        if (streak >= 3) {
            killer.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.sect_war.streak", streak, points), true);
        }
    }

    private static String sideOf(ServerPlayer player, WarData data) {
        String[] sect = {""};
        com.xunxian.seekingimmortals.cultivation.CultivationHelper.get(player).ifPresent(cultivation ->
                sect[0] = cultivation.getSevenMysteriesQuest().getSectId());
        if (sect[0] == null || sect[0].isBlank()) {
            return "";
        }
        String s = sect[0].toLowerCase(Locale.ROOT);
        if (s.contains(data.factionA)) {
            return "A";
        }
        if (s.contains(data.factionB)) {
            return "B";
        }
        return "";
    }

    private static void broadcast(MinecraftServer server, Component message) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.displayClientMessage(message, false);
        }
    }

    private static String normalize(String id) {
        return id == null ? "qinglan" : id.trim().toLowerCase(Locale.ROOT);
    }
}
